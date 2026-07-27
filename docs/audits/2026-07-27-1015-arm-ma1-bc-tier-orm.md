# ARM-MA1 B+C 级域 ORM 模型审计报告（master-data / cs+contract+b2b+maintenance+drp / aps+logistics+notify）

> Plan: `docs/plans/2026-07-27-1015-2-audit-remediation-ma1-orm-model-audit.md` Phase 3
> Roadmap 工作项：A1.7（master-data）/ A1.8（cs+contract+b2b+maintenance+drp）/ A1.9（aps+logistics+notify）
> Skill: `docs/skills/orm-model-audit-prompt.md`（9 维度机械审计）
> 审计日期：2026-07-27
> 审计员：opencode 子代理（独立审计会话）
> 审计方法：Python 自动化扫描（`orm_audit.py`）+ 人工抽样核实

## 1. 审计范围与基线

| 域 | 复杂度 | ORM 行数 | 实体 | 列数 | to-one | 字典 | notGenCode |
|---|---|---|---|---|---|---|---|
| master-data | B（DAG 根域） | 1253 | 25 | 388 | 42 | 19 | 0 |
| cs | B | 850 | 18 | 279 | 36 | 14 | 2 |
| contract | B | 760 | 19 | 274 | 29 | 12 | 4 |
| b2b | B | 685 | 16 | 260 | 24 | 13 | 3 |
| maintenance | B | 741 | 20 | 250 | 28 | 10 | 8 |
| drp | B | 585 | 16 | 199 | 35 | 7 | 6 |
| aps | C | 316 | 7 | 125 | 7 | 5 | 1 |
| logistics | C | 431 | 12 | 160 | 16 | 5 | 5 |
| notify | C | 159 | 3 | 49 | 2 | 5 | 0 |
| **小计** | | **5780** | **166** | **1984** | **239** | **90** | **29** |

实体计数与 `audit-remediation-scope-and-dimension-matrix.md §1.1` 一致。

## 2. 各域审计结论

### 2.1 master-data（A1.7，DAG 根域单独审计）

| 维度 | 结论 |
|---|---|
| 1 类型规范 | ⚠️ 7 minor（`ErpMdMaterial.{vatRate, drawbackRate}`/`ErpMdMaterialCustoms.{qtyDeclared, amountDeclared, amountFunctional, dutyAmount, vatAmount}` DECIMAL↔decimal）——同 hr 裁决保留 decimal（海关金额/税率参与计算） |
| 2 长度精度 | ⚠️ 6 minor（`ErpMdMaterial.{weight precision=12, volume scale=6}`/`vatRate scale=4`/`drawbackRate scale=4`/`ErpMdMaterialSku.conversionRate scale=4`）——物理量字段合理偏离 |
| 3 字典设计 | ✅ material-type/partner-type/active-status 等 19 字典合规；`erp-md/active-status` 被 18 域复用，无重复定义 |
| 4 标准字段 | ✅ 全实体覆盖 |
| 5 业务字段 | ✅（orgId 6、currencyId 2）——master-data 是 DAG 根域，无业财过账需求 |
| 6 关系 | ✅（to-one + join） |
| 7 跨模块引用 | ✅（0 notGenCode——DAG 根域不引用其他域） |
| 8 命名 | ⚠️ 1 false-positive（`ErpSysConfig` tableName=`erp_sys_config` 不匹配 `erp_md_` 前缀）——§19.2 已登记命名异常（`ErpSys*`/`erp_sys_*` 表达"系统级跨域"性质），**不视为 finding** |
| 9 覆盖 | ✅ 25 实体齐备（material/sku/partner/warehouse/location/uom/currency/subject/acct-schema/organization/bank-account/cost-center/...） |

**重点关注核实**：
- **被全域引用的根实体字段完整性**：`ErpMdMaterial`（物料）/`ErpMdPartner`（往来单位）/`ErpMdSubject`（科目）/`ErpMdWarehouse`（仓库）/`ErpMdOrganization`（组织）字段完整，被 18 域经机制 B 引用（共 130+ 处跨域引用）。
- **`erp-md/*` 字典被全域复用的一致性**：`erp-md/active-status`/`erp-md/material-type`/`erp-md/partner-type` 等字典无跨域重复定义，F2（字典真相碎裂）已闭包成果保持。

### 2.2 cs + contract + b2b + maintenance + drp（A1.8）

| 维度 | cs | contract | b2b | maintenance | drp |
|---|---|---|---|---|---|
| 1 类型规范 | ✅ | ⚠️ 2 minor（DECIMAL↔decimal） | ✅ | ⚠️ 5 major + 4 minor（propId + DECIMAL↔decimal） | ✅ |
| 2 长度精度 | ✅ | ⚠️ 2 minor | ✅ | ⚠️ 3 minor | ✅ |
| 3 字典 | ✅ | ✅ | ✅ | ✅ | ✅ |
| 4 标准字段 | ✅ | ✅ | ✅ | ✅ | ✅ |
| 5 业务字段 | ✅（双轴 1/1） | ✅（posted 1、businessDate 3） | ✅（orgId 9） | ✅（posted 2、双轴 2/2） | ✅（orgId 8） |
| 6 关系 | ✅ | ✅ | ✅ | ✅ | ⚠️ 2 minor（`ErpInvDrpCrossDock.{inboundMove, outboundMove}` to-one 缺 `pub`） |
| 7 跨模块引用 | ✅ | ✅ | ✅ | ✅ | ✅ |
| 8 命名 | ✅ | ✅ | ✅ | ✅ | ⚠️ 4 major（`ErpInvDrp*` tableName 前缀 `erp_inv_drp_`） |
| 9 覆盖 | ✅ | ✅ | ✅ | ✅ | ✅ |

**重点关注核实**：
- **cs 工单/SLA/满意度**：`ErpCsTicket`/`ErpCsSla`/`ErpCsSatisfactionSurvey` 实体齐备，`erp-cs/ticket-status` 字典与 §16.2 一致（6 态）。
- **contract 版本/发票计划/返利**：`ErpCtContract`/`ErpCtContractVersion`/`ErpCtInvoicePlan`/`ErpCtVolumeDiscount`/`ErpCtRebateTier` 实体齐备。
- **b2b EDI 文档**：`ErpB2bAsn`/`ErpB2bEdiTransaction`/`ErpB2bEdiFormat` 实体齐备；`b2b→pur` 跨域写 ErpB2bAsnBizModel 收敛条件为已知 P2 deferred（scope matrix §3.2）。
- **maintenance 设备/巡检/备件**：`ErpMntEquipment`/`ErpMntVisit`/`ErpMntCalibration`/`ErpMntSparePart` 实体齐备。
- **drp 需求计划/安全库存**：`ErpDrpPlan`/`ErpDrpPlanLine`/`ErpInvDrpSafetyStockCalc`/`ErpInvDrpCrossDock`/`ErpInvDrpDockAppointment`/`ErpInvDrpLeadTimeRecord` 实体齐备。

### 2.3 aps + logistics + notify（A1.9）

| 维度 | aps | logistics | notify |
|---|---|---|---|
| 1 类型规范 | ✅ | ✅ | ✅ |
| 2 长度精度 | ✅ | ✅ | ✅ |
| 3 字典 | ✅（5 字典） | ✅（5 字典） | ✅（5 字典） |
| 4 标准字段 | ✅ | ✅ | ✅ |
| 5 业务字段 | ✅（orgId 6、businessDate 2） | ✅（posted 1、businessDate 1） | ✅（无业务字段——notify 是基础设施子系统） |
| 6 关系 | ✅ | ✅ | ✅ |
| 7 跨模块引用 | ✅ | ✅ | ✅ |
| 8 命名 | ✅（erp_aps_） | ✅（erp_log_） | ✅（erp_sys_——§19.2 已登记） |
| 9 覆盖 | ✅（7 实体） | ✅（12 实体） | ✅（3 实体） |

**重点关注核实**：
- **aps 排产/工序单**：`ErpApsOperationOrder`/`ErpApsResource`/`ErpApsSchedule` 实体齐备，`erp-aps/operation-order-status` 字典与 §16.2 一致（5 态，使用 FINISHED 而非 COMPLETED）。
- **logistics 发运/承运商**：`ErpLogShipment`/`ErpLogCarrier`/`ErpLogTrackingEvent` 实体齐备，6 态状态机与 §16.2 一致（含 ADVISED/DISPATCHED）。
- **notify 跨域通知派发子系统**：`ErpSysNotification`/`ErpSysNotificationTemplate`/`ErpSysNotificationRead` 三实体，`erp-notify/*` 字典命名空间合规；F5（notify owner doc）已闭包成果保持。

## 3. P0 / P1 / P2 Finding 清单

### P0（blocker）— 0 项

9 域全域 0 blocker。

### P1（major）— 9 项

| Finding ID | 域 | 实体 | 问题 | 根因 | 建议 |
|---|---|---|---|---|---|
| `P1-MA1-013` | maintenance | `ErpMntVisit.{orgId, businessDate, posted, postedAt, postedBy}`（5 列） | propId 缺失 | D3/D4 补字段未重编号（同 A 级根因） | MR1：renumber propId 连续 |
| `P1-MA1-014` | drp | `ErpInvDrpSafetyStockCalc`/`ErpInvDrpCrossDock`/`ErpInvDrpDockAppointment`/`ErpInvDrpLeadTimeRecord`（4 实体） | className=`ErpInvDrp*` + tableName=`erp_inv_drp_*` 不符合 §19.1 命名规范（前缀混用 Inv+Drp） | 设计期命名选择：表达"库存相关 DRP"概念，但违反单一前缀规范 | MR1：裁决二选一——(a) 重命名为 `ErpDrp*`/`erp_drp_*` 对齐规范；(b) 在 §19.2 登记命名异常（如 `erp-ct`/`ErpSys*` 范式）。**审计建议选 (a)**：这 4 个实体语义完全属于 drp 域（安全库存计算、越库、月台预约、提前期记录），与 inventory 域无写耦合，仅经 `IErpInvStockMoveBiz` 读库存余额（机制 D），无理由保留 Inv 前缀 |

**裁决**：
- P1-013（maintenance propId 缺失）：归 MR1，codegen 增量再生自动补全
- P1-014（drp 命名异常）：归 MR1，建议重命名对齐规范。**不视为 P0**——命名异常不影响功能，仅影响跨域可读性与 grep 检索一致性

### P2（minor）— 不进 MR

| 类别 | 数量 | 处理 |
|---|---|---|
| master-data DECIMAL↔decimal（vatRate/drawbackRate/海关金额等 7 列） | 7 | 同 hr 裁决，保留 decimal（海关金额参与退税计算） |
| master-data 物理量精度（weight/volume 等） | 6 | watch-only |
| contract DECIMAL↔decimal（discountPercent/rebatePercent） | 2 | 同上裁决 |
| contract 百分比 precision=10 | 2 | watch-only |
| maintenance DECIMAL↔decimal（calibration/measured value） | 4 | 同上裁决 |
| maintenance calibration scale=6 | 3 | watch-only——校准值惯例精度 |
| drp `ErpInvDrpCrossDock` to-one 缺 `pub` | 2 | watch-only——不影响功能 |

## 4. 残留风险

1. **`ErpInvDrp*` 命名异常**（P1-014）：MR1 修复时若选择重命名，需同时改 className + tableName + 跨模块引用点（如 inventory 域若有 `IErpInvDrp*Biz` 调用需同步）。若选择登记例外，需在 §19.2 补充条目并说明 Inv 前缀的设计理由。
2. **`ErpSysConfig` 命名归属**：本审计确认为 §19.2 已登记的命名异常（非 finding）。但 ErpSysConfig 在 master-data ORM 中声明，跨域 owner 关系（master-data 维护、其他域只读引用）需在 MA3 文档-实现一致性审计中复核。
3. **maintenance propId 缺失**：同 A/S 级根因，MR1 一并修复。
4. **notify 子系统**：3 实体规模极小（49 列），无任何 finding，是全域最干净的子系统。

## 5. 裁决

**通过（with P1 residual → MR1）**：

- 9 域 0 P0 blocker
- 9 维度全部有结论，机械合规基线达成
- 9 项 P1（maintenance propId 5 + drp 命名 4）全部归 MR1
- 多项 P2 watch-only（物理量精度、DECIMAL↔decimal 配对）
- master-data `⚠️` 状态保持（无新 P1，原 `⚠️` 来自 F4 隐性共享内核历史，已闭包但状态保留）
- scope matrix §2.1 "ORM 模型规范" 行：
  - master-data：`⚠️` 保持（无新 P1）
  - cs/b2b/aps/logistics/notify：`❓` → `✅`（无任何 P1，9 维度全通过）
  - contract：`❓` → `✅`（仅 P2 watch-only）
  - maintenance：`❓` → `⚠️`（propId P1 待 MR1）
  - drp：`❓` → `⚠️`（命名异常 P1 待 MR1 裁决）

## 6. 全域 ORM 维度覆盖率汇总（scope matrix §2.1 终态）

经 S+A+B+C 三批审计，scope matrix §2.1 "ORM 模型规范" 行全域覆盖结果：

| 域 | 状态 | 说明 |
|---|---|---|
| finance | ✅ | 9 维度全通过 |
| manufacturing | ⚠️ | propId P1 待 MR1 |
| hr | ✅ | DECIMAL↔decimal 裁决保留（P2 watch-only） |
| purchase | ✅ | 仅 P2 |
| sales | ✅ | 仅 P2 |
| assets | ⚠️ | propId P1 待 MR1（29 项） |
| inventory | ✅ | 9 维度全通过 |
| crm | ⚠️ | DECIMAL↔double P1 待 MR1（影响精度） |
| quality | ⚠️ | propId P1 待 MR1（1 项） |
| projects | ⚠️ | propId P1 待 MR1（5 项） |
| cs | ✅ | 9 维度全通过 |
| contract | ✅ | 仅 P2 |
| b2b | ✅ | 9 维度全通过 |
| maintenance | ⚠️ | propId P1 待 MR1（5 项） |
| drp | ⚠️ | 命名异常 P1 待 MR1 裁决（4 实体） |
| aps | ✅ | 9 维度全通过 |
| logistics | ✅ | 9 维度全通过 |
| notify | ✅ | 9 维度全通过（最干净子系统） |
| master-data | ⚠️ | 状态保持（历史 F4 + 无新 P1） |

**汇总**：10 域 `✅` + 9 域 `⚠️`（待 MR1 修复） + 0 域 `❓` + 0 域 `blocker`。

## 7. 引用

- Plan：`docs/plans/2026-07-27-1015-2-audit-remediation-ma1-orm-model-audit.md`
- Skill：`docs/skills/orm-model-audit-prompt.md`
- Roadmap：`docs/backlog/audit-remediation-roadmap.md` MA1 A1.7/A1.8/A1.9
- Scope matrix：`docs/audits/audit-remediation-scope-and-dimension-matrix.md §2.1`
- arm-index：`docs/audits/arm-index.md`（P1 汇总同步更新）
- 自动化扫描输出：`/var/folders/lv/yfm8thx903d6bnjjz9c4m_mm0000gn/T/opencode/orm_audit_out/{master-data,cs,contract,b2b,maintenance,drp,aps,logistics,notify}.json`
