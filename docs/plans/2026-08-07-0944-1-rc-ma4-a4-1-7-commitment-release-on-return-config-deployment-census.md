# 2026-08-07-0944-1 rc-ma4-a4-1-7-commitment-release-on-return-config-deployment-census 承付 release-on-return config 默认 off 实际启用状态部署普查

> Plan Status: completed
> Last Reviewed: 2026-08-07
> Mission: requirement-compliance
> Work Item: A4.1.7（MA4 运行时行为验证 — A1.2 §7-4：承付 release-on-return 接入点 #4 config 默认 off 的实际启用状态部署普查）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A4.1.7；存疑点来源 `docs/audits/2026-08-02-1700-rc-ma1-a1-2-finance-f2-budget.md` §7 存疑点 4
> Related: `docs/plans/2026-08-07-0300-3-rc-ma4-a4-1-finance-runtime-expander.md`（A4.1 展开器 done，本行即其展开的实体行）、`docs/plans/2026-08-02-1600-2-rc-ma1-a1-2-finance-f2-budget-commitment.md`（A1.2 done，§5.2 caveat ① + §7 存疑点 4）、`docs/plans/2026-08-06-0847-1-rc-ma4-a4-1-4-budget-config-default-deployment-contract.md`（A4.1.4 done — 预算控制 config 默认关闭部署契约核对，本存疑点同型「config 默认值部署普查」范式）
> Audit: required

## Current Baseline

> 本计划是**运行时行为验证**（verification or audit work），结果表面 = 一份 A4.1.7 验证报告（落盘 `docs/audits/2026-08-07-0944-rc-ma4-a4-1-7-commitment-release-on-return-config-deployment-census.md`）+ 必要时 arm-index finding 登记。**不改代码/ORM/api.xml/真相源**（只读普查：grep config 消费点 + 部署工件 + 读既有 JUnit + 复用 MA2）。范式对齐 A4.1.4（已 done 的「config 默认关闭部署契约核对」同型工作项）。

- **存疑点原文**（A1.2 报告 §7 存疑点 4，`2026-08-02-1700-...-a1-2-budget.md` §7）：「承付 release-on-return（接入点 #4）config 默认 off 的实际启用状态」——L3 确认钩子已落地（`ErpPurReturnProcessor:281-297`）但 config 默认 false（`commitment-release-on-return` + 依赖 `budget-commitment-enabled`）。MA2 A2.16 `P1-MA2-082` 已登记（保守方向偏移：默认 off 致采购退货不释放承付，剩余未开票数量失去承付保护）。本切片不重复登记，交 MA4 按需确认部署是否启用——属既有 finding 行为证据，非新发现。

- **关联既有结论**：
  - A1.2 §5.2 caveat ①（config 默认关闭 = 接受，ERP 通用启用范式）+ §7 存疑点 1（A4.1.4 已闭合：全集真相源 + 部署工件零「开箱即用预算控制」声明）。
  - A4.1.4 done（`2026-08-06-0847-rc-ma4-a4-1-4-...`）：核对了 `budget-check-enabled` / `budget-commitment-enabled` 两总开关的「开箱即用」部署契约 → 维持接受。本存疑点是 A1.2 §7 遗留的**第三 config key**（`commitment-release-on-return`，接入点 #4 专用子开关），与 A4.1.4 同型但不同 config key、不同控制点（接入点 #4 release-on-return vs 总开关启用）。

- **需求契约（L1 权威）**：`docs/design/finance/use-cases.md` UC-FIN-13 断言③（`use-cases.md:257-258`）——「订单 CANCELLED 或发票接收 → 红冲 COMMITMENT」。**L1 逐字未列举「采购退货 → 红冲 COMMITMENT」**（接入点 #4 release-on-return 是 owner doc `budget.md §3 接入点 #4` 衍生扩展，非 L1 硬性验收标准）。L2（`budget.md §承付会计 §3 接入点 #4`）描述 release-on-return 作为衍生扩展（config-gated 默认 off + 全额释放语义 + 部分释放归 successor）。

- **实现现状（L3，实测锚点，本计划起草时核实）**：
  - config key 常量：`module-finance/erp-fin-service/.../service/ErpFinConstants.java:426` `CONFIG_BUDGET_COMMITMENT_RELEASE_ON_RETURN = "erp-fin.commitment-release-on-return"`。
  - 消费点：`module-purchase/erp-pur-service/.../service/processor/ErpPurReturnProcessor.java` `runCommitmentReleaseOnReturnHook:281-297`——`AppConfig.var(CONFIG_BUDGET_COMMITMENT_RELEASE_ON_RETURN, Boolean.FALSE):282-283` 默认 false 短路 `return:284`；启用后 `resolvePurchaseOrderCode:286-289` → `budgetCommitmentBiz.releaseIfPresent(...):291-292`（全额红冲，容错 catch NopException :293-296）。
  - **依赖链**：该钩子还需承付总开关 `budget-commitment-enabled` 为 true（`releaseIfPresent` 经 `ErpFinBudgetCommitmentBizModel.isCommitmentEnabled:117-119` 总开关 gate）；即「release-on-return 启用」需**两个** config 同时 true。
  - owner doc：`docs/design/finance/budget.md §3 接入点 #4`（config-gated 默认 false + 全额释放语义 + 部分释放 successor 与 P1-MA2-081 同 successor）。
  - 既有测试：`module-purchase/erp-pur-service/src/test/java/.../TestErpPurReturnCommitmentRelease.java`（`commitment-release-on-return=true` 场景1 退货审核红冲原 PO COMMITMENT 凭证——测试 profile 显式启用，佐证「启用后行为正确」）。

- **既有证据（复用输入）**：
  - A4.1.4 done：已证实「开箱即用预算控制/承付」部署契约全集零命中（真相源 + 全 20 生产 application.yaml + seed + README + 部署运维文档）。本验证复用其「总开关 deployment 契约」结论，**只补 release-on-return 子开关的 deployment 普查**（A4.1.4 未单列该子开关）。
  - MA2 `2026-07-28-1249-arm-ma2-budget-commitment-release.md`（A2.16）`P1-MA2-082`（release-on-return 保守方向偏移）已登记为 P1 watch-only / successor（按比例部分释放归 successor，与 P1-MA2-081 同 successor）。

- **初步实测（本计划起草时的部分普查，执行时复核全集）**：rg 全仓生产代码 `commitment-release-on-return` config-gate 站点仅 1 处（`ErpPurReturnProcessor:282-283`，单一消费点）；生产 application.yaml 系列零覆盖（待 Phase 1 全集复核）；测试 profile 显式启用（`return-commitment-test.yaml`）佐证行为正确。

- **剩余差距**：`commitment-release-on-return` 子开关的**实际启用状态**（生产 application.yaml / seed / 部署运维文档是否覆盖为 true）未全集普查。A4.1.4 已普查总开关但未单列该子开关，本验证补全该子开关 deployment 普查 + 裁决是否构成「开箱即用 release-on-return」隐含契约分歧。

- **保护区域**：只读普查（grep + 读 JUnit + 读配置 + 引用 A4.1.4/MA2），不触及 ORM/会计过账逻辑/数据删除。属 roadmap 预授权类目。本验证**不实施修复**——`P1-MA2-082` 已登记（保守方向偏移 + 按比例部分释放 successor），本验证仅核实 deployment 实际启用状态为其补行为证据；不自行改 config 默认值或真相源。

## Goals

- 全集普查 `erp-fin.commitment-release-on-return` config 的实际启用状态：① 消费点全集（grep 生产代码 `AppConfig.var(...CONFIG_BUDGET_COMMITMENT_RELEASE_ON_RETURN...)`）② 生产 application.yaml / application-*.yaml 是否覆盖为 true ③ seed/demo 配置 ④ README / 部署运维文档是否声明「开箱启用 release-on-return」。
- 复用 A4.1.4 结论：总开关 `budget-commitment-enabled` deployment 契约已闭合（零「开箱即用」声明）——本验证裁决 release-on-return 子开关是否独立构成 deployment 分歧。
- 裁决：若 deployment 普查发现任何「开箱启用 release-on-return」声明且与 config 默认 false 冲突 → 按 §2 定级（与 `P1-MA2-082` 同控制点则复用交叉注记，不同控制点才新建）；若无任何「开箱启用」声明 → 维持 A1.2 §5.2 caveat ① + `P1-MA2-082` 既有登记（保守方向偏移 successor），不升级。
- 产出验证报告 + §8 过程纪律自检；finding（若有）按 §7 裁决登记 arm-index。

## Non-Goals

- **不重新核实 release-on-return 钩子机制本身**（A1.2 §4.1 + MA2 A2.16 已证钩子落地 + 全额释放语义 + 部分释放 successor；本验证只核实 deployment 实际启用状态）。
- **不重新核实总开关 deployment 契约**（A4.1.4 已闭合；本验证复用其结论，只补子开关）。
- **不修改代码/ORM/api.xml/config 默认值/BizModel**（只读普查）。
- **不实施修复**（修复经 MR1；`P1-MA2-082` 已登记 successor，本验证不执行）。
- **不修改真相源**（§9 冻结）。
- **不展开其他 A1.2 §7 存疑点**（§7-1/2/3 已由 A4.1.4/5/6 闭合；本验证仅 §7-4）。

## Task Route

- Type: `verification or audit work`（deployment 普查 + 接受/分歧裁决）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 判据 + §7 衔接 + §8 自检 + §9 冻结 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A4.1.7 行）+ `docs/audits/2026-08-02-1700-rc-ma1-a1-2-finance-f2-budget.md` §7 存疑点 4 + §5.2 caveat ①（输入）+ `docs/design/finance/budget.md §3 接入点 #4` + `docs/plans/2026-08-06-0847-1-rc-ma4-a4-1-4-budget-config-default-deployment-contract.md`（A4.1.4 范本 + 总开关 deployment 结论复用）。
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定）。deployment 普查需多维度归类（消费点 / 生产配置覆盖 / seed / 部署文档声明 / 与 A4.1.4 总开关结论的复用关系 / 接受-or-分歧裁决）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。只读普查（grep + 读 JUnit + 读配置 + 引用 A4.1.4/MA2/A1.2）。§8 自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - release-on-return config 消费点 + deployment 启用状态全集普查

Status: completed
Targets: `docs/audits/2026-08-07-0944-rc-ma4-a4-1-7-commitment-release-on-return-config-deployment-census.md`（验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: A4.1 done（展开器已追加 A4.1.7 行）；A1.2 done（§7 存疑点 4 已落盘）；A4.1.4 done（总开关 deployment 结论可复用）

- [x] `Proof` 消费点全集普查：rg 全仓生产代码（排除 test）`CONFIG_BUDGET_COMMITMENT_RELEASE_ON_RETURN` / `commitment-release-on-return` 的 `AppConfig.var(...)` 站点，产出消费点清单（file:line + 默认值 + 是否被同站点其他 config gate 串联）。禁止抽样；消费点 < 1 不是异常（若零消费点则 config key 为死代码，本身即 finding）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
      - 落盘：报告 §2.1 清单 ①（消费点全集 = 1 活跃消费点 `ErpPurReturnProcessor.runCommitmentReleaseOnReturnHook:283` 默认 FALSE + 1 常量声明 `ErpFinConstants:426` + 1 javadoc + 1 接线点 `ErpPurReturnApproveProcessor.doApprove:60` + 双 config gate 依赖链 `isCommitmentEnabled:117-119`）。与 plan baseline「rg 全仓生产代码 config-gate 站点仅 1 处」逐字吻合；非死代码。
- [x] `Proof` deployment 工件普查：① 全集生产 application.yaml / application-*.yaml（非 test profile）是否显式 set `erp-fin.commitment-release-on-return=true` ② seed/demo 数据 ③ README / 部署运维文档是否声明「开箱启用 release-on-return」/「退货自动释放承付」④ 测试 profile（`return-commitment-test.yaml` 等）显式启用佐证「启用后行为正确」（不等同「开箱默认」）。产出逐条证据（文件:行 + 逐字原文/实测值）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
      - 落盘：报告 §2.2 清单 ②（全 20 生产 application.yaml 零覆盖 + 测试 profile `return-commitment-test.yaml:5` 显式启用[佐证行为正确] + seed 零 config 覆盖 + README/部署运维文档/product-scope 零声明 + owner doc `budget.md §配置项:313`+`§3 接入点 #4:258` 明确记载默认 OFF + 建筑层 `module-meta.yaml` 父特性 `budget-commitment` defaultValue:false[子开关继承 opt-in 分类，未单列]）。
- [x] `Proof` 复用 A4.1.4 总开关结论：A4.1.4 已证总开关 `budget-commitment-enabled` deployment 全集零「开箱即用」声明——release-on-return 子开关依赖总开关（双 config gate），故总开关默认 false 时子开关启用与否均不生效。在报告声明此依赖链 + 复用 A4.1.4 结论。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
      - 落盘：报告 §2.3 清单 ③（双 config gate 分层一致性：总开关默认 false 时子开关启用与否均不生效；A4.1.4 总开关 deployment 结论自动覆盖子开关；本普查复核子开关自身 deployment 亦零覆盖，分层一致）。
- [x] `Decision` §7-4 存疑点裁决（方法论 §2 判据 + §去重协议）：若 deployment 普查发现「开箱启用 release-on-return」声明且与 config 默认 false 冲突 → grep arm-index 同域同控制点（`P1-MA2-082` release-on-return 保守方向偏移）裁决「复用 or 新建」；若**无**任何「开箱启用」声明 → 维持 A1.2 §5.2 caveat ① 接受 + `P1-MA2-082` 既有 successor 登记（保守方向偏移，部分释放归 successor），不升级。裁决须列明证据依据 + 与 A4.1.4 总开关结论的分层一致性。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
      - 落盘：报告 §3 裁决 = **维持接受**（三清单零命中 + 双 config gate 依赖链 + 复用 A4.1.4 总开关结论分层一致 + A1.2 §5.2 caveat ① 三理由复核 CONFIRMED）。config-default-false「保守方向偏移」归 `P1-MA2-082` 既有 successor（resolved R1.27 钩子已落地；按比例部分释放 successor 触发条件未满足）。不升级 finding，不触发 MR0。

Exit Criteria:

- [x] 消费点清单 + deployment 工件普查清单落盘（全集，无遗漏），每条有证据（文件:行 + 逐字原文/实测值）
- [x] §7-4 裁决有明确结论（维持接受 / 升级 finding）+ 证据依据 + 与 A4.1.4 总开关结论分层一致

### Phase 2 - finding 衔接 + §8 自检 + 报告定稿

Status: completed
Targets: `docs/audits/2026-08-07-0944-rc-ma4-a4-1-7-commitment-release-on-return-config-deployment-census.md`（定稿）；`docs/audits/arm-index.md`（若新 finding / 交叉注记）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 普查 + 裁决完成

- [x] `Add` 若裁决升级 → 按 §7 grep arm-index finance 承付 release-on-return 同域同控制点（`P1-MA2-082`）裁决「复用 or 新建」`P*-RC-xxx`，写入 arm-index MA4 分区；finding → MR1 双向可追溯。若维持接受 → 在报告登记「无新 finding，归 A1.2 §5.2 caveat ① 接受 + `P1-MA2-082` 既有 successor」；若与 `P1-MA2-082` 同控制点则在 arm-index `P1-MA2-082` 行追加 RC 交叉注记（deployment 普查补行为证据）。
      - Skill: none
      - 落盘：报告 §4 裁决 = 维持接受 → 「无新 finding，归 A1.2 §5.2 caveat ① 接受 + `P1-MA2-082` 既有 successor」。§7「复用 or 新增」裁决 = 与 `P1-MA2-082` **同控制点**（release-on-return config-default-false deployment 启用状态面）→ **复用既有 finding ID**，不新建 `P*-RC-xxx`；在 arm-index `P1-MA2-082` 行（`arm-index.md:530`）追加 `【RC A4.1.7 普查 2026-08-07】` RC 交叉注记（deployment 实际启用状态 = 关闭，为保守方向偏移补行为证据）。
- [x] `Proof` §8 过程纪律自检：运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明「无回归风险」）；closure-audit 独立性声明；与 arm-index 交叉去重声明（与 A4.1.4 总开关 deployment / `P1-MA2-082` release-on-return / `P1-MA2-081` 部分开票释放的复用关系）。不以 checker 退出码 0 作为门控依据。
      - Skill: none
      - 落盘：报告 §5（§5.1 checker actual vs baseline 实测表[R1a-R2d 全匹配 baseline + R3+ checker 既有早退结构上不变，零生产代码变更] + §5.2 closure-audit 独立性声明 + §5.3 与 arm-index 交叉去重声明[P1-MA2-082 同控制点复用 + P1-RC-003/P1-MA2-081/P1-MA2-084/P1-MA2-083/P1-MA3-025 不同控制点/维度]）。

Exit Criteria:

- [x] 验证报告定稿（消费点 + deployment 普查清单 + 裁决 + finding/交叉注记[若有] + §8 自检齐全）
- [x] 新 finding/交叉注记（若有）已写入 arm-index MA4 分区并有 grep 依据（本验证无新 finding；同控制点 `P1-MA2-082` 追加 RC 交叉注记已写入 arm-index `arm-index.md:530`）

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_02b3ca505ffeVjvnhQ1KFIF1mz，fresh session，未起草本计划）。逐项核验 A-I 全 PASS：Deps（A4.1 展开器 completed + Closure Gates 全 [x] + 独立 closure audit ses_02c862159 PASS / A1.2 done roadmap:41 / A4.1.4 done roadmap:130）、单结果表面（一份验证报告，Non-Goals 显式排除 §7-1/2/3，与 A4.1.4 总开关 scope 区分）、Baseline 逐项实测命中（ErpFinConstants.java:426 config key 逐字 / ErpPurReturnProcessor:281-297 + AppConfig.var FALSE :282-283 / 双 config gate 依赖链 isCommitmentEnabled:117-119 / §7-4 存疑点逐字忠实引用 / P1-MA2-082 arm-index:530 真实登记 / TestErpPurReturnCommitmentRelease.java 存在）、只读审计正确（预授权类目无 ask-first）、反松弛合规、item typing（Phase 1 Proof|Decision + Phase 2 Add|Proof）+ Skill 记录齐、Closure Gates audit-only 删 build/test 有据（guide:236）、Q4 路由正确（缺陷→MR1，禁方案 B）、完整枚举纪律（消费点全集禁止抽样，<1 即 finding）。无阻塞。非阻塞观察：plan:32 称 P1-MA2-082 为「P1 watch-only/successor」而 arm-index:530 显示 `resolved (R1.27 done)`——与 A1.2 报告 + 展开器表述一致（hook 实现已 resolve release-path 缺口，config-default-false「保守方向偏移」是本 MA4普查验证的行为面），characterization 可辩护，无需修订。共识达成，转 active。

## Closure Gates

> 本计划为**只读运行时普查**（无代码/ORM/api.xml/view.xml/config 默认值/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 消费点 + deployment 普查清单完整性 + 裁决 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A4.1.7 验证报告消费点 + deployment 普查清单齐全（全集）+ 裁决 + finding/交叉注记（若有）登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §MA4 + §2 判据 + §去重协议 + §9 冻结一致；与 A1.2 §7-4 + §5.2 caveat ① + A4.1.4 总开关结论分层一致
- [x] 已运行验证：消费点 + deployment 普查完整性 + §8 checker actual vs baseline 实测记录（本计划无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### release-on-return 按比例部分释放（P1-MA2-082 successor）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是 deployment 普查，结果表面 = 验证报告 + 裁决 + finding/交叉注记登记。`P1-MA2-082`（release-on-return 保守方向偏移：默认 off + 全额释放语义，部分释放归 successor）已由 MA2 A2.16 登记为 P1 watch-only / successor（与 `P1-MA2-081` 部分开票全额释放同 successor）。config 默认值变更 / 按比例部分释放实现经 MR1（R1.0→RC-R1.n）实施。本验证闭环不阻塞于修复落地。
- Successor Required: yes（MR1 按本报告 finding 交叉引用 / `P1-MA2-082` successor 展开；若维持接受则无新 successor，归既有 `P1-MA2-082`）

## Closure

Status Note: 已完成（2026-08-07 执行者 pass）。两 Phase 全执行：Phase 1 消费点全集普查（唯一活跃消费点 = 1 `ErpPurReturnProcessor:283` 默认 FALSE，双 config gate 依赖链）+ deployment 工件普查（全 20 生产 application.yaml 零覆盖 + owner doc 明确记载默认 OFF + 建筑层父特性 defaultValue:false）+ 复用 A4.1.4 总开关结论（双 config gate 分层一致）+ §7-4 裁决 = 维持接受；Phase 2 finding 衔接（同控制点 `P1-MA2-082` 复用，追加 RC 交叉注记补 deployment 行为证据）+ §8 过程纪律自检。验证报告定稿 `docs/audits/2026-08-07-0944-rc-ma4-a4-1-7-commitment-release-on-return-config-deployment-census.md`（`> Audit Status: closed`）。A1.2 §7 存疑点 4 在 MA4 链路的 deployment 实际启用状态证据缺口已闭合：config key `erp-fin.commitment-release-on-return` 全仓生产代码唯一消费点默认 FALSE + deployment 工件全集零「开箱启用 release-on-return」声明 + 双 config gate 依赖承付总开关（总开关默认 false 时子开关启用与否均不生效），config 默认关闭属 ERP 通用启用范式不构成需求分歧。config-default-false「保守方向偏移」归 `P1-MA2-082` 既有 successor（resolved R1.27 钩子已落地；按比例部分释放 successor 触发条件未满足）。roadmap A4.1.7 已更新为 `done ✅`。

Closure Audit Evidence:

- Auditor / Agent: 执行者 pass（主代理，2026-08-07）；独立结束审计由后续 mission driver 闭环节点的独立子代理（新会话）执行（报告 §5.2 独立性声明）
- 验证报告：`docs/audits/2026-08-07-0944-rc-ma4-a4-1-7-commitment-release-on-return-config-deployment-census.md`（`> Audit Status: closed`，9 段落齐全：§0 TL;DR + §1 输入存疑点原文 + L1/L3 锚点 + §2 三清单[消费点 + deployment + 复用 A4.1.4] + §3 §7-4 裁决 + §4 §去重 + §5 §8 自检 + §6 范围与非目标 + §7 MR0 登记 + §8 结论）
- 消费点全集证据：§2.1 清单 ①（1 活跃消费点 + 1 常量声明 + 1 javadoc + 1 接线点 + 双 config gate 依赖链）
- deployment 工件全集证据：§2.2 清单 ②（4 子清单：生产 application.yaml[20 文件零覆盖] + 测试 profile[佐证行为正确] + seed[零 config 覆盖] + README/部署运维文档/owner doc/建筑层[零声明 + 反向登记默认 OFF]）
- 复用 A4.1.4 总开关结论：§2.3 清单 ③（双 config gate 分层一致性裁决）
- §8 checker actual vs baseline 实测表（R1a-R2d 全匹配 baseline + R3+ checker 既有早退结构上不变，零生产代码变更）
- §7-4 裁决证据：§3 三理由复核（钩子机制完整 + L1 未强制默认开启 + 功能非缺失 + P1-MA2-082 successor 均 CONFIRMED）
- 无新 finding，无 MR0 触发；同控制点 `P1-MA2-082` 追加 RC 交叉注记（arm-index `arm-index.md:530`）补 deployment 行为证据

Follow-up:

- 无（裁决为接受，config-default-false「保守方向偏移」归 `P1-MA2-082` 既有 successor；按比例部分释放 successor 触发条件未满足[多组织预算硬约束启用 + 部分开票/部分退货为常态业务路径]，Deferred But Adjudicated 的 successor 触发条件不满足）
