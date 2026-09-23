// Load test for the E-Commerce Order Management System API, using k6 (https://k6.io).
//
// Install: https://grafana.com/docs/k6/latest/set-up/install-k6/
// Run:     k6 run loadtest/loadtest.js
//          k6 run --vus 50 --duration 30s loadtest/loadtest.js   (override the scenario below)
//
// What it exercises:
//   1. Public browsing (GET /api/products) — the highest-traffic, now Redis-cached path
//   2. Login (POST /api/auth/login) — authentication under load
//   3. An authenticated read (GET /api/cart/{userId}) using the issued JWT
//
// Before running: register at least one test user (see registerTestUser.sh alongside
// this script, or just hit /api/auth/register once manually) so LOGIN_EMAIL/PASSWORD
// below are valid credentials.

import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
const LOGIN_EMAIL = __ENV.LOGIN_EMAIL || 'loadtest@example.com';
const LOGIN_PASSWORD = __ENV.LOGIN_PASSWORD || 'loadTestPassword123';

export const options = {
    scenarios: {
        browsing: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '10s', target: 20 },  // ramp up to 20 virtual users
                { duration: '30s', target: 20 },  // hold steady
                { duration: '10s', target: 0 },   // ramp down
            ],
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500'],   // 95% of requests should complete under 500ms
        http_req_failed: ['rate<0.01'],     // fewer than 1% of requests should fail
    },
};

export default function () {
    // 1. Browse products (public, Redis-cached — should be fast even under load)
    const productsRes = http.get(`${BASE_URL}/api/products`);
    check(productsRes, {
        'GET /api/products status is 200': (r) => r.status === 200,
    });

    sleep(1);

    // 2. Log in
    const loginRes = http.post(
        `${BASE_URL}/api/auth/login`,
        JSON.stringify({ email: LOGIN_EMAIL, password: LOGIN_PASSWORD }),
        { headers: { 'Content-Type': 'application/json' } }
    );
    check(loginRes, {
        'login status is 200': (r) => r.status === 200,
        'login returns a token': (r) => JSON.parse(r.body).token !== undefined,
    });

    if (loginRes.status === 200) {
        const body = JSON.parse(loginRes.body);
        const token = body.token;
        const userId = body.user.id;

        sleep(1);

        // 3. Authenticated read using the issued JWT
        const cartRes = http.get(`${BASE_URL}/api/cart/${userId}`, {
            headers: { Authorization: `Bearer ${token}` },
        });
        check(cartRes, {
            'GET /api/cart/{userId} status is 200': (r) => r.status === 200,
        });
    }

    sleep(1);
}
