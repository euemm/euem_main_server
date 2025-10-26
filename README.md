# EUEM Main Server

A comprehensive Spring Boot backend server providing complete user management with authentication, email verification, and profile management capabilities. Built with modern security practices including OAuth2 resource server, JWT tokens, and comprehensive validation.

## Overview

This application provides a robust user management system with secure authentication, email verification via OTP codes, and comprehensive profile management features. The system is built using Spring Boot 3.2.0 with Spring Security, featuring JWT-based stateless authentication and OAuth2 resource server configuration.

## Features

### Core User Management
- User Registration with email validation
- Email Verification using 6-digit OTP codes with multiple token types
- Secure Authentication with JWT tokens via OAuth2 resource server
- Profile Management (update name, change email with verification)
- Password Management (change password with current password validation)
- Account Deletion (soft delete functionality)
- Role-Based Access Control (USER, ADMIN roles)

### Security Features
- BCrypt Password Hashing for secure password storage
- JWT Token Authentication with configurable expiration (24 hours default)
- OAuth2 Resource Server implementation for token validation
- CORS Configuration for cross-origin requests
- Input Validation using Bean Validation (JSR-303)
- Global Exception Handling with structured error responses
- Comprehensive logging with multiple levels

### Email System
- OTP Generation (6-digit random codes)
- SMTP Integration for email delivery with multiple provider support
- Email Templates for different verification types (EMAIL_VERIFICATION, PASSWORD_RESET, EMAIL_CHANGE)
- Token Expiry Management (15 minutes default)
- Resend Functionality for failed deliveries
- Email change verification workflow

### Development & Testing
- Built-in testing endpoints for database and email functionality
- Development and production profile configuration
- Comprehensive error handling with custom exceptions
- Detailed logging for debugging and monitoring

## Project Structure

```
src/main/java/com/euem/server/
├── config/                          # Configuration classes
│   ├── AuthenticationConfig.java    # Authentication configuration
│   ├── DataInitializer.java         # Database initialization
│   └── SecurityConfig.java          # Security configuration with OAuth2
├── controller/                      # REST API endpoints
│   ├── AuthController.java          # Authentication endpoints (register, login, verify-email, resend-otp)
│   ├── UserController.java          # User management endpoints (profile, change-email, change-password, delete-account)
│   └── TestController.java          # Testing endpoints (database, email testing)
├── dto/                             # Data Transfer Objects
│   ├── request/                     # Request DTOs with validation
│   │   ├── ChangeEmailRequest.java
│   │   ├── ChangePasswordRequest.java
│   │   ├── LoginRequest.java
│   │   ├── RegisterRequest.java
│   │   ├── UpdateProfileRequest.java
│   │   └── VerifyEmailRequest.java
│   └── response/                    # Response DTOs
│       ├── AuthResponse.java
│       ├── MessageResponse.java
│       └── UserResponse.java
├── entity/                          # JPA Entities with relationships
│   ├── Role.java                    # Role entity with enum (USER, ADMIN)
│   ├── User.java                    # User entity with timestamps and soft delete
│   └── VerificationToken.java       # Token entity with expiry and type enum
├── exception/                       # Custom exceptions and global handling
│   ├── GlobalExceptionHandler.java  # Centralized exception handling
│   ├── InvalidOtpException.java
│   ├── InvalidPasswordException.java
│   ├── OtpExpiredException.java
│   ├── UserAlreadyExistsException.java
│   └── UserNotFoundException.java
├── repository/                      # JPA Repositories with custom queries
│   ├── RoleRepository.java
│   ├── UserRepository.java
│   └── VerificationTokenRepository.java
├── security/                        # Security components
│   ├── CustomUserDetailsService.java
│   ├── CustomUserPrincipal.java
│   └── JwtTokenProvider.java        # JWT token generation and validation
├── service/                         # Business logic layer
│   ├── EmailService.java            # Email sending with templates
│   └── UserService.java             # User management operations
├── util/                            # Utility classes
│   └── UuidGenerator.java
└── EuemMainServerApplication.java   # Main application class

src/main/resources/
├── application.yml                  # Production configuration
└── application-dev.yml             # Development configuration
```

## Technology Stack

- **Java 21** - Programming language
- **Spring Boot 3.2.0** - Application framework
- **Spring Security 6** - Authentication and authorization with OAuth2 resource server
- **Spring Data JPA** - Database abstraction and ORM
- **PostgreSQL** - Primary relational database
- **Gradle** - Build automation and dependency management
- **JWT (JJWT 0.11.5)** - Token-based authentication
- **BCrypt** - Password hashing algorithm
- **JavaMailSender** - Email functionality
- **Lombok** - Boilerplate code reduction
- **Hibernate** - JPA implementation with PostgreSQL dialect

## Prerequisites

- **Java 21** or higher (JDK 21)
- **PostgreSQL 15** or higher
- **SMTP Server** (for email functionality - MailHog for development, real SMTP for production)

## Database Setup

### PostgreSQL Installation

#### macOS with Homebrew
```bash
brew install postgresql@15
brew services start postgresql@15
```

#### Ubuntu/Debian
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
```

#### Windows
Download and install PostgreSQL from https://www.postgresql.org/download/windows/

### Database Configuration

1. **Connect to PostgreSQL:**
   ```bash
   psql -U postgres
   ```

2. **Create Database and User:**
   ```sql
   CREATE DATABASE euem_db;
   CREATE USER euem_user WITH PASSWORD 'euem_password';
   GRANT ALL PRIVILEGES ON DATABASE euem_db TO euem_user;
   ALTER USER euem_user CREATEDB;
   ```

3. **Verify Connection:**
   ```bash
   psql -h localhost -U euem_user -d euem_db
   ```

4. **Create Development Database:**
   ```sql
   CREATE DATABASE euem_dev_db;
   CREATE USER euem_dev_user WITH PASSWORD 'euem_dev_password';
   GRANT ALL PRIVILEGES ON DATABASE euem_dev_db TO euem_dev_user;
   ```

## Email Configuration

### Option 1: Local Development with MailHog

1. **Install MailHog:**
   ```bash
   # macOS
   brew install mailhog

   # Ubuntu/Debian
   go install github.com/mailhog/MailHog@latest

   # Or download binary from https://github.com/mailhog/MailHog/releases
   ```

2. **Start MailHog:**
   ```bash
   # macOS
   mailhog

   # Or run the binary
   ./MailHog_linux_amd64

   # Access web interface at http://localhost:8025
   ```

3. **Development Configuration (application-dev.yml):**
   ```yaml
   spring:
     mail:
       host: localhost
       port: 1025
       username: test
       password: test
       properties:
         mail:
           smtp:
             auth: false
             starttls:
               enable: false
   ```

### Option 2: Production SMTP Server

Update `application.yml`:
```yaml
spring:
  mail:
    host: smtp.gmail.com          # or your SMTP provider
    port: 587                     # or 465 for SSL
    username: your-email@gmail.com
    password: your-app-password   # or API key
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

### Option 3: Popular SMTP Services

#### Gmail SMTP
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-app-specific-password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

#### SendGrid SMTP
```yaml
spring:
  mail:
    host: smtp.sendgrid.net
    port: 587
    username: apikey
    password: your-sendgrid-api-key
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

## Running the Application

### Development Mode

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd euem-main-server
   ```

2. **Make scripts executable:**
   ```bash
   chmod +x gradlew
   ```

3. **Run with development profile:**
   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=dev'
   ```

4. **Or set environment variable:**
   ```bash
   export SPRING_PROFILES_ACTIVE=dev
   ./gradlew bootRun
   ```

### Production Mode

1. **Build the application:**
   ```bash
   ./gradlew clean build
   ```

2. **Run the JAR:**
   ```bash
   java -jar build/libs/euem-main-server-0.0.1-SNAPSHOT.jar
   ```

3. **Or run with production profile:**
   ```bash
   java -jar build/libs/euem-main-server-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
   ```

### Verify Startup

The application will start on `http://localhost:8080` and you should see logs indicating:
- Database connection successful
- Default roles (USER, ADMIN) created automatically
- Email configuration loaded
- Security configuration active

## Security Implementation

### Authentication Architecture

The application uses a multi-layered security approach:

1. **OAuth2 Resource Server** - Primary JWT token validation
2. **JWT Token Provider** - Token generation and parsing utilities
3. **BCrypt Password Encoder** - Secure password hashing
4. **Role-Based Access Control** - USER and ADMIN roles with method-level security

### Authentication Flow

1. **User Registration:**
   - Email and password validation
   - Password hashed with BCrypt (strength 10)
   - USER role automatically assigned
   - Verification email sent with 6-digit OTP

2. **Email Verification:**
   - OTP code valid for 15 minutes
   - Supports multiple token types (EMAIL_VERIFICATION, PASSWORD_RESET, EMAIL_CHANGE)
   - Successful verification marks user as verified

3. **Login Process:**
   - Credentials validated against database
   - JWT token generated with user claims
   - Token validated by OAuth2 resource server
   - Token expires in 24 hours (configurable)

### Security Configuration

#### Password Security
- **Algorithm:** BCrypt with strength 10
- **Salt:** Automatically generated per password
- **Storage:** Securely hashed in database

#### JWT Configuration
- **Algorithm:** HMAC-SHA (HS256)
- **Secret Key:** Configurable via `app.jwt.secret` (minimum 256 bits)
- **Expiration:** 24 hours default (86400000 milliseconds)
- **Issuer:** Configurable via OAuth2 resource server settings

#### CORS Policy
- **Allowed Origins:** Configurable patterns (default: all)
- **Allowed Methods:** GET, POST, PUT, DELETE, OPTIONS
- **Allowed Headers:** All headers
- **Credentials:** Enabled for authenticated requests

## API Documentation

### Base URL
```
http://localhost:8080/api
```

### Authentication Endpoints

#### Register User
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Response (200):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "isVerified": false,
  "isEnabled": true,
  "createdAt": "2024-01-01T12:00:00",
  "updatedAt": "2024-01-01T12:00:00",
  "roles": ["USER"]
}
```

#### Verify Email
```http
POST /api/auth/verify-email
Content-Type: application/json

{
  "otpCode": "123456"
}
```

**Response (200):** `MessageResponse` with success message

#### Resend OTP
```http
POST /api/auth/resend-otp?email=user@example.com
```

**Response (200):** `MessageResponse` with success message

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response (200):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "isVerified": true,
    "roles": ["USER"]
  }
}
```

### User Management Endpoints (Protected)

All user management endpoints require JWT authentication via `Authorization: Bearer <token>` header.

#### Get User Profile
```http
GET /api/users/profile
Authorization: Bearer <jwt-token>
```

**Response (200):** `UserResponse` object

#### Update Profile
```http
PUT /api/users/profile
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "firstName": "Jane",
  "lastName": "Smith"
}
```

**Response (200):** Updated `UserResponse` object

#### Change Email
```http
PUT /api/users/change-email
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "newEmail": "newemail@example.com"
}
```

**Response (200):** `MessageResponse` indicating verification email sent

#### Verify New Email
```http
POST /api/users/verify-new-email
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "otpCode": "654321"
}
```

**Response (200):** Updated `UserResponse` object

#### Change Password
```http
PUT /api/users/change-password
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "currentPassword": "oldpassword123",
  "newPassword": "newpassword456"
}
```

**Response (200):** `MessageResponse` with success message

#### Delete Account
```http
DELETE /api/users/account
Authorization: Bearer <jwt-token>
```

**Response (200):** `MessageResponse` with success message

### Testing Endpoints

#### Test Database Connection
```http
GET /api/test/database
```

**Response (200):**
```json
{
  "userCount": 0,
  "roleCount": 2,
  "hasUserRole": true,
  "hasAdminRole": true,
  "status": "Database connection successful"
}
```

**Response (500):** Database connection error details

#### Test Email Service
```http
POST /api/test/email?email=test@example.com
```

**Response (200):**
```json
{
  "status": "Email sent successfully",
  "message": "Check your email for verification code"
}
```

**Response (500):** Email service error details

## Database Schema

### Users Table
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_enabled ON users(is_enabled);
```

### Roles Table
```sql
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) UNIQUE NOT NULL
);

INSERT INTO roles (name) VALUES ('USER'), ('ADMIN');
```

### User Roles Junction Table
```sql
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_user ON user_roles(user_id);
CREATE INDEX idx_user_roles_role ON user_roles(role_id);
```

### Verification Tokens Table
```sql
CREATE TABLE verification_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    otp_code VARCHAR(6) NOT NULL,
    expiry_time TIMESTAMP NOT NULL,
    type VARCHAR(50) NOT NULL
);

CREATE INDEX idx_verification_tokens_user_type ON verification_tokens(user_id, type);
CREATE INDEX idx_verification_tokens_otp_expiry ON verification_tokens(otp_code, type, expiry_time);
CREATE INDEX idx_verification_tokens_expiry ON verification_tokens(expiry_time);
```

## Configuration

### Production Configuration (application.yml)
```yaml
server:
  port: 8080

spring:
  application:
    name: euem-main-server

  datasource:
    url: jdbc:postgresql://localhost:5432/euem_db
    username: euem_user
    password: ${DB_PASSWORD:euem_password}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: false

  mail:
    host: ${SMTP_HOST:localhost}
    port: ${SMTP_PORT:587}
    username: ${SMTP_USERNAME:euem_smtp_user}
    password: ${SMTP_PASSWORD:euem_smtp_password}
    properties:
      mail:
        smtp:
          auth: ${SMTP_AUTH:true}
          starttls:
            enable: ${SMTP_STARTTLS:true}

  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${JWT_ISSUER_URI:http://localhost:8080}
          secret: ${JWT_SECRET:euem-jwt-secret-key-that-is-at-least-256-bits-long-for-security-purposes}

logging:
  level:
    com.euem.server: INFO
    org.springframework.security: WARN
    org.hibernate.SQL: WARN

app:
  jwt:
    secret: ${JWT_SECRET:euem-jwt-secret-key-that-is-at-least-256-bits-long-for-security-purposes}
    expiration: 86400000 # 24 hours in milliseconds

  otp:
    expiry-minutes: ${OTP_EXPIRY_MINUTES:15}
    length: ${OTP_LENGTH:6}
```

### Development Configuration (application-dev.yml)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/euem_dev_db
    username: euem_dev_user
    password: euem_dev_password

  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

  mail:
    host: localhost
    port: 1025  # MailHog port
    username: test
    password: test
    properties:
      mail:
        smtp:
          auth: false
          starttls:
            enable: false

logging:
  level:
    com.euem.server: DEBUG
    org.springframework.security: DEBUG
    org.springframework.web: DEBUG
```

## Environment Variables

For production deployment, use these environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_PASSWORD` | Database password | euem_password |
| `SMTP_HOST` | SMTP server hostname | localhost |
| `SMTP_PORT` | SMTP server port | 587 |
| `SMTP_USERNAME` | SMTP username | euem_smtp_user |
| `SMTP_PASSWORD` | SMTP password | euem_smtp_password |
| `SMTP_AUTH` | Enable SMTP authentication | true |
| `SMTP_STARTTLS` | Enable STARTTLS | true |
| `JWT_SECRET` | JWT signing secret (min 256 bits) | [generated] |
| `JWT_ISSUER_URI` | JWT issuer URI | http://localhost:8080 |
| `OTP_EXPIRY_MINUTES` | OTP expiration in minutes | 15 |
| `OTP_LENGTH` | OTP code length | 6 |

## Testing

### Manual Testing

1. **Test Database Connection:**
   ```bash
   curl http://localhost:8080/api/test/database
   ```

2. **Test User Registration:**
   ```bash
   curl -X POST http://localhost:8080/api/auth/register \
     -H "Content-Type: application/json" \
     -d '{
       "email": "test@example.com",
       "password": "password123",
       "firstName": "Test",
       "lastName": "User"
     }'
   ```

3. **Test Login:**
   ```bash
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{
       "email": "test@example.com",
       "password": "password123"
     }'
   ```

4. **Test Email Service:**
   ```bash
   curl -X POST "http://localhost:8080/api/test/email?email=test@example.com"
   ```

### Automated Testing

Run the full test suite:
```bash
./gradlew test
```

Run tests with coverage:
```bash
./gradlew test jacocoTestReport
```

Run specific test class:
```bash
./gradlew test --tests "*UserServiceTest*"
```

## Deployment

### Docker Deployment

1. **Create Dockerfile:**
   ```dockerfile
   FROM eclipse-temurin:21-jdk
   COPY build/libs/euem-main-server-0.0.1-SNAPSHOT.jar app.jar
   EXPOSE 8080
   ENTRYPOINT ["java", "-jar", "/app.jar"]
   ```

2. **Build and run:**
   ```bash
   docker build -t euem-server .
   docker run -p 8080:8080 \
     -e DB_PASSWORD=your_password \
     -e SMTP_PASSWORD=your_smtp_password \
     -e JWT_SECRET=your_jwt_secret \
     euem-server
   ```

### Docker Compose

```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_PASSWORD=${DB_PASSWORD}
      - SMTP_PASSWORD=${SMTP_PASSWORD}
      - JWT_SECRET=${JWT_SECRET}
    depends_on:
      - postgres
      - mailhog

  postgres:
    image: postgres:15
    environment:
      - POSTGRES_DB=euem_db
      - POSTGRES_USER=euem_user
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data

  mailhog:
    image: mailhog/mailhog
    ports:
      - "1025:1025"
      - "8025:8025"

volumes:
  postgres_data:
```

### Production Build

```bash
# Clean build
./gradlew clean build

# Run with production profile
java -jar build/libs/euem-main-server-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --server.port=8080
```

## Error Handling

The application provides comprehensive error handling with custom exceptions and structured responses.

### Custom Exceptions

| Exception | HTTP Status | Description |
|-----------|-------------|-------------|
| `UserAlreadyExistsException` | 409 | Email address already registered |
| `UserNotFoundException` | 404 | User not found or disabled |
| `InvalidOtpException` | 400 | Invalid or expired OTP code |
| `OtpExpiredException` | 400 | OTP code has expired |
| `InvalidPasswordException` | 400 | Current password is incorrect |
| `BadCredentialsException` | 401 | Invalid login credentials |

### Error Response Format

All errors return structured JSON responses:
```json
{
  "status": 400,
  "message": "Invalid OTP code",
  "timestamp": "2024-01-01T12:00:00"
}
```

### Validation Errors

Validation errors return detailed field-level information:
```json
{
  "status": 400,
  "message": "Validation failed",
  "timestamp": "2024-01-01T12:00:00",
  "errors": {
    "email": "Email is required",
    "password": "Password must be at least 6 characters"
  }
}
```

## Logging

The application uses SLF4J with Logback for structured logging:

### Log Levels
- **ERROR** - Error conditions that require attention
- **WARN** - Warning messages for potential issues
- **INFO** - General application events and milestones
- **DEBUG** - Detailed application flow and debugging information

### Log Categories

#### Development (DEBUG level)
- `com.euem.server` - Application-specific logs
- `org.springframework.security` - Security events and authentication
- `org.springframework.web` - HTTP requests and responses
- `org.hibernate.SQL` - Database queries

#### Production (INFO level)
- `com.euem.server` - Application events only
- `org.springframework.security` - Security warnings only

### Log Formats

Application logs include:
- Timestamp with timezone
- Log level
- Thread name
- Class and method name
- Log message
- Exception stack traces (when applicable)

Example log entry:
```
2024-01-01 12:00:00.123 INFO  [http-nio-8080-exec-1] com.euem.server.service.UserService : User registered successfully: user@example.com
```

## Troubleshooting

### Common Issues

#### Database Connection Issues

**Problem:** Database connection refused
```bash
# Check if PostgreSQL is running
sudo systemctl status postgresql

# Check database configuration
psql -h localhost -U euem_user -d euem_db

# Verify application.yml configuration
cat src/main/resources/application.yml | grep -A 5 "datasource"
```

**Solution:**
1. Ensure PostgreSQL service is running
2. Verify database credentials in configuration
3. Check database server logs for connection issues
4. Confirm database and user exist with proper permissions

#### Email Service Issues

**Problem:** Email sending failed
```bash
# Check MailHog status (development)
curl http://localhost:8025/api/v1/events

# Test SMTP connection
telnet smtp.gmail.com 587

# Check email service logs
curl http://localhost:8080/api/test/email?email=test@example.com
```

**Solution:**
1. Verify SMTP configuration in application properties
2. Check SMTP server credentials and connectivity
3. For development, ensure MailHog is running on port 1025
4. Check firewall settings for SMTP ports

#### JWT Token Issues

**Problem:** JWT token validation failed
```bash
# Check JWT configuration
echo "JWT Secret length: ${#JWT_SECRET}"

# Test token generation
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "password": "password123"}'

# Decode token (use jwt.io for debugging)
```

**Solution:**
1. Ensure JWT secret is at least 256 bits (32 characters)
2. Verify token expiration settings
3. Check OAuth2 resource server configuration
4. Validate token format in Authorization header

#### Build and Dependency Issues

**Problem:** Gradle build failures
```bash
# Clear Gradle cache
./gradlew clean build --refresh-dependencies

# Check Java version
java -version

# Verify Gradle wrapper
./gradlew --version

# Check for dependency conflicts
./gradlew dependencies --configuration runtimeClasspath
```

**Solution:**
1. Ensure Java 21 is installed and active
2. Clear Gradle caches and rebuild
3. Check network connectivity for dependency downloads
4. Verify Gradle wrapper permissions

#### Application Startup Issues

**Problem:** Application fails to start
```bash
# Check for port conflicts
lsof -i :8080

# Run with debug logging
./gradlew bootRun --debug

# Check system resources
df -h
free -h
```

**Solution:**
1. Ensure port 8080 is available
2. Check system resources (memory, disk space)
3. Verify configuration file syntax
4. Check application logs for specific error messages

### Performance Monitoring

Monitor these metrics in production:

1. **Response Times** - Track API endpoint performance
2. **Database Connections** - Monitor connection pool usage
3. **Memory Usage** - Watch for memory leaks
4. **Error Rates** - Track application error percentages
5. **Email Delivery** - Monitor SMTP service performance

### Security Monitoring

1. **Authentication Failures** - Monitor login attempts
2. **Token Validation** - Track JWT token issues
3. **Role Authorization** - Monitor access control violations
4. **Email Security** - Track verification email patterns

## Additional Resources

### Documentation
- [Spring Boot 3.2.0 Documentation](https://docs.spring.io/spring-boot/docs/3.2.0/reference/html/)
- [Spring Security 6 Reference](https://docs.spring.io/spring-security/reference/)
- [Spring Data JPA Documentation](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [PostgreSQL 15 Documentation](https://www.postgresql.org/docs/15/)

### Security
- [JWT.io - JSON Web Tokens](https://jwt.io/) - Token debugging and validation
- [OWASP Security Guidelines](https://owasp.org/www-project-top-ten/) - Security best practices
- [OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)

### Email Services
- [MailHog Documentation](https://github.com/mailhog/MailHog) - Local SMTP testing
- [SendGrid SMTP API](https://docs.sendgrid.com/ui/account-and-settings/api-keys) - Production email service
- [Gmail SMTP Settings](https://support.google.com/a/answer/176600) - Gmail configuration

### Development Tools
- [Postman](https://www.postman.com/) - API testing
- [pgAdmin](https://www.pgadmin.org/) - PostgreSQL administration
- [Docker](https://docs.docker.com/) - Containerization
- [Gradle Documentation](https://docs.gradle.org/current/userguide/userguide.html)

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style Guidelines

- Follow Java naming conventions
- Use meaningful variable and method names
- Add JavaDoc comments for public APIs
- Write unit tests for new functionality
- Follow REST API best practices
- Use validation annotations appropriately

### Testing Requirements

- Unit tests for all business logic
- Integration tests for API endpoints
- Security tests for authentication flows
- Database tests for repository methods
- Minimum 80% code coverage

## License

This project is licensed under the MIT License. See the LICENSE file for details.

## Support

For support and questions:
1. Check the troubleshooting section above
2. Review application logs for error details
3. Test with the provided testing endpoints
4. Ensure all prerequisites are properly configured
5. Verify environment variables are set correctly

## Changelog

### Version 0.0.1-SNAPSHOT
- Initial implementation with user management
- JWT authentication with OAuth2 resource server
- Email verification with OTP codes
- Comprehensive security configuration
- Database initialization with roles
- Full REST API implementation
- Development and production profiles
- Docker support
- Comprehensive documentation