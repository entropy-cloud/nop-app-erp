# RC MA4 A4.2 — 扩展域 MA1 存疑点运行时确认展开器（展开映射记录）

> Audit Status: closed
> 里程碑：MA4（运行时行为验证 / 展开器工作项）
> 工作项：A4.2（扩展域 MA1 存疑点运行时确认**展开器**）
> 展开器 plan：`docs/plans/2026-08-07-0400-3-rc-ma4-a4-2-ext-domain-runtime-expander.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§MA4 展开器范式 + §10 R1.0 同构 + §去重协议 + §5 保护区域暂停协议 + §8 过程纪律自检）
> 输入：扩展域 MA1 切片报告 A1.8-A1.51 §7 静态存疑点清单（43 份报告覆盖 44 切片，A1.45-46 合并；扩展域范围）
> 结果表面：`docs/backlog/requirement-compliance-roadmap.md` MA4 表内追加的 A4.2.1-A4.2.185 实体行（本记录的映射对象）
> 审计性质：**只读展开器**（读 MA1 报告 §7 + 追加 roadmap 表；不改代码/ORM/api.xml/真相源；§9 冻结条款）
> 展开完成判据：全部 §7 存疑点已展开为 A4.2.n 行（done = 展开完成，非验证完成；§MA4 + §10 R1.0 同构，与 A4.1 范式一致）
> 审计日期：2026-08-07
> 审计者：主代理（独立结束审计由独立子代理执行，见 plan §Closure Gates）

---

## 0. 展开结论（TL;DR）

| 项 | 数量 |
|---|---|
| 输入扩展域 MA1 切片报告（§7 存疑点清单） | 43 份文件（覆盖 44 切片，A1.45-46 合并为 1 文件；范围 A1.8-A1.51） |
| §7 存疑点条目（逐条提取，条目制非词频制） | **200**（原始条目） |
| 跨报告同根因同控制点去重合并 | **8 组合并**（合并减少 15 行：dashboard orgId 8→1 减 7 + 7 组 2-3 入合并各减 1-2） |
| 展开为 A4.2.n 实体行 | **A4.2.1 - A4.2.185**（185 行，已追加到 roadmap MA4 表） |
| 触及保护区域（验证探针需 ask-first） | **0**（本展开器所有 185 行均为 read-only 普查 / 既有 E2E 复用 / JUnit 断言补强 / 报表·凭证·审计列观测 / 部署 config·doc 普查 / 跨域行为观测；无新探针 exercise 会计过账核心/数据删除/ORM 结构变更）；注：个别行（如 A4.2.59 换货功能确认真相源、A4.2.146 被引用 SKU 删除数据完整性事件）触及**需求契约确认 / 数据完整性行为**，但其验证探针仍为 read-only 观测，不修改 ORM/过账/删除逻辑 |
| A4.2 状态 | **done**（展开完成） |
| A4.2.n 状态 | **todo**（各自待独立 plan + 独立验证 done；mission driver 逐项 DRAFT_PLANS → 验证） |

**整体裁决**：扩展域 MA1 切片报告 A1.8-A1.51 的 §7 静态存疑点清单**全集**（43 份报告，200 条原始条目）经逐条提取（条目制）+ 跨报告去重（8 组同根因同控制点合并）展开为 185 条 A4.2.n 实体行，已追加到 `requirement-compliance-roadmap.md` MA4 表。展开完成即 A4.2 done（对齐 §MA4 + §10 R1.0 done 判据 = 展开完成而非验证完成，与 A4.1 范式一致）。本展开器**不执行运行时验证本身**——验证属后续各 A4.2.n 实体行（各自独立 plan + 独立 done）。零代码/ORM/api.xml/真相源变更（roadmap MA4 表追加是工作项追踪更新，非冻结真相源）。

---

## 1. 输入报告 §7 存在性核对（逐报告）

> 43 份报告覆盖 44 切片（A1.45-46 合并）。逐报告核对 §7 段落存在性 + §7 存疑点条目数（条目制）。无「无存疑点」切片（43 份报告均有 ≥1 §7 存疑点条目）。

| 切片 | 报告文件 | §7 段落 | §7 条目数 |
|------|---------|---------|----------|
| A1.8 mfg-F1 mrp-drp-engine | `2026-08-02-2042-2-...-a1-8-...md` | ✅ | 3 |
| A1.9 mfg-F2 work-order-reporting | `2026-08-02-2042-3-...-a1-9-...md` | ✅ | 3 |
| A1.10 mfg-F3 bom-routing | `2026-08-02-2231-1-...-a1-10-...md` | ✅ | 3 |
| A1.11 mfg-F4 variance-batch-kanban | `2026-08-02-2245-...-a1-11-...md` | ✅ | 4 |
| A1.12 hr-F1 employee-organization | `2026-08-02-2328-...-a1-12-...md` | ✅ | 5 |
| A1.13 hr-F2 shift-attendance | `2026-08-02-2344-...-a1-13-...md` | ✅ | 5 |
| A1.14 hr-F3 payroll-survey | `2026-08-03-0000-...-a1-14-...md` | ✅ | 5 |
| A1.15 purchase-F1 mainflow-requisition | `2026-08-03-0145-...-a1-15-...md` | ✅ | 6 |
| A1.16 purchase-F2 three-way-match-variance | `2026-08-03-0200-...-a1-16-...md` | ✅ | 7 |
| A1.17 purchase-F3 returns-business-finance | `2026-08-03-0300-...-a1-17-...md` | ✅ | 7 |
| A1.18 sales-F1 mainflow-pricing | `2026-08-03-0430-...-a1-18-...md` | ✅ | 5 |
| A1.19 sales-F2 outbound-concurrency | `2026-08-03-0530-...-a1-19-...md` | ✅ | 5 |
| A1.20 sales-F3 returns-family | `2026-08-03-0630-...-a1-20-...md` | ✅ | 5 |
| A1.21 sales-F4 gift-dashboards | `2026-08-03-0900-...-a1-21-...md` | ✅ | 5 |
| A1.22 assets-F1 depreciation-engine | `2026-08-03-0900-2-...-a1-22-...md` | ✅ | 4 |
| A1.23 assets-F2 disposal | `2026-08-03-0900-3-...-a1-23-...md` | ✅ | 3 |
| A1.24 assets-F3 capitalization-idle-cip-inventory-maintenance-splitmerge-dashboard | `2026-08-03-1200-1-...-a1-24-...md` | ✅ | 5 |
| A1.25 inventory-F1 stockmove-reversal-traceability | `2026-08-03-0953-...-a1-25-...md` | ✅ | 2 |
| A1.26 inventory-F2 batch-traceability-expiry-negative-stock | `2026-08-03-1200-3-...-a1-26-...md` | ✅ | 4 |
| A1.27 inventory-F3 stocktake-valuation-concurrency-dashboard | `2026-08-05-0900-...-a1-27-...md` | ✅ | 4 |
| A1.28 crm-F1 lead-lifecycle | `2026-08-05-1030-...-a1-28-...md` | ✅ | 6 |
| A1.29 crm-F2 marketing-forecast-quota-sequence-funnel | `2026-08-05-1100-...-a1-29-...md` | ✅ | 8 |
| A1.30 crm-F3 cpq-funnel-advancement | `2026-08-05-1830-...-a1-30-...md` | ✅ | 4 |
| A1.31 quality-F1 inspection-gating | `2026-08-05-1830-2-...-a1-31-...md` | ✅ | 5 |
| A1.32 quality-F2 ncr-capa-closure | `2026-08-05-1830-3-...-a1-32-...md` | ✅ | 4 |
| A1.33 quality-F3 spc-dashboard | `2026-08-05-2200-1-...-a1-33-...md` | ✅ | 5 |
| A1.34 projects-F1 startup-cost-collection | `2026-08-05-2200-2-...-a1-34-...md` | ✅ | 5 |
| A1.35 projects-F2 budget-dag | `2026-08-05-2200-3-...-a1-35-...md` | ✅ | 4 |
| A1.36 projects-F3 settlement-dashboard | `2026-08-05-2330-1-...-a1-36-...md` | ✅ | 5 |
| A1.37 cs-F1 ticket-lifecycle | `2026-08-05-2330-2-...-a1-37-...md` | ✅ | 5 |
| A1.38 cs-F2 sla-escalation | `2026-08-05-2330-3-...-a1-38-...md` | ✅ | 4 |
| A1.39 cs-F3 knowledge-quality-canned | `2026-08-06-0100-1-...-a1-39-...md` | ✅ | 5 |
| A1.40 cs-F4 survey-entitlement-catalog-fulfillment | `2026-08-06-0100-2-...-a1-40-...md` | ✅ | 5 |
| A1.41 master-data-full | `2026-08-06-0100-3-...-a1-41-...md` | ✅ | 5 |
| A1.42 maintenance-F1 scheduling-conflict | `2026-08-06-0245-1-...-a1-42-...md` | ✅ | 1（SP-2/SP-3 静态确认无需 MA4，仅 SP-1 留运行时确认） |
| A1.43 maintenance-F2 visit-sparepart | `2026-08-06-0245-2-...-a1-43-...md` | ✅ | 3 |
| A1.44 maintenance-F3 response-linkage-oee-dashboard | `2026-08-06-0245-3-...-a1-44-...md` | ✅ | 5 |
| A1.45-46 contract-lifecycle-billing-rebate | `2026-08-05-1400-1-...-a1-45-46-...md` | ✅ | 8（2 切片合并 1 报告） |
| A1.47 b2b-edi-full | `2026-08-05-1400-2-...-a1-47-...md` | ✅ | 7 |
| A1.48 drp-full | `2026-08-05-1400-3-...-a1-48-...md` | ✅ | 4 |
| A1.49 logistics-full | `2026-08-06-2243-1-...-a1-49-...md` | ✅ | 4 |
| A1.50 aps-full | `2026-08-06-2243-2-...-a1-50-...md` | ✅ | 4 |
| A1.51 notify-full | `2026-08-06-2243-3-...-a1-51-...md` | ✅ | 4 |
| **合计** | 43 份报告（44 切片） | 43/43 §7 段落存在 | **200**（原始条目） |

> 注：A1.42 报告 §7 含 SP-1/SP-2/SP-3 三条，但 SP-2/SP-3 自述「静态已确认无需 MA4」（SP-2 = 设备累计运行时长无采集入口归 P1-RC-064；SP-3 = recurrenceType dict 无 RUNTIME/USAGE 值），仅 SP-1 留运行时确认（低优先级端到端路径确认）。故 A1.42 计 1 条入 MA4 展开。

---

## 2. 跨报告去重依据（§去重协议）

> 方法论 §去重协议 + plan Phase 1 item 2：同根因同控制点合并为一行（显式标注合并依据 + 涉及切片）；不同根因/不同控制点各一行。

**去重结论：8 组合并**（200 条原始 → 185 条 A4.2.n 行）。合并组如下：

| 合并组 | A4.2.n | 涉及切片 §7 锚点 | 同根因 | 同控制点 | 合并依据 |
|--------|--------|-----------------|--------|---------|---------|
| **G1 dashboard orgId 行级权限** | A4.2.10 | A1.11 SP-3 + A1.21 SP-3 + A1.24 SP-4 + A1.27 SP-4 + A1.33 SP-1 + A1.36 SP-5 + A1.41 SP-1 + A1.44 SP-5（8 切片） | R1.29 `ErpOrgIsolationQueryTransformer` 全局 IQueryTransformer 注入有效性（reuse P1-MA2-093 resolved） | dashboard BizModel 经 `daoProvider.daoFor(...).findAllByQuery(q)` / `ormTemplate.findListByQuery(q)` 直访路径绕过 CrudBizModel 标准管道，全局 transformer 是否实际注入 orgId 过滤 | 8 域 dashboard 同一架构模式（IDaoProvider/IOrmTemplate 直访），同一根因（R1.29），同一控制点（dashboard 直访路径注入）。一次性运行时确认（多组织部署 + 跨组织查询断言）覆盖全部 8 域。与 A4.1.25（finance dashboard，A1.7 SP-4）同根因但不同范围（A4.1 = finance / A4.2 = 扩展域 8 域），各自独立行 |
| **G2 完工触发差异过账失败告警** | A4.2.4 | A1.9 SP-1 + A1.11 SP-1（2 切片） | P1-MA4-007 resolved R1.16 G3 错误分级 + 告警派发，`IErpSysNotificationBiz.notify` best-effort 降级 | 完工触发差异过账失败时告警通道投递成功率 + 运营响应闭环（手动重算入口 `calculateVariances` 是否被实际使用） | A1.11 SP-1 自述「与 A1.9 SP-1 同根因」。两切片同一 finding（P1-MA4-007）、同一 notify 通道、同一控制点（best-effort 投递 + 运营闭环） |
| **G3 UC-MFG-09 返工工单工作流** | A4.2.5 | A1.9 SP-2 + A1.31 SP-2（2 切片） | 完工质检 REJECTED + 工单保持 IN_PROCESS（config-gated 门控），返工工单工作流 | 操作员面对 REJECTED 工单的实际工作流（手动关闭→新建返工 / 重置质检 / useLogicalDelete）+ 「关联原工单」可追溯性 | A1.31 SP-2 自述「与 A1.9 SP-2 合并」（quality 侧投影）。同一控制点（REJECTED 工单操作员工作流），跨域投影（mfg 侧 + quality 侧） |
| **G4 MR1 预留写路径 successor** | A4.2.3 | A1.8 SP-3 + A1.9 SP-3（2 切片） | MR1 P1-RC-008 修复落地后预留写路径实现 | reservedQty / availableQuantity 实时一致性 + 跨工单并发预留 lost-update 防护（versionProp 覆盖预留写） | A1.9 SP-3 自述「与 A1.8 SP-3 协同」。同一 successor 触发条件（MR1 修复落地），同一控制点（预留写路径一致性） |
| **G5 订单级库存校验缺失** | A4.2.47 | A1.18 §7-1 + A1.19 §7-2（2 切片） | sales 订单审核不调库存 Facade（无订单级可用量校验） | 「销售员实际接单后到出库环节才发现库存不足」的运行时频度 / 业务影响 | A1.19 §7-2 自述同一存疑点。同一根因（订单层无库存校验），同一控制点（出库环节发现库存不足的运营频度） |
| **G6 跨域期间 CLOSED guard** | A4.2.43 | A1.17 §7-4 + A1.20 SP-3（2 切片） | finance 引擎 `ErpFinPostingProcessor.resolveOpenPeriod:524-527` 全局期间控制间接拦截跨域过账 | purchase（receive/invoice/return）+ sales（return）过账路径在期间 CLOSED 时是否被间接拦截 | 两切片同一守卫机制（finance resolveOpenPeriod），同一控制点（跨域过账路径期间控制），A1.17 §7-4 自述「采购侧未独立测试，MA4 可补采购侧 E2E」，A1.20 SP-3 自述「需运行时确认 SALES_RETURN 过账路径是否经此守卫」 |
| **G7 价税分离 GL 偏差** | A4.2.56 | A1.20 SP-1 + A1.21 SP-1（2 切片） | `recomputeLineAmount:172-179` 不重算 taxAmount + `recomputeOrderTotals:181-197` 复用陈旧 taxAmount 求和（P1-RC-022） | 多档税率混合单据 + 多档促销叠加场景下税额偏差范围量化（影响 AR 销项税 + 应收金额准确性） | A1.21 SP-1 自述「复用 P1-RC-022」（A1.20 finding）。同一根因（recomputeLineAmount/recomputeOrderTotals），同一控制点（多档税率混合 + 促销叠加税额偏差数值量化） |
| **G8 ExpenseCostAggregator 状态/超预算归集** | A4.2.113 | A1.34 SP-1 + A1.35 SP-1 + A1.35 SP-3（2 切片 3 锚点） | `ExpenseCostAggregator:60-64` 不校验项目状态（ON_HOLD/COMPLETED/CANCELLED）+ 不调 budgetChecker（P1-RC-050 + P1-RC-051 双缺口同站点） | 违规归集行经 closeProject 触发链路反向清理或仅累积（ON_HOLD/超预算/双条件叠加三路径在同一 `:60-64` 站点） | A1.35 SP-1 自述「与 P1-RC-050 SP-1 同根因——ON_HOLD/超预算两路径在 ExpenseCostAggregator 同站点」，A1.35 SP-3 自述「两 finding 修复须协同」。同一根因（ExpenseCostAggregator 缺口），同一控制点（同一代码站点 `:60-64`） |

> **未合并的跨报告主题相关对**（保留独立行 + 交叉引用）：
> - A1.15 §7-5 / A1.16 §7-6 / A1.17 §7-6（承付恢复不对称，reuse P1-MA2-083）：同根因但不同控制点（invoice reverseApprove vs order cancel vs return reverseApprove，三触发路径各自独立验证）→ 各自独立行 A4.2.31 / A4.2.38 / A4.2.45 + 交叉引用。
> - A1.22 SP-2 / A1.23 SP-1（折旧补提缺失，reuse P1-RC-029）：同根因但不同控制点（asset 折旧多漏提期偏差 vs disposal 月中出售累计折旧低估）→ 各自独立行 A4.2.64 / A4.2.67 + 交叉引用。
> - A1.34 SP-4 / A1.36 SP-1（多币种 exchangeRate=ONE，reuse P1-MA1-010）：同根因但不同控制点（工时过账 buildEvent vs ProjectPnl amountFunctional 聚合）→ 各自独立行 A4.2.116 / A4.2.120 + 交叉引用。
> - A1.10 SP-1 / SP-2（BOM 快照缺失，reuse P1-RC-009）：同根因但不同控制点（BOM 重展开读标准用量 vs GL 凭证金额偏差）→ 各自独立行 A4.2.6 / A4.2.7 + 协同标注。
>
> **既有 arm MA2 证据去重**（§去重协议）：200 条均经各 MA1 报告 §7 显式标注为「静态存疑、需运行时确认」（未被既有 MA2 报告证实），符合展开条件。无存疑点的切片不产生行（扩展域 43 份报告均有存疑点，故均产生行）。

---

## 3. 存疑点 → A4.2.n 行映射表（全 185 行，按来源切片分组）

> 每行字段：编号 / 来源切片 + §7 锚点 / §7 存疑点原文摘要 / 运行时验证方法（复用 E2E + `_helper.ts` 原语 / 既有 JUnit / 临时探针）/ 预期证实·证伪 / 触及保护区域（是/否 + 类别）。验证方法标注引用的原语参考 `tests/e2e/orchestration/_helper.ts`（`runP2pChain` / `runO2cChain` / `runP2pReverse` / `runO2cReverse` / `assertVoucherLines` 等）+ 既有 JUnit + grep 普查 + 部署 config/doc 普查。

### 3.1 mfg 域（A1.8-A1.11，11 行 A4.2.1-A4.2.11）

| A4.2.n | 来源 + §7 锚点 | §7 存疑点摘要 | 运行时验证方法 | 预期证实/证伪 | 保护区域 |
|--------|---------------|--------------|--------------|--------------|---------|
| A4.2.1 | A1.8 SP-1 | 预留量并发扣减运行时行为（reserved 恒为 0，多工单并发领料 stock move bookkeeper negative-stock 防护兜底） | 多工单并发领料同一物料同一仓库 E2E + 断言无 silent split-quantity corruption | 证实：P0-MA2-020 UK + 余额守恒在所有并发场景兜底；证伪：某并发场景致 split-quantity corruption | 否（并发行为观测） |
| A4.2.2 | A1.8 SP-2 | STOCK_PARTIAL 强制开工后领料 KitAvailabilityChecker 只读路径补料后可用量 | STOCK_PARTIAL 强制开工后补料 E2E，断言 KitAvailabilityChecker 正确反映补料后可用量 | 证实：只读路径无缓存/陈旧读；证伪：补料后可用量陈旧 | 否（read-only 路径观测） |
| A4.2.3 | A1.8 SP-3 + A1.9 SP-3（合并 G4） | MR1 P1-RC-008 修复落地后 reservedQty/availableQuantity 实时一致性 + 跨工单并发预留 lost-update 防护 | MR1 修复落地后构造并发预留场景断言 versionProp 覆盖预留写 | successor（MR1 修复落地后展开） | 否（successor 触发，非本展开器范围） |
| A4.2.4 | A1.9 SP-1 + A1.11 SP-1（合并 G2） | IErpSysNotificationBiz.notify 投递成功率 + 运营响应闭环 | 部署观测 notify best-effort 降级 + 手动重算入口 calculateVariances 使用频度 | 证实：notify 投递成功率 + 运营有响应闭环；证伪：notify 静默失败无人响应 | 否（部署可观测性普查） |
| A4.2.5 | A1.9 SP-2 + A1.31 SP-2（合并 G3） | UC-MFG-09 返工工单运行时操作流程（REJECTED 工单 IN_PROCESS 操作员工作流） | 运营流程调研 + 「关联原工单」可追溯性经工单备注/手工关联可达性确认 | 证实：操作员手动关闭→新建返工工单工作流可达；证伪：可追溯性完全不可达 | 否（运营流程调研） |
| A4.2.6 | A1.10 SP-1 | BOM 内容编辑后已开工工单是否按新 BOM 重算物料需求/成本（P1-RC-009） | BOM 子件编辑 + 已审核工单触发差异计算/重算/二次齐套 E2E，断言是否读新 BOM | 证实：差异计算/重算读新 BOM 致错误；证伪：快照机制隔离不受影响 | 否（read-only 行为观测） |
| A4.2.7 | A1.10 SP-2 | BOM 快照缺失运行时是否致成本结转凭证错误（P1-RC-009 GL 凭证） | config variance-auto-calc-enabled=true + BOM 编辑后断言 PRODUCTION_VARIANCE 凭证行级金额偏差 | 证实：凭证金额偏离审核时 BOM 内容；证伪：完工过账不受影响 | 否（read-only 凭证观测） |
| A4.2.8 | A1.10 SP-3 | bomId 弱隔离运行时边界（运营 BOM 变更实践：编辑 vs 新建） | 运营 BOM 变更操作调研 + 是否存在 BOM 版本化实践 | 证实：运营编辑同一 bomId 致隔离失效；证伪：运营新建 bomId 实践普遍 | 否（运营实践调研） |
| A4.2.9 | A1.11 SP-2 | best-effort 基因链写失败运行时缺口可观测性（BatchGenealogyWriter catch） | 部署观测 LOG.error 采集频率 + 基因链缺口业务影响（部分完工无追溯行） | 证实：catch 分支被触发 + 监控采集；证伪：写失败极罕见 | 否（部署可观测性普查） |
| A4.2.10 | 8 切片合并（G1） | dashboard orgId 行级权限 R1.29 transformer 对 IDaoProvider/IOrmTemplate 直访路径注入 | 多组织部署 + 用户归属 orgA 查 orgB dashboard 数据断言泄漏（复用 P1-MA2-093 运行时确认） | 证实：全局 transformer 注入 orgId 过滤；证伪：dashboard 直访路径泄漏 | 否（read-only 权限观测） |
| A4.2.11 | A1.11 SP-4 | 召回报告 degraded 模式运行时业务覆盖（受影响成品批次集合是否满足召回需求） | 实际召回事件触发 + inventory 域暴露按批次位置/去向查询方法集时观测（successor 触发条件） | 证实：degraded 模式不足需补位置/去向查询；证伪：受影响成品批次集合满足召回需求 | 否（业务覆盖调研） |

### 3.2 hr 域（A1.12-A1.14，15 行 A4.2.12-A4.2.26）

| A4.2.n | 来源 + §7 锚点 | §7 存疑点摘要 | 运行时验证方法 | 预期证实/证伪 | 保护区域 |
|--------|---------------|--------------|--------------|--------------|---------|
| A4.2.12 | A1.12 §7-1 | UC-HR-07 cron 运行时调度接线（scheduler.yaml + contract-expiry-cron config 非空） | 核查 scheduler.yaml 接线 + cron config 取值 | 证实：scheduler.yaml 接线 + config 非空；证伪：未接线 | 否（config/scheduler 普查） |
| A4.2.13 | A1.12 §7-2 | UC-HR-07 30/60/90 多档预警运行时配置（单一阈值多档调度） | 核查部署是否有多档调度配置（三 Job 实例 warningDays=30/60/90） | 证实：单一阈值 config + 多 Job 实例；证伪：无多档配置 | 否（config 普查） |
| A4.2.14 | A1.12 §7-3 | UC-HR-12 评估聚合权重运行时配置覆盖（assessment*Weight 默认 + AppConfig.var） | 核查部署是否有非默认配置覆盖 | 证实：config 驱动非硬编码 + 可能有非默认覆盖；证伪：硬编码 | 否（config 普查） |
| A4.2.15 | A1.12 §7-4 | UC-HR-08 handleContract 三态运行时行为（transferAutoHandleContract 默认 true） | 核查 config 是否被覆盖 | 证实：config 默认 true + 是否被覆盖；证伪：硬编码 | 否（config 普查） |
| A4.2.16 | A1.12 §7-5 | UC-HR-05 未到岗回退运行时处理（P2-RC-010 无 rollbackHire） | 运营调研 HR 是否经 close+重开处理未到岗场景 | 证实：HR 经 close+重开处理；证伪：无回退路径 | 否（运营流程调研） |
| A4.2.17 | A1.13 SP-1 | P1-RC-011 审批人超时自动转派缺失运行时影响面（SUBMITTED 休假悬挂量 vs 薪酬核算耦合） | 运行时确认 SUBMITTED 悬挂量与薪酬核算耦合度 | 证实：悬挂量影响薪酬核算；证伪：悬挂量与薪酬核算解耦 | 否（运行时数据采样） |
| A4.2.18 | A1.13 SP-2 | P1-RC-012 多次打卡 reject 运行时误判面（逃生路径 + HR 手工 DB 修正频度） | 运行时确认 reject 行为的运营影响（HR 手工修正频度 + 考勤申诉工单） | 证实：误判面 + HR 手工 DB 修正绕过；证伪：误判罕见 | 否（运营调研） |
| A4.2.19 | A1.13 SP-3 | P1-RC-013 夜班跨天 clockOut 运行时阻断（夜班占比 + 补录临时运维） | 运行时确认夜班占比与 clockOut 阻断频度 + 「夜班次日补录」临时运维 | 证实：夜班 clockOut 阻断频度高 + 补录运维普遍；证伪：夜班占比低 | 否（运行时数据采样） |
| A4.2.20 | A1.13 SP-4 | P1-RC-014 设备故障补卡运行时替代（标准 CRUD 绕过字段守卫越权风险） | 运行时确认补卡替代路径与权限（CrudBizModel 默认 save/update 不受字段级守卫保护） | 证实：补卡经标准 CRUD 绕过字段守卫（越权风险）；证伪：补卡有字段级守卫 | 否（权限观测） |
| A4.2.21 | A1.13 SP-5 | UC-HR-09⑲ 换班跨日期语义（ShiftSwapRequestSubmitProcessor 未校验同日期） | 构造跨日期换班场景确认语义（empA 7/1 ↔ empB 7/2 交换后语义可疑） | 证实：跨日期换班产生困惑语义 + 需补同日期守卫；证伪：运营实践不跨日期 | 否（语义观测） |
| A4.2.22 | A1.14 §7-1 | UC-HR-04 ⑯ 计提+公司承担过账运行时触发链（SALARY(270)+290/300 是否生成） | approve→APPROVED E2E 确认 GL 仅收 280 vs 290/300 event 是否生成 | 证实：HEAD 静态判定 = 永不生成（tryPostAccrual 零调用方）；证伪：290/300 实际生成 | 否（read-only 凭证观测） |
| A4.2.23 | A1.14 §7-2 | UC-HR-04 公司承担金额运行时丢弃确认（socialInsuranceER/housingFundER 是否经 billData 传递） | 断言 billData 不含 ER 金额（PayrollCalculator:110/115 计算 vs setRemark 丢弃） | 证实：公司承担金额经 remark/billData 丢弃；证伪：ER 金额正确传递 | 否（read-only 数据流观测） |
| A4.2.24 | A1.14 §7-3 | UC-HR-03 ②24h 校验运行时拦截（同日多条 TimesheetLine hours 之和 >24） | 构造 >24h 提交确认无报错（HEAD 静态判定 = 无校验） | 证实：无 24h 校验拦截；证伪：存在隐式校验 | 否（行为观测） |
| A4.2.25 | A1.14 §7-4 | UC-HR-11 ㉖匿名 respondentHash 运行时防重复（重复提交是否拦截） | 构造同 respondentHash 重复提交确认无拦截（HEAD 静态判定 = 无 writer/无校验） | 证实：匿名重复提交无拦截；证伪：存在隐式去重 | 否（行为观测） |
| A4.2.26 | A1.14 §7-5 | UC-HR-11 ㉘㉙ CLOSED 自动聚合 + eNPS 运行时计算（结果表是否永远空） | CLOSED 时确认 ErpHrSurveyResult 是否自动聚合 + eNPS 是否计算（HEAD 静态判定 = 无 mutation/无算法） | 证实：结果表永远空；证伪：CLOSED 触发聚合 | 否（行为观测） |

### 3.3 purchase 域（A1.15-A1.17，20 行 A4.2.27-A4.2.46）

> 完整字段见 roadmap MA4 表 A4.2.27-A4.2.46（每行含来源 + §7 锚点 + 摘要）。验证方法均复用 `runP2pChain` / `runP2pReverse` + 既有 JUnit + grep 普查 + 部署 config/doc 普查。触及保护区域 = 否（read-only 普查 / 既有 E2E 复用 / 凭证·审计列观测），除下表特别标注外：

| A4.2.n | 来源 | 摘要 | 保护区域 |
|--------|------|------|---------|
| A4.2.27-A4.2.32 | A1.15 §7-1..6 | UC-PUR-01 触发链 / paidStatus 一致性 / 应付余额恒等式 / 多供应商拆断（P1-RC-017）/ 承付恢复不对称（P1-MA2-083）/ 取消后再转化（P2-RC-012） | 否 |
| A4.2.33-A4.2.39 | A1.16 §7-1..7 | 两次入库凭证数==2 / 让步接收价格差异过账缺失（P1-RC-018）/ 三处理策略可达性 / 超收容差缺失（P1-RC-019）/ 短收超容差缺失（P2-RC-014）/ 关闭释放预留 config-gated / receivedQuantity 零 writer（P2-RC-013） | 否 |
| A4.2.40-A4.2.46 | A1.17 §7-1..7 | isReversed 标记缺失（P2-RC-015）/ credit-memo AP 余额回减（P2-MA2-006 复核）/ GR/IR 暂估应付凭证行 / **跨域期间 CLOSED guard（合并 G6）**/ 反审核删凭证（红字冲销）/ 承付恢复对称性（reuse P1-MA2-083）/ 多币种行级金额 | 否 |

### 3.4 sales 域（A1.18-A1.21，16 行 A4.2.47-A4.2.62）

| A4.2.n | 来源 | 摘要 | 保护区域 |
|--------|------|------|---------|
| A4.2.47 | A1.18 §7-1 + A1.19 §7-2（合并 G5） | 订单级可用量校验缺失运行时影响 | 否 |
| A4.2.48-A4.2.51 | A1.18 §7-2..5 | 最低价校验缺失触发面 / 价税分离 GL 影响（P1-RC-022）/ 客户应收余额双层设计（归 P2-MA2-038）/ 取价优先级链跨域协作 | 否 |
| A4.2.52-A4.2.55 | A1.19 §7-1,3,4,5 | 销售级 seam 并发行为 / 负库存配置并发结果 / deliveredQuantity 查询返回值 / 1 行×2 分批验证（P2-RC-019） | 否 |
| A4.2.56 | A1.20 SP-1 + A1.21 SP-1（合并 G7） | 价税分离多档税率混合+促销叠加 GL 偏差范围 | 否 |
| A4.2.57-A4.2.59 | A1.20 SP-2,4,5 | 退货成本库存策略偏差（P1-RC-026）/ ReturnRefundOrchestrator 并发竞态（P1-RC-028）/ 换货功能缺失 product-scope 确认（P1-RC-025） | 否（A4.2.59 触及**需求契约确认**，但验证探针为 read-only 真相源核对，不修改 product-scope） |
| A4.2.60-A4.2.62 | A1.21 SP-2,4,5 | 赠品成本多物料混合出库 abs() 求和 / AR 账龄 4 桶归类歧义（P2-RC-024）/ 赠品行 UI 标记缺口（P2-RC-023） | 否 |

### 3.5 assets 域（A1.22-A1.24，11 行 A4.2.63-A4.2.73）

| A4.2.n | 来源 | 摘要 | 保护区域 |
|--------|------|------|---------|
| A4.2.63-A4.2.66 | A1.22 SP-1..4 | 方式A 编排可达性（3 步链）/ 方式B 补提多漏提期偏差（P1-RC-029）/ 批量隔离 GL 科目缺失跳过 / 补提凭证 marker | 否 |
| A4.2.67-A4.2.69 | A1.23 SP-1..3 | 出售补提缺失会计影响量化（P1-RC-029）/ 合并科目凭证行级结构（P1-RC-030）/ posted=false 窗口 reverseApprove 行为（R1.16 P1-MA2-060） | 否 |
| A4.2.70-A4.2.73 | A1.24 SP-1,2,3,5 | 资本化折旧末期残值修正 / 维修资本化重算 PENDING→EXECUTED / 拆分 proportion tolerance 极端比例 / 盘亏 SCRAPPED 折旧计划 CANCELLED 同步 | 否 |

### 3.6 inventory 域（A1.25-A1.27，9 行 A4.2.74-A4.2.82）

| A4.2.n | 来源 | 摘要 | 保护区域 |
|--------|------|------|---------|
| A4.2.74-A4.2.75 | A1.25 SP-1,2 | InvPostingDispatcher post-commit 时序边缘风险 / forwardTrace 超深链 truncated | 否 |
| A4.2.76-A4.2.79 | A1.26 SP-1..4 | allow-negative-stock=true 并发出库下限 / batchTrace 跨域 move 链聚合 / expiryDate 无 writer 默认值 / MR1 P1-RC-031 reserved/available successor | 否 |
| A4.2.80-A4.2.82 | A1.27 SP-1,2,3 | completeTake DONE 后手工 generateMove 余额影响 / max-retry 耗尽移动单状态 / DeferredPostingSweepJob 兜底触发频率（P1-MA4-001 family） | 否 |

### 3.7 crm 域（A1.28-A1.30，18 行 A4.2.83-A4.2.100）

> A4.2.83-A4.2.100（A1.28 SP-1..6 + A1.29 SP-1..8 + A1.30 SP-1..4）。验证方法均 read-only（grep 普查 / E2E 构造 / config 普查 / 报表观测）。保护区域 = 否（crm 域不产生 GL 凭证，UTM/forecast/quota 缺失影响报表精度但不破坏 GL 平衡）。

### 3.8 quality 域（A1.31-A1.33，12 行 A4.2.101-A4.2.112）

> A4.2.101-A4.2.112（A1.31 SP-1,3,4,5 + A1.32 SP-1..4 + A1.33 SP-2..5）。验证方法均 read-only（运行时探针 / config 普查 / 审计可用性评估）。保护区域 = 否。

### 3.9 projects 域（A1.34-A1.36，11 行 A4.2.113-A4.2.123）

> A4.2.113（合并 G8）+ A4.2.114-A4.2.123。验证方法均 read-only（构造场景 + 断言归集行 / PnL 偏差观测 / config 普查）。保护区域 = 否。

### 3.10 cs 域（A1.37-A1.40，19 行 A4.2.124-A4.2.142）

> A4.2.124-A4.2.142（A1.37 SP-1..5 + A1.38 SP-1..4 + A1.39 SP-1..5 + A1.40 SP-1..5）。验证方法均 read-only（grep AMIS/xbiz + delta 普查 / cron 触发观测 / 并发断言 / 召回率采样）。保护区域 = 否（cs 域不产生 GL 凭证）。

### 3.11 master-data 域（A1.41，4 行 A4.2.143-A4.2.146）

> A4.2.143-A4.2.146（A1.41 SP-2..5）。验证方法均 read-only（grep 接线 / 并发 TOCTOU 探针 / 种子数据复核 / GraphQL delete 探针）。保护区域 = 否（A4.2.146 触及**数据完整性行为**（被引用 SKU 删除），但验证探针为 read-only GraphQL 调用观测，不修改 ORM/删除逻辑；软删路径保留物理行）。

### 3.12 maintenance 域（A1.42-A1.44，8 行 A4.2.147-A4.2.154）

> A4.2.147-A4.2.154（A1.42 SP-1 + A1.43 SP-1..3 + A1.44 SP-1..4）。验证方法均 read-only（端到端路径确认 / 跨域余额断言 / 跨域 guard 断言 / 隐式机制 grep）。保护区域 = 否。

### 3.13 contract 域（A1.45-46，8 行 A4.2.155-A4.2.162）

> A4.2.155-A4.2.162（A1.45-46 SP-1..8）。验证方法均 read-only（前端/XMeta 隐式校验 grep / v1 版本创建观测 / amend 行复制观测 / 全局 job 扫描 / InvoicePlan 隐式失效 / 审批引擎 grep / 折扣应用 grep / OCR 服务 grep）。保护区域 = 否。

### 3.14 b2b 域（A1.47，7 行 A4.2.163-A4.2.169）

> A4.2.163-A4.2.169（A1.47 SP-1..7）。验证方法均 read-only（Registry 派发观测 / 异步队列 grep / webhook archive 观测 / codeMappingResolver 观测 / 自动重试 grep / activate 状态守卫观测 / scheduler 自动停用 grep）。保护区域 = 否。

### 3.15 drp 域（A1.48，4 行 A4.2.170-A4.2.173）

> A4.2.170-A4.2.173（A1.48 SP-1..4）。验证方法均 read-only（0 值行下游拒绝观测 / config 默认值复核 / 事务边界观测 / SS 计算偏差观测）。保护区域 = 否。

### 3.16 logistics 域（A1.49，4 行 A4.2.174-A4.2.177）

> A4.2.174-A4.2.177（A1.49 SP-1..4）。验证方法均 read-only（双 shipment 凭证数断言 / webhook 滞留时长观测 / 容量超卖观测 / null 金额凭证断言）。保护区域 = 否。

### 3.17 aps 域（A1.50，4 行 A4.2.178-A4.2.181）

> A4.2.178-A4.2.181（A1.50 SP-1..4）。验证方法均 read-only（CRP 负荷断言 / 备选工作中心降级观测 / 派工物料齐套观测 / sales 交期承诺违约观测）。保护区域 = 否。

### 3.18 notify 域（A1.51，4 行 A4.2.182-A4.2.185）

> A4.2.182-A4.2.185（A1.51 SP-1..4）。验证方法均 read-only（ROLE resolver 角色数据一致性 / ${var} 插值多域解析 / deptId 精确匹配 vs 子部门递归 / kill-switch enabled=false 行为）。保护区域 = 否（notify 为 best-effort 通知子系统，不破坏业务事实）。

---

## 4. 计数自检（plan Phase 2 item 2）

| 域 | 报告 | §7 条目数（条目制） | 已展开为 A4.2.n | 编号区间 | 合并调整 |
|----|------|-------------------|----------------|---------|---------|
| mfg | A1.8-A1.11（4 报告） | 3+3+3+4=13 | 11 | A4.2.1-A4.2.11 | -2（G2/G3/G4 跨报告合并减 2：A1.9 SP-1/SP-2/SP-3 各合并 1，A1.11 SP-1 合并 1；A1.11 SP-3 拥有 G1 dashboard 合并行 +1，净 -2） |
| hr | A1.12-A1.14（3 报告） | 5+5+5=15 | 15 | A4.2.12-A4.2.26 | 0 |
| purchase | A1.15-A1.17（3 报告） | 6+7+7=20 | 20 | A4.2.27-A4.2.46 | 0（G6 跨域合并 A1.17 §7-4 拥有合并行，不减 purchase 计数） |
| sales | A1.18-A1.21（4 报告） | 5+5+5+5=20 | 16 | A4.2.47-A4.2.62 | -4（G5 减 1 + G6 减 1[A1.20 SP-3 合并入 A4.2.43] + G7 减 1[A1.21 SP-1 合并入 A4.2.56] + G1 减 1[A1.21 SP-3 合并入 A4.2.10]） |
| assets | A1.22-A1.24（3 报告） | 4+3+5=12 | 11 | A4.2.63-A4.2.73 | -1（G1 减 1：A1.24 SP-4 合并入 A4.2.10） |
| inventory | A1.25-A1.27（3 报告） | 2+4+4=10 | 9 | A4.2.74-A4.2.82 | -1（G1 减 1：A1.27 SP-4 合并入 A4.2.10） |
| crm | A1.28-A1.30（3 报告） | 6+8+4=18 | 18 | A4.2.83-A4.2.100 | 0 |
| quality | A1.31-A1.33（3 报告） | 5+4+5=14 | 12 | A4.2.101-A4.2.112 | -2（G3 减 1：A1.31 SP-2 合并入 A4.2.5 + G1 减 1：A1.33 SP-1 合并入 A4.2.10） |
| projects | A1.34-A1.36（3 报告） | 5+4+5=14 | 11 | A4.2.113-A4.2.123 | -3（G8 减 2：A1.35 SP-1/SP-3 合并入 A4.2.113 + G1 减 1：A1.36 SP-5 合并入 A4.2.10） |
| cs | A1.37-A1.40（4 报告） | 5+4+5+5=19 | 19 | A4.2.124-A4.2.142 | 0 |
| master-data | A1.41（1 报告） | 5 | 4 | A4.2.143-A4.2.146 | -1（G1 减 1：A1.41 SP-1 合并入 A4.2.10） |
| maintenance | A1.42-A1.44（3 报告） | 1+3+5=9 | 8 | A4.2.147-A4.2.154 | -1（G1 减 1：A1.44 SP-5 合并入 A4.2.10） |
| contract | A1.45-46（1 报告/2 切片） | 8 | 8 | A4.2.155-A4.2.162 | 0 |
| b2b | A1.47（1 报告） | 7 | 7 | A4.2.163-A4.2.169 | 0 |
| drp | A1.48（1 报告） | 4 | 4 | A4.2.170-A4.2.173 | 0 |
| logistics | A1.49（1 报告） | 4 | 4 | A4.2.174-A4.2.177 | 0 |
| aps | A1.50（1 报告） | 4 | 4 | A4.2.178-A4.2.181 | 0 |
| notify | A1.51（1 报告） | 4 | 4 | A4.2.182-A4.2.185 | 0 |
| **合计** | **43 报告（44 切片）** | **200** | **185** | A4.2.1-A4.2.185 | **-15（8 组合并）** |

**自检结论**：
- ✅ A4.2.n 行数（185）= 扩展域 §7 存疑点去重并集数（200 原始 - 15 合并减少 = 185）。
- ✅ 逐报告核对：A1.8-A1.51 各报告 §7 存疑点均已纳入或显式合并（43/43 报告，无遗漏；无「无存疑点」切片）。
- ✅ 无抽样：每个 §7 存疑点恰好一行（完整枚举纪律 §3），合并组有显式依据（§2 G1-G8）。
- ✅ roadmap MA4 表 A4.2.1-A4.2.185 已追加（见 `requirement-compliance-roadmap.md` MA4 表）。
- ✅ 与 A4.1（finance 业财域 A4.1.1-A4.1.25）不重叠（A4.1 = finance A1.1-A1.7 / A4.2 = 扩展域 A1.8-A1.51，切片范围互斥）。

---

## 5. 触及保护区域标注汇总（§5 保护区域暂停协议）

> 展开器不执行验证；本汇总标注各 A4.2.n **验证探针**是否触及保护区域（供 mission driver 执行到该行时按 §5 暂停协议门控）。

| 保护区域类别 | A4.2.n 行 | 数量 | 门控 |
|------------|----------|------|------|
| 不触及（read-only 普查 / 既有 E2E 复用 / JUnit 断言补强 / 报表·凭证·审计列观测 / 部署 config·doc 普查 / 跨域行为观测 / 运营流程调研 / 语义观测 / 权限观测 / 数据完整性 GraphQL 探针） | A4.2.1-A4.2.185（全部 185 行） | 185 | 预授权自动执行（验证探针不改 ORM/过账核心/删除逻辑） |
| 会计过账/成本过账行为探针 | 无 | 0 | — |
| 数据删除行为探针 | 无 | 0 | — |
| ORM 结构变更 | 无 | 0 | — |
| **合计** | 185 | 0 触及 + 185 不触及 | — |

> **与 A4.1 对比**：A4.1 有 2 行触及保护区域（A4.1.15 成本过账探针 / A4.1.16 数据删除探针），因 finance 域存疑点含 FIFO+到岸成本数值正确性 + delta 层物理删除行为。A4.2 扩展域存疑点均为 read-only 行为/配置/语义/运营流程类观测，无新探针 exercise 会计过账核心/数据删除/ORM 结构变更。
>
> **个别行的特殊注意**（非保护区域门控，但 mission driver 执行时留意）：
> - **A4.2.59**（换货功能缺失 product-scope 确认）：触及**需求契约确认**（§4 真相源层级），验证探针为 read-only product-scope 核对，不修改 product-scope。若确认真相源需修订（需求变更），按 §9 真相源冻结条款经人工批准。
> - **A4.2.146**（被引用 SKU 删除数据完整性事件）：触及**数据完整性行为**（P1-RC-062），验证探针为 read-only GraphQL `ErpMdMaterialSku__delete` 调用观测，不修改 ORM/删除逻辑。软删路径（useLogicalDelete=true）保留物理行。
> - **A4.2.3**（MR1 预留写路径 successor）：successor 触发条件 = MR1 P1-RC-008 修复落地后，本展开器不执行验证。

---

## 6. 过程纪律自检（§8 模板）

- [x] **checker 退出码门控核查**：本展开器产出后已运行 `bash docs/audits/nop-compliance-checker.sh`。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（方法论 §8），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本展开器**不**以 checker 脚本退出码作为门控通过依据。**本展开器无生产代码变更**（纯 roadmap 表追加 + 展开映射记录），checker 无回归风险。actual vs baseline 实测见下表。

  | 规则 | Baseline（machine-readable） | Actual（本展开器 HEAD 实测） | 状态 |
  |------|------------------------------|----------------------------|------|
  | R1a/R1b/R1c | 0/0/0 | 0/0/0 | ✅ |
  | R1d | 14 | 14 | ✅ |
  | R2a | 34 | 34 | ✅ |
  | R2b | 229 | 229 | ✅ |
  | R2c | 1382 | 1382 | ✅ |
  | R2d | 34 | 34 | ✅ |
  | R3-R12 | （既有基线） | 脚本输出在 R3 header 后截断（既有工具行为，非本展开器引入；本展开器零代码变更，无回归风险） | ✅（无回归风险） |

  > R1/R2 全部 actual == baseline，**0 漂移**。R3-R12 脚本输出截断是既有工具行为（与零代码变更的展开器无关）；权威基线以 `compliance-baseline.md §BASELINE (machine-readable)` 为准。本展开器不触发 CI（无代码变更）。

- [x] **closure-audit 独立性声明**：本展开器的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本展开器为**展开器工作项**（只读收集 + roadmap 表追加），**不新建 finding**。185 条 A4.2.n 行是运行时验证项的展开（非 finding 结论）；各 A4.2.n 验证结论才新建/复用 finding（届时按 §7 规则 grep arm-index 后裁决）。本展开器无未经比对直接新建的 finding。

---

## 7. 真相源冻结声明（§9）

本展开器未修改任何冻结真相源（`product-scope.md` / 各域 `use-cases.md` / owner doc 需求契约段落）。roadmap MA4 表追加 A4.2.1-A4.2.185 行 + A4.2 状态 `todo → done` 是工作项追踪更新（非冻结真相源），属展开器既定动作（plan Non-Goals 明示「roadmap MA4 表追加 A4.2.n 行是工作项追踪更新，属展开器既定动作」）。

---

## 8. Verdict

**Verdict: passes expander completion**（A4.2 展开完成）

- **展开完整性**：扩展域 A1.8-A1.51 §7 存疑点全集（43 报告 44 切片，200 条原始条目，条目制）已逐条展开 + 跨报告去重（8 组合并减少 15 行）为 A4.2.1-A4.2.185，追加到 roadmap MA4 表，0 遗漏、0 抽样。
- **去重**：8 组跨报告合并（G1 dashboard orgId 8→1 / G2 完工差异告警 2→1 / G3 返工工单工作流 2→1 / G4 预留写路径 successor 2→1 / G5 订单级库存校验 2→1 / G6 跨域期间 CLOSED guard 2→1 / G7 价税分离 GL 偏差 2→1 / G8 ExpenseCostAggregator 归集 3→1），合并依据显式（§2 表）；4 对主题相关但不同控制点保留独立行 + 交叉引用。
- **计数自检**：A4.2.n 行数（185）= §7 去重并集（200-15=185），逐报告核对 43/43 通过。
- **保护区域标注**：0 行触及（全部 185 行均为 read-only 观测/普查/调研类，无会计过账/数据删除/ORM 结构变更探针）；2 行特别注意（A4.2.59 需求契约确认 / A4.2.146 数据完整性行为）非保护区域门控。
- **A4.2 状态**：done（展开完成，非验证完成）。
- **A4.2.n 状态**：todo（各自待独立 plan + 独立验证 done；mission driver 逐项 DRAFT_PLANS → 验证）。

**剩余工作**（非本展开器范围）：各 A4.2.n 实体行的运行时验证（各自独立 plan + 独立 done）。MA4 里程碑 done 判据 = A4.1 + A4.2 done + 全部 A4.1.n/A4.2.n 实体行 done。
