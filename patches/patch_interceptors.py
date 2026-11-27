from pathlib import Path

ROOT = Path(r"C:\Users\MECHREVO\Documents\Codex\2026-11-09\w-xia\outputs\integration\m08-final\物资管理-DBA交付-最终")

FILES = [
    ROOT / "subsystem-00-admin/backend/src/main/java/com/school/material/admin/auth/AuthInterceptor.java",
    ROOT / "subsystem-03-requisition-outstock/backend/src/main/java/com/school/material/requisition/auth/AuthInterceptor.java",
    ROOT / "subsystem-04-borrow-return-damage/backend/src/main/java/com/school/material/borrow/auth/AuthInterceptor.java",
]

OLD = """                LoginUser lu = new LoginUser();
                try { lu.setUserId(Long.valueOf(cu.userId())); } catch (NumberFormatException ignored) { }
                lu.setUserNo(cu.userId());
                lu.setUserName(cu.userId());"""

NEW = """                LoginUser lu = new LoginUser();
                Long uid = null;
                String uname = cu.userId();
                String deptId = cu.businessId();
                String deptName = null;
                try { uid = Long.valueOf(cu.userId()); } catch (NumberFormatException ignored) { }
                if (uid == null) {
                    try {
                        java.util.Map<String, Object> row = jdbc.queryForMap(
                                "SELECT u.user_id, u.user_name, u.org_id, d.dept_name FROM mat_sys_user u "
                                        + "LEFT JOIN mat_dept d ON d.dept_id = u.org_id "
                                        + "WHERE u.person_no = ? AND u.del_flag = 0 LIMIT 1",
                                cu.userId());
                        uid = ((Number) row.get("user_id")).longValue();
                        uname = String.valueOf(row.get("user_name"));
                        Object org = row.get("org_id");
                        if ((deptId == null || deptId.isBlank()) && org != null) deptId = String.valueOf(org);
                        Object dn = row.get("dept_name");
                        if (dn != null) deptName = String.valueOf(dn);
                    } catch (Exception ignore) { }
                }
                lu.setUserId(uid);
                lu.setUserNo(cu.userId());
                lu.setUserName(uname);
                lu.setDeptId(deptId);
                lu.setDeptName(deptName);"""

for path in FILES:
    text = path.read_text(encoding="utf-8")
    if OLD not in text:
        print("MISS", path.name)
        continue
    text = text.replace(OLD, NEW)
    anchor = "    private final CurrentUserService currentUserService;"
    if anchor in text and "jdbc.queryForMap" in text and "JdbcTemplate jdbc" not in text:
        text = text.replace(anchor, anchor + "\n\n    @org.springframework.beans.factory.annotation.Autowired\n    private org.springframework.jdbc.core.JdbcTemplate jdbc;")
    path.write_text(text, encoding="utf-8")
    print("PATCHED", path.parent.name)
