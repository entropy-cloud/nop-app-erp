# Analysis Index

Use this directory for research, design tradeoffs, comparisons, and conclusions that are useful but not part of the active architecture contract.

## Good Fits

- compare two candidate architectures
- capture rejected options and why they lost
- summarize external article takeaways before converting them into owner docs
- analyze why source input was insufficient or misleading before implementation

## Sub-Directories

- `erp-survey/` — 18 个开源 ERP 项目的逐项目深度调研 + 横向分析（2026-06-22 产出）。入口：`erp-survey/2026-06-22-0000-survey-index.md`。配套架构结论在 `docs/architecture/domain-module-split-analysis.md`。横向分析含：`2026-06-22-0000-business-design-takeaways.md`（业务设计参考）、`2026-06-22-0000-module-split-comparison.md`（模块化对比）、`2026-06-22-0000-workflow-vs-state-machine.md`（流程引擎 vs 状态变迁）、`2026-06-22-0000-subdomain-opensource-coverage.md`（新增子域开源覆盖）。
  - `2026-06-22-0000-workflow-vs-state-machine.md` — 流程实现专题：流程引擎 vs 状态变迁横向对比（含 13 项目源码实测证据）。
  - `2026-06-22-0000-subdomain-opensource-coverage.md` — 新增子域（assets/manufacturing/projects/maintenance/quality）开源参考覆盖分析。

## Filename Guidance

Prefer dated filenames:

- `docs/analysis/YYYY-MM-DD-HHmm-topic.md`
- 子目录内的调研报告同样遵循 dated 命名：`docs/analysis/<topic>/YYYY-MM-DD-HHmm-<project>.md`
