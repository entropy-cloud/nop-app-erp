# Roadmap-Led Goal Execution

## Purpose

This guide defines the delivery loop for accepted plans in this repository. The repo is the source of truth; chat is temporary.

## One Accepted Plan, One Commit

A work item is closed by one independent closure audit, and the accepted plan is delivered in **one local commit**. Do not mix unrelated work into that commit. If this repo ships `tools/commit-plan.mjs`, use it; otherwise reproduce the same discipline manually:

1. Finish the roadmap tick, plan checkboxes, verification evidence, daily log and owner-doc updates before taking the proof snapshot.
2. Compute a payload proof over the exact changed files (the helper does this with `--snapshot`; without the helper, record the exact path list and hashes in the plan). Record it in `## Verification`.
3. Re-run the snapshot/check and confirm the digest is unchanged (fixed point) before committing.
4. Commit exactly those files, with the plan path in the commit body (the helper adds `AGE-Plan:`). A hook failure stops delivery; do not bypass hooks without understanding and recording the cause.

## Snapshot Procedure

1. **Finalize the roadmap tick before the first snapshot.** A tick landing after the snapshot changes the payload and invalidates recorded proof.
2. Take the snapshot, record the payload line, then substitute the hash only into digest-exempt lines (payload/pass/closure receipt lines). A hash on a wrapped continuation line breaks convergence.
3. **Re-run the snapshot and confirm the digest is unchanged (a fixed point) before committing.**
4. If delivery is rejected because the path set differs, shared log/roadmap content has already landed with an earlier commit: re-snapshot with only the actually-changed paths and update the proof line's `paths=` accordingly.

**Shared-surface discipline**: daily logs and the roadmap/worklist are append/tick surfaces shared across concurrent plans. Either serialize shared-surface writes across in-flight deliveries, or verify the swept-in content belongs to accepted work and say so in the commit body.

## Receipts And Independence

A created plan needs an independent draft review, an independent executor, and a closure auditor distinct from both. Record the actual actors in the plan. `plan-status --check` (where available) validates pairing and declared independence, not identity authentication; without the helper, re-read the whole plan.

## Degradation

If an independent reviewer or auditor is unavailable, keep the plan open and report the blocker. Cold replay is not a second reviewer.

## Delivery Commands (explicit)

A cold-start coordinator does not get an automatic commit. Use the helper explicitly after closure acceptance:

```bash
node tools/plan-status.mjs --check docs/plans/<slug>/<plan>.md
node tools/commit-plan.mjs --snapshot docs/plans/<slug>/<plan>.md -- docs/plans/<slug>/<plan>.md <changed-file>...
# record "- payload <sha> base=<base> paths=[...]" in ## Verification
# append "- pass <key> <runId> exit=0 payload=<sha>" after real verification
# auditor appends "- accepted #...: ... payload=<sha> ownership=whole-files"
node tools/commit-plan.mjs --snapshot docs/plans/<slug>/<plan>.md -- docs/plans/<slug>/<plan>.md <changed-file>...  # fixed point
node tools/commit-plan.mjs --commit docs/plans/<slug>/<plan>.md --message "feat(scope): deliver <work-item>" -- docs/plans/<slug>/<plan>.md <changed-file>...
```

The helper guarantees **exactly one** commit per plan: it refuses when an `AGE-Plan: <repo-relative-plan-path>` receipt already exists and reports `alreadyDelivered` on a matching retry. It stages only the explicit paths and requires `deliveryReady`, current payload, and whole-file ownership. Re-running `--commit` never creates a second commit. No hook or engine performs this step automatically.

## No Auto-Commit

There is **no** Git hook, Mission Driver, scheduler, or status transition that commits when a plan closes. Historical notes or older plans may say "每计划完成后自动提交一次"; that phrase described an earlier convention and is superseded by this guide: one accepted plan = **one commit**, triggered explicitly by the coordinator via `tools/commit-plan.mjs --commit`. Closing a plan without invoking the helper leaves changes uncommitted; a plain `git commit` / `git add -A` is not the delivery path and can cause mixed or duplicate commits.
