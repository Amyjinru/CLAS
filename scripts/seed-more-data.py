#!/usr/bin/env python3
"""CLAS 补充种子数据 — 更多用户、订单、评价"""
import urllib.request, json, time, random

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
        print(f"  ERR {method} {path}: {e.code}")
        return None
    except Exception as e:
        print(f"  ERR {method} {path}: {e}")
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
    return None

def ensure_user(phone, name, role):
    t = login(phone)
    if t: return t
    return register_user(phone, name, role)

# ═══ Admin ═══
print(">>> Admin login")
admin = login("13800000003") or register_user("13800000003", "admin", "ADMIN")
if not admin: print("FATAL"); exit(1)
print("  ✅")

# ═══ 注册更多普通用户 ═══
print("\n>>> 注册 20 个新用户")
new_users = []
for i in range(20, 40):
    phone = f"13800000{i:03d}"
    names = ["小张","小李","小王","小陈","小刘","小杨","小黄","小赵","小周","小吴",
             "阿花","阿杰","阿琳","阿龙","阿芳","阿明","阿华","阿宝","阿星","阿乐"]
    name = names[i-20]
    t = ensure_user(phone, name, "USER")
    if t:
        new_users.append((phone, t))
        api("PUT", "/user/profile", {"nickname": name}, token=t)
        print(f"  ✅ {name} ({phone})")
    else:
        print(f"  ❌ {name}")

# ═══ 给新用户添加地址 ═══
print("\n>>> 添加收货地址")
addrs = [
    "学生公寓1号楼", "学生公寓2号楼", "学生公寓3号楼", "学生公寓4号楼",
    "研究生公寓A栋", "研究生公寓B栋", "教师公寓1栋", "教师公寓2栋",
    "图书馆一楼", "教学楼A座", "实验楼B座", "行政楼大厅",
    "体育馆入口", "食堂西侧", "操场北门", "校医院旁边",
    "创业园一楼", "国际交流中心", "计算机学院楼", "经管学院楼"
]
for i, (phone, token) in enumerate(new_users):
    addr = addrs[i % len(addrs)]
    room = random.randint(101, 699)
    api("POST", "/address", {
        "contactName": f"同学{phone[-4:]}", "phone": phone,
        "address": f"{addr}{room}室", "isDefault": True,
        "longitude": 116.38 + random.uniform(-0.008, 0.008),
        "latitude": 39.90 + random.uniform(-0.008, 0.008)
    }, token=token)
print(f"  ✅ {len(new_users)} 条地址")

# ═══ 获取商家和产品 ═══
print("\n>>> 获取商家产品")
r = api("GET", "/merchant/admin/list?page=1&size=50", token=admin)
merchants = []
if r and r.get("code") == 200:
    d = r["data"]
    merchants = d if isinstance(d, list) else d.get("records", [])
    print(f"  {len(merchants)} 家商家")

# 确保所有商家营业时间为 06:00-23:30
for m in merchants:
    phone = m.get("userId") or m.get("phone")
    if phone and phone in ["13800000002"]: continue  # skip demo
    t = login(phone)
    if not t: continue
    api("PUT", "/merchant/my/profile", {
        "merchantName": m["merchantName"],
        "businessHours": "06:00-23:30",
        "address": m.get("address",""),
        "phone": m.get("phone", phone),
        "bankAccount": m.get("bankAccount", "6222000000000000002"),
        "settlementCycle": 7,
        "longitude": 116.38 + random.uniform(-0.01, 0.01),
        "latitude": 39.90 + random.uniform(-0.01, 0.01),
        "deliveryRadiusM": 5000,
        "code": CODE
    }, token=t)
print("  ✅ 商家时间和配送范围已更新")

# 获取产品
product_cache = {}
for m in merchants:
    mid = m["id"]
    r = api("GET", f"/product/list/{mid}")
    if r and r.get("code") == 200:
        prods = r.get("data", []) or []
        pmap = {}
        for p in prods:
            nm = p.get("productName") or p.get("name", "")
            pmap[nm] = p.get("id")
        product_cache[mid] = pmap

# ═══ 创建大量订单 ═══
print("\n>>> 创建外卖订单")
all_tokens = {p: t for p, t in new_users}
for old_phone in ["13800000101","13800000102","13800000103","13800000104",
                  "13800000105","13800000106","13800000107","13800000108",
                  "13800000109","13800000110"]:
    t = login(old_phone)
    if t: all_tokens[old_phone] = t

user_phones = list(all_tokens.keys())
order_count = 0
addr_list = ["学生公寓1号楼","学生公寓2号楼","学生公寓3号楼","学生公寓4号楼",
             "研究生公寓A栋","研究生公寓B栋","教师公寓1栋","教师公寓2栋"]

for round_num in range(2):
    for m in merchants:
        mid = m["id"]
        pmap = product_cache.get(mid, {})
        if len(pmap) < 2: continue
        buyers = random.sample(user_phones, min(2, len(user_phones)))
        for phone in buyers:
            t = all_tokens[phone]
            prods = random.sample(list(pmap.items()), min(random.randint(1, 2), len(pmap)))
            # 1) 清空购物车
            api("DELETE", "/cart/me", token=t)
            # 2) 加入购物车
            for pname, pid in prods:
                api("POST", "/cart/add", {"productId": pid, "quantity": random.randint(1, 2)}, token=t)
            # 3) 下单
            addr = random.choice(addr_list)
            r = api("POST", "/order/create", {
                "merchantId": mid,
                "deliveryAddress": f"{addr}{random.randint(101,699)}室"
            }, token=t)
            if r and r.get("code") == 200: order_count += 1
    print(f"  轮 {round_num+1}: {order_count} 笔")

print(f"  ✅ 共 {order_count} 笔订单")

# ═══ 添加收藏 ═══
print("\n>>> 添加收藏")
fav_count = 0
for phone, token in all_tokens.items():
    fav_merchants = random.sample(merchants, min(random.randint(2, 5), len(merchants)))
    for m in fav_merchants:
        r = api("POST", f"/favorites/{m['id']}", token=token)
        if r and r.get("code") == 200: fav_count += 1
print(f"  ✅ {fav_count} 条收藏")

# ═══ 商家接单并完成 ═══
print("\n>>> 商家接单完成")
for m in merchants:
    phone = m.get("userId") or m.get("phone")
    if not phone: continue
    t = login(phone)
    if not t: continue
    r = api("GET", "/order/merchant/me", token=t)
    if not r or r.get("code") != 200: continue
    orders = r.get("data", [])
    for ow in (orders or [])[:5]:
        o = ow.get("order", ow)
        oid = o.get("id")
        if not oid: continue
        api("POST", f"/order/accept/{oid}", token=t)
        api("POST", f"/order/complete/{oid}", token=t)
print("  ✅")

# ═══ 添加评价 ═══
print("\n>>> 添加评价")
reviews_done = 0
contents = [
    "非常好吃！分量足！", "味道不错，还会再来。", "送餐很快，包装完好。",
    "性价比很高，推荐！", "很满意！", "味道正宗！", "新鲜好吃！",
    "五星好评！", "物美价廉！", "服务态度好！", "非常满意！"
]
for phone, token in all_tokens.items():
    r = api("GET", "/order/me", token=token)
    if not r or r.get("code") != 200: continue
    orders = r.get("data", [])
    for order_wrap in orders[:1]:
        oid = order_wrap.get("order", {}).get("id") if isinstance(order_wrap, dict) else order_wrap.get("id")
        if not oid: continue
        rr = api("POST", "/review/add", {
            "orderId": oid, "score": random.randint(4, 5), "content": random.choice(contents)
        }, token=token)
        if rr and rr.get("code") == 200: reviews_done += 1
print(f"  ✅ {reviews_done} 条评价")

# ═══ 购买团购 ═══
print("\n>>> 购买团购")
deal_count = 0
# 获取团购列表
r = api("GET", "/merchant/list?page=1&size=50")
if r and r.get("code") == 200:
    deals_list = r.get("data", {}).get("records", []) if isinstance(r.get("data"), dict) else r.get("data", [])
    # 通过 GET /deals 公共接口获取团购
    r2 = api("GET", "/deals?page=1&size=50")
    if r2 and r2.get("code") == 200:
        deals = r2.get("data", {}).get("records", []) if isinstance(r2.get("data"), dict) else r2.get("data", [])
        for phone, token in all_tokens.items():
            if deals:
                d = random.choice(deals)
                did = d.get("id")
                if did:
                    rr = api("POST", f"/deals/{did}/buy", token=token)
                    if rr and rr.get("code") == 200: deal_count += 1
print(f"  ✅ {deal_count} 笔团购")

print(f"\n{'='*50}")
print(f"  补充数据完成!")
print(f"  新用户: {len(new_users)} 人")
print(f"  订单: {order_count} 笔")
print(f"  收藏: {fav_count} 条")
print(f"  评价: {reviews_done} 条")
print(f"  团购: {deal_count} 笔")
print(f"{'='*50}")
