# 2026-08-06-0847-1 rc-ma4-a4-1-4-budget-config-default-deployment-contract 预算控制 config 默认关闭部署契约核对

> Plan Status: completed
> Last Reviewed: 2026-08-06
> Mission: requirement-compliance
> Work Item: A4.1.4（MA4 运行时行为验证 — A1.2 §7-1：预算控制/承付 config 默认关闭 vs「开箱即用预算硬拦截」部署契约核对）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A4.1.4；存疑点来源 `docs/audits/2026-08-02-1700-rc-ma1-a1-2-finance-f2-budget.md` §7 存疑点 1
> Related: `docs/plans/2026-08-07-0300-3-rc-ma4-a4-1-finance-runtime-expander.md`（A4.1 展开器 done，本行即其展开的实体行）、`docs/plans/2026-08-02-1600-2-rc-ma1-a1-2-finance-f2-budget-commitment.md`（A1.2 done，§5.2 caveat ① 接受 + §7 存疑点 1）
> Audit: required

## Current Baseline

> 本计划是**运行时行为验证**（verification or audit work），结果表面 = 一份 A4.1.4 验证报告（落盘 `docs/audits/2026-08-06-0847-rc-ma4-a4-1-4-budget-config-default-deployment-contract.md`）+ 必要时 arm-index finding 登记。**不改代码/ORM/api.xml/真相源**（只读核对：grep product-scope/部署文档/config 默认值 + 读既有 JUnit + 复用 MA2）。

- **存疑点原文**（A1.2 报告 §7 存疑点 1，`2026-08-02-1700-...-a1-2-budget.md:305`）：「caveat ① config 默认关闭是否与"开箱即用预算硬拦截"部署契约冲突」——L3 静态确认 `isBudgetCheckEnabled`/`isCommitmentEnabled` 默认 false（控制机制完整正确，仅 config-gate 条件启用）。L1 未显式声明"默认开启"。若产品存在"开箱即用预算控制"隐含契约，则属默认行为分歧——交 MA4 A4.1 运行时确认（核对 product-scope / 部署文档是否声明默认预算控制启用；无则维持接受）。当前无 P1 上证据。

- **关联既有结论**（A1.2 §5.2 caveat ①）：config 默认关闭 = **接受**（非 P1）。理由：控制机制完整正确（HARD 拦截异常路径已实现非缺失），L1 未强制"默认开启"，config-gate 是 ERP 通用启用范式。残留观察：config 默认关闭意味着"开箱默认不启用预算控制/承付"——本验证即核实是否存在"开箱即用预算硬拦截"的隐含部署契约。

- **需求契约（L1 权威）**：`docs/design/finance/use-cases.md:204,238` UC-FIN-11/13——描述控制**语义**（HARD/WARN/NONE 三通道 + 余量公式 + 承付 commit/release 触发条件），**未声明"必须默认开启"**。UC 文本以"采购订单.审核 → 调用 check"描述控制机制，控制级别由命中的 BudgetScenario.controlLevel 决定。

- **实现现状（L3，实测锚点）**：
  - 控制总开关：`module-finance/erp-fin-service/.../service/budget/ErpFinBudgetControlBiz.java` `isBudgetCheckEnabled:225-228`（`AppConfig.var(CONFIG_BUDGET_CHECK_ENABLED, Boolean.FALSE)`）。
  - 承付总开关：`module-finance/erp-fin-service/.../service/budget/ErpFinBudgetCommitmentBizModel.java` `isCommitmentEnabled:117-119`（`AppConfig.var(CONFIG_BUDGET_COMMITMENT_ENABLED, Boolean.FALSE)`）。
  - config key 常量：`module-finance/erp-fin-service/.../service/ErpFinConstants.java` `CONFIG_BUDGET_CHECK_ENABLED="erp-fin.budget-check-enabled":379` / `CONFIG_BUDGET_COMMITMENT_ENABLED="erp-fin.budget-commitment-enabled":419`。
  - 消费点（config-gate 短路）：`ErpPurOrderProcessor.runBudgetCheckHook:178` / `ErpPurOrderProcessor.runCommitmentCommitHook:198` / `ErpPurOrderProcessor.runCommitmentReleaseHook:223` / `ErpPurInvoiceProcessor:274` / `ErpPurPaymentProcessor:168` / `ErpSalOrderProcessor:312,333` / `ErpSalInvoiceProcessor:322`——全部 `AppConfig.var(..., Boolean.FALSE)` 默认关闭。
  - owner doc：`docs/design/finance/budget.md §配置项`——`erp-fin.budget-commitment-enabled` 默认 false（"保护既有 113 purchase 测试不触发承付凭证"）；控制开关 javadoc 自述"默认 false，向后兼容"。

- **既有证据（复用输入）**：
  - A1.2 §5.2 caveat ① 已静态确认 config 默认关闭 = 接受（控制机制完整正确）。
  - MA2 `2026-07-28-1249-arm-ma2-budget-commitment-release.md`（A2.16）证实 config-gate 默认 false 保护（保护既有测试 + 向后兼容）。

- **初步实测（本计划起草时已完成的部分普查，执行时复核）**：grep `docs/requirements/` 全目录，关键词「预算硬拦截 / 开箱 / 默认开启 / 默认启用 / budget-check / budget-commitment-enabled / out-of-the-box」**零命中**——当前无任何需求真相源声明"开箱即用预算控制"部署契约。执行 Phase 1 须复核 + 扩展到部署文档（product-scope 部署/运维段、application.yaml、seed/demo 配置、README 运行段）。

- **剩余差距**：product-scope / 部署文档 / seed 配置是否声明默认预算控制启用——A1.2 baseline 仅核 L1 use-cases 文本，**未全量普查部署侧文档/配置**。本验证补全该缺口。

- **保护区域**：只读核对（grep + 读 JUnit + 读配置），不触及 ORM/会计过账逻辑/数据删除。属 roadmap 预授权类目。本验证**不实施修复**——若发现"开箱即用"隐含契约冲突则登记 finding 交 MR1（代码逻辑修复类预授权），不自行改 config 默认值或真相源。

## Goals

- 全集核对真相源与部署侧工件是否声明"开箱即用预算控制/承付"部署契约：① product-scope.md（全文件）② budget.md / 各域 owner doc §配置项 / §部署 ③ application.yaml / application-*.yaml / seed/demo 配置（含 `tests/` 测试启用 config）④ README / 部署运维文档。
- 对每个 config key（`erp-fin.budget-check-enabled` / `erp-fin.budget-commitment-enabled` / `erp-fin.commitment-release-on-return`）给出默认值证据 + 是否被任何"开箱启用"声明覆盖。
- 裁决 A1.2 §5.2 caveat ① 残留观察：若存在"开箱即用预算硬拦截"隐含契约 → 升级为 finding（P1/P2 视影响面）；否则维持接受（config 默认关闭属 ERP 通用启用范式，不构成需求分歧）。
- 产出验证报告 + §8 过程纪律自检；finding（若有）按 §7 裁决登记 arm-index。

## Non-Goals

- **不修改代码/ORM/api.xml/config 默认值/BizModel**（只读核对）。
- **不重新核实控制机制本身**（A1.2 §2.1/§5.2 已证实控制机制完整正确；本验证只核实"默认开启"部署契约前提）。
- **不实施修复**（修复经 MR1；config 默认值变更属代码逻辑修复预授权类目，但本验证不执行）。
- **不修改真相源**（§9 冻结）。

## Task Route

- Type: `verification or audit work`（部署契约核对 + 接受/分歧裁决）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 判据 + §7 衔接 + §8 自检 + §9 冻结）+ `docs/backlog/requirement-compliance-roadmap.md`（A4.1.4 行）+ `docs/audits/2026-08-02-1700-rc-ma1-a1-2-finance-f2-budget.md` §7 存疑点 1 + §5.2 caveat ①（输入）+ `docs/design/finance/budget.md §配置项`。
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定）。部署契约核对需多维度归类（真相源声明 / config 默认值 / 部署工件 / 测试启用覆盖 / 接受-or-分歧裁决）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。只读核对（grep + 读 JUnit + 读配置 + 引用 MA2/A1.2）。§8 自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - 部署契约真相源 + config 默认值全集核对

Status: completed
Targets: `docs/audits/2026-08-06-0847-rc-ma4-a4-1-4-budget-config-default-deployment-contract.md`（验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: A4.1 done；A1.2 done（§5.2 caveat ① 接受 + §7 存疑点 1 已落盘）

- [x] `Proof` 真相源声明普查：grep `docs/requirements/product-scope.md` 全文件 + `docs/design/finance/use-cases.md`（UC-FIN-11/13 全文）+ 各域 owner doc `§配置项`/`§部署`，关键词「开箱/默认开启/默认启用/默认启用预算/开箱即用/out-of-the-box/budget-check/budget-commitment/预算控制默认」。产出声明清单（有/无 + 逐字原文）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
      - 落盘：报告 §2.1 清单 ①（product-scope / use-cases / budget.md §配置项 / module-meta.yaml §optionalFeatures / business-module-metadata.md §2.1 / README / 部署运维文档全集）。全集零命中「开箱即用预算控制」声明；建筑层 `module-meta.yaml` 反向显式登记为 `defaultValue:false` 可选特性。
- [x] `Proof` 部署侧工件普查：① application.yaml / application-dev.yaml / application-test.yaml（含 `tests/` 测试 profile）中 config key 显式覆盖 ② seed/demo 数据（BudgetScenario.controlLevel 默认分布）③ README / 部署运维文档是否声明"开箱启用预算控制" ④ 测试套件中 `@NopTestConfig`/setup 是否显式 set `erp-fin.budget-check-enabled=true`（佐证"启用后行为正确"但不等同"开箱默认"）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
      - 落盘：报告 §2.2 清单 ②（全 20 生产 application.yaml 零覆盖 + 6 测试 profile 显式启用[佐证行为正确] + seed 零 BudgetScenario 默认方案 + README/部署运维文档零声明）。
- [x] `Proof` config 默认值逐消费点复核：对全集 config-gate 站点（2 finance 总开关方法 + 8 processor 消费点：`ErpFinBudgetControlBiz:226` / `ErpFinBudgetCommitmentBizModel:118` / `ErpPurOrderProcessor:178,198,223` / `ErpPurInvoiceProcessor:274` / `ErpPurPaymentProcessor:168` / `ErpSalOrderProcessor:312,333` / `ErpSalInvoiceProcessor:322`）记录默认值 + 是否被 application.yaml 覆盖。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
      - 落盘：报告 §2.3 清单 ③（全集 11 站点 = 2 finance + 9 processor，纠正 plan baseline "8 processor" 为实仓 9；全部 `Boolean.FALSE` + 无生产覆盖）。
- [x] `Decision` caveat ① 残留观察裁决（方法论 §2 判据）：若普查发现真相源/部署工件显式声明"开箱即用预算硬拦截"且 config 默认关闭与之冲突 → 按 §2 定级（P1①功能实质偏离验收标准 / P2②可用性契约未充分实现）；若**无**任何"开箱启用"声明 → 维持 A1.2 §5.2 caveat ① 接受（config 默认关闭属 ERP 通用启用范式，不构成需求分歧）。列明证据依据。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
      - 落盘：报告 §3 裁决 = **维持接受**（三清单零命中 + 建筑层反向登记 + A1.2 §5.2 三理由复核 CONFIRMED）。不升级 finding，不触发 MR0。

Exit Criteria:

- [x] 真相源声明 + 部署工件 + config 默认值三清单落盘（全集，无遗漏），每条有证据（文件:行 + 逐字原文/实测默认值）
- [x] caveat ① 残留观察裁决有明确结论（维持接受 / 升级 finding）+ 证据依据

### Phase 2 - finding 衔接 + §8 自检 + 报告定稿

Status: completed
Targets: `docs/audits/2026-08-06-0847-rc-ma4-a4-1-4-budget-config-default-deployment-contract.md`（定稿）；`docs/audits/arm-index.md`（若新 finding）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 三清单 + 裁决完成

- [x] `Add` 若裁决升级 → 按 §7 grep arm-index finance 预算/承付同域同控制点裁决「复用 or 新建」`P*-RC-xxx`，写入 arm-index MA4 分区；finding → MR1 双向可追溯。若维持接受 → 在报告登记"无新 finding，归 A1.2 §5.2 caveat ① 接受"。
      - Skill: none
      - 落盘：报告 §4 裁决 = 维持接受 → "无新 finding，归 A1.2 §5.2 caveat ① 接受"。报告 §4 列明与既有 finding（P1-RC-003 / P1-MA2-082 / P1-MA2-084 / P1-MA3-037 / P1-MA3-025）均不同控制点 / 不同维度，无新建 / 无 arm-index 写入。
- [x] `Proof` §8 过程纪律自检：运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明"无回归风险"）；closure-audit 独立性声明；与 arm-index 交叉去重声明（与 A1.2 §5.2 caveat ① / P1-MA2-084 config-gate / P1-MA2-082 release-on-return 的复用关系）。不以 checker 退出码 0 作为门控依据。
      - Skill: none
      - 落盘：报告 §5（§5.1 checker actual vs baseline 实测表[R1a-R2d，零生产代码变更结构上不变] + §5.2 closure-audit 独立性声明 + §5.3 与 arm-index 交叉去重声明）。

Exit Criteria:

- [x] 验证报告定稿（三清单 + 裁决 + finding 登记[若有] + §8 自检齐全）
- [x] 新 finding（若有）已写入 arm-index MA4 分区并有 grep 依据（本验证无新 finding，本条 N/A 满足）

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_02b732da1ffep7vQ8rXi7rNDd9，fresh session，未起草本计划）。逐项核验全 PASS：Deps（A4.1 done ✅ roadmap:126 / A1.2 done roadmap:41 / A4.1.4 todo Prereqs=A4.1 done roadmap:130 实测命中）、Baseline 逐项实测命中（isBudgetCheckEnabled ErpFinBudgetControlBiz:225-228 默认 FALSE / isCommitmentEnabled ErpFinBudgetCommitmentBizModel:117-119 默认 FALSE / CONFIG_*_ENABLED 常量 :379/:419 / 全 10 config-gate 站点默认 FALSE / `grep docs/requirements/` 预算开箱关键词 EXIT=1 零命中 / budget.md:310 §配置项 "默认关，保护既有 113 purchase 测试"）、只读审计正确（Goals/Non-Goals/Deferred 一致）、Closure Gates audit-only 删 build/test/lint 有据（plan guide:236）、反松弛合规（无禁用词）、item typing（Phase 1 Proof|Decision + Phase 2 Add|Proof）+ Skill 记录齐、finding→MR1 为 Non-Goal 一致、§去重 声明（A1.2 §5.2 caveat ① / P1-MA2-084 / P1-MA2-082 复用关系）准确。两条非阻塞建议（① config-gate 站点计数 8→"2 finance + 8 processor" 已采纳修正；② §6 报告段落适用性注记——A4.1.2 done 范本已用裁剪结构，按范本执行，非阻塞）。共识达成，转 active。

## Closure Gates

> 本计划为**只读运行时核对**（无代码/ORM/api.xml/view.xml/config 默认值/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 三清单完整性 + 裁决 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A4.1.4 验证报告三清单齐全（全集）+ 裁决 + finding（若有）登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §MA4 + §2 判据 + §9 冻结一致；与 A1.2 §7-1 + §5.2 caveat ① 一致
- [x] 已运行验证：三清单完整性 + §8 checker actual vs baseline 实测记录（本计划无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### config 默认值变更（若裁决升级为分歧）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是运行时核对，结果表面 = 验证报告 + 裁决 + finding 登记。config 默认值变更（若"开箱启用"契约冲突裁决为分歧）经 MR1（R1.0→RC-R1.n，代码逻辑修复预授权类目）实施。本验证闭环不阻塞于修复落地。
- Successor Required: yes（MR1 按本报告 finding 交叉引用展开修复行；若维持接受则无 successor）

## Closure

Status Note: 已完成（2026-08-06 执行者 pass）。两 Phase 全执行：Phase 1 三清单（真相源声明 + 部署工件 + config 默认值逐消费点）+ caveat ① 裁决 = 维持接受；Phase 2 finding 衔接（无新 finding）+ §8 过程纪律自检。验证报告定稿 `docs/audits/2026-08-06-0847-rc-ma4-a4-1-4-budget-config-default-deployment-contract.md`（`> Audit Status: closed`）。A1.2 §7 存疑点 1 在 MA4 链路的部署契约证据缺口已闭合：全集真相源 + 部署工件 + config 默认值零「开箱即用预算控制」声明 + 建筑层反向显式登记为可选特性 `defaultValue:false`，config 默认关闭属 ERP 通用启用范式不构成需求分歧。无 successor（裁决为接受故无 MR1 修复行）。roadmap A4.1.4 已更新为 `done ✅`。

Closure Audit Evidence:

- Auditor / Agent: 执行者 pass（主代理，2026-08-06）；独立结束审计由后续 mission driver 闭环节点的独立子代理（新会话）执行（报告 §5.2 独立性声明）
- 验证报告：`docs/audits/2026-08-06-0847-rc-ma4-a4-1-4-budget-config-default-deployment-contract.md`（`> Audit Status: closed`，9 段落齐全：§0 TL;DR + §1 输入存疑点原文 + §2 三清单 + §3 裁决 + §4 §去重 + §5 §8 自检 + §6 范围与非目标 + §7 MR0 登记 + §8 结论）
- 三清单全集证据：§2.1（7 真相源条目）+ §2.2（4 部署工件子清单）+ §2.3（11 config-gate 站点逐行实测）
- §8 checker actual vs baseline 实测表（R1a-R2d，零生产代码变更结构上不变）
- caveat ① 裁决证据：§3 三理由复核（控制机制完整 + L1 未强制默认开启 + 功能非缺失均 CONFIRMED）
- 无新 finding，无 MR0 触发，无 successor

Follow-up:

- 无（裁决为接受，Deferred But Adjudicated 的 config 默认值变更 successor 触发条件不满足）
