# Client App (React) - Gateway API Coverage

Trang client này gọi **qua Gateway** để bao phủ toàn bộ API chính của hệ thống microservice.

## Cac trang/chuc nang da co

- `Login`: `POST /auth/login`
- `Register`: `POST /auth/register`
- `External Login`: `POST /auth/external/login` (google/facebook access token)
- `Introspect`: `POST /auth/introspect`
- `Gateway/JWKS`: 
  - `GET /api/health`
  - `GET /.well-known/jwks.json`
  - `GET /.well-known/openid-configuration`
- `Orders(USER)`:
  - `POST /orders`
  - `GET /orders`
  - `GET /orders/{id}`
- `Admin Orders` (chi hien khi token co role `ADMIN`):
  - `PUT /orders/{id}`
  - `DELETE /orders/{id}`

## Token va role logic

- Luu token vao `localStorage` (`gateway_auth`)
- Tu dong parse JWT payload de doc `roles`
- Hien tab `Admin Orders` neu co role `ADMIN`
- Logout xoa toan bo session local

## Cau hinh Gateway URL

Mac dinh app goi: `http://localhost:8080`

Neu muon doi host/port, tao file `.env` trong `client-app`:

```bash
VITE_GATEWAY_URL=http://localhost:8080
```

## Chay app

```bash
npm install
npm run dev
```

## Cac file chinh

- `src/App.tsx`: UI page + business flow
- `src/auth/AuthContext.tsx`: auth state, token persistence, role check
- `src/api/http.ts`: HTTP client + error handling
- `src/api/authApi.ts`: auth endpoints
- `src/api/ordersApi.ts`: order endpoints
- `src/api/systemApi.ts`: health endpoint
- `src/lib/jwt.ts`: parse token + role helpers
