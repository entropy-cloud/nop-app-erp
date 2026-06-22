# Project Vision

## Purpose

Describe the long-term product and engineering attractor for `nop-app-erp`.

## Fill In

- Product goal: a reference enterprise resource planning (ERP) application on the Nop Platform, demonstrating model-first development for typical ERP business domains (e.g. master data, documents, postings). Specific business domain scope is decided during the ORM model design phase.
- Primary users: ERP system operators/admins, and developers learning Nop Platform from a realistic business-domain app
- Constraints that must stay true:
  - `model/app-erp.orm.xml` remains the single source of truth for persisted model
  - never hand-edit generated code
  - business exceptions extend `NopException` with `ErrorCode`
  - build requires `nop-entropy` parent POM in the local Maven repository
- Explicit non-goals:
  - not a framework-core project
  - not a generic admin template
  - does not replicate nop-app-mall; it is a separate ERP domain
- Success criteria for the first production milestone: ORM model designed, multi-module project generated, app builds and runs, first ERP business loop is testable end-to-end
- Required human decision points that AI should not silently invent:
  - which ERP business domains are in scope (procurement / sales / inventory / finance / etc.)
  - data-deletion and accounting-posting semantics
  - auth/permission model beyond nop-auth defaults
  - external integrations

## Notes

- Keep this document stable and high level.
- Do not turn it into a backlog.
- Do not duplicate current milestone scope from `docs/requirements/product-scope.md`.
- Do not duplicate current app surfaces from `docs/design/app-overview.md`.
- Move implementation sequencing into `docs/plans/` or `docs/requirements/`.
