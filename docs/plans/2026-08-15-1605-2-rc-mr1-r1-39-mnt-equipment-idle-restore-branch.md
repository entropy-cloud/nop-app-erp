# 2026-08-15-1605-2-rc-mr1-r1-39-mnt-equipment-idle-restore-branch RC-R1.39 — maintenance 设备 IDLE 恢复分支（MR1 第一批纯预授权）

> Plan Status: completed
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

Status: completed
Targets: 本计划范围裁决（无代码）
Item Types: `Decision`
Skill: `nop-backend-dev`

- [x] **D1**: 前态捕获载体——
  - **选项 A（推荐）**：`EquipmentStatusLinker` 内部 transient `ConcurrentHashMap<Long, String> priorStatusCache`（包级可见）：**仅 `linkToUnderMaintenance` 前置读取设备当前状态，且仅当 current==IDLE 时缓存**（RUNNING 来源无需缓存，恢复恒 RUNNING）；`linkToDown` **不缓存**（停机路径恢复目标恒 RUNNING——owner doc `equipment-integration.md §4.3` 停机恢复「更新设备状态为 RUNNING」字面语义，IDLE 来源停机恢复语义不在 P2-RC-061/UC-MAIN-03 范围，保持现状）；`restoreToRunning` 消费：缓存命中 IDLE → 恢复 IDLE + 移除条目；未命中 → 恢复 RUNNING（现状行为）。
  - **残余风险登记**：① 容器重启/多实例部署缓存丢失 → 回退 RUNNING（= 现状已接受行为，非新退化）② 缓存写入非事务性——`linkToUnderMaintenance` 所在事务回滚（提交失败）后缓存残留 IDLE 条目，污染该设备**下一次** restore（恢复 IDLE 而非 RUNNING）；缓解 = 恢复语义方向是「更可用态」非数据破坏 + 条目在下一次 `linkTo*` 覆盖或 restore 消费时清除，实际污染窗口受限且方向保守（IDLE 是更保守态）——登记 watch-only ③ 异常路径（维护开始后未走 restore 的悬挂条目）→ 下次该设备 restore 消费（语义正确）或再维护覆盖（正确），无永久泄漏 ④ 缓存大小守卫：`MAX_CACHE_ENTRIES=1024`，超限时清空全表（回退 RUNNING = 现状行为，fail-safe）⑤ 并发同设备双维护 → 既有 @Version 乐观锁兜底（javadoc 已声明）。
  - **选项 B（备选）**：调用方显式传前态——`ErpMntVisitStartProcessor.start` 在调 `linkToUnderMaintenance` 前 `equipmentBiz.get` 读状态并传参——与 A 等价但把读取责任外移，破坏 linker 自包含封装，否决。
  - **选项 C（排除）**：ORM `preMaintenanceStatus` 快照列——人工裁决 A4 明确不改 ORM，登记 successor。
  - 理由记录 + 备选分析 + 残余风险登记于本计划。
  - **决策记录（已执行）**：**选项 A（linker 内部 transient 前态缓存）**——`EquipmentStatusLinker` 新增包级可见 `transient ConcurrentHashMap<Long, String> priorStatusCache`（key=equipmentId, value=前态码）承载「根据之前状态」的前态载体：`linkToUnderMaintenance` 前置 `equipmentBiz.get(String.valueOf(equipmentId), false, context)` 读取设备当前状态，**仅 current==IDLE 时 put（覆盖写）**，非 IDLE（RUNNING/DOWN 等）时 `remove` 既有条目（清悬挂残留，保证非 IDLE 来源恢复恒 RUNNING 的 D2 规则结构性成立）；`linkToDown` **零缓存**（停机路径恢复目标恒 RUNNING——§4.3「更新设备状态为 RUNNING」字面语义保持，IDLE 来源停机恢复语义不在 P2-RC-061/UC-MAIN-03 范围）；`restoreToRunning` 消费 = `consumePriorStatus(equipmentId)` **先 remove 再恢复**（防并发重复消费同一条目），命中 IDLE → 恢复 IDLE，未命中 → 恢复 RUNNING（现状行为）。`MAX_CACHE_ENTRIES=1024` 超限清空全表 fail-safe。理由：(1) P2-RC-061 核心设计缺口 = 「根据之前状态」需前态载体，人工裁决 A4 排除 ORM 快照列后纯逻辑方案必须在 restore 时点可获知维护开始前的设备状态——linker 是 visit start/complete/cancel 与停机 record/complete 的**唯一状态写站点**（census 证实），内部捕获是最小面载体；(2) 仅 IDLE 缓存将捕获面收敛到 P2-RC-061/UC-MAIN-03 范围内（visit 路径），停机路径零捕获结构性保证 D2 恢复规则（行为零变化），满足 Goals「停机路径行为零变化」；(3) 自包含封装保留（选项 B 将读取责任外移破坏 linker 单一职责，调用方每次 start 须自行读状态 + 传参，与「唯一写站点」设计相悖）；(4) 选项 C（ORM 快照列）被 §7 A4 人工裁决显式排除，登记 successor（Deferred But Adjudicated 已列）。残余风险①-⑤全部登记 watch-only（详见 Deferred But Adjudicated「transient 缓存载体残余风险」——缓存丢失回退 RUNNING = 现状已接受行为非新退化；非事务缓存写残留方向保守；Nop ERP 参考应用单实例部署语义；P2 原分级判据不变）。
  - Skill: `nop-backend-dev`
- [x] **D2**: 恢复目标规则 = 前态==IDLE（仅 visit 路径捕获）→ IDLE；其余（RUNNING 来源、停机路径、缓存缺失）→ RUNNING——停机路径因 `linkToDown` 不缓存而结构性恒 RUNNING（§4.3「更新设备状态为 RUNNING」字面语义，行为零变化）。
  - **决策记录（已执行）**：恢复目标规则 = **前态==IDLE → IDLE；其余（RUNNING 来源、停机路径 linkToDown 不缓存、缓存缺失/重启丢失）→ RUNNING**。理由：(1) L1（`use-cases.md:57/63`）「设备→RUNNING/IDLE」+ L2（§3.3）「恢复为 RUNNING 或 IDLE（根据之前状态）」的收敛语义 = 按维护开始前的设备状态恢复——IDLE 出发的维护回 IDLE（§3.2 流转图「闲置(IDLE) → 开始维护 → 维护中 → 维护完成」），RUNNING 出发回 RUNNING（§3.2 语义不变）；(2)「取决于排产」的完整跨域排产协调判定属 P1-RC-068/RC-R1.76 越界行（Non-Goals 已声明），本行按「根据之前状态」收敛；(3) 停机路径因 D1 不捕获而结构性恒 RUNNING = §4.3 字面语义（「更新设备状态为 RUNNING」），行为零变化；(4) 缓存缺失（容器重启/多实例）回退 RUNNING = 现状已接受行为（更可用态），非新退化。测试语义：IDLE 输入断言反转（D3）+ DOWN 来源 seed 恢复 RUNNING 回归 + 缓存缺失回退断言（Phase 3）。
  - Skill: `nop-backend-dev`
- [x] **D3**: 测试语义调整——既有 `testVisitCompleteFromIdleEquipmentRestoresRunning`（A4.2.148 新增，当前断言 RUNNING）断言**反转**为 IDLE 并更名 `testVisitCompleteFromIdleEquipmentRestoresIdle`（A4.2.148 引用注记保留）；新增 RUNNING 来源回归断言不变。
  - **决策记录（已执行）**：既有 `testVisitCompleteFromIdleEquipmentRestoresRunning`（A4.2.148 新增，IDLE seed → complete → 断言 RUNNING 实证简化偏差）**断言反转**为 IDLE 并**更名** `testVisitCompleteFromIdleEquipmentRestoresIdle`（A4.2.148 引用注记保留——注释保留「A4.2.148 原断言 RUNNING」历史标记，实现前该测试 RED、实现后 GREEN 作 Proof）；RUNNING 来源回归 = 既有 `testVisitHappyPathWithEquipmentLink`（seed RUNNING → complete → 断言 RUNNING）重跑断言不变；新增 cancel 路径 IDLE 恢复（IDLE → start → cancel → 断言 IDLE）+ DOWN 来源 seed 恢复 RUNNING（DOWN → schedule → start → complete → 断言 RUNNING——linkToUnderMaintenance 对 DOWN 不缓存，恢复回退 RUNNING 证明捕获面收敛）+ 缓存缺失回退（直接构造 restore 无前置 linkTo* → 恢复 RUNNING 不回退为异常，模拟重启语义）。理由：(1) D2 恢复规则落地后 IDLE 输入的正确断言 = IDLE（断言反转是行为契约变更的显式测试面）；(2) 更名保留 A4.2.148 运行时证据的历史可追溯性；(3) 新增三测试覆盖 D2 规则的「其余 → RUNNING」边界（cancel 路径 + DOWN 来源 + 缓存缺失）。
  - Skill: `nop-testing`

Exit Criteria:

- [x] D1-D3 裁决记录于本计划（选择 + 备选 + 理由 + 残余风险），Phase 2/3 实现按裁决执行

### Phase 2 - EquipmentStatusLinker IDLE 分支实现

Status: completed
Targets: `module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/support/EquipmentStatusLinker.java`
Item Types: `Fix`
Skill: `nop-backend-dev`

- [x] 实现 D1 选项 A：`priorStatusCache`（transient `ConcurrentHashMap<Long, String>` + `MAX_CACHE_ENTRIES=1024` 超限清空 fail-safe）+ `linkToUnderMaintenance` 前置读取设备当前状态（`equipmentBiz.get(String.valueOf(equipmentId), false, context)`）**仅 IDLE 缓存**（覆盖写，key=equipmentId）+ `linkToDown` **不缓存**（D1 裁决）+ `restoreToRunning` 消费（命中 IDLE → 恢复 IDLE + `remove`；未命中 → RUNNING；`remove` 在恢复前执行防并发重复消费）
      **落地（已执行）**：`EquipmentStatusLinker` 新增包级可见 `transient ConcurrentHashMap<Long, String> priorStatusCache`（key=equipmentId）+ `MAX_CACHE_ENTRIES=1024` 常量 + 三个 protected helper——`capturePriorStatus`（linkToUnderMaintenance 前置调 `equipmentBiz.get` 读当前状态：IDLE → `guardCacheSize` 后 put 覆盖写；非 IDLE → remove 清除悬挂残留）、`consumePriorStatus`（restoreToRunning 内 **先 `remove` 再恢复**，防并发重复消费）、`guardCacheSize`（size >= 1024 时 clear 全表 fail-safe）；`linkToDown` 保持零缓存（仅 changeEquipmentStatus 落 DOWN）；`restoreToRunning` 消费语义 = 命中 IDLE → 恢复 IDLE、未命中 → 恢复 RUNNING（现状行为）。
- [x] javadoc 更新：去除「IDLE 设备恢复为 RUNNING 为已知的简化偏差」失实声明 → 双分支实现说明 + 残余风险注记（重启/多实例回退 RUNNING + 非事务缓存写残留 + 并发乐观锁兜底）
      **落地（已执行）**：类头 javadoc 改写——恢复目标双分支说明（§3.3「恢复为 RUNNING 或 IDLE（根据之前状态）」运行时成立 + §4.3 停机恢复恒 RUNNING 行为零变化声明）+ 残余风险①-⑤ 注记（重启/多实例回退 RUNNING / 非事务缓存写残留方向保守 / 异常路径悬挂条目消费或覆盖清除 / 超限清空 fail-safe / @Version 乐观锁兜底）；字段 javadoc 注明前态缓存职责与包级可见原因。
- [x] `changeEquipmentStatus` 既有签名/守卫不变（config 门控 + ERR_EQUIPMENT_NOT_FOUND 守卫保留）
      **落地（已执行）**：`changeEquipmentStatus(Long, String, IServiceContext)` 签名、config 门控（三 public 方法入口各自 `equipmentStatusLinkEnabled()` 守卫）、`ERR_EQUIPMENT_NOT_FOUND` 守卫、`equipmentBiz.updateEntity` 调用路径全部保持原样。

Exit Criteria:

- [x] visit 路径：IDLE 前态经 complete/cancel 后恢复 IDLE；RUNNING 前态恢复 RUNNING（Phase 3 测试断言）
- [x] 停机路径行为零变化（DOWN→RUNNING；IDLE 来源停机→RUNNING 现状保持）；config 关闭（equipment-status-link-enabled=false）行为不变（零状态写入）

### Phase 3 - 测试

Status: completed
Targets: `module-maintenance/erp-mnt-service/src/test`（TestErpMntVisitRequestStateMachine + TestErpMntDowntimeAndE2E）
Item Types: `Fix | Proof`
Skill: `nop-testing`

- [x] `testVisitCompleteFromIdleEquipmentRestoresRunning` 断言反转 + 更名 `testVisitCompleteFromIdleEquipmentRestoresIdle`（A4.2.148 注记保留）：IDLE → schedule → start（linkToUnderMaintenance 捕获 IDLE）→ complete → **断言 IDLE**（实现前该测试 RED，实现后 GREEN——Proof 步骤）
      **落地（已执行）**：`TestErpMntVisitRequestStateMachine#testVisitCompleteFromIdleEquipmentRestoresIdle`——seed 设备=IDLE → schedule → start → complete → 断言 `EQUIPMENT_STATUS_IDLE`；注释保留 A4.2.148 历史标记（「原断言 RUNNING 反转」）。实现前该测试 RED（恒恢复 RUNNING 断言 IDLE 失败）、实现后 GREEN 的 Proof 语义成立。
- [x] 新增 RUNNING 来源回归：RUNNING → schedule → start → complete → 断言 RUNNING（既有 `testVisitHappyPathWithEquipmentLink` 已覆盖——重跑佐证，断言不变）
      **落地（已执行）**：重跑既有 `testVisitHappyPathWithEquipmentLink`（seed RUNNING → start → UNDER_MAINTENANCE → complete → 断言 RUNNING）全绿，断言零变更（RUNNING 来源完整生命周期回归佐证）。
- [x] 新增 cancel 路径：IDLE → start → cancel → 断言 IDLE（恢复语义经 cancel 同样成立）
      **落地（已执行）**：新增 `TestErpMntVisitRequestStateMachine#testVisitCancelFromIdleEquipmentRestoresIdle`——seed 设备=IDLE → schedule → start（捕获 IDLE）→ cancel → 断言 visit=CANCELLED + 设备=IDLE 全绿。
- [x] 停机路径回归（重跑既有，断言不变——D1 裁决后行为零变化）：`TestErpMntDowntimeAndE2E#testDowntimeRecordSetsDownAndCompleteRestores`（DOWN→RUNNING）+ DOWN 来源 seed 测试（`TestErpMntVisitRequestStateMachine` DOWN seed → complete → RUNNING）
      **落地（已执行）**：① 重跑 `TestErpMntDowntimeAndE2E#testDowntimeRecordSetsDownAndCompleteRestores` + `#testDowntimeTotalMinutesReflectsDuration`（DOWN seed → complete → RUNNING）断言零变更全绿（停机路径行为零变化）；② 新增 `TestErpMntVisitRequestStateMachine#testVisitCompleteFromDownEquipmentRestoresRunning`——seed 设备=DOWN → schedule → start → complete → 断言 RUNNING（capturePriorStatus 仅捕获 IDLE，DOWN 非缓存态 → restore 回退 RUNNING，证实捕获面收敛于 visit IDLE 分支）。
- [x] 新增缓存消费/回退边界：缓存缺失（模拟重启语义——直接构造 restore 无前置 linkTo*）→ 恢复 RUNNING 不回退为异常（watch-only 语义断言）
      **落地（已执行）**：新增 `TestErpMntVisitRequestStateMachine#testRestoreWithoutPriorLinkFallsBackToRunning`——seed 设备=IDLE（OTHER_EQUIPMENT_ID=102，从未参与 linkTo*）→ 直接注入 `EquipmentStatusLinker.restoreToRunning(OTHER_EQUIPMENT_ID, CTX)`（无前置 linkTo*，模拟重启缓存丢失）→ 断言设备=RUNNING 且无异常（watch-only 回退语义断言，`CTX=new ServiceContextImpl()` 对齐 TestErpMntDueVisitJob/TestErpMntDashboard 既有直接调用范式）。

Exit Criteria:

- [x] 上述测试全部 GREEN + erp-mnt-service 既有 105 tests 零回归
- [x] 分域 `mvn test -pl module-maintenance/erp-mnt-service` 全绿

### Phase 4 - 验证与回填

Status: completed
Targets: 全量构建 + checker + arm-index + roadmap + owner doc + docs/logs
Item Types: `Proof | Follow-up`
Skill: `none`

- [x] Proof: 全量 `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh` actual==baseline 零漂移（零新增 daoFor/import 面，R2c=1399 / R10=9 不变）
      **落地（已执行）**：`mvn clean install -DskipTests` 全量 BUILD SUCCESS；`bash docs/audits/nop-compliance-checker.sh` 全 19 规则 actual==baseline 零漂移（R1d=14 / R2a=34 / R2b=230 / R2c=1399 / R2d=34 / R3=5 / R6=2 / R10=9 / R12a=69 / R12b=66 / R12c=40——EquipmentStatusLinker 零新增 daoFor/import 面，既有 `IErpMntEquipmentBiz` 注入）。
  - Skill: `nop-testing`
- [x] arm-index P2-RC-061 → `done (RC-R1.39)`（含修复落地摘要：D1 载体裁决 + 双分支实现 + 残余风险注记）
      **落地（已执行）**：`docs/audits/arm-index.md` P2-RC-061 行末列 `todo 【R1.0 展开归属】RC-R1.39` → `done (RC-R1.39)`（修复落地摘要：D1 载体裁决（transient 前态缓存/仅 IDLE 捕获/停机零缓存/先 remove 再恢复）+ D2 恢复目标规则 + javadoc 失实声明纠正 + 测试证据 4 组 + 108 tests + 零漂移 + owner doc 注记）。
- [x] roadmap RC-R1.39 行 → done ✅（含落地摘要）
      **落地（已执行）**：`docs/backlog/requirement-compliance-roadmap.md` RC-R1.39 行 `todo` → `done ✅`（含落地摘要：D1/D2 裁决 + 双分支实现 + 测试证据 + 108 tests 全绿 + 零漂移 + owner doc 注记）。
- [x] owner doc 注记：equipment-integration.md §3.3「恢复为 RUNNING 或 IDLE（根据之前状态）」补实现注记（IDLE 分支已实现 + transient 缓存载体 + 重启/多实例残余风险 + ORM 快照列 successor 备选）
      **落地（已执行）**：`docs/design/maintenance/equipment-integration.md` §3.3 表后补实现注记（双分支实现说明[linkToUnderMaintenance 仅 IDLE 捕获 / restoreToRunning 消费先 remove 再恢复] + 停机路径恒 RUNNING 行为零变化声明 + 残余风险①-④ + `preMaintenanceStatus` 快照列 successor 备选 + 测试证据 5 组引用）。
- [x] `docs/logs/2026/08-15.md` 顶部追加本计划落地日志条目（格式见 `docs/logs/00-log-writing-guide.md`）
      **落地（已执行）**：`docs/logs/2026/08-15.md` 顶部追加 RC-R1.39 落地条目（工作项/D1-D3 裁决/落地/测试/验证基线 108 tests 全绿 + 全量构建 + 零漂移/回填/Deferred 四源一致）。

Exit Criteria:

- [x] 全量构建 + checker 零漂移 + 回填完成且与 roadmap/arm-index/owner doc/logs 四源一致

## Draft Review Record

- Independent draft review iteration 1: needs revision (`ses_ffb8aa1a6ffec8UcyqAi1HPaEx`) because 1 blocking issue — B1 `linkToDown` 亦缓存 IDLE 致停机路径行为变更（IDLE 来源停机恢复 IDLE 与 owner doc §4.3「更新设备状态为 RUNNING」矛盾 + D2 内部不一致 + 无测试覆盖 + 超工作项范围），须限定 IDLE 捕获仅 visit 路径；N1-N7 非阻塞（Item Types / 行号 complete:37 cancel:31 / 非事务缓存写残留风险 / 缓存大小守卫 / 测试更名 + downtime 回归重跑 / 基线措辞 / 重复读优化）
- Independent draft review iteration 2: accept (`ses_ffb8136beffe88xaTgqshzA83q`) because B1 修复核实（缓存仅 linkToUnderMaintenance + linkToDown 不缓存 + Goals/Non-Goals/Exit/Deferred 全一致 + 新 Deferred 项）+ N1-N6 全部修订核实 + 实仓 sanity 全过（105 tests / 无快照列 / A4 裁决 / §3.3 双分支 + §4.3 RUNNING / arm-index+roadmap 行），无阻塞问题

## Closure Gates

- [x] 范围内行为完成（IDLE 分支落地 + 前态载体 + 零回归）
- [x] 相关文档对齐（owner doc 注记 + arm-index + roadmap + logs）
- [x] 已运行验证（`mvn test -pl module-maintenance/erp-mnt-service` + 全量 `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh` 零漂移）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

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

Status Note: 四 Phase 全部执行完成（2026-08-15）：Phase 1 D1-D3 裁决落盘（前态载体 = linker 内部 transient 缓存选项 A，仅 IDLE 捕获，停机零缓存，restore 先 remove 再恢复 + 残余风险①-⑤ 登记）；Phase 2 `EquipmentStatusLinker` IDLE 分支落地（priorStatusCache + MAX_CACHE_ENTRIES=1024 fail-safe + capturePriorStatus/consumePriorStatus/guardCacheSize 三 helper + javadoc 失实声明纠正，changeEquipmentStatus 签名/守卫/config 门控零变更）；Phase 3 测试（A4.2.148 IDLE 断言反转 + 更名 `testVisitCompleteFromIdleEquipmentRestoresIdle` + 新增 cancel 路径/DOWN 来源回退/缓存缺失回退 3 组，停机路径重跑断言零变更）；Phase 4 验证与回填（`mvn test -pl module-maintenance/erp-mnt-service` **108/108** 全绿 [105 基线 + 3 新增] + 全量 `mvn clean install -DskipTests` BUILD SUCCESS + compliance checker 全 19 规则 actual==baseline 零漂移 [R2c=1399/R10=9 不变] + arm-index P2-RC-061 `done (RC-R1.39)` + roadmap RC-R1.39 `done ✅` + owner doc equipment-integration.md §3.3 实现注记 + `docs/logs/2026/08-15.md` 条目）。Closure Gates 1-8 全部勾选——Gate 7（独立结束审计）由独立子代理（新会话，CLOSURE_VERIFY）执行并勾选，证据见下方 Closure Audit Evidence。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（CLOSURE_VERIFY，新会话，未复用执行者上下文）— mission driver 2026-08-14-070716
- Evidence: 实仓复核通过——① 代码落地：`EquipmentStatusLinker.java` 现文核验（`priorStatusCache` 包级可见 `ConcurrentHashMap` + `MAX_CACHE_ENTRIES=1024` + `capturePriorStatus`[仅 IDLE put 覆盖写/非 IDLE remove] + `consumePriorStatus`[先 remove 再恢复] + `guardCacheSize`[超限 clear fail-safe] + `linkToDown` 零缓存 + `restoreToRunning` 双分支，`changeEquipmentStatus` 签名/守卫/config 门控零变更）与 Phase 2 落地描述逐条一致；② 运行时接线（anti-hollow）：`linkToUnderMaintenance` 被 `ErpMntVisitStartProcessor:25` 调用、`restoreToRunning` 被 `ErpMntVisitCompleteProcessor:37` / `ErpMntVisitCancelProcessor:31` / `ErpMntDowntimeEntryCompleteProcessor:23` 调用、`linkToDown` 被 `ErpMntDowntimeEntryRecordProcessor:20` 调用——新 helper 全部经既有 public 入口可达，无悬挂空壳；③ 测试落地：`TestErpMntVisitRequestStateMachine` 现文核验 `testVisitCompleteFromIdleEquipmentRestoresIdle`（断言 IDLE + A4.2.148 反转注记）/`testVisitCancelFromIdleEquipmentRestoresIdle`/`testVisitCompleteFromDownEquipmentRestoresRunning`/`testRestoreWithoutPriorLinkFallsBackToRunning`（断言 RUNNING 回退）/`testVisitHappyPathWithEquipmentLink`（RUNNING 回归）五方法断言与 Phase 3 描述一致，`_cases/` 四组新快照目录已生成；`TestErpMntDowntimeAndE2E` 停机回归两方法存在；④ 回填四源：arm-index P2-RC-061 → `done (RC-R1.39)`（行 251 现文核验）+ roadmap RC-R1.39 → `done ✅`（行 431 现文核验）+ owner doc `equipment-integration.md:144` §3.3 双分支实现注记现文核验 + `docs/logs/2026/08-15.md:53-61` 日志条目现文核验——四源与 plan 状态一致；⑤ 五维一致性：Plan Status=completed / 四 Phase 全 completed / Exit Criteria 全 [x] / Closure Gates 全 [x] / Deferred 四项均为 watch-only residual 或 out-of-scope 且命名 successor，无隐藏 in-scope 缺陷。
- 验证基线（执行者记录，审计复核）：`mvn test -pl module-maintenance/erp-mnt-service` 108/108 全绿 + 全量 `mvn clean install -DskipTests` BUILD SUCCESS + compliance checker 零漂移（R2c=1399/R10=9 不变）

Follow-up:

- 无非阻塞跟进项——transient 缓存载体残余风险、「取决于排产」跨域协调、ORM 快照列备选、停机路径前态感知四项已全部在「Deferred But Adjudicated」裁定（watch-only residual / out-of-scope improvement，均命名 successor），无已确认缺陷滞留。
