# JWT Authentication Design

## Context

TV Looker currently has no backend authentication or authorization stack. The backend exposes REST endpoints under `/api/v1/**`, stores users locally, and represents user-owned data through UUID `userId` fields and JPA relations. Review, favorite-list, and interaction operations currently trust request or path `userId` values instead of an authenticated principal.

The frontend work should wait until the backend can issue and validate JWTs, protect user-owned endpoints, and expose predictable authorization behavior.

## Goals

- Add stateless backend-issued JWT authentication.
- Use Spring-native security and JWT support, not `jjwt`.
- Support anonymous guest browsing for public catalog reads.
- Support authenticated `USER` access for owned user data.
- Support `ADMIN` access for catalog management and global administrative views.
- Prevent users from reading or mutating other users' private data.
- Stop exposing passwords in API responses and store passwords with BCrypt.

## Non-Goals

- External identity providers such as Keycloak, Auth0, or OAuth login.
- Refresh tokens in the first pass.
- Treating `GUEST` as a persisted database role or JWT authority.
- Frontend authentication implementation in this backend-focused phase.

## Dependencies

Add Spring Security and Spring OAuth2 resource server support:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

If JWT signing support is not pulled transitively in this Spring Boot setup, add Spring Security JOSE explicitly:

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-oauth2-jose</artifactId>
</dependency>
```

Do not add `io.jsonwebtoken:jjwt-*` unless the project deliberately moves away from Spring's resource-server/Jose stack.

## Project Structure

Place new authentication classes in the repo's existing layered packages rather than creating a separate top-level security package:

- `config/SecurityConfig.java`
- `service/JwtService.java`
- `api/controller/AuthController.java`
- `service/AuthService.java`
- `api/dto/request/LoginRequest.java`
- `api/dto/request/RegisterRequest.java`
- `api/dto/response/AuthResponse.java`
- `domain/model/enums/UserAuthority.java`

Controllers should remain thin. They may receive the authenticated principal, but ownership decisions should be enforced in services and repositories.

## Authority Model

Guests are anonymous HTTP clients without JWTs. They may call public read-only catalog endpoints.

Persisted users receive one authority:

- `USER`: regular authenticated account.
- `ADMIN`: administrative account.

Add an enum-backed field to `UserEntity`, defaulting new registrations to `USER`. Admin assignment should not be possible through public registration.

JWTs should encode authorities with a claim such as:

```json
{
  "authorities": ["USER"]
}
```

Spring Security will map this claim into `GrantedAuthority` values through a configured `JwtAuthenticationConverter`.

## Auth Flow

Add public auth endpoints:

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`

Registration creates a local user with a BCrypt-hashed password and `USER` authority. Login verifies the password and returns a signed access token.

`AuthResponse` should include the token and enough non-sensitive user metadata for frontend bootstrapping, such as user ID, username/email, and authority. It must not include the password hash.

JWT claims should include:

- `sub`: user UUID.
- `username` or `preferred_username`: stable login/display identifier.
- `authorities`: `USER` or `ADMIN`.
- `iat`: issued-at timestamp.
- `exp`: expiration timestamp.

Start with a short access-token TTL, such as one hour. Store the signing secret in environment-backed configuration, not in source code.

## Endpoint Authorization

Public guest access covers safe catalog browsing only. These are `GET` operations for items/programs, genres, actors, directors, and item reviews intended for display on program pages.

`USER` access covers personal data and owned mutations. The backend must derive ownership from the JWT user ID instead of trusting request-supplied `userId` values.

`ADMIN` access covers catalog management and global views, including user inspection and fetching any review by ID.

Recommended review behavior:

- `GET /api/v1/reviews/{id}` returns a review only if the caller is `ADMIN` or owns the review.
- If a `USER` requests another user's review by ID, return `404` to avoid leaking that the review exists.
- Public program pages can still use item-scoped review listing if reviews are intended to be publicly visible.

Prefer current-user routes for owned resources:

- `GET /api/v1/reviews/me`
- `GET /api/v1/lists/me`
- `GET /api/v1/interactions/me`

Existing `/user/{userId}` routes can remain for `ADMIN` access, or be deprecated after frontend migration.

## Data Model And Repository Changes

Update `UserEntity` and related DTOs/mappers:

- Add `UserAuthority authority`.
- Store BCrypt password hashes.
- Remove password from `UserResponse`.
- Keep UUID user IDs.

Add login lookup methods such as:

- `UserRepository#findByUsername(...)`
- or `UserRepository#findByEmail(...)`

Add ownership-aware repository queries, starting with reviews:

- `ReviewRepository#findByIdAndUserId(...)`

The same ownership pattern should later be applied to favorite lists (`/api/v1/lists`) and interactions.

## Error Handling

Use normal Spring Security responses for authentication/authorization boundaries:

- Missing or invalid JWT: `401 Unauthorized`.
- Valid JWT without required authority: `403 Forbidden`.
- Valid `USER` accessing another user's private record by ID: `404 Not Found` for owner-scoped lookups where existence should not leak.

Validation errors for auth requests should follow the project's existing request validation behavior.

## Testing Strategy

Follow test-driven development for behavior changes. Each new behavior should have a failing test before production code.

Initial test targets:

- Register hashes the password and creates a `USER`.
- Login rejects bad credentials.
- Login returns a JWT with the expected subject and authority.
- User responses never include passwords.
- Guests can call public catalog `GET` endpoints.
- Missing tokens receive `401` on protected endpoints.
- `USER` can fetch, update, and delete their own review.
- `USER` receives `404` when fetching another user's review by ID.
- `ADMIN` can fetch any review by ID.
- Guest and `USER` cannot mutate catalog data; `ADMIN` can.

For Spring Security controller tests, prefer MockMvc security support with JWT authorities instead of hand-building JWT strings in every test. Service and repository tests should verify ownership behavior directly.

## Rollout Plan

1. Add Spring Security/JWT dependencies and security test support.
2. Add `UserAuthority`, password hashing, and password response cleanup.
3. Add auth request/response DTOs, auth service, and auth controller.
4. Add Spring Security config and JWT service using Spring-native JOSE support.
5. Secure review ownership first as the reference pattern.
6. Apply the same current-user ownership pattern to favorite lists and interactions.
7. Lock catalog mutations to `ADMIN` while keeping safe catalog reads public.

## Open Implementation Notes

- Admin creation needs a non-public path, such as a database seed, manual migration, or protected admin update endpoint.
- Refresh tokens are intentionally deferred. If frontend sessions feel too short, add refresh-token design later.
- Existing frontend calls using explicit `userId` routes will need migration after backend authorization is in place.
