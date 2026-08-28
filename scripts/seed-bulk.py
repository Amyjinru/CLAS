#!/usr/bin/env python3
"""CLAS 批量造数据 — 商家100+ / 用户300+ / 菜品300+"""
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
        r = json.loads(urllib.request.urlopen(req).read())
        if r.get("code") != 200 and r.get("code") != 400:
            pass  # silently continue
        return r
    except:
        return None

def login(phone):
    for pw in ["Aa123456!", "Abc123!"]:
        r = api("POST", "/user/login", {"phone": phone, "password": pw})
        if r and r.get("code") == 200: return r["data"]["token"]
    return None

def send_code(phone):
    api("POST", "/user/register/send-code", {"phone": phone})

def register_user(phone, name, role):
    send_code(phone)
    r = api("POST", "/user/register", {
        "phone": phone, "username": name, "password": "Aa123456!",
        "confirmPassword": "Aa123456!", "role": role, "code": CODE
    })
    if r and r.get("code") == 200:
        return r["data"]["token"]
    return login(phone)

# ═══ Admin ═══
# Admin 用旧密码 Abc123!
r = api("POST", "/user/login", {"phone": "13800000003", "password": "Abc123!"})
admin = r["data"]["token"] if r and r.get("code") == 200 else None
if not admin: print("Admin fail"); exit(1)
print("Admin ✅")

# ═══════════════════════════════════════════
# 1. 批量注册 75 个商家 → 总计 101
# ═══════════════════════════════════════════
print("\n>>> 批量注册 75 家商家")
CATEGORIES = ["美食", "美食", "美食", "饮品", "饮品", "美食", "美食", "美食", "饮品", "美食"]
SURNAMES = "赵钱孙李周吴郑王冯陈褚卫蒋沈韩杨朱秦尤许何吕施张孔曹严华金魏陶姜戚谢邹喻柏水窦章云苏潘葛奚范彭郎鲁韦昌马苗凤花方俞任袁柳酆鲍史唐费廉岑薛雷贺倪汤滕殷罗毕郝邬安常乐于时傅皮下齐康伍余元卜顾孟平黄和穆萧尹姚邵湛汪祁毛禹狄米贝明臧计伏成戴谈宋茅庞熊纪舒屈项祝董梁"
FOOD_NAMES = ["盖饭","米粉","面馆","烧烤","炸鸡","包子","瓦罐汤","冒菜","粥铺","卤味","火锅","披萨","麻辣烫","酸菜鱼","黄焖鸡","奶茶","寿司","甜品","饺子","炒饭","拌面","煎饼","拉面","米线","钵钵鸡","烤鱼","汉堡","烤鸭","煲仔饭","砂锅"]
DRINK_NAMES = ["咖啡","茶饮","冰品","奶昔","果汁","酸奶","糖水","冰淇淋","柠檬茶","果茶"]
STREETS = ["学生食堂一楼主厅","学生食堂二楼主厅","商业街美食区","北门外夜市","西门小吃街",
           "图书馆美食广场","体育馆北侧","教学楼楼下","研究生公寓底商","教师公寓旁",
           "东门商业街","南门小吃巷","行政楼对面","操场西侧","创业园一楼"]

merchant_phones = []
mcount = 0
for i in range(75):
    phone = f"13800001{i:03d}" if i < 60 else f"13800002{i-60:03d}"
    if i < 60 and phone in ["13800001000","13800001001","13800001002"]: continue  # skip existing
    surname = SURNAMES[i % len(SURNAMES)]
    cat = random.choice(CATEGORIES)
    name_pool = FOOD_NAMES if cat == "美食" else DRINK_NAMES
    mname = f"{surname}记{random.choice(name_pool)}"
    addr = random.choice(STREETS)
    dfee = random.choice([100,150,200,250,300])
    mo = random.choice([400,500,600,800,1000])
    avg = mo + random.choice([200,400,600,800,1000,1500])
    lon = 116.37 + random.uniform(-0.02, 0.02)
    lat = 39.89 + random.uniform(-0.015, 0.015)

    t = register_user(phone, surname, "MERCHANT")
    if not t: continue
    r = api("POST", "/merchant/register", {
        "merchantName": mname, "category": cat, "address": addr,
        "deliveryFee": dfee, "minOrderPrice": mo, "averagePrice": avg,
        "bankAccount": f"6222{random.randint(100000000,999999999):09d}",
        "settlementCycle": 7, "contactPhone": phone,
        "longitude": lon, "latitude": lat, "deliveryRadiusM": 5000
    }, token=t)
    if r and r.get("code") == 200:
        merchant_phones.append(phone)
        mcount += 1
        if mcount % 15 == 0: print(f"  {mcount}/75...")
time.sleep(1)

# ═══ 审核所有商家 ═══
print("\n>>> 审核")
r = api("GET", "/merchant/admin/list?page=1&size=200", token=admin)
all_m = r["data"] if isinstance(r["data"], list) else r["data"].get("records", [])
audit_count = 0
for m in all_m:
    if m.get("status") == "PENDING":
        mid = m["id"]
        api("POST", f"/merchant/admin/audit/{mid}", {"status": "APPROVED", "remarks": "系统自动审核"}, token=admin)
        api("POST", f"/merchant/admin/audit/{mid}", {"status": "OPEN", "remarks": "激活"}, token=admin)
        audit_count += 1
print(f"  {audit_count} 家审核通过 | 共 {len(all_m)} 家")

# ═══════════════════════════════════════════
# 2. 给所有新商家加菜品（每家 3-6 个）
# ═══════════════════════════════════════════
print("\n>>> 添加菜品")
FOOD_PRODUCTS = [
    ("招牌套餐A", 1500), ("招牌套餐B", 1800), ("经典单人餐", 1200),
    ("双人优惠餐", 2800), ("豪华全家桶", 4500), ("小份尝鲜装", 800),
    ("加量不加价", 1400), ("今日特惠", 1000), ("主厨推荐", 2200),
    ("学生优惠餐", 900), ("超级大份", 3200), ("迷你小份", 600),
]
DRINK_PRODUCTS = [
    ("招牌奶茶", 1200), ("经典咖啡", 1500), ("鲜榨果汁", 1400),
    ("芝士奶盖", 1800), ("冰镇柠檬茶", 1000), ("芒果冰沙", 1600),
    ("草莓多多", 1400), ("椰奶冻", 1300), ("抹茶拿铁", 2000),
]
prod_count = 0
for i, m in enumerate(all_m):
    phone = m.get("userId") or m.get("phone")
    if not phone: continue
    t = login(phone)
    if not t: continue
    cat = m.get("category", "美食")
    pool = FOOD_PRODUCTS if cat == "美食" else DRINK_PRODUCTS
    for name, price in random.sample(pool, min(random.randint(3, 6), len(pool))):
        r = api("POST", "/merchant/me/products", {
            "name": name, "price": price, "stock": 999,
            "description": f"{m['merchantName']} {name}"
        }, token=t)
        if r and r.get("code") == 200: prod_count += 1
    if (i+1) % 20 == 0: print(f"  {i+1}/{len(all_m)} 商家, {prod_count} 菜品...")
print(f"  ✅ {prod_count} 个菜品")

# ═══════════════════════════════════════════
# 3. 批量注册 230+ 个普通用户
# ═══════════════════════════════════════════
print("\n>>> 批量注册 235 个用户")
GIVEN_NAMES = ["伟","芳","娜","敏","静","丽","强","磊","洋","勇",
               "军","杰","娟","艳","涛","明","超","秀","霞","平",
               "刚","桂","文","华","飞","玉","春","红","斌","辉",
               "鑫","鹏","璐","琳","晨","宁","欣","帅","凯","宇",
               "浩","然","博","毅","恒","思","瑞","嘉","悦","莹"]
ucount = 0
for i in range(235):
    phone = f"139{i:08d}" if i < 100 else f"136{i-100:08d}"
    if i >= 200:
        phone = f"137{i-200:08d}"
    name = f"{random.choice(SURNAMES)}{random.choice(GIVEN_NAMES)}"
    t = register_user(phone, name, "USER")
    if t: ucount += 1
    if ucount % 50 == 0: print(f"  {ucount}/235...")
print(f"  ✅ {ucount} 个用户")

time.sleep(1)
# 给部分用户添加地址
print("\n>>> 收货地址")
addr_count = 0
for i in range(235):
    phone = f"139{i:08d}" if i < 100 else f"136{i-100:08d}"
    if i >= 200: phone = f"137{i-200:08d}"
    t = login(phone)
    if not t: continue
    api("POST", "/address", {
        "contactName": f"用户{phone[-4:]}", "phone": phone,
        "address": f"学生公寓{random.randint(1,12)}号楼{random.randint(101,899)}室",
        "isDefault": True,
        "longitude": 116.37 + random.uniform(-0.02, 0.02),
        "latitude": 39.89 + random.uniform(-0.015, 0.015)
    }, token=t)
    addr_count += 1
    if addr_count % 50 == 0: print(f"  {addr_count}/235...")
print(f"  ✅ {addr_count} 条地址")

# ═══ Final Stats ═══
time.sleep(1)
r = api("GET", "/api/public/stats")
if r and r.get("code") == 200:
    d = r["data"]
    print(f"\n{'='*50}")
    print(f"  🏪 商家: {d['merchants']}")
    print(f"  👥 用户: {d['users']}")
    print(f"  🍔 菜品: {d['products']}")
    print(f"{'='*50}")
