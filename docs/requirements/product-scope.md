# Product Scope

## Current Milestone (bootstrap)

- Product summary: initialize `nop-app-erp` as an ERP application skeleton on the Nop Platform. Stand up the AGE documentation structure and an empty ORM model, ready for domain design.
- Users: ERP operators/admins (eventual), developers (immediate)
- MVP scope:
  - AGE documentation structure applied and customized for `nop-app-erp`
  - `model/app-erp.orm.xml` skeleton with correct maven coordinates and empty dicts/domains/entities
  - ready to receive the first ERP business domain design
- Deferred scope:
  - choosing specific ERP business domains (procurement / sales / inventory / finance / etc.) — human decision
  - ORM entity design — next milestone
  - multi-module project generation via `nop-cli` — after ORM design
  - app build/run verification — after codegen
- Success metrics:
  - documentation structure is internally consistent (placeholders resolved or explicitly marked)
  - `model/app-erp.orm.xml` validates as a well-formed orm skeleton
  - `docs/context/project-context.md` reflects the real bootstrap state
- Constraints:
  - no Java modules exist yet; do not invent their paths as live
  - `nop-entropy` parent POM must be built before any future compilation

## Rule

This file owns current milestone scope.

Do not duplicate stable app surfaces and workflows here. Put current supported behavior in `docs/design/app-overview.md`.

Put implementation sequencing into plans, not here.
