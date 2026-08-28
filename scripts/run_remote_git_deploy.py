#!/usr/bin/env python3
import sys

import paramiko

HOST = "8.141.112.182"
USER = "root"
PASSWORD = sys.argv[1] if len(sys.argv) > 1 else ""

if not PASSWORD:
    print("Usage: python scripts/run_remote_git_deploy.py <ssh-password>")
    sys.exit(1)

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect(HOST, username=USER, password=PASSWORD, timeout=20)

cmd = (
    "bash -lc 'set -e; cd /opt/clas; "
    "git fetch upstream dev; git reset --hard upstream/dev; git clean -fd; "
    "clas deploy 2>&1; sleep 2; curl -sf http://127.0.0.1:8080/api/health; echo; "
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
