# 跨域凭证回链与双向导航范式（Voucher Back-Link Patterns）

> Owner docs: `docs/design/cross-doc-navigation-patterns.md`（同域 FK 导航范式，本文件为其跨域字符串指针对偶）、`docs/architecture/view-and-page-strategy.md`
> 落地计划：`docs/plans/2026-07-23-1408-2-cross-domain-voucher-back-link.md`
> 后端真相源：`module-finance/model/app-erp-finance.orm.xml`（`ErpFinVoucherBillR` 业财回链表 + `voucher` to-one 关系）

## 1. 目的与范围

固化「业务单据 ↔ 会计凭证」跨域双向导航的标准范式。与 `cross-doc-navigation-patterns.md` 的区别：

| 维度 | cross-doc-navigation（F9） | voucher-back-link（本范式） |
|------|---------------------------|----------------------------|
| 关联性质 | 同域 FK（`orderId` 等），`fixedProps` 子表 drawer | 跨域字符串指针（`billCode`），非 FK |
| 查询机制 | `__findPage filter_<fk>=${id}`（Long FK eq） | `__findPage filter $type:eq billCode`（字符串）+ `voucher.*` relation selection |
| 落点 | 各域自身 view.xml row-action + `ref-*.page.yaml` | 集中在 finance 域 2 个 page.yaml，各域 view.xml 引用 |
| 导航方向 | 单向（父→子查看 / 子→父跳转） | 双向（业务→凭证查看 + 凭证→业务跳转） |

**适用**：任意业务单据详情查看其过账产物凭证；任意凭证反查源业务单据并跳转。

**不适用**：凭证行级下钻（subjectCode/debitAmount 逐行）→ F16 范畴；凭证实时刷新/推送 → notify successor。

## 2. 数据模型基础

`ErpFinVoucherBillR`（业财回链表，过账产物）：

- 原生列：`voucherId`(Long) / `billType`(String=ErpFinBusinessType enum name) / `billCode`(String=源单据 code，可能带前后缀) / `billLineCode` / `businessType`(=billType)
- `voucher` to-one 关系（`voucherId → ErpFinVoucher.id`，xmeta `queryable="true"`）：经 relation selection 单步取回 `code` / `voucherType` / `postingType`(NORMAL/REVERSAL/BUDGET) / `isReversed` / `totalDebit` / `totalCredit`

**billCode 编码规则**（关键）：
- 直接过账源单据：`billCode = 源单据.code`（无修饰）。如 `AP_INVOICE → invoice.code`。
- 单据生成多张凭证时带判别后缀：`code + "-PV"`(生产差异) / `"-MI"`(制造发料/维修备件) / `"-SI"/"-SR"/"-SF"`(委外) / `"-ML"`(维修工时)。
- 系统生成凭证带前缀：`COMMITMENT-`(订单承诺) / `BUDGET-`(预算) / `INTERCOMPANY-`(内部交易) / `ELIMINATION-`(合并抵销) / `PL-`/`FX-REVAL-`/`BAD-DEBT-RESERVE-`(期末/汇兑/坏账)。
- 折旧：`asset.code + "#" + period`。

**权威枚举**：`ErpFinBusinessType`（`module-finance/erp-fin-dao/.../ErpFinBusinessType.java`），**非** `erp-fin/business-type` 字典（字典与枚举刻意分歧，字典缺部分类型）。

## 3. 双向导航实现范式

### 3.1 业务→凭证（Phase 1）：row-action drawer + 手写 page.yaml + service

**范式裁决（Phase 0）**：采 **方案 (b)** row-action drawer 打开手写 `page.yaml`，内嵌 AMIS `service` + raw GraphQL。否决方案 (a) form tab 内嵌 service（无 tabs 容器可复用的域无法落地）与 (c) gen-control service cell（详情表单 cell 复杂、9 实体重复）。手写 page.yaml 而非 `fixedProps` GenPage，因后者无法展示 `voucher.*` 嵌套关系字段。

**父页 view.xml（各域业务单据）**：
```xml
<action id="row-view-voucher-button" label="关联凭证" actionType="drawer">
    <drawer page="/erp/fin/pages/ErpFinVoucherBillR/voucher-by-bill.page.yaml" size="lg">
        <data>
            <billCode>${code}</billCode>
        </data>
    </drawer>
</action>
```

**子页 page.yaml（finance 域共享，跨域引用）** — service + raw GraphQL：
```yaml
- type: service
  api:
    url: /graphql
    method: post
    dataType: raw
    data:
      query: "query(${'$'}f:Map,${'$'}lim:Int){ ErpFinVoucherBillR__findPage(query:{offset:0,limit:${'$'}lim,filter:${'$'}f}){ items{ id billType billCode businessType voucher{ id code postingType isReversed totalDebit totalCredit } } total } }"
      variables:
        lim: 50
        f:
          $type: eq
          name: billCode
          value: "${billCode}"
    adaptor: |
      ...reshaped items with flattened voucher fields...
  body:
    - type: table
      source: "${items}"
      columns: [ voucherCode, businessType, postingType(红冲着色 tpl), totalDebit, totalCredit, isReversed ]
```

**关键点**：
- **row-action `<data>` 用 plain `${code}`（非 `${'$'}{code}` 转义）**——row-action data 经 XView 编译为按行解析的 AMIS 表达式；`${'$'}{code}` 转义会 emit 字面 `${code}` 不被解析（实测渲染为字面文本 `${code}`）。`${id}` 同理（对偶于 ErpPurOrder ref-order `<orderId>${id}</orderId>` 范式）。
- **filter 运算符限制（实测）**：`billCode` 服务端仅允许 `[eq, in, dateBetween, dateTimeBetween]`，`like`/`contains` 禁用（`nop.err.biz.prop-not-support-filter-op`）。故用 `$type:eq` 精确匹配 `billCode`。eq 命中条件：billCode = entity.code 无修饰（直接过账源单据：Invoice/StockMove/Payment/Receipt 等）。
- **带前后缀的 billCode（PO 承诺 `COMMITMENT-`/生产差异 `-PV`/资产折旧 `code#period` 等）无法经单值 eq 命中**，归 successor（需后端开启 like，或前端按实体构造派生 billCode，或 `in` 候选——但 AMIS 经 api.data.variables 标量解析已验证、列表项解析未验证，故当前用 eq 标量保可靠）。
- **TreeBean filter 须经 `$f:Map` 变量传递**——inline `$type` 会被 GraphQL 误解析为变量引用（`nop.err.commons.text.scan-invalid-var`）。`variables.f` = `{$type:'eq', name:'billCode', value:'${billCode}'}`。
- `voucher { ... }` relation selection 单步取回凭证头字段（Phase 0 PoC + E2E 验证 `voucher` prop `queryable="true"` 支持嵌套选择，postingType/totalDebit/totalCredit/isReversed 均可达）。
- page.yaml 中 `${billCode}` = AMIS data scope 变量（drawer 注入，标量解析已验证）；`${'$'}f` = GraphQL `$f` 变量引用。

### 3.2 凭证→业务（Phase 2）：voucherId 反查 + 静态路由映射

**范式裁决（Phase 0 Decision 2）**：采 **方案 (a)** 静态 `billType → 目标实体路由` 映射表（编译期确定、简单可靠）。否决 (b) xlib 注册式（增加配置层无收益）与 (c) 后端返回路由（改后端，超出实现变更范围）。新增 businessType 须改映射表，归 successor。

**父页 ErpFinVoucher.view.xml**：
```xml
<action id="row-view-source-bills-button" label="源单据" actionType="drawer">
    <drawer page="/erp/fin/pages/ErpFinVoucherBillR/bills-by-voucher.page.yaml" size="lg">
        <data>
            <voucherId>${'$'}{id}</voucherId>
        </data>
    </drawer>
</action>
```

**子页 page.yaml** — `voucherId` eq 反查 + `cleanCode` 剥离前后缀 + 路由映射 + `operation` 列 `actionType: link` 跳转：
```yaml
query: "query(${'$'}vid:Long,...){ ErpFinVoucherBillR__findPage(query:{...,filter:{voucherId:{${'$'}eq:${'$'}vid}}}){ items{ id billType billCode businessType } total } }"
adaptor: |
  const routeMap = { AP_INVOICE: '/ErpPurInvoice-main', ... };
  function cleanCode(code){ 剥离 #period / -(PV|MI|SI|SR|SF|ML) / ^(COMMITMENT-|BUDGET-|...) ; }
  items.map(r => ({..., link: routeMap[r.billType] ? routeMap[r.billType]+'?filter_code='+encodeURIComponent(cleanCode(r.billCode)) : ''}))
columns:
  - { name: billType }
  - { name: billCode }
  - type: operation
    buttons:
      - { type: button, actionType: link, link: "${link}", visibleOn: "${!!link}" }
```

## 4. billType → 目标实体路由映射表

> 来源：各域 PostingDispatcher 写入 `billType = ErpFinBusinessType.<CONST>.name()`（`ErpFinPostingProcessor.java:827`）。路由 = `/<EntityShortName>-main`。

### 4.1 直接过账源单据（billCode = entity.code，cleanCode 无修饰）

| billType | 目标实体 | 路由 |
|----------|---------|------|
| AP_INVOICE | ErpPurInvoice | /ErpPurInvoice-main |
| PAYMENT | ErpPurPayment | /ErpPurPayment-main |
| PURCHASE_RETURN | ErpPurReturn | /ErpPurReturn-main |
| AR_INVOICE | ErpSalInvoice | /ErpSalInvoice-main |
| RECEIPT | ErpSalReceipt | /ErpSalReceipt-main |
| SALES_RETURN | ErpSalReturn | /ErpSalReturn-main |
| PURCHASE_INPUT / SALES_OUTPUT / MANUFACTURING_RECEIPT / PURCHASE_PRICE_VARIANCE | ErpInvStockMove | /ErpInvStockMove-main |
| LANDED_COST | ErpInvLandedCost | /ErpInvLandedCost-main |
| COST_ADJUSTMENT | ErpInvCostAdjust | /ErpInvCostAdjust-main |
| OWNERSHIP_TRANSFER / INTERCOMPANY_SALE / INTERCOMPANY_PURCHASE | ErpInvOwnershipTransfer | /ErpInvOwnershipTransfer-main |
| CAPITALIZATION | ErpAstCipCapitalization | /ErpAstCipCapitalization-main |
| DISPOSAL | ErpAstDisposal | /ErpAstDisposal-main |
| VALUE_ADJUSTMENT | ErpAstValueAdjustment | /ErpAstValueAdjustment-main |
| ASSET_SPLIT / ASSET_MERGE | ErpAstSplit / ErpAstMerge | /ErpAst*-main |
| ASSET_INVENTORY_ADJUSTMENT | ErpAstInventory | /ErpAstInventory-main |
| MAINTENANCE_EXPENSE / MAINTENANCE_CAPITALIZATION | ErpAstMaintenance | /ErpAstMaintenance-main |
| EXPENSE_CLAIM | ErpFinExpenseClaim | /ErpFinExpenseClaim-main |
| EMPLOYEE_ADVANCE / EMPLOYEE_ADVANCE_SETTLE | ErpFinEmployeeAdvance | /ErpFinEmployeeAdvance-main |
| NOTES_RECEIVABLE_* | ErpFinNotesReceivable | /ErpFinNotesReceivable-main |
| NOTES_PAYABLE_* | ErpFinNotesPayable | /ErpFinNotesPayable-main |
| CREDIT_FACILITY_INTEREST | ErpFinCreditFacility | /ErpFinCreditFacility-main |
| BAD_DEBT_WRITE_OFF / BAD_DEBT_RECOVERY | ErpFinBadDebt | /ErpFinBadDebt-main |
| BANK_RECON_ADJ | ErpFinBankReconciliation | /ErpFinBankReconciliation-main |
| BUDGET_SCENARIO | ErpFinBudgetScenario | /ErpFinBudgetScenario-main |
| CONSOLIDATION_ELIMINATION | ErpFinConsolidationElimination | /ErpFinConsolidationElimination-main |
| SALARY / SALARY_PAYMENT | ErpHrSalary | /ErpHrSalary-main |
| FREIGHT | ErpLogShipment | /ErpLogShipment-main |
| NCR_SCRAP | ErpQaNcr | /ErpQaNcr-main |
| PROJECT_COST_COLLECTION | ErpPrjTimesheet | /ErpPrjTimesheet-main |
| PROJECT_SETTLEMENT | ErpPrjProjectSettlement | /ErpPrjProjectSettlement-main |

### 4.2 带后缀/前缀（cleanCode 剥离后过滤）

| billType | 目标实体 | billCode 修饰 | cleanCode 规则 |
|----------|---------|--------------|----------------|
| PRODUCTION_VARIANCE | ErpMfgWorkOrder | `woCode + "-PV"` | 去尾 `-PV` |
| MANUFACTURING_ISSUE | ErpMfgMaterialIssue | `issue.code + "-MI"` | 去尾 `-MI` |
| SUBCONTRACT_ISSUE/RECEIPT/FEE | ErpMfgSubcontractOrder | `order.code + "-SI/-SR/-SF"` | 去尾 `-S?` |
| MAINTENANCE_ISSUE | ErpMntSparePartUsage | `usage.code + "-MI"` | 去尾 `-MI` |
| MAINTENANCE_LABOR | ErpMntVisit | `code + "-ML"` | 去尾 `-ML` |
| DEPRECIATION | ErpAstAsset | `asset.code + "#period"` | 去 `#...` |
| PURCHASE_ORDER_COMMITMENT | ErpPurOrder | `"COMMITMENT-" + order.code` | 去前缀 `COMMITMENT-` |
| BUDGET_SCENARIO | ErpFinBudgetScenario | `"BUDGET-" + scenario.code` | 去前缀 `BUDGET-` |
| INTERCOMPANY_* | ErpInvOwnershipTransfer | `"INTERCOMPANY-" + ...` | 去前缀 `INTERCOMPANY-` |
| CONSOLIDATION_ELIMINATION | ErpFinConsolidationElimination | `"ELIMINATION-" + ...` | 去前缀 `ELIMINATION-` |

### 4.3 系统生成（无源单据，link='' 不显示跳转）

`PERIOD_CLOSE` / `EXCHANGE_GAIN_LOSS` / `PROFIT_TO_RETAINED_EARNINGS` / `BAD_DEBT_RESERVE` / `BAD_DEBT_RELEASE` / `BUDGET_SCENARIO_CARRY_FORWARD` — billCode 为合成前缀（`PL-`/`FX-REVAL-`/`BAD-DEBT-RESERVE-` 等），无对应业务单据，operation 列 `visibleOn="${!!link}"` 隐藏跳转按钮。

## 5. 红冲凭证可视化规则

| 字段 | 来源 | 渲染 |
|------|------|------|
| postingType | `voucher.postingType`（relation selection） | `REVERSAL` → `label-danger`（红）；`NORMAL` → `label-success`（绿） |
| isReversed | `voucher.isReversed`（relation selection） | `true` → `label-danger "已红冲"`；`false` → `label-default "正常"` |

> 红冲凭证与原凭证共用同一 `billCode`（reverseProcess 同 code 写 voucher_bill_r），故 `billCode` 无法区分原/红字；唯一区分项是 `postingType`（NORMAL/REVERSAL）。业务→凭证区同时展示二者，用户可辨。

## 6. 反模式自检

| 不要这样写 | 应该这样写 |
|-----------|-----------|
| row-action `<data>` 用 `${'$'}{code}` 转义（emit 字面 `${code}` 不解析） | 用 plain `${code}`（XView 编译为按行解析的 AMIS 表达式，对偶 ErpPurOrder `<orderId>${id}</orderId>`） |
| 用 `$contains`/`like` 过滤 billCode（服务端禁用，仅 [eq,in,dateBetween,dateTimeBetween]） | 用 `$type:eq` TreeBean（billCode=entity.code 无修饰时命中） |
| TreeBean filter inline 写 `{$type:"eq",...}`（`$type` 被误解析为 GraphQL 变量） | 经 `$f:Map` 变量传递（`variables.f={$type,name,value}`） |
| 在各域 view.xml 内嵌 `<service>` 查凭证（9 实体重复 + XLang 转义复杂） | 集中 finance 域 1 个 `voucher-by-bill.page.yaml`，各域 row-action drawer 引用 |
| 用 `fixedProps` GenPage 展示凭证（无法取 voucher.* 嵌套字段） | 手写 page.yaml + raw GraphQL relation selection |
| 凭证→业务用 billType 字典（与枚举分歧）做路由 | 用 `ErpFinBusinessType` 枚举 name 做映射键 |
| 跳转链接用 billCode 原值（带后缀/前缀导致 filter_code 失配） | 经 `cleanCode` 剥离修饰后构造 `?filter_code=` |
| 反向跳转 `<a href>`（非 SPA 路由） | AMIS `type: operation` + `type: button` + `actionType: link`（SPA 导航） |
| 后端新增跨域 `@BizQuery` Finder | 用 codegen 已生成的 `ErpFinVoucherBillR__findPage` + filter + relation selection |
| 把 billCode 当 FK 用 `fixedProps` | billCode 是字符串指针非 FK，用 `$type:eq` filter |

## 7. 落地清单

- **Phase 1（业务→凭证）**：`module-finance/.../ErpFinVoucherBillR/voucher-by-bill.page.yaml`（共享）+ 9 实体 view.xml row-action：purchase(ErpPurOrder/Receive/Invoice)、sales(ErpSalOrder/Delivery/Invoice)、inventory(ErpInvStockMove)、mfg(ErpMfgWorkOrder)、assets(ErpAstAsset)。
- **Phase 2（凭证→业务）**：`module-finance/.../ErpFinVoucherBillR/bills-by-voucher.page.yaml` + `ErpFinVoucher.view.xml` row-action。
- **回归**：`tests/e2e/business-actions/voucher-back-link.action.spec.ts`（双向数据可达性 + Phase 0 PoC relation selection）。

## 8. 参考文件

- `../nop-entropy/docs-for-ai/02-core-guides/dql-query.md` — `$contains`/`$eq` filter 算子、`query:{filter}` 语法
- `../nop-entropy/docs-for-ai/02-core-guides/page-dsl-pattern-catalog.md` — service + raw GraphQL 范式
- `docs/design/cross-doc-navigation-patterns.md` — 同域 FK 导航范式（本范式的对偶）
- `module-cs/erp-cs-web/.../ErpCsTicketAction/timeline.page.yaml` — service + raw GraphQL + adaptor 范式来源
