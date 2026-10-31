# REST API Architecture Rules

You are building a Groovy REST API. Follow these architecture-specific rules in addition to the common Groovy rules.

## API Design
- Use **consistent URL patterns**: plural nouns for resources, nested routes for relationships. Avoid verbs in URLs.
- Return **proper HTTP status codes**: 200 OK, 201 Created, 400 Bad Request, 401 Unauthorised, 404 Not Found, 500 Internal Server Error.
- **Version your API from day one**. Use URL prefix (/v1/) or Accept header versioning.
- Validate all **request bodies** and **query parameters** at the boundary. Return structured error responses with error codes.

## Endpoint Features
- Implement **pagination** for all list endpoints: cursor-based (preferred) or offset-based. Include total count and next/prev links.
- Use **consistent error response format**: `{ "error": { "code": "VALIDATION_ERROR", "message": "...", "details": [...] } }`.
- Add **rate limiting** with clear headers: X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset.
- Support **filtering, sorting, and field selection** on list endpoints. Use query params: `?sort=-created_at&fields=id,name`.
- Use **ETags** or **Last-Modified** for caching. Return 304 Not Modified when the resource has not changed.

## Operational
- Implement **health check endpoints**: /health for basic liveness, /ready for dependency checks (DB, cache, external APIs).
- Use **API documentation** (OpenAPI/Swagger) as the source of truth. Generate client SDKs and server stubs from the spec.
- Implement **graceful degradation**: when a non-critical dependency fails, return partial results with a warning header.
- Support **bulk operations** for endpoints that users frequently call in loops. Batch create/update/delete saves round trips.
- Use **webhooks** for push notifications instead of polling. Include HMAC signatures for webhook payload verification.
- Implement **request/response compression** (gzip, brotli) for payloads over 1KB. Respect Accept-Encoding headers.
- Add **audit logging** for all write operations. Track who changed what, when, and from which IP/client.
