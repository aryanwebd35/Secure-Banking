# 📚 Banking App — Complete Learning Guide

> **For Aryan** | Java + Spring Boot beginner's guide to understanding this project from scratch.
> Written in easy language. No prior Java experience required!

---

## 🗺️ Table of Contents

1. [What is this project?](#-what-is-this-project)
2. [The Big Picture — How it all works](#-the-big-picture)
3. [Concept: The 5 Layers of Spring Boot](#-concept-the-5-layers-of-spring-boot)
4. [Reading Order — File by File](#-reading-order-file-by-file)
5. [Key Annotations Cheat Sheet](#-key-annotations-cheat-sheet)
6. [How a Request flows through the app](#-how-a-request-flows-through-the-app)
7. [Authentication Deep Dive](#-authentication-deep-dive)
8. [Database Tables Diagram](#-database-tables-diagram)
9. [API Endpoints Reference](#-api-endpoints-reference)
10. [How to Run the Project](#-how-to-run-the-project)
11. [Common Beginner Questions](#-common-beginner-questions)

---

## 🏦 What is this project?

This is a **Secure Digital Banking Backend** — a REST API that powers a banking application where:

- Users can **sign up / log in** via Clerk (like Google Sign-In)
- Users can **deposit**, **withdraw**, and **transfer money** using phone numbers
- Admins can **manage accounts** (block/unblock/close), view stats, etc.
- Every action is **secured** — you must be logged in with a valid JWT token

**Tech Stack:**
| Tool | What it does |
|------|-------------|
| Java 17 | Programming language |
| Spring Boot 4.x | Framework that makes Java apps easy |
| MySQL | Database that stores users, accounts, transactions |
| Spring Security | Authentication & authorization |
| JWT (JSON Web Token) | Stateless login tokens |
| Clerk | Third-party login service (like Auth0) |
| Spring Data JPA | Makes database queries easy |
| Lombok | Reduces boilerplate code |
| Swagger (OpenAPI) | Auto-generated API documentation |

---

## 🌐 The Big Picture

```
FRONTEND (React / Next.js)
        |
        |  HTTP Request (e.g., POST /transfer with JWT token)
        v
SPRING BOOT BACKEND (this project)
        |
        |-- 1. JwtAuthenticationFilter -> checks if JWT is valid
        |
        |-- 2. SecurityConfig -> is this endpoint public or restricted?
        |
        |-- 3. Controller -> receives the HTTP request
        |
        |-- 4. Service -> runs the business logic
        |
        |-- 5. Repository -> talks to the database
        |
        --> 6. MySQL Database -> stores all data
```

---

## 🧱 Concept: The 5 Layers of Spring Boot

Spring Boot apps are organized in layers. Think of it like a restaurant:

```
CUSTOMER (Frontend / Postman)
  Makes HTTP requests (orders from the menu)

CONTROLLER LAYER (Waiter)
  Receives HTTP requests, validates input, calls service
  Files: AccountController, AuthController, etc.

SERVICE LAYER (Kitchen / Chef)
  Contains all business logic - "what should actually happen"
  Files: AccountServiceImpl, TransactionServiceImpl, etc.

REPOSITORY LAYER (Storage Room Manager)
  Talks directly to the MySQL database
  Files: AccountRepository, UserRepository, etc.

DATABASE (The actual storage)
  MySQL tables: users, accounts, transactions, roles, etc.
```

**In between each layer:**
- **Controller <-> Service**: uses **DTOs** (Data Transfer Objects) — simple containers for data
- **Service <-> Database**: uses **Entities** — Java objects that map to MySQL tables
- **Mappers** convert DTOs <-> Entities

---

## 📂 Reading Order — File by File

**Start here and read in this exact order. Each file builds on the previous!**

---

### 🟢 STEP 1: Entry Point (Start here!)
**File:** `src/main/java/net/javaguides/Banking_app/BankingAppApplication.java`

This is where EVERYTHING starts. The `main()` method is the first thing Java runs.
- **Key concept:** `@SpringBootApplication` = 3 annotations in one
- After reading this, you know: "Spring Boot starts here and auto-configures everything"

---

### 🟢 STEP 2: Application Settings
**File:** `src/main/resources/application.properties`

This is the configuration file. No Java code here — just settings.
- Database URL, username, password
- JWT secret key and expiry time
- Email (SMTP) settings for sending OTPs
- Admin email and password hash

---

### 🟡 STEP 3: Entities (Database Tables)
These files represent your MySQL tables. Read them in this order:

| Order | File | What it represents |
|-------|------|--------------------|
| 1 | `entity/BaseEntity.java` | Parent class — adds `createdAt`, `updatedAt` to all tables |
| 2 | `entity/Role.java` | The `roles` table — ROLE_USER or ROLE_ADMIN |
| 3 | `entity/User.java` | The `users` table — who is registered |
| 4 | `entity/Account.java` | The `accounts` table — bank accounts |
| 5 | `entity/AccountStatus.java` | Enum: ACTIVE, BLOCKED, CLOSED |
| 6 | `entity/Transaction.java` | The `transactions` table — every money movement |
| 7 | `entity/TransactionType.java` | Enum: DEPOSIT, WITHDRAW, TRANSFER |
| 8 | `entity/TransactionStatus.java` | Enum: SUCCESS, FAILED |
| 9 | `entity/OtpVerification.java` | The `otp_verifications` table — OTP codes |

**Key concepts to learn here:** `@Entity`, `@Table`, `@Id`, `@Column`, `@OneToOne`, `@ManyToMany`, `@Enumerated`, Lombok annotations

---

### 🟡 STEP 4: Repositories (Database Access)
These talk directly to MySQL. Spring Data generates the SQL for you!

| Order | File | Purpose |
|-------|------|---------|
| 1 | `repository/UserRepository.java` | Find users by email, phone, Clerk ID |
| 2 | `repository/RoleRepository.java` | Find roles by name |
| 3 | `repository/AccountRepository.java` | Find accounts by email, phone, Clerk ID + sum balances |
| 4 | `repository/TransactionRepository.java` | Find transaction history with date filtering |
| 5 | `repository/OtpVerificationRepository.java` | Find and delete OTP records |

**Key concepts:** `JpaRepository`, Spring Data method naming, `@Query`, `Optional<T>`, `Page<T>`

---

### 🟡 STEP 5: Config
Two simple configuration classes:

| File | Purpose |
|------|---------|
| `config/JpaConfig.java` | Enables automatic `createdAt`/`updatedAt` timestamps |
| `config/OpenApiConfig.java` | Sets up Swagger UI (interactive API docs at /swagger-ui) |

---

### 🔴 STEP 6: Security
This is the most complex part. Read carefully!

| Order | File | Purpose |
|-------|------|---------|
| 1 | `security/JwtTokenProvider.java` | Creates and validates JWT tokens |
| 2 | `security/JwtAuthenticationFilter.java` | Reads JWT from every HTTP request header |
| 3 | `security/ClerkTokenValidator.java` | Validates Clerk's JWT using RSA public keys |
| 4 | `security/SecurityConfig.java` | Master security rules (who can access what) |

**Key concepts:** JWT, BCrypt, `@Component`, `SecurityFilterChain`, CORS, STATELESS sessions

---

### 🟡 STEP 7: Mappers (Translators)
These convert between Entities (DB) and DTOs (API):

| File | Purpose |
|------|---------|
| `mapper/UserMapper.java` | User Entity <-> UserDto |
| `mapper/AccountMapper.java` | Account Entity <-> AccountDto |
| `mapper/TransactionMapper.java` | Transaction Entity -> TransactionDto |

---

### 🟡 STEP 8: Exceptions (Error Handling)

| File | When is it thrown? | HTTP Status |
|------|--------------------|-------------|
| `exception/AccountNotFoundException.java` | Account ID doesn't exist | 404 Not Found |
| `exception/UserNotFoundException.java` | User email doesn't exist | 404 Not Found |
| `exception/InsufficientBalanceException.java` | Balance too low | 400 Bad Request |
| `exception/AccountBlockedException.java` | Account is blocked/closed | 403 Forbidden |
| `exception/InvalidTransactionException.java` | Invalid transaction (amount <= 0) | 400 Bad Request |
| `exception/UnauthorizedAccessException.java` | Not authorized | 401 Unauthorized |
| `exception/GlobalExceptionHandler.java` | Catches ALL exceptions globally | Varies |

---

### 🟢 STEP 9: Service Interfaces
These define WHAT operations exist (not HOW):

| File | What it defines |
|------|----------------|
| `service/UserService.java` | Register/update user, setup phone, list users |
| `service/AccountService.java` | Create/get/deposit/withdraw/delete accounts, dashboard |
| `service/TransactionService.java` | Transfer, deposit, withdraw, history, by ID |
| `service/AdminService.java` | Admin list users/accounts, update status, dashboard stats |

---

### 🟠 STEP 10: Service Implementations (The real logic!)
These contain the ACTUAL business logic — read these carefully!

| Order | File | Complexity |
|-------|------|-----------|
| 1 | `service/impl/UserServiceImpl.java` | Beginner-friendly, good starting point |
| 2 | `service/impl/AdminServiceImpl.java` | Simple, short, good to read |
| 3 | `service/impl/AccountServiceImpl.java` | Medium complexity |
| 4 | `service/impl/TransactionServiceImpl.java` | **Most complex** — the heart of banking logic |

**Key concepts:** `@Service`, `@Transactional`, `@Lazy`, Java Stream API, circular dependency

---

### 🟢 STEP 11: Controllers (The API endpoints!)
These are the HTTP endpoints that the frontend calls:

| Order | File | Handles |
|-------|------|---------|
| 1 | `controller/AuthController.java` | Login (admin, Clerk, demo) and phone setup |
| 2 | `controller/AccountController.java` | Account CRUD operations |
| 3 | `controller/TransactionController.java` | Transfers, history, dashboard |
| 4 | `controller/AdminController.java` | Admin-only endpoints |

---

## 🏷️ Key Annotations Cheat Sheet

### Class-Level Annotations

| Annotation | Meaning | Where used |
|-----------|---------|-----------|
| `@SpringBootApplication` | Start of the app. Auto-configure everything. | `BankingAppApplication.java` |
| `@Entity` | This class = a MySQL table | All entity files |
| `@Table(name="x")` | The MySQL table is named "x" | All entity files |
| `@MappedSuperclass` | This class is inherited (not its own table) | `BaseEntity.java` |
| `@Service` | This is a business logic bean | All ServiceImpl files |
| `@Repository` | This is a database access bean | Repository files (optional) |
| `@RestController` | This handles HTTP and returns JSON | All Controller files |
| `@Configuration` | This class has Spring settings | Config files |
| `@Component` | A general Spring bean | Security files |
| `@ControllerAdvice` | Global exception handler | `GlobalExceptionHandler.java` |
| `@EnableWebSecurity` | Activate Spring Security | `SecurityConfig.java` |
| `@EnableJpaAuditing` | Auto-fill createdAt/updatedAt | `JpaConfig.java` |
| `@Slf4j` | Auto-create log variable (Lombok) | Service & Controller files |
| `@Getter / @Setter` | Auto-generate getters/setters (Lombok) | Entity files |
| `@NoArgsConstructor` | Auto-generate empty constructor (Lombok) | Entity files |
| `@AllArgsConstructor` | Auto-generate all-args constructor (Lombok) | Entity files |

### Field-Level Annotations

| Annotation | Meaning |
|-----------|---------|
| `@Id` | This field is the primary key |
| `@GeneratedValue(strategy=IDENTITY)` | Auto-increment the ID (1, 2, 3...) |
| `@Column(name="x", nullable=false)` | Map to column "x", cannot be null |
| `@Enumerated(EnumType.STRING)` | Store enum as text (not a number) |
| `@OneToOne` | This table has one related record in another table |
| `@ManyToMany` | Multiple records link to multiple records |
| `@JoinColumn` | Defines the foreign key column |
| `@JoinTable` | Creates a join/bridge table (for ManyToMany) |
| `@CreatedDate` | Auto-set to NOW when first saved |
| `@LastModifiedDate` | Auto-update to NOW every time saved |
| `@Value("${property}")` | Read from application.properties |

### Method / Parameter Annotations

| Annotation | Meaning |
|-----------|---------|
| `@GetMapping("/path")` | Handle HTTP GET to this path |
| `@PostMapping("/path")` | Handle HTTP POST to this path |
| `@PutMapping("/path")` | Handle HTTP PUT to this path |
| `@DeleteMapping("/path")` | Handle HTTP DELETE to this path |
| `@Transactional` | Wrap in DB transaction (all-or-nothing) |
| `@ExceptionHandler(X.class)` | Handle exception of type X |
| `@Bean` | This method returns a Spring-managed object |
| `@Override` | This method overrides a parent method |
| `@PathVariable` | Read {id} from URL: `/api/accounts/{id}` |
| `@RequestBody` | Read JSON body and convert to Java object |
| `@RequestParam` | Read query parameters: `?page=0&size=10` |
| `@Valid` | Validate the object using its rules |

---

## 🔄 How a Request flows through the app

**Example: User wants to check their account balance**

```
Step 1: FRONTEND
  GET http://localhost:8080/api/accounts/my
  Header: Authorization: Bearer eyJhbGci...

Step 2: JwtAuthenticationFilter (runs FIRST on every request)
  - Reads "Authorization" header
  - Extracts token: "eyJhbGci..."
  - Validates token: correct signature? not expired?
  - Extracts email from token: "aryan@gmail.com"
  - Sets user as "authenticated" in Spring Security

Step 3: SecurityConfig checks
  - Is /api/accounts/my in public routes? NO
  - Does user have valid token? YES
  - Allow request to proceed

Step 4: AccountController.getMyAccount()
  - @GetMapping("/my") handles this URL
  - Gets email from Authentication object
  - Calls: accountService.getMyAccount("aryan@gmail.com")

Step 5: AccountServiceImpl.getMyAccount()
  - Calls: accountRepository.findByUserEmail("aryan@gmail.com")
  - Gets Optional<Account>
  - If empty -> throws AccountNotFoundException -> GlobalExceptionHandler -> 404 response
  - If found -> converts Account entity -> AccountDto using AccountMapper

Step 6: Response
  - Returns: HTTP 200 OK + JSON:
    {
      "id": 3,
      "accountHolderName": "Aryan Sharma",
      "balance": 1500.0,
      "accountStatus": "ACTIVE"
    }
```

---

## 🔐 Authentication Deep Dive

### Two Types of Login

**Admin Login (Email + Password):**
```
POST /api/auth/admin/login
Body: { "email": "admin@bank.com", "password": "adminPassword123" }

Server:
  1. Reads admin.email from application.properties
  2. Compares password to stored BCrypt hash using passwordEncoder.matches()
  3. If match -> creates JWT with ROLE_ADMIN
  4. Returns JWT

Frontend stores JWT and uses it for all /admin/** requests.
```

**Regular User Login (via Clerk):**
```
1. User clicks "Sign In with Google" (handled by Clerk on frontend)
2. Clerk gives a Clerk JWT to the frontend
3. Frontend sends: POST /api/auth/clerk-login
   Body: { "clerkToken": "<clerk's JWT>" }

4. ClerkTokenValidator fetches Clerk's public keys from JWKS URL
5. Verifies Clerk JWT signature (RSA verification)
6. Extracts: clerkUserId, name, email from Clerk token
7. UserService.registerOrUpdateUser() creates/updates user in OUR database
8. Returns our own JWT with ROLE_USER + needsPhoneSetup flag

If needsPhoneSetup = true:
9. Frontend shows "Enter your phone number" screen
10. POST /api/auth/setup-phone with { "phoneNumber": "9876543210" }
11. UserService.setupPhone() saves phone AND creates bank account
12. Returns fresh JWT, user is fully onboarded!
```

---

## 🗄️ Database Tables Diagram

```
users                          roles
+------------------+           +------------------+
| id (PK)          |           | id (PK)          |
| clerk_user_id    |           | name (UNIQUE)    |
| name             |           +------------------+
| email (UNIQUE)   |
| phone_number     |           user_roles (JOIN)
| password         |           +------------------+
| created_at       |           | user_id (FK)     |
| updated_at       |           | role_id (FK)     |
+------------------+           +------------------+

accounts                       transactions
+------------------+           +----------------------+
| id (PK)          |           | id (PK)              |
| account_holder   |           | transaction_type     |
| balance          |           | amount               |
| user_id (FK)     |           | sender_account_id FK |
| account_status   |           | receiver_account_id  |
| created_at       |           | status               |
| updated_at       |           | timestamp            |
+------------------+           | remarks              |
                               | created_at           |
                               +----------------------+

otp_verifications
+------------------+
| id (PK)          |
| email            |
| name             |
| otp_code (6)     |
| created_at       |
| expires_at       |
| verified         |
+------------------+
```

---

## 🛠️ API Endpoints Reference

### Auth Endpoints (Public — no JWT required)
| Method | URL | What it does |
|--------|-----|-------------|
| POST | `/api/auth/admin/login` | Admin login with email+password |
| POST | `/api/auth/clerk-login` | User login with Clerk token |
| POST | `/api/auth/setup-phone` | Set phone number (requires JWT) |
| POST | `/api/auth/simulated-login` | Demo/guest login (no real auth) |

### Account Endpoints (Requires JWT)
| Method | URL | What it does |
|--------|-----|-------------|
| GET | `/api/accounts/my` | Get logged-in user's account |
| PUT | `/api/accounts/my/deposit` | Deposit to own account |
| PUT | `/api/accounts/my/withdraw` | Withdraw from own account |
| POST | `/api/accounts` | Create a new account |
| GET | `/api/accounts/{id}` | Get account by ID |
| GET | `/api/accounts` | List all accounts |
| DELETE | `/api/accounts/{id}` | Delete an account |

### Transaction Endpoints (Requires JWT)
| Method | URL | What it does |
|--------|-----|-------------|
| POST | `/transfer` | Transfer money by phone number |
| GET | `/transactions/history` | Get transaction history (paginated) |
| GET | `/transactions/{id}` | Get one transaction by ID |
| GET | `/dashboard` | Get account dashboard |

### Admin Endpoints (Requires ROLE_ADMIN)
| Method | URL | What it does |
|--------|-----|-------------|
| GET | `/admin/users` | List all users (paginated) |
| GET | `/admin/accounts` | List all accounts (paginated) |
| GET | `/admin/transactions` | List all transactions (paginated) |
| PUT | `/admin/block-account/{id}` | Block an account |
| PUT | `/admin/unblock-account/{id}` | Unblock an account |
| PUT | `/admin/close-account/{id}` | Close an account permanently |
| GET | `/admin/dashboard` | Admin dashboard statistics |

---

## 🚀 How to Run the Project

### Prerequisites
1. Install **Java 17+** (check: `java --version`)
2. Install **MySQL** and create a database called `banking-app`

### Steps
```bash
# 1. Update application.properties with YOUR MySQL password
# Change: spring.datasource.password=admin@1234
# To your actual MySQL root password

# 2. Run the app (from the project root folder)
# On Windows:
mvnw.cmd spring-boot:run

# OR in IntelliJ IDEA:
# Right-click BankingAppApplication.java -> Run 'BankingAppApplication'
```

### After starting:
- API is live at: `http://localhost:8080`
- **Swagger docs at: `http://localhost:8080/swagger-ui/index.html`** ← Try your APIs here!
- Health check at: `http://localhost:8080/actuator/health`

---

## ❓ Common Beginner Questions

### Q: Why do we have both an Interface and an Impl class for Services?
**A:** The Interface defines WHAT operations exist. The Impl defines HOW they work.
This is a design pattern called **"Program to an interface, not an implementation."**
Benefits:
- Easier testing (you can swap the Impl with a fake/mock one)
- Controller doesn't need to know implementation details
- Professional best practice in Java

### Q: What is an Optional<T>?
**A:** `Optional<Account>` is a wrapper that either contains an Account or is empty.
Instead of: `Account account = repo.findById(id)` (could be null and crash!)
We use: `Optional<Account> opt = repo.findById(id)`
Then: `opt.orElseThrow(() -> new AccountNotFoundException("..."))` (throws a clear error if empty)

### Q: What does @Transactional actually do?
**A:** It wraps your method in a database transaction.
- If ALL steps succeed -> ALL changes are COMMITTED (saved) to the database.
- If ANY step fails -> ALL changes are ROLLED BACK (undone).
- This is CRITICAL for money transfers — you never want to deduct from sender but fail to add to receiver!

### Q: What is the stream() API I see everywhere?
**A:** It's Java's way to process collections (lists) in a functional style.
```java
accounts.stream()
    .filter(a -> a.getBalance() > 0)      // keep only accounts with balance
    .map(AccountMapper::mapToAccountDto)   // convert each Account -> AccountDto
    .collect(Collectors.toList())          // put all AccountDtos into a List
```

### Q: What is a DTO vs Entity?
**A:**
- **Entity** = Java class mapped to a database table. May have sensitive/internal fields.
- **DTO** (Data Transfer Object) = Simple container for sending/receiving data via API.
- We use DTOs to control WHAT data goes in/out of the API. For example, password hash is in the User entity but NOT in the UserDto.

### Q: What is Lombok?
**A:** A Java library that auto-generates boilerplate code.
Without Lombok: every entity class needs 50+ lines of getters/setters/constructors.
With Lombok: just add `@Getter @Setter @NoArgsConstructor @AllArgsConstructor` annotations!
The code is generated at compile time — you don't see it but it's there.

### Q: What is JWT and why use it?
**A:** JWT = JSON Web Token. It's like a concert wristband.
- After login, server gives you a JWT.
- Every request, you show your JWT in the Authorization header: `Bearer <token>`
- Server verifies the JWT is real (checks signature) — no database lookup needed!
- This is "stateless" — server doesn't need to remember sessions at all.

### Q: What is @Lazy and why is it used in AccountServiceImpl?
**A:** AccountServiceImpl uses TransactionService, and TransactionServiceImpl uses AccountRepository.
When Spring starts, it tries to create AccountServiceImpl first but needs TransactionService,
which needs AccountRepository... and there's a circular dependency (A needs B, B needs A).
`@Lazy` tells Spring: "Don't inject TransactionService right now at startup.
Inject it later, the first time a method that needs it is actually called."
This breaks the circular chain!

---

## 🎯 Your Study Plan (7 Days)

| Day | What to read | Goal |
|-----|-------------|------|
| Day 1 | Steps 1-2 (Entry point + properties) | Understand how the app starts |
| Day 2 | Step 3 (All Entities) | Understand the database structure |
| Day 3 | Steps 4-5 (Repositories + Config) | Understand how DB queries work |
| Day 4 | Steps 6 (Security) | Understand JWT + authentication |
| Day 5 | Steps 7-9 (Mappers + Exceptions + Service interfaces) | Understand the architecture |
| Day 6 | Step 10 (Service implementations) | Read actual business logic |
| Day 7 | Step 11 (Controllers) + Use Swagger UI | Test the entire API! |
| Bonus | Try adding a feature | Solidify your learning |

> 💡 **Pro Tip:** Always read the comments in each file — they explain every line in easy language!
> Use the Swagger UI at `http://localhost:8080/swagger-ui/index.html` to test APIs without Postman.
