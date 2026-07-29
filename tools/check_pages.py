# -*- coding: utf-8 -*-
"""自查脚本：校验 json 合法性、wxml 标签闭合、scss 花括号配平"""
import json
import re
import pathlib

ROOT = pathlib.Path(r"D:\demo\BiteSmart\BiteSmartMin\miniprogram")

PAGES = [
    "index/index",
    "food/food",
    "profile/profile",
    "health/health",
    "assessment/assessment",
    "plan/plan",
    "exercise/exercise",
    "combo/combo",
    "combo-detail/combo-detail",
    "food-detail/food-detail",
    "cart/cart",
    "checkout/checkout",
    "pay-result/pay-result",
    "order/order",
    "order-detail/order-detail",
    "delivery/delivery",
    "review/review",
    "login/login",
    "account-setup/account-setup",
    "ai/ai",
    "chat/chat",
    "member/member",
    "address/address",
    "notice/notice",
    "settings/settings",
    "complaint/complaint",
    "m-home/m-home",
    "m-orders/m-orders",
    "m-dishes/m-dishes",
    "m-me/m-me",
    "m-stats/m-stats",
    "m-reviews/m-reviews",
    "m-dish-edit/m-dish-edit",
    "m-shop-edit/m-shop-edit",
    "m-combos/m-combos",
    "m-combo-edit/m-combo-edit",
    "r-tasks/r-tasks",
    "r-task/r-task",
    "r-exception/r-exception",
    "r-stats/r-stats",
    "r-me/r-me",
    "r-reviews/r-reviews",
    "r-profile/r-profile",
    "r-feedback/r-feedback",
]


def page_file(p: str, ext: str) -> pathlib.Path:
    name = p.split("/")[1]
    return ROOT / "pages" / name / (name + "." + ext)


fail = 0

for f in [ROOT / "app.json"] + [page_file(p, "json") for p in PAGES]:
    try:
        json.load(open(f, encoding="utf-8"))
        print("JSON OK ", f.name)
    except Exception as e:
        fail += 1
        print("JSON FAIL", f, e)

VOID = {"input", "br", "import", "include"}
for p in PAGES:
    f = page_file(p, "wxml")
    if not f.exists():
        fail += 1
        print("WXML MISSING", f)
        continue
    s = open(f, encoding="utf-8").read()
    s = re.sub(r"<!--.*?-->", "", s, flags=re.S)
    stack = []
    ok = True
    for m in re.finditer(r"<(/?)([a-zA-Z][\w-]*)((?:[^>\"']|\"[^\"]*\"|'[^']*')*?)(/?)>", s):
        closing, tag, _attrs, selfclose = m.groups()
        if closing:
            if not stack or stack[-1] != tag:
                print("WXML MISMATCH", f.name, "tag", tag, "stack tail", stack[-3:])
                ok = False
                fail += 1
                break
            stack.pop()
        elif selfclose or tag in VOID:
            continue
        else:
            stack.append(tag)
    if ok:
        if stack:
            fail += 1
            print("WXML UNCLOSED", f.name, stack)
        else:
            print("WXML OK ", f.name)

for p in PAGES:
    f = page_file(p, "scss")
    if not f.exists():
        print("SCSS MISSING", f.name)
        fail += 1
        continue
    s = open(f, encoding="utf-8").read()
    s = re.sub(r"/\*.*?\*/", "", s, flags=re.S)
    if s.count("{") != s.count("}"):
        fail += 1
        print("SCSS BRACE MISMATCH", f.name, s.count("{"), s.count("}"))
    else:
        print("SCSS OK ", f.name)

# 全局 scss
s = open(ROOT / "app.scss", encoding="utf-8").read()
s = re.sub(r"/\*.*?\*/", "", s, flags=re.S)
print("SCSS OK  app.scss" if s.count("{") == s.count("}") else "SCSS BRACE MISMATCH app.scss")

# 禁止 emoji 粗查（设计规范明确允许 ✓ › ‹ × 等文字符号，故只查 emoji 区段）
EMOJI = re.compile("[\U0001F000-\U0001FAFF\U0001F1E6-\U0001F1FF]")
for p in PAGES:
    for ext in ("wxml", "ts"):
        f = page_file(p, ext)
        if f.exists():
            t = open(f, encoding="utf-8").read()
            hit = EMOJI.search(t)
            if hit:
                fail += 1
                print("EMOJI FOUND", f.name, repr(hit.group(0)))

print("FAILURES:", fail)
raise SystemExit(1 if fail else 0)
