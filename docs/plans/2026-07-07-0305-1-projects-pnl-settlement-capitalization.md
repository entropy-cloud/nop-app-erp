# 2026-07-07-0305-1 projects-pnl-settlement-capitalization

> Plan Status: completed
> Last Reviewed: 2026-07-07
> Source: `docs/backlog/extended-roadmap.md` 工作项 2.6b（UC-PRJ-05~07：项目损益计算 / 项目结算 / 项目结算转固）；承接 `docs/plans/2026-07-03-1018-2-projects-cost-collection.md` Deferred「项目成本资本化（CIP→固定资产）/ 关闭结转损益」+ `docs/plans/2026-07-06-1606-1-remaining-domain-dashboards-backend.md` Deferred「项目毛利率 `ErpPrjProjectPnl` 缺失」
> Related: `2026-07-03-1018-2-projects-cost-collection.md`（成本归集前置）、`2026-07-02-1000-2-assets-depreciation-disposal-capitalization.md`（资本化/转固前置）、`2026-07-06-1606-1-remaining-domain-dashboards-backend.md`（看板后置 successor）
> Audit: required

## Current Baseline

- **projects 域成本侧已就绪**：`ErpPrjCostCollection`（工时/物料/费用/分包归集，costCategory 四类）done 计划 1018-2；`ErpPrjTimesheet.costAmount` 明细 done；预算侧 `ErpPrjBudget/BudgetLine`（含 committedAmount）存在；收入侧 `ErpPrjBilling`（开票 amountFunctional）+ `ErpPrjMilestone.billingAmount` 存在。`ErpPrjProject` 有 status 状态机（含 COMPLETED 终态）。
- **转固（资本化）通道已就绪**：assets 域 `IErpAstAssetBiz` + `CapitalizationAcctDocProvider` + `AssetPostingDispatcher`（`CAPITALIZATION(80)` 业务类型，支持 sourceType `DIRECT_PURCHASE(30)`/`CIP(20)`）done 计划 1000-2。`ErpAstAsset` 卡片可经跨域 R 创建（projects→assets，镜像 2.6 cost-collection→assets notGenCode 范式）。
- **业财过账引擎 SPI 就绪**：`IErpFinVoucherBiz.post/reverse` facade（finance-dao 跨域契约层）+ `IErpFinAcctDocProvider` 注册表（finance-service；projects-service 须依赖 `app-erp-finance-service`，镜像 `erp-ast-service/pom.xml` 既有依赖形态）。`ErpFinBusinessType` 枚举当前最大 code=420（`COST_ADJUSTMENT`），下一个空闲 code=430。
- **nop-job 已接线**：`app-erp-all/src/main/resources/_vfs/nop/job/conf/scheduler.yaml` 已注册 11 个 job（含 erp-fin/erp-ast/erp-mfg/erp-crm/erp-cs），cron 经配置键门控的范式已验证（如 `erp-ast.depreciation-cron`）。
- **设计文档存在但存在命名/编号漂移**：owner 设计在 `docs/design/projects/profitability.md`（108 行，三实体字段清单 + 流程 + 关键决策完整），但 roadmap 目标路径写作 `projects/pnl-settlement.md`（不存在）。`projects/use-cases.md` 中 UC-PRJ-05=DAG校验/06=损益/07=结算/08=转固，与 roadmap「2.6b=UC-PRJ-05~07、2.6c=UC-PRJ-08」标签冲突 —— 本计划覆盖 use-cases 口径的 UC-PRJ-06（损益）/07（结算）/08（转固）。
- **三个目标实体均未物化**：`grep ErpPrjProjectPnl|ErpPrjProjectSettlement module-projects/model` 返回 0 命中。`ErpPrjDashboardBizModel`（1606-1）因 `ErpPrjProjectPnl` 缺失，项目毛利率指标裁定为 Non-Goal。
- **剩余差距**：损益汇总、结算单（含质保金 retention）、CLOSE 结算转固全链未实现。

## Goals

- 物化 `ErpPrjProjectPnl` / `ErpPrjProjectSettlement` / `ErpPrjProjectSettlementLine` 三实体 + 2 字典，经 model→codegen 生成 dao/meta/service/web 链。
- 实现项目损益汇总引擎：按项目聚合 Billing 收入 + CostCollection 四类成本 → `ErpPrjProjectPnl`（含毛利/毛利率/完工预测 EAC），支持手工 `refreshPnl` 与 nop-job 周期触发。
- 实现项目结算单三轴状态机（docStatus/approveStatus/posted）：submit/approve/reject/cancel + `FINAL`/`INTERIM`/`CLOSE` 三 settlementType。
- 实现 CLOSE 结算转固：调用 `IErpAstAssetBiz` 生成资产卡片（IN_SERVICE + 折旧计划）+ 经 finance 域 `IErpFinAcctDocProvider` 注册新业务类型 `PROJECT_SETTLEMENT(430)` 生成结转凭证，`reverseSettlement` 红冲回退卡片状态与凭证。
- 解除 1606-1 Deferred「项目毛利率 `ErpPrjProjectPnl` 缺失」的数据源阻塞（实体 + 计算落地，看板后端 successor 取数通道就绪）。

## Non-Goals

- 项目毛利率看板后端 `@BizQuery` 接线与前端卡片（独立 successor，触发条件=本计划 `ErpPrjProjectPnl` 落地；属 dashboard 结果面，非本计划结果面）。
- 质保金 retention 的 AR/AP 辅助账核销与到期催收（归 finance AR/AP successor）。
- 收入确认（revenue recognition）独立引擎（按里程碑/完工百分比自动确认收入）—— 本计划仅汇总已开票 Billing，不做确认时点判断。
- 多项目/多组织合并损益报表渲染（归 nop-report successor，报表子系统已就绪 0504-2）。
- 里程碑自动触发结算（本计划手工/批量触发；自动触发归 successor）。
- 项目间成本分摊/内部交易抵消（独立能力面）。

## Task Route

- Type: `implementation-only change`（owner 设计 done 于 `profitability.md`，仅一处文档命名/UC 编号 Decision 需收敛）
- Owner Docs: `docs/design/projects/profitability.md`（权威设计）、`docs/design/projects/cost-collection.md`（成本数据源）、`docs/design/assets/state-machine.md`（转固目标侧）、`docs/architecture/` 过账 SPI 与模块边界
- Skill Selection Basis: 全部为后端 BizModel/IBiz/跨域 I*Biz/ErrorCode/过账 Provider/nop-job 工作 → 加载 `nop-backend-dev`；测试经 `JunitAutoTestCase`+IGraphQLEngine → 加载 `nop-testing`。两技能必需输入（owner 设计、ORM 模型、过账 SPI 范式）均就绪。

## Infrastructure And Config Prereqs

- nop-job 调度器已运行（`app-erp-all` scheduler.yaml enabled=true），新 job 仅需追加注册项。
- 新增配置键（`ErpPrjConstants` 声明 + `NopSysVariable` 默认值）：`erp-prj.pnl-calc-cron`（损益汇总周期，默认 `0 0 1 * * ?`）、`erp-prj.pnl-auto-calc-enabled`（默认 false）、`erp-prj.settlement-require-approval`（默认 true）。
- 新增 finance 业务类型 `PROJECT_SETTLEMENT(430)` 须同步追加 `ErpFinBusinessType` 枚举常量 + `_ErpFinDaoConstants.BUSINESS_TYPE_PROJECT_SETTLEMENT` + 字典 `erp-fin/business-type` 数值项（保护区域契约，镜像 0540-3/2352-2 范式）。
- 无外部端口/密钥/数据迁移依赖；codegen 增量生成，无回滚脚本需求（新增实体无既有数据）。

## Execution Plan

### Phase 1 - 设计收敛与 ORM 模型物化

Status: completed
Targets: `docs/design/projects/profitability.md`、`module-projects/model/app-erp-projects.orm.xml`、`docs/backlog/extended-roadmap.md`（UC 标签）、codegen 产物
Skill: `nop-backend-dev`

- Item Types: `Decision | Add`
- Prereqs: 无

- [x] Decision: 收敛 owner 设计命名与 UC 编号。将 `docs/design/projects/profitability.md` 重命名为 roadmap 目标路径 `projects/pnl-settlement.md`（或反向修正 roadmap 路径）；在 `extended-roadmap.md` 与 `projects/use-cases.md` 之间统一 UC-PRJ-06/07/08 标签口径，记录选择与残留风险（既有指向 `profitability.md` 的外部链接可能失效，需全仓 grep 修正引用）。
  - Skill: none
- [x] Add: 在 `module-projects/model/app-erp-projects.orm.xml` 追加三实体（字段对齐 `profitability.md` §实体清单）：`ErpPrjProjectPnl`（projectId/periodFrom/periodTo/多币种四件套/revenueAmount/costLabor/costMaterial/costExpense/costSubcontract/totalCost/grossProfit/grossMarginPct/committedCost/budgetAmount/forecastCompleteCost/calcStatus/posted+postedAt+postedBy/docStatus/approveStatus）、`ErpPrjProjectSettlement`（projectId/customerId/businessDate/settlementType/pnlSnapshotId/finalRevenue/finalCost/finalProfit/retentionAmount/retentionDueDate/transferToAsset/assetCardId/settlementVoucherCode/docStatus/approveStatus/posted 三件套）、`ErpPrjProjectSettlementLine`（settlementId/lineNo/lineType/sourceBillType+sourceBillCode/subjectId/amount）。新增字典 `erp-prj/pnl-calc-status`、`erp-prj/settlement-type`。
  - Skill: `nop-backend-dev`
- [x] Add: 经 nop-cli 对 projects 域增量 codegen（dao entity + IBiz + meta + service/web 空壳 + action-auth），验证生成产物与既有 17 实体链一致。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 三实体 + 2 字典在 orm.xml 中通过 XDef 校验；projects 域 codegen 产物存在（dao entity 类、IBiz 接口、CrudBizModel 空壳）。
- [x] projects 模块 `mvn clean install -DskipTests -pl module-projects -am` BUILD SUCCESS（解除 Phase 2 编译依赖）。

### Phase 2 - 项目损益汇总引擎与 nop-job

Status: completed
Targets: `IErpPrjProjectPnlBiz`、`ErpPrjProjectPnlBizModel`、`ProjectPnlCalculator`、`ErpPrjPnlCalcJob`、`ErpPrjConstants`、`ErpPrjErrors`、scheduler.yaml
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] Add: 在 `IErpPrjProjectPnlBiz` 声明并实现 `refreshPnl(@Name projectId, @Name periodFrom, @Name periodTo, context)`（@BizMutation）—— 经 `ProjectPnlCalculator` 聚合：收入=该期间 `IErpPrjBillingBiz` sum(amountFunctional)；成本=该期间 `IErpPrjCostCollectionBiz` 按 costCategory 分组 sum(amountFunctional)；committedCost=`ErpPrjBudgetLine.committedAmount`；budgetAmount=`ErpPrjBudget.totalAmount`；forecastCompleteCost（EAC）=实际成本+ETC（ETC=budget−committed，config-gated 算法）。多币种经 exchangeRate 折算到项目 currencyId。幂等（同期间重算清旧重建行 / 或 upsert by projectId+periodFrom+periodTo）。
  - Skill: `nop-backend-dev`
- [x] Add: `@BizQuery getProjectPnl(projectId, context)` 返回最新 calcStatus=CALCULATED 汇总快照。
  - Skill: `nop-backend-dev`
- [x] Add: `ErpPrjPnlCalcJob`（nop-job invoker bean `erpPrjPnlCalcJob`）扫描 status∈{IN_PROGRESS,...活跃} 项目批量 refreshPnl；在 scheduler.yaml 追加 job 项（cron 经 `erp-prj.pnl-calc-cron` 门控 + 总开关 `erp-prj.pnl-auto-calc-enabled` 默认 false 双层门控，镜像 erp-mfg-jobcard-auto-generate 范式）。
  - Skill: `nop-backend-dev`
- [x] Add: `ErpPrjErrors` 声明 `ERR_PRJ_PNL_PERIOD_INVALID`、`ERR_PRJ_PNL_RECALC_FROZEN` 等 ErrorCode；`ErpPrjConstants` 声明配置键常量。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] `refreshPnl` 对含 Billing+CostCollection 的项目产出 `ErpPrjProjectPnl` 行，毛利/毛利率数值与手算一致（成功模式）；空项目不报错产出零行；非法期间抛 ErrorCode。
- [x] nop-job job 项注册且 invoker bean 可解析（本地化类型检查通过，解除 Phase 3 调度依赖不要求）。

### Phase 3 - 结算单状态机与转固过账

Status: completed
Targets: `IErpPrjProjectSettlementBiz`、`ErpPrjProjectSettlementBizModel`、`SettlementProcessor`、`ProjectSettlementAcctDocProvider`、`ProjectSettlementPostingDispatcher`、`ErpFinBusinessType`、`_ErpFinDaoConstants`、`erp-fin/business-type` 字典
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 2

- [x] Add: `IErpPrjProjectSettlementBiz` 声明并实现三轴状态机动作 `submit/approve/reject/cancel`（@BizMutation，强制审批 config-gated `erp-prj.settlement-require-approval`）；`createSettlement(projectId, settlementType, context)` 基于最新 pnlSnapshotId 快照填充 finalRevenue/finalCost/finalProfit + 按来源单据（Billing/CostCollection）生成 SettlementLine 明细。
  - Skill: `nop-backend-dev`
- [x] Add: 多步编排走 `SettlementProcessor`（protected step 方法，产品化可定制）：validateTransition → buildLines → doPost(审批通过后) → postProcess。`approve` 末尾按 settlementType 分派：FINAL/INTERIM 仅过账；CLOSE 额外触发转固。
  - Skill: `nop-backend-dev`
- [x] Add: CLOSE 转固分支 —— 调用 `IErpAstAssetBiz`（跨域 R，projects→assets）以 `transferToAsset=true` 创建资产卡片（status=IN_SERVICE + 生成折旧计划），回写 settlement.assetCardId；经 `ProjectSettlementPostingDispatcher` 组装 `PostingEvent`(PROJECT_SETTLEMENT) 调 `IErpFinVoucherBiz.post`；`ProjectSettlementAcctDocProvider implements IErpFinAcctDocProvider` 按 settlementType 方向相关分解科目（CLOSE：借固定资产/贷在建工程或项目成本结转）。
  - Skill: `nop-backend-dev`
- [x] Add: 扩展 `ErpFinBusinessType` 枚举新增 `PROJECT_SETTLEMENT(430)` + `_ErpFinDaoConstants.BUSINESS_TYPE_PROJECT_SETTLEMENT` + 同步字典 `erp-fin/business-type` 数值项（保护区域契约三件套）。
  - Skill: `nop-backend-dev`
- [x] Add: `reverseSettlement(settlementId, context)`（@BizMutation）红字冲销：经 `IErpFinVoucherBiz.reverse` 红冲 PROJECT_SETTLEMENT 凭证 + 回退资产卡片状态（已转固时经 IErpAstAssetBiz 反向）+ settlement.posted=false。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] FINAL 结算 approve 后生成 PROJECT_SETTLEMENT 凭证且 settlement.posted=true（成功模式）；CLOSE 结算 approve 后额外创建 `ErpAstAsset` 卡片（assetCardId 非空）+ 凭证；非 CLOSE 不创建卡片。
- [x] reverseSettlement 红冲凭证 + 卡片状态回退一致。

### Phase 4 - 测试与看板 successor 解除

Status: completed
Targets: `TestErpPrjProjectPnl`、`TestErpPrjProjectSettlement`、`module-projects/erp-prj-service/src/test/`
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 3

- [x] Proof: `TestErpPrjProjectPnl`（IGraphQLEngine）：含 Billing+四类成本项目的 refreshPnl 毛利计算、空项目零行、非法期间 ErrorCode、重算幂等。
  - Skill: `nop-testing`
- [x] Proof: `TestErpPrjProjectSettlement`：FINAL/CLOSE 两路径（FINAL 仅过账；CLOSE 转固创建卡片+凭证+assetCardId 回写）、状态机非法迁移 ErrorCode、强制审批门控、reverseSettlement 红冲一致性、sourceBillType 三元组凭证回链。
  - Skill: `nop-testing`
- [x] Proof: PROJECT_SETTLEMENT 凭证经 `IErpFinVoucherBiz` 落库 + reverse 后红字凭证存在（凭证回链断言，镜像 0540-3 `findBillLinks` 范式）。
  - Skill: `nop-testing`

Exit Criteria:

- [x] projects 域新测试全绿（0 failures/0 errors），覆盖成功 + 异常路径。

## Draft Review Record

- Independent draft review iteration 1: accept (ses_0c72941bcffesGPdJBzVNG2zfT) — 所有最低规则 R1–R14 满足，全部依赖在实时仓库确认就绪（三实体缺失、ErpFinBusinessType 430 空闲、nop-job 11 job 双层门控范式、CapitalizationAcctDocProvider/IErpAstAssetBiz 存在、两处继承 Deferred 核实）。采纳非阻塞建议：修正 `IErpFinAcctDocProvider` 模块归属（finance-service 而非 finance-dao，指明 projects-service 依赖形态）、Phase 1 Decision 内联残留风险（外部链接失效）。

## Closure Gates

- [x] 范围内行为完成（损益汇总 + 结算状态机 + CLOSE 转固过账 + reverse）
- [x] 相关文档对齐（`profitability.md`→`pnl-settlement.md` 命名收敛、UC 编号口径统一、roadmap 状态更新、`docs/logs/` 日志）
- [x] 已运行验证：`mvn clean install -DskipTests`（全 154+ 模块）+ `mvn test -pl module-projects -am` + 全 workspace `mvn test` 0 failures/0 errors
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 项目毛利率看板后端接线 + 前端卡片

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 属 dashboard 结果面（1606-1/1247-2/1606-2 owner），非本计划「projects PnL/settlement/转固」结果面。本计划落地 `ErpPrjProjectPnl` 实体与计算即解除数据源阻塞。
- Successor Required: yes —— 触发条件=本计划 completed；successor 在 `ErpPrjDashboardBizModel` 补 `getProjectGrossMargin` `@BizQuery` 读 `ErpPrjProjectPnl` + AMIS 卡片。

### retention 质保金 AR/AP 辅助账核销

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 质保金应收核销属 finance AR/AP 辅助账能力面（0300-3/0115-1 owner），本计划仅记录 retentionAmount/retentionDueDate 字段，不做核销。
- Successor Required: yes —— 触发条件=finance AR/AP 核销引擎扩展支持 project-settlement retention 来源类型时。

## Closure

Status Note: 全部 4 个 Phase 执行完成。三实体（ErpPrjProjectPnl/ErpPrjProjectSettlement/ErpPrjProjectSettlementLine）+ 2 字典物化并经 codegen 生成完整 dao/meta/service/web 链。损益汇总引擎（ProjectPnlCalculator + ErpPrjPnlCalcJob 双层门控）+ 结算三轴状态机（SettlementProcessor protected step）+ CLOSE 转固（IErpAstAssetBiz 跨域建卡）+ PROJECT_SETTLEMENT(430) 保护区域契约三件套扩展 + reverseSettlement 红冲全部落地。8 个新测试全绿（45 projects-service + 177 finance-service 0 failures/0 errors），解除 1606-1 Deferred「ErpPrjProjectPnl 缺失」数据源阻塞。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，glm-5.2 build agent）已执行两轮。
- 第一轮发现：保护区域契约三件套仅 1/3 landed——`ErpFinBusinessType.PROJECT_SETTLEMENT(430)` 枚举存在，但 `erp-fin/business-type` 字典 option 与 codegen 派生的 `_ErpFinDaoConstants.BUSINESS_TYPE_PROJECT_SETTLEMENT` 常量缺失（dict 在 COST_ADJUSTMENT 后即闭合）。
- 修复：在 `module-finance/model/app-erp-finance.orm.xml` business-type 字典追加 `<option code="PROJECT_SETTLEMENT" label="项目结算" .../>`；`mvn clean install -DskipTests -pl module-finance/erp-fin-codegen -am` 增量 codegen 重新生成 `_ErpFinDaoConstants.BUSINESS_TYPE_PROJECT_SETTLEMENT`（line 279）。
- 验证：`mvn clean install -DskipTests` 全 workspace BUILD SUCCESS；`mvn test -pl module-projects/erp-prj-service` 45 tests 0 failures/0 errors（含 TestErpPrjProjectPnl 4 + TestErpPrjProjectSettlement 4）；finance-service 全部 test class 0 failures/0 errors。
- Evidence: 执行证据——新增文件清单见各 Phase 产物（ProjectPnlCalculator/ErpPrjPnlCalcJob/ErpPrjProjectSettlementProcessor/ProjectSettlementPostingDispatcher/ProjectSettlementAcctDocProvider/IErpPrjProjectPnlBiz/IErpPrjProjectSettlementBiz + 2 测试类）。

Follow-up:

- 项目毛利率看板接线（见 Deferred successor）。
