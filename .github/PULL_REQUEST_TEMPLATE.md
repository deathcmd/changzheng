## Summary

Describe the problem and the smallest change that solves it.

## Verification

- [ ] `./mvnw --batch-mode --no-transfer-progress clean verify`
- [ ] `npm --prefix changzheng-admin-web ci --ignore-scripts`
- [ ] `npm --prefix changzheng-admin-web run build`
- [ ] Mini program JavaScript checked when changed
- [ ] Compose configuration checked when deployment files changed

## Risk review

- [ ] Tests cover changed behavior
- [ ] No credentials, tokens, student data, database exports, or upload contents are included
- [ ] Authentication, file, network, migration, and dependency changes are called out explicitly
- [ ] User and deployment documentation is updated where needed
