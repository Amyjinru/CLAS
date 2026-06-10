#!/usr/bin/env python3
"""CLAS 数据填充 — 使用正确的 API 路径"""
import urllib.request, json, sys, time

BASE = "http://127.0.0.1:8080/api"
CODE = "888888"
TOKENS = {}

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
        body = e.read().decode()[:200]
        print(f"  ❌ {method} {path}: HTTP {e.code} {body}")
        return None

def send_code(phone, scene="register"):
    return api("POST", "/user/register/send-code", {"phone": phone, "scene": scene})

def login_or_register(phone, username, role):
    r = api("POST", "/user/login", {"phone": phone, "password": "Abc123!"})
    if r and r.get("code") == 200:
        TOKENS[phone] = r["data"]["token"]
        return r["data"]["token"]
    send_code(phone)
    r = api("POST", "/user/register", {
        "phone": phone, "username": username, "password": "Abc123!",
        "confirmPassword": "Abc123!", "role": role, "code": CODE
    })
    if r and r.get("code") == 200:
        TOKENS[phone] = r["data"]["token"]
        return r["data"]["token"]
    return None

# ═══ Admin ═══
print(">>> Admin")
t = login_or_register("13800000003", "admin", "ADMIN")
ADMIN = t
print(f"  {'✅' if t else '❌'}")

# ═══ Step 1: 注册 10 个商家 ═══
print("\n>>> 商家账号")
for phone, name in [
    ("13800000010","老王"),("13800000011","张厨"),("13800000012","李师傅"),
    ("13800000013","赵老板"),("13800000014","孙掌柜"),("13800000015","周大厨"),
    ("13800000016","吴店长"),("13800000017","郑老板"),("13800000018","钱师傅"),
    ("13800000019","陈主厨"),
]:
    t = login_or_register(phone, name, "MERCHANT")
    print(f"  {'✅' if t else '❌'} {name}")

# ═══ Step 2: 创建店铺 ═══
print("\n>>> 创建店铺")
MPROFILES = [
    ("13800000010","老王黄焖鸡","美食","软件园南区食堂一层",200,1000,1800, 116.385, 39.905),
    ("13800000011","张厨酸菜鱼","美食","软件园北门美食街2号",300,1500,3200, 116.388, 39.908),
    ("13800000012","李记麻辣烫","美食","大学城步行街C区3号",150,800,1500, 116.390, 39.910),
    ("13800000013","赵氏奶茶店","饮品","图书馆一楼大厅",100,500,1200, 116.386, 39.906),
    ("13800000014","孙记烧烤","美食","学生公寓2号楼底商",200,1200,2500, 116.392, 39.912),
    ("13800000015","周氏面馆","美食","行政楼对面巷子口",200,800,1200, 116.384, 39.904),
    ("13800000016","吴记水果捞","饮品","体育馆西门旁",100,400,1800, 116.389, 39.907),
    ("13800000017","郑家煎饼果子","美食","教学楼B座门口",100,500,800, 116.387, 39.903),
    ("13800000018","钱氏日料","美食","商业街国际美食区1号",400,2000,4500, 116.391, 39.911),
    ("13800000019","陈记甜品站","饮品","大学生活动中心一层",100,500,1500, 116.385, 39.909),
]
# 路径: POST /api/merchant/register
for phone,name,cat,addr,dfee,mino,avg,lon,lat in MPROFILES:
    t = TOKENS.get(phone)
    if not t: continue
    r = api("POST","/merchant/register", {
        "merchantName":name,"category":cat,"address":addr,
        "deliveryFee":dfee,"minOrderPrice":mino,"averagePrice":avg,
        "bankAccount":"6222000000000000002","settlementCycle":7,
        "contactPhone":phone,
        "longitude":lon, "latitude":lat, "deliveryRadiusM":3000
    }, token=t)
    if r and r.get("code")==200: print(f"  ✅ {name}")
    else:
        msg = r.get("message","") if r else ""
        print(f"  ⚠️ {name}: {msg[:50]}")

# ═══ Step 3: 注册 10 个普通用户 ═══
print("\n>>> 普通用户")
for phone, name in [
    ("13800000101","小明"),("13800000102","小红"),("13800000103","阿强"),
    ("13800000104","小美"),("13800000105","大壮"),("13800000106","翠花"),
    ("13800000107","学霸张三"),("13800000108","运动达人李四"),
    ("13800000109","吃货王五"),("13800000110","宅男赵六"),
]:
    t = login_or_register(phone, name, "USER")
    if t: api("PUT","/user/profile",{"nickname":name}, token=t)
    print(f"  {'✅' if t else '❌'} {name}")
time.sleep(1)

# ═══ Step 4: 审核商家（PENDING → OPEN） ═══
print("\n>>> 审核商家")
# 用 admin API 列出所有商家（含 PENDING）
r = api("GET", "/merchant/admin/list?page=1&size=50", token=ADMIN)
mid_map = {}
if r and r.get("code") == 200:
    recs = r["data"].get("records", []) if isinstance(r["data"], dict) else r.get("data", [])
    for m in recs:
        mid = m["id"]
        mid_map[m["merchantName"]] = mid
        # 如果是 PENDING 状态，审批通过
        if m.get("status") == "PENDING":
            ar = api("POST", f"/merchant/admin/audit/{mid}", {"status": "APPROVED", "remarks": "系统自动审核通过"}, token=ADMIN)
            if ar and ar.get("code") == 200:
                # 再激活为 OPEN
                api("POST", f"/merchant/admin/audit/{mid}", {"status": "OPEN", "remarks": "自动激活营业"}, token=ADMIN)
                print(f"  ✅ {m['merchantName']} 审核→营业")
            else:
                print(f"  ⚠️ {m['merchantName']} 审核失败")
        elif m.get("status") == "APPROVED":
            api("POST", f"/merchant/admin/audit/{mid}", {"status": "OPEN", "remarks": "自动激活营业"}, token=ADMIN)
            print(f"  ✅ {m['merchantName']} 激活营业")
print(f"  共 {len(mid_map)} 家商家")

# ═══ Step 5: 添加菜品 ═══
print("\n>>> 添加菜品")
ALL_PRODUCTS = {
    "老王黄焖鸡": [("黄焖鸡米饭",1800,"招牌黄焖鸡配米饭"),("黄焖排骨饭",2200,"大块排骨慢炖入味"),("黄焖茄子饭",1500,"素菜经典"),("卤蛋",200,"秘制卤蛋"),("冰峰汽水",500,"陕西特产汽水")],
    "张厨酸菜鱼": [("招牌酸菜鱼",3800,"新鲜活鱼现杀"),("番茄鱼",3500,"酸甜番茄汤底"),("麻辣鱼",3800,"重辣爱好者必点"),("酸菜鱼面",2800,"酸菜鱼汤面"),("红糖糍粑",800,"手工现做")],
    "李记麻辣烫": [("自选麻辣烫(素)",1200,"任选6种素菜"),("自选麻辣烫(荤)",1800,"任选4荤4素"),("麻辣拌",1500,"干拌风味"),("冰粉",600,"手工冰粉")],
    "赵氏奶茶店": [("珍珠奶茶",1200,"Q弹珍珠"),("椰果奶茶",1200,"清爽椰果"),("芝士奶盖茶",1800,"浓郁芝士奶盖"),("柠檬红茶",1000,"鲜切柠檬"),("芒果冰沙",1500,"新鲜芒果"),("芋圆烧仙草",1400,"手工芋圆")],
    "孙记烧烤": [("烤羊肉串(10串)",2500,"新疆风味"),("烤鸡翅(5个)",1800,"蜜汁烤翅"),("烤茄子",1000,"蒜蓉烤茄子"),("烤韭菜",800,"秘制酱料")],
    "周氏面馆": [("兰州拉面",1200,"一清二白三红四绿"),("炸酱面",1400,"老北京炸酱"),("油泼面",1300,"陕西风味"),("牛肉面",1800,"大块牛肉")],
    "吴记水果捞": [("经典水果捞",2000,"8种水果配酸奶"),("芒果椰奶捞",2200,"泰国芒果+椰奶"),("草莓多多",2400,"新鲜草莓")],
    "郑家煎饼果子": [("经典煎饼果子",800,"加蛋+薄脆"),("豪华煎饼果子",1200,"双蛋+火腿+肉松"),("豆浆",300,"现磨豆浆"),("豆腐脑",500,"咸味豆腐脑")],
    "钱氏日料": [("三文鱼刺身",4800,"挪威三文鱼"),("加州卷(8枚)",3200,"经典寿司"),("豚骨拉面",2800,"日式拉面"),("天妇罗拼盘",3500,"炸虾+蔬菜")],
    "陈记甜品站": [("提拉米苏",2200,"意式经典"),("芒果千层",2500,"层层芒果"),("马卡龙(6枚)",3500,"法式小甜点"),("抹茶拿铁",1800,"日式抹茶")],
}
# 路径: POST /api/merchant/me/products
product_cache = {}
for mname, prods in ALL_PRODUCTS.items():
    mid = mid_map.get(mname)
    if not mid: continue
    # 找到商家对应的 token
    phone = [v[0] for v in MPROFILES if v[1] == mname]
    phone = phone[0] if phone else None
    t = TOKENS.get(phone)
    if not t: continue
    pmap = {}
    for name, price, desc in prods:
        r = api("POST","/merchant/me/products",{"name":name,"price":price,"description":desc,"stock":999}, token=t)
        if r and r.get("code")==200: pmap[name] = r["data"].get("id") if isinstance(r["data"],dict) else None
    product_cache[mid] = pmap
    print(f"  ✅ {mname}: {len(pmap)}个")

# ═══ Step 6: 创建团购 ═══
print("\n>>> 创建团购（POST /api/deals/merchant）")
DEALS = [
    ("老王黄焖鸡", "黄焖鸡双人套餐", "黄焖鸡×2+卤蛋×2", 4000, 2899, 100, 30),
    ("张厨酸菜鱼", "酸菜鱼3人餐", "招牌酸菜鱼+糍粑+米饭×3", 6800, 4999, 50, 30),
    ("李记麻辣烫", "麻辣烫畅吃卡", "8次畅吃荤素不限", 9600, 6999, 30, 60),
    ("赵氏奶茶店", "奶茶月卡", "30天每天1杯", 36000, 19999, 20, 30),
    ("孙记烧烤", "烧烤狂欢套餐", "烤串×20+烤翅×10", 8800, 5999, 40, 15),
    ("钱氏日料", "日料体验套餐", "刺身+寿司+天妇罗", 9800, 7999, 25, 30),
    ("陈记甜品站", "下午茶双人套餐", "提拉米苏×2+拿铁×2", 4400, 2999, 60, 30),
    ("周氏面馆", "工作餐月卡", "30天每天1碗", 30000, 16800, 30, 30),
]
for mname, title, desc, orig, deal, stock, days in DEALS:
    mid = mid_map.get(mname)
    if not mid: continue
    phone = [v[0] for v in MPROFILES if v[1] == mname][0]
    t = TOKENS.get(phone)
    if not t: continue
    r = api("POST","/deals/merchant",{"title":title,"description":desc,"originalPrice":orig,"dealPrice":deal,"stock":stock,"validDays":days}, token=t)
    print(f"  {'✅' if r and r.get('code')==200 else '⚠️'} {title}")

# ═══ Step 7: 创建外卖订单 ═══
print("\n>>> 创建外卖订单（POST /api/order/create）")
ORDERS = [
    ("13800000101","老王黄焖鸡",[("黄焖鸡米饭",1),("卤蛋",1)],"学生公寓1号楼301室"),
    ("13800000102","张厨酸菜鱼",[("招牌酸菜鱼",1),("红糖糍粑",1)],"女生公寓3号楼502室"),
    ("13800000103","李记麻辣烫",[("自选麻辣烫(荤)",1),("冰粉",1)],"研究生公寓A栋203"),
    ("13800000104","赵氏奶茶店",[("珍珠奶茶",2),("芝士奶盖茶",1)],"女生公寓5号楼101"),
    ("13800000105","孙记烧烤",[("烤羊肉串(10串)",1),("烤鸡翅(5个)",1)],"学生公寓2号楼601"),
    ("13800000101","周氏面馆",[("兰州拉面",1),("牛肉面",1)],"学生公寓1号楼301室"),
    ("13800000107","郑家煎饼果子",[("经典煎饼果子",1),("豆浆",2)],"教学楼B座"),
    ("13800000103","钱氏日料",[("豚骨拉面",1),("加州卷(8枚)",1)],"研究生公寓A栋203"),
    ("13800000108","陈记甜品站",[("提拉米苏",1),("抹茶拿铁",2)],"体育馆"),
    ("13800000109","老王黄焖鸡",[("黄焖排骨饭",1),("冰峰汽水",1)],"学生公寓"),
    ("13800000105","张厨酸菜鱼",[("麻辣鱼",1)],"学生公寓2号楼601"),
    ("13800000106","吴记水果捞",[("经典水果捞",1)],"学生公寓4号楼202"),
    ("13800000102","孙记烧烤",[("烤茄子",2),("烤韭菜",1)],"女生公寓3号楼502室"),
]
order_ids = []
for uid, mname, items, addr in ORDERS:
    t = TOKENS.get(uid)
    if not t: continue
    mid = mid_map.get(mname)
    if not mid: continue
    pmap = product_cache.get(mid, {})
    oitems = [{"productId":pmap[n],"quantity":q} for n,q in items if n in pmap]
    if not oitems: continue
    r = api("POST","/order/create",{"merchantId":mid,"items":oitems,"deliveryAddress":addr}, token=t)
    if r and r.get("code")==200:
        oid = r["data"].get("id") if isinstance(r["data"],dict) else None
        if oid: order_ids.append((uid, mname, oid))
        print(f"  ✅ {uid} → {mname} #{oid}")
    else:
        print(f"  ⚠️ {uid} → {mname}")

# ═══ Step 8: 添加收藏 ═══
print("\n>>> 添加收藏（POST /api/favorites/{merchantId}）")
for uid, mnames in [
    ("13800000101",["老王黄焖鸡","张厨酸菜鱼","赵氏奶茶店","周氏面馆"]),
    ("13800000102",["张厨酸菜鱼","孙记烧烤","郑家煎饼果子"]),
    ("13800000103",["李记麻辣烫","钱氏日料","郑家煎饼果子"]),
    ("13800000104",["赵氏奶茶店","陈记甜品站","吴记水果捞"]),
    ("13800000105",["孙记烧烤","周氏面馆","老王黄焖鸡"]),
]:
    t = TOKENS.get(uid)
    if not t: continue
    for mn in mnames:
        mid = mid_map.get(mn)
        if mid: api("POST",f"/favorites/{mid}", token=t)
    print(f"  ✅ {uid} → {len(mnames)}家")

# ═══ Step 9: 创建到店预约 ═══
print("\n>>> 到店预约（POST /api/bookings）")
for uid, mname, svc, tm, note in [
    ("13800000101","老王黄焖鸡","午餐预约","2026-06-12 12:00:00","2人"),
    ("13800000102","张厨酸菜鱼","晚餐聚会","2026-06-12 18:30:00","4人"),
    ("13800000103","钱氏日料","下午茶","2026-06-13 15:00:00","1人"),
    ("13800000104","郑家煎饼果子","早餐外带","2026-06-13 07:30:00","打包"),
    ("13800000105","孙记烧烤","夜宵","2026-06-12 22:00:00","6人"),
]:
    t = TOKENS.get(uid)
    if not t: continue
    mid = mid_map.get(mname)
    if not mid: continue
    r = api("POST","/bookings",{"merchantId":mid,"serviceName":svc,"appointmentTime":tm,"contactPhone":uid,"note":note}, token=t)
    print(f"  {'✅' if r and r.get('code')==200 else '⚠️'} {uid} → {mname}")

# ═══ Step 10: 购买团购 ═══
print("\n>>> 购买团购（POST /api/deals/{id}/buy）")
# 获取团购列表
for uid in ["13800000101","13800000102","13800000103","13800000104"]:
    t = TOKENS.get(uid)
    if not t: continue
    r = api("GET","/deals/mine", token=t)
    if not r or r.get("code")!=200: continue
    deals = r.get("data",[])
    # 买列表中的前几个团购
    for d in deals[:3] if isinstance(deals,list) else []:
        did = d.get("dealId") or d.get("id")
        if did:
            api("POST",f"/deals/{did}/buy", token=t)
    print(f"  ✅ {uid} 已购买团购")

# ═══ Step 11: 添加评价 ═══
print("\n>>> 评价（POST /api/review/add）")
reviews_map = [
    ("老王黄焖鸡",5,"黄焖鸡味道绝了！分量足，鸡肉嫩滑入味，推荐！"),
    ("张厨酸菜鱼",4,"酸菜鱼汤底浓郁，鱼片新鲜。配菜可以再多一点。"),
    ("李记麻辣烫",5,"自选食材很新鲜，麻辣味够劲！每次必点。"),
    ("赵氏奶茶店",4,"奶茶好喝，珍珠Q弹。高峰期要等很久。"),
    ("孙记烧烤",5,"深夜烧烤太爽了！羊肉串配啤酒绝配。"),
    ("周氏面馆",4,"面很劲道，汤也鲜美，就是量少了一点。"),
    ("郑家煎饼果子",5,"料足实惠，早餐首选！煎饼果子好吃。"),
    ("钱氏日料",5,"三文鱼很新鲜，日料品质值得这个价钱。"),
    ("陈记甜品站",4,"提拉米苏好吃，马卡龙略甜。环境不错。"),
]
for i, (mname, score, content) in enumerate(reviews_map):
    if i >= len(order_ids): break
    uid, oname, oid = order_ids[i]
    # 确保评价的商家和订单的商家一致
    if oname != mname: continue
    t = TOKENS.get(uid)
    if not t: continue
    r = api("POST","/review/add",{"orderId":oid,"score":score,"content":content}, token=t)
    print(f"  {'✅' if r and r.get('code')==200 else '⚠️'} {uid} 评 {mname} #{oid} {score}⭐")

print("\n" + "="*60)
print("  🎉 数据填充完成!")
print(f"  商家: {len(mid_map)}家 | 菜品: {sum(len(v) for v in ALL_PRODUCTS.values())}个")
print("="*60)
