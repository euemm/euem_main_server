# API Documentation

This document describes all available API endpoints for the EUEM Main Server application.

## Base URL

All endpoints are relative to the base URL of the server. The default base URL is typically `http://localhost:8080` unless configured otherwise.

## Authentication

Most endpoints require JWT authentication. Include the JWT token in the Authorization header:

```
Authorization: Bearer <your-jwt-token>
```

The JWT token is obtained from the `/auth/login` endpoint and has an expiration time configured in the application properties.

## Endpoints

### Health Check

#### GET /healthz

Check the health status of the server.

**Authentication:** Not required

**Response:**
```json
{
    "status": "ok"
}
```

**Status Codes:**
- `200 OK` - Server is healthy

---

### Authentication Endpoints

All authentication endpoints are publicly accessible and do not require authentication.

#### POST /auth/register

Register a new user account.

**Authentication:** Not required

**Request Body:**
```json
{
    "email": "user@example.com",
    "password": "password123",
    "firstName": "John",
    "lastName": "Doe"
}
```

**Validation Rules:**
- `email`: Required, must be a valid email address
- `password`: Required, must be at least 8 characters long
- `firstName`: Required, must be between 2 and 50 characters
- `lastName`: Required, must be between 2 and 50 characters

**Response:**
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

**Status Codes:**
- `200 OK` - Registration successful
- `400 Bad Request` - Validation error or user already exists
- `409 Conflict` - User with this email already exists

**Notes:**
- After registration, a verification email with an OTP code is sent to the provided email address
- The user must verify their email before they can fully use the account

---

#### POST /auth/verify-email

Verify the user's email address using the OTP code sent during registration.

**Authentication:** Not required

**Request Body:**
```json
{
    "otpCode": "123456"
}
```

**Validation Rules:**
- `otpCode`: Required, must be exactly 6 digits

**Response:**
```json
{
    "message": "Email verified successfully",
    "success": true
}
```

**Status Codes:**
- `200 OK` - Email verified successfully
- `400 Bad Request` - Invalid or expired OTP code
- `404 Not Found` - OTP code not found

**Notes:**
- The OTP code expires after a configured time period
- Each OTP code can only be used once

---

#### POST /auth/resend-otp

Resend the verification OTP code to the user's email address.

**Authentication:** Not required

**Query Parameters:**
- `email` (required): The email address to resend the OTP code to

**Example Request:**
```
POST /auth/resend-otp?email=user@example.com
```

**Response:**
```json
{
    "message": "Verification code sent to your email",
    "success": true
}
```

**Status Codes:**
- `200 OK` - OTP code sent successfully
- `400 Bad Request` - Invalid email or user not found

---

#### POST /auth/login

Authenticate a user and receive a JWT token.

**Authentication:** Not required

**Request Body:**
```json
{
    "email": "user@example.com",
    "password": "password123"
}
```

**Validation Rules:**
- `email`: Required, must be a valid email address
- `password`: Required

**Response:**
```json
{
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600000,
    "user": {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "email": "user@example.com",
        "firstName": "John",
        "lastName": "Doe",
        "isVerified": true,
        "isEnabled": true,
        "createdAt": "2024-01-01T12:00:00",
        "updatedAt": "2024-01-01T12:00:00",
        "roles": ["USER"]
    }
}
```

**Status Codes:**
- `200 OK` - Login successful
- `400 Bad Request` - Validation error
- `401 Unauthorized` - Invalid credentials

**Notes:**
- The `accessToken` should be included in the Authorization header for subsequent authenticated requests
- The `expiresIn` value is in milliseconds
- The token type is always "Bearer"

---

### User Endpoints

All user endpoints require JWT authentication. Include the JWT token in the Authorization header.

#### GET /users/profile

Get the current authenticated user's profile information.

**Authentication:** Required

**Response:**
```json
{
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "isVerified": true,
    "isEnabled": true,
    "createdAt": "2024-01-01T12:00:00",
    "updatedAt": "2024-01-01T12:00:00",
    "roles": ["USER"]
}
```

**Status Codes:**
- `200 OK` - Profile retrieved successfully
- `401 Unauthorized` - Invalid or missing authentication token

---

#### PUT /users/profile

Update the current authenticated user's profile information.

**Authentication:** Required

**Request Body:**
```json
{
    "firstName": "Jane",
    "lastName": "Smith"
}
```

**Validation Rules:**
- `firstName`: Optional, if provided must be between 2 and 50 characters
- `lastName`: Optional, if provided must be between 2 and 50 characters

**Response:**
```json
{
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "firstName": "Jane",
    "lastName": "Smith",
    "isVerified": true,
    "isEnabled": true,
    "createdAt": "2024-01-01T12:00:00",
    "updatedAt": "2024-01-01T12:30:00",
    "roles": ["USER"]
}
```

**Status Codes:**
- `200 OK` - Profile updated successfully
- `400 Bad Request` - Validation error
- `401 Unauthorized` - Invalid or missing authentication token

**Notes:**
- Only the fields provided in the request body will be updated
- The `updatedAt` timestamp is automatically updated

---

#### PUT /users/change-email

Request to change the user's email address. A verification code will be sent to the new email address.

**Authentication:** Required

**Request Body:**
```json
{
    "newEmail": "newemail@example.com"
}
```

**Validation Rules:**
- `newEmail`: Required, must be a valid email address

**Response:**
```json
{
    "message": "Verification code sent to new email address",
    "success": true
}
```

**Status Codes:**
- `200 OK` - Verification code sent successfully
- `400 Bad Request` - Validation error or email already in use
- `401 Unauthorized` - Invalid or missing authentication token
- `409 Conflict` - Email address already exists

**Notes:**
- The email change is not completed until the verification code is verified using `/users/verify-new-email`
- The old email address remains active until verification is complete

---

#### POST /users/verify-new-email

Verify the new email address using the OTP code sent to the new email.

**Authentication:** Required

**Request Body:**
```json
{
    "otpCode": "123456"
}
```

**Validation Rules:**
- `otpCode`: Required, must be exactly 6 digits

**Response:**
```json
{
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "newemail@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "isVerified": true,
    "isEnabled": true,
    "createdAt": "2024-01-01T12:00:00",
    "updatedAt": "2024-01-01T13:00:00",
    "roles": ["USER"]
}
```

**Status Codes:**
- `200 OK` - Email changed successfully
- `400 Bad Request` - Invalid or expired OTP code
- `401 Unauthorized` - Invalid or missing authentication token
- `404 Not Found` - OTP code not found

**Notes:**
- After successful verification, the user's email address is updated
- The OTP code expires after a configured time period

---

#### PUT /users/change-password

Change the user's password.

**Authentication:** Required

**Request Body:**
```json
{
    "currentPassword": "oldpassword123",
    "newPassword": "newpassword123"
}
```

**Validation Rules:**
- `currentPassword`: Required
- `newPassword`: Required, must be at least 8 characters long

**Response:**
```json
{
    "message": "Password changed successfully",
    "success": true
}
```

**Status Codes:**
- `200 OK` - Password changed successfully
- `400 Bad Request` - Validation error or invalid current password
- `401 Unauthorized` - Invalid or missing authentication token

**Notes:**
- The current password must be correct for the password change to succeed
- After password change, the user will need to login again with the new password

---

#### DELETE /users/account

Delete the current authenticated user's account.

**Authentication:** Required

**Response:**
```json
{
    "message": "Account deleted successfully",
    "success": true
}
```

**Status Codes:**
- `200 OK` - Account deleted successfully
- `401 Unauthorized` - Invalid or missing authentication token

**Notes:**
- This action is permanent and cannot be undone
- All user data associated with the account will be deleted

---

### Test Endpoints

Test endpoints are available for development and testing purposes. These endpoints may not be available in production environments.

#### GET /test/database

Test the database connection and check if default roles exist.

**Authentication:** Not required

**Response:**
```json
{
    "userCount": 5,
    "roleCount": 2,
    "hasUserRole": true,
    "hasAdminRole": true,
    "status": "Database connection successful"
}
```

**Status Codes:**
- `200 OK` - Database connection successful
- `500 Internal Server Error` - Database connection failed

**Error Response:**
```json
{
    "status": "Database connection failed",
    "error": "Connection timeout"
}
```

---

#### POST /test/email

Send a test verification email to the specified email address.

**Authentication:** Not required

**Query Parameters:**
- `email` (required): The email address to send the test email to

**Example Request:**
```
POST /test/email?email=test@example.com
```

**Response:**
```json
{
    "status": "Email sent successfully",
    "message": "Check your email for verification code"
}
```

**Status Codes:**
- `200 OK` - Email sent successfully
- `500 Internal Server Error` - Email sending failed

**Error Response:**
```json
{
    "status": "Email sending failed",
    "error": "SMTP connection error"
}
```

---

## Error Responses

All endpoints may return error responses in the following format:

### Validation Error Response

```json
{
    "message": "Validation failed",
    "errors": [
        {
            "field": "email",
            "message": "Email should be valid"
        },
        {
            "field": "password",
            "message": "Password must be at least 8 characters long"
        }
    ]
}
```

### Standard Error Response

```json
{
    "message": "Error message describing what went wrong",
    "timestamp": "2024-01-01T12:00:00",
    "status": 400,
    "error": "Bad Request",
    "path": "/auth/register"
}
```

## Common HTTP Status Codes

- `200 OK` - Request successful
- `400 Bad Request` - Invalid request data or validation error
- `401 Unauthorized` - Authentication required or invalid token
- `404 Not Found` - Resource not found
- `409 Conflict` - Resource conflict (e.g., email already exists)
- `500 Internal Server Error` - Server error

## CORS Configuration

The API supports Cross-Origin Resource Sharing (CORS) with the following configuration:
- Allowed origins: All origins (`*`)
- Allowed methods: GET, POST, PUT, DELETE, OPTIONS
- Allowed headers: All headers (`*`)
- Credentials: Allowed

## Notes

- All timestamps are in ISO 8601 format (e.g., `2024-01-01T12:00:00`)
- UUIDs are in standard UUID format (e.g., `550e8400-e29b-41d4-a716-446655440000`)
- Password requirements: Minimum 8 characters
- OTP codes are 6-digit numeric codes
- JWT tokens are stateless and do not require server-side session storage
- Email verification is required for full account functionality


