# MediTrace – architecture

## Layers

```mermaid
flowchart TB
    subgraph Client["Client"]
        C1["Customer phone<br/>(scans QR)"]
        C2["Manufacturer / Distributor /<br/>Pharmacy browser"]
    end

    subgraph Tomcat["Apache Tomcat 10.1 – meditrace.war"]
        F1["EncodingFilter<br/>UTF-8 + security headers"]
        F2["AuthFilter<br/>session + role check"]
        S["Servlets<br/>Home · Login · Register · Logout<br/>Verify · Dashboard · Batch"]
        J["JSP views + JSTL<br/>css/style.css"]
        SV["Services<br/>Auth · Batch · Verification<br/>Ledger · Dashboard"]
        D["DAOs<br/>GenericDAO&lt;T,ID&gt; + AbstractDAO"]
        subgraph BC["blockchain package (pure Java)"]
            CH["Blockchain<br/>List&lt;Block&gt;"]
            MI["Miner thread<br/>BlockingQueue"]
            VA["ChainValidator<br/>every 2 min"]
        end
        L["AppContextListener<br/>start / stop"]
    end

    DB[("MySQL 8<br/>8 tables")]

    C1 -- "GET /verify?code=…" --> F1
    C2 --> F1
    F1 --> F2 --> S
    S --> SV --> D --> DB
    S --> J
    SV -- "submit(MiningTask)" --> MI
    MI -- "mine + addBlock" --> CH
    MI -- "persist (JDBC tx)" --> D
    VA -- "loadAllBlocks + validate" --> D
    VA -. compare .-> CH
    SV -- "trusted hashes" --> CH
    L -. creates .-> CH
    L -. starts .-> MI
    L -. schedules .-> VA
```

## Batch registration (JDBC transaction + background mining)

```mermaid
sequenceDiagram
    actor M as Manufacturer
    participant BS as BatchServlet
    participant SV as BatchService
    participant DB as MySQL
    participant Q as Miner queue
    participant MI as Miner thread
    participant BC as Blockchain

    M->>BS: POST /manufacturer/batches
    BS->>SV: registerBatch(user, batch)
    SV->>DB: setAutoCommit(false)
    SV->>DB: INSERT batches
    SV->>DB: INSERT packs (JDBC batch, N rows)
    SV->>DB: INSERT transactions (REGISTER)
    alt all inserts succeed
        SV->>DB: commit()
    else any error
        SV->>DB: rollback()
    end
    SV->>Q: submit(MiningTask)
    BS-->>M: 302 redirect to the batch page (PRG)
    MI->>Q: take()
    MI->>DB: read tx + batch + pack codes
    MI->>BC: prepareNextBlock(payload) – proof-of-work
    MI->>DB: INSERT blocks + UPDATE transactions.block_index (one tx)
    MI->>BC: addBlock(block) – synchronized
```

## Customer verification

```mermaid
flowchart TD
    A["GET /verify?code=X"] --> B{"valid format and<br/>pack exists?"}
    B -- no --> NF["NOT FOUND"]
    B -- yes --> C["load batch, pack codes,<br/>transactions"]
    C --> D{"for every sealed transaction:<br/>block hash OK?<br/>matches in-memory chain?<br/>txHash matches row?<br/>batchHash matches batch?"}
    D -- any mismatch --> T["TAMPERED<br/>(names the changed record)"]
    D -- all match --> E{"pack status SOLD?"}
    E -- yes --> PF["POSSIBLE FAKE<br/>(copied QR)"]
    E -- no --> G["GENUINE"]
    NF & T & PF & G --> LOG["INSERT scans (audit log)"] --> V["verify.jsp: status card,<br/>facts, journey timeline"]
```

## Threads at run time

| Thread | Created by | Job |
|---|---|---|
| Tomcat request threads | Tomcat | Handle HTTP requests (many at once) |
| `meditrace-miner` | `AppContextListener` → `Miner.start()` | Single consumer of the `BlockingQueue`; mines and stores blocks |
| `meditrace-chain-validator` | `ChainValidator.start()` | Re-validates the whole chain from MySQL every 120 s |

Shared state:
- the `Blockchain` uses `synchronized` methods and hands out snapshot copies;
- the latest `ValidationReport` is a `volatile` field;
- the login-failure counters are a `ConcurrentHashMap`.
