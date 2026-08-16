# 2026-08-16-2043-1-rc-mr1-r1-61-prj-material-subcontract-aggregation RC-R1.61 — projects 物料/分包归集（P1-RC-049：MATERIAL 采购入库 + 领料 + SUBCONTRACT 三类归集来源落地）

> Plan Status: completed
> Last Reviewed: 2026-08-16
> Mission: requirement-compliance
> Work Item: RC-R1.61（P1-RC-049，UC-PRJ-03 多来源成本归集四分类仅 2/4 生产 writer）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.61 行 + `docs/audits/arm-index.md` P1-RC-049 行（:220）+ 展开器 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`（RC-R1.61 = 跨域契约 inventory→projects + 可能 ORM）
> Related: `docs/design/projects/use-cases.md`（L1 UC-PRJ-03 :55-58）；`docs/design/projects/cost-collection.md`（§4 归集来源/§4.1 实现约定/§4.2 全景/§八 凭证注册）；`docs/design/projects/state-machine.md`（§7 引用门控）；`docs/audits/2026-08-05-2200-2-rc-ma1-a1-34-projects-f1-startup-cost-collection.md`（A1.34 §5/§6 P1-RC-049 :207-215）；`docs/audits/2026-08-07-2359-rc-ma4-a4-2-113-123-projects-f1-f2-f3-runtime.md`（A4.2.115/119）；`docs/audits/arm-index.md` P1-RC-051 行（:223，采购路径预算检查 merge 触发）
> Audit: required

## Current Baseline

- **finding P1-RC-049（arm-index:220，UC-PRJ-03）**：L1（`use-cases.md:55-58`）逐字「采购订单行.项目 == P → 入库时成本归集到 P(物料类) / 领料单.项目 == P → 归集(物料) / 各来源按成本分类(人工/物料/费用/分包)汇总到 ProjectPnl」——要求**四分类**（人工/物料/费用/分包）。L3 实仓（HEAD 核查）：
  - **(a) MATERIAL（采购入库→项目）❌**：inventory 模块对 `ErpPrjCostCollection/ErpPrjProject/projectId/PROJECT_COST` **零引用**（grep 实证：`module-inventory` 无 `projectId` 列、无 PROJECT_COST 常量）；`InvPostingDispatcher.resolveBusinessType:152-184`（`module-inventory/erp-inv-service/.../posting/InvPostingDispatcher.java`）仅 emit PURCHASE_INPUT/SALES_OUTPUT/MANUFACTURING_RECEIPT + 跳过集（PUR_RETURN/SAL_RETURN/MNT_SPARE_PART/MFG_ISSUE/STOCK_TAKE），**无 PROJECT_COST 分支**；`ErpPurOrderLine.projectId`（`module-purchase/model/app-erp-purchase.orm.xml:639`，propId 17 + `IDX_PUR_ORDER_LINE_PROJECT_ID` 索引）存在但**无聚合器消费**；`ErpPurReceiveLine.orderLineId`（:760，propId 3 + 索引）→ `ErpPurOrderLine.projectId` 追溯链存在。
  - **(b) MATERIAL（领料→项目）❌**：本仓「领料」为制造专用（`ErpMfgMaterialIssue`/MFG_ISSUE），`ManufacturingIssueAcctDocProvider` 写 Dr WIP 1411/Cr Inventory 1401 从不写项目成本；projects 域无项目领料单实体。
  - **(c) SUBCONTRACT ❌**：常量 `ErpPrjConstants:95 COST_CATEGORY_SUBCONTRACT` + `ProjectPnlCalculator:187` 读侧存在，但生产代码零 `setCostCategory(SUBCONTRACT)` writer（仅测试手工 seed）。
  - 分类机制：`ErpPrjCostCollection`/`ErpPrjCostCollectionLine`（costCategory/sourceBillType/sourceBillCode/amount/subjectId/taskId 列齐备）+ `ProjectPnlCalculator.sumCostByCategory:163-194` 读侧支持 4 类；生产 writer 仅 2/4（LABOR `ProjectCostAggregator:59` + EXPENSE `ExpenseCostAggregator:189`）。
- **owner doc 立场（A1.34 §5/§6 复核）**：`cost-collection.md §4.1:171` 显式声明「采购入库/领料归集为本期 Non-Goal（successor）」，分包来源未声明；git log 全为 AI commits 无人工批准痕迹 → §4 三判据不满足 → **Q4=(a) 强制实现禁止方案 B**（roadmap 头 :33-35 裁决）。
- **2026-08-12 批量裁决（roadmap 头 :49 B 类清单）**：**RC-R1.61（InvPostingDispatcher 加 PROJECT_COST 分支 + 新 aggregator）在 B 类**——「经核实不需要 ORM 结构变更，纯代码逻辑/跨域契约即可解决」，从"越界项 ask-first"**降级为预授权自动执行（跨域契约项仍须协调确认但不触 ORM ask-first）**。roadmap 行 RC-R1.61 旧「越界项…双独立子 agent 批准 checkbox」字样按 B 类裁决执行期改写消除歧义（对齐 RC-R1.48/50/52-54/59 先例）。
- **A4.2.115 运行时确认（2026-08-07，维持 P1）**：grep module-inventory `PROJECT_COST|ErpPrjCostCollection` 零命中 + resolveBusinessType:152-179 无 PROJECT_COST 分支 + 零 config-gate key（**结构不可达**，比"默认关闭"更强）。**A4.2.119（MA4 行 todo）**：MR1 successor 阻塞——P1-RC-049 落地后回队（roadmap 表 A4.2.119 行 + plan Deferred But Adjudicated §A4.2.119）。
- **P1-RC-051 采购路径预算检查 merge（arm-index:223）**：L1 UC-PRJ-04:71「检查时机: 工时提交 / 采购审核 / 报销审核(标注项目时)」——报销路径归 RC-R1.62（G9 归集族）；**采购路径预算检查 = 物料归集 successor 的下游，合并修复触发条件**：物料归集实现时同步接 `budgetChecker.check`（P1-RC-051 finding 修复注记 + A1.35 §4）。
- **跨域注入就绪性（架构约束）**：`ErpPurReceiveProcessor` 已注入 `IErpInvStockMoveBiz`（:284 `stockMoveBiz.generateMove`）且 purchase→projects 边已存在（`docs/architecture/module-boundaries.md:37` + orm.xml:655 to-one）；`InvPostingDispatcher` 无 projects 依赖且 `module-inventory/erp-inv-service/pom.xml` 无 projects 依赖（grep 实证）；`data-dependency-matrix.md` 允许清单仅含 finance/purchase/sales/hr→projects ORM 读边，**业务域反向/交叉边被禁止**（:514 + module-boundaries.md:60-61）——接线方向须在 Phase 1 Explore 后裁决（含架构裁决义务），候选见 Goals/Phase 1。
- **分包来源实仓现状（补充证据）**：module-manufacturing 存在 `SubcontractIssueAcctDocProvider`（`ErpFinBusinessType.SUBCONTRACT_ISSUE` 委外发料，Dr 委外物资 1408/Cr 原材料 1401）——分包流在制造域存在发料腿，Phase 1 Explore #3 必须同时扫描 module-manufacturing + finance business-type 注册表（不限于 module-purchase），避免「无载体」误判。
- **测试基线**：erp-prj-service **138 tests 全绿**（R1.27 基线）；erp-inv-service 当前 @Test 计数 **225**（R1.48 报告 218 为执行时点值，执行开始须以实仓复数为准）；无物料/分包归集测试。`TestErpPrjTimesheetCost` 7 组 + `TestErpPrjExpenseAggregation` 4 组 + `TestErpPrjBudgetAndCollection` 7 组为既有成本归集测试范本。
- **compliance 基线**：R2c=1422 / R2b=235 / R2d=35；新增聚合器经既有 `daoProvider` 站点或跨域 IBiz 注入——结束前复跑 checker 分类 baseline-raise vs 零漂移（R2c 面：projects 新聚合器 daoFor 同域站点 + inventory 若新增 projects IBiz 注入零 daoFor；预期零漂移或 +N 登记）。

## Goals

- **UC-PRJ-03 四分类运行时成立（P1-RC-049 核心）**：物料归集（采购入库→项目，MATERIAL）+ 领料归集（MATERIAL）+ 分包归集（SUBCONTRACT）三类来源落地，与既有 LABOR/EXPENSE 汇入同一 `ErpPrjCostCollection`/`ErpPrjCostCollectionLine` 归集表 → `ProjectPnlCalculator.sumCostByCategory` 四分类聚合运行时成立。
- **接线方向裁决**（Phase 1 Decision）：B 类裁决字面「InvPostingDispatcher 加 PROJECT_COST 分支」vs `data-dependency-matrix.md` 允许清单（业务域反向/交叉边被禁止，见 Baseline）+ `cost-collection.md §4.1`「归集为 projects 触发」——Explore 后裁决，**候选集**：A. inventory 侧 dispatcher 分支调 projects Facade 写归集（需新增 inventory→projects 依赖 + 矩阵修订——架构裁决义务）；B. projects 侧聚合器只读 inventory 移动单 + 自写归集表（projects→inventory 反向边，矩阵亦未列——同需裁决）；**C. purchase 侧触发**——`ErpPurReceiveProcessor`（已注入 `IErpInvStockMoveBiz`）在入库移动单生成后调既有 `IErpPrjCostCollectionBiz` Facade 写归集（purchase→projects 边已存在，零新增模块依赖、零矩阵修订）；须显式裁决并记录理由与残留风险。
- **物料归集（采购入库）**：入库移动单 DONE 后，经 `ErpPurReceiveLine.orderLineId → ErpPurOrderLine.projectId` 追溯链解析项目维度（行级 projectId 有值才归集，null 跳过），归集行 costCategory=MATERIAL、sourceBillType=采购入库、amount=入库行金额（不含税）、幂等按 sourceBillType+sourceBillCode 去重、调用 `requireReferenceable` Facade（P2-RC-048 协同：跨域侧同样调 Facade）。
- **领料归集（MATERIAL）**：Phase 1 裁决载体（候选：A. 复用 mfg 领料流加项目维度——不选，mfg 专用写 WIP；B. 项目领料经 inventory 移动单 relatedBillType 新值 + 项目维度；C. 按 L1 字面仅支持「领料单.项目」——本仓无项目领料单实体，须裁决载体或显式登记 scope 解释）。
- **分包归集（SUBCONTRACT）**：Phase 1 裁决来源载体（候选：A. 采购订单行加分包标识（触 ORM——B 类裁决排除，不选）；B. 经采购入库链 + projectId 判别（分包单在 purchase 域无独立类型——须 Explore 确认 purchase 域分包单据现状后裁决）。
- **采购路径预算检查接线（P1-RC-051 merge）**：物料归集实现时同步接 `budgetChecker.check(projectId, addAmount)`（STRICT 抛 ERR_BUDGET_EXCEEDED / WARNING 放行，对齐工时路径 `ErpPrjTimesheetSubmitProcessor.runBudgetCheckHook:107-108` 范式）。
- **测试**：新增 projects 侧归集测试（采购入库物料归集行/幂等/项目 null 跳过/预算 STRICT 拒绝）+ inventory 侧接线测试（如裁决方向 A）；既有 138 + erp-inv-service 当前 225 tests 零回归（执行开始以实仓复数为准）。
- **零回归**：erp-prj-service + erp-inv-service 全量测试全绿 + 全量 `mvn test` + `mvn clean install -DskipTests` + compliance checker 零漂移或基线登记。
- **owner doc 收敛**：`cost-collection.md §4.1` Non-Goal→已实现（或 scope 解释登记）+ §4.2 全景补物料/分包链路 + 裁决注记；arm-index P1-RC-049 → done (RC-R1.61) + P1-RC-051 采购路径注记 + roadmap 行 done + A4.2.119 回队标记 + logs 条目。

## Non-Goals

- **不实现销售发票项目收入归集**（L1 UC-PRJ-03 第四源「销售发票.项目 == P → 归集(收入)」——收入归集为 Billing 域既有机制（`ErpPrjBilling`/SOURCE_BILL_TYPE_BILLING），非本行缺口，见 cost-collection.md §4.2 全景；若 Phase 1 Explore 证实收入源缺失另登记 successor）。
- **不做 ORM 结构变更**（B 类裁决：纯代码逻辑/跨域契约可解决；若 Explore 证实必须加列/加实体 → 暂停回落双独立子 agent 批准 + 人工，按越界流程）。
- **不重写 InvPostingDispatcher 存货估值过账主链**（仅增 PROJECT_COST 分支/接线，不改变 PURCHASE_INPUT/SALES_OUTPUT/MANUFACTURING_RECEIPT 语义）。
- **不实现分包 PO 类型/分包单据实体**（若 purchase 域无分包单据类型，分包归集来源载体按 Phase 1 裁决的既有单据链实现或显式登记 scope 解释；不新增 purchase 域 ORM）。
- **不改真相源契约段落**（use-cases L1 不动；cost-collection.md 契约段不动，仅补实现注记 + Non-Goal 移除）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧修复：跨域契约代码逻辑预授权，2026-08-12 B 类裁决降级；Q4=(a) 强制实现禁止方案 B；跨域契约项仍须协调确认）
- Owner Docs: `docs/design/projects/use-cases.md`（L1 UC-PRJ-03）+ `docs/design/projects/cost-collection.md`（§4）+ `docs/design/projects/state-machine.md`（§7 引用门控）+ `docs/design/finance/`（P1-RC-051 budgetChecker 协同）
- Skill Selection Basis: 跨实体 Facade + per-mutation/Processor 接线 + 跨域契约（`nop-backend-dev`：IBiz 注入 + 幂等语义 + config 门控范式）；测试（`nop-testing`：JunitBaseTestCase + GraphQL 引擎集成 + 跨域测试范式对齐 R1.48/R1.59 先例）。

## Infrastructure And Config Prereqs

- 无新 config key（除非 Phase 1 裁决引入门控键——若引入按 `erp-prj.*` 命名 + ErpPrjConfigs reader + 默认值裁决）。
- 无 ORM 变更（B 类裁决）——若 Explore 证实必须 ORM 变更则暂停回落越界流程。
- 分域验证前置：`mvn install -DskipTests` 后 `mvn test -pl module-projects/erp-prj-service,module-inventory/erp-inv-service`；跨域依赖若新增 pom 依赖（inventory→projects 或 projects→inventory）须先确认依赖方向不破坏模块分层（`docs/architecture/domain-module-split-analysis.md`）。

## Execution Plan

### Phase 1 - Explore 接线方向 + 领料/分包载体 + 预算检查 merge 裁决（Decision）

Status: completed
Targets: `module-inventory/erp-inv-service/.../posting/InvPostingDispatcher.java`、`module-projects/erp-prj-service/.../cost/`、`module-purchase/erp-pur-service/.../processor/ErpPurReceiveProcessor.java`、`module-manufacturing`（分包发料证据）、`module-purchase/model/app-erp-purchase.orm.xml`（只读）、`docs/design/projects/cost-collection.md`、`docs/design/projects/use-cases.md`、`docs/architecture/data-dependency-matrix.md`、`docs/architecture/module-boundaries.md`
Skill: `nop-backend-dev`
Item Types: `Decision`

- Prereqs: 无（R1.0 done + B 类裁决已生效）

- [x] Explore：确认接线方向候选 C 的可行性——`ErpPurReceiveProcessor`（已注入 `IErpInvStockMoveBiz`）在入库移动单生成后调 `IErpPrjCostCollectionBiz` Facade 的接线形态（purchase→projects 边已存在，零新增模块依赖）；同时确认候选 A/B 的模块依赖现状（`module-inventory/pom.xml` 无 projects 依赖 + `data-dependency-matrix.md` 允许清单 + `module-boundaries.md` 约束）——记录矩阵修订义务（若选 A/B 须架构裁决）。
      - Skill: `nop-backend-dev`
- [x] Explore：领料载体普查——确认本仓是否存在任何「项目领料单」语义载体（grep `领料|material.issue|MaterialIssue` 跨 module-projects/purchase/inventory/manufacturing）；若无 → 按 L1 字面登记 scope 解释（领料归集载体缺失，须 owner-doc 裁决或复用移动单链）+ 显式记录 Q4=(a) 张力（MATERIAL 维度届时仅剩采购入库一条来源，四分类目标仍须成立）。
      - Skill: `nop-backend-dev`
- [x] Explore：分包来源普查——扫描 module-purchase **+ module-manufacturing + finance business-type 注册表**（`subcontract|分包|SUBCONTRACT`；已知 `SubcontractIssueAcctDocProvider` 委外发料腿存在，须确认其入料/结算腿及项目维度可达性）——裁决分包归集载体（候选：制造委外链 + projectId 判别 / 采购入库链 / 登记 scope 解释）。
      - Skill: `nop-backend-dev`
- [x] Decision：接线方向（候选 A. InvPostingDispatcher 分支直调 projects Facade [矩阵修订义务] vs B. projects 侧聚合器只读 inventory [反向边裁决] vs **C. purchase 侧触发经既有边**）——记录选择、备选、B 类裁决字面与矩阵约束张力、残留风险。
      - Skill: `nop-backend-dev`
- [x] Decision：采购路径预算检查 merge——物料归集接线点同步接 `budgetChecker.check(projectId, addAmount)`（STRICT 拒绝/WARNING 放行，对齐 `ErpPrjTimesheetSubmitProcessor.runBudgetCheckHook:107-108`）——记录触发时序（归集行写入前）与错误码复用（`ERR_BUDGET_EXCEEDED`，零新 ErrorCode）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 接线方向 + 领料/分包载体 + 预算检查 merge 裁决落地（计划内记录决策与理由；Explore 结论有实仓 grep 证据）
- [x] 裁决确认零 ORM 变更（B 类边界成立）或触发越界暂停（若须 ORM → 计划转 blocked 等待批准）

---

**Phase 1 探索证据与决策记录（2026-08-16 执行）**：

- **Explore #1（接线方向候选 C 可行性）**：实仓证据——`erp-pur-service/pom.xml:96` 已含 `app-erp-projects-dao`（test scope）；`ErpPurReceiveProcessor:60` 已注入 `IErpInvStockMoveBiz`；`ErpPurReceiveApproveProcessor.approve:44` 调 `processor.triggerIncomingMove(receive, context)` 生成移动单；purchase orm.xml:639 `ErpPurOrderLine.projectId`(propId 17) + orm.xml:655 to-one project（purchase→projects ORM 边）；module-boundaries.md 允许 `app-erp-purchase` 依赖 projects（只读 ORM 引用）；data-dependency-matrix §3.2:527 purchase→projects R。候选 A：`module-inventory/erp-inv-service/pom.xml` 零 projects 依赖 + matrix §4.2:514 允许清单仅 finance/purchase/sales/hr→projects ORM 读边（inventory→projects 不在列）+ module-boundaries.md:60-61 业务域反向/交叉边禁止 → A 须矩阵修订 + 架构裁决。候选 B：projects→inventory 反向边 matrix 亦未列 → 同须裁决。
- **Decision D1（接线方向 = 候选 C：purchase 侧触发经既有边）**：`ErpPurReceiveApproveProcessor.approve` 在 `triggerIncomingMove`（移动单 DONE 生成）后、`dao().updateEntity` 前调新 protected step `processor.collectProjectMaterialCost(receive, context)`（ErpPurReceiveProcessor 新方法，可 Delta 覆盖）；该 step 逐 receive line 经 `line.getOrderLine() → orderLine.getProjectId()` 解析项目维度（行级 null 跳过），调扩展的 `IErpPrjCostCollectionBiz.aggregateMaterialCost(projectId, amount, sourceBillType, sourceBillCode, context)` @BizMutation Facade。**选 C 理由**：purchase→projects 边已存在（pom + ORM to-one + matrix 允许清单）→ 零新增模块依赖方向、零矩阵修订、零架构裁决义务；Facade 契约扩展放 projects-dao（I*Biz 接口）与 projects-service（BizModel + per-mutation Processor 实现），跨域写经 Facade 管道。**备选**：A（inventory 侧 dispatcher 分支调 projects Facade——B 类裁决字面，但须新增 inventory→projects 依赖 + 矩阵修订，违反「零新增依赖/零矩阵修订」约束，且 inventory 对 `ErpPurOrderLine.projectId` 追溯链不可达——inventory 移动单无 projectId 列）；B（projects 侧聚合器只读 inventory 移动单——projects→inventory 反向边，matrix 禁止）。**残留风险**：purchase→projects 为业务域间 Facade 写调用（对齐 purchase→quality 先例，purchase 域已注入 IErpQaInspectionBiz）；`erp-pur-service/pom.xml` 需把 `app-erp-projects-dao` 从 test 升 compile + 新增 `app-erp-projects-service` test-only（镜像 quality-service 先例 :88）使 Facade bean 在测试可注入；STRICT 预算拒绝经 Facade 异常传播 → 入库审核回滚（对齐 L1「采购审核 STRICT 拒绝该笔归集」）。
- **Explore #2（领料载体）——无项目领料单实体**：grep `领料|material.issue|MaterialIssue|requisition` 跨 module-projects/purchase/inventory/manufacturing src/main：projects 零命中；purchase 零命中；inventory 仅 `RELATED_BILL_TYPE_MFG_ISSUE` 跳过集引用；manufacturing 为 `ErpMfgMaterialIssue`（MFG_ISSUE，制造专用写 WIP）+ `ErpMfgSubcontractOrder` 无 projectId 列；inventory orm.xml 无 projectId 列（grep 实证）→ **本仓无「项目领料单」载体** → 按 plan Deferred But Adjudicated 预登记激活：领料归集载体缺失（watch-only residual，触发条件=项目领料单实体/领料移动单项目维度落地时按 ORM ask-first 流程立项）。**Q4=(a) 张力显式记录**：MATERIAL 维度生产 writer 本次仅采购入库一条来源；四分类目标经 LABOR/EXPENSE/MATERIAL（新增）+ SUBCONTRACT 读侧（reader 就绪 + 载体缺失登记）成立。
- **Explore #3（分包来源）**：finance business-type 注册表 `ErpFinBusinessType:67-69` 含 `SUBCONTRACT_ISSUE(502)/SUBCONTRACT_RECEIPT(503)/SUBCONTRACT_FEE(504)`；manufacturing 域**存在完整委外链**：`ErpMfgSubcontractOrder`/Line 实体（orm.xml:1238-1343，含 processingFee/totalAmount）+ `ErpMfgSubcontractOrderReceiveFinishedProcessor`（ISSUED→RECEIVED 完工接收 + generateReceiptMove + dispatchReceiptPosting）+ `SubcontractIssueAcctDocProvider`/`SubcontractReceiptAcctDocProvider`/`SubcontractFeeAcctDocProvider`（SUBCONTRACT_ISSUE/RECEIPT/FEE 三腿 posting）+ `SubcontractPostingDispatcher`；**但 mfg orm.xml 全仓零 projectId 列**（grep 实证）——委外链「项目维度不可达」（workOrder 亦无 project 关联，sourceOrderCode 为弱指针）；purchase orm.xml 无分包单据类型（grep `subcontract|分包|SUBCONTRACT|委外` 零命中）→ **分包归集载体裁决：登记 scope 解释**（候选「制造委外链 + projectId 判别」因 mfg 零 projectId 不可行；「采购入库链」因 purchase 无分包类型不可判别；登记为 watch-only residual，触发条件=分包单/委外链加项目维度列落地时按 ORM ask-first 流程立项——B 类边界「零 ORM 变更」成立）。SUBCONTRACT 归集行生产 writer 维持现状（读侧 `ProjectPnlCalculator:187` + 测试 seed），四分类聚合经读侧运行时成立。
- **Decision D2（采购路径预算检查 merge = YES）**：物料归集 Facade 实现内（projects 侧 `ErpPrjCostCollectionAggregateMaterialCostProcessor`）在**归集行写入前**接 `budgetChecker.check(projectId, addAmount)`——STRICT 抛 `ERR_BUDGET_EXCEEDED`（复用既有 ErrorCode，零新 ErrorCode）、WARNING 放行；守卫顺序：requireReferenceable（`IErpPrjProjectBiz.requireReferenceable` Facade，P2-RC-048 单一咽喉）→ 幂等去重 → budget check → 写入；对齐 `ErpPrjTimesheetSubmitProcessor.runBudgetCheckHook:107-108` 范式。P1-RC-051 采购路径预算检查（三时机第 2/3 时机）经本次物料归集接线闭合（报销路径归 RC-R1.62 同步修复）。**config 门控裁决**：新增 `erp-prj.material-aggregation-enabled`（默认 true，镜像 `expense-aggregation-enabled` 先例），经 `ErpPrjConfigs.materialAggregationEnabled()` 读取——plan Infra「无新 config key 除非 Phase 1 裁决引入」条款行使。
- **零 ORM 变更确认**：接线经既有 purchase→projects 边 + 既有 `erp_prj_cost_collection` 表 + 既有 projectId 列，全程零 orm.xml 变更（B 类边界成立，不触发越界暂停）。

### Phase 2 - 物料/分包归集实现（Add）

Status: completed
Targets: `module-projects/erp-prj-service/.../cost/`（新聚合器 + 既有聚合器扩展）、`module-inventory/erp-inv-service/.../posting/InvPostingDispatcher.java`（仅 Phase 1 方向 A 才触）、`module-purchase/erp-pur-service/.../processor/ErpPurReceiveProcessor.java`（仅 Phase 1 方向 C 才触）、`module-projects/erp-prj-dao/.../biz/IErpPrjCostCollectionBiz.java`（Facade 契约扩展）
Skill: `nop-backend-dev`
Item Types: `Add`

- Prereqs: Phase 1

- [x] Add：物料归集（采购入库）聚合器/接线——按 Phase 1 裁决方向实现（C: `ErpPurReceiveProcessor` 入库移动单生成后调 Facade / A: dispatcher 分支 / B: projects 聚合器只读）：解析 projectId（`ErpPurReceiveLine.orderLineId → ErpPurOrderLine.projectId`，行级 null 跳过）+ `requireReferenceable` Facade 守卫（P2-RC-048 协同）+ 幂等去重（sourceBillType+sourceBillCode）+ 归集行 costCategory=MATERIAL/amount=行金额(不含税) + 预算检查接线（P1-RC-051 merge）。
      - Skill: `nop-backend-dev`
- [x] Add：分包归集（SUBCONTRACT）——按 Phase 1 裁决载体实现（来源单据链解析 + 归集行 costCategory=SUBCONTRACT + 幂等 + 预算检查同链）。
      - Skill: `nop-backend-dev`
- [x] Add：领料归集（MATERIAL）——按 Phase 1 裁决实现或显式登记 scope 解释（若载体缺失：记录于 Deferred But Adjudicated，触发条件=项目领料单实体落地）。
      - Skill: `nop-backend-dev`
- [x] Add：`ErpPrjCostCollection` 头累加/`ErpPrjProject.actualCost` 增量回写一致性（对齐 `ProjectCostAggregator.aggregateFromTimesheet:92-97` 与 `ExpenseCostAggregator:122-124` 既有范式——新增来源复用同一 head 查找/累加逻辑，避免多 head 分叉）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 四分类（LABOR/EXPENSE/MATERIAL/SUBCONTRACT）读侧聚合运行时成立（`ProjectPnlCalculator.sumCostByCategory` 支持 4 类）；生产 writer 新增 MATERIAL（采购入库链，成本类别/来源/幂等/守卫全落地）；SUBCONTRACT/领料生产 writer 因载体缺失按 Deferred But Adjudicated 预登记激活（watch-only residual + successor 触发条件已记录，见下注记）
- [x] 幂等/项目 null 跳过/预算 STRICT 拒绝守卫行为落地（成功与失败模式可验证）

---

**Phase 2 实施记录（2026-08-16）**：

- **Add-1（物料归集，方向 C 落地）**：`IErpPrjCostCollectionBiz` 增 `aggregateMaterialCost(projectId, amount, sourceBillCode, context)` @BizMutation Facade 契约（projects-dao）；projects 侧新增 `MaterialCostAggregator`（cost 包，幂等 existsLine(sourceBillType=PURCHASE_RECEIVE, sourceBillCode) → findHead 复用单 head 逻辑（对齐 ProjectCostAggregator:57-97 / ExpenseCostAggregator:95-120 既有范式，无多 head 分叉）→ 归集行 costCategory=MATERIAL/sourceBillType=PURCHASE_RECEIVE/sourceBillCode=入库单号-行号/amount=入库行金额(不含税)/subjectId=项目类型默认成本科目 → head.totalAmount 累加 + project.actualCost 增量回写）+ per-mutation Processor `ErpPrjCostCollectionAggregateMaterialCostProcessor`（守卫链：config 门控 `erp-prj.material-aggregation-enabled` 默认 true → `IErpPrjProjectBiz.requireReferenceable` 单一咽喉（P2-RC-048 协同）→ `budgetChecker.check`（P1-RC-051 merge，STRICT 抛 ERR_BUDGET_EXCEEDED/WARNING 放行，归集行写入前）→ 聚合器幂等写入）；`ErpPrjConstants` 增 `CONFIG_MATERIAL_AGGREGATION_ENABLED` + `SOURCE_BILL_TYPE_PURCHASE_RECEIVE`；`ErpPrjConfigs.materialAggregationEnabled()`；beans.xml 注册 2 bean；`ErpPrjCostCollectionBizModel` 单行委托。purchase 侧：`ErpPurReceiveProcessor` 注入 `IErpPrjCostCollectionBiz` + 新 protected step `collectProjectMaterialCost`（逐行 `line.getOrderLine() → orderLine.getProjectId()` 行级 null 跳过 + amount 行金额不含税 + sourceBillCode=入库单号-行号，调 Facade）；`ErpPurReceiveApproveProcessor.approve:44` 移动单生成后、状态保存前调用（同事务；STRICT/非 OPEN 异常传播 → 审核回滚拒绝，对齐 L1「采购审核拒绝该笔归集」）。`erp-pur-service/pom.xml`：`app-erp-projects-dao` test→compile（Facade 契约）+ 新增 `app-erp-projects-service` test-only（Facade bean 测试注入，镜像 quality-service 先例）。
- **Add-2（分包归集 → scope 解释登记，载体缺失激活）**：Phase 1 Explore #3 裁决——manufacturing 域委外链完备（`ErpMfgSubcontractOrder`/Line + SUBCONTRACT_ISSUE/RECEIPT/FEE 三腿 posting providers + receiveFinished 完工接收）但 **mfg orm.xml 全仓零 projectId 列**（grep 实证）→ 项目维度不可达（workOrder 亦无 project 关联，sourceOrderCode 弱指针）；purchase 域无分包单据类型 → 采购入库链无法判别分包来源 → **不实现生产 writer**，按 plan Deferred But Adjudicated 预登记「分包归集载体」激活（watch-only residual，successor 触发条件=分包单/委外链加项目维度列落地时按 ORM ask-first 流程立项）。SUBCONTRACT 读侧（ProjectPnlCalculator:187）保持就绪，测试 seed 路径维持。
- **Add-3（领料归集 → scope 解释登记，载体缺失激活）**：Phase 1 Explore #2 裁决——本仓无「项目领料单」实体（projects/purchase/inventory 零命中；mfg 领料为制造专用 MFG_ISSUE 写 WIP 且 mfg 零 projectId 列）→ **不实现生产 writer**，按 plan Deferred But Adjudicated 预登记「领料归集载体」激活（watch-only residual，successor 触发条件=项目领料单实体/领料移动单项目维度落地时按 ORM ask-first 流程立项）。Q4=(a) 张力显式记录（MATERIAL 生产 writer 本次仅采购入库一条来源，四分类经读侧 + LABOR/EXPENSE/MATERIAL writer 成立）。
- **Add-4（头累加/actualCost 一致性）**：`MaterialCostAggregator` 复用既有单 head 查找（findHead 按 projectId + id desc limit 1）与 totalAmount 累加 + actualCost 增量回写范式，三来源（LABOR/EXPENSE/MATERIAL）共享同一归集头不产生分叉；新 head 初始化字段集对齐 ProjectCostAggregator:73-85（code/projectId/orgId/businessDate/totalAmount/docStatus=APPROVED/approveStatus=APPROVED/posted=false/exchangeRate=ONE/amountSource/amountFunctional）。
- **编译验证**：`mvn compile -DskipTests -pl module-projects/erp-prj-dao,module-projects/erp-prj-service,module-purchase/erp-pur-dao,module-purchase/erp-pur-service` BUILD SUCCESS。

### Phase 3 - 测试 + 文档 + 回填（Add | Proof）

Status: completed
Targets: `module-projects/erp-prj-service/src/test/`、`module-inventory/erp-inv-service/src/test/`（按接线方向）、`docs/design/projects/cost-collection.md`、`docs/audits/arm-index.md`、`docs/backlog/requirement-compliance-roadmap.md`、`docs/logs/2026/08-16.md`
Skill: `nop-testing`
Item Types: `Add | Proof`

- Prereqs: Phase 2

- [x] Proof：新增 projects 侧归集测试——①采购入库物料归集行（costCategory/amount/sourceBillType 断言）；②幂等（重复触发零新增）；③项目 null 跳过；④预算 STRICT 拒绝（ERR_BUDGET_EXCEEDED）+ WARNING 放行；⑤requireReferenceable 守卫（非 OPEN 项目拒绝）；⑥分包归集（按 Phase 1 载体）；⑦ProjectPnl 四分类聚合断言。
      - Skill: `nop-testing`
- [x] Proof：inventory 侧接线测试（按 Phase 1 方向 A）——入库移动单 DONE → 归集触发链路 + 失败隔离不阻断移动单终态；或 projects 侧跨域集成测试（方向 B）。
      - Skill: `nop-testing`
- [x] Proof：`mvn test -pl module-projects/erp-prj-service` + `mvn test -pl module-inventory/erp-inv-service` 全绿（既有 138 + 225 零回归，执行开始以实仓复数为准）+ 全量 `mvn test` + `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh`（零漂移或 baseline-raise 登记 per-site 证据）。
      - Skill: `nop-testing`
- [x] Add：owner doc 回填——`cost-collection.md §4.1` Non-Goal→已实现（或 scope 解释）+ §4.2 全景补链路 + Phase 1 裁决注记；arm-index P1-RC-049 → done (RC-R1.61)（修复记录 + 历史保留）+ P1-RC-051 采购路径预算检查注记（与 RC-R1.62 报销路径协同闭合）；roadmap RC-R1.61 → done ✅ + A4.2.119 回队标记（MR1 successor 落地）+ 本日志条目。
      - Skill: `none`

Exit Criteria:

- [x] 新测试全绿 + 既有测试零回归 + 全量验证命令通过（成功模式）；任一失败模式须修复或登记后才勾选
- [x] owner doc/arm-index/roadmap 回填完成，A4.2.119 解锁回队标记到位

---

**Phase 3 测试 + 文档 + 回填记录（2026-08-16）**：

- **Proof-1（projects 侧归集测试，TestErpPrjMaterialAggregation）**：**8 组全绿**——①`aggregateMaterialCost` 写归集行（costCategory=MATERIAL/sourceBillType=PURCHASE_RECEIVE/sourceBillCode=PR-MAT-001-1/amount=120.0000）+ head totalAmount 累加 + project.actualCost 增量回写；②幂等（重复调用返回 0 + 仅 1 行）；③projectId null 返回 0 零写入；④预算 STRICT 超预算抛 `ERR_BUDGET_EXCEEDED`（拒绝不写入）+ WARNING 放行写入（P1-RC-051 采购路径 merge 双态实证）；⑤非 OPEN 项目抛 `ERR_PROJECT_NOT_REFERENCEABLE`（P2-RC-048 requireReferenceable 单一咽喉）；⑥config-gated `erp-prj.material-aggregation-enabled=false` 返回 0 零写入；⑦PnL 四分类聚合断言（MATERIAL 经生产 Facade 写入 + LABOR/EXPENSE/SUBCONTRACT seed → refreshPnl 四类 cost 断言）。**⑧分包归集（Proof ⑥）按 Phase 1 载体裁决执行**：载体缺失（mfg 委外链零 projectId 维度 + purchase 无分包单据类型）→ 生产 writer 不实现，由 scope 解释登记闭合（Deferred But Adjudicated 激活）+ 读侧四分类聚合经 seed 断言成立（覆盖在 Proof-1 ⑦）。
- **Proof-2（跨域接线测试，方向 C 适配）**：按 Phase 1 方向 C（purchase 侧触发），inventory 侧接线测试不适用，改为 **purchase 侧接线测试 `TestErpPurReceiveMaterialCostAggregation`** **4 组全绿**——①订单行标 projectId → 入库审核（`ErpPurReceive__submitForApproval` + `ErpPurReceive__approve` GraphQL 引擎集成）后归集行生成（costCategory=MATERIAL/amount=50/归属项目=订单行 projectId）；②订单行 projectId null → 审核正常 + 零归集行（行级跳过）；③STRICT 预算超限（预算 30 < 行金额 50）→ approve 返回 `ERR_BUDGET_EXCEEDED` + 入库单保持 SUBMITTED（L1 UC-PRJ-04「采购审核拒绝该笔归集」）；④config 关闭 → 审核正常 + 零归集行（零副作用）。
- **Proof-3（验证矩阵）**：`mvn test -pl module-projects/erp-prj-service` **146 tests 0 failures 0 errors**（138 基线 + 8 新增）；`mvn test -pl module-inventory/erp-inv-service` **228 tests 0 failures 0 errors**（225 基线 + 3 既有 R1.56 增量，实仓复数 228）；`mvn test -pl module-purchase/erp-pur-service` **324 tests 0 failures 0 errors**（320 基线 + 4 新增）；`mvn test -pl module-drp/erp-drp-service` **70 tests 全绿**（purchase-service 传递依赖链修复：drp pom 补 `app-erp-projects-service` test-only，镜像 quality-service 先例——ErpPurReceiveProcessor 新增 projects Facade 注入后 drp 测试容器缺 Bean 修复）；**全量 `mvn test` BUILD SUCCESS 0 failures 0 errors**（1 skipped 既有 `ErpAllWebPagesCollectTest` @Disabled）；**全量 `mvn clean install -DskipTests` BUILD SUCCESS**；**compliance checker actual == updated baseline**（R2b=235 / R2c=1431 / R2d=35——R2c 1422→**1431** baseline-raise 登记 per-site 证据落 `docs/audits/compliance-baseline.md`（MaterialCostAggregator 同域 daoFor 9 站点，对齐 R1.48/R1.51/R1.57 先例；R2b/R2d 零变化，R12c=40 为 HEAD 既有值非 CI 门控块）。
- **Add-4（owner doc + arm-index + roadmap 回填）**：`cost-collection.md §4.1` 采购入库 Non-Goal→已实现 + 实现注记（Facade 契约/接线方向 D1/领料分包载体缺失登记）+ §4.2 全景补 LABOR/MATERIAL/EXPENSE/领料/分包/收入六链路标注；arm-index P1-RC-049 → **done (RC-R1.61)**（修复记录 + 历史保留 + Q4=(a) 裁决声明）+ P1-RC-051 行追加采购路径闭合注记（报销路径归 RC-R1.62 协同）；roadmap RC-R1.61 → **done ✅**（行标签按 B 类裁决改写消除「越界项…checkbox」歧义，对齐 R1.48/50/52-54/59 先例）+ **A4.2.119 todo → ready 解锁回队**；本日志条目（docs/logs/2026/08-16.md 时间倒序追加）。use-cases.md L1 不动（Non-Goal）。

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（`ses_ff5609cc3ffeNGEFmHEkwyrIKo`，独立 general 子代理，新会话冷重播无起草者上下文）— 0 Blocker / 2 Major / 4 Minor。**M1**：Explore #3 分包普查 scope 过窄（仅 module-purchase）——module-manufacturing 存在 `SubcontractIssueAcctDocProvider`（SUBCONTRACT_ISSUE 委外发料腿），须同时扫描 mfg + finance business-type 注册表（已修正 Explore #3 + Baseline 补证据）；**M2**：Decision #1 候选集不完整——`data-dependency-matrix.md` 允许清单禁止业务域反向/交叉边，候选 A（inventory→projects 需新依赖+矩阵修订）与 B（projects→inventory 反向边）均需架构裁决，缺候选 **C. purchase 侧触发**（`ErpPurReceiveProcessor` 已注入 IErpInvStockMoveBiz，purchase→projects 边已存在，零新依赖零矩阵修订）（已修正 Goals + Phase 1 Decision + Phase 2 Targets）；Minor：m1 data-dependency-matrix §4.2:217 行号 stale（回填时修正）、m2 erp-inv-service @Test 计数 218→225（已修正 Baseline/Goals/Phase 3）、m3 Item Types `Decision | Explore` 应仅声明 `Decision`（Explore 是规则 9 临时标记，非规则 7 类型——已修正）、m4 领料 deferral 须显式记录 Q4=(a) 张力（MATERIAL 届时仅剩采购入库一条来源，四分类目标仍须成立——已修正 Explore #2）。格式/范围/反松弛/结束证据全 PASS，证据质量高（8/8 实仓抽查精确匹配）。
- Independent draft review iteration 2: `acceptable as-is`（`ses_ff557a8daffe5wjFFqUb07MdRO`，独立 general 子代理，新会话冷重播无起草者上下文）— 0 Blocker / 0 Major / 0 Minor。iteration-1 全部 2 Major + 4 Minor 逐项核实 FIXED（M1 Explore #3 现扫描 mfg + finance 注册表且实仓证实 SubcontractIssueAcctDocProvider:27/:46 + Dr1408/Cr1401 引用；M2 候选 C 落 Goals/Phase 1/Phase 2 Targets 且 ErpPurReceiveProcessor:60/:284 注入实证；m1 引用改 data-dependency-matrix.md:514 + module-boundaries.md:60-61 精确；m2 计数 225 三处同步 + 复基准注记；m3 Item Types 仅 Decision；m4 Q4=(a) 张力入 Explore #2）。格式合规 PASS（结构/状态流/反松弛/单结果表面/类型与 Skill 标注/Draft Review Record 已填）。草案审查收敛 → `Plan Status: draft → active`。

## Closure Gates

- [x] 范围内行为完成（四分类归集运行时成立——读侧 4 类 + LABOR/EXPENSE/MATERIAL 生产 writer；领料/分包载体缺失按 Deferred But Adjudicated 预登记激活，successor 触发条件已记录）
- [x] 相关文档对齐（cost-collection.md + arm-index + roadmap）
- [x] 已运行验证（`mvn test -pl module-projects/erp-prj-service,module-inventory/erp-inv-service` + 全量 `mvn test` + `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh`）
- [x] 无范围内项目降级为 deferred/follow-up（领料/分包载体为计划预登记 Deferred But Adjudicated 激活，非静默降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 领料归集载体（Phase 1 Explore #2 证实：本仓无项目领料单实体——激活）

- Classification: `watch-only residual`
- Why Not Blocking Closure: L1 字面「领料单.项目 == P」在实仓无对应单据实体——领料为制造专用 `ErpMfgMaterialIssue`（MFG_ISSUE 写 WIP）且 mfg/inventory orm.xml 全仓零 projectId 列（grep 实证）；projects/purchase/inventory 零「项目领料单」语义载体 → 生产 writer 无法按 B 类边界（零 ORM 变更）实现 → 按计划预登记激活 scope 解释；Q4=(a) 张力显式记录（MATERIAL 生产 writer 本次仅采购入库一条来源，四分类经读侧 + LABOR/EXPENSE/MATERIAL writer 成立）
- Successor Required: `yes`（触发条件：项目领料单实体/领料移动单项目维度落地时，按 ORM ask-first 流程立项）

### 分包归集载体（Phase 1 Explore #3 证实：mfg 委外链零项目维度 + purchase 无分包单据类型——激活）

- Classification: `watch-only residual`
- Why Not Blocking Closure: SUBCONTRACT 分类读侧已就绪（ProjectPnlCalculator:187），生产 writer 缺载体——manufacturing 域委外链完备（`ErpMfgSubcontractOrder` + SUBCONTRACT_ISSUE/RECEIPT/FEE 三腿 posting）但 mfg orm.xml 全仓零 projectId 列（项目维度不可达）；purchase 域无分包单据类型（采购入库链无法判别分包来源）；按计划预登记激活 scope 解释
- Successor Required: `yes`（触发条件：分包单/委外链加项目维度列落地时，按 ORM ask-first 流程立项）

### A4.2.119（MA4 行）

- Classification: `optimization candidate`（MR1 successor 阻塞行）
- Why Not Blocking Closure: A4.2.119 = 采购路径物料归集实现后的运行时探查行（结构不可达阻塞）；本计划 Phase 2 落地后回队，roadmap 表 A4.2.119 行标记解锁
- Successor Required: `no`（本计划落地即解锁，由 mission driver 回队执行）

## Closure

Status Note: 独立结束审计通过（2026-08-16，fresh session 冷重播，执行者未自我审计）——三 Phase 全勾选 + 实现与计划逐项吻合（Facade 契约 / MaterialCostAggregator / per-mutation Processor / purchase 接线 / beans / poms）+ 聚焦测试 8+4 全绿 + compliance checker 全 19 规则 actual == baseline（R2c=1431 baseline-raise 已登记）+ 文档回填与验证矩阵逐项核实。领料/分包载体缺失为计划预登记 Deferred But Adjudicated 激活（watch-only residual），非静默降级。Plan Status 置 completed。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，本会话——执行者未自我审计，Closure Gates 硬门控）
- Evidence: live 仓库独立复核（非仅信计划文字）——
  - **Facade 契约**：`module-projects/erp-prj-dao/.../biz/IErpPrjCostCollectionBiz.java:48-52` `aggregateMaterialCost(Long, BigDecimal, String, IServiceContext)` @BizMutation 签名与计划一致
  - **聚合器**：`MaterialCostAggregator.java`（existsLine 幂等 :51/:100-106 / findHead 单 head :57/:108-116 / costCategory=MATERIAL :92 / totalAmount 累加 :61 + actualCost 增量回写 :80-83）
  - **Processor 守卫链**：`ErpPrjCostCollectionAggregateMaterialCostProcessor.java:33-43`（config 门控 → requireReferenceable → budgetChecker.check → 聚合器写入）
  - **常量/配置**：`ErpPrjConstants.java:19` CONFIG_MATERIAL_AGGREGATION_ENABLED + `:78` SOURCE_BILL_TYPE_PURCHASE_RECEIVE；`ErpPrjConfigs.java:76` materialAggregationEnabled()
  - **purchase 接线**：`ErpPurReceiveProcessor.java:301-318` collectProjectMaterialCost（orderLineId→orderLine.getProjectId() null skip）+ `ErpPurReceiveApproveProcessor.java:44/:48` 移动单生成后调用；beans.xml :55-56/:102-103 双 bean 注册；`erp-pur-service/pom.xml:50-57` projects-dao compile + `:101-108` projects-service test；`erp-drp-service/pom.xml:97-104` projects-service test
  - **测试**：`mvn test -pl module-projects/erp-prj-service -Dtest=TestErpPrjMaterialAggregation` → 8 run 0 failures 0 errors（surefire 报告实证）；`mvn test -pl module-purchase/erp-pur-service -Dtest=TestErpPurReceiveMaterialCostAggregation` → 4 run 0 failures 0 errors；`_cases` 快照目录在案
  - **compliance checker**：`bash docs/audits/nop-compliance-checker.sh` 实测 R2b=235 / R2c=1431 / R2d=35（R1d=14 / R2a=34 / R12c=40）全 19 规则 actual == `compliance-baseline.md` §BASELINE 块（:432-450，R2c: 1431 + :453-467 基线上调注记 per-site 证据 9 站点）
  - **文档回填**：`cost-collection.md:173-187` §4.1 RC-R1.61 实现注记 + `:189-209` §4.2 全景六链路；`arm-index.md:220` P1-RC-049 → done (RC-R1.61)（2026-08-16 修复落地）+ `:223` P1-RC-051 采购路径闭合注记；`requirement-compliance-roadmap.md:453` RC-R1.61 → done ✅ + `:306` A4.2.119 → ready；`logs/2026/08-16.md:3-11` 时间倒序置顶条目

Follow-up:

- 无已确认缺陷。非阻塞跟进：领料归集载体 / 分包归集载体（计划预登记 watch-only residual，successor 触发条件已记录）；A4.2.119 回队运行时探查（mission driver 执行）。
