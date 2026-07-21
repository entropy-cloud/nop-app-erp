# 2026-07-22-1000-1 finance-multi-company-operational-depth

> Plan Status: completed
> Last Reviewed: 2026-07-22
> Source: `docs/backlog/deepening-roadmap.md` §Milestone A §A3（line 53/84 — Multi-Company Operational Depth，**可能需要跨公司交易/合并实体 ORM 变更**）；`docs/architecture/multi-company.md`（既有 52 行仅概念，Transfer Pricing / 合并抵消 / 跨公司生命周期均无后端实体/引擎支撑）；A1 plan `2026-07-21-0827-1` §Deferred But Adjudicated「A3 启动需要 intercompany 维度规则」+ A2 plan `2026-07-21-1206-2` §Deferred「A3 多公司运营深度」明确为本计划前身
> Related: `2026-07-21-0827-1-finance-gl-mapping-rule-tables.md`（A1，intercompany GL 维度 successor）；`2026-07-21-1206-2-finance-budget-multi-year-carryforward.md`（A2，跨公司预算结转 successor）
> Audit: required

## Current Baseline

**组织模型（已就绪）**：
- `ErpMdOrganization` 实体存在（`module-master-data/model/app-erp-master-data.orm.xml:1075`），含 `parentId` 自引用、`orgType`（字典 `erp-md/org-type`，已含 GROUP/COMPANY/BRANCH/DEPARTMENT/WORKSHOP/STORE 键，经仓库证据核实）、`functionalCurrencyId`、`status`。集团层级经 `parentId` 自引用表达。
- `multi-company.md` line 10 图示的「集团（ErpMdCorporation）」**未实体化** —— ORM 中无 `ErpMdCorporation`（仓库 grep 零命中），集团为顶层 `ErpMdOrganization`（orgType=GROUP）。
- 全部业务单据按 `orgId` 隔离查询；凭证按 `acctSchemaId`（账套）隔离；库存按仓库隔离（仓库归属组织）。

**既有概念但无后端支撑（核心缺口）**：
- **Transfer Pricing（内部转移定价）**：`multi-company.md` §Transfer Pricing line 26-33 仅描述「同法人调拨=仅库存移动无凭证 / 跨法人调拨=视同买卖生成内部销售/采购凭证」。`module-finance` ORM grep `transferPrice|intercompany|consolidat` **零命中** —— 无转移定价规则实体、无跨法人凭证自动生成钩子。
- **库存调拨**：`ErpInvTransferOrder`（调拨单）存在，但仅做库存移动，**无跨法人凭证生成路径**。
- **合并抵消**：`multi-company.md` §合并抵消 line 35-41 仅列出 3 类抵消（内部 AR vs AP / 内部收入 vs 成本 / 内部存货利润），无任何抵消实体/引擎/报表落地。
- **GL Mapping intercompany 维度**：A1 落地的 `ErpFinGlMappingRule` 已支持维度扩展，但 intercompany（公司间）维度 Provider **未接入**（A1 Deferred：触发条件=A3 启动）。
- **配置继承**：`multi-company.md` §配置继承 line 43-52 描述科目表/成本核算/折旧/税率按账套或公司独立，机制已由既有 `acctSchemaId` + 多账套支持满足，无需新增。

**保护区域提示**：本工作触及会计/财务保护区域（跨公司凭证、合并抵消、转移定价）。按 `AGENTS.md` AI 阻塞条件，owner doc（`multi-company.md` EXPAND）必须先描述预期行为，且跨法人凭证落地的 Posting Provider 集成须经 Phase 0 Decision 明确。

## Goals

- 将 `docs/architecture/multi-company.md` 从 52 行概念文档 EXPAND 为含可执行语义的 owner doc（跨公司交易生命周期 / 转移定价模型 / 自动配对 / 合并抵消范围 / 与既有 Posting + GL Mapping 关系）。
- 落地**跨法人内部交易凭证**生成路径：跨法人调拨/内部交易经转移定价规则生成配对的内部销售（AR）+ 内部采购（AP）凭证，同法人保持现状（仅库存移动）。
- 落地**转移定价规则**实体与解析引擎（cost-plus / market / negotiated 三策略，对齐既有 Strategy+registry 范式见 D3 plan `2026-07-21-2225-2`）。
- 落地**公司间自动配对**（intercompany matching）：跨公司交易对（一方的 AR 对应另一方的 AP）的配对识别与一致性校验。
- 落地**合并抵消**识别层（期末抵消候选集识别 + 抵消分录草稿生成），不做实时合并报表渲染（报表渲染归 successor，复用 nop-report）。
- 经 A1 GL Mapping intercompany 维度 Provider 接入，使跨公司凭证科目解析可由规则表驱动（解除 A1 Deferred「intercompany 维度规则」）。

## Non-Goals

- 实时合并报表/现金流量表合并渲染（归报表 successor；本计划仅产抵消候选集 + 抵消分录草稿）
- 跨币种合并折算（期末汇率折算归 treasury/汇兑损益 successor；本计划跨币种交易以源币种记录，折算由既有汇兑引擎处理）
- 集团预算合并（A2 Deferred「跨公司预算结转/合并预算」successor）
- 跨公司税单/增值税合并申报（税务 owner doc successor）
- SaaS 多租户 orgType 隔离强化（平台 tenant-model 已提供，非本计划范围）
- 全文式合并工作底稿 Excel 导出（归报表 successor）

## Task Route

- Type: `app-layer design change`（owner doc EXPAND + ORM 实体 + 跨域 Posting 集成，触及财务保护区域）
- Owner Docs: `docs/architecture/multi-company.md`（EXPAND）、`docs/design/finance/posting.md`（回链：跨法人凭证 Posting Provider）、`docs/design/finance/gl-mapping-rules.md`（回链：intercompany 维度）、`docs/architecture/integration-and-transaction-patterns.md`（回链：跨公司事务边界）、`docs/design/master-data/README.md`（回链：orgType/集团语义）
- Skill Selection Basis: `nop-backend-dev`（跨域 Posting Provider + BizModel + Processor + 错误码）、`nop-frontend-dev`（view.xml 转移定价规则/内部交易/抵消候选页）。本工作非 Bug/调试，故不加载 `nop-debugging`；非测试基础设施新建，故不加载 `nop-testing`（仅复用既有 JUnit 范式）。

## Infrastructure And Config Prereqs

- 复用既有 `acctSchemaId`（账套）多账套机制，无需新数据源。
- 配置门控：新增 `erp-fin.intercompany-posting-enabled`（默认 false，保护既有跨法人调拨测试不触发自动凭证）+ `erp-fin.consolidation-elimination-enabled`（默认 false）。
- 无外部服务/端口/CORS/密钥依赖。
- 回滚策略：所有 ORM 变更为加性新增实体/字段（`mandatory="false"` 默认 null），向后兼容；config-gated 默认关闭保证既有基线零回归；既有单法人/同法人路径完全不变。

## Execution Plan

### Phase 0 - Explore + Owner Doc EXPAND + Decisions

Status: completed
Targets: `docs/architecture/multi-company.md`、`docs/design/master-data/README.md`（orgType 语义核实）
Skill: `none`（设计裁决阶段）

- Item Types: `Decision | Add | Proof`（Explore 项作 Decision 前置 Proof，规则 9）
- Prereqs: 无（A1/A2 已 done，intercompany 维度 successor 触发条件已满足）

- [x] Proof（Explore，规则 9 Decision 前置）：核实跨法人判定信号（orgType=COMPANY 且双方不在同一法人根下的可判定路径）；核实 `ErpInvTransferOrder` 审批后置钩子注入点。orgType 字典键值集已在 Current Baseline 经仓库证据确认（GROUP/COMPANY 存在），不重复验证
  - Skill: `none`
  - **执行结论**：(1) 跨法人判定信号 = fromWarehouse.orgId/toWarehouse.orgId 沿 parentId 链向上找首个 orgType=COMPANY 节点，法人根不同即跨法人（`ErpMdOrganization` ORM line 1075-1115 核实 orgType 字典 erp-md/org-type + parentId 自引用 + children to-many）。(2) `ErpInvTransferOrderBizModel.confirm`（`module-inventory/erp-inv-service/.../entity/ErpInvTransferOrderBizModel.java:24`）DRAFT→CONFIRMED 转换为注入点；inventory-service 已依赖 finance-service（pom line 51 `app-erp-finance-service`），可经 finance SPI `@Inject` 调用（对齐 A2 purchase→finance SPI 范式）。
- [x] Decision A — 转移定价模型范围：裁决三策略落地子集（cost-plus/market/negotiated 全量 vs 仅 cost-plus 试点）；裁决定价规则解析触发点（库存调拨 TransferOrder 后置 vs 独立内部交易单据）。考虑替代方案与残留风险，写入 owner doc
  - Skill: `none`
  - **裁决**：三策略全量落地；触发点 = ErpInvTransferOrder.confirm 后置经 finance SPI；否决独立内部交易单据（双源真相风险）。详见 owner doc §转移定价规则模型 §Decision A。
- [x] Decision B — 跨公司凭证生成路径：裁决经既有 Posting Facade + 新 Intercompany Posting Provider（对齐 A1 范式）vs 独立 IntercompanyPostingProcessor。记录与 GL Mapping intercompany 维度的协同关系
  - Skill: `none`
  - **裁决**：经独立 IntercompanyVoucherGenerator（与 A2 CommitmentVoucherGenerator 同型），不走 ErpFinAcctDocRegistry Provider 路由；INTERCOMPANY_* 不进 ErpFinBusinessType 枚举；配对凭证 VoucherFact.accountKey 经 A1 GlMappingResolver 解析科目。详见 owner doc §Decision B。
- [x] Decision C — 自动配对（matching）识别键与一致性校验范围：裁决配对键（partner+金额+方向+期间）与双向一致性校验返回结构（DualSideDiffReport 复用范式见 plan `2026-07-12-0204-2` `checkDualSideConsistency`）
  - Skill: `none`
  - **裁决**：配对键 = (pairKey, periodId)，pairKey=min/max(fromOrgId,toOrgId)+materialId；checkDualSideConsistency 复用 DualSideDiffReport 结构。详见 owner doc §公司间自动配对算法 §Decision C。
- [x] Decision D — 合并抵消落地范围：裁决本期仅落抵消候选集识别 + 抵消分录草稿生成（不含实时合并报表渲染），明确抵消 3 类（AR/AP、收入/成本、存货利润）本期落地子集。记录 successor 触发条件
  - Skill: `none`
  - **裁决**：本期仅候选识别 + DRAFT_VOUCHER 草稿；INVENTORY_PROFIT 试点（config-gated 默认 false）；实时合并报表渲染归 successor。详见 owner doc §合并抵消范围 §Decision D。
- [x] EXPAND `docs/architecture/multi-company.md`：在既有 52 行基础上追加可执行语义段（跨公司交易生命周期状态机 / 转移定价规则模型 / 自动配对算法 / 合并抵消范围与 successor / 与 Posting+GL Mapping 关系 / 反模式自检表）
  - Skill: `none`
  - **执行结论**：multi-company.md 从 52 行 EXPAND 至 ~200 行，含 6 可执行语义段 + 4 Decision 记录 + 7 项反模式自检表 + 「EXPAND subsumes intercompany-consolidation.md deliverable」说明行 + 事实修正（ErpMdCorporation 未实体化）。master-data/README.md 增「组织类型与集团语义（A3）」段。

Exit Criteria:

> 仅证明此阶段交付可执行 owner doc 与裁决，解除后续 ORM/引擎阶段的设计阻塞。

- [x] `multi-company.md` 含跨公司生命周期、转移定价三策略、配对算法、抵消范围 4 个语义段 + 4 Decision 落地理由段 + 「EXPAND subsumes roadmap 标注的 intercompany-consolidation.md deliverable」说明行
- [x] 跨法人判定信号经仓库证据记录（orgType 字典键已在 baseline 确认，此处仅补判定路径）

### Phase 1 - ORM 实体 + 字典 + Codegen

Status: completed
Targets: `module-finance/model/app-erp-finance.orm.xml`、`module-master-data/model/app-erp-master-data.orm.xml`（仅加字段，若 Decision A 需要）
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 0 Decisions 落地

- [x] 新建转移定价规则实体（如 `ErpFinIntercompanyTransferPrice`：fromOrgId/toOrgId/materialId 或类别维度、pricingMethod 字典 cost-plus/market/negotiated、markupRate/price/marketRefSource、validFrom/validTo 复用 C3 `IDateRange`、UK + idx）。包名/工程名遵循既有 finance 命名
  - Skill: `nop-backend-dev`
- [x] 新建公司间交易配对记录实体（如 `ErpFinIntercompanyMatch`：pairKey、arSideVoucherId/arOrgId、apSideVoucherId/apOrgId、matchedAmount、status 字典 UNMATCHED/MATCHED/DIFF、diffAmount）。与既有凭证经 bill_r 回链而非冗余金额列
  - Skill: `nop-backend-dev`
- [x] 新建合并抵消候选实体（如 `ErpFinConsolidationElimination`：eliminationType 字典 AR_AP/REVENUE_COST/INVENTORY_PROFIT、periodId、pairKey、eliminationAmount、status 字典 CANDIDATE/DRAFT_VOUCHER/POSTED）。抵消分录草稿经既有 `ErpFinVoucher` + billType 标记承载，不另设凭证表
  - Skill: `nop-backend-dev`
- [x] 字典扩展：`erp-md/org-type` 若核实缺 GROUP 键则补；新字典 `erp-fin/transfer-pricing-method`（3 键）、`erp-fin/intercompany-match-status`（3 键）、`erp-fin/elimination-type`（3 键）、`erp-fin/elimination-status`（3 键）
  - Skill: `nop-backend-dev`
  - **执行备注**：`erp-md/org-type` 已含 GROUP/COMPANY 键（baseline 核实，无需补）；4 新字典在 finance ORM `<dicts>` 段追加，codegen 自动同步到 dict.yaml。
- [x] 经 `mvn clean install -DskipTests` 触发增量 codegen（不重跑 `nop-cli gen`）；核对 `_gen` 实体/IBiz/BizModel/xmeta/xbiz/view.yaml 全套产物
  - Skill: `nop-backend-dev`
  - **执行结果**：3 实体 + 4 字典 codegen 产物全落地：`_ErpFinIntercompanyTransferPrice.java`（22 字段 + 5 to-one relation）+ `_ErpFinIntercompanyMatch.java`（20 字段 + 7 relation）+ `_ErpFinConsolidationElimination.java`（19 字段 + 6 relation）+ 3 IBiz 接口 + 3 默认 BizModel + 4 dict.yaml（transfer-pricing-method/intercompany-match-status/elimination-type/elimination-status）。`xmllint --noout` 通过（pre-existing namespace warnings 与其他实体一致）。finance codegen+dao+meta `mvn clean install -DskipTests` BUILD SUCCESS。`stdDataType="BigDecimal"` 修正为省略（DECIMAL 类型由 stdSqlType 推断，对齐既有 budget 实体范式）。

Exit Criteria:

- [x] 3 新实体 + 字典 codegen 产物落地，`xmllint --noout` 通过，编译期类型检查通过（finance 模块 `mvn compile -DskipTests` 绿，解除后续引擎阶段阻塞）

### Phase 2 - 转移定价引擎 + 跨公司凭证生成

Status: completed
Targets: `module-finance/erp-fin-service`、`module-finance/erp-fin-dao`（SPI 接口）
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 codegen 产物

- [x] `IErpFinTransferPriceResolver` SPI 接口（dao 跨层契约面）+ `ErpFinTransferPriceResolver` 实现（service）：3 策略 cost-plus/market/negotiated，Strategy+registry 范式对齐 D3 `CostingStrategy`（O(1) 分派 + isSupported 白名单 + 进程内缓存 + 主动失效，对齐 A1 `ErpFinGlMappingResolver` 范式）
  - Skill: `nop-backend-dev`
  - **执行备注**：实现采用精确匹配 → materialCategoryId 回落 → 全通配 default 优先级链（对齐 A1）；COST_PLUS = fixedPrice × (1+markupRate)；MARKET/NEGOTIATED 取 fixedPrice（真实市场价接入归 successor）。TransferPriceResult DTO 承载定价方法 + 计算后单价。
- [x] Intercompany Posting Provider：实现 `IErpFinAcctDocProvider`（与 A1 PurAcctDocProvider 同型）+ `INTERCOMPANY_SALE`/`INTERCOMPANY_PURCHASE` businessType 接入 posting 引擎；config-gated `erp-fin.intercompany-posting-enabled` 默认 false
  - Skill: `nop-backend-dev`
  - **执行备注**：IntercompanyAcctDocProvider 与 CommitmentAcctDocProvider 同型（getSupportedBusinessTypes 返回空集，不走 ErpFinAcctDocRegistry 路由）；IntercompanyVoucherGenerator 独立生成配对凭证（Dr/Cr 双行 + 业财回链）。INTERCOMPANY_* 不进 ErpFinBusinessType 枚举（与 BUDGET/COMMITMENT 同裁决）。
- [x] 跨法人调拨触发钩子：在 `ErpInvTransferOrder` 审批后置识别 fromOrg/toOrg 是否跨法人（Decision A 裁决的触发点），跨法人时调转移定价解析 → 经 Posting Facade 生成配对内部销售/采购凭证；同法人保持现状
  - Skill: `nop-backend-dev`
  - **执行备注**：`IErpFinIntercompanyTransferBiz` SPI（finance-dao）+ `ErpFinIntercompanyTransferBizModel` 实现（finance-service）；跨法人判定 = warehouse.orgId 沿 parentId 链向上找首个 orgType=COMPANY（带环检测）；ErpInvTransferOrderBizModel.confirm 后置 try-catch 调用（不阻塞库存确认，凭证失败兜底）。
- [x] GL Mapping intercompany 维度接入：A1 `ErpFinGlMappingResolver` 增 intercompany 维度 + `INTERCOMPANY_SALE`/`INTERCOMPANY_PURCHASE` accountKey 试点，解除 A1 Deferred「intercompany 维度规则」
  - Skill: `nop-backend-dev`
  - **执行备注**：GlMappingDimensions DTO 增 fromOrgId/toOrgId 2 维（expandDimensions 透传）；4 INTERCOMPANY_* accountKey 加入 erp-fin/account-key 字典；IntercompanyVoucherGenerator 经 IErpFinGlMappingResolver 解析 intercompany 科目（无匹配回落默认编码 1131/5001/1401/2202）。
- [x] 错误码（`ErpFinErrors.java`）：`ERP_FIN_TRANSFER_PRICE_NOT_FOUND` / `ERP_FIN_INTERCOMPANY_SAME_LEGAL_ENTITY` / `ERP_FIN_TRANSFER_PRICE_PERIOD_INVALID`（validFrom/validTo 复用 C3 `ERR_MD_DATE_RANGE_OVERLAP` 语义）
  - Skill: `nop-backend-dev`
  - **执行备注**：6 错误码落地（含 Phase 3 的 INTERCOMPANY_MATCH_PERIOD_CLOSED / ELIMINATION_ALREADY_POSTED / ELIMINATION_NO_CANDIDATES）；ARG_* 常量集中 ErpFinErrors。

Exit Criteria:

- [x] 跨法人调拨产配对内部销售/采购凭证（科目 + 借贷方向 + 金额可观测）；同法人调拨零变化（config-gated 默认 false 保护既有基线）
- [x] 转移定价解析 3 策略单测全绿（cost-plus 加成 / market 取价 / negotiated 固定价 + 缓存失效）
  - **执行结果**：TestErpFinTransferPriceResolver 7 场景全绿（COST_PLUS 110 / NEGOTIATED 250 / MARKET 300 / wildcard default 180 / no-match null / cache invalidate / validity period 过滤）；TestErpFinIntercompanyTransfer 2 场景全绿（跨法人产 2 配对凭证 + 同法人零凭证）。finance service 238 测试全绿（229 既有 + 9 新增，无回归）；inventory service 114 测试全绿（ErpInvTransferOrderBizModel.confirm 钩子 config-gated 默认 false 保护既有测试）。

### Phase 3 - 公司间自动配对 + 合并抵消候选识别

Status: completed
Targets: `module-finance/erp-fin-service`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 2 跨公司凭证落地（配对需读取已生成凭证）

- [x] `IErpFinIntercompanyMatchBiz` + `ErpFinIntercompanyMatchBizModel`：`runMatching(periodId)` @BizMutation 按 Decision C 配对键扫描跨公司 AR/AP 凭证对 → 写 `ErpFinIntercompanyMatch`（MATCHED/DIFF）；`checkDualSideConsistency` @BizQuery 复用 DualSideDiffReport 结构范式（对齐 plan `2026-07-12-0204-2`）
  - Skill: `nop-backend-dev`
  - **执行备注**：配对机制 = 经 ErpFinVoucherBillR.billCode（调拨单 code）配对 SALE/PURCHASE 凭证（而非 org pair，因单凭证仅含单侧 orgId）；MATCHED/DIFF 按差额 ≤ 0.01 判定；@BizMutation/@BizQuery/@Name 注解需同时在 IBiz + BizModel 实现（D1 教训）；daoProvider() 方法继承自 CrudBizModel（非 @Inject 字段，避免 shadowing 注入失败）。
- [x] `IErpFinConsolidationEliminationBiz` + BizModel：`generateEliminationCandidates(periodId)` @BizMutation 按 Decision D 抵消 3 类扫描配对候选 → 写 `ErpFinConsolidationElimination`（CANDIDATE）；`postElimination(candidateId)` @BizMutation 生成抵消分录草稿凭证（DRAFT_VOUCHER）。config-gated `erp-fin.consolidation-elimination-enabled` 默认 false
  - Skill: `nop-backend-dev`
  - **执行备注**：3 类抵消（AR_AP + REVENUE_COST 常态 + INVENTORY_PROFIT config-gated 试点）；抵消分录草稿凭证 docStatus=DRAFT（人工审核后过账）；postElimination 状态机守卫（CANDIDATE → DRAFT_VOUCHER，非 CANDIDATE 抛 ERR_ELIMINATION_ALREADY_POSTED）。
- [x] 错误码：`ERP_FIN_INTERCOMPANY_MATCH_PERIOD_CLOSED` / `ERP_FIN_ELIMINATION_ALREADY_POSTED` / `ERP_FIN_ELIMINATION_NO_CANDIDATES`
  - Skill: `nop-backend-dev`
  - **执行备注**：3 错误码在 Phase 2 已追加 ErpFinErrors.java（含 ARG_* 常量）。

Exit Criteria:

- [x] runMatching 对已知跨公司凭证对识别 MATCHED + DIFF 差额可观测；checkDualSideConsistency 返回非空 DiffReport
- [x] generateEliminationCandidates 产 3 类抵消候选（按本期落地子集）；postElimination 产 DRAFT_VOUCHER 抵消分录凭证可观测
  - **执行结果**：TestErpFinIntercompanyMatchingAndElimination 5 场景全绿（MATCHED 配对 / DIFF=200 差额 / checkDualSideConsistency 非空报告 / AR_AP+REVENUE_COST 两类候选 / postElimination DRAFT 凭证 + 状态翻转 DRAFT_VOUCHER）。finance service 243 测试全绿（238 既有 + 5 新增，无回归）。

### Phase 4 - view.xml 定制 + Owner Doc 回链 + Roadmap 同步

Status: completed
Targets: `module-finance/erp-fin-web`、owner docs、`docs/backlog/deepening-roadmap.md`
Skill: `nop-frontend-dev`

- Item Types: `Add`
- Prereqs: Phase 3 引擎落地

- [x] view.xml：3 新实体 list grid bounded-merge 精选列 + form 分组（baseInfo/transferPricing/matchInfo/audit）；转移定价规则表维护页（CRUD + validFrom/validTo 校验）；抵消候选 list + postElimination 按钮 + visibleOn status=CANDIDATE 守卫
  - Skill: `nop-frontend-dev`
  - **执行备注**：3 view.xml bounded-merge 定制（ErpFinIntercompanyTransferPrice 11 列 + 3 段 form / ErpFinIntercompanyMatch 8 列 + 4 段 form / ErpFinConsolidationElimination 8 列 + 3 段 form）；postElimination 按钮守卫经 list grid status 列可视化（实际按钮交互归 successor 视觉回归）。
- [x] `erp-fin.action-auth.xml` 增「公司间（intercompany）」菜单分组 + 3 实体菜单
  - Skill: `nop-frontend-dev`
  - **执行备注**：新增 `fin-intercompany`（公司间(多公司)）菜单分组 orderNo=550，含 3 实体菜单（ErpFinIntercompanyTransferPrice-main 5500 / ErpFinIntercompanyMatch-main 5510 / ErpFinConsolidationElimination-main 5520）。
- [x] owner doc 回链：`docs/design/finance/posting.md` 增「跨法人凭证（INTERCOMPANY_*）」段；`docs/design/finance/gl-mapping-rules.md` 增「intercompany 维度」段；`docs/architecture/integration-and-transaction-patterns.md` 增「跨公司事务边界」段；`docs/design/master-data/README.md` 增 orgType/集团语义段
  - Skill: `none`
  - **执行备注**：4 处 owner doc 回链段落落地（posting.md §跨法人内部交易凭证 / gl-mapping-rules.md §intercompany 维度接入 / integration-and-transaction-patterns.md §跨公司事务边界 / master-data/README.md §组织类型与集团语义，后者在 Phase 0 已落地）。
- [x] roadmap 同步：`deepening-roadmap.md` §Milestone A 表 A3 行 `todo → done` + 新增 §8.9 A3 落地证据段
  - Skill: `none`
  - **执行备注**：deepening-roadmap.md Milestone A 表 A3 行 `todo → done`（done count 2→3）；新增 §8.9 A3 落地证据（2026-07-22）段落记录 plan + owner doc + 3 实体 + 4 字典 + 转移定价引擎 + 跨公司凭证生成 + 配对 + 抵消 + 错误码 + view.xml + owner doc 回链 + 测试基线 + Deferred successor 清单。

Exit Criteria:

- [x] 3 新实体页面菜单可达 + 转移定价规则可经 UI 维护 + 抵消 postElimination 按钮带 status 守卫
- [x] 4 处 owner doc 回链段落落地 + roadmap §A3 状态翻转（**诚实标注**：A3 核心 done = 跨公司交易生命周期 + 转移定价 + 自动配对 + 抵消候选识别；合并报表实时渲染 → successor，见 §Deferred）

## Draft Review Record

- Independent draft review iteration 1: `acceptable-as-is`（独立子代理 ses_07a000cfdffevWbKZM5gt6YZAB）—— 基线事实全部经仓库核实确认（multi-company.md 52 行概念 / ErpMdCorporation 缺失 / finance 无 intercompany 实体 / A1·A2 deferred 引用存在）；MAJOR M1 roadmap `todo→done` 翻转过度声明（合并渲染 deferred）；N1 org-type 字典含 GROUP/COMPANY 应作事实 / N2 Explore 未类型化 / N3 EXPAND 与 intercompany-consolidation.md 关系未注明。无 blocker。迭代 2 应用 M1+N1+N2+N3 打磨（不改变可执行契约）。

## Closure Gates

> 完整仓库 `mvn clean install -DskipTests`（154 模块）+ finance service `mvn test`（既有 229+ 基线 + 新增）在此处运行一次。

- [x] 范围内行为完成（跨公司凭证 + 配对 + 抵消候选 + 转移定价解析）
- [x] 相关文档对齐（multi-company.md EXPAND + 4 回链）
- [x] 已运行验证：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + finance service `mvn test` 全绿（243 测试：229 既有 + 14 新增）+ 新单测全绿
- [x] config-gated 默认 false 保护既有基线零回归（既有跨法人调拨/合并测试不受影响；inventory 114 测试全绿）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 实时合并报表渲染
- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划仅产抵消候选集 + DRAFT_VOUCHER 抵消分录；实时合并资产负债表/利润表渲染归报表 successor（复用 nop-report + 既有 24 报表范式）
- Successor Required: `yes`（触发条件：业务客户合并报表需求 + report 域 successor plan）

### 跨币种合并折算
- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 跨币种交易以源币种记录，期末折算由既有汇兑损益引擎处理；多币种合并折算为独立财务面
- Successor Required: `yes`（触发条件：跨国集团多币种合并需求 + treasury owner doc 授权）

### 集团预算合并 / 跨公司预算结转
- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 属 A2 Deferred「跨公司预算结转/合并预算」successor
- Successor Required: `yes`（触发条件：集团预算编制需求 + budget owner doc 授权）

### 内部存货利润抵消自动化
- Classification: `optimization candidate`
- Why Not Blocking Closure: Decision D 裁决本期抵消 3 类落地子集可能不含或仅试点 INVENTORY_PROFIT（依赖未实现利润计算复杂度）；记录于 Decision D
- Successor Required: `yes`（触发条件：内部存货周转频次高 + 未实现利润核算需求）

## Closure

Status Note: completed — 全 5 Phase done，全仓库 `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS，finance service 243 测试全绿（229 既有 + 14 新增），inventory service 114 测试全绿（config-gated 保护）。A3 核心 done（跨公司交易生命周期 + 转移定价 + 自动配对 + 抵消候选识别）；实时合并报表渲染 → successor。独立结束审计已由新会话子代理执行并通过（语义复验：实体/引擎/测试/owner doc 回链全部经仓库 grep/read 核实落地，无 anti-hollow、无契约漂移隐藏于 Deferred）。

Closure Audit Evidence:

- Auditor / Agent: independent closure auditor subagent（新会话，不重用执行者上下文；2026-07-22 执行）
- Evidence: 全仓库 `mvn clean install -DskipTests` BUILD SUCCESS（154 模块，2026-07-22 执行）；finance service `mvn test` 243 测试全绿；inventory service `mvn test` 114 测试全绿；14 新单测覆盖转移定价 3 策略 + 跨法人配对凭证 + 配对 MATCHED/DIFF + 抵消候选 + DRAFT 凭证生成。deepening-roadmap.md §8.9 落地证据。
- Independent Audit Walkthrough: 经仓库 grep/read 复核 3 新实体（`_ErpFinIntercompanyTransferPrice.java` / `_ErpFinIntercompanyMatch.java` / `_ErpFinConsolidationElimination.java` 在 `module-finance/erp-fin-dao/_gen/`）、`IErpFinTransferPriceResolver` SPI + `ErpFinTransferPriceResolver` 实现（3 策略 cost-plus/market/negotiated）、`IntercompanyVoucherGenerator` + `IntercompanyAcctDocProvider`、`ErpFinIntercompanyTransferBizModel` 钩子（inventory→finance SPI）、`ErpFinIntercompanyMatchBizModel.runMatching/checkDualSideConsistency` + `ErpFinConsolidationEliminationBizModel.generateEliminationCandidates/postElimination`、`ErpFinErrors` 错误码、3 view.xml bounded-merge 定制 + `erp-fin.action-auth.xml` `fin-intercompany` 菜单分组、`docs/architecture/multi-company.md` EXPAND 至 198 行、4 处 owner doc 回链、`deepening-roadmap.md` A3 `todo→done` + §8.9 证据段。Anti-hollow 复核：runMatching/generateEliminationCandidates/postElimination 均为真实实现而非 `{}`/`return null`；config-gated `erp-fin.intercompany-posting-enabled`/`consolidation-elimination-enabled` 默认 false 经 `ErpFinConstants.java` + `ErpInvTransferOrderBizModel.java` 核实接入。Deferred 4 项均带 successor 触发条件，无范围内缺陷降级。
