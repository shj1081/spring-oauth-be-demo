# Spring Boot OAuth2 Authentication Server

A complete, secure, and modern OAuth2 authentication server built with Spring Boot. It's designed to work seamlessly
with a single-page application (SPA) client, providing a robust backend for user authentication and authorization.

## Features

- **Secure OAuth2 Flow**: Full implementation of the OAuth2 Authorization Code Flow with GitHub as the identity
  provider.
- **JWT Implementation**: Issues short-lived Access Tokens and long-lived Refresh Tokens for a stateless architecture.
- **Advanced Token Security**:
    - **Refresh Token Rotation**: Automatically invalidates and re-issues refresh tokens upon use to enhance security.
    - **HttpOnly Cookies**: Stores Refresh Tokens in secure, HttpOnly cookies to prevent XSS attacks.
    - **Redis Integration**: Manages Refresh Tokens on the server-side using Redis for high performance and automatic
      expiration (TTL).
- **Stateless Architecture**: No HTTP sessions are used; authentication is managed entirely through JWTs.
- **Global Exception Handling**: Centralized handling of authentication, authorization, and business logic exceptions
  for consistent API responses.
- **Database Integration**: Uses Spring Data JPA and Hibernate with MySQL to persist user data.

## Architecture

- **Backend Framework**: Spring Boot 3.5.4
- **Security**: Spring Security 6+
- **Authentication**: OAuth2, JWT (jjwt library 0.11.5)
- **Database**: Spring Data JPA, MySQL
- **In-Memory Storage**: Spring Data Redis (for Refresh Token management)
- **Build Tool**: Gradle 8+
- **Language**: Java 17

## Getting Started

### Prerequisites

- Java 17 or higher
- Gradle 8+
- Docker and Docker Compose (for easy MySQL and Redis setup)
- A **GitHub OAuth App** with a Client ID and Client Secret

#### Setting up GitHub OAuth App

1. Go to [GitHub Developer Settings](https://github.com/settings/developers)
2. Click "New OAuth App"
3. Fill in the application details:
    - **Application name**: Your app name
    - **Homepage URL**: `http://localhost:8080`
    - **Authorization callback URL**: `http://localhost:8080/login/oauth2/code/github`
4. Click "Register application"
5. Copy the **Client ID** and generate a **Client Secret**

> **Note**: This server is designed to work with a frontend application running on `http://localhost:3000`. After
> successful OAuth authentication, users will be redirected to `http://localhost:3000/oauth/redirect` with a temporary
> authorization code.

### Installation & Configuration

#### 1. Set up MySQL and Redis using Docker Compose

The project includes a `docker-compose.yml` file that sets up MySQL and Redis for you. To start these services:

```bash
# Start MySQL and Redis containers in the background
docker-compose up -d

# To stop the containers when you're done
docker-compose down
```

This will start:

- **MySQL** on port `3306` with database `oauth-test` and root password `1234`
- **Redis** on port `6379`

The containers will persist data until you run `docker-compose down -v` (the `-v` flag removes volumes).

#### 2. Configure the Application

1. Clone the repository.
2. Navigate to `src/main/resources/` and copy `secret-example.yml` to `secret.yml`.
3. Edit the `secret.yml` file and fill in your credentials:

   ```yaml
   # GitHub OAuth2 Client Credentials
   spring:
     security:
       oauth2:
         client:
           registration:
             github:
               client-id: YOUR_GITHUB_CLIENT_ID
               client-secret: YOUR_GITHUB_CLIENT_SECRET
               scope:
                 - read:user
                 - user:email

     # Database Connection (for Docker Compose setup)
     datasource:
       url: jdbc:mysql://localhost:3306/oauth-test?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
       username: root
       password: 1234
       driver-class-name: com.mysql.cj.jdbc.Driver

     # Redis Connection (for Docker Compose setup)
     data:
       redis:
         host: localhost
         port: 6379

   # JWT Configuration
   jwt:
     secret: your-super-long-and-incredibly-secure-secret-key-for-hs512-must-be-at-least-64-bytes-long
     access-token-expiry: 3600000 # 1 hour
     refresh-token-expiry: 604800000 # 7 days
     auth-code-expiry: 60000 # 5 minutes
   ```

   **Note**: If you're using the provided Docker Compose setup, the database credentials are already configured
   correctly. You only need to add your GitHub OAuth credentials and JWT secret.

4. Make sure your `application.yml` file is configured to import the secrets file:
   ```yaml
   spring:
     config:
       import: optional:secret.yml
   # ... other non-secret configurations
   ```

### Running the Application

#### Using Docker Compose (Recommended)

1. Start the database and Redis services:

   ```bash
   docker-compose up -d
   ```

2. Build the project using Gradle:

   ```bash
   ./gradlew build
   ```

3. Run the application:

   ```bash
   ./gradlew bootRun
   ```

   Or alternatively:

   ```bash
   java -jar build/libs/oauth-test-0.0.1-SNAPSHOT.jar
   ```

   The server will start on `http://localhost:8080`.

### Database Setup

The application uses JPA with Hibernate and is configured to automatically create/update database tables (
`ddl-auto: update`). When you start the application for the first time:

1. The database tables will be automatically created
2. User data will be stored in the `user` table with fields: `id`, `name`, `email`, `picture`, `role`
3. User roles are managed through the `UserRole` enum (GUEST, USER)

### Frontend Integration

This backend is designed to work with a React/Vue/Angular frontend application. The typical integration flow:

1. Frontend redirects user to `http://localhost:8080/oauth2/authorization/github` for login
2. After successful GitHub OAuth, user is redirected to `http://localhost:3000/oauth/redirect?code=<temp_code>`
3. Frontend exchanges the temporary code for tokens via POST to `/api/v1/auth/token`
4. Frontend stores Access Token and uses it in `Authorization: Bearer <token>` header for API calls
5. Refresh Token is automatically handled via HttpOnly cookies

#### Manual Setup

If you prefer to set up MySQL and Redis manually instead of using Docker:

1. Install and start MySQL and Redis on your system
2. Create a database named `oauth-test` in MySQL
3. Update the database credentials in your `secret.yml` file
4. Build and run the application as shown above

## API Endpoints

The server provides these endpoints for the client:

### Authentication Flow

- `GET /oauth2/authorization/github` - Initiates the GitHub login flow (Redirect)
- `POST /api/v1/auth/token` - Exchanges an authorization code for JWTs
- `POST /api/v1/auth/refresh` - Refreshes the access token using the refresh token from HttpOnly cookie
- `POST /api/v1/auth/logout` - Logs the user out by deleting the refresh token from Redis and clearing the client cookie

### Protected Endpoints

- `GET /api/v1/user/me` - Gets the authenticated user's profile information
- `GET /api/v1/user/for-user` - check if the user has role of `USER` not the `GUEST` (the change of role can be
  reflected only when login again or refresh access token)

### Authentication Flow Details

1. **Initial Login**: User visits `/oauth2/authorization/github` to start GitHub OAuth flow
2. **GitHub Callback**: After successful GitHub authentication, user is redirected to backend with authorization code
3. **Token Exchange**: Frontend receives a temporary code and exchanges it via `/api/v1/auth/token` for JWT tokens
4. **Authenticated Requests**: Frontend uses Access Token in `Authorization: Bearer <token>` header
5. **Token Refresh**: When Access Token expires, frontend calls `/api/v1/auth/refresh` using HttpOnly cookie
6. **Logout**: Frontend calls `/api/v1/auth/logout` to invalidate tokens

## Security Features

- **Refresh Token Rotation**: A new refresh token is issued on every refresh request, invalidating the old one to
  prevent token reuse if compromised.
- **HttpOnly Cookie Storage**: Refresh tokens are stored in HttpOnly, Secure cookies, making them inaccessible to
  client-side JavaScript and mitigating XSS risks.
- **Server-Side Validation**: Refresh tokens are stored and validated against Redis, allowing the server to invalidate
  sessions at any time.
- **Temporary Authorization Codes**: Initial OAuth success creates a short-lived temporary code (5 minutes TTL) that
  must be exchanged for tokens, adding an extra security layer.
- **Stateless by Design**: The server does not rely on HTTP sessions, making it scalable and robust.
- **CORS Configuration**: Configured to allow requests from `http://localhost:3000` for frontend integration.
- **Role-Based Access Control**: Users have roles (GUEST, USER) for fine-grained permission management.
- **Clear Exception Handling**: Differentiates between authentication (`401`) and authorization (`403`) failures for
  clear client-side error handling.

## Development

The project uses Gradle for dependency management and builds. All major components are designed with dependency
injection for easy testing and maintenance.

### Project Structure

```
src/main/java/dev/hyzoon/oauth_test/
├── api/
│   ├── controller/          # REST Controllers
│   │   ├── auth/           # Authentication endpoints (/api/v1/auth/*)
│   │   └── user/           # User profile endpoints (/api/v1/user/*)
│   ├── exception/          # Global exception handling
│   │   ├── GlobalExceptionHandler.java
│   │   └── InvalidRefreshTokenException.java
│   └── service/            # Business logic services
│       └── auth/           # Authentication business logic
├── config/
│   ├── properties/         # Configuration properties (JWT settings)
│   ├── redis/             # Redis configuration
│   └── security/          # Security configuration (CORS, JWT filter, OAuth2)
├── domain/
│   └── user/              # User entity and role enum
├── oauth/
│   ├── dto/               # OAuth2 DTOs (GitHub user info)
│   ├── factory/           # OAuth2 user info factory
│   ├── filter/            # JWT authentication filter
│   ├── handler/           # OAuth2 success/failure handlers, JWT error handlers
│   ├── service/           # OAuth2 services (Custom user service)
│   └── token/             # Token management (TokenDto, TokenProvider)
├── repository/
│   └── user/              # Data access layer (JPA repository)
└── util/                  # Utility classes (Cookie utilities)
```

### Key Components

- **JwtAuthenticationFilter**: Validates JWT tokens in request headers and sets authentication context
- **OAuth2AuthenticationSuccessHandler**: Handles successful OAuth2 login, creates temporary codes and stores tokens in
  Redis
- **TokenProvider**: Generates and validates JWT tokens (Access & Refresh tokens)
- **CustomOAuth2UserService**: Processes OAuth2 user information and creates/updates user records
- **AuthService**: Handles token exchange, refresh, and logout operations
- **SecurityConfig**: Configures Spring Security with OAuth2, JWT, and CORS settings

### Build & Run

- `docker-compose up -d` - Start MySQL and Redis containers
- `./gradlew build` - Compile and package the application
- `./gradlew bootRun` - Run the application in development mode
- `docker-compose down` - Stop the MySQL and Redis containers

### Troubleshooting

**Common Issues:**

1. **JWT Secret Too Short**: Ensure your JWT secret is at least 64 bytes (512 bits) long
2. **CORS Errors**: Frontend must run on `http://localhost:3000` or update CORS configuration in `SecurityConfig.java`
3. **GitHub OAuth Callback Mismatch**: Verify the callback URL in GitHub OAuth app matches
   `http://localhost:8080/login/oauth2/code/github`
4. **Redis Connection Failed**: Ensure Redis is running on port 6379 or update the configuration
5. **MySQL Connection Failed**: Check if MySQL is running and the database `oauth-test` exists

**Logs to Check:**

- JWT token generation and validation
- Redis operations (auth codes and refresh tokens)
- OAuth2 authentication flow
- Database operations

**Testing Tips:**

- Use browser developer tools to inspect HttpOnly cookies
- Check Redis CLI: `redis-cli` → `KEYS *` to see stored tokens
- Monitor application logs for authentication flow debuggingtion.

## Contributing

1. Adhere to the existing layered architecture.
2. Ensure separation of concerns between components.
3. Write clear and concise commit messages.
4. Thoroughly test authentication and authorization flows after making changes.

## License

This project is open source and available under the MIT License.
