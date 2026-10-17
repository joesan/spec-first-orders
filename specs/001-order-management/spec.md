# Feature 001: Order management

| | |
|---|---|
| **Status** | Approved |
| **Version** | 1.0.0 (matches `info.version` in the contract) |
| **Owner** | Orders team |
| **Contract** | [`api/openapi.yaml`](../../api/openapi.yaml) |
| **Rule tests** | `src/test/java/com/example/orders/domain/OrderRulesProperties.java` |

This file is the **intent** and **rules** layer of the spec. The API shape lives in
the OpenAPI contract; this file says what the API must *do*. Both are human-owned:
agents read them and never change them during implementation.

## 1. Intent

Customers place orders for one or more products. An order moves through a simple
lifecycle (created → paid → shipped, or cancelled). Large orders get a volume
discount. Other services read orders to drive payment and fulfilment.

## 2. User scenarios and acceptance criteria

### S1 — Place an order
**Given** a customer id and 1–50 order lines
**When** the client calls `POST /orders`
**Then** the order is stored in status `CREATED`, the response is `201` with the
order and a `Location` header, and the totals follow rules R2 and R3.

### S2 — Reject invalid orders
**Given** a request that violates R1 (no lines, more than 50 lines, quantity or price
out of range, unknown fields)
**When** the client calls `POST /orders`
**Then** the response is `400` with an `application/problem+json` body and nothing is stored.

### S3 — Pay, ship, cancel
**Given** an existing order
**When** the client calls `/pay`, `/ship` or `/cancel`
**Then** the transition happens if R4 allows it (`200` with the updated order);
otherwise the response is `409` and the order is unchanged.

### S4 — Read orders
`GET /orders/{orderId}` returns the order or `404`. `GET /orders` returns at most
`limit` orders (default 20, max 100), newest first, optionally filtered by status.

## 3. Business rules

Each rule has a stable id. The OpenAPI operations reference them in
`x-business-rules`, and each rule has at least one property test named after it.

| Id | Rule |
|---|---|
| **R1** | An order has 1–50 lines. Each line has quantity 1–1000 and unit price 1–10,000,000 cents. |
| **R2** | `subtotalCents` = Σ quantity × unitPriceCents. `totalCents` = subtotal − discount. The total is never negative. |
| **R3** | Volume discount: if the subtotal is at least 100,000 cents (€1,000), the discount is 10 % of the subtotal, rounded down to whole cents, capped at 50,000 cents (€500). Otherwise the discount is 0. The discount never exceeds the subtotal. |
| **R4** | Allowed transitions: `CREATED → PAID`, `PAID → SHIPPED`, `CREATED → CANCELLED`, `PAID → CANCELLED`. `SHIPPED` and `CANCELLED` are final. Any other transition is rejected and leaves the order unchanged. |
| **R5** | A cancelled order can never be shipped, whatever sequence of calls precedes it. |

## 4. Non-functional requirements

- **Errors:** all 4xx responses use RFC 9457 problem details.
- **Strictness:** request bodies reject unknown fields (R1, S2).
- **Money:** amounts are integer euro cents; no floating point anywhere.
- **Storage:** in-memory for this reference service (replace with a real repository behind `OrderRepository`).

## 5. Out of scope

Authentication, payment processing, inventory checks, editing order lines.

## 6. Open questions

None. New questions go here and must be resolved by the owner before the spec is
changed (run `/speckit.clarify`).

## 7. Changelog

Every change to this spec adds a row here, newest first. The change-request id is
the GitHub issue number (`CR-17` = issue #17). CI rejects a change to this file
without a new `CR-<number>` entry.

When a rule changes, keep its id and add the old value in the rule text, for
example: *"… capped at 75,000 cents (€750). Changed by CR-17; was 50,000."*
Removed rules keep their id, marked *"Removed by CR-n"*, so ids are never reused.

| Version | Change request | Type | Change |
|---|---|---|---|
| 1.0.0 | CR-1 | New feature | Initial specification: scenarios S1–S4, rules R1–R5. |
