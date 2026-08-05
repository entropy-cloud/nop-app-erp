# 2026-08-06-0245-2 rc-ma1-a1-43-maintenance-f2-visit-sparepart maintenance-F2 维护访问与备件消耗需求符合性审计

> Plan Status: completed
> Last Reviewed: 2026-08-05
> Mission: requirement-compliance
> Work Item: A1.43（MA1 需求追踪矩阵审计 — maintenance-F2 维护访问全流程(设备状态联动) + 备件消耗闭环(出库+凭证)）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.43
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.43 的 0.2 依赖）、`2026-08-06-0245-1-rc-ma1-a1-42-maintenance-f1-scheduling-conflict.md`（A1.42 同批 N=1，maintenance 域首个 RC 切片；本切片 N=2 为 maintenance 域 F2 访问与备件）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点被审功能现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.43 给出 UC 清单 = `UC-MAIN-03/04`（2 UC），覆盖率 `✅ 一致`（无基线分歧 D-xx）。本切片与 A1.42/A1.44 同批（maintenance 域 3 切片），本切片覆盖 F2 访问与备件 2 UC。

- **L1 需求契约（权威真相源）**：`docs/design/maintenance/use-cases.md`（机制细节引用 `state-machine.md §2`、`equipment-integration.md §二/§三`，L2 设计参考）：
  - **UC-MAIN-03 维护访问全流程**（`:49`）：行为链路：维护访问 SCHEDULED → IN_PROGRESS（开始，设备→MAINTENANCE）→ 执行任务 + 消耗备件 → COMPLETED（完成，设备→RUNNING/IDLE）。可验证断言（`state-machine.md §2`）：维护访问 IN_PROGRESS 时：关联设备.状态 = MAINTENANCE；COMPLETED 时：设备.状态 恢复（RUNNING/IDLE，取决于排产）；设备状态由维护访问状态驱动（见 equipment-integration §三）。
  - **UC-MAIN-04 备件消耗闭环**（`:71`）：维护访问记录备件消耗 → 调用 IErpInvStockMoveBiz.generateConsumptionMove（出库）→ 库存余额[备件] -= 消耗量 → 生成凭证：借 维修费用，贷 存货；备件不足 → 校验失败（见 ../inventory cross-domain）。

- **L3 代码实现现状（实测，`module-maintenance/erp-mnt-service`）**——**维护访问状态机完整(R6.7 per-mutation) + 设备状态联动 start 侧已实现(complete 侧待核) + 备件消耗出库+过账已实现(P1-MA2-074 resolved R1.16)**：
  - **UC-MAIN-03 维护访问全流程（✅ 状态机完整，⚠️ 设备状态联动 complete 侧待核）**：`ErpMntVisitBizModel.java`（薄 facade）委派 4 per-mutation Processor（R6.7）：`ErpMntVisitScheduleProcessor`#schedule（DRAFT→SCHEDULED，:67）+ `ErpMntVisitStartProcessor`#start（SCHEDULED→IN_PROGRESS，:24 设状态翻转 + startTime 兜底 + **设备状态联动（UNDER_MAINTENANCE）**，javadoc:10 显式）+ `ErpMntVisitCompleteProcessor`#complete（IN_PROGRESS→COMPLETED，:36）+ `ErpMntVisitCancelProcessor`#cancel（→CANCELLED + MAINTENANCE_LABOR 凭证红冲 config-gated 对称，:13/32/35）。**待核**：①start 侧设备状态写 UNDER_MAINTENANCE（实现用常量名，须核 ORM dict 是否对齐 L1"MAINTENANCE"语义）②**complete 侧是否恢复设备状态至 RUNNING/IDLE**（grep ErpMntVisitCompleteProcessor 设备状态恢复站点——L1 明确"COMPLETED 时设备.状态恢复"，若 complete 不恢复则候选缺口）③设备状态经 `IErpMntEquipmentBiz.changeStatus` 或直接 setStatus（ErpMntEquipmentBizModel.changeStatus:20 存在，须核调用链）。
  - **UC-MAIN-04 备件消耗闭环（✅ 出库+过账已实现，⚠️ 余额断言+不足校验+凭证行级断言待核）**：`ErpMntSparePartUsageBizModel.java`（薄 facade）委派 `ErpMntSparePartUsageConfirmProcessor`#confirm（消耗确认）+ `ErpMntSparePartUsageReverseConfirmProcessor`#reverseConfirm（红冲 MAINTENANCE_ISSUE 凭证 config-gated 对称，:27 javadoc）。备件出库经 `IErpInvStockMoveBiz.generateConsumptionMove`（须核 confirm 调用链是否真正触发库存出库移动单）；过账经 `MaintenanceIssuePostingDispatcher`（备件消耗，MAINTENANCE_ISSUE 凭证 借维修费用/贷存货）+ `MaintenanceIssueAcctDocProvider`（createFacts）。**P1-MA2-074 resolved R1.16**：`MaintenanceIssuePostingDispatcher.tryPost` 曾吞异常致 posted=false 悬挂，已修复（complete 检查 postLabor 返回值 + 失败告警，对齐 §去重协议只补需求视角差异）。**待核**：①库存余额[备件] -= 消耗量断言（测试是否断言库存减少）②备件不足校验失败路径（调 inventory cross-domain validateAvailable 抛 ERR）③凭证科目映射（借维修费用科目/贷存货科目）+ 行级断言强度。

- **L4 测试证据现状**（`module-maintenance/erp-mnt-service/src/test`）：
  - UC-MAIN-03：`TestErpMntVisitRequestStateMachine.java`（visit + request 状态机迁移）+ `TestErpMntVisitCancelReversal.java`（cancel + 凭证红冲）+ `TestErpMntVisitCrudSmoke.java`（CRUD 冒烟）；须执行时核验是否断言"IN_PROGRESS 时设备=MAINTENANCE"+"COMPLETED 时设备恢复"。
  - UC-MAIN-04：`TestErpMntSparePartPosting.java`（备件消耗过账）+ `TestErpMntSparePartUsageReversal.java`（红冲）+ `TestErpMntSparePartAndSchedule.java`；须执行时核验是否断言"库存余额减少"+"备件不足校验"+"凭证科目/行级"。
  - E2E：`tests/e2e/dashboards/maintenance.{smoke,value}.spec.ts`（看板域，不覆盖访问/备件链路）；MA5 `2026-07-29-1430-arm-ma5-{test-isolation,e2e-effectiveness}.md` 评级待引用。
  - **缺口**：UC-MAIN-03 complete 侧设备状态恢复断言 + UC-MAIN-04 库存余额/不足校验/凭证行级断言 须执行时确认强度。

- **L5 既有证据（MA2 复用输入，方法论 §去重协议）**：
  - **无 maintenance 专属 MA2 状态机报告**（MA2 报告零 maintenance 访问/备件行为证据）。本切片为 maintenance 访问+备件行为的首份证据（与 A1.42 并列 maintenance 域首份）。
  - **maintenance 相关既有 finding**：`P1-MA2-074`（maintenance Labor/Issue 过账 tryPost 吞异常致 posted=false 悬挂，resolved R1.16——config-gated 默认 OFF + complete 检查返回值 + 告警派发；**备件消耗过账吞异常维度已 resolved，本切片只补需求契约视角差异**）；`P1-MA1-011/013`（ErpMntVisit propId，fixed）；`P1-MA1-022`（跨域 daoFor：MaintenanceIssuePostingDispatcher daoFor ErpInvStockMove/ErpInvStockLedger，resolved plan 2026-07-29-2225-1，平台一致性维度不重审）。**无任何 UC-MAIN-03/04 需求符合性 finding**。
  - **本切片须声明与 MA2 报告差异增量**（报告段落 9）：无 maintenance 专属 MA2 报告；P1-MA2-074 引作备件过账吞异常已 resolved 证据（不重审过账吞异常维度），只补需求视角差异（设备状态联动 complete 侧 / 备件余额断言 / 不足校验路径）。

- **arm-index 既有 finding 衔接**：grep arm-index maintenance/mnt/UC-MAIN/visit/spare-part/备件/consumption → **无 UC-MAIN-03/04 finding**。P1-MA2-074/P1-MA1-011/013/022 非 UC-MAIN 维度（复用注记）。本切片须 grep arm-index maintenance visit status/equipment status/spare part/consumption/MAINTENANCE_ISSUE 同域同控制点后裁决复用 or 新建 `P*-RC-xxx`（续编，执行时取最新——当前至 P2-RC-059 / P1-RC-063）。

- **保护区域**：本审计为**只读审计**。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按方法论 §10。本切片候选偏差（设备状态联动 complete 侧 / 备件余额断言 / 不足校验）多为**代码逻辑**类（预授权）；**备件消耗过账触及会计保护区域**（MAINTENANCE_ISSUE 凭证借维修费用/贷存货），其修复沿用 P1-MA2-074 ask-first 先例（已 resolved R1.16，新增需求视角偏差若触及凭证科目映射/VoucherFact 须 ask-first）；须在报告逐项标注触及保护区域。

- **剩余差距**：A1.43 切片五级追踪审计报告缺失 = MA4 及 MR1 该切片证据缺口来源。本计划产出 A1.43 报告并登记 finding，解除 maintenance 访问与备件证据缺口。

## Goals

- 产出 A1.43 切片审计报告 `docs/audits/2026-08-06-0245-2-rc-ma1-a1-43-maintenance-f2-visit-sparepart.md`，含方法论 §6 **9 段全部内容**。
- 对 UC-MAIN-03/04 逐条核验**每条验收标准**（完整枚举，§3）：UC-MAIN-03 SCHEDULED→IN_PROGRESS 设备→MAINTENANCE / 执行任务+消耗备件 / COMPLETED 设备→RUNNING/IDLE / 设备状态由访问状态驱动；UC-MAIN-04 记录备件消耗 / generateConsumptionMove 出库 / 库存余额-=消耗量 / 凭证借维修费用贷存货 / 备件不足校验失败。
- 对候选缺口给出分级结论：①UC-MAIN-03 **complete 侧设备状态恢复至 RUNNING/IDLE**（待执行时核验 ErpMntVisitCompleteProcessor——L1 明确"COMPLETED 时设备恢复"，若缺失倾向 **P1** §2 ①/③；修复为 BizModel 代码逻辑预授权，若触及设备状态 dict 映射则核 ORM）；②UC-MAIN-03 设备状态常量 UNDER_MAINTENANCE vs L1"MAINTENANCE"语义对齐（cosmetic 倾向 P2 或接受）；③UC-MAIN-04 **库存余额-=消耗量断言 + 备件不足校验失败路径 + 凭证科目/行级断言**（待执行时核验测试断言强度，缺失倾向 P2 §2 ⑤ 仅冒烟，或 P1 若功能缺失）；④UC-MAIN-03 状态机迁移 + UC-MAIN-04 出库+过账主路径倾向**接受**（P1-MA2-074 已 resolved）——按 §2 判据定级，若为 P0/P1 则新建 `P*-RC-xxx`（续编）并按 §10 触发 MR0/MR1（本计划仅登记，不实施修复；**会计过账类须 ask-first**）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区；audit reports 表新增 A1.43 行）。

## Non-Goals

- **不修复 finding**（修复属 MR0/MR1；本计划是审计）。
- **不修改真相源**（§9 冻结条款——分歧记入报告，不直改 use-cases.md/state-machine.md/equipment-integration.md/product-scope.md）。
- **不修改代码/ORM/api.xml**（只读审计）。
- **不审计 maintenance-F1/F3**（A1.42/A1.44 各自独立 plan；A1.43 只覆盖 UC-MAIN-03/04）。
- **不重审 P1-MA2-074 过账吞异常维度**（§去重协议：resolved R1.16 过账吞异常悬挂维度裁决不重审；本切片只引用其已修复现状，从需求契约视角审设备状态联动/备件余额断言/不足校验）。
- **不重审 P1-MA1-011/013/022**（propId/跨域 daoFor，非 UC-MAIN 维度，复用不复审）。
- **不执行 MA4 运行时探针展开**（本计划只产出静态存疑点清单）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.43 工作项）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.43 UC 锚点）+ `docs/design/maintenance/use-cases.md`（L1 真相源）+ `docs/design/maintenance/state-machine.md §2`+`equipment-integration.md §二/§三`（L2 设计参考，非真相源）+ `docs/audits/arm-index.md`（finding 衔接）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。其必需输入均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。若需即时行为确认可跑既有 JUnit（如 `mvn test -pl module-maintenance/erp-mnt-service -Dtest=TestErpMntVisitRequestStateMachine,TestErpMntVisitCancelReversal,TestErpMntSparePartPosting,TestErpMntSparePartUsageReversal`）。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论

Status: completed
Targets: `docs/audits/2026-08-06-0245-2-rc-ma1-a1-43-maintenance-f2-visit-sparepart.md`（产出 §1-§5）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done

- [x] `Proof` 对 UC-MAIN-03/04 **逐验收标准一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:49/71` 验收标准原文；L2 引用 `state-machine.md §2`+`equipment-integration.md §二/§三`（标注"设计参考，冲突以 L1 为准"）；L3 引用 `ErpMntVisitBizModel`#schedule/start/complete/cancel + `ErpMntVisitStartProcessor`/`ErpMntVisitCompleteProcessor`/`ErpMntVisitCancelProcessor` + `ErpMntEquipmentBizModel`#changeStatus + `ErpMntSparePartUsageBizModel`#confirm/reverseConfirm + `ErpMntSparePartUsageConfirmProcessor` + `MaintenanceIssuePostingDispatcher`+`MaintenanceIssueAcctDocProvider` + IErpInvStockMoveBiz.generateConsumptionMove 跨域调用链（含行号）；L4 引用 `TestErpMntVisitRequestStateMachine`/`TestErpMntVisitCancelReversal`/`TestErpMntSparePartPosting`/`TestErpMntSparePartUsageReversal`#method（注明断言强度）；L5 标注无 maintenance 专属 MA2 报告 + P1-MA2-074 备件过账已 resolved 复用。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 重点核验**候选缺口**（逐条对照）：UC-MAIN-03 start 侧设备→UNDER_MAINTENANCE（✅ start:24 javadoc:10）+ **complete 侧设备恢复 RUNNING/IDLE**（✅ 已实现 ErpMntVisitCompleteProcessor:31→restoreToRunning——恢复 RUNNING；IDLE 分支缺失归 P2-RC-061）+ 设备状态常量语义对齐（UNDER_MAINTENANCE vs L1"MAINTENANCE"——cosmetic 语义一致记 §9）；UC-MAIN-04 generateConsumptionMove 出库调用链（✅ SparePartIssueService#issue→IErpInvStockMoveBiz.generateMove(OUTGOING)，方法名差异 cosmetic §9）+ 库存余额-=消耗量（✅ 跨域 inventory bookkeeper.bookCompletion，SP-2 运行时确认）+ 备件不足校验失败（✅ 跨域 inventory validateAvailable ERR_AVAILABLE_INSUFFICIENT 默认禁负库存，SP-3 运行时确认）+ 凭证科目映射借维修费用/贷存货（✅ MaintenanceIssueAcctDocProvider.createFacts Dr6602/Cr1403 行级强断言）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` 按 §2 判据对 UC-MAIN-03/04 给出符合性结论（取最高）：UC-MAIN-03 = **P2**（start/complete/驱动 主路径接受 + complete 侧设备恢复 RUNNING **已实现**[纠正计划基线「待核倾向 P1」假设]，候选缺口收窄为 IDLE 恢复分支缺失 **P2-RC-061** §2 P2① 次要验收标准；§4 三判据复核均不成立但影响限于 IDLE 边界非数据破坏，watch-only 声明 Q4 张力）；UC-MAIN-04 = **接受**（出库+余额+凭证+不足校验主路径全满足，P1-MA2-074 过账吞异常维度 resolved R1.16 复用，命名差异 cosmetic §9）。每结论列明命中判据编号 + 三源对照 + §4 三判据复核 + 触及保护区域标注（**备件过账凭证科目/VoucherFact 类须 ask-first**——本切片无新增会计过账变更；P2-RC-061 修复视形态：纯逻辑预授权 / ORM 快照列 ask-first）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] 报告 §1-§5 已落盘：UC-MAIN-03/04 矩阵行（逐验收标准进入 L5 判读），L1 逐字引用、L2 引用 state-machine.md §2 + equipment-integration.md §二/§三、L3 含行号 + 跨域 IErpInvStockMoveBiz 调用链、L4 注明断言强度、L5 标注无专属 MA2 + P1-MA2-074 复用
- [x] UC-MAIN-03/04 有符合性结论且列明 §2 判据编号；UC-MAIN-03 P2 裁决（IDLE 恢复分支缺失，complete 恢复 RUNNING 已实现）含 owner doc Deferred 标注的人工批准痕迹核查结论（§4 三判据均不成立）；触及会计保护区域项显式标注 ask-first

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: completed
Targets: `docs/audits/2026-08-06-0245-2-rc-ma1-a1-43-maintenance-f2-visit-sparepart.md`（补 §6-§9）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成

- [x] `Decision` **复用 or 新增 裁决**（§7）：grep `arm-index.md` maintenance visit/equipment status/spare part/consumption/MAINTENANCE_ISSUE 同域同控制点后裁决——UC-MAIN-03 IDLE 恢复分支为**新根因**（无 UC-MAIN-03 finding）→ 新建 **P2-RC-061**（UC-MAIN-03 C-IDLE 分支，§2 P2①）；UC-MAIN-04 出库+余额+凭证+不足校验主路径全满足，无新 finding（P1-MA2-074 过账吞异常维度 + A2.14 状态机迁移维度均复用不重审）。执行时 grep arm-index 取最新续编号（A1.42 已占至 P1-RC-066/P2-RC-060，本切片续编 P2-RC-061）。禁止未经比对新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 successor MR1）+ **备件消耗过账凭证科目映射/VoucherFact 修复触及会计保护区域须 ask-first + 独立 plan-audit**（沿用 P1-MA2-074 ask-first 先例；本切片无新增会计过账变更）+ **UC-MAIN-04 出库须与 inventory 域 IErpInvStockMoveBiz 跨域协调**。
      - Skill: none
- [x] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记 L5 无法静态定论、需运行时确认的点（SP-1 complete 时 IDLE 设备状态是否运行时恢复 RUNNING/IDLE[静态+代码已确认归 P2-RC-061] / SP-2 generateMove 出库运行时是否真实触发库存出库移动单 + 余额扣减[跨域 inventory] / SP-3 备件不足校验失败路径运行时是否抛 inventory ERR[跨域 guard]；每存疑点一行）。**P0 即时通道评估**（本切片无活跃数据破坏候选——备件过账悬挂经 P1-MA2-074 已 resolved；设备状态联动 IDLE 分支缺失属状态一致性非数据破坏——评估在报告 §7 给结论）。
      - Skill: none
- [x] `Proof` 报告 §8 过程纪律自检段：实际运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（R1/R2 实测 + R3+ 引同日 A1.42 值，零代码变更）；含基线漂移注记（baseline 文件 R2b/R2c/R2d 与同日 A1.42 记录不一致，预存态非本审计引入）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 退出码 0 为门控通过依据**（无生产代码变更，注明"无回归风险"）。
      - Skill: none
- [x] `Add` 报告 §9 与 MA2 报告差异增量声明：无 maintenance 专属 MA2 报告；P1-MA2-074 引作备件过账吞异常已 resolved 证据（不重审过账吞异常维度）+ A2.14 引作状态机迁移守卫已证实证据（不重审状态机迁移维度）；列明只补的需求视角差异（UC-MAIN-03 complete 侧设备恢复已实现纠正计划基线 + IDLE 分支缺失 + UC-MAIN-04 出库+余额+凭证+不足校验主路径全满足 + 命名差异 cosmetic）。
      - Skill: none
- [x] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 RC finding（P2-RC-061）入 RC 发现追踪分区；audit reports 表新增 A1.43 行；附 A1.43 交叉引用注记 block。
      - Skill: none
- [x] `Proof` 报告 9 段完整性自检：落盘前自查 §1-§9 全部存在。
      - Skill: none

Exit Criteria:

- [x] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据
- [x] 新 RC finding（P2-RC-061）已写入 `arm-index.md`；静态存疑点清单已登记（供 A4.1/A4.2 展开）；P0 候选评估有结论（本切片无 P0）
- [x] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_02db36a4effeG6kur04GthBbts，fresh session，未起草本计划）。范围/UC 覆盖（A1.43=UC-MAIN-03/04）/依赖（0.2 done）/结果表面/方法论（9 段 §6 + §4 三判据 + §5 ask-first + §7 reuse + §去重协议）/反 slack/模板/保护区域标注全 PASS；load-bearing 引用经实仓复核 CONFIRMED TRUE：①`ErpMntVisitBizModel` 委派 4 per-mutation Processor（schedule:35/start:41/complete:47/cancel:53）✅；②`ErpMntVisitStartProcessor` javadoc:10 "设备状态联动（UNDER_MAINTENANCE）" ✅；③`ErpMntSparePartUsageBizModel` confirm:29/reverseConfirm:35 + `MaintenanceIssuePostingDispatcher` 存在 ✅；④`ErpMntEquipmentBizModel.changeStatus:20` 存在 ✅；⑤`ErpMntVisitCancelProcessor` MAINTENANCE_LABOR 凭证红冲 config-gated（javadoc:13-15 + doCancel:32-35）✅；⑥baseline-inventory:377 A1.43 锚点一致 ✅。**INFO（非阻塞，执行时留意）**：`ErpMntVisitCompleteProcessor:31` 实际调用 `equipmentStatusLinker.restoreToRunning`——执行时 UC-MAIN-03 complete 侧设备恢复倾向"接受"（本计划"若缺失倾向 P1"的条件框架对审计计划正确，非失实声明）。会计保护区域（UC-MAIN-04 备件凭证 MAINTENANCE_ISSUE）正确标 ask-first 沿用 P1-MA2-074 先例。共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 报告 9 段完整性 + 五级矩阵逐验收标准覆盖 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A1.43 报告 9 段齐全 + UC-MAIN-03/04 矩阵行（逐验收标准）+ finding 登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.43 锚点一致
- [x] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（无代码变更故不跑 build/test；mvn test 仅作 §3 行为基线确认 4 测试类 18 用例 BUILD SUCCESS）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留空作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按 §10 经 MR0（P0）/ MR1（R1.0 展开 RC-R1.n，P1）实施；本切片候选偏差多为**代码逻辑**类（预授权——设备状态联动 complete 侧 / 备件余额断言 / 不足校验）；**备件消耗过账凭证科目映射/VoucherFact 触及会计保护区域须 ask-first + 独立 plan-audit**（沿用 P1-MA2-074 先例）。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行；UC-MAIN-04 出库须与 inventory 域跨域协调）

## Closure

Status Note: 执行完成（2026-08-05）。A1.43 审计报告 `docs/audits/2026-08-06-0245-2-rc-ma1-a1-43-maintenance-f2-visit-sparepart.md` 已落盘（9 段齐全），UC-MAIN-03 = P2（接受 on start/complete/驱动主路径 + P2-RC-061 IDLE 恢复分支缺失），UC-MAIN-04 = 接受（出库+余额+凭证+不足校验主路径全满足）。1 新 P2（P2-RC-061）+ 2 reuse（P1-MA2-074 resolved R1.16 + A2.14 状态机迁移）已登记入 `arm-index.md`（RC 发现追踪分区 + 报告清单 + 交叉引用注记）。零 P0。独立结束审计由独立子代理（新会话，fresh session 无执行者上下文）执行并通过。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，fresh context，未起草/未执行本计划）
- Audit Date: 2026-08-05
- Verification walkthrough（逐项对照报告 §1-§9 + Closure Gates 与实仓）：
  - **报告 9 段齐全** CONFIRMED：`docs/audits/2026-08-06-0245-2-rc-ma1-a1-43-maintenance-f2-visit-sparepart.md` §1-§9 全部存在（grep `^## [1-9]\.` 命中 9 段）。
  - **UC-MAIN-03 complete 侧设备恢复 RUNNING 已实现** CONFIRMED：`ErpMntVisitCompleteProcessor.java:31` 实调 `equipmentStatusLinker.restoreToRunning(visit.getEquipmentId(), context)`；`EquipmentStatusLinker.java:38-43` 恒恢复 `EQUIPMENT_STATUS_RUNNING`，无 IDLE 分支 + 无前置状态快照列；`EquipmentStatusLinker` javadoc:16-17 AI 代码层自承「IDLE 设备恢复为 RUNNING 为已知的简化偏差」（非 owner doc Deferred 无人工批准）—— 与报告 §5/§7 P2-RC-061 描述一致。
  - **UC-MAIN-03 start 侧设备→UNDER_MAINTENANCE** CONFIRMED：`EquipmentStatusLinker#linkToUnderMaintenance:24-29` 写 `EQUIPMENT_STATUS_UNDER_MAINTENANCE`，config `erp-mnt.equipment-status-link-enabled` 门控。
  - **UC-MAIN-04 出库跨域调用** CONFIRMED：`SparePartIssueService.java:55` 调 `stockMoveBiz.generateMove(request, context)`，`request.setMoveType(MOVE_TYPE_OUTGOING)` + `setRelatedBillType(RELATED_BILL_TYPE_MNT_SPARE_PART)`（方法名 `generateMove` vs L1 `generateConsumptionMove` cosmetic §9 已记录）。
  - **UC-MAIN-04 凭证科目映射** CONFIRMED：`MaintenanceIssueAcctDocProvider.java:62-91` 构造借维修费用（`DEFAULT_EXPENSE_SUBJECT_CODE="6602"`）/ 贷存货（`DEFAULT_INVENTORY_SUBJECT_CODE="1403"` 按物料分列），支持业务类型 `MAINTENANCE_ISSUE`；`ErpMntConstants.java:43-44` 6602/1403 常量一致。
  - **L4 测试断言强度** CONFIRMED：`TestErpMntVisitRequestStateMachine.java:66-89` `testVisitHappyPathWithEquipmentLink` 强断言设备 RUNNING→UNDER_MAINTENANCE→RUNNING（seed 恒 RUNNING 未覆盖 IDLE 输入分支，与 P2-RC-061 一致）；`TestErpMntSparePartPosting.java:101/143` `testSparePartPostingBasic/MultiMaterial` 行级强断言 Dr 6602/Cr 1403 + 借贷平衡 + 幂等防双重扣减。
  - **arm-index 衔接** CONFIRMED：`docs/audits/arm-index.md` audit reports 表新增 A1.43 行（done）+ RC 发现追踪分区登记 `P2-RC-061`（UC-MAIN-03 C-IDLE 分支）+ A1.43 交叉引用注记 block；reuse P1-MA2-074（resolved R1.16，不重审过账吞异常维度）+ A2.14（状态机迁移守卫，不重审迁移维度）裁决正确，无重复登记。
  - **零 P0 评估** CONFIRMED：IDLE 状态一致性分歧（RUNNING 更可用态）非数据破坏/GL 平衡破坏/会计错误；备件过账悬挂 P1-MA2-074 resolved R1.16 config-gated + 幂等 + 失败告警；备件不足跨域 guard 存在（inventory `validateAvailable` `ERR_AVAILABLE_INSUFFICIENT` 默认禁负库存）。
  - **保护区域标注** CONFIRMED：报告与方法论 §5/§10 一致——本切片无新增会计过账变更；P2-RC-061 修复视形态（纯 BizModel 前态恢复预授权 / 加 `ErpMntEquipment` preMaintenanceStatus 快照列 ORM ask-first）；备件过账凭证科目/VoucherFact 未来修复须 ask-first 沿用 P1-MA2-074 先例；UC-MAIN-04 出库须与 inventory 域跨域协调。
  - **§8 过程纪律自检** CONFIRMED：报告 §8 含 checker actual vs baseline 实测表 + 独立性声明 + 交叉去重声明；本审计零生产代码变更故跳过 build/test 门控合理。
  - **文本一致性** CONFIRMED：Plan Status `completed` / 两 Phase Status `completed` / 全部 Exit Criteria `[x]` / 全部 Closure Gates `[x]` / `docs/logs/2026/08-06.md` A1.43 条目一致。
- Independence attestation: 本次审计在新会话执行，未参与计划起草、草案审查或执行；执行者已退出上下文。审计通过，所有范围内项目 landed 或 adjudicated，无活跃 P0/P1 隐瞒降级。

Follow-up:

- MR0/MR1 按 §10 展开本报告 finding 修复（备件过账凭证类须 ask-first + 跨域 inventory 协调）。
- MA4 运行时探针展开 §7 静态存疑点清单 SP-1~SP-3（运行时确认 complete 设备恢复 / generateConsumptionMove 出库触发 / 备件不足校验路径）。
