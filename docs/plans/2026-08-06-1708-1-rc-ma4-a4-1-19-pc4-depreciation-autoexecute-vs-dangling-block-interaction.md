# 2026-08-06-1708-1 rc-ma4-a4-1-19-pc4-depreciation-autoexecute-vs-dangling-block-interaction PC-4 资产折旧 auto-execute 与悬挂阻断交互运行时行为评估

> Plan Status: completed
> Last Reviewed: 2026-08-06
> Mission: requirement-compliance
> Work Item: A4.1.19（MA4 运行时行为验证 — A1.6 §7 存疑点 2：UC-FIN-06 PC-4 资产未折旧——`runDepreciation` G3 容错跳过[impl 未就绪]与 `findUnresolvedDepreciationSchedules` 悬挂阻断的交互：assets 域部署但折旧因配置错误失败时 rethrow 阻断 vs 悬挂扫描是否双重报告/单一路径有效阻断）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A4.1.19；存疑点来源 `docs/audits/2026-08-02-2100-rc-ma1-a1-6-finance-f6-period-close.md` §7 存疑点 2
> Related: `docs/plans/2026-08-07-0300-3-rc-ma4-a4-1-finance-runtime-expander.md`（A4.1 展开器 done）、`docs/plans/2026-08-06-1517-3-rc-ma4-a4-1-18-pc3-arap-reminder-nonblocking-runtime-behavior.md`（A4.1.18 done — period-close 同切片 A1.6 config-gated 路径评估同型范式）、`docs/audits/2026-08-02-2100-rc-ma1-a1-6-finance-f6-period-close.md`（A1.6 报告 §2.4 PC-4 间接实现 + §5.2 PC-4 接受[行为达成] + §7 存疑点 2 + §3.7 测试覆盖缺口[跨域悬挂阻断无单测]）、`docs/design/finance/period-close.md §期末结账步骤 8步 :60-111`（L2 设计参考：步骤3 折旧计提 auto-execute）
> Audit: required

## Current Baseline

> 本计划是**运行时行为验证**（verification or audit work），结果表面 = 一份 A4.1.19 验证报告（落盘 `docs/audits/2026-08-06-1708-rc-ma4-a4-1-19-pc4-depreciation-autoexecute-vs-dangling-block-interaction.md`）+ 必要时 arm-index finding 注记更新。**不改代码/ORM/api.xml/真相源**（只读评估：读 `runDepreciation` G3 分支逻辑 + `findUnresolvedDepreciationSchedules` 悬挂扫描 + `ClosePeriodProcessor` 阻断门控交互 + 既有测试覆盖普查）。范式对齐 A4.1.18（done — period-close config-gated 路径评估同型工作项）。

- **存疑点原文**（A1.6 报告 §7 存疑点 2，`2026-08-02-2100-...-a1-6-period-close.md` §7）：「PC-4 资产折旧 auto-execute + 悬挂阻断交互——`runDepreciation` G3 容错跳过[impl 未就绪]与 `findUnresolvedDepreciationSchedules` 悬挂阻断的交互：若 assets 域部署但折旧因配置错误失败，rethrow 阻断 vs 悬挂扫描是否双重报告」。触发条件 = assets 域部署 + 折旧配置错误（如 `ERR_DEPRECIATION_RATE_MISSING`）。交 A4.1 运行时探针（确认 PC-4 验收标准「资产未折旧 → 拒绝」在 auto-execute + 悬挂双路径下运行时有效阻断且无双报/无漏报）。

- **关联既有 finding**：
  - **PC-4**（A1.6 §5.2 裁决：接受[行为达成]）：UC-FIN-06 PC-4「资产未折旧 → 拒绝」经**间接实现**（auto-execute 步骤 `runDepreciation` + 悬挂兜底阻断 `findUnresolvedDepreciationSchedules`）达成，无显式 preCheck 字段。本验证评估该双路径在配置错误场景下的运行时交互是否真正有效阻断（rethrow 优先 vs 悬挂扫描后置）。
  - **P1-MA4-004**（arm-index，resolved R1.16）：跨域异常吞噬修复——`runDepreciation:195-198` catch NopException → LOG.error → **rethrow**（G3 配置错误/真实故障阻断结账，impl 未就绪 try/catch 容错跳过）。本验证只评 rethrow 与悬挂扫描的**运行时交互**（执行时序 + 双报可能性），不重新核实 P1-MA4-004 修复本身（A1.6 §6.2 + A4.1b 已证 resolved）。

- **需求契约（L1 权威）**：`docs/design/finance/use-cases.md:110`（UC-FIN-06 heading）/ `:117`（PC-4 逐字）「若 资产未折旧 → 拒绝」。L2 `period-close.md §期末结账步骤 8步 :60-111`（步骤3 折旧计提 auto-execute，config-gated）。

- **实现现状（L3，实测锚点，本计划起草时 live repo 核实，HEAD 经独立子代理实测，全在 module-finance）**：
  - `runDepreciation` G3 双分支（`ErpFinAccountingPeriodProcessor.java`）：
    - **Branch A — impl 未就绪容错跳过（:184-191）**：`bizObjectManager.getBizObject(ErpAstDepreciationSchedule...)` catch generic `Exception` → `LOG.warn` → `return`（不阻断）。触发 = assets 域未部署/impl 未注册。
    - **Branch B — 配置错误/真实故障 rethrow（:195-198）**：`executeBatchDepreciation` catch `NopException` → `LOG.error` → `throw e`（阻断结账）。触发 = assets 部署但折旧配置错误（如折旧率缺失）。
    - config gate（:180-182）：`isAutoDepreciationOnClose()` 默认 true（:638-641），关闭则直接 return。
  - 入口：`closeAssetModule:151-155`（`runDepreciation` → `advanceModule(AST)`），run **先于** AST 模块状态推进。
  - closePeriod 编排（`ErpFinAccountingPeriodClosePeriodProcessor.doClosePeriod`）：`:69-71` 按序 AR→AP→INV→**AST**→GL 关账；`:50` preCheck（含悬挂扫描）；`:59-63` `if (!isAutoPostOnClose() && report.hasIssues()) → ERR_PRE_CHECK_BLOCKED`（悬挂阻断仅 auto-post-on-close=false 时生效）。
  - 悬挂兜底扫描 `findUnresolvedDepreciationSchedules:506-527`（filter `posted=FALSE AND status=EXECUTED AND period=code` :510-517；key 形如 `depreciation:<assetId>#<periodCode>` :519-520；try/catch 安全跳过 :522-525 若 assets entity 未注册）。经 `findUnresolvedPostingExceptionKeys:471-477`（三层：finance exceptions + depreciation schedules + landed costs）→ preCheck `unresolvedPostingExceptionKeys` → `hasIssues()`。
  - **交互时序（本存疑点核心）**：closePeriod 流程 = `preCheck`（含悬挂扫描，扫**上一周期遗留**的悬挂）→ 阻断门控 → `doClosePeriod`（含 `closeAssetModule → runDepreciation`，若失败 rethrow **立即**回滚事务）。即 preCheck 悬挂扫描在 runDepreciation **之前**执行（扫历史悬挂），runDepreciation rethrow 在 closePeriod 事务内**之后**（若失败整个事务回滚，悬挂扫描结果不落地）。

- **既有证据（复用输入）**：
  - A1.6 §2.4 + §5.2：PC-4 间接实现已静态确认（auto-execute + 悬挂阻断双路径）；接受[行为达成]。本验证补「双路径运行时交互[执行时序 + 配置错误场景 + 双报可能性]」差异。
  - A2.3 period-close E2E（`2026-07-27-1949-arm-ma2-period-close-e2e.md`）：period-close 主链路行为 PASS（不含跨域悬挂/折旧 rethrow 单测覆盖）。A4.1b（`2026-07-28-2130-arm-ma4-finance-...-code-quality.md`）：P1-MA4-004 rethrow resolved R1.16。
  - A1.6 §3.7 测试覆盖缺口（实测确认）：**「折旧/成本重算失败传播（G3 rethrow）无单测」+「跨域悬挂阻断（assets 折旧 posted=false 悬挂）无单测」**。本验证确认该缺口对 PC-4 运行时有效阻断的实际风险面。

- **剩余差距**：PC-4 的 auto-execute（runDepreciation rethrow）与悬挂兜底（findUnresolvedDepreciationSchedules）双路径的**运行时交互**未验证——①配置错误场景下 rethrow 是否在悬挂扫描前优先阻断（事务回滚）；②是否存在 rethrow 失败被悬挂扫描兜底[或反之]的双报/漏报窗口；③既有零测试覆盖对 PC-4 运行时有效阻断的实际风险。本验证闭合 PC-4「行为达成」的运行时确认 + P1-MA4-004 rethrow resolved 的交互面确认。

- **保护区域**：只读评估（读 runDepreciation/悬挂扫描/ClosePeriod 门控交互 + 既有测试普查），不触及 ORM/会计过账逻辑**修改**。属 roadmap 预授权类目（只读评估）。本验证**不实施修复**（若发现双报/漏报缺口，登记 finding 归 MR1，BizModel 代码逻辑修复预授权类目不触发 §5 ask-first；或测试覆盖缺口归 MR1 测试补充）。

## Goals

- runDepreciation G3 双分支运行时触发面评估：核验 Branch A（impl 未就绪 catch Exception → warn skip :184-191）vs Branch B（配置错误 catch NopException → LOG.error → rethrow :195-198）的触发条件 + 配置门控（`auto-depreciation-on-close` 默认 true :180-182）——确认各分支在 assets 部署/未部署/配置错误场景下的运行时行为。
- auto-execute rethrow 与悬挂兜底阻断的**执行时序与交互**评估（本存疑点核心）：核验 closePeriod 流程 `preCheck`（含 `findUnresolvedDepreciationSchedules` 悬挂扫描历史遗留 :50）→ `doClosePeriod`（含 `closeAssetModule → runDepreciation` rethrow :69-70）的时序——确认①配置错误场景 rethrow 在事务内立即阻断（事务回滚，悬挂扫描结果不落地）；②悬挂扫描只覆盖**历史遗留** EXECUTED+posted=false（preCheck 在 runDepreciation 前），不覆盖**本次** runDepreciation 产生的悬挂（本次失败经 rethrow 回滚，无悬挂落地）；③无双报（rethrow 优先回滚 vs 悬挂扫描 hasIssues 门控 auto-post-on-close=false）也无漏报（auto-post-on-close=true 时 rethrow 仍阻断，悬挂扫描失效但不致漏——因 rethrow 独立于 auto-post-on-close）。
- 悬挂兜底扫描 `findUnresolvedDepreciationSchedules` 过滤逻辑核验：核验 `posted=FALSE AND status=EXECUTED AND period=code` 过滤（:510-517）是否精确覆盖 PC-4「资产未折旧」语义（EXECUTED=已计提但 posted=false 未过账悬挂；REVERSED/CANCELLED 合法不阻断）+ try/catch 安全跳过（:522-525，assets entity 未注册场景）。
- auto-post-on-close 配置对 PC-4 双路径阻断的交互评估：核验 `ClosePeriodProcessor:59-63`（`!isAutoPostOnClose() && hasIssues()`）——悬挂扫描门控**仅** auto-post-on-close=false 生效；runDepreciation rethrow **独立**于 auto-post-on-close（rethrow 无条件回滚）。确认 auto-post-on-close=true 时 PC-4 阻断**仅**依赖 rethrow（悬挂扫描失效但 rethrow 仍阻断 → 无漏报）。
- 既有测试覆盖边界普查：grep `TestErpFinDepreciationIntegration`（覆盖 Branch A impl 未就绪跳过）+ runDepreciation rethrow 路径测试（NopException 注入）+ findUnresolvedDepreciationSchedules 悬挂扫描测试 全集，产出测试覆盖边界清单 + 标注 rethrow 路径缺口 + 悬挂扫描缺口（A1.6 §3.7 已记）。
- 对齐 UC-FIN-06 PC-4「资产未折旧 → 拒绝」+ §5.2 PC-4 接受[行为达成]给出运行时确认结论：①若 rethrow 优先阻断 + 悬挂扫描覆盖历史遗留 + auto-post-on-close=true 时 rethrow 独立阻断无漏报 → PC-4 维持接受[运行时确认行为达成]，测试覆盖缺口归 MR1 测试补充（不降级 PC-4 符合性）；②若发现某路径漏报（如 rethrow 被吞噬致悬挂扫描又失效的窗口）→ 登记 finding（watch-only 或 MR1）。
- 产出验证报告 + §8 过程纪律自检。

## Non-Goals

- **不修复 PC-4 双路径交互**（若发现双报/漏报，登记 finding 归 MR1；BizModel 修复或测试补充预授权类目）。
- **不修改代码/ORM/api.xml/BizModel/真相源**（只读评估）。
- **不重新核实 P1-MA4-004 rethrow 修复本身**（A1.6 §6.2 + A4.1b 已证 resolved R1.16；本验证只评 rethrow 与悬挂扫描的运行时交互）。
- **不重新核实 UC-FIN-06 全部验收标准**（A1.6 §5 已判 PC-4 接受 + PC-3 P2 + 其余接受；本验证只评 PC-4 双路径交互差异）。
- **不展开 A1.6 §7-3/§7-4**（A4.1.20 RC-9 审计缺失 / A4.1.21 年末反结账边界）。
- **不部署真实 assets 域跑折旧配置错误场景**（只读交互逻辑推理 + 既有测试 + 执行时序分析；真实折旧注入测试属 MR1 测试补充范围，非本验证范围）。

## Task Route

- Type: `verification or audit work`（PC-4 双路径交互运行时评估 + PC-4 符合性运行时确认）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 判据 + §4 Q1 真相源层级 + §7 衔接 + §8 自检 + §9 冻结 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A4.1.19 行）+ `docs/audits/2026-08-02-2100-rc-ma1-a1-6-finance-f6-period-close.md` §7 存疑点 2 + §2.4 PC-4 + §5.2 PC-4 接受 + §6.2 P1-MA4-004 resolved（输入）+ `docs/design/finance/period-close.md §期末结账步骤 8步 :60-111`（L2 设计参考）。
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定）。PC-4 双路径交互评估需多维度归类（runDepreciation G3 双分支 / 悬挂扫描过滤 + 时序 / ClosePeriod 门控交互 / auto-post-on-close 配置依赖 / rethrow 独立性 / 既有测试覆盖边界 / 双报-or-漏报判定 / PC-4 符合性维持-or-登记 finding）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。只读评估（读 runDepreciation/悬挂扫描/ClosePeriod 门控交互 + 既有测试普查 + 执行时序推理）。§8 自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - PC-4 auto-execute rethrow 与悬挂兜底阻断交互运行时评估

Status: completed
Targets: `docs/audits/2026-08-06-1708-rc-ma4-a4-1-19-pc4-depreciation-autoexecute-vs-dangling-block-interaction.md`（验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: A4.1 done（展开器已追加 A4.1.19 行）；A1.6 done（§7 存疑点 2 已落盘 + §2.4 PC-4 间接实现 + §5.2 PC-4 接受 + §6.2 P1-MA4-004 resolved + §3.7 测试覆盖缺口）

- [x] `Proof` runDepreciation G3 双分支核验：给出 `ErpFinAccountingPeriodProcessor.runDepreciation` 证据（file:line）——Branch A impl 未就绪 catch Exception → LOG.warn → return（:184-191）+ Branch B 配置错误 catch NopException → LOG.error → throw e（:195-198）+ config gate `isAutoDepreciationOnClose` 默认 true（:180-182/:638-641）。证实各分支触发条件（assets 未部署/部署/配置错误）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
      - Evidence: 验证报告 §2.1（`ErpFinAccountingPeriodProcessor.java:180-199` + `:638-641` live 实测，三行触发条件 + 阻断/容错矩阵落盘）
- [x] `Proof` closePeriod 执行时序与 rethrow/悬挂交互核验（本存疑点核心）：给出 `ErpFinAccountingPeriodClosePeriodProcessor.doClosePeriod` 时序证据——`:50` preCheck（含 `findUnresolvedPostingExceptionKeys` → `findUnresolvedDepreciationSchedules` 扫**历史遗留**悬挂）**先于** `:67-71` 模块按序关账（AR→AP→INV→AST→GL），其中 `:70 closeAssetModule` → runDepreciation（**本次**折旧，若失败 rethrow 回滚事务）。证实①悬挂扫描覆盖历史遗留 EXECUTED+posted=false，不覆盖本次 runDepreciation 产生悬挂（本次失败经 rethrow 回滚无悬挂落地）；②rethrow 在事务内立即阻断（事务回滚，preCheck 悬挂扫描结果不落地）；③无双报无漏报。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
      - Evidence: 验证报告 §2.2 + §4.4（`ErpFinAccountingPeriodClosePeriodProcessor.java:46-87` 11 步时序表 + 时序流图 + 4 项关键时序断言全部证实）
- [x] `Proof` 悬挂兜底扫描过滤逻辑核验：给出 `findUnresolvedDepreciationSchedules:506-527` 过滤证据（file:line）——`posted=FALSE AND status=EXECUTED AND period=code`（:510-517）精确覆盖 PC-4「资产未折旧」语义（EXECUTED=已计提 posted=false 悬挂；REVERSED/CANCELLED 合法不阻断）+ try/catch 安全跳过（:522-525 assets entity 未注册）。证实悬挂扫描语义正确。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
      - Evidence: 验证报告 §2.3（`ErpFinAccountingPeriodProcessor.java:506-527` 代码块 + 4 行过滤条件语义对应表）
- [x] `Proof` auto-post-on-close 配置对 PC-4 双路径阻断的交互评估：核验 `ClosePeriodProcessor:59-63`（`!isAutoPostOnClose() && report.hasIssues()`）——悬挂扫描门控**仅** auto-post-on-close=false 生效；runDepreciation rethrow **独立**于 auto-post-on-close（rethrow 在 doClosePeriod 事务内无条件回滚）。确认 auto-post-on-close=true 时 PC-4 阻断仅依赖 rethrow（悬挂扫描失效但 rethrow 仍阻断 → 无漏报）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
      - Evidence: 验证报告 §2.4（`ClosePeriodProcessor:59-63` + `:633-636` + 双路径配置轴交互矩阵 2×2 + 关键断言「rethrow 不读 auto-post-on-close config」）
- [x] `Proof` 既有测试覆盖边界普查：grep `TestErpFinDepreciationIntegration`（Branch A impl 未就绪跳过覆盖 :46-57）+ runDepreciation rethrow 路径测试（NopException 注入）+ findUnresolvedDepreciationSchedules 悬挂扫描测试 全集，产出测试覆盖边界清单 + 标注 rethrow 路径缺口（零覆盖，A1.6 §3.7 已记）+ 悬挂扫描缺口（零覆盖，A1.6 §3.7 已记）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
      - Evidence: 验证报告 §3（`TestErpFinDepreciationIntegration.java:46-57` 覆盖 + grep 实测 0 命中确认 Branch B rethrow + 悬挂扫描零覆盖，4 行缺口清单 + 风险评估归 MR1）
- [x] `Proof` MA4↔A5.6 边界声明：本验证审「行为是否符合需求」（PC-4 双路径交互是否有效阻断），与 A5.6 审「E2E 断言强度」边界按此执行。不重做 A5.6 E2E 断言强度审计。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
      - Evidence: 验证报告 §8（MA4↔A5.6 边界声明 checkbox 勾选，引用方法论 §去重协议）
- [x] `Decision` PC-4 符合性运行时确认（方法论 §2 判据 + 三源对照）：①若 rethrow 优先阻断 + 悬挂扫描覆盖历史遗留 + auto-post-on-close=true 时 rethrow 独立阻断无漏报 → PC-4 维持接受[运行时确认行为达成]，测试覆盖缺口归 MR1 测试补充（不降级 PC-4 符合性，§2 接受判据）；②若发现某路径漏报 → 登记 finding（watch-only 或按 §2 升 P1，BizModel 修复归 MR1）。裁决须列明 §2 判据编号 + L1/L2/L3 三源 + 与 A1.6 §5.2 PC-4 接受 + §6.2 P1-MA4-004 resolved 分层一致。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
      - Evidence: 验证报告 §5（PC-4 维持接受[运行时确认行为达成]——§2 接受判据；L1 use-cases.md:110 + L2 period-close.md §期末结账步骤 + L3 file:line 三源对照；§5.2 与 A1.6 §5.2 PC-4 接受 + §6.2 P1-MA4-004 resolved 分层一致表；§5.3 候选缺口评估全部归 MR1 测试补充或无 finding）

Exit Criteria:

- [x] runDepreciation G3 双分支 + 执行时序交互 + 悬挂过滤 + auto-post-on-close 交互 + 测试覆盖边界证据落盘（全集，无遗漏），每条有证据（file:line）
- [x] PC-4 符合性运行时确认有明确结论（维持接受 或 登记 finding），与 A1.6 §5.2 + §6.2 P1-MA4-004 分层一致

### Phase 2 - finding 衔接 + §8 自检 + 报告定稿

Status: completed
Targets: `docs/audits/2026-08-06-1708-rc-ma4-a4-1-19-pc4-depreciation-autoexecute-vs-dangling-block-interaction.md`（定稿）；`docs/audits/arm-index.md`（PC-4 / P1-MA4-004 注记更新，若有）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 PC-4 双路径交互评估 + 运行时确认完成

- [x] `Add` finding/注记更新：若维持接受 → 在 arm-index 无新 finding（PC-4 符合性维持，A1.6 §5.2 已裁决接受）；若发现漏报 → 登记 finding（watch-only 或 P1 MR1，BizModel 修复预授权类目）。测试覆盖缺口（rethrow 路径 + 悬挂扫描零覆盖）登记为 MR1 测试补充 follow-up（不降级 PC-4 符合性）。禁止未经比对新建重复 finding（P1-MA4-004 已登记 rethrow，本验证只确认交互 or 增量登记漏报）。
      - Skill: none
      - Evidence: 验证报告 §6（裁决 = 维持接受无新 finding；arm-index 无变更；测试缺口归 P1-MA4-005 MR2 follow-up + A1.6 §3.7 已记，复用不新建；grep 依据 `rg "P1-MA4-004|PC-4|资产未折旧|findUnresolvedDepreciation" docs/audits/arm-index.md` 命中 `:375`/`:822`/`:823` 与结论一致）
- [x] `Proof` §8 过程纪律自检：运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明「无回归风险」）；closure-audit 独立性声明；与 arm-index 交叉去重声明（与 A1.6 §2.4/§5.2 PC-4 / §6.2 P1-MA4-004 / A2.3 period-close E2E 的复用关系 + MA4↔A5.6 边界）。不以 checker 退出码 0 作为门控依据。
      - Skill: none
      - Evidence: 验证报告 §8（6 项 checkbox 全勾：checker actual vs baseline 实测表 R1a-R2d + 本验证零生产代码变更声明 + closure-audit 独立性 + arm-index 交叉去重 + MA4↔A5.6 边界 + 真相源冻结 + 保护区域纪律）

Exit Criteria:

- [x] 验证报告定稿（runDepreciation 双分支 + 执行时序交互 + 悬挂过滤 + auto-post-on-close 交互 + 测试覆盖边界 + 运行时确认 + finding 衔接 + §8 自检齐全）
- [x] PC-4 注记或新 finding 已更新入 arm-index（若有变更）或有明确「维持接受无变更」记录并有 grep 依据

## Draft Review Record

- Independent draft review iteration 1: accept (mission-driver 独立子代理 ses_029a60275ffePjiZzJWRaXgmLd，新会话不重用执行者上下文) — 全 9 checklist 项 PASS，零信任核对 live code（runDepreciation G3 双分支 :180-200 / findUnresolvedDepreciationSchedules filter :510-517 / findUnresolvedPostingExceptionKeys 三层 :471-477 / ClosePeriodProcessor :50 preCheck + :59-63 门控 / closeAssetModule:70 先于 advanceModule）零漂移；格式合规；单一结果表面；anti-slack 零命中；item typing 合规（Proof/Decision/Add 无 Fix）；Deps 门控满足（A4.1 expander done + A1.6 done）；保护区域纪律（只读不改 BizModel + 修复归 MR1 预授权类目）；逻辑健全（preCheck 扫历史遗留先于 runDepreciation 本次折旧 + 两路径 config 轴互斥无双报 + rethrow 回滚无悬挂落地 — 验证假设框架正确）；Closure Gates 删除全仓 typecheck/build（只读）对齐 guide + A4.1.18。无 Blocker/Major。1 non-blocking Minor（M1 行锚 `:69-70` 误归 :69=closeInvModule 给 AST — **已修订**：改为 `:67-71` 模块序列 + `:70 closeAssetModule`，对齐 A1.6 §2.6 既有约定）。promote to active。

## Closure Gates

> 本计划为**只读 PC-4 双路径交互运行时评估**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = runDepreciation 双分支 + 执行时序交互 + 悬挂过滤 + auto-post-on-close 交互 + 测试覆盖边界 + 运行时确认 + finding 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A4.1.19 验证报告 runDepreciation 双分支 + 执行时序交互 + 悬挂过滤 + auto-post-on-close 交互 + 测试覆盖边界 + 运行时确认齐全 + finding/注记更新（若有）
- [x] 相关文档对齐：报告与方法论 §MA4 + §2 判据 + §4 Q1 + §去重协议一致；与 A1.6 §7-2 + §2.4 PC-4 + §5.2 PC-4 接受 + §6.2 P1-MA4-004 resolved 一致
- [x] 已运行验证：runDepreciation 双分支 + 执行时序交互 + §8 checker actual vs baseline 实测记录（本计划无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up（若登记 finding 是验证**输出**，非范围内项目降级；测试覆盖缺口归 MR1 在 §Deferred But Adjudicated 预声明）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项保留为未勾选状态作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### PC-4 双路径测试覆盖缺口（rethrow 路径 + 悬挂扫描零覆盖）

- Classification: `out-of-scope improvement`（本验证是双路径交互评估，测试补充归 MR1）
- Why Not Blocking Closure: 本计划是双路径交互评估，结果表面 = 验证报告 + PC-4 符合性运行时确认。测试覆盖缺口（rethrow NopException 注入路径 + findUnresolvedDepreciationSchedules 悬挂扫描零覆盖，A1.6 §3.7 已记）归 MR1 测试补充。本验证闭环不阻塞于测试落地。
- Successor Required: yes（MR1 按本报告测试缺口方向展开：注入抛 NopException 的 IErpAstDepreciationScheduleBiz mock 覆盖 rethrow 路径 + 构造 EXECUTED+posted=false schedule 覆盖悬挂扫描）

## Closure

Status Note: 全 2 Phase 执行完成（Phase 1 PC-4 双路径交互运行时评估 + Phase 2 finding 衔接/§8 自检/报告定稿）。验证报告落盘 `docs/audits/2026-08-06-1708-rc-ma4-a4-1-19-pc4-depreciation-autoexecute-vs-dangling-block-interaction.md`。裁决：PC-4 维持接受[运行时确认行为达成]，无新 finding（测试覆盖缺口归 P1-MA4-005 MR2 follow-up + A1.6 §3.7 已记）。本计划零生产代码变更（只读评估），故跳过 build/test 门控（Closure Gates 顶部声明）。结束审计已由独立子代理（新会话，不重用执行者上下文）执行并通过。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（closure-auditor，新会话 MISSION_DRIVER:2026-08-04-224309-mission-driver，不重用执行者上下文）
- Evidence: 逐项零信任核对验证报告（`docs/audits/2026-08-06-1708-rc-ma4-a4-1-19-...md`，336 行）与 live repo 一致——①`ErpFinAccountingPeriodProcessor.runDepreciation:179-200` G3 双分支实测（Branch A `:184-191` catch Exception→warn→return；Branch B `:192-199` catch NopException→LOG.error→throw e；config gate `:180-182`）；②`ErpFinAccountingPeriodClosePeriodProcessor.doClosePeriod:46-87` 执行时序实测（preCheck `:50` 先于 closeAssetModule `:70` 先于状态推进 `:81-84`；gate `:59-63` `!isAutoPostOnClose()&&hasIssues()`）；③`findUnresolvedDepreciationSchedules:506-527` 过滤实测（posted=FALSE `:511` + status=EXECUTED `:514` + period=code `:516` + try/catch 安全跳过 `:522-525`）；④`TestErpFinDepreciationIntegration:46-57` 仅覆盖 Branch A，Branch B rethrow + 悬挂扫描零覆盖（归 MR1，§Deferred But Adjudicated 预声明）；⑤PC-4 维持接受[运行时确认行为达成]与 A1.6 §5.2 + §6.2 P1-MA4-004 分层一致；⑥§8 过程纪律自检 6 项全勾（checker actual vs baseline 零本验证引入变更 + closure-audit 独立性 + arm-index 交叉去重 + MA4↔A5.6 边界 + 真相源冻结 + 保护区域纪律）。五点文本一致性：Plan Status completed / 两 Phase completed / 全 Exit Criteria `[x]` / Closure Gates 全 `[x]` / Closure evidence 实证落盘。anti-hollow：只读评估无生产代码，验证报告 336 行非空壳。无 deferred 掩盖 live defect（测试缺口显式归 MR1，§Deferred But Adjudicated 预声明）。审计通过，无 Blocker/Major。

Follow-up:

- MR1 测试补充 PC-4 双路径覆盖缺口（rethrow 路径 + 悬挂扫描）
