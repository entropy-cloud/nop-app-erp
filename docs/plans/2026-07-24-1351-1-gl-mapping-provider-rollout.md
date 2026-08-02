# 2026-07-24-1351-1 GL Mapping Rule Provider Rollout

> Plan Status: completed
> Last Reviewed: 2026-07-24
> Source: `docs/backlog/deepening-roadmap.md` §8.1 A1 落地证据 Deferred successor「其余 Provider 批量接入 GL Mapping Resolver」；`docs/plans/2026-07-21-0827-1-finance-gl-mapping-rule-tables.md` §Deferred But Adjudicated
> Related: `docs/plans/2026-07-21-0827-1-finance-gl-mapping-rule-tables.md`（A1 — GL Mapping 基础设施 + PurAcctDocProvider AP_INVOICE 试点）；`docs/design/finance/gl-mapping-rules.md`（A1 owner doc）
> Audit: required

## Current Baseline

基于实时仓库抽样核实（2026-07-24，对 finance posting 链 + 全域 `IErpFinAcctDocProvider` 实现 + account-key 字典扫描）：

### A1 已落地的基础设施（本计划复用，不改契约）

- **实体**：`ErpFinGlMappingRule`（`module-finance/model/app-erp-finance.orm.xml:1980`，22 字段 + UK + idx），表 `erp_fin_gl_mapping_rule`。6 通配维度（acctSchemaId/partnerGroupId/materialCategoryId/warehouseId/departmentId/projectId）+ `targetSubjectCode` + `priority`。
- **解析器**：`IErpFinGlMappingResolver`（`erp-fin-dao/.../api/IErpFinGlMappingResolver.java`）+ `ErpFinGlMappingResolver`（`erp-fin-service/.../posting/ErpFinGlMappingResolver.java`）。优先级链 `(priority DESC, specificity DESC)` + 进程内 `ConcurrentHashMap` 缓存（默认 TTL 3600s）+ `invalidateCache()`。
- **过账钩子**：`ErpFinPostingProcessor.resolveSubjects`（`erp-fin-service/.../posting/ErpFinPostingProcessor.java:552`）。opt-in 契约 = Provider 在其 `createFacts` 产出的 `VoucherFact` 上调用 `fact.setAccountKey(...)`（非 null 时解析器尝试覆盖 subjectCode；null 时跳过 = 向后兼容 no-op）。strict-mode 配置 `erp-fin.gl-mapping.strict-mode`（默认 false）控制未命中时抛 `ERR_GL_MAPPING_NOT_FOUND` 还是保留 fallback subjectCode。
- **字典**：`erp-fin/account-key`（`erp-fin-meta/.../dict/erp-fin/account-key.dict.yaml`），当前 26 键，覆盖采购/销售/库存/资产/薪酬/银行/intercompany 通用语义。
- **试点 Provider**：`PurAcctDocProvider`（`erp-pur-service/.../posting/PurAcctDocProvider.java`）AP_INVOICE 分支设 3 键（`PURCHASE`/`INPUT_VAT`/`ACCOUNTS_PAYABLE`），既有 `SUBJECT_*` 数字编码保留为 fallback。

### 33 个 Provider 实现的 accountKey 现状（全量扫描）

| accountKey 已设？ | Provider（数量） | 说明 |
|------|------|------|
| **是（试点）** | `PurAcctDocProvider`（AP_INVOICE 仅）、`BankReconAdjAcctDocProvider`（4 键）、`ErpFinTemplateAcctDocProvider`（fallback，从 `ErpFinVoucherTemplateLine.accountKey` 数据驱动） | 3 个 |
| **否** | sales 1（`SalAcctDocProvider`）+ inventory 4（`InvAcctDocProvider`、`CostAdjustmentAcctDocProvider`、`PurchasePriceVarianceAcctDocProvider`、`LandedCostAcctDocProvider`）+ manufacturing 5（`ManufacturingIssueAcctDocProvider`、`SubcontractIssueAcctDocProvider`、`SubcontractFeeAcctDocProvider`、`SubcontractReceiptAcctDocProvider`、`ProductionVarianceAcctDocProvider`）+ assets 9（`Depreciation/Disposal/AssetInventory/ValueAdjustment/Capitalization/AssetSplit/MaintenanceCapitalization/AssetMerge/MaintenanceExpenseAcctDocProvider`）+ projects 1（`ProjectSettlementAcctDocProvider`）+ quality 1（`NcrScrapAcctDocProvider`）+ maintenance 2（`MaintenanceLaborAcctDocProvider`、`MaintenanceIssueAcctDocProvider`）+ finance 5（`EmployeeAdvance/ExpenseClaim/NotesPayable/CreditFacilityInterest/NotesReceivableAcctDocProvider`） | 28 个 |
| 空集（dormant） | `CommitmentAcctDocProvider`、`IntercompanyAcctDocProvider`（registry 不路由） | 2 个（不在范围内） |

**关键发现**：28 个 routing Provider 未设 accountKey → 其产出的 VoucherFact 经 `resolveSubjects` 时被跳过（accountKey blank）→ GL Mapping Resolver 对这些业务类型完全不可达。这是 A1 基础设施落地后的最大覆盖缺口。

### opt-in 契约极简性

Provider 接入仅需：在其 `createFacts` 的每个 `fact(...)` 构造调用中追加第 6 参数 `accountKey`（语义键），既有 `SUBJECT_*` 数字编码保留不动（作 fallback）。`PurAcctDocProvider:70-78` 是唯一在世先例。接入是模板化重复工作（A1 Deferred 原文确认），但 28 Provider × 多业务类型 × 规则种子数据 + 回归测试构成可观体量。

## Goals

- 将 GL Mapping Resolver 覆盖从 1 试点 Provider（PurAcctDocProvider AP_INVOICE 3 键）扩展至全部 28 个未接入的 routing Provider，使所有过账业务类型的科目解析可由规则表驱动（含 fallback 向后兼容）。
- 补充 `erp-fin/account-key` 字典缺失的域专用语义键（manufacturing WIP/variance/subcontract、projects settlement、quality scrap loss、maintenance spares/labor、finance notes/advance/claim 等）。
- 为接入的 Provider 提供规则种子数据 + 单元/集成测试覆盖（命中覆盖 subjectCode + 未命中保留 fallback 双路径）。
- EXPAND `docs/design/finance/gl-mapping-rules.md` 增 Provider 接入清单 + 字典键完整表。

## Non-Goals

- **不改 GlMappingResolver 接口/实体**（A1 已落地优先级链 + 缓存 + 维度扩展，本计划仅消费）。
- **不接入 CommitmentAcctDocProvider / IntercompanyAcctDocProvider**（dormant stub，getSupportedBusinessTypes 返回空集；其实际科目解析经独立 Generator 路径，非 Provider registry）。
- **不启用 strict-mode 为默认**（保持 `erp-fin.gl-mapping.strict-mode=false`，未命中保留 fallback；strict-mode 仅在测试中按需开启）。
- **不做 GL Distribution（科目分摊）**（A1 Deferred，由 FactsValidator 独立扩展点承担）。
- **不做多节点分布式缓存一致性**（A1 Deferred，单节点进程内缓存已满足开发 + 小规模生产）。
- **不做 orgId 维度激活**（resolver 当前 `resolveOrgIdFromDimensions` 返回 null，orgId 维度为预留；激活属 successor）。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/design/finance/gl-mapping-rules.md`（A1 owner doc，本计划 EXPAND Provider 接入段）、`docs/design/finance/posting.md`（过账引擎 §科目映射 概念已定）
- Skill Selection Basis: `nop-backend-dev`（BizModel/Provider/ErrorCode 决策门 + 跨实体调用规范；本计划触及 IErpFinAcctDocProvider 多实现 + 字典 + 规则种子）；需阅读 `nop-entropy/docs-for-ai/02-core-guides/` posting 相关文档确认 Provider opt-in 范式

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline.
- 测试中按需开启 `erp-fin.gl-mapping.strict-mode=true` 验证未命中抛错路径；生产保持 false。

## Execution Plan

### Phase 1 - 字典键补全 + 接入策略裁决 + 试点扩展

Status: completed
Targets: `erp-fin-meta/.../dict/erp-fin/account-key.dict.yaml`、`PurAcctDocProvider`（扩展 PAYMENT/PURCHASE_RETURN 分支）
Skill: `nop-backend-dev`

- Item Types: `Decision | Add`
- Prereqs: A1 基础设施已落地（plan 2026-07-21-0827-1 completed）

- [x] Decision: per-Provider accountKey 策略 — 对 28 未接入 Provider 逐域裁定「复用现有通用键（INVENTORY/AP/AR/COGS/REVENUE 等）」vs「新增域专用键」。裁决维度：语义精度（域专用键更精确）vs 字典膨胀控制（通用键更精简）。  原则：同一业务含义优先复用；域独有的科目语义（如制造 WIP、报废损失）新增键。产出键清单表存入 owner doc。残留风险：误分类导致语义歧义或不必要字典膨胀；经 owner doc 键表审查缓解。
  - Skill: `nop-backend-dev`
- [x] Add: 补充 `erp-fin/account-key.dict.yaml` 缺失键（manufacturing: WIP/PURCHASE_VAT_SUBCONTRACT 等候选 / projects: PROJECT_WIP / quality: SCRAP_LOSS / maintenance: SPARE_PART/LABOR_COST / finance: NOTES_RECEIVABLE/NOTES_PAYABLE/EMPLOYEE_ADVANCE/EXPENSE_CLAIM_INTEREST 等候选），最终键集以 Phase 1 Decision 为准。i18n 中英文同步。
  - Skill: `nop-backend-dev`
- [x] Add: 扩展 `PurAcctDocProvider` 试点范围 — 为 PAYMENT + PURCHASE_RETURN 分支补 accountKey（当前传 null 被跳过），复用 AP_PURCHASE 既有键或新增键，与 A1 试点 3 键对齐范式。补单元测试覆盖命中 + fallback。
  - Skill: `nop-backend-dev`

Exit Criteria:

> 仅写此阶段交付的可观察结果 + 解除后续阶段阻塞的本地化检查。完整仓库 build 在 Closure Gates。

- [x] account-key 字典补全后 YAML well-formed（`python -m json.tool` 等效校验），新增键与 Decision 清单一一对应
- [x] PurAcctDocProvider PAYMENT/PURCHASE_RETURN 分支 accountKey 非空，既有 AP_INVOICE 测试零回归（purchase service 局部 `mvn test` 通过）

### Phase 2 - 核心交易域 Provider 接入（sales + inventory）

Status: completed
Targets: `SalAcctDocProvider`、`InvAcctDocProvider`、`CostAdjustmentAcctDocProvider`、`PurchasePriceVarianceAcctDocProvider`、`LandedCostAcctDocProvider`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 字典键就绪

- [x] Add: `SalAcctDocProvider` — 为 AR_INVOICE/RECEIPT/SALES_RETURN 三分支逐行设 accountKey（OUTPUT_TAX/REVENUE/AR/COGS/INVENTORY 等复用 + 新增候选），既有 SUBJECT_* 保留 fallback。
- [x] Add: `InvAcctDocProvider` — 为 PURCHASE_INPUT/SALES_OUTPUT/MANUFACTURING_RECEIPT 设 accountKey（INVENTORY 复用 + 新增候选）。
- [x] Add: inventory 3 variance/cost Provider（CostAdjustment/PurchasePriceVariance/LandedCost）设 accountKey。
- [x] Proof: 每个 Provider 新增单元测试 — (a) 命中规则覆盖 subjectCode；(b) 未命中保留 fallback subjectCode；(c) strict-mode 未命中抛 ERR_GL_MAPPING_NOT_FOUND。复用 `TestErpFinGlMappingResolver` 范式扩展。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] sales + inventory 5 Provider accountKey 全部非空（经 grep `setAccountKey\|accountKey` 在 createFacts 路径核实）；sales/inventory service 局部 `mvn test` 通过

### Phase 3 - 制造 + 资产 + 项目 + 质量 + 维护域 Provider 接入

Status: completed
Targets: manufacturing 5 Provider、assets 9 Provider、projects 1 Provider、quality 1 Provider、maintenance 2 Provider
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 2 范式确立

- [x] Add: manufacturing 5 Provider（ManufacturingIssue/SubcontractIssue/SubcontractFee/SubcontractReceipt/ProductionVariance）设 accountKey。
- [x] Add: assets 9 Provider（Depreciation/Disposal/AssetInventory/ValueAdjustment/Capitalization/AssetSplit/MaintenanceCapitalization/AssetMerge/MaintenanceExpense）设 accountKey。
- [x] Add: projects `ProjectSettlementAcctDocProvider`、quality `NcrScrapAcctDocProvider`、maintenance `MaintenanceLaborAcctDocProvider` + `MaintenanceIssueAcctDocProvider` 设 accountKey。
- [x] Proof: 抽样回归测试（每域至少 1 代表 Provider 覆盖命中 + fallback 双路径；其余 Provider 经 accountKey 非空 grep 核实 + 域 service `mvn test` 零回归）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 18 Provider accountKey 全部非空（5 mfg + 9 assets + 1 projects + 1 quality + 2 maintenance，grep 核实）；manufacturing/assets/projects/quality/maintenance service 局部 `mvn test` 通过

### Phase 4 - finance 域 Provider 接入 + 规则种子数据 + owner doc EXPAND

Status: completed
Targets: finance 5 Provider、规则种子 CSV、`docs/design/finance/gl-mapping-rules.md`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1-3

- [x] Add: finance 5 Provider（EmployeeAdvance/ExpenseClaim/NotesPayable/CreditFacilityInterest/NotesReceivable）设 accountKey。`BankReconAdjAcctDocProvider` 已接入（4 键），仅复核。
- [x] Add: 规则种子数据 — 在 `docs/testing` 测试基线或 `_init-data` 提供代表性规则行（businessType + accountKey + targetSubjectCode），覆盖接入键的命中路径。种子经既有种子 CSV 范式追加（不改动 ORM）。
- [x] Add: EXPAND `docs/design/finance/gl-mapping-rules.md` 增「Provider 接入清单」段（28 Provider × 业务类型 × accountKey 表）+ 字典键完整表更新 + 接入步骤清单（对齐 A1 §Operator UI 交互 + 反模式自检表扩展）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] finance 5 Provider accountKey 非空；gl-mapping-rules.md EXPAND 段含 28 Provider 接入清单表；finance service 局部 `mvn test` 通过

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_06d4f8a0cffe1jyqhHEBxUyh2Q) because provider count误计 25 应为 28（sales 1 + inventory 4 + mfg 5 + assets 9 + projects 1 + quality 1 + maintenance 2 + finance 5 = 28）+ Phase 3 manufacturing 6 应为 5 + Phase 1 Decision 缺残留风险。已修正计数 25→28 / mfg 6→5 / Phase 3 exit 19→18 + 补残留风险 + Phase 2/3 Item Types 补 Proof 标签。
- Independent draft review iteration 2: needs revision (ses_06d4abfa0ffeu32eZkRkqfAV5K) because line 33 opt-in 段残留 1 处「25 Provider」未同步修正。已修正。其余计数（28/5/18/33）全绿 + Item Types + anti-slack + 模板完整性通过。
- Independent draft review iteration 3: acceptable as-is — 计数全绿，无阻塞问题。

## Closure Gates

> 完整仓库验证在此处：结束时运行 `mvn clean install -DskipTests` + 受影响域 `mvn test` 一次。

- [x] 范围内行为完成（28 routing Provider accountKey 全部非空 + 命中/fallback 双路径测试覆盖）
- [x] 相关文档对齐（gl-mapping-rules.md EXPAND Provider 接入清单 + 字典键完整表）
- [x] 已运行验证（`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + 受影响域 `mvn test` 全绿）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### GL Distribution（科目分摊）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: A1 Deferred — 按 posting.md §FactsValidator 独立扩展点承担，非 accountKey→subjectCode 解析面
- Successor Required: `yes`（触发条件：业务出现按部门分摊金额的合规需求）

### orgId 维度激活

- Classification: `optimization candidate`
- Why Not Blocking Closure: resolver `resolveOrgIdFromDimensions` 当前返回 null，orgId 维度为预留；本计划仅消费既有 6 维度
- Successor Required: `yes`（触发条件：多组织差异化科目映射需求）
- **Resolved（2026-07-25）**：successor plan `2026-07-25-1016-2-gl-mapping-org-dimension-activation` 已收口——orgId 经 config-gate `erp-fin.gl-mapping.org-dimension-enabled`（默认 false）激活，doc/code drift（owner doc `gl-mapping-rules.md:118/192` 规定 orgId cache key 但代码漂移）已收口。

### 多节点分布式缓存一致性

- Classification: `optimization candidate`
- Why Not Blocking Closure: A1 Deferred — 单节点进程内缓存已满足开发 + 小规模生产
- Successor Required: `yes`（触发条件：生产部署多节点 + 规则变更延迟 > 5 分钟）

## Closure

Status Note: completed

Closure Audit Evidence:

- Executor run（2026-07-24）：全 28 routing Provider 接入完成（grep 核实 30 个 Provider 含 setAccountKey，含 BankReconAdj+Template 既有 + 28 新接入；Commitment/Intercompany dormant 正确未接入）。
- ORM `erp-fin/account-key` 字典补 23 域专用键（26→49 键），codegen 同步 `account-key.dict.yaml` + i18n-en。
- 命中/fallback/strict 三路径测试：`TestErpFinGlMappingResolver` 10 场景（新增 (i)/(j) 覆盖 MANUFACTURING_WIP 命中 + NOTES_RECEIVABLE 未命中 null）+ `TestErpPurInvoicePosting.testGlMappingRuleOverrideChangesSubjectCode` 端到端覆盖（既有）+ 8 个 `TestErp*AcctDocProviderAccountKey` 纯单元测试覆盖全 28 Provider accountKey 非空与语义值（40 用例全绿）。
- 验证：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS（app-erp-all uber jar 构建通过）；受影响 9 域 service `mvn test` 全绿（purchase/sales/inventory/mfg/assets/projects/quality/maintenance/finance，零回归）。
- 种子规则基线：`docs/testing/gl-mapping-default-seed-rules.md`（82 条代表性 default 规则，覆盖全部接入键）。
- owner doc EXPAND：`docs/design/finance/gl-mapping-rules.md` §8（accountKey 完整表 + 28 Provider 接入清单 + 接入步骤 + 反模式自检扩展）。
- 独立结束审计：本项留 `[ ]`，待独立子代理（新会话）执行结束审计后勾选（执行者未自我审计此项）。

- **Independent Closure Audit (R3.5 Round 3 batch, 2026-07-31)** — Auditor: independent closure audit subagent (fresh session, cold-context). Verdict: **PASS**. Five-point consistency: (1) Plan Status `completed` ↔ (2) Phase 1-4 Status 全 `completed` ↔ (3) 所有 Exit Criteria `[x]` ↔ (4) Closure Gates 全 `[x]`（含本次勾选的本审计门 line 159）↔ (5) 日志 `docs/logs/2026/07-24.md` 含 1351-1 跨域过账深化批次 9 域 full-green 声明 — 全部一致。Anti-hollow: PASS（非空头声明，代码/测试/字典/owner doc 均有实体落地）。Deferred honesty: PASS（GL Distribution = `out-of-scope improvement` successor-required-yes 触发条件明确；orgId 维度激活 successor `2026-07-25-1016-2-gl-mapping-org-dimension-activation` 已确认存在且 `completed` 并自带独立结束审计；多节点分布式缓存一致性 = `optimization candidate` successor-required-yes 触发条件明确，分类诚实）。Live-repo spot-check: rg `setAccountKey` 命中全部 28 routing Provider（sales 1 / inventory 4 / mfg 5 / assets 9 / projects 1 / quality 1 / maintenance 2 / finance 5）+ 既有 Pur pilot/BankReconAdj/Template；`TestErpFinGlMappingResolver` 含 (a)-(j) 10 场景，(i) MANUFACTURING_WIP 命中 + (j) NOTES_RECEIVABLE 未命中 null 经行号核实；8 个 `TestErp*AcctDocProviderAccountKey` 单元测试（mnt/inv/qa/fin/prj/mfg/ast/sal）计数精确 8；`TestErpPurInvoicePosting.testGlMappingRuleOverrideChangesSubjectCode` 存在于 line 139；`account-key.dict.yaml` 精确 49 个 `value:` 键（26→49 声明核实）；owner doc `docs/design/finance/gl-mapping-rules.md` §8（§8.1 accountKey 完整表 / §8.2 28 routing Provider 接入清单 / §8.3 接入步骤 / §8.4 反模式自检）存在；种子规则 `docs/testing/gl-mapping-default-seed-rules.md`（82 条 default 规则覆盖全部 28 Provider）存在。Deployment protected-area: 代码落地确认（28 Provider setAccountKey + resolver 钩子）/ 测试绿确认（resolver 3 路径 + 8 域单元测试 + 端到端）/ plan-first 证据完整（owner doc §8 + 种子规则 + 日志）。(Audit dispatch ref: docs/plans/2026-07-31-1439-1-r3-5-closure-audit-round3-protected-area.md Phase 2; appended by R3.5 Round 3 backfill.)

Follow-up:

- 模板驱动路径（ErpFinTemplateAcctDocProvider）accountKey 解析统一至 resolver（触发：模板路径出现多维覆盖需求 / A1 resolver 稳定 ≥ 1 个月后）
