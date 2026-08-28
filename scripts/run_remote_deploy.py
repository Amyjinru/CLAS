#!/usr/bin/env python3
"""Upload changed files only — prefer run_remote_git_deploy.py to avoid breaking CI git pull."""
import sys
from pathlib import Path

import paramiko

HOST = "8.141.112.182"
USER = "root"
PASSWORD = sys.argv[1] if len(sys.argv) > 1 else ""

if len(sys.argv) > 2 and sys.argv[1] == "--git":
    PASSWORD = sys.argv[2]
    use_git_sync = True
else:
    use_git_sync = False

ROOT = Path(__file__).resolve().parents[1]
FILES = [
    "backend/src/main/java/com/clas/dto/MerchantResponse.java",
    "backend/src/main/java/com/clas/service/MerchantService.java",
    "backend/src/main/java/com/clas/service/RecommendService.java",
    "backend/src/test/java/com/clas/service/MerchantAveragePriceTest.java",
    "backend/src/test/java/com/clas/service/RecommendServiceTest.java",
    "frontend/src/views/HomeView.vue",
    "frontend/src/views/MerchantDetailView.vue",
]

if not PASSWORD:
    print("Usage:")
    print("  python scripts/run_remote_deploy.py <ssh-password>          # legacy file upload")
    print("  python scripts/run_remote_deploy.py --git <ssh-password>    # recommended git sync")
    sys.exit(1)

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect(HOST, username=USER, password=PASSWORD, timeout=20)

if not use_git_sync:
    sftp = client.open_sftp()
    for rel in FILES:
        local = ROOT / rel
        remote = "/opt/clas/" + rel.replace("\\", "/")
        sftp.put(str(local), remote)
        print("uploaded", rel)
    sftp.close()

cmd = (
    "bash -lc 'set -e; cd /opt/clas; "
    + (
        "git fetch upstream dev; git reset --hard upstream/dev; git clean -fd; "
        if use_git_sync
        else ""
    )
    + "clas deploy 2>&1; sleep 2; curl -sf http://127.0.0.1:8080/api/health; echo; "
    "git log -1 --oneline'"
)
stdin, stdout, stderr = client.exec_command(cmd, timeout=600)
out = stdout.read().decode("utf-8", errors="replace")
err = stderr.read().decode("utf-8", errors="replace")
code = stdout.channel.recv_exit_status()
print("EXIT", code)
if out:
    print(out[-5000:])
if err:
    print("STDERR:", err[-2000:])
client.close()
sys.exit(code)
