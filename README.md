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

## Security rules (Lab 5)

Roles: `CLERK` (junior) and `MANAGER` (senior). The locked action is `approve` (DRAFT to APPROVED).

| Action | CLERK | MANAGER | No token |
| --- | --- | --- | --- |
| `POST /api/v1/auth/login` | anyone | anyone | allowed |
| Create and read a purchase request | yes | yes | 401 |
| `POST /api/v1/purchase-requests/{id}/approve` | 403 | yes | 401 |
| `GET /api/me` | yes | yes | 401 |
| `GET /actuator/health` | anyone | anyone | allowed |
| `GET /` and its HTML, CSS, and JavaScript assets | anyone | anyone | allowed |
| springdoc UI and `/v3/api-docs/**` | anyone | anyone | allowed |
| Anything else | 403 | 403 | 401 |

Dev users (profile `dev` only): `clerk` / `clerk-dev` and `manager` / `manager-dev`.

Login returns a 15-minute HS256 Bearer token from `POST /api/v1/auth/login`.
`GET /api/me` reports the authenticated username and authorities. The browser
page is served by Spring Boot at `http://localhost:8080/`; it keeps the token
in memory only. Use the dev credentials above for local review.

The approve endpoint is `POST /api/v1/purchase-requests/{id}/approve`. It moves
an existing request from `DRAFT` to `APPROVED` and records the authenticated
username in the application log. A `CLERK` receives 403 without changing the
row; a `MANAGER` can approve it.

An unknown route or unsupported API method returns `403 FORBIDDEN` for a
signed-in user and `401 UNAUTHENTICATED` without a token. Reading a missing
purchase request at a valid UUID still returns `404 NOT_FOUND`.

## Optional registration demo

Use this command in place of the regular Lab 5 launch command when you want
registration. It runs login, registration, and the purchase-request API in
one process on port 8080.

```sh
mvn spring-boot:run -Dspring-boot.run.profiles=dev,registration \
  '-Dspring-boot.run.arguments=--server.address=127.0.0.1 --server.port=8080'
```

Open the [login page](http://localhost:8080/) or the
[registration page](http://localhost:8080/register.html).

In this mode, `POST /api/v1/auth/register` accepts `username` and `password`.
It returns `201` with the username and role, `400` for invalid input, or
`409` for an unavailable username. Usernames are case insensitive. The
password must contain at least eight characters and fit in 72 UTF-8 bytes.
Each new account is a `CLERK` and can sign in through the existing page.

Registered accounts and their BCrypt password hashes are stored in PostgreSQL
in `registration.account`. You can sign in after a restart using the same
database and profiles. The registration schema has its own Flyway history. The
default `dev` profile follows the Lab 5 login-only workflow and does not create
or read accounts in this schema. Production also keeps registration disabled.
