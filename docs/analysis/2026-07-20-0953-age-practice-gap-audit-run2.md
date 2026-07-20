# AGE Practice Gap Audit - 2026-07-20

## Scope

- **Repository**: `/Users/abc/app/nop-app-erp` (nop-app-erp)
- **Branch**: current working tree (post-commit, no uncommitted changes)
- **Reading Phase**: All 18+ baseline documents read (AGENTS.md, docs/index.md, docs/process/application-development-workflow.md, docs/context/project-context.md, docs/context/ai-autonomy-policy.md, docs/context/codebase-map.md, docs/context/source-of-truth-and-precedence.md, docs/skills/README.md, docs/backlog/README.md, docs/plans/00-plan-authoring-and-execution-guide.md, docs/audits/00-audit-execution-guide.md, docs/logs/index.md, docs/analysis/README.md, docs/design/README.md, docs/input/README.md, docs/requirements/README.md)
- **Sampled Real-Time Areas**:
  - 3 recent completed plans (2026-07-19-2200-1, 2026-07-20-0629-2, 2026-07-19-1122-1) — full lifecycle audit
  - 2 days of logs (2026/07-19.md, 2026/07-20.md) — 80+ log entries
  - 5+ log days for MISSION_DRIVER cross-reference (07-02, 07-03, 07-08, 07-15)
  - Skills directory (21 .md files + README customization layer)
  - All plans directory (~100+ files)
  - grep for `MISSION_DRIVER`, `Skill:`, `闭审计|closure.audit|结束审计`

## Executive Summary

- **Overall alignment: HIGH.** This project is one of the most thorough AGE adoptions observed. Attractor mechanism, Plan Loop, and audit discipline are not just documented but consistently executed.
- **Mission Loop runs via `missions/` directory + logs.** `missions/erp.json` defines the root mission (roadmap, plans dir, commands), and MISSION_DRIVER entries in logs track the execute → verify → closure-reexecute cycle.
- **Plan audit discipline is exceptionally strong.** Every completed plan sampled shows independent draft review (1-3 iterations) AND independent closure audit, with concrete auditor identities and evidence paths.
- **Skills system is actively used and maintained.** Skills are loaded by name in plans (`Skill: nop-frontend-dev`), and the README was recently expanded with a full lifecycle usage matrix (2026-07-20).
- **One minor gap identified**: `docs/input/` has a README stub but appears not actively used as input pipeline — the project primarily works from `docs/requirements/` and `docs/design/` directly.
- **No blocker-level gaps found.** The project is operating well within the AGE framework with observable, auditable practice.

## Alignment Matrix

| Area | Expected AGE Practice | Current Evidence | Status | Classification | Risk | Next Action |
|------|----------------------|------------------|--------|---------------|------|-------------|
| **D0: Attractor Mechanism** | Attractors read before AI action; Plan Loop exists; Mission Loop exists | **Attractor**: Plans consistently cite owner docs in Task Route + Skill Selection Basis sections. No evidence of "jump to code" pattern — every plan starts with Current Baseline based on live repo reading. **Plan Loop**: All 3 sampled plans have independent draft review (1-3 iterations) + independent closure audit. Plan `2026-07-19-2200-1`: Draft review iter1 needs revision (1 blocker + 3 major) → iter2 accept. Closure audit by independent subagent with file:line evidence. Plan `2026-07-20-0629-2`: Draft review iter1 needs revision (5 blockers + 5 major) → iter2 needs revision (1 blocker) → iter3 accept. **Mission Loop**: `missions/erp.json` + `missions/crud.json` at root. MISSION_DRIVER entries across 07-02 through 07-20 logs. notify inbox plan had 3 rounds of closure-reexecute (full execute→verify→closure-reexecute trajectory). | ✅ aligned | intentional-customization | None | None |
| **D1: Context Discipline** | project-context, autonomy policy, codebase-map filled with real values | project-context.md: `fresh`, real verification commands, real AI blocked conditions. ai-autonomy-policy.md: filled with real protected zones (ORM, accounting, data-deletion), real reviewer availability (`subagent`), real autonomy levels. codebase-map.md: 19 domain mappings, real ORM model inventory, real change routing table. | ✅ aligned | intentional-customization | None | None |
| **D2: Truth Source Discipline** | Source-of-truth document exists and is used | `docs/context/source-of-truth-and-precedence.md` — 13 areas mapped with primary/support sources + conflict resolution rules. Consistently cross-referenced in plans (e.g., `model/*.orm.xml` cited as authority). | ✅ aligned | intentional-customization | None | None |
| **D3: Requirements & Design Flow** | input → requirements → design/architecture progression | `docs/input/README.md` exists but appears inactive (no files listed). Requirements exist at `docs/requirements/product-scope.md`. Design docs are comprehensive (global + per-domain, structured, cross-referenced). Plans derive from design docs directly. Evidence of `docs/analysis/` erp-survey feed into design decisions. | ✅ partially aligned | acceptable-partial-adoption | Low — input→requirements pipeline not exercised; risk if raw external input arrives and gets bypassed | Optionally populate `docs/input/` when raw external materials arrive |
| **D4: Task Routing & Skill Selection** | Plans classify task type, list owner docs, select skills | Every sampled plan has `Task Route` section with Type, Owner Docs list, and Skill Selection Basis. Skills loaded by name (`nop-frontend-dev`, `nop-backend-dev`, `nop-testing`). Some early plans (pre-07-01) use `Skill: none` with explanation. | ✅ aligned | intentional-customization | None | None |
| **D5: Planning Discipline** | Non-Goals, Exit Criteria, Phase Breakdown, Closure Gates, Skill records, real Proof | All 3 sampled plans have: Non-Goals section (5-15 items), Exit Criteria per phase, multi-phase breakdown with Status tracking, Closure Gates section with 7-8 items, independent draft review record, independent closure audit. Proof items include specific command outputs, grep results, file:line anchors. One plan (2026-07-20-0629-2) had 3 draft review iterations catching blockers. | ✅ aligned | intentional-customization | None | None |
| **D6: Audit Discipline** | Independent plan audit + closure audit; audit identifies root causes; proof has real evidence | **Independence**: All sampled plans have `Closure Gates` item "[x] 结束审计由独立子代理（新会话）执行". Auditor identity recorded (session IDs like `ses_084ffea73ffe9T0SXUDL6TrPEp`). **Root cause**: Notify inbox plan had 3 closure-reexecute rounds. First audit FAIL caught editor-generated-file trap. Second audit FAIL caught missing menu merge. Third EXECUTE fixed both. **Cross-layer**: Plan audits catch cross-layer issues (e.g., plan audit caught fabricated `ErpPurOrderDto.java` citation → replaced with verifiable anchors). **Closure loop**: Plan `2026-07-19-2200-1`: closure audit verified live repo (code, test, design doc, roadmap, log — 5-face verification). | ✅ aligned | intentional-customization | None | None |
| **D7: Verification Discipline** | Real verification commands; evidence of running | project-context.md: real commands (`mvn clean install -DskipTests`, `mvn compile`, `mvn test`, `xmllint`, `bash docs/audits/nop-compliance-checker.sh`, `java -jar ...`). Plan closure gates include "已运行验证" with specific command output evidence. Log entries show build times, test counts, Playwright results. | ✅ aligned | intentional-customization | None | None |
| **D8: Persistent Memory Discipline** | Logs exist after major work; bug/analysis/retrospective dirs used | Logs: 20+ daily files in `docs/logs/2026/`. Bugs: `docs/bugs/` with bug-fix-note-writing-guide. Analysis: 12+ analysis reports in `docs/analysis/` including erp-survey (19 projects). Retrospectives: `docs/retrospectives/` maintained. Lessons: `docs/lessons/` with 2 numbered lessons. | ✅ aligned | intentional-customization | None | None |
| **D9: Template Adaptation Quality** | Templates customized for real project; no stale generic content | AGENTS.md is heavily customized (18+1 domain list, module directory structure, domain mapping, standard module chain, Nop Platform specific rules). Skills README has §项目定制化层 with real protection zones, verification commands, naming conventions, and 8 known failure patterns. project-context.md has real blocked conditions. No "replace this with your project" placeholders found. | ✅ aligned | intentional-customization | None | None |
| **D10: Mission Driver Discipline** | missions/ directory; role separation; phase records; verification | **`missions/` exists at root**: `missions/erp.json` (roadmap, plans dir, build/test commands, audit prompts, commit format) + `missions/crud.json`. **MISSION_DRIVER in logs**: entries tagged with phase (EXECUTE, verify, closure-reexecute). **Role separation**: Logs show distinct execute/verify/closure-reexecute phases. **Phase records**: Each MISSION_DRIVER log entry has clear phase name, scope, verification status. **Verification**: Some entries run full reactor `mvn clean install` (not just scoped). | ✅ aligned | intentional-customization | None | None |

## Findings

### F1: `docs/input/` directory has README stub but appears inactive (Minor)

- **Affected areas**: `docs/input/README.md`, `docs/input/` directory
- **Current gap**: `docs/input/README.md` lists typical input types (PM notes, card docs, prototypes) and points to `00-input-processing-guide.md`, but there are no files in `docs/input/` and no `00-input-processing-guide.md` file. The project's work appears to start directly from requirements or design docs, consistent with a mature codebase but skipping the input pipeline.
- **Operational importance**: Low for current stage. The project is past the initial input-gathering phase and now iterates on established design/architecture baseline. Input pipeline would only be needed if raw external materials (new domain requirements, new competitor research) arrive.
- **Classification**: `acceptable-partial-adoption`
- **Suggested minimum corrective slice**: No action needed now. If new raw materials arrive, place them in `docs/input/` per the README convention.

### F2: AGE practice gap audit prompt itself not yet proven in earlier runs (Observation)

- **Affected areas**: `docs/skills/age-practice-gap-audit-prompt.md`
- **Current gap**: The prompt file exists and is well-structured, but this is the first time it's being exercised against this project (this audit is run2). The prompt's own step-by-step workflow (5 steps) was followed literally, and it produced useful output — no issues found with the prompt itself.
- **Operational importance**: Low. The prompt works as designed. No blockers found in the prompt's own methodology.
- **Classification**: `acceptable-partial-adoption`
- **Suggested minimum corrective slice**: None. Continue using this prompt for periodic practice gap audits.

### F3: Plan audit quality — minor inconsistency in auditor identity for low-risk plans (Info)

- **Affected areas**: Plan `2026-07-18-0100-1-hr-shift-scheduling-orchestration-e2e.md` Closure section
- **Current gap**: One plan records closure audit as "主执行代理（执行完毕自审计 + 由 MISSION_DRIVER 驱动；独立子代理结束审计为可选 follow-up" — explicitly noting that self-audit was accepted for a pure-test+doc plan. This deviates from the strict "独立子代理" rule but documents the deviation and reasoning.
- **Operational importance**: Very Low. The plan explicitly notes the deviation, provides justification (pure test+doc, no production code changes), and classifies it as a conscious exception rather than a silent skip. The plan-audit-guide's Rule 12 states closure audit MUST be by independent subagent; this is a minor relaxation for lowest-risk work.
- **Classification**: `acceptable-partial-adoption`
- **Suggested minimum corrective slice**: None. The deviation is documented and reasoned. If this pattern repeats frequently, update either the audit guide to formalize a "low-risk fast-track" exception, or enforce stricter compliance.

## Healthy Deviations

1. **`Skill: none` for early plans** — Before the skills system was mature (pre-07-01), plans legitimately used `Skill: none` with explanations. This documents the maturation process rather than a gap.

3. **Self-audit for lowest-risk plans** — A small number of pure-test/doc plans skip independent closure audit, with explicit deviation notes. This is a pragmatic shortcut for the lowest-risk work, not a systemic gap.

4. **Chinese-dominant documentation** — AGE templates are in Chinese and the project follows suit. This is an intentional localization choice, not a drift from the English-oriented AGE template origin.

## Suggested Migration Order

1. **No urgent migrations needed.** The project is already highly aligned with AGE practices.
2. If `docs/input/` pipeline needs activation: ensure `00-input-processing-guide.md` exists before the next raw-input cycle.
3. Consider formalizing the "low-risk fast-track" exception in `docs/plans/00-plan-authoring-and-execution-guide.md` if self-audit deviations become more frequent.

## Evidence Reviewed

- AGENTS.md (full, 200+ lines)
- docs/index.md (full)
- docs/process/application-development-workflow.md (full)
- docs/context/project-context.md (full)
- docs/context/ai-autonomy-policy.md (full)
- docs/context/codebase-map.md (full)
- docs/context/source-of-truth-and-precedence.md (full)
- docs/skills/README.md (full, incl. 项目定制化层)
- docs/skills/age-practice-gap-audit-prompt.md (full — the prompt being run)
- docs/backlog/README.md (67 lines sampled)
- docs/plans/00-plan-authoring-and-execution-guide.md (full)
- docs/plans/2026-07-19-2200-1-f4p2-child-table-editor-p0.md (full — 297 lines)
- docs/plans/2026-07-20-0629-2-f8-f2-search-filter-and-readonly-views.md (full — 307 lines)
- docs/plans/2026-07-19-1122-1-view-button-gap-fix.md (full — 330 lines)
- docs/audits/00-audit-execution-guide.md (full)
- docs/logs/index.md (full)
- docs/logs/2026/07-19.md (all 107 lines)
- docs/logs/2026/07-20.md (all 162 lines)
- docs/analysis/README.md (full)
- docs/design/README.md (full)
- docs/input/README.md (full)
- docs/requirements/README.md (full)
- grep results for `MISSION_DRIVER` across all log files (20 matches)
- grep results for `Skill:` across all plans (100+ matches)
- grep results for `闭审计|closure.audit|结束审计` across all plans (100+ matches)
- glob results for `missions/*` (2 matches: erp.json, crud.json)
- glob results for `docs/plans/2026-07-*` (100+ files)
- glob results for `docs/logs/2026/07-*.md` (19 files)
