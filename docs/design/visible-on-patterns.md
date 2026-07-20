# 非状态 visibleOn 与主数据专用交互范式（Visible-On Patterns）

> Status: stable
> Owner docs: `docs/backlog/frontend-ui-roadmap.md` §F7 §1+§3 + §跨切面 UI 模式 6/7、`docs/design/master-data/ui-patterns.md`、`docs/design/inventory/ui-patterns.md`、`docs/design/assets/ui-patterns.md`
> 落地计划：`docs/plans/2026-07-20-1020-2-f7-non-status-visibleon-and-master-data-interactions.md`
> 平台参考: `../nop-entropy/docs-for-ai/03-runbooks/replace-field-with-complex-control.md`（gen-control + visibleOn）、`../nop-entropy/docs-for-ai/03-runbooks/add-field-and-validation.md`（async validator + onEvent）、`nop-auth/pages/NopAuthResource/NopAuthResource.view.xml`（cell-level visibleOn + clearValueOnHidden 范例）

## 1. 范式目标

本文档固化 ERP 系统中 **非状态驱动** 的智能 UI 交互范式，作为：

1. 字段值驱动 `visibleOn` 表达式库（按表单内字段值动态显隐其他字段）
2. 主数据编码唯一性前置校验范式（async validator on blur）
3. 主数据删除引用预览范式（mutation 前 @BizQuery countReferences + dialog 阻断）
4. 主数据启用/停用 Switch 控件范式（替代 codegen 默认 button-group-select + 停用确认 dialog）
5. **ErpFinVoucherLine 借贷金额切换 visibleOn 范式**（为 F4 finance voucher successor plan 预冻结表达式库）

**与 F1 的边界**：F1 覆盖**状态驱动**（`docStatus`/`approveStatus`）的按钮 `visibleOn`。本范式覆盖**字段值驱动**（`moveType`/`treatment`/`dcDirection` 等）以及**主数据专用交互原语**。

## 2. 平台管线

view.xml `<form>` 段的 `<cell>` 元素支持两个核心属性（见 `nop/schema/xui/xview.xdef`）：

| 属性 | 类型 | 语义 |
|------|------|------|
| `visibleOn` | AMIS 表达式 | 表达式为 `false` 时该 cell 不渲染（运行时 AMIS 控制响应式刷新） |
| `clearValueOnHidden` | boolean | cell 隐藏时自动清空字段值，防止隐藏字段提交脏数据（如 INCOMING 时 sourceLocationId 不显示但仍提交旧值） |

**范例来源**：`nop-auth/.../NopAuthResource.view.xml:80-88`：

```xml
<cell id="menuProps" clearValueOnHidden="true">
    <visibleOn>${resourceType != 'FNPT'}</visibleOn>
</cell>
<cell id="authProps" clearValueOnHidden="true">
    <visibleOn>${resourceType == 'FNPT'}</visibleOn>
</cell>
```

`<visibleOn>` 表达式语法为 AMIS 表达式（`${...}` 包裹，支持 `==/!=/&&/||/>/<` 等操作符）。表达式中的标识符（如 `moveType`、`treatment`）从当前 form 上下文中读取。

## 3. 字段值驱动 visibleOn 表达式库（约束式表格）

下表为本计划落地范围 + F4 finance voucher successor 引用前置。**字段名均经实时仓库 ORM 核实**。

| 实体 | 字段 | 表达式 | clearValueOnHidden | 业务语义 | 落地状态 |
|------|------|--------|-------------------|----------|---------|
| `ErpInvStockMove` | `sourceLocationId` | `${moveType != 'INCOMING'}` | true | 入库（INCOMING）无内部来源库位（来源由外部供应商/生产单确定） | ✅ F7 |
| `ErpInvStockMove` | `destLocationId` | `${moveType != 'OUTGOING'}` | true | 出库（OUTGOING）无内部去向库位（去向由外部客户/领用确定） | ✅ F7 |
| `ErpAstMaintenance` | `capitalizedAmount` | `${treatment == 'CAPITALIZE'}` | true | 费用化（EXPENSE）时无资本化金额（直接计入当期费用） | ✅ F7 |
| `ErpFinVoucherLine` | `debitAmount` | `${dcDirection == 'DEBIT'}` | true | 贷方分录无借方金额 | 🟡 F4 successor（表达式库预冻结，本计划仅记录） |
| `ErpFinVoucherLine` | `creditAmount` | `${dcDirection == 'CREDIT'}` | true | 借方分录无贷方金额 | 🟡 F4 successor |

**dict 值核实**（实时仓库 ORM）：

- `erp-inv/operation-type`：`INCOMING` / `OUTGOING` / `INTERNAL` / `MANUFACTURE`（4 值，无 TRANSFER）—— `module-inventory/model/app-erp-inventory.orm.xml`
- `erp-ast/maintenance-treatment`：`CAPITALIZE` / `EXPENSE`（2 值）—— `module-assets/model/app-erp-assets.orm.xml:160-164`
- `erp-fin/dc-direction`：`DEBIT` / `CREDIT`（2 值）—— `module-finance/model/app-erp-finance.orm.xml`

## 4. cell-level visibleOn 写法范式

```xml
<form id="edit">
    <layout x:override="replace">
        ...
        sourceLocationId[源库位] destLocationId[目标库位]
        ...
    </layout>
    <cells>
        <cell id="sourceLocationId" clearValueOnHidden="true">
            <visibleOn>${moveType != 'INCOMING'}</visibleOn>
        </cell>
        <cell id="destLocationId" clearValueOnHidden="true">
            <visibleOn>${moveType != 'OUTGOING'}</visibleOn>
        </cell>
    </cells>
</form>
```

**关键点**：

1. `<layout>` 中保留字段占位（`sourceLocationId destLocationId`），由 `<cells>` 注入 `visibleOn` 约束
2. `clearValueOnHidden="true"` 必填——防止用户切换 moveType 后旧值作为隐藏字段提交
3. 同时配置 `view` 与 `edit` form（add form 缺省继承 edit 配置）

### 4.1 反模式

| 不要这样写 | 应该这样写 |
|-----------|-----------|
| `<col ... visibleOn="..."/>` 在 grid list 上 | 字段驱动 visibleOn 仅在 form view/edit cell；list grid 列固定 |
| 漏写 `clearValueOnHidden="true"` | 隐藏字段会保留旧值并被 __save 提交，污染数据 |
| 在 `<layout>` 文本中嵌入 `visibleOn` 表达式 | `<layout>` 仅声明占位，约束写在 `<cells>/<cell>` 子元素中 |
| 仅在 `edit` form 加 visibleOn 不加 `view` | view（查看态）也应一致，避免同一字段在查看/编辑态显示行为不一致 |

## 5. 主数据编码唯一性前置校验范式（async validator on blur）

### 5.1 触发时机裁决

- **采纳方案 A**：`onEvent.blur` 异步触发（用户离开输入框时校验）
- 否决 `onEvent.change`：每键击一次请求，对后端压力过大且 UX 闪烁

### 5.2 后端 @BizQuery 范式

```java
// IBiz 接口
public interface IErpMdMaterialBiz extends ICrudBiz<ErpMdMaterial> {
    @BizQuery
    boolean isCodeUnique(@Name("code") String code,
                         @Optional @Name("excludeId") Long excludeId,
                         IServiceContext context);
}

// BizModel 实现
@Override
@BizQuery
public boolean isCodeUnique(@Name("code") String code,
                            @Optional @Name("excludeId") Long excludeId,
                            IServiceContext context) {
    if (code == null || code.isEmpty()) {
        return true;
    }
    QueryBean query = new QueryBean();
    query.addFilter(eq("code", code));
    if (excludeId != null) {
        query.addFilter(ne("id", excludeId));
    }
    return findCount(query, context) == 0;
}
```

**excludeId 语义**：edit 模式下排除自身 ID（用户编辑现有实体保留原 code 不应误判为冲突）。view模式 add 时 excludeId 为 null。

### 5.3 前端 view.xml onEvent 范式

```xml
<cell id="code">
    <gen-control>
        <c:script><![CDATA[
            return {
                type: 'input-text',
                name: 'code',
                required: true,
                onEvent: {
                    blur: {
                        actions: [
                            {
                                actionType: 'ajax',
                                api: {
                                    method: 'post',
                                    url: '/graphql',
                                    dataType: 'raw',
                                    data: {
                                        query: '${"query{ErpMdMaterial__isCodeUnique(code:\"" + code + "\",excludeId:" + (id || "null") + "){v}}"}'
                                    },
                                    responseType: 'response',
                                    adapt: function(payload) {
                                        const ok = payload?.data?.ErpMdMaterial__isCodeUnique === true;
                                        return { status: ok ? 0 : 500, msg: ok ? '' : '编码已存在' };
                                    }
                                }
                            }
                        ]
                    }
                }
            };
        ]]></c:script>
    </gen-control>
</cell>
```

**关键点**：

1. `onEvent.blur` → `actionType: 'ajax'` 调 `/graphql` endpoint
2. GraphQL query 内联（避免引入额外 API 模板）
3. `adapt` 函数将 GraphQL 响应转为 AMIS 标准状态（status=0 成功 / status>=400 失败 + msg）
4. AMIS 自动以 toast 形式展示 msg，无需手写 ✓/✗ 图标（简化实现，保留语义）

### 5.4 反模式

| 不要这样写 | 应该这样写 |
|-----------|-----------|
| `onEvent.change` 每键击触发 | `onEvent.blur` 离开输入框触发 |
| 在 __save 时才校验唯一性 | 前置到 onEvent.blur |
| 异步校验抛 throw 阻断 blur | 用 adapt 转 AMIS 状态码 + msg 提示 |

## 6. 主数据删除引用预览范式（countReferences + dialog）

### 6.1 覆盖范围裁决

- **采纳方案 A**：本计划仅覆盖 `ErpMdPartner` + `ErpMdMaterial`（最高频引用）
- 引用域限定 purchase（Order/Receive/Invoice）+ sales（Order/Delivery/Invoice）+ inventory（StockMove）共 7 表 count
- 长尾域引用（assets/projects/quality/maintenance/manufacturing/contract/drp 等）归 successor

### 6.2 跨域依赖处理（SPI 模式）

`master-data` 是基础域，**不可反向依赖** purchase/sales/inventory（依赖环约束，见 `docs/architecture/domain-module-split-analysis.md`）。

**采纳方案**：SPI 端口 + @Nullable @Inject（镜像 `IErpMdSkuReferenceChecker` 范式）：

```java
// SPI 在 master-data-dao 声明
public interface IErpMdMaterialReferenceChecker {
    /** 返回 materialId 被各业务单据引用的计数 Map（key=域名，value=行数）。无实现返回空 Map。 */
    Map<String, Long> countReferences(Long materialId);
}

// BizModel 经 @Nullable @Inject 收集
@Inject
@Nullable
protected IErpMdMaterialReferenceChecker materialReferenceChecker;

@Override
@BizQuery
public Map<String, Long> countReferences(@Name("id") Long id, IServiceContext context) {
    if (id == null || materialReferenceChecker == null) {
        return Collections.emptyMap();
    }
    return materialReferenceChecker.countReferences(id);
}
```

**默认空转**：无下游 SPI 实现时返回空 Map（删除直接走原 __delete 路径，UX 与未实现前一致）。下游 purchase/sales/inventory 各域注册 SPI 实现后引用预览生效。

### 6.3 前端 view.xml rowActions 改造范式

```xml
<pages>
    <crud name="main">
        <rowActions x:override="bounded-merge">
            <action id="row-view-button"/>
            <action id="row-update-button" actionType="drawer"/>
            <!-- 改造 row-delete-button：先 countReferences 后判定是否弹引用预览 dialog -->
            <action id="row-delete-button" level="danger" label="删除">
                <api url="@mutation:ErpMdMaterial__delete?id=$id"/>
                <confirmText>确认删除此物料？</confirmText>
                <onEvent>
                    <click>
                        <actions>
                            <!-- Step 1: 调 countReferences -->
                            {
                                actionType: 'ajax',
                                api: {
                                    method: 'post',
                                    url: '/graphql',
                                    data: { query: '${"query{ErpMdMaterial__countReferences(id:" + id + "){k v}}"}' },
                                    responseType: 'response'
                                },
                                outputVar: 'refCount'
                            },
                            <!-- Step 2: 引用 > 0 弹 dialog 阻断；= 0 走原 __delete -->
                            {
                                actionType: 'dialog',
                                condition: '${SUM(refCount.data.ErpMdMaterial__countReferences[*].v) > 0}',
                                dialog: {
                                    title: '无法删除：存在引用',
                                    body: [{
                                        type: 'tpl',
                                        tpl: '该物料被以下单据引用：${JOIN(MAP(refCount.data.ErpMdMaterial__countReferences, item => item.k + ":" + item.v), ", ")}'
                                    }],
                                    actions: [{ type: 'button', actionType: 'close', label: '知道了' }]
                                }
                            },
                            <!-- 0 引用 → 触发原 __delete（确认弹窗后真正调用 mutation） -->
                            {
                                actionType: 'confirm',
                                condition: '${SUM(refCount.data.ErpMdMaterial__countReferences[*].v) == 0}'
                            }
                        ]
                    </click>
                </onEvent>
            </action>
        </rowActions>
    </crud>
</pages>
```

**关键点**：

1. `row-delete-button` 用 `x:override="bounded-merge"`，从 codegen 默认继承并加 `onEvent.click` 拦截
2. `countReferences` 走 GraphQL（与 isCodeUnique 一致）
3. condition 表达式判断是否阻断
4. 0 引用走原 confirm → __delete 路径，行为对用户无感

### 6.4 反模式

| 不要这样写 | 应该这样写 |
|-----------|-----------|
| 直接 `daoFor(ErpPurOrderLine.class)` count | SPI 解耦（master-data 不可反向依赖） |
| 删除时才查询引用 | 删除按钮点击时立即查询（dialog 前置） |
| 引用预览 dialog 不阻断 | dialog 关闭后不调 __delete（condition 表达式守卫） |

## 7. 主数据启用/停用 Switch 控件范式

### 7.1 覆盖范围裁决

- **采纳方案 A**：本计划仅 `ErpMdMaterial` + `ErpMdPartner` 落地 Switch（2 高频实体）
- `ErpMdSubject` 有 status 字段但归 successor（触发条件「按域推进主数据 Switch 控件全覆盖」）
- 理由：保持 3 实体唯一性 + 2 实体 Switch 的最小完整切片，避免范围蔓延

### 7.2 gen-control 范式

```xml
<cell id="status">
    <gen-control>
        <c:script><![CDATA[
            return {
                type: 'switch',
                name: 'status',
                trueValue: 'ACTIVE',
                falseValue: 'INACTIVE',
                onEvent: {
                    change: {
                        actions: [
                            {
                                actionType: 'dialog',
                                condition: '${status == "INACTIVE"}',
                                dialog: {
                                    title: '停用确认',
                                    body: '停用后该实体不可在新单据中选择，是否继续？',
                                    actions: [
                                        {
                                            type: 'button',
                                            actionType: 'ajax',
                                            label: '确认停用',
                                            api: '@mutation:ErpMdMaterial__update/id?id=$id',
                                            data: { status: 'INACTIVE' },
                                            close: true
                                        },
                                        {
                                            type: 'button',
                                            actionType: 'close',
                                            label: '取消'
                                        }
                                    ]
                                }
                            }
                        ]
                    }
                }
            };
        ]]></c:script>
    </gen-control>
</cell>
```

**关键点**：

1. `type: 'switch'` 替代 codegen 默认 `button-group-select`
2. `trueValue: 'ACTIVE'` / `falseValue: 'INACTIVE'` 与 dict 值一致（保留 DB schema 兼容）
3. `condition: '${status == "INACTIVE"}'` 仅停用方向弹确认（启用方向无需提示）
4. 取消按钮关闭 dialog 后，AMIS switch 控件自动回滚视觉状态（form data 未变）

### 7.3 反模式

| 不要这样写 | 应该这样写 |
|-----------|-----------|
| 启用方向也弹确认 | 仅停用方向弹（启用是恢复，无需提示） |
| onEvent.change 直接 __update | 走 dialog 用户确认后才 __update（防误触） |
| Switch 控件 trueValue=1/falseValue=0 | 与 dict 字面量一致（'ACTIVE'/'INACTIVE'），保留 DB schema 兼容 |

## 8. ErpFinVoucherLine dcDirection 切换范式（F4 successor 前置）

> **本节为 F4 finance voucher successor plan 预冻结表达式库**。ErpFinVoucher sub-grid-edit 落地本身属 F4 范畴；本节仅记录范式待引用。

### 8.1 业务语义

凭证录入时每行有 `dcDirection`（借贷方向）+ `debitAmount`（借方金额）+ `creditAmount`（贷方金额）。业务规则：

- `dcDirection == 'DEBIT'` → 仅填 `debitAmount`，`creditAmount` 隐藏并清空
- `dcDirection == 'CREDIT'` → 仅填 `creditAmount`，`debitAmount` 隐藏并清空
- 防止借贷同填（会计基本平衡约束）

### 8.2 visibleOn 表达式（cell 级）

```xml
<cell id="debitAmount" clearValueOnHidden="true">
    <visibleOn>${dcDirection == 'DEBIT'}</visibleOn>
</cell>
<cell id="creditAmount" clearValueOnHidden="true">
    <visibleOn>${dcDirection == 'CREDIT'}</visibleOn>
</cell>
```

### 8.3 onEvent 切换 dcDirection 时清空对方字段（防借贷同填）

由于 `clearValueOnHidden=true` 已确保隐藏字段被清空，**onEvent 切换 dcDirection 无需额外动作**——visibleOn 自动响应 dcDirection 变化，clearValueOnHidden 自动清空变为隐藏的字段。

**反模式（无需手写）**：

```xml
<!-- ❌ 不需要：clearValueOnHidden 已自动处理 -->
<cell id="dcDirection">
    <gen-control>
        <c:script><![CDATA[
            return {
                type: 'button-group-select',
                onEvent: {
                    change: {
                        actions: [
                            { actionType: 'setValue', args: { value: { debitAmount: 0 } }, condition: '${dcDirection == "CREDIT"}' },
                            { actionType: 'setValue', args: { value: { creditAmount: 0 } }, condition: '${dcDirection == "DEBIT"}' }
                        ]
                    }
                }
            };
        ]]></c:script>
    </gen-control>
</cell>
```

## 9. 长尾域扩展参考

后续域（projects/assets/maintenance/quality 等）按本范式补齐字段驱动 visibleOn 时，遵循：

1. **ORM 字典核实**：字段名 + dict 值经实时仓库 `*.orm.xml` 核实（避免误用历史名）
2. **3 步落地**：`<layout>` 保留占位 + `<cells>` 加 `<visibleOn>` + `<clearValueOnHidden="true">`
3. **跨域引用预览**：经 SPI（`IErpMd<Entity>ReferenceChecker`）解耦，下游各域注册实现
4. **唯一性前置校验**：每域至少覆盖 `code` 字段（高频业务实体扩展到业务编号如 `soCode`/`poCode`）
5. **Switch 控件扩展**：按域推进（触发条件「按域推进主数据 Switch 控件全覆盖」）

## 10. 调研参考

| 设计点 | 参考来源 | 应用方式 |
|--------|----------|----------|
| 字段值驱动 visibleOn | NopAuthResource.view.xml | `${resourceType != 'FNPT'}` 范式 → ERP 字段值场景 |
| clearValueOnHidden 防隐藏字段污染 | AMIS 文档 + NopAuthResource | 库位/金额切换场景必填 |
| async validator on blur | AMIS onEvent + GraphQL | 编码唯一性前置校验（避免保存时报错） |
| 删除引用预览 dialog | 管伊佳#Material 引用预览 | dialog 列出引用计数 + 阻断删除 |
| Switch 控件 | AMIS switch + onEvent.change | 主数据启用/停用 + 停用确认 |
| SPI 跨域解耦 | `IErpMdSkuReferenceChecker` 范式 | master-data 反向依赖经端口-适配器解耦 |
