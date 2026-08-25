# ck-projects — projects 域实现代码检查报告

> 工作项：C5.1。执行日期：2026-08-25。执行者：域检查子代理（ZCode 会话）。
> 范围：`module-projects/erp-prj-service/src/main/java` 全部 57 个手写生产文件——根常量 3 类（`ErpPrjConstants`/`ErpPrjConfigs`/`ErpPrjErrors`）+ cost 5 类（`BudgetChecker`/`CostRateResolver`/`ExpenseCostAggregator`/`MaterialCostAggregator`/`ProjectCostAggregator`）+ entity BizModel 16 类（Project/Task/Timesheet/CostCollection(Settlement/SettlementLine)/ProjectPnl/ProjectSettlement/Billing(±Line)/Budget(±Line)/Milestone/ActivityType/ProjectType/ProjectUser/Role）+ posting 5 类（`ProjectPostingExecutor`/`TimesheetPostingDispatcher`/`ProjectCostCollectionProvider`/`ProjectSettlementPostingDispatcher`/`ProjectSettlementAcctDocProvider`）+ processor 18 类（工时 submit/approve/cancel + 结算 facade + create/approve/cancel/reverse/submit/reject/returnRetention + project close/hold/resume/refreshActualCost + pnl refreshPnl + costcollection aggregateMaterial/refreshExpense）+ statemachine 5 Bean + `TaskDependencyValidator` + `ProjectPnlCalculator` + `ErpPrjProjectPnlCalcHelper` + dashboard/report 各 1 + `_vfs` 接线（`app-service.beans.xml`/`erp-prj.data-auth.xml`/`prj/pnl-calc.batch.xml`、`app-erp-all/_vfs/nop/job/conf/erp-prj-pnl-calc.job.yaml`、两张种子报表 xpt.xml）。api/web 骨架按任务指令不深查。跨域实证：`ErpFinVoucherBizModel#post/reverse`（REQUIRES_NEW + 幂等 null）、`ErpFinPostingProcessor#alreadyPosted`（isReversed 不视为幂等命中）、`AcctSchemaResolver#resolvePrimarySchemaId`（null 语义）、`ErpPurReceiveProcessor#collectProjectMaterialCost`（行级 sourceBillCode 实证）、finance orm `ErpFinExpenseClaimLine.projectId`（多项目报销行载体实证）。
> 方法：Skill: `code-quality-audit-prompt` + `behavioral-failure-mode-scan-prompt`（B1/B2/B3.1/B3.2/B4）。核心链逐文件深读（工时 submit→approve→cancel→红冲→归集全链、结算 create→approve→过账→转固→cancel/reverse→质保金返还全链、三聚合器 + 预算检查器 + 成本率解析器全读、PnL 计算器 + job 全读）+ 平台与跨域源码实证（post 幂等返回值、alreadyPosted isReversed 语义、引擎 amountSource/amountFunctional fallback、ORM versionProp/UK/精度逐实体核对）+ arm-index 复用裁决。

## 发现清单

> 状态初始 `open`；修复阶段逐项测试验证后回填 `ai-check-index.md`。

### P1-CK-prj-001（D8）工时 cancel 只红冲 GL 凭证，不回退成本归集——归集行/头 totalAmount/project.actualCost 永久残留，撤回改时重提后归集金额陈旧

- **控制点**：`app/erp/prj/service/processor/ErpPrjTimesheetCancelProcessor.java#cancel`（L35-44：APPROVED+posted 时 `postingDispatcher.reverse(timesheet)` + 清 posted 契约 + 置 UNSUBMITTED——**零归集回退步骤**）对照正向写入点 `app/erp/prj/service/cost/ProjectCostAggregator.java#aggregateFromTimesheet`（L49-97：existsLine 去重写入 LABOR 归集行 + `existingHead.setTotalAmount(...+amount)` L70 + `project.setActualCost(...+amount)` L94）——grep 全 service `setActualCost`/`deleteEntity`/`batchDelete`：**无任何 subtract/删除路径**（actualCost 四个 writer 全为增量或全量重算 refreshActualCost，而 refreshActualCost 重算的输入正是未回退的归集行，无法自愈）。
- **证据**：approve（APPROVED）→ GL 凭证 + 归集行 + actualCost 增量；cancel（APPROVED+posted）→ GL 凭证红冲（净额归零）但 `erp_prj_cost_collection_line`（LABOR, sourceBillCode=工时单号）+ 头 totalAmount + `ErpPrjProject.actualCost` 全部残留。后果两分支：① 单据不再提交 → GL 已冲回而项目成本/PnL（`ProjectPnlCalculator.sumCostByCategory` 读归集行）仍含该笔——**业账两面净额分叉**；② 撤回修改 hours 后 resubmit→approve → `aggregateFromTimesheet` L49 `existsLine(TIMESHEET, code)` 命中旧归集行 → 幂等跳过 → **新 costAmount 永不更新**（归集行/actualCost 停留旧值，GL 凭证按新值）。owner doc `state-machine.md §适用对象四` 迁移表「cancel APPROVED+posted → 先红冲过账 + 清 posted 契约」只声明了凭证轴闭环，归集轴未声明——但「APPROVED 触发归集行回写」（L193）的对称回退是成本归集语义的必然要求（GL 冲回而成本账不冲回即失配）。
- **问题**：工时→成本→凭证反写闭环（D8 检查项）在 cancel 方向断裂。P1：用户可见正确性（项目成本虚高/陈旧）+ 业财一致性缺陷。
- **建议修复方向**：cancel（APPROVED 分支）镜像回退：删除/负冲该 sourceBillCode 归集行 + 头 totalAmount 与 project.actualCost 同额减回（或对 cancel 后 resubmit 的 approve 采取「删旧行写新行」而非纯幂等跳过）。
- **arm-index 裁决**：新增（grep「归集 回退 / timesheet cancel 归集 / 红冲 归集行」零命中；P1-RC-050（RC-R1.62）只闭合费用路径 requireReferenceable 门控，不涉归集回退维度）。

### P1-CK-prj-002（D6/D8）收入链路断裂——PnL 收入与结算收入依赖 `ErpPrjBilling.amountFunctional`，该字段全仓零 writer（默认 0），开票金额 totalAmount 永不进入收入计算

- **控制点**：`app/erp/prj/service/pnl/ProjectPnlCalculator.java#sumRevenue`（L144-160：`sum.add(nz(b.getAmountFunctional()))`）+ `app/erp/prj/service/processor/ErpPrjProjectSettlementProcessor.java#buildLines`（L186 `line.setAmount(nz(billing.getAmountFunctional()))`，INCOME 行）+ `ErpPrjProjectSettlementCreateSettlementProcessor` L45 `setFinalRevenue(nz(snapshot.getRevenueAmount()))`（= Σ amountFunctional）——对照 grep 全 `module-projects` `setAmountFunctional`：**仅 CostCollection 头三聚合器写入，Billing 零业务 writer**（仅 `_gen` setter 与 api bean）；ORM `app-erp-projects.orm.xml` L657-658：`amountSource`/`amountFunctional` `defaultValue="0"` 非必填，业务金额列为 `totalAmount`（L648 开票金额）。
- **证据**：Billing 是 CRUD 桩（`state-machine.md §适用对象三`：审批状态机属 successor，BizModel 18 行零业务钩子）——用户经 CRUD 建开票单填 `totalAmount`（开票金额），`amountFunctional` 停留默认 0。于是：PnL `revenueAmount=0`、`grossProfit=-totalCost`、毛利率 -100%；结算 `finalRevenue=0` → 凭证腿「借项目成本 finalCost / 贷项目收入 0 / 贷本年利润 finalCost」——收入侧整体失效，利润表把全部成本确认为亏损。除非操作员在 CRUD 页手工补填「本位币金额」（多币种四件套衍生字段，无联动提示）。
- **问题**：UC-PRJ-06「收入 = Σ Billing.amountFunctional」与 profitability.md 数据源设计依赖一个**后端零写入、默认 0** 的字段——`totalAmount ↔ amountFunctional` 无任何联动（对照 mfg/inventory 派生金额均有 builder 回写）。收入为 0 不是数据损坏但直接击穿盈利分析/结算两条主链的语义，P1。
- **建议修复方向**：Billing 保存钩子（defaultPrepareSave/Update）联动 `amountFunctional = totalAmount × exchangeRate`（单币种 rate=1 时等于 totalAmount），或 PnL/结算读侧回退 `amountFunctional != 0 ? amountFunctional : totalAmount`；修复时核对 AMIS 前端是否已有该联动（若有则降 P2——后端 API 路径仍无守卫的事实不变）。
- **arm-index 裁决**：新增（A1.36 RC 切片「UC-PRJ-06 接受 on ②③④⑤⑥聚合算术」审的是聚合公式本身（测试种子直接写 amountFunctional），未审字段写入面；grep「amountFunctional 零 writer」零命中）。

### P1-CK-prj-003（D8）费用归集幂等键用报销单号（头级）而非行级——跨项目报销单只有第一个刷新的项目被归集，其余项目行被 existsLine 静默吞掉

- **控制点**：`app/erp/prj/service/cost/ExpenseCostAggregator.java#refreshExpenseCost`（L95-97：`String sourceBillCode = claim.getCode(); if (existsLine(SOURCE_BILL_TYPE_EXPENSE, sourceBillCode)) continue;`——去重键为**报销单头 code**）对照 `#findLinesForProject`（L189-194：`eq("claimId", claimId) + eq("projectId", projectId)` 按**行级 projectId** 筛行；finance orm 实证 `ErpFinExpenseClaimLine.projectId` propId 19 存在）与 `#existsLine`（L196-202：全局 `(sourceBillType, sourceBillCode)` 查询，**不含 projectId 维度**）。
- **证据**：一张报销单含项目 A 行 500 元 + 项目 B 行 300 元（合法场景，报销行原生带 projectId 归集维度）：`refreshExpenseCost(A)` 归集 A 行（sourceBillCode=单号）；随后 `refreshExpenseCost(B)`（closeProject(B) 或手动触发）对同一单号 `existsLine` 命中 A 行 → **B 的 300 元被静默跳过，永不归集**——B 项目 actualCost/PnL 费用低估，且无日志无错误。对照物料路径无此问题（`ErpPurReceiveProcessor#collectProjectMaterialCost` L423 `sourceBillCode = receive.getCode() + "-" + line.getLineNo()` 行级键，已实证）。owner doc `cost-collection.md §4.1` 写「sourceBillCode=报销单号」——设计口径本身携带此缺陷，实现照抄。
- **问题**：多项目报销单的成本分摊静默丢失（P1：用户可见正确性，静默无观测）。同因次生：同一项目同一单多行时多个归集行共享同一 sourceBillCode（行表无 UK 不报错，但行级追溯粒度退化）。
- **建议修复方向**：sourceBillCode 细化为 `claim.code + "-" + line.getLineNo()`（对齐物料路径范式）；或 existsLine 增加 projectId/headId 维度。owner doc §4.1 口径同步修正。
- **arm-index 裁决**：新增（grep「报销 跨项目 归集 / sourceBillCode 单号」零命中；RC-R1.61/R1.62 审的是物料路径与状态门控）。

### P1-CK-prj-004（D5/D8）结算单无重复创建守卫——同项目可反复 createSettlement+approve，每张都按全量 finalRevenue/finalCost 过账，GL 收入/成本重复确认

- **控制点**：`app/erp/prj/service/processor/ErpPrjProjectSettlementCreateSettlementProcessor.java#createSettlement`（L26-67：仅校验 PnL 快照存在，**无「项目已有未取消结算单」守卫**，settlementType 也不校验项目状态/是否已结算）+ `ErpPrjProjectSettlementApproveProcessor#approve`（L25-36：每张单独立 `doPost` 全额过账）+ ORM 实证：`erp_prj_project_settlement` UK 仅 `(code, orgId)`（L854-855，code 毫秒时间戳生成永不碰撞），**无 projectId 维度约束**；grep 全 service 无「同项目已有 FINAL/CLOSE 结算」检查。
- **证据**：项目 P 完工，操作员调 `createSettlement(P, FINAL)` 两次（误操作或不同人）→ 两张 DRAFT 单各取最新 PnL（finalRevenue=R, finalCost=C）→ 各自 submit+approve → GL 出现**两张**「借项目成本 C / 贷项目收入 R」凭证（收入 2R、成本 2C 重复确认）；质保金腿同理双倍（借 1122/贷 2241 ×2）。INTERIM 语义上允许多次阶段结算，但 FINAL/CLOSE 无任何「已存在即拒绝/增量口径」设计——owner doc `profitability.md §关键流程 2` 只描述单次竣工结算流程，未裁决重复创建行为（设计缺口 + 实现零守卫）。
- **问题**：结算→GL 主链可被重复触发全额过账（P1：财务正确性 + 用户可见）。触发面为常规业务操作（无恶意前提）。
- **建议修复方向**：createSettlement 增加守卫——同项目存在 `docStatus != CANCELLED` 且 `settlementType ∈ {FINAL, CLOSE}` 的结算单时抛错（INTERIM 可放行但建议登记序号）；或 owner doc 显式裁决重复创建语义并配增量结算口径。
- **arm-index 裁决**：新增（grep「重复结算 / 多张结算 / settlement 幂等创建」零命中；P1-RC-052（RC-R1.63）质保金闭合与本维度正交）。

### P1-CK-prj-005（D8/D2）CLOSE 结算过账失败时资产卡片已建 IN_SERVICE，而 cancel 的资产回退被锁在 posted=true 分支内——卡片永久滞留在役（将进入折旧），且 reverseSettlement 被 posted 硬守卫封死

- **控制点**：`app/erp/prj/service/processor/ErpPrjProjectSettlementApproveProcessor.java#approve`（L28-34 编排顺序：`createAndActivateAsset`（L30，建卡 status=IN_SERVICE，见 facade L201-221）→ `doPost`（L32，`tryPost` 失败吞异常返回 false，仅置 posted 不置 true）→ `doApprove` 照常置 APPROVED/docStatus=APPROVED）+ `app/erp/prj/service/processor/ErpPrjProjectSettlementCancelProcessor.java#cancel`（L29-42：**`rollbackAssetIfNeeded` 位于 `if (Boolean.TRUE.equals(settlement.getPosted()))` 块内** L37——posted=false 时跳过）+ `ErpPrjProjectSettlementReverseSettlementProcessor` L26-31（posted=true 硬前置，无法经红冲路径回退）。
- **证据**：CLOSE+transferToAsset 单：approve → 资产卡已建 IN_SERVICE（assets 侧可折旧）→ 过账失败（如期间锁定/科目缺失，`ProjectSettlementPostingDispatcher#tryPost` L41-54 吞异常）→ 单据 APPROVED + posted=false。此后：cancel → posted=false → 跳过 reverse **也跳过 rollbackAssetIfNeeded** → docStatus=CANCELLED 但资产卡永久 IN_SERVICE；reverseSettlement → posted=false 守卫拒绝。唯一出路是手工改 assets 数据。转固链在「过账失败 + 取消」组合下资产卡片成为无凭证背书的孤儿在役资产（将自动折旧进 GL）。
- **问题**：approve 编排把「建卡」放在「过账」之前且失败不阻断终态（失败隔离设计），但 cancel 的回退对称性只覆盖了 posted=true 组合——失败组合（posted=false + assetCardId != null）的回退路径缺失。P1（数据一致性 + 财务影响）。
- **建议修复方向**：cancel 的资产回退条件改为 `assetCardId != null`（与 posted 解耦）；或 approve 编排调整为过账成功后才建卡（建卡移到 doPost 之后）。
- **arm-index 裁决**：新增（P1-RC-052/053、A2.13 均未覆盖「过账失败 + 转固回退」组合；grep「rollbackAssetIfNeeded / 转固 回退 posted」零命中）。

### P1-CK-prj-006（D7/D8，同型 P2-CK-inv-012/P1-CK-pur-002 族；P1-CK-fin-003 受影响面确认）工时/结算 approve 的 tryPost（REQUIRES_NEW）先于主事务提交——主事务失败留下孤儿凭证，且重试时 post() 幂等命中返回 null（fin-003）使 posted 永久 false、cancel 无法红冲，悬挂不可恢复

- **控制点**：`app/erp/prj/service/processor/ErpPrjTimesheetApproveProcessor.java#approve`（L49 `tryPost`（内部经 `ProjectPostingExecutor#postEvent` → `ErpFinVoucherBizModel#post`，`@Transactional(REQUIRES_NEW)` 独立提交）→ **其后仍有可失败步骤**：L50-59 实体重载+updateEntity（乐观锁可抛）、L63 `costAggregator.aggregateFromTimesheet`（可抛））+ 结算链同型（`ErpPrjProjectSettlementApproveProcessor#approve` L32 doPost → L33-34 doApprove+save）+ 跨域实证 `ErpFinPostingProcessor#process` L139-140（「幂等命中（源单已过账）返回 null」）+ `TimesheetPostingDispatcher#tryPost` L79-80 / `ProjectSettlementPostingDispatcher#tryPost` L44-45（`return voucherId != null`——**null 既表示幂等命中又表示未过账**）。
- **证据**：时序：tryPost 成功 → GL 凭证 + `ErpFinVoucherBillR` 回链已独立提交 → 主事务随后失败（聚合/乐观锁/预算）回滚 → 单据留在 SUBMITTED、posted 未置 true，但凭证已存在。用户重试 approve → `alreadyPosted` 命中（billR 已在）→ post 返回 **null** → tryPost 返回 false → 状态翻 APPROVED 但 **posted 永久 false**；此后 cancel 走 `if (posted)` 分支**不红冲**（`ErpPrjTimesheetCancelProcessor` L36）→ GL 凭证永久悬挂、单据永无法标记已过账。结算链同构（且 returnRetention 被 posted=true 守卫永久拒绝）。验证为正确的部分：**reverse 后重发不受影响**——`ErpFinPostingProcessor#alreadyPosted` L486 明确「已冲销凭证（isReversed=true）不视为幂等命中」，cancel 正常红冲后 resubmit 的重发会生成新凭证；本 finding 仅覆盖「凭证存在而源单 posted=false」的悬挂组合。
- **问题**：同 P2-CK-inv-012/P1-CK-pur-002/P2-CK-mfg-011 家族（REQUIRES_NEW 凭证先提交）——prj 站点登记；且因 fin-003 的 null 语义，恢复路径被封死，比家族内其他站点更严重（升级 P1 依据，与 mfg-011 的 P2 差异点：mfg 幂等键含毫秒可再生成新凭证，prj billHeadCode=工时单号/结算单号固定，幂等命中后**没有任何再过账通道**）。
- **建议修复方向**：与 fin-003 联合修复（post() 幂等命中返回已有 voucherId 或三态化）；短期缓解 = tryPost 后主事务步骤最小化（聚合移入 tryPost 前）+ posted=false 单据提供运维端显式补过账/红冲入口。
- **arm-index 裁决**：同型登记（P2-CK-inv-012/P1-CK-pur-002/P2-CK-mfg-011 族，prj 2 站点新增计数）；`P1-CK-fin-003` 受影响面确认注记见下表。

### P1-CK-prj-007（D6）看板毛利率聚合对多快照项目全量求和——每项目多行 PnL 全部相加，收入/成本/毛利随快照数线性放大

- **控制点**：`app/erp/prj/service/dashboard/ErpPrjDashboardBizModel.java#getProjectGrossMargin`（L175-192：`dao.findAllByQuery(q)` 后 `for (ErpPrjProjectPnl p : rows) { totalRevenue = totalRevenue.add(...); ... }`——**逐行累加全部 PnL 快照**，无 calcStatus/projectId 去重、无「每项目仅取最新快照」约束；`projectIds` Set 仅用于计数）对照 `ProjectPnlCalculator#findLatestCalculated`（L134-142：正确地按 `periodTo` DESC limit 1 取最新——结算取数通道无此问题）。
- **证据**：`refreshPnl` 以 `(projectId, periodFrom, periodTo)` 为幂等键（L78、L247-260），同参数重复刷新复用行；但**参数不同即新行**（见 P2-CK-prj-011：job 默认 `to=today()` 每日一行的放大源）。项目 P 有 5 个快照（每快照 revenue=R）→ 看板 `totalRevenue=5R`、`grossMarginPct` 分子分母同比放大后比率不变但绝对值（totalRevenue/totalCost/totalGrossProfit）全部失真；`projectCount` 恰好正确（Set 去重）掩盖问题。
- **问题**：UC-PRJ-10 KPI「实时聚合」在多快照存在时错误（P1：用户可见数值错误；前提条件 = 同项目 ≥2 快照，而默认 job 语义每天制造一个）。
- **建议修复方向**：聚合前按 projectId 取最新 CALCULATED 快照（SQL 窗口/两步查询），或聚合 distinct projectId + findLatestCalculated；与 P2-CK-prj-011 联合修复。
- **arm-index 裁决**：新增（A1.36「UC-PRJ-10 接受 on ①②③④⑤」审的是单快照聚合算术 + `TestErpPrjDashboardGrossMargin` 单快照用例；SP-5 存疑点登记过「Σprofit/Σrevenue 聚合口径」但未立案；grep「多快照 重复计数」零命中）。

### P2-CK-prj-008（D5/D3，同型 P1-CK-pur-003 族）全实体裸 CrudBizModel 无状态守卫——已过账工时/结算/归集头可经通用 update/delete 直接改删

- **控制点**：`app/erp/prj/service/entity/` 下 16 个 BizModel 中 11 个为 15-18 行裸 `CrudBizModel<T>`（Billing/±Line/Budget/±Line/Milestone/ActivityType/ProjectType/ProjectUser/Role/CostCollectionLine/SettlementLine，grep 零 `defaultPrepareSave/Update/Delete` 覆写）；`ErpPrjTimesheetBizModel`/`ErpPrjProjectSettlementBizModel`/`ErpPrjCostCollectionBizModel`/`ErpPrjProjectBizModel`/`ErpPrjTaskBizModel` 除命名 mutation 外同样不覆写通用 CRUD 钩子。
- **证据**：APPROVED+posted 工时可经 `ErpPrjTimesheet__update_` 直改 `hours/costAmount`（GL 凭证与归集行不同步）、可 `delete_`（凭证悬挂）；APPROVED+posted 结算单可 delete（凭证 + 资产卡 + 质保金凭证全悬挂）；归集头/行可手改（actualCost/PnL/预算检查全部失真基准）；CLOSED 项目可经 update 复活 status=OPEN 绕过 `requireReferenceable` 门控。
- **问题**：同 P1-CK-pur-003/P1-CK-sal-004/P1-CK-inv-005/P2-CK-mfg-006 全域同型——prj 站点登记（不复用展开）。prj 特有加重面：timesheet/settlement 带业财过账契约（posted + 凭证回链），CRUD 旁路直接制造 GL 悬挂。
- **建议修复方向**：posted=true / 终态实体 defaultPrepareUpdate/Delete 守卫；归集头行与 PnL 快照建议禁用通用 update。
- **arm-index 裁决**：同型登记（P1-CK-pur-003 族，prj 站点新增计数）。

### P2-CK-prj-009（D5/D10）入参边界缺失——工时 submit 不校验负 hours；createSettlement 不校验 settlementType 字典值且 project 缺失时 NPE

- **控制点**：`app/erp/prj/service/processor/ErpPrjTimesheetSubmitProcessor.java#submit`（L54-57：`BigDecimal hours = nz(timesheet.getHours());` 仅 null→0，**负数直接参与** `CostRateResolver.computeCostAmount`（`hours.multiply(costRate)`）→ 负 costAmount 入库（列 scale 4 可存）→ 归集负数冲减 actualCost + 预算检查 `BudgetChecker#check` L45 `addAmount.signum() <= 0` **静默跳过**——输错符号的工时静默冲减项目成本且不触发任何预算/错误提示）；`ErpPrjProjectSettlementCreateSettlementProcessor#createSettlement`（L41 `settlement.setSettlementType(settlementType)` 原样透传——**非字典值（如 "FOO"）不拒绝**，`transferToAsset=false` 后 approve 走 FINAL/INTERIM 腿全额过账；L27-38 `facade.loadProject(projectId)` 返回 null 无判空 → L38 `project.getOrgId()` **NPE**（非 NopException，D10））。
- **证据**：任务指令 D5 明列「负工时/超任务工时/非法结算类型」三项——超任务工时经查为 owner doc Non-Goal（task-dag.md §8「任务工时与状态联动归 cost-collection successor」）不计；负工时与非法结算类型确认在案。NPE 违反「业务异常必须 NopException」项目约定（虽是 RuntimeException 语义而非自定义异常类，仍属边界缺失）。
- **问题**：P2：边界处理缺失（负值静默生效 + 非法枚举透传 + 原始 NPE）。
- **建议修复方向**：submit 前置 `hours.signum() <= 0` 抛业务错误码；createSettlement 校验 `settlementType ∈ {FINAL, INTERIM, CLOSE}` + project null 时抛 `ERR_PROJECT_NOT_FOUND`。
- **arm-index 裁决**：新增（grep「负工时 / settlementType 校验」零命中）。

### P2-CK-prj-010（D2）结算过账失败无告警通道——posted=false 悬挂仅 LOG 可见（工时链有 prj.timesheet-posting-failure 告警，结算链缺失）

- **控制点**：`app/erp/prj/service/posting/ProjectSettlementPostingDispatcher.java#tryPost`（L41-54：catch 块仅 `LOG.warn/error` 后 return false——**无 `IErpSysNotificationBiz` 派发**，grep posting 包 notificationBiz 仅 TimesheetPostingDispatcher 命中）对照 `TimesheetPostingDispatcher#tryPost + dispatchFailureAlert`（L76-110：失败派发 `prj.timesheet-posting-failure` 告警，G3 分级，cost-collection.md §六 明示该告警为运营感知通道）。
- **证据**：结算 approve 后过账失败（期间锁定/科目未预置 1122/2241/1601/1603 等）→ APPROVED + posted=false，仅服务端日志；state-machine.md §适用对象五「过账失败 | ProjectSettlementPostingDispatcher 失败隔离（不阻塞终态）」与工时链同语义但告警通道不对称。结算单过账失败叠加 P1-CK-prj-006 的无重试入口 → 悬挂只能靠人工对账发现（比工时链更弱：工时至少有告警事件）。质保金返还路径已有显式抛错（`ERR_RETENTION_RETURN_POSTING_FAILED`）不在此列。
- **问题**：lesson 09（业财过账吞异常悬挂）家族的「无告警通道」维度——登记 P2（有 LOG 无告警，悬挂可被日志检索但不被运营感知）。
- **建议修复方向**：镜像 TimesheetPostingDispatcher 增加 `prj.settlement-posting-failure` 告警派发（notify 失败降级不阻断）。
- **arm-index 裁决**：新增（带同族注记）——P1-MA2-068（resolved R1.16）修复覆盖的是工时链告警；结算链控制点未被覆盖；grep「settlement posting failure 告警」零命中。

### P2-CK-prj-011（D6/D4）refreshPnl 默认期间 `to=today()` 使幂等键逐日漂移——job 每日运行/逐日手工刷新每项目新建一行快照，幂等「清旧重建」失效、表无界增长

- **控制点**：`app/erp/prj/service/pnl/ProjectPnlCalculator.java#refreshPnl`（L64-71：`from = periodFrom != null ? periodFrom : project.getStartDate(); to = periodTo != null ? periodTo : CoreMetrics.today();`）+ `#findExisting`（L247-260：`eq("periodFrom", from) + eq("periodTo", to)` 精确匹配为幂等键）+ job 调用链实证 `ErpPrjProjectPnlCalcHelper#recalculateOne` L77 `pnlBiz.refreshPnl(projectId, null, null, svcCtx)`——**每日传入的 to 随日变化**。
- **证据**：job 开启（双层门控均 true）后每日为每个活跃项目生成 `PNL-{projectId}-{millis}` 新行（昨日行不清理——javadoc L44「已存在快照若未过账则清旧重建」的语义仅在完全相同 (from,to) 时成立）。后果：① 表行数随项目数×天数无界增长（job 默认 cron 每日一次）；② 为 P1-CK-prj-007 看板重复计数提供每日放大源；③ `findLatestCalculated` 永远取到最新行（结算取数不受影响）。双门控默认 false（部署 opt-in），默认部署不触发——降 P2 依据。
- **问题**：幂等设计（同键清旧重建）与默认期间语义（滚动 today）组合失效。
- **建议修复方向**：job 路径传固定期间（如月初至今或项目起止全期间），或 findExisting 改为「同 projectId 取最新行更新其 periodTo」语义；与 007 联合修复。
- **arm-index 裁决**：新增（RC-R1.27（P1-RC-053）接线审的是调度可达性，未审期间键漂移；grep「pnl 快照 每日 新行」零命中）。

### P2-CK-prj-012（D8/D6）CostCollection 头 amountSource/amountFunctional 仅建头时一次性写入，后续聚合只累加 totalAmount——头本位币金额永久停留在首笔，结算 COST 行金额失真

- **控制点**：三聚合器同型——`ProjectCostAggregator#aggregateFromTimesheet`（新建头 L82-84 `setAmountSource(amount)/setAmountFunctional(amount)` vs 既有头 L70 仅 `setTotalAmount(...add(amount))`）、`MaterialCostAggregator#aggregateMaterial`（L73-75 vs L61）、`ExpenseCostAggregator#refreshExpenseCost`（L134-136 vs L122）。
- **证据**：项目首笔归集 100 元建头（amountFunctional=100）；此后工时/物料/费用累加至 totalAmount=10000 → 头 `amountFunctional=100` 永久陈旧。消费面：`ErpPrjProjectSettlementProcessor#buildLines` L196 COST 行 `line.setAmount(nz(cc.getAmountFunctional()))` → 结算成本行（备查明细）系统性低估；PnL 成本读**行**（`sumCostByCategory` L172-175）不受影响；dashboard incurredCost 读 totalAmount 不受影响。
- **问题**：冗余字段失同步（D8）——三处同型，影响结算明细行金额。
- **建议修复方向**：既有头分支同步累加 amountSource/amountFunctional（与 totalAmount 对称），或 buildLines 改读 totalAmount。
- **arm-index 裁决**：新增（grep「amountFunctional 头 停留 首笔」零命中）。

### P2-CK-prj-013（D6）结算 Provider 未按 RC-R1.64 双金额范式折算——fact() 不设 amountSource/amountFunctional，event.exchangeRate 透传但引擎 fallback 使汇率不作用于金额（当前潜伏，P2-RC-050 激活后显性）

- **控制点**：`app/erp/prj/service/posting/ProjectSettlementAcctDocProvider.java#fact`（L138-149：仅 `fact.setAmount(amount)`——**无 setAmountSource/setAmountFunctional**，对照同域 `ProjectCostCollectionProvider#fact` L101-114 的 RC-R1.64 双点范式：`functional = sourceAmount.multiply(rate)` 三字段齐设）+ 跨域实证 `ErpFinPostingProcessor` persistVoucher L853-857（`amtSource/amtFunctional` null 时 **fallback 到 amount**）+ `ProjectSettlementPostingDispatcher#buildEvent` L108（`setExchangeRate(settlement.getExchangeRate() != null ? ... : ONE)`——汇率确有透传，行上 `line.setExchangeRate(exchangeRate)` 也会落库）。
- **证据**：当前数据路径 PnL `exchangeRate` 恒 ONE（`ProjectPnlCalculator` L105，P2-RC-050 watch-only），结算复制快照汇率 → rate=1 时 fallback 无损，**行为正确**；但一旦 P2-RC-050 修复（快照携带真实汇率）或 CRUD 手填 settlement.exchangeRate≠1 → 凭证行记录了汇率却按源币金额入本位币账（借贷金额未乘汇率，试算平衡仍平但币种语义错）。
- **问题**：与 RC-R1.64 已确立的域内范式不对称的潜伏缺陷（D6）。同族注记：P2-CK-inv-010/P3-CK-mfg-021「汇率恒 1」家族的变体（此处汇率有传、折算缺失）。
- **建议修复方向**：结算 Provider fact() 镜像 ProjectCostCollectionProvider 的 `source × rate = functional` 三字段写法；与 P2-RC-050 联合修复时验证。
- **arm-index 裁决**：新增（带同族注记 P2-CK-inv-010；P2-RC-050 审的是 PnL 快照汇率，本条是凭证 fact 层，控制点不同）。

### P2-CK-prj-014（D1）rollbackAssetIfNeeded 经 daoFor(ErpAstAsset) 直接跨域写 assets 实体——绕过 IErpAstAssetBiz 与 assets 状态机守卫

- **控制点**：`app/erp/prj/service/processor/ErpPrjProjectSettlementProcessor.java#rollbackAssetIfNeeded`（L227-232：`IEntityDao<ErpAstAsset> dao = daoProvider.daoFor(ErpAstAsset.class); ... asset.setStatus(ASSET_STATUS_DRAFT); dao.updateEntity(asset);`——**跨域实体写**经 daoProvider 直改 status，无注释豁免理由）对照同文件 `createAndActivateAsset` L213（正向建卡正确走 `assetBiz.save(data, context)` 跨域 Facade）。
- **证据**：AGENTS.md「跨实体访问：始终为其他实体注入 I*Biz 接口」+ P1-MA4-008 豁免仅覆盖**读侧**子集；`data-dependency-matrix.md §3.2`「finance 对业务域是纯读——从不写业务表」的方向性约束同构适用于 prj→ast（写必须经 Facade）。直写 `status=DRAFT` 同时绕过 assets 域状态机 Bean 对 IN_SERVICE→DRAFT 合法性的判定（assets 域有自己的生命周期守卫），且若资产已跑折旧，简单置 DRAFT 与折旧记录不一致。
- **问题**：D1 平台反模式（跨域 daoFor 写）+ D3 旁路（绕过对端状态机）。P2。
- **建议修复方向**：assets 侧提供回退 Facade（如 `IErpAstAssetBiz.rollbackCapitalization`）或至少 status 迁移经 assets 状态机校验；matrix 同步登记 prj→ast 写边。
- **arm-index 裁决**：新增（P1-MA4-008 豁免范围核验不含 ast 写路径；grep「daoFor(ErpAstAsset)」arm-index 零命中）。

### P3-CK-prj-015（D9）聚合链 N+1 与全表加载——费用归集全表载入已审核报销单+逐单调行；预算/成本内存全量求和；既有头路径逐行 nextLineNo 全量加载

- **控制点**：`ExpenseCostAggregator#findApprovedClaims`（L162-177：`expenseClaimBiz.findList(q)` **无 projectId 过滤全表**载入所有已审核报销单——projectId 维度只在行级，导致逐单 `findLinesForProject` N+1（L92-93 循环内每单一查询））+ `BudgetChecker#sumUsedAmount`（L79-103：全头+全行实体载入内存求和，无 SQL 聚合；每次工时 submit/物料归集/费用刷新各触发一次）+ `ProjectCostAggregator#sumCollectedAmount`（L118-139 同型）+ `ExpenseCostAggregator#saveExpenseLine(existingHead 分支)`（L119-121 每行调 `nextLineNo`（L222-227）全量加载该头所有行——100 行归集 = 100 次增长中的全量查询；新头分支 L138-141 已正确用增量 lineNo，不对称）。
- **问题**：归集/预算检查在数据量增长后线性退化（P3 纯性能；结果正确）。
- **建议修复方向**：findApprovedClaims 下推（两步 join 或行表按 projectId 反查 claimId 集）；sum 用 QueryFieldBean 聚合（dashboard `sumByProject` 已有正确示范）；nextLineNo 改增量计数。
- **arm-index 裁决**：新增（同型 P3-CK-pur-011/sal-026/inv-020/mfg-014 家族注记）。

### P3-CK-prj-016（D7）归集头创建与幂等去重无 UK/锁支撑——并发聚合可产生多头与重复归集行

- **控制点**：三聚合器 `findHead`（如 `ProjectCostAggregator#findOpenHead` L149-157：`eq("projectId") + addOrderField("id", true) + setLimit(1)` 查后写，无锁）+ `existsLine`（L141-147 查后写）+ ORM 实证：`erp_prj_cost_collection_line` **无 (sourceBillType, sourceBillCode) UK**（全 ORM unique-key 清单核对，行表零 UK）；`erp_prj_cost_collection` UK 仅 (code, orgId)（code 含毫秒可碰撞窗口极窄）。
- **证据**：两个工时并发 approve 到同一首次归集项目 → 双方 findOpenHead 均 null → 各建一头（findHead 取最新 id 掩盖多头；refreshActualCost 全头求和使总额自洽，但「每项目单头」 javadoc 契约破坏）；并发同源 aggregateMaterialCost → existsLine 双 false → 双行重复归集（同 P2-CK-pur-006 家族的「查后写无共享行锁」模式）。@BizMutation 事务隔离（默认 READ_COMMITTED 级别下）不阻止此竞态；乐观锁 versionProp 只保护已有实体的并发 update，不覆盖 insert 竞态。
- **问题**：P3（触发面窄——同项目并发首归集/同单并发触发；后果多为多头冗余，重复行的金额失真面同 P1-CK-prj-003 族）。任务指令 D7「并发工时提交/结算与工时并发」检查项落点。
- **建议修复方向**：行表补 (sourceBillType, sourceBillCode) UK（ORM 变更走 dual-agent-approval）或聚合入口按 projectId 串行化（SELECT FOR UPDATE / 分布式锁）。
- **arm-index 裁决**：新增（带同族注记 P2-CK-pur-006；grep「cost_collection 并发 多头」零命中）。

### P3-CK-prj-017（D3/D6）PnL 收入口径与 javadoc 声明漂移——只排除 CANCELLED 不过滤审批态（Billing 审批轴 successor 残留）；PnL posted 列零 writer 使「已过账冻结重算」守卫为死守卫

- **控制点**：`ProjectPnlCalculator#sumRevenue`（L144-160：过滤仅 `ne("docStatus", CANCELLED)`）对照类 javadoc L36「收入：ErpPrjBilling 头 amountFunctional（**过滤审批通过**/未取消）」——`approveStatus` 维度零过滤；Billing 为 CRUD 桩（`state-machine.md §适用对象三`：approveStatus 五态零 writer 死状态）→ DRAFT/UNSUBMITTED 开票单全额计入收入。次生：`#refreshPnl` L79-82 `existing.getPosted()` 冻结守卫——grep 全仓 `ErpPrjProjectPnl` setPosted **零业务 writer**（PnL posted 列与 docStatus/approveStatus 同为无迁移轴字段）→ `ERR_PRJ_PNL_RECALC_FROZEN` 不可达（死守卫），javadoc「已过账抛错拒绝重算」语义空转。
- **问题**：与 P1-CK-prj-002 复合（收入既可能恒 0 又无审批口径）；死守卫属 dict/字段死状态家族（lesson 10 变体：守卫存在、状态不可达）。P3（Billing 审批轴已在 owner doc 声明 successor，口径缺口为已登记残留的传导）。
- **建议修复方向**：收入过滤补 `approveStatus=APPROVED`（待 Billing 审批轴 successor 落地时生效，先行加过滤会使当前全 DRAFT 单据收入=0——与 002 联合裁决）；javadoc 修正；posted 冻结语义随 PnL 过账 successor 一并落地或移除守卫。
- **arm-index 裁决**：新增（带复用注记——Billing 审批轴 successor 已由 P1-MA2-069（Deferred 裁决在位）覆盖，本条登记其向 PnL/结算收入的传导面 + PnL posted 死守卫新维度）。

### P3-CK-prj-018（D6/D10）PnL 窄期间成本按归集头 businessDate 归属而非行发生日——期间切片快照成本系统性错配

- **控制点**：`ProjectPnlCalculator#sumCostByCategory`（L163-194：期间过滤 `findCostHeads` L196-208 作用于**头** `businessDate`（= 头创建日，`CoreMetrics.today()`，三聚合器新建头时固定一次），行无业务日期列、工时行不透传 workDate）。
- **证据**：项目归集头 1 月创建（businessDate=1 月），3 月新 approve 的工时行挂在同一头下；查 3 月期间快照 `refreshPnl(P, 2026-03-01, 2026-03-31)` → 头 businessDate < from → 头及其全部行（含 3 月工时）被排除 → 3 月成本=0；反向查 1 月期间会把 1-3 月全部行算入。默认全期间快照（from=startDate）不受影响；窄期间快照经 `refreshPnl` API（periodFrom/periodTo 参数开放）可达。
- **问题**：P3（当前主路径默认全期间正确；期间维度语义错配为结构性——行无日期载体，修复需模型层加列或行级关联源单日期）。
- **建议修复方向**：短端 = 期间过滤仅用于收入（Billing 有 businessDate），成本恒全量（快照语义改为「截至 periodTo 累计」）；长端 = 归集行加业务日期列（ORM 变更 dual-agent）。
- **arm-index 裁决**：新增（grep「PnL 期间 头日期」零命中）。

### P3-CK-prj-019（D3/D9）延期预警消费终态 CANCELLED——已取消项目持续误报延期；看板全域 orgId 缺失复用 P2-RC-086 注记

- **控制点**：`ErpPrjDashboardBizModel#findDelayedProjectAlert`（L144：`q.addFilter(ne("status", PROJECT_STATUS_COMPLETED))`——**仅排除 COMPLETED**，CANCELLED 项目 endDate 过期仍进延期告警列表，pur/sal 教训「dashboard 消费死/终态」变体：此处是消费了不该消费的终态）；同文件 KPI/分布/超支/毛利率全部查询无 orgId 过滤 + `erp-prj.data-auth.xml` 空（`<objs/>`）对照 UC-PRJ-10 断言「看板数据受行级权限约束(只看自己组织)」。
- **问题**：误报噪音（P3）；orgId 维度——**复用 P2-RC-086**（全域看板直访路径 watch-only successor，purchase/inventory 报告同型处理，不重复登记），本条不展开。
- **建议修复方向**：过滤改 `notIn(status, [COMPLETED, CANCELLED])`。
- **arm-index 裁决**：CANCELLED 误报维度新增（grep「findDelayedProjectAlert / 延期 CANCELLED」零命中）；orgId 维度复用 P2-RC-086。

### P3-CK-prj-020（D2/D10，同型全域族）宽 catch 静默 + 错误码误用 + 审计字段兜底 "system"

- **控制点**：`ErpPrjTimesheetApproveProcessor#currentUserId`（L78-84 `catch (Exception e) { return null; }` 无日志——同型 P3-CK-md-008/pur-010/sal-024/inv-018/mfg-016 全域族，影响 approvedBy/postedBy 写 null）；`ExpenseCostAggregator#refreshExpenseCost` L76-79（project 不存在抛 `ERR_PROJECT_NOT_REFERENCEABLE`——错误码语义误用，应为 NOT_FOUND）；`ErpPrjProjectSettlementProcessor#requireSettlement` L240-244（not-found 抛 `ERR_SETTLEMENT_ILLEGAL_STATUS_TRANSITION` 同型误用）；`#resolveUserId` L274-276（context 无用户时返回 `"system"` 写入 approvedBy/postedBy 审计字段——审计语义弱化，且与 currentUserId 的 null 兜底两种口径不一致）。
- **问题**：P3 可观测性/语义质量（同型全域族 + prj 特有错误码误用两点）。
- **建议修复方向**：catch 补 LOG.warn；错误码归位；审计字段兜底口径统一（null 或 "system" 二选一并 javadoc 声明）。
- **arm-index 裁决**：同型登记（P3-CK-md-008/pur-010/sal-024/inv-018/mfg-016 全域族，prj 站点）+ 错误码误用新增维度并入本条。

### P3-CK-prj-021（D4）死常量与死方法——结算借贷科目 billData 键零消费、parseAmount 零调用

- **控制点**：`ErpPrjConstants.java` L124-125 `BILL_DATA_DEBIT_SUBJECT_CODE_SETTLEMENT`/`BILL_DATA_CREDIT_SUBJECT_CODE_SETTLEMENT`（grep 全 service 零消费——Provider 用硬编码科目 + accountKey 映射，不经 billData 键）+ `BILL_DATA_FINAL_PROFIT`（dispatcher L117 填入但 Provider createFacts 不读，自行重算 profitLoss——信息性冗余）+ `ErpPrjProjectSettlementProcessor#parseAmount` L318-327（grep 零调用方，死方法）。对照 job 接线（pnl-calc）**无死配置**——`erp-prj.pnl-calc-cron`/`pnl-auto-calc-enabled` 均有活跃消费点（job.yaml @cfg + helper 门控），D4 调度链完整（见验证为正确节）。
- **问题**：P3 代码卫生（死常量误导后续开发者以为科目可经 billData 配置）。
- **建议修复方向**：删除死常量/死方法或接线消费。
- **arm-index 裁决**：新增（同型 P3-CK-inv-021/sal-023/mfg-013 死配置家族注记——prj 的 cron 键无漂移，仅死常量维度）。

## 跨域关联影响面注记（不新建 finding）

| 已登记 finding | prj 受影响面 | 结论 |
| --- | --- | --- |
| `P1-CK-fin-003`（post() 幂等命中返回 null 与 dispatcher null=失败语义冲突） | `TimesheetPostingDispatcher#tryPost` L79-80 与 `ProjectSettlementPostingDispatcher#tryPost` L44-45 均为 `voucherId != null`——幂等命中时 posted 永不置 true；prj 的具体悬挂场景与放大后果已并入 **P1-CK-prj-006**（同型族登记），不重复单列 | **同型受影响面确认**：prj 2 dispatcher，随 fin-003 三态化修复联动核 |
| `P1-CK-fin-005`（AcctSchemaResolver null→post 静默成功零凭证） | 两 dispatcher `resolveAcctSchemaId`（Timesheet L190-192 / Settlement L132-134）调 `AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId)`——实测返回 null 当 orgId null 或组织无 ACTIVE 账套；null acctSchemaId 传入 event 后 finance 引擎按 fin-005 静默路径处理 | **同型受影响面确认**：随 fin-005 修复联动核；prj 侧可选加固 = 建账前置校验 |
| `P1-CK-pur-003`（CRUD update 无已审守卫全域同型） | prj 16 个实体 BizModel（11 裸桩 + 5 命名 mutation 无通用钩子守卫） | **同型登记为 P2-CK-prj-008** |
| `P2-CK-inv-012`/`P1-CK-pur-002`/`P2-CK-mfg-011`（REQUIRES_NEW 凭证先于主事务提交） | 工时 approve 与结算 approve 两条链 | **同型登记为 P1-CK-prj-006**（prj 因幂等键固定 + 无重试入口升级 P1） |
| `P2-RC-086`（全域看板 orgId 行级过滤缺失 watch-only） | prj dashboard 全部 @BizQuery 无 orgId + data-auth.xml 空 | **复用不展开**（purchase/inventory 报告同型处理），见 P3-CK-prj-019 注记 |

## 验证为正确（显式排除，防误报）

- **D1 机械扫描全零**：57 文件 `@Inject private`=0、`System.currentTimeMillis/LocalDateTime.now/new Date()`=0（全部 `CoreMetrics`）、`extends RuntimeException/Exception`=0（唯一 `IllegalArgumentException` 在 assertCan default 分支属编程错误断言，非业务异常）、字典字符串 `==` 比较=0（唯一 `== null` 判空合法）、无手编生成产物（`_service.beans.xml` 为生成导入）。
- **乐观锁在位**：projects ORM 全部 19 实体 `versionProp="version"`（逐实体实证）——工时并发 submit/approve、项目并发状态迁移由乐观锁检测冲突（D5 前提成立；insert 竞态另见 P3-CK-prj-016）。
- **CostRateResolver 五级解析链正确**（RC-R1.60 / P1-RC-048 复核）：单填 > 用户级（ProjectUser.costRate 按 projectId+userId）> 角色级（role 文本精确匹配 ErpPrjRole.code）> 活动类型 > 全局 config，逐 tier signum>=0 守卫 + 缺失跳过，五处皆无抛 `ERR_COST_RATE_NOT_AVAILABLE`——与 cost-collection.md §2.2 实现约定一致。
- **requireReferenceable 单一咽喉在位**（RC-R1.62 / P1-RC-050 复核）：`ErpPrjProjectBizModel#requireReferenceable`（仅 OPEN 通过）被费用路径（`ExpenseCostAggregator` L82）与物料路径（`ErpPrjCostCollectionAggregateMaterialCostProcessor` L40）消费；工时路径内联校验保留专项错误码——UC-PRJ-09 AC-① 闭合无回退。
- **质保金全链正确**（RC-R1.63 / P1-RC-052 复核）：FINAL 自动留存（ratio scale 4 HALF_UP，默认 0 opt-in）+ 主凭证留存腿/返还镜像腿（借 2241/贷 1122）+ `#RETURN` 后缀独立凭证幂等反查 + returnRetention 守卫链（APPROVED 双轴 + posted + retention>0 + 到期 + 未返还）+ 返还过账失败显式抛错（用户显式操作不吞异常）+ cancel/reverseSettlement 的未返还守卫——与 profitability.md 实现注记逐条一致。
- **pnl-calc 调度接线完整**（RC-R1.27 / P1-RC-053 复核）：`erp-prj-pnl-calc.job.yaml`（enabled `nop.job.*|false` opt-in + cronExpr `@cfg:erp-prj.pnl-calc-cron|0 0 1 * * ?`）→ `nopBatchTaskRunner` → `prj/pnl-calc.batch.xml`（orm-reader status in [DRAFT,OPEN,ON_HOLD] + processor `inject('erpPrjProjectPnlCalcHelper').recalculateOne`）→ helper 双层业务键门控（`pnl-auto-calc-enabled` 默认 false + cron 非空）+ REQUIRES_NEW 单条失败隔离（WARN 不阻断批次）+ `batchChunkCtx.serviceContext` null 兜底——无孤立 job、无键漂移（对照 mfg P3-CK-mfg-013 家族检查通过）。
- **工时过账汇率三态解析正确**（RC-R1.64 复核）：currencyId null/币种不存在/本位币 → rate=1；非本位币经 `ErpMdExchangeRate` from+to+`dateBetween(validFrom ≤ voucherDate ≤ validTo)` 边界匹配 + `addOrderField("validFrom", true)`（第二参 desc=true=最近生效优先，沿用 C1.2 校准的 API 语义）limit 1；本位币缺失/汇率未命中抛 `ERR_EXCHANGE_RATE_REQUIRED`——与 cost-collection.md 多币种注记一致；rateType 不过滤为文档化裁决。
- **buildEvent 校验错误传播语义正确**：`TimesheetPostingDispatcher#tryPost` L77 `buildEvent` 在 try 块外——借方科目缺失/汇率缺失等校验错误传播回滚 approve（单据保持 SUBMITTED 可补录后重试），与 RC-R1.64 汇率错误路径选项 α 一致，非缺陷。
- **reverse 后重发链路正确**：`ErpFinPostingProcessor#alreadyPosted` L486「已冲销凭证（isReversed=true）不视为幂等命中」——工时 cancel 红冲后 resubmit→approve 会生成**新**凭证并正确置 posted=true（P1-CK-prj-006 只覆盖 posted=false + 凭证存在的悬挂组合，正常红冲重发不受 fin-003 影响）。
- **物料归集行级幂等键正确**：purchase 侧 `ErpPurReceiveProcessor#collectProjectMaterialCost` L423 `sourceBillCode = receive.getCode() + "-" + line.getLineNo()`——行级键无跨单/跨项目碰撞（对照 expense 头级键缺陷 P1-CK-prj-003）；行级 projectId 解析 + null 跳过 + 负额跳过守卫在位。
- **预算三时机闭合在位**（RC-R1.62 / P1-RC-051 复核）：工时 submit（`runBudgetCheckHook`）+ 采购入库（AggregateMaterialCostProcessor L42）+ 报销（ExpenseCostAggregator L114）三路径写入前 BudgetChecker——WARNING warn 放行/STRICT 抛 `ERR_BUDGET_EXCEEDED`；`signum<=0` 跳过为负向不检查的正确语义（正向检查维度）。closeProject 前刷新的 STRICT 回滚后果已在 owner doc §3.3 声明。
- **任务 DAG 校验与状态机正确**：`TaskDependencyValidator.detectCycle`（自环优先 + visited revisit + maxDepth 兜底）+ 跨项目校验 + save/update 双钩子触发，与 task-dag.md §2/§3 算法逐条一致；`ErpPrjTaskStateMachine` 4 态 4 边矩阵 + blockReason 必填 + 前置 DONE 校验（config-gated STRICT/WARN）落地；`findSuccessors` 传递闭包语义与 owner doc §10 一致。
- **项目/工时/结算状态机 Bean 与 owner doc 一致**：项目 5 态 7 边（cancel 多源 DRAFT/OPEN/ON_HOLD 已文档化）；工时 REJECTED 死状态 + cancel 全放行为 §11.2 M4 文档化 intentional legacy；结算 APPROVED dict-value drift + OPEN/ON_HOLD/COMPLETED 共享死状态 + REJECTED 汇均为文档化 Deferred/Decision——不按 dict 死状态家族重复登记（MR1 R1.13-R1.20 先例）。
- **除零守卫在位**：`marginPct` revenue signum==0 返回 ZERO；dashboard `executionRate`/report `executionRate` budget<=0 分支处理；`grossMarginPct` divide 带 scale 4 HALF_UP。
- **计算精度匹配列定义**：submit `costAmount = hours × rate` 后 `setScale(4, HALF_UP)` 对齐列 scale 4（costAmount/costRate/amount 域均 scale 4）；`multiply` 本身精确无需中间舍入；质保金 ratio scale 4 HALF_UP。
- **beans 接线完整**：`app-service.beans.xml` 全部 5 状态机 Bean + 5 posting 组件 + 4 cost 组件 + PnL 引擎 + job helper + 18 per-mutation Processor 注册（逐 bean 核对无遗漏）；两张报表模板存在于 `/nop/main/report/prj/`。
- **报表路径防注入在位**：`resolveReportPath` 经 `StringHelper.isValidVPath` 校验 + 域隔离前缀 + renderType 白名单（html/xlsx/pdf）+ 临时资源 5 分钟延迟清理——镜像 mfg/hr 范式。
- **通知告警降级正确**（工时链）：`dispatchFailureAlert` notify 失败 catch LOG.warn 降级不阻断主流程（G3 分级）；`notificationBiz == null` 判空为防御性冗余非缺陷。
- **任务板/报表查询无 orgId 但有界**：`findBoardData` limit 200 封顶 + projectId 过滤（orgId 维度归 P2-RC-086 注记）；报表 loadTimesheets/loadProjects 条件可下推。

## arm-index 复用 or 新增裁决（汇总）

- 关键符号 `归集 回退`/`amountFunctional 零 writer`/`报销 跨项目 归集`/`重复结算`/`rollbackAssetIfNeeded posted`/`getProjectGrossMargin 多快照`/`pnl 每日 新行`/`负工时`/`settlementType 校验`/`findDelayedProjectAlert CANCELLED`/`cost_collection 并发 多头`/`daoFor(ErpAstAsset)` 在 `docs/audits/arm-index.md` **零命中 → 新增**。
- **复用（不重复登记，报告中注记）**：
  - dashboard orgId 行级过滤缺失 → **P2-RC-086**（全域 watch-only successor，含 prj）——不展开。
  - Billing 审批轴 successor（DRAFT 开票计入收入的前置成因）→ **P1-MA2-069**（resolved-via-deferral）——P3-CK-prj-017 只登记 PnL 传导面 + PnL posted 死守卫新维度。
  - 工时过账失败告警 → **P1-MA2-068**（resolved R1.16，工时链告警在位已验证）——P2-CK-prj-010 为结算链新控制点（新增带同族注记）。
  - PnL 多币种 exchangeRate=ONE → **P2-RC-050**（watch-only successor）——P2-CK-prj-013 为凭证 fact 层新控制点（带同族注记）。
  - CRUD 无守卫 → **P1-CK-pur-003 族**——P2-CK-prj-008 同型登记。
  - REQUIRES_NEW 凭证先提交 → **P2-CK-inv-012/P1-CK-pur-002/P2-CK-mfg-011 族**——P1-CK-prj-006 同型登记（prj 升级依据已注明）。
  - 汇率/双金额不对称 → **P2-CK-inv-010 族**——P2-CK-prj-013 同族站点。
  - currentUserId 宽 catch 无日志 → **P3-CK-md-008/pur-010/sal-024/inv-018/mfg-016 全域族**——P3-CK-prj-020 同型登记。
  - N+1/全表加载 → **P3-CK-pur-011/sal-026/inv-020/mfg-014 家族**——P3-CK-prj-015 同族站点。
  - 查后写无锁竞态 → **P2-CK-pur-006 家族**——P3-CK-prj-016 带同族注记。

## 统计

| 级别 | 数量 | 编号 |
| --- | --- | --- |
| P0 | 0 | — |
| P1 | 7 | P1-CK-prj-001..007 |
| P2 | 7 | P2-CK-prj-008..014 |
| P3 | 7 | P3-CK-prj-015..021 |

按主维度：D8×5（001/003/004/005/012）、D6×5（002/007/011/013/018）、D5×2（008/009，009 跨 D10 计 D5）、D7×2（006/016）、D2×2（010/020）、D3×2（017/019，019 跨 D9 计 D3）、D1×1（014）、D9×1（015）、D4×1（021）。（精确主维度归属：001 D8、002 D6、003 D8、004 D8、005 D8、006 D7、007 D6、008 D5、009 D5、010 D2、011 D6、012 D8、013 D6、014 D1、015 D9、016 D7、017 D3、018 D6、019 D3、020 D2、021 D4。）

跨域关联注记 5 项（fin-003 两 dispatcher 受影响面确认 / fin-005 两 dispatcher 受影响面确认 / pur-003 同型登记 008 / REQUIRES_NEW 族同型登记 006 / P2-RC-086 复用不展开）。

## 剩余风险（查了什么/没查什么）

- **已查**：57 个手写文件中 52 个逐行深读（三聚合器/预算检查器/成本率解析器全读、工时 submit→approve→cancel 与 posting dispatcher/provider 全读、结算 7 processor + facade 全读、PnL 计算器 + job helper + batch.xml/job.yaml 全读、5 状态机 Bean 全读、dashboard/report 全读、TaskDependencyValidator 全读；11 个 15-18 行裸 CRUD 桩 BizModel 与 4 个纯委托 facade BizModel 抽样确认结构）。平台与跨域源码实证 6 处（`ErpFinVoucherBizModel#post/reverse` REQUIRES_NEW、`ErpFinPostingProcessor#process` 幂等返回 null L139-140、`alreadyPosted` isReversed 语义 L486、persistVoucher amountSource/amountFunctional fallback L853-857、`AcctSchemaResolver` null 语义、`ErpPurReceiveProcessor#collectProjectMaterialCost` 行级键）。ORM 核对（全实体 versionProp、19 处 UK 清单、行表零 UK、amount/costAmount/costRate/hours 域精度、Billing amountFunctional defaultValue=0、settlement 全列）。arm-index 全分区 grep 复用裁决（projects 相关 18 处 PRJ finding 复核 + 4 项复用裁定）。
- **未深查**：`erp-prj-web` AMIS view.xml 契约 drift（归 C8.2）；`erp-prj-api`/meta 生成物与 xmeta 前端侧守卫（P2-CK-prj-008 的证伪路径之一 = xmeta updatability；P1-CK-prj-002 的缓解路径之一 = 前端 amountFunctional 联动公式，均需前端层核实）；`ErpPrjMilestone.billingAmount` 是否有消费方（profitability.md 数据源提及「可选」，grep 本域零消费，跨域未查）；assets 侧 `IErpAstAssetBiz.save(data map)` 契约鲁棒性（SP-4 存疑点，归 assets 域报告）；`ErpPrjBilling` 与 finance AR/开票的集成（Billing 是否从 sales/finance 回写——本域零证据，若存在跨域回写则 P1-CK-prj-002 触发面收窄）；测试代码正确性（31 个测试文件仅用于行为语义交叉引用）；closeProject 不自动创建结算单 vs profitability.md「项目 status→COMPLETED 时生成结算」的设计歧义（实现为手动 createSettlement，登记为疑似设计分歧，修复阶段与 owner doc 一并裁决，未立案）。
- **最不确定、建议主 agent 复核**：
  1. **P1-CK-prj-002**（收入恒 0）——若 AMIS 前端或 seed 数据（`app-erp-test-data`）在创建 Billing 时联动填 amountFunctional，则触发面收窄为「API/后端路径创建的开票单」，级别或降 P2；但后端零 writer 的事实不变。建议用一条仅填 totalAmount 的开票单走 refreshPnl 集成测试实证 revenueAmount==0。
  2. **P1-CK-prj-006**（REQUIRES_NEW 孤儿凭证 + fin-003 posted 永久悬挂）——触发窗口为主事务在 tryPost 之后失败（乐观锁冲突/聚合异常），频率低但恢复通道确实被封死；severity 取决于 fin-003 的三态化修复时序（若 fin-003 先修，本条降为家族 P2 口径）。
  3. **P1-CK-prj-004**（重复结算）——若产品语义允许多次 FINAL 结算（如分次竣工结算），则需改为「增量口径」而非「拒绝重复」；owner doc 未裁决，修复方向需与 PM 确认。
  4. **P2-CK-prj-013**（结算汇率不折算潜伏）——与 P2-RC-050（watch-only successor）联动；若 successor 明确结算永远单币种（rate 恒 1），可降 P3。
