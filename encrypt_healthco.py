import psycopg2
from cryptography.fernet import Fernet
import base64
import sys

sra_key = sys.argv[1]
key_bytes = bytes.fromhex(sra_key)
fernet_key = base64.urlsafe_b64encode(key_bytes)
f = Fernet(fernet_key)

conn = psycopg2.connect(
    host="localhost", port=5434,
    database="healthco", user="healthco_user", password="healthco123"
)
cur = conn.cursor()

cur.execute("""
    ALTER TABLE patients 
    ADD COLUMN IF NOT EXISTS full_name_enc TEXT,
    ADD COLUMN IF NOT EXISTS email_enc TEXT,
    ADD COLUMN IF NOT EXISTS phone_enc TEXT,
    ADD COLUMN IF NOT EXISTS aadhaar_enc TEXT,
    ADD COLUMN IF NOT EXISTS diagnosis_enc TEXT,
    ADD COLUMN IF NOT EXISTS prescription_enc TEXT,
    ADD COLUMN IF NOT EXISTS is_encrypted BOOLEAN DEFAULT FALSE
""")
conn.commit()

cur.execute("SELECT id, full_name, email, phone, aadhaar, diagnosis, prescription FROM patients WHERE is_encrypted IS NOT TRUE")
patients = cur.fetchall()
print(f"Encrypting {len(patients)} patient records...")

for patient in patients:
    pid, name, email, phone, aadhaar, diagnosis, prescription = patient
    cur.execute("""
        UPDATE patients SET
            full_name_enc = %s, email_enc = %s, phone_enc = %s,
            aadhaar_enc = %s, diagnosis_enc = %s, prescription_enc = %s,
            is_encrypted = TRUE,
            full_name = '[ENCRYPTED]', email = '[ENCRYPTED]',
            phone = '[ENCRYPTED]', aadhaar = '[ENCRYPTED]',
            diagnosis = '[ENCRYPTED]', prescription = '[ENCRYPTED]'
        WHERE id = %s
    """, (
        f.encrypt(name.encode()).decode(),
        f.encrypt(email.encode()).decode(),
        f.encrypt(phone.encode()).decode(),
        f.encrypt(aadhaar.encode()).decode(),
        f.encrypt(diagnosis.encode()).decode(),
        f.encrypt(prescription.encode()).decode(),
        pid
    ))
    print(f"  Encrypted patient ID {pid} — {name}")

conn.commit()
cur.close()
conn.close()
print("\nAll patient records encrypted with SRA Shield key!")