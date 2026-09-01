# maintenance 域种子数据

> **资产类型**：部署期种子资产（`app-erp-all/src/main/resources/_vfs/_init-data/`），非测试资产（边界裁决见 `docs/architecture/seed-data.md` 头部）。
> **落地批次**：M1.2b（plan `docs/plans/2026-09-01-1245-3-m12b-mnt-qa-prj-seed-expansion.md`，2026-09-01）。此前 8 张 maintenance 表 seed（equipment / equipment_category / schedule / request / downtime_entry / visit / visit_task / spare_part_usage，plan `2026-07-09-0930-2`）见 `docs/architecture/seed-data.md` 历史批次段。
> **业务语义 owner docs**：`docs/design/maintenance/` 各域文档（设备集成/维护流程）；本文件只登记种子数据面，不重复业务语义。

## 种子数据范围（M1.2b 批次 7 表）

maintenance 域 15 规格实体中，8 表已由历史批次 seed；本批补齐其余 **7 规格表（18 行）**，达成 maintenance 域全量 seed 覆盖（15 / 15）。

### 7 规格表 CSV

| 实体 | CSV 文件 | 行数 | 用例指示 | 必填 FK 闭环 |
|---|---|---|---|---|
| ErpMntCalibration（校准记录） | `erp_mnt_calibration.csv` | 3 | P×2+N-TERM(CANCELLED) | EQUIPMENT_ID→ErpMntEquipment〔已seed 1/2/3〕；CALIBRATED_BY 可选→ErpMdEmployee〔跨域:md·已seed 17〕 |
| ErpMntEquipmentStatusLog（设备状态日志） | `erp_mnt_equipment_status_log.csv` | 3 | P | EQUIPMENT_ID→ErpMntEquipment〔已seed 1/3〕；FROM/TO_STATUS ∈ `erp-mnt/equipment-status`、SOURCE ∈ `erp-mnt/status-log-source`；时间戳与既有 downtime_entry 停机事实对齐（07-05 10:00–14:00 预防性维护 / 07-02 08:00 起主电机故障） |
| ErpMntMaintenanceTeam（维护团队） | `erp_mnt_maintenance_team.csv` | 2 | P | —（无必填 FK）；LEADER_ID 可选→ErpMdEmployee〔已seed 17/3〕 |
| ErpMntMaintenanceTeamMember（团队成员） | `erp_mnt_maintenance_team_member.csv` | 3 | P（2+1 按头） | TEAM_ID→ErpMntMaintenanceTeam〔本批〕、EMPLOYEE_ID→ErpMdEmployee〔跨域:md·已seed 17/3/1〕 |
| ErpMntSparePartUsageLine（备件消耗行） | `erp_mnt_spare_part_usage_line.csv` | 2 | P | SPARE_PART_USAGE_ID→ErpMntSparePartUsage〔已seed 1〕、MATERIAL_ID→ErpMdMaterial〔已seed 3/4〕、**UO_M_ID→ErpMdUoM〔已seed 2/4〕（列名 `UO_M_ID` 分叉拼写陷阱显式落实）**；行合计 50+120=170.00 与头表 TOTAL_AMOUNT 对账 |
| ErpMntTaskTemplate（保养任务模板） | `erp_mnt_task_template.csv` | 2 | P+N-DIS(IS_ACTIVE=0) | —（无必填 FK）；EQUIPMENT_CATEGORY_ID 可选→ErpMntEquipmentCategory〔已seed 1〕 |
| ErpMntTaskTemplateLine（模板任务行） | `erp_mnt_task_template_line.csv` | 3 | P（2+1 按头） | TEMPLATE_ID→ErpMntTaskTemplate〔本批〕；MATERIAL_ID 可选→ErpMdMaterial〔已seed 3/4〕 |

## FK 闭环图

```
[跨域:md·已seed] erp_md_employee(17 维护员甲/3 王五/1 张三)、erp_md_material(3 原料X/4 包装箱)、erp_md_uom(2 KG/4 BOX)
   ──CALIBRATED_BY/LEADER_ID/EMPLOYEE_ID/MATERIAL_ID/UO_M_ID──▶ 校准/团队/成员/消耗行/模板行
[已seed·mnt] erp_mnt_equipment(1/2/3)、erp_mnt_spare_part_usage(1 SPU-2026-001)、erp_mnt_equipment_category(1)
   ──EQUIPMENT_ID/SPARE_PART_USAGE_ID/EQUIPMENT_CATEGORY_ID──▶ 校准/状态日志/消耗行/任务模板
[本批] erp_mnt_maintenance_team(1..2) ──TEAM_ID──▶ team_member
[本批] erp_mnt_task_template(1..2) ──TEMPLATE_ID──▶ task_template_line
```

全部必填 FK 落在〔已seed〕∪〔本批〕集合内，零悬空、白名单零增量（`TestErpSeedDataIntegrity` 全绿背书）。

## 干扰面零漂移设计（本批核验结论）

- **状态日志行对 OEE 面显示惰性**：`EquipmentRuntimeCalculator.findLogs(equipmentId)` 是状态日志唯一读取方，消费方 = OEE 计算（`OeeCalculator`）与 RUNTIME 触发维保生成（`ScheduleDueGenerator`）——集成用例 C10/C11/C12 与 E2E 均不调用；`maintenance-oee.value.spec.ts` 断言锚定「3 台设备均无 WORKCENTER_ID → OEE 分量恒 null / computedCount=0」（equipment 表不在本批，谓词输入零变化）→ 种子状态日志行不改变任何被断言值。
- **C10 零交集**：`TestErpC10MntRequestSparePart` 备件过账按自建 usage（代码 IT-C10-SPU-001）作用域，种子行挂 seed usage id=1；状态日志写入点（`EquipmentStatusLogWriter`）为纯追加无读取。
- 快照机制 `_chgType` 增量记录，显式种子 id < 100000 不入既有快照、不耗 default 序列（C10 自建行 id 实测 100002+/100011+）。

## 与既有 8 表 seed 的衔接（语义一致性约束）

- **停机事实对齐**：状态日志 3 行时间戳与既有 `erp_mnt_downtime_entry` 两行精确对齐（设备 3：07-02 08:00 起主电机烧毁进行中；设备 1：07-05 10:00–14:00 预防性维护）。
- **消耗头行对账**：spare_part_usage_line 两行合计 170.00 = seed 头 SPU-2026-001 TOTAL_AMOUNT。
- **维度值复用既有值域**：组织 2；员工 17（维护员甲）/3/1；物料 3/4；UoM 2/4；设备 1/2/3；分类 1；全部静态日期落在 2026-07 参考期（冻结时钟纪律）。

## 用例指示编码与 negative 行语义

- **P（最小正例行）**：全部 7 表均有。
- **N-TERM（终态行）**：calibration id=3 `DOC_STATUS=CANCELLED`（校准单取消终态）。
- **N-DIS（禁用行）**：task_template id=2 `IS_ACTIVE=0`（停用模板，启用前置守卫负路径）。

## 执行期登记（pre-existing ORM quirk）

`ErpQaCalibration`（quality 域，同批登记）`targetValue`/`tolerance` 为 `stdDataType=decimal + stdSqlType=VARCHAR`（domain="measuredValue" 覆写）——见 `docs/design/quality/seed-data.md` 与 plan `2026-09-01-1245-3` Deferred But Adjudicated。mnt 域 7 表无同类例外。

## 约定对齐

- 列头 = DB 列名大写下划线（逐表以 ORM `code=` 为准，含 `UO_M_ID` 分叉拼写逐表核对）；省略审计列；ISO 日期与 `yyyy-MM-dd HH:mm:ss` 时间戳（对齐同域 downtime_entry 先例）；小写布尔；ID < 100000；字典码 ∈ `erp-mnt/*` + `erp/doc-status` + `wf/approve-status` 字典（逐值核对）。
- 新增/修改本域 seed 时同步履行 `docs/architecture/seed-data.md`「快照重录义务」与 `TestErpSeedDataIntegrity` 门禁（基线常量随批更新协议 190→218），并保持本文件「干扰面零漂移设计」条件不被破坏。
