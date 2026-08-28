#!/usr/bin/env python3
"""CLAS 补齐所有缺失数据 + 达到目标"""
import urllib.request, json, random, time

BASE = "http://127.0.0.1:8080/api"
CODE = "888888"

def api(method, path, data=None, token=None):
    url = f"{BASE}{path}"
    h = {"Content-Type": "application/json"}
    if token: h["Authorization"] = f"Bearer {token}"
    body = json.dumps(data).encode() if data else None
    req = urllib.request.Request(url, data=body, headers=h, method=method)
    try: return json.loads(urllib.request.urlopen(req).read())
    except: return None

def login(phone):
    for pw in ["Aa123456!", "Abc123!"]:
        r = api("POST", "/user/login", {"phone": phone, "password": pw})
        if r and r.get("code") == 200: return r["data"]["token"]
    return None

def reg_user(phone, name, role):
    api("POST", "/user/register/send-code", {"phone": phone})
    r = api("POST", "/user/register", {
        "phone": phone, "name": name, "username": name, "password": "Aa123456!",
        "confirmPassword": "Aa123456!", "role": role, "code": CODE
    })
    if r and r.get("code") == 200: return r["data"]["token"]
    return login(phone)

admin = api("POST", "/user/login", {"phone": "13800000003", "password": "Abc123!"})
admin = admin["data"]["token"] if admin else None
if not admin: print("Admin fail"); exit(1)
print("Admin ✅")

# ═══ 1. 加2商家到100+ ═══
print("\n>>> +2 商家")
for i, (phone, name, cat, addr) in enumerate([
    ("13800000200", "蔡记快餐", "美食", "东门商业街尽头"),
    ("13800000201", "潘记茶餐厅", "饮品", "南门广场二楼"),
]):
    t = reg_user(phone, name[:2], "MERCHANT")
    if t:
        r = api("POST", "/merchant/register", {
            "merchantName": name, "category": cat, "address": addr,
            "deliveryFee": 200, "minOrderPrice": 800, "averagePrice": 1500,
            "bankAccount": "6222000000000000200", "settlementCycle": 7,
            "contactPhone": phone, "longitude": 116.38 + random.uniform(-0.01, 0.01),
            "latitude": 39.90 + random.uniform(-0.01, 0.01), "deliveryRadiusM": 5000
        }, token=t)
        if r and r.get("code") == 200:
            mid = r["data"]["id"] if isinstance(r["data"], dict) else None
            if mid:
                api("POST", f"/merchant/admin/audit/{mid}", {"status": "APPROVED"}, token=admin)
                api("POST", f"/merchant/admin/audit/{mid}", {"status": "OPEN"}, token=admin)
            # Add products
            for pn, pp in [("招牌快餐A",1500),("招牌快餐B",1800),("学生特惠餐",900),("大份套餐",2200)]:
                api("POST", "/merchant/me/products", {"name": pn, "price": pp, "stock": 999}, token=t)
            print(f"  ✅ {name}")
time.sleep(1)

# ═══ 2. 评价到 500+ ═══
print("\n>>> 评价 (目标500+)")
# Get all user tokens
user_tokens = []
for i in range(20, 300):
    phone = f"13800000{i:03d}" if i < 100 else f"139{i-100:08d}" if i < 200 else f"136{i-200:08d}" if i < 300 else f"137{i-300:08d}"
    t = login(phone)
    if t: user_tokens.append((phone, t))
print(f"  {len(user_tokens)} 用户可用")

review_count = 0
# Get orders that are completed for each user
for phone, token in random.sample(user_tokens, min(150, len(user_tokens))):
    r = api("GET", "/order/me", token=token)
    if not r or r.get("code") != 200: continue
    for ow in (r.get("data", []) or [])[:1]:
        o = ow.get("order", ow)
        oid = o.get("id")
        if not oid: continue
        # Pay if needed
        st = o.get("status", "")
        if st == "PENDING_PAYMENT":
            api("POST", f"/order/pay/{oid}", token=token)
        # Try review anyway
        rr = api("POST", "/review/add", {
            "orderId": oid,
            "score": random.randint(3, 5),
            "content": random.choice([
                "非常好吃！推荐！", "不错，会回购。", "味道正宗！",
                "送餐很快！", "性价比高！", "满意！", "棒！",
                "好吃不贵！", "值得推荐！", "很喜欢！",
                "份量足！", "味道好！", "新鲜！", "赞！",
            ])
        }, token=token)
        if rr and rr.get("code") == 200: review_count += 1
    if review_count % 50 == 0 and review_count > 0: print(f"  {review_count}...")
print(f"  ✅ {review_count} 条评价")

# ═══ 3. 评论到 200+ (reply to reviews) ═══
print("\n>>> 评论 (目标200+)")
reply_count = 0
for phone, token in random.sample(user_tokens, min(100, len(user_tokens))):
    r = api("GET", "/review/mine", token=token)
    if not r or r.get("code") != 200: continue
    revs = r.get("data", [])
    if isinstance(revs, dict): revs = revs.get("records", [])
    for rv in (revs or [])[:2]:
        rid = rv.get("id")
        if not rid: continue
        rr = api("POST", f"/review/{rid}/comments", {"content": random.choice(["赞同！","同感！","说得好！","确实！","+1","支持！","好评价！","没错！"])}, token=token)
        if rr and rr.get("code") == 200: reply_count += 1
    if reply_count % 50 == 0 and reply_count > 0: print(f"  {reply_count}...")
print(f"  ✅ {reply_count} 条评论")

# ═══ 4. 取消到 100+ ═══
print("\n>>> 取消订单 (目标100+)")
cancel_count = 0
for phone, token in random.sample(user_tokens, min(80, len(user_tokens))):
    r = api("GET", "/order/me", token=token)
    if not r or r.get("code") != 200: continue
    for ow in (r.get("data", []) or [])[:1]:
        o = ow.get("order", ow)
        oid = o.get("id")
        st = o.get("status", "")
        if not oid or st in ["COMPLETED", "CANCELED", "REJECTED"]: continue
        rr = api("POST", f"/order/cancel/{oid}", token=token)
        if rr and rr.get("code") == 200: cancel_count += 1
    if cancel_count % 20 == 0 and cancel_count > 0: print(f"  {cancel_count}...")
print(f"  ✅ {cancel_count} 笔取消")

# ═══ 5. 投诉/举报 ═══
print("\n>>> 举报")
report_count = 0
for phone, token in random.sample(user_tokens, min(50, len(user_tokens))):
    r = api("GET", "/review/mine", token=token)
    if not r or r.get("code") != 200: continue
    revs = r.get("data", [])
    if isinstance(revs, dict): revs = revs.get("records", [])
    for rv in (revs or [])[:1]:
        rid = rv.get("id")
        if not rid: continue
        rr = api("POST", f"/review/{rid}/report", {"reportReason": "内容不当"}, token=token)
        if rr and rr.get("code") == 200: report_count += 1
print(f"  ✅ {report_count} 条举报")

# ═══ 6. 系统公告 ═══
print("\n>>> 系统公告")
anc_count = 0
for title, content in [
    ("欢迎使用CLAS平台", "欢迎各位同学！CLAS校园生活服务平台正式上线，外卖、团购、到店预约一站式服务。"),
    ("618校园美食节", "6月18日-6月25日，全场团购8折，外卖配送费全免！快来参与！"),
    ("平台安全提示", "请勿向任何人透露您的账号密码和验证码。如遇可疑订单，请立即联系客服。"),
    ("新增商家公告", "本周新增刘记盖饭、杨氏米粉等15家商家，品类更丰富，选择更多样！"),
    ("系统升级通知", "平台将于6月20日凌晨2:00-4:00进行系统升级，届时部分功能可能暂时不可用。"),
]:
    r = api("POST", "/announcement/create", {"title": title, "content": content}, token=admin)
    if r and r.get("code") == 200: anc_count += 1
print(f"  ✅ {anc_count} 条公告")

# ═══ 7. 优惠券 ═══
print("\n>>> 优惠券")
coupon_count = 0
for i in range(5):
    r = api("POST", "/coupon/claim/1", token=admin)  # Try creating coupons
    # This might need a different endpoint, skip if not working
    break
# Just note: coupon creation might need admin API

# ═══ 8. 聊天消息 ═══
print("\n>>> 聊天消息")
chat_count = 0
for phone, token in random.sample(user_tokens, min(20, len(user_tokens))):
    r = api("POST", "/chat/consult/1", {"content": random.choice(["你好","在吗","有什么推荐？","今天有什么优惠？","多久能送到？","能开发票吗？"])}, token=token)
    if r and r.get("code") == 200: chat_count += 1
print(f"  ✅ {chat_count} 条聊天")

# ═══ Final ═══
print(f"\n{'='*50}")
print(f"  评价: {review_count} | 评论: {reply_count} | 取消: {cancel_count}")
print(f"  举报: {report_count} | 公告: {anc_count} | 聊天: {chat_count}")
print(f"{'='*50}")
