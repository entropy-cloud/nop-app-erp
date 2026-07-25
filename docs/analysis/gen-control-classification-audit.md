# gen-control 块全量分类审计

> Plan: `docs/plans/2026-07-25-1430-1-gen-control-domain-mapping-convergence.md` Phase 1
> Date: 2026-07-25
> Scope: `module-*/erp-*-web/src/main/resources/_vfs/erp/*/pages/**/*.view.xml`

## 1. 审计方法

Python 分类脚本逐块提取 `<gen-control>` 内 `<c:script>` 的返回对象 + 所在 `<col>` 的 `id`/`domain`/`stdDataType`/`grid_id`/`editMode` 属性，按四分类规则裁决：

- **R（redundant）**：col 已设正确 domain + control.xlib 映射等价 → gen-control 可移除
- **D（domain-gap）**：col 缺 domain（金额/数量路径）或 control.xlib 缺 view-mode 映射（日期路径）→ 补齐后转 R
- **C（custom-keep）**：tpl/switch/mapping/picker/input-number 等自定义渲染，或行为变更风险（edit-mode display→editable）→ 保留
- **U（unknown）**：逐项裁决后全部降级为 C（非展示控件返回 / 自定义 rate 显示）

## 2. 总体计数

| 分类 | 块数 | 占比 |
|------|------|------|
| R | 4 | 0.4% |
| D | 625 | 64.7% |
| C | 337 | 34.9% |
| U | 0 | 0% |
| **合计** | **966** | 100% |

涉及文件：224 个（其中 208 个含 R/D 可收敛块）。

## 3. D 类按 domain × mode 分布

| domain | mode | 块数 | 收敛路径 |
|--------|------|------|---------|
| amount | view | 162 | col 补 `domain="amount"` → view-amount 映射（已存在） |
| date | view | 179 | stdDataType=date → view-date 映射（**Phase 2 需新增 view-date**） |
| datetime | view | 172 | stdDataType=timestamp/datetime → view-timestamp/view-datetime 映射（**Phase 2 需新增 view-timestamp**） |
| quantity | view | 85 | col 补 `domain="quantity"` → view-quantity 映射（**Phase 2 需新增 view-quantity**） |
| unitPrice | view | 27 | col 补 `domain="unitPrice"` → view-unitPrice 映射（**Phase 2 需新增 view-unitPrice**） |
| **合计** | | **625** | |

## 4. 等价性核验（Phase 1 Proof）

控件匹配链（`XuiHelper.getControlTag`）：`control` → `domain` → `stdDomain` → `relKind` → `stdDataType`，查找 `{mode}-{type}` 标签。`mode` 为 view（grid 显示）/ edit / list-edit / query。

### 4.1 xjson 编译规则确认

`docs-for-ai` 平台文档 `xjson.md` 确认：xjson `outputMode` 下 XML tagName 映射为 JSON `type` 属性，**显式 `type` 属性覆盖 tagName 推导值**。因此 control.xlib 中：

```xml
<view-amount outputMode="xjson">
    <source>
        <text type="number" kilometer="@:true" precision="@:2"/>
    </source>
</view-amount>
```

编译为 JSON `{ type: 'number', kilometer: true, precision: 2 }`（tagName "text" 被显式 `type="number"` 覆盖）。

### 4.2 逐路径 AMIS 属性对照表

| 路径 | gen-control 输出 | control.xlib 标签输出 | 等价 |
|------|-----------------|---------------------|------|
| amount (view) | `{ type:'number', kilometer:true, precision:2 }` | view-amount → `{ type:'number', kilometer:true, precision:2 }` | ✅ |
| quantity (view) | `{ type:'number', kilometer:true, precision:4 }` | view-quantity（新增）→ `{ type:'number', kilometer:true, precision:4 }` | ✅ |
| unitPrice (view) | `{ type:'number', kilometer:true, precision:4 }` | view-unitPrice（新增）→ `{ type:'number', kilometer:true, precision:4 }` | ✅ |
| date (view) | `{ type:'date', format:'YYYY-MM-DD' }` | view-date（新增）→ `{ type:'date', format:'YYYY-MM-DD' }` | ✅ |
| datetime (view, TIMESTAMP col) | `{ type:'datetime', format:'YYYY-MM-DD HH:mm:ss' }` | view-timestamp（新增）→ `{ type:'datetime', format:'YYYY-MM-DD HH:mm:ss' }` | ✅ |
| datetime (view, DATETIME col) | `{ type:'datetime', format:'YYYY-MM-DD HH:mm:ss' }` | view-datetime（已存在）→ `{ type:'datetime', format:'YYYY-MM-DD HH:mm:ss' }` | ✅ |

### 4.3 ORM domain scale 与 control.xlib precision 对照

ORM 定义（逐域抽样一致）：

| domain | stdSqlType | precision | scale | AMIS precision | control.xlib 现状 |
|--------|-----------|-----------|-------|---------------|-----------------|
| amount | DECIMAL | 18 | 2 | 2 | edit-amount=2 ✅ / view-amount=2 ✅ |
| quantity | DECIMAL | 20 | 4 | 4 | edit-quantity=**3** ❌ / view-quantity=**缺失** ❌ |
| unitPrice | DECIMAL | 20 | 4 | 4 | edit-unitPrice=4 ✅ / view-unitPrice=**缺失** ❌ |

**关键发现**：control.xlib `edit-quantity` 当前 precision=3，与 ORM quantity domain scale=4 不匹配。Phase 2 必须修正为 precision=4（与 ORM scale=4 一致），否则域内所有数量字段视图精度从 4 降为 3 = 渲染回归。

## 5. U 类裁决记录

| 块数 | 模式 | 返回对象 | 裁决 | 理由 |
|------|------|---------|------|------|
| 8 | view | `{ type:'number', precision:4 }`（taxRate 列） | → C | 无 kilometer，税率自定义显示（非标准 amount/quantity），control.xlib 无匹配 |
| 4 | edit | `{ status: ok?0:500, msg:... }` | → C | 表单校验返回，非展示控件 |
| 2 | view | `{ kbId:..., code:..., title:... }` | → C | 知识库联动数据返回，非展示控件 |
| 1 | edit | `{ status:0, msg:'', data:{...} }` | → C | 预览数据返回，非展示控件 |

## 6. C 类分布（保留不替换）

| 类别 | 块数 | 说明 |
|------|------|------|
| tpl 自定义渲染 | 155 | F5 状态着色 / 敏感字段脱敏（`tpl:'******'`）/ 链接单元格 |
| 复合/非简单对象 | 72 | 含 onEvent/validations/name 的复杂控件（多见于 sub-grid-edit 可编辑单元格） |
| picker 关联选择 | 31 | 物料/往来单位/科目等 picker |
| input-number 编辑控件 | 28 | 含 step/validations/onEvent 的可编辑数值（与 view-mode display number 不同） |
| input-text 编辑控件 | 9 | 自定义文本输入 |
| input-date 编辑控件 | 7 | edit-mode 日期输入 |
| input-password | 5 | 密码/密钥脱敏输入 |
| 其他（static/select/switch/tree-select 等） | 21 | 域特有自定义 |
| edit-mode display-number（行为风险） | 3 | sub-grid-edit 内只读 display 单元格；移除 gen-control 会经 list-edit 回退链选择 edit-amount/edit-quantity（input-number 可编辑），改变行为 → 保留 |
| exchangeRate precision=8 | 2 | 仅 2 域（cs/md），<3 域阈值，保留为 C |
| 非标准日期格式 | 2 | `YYYY-MM-DD`（datetime 列）/ `YYYY-MM-DD HH:mm`（截断秒） |

## 7. Phase 2 必需的 control.xlib 变更

基于等价性核验（§4）与 ORM scale 对照（§4.3），Phase 2 必须对项目级 control.xlib 做以下变更：

1. **修正 `edit-quantity`**：precision 3→4（与 ORM quantity scale=4 一致），step 保持 1
2. **新增 `view-quantity`**：`<text type="number" kilometer="@:true" precision="@:4"/>`
3. **新增 `view-unitPrice`**：`<text type="number" kilometer="@:true" precision="@:4"/>`
4. **新增 `view-date`**：`<text type="date" format="@:YYYY-MM-DD"/>`
5. **新增 `view-timestamp`**：`x:prototype="view-datetime"`（TIMESTAMP 列经 stdDataType=timestamp 匹配）

这些变更与 plan Phase 2 Decision（扩展项目级 control.xlib 追加 date/datetime 映射）一致，且 §4 已验证等价性（零渲染差异）。

## 8. 结论

- 966 块中 629 块（R 4 + D 625）可在 Phase 2（control.xlib 修正 + domain 补齐）后于 Phase 3 移除
- 337 块 C 类保留（域特有自定义渲染 / 行为风险 / 低频非标准精度）
- 0 块 U 类遗留
