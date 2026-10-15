-- =====================================================================
--  MediTrace - live tamper demo for the review.
--
--  1. Start the app and scan MT-B1024-CEC213  -> GENUINE.
--  2. Run the TAMPER statement below in MySQL (a "corrupt insider" extends
--     the expiry date of an old batch directly in the database).
--  3. Scan MT-B1024-CEC213 again               -> TAMPERED, because the batch
--     no longer matches the fingerprint sealed in its blockchain block.
--  4. Run the RESTORE statement to put things back.
-- =====================================================================

USE meditrace;

-- TAMPER
UPDATE batches SET expiry_date = '2030-12-31' WHERE batch_no = 'B1024';

-- RESTORE (run separately afterwards)
-- UPDATE batches SET expiry_date = '2028-07-31' WHERE batch_no = 'B1024';
