# mock-mbway

Android app that simulates the **SIBS Payment Gateway + MB WAY** end of the
payment flow, so the Tessera ticket-service can be developed against the real
protocol without a SIBS merchant contract. The app does two things at once:

1. **Polls the ticket-service** for pending payments (`POST
   /api/v1/mbway/relay/poll`, every ~3s) instead of hosting a server the
   backend calls into — the phone may be on a network the backend can't
   reach (its own hotspot, mobile data), but the reverse direction (phone →
   backend) always works, since the backend has a stable, reachable address.
2. **Compose UI** showing pending payments and offering **Aceitar** / **Recusar**
   buttons — the device plays the role of the customer's phone receiving the
   MB WAY push.

When the operator taps Aceitar/Recusar, the app makes an HTTP callback to the
merchant URL supplied with the payment (the ticket-service
`/api/v1/webhooks/mbway`) exactly like SIBS does in production.

## Why "fiel ao protocolo SIBS" (with one deliberate exception)

The webhook leg (phone → backend confirmation) keeps full protocol fidelity —
same shapes, same idea as the real SIBS callback. The **poll leg is a
deliberate, scoped exception**: the real SIBS gateway is push-based and will
never poll us, so swapping mock-mbway for the real thing in production is a
config change to `MbwayGatewayClient` on the backend (which would go back to
calling out to SIBS's API), not a drop-in replacement on the mock's side —
that trade-off is intentional, made so mock-mbway keeps working regardless of
which network the phone and the backend happen to be on.

Things deliberately **not** replicated (mock-only simplifications):

- AES-GCM webhook payload encryption (SIBS encrypts the notification body
  with a shared key; mock sends plain JSON for ease of debugging)
- The `transactionSignature` Digest scheme (kept in the wire shape for
  parity, but never validated)
- TLS between the mock and the backend on a LAN (plain HTTP); TLS is used
  automatically once the backend is a deployed HTTPS origin

## Protocol

### 1. Poll for pending payments (mock → ticket-service)

`POST {backend}/api/v1/mbway/relay/poll`

Headers:
- `X-Relay-Secret: <shared secret>` — must match `MBWAY_RELAY_SECRET` on the
  backend; the endpoint is public once the backend is deployed, unlike the
  old LAN-only embedded server.

Response 200 (possibly empty array — this is a normal "nothing pending" tick,
not an error):
```json
[
  {
    "transactionID": "...",
    "transactionSignature": "...",
    "merchantTransactionId": "ticket-1234",
    "terminalId": 47215,
    "description": "Tessera · SU 1º Dezembro vs Praiense",
    "amount": { "value": 8.00, "currency": "EUR" },
    "customerPhone": "351#912345678",
    "callbackUrl": "https://<backend>/api/v1/webhooks/mbway"
  }
]
```

Each item is removed from the backend's queue the moment it's handed back in
a poll response (no delivery confirmation / redelivery — if the response is
lost, the payment is lost too, same best-effort spirit as the rest of this
project; the fan just retries `pay()`, which mints a fresh one). Polled every
~3s while the app is running (see `RelayPoller`).

Side effect: each item appears in the Compose UI as a card with Aceitar /
Recusar buttons.

### 2. Merchant webhook (mock → ticket-service)

When the operator taps Aceitar / Recusar in the UI, the mock makes:

`POST <callbackUrl>` (from the polled item — always
`{backend}/api/v1/webhooks/mbway`)

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
`ticket.mbway_transaction_id` column and transitions the ticket from PENDING
to PAID (Success) or leaves it PENDING (Declined/Expired).

## Networking

The mock never listens for inbound connections — it only ever dials out to
the backend, so it works on any network (its own hotspot, mobile data,
whatever), including one entirely unrelated to the backend's. Open
**Definições** in the app and set:

- **Endereço do backend** — just the host, no scheme/port
  (e.g. `192.168.1.10` for local dev, or the deployed FQDN). The app tries
  `http://<host>:8081` first, then `https://<host>` — whichever answers is
  remembered.
- **Segredo partilhado** — must match `MBWAY_RELAY_SECRET` in the backend's
  `.env` (defaults to `dev-secret` on both sides for local dev).

## Running

```
./gradlew :app:installDebug
```

then launch the app on any device with internet/LAN access to the backend —
same Wi-Fi as the Docker host for local dev, or just mobile data if the
backend is deployed.
