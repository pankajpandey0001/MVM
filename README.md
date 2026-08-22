# 🚀 Multi-Vendor Inventory Management System

A full-stack web application that allows a central Admin to manage a global product catalog, while allowing multiple independent Vendors to manage their own stock and pricing securely.

## 🛠️ Technology Stack & Why We Use It

**Backend (The Brain of the Application)**
*   **Java 21 & Spring Boot 3:** The core programming language and framework. We use this because it is highly secure, fast, and excellent for building large-scale enterprise applications.
*   **Spring Security:** Protects our application from unauthorized access.
*   **JWT (JSON Web Token):** A secure way to log users in. Instead of saving user sessions on the server, JWT gives the user a digital "ID card" (token) that they show every time they request data.
*   **MySQL:** A relational database used to store our permanent data like user accounts, products, and inventory history.
*   **Redis:** An in-memory database that is incredibly fast. We use it to store OTPs (One-Time Passwords) temporarily so we can verify users quickly before saving them to the main MySQL database.

**Frontend (What the User Sees)**
*   **React/JS:** A JavaScript library for building fast, interactive user interfaces.
*   **HTML & CSS:** The basic building blocks for structuring and styling web pages.

---

## 🛑 Core Business Rules

To keep the system secure and fair, the application enforces these strict rules:

1.  **Phone Number Validation:** Must be exactly 10-digit Indian numbers (starting with 6, 7, 8, or 9).
    *   *Why?* Ensures we only collect valid mobile numbers for the targeted region.
2.  **Strong Passwords:** Passwords must be 12 to 20 characters long and include an Uppercase letter, a Lowercase letter, a Number, and a Special Character.
    *   *Why?* Protects vendor and admin accounts from brute-force hacking attempts.
3.  **Registration Pipeline:** User details are temporarily held in Redis, and an OTP is sent. Only after the OTP is verified does the system write the data to the main MySQL database.
    *   *Why?* Prevents spam or fake accounts from filling up our main database.
4.  **Pricing Guardrails:** Vendor Price must be: `Admin Min Price <= Vendor Price <= Admin MRP`.
    *   *Why?* Prevents vendors from selling fake goods at suspiciously low prices or overcharging customers beyond the Maximum Retail Price (MRP).
5.  **Inventory Ledger:** Uses a double-entry system (tracking ADD, REMOVE, ADJUST actions).
    *   *Why?* If stock goes missing, we have a clear, unchangeable history of exactly when and how the stock numbers changed.
6.  **Security & Tracking:** We use "Soft Deletes" (marking a user as deleted without erasing their data) and keep a history of Ban/Unban actions.
    *   *Why?* If an admin bans a bad vendor, we keep their data for audit purposes instead of losing the evidence.

---

## 🏗️ Full-Stack Execution Matrix (Modules)

### 1. Core Authentication (Auth)
*   **Backend:** Handles Registration, encrypts passwords using **Bcrypt** (a mathematical algorithm that scrambles passwords so even database admins can't read them), manages JWT generation, and verifies Redis OTPs.
*   **Frontend:** Clean Login and Registration forms with an interactive OTP verification modal (a pop-up window).

### 2. Admin Hub
*   **Backend:** Controls **RBAC (Role-Based Access Control)**. This means the system checks if a user's "Role" is Admin before letting them approve vendors or ban users.
*   **Frontend:** Dashboard with easy-to-use tabs sorting vendors into "Pending", "Accepted", and "Banned" categories.

### 3. Catalog Management
*   **Backend:** Handles **CRUD (Create, Read, Update, Delete)** operations for the product hierarchy: `Category ➔ Sub-Category ➔ Product`. Admins also set the Minimum Price and MRP here.
*   **Frontend:** Displays a nested accordion tree (a menu that expands and collapses) so users can easily browse categories.

### 4. Vendor Inventory
*   **Backend:** Processes stock adjustments, enforces the pricing guardrails, and records every transaction in the ledger.
*   **Frontend:** A dedicated overview panel for vendors to quickly add stock and manage their specific inventory.

### 5. Audit & Deployment
*   **Backend:** Integrated audit logs to track *who* did *what* and *when*.
*   **Frontend:** Seamlessly switches between the Admin Portal and the Vendor Portal depending on who is logged in.

---

## 🚀 How to Run the Project Locally

Because this project relies on external databases, you must have them running on your local machine before starting the application.

1.  **Clone the repository:**
    `git clone https://github.com/pankajpandey0001/Multi-Vendor-Inventory-Management.git`
2.  **Start MySQL:** Ensure your local MySQL server is running. Create a database named `inventory_db` (or whatever name is in your application properties).
3.  **Start Redis:** Ensure your local Redis server is running on the default port (6379).
4.  **Configure Database Credentials:** Open `src/main/resources/application.properties` and update the MySQL username and password to match your local setup.
5.  **Run the Backend:** Start the Spring Boot application using your IDE (like IntelliJ/Eclipse) or via the terminal using Maven: `./mvnw spring-boot:run`
6.  **Test the APIs:** You can use a tool like Postman to test the backend endpoints directly on `http://localhost:8080`.