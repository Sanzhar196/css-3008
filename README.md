# Purchase Requests

## Product
We build a purchase request tracker. People track a purchase request (`RequestId`) as it moves through statuses.

## Core item
`RequestId` is the unique identifier of one purchase request. It must not be null or blank.

## Status table
Statuses: `DRAFT`, `APPROVED`, `REJECTED`, `CANCELLED`.

| # | From | To | Result |
| --- | --- | --- | --- |
| 1 | DRAFT | APPROVED | allowed |
| 2 | APPROVED | CANCELLED | allowed |
| 3 | DRAFT | CANCELLED | forbidden |
| 4 | REJECTED | CANCELLED | forbidden |

## Forbidden — why
- **DRAFT → CANCELLED:** a draft was never approved, so there is nothing to cancel. It is rejected or edited instead.
- **REJECTED → CANCELLED:** a rejected request is already closed. Cancelling it again would break the audit history.