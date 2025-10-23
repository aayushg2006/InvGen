# InvGen - Full-Stack Invoice Generator & Business Management Suite

InvGen is a comprehensive, full-stack web application designed to simplify invoicing, expense tracking, and financial reporting for small businesses and freelancers. Built with a robust Java Spring Boot backend and a dynamic vanilla JavaScript frontend, it offers a complete solution for managing business finances.

The application features a secure user authentication system, full CRUD functionality for products, customers, and expenses, and seamless payment integration with Razorpay.

## Key Features

* **Dashboard Analytics**: At-a-glance view of key business metrics like revenue, GST payable, and invoice statuses with chart visualizations.
* **Invoice & Quote Management**: Create, view, and manage professional invoices and quotes. Convert quotes to invoices with a single click.
* **Automated PDF Generation**: Automatically generate and attach PDF documents for invoices, quotes, and payment receipts.
* **Recurring Invoices**: Set up automated daily, weekly, monthly, or yearly invoices for repeat customers.
* **Payment Integration**: Securely process payments via **Razorpay**, with support for real-time QR code generation and automated payment confirmation via webhooks.
* **Email Automation**:
    * Automatically email quotes and invoices to clients upon creation.
    * Send payment confirmation emails with attached receipts and paid invoices.
    * Automatically send weekly payment reminders for overdue invoices.
* **Expense Tracking**: Log, categorize, and manage business expenses to maintain a clear financial record.
* **Advanced Reporting**:
    * **Profit & Loss Statements**: Generate P&L reports for any date range.
    * **Sales Analysis**: Visualize sales by product and revenue by customer.
    * **Payment summaries**: Track revenue by payment method.

## Tech Stack

* **Backend**:
    * Java 21
    * Spring Boot 3
    * Spring Security (with JWT for authentication)
    * Spring Data JPA (with Hibernate)
    * MySQL Database
    * **Libraries**: iTextPDF, SendGrid, Razorpay Java SDK

* **Frontend**:
    * HTML5
    * CSS3
    * Vanilla JavaScript (ES6+)
    * **Libraries**: Chart.js, QRCode.js

## Setup and Installation

### 1. Backend (Spring Boot)

* **Prerequisites**:
    * JDK 21 or later
    * Maven or Gradle
    * MySQL Server
* **Database Setup**:
    1.  Create a new MySQL database named `invoice`.
* **Configuration**:
    1.  Navigate to `backend/generator/src/main/resources/application.properties`.
    2.  Update the `spring.datasource.username` and `spring.datasource.password` with your MySQL credentials.
    3.  Add your API keys for SendGrid and Razorpay.
* **Run the application**:
    * Open a terminal in the `backend/generator/` directory and run:
        ```bash
        ./gradlew bootRun
        ```
    * The server will start on `http://localhost:8080`.

### 2. Frontend (Vanilla JS)

* **Prerequisites**:
    * A modern web browser.
    * A live-server extension for your code editor (like VS Code's "Live Server") is recommended for development.
* **Running the application**:
    1.  Open the `frontend` directory in your code editor.
    2.  Right-click on `index.html` and open it with Live Server.
    3.  The application will be accessible at `http://127.0.0.1:5500` (or a similar port).

## Environment Configuration

```properties
# MySQL Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3006/invoice
spring.datasource.username=your_mysql_username
spring.datasource.password=your_mysql_password

# JPA/Hibernate Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# File Upload Directory
file.upload-dir=uploads/

# SendGrid Configuration
sendgrid.api.key=YOUR_SENDGRID_API_KEY_HERE

# Razorpay Configuration
razorpay.key.id=YOUR_RAZORPAY_KEY_ID_HERE
razorpay.key.secret=YOUR_RAZORPAY_SECRET_HERE
razorpay.webhook.secret=YOUR_WEBHOOK_SECRET_HERE
