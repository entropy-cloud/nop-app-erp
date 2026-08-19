# 2026-08-19-2040-2-rc-mr1-r1-81-82-drp-crossdock-leadtime-family drp 域越界修复族（越库状态机 + 提前期统计与供应商评分）

> Plan Status: active
> Mission: requirement-compliance
> Work Item: RC-R1.81 + RC-R1.82（MR1 越界项，drp 域收尾两件套）
> Last Reviewed: 2026-08-19
> Source: `docs/backlog/requirement-compliance-roadmap.md` MR1 行 RC-R1.81/82 + arm-index P1-RC-081/P1-RC-082
> Related: A1.48 审计 `docs/audits/2026-08-05-1400-3-rc-ma1-a1-48-drp-full.md` + A4.2 报告 `docs/audits/2026-08-07-1410-rc-ma4-a4-2-170-173-drp-runtime.md`；P2-RC-072 联合变分联动注记
> Audit: required

## Authorization Ledger

- RC-R1.81：**A 类 + B 类混合**（2026-08-12 批量裁决 A 类 ORM 纯加性授权「drp: RC-R1.81（ErpInvDrpCrossDock 加 matchingStrategy 列）」+ BizModel 逻辑/config 预授权；跨域 inv/pur/sal 事件协调义务经 D1 决策项 + data-dependency-matrix Java 层边登记履行，对齐 R1.76/77/85 先例）
- RC-R1.82：**A 类 + B 类混合**（2026-08-12 批量裁决 A 类 ORM 纯加性授权「drp: RC-R1.82（新增供应商评分汇总实体）」+ BizModel 逻辑/统计查询预授权；跨域 purchase 收货事件协调经 D4 决策项 + matrix 登记；leadTimeStdDev 持久化载体经 D5 裁决收敛在 A 类授权内，越界回落双独立子 agent 批准）

## Current Baseline

- **P1-RC-081（UC-DRP-07 越库全缺）**：`ErpInvDrpCrossDockBizModel`/`ErpInvDrpDockAppointmentBizModel` 均 17 行裸 CrudBizModel；ORM 字段就绪（status propId15 + matchedAt/loadedAt/stagingLocationId + dict `erp-inv/drp-xdock-status` 已存在）但零状态迁移/匹配/超时/质检/config 门控业务方法；无 matchingStrategy 列。L1 `use-cases.md:75-85` 要求状态机 PENDING→STAGING/MATCHED→LOADED→COMPLETED + 三匹配策略[PRE_ALLOCATED/ON_RECEIPT/MANUAL] + 超时 24h 转正常入库 + 暂存区质检快检 + `erp-inv.drp-xdock-enabled` 门控。注意：物料级「需质检」载体不存在（ErpMdMaterial 无 inspection 列；现行质检门控为 billType config 维度 `erp-qua.mandatory-inspection-bill-types` + 物料级检验模板），越库质检守卫载体须 D2 裁决。
- **P1-RC-082（UC-DRP-08 提前期统计与评分全缺）**：`ErpInvDrpLeadTimeRecordBizModel` 17 行裸 CrudBizModel；ORM 记录字段就绪（actualLeadTime/expectedLeadTime/varianceDays/isOnTime/earlyLateFlag）但 **dict `erp-inv/drp-lt-flag` 不存在**（erp-drp-meta 现仅 drp-xdock-status/drp-ss-method/drp-service-level 三 dict，earlyLateFlag 载体须本计划物化）；零自动计算/统计/联合变分/回写/评分方法；无采购收货事件监听；无供应商评分汇总实体。`SafetyStockEngine:49` javadoc「联合变分归 Deferred」系 AI 自标（arm-index §4 三判据裁决不成立）。
- drp 域已落地能力（不得回归）：DrpEngine 净需求 + SafetyStockEngine 三法（STATISTICAL/SIMPLE/DDMRP）+ confirmWriteback 人工审查门 + DrpReleaseService 释放链 + 仿真，erp-drp-service 当前 12 测试类全绿基线（顶层 8：TestErpDrpEngine/ForecastSource/InventoryIntegration/PlanCrudSmoke/SafetyStock/ScheduleRelease/Simulation/WiringRegression + statemachine/ 4）。
- 关联 P2：P2-RC-072 联合变分 Deferred 与本计划 R1.82 联动（触发条件 = 提前期 σ 统计可计算）；P2-RC-069/070/071 维持 watch-only 不在本计划范围。

## Goals

- P1-RC-081：越库执行引擎落地——状态机 mutation 族（owner doc 状态机全部合法边含直连 PENDING→MATCHED、PENDING→STAGING→MATCHED、MATCHED→LOADED→COMPLETED、各态→CANCELLED 守卫）+ matchingStrategy 列（A 类授权载体）与三策略实现 + 超时转正常入库调度 + `erp-inv.drp-xdock-enabled`/`drp-xdock-staging-timeout`/`drp-xdock-default-strategy` 三 config 消费 + 暂存区质检快检分支（载体经 D2 裁决，config-gated）。
- P1-RC-082：提前期跟踪与评分落地——采购收货确认→actualLeadTime 自动计算写 ErpInvDrpLeadTimeRecord（含 `erp-inv/drp-lt-flag` dict 物化）+ 统计分析（μ/σ/准时率/变异系数，供应商+物料粒度）+ 联合变分公式接入 SafetyStockEngine（σ_lt 来源 = LeadTimeRecord 统计；中/高变异档统一采用联合变异公式，高档「额外缓冲」量化系数无 L1/owner doc 依据，显式简化为联合变异值，记入 owner doc 注记）+ ErpDrpParameter 动态回写建议 + 供应商可靠性评分汇总实体（A 类授权载体）与四维评分（无样本维度得分记 0 并在汇总行标注样本缺失——边界语义见 Proof）。
- 全部配套 dedicated 测试 + owner doc 实现注记 + arm-index 两行 → done（+ P2-RC-072 联合变分分量闭合注记）。

## Non-Goals

- 月台预约调度引擎（ErpInvDrpDockAppointment 冲突检测/预到达通知/超时释放——cross-dock.md 月台预约节；L1 UC-DRP-07 未列月台验收标准，登记 successor）
- ASN/b2b 入站自动识别 crossDockFlag（cross-dock.md 方式 2；L1 主路径为 DRP 计划行标记 + 收货时匹配 + 手工标记，ASN 自动识别归 b2b 集成 successor）
- 提前期趋势月度/季度报表模板与仪表盘（统计 API 落地，报表渲染归报表子系统后续）
- P2-RC-069/070/071（0 值行/采购单价=0/参数校验，watch-only 登记不强制）
- 评分影响策略的自动执行（A/B/C/D 等级对审批流的自动放宽/收紧——评分产出登记，策略执行归 purchase 审批配置 successor）

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/design/drp/use-cases.md`（L1 真相源，不改）+ `docs/design/drp/cross-dock.md` + `docs/design/drp/lead-time-tracking.md` + `docs/design/drp/safety-stock-optimization.md`（联合变分集成注记）+ `docs/architecture/data-dependency-matrix.md`
- Skill Selection Basis: `nop-backend-dev`（状态机 mutation/跨域 IBiz/Processor 范式 + 反模式自检）+ `nop-testing`（分域测试范式）；统计公式与评分算术无匹配技能（Skill: none，算术以 owner doc 公式为准 + 边界单测）。

## Infrastructure And Config Prereqs

- 无新端口/外部服务。消费 cross-dock.md §配置点 五键（`erp-inv.drp-xdock-enabled` 默认 false / `drp-xdock-staging-timeout` 24 / `drp-xdock-default-strategy` ON_RECEIPT / dock 两键归月台 Non-Goal 不消费）；R1.82 新增容差系数 config（默认 0.1，对齐 lead-time-tracking.md:77）。
- ORM 变更走 `mvn clean install -DskipTests` 增量重生成链。

## Execution Plan

### Phase 1 — P1-RC-081 越库状态机与匹配策略

Status: planned
Targets: `module-drp/model/app-erp-drp.orm.xml`（matchingStrategy 列）+ `module-drp/erp-drp-service/`（CrossDock BizModel/Processor/job）+ 双独立子 agent 批准记录（§ORM Approvals）
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: 双独立子 agent 对 matchingStrategy 纯加性可空列（无默认无索引无 UK，dict 复用或新 dict 值属数据变更）批准。

- [ ] Add: ORM `ErpInvDrpCrossDock.matchingStrategy` 列（A 类授权；null = 未声明策略，读取时回落 `erp-inv.drp-xdock-default-strategy`）。
      - Skill: `nop-backend-dev`
- [ ] Decision: D1 跨域触发模型裁决——收货识别越库标记的载体（选项 A：purchase receive approve Processor 后置调 drp Facade；注意与 R1.61 候选 C 的差异：R1.61 复用了既有 purchase→projects 边，purchase→drp 现无 Java 层边，选项 A 伴随新 pom 边 + matrix 登记；选项 B：drp 侧拉取扫描）。裁决依据 data-dependency-matrix 允许边方向，结果 + matrix Java 层边登记写入 owner doc。
      - Skill: none
- [ ] Decision: D2 越库质检守卫载体裁决——ErpMdMaterial 无物料级「需质检」列（禁新增 master-data 列）。选项 A：物料存在有效检验模板（ErpQaInspectionTemplate 匹配，quality 域只读）即视为需质检；选项 B：复用 `erp-qua.mandatory-inspection-bill-types` config 维度（越库出库视为一种 billType）；选项 C：越库记录自身列（matchingStrategy 同批 A 类）。裁决结果 + 残留风险记入 owner doc；质检分支 config-gated（`erp-inv.drp-xdock-quality-gate-enabled` 默认 false）。
      - Skill: none
- [ ] Add: 状态机 mutation 族——receiveMark（收货识别→STAGING + inboundMoveId 回写）/ match（三策略：PRE_ALLOCATED 读 drpLine 预分配目标 / ON_RECEIPT 扫描待出库销售订单按承诺发货日期 ASC + 创建时间 ASC / MANUAL 指定目标单 → MATCHED + targetBill 回写；收货即匹配场景支持直连 PENDING→MATCHED，对齐 owner doc 状态图 `[inbound 到达 + 匹配目标订单] → MATCHED` 边）/ load（生成出站移动 + outboundMoveId 回写 → LOADED）/ complete（出库确认 → COMPLETED）/ cancel（→ CANCELLED）；全迁移经 owner doc 状态机守卫，`erp-inv.drp-xdock-enabled` 默认 false 总门控（功能整体 opt-in，与 owner doc 默认值一致）。
      - Skill: `nop-backend-dev`
- [ ] Add: 超时回退调度——nop-job 简单 job bean（R1.38 范式：cron 空值跳过 + limit 200 + 逐条失败隔离）扫描 STAGING 超 `drp-xdock-staging-timeout` 小时未匹配记录 → 转正常入库（staging→正常存储位移动单）+ CANCELLED；质检快检分支——按 D2 裁决载体判定「需质检」物料，match 前置守卫（未快检拒绝匹配，提示暂存区快检；config-gated 默认 false）。
      - Skill: `nop-backend-dev`
- [ ] Proof: `TestErpDrpCrossDock` 至少 9 组——状态机合法迁移（含直连 PENDING→MATCHED）/非法迁移拒绝、三策略各自匹配成功与无匹配、超时转正常入库、质检守卫（D2 载体双路径 + config 关闭跳过）、config 关闭整体跳过、CANCELLED 终态、（并发组：双收货同记录匹配幂等）。
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 越库状态机 + 匹配 + 超时 + 质检 + config 门控运行时可观察（成功/拒绝模式）
- [ ] erp-drp-service 分域测试全绿 + TestErpAllJobYamlLoading 计数同步（若注册新 job.yaml）

### Phase 2 — P1-RC-082 提前期统计与供应商可靠性评分

Status: planned
Targets: `module-drp/model/app-erp-drp.orm.xml`（供应商评分汇总实体）+ `module-drp/erp-drp-service/`（LeadTime 统计/评分/回写 + SafetyStockEngine 联合变分）+ purchase 侧接线点（D4 裁决后）+ §ORM Approvals
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1 无硬依赖（可并行排程，D4/D5 裁决先行）；双独立子 agent 对评分汇总纯加性新实体批准。

- [ ] Add: ORM 新增供应商评分汇总实体（supplierId+materialId 粒度聚合载体：μ/σ/准时率/变异系数/四维得分/总分/等级[A/B/C/D]/统计窗口/样本数 + 自有 UK(supplierId, materialId)——A 类授权「新增供应商评分汇总实体」）。
      - Skill: `nop-backend-dev`
- [ ] Decision: D4 收货确认触发裁决——选项 A：purchase receive approve Processor 后置 protected step 调 drp Facade（actualLeadTime = DATEDIFF(receiptDate, orderDate)，镜像 R1.61 接线方向）；选项 B：drp 侧事件拉取。结果 + matrix Java 层边登记写入 owner doc。
      - Skill: none
- [ ] Decision: D5 σ_lt 持久化载体裁决——选项 A：统计查询时实时计算（不留列，SafetyStockEngine 接联合变分时按 supplier+material 现算 σ/μ）；选项 B：ErpDrpParameter 加 leadTimeStdDev 列（越界回落双独立子 agent 批准）。倾向 A（零越界、统计窗口可控）；裁决与残留风险（实时计算的样本窗口语义）记入 owner doc。
      - Skill: none
- [ ] Add: LeadTime 记录自动计算——按 D4 接线写 ErpInvDrpLeadTimeRecord（varianceDays/isOnTime/earlyLateFlag 按容差系数 config 计算，earlyLateFlag 载体 = 物化 dict `erp-inv/drp-lt-flag` 三值 ON_TIME/EARLY/LATE，属 meta 数据变更）+ 幂等守卫（同 purchaseOrderCode+materialId 不重复落记录）。
      - Skill: `nop-backend-dev`
- [ ] Add: 统计分析 + 评分——@BizQuery 统计（供应商级/供应商+物料级/物料级 μ/σ/准时率/中位数/样本数）+ 评分计算（四维权重 40/30/20/10，数量准确率读采购收货偏差[drp→pur 只读]、质量合格率读 quality 来料检验[drp→qa 只读]——两条只读 Java 边按 matrix §2.4 范式登记；等级 A/B/C/D 阈值 90/75/60；无样本维度得分记 0 且汇总行标注样本缺失，不静默忽略）+ 回写评分汇总实体 + `recalculateLeadTimeStats` 触发 mutation。
      - Skill: `nop-backend-dev`
- [ ] Add: 联合变分接入 SafetyStockEngine——STATISTICAL 法在 σ_lt 可得（样本数 ≥ 5，L1 UC-DRP-08 字面「样本 <5 降级」；订单/收货日期缺失行不入统计）且 σ/μ > 0.2 时按 `Z × √(σ_d² × μ_lt + μ_d² × σ_lt²)` 计算（lead-time-tracking.md 调整策略表三档），confirmWriteback 人工审查门保持不变；ErpDrpParameter 回写建议（replenishmentLeadTime←μ_lt / safetyStock←联合变分值）经既有确认回写链（不绕过人工门）。
      - Skill: `nop-backend-dev`
- [ ] Proof: `TestErpDrpLeadTimeStats` 至少 9 组——自动计算+幂等、容差三档 flag、统计指标数值断言（构造样本 μ/σ/准时率精确值）、评分四维合成+等级边界（90/75/60 边界值）、无样本维度得分 0 + 标注边界、联合变分低变异走标准公式/中高变异统一走联合公式数值断言、样本不足降级、confirmWriteback 人工门保持、（并发组：统计重算幂等）。
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 提前期自动记录 + 统计 + 评分 + 联合变分 + 回写建议运行时可观察（成功/降级模式）
- [ ] erp-drp-service 分域测试全绿 + purchase 侧（若接线）分域零回归

## ORM Approvals（双独立子 agent 批准记录 — 执行期填充）

> A 类授权 + 保护区域 dual-agent-approval：两个 fresh session 子 agent 分别独立复核 matchingStrategy 可空列与评分汇总新实体（纯加性、零既有语义改动、零删除/迁移），各自 APPROVE 后方可执行 ORM 编辑。

- [ ] Approver 1（RC-R1.81 列 / RC-R1.82 实体，session id + 结论 + 日期）：______
- [ ] Approver 2（RC-R1.81 列 / RC-R1.82 实体，session id + 结论 + 日期）：______

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_fe5f1369effeWCWyY4feiryIqX) because ① 基线误报 dict `erp-inv/drp-lt-flag` 已存在（实仓三 dict 无此件）② ASN Deferral 触发条件已被实仓 b2b ASN 自动建收货流程满足（失效）③ `inspection_required` 载体不存在致质检守卫条目不可实现；另 8 条非阻塞注记（测试类计数 9→12、五/三 config 措辞、高档额外缓冲简化声明、评分只读边登记、R1.61 镜像差异注记、直连 PENDING→MATCHED 边、无样本维度边界用例）。
- Independent draft review iteration 2: accept (ses_fe5e8af33ffeTNEQLneZByjNqQ) after 修订复核——三项 round-1 blocker 逐一实仓验证闭合（dict 不存在声明 + Phase 2 物化 / ASN Deferral 重裁决为 b2b 协同立项触发 / D2 质检载体三选项决策项 + master-data 列禁令），7 条非阻塞注记全吸收；round-2 仅余枚举完整性小注（drp→sal 边、样本阈值钉死、dock deferral 措辞），已在本轮补齐（closure gates 增 ON_RECEIPT drp→sal 只读边 + 样本阈值 = 5 钉死）。共识达成，草案可执行。

## Closure Gates

> 完整仓库验证在此处运行一次。

- [ ] 范围内行为完成（P1-RC-081/082 全部验收点落地）
- [ ] 相关文档对齐（cross-dock.md 实现注记 + lead-time-tracking.md 实现注记[含高档额外缓冲简化声明] + safety-stock-optimization.md 联合变分集成注记 + data-dependency-matrix D1/D4 边 + 评分只读 drp→pur/drp→qa 两边 + ON_RECEIPT 策略 drp→sal 只读边登记 + arm-index P1-RC-081/082 → done (RC-R1.81/82) + P2-RC-072 联合变分分量闭合注记 + roadmap 行状态同步 + docs/logs/ 当日条目）
- [ ] 已运行验证：erp-drp-service 分域 `mvn test`（+ purchase 侧分域）+ 全仓 `mvn clean install -DskipTests` + 全仓 `mvn test` + `bash docs/audits/nop-compliance-checker.sh`（若 actual > baseline 则 baseline-raise 登记 per-site 证据）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 月台预约调度引擎（dock 冲突检测/预到达通知/超时释放）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: L1 UC-DRP-07 验收标准未含月台维度；owner doc 月台节为可扩展设计；实体 ErpInvDrpDockAppointment 已在 ORM 但非 L1 强制。
- Successor Required: no（与 2026-08-19-2040-1 logistics 计划 §Deferred 月台对接互为触发参考）

### ASN/b2b 入站自动识别 crossDockFlag

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: L1 主路径三触发方式中本计划覆盖计划行标记（方式 1 经 matchingStrategy/预分配）+ 收货匹配（方式 3 之收货时匹配变体）+ 手工标记；方式 2（ASN 自动识别）要求 b2b 域 `ErpB2bAsnCreateReceiveFromAsnProcessor.createReceiveFromAsn` 自动建收货流程（`erp-b2b.asn-auto-create-receive` config-gated，已落地）中识别越库标记并联动物化 CrossDock——属 b2b 域协同改造非 drp 域单侧可闭合。
- Successor Required: yes（触发条件：越库 × b2b ASN 自动建收货集成裁决立项时）

### 评分影响策略自动执行（A/B/C/D 等级联动审批放宽/收紧与 SS 幅度 ±1σ/±2σ）

- Classification: `watch-only residual`
- Why Not Blocking Closure: roadmap 行 RC-R1.82 修复义务止于「评分」产出；SS 幅度影响经 confirmWriteback 人工审查门间接可达（计划员参考等级人工调整）；等级→审批流自动联动依赖 purchase 审批配置载体（现无等级消费方）。
- Successor Required: yes（触发条件：purchase 审批策略消费供应商等级的需求立项时）

### ErpDrpParameter.leadTimeStdDev 物化列（若 D5 选 A）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 实时统计满足联合变分输入需求；物化列仅优化大样本查询性能。
- Successor Required: no（触发条件：LeadTimeRecord 万级以上样本统计时延不可接受时）

## Closure

Status Note: pending

Closure Audit Evidence:

- Auditor / Agent: pending
- Evidence: pending

Follow-up:

- 无（已确认缺陷不入此节）
