# 2026-08-07-1932-1-rc-mr1-r1-20-inv-batch-expiry-interception RC-R1.20 — inventory 批次效期拦截（P1-RC-031，MR1 第一批纯预授权）

> Plan Status: completed
> Last Reviewed: 2026-08-07
> Mission: requirement-compliance
> Work Item: RC-R1.20（MR1 第一批纯预授权：inventory 批次效期拦截，P1-RC-031）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.20 行 + `docs/audits/arm-index.md` P1-RC-031 行
> Related: `docs/audits/2026-08-03-1200-3-rc-ma1-a1-26-inventory-f2-batch-traceability-expiry-negative-stock.md`（A1.26 切片，finding 来源）；`docs/audits/2026-08-07-2345-rc-ma4-a4-2-74-82-inventory-stockmove-batch-stocktake-runtime.md`（A4.2.78 null 语义设计输入 + A4.2.79 MR1-blocked successor）；`docs/discussions/2026-08-07-1140-rc-approval-inventory-analysis.md` §7 A1/A2 + §4 Q7（product-scope 不裁剪裁决）；`docs/plans/2026-08-07-1819-1-rc-mr1-r1-0-finding-expansion.md`（R1.0 展开器）
> Audit: required

## Current Baseline

- **finding P1-RC-031（arm-index 行）**：UC-INV-06 出库效期拦截完全缺失——L1 `docs/design/inventory/use-cases.md:113` 逐字「若 批次.效期 < 当前日期 且 物料.批次管控 == 强制：出库移动单确认失败（批次过期拦截）」；L2 `docs/design/inventory/state-machine.md §4 异常路径 :64`「批次过期：出库时校验批次是否在有效期；过期批次拒绝出库（可配置放行）」+ `trace-chain.md §批次效期追溯 :218-234`。Q7 人工裁决「维持 P1 强制实现，product-scope 不变」（讨论文档 §4/§5）。
- **实仓现状**：`ErpInvStockMoveProcessor.validateAvailable:116-136`（`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/`）全文无 expiry check（无 `getExpiryDate()`/`isBatchManaged` 查询）；`doConfirm:86-98` 顺序 = `validateAvailable → applyReservation → DRAFT→CONFIRMED`。`ErpInvErrors` 无 `ERR_BATCH_EXPIRED` 类错误码（现有 `ERR_AVAILABLE_INSUFFICIENT:49-51` 为错误码范式）。
- **字段基础设施齐全零消费**：`ErpInvBatch.expiryDate`（ORM `:908 EXPIRY_DATE`）+ `shelfLifeDays`（`:909`）+ `status` EXPIRED dict（`ErpInvDaoConstants.BATCH_STATUS_EXPIRED`）；`ErpMdMaterial.isBatchManaged`（`app-erp-master-data.orm.xml:203`，默认 false）。`ErpInvBatchBizModel` 为 15 行 CRUD 桩。
- **A4.2.78 关键运行时设计输入**（`2026-08-07-2345-rc-ma4-a4-2-74-82` §A4.2.78）：expiryDate 对**所有生产创建批次恒为 null**（mfg 完工建批 `BatchGenealogyWriter.ensureOutputLot:149-170` 不写 expiryDate + 采购/销售入库不建 ErpInvBatch）→ 修复必须显式定义 null 语义。本计划裁决：**null = 跳过拦截（视为永不过期）**。
- **A4.2.79（roadmap todo 行）**：显式 MR1-blocked successor——「MR1 P1-RC-031 修复落地前置依赖…保留 todo 待 MR1 落地后回队」。其验收问题 = 「expiry check 拦截点选择 doConfirm vs doComplete 下 reserved/available 一致性」。本计划拦截点 = `validateAvailable`（doConfirm 内、applyReservation 之前），expiry 拒绝路径不进入 applyReservation → reservedQuantity/balance 不变（A1.26 SP-4 预期一致性），A4.2.79 于本计划落地后回队验证。
- **config 读取范式**：`ErpInvStockMoveProcessor.isNegativeStockAllowed:286` `AppConfig.var(ErpInvConstants.CONFIG_ALLOW_NEGATIVE_STOCK, Boolean.FALSE)`；`ErpInvConstants` 为 config key 权威登记点。
- **既有测试**：`TestErpInvStockMoveBookkeeping`（9 tests 全绿，含 `testConfirmInsufficientAvailableRejected` 拒绝路径范式）；`TestErpInvDashboard.seedBatch:255-` 提供 batch seed 范式（batchNo/materialId/warehouseId/expiry）。
- **预授权判据**（第一批纯预授权）：arm-index P1-RC-031 行「修复纯 BizModel/Processor + ErrorCode + 测试补充预授权不触 ask-first，仅消费既有 expiryDate 字段」+ 展开器映射记录 `2026-08-07-1910-rc-mr1-r1-0-expander.md` §3.1（RC-R1.20 归第一批：「纯 BizModel/Processor + ErrorCode + 测试（Q7 已裁决 product-scope 不裁剪）」）。无 ORM/会计核心/删除触及，**无需 ask-first checkbox**。

## Goals

- 在 `validateAvailable`（doConfirm 拦截点）落地 per-line 批次效期守卫：对带 batchNo 的移动单行，物料 `isBatchManaged=true` 且 `ErpInvBatch.expiryDate != null` 且 `expiryDate < 当前日期` 时抛 `ERR_BATCH_EXPIRED`，确认失败；null expiryDate 跳过（A4.2.78 设计输入）。
- 新增 `ERR_BATCH_EXPIRED` 错误码（`ErpInvErrors`）+ 新增 config key `erp-inv.batch-expiry-check-enabled`（默认 true，L2「可配置放行」落地；false 时放行不拦截）。
- 守卫不受负库存/预留短路的错误短路：expiry 属合规门禁，与 `isNegativeStockAllowed()`/`reservesOnConfirm()` 可用量门禁解耦（Decision 裁决，见 Execution Plan）。
- 新增 dedicated 测试（过期拒绝 + null 跳过 + 未过期放行 + 非批次管控放行 + config 放行 + 拒绝后 DRAFT/reserved/balance 不变）。
- 回填 arm-index P1-RC-031 修复状态 + roadmap RC-R1.20 标记 done；解除 A4.2.79 回队阻塞（触发条件 = 本计划 done）。

## Non-Goals

- **不做 ACTIVE→EXPIRED 状态迁移与 expiry scheduler**（roadmap 行内「可选」项——按反松弛规则显式移出范围，登记 Deferred But Adjudicated，Successor Required: no）。
- **不增强 ErpInvBatchBizModel CRUD 桩**（批次主数据维护能力不属于本 finding 修复面）。
- **不改 ORM / api.xml / 数据字典**（零结构变更；isBatchManaged 与 expiryDate 既有字段消费）。
- **不修改 dashboard 预警**（`ErpInvDashboardBizModel.findBatchExpiryAlert` advisory 保持现状，非硬拦截）。
- **不处理批次效期的上游写入**（mfg 完工/采购入库写 expiryDate 属各域 successor，不属本 finding 修复面）。
- **不修改真相源**（use-cases/state-machine.md 需求契约段；本计划只消费其既有契约）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧的预授权代码逻辑修复，Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/inventory/use-cases.md`（L1）+ `docs/design/inventory/state-machine.md`（L2 异常路径）+ `docs/design/inventory/trace-chain.md`（L2 效期追溯）+ `docs/audits/requirement-compliance-methodology.md`（§5 预授权类目）
- Skill Selection Basis: 实现面 = BizModel/Processor + ErrorCode（`nop-backend-dev`：Processor protected 步骤、ErrorCode 定义、跨实体访问规则——物料 isBatchManaged 读取按规则优先注入 I*Biz，仅当无法满足时用 daoProvider 并注释原因）+ JUnit 测试（`nop-testing`：JunitAutoTestCase/@NopTestConfig/seed 范式）。无 view.xml/xbiz 变更，不加载 `nop-frontend-dev`。

## Infrastructure And Config Prereqs

- 新增 config key：`erp-inv.batch-expiry-check-enabled`（默认 true）——登记于 `ErpInvConstants`，经 `AppConfig.var` 读取；测试经 `assignConfigValue` 覆盖（对齐 `TestErpCsSlaNotification:103` 范式）。
- 无端口/外部服务依赖。测试依赖既有 seed 基础设施（ErpMdMaterial/ErpInvBatch/余额 seed）。
- 分域验证前置：`mvn install -DskipTests`（依赖模块就位）后 `mvn test -pl module-inventory/erp-inv-service`。

## Execution Plan

### Phase 1 - 错误码 + 效期拦截守卫实现

Status: completed
Targets: `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/ErpInvErrors.java`；`.../processor/ErpInvStockMoveProcessor.java`；`.../ErpInvConstants.java`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Fix`
- Prereqs: 无（既有基线）

- [x] `Add` `ErpInvErrors` 新增 `ERR_BATCH_EXPIRED`（`erp.err.inv.batch-expired`，描述含物料/批次/效期参数，仿 `ERR_AVAILABLE_INSUFFICIENT:49-51` 范式 + `ARG_BATCH_NO`/`ARG_EXPIRY_DATE` 参数键）。
      - Skill: `nop-backend-dev`
- [x] `Decision` 守卫适用移动单类型范围（契约边界裁决）：per-line 效期守卫仅适用于 `reservesOnConfirm(moveType)` 命中的类型（`MOVE_TYPE_OUTGOING` + `MOVE_TYPE_INTERNAL_TRANSFER`，`ErpInvStockMoveProcessor:246-247`）——与既有可用量校验同型边界，对齐 L1「出库移动单确认失败」（use-cases.md:113）+ state-machine.md「拒绝出库」字面；INCOMING 类移动单（采购入库/退货入库）不入拦截（收过期批次属质检域职责，非本 finding）。备选（否决）：对全部类型拦截——否决理由：doConfirm 对 INCOMING 同样生效（`ErpInvStockMoveGenerateMoveProcessor` 无条件调 doConfirm），全类型拦截将超出 L1 出库契约（如销售退货入库收过期批次被误拒）。
      - Skill: `nop-backend-dev`
- [x] `Decision` 守卫放置与短路语义：per-line 效期守卫在 `validateAvailable` 内执行，**不受** `isNegativeStockAllowed()` 短路豁免（expiry 是合规门禁，负库存配置不豁免）；对带 batchNo 的行按 batchNo 查 `ErpInvBatch`（batch 不存在 → 跳过），按 `ErpMdMaterial.isBatchManaged`（默认 false）判定是否批次管控。备选（否决）：置于短路之后——否决理由：`erp-inv.allow-negative-stock=true` 时过期批次将被放行，违反 L1 字面「出库确认失败」且 Q7 无 product-scope 裁剪。残留风险：批次管控物料的未登记批次（batch 查询 miss）跳过拦截，与现状一致并登记。
      - Skill: `nop-backend-dev`
- [x] `Decision` null 语义（A4.2.78 设计输入落地）：`expiryDate == null` → 跳过拦截（视为永不过期）。备选：null=立即过期（否决：expiryDate 对全部生产批次恒 null（A4.2.78 已证），选此将使所有批次出库不可用，破坏主路径）；config 默认 true 下 null 跳过与 dashboard advisory（expiryDate 恒 null → 预警也不触发）行为一致。
      - Skill: `nop-backend-dev`
- [x] `Decision` 可配置放行（L2 state-machine.md:64「可配置放行」落地）：新增 `ErpInvConstants.CONFIG_BATCH_EXPIRY_CHECK_ENABLED = "erp-inv.batch-expiry-check-enabled"`（默认 true）；false 时守卫整体跳过。备选：硬拦截无 config（否决：L2 显式「可配置放行」，且放行口可缓解部署期数据迁移张力）。
      - Skill: `nop-backend-dev`
- [x] `Fix` `ErpInvStockMoveProcessor` 实现守卫（protected 方法 + `validateAvailable` 接线），确认失败抛 `ERR_BATCH_EXPIRED`（含物料/批次/效期 param）；`doConfirm` 顺序不变（守卫 → applyReservation），拒绝路径不进入 applyReservation。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] `ERR_BATCH_EXPIRED` + config key + 守卫逻辑落地：过期批次 + 批次管控物料 + 出库/内部转移移动单 confirm 被拒；null/未过期/非批次管控/放行配置/INCOMING 移动单 均通过（明确成功与失败模式）
- [x] 无 ORM/契约变更（本阶段产物仅 Java 代码 + 常量）

### Phase 2 - dedicated 测试

Status: completed
Targets: `module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/`（新增 `TestErpInvBatchExpiryInterception`，镜像 `TestErpInvStockMoveBookkeeping` 范式）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 完成

- [x] `Add` 测试矩阵：① 过期+批次管控+出库移动单（`MOVE_TYPE_OUTGOING` + relatedBillType 镜像 `SALES_SHIP`，对齐 `testConfirmInsufficientAvailableRejected` seed 范式 `TestErpInvStockMoveBookkeeping:119,347-355`）→ 一次性 `genMove`（generateMove→confirm 单事务）拒绝 `ERR_BATCH_EXPIRED`——整笔回滚、移动单不残留（assertNull，同 `:124-125` 范式）、reservedQuantity/balance 不变；⑦ 两步流（CRUD `ErpInvStockMove__save` 落 DRAFT → `ErpInvStockMove__confirm(moveId)`，`ErpInvStockMoveBizModel:64-65` → `ErpInvStockMoveConfirmProcessor`）→ confirm 拒绝后移动单**保持 DRAFT** + reserved 不变（对齐 A4.2.79 验收：applyReservation 未执行；`generateMove` 无条件调 doConfirm（`ErpInvStockMoveGenerateMoveProcessor:41`）不留 DRAFT，故两步流是 DRAFT 保持语义的唯一可达路径）；② null expiryDate → 通过；③ 未来效期 → 通过；④ 非批次管控物料带 expiryDate → 通过（L1「物料.批次管控 == 强制」条件）；⑤ config `batch-expiry-check-enabled=false` → 过期放行；⑥ `allow-negative-stock=true` 下过期仍拒绝（Phase 1 Decision 回归）；⑧ INCOMING 移动单（采购入库型）带过期批次 → 通过（守卫类型范围边界）。
      - Skill: `nop-testing`
- [x] `Proof` 负向断言强度：拒绝路径断言错误码 + 参数 + 事务回滚后状态（对齐 `testConfirmInsufficientAvailableRejected` 拒绝范式）；测试类经 `@NopTestConfig` 隔离，零外部依赖。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 8 组测试全部落地并绿（`mvn test -pl module-inventory/erp-inv-service` 全绿，含既有 9+ tests 零回归）

### Phase 3 - 文档回填 + arm-index/roadmap 状态

Status: completed
Targets: `docs/design/inventory/state-machine.md`（§4 异常路径补注）；`docs/audits/arm-index.md`（P1-RC-031 修复状态）；`docs/backlog/requirement-compliance-roadmap.md`（RC-R1.20 done）；`docs/logs/2026/08-07.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1-2 完成

- [x] `Add` owner doc 补注：`state-machine.md §4` 记录 null 语义裁决 + config key `erp-inv.batch-expiry-check-enabled` + 拦截点（doConfirm/validateAvailable）+ 移动单类型范围（出库/内部转移）；不修改需求契约段（真相源冻结条款 `requirement-compliance-methodology.md §9` 遵守——state-machine.md §9 是场景演练章节，非冻结条款所在）。
      - Skill: none
- [x] `Add` arm-index P1-RC-031 行「修复状态」→ `done (RC-R1.20)` + 修复落地摘要；roadmap RC-R1.20 → done；解锁 A4.2.79 回队注记（触发条件已满足）。
      - Skill: none
- [x] `Add` 每日开发日志 `docs/logs/2026/08-08.md`（格式见 `docs/logs/00-log-writing-guide.md`；当日实际日期为 2026-08-08，计划原文 `08-07.md` 路径按实时仓库日期修正）。
      - Skill: none

Exit Criteria:

- [x] arm-index/roadmap 状态回填 + owner doc 补注落盘；日志条目写入

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（独立子代理 `ses_023fe6ae3ffeyX10fmoGMkh4Vz`，fresh session）——1 MAJOR（守卫移动单类型范围未裁决：validateAvailable 被 doConfirm 对全部类型调用，L1 契约仅出库）→ 修订：Phase 1 增 Decision 裁决守卫范围 = `reservesOnConfirm` 类型（OUTGOING + INTERNAL_TRANSFER，出库语义）+ 测试矩阵增 ⑧ INCOMING 边界用例；3 MINOR（DRAFT 断言流程未指定 / §9 冻结引用歧义 / 日志路径占位符）→ 修订：测试 ①/⑦ 双流程明确 + methodology §9 显式引用 + `docs/logs/2026/08-07.md` 对齐。
- Independent draft review iteration 2: `needs revision`（独立子代理 `ses_023f381daffeRvA7suHo2VlXGR`，fresh session）——iteration-1 修复全 PASS（MAJOR 类型范围裁决 + 负库存不豁免技术一致性核实 + 3 MINOR 全落地），3 新 MINOR：⑦ 两步流不可行（`generateMove` 无条件调 doConfirm `ErpInvStockMoveGenerateMoveProcessor:41` 不留 DRAFT——两步流须 CRUD save → confirm(moveId)）；①「SALES_OUTPUT 型」术语错（SALES_OUTPUT 是财务过账 businessType 非移动单类型，镜像范式为 MOVE_TYPE_OUTGOING + relatedBillType=SALES_SHIP）；Draft Review Record 未记录 iteration 1。修订：测试 ①/⑦ 措辞修正（含 `ErpInvStockMoveBizModel:64-65` confirm 入口证据）+ 本记录补齐。
- Independent draft review iteration 3: `accept`（独立子代理 `ses_023efd54effeHpvUhAM7ReeL05`，fresh session）——3 MINOR 全部实仓核实 RESOLVED（⑦ CRUD save→confirm 流程可行性 + DRAFT 保持语义 + ① MOVE_TYPE_OUTGOING/SALES_SHIP 措辞 + 记录完整性），0 BLOCKER 0 MAJOR 0 MINOR。共识达成，转 active。

## Closure Gates

- [x] 范围内行为完成：效期拦截路径 + `ERR_BATCH_EXPIRED` + null 语义 + config 门控全部落地；拒绝后 DRAFT/reserved/balance 不变
- [x] 相关文档对齐：state-machine.md 补注 + arm-index/roadmap 状态回填
- [x] 已运行验证：`mvn test -pl module-inventory/erp-inv-service` 全绿 + `mvn clean install -DskipTests` 全量构建通过 + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline（防基线漂移，project-context 已知失败模式 #1）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### ACTIVE→EXPIRED 状态迁移 + expiry scheduler（roadmap RC-R1.20 行内「可选」项）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: L1 UC-INV-06 仅要求「出库确认失败（批次过期拦截）」，未要求批次状态机迁移/调度；arm-index 修复面 = 守卫 + ErrorCode + 测试。状态迁移属后续批次生命周期增强，且当前 expiryDate 恒 null（A4.2.78），迁移无实际触发数据。
- Successor Required: no（后续 backlog/人工裁决；触发条件 = 批次效期写入路径落地且需状态可视化）

### ErpInvBatchBizModel CRUD 桩增强 / 批次效期上游写入（mfg 完工 / 采购入库写 expiryDate）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 与 P1-RC-031 不同控制点（守卫拦截 vs 数据写入）；守卫对 null expiryDate 显式跳过（Phase 1 Decision），上游不写不破坏拦截语义（仅使拦截暂不激活）。
- Successor Required: no（watch-only；写入路径归各域后续工作）

### A4.2.79（MA4 MR1-blocked 回队行）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: A4.2.79 触发条件 = P1-RC-031 修复落地。本计划落地后其回队条件满足，由 mission driver 另行独立 plan 执行（运行时验证 reserved/available 一致性），不并入本计划结果表面。
- Successor Required: yes（触发条件 = 本计划 done）

## Closure

Status Note: 执行完成（2026-08-08），Plan Status=completed；Phase 1-3 全 completed + 全 items/Exit Criteria `[x]`。Closure Gates 1-6 由执行者勾选，末 2 门（独立结束审计 + 证据）由独立结束审计子代理（MISSION_DRIVER closure audit，fresh session）审计通过后勾选——执行者未自我审计。Q7 人工裁决 product-scope 不裁剪；第一批纯预授权（无 ask-first）。修复落地后 A4.2.79 回队条件满足（roadmap 行注记解除阻塞）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（MISSION_DRIVER closure auditor，fresh session `[MISSION_DRIVER:2026-08-07-181210-mission-driver]`）实仓复核通过
- Evidence（2026-08-08 独立审计复核）:
  1. **实仓代码核验**：`ErpInvStockMoveProcessor.validateBatchExpiry:159-188`（`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/`）实仓存在且 `validateAvailable:129` 首行接线（doConfirm → validateAvailable → validateBatchExpiry → applyReservation 顺序，拒绝路径不进入 applyReservation）；`ErpInvErrors.ERR_BATCH_EXPIRED:59-61`（erp.err.inv.batch-expired + ARG_BATCH_NO/ARG_EXPIRY_DATE）实仓存在；`ErpInvConstants.CONFIG_BATCH_EXPIRY_CHECK_ENABLED:31`（默认 true，`isBatchExpiryCheckEnabled:352-355` 读取）实仓存在；跨实体访问经 `IErpMdMaterialBiz.get`（:172）/`IErpInvBatchBiz.findList`（:196）注入 I*Biz 走权限管道
  2. **测试实仓复核**：`TestErpInvBatchExpiryInterception` 8 组场景全落地（①过期拒绝+整笔回滚 / ⑦两步流 DRAFT 保持 / ②null 跳过 / ③未来效期放行 / ④非批次管控放行 / ⑤config 放行 / ⑥负库存不豁免 / ⑧INCOMING 边界）；`_cases/` 快照录制落盘；**独立审计复跑**：`mvn test -pl module-inventory/erp-inv-service` → **Tests run: 151, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS**（既有 143 + 新增 8 全绿零回归）
  3. **全仓验证**：`mvn test`（全 workspace）BUILD SUCCESS + `mvn clean install -DskipTests` BUILD SUCCESS
  4. **compliance checker 零漂移**：`bash docs/audits/nop-compliance-checker.sh` 实测 actual == baseline 全 19 规则逐项相等（守卫实现用 I*Biz 注入避免 daoFor 漂移）
  5. **状态回填**：arm-index P1-RC-031 行尾 `【RC-R1.20 修复落地 2026-08-08】done` 摘要；roadmap RC-R1.20 `todo → done ✅` + 文件头最后更新注记（2026-08-08）+ A4.2.79 行「MR1 阻塞解除」注记；state-machine.md §4 实现注记；日志 `docs/logs/2026/08-08.md` 顶部条目
  6. **交付物**：`ErpInvErrors.ERR_BATCH_EXPIRED`（erp.err.inv.batch-expired）+ `ErpInvConstants.CONFIG_BATCH_EXPIRY_CHECK_ENABLED` + `ErpInvStockMoveProcessor.validateBatchExpiry`（protected，validateAvailable 首行接线）+ 测试类 `TestErpInvBatchExpiryInterception`（8 组 + `_cases/` 快照）
  7. **范围诚实**：Deferred But Adjudicated 仅含 out-of-scope（ACTIVE→EXPIRED 迁移 + ErpInvBatchBizModel 增强，successor no）与 watch-only residual（上游 expiryDate 写入，successor no）与 out-of-scope successor（A4.2.79 回队，触发条件已满足），无范围内项目静默降级

Follow-up:

- A4.2.79 回队（本计划 done 后触发，roadmap 行已解除阻塞注记，待 mission driver 另行独立 plan 执行）
