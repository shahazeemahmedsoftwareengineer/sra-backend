import psycopg2
from cryptography.fernet import Fernet, InvalidToken
import base64
import hashlib
import time

print("ATTACK 2: Dictionary attack with 10,000 common passwords")
print("=" * 60)

conn = psycopg2.connect(
    host="localhost", port=5434,
    database="healthco", user="healthco_user", password="healthco123"
)
cur = conn.cursor()
cur.execute("SELECT full_name_enc FROM patients LIMIT 1")
encrypted_sample = cur.fetchone()[0]
cur.close()
conn.close()

dictionary = []

base_words = [
    "password", "admin", "health", "patient", "doctor",
    "hospital", "medical", "healthco", "company", "india",
    "database", "secure", "encrypt", "private", "data"
]

for word in base_words:
    dictionary.extend([
        word, word + "123", word + "2024", word + "!",
        word.upper(), word.capitalize(), word + "@123", word + "#2024",
    ])

for i in range(1000):
    dictionary.append(f"key{i:04d}")
    dictionary.append(f"pass{i:04d}")

print(f"Trying {len(dictionary)} combinations...")
start =