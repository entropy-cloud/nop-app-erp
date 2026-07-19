# ERP 状态标签着色设计 (F5)

> Status: completed (Phase 0–3 全部落地)
> Owner: docs/design/status-color-map.md (单一真相源)
> Plan: `docs/plans/2026-07-19-1818-3-f5-status-tag-coloring.md`

## 1. 目的与范围

为 ERP 18+1 业务域的所有主要业务实体的状态列（`docStatus` / `approveStatus` / 业务专用 `status`）建立统一的颜色映射，将 codegen 默认的纯文本渲染升级为彩色 label span。

**范围**：列表页 (`<grid id="list">`) 状态列；不包含详情页表单（详情页字段渲染另由 F12 处理）、进度条/状态流转图（F12 范畴）。

**覆盖实体总数**：68 个（Phase 1 核心 4 域 26 个 + Phase 2 扩展 14 域 42 个），其中 master-data 9 / purchase 6 / sales 6 / inventory 2 / finance 12 / mfg 6 / projects 9 / quality 8 / maintenance 6 / crm 3 / cs 2 / hr 11 / aps 2 / logistics 1 / b2b 3 / contract 5 / drp 3。

## 2. Phase 0 决策表

### Decision (a): AMIS 渲染器选型

| 选项 | 优势 | 劣势 | 决策 |
| --- | --- | --- | --- |
| `type=tag` + colorMap | AMIS 原生 tag 组件 | Nop 集成 AMIS 未见先例；colorMap 仅支持静态映射 | ✗ |
| `type=mapping` + className | 静态 map 支持自定义 HTML | 不支持基于 field 值的动态 className | ✗ |
| `type=tpl` + 三元表达式 | 支持 `${expr ? a : b}` 三元；HTML 直出；`${field}` 字段引用 | 表达式较长 | **✓ 选用** |
| `type=static-mapping` | 已用于 `view-boolean` | map value 静态，不支持动态 className | ✗ |

**证据**:
- `_dump/nop-app/nop/web/xlib/control.xlib:770` `view-boolean` 使用 `static-mapping` + HTML span（证明 HTML 直出可行）
- `_dump/nop-app/nop/web/xlib/control.xlib:336-348` `view-enum` 默认渲染为 `{type:"static", name:"status_label"}` 纯文本（证明需要替换）

### Decision (b): 颜色映射单一真相源 + 引用机制

**Explore (c) 证实**：`<gen-control>` 内容为 `xpl-xjson` 域，XPL 解析器在该上下文中拒绝未识别的 namespace 标签（`nop.err.xlang.xpl.not-allow-unknown-tag`），即 `<statusTag:Tag xpl:lib="/erp/common/xlib/status-tag.xlib"/>` 不可用。`<c:import>`/`x:include` 同样不适用（gen-control 期望 XPL 表达式而非声明）。

**决策**：放弃共享 AMIS 片段，每域 `view.xml` 直接 inline 颜色映射（`<gen-control><c:script>...</c:script></gen-control>`）。本文件 `§3 颜色映射权威表` 作为唯一文档真相源，每域 view.xml 的 gen-control 复制该映射。**nop-entropy 平台 `ui:statusLabel` xmeta 原生属性扩展**登记为 successor（见 plan Deferred 节）。

**Explore 证据 file:line**:
- `/Users/abc/app/nop-app-erp/_dump/nop-app/nop/schema/xui/disp.xdef:61` `<gen-control>xpl-xjson</gen-control>`（domain 类型）
- `/Users/abc/app/nop-app-erp/_dump/nop-app/nop/web/xlib/web.xlib:454-466` gen-control eval 上下文：仅暴露 `dispMeta/propMeta/editMode/bizObjName/objMeta/mandatory` 变量，未暴露 xlib namespace 绑定
- `/Users/abc/app/nop-app-erp/_dump/nop-app/nop/web/xlib/control.xlib:770` view-boolean 证明 `<c:script>` 内 `return { type: ... }` 模式可行
- 实测：`<statusTag:Tag xpl:lib="/erp/common/xlib/status-tag.xlib"/>` 报 `not-allow-unknown-tag`；改用 `<c:script>` inline `return { type: "tpl", ... }` 后 `ErpAllWebPagesTest` 通过

### Decision (c): 状态字典覆盖范围

本计划纳入颜色映射的字典清单（Phase 1+2 全覆盖）：

**通用字典**（每域 `erp-<short>/doc-status.dict.yaml` + `approve-status.dict.yaml` 复制相同值集）：
- `doc-status`: `DRAFT` / `ACTIVE` / `CANCELLED`
- `approve-status`: `UNSUBMITTED` / `SUBMITTED` / `APPROVED` / `REJECTED`
- `active-status` (master-data): `ACTIVE` / `INACTIVE`

**域专用进度字典**：
- `receive-status` (purchase): `NOT_RECEIVED` / `PARTIAL` / `RECEIVED`
- `paid-status` (purchase/finance): `UNPAID` / `PARTIAL` / `PAID`
- `deliver-status` (sales): `NOT_DELIVERED` / `PARTIAL` / `DELIVERED`

**长尾低频字典**（Phase 2 显式 defer 清单见 plan）。

### Decision (d): 组合状态显示策略

**决策**：列表页保持双标签并列（`docStatus` 列 + `approveStatus` 列各自渲染），不合并为单一标签。

**理由**：
- 信息保真：`docStatus=ACTIVE, approveStatus=REJECTED` 与 `docStatus=ACTIVE, approveStatus=APPROVED` 含义不同
- 与 `docs/design/purchase/ui-patterns.md` 现有约定一致
- codegen 已默认生成两列，无需额外列删除/合并

## 3. 颜色映射权威表

### 3.1 通用 docStatus (`erp-<short>/doc-status.dict.yaml`)

| 值 | CSS class | 颜色 | 视觉 |
| --- | --- | --- | --- |
| `DRAFT` | `label label-default` | #999 灰 | 灰底浅文 |
| `ACTIVE` | `label label-primary` | #1890ff 蓝 | 蓝底白文 |
| `CANCELLED` | `label label-default` + `style='text-decoration:line-through'` | #999 灰 + 删除线 | 灰底删除线 |

### 3.2 通用 approveStatus (`erp-<short>/approve-status.dict.yaml`)

| 值 | CSS class | 颜色 |
| --- | --- | --- |
| `UNSUBMITTED` | `label label-default` | #999 灰 |
| `SUBMITTED` | `label label-info` | #1890ff 蓝 |
| `APPROVED` | `label label-success` | #52c41a 绿 |
| `REJECTED` | `label label-danger` | #ff4d4f 红 |

### 3.3 master-data active-status (`erp-md/active-status.dict.yaml`)

| 值 | CSS class | 颜色 |
| --- | --- | --- |
| `ACTIVE` | `label label-success` | #52c41a 绿 |
| `INACTIVE` | `label label-default` | #999 灰 |

### 3.4 业务专用进度状态 (receiveStatus / paidStatus / deliverStatus)

| 值 | CSS class | 颜色 |
| --- | --- | --- |
| `*_RECEIVED` / `PAID` / `DELIVERED` / `COMPLETED` / `SETTLED` | `label label-success` | 绿 |
| `PARTIAL` / `IN_PROGRESS` | `label label-primary` | 蓝 |
| 其他 (`NOT_*` / `UNPAID` / `NOT_STARTED`) | `label label-default` | 灰 |

### 3.5 Smart 启发式映射（用于 14 扩展域的域专用 status）

为兼顾 14 扩展域的多种域专用 status dict（如 erp-mfg/job-card-status、erp-prj/project-status、erp-qa/action-status 等），引入 Smart 启发式：将常见状态值词根映射到颜色，无需为每域字典单独写映射。

| 颜色 | 包含的值（部分清单） |
| --- | --- |
| `success` (绿) | COMPLETED, APPROVED, RECEIVED, DELIVERED, PAID, SETTLED, HONORED, RETRIED, EXECUTED, COMPUTED, CONFIRMED, RUNNING, MATERIAL_TRANSFERRED, ACTIVE, CLOSED, DONE, SUCCESS |
| `danger` (红) | REJECTED, CANCELLED, FAILED, DISHONORED, OVERDUE, DOWN, TERMINATED, WRITE_OFF |
| `warning` (黄) | ON_HOLD, SUSPENDED, EXPIRED, RETRYING |
| `primary` (蓝) | SUBMITTED, IN_PROGRESS, PARTIAL, PARTIALLY_TRANSFERRED, WORK_IN_PROGRESS, PROCESSING, NEGOTIATION, OPEN, UNDER_MAINTENANCE, COLLECTION_PENDING, DISCOUNTED, ENDORSED, ISSUED |
| `default` (灰) | DRAFT, NEW, PLANNED, IDLE, DECOMMISSIONED, 其他未匹配值 |

### 3.6 Finance 域专用状态（custom 模板）

| 字段 / dict | 值 → CSS class |
| --- | --- |
| `posting-exception-status` | RETRIED=success, RETRYING/MANUAL=warning, 其他=default |
| `notes-payable-status` | HONORED=success, DISHONORED=danger, ISSUED=primary, 其他=default |
| `notes-receivable-status` | HONORED=success, DISHONORED=danger, RECEIVED/DISCOUNTED/ENDORSED/COLLECTION_PENDING=primary, 其他=default |

## 4. view.xml inline 引用范式

### 4.1 docStatus 列范式

```xml
<col id="docStatus" mandatory="true" sortable="true">
    <gen-control>
        <c:script><![CDATA[
            const valueProp = dispMeta?.id || 'docStatus';
            const labelProp = propMeta?.['graphql:labelProp'] || (valueProp + '_label');
            return {
                type: "tpl",
                tpl: '<span class="label label-${' + valueProp
                    + " == 'ACTIVE' ? 'primary' : 'default'}\" "
                    + "${" + valueProp
                    + " == 'CANCELLED' ? \"style='text-decoration:line-through'\" : ''}>"
                    + "${" + labelProp + "}</span>"
            };
        ]]></c:script>
    </gen-control>
</col>
```

### 4.2 approveStatus 列范式

```xml
<col id="approveStatus" mandatory="true" sortable="true">
    <gen-control>
        <c:script><![CDATA[
            const valueProp = dispMeta?.id || 'approveStatus';
            const labelProp = propMeta?.['graphql:labelProp'] || (valueProp + '_label');
            return {
                type: "tpl",
                tpl: '<span class="label label-${' + valueProp
                    + " == 'APPROVED' ? 'success' : "
                    + valueProp + " == 'REJECTED' ? 'danger' : "
                    + valueProp + " == 'SUBMITTED' ? 'info' : 'default'}\">"
                    + "${" + labelProp + "}</span>"
            };
        ]]></c:script>
    </gen-control>
</col>
```

### 4.3 active-status 列范式（master-data）

```xml
<col id="status" mandatory="true" sortable="true">
    <gen-control>
        <c:script><![CDATA[
            const valueProp = dispMeta?.id || 'status';
            const labelProp = propMeta?.['graphql:labelProp'] || (valueProp + '_label');
            return {
                type: "tpl",
                tpl: '<span class="label label-${' + valueProp
                    + " == 'ACTIVE' ? 'success' : 'default'}\">"
                    + "${" + labelProp + "}</span>"
            };
        ]]></c:script>
    </gen-control>
</col>
```

### 4.4 进度状态列范式（receiveStatus / paidStatus / deliverStatus）

```xml
<col id="receiveStatus" sortable="true">
    <gen-control>
        <c:script><![CDATA[
            const valueProp = dispMeta?.id || 'receiveStatus';
            const labelProp = propMeta?.['graphql:labelProp'] || (valueProp + '_label');
            return {
                type: "tpl",
                tpl: '<span class="label label-${' + valueProp
                    + " == 'RECEIVED' || " + valueProp + " == 'COMPLETED' ? 'success' : "
                    + valueProp + " == 'PARTIAL' || " + valueProp + " == 'IN_PROGRESS' ? 'primary' : 'default'}\">"
                    + "${" + labelProp + "}</span>"
            };
        ]]></c:script>
    </gen-control>
</col>
```

## 5. 渲染机制说明

1. **gen-control 覆盖默认控件**：view-gen 的 `GenGridCol`（`_dump/nop-app/nop/web/xlib/web.xlib:454-466`）若发现 col 上有 `<gen-control>`，则用其 XPL 返回值替换默认 control.xlib 推导（默认 `view-enum` → `{type:"static", name:"status_label"}` 纯文本）。
2. **`<c:script>` 返回 AMIS JSON**：XPL `c:script` 标签的最后一行 `return ...` 作为 gen-control 的 XJSON 输出。
3. **AMIS tpl 渲染**：`{type:"tpl", tpl:"<span class='...'>${field}</span>"}` 由前端 AMIS tpl renderer 求值，`${expr}` 支持 JS 三元表达式与字段引用。
4. **`*_label` 字段由 DictLabelFetcher 自动生成**：后端 GraphQL 响应自动包含 `status_label` 等派生字段，无需额外配置（见 `docs/analysis/2026-07-19-frontend-ui-design-completeness-and-quality-analysis.md §7.5`）。

## 6. 验证基线

- `mvn -pl app-erp-all test -Dtest=ErpAllWebPagesTest`：全量校验所有 page.yaml 可被 PageProvider 加载（含 gen-control XPL 求值），通过即证明 inline 范式语法正确。
- `mvn test`（154 模块全绿，仅 mfg-service 的 `TestErpMfgProductionVariance#testManualCalculateVariancesIdempotent` 为已知 flaky，DEL_VERSION 时间戳不一致，与本计划无关）。
- `npx playwright test tests/e2e/visual/status-tag.visual.spec.ts`：12 个用例（9 个 must-pass + 3 个 soft-probe）全绿，DOM className 断言证明 gen-control XPL 求值链路完整：
  - ErpPurOrder/ErpSalOrder/ErpFinVoucher/ErpMdMaterial/ErpMfgWorkOrder/ErpQaInspection 等 7 实体的状态列渲染 `<span class="label label-{success|primary|...}">`。
  - CANCELLED 行的 line-through 样式契约（best-effort）。
  - ErpPurOrder 双标签并列（docStatus + approveStatus）渲染验证。

**抽样 file:line 证据**（运行时 PageProvider__getPage GraphQL 接口返回的 AMIS tpl 字符串）：
- `/erp/pur/pages/ErpPurOrder/main.page.yaml` 列 [9] docStatus：`<span class="label label-${docStatus == 'ACTIVE' ? 'primary' : 'default'}" ${docStatus == 'CANCELLED' ? "style='text-decoration:line-through'" : ''}>${docStatus_label}</span>`
- `/erp/pur/pages/ErpPurOrder/main.page.yaml` 列 [10] approveStatus：`<span class="label label-${approveStatus == 'APPROVED' ? 'success' : approveStatus == 'REJECTED' ? 'danger' : approveStatus == 'SUBMITTED' ? 'info' : 'default'}">${approveStatus_label}</span>`
- `/erp/aps/pages/ErpApsOperationOrder/main.page.yaml` 列 [17] status (smart)：`<span class="label label-${['COMPLETED','APPROVED',...].indexOf(status) >= 0 ? 'success' : ...}">${status_label}</span>`

## 7. 长尾低频实体 Defer 清单

- **ErpHrShiftAssignment / ErpInvDrpDockAppointment**: status 字段无 dict（自由文本），defer。
- **ErpCrmLeadSequenceProgress / ErpHrDevelopmentPlanItem**: 子实体进度跟踪表，无独立 status dict，defer。
- **inventory 长尾**: ErpInvStockTake/CostAdjust/TransferOrder/PickingOrder/OwnershipTransfer 经核实 grid 未暴露 status 列（仅 ORM 内部状态字段），defer。
- **各 *_Line 子实体**: 行项目表无独立 status 列（继承自主单据状态），defer。
- **配置实体 isActive 字段**: ErpMdCurrency/ErpMdTaxRate 等的 isActive 经核实未在 grid 暴露（详情页字段），defer 至 F12 详情页结构增强。
- **ErpSysNotification (notify)**: 经核实 grid 未暴露 status 列（未读/已读在 UI 通过其他方式呈现），defer。

## 8. Successor 触发条件

- **nop-entropy 平台 `ui:statusLabel` xmeta 原生属性扩展**：当平台 schema 扩展提案被采纳时，本设计的 inline 范式可被替换为 xmeta prop 层 `ui:statusLabel="doc-status"` 声明，由 codegen 自动生成 gen-control。当前 inline 范式的复制成本（每 view.xml ~10 行 c:script）通过 `docs/design/status-color-map.md §3` 的颜色映射权威表 + `§4` 的 view.xml inline 引用范式集中管理。
- **F12 详情页状态进度条/状态流转可视化**：本计划仅做列表页标签着色；详情页结构增强属 F12 独立范围。
