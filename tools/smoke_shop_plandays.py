# -*- coding: utf-8 -*-
"""冒烟：openStatus 读写 / 打烊拒单 / planDays 7 与 21（会员降级与放行）"""
import json, sys, urllib.request, urllib.error

BASE = "http://localhost:8080/api"

def call(method, path, body=None, token=None, form=False):
    req = urllib.request.Request(BASE + path, method=method)
    if token:
        req.add_header("Authorization", "Bearer " + token)
    data = None
    if body is not None:
        if form:
            data = urllib.parse.urlencode(body).encode()
            req.add_header("Content-Type", "application/x-www-form-urlencoded")
        else:
            data = json.dumps(body).encode()
            req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req, data, timeout=15) as resp:
            return json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        return {"code": e.code, "message": e.read().decode()[:300]}

import urllib.parse

def step(name, ok, extra=""):
    print(("PASS" if ok else "FAIL"), name, extra)
    if not ok:
        sys.exit(1)

# --- merchant login & shop openStatus ---
r = call("POST", "/auth/login", {"username": "merchant", "password": "merchant123", "roleType": 20})
step("merchant login", r.get("code") == 200, str(r)[:150])
mtoken = r["data"]["token"]

r = call("GET", "/merchant/shop", token=mtoken)
shop = r.get("data") or {}
step("shop has openStatus", "openStatus" in shop, "openStatus=%s" % shop.get("openStatus"))
orig = shop.get("openStatus")

r = call("PUT", "/merchant/shop", {"openStatus": 20, "shopNotice": "冒烟测试公告"}, token=mtoken)
step("PUT shop openStatus=20", r.get("code") == 200, str(r)[:150])
r = call("GET", "/merchant/shop", token=mtoken)
shop = r.get("data") or {}
step("GET shop reads back", shop.get("openStatus") == 20 and shop.get("shopNotice") == "冒烟测试公告",
     "openStatus=%s notice=%s" % (shop.get("openStatus"), shop.get("shopNotice")))

# --- user: closed shop rejects order ---
r = call("POST", "/auth/login", {"username": "user", "password": "user123", "roleType": 10})
utoken = r["data"]["token"]
call("DELETE", "/cart", token=utoken)  # 清空购物车（若支持）
r = call("POST", "/cart?dishId=1&itemType=10&quantity=1", token=utoken)
step("add to cart", r.get("code") == 200, str(r)[:150])
r = call("GET", "/cart", token=utoken)
data = r.get("data")
if isinstance(data, dict):
    items = data.get("items") or []
elif isinstance(data, list):
    items = data
else:
    items = []
cart_id = items[0]["id"] if items else None
if cart_id:
    call("PUT", "/cart/%s/select?selected=1" % cart_id, token=utoken)
qs = urllib.parse.urlencode({"address": "测试地址", "receiverName": "测试", "receiverPhone": "13800000000"})
r = call("POST", "/orders?" + qs, token=utoken)
step("closed shop rejects order", r.get("code") != 200 and "打烊" in str(r.get("message", "")), str(r)[:200])

# --- reopen shop ---
r = call("PUT", "/merchant/shop", {"openStatus": 10}, token=mtoken)
r = call("GET", "/merchant/shop", token=mtoken)
step("reopen shop", (r.get("data") or {}).get("openStatus") == 10)

# --- planDays=21 without membership -> downgrade to 7 ---
r = call("POST", "/plan/generate", {"planDays": 21}, token=utoken)
plan = (r.get("data") or {}).get("plan") or {}
meals = (r.get("data") or {}).get("meals") or []
step("21 without member -> 7", plan.get("planDays") == 7 and len(meals) == 21,
     "planDays=%s meals=%d" % (plan.get("planDays"), len(meals)))

# --- invalid planDays -> 7 ---
r = call("POST", "/plan/generate", {"planDays": 99}, token=utoken)
plan = (r.get("data") or {}).get("plan") or {}
step("planDays=99 -> 7", plan.get("planDays") == 7, "planDays=%s" % plan.get("planDays"))

# --- planDays=21 with active membership (inserted via SQL) -> 63 meals ---
r = call("POST", "/plan/generate", {"planDays": 21}, token=utoken)
plan = (r.get("data") or {}).get("plan") or {}
meals = (r.get("data") or {}).get("meals") or []
step("21 with member -> 63 meals", plan.get("planDays") == 21 and len(meals) == 63,
     "planDays=%s meals=%d" % (plan.get("planDays"), len(meals)))

# --- default (no body) -> 7 ---
r = call("POST", "/plan/generate", token=utoken)
plan = (r.get("data") or {}).get("plan") or {}
step("no body -> 7", plan.get("planDays") == 7, "planDays=%s" % plan.get("planDays"))

# 留一份进行中的计划给定时任务验证
call("POST", "/plan/start", token=utoken)
print("ALL API SMOKE TESTS PASSED")
