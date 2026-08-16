# 2026-08-16-0424-3-rc-ma4-a4-2-3-mfg-reservation-runtime-confirmation A4.2.3 — mfg 预留写路径落地后 reserved/available 一致性 + 跨工单并发预留运行时验证（MA4 回队行）

> Plan Status: completed
> Last Reviewed: 2026-08-16
> Mission: requirement-compliance
> Work Item: A4.2.3（MA4 回队行：MR1 P1-RC-008 预留写路径落地后 reservedQty/availableQuantity 实时一致性 + 跨工单并发预留 lost-update 防护运行时核验）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MA4 A4.2.3 行（todo，2026-08-16 回队解锁注记）+ `docs/audits/2026-08-02-2042-2-rc-ma1-a1-8-mfg-f1-mrp-drp-engine.md` §7 SP-3 + `docs/audits/2026-08-02-2042-3-rc-ma1-a1-9-mfg-f2-work-order-reporting.md` §7 SP-3
> Related: `docs/plans/2026-08-15-2119-3-rc-mr1-r1-48-mfg-material-reservation-write-path.md`（RC-R1.48 修复落地 plan，本行阻塞解除来源）；`docs/audits/2026-08-06-1926-rc-ma4-a4-2-1-2-mfg-reservation-availability-runtime.md`（A4.2.1/A4.2.2 前置运行时确认）；`docs/design/manufacturing/use-cases.md`（L1 UC-MFG-05 :90-104 + UC-MFG-08 :144-156 + UC-MFG-06 :107-121）；`docs/audits/requirement-compliance-methodology.md`（MA4 判据 + 去重协议）
> Audit: required

## Current Baseline

- **存疑点来源（合并行）**：A1.8 §7 SP-3（`2026-08-02-2042-2-...-a1-8-...md`）+ A1.9 §7 SP-3（`2026-08-02-2042-3-...-a1-9-...md`）合并——「MR1 P1-RC-008 预留写路径 successor 同根因同控制点」：MR1 修复落地后 reservedQty/availableQuantity 实时一致性 + 跨工单并发预留 lost-update 防护运行时核验。
- **roadmap A4.2.3 行**：`todo`，2026-08-16 注记「✅ 回队解锁（2026-08-16，RC-R1.48 修复落地）：预留占/释放/消耗全经 `StockMoveBookkeeper.updateBalanceWithRetry`（versionProp 乐观锁 + UK 冲突 evict+reload+重试 `erp-inv.concurrent-deduct-max-retry`=5），跨工单并发预留 lost-update 防护运行时义务满足（`TestErpInvReservationWriteApi#testConcurrentCreateReservationNoLostUpdate` 多线程并发双写预留断言 4+4=8 无丢失 + available=2）；reservedQty/availableQuantity 实时一致性经 recomputeAvailable 派生 + 新测试断言链（创建/释放/消耗后余额断言全覆盖）。**回队验证义务**：A4.2.3 展开器按「运行时确认」执行——跨工单并发预留真实并发场景 + reserved/available 一致性运行时核验」。Deps（A4.2 done）已满足。
- **修复落地实仓（RC-R1.48）**：`IErpInvReservationBiz` 增 `createReservation`/`releaseReservation`/`consumeReservation` purpose-built 写接口（余额增减经 `StockMoveBookkeeper.updateBalanceWithRetry` 乐观锁重试）；mfg 四接线（approve→`createReservations` / cancel→`releaseReservations` / 完工→`releaseRemainingReservations` / 领料 confirm→`consumeReservations`，config-gated `erp-mfg.reservation-enabled` 默认 true）；`ErpInvStockBalance.reservedQuantity` writer + `availableQuantity` 派生（`recomputeAvailable` = total − reserved − locked）。
- **既有测试证据（RC-R1.48 交付）**：`TestErpInvReservationWriteApi` 10 组（含 `testConcurrentCreateReservationNoLostUpdate`：多线程并发双写预留断言 4+4=8 无丢失 + available=2）+ `TestErpMfgReservationLifecycle` 9 组（①审核创建②取消释放③完工释放④领料消耗⑤超预留放行⑥config 关闭全链跳过⑦头状态终态⑧旧数据 no-op⑨无 BOM 不阻断）；erp-inv-service 218 / erp-mfg-service 269 tests 零回归（RC-R1.48 结束审计复跑）。
- **验证缺口（本计划补齐）**：既有并发测试为**单物料余额行**多线程并发预留（同库存余额行 lost-update 防护，`testConcurrentCreateReservationNoLostUpdate` 写 API 层并发），**未覆盖 mfg 集成层跨工单语义**（两工单同物料并发审核各建预留 + 预留后领料消耗/释放链余额一致性）——「跨工单并发预留真实并发场景」回队义务需**无条件新增**探针（对齐 A4.2.79 探针先例）。
- **分层与去重**：A4.2.3 与 A4.2.119（P1-RC-049 物料归集，仍 MR1-blocked todo，**不覆盖**）不同控制点；与 A4.2.1（预留并发扣减运行时安全确认，本行修复前基线）为前后关系（本行消费其结论 + 新基线）；与 A4.2.79（批次效期拦截 reserved 一致性）不同控制点。本行为验证工作（**生产代码零变更，只读确认生产面 + 新增无条件测试探针**），不重复核实 P1-RC-008 分级（RC-R1.48 已落地，无分级裁决义务）。

## Goals

- 运行时确认跨工单并发预留 lost-update 防护：复跑 `testConcurrentCreateReservationNoLostUpdate`（写 API 层并发，既有断言 4+4=8 无丢失 + available=2）+ **新增跨工单并发探针（无条件落地）**——两工单同物料经 mfg approve 集成层并发建预留（既有测试仅覆盖单余额行写 API 层并发，未覆盖 mfg 集成层跨工单语义，本计划补齐）。
- 运行时确认 reserved/available 一致性：复跑创建/释放/消耗后余额断言链（create→reserved+ / release→reserved− / consume→reserved− 与 total 守恒 + available=total−reserved 恒等式）+ 探针断言（跨工单双预留后 reserved/available 守恒）。
- 产出验证报告落盘 `docs/audits/`（对齐 A4.2.79 报告范式）+ roadmap A4.2.3 → `done` + arm-index P1-RC-008 行追加 A4.2.3 运行时注记（不新建 finding）+ `docs/logs/` 聚合日志条目。

## Non-Goals

- **不重新裁决/不撤销 P1-RC-008 分级**（修复已由 RC-R1.48 落地；本计划只做落地后运行时一致性验证，无分级裁决义务）。
- **不覆盖 A4.2.119**（P1-RC-049 物料归集，MR1-blocked 未解除，保留 todo）。
- **不改生产代码/ORM/api.xml/config 默认值**（零结构变更；仅新增**无条件**跨工单并发测试探针，对齐 MA4 验证 plan 先例——A4.2.148 新增 IDLE 输入单测 / A4.2.79 探针范式）。
- **不覆盖真并发 check-then-act over-commitment 窗口**（A2.17 既有追踪，R1.48 plan Deferred 登记，非本行范围）。
- **不改真相源**（use-cases/state-machine.md 需求契约段）。

## Task Route

- Type: `verification or audit work`（MA4 运行时行为验证，read-only 确认 + 无条件测试探针）
- Owner Docs: `docs/audits/2026-08-02-2042-2-rc-ma1-a1-8-mfg-f1-mrp-drp-engine.md`（§7 SP-3 存疑点原文）+ `docs/audits/2026-08-02-2042-3-rc-ma1-a1-9-mfg-f2-work-order-reporting.md`（§7 SP-3）+ `docs/plans/2026-08-15-2119-3-rc-mr1-r1-48-mfg-material-reservation-write-path.md`（修复落地证据）+ `docs/audits/requirement-compliance-methodology.md`（MA4 判据）
- Skill Selection Basis: 验证框架 = `docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定 Skill；多维裁决 + 证据链）；测试探针 = `nop-testing`（JunitAutoTestCase / @NopTestConfig / seed 范式，镜像 `TestErpInvReservationWriteApi`/`TestErpMfgReservationLifecycle`）。无生产代码/前端变更，不加载 `nop-backend-dev`/`nop-frontend-dev`。

## Infrastructure And Config Prereqs

- 无新 infra/config（探针复用既有 seed 基础设施：ErpMdMaterial / 库存余额 / 预留 / 工单 seed，镜像 `TestErpInvReservationWriteApi` 范式）。
- 验证命令：`mvn test -pl module-inventory/erp-inv-service,module-manufacturing/erp-mfg-service`（含既有 218/269 tests 零回归）。

## Execution Plan

### Phase 1 - 跨工单并发预留 + reserved/available 一致性运行时确认（证据采集）

Status: completed
Targets: `module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvReservationWriteApi.java`（只读复跑）+ `module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgReservationLifecycle.java`（只读复跑 + 新增跨工单并发探针）
Skill: `nop-testing`（探针）

- Item Types: `Proof | Add`
- Prereqs: 无

- [x] `Proof` 既有测试复跑证据：`mvn test -pl module-inventory/erp-inv-service,module-manufacturing/erp-mfg-service` 全绿（inv 218 + mfg 269 tests），记录 `testConcurrentCreateReservationNoLostUpdate`（4+4=8 无丢失 + available=2）与创建/释放/消耗余额断言链输出。
      - Skill: none
- [x] `Proof` 并发防护机制运行时确认：`updateBalanceWithRetry` 版本乐观锁 + UK 冲突重试路径 grep/read 记录（`StockMoveBookkeeper`），与 A4.2.1 前置结论对照（无 silent split-quantity corruption）。
      - Skill: none
- [x] `Add` 跨工单并发探针（无条件）：新增测试（镜像 `TestErpMfgReservationLifecycle` seed 范式）——两工单同物料并发 approve（多线程，对齐 `testConcurrentCreateReservationNoLostUpdate` ExecutorService+CountDownLatch 模式）→ 断言两工单预留均落库 + reservedQuantity 累加无丢失 + available = total − reserved 恒等式保持 + 无异常/无重试耗尽。
      - Skill: `nop-testing`
- [x] `Proof` 报告落盘：`docs/audits/2026-08-16-rc-ma4-a4-2-3-mfg-reservation-write-path-runtime.md`（对齐 A4.2.79 报告结构：结论 + 证据链 + 与 A4.2.1/2 前置衔接 + 探针结果 + 无新 finding 声明）+ roadmap A4.2.3 → done + arm-index P1-RC-008 追加 A4.2.3 注记。
      - Skill: none

Exit Criteria:

- [x] 运行时确认结论落盘：跨工单并发预留 lost-update 防护成立（既有断言链复跑 + 跨工单探针断言双实证）+ reserved/available 一致性恒等式成立；零新 finding / 零分级变更。
- [x] roadmap A4.2.3 → done + arm-index 注记 + `docs/logs/2026/08-16.md` 聚合日志条目（若仅此一个 MA4 行则与 Phase 1 同步完成）。

## Draft Review Record

- Independent draft review iteration 1: needs revision（`ses_ff8e4a24bffeLXO8P3sz3t5sxe`）——跨工单并发探针条件化措辞（「视缺口」/「仅当缺口确认时」）违反反松弛规则 + 欠交付 roadmap 回队义务（既有测试仅覆盖单余额行写 API 层并发）；修订：探针改为**无条件** `Add` 项（两工单同物料并发 approve 断言链）+ Goals/Exit Criteria/基线同步 + A1.9 源文件名修正 + 报告文件具体名 + Closure Gates 增 checker 对比。
- Independent draft review iteration 2: needs revision（`ses_ff8dcfe31ffe01Ohl72yU2R6qh`）——Non-Goals/Current Baseline 残留「视缺口/视需」措辞与无条件契约矛盾；修订：Non-Goals「仅视缺口」→「仅新增无条件跨工单并发测试探针」、验证缺口段「新增无条件探针」。
- Independent draft review iteration 3: needs revision（`ses_ff8d77eddffeSNQjiERHJj2js1`）——Current Baseline「分层与去重」段仍残留「只读确认 + 视需测试探针」第三处「视需」；修订：改为「生产代码零变更，只读确认生产面 + 新增无条件测试探针」，措辞澄清（区别于合法日志聚合时序条件）。（注：Exit Criteria 日志聚合「若仅此一个 MA4 行则与 Phase 1 同步完成」为指南执行时规则 10 的合法时序条件，非探针 slack，保留。）
- Independent draft review iteration 4: acceptable as-is（`ses_ff8d50b85ffepwvIme46CHE1pX`）——全计划 scope 文本无条件化扫描通过（唯一保留条件 = 日志聚合时序合法条件）；文本一致性/命名/条目类型/Skill/Closure Gates 合规。

> **Plan Status 由 draft 转 active（2026-08-16，独立草案审查已收敛）**

## Closure Gates

- [x] 范围内行为完成（Phase 1 全部执行项与退出标准）
- [x] 相关文档对齐（验证报告 + roadmap A4.2.3 done + arm-index 注记）
- [x] 已运行验证：`mvn test -pl module-inventory/erp-inv-service,module-manufacturing/erp-mfg-service` + `bash docs/audits/nop-compliance-checker.sh`（actual == baseline 确认探针零生产面漂移——验证类计划门控自定义依据：本计划零生产代码变更，全量 `mvn clean install` 非本计划门控，对齐 A4.2.79 先例）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 真并发 check-then-act over-commitment 窗口

- Classification: `watch-only residual`
- Why Not Blocking Closure: A2.17 既有追踪（O2C 审计登记）；R1.48 plan Deferred 登记（预留写经 updateBalanceWithRetry 乐观锁防护 lost-update；read-time 校验窗口归 A2.17）。本行为运行时确认不改变该追踪。
- Successor Required: `no`

## Closure

Status Note: 独立结束审计 PASS（2026-08-16，task `ses_ff80f3b5fffezmMPQQCSXjOrm5`）——Phase 1 全部执行项/退出标准/Closure Gates 勾选完整 + 探针代码（TestErpMfgReservationLifecycle:362-436 add-only +95 行）与计划契约逐项核对通过 + 验证独立复跑（inv 218 + mfg 270 tests 全绿 + checker actual==baseline 零漂移）+ 报告/roadmap A4.2.3 done/arm-index 注记/日志齐备 + 零生产代码漂移。审计提示的 3 个非计划工作树文件（`docs/backlog/README.md` id-string P0 行、`id-string-migration-roadmap.md`、`missions/id-string-migration.json`）属并发独立工作项（id-string-migration mission），非本计划产物，提交时按其自身工作项分隔。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，不重用执行者上下文）
- Evidence: task `ses_ff80f3b5fffezmMPQQCSXjOrm5`，8/8 项 PASS（计划一致性 / 探针代码 / 验证证据 218+270+checker 零漂移 / 报告文件 / roadmap A4.2.3 done / arm-index P1-RC-008 注记 / 日志 / 零生产漂移），PASS 裁决 + 1 条非阻塞信息性提示（并发工作项文件分隔，非本计划范围）

Follow-up:

- 无（本计划范围内无阻塞/非阻塞跟进项；真并发 check-then-act over-commitment 窗口已归 A2.17 既有追踪，Deferred But Adjudicated 登记不变）
