# E-Commerce Order Management System

A backend-only e-commerce REST API built with Spring Boot, demonstrating real-world
integrations (JWT authentication, Razorpay payments, transactional order processing,
email notifications) commonly required in production and client work.

**Author:** Suresh Kosana

---

## Features

- **JWT-based authentication** with role-based access (`CUSTOMER` / `ADMIN`), plus a
  **refresh token flow** — short-lived (1hr) access tokens with a 7-day refresh token
  and a **logout endpoint** that revokes it server-side
- **Rate limiting** on login/register (Bucket4j, 5 requests/minute per IP) to slow down
  brute-force and registration-spam attempts
- **Redis caching** on the product catalog (`GET /api/products`, `GET /api/products/{id}`),
  with cache eviction wired into every write path — including stock changes from order placement
- **Product & category catalog** with search, filtering, and **pagination/sorting**
  (`GET /api/products/paged?page=0&size=10&sort=price,asc`)
- **Shopping cart** — get-or-create cart, duplicate-item merging, live total calculation
- **Order placement** — atomic, transactional cart→order conversion with stock
  validation, stock deduction, and price-at-purchase snapshotting
- **Razorpay payment integration** — order creation, HMAC-SHA256 signature verification
- **Email notifications** — order confirmation emails via Gmail SMTP (JavaMail)
- **Centralized exception handling** — consistent JSON error responses (`@RestControllerAdvice`)
- **Interactive API documentation** — both Swagger UI and Scalar
- **A minimal demo storefront** (`/storefront.html`) — plain HTML/JS, talks directly to
  the REST API (register, browse, cart, checkout), served same-origin so no CORS setup needed
- **Unit and integration tested** — JUnit 5 + Mockito for all services, plus a full-stack
  MockMvc integration test for the auth flow (register → login → refresh → logout) against
  an in-memory H2 database
- **Load tested** — a k6 script (`loadtest/`) exercising browsing, login, and cart under load
- **CI/CD** — GitHub Actions runs the full test suite on every push
- **Dockerized** — multi-stage build, docker-compose with MySQL and Redis

## Tech Stack

| Category | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.1 |
| Database | MySQL 8.0 |
| ORM | Spring Data JPA / Hibernate |
| Security | Spring Security + JWT (jjwt), Bucket4j (rate limiting) |
| Payments | Razorpay Java SDK |
| Email | JavaMail (Gmail SMTP) |
| Caching | Spring Cache + Redis |
| API Docs | springdoc-openapi (Swagger UI + Scalar) |
| Testing | JUnit 5, Mockito, MockMvc (integration), H2 (test DB), k6 (load testing) |
| CI/CD | GitHub Actions |
| Containerization | Docker, Docker Compose |

## Architecture Highlights

- **Price snapshot:** `OrderItem` stores its own `priceAtPurchase` instead of referencing
  `Product.price` directly, so past orders stay accurate even if a product's price changes later.
- **Atomic checkout:** `OrderService.placeOrder()` runs stock validation, stock deduction,
  order creation, and cart clearing inside a single `@Transactional` method — all succeed
  together or all roll back together.
- **Fail-safe email:** order confirmation emails are sent in a try-catch separate from the
  order transaction, so an SMTP failure never rolls back a successful order.
- **Signature-verified payments:** Razorpay payments are only marked `SUCCESS` after
  verifying the callback signature server-side, preventing spoofed "successful payment" calls.

## Getting Started

### Prerequisites
- Java 21
- Maven
- MySQL 8.0 (or use the provided Docker setup)
- A Razorpay test account (for payment testing)
- A Gmail account with an App Password (for email testing)

### Environment Variables
This project never hardcodes secrets. Set the following before running:

| Variable | Purpose |
|---|---|
| `DB_PASSWORD` | MySQL password |
| `JWT_SECRET` | Secret key for signing JWTs (32+ random characters) |
| `RAZORPAY_KEY_ID` | Razorpay test Key ID |
| `RAZORPAY_KEY_SECRET` | Razorpay test Key Secret |
| `MAIL_USERNAME` | Gmail address used to send confirmation emails |
| `MAIL_PASSWORD` | Gmail App Password (not your regular password) |
| `REDIS_HOST` / `REDIS_PORT` | Optional — defaults to `localhost:6379` if unset |

### Run locally
```bash
mvn spring-boot:run
```
The API starts on `http://localhost:8081`.

### Run with Docker
```bash
cp .env.example .env   # fill in real values
docker compose up --build
```
This starts both the app and a MySQL container together.

## API Documentation

Once running, explore and test every endpoint interactively:

- **Scalar** (modern UI + built-in API client): `http://localhost:8081/scalar`
- **Swagger UI**: `http://localhost:8081/swagger-ui/index.html`
- Raw OpenAPI spec: `http://localhost:8081/v3/api-docs`

## Demo Storefront

A minimal browsable UI is served at `http://localhost:8081/storefront.html` — register,
log in, browse products, add to cart, and place an order, all against the real API. It's
plain HTML/JS with no build step, meant to demonstrate the backend working end-to-end
rather than as a production frontend.

## Running Tests
```bash
mvn test
```
Covers `UserService`, `CartService`, `OrderService`, and `PaymentService`.

## Project Status

| Area | Status |
|---|---|
| Core backend (entities, repos, services, controllers) | ✅ Done |
| JWT authentication, refresh tokens & logout | ✅ Done |
| Cart → order checkout flow | ✅ Done |
| Razorpay payment integration | ✅ Done (live-verified) |
| Centralized exception handling | ✅ Done |
| Unit + integration testing (33 tests) | ✅ Done |
| Email notifications | ✅ Done |
| Swagger/OpenAPI + Scalar docs | ✅ Done |
| Rate limiting (Bucket4j) | ✅ Done |
| Redis caching (product catalog) | ✅ Done |
| Demo storefront (static HTML/JS) | ✅ Done |
| Load test script (k6) | ✅ Done |
| CI/CD (GitHub Actions) | ✅ Done |
| Dockerization (app + MySQL + Redis) | ✅ Done |
| README + .env.example + .gitignore | ✅ Done |
| Push to GitHub | ✅ Done |
