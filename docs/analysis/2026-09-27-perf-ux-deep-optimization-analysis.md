
# 性能与 UI/UX 深度优化分析报告

> 日期：2026-09-27
>
> **mission 收官（2026-09-28）**：修复批次 1-5 + A9 补遗批（`2026-09-28-1418-4`）全部 completed 并独立提交（f11c3d5ae → 18bc67af2 共 6 个提交）。收官增量重扫描确认：除已收口的 A9 漏盘 4 站点（批次 6）外无新的可优化项；残留全部处于 §6 Deferred/各计划 Deferred But Adjudicated 登记状态（触发条件驱动）。 mission 过程沉淀 lessons/25（门控预勾）与 lessons/26（stale jar 验证旧态）。
> 性质：实施导向的深度分析——在既有审查（`2026-09-14-complex-page-ux-review.md`、ai-check 各轮、optimization-audit-roadmap 框架）基础上，由 3 个独立只读分析通道（后端性能 / UI-UX 现状核验 / 前端性能与 flux 合规）对 HEAD 实仓完成全量扫描，产出可执行优化工作项。**本报告只分析不动代码；修复按后续 plan 逐批执行。**
> 量化基线：手写 Java（src/main）3,412 文件；view.xml 368-370 页（两种口径）；page.yaml 855；orm.xml 19 个（索引定义 313 处）。
> 关联：`docs/analysis/2026-09-14-complex-page-ux-review.md`（UX 基线）、`docs/backlog/optimization-audit-roadmap.md`（research 框架，其 M1-M5 报告仍 todo；本报告不替代也不吞并它）

---

## 1. 结论先行

| 面向 | 核心结论 |
|------|---------|
| 后端性能 | **看板/报表读路径是最大热点**：4 个核心域看板（sal/pur/fin/inv）把企业量级大表全量物化到内存求和/分组，另有 8+ 个域的 Report 查询无界。修复范式在同仓库已有正面先例（`findCustomerTopN` 等 SQL `GROUP BY`/`sum()` 写法），属机械推广。索引覆盖总体良好（19/19 orm.xml 有索引），仅 2 处复合索引缺口。事务内外发（汇率 HTTP、物流网关重试 sleep 最长 60s）是低频但高危害的结构问题 |
| UI/UX | **2026-09-14 报告的 5 处 P1 全部原样未修**（硬编码参数/死按钮），另发现 1 处新 P1（预算方案驳回/作废无二次确认——全库危险动作覆盖率 93.5% 的唯一缺口）。一致性面：65 页编辑 drawer 无显式尺寸、15 页状态机派生字段在编辑表单可改、43 页双查询入口并存。正面：菜单结构/i18n/校验标识/危险确认范式整体健康 |
| 前端渲染 | 65 页列表超 AMIS 列虚拟化阈值 13 列（峰值 40 列 ErpLogShipment），与 09-14 基线（67 页）相比基本未收敛；最重编辑面为 ErpFinVoucherLine 17 列行内编辑子表；flux 门禁报告（09-11）落后于导出产物（09-21），须重跑对账（325 error 全部为已裁决基线族，预期零新增） |

**裁决总表**（修复批次见 §5）：

- **本轮修复**：P1×6（UX 交互缺陷，已由 Plan `2026-09-27-0318-1` 修复），看板/报表读路径 SQL 聚合 + 无界治理、非保护区 N+1 批量化、事务内外发重排（汇率）、2 处 orm.xml 复合索引（走保护区双批准）、UX 一致性批量（状态字段 readOnly / drawer 尺寸 / 空态 / 死菜单等）
- **Deferred But Adjudicated**（触发条件驱动，见 §6）：fin 现金流量表聚合重写（报表口径逐分对账风险）、物流/B2B 网关重试移出事务（行为契约变更）、fin-posting 热路径微优化（收益低风险高）、循环 save 批量化（需平台批量 API spike）、flux 18k warning 模板级收敛（生成层问题，app 仓不可达）

---

## 2. 后端性能发现（全量清单）

> **状态回填（2026-09-27）**：§2.1 A1/A2/A3/A6/A7/A8/B1/D1-G1 中读路径项与 §2.2 A9/B2 等已由 plan `2026-09-27-0325-2` 处置（sal/pur/inv/fin 看板 SQL 聚合 + 三单匹配 N+1 消除 + 8 文件无界治理 + 汇率 HTTP 事务内问题归 Plan 3；D1 的 SUM 聚合接口部分为该计划内 Deferred/白名单处置）；A4/A5/G2/G3/C1/B6/I2 按 §6 Deferred 裁决保留。
> **状态回填（2026-09-28）**：§2.1 G1 批量部分与 §2.2 E2/E3/B3/B4/B5/J1-J3 已由 plan `2026-09-28-0835-1` 处置（5 站点批量预载 + notify 循环外提升 + fin KPI scope 单次解析 + J1 实证零站点更正 + J3 平台 bytesToHex）；G1 事务外重排改判 Deferred（实时基线：factory 仅 mock provider、config 默认关；触发=真实 provider 立项，按 external-api-integration-pattern §6.3/AP5 落地）。

### 2.1 P1——数据量增长会直接拖垮

| # | 发现 | 位置 | 证据 | 修复方向 |
|---|------|------|------|---------|
| A1 | 销售看板 KPI/Trend 全量发票物化内存求和（Trend 为 12 个月全量） | `module-sales/erp-sal-service/.../dashboard/ErpSalDashboardBizModel.java:74,:105,:219-226` | `List<ErpSalInvoice> invoices = loadPostedInvoicesInRange(from,to); salesAmount = salesAmount.add(...)` | 复用同文件 `findCustomerTopN:134-140` 的 `QueryFieldBean.mainField(...).sum()` SQL 聚合范式 |
| A2 | 采购看板同型 + `findVendorTopN` 无界 + 及时率 `setLimit(5000)` 静默截断 | `module-purchase/erp-pur-service/.../ErpPurDashboardBizModel.java:83,:115,:142,:281,:303` | `loadActiveInvoicesInRange(null,null)` 全表 | SQL GROUP BY supplierId + ORDER BY SUM DESC LIMIT N；onTimeRate SQL 聚合 |
| A3 | 财务看板 ArApItem 全量加载内存求和 + BankBalance 同型 | `module-finance/erp-fin-service/.../ErpFinDashboardBizModel.java:228-242,:216-226` | `for(...) sum = sum.add(...)` | `openAmountFunctional` SQL SUM（读路径，不改口径） |
| A4 | 财务现金流量表：一个期间全部凭证+全部凭证行物化，IN 无界；直接法/间接法各加载一次 | `ErpFinReportBizModel.java:511-529` | `findAllByQuery(vq)... in("voucherId", voucherIds)` | **Deferred**：报表数字须与 owner doc 逐分对账；最小化改造仅「直接/间接共享一次加载」，完整 SQL 聚合重写待专门 plan |
| A5 | AR/AP 账龄报表全量物化（`openItemsQuery` 无 direction/org 过滤、无 limit） | `ErpFinReportBizModel.java:407-431,:532-538` | 同上 | **Deferred**（同 A4 保护 adjacency；账龄 CASE WHEN 分桶聚合待专门 plan） |
| A6 | 库存看板 KPI 同请求**两次**全量扫描（sumMoveQty 与 sumOutgoingCost 各自 loadDoneMoves+loadMoveLines 巨型 IN） | `module-inventory/erp-inv-service/.../ErpInvDashboardBizModel.java:82,:298,:326,:347-362` | 同一 KPI 请求整集加载两次 | 合并为一次 SQL GROUP BY moveType 聚合 quantity/totalCost |
| A7 | 库存趋势 StockLedger 12 个月全量物化按月分组（append-only 全库增长最快表） | 同文件 `:112,:364-370` | `loadLedgersInRange` | SQL GROUP BY 月份 |
| A8 | 滞销预警全量出库记录扫描求每物料最后出库日 | 同文件 `:417-439` | `loadLastOutgoingDates` | SQL `MAX(businessDate) GROUP BY materialId` |
| B1 | 采购三单匹配预警 N+1：每张发票 3-4 次子查询（invoiceLine→receiveLine→orderLine + 循环外 partner 查询） | `ErpPurDashboardBizModel.java:189-208,:314-358` | `for(inv) hasPriceVariance(inv.getId())` 内三连查 | 一次性 `in("invoiceId", ids)` 批量取三类行内存建 Map；supplier 名批量 Map |
| D1 | `findOpenItems` 无界返回财务最大表（无 limit、无 org scope），6 处生产调用 | `ErpFinArApItemBizModel.java:53-61`（接口 `IErpFinArApItemBiz.java:51`） | `findList(query, null, context)` | 余额场景改 SUM 聚合接口；明细场景调用方治理。**接口契约面：新增方法属加法；不改既有方法语义** |
| G1 | `@BizMutation refreshRatesFromApi` 事务内同步外部汇率 HTTP + 循环内逐汇率 findExistingRate + 逐行 save/update | `module-master-data/.../ErpMdCurrencyBizModel.java:28-33` → `ErpMdCurrencyRefreshRatesFromApiProcessor.java:60,:77,:90-92` | HTTP 超时 5-30s 期间占用 DB 连接+事务 | **批次 3 部分处置（plan 2026-09-28-0835-1）**：findExistingRate 已改 in() 单次批载；事务外重排经实时基线（factory 仅 mock provider、config 默认关）改判 Deferred——触发=真实 provider 立项时按 §6.3/AP5 落「事务外取数+事务内落库」 |

### 2.2 P2/P3——批量治理项

| # | 发现 | 位置 | 修复方向 |
|---|------|------|---------|
| A9 | 8+ 个域 Report/Dashboard 查询无 limit 全量：inv report `:235,:250,:278`、mfg report `:377,:393,:407,:414`、qa `:331,:338,:350,:362`、cs `:253,:262,:295`、crm `:297,:345,:352,:366`、hr `:349,:365`、ast dashboard+report 全文 0 处 setLimit | 各域 `Erp*ReportBizModel.java` | 报表类 SQL 聚合；预警类设每域硬上限常量（inv 域 `ALERT_MAX_ROWS=5000` 先例，cap 值各自裁决留痕） |

> **A9 补遗回填（2026-09-28，plan `2026-09-28-1418-4`）**：收官全局重扫描发现 A9 枚举漏盘 4 站点并已收口——cs 质量看板 `ErpCsQualityDashboardBizModel.loadClosedTickets`（createTime desc+`CS_DASHBOARD_SCAN_CAP=5000`，三入口 SLA/CSAT 聚合 SQL 重写 Deferred）；prj `ErpPrjReportBizModel.loadTimesheets`（(projectId,userId) 分组投影+行序保持，旧 loadTimesheets/Aggregator 移除；`loadProjects` 主数据小表豁免登记）；mnt `ErpMntReportBizModel.loadVisits`（`REPORT_LIST_MAX_ROWS=5000` cap）/`loadDowntimeEntries`（(equipmentId,reason) 分组投影——原无 orderBy 禁无排序 cap）；qa `ErpQaDashboardBizModel.countOutOfControlCharts`+孪生 `countInadequateCapabilityCharts`（chartId 单列投影+内存去重——**平台对纯维度投影仍注入主键维度的坑二度变体**，ast 先例实为投影减列+内存去重）；`findCapaOverdueAlert` 日期下推+排序+`QA_ALERT_CAP=5000`。mnt `loadScheduleIdsWithVisit` 既有无排序截断挂 watch-only（截断方向为错误数据非保守下偏）。
| B2 | 超期预警逐行查 partner 名称（sal `:204` / pur `:236`） | 两个 dashboard | distinct partnerId 批量 `in("id", ids)` 建 Map |
| B3 | 销售退货 processor 行循环逐行取 delivery/order line | `ErpSalReturnProcessor.java:508,:519` | 循环前按 id 集合批量预载 |
| B4 | B2B ASN 生成收货单行循环逐行查物料 | `ErpB2bAsnCreateReceiveFromAsnProcessor.java:143` | 批量预载（`batchLoadProps` 先例 `ErpSalOrderBizModel.java:399`） |
| B5 | notify 派发逐接收人 findMergeable + 逐候选 isRead | `NotificationMergeCoordinator.java:46-77` | 候选一次查出按 user 分组；isRead 批量 in |
| E2 | AcctSchemaResolver 无缓存，fin 看板/报表每请求 2-3 次 + 各过账 Dispatcher 每次 1 次 | `erp-md-dao/.../AcctSchemaResolver.java:42-54` | orgId→schemaId 请求级 memo（不引入跨请求缓存，避免失效复杂度） |
| E3 | notify 循环内重复 parseChannelSet / AppConfig.var | `NotificationDispatcher.java:105,:144` | 循环外解析一次传参 |
| C1 | 全仓约 60 处循环内逐行 saveEntity/updateEntity，零批量 API 使用（notify 广播、HR payroll、DRP/MRP 行生成、fin 预测刷新等） | 见 agent 报告代表站点清单 | **Deferred**：Nop session flush 时可 JDBC batch，实际放大系数未证实；先 spike 平台批量 API，再逐文件接入 |
| B6 | fin-posting 热路径循环内 getEntityById（4 站点 + 4 同族） | `ErpFinPostingProcessor.java:508,:956,:976,:1019` 等 | **Deferred**：link 数≈账套数（1-2），放大有限；位于会计过账保护区，收益低风险高 |
| G2 | 物流网关重试退避 Thread.sleep 最长 60s×多次，位于调用方 @BizMutation 事务内（类注释自认） | `GatewayDispatcher.java:118-136,:46,:466-472` | **Deferred**：重试移出事务=行为契约变更（派发状态机语义），须 owner doc 对齐后专门 plan |
| G3 | B2B TransportManager 重试 sleep 上限 5s | `TransportManager.java:183` | 同 G2 随行 |
| I2 | notify 外发通道逐通知串行 sendEmail/sendSms（1000 人广播=2000 次串行网络调用在请求内） | `NotificationDispatcher.java:115-119,:209,:224` | **Deferred**：批量化 sender/异步队列（nop-message 总线）归 notify 深化 plan |
| J1-J3 | Pattern 实例字段、循环内 String.format、低频正则重编译 | 各散点 | 顺手项，随所在批次修复 |

### 2.3 索引（orm.xml 保护区）

> **状态回填（2026-09-28）**：F1/F2 已由 plan `2026-09-28-0852-2` 处置（保护区双独立子代理批准 GRANT 后实施：erp_fin_ar_ap_item +IDX_FIN_AR_AP_ITEM_DIRECTION_STATUS、erp_sys_notification TYPE_USER 替换为含 createTime 三列 + 三方言 deploy SQL 派生物同步）；F2 最初描述的「order desc」经平台源码实裁正确（QueryBean.addOrderField true=DESC）。

- **总体健康**：19/19 orm.xml 定义索引共 313 处，核心大表（stock_move/stock_ledger/ar_ap_item 等）高频列已覆盖。
- **F1 [P2]**：`erp_fin_ar_ap_item` 缺 `(direction, status)` 复合索引——`findOpenItems`/`findArApAging` 的 where `direction=? AND status IN(...)` 无法命中现有 `(orgId,status)` 索引（查询不带 orgId）→ 全表扫描+排序。修复 = **仅加索引，不改查询语义**（加 org scope 会改变多公司隔离下的结果语义，不做）。
- **F2 [P3]**：`erp_sys_notification` 缺 createTime 复合索引——`findMergeable` 的 `ge(createTime)` + `order by createTime desc` 无索引覆盖，append-only 表持续增长。
- 两项均触发 orm.xml 保护区：`auto + dual-agent-approval`（两个独立子 agent 分别批准）+ `mvn clean install -DskipTests` 重生成。

### 2.4 排除项（核查后不修）

- `StockMoveBookkeeper.java:266` while(true)——乐观锁有界重试（默认 5 次）+ 指标埋点，设计健康。
- notify 派发事件驱动非轮询；logistics scanForPolling 有 limit 100——架构健康。
- 各域看板已有 SQL 聚合正面范式（`findCustomerTopN`、`loadGlBalances`、mfg/qa/prj/mnt dashboard count+groupBy）——作为修复模板，不需要动。

---

## 3. UI/UX 发现

> **状态回填（2026-09-28）**：§3.1 P2 四行与 §3.2 全部 P2/P3 已由 plan `2026-09-28-0906-3` 处置（64 文件状态字段只读——泛型 status 56 文件 Deferred、编辑 drawer 尺寸 68 站点、dialog/drawer 混用与查看态容器 37 站点归一、batch 按钮命名 watch-only、35+13 文件状态筛选 cell、45 page.yaml 空态（chart/crud/list 44 处；html 组件 Deferred）、10 域看板联动 8 域、l10n-cn 死链整段移除；§4-E flux 门禁重跑 errors 325=325 零新增）；列宽配置维持 §6 Deferred。

### 3.1 2026-09-14 基线核验（HEAD 实核；修复状态于 2026-09-27 由 plan `2026-09-27-0318-1` 回填）

| 09-14 发现 | 当前状态 | 证据 |
|-----------|---------|------|
| P1-1 `assign?ticketId=$id&assignedToId=0`（真 bug：绕过后端回落，分派给不存在用户 0） | **✅ 已修复**（dialog + 用户 picker） | `erp-cs-web/.../ErpCsTicket.view.xml:382`；后端 `ErpCsTicketBizModel.java:240` 回落逻辑未变 |
| P1-2 `resolve?resolution=` 空串提交（cs + qa 同构） | **✅ 已修复**（resolution 必填 dialog ×2） | `ErpCsTicket.view.xml:396`；`erp-qa-web/.../ErpQaNonConformance.view.xml:150` |
| P1-3 `reportCompletion?workOrderId=$id&completedQty=0`（无法报实际数量） | **✅ 已修复**（completedQty 必填 dialog） | `erp-mfg-web/.../ErpMfgWorkOrder.view.xml:216` |
| P1-4 `scheduleForward?scheduleId=0` 死按钮（confirmText 与 mutation 并存） | **✅ 已修复**（改 link 跳排程方案列表） | `erp-aps-web/.../ErpApsOperationOrder.view.xml:144-145` |
| P1-5 escalateQuality materialId 手输数字 ID | **✅ 已修复**（gen-control v3.2 物料 picker） | `ErpCsTicket.view.xml:325-330` |
| P2 编辑表单暴露状态机字段 | **仍开放**（pur/cs/fin 三文件 grep readOnly 零命中） | `ErpPurOrder.view.xml:89-91,:123-124`；`ErpCsTicket.view.xml:114-115,:207-208`；`ErpFinVoucher.view.xml:75` |
| P2 编辑 drawer 无显式尺寸 | 基本仍开放（3/68 已修，余 65） | 3 处已修：`ErpAstInventory:71-74`/`ErpHrSurvey:137-140`/`ErpLogCarrier:127-130` |
| P2 dialog/drawer 混用 | 仍开放 | `ErpHrEmployee.view.xml:312,:320`；`ErpFinBudgetScenario.view.xml:196,:201` |
| P2 凭证头合计非实时 | 仍开放（已有降级提示+过账闸门兜底） | `ErpFinVoucher.view.xml:126-135`（DEFERRED 注释）、`:97`、`:241` |
| P3 列数>13 | 边际改善（67→65 页） | Top：ErpLogShipment 40 列未动 |
| P3 行按钮过载 | 边际改善（15→14/16 页） | 峰值 ErpCsTicket/ErpMfgWorkOrder 各 16 |
| P3 币种→汇率/物料→单位不联动、批量驳回缺失、双查询入口 43 页 | 仍开放 | 见 agent 报告 |

### 3.2 新发现（09-14 未覆盖）

| 级别 | 发现 | 位置 | 修复方向 |
|------|------|------|---------|
| **P1** | ErpFinBudgetScenario 驳回/作废（danger）无 confirmText 无 dialog 直接 @mutation——全库 62 处危险动作中唯一裸奔对 | `erp-fin-web/.../ErpFinBudgetScenario.view.xml:187-192` | 补 confirmText（同库 ErpApsOperationOrder:146 / ErpMdMaterial 停用确认范式） |
| P2 | 聚合菜单 3 个死链接（l10n-cn 中国本地化菜单指向不存在的 module，admin 可见点击即 404；注释自述「设计阶段」） | `app-erp-all/src/main/resources/_vfs/nop/main/auth/app.action-auth.xml:31-56` | 设计阶段菜单不应注册进聚合 app 菜单（移除或注释保留） |
| P2 | 查看态容器错配：15 个 tabs 页的只读 view 表单（多数 `size="lg"`）被未标尺寸的默认 dialog 承载 | `_gen/_ErpPurOrder.view.xml:194-196` 默认 + 各 tabs 页 | 复杂单据 view 按钮显式 drawer + size |
| P2 | batch 按钮命名/语义漂移（qa `batch-pass` vs pur/sal `batch-approve`；fin 裸 `batch-delete` 无文案） | `ErpQaInspection`、pur/sal 列表、fin 列表 | 统一命名+色表 |
| P3 | 36 页有状态列无状态筛选 cell（仅 92/368 页可按状态筛） | 全库 grep 可复现 | 补 `filterOp` cell |
| P3 | 45 个看板/报表 page.yaml 空数据态零覆盖（0 个 `暂无`/emptyText） | 20 dashboard + 25 report | 补空态文案（正例 `ErpCsTicket` kbSuggestion） |
| P3 | 采购看板刷新 5 连击（then 链串 5 个 refreshSource，filter 变更不联动） | `pur dashboard/main.page.yaml:33-52` | filter onEvent 联动刷新 |
| P3 | 列宽配置全库 0 处（65 页超限的伴生症状） | — | 随列裁剪批次处理 |

### 3.3 正面结论（无需修）

菜单分层（18 域 + notify + sys）清晰、双语 `i18n-en` 菜单覆盖 100%、url 零重复（除 l10n-cn 死链）；368 页全部具备只读 view 入口；`mandatory` 标识 1536 处统一；手写层 i18n 英文裸串 0 处；危险操作确认覆盖 93.5%（58/62）且色表一致；看板无轮询/无重资源内联。

---

## 4. 前端性能与 flux 合规发现

| # | 发现 | 证据 | 处置 |
|---|------|------|------|
| A | 65 页 list grid 超 13 列阈值（Top：ErpLogShipment 40、ErpB2bMftConfig 29、ErpApsOperationOrder/ErpB2bMftLog/ErpMdMaterial 28、ErpDrpLine 25…）；域分布 ct 11 / b2b 10 / md 9 / drp 7 / log 6 / aps 5 / fin 5 … | 量化脚本口径见 agent 报告 | **列裁剪属逐页业务决策**——本轮处置见 §5 批次 5（P3，与 09-14 §7.9 同一债务；优先 Top 最宽页做「关键列前置」示范，全面收敛 Deferred） |
| B | 行动作≥10 页 16 个（ErpCsTicket/ErpMfgWorkOrder 各 16） | 各域 view.xml | 「更多」下拉归并属交互设计决策，与 P1 参数 dialog 修复不同面；随 UX 批次对峰值 2 页示范 |
| C | 无自动刷新（全 855 page.yaml interval 类零命中）；最重看板 qa（3 chart/6 ds/6 api）与 ErpCsTicket kanban（6 ds/11 api） | — | 取数合并属后端接口决策，Deferred（触发：看板首屏耗时实测超标） |
| D | 子表 >10 列 6 实体：ErpFinVoucherLine 17 列 list-edit 最重 | `ErpFinVoucherLine.view.xml:39,:240` | 子表列裁剪与 A 同面，随批次 5 处理代表页 |
| E | flux 门禁报告过期：报告 09-11 vs 导出 09-21（pageCount=999）；325 error 全部为已裁决 `variant="primary"` 基线族（`flux-page-export-and-validation.md:244-246`）；warning 18494（conflicting-field-definition 11044 + unknown-property 6349 两族） | `_tmp/flux-page-validation-report.json`、`manifest.json` | **本轮重跑 `npm run validate:flux` 对账（零新增 error 即 pass）**；warning 模板级收敛属生成层（nop-entropy/_gen 链），app 仓不可达，Deferred 登记 |
| F | 无 base64/图片内联；手写 HTML badge 仅 1 处（`ErpFinVoucher.view.xml:97` gen-control 拼 HTML） | — | badge 换组件随批次 1（fin 文件）顺带评估；若 flux 组件能力不足则维持（F5.5 已知降级先例） |
| G | duplicate-schema-id warning 7 条 | 同报告 | flux 层 id 生成规则问题，Deferred |

---

## 5. 修复批次规划（本轮执行）

> 每批 = 一个独立 plan（独立草案审查 → 执行 → 验证 → 独立结束审计 → 提交）。命名遵循 `00-plan-authoring-and-execution-guide.md`。

| 批次 | Plan 主题 | 覆盖发现 | 改动面 | 保护区/审批 |
|------|----------|---------|--------|------------|
| 1 | UX 行级业务动作缺陷修复（P1×6） | 09-14 P1-1~P1-5 + 新 P1（FinBudgetScenario 确认） | cs/mfg/qa/aps/fin 5 个 view.xml | 无（view.xml 非保护区）——✅ done（2026-09-27，plan `2026-09-27-0318-1`） |
| 2 | 看板/报表读路径性能优化（sal/pur/inv/fin 看板 + 三单匹配 N+1 + 各域 Report 无界治理） | A1,A2,A3,A6,A7,A8,A9,B1,B2,E4,J2 | ~14 个 Java 文件（dashboard/report BizModel） | 读路径重写，数字口径不变；fin 看板仅读路径（非过账）；有既有 value E2E/JUnit 数字断言护栏 |
| 3 | 事务内外发重排 + 服务层 N+1/热点治理 | G1,E2,E3,B3,B4,B5,J1,J3 | md/logistics?/b2b/sal/notify/md-dao ~8 文件 | G2/G3（网关重试）Deferred 不在本批；notify/sal/b2b 非保护区；行为需测试护栏 |
| 4 | orm.xml 复合索引补齐（F1+F2） | 2.3 节 | 2 个 orm.xml 各 +1 索引 + 重生成 | **orm.xml 保护区：双独立子 agent 批准** |
| 5 | UX 一致性批量治理 | 3.1 P2 项 + 3.2 P2/P3 + §4-E 门禁重跑 | ~60-80 个 view.yaml/page.yaml + 1 菜单文件 | 无；`validate:flux` 门禁 + 构建 + 抽样 E2E |

**执行顺序**：1 → 2 → 3 → 4 → 5（1 最小先行验证流程；2/3 后端性能主线；4 需双批准独立走；5 面最大放最后）。

## 6. Deferred But Adjudicated（本轮明确不做 + 重开触发条件）

| 项 | 分类 | 不做原因 | 重开触发条件 |
|----|------|---------|-------------|
| A4/A5 fin 现金流量表/账龄报表 SQL 聚合重写 | optimization candidate | 报表数字须与 owner doc 逐分对账，重写风险>收益；最小化「直接/间接共享加载」随批次 2 评估，完整重写独立立项 | fin 报表月度凭证量实测>1 万张或报表 RT 实测超标 |
| G2/G3 物流/B2B 网关重试移出事务 | out-of-scope improvement | 派发状态机行为契约变更，须 owner doc（logistics state-machine）先行修订 | 承运商 5xx 实际阻塞事务/连接池耗尽事件暴露；或 owner doc 增补异步重试语义 |
| B6 fin-posting 热路径 getEntityById | watch-only residual | link 数≈账套数放大有限；会计过账保护区收益/风险比不划算 | 过账 RT 实测成为瓶颈（profiling 证据） |
| C1 循环 save 批量化（60 处） | optimization candidate | Nop session flush 可 JDBC batch，放大系数未证实；先 spike 平台批量 API | 平台批量 API spike 结论落地，或 HR payroll/notify 广播实测超标 |
| I2 notify 外发通道批量化 | optimization candidate | 涉及 sender SPI 扩展/异步队列选型 | 广播量级实测（>500 人）或 nop-message 总线接入时 |
| flux warning 18k 模板级收敛 + duplicate-schema-id | out-of-scope improvement | 根因在 codegen 模板/flux 导出层，app 仓不可达（外部仓库保护区） | nop-entropy/nop-chaos-flux 模板迭代时 |
| 看板/报表取数合并（qa/mfg/kanban 重页） | optimization candidate | 需新增聚合接口（api 契约面） | 看板首屏 RT 实测超标 |
| 65 页列裁剪全面收敛 / 行按钮「更多」归并全面推广 | optimization candidate | 逐页业务决策量大；本轮做 Top 代表页示范 + 基线登记 | 逐域 UX 迭代排期；或 flux 列虚拟化行为实证后统一策略 |
| 币种→汇率/物料→单位联动、批量驳回/撤回、43 页双查询入口收敛 | optimization candidate（09-14 P3 遗留） | 交互设计决策，需 owner doc（ui-patterns）先行定范式 | `docs/design/<domain>/ui-patterns.md` 增补对应范式后随域迭代 |

## 7. 验证与基线义务（适用全部批次）

- 全项目构建：`mvn clean install -DskipTests`（orm 变更批须重生成验证）
- 单测：`mvn test -pl <module> -am`（触域模块）
- 合规检查器：每批代码变更后 `bash docs/audits/nop-compliance-checker.sh`，若 actual > baseline 逐站点 git diff 分类（合法 baseline-raise 带证据 / 真违规 Fix）——lesson 07 义务
- 前端批次：`npm run validate:flux` exit 0（零新增 error 对照 325 基线）
- 看板/报表数字口径：既有 `*.value.spec.ts` 浏览器断言 + JUnit 为护栏；环境允许时跑受影响域抽样 E2E
- CJK/i18n checker（如触运行时字符串）
- 每批完成更新 `docs/logs/2026/09-27.md`；每批独立 git 提交（不混入工作树中其他在途未提交文件）
