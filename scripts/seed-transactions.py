#!/usr/bin/env python3
"""CLAS 批量交易数据 — 订单/团购/评价/举报/取消"""
import urllib.request, json, random, time, sys

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
        return None  # silent skip
    except: return None

def login(phone):
    for pw in ["Aa123456!", "Abc123!"]:
        r = api("POST", "/user/login", {"phone": phone, "password": pw})
        if r and r.get("code") == 200: return r["data"]["token"]
    return None

# ═══ Admin ═══
admin = api("POST", "/user/login", {"phone": "13800000003", "password": "Abc123!"})
admin = admin["data"]["token"] if admin else None
if not admin: print("Admin fail"); sys.exit(1)
print("Admin ✅")

# ═══ 获取全部商家和产品 ═══
print("\n>>> 获取商家")
r = api("GET", "/merchant/admin/list?page=1&size=200", token=admin)
merchants = r["data"] if isinstance(r["data"], list) else r["data"].get("records", [])
print(f"  {len(merchants)} 家")

product_cache = {}
for i, m in enumerate(merchants):
    mid = m["id"]
    r = api("GET", f"/product/list/{mid}")
    if r and r.get("code") == 200:
        prods = r.get("data", []) or []
        pmap = {}
        for p in prods:
            nm = p.get("productName") or p.get("name", "")
            if p.get("id"): pmap[nm] = p["id"]
        if pmap:
            product_cache[mid] = pmap
            product_cache[mid, "phone"] = m.get("userId") or m.get("phone")
print(f"  {len(product_cache)} 家有产品")

# ═══ 获取 100 个用户 token ═══
print("\n>>> 获取用户 tokens")
user_tokens = []
sample_users = list(range(20, 40)) + list(range(60, 70)) + list(range(101, 200))
for i in sample_users[:100]:
    phone = f"13800000{i:03d}" if i < 100 else f"1380000{i:04d}"
    if i >= 100 and i < 1100:
        phone = f"139{i-100:08d}"
    elif i >= 110:
        phone = f"136{i-110:08d}"
    # Try a range
    for base in ["13800000", "13800001", "13800002"]:
        p = f"{base}{i%100:03d}"
        t = login(p)
        if t:
            user_tokens.append((p, t))
            break
    if i >= 100:
        p = f"139{i-100:08d}"
        t = login(p)
        if t: user_tokens.append((p, t))
    if i >= 200:
        p = f"136{i-200:08d}"
        t = login(p)
        if t: user_tokens.append((p, t))
    if i >= 300:
        p = f"137{i-300:08d}"
        t = login(p)
        if t: user_tokens.append((p, t))
print(f"  {len(user_tokens)} 个用户")

if len(user_tokens) < 5:
    print("Not enough users!"); sys.exit(1)

# ═══════════════════════════════════════
# 1. 大量外卖订单
# ═══════════════════════════════════════
print("\n>>> 创建外卖订单")
order_ids = []
ADDRS = [f"学生公寓{x}号楼{random.randint(101,899)}室" for x in range(1,13)]
for round_num in range(3):
    random.shuffle(user_tokens)
    for phone, token in user_tokens[:60]:  # 60 users per round
        # Pick random merchant with products
        valid = [(mid, pmap) for mid, pmap in product_cache.items() if isinstance(mid, int) and len(pmap) >= 2]
        if not valid: continue
        mid, pmap = random.choice(valid)
        prods = random.sample(list(pmap.items()), min(random.randint(1, 3), len(pmap)))
        # Clear cart → add items → create order
        api("DELETE", "/cart/me", token=token)
        for _, pid in prods:
            api("POST", "/cart/add", {"productId": pid, "quantity": random.randint(1, 3)}, token=token)
        r = api("POST", "/order/create", {
            "merchantId": mid, "deliveryAddress": random.choice(ADDRS)
        }, token=token)
        if r and r.get("code") == 200:
            oid = r["data"]["order"]["id"] if isinstance(r["data"], dict) and "order" in r["data"] else None
            if oid: order_ids.append(oid)
    print(f"  轮{round_num+1}: {len(order_ids)} 笔")

# ═══ 支付 + 商家接单 + 完成 ═══
print("\n>>> 支付+接单+完成")
completed_ids = []
# Pay for orders — save (user_token, orderId) pairs during creation
# Re-create order ids with user info
ordered_pairs = []  # (phone, token, orderId)
# Create new batch where we track user-owner
for _ in range(2):
    for phone, token in random.sample(user_tokens, min(50, len(user_tokens))):
        valid = [(mid, pmap) for mid, pmap in product_cache.items() if isinstance(mid, int) and len(pmap) >= 2]
        if not valid: continue
        mid, pmap = random.choice(valid)
        prods = random.sample(list(pmap.items()), min(random.randint(1, 2), len(pmap)))
        api("DELETE", "/cart/me", token=token)
        for _, pid in prods:
            api("POST", "/cart/add", {"productId": pid, "quantity": random.randint(1, 2)}, token=token)
        r = api("POST", "/order/create", {"merchantId": mid, "deliveryAddress": f"学生公寓{random.randint(1,12)}号楼{random.randint(101,899)}室"}, token=token)
        if r and r.get("code") == 200:
            oid = r["data"]["order"]["id"]
            ordered_pairs.append((phone, token, oid))
            order_ids.append(oid)

# Now pay with the right user
pay_count = 0
for phone, token, oid in ordered_pairs[:80]:
    r = api("POST", f"/order/pay/{oid}", token=token)
    if r and r.get("code") == 200: pay_count += 1
print(f"  支付: {pay_count} 笔")

# Now accept (merchant) + complete (user)
print(f"  待处理: {len(ordered_pairs[:80])} 笔")
for phone, token, oid in ordered_pairs[:80]:
    # Get order details to find merchant
    r = api("GET", f"/order/merchant/{oid}", token=token)  # won't work, try different approach
    # Just try accepting with a random merchant token since we know the merchant is in the order
    # Actually, need to match order → merchant → accept. Let's iterate merchants.
    pass

# Better approach: merchant accepts all pending orders
for m in random.sample(merchants, min(20, len(merchants))):
    mphone = m.get("userId") or m.get("phone")
    if not mphone: continue
    t = login(mphone)
    if not t: continue
    r = api("GET", "/order/merchant/me", token=t)
    if not r or r.get("code") != 200: continue
    orders = r.get("data", [])
    for ow in (orders or [])[:5]:
        o = ow.get("order", ow)
        oid = o.get("id")
        uid = o.get("userId")
        if not oid: continue
        # Merchant accepts
        r1 = api("POST", f"/order/accept/{oid}", token=t)
        if r1 and r1.get("code") == 200:
            # USER completes (need user's token)
            ut = login(uid)
            if ut:
                r2 = api("POST", f"/order/complete/{oid}", token=ut)
                if r2 and r2.get("code") == 200:
                    completed_ids.append(oid)
print(f"  完成: {len(completed_ids)} 笔")

for m in random.sample(merchants, min(25, len(merchants))):
    phone = m.get("userId") or m.get("phone")
    if not phone: continue
    t = login(phone)
    if not t: continue
    r = api("GET", "/order/merchant/me", token=t)
    if not r or r.get("code") != 200: continue
    orders = r.get("data", [])
    for ow in (orders or [])[:10]:
        o = ow.get("order", ow)
        oid = o.get("id")
        if not oid: continue
        r1 = api("POST", f"/order/accept/{oid}", token=t)
        if r1 and r1.get("code") == 200:
            r2 = api("POST", f"/order/complete/{oid}", token=t)
            if r2 and r2.get("code") == 200:
                completed_ids.append(oid)
print(f"  ✅ {len(completed_ids)} 笔已完成")

# ═══ 部分取消订单 ═══
print("\n>>> 取消订单")
cancel_count = 0
for phone, token in random.sample(user_tokens, min(30, len(user_tokens))):
    r = api("GET", "/order/me", token=token)
    if not r or r.get("code") != 200: continue
    for ow in (r.get("data", []) or [])[:1]:
        o = ow.get("order", ow)
        oid = o.get("id")
        st = o.get("status")
        if not oid or st in ["COMPLETED", "CANCELED", "REJECTED"]: continue
        rr = api("POST", f"/order/cancel/{oid}", token=token)
        if rr and rr.get("code") == 200: cancel_count += 1
print(f"  ✅ {cancel_count} 笔取消")

# ═══ 2. 购买团购 ═══
print("\n>>> 购买团购")
r = api("GET", "/merchant/list?page=1&size=app")
deal_count = 0
# Get deals via merchant public API... try different paths
for path in ["/deals?page=1&size=200", "/api/deals?page=1&size=200"]:
    try:
        r2 = api("GET", path)
        if r2 and r2.get("code") == 200:
            deals = r2.get("data", [])
            if isinstance(deals, dict): deals = deals.get("records", [])
            for phone, token in random.sample(user_tokens, min(80, len(user_tokens))):
                if not deals: break
                d = random.choice(deals)
                did = d.get("id")
                if did:
                    r3 = api("POST", f"/deals/{did}/buy", token=token)
                    if r3 and r3.get("code") == 200: deal_count += 1
            break
    except: pass
print(f"  ✅ {deal_count} 笔团购")

# ═══ 3. 添加评价 ═══
print("\n>>> 添加评价")
review_contents = [
    "味道超赞！分量很足！", "非常好吃，推荐大家试试。", "送餐快，包装好。",
    "性价比很高，会回购。", "味道正宗，很满意！", "不错不错，支持！",
    "稍微咸了点但整体不错。", "新鲜！好吃！", "五星好评！",
    "朋友推荐的，果然不错。", "等了有点久但值得。", "很好吃！下次还来！",
    "今日最佳！", "物超所值！", "强烈推荐！"
]
review_count = 0
for phone, token in random.sample(user_tokens, min(60, len(user_tokens))):
    r = api("GET", "/order/me", token=token)
    if not r or r.get("code") != 200: continue
    orders = r.get("data", [])
    for ow in (orders or [])[:1]:
        o = ow.get("order", ow)
        oid = o.get("id")
        if not oid: continue
        r2 = api("POST", "/review/add", {
            "orderId": oid, "score": random.randint(3, 5), "content": random.choice(review_contents)
        }, token=token)
        if r2 and r2.get("code") == 200: review_count += 1
print(f"  ✅ {review_count} 条评价")

# ═══ 4. 添加举报 ═══
print("\n>>> 添加举报")
report_count = 0
for phone, token in random.sample(user_tokens, min(30, len(user_tokens))):
    r = api("GET", "/review/mine", token=token)
    if not r or r.get("code") != 200: continue
    reviews = r.get("data", [])
    if isinstance(reviews, dict): reviews = reviews.get("records", [])
    for rv in (reviews or [])[:1]:
        rid = rv.get("id")
        if not rid: continue
        r2 = api("POST", f"/review/{rid}/report", {"reportReason": "内容不当"}, token=token)
        if r2 and r2.get("code") == 200: report_count += 1
print(f"  ✅ {report_count} 条举报")

# ═══ 5. 创建到店预约 ═══
print("\n>>> 到店预约")
booking_count = 0
for phone, token in random.sample(user_tokens, min(40, len(user_tokens))):
    mid = random.choice([m["id"] for m in merchants])
    r = api("POST", "/bookings", {
        "merchantId": mid,
        "serviceName": random.choice(["午餐","晚餐","下午茶","聚会","团建"]),
        "appointmentTime": f"2026-06-{random.randint(13,30):02d}T{random.randint(10,20):02d}:00:00",
        "contactPhone": phone,
        "note": f"{random.randint(1,8)}人"
    }, token=token)
    if r and r.get("code") == 200: booking_count += 1
print(f"  ✅ {booking_count} 个预约")

# ═══ Final ═══
print(f"\n{'='*50}")
print(f"  📦 订单: {len(order_ids)} 笔 ({len(completed_ids)} 已完成)")
print(f"  🎫 团购: {deal_count} 笔")
print(f"  ⭐ 评价: {review_count} 条")
print(f"  🚩 举报: {report_count} 条")
print(f"  📅 预约: {booking_count} 个")
print(f"{'='*50}")
