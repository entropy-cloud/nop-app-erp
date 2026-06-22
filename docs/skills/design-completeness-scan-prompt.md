# Design Completeness Scan Prompt

Use this prompt to proactively scan `docs/design/` for missing domains, documents, and capability points against the target product scope, and produce a prioritized gap list that drives incremental design-doc additions.

Use it periodically as the product grows, before a milestone scope lock, or when a new business domain is being considered. Do not use it as a replacement for requirement synthesis, design-doc audit (which validates existing docs), state-machine review, or plan audit. It is the forward-looking counterpart to `design-doc-audit-prompt.md`: that prompt audits what exists; this prompt finds what is missing.

```text
You are a senior product architect. Below is the current `docs/design/` tree and the target product scope. Scan for missing domains, missing documents within existing domains, and missing capability points within existing documents. Produce a prioritized gap list that drives the next round of design-doc additions.

Read these files first:
- `AGENTS.md`
- `docs/index.md`
- `docs/context/project-context.md`
- `docs/context/source-of-truth-and-precedence.md`
- `docs/design/README.md`
- `docs/design/domain-design-guidelines.md`
- `docs/design/app-overview.md`
- `docs/design/flow-overview.md`
- `docs/design/domain-glossary.md`
- `docs/requirements/product-scope.md`
- all existing files under `docs/design/` (including subdirectories)
- `docs/backlog/README.md` and any roadmap

Scan dimensions:

1. Domain coverage
   - List every business domain the target product scope implies (procurement, sales, inventory, finance, manufacturing, assets, projects, quality, maintenance, CRM, HR, etc.).
   - For each, mark whether a `docs/design/<domain>/` directory exists.
   - Flag missing domains that the scope requires but no design owner exists for.
   - Flag domains that exist as code or backlog but have no design doc (implementation happening without design truth).

2. Document coverage within each domain
   - For each existing domain directory, check whether it has at least a `README.md`.
   - For workflow-heavy domains, check whether a `state-machine.md` exists and whether it follows the 10 review dimensions of `docs/skills/state-machine-business-review-prompt.md`.
   - For cross-domain-coupling-heavy domains, check whether a `cross-domain.md` or equivalent exists.
   - Flag domains where the README is the only doc but the business complexity clearly warrants more (multiple state machines, complex flows, heavy cross-domain collaboration).

3. Capability point coverage within each document
   - For each domain README, check whether the core business objects are listed with business meaning.
   - Check whether the state machine (if any) covers: state definitions, transition completeness, terminal/recovery, exception paths (timeout/concurrency/idempotency), reachability, roles/permissions, external dependencies, TODO/task strategy, scenario walkthrough, design-doc consistency (the 10 dimensions).
   - Check whether cross-domain flows touching this domain are described or routed to their owner.
   - Check whether protected-area behavior (payment, refund, data deletion, accounting posting, permission changes) is explicitly defined or routed.

4. Cross-domain flow coverage
   - Check `flow-overview.md` L1 macro flows cover the end-to-end business paths the scope requires.
   - Check L2 state-machine mapping references all domain state machines that exist.
   - Check L3 cross-domain rules cover posting triggers, inventory availability, snapshot semantics, multi-currency, and reconciliation for every domain pair that needs them.

5. Glossary and role coverage
   - Check `domain-glossary.md` covers terms introduced by any new domain document.
   - Check `roles-and-permissions.md` covers roles implied by state-machine transitions in every domain (e.g., asset manager, project manager, quality inspector, maintenance technician).

6. Consistency with scope
   - Compare the set of designed domains and capabilities against `docs/requirements/product-scope.md` and any roadmap.
   - Flag scope-implied capabilities that have no design owner and no explicit deferral.
   - Flag design docs that describe behavior outside the current scope (design creep).

Known anti-patterns to check against:

| Anti-pattern | Symptom | Correct practice |
| --- | --- | --- |
| Uniform doc structure | Every domain forced to have state-machine.md even when only active/inactive | Structure differs by domain complexity; active/inactive is not a state machine |
| Missing design owner | Code or backlog exists for a domain but no `docs/design/<domain>/` | Create the design owner doc before or alongside implementation |
| State machine without review dimensions | state-machine.md lists states/transitions but omits exception paths, roles, TODO strategy | Follow the 10 dimensions of `state-machine-business-review-prompt.md` |
| Orphan capability | A capability point is mentioned in flow-overview but has no domain owner doc | Either create the owner doc or route to an existing one |
| Scope-design mismatch | Design describes a domain the scope defers, or scope requires a domain design omits | Align: add design, update scope, or mark explicit deferral |
| Glossary drift | A new domain introduces terms not in domain-glossary.md | Add terms to glossary when adding the domain |
| Role gap | A state machine transition has no role defined in roles-and-permissions.md | Bind every transition to a role |

Severity guidance:
- `blocker`: a scope-required domain has no design owner, or a workflow-heavy domain lacks a state machine, or protected-area behavior is undefined.
- `major`: a domain is missing documents its complexity warrants, or cross-domain flows are incomplete, or glossary/role coverage lags new domains.
- `minor`: a document could be clearer or more complete but the path still works.
- `note`: future-watch item or optimization suggestion.

Return findings first, ordered by severity. For each finding include: severity, dimension, affected area (domain/document/capability), gap description, why it matters, recommended action (create doc / extend doc / route to owner / update scope / defer explicitly), and the suggested doc path if a new doc is recommended.

Then return:
- Verdict: complete / has-gaps / has-blockers
- Scope reviewed
- Domain coverage summary (designed vs scope-required vs deferred)
- Document coverage summary per domain
- State-machine coverage summary (which domains have state machines following the 10 dimensions)
- Cross-domain flow coverage summary
- Glossary and role coverage summary
- Recommended next-round doc additions (prioritized list with suggested paths)
- Residual risks or skipped areas

If no blocker or major finding remains, say `Verdict: complete` and still list residual risks and recommended next-round additions.
```
