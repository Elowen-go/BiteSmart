# -*- coding: utf-8 -*-
"""BiteSmart 计划模块冒烟测试：login -> profile -> generate -> current -> start -> check -> swap -> uncheck -> advance -> exercise library"""
import json, sys, urllib.request, urllib.error
from datetime import date

BASE = "http://localhost:8080/api"

def call(method, path, body=None, token=None):
    req = urllib.request.Request(BASE + path, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    data = json.dumps(body).encode() if body is not None else None
    try:
        with urllib.request.urlopen(req, data, timeout=15) as resp:
            return json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        return {"code": e.code, "message": e.read().decode()[:300]}

def step(name, ok, extra=""):
    print(("PASS" if ok else "FAIL"), name, extra)
    if not ok:
        sys.exit(1)

# 1. login
r = call("POST", "/auth/login", {"username": "user", "password": "user123", "roleType": 10})
step("login", r.get("code") == 200 and r.get("data", {}).get("token"), str(r)[:200])
token = r["data"]["token"]

# 2. save profile with new fields
profile = {
    "age": 24, "gender": 10, "height": 178, "weight": 72.4,
    "activityLevel": 30, "healthGoal": "减脂",
    "dietPreference": '["少油少盐"]', "allergyInfo": '["香菜"]',
    "targetWeight": 70, "exerciseFreq": 3, "focusParts": '["全身","腰腹"]'
}
r = call("PUT", "/user/profile", profile, token)
step("save profile", r.get("code") == 200, str(r)[:200])

# 3. read profile back
r = call("GET", "/user/profile", token=token)
d = r.get("data") or {}
step("profile new fields", r.get("code") == 200 and d.get("targetWeight") == 70 and d.get("exerciseFreq") == 3 and d.get("focusParts"),
     str({k: d.get(k) for k in ("targetWeight", "exerciseFreq", "focusParts")}))

# 4. generate plan
r = call("POST", "/plan/generate", token=token)
step("generate", r.get("code") == 200 and r.get("data", {}).get("plan"), str(r)[:200])
plan = r["data"]["plan"]
meals = r["data"]["meals"]
step("21 meals", len(meals) == 21, "got %d" % len(meals))
step("meal has dish info", all(m.get("dishName") and m.get("calories") is not None for m in meals), str(meals[0])[:200])

# 5. current
r = call("GET", "/plan/current", token=token)
step("current", r.get("code") == 200 and r["data"]["plan"]["id"] == plan["id"])

# 6. start
r = call("POST", "/plan/start", token=token)
step("start", r.get("code") == 200 and r["data"]["plan"]["status"] == 20 and r["data"]["plan"]["curDay"] == 0, str(r)[:200])

# 7. check day0 breakfast
meal0 = [m for m in meals if m["dayIndex"] == 0 and m["mealIndex"] == 0][0]
r = call("POST", "/plan/check", {"mealId": meal0["id"], "checked": True}, token)
step("check", r.get("code") == 200, str(r)[:200])

# 8. diet record synced
today = date.today().isoformat()
r = call("GET", "/health/diet?date=" + today, token=token)
recs = r.get("data") or []
plan_rec = [x for x in recs if x.get("sourceType") == 30 and (x.get("foodName") or "").endswith("（计划餐）")]
step("diet_record synced", len(plan_rec) == 1 and plan_rec[0].get("mealType") == 10, str(plan_rec)[:300])

# 9. check non-current day should fail
meal_d1 = [m for m in meals if m["dayIndex"] == 1 and m["mealIndex"] == 0][0]
r = call("POST", "/plan/check", {"mealId": meal_d1["id"], "checked": True}, token)
step("check other day rejected", r.get("code") != 200, str(r)[:150])

# 10. swap day0 lunch
meal1 = [m for m in meals if m["dayIndex"] == 0 and m["mealIndex"] == 1][0]
r = call("POST", "/plan/swap", {"mealId": meal1["id"]}, token)
new_meal1 = [m for m in r["data"]["meals"] if m["id"] == meal1["id"]][0]
step("swap", r.get("code") == 200 and new_meal1["swapCount"] == 1 and new_meal1["dishId"] != meal1["dishId"],
     "old=%s new=%s" % (meal1["dishId"], new_meal1["dishId"]))

# 11. swap checked meal should fail
r = call("POST", "/plan/swap", {"mealId": meal0["id"]}, token)
step("swap checked rejected", r.get("code") != 200, str(r)[:150])

# 12. uncheck -> diet record removed
r = call("POST", "/plan/check", {"mealId": meal0["id"], "checked": False}, token)
step("uncheck", r.get("code") == 200, str(r)[:200])
r = call("GET", "/health/diet?date=" + today, token=token)
plan_rec = [x for x in (r.get("data") or []) if x.get("sourceType") == 30]
step("diet_record removed", len(plan_rec) == 0, "remain=%d" % len(plan_rec))

# 13. advance
r = call("POST", "/plan/advance", token=token)
step("advance", r.get("code") == 200 and r["data"]["plan"]["curDay"] == 1, str(r)[:200])

# 14. exercise library
r = call("GET", "/exercise/library", token=token)
ex = r.get("data") or []
step("exercise library", r.get("code") == 200 and len(ex) == 8 and ex[0].get("sort") == 1 and ex[0].get("kcalPerMin"),
     "count=%d first=%s" % (len(ex), ex[0].get("name") if ex else None))

# 15. dish detail has new fields
r = call("GET", "/dishes/1", token=token)
d = r.get("data") or {}
dish = d.get("dish") if isinstance(d, dict) and "dish" in d else d
step("dish new fields", bool(dish.get("tags")) and bool(dish.get("aiComment")) and bool(dish.get("fitScenes")) and bool(dish.get("cautions")),
     str({k: (str(dish.get(k)) or "")[:20] for k in ("tags", "aiComment", "fitScenes", "cautions")}))

print("ALL SMOKE TESTS PASSED")
