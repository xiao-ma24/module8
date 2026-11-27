#!/usr/bin/env bash
# ============================================================
# 一键打包 6 个后端，并按交付命名输出到 dist/jars/
# 可选：--with-frontend 同时构建 5182 统一前端，输出到 dist/frontend/
# 依赖：JDK 17+、Maven 3.8+、Node 18+（仅前端）
# ============================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MVN="${MVN:-mvn}"

MODULES=(
  "subsystem-00-admin/backend"
  "subsystem-01-base-data/backend"
  "subsystem-02-purchase-instock/backend"
  "subsystem-03-requisition-outstock/backend"
  "subsystem-04-borrow-return-damage/backend"
  "subsystem-05-stocktake-replenish/backend"
)
ARTIFACTS=(
  "admin-platform-1.0.0.jar"
  "material-base-data-1.0.0.jar"
  "purchase-instock-1.0.0.jar"
  "requisition-outstock-1.0.0.jar"
  "borrow-return-damage-1.0.0.jar"
  "stocktake-replenish-1.0.0.jar"
)
DELIVER=(
  "m00-backend.jar"
  "m01-backend.jar"
  "m02-backend.jar"
  "m03-backend.jar"
  "m04-backend.jar"
  "m05-backend.jar"
)

OUT="$ROOT/dist/jars"
mkdir -p "$OUT"

for i in "${!MODULES[@]}"; do
  echo ">>> 打包 ${MODULES[$i]}"
  (cd "$ROOT/${MODULES[$i]}" && "$MVN" -q -DskipTests clean package)
  cp "$ROOT/${MODULES[$i]}/target/${ARTIFACTS[$i]}" "$OUT/${DELIVER[$i]}"
  echo "    -> dist/jars/${DELIVER[$i]}"
done

if [[ "${1:-}" == "--with-frontend" ]]; then
  build_front() {
    local dir="$1" out="$2"
    echo ">>> 构建 $dir"
    (cd "$ROOT/$dir" && (npm ci || npm install) && npm run build)
    rm -rf "$ROOT/$out"
    cp -R "$ROOT/$dir/dist" "$ROOT/$out"
    echo "    -> $out/"
  }
  # 02 的前端即统一大系统前端（含采购/领用/借用/盘点/基础数据页面）
  build_front "subsystem-02-purchase-instock/frontend"   "dist/frontend"
  build_front "subsystem-00-admin/frontend"              "dist/frontend-m00"
  build_front "subsystem-01-base-data/frontend"          "dist/frontend-m01"
  build_front "subsystem-03-requisition-outstock/frontend" "dist/frontend-m03"
  build_front "subsystem-04-borrow-return-damage/frontend" "dist/frontend-m04"
  build_front "subsystem-05-stocktake-replenish/frontend"  "dist/frontend-m05"
fi

echo "打包完成：$OUT"
