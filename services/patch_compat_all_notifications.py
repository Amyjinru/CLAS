#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parent / "clas-compat" / "src" / "main" / "java" / "com" / "clas" / "service"

for path in ROOT.glob("*.java"):
    if path.name == "NotificationBridge.java":
        continue
    text = path.read_text(encoding="utf-8")
    if "NotificationService" not in text:
        continue
    text = text.replace("NotificationService.NotificationTarget", "NotificationBridge.NotificationTarget")
    text = text.replace("NotificationService notifications", "NotificationBridge notifications")
    text = text.replace("NotificationService notificationService", "NotificationBridge notificationService")
    text = text.replace("NotificationService notifications,", "NotificationBridge notifications,")
    text = text.replace("NotificationService notificationService,", "NotificationBridge notificationService,")
    text = text.replace("new NotificationService.NotificationTarget(", "new NotificationBridge.NotificationTarget(")
    path.write_text(text, encoding="utf-8")
    print(path.name)

print("done")
