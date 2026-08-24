# Capital Bank - OIDC Banking Application

A comprehensive banking application that integrates with Fayda for secure identity verification and provides a complete banking dashboard experience.

## Features

- Secure authentication via Fayda integration
- Professional banking dashboard
- Account verification status tracking
- Loan eligibility checker
- Account balance overview
- Transaction history with filtering
- Exchange rates with currency converter
- User profile management with KYC fields
- Session-based authentication with logout functionality

## Prerequisites

- Java 17 or higher
- Maven 3.9 or higher
- Git

## Installation and Setup

### 1. Clone the Repository

```
git clone <repository-url>
cd fayda-bank
```

### 2. Environment Configuration

A `.env` file already exists in the project root. Open it and fill in the following variables:

```
CLIENT_ID=your_client_id
FAYDA_AUTHORIZATION_URI=your_authorization_endpoint
FAYDA_TOKEN_URI=your_token_endpoint
FAYDA_USER_INFO_URI=your_userinfo_endpoint
FAYDA_JWK_SET_URI=your_jwk_set_endpoint
PRIVATE_KEY=your_private_key
```

`application.yml` reads every one of these directly from `.env` at startup — there is no separate template file to copy, and `.env` is git-ignored so your credentials never get committed.

### 3. Install Dependencies

```
mvn install
```

## Running the Application

### Start the Development Server

```
mvn spring-boot:run
```

The application will be available at http://localhost:3000

### Port Configuration

- Default Port: 3000
- Callback URL: http://localhost:3000/callback
- Redirect URI: configured in `application.yml`, must match the redirect URI registered with Fayda

If you need to change the port, update `server.port` in `application.yml` and the redirect URI registered with Fayda accordingly.

## Usage

### 1. Access the Application

Open your web browser and navigate to http://localhost:3000

### 2. Authentication Flow

- Click "Sign in with Fayda" on the home page
- You will be redirected to the Fayda authentication system
- Complete the authentication process
- You will be redirected back to the application with your verified identity
- Access the banking dashboard with your authenticated session

### 3. Banking Features

Once authenticated, you can access:

- **Dashboard**: Overview of all banking features
- **Profile Management**: View personal information and KYC details
- **Account Verification**: Check verification status
- **Loan Eligibility**: Check loan options and eligibility
- **Account Balance**: View account balances
- **Transactions**: Review transaction history
- **Exchange Rates**: View currency exchange rates

### 4. Logout

Click the "Logout" button in the header to end your session and return to the home page.

## Project Structure

```
fayda-bank/
├── src/main/java/et/fayda/bank/
│   ├── config/
│   │   ├── OAuthClientConfig.java
│   │   ├── SecurityConfig.java
│   │   └── FaydaUserService.java
│   ├── controller/
│   │   └── PageController.java
│   └── FaydaBankApplication.java
├── src/main/resources/
│   ├── templates/
│   │   ├── home.html
│   │   ├── callback.html
│   │   ├── dashboard.html
│   │   ├── profile.html
│   │   ├── account_verification.html
│   │   ├── loan_eligibility.html
│   │   ├── account_balance.html
│   │   ├── transactions.html
│   │   └── exchange_rates.html
│   ├── static/
│   └── application.yml
├── .env
└── pom.xml
```

## Technical Details

- **Framework**: Spring Boot 3.3
- **Authentication**: OIDC with Fayda (`private_key_jwt` client assertion via Nimbus JOSE+JWT)
- **Session Management**: Spring Security sessions
- **Frontend**: Thymeleaf, HTML5, CSS3
- **Build Tool**: Maven

## Troubleshooting

### Common Issues

**Port Already in Use:**
- Change the port in `application.yml` (`server.port`)
- Update the redirect URI registered with Fayda accordingly

**Environment Variables:**
- Ensure all required variables are set in the `.env` file
- Check that the redirect URI matches your server configuration

**Authentication Issues:**
- Verify Fayda endpoints are accessible
- Check client credentials and private key format (must be a single-line base64-encoded RSA JWK)

## Security Notes

- This application uses session-based authentication
- User data is stored in the Spring Security session
- All OIDC communications are secured with JWT tokens
- Private keys should be kept secure and not committed to version control (`.env` is git-ignored)

## Support

For technical support or questions about the application, please contact the development team.