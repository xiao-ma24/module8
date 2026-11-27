#!/usr/bin/env bash
# ============================================================
# 停止 start-all.sh 启动的 6 个后端
# ============================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [[ ! -d "$ROOT/run" ]]; then
  echo "没有 run/ 目录，无需停止"
  exit 0
fi

for pid_file in "$ROOT"/run/*.pid; do
  [[ -e "$pid_file" ]] || continue
  name="$(basename "$pid_file" .pid)"
  pid="$(cat "$pid_file")"
  if kill -0 "$pid" 2>/dev/null; then
    echo "停止 $name (pid $pid)"
    kill "$pid" 2>/dev/null || true
  else
    echo "$name 已退出"
  fi
  rm -f "$pid_file"
done
