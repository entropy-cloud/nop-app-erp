# ERP 字段格式化设计 (F6)

> Status: active
> Owner: docs/design/field-formatting-patterns.md (单一真相源)
> Plan: `docs/plans/2026-07-19-2200-2-f6-field-formatting-xmeta.md`

> **Flux 控件映射**（2026-08-03 全量迁移后）：金额/数量/单价格式化经 flux-control.xlib domain→控件映射输出（edit-amount → flux `input-number`，kilometer/precision 经 ERP control.xlib 标签输出而非 GenGridCol pick 层，列键集 AMIS↔flux 一致）。列表/子表只读格式化经 flux-web GenGridCol 输出 flux 列。下文 xmeta `ext:displayFormat`/`precision`/`kilometer` 配置作为**模型层语义**仍权威（模型驱动、渲染器无关），AMIS tpl/kilometer 专属输出降级为历史注记。

## 1. 目的与范围

为 ERP 18+1 业务域的金额、数量、单价、税率、汇率、百分比、日期、日期时间字段建立统一的显示格式化机制，将 codegen 默认的纯 BigDecimal / ISO 时间戳渲染升级为「千分位 + 固定小数位」或「友好日期格式」。

**范围**：
- 列表页 `<grid id="list">` 列
- 子表视图 `<grid id="sub-grid-view">` 行内列
- 主表单只读 `<form id="view">` 字段（cell）（依赖 gen-control，与列表同机制）

**不包含**：
- 编辑态表单字段（输入态保留原始值，避免千分位干扰输入）
- 报表 / 看板金额字段（nop-report 走 `formatExpr` 独立机制）
- 币种符号本地化（CNY ¥ / USD $，归 l10n successor——F15 i18n 文本标签层与币种符号数值本地化属不同面，见本文件 §9 与 Deferred）
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
- `module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/ErpPurOrder/ErpPurOrder.view.xml:16-31`（F5 docStatus gen-control，证明 `return {type:"tpl", tpl:...}` 经 putAll 注入到 AMIS col）
- `module-hr/erp-hr-web/src/main/resources/_vfs/erp/hr/pages/ErpHrEmployee/ErpHrEmployee.view.xml:121-125` `<input-date format="YYYY-MM-DD">`：HR 用 form cell gen-control + `<input-date format=>` 嵌套控件方式；列表 grid col 不可直接复用（gen-control 包裹控件模式 vs col 直接 format 属性）
- 决策：列表 grid col 采用方案 D（c:script 返回 `{type:'date', format:'YYYY-MM-DD'}` / `{type:'number', kilometer:true, precision:N}`），与 F5 范式一致；form cell 编辑态保留 codegen 默认（`edit-date`/`edit-datetime` 已正确 format），不动 form 编辑态

### Decision (b): 格式化映射表 — 按 ORM domain 与字段语义

| domain | ORM scale | 显示格式 | AMIS type | AMIS 关键属性 | 对齐 | 与 roadmap F6 偏差裁决 |
| --- | --- | --- | --- | --- | --- | --- |
| `amount` | 2-4 | 千分位 + 2 位小数 | `number` | `{kilometer:true, precision:2}` | 右对齐 | 一致 |
| `quantity` | 4 | 千分位 + 4 位小数 | `number` | `{kilometer:true, precision:4}` | 右对齐 | 偏差：roadmap 给 `#,##0.###`（3 位）；按 ORM scale=4 选 4 位避免丢精度 |
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

按 F5 实际经验（68 实体 ~150 列），覆盖承诺：
- **Phase 2**：核心 4 域（purchase/sales/inventory/finance）头实体 + 主 Line 实体的金额/数量/单价/税率字段
- **Phase 3**：汇率/百分比/日期/日期时间字段（全域）
- **Phase 4**：13 扩展域剩余金额/数量/日期字段
- **长尾字段**（低频、非业务核心）显式 defer 到 successor，记录于 plan

### Decision (e): control.xlib 自动映射优先于 gen-control

> 上方 Decision (a) 方案 C（codegen `domain → format` 全局映射）原标 `✗（successor）`，现经项目级 `control.xlib`（`app-erp-all/src/main/resources/_vfs/erp/xlib/control.xlib`，`x:extends` 平台基线）**为首选机制**。方案 D（inline gen-control）降级为「仅在 control.xlib 无法覆盖时使用」。

**约定**：标准金额/数量/单价/日期/日期时间格式化**必须**经 ORM `domain`（amount/quantity/unitPrice）或 `stdSqlType`（DATE/TIMESTAMP）→ 项目级 `control.xlib` 控件匹配链（`XuiHelper.getControlTag`：`{mode}-{domain}` / `{mode}-{stdDataType}`）自动选择控件，**不得**新写 `<gen-control>` 内联脚本。仅以下场景保留 gen-control：

- 自定义渲染（tpl 状态标签 / 敏感字段脱敏 / 链接单元格 / switch）
- 非标准 precision（<3 域共享，如 exchangeRate precision=8）
- edit-mode display-number（避免 list-edit 回退链选择可编辑控件改变行为）

控件匹配链与 control.xlib 标签清单见 `../nop-entropy/docs-for-ai/02-core-guides/frontend-rendering-pipeline.md` 与 `docs/analysis/gen-control-classification-audit.md`。629 块冗余 gen-control 已移除（R 4 + D 625），337 块 C 类自定义渲染保留。

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

`<form id="edit">` / `<form id="add">` / sub-grid-edit 中的 `<cell>` / `<col>` 保留 codegen 默认控件（`edit-decimal` 已设 `precision: scale`，`edit-date` 已设 `format:"YYYY-MM-DD"`）。仅覆盖只读展示态。

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
| 币种符号本地化（CNY ¥ / USD $） | l10n successor 启动时（币种符号属 l10n 数值格式面，与 F15 i18n 文本 label 解耦） |
| 负数红字（会计专用借贷方向色） | F5 状态色继承 / finance 域专用方向色 plan 启动时 |
| ~~F7 敏感字段脱敏~~ | 见 §9 敏感字段脱敏段 |
| 报表金额字段格式化（nop-report formatExpr） | 报表格式化增强 plan / nop-report 模板统一审计启动时 |
| nop-entropy 平台 codegen 全局 domain → format 映射扩展 | nop-entropy 平台 codegen 扩展提案被采纳时（方案 C successor） |
| Form 字段编辑态格式化 | Phase 1 Explore (a) 验证 form 字段 format 支持时（编辑态保留默认是当前设计） |

## 9. 敏感字段脱敏（F7）

> Owner: `docs/design/field-formatting-patterns.md` §9（单一真相源）
> Plan: `docs/plans/2026-07-22-1400-3-cross-cutting-sensitive-field-masking.md`
> 覆盖：hr `ErpHrEmployee`（idCardNo / mobilePhone / bankAccountId / socialSecurityNo）+ logistics `ErpLogCarrierConfig`（apiKey / apiSecret）

### 9.1 决策摘要

| 决策 | 裁决 | 理由 |
| --- | --- | --- |
| (a) gen-control tpl 字符串变换可行性 | **可行** | amis-formula 内置文本函数 `LEFT`/`RIGHT`/`MID`/`CONCATENATE`/`ISEMPTY`（证据：`baidu/amis` `packages/amis-formula/src/doc.ts`）；与 F5 status-tag tpl 同一 codegen 链路 |
| (b) unhide+mask vs keep-hidden | **unhide+mask（候选 a）** | roadmap §跨切面 §4 明确要求「脱敏显示」；exit criterion 要求覆盖；hidden 与「脱敏」语义冲突 |
| (c) 前端层 vs 后端层脱敏 | **前端 gen-control tpl（候选 a）** | 与 F5/F6 范式一致；不改后端保护区域；GraphQL 响应仍含明文（安全边界明确，后端 @BizLoader 留作 successor） |

**安全边界声明**：本机制为**前端渲染层脱敏**（AMIS tpl 打码）。GraphQL 响应仍包含明文（浏览器开发者工具 / F12 网络面板可见）。若需 API 层脱敏（GraphQL 响应即打码），需后端 BizModel `@BizLoader` 实现（归安全审计 successor）。

### 9.2 脱敏打码模板（3 类）

#### 手机号（保留前 3 后 4）：`138****0000`

```xml
<col id="mobilePhone">
    <gen-control>
        <c:script><![CDATA[
            return { type: 'tpl', tpl: '${LEFT(mobilePhone,3)}****${RIGHT(mobilePhone,4)}' };
        ]]></c:script>
    </gen-control>
</col>
```

#### 证件号 / 银行卡（保留首 1 末 4 或仅末 4）：`张******1234` / `****1234`

```xml
<!-- 证件号：首1末4 -->
<cell id="idCardNo">
    <gen-control>
        <c:script><![CDATA[
            return { type: 'tpl', tpl: '${LEFT(idCardNo,1)}******${RIGHT(idCardNo,4)}' };
        ]]></c:script>
    </gen-control>
</cell>

<!-- 银行卡：仅末4 -->
<col id="bankAccountId">
    <gen-control>
        <c:script><![CDATA[
            return { type: 'tpl', tpl: '****${RIGHT(bankAccountId,4)}' };
        ]]></c:script>
    </gen-control>
</col>
```

#### 高敏感凭据（全打码或保留首2末4）：`******` / `sk****89ab`

```xml
<!-- 社保号：全打码 -->
<cell id="socialSecurityNo">
    <gen-control>
        <c:script><![CDATA[
            return { type: 'tpl', tpl: '******' };
        ]]></c:script>
    </gen-control>
</cell>

<!-- API Key：首2末4（仅当字段 published="true"、GraphQL 回传明文时可用） -->
<col id="apiKey">
    <gen-control>
        <c:script><![CDATA[
            return { type: 'tpl', tpl: '${LEFT(apiKey,2)}****${RIGHT(apiKey,4)}' };
        ]]></c:script>
    </gen-control>
</col>
```

**写回型凭据例外（logistics `ErpLogCarrierConfig.apiKey/apiSecret`）**：这两个字段在 xmeta 中 `published="false" queryable="false"`（写回型集成凭据：前端可录入、后端永不回传明文）。由于 GraphQL 响应不含字段值，**动态 `LEFT/RIGHT` tpl 会渲染 `undefined`（垃圾值）**，故查看态（form view cell + sub-grid-view col）改用**静态全打码** `******`（与社交号同模板）。编辑态仍用 `{type:'input-password'}` 录入。这比「发布明文 + 前端打码」更安全（明文永不离开服务端），是 Phase 0 Decision (c)「前端层脱敏」对集成凭据的合理例外。

### 9.3 编辑态：input-password

编辑态表单 cell 使用 `{type:'input-password'}` 替换 codegen 默认 input-text，用户键入时显示圆点。AMIS `input-password` 内置 `showRevealToggle`（眼睛图标切换明文/密文）。

```xml
<form id="edit">
    <cells>
        <cell id="idCardNo">
            <gen-control>
                <c:script><![CDATA[
                    return { type: 'input-password' };
                ]]></c:script>
            </gen-control>
        </cell>
    </cells>
</form>
```

**例外**：银行账户号（`bankAccountId`）编辑态保留 codegen 默认 input-text（需肉眼核对录入），仅查看态打码。

### 9.4 前端层 vs 后端层脱敏边界

| 层 | 机制 | 覆盖范围 | 安全性 | 适用场景 |
| --- | --- | --- | --- | --- |
| **前端渲染层**（本设计） | gen-control tpl 打码 AMIS 输出 | list grid col + form view cell + sub-grid-view col | 低（GraphQL 响应含明文，F12 可见） | UI 防偷窥、截图脱敏、合规展示 |
| **写回型凭据**（logistics apiKey/apiSecret） | xmeta `published="false"` + 查看态静态 `******` | GraphQL 响应不含字段值 | 高（明文永不离开服务端） | 集成凭据（API key/secret）：前端可录不可读 |
| 后端响应层（successor） | BizModel `@BizLoader` 打码 GraphQL 响应 | GraphQL response 全链路 | 高（API 消费者也拿到打码值） | 安全审计要求 API 层脱敏、第三方集成 |

> **字段级清单（冻结输入）**：保密五面（薪酬/合同/EDI/供应商价格/成本分解）+ F7 已落地基线 + `taxFileNo`（隐藏非脱敏）的逐字段七元组清单见下方 **§9.7**，作为本节后端响应层 successor（E3.1）与字段级可见性（E4.1）的冻结输入。清单来源 plan：`docs/plans/2026-08-09-1314-1-sensitive-field-confidentiality-inventory.md`（P1.1）。

### 9.5 反模式自检表（脱敏补充）

| 不要这样写 | 应该这样写 |
| --- | --- |
| 用 JS `.substring()` / `.padStart()` 写在 tpl 表达式里（amis-formula 不支持 JS 方法调用） | 用 amis-formula 内置函数 `LEFT(text,len)` / `RIGHT(text,len)` / `MID(text,from,len)` |
| 编辑态也用 tpl 打码（导致无法录入真实值） | 编辑态用 `{type:'input-password'}`；查看态用 tpl 打码 |
| 在 `<form id="edit">` cell 上加 gen-control tpl（覆盖了编辑控件） | edit form cell 用 input-password；view form cell 用 tpl 打码 |
| 忘记从 layout 移除「敏感字段隐藏」注释 | 更新 layout section title（如 payroll 段去掉「脱敏待落地」字样） |
| 用 `visibleOn="${false}"` 隐藏（字段不可见，非脱敏） | 移除 visibleOn + 加 gen-control tpl 打码（字段可见但打码） |
| 对 xmeta `published="false"` 的写回型凭据用动态 `LEFT/RIGHT` tpl（渲染 `undefined` 垃圾值） | 改用静态全打码 `******`（字段值不回传前端，无法动态截取）；或评估是否发布字段 |

### 9.6 PoC 结论（Phase 0 Explore (a)）

amis-formula tpl 表达式 `${ expr }` 内置完整文本函数集（经 `baidu/amis` 官方源 `packages/amis-formula/src/doc.ts` 核实）：

- `LEFT(text, len)` — 取左侧 N 字符
- `RIGHT(text, len)` — 取右侧 N 字符
- `MID(text, from, len)` — 从位置 from 取 len 字符
- `CONCATENATE(t1, t2, ...)` / `+` — 拼接
- `LEN(text)` / `ISEMPTY(text)` / `PADSTART(text, num, pad)` / `REPLACE(text, search, replace)`

gen-control `{type:'tpl', tpl:'${LEFT(field,N)}****${RIGHT(field,M)}'}` 经 `flux-web.xlib:GenGridCol` → `eval(colXpl)` → putAll 到 AMIS col JSON 链路，在 list grid col、form view cell、sub-grid-view col 三态均生效（与 F5 status-tag tpl 同一机制）。

### 9.7 保密敏感字段清单（P1.1 冻结输入）

> Owner: 本节（§9 子节，单一真相源）
> 来源 plan：`docs/plans/2026-08-09-1314-1-sensitive-field-confidentiality-inventory.md`（permissions-enforcement 路线图 P1.1）
> 用途：为 §9.4「后端响应层脱敏 successor（E3.1）」与「字段级可见性（E4.1）」提供逐字段冻结输入；同时显式复核确认 F7 既有 PII 集落地状态。
> 范围：保密五面（薪酬 / 合同 / EDI / 供应商价格 / 成本分解）+ F7 已落地前端脱敏基线 + `taxFileNo`（隐藏非脱敏）。
> **非目标**：不实现 E3.1/E4.1（仅产清单）；不改 GraphQL schema/ORM/xmeta（仅读取记录现状）；不做逐字段角色绑定裁决（归 E4.1，受 P1.2 Q1/Q4 约束）；不做成本取值豁免裁决（归 P1.2 Q4 + E3.2）。

### 9.7.1 七元组约定

每行字段携带：`{实体, 字段(propId, stdSqlType), ORM 行号, 当前脱敏方式, xmeta published/queryable 现值, GraphQL schema 影响Y/N, 拟落地层}`。

- **当前脱敏方式**取值：`F7前端tpl`（gen-control tpl 打码）/ `写回型published=false`（响应不含值）/ `隐藏visibleOn=false`（非脱敏）/ `无`。
- **GraphQL schema 影响 Y/N**：字段 xmeta `published`/`queryable` 现值为默认 `true`（GraphQL 暴露该字段）→ 翻转会改变对外契约 → **Y**；若已 `published="false"`（不暴露）→ **N**。
- **证据规则**：xmeta `published`/`queryable` 现值以**保留层 delta xmeta**（`<Entity>.xmeta`，非 `_` 前缀）的显式覆盖为准；无显式覆盖时回落生成基线 `_<Entity>.xmeta` 默认 `queryable="true"`、`published` 默认 `true`（Nop 约定）。全域相关 delta xmeta 仅 logistics `ErpLogCarrierConfig.xmeta` 存在 `published="false"` 覆盖（grep 证据：hr/ct/b2b/pur/md/mfg/log delta xmeta 中 `published="false"` 仅命中 logistics 该实体的 apiKey/apiSecret/credentials 三 prop）。

### 9.7.2 F7 已落地前端脱敏基线（PII 集）—— 复核确认

**hr `ErpHrEmployee`**（`module-hr/model/app-erp-hr.orm.xml:271`，实体头）—— 4 字段，经 view.xml gen-control tpl 打码：

| 字段 (propId, stdSqlType) | ORM 行号 | 当前脱敏方式 | xmeta published/queryable | GraphQL schema 影响 | 拟落地层 |
| --- | --- | --- | --- | --- | --- |
| idCardNo (9, VARCHAR) | orm:283 | F7前端tpl（form-view cell 首1末4 `view.xml:147-152`；edit input-password `:202-206`） | 默认 true/true（`_ErpHrEmployee.xmeta:56` queryable=true，无 published 覆盖） | **Y**（响应含明文，F12 可见） | 已落地（前端）；后端 successor E3.1 |
| mobilePhone (11, VARCHAR) | orm:285 | F7前端tpl（form-view cell 首3末4 `:154-159`；edit input-password `:209-213`） | 默认 true/true（`_xmeta:64`） | **Y** | 同上 |
| bankAccountId (28, BIGINT) | orm:302 | F7前端tpl（list grid col 末4 `:16-21`；form-view cell 末4 `:161-166`；edit 保留 input-text 以便肉眼核对录入） | 默认 true/true（`_xmeta:132`） | **Y** | 同上 |
| socialSecurityNo (29, VARCHAR) | orm:303 | F7前端tpl（form-view cell 全打码 `******` `:168-173`；edit input-password `:216-220`） | 默认 true/true（`_xmeta:136`） | **Y** | 同上 |

> 实现注记：当前 view.xml tpl 实际使用 JS `.slice()`（如 `${(idCardNo||"").slice(0,1)}`），与本节 §9.2/§9.5 描述的 amis-formula `LEFT/RIGHT` 模板存在表述差异；flux 渲染下 `.slice()` 实测生效（F7 已落地基线），功能上达成打码。该差异为既有实现细节，非本清单范围，登记备查。

**logistics `ErpLogCarrierConfig`**（`module-logistics/model/app-erp-logistics.orm.xml:128`，实体头）—— 2 字段，写回型凭据：

| 字段 (propId, stdSqlType) | ORM 行号 | 当前脱敏方式 | xmeta published/queryable | GraphQL schema 影响 | 拟落地层 |
| --- | --- | --- | --- | --- | --- |
| apiKey (7, VARCHAR(500)) | orm:138 | 写回型published=false（GraphQL 响应不含字段值；查看态静态 `******`） | **false/false**（`ErpLogCarrierConfig.xmeta:5`，显式覆盖） | **N**（schema 已不暴露，翻转无契约影响） | 已落地（写回型，全仓唯一后端级保密先例） |
| apiSecret (8, VARCHAR(500)) | orm:139 | 同上 | **false/false**（`:6`） | **N** | 同上 |

> 旁注：派生 prop `credentials`（xmeta `:7`）同样 `published="false" queryable="false"`。F7 集按 §9 锚定为 hr 4 + logistics 2 = 6 字段。

### 9.7.3 `taxFileNo`（隐藏非脱敏）—— 单独登记

| 实体 / 字段 (propId, stdSqlType) | ORM 行号 | 当前脱敏方式 | xmeta published/queryable | GraphQL schema 影响 | 拟落地层 |
| --- | --- | --- | --- | --- | --- |
| hr ErpHrEmployee.taxFileNo (30, VARCHAR) | orm:304 | **隐藏visibleOn=false**（`ErpHrEmployee.view.xml:223-225`，§9.5 反模式点名「字段不可见，非脱敏」） | 默认 true/true（`_ErpHrEmployee.xmeta:140` queryable=true） | **Y**（GraphQL 响应含明文，前端仅隐藏不显示） | **E3.1 后端响应层脱敏**（路由：unhide+后端打码，候选），非 F7 tpl 集 |

### 9.7.4 面A：薪酬面（hr）

> 全域无 xmeta `published`/`queryable` 覆盖 → 字段均默认 `published=true queryable=true` → GraphQL schema 影响 **Y**、响应含明文。下表省略重复现值列，统一记「默认true/true」。

| 实体 / 字段 (propId, stdSqlType) | ORM 行号 | 当前脱敏方式 | published/queryable | schema影响 | 拟落地层 |
| --- | --- | --- | --- | --- | --- |
| ErpHrSalary.basicSalary (5, DECIMAL) | orm:728 | 无 | 默认true/true | Y | E3.1 + E4.1（仅薪酬审批人） |
| ErpHrSalary.positionAllowance (6, DECIMAL) | orm:729 | 无 | 默认true/true | Y | 同上 |
| ErpHrSalary.performanceBonus (7, DECIMAL) | orm:730 | 无 | 默认true/true | Y | 同上 |
| ErpHrSalary.overtimePay (8, DECIMAL) | orm:731 | 无 | 默认true/true | Y | 同上 |
| ErpHrSalary.mealAllowance (9, DECIMAL) | orm:732 | 无 | 默认true/true | Y | 同上 |
| ErpHrSalary.transportAllowance (10, DECIMAL) | orm:733 | 无 | 默认true/true | Y | 同上 |
| ErpHrSalary.otherAllowance (11, DECIMAL) | orm:734 | 无 | 默认true/true | Y | 同上 |
| ErpHrSalary.grossSalary (12, DECIMAL) | orm:735 | 无 | 默认true/true | Y | 同上（应发合计，汇总） |
| ErpHrSalary.socialInsurance (13, DECIMAL) | orm:736 | 无 | 默认true/true | Y | 同上（社保个人部分） |
| ErpHrSalary.housingFund (14, DECIMAL) | orm:737 | 无 | 默认true/true | Y | 同上（公积金个人部分） |
| ErpHrSalary.taxAmount (15, DECIMAL) | orm:738 | 无 | 默认true/true | Y | 同上（个税） |
| ErpHrSalary.otherDeductions (16, DECIMAL) | orm:739 | 无 | 默认true/true | Y | 同上 |
| ErpHrSalary.netSalary (17, DECIMAL) | orm:740 | 无 | 默认true/true | Y | 同上（实发合计，汇总） |
| ErpHrSalary.cumulativeData (34, VARCHAR(2000)) | orm:750 | 无 | 默认true/true | Y | E3.1（累计个税数据，非金额但是个税机密） |
| ErpHrEmploymentContract.socialInsuranceBase (14, DECIMAL) | orm:442（实体@425） | 无 | 默认true/true | Y | E3.1 + E4.1 |
| ErpHrSocialInsuranceBase.socialInsuranceBase (4, DECIMAL) | orm:1020（实体@1013） | 无 | 默认true/true | Y | E3.1 + E4.1 |
| ErpHrSocialInsuranceBase.housingFundBase (5, DECIMAL) | orm:1021 | 无 | 默认true/true | Y | 同上 |
| ErpHrSocialInsuranceConfig.companyRate (4, DECIMAL(8,6)) | orm:992（实体@985） | 无 | 默认true/true | Y | E4.1（社保配置 rate，非个人薪酬） |
| ErpHrSocialInsuranceConfig.employeeRate (5, DECIMAL(8,6)) | orm:993 | 无 | 默认true/true | Y | 同上 |
| ErpHrSocialInsuranceConfig.baseLowerLimit (6, DECIMAL) | orm:994 | 无 | 默认true/true | Y | 同上（基数上下限） |
| ErpHrSocialInsuranceConfig.baseUpperLimit (7, DECIMAL) | orm:995 | 无 | 默认true/true | Y | 同上 |
| ErpHrSalarySimulationItemAdjustment.originalAmount (5, DECIMAL) | orm:924（实体@916） | 无 | 默认true/true | Y | E3.1 + E4.1 |
| ErpHrSalarySimulationItemAdjustment.adjustedAmount (6, DECIMAL) | orm:925 | 无 | 默认true/true | Y | 同上 |

> 面A 旁注：`ErpHrSalarySimulation`（实体@860）为模拟头实体，**无直接金额字段**（金额经子表 `ErpHrSalarySimulationItemAdjustment` 承载，已登记）。`ErpHrSalaryItem`（实体@952）为薪酬项目配置实体，**无金额字段**，含 `formula`/`isTaxable`/`isSocialInsuranceBase` 核算规则，属薪酬配置可见性范畴（E4.1），非金额脱敏。`ErpHrSalary.performanceFactor`(29)/`actualWorkDays`(30)/`requiredWorkDays`(31)/`unpaidLeaveDays`(33) 为核算系数/考勤，归 F6 §7 长尾 defer（非保密金额）。

### 9.7.5 面B：合同面（contract）

> 全域无 xmeta `published`/`queryable` 覆盖 → 字段默认 `published=true queryable=true` → schema 影响 **Y**、响应含明文。

| 实体 / 字段 (propId, stdSqlType) | ORM 行号 | 当前脱敏方式 | published/queryable | schema影响 | 拟落地层 |
| --- | --- | --- | --- | --- | --- |
| ErpCtContract.totalAmount (9, DECIMAL(20,4)) | orm:134（实体@122） | 无 | 默认true/true | Y | E3.1 + E4.1（合同审批人/专员） |
| ErpCtContractLine.amount (8, DECIMAL(20,4)) | orm:206（实体@195） | 无 | 默认true/true | Y | 同上 |
| ErpCtInvoicePlan.amount (4, DECIMAL(20,4)) | orm:281（实体@274） | 无 | 默认true/true | Y | 同上（开票金额） |
| ErpCtConsumptionLine.amount (6, DECIMAL(20,4)) | orm:314（实体@305） | 无 | 默认true/true | Y | 同上 |
| ErpCtApprovalMatrix.minAmount (4, DECIMAL) | orm:367（实体@360） | 无 | 默认true/true | Y | E4.1（审批矩阵配置） |
| ErpCtApprovalMatrix.maxAmount (5, DECIMAL) | orm:368 | 无 | 默认true/true | Y | 同上 |
| ErpCtRebateAgreement.totalAccumulatedAmount (12, DECIMAL) | orm:480（实体@465） | 无 | 默认true/true | Y | E3.1 + E4.1（返利机密） |
| ErpCtRebateAgreement.estimatedRebateAmount (13, DECIMAL) | orm:481 | 无 | 默认true/true | Y | 同上 |
| ErpCtRebateTier.fromAmount (3, DECIMAL) | orm:526（实体@520） | 无 | 默认true/true | Y | E4.1（返利档配置） |
| ErpCtRebateTier.toAmount (4, DECIMAL) | orm:527 | 无 | 默认true/true | Y | 同上 |
| ErpCtRebateTier.rebateAmount (6, DECIMAL) | orm:529 | 无 | 默认true/true | Y | 同上 |
| ErpCtRebateAccrual.billAmountSource (6, DECIMAL) | orm:558（实体@549） | 无 | 默认true/true | Y | E3.1 + E4.1 |
| ErpCtRebateAccrual.accruedRebate (7, DECIMAL) | orm:559 | 无 | 默认true/true | Y | 同上 |
| ErpCtRebateSettlement.totalRebateAmount (5, DECIMAL) | orm:591（实体@583） | 无 | 默认true/true | Y | 同上 |

电子签相关字段（`ErpCtSignatureRequest`，实体@627）—— 合同域**无后端级保密先例**（无明文 secret 字段，与 logistics apiKey/apiSecret 不同），登记为电子签事务/证书引用可见性：

| 字段 (propId, stdSqlType) | ORM 行号 | 当前脱敏方式 | published/queryable | schema影响 | 拟落地层 |
| --- | --- | --- | --- | --- | --- |
| signers (7, VARCHAR(2000)) | orm:637 | 无 | 默认true/true | Y | E4.1（含签署人 PII） |
| providerRequestId (5, VARCHAR(200)) | orm:635 | 无 | 默认true/true | Y | E4.1（集成事务标识） |
| certificateUrl (10, VARCHAR(1000)) | orm:640 | 无 | 默认true/true | Y | E4.1（完成证书 URL） |
| evidenceNo (11, VARCHAR(200)) | orm:641 | 无 | 默认true/true | Y | E4.1（存证编号） |

> 面B 旁注：`provider`(4)/`status`(6) 为枚举配置，非机密金额。`ErpCtRebateTier.rebatePercent`(5, DECIMAL(10,2)) 为返利比例（配置 rate），与 F6 §7 长尾 rate 范畴重叠，登记备查不单列。

### 9.7.6 面C：EDI 面（b2b）

> 全域无 xmeta `published`/`queryable` 覆盖 → 默认 `true/true` → schema 影响 **Y**。

| 实体 / 字段 (propId, stdSqlType) | ORM 行号 | 当前脱敏方式 | published/queryable | schema影响 | 拟落地层 |
| --- | --- | --- | --- | --- | --- |
| ErpB2bEdiFormat.formatStandard (5, VARCHAR(20)) | orm:142（实体@134） | 无 | 默认true/true | Y（枚举，翻转低敏） | E4.1（EDI 配置可见性） |
| ErpB2bEdiFormat.direction (6, VARCHAR(20)) | orm:143 | 无 | 默认true/true | Y（枚举） | 同上 |
| ErpB2bEdiDoc.attachmentFileId (11, VARCHAR(200)) | orm:177（实体@163） | 无 | 默认true/true | Y | E4.1（EDI 报文文件载荷引用，商业机密） |
| ErpB2bEdiDoc.error (9, VARCHAR(2000)) | orm:175 | 无 | 默认true/true | Y | E4.1（错误信息可能含载荷片段） |

> 面C 旁注：EDI 报文明文载荷以**文件**（`attachmentFileId`）承载，非 DB 列；字段级清单覆盖报文文件引用可见性，文件本体访问控制归文件权限层（非本清单）。`ErpB2bEdiDoc.state`(7)/`blockingLevel`(8)/`relatedBillCode`(6) 为状态/引用，非机密载荷（长尾 defer）。

### 9.7.7 面D：供应商价格面（purchase + master-data）

> 全域无 xmeta `published`/`queryable` 覆盖 → 默认 `true/true` → schema 影响 **Y**。

| 实体 / 字段 (propId, stdSqlType) | ORM 行号 | 当前脱敏方式 | published/queryable | schema影响 | 拟落地层 |
| --- | --- | --- | --- | --- | --- |
| ErpPurSupplierPriceList.unitPrice (6, DECIMAL(20,4)) | orm:393（实体@384） | 无 | 默认true/true | Y | E3.1 + E4.1（采购员/管理员） |
| ErpPurSupplierPriceList.taxRate (7, DECIMAL(10,4)) | orm:394 | 无 | 默认true/true | Y | E4.1（与价格清单绑定的 rate） |
| ErpPurSupplierPriceList.minOrderQuantity (8, DECIMAL) | orm:395 | 无 | 默认true/true | Y | E4.1（商务条款：起订量） |
| ErpMdMaterialSku.purchasePrice (7, DECIMAL(20,4)) | orm:384（实体@371） | 无 | 默认true/true | Y | E3.1 + E4.1 |
| ErpMdMaterialSku.salePrice (8, DECIMAL(20,4)) | orm:385 | 无 | 默认true/true | Y | E3.1 + E4.1（销售机密，邻近登记） |
| ErpMdMaterialSku.wholesalePrice (9, DECIMAL(20,4)) | orm:386 | 无 | 默认true/true | Y | 同上 |

### 9.7.8 面E：成本分解面（manufacturing）

> 全域无 xmeta `published`/`queryable` 覆盖 → 默认 `true/true` → schema 影响 **Y**。`ErpMfgCostRollupLine` 实体@1259（`module-manufacturing/model/app-erp-manufacturing.orm.xml`）。

| 字段 (propId, stdSqlType) | ORM 行号 | 当前脱敏方式 | published/queryable | schema影响 | 拟落地层 |
| --- | --- | --- | --- | --- | --- |
| materialCost (6, DECIMAL(20,4)) | orm:1268 | 无 | 默认true/true | Y | E3.1 + E4.1（成本机密） |
| laborCost (7, DECIMAL(20,4)) | orm:1269 | 无 | 默认true/true | Y | 同上 |
| overheadCost (8, DECIMAL(20,4)) | orm:1270 | 无 | 默认true/true | Y | 同上 |
| subcontractCost (9, DECIMAL(20,4)) | orm:1271 | 无 | 默认true/true | Y | 同上（委外成本） |
| totalCost (10, DECIMAL(20,4)) | orm:1272 | 无 | 默认true/true | Y | 同上（汇总） |
| unitCost (11, DECIMAL(20,4)) | orm:1273（domain=unitPrice） | 无 | 默认true/true | Y | 同上（单位标准成本） |

> 成本分解字段 ORM 行号统一引用 `app-erp-manufacturing.orm.xml:1268-1273`（实体头@1259，6 金额列 propId 6-11）。`CostRollupService` 跨域读值豁免裁决归 P1.2 Q4 + E3.2（Non-Goals 明示）。

### 9.7.9 清单边界取舍决策

- **范围裁决**：仅覆盖保密五面（roadmap 显式边界）。长尾低敏字段（各域审计时间戳、非业务核心配置 rate、考勤系数）移出范围，沿用 F6 §7 长尾 defer 范畴。
- **替代方案**：(a) 仅保密五面（**采纳**，roadmap 显式边界 + 信噪比高）；(b) 含长尾全量枚举（**拒绝**：非保密诉求，与 F6 §7 长尾 defer 重叠，稀释清单信噪比，放大 E4.1 裁决面）。
- **残留风险**：若未来出现新保密诉求（如新增机密域/字段），需扩清单。触发条件 = 新保密诉求出现（见 Deferred）。

### 9.7.10 复核结论（P1.1 范围内）

1. **F7 既有 PII 集经实测证据确认**：hr 4 字段（idCardNo/mobilePhone/bankAccountId/socialSecurityNo）前端 tpl 打码 + logistics 2 字段（apiKey/apiSecret）写回型 `published=false`，与 §9 文档描述一致。
2. **`taxFileNo` 现状确认**：`visibleOn=${false}` 隐藏非脱敏（§9.5 反模式），登记为「已发现敏感字段、现状态=隐藏、拟路由 E3.1」，**不计入** F7 已落地基线。
3. **保密五面均无脱敏**：既无前端 tpl，也无后端 `@BizLoader`；字段 xmeta 均 `published=true queryable=true` → GraphQL schema 影响 **Y**（翻转 published/queryable 将改变对外契约），为 E4.1 契约变更门控（横切关注点 5）提供冻结输入。
4. **清单可被 E3.1/E4.1 直接消费**：每字段七元组齐全，含 `propId`、`stdSqlType`、`published/queryable` 现值、GraphQL schema 影响标记、拟落地层。
