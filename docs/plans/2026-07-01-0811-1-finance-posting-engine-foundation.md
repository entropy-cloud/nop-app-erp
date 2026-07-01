# 2026-07-01-0811-1 财务过账引擎基础：IErpFinAcctDocProvider SPI + 注册中心 + 模板驱动凭证生成

> Plan Status: completed
> Last Reviewed: 2026-07-01
> Source: `docs/backlog/core-business-roadmap.md` P1 工作项 1.5（IErpFinAcctDocProvider 过账 Provider）
> Related: `docs/plans/2026-07-01-0811-2-inventory-stockmove-bizmodel.md`（本计划为其存货过账提供引擎；同批）、`docs/plans/2026-06-30-2328-2-phase4-crud-smoke-tests.md`（其 Deferred「BizModel 业务逻辑/状态机/业财过账测试」「跨域端到端业务循环验证」的**业财过账部分**后继——本计划只覆盖业财过账基础，不含通用 BizModel 逻辑/状态机测试）、`docs/design/finance/posting.md`（过账机制权威源）、`docs/design/finance/state-machine.md`（凭证状态机权威源）、`docs/design/flow-overview.md` L3（业财打通层）
> Audit: required

## Current Baseline

**项目阶段**（实时核实 `docs/context/project-context.md`）：codegen 完成、待 BizModel 业务逻辑深化。18 域 CRUD 全绿（90 冒烟测试），全部 BizModel 仍为 `CrudBizModel<T>` 空壳。业财过账引擎**尚未存在**——本计划是该能力的 greenfield 基础。

**过账 SPI 全仓不存在**（实时核实）：`IErpFinAcctDocProvider` / `ErpFinAcctDocRegistry` / `IErpFinFactsValidator` 在 `module-*` 下 **0 个 Java 命中**（grep 确认）。`posted` 过账幂等标志已在**全部 8 个业务域**的业务单据实体上落地（purchase 5、sales 5、inventory 3、projects 3、manufacturing 4、assets 8、maintenance 2、quality 1 个 `posted` 列，`stdSqlType=BOOLEAN defaultValue=false`），但**没有任何代码读取或驱动它**——标志就位，引擎缺失。

**凭证模型已就绪**（实时核实 `module-finance/model/app-erp-finance.orm.xml` + 字典）：
- `ErpFinVoucher`（凭证头）：`code`/`voucherType`(int dict `erp-fin/voucher-type`)/`postingType`(int dict `erp-fin/posting-type`)/`voucherDate`/`orgId`/`acctSchemaId`/`periodId`/`totalDebit`/`totalCredit`(均 VARCHAR amount 域)/`isReversed`(bool)/`reversalOfVoucherId`/`docStatus`(int dict `erp-fin/voucher-status`)/`postedBy`/`postedAt`；关系 `period`/`acctSchema`/`lines`/`billLinks`。
- `ErpFinVoucherLine`（分录行）：`voucherId`/`lineNo`/`subjectId`/`subjectCode`/`subjectName`/`dcDirection`(int dict `erp-fin/dc-direction`)/`debitAmount`/`creditAmount`(VARCHAR amount)/`currencyId`/`exchangeRate`/`amountSource`/`amountFunctional`/`acctSchemaId`/`orgId`/`memo` + 辅助核算维度（`partnerId`/`departmentId`/`projectId`/`warehouseId`/`materialId`/`businessType`/`costCenterId`）。
- `ErpFinVoucherTemplate`（凭证模板头）：`code`/`acctSchemaId`(空=通用)/`businessType`(int dict `erp-fin/business-type`, mandatory)/`voucherType`/`isActive`/`validFrom`/`validTo`；关系 `lines`。
- `ErpFinVoucherTemplateLine`（模板行）：`templateId`/`lineNo`/`subjectCode`(可含占位符)/`dcDirection`/`amountExpression`/`accountKey`(科目映射键)/`amountKey`(金额占位键)/`memoTemplate`。
- `ErpFinVoucherBillR`（业财回链）：`voucherId`/`billType`/`billCode`/`billLineCode`/`businessType`。
- `ErpFinAccountingPeriod`（会计期间）：`status`(int dict `erp-fin/period-status`)/`year`/`month`/`startDate`/`endDate`。
- `ErpMdAcctSchema`（账套，master-data）。

**字典权威值**（实时核实 `module-finance/erp-fin-meta/.../dict/erp-fin/`）：
- `business-type`（`valueType: int`，13 项）：10 采购入库 / 20 销售出库 / 30 应付发票 / 40 应收发票 / 50 付款 / 60 收款 / 70 折旧 / 80 资本化 / 90 处置 / 100 生产成本结转 / 110 项目成本归集 / 120 期末结转 / 130 汇兑损益。
- `voucher-status`（int）：10 草稿 / 20 已过账 / 30 已作废。
- `dc-direction`（int，已存在）：借/贷方向。

**凭证状态机**（`docs/design/finance/state-machine.md` 权威）：DRAFT → POSTED（前置：草稿、**借贷平衡**、**期间未结账**、科目有效、汇率存在）；DRAFT → CANCELLED；POSTED → 红冲（生成红字凭证，金额取负，关联原凭证，双向回链）。

**过账机制**（`docs/design/finance/posting.md` 权威）：业务单据审核通过 → 设 `posted=false` → 发 `PostingEvent`(post-commit 异步) → `ErpFinAcctDocRegistry` 按 `businessType` O(1) 查 Provider → `createFacts()` → `IErpFinFactsValidator` 链 → 借贷平衡校验 → 写凭证+分录+回链 → 设 `posted=true`；幂等靠 `posted` 前置检查；冲销靠红字凭证。

**关键约束/陷阱**（实时核实）：
- `totalDebit`/`totalCredit`/`debitAmount`/`creditAmount` 均为 **VARCHAR 存储**（`domain="amount"`，DECIMAL 以字符串持久化）——平衡校验须 `BigDecimal` 解析，不可按数值列直比。
- 无专用**科目映射实体**（finance ORM 无 AccountMapping 类实体）——`posting.md` 的多维科目映射在当前模型中由模板行的 `subjectCode`(可含占位符)+`accountKey` 承载，无独立配置表。
- 财务 BizModel/I*Biz 全为 `CrudBizModel` 空壳（`ErpFinVoucherBizModel` 等 17 个）。

**剩余差距**：从零构建过账引擎（SPI+注册中心+编排服务+默认模板驱动 Provider）+ 幂等 + 期间门控 + 借贷平衡 + 业财回链 + 红冲，并以服务层集成测试证明端到端可运行。

## Goals

- **过账 SPI 与注册中心落地**：定义 `IErpFinAcctDocProvider`（`getSupportedBusinessTypes()` / `createFacts(billData, ctx)`）、`IErpFinFactsValidator`、`ErpFinAcctDocRegistry`（`@Inject List<IErpFinAcctDocProvider>` 启动时建 `BusinessType→Provider` ImmutableMap，运行时 O(1) 查找），`ErpFinBusinessType` 枚举（int code 与 `erp-fin/business-type` 字典逐一对齐），`PostingEvent`/`AcctDocContext` 契约。
- **过账编排服务可运行**：`ErpFinPostingService` 实现 `posting.md` 全流程——幂等（按 `billHeadCode`+`businessType` 反查 `ErpFinVoucherBillR`，已存在已过账凭证则空操作）→ 查 Provider → `createFacts` → FactsValidator 链 → 期间未结账校验 → 借贷平衡校验 → 写 `ErpFinVoucher`+`ErpFinVoucherLine`+`ErpFinVoucherBillR` + 置**凭证** `docStatus=POSTED`。引擎只负责**凭证侧**状态（凭证 + 回链 + 凭证状态）；**源业务单据的 `posted` 标志由域调用方在 `post()` 成功返回后自行置位**（引擎不持有任意源实体的引用——见 Non-Goals），故幂等以业财回链反查而非源 `posted` 为准。
- **默认模板驱动 Provider**：finance 工程内置一个通用 Provider，按 `businessType`+`acctSchemaId` 读 `ErpFinVoucherTemplate(+Line)`，按 `amountKey` 填充金额、按 `subjectCode`(占位符)/`accountKey` 解析科目，产出 `VoucherLine`——使引擎无任何业务域 Provider 时即可端到端验证，并作为后续 Pur/Sal/Inv Provider 的参照实现。
- **红字冲销入口**：业务单据作废 → 按业财回链反查已过账凭证 → 生成红字凭证（金额取负、`isReversed=true`、`reversalOfVoucherId` 关联原凭证）→ 走正常 DRAFT→POSTED。
- **服务层集成测试证明**：happy path（凭证生成+回链+**凭证** `docStatus=POSTED`）、幂等（重复过账空操作）、借贷不平衡拒绝、期间已结账拒绝、红字冲销，全部可重复通过。

## Non-Goals

- **不修改任何 `model/*.orm.xml` / `.api.xml`**（保护区域）——全部复用已生成实体；无 ORM 变更即无需 regen。
- **不实现业务域专属 Provider**（`PurAcctDocProvider`/`SalAcctDocProvider`/`InvAcctDocProvider`）——属各业务域 BizModel 计划（inventory 见同批 `2026-07-01-0811-2`；purchase/sales 属后续批次）。本计划只建引擎 + finance 内置通用 Provider。
- **不置位源业务单据的 `posted` 标志**——引擎只持有 `PostingEvent` 快照（`billHeadCode`/`billData`），不持有任意源实体的 ORM 引用，无法也不应跨域改源单据。源 `posted=true` 由**域调用方**在 `post()` 成功返回后自行置位（inventory 侧见 `2026-07-01-0811-2` Phase 3）。引擎幂等因此以 `ErpFinVoucherBillR` 反查为准（与 `posting.md` §幂等保证 的 `posted` 兜底并不矛盾——`posted` 是源侧二次防重，回链是引擎侧权威防重）。
- **不做异步派发与兜底扫描调度**——`posting.md` 的 post-commit 异步（nop-message）与定时兜底扫描（nop-job）属运营基础设施；本计划引擎暴露**同步** `post(PostingEvent)` 入口（可被事件订阅者/兜底 job 复用），异步接线与重试退避为显式 Follow-up。
- **不做多账套并行扇出**——单事件按其 `acctSchemaId` 生成一组凭证；多套科目表并行（管理账/税务账）为 Follow-up（模板已有 `acctSchemaId` 维度，引擎预留扩展点）。
- **不做 GL Distribution / 复杂 FactsValidator 实现**——只定义 `IErpFinFactsValidator` SPI 与执行链；按部门/项目分摊等具体 Validator 属后续。
- **不做完整多维科目映射**——通用 Provider 用模板行 `subjectCode`(占位符替换)+`accountKey` 解析；按物料类别/仓库/部门的多维决策映射（`posting.md` §科目映射）为 Follow-up（届时用 nop-rule 配置，不引入 ORM 实体）。
- **不做手工凭证过账的 BizModel mutation 接线**——引擎内部可复用 `post(voucherId)`；`ErpFinVoucherBizModel` 的 GraphQL `__post` mutation 接线为 Follow-up。
- **不做期末结账全流程**（成本核算/折旧/结转损益）——属 `core-business-roadmap.md` P4。

## Task Route

- Type: `architecture change`（业财过账是全部业务域共享的跨域契约，`IErpFinAcctDocProvider` 是 DAG 顶层财务域对外聚合接口；`ErpFinAcctDocRegistry` 的冲突裁决语义是后续各域 Provider 接入的稳定契约）。
- Owner Docs: `docs/design/finance/posting.md`（过账机制）、`docs/design/finance/state-machine.md`（凭证状态机）、`docs/design/flow-overview.md`（L3 业财打通层）、`docs/architecture/testing-strategy.md`（测试 runbook）、平台 `../nop-entropy/docs-for-ai/02-core-guides/service-layer.md` 与 `../nop-entropy/docs-for-ai/03-runbooks/`（BizModel/服务模式、IoC `@Inject` 规范）。
- Skill Selection Basis: `Skill: none`（实施）。`docs/skills/README.md` 现有技能均为审计/审查方法，无过账引擎编写技能匹配；实施遵循平台 service-layer 指南与 posting.md。独立草案/结束审计用 `plan-audit-prompt.md` / `closure-audit-prompt.md`；凭证状态机正确性用 `state-machine-business-review-prompt.md` 复核（见 Phase 2 Proof）。

## Infrastructure And Config Prereqs

- 无新增基础设施。测试用 H2 内存库（`erp-fin-app` 已含 `quarkus-jdbc-h2`；服务层测试 `@NopTestConfig(localDb=true, initDatabaseSchema=TRUE)` 强制 H2，复用 CRUD 冒烟 runbook）。
- `nop-entropy` 2.0.0-SNAPSHOT 父 POM 已在本地 Maven 仓库（`project-context.md` 确认可构建）。
- 无数据迁移（greenfield 服务，复用已存在实体与表）。回滚：本计划仅新增 Java 文件 + 测试，`git` 可整目录 revert；不触碰模型与生成器。

## Execution Plan

### Phase 1 - 过账 SPI 契约 + BusinessType 枚举 + 注册中心

Status: completed
Targets: `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/`（新增包：`IErpFinAcctDocProvider`、`IErpFinFactsValidator`、`ErpFinAcctDocRegistry`、`ErpFinBusinessType`、`PostingEvent`、`AcctDocContext`、`VoucherFact`）
Skill: none

- Item Types: `Decision | Add`
- Prereqs: 无

- [x] `Decision`：`ErpFinBusinessType` 枚举的 int code 与字典对齐方式。裁决：枚举常量的 `code` 字段**逐一对齐** `erp-fin/business-type` 字典现值（PURCHASE_INPUT=10、SALES_OUTPUT=20、AP_INVOICE=30、AR_INVOICE=40、PAYMENT=50、RECEIPT=60、DEPRECIATION=70、CAPITALIZATION=80、DISPOSAL=90、MANUFACTURING_COST_CLOSE=100、PROJECT_COST_COLLECTION=110、PERIOD_CLOSE=120、EXCHANGE_GAIN_LOSS=130）。`posting.md` 表中的字符串 businessType（如 `PURCHASE_INPUT`）映射为本枚举常量名；字典是数值权威源，枚举是类型安全门面。备选（被否）：直接用裸 int——丢失类型安全，`Registry` 无法 O(1) 按 BusinessType 建 Map。残留风险：字典未来新增项须同步加枚举常量（在 `posting.md` 业务类型映射表与本枚举间建立「新增须同步」约定）。**今日事实**（非仅未来风险）：`posting.md` §业务类型映射 自称唯一权威源，但其列出的 ~30 个细粒度字符串 businessType（`EXPENSE_CLAIM`/`EMPLOYEE_ADVANCE`/`NOTES_RECEIVABLE_*`/`FREIGHT`/`NCR_SCRAP`/`SALARY`/`INTER_TRANSFER`/`MANUFACTURING_FINISHED_INPUT` 等）在 13 项的 `erp-fin/business-type` 字典中**无对应 int code**——属 owner-doc 与字典的现存漂移。本计划枚举只覆盖字典已有的 13 项；各域 Provider 须将其细粒度业务映射到这 13 个通用核算类别之一（或后续提 owner-doc 漂移 Fix 扩字典，扩字典属保护区域须人工批准）。
  - Skill: none
- [x] `Decision`：`ErpFinAcctDocRegistry` 的冲突裁决语义（后续各域 Provider 接入的稳定契约）。裁决：默认模板 Provider（`ErpFinTemplateAcctDocProvider`）标记为 **fallback**；域专属 Provider（`InvAcctDocProvider`/`PurAcctDocProvider`/`...`）优先。当某 `businessType` 既有域 Provider 又有默认 Provider 时，**域 Provider 胜**（默认仅兜底未被任何域 Provider 接管的类型）；**两个非默认 Provider 声明同一 businessType = 启动期 fail-fast**（抛 `NopException`，暴露配置错误而非静默覆盖）。`Registry` 在 `@PostConstruct` 先装非默认 Provider 建 Map（冲突即 fail-fast），默认 Provider 仅填充 Map 中空缺的 key。备选（被否）：(a) 静默后者覆盖——隐藏配置错误；(b) 纯 `@Order` 数值优先级——默认 Provider 须声明全部类型才能兜底，且优先级数字易写错。残留风险：若域 Provider 误声明了应归默认的类型，会静默接管——须在 `posting.md` 业务类型映射表约定「各域 Provider 仅声明自有触发域」。
  - Skill: none
- [x] `Decision`：`createFacts` 的返回模型。裁决：Provider 返回 `List<VoucherFact>`（内部 DTO：`subjectCode`/`subjectId`/`dcDirection`/`amount`(BigDecimal)/`amountKey`/辅助维度/`memo`），由编排服务装配为 `ErpFinVoucherLine`（含 `amountSource`/`amountFunctional`/`exchangeRate` 双币种）。理由：解耦 Provider 与持久化实体，便于 FactsValidator 改写。备选（被否）：Provider 直接返回 `ErpFinVoucherLine` 实体——Validator 改写需操作实体、与 ORM session 耦合过紧。
  - Skill: none
- [x] `Add`：`IErpFinAcctDocProvider`（`Set<ErpFinBusinessType> getSupportedBusinessTypes()`；`List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx)`）、`IErpFinFactsValidator`（`List<VoucherFact> validate(List<VoucherFact>, AcctDocContext)` + `int getOrder()`）、`PostingEvent`（`businessType`/`billHeadCode`/`tenantId`/`acctSchemaId`/`billData`/`voucherDate`，对齐 `posting.md` PostingEvent 契约）、`AcctDocContext`（承载期间/账套/币种/汇率解析结果，供 Provider/Validator 共享）。
  - Skill: none
- [x] `Add`：`ErpFinAcctDocRegistry`（收集 `List<IErpFinAcctDocProvider>` 与 `List<IErpFinFactsValidator>`——经 IoC `<ioc:collect-beans>` 收集后由 setter 注入；`@PostConstruct init()` 先装**非默认** Provider 建 `EnumMap<ErpFinBusinessType, IErpFinAcctDocProvider>`，遇同 key 冲突即 fail-fast 抛 `NopException`，再用**默认(fallback)** Provider 填充空缺 key；`getProvider(ErpFinBusinessType)` O(1) 查找；Validators 按 `getOrder()` 排序）。类型安全注册，无反射字符串依赖（对齐 `posting.md` §类型安全注册 + 上方冲突裁决 Decision）。【实现说明：Nop IoC 的 `@Inject List<T>` 字段按 `List.class` 解析、不自动收集接口实现，故 Provider/Validator 列表改用 `app-service.beans.xml` 的 `<ioc:collect-beans by-type=.../>` 注入，语义与「收集所有实现 Bean」一致。】
  - Skill: none
- [x] `Proof`（本地化、解除 Phase 2 阻塞）：`testRegistryDomainProviderWinsOverDefault` + `testRegistryDuplicateNonDefaultFailsFast`——证明 fallback/优先/fail-fast 语义在 Phase 2 接入真实 Provider 前已稳定。`mvn test -pl module-finance/erp-fin-service -am` 全绿（2 tests run, 0 failures）。
  - Skill: none

Exit Criteria:

> 本阶段交付可编译的 SPI 契约与注册中心（无运行行为）。完整仓库 `mvn test` 归 Closure Gates。

- [x] 新增 SPI/枚举/DTO/Registry 文件存在且 `mvn compile -pl module-finance/erp-fin-service -am` 通过（解除 Phase 2 编译阻塞的本地化检查）
- [x] `ErpFinBusinessType` 13 个常量的 code 与 `business-type.dict.yaml` 逐一核对一致
- [x] Registry fallback/优先/fail-fast 语义 2 个测试通过（解除 Phase 2 Provider 接入的契约阻塞）

### Phase 2 - 过账编排服务 + 默认模板驱动 Provider（happy path / 幂等 / 平衡 / 期间门控）

Status: completed
Targets: `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinPostingService.java`、`.../provider/ErpFinTemplateAcctDocProvider.java`（默认通用 Provider）、`module-finance/erp-fin-service/src/test/.../posting/`
Skill: none

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1

- [x] `Decision`：测试策略。裁决：**服务层集成测试**——`@NopTestConfig(localDb=true, initDatabaseSchema=TRUE, enableActionAuth=FALSE)` + H2，直接调用 `ErpFinPostingService` Java API（不走 GraphQL 快照——快照模式用于 CRUD 页面行为，过账是服务内编排逻辑，断言实体状态而非响应 JSON）。测试自包含：seed `ErpFinVoucherTemplate(+Line)` + 一个合成 `PostingEvent` → 调 `post()` → 断言 `ErpFinVoucher`/`ErpFinVoucherLine`/`ErpFinVoucherBillR` 落库 + **凭证** `docStatus=POSTED`（不断言源单据 posted——引擎不持有源实体，见 Non-Goals）。备选（被否）：GraphQL mutation 快照——过账是内部服务非 GraphQL 面，且金额/时间戳非确定性高。
  - Skill: none
- [x] `Decision`：科目解析方式（无专用映射实体）。裁决：默认 `ErpFinTemplateAcctDocProvider` 按模板行 `subjectCode`（支持 `${...}` 占位符，从 `billData`/`AcctDocContext` 取值替换）解析最终科目编码，`accountKey` 作为科目维度标识记录在 `VoucherFact`（供未来多维映射扩展）。不引入新 ORM 实体；科目编码经编排服务在写库前查 `ErpMdSubject` 反查 `subjectId`/`subjectName`（对齐状态机「科目有效」前置：缺失抛 `NopException`）。备选（被否）：本计划新建科目映射表——AGENTS.md 禁止无 owner doc 改 ORM（保护区域），且 `posting.md` 多维映射归属后续。
  - Skill: none
- [x] `Add`：`ErpFinTemplateAcctDocProvider`（finance 内置**默认/fallback** Provider）——声明支持**确定集合**：核心 6 类进销存+收付款 `{PURCHASE_INPUT(10), SALES_OUTPUT(20), AP_INVOICE(30), AR_INVOICE(40), PAYMENT(50), RECEIPT(60)}`（`posting.md` 核心业务类型表）；标记为 fallback（Registry 中域 Provider 优先，见 Phase 1 冲突裁决 Decision）。按 `event.businessType`+`acctSchemaId` 查启用且在 `validFrom/validTo` 区间内的 `ErpFinVoucherTemplate`（具体账套优先于通用 null），遍历 `ErpFinVoucherTemplateLine`，按 `amountKey` 从 `billData` 取金额填入 `VoucherFact.amount`，按 `dcDirection`/`subjectCode`(占位符替换) 产出 `List<VoucherFact>`。模板缺失抛 `NopException`（对齐 `posting.md` 失败处理「模板缺失」）。其余 businessType（折旧/资本化/期末结转等）由对应域 Provider 接管或后续扩展。
  - Skill: none
- [x] `Add`：`ErpFinPostingService.post(PostingEvent)`——(1) 幂等前置：按 `billHeadCode`+`businessType` 反查 `ErpFinVoucherBillR`，已存在非红字已过账凭证则空操作返回（引擎权威防重；源侧 `posted` 是二次兜底，见 Non-Goals）；(2) `Registry.getProvider` → `createFacts`（缺失 Provider 抛 `ERR_NO_PROVIDER`）；(3) 按 `getOrder()` 依次 `IErpFinFactsValidator.validate`；(4) 期间门控：按 `event.voucherDate` 定位 `ErpFinAccountingPeriod`（`startDate<=date<=endDate`），`status` 非 OPEN(10)（含已结账/未开启/结账中）则抛 `NopException`；(5) 科目有效性：`subjectCode`→`ErpMdSubject` 反查，缺失抛 `NopException`；(6) 借贷平衡：`BigDecimal` 汇总借/贷，不等则抛 `NopException`；(7) 装配 `ErpFinVoucher`+`ErpFinVoucherLine`+`ErpFinVoucherBillR` 落库，置**凭证** `docStatus=POSTED`。`totalDebit`/`totalCredit` 按 BigDecimal 写 VARCHAR 列。引擎返回凭证 id；**不置位源业务单据**（源 `posted` 由域调用方在成功返回后自行置位）。非 BizModel 服务：`@SingleSession`+`@Transactional`（平台全局 pointcut 拦截器自动应用），`IDaoProvider`/`ErpFinAcctDocRegistry` 经 `@Inject` 字段注入，bean 在 `app-service.beans.xml` 注册。
  - Skill: none
- [x] `Proof`：服务层集成测试——`testPostHappyPath`（凭证+分录+回链落库、**凭证** `docStatus=POSTED`、借贷平衡——不断言源单据 posted，因引擎不持有源实体）、`testPostIdempotent`（同一 event 二次 `post` 空操作返回 null，不产生第二张凭证）、`testPostUnbalancedRejected`（模板配置致借贷不等 → `NopException`、不落库）、`testPostPeriodClosedRejected`（期间 status=已结账(30) → `NopException`）。`mvn test -pl module-finance/erp-fin-service -am` 全绿（4 tests run, 0 failures）。
  - Skill: none
- [x] `Proof`：凭证状态机正确性复核——用 `docs/skills/state-machine-business-review-prompt.md` 针对 DRAFT→POSTED 的前置与异常路径自检，结论记录如下（非阻塞门控，已执行）。
  - Skill: state-machine-business-review-prompt

  **复核结论（Verdict: pass，无 P0/P1）**：
  - DRAFT→POSTED 五前置全部落实：草稿（facts 内存态，自动过账不产生 DRAFT 待办，对齐 state-machine 场景 A）✓、借贷平衡（BigDecimal 比对，不平衡抛 `ERR_UNBALANCED`）✓、期间未结账（仅 OPEN(10) 允许，其余抛 `ERR_PERIOD_CLOSED`/`ERR_PERIOD_NOT_FOUND`）✓、科目有效（`ErpMdSubject` 反查，缺失抛 `ERR_SUBJECT_NOT_FOUND`）✓、汇率存在（见残留风险）。
  - 异常路径：模板缺失/Provider 缺失/科目缺失/借贷不平衡/期间非开启/重复过账幂等/红冲源缺失，全部抛 NopException 或空操作，与 state-machine.md §4 异常路径表一致。
  - POSTED 为终态、需红冲（`reverse()` 见 Phase 3）；引擎非用户面（无 auth，由域 BizModel 把权限，对齐 state-machine §6「业务自动触发的凭证无需人工过账动作」）。
  - **残留风险（P2，非阻塞）**：(a) `exchangeRate` 当事件未提供时默认 1（本位币假设）；非本位币须由调用方提供汇率，严格「汇率缺失报错」的多币种强校验属 Follow-up（多币种/多账套扇出）。(b) 同一单据并发 `post()` 的幂等反查存在 TOC 竞态（极小概率产生 2 张凭证）；生产由 post-commit 异步派发串行化 + 源 `posted` 兜底兜住（异步派发属 Deferred Follow-up）。

Exit Criteria:

> 本阶段交付可运行的过账编排 + 默认 Provider + 4 类行为测试。完整仓库 `mvn test` 归 Closure Gates。

- [x] `ErpFinPostingService.post` 4 个行为测试存在且 `mvn test -pl module-finance/erp-fin-service -am` 全绿（happy/幂等/不平衡/期间结账）
- [x] 默认 `ErpFinTemplateAcctDocProvider` 能从 seeded 模板产出平衡的借贷分录（happy path 证明）

### Phase 3 - 红字冲销 + 失败路径测试 + 收尾

Status: completed
Targets: `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinPostingService.java`（增 `reverse`）、`module-finance/erp-fin-service/src/test/.../posting/`、`docs/logs/2026/07-01.md`
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 2

- [x] `Add`：`ErpFinPostingService.reverse(billHeadCode, businessType)`——按业财回链反查**非红字**已过账凭证（`docStatus=POSTED` 且 `isReversed=false`）；生成红字凭证：复制原分录但金额取负（`debitAmount`/`creditAmount`/`amountSource`/`amountFunctional` 均 negate）、`isReversed=true`、`reversalOfVoucherId=原凭证id`、`postingType=红字冲销(50)`；走正常平衡/期间校验（复用 `resolveOpenPeriod`，按原凭证 `voucherDate` 重定位期间）；写新回链关联同一业务单据（`billCode`/`businessType`）。原凭证保留（审计轨迹，不改其状态）。currency/exchangeRate 经 `AcctDocContext` 从原分录携带（红字凭证分录的 `currencyId` 非空）。反查失败（无已过账凭证）抛 `NopException`（`ERR_REVERSE_SOURCE_NOT_FOUND`，对齐状态机「业务单据作废触发红冲失败→标记异常」）。
  - Skill: none
- [x] `Proof`：`testReverse`（先 happy 过账 → `reverse` → 红字凭证落库且金额为负、`isReversed=true`、`reversalOfVoucherId` 正确、原凭证保留非红字、新旧凭证经 `reversalOfVoucherId` 双向可追溯、原+红借贷净额为 0、回链 2 条）。`testReverseNotFound`（无已过账凭证 → `NopException`）。`mvn test -pl module-finance/erp-fin-service -am` 全绿（2 tests run, 0 failures）。
  - Skill: none
- [x] `Add`：更新当日开发日志 `docs/logs/2026/07-01.md`（按 `docs/logs/00-log-writing-guide.md`，时间倒序），记录过账引擎落地 + 验证状态（全绿）+ 关键实现发现（`@Inject List<T>` 不收集→`<ioc:collect-beans>`；全局 pointcut 拦截器应用于非 BizModel bean）。
  - Skill: none

Exit Criteria:

> 本阶段交付红字冲销 + 收尾。完整仓库 `mvn test` 归 Closure Gates。

- [x] `testReverse` / `testReverseNotFound` 存在且 `mvn test -pl module-finance/erp-fin-service -am` 全绿
- [x] 当日日志已记过账引擎落地与验证状态

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（ses_0e4f3d8bcffe6PJV2SuiY2BjwP，独立 general 子代理，新会话）— 基线主张全部实时核实属实（SPI/Registry 0 命中、`posted` 计数 purchase5/sales5/inventory3/projects3/mfg4/assets8/mnt2/qa1、finance 实体与列、business-type 13 项 10..130、enum 映射精确、VARCHAR amount、ErpMdAcctSchema 在 master-data、无专用映射实体、模块路径正确）。2 项阻塞：(B1) 默认 Provider 范围用「按需」违反 anti-slack + Registry 冲突语义未定义（Plan 2 `InvAcctDocProvider` 接入即撞）；(B2) Goals/Proof 称「置单据 posted=true」但引擎不持有源实体、无可达机制。3 项建议：S1 posting.md ~30 字符串 businessType 与 13 项字典现存漂移应承认为今日事实；S2 收窄 Related 后继表述为「业财过账部分」；S3 Task Route `architecture change + implementation-only` 冗余。迭代 1 已修订：B1→新增 Registry 冲突裁决 Decision（域 Provider 优先 + 默认 fallback + 非默认重复 fail-fast）+ Phase 1 增 2 个 Registry 契约测试 + 默认 Provider 范围固定为 6 核心类型；B2→Goals/Proof/Non-Goals 改为引擎只负责凭证侧（凭证+回链+`docStatus=POSTED`），源 `posted` 由域调用方置位（幂等改以回链反查为权威）；S1→Phase 1 Decision 增「今日事实」漂移段；S2→Related 改「业财过账部分」；S3→Type 改纯 `architecture change`。
- Independent draft review iteration 2: **needs revision**（ses_0e4e98a49ffei7kvmTY33MizoE，独立 general 子代理，新会话）— 复审 B1/B2 已修复确认（默认 Provider 固定 6 类型无「按需」、Registry 冲突裁决 Decision 含选择+2 备选+残留风险、Phase 1 含 2 契约测试 + 退出标准、引擎 Non-Goal/Goals/Add/Proof 一致限定凭证侧、幂等以回链反查、S1/S2/S3 已处理、anti-slack 干净、Rule 4/9 通过、6 类型码 10/20/30/40/50/60 均在字典、与 Plan 2 seam 一致、测试计数 2+4+2=8 一致）。发现 1 项文本一致性残留（Rule 11）：Goals 测试行与 Phase 2 测试策略 Decision 正文仍残留「单据 posted=true」（与同阶段 Proof/Add/Non-Goal 矛盾）—— B1-new。+ 1 建议 S1（Follow-up「业务域专属 Provider」缺触发条件）。迭代 2 已修订：Goals 测试行改「凭证 docStatus=POSTED」；Phase 2 测试策略 Decision 正文改「凭证 docStatus=POSTED（不断言源单据 posted）」并移除误增的重复执行落地行；Follow-up「业务域专属 Provider」补触发条件。
- Independent draft review iteration 3: **accept / consensus**（ses_0e4e41732ffe1K2NywKWeNoTa7，独立 general 子代理，新会话）— B1-new（Goals 测试行 + Phase 2 测试策略 Decision 正文的「单据 posted=true」残留）已改为「凭证 docStatus=POSTED」并移除重复行；S1（Follow-up 触发条件）已补。逐行核实：Goals(line43/46)/Proof(line121)/Add(line119)/Non-Goals(line52) 一致限定引擎为凭证侧、源 posted 归调用方；测试计数 2+4+2=8 与 Closure/Status Note 一致；无「按需」/anti-slack 回潮；Registry 冲突裁决 Decision + Phase 1 契约测试完好。**共识达成**（迭代 1-3 收敛）：计划为可接受的执行契约，Plan Status 升级为 `active`。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。本计划为 greenfield 服务代码 + 测试，结束时运行一次完整仓库验证。

- [x] 范围内行为完成：过账 SPI + 注册中心（含 fallback/fail-fast 裁决）+ 编排服务 + 默认模板 Provider + 红冲全部落地，8 个测试通过
- [x] 相关文档对齐：`core-business-roadmap.md` P1 工作项 1.5 标注进展（✅ done）；当日日志已记
- [x] 已运行验证：`mvn test -pl module-finance/erp-fin-service -am` 全绿；根 `mvn test -fae` = BUILD SUCCESS（无回归，18 域 + 引擎全绿）
- [x] 无范围内项目降级为 deferred/follow-up（异步派发/多账套扇出/多维科目映射/手工 mutation 接线均为计划内 Non-Goal，非范围内降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于 `Closure` 节

## Deferred But Adjudicated

### 异步过账派发与兜底扫描调度

- Classification: `optimization candidate`
- Why Not Blocking Closure: 引擎提供同步 `post(PostingEvent)` 入口，可被 nop-message 事件订阅者与 nop-job 兜底扫描复用；异步接线/重试退避/告警属运营基础设施，不阻塞引擎正确性与业务域 Provider 接入。
- Successor Required: yes（触发条件：业务域 Provider 接入后需要生产级最终一致性时，接线 post-commit 异步 + 每分钟兜底扫描）

### 多账套并行扇出（管理账/税务账/合并账）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 单事件按其 `acctSchemaId` 生成一组凭证已满足基线；模板已有 `acctSchemaId` 维度，引擎预留扩展点。
- Successor Required: yes（触发条件：启用多套科目表并行核算时）

### 完整多维科目映射（物料类别/仓库/部门→科目）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 默认 Provider 用模板行 `subjectCode`(占位符)+`accountKey` 已可解析科目；按维度的决策映射属 `posting.md` §科目映射 的后续增强（届时用 nop-rule 配置，不引入 ORM 实体）。
- Successor Required: yes（触发条件：同一 businessType 因物料类别/仓库需不同存货科目时）

### GL Distribution（科目分摊）与行业 FactsValidator

- Classification: `optimization candidate`
- Why Not Blocking Closure: `IErpFinFactsValidator` SPI 与执行链已定义；按部门/项目分摊、附加税计提等具体实现属后续。
- Successor Required: yes（触发条件：需要按辅助维度分摊金额或行业合规校验时）

### 手工凭证过账的 BizModel mutation 接线

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 引擎聚焦业务单据自动过账；`ErpFinVoucherBizModel` 的 GraphQL `__post`/`__reverse` mutation 接线属页面/接口层。
- Successor Required: yes（触发条件：财务员需经界面手工过账/红冲凭证时）

## Closure

Status Note: 计划可关闭的条件——过账引擎（SPI+注册中心+编排服务+默认模板 Provider+红冲）落地，8 个测试全绿（2 个 Registry 契约：fallback 优先 / 重复 fail-fast；4 个过账行为：happy/幂等/不平衡/期间结账；2 个红冲：成功/未找到），根 `mvn test` BUILD SUCCESS 无回归，当日日志已记。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话 ses_0e4b3a8ccffeH1qR2EIXArunPX，general 子代理；非执行者）。Verdict: **passes closure audit**（无 P0/P1）。
  - 实时核实 10 个 SPI/引擎 Java 文件 + `app-service.beans.xml` 与计划契约一致：`ErpFinBusinessType` 13 个 code 10..130 与 `business-type.dict.yaml` 完全一致；Registry EnumMap + 非默认优先/重复 fail-fast/fallback 填充 + validator 排序；TemplateProvider `isFallback()`+6 核心类型 + amountKey/占位符；`post()` 幂等回链反查 / Provider / Validator 链 / 仅 OPEN 期间门控 / ErpMdSubject 反查 / BigDecimal 平衡 / 持久化 Voucher+Line+BillR + `docStatus=POSTED` + `postedAt=CoreMetrics.currentDateTime()`；`reverse()` 红字凭证（金额取负、`isReversed=true`、`reversalOfVoucherId`、`postingType=50`）。引擎只触及财务实体——**从不置位源业务单据 `posted` 标志**（Non-Goal 遵守）。
  - 反模式检查全过：`@Inject` 字段均包级可见；时间取 `CoreMetrics`；全程 `NopException`+`ErrorCode`；`@SingleSession @Transactional` 仅顶层入口；无 `_gen`/`_` 前缀生成文件手改。
  - 测试：`mvn test -pl module-finance/erp-fin-service -am -Dtest=TestErpFinAcctDocRegistry,TestErpFinPostingService` → `Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`，BUILD SUCCESS。
  - 保护区域：无 `*.orm.xml`/`*.api.xml` 改动。文档对齐：`docs/logs/2026/07-01.md` 过账引擎条目在；`core-business-roadmap.md` 1.5 标 ✅ done。
  - 残留（非阻塞）：审计者未重跑根 `mvn test -fae`（执行者已确认全绿，变更均为增量）；`reverse()` 不重跑 FactsValidator 链（与 Phase 3 计划文本「走正常平衡/期间校验」一致）；多币种严格汇率校验 + post() TOC 串行化属已规划 Follow-up。
  - P2 提示：工作树含并发的无关变更（mission-driver 重构、同批计划 0811-2），建议提交时分离边界。

Follow-up:

- 异步过账派发 + 兜底扫描调度（见上方 Deferred）
- 多账套并行扇出（见上方 Deferred）
- 完整多维科目映射（见上方 Deferred）
- GL Distribution 与行业 FactsValidator（见上方 Deferred）
- 手工凭证过账 BizModel mutation 接线（见上方 Deferred）
- 业务域专属 Provider（Inv 见 `2026-07-01-0811-2`；Pur/Sal 属后续批次；触发条件：purchase/sales BizModel 计划起草并实现各自过账触发时）
