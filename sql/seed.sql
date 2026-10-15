-- =====================================================================
--  MediTrace - demo data (run AFTER schema.sql)
--  All three demo accounts use the password:  demo1234
--  (hashes generated with: java -cp target/classes com.meditrace.util.PasswordUtil demo1234)
--
--  NOTE: no rows are inserted into `blocks` here. When the web app starts, the
--  AppContextListener creates the genesis block and the Miner thread seals every
--  transaction below into a real, proof-of-work block.
-- =====================================================================

USE meditrace;

-- ---------------------------------------------------------------- users
INSERT INTO users (user_id, username, email, password_hash, full_name, role, company, city, license_no, created_at) VALUES
(1, 'xyzpharma', 'qa@xyzpharma.in',
 'pbkdf2_sha512$210000$9laocvfYMDMQHkJp5C6XHQ==$QTf2ZheFM4jv1to3vwVhN77mFr28Jx4kvtbNXw3PunA93iysCaVUIHT+iLmcUobfjd9tOkTZYCumog9ZQp5+0A==',
 'Rajesh Kumar', 'MANUFACTURER', 'XYZ Pharma Pvt. Ltd.', 'Baddi', 'MNB/05/312', '2026-06-01 10:00:00'),
(2, 'abcdist', 'orders@abcdistributors.in',
 'pbkdf2_sha512$210000$tlBGUQBa5lZOoeHEdE4khg==$EJPItzFgbC6mvIcOoxgHWf2AyEkLo/pIWnn4Jwn9V/QMCtbmB0s8l/8+zGMHIAiBENxwJJIdWtwL0Amv/8cimQ==',
 'Neha Gupta', 'DISTRIBUTOR', 'ABC Distributors', 'Delhi', 'DL-WLF20B-1187', '2026-06-03 11:30:00'),
(3, 'sharmamed', 'sharmamedicals.gzb@gmail.com',
 'pbkdf2_sha512$210000$VWgDYwcNcNqPdvG1wAYVeA==$/cw2Myt5hTtrRd5ZiJHkJ9YYoVqFQihYUHU3jEV1yPJdqhJILCZDTihMKFJQOZp3XYBvBMQDOKmvVg31HRItTg==',
 'Amit Sharma', 'PHARMACY', 'Sharma Medicals', 'Ghaziabad', 'UP-GZB-RLF20-4471', '2026-06-05 09:15:00');

-- ---------------------------------------------------------------- batches
INSERT INTO batches (batch_id, batch_no, medicine_name, strength, dosage_form, manufacturer_id, mfg_date, expiry_date, mrp, pack_count, created_at) VALUES
(1, 'B1023', 'Paracetamol',  '500 mg', 'Tablet',  1, '2026-07-01', '2028-06-30', 25.00, 12, '2026-07-02 09:00:00'),
(2, 'B1024', 'Amoxicillin',  '250 mg', 'Capsule', 1, '2026-08-10', '2028-07-31', 68.50,  8, '2026-08-11 10:20:00'),
(3, 'B1025', 'Cetirizine',   '10 mg',  'Tablet',  1, '2026-09-20', '2028-08-31', 18.00,  6, '2026-09-21 15:45:00'),
(4, 'B0991', 'Pantoprazole', '40 mg',  'Tablet',  1, '2026-03-05', '2028-02-29', 92.00,  6, '2026-03-06 12:00:00');

-- ---------------------------------------------------------------- packs
-- B1023 Paracetamol: reached Sharma Medicals; two packs already sold in Ghaziabad.
INSERT INTO packs (pack_code, batch_id, status, current_holder_id, sold_city, sold_at, created_at) VALUES
('MT-B1023-7F3A9C', 1, 'IN_CHAIN', 3, NULL, NULL, '2026-07-02 09:00:00'),
('MT-B1023-2D81E4', 1, 'SOLD',     3, 'Ghaziabad', '2026-10-02 18:40:00', '2026-07-02 09:00:00'),
('MT-B1023-9F2704', 1, 'IN_CHAIN', 3, NULL, NULL, '2026-07-02 09:00:00'),
('MT-B1023-23CE24', 1, 'IN_CHAIN', 3, NULL, NULL, '2026-07-02 09:00:00'),
('MT-B1023-9029BC', 1, 'IN_CHAIN', 3, NULL, NULL, '2026-07-02 09:00:00'),
('MT-B1023-487C09', 1, 'SOLD',     3, 'Ghaziabad', '2026-10-07 11:05:00', '2026-07-02 09:00:00'),
('MT-B1023-3D61BB', 1, 'IN_CHAIN', 3, NULL, NULL, '2026-07-02 09:00:00'),
('MT-B1023-B18145', 1, 'IN_CHAIN', 3, NULL, NULL, '2026-07-02 09:00:00'),
('MT-B1023-CB5301', 1, 'IN_CHAIN', 3, NULL, NULL, '2026-07-02 09:00:00'),
('MT-B1023-A16811', 1, 'IN_CHAIN', 3, NULL, NULL, '2026-07-02 09:00:00'),
('MT-B1023-CF20EE', 1, 'IN_CHAIN', 3, NULL, NULL, '2026-07-02 09:00:00'),
('MT-B1023-F14E87', 1, 'IN_CHAIN', 3, NULL, NULL, '2026-07-02 09:00:00');

-- B1024 Amoxicillin: with ABC Distributors.
INSERT INTO packs (pack_code, batch_id, status, current_holder_id, created_at) VALUES
('MT-B1024-CEC213', 2, 'IN_CHAIN', 2, '2026-08-11 10:20:00'),
('MT-B1024-026DE1', 2, 'IN_CHAIN', 2, '2026-08-11 10:20:00'),
('MT-B1024-21330F', 2, 'IN_CHAIN', 2, '2026-08-11 10:20:00'),
('MT-B1024-6B0159', 2, 'IN_CHAIN', 2, '2026-08-11 10:20:00'),
('MT-B1024-3A4FCD', 2, 'IN_CHAIN', 2, '2026-08-11 10:20:00'),
('MT-B1024-F8A892', 2, 'IN_CHAIN', 2, '2026-08-11 10:20:00'),
('MT-B1024-044952', 2, 'IN_CHAIN', 2, '2026-08-11 10:20:00'),
('MT-B1024-076A3A', 2, 'IN_CHAIN', 2, '2026-08-11 10:20:00');

-- B1025 Cetirizine: still at the factory in Baddi.
INSERT INTO packs (pack_code, batch_id, status, current_holder_id, created_at) VALUES
('MT-B1025-075FBB', 3, 'IN_CHAIN', 1, '2026-09-21 15:45:00'),
('MT-B1025-A61919', 3, 'IN_CHAIN', 1, '2026-09-21 15:45:00'),
('MT-B1025-1AF9F9', 3, 'IN_CHAIN', 1, '2026-09-21 15:45:00'),
('MT-B1025-A17FFA', 3, 'IN_CHAIN', 1, '2026-09-21 15:45:00'),
('MT-B1025-C4D7EE', 3, 'IN_CHAIN', 1, '2026-09-21 15:45:00'),
('MT-B1025-6BA755', 3, 'IN_CHAIN', 1, '2026-09-21 15:45:00');

-- B0991 Pantoprazole: reached the pharmacy, later recalled (see recalls).
INSERT INTO packs (pack_code, batch_id, status, current_holder_id, created_at) VALUES
('MT-B0991-A96F09', 4, 'IN_CHAIN', 3, '2026-03-06 12:00:00'),
('MT-B0991-450DFB', 4, 'IN_CHAIN', 3, '2026-03-06 12:00:00'),
('MT-B0991-02733B', 4, 'IN_CHAIN', 3, '2026-03-06 12:00:00'),
('MT-B0991-73A258', 4, 'IN_CHAIN', 3, '2026-03-06 12:00:00'),
('MT-B0991-E08936', 4, 'IN_CHAIN', 3, '2026-03-06 12:00:00'),
('MT-B0991-2B2337', 4, 'IN_CHAIN', 3, '2026-03-06 12:00:00');

-- ---------------------------------------------------------------- transactions (block_index filled in by the Miner)
INSERT INTO transactions (batch_id, tx_type, from_user_id, to_user_id, quantity, tx_time, remarks) VALUES
(4, 'REGISTER', 1, NULL, 6,  '2026-03-06 12:00:00', 'Batch registered at Baddi plant'),
(4, 'HANDOVER', 1, 2,    6,  '2026-03-20 16:30:00', 'Dispatched via road, invoice XYZ/INV/0412'),
(4, 'HANDOVER', 2, 3,    6,  '2026-04-02 11:10:00', 'Delivered to Sharma Medicals'),
(1, 'REGISTER', 1, NULL, 12, '2026-07-02 09:00:00', 'Batch registered at Baddi plant'),
(1, 'HANDOVER', 1, 2,    12, '2026-07-18 14:25:00', 'Dispatched via road, invoice XYZ/INV/0877'),
(1, 'HANDOVER', 2, 3,    12, '2026-08-05 10:40:00', 'Delivered to Sharma Medicals'),
(2, 'REGISTER', 1, NULL, 8,  '2026-08-11 10:20:00', 'Batch registered at Baddi plant'),
(2, 'HANDOVER', 1, 2,    8,  '2026-08-29 17:05:00', 'Cold-room storage at Delhi warehouse'),
(3, 'REGISTER', 1, NULL, 6,  '2026-09-21 15:45:00', 'Batch registered at Baddi plant');

-- ---------------------------------------------------------------- scans
INSERT INTO scans (pack_code, pack_id, result, city, ip_address, user_agent, scanned_at) VALUES
('MT-B1023-2D81E4', 2, 'GENUINE',       'Ghaziabad', '103.21.58.14',  'Mozilla/5.0 (Linux; Android 14)', '2026-10-02 18:52:00'),
('MT-B1023-487C09', 6, 'GENUINE',       'Ghaziabad', '103.21.58.90',  'Mozilla/5.0 (Linux; Android 13)', '2026-10-07 11:20:00'),
('MT-B1023-2D81E4', 2, 'POSSIBLE_FAKE', 'Lucknow',   '49.36.112.7',   'Mozilla/5.0 (iPhone; iOS 18)',    '2026-10-09 20:14:00'),
('MT-B1023-7F3A9C', 1, 'GENUINE',       'Ghaziabad', '103.21.58.33',  'Mozilla/5.0 (Linux; Android 14)', '2026-10-12 17:03:00'),
('MT-B7777-ABCDEF', NULL, 'NOT_FOUND',  'Lucknow',   '49.36.112.7',   'Mozilla/5.0 (iPhone; iOS 18)',    '2026-10-10 09:31:00');

-- ---------------------------------------------------------------- reports
INSERT INTO reports (pack_code, scan_id, reporter_name, reporter_phone, city, description, status, created_at) VALUES
('MT-B1023-2D81E4', 3, 'Priya Verma', '98XXXXXX21', 'Lucknow',
 'Bought this strip from a roadside vendor near Charbagh. App says the code was already sold in Ghaziabad. Print quality of the foil is poor.',
 'INVESTIGATING', '2026-10-09 20:20:00');

-- ---------------------------------------------------------------- recalls
INSERT INTO recalls (batch_id, issued_by, reason, severity, recall_date, active) VALUES
(4, 1, 'Declared Not of Standard Quality (NSQ) in the CDSCO monthly drug alert - dissolution test failure.',
 'CLASS_II', '2026-09-28', TRUE);
