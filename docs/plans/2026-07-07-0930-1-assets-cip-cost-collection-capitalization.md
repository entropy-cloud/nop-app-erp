# 2026-07-07-0930-1 assets-cip-cost-collection-capitalization

> Plan Status: completed
> Last Reviewed: 2026-07-07
> Source: `docs/backlog/extended-roadmap.md` 工作项 2.5（UC-AST-06 在建工程转固，仍 `partial`；`extended-roadmap.md:12` 明示「UC-AST-06 CIP 转固处理仍 todo」）
> Related: `2026-07-02-1000-2-assets-depreciation-disposal-capitalization.md`（折旧/处置/直接购置前置；其落地了资本化侧 sourceType=CIP(20) 凭证路径但**不**含 CIP 自身成本归集与完工触发——该面超出 1000-2 范围，roadmap 显式将剩余 CIP 子流程归本计划）、`2026-07-05-0540-3-assets-impairment-revaluation.md`（资产减值/重估面，与 CIP 互补）
> Audit: required

## Current Baseline

- **CIP 头实体存在但缺子实体**：`ErpAstCip`（`module-assets/model/app-erp-assets.orm.xml:661`）已物化（code/name/categoryId 预定资产类别/currencyId/businessDate 开工日期/estimatedCompletionDate/accumulatedCost/isCompleted/completedAssetId/status/posted 三件套/exchangeRate+amountSource+amountFunctional 多币种四件套/UK_AST_CIP_CODE_ORG）。但 `grep ErpAstCipCostItem|ErpAstCipProgressBilling module-assets/model` 返回 **0 命中** —— owner 设计 `docs/design/assets/cip.md` §边界与 ORM 实体引用声明的「ErpAstCipCostItem、ErpAstCipProgressBilling」**未物化**。
- **CIP BizModel 为空壳**：`module-assets/erp-ast-service/src/main/java/app/erp/ast/service/entity/ErpAstCipBizModel.java` 仅 `extends CrudBizModel<ErpAstCip>`，无业务方法。`IErpAstCipBiz` 仅 `ICrudBiz`。
- **CIP 状态轴语义错位**：`ErpAstCip.status` 列绑定 `ext:dict="erp-ast/asset-status"`（DRAFT/IN_SERVICE/IDLE/SCRAPPED/SOLD），与 owner 设计 `cip.md` §流程的三态「DRAFT / IN_CONSTRUCTION / TRANSFERRED」语义不一致（IN_SERVICE/IDLE/SCRAPPED/SOLD 对 CIP 不适用）。需新增 `erp-ast/cip-status` 字典，或确认沿用既有 `asset-status` 的语义映射（Decision 项，见 Phase 1）。
- **资本化通道已就绪但 sourceType=CIP 路径未真正使用**：`ErpAstAssetCapitalizationBizModel` + `CapitalizationAcctDocProvider`（`CAPITALIZATION(80)` 业务类型，sourceType `DIRECT_PURCHASE(30)`/`CIP(20)`）done 计划 1000-2；1000-2 落地的资本化 BizModel 在 sourceType=CIP(20) 时按"借固定资产/贷在建工程"出凭证，但贷方科目当前由 `CapitalizationPostingDispatcher`（`SUBJECT_CIP="1603"` 常量 + `BILL_DATA_CREDIT_SUBJECT_CODE` 覆盖键）确定，**1000-2 加的 `ErpAstAssetCategory.cipSubjectId` 列尚未接入 Provider**——本计划在 `buildCapitalizationRequest` 显式传 `BILL_DATA_CREDIT_SUBJECT_CODE` 以驱动类别贷方科目；1000-2 的范围本身**未包含** CIP 自身"成本归集 + 完工触发"——剩余 CIP 子流程由 `extended-roadmap.md:12` 显式归本计划承接（非 1000-2 Deferred 项；1000-2 Deferred 仅有减值/重估、库存转固、nop-job）。
- **资本化审批反向方法名称核实**：`IErpAstAssetCapitalizationBiz` extends `ICrudBiz` + `IApprovableBiz`，**无 `reverse` 方法**；正确名称为 `reverseApprove`（由 `IApprovableBiz` 声明，`ErpAstAssetCapitalizationProcessor` 实现，红冲 CAPITALIZATION 凭证 + 资产卡片 status 回 DRAFT + 取消折旧计划）。本计划 `reverseTransfer` 委托 `reverseApprove`。
- **业财过账引擎 SPI 就绪**：`IErpFinVoucherBiz.post/reverse` facade（finance-dao 跨域契约层）+ `IErpFinAcctDocProvider` 注册表（finance-service）。assets-service 已依赖 finance-service（见 1000-2 接线）。`ErpFinBusinessType.CAPITALIZATION(80)` 既有，无需新增业务类型。
- **资产卡片创建与折旧计划生成通道就绪**：`IErpAstAssetBiz.requireEntity/save` + 资本化审批通过后建卡 + 生成 `ErpAstDepreciationSchedule` 折旧计划，全部 done 计划 1000-2 Phase 2。
- **剩余差距**：(1) CIP 成本归集子实体（CIP CostItem）未物化；(2) 进度付款子实体（CIP ProgressBilling）未物化；(3) CIP 状态机（DRAFT→IN_CONSTRUCTION→TRANSFERRED）与触发逻辑未实现；(4) 完工转固 → 调用既有资本化通道建卡 + 出 CAPITALIZATION(80) 凭证 + 回写 CIP.isCompleted/completedAssetId 的链路未接通；(5) 部分转固（选择部分 CostItem 先转固）未实现；(6) 利息资本化未实现。

## Goals

- 物化 `ErpAstCipCostItem`（CIP 成本归集行：采购/人工/服务/利息资本化按笔归集）+ `ErpAstCipProgressBilling`（进度付款记录）两子实体，经 model→codegen 生成 dao/meta/service/web 链。
- 实现 CIP 三态状态机：`DRAFT`（草稿）→ `IN_CONSTRUCTION`（建设中，开放成本归集）→ `TRANSFERRED`（完工转固，终态）。状态经新字典 `erp-ast/cip-status` 物化（替代错位的 `asset-status` 复用）。
- 实现 CIP 成本归集：`addCostItem(cipId, costType, amountFunctional, sourceBillType+sourceBillCode, remark, ctx)` 显式入口（@BizMutation）+ CIP.accumulatedCost 实时累加；`addProgressBilling` 进度付款记录入口（仅记已付工程款，不转固）。
- 实现 CIP 完工转固：`transferToAsset(cipId, [costItemIdList|null=全部], ctx)` —— 汇总所选 CostItem 转固成本 → 调用既有 `IErpAstAssetCapitalizationBiz` 建卡（sourceType=CIP(20)）+ 生成 CAPITALIZATION(80) 凭证 → 回写 CIP.isCompleted/completedAssetId/status=TRANSFERRED。
- 实现部分转固：选择部分 CostItem 先行转固（CIP 状态保持 IN_CONSTRUCTION 直至全部 CostItem 转固），新建资产卡片原值=部分汇总。
- 实现 reverseTransfer（红冲转固）：委托 `IErpAstAssetCapitalizationBiz.reverseApprove`（1000-2 已实现：红冲 CAPITALIZATION 凭证 + 资产卡片 status 回 DRAFT + 取消折旧计划）→ 回退 CIP 子实体 CostItem.postedTransferFlag=false → CIP 状态从 TRANSFERRED 回 IN_CONSTRUCTION（仅全部红冲；部分红冲归 Non-Goal）。
- 利息资本化作为本期 config-gated 探索项：若需新增利息计提子流程，本期仅物化 costType=INTEREST_CAPITALIZATION 行 + config-gated 默认 false（计算逻辑归后继）。
- 解除 `extended-roadmap.md:12` 工作项 2.5 `partial` → `done`（UC-AST-06 CIP 转固处理落地）。

## Non-Goals

- **采购单据自动归集**：`cip.md` §关键业务规则 2「采购单据行标注 cipAssetId，审核过账时自动写入 ErpAstCipCostItem」属跨域 hook（purchase→assets），本期仅提供显式 `addCostItem` 入口，自动归集归后继。触发条件：采购单据行 `cipAssetId` 字段落地时。
- **人工工时自动归集**：工时单关联项目（projects）再分摊到 CIP 属跨域多步编排，本期 Non-Goal。触发条件：项目工时分摊到 CIP 业务上线时。
- **利息资本化自动计算引擎**：`cip.md` §利息资本化规则（资本化期间 + 资本化率 + 累计支出加权平均数 + 上限不超过实际借款利息）属独立金融服务面，本期仅物化 costType 枚举 + 手工录入入口，自动计提归后继。触发条件：专项借款利息管理需求启动时。
- **CIP 与项目（ErpPrjProject）的强关联**：`cip.md` §关键业务规则 1「CIP 关联一个项目」—— 本期 CIP 不强引用 projectId（加可空弱引用列，不做强制外键）；项目→CIP 自动成本流转归后继。触发条件：项目结算转固（0305-1）与 CIP 双通道业务上线时。
- **多币种 CIP 外币重估**：CIP 累计成本若跨币种需期末外币重估，归 finance 期末结账 successor（1000-3/0540-2 范式）。
- **CIP 资产类别的成本归集维度（按工程子项/楼层等）**：`cip.md` §部分转固「整条生产线分阶段完工」语义若需 CostItem 树状分组，归后继。
- **nop-job 自动完工检测**：`cip.md` §配置选项「转固触发方式 MANUAL/AUTO」—— 本期仅 MANUAL，AUTO 自动检测完工归后继。触发条件：自动完工检测规则上线时。

## Task Route

- Type: `implementation-only change`（owner 设计 done 于 `cip.md`；一处状态字典 Decision 需收敛；ORM 加性增量子实体；多步编排走 Processor 模式）
- Owner Docs: `docs/design/assets/cip.md`（权威设计）、`docs/design/assets/state-machine.md`（卡片状态机）、`docs/design/assets/depreciation-and-posting.md` §二（资本化/转固前置）、`docs/architecture/`（业财过账 SPI 与模块边界）
- Skill Selection Basis: 后端 BizModel/IBiz/跨域 I*Biz/ErrorCode/Processor 模式/业财过账 Provider → 加载 `nop-backend-dev`；测试经 `JunitAutoTestCase`+IGraphQLEngine → 加载 `nop-testing`。两技能必需输入（owner 设计 `cip.md`、assets ORM 模型、过账 SPI 范式 1000-2/0540-3）均就绪。

## Infrastructure And Config Prereqs

- nop-job 调度器已运行（`app-erp-all` scheduler.yaml enabled=true），本期不新增 job（MANUAL 触发范式）。
- 新增配置键（`ErpAstConstants` 声明 + `NopSysVariable` 默认值）：`erp-ast.cip-interest-capitalization-enabled`（默认 false，关闭时拒收 costType=INTEREST_CAPITALIZATION）、`erp-ast.cip-require-approval`（默认 true，CIP 转固强制审批）、`erp-ast.cip-partial-transfer-allowed`（默认 true）。
- 无新业务类型（复用既有 `CAPITALIZATION(80)`）；无新外部端口/密钥/数据迁移依赖；codegen 增量生成，无回滚脚本需求。

## Execution Plan

### Phase 1 - ORM 加性：CIP 子实体物化 + 状态字典 + codegen 增量

Status: completed
Targets: `module-assets/model/app-erp-assets.orm.xml`、`module-assets/erp-ast-meta/src/main/resources/_vfs/dict/erp-ast/cip-status.dict.yaml`、codegen 产物
Skill: `nop-backend-dev`

- Item Types: `Decision | Add`
- Prereqs: 无

- [x] Decision: CIP 状态字典。**选择**：新增 `erp-ast/cip-status` 字典（DRAFT/IN_CONSTRUCTION/TRANSFERRED），将 `ErpAstCip.status` 列 `ext:dict` 从 `erp-ast/asset-status` 改绑 `erp-ast/cip-status`。**替代**：(a) 复用 asset-status + 语义映射（IN_SERVICE 当 IN_CONSTRUCTION，SCRAPPED 当 TRANSFERRED —— 语义错位，rejected）；(b) 加 `cipStatus` 独立列（与既有 `status` 列并存，数据冗余，rejected）。**残留风险**：既有 CIP 数据（如有）status 值需迁移；bootstrap 期无生产数据，DDL 自动重建。
  - Skill: none
- [x] Decision: CIP 终态命名。owner 设计 `cip.md:53` 用 `COMPLETED`，本计划用 `TRANSFERRED`。**选择**：`TRANSFERRED`（语义精准——CIP 资产已转出为固定资产，区别于"完工但未转固"）。**替代**：沿用 `COMPLETED`（语义模糊——CIP 完工≠转固，可能完工待决算）。**残留风险**：`cip.md:53` 需同步修订（Phase 4 owner doc 收口）；本期 DDL 直接建 TRANSFERRED。
  - Skill: none
- [x] Decision: 利息资本化 config 默认值。owner 设计 `cip.md:75` 默认 `true`，本计划默认 `false`。**选择**：`false`（本期不实现自动计提引擎，默认 true 会导致 INTEREST_CAPITALIZATION CostItem 在无计算引擎时被业务方误用）。**替代**：(a) 默认 true + 仅校验录入（与设计对齐，但隐藏"无计算引擎"风险，rejected）；(b) 默认 true + 抛 ERR（阻断录入，过严，rejected）。**残留风险**：与 `cip.md:75` 不一致，需 Phase 4 owner doc 同步修订为"自动计提引擎落地前默认 false"。
- [x] Add: 在 `app-erp-assets.orm.xml` 追加两子实体（字段对齐 `cip.md` §流程）：
  - `ErpAstCipCostItem`（cipId FK + lineNo + costType 字典 `erp-ast/cip-cost-type`（PURCHASE/SERVICE/LABOR/INTEREST_CAPITALIZATION/OTHER）+ amountSource + exchangeRate + amountFunctional + sourceBillType + sourceBillCode + postedTransferFlag（已转固标记，部分转固用）+ remark + 标准八列）
  - `ErpAstCipProgressBilling`（cipId FK + lineNo + billingDate + billingMilestone + amountSource + exchangeRate + amountFunctional + paymentVoucherCode + paidFlag + remark + 标准八列）
  - 头实体 `ErpAstCip` 加性 `projectId`（可空弱引用列，不强外键）+ `cipAssetCategorySnapshot`（转固时类别快照，避免类别后续修改影响追溯）。新字典 `erp-ast/cip-status` + `erp-ast/cip-cost-type`。
  - Skill: `nop-backend-dev`
- [x] Add: 经 `mvn clean install -DskipTests -pl module-assets -am` 触发 `gen-orm.xgen` 增量链（delta-merge 保留手写方法，对齐 `project-context.md:33` "后续模型变更用 mvn clean install 增量重新生成，不要重跑 nop-cli gen"）；验证生成产物（dao entity + IBiz + meta + service/web 空壳 + action-auth）与既有 10 实体链一致；`ErpAstCipBizModel` 既有手写方法保留。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 两子实体 + 2 字典在 orm.xml 中通过 XDef 校验；assets 域 codegen 产物存在（dao entity 类、IBiz 接口、CrudBizModel 空壳）；`ErpAstCip.status` 列 dict 改绑生效。
- [x] assets 模块 `mvn clean install -DskipTests -pl module-assets -am` BUILD SUCCESS（解除 Phase 2/3 编译依赖）。

### Phase 2 - CIP 成本归集与进度付款 BizModel

Status: completed
Targets: `IErpAstCipBiz`、`IErpAstCipCostItemBiz`、`IErpAstCipProgressBillingBiz`、对应 BizModel、`ErpAstConstants`、`ErpAstErrors`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] Add: `IErpAstCipBiz` 扩展方法：
  - `@BizMutation ErpAstCip startConstruction(@Name("cipId") Long cipId, IServiceContext context)` —— DRAFT→IN_CONSTRUCTION（仅 DRAFT 允许；CIP 信息完整校验：categoryId + businessDate + 至少一条 CostItem 或不强制，Decision 见下）；非法迁移抛 `ERR_CIP_ILLEGAL_STATUS_TRANSITION`。
  - `@BizMutation ErpAstCipCostItem addCostItem(@Name("cipId") Long cipId, @Name("costType") String costType, @Name("amountFunctional") BigDecimal amountFunctional, @Name("sourceBillType") String sourceBillType, @Name("sourceBillCode") String sourceBillCode, @Name("remark") String remark, IServiceContext context)` —— 校验 CIP 状态 IN_CONSTRUCTION（否则 `ERR_CIP_NOT_IN_CONSTRUCTION`）；amountFunctional > 0；costType=INTEREST_CAPITALIZATION 时 config-gated（`erp-ast.cip-interest-capitalization-enabled=false` 则拒收，抛 `ERR_CIP_INTEREST_CAPITALIZATION_DISABLED`）；写 CostItem + 累加 CIP.accumulatedCost（atomic update）+ amountFunctional 派生 amountSource/exchangeRate 经 CIP 头币种。
  - `@BizMutation ErpAstCipProgressBilling addProgressBilling(@Name("cipId") Long cipId, @Name("billingDate") LocalDate billingDate, @Name("billingMilestone") String billingMilestone, @Name("amountFunctional") BigDecimal amountFunctional, @Name("paymentVoucherCode") String paymentVoucherCode, IServiceContext context)` —— 仅 IN_CONSTRUCTION 允许；进度款本身不转固，只作已付工程款记录；金额 > 0 校验。
  - `@BizQuery List<ErpAstCipCostItem> findCostItems(@Name("cipId") Long cipId, @Name("onlyUntransferred") boolean onlyUntransferred, IServiceContext context)` 与 `findProgressBillings` 查询入口。
  - Skill: `nop-backend-dev`
- [x] Add: `ErpAstErrors` 扩展 ErrorCode：`ERR_CIP_ILLEGAL_STATUS_TRANSITION`（参数 cipCode/currentStatus/targetStatus）、`ERR_CIP_NOT_IN_CONSTRUCTION`（cipCode/currentStatus）、`ERR_CIP_INTEREST_CAPITALIZATION_DISABLED`、`ERR_CIP_COST_ITEM_ALREADY_TRANSFERRED`（部分转固重复选择防护）、`ERR_CIP_NO_COST_TO_TRANSFER`（空 CostItem 转固防护）、`ERR_CIP_ALREADY_COMPLETED`（终态防护）。`ARG_*` 常量齐备。描述中文，i18n 框架翻译。
  - Skill: `nop-backend-dev`
- [x] Add: `ErpAstConstants` 扩展配置键常量：`CFG_CIP_INTEREST_CAPITALIZATION_ENABLED`、`CFG_CIP_REQUIRE_APPROVAL`、`CFG_CIP_PARTIAL_TRANSFER_ALLOWED`，经 `AppConfig.var(..., defaultValue)` 读取。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] CIP 状态机三态迁移方法可调（DRAFT→IN_CONSTRUCTION）；非法迁移抛 ErrorCode；addCostItem 实时累加 CIP.accumulatedCost；INTEREST_CAPITALIZATION config-gated 拒收行为可观察。
- [x] `mvn compile -pl module-assets/erp-ast-service -am` BUILD SUCCESS（解除 Phase 3 编译依赖；具体行为测试在 Phase 4 统一编写）。

### Phase 3 - 完工转固（全部 + 部分）+ Processor 模式 + 业财过账接线

Status: completed
Targets: `IErpAstCipBiz.transferToAsset`/`reverseTransfer`、`CipTransferProcessor`、`CipCapitalizationRequestBuilder`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: Phase 2

- [x] Decision: 转固路径选择。**选择**：CIP 转固调用既有 `IErpAstAssetCapitalizationBiz` 的 `submit/approve` 链（sourceType=CIP(20)），由 Capitalization 单审批通过后建卡 + 出 CAPITALIZATION(80) 凭证；CIP 不重复实现建卡/过账逻辑（DRY + 复用 1000-2 已审计管线）。**替代**：(a) CIP 直接调 `IErpAstAssetBiz.save` 建卡 + 自己组装 PostingEvent → 凭证不走资本化审批链（绕过 1000-2 审批门控，rejected）；(b) 新增 CIP_TRANSFER 业务类型（与 CAPITALIZATION(80) 重复，rejected）。**残留风险**：CIP 转固需经两层审批（CIP 完工审批 + 资本化审批），可经 config-gated 让 Capitalization 走 DIRECT 审批模式简化；残留风险记录于 owner doc。
  - Skill: none
- [x] Add: `IErpAstCipBiz.transferToAsset(@Name("cipId") Long cipId, @Name("costItemIds") List<Long> costItemIds, @Name("transferDate") LocalDate transferDate, IServiceContext context)` —— 多步编排走 `CipTransferProcessor`（protected step 方法，产品化可定制）：
  - `validateTransferable(cip, costItems, ctx)`：CIP 状态 IN_CONSTRUCTION；costItemIds 非空（空表示全部，由 caller 显式传 all）；所选 CostItem 均 postedTransferFlag=false（防重复转固）；`erp-ast.cip-partial-transfer-allowed=false` 时必须选择全部 CostItem。
  - `buildCapitalizationRequest(cip, costItems, ctx)`：汇总 amountFunctional → 构造 `ErpAstAssetCapitalization` 头（sourceType=CIP(20)、sourceCipId=cip.code、categoryId=cip.categoryId、acquisitionCost=汇总金额、businessDate=transferDate、remark=拼接 costItem lineNos）。
  - `doTransfer(capitalization, cip, costItems, ctx)`：调 `IErpAstAssetCapitalizationBiz.submit` 启动资本化审批流（或 DIRECT 模式 approve 立即建卡，config-gated）。
  - `postProcess(cip, costItems, capitalization, ctx)`：标记 CostItem.postedTransferFlag=true；若全部 CostItem 转固 → CIP.status=TRANSFERRED + isCompleted=true；否则保持 IN_CONSTRUCTION；回写 capitalization.completedAssetId 到 CIP（在 Capitalization approve 链末回调，或经 CIP 查询资本化单）。
  - Skill: `nop-backend-dev`
- [x] Add: `reverseTransfer(@Name("cipId") Long cipId, @Name("capitalizationId") Long capitalizationId, IServiceContext context)` —— 红字冲销：调既有 `IErpAstAssetCapitalizationBiz.reverseApprove`（1000-2 已实现：红冲 CAPITALIZATION 凭证 + 资产卡片 status 回 DRAFT + 取消折旧计划）→ 回退 CostItem.postedTransferFlag=false → CIP 状态从 TRANSFERRED 回 IN_CONSTRUCTION（仅当全部红冲时；部分红冲抛 `ERR_CIP_PARTIAL_REVERSE_NOT_SUPPORTED`，归 Non-Goal）。
  - Skill: `nop-backend-dev`
- [x] Add: owner doc 收口 —— `cip.md` 修订与「实现注记（计划 0930-1）」节：(a) `cip.md:11` 实体引用 `ErpAstCipAsset` → `ErpAstCip`（命名漂移修正）；(b) `cip.md:53` 终态 `COMPLETED` → `TRANSFERRED`（同步本计划 Decision）；(c) `cip.md:75` 利息资本化默认 `true` → `false`（同步本计划 Decision + 注明"自动计提引擎落地前默认 false"）；(d) 新增实现注记：状态字典 Decision、转固路径 Decision（复用 Capitalization 链）、采购单据自动归集/工时归集/利息资本化计算引擎/CIP-项目强关联/AUTO 完工检测/部分红冲 Non-Goal。
  - Skill: none

Exit Criteria:

- [x] 完工转固（全部）链路可观察：CIP IN_CONSTRUCTION → transferToAsset → 调资本化建卡 + CAPITALIZATION 凭证 → CIP.status=TRANSFERRED + isCompleted=true + completedAssetId 回写。
- [x] 部分转固可观察：选部分 CostItem → 新资产卡片原值=部分汇总 + CIP.status 保持 IN_CONSTRUCTION + 重复转固防护 ErrorCode 触发。
- [x] reverseTransfer 红冲回退 CIP 状态 + 资产卡片 + CAPITALIZATION 凭证（经既有 CapitalizationBiz.reverseApprove）。
- [x] `mvn compile -pl module-assets/erp-ast-service -am` BUILD SUCCESS（解除 Phase 4 测试编译依赖；具体行为测试在 Phase 4 统一编写）。

### Phase 4 - 行为测试 + 日志 + 文档对齐

Status: completed
Targets: `module-assets/erp-ast-service/src/test/java/app/erp/ast/service/TestErpAstCipTransfer.java`、`docs/logs/2026/07-{执行当日}.md`、`docs/backlog/extended-roadmap.md`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 3

- [x] Add: `TestErpAstCipTransfer`（`@NopTestConfig(localDb=true, initDatabaseSchema=TRUE, enableActionAuth=FALSE)` + H2 + 直接调用 BizModel Java API；不走 GraphQL 快照——CIP 转固是内部服务编排逻辑，断言实体状态而非响应 JSON）：
  - 场景 1（全部转固 happy path）：建 CIP（DRAFT）→ startConstruction（IN_CONSTRUCTION）→ addCostItem 多笔（PURCHASE + LABOR）+ addProgressBilling → transferToAsset(全部) → 断言：CIP.status=TRANSFERRED + isCompleted=true + completedAssetId 非空 + CostItem.postedTransferFlag 全 true + 资产卡片原值=Σ CostItem amountFunctional + CAPITALIZATION 凭证生成（借固定资产/贷在建工程 cipSubjectId）。
  - 场景 2（部分转固）：transferToAsset(部分 CostItem) → 断言：CIP.status 保持 IN_CONSTRUCTION + isCompleted=false + 新卡片原值=部分汇总 + 已转固 CostItem.postedTransferFlag=true / 未转固=false。
  - 场景 3（非法状态迁移）：DRAFT 直接 transferToAsset → `ERR_CIP_NOT_IN_CONSTRUCTION`；TRANSFERRED 终态再 transferToAsset → `ERR_CIP_ALREADY_COMPLETED`。
  - 场景 4（重复转固防护）：选已 postedTransferFlag=true 的 CostItem → `ERR_CIP_COST_ITEM_ALREADY_TRANSFERRED`。
  - 场景 5（INTEREST_CAPITALIZATION config-gated）：config=false → addCostItem(costType=INTEREST_CAPITALIZATION) → `ERR_CIP_INTEREST_CAPITALIZATION_DISABLED`；config=true → 成功。
  - 场景 6（进度付款不影响转固）：addProgressBilling 多笔 + transferToAsset 全部 → 资产原值=Σ CostItem（不含 ProgressBilling，已付工程款不参与转固成本）。
  - 场景 7（reverseTransfer 红冲）：全部转固后 → reverseTransfer → 断言：CIP.status 回 IN_CONSTRUCTION + CostItem.postedTransferFlag 全 false + 资产卡片已处置 + CAPITALIZATION 凭证红冲。
  - Skill: `nop-testing`
- [x] Proof: `mvn test -pl module-assets/erp-ast-service -am`（含本期新增 7+ 测试） → 0 failures / 0 errors；assets 既有 19 测试无回归（1000-2 落地）。命令：`mvn test -pl module-assets/erp-ast-service -am -Dtest=TestErpAst*`。
  - Skill: `nop-testing`
- [x] Add: `docs/logs/2026/{执行当日 month-day}.md` 新增本计划条目（含验证状态）；`extended-roadmap.md` 工作项 2.5 `partial` → `done`（标注 UC-AST-06 落地）；`cip.md` 实现注记补注。
  - Skill: none

Exit Criteria:

- [x] 新增行为测试 7+ 全绿；assets-service 既有测试无回归（0 failures/0 errors）。
- [x] 当日日志条目在位；roadmap 2.5 标 done；owner doc `cip.md` 实现注记补注。

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（`ses_0c67dc27fffebcE4zTTHJDccqD`，独立 general 子代理，新会话，冷重播无执行者上下文）— 17 项 baseline 主张逐行核实（ErpAstCip/CipCostItem 缺失/CipBizModel 空壳/CIPITALIZATION(80)/cip.md 流程匹配 全部 TRUE）。4 BLOCKER：(B1) Source/Related/Goal 误称「1000-2 Deferred 指向 CIP」—— 1000-2 Deferred 仅有减值/重估、库存转固、nop-job 三项，CIP 子流程归属实为 `extended-roadmap.md:12`；(B2) `reverseTransfer` 引用不存在的 `IErpAstAssetCapitalizationBiz.reverse`（实为 `reverseApprove`）+ Goals 行 26 与 Phase 3 行 120 双设计矛盾；(B3) owner doc `cip.md` 三处契约漂移未裁决（config 默认 true→false、终态 COMPLETED→TRANSFERRED、实体名 ErpAstCipAsset→ErpAstCip）；(B4) Phase 1 用 `nop-cli gen` 违反 `project-context.md:33`（应为 `mvn clean install` delta-merge）。3 建议（S1 cipSubjectId 未接入 Provider 实情/S2 anti-slack 词 `optional`+`如需`/S3 Phase 2/3 退出标准缺本地化 compile 检查）。**已修订**：B1→Source/Related/Goal 重写引用 `extended-roadmap.md:12`；B2→reverseTransfer 改委托 `reverseApprove` + Goals/Phase 3 一致；B3→新增 CIP 终态命名 Decision + 利息资本化 config 默认值 Decision + Phase 3 owner doc 项扩展为四项修订；B4→改 `mvn clean install` 增量；S1→Baseline 补 cipSubjectId 实情；S2→去 `optional`/`如需`；S3→Phase 2/3 退出标准加 `mvn compile` 本地化检查。
- Independent draft review iteration 2: **accept / consensus**（`ses_0c6755d7fffe60Fl74h03e3QH1`，独立 general 子代理，新会话，冷重播无执行者上下文）— B1/B2/B3/B4 全部 RESOLVED（live repo 核实：1000-2 Deferred 无 CIP 项 + roadmap 2.5 partial 含 UC-AST-06 + `IErpAstAssetCapitalizationBiz`/Processor 方法名 `reverseApprove` + cip.md 三处漂移 + project-context.md:33 命令）。0 NEW BLOCKER。3 非阻塞 nit（N1 Deferred 残留 `CapitalizationBiz.reverse` 应为 `reverseApprove`/N2 Baseline 残留 `如需`/N3 Phase 4 测试不走 GraphQL 快照方法学说明）**已吸收**：N1→改 `reverseApprove`；N2→改陈述式表述；N3→保留（理由充分）。**共识达成**：Plan Status 升级为 `active`。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处。

- [x] 范围内行为完成（CIP 三态状态机 + 成本归集 + 进度付款 + 完工转固/部分转固 + reverseTransfer + 业财过账复用 Capitalization 链）
- [x] 相关文档对齐（`extended-roadmap.md` 2.5 done；当日日志；`cip.md` 实现注记）
- [x] 已运行验证：`mvn clean install -DskipTests`（全模块）+ `mvn test -pl module-assets/erp-ast-service -am`（含新增测试）；0 failures / 0 errors
- [x] 无范围内项目降级为 deferred/follow-up（采购自动归集/工时归集/利息资本化计算引擎/CIP-项目强关联/多币种重估/AUTO 完工检测/部分红冲 均为计划内 Non-Goal 附触发条件）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 采购单据自动归集到 CIP CostItem

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 跨域 hook（purchase→assets），需采购单据行加 cipAssetId 字段 + 审核过账时调 IErpAstCipCostItemBiz.addCostItem；本期提供显式 addCostItem 入口，自动归集属跨域编排面。
- Successor Required: yes（触发条件：采购单据行 cipAssetId 字段落地时）

### 项目工时自动分摊到 CIP

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 工时单关联项目（projects）再分摊到 CIP 属跨域多步编排，依赖项目→CIP 强关联建模。
- Successor Required: yes（触发条件：项目工时分摊到 CIP 业务上线时）

### 利息资本化自动计算引擎

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `cip.md` §利息资本化规则（资本化期间 + 资本化率 + 累计支出加权平均数 + 上限）属独立金融服务面，依赖专项借款管理；本期仅物化 costType=INTEREST_CAPITALIZATION 行 + 手工录入入口。
- Successor Required: yes（触发条件：专项借款利息管理需求启动时）

### CIP 与项目（ErpPrjProject）强关联

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期 CIP 不强引用 projectId（可选弱引用列已加）；项目→CIP 自动成本流转属跨域编排面。
- Successor Required: yes（触发条件：项目结算转固（0305-1）与 CIP 双通道业务上线时）

### 部分红冲（部分 reverseTransfer）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 部分红冲涉及"已转固资产卡片部分回退 + CostItem 部分回退"复杂一致性，本期仅支持全部红冲（调既有 CapitalizationBiz.reverseApprove）。
- Successor Required: yes（触发条件：部分红冲业务需求上线时）

### nop-job AUTO 完工检测
- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `cip.md` §配置选项「转固触发方式 AUTO」需自动检测完工规则（如 estimatedCompletionDate 到期 + accumulatedCost ≥ 预算），属预测性面；本期仅 MANUAL。
- Successor Required: yes（触发条件：自动完工检测规则上线时）

## Closure

Status Note: 4 Phase 全部完成（Phase 1 ORM 加性 + 字典 + codegen；Phase 2 成本归集 + 进度付款 BizModel + ErrorCode + config 键；Phase 3 完工转固/部分转固 + reverseTransfer + 业财过账复用 Capitalization 链 + owner doc 收口；Phase 4 行为测试 + 日志 + roadmap）。所有阶段 Exit Criteria 与 Closure Gates 全部 `[x]`。独立结束审计（独立会话、冷重播无执行者上下文）核实 live repo 证据与计划一致，无范围内降级、无 hollow 实现、无隐藏契约漂移。

Closure Audit Evidence:

- Auditor / Agent: 独立 closure auditor 子代理（新会话，冷重播无执行者上下文）
- Verification method: 逐项 grep/glob/read 核实 live repo 证据，反 hollow 检查（runtime 接线 + 方法体非空 + 异常路径可达），文本一致性五点核对（Plan Status / 各 Phase Status / 各 Exit Criteria / Closure Gates / 日志条目一致），Deferred honesty（6 项 Non-Goal 全部附触发条件、无 in-scope 缺陷藏匿），owner doc 同步核对。
- Evidence (live repo, 全部 TRUE):
  - ORM 子实体物化：`module-assets/model/app-erp-assets.orm.xml:752`（`ErpAstCipCostItem`）+ `:806`（`ErpAstCipProgressBilling`）；`ErpAstCip` 头加性 `projectId` + `cipAssetCategorySnapshot`；`status` 列 dict 改绑 `cip-status`。
  - 字典：`module-assets/erp-ast-meta/src/main/resources/_vfs/dict/erp-ast/cip-status.dict.yaml` + `cip-cost-type.dict.yaml` 存在。
  - IBiz/BizModel 接线：`IErpAstCipBiz` 声明 7 方法（`startConstruction`/`addCostItem`/`addProgressBilling`/`findCostItems`/`findProgressBillings`/`transferToAsset`/`reverseTransfer`，dao 层）；`ErpAstCipBizModel` 委托 `ErpAstCipProcessor` 实现。
  - Processor 模式（产品化 protected step）：`module-assets/erp-ast-service/.../service/processor/ErpAstCipProcessor.java` 落地四步编排（`validateTransferable`/`buildCapitalizationRequest`/`doTransfer`/`postProcess`）+ `reverseTransfer` 委托 `IErpAstAssetCapitalizationBiz.reverseApprove`（非空方法体、异常路径全抛 ErrorCode）。**命名漂移注记**：计划 `Targets` 写 `CipTransferProcessor`/`CipCapitalizationRequestBuilder`，实现合并为 `ErpAstCipProcessor`（匹配 `ErpAst*` 前缀兄弟约定，如 `ErpAstAssetCapitalizationProcessor`），功能面与 protected step 语义保留，非反 hollow。
  - ErrorCode 齐备：`ErpAstErrors` 含 `ERR_CIP_ILLEGAL_STATUS_TRANSITION`/`ERR_CIP_NOT_IN_CONSTRUCTION`/`ERR_CIP_INTEREST_CAPITALIZATION_DISABLED`/`ERR_CIP_COST_ITEM_ALREADY_TRANSFERRED`/`ERR_CIP_NO_COST_TO_TRANSFER`/`ERR_CIP_ALREADY_COMPLETED`（全部含 ARG_* 常量 + 中文描述）。
  - Config 键齐备：`ErpAstConstants.CONFIG_CIP_INTEREST_CAPITALIZATION_ENABLED`/`CONFIG_CIP_REQUIRE_APPROVAL`/`CONFIG_CIP_PARTIAL_TRANSFER_ALLOWED`，经 `AppConfig.var(...)` 读取（`ErpAstCipProcessor:370,374` 默认值与计划一致）。
  - 行为测试：`module-assets/erp-ast-service/src/test/java/app/erp/ast/service/TestErpAstCipTransfer.java` 含 8 cases（全部转固 happy path / 部分转固 / 非法状态迁移 ×2 / 重复转固防护 / INTEREST_CAPITALIZATION config-gated / 进度款不影响转固 / reverseTransfer 红冲回退）。
  - Owner doc 同步：`docs/design/assets/cip.md` 三处契约漂移修正（`ErpAstCipAsset`→`ErpAstCip` / `COMPLETED`→`TRANSFERRED` / 利息资本化默认 `true`→`false`）+ 新增「实现注记（计划 0930-1）」节含 4 Decision + 6 Non-Goal（含触发条件）。
  - Roadmap：`docs/backlog/extended-roadmap.md:12` 工作项 2.5 标 ✅ done（UC-AST-06 CIP 转固处理落地，引用本计划文件名）。
  - 日志：`docs/logs/2026/07-07.md` 含本计划聚合条目（4 Phase 完成记录 + 验证状态：assets-service 53 tests 0 failures/0 errors，含新增 8 tests；既有 45 tests 无回归，与 1000-2 落地基线一致）。
- Textual Consistency: Plan Status `completed` / 4 Phase `completed` / 各 Phase Exit Criteria 全 `[x]` / Closure Gates 全 `[x]` / 日志条目一致 — 五点对齐。
- Anti-Hollow: BizModel 委托 Processor 实非空；`transferToAsset` 经资本化链建卡 + 出凭证（运行时可达）；`reverseTransfer` 委托既有 `reverseApprove`（1000-2 已审计）；异常路径全经 ErrorCode 抛出（无吞异常）；config 键经 `AppConfig.var` 运行时读取（非 dead code）。
- Deferred Honesty: 6 Non-Goal 全部移入 `## Deferred But Adjudicated` 并附 successor 触发条件；无 in-scope 缺陷或契约漂移藏匿于 Deferred。
- Skill 使用核对: `nop-backend-dev`（Phase 1/2/3）+ `nop-testing`（Phase 4）均匹配任务面；执行期未变更。

Follow-up:

- 无非阻塞跟进项（全部 Non-Goal 已归 Deferred But Adjudicated 节，附 successor 触发条件）。
