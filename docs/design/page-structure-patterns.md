# 页面结构增强范式（Page Structure Patterns）

> Owner docs: `docs/backlog/frontend-ui-roadmap.md` §F12（页面结构增强）、`docs/architecture/view-and-page-strategy.md`（页面策略）、`docs/design/child-table-editor-patterns.md`（F4 子表编辑范式，与本范式正交）
> 落地计划：`docs/plans/2026-07-21-0330-3-f12-page-structure-tabs-wizards.md`（F12 Tier A 5 头实体 + Tier B 3 头实体）

## 1. 目的与范围

固化「ERP 复杂页面结构」的标准范式，覆盖 **tabs 容器** 与 **仪表板/多 tab 详情** 两类，供后续域（projects/quality/crm/cs/contract 等长尾页面 successor plan）按图施工。

**适用范围**：
- 头+行单据需要按 section 分 tab 展示（基本信息 / 行明细 / 关联单据 / 审计信息）
- 单实体多 group 仪表板（HR 员工档案 / AST 资产卡片 / MNT 设备详情）
- 跨实体子表多 tab 详情（如 HR 员工 + 合同 + 考勤 + 休假）—— 本计划范围外，归 successor

**不适用**：
- 子表行内编辑本身 → `child-table-editor-patterns.md`（F4）
- 跨单据行导入 → `cross-doc-navigation-patterns.md`（F9）
- 多步骤业务向导（wizard） → Tier C successor（ErpFinAccountingPeriod 期末结账向导等）
- 拖拽式看板视图、甘特图、日历 → F13 / F16

## 2. tabs 容器：两种实现机制

Nop view.xml 提供两条 tabs 实现路径，按场景选择。

### 机制 A：`<form layoutControl="tabs">`（form 内 group→tab）

**适用场景**：单实体 form 内多 group 已存在，希望「分段折叠显示」改为「分段 tab 显示」。

**机制原理**：在 `<form id="view|edit">` 上加 `layoutControl="tabs"` 属性。Nop codegen `web.xlib:GenFormBody` 检测此属性后调用 `GenLayoutTabs` 而非 `GenLayoutGroups`，把 `<layout>` 中以 `=========>sectionId[Section Title]======`（10 个等号 + `>` 标记）或 `==========^sectionId[Section Title]=========`（10 个等号 + `^` 标记，原 collapsable fieldSet）分隔的 group 自动展开为 AMIS `tabs` + 每 group 一个 `tab`。

**关键证据**：
- `nop-entropy/.../nop-web/src/main/resources/_vfs/nop/web/xlib/web.xlib:130-153`（GenFormBody 内 `c:choose` 分发）
- `nop-entropy/.../nop-web/src/main/resources/_vfs/nop/web/xlib/web.xlib:210-241`（GenLayoutTabs 实现）
- `nop-entropy/.../nop-kernel/nop-xdefs/src/main/resources/_vfs/nop/schema/xui/form.xdef:5`（属性 schema 说明）

**最小写法**：

```xml
<form id="view" size="lg" layoutControl="tabs">
    <layout x:override="replace">
=========>baseInfo[基本信息]======
 code[单号] orgId[业务组织]
=========>amount[金额信息]======
 @totalAmount[合计金额] @totalTaxAmount[合计税额]
=========>lines[明细行]======
 lines[明细行](2)
=========>audit[审计信息]======
 createdBy[创建人] createTime[创建时间]
    </layout>
    <cells>
        <cell id="lines">
            <view path="/erp/{short}/pages/{LineEntity}/{LineEntity}.view.xml" grid="sub-grid-view"/>
        </cell>
    </cells>
</form>
```

**渲染输出**（_dump 验证）：
```yaml
body:
- type: tabs
  id: view-tabs
  tabs:
  - title: 基本信息
    name: baseInfo
    tab: [...]
  - title: 金额信息
    name: amount
    tab: [...]
  - title: 明细行
    name: lines
    tab:
    - type: group
      body:
        name: lines
        type: input-table  # F4 sub-grid-edit 仍渲染在 lines tab 内
        columns: [...]
  - title: 审计信息
    name: audit
    tab: [...]
```

**子表（F4 child-table-editor）兼容性**：form tabs 默认不 unmount 非活动 tab（不同于 `<pages><tabs unmountOnExit>`），切换 tab 不丢失子表行数据。`<cell id="lines"><view path="..." grid="sub-grid-edit|sub-grid-view"/></cell>` 配置完全不动，继续在 lines tab 内渲染 AMIS `input-table`。

**F9 关联单据兼容性**：既有 row-action drawer（如 ErpPurOrder `row-view-receive-button` 打开关联入库单 drawer）保持不变；关联单据作为独立 row-action 触发，不嵌入 form tabs。这与 F9 范式正交，可叠加。

### 机制 B：`<pages><tabs>` + 多 `<simple>/<crud>` 子页（page 级 tabs）

**适用场景**：跨实体多 tab 详情（如员工档案 = 员工表单 + 合同子表 + 考勤子表 + 休假子表）；需要一个 row-action 打开复杂结构 drawer。

**机制原理**：在 `<pages>` 内新增 `<tabs name="...">` 元素，其 `<tab page="..."/>` 引用同 `<pages>` 内其他已定义的 `<simple>` 或 `<crud>` 子页。row-action 通过 `dialog page="<tabs name>"` 打开包含 tabs 的 drawer。

**真实样例**：`nop-entropy/nop-job/nop-job-web/.../NopJobSchedule/NopJobSchedule.view.xml:123-126` —— `<tabs name="runtimeTabs">` + `<tab page="runtimeSummary"/>` + `<tab page="runtimeFires"/>`，由 row-action `runtime-summary-button` 打开。

**最小写法**：

```xml
<pages>
    <crud name="main" x:inherit="true">
        <rowActions>
            <action id="row-view-tabs-button" label="查看详情" actionType="drawer">
                <dialog page="detailTabs" size="xl">
                    <data>
                        <id>${id}</id>
                    </data>
                </dialog>
            </action>
        </rowActions>
    </crud>

    <simple name="headerForm" form="view">
        <initApi url="@query:Entity__get?id=${id}" gql:selection="{@formSelection}"/>
    </simple>

    <crud name="relatedLines" x:prototype="view-list">
        <table>
            <api url="@query:RelatedEntity__findPage/{@pageSelection}?filter_entityId=${id}"/>
        </table>
    </crud>

    <tabs name="detailTabs" tabsMode="vertical" mountOnEnter="true" unmountOnExit="true">
        <tab name="headerForm" page="headerForm" title="基本信息"/>
        <tab name="relatedLines" page="relatedLines" title="关联明细"/>
    </tabs>
</pages>
```

**何时用 B 而非 A**：
- 需嵌入跨实体子 crud（如员工档案的合同/考勤子表） → B
- 需独立数据加载策略（懒加载、缓存、reload） → B（`mountOnEnter=true` 仅在切到 tab 时拉数据）
- 需不同 tab 用不同 form/page 模板 → B
- 仅希望把单 form 的多 group 分 tab 显示 → A（最小代价）

## 3. 仪表板范式：双列布局 + 时间线 + 数据加载策略

### 双列布局

AMIS `form` 的 `mode="inline"` + `<layout>` 中每行 2 cell（默认 `defaultColumnRatio=2`）天然形成双列。无需特殊配置，layout 写两字段一行即可。

### 时间线/凭证列表

**前置后端条件**：仪表板若需展示时间序列（折旧计划、维护历史、凭证流水等），按以下优先级选择数据源：

1. **既有 `@BizQuery` 专用聚合**（最佳）—— 例：`ErpMntReport__maintenanceHistoryData(equipmentId, startDate, endDate)` 已就绪，返回 visit×equipment 聚合。
2. **既有实体 `findPage` + filter_** （降级）—— 例：`ErpAstDepreciationSchedule__findPage?filter_assetId=$id` 直接拉按 assetId 过滤的折旧计划行；接受可能 N+1。
3. **新增后端 `@BizQuery` 聚合**（性能优化）—— 仅当数据量 > 1000 行或加载 > 2s 时触发，归 successor plan。本计划不实施。

**`@BizQuery` 返回 `List<Map>` vs `findPage` 的选择**：
- `findPage` 天然适配 AMIS `<crud>` 的分页 API（`/findPage/{@pageSelection}`），**优先用于 crud tab**
- `List<Map>` 非分页，适合报表渲染（如 `maintenance-history.page.yaml` 的 service+html 模式），**不适合 crud tab**
- 例：`maintenanceHistoryData` 返回 `List<Map>`，故 ErpMntEquipment drawer 的维护时间线 tab 用 `ErpMntVisit__findPage?filter_equipmentId=${id}` 而非直接调 `maintenanceHistoryData`

### 完整仪表板 drawer（机制 B）真实样例

本仓库 3 个生产落地（plan 2026-07-22-0845-1）：

| 头实体 | drawer tabs | ref-*.page.yaml |
|--------|------------|-----------------|
| ErpHrEmployee | 基本信息 / 合同（ErpHrEmploymentContract filter_employeeId）/ 考勤（ErpHrAttendance）/ 休假（ErpHrLeaveRequest）/ 工时（ErpHrTimesheet） | `module-hr/erp-hr-web/.../ErpHrEmployee/ref-employee.page.yaml` |
| ErpAstAsset | 基本信息 / 折旧时间线（ErpAstDepreciationSchedule filter_assetId） | `module-assets/erp-ast-web/.../ErpAstAsset/ref-asset.page.yaml` |
| ErpMntEquipment | 基本信息（含 status 色块 gen-control）/ 维护时间线（ErpMntVisit filter_equipmentId）/ 备件消耗（ErpMntSparePartUsage filter_equipmentId） | `module-maintenance/erp-mnt-web/.../ErpMntEquipment/ref-equipment.page.yaml` |

**最小写法**（以 ErpAstAsset 为例）：

```xml
<view x:extends="_gen/_ErpAstAsset.view.xml" ...>
    <grids>
        <!-- 既有 list / pick-list -->
        <!-- NEW: drawer crud grid。custom="true" 因为字段属于关联实体 ErpAstDepreciationSchedule，不在 ErpAstAsset 的 objMeta 中 -->
        <grid id="archive-depreciation-list">
            <cols>
                <col id="id" custom="true" mandatory="true" ui:number="true" sortable="true" domain="long"/>
                <col id="period" custom="true" sortable="true" domain="string"/>
                <col id="plannedAmount" custom="true" sortable="true" align="right" domain="amount">
                    <gen-control><c:script><![CDATA[ return { type: 'number', kilometer: true, precision: 2 }; ]]></c:script></gen-control>
                </col>
                <col id="status" custom="true" mandatory="true" sortable="true" domain="string"/>
            </cols>
        </grid>
    </grids>

    <pages>
        <crud name="main">
            <rowActions x:override="bounded-merge">
                <!-- 既有 row-actions -->
                <!-- NEW: drawer 触发按钮 -->
                <action id="row-view-asset-dashboard-button" label="完整仪表板" level="info" icon="fa fa-tachometer"
                        actionType="drawer">
                    <dialog page="assetDashboard" size="xl">
                        <data><id>${id}</id></data>
                    </dialog>
                </action>
            </rowActions>
        </crud>

        <!-- NEW: headerForm simple（复用既有 view form，initApi 拉头数据） -->
        <simple name="assetHeader" form="view">
            <initApi url="@query:ErpAstAsset__get?id=${id}" gql:selection="{@formSelection}"/>
        </simple>

        <!-- NEW: 关联实体 crud（filter_assetId=${id}，{@pageSelection} 解析为关联实体的页面选择集） -->
        <crud name="assetDepreciation" grid="archive-depreciation-list">
            <table noOperations="true">
                <api url="@query:ErpAstDepreciationSchedule__findPage/{@pageSelection}?filter_assetId=${id}"/>
            </table>
        </crud>

        <!-- NEW: tabs 组装。mountOnEnter=true 懒加载；unmountOnExit=false 保留 DOM 状态 -->
        <tabs name="assetDashboard" mountOnEnter="true" unmountOnExit="false">
            <tab name="header" page="assetHeader" title="基本信息"/>
            <tab name="depreciation" page="assetDepreciation" title="折旧时间线"/>
        </tabs>

        <picker name="picker" filterForm="pick-query"/>
    </pages>
</view>
```

**配套 `ref-asset.page.yaml`**（薄 wrapper，便于 URL 直访或跨页面 dialog 引用）：

```yaml
x:gen-extends: |
  <web:GenPage view="ErpAstAsset.view.xml" page="assetDashboard" xpl:lib="/nop/web/xlib/web.xlib" />
```

**关键决策点**：
- `<simple name="...Header" form="view">` 复用既有 view form（已含 layoutControl="tabs" + 敏感字段 visibleOn="${false}"）。若需独立 headerForm，新建 `<form id="dashboardHeader" editMode="view">` + `<simple form="dashboardHeader">`
- 跨实体 crud 的 grid 列**必须**加 `custom="true"` + `domain="..."`（绕过 `ERR_GRID_COL_NOT_PROP`，见 §6 反模式表）
- `{@pageSelection}` 由 codegen 按 `@query:Entity__findPage` 的实体名解析为该实体的页面字段集，**不**从头实体推导
- `mountOnEnter=true` + `unmountOnExit=false`：初始只拉 headerForm，切 tab 时才拉对应 crud，且切回时保留 DOM 状态（不重复拉）
- `noOperations="true"`：drawer 内 crud 默认只读（不显示 row-action 操作列）；如需操作，移除该属性并显式定义 rowActions

### 跨域 GraphQL selection 注意事项

跨工程 GraphQL selection（如 assets → finance ErpFinVoucherBillR）需满足：
1. 目标实体在 GraphQL schema 中注册（codegen 自动处理）
2. 头实体 view.xml 能引用跨工程实体的 `findPage`（API URL 用 `@query:ErpFinVoucherBillR__findPage`）
3. **filter_ 字段必须在目标实体上存在**——若字段不存在（如 plan 描述的 `filter_sourceEntityType=ASSET` 在 ErpFinVoucherBillR 上不存在），降级为同域单 tab 或归跨域集成 successor

本计划 ErpAstAsset → ErpFinVoucherBillR 因 ErpFinVoucherBillR 无 `assetId`/`sourceEntityType` 字段（实际字段为 `voucherId`/`billType`/`billCode`/`businessType`），按 assetId 反查凭证需 join depreciation/disposal/capitalization 等多张单据，降级为同域单 tab（仅折旧时间线）。

### 数据加载策略：一次 GraphQL 拉全部 vs 每 tab 独立拉取

| 策略 | 适用 | 优缺点 |
|------|------|--------|
| **一次拉全部**（form initApi gql:selection 含子表字段） | 单实体 form + 嵌套 lines 子表 | 优：1 RTT，初始化快；劣：子表数据量大时初始加载慢，无懒加载 |
| **每 tab 独立拉**（mechanism B + `mountOnEnter=true`） | 跨实体多 tab 详情 | 优：初始只拉头，切 tab 时才拉子表；劣：N RTT，tab 切换有延迟 |

**默认推荐**：
- 机制 A `layoutControl="tabs"` 走「一次拉全部」（form initApi 已含 gql:selection）
- 机制 B `<pages><tabs>` 走「每 tab 独立拉」+ `mountOnEnter=true` + `unmountOnExit=false`（避免重复拉，但保留 DOM 状态）

## 4. F12 落地清单

### 已落地（本仓库共 16 页面：Tier A/B 8 + Tier D 5 + Tier B 完整 drawer 3）

#### F12 Tier A/B（plan 2026-07-21-0330-3，8 页面）

| 实体 | 域 | 机制 | tab 列表 |
|------|----|------|---------|
| ErpPurOrder | purchase | A | 基本信息 / 金额信息 / 明细行 / 审批与过账 / 审计信息 |
| ErpSalOrder | sales | A | 基本信息 / 金额信息 / 明细行 / 审批与过账 / 审计信息 |
| ErpInvStockMove | inventory | A | 基本信息 / 仓库信息 / 关联信息 / 明细行 / 审计信息 |
| ErpMfgWorkOrder | manufacturing | A | 基本信息 / BOM 信息 / 计划信息 / 明细行 / 成本信息 / 审计信息 |
| ErpFinVoucher | finance | A | 基本信息 / 过账信息 / 分录行 / 审计信息 |
| ErpHrEmployee | human-resource | A | 基本信息 / 联系方式 / 证件信息（idCardNo 隐藏）/ 雇佣信息 / 薪酬信息（敏感字段隐藏）/ 审计信息 |
| ErpAstAsset | assets | A | 基本信息 / 价值信息 / 折旧信息 / 使用信息 / 审计信息 |
| ErpMntEquipment | maintenance | A | 基本信息 / 审计信息 |

#### F12 Tier D（plan 2026-07-22-0845-1，5 长尾头实体）

| 实体 | 域 | 机制 | tab 列表 | 子表 tab 状态 |
|------|----|------|---------|--------------|
| ErpCrmLead | crm | A | 基本信息 / 联系方式 / 收入预测 / 来源跟踪 / 丢单信息 / 审计信息（6 tab） | activities/quotations Deferred（crm 域 F4 successor） |
| ErpCsTicket | cs | A | 基本信息 / SLA与处理 / 操作历史 / 审计信息（4 tab） | actions cell 就绪（ErpCsTicketAction sub-grid-view） |
| ErpCtContract | contract | A | 基本信息 / 合同方信息 / 财务信息 / 日期信息 / 合同明细行 / 合同版本 / 审计信息（7 tab） | lines + versions cell 就绪（sub-grid-view + sub-grid-edit） |
| ErpPrjProject | projects | A | 基本信息 / 进度信息 / 财务信息 / 审计信息（4 tab） | tasks/budget Deferred（projects 域 F4 successor） |
| ErpQaInspection | quality | A | 基本信息 / 检验信息 / 抽样信息 / 审计信息（4 tab） | lines/results Deferred（quality 域 F4 successor） |

#### F12 Tier B 完整仪表板 drawer（plan 2026-07-22-0845-1，机制 B，3 drawer）

| 头实体 | 域 | drawer tabs | row-action | ref-*.page.yaml |
|--------|----|------------|-----------|-----------------|
| ErpHrEmployee | human-resource | 基本信息 / 合同 / 考勤 / 休假 / 工时（5 tab） | `row-view-employee-archive-button` | `ref-employee.page.yaml` |
| ErpAstAsset | assets | 基本信息 / 折旧时间线（2 tab；跨域凭证 Deferred） | `row-view-asset-dashboard-button` | `ref-asset.page.yaml` |
| ErpMntEquipment | maintenance | 基本信息 / 维护时间线 / 备件消耗（3 tab） | `row-view-equipment-dashboard-button` | `ref-equipment.page.yaml` |

### Deferred（Tier C / 敏感字段脱敏 / 跨域凭证 / 仪表板性能优化 / F16 territory）

| 实体/能力 | 类别 | 触发条件 |
|-----------|------|---------|
| ErpFinAccountingPeriod 期末结账向导（5 步） | Tier C wizard | 财务保护区域 owner doc 明确 wizard 行为 + 后端 mutation 重构授权（roadmap 描述的 5 步 mutation 与实际 BizModel 不一致） |
| ErpMntVisit 任务+备件+停机 tabs / 4 步向导 | Tier C | maintenance F4 P2 successor 完成（child-table-editor 基线就绪） |
| Tier D 域 F4 successor（crm Lead activities/quotations / projects tasks/budget / quality lines/results 子表基线补齐） | Tier D 子表 | 对应域 F4 successor plan 启动 |
| ErpHrEmployee 完整档案 drawer 跨域凭证 tab（assets → finance ErpFinVoucherBillR） | 跨域集成 successor | ErpFinVoucherBillR 无 assetId/sourceEntityType 字段；跨域查询方案明确后（需 join depreciation/disposal/capitalization 等多张单据） |
| 敏感字段脱敏（hr bankAccount / salaryBase / logistics API Key/Secret） | cross-cutting | 敏感字段脱敏独立 plan 启动 |
| 仪表板后端专用 `@BizQuery` | 性能优化 | 仪表板数据量 > 1000 行或加载 > 2s 时 |
| F16 低风险复杂页面（凭证录入完成 + 凭证模板配置 + 三单匹配联查 + 工单进度仪表板 + NCR 详情页） | F16 低风险批已完成 | ✅ 已落地（plan `2026-07-22-0845-2`），见 §8 F16 复杂页面范式 |
| F16 高风险复杂页面（aps 甘特图 + mfg BOM 树） | F16 高风险批已完成 | ✅ 已落地（plan `2026-07-22-1400-1`），见 §8.7-§8.8 |
| F16 高风险余项（inventory PDA 扫码 + maintenance 4 步向导） | F16 territory | inventory PDA 硬件 Non-Goal（归 Barcode/PDA cross-cutting successor）；maintenance 向导 BLOCKED（F4 child-table-editor 基线缺失） |
| F16 P2（hr 薪酬/组织 + logistics 时间线 + b2b EDI/ASN + contract diff + drp 报表） | F16 territory | ✅ 已落地（plan `2026-07-22-1400-2`），见 §8.9-§8.12 |

## 5. wizard 范式占位（待 successor 落地后回填）

Nop view.xdef 已内置 `<wizard>` 元素（`xview.xdef:177-192`），支持 `<step page="..." title="..."/>` 多步骤。但 wizard 范式在本仓库**尚未在任何计划落地**——AMIS wizard 组件 + step-state 管理 + 步骤间状态守卫均未在生产代码中验证。

**首个 wizard 落地建议**：选择 ErpFinAccountingPeriod 期末结账向导 successor plan 作为 PoC，但需先完成：
1. 后端 mutation 重构（roadmap 描述的 5 步独立 mutation `closeCostTransfer/closeFx/closePnl/reviewVoucher/closePeriod` 当前不存在，closePeriod 内部一次性多步执行）
2. 财务保护区域人工审查（AGENTS.md AI 阻塞条件）
3. AMIS wizard PoC + 步骤间数据持久化 + 回退/前进守卫

PoC 落地后回填本节，包含：
- step-state 管理机制（前端 vs 后端）
- 步骤间数据校验位置
- 回退守卫（已确认步骤不可回退到未完成步骤）
- wizard 与 `<pages><tabs>` 的关系（wizard 是 step + 顺序约束的 tabs）

## 6. 反模式自检表

| 不要这样写 | 应该这样写 |
|-----------|-----------|
| 用 `<pages><tabs>` 包装单实体 form（多此一举） | 单实体多 group 用 `<form layoutControl="tabs">` |
| 用 page.yaml 独立 AMIS tabs 绕过 view.xml 抽象 | 优先 view.xml `layoutControl="tabs"`；仅跨实体多 crud 才用 page.yaml |
| 在 form tabs 内移除 `<cell id="lines">` 的 sub-grid-edit | 保持 `<cell id="lines"><view path=... grid="sub-grid-edit|sub-grid-view"/></cell>` 不变，tabs 自动包装 |
| 把 F9 关联单据 drawer 改为嵌入 tab | 保持 F9 row-action drawer 独立；本范式与 F9 正交可叠加 |
| 在 wizard 范式 PoC 落地前直接在财务保护区域手写 AMIS wizard JSON | 先完成后端 mutation 重构 + 人工审查 + wizard PoC，再回填 §5 |
| 把敏感字段脱敏放在 view.xml layout 隐藏（layout 移除 = 隐藏） | layout 移除仅前端不可见，sql 直查仍可见；完整脱敏需后端 `@Sensitive` 或 BizModel `@BizLoader` mask transformer |
| 仪表板时间线直接 N+1 查询（每行 visit 单独拉 sparePartUsage） | 用后端 `@BizQuery` 聚合或 GraphQL `findPage` + `gql:selection` 批量预取 |
| 跨实体 crud grid 列引用关联实体字段但不加 `custom="true"` | 加 `custom="true"` + 显式 `domain="..."` 绕过 `ERR_GRID_COL_NOT_PROP`（头实体 objMeta 不含关联实体字段） |
| ref-*.page.yaml drawer 未配 `mountOnEnter=true` 导致初始 N+1（所有 tab 同时拉数据） | `<tabs mountOnEnter="true" unmountOnExit="false">` 仅在切到 tab 时拉数据，且保留 DOM 状态 |
| 跨域 GraphQL selection 用不存在的 filter_ 字段（如 `filter_sourceEntityType=ASSET` 在 ErpFinVoucherBillR 上不存在） | 先核实目标实体实际字段；不可用则降级同域单 tab 或归跨域集成 successor |
| `@BizQuery` 返回 `List<Map>` 直接用于 `<crud>` table api（非分页） | crud table api 必须用 `findPage`（分页）；`List<Map>` 适合报表 service+html 模式 |
| 在 ref-*.page.yaml 用 `x: gen-extends:`（冒号后有空格）导致 YAML 解析失败 | 用 `x:gen-extends:`（`x:` 是命名空间前缀，与 `gen-extends` 之间无空格） |

## 7. 参考

- `nop-entropy/docs-for-ai/02-core-guides/view-and-page-customization.md` — view.xml 三层模型与 `<pages>` 结构
- `nop-entropy/docs-for-ai/02-core-guides/page-dsl-pattern-catalog.md` — AMIS tabs / wizard / dashboard DSL 模式目录
- `nop-entropy/docs-for-ai/03-runbooks/build-tabs-workspace-page.md` — `<pages><tabs>` 工作台页面构建 runbook
- `nop-entropy/nop-job/.../NopJobSchedule.view.xml` — 真实样例（机制 B `<tabs>` + `<simple>` + `<crud>`）
- `docs/design/child-table-editor-patterns.md` — F4 子表编辑范式（与本范式正交）
- `docs/design/cross-doc-navigation-patterns.md` — F9 关联单据 drawer 范式（与本范式正交）
- `docs/design/notify/inbox-patterns.md` — page.yaml + AMIS tabs 真实样例（跨实体 drawer）

## 8. F16 复杂页面范式（低风险批，plan `2026-07-22-0845-2`）

固化 5 类「非标准 CRUD、但后端就绪且不需 custom AMIS 组件」的复杂页面范式。每类含 Phase 0 Explore PoC 结论 + 反模式条目。

### 8.1 实时聚合 / 实时校验（finance 凭证录入）

**场景**：头-行单据需要行录入时实时重算头合计（如凭证 totalDebit/totalCredit）。

**PoC 结论（Phase 0 Explore (a)）**：Nop input-table 内**行级 `onEvent.change → setValue` 可触发**（ErpFinVoucherLine sub-grid-edit 已大量使用，写 row-scope 字段）。但**跨行→头合计聚合**受 xview schema 约束：`<view>` 仅允许 `<data>` 子节点，行 onEvent 运行于 row scope，无法干净地写头级字段。

**落地范式（graceful fallback）**：
- 头合计刷新由独立 cell 按钮（`autoBalance`）的 `onEvent.click → doAction(setValue, {totalDebit, totalCredit})` 承担（计算 `event.data.lines` SUM）
- 实时**可见性**由头合计旁的 `balanceBadge` gen-control tpl 提供（`${Number(totalDebit)==Number(totalCredit) ? "平衡" : "不平衡"}`，label-success/label-danger）
- per-keystroke 头聚合（行 onEvent 直接写头）归 successor

**反模式**：
| 不要这样写 | 应该这样写 |
|-----------|-----------|
| 期望 input-table 行 onEvent 直接写头字段（scope 隔离） | 用独立 cell 按钮计算 + doAction(setValue) 写头；行 onEvent 仅写 row-scope 派生字段 |
| 在 balanceBadge 用 `${totalDebit == totalCredit}`（string 比较，NaN 风险） | 用 `${Number(totalDebit) == Number(totalCredit)}` 数值比较 |

### 8.2 表达式引擎预览（finance 凭证模板）

**场景**：凭证模板行 `amountExpression` 支持 `${placeholder}` + 算术（`DOC_TOTAL * 0.13`），前端预览生成凭证。

**PoC 结论（Phase 0 Explore (b)）**：既有 `ErpFinTemplateAcctDocProvider.resolveAmount` 仅支持 amountKey 查找 + 字面 BigDecimal，算术抛 `ERR_AMOUNT_KEY_NOT_RESOLVED`。扩展过账 provider 触及财务保护区域（高风险）。

**落地范式（候选 (c)，隔离 mutation）**：
- 新增 `@BizMutation renderTemplate(businessType, context)` 到 `ErpFinVoucherTemplateBizModel`（**不动过账引擎**）
- 内部最小安全算术求值器：BigDecimal 四则 + 括号 + 一元负号 + 变量引用，白名单字符集，无反射/无代码执行，除零/未定义变量/非法字符抛 NopException
- 前端 dialog 收集 context（DOC_TOTAL）→ 调 mutation → 预览 input-table → 「应用」按钮 doAction(setValue,{lines}) + closeDialog 写回头表单
- 算术进真实过账引擎归 successor（高级特性）

**反模式**：
| 不要这样写 | 应该这样写 |
|-----------|-----------|
| 在过账 provider 直接 eval 用户表达式（财务保护区域 + 注入风险） | 新增隔离 `@BizMutation` 预览方法，最小安全求值器白名单字符 |
| 用 `new BigDecimal(expr)` 处理算术（抛 NumberFormatException） | 先尝试字面量，失败走递归下降求值器 |

### 8.3 多 doc 联查（purchase 三单匹配）

**场景**：采购订单↔入库单↔发票三表联查，差异高亮，容差可视化。

**PoC 结论（Phase 0 Explore (c)）**：`findThreeWayMatchDiffAlert` 返回扁平非分页 `List<Map>`（无 3-doc join 后端）。3-crud 并列候选最可行（各用标准 findPage + filter_supplierId）。

**落地范式（候选 (a)，独立 page.yaml）**：
- 顶部差异预警 crud（消费 `findThreeWayMatchDiffAlert`，adaptor 转 `{items, count}`；varianceType 红色 tpl 标签 = 差异高亮；容差阈值进度条 tpl = 容差可视化）
- 共享 supplierId 过滤 form（reload 全部 crud）
- 下方 3 个并列 crud（grid 容器内，ErpPurOrder/ErpPurReceive/ErpPurInvoice 各 findPage + filter_supplierId）= 三表并列对比
- 菜单接入 `erp-pur.action-auth.xml`（归看板分组）

**反模式**：
| 不要这样写 | 应该这样写 |
|-----------|-----------|
| 对 `List<Map>` 非分页返回直接用作 crud table api | adaptor 转 `{items, count}` 模拟分页结构 |
| 跨 crud 行级自动匹配（需后端 join） | 顶部预警 crud 已标记差异行 + 三表并列供人工比对 |

### 8.4 进度仪表板（manufacturing 工单进度）

**场景**：工单详情页展示阶段进度条 + 颜色阈值高亮。

**落地范式（前端组装既有头字段，view form 新增 progress tab）**：
- `workOrderProgress` cell gen-control tpl：完工进度条（`completedQuantity/plannedQuantity*100`，inline style width；颜色阈值 绿≥90%/黄70-90%/红<70%）+ 报废率进度条（红色）+ 状态阶段标签
- pick/report 阶段明细经既有 F9 row-action drawer 查看，不在详情页聚合（避免后端查询）
- 不动后端，纯前端组装

**反模式**：
| 不要这样写 | 应该这样写 |
|-----------|-----------|
| 进度计算放后端 `@BizQuery`（若头字段已够） | 头字段（plannedQuantity/completedQuantity）可直接前端 tpl 计算 |
| tpl 百分比用 `${completedQuantity/plannedQuantity}` 不判空 | `${plannedQuantity ? (completedQuantity/plannedQuantity*100) : 0}` 防 NaN |

### 8.5 嵌入子表 + 效果验证（quality NCR 详情）

**场景**：NCR 详情页内嵌 CAPA（ErpQaAction）子表 + 效果验证 section。

**落地范式（to-many 关系 + sub-grid-view，view form 转 tabs）**：
- ErpQaNonConformance view form 加 `layoutControl="tabs"`，新增 `capa` tab 含 `actions` cell（`<view path=... grid="sub-grid-view"/>`）
- CAPA 经 `ErpQaNonConformance.actions` to-many 关系（orm）随头 gql:selection 嵌套加载（FK 字段名为 `ncrId`）
- ErpQaAction.view.xml 新增 `sub-grid-view` grid（含 verificationPerson/verificationDate 列）
- `verification[效果验证]` tab：复用 NCR 既有 resolvedBy/resolvedAt/resolution + CAPA 子表 verification 列。**实体无独立 verification 字段时不修改 ORM**（经 `_ErpQaNonConformance.java` 核实）

**反模式**：
| 不要这样写 | 应该这样写 |
|-----------|-----------|
| 为 verification section 新增 ORM 字段（保护区域） | 复用既有 resolvedBy/resolvedAt/resolution + CAPA 子表 verification 列 |
| 草稿用 `filter_nonConformanceId`（字段名臆测） | 经实时仓库核实实际 FK 字段名（此处为 `ncrId`） |

### 8.6 快捷模板 toolbar（凭证录入）

**场景**：凭证录入 toolbar 一键按模板生成分录行。

**落地范式**：edit form 新增 `quickTemplate` cell button（`actionType:dialog`）：dialog 内 form 收集 businessType + DOC_TOTAL context → 调 §8.2 `renderTemplate` mutation → adaptor 转 `{previewLines}` → 预览 input-table → 「应用到凭证」按钮 `doAction(setValue,{lines:${previewLines}})` + `closeDialog` 写回头表单。AMIS setValue 于 dialog action 内写 dialog data scope，closeDialog 合并回父表单。

### 8.7 甘特图（aps 排产，echarts custom series 只读）

**场景**：排产甘特图——Y=工作中心 category 轴，X=时间轴（dataZoom 缩放），每道工序工单=一根甘特条（plannedStart→plannedEnd），颜色编码 status。

**Phase 0 Explore (a) PoC 结论**：(1) Nop AMIS `type:chart` adaptor **可承载 echarts custom series**——qa dashboard 已证明 adaptor 返回 config 中的 JS 函数（`tooltip.formatter`/`renderItem`）被 AMIS 原样合并进 echarts option，不经 JSON 序列化。custom series 是 echarts 标准 series 类型，无需新平台机制。(2) **关键 schema 核实**：`_ErpApsOperationOrder` **无 scheduleId 字段**——经 `workOrderId`/`machineId`/`plannedStartDateT`/`plannedEndDateT`/`status` 表达，设计文档 `scheduling.md §8.3` 的 `IEtpApsGanttService`（含拖拽 `dragUpdateOperation` + 冲突报告）是**未来全实现 spec**，本范式仅落地只读甘特。(3) 数据量：按 date-between + machineId 过滤后典型 ≤ 200 行，客户端 reduce 分组性能充裕。

**落地范式（候选 (b)，前端 adaptor + 不新增后端 delta）**：
- 独立 page.yaml 顶部 form（machineId + status + dateRange）+ `type:chart`
- API：`ErpApsOperationOrder__findPage`（`filter_machineId` + `filter_status` + `limit` 500 + orderBy plannedStartDateT ASC），adaptor 客户端按 dateRange 过滤 + 按 machineId 分组建 Y category
- adaptor 构建 custom series data：`{value:[startMs, endMs, categoryIndex], itemStyle:{color: statusColor}, _op:{...meta}}`；`renderItem(params, api)` 返回 `{type:'rect', shape:{x: api.coord([v0,cat])[0], y: start[1]-h/2, width: max(end-start, 2), height: h}}`
- xAxis `type:'time'`，dataZoom slider+inside；tooltip.formatter 读 `params.data._op` 展示 workOrderId/qty/duration/priority
- 颜色编码对齐 `scheduling.md §8.2`：DRAFT=灰/PLANNED=蓝/IN_PROGRESS=黄/FINISHED=绿/CANCELLED=红
- 拖拽 = Non-Goal（设计文档明确 + F13 先例裁决 AMIS service scope 拖拽不可行）

**反模式**：
| 不要这样写 | 应该这样写 |
|-----------|-----------|
| 按草稿 `filter scheduleId` 过滤（entity 无 scheduleId） | 经 `_ErpApsOperationOrder.java` 核实实际字段，用 machineId + plannedStartDateT dateRange |
| 在 adaptor 用 `new Date(plannedStartDateT)` 直接解析（空格分隔报错） | `.replace(' ','T')` 转 ISO 后 `new Date().getTime()` |
| custom series renderItem 不设 `encode:{x:[0,1],y:2}` | encode 声明 value 数组到 x(0,1)/y(2) 的映射，否则坐标错乱 |
| 期望 AMIS chart adaptor 返回值经 JSON 序列化（函数丢失） | adaptor 返回 config 中的 JS 函数被原样保留（qa dashboard 已验证） |

### 8.8 BOM 树（manufacturing，AMIS tree 重建）

**场景**：BOM 多级展开树浏览——选 BOM → 调 `explode` 多级展开 → AMIS tree 多级展开/折叠。

**Phase 0 Explore (b) PoC 结论**：(1) `IErpMfgBomBiz.explode` 返回**扁平** `List<BomExplosionNode>`（字段 materialId/quantity/operationId/sourceBomId/level/manufactured），`BomExpander` 返回 **pre-order DFS**（父节点先于子节点 add），phantom（bomType=20）已在展开时合并、本身不出现在结果中（前端无需特殊处理）。(2) AMIS `type:tree` 经 F10 tree-list 范式验证可行；本页面用独立 page.yaml 的 `type:tree` + adaptor 栈算法重建嵌套。

**落地范式（候选 (a)，adaptor level 栈算法 + 不新增后端 delta）**：
- 独立 page.yaml 顶部 form（bomId required + qty 可选 + useMultiLevel switch 默认 true）+ `type:service`（fetch explode）内嵌 `type:tree`
- API：`ErpMfgBom__explode`（`$bid:Long,$q:BigDecimal,$ml:Boolean`），adaptor 按 **level 栈算法**重建嵌套：对每个 node（pre-order），弹栈直到 `top.level < node.level`（top 即父），attach 到 `top.children`，push `{node, level}`；空 children 数组删除以让 AMIS tree 识别叶子
- 未填 bomId 时 explode 报错（requireBom）→ adaptor 探测 `payload.data.errors` 优雅返回 `{hasData:false}`，tpl 占位提示（**adaptor 必须吞 GraphQL error**，否则页面 console 报错）
- 节点 label 含物料ID + 制造件/采购件 tag + quantity；制造件可继续展开（有 children），采购件为叶子
- 工艺路线水平流向图 = Non-Goal（BomExplosionNode 含 operationId 但无前后序关系数据，归 routing successor）

**栈算法核心**（pre-order DFS 重建）：
```
roots=[]; stack=[]  // [{node, level}]
for n in flat:
  node = {label, value, children:[], ...meta}
  while stack and stack.top.level >= n.level: stack.pop()   // top 即父
  (stack.empty ? roots : stack.top.node.children).push(node)
  stack.push({node, level: n.level})
```

**反模式**：
| 不要这样写 | 应该这样写 |
|-----------|-----------|
| 假设 explode 返回嵌套结构（实为扁平 + level） | 按 level 栈算法重建；BomExpander javadoc 明确「扁平化」 |
| 为 phantom 节点做前端特殊处理 | BomExpander 已合并 phantom（本身不出现），前端无需处理 |
| service auto-fetch 空 bomId 导致 GraphQL error 冒泡到页面 | adaptor 探测 `payload.data.errors` 优雅返回空 + 占位提示 |
| 用臆测字段名（如 `nonConformanceId`） | 经实时仓库核实实际字段名（此处 FK 为 sourceBomId/level/materialId） |

### 8.9 汇总审批页（hr 薪酬核算审批，plan `2026-07-22-1400-2`）

**场景**：薪酬记录（ErpHrSalary）按期间过滤，前端聚合汇总卡片（人数/应发/社保/个税/实发）+ 明细 crud + 审批/发放 row-action。

**Phase 0 Explore 结论**：ErpHrSalary 实体经实时仓库核实：字段为 `grossSalary`/`netSalary`/`taxAmount`（非 grossAmount/netAmount/individualTax），期间为 `year`+`month` 双字段（非 payPeriod），双状态 `paymentStatus`（erp-hr/salary-payment-status）+ `approveStatus`（wf/approve-status）。**实体无 `departmentId` 字段**（有 `orgId` + `employeeId`），分组维度改用 orgId（业务组织）。

**实现期裁决（filter 语法）**：erp 域实体的 `findPage` **不支持 `filter_<field>` 简写参数**（`nop.err.graphql.undefined-field-arg`），也不支持 `limit`/`orderBy` 作为直接参数。统一改用 `query:{limit:N}` 内联 QueryBean + adaptor 客户端过滤（`rows.filter(function(r){return r.field==val;})`）。这对 demo 级数据量充裕，大表需后端 `@BizQuery` 聚合（successor）。

**落地范式（独立 page.yaml，service 聚合 + crud 明细）**：
- 顶部 form 年月选择（year input-number + month select）
- `type:service`（payrollSummary）：`ErpHrSalary__findPage(query:{limit:2000})` → adaptor 客户端 filter year/month + reduce 聚合（人数/grossSalary Σ/socialInsurance Σ/taxAmount Σ/netSalary Σ + 按 orgId 分组 table）
- 汇总卡片 = 5 个 `type:tpl` panel（grid 布局），数值从 service data scope 取
- 明细 `type:crud`：`ErpHrSalary__findPage(query:{limit:2000})`，adaptor 客户端 filter，行级 markPaid/voidSalary row-action（visibleOn paymentStatus，调 `__markPaid`/`__voidSalary` mutation）
- Excel 导出 = Non-Goal（平台导出归 successor）

**反模式**：
| 不要这样写 | 应该这样写 |
|-----------|-----------|
| 用臆测字段名（grossAmount/netAmount/individualTax/payPeriod） | 经实时仓库核实：grossSalary/netSalary/taxAmount/year+month |
| 假设 salary 有 departmentId 做「group-by 部门」 | 实体无 departmentId（有 orgId），分组改用 orgId 或经 employee 关联 |
| 用 `filter_<field>:val` 简写（erp 域实体不支持） | 用 `query:{limit:N}` 内联 + adaptor 客户端 filter |
| visibleOn 双引号嵌套（`"${x == "Y"}"` → YAML 解析冲突） | 用单引号包裹 YAML 值（`'${x == "Y"}'`） |

### 8.10 版本 diff 对比（contract 合同版本对比，plan `2026-07-22-1400-2`）

**场景**：合同版本（ErpCtContractVersion）两版本对比，差异高亮。

**Phase 0 Explore (a) PoC 结论（关键数据模型发现）**：`ErpCtContractVersion` 实体**仅存储 `content`（free-text 4000 字符 blob）+ 版本元数据**（versionNo/versionDate/status/approvedBy/approvedAt/isCurrent/remark/attachmentFileId）。**无结构化业务字段**（totalAmount/startDate/endDate/terms 在父实体 ErpCtContract 上，不在版本实体）。因此候选 (a) 的「逐字段数值 diff」**数据模型不支持**——是数据缺失，非 tpl 复杂度问题。

**落地范式（降级候选 (b)，元数据对比表 + content 并排）**：
- 顶部 form 选合同ID → `ErpCtContractVersion__findList(filter_contractId)` 加载版本列表 → select 下拉选两版本（`findSiblings` 是 protected helper 非 @BizQuery，用标准 findList）
- `type:service`（compareService）：`__get` 两版本 → adaptor 逐字段对比元数据（versionNo/versionDate/status/approvedBy/approvedAt/isCurrent/remark）生成 `{field, valueA, valueB, changed}` rows
- 元数据对比 `type:table`：变更行 `<span style="background:#fef9c3">` 黄色高亮 + changed 标签（label-warning/label-default）
- content 并排对比：两个 `type:grid` 列各含 `<pre>` 纯文本展示（非 diff 库，非 code-level 高亮）
- 结构化字段级 diff 需 ORM 变更（Non-Goal，归 successor）

**反模式**：
| 不要这样写 | 应该这样写 |
|-----------|-----------|
| 假设版本实体有结构化业务字段（amount/dates/terms）做数值 diff | 经实时仓库核实：ErpCtContractVersion 仅 content blob + 元数据；结构化字段在父实体 |
| 用 `findSiblings`（protected helper，非 @BizQuery） | 用标准 `ErpCtContractVersion__findList(filter_contractId)` |
| content diff 引入 code-level 高亮库（highlight.js） | 纯 `<pre>` 文本展示满足可读性（语法高亮库 = Non-Goal） |

### 8.11 分组折叠报表（drp 净需求计算报表，plan `2026-07-22-1400-2`）

**场景**：净需求明细（ErpDrpLine）按物料分组，每组含 Σ 公式可视化 + 明细行。

**Phase 0 Explore (c) PoC 结论**：AMIS crud **无原生行分组**组件（F13 矩阵表先例已证明 custom table 组装更可靠）。

**落地范式（候选 (b)，service 分组 + each section + 嵌套 table）**：
- 顶部 form 选 planId → `ErpDrpLine__findList(filter_planId)` + `ErpDrpPlan__get(id)`
- `type:service`（netReqService）：adaptor 按 materialId 分组 reduce（Σ safetyStock/forecastDemand/currentStock/allocatedQty/onOrderQty/netRequirement/suggestedQty per group）
- `type:each`（groups）：每物料一个 section —— tpl panel header（物料ID + 行数 + 净需求合计 + 建议补货）+ tpl Σ 公式可视化（`安全库存(X) + 预测需求(X) − 当前库存(X) + 已分配(X) − 在途(X) = 净需求(X)`，label-danger if >0）+ 嵌套 `type:table`（source: detailRows）
- 字段经核实全部正确（safetyStock/forecastDemand/currentStock/allocatedQty/onOrderQty/netRequirement/suggestedQty/replenishmentType）

**反模式**：
| 不要这样写 | 应该这样写 |
|-----------|-----------|
| 用 AMIS crud groupBy column（无原生支持） | service + adaptor 分组 + each section + 嵌套 table |
| Σ 公式硬编码数值（静态） | tpl 插值 `${totSafety} + ${totForecast} − ...` 动态展示分组聚合 |

### 8.12 流程步骤条（b2b ASN 流程跟踪，plan `2026-07-22-1400-2`）

**场景**：ASN（ErpB2bAsn）状态流程条——三活跃阶段 RECEIVED→MATCHED→RECEIVED_TO_STOCK + CANCELLED 终态，当前阶段高亮。

**Phase 0 / Draft Review 裁决**：字典 `erp-b2b/asn-status` 经实时仓库核实实际 **4 值**（RECEIVED/MATCHED/RECEIVED_TO_STOCK/CANCELLED）。roadmap 描述「五阶段」（含 VALIDATED/PENDING_RECEIPT）为**笔误传播**——VALIDATED/PENDING_RECEIPT 不存在于字典。本范式以实时仓库字典为准。

**落地范式（each+tpl 色块 + row-action dialog）**：
- ASN 列表 crud（`ErpB2bAsn__findPage(filter_status)`）+ row-action「查看流程」dialog
- dialog 内 `type:service`：`ErpB2bAsn__get` + `ErpB2bAsnLine__findList(filter_asnId)` → adaptor 计算阶段状态（done/current/todo per stage by order.indexOf(status)）
- 流程条 = `type:each`（flowSteps）每步 tpl 色块：current=蓝(`#3b82f6`,border 加粗)+done=绿(`#22c55e`)+todo=灰(`#f1f5f9`)
- 明细行匹配状态：ErpB2bAsnLine **无 poLineId/receiveLineId/matched-status 字段**（经核实），改用 `quantity`（订单）vs `shippedQty`（发货）对比 → full/partial/unshipped 标签

**反模式**：
| 不要这样写 | 应该这样写 |
|-----------|-----------|
| 按 roadmap「五阶段」建模（VALIDATED/PENDING_RECEIPT 不存在） | 以实时仓库字典 `asn-status.dict.yaml` 为准（4 值） |
| 假设 ErpB2bAsnLine 有 matched-status 字段 | 经核实仅有 quantity/shippedQty，匹配状态经两者对比推断 |
| 用 AMIS `type:steps`（prop 契约可能不稳） | each+tpl 色块（与 F13 拖拽降级先例一致的 custom JSON 组装） |
