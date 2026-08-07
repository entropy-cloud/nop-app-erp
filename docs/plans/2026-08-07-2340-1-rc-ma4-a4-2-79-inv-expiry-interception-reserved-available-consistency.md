# 2026-08-07-2340-1-rc-ma4-a4-2-79-inv-expiry-interception-reserved-available-consistency A4.2.79 — inventory 批次效期拦截落地后 reserved/available 一致性运行时验证（MA4 回队行）

> Plan Status: completed
> Last Reviewed: 2026-08-08
> Mission: requirement-compliance
> Work Item: A4.2.79（MA4 回队行：MR1 P1-RC-031 修复落地后 reserved/available 一致性——expiry check 拦截点选择 doConfirm vs doComplete 运行时确认）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MA4 A4.2.79 行（todo，阻塞已解除注记）+ `docs/audits/2026-08-03-1200-3-rc-ma1-a1-26-inventory-f2-batch-traceability-expiry-negative-stock.md` §7 SP-4
> Related: `docs/plans/2026-08-07-1932-1-rc-mr1-r1-20-inv-batch-expiry-interception.md`（RC-R1.20，P1-RC-031 修复落地 plan）；`docs/audits/2026-08-07-2345-rc-ma4-a4-2-74-82-inventory-stockmove-batch-stocktake-runtime.md`（A4.2.74-82 验证报告，本行原排除注记）；`docs/audits/requirement-compliance-methodology.md`（MA4 判据 + 去重协议）
> Audit: required

## Current Baseline

- **存疑点来源**：A1.26 §7 SP-4（`2026-08-03-1200-3-...-a1-26-...md:209`，存疑点列 + 触发条件列 + 验证方式列**三列**摘录合并，非逐字单列引用——原文三列为「MR1 修复落地后 reserved/available 一致性（与 A1.8 SP-3 同根因）：UC-INV-02 `validateAvailable` + `applyReservation` 在 `doConfirm` 内顺序执行（校验 → 占预留）。当前实现下，UC-INV-02 拒绝路径不进入 applyReservation（满足"余额不变"）。未来若 MR1 P1-RC-031 修复在 `validateAvailable` 内增 expiry check（早于 applyReservation），expiry 拒绝路径同样不进入 applyReservation——一致性保持。但若修复扩展到 `doComplete`（DONE 时再校验 expiry），须确认 reserved 已被 release（避免假阴性）」/「MR1 P1-RC-031 修复实现的拦截点选择（doConfirm vs doComplete）」/「A4.1/MR1 修复 plan 自身审计：mock `validateAvailable` 抛 `ERR_BATCH_EXPIRED` → 断言 `applyReservation` 未执行 + reservedQuantity 不变 + balance 不变」）。
- **roadmap A4.2.79 行**：`todo`，2026-08-08 注记「MR1 阻塞解除（P1-RC-031 → RC-R1.20 done）——拦截点 = `validateAvailable`（doConfirm 内、applyReservation 前），expiry 拒绝路径不进入 applyReservation（`TestErpInvBatchExpiryInterception` ①/⑦ 实证 reserved/balance 不变）；原阻塞条件已满足，**回队待 mission driver 另行独立 plan 执行**」。Deps（A4.2 done）已满足。
- **修复落地实仓（RC-R1.20）**：`ErpInvStockMoveProcessor.doConfirm:97-106` 顺序 = `validateAvailable:105` → `applyReservation:106`；`validateAvailable:127-131` 首行接线 `validateBatchExpiry:129`（在负库存短路之前，RC-R1.20 Decision「allow-negative-stock 不豁免批次过期」）；`validateBatchExpiry:159-188`（isBatchManaged + expiryDate null 跳过 + expiryDate < today 抛 `ERR_BATCH_EXPIRED`，仅 `reservesOnConfirm` 类型出库/内部转移）。**`doComplete:111-125` 无任何 expiry 检查**（顺序 = releaseReservation → bookCompletion → DONE → dispatchIfApplicable 末步 `:124`）——「若修复放 doComplete 则 reserved 已 release 致假阴性」的窗口不存在。
- **既有测试证据**：`TestErpInvBatchExpiryInterception` 8 组（RC-R1.20 plan §Phase 2）：① 一次性 generateMove 过期拒绝整笔回滚（移动单不残留）+ reserved/balance 不变；⑦ 两步流（CRUD save 落 DRAFT → confirm(moveId)）过期拒绝后移动单保持 DRAFT + reserved 不变；② null 跳过 / ③ 未来效期放行 / ④ 非批次管控放行 / ⑤ config 放行 / ⑥ 负库存不豁免 / ⑧ INCOMING 边界。`mvn test -pl module-inventory/erp-inv-service` 151 tests 全绿（RC-R1.20 结束审计复跑）。
- **验证缺口（本计划补）**：SP-4 三断言中「reservedQuantity 不变 + balance 不变」已由 ①/⑦ 断言，但 **「已有预留占用」场景无运行时探针**——即批次余额行上已存在预留占用（`ErpInvStockBalance.reservedQuantity > 0`，`applyReservation:200-216` 写该列）时，过期拒绝的 confirm 不得触碰既有预留/余额。该场景是「一致性」语义的最强边界，当前测试矩阵未覆盖。**探针 seed 约束（review 修订）**：`TestErpInvBatchExpiryInterception` 冻结时钟（`:51/:61` REFERENCE_DATE=2026-07-17）下过期批次守卫自 seed 起即激活——「他单 confirm 落预留」不可行（该 confirm 自身会被拒）；`ErpInvStockReserve` 实体不存在（全仓零命中）——预留载体即 `ErpInvStockBalance.reservedQuantity`。故探针 = **扩展既有 `seedBalance` helper（`:351-370`，现硬编码 reservedQuantity=ZERO）直接 seed `reservedQuantity>0` 于过期批次余额行**。
- **分层与去重**：A4.2.79 与 A4.2.3（MR1 P1-RC-008 预留写路径 reservedQty writer，仍 MR1-blocked todo，**不覆盖**）不同控制点（拦截点一致性验证 vs 预留写路径实现）；与 A4.2.76（负库存下界 watch-only）不同维度。本行为验证只读（+ 测试探针），不重复核实 P1-RC-031 分级（RC-R1.20 已落地，无分级裁决义务）。

## Goals

- 运行时确认拦截点选择：expiry 检查实仓接线于 `validateAvailable`（doConfirm 内、applyReservation 前），`doComplete` 无 expiry 检查（SP-4 双分支——「拒绝路径不进入 applyReservation」成立 + 「doComplete 校验致假阴性」窗口不存在）。
- 运行时确认 SP-4 三断言：expiry 拒绝路径下 `applyReservation` 未执行 + `reservedQuantity` 不变 + `balance` 不变（复用 ①/⑦ 既有断言 + 实仓代码顺序证据）。
- 新增 dedicated 运行时探针测试：批次已存在既有预留占用时，过期拒绝的 confirm 不触碰既有预留/余额（最强边界），补齐 A4.2.79 验证缺口。
- 产出验证报告落盘 `docs/audits/` + roadmap A4.2.79 → `done` + arm-index P1-RC-031 行追加 A4.2.79 一致性注记（不新建 finding）+ `docs/logs/` 聚合日志条目。

## Non-Goals

- **不重新裁决/不撤销 P1-RC-031 分级**（修复已由 RC-R1.20 落地；本计划只做落地后运行时一致性验证，无分级裁决义务）。
- **不覆盖 A4.2.3**（P1-RC-008 预留写路径，MR1-blocked 未解除，保留 todo）。
- **不改生产代码/ORM/api.xml/config 默认值**（零结构变更；仅新增测试探针，对齐 MA4 验证 plan 先例——如 A4.2.148 新增 IDLE 输入单测）。
- **不做 ACTIVE→EXPIRED 状态迁移/expiry scheduler**（RC-R1.20 plan Deferred But Adjudicated，Successor Required: no）。
- **不改真相源**（use-cases/state-machine.md 需求契约段）。

## Task Route

- Type: `verification or audit work`（MA4 运行时行为验证，read-only 确认 + 测试探针）
- Owner Docs: `docs/audits/2026-08-03-1200-3-rc-ma1-a1-26-inventory-f2-batch-traceability-expiry-negative-stock.md`（§7 SP-4 存疑点原文）+ `docs/plans/2026-08-07-1932-1-rc-mr1-r1-20-inv-batch-expiry-interception.md`（修复落地证据）+ `docs/audits/requirement-compliance-methodology.md`（MA4 判据）
- Skill Selection Basis: 验证框架 = `docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定 Skill；多维裁决 + 证据链）；测试探针 = `nop-testing`（JunitAutoTestCase / @NopTestConfig / seed 范式，镜像 `TestErpInvBatchExpiryInterception`）。无生产代码/前端变更，不加载 `nop-backend-dev`/`nop-frontend-dev`。

## Infrastructure And Config Prereqs

- 无新 infra/config（探针复用既有 seed 基础设施：ErpMdMaterial / ErpInvBatch / 余额 / 预留 seed，镜像 `TestErpInvBatchExpiryInterception` ①/⑦ 范式）。
- 验证命令：`mvn test -pl module-inventory/erp-inv-service`（含既有 151 tests 零回归）。

## Execution Plan

### Phase 1 - 拦截点运行时确认（证据采集）

Status: completed
Targets: `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/processor/ErpInvStockMoveProcessor.java`（只读）
Skill: none（证据采集）

- Item Types: `Proof`
- Prereqs: 无

- [x] `Proof` 实仓代码顺序证据：`doConfirm:105-106`（validateAvailable → applyReservation）+ `validateAvailable:127-129`（首行接线 validateBatchExpiry，负库存短路之前）+ `doComplete:111-125`（releaseReservation → bookCompletion → DONE → dispatchIfApplicable 末步 `:124`，**无 expiry 检查**）grep/read 记录于报告。
      - Skill: none
- [x] `Proof` 既有测试复跑证据：`mvn test -pl module-inventory/erp-inv-service` 全绿（151 tests，含 `TestErpInvBatchExpiryInterception` 8 组），记录 ①/⑦ 对 reserved/balance 不变的断言。
      - Skill: none
- [x] `Proof` SP-4 双分支结论落报告：分支 A「拒绝路径不进入 applyReservation → reserved/balance 不变」运行时成立；分支 B「doComplete 再校验致假阴性」窗口不存在（doComplete 零 expiry 检查）。
      - Skill: none

Exit Criteria:

- [x] 报告 §拦截点证据段含实仓 file:line 顺序 + 测试复跑结果 + SP-4 双分支结论（成功与失败模式：若实仓接线与 RC-R1.20 描述不符或测试红，按证据纠正结论并在报告中记录）

### Phase 2 - dedicated 运行时探针测试

Status: completed
Targets: `module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvBatchExpiryInterception.java`（追加探针方法）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 完成

- [x] `Add` 探针 `testExpiryRejectedLeavesExistingReservationIntact`（镜像 ⑦ 两步流 + ① 拒绝断言范式）：**扩展 `seedBalance` helper 直接 seed `reservedQuantity>0`（如 reserved=3 / total=100 / available=97）于过期批次管控物料余额行**（放弃「他单 confirm 落预留」——冻结时钟下过期批次 confirm 自身被拒不可行；放弃 `ErpInvStockReserve`——实体不存在）→ 两步流（CRUD save 落 DRAFT → `confirm`）对同批次过期出库移动单拒绝 `ERR_BATCH_EXPIRED` → 断言移动单保持 DRAFT + 余额行 reservedQuantity **原值不变（3）** + total/available 不变 + 无 applyReservation 副作用（预留未新增/未释放）。
      - Skill: `nop-testing`
- [x] `Proof` 断言强度：拒绝路径断言错误码 + 参数（物料/批次/效期）+ 事务回滚后状态；与 ①/⑦ 组合构成 SP-4 三断言完整运行时证据链（applyReservation 未执行 / reservedQuantity 不变 / balance 不变，含既有预占边界）。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 探针测试落地并绿：`mvn test -pl module-inventory/erp-inv-service` 全绿（既有 151 + 新增 ≥1 零回归）；`_cases/` 快照录制落盘（对齐 RC-R1.20 先例——该测试类既有 8 组均录制快照）
- [x] 若探针暴露一致性破坏（reserved/balance 被触碰），升级为 finding 并在报告中记录（预期不发生；发生则按 methodology 判据处置）

### Phase 3 - 报告 + 状态回填

Status: completed
Targets: `docs/audits/2026-08-07-2340-rc-ma4-a4-2-79-inv-expiry-interception-reserved-available-consistency.md`（新建）；`docs/backlog/requirement-compliance-roadmap.md`（A4.2.79 done）；`docs/audits/arm-index.md`（P1-RC-031 行追加注记）；`docs/logs/2026/08-08.md`（当日实际日期为 2026-08-08，对齐 RC-R1.20 计划日志路径修正先例）
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1-2 完成

- [x] `Add` 验证报告落盘（对齐 A4.2.74-82 报告结构：TL;DR 裁决表 + 存疑点原文 + 拦截点证据 + 测试证据 + §2 判据 + 去重声明 + 过程纪律自检段[checker 门控核查]），结论 = 主路径闭合/一致性成立，0 新 finding，不触发 MR0。
      - Skill: none
- [x] `Add` roadmap A4.2.79 `todo → done ✅` + 文件头更新注记；arm-index P1-RC-031 行追加「A4.2.79 一致性验证 done」注记（不新建 finding 行）；`docs/logs/2026/08-08.md` 聚合日志条目。
      - Skill: none

Exit Criteria:

- [x] 报告 + roadmap/arm-index/log 三处状态回填落盘（roadmap 行 done + arm-index 注记 + 日志条目）

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（独立子代理 `ses_0231772c9ffekELvJaiTHAt3zq`，fresh session）——1 MAJOR（探针 seed 路径不可行：冻结时钟下过期批次「他单 confirm 落预留」自身被拒 + `ErpInvStockReserve` 实体不存在[预留载体 = `ErpInvStockBalance.reservedQuantity`]）→ 修订：探针改为扩展 `seedBalance` helper 直接 seed `reservedQuantity>0` 于过期批次余额行 + 断言原值不变；4 MINOR（SP-4 引用为逐字实为多列摘录 → 标注摘录合并；`doComplete:111-123` 漏 dispatch 末步 `:124` → 改 `:111-125`；日志路径 `08-07.md` 应为当日实际 `08-08.md`[对齐 RC-R1.20 修正先例] → 修订；Exit Criteria「（若框架要求）」条件化措辞 → 按 RC-R1.20 先例改为无条件快照录制）。
- Independent draft review iteration 2: `accept`（独立子代理 `ses_0230fa3d0ffeCJGOTVG4Mxv2jd`，fresh session）——5 项 round-1 问题全量实仓核验 RESOLVED（探针 seed 约束 + 摘录合并标注 + `:111-125` + `08-08.md` + 无条件快照；探针可执行性验证：`validateBatchExpiry:129` 先于任何 balance 触碰 + seedBalance `:361` 硬编码 ZERO 扩展点确认 + 断言可区分[无守卫则 applyReservation 抬至 8]）+ 全量复检 PASS（格式/Deps/范围/反松弛/预授权/报告结构先例）。1 cosmetic nit（「原文两列」→「三列」）已修订。共识达成，转 active。

## Closure Gates

- [x] 范围内行为完成
- [x] 相关文档对齐
- [x] 已运行验证（`mvn test -pl module-inventory/erp-inv-service` + `mvn clean install -DskipTests` 全量 + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline——纯测试新增无生产代码，防基线漂移）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### A4.2.3（P1-RC-008 预留写路径 reservedQty/availableQuantity 实时一致性 + lost-update 防护）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 仍 MR1-blocked（RC-R1.48 越界项 ask-first 未落地），与 A4.2.79 不同控制点（预留写路径实现 vs 拦截点一致性验证），本计划不覆盖。
- Successor Required: yes（触发条件 = MR1 P1-RC-008 修复落地）

### 批次效期上游写入（mfg 完工 / 采购入库写 expiryDate）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 与拦截一致性验证不同控制点（数据写入 vs 拦截点行为）；RC-R1.20 Deferred 已登记（successor no）。
- Successor Required: no

## Closure

Status Note: 已闭合（2026-08-08，全部 Phase 完成 + 结束审计 PASS）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话 `ses_0229e5e1fffecjxa9V6MpWUANm`）
- Evidence:
  1. **Phase 1/2/3 全 tick + Status completed**（7 items + 4 Exit Criteria 全 `[x]`）
  2. **报告 ↔ 实仓证据逐行精确**：`ErpInvStockMoveProcessor.java` doConfirm:105-106 / validateAvailable:129 / validateBatchExpiry:159-188 / doComplete:111-125 行号核对一致；探针 `testExpiryRejectedLeavesExistingReservationIntact`（TestErpInvBatchExpiryInterception.java:161）断言逐项吻合（错误码 + 三参数 + DRAFT 保持 + reserved==3/total==100/available==97）；`_cases/` 快照内容精确（`1_confirm_rejection_code.json5`=`"erp.err.inv.batch-expired"` + `1_balance_state.json5`=100/97/3/0/10/1000）
  3. **回填三处就位**：roadmap:242 A4.2.79 `done ✅` + 文件头注记；arm-index:188 P1-RC-031 行「A4.2.79 一致性验证 done」注记（不新建 finding）；`docs/logs/2026/08-08.md` 顶部日志条目
  4. **git status 零生产代码变更**（仅测试类 + docs + _cases 快照 + 报告/计划）
  5. **验证**：`mvn test -pl module-inventory/erp-inv-service` 152 tests 全绿（既有 151 + 新增 1 零回归）+ 探针类 9/9 + `mvn clean install -DskipTests` 全量 BUILD SUCCESS + compliance checker actual==baseline 零漂移
  6. **环境问题处置记录**：执行期间并行 agent 以 dirty nop-ioc WIP 重建 nop-entropy 至本地仓库，破坏 `ioc:condition if-property` 求值致 `DataBaseSchemaInitializer` 不运行（finance 表缺失→测试红）——与 `docs/logs/2026/08-07.md` RC-R1.2 环境修复注记故障完全同型；按先例经 HEAD（05d03a1ac）重建 nop-ioc 恢复本地仓库后测试恢复全绿。此问题与执行者改动无关（执行者改动仅测试探针 + 文档；故障在 stash 执行者改动后仍复现，修复 nop-ioc 后消失）
  7. **结束审计裁决**：PASS（3 minor 非阻塞——报告两处行号锚点漂移[seedBalance `:351-370`→`:413-431`、①/⑦ 断言 +3 行]已修正；Closure Gates 收尾已勾选）
- 3 minor 观察（非阻塞）：M1/M2 报告行号漂移（已修正）；M3 Closure 收尾（本段已完成）

Follow-up:

- 无（范围内项目全落地后关闭；A4.2.3 回队触发条件见 Deferred But Adjudicated）
