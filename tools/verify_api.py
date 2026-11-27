import json
import urllib.request
import http.cookiejar

AUTH = "http://127.0.0.1:18080"
SITE = "http://127.0.0.1:5182"

ROLES = {
    "T0005": ("ADMIN", [
        "/api/v1/admin/overview",
        "/api/v1/purchase/requests",
        "/api/v1/purchase/orders",
        "/api/v1/requisition/outbounds/pending",
        "/api/v1/borrow/orders",
        "/api/v1/stocktake/warnings",
        "/api/v1/basedata/materials?pageNum=1&pageSize=1",
    ]),
    "T0002": ("PURCHASER", [
        "/api/v1/purchase/requests",
        "/api/v1/purchase/orders",
        "/api/v1/purchase/acceptances",
        "/api/v1/purchase/dashboard/summary",
        "/api/v1/basedata/materials?pageNum=1&pageSize=1",
    ]),
    "T0007": ("CHECKER", [
        "/api/v1/purchase/acceptances",
        "/api/v1/purchase/orders/receivable",
        "/api/v1/basedata/materials?pageNum=1&pageSize=1",
    ]),
    "T0003": ("STORE_KEEPER", [
        "/api/v1/purchase/inbounds",
        "/api/v1/basedata/warehouses",
        "/api/v1/stocktake/warnings",
        "/api/v1/requisition/orders",
        "/api/v1/borrow/orders",
    ]),
    "T0004": ("STOCKTAKER", [
        "/api/v1/stocktake/orders",
        "/api/v1/stocktake/warnings",
        "/api/v1/stocktake/replenish/suggestions",
        "/api/v1/basedata/materials?pageNum=1&pageSize=1",
    ]),
    "T0006": ("APPLICANT", [
        "/api/v1/purchase/requests",
        "/api/v1/requisition/orders",
        "/api/v1/borrow/orders",
        "/api/v1/borrow/returns/overdue",
        "/api/v1/basedata/purchase/records?registrarUserId=T0006",
    ]),
    "T0001": ("DEPT_HEAD", [
        "/api/v1/purchase/requests",
        "/api/v1/purchase/requests/pending",
        "/api/v1/requisition/orders",
        "/api/v1/borrow/orders",
        "/api/v1/basedata/purchase/records?registrarUserId=T0001",
    ]),
}

NEGATIVE = {
    "T0006": [("/api/v1/purchase/orders", 403), ("/api/v1/stocktake/warnings", 403), ("/api/v1/admin/overview", 403)],
    "T0002": [("/api/v1/admin/overview", 403)],
    "T0001": [("/api/v1/stocktake/orders", 403)],
}

def session(account):
    jar = http.cookiejar.CookieJar()
    opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))
    body = json.dumps({"login_name": account, "password": "Campus@123"}).encode()
    req = urllib.request.Request(f"{AUTH}/api/auth/login", data=body, headers={"Content-Type": "application/json"})
    with opener.open(req, timeout=10) as resp:
        user = json.load(resp)
    return opener, user

def get(opener, path):
    try:
        with opener.open(SITE + path, timeout=15) as resp:
            return resp.status, resp.read()
    except urllib.error.HTTPError as e:
        return e.code, b""

failures = []
for account, (role, paths) in ROLES.items():
    opener, user = session(account)
    if user.get("role") != role:
        failures.append(f"{account} login role {user.get('role')} != {role}")
    for path in paths:
        status, _ = get(opener, path)
        if status != 200:
            failures.append(f"{role} {account} GET {path} -> {status}")
    print(f"{role:12s} {account} login-ok, {len(paths)} endpoints checked")

for account, cases in NEGATIVE.items():
    opener, _ = session(account)
    for path, expect in cases:
        status, _ = get(opener, path)
        if status != expect:
            failures.append(f"negative {account} {path} -> {status}, expected {expect}")
    print(f"{account} negative cases checked")

print("FAILURES:", len(failures))
for item in failures:
    print(" -", item)
