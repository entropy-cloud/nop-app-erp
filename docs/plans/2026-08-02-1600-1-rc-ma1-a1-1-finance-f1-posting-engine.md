# 2026-08-02-1600-1 rc-ma1-a1-1-finance-f1-posting-engine finance-F1 过账引擎与凭证链路需求符合性审计

> Plan Status: completed
> Last Reviewed: 2026-08-02
> Mission: requirement-compliance
> Work Item: A1.1（MA1 需求追踪矩阵审计 — finance-F1 过账引擎与凭证链路）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.1
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.1 的 0.2 依赖）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点的是被审功能的现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1 五级矩阵 / §2 分级判据 / §3 完整枚举 / §4 Q1 真相源层级 / §5 Q4 修复义务 + 保护区域暂停协议 / §6 报告 9 段骨架 / §7 arm-index 衔接 / §8 过程纪律自检 / §9 真相源冻结 / §10 MR0/MR1 机制 / §去重协议）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.1 给出 UC 清单 = `UC-FIN-01/02/03/04/12/15`（6 UC），含每 UC 的 `use-cases.md:line` 锚点。

- **L1 需求契约（权威真相源）**：`docs/design/finance/use-cases.md`：
  - UC-FIN-01 业财自动过账（`:16`）：审核触发按 businessType 路由 Provider → 填充凭证模板 → FactsValidator → 写库 + VoucherBillR 业财回链 → 单据 `posted=true`；断言含借贷平衡、VoucherBillR 双向回链。
  - UC-FIN-02 业务单据作废触发红字冲销（`:42`）：作废 → 经 VoucherBillR 反查关联凭证 → 红字凭证（金额取负，关联原凭证）+ 原凭证 `isReversed=true` → 单据 `posted=false`；红字凭证走 DRAFT→POSTED。
  - UC-FIN-03 可插拔 Provider 路由（`:60`）：新增 `IErpFinAcctDocProvider` Bean（注册 businessType）→ `ErpFinAcctDocRegistry` 自动聚合（`@Inject List`）→ 该 businessType 被路由调用，核心过账引擎零改。
  - UC-FIN-04 FactsValidator 科目分摊（`:76`）：原始凭证行命中 GlDistribution 规则拆多行；`Σ 拆分行金额 == 原行金额`；`Σ percent != 100` 抛异常拒绝过账。
  - UC-FIN-12 多币种过账（`:223`）：凭证行本位币金额 == 源币金额 × 汇率；**若汇率缺失 → 报错拒绝过账**；外币对账未达账项调整考虑汇兑损益 `EXCHANGE_GAIN_LOSS`。
  - UC-FIN-15 科目分摊(GL Distribution)（`:298`）：UC-FIN-04 同主题更详细断言版，含 `ErpFinGlDistributionValidator(IErpFinFactsValidator 实现)` 执行分摊、`getOrder() 较高`。
  - **基线分歧 D-02**（`rc-requirement-baseline-inventory.md §基线分歧登记`）：UC-FIN-04/15 同主题均归 A1.1（roadmap 已裁决），审计时按 L1 逐字引用两条验收标准，不合并（§9 冻结）。

- **L3 代码实现现状（实测，subagent 探查）**：
  - 过账引擎主体**已实现**：`ErpFinPostingProcessor.process():127`（正向过账）/ `reverseProcess():218`（红字冲销）；`ErpFinAcctDocRegistry:46/81`（Provider 注册 + O(1) 路由 + 重复 fail-fast）；`ErpFinVoucherBizModel:69-105`（`post/reverse` 入口）。
  - 业财回链 + `isReversed` + 幂等：`persistVoucher():857-867`（VoucherBillR 写入）、`markOriginalVoucherReversed():933-947`（原凭证 `isReversed=true`）、`alreadyPosted():485-497`（幂等跳过）。
  - 多账套传播：`SchemaPropagator.resolveTargetSchemas:134` + `translateFactsForSchema:665-720`。
  - 跨域执行器：`FinPostingExecutor:23-37`（postEvent/reverse 经 `IErpFinVoucherBiz`）。
  - **已知缺口 ①（UC-FIN-04/15 科目分摊）**：`ErpFinGlDistributionValidator` 类**不存在**——`rg "implements IErpFinFactsValidator"` 全仓零命中（接口仅被 `ErpFinAcctDocRegistry` 作为集合元素类型引用，无任何业务实现 Bean）；FactsValidator 调用循环（`ErpFinPostingProcessor:563-565`）在无 Delta 注入时为死代码；"科目分摊"唯一痕迹是 UI 菜单资源 `module-finance/erp-fin-web/.../erp-fin.action-auth.xml:189-193`。即 `Σ percent != 100 拒绝过账` / 拆行平衡 **未实现**（疑似 P1 功能缺失）。
  - **已知缺口 ②（UC-FIN-12 汇率缺失拒绝过账）**：`event.getExchangeRate()` 为 null 时引擎静默回退 `EXCHANGE_RATE_DEFAULT=1`（`ErpFinPostingProcessor:78,537,818-820`），**未拒绝过账**；`TestErpFinPostingService.testMultiCurrencyPostingLineLevelAssertions:243-283` 的 javadoc 将完整多币种源币金额迁移称为"documented successor"。即需求"汇率缺失→拒绝过账"与实现冲突（疑似 P0/P1 会计过账正确性分歧）。
  - 既有 FX 实现：`ExchangeRevaluationService.revalue():69`（期末 AR/AP + 银行存款重估→`EXCHANGE_GAIN_LOSS(130)` 凭证）；`NotesReceivableAcctDocProvider:40-90`（贴现 FX 6051 分支）。

- **L4 测试证据现状**：`TestErpFinPostingService`（happy/幂等/不平衡/期间 CLOSED 拒绝/冲销/多币种行级断言）、`TestErpFinAcctDocRegistry`（fallback/域优先级 + 重复 fail-fast）、`TestErpFinReversalDispatch`/`TestErpFinReversalListenerRegistry`（冲销事件派发 + 失败隔离）、`TestErpFinMultiSchemaPosting`、`PropertyErpFinMultiCurrencyBalance`（jqwik 多币种平衡不变式）、`TestErpFinExchangeRevaluation`、`TestErpFinPostingFaultInjection`。**注意**：UC-FIN-04/15 科目分摊**无测试**（类不存在）；UC-FIN-12 汇率缺失拒绝过账的测试**断言的是回退行为（amountSource==amountFunctional）而非拒绝**。

- **L5 既有证据（MA2 复用输入，方法论 §去重协议）**：
  - `docs/audits/2026-07-27-2211-arm-ma2-finance-posting-voucher-state-machine.md`（过账 + 凭证状态机行为）。
  - `docs/audits/2026-07-28-2130-arm-ma4-finance-posting-voucher-code-quality.md`（过账代码质量）。
  - `docs/audits/2026-07-27-1949-arm-ma2-procure-to-pay-e2e.md` / `...-order-to-cash-e2e.md` / `...-period-close-e2e.md`（跨域过账链 E2E 行为）。
  - E2E specs：`tests/e2e/business-actions/finance-voucher-post.action.spec.ts`、`fin-gl-mapping-routing.action.spec.ts`、`fin-notes-receivable*.action.spec.ts`、`fin-notes-payable*.action.spec.ts`、`fin-employee-advance*.action.spec.ts`、`fin-expense-claim*.action.spec.ts`、`orchestration/p2p-chain.spec.ts` / `o2c-chain.spec.ts` 等。
  - **本切片须声明与上述 MA2 报告的差异增量**（报告段落 9）：复用其已证实的状态机/链路行为，只补"需求契约↔行为"差异（科目分摊缺失、汇率缺失未拒绝）。

- **保护区域**：本审计为**只读审计**（读代码/测试/报告，不改代码/ORM/api.xml/真相源）。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按方法论 §10，P0 经 MR0 即时通道、P1 经 MR1（R1.0 展开 RC-R1.n）修复；触及 ORM/会计过账逻辑的修复行须 ask-first（§5 保护区域暂停协议）。

- **剩余差距**：A1.1 切片的五级追踪审计报告缺失 = MA4（A4.1 业财展开器，Deps=MA1 done）及 MR1（R1.0，Deps=MA1-MA4 done）的部分阻塞来源。本计划产出 A1.1 报告并登记 finding，解除其在 MA4/MR1 链路的该切片证据缺口。

## Goals

- 产出 A1.1 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-1-finance-f1-posting.md`，含方法论 §6 **9 段全部内容**：①UC-FIN-01/02/03/04/12/15 需求契约原文（逐字引用，不转述）②实现证据（`file:line`，跨域调用链列全）③测试证据（注明断言强度）④运行时行为证据（复用 MA2/E2E，补差异）⑤五级追踪矩阵 + 每 UC 符合性结论（P0/P1/P2/接受）⑥与 arm-index 衔接（复用 or 新增 裁决）⑦静态存疑点清单（供 MA4 展开）⑧过程纪律自检段 ⑨与 MA2 报告差异增量声明。
- 对 6 UC 逐条核验**每条验收标准**（完整枚举，§3）：禁止 UC 跳号、禁止验收标准抽样、禁止跨 UC 合并行；每个 UC 一矩阵行。
- 对已知缺口①②给出分级结论：科目分摊缺失（UC-FIN-04/15）、汇率缺失未拒绝过账（UC-FIN-12）——按 §2 判据定级，若为 P0/P1 则新建 `P0-RC-xxx`/`P1-RC-xxx` 并按 §10 触发 MR0/MR1（本计划仅登记，不实施修复）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区）。

## Non-Goals

- **不修复 finding**（修复属 MR0 即时通道 / MR1 R1.0 展开的 RC-R1.n；本计划是审计，结果表面 = 一份报告 + arm-index 登记）。
- **不修改真相源**（product-scope / finance use-cases / owner doc 需求契约段落；§9 冻结条款——分歧记入报告，不直改真相源）。
- **不修改代码/ORM/api.xml/BizModel/Processor/view.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.2-A1.51 各自独立 plan；A1.1 只覆盖 UC-FIN-01/02/03/04/12/15）。
- **不执行 MA4 运行时探针展开**（A4.1 展开器读取本报告静态存疑点清单后追加 A4.1.n 实体行；本计划只产出存疑点清单）。
- **不重跑既有 MA2 行为审计**（§去重协议：MA2 已证实行为直接引用，只补需求视角差异）。

## Task Route

- Type: `verification or audit work`（需求→实现符合性五级追踪审计；非实现变更、非需求澄清）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（审计契约 §1-§10 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.1 工作项 + Work Item Details MA1）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.1 UC 锚点 + D-02 基线分歧）+ `docs/design/finance/use-cases.md`（L1 真相源）+ `docs/design/finance/posting.md` / `cost-center.md`（L2 设计参考，非真相源）+ `docs/audits/arm-index.md`（finding 衔接）+ 上述 MA2/MA4 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。该技能定义多维审计 prompt 范式，本切片需求↔实现符合性审计复用其维度框架；其必需输入（owner doc + use-cases + 代码路径 + 测试）均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。**L5 行为证据**默认复用既有 MA2 报告 + E2E recordings（方法论 §去重协议），无需起服务；若需对存疑点做即时行为确认，可跑既有 JUnit（`mvn test -pl module-finance/erp-fin-service -Dtest=...`）或读 E2E 录制，不引入新依赖。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更故无回归风险，仅记录 actual vs baseline）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论

Status: completed
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-1-finance-f1-posting.md`（新建，先填 §1-§5；命名遵循方法论 §归档规范）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done（方法论契约 + UC 锚点就绪）

- [x] `Proof` 对 UC-FIN-01/02/03/04/12/15 **逐 UC 一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:line` 验收标准原文（禁止转述）；L2 引用 `posting.md`/`cost-center.md` 对应 section（标注"设计参考，冲突以 L1 为准"）；L3 引用 `module-finance/erp-fin-service/.../<file>:line`（含 `ErpFinPostingProcessor`/`ErpFinAcctDocRegistry`/`ErpFinVoucherBizModel`/`persistVoucher`/`markOriginalVoucherReversed`/`SchemaPropagator` 跨域调用链列全）；L4 引用 `Test*.java#method` / E2E spec（注明断言强度，引用 MA5 评级）；L5 复用 MA2/E2E 已证实行为 + 本切片差异。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 重点核验**已知缺口**（逐条验收标准对照）：①UC-FIN-04/15 科目分摊（`ErpFinGlDistributionValidator` 是否存在、`Σ percent != 100 拒绝过账`、拆行平衡）；②UC-FIN-12 汇率缺失（实现是回退 rate=1 还是拒绝过账）；UC-FIN-01 业财回链/VoucherBillR 双向；UC-FIN-02 红字冲销 `isReversed` + `posted=false`；UC-FIN-03 可插拔 Provider 零改核心（注册新 businessType 是否被自动路由）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` 按 §2 判据对每 UC 给出符合性结论（P0/P1/P2/接受，取最高）：已知缺口①若确认为"功能完全缺失"→ P1（§2 P1①）；已知缺口②若确认为"会计过账正确性破坏/异常路径未实现"→ P0 或 P1（§2 P0④ / P1②）；其余 UC 按实测定级。每结论须列明命中判据编号 + 三源对照。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] 报告 §1-§5 已落盘：6 UC 各一矩阵行（无跳号、无跨 UC 合并），L1 逐字引用、L3 含行号、L4 注明断言强度、L5 标注复用 MA2 来源
- [x] 每 UC 有符合性结论（P0/P1/P2/接受）且列明 §2 判据编号；已知缺口①②有明确分级（非悬空"待查"）

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: completed
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-1-finance-f1-posting.md`（补 §6-§9，报告定稿）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成（矩阵 + 结论已出）

- [x] `Decision` **复用 or 新增 裁决**（§7）：产出 finding 前 grep `arm-index.md` finance 过账同域同控制点（如 P1-MA2-001 GRNI 冲回 / 多币种 / 凭证幂等相关行）后裁决——同根因同控制点 → 复用既有 ID（追加 RC 交叉引用注记，不新建）；新根因/新功能点 → 新建 `P0-RC-xxx`/`P1-RC-xxx` 并列明与既有 finding 的差异依据。禁止未经比对直接新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 的复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR0/MR1）。
      - Skill: none
- [x] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记本切片 L5 无法静态定论、需运行时确认的点（每存疑点一行；无则注明"无"）。**P0 即时通道**：若 Phase 1 定级出 P0，按 §10 在报告登记并在本计划记录"已触发 MR0 追加 R0.n 实体行"（本计划不实施修复）。
      - Skill: none
- [x] `Proof` 报告 §8 过程纪律自检段（§8 模板）：实际运行 `bash docs/audits/nop-compliance-checker.sh` 并附 actual vs baseline 汇总表（本审计无生产代码变更，注明"无回归风险"）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 脚本退出码 0 作为门控通过依据**（区分 reporter vs CI 门控，吸取 R6.9 教训）。
      - Skill: none
- [x] `Add` 报告 §9 与 MA2 报告差异增量声明：声明复用 `2026-07-27-2211-arm-ma2-finance-posting-voucher-state-machine.md` 等已证实行为，列明本切片只补的需求视角差异（科目分摊缺失 / 汇率缺失未拒绝等）。
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

- Independent draft review iteration 1: `acceptable as-is`（独立子代理 ses_03e386542ffeLidbdn30pKF6f6，fresh session，未起草本计划）。12 项检查 A-L 全 PASS：格式完整、Deps 正确（M0.1+M0.2 实测 completed）、单结果表面（无 finance 全域合并，§3 合规）、Baseline 准确（process/reverse/persistVoucher/markOriginalVoucherReversed/EXCHANGE_RATE_DEFAULT 行号逐项实测命中；科目分摊 stub + 汇率回退两缺口结论经实仓核实为真）、UC 覆盖 UC-FIN-01/02/03/04/12/15 精确、方法论 §1-§10 + §去重对齐、反松弛合规、Closure Gates audit-only 删除 build/test 有据、无范围蔓延（finding→MR0/MR1，真相源不动）、item typing 合规、Skill multi-dimensional-audit-prompt.md 输入就绪、Plan Status=draft。无阻塞。1 项 non-blocking 已吸收（N1：FactsValidator grep 引用措辞精确化——"全仓零命中，接口仅被 Registry 作集合元素类型引用"，结论不变，本次已修订）。共识达成，可转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控——审计报告产出不触发编译或测试。验证 = 报告 9 段完整性 + 五级矩阵逐 UC 覆盖 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。（§8 含 checker 实测记录，但 checker 是 reporter 非门控；门控真值在 CI workflow。）

- [x] 范围内行为完成：A1.1 报告 9 段齐全 + 6 UC 逐矩阵行 + finding 登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.1 锚点一致
- [x] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（本计划无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按方法论 §10 经 MR0（P0 即时通道）/ MR1（R1.0 展开 RC-R1.n，P1 批量）实施；触及会计过账逻辑（如 UC-FIN-12 汇率缺失拒绝、科目分摊 Validator）的修复行须 ask-first + 独立 plan-audit（§5 保护区域暂停协议）。本审计闭环不阻塞于修复落地。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行）

## Closure

Status Note: 已完成。审计报告 `docs/audits/2026-08-02-1645-rc-ma1-a1-1-finance-f1-posting.md` 9 段齐全，6 UC（UC-FIN-01/02/03/04/12/15）五级追踪矩阵填齐。UC-FIN-01/02/03 接受；2 项新 P1 finding（P1-RC-001 科目分摊机制完全缺失 / P1-RC-002 汇率缺失未拒绝过账）登记入 `docs/audits/arm-index.md` RC 分区，待 MR1（R1.0 展开为 RC-R1.n）。无 P0，未触发 MR0。本审计为只读（无代码/ORM/真相源变更）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 ses_03e2e17d5ffecJOhcvbmLfq3yg（fresh session，cold-context，未执行本计划）
- Verdict: pass（9 项核验 A-I 全 PASS，零 blocking issues）
- Evidence: 独立 grep 复核两缺口声明为真（`rg "implements IErpFinFactsValidator"`=0 业务实现 / `rg "ErpFinGlDistribution" --glob *.orm.xml`=0 实体；`ErpFinPostingProcessor:537,817-820` 静默回退 rate=1 而非拒绝）；L1 逐字引用核验；行号核验（≤1 行偏差）；6 UC 矩阵完整枚举；§2 分级 + 无 P0/MR0；arm-index RC 分区 + 报告清单；git status 仅文档变更（零代码/ORM/真相源修改）；§8 三 checkbox + checker 19 规则 0 漂移表；Non-Goals 无修复实施。推荐「Plan may be marked completed」。

Follow-up:

- 本报告 finding 由 MR0（P0）/ MR1 R1.0（P1）展开为 R0.n / RC-R1.n 修复行；静态存疑点由 A4.1（业财展开器，Deps=MA1 done）读取后追加 A4.1.n 实体行。P1-RC-001/002 修复触及会计过账逻辑（+ P1-RC-001 可能新增 ORM 实体），按方法论 §5 须 ask-first + 独立 plan-audit。
