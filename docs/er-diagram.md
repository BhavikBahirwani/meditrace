# MediTrace – ER diagram

Rendered automatically by GitHub (Mermaid). Source of truth: [`sql/schema.sql`](../sql/schema.sql).

```mermaid
erDiagram
    USERS ||--o{ BATCHES : "manufactures"
    BATCHES ||--|{ PACKS : "contains"
    USERS |o--o{ PACKS : "currently holds"
    BATCHES ||--|{ TRANSACTIONS : "history of"
    USERS ||--o{ TRANSACTIONS : "sends (from)"
    USERS |o--o{ TRANSACTIONS : "receives (to)"
    BLOCKS |o--o| TRANSACTIONS : "seals"
    PACKS |o--o{ SCANS : "is scanned in"
    SCANS |o--o{ REPORTS : "leads to"
    BATCHES ||--o{ RECALLS : "is recalled by"
    USERS ||--o{ RECALLS : "issues"

    USERS {
        BIGINT user_id PK
        VARCHAR username UK
        VARCHAR email UK
        VARCHAR password_hash "PBKDF2-SHA512"
        VARCHAR full_name
        ENUM role "MANUFACTURER|DISTRIBUTOR|PHARMACY"
        VARCHAR company
        VARCHAR city
        VARCHAR license_no
        DATETIME created_at
    }
    BATCHES {
        BIGINT batch_id PK
        VARCHAR batch_no UK "e.g. B1023"
        VARCHAR medicine_name
        VARCHAR strength
        VARCHAR dosage_form
        BIGINT manufacturer_id FK
        DATE mfg_date
        DATE expiry_date
        DECIMAL mrp "INR"
        INT pack_count
        DATETIME created_at
    }
    PACKS {
        BIGINT pack_id PK
        VARCHAR pack_code UK "MT-B1023-7F3A9C"
        BIGINT batch_id FK
        ENUM status "IN_CHAIN|SOLD"
        BIGINT current_holder_id FK
        VARCHAR sold_city
        DATETIME sold_at
        DATETIME created_at
    }
    BLOCKS {
        INT block_index PK
        CHAR prev_hash "links to previous block"
        CHAR hash UK "SHA-256"
        BIGINT nonce "proof-of-work"
        BIGINT timestamp "epoch ms"
        TEXT data_json "payload incl. txHash, batchHash"
        VARCHAR signature "reserved"
    }
    TRANSACTIONS {
        BIGINT tx_id PK
        BIGINT batch_id FK
        ENUM tx_type "REGISTER|HANDOVER"
        BIGINT from_user_id FK
        BIGINT to_user_id FK
        INT quantity
        DATETIME tx_time
        INT block_index FK, UK "NULL until mined"
        VARCHAR remarks
    }
    SCANS {
        BIGINT scan_id PK
        VARCHAR pack_code
        BIGINT pack_id FK
        ENUM result "GENUINE|TAMPERED|POSSIBLE_FAKE|NOT_FOUND"
        VARCHAR city
        VARCHAR ip_address
        VARCHAR user_agent
        DATETIME scanned_at
    }
    REPORTS {
        BIGINT report_id PK
        VARCHAR pack_code
        BIGINT scan_id FK
        VARCHAR reporter_name
        VARCHAR reporter_phone
        VARCHAR city
        VARCHAR description
        ENUM status "OPEN|INVESTIGATING|CLOSED"
        DATETIME created_at
    }
    RECALLS {
        BIGINT recall_id PK
        BIGINT batch_id FK
        BIGINT issued_by FK
        VARCHAR reason
        ENUM severity "CLASS_I|CLASS_II|CLASS_III"
        DATE recall_date
        BOOLEAN active
    }
```

## Design notes

- **One `users` table with a `role` column** rather than three tables. The three roles share the same columns, and the Java side still gets proper subclasses through `UserFactory`.
- **`packs` is one row per physical pack.** That is what makes a copied QR detectable: a second scan of a code whose status is already `SOLD`.
- **`transactions.block_index` is nullable and UNIQUE.** A row is written immediately, then the Miner thread links it to exactly one block.
- **`scans.pack_code` has no foreign key**, so scans of unknown (fake) codes are recorded too.
- **`blocks.data_json` is `TEXT`, not `JSON`.** MySQL would re-format a JSON column and change the hash.
- **Deletes are restricted** (no `ON DELETE CASCADE` on ledger tables). Supply-chain history must never disappear silently.
