# Security Policy

## Supported version

Security fixes are applied to the latest commit on the main branch. Older commits and privately modified deployments are not maintained as separate release lines.

## Reporting a vulnerability

Please use GitHub private vulnerability reporting for this repository when it is available. Do not publish exploitable details, credentials, personal data, or production URLs in a public issue.

If private reporting is unavailable, open a public issue containing only a short request for a private contact channel. A maintainer will respond there without asking for sensitive evidence in public.

Useful reports include the affected commit, component, impact, a minimal reproduction, and a suggested mitigation. Please redact JWTs, WeChat session data, database records, API keys, and student information.

## Security-sensitive areas

Changes to authentication, gateway routing, identity headers, file upload/delete handling, encryption, deployment scripts, Docker images, dependencies, or third-party API calls require focused review and regression tests.
