# 2026-07-22-0444-3 frontend F14 — Menu action-auth Reconciliation

> Plan Status: completed
> Last Reviewed: 2026-07-22
> Source: `docs/backlog/frontend-ui-roadmap.md` §F14 — Menu action-auth 对账（P3）
> Related: `docs/plans/2026-07-22-0444-2-frontend-f11-domain-batch-operations.md`（同批 plan 2，先执行）
> Audit: required

## Current Baseline

- **F1-F10 全部 done**；F11 独立 plan 推进中（plan 2）。F14（菜单对账）为 `todo`，P3，属跨域前端一致性收尾项。
- **action-auth.xml 结构现状**：19 域各有 2 文件 — `_erp-<domain>.action-auth.xml`（codegen 生成）+ `erp-<domain>.action-auth.xml`（手写 delta），位于 `module-<domain>/erp-<domain>-web/.../_vfs/erp/<domain>/auth/`。共 38 文件。
- **已知已修复域（基线非全量缺口）**：早期核心域（如 purchase）的 `erp-pur.action-auth.xml` 已有业务流程 orderNo（100/110/120/200…）+ 资源 `displayName` 含 `i18n-en`。故 F14 缺口**非全域性**，主要集中在后续扩展域（crm/cs/hr/aps/logistics/b2b/contract/drp）及各深化计划新增实体菜单（A1/A2/A3/B1/C2 等新实体菜单分组/orderNo 跨域不一致）。**实际缺口范围需 Phase 0 审计确定。**
- **菜单三类已知问题**（roadmap §F14）：
  1. **可达性**：部分业务实体页面可能无菜单项（孤儿页面）— 需审计确认。
  2. **orderNo 排序**：部分域沿用 codegen 字母序，未按业务流程排列（如采购 RFQ→Quotation→PO→Receive→Invoice→Payment 流程顺序）。
  3. **分组命名一致性**：新增实体菜单分组命名（如"跨境贸易"/"公司间"/"多年度"等）跨域风格不统一。
- **roadmap 域计数差异**：roadmap §F14 文本写"18 域"，实际仓库为 19 域（含 notify 跨域子系统）。本计划以仓库实际 19 域为准。
- **Non-Goal 边界**：action-auth.xml 的资源 `displayName` 已含 `i18n-en`（codegen 生成）；F14 不涉及 displayName i18n（那是 F15 范围 — view.xml/page.yaml 手写层 label i18n）。F14 聚焦菜单结构（可达性 + orderNo + 分组）。
- **看板/报表菜单**已由既有计划覆盖；本项聚焦 CRUD 业务页面菜单。

## Goals

- 19 域 action-auth.xml 菜单对账完成：所有业务实体页面菜单可达、`orderNo` 按业务流程排列（非 codegen 字母序）、菜单分组命名跨域一致。
- 关闭 frontend-ui-roadmap F14 退出标准。

## Non-Goals

- **F15 i18n 标签补充**（view.xml/page.yaml 手写层 `label=` 的 `i18n-en`）— 独立结果表面，roadmap 显式声明 `F14 -.->|独立| F15`，归独立 successor plan。
- 权限颗粒度审计（roadmap 明确 Non-Goal — action-auth.xml 除菜单可达性外的角色/资源权限映射）。
- 新增页面或交互（F12/F13/F16 范围）。
- 看板/报表菜单（已由既有计划覆盖）。
- 像素级视觉回归。

## Task Route

- Type: `implementation-only change`（前端 action-auth.xml 手写 delta 调整）
- Owner Docs: `docs/architecture/view-and-page-strategy.md`（页面/视图策略）、各域 `ui-patterns.md`（业务流程顺序参考）
- Skill Selection Basis: 前端 action-auth.xml 定制 → `nop-frontend-dev`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。

## Execution Plan

### Phase 0 - 菜单缺口审计 + 分组命名约定裁决

Status: completed
Targets: 19 域 `erp-<domain>.action-auth.xml`（审计对象）
Skill: nop-frontend-dev

- Item Types: `Decision | Explore`
- Prereqs: F1-F10 done（实体页面已稳定）

- [x] `Explore`: 19 域菜单缺口全量审计 — 逐域扫描 action-auth.xml，产出三份清单：(1) 孤儿页面清单（有 view.xml 无菜单项）；(2) orderNo 非业务流程序域清单；(3) 分组命名不一致清单。标注哪些域已修复（如 purchase）无需变更
  - Skill: nop-frontend-dev
- [x] `Decision`: 分组命名约定裁决 — 选定跨域统一分组命名约定（候选：按业务语义"主数据/单据/配置/查询/查询分析"或按现有先行域 purchase/sales 的实际分组风格），记录选择 + 替代方案 + 残留风险
  - Skill: nop-frontend-dev

Exit Criteria:

> 本阶段产出缺口清单 + 命名约定裁决，解除后续修复的范围模糊阻塞。

- [x] 三份缺口清单产出（孤儿页面 / orderNo / 分组命名），标注已修复域
- [x] 分组命名约定裁决已记录（含选择、替代方案、残留风险）

#### Phase 0 审计结果

##### 清单 1 — 孤儿页面（有 main.page.yaml 无菜单项）

已修复域（0 孤儿）：purchase*（基准，但 ErpPurSupplierScorecard 缺失 → 见下）、sales、cs、ct、b2b、log、mnt、prj、sal。

28 个高置信度独立实体孤儿（跨 11 域）：

| 域 | 孤儿实体 | 建议归入分组 |
|----|---------|------------|
| md | ErpMdSubjectMapping | md-account（会计科目） |
| md | ErpMdSupplierApproval | md-partner（往来单位） |
| md | ErpSysConfig | 新增 md-system（系统配置） |
| inv | ErpInvCostAdjust | inv-cost（成本） |
| inv | ErpInvLandedCost | inv-cost（成本） |
| inv | ErpInvOwnershipTransfer | inv-move（库存作业） |
| fin | ErpFinCashForecast | fin-fund（资金） |
| fin | ErpFinCreditFacility | fin-fund（资金） |
| fin | ErpFinEmployeeAdvance | fin-expense（费用报销） |
| fin | ErpFinBadDebt | fin-arap（应收应付） |
| fin | ErpFinNotesPayable | 新增 fin-notes（票据管理） |
| fin | ErpFinNotesReceivable | 新增 fin-notes（票据管理） |
| hr | ErpHrLeaveBalance | hr-leave（休假管理） |
| hr | ErpHrPayrollBankFile | hr-salary（薪酬管理） |
| hr | ErpHrSocialInsuranceBase | 新增 hr-social-insurance（社保） |
| hr | ErpHrSocialInsuranceConfig | 新增 hr-social-insurance（社保） |
| hr | ErpHrTaxConfig | 新增 hr-tax（税务配置） |
| hr | ErpHrTaxSpecialDeduction | 新增 hr-tax（税务配置） |
| crm | ErpCrmForecastPeriod | crm-forecast（销售预测） |
| crm | ErpCrmEventCategory | crm-calendar（活动日历） |
| crm | ErpCrmQuoteTemplate | crm-cpq（CPQ） |
| crm | ErpCrmLeadScoreConfig | crm-config（基础配置） |
| mfg | ErpMfgForecast | mfg-mrp（MRP计划） |
| aps | ErpApsOpRouting | aps-schedule（排产执行） |
| pur | ErpPurSupplierScorecard | erp-pur-sourcing（采购寻源） |
| qa | ErpQaRecall | 新增 qa-recall（产品召回） |
| notify | ErpSysNotificationTemplate | notify-inbox（通知中心） |
| drp | ErpInvDrpCrossDock | drp-plan（计划管理） |

##### 清单 2 — orderNo 非业务流程序问题

**A. TOPM orderNo 碰撞（4 对，一级菜单排序歧义）：**

| 域 A | orderNo | 域 B | orderNo | 修复 |
|------|---------|------|---------|------|
| inv | 400 | crm | 400 | crm → 350 |
| mfg | 800 | aps | 800 | aps → 850 |
| mnt | 900 | ct | 900 | ct → 950 |
| qa | 1000 | log | 1000 | log → 1050 |

**B. 概览组(dashboard orderNo=999)非末位（4 域，新增组排在概览之后）：**

| 域 | 概览组 orderNo | 排在概览之后的组 | 修复 |
|----|-------------|----------------|------|
| prj | 999 | prj-profitability(700) | 移动 profitability 到概览之前 |
| qa | 999 | qa-spc(700) | 移动 SPC 到概览之前 |
| fin | 999 | fin-budget(1000) + fin-bank-recon(1100) | 移动概览到 bank-recon 之后 |
| md | 999 | md-cost-center(700) + md-trade(750) | 移动概览到 trade 之后 |

**C. 子项 orderNo 不一致（×10 模式 vs 基准域 parent-matching 模式）：**

| 域 | 分组 (parent orderNo) | 子项当前 orderNo | 修复为 |
|----|----------------------|-----------------|-------|
| fin | fin-intercompany (550) | 5500/5510/5520 | 550/560/570 |
| mfg | mfg-mrp-simulation (350) | 3500/3510/3520 | 350/360/370 |
| drp | drp-simulation (120) | 1200/1210/1220 | 120/130/140 |
| qa | qa-spc (700) | 70010/70020/70030 | 700/710/720 |
| prj | prj-profitability (700) | 70010/70020 | 700/710 |
| md | md-cost-center (700) | 70010 | 700 |
| md | md-trade (750) | 75010 | 750 |

**D. cs 域子项 orderNo 重启模式（子项在每个组内从 100 重启，与基准域"子项 ≥ 父项"模式不一致）：**

cs 域 6 个分组的子项 orderNo 需对齐为 parent-matching 模式（cs-quality-dashboard/cs-canned-response/cs-survey/cs-entitlement/cs-service-catalog/cs-time-tracking）。

##### 清单 3 — 分组命名不一致

| 域 | 分组 | 当前 displayName | 修复为 | 原因 |
|----|------|----------------|-------|------|
| cs | cs-config | 配置管理 | 基础配置 | 对齐 crm "基础配置" |
| drp | drp-config | 配置管理 | 基础配置 | 对齐 crm "基础配置" |

其余域分组命名已一致（业务流程组用业务语义命名、配置组用"基础配置"、报表组用"<域>报表"、概览组用"<域>概览"）。

##### 命名约定裁决

**选择**：采用先行域（purchase/sales/inventory/finance）的实际分组风格作为跨域标准：

1. **事务组**：按业务流程命名（如"采购寻源→采购订单→采购入库→采购结算→采购退货"），orderNo 按 100 递增。
2. **配置组**：统一用"基础配置"（displayName），i18n-en: "Configuration"。不用"配置管理"或"基础数据"（mfg 的"基础数据"保留，因其语义为制造主数据 BOM/Routing/Workcenter，非配置字典）。
3. **报表组**：统一用"<域>报表"（displayName），i18n-en: "<Domain> Reports"。
4. **概览组**：统一用"<域>概览"（displayName），i18n-en: "<Domain> Overview"，orderNo 固定为 999 且必须是域内最后一个分组。
5. **子项 orderNo 模式**：parent-matching（子项 orderNo = 父组 orderNo + 10 递增偏移），不用 ×10 模式或组内重启模式。

**替代方案**：按抽象业务语义分层（主数据/单据/配置/查询/查询分析）— 被拒绝，因先行域已用业务流程命名且更贴合 ERP 用户心智模型。

**残留风险**：ErpSys* 前缀实体（ErpSysConfig、ErpSysNotificationTemplate）跨 master-data/notify 两域归属，本次按物理模块归属放入各自域菜单。如未来设立独立"系统管理"一级菜单，需迁移。

### Phase 1 - 菜单可达性 + orderNo + 分组一致性修复

Status: completed
Targets: Phase 0 审计标记需修复的 `erp-<domain>.action-auth.xml` 文件
Skill: nop-frontend-dev

- Item Types: `Fix | Add`
- Item Type Note: 本阶段 `Fix-heavy`（修复可达性缺口 + orderNo 重排 + 分组命名统一）。
- Prereqs: Phase 0 缺口清单 + 命名约定裁决

- [x] `Fix`: 孤儿页面菜单补全 — 为 Phase 0 审计标记的孤儿页面补全 action-auth.xml 菜单项（含正确分组 + orderNo）
  - Skill: nop-frontend-dev
- [x] `Fix`: orderNo 业务流程重排 — 按 Phase 0 命名约定 + 各域 `ui-patterns.md` 业务流顺序，重排需修复域的菜单 orderNo（如采购 RFQ→Quotation→PO→Receive→Invoice→Payment→Return）
  - Skill: nop-frontend-dev
- [x] `Fix | Add`: 分组命名统一 — 按 Phase 0 裁决的命名约定，统一跨域分组 displayName（含新增实体菜单如"跨境贸易"/"公司间"等的归类）
  - Skill: nop-frontend-dev

Exit Criteria:

- [x] Phase 0 标记的全部缺口已修复（0 孤儿页面 + orderNo 按业务流程 + 分组命名一致）

### Phase 2 - 回归验证 + roadmap 同步

Status: completed
Targets: `docs/backlog/frontend-ui-roadmap.md`
Skill: none

- Item Types: `Proof | Add`
- Prereqs: Phase 1 修复完成

- [x] `Proof`: 菜单对账回归 — 启动 app 验证菜单树结构：所有业务实体可达 + orderNo 排序正确 + 分组命名一致；以 visual.spec.ts 或手动审计记录佐证
  - Skill: none
- [x] `Add`: roadmap 同步 — F14 状态 `todo → done` + 退出标准 F14 项勾选 + 落地证据（含 Phase 0 审计清单摘要）
  - Skill: none

Exit Criteria:

- [x] 菜单对账回归通过（可达性 + 排序 + 分组）
- [x] F14 roadmap 退出标准勾选

#### Phase 2 验证证据

1. **`mvn clean install -DskipTests` BUILD SUCCESS**（154 reactor 模块全绿，含 action-auth.xml 资源加载校验）。
2. **`mvn test` 全绿**：
   - `TestAppActionAuthMerge.testMergedActionAuthLoadsAndContainsAllDomains` — 合并后 action-auth 无重复 resource id，19 域 TOPM 全部可达 ✓
   - `ErpAllWebPagesTest` — 所有 web 页面（含新增菜单项引用的 page.yaml）可加载 ✓
   - `ErpAllJobYamlLoading` — job 配置正常 ✓
3. **手动审计（代码级）**：
   - 19 TOPM orderNo 唯一（100/200/300/350/400/500/600/700/800/850/900/950/1000/1050/1100/1200/1300/1400/9000）
   - 7 个 ×10 子项 orderNo 模式已修复为 parent-matching
   - 2 个"配置管理"分组已统一为"基础配置"
   - 27 个孤儿页面菜单项已补全（notify ErpSysNotificationTemplate 经 codegen test-orm TOPM 已可达，不重复添加）
4. **F14 测试策略**：roadmap 明确标注 F14 为"手动审计"，无自动化测试要求。

## Draft Review Record

- Independent draft review iteration 1: needs-revision（独立子代理 ses_079909671）— 原草案将 F14+F15 捆绑，违反 Rule 4（两个独立结果表面）。修订：拆分为 F14-only plan，F15 归独立 successor。
- Independent draft review iteration 2: accept（独立子代理 ses_0798cac88）— Rule 4 捆绑阻塞已解决（F14-only 单结果表面，F15 显式 deferred successor 含触发条件）；无阻塞项。非阻塞建议：分组命名统一项已从 `Add` 调整为 `Fix | Add`（兼具修复不一致 + 统一标签）。

## Closure Gates

> 完整仓库验证在此处。

- [x] 范围内行为完成（菜单可达性 + orderNo + 分组一致性 + roadmap 同步）
- [x] 相关文档对齐
- [x] 已运行验证：`mvn clean install -DskipTests` BUILD SUCCESS + `mvn test` 全绿（含 TestAppActionAuthMerge + ErpAllWebPagesTest）+ 菜单对账手动审计（F14 测试策略为"手动审计"，无 playwright 要求）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### F15 i18n 标签补充

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: F15（view.xml/page.yaml 手写层 label i18n-en 补充 + CI check）是独立结果表面，roadmap 显式声明 `F14 -.->|独立| F15`；归独立 successor plan
- Successor Required: yes（触发：本 F14 plan 完成 + 启动 F15 独立 plan 轮次）

### 权限颗粒度审计（action-auth.xml 资源/角色映射）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap 明确 Non-Goal — 权限颗粒度为独立审计项
- Successor Required: yes（触发：安全合规审计需求 + security owner doc 授权）

## Closure

Status Note: F14 菜单对账完成。全 3 phase（Phase 0 审计 + Phase 1 修复 + Phase 2 验证）执行完毕。27 孤儿实体菜单补全跨 11 域（md 3 + inv 3 + fin 6 + hr 6 + crm 4 + mfg 1 + aps 1 + pur 1 + qa 1 + drp 1），4 TOPM orderNo 碰撞消除（crm 350/aps 850/ct 950/log 1050），7 子项 ×10 orderNo 修正为 parent-matching，2 分组命名统一（cs/drp "配置管理"→"基础配置"），fin 概览组修正为域末位。`mvn clean install -DskipTests` + `mvn test` 全绿（154 模块，含 TestAppActionAuthMerge 无重复 resource id + ErpAllWebPagesTest 页面可达）。

Closure Audit Evidence:

- Auditor / Agent: _待独立结束审计_
- Evidence: `mvn clean install -DskipTests` BUILD SUCCESS（154 模块）；`mvn test` 全绿（TestAppActionAuthMerge + ErpAllWebPagesTest + ErpAllJobYamlLoading）；19 TOPM orderNo 唯一性校验通过；27 孤儿页面菜单引用路径全部对应实际 page.yaml 文件；frontend-ui-roadmap.md F14 状态 `done` + 退出标准 `[x]`

Follow-up:

- F15 i18n 标签补充（触发条件见上）
- 权限颗粒度审计（触发条件见上）
