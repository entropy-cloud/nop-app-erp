# 2026-08-07-0330-3 rc-ma4-a4-1-3-project-settlement-provider-census UC-FIN-03 PROJECT_SETTLEMENT Provider 注册实例普查

> Plan Status: completed
> Last Reviewed: 2026-08-07
> Mission: requirement-compliance
> Work Item: A4.1.3（MA4 运行时行为验证 — A1.1 §7-3：UC-FIN-03 PROJECT_SETTLEMENT businessType 是否已有 IErpFinAcctDocProvider Bean 注册，实例普查）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A4.1.3；存疑点来源 `docs/audits/2026-08-02-1645-rc-ma1-a1-1-finance-f1-posting.md` §7 存疑点 3
> Related: `docs/plans/2026-08-07-0300-3-rc-ma4-a4-1-finance-runtime-expander.md`（A4.1 展开器 done，本行即其展开的实体行 / deferred successor）、`docs/plans/2026-08-02-1600-1-rc-ma1-a1-1-finance-f1-posting-engine.md`（A1.1 done，UC-FIN-03 可插拔 Provider 路由判「接受」）
> Audit: required

## Current Baseline

> 本计划是**运行时行为验证**（verification or audit work），结果表面 = 一份 A4.1.3 验证报告（落盘 `docs/audits/2026-08-07-0330-rc-ma4-a4-1-3-project-settlement-provider-census.md`）+ 必要时 arm-index finding 登记。**不改代码/ORM/api.xml/真相源**（只读普查：rg `getSupportedBusinessTypes` / Provider 注册点 + 读既有 JUnit）。

- **存疑点原文**（A1.1 报告 §7 存疑点 3，`2026-08-02-1645-...-a1-1-posting.md:295`）：「UC-FIN-03 PROJECT_SETTLEMENT businessType 是否已有 Provider 注册」——L3 证实可插拔机制通用（`ErpFinAcctDocRegistry` init:45-79，`@Inject List<IErpFinAcctDocProvider>` 自动聚合 + EnumMap 路由 + 重复 fail-fast + 默认填充），但 UC 文本以 `PROJECT_SETTLEMENT` 为**示例 businessType**，该具体 businessType 是否已有 `IErpFinAcctDocProvider` Bean 注册属**实例普查**——交 MA4 A4.1 展开（rg `getSupportedBusinessTypes` 含 PROJECT_SETTLEMENT 的 Provider）。

- **需求契约（L1 权威）**：`docs/design/finance/use-cases.md:60` UC-FIN-03 断言①「新增 IErpFinAcctDocProvider Bean(注册 businessType=PROJECT_SETTLEMENT)」②「Registry 自动聚合(@Inject List)」③「项目结算单审核时, 该 Provider 被路由调用, 生成凭证」④「核心过账引擎代码无改动」。**UC-FIN-03 验收的是「可插拔机制」（机制层）**，PROJECT_SETTLEMENT 为示例——A1.1 据此对 UC-FIN-03 判「接受」（机制 4 断言 L3-L5 全证据一致）。本存疑点是 A1.1 遗留的**实例层补查**：该示例 businessType 当前是否已有 Provider 实例（影响「示例是否已落地为可用能力」，非机制符合性）。

- **实现现状（L3，实测锚点）**：
  - Registry：`module-finance/erp-fin-service/.../service/posting/ErpFinAcctDocRegistry.java` init:45-79（List 注入 :37/41 + EnumMap :47-74 + 非默认优先 :49-62 + 重复 fail-fast :55-60 + 默认填充 :64-71）/ getProvider:81-83。
  - Provider 注册面（本验证对象）：rg `getSupportedBusinessTypes`（或等价注册声明）含 `PROJECT_SETTLEMENT` 的 `IErpFinAcctDocProvider` 实现；以及 projects 域（项目结算单）是否经该 Provider 过账（调用链 evidence）。
  - **注意（独立草案审查实测）**：实仓 grep 已初现 `module-projects/.../ProjectSettlementAcctDocProvider.java` 注册 `ErpFinBusinessType.PROJECT_SETTLEMENT`（enum 约 430）+ `ProjectSettlementPostingDispatcher` 派发——故本验证**很可能结论为「实例层接受（示例已落地）」**，即这是一次**确认性普查**而非发现性普查（边际信息量较低）。剩余价值 = 调用链证据 + `TestErpFinAcctDocRegistry` 是否含 PROJECT_SETTLEMENT 用例的覆盖核查 + 完整 businessType→Provider 映射表（为后续 A4.2/MA4 复用）。Phase 1 仍须实仓核实（不预判结论），但执行者应预期「接受」为最可能结果。

- **既有证据（复用输入）**：
  - A1.1 §5.1 UC-FIN-03 矩阵行「接受」（机制 4 断言全证据）+ §4.1 MA2 §5.7「38 Provider 一致经引擎路径，零绕过」。
  - 本验证**不重新核实机制符合性**（A1.1 已定级接受）；只补「PROJECT_SETTLEMENT 实例是否存在」这一实例层事实。

- **剩余差距**：PROJECT_SETTLEMENT businessType 是否已有 Provider Bean 注册**未实例普查**（A1.1 证机制通用，未核该示例实例）。

- **保护区域**：只读普查（rg + 读 JUnit），不触及 ORM/会计过账逻辑/数据删除。属 roadmap 预授权类目。本验证**不实施修复**——若 UC 隐含「示例应可用」且实例缺失被定为分歧，修复经 MR1（新增 Provider 属会计过账逻辑，须 ask-first + 独立 plan-audit）。

## Goals

- 实例普查：rg 全仓 `IErpFinAcctDocProvider` 实现的 `getSupportedBusinessTypes`（或等价注册声明），确认 `PROJECT_SETTLEMENT` 是否已被任一 Provider 注册。
- 给出实例层证据：注册该 businessType 的 Provider 文件:行（或「无 Provider 注册」结论）+ projects 域项目结算单过账调用链（是否经该 businessType 路由）+ 既有 JUnit 覆盖（TestErpFinAcctDocRegistry 是否含 PROJECT_SETTLEMENT 用例）。
- 对齐 UC-FIN-03 语义给出实例层结论：①机制符合性 = 接受（A1.1 已定，本验证不推翻）；②若 product-scope/use-cases 隐含「PROJECT_SETTLEMENT 示例应可用」且实例缺失 → 记为 P2（示例能力未落地，非机制缺陷，非默认活跃路径破坏）或登记为 successor（需求未硬性要求该示例可用时）；裁决须三源对照。
- 产出验证报告 + §8 过程纪律自检；finding（若有）按 §7 裁决登记 arm-index。

## Non-Goals

- **不重新核实 UC-FIN-03 机制符合性**（A1.1 已判接受；本验证只补实例层事实）。
- **不修改代码/ORM/api.xml/BizModel**（只读普查）。
- **不实施 Provider 新增修复**（经 MR1；属会计过账逻辑须 ask-first）。
- **不修改真相源**（§9 冻结；若需澄清「示例是否为硬性需求」须登记分歧，不直改 product-scope/use-cases）。

## Task Route

- Type: `verification or audit work`（运行时实例普查）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 判据 + §7 衔接 + §8 自检 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A4.1.3 行）+ `docs/audits/2026-08-02-1645-rc-ma1-a1-1-finance-f1-posting.md` §7 存疑点 3 + §5.1 UC-FIN-03（输入）+ `docs/design/finance/posting.md §过账引擎(可插拔)` + `docs/design/projects/`（项目结算单过账契约）。
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定）。实例普查需多维度归类（Provider 注册面 / 调用链 / 测试覆盖 / 需求语义边界）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。只读普查（rg + 读 JUnit + 引用 A1.1）。§8 自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - PROJECT_SETTLEMENT Provider 注册实例普查

Status: completed
Targets: `docs/audits/2026-08-07-0330-rc-ma4-a4-1-3-project-settlement-provider-census.md`（验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: A4.1 done；A1.1 done（UC-FIN-03 接受 + §7 存疑点 3 已落盘）

- [x] `Proof` 注册面普查：rg 全仓 `IErpFinAcctDocProvider` 实现的 `getSupportedBusinessTypes`（或等价注册声明，如注解/EnumMap 显式 put），产出全部已注册 businessType → Provider 映射表；确认 `PROJECT_SETTLEMENT` 是否在其中。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 调用链证据：projects 域项目结算单（projects 域过账入口）过账时是否以 businessType=PROJECT_SETTLEMENT 经引擎路由（调用链 file:line）；若无 Provider 注册，确认该 businessType 当前命中默认 fallback 或无过账。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 测试覆盖：`TestErpFinAcctDocRegistry`（及 projects 域过账测试）是否含 PROJECT_SETTLEMENT 用例（引用 + 断言强度强/弱/无）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` 实例层符合性结论（三源对照 + §2 判据）：UC-FIN-03 机制符合性维持「接受」（A1.1 已定）；实例层——①若 PROJECT_SETTLEMENT 已有 Provider 注册 → 实例层「接受」（示例已落地）；②若未注册且 product-scope/use-cases 未硬性要求该示例可用 → 登记 successor（示例能力待落地，非缺陷）；③若未注册但需求隐含「项目结算单应经专用 Provider 过账」且当前无过账/命中 fallback → P2（示例能力未落地，非默认活跃路径破坏）。裁决须列明 L1/L2/L3 三源 + 不与 A1.1 UC-FIN-03 接受结论冲突（机制 vs 实例分层）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] businessType→Provider 全集映射表落盘 + PROJECT_SETTLEMENT 注册状态明确
- [x] 实例层符合性结论明确（接受 / successor 登记 / P2），与 A1.1 UC-FIN-03 机制接受结论分层一致

### Phase 2 - finding/successor 衔接 + §8 自检 + 报告定稿

Status: completed
Targets: `docs/audits/2026-08-07-0330-rc-ma4-a4-1-3-project-settlement-provider-census.md`（定稿）；`docs/audits/arm-index.md`（若新 finding/successor）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 普查 + 实例层结论完成

- [x] `Add` 若定 P2 或 successor → 按 §7 grep arm-index 同域同控制点（PROJECT_SETTLEMENT / projects 过账 / Provider 注册面）裁决「复用 or 新建」`P*-RC-xxx` 或 successor 行，写入 arm-index MA4 分区；双向可追溯注记（finding → MR1 / successor 触发条件）。
      - Skill: none
      - Note: 条件 no-op——Phase 1 实例层裁决为「接受（示例已落地）」，不命中 P2/successor 前提（§6.2）；§7.1 grep arm-index 无同控制点 open finding；不新增 arm-index 行。
- [x] `Proof` §8 过程纪律自检：运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明「无回归风险」）；closure-audit 独立性声明；与 arm-index 交叉去重声明。不以 checker 退出码 0 作为门控依据。
      - Skill: none

Exit Criteria:

- [x] 验证报告定稿（映射表 + 实例层结论 + finding/successor 衔接 + §8 自检齐全）
- [x] 新 finding/successor（若有）已写入 arm-index MA4 分区并有 grep 依据（本验证无新 finding/successor，条件 no-op）

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_02c7b35bdffe5vU7kJbw6DJgv0，fresh session，未起草本计划）。逐项核验 A-J 全 PASS：Deps（A4.1 done 实测）、Baseline 逐项实测命中（ErpFinAcctDocRegistry.init/getProvider / getSupportedBusinessTypes 注册面 / A1.1 §7 存疑点 3 逐字忠实引用 / UC-FIN-03 机制接受结论分层正确）、只读普查正确、决策树（接受/successor/P2）与 Q4 一致（successor 与 P2 对*示例*层合法，A1.1 已在*机制*层接受 UC-FIN-03，Q4 仅禁 P0/P1 方案 B）、新增 Provider 属会计过账逻辑须 ask-first 正确、完整枚举纪律（全部已注册 businessType→Provider 映射表）、反松弛合规、item typing/Skill 记录齐、Closure Gates audit-only 删 build/test 有据。无阻塞。采纳非阻塞建议（已修订入计划）：实仓 grep 已初现 `module-projects/.../ProjectSettlementAcctDocProvider.java` 注册 PROJECT_SETTLEMENT（enum 约 430）+ ProjectSettlementPostingDispatcher——已将 baseline 显式标注「本验证很可能结论为接受（示例已落地），属确认性普查，边际信息量较低；剩余价值 = 调用链证据 + 测试覆盖核查 + 完整映射表」；Phase 1 仍须实仓核实不预判。共识达成，转 active。

## Closure Gates

> 本计划为**只读运行时普查**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 注册面映射完整性 + 实例层结论 + finding/successor 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A4.1.3 验证报告注册面映射齐全 + 实例层结论 + finding/successor（若有）登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §MA4 + §2 判据 + §去重协议一致；与 A1.1 §7-3 + UC-FIN-03 机制接受结论分层一致
- [x] 已运行验证：注册面映射完整性 + §8 checker actual vs baseline 实测记录（本计划无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（independent closure audit done by fresh-session subagent on 2026-08-07）
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### PROJECT_SETTLEMENT Provider 实例缺失的修复（若定 P2/successor）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是运行时普查，结果表面 = 验证报告 + 实例层结论 + finding/successor 登记。UC-FIN-03 机制符合性已接受（A1.1）；示例 Provider 实例缺失属示例能力未落地（非机制缺陷、非默认活跃路径破坏）。修复（新增 PROJECT_SETTLEMENT Provider）经 MR1（R1.0→RC-R1.n，属会计过账逻辑须 ask-first + 独立 plan-audit）。本验证闭环不阻塞于修复落地。
- Successor Required: yes（若登记 successor：触发条件 = projects 域项目结算单过账需求明确要求专用 Provider 时回队 MR1）

## Closure

> （独立结束审计通过后填入）

- **Verdict**: **PASS**（2026-08-07）
- **Auditor**: 独立结束审计子代理（fresh session，未起草本计划、未执行本计划、非草案审查者 ses_02c7b35bdffe5vU7kJbw6DJgv0）。冷重播全部验证，对照 LIVE 仓库逐项核实，非信任报告。
- **Phase 一致性**: Phase 1（4 item + 2 Exit Criteria 全 `[x]`，Status: completed）、Phase 2（2 item + 2 Exit Criteria 全 `[x]`，Status: completed）、Plan Status: completed。Phase item / Exit Criteria 无 `[ ]` 残留（仅本节 Closure Gates 原为 `[ ]`，本次审计已逐项勾选）。
- **核心证据独立核实（file:line）**:
  - `ProjectSettlementAcctDocProvider.java:52-54` → `Collections.singleton(ErpFinBusinessType.PROJECT_SETTLEMENT)` ✅
  - `ErpFinBusinessType.java:56` → `PROJECT_SETTLEMENT(430)` 在册 ✅
  - `ErpFinAcctDocRegistry.java` init:45-79（List 注入 :37/41 + EnumMap :47 + 非默认优先 :49-62 + fail-fast :55-60 + 默认 putIfAbsent :64-71）/ getProvider:81-83 ✅
  - `ProjectSettlementPostingDispatcher.java` buildEvent:73 `setBusinessType(PROJECT_SETTLEMENT)` / tryPost:40-53 `executor.postEvent` / reverse:60 冲销同向 ✅
  - `ErpPrjProjectSettlementProcessor.java` doPost:140-147 `postingDispatcher.tryPost` ✅
  - `module-projects/.../_vfs/erp/prj/beans/app-service.beans.xml:22-23` 显式 `<bean ... ProjectSettlementAcctDocProvider/>` ✅
  - `TestErpFinAcctDocRegistry.java` 确证**不含** PROJECT_SETTLEMENT（用 PURCHASE_INPUT/AP_INVOICE/SALES_OUTPUT 示例验机制层，设计正确）✅
  - `TestErpPrjAcctDocProviderAccountKey.java:50` 直接 `setBusinessType(PROJECT_SETTLEMENT)` 命中目标 Provider（2 @Test 强断言）✅
- **§3.2 映射表抽样（4/37 Provider，全部一致）**: LandedCostAcctDocProvider.java:47→LANDED_COST(490)；SubcontractFeeAcctDocProvider.java:42→SUBCONTRACT_FEE(504)；SalaryPostingProvider.java:53-59→SALARY/SALARY_PAYMENT/SOCIAL_INSURANCE_ER/HOUSING_FUND_ER；AssetMergeAcctDocProvider.java:37→ASSET_MERGE(450)。37 行总数 = rg `implements IErpFinAcctDocProvider`（38 命中）− 1 测试文件（TestErpFinAcctDocRegistry）= 37 生产 Provider，与报告一致 ✅。
- **结论逻辑健全性**: A1.1 报告 `:217` UC-FIN-03 矩阵行机制层「接受（PROJECT_SETTLEMENT 为示例 businessType）」+ `:295` §7-3 存疑点逐字引用一致。本验证实例层「接受（示例已落地）」与 A1.1 机制层接受**分层一致，无冲突**。无 P2、无 successor 的裁决命中 §2 判据①，前提②③「未注册」为假——逻辑成立 ✅。
- **Roadmap**: `docs/backlog/requirement-compliance-roadmap.md` A4.1.3 行 `todo` → `done ✅`（git diff 确认）✅。
- **真相源未变更**: `git status --short` 仅 3 改 + 1 增，全部在 `docs/`（backlog/requirement-compliance-roadmap.md、logs/2026/08-07.md、plans/2026-08-07-0330-3-...md、audits/2026-08-07-0330-...md）。**零** Java/ORM/`*.api.xml`/view.xml/xbiz/字典/种子/配置变更 ✅。
- **§8 checker 复跑**: `bash docs/audits/nop-compliance-checker.sh` 退出码 1（R3 段既有行为，A4.1.2 报告 §5.1 已文档化，与本计划无关）。checker 为纯 reporter 非门控；本计划零生产代码变更故结构上无回归风险。
- **非阻塞观察（Non-blocking）**:
  1. 报告 §8.1 标注 checker `EXIT=0` 与实测退出码 1 不符（R3 段既有行为）。建议报告改为「退出码 1（R3 段既有行为，非门控）」以精确对齐 A4.1.2 §5.1 的表述。
  2. 报告 §8.1 表将 R1 折叠为 `0`（仅对应 R1a/b/c；R1d=14 未单列）、R2 折叠为单值 `34`（即 R2a，未含 R2b=229/R2c=1382/R2d=34）。底层计数与前序基线一致、非伪造，仅是汇总粒度粗于 A4.1.2 §5.1 的细分表。建议后续报告沿用 A4.1.2 的 R1a-d/R2a-d 细分以提高可审计性。
- **Audit Scope Confirmation**: 本次审计为只读（未修改代码/真相源）；仅勾选本计划 Closure Gates + 填写本 Closure 证据块。未改动报告本体与 roadmap。
