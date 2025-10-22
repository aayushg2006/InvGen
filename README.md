# Invoice Generator Web Application

A full-stack web application designed to simplify the process of creating, managing, and tracking professional invoices. Built with a modern tech stack, this application provides a secure, user-friendly interface for small business owners and freelancers to handle their invoicing needs efficiently.

## Features

- **Secure Authentication:** User registration and login system using JWT (JSON Web Tokens) for secure API access.
- **Dashboard Analytics:** An at-a-glance view of key business metrics including monthly revenue, total GST payable, and the number of paid vs. due invoices.
- **Product Management:** Easily add, view, and manage products and services, including their selling price and associated GST rates.
- **Customer Management:** Maintain a dynamic list of customers with their contact details. Customers can be added, edited, or deleted.
- **Dynamic Invoice Creation:**
    - Create professional invoices by selecting existing customers or adding new ones on the fly, including their contact details.
    - Add multiple line items from your product list, with automatic calculation of totals.
- **Instant PDF Generation & Download:** Upon creation, invoices are instantly generated as PDF files, complete with the shop logo, and downloaded directly to the user's device.
- **Comprehensive Invoice Management:**
    - View a searchable and sorted list of all created invoices, showing the most recent ones first.
    - Update the payment status of any invoice (e.g., from 'Pending' to 'Paid').
    - View a detailed, printable web version of any specific invoice.
- **Shop Customization:**
    - Update shop details like name, address, and GSTIN.
    - Upload a custom shop logo that appears on all generated PDF invoices and the invoice detail page.

## Tech Stack

### Backend
- **Java 21**
- **Spring Boot 3**
    - Spring Web
    - Spring Security (with JWT for token-based authentication)
    - Spring Data JPA (Hibernate)
- **MySQL Database**
- **iTextPDF:** For dynamic PDF generation.
- **Gradle:** For dependency management and building the project.

### Frontend
- **HTML5**
- **CSS3:** Modern design using Flexbox and Grid layouts.
- **JavaScript (ES6+):** Vanilla JS for all client-side logic, DOM manipulation, and API communication using the Fetch API.

## Prerequisites

Before you begin, ensure you have the following installed on your local machine:
- **Java JDK 21** or later.
- **MySQL Server:** Make sure your MySQL server is running.
- A **MySQL client** (like MySQL Workbench or DBeaver) to manage the database.
- An IDE like **VS Code** with the **Live Server** extension (recommended for running the frontend).

## Local Setup & Installation

### 1. Clone the Repository

```bash
git clone <your-repository-url>
cd invoice-generator
```

### 2. Backend Setup

1.  **Create the Database:**
    - Open your MySQL client and create a new database schema.
    ```sql
    CREATE DATABASE invoice;
    ```

2.  **Configure Database Connection:**
    - Navigate to `backend/generator/src/main/resources/`.
    - Open the `application.properties` file.
    - Update the `spring.datasource.username` and `spring.datasource.password` with your personal MySQL credentials.

    ```properties
    # MySQL Database Configuration
    spring.datasource.url=jdbc:mysql://localhost:3306/invoice
    spring.datasource.username=your_mysql_username
    spring.datasource.password=your_mysql_password
    
    # This setting is required for the PDF logo feature to work
    file.upload-dir=uploads/
    ```

3.  **Run the Backend Server:**
    - Open a terminal in the `backend/generator/` directory.
    - Run the application using the Gradle wrapper. The first run might take a minute to download dependencies.
    - On macOS/Linux:
      ```bash
      ./gradlew bootRun
      ```
    - On Windows:
      ```bash
      .\gradlew.bat bootRun
      ```
    - The backend API will start on `http://localhost:8080`. The application will also automatically create an `uploads/logos` directory in this folder if it doesn't exist.

### 3. Frontend Setup

1.  **Open with a Live Server:**
    - The easiest way to run the frontend is to use a live server extension in your code editor.
    - If you are using VS Code, install the "Live Server" extension from the marketplace.
    - Right-click on the `frontend/index.html` file and choose "Open with Live Server".
    - Your browser will open the application, typically at `http://127.0.0.1:5500`.

2.  **Verify API Connection:**
    - The frontend is configured in `frontend/js/api.js` to connect to `http://localhost:8080/api` by default. This should work seamlessly with the local backend setup.

You can now register a new user and start exploring all the features of the application!
