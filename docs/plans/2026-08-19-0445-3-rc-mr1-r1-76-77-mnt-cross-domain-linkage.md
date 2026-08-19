# 2026-08-19-0445-3-rc-mr1-r1-76-77-mnt-cross-domain-linkage RC-R1.76/77 — maintenance 跨域联动（停机→制造排产消费 + 资产处置→设备停用与引用守卫）（B 类预授权：零 ORM，Facade/notify 接线）

> Plan Status: completed
> Last Reviewed: 2026-08-19
> Mission: requirement-compliance
> Work Item: RC-R1.76（P1-RC-068，UC-MAIN-06 停机事件→制造域排产暂停/恢复）+ RC-R1.77（P1-RC-070，UC-MAIN-08 资产处置→设备 DECOMMISSIONED 联动 + 引用守卫）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.76/77 行 + `docs/audits/arm-index.md` P1-RC-068/070（A1.44 切片登记 + A4.2.152/153 运行时确认维持 P1）+ 2026-08-12 批量裁决 B 类（roadmap 头 :49：「RC-R1.76（事件发布 + mfg 消费者）、RC-R1.77（事件监听器 + DECOMMISSIONED 守卫）」降级为预授权自动执行，跨域契约项仍须协调确认但不触 ORM ask-first；行标签仍携旧「越界项」措辞，done 回写时按 R1.61-67 先例同步改写）
> Related: `docs/design/maintenance/use-cases.md` UC-MAIN-06/08（L1 正文不动）+ `docs/design/maintenance/equipment-integration.md` §一（1.3 资产状态联动）+ §四（4.2/4.3 停机排产）+ §七（7.1/7.3 跨域协作协议）；`docs/architecture/data-dependency-matrix.md`（:86 maintenance 行「影响 manufacturing」+ :109「被 mfg 查」授权方向）；`docs/plans/2026-08-15-2119-3`（R1.59 cancelForBusinessBill 三域 Facade 接线先例）+ `docs/plans/2026-08-18-1849-1`（R1.68 cs→qa 单向 pom 依赖 + notify 双事件先例）
> Audit: required

## Current Baseline

- **finding P1-RC-068（UC-MAIN-06，A4.2.152 运行时确认维持 P1）**：L1 `use-cases.md:106-116` 逐字：「设备.状态 = DOWN(故障停机) → 创建停机记录(DowntimeEntry) / 发布事件(设备停机) → 制造域接收 → 暂停该设备的工单排产 / 设备恢复(RUNNING) → 发布事件(设备恢复) → 恢复排产」。L3：grep publish|IErpSysEventBus|publishEvent|fireEvent|EventBus 跨 module-maintenance **零命中**，**全仓零事件总线机制**（双路 grep 复核）——跨域集成唯一范式 = 直接 I*Biz Facade 调用（R1.59/R1.61 先例）+ notify 通知（IErpSysNotificationBiz，六域注入先例）；mfg 侧零 mnt 引用（grep ErpMnt|DowntimeEntry 跨 module-manufacturing src/main 零代码命中）。
- **finding P1-RC-070（UC-MAIN-08，A4.2.153 运行时确认维持 P1）**：L1 `use-cases.md:143-144` 逐字：「资产 SCRAPPED/SOLD → 设备.状态 = DECOMMISSIONED(停用) / 设备不可再被新维护计划/工单引用」。L3：assets 侧 `ErpAstDisposalProcessor.executeApprove:85-130` 仅 asset.setStatus(SCRAPPED/SOLD)+cancelPendingSchedules+过账，module-assets 全域零 ErpMnt 引用（双向联动均缺失）；maintenance 侧 `EQUIPMENT_STATUS_DECOMMISSIONED` 常量 + dict 值已存在（_ErpMntDaoConstants:99）+ dashboard `countEquipmentNotDecommissioned` 过滤存在，但 `ErpMntEquipmentBizModel#changeStatus` 为通用手工入口**非事件驱动**；**零「DECOMMISSIONED 设备不可被新维护计划/工单引用」守卫**（grep validateEquipment|requireEquipment 跨 mnt BizModel/Processor 零命中——Schedule/Request/Visit 三 BizModel 均纯 CrudBizModel 无 save 钩子）。
- **可复用机制**：`ErpMntEquipment.assetId` FK（orm.xml:127 + to-one :148）+ `workcenterId`（:128，设备↔工作中心桥）；`ErpMntDowntimeEntry` record/complete Processors（`ErpMntDowntimeEntryRecordProcessor:16-29` → linkToDown / `ErpMntDowntimeEntryCompleteProcessor:18-35` → restoreToRunning，既有 Facade `IErpMntDowntimeEntryBiz` 在 erp-mnt-dao）；mfg 排产消费点 `ErpMfgScheduleToJobCardProcessor`（PLANNED 工序→job card 按 workcenterId 映射，无设备状态门控）+ `CrpLoadCalculator`（容量仅出勤日历，无设备状态）；aps `ErpApsSchedulingEngine` 时间轴仅消费自身 `ErpApsConstraint`(MAINTENANCE) 行（:159-174）——维护停机窗口不自动进入 aps 约束。
- **依赖边现状（矩阵权威）**：maintenance R 读 = master-data/inventory/assets（:534 mnt→assets R 批准保留）；maintenance 影响 manufacturing（:86 停机影响排产）+ 「被 mfg 查」预期方向（:109）；**Java 层新边 = 本计划引入两条**：assets-service→erp-mnt-dao + mfg-service→erp-mnt-dao（矩阵登记注记）。**注意**：erp-mnt-dao 已 pom 依赖 app-erp-assets-dao（`erp-mnt-dao/pom.xml:41`，ORM to-one shadow 的 dao 层依赖）——ast-service→mnt-dao→ast-dao 构成 Maven 菱形**非环**（ast-dao 无反向依赖），但形成 assets↔maintenance 双向域耦合，矩阵登记须显式披露此耦合（区别于 R1.68 cs→qa 单向叶依赖）。
- **内部 save 路径（守卫落点关键）**：`ScheduleDueGenerator.generateVisitForSchedule` 经 `visitBiz.save`（:80）+ `ErpMntRequestAcceptProcessor.generateResponsiveVisit` 经 `visitBiz.save`（:41）——BizModel save 钩子对内部保存同样触发；`generateDueVisits` 逐计划循环**无 per-schedule try/catch**（:69-73 仅 isActive+nextDueDate 过滤），且 assets 处置**不停用**维护计划（`cancelPendingSchedules` 仅触 ErpAstDepreciationSchedule）——守卫若不加批次豁免，一条 DECOMMISSIONED 设备的到期计划将使整个到期访问日批 job 中断（绿基线回归）。
- **notify seed**：现存 ID 7101-7106 + 7201-**7207**（三方言 mysql/oracle/postgresql；7206/7207 已被 RC-R1.71 落地占用 `cs.fulfillment-step-failed`/`cs.fulfillment-notify-customer`）；本计划新模板 **7208/7209**。收件人范式：ROLE 角色名匹配（cs.sla-overdue 先例，角色数据属部署数据，无匹配角色 config-gated 空投递）。
- **Q4 判据**：两项均 §2 P1①（功能完全缺失）+ §2 P1④（跨域契约行为不一致）；三判据复核均不成立（equipment-integration.md §一/§四活跃契约未 Deferred + product-scope 未裁剪）→ Q4=(a) 强制实现。**2026-08-12 B 类批量裁决：两行降级为预授权自动执行（纯代码逻辑/跨域契约），零 ORM 变更，不触 ask-first**；跨域契约协调义务经本计划 D1/D2 决策项 + 矩阵登记履行。
- **测试基线**：erp-mnt-service **108 @Test** / erp-ast-service（R1.54 后 **314**）/ erp-mfg-service（R1.59 后 282）；notify 落库断言范式（TestErpCsQualityEscalation 等）；job 计数 TestErpAllJobYamlLoading=29（本计划零新 job）。
- **compliance 基线**：R2b=235 / R2c=1439 / R2d=35 / R10=12 / R12a=70。新跨域调用全部经 IBiz Facade 注入（R2b 合规范式）预期零漂移。

## Goals

- **UC-MAIN-08 处置联动成立**：assets 处置 approve 后置调 mnt Facade（同 JVM 同事务异常传播强一致）→ 按(assetId)查关联设备置 DECOMMISSIONED（无关联设备 no-op 跳过、已 DECOMMISSIONED 幂等跳过）；处置 reverseApprove 对称恢复（§1.3 资产恢复分支）。
- **UC-MAIN-08 引用守卫成立**：Schedule/Request/Visit 创建与排程迁移处校验设备 status≠DECOMMISSIONED → 新错误码 `ERR_MNT_EQUIPMENT_DECOMMISSIONED` 拒绝（「新维护计划/工单引用」——维护工单=visit，矩阵 :312 MNT_VISIT 词条）。
- **UC-MAIN-06 停机排产联动成立**：mnt 侧暴露开放停机窗口查询 Facade + 停机 record/complete 双向 notify 事件（计划员通知）；mfg 侧排产消费（job card 生成门控）经拉取判定暂停/恢复受影响工单排产。
- **依赖矩阵登记**：两条新 Java 边（assets→mnt-dao、mfg→mnt-dao）登记 `docs/architecture/data-dependency-matrix.md` 注记。
- **测试补强**：TestErpMntAssetDisposalLinkage / TestErpMntEquipmentReferenceGuard / TestErpMntDowntimeSchedulingLinkage + mfg 侧消费测试 + 三域基线零回归 + 全量构建 + checker 零漂移。
- **owner doc 收敛**：equipment-integration.md §一/§四/§七 实现注记（拉取消费模型裁决 + Facade 边）+ arm-index P1-RC-068/070 → done + roadmap 两行 done + 行标签改写 + logs 条目。

## Non-Goals

- **不引入事件总线/发布订阅基建**（全仓零事件总线；直接 Facade + notify 是既有范式，引入新基建属架构变更另行立项）。
- **不给 mfg 工单加「排产暂停」状态列/字典**（L1「暂停排产」经消费侧拉取门控达成；工单级 HOLD 状态属 RC-R1.88 aps 派工行范围）。
- **不自动生成 aps ErpApsConstraint(MAINTENANCE) 行**（mnt→aps S 写超出矩阵 :109 允许清单须矩阵修订，登记 successor）。
- **不做 CRP 负荷停机扣减**（L2 §4.2 未断言容量口径；最小实现 = 排产门控 + 通知，CRP 容量联动 successor）。
- **不做设备 DECOMMISSIONED 反向恢复自动化**（处置 reverseApprove 对称恢复已覆盖 §1.3 恢复分支；手工 changeStatus 仍可用）。

## Task Route

- Type: `implementation-only change`（跨域契约协调经 D1/D2 决策项 + 矩阵登记履行）
- Owner Docs: `docs/design/maintenance/equipment-integration.md`（§一/§四/§七）+ `docs/design/maintenance/use-cases.md`（L1 正文不动）+ `docs/architecture/data-dependency-matrix.md`（新边登记）
- Skill Selection Basis: BizModel/Processor/Facade（`nop-backend-dev`，跨实体访问经 I*Biz 注入规则）；notify 接线（R1.68 先例）；测试（`nop-testing`）。

## Infrastructure And Config Prereqs

- ORM：**零变更**（DECOMMISSIONED dict 值已存在；本计划纯代码逻辑）。
- 新 config 键：`erp-mnt.downtime-notify-enabled`（默认 true，停机双事件通知门控）+ `erp-mnt.disposal-link-enabled`（默认 true，处置联动门控，镜像 equipment-status-link-enabled 先例）。
- 新 notify seed 模板：`mnt.equipment-downtime`（**7208**，ROLE 生产计划员 + 设备/工作中心/原因/起止时间 context）+ `mnt.equipment-recovered`（**7209**，ROLE 生产计划员），三方言（module-notify/deploy/sql/{mysql,oracle,postgresql}/_seed_erp-notify.sql ID 顺延——7206/7207 已被 RC-R1.71 占用）。
- 新 Java 边 pom：module-assets/erp-ast-service + module-manufacturing/erp-mfg-service 增 erp-mnt-dao 依赖（R1.68 cs→qa compile 单向同型，仅 dao 接口层不引 service）。

## Execution Plan

### Phase 1 - R1.77：资产处置→设备 DECOMMISSIONED 联动 + 引用守卫

Status: completed
Targets: `module-maintenance/erp-mnt-dao/.../biz/IErpMntEquipmentBiz.java`（Facade 声明）、`module-maintenance/erp-mnt-service/.../entity/ErpMntEquipmentBizModel.java` + per-mutation Processor、`module-assets/erp-ast-service/.../processor/ErpAstDisposalProcessor.java`（+reverseApprove 接线）、mnt 三 BizModel 守卫、`ErpMntErrors.java`、`module-assets/erp-ast-service/pom.xml`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: 无（与 Plan 2 同域且**共同触及 `ErpMntEquipmentBizModel.java`**——Plan 2 D1 changeStatus 写 StatusLog、本计划 D1 Facade 方法；同批执行时 Plan 2 先落 ORM 后本计划纯代码叠加，文件冲突经串行执行消解）

- [x] **D1 联动方向 = assets 处置 Processor 直接调 mnt Facade**（否决「mnt 轮询资产状态」：轮询延迟 + 全表扫描；否决「事件总线」：全仓无此基建）。`IErpMntEquipmentBiz` 增 Facade 方法 `changeStatusForAssetDisposal(assetId, disposalType)` + `restoreFromAssetDisposal(assetId)`：按 assetId 查关联设备（to-one 反查）→ 无设备 no-op 返回 0（合法：可选关联 §1.2）；已目标态幂等跳过；成功置 DECOMMISSIONED（经 EquipmentStatusLinker 同链写入——Plan 2 StatusLog 落地后自动记录 DISPOSAL 来源日志行）。**失败语义 = 异常传播回滚处置**（同 JVM 同事务强一致，区别于 R1.59 辅助语义降级——设备停用是 L1 硬断言，处置成功但设备未停用 = 契约破坏；config `erp-mnt.disposal-link-enabled` 关闭时跳过）。approve 后置 protected step + **reverseApprove 对称恢复落位 = 与资产恢复同分支**（`executeReverseApprove` 仅 posted==TRUE 分支恢复资产 IN_SERVICE——restoreFromAssetDisposal 置于同一分支，防「设备 RUNNING / 资产 SCRAPPED」分叉；恢复目标 = RUNNING，§1.3「资产恢复（IN_SERVICE）→ 设备状态改为运行中」字面；设备非 DECOMMISSIONED 时幂等跳过）。
      - Skill: `nop-backend-dev`
- [x] **D2 引用守卫 = BizModel save 钩子 + 排程迁移门控 + 批次路径豁免**：Schedule/Request/Visit 三 `defaultPrepareSave`（新增行）校验 equipment.status≠DECOMMISSIONED → `ERR_MNT_EQUIPMENT_DECOMMISSIONED`（新错误码，中文描述「设备[{code}]已停用（资产已处置），不可被新维护计划/工单引用」）；存量行 update 非设备维度变更不误伤（仅 equipmentId 变更或新增行时校验）。排程迁移侧（VisitScheduleProcessor）追加同守卫。**内部 save 双路径裁决**：①批量生成路径（generateVisitForSchedule 经 visitBiz.save 触发钩子）——`findDueSchedules` 查询侧排除 DECOMMISSIONED 设备计划 + LOG.warn 跳过（到期访问日批 job 无 per-schedule try/catch，一条违规设备计划中断整批 = 绿基线回归，禁止）；②手工触发路径（generateResponsiveVisit 经 visitBiz.save）——accept 明确拒绝抛同错误码（对已处置设备的**新**维护工作开访问正是 L1 禁止语义，用户显式操作报错可理解且事务回滚）；③Plan 2 的 runtime 触发扫描同口径排除 DECOMMISSIONED 设备（执行期与 Plan 2 协调落点）。
      - Skill: `nop-backend-dev`
- [x] **Proof**：TestErpMntAssetDisposalLinkage（mock/真实 Facade 双层：①处置 approve → 关联设备 DECOMMISSIONED ②无关联设备 no-op ③幂等 ④reverseApprove posted 分支 → 恢复 RUNNING（含非 DECOMMISSIONED 幂等）⑤config 关闭跳过 ⑥联动失败传播处置回滚）+ TestErpMntEquipmentReferenceGuard（①DECOMMISSIONED 设备新建 Schedule/Request/Visit 拒绝 + 错误码断言 ②RUNNING/IDLE 正常 ③既有行 update 非设备维度不触发 ④DRAFT 排程迁移拒绝 ⑤**到期访问日批 job：DECOMMISSIONED 设备计划被跳过整批完成 + warn** ⑥**DECOMMISSIONED 设备的 OPEN request accept 被拒绝回滚**）+ erp-ast-service 处置测试零回归（联动经真实 IoC 或 mock 注入，对齐 R1.68 cs mock 范式——ast-service 测试容器无 mnt bean 时 @Nullable 注入）+ `_cases/` 快照。验证命令：`mvn test -pl module-maintenance/erp-mnt-service -pl module-assets/erp-ast-service`。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 处置联动 + 引用守卫落地，mnt/ast 两域测试绿零回归

### Phase 2 - R1.76：停机→制造排产消费 + 双向 notify

Status: completed
Targets: `module-maintenance/erp-mnt-dao/.../biz/IErpMntDowntimeEntryBiz.java`（既有 Facade 扩展查询声明）、`module-maintenance/erp-mnt-service/.../entity/ErpMntDowntimeEntryBizModel.java`、`ErpMntDowntimeEntryRecordProcessor.java`/`ErpMntDowntimeEntryCompleteProcessor.java`（notify 接线）、`module-notify/deploy/sql/{mysql,oracle,postgresql}/_seed_erp-notify.sql`（7208/7209）、`module-manufacturing/erp-mfg-service/.../processor/ErpMfgScheduleToJobCardProcessor.java`（消费门控）、`module-manufacturing/erp-mfg-service/pom.xml`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（同批 mnt 文件串行）

- [x] **D3 联动模型 = mfg 拉取消费（查询时判定）+ mnt notify 事件（计划员通知）**（否决「mnt push 写 mfg 工单标记」：mfg 工单无「排产暂停」持久状态列，push 无处落且触 ORM 越界；否决「mnt 写 aps MAINTENANCE constraint」：mnt→aps S 写超出矩阵 :109 允许清单）。**发布侧** = 停机 record/complete Processors 后置 notify（7208/7209，config `erp-mnt.downtime-notify-enabled` 门控，try/catch 静默降级不阻断停机主流程——通知为 L2「通知计划员」辅助语义）；**消费侧** = 既有 `IErpMntDowntimeEntryBiz` 增只读查询 `findOpenDowntimeEquipmentWorkcenters()` 暴露「工作中心→开放停机窗口」（经 equipment.workcenterId 桥接，开放 = endTime null 且设备 status=DOWN），mfg `ErpMfgScheduleToJobCardProcessor` 生成 job card 前拉取一次 → 开放停机工作中心的受影响工序跳过生成 + LOG.warn（排产暂停）；停机 complete 后窗口关闭 → 下次生成自然恢复（恢复排产，拉取模型恢复语义免 push）。恢复即时性 = 下次排产执行时点（L2 §4.3「重新计算生产计划」对齐）。
      - Skill: `nop-backend-dev`
- [x] **D4 pom 边 + 矩阵登记**：mfg-service 增 erp-mnt-dao compile 依赖（仅 Facade 接口，矩阵 :109「被 mfg 查」预期方向落地）；Phase 1 assets→mnt-dao 边一并登记 `docs/architecture/data-dependency-matrix.md`（Java 层边注记段；**显式披露 assets↔maintenance 双向域耦合**——mnt-dao 已依赖 ast-dao[pom.xml:41]，ast-service→mnt-dao 构成 Maven 菱形非环，耦合语义与 R1.68 cs→qa 单向叶依赖不同，登记时注明）。
      - Skill: `nop-backend-dev`
- [x] **Proof**：TestErpMntDowntimeSchedulingLinkage（①record → 设备 DOWN + notify 7208 落库（ROLE 生产计划员，无角色数据 config-gated 空投递断言）②complete → RUNNING + notify 7209 ③config 关闭零通知 ④Facade 开放窗口查询数学断言[开放/已完/无工作中心设备三态]）+ mfg 侧 TestErpMfgJobCardDowntimeGate（①工作中心开放停机 → 该 workcenter 工序不生成 job card + 其他 workcenter 不受影响 ②停机 complete 后重新生成恢复 ③mnt bean 缺失 @Nullable 容错零回归）+ 既有 downtime/visit 测试零回归 + `_cases/` 快照。验证命令：`mvn test -pl module-maintenance/erp-mnt-service -pl module-manufacturing/erp-mfg-service`。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 停机排产消费链 + 双向通知落地，mnt/mfg 两域测试绿零回归

### Phase 3 - 验证收口 + 文档回填

Status: completed
Targets: `docs/design/maintenance/equipment-integration.md`、`docs/architecture/data-dependency-matrix.md`、`docs/architecture/job-scheduling.md`（如涉）、`docs/audits/arm-index.md`、`docs/backlog/requirement-compliance-roadmap.md`、`docs/logs/2026/08-{当期}.md`
Skill: none

- Item Types: `Proof | Add`
- Prereqs: Phase 1-2 全绿

- [x] 全量验证：`mvn clean install -DskipTests` BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline（新跨域调用全经 IBiz 注入，预期零漂移）+ TestErpAllJobYamlLoading=29 不变（本计划零新 job.yaml）+ 三域分域全绿。
      - Skill: none
- [x] owner doc 回填：equipment-integration.md §一（处置联动实现注记 + D1 裁决）+ §四/§七（拉取消费模型 + notify 事件 + D3 裁决 + CRP/aps constraint successor）+ data-dependency-matrix.md 两条新 Java 边登记 + arm-index P1-RC-068/070 → done (RC-R1.76/77) + roadmap 两行 done + 行标签 B 类改写 + logs 条目（全绿验证状态）。
      - Skill: none

Exit Criteria:

- [x] 六处回填一致（代码 / equipment-integration / 矩阵 / arm-index / roadmap / logs）

## Draft Review Record

- Independent draft review iteration 1: needs-revision（task `ses_fe951afa6ffe3Hv7r3u8rXmn2B`，2026-08-19）——BLOCKER-1：seed ID 7206/7207 已被 RC-R1.71 占用（三方言 :153/:161 实测），按原文执行将 PK 碰撞 → 改 7208/7209；BLOCKER-2：D2 守卫经 visitBiz.save 内部路径触发，`generateDueVisits` 循环无 per-schedule try/catch 且处置不停用维护计划 → 一条 DECOMMISSIONED 设备到期计划中断整个日批 job（绿基线回归）→ 补内部 save 双路径裁决（批量查询侧排除跳过 + 手工 accept 路径显式拒绝）+ Proof ⑤⑥；MINORS 7 项已采纳：Facade 更正为既有 `IErpMntDowntimeEntryBiz` 扩展 / ast 测试基线 314 / reverseApprove 对称恢复钉 posted 分支防分叉 / mnt-dao→ast-dao 既有依赖披露（菱形非环 + 双向耦合矩阵显式登记）/ Plan 2 文件重叠更正 / job 计数归属 TestErpAllJobYamlLoading / aps 行号 :159-174。设计 D1/D3、scope、授权、closure 结构复核通过。
- Independent draft review iteration 2: acceptable（task `ses_fe948140fffe8CJbzs65JQgO0l`，2026-08-19）——零 BLOCKER；B1/B2 修订全部落地且实测为真（三方言 seed max=7207、7208/7209 空闲；内部 save 双路径裁决完整；IErpMntDowntimeEntryBiz 扩展路径可行[BankLedgerQuery 关联路径过滤先例]；reverseApprove posted 分支钉位与实仓 :141-149 一致；菱形非环 + 双向耦合披露；TestErpAllJobYamlLoading=29）；MINORS 2 项非阻塞（循环体行号锚点语义正确；D2① 查询侧排除措辞兼容两步实现，Proof ⑤ 仅约束行为不约束实现）。**共识达成，计划转 active。**

## Closure Gates

- [x] 范围内行为完成
- [x] 相关文档对齐
- [x] 已运行验证（`mvn clean install -DskipTests` + 分域 `mvn test` + compliance checker）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### mnt 自动生成 aps ErpApsConstraint(MAINTENANCE) 行

- Classification: `optimization candidate`
- Why Not Blocking Closure: mnt→aps S 写超出矩阵允许清单须矩阵修订；拉取消费模型已满足 L1「暂停/恢复排产」断言；aps constraint 自动化提升精确时间轴排产（回填窗口约束）。
- Successor Required: yes（触发条件：RC-R1.86-88 aps 域修复实施时协同接线，矩阵修订一并裁决）

### CRP 负荷停机扣减

- Classification: `watch-only residual`
- Why Not Blocking Closure: L2 §4.2 未断言容量口径；排产门控已达成「暂停受影响工单排产」主语义。
- Successor Required: yes（触发条件：CRP 容量精度需求立项）

## Closure

Status Note: closed（2026-08-19 独立结束审计 PASS，证据见下）

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，task `ses_fe6147d34ffeTrjG80sbdLKFOa`，2026-08-19）——**verdict PASS**（1 项非阻塞 MINOR 已修复）
- Evidence: ①Phase 1 实仓核验——`IErpMntEquipmentBiz.java:25-37` Facade 双方法 @BizMutation + `ErpMntEquipmentBizModel.java:47-61` 实装；`EquipmentStatusLinker.java:76-106` disposalLinkEnabled 门控/no-op/幂等/DISPOSAL 日志源；`ErpAstDisposalProcessor.java:123`（approve 后置）+ `:168`（reverseApprove posted==TRUE 分支 `:156`）+ `:79-81` @Nullable 注入 + 失败异常传播（无 try/catch）；`DecommissionedEquipmentGuard.java:28-41` + Schedule/Request/Visit BizModel save/update 钩子 + VisitScheduleProcessor:35 + RequestAcceptProcessor:35 + ScheduleDueGenerator :102-106/:127-131 双路径批量排除（TIME/RUNTIME）+ beans :27-28；`ErpMntErrors.java:98-99` 错误码中文描述；ast pom :35-39 compile 边。②Phase 2——`IErpMntDowntimeEntryBiz.java:38-39` @BizQuery + MntOpenDowntimeWindow DTO；`ErpMntDowntimeEntryBizModel.java:57-78` 继承 daoFor 无遮蔽字段；`AbstractErpMntDowntimeEntryProcessor.java:75-99` notifyDowntimeEvent 门控+静默降级 + Record:23/Complete:29 接线；三方言 seed :168-177 7208/7209 ROLE 生产计划员 + PK 唯一性 `uniq -d` 空 + max=7209；mfg Processor :75-77 @Nullable + :90-102 拉取 + :109-122 门控 + Generate processor :38-40 + pom :70-74 compile/:174-178 test。③验证复跑（fresh surefire 20:09-20:11）——mnt **147/0/0** + mfg **289/0/0** + ast **318/0/0** + TestErpAllJobYamlLoading 1/0/0（断言恰 29 个 job.yaml）+ checker EXIT=0（R2b=236/R2c=1469 与 compliance-baseline.md BASELINE 块一致 + :540-546 per-site 注记）。④六处回填一致——equipment-integration.md §1.3/§4.2/§4.3/§7.1/§7.3/§7.4/§5.2、矩阵 §2.4 :121-132（菱形/双向耦合披露 :129 + test-scope 注记 :132）、arm-index :253/:255 done、roadmap :468/:469 done B 类改写（对齐 :460 R1.68 先例）、logs :5 条目、Non-Goals 零越界（EventBus/publishEvent/fireEvent grep 零命中 + mfg 无 HOLD 列 + 零 .orm.xml 改动 + module-maintenance 零 ErpApsConstraint）。MINOR 处置：M1 ast 计数 320→318 修正（logs/roadmap/arm-index 三处；surefire XML 权威计数 318 = 314 基线 @Test + 3 新增，console 320 为汇总口径差异，执行者复核确认）；M2 Closure Gates/Status 由执行者按本审计结论落位（本节）；M3 错误码常量名 `ERR_EQUIPMENT_DECOMMISSIONED`（文件级命名约定无 MNT 前缀）与计划速记 `ERR_MNT_EQUIPMENT_DECOMMISSIONED` 字符串/语义完全一致，非偏差。

Follow-up:

- 无已确认缺陷；aps constraint 自动化 / CRP 扣减归 Deferred But Adjudicated successor。
