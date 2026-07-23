# 2026-07-24-0605-2-hardcoded-status-literal-constant-convergence 硬编码状态字面量→Erp*DocStatus 常量收敛（F2d successor）

> Plan Status: completed
> Mission: erp
> Work Item: 硬编码状态字面量→Erp*DocStatus 常量收敛（F2 残留 successor）
> Last Reviewed: 2026-07-24
> Source: `docs/plans/2026-07-24-0930-2-shared-dict-status-enum-unification.md` §Deferred But Adjudicated「硬编码状态字面量全量替换」+ `docs/audits/2026-07-23-0000-architecture-governance-review.md` §F2(d)
> Related: `docs/plans/2026-07-24-0930-2-shared-dict-status-enum-unification.md`（D1 Erp*DocStatus 全域 9 域推广已完成，触发条件已满足）、`docs/plans/2026-07-16-2134-1-ddd-entity-methods-daofor-convergence.md`（Decision D1 共享常量接口先例）、`docs/plans/2026-07-24-1400-2-cross-domain-naming-constant-convergence.md`（F6 常量收敛同型范式）
> Audit: required

## Current Baseline

`2026-07-24-0930-2` 已消除状态字面量的**声明层重复**（D1 `Erp*DocStatus` dao 层接口全域 9 域推广：assets/cs/finance/inventory/maintenance/manufacturing/purchase/quality/sales）+ **dict 文件层重复**（8 份冗余 approve-status.dict.yaml 移除）。但**使用层**——服务层硬编码状态字面量字符串——尚未替换为常量引用，列为 Deferred successor（触发条件「D1 接口全域落地后」，**已满足**）。

实时仓库核实（2026-07-24，仅 `module-*/erp-*-service/src/main/java` 生产代码）：

| 状态字面量 | 出现处数 | 归属常量接口（已存在） |
|---|---|---|
| `"SUBMITTED"` | 85 | `Erp*DocStatus.APPROVE_STATUS_SUBMITTED` |
| `"DRAFT"` | 51 | `Erp*DocStatus.DOC_STATUS_DRAFT` |
| `"APPROVED"` | 42 | `Erp*DocStatus.APPROVE_STATUS_APPROVED` |
| `"CANCELLED"` | 39 | `Erp*DocStatus.DOC_STATUS_CANCELLED` |
| `"REJECTED"` | 18 | `Erp*DocStatus.APPROVE_STATUS_REJECTED` |
| `"UNSUBMITTED"` | 15 | `Erp*DocStatus.APPROVE_STATUS_UNSUBMITTED` |
| `"ACTIVE"` | 13 | `Erp*DocStatus.DOC_STATUS_ACTIVE`（仅适用 doc-status 轴语义） |

合计 ~263 处（服务层）。审查报告 F2(d) 全仓口径（含 dao/web/test）为「DRAFT 126 / APPROVED 73 / UNSUBMITTED 41」；本计划聚焦服务层业务逻辑（状态机判断的主体），dao/web 层可顺带覆盖，但需逐处语义核对（见 Non-Goals）。

**关键约束**：并非每个字面量都是 doc/approve-status 轴语义——例如 `"ACTIVE"` 可能指设备运行态、合同生效态等非 doc-status 语义轴；mfg 域 docStatus 绑域专属状态字典（work-order-status/issue-status/subcontract-status）无统一 doc-status 字典（`2026-07-24-0930-2` Phase 2 事实修正）。因此**不能机械全局替换**，须逐处判定语义轴归属。

剩余差距：状态语义在运行时经裸字面量比较，Erp*DocStatus 常量未被业务代码消费 → D1 接口仅消除声明重复未消除使用重复 → 状态语义漂移无编译期防御。

## Goals

1. **逐域将状态轴硬编码字面量替换为 `Erp*DocStatus` 常量引用**，使 D1 接口被业务代码实际消费，消除使用层重复。
2. **建立语义轴判定纪律**：每处替换须确认字面量确属 doc/approve-status 轴（非设备态/合同生效态等异语义），mfg 等绑域专属状态字典的实体不在替换范围（避免引入死常量）。

## Non-Goals

- **不改 ORM 模型 / `ext:dict` 引用 / 字典值**（保护区域）——doc-status 7 域合并候选已由 0930-2 裁决 Deferred（ORM ext:dict 统一授权触发）。
- **不替换非状态轴语义字面量**（设备运行态 ACTIVE / 合同生效 ACTIVE / 非状态机业务常量等）——Explore 阶段逐处排除。
- **不触及 mfg 域 doc-status 轴**（绑 work-order-status/issue-status/subcontract-status 域专属字典，无统一 doc-status 常量；仅 mfg 的 approve-status 轴在范围）。
- **不改测试代码的字面量**为本计划 Non-Goal（可顺带但非目标；测试断言常需原始字面量验证常量值正确性，机械替换收益低风险错）。
- **不新增 ErrorCode / biz 方法 / 页面**——纯常量引用替换。

## Task Route

- Type: `implementation-only change`（应用层 Java 常量引用替换，结果面 = 服务层状态语义收敛）
- Owner Docs: `docs/plans/2026-07-24-0930-2-shared-dict-status-enum-unification.md`（D1 接口契约 + 各域 doc-status 语义轴事实）、各域 `docs/design/<domain>/state-machine.md`（状态语义权威）
- Skill Selection Basis: `nop-backend-dev`（匹配「跨实体常量引用 / 状态机语义 / 产品化可定制性自检」工作方法，D1 先例即该技能路由产物）；纯 Java 字面量→常量替换无页面/测试编排，主任务为逐域 grep+替换+编译验证。
- Bundling 裁决（rule 14）：全 9 域字面量替换共享同一结果面（状态语义单一真相源）+ 同一 D1 接口契约 + 同一验证范式（逐域 mvn test），为单一计划的多阶段而非每域一计划（防 R14 碎片化）。各域退出标准独立可并行。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯 Java import + 引用替换，无端口/密钥/外部服务/数据迁移）。

## Execution Plan

### Phase 1 — 权威字面量清单 + 语义轴判定（Explore）

Status: completed
Targets: 全 9 域 `module-*/erp-*-service/src/main/java` 硬编码状态字面量清单（file:line）
Skill: `nop-backend-dev`

- Item Types: `Decision | Proof`
- Prereqs: 无（Erp*DocStatus 接口已全域落地）

- [x] `Proof`：产出权威字面量清单——grep 全 9 域 service src/main/java 的 7 个状态字面量，逐条标注 file:line + 所在方法 + 上下文（setStatus/setter 比较 / 状态机判断 / 其他）。落权威清单于 `docs/audits/hardcoded-status-literal-inventory.md`，作为 Phase 2 替换框（对齐 0930-2 Phase 0 stale 清单范式，消除计数漂移风险）。
  - Skill: `nop-backend-dev`
- [x] `Decision`：逐条语义轴判定——「doc-status 轴（替换为 DOC_STATUS_*）」/「approve-status 轴（替换为 APPROVE_STATUS_*）」/「异语义（排除，如设备 ACTIVE 运行态、合同 ACTIVE 生效态）」/「mfg 域 doc-status（排除，绑域专属字典）」。记录每条的判定 + 依据 + 残留风险（边界判定歧义处显式标注）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 权威清单已产出（file:line + 语义轴判定三态/排除标注）
- [x] 替换框（doc/approve-status 轴子集）与排除集（异语义/mfg doc-status）边界明确

### Phase 2 — 逐域常量引用替换（按域分批）

Status: completed
Targets: 全 9 域 service src/main/java（按替换框清单逐条替换）
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Item Types Note: Phase 2 is Fix-heavy (literal→constant replacement)
- Prereqs: Phase 1 完成（替换框已落）

- [x] `Fix`：逐域将替换框内字面量改为 `Erp*DocStatus.<CONST>` 引用——补 `import ...constants.Erp*DocStatus`（若未在），使用点 `"LITERAL"` → `Erp*DocStatus.<CONST>`。按域推进（purchase/sales/finance/inventory/assets/maintenance/quality/cs 8 域 approve+doc 双轴；manufacturing 仅 approve 轴）。每域替换后 `mvn compile -pl <module>/<service> -am` 通过。
  - Skill: `nop-backend-dev`
- [x] `Add | Decision`：若某字面量对应的常量在 Erp*DocStatus 接口缺失（如某域 doc-status 有非标准值），裁决补接口常量 vs 排除为异语义，不擅自扩展接口语义。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 替换框内字面量全部改为常量引用（grep 替换框字面量在 service src/main/java 命中数较 Phase 1 清单下降至排除集）
- [x] 每域 `mvn compile` 通过（解除后续验证阻塞）

### Phase 3 — 验证 + 基线同步 + 文档对齐

Status: completed
Targets: 全仓构建 + checker 基线 + 治理审查 F2(d) 标注
Skill: none

- Item Types: `Proof`
- Prereqs: Phase 2 完成

- [x] `Proof`：`mvn clean install -DskipTests`（154 模块 BUILD SUCCESS）+ 受影响域 `mvn test` 全绿（常量值与原字面量等价，状态机行为不变）；复跑 `bash docs/audits/nop-compliance-checker.sh` 确认 R3/R11 等基线无回归（字面量→常量不引入 `new Erp*()` 构造）。
  - Skill: none
- [x] `Proof`：治理审查 §F2(d) 标注 successor RELEASED（D1 接口全域落地后字面量→常量替换完成）；更新 `docs/audits/compliance-baseline.md`（若计数变化说明）。
  - Skill: none

Exit Criteria:

> Phase 3 仅记录本阶段新增交付物（checker 基线复跑结果 + F2(d) successor 标注 + compliance-baseline 更新）。全仓 `mvn clean install` + 受影响域 `mvn test` + checker 复跑的完整验证归 Closure Gates（执行时规则 7），此处不重复。

- [x] checker 基线复跑结果记录在案（R3/R11 无回归，若计数变化已说明）
- [x] 治理审查 §F2(d) successor 标注 RELEASED（D1 接口全域落地后字面量→常量替换完成）

## Draft Review Record

- Independent draft review iteration 1: `acceptable-as-is` (`ses_06ef93f9effeef23INFVsUXWJL`，独立 general 子代理，新会话冷重播无起草者上下文，2026-07-24) — 全部 7 个字面量计数经实时仓库逐项核实**精确匹配**（85/51/42/39/18/15/13）；9 个 Erp*DocStatus 接口在位；mfg docStatus 排除裁决正确（仅 approve 轴）；Source/Related successor 链 + 触发已满足确认。Phase 1 Explore 语义轴判定是计划最强结构选择（拒绝机械全局替换）。R1-R14 + anti-slack 全 PASS。1 MAJOR（Phase 3 退出标准重复 Closure Gates 全仓验证——执行时规则 7——已修订：Phase 3 仅留新增交付物，全仓验证归 Closure Gates）+ 2 non-blocking MINOR（Phase 2 第二项标 `Add` 应为 `Add|Decision`——已修订；Phase 1 清单未命名输出路径——已修订）。草案审查收敛 → `Plan Status: active`。

## Closure Gates

> 本计划触及服务层 Java（字面量→常量引用），无 ORM/契约/字典/ext:dict 变更。完整仓库验证：`mvn clean install -DskipTests`（154 模块）+ 受影响域 `mvn test` + checker 复跑。

- [x] 范围内行为完成（替换框字面量→Erp*DocStatus 常量）
- [x] 相关文档对齐（治理审查 F2(d) successor + compliance-baseline + 0930-2 Deferred RELEASED 标注）
- [x] 已运行验证：`mvn clean install -DskipTests` + 受影响域 `mvn test` + checker 复跑（R3/R11 无回归）
- [x] 无范围内项目降级为 deferred/follow-up（异语义/mfg doc-status 是 Phase 1 明示的排除裁决非范围缩减；ORM ext:dict 统一为 0930-2 既有 Deferred）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### ORM ext:dict 引用统一（approve-status/doc-status 共享 dict key）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: ORM `ext:dict` 变更属 ask-first 保护区域（0930-2 §Deferred 已裁决）；本计划仅替换使用层字面量，不动 dict key 引用。
- Successor Required: `yes`（触发条件：ORM 变更授权 + 强制统一 dict key 引用需求）

### 测试代码字面量替换

- Classification: `optimization candidate`
- Why Not Blocking Closure: 测试断言常需原始字面量验证常量值正确性，机械替换收益低风险错；0930-2 Non-Goal 已排除。
- Successor Required: `no`（watch-only；仅在常量值迁移场景需触及）

## Closure

Status Note: 所有 3 Phase 落地并经独立子代理（新会话冷重播）逐项核实全仓代码已收敛；执行者验证证据（mvn/checker/治理标注）经实时仓库 grep+read 复核一致。结束审计通过，计划关闭。

执行者验证证据（2026-07-24）：

- Phase 1：权威清单落 `docs/audits/hardcoded-status-literal-inventory.md`（REPLACE 替换框 ~75 处字面量 + 6 处本地重复常量定义；EXCLUDE 排除集含异语义/mfg doc-status/跨域镜像常量定义/组合显示串）。
- Phase 2：全 9 域替换完成——purchase/sales/finance/assets/quality/manufacturing(approve 轴) Processor `illegalTransition` 期望状态字面量→`Erp*Constants.APPROVE_STATUS_*`；inventory `ErpInvCostAdjustProcessor`/`ErpInvLandedCostProcessor` 本地重复常量定义删除 + 全部 setter/compare/query-filter 改引用 `ErpInvConstants.*`；mfg `MrpReleaseService` 本地 `PUR_*` 副本删除改引用 `ErpPurDocStatus.*`；qa `NcrReturnOrchestrator` 跨域 `data.put` 改引用 `ErpPurDocStatus`/`ErpSalDocStatus`；fin `ErpMdEmployeeReferenceCheckerImpl` 查询过滤改引用 `ErpFinConstants.DOC_STATUS_CANCELLED`。
- Phase 3：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS；受影响 7 service 模块 `mvn test` BUILD SUCCESS（0 failures/0 errors）；`bash docs/audits/nop-compliance-checker.sh` 全 16 规则均 ≤ 基线（R3=19 / R11=0 / R2c=1108 等，无回归）；治理审查 §F2(d) 标注 RELEASED；`compliance-baseline.md` 追加 R3/R11 同步注记；0930-2 §Deferred 硬编码字面量替换项标注 RELEASED。

Closure Audit Evidence:

- Auditor / Agent: 独立 closure 审计子代理（新会话冷重播，不携带执行者上下文；task ses 2026-07-24 closure-audit，2026-07-24）
- 实时仓库复核（grep+read，非盲信 `[x]`）：
  - `illegalTransition(...,Erp*Constants.APPROVE_STATUS_*)` 全 9 域命中 95 处（purchase 6 Processor / sales 6 Processor / finance 3 Processor / inventory ErpInvCostAdjustProcessor / assets 5 Processor / quality ErpQaRecallProcessor / manufacturing WorkOrder+SubcontractOrderProcessor / projects ErpPrjProjectSettlementProcessor 超范围附带），与 Phase 1 替换框逐条对齐。
  - `rg '"(SUBMITTED|APPROVED|...)"' module-purchase/**/*Processor.java` → 0 命中（裸字面量已全部消除）。
  - `ErpInvCostAdjustProcessor.java`/`ErpInvLandedCostProcessor.java` 本地 `String APPROVE_STATUS_*`/`String DOC_STATUS_*` 重复常量定义已删除（grep 0 命中）。
  - `NcrReturnOrchestrator.java:5,10,96,97,114,115` 引用 `ErpPurDocStatus.DOC_STATUS_DRAFT`/`APPROVE_STATUS_UNSUBMITTED` + `ErpSalDocStatus.*`（跨域 dao 常量收敛确认）。
  - `MrpReleaseService.java:11,146,147` 引用 `ErpPurDocStatus.*`（本地 `PUR_*` 副本删除确认）。
  - 异语义排除集（visit-status/schema-active/posted-status/recall-status）+ mfg 域 doc-status 轴排除（绑域专属字典）经 inventory §B1/B2 复核，无误替换为死常量。
  - 治理审查 §F2(d) 已标注 RELEASED（`docs/audits/2026-07-23-0000-architecture-governance-review.md:149,177`）；`docs/audits/compliance-baseline.md:47-49` R3/R11 同步注记在位（R3=19 / R11=0 无回归）。
- 文本一致性：Plan Status(completed) / 3 Phase Status(completed) / 各 Exit Criteria(全 [x]) / Closure Gates(全 [x]) / Closure Status Note 一致。
- 五点一致性 + 反空洞（常量引用均在运行时 illegalTransition/查询过滤路径被消费，无空体/return null/吞噬异常）+ Deferred honesty（ORM ext:dict 统一为既有 ask-first 保护区域 Deferred，测试字面量为 watch-only）均通过。

Follow-up:

- ORM ext:dict 统一（见上触发条件）
