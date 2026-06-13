#!/usr/bin/env python3
import json
import sys
import urllib.parse
import urllib.request

HOST = sys.argv[1] if len(sys.argv) > 1 else "8.141.112.182"
TOP_N = int(sys.argv[2]) if len(sys.argv) > 2 else 12

url = f"http://{HOST}/api/merchant/list?" + urllib.parse.urlencode({"sort": "recommend"})
with urllib.request.urlopen(url, timeout=20) as response:
    payload = json.loads(response.read().decode("utf-8"))

merchants = payload.get("data") or []
print(f"=== recommend top {TOP_N} @ {HOST} ===")
no_logo_in_top = 0
for index, merchant in enumerate(merchants[:TOP_N], start=1):
    logo = (merchant.get("logo") or "").strip()
    has_logo = bool(logo)
    if not has_logo:
        no_logo_in_top += 1
    print(
        f"{index:02d}. id={merchant.get('id')} "
        f"name={merchant.get('merchantName')} "
        f"score={merchant.get('score')} "
        f"logo={'Y' if has_logo else 'N'}"
    )

print("")
print(f"top{TOP_N}_without_logo={no_logo_in_top}")
if merchants:
    top = merchants[0]
    print(
        "top1_check="
        + ("PASS" if (top.get("logo") or "").strip() else "FAIL")
    )
