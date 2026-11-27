#!/usr/bin/env bash
# ============================================================
# 一键启动 6 个后端（需先执行 scripts/build-all.sh）
# 环境变量：优先读取仓库根目录 .env（参考 .env.example），可用 PORT_xx 覆盖端口
# 日志：run/mXX.log    进程号：run/mXX.pid
# ============================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [[ -f "$ROOT/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  . "$ROOT/.env"
  set +a
fi


# ---------- 兼容《SpringBoot 云数据库接入教程》的变量名 ----------
# 教程约定：DB_URL（完整 JDBC URL）、DB_USER、DB_PASSWORD
# 本脚本同时支持 DB_HOST/DB_PORT/DB_NAME/DB_PASS 写法，二者取其一即可
if [[ -n "${DB_URL:-}" ]]; then
  export SPRING_DATASOURCE_URL="$DB_URL"
fi
if [[ -n "${DB_USER:-}" ]]; then
  export SPRING_DATASOURCE_USERNAME="$DB_USER"
fi
if [[ -n "${DB_PASSWORD:-}" ]]; then
  export SPRING_DATASOURCE_PASSWORD="$DB_PASSWORD"
elif [[ -n "${DB_PASS:-}" ]]; then
  export SPRING_DATASOURCE_PASSWORD="$DB_PASS"
fi

mkdir -p "$ROOT/run"
JAVA_BIN="${JAVA_BIN:-java}"
JAVA_OPTS="${JAVA_OPTS:--Xms256m -Xmx512m}"

start_one() {
  local name="$1" port="$2" jar="$3"
  if [[ -f "$ROOT/run/$name.pid" ]] && kill -0 "$(cat "$ROOT/run/$name.pid")" 2>/dev/null; then
    echo "[$name] 已在运行，跳过"
    return
  fi
  echo "[$name] 启动，端口 $port"
  PORT="$port" nohup "$JAVA_BIN" $JAVA_OPTS -jar "$ROOT/dist/jars/$jar" \
    > "$ROOT/run/$name.log" 2>&1 &
  echo $! > "$ROOT/run/$name.pid"
}

# 顺序：库存唯一写入口（②）先启动，其余服务随后
start_one m02 "${PORT_02:-8082}" "m08-02-backend.jar"
start_one m01 "${PORT_01:-8081}" "m08-01-backend.jar"
start_one m03 "${PORT_03:-8083}" "m08-03-backend.jar"
start_one m04 "${PORT_04:-8084}" "m08-04-backend.jar"
start_one m05 "${PORT_05:-8085}" "m08-05-backend.jar"
start_one m00 "${PORT_00:-8080}" "m08-00-backend.jar"

echo "启动指令已发出，查看日志：tail -f run/m02.log"
