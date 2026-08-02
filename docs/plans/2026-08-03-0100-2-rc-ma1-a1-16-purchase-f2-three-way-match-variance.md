# 2026-08-03-0100-2 rc-ma1-a1-16-purchase-f2-three-way-match-variance purchase-F2 三单匹配与差异需求符合性审计

> Plan Status: active
> Last Reviewed: 2026-08-03
> Mission: requirement-compliance
> Work Item: A1.16（MA1 需求追踪矩阵审计 — purchase-F2 三单匹配与差异：UC-PUR-02 三单匹配 + UC-PUR-03 部分入库与分批收货 + UC-PUR-05 价格差异 + UC-PUR-06 数量差异）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.16
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.16 的 0.2 依赖）、`2026-08-03-0100-1-rc-ma1-a1-15-purchase-f1-mainflow-requisition.md`（A1.15 同批，purchase 主流程/回链/入库为三单匹配前置）、`2026-08-02-1600-1-rc-ma1-a1-1-finance-f1-posting-engine.md`（A1.1 done，业财过账引擎范式）、`2026-08-02-2250-3-rc-ma1-a1-14-hr-f3-payroll-survey.md`（A1.14 done，最新同范式参考）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点被审功能现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.16 给出 UC 清单 = `UC-PUR-02/03/05/06`（4 UC），锚点 `use-cases.md:55 / :81 / :130 / :151`（baseline inventory :86/:87/:89/:90 + 切片索引确认一致 ✅）。

- **L1 需求契约（权威真相源）**：`docs/design/purchase/use-cases.md`：
  - UC-PUR-02 三单匹配（`:55`）：验证订单/入库/发票三方一致性。验收标准：①回链三元组：发票行.来源单类型==采购入库 + 发票行.来源单号==入库单.单号 + 发票行.来源行号==入库行.行号；②数量匹配：入库数量之和 <= 订单数量 * (1 + 超收容差)；③价格匹配：|发票单价-订单单价| <= 订单单价 * 价格容差，否则→匹配状态=价格差异待处理；④可追溯：每条发票行→可追溯到入库行与订单行。
  - UC-PUR-03 部分入库与分批收货（`:81`）：订单(数量=100)审核→第一次入库(60)审核→第二次入库(40)审核。验收标准：①订单行.已入库数量==60（第一次后）；②订单行.已入库数量==100（第二次后，派生字段）；③凭证数量==2（两次入库各自独立过账）；④订单.单据状态 != 已关闭（未全部入库前不自动关闭）。
  - UC-PUR-05 价格差异（`:130`）：供应商发票单价高于订单约定价。验收标准：①差异=发票单价-订单单价；②若|差异|>订单单价*价格容差→匹配状态=价格差异待处理；③处理策略 ∈ {拒绝, 审批后接收, 接收并过账差异}；④让步接收时存在过账行：科目==价格差异科目 且 金额==差异*数量。
  - UC-PUR-06 数量差异/短收（`:151`）：入库数量少于订单。验收标准：①短收数量=订单数量-入库数量之和；②若短收数量<=容差→订单可继续入库或手动关闭；③若短收数量>容差→触发差异处理；④按实际入库过账，不按订单（凭证金额基于实际入库数量，非订单数量）；⑤长期未收的余量：订单.关闭()→单据状态=已作废，释放预留。

- **L2 owner doc 设计参考**：`docs/design/purchase/three-way-match.md`（§匹配规则 / §回链关系 / §数量匹配 / §价格差异 / §不匹配的处理策略；**P2-MA2-005 owner doc 内部不一致：§一致性规则:92「失败则拒绝审核」vs §匹配严格度:84-88「默认非严格 warn+放行」watch-only**；代码遵循可配 strict-default-false）+ `docs/design/purchase/state-machine.md`（订单已入库数量派生字段 + 关闭/释放预留）+ `docs/design/purchase/README.md`。**注意**：L2 为设计参考，与 L1 冲突时按 §4 Q1 以 L1 为准。

- **L3 代码实现现状（执行时实测核验，路径已确认存在）**：
  - **三单匹配核心（UC-PUR-02/05/06）**：`entity/ThreeWayMatcher.java`（匹配引擎：回链三元组校验 + 数量容差 erp-pur.match-qty-tolerance + 价格容差 + 匹配状态派生；**P2-MA2-004 dead config read watch-only：qtyTolerance 计算后被空守护置零，invoice 侧未使用，invoice>receive 硬拒绝无容差；qty 容差语义仅作用于 receive-vs-order 侧**）。
  - **部分入库/分批（UC-PUR-03）**：`ErpPurReceiveBizModel.java` + `ErpPurReceiveLineBizModel.java`（订单行.已入库数量 派生字段聚合）+ `processor/ErpPurReceiveApproveProcessor.java`（每次入库独立过账 GOODS_RECEIPT——触发路径归 A1.15 核验，本切片核验"两次入库各自独立过账"行为）+ `ErpPurOrderLineBizModel.java`（已入库数量派生）。
  - **价格差异处理（UC-PUR-05）**：`ThreeWayMatcher.java`（价格容差 + 匹配状态=价格差异待处理）+ 处理策略实现（拒绝/审批后接收/接收并过账差异——执行时核验三策略是否完整落地 + 让步接收价格差异科目过账行）+ `ErpPurInvoiceApproveProcessor.java`（让步接收过账差异）。
  - **数量差异/短收（UC-PUR-06）**：`ThreeWayMatcher.java`（超收容差 + 短收判定）+ `ErpPurOrderBizModel.java`（订单关闭 cancel → 释放预留）+ `ErpPurOrderProcessor.java`（关闭流程）+ 凭证金额基于实际入库（执行时核验：GOODS_RECEIPT 凭证金额取实际入库数量而非订单数量）。
  - **执行时核验点**：ThreeWayMatcher 三策略完整性（UC-PUR-05 ③）+ 价格差异科目过账行（UC-PUR-05 ④）+ 容差配置语义（P2-MA2-004）+ 短收余量关闭释放预留（UC-PUR-06 ⑤）。

- **L4 测试证据现状**：`TestErpPurThreeWayMatch`（三单匹配 + 价格/数量容差 + 匹配状态）+ `TestErpPurSettleThreeWayMatchRecheck`（settle 后三单匹配复检）+ `TestErpPurReceiveApproval`（部分入库派生已入库数量）+ `TestErpPurReceiveStockMove`（库存 incoming）+ `TestErpPurOrderApproval`（订单关闭）+ `TestErpPurInvoiceApproval`（发票匹配）。E2E：`tests/e2e/orchestration/p2p-chain.spec.ts`（含部分入库场景）。**执行时核验断言强度**：UC-PUR-02 ①-④ + UC-PUR-03 ①-④ + UC-PUR-05 ①-④ + UC-PUR-06 ①-⑤ 各验收标准是否有强断言（价格差异科目过账行 / 短收容差边界 / 两次入库独立过账）。

- **L5 既有证据（MA2 复用输入，方法论 §去重协议）**：
  - **`docs/audits/2026-07-28-0230-arm-ma2-purchase-state-machine.md`（A2.8）= purchase 9 实体状态机审查**：Verdict 主路径状态迁移守卫齐全 + 跨域写经 I*Biz Facade。**零 P0**；本切片相关 watch-only finding 来源于 **`docs/audits/2026-07-27-1949-arm-ma2-procure-to-pay-e2e.md`（audit-remediation A2.1 P2P 端到端审计）**：P2-MA2-004（ThreeWayMatcher dead config read watch-only，qty 容差 invoice 侧未用）+ P2-MA2-005（three-way-match.md 内部不一致 watch-only）+ P2-MA2-007（订单审核价格锁缺失 watch-only）+ P2-MA2-008（PaymentSettler 并发归 A2.17，本切片不涉及）；A2.8 另登记 P1-MA2-003（付款核销缺发票三单匹配完成态复核，**resolved plan 2026-07-29-2322-1 方案 A**——PaymentSettler 注入 ThreeWayMatcher + recheckThreeWayMatchAtSettle 强制 strict 复核 + config-gated 默认 false，**HEAD 复核 UC-PUR-02 三单匹配完成态在 settle 前置的落地**——与 UC-PUR-02 ④可追溯 + 三单匹配完成态语义相关）。
  - **`ma2-procure-to-pay-e2e`（A2.1 P2P 端到端审计）**：三单匹配 / 部分入库 / 差异处理主路径行为已证实，本切片只补"需求契约↔实际行为"差异（验收标准逐条视角，重点：UC-PUR-05 价格差异科目过账行 / UC-PUR-06 短收关闭释放预留 是否有强断言）。
  - **注意**：A2.8/A2.1 P2P e2e 覆盖**状态机迁移/链路行为**。本切片从**需求契约↔实现符合性**视角补差异（UC-PUR-02/03/05/06 验收标准逐条 + ThreeWayMatcher 容差语义 + 价格差异处理策略完整性 + 短收关闭释放预留 + **resolved/watch-only finding HEAD 复核**：P1-MA2-003/ P2-MA2-004/005/006/007 状态确认）。

- **arm-index 既有 finding 衔接**：三单匹配与差异相关——`P1-MA2-003`（付款核销缺发票三单匹配完成态复核，**resolved plan 2026-07-29-2322-1 方案 A**——PaymentSettler.recheckThreeWayMatchAtSettle + `erp-pur.settle-recheck-three-way-match` config-gated，**HEAD 复核与 UC-PUR-02 三单匹配完成态契约一致性**）/ `P2-MA2-004`（ThreeWayMatcher dead config read watch-only，**HEAD 复核 qty 容差 invoice 侧是否仍未用**）/ `P2-MA2-005`（three-way-match.md 内部不一致 watch-only，**HEAD 复核 owner doc 是否已统一**）/ `P2-MA2-007`（订单审核价格锁缺失 watch-only，**与 UC-PUR-05 价格差异相关**）/ `P2-MA2-006`（returns.md red invoice drift **resolved plan 2026-07-29-2322-1**，归 A1.17 但本切片交叉引用）/ `P1-MA1-022`（跨域只读 daoFor 维持治理缺陷）。UC-PUR-05 价格差异科目过账行完整性 / UC-PUR-05 三处理策略完整性 / UC-PUR-06 短收关闭释放预留 / UC-PUR-03 两次入库独立过账为候选新维度（既有审计未从需求契约视角裁决），执行时 grep `arm-index.md` purchase 三单匹配/容差/价格差异/数量差异同域同控制点后裁决复用 or 新建 `P*-RC-xxx`。

- **保护区域**：本审计为**只读审计**（读代码/测试/报告，不改代码/ORM/api.xml/真相源）。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按方法论 §10，P0 经 MR0 即时通道、P1 经 MR1（R1.0 展开 RC-R1.n）；**触及会计过账逻辑（价格差异科目过账 / PostingProvider / VoucherFact / PostingProcessor 核心路径）的修复行须 ask-first + 独立 plan-audit**（§5 保护区域暂停协议）。

- **剩余差距**：A1.16 切片的五级追踪审计报告缺失 = MA4（A4.1 业财域展开器，Deps=MA1 done）及 MR1（R1.0，Deps=MA1-MA4 done）的该切片证据缺口来源。本计划产出 A1.16 报告并登记 finding，解除其在 MA4/MR1 链路的该切片证据缺口。

## Goals

- 产出 A1.16 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-16-purchase-f2-three-way-match-variance.md`，含方法论 §6 **9 段全部内容**：①UC-PUR-02/03/05/06 需求契约原文（逐字引用，不转述）②实现证据（`file:line`，含 ThreeWayMatcher + Receive/Order/Invoice BizModel 与 Processor）③测试证据（注明断言强度）④运行时行为证据（复用 A2.8/P2P e2e，补差异）⑤五级追踪矩阵 + 每 UC 符合性结论（P0/P1/P2/接受）⑥与 arm-index 衔接（复用 or 新增 裁决）⑦静态存疑点清单（供 MA4 展开）⑧过程纪律自检段 ⑨与 MA2 报告差异增量声明。
- 对 4 UC 逐条核验**每条验收标准**（完整枚举，§3）：UC-PUR-02（①回链三元组 + ②数量匹配超收容差 + ③价格匹配价格容差+匹配状态 + ④可追溯）+ UC-PUR-03（①第一次后已入库60 + ②第二次后已入库100派生 + ③两次入库各自独立过账凭证数==2 + ④未全部入库前不自动关闭）+ UC-PUR-05（①差异计算 + ②价格容差触发匹配状态 + ③三处理策略{拒绝/审批后接收/接收并过账差异}完整性 + ④让步接收价格差异科目过账行）+ UC-PUR-06（①短收数量计算 + ②<=容差继续/关闭 + ③>容差触发差异处理 + ④按实际入库过账非订单 + ⑤关闭释放预留），各一矩阵行。
- 对候选缺口/偏离给出分级结论：**UC-PUR-05 价格差异科目过账行完整性**（HEAD 核验：让步接收是否存在 科目==价格差异科目 且 金额==差异*数量 的过账行；缺失→P1①功能缺失 or P0④会计过账正确性按 §2 定级）+ UC-PUR-05 三处理策略完整性（缺失→P1①）+ UC-PUR-06 短收关闭释放预留（HEAD 核验）+ UC-PUR-03 两次入库独立过账（HEAD 核验凭证数量==2）+ **resolved/watch-only finding HEAD 复核**：P2-MA2-004/005/007——按 §2 判据定级，若为 P0/P1 则新建 `P0-RC-xxx`/`P1-RC-xxx` 并按 §10 触发 MR0/MR1（本计划仅登记，不实施修复）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区）。

## Non-Goals

- **不修复 finding**（修复属 MR0 即时通道 / MR1 R1.0 展开的 RC-R1.n；本计划是审计，结果表面 = 一份报告 + arm-index 登记）。
- **不修改真相源**（product-scope / purchase use-cases / three-way-match.md / state-machine.md 需求契约段落；§9 冻结条款——分歧记入报告，不直改真相源）。
- **不修改代码/ORM/api.xml/BizModel/Processor/view.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.16 只覆盖 UC-PUR-02/03/05/06；UC-PUR-01/08 归 A1.15，UC-PUR-04/07 归 A1.17）。**UC-PUR-03 涉及的 GOODS_RECEIPT 过账触发路径归 A1.15 核验**，本切片只核验"两次入库各自独立过账"行为；**UC-PUR-05 让步接收价格差异过账归本切片**（价格差异科目属会计正确性）。
- **不重跑既有状态机/P2P 链路行为审计**（§去重协议：A2.8/P2P e2e 已证实状态机迁移/链路行为，只补需求视角差异；不重审架构/代码质量维度）。

## Task Route

- Type: `verification or audit work`（需求→实现符合性五级追踪审计；非实现变更、非需求澄清）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（审计契约 §1-§10 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.16 工作项 + Work Item Details MA1）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.16 UC 锚点）+ `docs/design/purchase/use-cases.md`（L1 真相源）+ `docs/design/purchase/three-way-match.md` + `state-machine.md` + `README.md`（L2 设计参考，非真相源）+ `docs/audits/arm-index.md`（finding 衔接）+ A2.8/P2P e2e 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。该技能定义多维审计 prompt 范式，本切片需求↔实现符合性审计复用其维度框架；其必需输入（owner doc + use-cases + 代码路径 + 测试）均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。**L5 行为证据**默认复用 A2.8/P2P e2e 审计（方法论 §去重协议），无需起服务；若需对存疑点做即时行为确认，可跑既有 JUnit（`mvn test -pl module-purchase/erp-pur-service -Dtest=TestErpPurThreeWayMatch,TestErpPurSettleThreeWayMatchRecheck,TestErpPurReceiveApproval,TestErpPurInvoiceApproval`），不引入新依赖。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更故无回归风险，仅记录 actual vs baseline）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论 + watch-only finding HEAD 复核

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-16-purchase-f2-three-way-match-variance.md`（落盘 §1-§5；命名遵循方法论 §归档规范）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done（方法论契约 + UC 锚点就绪）

- [ ] `Proof` 对 UC-PUR-02/03/05/06 **逐 UC 一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:55/:81/:130/:151` 验收标准原文（禁止转述）；L2 引用 `three-way-match.md`（§匹配规则/§回链关系/§数量匹配/§价格差异/§不匹配的处理策略，标注 P2-MA2-005 内部不一致 + "设计参考，冲突以 L1 为准"）+ `state-machine.md`（已入库数量派生 + 关闭/释放预留）；L3 引用 `entity/ThreeWayMatcher.java:line`（回链三元组 + 数量/价格容差 + 匹配状态）+ `ErpPurReceiveBizModel.java:line` + `ErpPurReceiveLineBizModel.java:line`（已入库数量派生）+ `ErpPurReceiveApproveProcessor:line`（每次入库独立过账）+ `ErpPurInvoiceApproveProcessor.java:line`（让步接收价格差异过账）+ `ErpPurOrderBizModel.java:line` + `ErpPurOrderProcessor.java:line`（关闭释放预留）；L4 引用 `Test*.java#method`（注明断言强度）；L5 复用 A2.8/P2P e2e + 本切片差异。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 重点核验**候选缺口/偏离**（逐条验收标准对照）：UC-PUR-02——①回链三元组（来源单类型/单号/行号）；②数量匹配超收容差（erp-pur.match-qty-tolerance）；③价格匹配价格容差 + 匹配状态=价格差异待处理；④可追溯（每发票行→入库行→订单行）。UC-PUR-03——①第一次后已入库数量==60；②第二次后==100（派生）；③两次入库各自独立过账（凭证数==2）；④未全部入库前订单不自动关闭。UC-PUR-05——①差异=发票单价-订单单价；②|差异|>订单单价*价格容差→匹配状态；③三处理策略{拒绝/审批后接收/接收并过账差异}完整性（**关键**）；④让步接收价格差异科目过账行（科目==价格差异科目 且 金额==差异*数量，**会计正确性关键**）。UC-PUR-06——①短收数量计算；②<=容差继续/关闭；③>容差触发差异处理；④按实际入库过账非订单数量；⑤关闭()→作废+释放预留。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` **watch-only/resolved finding HEAD 复核**：**P1-MA2-003 付款核销三单匹配完成态复核（resolved plan 2026-07-29-2322-1 方案 A）**——HEAD 复核 `PaymentSettler.recheckThreeWayMatchAtSettle` + `erp-pur.settle-recheck-three-way-match` config-gated 接线（与 UC-PUR-02 ④可追溯 + 三单匹配完成态契约一致性）；**P2-MA2-004 ThreeWayMatcher dead config read（watch-only）**——HEAD 复核 qty 容差 invoice 侧是否仍空守护置零（watch-only 维持 P2 or 升级评估）；**P2-MA2-005 three-way-match.md 内部不一致（watch-only）**——HEAD 复核 owner doc §一致性规则 vs §匹配严格度 是否已统一；**P2-MA2-007 订单审核价格锁缺失（watch-only）**——HEAD 复核（与 UC-PUR-05 价格差异契约相关，若订单审核后可改价则影响价格匹配基准）。逐条记录复核结论（仍 watch-only / 已修复 / 升级 P1）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Decision` 按 §2 判据对每 UC 给出符合性结论（P0/P1/P2/接受）：UC-PUR-05 价格差异科目过账行（HEAD 复核：完整→接受 on ④；缺失→P0④会计过账正确性破坏 or P1①功能缺失按 §2 定级，**会计类 Q4 无例外**）；UC-PUR-05 三处理策略完整性（缺失→P1①）；UC-PUR-06 短收关闭释放预留（HEAD 核验）；UC-PUR-03 两次入库独立过账（HEAD 核验凭证数==2）；watch-only/resolved finding HEAD 复核（P1-MA2-003/P2-MA2-004/005/007）。每结论须列明命中判据编号 + 三源对照。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [ ] 报告 §1-§5 已落盘：UC-PUR-02/03/05/06 各一矩阵行（验收标准全覆盖），L1 逐字引用、L3 含行号、L4 注明断言强度、L5 标注复用 A2.8/P2P e2e 来源
- [ ] 每 UC 有符合性结论（P0/P1/P2/接受）+ §2 判据编号；候选缺口有明确分级（非悬空"待查"）；**UC-PUR-05 价格差异科目过账行完整性 HEAD 复核结论已记录（会计正确性类 Q4 关键证据）**；P1-MA2-003/P2-MA2-004/005/007 HEAD 复核结论已记录

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-16-purchase-f2-three-way-match-variance.md`（落盘 §6-§9，报告定稿）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成（矩阵 + 结论已出）

- [ ] `Decision` **复用 or 新增 裁决**（§7）：产出 finding 前 grep `arm-index.md` purchase 三单匹配/容差/价格差异/数量差异同域同控制点（如 P1-MA2-003、P2-MA2-004/005/007、P1-MA1-022）后裁决——同根因同控制点 → 复用既有 ID（追加 RC 交叉引用注记，不新建）；新根因/新功能点（如 UC-PUR-05 价格差异科目过账行缺失 / UC-PUR-05 三处理策略缺失 / UC-PUR-06 关闭释放预留缺失）→ 新建 `P0-RC-xxx`/`P1-RC-xxx` 并列明与既有 finding 的差异依据。**特别注意**：UC-PUR-05 价格差异过账与 A1.1 业财过账引擎同根因则交叉引用。禁止未经比对直接新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 的复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR0/MR1）。
      - Skill: none
- [ ] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记本切片 L5 无法静态定论、需运行时确认的点（如价格差异科目过账行运行时生成 / 三处理策略运行时分支可达 / 短收关闭运行时释放预留 / 两次入库独立过账运行时凭证数 / 容差配置运行时语义；每存疑点一行；无则注明"无"）。**P0 即时通道**：若 Phase 1 定级出 P0，按 §10 在报告登记并在本计划记录"已触发 MR0 追加 R0.n 实体行"（本计划不实施修复）。
      - Skill: none
- [ ] `Proof` 报告 §8 过程纪律自检段（§8 模板）：实际运行 `bash docs/audits/nop-compliance-checker.sh` 并附 actual vs baseline 汇总表（本审计无生产代码变更，注明"无回归风险"）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 脚本退出码 0 作为门控通过依据**（区分 reporter vs CI 门控）。
      - Skill: none
- [ ] `Add` 报告 §9 与 MA2 报告差异增量声明：声明复用 A2.8（9 实体状态机 + 跨域 Facade）+ A2.1 P2P e2e（三单匹配/部分入库/差异处理链路行为 + **P1-MA2-003[resolved plan 2026-07-29-2322-1 方案 A] + P2-MA2-004/005/007** watch-only finding）已证实结论，列明本切片只补的需求视角差异（UC-PUR-02/03/05/06 验收标准逐条 + 价格差异科目过账行 + 三处理策略完整性 + 短收关闭释放预留 + watch-only/resolved finding HEAD 复核）。
      - Skill: none
- [ ] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 `P*-RC-xxx` 入对应分区（MA1 finding 区），既有行追加 RC 交叉引用注记。
      - Skill: none
- [ ] `Proof` 报告 9 段完整性自检（§6 段落完整性自检）：落盘前自查 §1-§9 全部存在；缺任一段即回到 Phase 补齐。
      - Skill: none

Exit Criteria:

- [ ] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据（无未经比对新建）
- [ ] 新 RC finding 已写入 `arm-index.md` 对应分区；静态存疑点清单已登记（供 A4.1 展开）
- [ ] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 `ses_03cbb528daffeDJIDFbANSwoeSD`，fresh session，未起草本计划）。逐项实测核验：roadmap 对齐（A1.16 / UC-PUR-02/03/05/06 共 17 验收标准 / Deps=0.2 done / Skill）、UC 锚点 :55/:81/:130/:151 全匹配、L3 全部 7 代码路径 + L4 4 测试全存在、arm-index P2-MA2-004/005/007 watch-only 全 confirmed；跨切片边界正确（UC-PUR-01/08→A1.15、UC-PUR-04/07→A1.17、GOODS_RECEIPT 触发→A1.15 本切片仅核验"两次入库独立过账"、UC-PUR-05 价格差异科目过账→in-scope 会计正确性）；**UC-PUR-05 ④ 价格差异科目过账 Q4 framing sound**（P0④ or P1① missing，会计类 Q4 无例外，无 scheme B 例外）；9 段全覆盖；anti-slack 干净；rule 14 bundling 正确。**1 项 MINOR 非阻塞**：P2-MA2-004/005/007 的 source-file attribution 应为 `ma2-procure-to-pay-e2e`（A2.1）而非 A2.8 状态机报告（已在迭代 1 修订中纠正为 A2.1 P2P e2e 来源 + 追加 P1-MA2-003 resolved plan 2026-07-29-2322-1 方案 A settle 三单匹配复核）。**无阻塞 issue**，共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控——审计报告产出不触发编译或测试。验证 = 报告 9 段完整性 + 五级矩阵逐 UC 覆盖 + watch-only/resolved finding HEAD 复核 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [ ] 范围内行为完成：A1.16 报告 9 段齐全 + UC-PUR-02/03/05/06 逐矩阵行 + watch-only/resolved finding HEAD 复核（含 P1-MA2-003 settle 三单匹配复核 + UC-PUR-05 价格差异科目过账会计正确性）+ finding 登记入 arm-index
- [ ] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.16 锚点一致
- [ ] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（本计划无代码变更故不跑 build/test）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留空作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按方法论 §10 经 MR0（P0 即时通道）/ MR1（R1.0 展开 RC-R1.n，P1 批量）实施；**触及会计过账逻辑（价格差异科目过账 / PostingProvider / VoucherFact / PostingProcessor 核心路径）的修复行须 ask-first + 独立 plan-audit**（§5 保护区域暂停协议，会计过账核心路径类）。本审计闭环不阻塞于修复落地。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行）

## Closure

Status Note: <关闭时填写>

Closure Audit Evidence:

- Auditor / Agent: <独立审计者或独立子代理>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- finding 修复属 MR0（P0 即时通道）/ MR1（R1.0 展开 RC-R1.n）successor，非阻塞本审计闭环（§Deferred But Adjudicated 已 adjudicated）
