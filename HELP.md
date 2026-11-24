# Lab Scheduler

A Java 17 Maven project structured with standard tiered architecture.

## Package Architecture
- `com.lab.model` - Data models and domain entities.
- `com.lab.service` - Business logic and services.
- `com.lab.dao` - Data Access Objects for database persistence.
- `com.lab.web` - Web controllers, servlets, and endpoints.
- `com.lab.Main` - Application entry point.

## Prerequisites
- Java 17+ JDK
- Maven 3.8+ (or use included `mvnw` / `mvnw.cmd`)

## Running the Project
```bash
# Compile and run via Exec Maven Plugin
mvn clean compile exec:java

# Or build the JAR and run it
mvn clean package
java -jar target/lab-scheduler-1.0-SNAPSHOT.jar

# Run JUnit 5 tests
mvn test
```
