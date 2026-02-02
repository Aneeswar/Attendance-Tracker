# 📚 Attendance Management System

A comprehensive Spring Boot-based attendance tracking and management system for educational institutions. This application helps students track their attendance, calculate eligibility for exams based on attendance thresholds, and allows administrators to manage courses, holidays, and academic calendars.

## ✨ Features

### Student Features
- **Course Management**: Add new courses or select from existing courses in the system
- **Attendance Tracking**: 
  - Enter attendance by total classes conducted and classes attended
  - Mark attendance by individual dates with a calendar interface
  - Automatic date generation based on course schedule and start date
- **Attendance Report**: View detailed attendance reports including:
  - Current attendance percentage
  - 75% eligibility status (standard threshold)
  - 65% eligibility status (medical/relaxation)
  - Classes that can be skipped while maintaining threshold
  - Projected attendance if all remaining classes are attended
  - Upcoming exam eligibility calculations
- **Holiday Calendar**: View institutional holidays and exam dates
- **Timetable Management**: Set and manage weekly class schedules

### Admin Features
- **Dashboard Statistics**: View total students, courses, and academic year
- **Academic Calendar Management**: Set exam dates (CAT-1, CAT-2, FAT) and academic year
- **Holiday Management**: Add and manage institutional holidays
- **Course Management**: Create and manage courses available for students
- **Semester Configuration**: Configure different semesters with unique exam dates and holidays

## 🛠️ Technology Stack

- **Backend**: 
  - Spring Boot 4.0.2
  - Spring Security with JWT Authentication
  - Spring Data JPA
  - PostgreSQL 16.9
  
- **Frontend**: 
  - HTML5, CSS3, JavaScript
  - Responsive design with gradient UI
  - Thymeleaf templates
  
- **Build**: Maven with Maven Wrapper
- **Java Version**: Java 21

## 📋 Prerequisites

- Java 21 or higher
- PostgreSQL 16.9 or higher
- Maven 3.8+ (or use the included Maven wrapper)

## 🚀 Installation & Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd Attendance
```

### 2. Database Setup
```bash
# Create PostgreSQL database
psql -U postgres -c "CREATE DATABASE attendance_db;"

# Update application.properties with your database credentials
# File: src/main/resources/application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/attendance_db
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### 3. Build the Project
```bash
# Using Maven wrapper (Windows)
.\mvnw.cmd clean install -DskipTests

# Using Maven wrapper (Linux/Mac)
./mvnw clean install -DskipTests
```

### 4. Run the Application
```bash
# Using Maven
.\mvnw.cmd spring-boot:run

# Or directly run the JAR
java -jar target/Attendance-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8081`

## 📖 Usage Guide

### For Students

1. **Login**: Navigate to `/login` and enter your credentials
2. **Dashboard**: Access your dashboard with quick links to:
   - Add Courses
   - Enter Attendance
   - View Attendance Report
   - Check Holidays
   
3. **Add Courses**:
   - Click "Add Courses"
   - Choose between:
     - Creating a new course with custom schedule
     - Adding an existing course from the system with your own schedule
   - Set course start date and weekly class days
   
4. **Mark Attendance**:
   - Two methods available:
     - **Total/Attended**: Enter total classes conducted and attended
     - **Mark by Date**: Select individual dates and mark present/absent
   
5. **View Reports**: Check your eligibility status for upcoming exams

### For Admins

1. **Login**: Navigate to `/admin/login`
2. **Dashboard**: View system statistics
3. **Academic Calendar**: Set exam dates and academic year
4. **Manage Holidays**: Add institutional holidays
5. **Manage Courses**: Create courses available for students

## 📁 Project Structure

```
Attendance/
├── src/
│   ├── main/
│   │   ├── java/com/deepak/Attendance/
│   │   │   ├── config/           # Security and web configuration
│   │   │   ├── controller/       # REST and Page controllers
│   │   │   ├── dto/              # Data Transfer Objects
│   │   │   ├── entity/           # JPA entities
│   │   │   ├── repository/       # Data access layer
│   │   │   ├── security/         # JWT and security utilities
│   │   │   └── service/          # Business logic
│   │   └── resources/
│   │       ├── templates/        # HTML templates
│   │       ├── static/           # Static files
│   │       └── tessdata/         # OCR data
│   └── test/
└── pom.xml
```

## 🔑 Key APIs

### Authentication
- `POST /api/auth/login` - User login
- `POST /api/auth/register` - User registration

### Student Endpoints
- `GET /api/student/courses` - Get student's courses
- `POST /api/student/courses` - Add new course
- `GET /api/student/available-courses` - Get available courses to add
- `GET /api/student/search-courses` - Search for courses
- `POST /api/student/add-existing-course` - Add existing course
- `POST /api/student/attendance/save` - Save total/attended attendance
- `POST /api/student/date-attendance` - Save date-based attendance
- `GET /api/student/attendance-report` - Get attendance report

### Admin Endpoints
- `GET /api/admin/academic-calendar/current` - Get current academic calendar
- `PUT /api/admin/academic-calendar` - Update academic calendar
- `GET /api/admin/academic-calendar/stats` - Get dashboard statistics
- `GET /api/admin/holidays` - Get all holidays
- `POST /api/admin/holidays` - Add holiday

## 🎯 Attendance Calculation Logic

### Eligibility Determination
- **Standard Threshold**: 75% attendance required
- **Relaxed Threshold**: 65% (with medical/other relaxation)
- **Ceiling Rounding**: Used for percentage comparisons (74.1% = 75%)

### Future Classes Calculation
- Excludes weekends (only Tue-Sat count)
- Excludes institutional holidays
- Excludes exam periods (CAT-1, CAT-2, FAT)
- For CAT exams: Excludes day before exam start
- For FAT exam: Includes last working day

### Attendance Override
- Saving **total/attended** auto-generates individual date records
- Saving **mark-by-date** overrides and deletes total/attended record
- Most recent save method becomes the source of truth

## 🔐 Security Features

- JWT-based authentication
- Role-based access control (STUDENT/ADMIN)
- Password hashing with bcrypt
- Secure API endpoints with authorization checks
- CORS protection

## 📝 Configuration

### Application Properties
```properties
spring.application.name=Attendance
spring.datasource.url=jdbc:postgresql://localhost:5432/attendance_db
spring.datasource.username=postgres
spring.datasource.password=password
spring.jpa.hibernate.ddl-auto=update
server.port=8081
app.jwt.secret=your-secret-key
app.jwt.expirationMs=86400000
app.upload.dir=uploads/timetables
```

## 🤝 Contributing

1. Create a feature branch (`git checkout -b feature/AmazingFeature`)
2. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
3. Push to the branch (`git push origin feature/AmazingFeature`)
4. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👤 Author

**Deepak** - Project Creator

## 🆘 Support

For issues and questions, please create an issue in the repository.

## 🗺️ Roadmap

- [ ] Mobile application (Flutter/React Native)
- [ ] Advanced analytics and reporting
- [ ] Batch student import
- [ ] Email notifications for attendance alerts
- [ ] Integration with external authentication systems
- [ ] Multi-semester management
- [ ] Performance optimizations

## 📚 Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

---

**Last Updated**: February 2, 2026
