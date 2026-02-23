# Stage 1: Build stage for compiling the Spring Boot application
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy the Maven Wrapper and .mvn directory first to leverage Docker layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Ensure the Maven Wrapper is executable
RUN chmod +x mvnw

# Pre-download dependencies to optimize subsequent builds
RUN ./mvnw dependency:go-offline -B

# Copy source code and build the JAR file
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime stage with minimal footprint
FROM eclipse-temurin:21-jre
WORKDIR /app

# Security best practice: Create a non-root system user for application runtime
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

# Copy the generated JAR file from the build stage - assuming single artifact in target/
# Using a glob pattern and copying it as a generic name 'app.jar'
COPY --from=build /app/target/*.jar app.jar

# Explicitly expose the port specified by the user
EXPOSE 8081

# Command to execute the application
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
