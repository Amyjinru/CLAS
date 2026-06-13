#!/usr/bin/env python3
import sys
from pathlib import Path

import paramiko

HOST = "8.141.112.182"
USER = "root"
PASSWORD = sys.argv[1] if len(sys.argv) > 1 else ""
SQL_FILES = [
    Path(__file__).resolve().parents[1] / "database" / "seed-demo-fix-and-boost.sql",
    Path(__file__).resolve().parents[1] / "database" / "seed-demo-favorites-reviews.sql",
    Path(__file__).resolve().parents[1] / "database" / "seed-demo-extra-reviews.sql",
]

if not PASSWORD:
    print("Usage: python scripts/run_remote_seed.py <ssh-password>")
    sys.exit(1)

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect(HOST, username=USER, password=PASSWORD, timeout=20)
sftp = client.open_sftp()

exit_code = 0
for sql_path in SQL_FILES:
    remote_sql = f"/tmp/{sql_path.name}"
    sftp.put(str(sql_path), remote_sql)
    cmd = (
        "bash -lc 'set -e; source /etc/clas/clas.env; "
        f"mysql -h\"$MYSQL_HOST\" -P\"$MYSQL_PORT\" -u\"$MYSQL_USER\" -p\"$MYSQL_PASSWORD\" \"$MYSQL_DATABASE\" < {remote_sql}'"
    )
    stdin, stdout, stderr = client.exec_command(cmd, timeout=180)
    out = stdout.read().decode("utf-8", errors="replace")
    err = stderr.read().decode("utf-8", errors="replace")
    code = stdout.channel.recv_exit_status()
    print(f"=== {sql_path.name} EXIT {code} ===")
    if out.strip():
        print(out)
    if err.strip():
        print(err)
    exit_code = max(exit_code, code)

sftp.close()

verify_cmd = (
    "bash -lc 'source /etc/clas/clas.env; "
    "mysql -h\"$MYSQL_HOST\" -P\"$MYSQL_PORT\" -u\"$MYSQL_USER\" -p\"$MYSQL_PASSWORD\" \"$MYSQL_DATABASE\" -e "
    "\"SELECT phone, username FROM user WHERE phone LIKE \\\"1380000000%\\\" OR phone LIKE \\\"1380000001%\\\" ORDER BY phone; "
    "SELECT m.id, m.merchant_name, ROUND(m.score,2) score, COUNT(DISTINCT f.user_id) fav_users, COUNT(DISTINCT r.id) reviews "
    "FROM merchant m LEFT JOIN favorite f ON f.merchant_id=m.id LEFT JOIN orders o ON o.merchant_id=m.id "
    "LEFT JOIN review r ON r.order_id=o.id WHERE m.status=\\\"OPEN\\\" AND m.logo IS NOT NULL AND TRIM(m.logo)<>\\\"\\\" "
    "GROUP BY m.id, m.merchant_name, m.score ORDER BY m.id LIMIT 15;\"'"
)
stdin, stdout, stderr = client.exec_command(verify_cmd, timeout=60)
print("VERIFY:")
print(stdout.read().decode("utf-8", errors="replace"))
client.close()
sys.exit(exit_code)
