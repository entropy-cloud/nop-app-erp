# 2026-09-17-0800-1 ai-check r1 修复批次 F3.10：mfg 域 14 条 P2（HEAD 复核实际开放面）

> Plan Status: active
> Last Reviewed: 2026-09-17
> Source: ai-check-index.md mfg 系 open 行 + ck-mfg-workorder.md / ck-mfg-bom-mrp.md / ck-mfg-subcontracting.md
> Audit: required
> **Protected Area**: mfg 域——双独立子 agent 批准流程同 F3.8/F3.9
> **保护区约束（第一批准者 iteration 1/2）**：凭证键/金额/REQUIRES_NEW 边界零变更；不触 ORM 模型与 nop-entropy；mfg3-006 镜像 F2.5 范式（NopException 中止 @BizMutation 保持可重试）；mfg3-009 仅限门控与 posted 回写；mfg2-010 新增抛错仅 qty≤0；mfg-007 kit 沿用 null-skip 契约

## Current Baseline

- HEAD `32c775591`。审查 iteration 1 实测 12 锚点 + iteration 2 确认：mfg-010、mfg3-010、mfg2-012、mfg3-011 四条已在前批次修复；mfg-007 dashboard 半边已修复。**实际开放面恰 14 条**：
  - mfg-006（快照族守卫惰性——核心实体基类守卫已在位）
  - mfg-007（kit 半边 orgId——dashboard 半边已修）
  - mfg-008（consumption 零消费）
  - mfg2-004（MRP warehouseId——需 owner doc Decision）
  - mfg2-006（lookupStandardCost 价源——需 Decision）
  - mfg2-007（session 托管写值）
  - mfg2-009（useMultiLevelBom 零消费）
  - mfg2-010（qty=0 除零 + CostRollupService 姊妹）
  - mfg2-011（最晚 requirementDate + SimulationMrpEngine fork）
  - mfg3-006（红冲吞异常——F2.5 范式修复）
  - mfg3-007（入参边界）
  - mfg3-008（去重丢量）
  - mfg3-009（反转链悬挂——门控+posted 回写）
  - mfg3-012（委外 reverseApprove 守卫）
- 已修复（HEAD 复核确认，索引不动）：mfg-010（r3 M2.3）、mfg3-010（F3.8 前批）、mfg2-012/mfg3-011（F1.3 基类守卫）、mfg-007 dashboard 半边（plan 2026-09-12-1000-1）
- 测试基线：mfg-service 全绿

## Goals

- 修复上述 14 条 P2，回填索引/roadmap，终态 fixed/deferred 按 adjudicated 分支。

## Non-Goals

- mfg-009（设计分歧需产品裁决——STOCK_RESERVED 取消路径需迁移表修订）
- mfg-011（REQUIRES_NEW 后可失败步骤编排——领料链在码裁决 Deferred F1.2；工单链残余同型归 Deferred）
- mfg2-005（MRP→CRP 负荷链需 CRP owner 裁决）
- mfg2-008（cost-rollup-status 状态机需模型设计——bom-and-routing.md §实现注记；重开触发：Forecast approve/cancel 范式需求）
- mfg-010/mfg3-010/mfg2-012/mfg3-011/mfg-007 dashboard 半边（前批次已修，HEAD 复核确认，索引不动）

## Task Route

- Type: `implementation-only change`
- Owner Docs: docs/design/manufacturing/ 各 owner doc（workorder.md、bom-and-routing.md、subcontracting.md、mrp.md）
- Skill Selection Basis: 代码阶段 `Skill: none`

## Infrastructure And Config Prereqs

- 无新增基础设施。

## Execution Plan

### Phase 1 — mfg-006 快照族守卫 + mfg-007 kit orgId

Status: planned
Targets: ErpMfgWorkOrderBomSnapshotBizModel、ErpMfgWorkOrderLineSnapshotBizModel、ErpMfgWorkOrderOperationSnapshotBizModel、KitAvailabilityChecker.java
Skill: none

- Item Types: `Fix`
- [ ] mfg-006：BomSnapshot 三实体 override `defaultPrepareUpdate` 与 `defaultPrepareDelete`——快照不可变语义（update 抛错；delete 保留 cascade-delete 不经 prepare 钩子故不受影响）
- [ ] mfg-007：KitAvailabilityChecker#buildBalanceQuery 补 `eq("orgId", wo.getOrgId())`（沿用 dashboard null-skip 契约）
- [ ] Proof：快照 update 被拒 + orgId 隔离断言

Exit Criteria:
- [ ] 两修复落地；新测试红→绿；mfg 既有测试零回归

### Phase 2 — mfg-008 consumption + mfg2-009/010/011 BOM/MRP 四缺陷

Status: planned
Targets: ErpMfgMaterialIssueConfirmProcessor.java、BOM 展开逻辑、MrpEngine、SimulationMrpEngine、CostRollupService
Skill: none

- Item Types: `Fix`
- [ ] mfg-008：confirm 按 bom.consumption 分级——STRICT 超领（对 WO 行 requiredQuantity 累计）抛错/WARNING warn/FLEXIBLE 放行
- [ ] mfg2-009：useMultiLevelBom=false 时单级展开（齐套/预留展开深度控制；MRP 不受该列治理）
- [ ] mfg2-010：BOM qty≤0 抛错（不静默归零）+ CostRollupService.divide 姊妹站点同修；成功路径数值零变化（保护区约束 4）
- [ ] mfg2-011：requirementDate 聚合改最早 + SimulationMrpEngine#topDemandsByMaterial fork 同步
- [ ] Proof：各修复红→绿

Exit Criteria:
- [ ] 四缺陷修复；新测试红→绿；mfg 既有测试零回归

### Phase 3 — mfg3 组委外域四缺陷

Status: planned
Targets: ErpMfgSubcontractOrderProcessor、ErpMfgSubcontractOrderReceiveFinishedProcessor、ErpMfgSubcontractOrderBizModel、genealogy/BatchGenealogyWriter
Skill: none

- Item Types: `Fix`
- [ ] mfg3-006：红冲吞异常改 F2.5 范式（NopException 中止 @BizMutation，保持 DONE+posted=true 可重试；不天真 rethrow 破坏部分红冲幂等——现注释「swallowed to keep idempotency」语义须显式重裁决）
- [ ] mfg3-007：仓库参数非空守卫 + 收货数量上限守卫（损耗扣除口径 scope 注记）+ 非正数量拒绝
- [ ] mfg3-008：多领料行去重改 sum 聚合（inputQty 累加替代首行）
- [ ] mfg3-012：reverseApprove 补委外语义 docStatus 白名单（≤APPROVED/未发料——工单词汇 NOT_STARTED 不适用）
- [ ] Proof：各修复红→绿

Exit Criteria:
- [ ] 四缺陷修复；新测试红→绿；mfg 既有测试零回归

### Phase 4 — mfg2-004 Decision + mfg2-006 Decision + mfg2-007 + mfg3-009

Status: planned
Targets: MrpEngine/DemandAggregator、SimulationVersionComparator、差异重算链
Skill: none

- Item Types: `Decision | Fix`
- [ ] mfg2-004 Decision：owner doc mrp.md L88 裁决（行漂移；L88 现文背书 MRP 忽略仓库维度，Decision 须同步修订 L88；"三处契约"实为四处视角：mrp.md L88/ORM 注释/DRP javadoc/实现分支） warehouseId 过滤（MRP 过滤 vs 三处契约改声明）→ 裁决后实施对应分支
- [ ] mfg2-006 Decision：lookupStandardCost 价源选择——接 CostRollupLine FIRMED 最新 或 owner doc simulation-engine.md Decision C 显式 Deferred + 重开触发
- [ ] mfg2-007：SimulationVersionComparator.indexLines session 托管写值改本地聚合 Map（零实体写入）
- [ ] mfg3-009：reverseIfExists 门控改按凭证存在性（或追加「存在未红冲同码凭证」分支）+ 幂等命中时 posted 回写——仅限门控与 posted 回写修复，不触差异金额计算
- [ ] Proof：各修复红→绿

Exit Criteria:
- [ ] 四项落地；新测试或行为等价断言红→绿；mfg 既有测试零回归

## Protected Area Approval Record

- Approval agent 1（plan review，agent_85f00db2）: **pass**（iteration 3, 2026-09-17）——iteration 1 有条件通过（B1-B6 前置）→ iteration 2 确认 B1-B4/B5 部分解决 + R1/R2 点状残留 → iteration 3 accept + 第一批准 pass（约束以计划头部引用块为准）
- Approval agent 2（protected-area owner-doc conformance，agent_7905f151）: **approve（附 6 约束，C1 阻断性）**（2026-09-17）——32 处实码独立复核不依赖第一批准者声称；14 条缺陷实质全部成立；六项保护区约束全过。
  - **C1（阻断）**：mfg-007 dashboard 半边「已修复」失实——ErpMfgDashboardBizModel 各查询无 orgId 过滤、resolveOrgId 零调用死代码。修正：dashboard 各查询补 eq("orgId", resolveOrgId(context)) 纳入 Phase 1 范围（kit+dashboard 双半同批）。
  - C2：mfg3-009 posted 回写须区分同额重试命中（可回写）与红冲失败后命中旧凭证（不得回写，中止或保持 false）。
  - C3：mfg3-006 F2.5 化须对「部分已红冲后重入」显式裁决+测试断言。
  - C4（残留登记）：mfg2-010 BOM 写侧 qty>0 校验不在本批，closure 登记follow-up。
  - C5：Task Route workorder.md 不存在（owner 语义在 README.md §3 + bom-and-routing.md + material-reservation.md），修正引用。
  - C6：closure 时 owner doc 登记（subcontracting.md 红冲/mrp.md L88/bom-and-routing.md consumption+useMultiLevelBom）不得只改码不改 doc。

## Draft Review Record

- Independent draft review iteration 1: needs revision (agent_85f00db2, 2026-09-17) because B1 Baseline 失实（4 条已修+mfg-007 半边）；B2 mfg3-012 工单词汇错配；B3 mfg3-009 机制错配；B4 mfg2-006 no-op 违反不可降级；B5 mfg2-004 缺 Decision；B6 形式要件缺失。非阻塞 8 条全部采纳。
- Independent draft review iteration 2: needs revision (agent_85f00db2, 2026-09-17) because 重写误删模板段落（Exit Criteria/类型标注/Skill/Targets/Task Route/Draft Review Record/Protected Area Record/Closure/验证命令）+ mfg2-004 覆盖漏洞 + Deferred 不合规。非阻塞 iteration 2 非阻塞 1/2/3 采纳。本轮全面重写恢复全部模板段落 + 恢复 mfg2-004 + Deferred 合规化 + N-a/b/c 落盘。
- Independent draft review iteration 3: accept (agent_85f00db2, 2026-09-17) after iteration 2 三阻塞全部解除 + 模板段落全恢复 + mfg2-004 覆盖恢复 + Deferred 合规化。5 条非阻塞修正随激活落盘（mrp.md L88 引用/Phase 3 Targets 具名/mfg2-009 MRP 删除/N-a-b-c 出处修正/终态措辞）。保护区第一批准 pass（agent_85f00db2，iteration 3）。
- Draft Review Record 注：iteration 2 中引用的 "N-a/N-b/N-c" 实为 iteration 2 审查者自身提出的非阻塞 1/2/3 项，非 F3.9 批语言。

## Closure Gates

- [ ] 范围内行为完成（Phase 1-4 全部退出标准勾选）
- [ ] 相关文档对齐（owner doc 登记；ai-check-index.md 回填；roadmap 进度；baselines 基线行；compliance-baseline 裁决；docs/logs 当日日志）
- [ ] 已运行验证：`mvn test -pl module-manufacturing/erp-mfg-service`（含新增测试类全绿）；全 reactor `mvn install -DskipTests` + `mvn test`（install 先于 test——F3.8 教训）；compliance checker
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 保护区双独立子 agent 批准记录落盘
- [ ] 文本一致性已验证
- [ ] 结束审计由独立子代理执行
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### mfg-009 STOCK_RESERVED 取消路径（设计分歧）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 代码与 owner doc 迁移树字面一致；需产品裁决迁移表修订（cancel 白名单补 STOCK_RESERVED/STOCK_PARTIAL + state-machine.md §2 同步）
- Successor Required: `yes`（触发条件：产品裁决迁移表修订后立项）

### mfg-011 REQUIRES_NEW 后可失败步骤（工单完工链）

- Classification: `watch-only residual`
- Why Not Blocking Closure: MfgPostingExecutor REQUIRES_NEW 边界是 processor-extension-pattern 硬规则 1 平台裁决；领料链在码裁决注释 L65-66「残余依赖移动单产出不可前移，归 Deferred（plan F1.2）」；工单链同型残余
- Successor Required: `yes`（触发条件：实际孤儿凭证案例出现时立项 afterCommit 编排或对账告警）

### mfg2-005 MRP→CRP 负荷链断裂

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 涉及 CRP 引擎签名变更（routingId/plannedEndDate 写入）与 CRP 查询 NULL 语义变更
- Successor Required: `yes`（触发条件：CRP owner 裁决口径后立项）

### mfg2-008 cost-rollup-status 状态机缺失

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 需模型设计（状态机 + writer + 迁移守卫）；bom-and-routing.md §实现注记已显式登记
- Successor Required: `yes`（触发条件：标准成本 FIRMED 消费链实际受阻时立项；备选：Forecast approve/cancel 范式的 purpose-built firm/cancel mutation）

### mfg-010/mfg3-010/mfg2-012/mfg3-011（前批次已修，HEAD 复核确认）

- Classification: `resolved elsewhere`
- Why Not Blocking Closure: 四条已在 r3 M2.3 / F3.8 前批 / F1.3 基类守卫中修复，HEAD 实测确认
- Successor Required: `no`

## Closure

Status Note: (pending closure audit)

Closure Audit Evidence:

- Auditor / Agent: (pending)

Follow-up:

- (pending)
