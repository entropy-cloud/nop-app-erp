# 2026-08-02-2042-1 rc-ma1-a1-7-finance-f7-reports-dashboards-multischema finance-F7 报表/看板/多账套需求符合性审计

> Plan Status: completed
> Last Reviewed: 2026-08-02
> Executed: 2026-08-02（报告 `docs/audits/2026-08-02-2115-rc-ma1-a1-7-finance-f7-reports-dashboards-multischema.md` 落盘 + arm-index 更新；2 新 finding P1-RC-007/P2-RC-008 + 复用 P1-MA2-093 + A1.2 caveat ③ 收口）
> Mission: requirement-compliance
> Work Item: A1.7（MA1 需求追踪矩阵审计 — finance-F7 报表/看板/多账套：UC-FIN-05 多账套并行过账 + UC-FIN-16 财务三大报表 + UC-FIN-17 财务看板）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.7
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.7 的 0.2 依赖）、`2026-08-02-1815-3-rc-ma1-a1-6-finance-f6-period-close.md`（A1.6 done，同 finance 审计范式）、`2026-08-02-1600-2-rc-ma1-a1-2-finance-f2-budget-commitment.md`（A1.2 done，报告 caveat ③ COMMITMENT 凭证是否影响试算平衡/三表，交本切片复核）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点被审功能现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1 五级矩阵 / §2 分级判据 / §3 完整枚举 / §4 Q1 真相源层级 / §5 Q4 修复义务 + 保护区域暂停协议 / §6 报告 9 段骨架 / §7 arm-index 衔接 / §8 过程纪律自检 / §9 真相源冻结 / §10 MR0/MR1 机制 / §去重协议）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.7 给出 UC 清单 = `UC-FIN-05/16/17`（3 UC），含 `use-cases.md:93` / `:318` / `:350` 锚点（inventory :341 确认一致）。

- **L1 需求契约（权威真相源）**：`docs/design/finance/use-cases.md`：
  - UC-FIN-05 多账套并行过账（`:93`）：同一业务单据审核 → 对每个启用的 AcctSchema 各生成一组凭证；每组凭证.acctSchemaId 不同、科目映射不同；所有组凭证.posted==true；GlBalance 按 acctSchemaId 隔离（各账套余额独立）。
  - UC-FIN-16 财务三大报表（`:318`）：三表基于 ErpFinGlBalance（按 subject×period×维度聚合）；资产负债表恒等式 资产合计==负债+所有者权益；利润表 收入-成本-费用=净利润，净利润结转至"未分配利润"；现金流量表按科目现金流分类（经营/投资/筹资）调整、间接法 净利润+非现金项目+营运资金变动；报表基于已 CLOSED 期间的 GlBalance（未结账期间数据不完整）。
  - UC-FIN-17 财务看板（`:350`）：KPI 卡片值 == 对应实体实时聚合（按期间/orgId/权限过滤）——收入/支出/净利润/银行余额/收支趋势/预算执行率/现金流预警；预警项 == 满足阈值条件的记录（阈值来自系统配置，非硬编码）；看板数据受行级权限约束（只看自己组织/部门/成本中心）。

- **L2 owner doc 设计参考**：`docs/design/finance/multiple-accounting-schemas.md`（§并行核算机制 `:60` / §科目映射规则 `:85` / §账套查询与报表 `:149` / §账套对账 `:171` / §数据隔离 `:243`）、`docs/design/dashboards.md`（§4 财务看板 `:105`，KPI 指标可追溯到实体；§实现约定 `:236`）、`docs/design/finance/posting.md`（§多套科目表，被 UC-FIN-05 引用）。**注意**：L2 为设计参考，与 L1 冲突时按 §4 Q1 以 L1 为准。

- **L3 代码实现现状（执行时实测核验）**——功能**已实现**（报表/看板/多账套传播均为已落地查询面）：
  - **报表**：`module-finance/erp-fin-service/.../report/ErpFinReportBizModel.java`（`renderHtml:92` / `download:107` 通用渲染 + 数据集查询 `balanceSheetData:238` / `incomeStatementData:244` / `cashFlowStatementData:250` / `arApAgingData:256` / `periodCloseReportData:263` + `buildBudgetVsActualDataset:230`）。三大报表数据来自 ErpFinGlBalance 聚合（执行时核验恒等式实现 + CLOSED 期间门控是否强制）。
  - **看板**：`module-finance/erp-fin-service/.../dashboard/ErpFinDashboardBizModel.java`（`getDashboardKpi:57` 返回 revenue/expense/netProfit/bankBalance/arBalance/apBalance + 收支趋势/预算执行率；执行时核验 KPI 是否硬编码 vs 实时聚合、预警阈值来源、行级权限过滤）。
  - **多账套传播**：`module-finance/erp-fin-service/.../posting/SchemaPropagator.java`（多 AcctSchema 凭证生成传播）+ 各 PostingDispatcher 经其展开；GlBalance 按 acctSchemaId 隔离（执行时核验隔离边界 + 每账套独立三表）。

- **L4 测试证据现状**：`TestErpFinReportRendering`（五张报表渲染）、`TestErpFinDashboard`（看板 KPI）、`TestErpFinMultiSchemaPosting`（多账套并行过账）、`TestErpFinMultiSchemaReportIsolation`（多账套报表隔离）、`TestErpFinReportRenderPerf`（渲染性能）。**执行时逐项核验断言强度**：UC-FIN-05 隔离（ acctSchemaId 余额独立）、UC-FIN-16 恒等式（资产==负债+权益）+ CLOSED 期间门控、UC-FIN-17 KPI 非硬编码 + 阈值配置化 + 行级权限。**历史信号**：`2026-07-06-use-case-implementation-audit.md:112` 曾标 UC-FIN-05 🔶（"无凭证/余额隔离测试"）——执行时核验 `TestErpFinMultiSchemaPosting`/`TestErpFinMultiSchemaReportIsolation` 是否已闭环该 🔶（HEAD 复核）。

- **L5 既有证据（MA2 复用输入，方法论 §去重协议）**：
  - 报表/看板/多账套为**查询/读面**，**无专属 MA2 状态机审计报告**（状态机类审计不覆盖纯查询面）。既有证据主要来自 `docs/audits/2026-07-06-use-case-implementation-audit.md`（§finance UC-FIN-05 🔶 / UC-FIN-16 ✅ / UC-FIN-17 ✅）。
  - **跨切片输入**：A1.2 报告（`docs/audits/2026-08-02-1700-rc-ma1-a1-2-finance-f2-budget.md:306`）caveat ③——COMMITMENT/BUDGET 影子凭证 header 借贷不平且不经 assertBalanced，**若通用试算平衡/三大报表对所有 postingType 求和不过滤 COMMITMENT 会破坏平衡恒等式**——交本切片 UC-FIN-16 复核（核对 balanceSheetData/incomeStatementData 是否过滤 BUDGET/COMMITMENT 影子凭证），并按需纳入静态存疑点清单供 MA4 A4.1 运行时确认。
  - **本切片须声明与上述既有证据的差异增量**（报告段落 9）：复用其已证实的渲染/数据源结论，只补"需求契约↔行为"差异（恒等式/隔离/CLOSED 门控/KPI 实时性/阈值配置化/行级权限）。

- **arm-index 既有 finding 衔接**：执行时 grep `arm-index.md` finance 报表/看板/多账套相关行后裁决复用 or 新建。已知相邻项：多公司隔离（`2026-07-28-1510-arm-ma2-multi-company-isolation.md`）可能触及 acctSchema 隔离维度（执行时核验去重）。本切片新发现的需求↔实现分歧须按 §7 grep 比对后裁决 `P*-RC-xxx`。

- **保护区域**：本审计为**只读审计**（读代码/测试/报告，不改代码/ORM/api.xml/真相源）。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按方法论 §10，P0 经 MR0 即时通道、P1 经 MR1（R1.0 展开 RC-R1.n）；触及会计过账逻辑（多账套凭证生成/科目映射）的修复行须 ask-first（§5 保护区域暂停协议）。

- **剩余差距**：A1.7 切片的五级追踪审计报告缺失 = MA4（A4.1 业财展开器，Deps=MA1 done）及 MR1（R1.0，Deps=MA1-MA4 done）的该切片证据缺口来源。本计划产出 A1.7 报告并登记 finding，解除其在 MA4/MR1 链路的该切片证据缺口。

## Goals

- 产出 A1.7 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-7-finance-f7-reports-dashboards-multischema.md`，含方法论 §6 **9 段全部内容**：①UC-FIN-05/16/17 需求契约原文（逐字引用，不转述）②实现证据（`file:line`，含 SchemaPropagator 多账套传播链 + ReportBizModel 数据集查询 + DashboardBizModel KPI 聚合链）③测试证据（注明断言强度）④运行时行为证据（复用既有证据，补差异）⑤五级追踪矩阵 + 每 UC 符合性结论（P0/P1/P2/接受）⑥与 arm-index 衔接（复用 or 新增 裁决）⑦静态存疑点清单（供 MA4 展开）⑧过程纪律自检段 ⑨与既有证据差异增量声明。
- 对 3 UC 逐条核验**每条验收标准**（完整枚举，§3）：UC-FIN-05（4 断言：每 AcctSchema 一组凭证/acctSchemaId 隔离/科目映射不同/全部 posted）+ UC-FIN-16（4 断言：GlBalance 数据源/资产负债恒等式/利润表结转/现金流量分类 + CLOSED 期间门控）+ UC-FIN-17（3 断言：KPI 实时聚合非硬编码/预警阈值配置化/行级权限约束），各一矩阵行。
- 对候选缺口/偏离给出分级结论：UC-FIN-05 隔离是否真正落地（历史 🔶 HEAD 复核）、UC-FIN-16 三表是否过滤 BUDGET/COMMITMENT 影子凭证（A1.2 caveat ③ 交叉）、UC-FIN-16 CLOSED 期间门控是否强制、UC-FIN-17 KPI 是否硬编码、预警阈值是否配置化、行级权限是否约束看板数据——按 §2 判据定级，若为 P0/P1 则新建 `P0-RC-xxx`/`P1-RC-xxx` 并按 §10 触发 MR0/MR1（本计划仅登记，不实施修复）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区）。

## Non-Goals

- **不修复 finding**（修复属 MR0 即时通道 / MR1 R1.0 展开的 RC-R1.n；本计划是审计，结果表面 = 一份报告 + arm-index 登记）。
- **不修改真相源**（product-scope / finance use-cases / multiple-accounting-schemas.md / dashboards.md 需求契约段落；§9 冻结条款——分歧记入报告，不直改真相源）。
- **不修改代码/ORM/api.xml/BizModel/view.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.1-A1.6 done/draft；A1.8-A1.51 各自独立 plan；A1.7 只覆盖 UC-FIN-05/16/17）。
- **不重跑既有状态机行为审计**（§去重协议：报表/看板/多账套为查询面，无专属 MA2 状态机报告；复用 2026-07-06 use-case audit + A1.2 caveat ③，只补需求视角差异；不重审架构/代码质量维度）。

## Task Route

- Type: `verification or audit work`（需求→实现符合性五级追踪审计；非实现变更、非需求澄清）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（审计契约 §1-§10 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.7 工作项 + Work Item Details MA1）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.7 UC 锚点）+ `docs/design/finance/use-cases.md`（L1 真相源）+ `docs/design/finance/multiple-accounting-schemas.md` + `docs/design/dashboards.md` + `docs/design/finance/posting.md`（L2 设计参考，非真相源）+ `docs/audits/arm-index.md`（finding 衔接）+ `docs/audits/2026-07-06-use-case-implementation-audit.md`（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。该技能定义多维审计 prompt 范式，本切片需求↔实现符合性审计复用其维度框架；其必需输入（owner doc + use-cases + 代码路径 + 测试）均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。**L5 行为证据**默认复用既有 use-case audit + E2E recordings（方法论 §去重协议），无需起服务；若需对存疑点做即时行为确认，可跑既有 JUnit（`mvn test -pl module-finance/erp-fin-service -Dtest=TestErpFinReportRendering,TestErpFinDashboard,TestErpFinMultiSchemaPosting,TestErpFinMultiSchemaReportIsolation`），不引入新依赖。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更故无回归风险，仅记录 actual vs baseline）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论 + 历史 🔶/caveat HEAD 复核

Status: completed
Targets: `docs/audits/2026-08-02-2115-rc-ma1-a1-7-finance-f7-reports-dashboards-multischema.md`（新建，先填 §1-§5；命名遵循方法论 §归档规范）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done（方法论契约 + UC 锚点就绪）

- [x] `Proof` 对 UC-FIN-05/16/17 **逐 UC 一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:93/:318/:350` 验收标准原文（禁止转述）；L2 引用 `multiple-accounting-schemas.md`（§并行核算/§数据隔离）/`dashboards.md`（§4 财务看板）/`posting.md`（§多套科目表）对应 section（标注"设计参考，冲突以 L1 为准"）；L3 引用 `module-finance/erp-fin-service/.../report/ErpFinReportBizModel.java:line` + `.../dashboard/ErpFinDashboardBizModel.java:line` + `.../posting/SchemaPropagator.java:line`（调用链列全）；L4 引用 `Test*.java#method`（注明断言强度）；L5 复用 2026-07-06 use-case audit + 本切片差异。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 重点核验**候选缺口/偏离**（逐条验收标准对照）：UC-FIN-05——①每 AcctSchema 一组凭证（SchemaPropagator 展开）；②acctSchemaId 隔离（GlBalance 独立）；③科目映射不同；④全部 posted；UC-FIN-16——⑤三表基于 GlBalance；⑥资产负债恒等式（资产==负债+权益）；⑦利润表结转未分配利润；⑧现金流量分类 + CLOSED 期间门控是否强制；**⑨三表是否过滤 BUDGET/COMMITMENT 影子凭证（A1.2 caveat ③ 交叉复核）**；UC-FIN-17——⑩KPI 实时聚合非硬编码；⑪预警阈值配置化非硬编码；⑫行级权限约束看板数据。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` **历史 🔶/caveat HEAD 复核**：① UC-FIN-05 🔶（2026-07-06 审计标"无凭证/余额隔离测试"）→ 核验 `TestErpFinMultiSchemaPosting`/`TestErpFinMultiSchemaReportIsolation` 在当前 HEAD 是否已闭环（记录"已闭环/仍 open"）；② A1.2 caveat ③ COMMITMENT 影子凭证对试算平衡/三表影响 → 核验 ReportBizModel 数据集查询是否过滤 postingType，给出结论。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` 按 §2 判据对每 UC 给出符合性结论（P0/P1/P2/接受）：UC-FIN-05 隔离若未落地（P1 候选，会计正确性）；UC-FIN-16 恒等式若被影子凭证破坏（P0/P1 候选，报表正确性）；CLOSED 期间门控若未强制（P2 候选）；UC-FIN-17 KPI 硬编码/阈值硬编码/无行级权限（按 §2 定级）。每结论须列明命中判据编号 + 三源对照。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] 报告 §1-§5 已落盘：UC-FIN-05/16/17 各一矩阵行（验收标准全覆盖），L1 逐字引用、L3 含行号、L4 注明断言强度、L5 标注复用来源
- [x] 每 UC 有符合性结论（P0/P1/P2/接受）+ §2 判据编号；候选缺口 ①-⑫ 有明确分级（非悬空"待查"）；历史 🔶/caveat HEAD 复核结论已记录

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: completed
Targets: `docs/audits/2026-08-02-2115-rc-ma1-a1-7-finance-f7-reports-dashboards-multischema.md`（补 §6-§9，报告定稿）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成（矩阵 + 结论已出）

- [x] `Decision` **复用 or 新增 裁决**（§7）：产出 finding 前 grep `arm-index.md` finance 报表/看板/多账套/多公司隔离同域同控制点行后裁决——同根因同控制点 → 复用既有 ID（追加 RC 交叉引用注记，不新建）；新根因/新功能点 → 新建 `P0-RC-xxx`/`P1-RC-xxx` 并列明与既有 finding 的差异依据。**特别注意**：多公司隔离报告（`2026-07-28-1510-arm-ma2-multi-company-isolation.md`）若已覆盖 acctSchema 隔离维度则复用并追加 RC 交叉引用。禁止未经比对直接新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 的复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR0/MR1）。
      - Skill: none
- [x] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记本切片 L5 无法静态定论、需运行时确认的点（如三表运行时恒等式、多账套并发隔离、看板行级权限运行时过滤；每存疑点一行；无则注明"无"）。**P0 即时通道**：若 Phase 1 定级出 P0，按 §10 在报告登记并在本计划记录"已触发 MR0 追加 R0.n 实体行"（本计划不实施修复）。
      - Skill: none
- [x] `Proof` 报告 §8 过程纪律自检段（§8 模板）：实际运行 `bash docs/audits/nop-compliance-checker.sh` 并附 actual vs baseline 汇总表（本审计无生产代码变更，注明"无回归风险"）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 脚本退出码 0 作为门控通过依据**（区分 reporter vs CI 门控）。
      - Skill: none
- [x] `Add` 报告 §9 与既有证据差异增量声明：声明复用 `2026-07-06-use-case-implementation-audit.md`（UC-FIN-05/16/17）+ A1.2 caveat ③ 已证实结论，列明本切片只补的需求视角差异（隔离 HEAD 复核 / 恒等式影子凭证影响 / CLOSED 门控 / KPI 实时性 / 阈值配置化 / 行级权限）。
      - Skill: none
- [x] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 `P*-RC-xxx` 入对应分区（MA1 finding 区），既有行追加 RC 交叉引用注记。
      - Skill: none
- [x] `Proof` 报告 9 段完整性自检（§6 段落完整性自检）：落盘前自查 §1-§9 全部存在；缺任一段即回到 Phase 补齐。
      - Skill: none

Exit Criteria:

- [x] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据（无未经比对新建）
- [x] 新 RC finding 已写入 `arm-index.md` 对应分区；静态存疑点清单已登记（供 A4.1 展开）
- [x] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is`（独立子代理 `ses_03d4f4431ffetnKIk56hh8ieiy`，fresh session，未起草本计划）。逐项实测核验全 PASS：格式/命名、roadmap 对齐（A1.7 / UC-FIN-05/16/17 / Deps=0.2 done / Skill 匹配 roadmap:46）、UC 锚点 :93/:318/:350 匹配 baseline-inventory、L3 代码路径与方法行号实测命中（renderHtml:92/download:107/balanceSheetData:238/incomeStatementData:244/cashFlowStatementData:250/arApAgingData:256/periodCloseReportData:263/buildBudgetVsActualDataset:230/getDashboardKpi:57）、L4 测试全集存在、L2 owner doc 存在、L5 2026-07-06 audit UC-FIN-05 🔶 实测命中、UC 完整枚举（UC-FIN-05×4 + UC-FIN-16×5 + UC-FIN-17×3）、item typing/skill/anti-slack/Closure-Gate 只读删门控有据/§去重/保护区域 全合规。无阻塞 issue。Non-blocking（已吸收）：①A1.2 audit 报告路径 typo（缺 `-f2-` 段）→**已修正**为 `...-a1-2-finance-f2-budget.md`；②Non-Goals"A1.1-A1.6 done"措辞（A1.4 roadmap 仍标 todo）→**已修正**为"done/draft"；③getDashboardKpi 行号 `:7-`→**已修正**为 `:57`。共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控——审计报告产出不触发编译或测试。验证 = 报告 9 段完整性 + 五级矩阵逐 UC 覆盖 + finding arm-index 衔接 + 历史 🔶/caveat HEAD 复核 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A1.7 报告 9 段齐全 + UC-FIN-05/16/17 逐矩阵行 + 历史 🔶/caveat HEAD 复核 + finding 登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.7 锚点一致
- [x] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（本计划无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按方法论 §10 经 MR0（P0 即时通道）/ MR1（R1.0 展开 RC-R1.n，P1 批量）实施；触及会计过账逻辑（多账套凭证生成/科目映射/三表聚合过滤）的修复行须 ask-first + 独立 plan-audit（§5 保护区域暂停协议）。本审计闭环不阻塞于修复落地。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行）

## Closure

Status Note: 本计划为只读需求-实现符合性审计（verification or audit work），结果表面 = 一份 9 段审计报告 + arm-index finding 登记。两个 Phase 均 completed，全部执行项目与 Exit Criteria 已勾选 [x]，Closure Gates 8/8 全勾。报告产出 `docs/audits/2026-08-02-2115-rc-ma1-a1-7-finance-f7-reports-dashboards-multischema.md`（9 段齐全）+ `docs/audits/arm-index.md` 新增 A1.7 清单行 + P1-RC-007/P2-RC-008 finding 行 + A1.7 summary 注记（含 P1-MA2-093 复用 + A1.2 caveat ③ 收口 + UC-FIN-05 🔶 闭环 + P1-MA2-095 HEAD fix 复核）。无 P0 → 不触发 MR0；P1-RC-007 由 MR1 R1.0 展开（触及 ORM 结构变更须 ask-first），P2-RC-008 successor watch-only，4 项静态存疑点交 MA4 A4.1 运行时展开。finding 的修复实施归类为 out-of-scope（本审计不实施修复），不阻塞本审计闭环。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（closure auditor，fresh session，未参与本计划起草与执行，session 独立于 EXECUTE 上下文）
- Evidence:
  - **结构完整性**：`node .../plan-check.mjs ... --strict` 复跑 PASS（22 项全勾选，含本轮补勾的 2 项独立审计门控 + 新增 `## Closure` 段）。Front matter `Plan Status: completed` / `Last Reviewed: 2026-08-02` 一致；2 Phase 均 `Status: completed`，Phase 1/2 Exit Criteria 各 2/3 项全 `[x]`；Closure Gates 8/8 全 `[x]`。
  - **结果表面落地**：报告 `docs/audits/2026-08-02-2115-rc-ma1-a1-7-finance-f7-reports-dashboards-multischema.md`（37187 字节，9 段齐全 §1-§9 + 9 段完整性自检 + finding 摘要附录）实测存在；arm-index（`docs/audits/arm-index.md:81`）已登记 A1.7 报告清单行（finance / 报表/看板/多账套 / done）+ P1-RC-007（`:102`）+ P2-RC-008（`:103`）+ A1.7 summary 注记（`:114`）。
  - **finding 真实性 LIVE 复核**（HEAD `8659e9f04`，报告 L3/L4 核验 HEAD `c1b775491` 同日同段）：① P1-RC-007 — `ErpFinReportBizModel.java:314` 实测 `r.put("section", "OPERATING")` 硬编码全部为 OPERATING（投资/筹资分类 + 间接法缺失确认），与报告 §5.2/§5.3 + arm-index `:102` 一致；② P2-RC-008 — `ErpFinReportBizModel.java:386-413` `loadGlBalances` 实测仅 `eq("periodId", ...)`，无 `period.status==CLOSED` 守卫，与报告 §5.2 + arm-index `:103` 一致；③ P1-MA2-093 复用裁决 — 同根因同控制点（A2.18 orgId 查询隔离全仓未落地），按 §去重协议追加 RC 交叉引用不新建，裁决合规。
  - **caveat ③ 收口**：A1.2 caveat ③（COMMITMENT 影子凭证是否破坏试算平衡/三表恒等式）→ BS/IS 安全（`orm.xml:1740-1742` 注记 BUDGET/COMMITMENT 不入 GlBalance）；cash flow 读 VoucherLine 不过滤 postingType 但实操不触现金科目 → 登记为 §7 SP-1 供 MA4 运行时确认。
  - **过程纪律**：报告 §8 含 `nop-compliance-checker.sh` actual vs baseline 实测表（R12c actual=40==baseline=40，零裸漂移）；本审计为只读（零代码/ORM/api.xml/view.xml/真相源变更），Closure Gates 显式删除 build/test/lint/typecheck 门控合规（adjudicated gate set）；独立草案审查 1 轮 acceptable-as-is 已记录（`ses_03d4f4431ffetnKIk56hh8ieiy`）。
  - **日志同步**：`docs/logs/2026/08-02.md:5-11` 含 A1.7 EXECUTE 条目（Phase 1/2 全完成 + 产出文件 + bookkeeping[plan Status active→completed + 2 Phase planned→completed + 全 items/Exit Criteria 勾选 + Closure Gates 6/8 勾选 + roadmap MA1 表 A1.7 todo→done] + 下一步 successor）。
  - **文本一致性五点**：Plan Status completed ↔ 2 Phase Status completed ↔ Phase Exit Criteria 全 [x] ↔ Closure Gates 8/8 [x] ↔ Closure evidence 齐全 ↔ docs/logs 条目一致。
  - **Anti-Hollow**：本计划无生产代码变更（只读审计），无空函数/return null/吞异常/未接线组件风险维度；结果表面（报告 + arm-index 登记）均实际落盘可读，非占位符。

Follow-up:

- P1-RC-007 → MR1 R1.0 展开 RC-R1.n 修复行（触及 `ErpMdSubject` ORM 结构变更[cashFlowType 字段] 须 ask-first + 独立 plan-audit，§5 ORM 结构变更类）。
- P2-RC-008 → successor watch-only（修复为 BizModel 代码逻辑，按 roadmap 预授权可自动执行；或 owner doc 显式标注 OPEN 期间数据不完整警告，纯文档可自动执行）。
- 4 项静态存疑点（SP-1 cash flow VoucherLine postingType 过滤运行时 / SP-2 多账套每账套独立三表渲染 / SP-3 CLOSED 门控缺失数据完整性 / SP-4 看板行级权限跨组织泄漏）→ MA4 A4.1 运行时展开。
