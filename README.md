# 🍱 FoodShare — AI-Powered Food Donation Marketplace

**Reduce food waste. Feed the hungry. Build community.**

FoodShare is a full-stack web platform that connects **donors** (restaurants, grocery stores, households, event caterers) with **NGOs and food banks**, using AI to make food rescue fast, smart, and transparent.

> 🌐 **Live Demo:** https://fooddonation-jxvu.onrender.com
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/a0596155-3518-48e6-b2c0-23f9e442a201" />

---

## 🎯 The Real-Life Problem We're Solving

Every year, **roughly one-third of all food produced globally is wasted** — over **1.3 billion tonnes** — while millions of people go hungry. The problem isn't a lack of food; it's a **lack of connection** between surplus and need.

Most surplus food today is simply thrown away because:

- Donors don't know which NGO can use what they have.
- NGOs can't discover donations quickly enough (food expires).
- There is no simple, trustable way to coordinate pickup.
- Matching is manual, slow, and full of friction.

**FoodShare solves this** with a real-time marketplace + AI assistant that connects surplus food to the people who need it, in minutes instead of days.

---

## ✨ Features

### 👤 Role-Based Platform (Donor / NGO / Admin)
- **Donor** – register, create food donation listings (type, quantity, pickup location/time), track donations, cancel.
- **NGO** – browse available donations, accept, and drive the life-cycle to completion.
- **Admin** – platform dashboard with live stats, manage users (activate/deactivate), monitor all donations.

### 🤖 AI Assistant (Powered by Groq — Llama 3.3 70B)
- **AI Chat** – ask questions about the platform, food safety, or donation best practices.
- **AI Listing Assistant** – get smart suggestions when creating a donation (title, description, category, quantity).
- **AI Impact Report** – auto-generate a human-readable social & environmental impact summary from real platform stats.

### 🔄 Donation Life-Cycle
`ACCEPTED → PICKED_UP → DELIVERED → COMPLETED` — a clean state machine with role-based guards and optimistic-locking safety.

### 🔔 Real-Time Notifications
- WebSocket (STOMP) push notifications the moment a donation is accepted or a status changes.
- In-app notification centre with unread counts and "mark all read".

### 🔐 Security & Reliability
- Spring Security + role-based access control on the web UI.
- JWT-based stateless REST API for mobile/third-party clients.
- **Brute-force login protection** (Redis token-bucket lockout).
- **Rate limiting** on AI & auth endpoints (distributed via Redis).
- CSRF protection, HTTPS-ready, resilience via **Resilience4j** (circuit breaker + retry for AI calls).

### 📊 Data & Insights
- Full-text search on donation listings (PostgreSQL).
- Impact dashboard: total donations, meals rescued, CO₂ saved, families served.
- Scheduled job that auto-expires stale donations.
- Health/readiness endpoints (`/actuator/health`), Prometheus metrics.

---

## 🛠 Tech Stack

| Layer        | Technology |
|--------------|------------|
| Backend      | Java 17, Spring Boot 3.2, Spring MVC, Spring Security, Spring Data JPA |
| Frontend     | Thymeleaf, Tailwind CSS, vanilla JS |
| Database     | PostgreSQL 16 (+ Flyway versioned migrations) |
| Cache/Session| Redis 7 (sessions, cache, distributed rate-limit buckets) |
| Real-Time    | WebSocket + STOMP (in-memory simple broker) |
| AI           | Groq API — `llama-3.3-70b-versatile` via `OpenAI-compatible` client |
| Resilience   | Resilience4j circuit breaker + retry |
| Email        | JavaMail (Mailpit in dev, any SMTP in prod) |
| API Docs     | springdoc / OpenAPI (Swagger UI) |
| Ops          | Docker & Docker Compose, Actuator, Prometheus metrics |
| CI/CD        | GitHub → Render (Docker web service) |

---

## 💼 Business Impact

| Metric | Impact |
|--------|--------|
| **Food rescued** | Surplus that would be landfilled is redirected to feeding programs |
| **CO₂ reduction** | Every kg of food saved avoids methane from landfill decomposition |
| **NGO efficiency** | Pickup logistics cut from days to minutes — less food expires |
| **Donor engagement** | CSR tax-benefit + brand value from visible, measurable giving |
| **Trust & safety** | Role-based access, moderation controls, and auditability for admins |
| **Scalability** | Redis-backed sessions/rate-limiting → horizontal scaling ready |

**Verdict:** FoodShare turns wasted food into saved meals — a high-leverage, low-cost, AI-accelerated solution for one of the most tractable climate + hunger problems.

---

## 🚀 Getting Started (Local Development)

### Prerequisites
- Docker + Docker Compose
- A [Groq API key](https://console.groq.com/keys) (free)

### 1. Set up secrets
```bash
cp .env.example .env
```
Then edit `.env` and paste your real values:
```
GROQ_API_KEY=gsk_...
JWT_SECRET=<openssl rand -base64 48>
```

### 2. Run the stack
```bash
docker compose up -d --build
```

### 3. Open the app
- **App:** http://localhost:8080
- **Mailpit (dev inbox):** http://localhost:8025
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **Health:** http://localhost:8080/actuator/health

### Demo Accounts (auto-seeded)
| Role  | Email             | Password      |
|-------|-------------------|---------------|
| Admin | `admin@demo.com`  | `password123` |
| Donor | `donor@demo.com`  | `password123` |
| NGO   | `ngo@demo.com`    | `password123` |

---

## 🧪 Testing

```bash
./mvnw.cmd test        # unit + integration tests
docker exec fooddonation-postgres psql -U root -d food_donation_db   # inspect data
```

---

## 🌐 Deployment (Render)

The app deploys as a Docker web service. Required environment variables:

| Variable | Description |
|----------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<host>:5432/<db>?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | Render PostgreSQL user |
| `SPRING_DATASOURCE_PASSWORD` | Render PostgreSQL password |
| `SPRING_DATA_REDIS_HOST` | Redis host (e.g. Upstash) |
| `SPRING_DATA_REDIS_PORT` | `6379` |
| `SPRING_DATA_REDIS_PASSWORD` | Redis token |
| `SPRING_DATA_REDIS_SSL_ENABLED` | `true` for Upstash |
| `GROQ_API_KEY` | Groq API key |
| `JWT_SECRET` | Random base64 secret |
| `APP_BASE_URL` | `https://<your-app>.onrender.com` |
| `APP_MAIL_ENABLED` | `false` when no SMTP |
| `JAVA_OPTS` | `-Xmx192m -XX:MaxMetaspaceSize=192m -XX:ReservedCodeCacheSize=64m -Xss512k -XX:+UseSerialGC -XX:MaxDirectMemorySize=32m` (free tier) |

Health check path: `/actuator/health`

---

## 📁 Project Structure

```
src/main/java/com/app/fooddonation/
├── config/        # Security, WebSocket, cache, rate-limit, seeding
├── controller/    # Web + REST API controllers
├── dto/           # Request/response records
├── event/         # Application events (donation created, …)
├── exception/     # Domain exceptions + global handler
├── health/        # Custom health indicators
├── integration/   # Groq AI client
├── job/           # Scheduled jobs (expiry cleanup)
├── model/         # JPA entities & enums
├── ratelimit/     # Token-bucket (Redis/local)
├── repository/    # Spring Data repositories
├── security/      # JWT, login protection, access handlers
└── service/       # Business logic
```

Database schema is versioned with **Flyway** migrations in `src/main/resources/db/migration/`.

---

## 📜 License

This project is for educational/demo purposes.

---

Built with ❤️ by Atanu — *let's end food waste, one meal at a time.*
