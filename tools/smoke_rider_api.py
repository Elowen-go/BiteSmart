# -*- coding: utf-8 -*-
"""冒烟：骑手拒单回池 / 骑手profile / 轨迹上传→tracking / 订单备注快照"""
import json, sys, time, urllib.request, urllib.error, urllib.parse

BASE = "http://localhost:8080/api"

def call(method, path, body=None, token=None):
    req = urllib.request.Request(BASE + path, method=method)
    if token:
        req.add_header("Authorization", "Bearer " + token)
    data = None
    if body is not None:
        data = json.dumps(body).encode()
        req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req, data, timeout=15) as resp:
            return json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        return {"code": e.code, "message": e.read().decode()[:300]}

def step(name, ok, extra=""):
    print(("PASS" if ok else "FAIL"), name, extra)
    if not ok:
        sys.exit(1)

def login(u, p, role):
    r = call("POST", "/auth/login", {"username": u, "password": p, "roleType": role})
    assert r.get("code") == 200, str(r)
    return r["data"]["token"]

# ---------- 造单：用户下单→支付→商家接单/备餐/出餐 ----------
utoken = login("user", "user123", 10)
call("POST", "/cart?dishId=1&itemType=10&quantity=1", token=utoken)
r = call("GET", "/cart", token=utoken)
items = r.get("data") if isinstance(r.get("data"), list) else (r.get("data") or {}).get("items", [])
for it in items:
    call("PUT", "/cart/%s/select?selected=1" % it["id"], token=utoken)
qs = urllib.parse.urlencode({"address": "滨江区测试路1号", "receiverName": "王硕",
                             "receiverPhone": "13800000000", "remark": "少辣，放前台"})
r = call("POST", "/orders?" + qs, token=utoken)
step("create order", r.get("code") == 200, str(r)[:200])
order_no = r.get("data") if isinstance(r.get("data"), str) else (r.get("data") or {}).get("orderNo")
r = call("GET", "/orders", token=utoken)
orders = r.get("data") if isinstance(r.get("data"), list) else (r.get("data") or {}).get("records", [])
order = [o for o in orders if o.get("orderNo") == order_no][0] if order_no else orders[0]
order_id = order["id"]
r = call("POST", "/orders/%s/pay?payMethod=20" % order_id, token=utoken)
step("pay order", r.get("code") == 200, str(r)[:200])

mtoken = login("merchant", "merchant123", 20)
r = call("PUT", "/merchant/orders/%s/accept" % order_id, token=mtoken)
step("merchant accept", r.get("code") == 200, str(r)[:150])
r = call("PUT", "/merchant/orders/%s/prepare" % order_id, token=mtoken)
step("merchant prepare", r.get("code") == 200, str(r)[:150])
r = call("PUT", "/merchant/orders/%s/done" % order_id, token=mtoken)
step("merchant done (create task)", r.get("code") == 200, str(r)[:150])

# ---------- 注册骑手 ----------
uname = "rider_smoke_%d" % int(time.time())
r = call("POST", "/auth/register", {"username": uname, "password": "rider123", "roleType": 30, "nickname": "冒烟骑手"})
step("register rider user", r.get("code") == 200, str(r)[:150])
dtoken = login(uname, "rider123", 30)
r = call("POST", "/driver/register?" + urllib.parse.urlencode(
    {"realName": "冒烟骑手", "phone": "13900000000", "idCard": "330100200001010011", "vehicleType": 10}), token=dtoken)
step("driver register", r.get("code") == 200, str(r)[:150])

# ---------- 接单 → 拒单 → 回池 ----------
r = call("GET", "/driver/tasks/pending", token=dtoken)
tasks = r.get("data") or []
task = [t for t in tasks if t.get("orderId") == order_id]
step("task in pending pool", len(task) == 1, "pending=%d" % len(tasks))
task = task[0]
step("task has orderRemark", task.get("orderRemark") == "少辣，放前台", "remark=%s" % task.get("orderRemark"))
tid = task["id"]

r = call("POST", "/driver/tasks/%s/accept" % tid, token=dtoken)
step("accept task", r.get("code") == 200, str(r)[:150])

r = call("POST", "/driver/tasks/%s/reject?reason=%s" % (tid, urllib.parse.quote("商家出餐太慢")), token=dtoken)
step("reject task", r.get("code") == 200, str(r)[:150])

r = call("GET", "/driver/tasks/pending", token=dtoken)
back = [t for t in (r.get("data") or []) if t.get("id") == tid]
step("task back to pool with reason", len(back) == 1 and back[0].get("taskStatus") == 10
     and back[0].get("driverId") is None and back[0].get("rejectReason") == "商家出餐太慢",
     str({k: back[0].get(k) for k in ("taskStatus", "driverId", "rejectReason")}) if back else "not found")

r = call("POST", "/driver/tasks/%s/reject?reason=x" % tid, token=dtoken)
step("reject again rejected(400)", r.get("code") != 200, str(r)[:150])

# ---------- profile ----------
r = call("GET", "/driver/profile", token=dtoken)
step("get profile", r.get("code") == 200 and r["data"].get("realName") == "冒烟骑手", str(r.get("data"))[:150])
r = call("PUT", "/driver/profile", {"phone": "13911112222", "vehicleType": 20}, token=dtoken)
step("put profile", r.get("code") == 200, str(r)[:150])
r = call("GET", "/driver/profile", token=dtoken)
step("profile updated", r["data"].get("phone") == "13911112222" and r["data"].get("vehicleType") == 20,
     "phone=%s vehicle=%s" % (r["data"].get("phone"), r["data"].get("vehicleType")))

# ---------- 重新接单 → 取餐 → 上传轨迹 ----------
r = call("POST", "/driver/tasks/%s/accept" % tid, token=dtoken)
step("re-accept task", r.get("code") == 200, str(r)[:150])
r = call("POST", "/driver/tasks/%s/pickup" % tid, token=dtoken)
step("pickup task", r.get("code") == 200, str(r)[:150])

r = call("POST", "/driver/location", {"taskId": tid, "latitude": 30.2084000, "longitude": 120.2120100}, token=dtoken)
step("upload location 1", r.get("code") == 200, str(r)[:150])
time.sleep(1)
r = call("POST", "/driver/location", {"taskId": tid, "latitude": 30.2095000, "longitude": 120.2130200}, token=dtoken)
step("upload location 2", r.get("code") == 200, str(r)[:150])

# 状态不符的任务上传应失败（已完成订单随便造个id不行，用拒单流程外的校验：换个不属于他的任务即可跳过，这里验证路径即可）

# ---------- 用户端轨迹查询（免登录） ----------
r = call("GET", "/delivery/tracking/%s" % order_id)
d = r.get("data") or {}
step("tracking rider fields", d.get("riderLat") is not None and d.get("riderLng") is not None and d.get("locTime"),
     "riderLat=%s riderLng=%s locTime=%s" % (d.get("riderLat"), d.get("riderLng"), d.get("locTime")))
path = d.get("path") or []
step("tracking path", len(path) == 2 and path[0].get("latitude") and path[0].get("time"),
     "points=%d first=%s" % (len(path), str(path[0]) if path else None))
step("tracking keeps old fields", d.get("taskStatus") == 30 and d.get("pickupCode") and "driver" in d,
     "taskStatus=%s" % d.get("taskStatus"))
step("tracking orderRemark", d.get("orderRemark") == "少辣，放前台", "remark=%s" % d.get("orderRemark"))

print("ALL RIDER SMOKE TESTS PASSED")
