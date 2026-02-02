# 📚 Attendance Management System - Complete Feature & Architecture Guide

## Table of Contents
1. [System Overview](#system-overview)
2. [Core Features](#core-features)
3. [Technology Stack](#technology-stack)
4. [Database Architecture](#database-architecture)
5. [System Architecture](#system-architecture)
6. [Key Business Logic](#key-business-logic)
7. [User Roles & Permissions](#user-roles--permissions)
8. [API Endpoints](#api-endpoints)
9. [Development Guide](#development-guide)
10. [Deployment](#deployment)

---

## System Overview

The **Attendance Management System** is a comprehensive Spring Boot web application designed for educational institutions to:
- Track student attendance across multiple courses
- Manage academic calendars, holidays, and exam dates
- Calculate exam eligibility based on attendance thresholds
- Provide real-time attendance insights and reports

**Target Users:**
- **Students**: Track their attendance, manage courses, view eligibility status
- **Administrators**: Configure academic calendars, manage courses and holidays, view system statistics

**Key Metrics:**
- Default attendance threshold: **75%** (standard)
- Medical/relaxation threshold: **65%**
- Academic periods tracked: Multiple semesters with exam dates (CAT-1, CAT-2, FAT)

---

## Core Features

### 🎓 Student Features

#### 1. **Authentication & Authorization**
- User registration with username, email, and password
- Secure login with JWT token-based authentication
- Session management and token expiration (24 hours)
- Role-based access control (STUDENT role)

#### 2. **Course Management**
- **Add New Courses**: Students can add any new course to their profile
- **Course Details Tracked:**
  - Course Code (unique identifier)
  - Course Name (human-readable name)
  - Course Start Date (for date-based attendance)
  - Weekly class schedule (which days have classes and how many)
  
- **Feature Flow:**
  - Student selects days (Tuesday-Saturday) when course has classes
  - Student specifies number of classes per day
  - System automatically generates expected class dates

#### 3. **Attendance Marking - Dual Approach**

##### A. **Aggregate Attendance Method**
- Input total classes conducted and classes attended
- Formula: `Percentage = (Classes Attended / Total Classes) × 100`
- Useful for quick entry of overall course attendance
- Stored in `AttendanceInput` entity
- Example: "Out of 30 classes, I attended 25" → 83.33%

##### B. **Date-Based Attendance Method**
- Mark attendance for specific class dates on a calendar interface
- System auto-generates expected class dates based on:
  - Course start date
  - Weekly timetable (days when course has classes)
  - Number of classes per day
  - Institutional holidays (excluded from count)
  
- **Calendar Features:**
  - Visual calendar interface for marking attendance
  - Toggle between attended/absent for each date
  - Real-time attendance percentage calculation
  - Shows holidays and non-class days in different colors
  
- **Stored in:** `DateBasedAttendance` entity (one record per class date)

#### 4. **Attendance Report & Exam Eligibility**
The system provides comprehensive attendance analytics:

**Current Status:**
- Current attendance percentage
- Total classes conducted vs attended
- Eligible for 75% threshold (standard exam)
- Eligible for 65% threshold (medical/relaxation)

**Future Projections:**
- Number of classes that can be skipped while maintaining 75%
- Number of classes that can be skipped while maintaining 65%
- Minimum classes required to attend remaining classes
- Projected attendance if all remaining classes are attended

**Smart Calculation Features:**
- Only counts working days (Tuesday-Saturday)
- Excludes institutional holidays
- Considers current date and exam dates from academic calendar
- Last working day before exam is excluded from future calculations
- Alerts when eligibility is at risk or impossible

**Report Fields (in `AttendanceResult` entity):**
- `currentPercentage`: Current attendance %
- `futureClassesAvailable`: Upcoming classes before exam
- `minClassesRequired`: Minimum classes to maintain threshold
- `eligibilityStatus`: SAFE, AT_RISK, or IMPOSSIBLE

#### 5. **Holiday & Academic Calendar View**
- View institutional holidays for current semester
- See exam date ranges (CAT-1, CAT-2, FAT)
- Understand semester start and end dates
- Plan attendance accordingly

#### 6. **Timetable Management**
- Set weekly class schedule (days and number of classes per day)
- Used for both attendance tracking and calendar generation
- Can be updated when course schedule changes
- Applied to all future attendance calculations

---

### 👨‍💼 Admin Features

#### 1. **Dashboard Analytics**
- **System Statistics:**
  - Total number of registered students
  - Total number of courses in system
  - Current academic year and semester info
  - Quick overview of system health

#### 2. **Academic Calendar Management**
Create and manage academic calendars with:

- **Semester Configuration:**
  - Academic year (e.g., "2023-24")
  - Semester start date
  - Exam start date (general)

- **Exam Schedule Definition:**
  - CAT-1 (Continuous Assessment Test 1): Start and end dates
  - CAT-2 (Continuous Assessment Test 2): Start and end dates
  - FAT (Final Assessment Test): Start and end dates

- **Features:**
  - CRUD operations (Create, Read, Update, Delete)
  - Multiple calendars support (for different semesters)
  - Validation to ensure valid date ranges
  - Auto-timestamp on creation and updates

#### 3. **Holiday Management**
- Add institutional holidays for specific academic calendar
- Holiday types implicitly supported (can be extended)
- Holidays automatically excluded from attendance calculations
- Can be linked to exam dates or semester breaks

#### 4. **Course Administration**
- View all courses created by students
- Manage course catalog
- Monitor course enrollment
- (Can be extended for course approval, editing, etc.)

---

## Technology Stack

### Backend
| Component | Technology | Version |
|-----------|-----------|---------|
| Framework | Spring Boot | 4.0.2 |
| Java Version | JDK | 21 |
| Build Tool | Maven | 3.8+ |
| Data Access | Spring Data JPA | Latest |
| Security | Spring Security | Latest |
| JWT Library | jjwt | 0.12.3 |
| Database Driver | PostgreSQL | Latest |
| Logging | SLF4J/Logback | Latest |
| Utilities | Lombok | Latest |

### Frontend
| Component | Technology |
|-----------|-----------|
| Templates | Thymeleaf |
| Markup | HTML5 |
| Styling | CSS3 with Gradients |
| Interactivity | JavaScript (Vanilla) |
| Calendar UI | Custom JavaScript Calendar |
| CAPTCHA | Custom CAPTCHA Image Generation |

### Database
| Component | Technology |
|-----------|-----------|
| RDBMS | PostgreSQL 16.9+ |
| Connection Pooling | HikariCP |
| ORM | Hibernate |

### Deployment
| Component | Technology |
|-----------|-----------|
| Containerization | Docker |
| Orchestration | Docker Compose |
| Cloud Hosting | Render.com |
| CI/CD | Render Deployment |

---

## Database Architecture

### Database Schema

```
TABLE: users
├── id (PK)
├── username (UNIQUE, NOT NULL)
├── password (NOT NULL, BCrypt encrypted)
├── email (NOT NULL)
├── enabled (DEFAULT true)
└── roles (M-to-M via user_roles)

TABLE: roles
├── id (PK)
├── name (UNIQUE) [ADMIN, STUDENT]

TABLE: user_roles (Junction Table)
├── user_id (FK → users)
└── role_id (FK → roles)

TABLE: courses
├── id (PK)
├── userId (FK → users.id) - course owner (student)
├── courseCode (NOT NULL, unique per user)
├── courseName
├── courseStartDate (for date-based attendance)
└── timetableEntries (O-to-M)

TABLE: timetable_entries
├── id (PK)
├── course_id (FK → courses.id)
├── dayOfWeek (TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY)
└── classesCount (number of classes on this day)

TABLE: attendance_input (Aggregate Method)
├── id (PK)
├── course_id (FK → courses.id)
├── totalClassesConducted (T)
├── classesAttended (A)
└── lastUpdated (TIMESTAMP)

TABLE: date_based_attendance (Date-Based Method)
├── id (PK)
├── course_id (FK → courses.id)
├── attendanceDate (NOT NULL, UC with course_id)
├── attended (BOOLEAN)
└── recordedAt (TIMESTAMP)

TABLE: attendance_results (Calculated Results)
├── id (PK)
├── course_id (FK → courses.id)
├── currentPercentage (DOUBLE)
├── futureClassesAvailable (INT)
├── minClassesRequired (INT)
├── eligibilityStatus (SAFE, AT_RISK, IMPOSSIBLE)
└── calculatedAt (TIMESTAMP)

TABLE: academic_calendar
├── id (PK)
├── academicYear (e.g., "2023-24")
├── semesterStartDate (NOT NULL)
├── examStartDate (NOT NULL)
├── cat1StartDate, cat1EndDate
├── cat2StartDate, cat2EndDate
├── fatStartDate, fatEndDate
├── createdAt (NOT NULL)
└── updatedAt (NOT NULL)

TABLE: holidays
├── id (PK)
├── academicCalendarId (FK → academic_calendar.id)
├── date (NOT NULL)
├── description
├── createdAt
└── updatedAt
```

### Key Database Relationships

1. **User → Roles** (Many-to-Many)
   - Each user can have multiple roles
   - Each role can be assigned to multiple users
   - Supports role-based authorization

2. **User → Courses** (One-to-Many)
   - Each student owns multiple courses
   - Each course belongs to one student
   - Courses isolated by user for data privacy

3. **Course → Attendance Data** (One-to-Many)
   - Each course has multiple attendance records
   - Supports both aggregate and date-based methods
   - Results calculated and cached in `attendance_results`

4. **Course → Timetable** (One-to-Many)
   - Defines class schedule for each course
   - Used for auto-generating expected class dates

5. **Academic Calendar → Holidays** (One-to-Many)
   - Each calendar can have multiple holidays
   - Holidays linked to specific academic period

---

## System Architecture

### High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        PRESENTATION LAYER                       │
│  (HTML Templates with Thymeleaf + JavaScript + CSS)             │
│                                                                 │
│  ├─ login.html          (Authentication)                       │
│  ├─ register.html       (User Registration)                    │
│  ├─ dashboard.html      (Home Page)                            │
│  ├─ student-*.html      (Student Pages)                        │
│  └─ admin-*.html        (Admin Pages)                          │
└────────────────────────────┬────────────────────────────────────┘
                             │
                    HTTP/HTTPS Requests
                             │
┌────────────────────────────▼────────────────────────────────────┐
│                        SECURITY LAYER                           │
│                 (Spring Security + JWT Filter)                  │
│                                                                 │
│  ├─ JwtAuthenticationFilter    (Extract & validate JWT)        │
│  ├─ SecurityConfig             (Configure access rules)        │
│  ├─ JwtTokenProvider           (Generate & parse JWT)          │
│  └─ CustomUserDetailsService   (Load user authorities)         │
└────────────────────────────┬────────────────────────────────────┘
                             │
                    Authenticated Requests
                             │
┌────────────────────────────▼────────────────────────────────────┐
│                     CONTROLLER LAYER                            │
│              (Spring MVC & REST Controllers)                    │
│                                                                 │
│  ├─ AuthController                  (Login/Register)           │
│  ├─ StudentViewController            (View Routing)            │
│  ├─ HomeController                   (Dashboard & Pages)       │
│  ├─ StudentController                (Course & Attendance)     │
│  ├─ DateBasedAttendanceController    (Calendar Entry)          │
│  ├─ AcademicCalendarController       (Admin Calendar Mgmt)     │
│  ├─ HolidayController                (Admin Holiday Mgmt)      │
│  └─ AdminPageController              (Admin Views)             │
└────────────────────────────┬────────────────────────────────────┘
                             │
             Business Logic Processing
                             │
┌────────────────────────────▼────────────────────────────────────┐
│                      SERVICE LAYER                              │
│         (Business Logic & Data Transformation)                  │
│                                                                 │
│  ├─ StudentService                 (Course & Timetable Mgmt)   │
│  ├─ AttendanceCalculationService   (Smart Attendance Calc)     │
│  ├─ DateBasedAttendanceService     (Calendar Attendance)       │
│  ├─ AcademicCalendarService        (Calendar Operations)       │
│  ├─ HolidayService                 (Holiday Management)        │
│  ├─ WorkingDayService              (Date Utilities)            │
│  └─ CustomUserDetailsService       (User Authentication)       │
└────────────────────────────┬────────────────────────────────────┘
                             │
                    Data Access Operations
                             │
┌────────────────────────────▼────────────────────────────────────┐
│                   REPOSITORY LAYER                              │
│         (Spring Data JPA - Data Access Objects)                 │
│                                                                 │
│  ├─ UserRepository                                             │
│  ├─ RoleRepository                                             │
│  ├─ CourseRepository                                           │
│  ├─ TimetableEntryRepository                                   │
│  ├─ AttendanceInputRepository                                  │
│  ├─ DateBasedAttendanceRepository                              │
│  ├─ AttendanceResultRepository                                 │
│  ├─ AcademicCalendarRepository                                 │
│  └─ HolidayRepository                                          │
└────────────────────────────┬────────────────────────────────────┘
                             │
                  SQL Query Execution
                             │
┌────────────────────────────▼────────────────────────────────────┐
│                   PERSISTENCE LAYER                             │
│              (Hibernate ORM + HikariCP Pool)                    │
└────────────────────────────┬────────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────────┐
│                      DATABASE LAYER                             │
│                  (PostgreSQL 16.9+)                             │
│          (Transactional ACID-compliant RDBMS)                   │
└─────────────────────────────────────────────────────────────────┘
```

### Request Flow Example: Student Marking Attendance

```
1. Student views calendar page
   ↓
2. Browser loads /student-attendance page
   ↓
3. HTML template requests course dates via JavaScript
   ↓
4. Frontend calls API: GET /api/student/courses/{courseId}/calendar
   ↓
5. Request reaches JwtAuthenticationFilter
   ↓
6. JWT token validated and user authenticated
   ↓
7. Request routed to StudentController
   ↓
8. StudentService retrieves:
   - Course details
   - Timetable entries
   - Academic calendar
   - Holidays
   - Existing attendance records
   ↓
9. Service generates calendar with all expected class dates
   ↓
10. Filters out:
    - Non-class days (Sun, Mon)
    - Holidays
    - Past dates
   ↓
11. Returns JSON calendar data to frontend
   ↓
12. JavaScript renders interactive calendar
   ↓
13. Student clicks dates to mark attended/absent
   ↓
14. Frontend POST request: /api/student/courses/{courseId}/attendance
   ↓
15. DateBasedAttendanceController receives attendance updates
   ↓
16. DateBasedAttendanceService saves/updates records
   ↓
17. AttendanceCalculationService recalculates eligibility
   ↓
18. Results stored in attendance_results table
   ↓
19. Frontend updates UI with new attendance percentage
```

---

## Key Business Logic

### 1. **Attendance Calculation Algorithm**

#### Aggregate Method (Simple)
```
Percentage = (Classes Attended / Total Classes Conducted) × 100
```

#### Date-Based Method (Smart)
```
1. Retrieve course start date and timetable
2. Generate expected class dates:
   - Start from courseStartDate
   - Add classes for each day in timetable
   - Multiply by number of classes per day
   - Exclude Sundays and Mondays
   - Exclude holidays from academic calendar
3. Count attended dates (attended = true)
4. Calculate percentage:
   Percentage = (Attended Dates / Total Expected Dates) × 100
```

**Example Calculation:**
- Course starts: 2024-01-15 (Monday)
- Timetable: Tuesday (2 classes), Wednesday (1 class), Thursday (2 classes)
- Expected classes in January:
  - Week 1: 2 (Tue) + 1 (Wed) + 2 (Thu) = 5 classes
  - Week 2: 2 + 1 + 2 = 5 classes
  - Week 3: 2 + 1 + 2 = 5 classes
  - Week 4: 2 + 1 + 2 = 5 classes
  - Total January: 20 classes
- If attended 17: 17/20 = 85%

### 2. **Future Eligibility Calculation**

The system calculates several metrics for exam eligibility:

#### Key Algorithm: `getValidWorkingDays()`

```
Input: None (uses current date and academic calendar)
Output: List of future valid class dates

Steps:
1. Get current date (today)
2. Retrieve active academic calendar:
   - Where semesterStartDate ≤ today
   - AND examStartDate ≥ today
3. If no active calendar: return empty list
4. Get exam start date from calendar
5. Get all holidays for this calendar
6. Get last working day before exam:
   - Start from (examStartDate - 1 day)
   - Move backwards while date is after today
   - Only consider Tuesday-Saturday
   - Skip holidays
7. Loop from today to exam start:
   - Check if day is Tuesday-Saturday
   - Check if not holiday
   - Check if not last working day before exam
   - Add to valid days list
8. Return valid days list
```

#### Example Scenario
- Current date: 2024-02-15 (Thursday)
- Exam starts: 2024-04-01
- Holidays: 2024-02-26 (Republic Day), 2024-03-15 (Holi)
- Last working day before exam: 2024-03-29 (calculated)

Valid working days between now and exam:
- All Tue, Wed, Thu, Fri, Sat between 2024-02-15 and 2024-03-29
- Except 2024-02-26 and 2024-03-15
- Except 2024-03-29 (last working day)

#### Eligibility Status Determination

```
IF current_percentage ≥ 75%:
   status = "SAFE"
ELSE IF current_percentage ≥ 65%:
   Calculate: classes_can_skip = future_classes - (need_to_pass)
   IF classes_can_skip > 0:
      status = "AT_RISK" (but still possible)
   ELSE:
      status = "IMPOSSIBLE"
ELSE:
   status = "AT_RISK"
```

### 3. **Timetable Processing**

#### Auto-Generated Calendar
```
Given:
- Course start date: 2024-01-15
- Timetable: [Tuesday: 2 classes, Thursday: 2 classes, Friday: 1 class]
- Academic calendar with holidays

Generate:
1. Start from courseStartDate
2. For each week until exam date:
   - If Tuesday and has classes: add 2 date entries
   - If Thursday and has classes: add 2 date entries
   - If Friday and has classes: add 1 date entry
   - Skip if date is a holiday
3. Create DateBasedAttendance records (default: not attended)
4. Return calendar for UI rendering
```

---

## User Roles & Permissions

### Role Hierarchy

```
┌──────────────────────┐
│        ADMIN         │
├──────────────────────┤
│ Can access:          │
│ • /admin/*           │
│ • /api/admin/*       │
│ • Dashboard stats    │
│ • Calendar mgmt      │
│ • Holiday mgmt       │
│ • Course listing     │
└──────────────────────┘

┌──────────────────────┐
│       STUDENT        │
├──────────────────────┤
│ Can access:          │
│ • /student-*         │
│ • /api/student/*     │
│ • Dashboard (own)    │
│ • Own courses        │
│ • Own attendance     │
│ • Own reports        │
└──────────────────────┘

┌──────────────────────┐
│      ANONYMOUS       │
├──────────────────────┤
│ Can access:          │
│ • /login             │
│ • /register          │
│ • /api/auth/*        │
│ • /captcha-images/*  │
└──────────────────────┘
```

### Security Configuration Details

**From `SecurityConfig.java`:**

```java
// Public endpoints (no authentication required)
- "/" (root)
- "/index.html"
- "/login"
- "/register"
- "/api/auth/**" (all auth endpoints)
- "/captcha-images/**" (CAPTCHA images)
- "/api/student/portal/captcha-image/**" (CAPTCHA endpoint)

// Student-only endpoints
- "/student-dashboard"
- "/student-attendance"
- "/student-attendance-report"
- "/api/student/**"

// Admin-only endpoints
- "/admin-dashboard"
- "/admin/**"
- "/api/admin/**"

// Authentication
- JWT token-based (stateless sessions)
- Token expiration: 24 hours (86400000 ms)
- CORS enabled for localhost:3000, localhost:8080
```

---

## API Endpoints

### Authentication APIs (`/api/auth/`)

```
POST /api/auth/login
├─ Request: { "username": "string", "password": "string" }
├─ Response: { "token": "JWT token", "username": "string", "message": "string" }
└─ Status: 200 OK or 400 Bad Request

POST /api/auth/register
├─ Request: { "username": "string", "password": "string", "email": "string" }
├─ Response: { "message": "Registration successful" }
└─ Status: 201 Created or 400 Bad Request
```

### Student APIs (`/api/student/`)

#### Course Management
```
GET /api/student/courses
├─ Description: Get all courses for logged-in student
├─ Response: List of Course objects
└─ Status: 200 OK

POST /api/student/courses
├─ Description: Add/update courses and timetable
├─ Request: List of { courseCode, courseName, weeklySchedule, courseStartDate }
└─ Response: Success message

GET /api/student/courses/{courseId}/calendar
├─ Description: Get expected class dates for calendar view
├─ Response: List of dates with attendance status
└─ Status: 200 OK
```

#### Attendance (Aggregate Method)
```
POST /api/student/attendance
├─ Description: Save/update attendance (total classes & attended)
├─ Request: { courseId, totalClassesConducted, classesAttended }
├─ Response: Saved AttendanceInput object
└─ Status: 201 Created or 200 OK

GET /api/student/attendance/{courseId}
├─ Description: Get current attendance for a course
├─ Response: AttendanceInput object
└─ Status: 200 OK
```

#### Attendance (Date-Based Method)
```
POST /api/student/courses/{courseId}/attendance
├─ Description: Mark attendance for specific dates
├─ Request: { "attendances": [{ "date": "2024-01-15", "attended": true }] }
├─ Response: Success message
└─ Status: 201 Created

GET /api/student/courses/{courseId}/attendance
├─ Description: Get all date-based attendance records
├─ Response: List of DateBasedAttendance objects
└─ Status: 200 OK
```

#### Attendance Report & Eligibility
```
GET /api/student/attendance-report/{courseId}
├─ Description: Get detailed attendance report and eligibility
├─ Response: {
│   "currentPercentage": 85.5,
│   "threshold75": true,
│   "threshold65": true,
│   "classesCanSkip75": 2,
│   "classesCanSkip65": 5,
│   "futureClasses": 15,
│   "minClassesRequired": 12,
│   "projectedAttendance": 88.2,
│   "examEligible": true
│ }
└─ Status: 200 OK
```

#### Holiday & Calendar Info
```
GET /api/student/holidays
├─ Description: Get holidays for current academic calendar
├─ Response: List of Holiday objects
└─ Status: 200 OK

GET /api/student/academic-calendar
├─ Description: Get current academic calendar details
├─ Response: AcademicCalendar object
└─ Status: 200 OK
```

### Admin APIs (`/api/admin/`)

#### Academic Calendar Management
```
GET /api/admin/academic-calendars
├─ Description: Get all academic calendars
├─ Response: List of AcademicCalendar objects
└─ Status: 200 OK

POST /api/admin/academic-calendars
├─ Description: Create new academic calendar
├─ Request: AcademicCalendar object
└─ Status: 201 Created

PUT /api/admin/academic-calendars/{id}
├─ Description: Update academic calendar
├─ Request: Updated AcademicCalendar object
└─ Status: 200 OK

DELETE /api/admin/academic-calendars/{id}
├─ Description: Delete academic calendar
└─ Status: 204 No Content
```

#### Holiday Management
```
GET /api/admin/holidays
├─ Description: Get all holidays
├─ Response: List of Holiday objects
└─ Status: 200 OK

POST /api/admin/holidays
├─ Description: Create new holiday
├─ Request: { "academicCalendarId": 1, "date": "2024-03-15", "description": "Holi" }
└─ Status: 201 Created

DELETE /api/admin/holidays/{id}
├─ Description: Delete holiday
└─ Status: 204 No Content
```

#### Course Management
```
GET /api/admin/courses
├─ Description: Get all courses in system
├─ Response: List of Course objects
└─ Status: 200 OK
```

#### Dashboard Statistics
```
GET /api/admin/dashboard-stats
├─ Description: Get system statistics
├─ Response: {
│   "totalStudents": 150,
│   "totalCourses": 450,
│   "currentAcademicYear": "2024-25",
│   "currentSemester": "Spring 2024"
│ }
└─ Status: 200 OK
```

---

## Development Guide

### Project Structure

```
Attendance/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/deepak/Attendance/
│   │   │       ├── AttendanceApplication.java (Main entry point)
│   │   │       ├── config/
│   │   │       │   ├── SecurityConfig.java (Spring Security configuration)
│   │   │       │   ├── DataInitializer.java (Default data on startup)
│   │   │       │   └── WebMvcConfig.java (MVC configuration)
│   │   │       ├── controller/
│   │   │       │   ├── AuthController.java (Login/Register)
│   │   │       │   ├── StudentController.java (REST APIs for students)
│   │   │       │   ├── DateBasedAttendanceController.java (Calendar attendance)
│   │   │       │   ├── AcademicCalendarController.java (Admin calendar)
│   │   │       │   ├── HolidayController.java (Admin holidays)
│   │   │       │   ├── HomeController.java (Page routing)
│   │   │       │   ├── AdminPageController.java (Admin page views)
│   │   │       │   └── StudentViewController.java (Student page views)
│   │   │       ├── entity/
│   │   │       │   ├── User.java (User account)
│   │   │       │   ├── Role.java (User roles)
│   │   │       │   ├── Course.java (Student course)
│   │   │       │   ├── TimetableEntry.java (Course schedule)
│   │   │       │   ├── AttendanceInput.java (Aggregate attendance)
│   │   │       │   ├── DateBasedAttendance.java (Calendar attendance)
│   │   │       │   ├── AttendanceResult.java (Calculated results)
│   │   │       │   ├── AcademicCalendar.java (Academic period)
│   │   │       │   └── Holiday.java (Institutional holidays)
│   │   │       ├── repository/ (Spring Data JPA interfaces)
│   │   │       │   ├── UserRepository.java
│   │   │       │   ├── RoleRepository.java
│   │   │       │   ├── CourseRepository.java
│   │   │       │   ├── TimetableEntryRepository.java
│   │   │       │   ├── AttendanceInputRepository.java
│   │   │       │   ├── DateBasedAttendanceRepository.java
│   │   │       │   ├── AttendanceResultRepository.java
│   │   │       │   ├── AcademicCalendarRepository.java
│   │   │       │   └── HolidayRepository.java
│   │   │       ├── service/ (Business logic)
│   │   │       │   ├── StudentService.java
│   │   │       │   ├── AttendanceCalculationService.java
│   │   │       │   ├── DateBasedAttendanceService.java
│   │   │       │   ├── AcademicCalendarService.java
│   │   │       │   ├── HolidayService.java
│   │   │       │   ├── WorkingDayService.java
│   │   │       │   └── CustomUserDetailsService.java
│   │   │       ├── security/ (JWT & Authentication)
│   │   │       │   ├── JwtTokenProvider.java
│   │   │       │   └── JwtAuthenticationFilter.java
│   │   │       └── dto/ (Data Transfer Objects)
│   │   │           ├── LoginRequest.java
│   │   │           ├── LoginResponse.java
│   │   │           ├── AttendanceReportDTO.java
│   │   │           ├── TimetableEntryDTO.java
│   │   │           └── AcademicCalendarDTO.java
│   │   └── resources/
│   │       ├── application.properties (Configuration)
│   │       ├── application.properties.example (Template)
│   │       ├── templates/ (Thymeleaf HTML templates)
│   │       │   ├── login.html
│   │       │   ├── register.html
│   │       │   ├── dashboard.html
│   │       │   ├── student-dashboard.html
│   │       │   ├── student-attendance.html
│   │       │   ├── student-attendance-report.html
│   │       │   ├── student-attendance-calendar.html
│   │       │   ├── student-manage-courses.html
│   │       │   ├── admin-dashboard.html
│   │       │   ├── admin-academic-calendar.html
│   │       │   ├── admin-holidays.html
│   │       │   └── ...
│   │       └── static/ (CSS, JS, Images)
│   └── test/
│       └── java/ (Unit tests)
├── pom.xml (Maven configuration)
├── Dockerfile (Container configuration)
├── docker-compose.yml (Multi-container setup)
├── render.yaml (Render.com deployment config)
└── README.md
```

### Key Dependencies & Versions

```xml
<!-- Spring Boot Framework -->
<spring-boot-starter-parent>4.0.2</spring-boot-starter-parent>
<spring-boot-starter-web> <!-- REST APIs -->
<spring-boot-starter-security> <!-- Authentication -->
<spring-boot-starter-data-jpa> <!-- Database access -->
<spring-boot-starter-thymeleaf> <!-- Template engine -->

<!-- Database -->
<postgresql>16.9+ <!-- PostgreSQL driver -->

<!-- JWT Authentication -->
<jjwt-api>0.12.3
<jjwt-impl>0.12.3
<jjwt-jackson>0.12.3

<!-- Utilities -->
<lombok> <!-- Annotation processor -->

<!-- Java Version -->
<java.version>21

<!-- Testing -->
<spring-boot-starter-test>
<spring-security-test>
```

### Important Configuration Files

#### `application.properties`

```properties
# App Name
spring.application.name=Attendance

# Database (PostgreSQL)
spring.datasource.url=${SPRING_DATASOURCE_URL}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update          # Auto-create/update schema
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.show-sql=false                     # Don't log SQL

# Connection Pool
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.minimum-idle=2

# JWT
jwt.secret=${JWT_SECRET:defaultSecretKey}
jwt.expiration=86400000                       # 24 hours

# Server
server.port=${PORT:8081}

# Logging
logging.level.root=INFO
logging.level.com.deepak.Attendance=DEBUG
```

### Default Test User

**Created on application startup by `DataInitializer.java`:**

| Field | Value |
|-------|-------|
| Username | `admin` |
| Password | `admin123` |
| Email | `admin@example.com` |
| Role | `ADMIN` |

### Building from Source

```bash
# 1. Clone repository
git clone <repository-url>
cd Attendance

# 2. Create database
createdb attendance_db

# 3. Configure environment
# Option A: Create .env file
echo "SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/attendance_db" > .env
echo "SPRING_DATASOURCE_USERNAME=postgres" >> .env
echo "SPRING_DATASOURCE_PASSWORD=your_password" >> .env
echo "JWT_SECRET=your_jwt_secret_key" >> .env

# Option B: Modify application.properties
# Edit src/main/resources/application.properties

# 4. Build project
./mvnw clean install -DskipTests

# 5. Run application
./mvnw spring-boot:run
# OR
java -jar target/Attendance-0.0.1-SNAPSHOT.jar

# 6. Access application
# Open browser: http://localhost:8081
# Login with admin/admin123
```

### Code Organization Best Practices

1. **Entity Layer** (`entity/`)
   - Pure data models with JPA annotations
   - Only @Getters, @Setters, @Data from Lombok
   - No business logic here

2. **Repository Layer** (`repository/`)
   - Spring Data JPA interfaces
   - Custom query methods using `@Query`
   - Database operations only

3. **Service Layer** (`service/`)
   - All business logic lives here
   - Complex calculations and transformations
   - Orchestration of multiple repositories
   - Exception handling

4. **Controller Layer** (`controller/`)
   - HTTP request/response handling
   - Request validation
   - Route requests to services
   - Serialize responses

5. **Security Layer** (`security/`)
   - JWT token management
   - Authentication filters
   - Authorization logic

6. **Configuration** (`config/`)
   - Spring configuration beans
   - Security configuration
   - Startup initialization

---

## Deployment

### Docker Deployment

#### Build Docker Image
```bash
# Build image
docker build -t attendance:latest .

# Run container
docker run -d \
  --name attendance \
  -p 8081:8081 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/attendance_db \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=password \
  -e JWT_SECRET=your_secret \
  attendance:latest
```

#### Docker Compose (Multi-Container)
```bash
# Start services
docker-compose up -d

# View logs
docker-compose logs -f attendance

# Stop services
docker-compose down
```

**Services defined in `docker-compose.yml`:**
- PostgreSQL database
- Attendance application
- Networking between services
- Volume persistence

### Render.com Deployment

The project includes `render.yaml` for automated deployment on Render:

**Deployment Steps:**
1. Push code to GitHub
2. Connect Render to GitHub repository
3. Render automatically detects `render.yaml`
4. Services created:
   - PostgreSQL database
   - Web service (Java app)
5. Environment variables configured automatically
6. Application runs at: `https://your-app.onrender.com`

**Render.yaml defines:**
```yaml
services:
  - type: web
    name: attendance-app
    runtime: java
    buildCommand: ./mvnw clean install -DskipTests
    startCommand: java -jar target/Attendance-0.0.1-SNAPSHOT.jar
    envVars:
      - name: SPRING_DATASOURCE_URL
        fromDatabase:
          name: attendance_db
          property: connectionString
      - name: JWT_SECRET
        sync: false
  
  - type: pserv
    name: attendance_db
    runtime: postgresql
    version: 16
```

### Environment Variables Required

```bash
SPRING_DATASOURCE_URL       # Database connection string
SPRING_DATASOURCE_USERNAME  # Database user
SPRING_DATASOURCE_PASSWORD  # Database password
JWT_SECRET                  # JWT signing key (min 32 chars recommended)
PORT                        # Server port (default: 8081)
```

### Production Checklist

- [ ] Database backups enabled
- [ ] JWT secret changed from default
- [ ] HTTPS/TLS enabled
- [ ] Environment variables configured
- [ ] Application logs monitored
- [ ] Database connections pooled (HikariCP configured)
- [ ] CORS origins updated (not `*` in production)
- [ ] Default admin credentials changed
- [ ] Database schema migrated
- [ ] Application performance tested

---

## Summary

This **Attendance Management System** is a production-ready Spring Boot application that provides:

✅ **Robust Authentication** - JWT-based security with role-based access control
✅ **Flexible Attendance Tracking** - Two methods (aggregate and date-based)
✅ **Smart Calculations** - Exam eligibility determination with future projections
✅ **Academic Management** - Calendar, exam schedules, and holidays
✅ **Clean Architecture** - Proper separation of concerns (Controllers → Services → Repositories)
✅ **Scalability** - PostgreSQL with connection pooling
✅ **Deployment Ready** - Docker and Render.com support

### For New Engineers

Start by understanding:
1. **Database Schema** - Review the entity classes and their relationships
2. **User Journey** - Follow a request through the security filter → controller → service → repository
3. **Business Logic** - Study `AttendanceCalculationService.java` for core algorithms
4. **Security** - Review `SecurityConfig.java` and `JwtAuthenticationFilter.java`
5. **API Contract** - Check controllers for endpoint documentation

The codebase is well-structured, properly documented, and ready for extension!
