# Audit Example

`tools/audit/` is a representative example of a small rule-based scanner framework.

It is intentionally generic and lightweight. The included rules are starter examples, not universal truth.

Use it when a copied project needs:

- repeatable static heuristics for suspicious code patterns
- a place to accumulate project-specific audit rules over time
- a simple example of reusable scanner infrastructure

Do not assume the bundled rules are appropriate without project-specific review.
