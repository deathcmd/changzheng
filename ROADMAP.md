# Roadmap

This roadmap records maintainable, verifiable work that is not implemented yet. It is not a release promise.

## Near term

- Add MySQL-backed integration tests for migrations, concurrent step synchronization, and concurrent student binding.
- Replace deterministic legacy AES records with versioned authenticated encryption (AES-GCM), including backwards-compatible reads, schema sizing, and a tested data migration.
- Parse large student spreadsheets with a bounded streaming reader so the 5,000-row limit is enforced before loading a complete worksheet into memory.
- Replace the current empty banner response with an administrator-managed banner model and API.
- Add server-side achievement evaluation and persistence; the mini program currently derives achievement cards from progress locally.
- Add media lifecycle tracking so content deactivation can identify and safely retire unreferenced uploads.
- Add a controlled avatar upload and moderation path; selected WeChat avatars currently remain local to one device.

## Longer term

- Add class-level aggregate rankings with privacy-preserving minimum group sizes.
- Add operator audit records for node, content, student import, unbind, and manual data correction actions.
- Publish versioned releases with database upgrade notes and a tested backup/restore procedure.
