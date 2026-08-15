# 2026-08-15-1605-2-rc-mr1-r1-39-mnt-equipment-idle-restore-branch RC-R1.39 — maintenance 设备 IDLE 恢复分支（MR1 第一批纯预授权）

> Plan Status: active
> Last Reviewed: 2026-08-15
> Mission: requirement-compliance
> Work Item: RC-R1.39（P2-RC-061：EquipmentStatusLinker.restoreToRunning 补 IDLE 分支，不改 ORM）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.39 行 + `docs/audits/arm-index.md` P2-RC-061 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md` + 人工裁决 `docs/discussions/2026-08-07-1140-rc-approval-inventory-analysis.md` §7 A4（2026-08-08 生效：P2-RC-061 纯逻辑修复，不改 ORM，按 A2 预授权自动执行）
> Related: `docs/design/maintenance/use-cases.md`（L1 UC-MAIN-03）；`docs/design/maintenance/equipment-integration.md`（§3.2 状态流转 / §3.3 状态联动规则）；`docs/audits/2026-08-08-0135-rc-ma4-a4-2-147-154-maintenance-runtime.md`（A4.2.148 运行时证据 + 新测试）；`module-maintenance/erp-mnt-service/.../support/EquipmentStatusLinker.java`
> Audit: required

## Current Baseline

- **finding P2-RC-061（arm-index 行，UC-MAIN-03 C-IDLE 分支）**：L1（`use-cases.md:57`）「COMPLETED(完成, 设备→RUNNING/IDLE)」+（`:63`）「设备.状态 恢复(RUNNING/IDLE, 取决于排产)」——L1 显式 **RUNNING/IDLE 双分支取决于排产**。L2（`equipment-integration.md §3.3:141`）「恢复为 RUNNING 或 IDLE（根据之前状态）」+ §3.2 状态流转「闲置(IDLE) → 开始维护 → 维护中(MAINTENANCE) → 维护完成」（IDLE 出发的维护应回到 IDLE）。
- **L3 实仓**：`EquipmentStatusLinker.java`（54 行，`module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/support/`）——`linkToUnderMaintenance:24-29` / `linkToDown:31-36` / `restoreToRunning:38-43` 三方法全部经 `changeEquipmentStatus:45-53`（`equipmentBiz.get` → `setStatus(newStatus)` → `updateEntity`）**恒写目标状态**；`restoreToRunning` **恒 `EQUIPMENT_STATUS_RUNNING`，无 IDLE 分支**、无前置状态快照列（ORM `ErpMntEquipment` 仅 status 列，propId=9）；javadoc:16-17 AI 代码层自承「设备无独立持久化的前置状态快照列，故以 RUNNING 作为标准运行态恢复；IDLE 设备恢复为 RUNNING 为已知的简化偏差」——**非 owner doc Deferred 段落，无人工批准痕迹**。
- **调用点 census**：visit 路径——`ErpMntVisitStartProcessor#start:25` 调 `linkToUnderMaintenance`（visit SCHEDULED→IN_PROGRESS 时设备 → UNDER_MAINTENANCE）+ `ErpMntVisitCompleteProcessor#complete:37`（IN_PROGRESS→COMPLETED 后调 restoreToRunning）+ `ErpMntVisitCancelProcessor#cancel:31`；停机路径——`ErpMntDowntimeEntryRecordProcessor:20` 调 `linkToDown` + `ErpMntDowntimeEntryCompleteProcessor:23` 调 `restoreToRunning`（DOWN→恢复）。全链路 config-gated（`erp-mnt.equipment-status-link-enabled` 默认 true）。
- **运行时证据（A4.2.148）**：新增测试 `TestErpMntVisitRequestStateMachine#testVisitCompleteFromIdleEquipmentRestoresRunning` PASS（seed 设备=IDLE → schedule → start → complete → 断言 RUNNING，简化偏差实证）——类级计数 10 tests（A4.2.148 实测），模块级基线经 R1.30/R1.31 后为 **105 tests**（99 基线 + 6 新增）。
- **分级**：P2 watch-only（RUNNING 是「更可用」态，非数据破坏，不触发 MR0）；Q4=(a) 张力声明 + 2026-08-08 人工裁决 A4 确认修复义务（纯逻辑修复预授权，**不改 ORM**——方案 B「preMaintenanceStatus 快照列」被人工排除）。
- **核心设计缺口**：恢复目标「根据之前状态」需要**前态载体**——无 ORM 快照列时，纯逻辑方案必须解决「如何在 restore 时点获知维护开始前的设备状态」。
- **涉及文件**：`EquipmentStatusLinker.java`（修改）+ `TestErpMntVisitRequestStateMachine.java`（测试调整/新增）+ 可能 `ErpMntDowntimeEntry*Processor`（若决策需要显式前态传递，Decision）+ owner doc（equipment-integration.md / state-machine.md 注记）+ arm-index + roadmap + `docs/logs/2026/08-15.md`（回填）。

## Goals

- **restoreToRunning 补 IDLE 分支（P2-RC-061 收敛，owner doc §3.3「恢复为 RUNNING 或 IDLE（根据之前状态）」运行时成立，visit 路径）**：维护访问开始前设备为 **IDLE** → 完成/取消后恢复 **IDLE**；RUNNING 来源 → 恢复 **RUNNING**（§3.2 语义不变）。
- **停机路径行为零变化**：`linkToDown` 不缓存前态 → DOWN/IDLE 来源停机恢复均恒 RUNNING（`equipment-integration.md §4.3`「更新设备状态为 RUNNING」字面语义，P2-RC-061/UC-MAIN-03 范围仅覆盖 visit complete/cancel 分支——停机 IDLE 来源恢复语义不属本行，保持现状）。
- **前态捕获载体（纯逻辑，零 ORM 变更）**：Decision D1——推荐 `EquipmentStatusLinker` 内部 transient 前态缓存（`linkToUnderMaintenance` 时读取设备当前状态捕获 IDLE，restore 时消费），残余风险显式登记。
- **零行为回归**：RUNNING 来源完整生命周期（RUNNING→UNDER_MAINTENANCE→RUNNING）不变；停机 DOWN→RUNNING 不变；config 门控不变。
- **测试**：IDLE 输入单测断言反转（complete → 设备 IDLE）+ RUNNING 回归 + cancel 路径 IDLE 恢复 + downtime DOWN→RUNNING 不变 + 缓存消费/缺失回退行为。
- **零回归**：erp-mnt-service 105 tests 全绿 + 全量构建 + compliance checker 零漂移（零新增 daoFor/import 面——EquipmentStatusLinker 既有 `IErpMntEquipmentBiz` 注入）。
- **回填**：arm-index P2-RC-061 → `done (RC-R1.39)` + roadmap 行 → done ✅ + owner doc 注记（§3.3 双分支已实现 + 残余风险）+ `docs/logs/2026/08-15.md` 日志条目。

## Non-Goals

- **不触 ORM 结构**（人工裁决 A4 明确不改 ORM——`ErpMntEquipment.preMaintenanceStatus` 快照列方案被排除，登记 successor 备选）。
- **不改变停机路径恢复语义**（IDLE 来源停机恢复恒 RUNNING——§4.3 字面语义保持，前态感知化属 successor 登记；P2-RC-061/UC-MAIN-03 范围仅覆盖 visit complete/cancel 分支）。
- **不做跨域排产协调判定**（「取决于排产」字面语义——本行按「根据之前状态」收敛，排产联动属 P1-RC-068/RC-R1.76 跨域越界行范围）。
- **不实现 P1-RC-067**（visit→request 联动 requestId 列——独立越界行 RC-R1.75 须 ask-first）。
- **不改 `ErpMntVisitStartProcessor` / 各 Processor 的编排结构**（除非 D1 裁决需要显式前态传递，见 D1 备选 B）。
- **不做前端 AMIS 接线**。
- **不改真相源契约段落**（use-cases L1 不动）。

## Task Route

- Type: `implementation-only change`（P2 finding 的收敛修复，2026-08-08 §7 A4 人工裁决纯逻辑预授权；Q4=(a) 张力声明下 P2 修复义务确认）
- Owner Docs: `docs/design/maintenance/use-cases.md`（L1 UC-MAIN-03）+ `docs/design/maintenance/equipment-integration.md`（§3.2/§3.3 设备状态契约）+ `docs/design/maintenance/state-machine.md`（§2 设备联动）+ `docs/audits/2026-08-08-0135-rc-ma4-a4-2-147-154-maintenance-runtime.md`（A4.2.148 运行时证据）
- Skill Selection Basis: 实现面 = BizModel/support 组件纯代码逻辑（`nop-backend-dev`——@Inject 非 private 规则 + `IErpMntEquipmentBiz` 跨实体注入 + config 门控既有范式）；测试（`nop-testing`——JunitAutoTestCase + 状态机断言调整，对齐 A4.2.148 新增测试范式）。无 view.xml/xbiz/ORM/会计变更。

## Infrastructure And Config Prereqs

- 无新外部服务/环境变量/config key（复用既有 `erp-mnt.equipment-status-link-enabled` 门控，默认 true 不变）。
- 分域验证前置：`mvn install -DskipTests` 后 `mvn test -pl module-maintenance/erp-mnt-service`。

## Execution Plan

### Phase 1 - 决策裁决（前态捕获载体 + 恢复目标规则）

Status: planned
Targets: 本计划范围裁决（无代码）
Item Types: `Decision`
Skill: `nop-backend-dev`

- [ ] **D1**: 前态捕获载体——
  - **选项 A（推荐）**：`EquipmentStatusLinker` 内部 transient `ConcurrentHashMap<Long, String> priorStatusCache`（包级可见）：**仅 `linkToUnderMaintenance` 前置读取设备当前状态，且仅当 current==IDLE 时缓存**（RUNNING 来源无需缓存，恢复恒 RUNNING）；`linkToDown` **不缓存**（停机路径恢复目标恒 RUNNING——owner doc `equipment-integration.md §4.3` 停机恢复「更新设备状态为 RUNNING」字面语义，IDLE 来源停机恢复语义不在 P2-RC-061/UC-MAIN-03 范围，保持现状）；`restoreToRunning` 消费：缓存命中 IDLE → 恢复 IDLE + 移除条目；未命中 → 恢复 RUNNING（现状行为）。
  - **残余风险登记**：① 容器重启/多实例部署缓存丢失 → 回退 RUNNING（= 现状已接受行为，非新退化）② 缓存写入非事务性——`linkToUnderMaintenance` 所在事务回滚（提交失败）后缓存残留 IDLE 条目，污染该设备**下一次** restore（恢复 IDLE 而非 RUNNING）；缓解 = 恢复语义方向是「更可用态」非数据破坏 + 条目在下一次 `linkTo*` 覆盖或 restore 消费时清除，实际污染窗口受限且方向保守（IDLE 是更保守态）——登记 watch-only ③ 异常路径（维护开始后未走 restore 的悬挂条目）→ 下次该设备 restore 消费（语义正确）或再维护覆盖（正确），无永久泄漏 ④ 缓存大小守卫：`MAX_CACHE_ENTRIES=1024`，超限时清空全表（回退 RUNNING = 现状行为，fail-safe）⑤ 并发同设备双维护 → 既有 @Version 乐观锁兜底（javadoc 已声明）。
  - **选项 B（备选）**：调用方显式传前态——`ErpMntVisitStartProcessor.start` 在调 `linkToUnderMaintenance` 前 `equipmentBiz.get` 读状态并传参——与 A 等价但把读取责任外移，破坏 linker 自包含封装，否决。
  - **选项 C（排除）**：ORM `preMaintenanceStatus` 快照列——人工裁决 A4 明确不改 ORM，登记 successor。
  - 理由记录 + 备选分析 + 残余风险登记于本计划。
- [ ] **D2**: 恢复目标规则 = 前态==IDLE（仅 visit 路径捕获）→ IDLE；其余（RUNNING 来源、停机路径、缓存缺失）→ RUNNING——停机路径因 `linkToDown` 不缓存而结构性恒 RUNNING（§4.3「更新设备状态为 RUNNING」字面语义，行为零变化）。
  - Skill: `nop-backend-dev`
- [ ] **D3**: 测试语义调整——既有 `testVisitCompleteFromIdleEquipmentRestoresRunning`（A4.2.148 新增，当前断言 RUNNING）断言**反转**为 IDLE 并更名 `testVisitCompleteFromIdleEquipmentRestoresIdle`（A4.2.148 引用注记保留）；新增 RUNNING 来源回归断言不变。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] D1-D3 裁决记录于本计划（选择 + 备选 + 理由 + 残余风险），Phase 2/3 实现按裁决执行

### Phase 2 - EquipmentStatusLinker IDLE 分支实现

Status: planned
Targets: `module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/support/EquipmentStatusLinker.java`
Item Types: `Fix`
Skill: `nop-backend-dev`

- [ ] 实现 D1 选项 A：`priorStatusCache`（transient `ConcurrentHashMap<Long, String>` + `MAX_CACHE_ENTRIES=1024` 超限清空 fail-safe）+ `linkToUnderMaintenance` 前置读取设备当前状态（`equipmentBiz.get(String.valueOf(equipmentId), false, context)`）**仅 IDLE 缓存**（覆盖写，key=equipmentId）+ `linkToDown` **不缓存**（D1 裁决）+ `restoreToRunning` 消费（命中 IDLE → 恢复 IDLE + `remove`；未命中 → RUNNING；`remove` 在恢复前执行防并发重复消费）
- [ ] javadoc 更新：去除「IDLE 设备恢复为 RUNNING 为已知的简化偏差」失实声明 → 双分支实现说明 + 残余风险注记（重启/多实例回退 RUNNING + 非事务缓存写残留 + 并发乐观锁兜底）
- [ ] `changeEquipmentStatus` 既有签名/守卫不变（config 门控 + ERR_EQUIPMENT_NOT_FOUND 守卫保留）

Exit Criteria:

- [ ] visit 路径：IDLE 前态经 complete/cancel 后恢复 IDLE；RUNNING 前态恢复 RUNNING（Phase 3 测试断言）
- [ ] 停机路径行为零变化（DOWN→RUNNING；IDLE 来源停机→RUNNING 现状保持）；config 关闭（equipment-status-link-enabled=false）行为不变（零状态写入）

### Phase 3 - 测试

Status: planned
Targets: `module-maintenance/erp-mnt-service/src/test`（TestErpMntVisitRequestStateMachine + TestErpMntDowntimeAndE2E）
Item Types: `Fix | Proof`
Skill: `nop-testing`

- [ ] `testVisitCompleteFromIdleEquipmentRestoresRunning` 断言反转 + 更名 `testVisitCompleteFromIdleEquipmentRestoresIdle`（A4.2.148 注记保留）：IDLE → schedule → start（linkToUnderMaintenance 捕获 IDLE）→ complete → **断言 IDLE**（实现前该测试 RED，实现后 GREEN——Proof 步骤）
- [ ] 新增 RUNNING 来源回归：RUNNING → schedule → start → complete → 断言 RUNNING（既有 `testVisitHappyPathWithEquipmentLink` 已覆盖——重跑佐证，断言不变）
- [ ] 新增 cancel 路径：IDLE → start → cancel → 断言 IDLE（恢复语义经 cancel 同样成立）
- [ ] 停机路径回归（重跑既有，断言不变——D1 裁决后行为零变化）：`TestErpMntDowntimeAndE2E#testDowntimeRecordSetsDownAndCompleteRestores`（DOWN→RUNNING）+ DOWN 来源 seed 测试（`TestErpMntVisitRequestStateMachine` DOWN seed → complete → RUNNING）
- [ ] 新增缓存消费/回退边界：缓存缺失（模拟重启语义——直接构造 restore 无前置 linkTo*）→ 恢复 RUNNING 不回退为异常（watch-only 语义断言）

Exit Criteria:

- [ ] 上述测试全部 GREEN + erp-mnt-service 既有 105 tests 零回归
- [ ] 分域 `mvn test -pl module-maintenance/erp-mnt-service` 全绿

### Phase 4 - 验证与回填

Status: planned
Targets: 全量构建 + checker + arm-index + roadmap + owner doc + docs/logs
Item Types: `Proof | Follow-up`
Skill: `none`

- [ ] Proof: 全量 `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh` actual==baseline 零漂移（零新增 daoFor/import 面，R2c=1399 / R10=9 不变）
  - Skill: `nop-testing`
- [ ] arm-index P2-RC-061 → `done (RC-R1.39)`（含修复落地摘要：D1 载体裁决 + 双分支实现 + 残余风险注记）
- [ ] roadmap RC-R1.39 行 → done ✅（含落地摘要）
- [ ] owner doc 注记：equipment-integration.md §3.3「恢复为 RUNNING 或 IDLE（根据之前状态）」补实现注记（IDLE 分支已实现 + transient 缓存载体 + 重启/多实例残余风险 + ORM 快照列 successor 备选）
- [ ] `docs/logs/2026/08-15.md` 顶部追加本计划落地日志条目（格式见 `docs/logs/00-log-writing-guide.md`）

Exit Criteria:

- [ ] 全量构建 + checker 零漂移 + 回填完成且与 roadmap/arm-index/owner doc/logs 四源一致

## Draft Review Record

- Independent draft review iteration 1: needs revision (`ses_ffb8aa1a6ffec8UcyqAi1HPaEx`) because 1 blocking issue — B1 `linkToDown` 亦缓存 IDLE 致停机路径行为变更（IDLE 来源停机恢复 IDLE 与 owner doc §4.3「更新设备状态为 RUNNING」矛盾 + D2 内部不一致 + 无测试覆盖 + 超工作项范围），须限定 IDLE 捕获仅 visit 路径；N1-N7 非阻塞（Item Types / 行号 complete:37 cancel:31 / 非事务缓存写残留风险 / 缓存大小守卫 / 测试更名 + downtime 回归重跑 / 基线措辞 / 重复读优化）
- Independent draft review iteration 2: accept (`ses_ffb8136beffe88xaTgqshzA83q`) because B1 修复核实（缓存仅 linkToUnderMaintenance + linkToDown 不缓存 + Goals/Non-Goals/Exit/Deferred 全一致 + 新 Deferred 项）+ N1-N6 全部修订核实 + 实仓 sanity 全过（105 tests / 无快照列 / A4 裁决 / §3.3 双分支 + §4.3 RUNNING / arm-index+roadmap 行），无阻塞问题

## Closure Gates

- [ ] 范围内行为完成（IDLE 分支落地 + 前态载体 + 零回归）
- [ ] 相关文档对齐（owner doc 注记 + arm-index + roadmap + logs）
- [ ] 已运行验证（`mvn test -pl module-maintenance/erp-mnt-service` + 全量 `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh` 零漂移）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### transient 缓存载体残余风险（重启/多实例丢失 + 非事务缓存写残留）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 缓存丢失回退 RUNNING = 现状已接受行为（非新退化）；非事务缓存写残留（`linkToUnderMaintenance` 事务回滚后 IDLE 条目污染下一次 restore）方向保守（恢复 IDLE 而非 RUNNING，IDLE 是「更保守可用态」非数据破坏）且条目在下一次 `linkTo*` 覆盖或 restore 消费时清除；Nop ERP 参考应用单实例部署语义；P2-RC-061 原分级判据不变
- Successor Required: `yes`（持久化前态快照列 `ErpMntEquipment.preMaintenanceStatus` 需求立项后按 ask-first 流程实施，人工裁决 A4 已显式排除当前实施）

### 「取决于排产」字面语义（跨域排产协调判定）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: L1 「取决于排产」的完整语义需跨域排产联动（mfg CRP 暂停/恢复），属 P1-RC-068 / RC-R1.76 越界跨域行范围；本行按 owner doc §3.3「根据之前状态」收敛为 IDLE/RUNNING 双分支
- Successor Required: `yes`（RC-R1.76 跨域契约行）

### ORM 快照列备选（D1 选项 C）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 2026-08-08 §7 A4 人工裁决明确「不改 ORM」；纯逻辑方案满足 P2 收敛义务
- Successor Required: `yes`（持久化前态需求立项时评估）

### 停机路径前态感知（IDLE 来源停机恢复）

- Classification: `watch-only residual`
- Why Not Blocking Closure: owner doc §4.3「更新设备状态为 RUNNING」字面语义保持（现状行为）；P2-RC-061/UC-MAIN-03 范围仅覆盖 visit complete/cancel 分支；停机路径行为变更需独立裁决
- Successor Required: `yes`（停机恢复语义复核需求立项时评估，与 ORM 快照列 successor 同源）

## Closure

Status Note: <pending closure>

Closure Audit Evidence:

- Auditor / Agent: <pending>
- Evidence: <pending>

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
