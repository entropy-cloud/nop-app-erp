# 2026-08-15-2119-3-rc-mr1-r1-48-mfg-material-reservation-write-path RC-R1.48 — mfg 物料预留写路径（MR1 第二批 B 类预授权降级）

> Plan Status: completed
> Last Reviewed: 2026-08-15
> Mission: requirement-compliance
> Work Item: RC-R1.48（P1-RC-008 mfg 工单审核触发/释放/领料扣减物料预留写路径完全缺失）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.48 行 + `docs/audits/arm-index.md` P1-RC-008 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`（**2026-08-12 批量裁决 B 类：inventory 域已有 ErpInvReservation 表，mfg 侧跨域调用，降级为预授权自动执行**）
> Related: `docs/design/manufacturing/use-cases.md`（L1 UC-MFG-05 :90-104 + UC-MFG-08 :144-156 + UC-MFG-06 :107-121）；`docs/design/manufacturing/material-reservation.md`（owner doc，整体 Deferred 标注）；`docs/audits/2026-08-02-2042-2-rc-ma1-a1-8-mfg-f1-mrp-drp-engine.md`（A1.8 报告 P1-RC-008 新建）；`docs/audits/2026-08-06-1926-rc-ma4-a4-2-1-2-mfg-reservation-availability-runtime.md`（A4.2.1/A4.2.2 运行时安全性确认）
> Audit: required

## Current Baseline

- **finding P1-RC-008（arm-index 行，UC-MFG-05/08/06）**：L1（`use-cases.md:90-104,144-156`）逐字要求 UC-MFG-05 4 断言（①BOM 展开创建 MaterialReservation 每个子件一条 ②预留量=min(需求,可用) ③库存余额.预留量+= ④工单.reservationStatus=RESERVED(或 PARTIAL_RESERVED)）+ UC-MFG-08 3 断言（⑤CANCELLED/COMPLETED 释放未领料预留 ⑥库存余额.预留量-= ⑦MaterialReservation.状态=RELEASED）+ UC-MFG-06 ⑫⑬⑭⑯（超预留拒绝/警告 + pickedQty+= + reservedQty-= + 库存余额.预留量-=；⑮现有量-= 已实现归接受）。L3 实仓（HEAD 核查）：
  - 制造域 7 断言全未实现——`KitAvailabilityChecker.java:62-89` 只读齐套校验（Javadoc:31-33 显式「只读校验，不写预留」）+ `ErpMfgWorkOrderProcessor` 无预留创建（`rg reservation` 全文 0 命中）+ `ErpMfgMaterialReservation` 未物化 + `ErpMfgWorkOrder` 无 reservationStatus 字段 + `ErpMfgConstants` 无 erp-mfg.reservation-* config key；
  - 跨域 `ErpInvReservationBizModel.java`（`module-inventory/erp-inv-service/.../entity/`，15 行 CRUD 桩）+ `IErpInvReservationBiz.java`（`module-inventory/erp-inv-dao/.../biz/`，10 行 `extends ICrudBiz<ErpInvReservation>`）**无 purpose-built 预留写接口**；
  - **库存域 ErpInvReservation 实体已存在且结构完备**（`app-erp-inventory.orm.xml:440-536`）：头 `ErpInvReservation`（code/orgId/businessDate/sourceBillType:448/sourceBillCode:449/reservedForPartnerId/status:451 dict `erp-inv/reservation-status`/validUntil/remark + UK_CODE_ORG）+ 行 `ErpInvReservationLine`（reservationId/lineNo/materialId/skuId/warehouseId/locationId/batchNo/**reservedQuantity:497**/**consumedQuantity:498**/uomId/**sourceLineCode:500**）；`ErpInvStockBalance.reservedQuantity:379`（propId 9，defaultValue 0）+ `availableQuantity:381`（app 层计算 `available = total − reserved − locked`，`recomputeAvailable:350-355`）；
  - **库存侧既有 reservedQuantity 写入口**：`ErpInvStockMoveProcessor.applyReservation:210-226`（OUTGOING/INTERNAL 移动单 confirm 时占 + complete 时释放）+ `StockMoveBookkeeper.updateBalanceWithRetry:256-328`（versionProp 乐观锁 + UK 冲突 evict+reload+重试 `erp-inv.concurrent-deduct-max-retry`=5——**并发预留 lost-update 防护机制可复用**，A4.2.3 SP-3 关注点）；mfg 领料移动单（`ErpMfgMaterialIssueConfirmProcessor.confirm:33-72` → `MaterialIssueStockMoveBuilder.build:29-47` MOVE_TYPE_OUTGOING_ISSUE → `stockMoveBiz.generateMove:54`）经 `generateMove` 同事务 doConfirm+doComplete **net-zero apply-release 不持久化预留**（A4.2.1 精化确认）。
- **A4.2.1/A4.2.2 运行时安全性确认（2026-08-06，维持 P1 不升 P0）**：当前 Deferred 状态下并发领料无 silent split-quantity corruption（updateBalanceWithRetry 串行化）+ 齐套校验无陈旧读（实时 findAllByQuery）；**维持 P1-RC-008 P1**（需求契约维度缺口）；残留真并发 over-commitment 归 A2.17 既有追踪；**SP-3（A4.2.3 行）**= MR1 修复落地后 reservedQty/availableQuantity 实时一致性 + 跨工单并发预留 lost-update 防护——**本行修复即解锁 A4.2.3 回队**（roadmap A4.2.3 行 todo，Deps 注记「MR1 P1-RC-008 修复落地后回队」）。
- **预授权判据（2026-08-12 批量裁决 B 类）**：**inventory 域已有 ErpInvReservation 表（结构完备），mfg 侧跨域调用——不需要 mfg 新增 ORM 结构变更**，纯代码逻辑/跨域契约（IErpInvReservationBiz purpose-built 写接口 + mfg Processor 接线）即可解决 → **降级为预授权自动执行**（跨域契约项仍须协调确认但不触 ORM ask-first）；无 dict 结构变更（reservation-status dict 既有 OPEN/PARTIALLY_CONSUMED/CONSUMED/CANCELLED/EXPIRED 五态，L1 ⑦ RELEASED 语义映射按 Phase 1 裁决——见 D2）。roadmap RC-R1.48 行 `todo`，Deps（R1.0 done）已满足。**仍需独立 plan-audit**（跨域写 + 库存余额写路径行为变更的标准义务）。
- **owner doc `material-reservation.md`（299 行，整体 Deferred 标注 :9-16）**：持久化真相源 = 库存域 `ErpInvReservation*`（:11「实际落位以库存域 ErpInvReservation* 模型为准；预留量的读写跨域走 IErpInvReservationBiz 接口，不在制造域做 ORM 跨工程引用」）；`ErpMfgMaterialReservation`-style 字段（requiredQty/reservedQty/pickedQty/releasedQty）+ 6 态 reservationStatus（:42-52）为**业务语义参考非实现**；流程规格（approve→BOM 展开→建预留 :73-112 / 库存预留量 ± :114-118 / cancel 全释放+complete 未领料释放+qty 缩减按比例 :184-223 / 领料消耗 pickedQty+ reservedQty− :225-261）；config keys（:280-291，均未落地）：`erp-mfg.reservation-enabled`(true)/`reservation-on-approve`(true)/`over-pick-warning`(true)/`auto-release-on-complete`(true)。
- **跨域调用范式（可复用）**：mfg `@Inject IErpInv*Biz` 接口（dao 模块）+ `IServiceContext` 调用——`ErpMfgWorkOrderProcessor` 注入 `IErpInvStockMoveBiz stockMoveBiz`（:68-69，`generateMove` :373）；`AbstractErpMfgMaterialIssueProcessor` 注入 `IErpInvStockMoveBiz`(:47) + `IErpInvStockLedgerBiz`(:49)；I*Biz 契约风格（`IErpInvStockMoveBiz` 92 行：@BizMutation generateMove/confirm/complete + @BizQuery 追溯）。**跨模块 I*Biz 自动注入无需 beans.xml 显式注册**（IErpInvStockMoveBiz 实证）。
- **测试基线**：mfg 零预留测试（P1-MA5-006 successor）；inventory `TestErpInvStockMoveBizModel#testCancelReleasesReservation:95-115`（CONFIRMED 占 5 / cancel 释放范式）+ `TestErpInvStockMoveBookkeeping`（:165-174/:215-256 CONFIRMED 占/DONE 释放 + available=total−reserved−locked 不变量）+ `TestErpMfgWorkOrderEndToEnd`（:74 全链）/`TestErpMfgMaterialIssue`。
- **涉及文件**：`module-inventory/erp-inv-dao/.../IErpInvReservationBiz.java`（purpose-built 写接口）；`module-inventory/erp-inv-service/.../ErpInvReservationBizModel.java`（写接口实现）；`module-manufacturing/erp-mfg-service/.../processor/ErpMfgWorkOrderProcessor.java`（approve/cancel 接线）；`ErpMfgWorkOrderApproveProcessor.java`；`ErpMfgWorkOrderReportCompletionProcessor.java`（complete 释放）；`ErpMfgMaterialIssueConfirmProcessor.java`（领料消耗）；`ErpMfgConstants.java`（config keys）；测试（inventory + mfg）；owner doc `material-reservation.md`（Deferred 标注更新 + 实现注记）+ arm-index/roadmap/`docs/logs/`（回填）+ A4.2.3 行解锁注记。

## Goals

- **预留写路径运行时成立（P1-RC-008 核心，UC-MFG-05/08/06 三 UC 贯通）**：
  - **①+②+③+④ 审核触发**（UC-MFG-05）：工单 approve 时经跨域 `IErpInvReservationBiz` purpose-built 写接口创建 `ErpInvReservation`（头 sourceBillType=WORK_ORDER + sourceBillCode=wo.code + 行 per 子件，行 reservedQuantity=min(需求,可用) + sourceLineCode=工单行来源行号），库存余额 `reservedQuantity += 预留量`（复用 updateBalanceWithRetry 乐观锁）；
  - **⑤+⑥+⑦ 释放**（UC-MFG-08）：工单 CANCELLED 全释放未领料预留 + COMPLETED 释放未领料部分（reservedQuantity − consumedQuantity），库存余额 `reservedQuantity -=`，头状态按 D2 裁决映射（L1 ⑦ RELEASED 语义）；
  - **⑫⑬⑭⑯ 领料扣减**（UC-MFG-06）：领料 confirm 消耗预留（picked/consumed 追踪 + 库存余额预留量扣减），超预留按 D1 裁决（config `erp-mfg.over-pick-warning` 拒绝/警告）。
- **并发防护**：预留写（占/释放/消耗）全部经 `StockMoveBookkeeper.updateBalanceWithRetry`（versionProp 乐观锁 + 重试）——满足 A4.2.3 SP-3「跨工单并发预留 lost-update 防护」运行时义务。
- **config 门控**：`erp-mfg.reservation-enabled`（默认按 D3 裁决——owner doc 建议 true，倾向对齐既有 config-gate 范式）消费链完整（ErpMfgConstants + 读点），`over-pick-warning`/`auto-release-on-complete` 按 Phase 1 裁决范围落地。
- **测试**：① 审核创建（approve → ErpInvReservation 头+行落库 + reservedQuantity+= + 状态断言）；② 取消释放（cancel → 预留释放 + reservedQuantity-=）；③ 完工释放（complete → 未领料部分释放）；④ 领料消耗（confirm → consumedQuantity+= + 预留量-=）；⑤ 超预留边界（over-pick 拒绝/警告按裁决）；⑥ 并发 lost-update（双工单并发预留 → 乐观锁/重试无丢失）；⑦ 既有 inventory/mfg 测试零回归。
- **零回归**：既有 inventory/mfg 测试全绿 + 全仓构建 + compliance checker 零漂移或 per-site 登记（新跨域方法若引入 daoFor/import 面按既有先例登记 baseline-raise——对齐 RC-R1.29/RC-R1.33 先例）。
- **owner doc 收敛**：`material-reservation.md` Deferred 标注更新为已实现（+ 实现注记：跨域调用点 + config keys + 状态映射裁决 + 残留边界）；不修改需求契约段（use-cases L1 不动）。
- **回填**：arm-index P1-RC-008 → `done (RC-R1.48)` + roadmap 行 → `done` + A4.2.3 行解锁注记（回队触发条件满足）+ `docs/logs/` 日志条目。

## Non-Goals

- **不新增 mfg ORM 结构变更**（2026-08-12 B 类裁决：inventory 已有 ErpInvReservation 表，mfg 侧跨域调用——`ErpMfgMaterialReservation` 实体/`ErpMfgWorkOrder.reservationStatus` 字段不物化；L1 ④ 语义经 Phase 1 D2 裁决映射到既有状态载体）。
- **不改 `erp-inv/reservation-status` dict 结构**（B 类裁决不涉 dict 结构变更；L1 ⑦ RELEASED 语义按 D2 裁决映射到既有五态——RELEASED 字面态不新增，除非 D2 裁决追加（对齐 RC-R1.88「dict 加值是数据非结构变更」先例，如裁决追加则属数据变更预授权，见 D2 选项））。
- **不实现 MRP 引擎本身**（runMrp/释放/事务原子性/幂等/仿真——A2.6b pass 结论复用 = 接受，不在本 finding 范围）。
- **不实现 6 态 reservationStatus 全态机**（PARTIAL_RESERVED/PARTIAL_PICKED/PICKED 等业务语义参考态——以 ErpInvReservation 头 status 五态 + 行 consumedQuantity 承载，owner doc 语义映射见 Phase 1 裁决）。
- **不重开 A2.17 并发竞争**（真并发 check-then-act over-commitment 窗口归 A2.17 既有追踪——预留层实现后并发模型不变，不重复登记）。
- **不改领料移动单扣减主链**（⑮库存现有量-= 已实现经 stock move bookkeeper + P0-MA2-020 UK 独立防护——仅在其上叠加预留消耗追踪）。
- **不改真相源契约段落**（use-cases L1 不动；material-reservation.md Deferred 标注更新为实现状态 + 注记）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧修复：跨域契约[inventory→mfg]写接口 + mfg 接线，Q4=(a) 强制实现禁止方案 B；2026-08-12 裁决 B 类预授权自动执行）
- Owner Docs: `docs/design/manufacturing/use-cases.md`（L1 UC-MFG-05/08/06）+ `docs/design/manufacturing/material-reservation.md`（owner doc 更新）
- Skill Selection Basis: 实现面 = 跨域 I*Biz 写接口（`nop-backend-dev`：IBiz 契约 + BizModel 实现 + per-mutation Processor 范式 + 库存余额写经 updateBalanceWithRetry 复用）；mfg Processor 接线（`nop-backend-dev`：protected step 模式 + config 消费）；测试（`nop-testing`：JunitBaseTestCase 直断言 + 跨域集成测试范式对齐 TestErpMfgWorkOrderEndToEnd）。无 view.xml/xbiz/ORM 变更（跨域 I*Biz 自动注入，见基线）。

## Infrastructure And Config Prereqs

- 新 config keys：`erp-mfg.reservation-enabled`（+ 按 Phase 1 裁决的 over-pick-warning/auto-release-on-complete）——默认值语义按 D3 裁决（config-gate 范式对齐 A4.1.4/R1.4 先例）。
- 无外部服务/环境变量。
- 分域验证前置：`mvn install -DskipTests` 后 `mvn test -pl module-inventory/erp-inv-service,module-manufacturing/erp-mfg-service`。

## Execution Plan

### Phase 1 - 状态映射与 config 语义裁决（Decision）

Status: completed
Targets: `material-reservation.md`；`reservation-status.dict.yaml`；`ErpMfgConstants.java`；`ErpMfgWorkOrderProcessor.java`
Skill: `nop-backend-dev`

- Item Types: `Decision | Proof`
- Prereqs: 无（既有基线）

- [x] `Decision` **D1 超预留语义（UC-MFG-06 ⑫）**：**选项 A（倾向）** = config `erp-mfg.over-pick-warning` 默认 true → 领料确认时领料量 > 预留未消耗量（reservedQuantity − consumedQuantity）时 LOG.warn 放行（对齐既有 warn 范式，不阻断领料主链）；**选项 B（否决）** = 默认拒绝（抛错阻断——改变既有领料行为面，无 config 门控时既有工单（无预留）会被误拦）；**选项 C（否决）** = 无预留工单豁免 + 有预留超量拒绝（两态混合——实现复杂 + 边界不清）。**理由**：owner doc `over-pick-warning` 语义即警告非阻断；无预留工单（既有数据）必须豁免零回归。记录替代方案与残留风险。
      - Skill: `nop-backend-dev`
- [x] `Decision` **D2 状态语义映射（L1 ④⑦ + dict 五态）**：**选项 A（倾向）** = 头 status 五态承载（RESERVED 语义 = 头创建后 status=OPEN[生效中] + 工单侧以既有 docStatus 状态机承载 RESERVED/PARTIAL 语义——工单齐套态 `STOCK_RESERVED`/`STOCK_PARTIAL` 已存在，`checkAvailability:117-124` 已设置；RELEASED 语义 = 释放后行 consumedQuantity=reservedQuantity 且头 status 按 D2 细裁[CANCELLED（取消释放）vs CONSUMED（完工领完）vs PARTIALLY_CONSUMED 过渡]）；**选项 B（否决）** = dict 追加 RELEASED 值（RC-R1.88 先例「dict 加值是数据非结构变更」虽预授权，但 L1 字面 RELEASED 与 ERP 语义 CONSUMED/CANCELLED 二值表达重复——头状态表达"预留单生命周期"，释放后的终态语义由消费方上下文决定）。**理由**：dict 五态已覆盖预留单生命周期；工单侧 RESERVED/PARTIAL 语义经 docStatus（STOCK_RESERVED/STOCK_PARTIAL）承载零新增结构；L1 ⑦「MaterialReservation.状态=RELEASED」以释放动作 + 行消耗追踪达成语义等价（owner doc 注记声明映射）。记录替代方案与残留风险（字面 RELEASED 态缺失——owner doc 声明语义等价映射）。
      - Skill: `nop-backend-dev`
- [x] `Decision` **D3 config 默认值 + 键集**：**选项 A（倾向）** = `erp-mfg.reservation-enabled` 默认 true（owner doc :284 建议 true；预留写路径启用后既有工单流程行为变化仅限新增预留记录 + reservedQuantity 占用——领料/完工主链不变，config-gate 仍可关闭回退）vs **选项 B（否决）** = 默认 false（零行为变化但功能默认不可用，对齐 budget config-gate 范式——但本行是需求契约强制实现，默认关闭则修复义务未达）。**理由**：Q4 强制实现 + A4.2.3 回队需要默认活跃；`auto-release-on-complete` 随 enabled 联动（enabled 关闭时全链跳过）。**键集合并裁决（MINOR-3 修复）**：owner doc :284-290 四键中 `reservation-on-approve` **并入 `reservation-enabled`**（本行唯一激活语义 = 审核时建预留，无「enabled 但 approve 不建」的组合需求）——D3 记录合并决策 + Phase 4 owner doc config 表映射更新，避免死配置键（对齐 RC-R1.41/R1.42「dead config 转活跃」教训）。记录残留风险（config 默认 true 的既有工单流程影响——测试证明主链零回归）。
      - Skill: `nop-backend-dev`
- [x] `Proof` **接线点全集确认**：`ErpMfgWorkOrderProcessor.doApprove:240-247`/`cancel:133-139`/`ErpMfgWorkOrderReportCompletionProcessor:31-104`/`ErpMfgMaterialIssueConfirmProcessor.confirm:33-72` 四接线点现状（代码实读 + 行号确认）；`ErpMfgConstants` config key 声明位（:73-74 先例 CONFIG_ALLOW_PARTIAL_KIT_START/INSPECTION_GATE_ENABLED）；inventory `updateBalanceWithRetry` 可复用性确认（public 方法 + 注册 bean `app-service.beans.xml:12-13`——ErpInvReservationBizModel 可直接注入复用，见 Phase 2 实现裁决）；**补充接线点边界**：`ErpMfgWorkOrderProcessor.doReverseApprove:254-259`（反审核后 docStatus 保持 NOT_STARTED 且预留保留——L1 未列反审核释放语义，按「cancel 兜底 + L1 一致性」裁决为保留预留不释放，Phase 1 记录）；`ErpMfgWorkOrderCloseProcessor`（STOPPED/IN_PROCESS→CLOSED 可达路径不触发 cancel/complete 释放——见 Deferred But Adjudicated 登记）。
      - Skill: `nop-backend-dev`

### Phase 1 裁决记录（执行落盘）

- **D1 超预留语义（UC-MFG-06 ⑫）— 选项 A 落地**：`erp-mfg.over-pick-warning` 默认 true。领料确认时经 `IErpInvReservationBiz.consumeReservation` 消耗预留，inventory 侧对超出未消耗量部分按 min 语义封顶消费（不产生负预留）；mfg 侧 `ErpMfgMaterialIssueConfirmProcessor` 在消费前直读预留行计算「预留未消耗量 vs 领料量」，超出时 LOG.warn 放行（不阻断领料主链）。备选 B（默认拒绝）否决——改变既有领料行为面，无预留工单（既有数据）会被误拦；备选 C（无预留豁免 + 有预留拒绝）否决——两态混合实现复杂 + 边界不清。残留风险：超预留放行后预留追踪与实际领料存在差额（差额部分无预留覆盖，归运营可见——领料移动单主链扣减仍守恒）。
- **D2 状态语义映射（L1 ④⑦ + dict 五态）— 选项 A 落地**：头 status 五态承载预留单生命周期——创建后 `OPEN`（生效中）；领料消耗后按行消耗度推进 `PARTIALLY_CONSUMED`/`CONSUMED`；释放路径按释放语义映射终态：取消释放 → `CANCELLED`，完工释放（剩余>0）→ `PARTIALLY_CONSUMED`，完工时已全领 → `CONSUMED`。工单侧 RESERVED/PARTIAL 语义经既有 docStatus（`STOCK_RESERVED`/`STOCK_PARTIAL`，`checkAvailability` 已设置）承载，零新增结构。L1 ⑦「MaterialReservation.状态=RELEASED」以释放动作 + 行 `consumedQuantity`/`reservedQuantity` 追踪达成语义等价。备选 B（dict 追加 RELEASED）否决——与 CONSUMED/CANCELLED 二值表达重复。残留风险：字面 RELEASED 态缺失——owner doc 声明语义等价映射。
- **D3 config 默认值 + 键集 — 选项 A 落地**：`erp-mfg.reservation-enabled` 默认 **true**（owner doc 建议 true；Q4 强制实现 + A4.2.3 回队需要默认活跃）；`erp-mfg.over-pick-warning` 默认 true（D1）；`erp-mfg.auto-release-on-complete` 默认 true（随 enabled 联动，enabled=false 时全链跳过）。**键集合并（MINOR-3 修复）**：owner doc 四键中 `reservation-on-approve` **并入 `reservation-enabled`**（本行唯一激活语义 = 审核时建预留），不再单独声明。残留风险：config 默认 true 对既有工单流程的影响——由测试证明无预留数据（无 BOM/无仓库行）路径零回归（approve 跳过预留创建不阻断）。
- **Proof 接线点全集（实仓核实）**：
  - `ErpMfgWorkOrderProcessor.doApprove:240-247` ✓（approve 挂接点 = doApprove 内增 protected step `createReservations`，经 `ErpMfgWorkOrderApproveProcessor.approve:30-37` 链自动生效）；
  - `ErpMfgWorkOrderProcessor.cancel:133-139` ✓（cancel 挂接点 = 状态翻转后增 `releaseReservations`）；
  - `ErpMfgWorkOrderReportCompletionProcessor.reportCompletion:31-104` ✓（complete 挂接点 = willFinish 终态判定后 :76-80 增 `releaseRemainingReservations`）；
  - `ErpMfgMaterialIssueConfirmProcessor.confirm:33-72` ✓（issue 挂接点 = generateMove :54 + flush :56 后增 `consumeReservations`）；
  - `ErpMfgConstants` config 声明位：:73-74 既有 `CONFIG_ALLOW_PARTIAL_KIT_START`/`CONFIG_INSPECTION_GATE_ENABLED` 先例 ✓；
  - `updateBalanceWithRetry` 可复用性：`StockMoveBookkeeper.updateBalanceWithRetry:256-328` public ✓，bean 注册于 `app-service.beans.xml:12-13`（`app.erp.inv.service.stock.StockMoveBookkeeper`）✓——ErpInvReservationBizModel 直接注入复用，封装裁决见 D4；
  - 边界：`doReverseApprove:254-259` 反审核后 docStatus 回 NOT_STARTED 且**预留保留不释放**（L1 未列反审核释放语义，cancel 兜底 + L1 一致性）✓ 记录；`ErpMfgWorkOrderCloseProcessor`（STOPPED/IN_PROCESS→CLOSED）不触发 cancel/complete 释放——已登记 Deferred But Adjudicated（watch-only residual + successor 触发条件）✓。
  - 补充核实：`ErpMfgWorkOrder` 头无 sourceWarehouseId 字段（ORM :570-665），仓库回退链 = `ErpMfgWorkOrderLine.sourceWarehouseId:682` → null 则跳过该行预留 LOG.warn（MINOR-8）；`KitAvailabilityChecker.aggregateRequirements:91` 与 `resolveBomId:133-143` 均为 private——Phase 3 将二者改 public 复用（MINOR-7）；BOM 无子件爆炸空结果 → 零预留静默返回。

Exit Criteria:

- [x] D1-D3 裁决记录落盘计划（选择 + 备选 + 理由 + 残留风险），四接线点 + config 声明位全集证据产出
- [x] updateBalanceWithRetry 复用路径确认（封装裁决）

### Phase 2 - inventory 跨域写接口（IErpInvReservationBiz purpose-built 方法）

Status: completed
Targets: `IErpInvReservationBiz.java`；`ErpInvReservationBizModel.java`（或新 per-mutation Processor，按 D4 裁决）；`ErpInvErrors.java`（按需）
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: Phase 1 完成

- [x] `Decision` **D4 实现形态（执行落盘：选项 A 落地）**：`ErpInvReservationBizModel` 直接实现三写方法（对齐 `ErpInvStockMoveBizModel` 委托范式），注入 `StockMoveBookkeeper`（bean 已注册 `app-service.beans.xml:12-13`，`updateBalanceWithRetry:256-328` public 可复用）+ `IDaoProvider` + `IOrmTemplate`；余额查找在 BizModel 内镜像 `StockMoveBookkeeper.findBalance` 查询语义（同口径避免与移动单路径余额行错位），无独立事务边界需求 → 不升 per-mutation Processor。理由：跨域调用方是 mfg 单侧非多单据复用；执行期未发现需独立事务边界的场景。
      - Skill: `nop-backend-dev`
- [x] `Add` `IErpInvReservationBiz` purpose-built 写方法契约（对齐 IErpInvStockMoveBiz 注解风格）：`createReservation`（@BizMutation：request=ReservationCreateRequest[orgId/businessDate/sourceBillType/sourceBillCode/reservedForPartnerId/remark/lines: ReservationLineRequest{materialId/skuId/warehouseId/locationId/batchNo/requestedQuantity/uomId/sourceLineCode}] → 建头(status=OPEN)+行(reservedQuantity=min(requested, available) 经余额读) + 余额 reservedQuantity+= + 返回含实际预留量；同源单幂等返回既有头）、`releaseReservation`（@BizMutation：sourceBillType+sourceBillCode+reason[CANCELLED/COMPLETED]，未消耗部分全释放 + 余额 -= + 头状态按 D2 映射）、`consumeReservation`（@BizMutation：request=ReservationConsumeRequest[sourceBillType/sourceBillCode/lines: ReservationConsumeLine{materialId/warehouseId/locationId/batchNo/quantity}]，consumedQuantity+= + 余额 -=，超量按 min 封顶不产生负预留）；**no-op 语义（MINOR-4 修复）**：release/consume 查无预留记录 → 静默返回 null（零异常零写入），javadoc 显式声明。新增 4 个 DTO（ReservationCreateRequest/ReservationLineRequest/ReservationConsumeRequest/ReservationConsumeLine，`erp-inv-dao/.../biz/` 对齐 StockMoveRequest 先例）。
      - Skill: `nop-backend-dev`
- [x] `Add` `ErpInvReservationBizModel` 实现：头/行创建（code = `RSV-` + generateUUID，对齐 `newMoveCode:391-393` 范式）+ 余额读写经 `StockMoveBookkeeper.updateBalanceWithRetry`（乐观锁 + UK 冲突重试，A4.2.3 SP-3 义务）+ `recomputeAvailable` 派生 availableQuantity；`findOrNewBalance` 镜像 upsertBalance 语义（查询前 flushSession + 缺失建 TRANSIENT 候选交 INSERT 路径重试）；事务边界 @BizMutation 自动包装。跨包可见性未受限（bookkeeper 方法均 public），无需镜像重试逻辑。
      - Skill: `nop-backend-dev`
- [x] `Proof` 编译 + 单元验证：`mvn test -pl module-inventory/erp-inv-service` —— 新增 `TestErpInvReservationWriteApi`（10 用例：创建占用/min 语义/幂等/取消释放/完工部分释放/完工全领/消耗追踪/超预留封顶/no-op/并发 lost-update）全绿 + 既有 inventory 测试零回归。
      - Skill: `nop-testing`

Exit Criteria:

- [x] IErpInvReservationBiz 三写方法契约 + 实现落地（grep 证据 + 编译通过），inventory 定向测试绿（创建/释放/消耗 + 余额断言 + 乐观锁冲突重试）
- [x] D4 裁决落盘记录

### Phase 3 - mfg 接线（approve/cancel/complete/issue 四挂钩）

Status: completed
Targets: `ErpMfgWorkOrderProcessor.java`；`ErpMfgWorkOrderApproveProcessor.java`；`ErpMfgWorkOrderReportCompletionProcessor.java`；`ErpMfgMaterialIssueConfirmProcessor.java`；`ErpMfgConstants.java`
Skill: `nop-backend-dev`

- Item Types: `Add | Fix`
- Prereqs: Phase 2 完成（写接口可用）

- [x] `Add` config keys：`ErpMfgConstants` 增 `CONFIG_RESERVATION_ENABLED`（`erp-mfg.reservation-enabled` 默认 true，D3）+ `CONFIG_OVER_PICK_WARNING`（`erp-mfg.over-pick-warning` 默认 true，D1）+ `CONFIG_AUTO_RELEASE_ON_COMPLETE`（`erp-mfg.auto-release-on-complete` 默认 true，D3 联动语义）+ `SOURCE_BILL_TYPE_WORK_ORDER`（预留 sourceBillType 码值）+ 读点（`ErpMfgWorkOrderProcessor.isReservationEnabled/isOverPickWarningEnabled/isAutoReleaseOnComplete`，readBoolConfig 范式 :470-480）。
      - Skill: `nop-backend-dev`
- [x] `Fix` approve 接线（UC-MFG-05 ①-④）：`ErpMfgWorkOrderProcessor.doApprove` 增 protected step `createReservations(wo, context)`（经 `ErpMfgWorkOrderApproveProcessor.approve` 链自动生效）——config-gated（reservation-enabled）+ 注入 `IErpInvReservationBiz` + BOM 展开子件需求（**MINOR-7 落地**：`KitAvailabilityChecker.aggregateRequirements`/`resolveBomId` 改 public + 新增 public `explodeRequirements` 复用）`+ 行维度仓库取 `ErpMfgWorkOrderLine.sourceWarehouseId:682`（**MINOR-8 落地**：WO 头无 sourceWarehouseId 字段（ORM :570-665 核实），回退链 = 跳过该行预留 LOG.warn，不阻断 approve）+ 调 `createReservation`（min(需求,可用) 在库存侧）→ 工单齐套态 docStatus 维持（D2 语义）；**无 BOM 场景（MINOR-5 落地）**：`resolveBomId` 抛 `ERR_DEFAULT_BOM_NOT_FOUND` → 跳过预留创建 LOG.warn 不阻断 approve；无子件/无仓库行 → 零预留静默/告警跳过。
      - Skill: `nop-backend-dev`
- [x] `Fix` cancel 接线（UC-MFG-08 ⑤⑥⑦）：`ErpMfgWorkOrderProcessor.cancel:133-139` 状态翻转后增 `releaseReservations(wo, context)`——config-gated + 调 `releaseReservation(reason=CANCELLED)`（未领料全释放 + 余额-= + 头状态 → CANCELLED）。
      - Skill: `nop-backend-dev`
- [x] `Fix` complete 接线（UC-MFG-08 ⑤⑥⑦）：`ErpMfgWorkOrderReportCompletionProcessor` 终态判定（willFinish）后增 `facade.releaseRemainingReservations(wo, context)`——config-gated（auto-release-on-complete + enabled 联动）+ 调 `releaseReservation(reason=COMPLETED)`（未领料部分释放 + 余额-= + 头状态按 D2 映射）。查无预留 no-op 零写入。
      - Skill: `nop-backend-dev`
- [x] `Fix` issue 领料消耗接线（UC-MFG-06 ⑬⑭⑯）：`ErpMfgMaterialIssueConfirmProcessor.confirm` generateMove+flush 后增 `consumeReservations(issue, lines, context)`——config-gated（enabled）+ 领料量行映射（issuedQuantity?:requiredQuantity）+ 超预留按 D1 裁决 `warnIfOverPick`（config over-pick-warning 默认 true LOG.warn 放行）+ 调 `consumeReservation`（consumedQuantity+= + 余额-= + 超量 min 封顶）；查无预留 no-op；**领料移动单主链（generateMove）零改动**（git diff 核实）。
      - Skill: `nop-backend-dev`
- [x] `Proof` 分域编译 + 集成测试前置：`mvn test -pl module-manufacturing/erp-mfg-service` 既有 260 tests 全绿（config 默认 true 下既有工单流程测试零回归——无预留数据旧路径兼容性证明，含 TestErpMfgWorkOrderEndToEnd/TestErpMfgCostFlowEndToEnd/TestErpMfgCompletionPosting 等 approve/cancel/complete 全链）。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 四接线点落地（grep 证据：approve/cancel/complete/issue 各含 create/release/consume 调用）+ config 三键声明/读点 + 分域既有测试零回归
- [x] 领料移动单主链零改动（git diff 检查）

### Phase 4 - 测试 + 零回归 + 文档回填

Status: completed
Targets: 新增测试类（inventory + mfg）；`material-reservation.md`；arm-index/roadmap/`docs/logs/`；A4.2.3 行解锁注记
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 3 完成

- [x] `Add` inventory 侧测试（Phase 2 定向测试完善）：`TestErpInvReservationWriteApi`（`module-inventory/erp-inv-service/src/test/.../TestErpInvReservationWriteApi.java`）——10 组全绿：创建（头+行+reservedQuantity+= + min 语义）/ 幂等 / 取消释放（CANCELLED + 余额回退）/ 完工部分释放（PARTIALLY_CONSUMED + 行归位）/ 完工全领（CONSUMED）/ 消耗追踪（consumedQuantity+= + 余额-= + 行 reservedQuantity 固定语义）/ 超预留 min 封顶 / no-op（release/consume 查无预留零写入）/ 多线程并发 lost-update（双写预留 4+4=8 无丢失 + available=2，updateBalanceWithRetry 重试实证）。
      - Skill: `nop-testing`
- [x] `Add` mfg 侧集成测试：`TestErpMfgReservationLifecycle`（`module-manufacturing/erp-mfg-service/src/test/.../TestErpMfgReservationLifecycle.java`，对齐 `TestErpMfgWorkOrderEndToEnd` 范式）——9 组全绿：① 审核创建（approve → ErpInvReservation 落库 + 余额占用 + min 语义 + 行仓库/sourceLineCode）；② 取消释放（cancel → 释放 + 余额回退）；③ 完工释放（complete → 未领料部分释放 + 行 reserved=consumed 归位）；④ 领料消耗（confirm → 消耗追踪 + 余额扣减）；⑤ 超预留 warn 路径（confirm 放行 + min 封顶消耗 + 领料主链扣减不受影响）；⑥ config 关闭全链跳过（reservation-enabled=false 既有行为零变化）；**⑦ 头状态断言（MINOR-6 落地）**：cancel→CANCELLED / 领料领完→CONSUMED / 部分领料→PARTIALLY_CONSUMED（L1 ⑦ RELEASED 语义的载体证明）；**⑧ no-op 语义断言**：无预留工单（旧数据，无仓库行）cancel/confirm/complete 不抛异常零写入；**⑨ 无 BOM 工单 approve 不阻断**（跳过预留 LOG.warn）。
      - Skill: `nop-testing`
- [x] `Add` owner doc 更新：`material-reservation.md` Deferred 标注更新为已实现（跨域调用点 + config keys[含 reservation-on-approve 并入 reservation-enabled 的 config 表映射，D3 裁决] + D1-D4 裁决映射 + 残留边界声明[RELEASED 语义等价映射/6 态业务参考态/A2.17 并发归口/close 路径保留预留]）。
      - Skill: `nop-backend-dev`
- [x] `Proof` 零回归验证：`mvn test -pl module-inventory/erp-inv-service,module-manufacturing/erp-mfg-service` 全绿（218 + 269）+ `mvn clean install -DskipTests` 全量构建 BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh`（新增 daoFor 面 6 处 → **baseline-raise per-site 登记**落 `docs/audits/compliance-baseline.md`（R2b 230→233 / R2c 1399→1405 / R2d 34→35，per-site 证据：ErpInvReservationBizModel 3 处 + ErpMfgWorkOrderProcessor:604 uom 回退[同文件 :358 先例] + ErpMfgMaterialIssueConfirmProcessor 2 处预留只读聚合，对齐 RC-R1.29/RC-R1.33 先例））+ 回填（arm-index P1-RC-008 → done (RC-R1.48) + roadmap RC-R1.48 行 done ✅ + **roadmap RC-R1.48 行旧「越界项…双独立子 agent 批准 checkbox」字样按 2026-08-12 B 类裁决改写消除歧义** + A4.2.3 行解锁注记[回队触发条件满足] + `docs/logs/2026/08-16.md` 日志条目）。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 新测试全绿（①-⑨）+ 既有 inventory/mfg 测试零回归 + compliance checker 通过（per-site 登记 baseline-raise）
- [x] owner doc 更新 + 四处回填（arm-index/roadmap/A4.2.3 解锁/log）+ 无范围内项目降级

## Draft Review Record

- Independent draft review iteration 1: `acceptable` (`ses_ffa618947ffeaXC0w2LJTa846R`) — 0 MAJOR，9 MINOR 全部已修正并入：① close 路径预留保留 → Deferred But Adjudicated 登记（watch-only + successor 触发条件）；② reverseApprove 路径 → Phase 1 Proof 补充边界裁决注记（保留预留不释放，cancel 兜底，L1 一致）；③ `reservation-on-approve` 并入 `reservation-enabled` → D3 键集合并裁决 + Phase 4 owner doc config 表映射；④ 写接口 no-op 语义（查无预留静默跳过）→ Phase 2 契约显式声明；⑤ 无 BOM/无子件 approve 跳过预留创建不阻断 → Phase 3 Fix 项；⑥ D2 状态映射测试缺口 → Phase 4 新增头状态终态断言（⑦ CANCELLED/CONSUMED/PARTIALLY_CONSUMED）；⑦ `aggregateRequirements` private 可见性 → Phase 3 注明抽 public helper 或等价；⑧ `sourceWarehouseId` null 回退 → Phase 3 定义回退链 + 跳过行 LOG.warn；⑨ roadmap RC-R1.48 行旧字样 → Phase 4 回填改写承诺。全部 baseline 声明实仓核实 PASS（ORM 实体/行字段/五态 dict/桩接口/接线点行号/config 先例/跨域注入机制/updateBalanceWithRetry public 可注入）。共识达成，计划可转 active。
- Independent draft review iteration 2: `acceptable` (`ses_ffa56a4d0fferPtUsmcCMTDpPc`) — 9 MINOR 落位逐项复核确认（close 路径 Deferred 条目 + successor 触发条件、reverseApprove 边界、D3 键集合并裁决与 Phase 3 Add 项 3 键无矛盾、no-op 语义、无 BOM 不阻断、测试⑦ 头状态断言、aggregateRequirements private、sourceWarehouseId 回退链、roadmap 行回填承诺）；反松弛扫描 scope 项零禁词；Draft Review Record/Plan Status 一致。非阻塞观察 O1（Phase 4 exit criteria 测试枚举 ①-⑥ 过期）已就地修正为 ①-⑨。共识达成，计划可转 active。

## Closure Gates

- [x] 范围内行为完成（P1-RC-008 预留写路径：inventory 写接口 + mfg 四接线 + config + 测试）
- [x] 相关文档对齐（material-reservation.md 更新 + arm-index P1-RC-008 → done (RC-R1.48) + roadmap 行 done + A4.2.3 解锁注记）
- [x] 已运行验证（`mvn clean install -DskipTests` + `mvn test -pl module-inventory/erp-inv-service,module-manufacturing/erp-mfg-service` + `bash docs/audits/nop-compliance-checker.sh`）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（审计证据见 Closure 节，独立子代理执行）
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 工单 close 路径（STOPPED/IN_PROCESS→CLOSED）预留保留

- Classification: `watch-only residual`
- Why Not Blocking Closure: `ErpMfgWorkOrderCloseProcessor`（STOPPED/IN_PROCESS→CLOSED）可达路径不触发 cancel/complete 释放——active 预留 + OPEN 头可残留；L1 ⑤ 字面仅列 CANCELLED/COMPLETED 释放触发，close 路径字面合规不受影响（Phase 1 Proof 已登记）；残留预留经余额 reservedQuantity 占用可见（可观测非静默），后续运营可经 cancel/释放接口清理；若 close 亦释放则改变 close 语义需 owner doc 裁定
- Successor Required: `yes`（触发条件：运营反馈 close 后预留长期残留影响可用量时，经 owner doc 裁定 close 释放语义 + 接线）

### 字面 RELEASED 状态（dict 追加）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: D2 裁决以释放动作 + 行 consumedQuantity 追踪达成 L1 ⑦ RELEASED 语义等价（头 status 五态承载生命周期）；dict 追加 RELEASED 值会产生 CONSUMED/RELEASED 语义重复
- Successor Required: `no`

### 6 态 reservationStatus 业务参考态（PARTIAL_RESERVED/PARTIAL_PICKED/PICKED）

- Classification: `watch-only residual`
- Why Not Blocking Closure: owner doc :42-52 业务语义参考态，以 ErpInvReservation 头 status + 行 consumedQuantity 承载（映射注记）；全态机物化需 mfg 侧状态字段（ORM 变更超出 B 类授权）
- Successor Required: `yes`（触发条件：运营需要工单级 PARTIAL_PICKED/PICKED 粒度可视化时，按 ORM ask-first 流程立项）

### 真并发 check-then-act over-commitment 窗口

- Classification: `watch-only residual`
- Why Not Blocking Closure: A2.17 既有追踪（O2C 审计登记）；预留层实现后底层扣减并发模型不变（A4.2.1 确认），预留写经 updateBalanceWithRetry 乐观锁防护 lost-update；read-time 校验窗口归 A2.17
- Successor Required: `no`

### 位置/去向查询（inventory 域）

- Classification: `watch-only residual`
- Why Not Blocking Closure: A4.2.11 已登记 watch-only——位置/去向查询方法集零暴露，与预留写路径不同控制点
- Successor Required: `no`

## Closure

Status Note: 全部 4 Phase 完成（P1-RC-008 预留写路径落地：inventory 跨域写接口 create/release/consume + mfg 四接线 + config 3 键 + 测试 19 组全绿 + 文档回填），独立结束审计通过（零阻塞项）。验证全绿：`mvn test -pl module-inventory/erp-inv-service,module-manufacturing/erp-mfg-service`（218 + 269）+ `mvn clean install -DskipTests` BUILD SUCCESS + compliance checker actual ≤ baseline（baseline-raise R2b 233/R2c 1405/R2d 35 per-site 登记）。A4.2.3 解锁回队注记已落 roadmap。无范围内项目降级（Deferred But Adjudicated 仅含 5 项预批准 watch-only 残留）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（新会话，无执行者上下文）`ses_ff8fc15c9ffeowgwXIp473MKZb` — VERDICT: **pass**
- Evidence: 7 项检查清单全 PASS（Phase 一致性/范围内行为/grep 证据/测试 19 组/三验证命令实测全绿/文档回填四处/无降级/反松弛扫描零禁词）；非阻塞观察 2 项（工作区未提交——仓库惯例；closure gate 勾选后由本审计实质满足——本节即证据落盘）

Follow-up:

- 无（A4.2.3 回队验证义务已注记 roadmap 行；close 路径预留释放 / 6 态 reservationStatus 物化的 successor 触发条件已登记 Deferred But Adjudicated）
