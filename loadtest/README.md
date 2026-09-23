# Load Testing

This directory holds a [k6](https://k6.io) load test for the running API.

## 1. Install k6
See https://grafana.com/docs/k6/latest/set-up/install-k6/ (single binary, no dependencies).

## 2. Register a test user
The script logs in repeatedly, so it needs one real account. Either:
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Load Test","email":"loadtest@example.com","password":"loadTestPassword123"}'
```
or register one via Swagger UI / Scalar / Postman with the same credentials the script uses.

## 3. Run it
With the app running locally (`mvn spring-boot:run`) or via Docker (`docker compose up`):
```bash
k6 run loadtest/loadtest.js
```

Override the target, ramp, or credentials without editing the file:
```bash
BASE_URL=http://localhost:8081 LOGIN_EMAIL=loadtest@example.com LOGIN_PASSWORD=loadTestPassword123 \
  k6 run loadtest/loadtest.js
```

## 4. Reading the output
k6 prints a summary at the end. The two numbers that matter most:
- **`http_req_duration`** — response time distribution. The script's threshold fails the
  run if the 95th percentile exceeds 500ms.
- **`http_req_failed`** — error rate. The threshold fails the run if more than 1% of
  requests error out.

## What this is meant to demonstrate
- `GET /api/products` is Redis-cached (see `RedisConfig` / `ProductService`) — comparing
  its response time here against an uncached endpoint under the same load is a concrete
  way to show the cache is actually doing something, not just present in the code.
- `POST /api/auth/login` and `POST /api/auth/register` are rate-limited per IP (see
  `RateLimitingFilter`) — running this script with a high VU count from a single machine
  will start hitting `429 Too Many Requests` on those two endpoints by design. That's
  expected, not a bug in the load test.
