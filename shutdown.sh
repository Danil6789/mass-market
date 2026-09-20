#!/usr/bin/env bash
# shutdown.sh — останавливает все микросервисы, запущенные через startup.sh

set -uo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PIDS_DIR="$ROOT_DIR/.pids"

if [ ! -d "$PIDS_DIR" ]; then
  echo "Нет $PIDS_DIR — нечего останавливать"
  exit 0
fi

stopped=0
for pid_file in "$PIDS_DIR"/*.pid; do
  [ -f "$pid_file" ] || continue
  module=$(basename "$pid_file" .pid)
  pid=$(cat "$pid_file")
  if kill -0 "$pid" 2>/dev/null; then
    echo "[$module] останавливаю PID=$pid..."
    kill "$pid" 2>/dev/null || true
    # Даём процессу 5 секунд на graceful shutdown
    for _ in 1 2 3 4 5; do
      kill -0 "$pid" 2>/dev/null || break
      sleep 1
    done
    # Если всё ещё жив — принудительно
    if kill -0 "$pid" 2>/dev/null; then
      echo "[$module] принудительная остановка..."
      kill -9 "$pid" 2>/dev/null || true
    fi
    stopped=$((stopped + 1))
  fi
  rm -f "$pid_file"
done

# Подчищаем оставшиеся gradle/java процессы от bootRun (могут остаться детёныши)
# Ищем java -jar или GradleWrapperMain по нашим сервисам
for module in eureka-server api-gateway user-service product-service order-service notification-service admin-service; do
  pkill -f "GradleWrapperMain.*${module}" 2>/dev/null || true
  pkill -f "${module}.*spring-boot" 2>/dev/null || true
done

echo "==> Остановлено сервисов: $stopped"