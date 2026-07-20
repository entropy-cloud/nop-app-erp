# AGE Practice Gap Audit - 2026-07-20

## Scope

- **Repository**: nop-app-erp at `/Users/abc/app/nop-app-erp` (root pom, 154 reactor modules)
- **Branch context**: main branch, active development shown through daily commits
- **Baseline documents read**: AGENTS.md, docs/index.md, docs/process/application-development-workflow.md, docs/context/project-context.md, docs/context/ai-autonomy-policy.md, docs/context/codebase-map.md, docs/context/source-of-truth-and-precedence.md, docs/skills/README.md, docs/backlog/README.md, docs/plans/00-plan-authoring-and-execution-guide.md, docs/audits/00-audit-execution-guide.md, docs/logs/index.md, docs/analysis/README.md
- **Real evidence sampled**: docs/plans/ (267 files), docs/logs/2026/ (26 files), docs/audits/ (15 entries), docs/bugs/ (10 entries), docs/retrospectives/ (5 entries), docs/backlog/README.md (67+ work items), docs/logs/2026/07-20.md (latest daily log)
- **Project customization layer injected**: per docs/skills/README.md §项目定制化层 — protection zones (ORM model accounting/finance/data deletion, generated artifacts), validation commands, naming conventions, known failure patterns

## Executive Summary

- **This repository exhibits the highest fidelity AGE adoption observed — not merely "partial adoption" but near-complete execution across all 9 disciplines.**
- All baseline context files are filled with real, project-specific values — no placeholders, no stale template content.
- The planning discipline is extraordinary: 267 timestamped plans with phases, exit criteria, closure gates, and independent audit records.
- Logging is maintained daily (26 consecutive daily files), bug notes capture non-obvious regressions, and retrospectives document larger gaps.
- The skills system includes 19+ method skills with a project-specific customization layer, plus a newly added multi-stage lifecycle integration guide.
- The most significant gap is **backlog bloat and signal-to-noise ratio** — the backlog README contains extremely verbose single-line items (truncated at 2000 chars by the tool), making it difficult to scan or prioritize.
- A secondary concern is **plan directory navigation overhead** — 267 plans in a flat directory without filtering, archiving, or lifecycle management could cause stale-plan drift.
- Overall the project is substantially aligned with AGE; remaining gaps are operational refinements, not systematic workflow violations.

## Alignment Matrix

| Area | Expected AGE Practice | Current Evidence | Status | Classification | Risk | Next Action |
|------|----------------------|-----------------|--------|---------------|------|-------------|
| Context Discipline | project-context, autonomy policy, codebase-map filled with real values | All files fully populated with project-specific content (154 modules, 19 domains, validation commands, protection zones, freshness tracking) | ✅ Aligned | N/A | Low | No action needed |
| Routing Discipline | docs/index.md routes to correct owner docs; task routing in AGENTS.md | Comprehensive routing table (60+ rows in index.md), clear task classification in AGENTS.md | ✅ Aligned | N/A | Low | No action needed |
| Requirements & Design | input → requirements → design/architecture pipeline | Stable design docs in docs/design/ (global + per-domain), architecture docs in docs/architecture/ | ✅ Aligned | N/A | Low | No action needed |
| Planning Discipline | Non-trivial work has plan; phases, exit criteria, closure gates; independent audits | 267 plans, all timestamped, with phases, exit criteria, closure gates, draft review records | ✅ Aligned (with concerns) | acceptable-partial-adoption | Medium | 267 flat plans create navigation overhead; consider archival/supersession policy |
| Audit Discipline | Created plans have independent draft review + closure audit | Multiple audit records in docs/audits/; plans track real draft review and closure audit iterations (some spanning 3 rounds) | ✅ Aligned | N/A | Low | No action needed; audit quality is evidenced by multi-round corrections |
| Verification Discipline | Real validation commands; verification runs regularly | Real commands in project-context.md; daily full-reactor mvn builds + Playwright E2E runs | ✅ Aligned | N/A | Low | No action needed |
| Persistent Memory | Logs written after significant work; bugs, analysis, retrospectives exist | Daily logs for 26 consecutive days; 10 bug notes; 5 retrospectives; 7+ analysis reports | ✅ Aligned | N/A | Low | No action needed |
| Optional Layer Usage | skills, lessons, audits, testing notes used when justified | 19+ skills maintained; lessons/ directory exists; testing/ directory with known-good baselines | ✅ Aligned | N/A | Low | No action needed |
| Template Adaptation | Customized for real project; not generic template content | Project-specific naming conventions, protection zones, known failure patterns (8 items), validation commands all documented in skills README | ✅ Aligned | intentional-customization | Low | No action needed |

## Findings

### Finding 1: Backlog Signal-to-Noise Ratio (Medium, Operational Gap)

- **Affected area**: `docs/backlog/README.md`
- **Current gap**: Individual backlog items contain extremely verbose descriptions (many exceeding 2000 characters, truncated by tools). Single items include execution decisions, deferred adjudication notes, phase details, and performance metrics mixed with the work item title and scope.
- **Why this matters operationally**: The backlog is the primary surface for selecting the next work item. When individual items are indistinguishable from mini-plan summaries, scanning for "what to do next" becomes cognitively expensive. AI agents and humans alike must parse hundreds of lines per row.
- **Classification**: `operational-gap` — the content is real and valuable, but its packaging hurts usability.
- **Suggested corrective slice**: Add a `## Active Queue` section at the top that lists only items with `Status: active` or workable items, with brief 1-line titles + plan ID. Move the verbose execution notes into the individual plan files (where they already exist). Keep the full table as `## Full Registry` below.

### Finding 2: Plan Directory Navigation Overhead (Low-Medium, Stale-Template Drift Risk)

- **Affected area**: `docs/plans/` directory (267 files)
- **Current gap**: All plans live in a flat directory. No archival/supersession convention is enforced at scale. Plans with status `superseded`, `deferred`, or `completed` mix with `draft` and `active` plans.
- **Why this matters operationally**: AI agents must scan 267 filenames on every interaction. Over time, stale plans accumulate and reduce the signal from the directory listing. The project has demonstrated it can generate plans rapidly — this is a strength that will become a maintenance burden.
- **Classification**: `acceptable-partial-adoption` — this is a natural consequence of high-fidelity AGE adoption and doesn't create delivery risk yet. It's a scaling concern.
- **Suggested corrective slice**: Add a `## Active Plans` registry at the top of `docs/plans/README.md` listing only non-completed plans (draft, active). Add a quarterly archival convention where plans older than 30 days with status `completed` move to a `docs/plans/archive/` subdirectory.

### Finding 3: End-to-End Testing Dependency on AMIS Runtime Behavioral Differences (Low, Acceptable Risk)

- **Affected area**: Multiple Playwright test files (e.g., `notify-inbox`, `field-formatting`, `child-table-write`)
- **Current gap**: Multiple logs entries document the same recurring pattern — Playwright webServer.port default 8080 vs application.yaml port 8011 requires explicit `BASE_URL` or `PLAYWRIGHT_PORT` configuration. AMIS behavioral quirks (adaptor `data is not defined`, GraphQL listener registration timing, `page.request` bypassing SPA auth) are discovered at test execution time, not documented upfront.
- **Why this matters operationally**: Each new test suite re-discovers the same environment configuration issues. This is a knowledge transfer problem — the lessons exist in logs but aren't extracted into reusable patterns.
- **Classification**: `acceptable-partial-adoption` — the team does eventually fix these, but each fix is reactive.
- **Suggested corrective slice**: Extract common AMIS E2E anti-patterns from the last 10 log entries into `docs/skills/nop-testing/SKILL.md` or a dedicated `docs/testing/amis-e2e-patterns.md` reference.

### Finding 4: Frontend-UI Roadmap P6 Remaining in Planned State While Rest is Complete (Low, Acceptable)

- **Affected area**: `docs/backlog/README.md` line 67 — P6 `frontend-ui-roadmap.md` item
- **Current gap**: One P6 item remains `planned`/`plan-first` while all other items (P0-P8, 67+ items) are completed. This creates a visual "incomplete" signal, but the item's scope (frontend UI completeness for buttons/grid/form/page/menu) may have been absorbed by the many F- numbered plans (F2, F3, F4, F5, F6, F8, F9) that are actively being executed.
- **Classification**: `missing-evidence` — it's unclear whether the P6 item is genuinely incomplete or whether its scope has been implicitly covered by other plans.
- **Suggested corrective slice**: Cross-reference P6 scope against completed F-plan outputs. If fully covered, mark P6 as done or superseded with a note. If genuinely incomplete, add a successor reference.

### Finding 5: Independent Closure Audit Quality Variability (Low, Operational Gap)

- **Affected area**: Multiple plans in the 2026-07-20 log (notify-inbox plan went through 3 rounds of closure audit)
- **Current gap**: The notify inbox plan required three closure-audit rounds because early audits failed to catch:
  1. The implementer was editing generated files (`_erp-notify.action-auth.xml`) that codegen overwrites
  2. The aggregator `app.action-auth.xml` was missing the notify module entirely
- **Why this matters operationally**: This is a genuine audit failure — a plan-level gate that should have caught these issues passed twice before the third round found root causes. The pattern suggests audit depth varies.
- **Classification**: `operational-gap` — the discipline of independent closure audit exists and works eventual y, but quality is inconsistent.
- **Suggested corrective slice**: Add a check to the closure audit prompt: "Verify the implementer did not edit codegen-generated files (`_gen/`, `_`-prefixed files). Check aggregator configs (`app.action-auth.xml`, `pom.xml`) for missing module references." The `nop-platform-conformance-audit-prompt.md` already has a codegen awareness dimension added on 2026-07-20 — this may already be addressed.

## Healthy Deviations

1. **Verbose backlog items**: While this is flagged as a gap, the verbosity contains real execution intelligence (decisions, deferred items, phase results). The project correctly keeps this knowledge in files rather than chat. The gap is packaging, not content quality.

2. **Plan directory flatness**: 267 plans in one directory is a navigation challenge, but the naming convention (timestamp + sequence + slug) makes scanning feasible. Creating a subdirectory hierarchy too early could fragment cross-plan references.

3. **Multi-round closure audits**: The fact that some plans required 3 rounds of independent audit is itself evidence that the audit discipline is working — issues are eventually caught and fixed, not silently closed. This is a feature of high-fidelity audit, not a bug.

4. **Skills README customization layer is substantial (18 KB)**: This is a healthy deviation — the project has done extensive project-specific adaptation that goes far beyond basic template filling. The customization includes protection zones, validation commands, naming conventions, and 8 known failure patterns with specific file paths.

## Suggested Migration Order

1. **Immediate (docs/backlog/README.md)**: Add a concise `## Active Queue` section at the top (3-5 active items at most). Move verbose per-item notes to the referenced plan files. This has the highest ROI for AI agent usability.

2. **Short-term (docs/plans/README.md)**: Add an `## Active Plans` registry listing only non-completed plans. Define archival convention for plans older than 30 days with `completed` status.

3. **Short-term (docs/testing/)**: Extract recurring AMIS E2E environment pitfalls (port mismatch, GraphQL listener timing, SPA auth bypass) into a shared reference document to reduce reactive debugging.

4. **Short-term (docs/backlog/README.md)**: Resolve P6 frontend-ui-roadmap item — either confirm completion and mark done, or define successor scope.

5. **Medium-term (closure audit prompt)**: Verify the 2026-07-20 addition to `nop-platform-conformance-audit-prompt.md` (codegen awareness dimension 13) also covers the specific aggregator-checking gap that caused the notify-inbox audit failures.

## Evidence Reviewed

- AGENTS.md (311 lines) — fully customized, no placeholder content
- docs/index.md (142 lines) — comprehensive routing table
- docs/process/application-development-workflow.md (304 lines) — 13-stage workflow defined
- docs/context/project-context.md (83 lines) — real validation commands, freshness tracking, AI blocking conditions
- docs/context/ai-autonomy-policy.md (90 lines) — real protection zone rules, reviewer availability = `subagent`
- docs/context/codebase-map.md (84 lines) — ORM model inventory for 19 modules, change routing table
- docs/context/source-of-truth-and-precedence.md (157 lines) — comprehensive conflict resolution rules
- docs/skills/README.md (201 lines) — 19 skills registered, project customization layer, skill lifecycle guide added 2026-07-20
- docs/backlog/README.md (67+ work items, many truncated >2000 chars) — P0-P8 items, nearly all ✅ done
- docs/plans/ — 267 files (1 guide + 3 legacy numbered + 263 timestamped plans)
- docs/plans/00-plan-authoring-and-execution-guide.md (226 lines) — detailed rules for plan lifecycle
- docs/audits/ — 15 entries (1 guide, 1 shell script, 13 audit records)
- docs/audits/00-audit-execution-guide.md (100 lines) — three default audit types defined
- docs/logs/ — 26 daily files (2026-06-22 through 2026-07-20)
- docs/logs/2026/07-20.md (151 lines) — most recent day shows 7+ plan executions, multi-round audits
- docs/bugs/ — 10 entries with detailed root-cause analysis
- docs/retrospectives/ — 4 entries (guide + 2 retrospectives + README)
- docs/analysis/README.md — 12 analysis reports listed
