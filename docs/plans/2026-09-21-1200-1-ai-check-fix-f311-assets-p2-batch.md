# 2026-09-21-1200-1 ai-check r1 修复批次 F3.11：assets 域 13 条 P2（HEAD 复核实际开放面）

> Plan Status: active（草案已起草，开放面经独立子 agent HEAD 实核；待独立草案审查 + 保护区双批准后实施）
> Last Reviewed: 2026-09-21（起草轮）
> Source: ai-check-index.md ast/ast2 系 open 行 + ck-assets-lifecycle.md / ck-assets-depreciation.md
> Audit: required
> **Protected Area**: assets 折旧/价值过账链——实施前须双独立子 agent 批准流程（同 F3.8-F3.10）
> **HEAD 复核**：独立 agent（2026-09-21，HEAD e3d4a1af3）逐条实核 14 条 open 行：13 条仍开放、ast2-014 部分开放（UNITS 半边已被 ast2-001 覆盖）、ast-016≡ast2-010 为同代码点重复登记合并修

## Current Baseline

- HEAD e3d4a1af3（F3.10 mfg 批收官提交）。mfg-service 323/0/0、全 reactor 4164/0/0/1、checker R2c=1571 全绿基线见 docs/testing/known-good-baselines.md 2026-09-21 行。
- 实际开放面 13 条（ast-016 与 ast2-010 同点合并为一条后计数）：
  - ast-008（Cap/Disposal 反审核守卫次序——GL 红冲 REQUIRES_NEW 先于资产状态 assert）
  - ast-010（CIP 余额 KPI 部分转固双计）
  - ast-011（CIP reverseTransfer 无归属校验）
  - ast-012（盘点 reverse 不回滚资产侧——单据轴可逆 vs 资产轴不可逆不对称）
  - ast-013（Merge 悬空源 null 放行 → executeApprove NPE）
  - ast-014（Merge 源资产无去重——价值双计）
  - ast-016≡ast2-010（ValueAdjustment newDepreciableBase 死代码 + config 零效果——减值后未来折旧不降）
  - ast2-007（折旧 elapsed 口径缺目标期过滤——DECLINING 期数错位）
  - ast2-008（批量折旧单事务逐资产吞异常——session 脏写+凭证孤儿群+无告警）
  - ast2-009（维修 post 无条件翻 POSTED——重试封死无告警）
  - ast2-012（批量折旧 N+1 查询）
  - ast2-013（维修贷方科目分支死代码——inventorySubject 读了不用）
  - ast2-014 余面（资产建卡无折旧三要素校验——0/null 年限静默 1 个月折完、残值≥原值静默零提；UNITS 半边已覆盖）

## Goals

- 修复上述 13 条 P2，回填索引/roadmap，终态 fixed/deferred 按 adjudicated 分支。

## Non-Goals

- mfg2-008 同型（ast2-008 根治版 nop-batch chunk 收敛）——本批仅做逐资产事务/告警最小修，引擎收敛归 successor
- ast2-006 遗留的 1604 减值准备处置结转科目腿（前批 Deferred 登记，保护区域独立计划）
- ORM 加列类根治（如折旧基数持久列）——触发即 ask-first

## Task Route

- Type: `implementation-only change`
- Owner Docs: docs/design/assets/ 各 owner doc（depreciation.md、lifecycle/cip/inventory 相关 owner doc，起草实施阶段逐条对位）
- Skill Selection Basis: 代码阶段 `Skill: none`

## Infrastructure And Config Prereqs

- 无新增基础设施。

## Execution Plan

### Phase 1 — Merge/VA 输入校验族（ast-013/014/016≡ast2-010）

Status: planned
Targets: ErpAstMergeProcessor、ErpAstValueAdjustmentProcessor、ErpAstErrors
Skill: none

- Item Types: `Fix | Decision`
- [ ] ast-013：validateSources 对悬空源引用（null）抛业务错误码（带行索引入参），对齐 Split 范式；消灭 executeApprove NPE 面
- [ ] ast-014：validateSources 增 sourceAssetId Set 去重（新增 ERR_AST_MERGE_SOURCE_DUPLICATE），对齐 Split validateLines 去重守卫
- [ ] ast-016≡ast2-010 Decision：减值/重估折旧基数联动——分支 A（推荐）：calculator 消费点按减值后净值轨迹取基数（不触 ORM）；分支 B：删死代码 + config 声明移除 + owner doc §十 登记 Deferred（根治需基数持久列，ask-first）→ 裁决后实施

Exit Criteria:
- [ ] 三项落地；新测试红→绿；ast 既有测试零回归

### Phase 2 — CIP/盘点族（ast-010/011/012）

Status: planned
Targets: ErpAstDashboardBizModel、ErpAstCipReverseTransferProcessor、ErpAstInventoryReverseProcessor/状态机
Skill: none

- Item Types: `Fix | Decision`
- [ ] ast-010：sumCipBalance 读侧修——按 CostItem `postedTransferFlag=false` 汇总（不动 accumulatedCost 归集审计语义）；Proof：部分转固后 KPI 余额=未转固成本
- [ ] ast-011：reverseTransfer 前置归属校验——capCostItems 的 `cipId` 全部匹配当前 CIP（或 Cap 单 `sourceCode == cip.code`），不满足抛业务错误码
- [ ] ast-012 Decision：盘点 reverse 资产侧回滚需状态机增 shortage 逆动作（触保护区模型面）vs 收紧 reverse 前置（存在盘盈新卡/盘亏 SCRAPPED 时拒绝 reverse，提示走补偿流程）+ owner doc 登记 → 裁决后实施对应分支

Exit Criteria:
- [ ] 三项落地；新测试红→绿；ast 既有测试零回归

### Phase 3 — 折旧执行/红冲守卫族（ast-008/ast2-007/ast2-009）

Status: planned
Targets: ErpAstAssetCapitalizationProcessor、ErpAstDisposalProcessor、ErpAstDepreciationScheduleProcessor/Execute/CatchUp Processor、ErpAstMaintenancePostProcessor、MaintenanceExpensePostingDispatcher
Skill: none

- Item Types: `Fix`
- [ ] ast-008：两链 executeReverseApprove 将 assertCanReverseCapitalize/Dispose 上提到 postingDispatcher.reverse 之前（先加载资产校验状态再红冲）
- [ ] ast2-007：countExecuted 增 `lt("period", targetPeriod)` 过滤，Execute/CatchUp 两调用点统一传目标期（顺带 count 查询替代 findAll().size()——与 ast2-012 交叠部分归 Phase 4）
- [ ] ast2-009：维修 post 改「过账成功才翻 POSTED」（失败保持 COMPLETED 可重试，对齐 VA voucherId 门控范式）+ tryPost 失败补 dispatchFailureAlert（对齐 mfg/sal 告警范式）；CAPITALIZE 路径资产联动与 GL 失败的补偿面按 F2.5/F1.2 既有范式裁决注记

Exit Criteria:
- [ ] 三项落地；新测试红→绿；ast 既有测试零回归

### Phase 4 — 批量折旧性能/死代码/建卡校验族（ast2-008/012/013/014余面）

Status: planned
Targets: ErpAstDepreciationScheduleExecuteBatchDepreciationProcessor、ExecuteDepreciationProcessor、MaintenanceExpenseAcctDocProvider/PostingDispatcher、ErpAstAssetBizModel、ErpAstErrors
Skill: none

- Item Types: `Fix | Decision`
- [ ] ast2-008：批量循环逐资产失败补告警派发（dispatchFailureAlert）+ 评估 session 脏写隔离最小修（逐资产 flush/clear 或独立事务边界——以保护区约束裁决为准，根治 nop-batch chunk 归 Non-Goal）
- [ ] ast2-012：批量入口期间查询上提（查一次传入）+ countExecuted 改 count 查询 + 类别按 categoryId 批量预取
- [ ] ast2-013 Decision：维修贷方科目分支——按成本类型拆分贷方（SPARE_PART→存货 1403 / 其余→银行 1002，billData 传分项金额）或删存货分支声明 + owner doc 登记 Deferred → 裁决后实施
- [ ] ast2-014 余面：Asset BizModel defaultPrepareSave/Update 补折旧三要素校验（usefulLifeMonths>0、0≤残值<原值、方法 dict 内）并接线 ERR_DEPRECIATION_USEFUL_LIFE_INVALID；UNITS 半边引用 ast2-001 既有执行层拒绝（不重复）

Exit Criteria:
- [ ] 四项落地；新测试红→绿；ast 既有测试零回归

## Protected Area Approval Record

- Approval agent 1（plan review）: (pending)
- Approval agent 2（protected-area owner-doc conformance）: (pending)
- 预期约束面（起草预注）：折旧凭证键/金额/REQUIRES_NEW 边界零变更；ast2-009 状态语义改动须与 ReversalListener/批处理 job 路径对位核验；ast2-013 涉科目口径须 owner doc depreciation.md 对位

## Draft Review Record

- Independent draft review iteration 1: (pending)

## Closure Gates

- [ ] 范围内行为完成（Phase 1-4 全部退出标准勾选）
- [ ] 相关文档对齐（owner doc 登记；ai-check-index.md 回填；roadmap 进度；baselines 基线行；compliance-baseline 裁决；docs/logs 当日日志）
- [ ] 已运行验证：`mvn test -pl module-assets/erp-ast-service`（含新增测试类全绿）；全 reactor `mvn install -DskipTests` + `mvn test`（install 先于 test）；compliance checker
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 保护区双独立子 agent 批准记录落盘
- [ ] 文本一致性已验证
- [ ] 结束审计由独立子代理执行
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### ast2-014 UNITS 半边（前批次已修，HEAD 复核确认）

- Classification: `resolved elsewhere`
- Why Not Blocking Closure: ast2-001（F2.9）执行层显式拒绝 ERR_DEPRECIATION_UNITS_NOT_CONFIGURED，静默恒 0 已消除
- Successor Required: `no`

## Closure

Status Note: (pending)

Closure Audit Evidence:

- Auditor / Agent: (pending)

Follow-up:

- (pending)
