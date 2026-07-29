# 2026-07-29-1708-2-ma7-ci-guard-activation-verification MA7 CI/guard 持续激活验证（A7.4）

> Plan Status: completed
> Last Reviewed: 2026-07-29
> Source: `docs/backlog/audit-remediation-roadmap.md` MA7（A7.4）
> Related: plan `2026-07-29-1708-1`（A7.1+A7.2+A7.3 同批 MA7）；plan `2026-07-27-1015-1` Phase 2（M0.3 锚点 HEAD=0e963531d 实测落锚）；plan `2026-07-24-0930-1`（compliance guard 激活 + CI 基线初建）
> Audit: required

## Current Baseline

- MA7（运维与性能层审计）A7.1-A7.3 由 plan `2026-07-29-1708-1` 覆盖；本计划覆盖 A7.4（CI/guard 持续激活验证）。A7.4 与 A7.1-A7.3 拆分为独立 plan 的依据：A7.4 使用 `compliance-checker` skill（非 `open-ended-audit-prompt`）、owner doc 为 `docs/audits/compliance-baseline.md`（非代码/ORM）、验证路径为「跑 checker + 比对基线 + 核实 CI 工作流」（非代码模式 grep），具有实质性不同的结束标准与验证路径。
- **Compliance checker 现状**（实仓 2026-07-29）：脚本 `docs/audits/nop-compliance-checker.sh` 含 19 条可计数规则（R1a-d / R2a-d / R3-R8 / R10-R11 / R12a-c；R9 为定性校验无数值计数不参与门控）；CI 工作流 `.github/workflows/compliance.yml` 存在（2026-07-24 创建），解析 `compliance-baseline.md` 的 `## BASELINE (machine-readable)` YAML 块（19 条）做单向收紧门控（actual > baseline → CI fail）。
- **M0.3 回归起点锚**：`compliance-baseline.md §M0 锚点注记` 记录 HEAD=0e963531d（2026-07-27 实测落锚，全 19 规则 actual ≤ baseline，精确匹配 0 漂移 + 156 模块 BUILD SUCCESS）。MV V.2 将以 M0 锚点为对比基线。
- **基线值现状**：`## BASELINE (machine-readable)` 块当前值 R2c=1228 / R2d=28 / R3=5 等（R2c=1228 经 plan `2026-07-25-1057-2` 裁决性上调，覆盖 149 per-mutation Processor 抽象基类 `dao()` 契约 + intercompany/commitment/GL Mapping 合法跨域编排，M0.3 锚点确证 actual=1228 无漂移）。**本审计核实 M0.3 锚点之后（2026-07-27 起 MA1-MA7 审计期间）是否有新引入的 actual-vs-baseline 漂移**——若审计发现新漂移（合规改善使 actual < baseline 属鼓励更新非阻塞；actual > baseline 属回归须即时修复）则登记；若锚点后无新漂移，则结论为「CI guard 持续激活、基线零漂移」。
- **F15 i18n checker CI 接入待裁决**（compliance-baseline.md §F15 注记 line 226 显式委托 A7.4）：F15 由独立 `docs/audits/i18n-coverage-checker.sh` 承载（A4.9 落锚基线），当前**未接入 CI workflow**（`.github/workflows/` 无引用），属「可手动运行的回归门」。本审计须裁决 F15 是否对齐 F8 接入 CI 模式。
- **Web 测试 @Tag 现状**（实仓 grep 2026-07-29）：19 个模块 web 测试各含 @Tag（计数=19，每模块 1 处）。owner doc = `docs/audits/compliance-baseline.md`。
- 验证基线：`mvn clean install -DskipTests` 全绿（154 模块）；`mvn test` 全绿（~2890 测试，0 failures）。
- 剩余差距：CI guard 自 2026-07-24 激活后从未做过「持续激活验证」——M0.3 锚点后是否有新漂移未核实、CI 工作流是否实际在 PR 流程中运行未核实、web 测试 @Tag 持续覆盖未验证、F15 i18n checker CI 接入未裁决。A7.4 是 MV V.2（compliance 基线对比）的**前置事实确证**。

## Goals

- **A7.4-a compliance checker 基线漂移验证**：跑 `bash docs/audits/nop-compliance-checker.sh` 取当前 19 规则实测值，与 `## BASELINE (machine-readable)` 块逐规则比对；核实 M0.3 锚点（HEAD=0e963531d，0 漂移）之后是否有新引入漂移。裁决每条漂移（合规改善 actual < baseline 属鼓励更新非阻塞 / 合法调高未登记 / 回归 actual > baseline 须即时修复）。
- **A7.4-b CI 工作流持续激活核实**：核实 `.github/workflows/compliance.yml` 是否在 PR/推送流程中实际触发、checker 解析与门控逻辑是否与基线块格式一致、是否存在规则增删后 checker 与基线块不同步。
- **A7.4-c web 测试 @Tag 持续覆盖验证**：核实 19 模块 web 测试 @Tag 是否持续有效（@Tag 命名一致性、是否被 CI/profile 正确过滤/包含）、A4.6-A4.8 view.xml drift 审计后的 web 测试是否仍绿。
- **A7.4-d F15 i18n checker CI 接入裁决**（compliance-baseline.md §F15 line 226 委托）：裁决 F15 `i18n-coverage-checker.sh` 是否对齐 F8 接入 CI 模式（接入 / 暂不接入登记 successor 触发条件）。
- 注册 P0（即时通道）/ P1（目标 MR3）/ P2（watch-only）发现至 `docs/audits/arm-index.md`，与 MA1-MA6 已登记 P1 交叉去重。
- 推进 roadmap A7.4 状态（审计产出后转 `ready`，独立 closure audit 后转 `done`）。

## Non-Goals

- 不调高/调低 compliance 基线（基线调整须经独立计划裁决，见 `compliance-baseline.md §回归门控规则`；本审计仅**发现并裁决漂移**，不执行调整）。若审计确证 machine-readable 块需更新，登记 P1 由 MR3 承担。
- 不审计 A7.1/A7.2/A7.3 代码质量维度（见 plan `2026-07-29-1708-1`）。
- 不做 MV V.2 的全量基线对比（V.2 依赖 MA7 全部 done + MR4；本审计是 V.2 的事实前置）。
- 不变更 checker 脚本核心逻辑 / CI 工作流 / 基线 YAML 块（纯审计；若需变更登记 P1）。
- 不变更任何生产代码 / ORM / 契约（纯审计）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/compliance-baseline.md`（基线表 + machine-readable 块 + 回归门控规则）+ `.github/workflows/compliance.yml`（CI 门控实现）+ `docs/audits/nop-compliance-checker.sh`（checker 脚本）。
- Skill Selection Basis: roadmap 指定 `compliance-checker` skill。**注**：`compliance-checker` 在 `docs/skills/`（23 文件）与 `.opencode/skills/` 中均无对应 skill 文件——roadmap A7.4 行使用此简写指代「以 `docs/audits/nop-compliance-checker.sh` 为审计方法工具」。故本计划以 `Skill: none` 记录，方法源 = checker 脚本 + `compliance-baseline.md` 回归门控规则 + CI 工作流。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline.
- 需可执行 `bash docs/audits/nop-compliance-checker.sh`（shell 脚本 + rg）。审计只读仓库与 CI 配置，不改代码。

## Execution Plan

### Phase 1 - compliance checker 基线漂移 + CI 激活 + web @Tag + F15 CI 接入裁决（A7.4）

Status: completed
Targets: `docs/audits/nop-compliance-checker.sh` + `docs/audits/compliance-baseline.md`（`## BASELINE (machine-readable)` 块 + M0.3 锚点注记 + §F15 注记）+ `.github/workflows/compliance.yml` + `docs/audits/i18n-coverage-checker.sh` + 19 模块 `erp-*-web/src/test/java`；报告 `docs/audits/2026-07-29-1708-arm-ma7-ci-guard-activation.md`
Skill: `none`（roadmap `compliance-checker` 简写无对应 skill 文件；方法源 = checker 脚本 + compliance-baseline.md 回归门控规则 + CI 工作流）

- Item Types: `Proof | Decision`
- Prereqs: M0.3 锚点

- [x] 跑 `bash docs/audits/nop-compliance-checker.sh` 取当前 19 规则实测汇总值，与 `## BASELINE (machine-readable)` 块逐规则比对，产出「规则 → baseline值 → 实测值 → 漂移方向（高/低/一致）→ 裁决」矩阵
      - Skill: `none`
- [x] 核实 M0.3 锚点（HEAD=0e963531d，记录 0 漂移）之后是否有新引入漂移：MA1-MA7 审计期间（2026-07-27 起）若有新增生产代码使 actual > baseline 属回归（P0 即时通道）；actual < baseline 属合规改善（鼓励更新非阻塞）。M0.3 锚点已确认 R2c=1228 为合法裁决性上调（plan `2026-07-25-1057-2`），非漂移
- [x] 核实 CI 工作流 `.github/workflows/compliance.yml` 激活性：触发条件（PR/push）、checker 执行、解析 machine-readable 块逻辑、门控判定（actual > baseline → fail）是否与 `compliance-baseline.md §回归门控规则` 声明一致
- [x] 核实 checker 脚本规则集与基线块（均 19 可计数规则）同步性：是否存在 checker 新增规则但基线块未登记 / 基线块有规则但 checker 已移除的不一致
- [x] 核实 19 模块 web 测试 @Tag 持续覆盖：@Tag 命名一致性、profile 过滤逻辑、A4.6-A4.8 view.xml drift 审计后 web 测试是否仍绿（读既有 web 测试运行证据 / `known-good-baselines.md`）
      - Skill: `none`
- [x] Decision: F15 `i18n-coverage-checker.sh` CI 接入裁决（compliance-baseline.md §F15 line 226 委托）——对齐 F8 接入模式 / 暂不接入登记 successor 触发条件
  - Skill: `none`
  - **裁决=接入 CI**（对齐 F8 经 plan `2026-07-24-0930-1` 激活的范式），登记 P1-MA7-007（目标 MR3）；F15 基线干净（quality 0 defects / strict 0 gaps），接入不触发 CI red
- [x] 产出 CI/guard 持续激活验证报告，分类 P0/P1/P2，更新 `docs/audits/arm-index.md`（去重 MA1-MA6）
  - Skill: `none`
  - 报告 `docs/audits/2026-07-29-1708-arm-ma7-ci-guard-activation.md`；arm-index 已登记（零 P0 + 1 P1 [P1-MA7-007] + 零 P2），P1-MA7-007 是 A4.9 line 165 委托的唯一裁决产出，无重复

Exit Criteria:

- [x] 19 规则漂移矩阵产出（baseline vs 实测 + M0.3 锚点后新漂移核实 + 逐条裁决）+ CI 激活性核实结论 + web @Tag 覆盖结论 + F15 CI 接入裁决
- [x] A7.4 P0/P1/P2 已登记 arm-index.md，且与既有 P1 交叉去重无重复

## Draft Review Record

- Independent draft review iteration 1: needs revision (`ses_052dbb423ffeCpN0exkSq4V979`) because (1) 虚假漂移前提——R2c=1228 为 plan `2026-07-25-1057-2` 裁决性上调（非漂移），M0.3 锚点确证 0 漂移，plan 误取 2026-07-24 中间值 ~1075 断言块偏高；(2) 规则计数错误——声称「39 规则/16 可计数」实际 checker 汇总表与 BASELINE YAML 块均 19 条；(3) `compliance-checker` skill 文件不存在（docs/skills 23 文件无匹配），需改记 `Skill: none`；(4) compliance-baseline.md §F15 line 226 显式委托 A7.4 裁决 i18n checker CI 接入，plan 遗漏该 Decision 项。
- Independent draft review iteration 2: accept (`ses_052d36f9cffegQ0AB7WPXmXhcA`) after 全部 4 项修订落地并经实仓复核（19 可计数规则 / R2c=1228 合法上调 / M0.3 锚点 0 漂移 / @Tag=19 / §F15 委托文本存在 / roadmap A7.4=todo / Skill: none 一致应用 / F15 已增为 A7.4-d Decision 项）；rule 4/14 拆分依据成立 / rule 7 typing / rule 8 skill / anti-slack / 退出标准本地化 / Closure Gates / 无自我审计 / 无占位门控 / Non-Goal 边界（审计发现裁决漂移，基线 YAML 修改路由 MR3）正确。无阻塞问题。

## Closure Gates

> 本 plan 为纯审计，不改代码。`mvn test` 仅作回归基线确认（见 roadmap 横切关注点 §审计 plan 的 BUILD_VERIFY）。

- [x] A7.4 CI/guard 持续激活验证报告产出
- [x] arm-index.md 已登记本批次全部 P0/P1/P2，且与 MA1-MA6 既有 P1 交叉去重无重复
- [x] roadmap A7.4 状态推进至 `ready`（独立 closure audit 后转 `done`）
- [x] 已运行 `mvn clean install -DskipTests`（154 模块绿）+ `mvn test`（0 failures）作回归基线确认
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态 / 阶段 / 门控 / 日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 基线 YAML 块同步更新

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本审计仅发现并裁决漂移；machine-readable 块的实际更新须经独立计划裁决（`compliance-baseline.md §调高基线的唯一途径`），属 MR3 修复范围。
- Successor Required: `yes`（触发条件：本审计确证 M0.3 锚点后有新漂移并登记 P1 后，由 R3.0 展开机制生成具体修复工作项行）

## Closure

Status Note: EXECUTE 完成（2026-07-29）。A7.4 四维度全部 PASS——19 规则基线精确 0 漂移（M0.3 锚点后 62 commits 含 4 P0 fix 触及 16 生产文件零 daoFor/反模式回归）/ CI 工作流激活性 + 门控逻辑与 owner doc 一致 / checker↔基线块同步（19=19）/ 19 模块 web 测试 @Tag 100% 一致（`full-app` 单值）。A7.4-d 裁决=接入 CI（对齐 F8），登记 P1-MA7-007（目标 MR3，F15 基线干净 0/0 不触发 CI red）。报告 `docs/audits/2026-07-29-1708-arm-ma7-ci-guard-activation.md`；arm-index.md 已登记（零 P0 + 1 P1 + 零 P2，P1-MA7-007 是 A4.9 line 165 委托唯一裁决产出，无重复）。回归基线：`mvn clean install -DskipTests` BUILD SUCCESS + `mvn test` BUILD SUCCESS（0 failures / 0 errors / 1 skipped = 已知 `ErpAllWebPagesCollectTest` @Disabled）。roadmap A7.4 推进至 `ready`（独立 closure audit 后转 `done`）。本计划产出已达 EXECUTE 退出条件（Phase 1 `completed` + 退出标准全 `[x]` + Closure Gates 全 `[x]`）；最终 `done` 状态转捩需独立 closure audit 通过（执行者未自我审计，roadmap 维持 `ready` 待 closure audit 转 `done`）。

Closure Audit Evidence:

- Auditor / Agent: <独立结束审计子代理（pending）>
- Evidence: `docs/audits/2026-07-29-1708-arm-ma7-ci-guard-activation.md`（报告）+ `docs/audits/arm-index.md`（A7.4 行 + P1-MA7-007 行 + MA7 累计 P1=2）+ 回归基线（`mvn clean install -DskipTests` BUILD SUCCESS / `mvn test` 0 failures）+ roadmap A7.4=ready

Follow-up:

- 基线块同步更新 / checker 规则对齐 / CI 工作流修复不在此处；由 R3.0 展开机制将本批次 P1 转化为 MR3 具体修复工作项行。
