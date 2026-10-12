# Task 2 – Employee Pagination, Sorting and Filtering

A small Spring Boot + PostgreSQL API that returns employees page by page. The client can sort the result and
filter by department, salary range and part of the name. All filtering, sorting and paging happens in the
database query – nothing is loaded into Java and filtered afterwards.

The endpoint is **public** (no login). Security and tenants belong to Task 3, not here.

## Tech stack

Java 17 · Spring Boot 4.1.1 (Web MVC, Data JPA, Validation) · Hibernate 7.4.5 · PostgreSQL 17 ·
JUnit 5, Mockito, AssertJ, MockMvc · Maven wrapper

## Project structure (`com.actify.employeepagination`)

| Package / class | Purpose |
|---|---|
| `EmployeePaginationApplication` | main class |
| `controller.EmployeeController` | `GET /api/employees`, reads query parameters, nothing else |
| `service.EmployeeService` | `searchEmployees` (salary range check, normalizing filters), `buildPageable` (page/size limits), `parseSort` (sort whitelist) |
| `repository.EmployeeRepository` | one `@Query` with a matching `countQuery`, takes a `Pageable` |
| `entity.Employee` | table `employees` (`id`, `name`, `department`, `salary`) |
| `dto` | `EmployeeSearchRequest`, `EmployeeResponse`, `PageResponse` |
| `exception` | `InvalidPageRequestException`, `InvalidSalaryRangeException`, `ApiError`, `GlobalExceptionHandler` |
| `service.DemoDataInitializer` | inserts 7 sample employees, **only** when `EMPLOYEE_SEED_DEMO_DATA=true` and the table is empty |

Request flow: `EmployeeController.getEmployees` → `EmployeeService.searchEmployees` → `buildPageable` +
`parseSort` → `EmployeeRepository.search` → PostgreSQL → `PageResponse`.

## Database

| | Database |
|---|---|
| Application | `Actify-Internship-Task-2-Employee-Pagination_db` |
| Integration tests | `financial_ledger_task2_test_db` (tables created and dropped by each run) |

```sql
CREATE DATABASE "Actify-Internship-Task-2-Employee-Pagination_db";
CREATE DATABASE financial_ledger_task2_test_db;
```

Hibernate creates the `employees` table (indexes on `department` and `salary`).

| Variable | Default |
|---|---|
| `EMPLOYEE_DB_URL` | `jdbc:postgresql://localhost:5432/Actify-Internship-Task-2-Employee-Pagination_db` |
| `EMPLOYEE_DB_USERNAME` | `postgres` |
| `EMPLOYEE_DB_PASSWORD` | empty → the PostgreSQL driver uses `%APPDATA%\postgresql\pgpass.conf` if it exists |
| `EMPLOYEE_SEED_DEMO_DATA` | `false` |
| `EMPLOYEE_TEST_DB_URL` / `_USERNAME` / `_PASSWORD` | test DB above, `postgres`, empty |

No password is stored in the project. Set variables only in the Eclipse run configuration or in one terminal window.

## How to run

Eclipse: *File → Import → Maven → Existing Maven Projects* → this folder (project `task-2-employee-pagination`),
then run `EmployeePaginationApplication` as *Spring Boot App* / *Java Application*.

Terminal:

```bat
set EMPLOYEE_SEED_DEMO_DATA=true
mvnw.cmd spring-boot:run
```

The app listens on **http://localhost:8081**.

## API

`GET /api/employees`

| Parameter | Default | Rule |
|---|---|---|
| `page` | `0` | 0 or more |
| `size` | `10` | 1 to 100 (`app.employee.max-page-size`) |
| `sort` | `id,asc` | `field` or `field,asc|desc`; field must be `id`, `name`, `department` or `salary` |
| `department` | – | exact match, case-insensitive (`it` finds `IT`) |
| `minSalary`, `maxSalary` | – | inclusive, 0 or more, `minSalary` must not be greater than `maxSalary` |
| `name` | – | partial match, case-insensitive (`AS` finds `Qasim`, `Abbas`, `Aakash`) |

Example: `GET /api/employees?department=IT&minSalary=40000&name=a&sort=salary,desc&page=0&size=1`

```json
{ "content": [ { "id": 4, "name": "Ali", "department": "IT", "salary": 90000.00 } ],
  "currentPage": 0, "pageSize": 1, "totalElements": 2, "totalPages": 2 }
```

Errors use one JSON shape: `{"timestamp","status","error","message","path"}`. Invalid page/size/sort/salary values
→ 400 with a clear message; unknown URL → 404; wrong method → 405.

## Important rules

- **Sort whitelist:** only `id`, `name`, `department`, `salary`. Anything else is rejected before it reaches the
  query. When sorting by something other than `id`, `id` is added as a second sort key so rows with the same value
  always come back in the same order (otherwise pages could repeat or skip rows).
- **Count query:** `EmployeeRepository.search` has a custom `countQuery` with exactly the same `WHERE` clause as the
  data query, so `totalElements` and `totalPages` always match the filters.
- **Null filters:** each condition is written as `(:param IS NULL OR …)`, so one query handles every filter
  combination. The service lower-cases the values in Java because `LOWER(:param)` on a null parameter fails on
  PostgreSQL.
- A page after the last one returns an empty `content` with the correct totals (not an error).

## Tests

```bat
mvnw.cmd clean test     :: unit tests only
mvnw.cmd clean verify   :: unit + integration tests (needs financial_ledger_task2_test_db)
```

| Class | Kind | Tests | What it proves |
|---|---|---|---|
| `EmployeeServiceTest` | unit (Mockito) | 8 | default sort, sort parsing + tie-breaker, sort whitelist, bad direction, page/size limits, min > max rejected before querying, filter normalization, blank filters ignored |
| `EmployeeIT` | integration (MockMvc + PostgreSQL) | 12 | response structure/defaults, requested page, empty page after the last one, size limits, sort asc/desc/tie-break, invalid sort, department (case-insensitive, exact), inclusive salary range, invalid salary values, partial case-insensitive name, combined filters with paging, JSON 404 |

Last run 2026-09-28: `mvnw.cmd clean verify` → 8 unit + 12 integration = 20 tests, 0 failures, BUILD SUCCESS –
**VERIFIED**.

## Manual verification (2026-09-28)

App started with `EMPLOYEE_SEED_DEMO_DATA=true` on port 8081 against `financial_ledger_task2_db` (7 demo rows).
That was the database's name before it was renamed to `Actify-Internship-Task-2-Employee-Pagination_db` on 2026-10-12.
20 requests checked with PowerShell, all as expected – **VERIFIED**: no token needed; `page=0&size=2`; last page;
page after the last; `sort=salary,desc` and `asc`; `department=IT` and `it`; `minSalary=40000`; `maxSalary=80000`;
`name=AS`; combined filters with `size=1` (`totalElements` 2 – the same number a direct SQL `COUNT` returned);
invalid sort field / direction, `size=101`, `size=0`, `page=-1`, min > max, `minSalary=abc` → 400; `POST` → 405.

## Requirement traceability

| Requirement | Class.method | Test | Manual |
|---|---|---|---|
| Pagination | `EmployeeService.buildPageable`, `EmployeeRepository.search(Pageable)` | `requestedPageAndSize`, `pageSizeLimits` | page/size requests |
| Sorting + whitelist | `EmployeeService.parseSort` | `sortAscendingAndDescending`, `invalidSortIsRejectedWithClearMessage`, unit sort tests | sort requests |
| Department filter | `EmployeeRepository.search` (`LOWER(e.department) = :department`) | `departmentFilterIsExactAndCaseInsensitive` | `department=IT/it` |
| Salary min/max | `EmployeeRepository.search`, range check in `searchEmployees` | `minAndMaxSalaryAreInclusive`, `invalidSalaryValuesAreRejected` | salary requests |
| Partial, case-insensitive name | `EmployeeService.searchEmployees` (`%name%`), `EmployeeRepository.search` | `partialNameSearchIsCaseInsensitive` | `name=AS` |
| Correct count | `countQuery` in `EmployeeRepository.search` | `pageAfterTheLastOneIsEmptyButCountStaysCorrect`, `combinedFiltersWithPagingAndSorting` | combined filter vs SQL count |

## Known limitations

- `%` and `_` typed into `name` are treated as SQL `LIKE` wildcards (not escaped).
- There is no API to create employees (not asked by the task); demo rows come from the optional seeder.
- Schema is managed by Hibernate `ddl-auto=update`, no migration tool.
