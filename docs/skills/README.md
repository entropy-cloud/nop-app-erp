# Skills Index

Use this directory for reusable prompts and workflow playbooks.

These are not one-off chat messages. They are reusable repo memory.

Skills should primarily capture reusable work methods, review methods, or audit methods. Do not use a skill as a substitute for requirement truth, design truth, or architecture truth.

A skill library is not the attractor. Without routing through `AGENTS.md`, `docs/index.md`, active requirements, and owner docs, a large skill library usually degenerates into structured vibe coding.

These prompts are generic defaults for copied projects. After copying the template, you MUST customize them to the project's real owner docs, protected areas, verification stack, naming conventions, known failure modes, and false-positive tolerance.

## Skill Routing Rule

Before choosing a skill:

1. Read the relevant requirement and owner docs first.
2. Classify the task type using `AGENTS.md`.
3. Choose the skill by matching the work method, not just the business label.
4. If multiple skills could fit, ask an independent subagent or reviewer to choose before implementation.
5. If no existing skill clearly fits, record `Skill: none` and proceed with the normal docs-driven workflow.
6. For non-trivial plans, record the skill selection basis and review result in the plan.

Do not add broad business-scenario skills as a replacement for project-specific owner docs. If a scenario repeats often, first check whether routing, owner docs, or plan guidance are missing. Promote a skill only when the reusable work method is stable.

## Skill Registry

| Skill                                     | Use when                                                                           | Do not use when                               | Required inputs                                                              | Expected output                                |
| ----------------------------------------- | ---------------------------------------------------------------------------------- | --------------------------------------------- | ---------------------------------------------------------------------------- | ---------------------------------------------- |
| `age-practice-gap-audit-prompt.md`        | a repo needs comparison between live practice and intended AGE workflow            | the task is a local feature implementation    | AGE baseline docs, current repo structure, active docs, sampled live evidence | analysis note under `docs/analysis/` with prioritized gaps |
| `document-audit-prompt.md`                | requirement, design, or architecture docs may be incomplete or inconsistent        | the task is trivial and local                 | target doc paths, relevant input or owner docs                               | audit findings and revision targets            |
| `design-doc-audit-prompt.md`              | `docs/design/` needs revalidation as the app-layer behavior baseline               | a single narrower audit (state machine, plan) is what is needed | all `docs/design/` files, relevant requirement, `domain-design-guidelines.md` when present | severity-ordered findings and dispositions     |
| `design-completeness-scan-prompt.md`      | proactively scan `docs/design/` for missing domains/documents/capabilities against target scope | validating existing docs (use `design-doc-audit-prompt.md` instead) | `docs/design/` tree, `product-scope.md`, roadmap, `flow-overview.md` | prioritized gap list driving next-round doc additions |
| `state-machine-business-review-prompt.md` | a workflow state machine (order/approval/dispute/lifecycle) needs correctness review | the change is trivial or unrelated to transitions | the owner doc defining the state machine, relevant requirement                 | P0–P3 findings, verdict, reachability/role/external summaries |
| `plan-audit-prompt.md`                    | a non-trivial plan is ready for challenge before implementation                    | no plan exists yet                            | plan file, related requirement and owner docs                                | pass/fail audit with concrete issues           |
| `closure-audit-prompt.md`                 | implementation claims completion and needs independent closure review              | work is still mid-flight                      | plan, verification evidence, relevant changed docs                           | closure verdict and remaining gaps             |
| `requirement-gap-retrospective-prompt.md` | landed work still missed expectations and the requirement pipeline needs diagnosis | the requirement is still being drafted        | original input, requirement/discussion docs, delivered result                | retrospective findings and process corrections |
| `multi-dimensional-audit-prompt.md`       | high-risk work needs challenge across multiple dimensions at once                  | a single-object audit is already sufficient   | relevant requirement/owner docs, plan or changed area, verification evidence | findings grouped across dimensions             |
| `open-ended-audit-prompt.md`              | hidden problems may exist outside the normal checklist                             | the work only needs a narrow structured audit | relevant requirement/owner docs, plan if any, logs, live changed code        | adversarial findings and unknown-risk notes    |
| `index-routing-audit-prompt.md`          | a docs index or directory structure needs routing effectiveness review             | the index has no routing role or is trivial   | top-level index, sub-indexes, target files                                   | coverage table, persona test results, structural findings |
| `bug-diagnosis-prompt.md`                 | a bug is real but the root cause is not yet proven                                | the defect is already obvious and local       | bug report, owner docs, reproduction path, verification command              | confirmed cause and proof path                 |
| `code-quality-audit-prompt.md`            | reviewing code for behavioral risk and implementation quality                      | only formatting or trivial nits are needed    | changed files, owner docs, tests or verification evidence                    | severity-ordered findings                      |
| `code-refactor-discovery-prompt.md`       | structural cleanup candidates need discovery before refactoring                    | the structural target is already agreed       | target area, owner docs, current code                                        | ranked refactor candidates                     |
| `code-refactor-prompt.md`                 | behavior-preserving structural refactor work is the task                           | the task changes supported behavior           | target area, invariants, verification commands                               | safe refactor execution and proof              |

## Starter Skills

- `age-practice-gap-audit-prompt.md`
- `document-audit-prompt.md`
- `design-doc-audit-prompt.md`
- `design-completeness-scan-prompt.md`
- `state-machine-business-review-prompt.md`
- `plan-audit-prompt.md`
- `closure-audit-prompt.md`
- `requirement-gap-retrospective-prompt.md`
- `multi-dimensional-audit-prompt.md`
- `open-ended-audit-prompt.md`
- `index-routing-audit-prompt.md`
- `bug-diagnosis-prompt.md`
- `code-quality-audit-prompt.md`
- `code-refactor-discovery-prompt.md`
- `code-refactor-prompt.md`

## Relationship With Tool-Native Skills

`docs/skills/` holds method- and audit-type skills that live inside the repository and stay portable across tools and editors. Some AI tools also support their own native skill loading (for example a project-local skills directory that the tool auto-loads). The two are complementary, not competing:

- put reusable review/audit methods and prompts in `docs/skills/` so they are versioned with the docs and readable by any agent or human
- put tool-loaded operational skills (framework-specific how-tos, codegen recipes, debug recipes) in the tool's own conventions if you use one
- keep routing through `AGENTS.md`, `docs/index.md`, and owner docs regardless of where a skill physically lives; skills select the work method, they do not replace owner-doc routing

The template itself stays tool-neutral and does not assume any specific AI tool.
