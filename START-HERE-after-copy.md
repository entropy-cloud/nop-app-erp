# Start Here After Applying AGE Template

Use this checklist to verify that the Attractor-Guided Engineering template has been properly applied to `nop-app-erp`.

## Required Before First AI Coding

- [x] Replace `<project-name>` and other placeholders with real `nop-app-erp` data.
- [x] Fill `docs/context/project-context.md` with real project identity, low-churn context, and verification commands.
- [x] Fill `docs/context/ai-autonomy-policy.md` with real protected areas and AI autonomy defaults.
- [x] Fill `docs/context/codebase-map.md` with real entry points, common change routes, and fragile files.
- [x] Fill `docs/backlog/README.md` with the first ready or blocked work items.
- [x] Choose the first requirement or roadmap slice and ensure it has concrete acceptance criteria.
- [x] Identify the relevant owner docs through `docs/index.md`, the requirement, or the backlog/roadmap item.
- [ ] Ensure verification commands are real commands for this repository. **(deferred — commands become valid only after the first codegen pass; see Current Project Stage in AGENTS.md)**

For a direct local low-risk edit, do not block on a polished backlog or codebase map if the requirement/owner-doc meaning is obvious and verification commands are real. Protected areas, stale docs, missing verification, or unclear user-visible behavior still block coding.

## Bootstrap-Stage Work Items

Before any implementation code can be generated, these must be completed (in roughly this order):

- [ ] Design the initial ORM model in `model/app-erp.orm.xml` (entities, dictionaries, domains).
- [ ] Run `nop-cli` to generate the Maven multi-module structure and Java sources.
- [ ] Fill in real verification commands in `docs/context/project-context.md`.
- [ ] Wire up `nop-entropy` parent POM and confirm the project builds.
- [ ] Add the first known-good verification row to `docs/testing/known-good-baselines.md` when real commands pass.

## Fill Progressively

Fill these as soon as they are needed. Do not block the first small feature just to write polished baseline docs.

- [ ] Fill `docs/architecture/project-vision.md` with long-term product direction and non-goals.
- [ ] Fill `docs/architecture/system-baseline.md` with the real stack and model/database source.
- [ ] Fill `docs/design/app-overview.md` with stable app surfaces, roles, and core workflows.
- [ ] Fill `docs/requirements/product-scope.md` with the current milestone scope.
- [ ] Remove or ignore optional directories you will not maintain yet.

## Minimum Before Coding

- [ ] The relevant requirement or roadmap slice has concrete acceptance criteria.
- [ ] The relevant owner docs are identified.
- [ ] AI autonomy is `implement`, or the work is explicitly `plan-first` and only a plan will be written.
- [ ] Protected-area placeholders are replaced with real entries or explicit `none`.
- [ ] The relevant requirement, owner docs, and touched code route are not stale or disputed, unless the task is research or baseline alignment only.
- [ ] Verification commands are real commands for this repository.
- [ ] Any conflict between raw input, requirements, owner docs, and live code is resolved or explicitly blocked.

## Do Not Start If

- `docs/context/project-context.md` is still blank.
- verification commands are placeholders.
- protected-area placeholders remain and the task touches auth, permissions, data deletion, model/schema, deployment, or external integrations.
- AI autonomy is `ask-first`, `research-only`, or `blocked` and no human decision has changed it.
- the relevant requirement or owner doc is stale/unknown/disputed and the task would change product behavior instead of auditing or aligning the baseline.
- the relevant requirement is unclear enough that implementation would require guessing user-visible behavior.
