# 2026-08-21-2025-1-bigint-id-m39-aps-migration 主键/外键 string 化 M3.9：aps 域迁移（冻结序位次 3）

> Plan Status: completed（2026-08-21：四 Phase 执行完毕 + 独立结束审计 `passes closure audit`（0 BLOCKER / 0 MAJOR / 3 MINOR 已整改复验），见 Closure；iteration 1 双独立审查 `passes draft review`（0 BLOCKER / 0 MAJOR）+ 保护区域双独立子 agent 批准，见 Draft Review Record）
> Mission: id-string-migration
> Work Item: M3.9（aps，冻结序位次 3）
> Last Reviewed: 2026-08-21
> Source: `docs/backlog/id-string-migration-roadmap.md` M2/M3 冻结总序表位次 3（M3.9）
> Related: `docs/plans/2026-08-21-1045-1-bigint-id-m0-order-freeze-audit-proofs.md`（M0.1）、`docs/plans/2026-08-21-1045-2-bigint-id-m13-common-service-orgid-string.md`（M1.3）、`docs/plans/2026-08-21-1045-3-bigint-id-m11-master-data-migration.md`（M1.1）、`docs/plans/2026-08-21-1657-2-bigint-id-m12-notify-migration.md`（M1.2）、`docs/plans/2026-08-21-1657-1-bigint-id-m02-forward-coupling-registry.md`（M0.2 登记册，消费来源）
> Audit: required（保护区域 `model/*.orm.xml`：独立 plan-audit + 双独立子 agent 批准，批准记录落盘本文件）

## Current Baseline

- **aps 域规模（2026-08-21 实况 scan，`tools/check-bigint-id-types.mjs` scan）**：`module-aps/model/app-erp-aps.orm.xml` 需改列 **26 = 自有 25（PK 7 + BIGINT FK 18，7 实体）+ notGenCode 外部实体 stub 1**。自有 PK：`ErpApsOperationOrder/Schedule/Constraint/OpRouting/DispatchRule/DispatchLog/CapacityReservation` 各 `.id`；自有 FK 含 `orgId` ×7（M1.3 common-service 已 String 语义，无额外适配面）、`workOrderId`/`machineId`/`operationOrderId`/`workcenterId` 等本域 FK。**不改列**：VARCHAR FK `assignedToId`（user，显式 string 本就 String）+ `delVersion` ×6（ErpApsCapacityReservation 无该列；工具「未分类」段实列 6，第 7 处文本命中为 `<domain>` 声明非列定义；非 PK/FK 保持 long，路线图规则 4）。
- **notGenCode stub（本域特有，需显式登记）**：aps orm 末尾嵌入 md 外部实体 stub `ErpMdOrganization`（约 :365-371，`notGenCode="true"`）——md 权威源（module-master-data/model）自 M1.1 已 String，stub 翻转为**与权威源对齐**（非新增决策）；dry-run 副本已核证 stub id 一并翻转（`_tmp` 时点副本 stdDataType="string"）。stub 不生成代码（`biz:moduleId="erp/md"`），无编译面，仅元数据一致性。
- **模块链与编译依赖（pom 实测）**：7 模块 = `module-aps/erp-aps-{codegen,dao,meta,service,web,app,api}`。aps-dao main compile 依赖 **md-dao**（orm 6 处 `org → ErpMdOrganization` to-one；另有 1 处本域 refEntityName `DispatchLog → ErpApsOperationOrder` :313，非跨域）——aps-dao `_gen` 关系胶水（`setOrgId(org.getId())`）自 M1.1 起处于已登记中间态（D3 对称耦合：Long glue ← String md），**本域迁移即自愈**（两端同 String）。aps-service main compile 依赖 **inv-dao + mfg-dao（均未迁移 Long）+ notify-dao（已 String）**；test 依赖 notify-service（已 String）+ app-erp-common-test（Proxy 桩，M1.2 已证编译安全）。
- **M0.2 登记册 aps 视角（§6.3，起草消费已核）**：A1 orm 延后 = 0；**A2 main 桥接 15 条**（inv 4：`ErpApsAtpCtpServiceImpl:12/13/14` + `ErpApsAutoDispatchProcessor:10`，退役 owner M2.2；mfg 11：`ErpApsAtpCtpServiceImpl:15/16`、`ApsLoadSourceProvider:5/6`、`ErpApsAutoDispatchProcessor:11-14`、`ErpApsWorkOrderToOperationProcessor:8/9/10`，退役 owner M3.1）——含 `ApsLoadSlot`/`IErpApsLoadSourceProvider` SPI（接口/数据类定义于 mfg-dao `app.erp.mfg.biz` 包，aps 实现侧）；**A3 test 桥接 4 条**（`TestErpApsAutoDispatch`/`TestErpApsCrossDomainIntegration`/`TestErpApsDemandPlanning`/`TestErpApsWorkOrderToOperationOrder` import inv/mfg 实体或 SPI）；C1 后向 main 1 条（backward-133 → notify：`ErpApsAutoDispatchProcessor`/`ErpApsWorkOrderToOperationProcessor`，M1.2 后已 String，编译器驱动修复定位面）；C2 后向 test 1 条（backward-193 → notify：同域 2 测试文件）；B 退役义务 = 0。
- **手写代码冲击面（实测）**：dao 手写 Long 相关 7 文件（`IErpApsOperationOrderBiz`/`IErpApsScheduleBiz`/`IErpApsAtpCtpService` + 值对象 `ScheduledOperationView`/`SchedulingResult`/`ConflictReport`/`WorkOrderOperationCreationResult`）；service 手写 Long 相关 12 文件（BizModel ×2、AtpCtpServiceImpl、ApsLoadSourceProvider、SchedulingEngine、Processor ×7）；api beans 14 文件为 **codegen 生成件**（api 模块零手写，M0.1 已核；orm 翻转后随 codegen 重生成为 String，零手改——M1.1/M1.2 先例「api beans 随动」）。`.getId()` main 手写 47 处。执行时以编译器清单为准。
- **测试资产（实测）**：service 13 个 `@Test` 类（CrudSmoke/StateMachine Matrix/BaselineIoC/DeltaOverride ×3/StateGuards/AlternativeRouting/AutoDispatch/CapacityReservation/CrossDomainIntegration/DemandPlanning/ScheduleManagement/SchedulingEngine/WorkOrderToOperationOrder）+ `ErpApsOperationOrderStateMachineDelta` helper；`_cases` 快照 182 文件（104 csv + 68 yaml + 10 json5——json5 为 CrudSmoke 输出 response 快照，重录后 id 字符串形态落盘）+ codegen 入口类；`ErpApsWebPagesTest` `@Tag("full-app")` + erp-aps-web pom surefire `<excludedGroups>` 模块级排除（先于本 mission 的已提交治理决策 plan 2026-07-24-0930-1，页面校验 successor = M4.1）。
- **已知风险（md/notify 先例登记）**：① 平台 IoC 回归 `nopSequenceGenerator` bean-init-self-wait（`docs/bugs/2026-08-21-nop-sequence-generator-ioc-self-wait-*.md`）——若复现按先例修复（test-scope VFS delta `ioc:lazy-property` 镜像平台先例，aps 现有 `test-aps-delta` 仅覆盖 erp/aps 域 beans，需新增 `nop/sys/beans/app-dao.beans.xml` delta）；② no-am 测试 classpath VFS 模块集变化（已登记中间态，回退方案 = seq-proof-yaml 模块禁用模式）；③ 陈旧 jar 二进制不兼容（本地仓未迁移 inv/mfg dao jar 引用旧 `getId()` 签名——aps 域级测试按设计不跨这些边界）。
- **回写机制（M0.1 裁定 Decision A，三步）**：① `node tools/check-bigint-id-types.mjs dry-run` 时点刷新；② `node tools/verify-id-fix-copy-diff.mjs module-aps` 新鲜度门控（零非 stdDataType 行）；③ 单文件落源 + `git diff` 逐行审核。禁止盲 cp 静态副本、禁止 apply 模式回写。
- **剩余差距**：aps orm 26 列全 `stdDataType="long"` 待改；aps 手写代码/测试/快照全部 Long 形态；冻结序位次 3（aps 之后位次 4 b2b 待迁移）。

## Goals

- aps 域 26 列（自有 25 + md stub 1）`stdDataType` long→string 落源（唯一源文件变更，`stdSqlType` 保持 BIGINT，DDL 零变化）。
- 增量重生成（no-am 7 模块链）+ 编译器驱动修复 aps 全部手写代码（dao 7 文件 + service 12 文件 Long 语义）+ A2 前向桥接 15 处落桥（String↔Long 转换 + grep 例外登记，退役 owner M2.2/M3.1）。
- 快照每域重录（RECORDING→CHECKING，用户裁决——不依赖 Number 宽容）。
- 语义陷阱 grep 门控清零（路线图横切 §3 清单，aps 范围；含 bridge-main-032 类语义引用不适用——aps 无此类）。
- 消费 M0.2 登记册：A2/A3 桥接 disposition 落盘本计划，C1/C2 修复定位面消费，heal M1.1 登记的 aps-dao `_gen` 对称胶水中间态。
- 路线图 M3.9 → `done` + 日志；冻结序位次 4（b2b）解锁。

## Non-Goals

- 不修复外域代码对 aps 的引用（aps 被引用 main/test = 0/0，登记册 §2 矩阵——aps 为叶子域，无下游登记义务）。
- 不迁移 inv/mfg 域（A2 桥接目标域，归 M2.2/M3.1）；不修复 inv/mfg 自身代码。
- 不改 `delVersion` 等非 PK/FK BIGINT 列（保持 long）；不改 VARCHAR FK 列（本就 String）。
- 不跑全量构建/全量测试/E2E（归 M4.1）；不跑 compliance checker（归 M4.1）。
- 不手改任何生成件（`_gen/`、`_` 前缀文件、api beans）；手写 view.xml 预期零改动（dict/md/notify 先例已实证，Phase 4 验证）。
- 不修 `ErpApsWebPagesTest` 的治理排除（已提交决策，successor = M4.1）。
- 不做 aps owner docs 之外的文档重写（`domain-design-guidelines.md` §16A 清理归 M4.1）。

## Task Route

- Type: `implementation-only change`（含保护区域 ORM 变更）
- Owner Docs: `docs/backlog/id-string-migration-roadmap.md` M2/M3 表位次 3 + 横切 §5 设计证据（`../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md` §主键设计方案 B + `docs/design/domain-design-guidelines.md` §16A.4 + M0.1 审计结论 + M0 裁决 §10）；aps 业务语义 owner doc = `docs/design/aps/`（Phase 4 注记对象）
- Skill Selection Basis: 路线图 §M1-M3「预期技能」指定域迁移 plan 加载 `nop-backend-dev`（BizModel/跨实体约定/桥接修复）+ `nop-testing`（快照重录 RECORDING→CHECKING 流程）；ORM 变更机制由 M0.1 审计与平台文档背书。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（无 DB DDL 变更、无端口/密钥/外部服务；DB 列保持 BIGINT）。no-am 构建硬前置 = 最后全绿基线 commit 全量 install + md/notify/common 链 install（M1.1/M1.2/M1.3 产物，均已就位）+ inv-dao/mfg-dao 基线 jar（未迁移 Long，正是桥接对象）。回滚策略：revert orm.xml + `mvn clean install -pl module-aps/erp-aps-codegen,module-aps/erp-aps-dao,module-aps/erp-aps-meta,module-aps/erp-aps-service,module-aps/erp-aps-web,module-aps/erp-aps-app,module-aps/erp-aps-api -DskipTests` 重生成回 Long 形态。

## Execution Plan

### Phase 1 - 消费登记册 + orm 回写（保护区域，双批准前置）

Status: completed（2026-08-21 执行记录：登记册消费核对一致——A1 orm 延后 = 0（grep 登记册 json5 无 aps orm-column-deferral 条目）；A2 main 桥接 15 条 = bridge-main-009..023（inv 4：009/010/011/016；mfg 11：012/013/014/015/017..023）与 Current Baseline 预对账一致；A3 = bridge-test-103..106（4 条）；C1 = backward-133（2 main 文件）/C2 = backward-193（2 test 文件）——零冲突。回写三步执行：dry-run 全量刷新（1579 列副本）→ `verify-id-fix-copy-diff.mjs module-aps` 门控通过（26 变更行、零非 stdDataType 行、延后列 0）→ 单文件 cp 落源。git diff 逐行核对：26 行 = id ×8（7 自有 PK + 1 notGenCode `ErpMdOrganization.id` stub）+ orgId ×7 + machineId ×4 + workcenterId ×2 + operationOrderId ×2 + workOrderId ×1 + selectedRoutingId ×1 + operationId ×1，每行仅 `stdDataType="long"→"string"`（归一化对比证实行内容其余部分逐字节一致），`stdSqlType="BIGINT"` 26/26 保留，`assignedToId`/`delVersion`/标签结构零变化。工具重扫 aps 段：26 列全 `ok`（含 stub `(notGenCode) ok`），零 `NEEDS FIX`/零 `DEFERRED`；残留 6 处 `long` 均为未分类 `delVersion`（规则 4 不改，CapacityReservation 无该列核实）。双独立子 agent 批准记录已在案（Draft Review Record 批准 1/2，ses_fdb72ef78ffeO6h9p4E7Wu5RP4 + ses_fdb72bf11ffeNkF0bF4vCzd777，2026-08-21））
Targets: `module-aps/model/app-erp-aps.orm.xml`
Skill: none

- Item Types: `Proof | Fix`
- Prereqs: M0.1 ✅ + M0.2 ✅ + M1.3 ✅ + M1.1 ✅ + M1.2 ✅（冻结序位次 1/2 已 done，位次 3 解锁）；本计划已通过独立 plan-audit + 第二独立子 agent 复核（保护区域 `auto + dual-agent-approval`，批准记录落盘 Draft Review Record）

- [x] Proof: 消费 M0.2 登记册——读取 `tools/id-migration-registry.json5` + `docs/audits/2026-08-21-1657-id-m02-forward-coupling-registry.md` §6.3 aps 节，逐条核对：(i) A1 orm 延后 = 0（工具豁免不适用，26 列全翻转）；(ii) A2 main 桥接 15 条与本地实测 import 对账（本计划 Current Baseline 已预对账一致，执行时以登记册+实况双源复核）；(iii) A3 test 桥接 4 条作为 Phase 3 定位面；(iv) C1/C2 后向指针（notify 2 main + 2 test 文件）作为 Phase 2/3 修复定位面。若登记册存在 aps 条目与本地实测矛盾，按路线图规则 6 停止回报（真相源冲突不得自行裁定）。
  - Skill: none
- [x] Proof: 双独立子 agent 批准记录落盘（批准人指针 + 结论 + 时间），未获批不得进入回写。
  - Skill: none
- [x] Fix: 回写 orm（M0.1 裁定三步机制）——① `node tools/check-bigint-id-types.mjs dry-run` 时点刷新；② `node tools/verify-id-fix-copy-diff.mjs module-aps` 新鲜度门控（零非 stdDataType 行）；③ 门控通过后单文件落源。禁止盲 cp 静态副本、禁止 apply 模式。
  - Skill: none
- [x] Proof: `git diff module-aps/model/app-erp-aps.orm.xml` 逐行核对——仅 26 列 `stdDataType="long"→"string"`（自有 25 = PK 7 + FK 18 + notGenCode `ErpMdOrganization.id` stub 1），`stdSqlType` 零变化、`assignedToId`/`delVersion`/标签结构零变化；`node tools/check-bigint-id-types.mjs` scan aps 段重扫零 `NEEDS FIX`/零 `DEFERRED` 残留。
  - Skill: none

Exit Criteria:

- [x] 登记册消费核对在案（零冲突或已停止回报）；双批准记录在案；新鲜度门控 + git diff + 工具重扫三重证明变更面精确 = 26 列 stdDataType

### Phase 2 - 增量重生成 + 主代码编译修复 + A2 桥接落桥

Status: completed（2026-08-21 执行记录：7 模块链 `clean install`（显式列表、no-am、`-Dmaven.test.skip=true`）**BUILD SUCCESS**。重生成验证：dao `_gen` 7 实体 String 化 + md org 关系胶水 `setOrgId(refEntity.getId())` 两端同 String 自愈（M1.1 登记中间态 D3 兑现）；meta xmeta/_templates 7 + web `_gen` view 7 + api beans 14 全部随动重生成（Long→String）；**手写 view 零改动**（git status 证实仅 `_gen/` 前缀文件变更）。
**主代码修复清单（编译器驱动，两轮）**：第一轮 37 错/9 文件 + 第二轮 9 错（WorkCenterTimeline 构造器 long→String）→ 0 错。分层：
- dao 手写 7 文件：`IErpApsOperationOrderBiz`（13 处 Long 参数→String：workOrderId/operationOrderId×8/routingId/scheduleId×2/materialId×2/machineId/opOrderId）、`IErpApsScheduleBiz`（id ×2）、`IErpApsAtpCtpService`（materialId ×3）、`SchedulingResult`（List<Long>→List<String> + addConflict/addScheduled 签名）、`ConflictReport`（operationOrderId）、`ScheduledOperationView`（workcenterId）、`WorkOrderOperationCreationResult`（workOrderId）。
- service 手写 13 文件（基线预估 12 + 执行期 `WorkCenterTimeline` 构造器/字段随引擎 Map 键 String 化联动）：`ErpApsOperationOrderBizModel`（全部 @Name Long 参数→String + requireEntity 直传 + byWorkOrder Map<String,?> + batchScheduleForward 移除 Long.valueOf/NumberFormatException 死分支）、`ErpApsScheduleBizModel`（publish/archive id ×2）、`ErpApsSchedulingProcessor`（6 个 protected helper Long 参数→String：loadPlannedInWindow/loadMaintenanceConstraintsByMachine/hasOverlappingReservation/releaseReservationsByOrder/requireSchedule/requireOperationOrder）、ScheduleForward/ScheduleBackward/InsertRushOrder 3 Processor（入口签名）、`ErpApsRoutingManualOverrideProcessor`（operationOrderId/routingId + requireRouting/findRouting）、`ErpApsAutoDispatchProcessor`（dispatchManually/hold/unhold/requireOp/countRunningOps→String + 2 处 mfg 桥）、`ErpApsWorkOrderToOperationProcessor`（入口→String + 4 处 mfg 桥）、`ErpApsSchedulingEngine`（Map<Long,WorkCenterTimeline>×5 + Map<Long,OpChain>×4 → Map<String,*> + RoutingCandidate.machineId→String）、`WorkCenterTimeline`（machineId long→String 字段/构造器/getter）、`ErpApsAtpCtpServiceImpl`（入口 String→内部 Long materialKey 桥 + Map<String,WorkCenterTimeline> + bottleneckWc String）、`ApsLoadSourceProvider`（SPI 桥，见下）。
**A2 桥接例外清单（15 条落桥，退役 owner M2.2×4 / M3.1×11）**：
| 条目 | file:line | 转换方向 |
| --- | --- | --- |
| bridge-main-009/010/011 | ErpApsAtpCtpServiceImpl 入口 `ConvertHelper.toLong(materialId)` → atpAvailable/sumOnHand/sumReserved（inv ErpInvStockBalance/Reservation/ReservationLine 查询） | aps String → inv Long |
| bridge-main-012/013 | ErpApsAtpCtpServiceImpl findDefaultBom/loadBomOperations（mfg ErpMfgBom productId / ErpMfgBomOperation bomId 查询）+ buildShadowOps `ConvertHelper.toString(bo.getWorkcenterId())` | mfg Long ↔ aps String |
| bridge-main-014/015 | ApsLoadSourceProvider findScheduledSlots：入参 List<Long>→List<String> 过滤 aps workOrderId；出参 slot.operationOrderId/workOrderId/workcenterId `ConvertHelper.toLong` 回填 mfg Long DTO | mfg Long ↔ aps String（SPI ApsLoadSlot/IErpApsLoadSourceProvider 定义于 mfg-dao） |
| bridge-main-016 | ErpApsAutoDispatchProcessor sumAvailable(Long materialId)（inv ErpInvStockBalance materialId 查询） | inv Long 内部保持 |
| bridge-main-017/018 | ErpApsAutoDispatchProcessor resolveBom(ErpMfgWorkOrder)/loadBomLines(Long bomId)（mfg ErpMfgBom/BomLine） | mfg Long 内部保持 |
| bridge-main-019 | ErpApsAutoDispatchProcessor checkMaterialAvailability :263 `ConvertHelper.toLong(op.getWorkOrderId())` → mfg ErpMfgWorkOrder.getEntityById | aps String → mfg Long |
| bridge-main-020 | ErpApsAutoDispatchProcessor resolveMaxConcurrentOps :230 `ConvertHelper.toLong(rule.getWorkcenterId())` → mfg ErpMfgWorkcenter.getEntityById | aps String → mfg Long |
| bridge-main-021/023 | ErpApsWorkOrderToOperationProcessor loadRoutingOperations（mfg ErpMfgRoutingOperation routingId Long 查询）+ workcenterExists(Long)（mfg ErpMfgWorkcenter）——mfg Long 内部保持（requireWorkOrder 的 String→Long 入口桥归 022） | aps String ↔ mfg Long |
| bridge-main-022 | ErpApsWorkOrderToOperationProcessor :75/:79/:114 `ConvertHelper.toString(wo.getId())` + buildOperationOrder :173/:175/:179 `ConvertHelper.toString`（wo.getId()/rop.getWorkcenterId()/wo.getOrgId()） | mfg Long → aps String |
**C1 后向修复（backward-133）**：notify API 自 M1.2 为 String 语义且 `notify(String,Map,ctx)` 签名不变——两 Processor 的 notificationBiz.notify 调用零编译错误、零适配需求（M1.2 登记的「main 侧零破坏」核证成立）；ctx map 值 Long 序列化为 JSON 数字，模板插值兼容。
**自身链破坏处置**：no-am 口径下零未登记破坏（7 模块全绿，无陈旧 jar 编译期表现）。）
Targets: `module-aps/erp-aps-dao/src/main/java/**`、`module-aps/erp-aps-service/src/main/java/**`（手写接口/实现/BizModel/Processor/Engine/SPI 实现侧；web main 手写实测 0；api beans 为生成件随动）
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: Phase 1

- [x] Fix: `mvn clean install -pl module-aps/erp-aps-codegen,module-aps/erp-aps-dao,module-aps/erp-aps-meta,module-aps/erp-aps-service,module-aps/erp-aps-web,module-aps/erp-aps-app,module-aps/erp-aps-api -Dmaven.test.skip=true`（D3 口径：7 模块显式列表、**不带 `-am`**、**必须 `-Dmaven.test.skip=true`** 隔离测试编译）触发增量重生成（`_gen/` 实体、I*Biz、xmeta、view、api 契约；生成件零手改）。预期：aps-dao `_gen` md org 关系胶水自 M1.1 的登记中间态自愈（两端同 String）。
  - Skill: `nop-backend-dev`
- [x] Fix: 编译器驱动修复主代码——逐条修复 aps dao + service 手写代码类型错误（基线预判：dao 7 文件 IBiz 签名/值对象字段、service 12 文件 Long 语义 + 47 处 `.getId()` 下游；以编译器实际清单为准），直到 7 模块链 `-Dmaven.test.skip=true` 构建全绿。修复清单（错误类型 × 处数，分 dao/service 层）落盘本计划；测试编译错误由 Phase 3 首轮 `test-compile` 产生后修复（`-Dmaven.test.skip=true` 阶段不编译测试，无移交清单）。
  - Skill: `nop-backend-dev`
- [x] Fix: A2 前向桥接 15 处落桥（D4 消费协议）——aps String id ↔ inv/mfg Long API 的调用点加转换桥（`String.valueOf`/`ConvertHelper` 方向以编译器清单为准；含 `ApsLoadSlot`/`IErpApsLoadSourceProvider` SPI 桥接面），每处登记 grep 例外清单（条目 id + file:line + 转换方向），退役 owner M2.2（inv 4 条）/M3.1（mfg 11 条）。
  - Skill: `nop-backend-dev`
- [x] Fix: C1 后向修复——notify 引用点（`ErpApsAutoDispatchProcessor`/`ErpApsWorkOrderToOperationProcessor`）对已 String 化 notify API 的编译适配（编译器驱动，定位面 = 登记册 backward-133）。
  - Skill: `nop-backend-dev`
- [x] Fix: 自身链破坏处置（D4 carve-out）——no-am 口径下 reactor 不含外域模块，预期零外域破坏；若出现**未登记**编译破坏（含本地仓陈旧 jar 二进制不兼容的编译期表现），按路线图规则 6 停止回报；登记册内已登记破坏按已登记中间态继续并履行登记义务（破坏模块清单 + successor 指针 + 逐模块 javac 错误点清单）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] aps 7 模块链（显式列表、no-am、`-Dmaven.test.skip=true`）构建全绿（main 代码）；主代码修复清单 + A2 桥接例外清单在案

### Phase 3 - 测试修复 + A3 桥接适配 + 快照重录 + 域级测试

Status: completed（2026-08-21 执行记录：首轮 `test-compile` 产生 9 错/3 文件（TestErpApsCapacityReservation :221/:224、TestErpApsAlternativeRouting :236/:237、TestErpApsAutoDispatch :373/:413/:416/:434/:518）→ 逐文件修复至零编译错。**测试修复清单（10 文件）**：TestErpApsCapacityReservation（MACHINE_A Long→String + seedReservation/findReservationsByOrder/setLatest/reloadOp/createSchedule/createOp/createOpPlanned/baseOp/saveOp/idOf 签名 String 化 + 字面量 1L/2L→"1"/"2" + 999_001L→"999001"）、TestErpApsAlternativeRouting（WC_PRIMARY/WC_ALT/OP_DEF Long→String + seedRouting/seedRoutingFull/setRoutingEnabled/createConstraint/clearMaintenance/createOp 族/createSchedule/runScheduleForward/reloadOp String 化 + seedRouting id 字面量 101L..162L→"101".."162" + routingId 请求参数 142L/999999L→String + disabledRoutingId String + machineId 断言去 toLong 直比 String + workOrderId 7001L→"7001"）、TestErpApsAutoDispatch（WC/RULE_ID/NOTIFY_TPL Long→String + seedOpWorkOrder(Long opId, Long workOrderId) 半 String 化（workOrderId String.valueOf 桥至 aps 列）+ seedNotifyTemplate/idOf/status/reloadOp/latestLog/countLogs/seedOp 族 String 化 + 6601L→"6601" + 5502L 调用桥）、TestErpApsOperationOrderStateGuards/TestErpApsScheduleManagement（idOf Number 分支删 + createOrder/createSchedule 返回 String）、TestErpApsSchedulingEngine（MACHINE_A/MACHINE_B→String + createOp 族/createConstraint/createSchedule/setLatest/setPlanned/runScheduleForward/reloadOp/idOf String 化 + workOrderId 字面量桥 + materialId 9999L→"9999"）、TestErpApsCrossDomainIntegration（**A3 bridge-test-104**：GraphQL map 值 String.valueOf(workOrderId/machineId) 桥，SPI 断言保持 mfg Long 语义）、TestErpApsDemandPlanning（**A3 bridge-test-105**：materialId 请求值 String.valueOf 桥，inv 种子保持 Long）、TestErpApsWorkOrderToOperationOrder（**A3 bridge-test-106 + C2 backward-193**：createFromWorkOrder/findOps String.valueOf 桥 + 断言 String.valueOf(WO_FULL/WC_1/WC_2) + createSchedule 返回 String + notify 模板 id 9401L/9402L→"9401"/"9402"（C2 notify String id 适配）；mfg 种子 WC_1/ROUTING_*/WO_* 保持 Long）。statemachine 3 测试 + CrudSmoke 零 Long 改动（GraphQL 数字标量由引擎 coerce）。
**平台 IoC 回归复现与修复**：首轮 `mvn test` 13 类全挂 `nop.err.ioc.bean-init-self-wait(nopSequenceGenerator)`（已知风险①如期复现）——按 md/notify 先例新增 test-scope VFS delta `_vfs/_delta/default/nop/sys/beans/app-dao.beans.xml`（ioc:lazy-property 镜像，注释指向本 plan）修复 12 类；第 13 类 TestErpApsOperationOrderStateMachineDeltaOverride 因 `delta-layer-ids=test-aps-delta` 替换 default 层集而失效——对齐 md 先例（TestErpMdSupplierApprovalStateMachineDeltaOverride `value = "default,test-md-delta"`）改为 `value = "default,test-aps-delta"` 后全绿。
**快照重录（RECORDING→CHECKING）**：12 测试类临时加 `snapshotTest = SnapshotTest.RECORDING` 运行（76 方法，67 个预期 `snapshot-finished` 异常，零真实失败）→ 足迹 = 32 文件内容 diff（json5 id/FK 字段数字→引号 String 形态（testCreateHead `"id": "1"`/`"workOrderId": "1"`/`"machineId": "1"` 实证）+ CSV 列集刷新（OperationOrder CSV 增 R1.87 四列 SELECTED_ROUTING_ID/ROUTING_SELECTION_REASON/MANUAL_OVERRIDE/ALLOW_FALLBACK，旘认快照滞后于 08-20 R1.87 落地）+ CRLF 行尾（M1.1 先例同款））+ 103 新增落盘（此前无 table 快照方法的 input/output tables CSV，含 TestErpApsFkNameLoader 孤儿 autotest.yaml 不变）→ 逐案审核后 1 处确定性修正：TestErpApsAutoDispatch/testMaterialShortageHoldsAndNotifies 的 erp_sys_notification.csv PAYLOAD_JSON 单元格含墙钟时间（notifyShortage payload 内嵌 plannedStartDateT.toString()，方法首次落盘 table 快照即天然 flaky）→ 置 `*` 通配（行级其余断言保留：模板/类型/接收人/STATUS，层 1 Java 断言不变）→ 注解还原（grep RECORDING/forceSaveOutput 零残留）→ CHECKING 复跑 **76/76 全绿 ×2 次**（稳定性确认）。
**域级测试命令**：`mvn test -pl module-aps/erp-aps-service,module-aps/erp-aps-web`（no-am）——service 76/76 绿（12 测试类 + Matrix 9 + statemachine 6）+ web BUILD SUCCESS 0 tests（`ErpApsWebPagesTest` 治理排除预期）。Closure Gates 口径 `mvn clean install -pl <7 模块> -DskipTests` 亦全绿。）
Targets: `module-aps/**/src/test/**`、`module-aps/erp-aps-service/_cases/**`
Skill: `nop-testing`

- Item Types: `Fix | Proof`
- Prereqs: Phase 2

- [x] Fix: 测试代码修复——13 个 service 测试类的 Long 用法（`Long`/`long` 字面量断言、helper 签名、seed `orm_propValueByName("id", id)` 形态——md/notify 先例），逐文件修复至测试编译通过；测试编译错误由本轮 `test-compile` 产生（`-Dmaven.test.skip=true` 阶段不编译测试）。
  - Skill: `nop-testing`
- [x] Fix: A3 test 桥接适配（4 文件，登记册 bridge-test-103..106）——本域测试引用 inv/mfg 实体/SPI 的 id 形态桥接（String↔Long 局部转换，与 Phase 2 桥接同型），适配后在登记册退役对应 test 桥接条目（owner M3.9 = 本计划）。
  - Skill: `nop-testing`
- [x] Fix: C2 后向 test 适配——2 个 notify 引用测试文件（backward-193）对 String 化 notify API 的适配（M1.2 登记的 successor 义务，本计划兑付）。
  - Skill: `nop-testing`
- [x] Fix: 快照每域重录（用户裁决固定步骤）——`RECORDING` 模式运行 aps service 测试 → 逐案审核 `_cases/` 新形态（182 文件基线：104 csv + 68 yaml + 10 json5；id 以 String 形态落盘，json5 输出快照数字→字符串形态重点审核）→ 注解还原（grep 零 RECORDING/forceSaveOutput 残留）→ 切回 `CHECKING` 复跑确认全绿。重录足迹（内容 diff vs 新增落盘分列）与审核结论记录本计划。
  - Skill: `nop-testing`
- [x] Proof: `mvn test -pl module-aps/erp-aps-service,module-aps/erp-aps-web`（D3 口径：不带 `-am`）全绿——service 13 测试类 + web 模块 BUILD SUCCESS（`ErpApsWebPagesTest` 按已提交治理排除，0 tests 预期）。若复现平台 IoC 回归（`nopSequenceGenerator` self-wait），按 md/notify 先例修复（test-scope VFS delta）并在执行记录登记。
  - Skill: `nop-testing`

Exit Criteria:

- [x] aps 域级测试全绿（service 13 类；web 治理排除偏差登记）；快照重录完成且 `CHECKING` 复跑通过；重录清单在案

### Phase 4 - 语义陷阱 grep 门控 + 收尾登记

Status: completed（2026-08-21 执行记录：**grep 门控结果逐项**——① `.longValue()`：1 命中 = TestErpApsSchedulingEngine:316 `countOpOrders()` 解析 findPage **total 计数**（非 id，合法）；② `Long.parseLong(`：同上 1 处（total 字符串兜底，非 id）；③ `Map<Long`：1 命中 = ErpApsAutoDispatchProcessor:280 `Map<Long, BigDecimal> requiredByMaterial`（键 = mfg `ErpMfgBomLine.getMaterialId()`，**A2 桥接登记例外** bridge-main-017/018，退役 owner M2.2/M3.1）；④ `Set<Long`/`String.format("%d`/`%d` 变体：零命中；⑤ Long 装箱 id `==`/`!=`：零命中（残留 `.equals` 均为 String equals 或常量字符串）；⑥ 残留 `Long` 逐条核清 = ErpApsAtpCtpServiceImpl materialKey/reservationId/bomId（inv/mfg 侧 A2 桥接例外）+ AutoDispatch bomId/materialId（同）+ WorkOrderToOperation workcenterId（mfg 参数）+ LoadSourceProvider SPI Long 集（mfg 契约）——全部为登记例外，非 aps id；⑦ `sql-lib.xml` 仓内零存在（M0.1 已核，本计划复核维持）。**view 零手改动验证**：`git status module-aps/erp-aps-web` = 仅 7 个 `_gen/` 前缀 view 变更（codegen 随动），手写 view 零被动变更。**登记册更新**：bridge-test-103..106 status → `retired` + note 补退役指针（指向本 plan Phase 3 桥接实现，晚域 M2.2/M3.1 翻转时移除桥接点）；A2 main 15 条保持 active（退役 owner M2.2/M3.1，代码内 bridge-main-XXX 注释双向指针在案）。**owner doc 注记**：grep `docs/design/aps/` 8 文件 Long/BIGINT/数字 id 陈述 = 零命中 → 「零 Long id 陈述，零文档变更」结论。**roadmap + 日志**：M3.9 → done（位次 3 行证据摘要 + 头部最后更新 + 位次 4 b2b 解锁）+ `docs/logs/2026/08-21.md` 条目（含验证状态全绿）。）
Targets: `module-aps/**`（手写代码）、`docs/backlog/id-string-migration-roadmap.md`、`docs/logs/2026/{08-21 或执行日}.md`、`tools/id-migration-registry.json5`
Skill: none

- Item Types: `Proof | Add`
- Prereqs: Phase 3

- [x] Proof: 语义陷阱 grep 门控（路线图横切 §3，aps 手写 main+test 范围）清零——`\.longValue\(\)`、`Long\.parseLong\(`、`Map<Long`、`Set<Long`、`String\.format\("%d` 及 `%d` 变体零命中（A2 桥接转换点为**登记例外**，逐条列于例外清单并标注退役 owner）；Long 装箱 `==`/`!=` 比较（id 上下文）逐条核清；残留 `Long` 逐条判定合法非 id（如 delVersion）或登记 successor；sql-lib.xml 仓内零存在（M0.1 已核，注明即可）。结果逐项记录本计划。
  - Skill: none
- [x] Proof: 手写 view.xml 零改动验证——`git status module-aps/erp-aps-web` 确认无手写 view 文件被动变更（生成 view 随 codegen 更新不在此列）。
  - Skill: none
- [x] Add: 登记册状态更新——A3 test 桥接 4 条（bridge-test-103..106）status → retired（owner M3.9）；A2 main 桥接 15 条保持 active（退役 owner M2.2/M3.1，晚域翻转时退役并移除本域桥接点——本计划在登记册/例外清单中留双向指针）。
  - Skill: none
- [x] Add: owner doc 注记——grep `docs/design/aps/` 中关于 aps id 为 Long/数字的陈述；存在则就地注记 Java 层已 String 化（引用本计划），不存在则记录「零 Long id 陈述，零文档变更」结论。
  - Skill: none
- [x] Add: 路线图 M3.9 → `done`（Work Item Status 表 M2/M3 行位次 3 + 头部「最后更新」；位次 4 b2b 解锁）+ 日志条目（含验证状态）。
  - Skill: none

Exit Criteria:

- [x] grep 门控零残留（例外为零或逐条核清记录 + 桥接例外清单在案）；view 零手改动在案
- [x] 路线图状态、登记册退役、日志三者一致

## Draft Review Record

- Independent draft review iteration 1（2026-08-21，双独立子 agent fresh session）：
  - 审查者 A（技术/执行视角 plan-audit，ses_fdb72ef78ffeO6h9p4E7Wu5RP4）：`passes draft review` — 0 BLOCKER / 0 MAJOR / 3 MINOR。事实核对全部属实（26 列机器复核精确 = 7 PK + 18 FK + 1 stub；A2 15 条/A3 4 条/C1 C2 行级吻合；测试资产/构建口径/治理排除逐项在案）。MINOR：① delVersion ×7 → ×6（CapacityReservation 无该列，第 7 处为 `<domain>` 声明）→ 已修正；② Phase 2「测试编译错误清单移交」在 `-Dmaven.test.skip=true` 下不可产生 → 已改「Phase 3 首轮 test-compile 产生」；③ stub 行区间 :365-371 → :365-372 → 已修正。
  - 审查者 B（治理/规范视角，ses_fdb72bf11ffeNkF0bF4vCzd777）：`passes draft review` — 0 BLOCKER / 0 MAJOR / 2 MINOR。模板/反松弛/Item Types/Skill/Non-Goals/Closure Gates/路线图对齐/登记册协议/保护区域三触点全部 PASS（登记册 json5 机器核对 aps 条目全集一致）。MINOR：① WebPagesTest 未入 Deferred But Adjudicated（notify 先例格式）→ 已补；② :15 本域 refEntityName 归属措辞 → 已修正。
  - 批准后 MINOR 修订注记：5 项 MINOR 均为事实精度/格式补齐，未变更范围、D3 命令口径与批准依据（orm 变更面仍 = 26 列 stdDataType long→string）。
  - **双独立子 agent 批准（保护区域 `model/*.orm.xml`，`ai-autonomy-policy.md` `auto + dual-agent-approval`）**：
    - 批准 1（技术视角）：ses_fdb72ef78ffeO6h9p4E7Wu5RP4，2026-08-21 — 「批准 M3.9 aps orm 保护区域变更（技术视角批准）」。依据：`orm-model-design.md` §主键设计方案 B 强制规则（:30/:186-:197）+ M0.1 seq-string Proof 4/4 绿 + M1.1/M1.2 同机制先例 + dry-run 副本实测精确 26 行 stdDataType-only diff + stub 翻转 = 与 md 权威源对齐。
    - 批准 2（治理视角）：ses_fdb72bf11ffeNkF0bF4vCzd777，2026-08-21 — 「批准 M3.9 aps orm 保护区域变更（治理视角批准）」。依据：五要素证据链（平台 design doc 方案 B + `domain-design-guidelines.md` §16A.4 + M0.1 seq-string Proof + M1.1 双批准先例 + M0 裁决 D3/D4/D6）；变更面 = 26 列 stdDataType、stdSqlType 不变 DDL 零变化。
- 共识达成（2026-08-21）：iteration 1 双审查者 0 BLOCKER / 0 MAJOR + 保护区域双批准 → 计划转 `active`。

## Closure Gates

> 完整仓库验证定制为域级口径（路线图规则 3 D3 修订：禁止以全量构建为中间 gate；全量构建仅存在于 M4.1）。

- [x] 范围内行为完成（26 列落源 + no-am 重生成 + 手写代码/测试修复 + A2/A3 桥接落桥与退役 + 快照重录 + grep 门控清零）
- [x] 相关文档对齐（owner doc 注记或零变更结论、路线图 M3.9 状态、登记册退役、日志）
- [x] 已运行验证：`mvn clean install -pl module-aps/erp-aps-codegen,module-aps/erp-aps-dao,module-aps/erp-aps-meta,module-aps/erp-aps-service,module-aps/erp-aps-web,module-aps/erp-aps-app,module-aps/erp-aps-api -DskipTests` 全绿 + `mvn test -pl module-aps/erp-aps-service,module-aps/erp-aps-web` 全绿 + 工具重扫零残留（aps 段 `NEEDS FIX` = 0）
- [x] 无范围内项目降级为 deferred/follow-up（web 页面测试治理排除为先于本计划的已提交决策 + M4.1 successor 登记，属偏差登记而非范围降级）
- [x] 保护区域双独立子 agent 批准记录落盘（Phase 1 前置）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（ses_fdb4a8964ffeBLobVcJy3xCyUl，见 Closure Audit Evidence）
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### `ErpApsWebPagesTest` 页面校验

- Classification: `watch-only residual`
- Why Not Blocking Closure: `@Tag("full-app")` + surefire excludedGroups 为先于本 mission 的已提交治理决策（plan 2026-07-24-0930-1），实证依赖全量 classpath（`/erp/xlib/control.xlib` 模块级缺失——M1.1 先例）
- Successor Required: `yes`（M4.1 app-erp-all `ErpAllWebPagesTest`）

### A2 main 桥接 15 处（inv 4 + mfg 11，String↔Long 临时转换）

- Classification: `watch-only residual`
- Why Not Blocking Closure: D4 登记册预先登记的中间态桥接——inv/mfg 未迁移（位次 10/14），桥接点为编译必需；退役 owner M2.2/M3.1 翻转对应域时退役条目并移除桥接点
- Successor Required: `yes`（M2.2 回收 inv 4 条；M3.1 回收 mfg 11 条）

### 平台 IoC 回归 delta（若 Phase 3 复现并落盘）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 平台 `nopSequenceGenerator` bean-init-self-wait 为已登记平台 Bug（`docs/bugs/2026-08-21-nop-sequence-generator-ioc-self-wait-*.md`），先例修复 = test-scope VFS delta
- Successor Required: `yes`（平台修复后统一移除，M4.1 复核）

## Closure

Status Note: completed（2026-08-21：四 Phase 全部执行完毕 + 独立结束审计 `passes closure audit`）

Closure Audit Evidence:

- **独立结束审计（ses_fdb4a8964ffeBLobVcJy3xCyUl，2026-08-21，fresh session 仅审计零修改）：`passes closure audit` — 0 BLOCKER / 0 MAJOR / 3 MINOR**。九项活仓核验全 PASS：① orm 26 列 stdDataType-only（scan aps 段零 NEEDS FIX/DEFERRED + git diff 52 行零非 std）② 7 模块 `-DskipTests` no-am BUILD SUCCESS ③ service 76/76 绿 + web 0 tests（治理排除证实 `@Tag("full-app")` + pom excludedGroups）④ grep 门控零未登记残留 ⑤ 210 变更文件全在合法类别（手写 view 零改动 + `_gen` diff 纯 codegen 类型变更）⑥ 登记册 bridge-test-103..106 retired / bridge-main-009..023 active ⑦ roadmap/日志/计划文本一致 ⑧ A2 桥接 ConvertHelper 落点在案 ⑨ 快照卫生零 RECORDING 残留 + json5 String id 实证。
- **3 MINOR（审计后已全部整改 + 复验）**：① 6 处桥接点缺代码内 bridge 注释（010/012/016/017/018/021）→ 已补注（011/015/023 原以合并注释覆盖），`grep bridge-main-0` 15 条目全覆盖复验；② Phase 2 表格 021/023 行映射与代码注释错位（requireWorkOrder 桥归 022）→ 已修正表格；③ Phase 2 记录「service 12 文件」实为 13（WorkCenterTimeline 联动）→ 已修正。整改后复跑：7 模块 `-DskipTests` BUILD SUCCESS + `mvn test -pl erp-aps-service,erp-aps-web` 76/76 绿 + web 0 tests BUILD SUCCESS。
- **验证状态（全绿）**：`mvn clean install -pl module-aps/erp-aps-{codegen,dao,meta,service,web,app,api} -DskipTests` 7 模块 SUCCESS（no-am）+ `mvn test -pl module-aps/erp-aps-service,module-aps/erp-aps-web` service 76/76（累计 4 轮全绿）+ `node tools/check-bigint-id-types.mjs` aps 段零残留。

Follow-up:

- （无；已确认缺陷不得出现在此处。中间态 successor 指针见 Deferred But Adjudicated 与 Phase 4 登记记录）
