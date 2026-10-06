# 2026-09-21-1200-1 ai-check r1 修复批次 F3.11：assets 域 13 条 P2（HEAD 复核实际开放面）

> Plan Status: completed
> Last Reviewed: 2026-10-06
> Source: ai-check-index.md ast/ast2 系 open 行 + ck-assets-lifecycle.md / ck-assets-depreciation.md
> Audit: required
> **Protected Area**: assets 折旧/价值过账链——实施前须双独立子 agent 批准流程（同 F3.8-F3.10）
> **HEAD 复核**：独立 agent（2026-09-21，HEAD e3d4a1af3）逐条实核 14 条 open 行：13 条仍开放、ast2-014 部分开放（UNITS 半边已被 ast2-001 覆盖）、ast-016≡ast2-010 为同代码点重复登记合并修

## Current Baseline

**确认记录（WI-05，2026-10-06，独立确认子代理 HEAD 5fcbf44b7 复核）**：13/13 全部仍存在（0 消失/0 部分存在）——快照 HEAD `e3d4a1af3`→`5fcbf44b7` 间仅 `110f84069`（perf-ux 0325-2 看板读路径）触及 assets 主代码且零口径变更；`git log -S` 三组关键串（ERR_AST_MERGE_SOURCE_DUPLICATE/dashboard postedTransferFlag/assertCanReverseCapitalize）均无修复提交，与 ai-check-index 现行 open 行一致。逐条锚点与红灯测试落点见确认矩阵（锚点 file:line 已核，执行阶段按表落位）。Baseline 订正 5 点已吸收：①HEAD 更新；②ast-010 锚点漂移（sumCipBalance 已改 GROUP BY 投影聚合——`110f84069`，双计语义原样，修在投影聚合新形态上加 CostItem 过滤）；③ast-008 注记（Cap 链 reverse 前已有 P1-CK-ast2-004 已执行折旧守卫 `ErpAstAssetCapitalizationProcessor.java:128-132`——assert 上提不得破坏其先于 reverse 的次序）；④ast2-014 措辞（ERR_DEPRECIATION_USEFUL_LIFE_INVALID 已定义于 ErpAstErrors.java:121 零消费——「消费既有码」非「新增接线」）；⑤ast-009（F3.1 orgId 过滤）与 ast-010 无轴交叠；实施时 Dashboard 变更顺带按 null-skip 契约补 orgId（批内注记非范围扩张）。

- HEAD 5fcbf44b7（perf-ux round-3 收官后）。全 reactor 4164/0/0/1、checker R2c=1571 全绿基线见 docs/testing/known-good-baselines.md 2026-09-21 行。
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

> **红灯测试映射表（WI-05 红灯义务逐条落位——M1）**：每条 finding 一个红灯载体（测试类#方法 或机械断言形态），修复前红/修复后绿：

| # | Finding | 红灯载体 | 绿灯断言 |
|---|---|---|---|
| 1 | ast-008 | `TestErpAstPostingReverse`（扩）：不可逆态资产 → reverse 调用 | 守卫失败断言 `executor.reverse` 零调用 + 领域码 |
| 2 | ast-010 | `TestErpAstDashboard`（扩既有 cipBalance 断言基座） | 部分转固后 cipBalance = 未转固成本 |
| 3 | ast-011 | `TestErpAstCipTransfer`（扩） | 外来 capitalizationId → 业务错误码 |
| 4 | ast-012 | `TestErpAstInventory`（扩，按 Decision 裁决分支） | 对应分支断言（拒绝码或资产侧补偿） |
| 5 | ast-013 | `TestErpAstSplitMerge`（扩） | 悬空源 → 业务错误码（带行索引）非 NPE |
| 6 | ast-014 | 同上 | 重复源 → 新码 ERR_AST_MERGE_SOURCE_DUPLICATE + 价值单计 |
| 7 | ast-016≡ast2-010 | `TestDepreciationCalculator`（纯函数直断） | 名义轨迹 no-op + 减值轨迹下降〔A3：no-op 断言双夹具尺度声明——整除夹具精确相等 + 舍入反馈夹具按记账尺度/明示 1e-4 容差〕 |
| 8 | ast2-007 | `TestErpAstCatchUpDepreciation`（扩） | 晚于目标期 EXECUTED 行不污染 DECLINING 期数 |
| 9 | ast2-008 | 行为载体 `testAst2_008BatchIsolationFailureFirstThenHealthyAssetsComplete`（隔离/零写/完成断言） | 告警派发面由镜像先例佐证：dispatchFailureAlert 为 TestDepreciationPostingFailureAlert 实测范式的机械镜像（同注入/同事件键族/同降级语义）+ 代码审查；实施期如实登记（桩接线未在载体内复制） |
| 10 | ast2-009 | `TestErpAstMaintenance`（扩） | GL 失败 status 保持 COMPLETED + 告警 + 可重试；**分支 i 专属断言（已回写落地）**：testAst2_009CapitalizeGlFailureRollsBackOriginalValueAndRetryable 断言 CAPITALIZE 失败 rollbackCapitalization 配对回滚/原值单倍不双计 + 重试成功翻 POSTED |
| 11 | ast2-012 | 查询次数机械断言 | period 单查 + count 查询 + 类别预取后查询计数下降 |
| 12 | ast2-013 | `TestErpAstAcctDocProviderAccountKey`（扩，按 Decision 分支） | SPARE_PART→1403 分腿（或分支 b 删除面编译+owner doc 核验） |
| 13 | ast2-014 余面 | `TestErpAstAssetCrudSmoke`（扩，`_cases` 快照设施先例） | 三要素违规 → ERR_DEPRECIATION_USEFUL_LIFE_INVALID |

### Phase 1 — Merge/VA 输入校验族（ast-013/014/016≡ast2-010）

Status: planned
Targets: ErpAstMergeProcessor、ErpAstValueAdjustmentProcessor、ErpAstErrors
Skill: none

- Item Types: `Fix | Decision`
- [x] ast-013：validateSources 对悬空源引用（null）抛业务错误码（带行索引入参），对齐 Split 范式；消灭 executeApprove NPE 面——`ERR_AST_MERGE_SOURCE_NOT_FOUND`（erp.err.ast.merge.source-not-found，mergeCode+lineIndex）
- [x] ast-014：validateSources 增 sourceAssetId Set 去重（`ERR_AST_MERGE_SOURCE_DUPLICATE` 新增落 ErpAstErrors），对齐 Split validateLines 去重守卫
  - [x] ast-016≡ast2-010 Decision：减值/重估折旧基数联动——分支 A（推荐）：calculator 消费点按减值后净值轨迹取基数（不触 ORM；**config `erp-ast.revaluation-adjust-depreciation-base`（ErpAstConstants L26）随接线转为生效开关**；名义轨迹代数恒等已验算——(nbv−残值)/剩余期 ≡ (原值−残值)/总期数；**依赖注记（A1 强制化）：剩余期分母依赖 elapsed 口径（ast2-007，Phase 3）——名义轨迹 no-op 不受影响；catch-up/重执行窗口对 DECLINING 同面、对直线法为回归面（旧法不消耗 elapsed，新法经剩余期分母消耗）——ast2-007 必须先于分支 A 或同变更集原子落地**）；分支 B：删死代码 + config 声明移除 + owner doc §十 登记 Deferred（根治需基数持久列，ask-first）→ 裁决后实施

Exit Criteria:
- [x] 三项落地；新测试红→绿；ast 既有测试零回归

### Phase 2 — CIP/盘点族（ast-010/011/012）

Status: planned
Targets: ErpAstDashboardBizModel、ErpAstCipReverseTransferProcessor、ErpAstInventoryReverseProcessor/状态机、ErpAstErrors
Skill: none

- Item Types: `Fix | Decision`
- [x] ast-010：sumCipBalance 读侧修——按 CostItem `postedTransferFlag=false` 汇总（amountFunctional 同源口径；先取未完结 CIP id 集〔id 投影〕再 in+分组聚合；实施期订正：纯聚合无维度字段触发 MdxQuerySplitter NPE，改 cipId 维度分组求和）〔C2：dashboards.md L306 同步随 Phase 5〕；Proof：部分转固后 KPI 余额=未转固成本（testAst010CipBalanceExcludesTransferredCost 红转绿）
- [x] ast-011：reverseTransfer 前置归属校验——capCostItems 的 `cipId` 逐项匹配当前 CIP，不满足抛 `ERR_CIP_REVERSE_OWNERSHIP_MISMATCH`（新增）
  - [x] ast-012 Decision（M2 修订——原「分支 B 引用不存在的补偿流程」已证伪，三分支重写）：**前提实证**——盘盈新卡「建卡不入账」（inventory.md §四 L62，无 Capitalization 单可逆）；盘亏 SCRAPPED 经 `inventoryShortageDisposal` 直接 dao 置位（无 Disposal 单）；SCRAPPED 唯一出边 `reverseDisposal`（SCRAPPED/SOLD→IN_SERVICE）仅由处置链 posted 窗口调用——**仓内不存在盘点来源的资产侧补偿流程**；inventory.md §五 L71 已声明资产侧「需经资产域冲销流程独立处理」但无接线入口。三分支：**(a) reverse 内联资产侧回滚**（复用既有 `reverseDisposal` 边形增 shortage 逆动作 + 盘盈新卡补偿 mutation）；**(b) 拒绝 reverse 且本批内落地资产侧补偿 mutation**（盘点来源专用）；**(c) 拒绝 reverse + owner doc 明文登记「POSTED 盘点单含资产侧效果时单据轴不可纠错」为接受契约**（须说明为何优于现状）。任一分支均须引用 inventory.md §五/L71 并登记语义变更（纳入保护区预注面）→ **裁决=分支 (c)（拒绝 + owner doc 契约登记）**：现状=POSTED 盘点单可 reverse 而资产侧 SCRAPPED/新卡静默残留（静默不一致）；(c) 将其变为响亮阻断（存在 newAssetId/disposalId 行即抛 `ERR_AST_INVENTORY_REVERSE_ASSET_SIDE_EFFECTS`，新增），纠错走资产域冲销流程独立处理——符合「响亮崩溃优于静默错算」域原则且零新状态机边/零新 mutation（(a)/(b) 均需 SM 加边或新 mutation，归 Deferred 触发条件：产品立项盘点资产侧自动补偿时）。红灯：testAst012c×2 红→绿

Exit Criteria:
- [x] 三项落地；新测试红→绿；ast 既有测试零回归

### Phase 3 — 折旧执行/红冲守卫族（ast-008/ast2-007/ast2-009）

Status: planned
Targets: ErpAstAssetCapitalizationProcessor、ErpAstDisposalProcessor、ErpAstDepreciationScheduleProcessor/Execute/CatchUp Processor、ErpAstMaintenancePostProcessor、MaintenanceExpensePostingDispatcher
Skill: none

- Item Types: `Fix | Decision`
- [x] ast-008：两链 executeReverseApprove 将 assertCanReverseCapitalize/Dispose 上提到 postingDispatcher.reverse 之前（ast2-004 守卫次序保持——实测确认其先于 reverse 不变）；红灯 testAst008Cap/Disposal×2 红→绿
- [x] ast2-007：countExecutedBefore(assetId, periodExclusive) 新增（lt 字典序过滤），Execute 调用点改用（**wasExecuted 补偿随之删除——lt 已排除本期自身行**）、CatchUp 调用点以 currentPeriod 为界（missedPeriods 中已执行跳过期由初始计数天然包含）；count 查询优化归 Phase 4；〔C4 已登记：RecalculateForCapitalizationMaintenanceProcessor:46 保留无过滤 countExecuted + as-of-now 语义注释〕；红灯 testAst2007 红→绿
  - [x] ast2-009 Decision：维修 post 改「过账成功才翻 POSTED」（失败保持 COMPLETED 可重试，对齐 VA voucherId 门控范式）+ tryPost 失败补 dispatchFailureAlert（对齐 mfg/sal 告警范式）。**危险面命名（M3）：`applyTreatmentCapitalize`（ErpAstMaintenanceProcessor L95-111）非幂等——无条件 `originalValue += increment`；「失败保持 COMPLETED 可重试」若直接落地，CAPITALIZE 路径 GL 失败 → 重试 → 资产原值/净值双倍增量而 GL 仅一凭证（比现状重试封死更危险）。三分支裁决面：(i) 失败时 `rollbackCapitalization` 配对（F1.2 对称回滚范式）；(ii) `applyTreatmentCapitalize` 加 posted/voucherId 门控幂等；(iii) 资产联动后移至 voucherId 成功分支——(iii) 须与 F1.2 在 VA 链**前移**的理由对勘（VA 前移因无 sweep 覆盖，维修链须先声明 sweep 覆盖面再选向），裁决后实施

Exit Criteria:
- [x] 三项落地；新测试红→绿；ast 既有测试零回归

### Phase 4 — 批量折旧性能/死代码/建卡校验族（ast2-008/012/013/014余面）

Status: planned
Targets: ErpAstDepreciationScheduleExecuteBatchDepreciationProcessor、ExecuteDepreciationProcessor、MaintenanceExpenseAcctDocProvider/PostingDispatcher、ErpAstAssetBizModel、ErpAstErrors
Skill: none

- Item Types: `Fix | Decision`
- [x] ast2-008：批量循环逐资产失败补告警派发（注入 IErpSysNotificationBiz，事件键复用 ast.depreciation-posting-failure——镜像折旧 G4 范式）+ session 脏写隔离**落地=失败 catch 内 clearSession / 成功后 flushSession**（aps ErpApsOperationOrderBizModel:147 先例；既有凭证 REQUIRES_NEW 边界不动）；载体断言=隔离/零写/完成（告警面镜像先例佐证，红灯表 #9 如实登记）——**实施期如实披露：本条与 ast2-012 初次执行轮被跳过（计划误勾），复审发现后补实现并全模块复跑 369/0/0**
- [x] ast2-012：批量入口 requirePeriodOpen 既有单查；countExecuted/countExecutedBefore 改 `dao.countByQuery(q)`（全量物化消除，实读在案）；类别经会话缓存预取（循环内 getCategory 命中会话）；行为等价断言 testAst2_012 绿（机械面 count 查询代码审查实读佐证）——同上披露：初次执行轮跳过，复审后补实现
  - [x] ast2-013 Decision（**裁决=分支 a**——owner doc L88/L154 既有声明的代码落地，B 审查确认「分支 a 一致」）：dispatcher billData 增 `MAINTENANCE_SPARE_PART_AMOUNT`（SPARE_PART 成本行合计，常量新增）；Provider 非 linkedVisit 分支拆分贷腿——SPARE_PART 额贷存货 1403（独立映射键 INVENTORY_SPARE_PART，无规则表匹配时保留 Provider 1403）+ 余额贷银行 1002，clamp 防超总额；无备件行单腿贷银行行为同旧。快照重录：testThreeCostTypesAggregated 的 voucher_bill_r/voucher_line 重录（新三腿结构借 6602/6000 贷 1403/2000+贷 1002/4000 与 owner doc 一致，人工核验在案）
- [x] ast2-014 余面：Asset BizModel defaultPrepareSave/Update 补折旧三要素校验（usefulLifeMonths>0、0≤残值<原值、方法 dict 内）并接线 ERR_DEPRECIATION_USEFUL_LIFE_INVALID；UNITS 半边引用 ast2-001 既有执行层拒绝（不重复）〔C1：校验作用域显式裁定——DRAFT「草稿待完善」语义 vs 强校验（已登记 state-machine.md §8 DRAFT 行，审计核验；非 §2）；残值违规文案扩展/换码随 i18n 检查闭合〕

Exit Criteria:
- [x] 四项落地；新测试红→绿；ast 既有测试零回归

## Protected Area Approval Record

- Approval agent 1（plan review，fresh session）: **APPROVED with conditions（3 条件）**（2026-10-06）——13/13 锚点实码核验零漂移（基线 HEAD 5fcbf44b7 vs 当前 3c07920f8，`git diff module-assets/` 空）；折旧金额专项演算（直线/双倍余额/名义轨迹恒等式验算 + SCALE=4 舍入反馈 1e-4 漂移反例 + CatchUp 直线法回归面识别）。条件：**A1** ast2-007 排序强制化（ast-016 分支 A 时 ast2-007 须先于/同变更集原子落地；「Phase 1-3 间同面非回归」修正为「对 DECLINING 同面、对直线法为回归面」）；**A2** 预注「折旧凭证键/金额零变更」补注「限名义（按期顺序）执行轨迹；ast2-007 对错位场景的金额纠正为目标行为不属违约」；**A3** 红灯 #7 no-op 断言尺度声明（整除夹具证精确相等 + 舍入反馈夹具按记账尺度/明示容差）。
- Approval agent 2（protected-area owner-doc conformance，fresh session）: **APPROVED with conditions（4 条件）**（2026-10-06）——13 条 owner doc 对位矩阵（9 一致/4 需修订且计划已内建修订义务）；实码 13/13 独立复核属实（新发现 ast2-007 第三调用点）。条件：**C1** ast2-014 三要素校验作用域裁定（DRAFT 态「草稿待完善」语义 vs 建卡强校验，须 state-machine.md §8 登记；残值违规沿用 ERR_DEPRECIATION_USEFUL_LIFE_INVALID 须扩文案或换码）；**C2** ast-010 聚合源改 CostItem 后 dashboards.md L306 字段口径行同步；**C3** ast-012 登记章节最小集（inventory.md §一/§五 + 分支 a/b 另触 state-machine.md §2/§3/§5）；**C4** ast2-007 第三调用点 `RecalculateForCapitalizationMaintenanceProcessor:46` 语义显式保留（维修资本化重算 as-of-now，禁止静默改口径）。
- **双批准成立（A+B 均 APPROVED），计划按 7 条件实施；实施期义务追加**：ast2-009 分支裁决后分支专属断言回写红灯表 #10；ast2-013 分支 b 须同步 maintenance.md §四 L84/L88 + §九 L154；ast-016 分支 B 须 depreciation-and-posting.md §十 Deferred 登记。
- 预期约束面（起草预注，iteration 1 消歧修订 + A2 条件补注）：折旧凭证键/金额零变更——**限名义（按期顺序）执行轨迹；ast2-007 对错位场景的金额纠正为目标行为，不属违约〔A2〕**；「REQUIRES_NEW 边界零变更」限定为**既有**凭证 REQUIRES_NEW 边界不动（ast2-008 若裁决逐资产独立事务边界属**新增**边界，不违反本预注）；ast2-009 状态语义改动须与 ReversalListener/批处理 job 路径对位核验（含 inventory.md §五/L71 既有契约声明对位——ast-012 任一分支）；ast2-013 涉科目口径须 owner doc maintenance.md 对位（L88/L154 既有声明）

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（独立草案审查子代理，fresh session，2026-10-06）——3 Major（M1 红灯义务未逐条落位 / M2 ast-012 分支 B 引用不存在的补偿流程〔前提实证证伪〕/ M3 ast2-009 嵌入未类型化双计危险裁决面）+ 7 Minor，10 项最小修订集；基线实核通过（13 条锚点全实存）。
- Independent draft review iteration 2: `needs revision → 全部落盘待确认`（同审查者增量复审，2026-10-06）——复审读取与执行者落盘发生竞态：其 R1-R7 指认缺席的 5 项（ast2-009 三分支/ast2-008 承诺式/count 归属/验证门扩充/UNITS 重开条件）与预注同步，在复审读取后已全部落盘并按其替换文本对齐（含红灯表 #9 隔离断言/#10 分支扩展注、杂散字符清理）；Draft Review Record 本两行即 R8 收尾。请审查者对当前盘面做增量确认后翻 active。
- Independent draft review iteration 2 (final): `accept`（同审查者，2026-10-06）——R1-R8 逐行比对全部落盘（含 R4 预注 maintenance.md 对位修正属改善）；**实施期约束义务**：ast2-009 三分支裁决落定时必须将分支专属断言回写红灯表 #10（失败回滚后原值单倍/幂等 no-op/后移次序断言），由结束审计核验；红灯表 #13 杂散「:28」实施期清理。计划转为可执行契约，保护区双批准先于 Phase 1。

## 执行记录（2026-10-06 实施轮）

- **红灯取证（stash 法）**：`git stash push -- src/main` 后跑行为载体 → **12 项行为失败**（testAst010CipBalanceExcludesTransferredCost / testAst2007DecliningElapsedIgnoresExecutedRowsAfterTargetPeriod / testAst016×2〔净值轨迹+舍入反馈〕/ testAst2_014×3〔Save 拒绝×2+IN_SERVICE 完备性〕/ TestErpAstPostingReverse×2〔ast-008〕/ TestErpAstMaintenance ast2-009 族 + 其余）+ 新契约载体编译失败（`ERR_AST_*`/`BILL_DATA_MAINTENANCE_SPARE_PART_AMOUNT`/countExecutedBefore 找不到符号——新契约缺失的机械红证）→ stash pop 后 **369/0/0 全绿**。日志 /tmp/redlight4.log（会话内留证，关键失败行已转录本节）。
- **快照重录**：testThreeCostTypesAggregated 的 voucher_bill_r/voucher_line 经 @EnableSnapshot(saveOutput) 重录——新三腿结构（借 6602 维修费用 6000 / 贷 1403 存货 2000 / 贷 1002 银行存款 4000）人工核验与 owner doc maintenance.md L88/L154 既有声明一致；既有审计轨迹用例种子补三要素（IN_SERVICE 迁移被新校验正确拒绝→种子适配业务契约）。
- **实施期订正（规则 10 如实记录）**：①ast-016 config「转生效开关」不可行（净值轨迹恒开，无分离语义承载）→ config 退役 no-op + 死块/死门删除（见 Phase 1 条目）；②sumCipBalance 纯聚合 NPE → cipId 维度分组；③Phase 实施顺序 Phase 3 先行（A1）。

## Closure Gates

- [x] 范围内行为完成（Phase 1-4 全部退出标准勾选——13/13 修复 + 13 红灯载体 + 快照重录 2）
- [x] 相关文档对齐（owner doc 登记：dashboards.md L306〔C2〕/state-machine.md §2 DRAFT 注记〔C1〕/depreciation-and-posting.md §十 分支 A〔+config 退役〕/inventory.md §五.3+§一〔C3 分支 c〕；ai-check-index.md 回填随 WI-06 确认矩阵；roadmap 进度；known-good-baselines 基线行随收官；compliance-baseline per-site +2；docs/logs 当日日志随收官）
- [x] 已运行验证：`mvn test -pl module-assets/erp-ast-service` **369/0/0 全绿**（含 13 红灯载体）；全 reactor `mvn install -DskipTests` BUILD SUCCESS（156 模块）+ `mvn test` 556/0/8 于 fin-service——**8 Errors 为干净 HEAD 归因的预存快照族失败**（TestErpFinVoucherTemplate* 日期敏感 NopSysChangeLog 变量族；stash 归因实证 + 审计依赖路径论证双确认，与本批无关，按 2026-08-31 基线行先例披露登记 successor）；compliance checker exit 0（per-site +2 注记落 compliance-baseline.md）；`node tools/check-hardcoded-cjk.mjs --strict` PASS（0 new violations）+ `i18n-coverage-checker.sh` PASS（0 defects）；`check:unbounded-queries` PASS（71 站点台账零新增）；`mvn test -pl app-erp-all` **80/0/0/1 全绿**；快照重录义务已兑现（testThreeCostTypesAggregated voucher_line/bill_r，人工核验）；E2E 不适用注记（零 view/flux 触及，git status 可证，沿 0355-5 形态）
- [x] 无范围内项目降级为 deferred/follow-up（ast-012 分支 c / ast2-009 分支 i / ast2-013 分支 a / ast-016 分支 A 四裁决均在批准范围；ast-016 config 退役偏离经规则 10 如实记录）
- [x] 独立草案审查已完成并记录（2 轮收敛 accept，见 Draft Review Record）
- [x] 保护区双独立子 agent 批准记录落盘（A×3 条件 + B×4 条件，全条件已履行——A3 红灯 #7 双夹具/A1 排序/C4 第三调用点/C1 作用域裁定/C2 dashboards.md/C3 登记章节）
- [x] 文本一致性已验证（收官时终验——随 Closure 粘贴完成 6 项簿记整改）
- [x] 结束审计由独立子代理执行（fresh session ACCEPT——6 项 Minor 簿记整改随 Closure 落盘）
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### ast2-014 UNITS 半边（前批次已修，HEAD 复核确认）

- Classification: `resolved elsewhere`
- Why Not Blocking Closure: ast2-001（F2.9）执行层显式拒绝 ERR_DEPRECIATION_UNITS_NOT_CONFIGURED，静默恒 0 已消除
- Reopen Trigger: 工作量（UNITS）数据模型/录入面落地时，并入 owner doc §十 UNITS 计算通路 Deferred 条目统一裁决
- Successor Required: `no`

## Closure

Status Note: (pending)

Closure Audit Evidence:

- Auditor / Agent: (pending)

Follow-up:

- (pending)
