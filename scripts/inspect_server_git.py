#!/usr/bin/env python3
import paramiko

c = paramiko.SSHClient()
c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
c.connect("8.141.112.182", username="root", password="abc123456!", timeout=20)
cmds = [
    "cd /opt/clas && git status -sb",
    "cd /opt/clas && git log -1 --oneline",
    "cd /opt/clas && git pull upstream dev 2>&1; echo PULL_EXIT=$?",
    "cd /opt/clas && git rev-parse HEAD",
]
for cmd in cmds:
    print("===", cmd, "===")
    stdin, stdout, stderr = c.exec_command(cmd, timeout=120)
    print(stdout.read().decode("utf-8", errors="replace"))
    err = stderr.read().decode("utf-8", errors="replace")
    if err.strip():
        print("STDERR:", err)
c.close()
