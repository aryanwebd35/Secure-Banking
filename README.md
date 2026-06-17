# Developer Guide & Architectural Walkthrough: SecureBank Application

Welcome to your first Java and Spring Boot project! This document is a complete, beginner-friendly guide that explains the architecture of your banking application, the core concepts of the Spring Boot framework, how each component of your codebase functions, how to read the code, and how to deploy it on GitHub, Vercel, and Railway.

---

## Table of Contents
1. [Where to Start: Suggested Code-Reading Order](#1-where-to-start-suggested-code-reading-order)
2. [How the Files are Connected (Visualizing a Request)](#2-how-the-files-are-connected-visualizing-a-request)
3. [Spring Annotation Reference (The Cheat Sheet)](#3-spring-annotation-reference-the-cheat-sheet)
4. [Deployment Guide: GitHub & Vercel & Railway](#4-deployment-guide-github--vercel--railway)
5. [Core Spring Boot Concepts Explained](#5-core-spring-boot-concepts-explained)
6. [Project Architecture & Directory Structure](#6-project-architecture--directory-structure)
7. [Transactional Business Logic (Deep Dive: Fund Transfers)](#7-transactional-business-logic-deep-dive-fund-transfers)
8. [Frontend SPA Architecture](#8-frontend-spa-architecture)

---

## 1. Where to Start: Suggested Code-Reading Order

When looking at a Spring Boot project for the first time, the massive amount of files can feel overwhelming. Do not read the packages alphabetically! Instead, follow this step-by-step path to build your understanding:

### Step 1: Start at the Configuration & Build Definitions
1. Open [pom.xml](file:///c:/Users/aryan/OneDrive/Desktop/CODE/Java&Spring%20Boot/Banking-app/Banking-app/pom.xml): This lists all the external libraries (starters) imported to support web APIs, databases, security, and emails.
2. Open [application.properties](file:///c:/Users/aryan/OneDrive/Desktop/CODE/Java&Spring%20Boot/Banking-app/Banking-app/src/main/resources/application.properties): Read through this to see how the app is set up (port 8080, MySQL login details, admin emails, etc.).

### Step 2: Understand the Database Tables (Entities)
Database tables are modeled as Java classes. Read these next:
1. Open [BaseEntity.java](file:///c:/Users/aryan/OneDrive/Desktop/CODE/Java&Spring%20Boot/Banking-app/Banking-app/src/main/java/net/javaguides/Banking_app/entity/BaseEntity.java) to see the shared timestamp fields.
2. Open [User.java](file:///c:/Users/aryan/OneDrive/Desktop/CODE/Java&Spring%20Boot/Banking-app/Banking-app/src/main/java/net/javaguides/Banking_app/entity/User.java) to see the structure of a customer profile.
3. Open [Account.java](file:///c:/Users/aryan/OneDrive/Desktop/CODE/Java&Spring%20Boot/Banking-app/Banking-app/src/main/java/net/javaguides/Banking_app/entity/Account.java) to see how accounts link to Users and store balances.

### Step 3: Follow the Flow of a Simple REST Endpoint (e.g. Balance/Account details)
See how data travels from the URL routing to the database and back:
1. **Controller**: Open [AccountController.java](file:///c:/Users/aryan/OneDrive/Desktop/CODE/Java&Spring%20Boot/Banking-app/Banking-app/src/main/java/net/javaguides/Banking_app/controller/AccountController.java#L35-L40) and locate `getMyAccount()`. It handles HTTP GET requests to `/api/accounts/my`.
2. **Service Interface**: Open [AccountService.java](file:///c:/Users/aryan/OneDrive/Desktop/CODE/Java&Spring%20Boot/Banking-app/Banking-app/src/main/java/net/javaguides/Banking_app/service/AccountService.java) to see the method signatures defining what business operations exist.
3. **Service Implementation**: Open [AccountServiceImpl.java](file:///c:/Users/aryan/OneDrive/Desktop/CODE/Java&Spring%20Boot/Banking-app/Banking-app/src/main/java/net/javaguides/Banking_app/service/impl/AccountServiceImpl.java#L132-L137) and read `getMyAccount()`. It queries the repository.
4. **Repository**: Open [AccountRepository.java](file:///c:/Users/aryan/OneDrive/Desktop/CODE/Java&Spring%20Boot/Banking-app/Banking-app/src/main/java/net/javaguides/Banking_app/repository/AccountRepository.java) to see how database query methods are declared.
5. **Mappers & DTOs**: Open [AccountMapper.java](file:///c:/Users/aryan/OneDrive/Desktop/CODE/Java&Spring%20Boot/Banking-app/Banking-app/src/main/java/net/javaguides/Banking_app/mapper/AccountMapper.java) and [AccountDto.java](file:///c:/Users/aryan/OneDrive/Desktop/CODE/Java&Spring%20Boot/Banking-app/Banking-app/src/main/java/net/javaguides/Banking_app/dto/AccountDto.java) to see how database objects are converted into lightweight containers to send back to the user.

---

## 2. How the Files are Connected (Visualizing a Request)

Let's look at how a user making a **Deposit** triggers the files in your project:

```
[1. User clicks 'Deposit' on Frontend SPA]
       │
       ▼ (Sends HTTP PUT to '/api/accounts/my/deposit' with header 'Authorization: Bearer <JWT>')
[2. Security Filters]
       │  ├─ JwtAuthenticationFilter.java ──► Extracts and validates JWT token signature.
       │  └─ SecurityConfig.java ───────────► Checks if user role is allowed to hit this url.
       ▼
[3. Controller (Presentation Layer)]
       │  └─ AccountController.java ────────► Map request body to variables, call service method.
       ▼
[4. Service Layer (Business Rules)]
       │  ├─ AccountServiceImpl.java ───────► Coordinate transaction logic.
       │  └─ TransactionServiceImpl.java ───► Validate balance, check if account is blocked.
       ▼
[5. Repository (Data Access)]
       │  └─ AccountRepository.java ────────► Retrieve and update SQL record via Spring Data.
       ▼
[6. Mapper Layer]
       │  └─ AccountMapper.java ────────────► Transform updated SQL Entity into a JSON DTO.
       ▼
[7. Response returned to Frontend]
```

---

## 3. Spring Annotation Reference (The Cheat Sheet)

Spring Boot uses **Annotations** (words starting with `@`) to tell the compiler to generate configuration code and intercept execution. Here are the most important annotations in your codebase:

| Annotation | Where it is found | What it means/does |
| :--- | :--- | :--- |
| `@SpringBootApplication` | [BankingAppApplication.java](file:///c:/Users/aryan/OneDrive/Desktop/CODE/Java&Spring%20Boot/Banking-app/Banking-app/src/main/java/net/javaguides/Banking_app/BankingAppApplication.java) | Marks this class as the launchpad of the application. It turns on auto-configuration and component scanning. |
| `@RestController` | Controllers (e.g., [AccountController.java](file:///c:/Users/aryan/OneDrive/Desktop/CODE/Java&Spring%20Boot/Banking-app/Banking-app/src/main/java/net/javaguides/Banking_app/controller/AccountController.java)) | Tells Spring this class exposes REST endpoints, and its methods return JSON data directly instead of loading HTML views. |
| `@Service` | Service Implementations | Marks the class as a business logic container in the Spring context. |
| `@Repository` | Repositories | Marks database helper interfaces. Spring generates SQL execution commands automatically. |
| `@Entity` | Entities (e.g., [Account.java](file:///c:/Users/aryan/OneDrive/Desktop/CODE/Java&Spring%20Boot/Banking-app/Banking-app/src/main/java/net/javaguides/Banking_app/entity/Account.java)) | Marks a class as a model class corresponding to a database table. |
| `@Table(name="...")` | Entities | Sets the specific SQL table name for this class in MySQL. |
| `@Id` & `@GeneratedValue` | Entities | Defines the primary key column and sets it to auto-increment. |
| `@OneToOne` / `@ManyToMany` | Entities | Declares database table relationships (foreign keys). |
| `@Transactional` | Service Implementations | Tells Spring to wrap the method in a database transaction. If any error happens, all database changes are rolled back automatically. |
| `@Configuration` | Configuration Classes | Tells Spring this is a settings file used to set up global configurations. |
| `@Bean` | Configuration Classes | Creates and registers a custom utility bean to the global Spring Context. |
| `@GetMapping` / `@PostMapping` | Controllers | Maps corresponding HTTP GET / POST calls to target Java methods. |
| `@RequestBody` | Controller Methods | Deserializes incoming JSON data in the HTTP body into a Java DTO. |
| `@PathVariable` | Controller Methods | Extracts values directly from variables embedded inside the url path (e.g. `/api/accounts/{id}`). |

---

## 4. Deployment Guide: GitHub & Vercel & Railway

Deploying a fullstack application involves setting up three components:
1. **GitHub**: Houses your source code.
2. **Vercel / GitHub Pages**: Hosts your static frontend files (`index.html`, `style.css`, `app.js`).
3. **Railway / Render**: Hosts your persistent Java Spring Boot backend web server and your MySQL database.

---

### Step 1: Pushing Code to GitHub

First, you need to push your project files to a GitHub repository:

1. Open a terminal inside your project directory `c:/Users/aryan/OneDrive/Desktop/CODE/Java&Spring Boot/Banking-app/Banking-app`.
2. Initialize Git:
   ```bash
   git init
   ```
3. Create a `.gitignore` file (Spring Boot initializer usually creates this). It ensures you do not upload compile files like `/target` or custom configurations.
4. Add all files to the staging area:
   ```bash
   git add .
   ```
5. Commit files:
   ```bash
   git commit -m "Initial banking app commit"
   ```
6. Open your browser, go to GitHub, create a new empty repository (do NOT check "Add a README" or ".gitignore"), and copy its remote URL (e.g., `https://github.com/yourusername/Banking-app.git`).
7. Link local Git to your GitHub repo and push:
   ```bash
   git branch -M main
   git remote add origin https://github.com/yourusername/Banking-app.git
   git push -u origin main
   ```

---

### Step 2: Deploying the Java Backend + MySQL Database on Railway

> [!NOTE]  
> Vercel is a serverless frontend hosting platform (mainly for React/Next.js/HTML). It does **not** natively support running a persistent Java Virtual Machine (JVM) web server. Therefore, you must host your Spring Boot backend on a server hosting provider like **Railway** or **Render**, and you can optionally host your frontend on Vercel.

**Railway** is the easiest option for beginners to host both Spring Boot and MySQL:

#### 1. Host the MySQL Database:
1. Log in to [Railway.app](https://railway.app) using your GitHub account.
2. Click **New Project** -> **Provision MySQL**.
3. Railway will spin up a MySQL database. Go to its settings and copy the **Connection URL** (e.g., `mysql://root:password@host:port/railway`).

#### 2. Host the Java Web Server:
1. Click **New Project** -> **Deploy from GitHub repo** and choose your repository.
2. Railway will analyze your project, see the `pom.xml`, and build it automatically using Maven.
3. Go to **Variables** on the service dashboard, and add your environmental variables to match your database values:
   * `SPRING_DATASOURCE_URL`: `jdbc:mysql://<your_railway_db_host>:<port>/railway`
   * `SPRING_DATASOURCE_USERNAME`: `root`
   * `SPRING_DATASOURCE_PASSWORD`: `<your_railway_db_password>`
   * `SPRING_JPA_HIBERNATE_DDL_AUTO`: `update`
   * `APP_JWT_SECRET`: `9a4f2c8d3b7a5e1f8c6b4d2e0f1a3c5e7g9h0i2j4k6l8m0n2o4p6q8r0s2t4u6v` (use a long random secret string)
   * `SPRING_MAIL_USERNAME`: `aryanchess5135@gmail.com`
   * `SPRING_MAIL_PASSWORD`: `ivqk bfxt nywx huuw`
4. Once deployed, Railway will generate a public domain URL for your backend (e.g., `https://banking-app-production.up.railway.app`).

---

### Step 3: Deploying the Frontend on Vercel (or hosting inside Spring Boot)

You have two design choices for the frontend deployment:

#### Option A: Unified Fullstack Deployment (Easiest)
Since your static files (`index.html`, `style.css`, `app.js`) are placed inside `src/main/resources/static/`, Spring Boot serves them automatically! 
When you deploy your backend on Railway, your frontend is already live at the same address (e.g., `https://banking-app-production.up.railway.app/index.html`). You do **not** need to do a separate Vercel deploy.

#### Option B: Decoupled Deploy (Frontend on Vercel)
If you want to host the frontend separately on Vercel:
1. **Edit Endpoint URLs**: In your local [app.js](file:///c:/Users/aryan/OneDrive/Desktop/CODE/Java&Spring%20Boot/Banking-app/Banking-app/src/main/resources/static/app.js), locate where you make API calls. Instead of relative links like `await fetch('/api/auth/clerk-login')`, update the base URL to your public Railway backend URL:
   ```javascript
   // Change:
   await fetch('/api/auth/clerk-login')
   // To:
   await fetch('https://banking-app-production.up.railway.app/api/auth/clerk-login')
   ```
   Do this for all `fetch` commands in [app.js](file:///c:/Users/aryan/OneDrive/Desktop/CODE/Java&Spring%20Boot/Banking-app/Banking-app/src/main/resources/static/app.js). Save and push to GitHub.
2. **Deploy on Vercel**:
   * Log in to [Vercel.com](https://vercel.com).
   * Click **Add New** -> **Project** -> Import your GitHub repository.
   * Under **Root Directory**, browse and select `src/main/resources/static`.
   * Under **Build and Development Settings**, leave the build command empty (Vercel will serve the HTML, CSS, and JS files as-is).
   * Click **Deploy**.
3. **Configure CORS**: Ensure your backend security configuration accepts requests from your new Vercel domain URL (in [SecurityConfig.java](file:///c:/Users/aryan/OneDrive/Desktop/CODE/Java&Spring%20Boot/Banking-app/Banking-app/src/main/java/net/javaguides/Banking_app/security/SecurityConfig.java#L41-L50), CORS is configured with `*` which accepts all domains, so it will work out of the box).

---

## 5. Core Spring Boot Concepts Explained

Let's review the main programming blocks that make your app run:

### Auto-Auditing Fields
Instead of manually typing `createdAt = LocalDateTime.now()` in every java method, we extend [BaseEntity.java](file:///c:/Users/aryan/OneDrive/Desktop/CODE/Java&Spring%20Boot/Banking-app/Banking-app/src/main/java/net/javaguides/Banking_app/entity/BaseEntity.java) on our database entities. The annotations `@CreatedDate` and `@LastModifiedDate` combined with `@EntityListeners(AuditingEntityListener.class)` automatically inject the timestamp when records are saved or updated.

### Database Updates
When you modify database columns (e.g. adding a field in `User.java`), Spring Boot reads `spring.jpa.hibernate.ddl-auto=update` and issues SQL ALTER commands behind the scenes. You never have to manually run SQL query command scripts to update database columns during development!

---

## 6. Project Architecture & Directory Structure

Here is a map of where classes reside:
* **config/**: Houses configurations.
* **controller/**: Routes HTTP endpoints.
* **dto/**: Holds REST contract request/response formats.
* **entity/**: Java class representation of MySQL database rows.
* **exception/**: Globally catches thrown errors to return standardized API responses.
* **mapper/**: Conversions between DTO containers and Entity database objects.
* **repository/**: DB interaction handles.
* **security/**: JWT authorization filters and public key parsers.
* **service/** & **service.impl/**: Interface declarations and logic implementations.

---

## 7. Transactional Business Logic (Deep Dive: Fund Transfers)

When a customer moves money, the process is wrapped inside `@Transactional(rollbackFor = Exception.class)`.

If Customer A has $100 and sends $50 to Customer B, the database subtracts $50 from Customer A and adds $50 to Customer B. If Customer B's account is blocked, an exception is thrown. Spring catches this exception, undoes the subtraction from Customer A, and preserves their balance. 

For audit trails, the failure logger class method uses `@Transactional(propagation = Propagation.REQUIRES_NEW)`. This executes in a separate transaction, ensuring that even if the primary balance transfer fails and rolls back, the database log recording the failure is committed.

---

## 8. Frontend SPA Architecture

The single page layout utilizes vanilla JavaScript router:
1. Navigation buttons call `navigateTo(pageId)` in [app.js](file:///c:/Users/aryan/OneDrive/Desktop/CODE/Java&Spring%20Boot/Banking-app/Banking-app/src/main/resources/static/app.js).
2. All page components are preloaded in [index.html](file:///c:/Users/aryan/OneDrive/Desktop/CODE/Java&Spring%20Boot/Banking-app/Banking-app/src/main/resources/static/index.html). The router sets `style.display = 'none'` for inactive elements and `style.display = 'block'` for active elements.
3. State is persisted inside the browser's `localStorage` variables.
