# Frontend (User)

Vite + React + TypeScript. Calls existing backend APIs only.

## Dev

- cd frontend
- npm install
- VITE_API_BASE_URL=http://localhost:8080 npm run dev

## Routes

- /login
- /register
- /products
- /orders
- /orders/new
- /orders/:id
- /orders/:id/pay

## Notes

- Token stored in localStorage, injected as Authorization: Bearer <token>.
- Requests via src/services/api.ts.




