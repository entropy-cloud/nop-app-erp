# 2026-07-29-0024-1-audit-remediation-ma4-mfg-work-order-bom-code-quality MA4 manufacturing 代码质量审计 — 工单与报工 / BOM 与工艺路线（A4.2a）

> Plan Status: completed
> Last Reviewed: 2026-07-29
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA4（工作项 A4.2a，S 级拆分 1/2）
> Related: `docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.4「代码质量（MA4）」行 + §1.3 manufacturing 功能模块拆分「工单与报工 / BOM 与工艺路线」切片；`docs/audits/arm-index.md`（P1 索引）；`docs/skills/code-quality-audit-prompt.md`（审计方法）；`docs/design/manufacturing/state-machine.md` + `bom-and-routing.md` + `README.md`（owner doc 锚点）；`docs/plans/2026-07-28-0109-1-audit-remediation-ma2-mfg-work-order-jobcard-state-machine.md`（A2.6a 同切片业务正确性审计——状态机正确性，本审计聚焦**代码实现质量**，互补不重叠）；`docs/plans/2026-07-29-0024-2-audit-remediation-ma4-mfg-mrp-quality-code-quality.md`（A4.2b 同域拆分 2/2——MRP/质量集成，不同功能模块，独立计划）；`docs/plans/2026-07-28-2130-2-audit-remediation-ma4-finance-posting-voucher-code-quality.md`（A4.1a finance 过账片——MA4 已落地的首例范式参照）
> Audit: required

## Current Baseline

manufacturing 代码质量审计工单与报工 / BOM 与工艺路线切片（代码与前端质量层 MA4 第三项，S 级拆分 1/2）。roadmap 工作项 A4.2a 声明审查"manufacturing 代码质量审计 — 工单/BOM（S 级拆分 1/2）"，owner doc 标注 `docs/design/manufacturing/`，skill `docs/skills/code-quality-audit-prompt.md`。

**关键基线事实（实时仓库核实）**：

- **manufacturing 域是 S 级域**（scope matrix §1.1 快照 2026-07-27，用于驱动 S 级分级）：41 实体 / 74 mutation / 35 query / 21 Proc/Engine/Resolver / 11 状态机实体 / 30 测试 / 31 view.xml / 47 跨域 daoFor。S 级（mutation ≥ 70 满足，分级结论稳定），按 scope matrix §1.3 功能模块拆分为 4 片，本审计覆盖「工单与报工」+「BOM 与工艺路线」2 个功能模块片。（注：scope matrix §1.1 自述数据会随代码变化而漂移，MV 验证里程碑需重跑确认。）
- **工单与报工 / BOM 与工艺路线链路代码规模**（实时仓库核实）：`find module-manufacturing -path "*service*" \( -name "*WorkOrder*" -o -name "*JobCard*" -o -name "*MaterialIssue*" -o -name "*Bom*" -o -name "*BOM*" -o -name "*Routing*" -o -name "*WorkCenter*" -o -name "*Operation*" \) -name "*.java" -not -path "*/target/*"` = 29 文件（含 7 测试）。核心组件：
  - **工单与报工**：`ErpMfgWorkOrderBizModel`（工单编排 + checkAvailability 齐套校验 + start/reportCompletion/close/cancel 状态机）/ `ErpMfgWorkOrderProcessor` + 审批轴 6 Processor（Submit/Approve/Reject/ReverseApprove/Withdraw + ScheduleToJobCard）/ `ErpMfgJobCardBizModel` + `ErpMfgJobCardProcessor`（报工 recordWork + laborCost 回写）/ `ErpMfgMaterialIssueBizModel` + `MaterialIssueStockMoveBuilder`（领料 OUTGOING 移动 + materialCost 回写）/ `ManufacturingIssuePostingDispatcher` + `ManufacturingIssueAcctDocProvider` + `MfgPostingExecutor`（领料过账 Facade）
  - **BOM 与工艺路线**：`ErpMfgBomBizModel` + `BomExpander`（BOM 多级展开）/ `ErpMfgBomLineBizModel` / `ErpMfgBomOperationBizModel` / `ErpMfgBomByproductBizModel` / `ErpMfgRoutingBizModel` + `ErpMfgRoutingOperationBizModel`（工艺路线 + 工作中心）
- **owner docs**：`state-machine.md`（工单状态机 + 齐套校验 + 质检门控 config-gated + 报工 + 领料）/ `bom-and-routing.md`（BOM 展开 + 工艺路线 + 工作中心）/ `README.md`（域业务对象概览）。
- **MA2 已审计的已知 finding（代码质量审计输入，非重复审计）**：A2.6a 工单/jobcard 状态机审查（P1-MA2-035 JobCard PARTIALLY_TRANSFERRED/MATERIAL_TRANSFERRED dict 死状态）；A2.17 并发审计（P1-MA2-086 erp-mfg-jobcard-auto-generate cron 并发重复副作用 + 透明乐观锁降级——WorkOrder 等 mfg 状态机实体声明 versionProp）；A2.6a 交接 ManufacturingIssuePostingDispatcher tryPost 吞异常悬挂同型根因（finance P1-MA2-032 + hr P1-MA2-048 + assets P1-MA2-060 + qa + projects + maintenance + logistics 同型）。
- **MA1 已审计的已知 finding**：P1-MA1-001（ErpMfgWorkOrder/ErpMfgMaterialIssue 多币种四件套 7 列 propId 缺失）；P1-MA1-022（aps 跨域只读 daoFor ErpMfgBom/ErpMfgBomOperation——读侧投影）。
- **MA3 已审计的已知 finding（owner-doc drift，复核输入）**：P1-MA3-040（state-machine.md §质检约束声明引用不存在的 INSPECTING 工单状态）；P1-MA3-041（state-machine.md 声明可配置超产但 code 无此 config）；P1-MA3-044（README 列 DowntimeEntry + ProductionPlan 实体但 ORM 不存在）。

**审计张力**：MA2 审计了工单/BOM 链路的**业务正确性**（状态机/并发），但**代码实现质量**（架构边界完整性 / 核心实现正确性 / 类型与契约质量 / 错误处理与操作安全 / 测试有效性 / 可维护性 / 自动化防护）是 MA4 的独立维度。MA2 已知 finding 是本审计的**输入**（复核运行时是否如 owner doc 声明 / 是否有未发现的代码缺陷），非重复审计。本审计聚焦 MA2 未覆盖的代码质量维度：如 WorkOrderProcessor 编排的事务边界健壮性 / 齐套校验 checkAvailability 的并发安全 / 领料 MaterialIssueStockMoveBuilder 的库存移动构造正确性 / ManufacturingIssuePostingDispatcher 的过账异常吞咽与悬挂 / BomExpander 多级展开的递归终止与环检测 / 报工 cost 回写的算术正确性 / 工单完工入库的跨域 Facade 调用错误传播 / 测试是否覆盖异常路径而非仅黄金路径。

剩余差距：需要一次工单与报工 / BOM 与工艺路线链路的代码实现质量审计。发现的缺陷分类为：(a) **架构边界违规**（major——跨域写绕过 I*Biz / 生成物手编）；(b) **核心实现正确性**（major/blocker——事务边界缺失 / 异常吞咽致悬挂 / 幂等破缺 / 算术错误）；(c) **错误处理与操作安全**（major——NopException 规范 / ErrorCode 完整性）；(d) **测试有效性**（major——异常路径未覆盖 / 测试断言强度不足）；(e) **可维护性风险**（P2——复杂度热点 / 重复模式）。blocker/major 登记为 P1（代码类目标 MR2——MR2 deps = MA3+MA4 done；若属业务正确性则目标 MR1）。若发现活跃数据破坏路径，升级标注走 P0 即时通道。

## Goals

- 按 `code-quality-audit-prompt.md` 7 重点领域（架构边界 / 核心实现正确性 / 类型契约 / 错误处理 / 测试有效性 / 可维护性 / 自动化防护）对 manufacturing 工单与报工 / BOM 与工艺路线链路代码做系统性实现质量审计，产出审计报告。
- 审计覆盖核心组件：`ErpMfgWorkOrderBizModel` 工单编排（状态机 + checkAvailability 齐套校验 + start/reportCompletion/close）/ `ErpMfgWorkOrderProcessor` + 审批轴 6 Processor / `ErpMfgJobCardBizModel` + `ErpMfgJobCardProcessor`（报工 + laborCost 回写）/ `ErpMfgMaterialIssueBizModel` + `MaterialIssueStockMoveBuilder`（领料 + materialCost 回写）/ `ManufacturingIssuePostingDispatcher` + `ManufacturingIssueAcctDocProvider`（领料过账）/ `BomExpander`（BOM 展开）/ Routing + WorkCenter。
- 复核 MA1/MA2/MA3 已知 finding（P1-MA1-001/022 / P1-MA2-035/086 / P1-MA3-040/041/044 + ManufacturingIssuePostingDispatcher tryPost 悬挂同型）的运行时状态，标记是否有 MA2 未发现的代码层缺陷。
- scope matrix §2.4「代码质量（MA4）」行增 manufacturing 工单/BOM 片完成注记段（§2.4 无 per-domain 列，以注记段反映进度；manufacturing 代码质量全片终态在 A4.2b 收口）。
- 发现的 blocker/major 登记为 P1 汇总至 `arm-index.md` §P1 发现汇总。roadmap A4.2a 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**做 MRP/DRP 引擎 / 质量集成与 NCR / 成本核算 / 基因追溯 / 差异计算代码质量 — 归 A4.2b（同批起草，S 级拆分 2/2，不同功能模块）。
- **不**做工单/BOM 链路的业务正确性/状态机审计 — 归 A2.6a（已 done）。本审计聚焦**代码实现质量**（事务边界/异常处理/类型安全/测试有效性），MA2 已知 finding 作为输入复核而非重复审计。
- **不**做 view.xml vs 后端契约 drift — 归 A4.6（MA4 view drift 批次）。本审计审后端代码实现质量，不审前端消费。
- **不**做 owner doc vs 代码 drift — 归 A3.3/A3.4（已 done）。本审计的 owner doc drift 复核以 A3.4 已登记 finding 为输入。
- **不**做测试覆盖深度统计 — 归 A5.2（MA5 测试层）。本审计的"测试有效性"维度审**异常路径覆盖 + 断言强度**，非覆盖深度统计。
- **不**做权限注解完整性 — 归 A6.1/A6.2（MA6 安全层）。
- **不**做完工入库 GL 过账 Provider 实现质量（制造完工过账依赖 finance 域 Provider，其 Facade 实现质量归 A4.1a）——本审计复核 mfg 侧 ManufacturingIssuePostingDispatcher 调用点的错误传播与悬挂。
- **不**在本计划内批量修复代码缺陷 — P1 经 R2.0/R1.0 展开机制进入 MR2/MR1。本审计只识别缺陷 + 分类。
- **不**手改生成物或 ORM 源模型。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/manufacturing/state-machine.md` + `bom-and-routing.md` + `README.md`（roadmap A4.2a 指定 owner doc `docs/design/manufacturing/`，本切片锚点为工单状态机 + BOM/工艺路线）；`module-manufacturing/erp-mfg-service/`（工单/BOM 链路代码实现——审计对象）；`docs/audits/2026-07-28-0109-arm-ma2-mfg-work-order-jobcard-state-machine.md`（A2.6a 已知 finding——本审计输入）；`docs/audits/2026-07-28-1953-arm-ma3-owner-doc-vs-code-drift.md` §mfg（A3.4 已知 drift——本审计输入）
- Skill Selection Basis: `code-quality-audit-prompt.md`（roadmap A4.2a 指定此 skill——7 重点领域 + 严重性指南 P0-P3 + 发现按严重性排序。项目定制化层见 `docs/skills/README.md`）。与 A4.2b 不同结果表面（工单/BOM vs MRP/质量集成），独立计划。与 A2.6a 不同维度（代码实现质量 vs 业务正确性状态机），互补不重叠。
- Verification: 审计不改代码/文档，故无单测回归；报告产出即更新 `arm-index.md`。代码缺陷修复在 MR2/MR1 批量进行。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。本审计为代码静态审查 + 测试有效性抽样，不运行应用。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码/文档，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。代码静态审查无回归风险，build/test 门控为同型审计 plan 的标准 Closure 实践。

## Execution Plan

### Phase 1 - 工单与报工 / BOM 与工艺路线链路代码实现质量系统性审计（7 重点领域）

Status: completed
Targets: `module-manufacturing/erp-mfg-service/` 工单/BOM 链路代码（ErpMfgWorkOrderBizModel + ErpMfgWorkOrderProcessor + 审批轴 6 Processor + ErpMfgJobCardBizModel + ErpMfgJobCardProcessor + ErpMfgMaterialIssueBizModel + MaterialIssueStockMoveBuilder + ManufacturingIssuePostingDispatcher + ManufacturingIssueAcctDocProvider + MfgPostingExecutor + BomExpander + Routing/WorkCenter）；owner docs `docs/design/manufacturing/{state-machine,bom-and-routing}.md` + `README.md`
Skill: `code-quality-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线）；MA1 + MA2 + MA3 done（已知 finding 作为输入）；A2.6a done（状态机基线）；A3.4 done（owner-doc drift 基线）；A4.1a done（MA4 范式参照）。

- [x] 领域「架构和边界完整性」：核查工单/BOM 链路代码的跨域访问合规性——领料库存移动是否经 IErpInvStockMoveBiz Facade（非 daoFor 直访）/ 完工入库过账是否经 IErpFinVoucherBiz Facade / 报工 cost 回写是否合规 / BOM 展开读 WorkCenter 是否合规。复核 P1-MA1-022（aps 读 ErpMfgBom/ErpMfgBomOperation 跨域投影）运行时状态。标记边界违规站点。
      - Skill: `code-quality-audit-prompt.md`
- [x] 领域「核心实现正确性」：核查 WorkOrderProcessor 编排的事务边界健壮性（@BizMutation 自动事务 + checkAvailability 齐套校验的 TOCTOU）/ 领料 MaterialIssueStockMoveBuilder 库存移动构造正确性（OUTGOING + materialCost 回写）/ ManufacturingIssuePostingDispatcher tryPost 的异常吞咽与悬挂（复核 tryPost 吞异常同型根因——是否检查返回值/设 posted/派发告警）/ 报工 recordWork 的 laborCost 算术正确性 / BomExpander 多级展开的递归终止与成环检测 / 完工入库 totalCost/unitCost 重算。标记事务/幂等/异常悬挂/算术缺陷。
      - Skill: `code-quality-audit-prompt.md`
- [x] 领域「类型和契约质量」：核查审批轴 6 Processor 的参数返回契约一致性 / 领料/报工 cost 回写的 BigDecimal 类型安全 / 工单多币种四件套（复核 P1-MA1-001 propId 缺失运行时影响）。标记类型不匹配/契约漂移。
      - Skill: `code-quality-audit-prompt.md`
- [x] 领域「错误处理和操作安全」：核查工单/BOM 链路异常是否全部扩展 NopException + ErrorCode（`erp.err.mfg.*`）/ 齐套校验失败/质检门控拦截/报工超量的错误传播 / 超产 config 承诺缺失（复核 P1-MA3-041）。标记裸异常/ErrorCode 缺失/错误信息不足。
      - Skill: `code-quality-audit-prompt.md`
- [x] 领域「测试有效性」：抽样 manufacturing 30 测试中工单/BOM 相关测试（TestErpMfgWorkOrderEndToEnd / TestErpMfgWorkOrderStateMachine / TestErpMfgMaterialIssue / TestErpMfgMaterialIssueReversal / TestErpMfgBomExplosion / TestErpMfgScheduleToJobCard），核查**异常路径覆盖**（齐套不足/质检门控拦截/报工超量/领料超量/过账失败悬挂）+ 断言强度（是否仅断言 status 翻转还是校验 cost 回写数值/库存移动/凭证行）。标记测试空洞。
      - Skill: `code-quality-audit-prompt.md`
- [x] 领域「可维护性和未来变更风险」：核查 WorkOrderProcessor 编排复杂度（行数/圈复杂度）/ 审批轴 6 Processor 的对称性 / BomExpander 递归可维护性。标记 P2 可维护性风险。
      - Skill: `code-quality-audit-prompt.md`
- [x] 领域「自动化和防护覆盖」：核查工单/BOM 链路是否有 compliance checker 规则守护（R8 Processor 无 xbiz / R2 daoFor 跨域）/ 是否有测试门控防止回归（齐套校验/完工入库/领料过账）。标记防护缺口。
      - Skill: `code-quality-audit-prompt.md`
- [x] 产出审计报告 `docs/audits/2026-07-29-0024-arm-ma4-mfg-work-order-bom-code-quality.md`（含：7 领域逐项审查结果 / MA1/MA2/MA3 已知 finding 运行时复核 / P0-P3 finding 清单按严重性排序 / 每项含文件路径+行引用 / 裁决通过/失败 / 剩余风险）。
      - Skill: none

Exit Criteria:

> 审计报告是唯一可观察产物。完整仓库 `mvn test` 属 Closure Gates（见执行时规则 7）。

- [x] 7 重点领域逐项审查结果产出（每领域至少一句裁决，含"本领域无缺陷"）
- [x] MA1/MA2/MA3 已知 finding 运行时复核产出（每项标记"如 owner doc 声明"或"发现新代码层缺陷"）
- [x] P0-P3 finding 清单产出按严重性排序，每个含文件路径+行引用+严重性+缺陷描述+影响+目标 MR

### Phase 2 - finding 汇总交接 MR2/MR1 + 索引/矩阵更新

Status: completed
Targets: 工单/BOM 链路代码质量 finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.4「代码质量（MA4）」manufacturing 列
Skill: none

- Item Types: `Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [x] finding 汇总：全部缺陷 blocker/major 登记为 P1 至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA4-NNN`，起始编号 = A4.1a/A4.1b 已分配最大 P1-MA4-N + 1，避免命名空间碰撞；报告、领域、功能模块、缺陷描述、目标 MR2[代码类]/MR1[业务正确性类]、修复状态 todo）。与 MA2/MA3/A4.1a/A4.1b 已登记 P1 经交叉去重无冲突。
      - Skill: none
- [x] 分类裁决：代码实现质量 finding 目标 MR2；业务正确性类 finding 目标 MR1；活跃数据破坏走 P0 即时通道，在报告中明确标注。
      - Skill: none
- [x] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §2.4「代码质量（MA4）」行增 manufacturing 工单/BOM 片完成注记段（§2.4 无 per-domain 列，以注记段反映）。
      - Skill: none

Exit Criteria:

- [x] 所有缺陷 blocker/major 已登记 arm-index §P1 汇总（代码类 MR2 / 业务正确性类 MR1），待展开
- [x] 与 MA2/MA3/A4.1a/A4.1b 已登记 P1 经交叉去重无重复登记
- [x] arm-index 报告清单 + scope matrix 注记段已反映审计结论

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_05671a498ffe341EtvfjvUR8jZ`，独立 general 子代理，fresh-context，对照实时仓库逐项复核）。VERDICT: accept，无 BLOCKER。LIVE-REPO 复核：find 计数 29（=基线声称，含 7 测试）可复现；核心组件全部存在（ErpMfgWorkOrderBizModel/ErpMfgWorkOrderProcessor/BomExpander/MaterialIssueStockMoveBuilder/ManufacturingIssuePostingDispatcher PASS）；finding ID 真实（P1-MA1-001/P1-MA2-035/P1-MA3-040/041/044 均 PASS，mfg 归属正确）；owner docs 存在（state-machine.md + bom-and-routing.md）；scope matrix §2.4 无 per-domain 列（退出标准正确指向注记段）。逐项裁决：规则 1 基线诚实可复现 PASS / 规则 2 边界清晰与 A4.2b/A2.6a 无不当重叠 PASS / 规则 4+14 拆分正当（§1.3 边界 + A4.1a/A4.1b 先例）PASS / 规则 5/7 类型标注 Proof/Follow-up PASS / 规则 8/9 skill 正确 PASS / anti-slack 无禁用词 PASS / 命名 N=1 PASS / Plan Status draft PASS。P1-MA4 命名空间起始 = A4.1a/A4.1b max(006)+1 = 007（Phase 2 已正确声明）。Plan Status 转 active。

## Closure Gates

> 本计划主体是代码静态审查 + 测试有效性抽样（不改代码；产出为审计报告 + arm-index/scope-matrix 更新）。完整仓库验证在此处运行一次（同型审计 plan 的标准 Closure 实践）。代码缺陷修复在 MR2/MR1 批量进行；活跃数据破坏走 P0 即时通道。本审计只识别缺陷 + 分类。

- [x] 范围内行为完成（A4.2a 工单与报工 / BOM 与工艺路线链路代码质量审计报告产出 + arm-index 更新 + scope matrix 标记完成）
- [x] 相关文档对齐（审计报告、arm-index、scope matrix 结论已反映）
- [x] 已运行验证：代码静态审查无代码变更，build/test 门控仅作回归基线确认（同型审计 plan 的相同 Closure 实践）
- [x] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR2/MR1）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证（状态、阶段、门控、日志都一致）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留空作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### MRP/质量集成 / 成本核算 / 基因追踪代码质量（A4.2b）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计聚焦工单与报工 + BOM 与工艺路线功能模块；manufacturing 其余功能模块（MRP/DRP 引擎 / 质量集成与 NCR / 成本核算 / 基因追溯 / 差异计算）归 A4.2b（同批起草，S 级拆分 2/2）。若工单/BOM 审计中发现跨模块代码质量问题，标注交接 A4.2b。
- Successor Required: `yes`——A4.2b 执行时复核。

### 业务正确性/状态机（A2.6a）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计审代码**实现质量**（事务/异常/类型/测试）；工单/BOM 链路业务正确性/状态机归 A2.6a（已 done）。MA2 已知 finding 作为本审计输入复核。
- Successor Required: `no`——A2.6a 已 done。

### view.xml drift（A4.6）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 前端 view.xml 调用的 API/字段 vs 后端契约 drift 归 A4.6。本审计审后端代码实现质量。
- Successor Required: `yes`——A4.6 执行时复核 mfg view。

### 测试覆盖深度统计（A5.2）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计"测试有效性"维度审异常路径覆盖 + 断言强度；覆盖深度统计归 A5.2。
- Successor Required: `yes`——A5.2 执行时复核 mfg 测试深度。

## Closure

Status Note: 计划全部执行完成。Phase 1（7 重点领域系统性审计 + 审计报告产出）与 Phase 2（P1 汇总 arm-index + scope matrix 注记段）均已 done。审计不改代码（仅产审计报告 + 索引/矩阵/roadmap 更新）；build/test 门控经 `mvn install -DskipTests`（mfg-service + deps BUILD SUCCESS）+ `mvn test`（mfg-service 141 tests, 0 failures/errors/skipped）确认回归基线绿色。独立结束审计由 fresh-context 子代理执行，VERDICT: pass，零 BLOCKER。

Closure Audit Evidence:

- Auditor / Agent: independent general subagent, fresh context（did not execute this plan；task ses_056647055ffeteSteT4Vje1BL0）
- Evidence: Read-only closure audit performed 2026-07-29 against LIVE repo. Verified all 7 claims: (1) 审计报告 `docs/audits/2026-07-29-0024-arm-ma4-mfg-work-order-bom-code-quality.md` 有 7 重点领域裁决（§2.1–2.7）+ 9 项 MA1/MA2/MA3 运行时复核（§6）+ P1/P2 finding 清单含 file:line 引用 + §8 FAIL 裁决——spot-check 6 处源码声称（ErpMfgWorkOrderProcessor:227-239 差异吞咽 catch / :364 daoFor ErpMdMaterial / KitAvailabilityChecker:107 daoFor ErpInvStockBalance / ManufacturingIssuePostingDispatcher:107-113 try-catch + :163/:174/:188 / ProductionVarianceDispatcher:111-117）全部逐字确认；(2) arm-index P1-MA4-007/008/009 行存在，MR1/MR1/MR2 目标，与 P1-MA4-001..006（finance）及 P1-MA1-022 无碰撞；(3) 报告清单行 + §P1 汇总段已加，表格完整性无损（6 列）；(4) scope matrix §2.4 A4.2a 注记段已加；(5) roadmap A4.2a = done；(6) Plan Status/Phase statuses 全 completed，0 leftover `- [ ]`（25 `- [x]`）；(7) 交叉去重合理——P1-MA4-008 正确标「不重复计入 MR2」vs P1-MA1-022；P1-MA4-007 vs P1-MA4-004 为不同代码层相邻路径（mfg 完工触发 vs finance 期间 Processor）。**VERDICT: pass。**

Follow-up:

- manufacturing 代码质量全片终态在 A4.2b（MRP/质量集成/成本核算/基因追溯）收口——A4.2b 执行时复核 ProductionVarianceCalculator/CostRollupService/BatchGenealogyWriter/MrpEngine 实现质量 + P1-MA4-008 同型 daoFor 是否在 A4.2b 范围文件复现。
- P1-MA4-007/008/009 按 MR1/MR2 展开机制进入修复里程碑（非降级，不在此处阻塞）。
