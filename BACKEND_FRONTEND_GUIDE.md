# 💻 AttenTrack: Backend & Frontend Technical Guide

This document provides a deep dive into the internal architecture, logic, and user interface of the AttenTrack system.

---

## 🏗️ 1. Backend Architecture (Spring Boot)

The backend is built using **Spring Boot 4.0.2** (Java 21) following a standard layered architecture:
- **Controller Layer**: Handles HTTP requests (`REST/Thymeleaf Rendering`).
- **Service Layer**: Contains core business logic (Attendance calculations, Eligibility checks).
- **Repository Layer**: Interfaces with PostgreSQL via **Spring Data JPA**.
- **Entity Layer**: Defines the database schema and ORM mappings.

### **Core Data Model**
*   **User/Student**: Handles authentication and profile information.
*   **Course**: Tracks course-specific data (Code, Name, Credits).
*   **Attendance**: Stores records mapped by date or count.
*   **AcademicCalendar/Holiday**: System-wide configuration that affects all student calculations.

### **Key Business Logic: Attendance Eligibility**
The system calculates real-time eligibility using the following formula:
$$ \text{Percentage} = \frac{\text{Classes Attended}}{\text{Classes Conducted}} \times 100 $$

*   **Logic for "Classes you can skip"**:
    The system solves for $x$ in:
    $$ \frac{\text{Attended}}{\text{Conducted} + x} \geq 0.75 $$
*   **Medical Relaxation**: Automates logic to check if a student is above **65%** for medical consideration.

### **Security Implementation**
*   **Spring Security**: Implements custom `UserDetails` and `JWT Token` generation.
*   **Role-Based Access Control (RBAC)**:
    - `ROLE_STUDENT`: Access to personal attendance, course management, and reports.
    - `ROLE_ADMIN`: Access to global academic calendar and holiday management.

---

## 🎨 2. Frontend Architecture (Thymeleaf & JS)

The frontend is a server-side rendered application using **Thymeleaf**, enhanced with modern UI frameworks.

### **Templating & UI**
*   **Thymeleaf**: Dynamically injects data into HTML templates before serving them to the client.
*   **Bootstrap 5**: Provides a responsive, mobile-first CSS grid and UI components.
*   **FontAwesome**: Used for icons in the dashboard and navigation.

### **Interactive Elements (JavaScript)**
*   **Attendance Calendar**: A custom JavaScript-driven calendar that allows students to click and toggle attendance status for specific dates.
*   **Dynamic Reports**: Charts and progress bars (using simple CSS and JS) that reflect current attendance percentages and exam readiness.
*   **AJAX (Optional/Hybrid)**: Used for rapid updates without full page reloads in administrative workflows.

### **User Workflows**
1.  **Dashboard**: Provides a bird's-eye view of all courses and their current statuses.
2.  **Course Management**: Multi-step forms for adding course details and weekly schedules.
3.  **Report View**: High-intensity data view showing "Next classes to attend" and "Can you skip?" advice.

---

## 🛠️ 3. Development Setup
To run the backend locally:
1.  Ensure **Java 21** and **Maven** are installed.
2.  Set up a **PostgreSQL 16+** database.
3.  Configure `application.properties` with your database credentials.
4.  Run `./mvnw spring-boot:run`.
