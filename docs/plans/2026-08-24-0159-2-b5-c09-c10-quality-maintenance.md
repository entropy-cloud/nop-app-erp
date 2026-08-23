# 2026-08-24-0159-2-b5-c09-c10-quality-maintenance 批次 B5 用例实施：C09 质检门控与 NCR/CAPA/SCRAP 闭环 + C10 维护工单与备件消耗过账

> Plan Status: completed
> Mission: integration-test
> Work Item: B5（C09, C10；主导域 quality + maintenance）
> Last Reviewed: 2026-08-24
> Source: `docs/backlog/integration-test-roadmap.md`（B5 行，M0.3 追加）
> Related: `2026-08-23-2133-1-b1-c01-c02-purchase-p2p-return`（done）、`2026-08-23-2133-2-b2-c03-c04-sales-o2c-return`（done）、`2026-08-23-2133-3-b3-c05-c06-inventory-cost`（done）、`2026-08-24-0159-1-b4-c07-c08-c21-mfg-aps`
> Audit: required

## Current Baseline

（全部事实 2026-08-24 实仓核验）

- **前置依赖**：M0.1 设计文档（`docs/design/integration-testing.md` §6 C09/C10 用例规格 + §3.4 机制实证结论 + §4 粒度约定 + §5 xwf 规避清单）+ M0.2 基建 + B1/B2/B3（均 done）。
- **基建就绪**（M0.2/B1-B3 产物）：基类 `ErpIntegrationTestCase`（机制 (c) 文件 H2 双模 + 10 个共享 step helper）；`_cases` 目录约定；surefire 模块级串行化已落地；已有 7 个集成用例类（Pilot + B1-B3 各 2 类 = 1+6）作为范本，**B5 按同范式实现（1 用例 = 1 类 1 方法 + @BeforeAll fresh-DB + RECORDING→CHECKING 往返）**。
- **qa 域既有测试资产（本批业务参照，不重复实现内部细节）**：`TestErpQaInspectionTrigger`（`ErpQaInspection__createForBusinessBill` + `isInspectionCleared` + 强制质检门控 `CONFIG_MANDATORY_INSPECTION_BILL_TYPES` = `erp-qua.mandatory-inspection-bill-types`；完工触发 FINAL 检验）、`TestErpQaNcrCapaEndToEnd`（NCR save/submitReview/resolve + CAPA 三步 `ErpQaAction__startAction`/`completeAction`/`verifyAction`）、`TestErpQaNcrPosting`（`ErpQaNonConformance__postNcr`/`reverseNcr` SCRAP 处置过账）、`TestErpQaRecallLocateNotifyReturn`（`ErpQaRecall__notifyCustomers`——注意：**实仓 QA 域主代码未见 NCR→ErpSysNotification 直连调用**，设计文档 §6 C09 步骤 4「NCR 状态变更触发 ErpSysNotification 生成（markRead 断言）」存在动作面漂移风险，实施期须裁决登记）。
- **mnt 域既有测试资产**：`TestErpMntDowntimeAndE2E`（`ErpMntRequest__save`/`accept`/`startRepair` + `ErpMntVisit__schedule`/`start`/`complete` + `ErpMntSparePartUsage__confirm`）、`TestErpMntSparePartPosting`（备件消耗过账 MAINTENANCE_ISSUE）、`TestErpMntLaborPosting`、`TestErpMntSparePartUsageReversal`（`reverseConfirm`）。
- **mnt 过账门控（实仓核验）**：`ErpMntConstants.CONFIG_SPARE_PART_POSTING_ENABLED` = `erp-mnt.spare-part-posting-enabled` **默认 false**（`DEFAULT_SPARE_PART_POSTING_ENABLED=false`；`MaintenanceIssuePostingDispatcher` 注释「关闭时仅库存出库，不生成凭证」）→ **C10 须 `@NopTestProperty(name="erp-mnt.spare-part-posting-enabled", value="true")`** 开启过账断言（对齐 C08 simulation 门控同型处理）。
- **C09 前置 seed（实仓核验在位）**：`erp_qa_inspection.csv`（INS-2026-001 ACCEPTED/INS-2026-002 REJECTED）、`erp_qa_non_conformance.csv`（NCR-2026-001 RETURN OPEN、NCR-2026-002 CONCESSION IN_REVIEW）、`erp_qa_action.csv`（NCR-2026-001 CAPA IN_PROGRESS）、`erp_mfg_work_order.csv`（WO-2026-003 IN_PROCESS 供完工门控负路径）。**完工门控机制（实仓核验）**：`ErpMfgWorkOrderProcessor.isInspectionGated`（config `erp-mfg.inspection-gate-enabled` + BOM `inspectionRequired=true`）→ 未清检验抛 `ERR_INSPECTION_REQUIRED`（mfg，**实仓唯一抛点 = `ErpMfgWorkOrderReportCompletionProcessor`**；qa 侧同名 ErrorCode 已定义但无抛点，不作断言源）；`ErpQaInspection__isInspectionCleared(billType, billCode)` 供正路径放行断言。
- **C10 前置 seed（实仓核验在位）**：`erp_mnt_request.csv`（REQ-2026-001 OPEN）、`erp_mnt_visit.csv`（VIS-2026-001/002 COMPLETED，scheduleId 留空避免 overdue 排除）、`erp_mnt_spare_part_usage.csv`（SPU-2026-001 ACTIVE/APPROVED）、`erp_mnt_equipment.csv`（EQ-2026-001 RUNNING assetId=2 跨域关联 AST-2026-002）、`erp_ast_asset.csv`（AST-2026-001/002 IN_SERVICE）。`ErpMntRequestStateMachine` **六态**（OPEN/ACCEPTED/IN_PROGRESS/COMPLETED/REJECTED/CANCELLED，`ErpMntRequestStateMachine.java:17-18` transitions 实证；accept 生成 DRAFT Visit 响应式副作用，`ErpMntRequestAcceptProcessor`）——C10 主路径 accept→startRepair→complete 不受影响；设计文档 §6 C10「状态机 5 态」表述为漂移点，Phase 2 结束后登记勘误。
- **运行互斥**：集成测试与 E2E live server 共用 `db/erp.mv.db`——执行前 `lsof -i :8011`（8080 同查）确认无 live server（e2e-runbook「集成测试」节纪律）。
- **基线计数**：known-good-baselines 2026-08-23 权威计数源 **3815 tests / 0 failures / 0 errors / 1 skipped / 624 报告文件**；app-erp-all **35/0/0/1**。B5 预期差量 **+2 tests / +2 报告文件**（2 用例类各 1 类 1 方法）——若 B4 已先行落地则以 B4 后基线为准。
- **流程改进登记（B3 closure MINOR-2 兑现）**：最终 `clean install` 会清除 surefire XML 计数证据——全量回归后、clean 前先拷贝计数/XML 再登记基线（本计划 Phase 3 执行纪律）。
- **deferred 触发条件（B1/M0.2 watch-only residual）**：若执行期发生 seed 修正授权路径（roadmap 横切关注点 3），须同步测量登记重录耗时（以单用例 252K/类为预算参考）；本批默认自包含建数，预期不触发。

## Goals

- **C09** = 测试类 `TestErpC09QaNcrCapaScrap`（1 类 1 方法）：自包含检验单 REJECTED → 完工门控阻断（`ErpMfgWorkOrder__close`/完工动作被拒，错误码断言）→ NCR save → resolve（无 CAPA 须显式 noCapaReason；CAPA 三步 startAction/completeAction/verifyAction）→ `ErpQaNonConformance__postNcr`（SCRAP 处置过账：凭证借贷平衡 + 库存扣减）→ 通知断言（**以当前实现为准裁决**：NCR→ErpSysNotification 直连或 markRead 的可行断言路径，若实仓无此机制则登记设计文档 §6 C09 勘误并改以可达断言替代）；三层全比对。
- **C10** = 测试类 `TestErpC10MntRequestSparePart`（1 类 1 方法）：自包含维护请求 save（OPEN）→ accept（生成 DRAFT Visit）→ startRepair → complete（六态状态机终态）→ `ErpMntVisit__complete`（访问完成 + visit_task）→ 备件消耗 `ErpMntSparePartUsage__confirm`（@NopTestProperty 开启过账门控）→ MAINTENANCE_ISSUE 过账（凭证借贷平衡 + 备件库存扣减）→ `ErpAstAsset__get`（设备关联资产卡片核对）+ 非法迁移守卫断言；三层全比对。
- 两用例 **RECORDING→CHECKING 往返全绿**。
- roadmap B5 状态 `todo` → `done`（经独立 closure audit）+ 日志 + 基线差量登记 + 设计文档 §6 C09 潜在动作面勘误登记（若发生）。

## Non-Goals

- B4/B6-B10 用例（各归对应分批计划——B4 已由 `2026-08-24-0159-1-b4-c07-c08-c21-mfg-aps` 承接）。
- 生产代码/ORM/契约/API 变更——用例实施中发现的产品缺陷按 roadmap 横切关注点 4 走保护区域门禁（accounting/finance postings = plan-first + owner doc + tests；其余 = auto + dual-agent-approval），**不得在本计划内直接改生产代码**。
- 默认修改 seed CSV——确需修正走 roadmap 横切关注点 3「seed 修正授权」流程；默认以自包含建数满足前置。
- 不接 CI（roadmap 已裁决出 scope）；不做浏览器层 E2E 页面验证（分工边界）。
- 不实现未覆盖功能（横切关注点 6「以当前实现为准」）：如 QA NCR 通知机制实仓不存在，则以设计文档勘误 + 可达断言替代，不强行实现通知链路。

## Task Route

- Type: `implementation-only change`（集成测试代码 + 快照 + 文档，零生产代码/契约/模型变更）
- Owner Docs: `docs/design/integration-testing.md`（§6 C09/C10 规格 + §3.4 编写纪律 + §4 粒度 + §5 xwf 清单）、`docs/backlog/integration-test-roadmap.md`（B5 行 + 横切关注点）、`docs/testing/e2e-runbook.md`（「集成测试」节）、`docs/testing/known-good-baselines.md`
- Skill Selection Basis: `nop-testing` 匹配测试基类选择/@NopTestConfig/@NopTestProperty（C10 过账门控 = C08/C06 同型）/快照录制回放（RECORDING→CHECKING）/三层验证模型；M0.2 试点 + B1-B3 六用例类为同模式范本，qa/mnt 既有 EndToEnd 测试为业务参照。

## Infrastructure And Config Prereqs

- 文件型 H2 `db/erp.mv.db` fresh 机制已就绪（基类每类 1 次清理）；**与 E2E live server 互斥**——执行前 `lsof -i :8011`（8080 同查）确认无 live server。
- **C10 config 门控**：`erp-mnt.spare-part-posting-enabled` 默认 false → 测试类 `@NopTestProperty(name="erp-mnt.spare-part-posting-enabled", value="true")`（容器启动前声明，对齐 C06/C08 `@NopTestProperty` 范式）。
- C09 完工门控：`erp-mfg.inspection-gate-enabled` 默认值以实仓为准（若默认关闭则自包含建 BOM `inspectionRequired=true` + 门控开启的生效路径须核实；若须 @NopTestProperty 开启则同型处理并登记）。
- 无端口/密钥/外部服务/环境变量新增；surefire 串行化已在 app-erp-all pom 落地。

## Execution Plan

### Phase 1 — C09 质检门控与 NCR/CAPA/SCRAP 闭环（混合类型：1/3 Add + 1/3 Proof + 1/3 Decision，逐项标注）

Status: completed
Targets: `app-erp-all/src/test/java/io/nop/app/all/it/TestErpC09QaNcrCapaScrap.java`、`app-erp-all/_cases/io/nop/app/all/it/TestErpC09QaNcrCapaScrap/`
Skill: nop-testing

- Item Types: `Add | Proof | Decision`
- Prereqs: M0.1 + M0.2 + B1-B3 done

- [x] Add：**C09 测试类** `TestErpC09QaNcrCapaScrap`（1 类 1 方法）——按设计文档 §6 C09 规格 + `TestErpQaInspectionTrigger`/`TestErpQaNcrCapaEndToEnd`/`TestErpQaNcrPosting` 业务参照：自包含检验单（REJECTED）→ 完工门控阻断（错误码断言：**`erp.mfg.inspection-required`（mfg 实仓唯一抛点，`ErpMfgWorkOrderReportCompletionProcessor`）**）→ NCR save → submitReview → resolve（CAPA 三步或 noCapaReason 门控）→ `ErpQaNonConformance__postNcr`（SCRAP 处置过账：凭证借贷平衡 + 库存扣减）→ 通知断言（按下方裁决落地）；静态载荷走 `input/N_step.json5` + `addVar`/`@var:` 动态 id。
  - Skill: nop-testing
- [x] Decision：**NCR 通知断言路径裁决**——核实实仓 NCR 状态变更是否触发 `ErpSysNotification` 生成（qa 域主代码未见直连调用；通知触发可能在 notify 域订阅机制或尚未实现）。选择 = 可达断言路径（如经 `IErpSysNotificationBiz.findUnread`/`markRead` 或 notify 域既有机制），备选 = 设计文档 §6 C09 步骤 4 勘误登记 + 改以可达替代断言；残留风险记录。同时裁决完工门控负路径的确切阻断点（close vs reportCompletion，对齐 B4 C07 完工驱动动作结论）。
  - Skill: nop-testing
- [x] Proof：**C09 RECORDING→CHECKING 往返**——RECORDING 录制 → CHECKING 复跑全绿；层 1 锚点：REJECTED 检验阻断完工（错误码断言）、NCR 门控（未闭 CAPA 被拒）、SCRAP 凭证借贷平衡、库存扣减数量正确、通知记录存在且可标记已读（按裁决路径）。
  - Skill: nop-testing

Exit Criteria:

- [x] C09 类存在且 RECORDING→CHECKING 往返绿（失败模式 = 任一断言/快照 diff 不通过）
- [x] 通知断言路径裁决落盘（含设计文档 §6 C09 勘误登记，若实仓无 NCR→notification 机制）

### Phase 2 — C10 维护工单与备件消耗过账（混合类型：1/3 Add + 1/3 Proof + 1/3 Decision，逐项标注）

Status: completed
Targets: `app-erp-all/src/test/java/io/nop/app/all/it/TestErpC10MntRequestSparePart.java`、`app-erp-all/_cases/io/nop/app/all/it/TestErpC10MntRequestSparePart/`
Skill: nop-testing

- Item Types: `Add | Proof | Decision`
- Prereqs: Phase 1（同基类同装配，先例链打通后无新机制风险）

- [x] Add：**C10 测试类** `TestErpC10MntRequestSparePart`（1 类 1 方法）——按设计文档 §6 C10 规格 + `TestErpMntDowntimeAndE2E`/`TestErpMntSparePartPosting` 业务参照：自包含维护请求 save（OPEN，引用 seed 设备 EQ-2026-001）→ accept（生成 DRAFT Visit）→ startRepair → complete（六态终态）→ `ErpMntVisit__complete`（访问完成 + visit_task）→ 备件消耗 `ErpMntSparePartUsage__confirm`（@NopTestProperty 开启 `erp-mnt.spare-part-posting-enabled`）→ MAINTENANCE_ISSUE 过账（凭证借贷平衡 + 备件库存扣减）→ `ErpAstAsset__get`（设备关联资产 AST-2026-002 核对）+ 非法迁移守卫断言；静态载荷走 `input/N_step.json5` + `addVar`/`@var:` 动态 id。
  - Skill: nop-testing
- [x] Decision：**过账门控配置裁决**——`erp-mnt.spare-part-posting-enabled` 默认 false（实仓 `DEFAULT_SPARE_PART_POSTING_ENABLED=false`）→ 采用 `@NopTestProperty` 开启断言 MAINTENANCE_ISSUE 凭证（对齐 C08 simulation 门控同型）；记录备选（seed/配置层开启）与残留风险（门控默认关闭态下凭证不存在，不作断言源）。
  - Skill: nop-testing
- [x] Proof：**C10 RECORDING→CHECKING 往返**——RECORDING 录制 → CHECKING 复跑全绿；层 1 锚点：Request 状态机终态 COMPLETED、MAINTENANCE_ISSUE 凭证借贷平衡、备件库存扣减、设备状态/资产字段联动、非法迁移被拒（守卫断言）。
  - Skill: nop-testing

Exit Criteria:

- [x] C10 类存在且 RECORDING→CHECKING 往返绿（失败模式 = 任一断言/快照 diff 不通过）
- [x] 过账门控配置裁决落盘（含设计文档 §6 C10 勘误登记，若与实仓门控键不一致）

### Phase 3 — 收尾与回归（Proof-heavy）

Status: completed
Targets: `docs/backlog/integration-test-roadmap.md`（B5 → done + 头「最后更新」注记）、`docs/logs/2026/08-24.md`、`docs/testing/known-good-baselines.md`（差量登记）、`docs/design/integration-testing.md`（§6 C09/C10 勘误登记，若 Phase 1/2 触发）
Skill: none

- Item Types: `Proof | Add`
- Prereqs: Phase 2

- [x] Proof：**局部回归** `mvn test -pl app-erp-all` 全绿（含 2 个新用例类，串行 fork 单 JVM 顺序执行）+ **全量回归** `mvn test`（全 reactor）零新增失败 + `mvn clean install -DskipTests` BUILD SUCCESS——对照 known-good-baselines（B4 后基线）口径，+2 tests / +2 报告文件全额归因 B5，零新增失败；**clean 前先拷贝 surefire 计数/XML 证据**（B3 closure MINOR-2 流程改进兑现）。
  - Skill: none
- [x] Add：roadmap B5 `todo` → `done` + 头「最后更新」注记 + `docs/logs/2026/08-24.md` 日志条目（按日志书写指南）+ known-good-baselines 差量登记 + 设计文档 §6 勘误登记（若触发，含 C10「状态机 5 态」→六态漂移点）。
  - Skill: none

Exit Criteria:

- [x] 局部 + 全量回归零新增失败（与已知基线口径一致）
- [x] roadmap B5 = done + 日志条目存在 + 基线差量登记 + 勘误登记（若触发）

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_fd0318f22ffeM4q9aeDzngZbxo，独立 general 子代理新会话) — 0 BLOCKER / 1 MAJOR / 3 MINOR。事实核验全过（qa/mnt 先例、config 键、9 seed 文件、QA 主代码无 NCR→ErpSysNotification 直连、门控抛点 = reportCompletion 非 close、基线计数）。MAJOR-1 `ErpMntRequestStateMachine` 实为**六态**（OPEN/ACCEPTED/IN_PROGRESS/COMPLETED/REJECTED/CANCELLED）非五态 → Baseline 已修订 + Phase 3 补记设计文档 §6 C10「5 态」漂移勘误义务；MINOR-2 qa 侧 `ERR_INSPECTION_MANDATORY_BLOCKED` 已定义无抛点（唯一抛点 = mfg `ERR_INSPECTION_REQUIRED` @ `ErpMfgWorkOrderReportCompletionProcessor`）→ Baseline/Add 表述已修订；MINOR-3 范本计数 6→7 → 已修订；MINOR-4 Phase 3 第二项类型 `Fix`→`Add`（文档/状态回写，对齐 B2 审查先例 MINOR-3）→ 已修订。
- Independent draft review iteration 2: needs revision（1 残留 MINOR，ses_fd02df2acffeGy1QmZ33spzUHz，独立 general 子代理新会话）——4/4 声明修复核验（MAJOR-1 部分落地：Baseline 已六态，但 Goals/Phase 2 残留「五态」表述致计划内部矛盾）。残留 MINOR-1 Goals L30 + Phase 2 L87「五态」→「六态」已修订（文本一致性规则 11 类）；其余 MINOR-2/3/4 全部确认 FIXED。0 BLOCKER / 0 MAJOR。
- Independent draft review iteration 3: accept (ses_fd0281bcaffeUvRnipifzbsk4x，独立 general 子代理新会话) — 0 BLOCKER / 0 MAJOR / 0 MINOR。迭代 2 残留 MINOR-1 完整修复核验（2/2 声明位置「五态」→「六态」实仓复核：六态 + 7 迁移边 + COMPLETED 终态；残留「5 态」仅存于设计文档漂移引述/勘误义务/审查历史三处合法语境）；计划内部一致性（Baseline/Goals/Phase 2/Phase 3 四处统一六态）+ 结构范围合规全过。**共识达成，计划可执行。**

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处：结束时运行 `typecheck`/`build`/`lint`/`test` 一次。

- [x] 范围内行为完成（C09/C10 两用例三层全比对全绿 + 门控/通知裁决）
- [x] 相关文档对齐（设计文档 §6 ↔ 用例实现 ↔ roadmap B5 ↔ e2e-runbook ↔ known-good-baselines 无矛盾）
- [x] 已运行验证（`mvn test -pl app-erp-all` 全绿 + 全量 `mvn test` 零新增失败 + `mvn clean install -DskipTests` BUILD SUCCESS）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

- （执行后按需填写——候选：seed 修正授权触发时的重录耗时测量（B1/M0.2 watch-only residual 触发条件登记）；无范围内降级项则留空）

## Closure

Status Note: C09/C10 两用例 RECORDING→CHECKING 往返全绿 + 局部回归 app-erp-all 40/0/0/1 + 全量回归 3820/0/0/1（+2 tests / +2 报告文件全额归因 B5，零新增失败；surefire 证据已拷贝 `_tmp/b5-surefire-evidence/`）+ `mvn clean install -DskipTests` 156 模块 BUILD SUCCESS；执行期裁决（完工门控阻断点 = reportCompletion、NCR 通知可达断言路径、SCRAP 不扣物理库存、C10 六态状态机、visit_task 仅 PLANNED 访问、备件库存自包含建数）落盘测试类 javadoc + 设计文档 §6 C09/C10 勘误登记；roadmap B5 done + 日志 + known-good-baselines 差量登记均完成。

Closure Audit Evidence:

- Auditor / Agent: 独立 general 子代理（新会话 `ses_fcf81ed3fffeIYIfSFd1QqKhR9`，fresh 会话无执行者上下文）
- Evidence: 独立审计逐项核验 5 域（计划一致性 / 测试代码合规 / 验证证据（实跑 `mvn test -pl app-erp-all -Dtest=TestErpC09QaNcrCapaScrap,TestErpC10MntRequestSparePart` BUILD SUCCESS 2/0/0，surefire 两用例各 1/0/0 + 证据目录 `_tmp/b5-surefire-evidence/` 在位）/ 文档一致性（roadmap B5 done + known-good-baselines 3820/0/0/1/629 + app-erp-all 40/0/0/1 + 设计文档 §6 C09/C10 勘误 + 日志）/ 无范围内降级（git status 零 module-*/model/src/main/seed 变更））——功能证据 5/5 PASS，唯一 MAJOR-1 = 审计时点 Closure Gates 未勾选 + Closure 段占位（本段即其兑现）；总评 0 BLOCKER，**接受**。

Follow-up:

- 无（非阻塞跟进项：C09/C10 响应快照跨 run 时间戳 @var 索引/millis 合并不稳定 → `*` 通配处置已随本计划落地；B6 分批（C11/C12）已由 roadmap B6 行承接）