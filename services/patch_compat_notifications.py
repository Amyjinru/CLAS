#!/usr/bin/env python3
"""Replace NotificationService with NotificationBridge in compat services."""
from pathlib import Path

ROOT = Path(__file__).resolve().parent / "clas-compat" / "src" / "main" / "java" / "com" / "clas" / "service"

def patch_file(path: Path):
    text = path.read_text(encoding="utf-8")
    orig = text
    text = text.replace("NotificationService.NotificationTarget", "NotificationBridge.NotificationTarget")
    text = text.replace("private final NotificationService notifications;", "private final NotificationBridge notifications;")
    text = text.replace("private final NotificationService notificationService;", "private final NotificationBridge notificationService;")
    text = text.replace("NotificationService notifications,", "NotificationBridge notifications,")
    text = text.replace("NotificationService notificationService,", "NotificationBridge notificationService,")
    text = text.replace("new NotificationService.NotificationTarget(", "new NotificationBridge.NotificationTarget(")
    if text != orig:
        path.write_text(text, encoding="utf-8")
        print(f"patched {path.name}")

for f in ROOT.glob("*.java"):
    if f.name == "NotificationBridge.java":
        continue
    if "NotificationService" in f.read_text(encoding="utf-8"):
        patch_file(f)

print("done")
