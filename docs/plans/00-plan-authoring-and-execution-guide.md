# Plan Authoring And Execution Guide

## Goal

`docs/plans/` is for non-trivial execution slices that need explicit scope, closure criteria, and proof.

## When To Write A Plan

Write a plan when the task:

- changes API, database/model, auth, integration, deployment, or public contract behavior
- changes user-visible behavior across more than one feature surface
- touches multiple modules and changes shared behavior
- is expected to take more than one AI session
- modifies more than 5 total files or is likely to exceed roughly 200 changed lines
- needs staged implementation or explicit proof before closure

## Plan Decision Table

| Scope                                                                                                                               | Plan Level | Audit Rule                                                      | Examples                                                                               |
| ----------------------------------------------------------------------------------------------------------------------------------- | ---------- | --------------------------------------------------------------- | -------------------------------------------------------------------------------------- |
| Trivial local edit                                                                                                                  | No plan    | No draft review                                                 | typo/copy change, single style tweak, test-only cleanup                                |
| Non-trivial tracked work                                                                                                            | Full plan  | independent draft review and independent closure audit required | small UI polish with docs/test update, simple local bug fix with clear existing test   |
| Contract, data/model, API, auth, permission, integration, deployment, cross-surface, stale-doc conflict, or clearly high-risk scope | Full plan  | independent draft review and independent closure audit required | checkout flow, login behavior, data migration, external webhook, multi-module refactor |

If unsure, use a full plan.

## Minimum Rules

1. **Start from live baseline.** Read the repo first, then write `Current Baseline`. Do not rely on memory or old plans. For net-new features, the baseline must inventory all existing code the feature will touch or contradict — hardcoded values, missing hooks, incompatible patterns. An inventory is not optional.
2. **Write Goals and Non-Goals.** If either is unclear, the plan boundary is not ready.
3. **Use checkboxes for execution and closure.** Unchecked items mean unfinished work until closure.
4. **One plan, one result surface.** If the plan needs multiple independent closure criteria, it is too wide. Split it. Multi-module extraction or migration that shares the same behavioral contract and closure criteria is still ONE result surface — do not over-split.
5. **Proof before closure.** Do not mark a plan complete until the repo contains verifiable proof for every exit criterion.
6. **No code-design dumps.** The plan captures scope, proof, and closure logic, not low-level implementation detail. Exception: refactoring and extraction plans MUST include the interface contracts between extracted modules — these are structural boundary definitions, not implementation pseudocode.
7. **Tag items with types.** Each execution item must be `Fix`, `Add`, `Decision`, `Proof`, or `Follow-up`. `Fix` covers defect repairs; `Add` covers net-new code or config. An item may carry multiple types (e.g., `Decision | Add`); when it does, all implied obligations apply. A confirmed live defect or contract drift must be `Fix`, not `Follow-up`. When 80%+ of items in a phase share one type, declare the uniform type at the phase level instead of per-item (e.g., `Phase 1 — Fix-heavy (8/10 items tagged Fix)`).
8. **Record skill usage deliberately.** For each phase or item where a reusable skill matters, record `Skill: <name>` or `Skill: none`. Skills choose the work method, not the business truth. If a skill is named, its required inputs and expected output must already be clear from `docs/skills/README.md` and the referenced owner docs.
9. **Record Decisions with rationale.** Every `Decision` item must document the choice, the alternatives considered, and the residual risk if any. Write the rationale into the plan or a referenced doc. If a decision requires prototyping or exploration before committing, add a temporary `Explore` item that must conclude before the `Decision` resolves. Framework-forced or obvious choices (e.g., "must match existing framework pattern") can be noted as constrained without full alternatives analysis.
10. **Checklist integrity before closure.** Before marking a plan complete, no in-scope checklist item may remain unchecked. Either complete it or explicitly move it out of scope with a written reason. Scope narrowing after plan approval is a scope change and must be recorded with rationale; silently removing items from scope is a violation.
11. **Text consistency before closure.** Before closing, verify that `Plan Status`, every phase `Status`, every phase `Exit Criteria`, `Closure Gates`, and the `docs/logs/` entry all agree. No `completed` at the top while a phase inside still says `draft`.
12. **Independent draft review and closure audit.** Do not implement a created plan until independent draft review has revised it into an acceptable execution contract, and do not mark it complete as a side effect of finishing the last implementation slice. Use a separate review pass. Closure audit MUST NOT run in the execution session: it must be performed by an independent subagent (a fresh session that does not reuse the executor's context) iterating until pass; the executor must not self-audit, must not tick the closure-audit gate, and must not leave it `[ ]` as a "human gate" placeholder. If no independent agent is available, the executor must explicitly spawn a fresh subagent session to audit; otherwise the plan stays open. Protected areas, unresolved product risk, and source-of-truth conflicts require human/subagent review or stay open.
13. **Non-degradable items** cannot be downgraded to non-blocking follow-ups: confirmed live defects, confirmed contract drift, confirmed owner-doc drift, and CI/lint rules already fixed in the repo.
14. **Prefer one owner plan for multiple capabilities of the same component.** When several independent capabilities belong to the same component (same owner doc, same result surface), write them as phases inside a single plan rather than one plan per capability. Split into separate plans only when they carry materially different closure criteria, owner-doc obligations, or verification paths. This is the concrete form of rule 4 ("one plan, one result surface") for the common "component capability enhancement" shape, and exists to prevent a queue cluttered with one-capability-per-plan fragments.

## Plan Status Flow

Use these statuses deliberately:

- `draft` - the plan exists but has not yet passed independent draft review
- `active` - independent draft review has converged on an acceptable execution contract and implementation may begin
- `completed` - independent closure audit accepted closure
- `superseded | replaced | deferred | cancelled` - use when the plan no longer owns live closure in its original form

Recommended default flow for created plans:

1. create the first honest draft as `draft`
2. run independent draft review until the draft is acceptable
3. record the iterations in `## Draft Review Record`
4. change `Plan Status` to `active`
5. execute and update phase/workstream statuses
6. close only after independent closure audit

### Anti-Slacking Rule

Every in-scope item before closure must land in exactly one state: `landed`, `adjudicated as residual-risk-only`, `moved to explicit successor ownership`, or `removed from scope with recorded reason`.

The following words are forbidden for in-scope items: `optional`, `if time permits`, `consider`, `maybe`, `nice to have`, `as needed`. If an item is truly optional, move it out of scope explicitly rather than leaving it in a fuzzy state.

A `Follow-up` item must name the trigger condition that would promote it into scope (e.g., "when user count exceeds 10K"). A `Deferred But Adjudicated` item must name the event or decision that would reopen it (e.g., "if the new API is adopted, this work may become redundant").

## When Executing

1. Before implementation, revise the plan directly until independent draft review finds no blocking issue, then record the draft-review evidence durably in the plan by default.
2. Keep new plans at `Plan Status: draft` during draft review. Change to `active` only after the draft-review record shows the plan is acceptable for execution.
3. When you start a slice, update its `Status` to `in progress`.
4. When you finish a slice, update its `Status` to `completed` and check off all its execution items and exit criteria.
5. Before executing a phase, confirm the listed `Skill` still matches the task and available inputs. If not, update the plan before proceeding.
6. owner-doc alignment is a **plan-level** obligation, not a fixed per-phase item. A phase's exit criteria should list a doc-update step **only when that phase actually changes the live baseline, a public contract, or owner behavior**. Do not write `No owner-doc update required` as boilerplate to fill a slot — if nothing changed, write nothing. Overall owner-doc consistency is verified in the closure step (text consistency check), not repeated in every phase exit. This is the same logic as rule 10 (checklist integrity): do not pad exit criteria with no-op items.
7. **Full-repo verification belongs in Closure Gates, not in phase exit criteria by default.** A phase's exit criteria should only include checks needed to prove that phase delivered its observable result and to unblock later phases (typically a focused unit test or a localized typecheck of the new code). Do not repeat full-repo `typecheck`/`build`/`lint`/`test` in every phase exit — run them once at closure (Closure Gates already cover this). Exception: if a phase changes a public contract that the very next phase immediately depends on, write a localized check (e.g., "typecheck of the changed package passes") to unblock, but a full `build` is not required per phase.
8. Do not mark a slice complete because the function signature exists. Verify that the behavior, error handling, and test coverage land too.
9. If an item cannot be completed, move it to `Deferred But Adjudicated` with classification and reason. Do not leave it unchecked in the execution list.
10. Keep `docs/logs/` in sync with plan progress. A single aggregate log entry at plan closure is sufficient when all phases cover the same feature in one sprint; individual phase entries are required only when a phase spans a different day or a distinct deliverable.

## When Closing

Before setting `Plan Status: completed`, do all of the following:

**All created plans:**

1. Check every phase `Exit Criteria` — every one must be `[x]`.
2. Check every `Closure Gates` item — every one must be `[x]`.
3. Verify text consistency: top status, phase statuses, exit criteria, closure gates, and log entry all agree.
4. Distinguish "interface exists" from "behavior is complete". Verify the actual runtime behavior with a test or demo, not just the type signature.
5. Run the real verification commands for the repo. For plans whose primary result surface is visual, behavioral, or UX-driven, customize the verification gates with explicit justification in the plan.
6. Closure audit MUST NOT run in the execution session. Spawn an independent subagent (fresh session, no executor context) to audit until pass; the executor must not self-audit, must not tick the closure-audit gate, and must not leave it `[ ]` as a "human gate". If no independent agent is available, the plan stays open.
7. If the plan used a solo cold-replay fallback (see `AGENTS.md` Reviewer-Availability Fallback), the closure record MUST state it was used and confirm the cold-replay self-check was performed against the plan, affected docs, the actual diff, and real verification commands.

**Full closure** (multi-session, multi-module, or high-risk plans — add these):

7. Re-read the entire plan from the top, not just the most recent slice.
8. Record independent closure-audit evidence in the plan's `Closure` section and link any stored audit file under `docs/audits/` when one exists.

If any of these fail, the plan stays open.

## Template

```md
# <plan-id> <title>

> Plan Status: draft
> Last Reviewed: YYYY-MM-DD
> Source: <requirement / bug / analysis / request>
> Related: <related plans, optional>
> Audit: required

## Current Baseline

- <what is true today>
- <what gap remains>

## Goals

- <result to achieve>

## Non-Goals

- <explicitly excluded work>

## Task Route

- Type: `<requirement clarification | app-layer design change | architecture change | implementation-only change | bug investigation | verification or audit work>`
- Owner Docs: `<paths>`
- Skill Selection Basis: `<why these skills or none apply>`

## Infrastructure And Config Prereqs

- <ports, env vars, CORS, secrets, .env, external services this feature depends on>
- <if none, write "No infra prereqs beyond existing baseline">
- <for data-migration plans: include rollback strategy or script path>

## Execution Plan

### Phase 1 - <name>

Status: planned
Targets: `<paths>`
Skill: `<skill-name | none>`

- Item Types: `Fix | Decision | Proof | Follow-up`
- Prereqs: <phases or external dependencies that must complete first>

- [ ] <implementation item>
      - Skill: `<skill-name | none>`
- [ ] <Decision: record rationale and alternatives in the item or a referenced doc>
  - Skill: `<skill-name | none>`
- [ ] <Proof: specify test strategy (unit/integration/e2e) and exact verification commands>
  - Skill: `<skill-name | none>`

Exit Criteria:

> Write only what this phase actually delivers as an observable result, plus any localized check needed to unblock later phases. Do not pad with boilerplate: omit the owner-doc line entirely when nothing changed (do not write `No owner-doc update required` to fill a slot). Full-repo `typecheck`/`build`/`lint`/`test` belong in Closure Gates, not here (see When Executing rule 7). `docs/logs/` is a plan-level closure step, not a per-phase item.

- [ ] <observable result this phase delivers — specify success and failure modes>
- [ ] <localized verification, only if a later phase depends on it: focused unit test or typecheck of the changed code>

## Draft Review Record

- Independent draft review iteration 1: <needs revision | acceptable as-is | accept> (<task/session id>) because <why>
- Independent draft review iteration 2: <needs revision | acceptable as-is | accept> (<task/session id>) after <what changed>

## Closure Gates

> Close only after every item here and every phase's Exit Criteria is checked `[x]`. **Full-repo verification lives here**: run `typecheck`/`build`/`lint`/`test` (or the project's equivalents) once at closure. Do not repeat these in phase exit criteria — phases only verify what they delivered plus what unblocks later phases (see When Executing rule 7). For plans with no code change (docs-only), drop the verification command gate and state why.

- [ ] in-scope behavior is complete
- [ ] relevant docs are aligned
- [ ] verification has run (specify which commands; customize for visual/UX domains if needed)
- [ ] no in-scope item downgraded to deferred/follow-up
- [ ] independent draft review completed and recorded
- [ ] text consistency verified: status, phases, gates, and log all agree
- [ ] closure audit performed by an independent subagent (fresh session); executor did not self-audit and did not leave this `[ ]` as a human-gate placeholder
- [ ] closure evidence exists in files

## Deferred But Adjudicated

### <item name>

- Classification: `watch-only residual | optimization candidate | out-of-scope improvement`
- Why Not Blocking Closure: <reason>
- Successor Required: `yes | no`

## Closure

Status Note: <why the plan can close>

Closure Audit Evidence:

- Auditor / Agent: <independent auditor or independent subagent>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- <non-blocking follow-up items only; confirmed defects must not appear here>
```
