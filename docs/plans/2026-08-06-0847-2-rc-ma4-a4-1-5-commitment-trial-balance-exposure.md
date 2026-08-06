# 2026-08-06-0847-2 rc-ma4-a4-1-5-commitment-trial-balance-exposure 承付凭证借贷不平时报平衡暴露核对

> Plan Status: completed
> Last Reviewed: 2026-08-06
> Mission: requirement-compliance
> Work Item: A4.1.5（MA4 运行时行为验证 — A1.2 §7-2：承付凭证 header 借贷不平时报暴露，试算平衡/三大报表是否过滤 COMMITMENT 影子凭证）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A4.1.5；存疑点来源 `docs/audits/2026-08-02-1700-rc-ma1-a1-2-finance-f2-budget.md` §7 存疑点 2
> Related: `docs/plans/2026-08-07-0300-3-rc-ma4-a4-1-finance-runtime-expander.md`（A4.1 展开器 done，本行即其展开的实体行）、`docs/plans/2026-08-02-1600-2-rc-ma1-a1-2-finance-f2-budget-commitment.md`（A1.2 done，§5.2 caveat ③ 接受 + §7 存疑点 2）、`docs/plans/2026-08-07-0330-1-rc-ma4-a4-1-1-posted-false-listener-coverage.md`（A4.1.1 done，§7 协同：影子凭证 posted 标记覆盖）
> Audit: required

## Current Baseline

> 本计划是**运行时行为验证**（verification or audit work），结果表面 = 一份 A4.1.5 验证报告（落盘 `docs/audits/2026-08-06-0847-rc-ma4-a4-1-5-commitment-trial-balance-exposure.md`）+ 必要时 arm-index finding 登记。**不改代码/ORM/api.xml/真相源**（只读核对：grep 试算平衡/三大报表/各 GL 路径的 postingType 过滤 + 读既有 JUnit + 复用 MA2/A1.7）。

- **存疑点原文**（A1.2 报告 §7 存疑点 2，`2026-08-02-1700-...-a1-2-budget.md:306`）：「caveat ③ 承付凭证 header 借贷不平（totalDebit≠totalCredit）是否被通用试算平衡报表暴露」——L3 静态确认 COMMITMENT 凭证经专用 Generator 直接写入（不经 ErpFinAcctDocRegistry 路由 / 不经 assertBalanced），过账引擎 balance 校验不触及。但通用试算平衡 / 财务三大报表（UC-FIN-16，A1.7 切片）若对所有 postingType 求和不过滤 COMMITMENT，会破坏平衡恒等式——交 MA4 A4.1 运行时确认（核对试算平衡报表是否过滤 BUDGET/COMMITMENT 影子凭证）。当前归 MA3 doc↔code 维度（L2 `budget.md:255` 两行 vs 实现单行）+ MA4 运行时确认，非 RC P1（L1 未规定凭证平衡）。

- **承付凭证结构（L3 实测）**：`module-finance/erp-fin-service/.../service/budget/CommitmentVoucherGenerator.java` `writeCommitmentVoucher:119-182` 实际写**单行单边凭证**——DEBIT 科目：debit=absAmount/credit=0（:141-142），凭证头 totalDebit=absAmount/totalCredit=0（:143-144），**仅一行** debitAmount=absAmount/creditAmount=0（:161-162）。即 header 借贷**不平**（Dr without Cr）。**非** L2 `budget.md:255` 描述的"Dr 预算占用 / Cr 应付-承付"两行平衡结构（L2 drift 归 MA3）。COMMITMENT 凭证经专用 Generator 直接写入，**不经** `ErpFinPostingProcessor.assertBalanced:736`（过账引擎 balance 校验），故过账引擎不拦截不平的承付凭证。

- **初步实测（本计划起草时已完成的关键普查，执行时复核）**：
  - **试算平衡快照**：`module-finance/erp-fin-service/.../service/processor/ErpFinAccountingPeriodProcessor.java` `findPostedVoucherIds:385-394` 过滤条件 = `docStatus=POSTED` + `isReversed=false` + `or(isNull(postingType), ne(postingType, BUDGET))`（:392）。**仅排除 BUDGET，未排除 COMMITMENT**——COMMITMENT 凭证若存在（config 开启时）会进入试算平衡快照，其单边 Dr 行破坏 Dr==Cr 平衡恒等式。
  - **年度结转**：`module-finance/erp-fin-service/.../service/annualclose/AnnualCloseService.java:339` = `or(isNull, ne(BUDGET))`——**仅排除 BUDGET**。
  - **损益结转**：`module-finance/erp-fin-service/.../service/profitloss/ProfitLossClosingService.java:192` = `or(isNull, ne(BUDGET))`——**仅排除 BUDGET**。
  - **坏账准备**：`module-finance/erp-fin-service/.../service/baddebt/BadDebtProvisionService.java:310` = `or(isNull, ne(BUDGET))`——**仅排除 BUDGET**。
  - **汇兑重估**：`module-finance/erp-fin-service/.../service/fx/ExchangeRevaluationService.java:227` = `or(isNull, ne(BUDGET))`——**仅排除 BUDGET**。
  - **预算对比报表**（P1-RC-003，A1.2 §5.3）：`ErpFinBudgetLineBizModel:65` = `or(isNull, ne(BUDGET))`——COMMITMENT 计入 actual（已为已知 P1）。
  - **对照：控制引擎（已正确）**：`ErpFinBudgetControlBiz.java:164` actual 通道 = `notIn(BUDGET, COMMITMENT)`——**正确排除两者**（P1-MA2-084 fix）。
  - **对照：carryForward（已正确）**：`ErpFinBudgetScenarioCarryForwardProcessor.java:222` 排除 BUDGET + COMMITMENT 两者。

- **关联既有结论**：
  - A1.2 §5.2 caveat ③ = **接受**（L1 层面，L1 未规定凭证平衡）；L2 drift（`budget.md:255` 两行 vs 实现单行）归 MA3。
  - A1.7 切片（UC-FIN-16 财务三大报表）已审计（done），其报告 §7 SP-1（= A4.1.22）记录「cash flow 读 VoucherLine 不过滤 postingType，BUDGET/COMMITMENT 影子凭证现金科目污染」——本验证须与之交叉去重（A1.7 已静态证 BS/IS 安全，CF 待运行时确认）。
  - P1-RC-003（A1.2 §5.3）= 预算对比报表口径错误（COMMITMENT 计入 actual），与本存疑点**不同控制点**（报表列数/口径 vs 试算平衡/三大报表过滤），各自独立 finding 但同根因（BUDGET-only 过滤漏 COMMITMENT）。

- **需求契约（L1 权威）**：`docs/design/finance/use-cases.md:257-258` UC-FIN-13 断言③——仅要求「采购订单.APPROVED → 生成 COMMITMENT 凭证」「订单 CANCELLED 或发票接收 → 红冲 COMMITMENT」——**未规定 Dr/Cr 结构或凭证平衡**。L1 未要求试算平衡/三大报表过滤影子凭证（这是实现侧完整性，非需求验收标准）。

- **剩余差距**：试算平衡快照 + 年度结转 + 损益结转 + 坏账准备 + 汇兑重估 5 个 GL 路径**静态已确认仅排除 BUDGET 未排除 COMMITMENT**，但**运行时实际影响**（config 开启时承付凭证是否真的进入这些路径并破坏平衡恒等式）须运行时确认 + finding 定级。三大报表（BS/IS/CF）的凭证行过滤须经 A1.7 报告交叉确认。

- **保护区域**：只读核对（grep + 读 JUnit），不触及 ORM/会计过账逻辑/数据删除。属 roadmap 预授权类目。本验证**不实施修复**——若确认平衡破坏则登记 finding 交 MR1（过滤逻辑修复属代码逻辑修复预授权类目；但触及损益结转/试算平衡核心路径，若裁决触及"会计过账核心路径"则须 ask-first）。

## Goals

- 全集枚举所有读取/聚合 VoucherLine 且按 postingType 过滤的 GL 路径（试算平衡快照 / 年度结转 / 损益结转 / 坏账准备 / 汇兑重估 / 预算对比报表 / 三大报表 BS/IS/CF / GL 余额视图 / AR-AP 辅助账 等），逐路径核验：是否排除 COMMITMENT 影子凭证（与排除 BUDGET 对称）。
- 对每路径给出运行时证据：文件:行 + 过滤条件（排除 BUDGET only / 排除 BUDGET+COMMITMENT / 不过滤）+ 既有 JUnit 承付场景覆盖（有/无）。
- 裁决平衡恒等式破坏风险：若存在路径在 config 开启（`budget-commitment-enabled=true`）+ 承付凭证存在时纳入单边 Dr 行 → 破坏 Dr==Cr 平衡 → 按方法论 §2 定级（P1④跨组件契约不一致 / P2①边界场景弱）。若全部路径安全 → 维持接受。
- 与 A1.7 §7 SP-1（A4.1.22）交叉去重声明（CF 不过滤 postingType = 不同存疑点但同根因家族）。
- 产出验证报告 + §8 过程纪律自检；finding（若有新控制点）按 §7 裁决登记 arm-index。

## Non-Goals

- **不修改代码/ORM/api.xml/BizModel**（只读核对）。
- **不重新核实承付凭证结构本身**（A1.2 §5.2 caveat ③ 已确认单行单边；L2 drift 归 MA3）。
- **不实施修复**（修复经 MR1；触及试算平衡/损益结转核心路径须 ask-first 评估）。
- **不重复 A4.1.22（A1.7 SP-1 CF 污染）的运行时验证**（交叉引用，不同存疑点）。
- **不修改真相源**（§9 冻结）。

## Task Route

- Type: `verification or audit work`（GL 路径过滤面普查 + 平衡恒等式风险评估）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 判据 + §7 衔接 + §8 自检 + §9 冻结）+ `docs/backlog/requirement-compliance-roadmap.md`（A4.1.5 行）+ `docs/audits/2026-08-02-1700-rc-ma1-a1-2-finance-f2-budget.md` §7 存疑点 2 + §5.2 caveat ③（输入）+ `docs/design/finance/budget.md §承付会计` + `docs/audits/2026-08-02-2115-...-a1-7-...md` §7 SP-1（A1.7 CF 存疑点交叉）。
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定）。GL 路径过滤面普查需多维度归类（路径清单 / 过滤条件 / 平衡恒等式影响 / 测试覆盖 / P0 平衡破坏评估）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。只读核对（grep + 读 JUnit + 引用 MA2/A1.2/A1.7）。§8 自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - GL 路径 postingType 过滤面全集普查 + 平衡恒等式影响评估

Status: completed
Targets: `docs/audits/2026-08-06-0847-rc-ma4-a4-1-5-commitment-trial-balance-exposure.md`（验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: A4.1 done；A1.2 done（§5.2 caveat ③ 接受 + §7 存疑点 2 已落盘）；A1.7 done（§7 SP-1 交叉）

- [x] `Proof` GL 路径全集枚举：grep 所有读取/聚合 `ErpFinVoucherLine` 或 `ErpFinVoucher` 且含 `postingType` 过滤表达式的 Java 文件（关键词 `ne("postingType"` / `notIn("postingType"` / `isNull("postingType"` / `eq("postingType"`），产出路径清单（禁止抽样）。须覆盖试算平衡快照 / 年度结转 / 损益结转 / 坏账准备 / 汇兑重估 / 预算对比报表 / 三大报表(BS/IS/CF) / GL 余额视图 / AR-AP 辅助账 / carryForward 等全部消费点。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 逐路径过滤条件核验：每路径记录 ①文件:行 ②过滤条件分类（排除 BUDGET only / 排除 BUDGET+COMMITMENT / 排除其他 / 不过滤）③该路径是否参与 Dr==Cr 平衡聚合（试算平衡/三大报表=是；预算对比/控制=否）④config 开启承付时 COMMITMENT 单边 Dr 行是否进入该路径 ⑤既有 JUnit 承付场景覆盖（引用 + 断言强度）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` 平衡恒等式破坏风险评估（方法论 §2 判据）：①试算平衡快照路径——config 开启承付 + PO approve 产生单边 Dr COMMITMENT 凭证 → 快照 closingDebit/closingCredit 求和失衡（Dr > Cr）→ **是否破坏平衡恒等式**（实测 + 既有 JUnit `TestErpFinProfitLossClosing` / `TestErpFinPeriodClosePerf` 是否在承付开启下运行）②三大报表路径——BS/IS/CF 是否独立过滤（A1.7 已证 BS/IS 安全；CF 归 A4.1.22）③若任一平衡聚合路径纳入单边 Dr 承付行 → 按 §2 定级（P1④跨组件契约不一致 / P2①边界场景弱）；若全部平衡聚合路径安全（config 默认关闭 + 平衡路径独立排除或承付不进）→ 维持接受。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] GL 路径过滤矩阵落盘（每路径 ≥5 字段齐备），无遗漏
- [x] 平衡恒等式破坏风险评估有明确裁决（维持接受 / 升级 finding）+ 路径证据 + config 默认关闭的运行时影响说明

### Phase 2 - finding 衔接 + §8 自检 + 报告定稿

Status: completed
Targets: `docs/audits/2026-08-06-0847-rc-ma4-a4-1-5-commitment-trial-balance-exposure.md`（定稿）；`docs/audits/arm-index.md`（若新 finding）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 路径矩阵 + 风险裁决完成

- [x] `Add` 若裁决升级 → 按 §7 grep arm-index finance 试算平衡/三大报表/影子凭证过滤同域同控制点裁决「复用 or 新建」`P*-RC-xxx`。须与 A1.7 §7 SP-1（A4.1.22 CF 污染）+ P1-RC-003（预算对比报表口径）交叉去重——同根因家族（BUDGET-only 过滤漏 COMMITMENT）但不同控制点（试算平衡/三大报表 vs CF vs 预算对比报表），各自独立 finding。finding → MR1 双向可追溯，标注触及损益结转/试算平衡核心路径的 ask-first 评估。
      - Skill: none
- [x] `Proof` §8 过程纪律自检：运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明"无回归风险"）；closure-audit 独立性声明；与 arm-index 交叉去重声明（与 A1.2 §5.2 caveat ③ / A1.7 §7 SP-1 / P1-RC-003 / P1-MA2-084 的复用关系）。不以 checker 退出码 0 作为门控依据。
      - Skill: none

Exit Criteria:

- [x] 验证报告定稿（路径矩阵 + 风险裁决 + finding 登记[若有] + §8 自检齐全 + 与 A1.7/A4.1.22 交叉去重声明）
- [x] 新 finding（若有）已写入 arm-index MA4 分区并有 grep 依据

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_02b730a4bffeRSEZd7InxRra0B，fresh session，未起草本计划）。逐项核验全 PASS：核心 anti-hollow claim **逐字实测命中**——试算平衡过滤 `ErpFinAccountingPeriodProcessor.java:392` = `or(isNull("postingType"), ne("postingType", POSTING_TYPE_BUDGET))`（仅排 BUDGET 未排 COMMITMENT，plan:20 准确）；承付凭证单行单边 `CommitmentVoucherGenerator.java:141-162`（header totalDebit≠totalCredit，单边 Dr）；5 GL 路径 BUDGET-only 实测（AnnualCloseService:339 / ProfitLossClosingService:192 / BadDebtProvisionService:310 / ExchangeRevaluationService:227 / ErpFinBudgetLineBizModel:65）；控制引擎对照 `ErpFinBudgetControlBiz.java:162-164` notIn(BUDGET,COMMITMENT) 正确排两者；carryForward `ErpFinBudgetScenarioCarryForwardProcessor:221-222` 排两者。Deps（A4.1/A1.2/A1.7 均 done 实测）。L1 use-cases.md:256-258 UC-FIN-13 断言③ 仅要求生成+红冲不要求凭证平衡 → "L1 层面接受" framing 成立；断言④ 三列报表 = P1-RC-003 独立控制点正确去重不混淆。只读审计正确（Goals/Non-Goals 无代码变更）；保护区域诚实（Deferred 标注触及损益结转/试算平衡核心路径须 ask-first 评估）；Closure Gates audit-only 删 build/test/lint 有据；§7 去重 + §9 冻结 + §8 checker=纯 reporter 声明齐。两条非阻塞注记（① carryForward 子包路径 service/budget vs service/processor 漂移——glob 可导航，行内容准确；② 控制引擎 :164 vs notIn:163 行号漂移——§1 锚点纪律"行号漂移非阻塞"）。共识达成，转 active。

## Closure Gates

> 本计划为**只读运行时核对**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 路径矩阵完整性 + 风险裁决 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A4.1.5 验证报告路径矩阵齐全（全集）+ 风险裁决 + finding（若有）登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §MA4 + §2 判据 + §9 冻结一致；与 A1.2 §7-2 + §5.2 caveat ③ + A1.7 §7 SP-1 一致
- [x] 已运行验证：路径矩阵完整性 + §8 checker actual vs baseline 实测记录（本计划无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 承付凭证结构 L2 drift（budget.md:255 两行 vs 实现单行）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: L2 owner doc `budget.md:255` 描述两行平衡 Dr/Cr 结构 vs 实现单行单边，属 owner-doc↔code 文本一致性维度，归 audit-remediation MA3（本 RC mission 不重做文本一致性，§去重协议）。本验证从需求契约视角（L1 未规定凭证平衡）维持接受。
- Successor Required: no（归 MA3，非 RC 范围）

### 过滤逻辑修复（若裁决升级为平衡破坏）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是运行时核对，结果表面 = 验证报告 + 风险裁决 + finding 登记。过滤逻辑修复（试算平衡/损益结转等路径补排除 COMMITMENT）经 MR1（R1.0→RC-R1.n）实施；触及损益结转/试算平衡核心路径的修复须 ask-first 评估是否属"会计过账核心路径"保护区域。本验证闭环不阻塞于修复落地。
- Successor Required: yes（MR1 按本报告 finding 交叉引用展开修复行；若维持接受则无 successor）

## Closure

Status Note: 两 Phase 全 done（路径矩阵全集 14 消费点 + 平衡恒等式风险裁决升级 P1-RC-091 + arm-index 登记 + §8 checker 实测 0 漂移）。6 项客观 closure gate [x]（范围完成/文档对齐/checker 实测/无降级/草案审查记录/文本一致性）。裁决升级为 P1-RC-091（试算平衡快照 + 4 GL 重分类/重估服务 BUDGET-only 过滤漏 COMMITMENT），经 MR1（R1.0→RC-R1.n），修复触及损益结转/试算平衡核心路径须 ask-first 评估。caveat ③ 凭证结构维持接受。独立结束审计由独立子代理（新会话）执行（gate 122/123 + Closure Audit Evidence 由其回填，执行者不自我审计）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（closure-verify 新会话，非执行者，未重用执行者上下文）。
- Audit Type: 结构检查修复 + 语义复核（mission driver SCRIPT_CHECK_RESULT=FAIL 路径：修复 2 项未勾选结束门控 + 验证证据落地）。
- Evidence:
  - 验证报告落盘 `docs/audits/2026-08-06-0847-rc-ma4-a4-1-5-commitment-trial-balance-exposure.md`（实测存在，32772 字节，10 节齐全：§0 TL;DR / §1 L1 契约 / §2 L3 路径矩阵[14 消费点全集普查] / §3 L4 测试 / §4 L5 运行时 / §5 裁决矩阵 / §6 arm-index 衔接 / §7 存疑点 / §8 过程纪律自检 / §9 差异增量 / §10 Verdict）。
  - 新 finding P1-RC-091 实测登记入 `docs/audits/arm-index.md:293`（finance 域 RC 发现追踪分区，5 GL 聚合路径 BUDGET-only 过滤漏 COMMITMENT）+ `:393` RC 交叉引用注记（首个 MA4 RC 运行时核对 finding）。
  - Anti-hollow 复核：§2.1 全集普查完整性自检声明 grep `postingType` 全 finance 主代码命中 = #1-#11（#12-14 经 GlBalance/ArApItem 间接消费），非空话填充；§8 checker actual vs baseline 实测表落盘，诚实区分「checker 脚本纯 reporter 退出码 0」vs「CI workflow 真正门控」，未以退出码 0 冒充门控通过。
  - 两 Phase 退出标准 [x] 与门控一致性：Phase 1（路径矩阵 + 平衡恒等式风险评估）+ Phase 2（finding 衔接 + §8 自检 + 报告定稿）全 [x]，与 Plan Status: completed 一致。
  - 保护区域诚实：Deferred 区记录「过滤逻辑修复经 MR1，触及损益结转/试算平衡核心路径须 ask-first 评估」——未将保护区域风险隐藏为已完成。
  - 脚本检查 `node tools/mission-driver/src/plan-check.mjs ... --strict` 经本次修复（门控 122/123 勾选 [x] + Closure 占位符替换为真实证据）后转 PASS。

Follow-up:

- MR1 R1.0 展开为 RC-R1.n：P1-RC-091 修复（5 GL 路径过滤条件改 `notIn(BUDGET,COMMITMENT)`），触及损益结转/试算平衡核心路径按 §5 暂停协议显式裁决 ask-first。
