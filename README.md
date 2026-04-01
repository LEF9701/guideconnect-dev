# GuideConnect

A web platform connecting tourists with local tour guides. Built with Java Spring Boot, Thymeleaf, and H2 (for testing, in-memory database).


---

## Tech Stack

| Layer       | Technology                          |
|-------------|-------------------------------------|
| Backend     | Java 17+, Spring Boot 3.4.3        |
| Frontend    | Thymeleaf, Bootstrap 5             |
| Database    | H2 (in-memory) / PostgreSQL        |
| Security    | Spring Security (session-based, BCrypt) |
| Build       | Maven                              |

---

## Prerequisites

- **Java 17** or higher (`java -version`)
- **Maven** (or use the included `mvnw` wrapper)

No external database required — H2 runs in-memory by default.

---

## Quick Start

```bash
# Clone the repository
git clone <repository-url>
cd guideconnect-dev-main

# Build and run
./mvnw spring-boot:run
```

The application starts at **http://localhost:8080**

---

## Running on GitHub Codespaces

```bash
# Install Java 17
sudo apt update && sudo apt install -y openjdk-17-jdk

# Make Maven wrapper executable
chmod +x mvnw

# Run the application
./mvnw spring-boot:run
```

Access via the Ports tab (port 8080) or the auto-generated Codespaces URL.

### Codespaces-Specific Configuration

If you encounter login redirect issues (`:8080` appended to URL), ensure `application.properties` contains:

```properties
server.forward-headers-strategy=framework
```

---

## Seed Data

The application auto-seeds on first startup with:

| Data           | Count  |
|----------------|--------|
| Users          | 101 (1 admin, 50 tourists, 50 guides) |
| Tour Listings  | 130+   |
| Bookings       | 1,050+ |
| Reviews        | 500+   |
| Transactions   | 600+   |
| Messages       | 400+   |
| Disputes       | 5      |

### Test Accounts

All accounts use the password: `password123`

| Role    | Email                      |
|---------|----------------------------|
| Admin   | admin@guideconnect.com     |
| Tourist | tourist1@test.com          |
| Guide   | guide1@test.com            |

Replace `1` with any number from 1 to 50 for additional tourist/guide accounts.

---

## H2 Database Console

Access the in-memory database at: **http://localhost:8080/h2-console**

| Field        | Value                       |
|--------------|-----------------------------|
| JDBC URL     | `jdbc:h2:mem:guideconnect`  |
| User Name    | `sa`                        |
| Password     | *(leave blank)*             |

---

## Project Structure

```
src/main/java/com/guideconnect/
├── config/            # SecurityConfig, WebConfig, DataSeeder
├── controller/        # 12 controllers (Auth, Tourist, Guide, Tour, Booking, etc.)
├── model/             # 7 JPA entities + 4 enums
├── repository/        # 7 Spring Data JPA repositories
├── service/           # 7 service classes (business logic)
└── GuideConnectApplication.java

src/main/resources/
├── templates/         # 32 Thymeleaf templates
│   ├── admin/         # Dashboard, users, disputes, reports
│   ├── auth/          # Login, register
│   ├── booking/       # Request, detail, messages, payment
│   ├── fragments/     # Header, footer (reusable)
│   ├── guide/         # Dashboard, profile, tours, requests, earnings
│   ├── tourist/       # Dashboard, profile, bookings
│   ├── tour/          # Search, detail
│   ├── review/        # Review form
│   └── error/         # 403, 500
├── static/
│   ├── css/style.css  # Custom styles (orange/navy theme)
│   └── js/main.js     # Client-side interactions
└── application.properties
```

---

## Features

### Tourist
- Browse and search tours (by city, price, language, category)
- Book tours and negotiate with guides via messaging
- Submit reviews and star ratings after tour completion
- View booking history and manage profile

### Guide
- Create, edit, and manage tour listings
- Accept/reject/negotiate booking requests
- View earnings and transaction history
- Manage profile (biography, languages)

### Admin
- Dashboard with platform-wide metrics
- User management (activate, suspend, ban accounts)
- Dispute resolution for flagged messages
- Platform reports

---

## Design Patterns

| Pattern           | Implementation                                              |
|-------------------|-------------------------------------------------------------|
| MVC               | Spring Boot controllers, Thymeleaf views, JPA models        |
| Repository        | Spring Data JPA repositories with custom JPQL queries        |
| Service Layer     | Business logic encapsulated in `@Service` classes            |
| State             | Booking lifecycle transitions (`canTransitionTo()` method)   |
| Strategy          | Tour search sorting (relevance, price asc/desc, rating)     |
| Observer          | Rating recalculation triggered on review submission          |
| Template Method   | Thymeleaf fragment-based page composition                    |

---

## Booking Lifecycle (State Pattern)

```
REQUESTED → NEGOTIATING → CONFIRMED → COMPLETED
    ↓            ↓            ↓
 REJECTED    CANCELLED    CANCELLED
```

Valid transitions are enforced by `Booking.canTransitionTo()`.

---

## Security

- BCrypt password hashing (work factor 12)
- Role-based URL authorization (`TOURIST`, `GUIDE`, `ADMIN`)
- Session-based authentication with 30-minute timeout
- CSRF protection (disabled for Codespaces compatibility)
- Custom login success handler with role-based redirects

---

## Switching to PostgreSQL

To use PostgreSQL instead of H2:

1. Start PostgreSQL and create a database named `guideconnect`
2. Update `pom.xml` — replace the H2 dependency with:
   ```xml
   <dependency>
       <groupId>org.postgresql</groupId>
       <artifactId>postgresql</artifactId>
       <scope>runtime</scope>
   </dependency>
   ```
3. Update `application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/guideconnect
   spring.datasource.username=postgres
   spring.datasource.password=postgres
   spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
   ```

Or use Docker:
```bash
docker compose up -d
```

---

## API Endpoints

| Method | Path                        | Access    | Description                  |
|--------|-----------------------------|-----------|------------------------------|
| GET    | `/`                         | Public    | Landing page                 |
| GET    | `/auth/login`               | Public    | Login page                   |
| GET    | `/auth/register`            | Public    | Registration page            |
| GET    | `/tours/search`             | Public    | Tour search with filters     |
| GET    | `/tours/{id}`               | Public    | Tour detail page             |
| GET    | `/tourist/dashboard`        | Tourist   | Tourist dashboard            |
| GET    | `/tourist/profile`          | Tourist   | Tourist profile              |
| GET    | `/guide/dashboard`          | Guide     | Guide dashboard              |
| GET    | `/guide/profile`            | Guide     | Guide profile                |
| POST   | `/guide/tours/new`          | Guide     | Create new tour              |
| GET    | `/guide/requests`           | Guide     | View booking requests        |
| POST   | `/bookings/request`         | Tourist   | Submit booking request       |
| POST   | `/bookings/{id}/status`     | Auth      | Update booking status        |
| GET    | `/bookings/{id}/messages`   | Auth      | Booking chat                 |
| POST   | `/reviews`                  | Tourist   | Submit a review              |
| GET    | `/admin/dashboard`          | Admin     | Admin dashboard              |
| GET    | `/admin/users`              | Admin     | User management              |
| GET    | `/admin/disputes`           | Admin     | Dispute management           |
