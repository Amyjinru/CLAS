#!/usr/bin/env python3
import re
import sys
import urllib.request

host = sys.argv[1] if len(sys.argv) > 1 else "8.141.112.182"
base = f"http://{host}"
index = urllib.request.urlopen(f"{base}/", timeout=20).read().decode("utf-8", errors="replace")
index_match = re.search(r"/assets/index-[^\"']+\.js", index)
if not index_match:
    print("index bundle not found")
    sys.exit(1)
index_js = urllib.request.urlopen(base + index_match.group(0), timeout=20).read().decode("utf-8", errors="replace")
home_matches = re.findall(r"HomeView-[A-Za-z0-9_-]+\.js", index_js)
if not home_matches:
    print("HomeView chunk not referenced in index bundle")
    sys.exit(1)
home_asset = "/assets/" + home_matches[0]
home_js = urllib.request.urlopen(base + home_asset, timeout=20).read().decode("utf-8", errors="replace")
print("HomeView asset:", home_asset)
print("has_relocate_text=", "重新定位" in home_js or "\\u91cd\\u65b0\\u5b9a\\u4f4d" in home_js)
print("has_location_bar=", "location-bar" in home_js)
