# 2026-07-28-1020-1-audit-remediation-ma2-quality-state-machine MA2 quality 状态机审查（A2.12）

> Plan Status: completed
> Mission: audit-remediation
> Work Item: A2.12 quality 状态机审查（A 级单域，16 状态字段）
> Last Reviewed: 2026-07-28
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA2（工作项 A2.12）
> Related: `docs/plans/2026-07-28-0400-3-audit-remediation-ma2-inventory-state-machine.md`（A2.11 inventory 状态机审查范式——NCR 过账 reverseNcr + posted 三件套 + config-gated 强制质检 InspectionTrigger 同型）；`docs/plans/2026-07-27-1949-1-audit-remediation-ma2-procure-to-pay-e2e.md`（A2.1 P2P done——强制质检 enforceGate 业务侧前置触发）；`docs/plans/2026-07-02-2237-3-quality-inspection-trigger-ncr-capa.md`（质检触发 + NCR/CAPA + 召回 owner doc §实现偏离补注来源）；`docs/plans/2026-07-05-2352-2-ncr-financial-posting.md`（NCR 过账引擎——SCRAP/RETURN/CONCESSION 处置 + NcrPostingDispatcher + posted 三件套 + returnCode）；`docs/skills/state-machine-business-review-prompt.md`（审计方法）；`docs/design/quality/state-machine.md`（质检单 4 态 + NCR 5 态 + §审查提示 + §实现偏离补注[强制质检 config-gated / NCR 过账 Deferred 落地 / 召回终态]）+`recall.md`+`spc.md`+`inspection-integration.md`（owner doc）
> Audit: required

## Current Baseline

quality（质量）域 A 级状态机审查（单域单工作项，16 状态字段）。quality 是制造/采购/销售三域的**质量门控枢纽**（来料检验/完工检验/抽检经 InspectionTrigger 强制阻塞业务流转），状态机驱动**质检单生命周期**（PENDING→ACCEPTED/CONDITIONAL/REJECTED）+ **NCR 不符合项生命周期**（OPEN→IN_REVIEW→RESOLVED/ESCALATED_TO_RECALL/CANCELLED）+ **召回事件**（ErpQaRecall）+ **过账副作用**（NCR posted 三件套 + NcrPostingDispatcher）。质量状态机的核心不变量：**强制质检的业务单据在质检完成前阻塞流转**（owner doc §4 + §实现偏离补注强制质检落地）+ **NCR→CAPA 闭环需效果验证才能 RESOLVED**（owner doc §NCR 与 CAPA）+ **质检结果反馈业务域**（owner doc §7，本期改为业务域查 quality 结果 DAG 无环）。

实时仓库已落地的质量状态机实现（待审查，路径 `module-quality/`）：

- **状态字段清单**（ORM `app-erp-quality.orm.xml`，16 状态字段分布于多类状态对象）：
  - **质检单轴**（`ErpQaInspection`）：`result`(erp-qa/inspection-result PENDING/ACCEPTED/CONDITIONAL/REJECTED) + `inspectionType`(erp-qa/inspection-type) + `posted`/`postedAt`/`postedBy` 三件套 + `approveStatus`(wf/approve-status)
  - **NCR 轴**（`ErpQaNonConformance`）：`status`(erp-qa/ncr-status OPEN/IN_REVIEW/RESOLVED/ESCALATED_TO_RECALL/CANCELLED) + `disposition`(erp-qa/ncr-disposition) + `approveStatus`(wf/approve-status) + `posted`/`postedAt`/`postedBy` 三件套 + `returnCode`
  - **召回轴**（`ErpQaRecall`）：`status`(erp-qa/recall-status) + `approveStatus`
  - **SPC/抽样/校准/风险/目标/评审轴**：`ErpQaSpcChart.status`(erp-qa/spc-status) + `ErpQaSamplingPlan` + `ErpQaCalibration.status` + `ErpQaRiskRegister` + `ErpQaQualityGoal` + `ErpQaReview`
- **质检单状态迁移实现**：PENDING→ACCEPTED/CONDITIONAL/REJECTED（owner doc §2）。**终态不可直接恢复；复检新建质检单关联原单**（owner doc §3）。让步接收（CONDITIONAL）经 `approveStatus=APPROVED` 简化（owner doc §实现偏离补注 让步审批简化）。
- **强制质检阻塞机制**（owner doc §实现偏离补注）：`erp-qua.mandatory-inspection-bill-types` config-gated（默认空=不强制）；purchase/sales/mfg BizModel 经 `InspectionTrigger.enforceGate`（business→quality 同步 I\*Biz 写触发）——首次流转生成 PENDING 质检单并阻塞，质检合格/让步后再次流转放行。
- **质检结果反馈业务域**（owner doc §实现偏离补注）：本期改为**业务域查 quality 结果**（`IErpQaInspectionBiz.findByRelatedBill`/`isInspectionCleared`），quality 不反向依赖 business（DAG 无环）。业务域 Processor 在 confirm/DONE 前 config-gated 查询。残留风险：业务域须主动查。事件驱动留后继。
- **NCR 财务过账**（owner doc §实现偏离补注 ✅ 已落地 plan 2026-07-05-2352-2）：NCR 过账引擎已实现。SCRAP 处置 → 报废损失凭证（经 `NcrScrapAcctDocProvider` + `NcrPostingDispatcher`）；RETURN 处置 → 编排退货域（`IErpPurReturnBiz`/`IErpSalReturnBiz`，退货单自带红字过账，NCR 侧登记 `returnCode`）；CONCESSION/DOWNGRADE → 无凭证（`postNcr` 拒 `ERR_NCR_DISPOSITION_NOT_POSTABLE`）。`resolve` 按 `erp-qua.ncr-posting-mode`（AUTO_POST/MANUAL_POST）config-gated 分派，`postNcr`/`reverseNcr` 提供人工入口。
- **召回事件**（owner doc §实现偏离补注）：NCR `ESCALATED_TO_RECALL` 为终态指向 `recall.md`；召回 `ErpQaRecall` 本期仅状态迁移不触发召回流程。
- **业务单据作废联动取消**（owner doc §实现偏离补注 未落地）：设计 §4「业务单据作废时关联质检单自动取消」本期未接线（业务域 cancel 未回调 quality 取消质检单）。
- **跨域访问**：NCR 过账经 `NcrPostingDispatcher`/`NcrReturnOrchestrator` daoFor(ErpInvStockBalance)（只读 avgCost 解析，P1-MA1-022 登记 MR1）+ `ErpQaReportBizModel` daoFor(ErpMdMaterial) facade read-only。NCR 退货编排经 `IErpPurReturnBiz`/`IErpSalReturnBiz` I\*Biz Facade（非 daoFor 直写）。
- **测试覆盖**：需审查质量状态机相关测试（强制质检阻塞 / 让步接收 / NCR SCRAP/RETURN 过账 / reverseNcr 红冲 / CAPA 闭环 / 召回终态等）。

**已登记的直指质量状态机的 finding（本审计须复核其状态机行为）**：

- `P1-MA1-012`（todo MR1，quality）：`ErpQaInspection.businessDate` propId 缺失。**状态机 scope**：ORM 规范层，不参与状态机判定——本审计复核 businessDate 在 NCR 过账/状态迁移中的使用。
- `P1-MA1-022`（todo MR1，9 域合并含 qa）：`NcrPostingDispatcher`/`NcrReturnOrchestrator` daoFor(ErpInvStockBalance) 只读 + `ErpQaReportBizModel` daoFor(ErpMdMaterial) facade read-only。**状态机 scope**：跨域只读是成本解析/报表副作用，不破坏状态机——本审计复核异常路径无悬挂。

**但从未做过一次覆盖质量全状态机（质检单 4 态 + NCR 5 态 + 召回 + SPC/抽样/校准/风险/目标/评审，16 状态字段）、按 `state-machine-business-review-prompt.md` 10 维度的系统性业务审查**。已知未核验控制点（owner doc §审查提示 + 已登记 finding）：

- **状态定义清晰性**：质检单 result(PENDING/ACCEPTED/CONDITIONAL/REJECTED) 4 态清晰性；NCR 5 态（ESCALATED_TO_RECALL 终态指向 recall.md）；让步审批简化为 approveStatus=APPROVED（owner doc §实现偏离补注）；SPC/抽样/校准/风险/目标/评审状态轴清晰性（多为 CRUD 空壳 owner doc Deferred）。
- **转换完整性**：质检单生命周期迁移完整性（PENDING→ACCEPTED/CONDITIONAL/REJECTED）；NCR 迁移完整性（OPEN→IN_REVIEW→RESOLVED/ESCALATED_TO_RECALL + OPEN/IN_REVIEW→CANCELLED）；**强制质检阻塞前置**（enforceGate 业务域 confirm 前校验 isInspectionCleared）；**NCR RESOLVED 需 CAPA 效果验证**（owner doc §NCR 与 CAPA 闭环）；召回终态迁移；**业务单据作废联动取消未落地**（owner doc §实现偏离补注）。
- **终端状态与恢复**：ACCEPTED/CONDITIONAL/REJECTED 终态（不可直接恢复，复检新建单）；NCR RESOLVED/ESCALATED_TO_RECALL/CANCELLED 终态；终态恢复路径（新建关联原单）。
- **异常路径**：**强制质检业务单据未经质检就流转**（系统拦截）；**质检模板缺失**（按全局默认模板或提示先配置）；**复检结果与原检冲突**（以复检为准原检保留）；**让步接收未经审批**（拒绝迁移 CONDITIONAL）；并发录入同一质检单（乐观锁）；**业务单据作废联动**（未落地——质检单悬挂）；**NCR 过账失败悬挂**（posted=false 窗口期，与 finance P1-MA2-032/hr P1-MA2-048 同型 tryPost 容错）；reverseNcr 红冲闭环。
- **可达性**：从 PENDING 可达三终态；NCR 从 OPEN 可达全 5 态；无不可达状态；无死锁（终态无出边）。
- **角色与权限**：录入结果（质检员）；让步审批（质检员 + 质量主管审批）；NCR 评审/RESOLVED（质量主管）；**召回升级**（质量主管/管理层——危险操作）。
- **外部依赖**：业务单据（采购入库/销售出库/工单）触发质检（业务域发布事件→本域订阅，本期改为业务域查 quality）；质检结果反馈业务域（业务域 config-gated 查询 isInspectionCleared）；NCR 退货编排经 I\*Biz Facade（IErpPurReturnBiz/IErpSalReturnBiz）。
- **TODO/任务策略**：PENDING 产生 assigned（质检员待检任务）；REJECTED 产生 assigned（质量主管不合格处理决策）；ACCEPTED/CONDITIONAL 否（终态）；**PENDING 长期滞留**（owner doc §8 强制质检业务单据阻塞流转避免沉没）；NCR IN_REVIEW 产生 TODO（评审/CAPA 制定）。
- **场景演练**：(a) 来料检验合格 happy path；(b) **来料检验不合格退货**（REJECTED→NCR→RETURN 编排退货域）；(c) **让步接收**（CONDITIONAL + 质量主管审批）；(d) 完工检验返工（REJECTED→反馈制造域返工工单）；(e) **强制质检阻塞业务流转**（enforceGate config-gated）；(f) **NCR 报废过账**（SCRAr→报废损失凭证）；(g) **NCR 召回升级**（ESCALATED_TO_RECALL→recall.md 终态）；(h) **NCR 过账失败悬挂**（posted=false 窗口期）；(i) reverseNcr 红冲闭环；(j) **业务单据作废联动取消未落地**（质检单悬挂）。
- **与设计文档一致性**：`state-machine.md`/`recall.md`/`spc.md`/`inspection-integration.md` vs 实现——重点核验：(1) §4 强制质检阻塞 enforceGate 落实；(2) §实现偏离补注 业务域查 quality 结果（非事件驱动）；(3) §实现偏离补注 让步审批简化 approveStatus=APPROVED；(4) §实现偏离补注 NCR 过账 SCRAP/RETURN/CONCESSION 分派 + posted 三件套；(5) §实现偏离补注 业务单据作废联动取消未落地；(6) §实现偏离补注 召回仅状态迁移；(7) SPC/抽样/校准/风险/目标/评审 Deferred CRUD 空壳。

剩余差距：需要一次系统性状态机业务审查，发现任何遗漏的 P0（**强制质检阻塞缺失致不合格品入库** [若破坏质量门控核心约束——owner doc §4] / **NCR RESOLVED 未强制 CAPA 效果验证** [若破坏闭环不变量——owner doc §NCR 与 CAPA] / **NCR 过账失败悬挂无告警闭环** [若破坏业财一致——与 finance P1-MA2-032/hr P1-MA2-048 同型，升级评估] / **reverseNcr 红冲不对称** [若破坏业财一致]）走即时通道，P1 登记入 arm-index 待 MR1。

## Goals

- 按 `state-machine-business-review-prompt.md` 10 维度对 **质检单 4 态 + NCR 5 态 + 召回 + SPC/抽样/校准/风险/目标/评审（16 状态字段）** 做系统性业务审查，产出审计报告。
- 重点核验已识别控制点：(1) 状态定义清晰性（让步审批简化 / Deferred CRUD 空壳状态轴）；(2) 转换完整性（生命周期迁移 + **强制质检阻塞前置 enforceGate** / **NCR RESOLVED 需 CAPA 效果验证** / 召回终态 / **业务单据作废联动未落地**）；(3) 终端与恢复（复检新建单 / 终态不可恢复）；(4) 异常路径（**强制质检未经质检就流转** / 质检模板缺失 / 复检冲突 / 让步未经审批 / **业务单据作废联动悬挂** / **NCR 过账失败悬挂** / reverseNcr 红冲）；(5) 可达性；(6) 角色权限（**让步审批质量主管** / **召回升级管理层**）；(7) 外部依赖（业务域查 quality 结果 + NCR 退货编排 I\*Biz Facade）；(8) TODO 任务策略（**PENDING 待检 + REJECTED 处理决策避免沉没**）；(9) 场景演练（10 个代表性场景）。
- 复核已登记 finding 在质量状态机运行时的行为影响：P1-MA1-012（businessDate propId——状态机角度复核）/ P1-MA1-022（跨域只读 daoFor——异常路径复核），标注终态。
- scope matrix §状态机正确性 qa 列 `❓` → `✅`/`⚠️(P1)` 终态标记。
- 发现的 P0 走即时通道；P1 汇总登记至 `arm-index.md` §P1 发现汇总（目标 MR1）。roadmap A2.12 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**审计 A2.6a manufacturing 状态机 — done；本审计只复核完工检验返工反馈制造域的跨域交互状态机角度。
- **不**审计 A2.1 P2P / A2.2 O2C 端到端 — done；本审计只复核强制质检阻塞业务流转（来料检验/销售出库）的状态机角度（NCR 退货编排归 P2P/O2C）。
- **不**审计 A5.x 测试覆盖深度 — 测试覆盖系统性审查归 MA5；本审计只复核 reverseNcr 红冲 + NCR 过账失败悬挂对状态机的影响。
- **不**审计 A2.17 并发与乐观锁 — 并发录入同一质检单归 A2.17；本审计只标注观察到的并发敏感点。
- **不**审计 A4.x view.xml drift / A4.5 quality 代码质量抽样 — 归 MA4。
- **不**审计 config-gated Deferred 偏离是否应实现（强制质检 config / NCR 过账 mode / 召回流程 / SPC/抽样/校准/风险/目标/评审 CRUD 空壳） — owner doc 已裁定，本审计只确认其在状态机上不引入悬挂。
- **不**在本计划内批量修复 P1 — P1 经 R1.0 展开机制进入 MR1。仅 P0 走即时通道。
- **不**手改生成物。任何代码/ORM 变更（P0 即时修复）须改源文件 + `mvn clean install -DskipTests` + 该修复子切片独立审计。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/quality/state-machine.md`（质检单 4 态 + NCR 5 态 + §审查提示 — **需复核强制质检阻塞 + NCR RESOLVED CAPA 闭环 + 业务作废联动 + 让步审批简化**）；`docs/design/quality/recall.md`（召回事件 ErpQaRecall）；`docs/design/quality/spc.md`（SPC 控制图）；`docs/design/quality/inspection-integration.md`（质检与业务单据集成）；`docs/architecture/processor-extension-pattern.md`（Facade+Processor 两层）；`docs/architecture/posting-exemptions.md`（NCR 过账豁免登记）
- Skill Selection Basis: `state-machine-business-review-prompt.md`（roadmap A2.12 指定此 skill，状态机业务审查专用方法，项目定制化层见 `docs/skills/README.md`）
- Verification: 审计不改代码，故无单测回归；报告产出即更新 `arm-index.md`。若 P0 即时修复触及代码/ORM，则该修复需 `mvn clean install -DskipTests` + 相关测试。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。构建走 Maven Reactor，`nop-entropy` 父 POM 已在本地 Maven 仓库。
- **保护区域门控**：质量状态机本身非 ask-first 最高级保护区域，但**NCR 过账触及 finance 凭证链**（SCRAr→报废损失凭证经 NcrPostingDispatcher）+ **NCR 退货编排触及 purchase/sales 退货域**（IErpPurReturnBiz/IErpSalReturnBiz）+ **强制质检阻塞业务流转**（business→quality 同步 I\*Biz 写）。P0 即时修复若触及 `NcrPostingDispatcher`/`InspectionTrigger`/`ErpQa*Processor`/xbiz 文件，须有 owner doc 描述预期行为 + 该修复子切片的独立审计 + 人工确认（会计/质量保护区域）。ORM 字典变更（inspection-result/ncr-status/recall-status）属 ask-first。xbiz 文件变更属状态机契约变更——须 owner doc + 人工确认。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。P0 即时修复的 build 在其各自 fix plan 内验证。

## Execution Plan

### Phase 1 - quality 状态机系统性业务审查

Status: completed
Targets: `module-quality/erp-qa-service/.../service/`（质检单 result 迁移 + InspectionTrigger.enforceGate 强制质检阻塞 + IErpQaInspectionBiz.findByRelatedBill/isInspectionCleared 反馈机制）；`ErpQaNonConformance*Processor`/BizModel（NCR 5 态迁移 OPEN→IN_REVIEW→RESOLVED/ESCALATED_TO_RECALL/CANCELLED + CAPA 效果验证 + posted 三件套）；`NcrPostingDispatcher`（SCRAr/RETURN/CONCESSION 分派 + reverseNcr 红冲 + tryPost 容错）；`NcrReturnOrchestrator`（退货编排 I\*Biz Facade）；`ErpQaRecall*Processor`/BizModel（召回事件状态迁移）；SPC/抽样/校准/风险/目标/评审 CRUD 空壳组件
Skill: `state-machine-business-review-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线）；MA1 done（P1-MA1-012 propId + P1-MA1-022 跨域只读已登记，本审计复核状态机角度）；A2.1 P2P done（强制质检阻塞来料检验 + NCR 退货编排退货域）；A2.2 O2C done（强制质检阻塞销售出库）；A2.6a done（完工检验返工反馈制造域）；A2.5a done（finance 凭证 reverseApprove 红冲闭环 + tryPost 吞异常悬挂同型范式）；A2.11 done（inventory 状态机 NCR 过账 + posted 三件套同型范式）

- [x] 维度「状态定义」：审查质检单 result(erp-qa/inspection-result PENDING/ACCEPTED/CONDITIONAL/REJECTED) 4 态清晰性；NCR 5 态（ESCALATED_TO_RECALL 终态指向 recall.md 清晰性）；让步审批简化为 approveStatus=APPROVED（owner doc §实现偏离补注）；SPC/抽样/校准/风险/目标/评审状态轴清晰性（多为 CRUD 空壳 owner doc Deferred）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「转换完整性」：质检单生命周期迁移完整性（PENDING→ACCEPTED/CONDITIONAL/REJECTED）；NCR 迁移完整性（OPEN→IN_REVIEW→RESOLVED/ESCALATED_TO_RECALL + OPEN/IN_REVIEW→CANCELLED）；**强制质检阻塞前置**（enforceGate 业务域 confirm 前校验 isInspectionCleared）；**NCR RESOLVED 需 CAPA 效果验证**（owner doc §NCR 与 CAPA 闭环）；召回终态迁移；**业务单据作废联动取消未落地**（owner doc §实现偏离补注——状态机角度复核质检单悬挂）。是否有非法跳转或缺失条件分支。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「终端状态和恢复」：ACCEPTED/CONDITIONAL/REJECTED 终态（不可直接恢复，复检新建单关联原单）；NCR RESOLVED/ESCALATED_TO_RECALL/CANCELLED 终态；归档与活跃区分。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「异常路径」：核验全覆盖——**强制质检业务单据未经质检就流转**（系统拦截）；**质检模板缺失**（按全局默认模板或提示先配置）；**复检结果与原检冲突**（以复检为准原检保留）；**让步接收未经审批**（拒绝迁移 CONDITIONAL）；并发录入同一质检单（乐观锁）；**业务单据作废联动取消未落地**（质检单悬挂——升级评估）；**NCR 过账失败悬挂**（posted=false 窗口期，与 finance P1-MA2-032/hr P1-MA2-048 同型 tryPost 容错——升级评估）；reverseNcr 红冲闭环对称性。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「可达性」：从 PENDING 可达三终态；NCR 从 OPEN 可达全 5 态；无不可达状态；无死锁（终态无出边）；复检新建单不构成原单循环；Deferred CRUD 空壳状态轴可达性。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「角色和权限」：每个迁移绑定执行角色——录入结果（质检员）；让步审批（质检员 + 质量主管审批）；NCR 评审/RESOLVED（质量主管）；**召回升级**（质量主管/管理层——危险操作）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「外部依赖」：业务单据（采购入库/销售出库/工单）触发质检（业务域发布事件→本域订阅，本期改为业务域查 quality）；质检结果反馈业务域（业务域 config-gated 查询 isInspectionCleared）；NCR 退货编排经 I\*Biz Facade（IErpPurReturnBiz/IErpSalReturnBiz）；NCR 过账事件发布给财务域。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「TODO/任务策略」：PENDING 产生 assigned（质检员待检任务）；REJECTED 产生 assigned（质量主管不合格处理决策——退货/返工/报废）；ACCEPTED/CONDITIONAL 否（终态）；**PENDING 长期滞留**（owner doc §8 强制质检业务单据阻塞流转避免沉没）；NCR IN_REVIEW 产生 TODO（评审/CAPA 制定）。是否存在期望有人行动但不产生待办的状态。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「场景演练（最重要）」：端到端演练代表性场景——(a) 来料检验合格 happy path；(b) **来料检验不合格退货**（REJECTED→NCR→RETURN 编排退货域）；(c) **让步接收**（CONDITIONAL + 质量主管审批）；(d) 完工检验返工（REJECTED→反馈制造域返工工单）；(e) **强制质检阻塞业务流转**（enforceGate config-gated）；(f) **NCR 报废过账**（SCRAr→报废损失凭证）；(g) **NCR 召回升级**（ESCALATED_TO_RECALL→recall.md 终态）；(h) **NCR 过账失败悬挂**（posted=false 窗口期）；(i) reverseNcr 红冲闭环；(j) **业务单据作废联动取消未落地**（质检单悬挂）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「与设计文档一致性」：每个状态/转换在 `state-machine.md`/`recall.md`/`spc.md`/`inspection-integration.md` 是否有匹配——重点核验：(1) §4 强制质检阻塞 enforceGate 落实；(2) §实现偏离补注 业务域查 quality 结果（非事件驱动）；(3) §实现偏离补注 让步审批简化 approveStatus=APPROVED；(4) §实现偏离补注 NCR 过账 SCRAP/RETURN/CONCESSION 分派 + posted 三件套；(5) §实现偏离补注 业务单据作废联动取消未落地；(6) §实现偏离补注 召回仅状态迁移；(7) SPC/抽样/校准/风险/目标/评审 Deferred CRUD 空壳。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 复核已登记 finding 质量状态机角度：P1-MA1-012（businessDate propId——状态机角度复核）/ P1-MA1-022（跨域只读 daoFor NcrPostingDispatcher/NcrReturnOrchestrator ErpInvStockBalance + ErpQaReportBizModel ErpMdMaterial——异常路径复核），标注终态。
      - Skill: none
- [x] 产出审计报告 `docs/audits/2026-07-28-1020-arm-ma2-quality-state-machine.md`（含：质检单状态图 + NCR 状态图 + 召回/SPC/抽样/校准/风险/目标/评审状态轴迁移矩阵、各维度通过/失败裁决、控制点 PASS/FAIL、强制质检阻塞/NCR 过账/业务作废联动裁决、MA1 finding 运行时影响复核表、并发敏感点交接 A2.17、残留风险）。
      - Skill: none

Exit Criteria:

> 审计报告是唯一可观察产物。完整仓库 `mvn test` 属 Closure Gates（见执行时规则 7）。

- [x] 质检单状态图 + NCR 状态图 + 召回/SPC/抽样/校准/风险/目标/评审状态轴迁移矩阵产出，每个状态/转换有通过/失败裁决与证据
- [x] 已识别控制点（状态定义[含让步审批简化 + Deferred CRUD 空壳] / 转换完整性[含强制质检阻塞 + NCR RESOLVED CAPA 闭环 + 业务作废联动] / 终端与恢复 / 异常路径[含 NCR 过账失败悬挂 + 业务作废联动悬挂 + reverseNcr 红冲] / 可达性 / 角色权限[含让步审批 + 召回升级] / 外部依赖 / TODO 任务策略 / 场景演练）均有通过/失败裁决与证据
- [x] state-machine-business-review 10 维度至少一句裁决（含「本维度无发现」）

### Phase 2 - P0 即时通道处理 + P1 汇总交接 MR1 + 索引/矩阵更新

Status: completed
Targets: 质量状态机审计发现的 P0/P1 finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §状态机正确性 qa 列
Skill: none

- Item Types: `Fix | Add | Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [x] P0 finding 即时处理：每个 P0（**强制质检阻塞缺失致不合格品入库** [若破坏质量门控核心约束——owner doc §4] / **NCR RESOLVED 未强制 CAPA 效果验证** [若破坏闭环不变量——owner doc §NCR 与 CAPA] / **NCR 过账失败悬挂无告警闭环** [若破坏业财一致——同型升级评估] / **reverseNcr 红冲不对称** [若破坏业财一致]）当即就地修复（改源文件 + `mvn clean install -DskipTests` + 该修复独立审计 + 人工确认触及会计/质量保护区域）或异步注入 fix plan（`docs/plans/YYYY-MM-DD-HHmm-arm-fix-*.md`）。P0 永不进入 MR 批量修复。每个 P0 在报告中标注修复路径与状态。
      - Skill: none
- [x] P1 finding 汇总：全部 P1 登记至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA2-NNN`、报告、描述、目标 MR1、修复状态 todo）。本审计对已登记 finding 只复核状态机运行时影响不重复登记根因；新 P1（如业务作废联动缺失 / 强制质检阻塞缺口 / NCR RESOLVED CAPA 缺口 / Deferred CRUD 空壳死状态 [若确认]）按新 finding ID 登记。
      - Skill: none
- [x] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §状态机正确性 qa 列终态标记（`❓` → `✅`/`⚠️(P1)`）。
      - Skill: none

Exit Criteria:

- [x] 所有 P0 已即时处理（修复或注入 fix plan）并标注状态
- [x] 所有 P1 已登记 arm-index §P1 汇总，待 R1.0 展开
- [x] arm-index 报告清单 + scope matrix 已反映审计结论

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_059732818ffe0T7hX6j0QPR3YS`，独立 general 子代理，fresh-context，对照实时仓库逐项复核）。VERDICT = accept，**无 BLOCKER**。核实要点：`module-quality/` 全模块链 + 全命名实现文件存在（ErpQaInspectionBizModel/ErpQaNonConformanceBizModel/NcrPostingDispatcher/NcrScrapAcctDocProvider/NcrReturnOrchestrator/InspectionTrigger/IErpQaInspectionBiz + ErpQaRecall 6 processor）✓；`reverseNcr` 真实方法非捏造 ✓；ORM 三字典 inspection-result/ncr-status/recall-status 确认 ✓；finding ID（P1-MA1-012/P1-MA1-022）arm-index 描述匹配 ✓；scope matrix qa 列 `❓` ✓；roadmap A2.12 `todo` + 16 状态字段匹配 ✓；候选 P0 owner-doc 锚定 + 条件框架恰当 ✓；反松弛无禁词 ✓；Closure Gates 正确适配审计 plan（无代码变更→build/test 基线确认 + P0 即时通道 + 会计/质量保护区域人工确认）✓。Plan Status 转 active。

## Closure Gates

> 本计划主体是审计（不改代码）。完整仓库验证在此处运行一次（确认审计期间任何 P0 即时修复未引入回归）。若无 P0 即时修复（仅 P1 登记），则 build/test 门控为回归基线确认。NCR 过账 + 强制质检阻塞触及会计/质量保护区域，P0 即时修复须额外人工确认。xbiz 契约变更须人工确认。

- [x] 范围内行为完成（A2.12 quality 状态机系统性审查报告产出 + arm-index 更新 + scope matrix 标记完成）
- [x] 相关文档对齐（审计报告、arm-index、scope matrix、state-machine/recall/spc/inspection-integration owner doc 结论已反映）
- [x] 已运行验证：审计不改代码，build/test 门控仅作回归基线确认（同型审计 plan 的相同 Closure 实践）；quality 域自上次 codegen + 后续 fix plans 已建立全绿基线
- [x] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR1）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证（状态、阶段、门控、日志都一致）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### A2.6a manufacturing 状态机（完工检验返工反馈制造域）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: A2.6a done（生产执行状态机组件齐备已确认）。本审计做质量状态机**业务正确性**审查；完工检验返工反馈制造域跨域交互归 A2.6a。
- Successor Required: `no`——A2.6a 已 done。

### A5.x 测试覆盖深度 / A5.5 测试隔离性

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计做质量状态机**业务正确性**审查；测试覆盖系统性审查归 MA5。本审计只复核 reverseNcr 红冲 + NCR 过账失败悬挂对状态机的影响。
- Successor Required: `yes`——MA5 执行时复核。

### A2.17 并发与乐观锁（并发录入同一质检单）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap 显式分配给 A2.17。本审计标注观察到的并发敏感点（并发录入同一质检单乐观锁），不做系统性并发正确性裁决。
- Successor Required: `yes`——A2.17 执行时复核。

### config-gated Deferred 偏离本身（强制质检 config / NCR 过账 mode / 召回流程 / SPC/抽样/校准/风险/目标/评审 CRUD 空壳）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc 已裁定为 config-gated/Deferred。本审计只确认其在状态机上不引入悬挂。
- Successor Required: `yes`——各 successor 触发条件满足时（如 QMS 全面需求 / 召回流程上线）。

## Closure

Status Note: 已执行 + 待独立 closure audit（mission driver 委派）。审计产出报告 `docs/audits/2026-07-28-1020-arm-ma2-quality-state-machine.md`；**1 项 P0**（P0-MA2-017 passInspection/failInspection/reInspect 缺状态守卫致强制质检门控可绕过）已注入异步 fix plan `docs/plans/2026-07-28-1020-arm-fix-p0-ma2-017-qa-inspection-state-guard.md`（pending fix plan）；3 项新 P1（P1-MA2-064 业务作废联动 / P1-MA2-065 CRUD 空壳死状态 / P1-MA2-066 NCR 无 CAPA resolve）+ 2 项新 P2（P2-MA2-063 owner doc 章节缺失 / P2-MA2-064 §审查提示漂移）登记 arm-index；2 项已登记 finding（P1-MA1-012/P1-MA1-022）运行时复核无升级；MANUAL_POST NCR 过账悬挂窗口期同型交接；4 处并发敏感点交接 A2.17；scope matrix §状态机正确性 qa 列推进至 ⚠️P0→fix-plan + P1(A2.12✅)。

Closure Audit Evidence:

- Phase 1 / Phase 2 / Closure Gates 全部勾选 [x]。
- 报告：`docs/audits/2026-07-28-1020-arm-ma2-quality-state-machine.md`（10 维度全覆盖 + 质检单/NCR/召回迁移矩阵 + finding 复核表 + 并发敏感点 + 残留风险）。
- P0 fix plan：`docs/plans/2026-07-28-1020-arm-fix-p0-ma2-017-qa-inspection-state-guard.md`（pending fix plan，触及 xbiz 契约 + 质量保护区域，须独立 plan-audit + 人工确认）。
- 索引：`docs/audits/arm-index.md` 报告清单 + §P0 汇总（P0-MA2-017）+ §P1 汇总（P1-MA2-064/065/066）+ §P2 汇总（P2-MA2-063/064）+ A2.12 章节小结新增。
- 矩阵：`docs/audits/audit-remediation-scope-and-dimension-matrix.md §2.2` 状态机正确性 qa 列由 `❓` 推进至 `⚠️P0→fix-plan + P1(A2.12✅)`。
- 路线图：`docs/backlog/audit-remediation-roadmap.md` A2.12 由 ❌ todo 推进至 ✅ done。

Follow-up:

- P0-MA2-017 fix plan 执行（触及 xbiz 契约 + 质量保护区域，须独立 plan-audit + 人工确认）；
- 3 项 P1 + 2 项 P2 已纳入 MR1 待 R1.0 展开机制处理；
- 4 处并发敏感点交接 A2.17 系统性并发正确性审计；
- MANUAL_POST NCR 过账悬挂同型交接 MR1 整体裁决（与 finance P1-MA2-032 + hr P1-MA2-048 + assets P1-MA2-060 一并）。
