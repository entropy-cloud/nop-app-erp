# ERP 字段格式化设计 (F6)

> Status: active
> Owner: docs/design/field-formatting-patterns.md (单一真相源)
> Plan: `docs/plans/2026-07-19-2200-2-f6-field-formatting-xmeta.md`

## 1. 目的与范围

为 ERP 18+1 业务域的金额、数量、单价、税率、汇率、百分比、日期、日期时间字段建立统一的显示格式化机制，将 codegen 默认的纯 BigDecimal / ISO 时间戳渲染升级为「千分位 + 固定小数位」或「友好日期格式」。

**范围**：
- 列表页 `<grid id="list">` 列
- 子表视图 `<grid id="sub-grid-view">` 行内列
- 主表单只读 `<form id="view">` 字段（cell）（依赖 gen-control，与列表同机制）

**不包含**：
- 编辑态表单字段（输入态保留原始值，避免千分位干扰输入）
- 报表 / 看板金额字段（nop-report 走 `formatExpr` 独立机制）
- 币种符号本地化（CNY ¥ / USD $，归 F15 i18n + l10n）
- 负数红字显示（会计专用，归 F5 状态色继承/successor）

## 2. Phase 1 决策表

### Decision (a): 格式化机制选型 — 方案 D（inline `<gen-control>` + `<c:script>`）

| 方案 | 描述 | 优势 | 劣势 | 决策 |
| --- | --- | --- | --- | --- |
| **A** | xmeta `<prop ui:format="...">` + codegen 透传 | 一次配置全场景生效 | nop-entropy xdef 无 `ui:format` 属性；需平台 codegen 改造（保护区域） | ✗ |
| **B** | view.xml `<col format="...">` 直接配置 | 简单 | **实测透传失败**：`flux-web.xlib:GenGridCol` 仅 pick 固定属性集（name/label/sortable/width/align/fixed/...），`format` 不在 pick 列表 | ✗ |
| **C** | codegen `domain → format` 全局映射 delta | 一改全改 | 需修改 nop-entropy `control.xlib` 添加 `list-view-amount`/`list-view-quantity` 等标签或定制 `controlLib`，影响面广 | ✗（successor） |
| **D** | view.xml `<col><gen-control><c:script>return {type:'number', kilometer:true, precision:N}</c:script></gen-control></col>` | 与 F5 范式一致；不依赖平台变更；每列自包含 | 每金额/数量/日期列需手写 view.xml delta override | **✓ 选用** |

**Explore (a) 证据**：
- `_dump/nop-app/erp/pur/pages/ErpPurOrder/main.page.yaml:2199-2213`：现状所有 amount/date 列均渲染为 `type: static`（无格式化、ISO BigDecimal 字符串直出）
- `/Users/abc/app/nop-entropy-wt/nop-entropy-master/nop-frontend-support/nop-web/src/main/resources/_vfs/nop/web/xlib/flux-web.xlib:438-481`（GenGridCol）：col 属性 pick 列表固定，`format`/`precision`/`kilometer` 不在 pick 范围
- 同上 `:466-469`：`if(colXpl != null){ control = eval(colXpl,...)}` 证明 gen-control 经 eval 后 putAll 到 col，可注入任意 AMIS 属性

**Explore (b) 证据**（xmeta `ui:format` 不存在）：
- `_dump/nop-app/nop/schema/xmeta.xdef` + `_dump/nop-app/nop/schema/schema/obj-schema.xdef`：xmeta `<prop>` 的 `<schema>` 元素仅支持 `domain/type/precision/scale/dict/...`，无 `ui:format` 属性声明
- `/Users/abc/app/nop-entropy-wt/nop-entropy-master/docs-for-ai/02-core-guides/orm-model-design.md §domain 表 line 452-470`：domain 属性列表无 `ui:format` 关联

**Explore (c) 证据**（codegen domain 推导逻辑）：
- `XuiHelper.java:148-188`（`_getControlTag`）：标签查找顺序为 `{mode}-{control}` → `{mode}-{domain}` → `{mode}-{baseDomain}` → `{mode}-{stdDomain}` → `{mode}-{relKind}` → `{mode}-{stdDataType}`
- `control.xlib`：现有标签列表（grep `^        <`）无 `list-view-amount` / `list-view-quantity` / `view-amount` / `view-quantity`，decimal/amount 字段在 list-view 模式下 fallback 到 `view-any`（仅渲染 `{type:"static"}`）
- 域专用标签需通过应用层 `controlLib` 自定义 xlib 注册（修改面广，归方案 C successor）

**Explore (d) 证据**（F5 inline gen-control 范式 + HR format= 先例）：
- `module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/ErpPurOrder/ErpPurOrder.view.xml:16-31`（F5 落地的 docStatus gen-control，证明 `return {type:"tpl", tpl:...}` 经 putAll 注入到 AMIS col）
- `module-hr/erp-hr-web/src/main/resources/_vfs/erp/hr/pages/ErpHrEmployee/ErpHrEmployee.view.xml:121-125` `<input-date format="YYYY-MM-DD">`：HR 用 form cell gen-control + `<input-date format=>` 嵌套控件方式；列表 grid col 不可直接复用（gen-control 包裹控件模式 vs col 直接 format 属性）
- 决策：列表 grid col 采用方案 D（c:script 返回 `{type:'date', format:'YYYY-MM-DD'}` / `{type:'number', kilometer:true, precision:N}`），与 F5 范式一致；form cell 编辑态保留 codegen 默认（`edit-date`/`edit-datetime` 已正确 format），本计划不动 form 编辑态

### Decision (b): 格式化映射表 — 按 ORM domain 与字段语义

| domain | ORM scale | 显示格式 | AMIS type | AMIS 关键属性 | 对齐 | 与 roadmap F6 偏差裁决 |
| --- | --- | --- | --- | --- | --- | --- |
| `amount` | 2-4 | 千分位 + 2 位小数 | `number` | `{kilometer:true, precision:2}` | 右对齐 | 一致 |
| `quantity` | 4 | 千分位 + 4 位小数 | `number` | `{kilometer:true, precision:4}` | 右对齐 | 偏差：roadmap 给 `#,##0.###`（3 位）；本计划按 ORM scale=4 选 4 位避免丢精度 |
| `unitPrice` | 4 | 千分位 + 4 位小数 | `number` | `{kilometer:true, precision:4}` | 右对齐 | 一致 |
| `taxRate` | 4 | 4 位小数（无千分位） | `number` | `{precision:4}` | 右对齐 | 偏差：roadmap 给 `0.0000`；税率值通常 < 1 千分位无意义，采纳 roadmap 不带千分位 |
| `taxAmount` | 2-4 | 千分位 + 2 位小数 | `number` | `{kilometer:true, precision:2}` | 右对齐 | 一致 |
| `exchangeRate` | 8 | 千分位 + 8 位小数 | `number` | `{kilometer:true, precision:8}` | 右对齐 | roadmap 未明确；ORM scale=8 保留 8 位避免丢精度 |
| `percentage`（如 discountRate） | 2 | 百分比 + 2 位小数 | `number` | `{percent:true, precision:2}` | 右对齐 | 一致（注：DB 存储小数 1.50 → 显示 `1.50%`） |
| `date`（如 businessDate） | - | 短日期 | `date` | `{format:'YYYY-MM-DD'}` | 居中 | 一致（含 HR 3 处既有先例） |
| `dateTime`（如 createTime） | - | 长日期时间 | `datetime` | `{format:'YYYY-MM-DD HH:mm:ss'}` | 居中 | 一致 |

**field-name 启发式**（用于识别未声明 domain 的字段）：
- `*Rate` / `*Percentage` / `discountRate` / `rate`：percent 或 taxRate 域
- `*Amount` / `*Money` / `*Cost` / `*Price` / `*Value`：amount 域
- `*Quantity` / `*Qty` / `*Count`：quantity 域
- `*Date` / `business*Date` / `delivery*Date` / `invoice*Date`：date 域
- `*Time` / `*At`（approvedAt/postedAt/createTime/updateTime）：dateTime 域

**精度漂移防范**（plan Phase 2 Fix 项）：
- 若 AMIS `type:'number'` 实际渲染四舍五入丢精度，备选 `type:'static-input-number'` + `precision` + `kilometer:true`
- 若 AMIS tpl 过滤器（`${val|toFixed:2|comma}`）渲染异常，备选 `type:'tpl'` + 表达式（残留风险低，AMIS number 控件为标准 CRUD 列类型）

### Decision (c): 列级 align 配置

`<col>` 的 `align` 属性会被 codegen pick（`flux-web.xlib:444` `align: colModel.align || ...`），所以无需 gen-control 包裹，直接在 col 上加 `align="right"`（金额/数量）或 `align="center"`（日期）。

### Decision (d): 实施粒度与覆盖范围

按 F5 实际落地经验（68 实体 ~150 列），本计划承诺：
- **Phase 2**：核心 4 域（purchase/sales/inventory/finance）头实体 + 主 Line 实体的金额/数量/单价/税率字段
- **Phase 3**：汇率/百分比/日期/日期时间字段（全域）
- **Phase 4**：13 扩展域剩余金额/数量/日期字段
- **长尾字段**（低频、非业务核心）显式 defer 到 successor，记录于 plan

## 3. view.xml inline 引用范式

### 3.1 金额列（amount）

```xml
<col id="totalAmountWithTax" sortable="true" align="right">
    <gen-control>
        <c:script><![CDATA[
            return {
                type: 'number',
                kilometer: true,
                precision: 2
            };
        ]]></c:script>
    </gen-control>
</col>
```

### 3.2 数量列（quantity）

```xml
<col id="quantity" mandatory="true" sortable="true" align="right">
    <gen-control>
        <c:script><![CDATA[
            return {
                type: 'number',
                kilometer: true,
                precision: 4
            };
        ]]></c:script>
    </gen-control>
</col>
```

### 3.3 税率列（taxRate，无千分位）

```xml
<col id="taxRate" align="right">
    <gen-control>
        <c:script><![CDATA[
            return { type: 'number', precision: 4 };
        ]]></c:script>
    </gen-control>
</col>
```

### 3.4 百分比列（discountRate）

```xml
<col id="discountRate" align="right">
    <gen-control>
        <c:script><![CDATA[
            return { type: 'number', percent: true, precision: 2 };
        ]]></c:script>
    </gen-control>
</col>
```

### 3.5 汇率列（exchangeRate，8 位小数）

```xml
<col id="exchangeRate" align="right">
    <gen-control>
        <c:script><![CDATA[
            return { type: 'number', kilometer: true, precision: 8 };
        ]]></c:script>
    </gen-control>
</col>
```

### 3.6 日期列（date）

```xml
<col id="businessDate" mandatory="true" sortable="true" align="center">
    <gen-control>
        <c:script><![CDATA[
            return { type: 'date', format: 'YYYY-MM-DD' };
        ]]></c:script>
    </gen-control>
</col>
```

### 3.7 日期时间列（dateTime）

```xml
<col id="createTime" sortable="true" align="center">
    <gen-control>
        <c:script><![CDATA[
            return { type: 'datetime', format: 'YYYY-MM-DD HH:mm:ss' };
        ]]></c:script>
    </gen-control>
</col>
```

## 4. 渲染机制说明

### 4.1 codegen 展开链路

```
view.xml <col> + <gen-control>
   ↓ flux-web.xlib:GenGridCol (line 416-483)
   ↓ eval(colXpl, {dispMeta, propMeta, editMode, ...}) → control 对象
   ↓ col.putAll(control) → 合并到 AMIS col JSON
   ↓ _dump/.../main.page.yaml → AMIS CRUD columns[]
```

### 4.2 与 F5 状态标签范式的对比

| 项 | F5 状态标签 | F6 字段格式化 |
| --- | --- | --- |
| gen-control 返回 | `{type:'tpl', tpl:'<span class="label ${...}">${...}</span>'}` | `{type:'number', kilometer:true, precision:N}` 或 `{type:'date', format:'YYYY-MM-DD'}` |
| AMIS 列类型 | tpl（HTML 直出） | number / date / datetime（AMIS 原生 CRUD 列控件） |
| 列属性 align | col 继承默认 | 在 col 上显式 `align="right"`/`center` |

### 4.3 编辑态保留 codegen 默认

`<form id="edit">` / `<form id="add">` / sub-grid-edit 中的 `<cell>` / `<col>` 保留 codegen 默认控件（`edit-decimal` 已设 `precision: scale`，`edit-date` 已设 `format:"YYYY-MM-DD"`）。本计划仅覆盖只读展示态。

## 5. 反模式自检表

| 不要这样写 | 应该这样写 |
| --- | --- |
| 在 col 上加 `format="#,##0.00"`（不被 codegen pick） | 用 `<gen-control><c:script>return {type:'number', kilometer:true, precision:2}</c:script></gen-control>` |
| 在 xmeta `<prop>` 上加 `ui:format`（xdef 不支持） | 在 view.xml col 上加 `<gen-control>` |
| 修改 nop-entropy `control.xlib` 加 `list-view-amount` 标签 | 用应用层 view.xml delta override + gen-control |
| 修改 `_gen/_<Entity>.view.xml`（生成物，重生成被覆盖） | 改保留层 `<Entity>.view.xml`（`x:extends="_gen/_<Entity>.view.xml"` + `bounded-merge`） |
| 给所有字段都加 gen-control（含 id/code/name 等非数值字段） | 仅对 amount/quantity/date 等需要格式化的字段加 |
| gen-control 返回 `{type:'static', tpl:'${...}'}` 用 tpl 自定义数字格式 | 用 AMIS 原生 `type:'number'`/`type:'date'`（保证 i18n + 可访问性） |
| 忘了加 `align="right"`（金额/数量右对齐） | col 上同时设 `align="right"`（金额/数量）或 `align="center"`（日期） |
| 编辑态表单也加 gen-control（干扰输入） | 仅 list / sub-grid-view / view 表单；edit/add 表单保留 codegen 默认 |
| 修改 ORM 模型 precision/scale（保护区域） | 仅改 view.xml 展示态精度，ORM 存储精度不动 |
| 子表行 amount 列忘了 `<grid id="sub-grid-view">` 也加格式化 | list 与 sub-grid-view 两份 grid 均需覆盖 |

## 6. 验证基线

### 6.1 编译期验证

- `mvn clean install -DskipTests`（154 模块 BUILD SUCCESS）
- `mvn -pl app-erp-all test -Dtest=ErpAllWebPagesTest` 全绿（PageProvider.validateAllPages 校验所有 page.yaml 加载，含所有 gen-control XPL 求值链路）
- `_dump/nop-app/erp/<short>/pages/<Entity>/main.page.yaml` grep `kilometer: true` / `type: number` / `type: date` 抽样核实

### 6.2 运行时验证（启动 app + 浏览器）

抽样 4 域列表页：
- purchase: ErpPurOrder 列表 totalAmountWithTax 显示 `123,456.78` 而非 `123456.78`
- sales: ErpSalOrder 列表 totalAmountWithTax 同
- inventory: ErpInvStockMove 数量字段千分位
- finance: ErpFinVoucher businessDate 显示 `2026-07-19`

### 6.3 E2E 自动化验证

`tests/e2e/visual/field-format.value.spec.ts` DOM 文本断言（详见 plan Phase 5）。

## 7. 长尾 defer 清单

| 字段类型 | 实体示例 | Defer 理由 |
| --- | --- | --- |
| 配置实体的 rate/percentage | ErpMdTaxRate.rate（仅 form view） | 列表不暴露该列，form 编辑态保留默认 |
| 子实体进度跟踪表的 *_quantity 字段 | ErpCrmLeadSequenceProgress 等 | 长尾低频，业务价值低 |
| audit 时间戳（createTime/updateTime）部分实体 | 各域 *_log / *_history 实体 | 审计追溯保留 ISO 时间戳便于精确定位（仅主要业务实体格式化） |

## 8. Successor 触发条件

| 后续项 | 触发条件 |
| --- | --- |
| 币种符号本地化（CNY ¥ / USD $） | F15 i18n + l10n 域 plan 启动时 |
| 负数红字（会计专用借贷方向色） | F5 状态色继承 / finance 域专用方向色 plan 启动时 |
| F7 敏感字段脱敏 | F7 cross-cutting 敏感字段脱敏 plan 启动时（F6 是脱敏前置基础设施） |
| 报表金额字段格式化（nop-report formatExpr） | 报表格式化增强 plan / nop-report 模板统一审计启动时 |
| nop-entropy 平台 codegen 全局 domain → format 映射扩展 | nop-entropy 平台 codegen 扩展提案被采纳时（方案 C successor） |
| Form 字段编辑态格式化 | Phase 1 Explore (a) 验证 form 字段 format 支持时（编辑态保留默认是当前设计） |
