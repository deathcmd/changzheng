# Contributing

Thank you for improving Changzheng. Keep pull requests focused and explain the user-facing or operational reason for each change.

## Development checks

Use JDK 17, Maven 3.8+, and Node.js 20.19+ (or 22.12+).

    mvn --batch-mode verify
    npm --prefix changzheng-admin-web ci
    npm --prefix changzheng-admin-web run build
    docker compose --env-file .env.example config --quiet

Never commit .env, credentials, JWTs, WeChat codes or session keys, database exports, student records, upload contents, node_modules, or generated dist files.

## Pull requests

- Add or update tests when behavior changes.
- Document new environment variables in .env.example.
- Treat gateway and downstream JWT checks as defense in depth; do not trust client-supplied X-User-Id, X-Admin-Id, or X-User-Type headers.
- Validate file paths, content signatures, request sizes, and outbound destinations at trust boundaries.
- Avoid logging secrets or personal data.
- Keep dependency updates separately reviewable when practical.

Report vulnerabilities according to SECURITY.md, not in a public pull request.
