# 看板/报表确定性数值期望值表（1445-2 数据驱动 E2E 断言）

> Owner: `docs/plans/2026-07-08-1445-2-data-driven-e2e-value-assertions.md` Phase 1 Exit Criteria
> 数据基线: `app-erp-all/src/main/resources/_vfs/_init-data/*.csv`（44 CSV：21 主数据 + 23 交易单据，1445-1 固化种子集）
> 聚合口径源: `ErpFinDashboardBizModel` / `ErpSalDashboardBizModel` / `ErpPurDashboardBizModel` / `ErpFinReportBizModel`

## 0. 确定性前提

- Playwright webServer 每次启动前 `rm -f db/erp.mv.db db/erp.trace.db`（fresh-DB 重置）+ `-Dnop.orm.init-database-data=true`，44 CSV 按拓扑序确定性插入。
- 种子集固定（1445-1 固化），故后端聚合结果是确定性的——spec 可硬编码期望值。
- **日期漂移防护**：销售/采购看板 KPI 默认区间依赖服务端当前日期（`today.withDayOfMonth(1)`→`today`）。种子发票业务日期为 2026-07。为消除「服务端日期 ≠ 种子日期」致 KPI=0 的非确定性，断言 spec **显式传 `startDate=2026-07-01` / `endDate=2026-07-31`** 覆盖种子日期，使 KPI 确定性不依赖运行时日期。财务看板 KPI 传 `periodId=1`（种子唯一期间）显式锁定种子期间。
- **seed 漂移同步机制**：若 1445-1 seed CSV 变更（行增减/金额改动），本表期望值须同步更新。Closure Gates 含「期望值表 ↔ seed CSV 一致性」检查。

## 1. 种子数据关键行（期望值派生依据）

### 1.1 GL 余额（`erp_fin_gl_balance.csv`，全部 periodId=1）

| balanceId | subjectId | 科目(code/name/class/direction) | periodDr | periodCr | closingDr | closingCr |
|-----------|-----------|---------------------------------|----------|----------|-----------|-----------|
| 1 | 2 | 1002 银行存款 / ASSET / DEBIT | 1130.00 | 960.50 | 169.50 | 0 |
| 2 | 3 | 1122 应收账款 / ASSET / DEBIT | 1130.00 | 1130.00 | 0 | 0 |
| 3 | 4 | 1405 库存商品 / ASSET / DEBIT | 960.50 | 0 | 960.50 | 0 |
| 4 | 5 | 2202 应付账款 / LIABILITY / CREDIT | 960.50 | 960.50 | 0 | 0 |
| 5 | 6 | 5001 主营业务收入 / INCOME / CREDIT | 0 | 1130.00 | 0 | 1130.00 |

### 1.2 AR/AP 辅助账（`erp_fin_ar_ap_item.csv`，全部 status=SETTLED）

| itemId | direction | sourceBillCode | amountFunctional | openAmountFunctional | status |
|--------|-----------|----------------|------------------|----------------------|--------|
| 1 | PAYABLE | PINV-2026-001 | 960.50 | 0 | SETTLED |
| 2 | PAYABLE | PAY-2026-001 | 960.50 | 0 | SETTLED |
| 3 | RECEIVABLE | SINV-2026-001 | 1130.00 | 0 | SETTLED |
| 4 | RECEIVABLE | REC-2026-001 | 1130.00 | 0 | SETTLED |

> 4 行全部 SETTLED / openAmount=0 —— 这是 1445-1 Decision (B)（首批仅留 open item，全结算态）的直接结果。

### 1.3 交易发票（驱动 sales/purchase 看板 KPI）

| 发票 | code | businessDate | amountFunctional | docStatus | posted |
|------|------|--------------|------------------|-----------|--------|
| 采购发票 | PINV-2026-001 | 2026-07-05 | 850.00 | ACTIVE | true |
| 销售发票 | SINV-2026-001 | 2026-07-06 | 1000.00 | ACTIVE | true |

### 1.4 订单

| 订单 | code | businessDate | docStatus |
|------|------|--------------|-----------|
| 采购订单 | PO-2026-001 | 2026-07-01 | ACTIVE |
| 销售订单 | SO-2026-001 | 2026-07-02 | ACTIVE |

## 2. 看板 KPI 期望值（GraphQL `__getDashboardKpi`）

### 2.1 财务看板 `ErpFinDashboard__getDashboardKpi(periodId=1)`

口径：revenue/expense 取 GL 余额损益类科目本期发生净额（INCOME activity = credit-debit；EXPENSE/COST activity = debit-credit）；ar/apBalance 取 ar_ap_item OPEN+PARTIAL openAmount 之和。

| KPI 字段 | 期望值 | 派生 |
|----------|--------|------|
| `revenue` | **1130** | INCOME 主营业务收入(5001) activity = 1130(credit) - 0(debit) = 1130 |
| `expense` | 0 | 无 EXPENSE/COST 类 GL 余额行（科目 7/8 未在 gl_balance seed） |
| `netProfit` | **1130** | revenue - expense = 1130 - 0 |
| `bankBalance` | 0 | 无 `erp_fin_fund_account` seed → Σ BANK 余额 = 0 |
| `arBalance` | 0 | ar_ap_item 全 SETTLED → 无 OPEN/PARTIAL 行 |
| `apBalance` | 0 | 同上 |

**主断言**：`revenue === 1130`、`netProfit === 1130`（非零确定性）。

### 2.2 销售看板 `ErpSalDashboard__getDashboardKpi(startDate=2026-07-01, endDate=2026-07-31)`

口径：salesAmount = Σ posted 发票 amountFunctional（businessDate 在区间内）；orderCount = ACTIVE 订单数；invoiceCount = 区间内 posted 发票数；conversionRate = invoiceCount/orderCount；arBalance 跨域读 ar_ap_item(RECEIVABLE, OPEN+PARTIAL)。

| KPI 字段 | 期望值 | 派生 |
|----------|--------|------|
| `salesAmount` | **1000** | 1 张 posted 销售发票 amountFunctional=1000（07-06 在区间内） |
| `orderCount` | **1** | 1 张 ACTIVE 销售订单 |
| `invoiceCount` | **1** | 1 张 posted 销售发票在区间内 |
| `conversionRate` | **1.0** | 1/1 |
| `arBalance` | 0 | ar_ap_item 全 SETTLED |

**主断言**：`salesAmount === 1000`、`orderCount === 1`（非零确定性）。

### 2.3 采购看板 `ErpPurDashboard__getDashboardKpi(startDate=2026-07-01, endDate=2026-07-31)`

口径：purchaseAmount = Σ ACTIVE 发票 amountFunctional（区间内）；orderCount = ACTIVE 订单数；apBalance 跨域读 ar_ap_item(PAYABLE, OPEN+PARTIAL)；onTimeRate = receive.businessDate ≤ 关联 order.deliveryDate 数 / 有 order 的 receive 总数。

| KPI 字段 | 期望值 | 派生 |
|----------|--------|------|
| `purchaseAmount` | **850** | 1 张 ACTIVE 采购发票 amountFunctional=850（07-05 在区间内） |
| `orderCount` | **1** | 1 张 ACTIVE 采购订单 |
| `apBalance` | 0 | ar_ap_item 全 SETTLED |
| `onTimeRate` | 0.0 | receive.orderId=1 存在但 PO 无 deliveryDate → orderDeliveryMap 为空 → delivery=null → onTime=0/totalWithOrder=1 |

**主断言**：`purchaseAmount === 850`、`orderCount === 1`（非零确定性）。

## 3. 报表渲染数值期望 token（GraphQL `ErpFinReport__renderHtml`）

> 断言范式：直接 POST `renderHtml` GraphQL query 取 HTML 字符串，断言含期望数值 token（绕过 AMIS 渲染层 DOM 抖动）。数值经 `numberFormat=#,##0.00` 格式化，spec 端剥离千分位逗号后比对（`960.50` / `1130.00`）。

### 3.1 资产负债表 `balance-sheet`（periodId=1）

数据集（ASSET/LIABILITY 段，closingDr/Cr 差额）：ASSET 银行存款(1002)=169.50、应收账款(1122)=0、库存商品(1405)=960.50、LIABILITY 应付账款(2202)=0。

> **实测渲染行为（运行时核实）**：种子报表模板 `balance-sheet.xpt.xml` 数据行按 `section` 字段展开（`expandType="r" field="section"`），故同 section 仅渲染首条记录——ASSET 段显示 `银行存款 169.50`，LIABILITY 段显示 `应付账款 0.00`；库存商品(1405)/应收账款(1122) 同属 ASSET 段被折叠。合计行 SUM 未渲染值（空）。此为 0504-2 种子报表模板既定行为，修改属 Non-Goal（不修改生产模板）。

**期望 token**（HTML 含）：标题 `资产负债表`；ASSET 段科目名 `银行存款`；数值 `169.50`（银行存款期末余额，剥离逗号后匹配）。

### 3.2 利润表 `income-statement`（periodId=1）

数据集（PnL 段，periodDr/Cr 差额）：

| section | 科目 | 本期发生额 |
|---------|------|------------|
| INCOME | 5001 主营业务收入 | 1130.00 |

**期望 token**：标题 `利润表`；科目名 `主营业务收入`；数值 `1130.00`（剥离逗号后）；净利润行标签 `净利润`。

### 3.3 AR/AP 账龄表 `ar-ap-aging`（asOfDate=2026-07-08）

数据集（OPEN+PARTIAL 行）：**空**（4 行 ar_ap_item 全 SETTLED，1445-1 Decision B 直接结果）。

**期望 token**：标题 `应收应付账龄分析表`（渲染结构存在）；数据段为空（无 RECEIVEIVABLE/PAYABLE 明细行）——确定性反映「全结算」态。此为 Decision B 的端到端数值链证明（seed 全结算 → 账龄报表无未核销明细）。

## 4. 断言范式 Decision（Phase 1 item 2）

**选择**：**直接 GraphQL query 取值断言**。

- **看板**：spec 内 `page.request.post('/graphql', { query, variables })`（复用 UI 登录会话 cookie）取后端 `getDashboardKpi` 原始返回 Map，与上方期望值表逐字段 `expect(Number(field)).toBe(expected)` 比对。辅以 AMIS DOM 文本「KPI 关键词存在」弱断言兜底（既有 smoke 不动）。
- **报表**：`page.request.post` 取 `renderHtml` 返回 HTML 字符串，断言含期望数值 token（剥离千分位逗号后匹配）。

**替代方案（rejected）**：
- (a) 仅 AMIS DOM 文本解析 —— AMIS 渲染层数值格式化/千分位/币种符号致 DOM 文本抖动，断言脆弱。
- (b) 后端 Java 测试覆盖 —— 0935-1/1606-1 已覆盖聚合口径，但不验证「seed → GraphQL → 浏览器/HTTP」端到端数值链，本计划价值正是该链。

**残留风险与防护**：seed 行变更致期望值漂移 → 期望值表标注 seed 依赖（§1）；1445-1 seed 变更须同步更新本表，Closure Gates 含一致性检查。日期漂移（sales/purchase 默认区间依赖运行时日期）→ spec 显式传 startDate/endDate 锁定种子日期区间。
