# 设备维护域页面设计要点

> 本文档定义设备维护域关键业务页面的结构布局、交互模式与导航流程。
> 字段定义以 `model/app-erp-maintenance.orm.xml` 为准，业务语义与状态机见 `state-machine.md`、`equipment-integration.md`。
> 调研引用格式 `[源项目#要点]`，详见 `docs/analysis/erp-survey/`。

## 设计原则

1. **设备状态一目了然**：设备列表页以状态色块/图标展示设备当前运行状态（运行中/闲置/维护中/故障/已停用），支持按状态快速筛选。
2. **计划到期主动提示**：维护计划到期自动生成待执行维护访问，在仪表板中集中展示"到期/逾期"维护项，逾期项红色标记。
3. **维护历史可追溯**：每台设备详情页聚合其全部维护历史（计划性维护 + 响应性维修 + 停机记录），形成完整设备档案。
4. **备件消耗联动库存**：维护访问执行时消耗备件通过库存移动单出库，页面内嵌备件选择器（实时显示备件库存可用量）。

## 页面清单

| 页面 | 类型 | 主要用户 | 复杂度 |
|------|------|----------|--------|
| 设备列表 | 卡片/列表双视图 | 维护员 | ★★☆ |
| 设备详情（档案） | 仪表板式详情 | 维护员 | ★★★ |
| 维护计划编辑 | 表单 | 维护计划员 | ★★☆ |
| 维护访问执行 | 分步向导/表单 | 维护技术员 | ★★★ |
| 维护请求（报修） | 表单+列表 | 操作工/维护员 | ★★☆ |
| 停机记录 | 简单表单 | 维护员 | ★☆☆ |
| 维护仪表板 | 卡片式概览 | 维护主管 | ★★☆ |

## 各页面设计要点

### 设备列表与详情

**设备列表**：
- 卡片视图：每个设备一张卡片，显示设备编码、名称、状态色块（运行中=🟢、闲置=🔵、维护中=🟡、故障停机=🔴、已停用=⚪）[注：依据 ORM 字典 erp-mnt/equipment-status，DOWN=故障停机而非"故障"]、位置
- 列表视图：表格列显式，支持按位置/状态/类别筛选
- 顶部搜索框支持编码/名称快速定位

**设备详情（档案）**：
```
┌─────────────────────────────────────────────────────────┐
│ 设备: EQ-001  空压机 A        🔴 故障停机                │
├─────────────────────────────────────────────────────────┤
│ ┌─ 基本信息 ────────────┐  当前状态: 故障停机            │
│ │ 资产编码: AST-001      │  故障时间: 2026-06-20 14:00   │
│ │ 位置: 车间 A-01        │  当前正在维修: WO-2026-001   │
│ │ 投产日期: 2023-01-01   │  [查看维修]                  │
│ └────────────────────────┘                              │
├─────────────────────────────────────────────────────────┤
│ 维护时间轴                                                │
│ ┌────────────────────────────────────────────────────┐  │
│ │ 06-01 月度润滑 (计划) ✓                             │  │
│ │ 05-15 更换滤芯 (计划) ✓                             │  │
│ │ 05-10 异响报修 (响应) ✓ → 更换轴承                   │  │
│ │ 04-01 季度保养 (计划) ✓                             │  │
│ └────────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────┤
│ 待执行维护计划                                            │
│ 下次润滑: 2026-07-01 (还有 7 天)                         │
│ 下次校准: 2026-08-15 (还有 52 天)                        │
└─────────────────────────────────────────────────────────┘
```

**要点**：
- 当前状态大色块展示，故障时状态区高亮闪烁（或醒目标记）
- 维护时间轴按时间倒序，计划性维护=蓝色、响应性维修=橙色
- 待执行计划显示到期倒计时（7 天内=黄色、已逾期=红色）
- [注：ErpMntEquipment 尚无 to-many 指向 Schedule/Visit/DowntimeEntry，维护时间轴需通过 IDaoProvider 查询聚合。ErpMntVisit 尚无 visitType(计划/响应)/result(正常/异常)列及 to-many 指向 SparePartUsage，为设计意图待补充 ORM。]

### 维护访问执行

**页面入口**：设备维护 → 待执行维护 → 开始执行

**分步引导**：
```
步骤 1: 维护信息确认
├─ 关联设备: (自动带出)         维护类型: (计划/响应)
├─ 计划日期: (自动带出)         执行日期: [选择]
├─ 维护人员: [选择]             维护内容: [文本 + 模板选择]

步骤 2: 备件消耗录入
├─ [+] [扫描条码]
│ ┌────┬────────┬──────┬──────┬──────┐
│ │ 备件  │ 数量   │ 单位  │ 库存量│ 操作  │
│ │ 滤芯  │ 2      │ 个    │ 5     │ [×]  │
│ │ 密封圈│ 4      │ 个    │ 12    │ [×]  │
│ └────┴────────┴──────┴──────┴──────┘
├─ 备件选择器实时显示库存，不足时黄色警告

步骤 3: 执行结果
├─ 维护结果: (正常 / 异常 / 部分完成)
├─ 备注/建议: [___]
├─ 附件: [上传图片/文档]
├─ 设备状态: (自动恢复"运行中" 或 手动选择)

步骤 4: [确认完成]
```

**要点**：
- 备件消耗自动生成库存出库移动单（后台调用 `IErpInvStockMoveBiz`）
- 完成时自动更新设备状态（默认恢复"运行中"）
- 若选择"异常"，自动创建维护请求或 NCR
- 支持扫码枪录入备件条码

### 维护请求（报修）

**简易表单**：
- 报修人/联系电话（自动带出当前用户）
- 设备：[选择] 或 [扫描设备二维码]（支持快速选择）
- 故障描述（必填）：文本 + 可选图片/视频附件
- 优先级：紧急/高/中/低（颜色标记：红/橙/黄/灰）
- 提交后自动创建维护访问（DRAFT），通知维护团队

### 维护仪表板

**上级页面入口**：设备维护 → 仪表板

```
┌──────────────┬──────────────┬──────────────┐
│ 待执行维护     │ 本月已完成    │ 故障未处理    │
│ 5 项          │ 12 项        │ 2 项          │
│ ⚠️ 2 项已逾期  │             │ 🔴 1 项紧急   │
├──────────────┴──────────────┴──────────────┤
│ 设备健康概览                                  │
│ 🟢 运行中 24台  🟡 维护中 3台  🔴 故障 2台   │
│ ⚪ 已停用 1台    🔵 闲置 2台                  │
├─────────────────────────────────────────────┤
│ 到期维护预警                                  │
│ ┌────┬────────┬──────┬──────┬────────┐     │
│ │ 设备 │ 维护项目│ 计划日 │ 状态  │ 操作   │     │
│ │ EQ01│ 润滑   │ 06-20 │ 🔴逾期│ [执行] │     │
│ │ EQ02│ 校准   │ 06-22 │ 🟡今日│ [执行] │     │
│ └────┴────────┴──────┴──────┴────────┘     │
└─────────────────────────────────────────────┘
```

## 跨页面导航流

```
维护计划 → [周期到期自动生成] → 待执行维护列表
    ↓
维护请求（报修）→ [受理] → 维护访问
    ↓
维护访问执行 → [备件消耗] → 库存出库移动单 (自动)
    ↓             ↓
 设备状态更新    生成维护凭证/成本记录
    ↓
设备详情 ← [维护时间轴更新] → 维护历史
```

## 调研参考

| 设计点 | 参考来源 | 应用方式 |
|--------|----------|----------|
| 设备状态色块卡片 | Atlas CMMS#Equipment | 设备列表页卡片式展示状态 |
| 维护计划周期自动生成 | Atlas CMMS#Schedule | 到期自动生成维护访问 |
| 备件消耗联动库存 | SuperCMMS#Parts | 维护访问内嵌备件选择器 |
| 设备维护历史时间轴 | Atlas CMMS#History | 设备详情页聚合全部维护记录 |
| 停机记录关联工作中心 | Odoo#maintenance | 停机记录影响制造域排产 |

## 主交易实体 form 布局分组

> 适用范围：维护域 6 个主交易实体（不含已 1500-1 覆盖的 `ErpMntVisit`）独立 `view.xml` 的 `<form id="view">` / `<form id="edit">` 分组。
> 决策来源：`docs/plans/2026-07-20-2059-1-f3-p1-mfg-tier-form-layout.md` Phase 0.E。
> 维护域主实体普遍字段数中等：Request 突出 5 态自定义 status；Equipment 突出设备状态/位置/分类（≥15 字段，size=lg）；Schedule 突出周期/触发规则；Calibration 与 ErpQaCalibration 同构。

### 模板分化决策

| 实体 | 分组结构 |
|------|----------|
| ErpMntCalibration | baseInfo + measure + schedule + status + audit |
| ErpMntDowntimeEntry | baseInfo + reason + audit |
| ErpMntEquipment | baseInfo + audit（≥15 字段，size=lg） |
| ErpMntRequest | baseInfo + dispatch + status + audit |
| ErpMntSchedule | baseInfo + audit |
| ErpMntSparePartUsage | baseInfo + amount + status + posting + audit |

### ErpMntRequest 模板（自定义 5 态 status）

```xml
<form id="view" size="lg">
    <layout x:override="replace">
=========>baseInfo[基本信息]======
 code[请求单号] equipmentId[设备]
 requestDate[请求日期] description[问题描述]
 priority[优先级] requestedBy[请求人]
=========>dispatch[派工]======
 assignedTo[指派给] acceptedBy[受理人]
=========>status[状态信息]======
 status[状态] completedBy[完成人]
 completedAt[完成时间]
========^audit[审计信息]=========
 remark[备注]
 createdBy[创建人] createTime[创建时间]
 updatedBy[修改人] updateTime[修改时间]
    </layout>
</form>
```

### ErpMntEquipment 模板（≥15 字段，size=lg）

```xml
<form id="view" size="lg">
    <layout x:override="replace">
=========>baseInfo[基本信息]======
 code[设备编码] name[设备名称]
 orgId[业务组织] assetId[资产]
 workcenterId[工作中心] locationId[位置]
 categoryId[设备分类] status[状态]
 serialNo[序列号] manufacturer[制造商]
 model[型号] installDate[安装日期]
 warrantyExpiry[保修到期]
========^audit[审计信息]=========
 remark[备注]
 createdBy[创建人] createTime[创建时间]
 updatedBy[修改人] updateTime[修改时间]
    </layout>
</form>
```

### query 表单基线

所有维护域主实体的 `<form id="query">` 至少含 5 个查询字段。`code` 配 `filterOp=like`；`equipmentId`/`status`/`docStatus`/`approveStatus`/`categoryId` 配 `filterOp=eq`；`requestDate`/`businessDate` 配 `filterOp=date-between`。

## Line 子实体 form 分组模板

> 适用范围：维护域 2 个 Line 子实体独立 `view.xml` 的 `<form id="view">` / `<form id="edit">` 分组。
> 维护域 Line 模板：备件行 baseInfo+quantity+reference+audit；任务行 baseInfo+status+reference+audit。

### 模板分化决策

| 实体 | 分组结构 |
|------|----------|
| ErpMntSparePartUsageLine | baseInfo + quantity + reference + audit |
| ErpMntVisitTask | baseInfo + status + reference + audit |

### ErpMntSparePartUsageLine 模板

```xml
<form id="view" size="lg">
    <layout x:override="replace">
=========>baseInfo[基本信息]======
 lineNo[行号] materialId[物料]
 uoMId[计量单位] batchNo[批次]
=========>quantity[数量与价格]======
 quantity[数量] unitCost[单价]
 amount[金额]
=========>reference[业务关联]======
 sparePartUsageId[备件使用单]
========^audit[审计信息]=========
 remark[备注]
 createdBy[创建人] createTime[创建时间]
 updatedBy[修改人] updateTime[修改时间]
    </layout>
</form>
```

### query 表单基线

所有维护域 Line 实体的 `<form id="query">` 至少含 5 个查询字段。`lineNo`/`materialId`/`sparePartUsageId` 配 `filterOp=eq`；`batchNo` 配 `filterOp=like`。
