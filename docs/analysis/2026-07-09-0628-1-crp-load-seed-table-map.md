# 制造域工作中心配置链 + crp_load 种子 — 表清单、列映射、加载拓扑序、范围裁决与期望值派生

> Owner: `docs/plans/2026-07-09-0628-1-manufacturing-crp-load-seed-value-assertion.md` Phase 1 Exit Criteria
> 权威源: `module-manufacturing/model/app-erp-manufacturing.orm.xml`（workcenter :429-450 / workcenter_calendar :455-485 / workcenter_capacity :490-522 / crp_load :527-557，逐列核实，非采信旧记忆）
> 报表读源: `module-manufacturing/erp-mfg-service/.../crp/CrpLoadCalculator.java#getLoadReport` :137-183 + `availableHours`/`shiftHours`/`patternMatches` :484-530
> 报表路由: `module-manufacturing/erp-mfg-service/.../report/ErpMfgReportBizModel.java#buildCrpLoadDataset` :237-260 + `deriveCrpWindow` :334-349
> 上游主数据参照: `docs/analysis/2026-07-08-1234-1-seed-data-table-column-map.md`（org=2 / material 1-4 已 seed）
> 前序种子范式: `docs/analysis/2026-07-09-0930-1-manufacturing-seed-table-map.md`（制造域 work_order/cost_variance/forecast「直 seed」范式，本计划镜像 + 解除其 crp_load Deferred）
> Strategy C 完整参照镜像源: `docs/analysis/2026-07-09-1145-2-quality-spc-seed-table-map.md`

## 0. 约定（与 1234-1 / 1445-1 / 2210-1 / 0930-1 / 0930-2 / 1045-1 / 1145-2 一致）

- CSV 列名 = 实体 column `code`（UPPER_SNAKE_CASE 数据库列名）。
- `ID` 列虽 `tagSet="seq-default"`，但跨表 FK 引用需固定 ID，故 CSV 显式提供 `ID`。
- 框架自动填充字段（`CREATED_BY`/`CREATE_TIME`/`UPDATED_BY`/`UPDATE_TIME`/`DEL_VERSION`/`VERSION`）由 ORM 拦截器自动填，CSV 不含。
- 多租户 `TENANT_ID` 由框架兜底（前序 7 批经验性确认 seed 无须提供）。
- 布尔列值用小写字符串 `true`/`false`（与 1445-1/2210-1/0930-1/0930-2 `POSTED`/`IS_EXTERNAL`/`IS_ACTIVE` 列一致）。
- 日期列值 `YYYY-MM-DD`（effectiveFrom/effectiveTo/loadDate 均为 DATE 列）。
- START_TIME/END_TIME 为 VARCHAR(8)，存 `HH:mm` 形式（`shiftHours` 经 `LocalTime.parse` 解析，ISO_LOCAL_TIME 接受 `HH:mm`）。
- 框架审计列（DEL_VERSION/VERSION/CREATED_BY 等）全部省略。

## 1. 加载拓扑序（DataInitInitializer 按 ORM `getEntityModelsInTopoOrder()` 自动排序）

本批 4 张新表，仅引用 1234-1 已 seed 主数据 + 0930-1 已 seed 的 work_order + 本批先 seed 的上游域表：

```
[1234-1 主数据(已 seed)] md_organization(2) / md_material(1)
[0930-1 制造域(已 seed)] erp_mfg_work_order(1 = WO-2026-001)
  → [工作中心配置头] erp_mfg_workcenter                                       （无 mandatory FK，独立）
      → [工作中心配置行]
        erp_mfg_workcenter_calendar                                            （workcenterId mandatory FK→workcenter）
        erp_mfg_workcenter_capacity                                            （workcenterId mandatory FK→workcenter + materialId FK→md_material=1）
      → [CRP 负荷快照行]
        erp_mfg_crp_load                                                       （workcenterId mandatory FK→workcenter + workOrderId 弱指针→work_order=1）
```

> `workcenter` 必须先于 calendar/capacity/crp_load：三者 workcenterId 均为 mandatory FK→workcenter（逻辑 to-one）。
> `crp_load.workOrderId` 为弱指针（逻辑 to-one refEntityName=ErpMfgWorkOrder），引用 0930-1 WO-1 丰富参照。
> **关键**：crp-load 报表非空需 crp_load 行（驱动 loadHours）+ workcenter（驱动 code）+ workcenter_calendar（驱动 capacityHours）。workcenter_capacity 提供 efficiencyFactor（缺省回退 1，但 seed 一行保证参照完整）。

## 2. seed 表清单 + 列映射（每表：mandatory 业务列 / FK 列 / 框架列省略）

> 标注：**M**=mandatory（CSV 须填）；**FK**=外键引用上游已 seed ID；**opt**=可选（默认值或 null，按需填）。框架审计列（DEL_VERSION/VERSION/CREATED_BY 等）全部省略。

### 2.1 工作中心配置链 + crp_load（manufacturing）— 4 张新表

| 表 | code 列（角色） | seed 行 |
|----|----------------|--------|
| erp_mfg_workcenter | ID; CODE(M, UK); NAME(M); CAPACITY(opt decimal); CAPACITY_UNIT(opt); HOURLY_RATE(opt decimal); WORK_HOURS_PER_DAY(opt decimal); IS_EXTERNAL(opt bool, default false); REMARK(opt) | 1 |
| erp_mfg_workcenter_calendar | ID; WORKCENTER_ID(FK workcenter,M); ORG_ID(FK org=2,opt); CALENDAR_NAME(M); SHIFT_TYPE(M, dict erp-mfg/shift-type: ONE_SHIFT/MORNING/AFTERNOON/NIGHT); WORK_DATE_PATTERN(opt, dict erp-mfg/work-date-pattern: ALL_WEEK/WEEKDAYS/WEEKEND → **本批置 ALL_WEEK** 使 patternMatches 恒 true，确定性跨周几); START_TIME(opt VARCHAR `HH:mm`); END_TIME(opt VARCHAR `HH:mm`); EFFECTIVE_FROM(opt DATE); EFFECTIVE_TO(opt DATE); IS_ACTIVE(opt bool, default true → **本批置 true** 驱动 calendarsByWorkcenter 读取); REMARK(opt) | 1 |
| erp_mfg_workcenter_capacity | ID; WORKCENTER_ID(FK workcenter,M); ORG_ID(FK org=2,opt); MATERIAL_ID(FK material,M); CAPACITY_PER_HOUR(M decimal); SETUP_TIME(opt decimal, default 0); CLEANUP_TIME(opt decimal, default 0); EFFICIENCY_FACTOR(opt decimal, default 1 → **本批置 1** 驱动 capacityHours=rawCapacity); IS_ACTIVE(opt bool, default true → **本批置 true** 驱动 efficiencyByWorkcenter 读取); REMARK(opt) | 1 |
| erp_mfg_crp_load | ID; WORKCENTER_ID(FK workcenter,M); ORG_ID(FK org=2,opt); WORK_ORDER_ID(FK work_order 弱指针,opt → **本批置 1** 引用 0930-1 WO-1 丰富参照); LOAD_DATE(M DATE); LOAD_HOURS(opt decimal, default 0 → **本批置 4** 驱动 loadHours 非零); SETUP_HOURS(opt decimal, default 0 → **本批置 1**); REMARK(opt) | 1 |

**字典码值（dict.yaml 已核实）**：
- `erp-mfg/shift-type`：ONE_SHIFT(全天单班)/MORNING(早班)/AFTERNOON(中班)/NIGHT(夜班) → 本批用 ONE_SHIFT
- `erp-mfg/work-date-pattern`：ALL_WEEK(全周)/WEEKDAYS(工作日周一至周五)/WEEKEND(仅周末) → 本批用 ALL_WEEK
  - `patternMatches`（CrpLoadCalculator.java:501-513）：pattern null 或 ALL_WEEK → 恒 true；WEEKDAYS → 周一~周五；WEEKEND → 周六/周日。选 ALL_WEEK 使负荷日 2026-07-15（周三）确定性命中，不受周几漂移影响。

## 3. capacityHours 口径核实（availableHours 精确派生）

### 3.1 availableHours 计算链（CrpLoadCalculator.java:484-499）

```java
availableHours(calendars, date):
  total = 0
  for c in calendars:
    if c.effectiveFrom != null && c.effectiveFrom.isAfter(date): continue   // 生效边界过滤
    if c.effectiveTo   != null && c.effectiveTo.isBefore(date):  continue
    if !patternMatches(date, c.workDatePattern): continue                  // 周几模式过滤
    total += shiftHours(c.startTime, c.endTime)                            // 班次时长
  return total
```

### 3.2 shiftHours 计算（CrpLoadCalculator.java:515-530）

```java
shiftHours(startTime, endTime):
  s = LocalTime.parse(startTime.trim())    // "08:00" → 08:00
  e = LocalTime.parse(endTime.trim())      // "16:00" → 16:00
  mins = Duration.between(s, e).toMinutes()  // 8h = 480 min
  return new BigDecimal(mins).divide(SIXTY=60, SCALE=4, HALF_UP)  // 8.0000
```

### 3.3 本批 seed 派生（确定性，逐项核实）

- seed workcenter_calendar: START_TIME=`08:00`, END_TIME=`16:00`, EFFECTIVE_FROM=`2026-07-01`, EFFECTIVE_TO=`2026-07-31`, WORK_DATE_PATTERN=`ALL_WEEK`, IS_ACTIVE=`true`
- 负荷日 LOAD_DATE=`2026-07-15`（周三）：
  - effectiveFrom(2026-07-01).isAfter(2026-07-15)? **否** → 不过滤
  - effectiveTo(2026-07-31).isBefore(2026-07-15)? **否** → 不过滤
  - patternMatches(2026-07-15, ALL_WEEK)? **是**（ALL_WEEK 恒 true）→ 不过滤
  - shiftHours("08:00","16:00") = Duration 480 min / 60 = **8.0000**
- availableHours = **8.0000**
- capacityHours = rawCapacity × efficiency = 8.0000 × efficiencyFactor(1.0000，seed workcenter_capacity) = **8.0000**（setScale 4 HALF_UP）
  - efficiency 经 `efficiencyByWorkcenter`：读 erp_mfg_workcenter_capacity where workcenterId=1 AND isActive=true → efficiencyFactor=1 → map={1: 1}

## 4. 范围 Decision（Phase 1 item 1.c）

### 4.1 (a) seed 范围裁决：**seed 4 表完整配置链（selected）**

**选择**：seed workcenter(1) + workcenter_calendar(1) + workcenter_capacity(1) + crp_load(1)，以一致 workcenterId=1 串联；crp_load.workOrderId 引用 0930-1 WO-1（id=1，弱指针参照丰富）；workcenter_capacity.materialId=1（1234-1 产品甲 MAT-001）。

**替代方案分析**：
- 「仅 seed workcenter + crp_load（不 seed calendar/capacity）」：**rejected**。理由：`getLoadReport` 需 calendar 算 capacityHours（`calendarsByWorkcenter` 读 calendar → `availableHours`）。缺 calendar 则 `calendarByWc.getOrDefault(wcId, emptyList())` 返回空 → availableHours=0 → capacityHours=0 → loadRate 经 `computeLoadRate` 返回 9999（loadHours>0 时除零兜底）/ 0 → 报表数值不可观测/不稳定（overloaded 恒 true 且 loadRate=9999 无业务意义）。故必须 seed calendar。capacity 虽 efficiency 缺省回退 1 仍可工作，但 seed 一行保证参照完整（镜像 1145-2 Strategy C 范式）。

### 4.2 (b) posted 裁决：四表均无 posted 列（N/A）

镜像 0930-1 裁决。依据：
1. 四表（workcenter/workcenter_calendar/workcenter_capacity/crp_load）ORM 均**无 `posted` 列**（逐表 ORM 核实），crp_load 为负荷快照非 GL，看板/报表读域表，`posted` 非任何过滤列；
2. 1234-1 seed 的科目表无制造费用/差异/在产品专用科目，seed GL 凭证徒增参照复杂度；
3. 制造域过账 → GL 凭证 seed 归后续（Deferred）。

### 4.3 (c) 日期窗口裁决

当前日期 2026-07-09。报表区间由 spec 显式传 workcenterId=1/startDate=2026-07-15/endDate=2026-07-15 锁定（镜像 fin-income-statement `data:{periodId}` 范式）。使报表区间 `[2026-07-15, 2026-07-15]` 确定性返回恰好 1 行（getLoadReport 内 `for d = periodFrom..periodTo` 仅迭代 1 次）：

- **crp_load.LOAD_DATE**（mandatory DATE）：置 `2026-07-15`（种子月内确定性单日，周三 ALL_WEEK 命中）。
- **workcenter_calendar.EFFECTIVE_FROM/EFFECTIVE_TO**（optional DATE）：置 `2026-07-01`/`2026-07-31`（覆盖负荷日 + 种子月，使 availableHours 生效边界过滤不过滤）。

### 4.4 (d) 期望值 token 派生（每 token 标注 seed 行依据 + 计算口径）

| token | 期望渲染值 | seed 行依据 | 计算口径 |
|-------|-----------|------------|---------|
| workcenterCode | `WC-001` | workcenter.CODE | seed 直值 |
| loadDate | `2026-07-15` | crp_load.LOAD_DATE | seed 直值 |
| loadHours | `4.00` | crp_load.LOAD_HOURS=4 | seed 直值（Decimal scale 2），经 NumberFormat `#,##0.00` 渲染 |
| capacityHours | `8.00` | calendar START_TIME=08:00/END_TIME=16:00 + capacity EFFICIENCY_FACTOR=1 | shiftHours(08:00,16:00)=480min/60=8.0000 × 1 = 8.0000，渲染 `8.00` |
| loadRate | `0.50` | loadHours/capacityHours | computeLoadRate(4.00, 8.0000) = 4.00/8.0000 = 0.5000（scale 4 HALF_UP），渲染 `0.50` |
| 报表标题 | `CRP 工作中心负荷分析表` | crp-load-report.xpt.xml:24 | 模板静态文本 |

**断言稳健性**：上述数值 token 经 `_helper.ts#assertReportRenderedWithValue` 剥离千分位逗号后匹配（`html.replace(/,/g,'')`）。`4.00`/`8.00`/`0.50` 既是 NumberFormat `#,##0.00` 渲染结果，亦是未格式化 BigDecimal toString（`4.00`/`8.0000`/`0.5000`）的子串，故无论报表引擎是否应用 cell numberFormat，断言恒成立。

**overloaded**：loadRate=0.5000 ≤ threshold=1.0（`erp-mfg.crp-overload-threshold` 默认 1.0）→ overloaded=`false`（非超负荷，业务合理：50% 负荷）。本批不断言 overloaded token（聚焦非空 token + 数值，overloaded=false 为派生副产物）。

## 5. 记录设计（引用 1234-1/0930-1 已 seed 主数据固定 ID）

通用：orgId=2(ERP-CO)、material 1(产品甲 MAT-001/PCS)、workOrder 1(WO-2026-001，0930-1 准时完工工单)。

**工作中心配置链 + crp_load**：
- **workcenter**（1 行）：
  - 1：`WC-001` 主装配线，capacity=10.0000 PCS，hourlyRate=120.0000，workHoursPerDay=8.00，isExternal=false。
- **workcenter_calendar**（1 行 IS_ACTIVE=true 驱动 capacityHours）：
  - 1：workcenterId=1，orgId=2，calendarName=主装配线单班日历，shiftType=ONE_SHIFT，workDatePattern=ALL_WEEK，startTime=08:00，endTime=16:00，effectiveFrom=2026-07-01，effectiveTo=2026-07-31，isActive=true。
- **workcenter_capacity**（1 行 EFFICIENCY_FACTOR=1 驱动 capacityHours=rawCapacity）：
  - 1：workcenterId=1，orgId=2，materialId=1，capacityPerHour=10.0000，setupTime=60.00，cleanupTime=30.00，efficiencyFactor=1.0000，isActive=true。
- **crp_load**（1 行 LOAD_HOURS=4 驱动 loadHours 非零）：
  - 1：workcenterId=1，orgId=2，workOrderId=1，loadDate=2026-07-15，loadHours=4.00，setupHours=1.00。

## 6. 条件性 SQL 裁决

Phase 2 条件性 SQL Add 项：**移出范围**。所有配置链 + crp_load 种子经 CSV INSERT 表达，无序列重置 / 批量 UPDATE 需求。故不补 `NN-init-mfg-crp-*.sql`。

## 7. seed 行数汇总

| 域 | 表数 | 行数 |
|----|------|------|
| manufacturing 工作中心配置链 + crp_load | 4（workcenter, workcenter_calendar, workcenter_capacity, crp_load） | 1+1+1+1 = 4 |
| **合计** | **4 张新 CSV** | **4 行** |

> 4 张新 CSV 加入 `_vfs/_init-data/`，与前序 87 张 CSV 共存（总计 87 + 4 = **91 张 CSV**）。

## 8. 残留风险与防护

- 参照完整性遗漏（FK 列引用未 seed 的上游 ID）→ 启动期 DataInitInitializer 抛 NopException（不静默跳过），Phase 3 fresh-DB 启动验证兜底暴露。
- 列名错配 → 同上启动期暴露。
- **START_TIME/END_TIME 格式**（VARCHAR `HH:mm`）：`shiftHours` 经 `LocalTime.parse` 解析，ISO_LOCAL_TIME 接受 `HH:mm`；格式错配（如 `8:00` 缺前导零）会抛 DateTimeParseException → shiftHours 返回 0 → capacityHours=0 → loadRate 除零兜底 9999。本批用 `08:00`/`16:00` 规范格式。
- 非幂等（前序 7 批已确认）→ fresh-DB 重置（删 `db/erp.mv.db`）是必需前置，playwright webServer 已内置。
- 字典码值错配（shift-type/work-date-pattern 非法值）→ 启动期 entity 校验阶段暴露（dict 校验）。
- **CRP 重算覆盖风险**：`ErpMfgCrpRunJob`/`CrpLoadCalculator.calculateLoad` 经 nop-job 双层门控（`erp-mfg.crp-run-cron` 默认空 + config `erp-mfg.crp-load-source` 默认 WORK_ORDER 不影响 `getLoadReport` 读既有行），fresh-DB 启动不触发重算，seed 静态 crp_load 行安全不被覆盖。重算链端到端回归属 Deferred。
- **capacityHours=0 风险**：若 calendar IS_ACTIVE≠true 或 effectiveFrom/To 不覆盖 loadDate 或 workDatePattern 不匹配 → availableHours=0 → loadRate 除零兜底 9999。本批 IS_ACTIVE=true + EFFECTIVE 2026-07-01~07-31 覆盖 2026-07-15 + ALL_WEEK 恒匹配，三重防护确保 capacityHours=8.0000 确定性。
