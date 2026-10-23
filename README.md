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

## Architecture

```
  dto          client        handler         config
  (JSON later) (HTTP later)  (HTTP week 9)   Application
                                             RequestService (@Service)
       \            \            /                |
        \            \          /           injects Rule
         \            \        /
                     domain
         RequestId  RequestStatus  RequestPolicy
         Rule + TransitionRule + OnlyApprovedCanCancelRule
                     (no Spring, no JPA)
```

Arrows point inward. `domain` never imports `org.springframework`, `java.sql` or `jakarta.persistence`.

## Persistence

PostgreSQL. The table is created by Flyway from
`src/main/resources/db/migration/V1__create_purchase_request.sql`:
surrogate `id uuid`, `UNIQUE business_key`, `CHECK` on status.
Hibernate only validates the schema (`ddl-auto: validate`); it never creates it.

`RequestEntity` and `RequestJpa` live in `persistence`. The service is `@Transactional`.
A duplicate business key becomes the domain error `DuplicateRequest` (unchecked, in `domain`).

| Test | What happens | Rows left |
| --- | --- | --- |
| `secondStatementRollsBack` | one transaction, two inserts, second fails | 0 |
| `secondRequestKeepsTheFirst` | two `register` calls, second fails | 1 |

## Run and test

    docker compose up -d
    mvn -q verify
    mvn spring-boot:run
