# 分析索引

使用此目录存放研究、设计权衡、比较和结论，这些内容有用但不属于活动架构契约的一部分。

## 适用场景

- 比较两个候选架构
- 记录被拒绝的选项及其原因
- 在转换为 owner docs 之前总结外部文章要点
- 在实施前分析源输入不足或误导的原因

## 顶层分析报告

- `2026-06-22-0000-cross-domain-coupling-vs-microservice.md` — **跨域耦合策略权衡分析**（覆盖三个递进问题）：① 直接 ORM 关联 vs 多模块 + `I*Biz` vs 物理微服务三模式对比，含决策矩阵；② §8 数据库架构实测：13 个开源 ERP 全部单库（按模块分库实证为 0），校正"强关联→必须单库"的因果误读；③ §9 跨模块数据引用机制实测：逐项目列出模块依赖声明 + 数据引用字段（Odoo ORM 强引用 / ERPNext Link+on_submit / iDempiere-Metasfresh `AD_Table_ID` 弱指针 / 赤龙 `FinVoucherBillR` 中式回链 / 管伊佳逗号字符串反范式 / Tryton Pool / OFBiz 视图实体），归纳"业财边界殊途同归用弱指针"的共识模式。结论支撑 `docs/architecture/domain-module-split-analysis.md` 已定稿决策（选多 Maven 工程 + 单库 + `I*Biz` + 弱指针）。

## 子目录

- `erp-survey/` — 18 个开源 ERP 项目的逐项目深度调研 + 横向分析（2026-06-22 产出）。入口：`erp-survey/2026-06-22-0000-survey-index.md`。配套架构结论在 `docs/architecture/domain-module-split-analysis.md`。横向分析含：`2026-06-22-0000-business-design-takeaways.md`（业务设计参考）、`2026-06-22-0000-module-split-comparison.md`（模块化对比）、`2026-06-22-0000-workflow-vs-state-machine.md`（流程引擎 vs 状态变迁）、`2026-06-22-0000-subdomain-opensource-coverage.md`（新增子域开源覆盖）。
  - `2026-06-22-0000-workflow-vs-state-machine.md` — 流程实现专题：流程引擎 vs 状态变迁横向对比（含 13 项目源码实测证据）。
  - `2026-06-22-0000-subdomain-opensource-coverage.md` — 新增子域（assets/manufacturing/projects/maintenance/quality）开源参考覆盖分析。

## 文件名指南

优先使用日期文件名：

- `docs/analysis/YYYY-MM-DD-HHmm-topic.md`
- 子目录内的调研报告同样遵循 dated 命名：`docs/analysis/<topic>/YYYY-MM-DD-HHmm-<project>.md`