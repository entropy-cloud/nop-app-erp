# APS 域种子数据（Seed Data）

> Owner: 本文件（aps 域「种子数据」owner doc；plan `2026-09-02-1415-1-m14b-aps-logistics-seed-expansion.md` Phase 2 落地）
> 上游规格: `docs/architecture/seed-data.md` 规格表 aps 节（7 实体逐行规格权威源）+ 对账表（计数权威源）
> 业务语义 owner docs: `docs/design/aps/`（scheduling / auto-dispatch / constraint-based-planning / alternative-routing / state-machine 等）；本文件只登记种子数据面，不重复业务语义

## 1. 范围与文件清单

M1.4b 批次（2026-09-02）为 aps 域补齐 7 个 seed CSV（此前 aps 为零 seed 域）。文件位于 `app-erp-all/src/main/resources/_vfs/_init-data/`，命名 = `<tableName>.csv`（`DataInitInitializer.loadCsvData` 按表名查找契约）。至此 aps 域 7 规格实体全量 seed 覆盖（7 / 7）。

| CSV | 行数 | 主子表 | 用例指示 | 说明 |
|---|---|---|---|---|
| erp_aps_operation_order.csv | 3 | 头/独立 | P+N-TERM | 冲压/装配 PLANNED×2（machineId 7001/7002→工单 1〔跨域:mfg·已seed〕）+ 包装 CANCELLED（终态） |
| erp_aps_schedule.csv | 2 | 头/独立 | P+N-TERM | 八月第一周 PUBLISHED + 六月归档 ARCHIVED（终态） |
| erp_aps_capacity_reservation.csv | 1 | 头/独立 | P | 冲压中心 7001 预留窗 2026-08-03 08:00~12:00（OPERATION_ORDER_ID→本批 op 1） |
| erp_aps_constraint.csv | 1 | 头/独立 | P | MAINTENANCE 维护停机窗 2026-08-10~12（machineId 7001） |
| erp_aps_op_routing.csv | 2 | 头/独立 | P | operationId 1 主选 7001（isDefault）+ 备选 7002，均 enabled |
| erp_aps_dispatch_rule.csv | 2 | 头/独立 | P | 冲压线 enableAuto=true + 装配线 enableAuto=false（手动模式） |
| erp_aps_dispatch_log.csv | 1 | 头/独立（FK→operation_order） | P | op 1 AUTO 派工 DRAFT→PLANNED（DISPATCHED_AT 静态） |

## 2. FK 闭环图

- **〔已 seed〕锚点**：`erp_md_organization`（ORG_ID=2）、`erp_mfg_work_order`（WORK_ORDER_ID=1 = WO-2026-001，弱指针无 to-one）。
- **〔本批〕链路**：
  - operation_order（独立头，machineId 7001/7002 为无 FK 弱指针）→ capacity_reservation（OPERATION_ORDER_ID→op 1）+ dispatch_log（OPERATION_ORDER_ID 必填 to-one→op 1）；
  - op_routing（OPERATION_ID→op 1 弱指针语义对齐，MACHINE_ID 7001/7002）；
  - dispatch_rule（WORKCENTER_ID 7001/7002 弱指针）。
- 全部必填 FK 落在〔已 seed〕∪〔本批〕集合内（`TestErpSeedDataIntegrity` 引用完整性断言门禁，白名单零增量）；ORM 声明 to-one 关系仅 org（=2）与 dispatch_log.operationOrder（→本批 op 1），其余 machineId/workcenterId/operationId/workOrderId 均为无 to-one 的弱指针 BIGINT 列，值取本批行 id 或〔已seed〕工单 1 保持业务可读闭环。

## 3. 用例指示编码与 negative 行语义

- **P（最小正例行）**：每表主行，字段值取规整演示值（数量 `.0000` 四位小数、耗时 `.00` 两位小数、时间戳空格分隔、静态日期 2026-06 ~ 2026-08）。
- **N-TERM（终态行）**：operation_order 3（CANCELLED）、schedule 2（ARCHIVED）——供业务动作非法迁移守卫负路径；全部静态时间戳，无滚动期间语义（冻结时钟纪律）。
- aps 规格表 7 行无 N-DIS 指示（dispatch_rule 2 号行为业务正例的手动模式承载，非禁用语义；enableAuto=false 是规则配置值而非行停用）。
- 行级用途以 REMARK 后缀（P）/（N-TERM）显式标注（沿 M1.4a 批先例；dispatch_log 表无 REMARK 列，编码承载于 NOTE 列，语义相同），与 `docs/architecture/seed-data.md`「seed 数据分层裁决」节 negative seed 语义一致。

## 4. 干扰面零漂移设计（本批核验结论，Phase 1 预分析逐点复证）

- **排程引擎读取面（`ErpApsSchedulingProcessor`）**：
  - `loadPendingOrders`（status ∈ {DRAFT, UNSCHEDULABLE} 且 earliestStartDateT 落方案窗口）：种子 op 行 status = PLANNED×2 + CANCELLED×1 均在 pending 集之外 → 永不被 C08/C21 排程装载；earliestStartDateT 静态 2026-08-01/02 亦避开 C08 fixture 窗口 2026-07-10T00:00 ~ 2026-07-20T00:00（双保险）。C08 fixture op（machineId="1"、DRAFT、earliest 2026-07-10T08:00）是窗口内唯一 pending 行，快照面零漂移。
  - `loadMaintenanceConstraints`（constraintType=MAINTENANCE 且窗口相交，机器维度无关）：种子 constraint 窗口 2026-08-10 08:00 ~ 2026-08-12 18:00 与 C08 窗口（endTime ≥ 2026-07-10 成立但 startTime ≤ 2026-07-20 不成立）不相交 → 被过滤，零装载；machineId 7001 避开 fixture 机器 1（`WC-001`）。
  - `loadEnabledRoutings`（isEnabled=true 全集装载）：种子两行 enabled=true 确会被装载，但引擎 `resolveCandidates` 仅在 `isDefault=true && machineId=op.machineId` 时视为候选（`ErpApsSchedulingEngine` 关联键实证）——种子行 machineId 7001/7002 ≠ fixture op 机器 1 → 恒零候选，C08/C21 走 legacy 单候选路径，零漂移（isEnabled 取 true 经复证裁决安全）。
  - `hasOverlappingReservation`（machineId + 区间相交）：种子预留 machineId 7001，fixture 排程 op 机器 1 → 机器维度隔离，`ERR_APS_CAPACITY_CONFLICT` 零风险。
- **publish 零读取**：`ErpApsSchedule__publish` 为纯状态翻转（C21 实证：publish 后 fixture op 保持 DRAFT，无任何排程读取）——种子 schedule 行（PUBLISHED/ARCHIVED）不参与任何引擎读取面（schedule 仅 by-id `requireSchedule` 消费，且要求 DRAFT 态才能重排；种子 P 行取 PUBLISHED 防误重排）。
- **auto-dispatch 惰性**：`ErpApsDispatchRule` 全表读仅存在于 `ErpApsAutoDispatchProcessor`，全局开关 `erp-aps.auto-dispatch-enabled` 缺省 false + `erp-aps-auto-dispatch.job.yaml` enabled `@cfg|false` 双门控（实仓核验）→ 种子 2 行规则零消费。`ErpApsWorkOrderScanJob` 同为 `enabled|false` 缺省关。
- **E2E 数值断言面**：`aps-schedule-gantt.value.spec.ts` 自包含 setup（`E2E-GANTT-*` 唯一 code 自建）+ 客户端 MACHINE_ID='910' 过滤 + findPage(limit:500) 全量行放大（种子行仅增大 allItems 无害）——种子 machineId 7001/7002 避开 '910'（gantt 过滤值）/ '100'（aps-action 套件值）/ '1'（C08/C21 fixture 值）三个串扰值；aps 4 个 action spec + `aps.smoke` 自包含/渲染型，零消费种子行。
- **集成快照零漂移**：`app-erp-all/_cases` aps 相关用例（TestErpC08MrpApsRelease / TestErpC21ApsCapacityLoad）仅经 `@var:` 自建实体断言，无 findPage/totalCount 种子计数断言（grep 实证全 `_cases` 仅 C05/C08/C19/C21 触及 ErpAps|ErpLog）；纯加性插入不入既有快照（M1.2b 实证先例）。

## 5. 约定对齐

- 列头 = DB 列名大写下划线（逐表以 ORM `code=` 为准）；省略审计列（capacity_reservation 无 delVersion 行为对齐 ORM 无逻辑删除声明）；日期 ISO（`2026-08-03`）、时间戳空格分隔（`2026-08-03 08:00:00`）；小写布尔；ID < 100000（各表独立自 1 起段，machineId/workcenterId 弱指针取 7001/7002 演示段）；字典码 ∈ `module-aps/model/app-erp-aps.orm.xml` `<dicts>`（erp-aps/ 命名空间 6 字典：operation-order-status / schedule-status / constraint-type / scheduling-mode / dispatch-type / routing-selection-reason）。
- 拓扑序由 `DataInitInitializer` 按 ORM `getEntityModelsInTopoOrder()` 自动排序，同批 dispatch_log（依赖 operation_order）无需手工排序文件名。
