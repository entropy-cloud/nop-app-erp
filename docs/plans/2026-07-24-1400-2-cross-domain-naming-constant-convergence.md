# 2026-07-24-1400-2-cross-domain-naming-constant-convergence 跨域命名与常量收敛（F6 闭包项 #10 + F7 闭包项 #11）

> Plan Status: completed
> Last Reviewed: 2026-07-24
> Source: `docs/audits/2026-07-23-0000-architecture-governance-review.md` §F6（LOW，闭包项 #10）+ §F7（LOW，闭包项 #11）
> Related: `docs/plans/2026-07-16-2134-1-ddd-entity-methods-daofor-convergence.md`（Decision D1 `Erp*DocStatus` dao 层非生成接口先例）、`docs/plans/2026-07-24-0930-2-shared-dict-status-enum-unification.md`（F2e drp dict 命名例外登记先例，已完成）
> Audit: required

## Current Baseline

两条 LOW 级跨域命名/常量耦合尚未关闭（实时仓库复核，2026-07-24）：

**F6（闭包项 #10）—— mfg 跨域依赖 qa 生成常量：**
- `module-manufacturing/erp-mfg-service/.../processor/ErpMfgWorkOrderProcessor.java:20` `import app.erp.qa.dao._ErpQaDaoConstants;`，在 `:195` 使用 `_ErpQaDaoConstants.INSPECTION_TYPE_FINAL`（完工检验触发，唯一使用点）。
- `_ErpQaDaoConstants` 是 codegen 产物（`_` 前缀，`module-quality/erp-qa-dao`）。
- qa 域**已有**非生成 dao 层接口 `ErpQaDocStatus`（`module-quality/erp-qa-dao/.../constants/ErpQaDocStatus.java`，D1 先例产物），但仅含 `APPROVE_STATUS_*` / `DOC_STATUS_*`，**不含** inspection-type 常量。
- 审查 #10 verification checkpoint：`grep _ErpQaDaoConstants module-manufacturing` 应返回 0。当前返回 2（import + 使用行）。

**F7（闭包项 #11）—— drp 4 实体命名前缀越界：**
- drp 域 4 个实体使用 `ErpInvDrp*` 前缀（`className=app.erp.drp.dao.entity.*`，实属 drp 域）：`ErpInvDrpCrossDock`、`ErpInvDrpSafetyStockCalc`、`ErpInvDrpDockAppointment`、`ErpInvDrpLeadTimeRecord`（对应表 `erp_inv_drp_*`）。
- `docs/design/drp/README.md:23` 已文档化命名现状（"安全库存计算/提前期记录/越库/月台预约实体保留 `ErpInvDrp*`...混用历史延续"），并在 `:92` 显式声明"实体重命名裁决归 F7 successor"——即**实体级命名例外尚未正式裁决登记**（仅 dict 级 F2e 已由 `2026-07-24-0930-2` 裁决登记）。
- 审查 #11 verification checkpoint 允许两种收口：`grep ErpInvDrp module-drp` 返回 0（重命名），**或**全部登记在 drp owner doc 的命名例外小节（登记豁免）。
- 重命名是高风险路径：触及 ORM `*.orm.xml`（保护区域）+ 表名 + 全部 BizModel/Processor/IBiz/test 引用 + 生成产物。

**先例可用性**：D1（`2026-07-16-2134-1`）已为 #10 提供完全同型先例（在 `-dao` 建非生成常量接口，消费者改 import）；F2e（`2026-07-24-0930-2`）已为 #11 提供命名例外登记先例。两条均无需新方法论。

## Goals

1. **关闭 F6 #10**：消除 mfg 对 qa 生成产物的跨域依赖——按 D1 先例在 `erp-qa-dao` 提供 inspection-type 常量的非生成 dao 层接口，mfg 改 import 它，`grep _ErpQaDaoConstants module-manufacturing` 归零。
2. **关闭 F7 #11**：对 4 个 `ErpInvDrp*` 实体做出带理由的命名裁决并登记——使 `grep ErpInvDrp module-drp` 的全部命中均在 drp owner doc 命名例外小节显式登记（满足 #11 checkpoint 的"或登记"分支）。

## Non-Goals

- **不重命名 `ErpInvDrp*` 实体**（ORM/表名/生成产物保护区域，高风险；#11 checkpoint 明确允许登记豁免作为等效收口）。重命名裁决作为 Deferred，带触发条件。
- **不处理 F2e drp dict 命名空间归属**（已由 `2026-07-24-0930-2` 裁决完成）。
- **不批量推广 inspection-type 常量到其他域**——仅迁移 mfg 实际消费的 `INSPECTION_TYPE_FINAL`（唯一使用点）。
- **不改 ORM 模型 / 字典**（保护区域）。

## Task Route

- Type: `implementation-only change`（#10）+ `architecture change`（#11 命名裁决，仅文档登记分支则降为 doc-only）
- Owner Docs: `docs/design/quality/README.md`（inspection-type 语义）、`docs/design/manufacturing/README.md`（完工检验触发）、`docs/design/drp/README.md`（命名例外登记）、`docs/architecture/domain-module-split-analysis.md §3`（命名约定）
- Skill Selection Basis: `nop-backend-dev` 匹配 #10 的"跨实体调用 / dao 层常量接口 / 产品化可定制性"工作方法（D1 先例即该技能路由的产物）。#11 命名裁决登记为纯文档，无 BizModel 编写，Skill: none。
- Bundling rationale (R4/R14)：#10 与 #11 是同一份架构治理审查（`2026-07-23-0000`）§F6+§F7 命名/常量收敛尾部的相邻 P2 行；各为 2-item 微切片，拆分将违反 R14 反碎片化意图；共享结果表面 = "audit governance naming-convergence closure"。两 Phase 退出标准独立（#10 grep `_ErpQaDaoConstants`=0 / #11 `ErpInvDrp` 全登记），可并行。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯 Java import 调整 + 文档登记，无端口/密钥/外部服务/数据迁移）。

## Execution Plan

### Phase 1 - F6 #10 mfg→qa 生成常量依赖消除

Status: completed
Targets: `module-quality/erp-qa-dao/.../constants/`（新增/扩展非生成接口）、`module-manufacturing/erp-mfg-service/.../processor/ErpMfgWorkOrderProcessor.java`（改 import）
Skill: `nop-backend-dev`

- Item Types: `Decision | Add | Fix`
- Prereqs: 无

- [x] `Decision | Add`：按 D1 先例，在 `erp-qa-dao` 的非生成常量接口承载 mfg 需要的 inspection-type 值。裁决写入位置：(A) 扩展现有 `ErpQaDocStatus`（语义偏离——它是 doc/approve 状态轴），或 (B) 新建专用非生成接口（如 `ErpQaInspectionType`，与 doc-status 同包）。记录选择理由（语义内聚 vs 文件数）+ 残留风险（选 A → inspection-type 语义混入 doc-status 接口；选 B → qa 域 dao 层常量接口 +1，与 D1 模式一致，可接受）。
  - Skill: `nop-backend-dev`
  - **执行**：选 (B) 新建专用接口 `module-quality/erp-qa-dao/.../constants/ErpQaInspectionType.java`，承载 `INSPECTION_TYPE_INCOMING/IN_PROCESS/FINAL/OUTGOING` 四值（erp-qa/inspection-type 字典）。理由：检验类型与 doc/approve 状态属不同语义轴，混入 `ErpQaDocStatus` 破坏其语义内聚；新建接口使 qa 域 dao 层常量接口 +1，与 D1 "每语义轴一接口" 模式一致，残留风险可接受。
- [x] `Fix`：`ErpMfgWorkOrderProcessor.java` 将 `_ErpQaDaoConstants.INSPECTION_TYPE_FINAL` 改为引用 #10 裁决的非生成接口常量；删除 `import ..._ErpQaDaoConstants`。
  - Skill: `nop-backend-dev`
  - **执行**：`import app.erp.qa.dao._ErpQaDaoConstants` → `import app.erp.qa.dao.constants.ErpQaInspectionType`；使用点 `_ErpQaDaoConstants.INSPECTION_TYPE_FINAL` → `ErpQaInspectionType.INSPECTION_TYPE_FINAL`。

Exit Criteria:

- [x] `rg "_ErpQaDaoConstants" module-manufacturing` 返回 0（审查 #10 verification checkpoint 满足）
- [x] 变更模块 `mvn compile` 通过（解除 Proof 阶段阻塞）

### Phase 2 - F7 #11 drp 实体命名裁决与登记

Status: completed
Targets: `docs/design/drp/README.md`（命名例外小节正式实体登记）、`docs/architecture/domain-module-split-analysis.md`（命名约定，若需追加例外引用）
Skill: none

- Item Types: `Decision | Add`
- Prereqs: 无（与 Phase 1 独立，可并行）

- [x] `Decision`：在 `docs/design/drp/README.md` 裁决 4 个 `ErpInvDrp*` 实体命名——推荐"登记命名例外"（零 ORM 风险，满足 #11 checkpoint 等效分支），记录理由（重命名触及 ORM 保护区域 + 表名 + 生成产物连锁，风险高于收益；物理归属已正确，`drp-` / `Drp` 段已显式标识归属）。明确否决"立即重命名"，重命名移入 Deferred 带触发条件。
  - Skill: none
  - **执行**：裁决"登记命名例外"（方案 b），4 理由记录于 `docs/design/drp/README.md` §`ErpInvDrp*` 实体命名例外登记；明确否决立即重命名，移入 Deferred（触发条件：drp 域重大 ORM 变更时顺带重命名为 `ErpDrp*`）。
- [x] `Add`：在 drp owner doc 命名例外小节正式登记 4 实体（类名 / className / 表名 / 所属域 / 豁免理由 / 收敛触发条件），使 `grep ErpInvDrp module-drp` 的全部命中均落在该登记覆盖范围内（满足 #11 checkpoint "或登记"分支）。
  - Skill: none
  - **执行**：drp owner doc 新增 §`ErpInvDrp*` 实体命名例外登记（裁决 + 4 实体逐项登记表：类名/className/表名/所属域/消费 dict/豁免理由/收敛触发条件）；登记覆盖范围声明覆盖全部 66 命中文件。`docs/architecture/domain-module-split-analysis.md §3` 追加已登记命名例外交叉引用。

Exit Criteria:

- [x] `docs/design/drp/README.md` 命名例外小节含 4 个 `ErpInvDrp*` 实体的逐项登记
- [x] `grep ErpInvDrp module-drp` 全部命中可追溯到该命名例外登记（审查 #11 verification checkpoint 满足）

### Phase 3 - 验证与闭包证据

Status: completed
Targets: 全仓构建 + checker 基线
Skill: none

- Item Types: `Proof`
- Prereqs: Phase 1 + Phase 2 完成

- [x] `Proof`：运行 `mvn clean install -DskipTests`（154 模块 BUILD SUCCESS）；复跑 `bash docs/audits/nop-compliance-checker.sh` 输出与 `docs/audits/compliance-baseline.md` 一致（#10 改 import 不应改变既有规则命中数；若 R3 `new Erp*` 等计数变化须在基线说明）。
  - Skill: none
  - **执行**：`mvn clean install -DskipTests` → BUILD SUCCESS（全 154 reactor 模块）。checker 复跑全 16 规则均 = 基线（R1a-d=0/0/0/23, R2a-d=37/319/1108/34, R3=19, R4=0, R5=0, R6=7, R7=2, R8=42, R10=51, R11=0, R12a-c=69/66/38），零 delta。R3=19 不变（新增 `ErpQaInspectionType` 为接口，无 `new Erp*()` 构造）；R12 不变（`ErpQaInspectionType` 是 qa 域内类型，非共享内核 R12 追踪的 3 类型）。

Exit Criteria:

- [x] 审查 #10 + #11 verification checkpoint 均满足
- [x] checker 输出与基线一致或变化已说明

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is` (`ses_06f550854ffeE6Pw1S4tdLP1rt`，独立 general 子代理，新会话冷重播无起草者上下文，2026-07-24) — 0 Blocker / 0 Major / 3 Minor。全部 13 项 load-bearing 事实主张经实时仓库逐项核实**零伪**。R1-R14 + anti-slack 全 PASS（R4/R14 bundling 经审查判定 legitimate，R7/R9 为 minor）。3 non-blocking Minor 全部修订落地：M1（Task Route 增 R4/R14 bundling rationale 句）/ M2（Phase 1 Item Types `Add|Fix` → `Decision|Add|Fix`）/ M3（Phase 1 Decision 项补残留风险）。草案审查收敛 → `Plan Status: active`。

## Closure Gates

- [x] 范围内行为完成（#10 import 切换 + #11 命名登记）
- [x] 相关文档对齐（`docs/design/drp/README.md` 新增 §`ErpInvDrp*` 实体命名例外登记 + line 23 命名现状注释更新；`docs/architecture/domain-module-split-analysis.md §3` 追加命名例外交叉引用；#10 为纯 Java 改动，quality/manufacturing owner doc 无需更新——inspection-type 语义未变）
- [x] 已运行验证：`mvn clean install -DskipTests`（154 模块 BUILD SUCCESS）+ checker 复跑（16 规则零回归，全 = 基线）
- [x] 无范围内项目降级为 deferred/follow-up（重命名是 #11 checkpoint 允许的等效收口，非范围内降级）
- [x] 独立草案审查已完成并记录（见 Draft Review Record）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### `ErpInvDrp*` 4 实体重命名（→ `ErpDrp*`）

- Classification: `optimization candidate`
- Why Not Blocking Closure: #11 verification checkpoint 明确允许"登记命名例外"作为等效收口；重命名触及 ORM `*.orm.xml` + 表名 `erp_inv_drp_*` + 全部 BizModel/Processor/IBiz/test + 生成产物，属保护区域高风险，收益（纯审美）低于风险。
- Successor Required: `yes`（触发条件：drp 域进行重大 ORM 变更时顺带重命名，或人工批准 ORM/表名迁移时）

## Closure

Status Note: 三 Phase 全部执行完成并验证通过（2026-07-24）。Phase 1（#10 mfg→qa 生成常量依赖消除）：新建 `ErpQaInspectionType` 非生成 dao 层接口（D1 先例，选 B 新建专用接口保持语义内聚），`ErpMfgWorkOrderProcessor` import/使用点切换，`grep _ErpQaDaoConstants module-manufacturing`=0。Phase 2（#11 drp 实体命名裁决）：裁决登记命名例外（方案 b，零 ORM 风险），4 实体逐项登记于 drp owner doc，`grep ErpInvDrp module-drp` 全部 66 命中落入登记覆盖范围。Phase 3：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + checker 16 规则零回归（R3=19 不变：新文件为接口；R12 不变：非共享内核类型）。源审计 `2026-07-23-0000` 闭包项 #10/#11 已标记 ✅。独立结束审计由独立子代理（新会话，冷重播无执行者上下文）执行并通过，证据见下。

Closure Audit Evidence:

- Auditor / Agent: 独立 closure-auditor 子代理（新会话，冷重播，无执行者上下文，2026-07-24）
- Evidence:
  - **Phase status / items 一致性**：Phase 1/2/3 均 `Status: completed`，Phase body 内全部执行项目与退出标准均为 `[x]`，无遗留 `- [ ]`（逐 Phase 核对原文）。
  - **Exit Criteria vs live repo（逐项冷复核）**：
    - Phase 1 #10：`rg "_ErpQaDaoConstants" module-manufacturing` 实测返回 **0 行**（checkpoint 满足）；`module-quality/erp-qa-dao/src/main/java/app/erp/qa/dao/constants/ErpQaInspectionType.java` 存在，承载 `INSPECTION_TYPE_INCOMING/IN_PROCESS/FINAL/OUTGOING` 四值（与 dict `erp-qa/inspection-type` 对齐）；`ErpMfgWorkOrderProcessor.java:20` import 已切到 `app.erp.qa.dao.constants.ErpQaInspectionType`，`:195` 使用点为 `ErpQaInspectionType.INSPECTION_TYPE_FINAL`。
    - Phase 2 #11：`rg "ErpInvDrp" module-drp` 实测 **66 文件**命中；`docs/design/drp/README.md:94` §`ErpInvDrp*` 实体命名例外登记存在，含裁决理由（4 条）+ 4 实体逐项登记表（`ErpInvDrpSafetyStockCalc`/`ErpInvDrpCrossDock`/`ErpInvDrpDockAppointment`/`ErpInvDrpLeadTimeRecord` 的类名/className/表名/所属域/消费 dict/豁免理由/收敛触发条件）+ `:121` 覆盖范围声明（66 文件全覆盖）；`docs/architecture/domain-module-split-analysis.md:161` 追加命名例外交叉引用。
  - **Anti-Hollow**：`ErpQaInspectionType` 为真实非空接口（4 常量），运行时由 `ErpMfgWorkOrderProcessor` 完工检验触发路径消费，非 `{}` / `return null` / 注册不可达。
  - **Five-point consistency**：Plan Status / 3 Phase Status / 退出标准 / Closure Gates（8/8 `[x]`）/ Closure evidence 全部一致为 completed。
  - **Deferred honesty**：`ErpInvDrp*` 重命名属 `optimization candidate`（#11 checkpoint 明确允许登记等效收口，非范围内缺陷降级），带明确触发条件（drp 域重大 ORM 变更或人工批准表名迁移）；无范围内缺陷被藏入 Deferred/Follow-up。
  - **Docs sync**：`docs/logs/2026/07-24.md` 已含本计划聚合日志条目（Phase 1/2/3 + 产出 + Deferred）；`docs/design/drp/README.md` + `docs/architecture/domain-module-split-analysis.md` 已更新；quality/manufacturing owner doc 无需更新（inspection-type 语义未变，#10 为纯 import 切换）。
  - **源审计回写**：`docs/audits/2026-07-23-0000-architecture-governance-review.md:354-355` 闭包项 #10/#11 均标 ✅ Done 且 grep checkpoint 标 ✅。

Follow-up:

- `ErpInvDrp*` 实体重命名（触发条件见上）
