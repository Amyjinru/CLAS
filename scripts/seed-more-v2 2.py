#!/usr/bin/env python3
"""CLAS — 增加商家、用户、菜品"""
import urllib.request, json, random, time

BASE = "http://127.0.0.1:8080/api"
CODE = "888888"

def api(method, path, data=None, token=None):
    url = f"{BASE}{path}"
    h = {"Content-Type": "application/json"}
    if token: h["Authorization"] = f"Bearer {token}"
    body = json.dumps(data).encode() if data else None
    req = urllib.request.Request(url, data=body, headers=h, method=method)
    try:
        return json.loads(urllib.request.urlopen(req).read())
    except urllib.error.HTTPError as e:
        msg = e.read().decode()[:120]
        if e.code not in [400]: print(f"  ERR {method} {path}: {e.code} {msg}")
        return None

def login(phone):
    r = api("POST", "/user/login", {"phone": phone, "password": "Abc123!"})
    return r["data"]["token"] if r and r.get("code") == 200 else None

def send_code(phone):
    api("POST", "/user/register/send-code", {"phone": phone})

def register_user(phone, name, role):
    send_code(phone)
    r = api("POST", "/user/register", {
        "phone": phone, "username": name, "password": "Abc123!",
        "confirmPassword": "Abc123!", "role": role, "code": CODE
    })
    if r and r.get("code") == 200:
        return r["data"]["token"]
    return login(phone)  # already exists

# ═══ Admin ═══
admin = login("13800000003")
if not admin: print("Admin fail"); exit(1)
print("Admin ✅")

# ═══════════════════════════════════════
# 1. 新增 15 个商家 + 账号
# ═══════════════════════════════════════
print("\n>>> 新商家")
NEW_MERCHANTS = [
    ("13800000040", "刘记盖饭", "美食", "学生食堂二楼A区", 200, 1200, 1600, 116.383, 39.905),
    ("13800000041", "杨氏米粉", "美食", "商业街小吃巷1号", 150, 800, 1200, 116.387, 39.908),
    ("13800000042", "朱家烧烤", "美食", "北门外夜市一条街", 200, 1000, 2200, 116.390, 39.911),
    ("13800000043", "马记拉面", "美食", "图书馆负一层美食广场", 200, 900, 1300, 116.384, 39.903),
    ("13800000044", "韩式炸鸡", "美食", "学生公寓5号楼底商", 300, 1200, 2400, 116.391, 39.912),
    ("13800000045", "林记包子铺", "美食", "教学楼C座门口", 100, 400, 600, 116.386, 39.904),
    ("13800000046", "何记瓦罐汤", "美食", "教师食堂隔壁", 200, 800, 1500, 116.385, 39.906),
    ("13800000047", "罗氏烘焙", "饮品", "大学生活动中心二楼", 100, 500, 1800, 116.382, 39.901),
    ("13800000048", "梁记咖啡馆", "饮品", "图书馆二楼咖啡角", 100, 500, 1500, 116.380, 39.900),
    ("13800000049", "宋氏冰品", "饮品", "体育馆南门", 100, 400, 1000, 116.388, 39.907),
    ("13800000050", "唐记冒菜", "美食", "研究生公寓楼下", 250, 1000, 1800, 116.393, 39.913),
    ("13800000051", "许记粥铺", "美食", "校医院隔壁", 100, 500, 800, 116.381, 39.902),
    ("13800000052", "邓记卤味", "美食", "西门小吃街中段", 200, 900, 2000, 116.379, 39.899),
    ("13800000053", "冯记火锅", "美食", "北门外商业广场2楼", 400, 2000, 4500, 116.394, 39.914),
    ("13800000054", "曹记披萨", "美食", "国际交流中心一楼", 350, 1500, 3200, 116.378, 39.898),
]

merchant_phones = []
for phone, name, cat, addr, dfee, mo, avg, lon, lat in NEW_MERCHANTS:
    t = register_user(phone, name[:2], "MERCHANT")
    if not t:
        print(f"  ❌ {name}")
        continue
    merchant_phones.append(phone)
    r = api("POST", "/merchant/register", {
        "merchantName": name, "category": cat, "address": addr,
        "deliveryFee": dfee, "minOrderPrice": mo, "averagePrice": avg,
        "bankAccount": f"6222{random.randint(100000000,999999999):09d}",
        "settlementCycle": 7, "contactPhone": phone,
        "longitude": lon, "latitude": lat, "deliveryRadiusM": 5000
    }, token=t)
    if r and r.get("code") == 200: print(f"  ✅ {name}")
    else:
        msg = r.get("message","") if r else "err"
        print(f"  ⚠️ {name}: {msg[:50]}")

# ═══ 审核新商家 ═══
print("\n>>> 审核商家")
time.sleep(1)
r = api("GET", "/merchant/admin/list?page=1&size=50", token=admin)
all_merchants = r["data"] if isinstance(r["data"], list) else r["data"].get("records", [])
for m in all_merchants:
    if m.get("status") == "PENDING":
        mid = m["id"]
        api("POST", f"/merchant/admin/audit/{mid}", {"status": "APPROVED", "remarks": "自动审核"}, token=admin)
        api("POST", f"/merchant/admin/audit/{mid}", {"status": "OPEN", "remarks": "激活营业"}, token=admin)
        print(f"  ✅ {m['merchantName']} 已激活")
print(f"  共 {len(all_merchants)} 家")

# ═══════════════════════════════════════
# 2. 新增 10 个普通用户
# ═══════════════════════════════════════
print("\n>>> 新用户")
new_users = []
for i in range(60, 70):
    phone = f"13800000{i:03d}"
    names = ["小徐","小胡","小林","小高","小罗","小梁","小宋","小唐","小许","小邓"]
    name = names[i-60]
    t = register_user(phone, name, "USER")
    if t:
        new_users.append((phone, t))
        print(f"  ✅ {name}")
time.sleep(1)

# ═══ 给新用户添加地址 ═══
print("\n>>> 收货地址")
for phone, token in new_users:
    api("POST", "/address", {
        "contactName": f"同学{phone[-4:]}", "phone": phone,
        "address": f"学生公寓{random.randint(1,8)}号楼{random.randint(101,699)}室",
        "isDefault": True,
        "longitude": 116.38 + random.uniform(-0.01, 0.01),
        "latitude": 39.90 + random.uniform(-0.01, 0.01)
    }, token=token)
print(f"  ✅ {len(new_users)} 条")

# ═══════════════════════════════════════
# 3. 为新商家添加菜品
# ═══════════════════════════════════════
print("\n>>> 新菜品")
PRODUCTS = {
    "刘记盖饭": [
        ("红烧肉盖饭", 1600), ("宫保鸡丁盖饭", 1500), ("鱼香肉丝盖饭", 1400),
        ("糖醋里脊盖饭", 1800), ("番茄鸡蛋盖饭", 1200), ("回锅肉盖饭", 1700),
    ],
    "杨氏米粉": [
        ("招牌牛肉米粉", 1400), ("酸辣米粉", 1200), ("三鲜米粉", 1300),
        ("螺蛳粉", 1500), ("干拌米粉", 1100),
    ],
    "朱家烧烤": [
        ("烤羊肉串(10串)", 2500), ("烤牛肉串(10串)", 2800), ("烤鸡翅(5个)", 1800),
        ("烤鱿鱼", 1500), ("蒜蓉生蚝(6只)", 2200), ("烤玉米", 800),
    ],
    "马记拉面": [
        ("招牌牛肉拉面", 1500), ("刀削面", 1400), ("炒拉条", 1600),
        ("大盘鸡拌面", 2200), ("羊肉泡馍", 2000),
    ],
    "韩式炸鸡": [
        ("原味炸鸡(半只)", 2400), ("甜辣炸鸡(半只)", 2600), ("蜂蜜黄油炸鸡", 2800),
        ("芝士年糕", 1200), ("韩国泡菜", 800),
    ],
    "林记包子铺": [
        ("鲜肉包(4个)", 800), ("三鲜包(4个)", 900), ("菜包(4个)", 600),
        ("豆浆", 300), ("小米粥", 400),
    ],
    "何记瓦罐汤": [
        ("排骨瓦罐汤", 1800), ("乌鸡瓦罐汤", 2000), ("老鸭汤", 1600),
        ("冬瓜排骨汤", 1500), ("菌菇汤", 1200),
    ],
    "罗氏烘焙": [
        ("法式可颂", 1200), ("巧克力慕斯", 1800), ("草莓蛋糕", 2000),
        ("蛋挞(4个)", 1000), ("全麦面包", 800),
    ],
    "梁记咖啡馆": [
        ("美式咖啡", 1500), ("拿铁", 1800), ("卡布奇诺", 2000),
        ("抹茶拿铁", 2200), ("冰博客dirty", 2500),
    ],
    "宋氏冰品": [
        ("芒果冰沙", 1200), ("西瓜冰", 1000), ("椰奶冰淇淋", 1500),
        ("草莓圣代", 1400), ("冰镇酸梅汤", 600),
    ],
    "唐记冒菜": [
        ("素冒菜(小份)", 1200), ("荤冒菜(小份)", 1800), ("荤冒菜(大份)", 2800),
        ("冒烤鸭", 3000), ("冰粉", 600),
    ],
    "许记粥铺": [
        ("皮蛋瘦肉粥", 800), ("南瓜小米粥", 600), ("海鲜粥", 1200),
        ("八宝粥", 700), ("绿豆粥", 500),
    ],
    "邓记卤味": [
        ("卤鸡腿(2个)", 1200), ("卤鸭脖(5根)", 1500), ("卤猪蹄", 1800),
        ("卤豆腐", 600), ("卤蛋(3个)", 500),
    ],
    "冯记火锅": [
        ("麻辣火锅底料(2人份)", 2800), ("番茄火锅底料(2人份)", 2500),
        ("肥牛卷", 3200), ("虾滑", 2800), ("毛肚", 3000),
    ],
    "曹记披萨": [
        ("玛格丽特披萨(9寸)", 3200), ("至尊披萨(9寸)", 3800),
        ("夏威夷披萨(9寸)", 3500), ("芝士条", 1500), ("鸡翅(6个)", 1800),
    ],
}

total_products = 0
for mname, prods in PRODUCTS.items():
    phone = None
    for p, n, *_ in NEW_MERCHANTS:
        if n == mname:
            phone = p
            break
    if not phone: continue
    t = login(phone)
    if not t: continue
    for name, price in prods:
        r = api("POST", "/merchant/me/products", {
            "name": name, "price": price, "stock": 999, "description": name
        }, token=t)
        if r and r.get("code") == 200: total_products += 1
    print(f"  ✅ {mname}: {len(prods)}个")

print(f"\n{'='*50}")
print(f"  新增: {len(merchant_phones)}商家 | {len(new_users)}用户 | {total_products}菜品")
print(f"{'='*50}")
