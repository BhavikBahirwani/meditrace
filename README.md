# MediTrace

**Scan. Verify. Trust your medicine.**

MediTrace is a blockchain-based medicine supply-chain traceability system for India, built as a Java web application (Servlets + JSP + JDBC + MySQL). Every pack gets a unique code at the factory. Each step from **Manufacturer → Distributor → Pharmacy** is sealed into a tamper-evident chain of SHA-256 blocks. A customer scans the QR on the pack and immediately sees one of:

| Result | Meaning |
|---|---|
| 🟢 **Genuine** | The code exists and every record matches what was sealed on the blockchain. |
| 🔴 **Tampered** | A database record (batch details, pack list, handover or block) was changed after it was sealed. |
| 🟠 **Possible fake** | The code was already sold to a customer, so this pack probably carries a copied QR. |
| ⚫ **Not found** | No pack is registered with this code. |

---

## 1. Problem

Fake and substandard medicines are a serious public-health problem in India. Packs pass through many hands (manufacturer, C&F agent, distributor, retailer). Records are kept in separate, easily edited systems, and a customer at the counter has no way to check what they are buying. Counterfeiters exploit this by copying genuine packaging, including printed codes.

MediTrace answers three questions:
1. **Is this pack real?** Every pack code is registered at the factory.
2. **Has anyone changed its history?** Records are fingerprinted into a hash-linked chain, so any later edit is detectable.
3. **Is this code being reused?** A code that was already sold, and is now scanned again, is flagged as a possible copy.

## 2. Features (Review 1)

- Registration and login for the three roles, with PBKDF2 password hashing, sessions and role-based access control
- **Blockchain core in plain Java**:
  - SHA-256 blocks, proof-of-work (difficulty 2) and a genesis block
  - full verification of every hash and every link
- **Miner thread**: a `BlockingQueue` producer-consumer that seals each transaction into a block in the background
- **Chain validator**: re-verifies the whole chain from MySQL every 2 minutes (`ScheduledExecutorService`)
- **Batch registration** in a single JDBC transaction (batch + N packs + REGISTER record, commit/rollback)
- **Public verify page** (mobile-first):
  - status card in 4 states
  - product facts
  - journey timeline showing block numbers and hashes
  - recall and expiry warnings
  - every scan logged
- **Role dashboards**:
  - header in the role's area colour
  - live stats, recent activity, stock and scans, all from the DB
  - a "chain verified" banner
- **Seed data**: realistic Indian demo data, plus a scripted live tamper demo
- **Tests**: 22 JUnit 5 tests for the blockchain, the Miner thread, passwords and JSON

## 3. Tech stack

| Layer | Technology |
|---|---|
| Language | Java 17 bytecode (built with JDK 24) |
| Web | Jakarta Servlet 6.0, JSP 3.1, JSTL 3.0 (no frameworks) |
| Server | Apache Tomcat 10.1 |
| Database | MySQL 8 via JDBC (`mysql-connector-j`), no ORM |
| Build | Maven, through the Maven Wrapper (`mvnw` / `mvnw.cmd`); no Maven install needed |
| UI | Plain HTML + one CSS file. Fonts: Bricolage Grotesque, Atkinson Hyperlegible Next / Mono |
| Tests | JUnit 5 |

## 4. Architecture

```
Browser ──HTTP──► EncodingFilter ─► AuthFilter (role check) ─► Servlets ─► Services ─► DAOs ─► MySQL
                                                                   │            │
                                                                   ▼            ▼
                                                                 JSPs     Blockchain (in memory)
                                                                            ▲         ▲
                                                Miner thread (BlockingQueue)┘         └ ChainValidator (every 2 min)
```

The layers each have one job:
- **Servlets**: read the request, call one service and forward to a JSP.
- **Services**: hold the business rules (verification, batch registration, ledger).
- **DAOs**: run SQL with `PreparedStatement` only.

The `blockchain` package is pure Java with no JDBC, so it is unit-testable. It reaches the database only through the `LedgerGateway` interface.

Detailed diagrams are in [docs/architecture.md](docs/architecture.md), [docs/class-diagram.md](docs/class-diagram.md) and [docs/er-diagram.md](docs/er-diagram.md).

### How tamper detection works
1. When a batch is registered, the Miner seals a **REGISTER** block. It holds `txHash` (the fingerprint of the transaction row) and `batchHash` (the fingerprint of the batch details **and every pack code**).
2. Every handover becomes a **HANDOVER** block with its own `txHash`. Each block stores the previous block's hash, so the blocks form a chain.
3. When a customer scans, `VerificationService` re-reads the rows from MySQL, recomputes the fingerprints and compares them with the sealed values. It also checks that the stored block still hashes correctly and matches the trusted in-memory chain.
4. If anything differs, the result is **Tampered**, and the page says which record changed.

`blocks.data_json` is stored as `TEXT`, not MySQL's `JSON` type. A `JSON` column re-formats the text, which would change the hash.

## 5. Folder structure

```
meditrace/
├── pom.xml                     Maven build (WAR)
├── mvnw, mvnw.cmd, .mvn/       Maven Wrapper
├── sql/
│   ├── schema.sql              8 tables, keys and constraints
│   ├── seed.sql                Indian demo data (password: demo1234)
│   └── demo_tamper.sql         live tamper demo for the review
├── docs/                       ER, class and architecture diagrams (Mermaid)
└── src/
    ├── main/java/com/meditrace/
    │   ├── model/              User (abstract) → Manufacturer/Distributor/Pharmacy, Role, Batch, Pack, …
    │   ├── blockchain/         Block, Blockchain, Miner, ChainValidator, Hashable, Verifiable, …
    │   ├── dao/                GenericDAO<T,ID>, AbstractDAO, RowMapper, 8 DAOs
    │   ├── service/            AuthService, BatchService, VerificationService, LedgerService, DashboardService
    │   ├── servlet/            Home, Login, Register, Logout, Verify, Dashboard, Batch
    │   ├── filter/             EncodingFilter, AuthFilter
    │   ├── listener/           AppContextListener (starts / stops Miner and validator)
    │   ├── exception/          MediTraceException hierarchy
    │   └── util/               DBConnection, PasswordUtil, HashUtil, JsonUtil, …
    ├── main/resources/
    │   └── db.properties.example   copy to db.properties (git-ignored)
    ├── main/webapp/
    │   ├── css/style.css       the only stylesheet
    │   ├── WEB-INF/web.xml     filters, JSP config, error pages
    │   └── WEB-INF/jsp/        pages + fragments (not directly reachable by URL)
    └── test/java/              JUnit 5 tests
```

## 6. Database (ER description)

| Table | Purpose | Key relationships |
|---|---|---|
| `users` | Every participant; `role` ENUM (MANUFACTURER / DISTRIBUTOR / PHARMACY), PBKDF2 `password_hash`, company, city, licence | — |
| `batches` | One production batch (batch no., medicine, strength, dates, MRP, pack count) | `manufacturer_id → users` |
| `packs` | One row per physical pack; `pack_code` (e.g. `MT-B1023-7F3A9C`), `status` IN_CHAIN / SOLD, `sold_city` | `batch_id → batches`, `current_holder_id → users` |
| `blocks` | The persisted blockchain: `block_index`, `prev_hash`, `hash`, `nonce`, `timestamp`, `data_json`, `signature` | linked to each other by `prev_hash` |
| `transactions` | REGISTER / HANDOVER events, each sealed into one block | `batch_id → batches`, `from_user_id`/`to_user_id → users`, `block_index → blocks` |
| `scans` | Every customer verification and its result (unknown codes too) | `pack_id → packs` (nullable) |
| `reports` | Customer complaints about suspected fakes | `scan_id → scans` (nullable) |
| `recalls` | Batches withdrawn from the market | `batch_id → batches`, `issued_by → users` |

Cardinalities:
- A manufacturer has many batches, a batch has many packs, and a batch has many transactions.
- Each transaction is sealed into exactly one block.
- A pack has many scans.

`CHECK` constraints enforce positive MRP and pack counts, and expiry after manufacture.

## 7. Setup (Windows)

**Prerequisites:** JDK 17+ (tested with 24), MySQL 8, Tomcat 10.1, and Eclipse IDE for Enterprise Java and Web Developers.

### 7.1 Create the tables and demo data
Use **MySQL Workbench**: *File → Open SQL Script* → `sql/schema.sql` → ⚡ Execute, then the same for `sql/seed.sql`.

Or use the MySQL command line client:
```sql
source C:/meditrace/sql/schema.sql
source C:/meditrace/sql/seed.sql
```
(PowerShell has no `<` redirection, so use `source` or Workbench.)

> **Resetting the demo data:** always **stop Tomcat first**, then run `schema.sql` **and** `seed.sql`, then start Tomcat. Re-running `schema.sql` while the app is running deletes the ledger underneath it. The dashboard then (correctly) shows a tamper alert, codes show *Not found* until `seed.sql` is loaded, and seeded rows stay *Waiting for miner*, because unsealed transactions are only queued at start-up.

### 7.2 Configure the database password
```powershell
copy src\main\resources\db.properties.example src\main\resources\db.properties
```
Edit `db.properties` and set `db.password=` to your MySQL root password. The file is in `.gitignore`, so it is never committed.

### 7.3 Build from the command line (optional)
```powershell
.\mvnw.cmd package          # Git Bash: ./mvnw package
```
This compiles the code, runs the tests and produces `target\meditrace.war`. You can copy that file into `C:\tomcat10\webapps\` and run `C:\tomcat10\bin\startup.bat`.

### 7.4 Run in Eclipse with Tomcat
1. **File → Import → Maven → Existing Maven Projects** and choose the `meditrace` folder.
2. Right-click the project → **Maven → Update Project** (Alt+F5).
3. **Window → Show View → Servers**, then click *"create a new server"* → **Apache → Tomcat v10.1 Server**:
   - Installation directory: `C:\tomcat10`
   - JRE: your JDK
4. Right-click the server → **Add and Remove…** and add `meditrace`.
5. Start the server and open **http://localhost:8080/meditrace/**

## 8. Demo logins

All passwords are `demo1234`.

| Username | Role | Company |
|---|---|---|
| `xyzpharma` | Manufacturer | XYZ Pharma Pvt. Ltd., Baddi |
| `abcdist` | Distributor | ABC Distributors, Delhi |
| `sharmamed` | Pharmacy | Sharma Medicals, Ghaziabad |

**Demo codes for the verify page**

| Code | Shows |
|---|---|
| `MT-B1023-7F3A9C` | Genuine: Paracetamol 500 mg, MRP ₹25.00, full journey |
| `MT-B1023-2D81E4` | Possible fake: already sold in Ghaziabad |
| `MT-B0991-A96F09` | Genuine, but with a recall warning |
| `MT-B9999-000000` | Not found |
| `MT-B1024-CEC213` | Run `sql/demo_tamper.sql`, then scan again: **Tampered** |

## 9. Review rubric map

| Rubric area | Where to look |
|---|---|
| **Problem understanding & design** | This README; `docs/` diagrams; `sql/schema.sql` (8 normalised tables, FKs, CHECKs); layered packages |
| **Core Java – OOP** | `model/User.java` (abstract, encapsulation) → `Manufacturer`, `Distributor`, `Pharmacy` (inheritance, overridden `getJourneyLabel()` / `canRegisterBatch()` = polymorphism); `Role` enum with fields; `UserFactory` |
| **Core Java – interfaces** | `blockchain/Hashable`, `blockchain/Verifiable` (implemented by `Block`, `Blockchain`, `Batch`, `Transaction`); `dao/GenericDAO<T, ID>` with default methods; `blockchain/LedgerGateway` |
| **Core Java – exceptions** | `exception/*`: checked `MediTraceException` hierarchy; SQL errors translated in `AbstractDAO`; `TamperDetectedException` thrown by `Blockchain.validate()` |
| **Core Java – collections & generics** | `List<Block>` + `Map<String, Block>` in `Blockchain`; `EnumMap` in `UserDAO.countByRole`; `ConcurrentHashMap` login lockout in `AuthService`; `LinkedHashSet` in `PackCodeGenerator`; generic `queryList` / `RowMapper<T>` |
| **Core Java – multithreading** | `blockchain/Miner` (`BlockingQueue`, producer-consumer, daemon thread); `Blockchain` `synchronized` methods; `blockchain/ChainValidator` (`ScheduledExecutorService`, `volatile`); `MinerTest` with 4 concurrent producers |
| **Modern Java** | records (`MiningTask`, `AuthService.FailedAttempts`), switch expressions, pattern-matching `instanceof`, lambdas, `Optional`, streams, `java.time` |
| **JDBC integration** | `util/DBConnection` (singleton, `db.properties`); `dao/AbstractDAO` (`PreparedStatement`, try-with-resources, generated keys); `PackDAO.insertAll` (JDBC batch); `BatchService.registerBatch` and `LedgerService.persist` (transactions with commit/rollback) |
| **Servlets & web integration** | `servlet/*` (GET/POST, PRG redirects, sessions, `changeSessionId`); `filter/AuthFilter` (RBAC, 403); `listener/AppContextListener`; `WEB-INF/web.xml` (filter order, JSP prelude, error pages); JSPs with JSTL + EL |
| **Testing** | `src/test/java`: valid chain passes; tampered data, re-mined blocks, broken links and missing blocks are detected |

## 10. Roadmap (next reviews)

- **QR labels**: generate printable QR stickers per pack, pointing to `/verify?code=…`
- **Handovers in the UI**: distributor/pharmacy accept consignments; the pharmacy marks packs sold at the counter
- **Digital signatures**: each participant gets an RSA key pair, and blocks are signed (the `blocks.signature` column is already reserved)
- **Report fake + heat-map**: a customer report form, plus a city heat-map of suspicious scans for regulators
- **WhatsApp verify**: send a pack code to a WhatsApp number and get the result back
- **Cold chain**: temperature logs for vaccines and insulin, sealed alongside handovers
- Hardening: a connection pool, CSRF tokens, pagination, HTTPS
