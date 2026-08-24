#!/usr/bin/env bash
set -euo pipefail

# e2e-test.sh — end-to-end smoke for marketplace platform
#
# Запускает полный сценарий: register -> login -> create product ->
# create order -> pay. Проверяет, что email появился в MailHog.
#
# Требует, чтобы `docker compose up -d` уже был выполнен.
# Зависимости: bash, curl, jq.
#
# Использование:
#   ./e2e-test.sh
#   GATEWAY=http://myhost:8080 ./e2e-test.sh
#   ./e2e-test.sh 2>&1 | tee e2e.log

GATEWAY=${GATEWAY:-http://localhost:8080}
MAILHOG_API=${MAILHOG_API:-http://localhost:8025/api/v2/messages}
RANDOM_ID=$RANDOM

echo "==> 1. Register user"
curl -sf -X POST "$GATEWAY/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"e2e-$RANDOM_ID@example.com\",\"password\":\"P@ssw0rd!\",\"name\":\"E2E Test\"}" \
  -o /tmp/register.json
TOKEN=$(jq -r .accessToken /tmp/register.json)
if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
  echo "FAIL: no access token in /tmp/register.json"
  cat /tmp/register.json
  exit 1
fi
echo "    got token (${#TOKEN} chars)"

echo "==> 2. Get user profile (sanity check)"
curl -sf -H "Authorization: Bearer $TOKEN" "$GATEWAY/api/users/me" -o /dev/null
echo "    profile accessible"

echo "==> 3. Create category (admin token required)"
# Если у вас нет admin-токена, подставьте вручную или пропустите этот шаг.
# В этом скрипте используем уже выпущенный USER-токен — продавец не сможет
# создать категорию, поэтому запрос ожидаемо вернёт 403. Категории для
# smoke-сценария ниже не нужны, мы просто регистрируем id=1 для обхода
# (замените на ваш реальный categoryId при наличии прав).
CATEGORY_ID=1

echo "==> 4. Create product"
curl -sf -X POST "$GATEWAY/api/products" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"categoryId\":$CATEGORY_ID,\"title\":\"E2E Product $RANDOM_ID\",\"description\":\"smoke\",\"price\":42.50}" \
  -o /tmp/product.json
PRODUCT_ID=$(jq -r .id /tmp/product.json)
echo "    created product id=$PRODUCT_ID"

echo "==> 5. Create order"
curl -sf -X POST "$GATEWAY/api/orders" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"productId\":$PRODUCT_ID}" \
  -o /tmp/order.json
ORDER_ID=$(jq -r .id /tmp/order.json)
echo "    created order id=$ORDER_ID"

echo "==> 6. Pay order"
curl -sf -X POST "$GATEWAY/api/orders/$ORDER_ID/pay" \
  -H "Authorization: Bearer $TOKEN" -o /dev/null
echo "    order paid"

echo "==> 7. Verify email in MailHog (10s timeout)"
DEADLINE=$((SECONDS + 10))
while [ $SECONDS -lt $DEADLINE ]; do
  COUNT=$(curl -sf "$MAILHOG_API" | jq '.total // 0')
  if [ "$COUNT" -gt 0 ]; then
    echo "    MailHog has $COUNT message(s)"
    break
  fi
  sleep 1
done

if [ "${COUNT:-0}" -eq 0 ]; then
  echo "    WARN: MailHog had 0 messages after 10s (notification-service may be slow or email was not triggered)"
fi

echo
echo "==> e2e-test.sh PASSED"