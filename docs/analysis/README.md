# 分析索引

使用此目录存放研究、设计权衡、比较和结论，这些内容有用但不属于活动架构契约的一部分。

## 适用场景

- 比较两个候选架构
- 记录被拒绝的选项及其原因
- 在转换为 owner docs 之前总结外部文章要点
- 在实施前分析源输入不足或误导的原因

## 顶层分析报告

- `2026-06-23-0003-menu-and-feature-completeness.md` — **菜单设计与功能完整性对照分析**：10 域菜单落地后，对照 erp-survey 中 10+ 开源 ERP（ERPNext/Odoo/赤龙/iDempiere/Atlas 等）逐域核查功能覆盖。结论：功能完整、菜单合理，145 实体 51 分组 105 菜单项覆盖主流 ERP 核心能力，无重大缺口。3 个可增强点（projects 调研深化/Calibration 归属/看板细化）+ 1 个层级取舍（三级 vs 两级）均非阻塞。菜单权威定义在各域 `erp-{short}.action-auth.xml`，本报告不复述。
- `2026-06-23-0002-design-doc-audit.md` — **设计文档审计（design-doc-audit-prompt 11 维度）**：裁决 fail（1 blocker + 5 major）。blocker：反审核目标态在 guidelines/flow-overview（REJECTED）与 purchase/sales state-machine（UNSUBMITTED）间冲突。major：erp-design-audit-checklist 重复维护点且混入实施状态、专题文档抄写 ORM schema 越界、词汇表状态集滞后 §11、核销 owner 错指、Java 伪代码越界架构层。
- `2026-06-23-0001-business-flow-coverage-audit.md` — **业务流程覆盖与准确性审计**：发现并补齐 4 类流程缺口（委外加工/生产异常返工报废退料/两步调拨在途/状态命名规范）+ 修正 2 处描述（状态命名三套并存、反审核目标态应为 REJECTED 非 UNSUBMITTED）。补齐前约 85% → 补齐后 100% 核心业务闭环。
- `2026-06-23-0000-design-doc-comprehensive-audit.md` — **设计文档综合审计报告**：4 维度系统核查（文档规范 / 功能设计 / Nop 平台最佳实践 / 对照开源 ERP 业务流程）。结论：设计文档完备合理清晰，1 个真实缺口（FactsValidator）已补齐，0 规范违规，0 最佳实践偏离，6 个超越开源 ERP 的设计点。子代理基于 erp-survey 推测的 15 个薄弱环节逐条核实，14 个实际已覆盖。
- `2026-06-22-0000-cross-domain-coupling-vs-microservice.md` — **跨域耦合策略权衡分析**（覆盖三个递进问题）：① 直接 ORM 关联 vs 多模块 + `I*Biz` vs 物理微服务三模式对比，含决策矩阵；② §8 数据库架构实测：13 个开源 ERP 全部单库（按模块分库实证为 0），校正"强关联→必须单库"的因果误读；③ §9 跨模块数据引用机制实测：逐项目列出模块依赖声明 + 数据引用字段（Odoo ORM 强引用 / ERPNext Link+on_submit / iDempiere-Metasfresh `AD_Table_ID` 弱指针 / 赤龙 `FinVoucherBillR` 中式回链 / 管伊佳逗号字符串反范式 / Tryton Pool / OFBiz 视图实体），归纳"业财边界殊途同归用弱指针"的共识模式。结论支撑 `docs/architecture/domain-module-split-analysis.md` 已定稿决策（选多 Maven 工程 + 单库 + `I*Biz` + 弱指针）。

## 子目录

- `erp-survey/` — 18 个开源 ERP 项目的逐项目深度调研 + 横向分析（2026-06-22 产出）。入口：`erp-survey/2026-06-22-0000-survey-index.md`。配套架构结论在 `docs/architecture/domain-module-split-analysis.md`。横向分析含：`2026-06-22-0000-business-design-takeaways.md`（业务设计参考）、`2026-06-22-0000-module-split-comparison.md`（模块化对比）、`2026-06-22-0000-workflow-vs-state-machine.md`（流程引擎 vs 状态变迁）、`2026-06-22-0000-subdomain-opensource-coverage.md`（新增子域开源覆盖）。
  - `2026-06-22-0000-workflow-vs-state-machine.md` — 流程实现专题：流程引擎 vs 状态变迁横向对比（含 13 项目源码实测证据）。
  - `2026-06-22-0000-subdomain-opensource-coverage.md` — 新增子域（assets/manufacturing/projects/maintenance/quality）开源参考覆盖分析。

## 文件名指南

优先使用日期文件名：

- `docs/analysis/YYYY-MM-DD-HHmm-topic.md`
- 子目录内的调研报告同样遵循 dated 命名：`docs/analysis/<topic>/YYYY-MM-DD-HHmm-<project>.md`