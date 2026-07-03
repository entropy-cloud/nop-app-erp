# 分析索引

使用此目录存放研究、设计权衡、比较和结论，这些内容有用但不属于活动架构契约的一部分。

## 适用场景

- 比较两个候选架构
- 记录被拒绝的选项及其原因
- 在转换为 owner docs 之前总结外部文章要点
- 在实施前分析源输入不足或误导的原因

## 顶层分析报告

- `2026-07-01-1900-platform-best-practices-compliance-audit.md` — **平台最佳实践合规审计（4 维度并行子代理 + 主代理 grep 复核）**：对照 `nop-entropy/docs-for-ai/` + 本项目 `docs/architecture/`，审计已按 ERP roadmap 编写的代码（CRUD 18 域 + P1 业务逻辑 5 段）。综合 **7.3/10**：模块结构与 ORM 命名优秀（8.5/10、7.0/10），**自定义服务层为最集中短板（5.5/10）** —— 全项目 `@BizMutation/@BizQuery` 命中 0 次（用 `@Transactional` 代替，阻塞 GraphQL 暴露）、跨域访问用 `daoFor()` 绕过 I*Biz 管道、`requireEntity` 退化为 `getEntityById`、`new ErpXxx()` 直接构造。另发现：3 核心域金额族 122+ 字段 VARCHAR（sales 对照已 DECIMAL，未登记决策）、purchase/sales→projects 跨域 ORM 引用违反 DAG、DRP 菜单 3 处断链、单据状态机按钮全未接线。每条附 `文件:行号` 证据 + 规则出处。**执行计划**：`docs/plans/2026-07-01-1900-1-platform-compliance-remediation.md`。
- `2026-06-30-0001-advanced-scenario-design-comparison.md` — **高级场景设计深度对比（计划 03 的设计依据）**：三层挖掘（erp-survey 调研报告 + `/Users/abc/sources/erp/` 源码回查 + 提炼），为计划 03 的 9 个 P0/P1 场景补充"最佳实践+反模式+落地方案+证据"。**纠正了 0000 分析报告与计划 03 初版的 3 处证据过度引申**（票据 metasfresh.md:83 实测只注册银行对账单非票据、TMS 主证应为 Metasfresh SPI 非 Odoo、EDI 主证应为 Odoo account_edi 而 ERPNext edi 仅是代码映射非引擎）、**2 处设计错误**（供应商评分卡应拆 AVL→master-data/评分→purchase、TMS SPI 应三层非单层）、补 5 处设计裁决、识别 4 个 SPI 注册中心同构（业财过账/CRM/TMS/EDI）。证据强度统一标注 🟢源码实测/🟡调研记载/⚪领域常识。**执行计划**：`docs/plans/03-advanced-scenario-design-gap-fill.md`。
- `2026-06-30-0000-advanced-scenario-gap-analysis.md` — **高级业务场景缺口分析（对照 erp-survey 13 个开源 ERP，经两轮独立子代理审查修正）**：在前序四份审计已确认核心闭环完备（含 0003 菜单审计的采购寻源 RFQ 覆盖结论）基础上，聚焦"高级/边缘场景"识别盲区，**实体存在性以各域 `model/*.orm.xml` 为权威**。结论：13 个未深入展开的场景——2 个 P0（费用报销/员工借款【与 projects 文档内部不一致需优先补齐】、资金/票据）、7 个 P1（CRP/APS【项目自承认延迟项】、供应商评分卡/AVL、VMI/寄售【调研证据弱已如实标注】、CRM、运输 TMS、EDI/ASN、批次召回）、4 个 P2 明确不纳入核心（DRP/HRMS/POS/售后，另有 ABC 分类作为 P2 备选）。所有扩展均复用现有架构契约（SPI/三轴状态/跨域 DAG），渐进式可插拔。**审查纠正了初稿三处严重问题**：① 误判采购 RFQ 为缺口（`ErpPurRfq`/`ErpPurQuotation` 实体早已存在，撤销 checklist 修正建议）；② 清除多处编造调研证据（VMI 代销/recall/hr_expense 等 erp-survey 零命中，liberoHR 归属 Metasfresh 非 iDempiere）；③ 补列 CRP（项目自承认延迟项）。仅 `feature-inventory.md` 缺报销条目一处真实不一致待修正。
- `2026-06-23-0004-configuration-comparison.md` — **配置项对照分析（产品级/用户级/系统级三层）**：对照 erp-survey 中 9 个开源 ERP（Odoo/ERPNext/iDempiere/Metasfresh/管伊佳/赤龙/星云/若依/Dolibarr）的配置机制。结论：nop-app-erp 配置只覆盖系统级+产品级两层（且产品级 42 键代码零落地），用户级完全空白，粒度仅全局。对照 iDempiere/Metasfresh 的 Client+Org 双层粒度、ERPNext 的 doctype 化、星云的按模块审批配置，有 6 项改进（P0 产品级落地 NopSysConfig / P1 粒度租户组织级 / P1 审批按模块配置化 / P2 用户级 / P2 规范化治理 / P2 生产 profile）。
- `2026-06-23-0003-menu-and-feature-completeness.md` — **菜单设计与功能完整性对照分析**：10 域菜单落地后，对照 erp-survey 中 10+ 开源 ERP（ERPNext/Odoo/赤龙/iDempiere/Atlas 等）逐域核查功能覆盖。结论：功能完整、菜单合理，145 实体 51 分组 105 菜单项覆盖主流 ERP 核心能力，无重大缺口。3 个可增强点（projects 调研深化/Calibration 归属/看板细化）+ 1 个层级取舍（三级 vs 两级）均非阻塞。菜单权威定义在各域 `erp-{short}.action-auth.xml`，本报告不复述。
- `2026-06-23-0002-design-doc-audit.md` — **设计文档审计（design-doc-audit-prompt 11 维度）**：裁决 fail（1 blocker + 5 major）。blocker：反审核目标态在 guidelines/flow-overview（REJECTED）与 purchase/sales state-machine（UNSUBMITTED）间冲突。major：erp-design-audit-checklist 重复维护点且混入实施状态、专题文档抄写 ORM schema 越界、词汇表状态集滞后 §11、核销 owner 错指、Java 伪代码越界架构层。
- `2026-06-23-0001-business-flow-coverage-audit.md` — **业务流程覆盖与准确性审计**：发现并补齐 4 类流程缺口（委外加工/生产异常返工报废退料/两步调拨在途/状态命名规范）+ 修正 2 处描述（状态命名三套并存、反审核目标态应为 REJECTED 非 UNSUBMITTED）。补齐前约 85% → 补齐后 100% 核心业务闭环。
- `2026-06-23-0000-design-doc-comprehensive-audit.md` — **设计文档综合审计报告**：4 维度系统核查（文档规范 / 功能设计 / Nop 平台最佳实践 / 对照开源 ERP 业务流程）。结论：设计文档完备合理清晰，1 个真实缺口（FactsValidator）已补齐，0 规范违规，0 最佳实践偏离，6 个超越开源 ERP 的设计点。子代理基于 erp-survey 推测的 15 个薄弱环节逐条核实，14 个实际已覆盖。
- `2026-06-22-0000-cross-domain-coupling-vs-microservice.md` — **跨域耦合策略权衡分析**（覆盖三个递进问题）：① 直接 ORM 关联 vs 多模块 + `I*Biz` vs 物理微服务三模式对比，含决策矩阵；② §8 数据库架构实测：13 个开源 ERP 全部单库（按模块分库实证为 0），校正"强关联→必须单库"的因果误读；③ §9 跨模块数据引用机制实测：逐项目列出模块依赖声明 + 数据引用字段（Odoo ORM 强引用 / ERPNext Link+on_submit / iDempiere-Metasfresh `AD_Table_ID` 弱指针 / 赤龙 `FinVoucherBillR` 中式回链 / 管伊佳逗号字符串反范式 / Tryton Pool / OFBiz 视图实体），归纳"业财边界殊途同归用弱指针"的共识模式。结论支撑 `docs/architecture/domain-module-split-analysis.md` 已定稿决策（选多 Maven 工程 + 单库 + `I*Biz` + 弱指针）。

## 子目录

- `erp-survey/` — 19 个开源项目逐项目深度调研 + 横向分析（初始 18 ERP + 2026-07-03 追加 1 AI CRM：Wukong AICRM）。入口：`erp-survey/2026-06-22-0000-survey-index.md`。配套架构结论在 `docs/architecture/domain-module-split-analysis.md`。横向分析含：`2026-06-22-0000-business-design-takeaways.md`（业务设计参考）、`2026-06-22-0000-module-split-comparison.md`（模块化对比）、`2026-06-22-0000-workflow-vs-state-machine.md`（流程引擎 vs 状态变迁）、`2026-06-22-0000-subdomain-opensource-coverage.md`（新增子域开源覆盖）。
  - `2026-06-22-0000-workflow-vs-state-machine.md` — 流程实现专题：流程引擎 vs 状态变迁横向对比（含 13 项目源码实测证据）。
  - `2026-06-22-0000-subdomain-opensource-coverage.md` — 新增子域（assets/manufacturing/projects/maintenance/quality）开源参考覆盖分析。

## 文件名指南

优先使用日期文件名：

- `docs/analysis/YYYY-MM-DD-HHmm-topic.md`
- 子目录内的调研报告同样遵循 dated 命名：`docs/analysis/<topic>/YYYY-MM-DD-HHmm-<project>.md`