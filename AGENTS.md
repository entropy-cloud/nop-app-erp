# AGENTS.md

## Project Intent

`nop-app-erp` is an enterprise resource planning (ERP) reference application built on the Nop Platform, using Attractor-Guided Engineering workflow for AI-assisted application development.

This repository is an application-layer product built on the Nop low-code platform (nop-entropy), not a framework-core project.

The repo is the source of truth. Chat is only a temporary working surface.

Before writing non-trivial code, agents must first understand:

- `docs/context/project-context.md`
- `docs/context/ai-autonomy-policy.md`
- `docs/context/codebase-map.md`
- the requirement, backlog, roadmap, plan, or user request that defines the current slice
- the relevant owner docs routed from `docs/index.md`
- the relevant raw inputs under `docs/input/` when requirement meaning depends on source material

Read `docs/context/source-of-truth-and-precedence.md` when facts conflict or you are unsure which artifact owns the answer.
Read `docs/process/application-development-workflow.md` when planning or workflow decisions are part of the task.

## Quick Routing

| If you need to... | Start with | Then check |
| --- | --- | --- |
| understand the product baseline | `docs/requirements/product-scope.md` | `docs/design/app-overview.md` |
| choose the next work item | `docs/backlog/README.md` | relevant requirement and owner docs |
| implement a feature | relevant requirement or backlog item | `docs/design/`, `docs/architecture/`, `<domain>/model/*.orm.xml`, plan guide if triggers apply |
| change persisted model or API contract | `<domain>/model/*.orm.xml`, `<domain>/model/*.api.xml` | Nop docs under `../nop-entropy/docs-for-ai/` |
| change a page or view | relevant design owner doc | AMIS `.view.xml` files under `app-erp-web` (after codegen setup) |
| review a planned or completed slice | relevant plan under `docs/plans/` | plan/closure audit prompts; ordinary audit evidence stays in the plan |
| run or verify the project | `docs/context/project-context.md` | `docs/context/codebase-map.md` |
| draft, execute, or audit a plan under `docs/plans/` | `docs/plans/00-plan-authoring-and-execution-guide.md` | `docs/logs/00-log-writing-guide.md` |

### Plan Authoring And Execution

When creating, revising, executing, or auditing a file under `docs/plans/`, you MUST read `docs/plans/00-plan-authoring-and-execution-guide.md` first. Plans are execution docs with explicit status, scope, exit criteria, and validation checklists. Tick off checklist items as you complete them. Re-audit the live repo before claiming completion.

## Task Routing

Before writing code, agents MUST classify the task first:

1. Determine the task type:
   - requirement clarification
   - app-layer design change
   - architecture change
   - implementation-only change
   - bug investigation
   - verification or audit work
2. Use `docs/index.md` to read the owner docs for that task type before acting.
3. Check `docs/skills/README.md` for candidate reusable skills before drafting or revising a plan.
4. For non-trivial work, record the chosen route and planned skill usage in the plan before implementation.

Do not jump from a feature request directly to code unless the route is already obvious from the relevant requirement and owner docs.

## Operating Rules

1. Prefer file-in, file-out collaboration.
2. Do not treat chat summaries as durable project memory.
3. Do not jump from raw PM text or prototype screenshots straight to code when scope is still unclear.
4. If input is ambiguous, first create or update a file in `docs/discussions/` or `docs/requirements/`.
5. Create or update a plan before implementation when the planning triggers below apply.
6. Keep `docs/design/` and `docs/architecture/` focused on the stable supported product baseline, not migration history, roadmap sequencing, or execution status.
7. Keep `docs/design/` focused on business semantics, workflows, and state meanings; keep persisted entities, field sets, and data dictionaries authoritative in `<domain>/model/*.orm.xml` rather than duplicating them in prose.
8. Keep logs short, dated, and append-only. After completing any significant code change, you MUST update the daily dev log at `docs/logs/{year}/{month}-{day}.md` (reverse chronological, see `docs/logs/00-log-writing-guide.md` for format). Changes to `nop-entropy` must be logged in `nop-entropy/ai-dev/logs/`, not in this project's `docs/logs/`.
9. Record non-obvious regressions in `docs/bugs/`.
10. If prototype and implementation diverge materially, capture the reason in `docs/retrospectives/` instead of silently moving on.
11. Promote repeated process lessons into `docs/skills/` or `docs/audits/` only when the pattern is recurring enough to justify reuse.
12. For high-risk or high-ambiguity requirement, design, or plan drafts, request an independent subagent or reviewer pass and revise until major objections are resolved. Every created plan MUST pass an independent plan audit before implementation begins and an independent closure audit before being marked complete.
13. Keep code comments minimal. Prefer self-explanatory code; add only rare comments when a local constraint is otherwise easy to misread.
14. When a referenced file is not found at its expected path, check `docs/archive/` before concluding it does not exist. Archived files retain their original relative name under `docs/archive/`. Do not move files to `docs/archive/` without human approval.
15. Treat reusable skills as method selectors, not substitutes for requirements, design, or architecture docs. Business knowledge belongs in owner docs first.

## Current Project Stage

`nop-app-erp` is in the **bootstrap / pre-codegen stage**. Only the AGE documentation structure and an empty ORM model skeleton exist. The Maven multi-module structure (api/codegen/dao/service/web/app/delta/meta) and Java sources will be generated by `nop-cli` from `<domain>/model/*.orm.xml` after the ORM model is designed.

Until the ORM model and generated modules exist:

- do not invent Java module paths, package names, or view paths as if they already exist
- design and discussion work should focus on `<domain>/model/*.orm.xml` and `docs/design/`, `docs/architecture/`, `docs/requirements/`
- the verification commands in `docs/context/project-context.md` are not yet executable; they will become valid after the first codegen pass

## Nop Platform Specific Rules

- Code generation is driven by XML models (`<domain>/model/*.orm.xml`, `<domain>/model/*.api.xml`). XLSX files can be generated from XML via `nop-cli convert` but are not the source of truth. Do not manually edit generated code; regenerate from models instead.
- ORM XML files (`*.orm.xml`) define entity mappings. The authoritative model source is `model/app-erp.orm.xml`; everything else is generated.
- Business logic lives in `*.xbiz.xml` and BizModel Java classes in `app-erp-service` (after codegen).
- SQL libraries are defined in `*.sql-lib.xml` files.
- Build requires `nop-entropy` parent to be built first.
- The project will use Maven multi-module structure. Module dependency order after codegen: codegen -> api -> dao -> service -> web -> app, with delta/meta as additional modules.

### Nop Platform Documentation (`nop-entropy/docs-for-ai/`)

The Nop Platform's authoritative development documentation lives at `../nop-entropy/docs-for-ai/` (sibling directory). This is the primary reference for all Nop platform conventions, APIs, and development patterns.

**When to read it:** Before implementing any feature that involves Nop platform APIs, code generation, BizModel patterns, page/view customization, delta customization, testing, or any non-trivial platform interaction.

**How to use it:**

1. Start with `docs-for-ai/INDEX.md` — contains a routing table mapping ~40 common tasks to the correct document.
2. Recommended lookup order: INDEX → `00-start-here/` → `03-runbooks/` → `02-core-guides/` → `01-repo-map/` → `04-reference/`
3. `00-start-here/ai-defaults.md` — core decision framework: Model → Delta → Java, anti-patterns table, self-check list.
4. `02-core-guides/` — canonical pattern documents covering model-first development, service layer (CrudBizModel), page customization, delta mechanism, auth, testing, etc.
5. `03-runbooks/` — task-oriented step-by-step guides for common operations (create entity, write BizModel method, build page, etc.).
6. `04-reference/common-java-helpers.md` and `04-reference/safe-api-reference.md` — quick reference for platform helper utilities and CrudBizModel safe APIs.

**Key rules from platform docs that apply to this project:**

- Decision order: Model → Delta → Java. Always prefer model/Delta/customization over writing new Java code.
- Never manually edit generated files (files under `_gen/`, with `_` prefix, or `_app.orm.xml`/`_service.beans.xml`).
- Use `CrudBizModel<T>` for standard entity services; use `@BizQuery`/`@BizMutation` annotations.
- Use platform helpers: `CoreMetrics.currentTimeMillis()` not `System.currentTimeMillis()`, `JsonTool` not third-party JSON libs, `StringHelper` not Apache Commons.
- `@Inject` fields cannot be `private` in Nop's IoC container.
- `@BizMutation` auto-wraps transactions; do not add `@Transactional` unless you need explicit propagation control.
- **Cross-entity access**: within BizModel, always inject `I*Biz` interfaces for other entities. Use `IDaoProvider` / `IOrmTemplate` / `@SqlLibMapper` only when `I*Biz` cannot satisfy the requirement, and document the reason in a code comment.
- **Exception handling**: all business exceptions MUST extend `NopException`. Use `ErrorCode` + `NopException` for public/GraphQL-facing errors (description in Chinese, i18n handles translation).

## Mandatory Skill Loading

Before doing **any** implementation work, agents **MUST** scan the available skills list and load every skill whose description matches the current task. This rule applies to **all** agents — main agent, subagent, and audit/review agent — without exception.

**Matching rule:** A skill matches when its `description` or trigger words cover the work about to be done (e.g. designing an ORM model, writing a BizModel method, creating a view.xml page, writing a test class). When multiple skills match, load all of them.

**Execution discipline:**

1. **Before writing code or designing models**, scan available skills and load every matching skill. The skill is the single entry point — it contains the required-reading routing table, code patterns, and anti-pattern self-check lists.
2. After the skill is loaded, read the docs it routes you to. The skill does not replace reading platform docs — it selects which docs to read for the current task.
3. After adding or modifying code or models, use the selfcheck mechanism provided by the loaded skill to verify no anti-patterns.
4. If a task spans multiple phases, re-scan and load matching skills when entering each new phase.
5. If no skill matches the current task, proceed without loading — but do not skip the scan step itself.

## Read This First

- `docs/context/project-context.md`
- `docs/context/ai-autonomy-policy.md`
- `docs/context/codebase-map.md`
- the requirement, backlog, roadmap, plan, or user request that defines the current slice
- the relevant owner docs routed from `docs/index.md`

Read additionally when needed:

- `docs/context/source-of-truth-and-precedence.md` for ownership or conflict questions
- `docs/context/conventions.md` for project-wide conventions
- `docs/process/application-development-workflow.md` for workflow questions
- `docs/index.md` when you need routing beyond the active files

## Documentation Ownership

- `docs/context/` owns mandatory AI context, source-of-truth precedence, and project-wide conventions.
- `docs/backlog/` owns roadmap, implementation ordering, and candidate work selection.
- `docs/input/` owns raw external inputs such as PM notes, card docs, article extracts, prototype references, and copied source material.
- `docs/discussions/` owns requirement clarification conversations and unresolved question records.
- `docs/requirements/` owns implementation-ready requirement synthesis.
- `docs/design/` owns stable app-layer business and feature design.
- `docs/architecture/` owns cross-cutting technical and module-boundary truth.
- `<domain>/model/*.orm.xml` and `<domain>/model/*.api.xml` own persisted model structure, data dictionaries, and generated contract truth.
- `docs/lessons/` owns durable reusable lessons extracted from bugs, audits, and retrospectives.
- `docs/plans/` owns execution and closure criteria for non-trivial work.
- `docs/audits/` owns audit methodology and specialized audit records.
- `docs/skills/` owns reusable prompts, review playbooks, and audit prompt templates.
- `docs/logs/` owns dated implementation memory.
- `docs/testing/` owns manual and exploratory testing records.
- `docs/bugs/` owns non-obvious bug histories and regression notes.
- `docs/analysis/` owns research, tradeoff analysis, and rejected directions.
- `docs/retrospectives/` owns post-implementation gap analysis and process improvements.

Do not hide mandatory rules in `docs/references/`; if an AI must apply it by default, put it in `docs/context/` or `AGENTS.md`.

## Default Workflow

1. Gather raw materials in `docs/input/`.
2. If needed, clarify ambiguity in `docs/discussions/`.
3. Synthesize implementation-ready requirements in `docs/requirements/`.
4. Split stable design output into app-layer design under `docs/design/` and technical design under `docs/architecture/`, with the two referencing each other when needed.
5. Route the task and select candidate reusable skills.
6. Write or update a plan when the planning triggers apply, and record skill usage per phase or item when relevant.
7. Audit the plan before implementation.
8. Implement the smallest complete slice.
9. Run verification.
10. Run closure audit for created plans.
11. Record logs and any needed bug notes.

## Optional Workflow Layers

Use these when warranted by task complexity. Plan and closure audits are mandatory for created plans.

- `docs/audits/` for specialized, complex, disputed, reusable, or future-replay-worthy audit evidence; ordinary plan/closure audit evidence belongs in the plan by default
- `docs/testing/` for manual or exploratory proof
- `docs/retrospectives/` for material requirement/prototype gaps
- `docs/skills/` for reusable prompts after repeated failures
- `docs/lessons/` for durable engineering lessons after repeated failures

Use `multi-dimensional-audit-prompt.md` when work must be challenged across several dimensions at once. Use `open-ended-audit-prompt.md` when the standard checklist may miss hidden risks. These prompts are generic defaults and MUST be customized after copy to match the project's real owner docs, protected areas, verification model, and recurring failure patterns.

## Planning Rule

Create a plan when the task has any of these traits:

- changes API, database/model, auth, integration, deployment, or public contract behavior
- changes user-visible behavior across more than one feature surface
- touches multiple modules and changes shared behavior
- is expected to take more than one AI session
- modifies more than 5 total files or is likely to exceed roughly 200 changed lines
- needs staged execution or explicit closure gates
- has unresolved product or technical risk that must not be hidden in chat

Skip a formal plan only for local low-risk edits such as copy changes, small styling fixes, test-only cleanups, and single-file behavior fixes with clear existing tests.

All created plans MUST pass independent subagent or reviewer audit before implementation begins and again before the plan is marked complete. Protected areas, unresolved product risk, and source-of-truth conflicts require human/subagent review or stay open.

## Skill Usage Rule

Before using a reusable skill, confirm all of the following:

- the task type and route are already clear from the requirement and owner docs
- the skill matches the work method, not just a similar business label
- required inputs listed in `docs/skills/README.md` are available
- the expected output is known and can be stored in the correct docs location

For non-trivial plans, each phase or item that depends on a reusable skill should record `Skill: <name>` or `Skill: none`.

## Docs Maintenance

After completing any significant code change, you MUST:

1. **Update the daily dev log** at `docs/logs/{year}/{month}-{day}.md` (reverse chronological, see `docs/logs/00-log-writing-guide.md` for format).
2. **Update relevant owner docs** in `docs/design/` or `docs/architecture/` when the change affects app-layer behavior or technical structure.

When verification passes completely (full green), record the verification status in the log entry and include it in the git commit message. This provides reliable known-good baselines for future debugging.

## Verification Baseline

Do not assume this template's example commands are valid for the copied project.

Use the real commands listed in `docs/context/project-context.md`.

If verification commands are blank or still placeholders, stop and fill them before reporting verification success.
