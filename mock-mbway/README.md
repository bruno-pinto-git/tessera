# mock-mbway

Android app that simulates the **SIBS Payment Gateway + MB WAY** end of the
payment flow, so the Tessera ticket-service can be developed against the real
protocol without a SIBS merchant contract. The app does two things at once:

1. **Embedded HTTP server** (Ktor on the device) exposing endpoints with the
   same shapes as the real SIBS Payment Gateway. The ticket-service talks to
   the device's IP+port as if it were `api.sibsgateway.com`.
2. **Compose UI** showing pending payments and offering **Aceitar** / **Recusar**
   buttons — the device plays the role of the customer's phone receiving the
   MB WAY push.

When the operator taps Aceitar/Recusar, the embedded server makes an HTTPS
callback to the merchant URL (the ticket-service `/webhooks/mbway/...`)
exactly like SIBS does in production.

## Why "fiel ao protocolo SIBS"

The intent is that swapping mock-mbway for the real SIBS gateway in production
should be **only a URL change** in the ticket-service config
(`MBWAY_GATEWAY_URL`). To honour that, the endpoint paths, request/response
shapes, and field names below mirror the real SIBS docs as published on
[docs.pay.sibs.com](https://www.docs.pay.sibs.com).

Things deliberately **not** replicated (mock-only simplifications):

- AES-GCM webhook payload encryption (SIBS encrypts the notification body
  with a shared key; mock sends plain JSON for ease of debugging)
- OAuth bearer-token issuance for the merchant client (we accept any `Bearer`
  value)
- The `transactionSignature` Digest scheme (we accept any value here too)
- `X-IBM-Client-Id` header validation (we read it for parity but don't check)
- TLS — the mock listens on plain HTTP on the LAN

## Protocol — endpoints exposed by the mock

Base path: `/api/v1`

### 1. Create checkout

`POST /api/v1/payments`

Headers:
- `Content-Type: application/json`
- `Authorization: Bearer <anything>`
- `X-IBM-Client-Id: <anything>`

Request body (subset we use):
```json
{
  "merchant": {
    "terminalId": 47215,
    "channel": "web",
    "merchantTransactionId": "ticket-1234"
  },
  "transaction": {
    "transactionTimestamp": "2026-05-24T12:00:00.000Z",
    "description": "Tessera · SU 1º Dezembro vs Praiense",
    "moto": false,
    "paymentType": "PURS",
    "paymentMethod": ["MBWAY"],
    "amount": { "value": 8.00, "currency": "EUR" }
  }
}
```

Response 200:
```json
{
  "returnStatus": {
    "statusCode": "000",
    "statusMsg": "SUCCESS",
    "statusDescription": "TRANSACTION CREATED SUCCESSFULLY"
  },
  "transactionID": "<server-side uuid>",
  "transactionSignature": "<server-side opaque token>",
  "amount": { "value": 8.00, "currency": "EUR" },
  "merchant": { "terminalId": 47215, "channel": "web",
                "merchantTransactionId": "ticket-1234" },
  "paymentMethodList": ["MBWAY"],
  "expiry": "<+5min>"
}
```

The mock additionally accepts a `callbackUrl` field inside `merchant` so the
ticket-service can pass its webhook URL per-payment (SIBS uses a Backoffice
config instead; for the mock, per-request is simpler).

### 2. Trigger MB WAY purchase

`POST /api/v1/payments/{transactionID}/mbway/purchase`

Headers:
- `Content-Type: application/json`
- `Authorization: <transactionSignature>` (any value, mock doesn't verify)

Request body:
```json
{ "customerPhone": "351#912345678" }
```

Response 200:
```json
{
  "returnStatus": { "statusCode": "000", "statusMsg": "Pending" },
  "paymentStatus": "Pending",
  "transactionID": "..."
}
```

Side effect: the payment appears in the Compose UI as a card with Aceitar /
Recusar buttons.

### 3. Status query (optional polling)

`GET /api/v1/payments/{transactionID}/status`

Response 200:
```json
{
  "returnStatus": { "statusCode": "000", "statusMsg": "Success" },
  "paymentStatus": "Pending" | "Success" | "Declined" | "Expired",
  "transactionID": "..."
}
```

### 4. Merchant webhook (server → merchant)

When the operator taps Aceitar / Recusar in the UI, the mock makes:

`POST <merchant.callbackUrl>`

Body (plain JSON in the mock; production SIBS sends AES-GCM-encrypted):
```json
{
  "returnStatus": { "statusCode": "000", "statusMsg": "Success" },
  "paymentStatus": "Success" | "Declined",
  "paymentMethod": "MBWAY",
  "transactionID": "...",
  "amount": { "currency": "EUR", "value": 8.00 },
  "merchant": { "terminalId": 47215 },
  "paymentType": "PURS",
  "notificationID": "<uuid>"
}
```

The ticket-service correlates the `transactionID` with its
`ticket.mbway_transaction_id` column (Flyway V3) and transitions the ticket
from PENDING to PAID (Success) or to CANCELLED (Declined).

## Networking

The mock listens on TCP `:8443` of the device (configurable). For the
ticket-service running in Docker to reach it, the device's LAN IP must be
reachable from the host. On the demo device the app shows that IP+port in
the header so the operator can paste it into the ticket-service env config:

```
MBWAY_GATEWAY_URL=http://192.168.X.Y:8443
```

The callback the ticket-service passes in `merchant.callbackUrl` must be
reachable from the device. For local dev:

```
http://<docker-host-ip>:8081/api/v1/webhooks/mbway/{transactionId}
```

## Running

```
./gradlew :app:installDebug
```

then launch the app on a device on the same Wi-Fi as the Docker host.
