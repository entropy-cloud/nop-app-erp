# 2026-08-08-1603-3-rc-mr1-r1-13-sal-order-availability-precheck RC-R1.13 — sales 订单级可用量预校验（P1-RC-020，MR1 第一批纯预授权）

> Plan Status: completed
> Last Reviewed: 2026-08-08
> Mission: requirement-compliance
> Work Item: RC-R1.13（MR1 第一批纯预授权：sales 订单级可用量预校验——`ErpSalOrderProcessor.validateBusinessRulesForApprove` 增可选库存可用量预校验，P1-RC-020）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.13 行 + `docs/audits/arm-index.md` P1-RC-020 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md` §3.1（RC-R1.13 = 纯 BizModel + config key）
> Related: `docs/design/sales/use-cases.md`（L1 UC-SAL-01 ① + UC-SAL-02）；`docs/design/sales/state-machine.md`（L2 冲突注记）；`docs/audits/2026-08-07-2330-rc-ma4-a4-2-47-55-sales-f1-f2-mainflow-outbound-runtime.md`（A4.2.47 运行时确认）；`docs/plans/2026-08-08-1154-3-rc-mr1-r1-10-pur-requisition-multi-supplier-split.md`（同批计划范式参照）
> Audit: required

## Current Baseline

- **finding P1-RC-020（arm-index 行）**：UC-SAL-01 ① 订单级可用量校验缺失（行为实质偏离 L1 字面控制点）。L1（`use-cases.md:27`）逐字「创建销售订单,审核通过(触发可用量校验 + 出库移动单生成)」；L2（`state-machine.md:56`）「销售订单 | 仅状态推进，不直接触发库存/凭证」与 L1 冲突——按 §4 Q1 以 L1 为准。L3 实仓：`ErpSalOrderProcessor.validateBusinessRulesForApprove:166-170` 仅 `requireCustomerActive` + `creditLimitChecker.check`；@Inject 簇（`:54-85`）零 `IErpInvStockMoveBiz`/`IErpInvStockBalanceBiz`；实际可用量校验 + 出库移动单生成在**出库审核**环节落实（`ErpSalDeliveryProcessor.triggerOutgoingMove:241-245` → `IErpInvStockMoveBiz.generateMove` → inv 域 `validateAvailable` 抛 `ERR_AVAILABLE_INSUFFICIENT`）。**非 P0**：可用量校验功能存在（出库审核环节落实）+ 不破坏活跃数据/会计过账 + O2C 核心循环完整。
- **A4.2.47 运行时确认**：`ErpSalOrderProcessor.validateBusinessRulesForApprove:166-170` 仅 requireCustomerActive + creditLimitChecker.check + @Inject 零库存 Facade 确认；实际校验落点 = 出库审核 `triggerOutgoingMove:241-245` → inv `validateAvailable:116-136`；运营影响 = 接单后到出库才发现缺货（SLA/客户体验类）。**维持 P1**。
- **实仓（HEAD 核查）**：
  - `ErpSalOrderProcessor.validateBusinessRulesForApprove:166-170`——**插入点**（`ErpSalOrderApproveProcessor.approve:50` 调 `processor.validateBusinessRulesForApprove(entity, context)`，位于 doApprove 之前）。
  - **跨域查询载体**：`IErpInvStockBalanceBiz`（`module-inventory/erp-inv-dao/.../biz/`，空 ICrudBiz 无自定义查询方法）+ `ErpInvStockBalance.availableQuantity`（propId 11 现成列）；订单行 `ErpSalOrderLine`：`materialId`（`_gen/_ErpSalOrderLine.java:1030`）+ `quantity`（`:1087`）+ `warehouseId`（`:1258` getWarehouseId；`:450` 为 getByPropId switch-case 内引用）；订单头 `ErpSalOrder.warehouseId`（`:1360`）——**零 ORM 变更**。
  - **跨域注入规则**：AGENTS.md「跨实体访问：在 BizModel 中，始终为其他实体注入 I*Biz 接口」——订单 Processor 注入 `IErpInvStockBalanceBiz`（或经库存域既有 @BizQuery 聚合查询入口），不用 IDaoProvider 直访库存表。
  - **config 范式参照**：sales 域既有 `erp-sal.credit-check-level`（`ErpSalConstants.java:39`，SOFT_WARNING 默认）三级策略——本行新增同级 config（如 `erp-sal.order-availability-check-level`，OFF/WARN/HARD 三值）对齐既有模式。
  - **测试基线**：`TestErpSalOrderApproval`（既有订单审批测试）+ `TestErpSalOrderToDeliveryEnd` / `TestErpSalOrderToCashEnd`（O2C 链）——新增校验后既有测试须零回归（默认 config 关闭保证）。
- **预授权判据**（第一批纯预授权）：纯 BizModel 代码逻辑修复 + 可能新增 config key，**不触 ORM 结构/会计核心/删除**；**无 ask-first checkbox**。roadmap RC-R1.13 行 `todo`，Deps（R1.0 done）已满足。
- **涉及文件**：`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalOrderProcessor.java`；`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/ErpSalConstants.java`（config key + 级别常量）；`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/ErpSalErrors.java`（如需新错误码）；测试类 1 个新增。

## Goals

- **订单级可用量预校验**：`ErpSalOrderProcessor.validateBusinessRulesForApprove` 增可选校验步骤——按新 config `erp-sal.order-availability-check-level`（默认 OFF）判定：WARN 级别不足时 LOG.warn 放行、HARD 级别不足时抛错拒绝审核（对齐 `credit-check-level` 三级范式）。
- **跨域查询**：经注入 `IErpInvStockBalanceBiz`（或库存域既有查询入口）按订单行 `materialId`+`warehouseId`（行级缺失回退订单头 `warehouseId`）聚合 `availableQuantity`，与订单行 `quantity` 比对；`availableQuantity == null`（含无余额行）视为 0——**决策项**（见 Execution Plan：HARD 下新物料无库存记录即拒绝，保守门禁语义显式记录）。
- **默认 OFF 保基线**：默认关闭不改变既有出库审核为唯一强制校验点的行为（config-gate = 部署启用决策，对齐 A4.1.4/A4.2.12 config-gate 范式）；既有测试零回归。
- **owner doc 收敛注记**：`state-machine.md:56`「仅状态推进」与 L1 冲突的注记更新（订单级可选预校验已落地，出库级强制校验保留）；`use-cases.md` 需求契约段不动（真相源冻结条款）。
- **测试矩阵**：WARN 不足放行 / HARD 不足拒绝 / OFF 默认跳过 / 足够放行 / 行级 warehouseId 回退订单头——分域 `mvn test -pl module-sales/erp-sal-service` 全绿 + `_cases/` 快照。
- 回填 arm-index P1-RC-020 → `done (RC-R1.13)` + roadmap RC-R1.13 → `done` + `docs/logs/` 日志条目。

## Non-Goals

- **不触 ORM 结构**（零列/零索引变更——availableQuantity/materialId/warehouseId/quantity 载体已就绪）。
- **不改变出库级强制校验**（`ErpSalDeliveryProcessor`/`validateAvailable` 不动——出库审核仍是硬校验点；订单级只是可选前置预校验）。
- **不做预留（reservation）**：订单级预校验是只读查询，不占用/不预留库存（预留语义仍归出库确认路径，跨域契约不动）。
- **不改 sales 取价/信用控制等其他 approve 校验**（`creditLimitChecker`/`requireCustomerActive` 保持）。
- **不改真相源契约段落**（use-cases L1 不动；state-machine.md 仅补实现注记）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧的预授权代码逻辑修复，Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/sales/use-cases.md`（L1 UC-SAL-01 ①）+ `docs/design/sales/state-machine.md`（L2 注记锚点）+ `docs/audits/requirement-compliance-methodology.md`（§5 预授权类目）+ `docs/audits/2026-08-07-2330-rc-ma4-a4-2-47-55-sales-f1-f2-mainflow-outbound-runtime.md`（A4.2.47 运行时证据）
- Skill Selection Basis: 实现面 = Processor protected step + 跨域 IBiz 注入 + config 三级读取（`nop-backend-dev`：跨实体访问规则[IBiz 注入]、Processor 派生覆盖点、config 范式）；测试（`nop-testing`：JunitAutoTestCase/IGraphQLEngine 断言 + 快照录制）。无 view.xml/xbiz/ORM 变更。

## Infrastructure And Config Prereqs

- 新增 config key `erp-sal.order-availability-check-level`（默认 OFF，WARN/HARD 部署启用时设置；无需 .env——缺省走默认值）。
- 分域验证前置：`mvn install -DskipTests` 后 `mvn test -pl module-sales/erp-sal-service`。

## Execution Plan

### Phase 1 - 订单级可用量预校验实现

Status: completed
Targets: `ErpSalOrderProcessor.java`；`ErpSalConstants.java`；`ErpSalErrors.java`
Skill: `nop-backend-dev`

- Item Types: `Fix | Decision`
- Prereqs: 无（既有基线）

- [x] `Decision` **跨域查询落点**：选项 A（推荐）= `ErpSalOrderProcessor` @Inject `IErpInvStockBalanceBiz`，经其 ICrudBiz 查询管道（`findList` + QueryBean 按 materialId/warehouseId 过滤）聚合 `availableQuantity`——符合「始终注入 I*Biz」跨域规则，零新增 IBiz 方法；选项 B = 在 `IErpInvStockBalanceBiz` 增自定义 @BizQuery 聚合方法（跨域接口契约扩展，审批面更广，超出本行最小修复面，弃）。备选与理由记录于本 Decision。
      - Skill: `nop-backend-dev`
      - **执行记录**：采用选项 A——`ErpSalOrderProcessor` 新增 `@Inject IErpInvStockBalanceBiz stockBalanceBiz`（非 private，对齐既有 `IErpInvStockMoveBiz` 注入范式），`resolveAvailableQuantity` 经 `stockBalanceBiz.findList(query, null, context)`（eq materialId + eq warehouseId + limit 1）读取余额。选项 B 需扩展跨域 IBiz 契约（新增 @BizQuery 方法 + BizModel 实现），超出本行最小修复面，弃。
- [x] `Decision` **null/无余额行语义**：选项 A（推荐）= `availableQuantity == null`（无 `ErpInvStockBalance` 行）视为 0——HARD 级别下新物料/未建余额物料默认拒绝（保守门禁：无库存记录即不可承诺）；选项 B = null 跳过该行（宽松：未建余额视为不校验，仅显式余额不足才拒绝）——宽松门禁可能放行实际无货订单，违背预校验目的，弃。备选与理由记录于本 Decision。**组织范围注记**：经 `IErpInvStockBalanceBiz` ICrudBiz 管道查询天然经过 R1.29 `ErpOrgIsolationQueryTransformer` 组织隔离过滤（对齐 A4.1.25/A4.2.10 治理面），多组织部署下余额读取按用户组织范围收敛——不新增绕过代码。
      - Skill: `nop-backend-dev`
      - **执行记录**：采用选项 A——`resolveAvailableQuantity` 对空查询结果或 `availableQuantity == null` 均返回 `BigDecimal.ZERO`；HARD 级别下无余额行即抛 `ERR_SAL_ORDER_AVAILABLE_INSUFFICIENT`（保守门禁，显式记录于 javadoc）。R1.29 组织隔离注记保持——findList 走 CrudBizModel 管道，org transformer（config-gated 默认关）激活时自动附加 orgId 过滤，无绕过代码。
- [x] `Fix` `ErpSalConstants` 新增 `CONFIG_ORDER_AVAILABILITY_CHECK_LEVEL = "erp-sal.order-availability-check-level"` + `ORDER_AVAILABILITY_CHECK_LEVEL_OFF/WARN/HARD` 三值常量（默认 OFF）；`ErpSalErrors` 新增 `ERR_SAL_ORDER_AVAILABLE_INSUFFICIENT`（带 ARG_ORDER_CODE / ARG_LINE_NO / ARG_MATERIAL_ID / ARG_WAREHOUSE_ID / ARG_AVAILABLE / ARG_REQUIRED 参数，中文描述）——供 HARD 级别抛错。
      - Skill: `nop-backend-dev`
- [x] `Fix` `ErpSalOrderProcessor` 新增 protected step `validateOrderAvailability(ErpSalOrder order, IServiceContext context)`：读 config 级别（默认 OFF，OFF 直接返回）；per 订单行：行 `materialId` + 行 `warehouseId`（null 回退订单头 `warehouseId`，仍 null 跳过该行）；经 `IErpInvStockBalanceBiz` 查询聚合 `availableQuantity`（null 视为 0）与 `quantity` 比对——不足时 WARN 级 `LOG.warn` 放行 / HARD 级抛 `ERR_SAL_ORDER_AVAILABLE_INSUFFICIENT`。
      - Skill: `nop-backend-dev`
- [x] `Fix` `validateBusinessRulesForApprove` 接线：在 `creditLimitChecker.check` 之后追加 `validateOrderAvailability(order, context)`（protected step 模式，派生可覆盖）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] `validateBusinessRulesForApprove` 含可用量预校验调用链；默认 OFF 不改变行为、WARN 放行、HARD 拒绝——Phase 2 测试断言证实
- [x] 无 ORM/出库强制校验路径变更（`git diff --stat` 仅 erp-sal-service Java + config 常量 + `_cases/` 快照）

### Phase 2 - 测试矩阵

Status: completed
Targets: `module-sales/erp-sal-service/src/test/java/app/erp/sal/service/TestErpSalOrderAvailabilityCheck.java`（新增）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 完成

- [x] `Add` 测试矩阵：① 默认 OFF 跳过（不设 config，库存不足订单 approve 通过——既有行为回归）；② WARN 不足放行（assignConfigValue 开 WARN + seed 库存不足 → approve 通过 + 无错误）；③ HARD 不足拒绝（assignConfigValue 开 HARD + seed 库存不足 → `ERR_SAL_ORDER_AVAILABLE_INSUFFICIENT`，approveStatus 保持 SUBMITTED）；④ 足够放行（库存 ≥ 行数量 → approve 通过）；⑤ 行级 warehouseId 回退订单头（行 warehouseId null + 订单头 warehouseId 命中库存）；⑥ 多行部分不足（HARD 下拒绝）。
      - Skill: `nop-testing`
      - **执行记录**：新建 `TestErpSalOrderAvailabilityCheck`（6 测试方法，`@NopTestConfig(localDb=true, initDatabaseSchema=TRUE, enableActionAuth=FALSE)`，DA 余额以 DAO 直写 `erp_inv_stock_balance` 种子）。快照录制：新类首录按 repo 范式预建 header-only `input/tables` CSVs（5 表：nop_sys_sequence/erp_md_partner/erp_sal_order/erp_sal_order_line/erp_inv_stock_balance——`AutoTestCaseDataBaseInitializer.createTables` 仅按 input/output 表文件建表），方法级 `@EnableSnapshot(saveOutput=true)` 录制（抛预期 snapshot-finished）后去注解切 CHECKING。`_cases/` 6 case 目录 output 快照落盘（HARD 拒绝路径 response.json5 含错误码 + erp_sal_order.csv approveStatus=SUBMITTED 证据）。
- [x] `Proof` GraphQL 冒烟断言（`executeRpc` 调 `ErpSalOrder__approve` HARD 场景返回错误码）+ `_cases/` 快照录制；既有 `TestErpSalOrderApproval`/`TestErpSalOrderToDeliveryEnd` 零回归。
      - Skill: `nop-testing`
      - **执行记录**：HARD 场景经 `ErpSalOrder__approve` RPC 断言 `erp.err.sal.order-available-insufficient` 错误码（response.json5 快照含 msg「可用量 5.0000 不足，需求数量 10.0000」）+ reload 断言 approveStatus 保持 SUBMITTED；WARN/OFF/足够/回退/多行五路径 Java 断言 + response.json5 快照。`mvn test -pl module-sales/erp-sal-service` 156 tests 全绿（既有 150 零回归 + 新 6，BUILD SUCCESS）+ 全仓 `mvn test` 2068 tests 0 失败 0 错误（1 skipped 预存）。**环境注记（21:00-21:22）**：全模块首跑遇 `NOP_SYS_SEQUENCE not found`——~/.m2 nop-ioc 被另一会话并发重建为 refactored `7e06450ff` 版本（BeanDefinition `getNextBeans` 零命中，与 2026-08-08 日志 RC-R1.5/6/12 同型故障；stash 验证本 plan 与失败无关——with/without 变更失败集逐类一致）+ 无关模块 erp-inv-service 同型失败证实平台级问题 → 按既有先例经 known-good worktree `/private/tmp/nop-ioc-good` 重建 nop-ioc 后 156 全绿；本 plan 新测试 6/6 在环境修复前已单独全绿（预建 input/tables 表驱动自建表，不依赖平台 schema init）。

Exit Criteria:

- [x] 新增测试矩阵全绿 + 既有 sales 测试零回归：`mvn test -pl module-sales/erp-sal-service`（BUILD SUCCESS）
- [x] OFF/WARN/HARD/足够/回退/多行六路径均有断言证据（无「行为落地但零覆盖」缺口）；快照录制完成

### Phase 3 - 文档回填 + arm-index/roadmap 状态

Status: completed
Targets: `docs/design/sales/state-machine.md`；`docs/audits/arm-index.md`；`docs/backlog/requirement-compliance-roadmap.md`；`docs/logs/2026/08-08.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1-2 完成

- [x] `Add` owner doc 注记：`state-machine.md:56` 销售订单行补「订单级可用量预校验（config-gated `erp-sal.order-availability-check-level`，默认 OFF）+ 出库级强制校验保留」实现注记（L1↔L2 冲突收敛说明）；不修改需求契约段。
      - Skill: none
      - **执行记录**：`state-machine.md` SUBMITTED→APPROVED 表销售订单行下方补实现注记块（订单级可选预校验 + 出库级强制校验保留 + L1↔L2 冲突收敛裁决引用）；use-cases.md 真相源契约段零改动（git diff 证实）。
- [x] `Add` arm-index P1-RC-020 行「修复状态」→ `done (RC-R1.13)` + 修复落地摘要；roadmap RC-R1.13 → done；`docs/logs/2026/08-08.md` 日志条目。
      - Skill: none
      - **执行记录**：arm-index P1-RC-020 行尾追加「修复状态：done（RC-R1.13，2026-08-08）」+ 落地摘要（protected step/config/错误码/测试/验证数字）；roadmap RC-R1.13 `todo → done ✅` + 摘要；`docs/logs/2026/08-08.md` 顶部追加 RC-R1.13 日志条目（含环境注记）。

Exit Criteria:

- [x] arm-index/roadmap 状态回填 + state-machine.md 注记落盘；日志条目写入

## Draft Review Record

- Independent draft review iteration 1: accept（独立子代理 ses_01f91f20affeLPJvu7IY8VeyjU）— 基线证据全部实仓核验准确（validateBusinessRulesForApprove:166-170 / @Inject 簇无库存 Facade / ErpSalOrderApproveProcessor:50 校验点位于 doApprove 之前 / IErpInvStockBalanceBiz 空 CrudBiz / credit-check-level 三级范式 / L1 逐字 / A4.2.47 运行时证据）；语义审查通过（默认 OFF 保基线正确 + 选项 A IBiz 注入符合跨实体规则 + 新 config key 属预授权范围）。3 非阻塞 Minor 已修订：`ErpSalOrderLine` warehouseId 行号 450→1258 精确化；`availableQuantity == null 视为 0` 从 Goals 提升为显式 Decision（选项 A 保守门禁语义 + 理由）；补 R1.29 组织隔离 transformer 经 ICrudBiz 管道生效注记。
- Independent draft review iteration 2: accept（独立子代理 ses_01f89e7a4ffefOHUTeiqnl1TVS 重扫）— 修订后文件无新问题；null 语义 Decision + org-scope 注记均在位。**计划可标记 active。**

## Closure Gates

- [x] 范围内行为完成
- [x] 相关文档对齐
- [x] 已运行验证（`mvn test -pl module-sales/erp-sal-service` 全绿 + `mvn clean install -DskipTests` 全量 BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

（无——范围内项目全落地后关闭；订单级预留（reservation）/预占库存归出库确认路径既有语义，不属本行。）

## Closure

Status Note: 全部 3 阶段完成（Phase 1 前次运行已落地 + Phase 2 测试矩阵 + Phase 3 文档回填），`mvn test -pl module-sales/erp-sal-service` 156 tests 全绿（新 6 + 既有 150 零回归）、全仓 `mvn test` 2068 tests 零失败零错误（1 skipped 预存）、`mvn clean install -DskipTests` 全量 BUILD SUCCESS、compliance checker actual==baseline 零漂移。环境注记：~/.m2 nop-ioc 曾被另一会话并发重建为 refactored 版本致全模块测试 H2 schema 初始化失效（NOP_SYS_SEQUENCE missing，stash 证实与 plan 无关），按既有先例经 known-good worktree `/private/tmp/nop-ioc-good` 重建后全绿。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理新会话（ses_01e61ba8effeTVq3PIXXmIUZ37）— **PASS**（0 P0/0 P1/0 P2；3 项 Info 非阻塞[baseline 顶部表历史遗留不一致非本计划引入 + Closure 段占位符待回填 + 全仓 2068 未独立复跑但模块级 156 全绿逻辑自洽]）；独立复跑 `mvn test -pl module-sales/erp-sal-service -Dtest=TestErpSalOrderAvailabilityCheck` 6 全绿 + 模块级 156 tests 0 失败 0 错误 + compliance checker actual==baseline 零漂移 + 保护区域核验（ORM 零变更 / 出库强制校验路径未动 / use-cases 契约段零改动）

Follow-up:

- <pending — 无范围外 follow-up；MR1 第一批后续 RC-R1.14+ 由 mission driver 继续>
