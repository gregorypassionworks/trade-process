# TradeProcessApplication

A Spring WebFlux service that processes incoming trades in parallel while ensuring trades with the same ID execute in strict sequence. It accepts an HTTP POST of trade data and streams back processed results with non-blocking backpressure control and per-ID fairness.

---

## Features

- Parallel processing with a global concurrency limit (default 5)
- Per-ID sequencing: trades sharing the same ID never overlap
- Fair dispatch: as soon as any slot frees, the next ready trade of any ID runs
- Handles infinite streams (e.g., Kafka) without blocking or unbounded buffering
- Clean completion when the request body completes and all trades finish

---

## Prerequisites

- Java 17 or later
- Maven 3.6+ or Gradle 7+
- Spring Boot 3.x
- Reactor Core and Spring WebFlux

---

## Getting Started

1. Clone the repository
2. Build with Maven or Gradle
3. Run the application
4. Send HTTP POST requests to /trades/process

## HTTP API

### Process list of trades

- **Endpoint:**  
  `POST /trades/process`
- **Content-Type:**  
  `application/json`
- **Request Body Example:**
  ```json
  [
  { "id": "T-0001", "symbol": "EUR/USD", "quantity": 100000, "price": 1.1010 },
  { "id": "T-0001", "symbol": "GBP/USD", "quantity": 150000, "price": 1.2750 },
  { "id": "T-0002", "symbol": "USD/JPY", "quantity": 200000, "price": 145.25 },
  { "id": "T-0004", "symbol": "AUD/USD", "quantity": 120000, "price": 0.6650 },
  { "id": "T-0005", "symbol": "USD/CAD", "quantity": 180000, "price": 1.3450 },

  { "id": "T-0003", "symbol": "GBP/USD", "quantity": 160000, "price": 1.2760 },
  { "id": "T-0006", "symbol": "NZD/USD", "quantity": 110000, "price": 0.5950 },
  { "id": "T-0002", "symbol": "USD/JPY", "quantity": 210000, "price": 145.30 },
  { "id": "T-0008", "symbol": "USD/CHF", "quantity": 130000, "price": 0.8900 },
  { "id": "T-0008", "symbol": "EUR/JPY", "quantity": 140000, "price": 159.80 }
]

### cURL Example
```text
curl -X POST http://localhost:8080/trades/process \
  -H "Content-Type: application/json" \
  -d '[
    { "id": "T-0001", "symbol": "EUR/USD", "quantity": 100000, "price": 1.1010 },
    { "id": "T-0001", "symbol": "GBP/USD", "quantity": 150000, "price": 1.2750 },
    { "id": "T-0002", "symbol": "USD/JPY", "quantity": 200000, "price": 145.25 },
    { "id": "T-0004", "symbol": "AUD/USD", "quantity": 120000, "price": 0.6650 },
    { "id": "T-0005", "symbol": "USD/CAD", "quantity": 180000, "price": 1.3450 },
    { "id": "T-0003", "symbol": "GBP/USD", "quantity": 160000, "price": 1.2760 },
    { "id": "T-0006", "symbol": "NZD/USD", "quantity": 110000, "price": 0.5950 },
    { "id": "T-0002", "symbol": "USD/JPY", "quantity": 210000, "price": 145.30 },
    { "id": "T-0008", "symbol": "USD/CHF", "quantity": 130000, "price": 0.8900 },
    { "id": "T-0008", "symbol": "EUR/JPY", "quantity": 140000, "price": 159.80 }
  ]'

Success Response

HTTP/1.1 200 OK
{"id":"T-0001","symbol":"EUR/USD","notional":110100.0,"status":"PROCESSED"}
{"id":"T-0001","symbol":"GBP/USD","notional":191250.0,"status":"PROCESSED"}
{"id":"T-0002","symbol":"USD/JPY","notional":29050000.0,"status":"PROCESSED"}
{"id":"T-0004","symbol":"AUD/USD","notional":79800.0,"status":"PROCESSED"}
{"id":"T-0005","symbol":"USD/CAD","notional":242100.0,"status":"PROCESSED"}
{"id":"T-0003","symbol":"GBP/USD","notional":204160.0,"status":"PROCESSED"}
{"id":"T-0006","symbol":"NZD/USD","notional":65450.0,"status":"PROCESSED"}
{"id":"T-0002","symbol":"USD/JPY","notional":30413000.0,"status":"PROCESSED"}
{"id":"T-0008","symbol":"USD/CHF","notional":115700.0,"status":"PROCESSED"}
{"id":"T-0008","symbol":"EUR/JPY","notional":22372.0,"status":"PROCESSED"}
