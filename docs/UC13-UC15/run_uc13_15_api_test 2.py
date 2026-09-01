#!/usr/bin/env python3
"""UC13-UC15 production API smoke tests. Output JSON summary."""
import json
import urllib.error
import urllib.request
from datetime import datetime, timezone

BASE = "http://8.141.112.182"
ACCOUNTS = {
    "user": ("13800000001", "Abc123!"),
    "merchant": ("13800000002", "Abc123!"),
    "admin": ("13800000003", "Abc123!"),
}


def req(method, path, token=None, body=None, expect_json=True):
    url = f"{BASE}{path}"
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = json.dumps(body).encode() if body is not None else None
    r = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(r, timeout=15) as resp:
            raw = resp.read().decode("utf-8", errors="replace")
            ct = resp.headers.get("Content-Type", "")
            if expect_json and "json" in ct:
                return resp.status, json.loads(raw), raw[:500]
            return resp.status, None, raw[:500]
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", errors="replace")
        try:
            return e.code, json.loads(raw), raw[:500]
        except json.JSONDecodeError:
            return e.code, None, raw[:500]


def login(role):
    phone, password = ACCOUNTS[role]
    status, data, _ = req("POST", "/api/user/login", body={"phone": phone, "password": password})
    if status != 200 or not data or data.get("code") != 200:
        raise RuntimeError(f"login {role} failed: status={status} body={data}")
    return data["data"]["token"]


def record(results, uc, test_id, name, passed, detail):
    results.append({
        "uc": uc, "id": test_id, "name": name, "passed": passed, "detail": detail
    })
    flag = "PASS" if passed else "FAIL"
    print(f"[{flag}] {test_id} {name} :: {detail}")


def main():
    results = []
    user_t = login("user")
    merchant_t = login("merchant")
    admin_t = login("admin")

    # UC13
    s, data, _ = req("GET", "/api/announcement/list")
    record(results, "UC13", "UC13-API-01", "公共公告列表",
           s == 200 and data and data.get("code") == 200 and isinstance(data.get("data"), list),
           f"status={s} count={len(data.get('data') or []) if data else 0}")

    s, data, _ = req("POST", "/api/announcement/create", user_t,
                     {"title": "机测-用户越权", "content": "应失败"})
    record(results, "UC13", "UC13-API-02", "非ADMIN创建公告拒绝",
           s in (401, 403), f"status={s}")

    s, data, _ = req("POST", "/api/announcement/create", admin_t,
                     {"title": "机测公告", "content": "UC13自动化"})
    record(results, "UC13", "UC13-API-03", "ADMIN创建公告",
           s == 200 and data and data.get("code") == 200 and data.get("data", {}).get("title") == "机测公告",
           f"status={s} title={data.get('data', {}).get('title') if data else None}")

    s, data, _ = req("GET", "/api/announcement/admin/list", admin_t)
    record(results, "UC13", "UC13-API-04", "ADMIN查看全部公告",
           s == 200 and data and data.get("code") == 200, f"status={s}")

    # UC14
    s, data, _ = req("GET", "/api/admin/users?page=1&size=5", admin_t)
    pwd_plaintext = False
    pwd_null_field = False
    if data and isinstance(data.get("data"), dict):
        recs = data["data"].get("records") or []
        for r in recs:
            if not r:
                continue
            val = r.get("password")
            if val:
                pwd_plaintext = True
            elif "password" in r:
                pwd_null_field = True
    record(results, "UC14", "UC14-API-01", "ADMIN用户列表无明文password",
           s == 200 and data and data.get("code") == 200 and not pwd_plaintext,
           f"status={s} plaintext={pwd_plaintext} null_field={pwd_null_field}")

    s, _, raw = req("GET", "/api/admin/export/orders", admin_t, expect_json=False)
    record(results, "UC14", "UC14-API-02", "ADMIN导出orders CSV",
           s == 200 and "订单ID" in raw, f"status={s} head={raw[:80]!r}")

    s, _, raw = req("GET", "/api/admin/export/users", user_t, expect_json=False)
    record(results, "UC14", "UC14-API-03", "USER导出users被拒绝",
           s in (401, 403), f"status={s}")

    s, data, _ = req("GET", "/api/admin/export/reviews", admin_t, expect_json=False)
    record(results, "UC14", "UC14-API-04", "ADMIN导出reviews CSV",
           s == 200, f"status={s}")

    # UC15
    s, data, _ = req("GET", "/api/admin/dashboard", admin_t)
    d = (data or {}).get("data") or {}
    fields_ok = all(k in d for k in ["totalUsers", "totalMerchants", "totalOrders", "totalSales"])
    record(results, "UC15", "UC15-API-01", "ADMIN dashboard统计字段",
           s == 200 and data and data.get("code") == 200 and fields_ok,
           f"status={s} keys={list(d.keys())[:6]}")

    s, data, _ = req("GET", "/api/admin/dashboard", user_t)
    record(results, "UC15", "UC15-API-02", "USER访问dashboard拒绝",
           s in (401, 403), f"status={s}")

    s, data, _ = req("GET", "/api/admin/stats/orders", admin_t)
    record(results, "UC15", "UC15-API-03", "ADMIN订单统计",
           s == 200 and data and data.get("code") == 200, f"status={s}")

    s, data, _ = req("GET", "/api/merchant/my/stats", merchant_t)
    ms = (data or {}).get("data") or {}
    mfields = all(k in ms for k in ["todayOrders", "todaySales", "totalSales", "dailySales"])
    record(results, "UC15", "UC15-API-04", "MERCHANT本店统计",
           s == 200 and data and data.get("code") == 200 and mfields,
           f"status={s} todaySales={ms.get('todaySales')}")

    s, data, _ = req("GET", "/api/public/stats")
    pub = (data or {}).get("data") or {}
    pub_ok = all(k in pub for k in ["merchants", "products", "users"])
    record(results, "UC15", "UC15-API-05", "公开平台统计",
           s == 200 and data and data.get("code") == 200 and pub_ok,
           f"status={s} data={pub}")

    summary = {
        "runAt": datetime.now(timezone.utc).isoformat(),
        "baseUrl": BASE,
        "total": len(results),
        "passed": sum(1 for r in results if r["passed"]),
        "failed": sum(1 for r in results if not r["passed"]),
        "results": results,
    }
    out = r"d:\HuaweiMoveData\Users\troub\Desktop\CLAS 1.2\CLAS\docs\use-cases\uc13-15-api-results.json"
    with open(out, "w", encoding="utf-8") as f:
        json.dump(summary, f, ensure_ascii=False, indent=2)
    print(f"\nSUMMARY: {summary['passed']}/{summary['total']} passed -> {out}")
    return 0 if summary["failed"] == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
