import re
from pathlib import Path

ROOT = Path(r"C:\Users\MECHREVO\Documents\Codex\2026-11-09\w-xia\outputs\integration\m08-final\物资管理-DBA交付-最终")
changed = []

def write(path, text):
    path.write_text(text, encoding="utf-8")
    changed.append(str(path.relative_to(ROOT)))

def replace_in(path, old, new, count=1):
    text = path.read_text(encoding="utf-8")
    if old not in text:
        print("MISS:", path.name, "->", old[:60])
        return False
    write(path, text.replace(old, new, count))
    return True

# 1) 13 处错误角色注解
for java in ROOT.rglob("*.java"):
    text = java.read_text(encoding="utf-8")
    if '@RequireRole({"A", "D", "M", "I", "N"})' in text:
        write(java, text.replace('@RequireRole({"A", "D", "M", "I", "N"})', '@RequireRole({"ADMIN"})'))

# 2) 01 拦截器路径
cors = ROOT / "subsystem-01-base-data/backend/src/main/java/com/school/material/config/CorsConfig.java"
replace_in(cors, 'addPathPatterns("/api/**")', 'addPathPatterns("/**")')

# 3) 菜单与后端角色对齐
pairs = [
    ("subsystem-02-purchase-instock/backend/src/main/java/com/school/material/purchase/controller/PurchaseRequestController.java",
     '@RequireRole({"PURCHASER", "APPLICANT", "ADMIN"})',
     '@RequireRole({"PURCHASER", "APPLICANT", "ADMIN", "DEPT_HEAD"})'),
    ("subsystem-02-purchase-instock/backend/src/main/java/com/school/material/purchase/controller/AcceptanceController.java",
     '@RequireRole({"CHECKER", "STORE_KEEPER", "ADMIN"})',
     '@RequireRole({"CHECKER", "STORE_KEEPER", "ADMIN", "PURCHASER"})'),
    ("subsystem-03-requisition-outstock/backend/src/main/java/com/school/material/requisition/controller/ReqOrderController.java",
     '@RequireRole({"APPLICANT", "STUDENT", "DEPT_HEAD", "ADMIN"})',
     '@RequireRole({"APPLICANT", "STUDENT", "DEPT_HEAD", "ADMIN", "STORE_KEEPER"})'),
    ("subsystem-04-borrow-return-damage/backend/src/main/java/com/school/material/borrow/controller/BorrowController.java",
     '@RequireRole({"APPLICANT", "STUDENT", "DEPT_HEAD", "ADMIN"})',
     '@RequireRole({"APPLICANT", "STUDENT", "DEPT_HEAD", "ADMIN", "STORE_KEEPER"})'),
    ("subsystem-05-stocktake-replenish/backend/src/main/java/com/school/material/stocktake/controller/WarningController.java",
     '@RequireRole({"STOCKTAKER", "ADMIN"})',
     '@RequireRole({"STOCKTAKER", "ADMIN", "STORE_KEEPER"})'),
    ("subsystem-05-stocktake-replenish/backend/src/main/java/com/school/material/stocktake/controller/ReplenishController.java",
     '@RequireRole({"STOCKTAKER", "ADMIN"})',
     '@RequireRole({"STOCKTAKER", "ADMIN", "STORE_KEEPER"})'),
]
for rel, old, new in pairs:
    path = ROOT / rel
    if path.exists():
        replace_in(path, old, new)
    else:
        print("MISSING FILE:", rel)

# 4) 01 读接口放开、写接口 ADMIN
def open_reads_admin_writes(path):
    lines = path.read_text(encoding="utf-8").splitlines()
    out, removed = [], False
    for line in lines:
        if not removed and line.strip() == '@RequireRole({"ADMIN"})':
            removed = True
            continue
        if re.match(r"^\s*@(Post|Put|Delete)Mapping", line):
            out.append('    @RequireRole({"ADMIN"})')
        out.append(line)
    write(path, "\n".join(out) + "\n")

for rel in [
    "subsystem-01-base-data/backend/src/main/java/com/school/material/controller/MaterialController.java",
    "subsystem-01-base-data/backend/src/main/java/com/school/material/controller/MaterialCategoryController.java",
    "subsystem-01-base-data/backend/src/main/java/com/school/material/controller/StockController.java",
    "subsystem-01-base-data/backend/src/main/java/com/school/material/controller/WarehouseController.java",
]:
    open_reads_admin_writes(ROOT / rel)

# 5) 01 个人发票登记接口按角色开放
purchase = ROOT / "subsystem-01-base-data/backend/src/main/java/com/school/material/purchase/controller/PurchaseController.java"
text = purchase.read_text(encoding="utf-8")
text = text.replace("""@RequiredArgsConstructor
@RequireRole({"ADMIN"})
public class PurchaseController {""", """@RequiredArgsConstructor
public class PurchaseController {""")
text = text.replace("""    /** 个人发票登记（仅登记，状态为待验收，不变更库存） */
    @PostMapping("/register")""", """    /** 个人发票登记（仅登记，状态为待验收，不变更库存） */
    @RequireRole({"ADMIN", "APPLICANT", "TEACHER", "DEPT_HEAD"})
    @PostMapping("/register")""")
text = text.replace("""    /** 查询个人发票登记记录：不传参查全部，可按登记人、审核状态筛选 */
    @GetMapping("/records")""", """    /** 查询个人发票登记记录：不传参查全部，可按登记人、审核状态筛选 */
    @RequireRole({"ADMIN", "APPLICANT", "TEACHER", "DEPT_HEAD", "STORE_KEEPER"})
    @GetMapping("/records")""")
text = text.replace("""    /** 查询待验收（0）的发票登记记录，供管理员验收入库 */
    @GetMapping("/pending")""", """    /** 查询待验收（0）的发票登记记录，供管理员验收入库 */
    @RequireRole({"ADMIN", "STORE_KEEPER"})
    @GetMapping("/pending")""")
text = text.replace("""    /** 管理员审核：1-通过并入库，2-驳回 */
    @PostMapping("/audit")""", """    /** 管理员审核：1-通过并入库，2-驳回 */
    @RequireRole({"ADMIN"})
    @PostMapping("/audit")""")
write(purchase, text)

# 6) 02 订单应收列表开放给验收员
order = ROOT / "subsystem-02-purchase-instock/backend/src/main/java/com/school/material/purchase/controller/PurchaseOrderController.java"
text = order.read_text(encoding="utf-8")
text = text.replace("""    @GetMapping("/receivable")""", """    @RequireRole({"PURCHASER", "ADMIN", "CHECKER"})
    @GetMapping("/receivable")""")
write(order, text)

# 7) 02 AI 状态对所有登录用户开放
ai = ROOT / "subsystem-02-purchase-instock/backend/src/main/java/com/school/material/purchase/controller/AiController.java"
text = ai.read_text(encoding="utf-8")
text = text.replace("""@RequiredArgsConstructor
@RequireRole({"PURCHASER", "ADMIN"})
public class AiController {""", """@RequiredArgsConstructor
public class AiController {""")
text = text.replace("""    @PostMapping("/chat")""", """    @RequireRole({"PURCHASER", "ADMIN"})
    @PostMapping("/chat")""")
text = text.replace("""    @GetMapping("/replenish-advice")""", """    @RequireRole({"PURCHASER", "ADMIN"})
    @GetMapping("/replenish-advice")""")
write(ai, text)

print("changed files:", len(changed))
for item in changed:
    print(" -", item)
