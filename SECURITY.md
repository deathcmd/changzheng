# Security Policy

## Supported version

Security fixes are applied to the latest commit on the main branch. Older commits and privately modified deployments are not maintained as separate release lines.

## Reporting a vulnerability

Please use GitHub private vulnerability reporting for this repository when it is available. Do not publish exploitable details, credentials, personal data, or production URLs in a public issue.

If private reporting is unavailable, open a public issue containing only a short request for a private contact channel. A maintainer will respond there without asking for sensitive evidence in public.

Useful reports include the affected commit, component, impact, a minimal reproduction, and a suggested mitigation. Please redact JWTs, WeChat session data, database records, API keys, and student information.

## Security-sensitive areas

Changes to authentication, gateway routing, identity headers, file upload/delete handling, encryption, deployment scripts, Docker images, dependencies, or third-party API calls require focused review and regression tests.

## Current security baseline

- The gateway and each Servlet service validate JWTs independently.
- Protected Servlet requests re-check account status so disabling a student or administrator revokes existing access tokens.
- Student, administrator, and refresh tokens have separate role and usage checks.
- Client-supplied identity headers are removed and rebuilt from verified token claims.
- Administrator login failures are atomically rate-limited with a fail-closed Redis dependency.
- Student binding and step synchronization serialize conflicting writes; content access requires an unlocked node and active user.
- Uploads validate the allowed extension and file signature; deletion is constrained to the configured upload root.
- Compose requires explicit secrets, keeps internal services off host ports, and runs Java containers as a non-root user.
- GitHub Actions validates Maven modules, the locked admin frontend plus dependency audit, mini program JavaScript, and Compose configuration. CodeQL analyzes Java and JavaScript, and Gitleaks scans Git history for credentials. Dependabot monitors Maven, npm, GitHub Actions, Compose images, and Java runtime images.

These controls reduce known risks but are not a guarantee that a deployment is secure. Operators remain responsible for HTTPS termination, secret rotation, backups, host patching, access logs, and reviewing local configuration changes.

The current encrypted student identifiers, names, and WeChat session keys retain the project's legacy deterministic AES format. A versioned AES-GCM migration with backwards-compatible reads is tracked in [ROADMAP.md](ROADMAP.md); changing the key or format without that migration can make existing data unreadable.
