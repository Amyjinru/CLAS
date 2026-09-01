#!/usr/bin/env python3
"""
为演示环境批量写入：多用户不等量收藏 + 好评（必要时自动走完下单闭环）。

用法:
  python scripts/seed_demo_favorites_reviews.py
  python scripts/seed_demo_favorites_reviews.py --base-url http://8.141.112.182
  python scripts/seed_demo_favorites_reviews.py --register-code 888888
"""
from __future__ import annotations

import argparse
import json
import sys
import urllib.error
import urllib.request
import uuid
from typing import Any

DEFAULT_BASE = "http://8.141.112.182"
DEFAULT_PASSWORD = "Abc123!"
DEMO_USER_PHONES = [
    "13800000001",
    "13800000004",
    "13800000005",
    "13800000006",
    "13800000007",
]
DEMO_USER_NAMES = ["user", "demo_user_a", "demo_user_b", "demo_user_c", "demo_user_d"]
POSITIVE_REVIEWS = [
    (5, "出餐很快，包装也很用心，味道超出预期，会继续回购。"),
    (5, "食材新鲜，分量足，配送准时，整体体验非常好。"),
    (5, "店铺环境在线，商品和图片一致，性价比高。"),
    (4, "整体满意，口味不错，下次还会点。"),
    (5, "服务态度好，餐品热乎，推荐给同学。"),
    (4, "味道稳定，配送员也很礼貌，体验不错。"),
    (5, "第一次点就被圈粉了，招牌商品很值得尝试。"),
]


class ApiClient:
    def __init__(self, base_url: str, dry_run: bool = False) -> None:
        self.base_url = base_url.rstrip("/")
        self.dry_run = dry_run
        self.tokens: dict[str, str] = {}

    def _request(
        self,
        method: str,
        path: str,
        *,
        token: str | None = None,
        body: dict | None = None,
        extra_headers: dict | None = None,
        allow_business_error: bool = False,
    ) -> Any:
        url = f"{self.base_url}{path}"
        headers = {"Content-Type": "application/json", "Accept": "application/json"}
        if token:
            headers["Authorization"] = f"Bearer {token}"
        if extra_headers:
            headers.update(extra_headers)
        data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
        if self.dry_run and method != "GET":
            print(f"[dry-run] {method} {path} body={body}")
            return {"code": 200, "data": {}}
        req = urllib.request.Request(url, data=data, headers=headers, method=method)
        try:
            with urllib.request.urlopen(req, timeout=45) as resp:
                payload = json.loads(resp.read().decode("utf-8"))
        except urllib.error.HTTPError as exc:
            raw = exc.read().decode("utf-8", errors="replace")
            try:
                payload = json.loads(raw)
            except json.JSONDecodeError:
                raise RuntimeError(f"{method} {path} HTTP {exc.code}: {raw}") from exc
            message = payload.get("message", raw)
            if allow_business_error:
                return payload
            raise RuntimeError(f"{method} {path}: {message}")
        if payload.get("code") != 200 and not allow_business_error:
            raise RuntimeError(f"{method} {path}: {payload.get('message')}")
        return payload

    def login(self, phone: str, password: str) -> str:
        payload = self._request("POST", "/api/user/login", body={"phone": phone, "password": password})
        token = payload["data"]["token"]
        self.tokens[phone] = token
        return token

    def send_register_code(self, phone: str) -> None:
        self._request("POST", "/api/user/register/send-code", body={"phone": phone})

    def register(self, phone: str, username: str, password: str, code: str) -> str:
        payload = self._request(
            "POST",
            "/api/user/register",
            body={
                "phone": phone,
                "username": username,
                "password": password,
                "confirmPassword": password,
                "code": code,
            },
        )
        token = payload["data"]["token"]
        self.tokens[phone] = token
        return token

    def list_merchants(self) -> list[dict]:
        payload = self._request("GET", "/api/merchant/list")
        return payload.get("data") or []

    def merchant_products(self, merchant_id: int) -> list[dict]:
        payload = self._request("GET", f"/api/product/list/{merchant_id}")
        return payload.get("data") or []

    def add_favorite(self, token: str, merchant_id: int) -> bool:
        payload = self._request(
            "POST",
            f"/api/favorites/{merchant_id}",
            token=token,
            allow_business_error=True,
        )
        if payload.get("code") == 200:
            return True
        msg = str(payload.get("message", ""))
        if "已收藏" in msg:
            return False
        raise RuntimeError(msg)

    def list_addresses(self, token: str) -> list[dict]:
        payload = self._request("GET", "/api/address/mine", token=token)
        return payload.get("data") or []

    def add_cart(self, token: str, product_id: int, quantity: int = 1) -> None:
        self._request(
            "POST",
            "/api/cart/add",
            token=token,
            body={"productId": product_id, "quantity": quantity},
        )

    def create_order(self, token: str, merchant_id: int, address_id: int) -> dict:
        payload = self._request(
            "POST",
            "/api/order/create",
            token=token,
            body={"merchantId": merchant_id, "addressId": address_id, "remark": "演示数据自动下单"},
        )
        return payload["data"]["order"]

    def pay_order(self, token: str, order_id: int) -> None:
        self._request(
            "POST",
            f"/api/order/pay/{order_id}",
            token=token,
            extra_headers={"Idempotency-Key": str(uuid.uuid4())},
        )

    def merchant_accept(self, merchant_token: str, order_id: int) -> None:
        self._request("POST", f"/api/order/accept/{order_id}", token=merchant_token)

    def user_complete(self, user_token: str, order_id: int) -> None:
        self._request("POST", f"/api/order/complete/{order_id}", token=user_token)

    def has_review(self, token: str, order_id: int) -> bool:
        payload = self._request(
            "GET",
            f"/api/review/order/{order_id}",
            token=token,
            allow_business_error=True,
        )
        return payload.get("code") == 200 and payload.get("data") is not None

    def add_review(self, token: str, order_id: int, score: int, content: str) -> bool:
        payload = self._request(
            "POST",
            "/api/review/add",
            token=token,
            body={"orderId": order_id, "score": score, "content": content, "images": []},
            allow_business_error=True,
        )
        if payload.get("code") == 200:
            return True
        msg = str(payload.get("message", ""))
        if "已评价" in msg:
            return False
        raise RuntimeError(msg)

    def list_orders(self, token: str) -> list[dict]:
        payload = self._request("GET", "/api/order/me", token=token)
        return payload.get("data") or []


def has_image(value: Any) -> bool:
    return bool(value and str(value).strip())


def ensure_demo_users(client: ApiClient, password: str, register_codes: list[str]) -> list[str]:
    active: list[str] = []
    for phone, username in zip(DEMO_USER_PHONES, DEMO_USER_NAMES):
        try:
            client.login(phone, password)
            print(f"  登录成功: {phone}")
            active.append(phone)
            continue
        except RuntimeError:
            if phone == "13800000001":
                raise
        registered = False
        last_error: Exception | None = None
        for code in register_codes:
            try:
                client.send_register_code(phone)
                client.register(phone, username, password, code)
                print(f"  注册成功: {phone} ({username}), code={code}")
                active.append(phone)
                registered = True
                break
            except RuntimeError as exc:
                last_error = exc
        if not registered and last_error:
            print(f"  跳过 {phone}: {last_error}")
    return active


def pick_eligible_merchants(client: ApiClient) -> list[dict]:
    eligible: list[dict] = []
    for merchant in client.list_merchants():
        if merchant.get("status") != "OPEN":
            continue
        if not has_image(merchant.get("logo")):
            continue
        products = client.merchant_products(int(merchant["id"]))
        pictured = [p for p in products if has_image(p.get("image")) and p.get("status") == "ON_SALE"]
        if not pictured:
            continue
        enriched = dict(merchant)
        enriched["_sample_product"] = pictured[0]
        eligible.append(enriched)
    return eligible


def build_favorite_plan(users: list[str], merchants: list[dict]) -> dict[int, list[str]]:
    plan: dict[int, list[str]] = {}
    if not merchants or not users:
        return plan
    patterns = [
        users[:],
        users[: max(1, len(users) // 2 + 1)],
        users[: max(1, len(users) - 1)],
        users[::2] or users[:1],
        users[:1],
        users[1:3] if len(users) >= 3 else users[:1],
    ]
    for index, merchant in enumerate(merchants):
        merchant_id = int(merchant["id"])
        owner = merchant.get("userId")
        chosen = [u for u in patterns[index % len(patterns)] if u != owner]
        plan[merchant_id] = chosen
    return plan


def seed_favorites(client: ApiClient, plan: dict[int, list[str]]) -> tuple[int, int]:
    created = 0
    skipped = 0
    for merchant_id, user_phones in plan.items():
        for phone in user_phones:
            token = client.tokens[phone]
            if client.add_favorite(token, merchant_id):
                created += 1
                print(f"  收藏新增: user={phone} -> merchant={merchant_id}")
            else:
                skipped += 1
                print(f"  收藏已存在: user={phone} -> merchant={merchant_id}")
    return created, skipped


def ensure_address(client: ApiClient, phone: str) -> int | None:
    token = client.tokens[phone]
    addresses = client.list_addresses(token)
    if addresses:
        default = next((a for a in addresses if a.get("isDefault")), addresses[0])
        return int(default["id"])
    print(f"  用户 {phone} 无收货地址，跳过自动下单（可在个人中心补地址）")
    return None


def seed_reviews_via_orders(
    client: ApiClient,
    users: list[str],
    merchants: list[dict],
    merchant_tokens: dict[str, str],
) -> tuple[int, int]:
    """为部分用户-商家组合自动下单并好评，保证演示有足够评价。"""
    created = 0
    skipped = 0
    review_idx = 0
    for index, merchant in enumerate(merchants):
        merchant_id = int(merchant["id"])
        owner_phone = merchant.get("userId")
        merchant_token = merchant_tokens.get(owner_phone or "")
        if not merchant_token:
            print(f"  商家 {merchant_id} 无法登录，跳过自动下单评价")
            continue
        target_users = users[:]
        if owner_phone in target_users:
            target_users = [u for u in target_users if u != owner_phone]
        # 每个商家选 1~2 个用户写好评，形成不等量
        selected = target_users[: (index % 3) + 1]
        product = merchant["_sample_product"]
        for phone in selected:
            user_token = client.tokens[phone]
            address_id = ensure_address(client, phone)
            if address_id is None:
                continue
            try:
                client.add_cart(user_token, int(product["id"]), 1)
                order = client.create_order(user_token, merchant_id, address_id)
                order_id = int(order["id"])
                client.pay_order(user_token, order_id)
                client.merchant_accept(merchant_token, order_id)
                client.user_complete(user_token, order_id)
                score, content = POSITIVE_REVIEWS[review_idx % len(POSITIVE_REVIEWS)]
                review_idx += 1
                if client.add_review(user_token, order_id, score, content):
                    created += 1
                    print(f"  好评新增: user={phone} merchant={merchant_id} order={order_id} score={score}")
                else:
                    skipped += 1
            except RuntimeError as exc:
                print(f"  自动下单评价失败 user={phone} merchant={merchant_id}: {exc}")
    return created, skipped


def seed_reviews_from_existing_orders(client: ApiClient, users: list[str]) -> tuple[int, int]:
    created = 0
    skipped = 0
    review_idx = 0
    for phone in users:
        token = client.tokens[phone]
        for entry in client.list_orders(token):
            order = entry.get("order") or entry
            if order.get("status") != "COMPLETED":
                continue
            order_id = int(order["id"])
            if client.has_review(token, order_id):
                skipped += 1
                continue
            score, content = POSITIVE_REVIEWS[review_idx % len(POSITIVE_REVIEWS)]
            review_idx += 1
            if client.add_review(token, order_id, score, content):
                created += 1
                print(f"  历史订单好评: user={phone} order={order_id} score={score}")
    return created, skipped


def login_merchants(client: ApiClient, merchants: list[dict], password: str) -> dict[str, str]:
    tokens: dict[str, str] = {}
    seen: set[str] = set()
    for merchant in merchants:
        phone = merchant.get("userId")
        if not phone or phone in seen:
            continue
        seen.add(phone)
        try:
            client.login(phone, password)
            tokens[phone] = client.tokens[phone]
            print(f"  商家登录: {phone} ({merchant.get('merchantName')})")
        except RuntimeError as exc:
            print(f"  商家登录失败 {phone}: {exc}")
    return tokens


def main() -> int:
    parser = argparse.ArgumentParser(description="Seed favorites and positive reviews for CLAS demo")
    parser.add_argument("--base-url", default=DEFAULT_BASE)
    parser.add_argument("--password", default=DEFAULT_PASSWORD)
    parser.add_argument(
        "--register-code",
        action="append",
        default=["888888", "123456"],
        help="注册验证码候选（可多次指定），默认依次尝试 888888 / 123456",
    )
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--skip-orders", action="store_true", help="不自动下单，仅处理已有完成订单")
    args = parser.parse_args()

    client = ApiClient(args.base_url, dry_run=args.dry_run)
    print(f"目标环境: {args.base_url}")

    print("\n[1/5] 准备演示用户...")
    users = ensure_demo_users(client, args.password, args.register_code)
    print(f"  可用用户: {', '.join(users)}")

    print("\n[2/5] 筛选有店铺图+商品图的营业商家...")
    merchants = pick_eligible_merchants(client)
    if not merchants:
        print("未找到符合条件的商家（需 logo 非空且存在带图 ON_SALE 商品）。")
        return 1
    for merchant in merchants:
        print(
            f"  - id={merchant['id']} name={merchant.get('merchantName')} "
            f"owner={merchant.get('userId')} logo={merchant.get('logo')}"
        )

    print("\n[3/5] 写入收藏（各商家收藏人数不等）...")
    plan = build_favorite_plan(users, merchants)
    for merchant_id, ups in plan.items():
        print(f"  商家 {merchant_id}: {len(ups)} 人 -> {', '.join(ups)}")
    fav_created, fav_skipped = seed_favorites(client, plan)

    print("\n[4/5] 为历史已完成订单补好评...")
    hist_created, hist_skipped = seed_reviews_from_existing_orders(client, users)

    order_created = order_skipped = 0
    if not args.skip_orders:
        print("\n[5/5] 自动下单并写入好评（按商家分配 1~N 个用户）...")
        merchant_tokens = login_merchants(client, merchants, args.password)
        order_created, order_skipped = seed_reviews_via_orders(client, users, merchants, merchant_tokens)
    else:
        print("\n[5/5] 已跳过自动下单。")

    print("\n========== 汇总 ==========")
    print(f"符合条件商家: {len(merchants)}")
    print(f"收藏新增: {fav_created}，已存在: {fav_skipped}")
    print(f"历史订单好评新增: {hist_created}，跳过: {hist_skipped}")
    print(f"自动下单好评新增: {order_created}，跳过/失败: {order_skipped}")
    print(f"好评合计新增: {hist_created + order_created}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
