# Expense Tracker Backend (AI Gateway)

This Spring Boot service acts as a thin backend for the Expense Tracker PTIT mobile app.
It is responsible for:

- Exposing AI-related APIs (chat, transaction parsing, insights).
- Verifying Firebase ID tokens (to be implemented next).
- Acting as a secure gateway to external LLM providers (e.g. Gemini).

## High-level API Contract

- `POST /api/ai/chat`
  - Request: `ChatRequest { message: string, locale?: string, context?: string }`
  - Response: `ChatResponse { reply: string, suggestions?: string[] }`

- `POST /api/ai/parse-transaction`
  - Request: `ParseTransactionRequest { text: string, locale?: string }`
  - Response: `ParsedTransactionDto { amount?: number, currencyCode?: string, categoryName?: string, description?: string, date?: LocalDate, walletName?: string }`

- `POST /api/ai/insights`
  - Request: `InsightsRequest { totalIncome: number, totalExpense: number, totalDebt: number, recentSpendingPattern?: string, timeRange?: string }`
  - Response: `InsightsResponse { alerts: string[], tips: string[] }`

Authentication
--------------

- All endpoints require **Firebase ID token**:
  - Header: `Authorization: Bearer <id_token>`
  - Token is verified server-side via Firebase Admin SDK.
- Test endpoint: `GET /api/me` returns `{ "userId": "<firebase_uid>" }` when token is valid.

Gemini setup (required for /api/ai/chat)
----------------------------------------
- Set Gemini API key: environment variable `GEMINI_API_KEY` or in `application.yml` under `gemini.api-key`.
- Default model: `gemini-1.5-flash-latest`.
- Endpoint used: `https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`.

## Run locally (recommended)

### Option A: Docker Compose (app + postgres)
From `backend/`:
- Create a local `.env` file (this repo ignores it) or export env vars in your shell.
- Minimum for AI endpoints: `GEMINI_API_KEY=<your_key>`

Run:
- `docker compose up --build`

The API will be available at:
- `http://localhost:8081`

#### Firebase Admin credentials (optional for local)
This backend initializes Firebase Admin using **Application Default Credentials**.

- If you run the app on host (Maven), set:
  - `GOOGLE_APPLICATION_CREDENTIALS=D:/keys/your-firebase-adminsdk.json`
- If you run via Docker Compose, use the addon file (mounts the JSON into the container):
  - PowerShell:
    - `$env:GOOGLE_APPLICATION_CREDENTIALS="D:/keys/your-firebase-adminsdk.json"`
    - `docker compose -f docker-compose.yml -f docker-compose.firebase.yml up --build`

### Option B: Postgres in Docker, app via Maven (best for dev)
From `backend/`:
- `docker compose up -d postgres`

Then run the Spring app with profile `dev`:
- PowerShell example:
  - `$env:SPRING_PROFILES_ACTIVE="dev"`
  - `$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/expensetracker"`
  - `$env:DB_USERNAME="postgres"`
  - `$env:DB_PASSWORD="password"`
  - `$env:GOOGLE_APPLICATION_CREDENTIALS="D:/keys/your-firebase-adminsdk.json"`
  - `$env:GEMINI_API_KEY="<your_key>"`
  - `mvn spring-boot:run`


