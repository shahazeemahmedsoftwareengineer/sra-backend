import psycopg2
from cryptography.fernet import Fernet, InvalidToken
import base64
import hashlib

print("ATTACK 1: Trying common/weak encryption keys")
print("=" * 50)

# Common keys hackers try first
common_attempts = [
    "password", "123456", "admin", "secret",
    "healthco", "database", "encryption",
    "healthco123", "admin123", "postgres123",
    "key123456", "mykey", "aeskey256",
    "0" * 64,  # all zeros
    "f" * 64,  # all f's
    "1234567890abcdef" * 4,  # repeating pattern
    "healthco_secret_key_2024_production",
    "sra_shield_key", "sra", "shield"
]

conn = psycopg2.connect(
    host="localhost", port=5434,
    database="healthco", user="healthco_user", password="healthco123"
)
cur = conn.cursor()
cur.execute("SELECT full_name_enc FROM patients LIMIT 1")
encrypted_sample = cur.fetchone()[0]
cur.close()
conn.close()

print(f"Target encrypted data: {encrypted_sample[:50]}...")
print()

cracked = False
for attempt in common_attempts:
    try:
        # Try as raw string padded to 32 bytes
        key_bytes = attempt.encode().ljust(32, b'0')[:32]
        fernet_key = base64.urlsafe_b64encode(key_bytes)
        f = Fernet(fernet_key)
        result = f.decrypt(encrypted_sample.encode())
        print(f"CRACKED with key: {attempt}")
        print(f"Decrypted: {result.decode()}")
        cracked = True
        break
    except Exception:
        print(f"  FAILED: {attempt[:30]}")

if not cracked:
    print()
    print("RESULT: All common keys FAILED.")
    print("Data remains encrypted.")