# 文档改进计划：基于设计审查决议

> **Plan Status**: completed
> **Last Reviewed**: 2026-06-29
> **Source**: `docs/discussions/2026-06-29-1000-grill-with-docs-design-review.md`
> **Audit**: required

## 目标

基于 `docs/discussions/2026-06-29-1000-grill-with-docs-design-review.md` 中的 81 个设计审查决议（Q1-Q83，排除 Q62/Q63 非具体决策），系统性地更新和补充设计文档，确保设计文档准确反映已确认的业务规则、状态机、配置策略和跨域约束。

## 非目标

- 不修改 ORM 模型文件（`.orm.xml`）—— 本计划仅涉及设计文档
- 不实现业务逻辑代码 —— 本计划仅涉及文档层面
- 不重新设计已确认的业务规则 —— 仅文档化已决议的内容
- 不修改已确认的架构决策 —— 仅文档化已决议的内容

## 任务路由

- **任务类型**：应用层设计变更（文档层面）
- **Owner Docs**：`docs/design/`、`docs/architecture/`、`docs/discussions/2026-06-29-1000-grill-with-docs-design-review.md`
- **Skill Selection Basis**：无特定技能匹配，使用通用文档编写技能
- **验证命令**：`xmllint --noout` 检查 XML 文件格式，人工审查文档内容一致性

## 当前基线

### 文档现状盘点

**全局设计文档（8份）**：
- `docs/design/README.md` — 设计文档索引与结构原则 ✅ 已存在
- `docs/design/app-overview.md` — 应用基线 ✅ 已存在
- `docs/design/domain-design-guidelines.md` — 域设计原则与标准字段约定 ✅ 已存在，需更新
- `docs/design/flow-overview.md` — 全局业务流程编排 ✅ 已存在，需更新
- `docs/design/domain-glossary.md` — 核心术语统一词汇表 ✅ 已存在，需更新
- `docs/design/erp-design-audit-checklist.md` — 设计核对维度 ✅ 已存在
- `docs/design/feature-inventory.md` — 功能清单 ✅ 已存在
- `docs/design/roles-and-permissions.md` — 角色与权限模型 ✅ 已存在

**域设计文档（部分需更新）**：
- `docs/design/purchase/` — 采购域文档 ✅ 已存在，需更新
- `docs/design/sales/` — 销售域文档 ✅ 已存在，需更新
- `docs/design/inventory/` — 库存域文档 ✅ 已存在，需更新
- `docs/design/finance/` — 财务域文档 ✅ 已存在，需更新
- `docs/design/manufacturing/` — 制造域文档 ✅ 已存在，需更新
- `docs/design/quality/` — 质量域文档 ✅ 已存在，需更新
- `docs/design/assets/` — 资产域文档 ✅ 已存在，需更新
- `docs/design/projects/` — 项目域文档 ✅ 已存在，需更新
- `docs/design/maintenance/` — 维护域文档 ✅ 已存在，需更新
- `docs/design/master-data/` — 主数据域文档 ✅ 已存在，需更新

**架构文档（需新增）**：
- `docs/architecture/` — 需新增多个架构文档

### 差距分析

**需要修改的文档**：约 25 份现有文档需更新
**需要新增的文档**：约 10 份新文档需创建
**覆盖的决议**：81 个具体决策（Q1-Q83）

## 执行阶段

### Phase 1: 全局设计文档更新 (12 项)

**Status**: completed
**Targets**: `docs/design/domain-design-guidelines.md`, `docs/design/flow-overview.md`, `docs/design/domain-glossary.md`
**Skill**: none

**执行项目**：

- [x] **Fix | Add** `domain-design-guidelines.md` §11.2 — 会计期间状态改为 `NOT_OPENED→OPEN→CLOSING→CLOSED` (Q2)
- [x] **Fix | Add** `domain-design-guidelines.md` §11.2 — purchase docStatus 初始态描述双轴化 (Q3)
- [x] **Add** `domain-design-guidelines.md` 新增 ErrorCode 节 — ErrorCode 命名空间、编码规则 (Q12)
- [x] **Fix** `domain-design-guidelines.md` §7 — 审计日志策略明确（复用平台 + 危险操作审计）(Q15)
- [x] **Fix** `domain-design-guidelines.md` §10 — 单据编号 orgId 内唯一规则 (Q10)
- [x] **Add** `domain-design-guidelines.md` 新增删除策略节 — 三档删除策略 (Q11)
- [x] **Add** `domain-design-guidelines.md` 新增期间节 — 跨域统一复用 finance 期间 (Q19)
- [x] **Fix** `flow-overview.md` §3 — 表格标题改为 approveStatus 映射 + 补充 docStatus 表 + 制造域状态映射 (Q1 Q6)
- [x] **Fix** `flow-overview.md` §5.3 — 撤销提交路径补充 (Q20)
- [x] **Fix** `domain-glossary.md` — ON_HOLD 标注 planned (Q5)
- [x] **Add** `domain-design-guidelines.md` — 外币重估规则 + 负库存标注（容错机制）(Q36/Q37)
- [x] **Add** `domain-design-guidelines.md` 或 `finance/posting.md` — 借贷方向约定五大类别 (Q40)

**退出标准**：
- [x] 所有全局设计文档更新完成
- [x] 会计期间状态机修正
- [x] ErrorCode 约定添加
- [x] 删除策略和期间统一规则添加
- [x] 文档间交叉引用检查通过

---

### Phase 2: 域设计文档更新 (21 项)

**Status**: completed
**Targets**: `docs/design/purchase/`, `docs/design/sales/`, `docs/design/finance/`, `docs/design/quality/`, `docs/design/manufacturing/`, `docs/design/assets/`, `docs/design/projects/`, `docs/design/maintenance/`, `docs/design/master-data/`, `docs/design/domain-design-guidelines.md`
**Skill**: none

**执行项目**：

- [x] **Fix** `purchase/state-machine.md` — 迁移图标注审批轴 + 撤销提交迁移 (Q1 Q3 Q20)
- [x] **Fix** `sales/README.md` — 补充信用额度规则 (Q21)
- [x] **Fix** `sales/state-machine.md` — 撤销提交迁移 (Q20)
- [x] **Fix** `finance/posting.md` — 补充 PostingEvent 契约 + 幂等检查 + 业务日期汇率 (Q4 Q14)
- [x] **Add** `finance/ar-ap-reconciliation.md` — 补充核销分摊 + 汇兑损益规则 (Q16)
- [x] **Fix** `finance/period-close.md` — 补充年度结转规则 (Q27)
- [x] **Add** `finance/opening-balance.md` — 期初余额导入方案 (Q17)
- [x] **Fix** `finance/costing-methods.md` — 展开全部四种计价方法（FIFO/移动平均/标准成本/个别计价）(Q29 Q42)
- [x] **Fix** `quality/state-machine.md` — NCR 财务影响规则 (Q25)
- [x] **Fix** `quality/` 补充质检-manufacturing 约束 — 双方显式声明 (Q6)
- [x] **Fix** `manufacturing/state-machine.md` — 质检对工单状态的约束声明 (Q6)
- [x] **Fix** `assets/depreciation-and-posting.md` — 折旧调度规则 (Q18)
- [x] **Fix** `projects/cost-collection.md` — 项目成本财务流向（资本化/费用化）(Q22)
- [x] **Fix** `maintenance/equipment-integration.md` — 备件消耗成本归属 (Q23)
- [x] **Fix** `master-data/sku-multi-unit.md` — 近似换算支持（单据行自定义系数）(Q28)
- [x] **Fix** `purchase/state-machine.md` + `sales/state-machine.md` — posted=true 物理锁定规则引用 (Q7)
- [x] **Fix** `domain-design-guidelines.md` §10 — 单据锁定规则（posted 保护）(Q7)
- [x] **Fix** `domain-design-guidelines.md` §10 — 单据编号策略升级为可配置模板 (Q44)
- [x] **Add** `master-data/README.md` — 主数据关键属性审核规则 + 可配置关键属性列表 (Q34 Q57)
- [x] **Add** `master-data/` 补充物料四档价格规则 — 可配置档位 + 默认最低价 + 折扣叠加 (Q39)
- [x] **Fix** `finance/costing-methods.md` — 成本核算方法扩展（FIFO/移动平均/标准成本/个别计价）(Q42)

**退出标准**：
- [x] 所有域设计文档更新完成
- [x] 状态机迁移图修正
- [x] 跨域约束声明补充
- [x] 业务规则文档化
- [x] 文档间术语一致性检查通过

---

### Phase 3: 新增架构文档 (10 项)

**Status**: completed
**Targets**: `docs/architecture/`, `docs/design/manufacturing/`, `docs/design/inventory/`
**Skill**: none

**执行项目**：

- [x] **Add** `docs/architecture/` 新增通知策略文档 — 三类通知 + 频控 (Q24)
- [x] **Add** `docs/architecture/` 新增 `approval-framework.md` — 按单据类型配置审批模式（NONE/DIRECT/WORKFLOW）(Q43)
- [x] **Add** `docs/architecture/` 新增 `multi-company.md` — Transfer pricing + 合并抵消 + 配置继承 (Q45)
- [x] **Add** `docs/architecture/` 新增 `tax-framework.md` — 可插拔税务引擎接口 + 中国增值税实现 (Q46)
- [x] **Add** `manufacturing/` 新增 `subcontracting.md` — 委外加工业务逻辑展开 (Q30)
- [x] **Fix** `manufacturing/bom-and-routing.md` — BOM 版本快照规则 (Q33)
- [x] **Fix** `inventory/cross-domain.md` — 在途库存规则 + 负库存会计处理 (Q31/Q36)
- [x] **Fix** `inventory/README.md` — 报废出库移动单（operationType=SCRAP）+ 批次选择策略（FIFO/FEFO）(Q32/Q38)
- [x] **Add** 各域 README 新增报告小节 — 报告目录 (Q66)
- [x] **Add** ORM 设计缺口补充 — 5 短文档 + 2 合并 + 7 已有覆盖 (Q71)

**退出标准**：
- [ ] 所有新增架构文档创建完成
- [ ] 通知策略文档化
- [ ] 审批框架文档化
- [ ] 多公司和税务框架文档化
- [ ] 文档格式和结构符合项目规范
- [ ] ORM 设计缺口补充完成

---

### Phase 4: 可配置点清单与审计 (5 项)

**Status**: completed
**Targets**: `docs/design/domain-design-guidelines.md`, `module-*/model/app-erp-*.orm.xml`
**Skill**: none

**执行项目**：

- [x] **Add** `domain-design-guidelines.md` 新增「可配置点清单」章节 — 统一列出全部配置项、默认值、可选值和作用域 (Q41)
- [x] **Fix** `domain-design-guidelines.md` §11.2 — 会计期间支持财年起始月配置 (Q47)
- [x] **Fix** `domain-design-guidelines.md` §10 — posted 锁定升级为三级可配（STRICT/ADMIN_OVERRIDE/TIME_WINDOW）(Q48)
- [x] **Fix** `domain-design-guidelines.md` 新增删除策略节 — 删除策略升级为按实体可配置 (Q49)
- [x] **Fix** ORM 字典审计 — 审计 10 个 orm.xml 的通用状态值一致性 (Q9)

**退出标准**：
- [x] 可配置点清单创建完成
- [x] 所有可配置策略升级文档化
- [x] ORM 字典审计完成
- [x] 配置项格式统一（名称、类型、默认值、可选值、作用域）

---

### Phase 5: 域特定可配置升级 (12 项)

**Status**: completed
**Targets**: `docs/design/finance/`, `docs/design/sales/`, `docs/design/inventory/`, `docs/design/purchase/`, `docs/design/manufacturing/`, `docs/design/quality/`, `docs/design/domain-design-guidelines.md`, `docs/design/master-data/`
**Skill**: none

**执行项目**：

- [x] **Fix** `finance/posting.md` — 过账模式升级为 ASYNC(默认) + SYNC 可配 (Q50)
- [x] **Fix** `finance/posting.md` — 汇率日期策略升级按公司可配 (Q51)
- [x] **Fix** `sales/README.md` — 信用额度校验升级为三级可配（SOFT_WARNING/SPECIAL_APPROVAL/HARD_BLOCK）(Q52)
- [x] **Fix** `inventory/README.md` — 负库存升级为按仓库三级枚举（ALLOW/WARN/BLOCK）(Q53)
- [x] **Fix** `purchase/state-machine.md` + `sales/state-machine.md` — 撤销提交策略升级为按单据类型可配 (Q54)
- [x] **Fix** `domain-design-guidelines.md` — 外币重估范围升级可配（MONETARY_ONLY/ALL/NONE）(Q55)
- [x] **Fix** `inventory/README.md` — 报废触发升级为按物料分类可配（MANUAL/AUTO_ON_NCR_CLOSE）(Q56)
- [x] **Fix** `manufacturing/bom-and-routing.md` — BOM 版本策略升级为按物料可配（LOCK_AT_CREATION/AUTO_UPGRADE）(Q58)
- [x] **Fix** `domain-design-guidelines.md` 新增期间节 — 期间独立性升级为 SHARED(默认) + INDEPENDENT 可配 (Q59)
- [x] **Fix** `quality/state-machine.md` — NCR 过账模式升级为按 NCR 类型可配（AUTO_POST/MANUAL_POST）(Q60)
- [x] **Fix** `master-data/README.md` — 主数据关键属性列表可配置 (Q57)
- [x] **Fix** `master-data/sku-multi-unit.md` — 计量单位换算严格度可配（RELAXED/STRICT）(Q61)

**退出标准**：
- [x] 所有域特定可配置升级文档化
- [x] 配置策略和默认值明确
- [x] 配置作用域清晰
- [x] 与可配置点清单（Phase 4）一致

---

### Phase 6: 集成与迁移策略 (6 项)

**Status**: completed
**Targets**: `docs/architecture/`, `docs/design/master-data/`
**Skill**: none

**执行项目**：

- [x] **Add** `docs/architecture/` 新增 `integration-pattern.md` — 外部 API 集成模式：webhook-only (Q64)
- [x] **Add** `docs/design/master-data/` 新增 `data-migration.md` — 数据迁移策略：三字段 + IMPORTED 状态 (Q65)
- [x] **Add** `docs/architecture/` 新增 `print-template.md` — 打印模板：ErpSysDocumentTemplate + nop-report (Q67)
- [x] **Add** `docs/architecture/` 新增 `job-scheduling.md` — 计划作业调度：nop-job + DAG 依赖 + 告警 (Q68)
- [x] **Add** `docs/architecture/` 新增 `seed-data.md` — 种子数据模块：app-erp-seed 独立模块 (Q69)
- [x] **Add** `docs/architecture/` 新增 `cross-domain-constraints.md` — 跨域集成约束：消弧事件 + @RefLink (Q70)

**退出标准**：
- [x] 所有集成与迁移策略文档创建完成
- [x] webhook 集成模式文档化
- [x] 数据迁移策略文档化
- [x] 打印模板和调度策略文档化
- [x] 跨域约束机制文档化

---

### Phase 7: 测试与文档补充 (5 项)

**Status**: completed
**Targets**: `docs/architecture/`, `docs/design/flow-overview.md`, `docs/design/manufacturing/`, `docs/design/finance/`
**Skill**: none

**执行项目**：

- [x] **Add** `docs/architecture/` 新增 `testing-strategy.md` — 测试策略：三层测试 + Nop 测试框架 (Q73)
- [x] **Fix** `flow-overview.md` §2 — 补充 quality/assets/maintenance 独立流程小节 (Q72)
- [x] **Fix** `manufacturing/subcontracting.md` + `flow-overview.md` — 统一术语为"外协"，补充双视角注释 (Q74)
- [x] **Fix** `finance/posting.md` — 补充预付/预收规则 + 单据支持 prepayment 标志 (Q75)
- [x] **Fix** `finance/costing-methods.md` — 明确成本层按批次分割规则 + FIFO 扣减顺序 + 拆分按比例分割 (Q76)

**退出标准**：
- [x] 测试策略文档创建完成
- [x] 流程覆盖补充完成
- [x] 术语统一完成
- [x] 预付/预收和批次成本规则文档化

---

### Phase 8: 横切关注点 (7 项)

**Status**: completed
**Targets**: `docs/design/domain-design-guidelines.md`, `docs/architecture/`, `docs/design/master-data/`, `docs/design/finance/`
**Skill**: none

**执行项目**：

- [x] **Add** `domain-design-guidelines.md` 新增「金额精度与舍入约定」节 — 金额精度业务可配 + 计算顺序硬约定 (Q77)
- [x] **Add** `docs/architecture/` 新增 `data-archival-strategy.md` — 数据归档策略：年度分区 + 分层存储 + 30 年法定保留 (Q78)
- [x] **Fix** `domain-design-guidelines.md` §10.1 — 时间戳与 businessDate 一律按公司时区存储 (Q79)
- [x] **Fix** `docs/architecture/approval-framework.md` — 补充委派规则 + 责任不转移 + 不可委派清单 (Q80)
- [x] **Add** `docs/design/master-data/` 新增 `exchange-rate-management.md` — 汇率主数据：日表 + FALLBACK 兜底 + 预警 + 已引用锁定 (Q81)
- [x] **Fix** `finance/period-close.md` — 补充辅助账（存货/AR/AP/资产）↔ 总账跨账对账 (Q82)
- [x] **Fix** `domain-design-guidelines.md` 或 `finance/posting.md` — 单据编号断号/跳号处理：业务号允许断号；凭证号过账后分配保连续 (Q83)

**退出标准**：
- [x] 金额精度与舍入规则文档化
- [x] 数据归档策略文档化
- [x] 时区处理规则文档化
- [x] 审批委派规则文档化
- [x] 汇率管理规则文档化
- [x] 跨账对账规则文档化
- [x] 凭证号连续性规则文档化

---

## Closure Gates

在设置 `Plan Status: completed` 之前，完成以下所有操作：

1. **验证所有文档更新**：检查每个修改/新增的文档是否准确反映了决议内容
2. **交叉引用一致性**：确保文档间的引用和术语一致
3. **状态机一致性**：验证所有状态机图和描述一致
4. **配置策略一致性**：确保所有可配置点有统一的描述格式
5. **运行文档检查**：`xmllint --noout` 检查所有 XML 文件格式正确
6. **Markdown 链接检查**：验证所有文档间的链接有效
7. **更新日志**：在 `docs/logs/2026/06-29.md` 中记录计划完成状态
8. **独立结束审计**：生成独立子代理进行结束审计直到通过

## Deferred But Adjudicated

以下决议经审查确认无需修改，不在本计划执行范围内：

- **Q35**：银行对账设计评审 — `finance/bank-reconciliation.md` 设计完整，无需修改
- **Q13**：nop-wf 集成 — 已在审批框架（Q43）中覆盖
- **Q26**：预算控制 — 已在 Q63 确认由单独模块处理

## Infrastructure And Config Prereqs

本计划无基础设施或配置依赖。所有工作均为文档层面更新，不涉及代码、数据库或部署配置变更。

## 风险与缓解

1. **风险**：文档更新可能引入新的不一致性
   **缓解**：每个阶段退出时进行交叉引用检查

2. **风险**：决议可能有歧义或冲突
   **缓解**：在实施前重新阅读相关决议，必要时澄清

3. **风险**：文档数量多，可能遗漏
   **缓解**：使用决议表格作为检查清单，逐项落实

4. **风险**：Phase 6-8 的新增文档可能与其他文档重复
   **缓解**：在创建前检查现有文档，确保不重复

## 资源

- 设计审查讨论文件：`docs/discussions/2026-06-29-1000-grill-with-docs-design-review.md`
- 计划指南：`docs/plans/00-plan-authoring-and-execution-guide.md`
- 项目上下文：`docs/context/project-context.md`
- 设计文档目录：`docs/design/`
- 架构文档目录：`docs/architecture/`
- 日志目录：`docs/logs/`

## Draft Review Record

| 日期 | 审查者 | 结果 | 行动 |
|------|--------|------|------|
| 2026-06-29 | AI 代理 | 草案创建 | 初始草案 |
| 2026-06-29 | 独立子代理 | 审查发现遗漏 Q64-Q83 | 重大修订，新增 Phase 6-8 |
| 2026-06-29 | 独立子代理 | 审查发现 9 项决议遗漏和格式问题 | 补充遗漏决议，调整格式符合模板 |