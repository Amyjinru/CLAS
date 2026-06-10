#!/usr/bin/env python3
import urllib.request, json
BASE = "http://127.0.0.1:8080/api"

def api(method, path, data=None, token=None):
    url = f"{BASE}{path}"
    h = {"Content-Type": "application/json"}
    if token: h["Authorization"] = f"Bearer {token}"
    body = json.dumps(data).encode() if data else None
    req = urllib.request.Request(url, data=body, headers=h, method=method)
    try:
        r = json.loads(urllib.request.urlopen(req).read())
        return r
    except urllib.error.HTTPError as e:
        body = e.read().decode()[:150]
        print(f"  ERR {method} {path}: {body}")
        return None

# 1. Admin login
r = api("POST", "/user/login", {"phone": "13800000003", "password": "Abc123!"})
admin = r["data"]["token"] if r and r.get("code") == 200 else None
if not admin:
    print("Admin login failed"); exit(1)
print("Admin OK")

# 2. Get all merchants
r = api("GET", "/merchant/admin/list?page=1&size=50", token=admin)
mid_map = {}
if r and r.get("code") == 200:
    data = r["data"]
    if isinstance(data, list):
        recs = data
    elif isinstance(data, dict) and "records" in data:
        recs = data["records"]
    else:
        recs = []
    for m in recs:
        mid_map[m.get("merchantName", "")] = m.get("id")
print(f"Merchants: {len(mid_map)}")

# 3. Update merchant business hours
coords = {
    "老王黄焖鸡": (116.385, 39.905), "张厨酸菜鱼": (116.388, 39.908),
    "李记麻辣烫": (116.390, 39.910), "赵氏奶茶店": (116.386, 39.906),
    "孙记烧烤": (116.392, 39.912), "周氏面馆": (116.384, 39.904),
    "吴记水果捞": (116.389, 39.907), "郑家煎饼果子": (116.387, 39.903),
    "钱氏日料": (116.391, 39.911), "陈记甜品站": (116.385, 39.909),
}
MP = [
    ("13800000010", "老王黄焖鸡", "软件园南区食堂一层", "13800000010", "6222000000000000010"),
    ("13800000011", "张厨酸菜鱼", "软件园北门美食街2号", "13800000011", "6222000000000000011"),
    ("13800000012", "李记麻辣烫", "大学城步行街C区3号", "13800000012", "6222000000000000012"),
    ("13800000013", "赵氏奶茶店", "图书馆一楼大厅", "13800000013", "6222000000000000013"),
    ("13800000014", "孙记烧烤", "学生公寓2号楼底商", "13800000014", "6222000000000000014"),
    ("13800000015", "周氏面馆", "行政楼对面巷子口", "13800000015", "6222000000000000015"),
    ("13800000016", "吴记水果捞", "体育馆西门旁", "13800000016", "6222000000000000016"),
    ("13800000017", "郑家煎饼果子", "教学楼B座门口", "13800000017", "6222000000000000017"),
    ("13800000018", "钱氏日料", "商业街国际美食区1号", "13800000018", "6222000000000000018"),
    ("13800000019", "陈记甜品站", "大学生活动中心一层", "13800000019", "6222000000000000019"),
]
for phone, name, addr, cphone, bank in MP:
    r = api("POST", "/user/login", {"phone": phone, "password": "Abc123!"})
    if not r or r.get("code") != 200:
        print(f"  Login failed: {name}")
        continue
    token = r["data"]["token"]
    # Send verification code for profile update
    api("POST", "/merchant/my/profile/send-code", {"phone": phone}, token=token)
    lon, lat = coords.get(name, (116.38, 39.90))
    r2 = api("PUT", "/merchant/my/profile", {
        "merchantName": name, "businessHours": "06:00-23:30",
        "address": addr, "phone": cphone, "bankAccount": bank,
        "settlementCycle": 7, "longitude": lon, "latitude": lat,
        "deliveryRadiusM": 3000, "code": "888888"
    }, token=token)
    print(f"  {'OK' if r2 and r2.get('code')==200 else 'FAIL'} {name}")

# 4. Get products
print("Products:")
product_cache = {}
for mname, mid in mid_map.items():
    r = api("GET", f"/product/list/{mid}")
    if r and r.get("code") == 200:
        prods = r.get("data", []) or []
        pmap = {}
        for p in prods:
            nm = p.get("productName") or p.get("name", "")
            pmap[nm] = p.get("id")
        product_cache[mid] = pmap
        if pmap:
            print(f"  {mname}: {len(pmap)}")

# 5. Login users
print("Users:")
users = {}
for uid in ["13800000101","13800000102","13800000103","13800000104",
            "13800000105","13800000106","13800000107","13800000108",
            "13800000109","13800000110"]:
    r = api("POST", "/user/login", {"phone": uid, "password": "Abc123!"})
    if r and r.get("code") == 200:
        users[uid] = r["data"]["token"]

# 6. Create orders
print("Orders:")
ORDERS = [
    ("13800000101", "老王黄焖鸡", [("黄焖鸡米饭",1),("卤蛋",1)], "学生公寓1号楼301室"),
    ("13800000102", "张厨酸菜鱼", [("招牌酸菜鱼",1),("红糖糍粑",1)], "女生公寓3号楼502室"),
    ("13800000103", "李记麻辣烫", [("自选麻辣烫(荤)",1),("冰粉",1)], "研究生公寓A栋203"),
    ("13800000104", "赵氏奶茶店", [("珍珠奶茶",2),("芝士奶盖茶",1)], "女生公寓5号楼101"),
    ("13800000105", "孙记烧烤", [("烤羊肉串(10串)",1),("烤鸡翅(5个)",1)], "学生公寓2号楼601"),
    ("13800000101", "周氏面馆", [("兰州拉面",1),("牛肉面",1)], "学生公寓1号楼301室"),
    ("13800000107", "郑家煎饼果子", [("经典煎饼果子",1),("豆浆",2)], "教学楼B座"),
    ("13800000103", "钱氏日料", [("豚骨拉面",1),("加州卷(8枚)",1)], "研究生公寓A栋203"),
    ("13800000108", "陈记甜品站", [("提拉米苏",1),("抹茶拿铁",2)], "体育馆"),
    ("13800000109", "老王黄焖鸡", [("黄焖排骨饭",1),("冰峰汽水",1)], "学生公寓"),
    ("13800000105", "张厨酸菜鱼", [("麻辣鱼",1)], "学生公寓2号楼601"),
    ("13800000106", "吴记水果捞", [("经典水果捞",1)], "学生公寓4号楼202"),
    ("13800000102", "孙记烧烤", [("烤茄子",2),("烤韭菜",1)], "女生公寓3号楼502室"),
]
order_ids = []
for uid, mname, items, addr in ORDERS:
    t = users.get(uid)
    if not t: continue
    mid = mid_map.get(mname)
    if not mid: continue
    pmap = product_cache.get(mid, {})
    oitems = [{"productId": pmap[n], "quantity": q} for n, q in items if n in pmap]
    if not oitems: continue
    r = api("POST", "/order/create",
            {"merchantId": mid, "items": oitems, "deliveryAddress": addr}, token=t)
    if r and r.get("code") == 200:
        d = r.get("data")
        oid = d.get("id") if isinstance(d, dict) else None
        if oid:
            order_ids.append((uid, mname, oid))
            print(f"  OK {mname} #{oid}")
    else:
        print(f"  FAIL {mname}")

# 7. Reviews
print(f"Reviews ({len(order_ids)} orders):")
reviews_text = [
    "味道绝了！分量足，推荐！", "酸菜鱼汤底浓郁，鱼片新鲜。",
    "食材新鲜，麻辣味够劲！", "好喝，珍珠Q弹。",
    "深夜烧烤太爽了！", "面很劲道，汤也鲜美。",
    "料足实惠，早餐首选！", "三文鱼很新鲜！",
    "提拉米苏好吃！", "黄焖排骨也很好吃。",
]
for i, (uid, mname, oid) in enumerate(order_ids[:10]):
    t = users.get(uid)
    if not t: continue
    content = reviews_text[i] if i < len(reviews_text) else "非常满意！"
    r = api("POST", "/review/add",
            {"orderId": oid, "score": 5, "content": content}, token=t)
    print(f"  {'OK' if r and r.get('code')==200 else 'FAIL'} #{oid}")

print(f"\n=== 完成 === 商家:{len(mid_map)} 订单:{len(order_ids)} ===")
