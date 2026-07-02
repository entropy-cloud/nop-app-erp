# 2026-07-02-1000-2-assets-depreciation-disposal-capitalization 资产折旧/处置/资本化 BizModel + 业财过账

> Plan Status: completed
> > Last Reviewed: 2026-07-02
> Source: `docs/backlog/extended-roadmap.md` 工作项 2.5（Asset 折旧/处置/资本化）；`docs/design/assets/depreciation-and-posting.md`、`docs/design/assets/state-machine.md`
> Related: `2026-07-02-1000-1-finance-treasury-notes.md`（同批 N=1，业务类型码段独立——本计划复用既有 70/80/90，不与 treasury 的 190–250 冲突）、`2026-07-01-2030-1-posting-engine-voucher-facade-processor.md`（过账 Provider 基础设施）、`2026-07-02-1000-3-finance-period-close.md`（N=3 期末结账，本计划折旧能力为其 step3 解除阻塞）
> Mission: erp
> Work Item: Asset 折旧/处置/资本化 BizModel + 业财过账（extended M2 · 2.5）
> Audit: required

## Current Baseline

实时仓库逐项核实的事实：

- **assets 全模块已生成**：`module-assets/` 含 model/codegen/dao/meta/service/web/app/api/deploy 模块（CRUD 已完成，crud-roadmap done）。`ErpAst*Api` CRUD 接口 + `IErpAst*Biz`/`ErpAst*BizModel` 已生成为 codegen 空 `CrudBizModel`/`ICrudBiz` 存根（无自定义业务方法，全仓 `rg` 核实 assets service BizModel 为空存根）。
- **资产卡片实体** `ErpAstAsset`（`assets.orm.xml:117`）：originalValue/currentValue/residualValue/depreciationMethod(dict erp-ast/depreciation-method)/depreciationRate/usefulLifeMonths/departmentId/locationId/employeeId/status(dict erp-ast/asset-status)。**缺失字段**：无 accumulatedDepreciation（累计折旧）列、无 netBookValue（净值）列——累计折旧/净值当前由 `ErpAstDepreciationSchedule.accumulatedDepreciation`/`netBookValue` 承载（计划条目级），资产卡片级汇总无持久化列。capitalization 后需在卡片反映累计折旧/净值。
- **资产类别** `ErpAstAssetCategory`（:174）：depreciationMethod/usefulLifeMonths/subjectId(固定资产科目)/depreciationSubjectId(累计折旧科目)/expenseSubjectId(折旧费用科目)。**缺失科目映射**：无 disposalGainLossSubjectId（清理损益科目）、无 cipSubjectId（在建工程科目）——`depreciation-and-posting.md §6.1` 要求五类科目（固定资产/累计折旧/折旧费用/清理损益/在建工程），现仅三类。
- **折旧计划** `ErpAstDepreciationSchedule`（:212）：assetId/period(VARCHAR)/plannedAmount/actualAmount/accumulatedDepreciation/netBookValue/status(dict erp-ast/depreciation-schedule-status)/posted/postedBy/postedAt/voucherId + 多币种三件套。**计划条目存在但无生成/执行逻辑**（codegen 空）。
- **处置单** `ErpAstDisposal`（:384）：assetId/disposalType(dict erp-ast/disposal-type)/disposalAmount/gainLoss/reason(docStatus+approveStatus 三轴+posted)。**无处置 BizModel**（codegen 空）。
- **资本化单** `ErpAstAssetCapitalization`（:433）：assetCode/assetName/categoryId/originalValue/sourceType(dict erp-ast/capitalization-source-type)/sourceCode(docStatus+approveStatus 三轴+posted)。**无资本化 BizModel**（codegen 空）。
- **价值调整** `ErpAstValueAdjustment`（:336）：实体存在，但属减值/重估面，**不在本工作项标题「折旧/处置/资本化」内**（Non-Goal）。
- **业财业务类型枚举（关键，草案审查核实）**：`ErpFinBusinessType`（erp-fin-dao）**已含** `DEPRECIATION(70)`/`CAPITALIZATION(80)`/`DISPOSAL(90)`（`:19-21`，与 PURCHASE_INPUT(10)…EMPLOYEE_ADVANCE_SETTLE(180) 并列）。**本计划复用既有 70/80/90，不新增业务类型常量**（追加同名常量将致 Java 编译错误）。`DISPOSAL(90)` 为单一类型，报废/出售的科目分解差异由 `DisposalAcctDocProvider` 按 `ErpAstDisposal.disposalType`（dict erp-ast/disposal-type: SCRAPPED=10/SOLD=20）内部分支处理，无需拆为 DISPOSAL_SCRAP/DISPOSAL_SALE。treasury 计划 1000-1 加 190–250 与本计划无关（码段独立）。
- **字典缺口（草案审查核实，须 Phase 1 补）**：(1) `erp-ast/capitalization-source-type`（assets.orm.xml:102）现仅 `INVENTORY(10)`/`CIP(20)`，**缺 DIRECT_PURCHASE**（直接购置场景，owner-doc §2.1 三场景）；本计划资本化 sourceType 用既有 `CIP(20)` + 新增 `DIRECT_PURCHASE(30)`。(2) `erp-ast/depreciation-schedule-status`（:78）现仅 `PENDING(10)`/`EXECUTED(20)`/`REVERSED(30)`，**缺 CANCELLED**（资产处置后后续期间计划标记取消，语义区别于 REVERSED 已执行冲销）；新增 `CANCELLED(40)`。
- **过账基础设施**（0811-1/2030-1 已落地）：入口 facade `IErpFinVoucherBiz.post`/`reverse`；`IErpFinAcctDocProvider` SPI + `ErpFinAcctDocRegistry`；共享执行器模式（executor 无 `@Transactional`）。assets posting Provider 注册 `app-service.beans.xml`（erp-ast-service 侧），产 facts 调 `IErpFinVoucherBiz.post`。
- **会计期间**：`ErpFinAccountingPeriod`(+Status) 存在（finance.orm.xml:348/381）；折旧执行需校验期间未结账（`depreciation-and-posting.md §1.2`）。
- **库存转固跨域**：`depreciation-and-posting.md §2.2` 库存转固需调 `IErpInvStockMoveBiz` 生成出库移动单——属跨域依赖，本计划 Non-Goal（仅直接购置 + 在建工程转固）。
- **剩余差距**：(1) 无折旧计算/执行/批量 + DEPRECIATION 凭证；(2) 无资本化（转固）审批→建卡→生成折旧计划 + CAPITALIZATION 凭证；(3) 无处置（报废/出售）审批→清理损益计算 + DISPOSAL 凭证；(4) 资产类别缺清理损益/在建工程科目映射；(5) 资产卡片缺累计折旧/净值汇总列。

## Goals

- ORM 小幅增量：`ErpAstAsset` 加 `accumulatedDepreciation`/`netBookValue` 汇总列；`ErpAstAssetCategory` 加 `disposalGainLossSubjectId`/`cipSubjectId` 科目映射列；`erp-ast/capitalization-source-type` 字典加 `DIRECT_PURCHASE(30)`；`erp-ast/depreciation-schedule-status` 字典加 `CANCELLED(40)`（assets 源模型 ask-first，重新 codegen）。
- 资本化 `IErpAstAssetCapitalizationBiz`（扩存根）三轴审批（submit/approve/reject）→ APPROVED 时建/激活 `ErpAstAsset`（status→IN_SERVICE）+ 生成 `ErpAstDepreciationSchedule` 折旧计划（按折旧方法每期一条）+ 触发 **CAPITALIZATION(80)** 过账（借固定资产/贷 在建工程(CIP)或银行存款 by sourceType）。
- 折旧 `IErpAstDepreciationScheduleBiz`（扩存根）：单资产 `executeDepreciation(period)` + 批量 `executeBatchDepreciation(period)`（查询 IN_SERVICE 资产按类别分组并行），计算本期折旧（直线法/双倍余额递减/工作量法），更新 schedule actualAmount/accumulatedDepreciation/netBookValue + 资产卡片汇总列，触发 **DEPRECIATION(70)** 过账（借折旧费用/贷累计折旧）。残值约束（净值不低于残值，`§1.4`）。
- 处置 `IErpAstDisposalBiz`（扩存根）三轴审批 → APPROVED 时计算清理损益（=处置收入−账面净值，`§3.3`）+ 资产 status→SCRAPPED/SOLD + 触发 **DISPOSAL(90)** 过账（单一类型，`DisposalAcctDocProvider` 按 disposalType=SCRAPPED/SOLD 分支科目分解：结转原值/累计折旧 + 清理损益）。
- 期间控制：折旧执行前校验目标期间未结账（`ErpFinAccountingPeriod.status != OPEN(10)`，即 CLOSED(30) 拒绝，`§关键规则1`）；折旧起止规则（资本化次月起、处置当月止，`§5.1`）。
- 行为测试覆盖三种折旧方法 + 残值约束 + 资本化建卡/计划生成 + 处置损益 + 四类凭证 + 红字冲减 + 期间控制。

## Non-Goals

- **资产减值/重估（VALUE_ADJUSTMENT）**：`ErpAstValueAdjustment` 实体存在，但减值/重估不在工作项 2.5 标题「折旧/处置/资本化」内；归后续计划（触发条件：减值测试需求启动）。
- **库存物料转固（IErpInvStockMoveBiz 出库）**：`§2.2` 库存转固需跨域调 inventory；本计划资本化 sourceType 仅支持 `DIRECT_PURCHASE(30)`（直接购置）+ `CIP(20)`（在建工程转固），`INVENTORY(10)` 归后续（触发条件：库存转固业务上线）。
- **nop-wf 多级审批路由**：资本化/处置审批 = 直接状态迁移 + `@BizMutation`（对齐全仓基线）。
- **nop-job 定时自动折旧**：`§5.1` 定时任务触发折旧属运营自动化；本计划提供手动 `executeBatchDepreciation`，定时调度归 Follow-up（触发条件：nop-job 接线时）。期末结账（1000-3）可调本计划批量折旧。
- **资产拆分/合并（ErpAstSplit/Merge）**：实体存在但属独立工作面，非本工作项。
- **资产移动（ErpAstMovement，部门/地点变更）**：属资产台账管理，非业财过账面。
- **折旧追溯报表 UI（nop-report）/ 资产台账报表**：仅提供数据与查询，不做报表渲染。
- **工作量法折旧的「预计总工作量」主数据维护 UI**：工作量法计算公式落地，但总工作量/累计工作量的录入维护属台账管理（提供字段，UI 归前端 roadmap）。

## Task Route

- Type: `implementation-only change`（业务逻辑 + ORM 小幅加性增量）。**注**：触及 ask-first 保护区域 `module-assets/model/app-erp-assets.orm.xml`（资产卡片/类别加列）。assets 已是生成模块，Phase 1 含 codegen 回归。
- Owner Docs: `docs/design/assets/depreciation-and-posting.md`（折旧/资本化/处置/科目映射/期间控制/批量规则）、`docs/design/assets/state-machine.md`（资产/处置状态机）、`docs/design/finance/posting.md`（Provider 三层 + 冲销）。
- Skill Selection Basis: BizModel 审批/批量计算 + 过账 Provider + 跨实体（资产→会计期间/科目）+ 错误码 → 加载 `nop-backend-dev`（覆盖 IBiz、跨实体访问、过账 Provider、CodeGen 增量回归自检、反模式自检）。
- **Decision（折旧计算实现位置）**：**选择**在 `erp-ast-service` 实现 `DepreciationCalculator`（直线法/双倍余额递减/工作量法三策略），由 `ErpAstDepreciationScheduleBizModel` 调用。**替代**：① 在 task.xml 编排（业务计算逻辑较重，编排不合适，rejected）；② SQL 存储过程（跨库不可移植，rejected）。**残留风险**：双倍余额递减最后两年改直线法的边界（由单测覆盖）。
- **Decision（批量折旧并行度）**：**选择**按资产类别分组串行组内并行（`§5.2`）。**替代**：全串行（大批量慢，rejected）；全并行无分组（事务/锁竞争，rejected）。**残留风险**：单资产失败错误隔离不影响他资产（`§5.3`，失败记录待人工）。
- **Decision（清理损益科目归属）**：**选择**按 `disposalGainLossSubjectId` 配置，区分收益（营业外收入）/损失（营业外支出）由 gainLoss 正负决定借贷方向。**替代**：硬编码科目（不灵活，rejected）。**残留风险**：类别未配科目时兜底扫描重试（`§7.2`）。

## Infrastructure And Config Prereqs

- 配置项（`depreciation-and-posting.md §配置` + 现仓约定）：`erp-ast.auto-depreciation-on-close`（默认 true，期末结账时是否自动批量折旧——供 1000-3 调用）、`erp-ast.depreciation-parallel-by-category`（默认 true）、`erp-ast.residual-value-enforced`（默认 true，净值不低于残值）。经 `AppConfig.var(..., defaultValue)` 读取，无 .env。
- 模块依赖：`erp-ast-service` 须 compile 依赖 `erp-fin-api`/`erp-fin-dao`（注入 `IErpFinVoucherBiz`、读 `ErpFinAccountingPeriod`）。需确认 pom 依赖方向（assets→finance 单向，finance 为 DAG 顶，符合 `data-dependency-matrix.md`）。
- **保护区域门控**：触及 `module-assets/model/app-erp-assets.orm.xml`（ask-first）。ORM 阶段实施前须：人工批准 + 本计划草案审查通过。审查者可用性 = `subagent`。

## Execution Plan

### Phase 1 — ORM 加列（资产卡片汇总 + 类别科目映射）+ 字典选项增量 + 重新 codegen + 回归

Status: completed
Targets: `module-assets/model/app-erp-assets.orm.xml`（ErpAstAsset 加 2 列、ErpAstAssetCategory 加 2 列、capitalization-source-type 加 DIRECT_PURCHASE(30)、depreciation-schedule-status 加 CANCELLED(40)）、经 codegen 重新生成
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: **人工批准**（model/*.orm.xml ask-first）+ 本计划草案审查通过。业务类型复用既有 70/80/90，与 treasury 计划 1000-1 的 190–250 码段独立，无前置依赖。

- [x] `Add`：`ErpAstAsset` 加 `accumulatedDepreciation`(domain amount, default 0) + `netBookValue`(domain amount) 汇总列（折旧执行后回写，反映卡片级累计折旧/净值）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpAstAssetCategory` 加 `disposalGainLossSubjectId`(→ErpMdSubject, 清理损益科目) + `cipSubjectId`(→ErpMdSubject, 在建工程科目)（补齐 `§6.1` 五类科目映射）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`erp-ast/capitalization-source-type` 字典加 `DIRECT_PURCHASE(30)`（直接购置，owner-doc §2.1 三场景补齐）；`erp-ast/depreciation-schedule-status` 字典加 `CANCELLED(40)`（资产处置后后续期间计划取消，区别于 REVERSED 已执行冲销）。
  - Skill: `nop-backend-dev`
- [x] `Proof`：重新 codegen 后，assets 既有 CRUD 套件全绿（加列/加字典选项不破坏既有）；本地化 `mvn test -pl module-assets/erp-ast-service -am`。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 1 交付资产卡片/类别加列 + 2 字典选项 + codegen 无回归（业务类型复用既有 70/80/90，无需新增）。解除 Phase 2/3/4 对列与字典的阻塞。

- [x] 资产卡片 2 列 + 类别 2 列 + capitalization-source-type DIRECT_PURCHASE + depreciation-schedule-status CANCELLED 落库（codegen 产物更新）；既有 assets 测试无回归

### Phase 2 — 资本化（转固）审批 → 建卡 → 折旧计划生成 + CAPITALIZATION 过账

Status: completed
Targets: `module-assets/erp-ast-service/.../entity/ErpAstAssetCapitalizationBizModel.java`(扩存根)、`.../entity/ErpAstAssetBizModel.java`(扩)、`.../biz/IErpAstAssetCapitalizationBiz.java`(扩存根)、`.../posting/CapitalizationAcctDocProvider.java`(新)、`.../posting/AssetPostingDispatcher.java`(新)、`.../ErpAstErrors.java`(扩)、`erp-ast-service/.../_vfs/erp/ast/beans/app-service.beans.xml`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（列/字典选项存在）。

- [x] `Add`：`IErpAstAssetCapitalizationBiz`（扩存根）三轴审批 submit/approve/reject。`ErpAstAssetCapitalizationBizModel` 实现：APPROVED 时建/激活 `ErpAstAsset`（categoryId→继承 depreciationMethod/usefulLifeMonths；originalValue/status→IN_SERVICE；accumulatedDepreciation=0；netBookValue=originalValue）。
  - Skill: `nop-backend-dev`
- [x] `Add`：资本化 APPROVED 时生成 `ErpAstDepreciationSchedule` 折旧计划（按折旧方法 + 残值 + 使用年限，每期一条 PENDING；直线法每期等额，残值约束最后一期调整）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`CapitalizationAcctDocProvider` 产 CAPITALIZATION(80) facts——借固定资产(类别 subjectId)/贷方按 sourceType：DIRECT_PURCHASE→银行存款/应付账款，CIP→在建工程(类别 cipSubjectId)。注册 beans。
  - Skill: `nop-backend-dev`
- [x] `Add`：`AssetPostingDispatcher`（复用共享 executor，无 `@Transactional`）——资本化 APPROVED 后组装 PostingEvent(businessType=CAPITALIZATION(80), billHeadCode, billData 含 ASSET_ID/CATEGORY_ID/ORIGINAL_VALUE/SOURCE_TYPE/DEPARTMENT_ID + orgId + acctSchemaId) 调 `IErpFinVoucherBiz.post`；成功置 posted=true。
  - Skill: `nop-backend-dev`
- [x] `Decision`：资本化建卡 vs 复用既有卡片——**选择**资本化单 APPROVED 时创建新 `ErpAstAsset`（资本化单是转固凭证，资产卡片是其产物）。**替代**：手工先建卡片再资本化（流程割裂，rejected）。**残留风险**：重复资本化同一资产——由 approveStatus 幂等 + 资产 code 唯一约束兜底。
  - Skill: none
- [x] `Proof`：`TestErpAstCapitalization`（三轴审批→建卡 IN_SERVICE + 折旧计划生成 + CAPITALIZATION(80) 凭证 借固定资产/贷方按 sourceType + posted=true；残值/使用年限继承正确）。`mvn test -pl module-assets/erp-ast-service -am -Dtest=TestErpAstCapitalization*`。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 2 交付资本化审批→建卡→折旧计划→CAPITALIZATION(80) 凭证。解除 Phase 3 对折旧计划/资产卡片的阻塞。

- [x] 资本化建卡 + 折旧计划生成 + CAPITALIZATION(80) 凭证落库单测通过

### Phase 3 — 折旧计算/执行/批量 + DEPRECIATION 过账 + 残值约束 + 期间控制

Status: completed
Targets: `module-assets/erp-ast-service/.../entity/ErpAstDepreciationScheduleBizModel.java`(扩存根)、`.../biz/IErpAstDepreciationScheduleBiz.java`(扩存根)、`.../posting/DepreciationAcctDocProvider.java`(新)、`.../service/DepreciationCalculator.java`(新,三策略)
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 2（资产卡片/折旧计划存在）。

- [x] `Add`：`DepreciationCalculator` 三策略——直线法 (原值−残值)/使用年限/12；双倍余额递减 2×账面净值/使用年限/12（最后两年改直线法）；工作量法 (原值−残值)/预计总工作量×本期工作量。残值约束：折旧后净值不低于残值（`§1.4`）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`IErpAstDepreciationScheduleBiz`（扩存根）`executeDepreciation(assetId, period)` + `executeBatchDepreciation(period)`（该方法发布到 `erp-ast-api` 供 N=3 期末结账经 I*Biz 跨模块调用——声明于 dao 层 IBiz 后须重新 codegen 使 Api 契约传播）。执行前校验目标期间 `ErpFinAccountingPeriod.status == OPEN(10)`（CLOSED(30)/CLOSING(20) 拒绝，注入读 `ErpFinAccountingPeriod`）；折旧起止规则（资本化次月起、处置当月止）；计算后更新 schedule actualAmount/accumulatedDepreciation/netBookValue/status=EXECUTED + 资产卡片 accumulatedDepreciation/netBookValue 汇总列。
  - Skill: `nop-backend-dev`
- [x] `Add`：`DepreciationAcctDocProvider` 产 DEPRECIATION(70) facts——借折旧费用(类别 expenseSubjectId)/贷累计折旧(类别 depreciationSubjectId)。批量折旧汇总单张凭证多行（`§5.2`）。注册 beans。
  - Skill: `nop-backend-dev`
- [x] `Add`：`executeDepreciation` 触发 DEPRECIATION(70) 过账（经 AssetPostingDispatcher）；schedule.posted=true + voucherId 回链。错误隔离（单资产失败记录待人工，不影响他资产，`§5.3`）。
  - Skill: `nop-backend-dev`
- [x] `Decision`：幂等性——同一期间重复执行折旧时**先冲销已执行折旧凭证再重新生成**（`§5.1`）。**替代**：拒绝重复执行（调整场景不便，rejected）。**残留风险**：冲销+重生成增加凭证数量（低频期末操作，可接受）。
  - Skill: none
- [x] `Proof`：`TestErpAstDepreciationStraightLine`（直线法每期等额 + 最后一期到残值 + DEPRECIATION(70) 凭证）、`TestErpAstDepreciationDoubleDeclining`（双倍余额递减 + 最后两年改直线 + 残值约束）、`TestErpAstDepreciationBatch`（批量按类别分组 + 期间 status != OPEN(10) 拒绝 + 重复执行幂等冲销重生成）、`TestErpAstDepreciationPeriodControl`（起止月规则）。`mvn test -pl module-assets/erp-ast-service -am -Dtest=TestErpAstDepreciation*`。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 3 交付三种折旧方法计算/执行/批量 + DEPRECIATION(70) 凭证 + 残值约束 + 期间控制。解除 Phase 4 处置对累计折旧/净值的依赖。

- [x] 三种折旧方法 + 批量 + DEPRECIATION(70) 凭证 + 残值约束 + 期间控制单测通过；资产卡片累计折旧/净值汇总正确

### Phase 4 — 处置（报废/出售）审批 → 清理损益 + DISPOSAL 过账 + 红字冲减

Status: completed
Targets: `module-assets/erp-ast-service/.../entity/ErpAstDisposalBizModel.java`(扩存根)、`.../biz/IErpAstDisposalBiz.java`(扩存根)、`.../posting/DisposalAcctDocProvider.java`(新)、`.../posting/AssetPostingDispatcher.java`(扩 DISPOSAL)
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 3（资产有累计折旧/净值）。

- [x] `Add`：`IErpAstDisposalBiz`（扩存根）三轴审批 submit/approve/reject。`ErpAstDisposalBizModel` 实现：APPROVED 时计算清理损益 gainLoss=处置收入−账面净值（账面净值=原值−累计折旧，`§3.3`）+ 资产 status→SCRAPPED(disposalType=SCRAPPED)/SOLD(出售)。
  - Skill: `nop-backend-dev`
- [x] `Add`：`DisposalAcctDocProvider` 产单一 DISPOSAL(90) facts，按 `disposalType` 内部分支科目分解：SCRAPPED（借累计折旧/借清理损失/贷固定资产）/ SOLD（借累计折旧/借银行存款/[借清理损失|贷清理收益]/贷固定资产）。清理损益科目按 gainLoss 正负走营业外支出/收入（类别 disposalGainLossSubjectId）。
  - Skill: `nop-backend-dev`
- [x] `Add`：处置 APPROVED 经 AssetPostingDispatcher 触发 DISPOSAL(90) 过账；disposal.posted=true。终态不可恢复（`§关键规则3`，已处置资产不可重新激活，需冲销）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`reverseApprove`/红字冲减——资本化/折旧/处置已 posted 单据调 `IErpFinVoucherBiz.reverse` 红字冲销；折旧冲销回滚资产卡片累计折旧/净值；处置冲销恢复资产 status。
  - Skill: `nop-backend-dev`
- [x] `Decision`：处置后剩余折旧计划——**选择**处置当月停止后续期间 schedule 标记 `CANCELLED(40)`（Phase 1 新增字典值，区别于 REVERSED 已执行冲销；`§5.1` 当月减少当月停）。**替代**：保留 PENDING（产生悬空计划，rejected）；用 REVERSED（语义混淆已执行冲销 vs 未执行取消，rejected）。**残留风险**：无。
  - Skill: none
- [x] `Proof`：`TestErpAstDisposalScrap`（报废 SCRAPPED→清理损失 + DISPOSAL(90) 凭证 + 资产 SCRAPPED + 后续 schedule CANCELLED(40)）、`TestErpAstDisposalSale`（出售 SOLD→清理收益/损失 + DISPOSAL(90) 凭证 + 银行存款）、`TestErpAstPostingReverse`（资本化/折旧/处置红字冲销 + 资产卡片/状态回滚）。`mvn test -pl module-assets/erp-ast-service -am -Dtest=TestErpAstDisposal*,TestErpAstPostingReverse*`。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 4 交付处置审批→清理损益→DISPOSAL(90) 凭证 + 红字冲减。完整仓库验证属 Closure Gates。

- [x] 报废/出售处置 + DISPOSAL(90) 凭证 + 清理损益 + 资产终态 + 后续折旧停止(schedule CANCELLED)单测通过；红字冲减（资本化/折旧/处置）回滚正确
- [x] 端到端（资本化→多期折旧→处置）单测全绿

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（`ses_0dfb6c6c8ffeXNFHRs6hsEp986`，独立 general 子代理，对照实时仓库复核）。3 BLOCKER：(B1) `ErpFinBusinessType` **已含** DEPRECIATION(70)/CAPITALIZATION(80)/DISPOSAL(90)（`:19-21`），追加同名 260–290 致 Java 编译错误 + 基线隐瞒；(B2) `erp-ast/capitalization-source-type` 仅 INVENTORY(10)/CIP(20)，缺 DIRECT_PURCHASE，且计划误用 CIP_TRANSFER；(B3) `erp-ast/depreciation-schedule-status` 仅 PENDING/EXECUTED/REVERSED，缺 CANCELLED。S 级 nit：BizModel/IBiz 标(新)实为存根应(扩)、基线措辞、treasury 前置耦合过紧、N=3 api 暴露、模块计数、过账措辞。**已修订**：复用既有 70/80/90（DISPOSAL 单一类型按 disposalType 内部分支，不拆）；Phase 1 改为加列 + 加 DIRECT_PURCHASE(30)/CANCELLED(40) 字典选项（去业务类型追加项）；CIP_TRANSFER→CIP(20)；BizModel/IBiz 标(扩存根)；去 treasury 硬前置；executeBatchDepreciation 发布到 erp-ast-api 供 N=3。
- Independent draft review iteration 2: **needs revision**（`ses_0dfaa57b9ffe4qMYJOktn4xUXa`，独立 general 子代理）。2 残留 BLOCKER：(B1) Phase 1 Targets 行残留「`erp-fin-dao/.../ErpFinBusinessType.java`（扩 260–290）」与复用 70/80/90 决议矛盾；(B2 新) `disposalType=OBSOLETE` 不存在——`erp-ast/disposal-type` 实为 SCRAPPED(10)/SOLD(20)（OBSOLETE 属 disposal-reason 字典）。S 级 nit：Related 行残留 260–290、Deferred 残留 CIP_TRANSFER。**已修订**：Phase 1 Targets 去 ErpFinBusinessType 改为字典选项增量；OBSOLETE→SCRAPPED（baseline/Goals/Phase4/Proof 全改，asset-status SCRAPPED(40)/SOLD(50) + disposal-type SCRAPPED(10)/SOLD(20) 双字典核实）；Related/Deferred 去 260–290/CIP_TRANSFER 残留。
- Independent draft review iteration 3: **pass after reconciliation**（`ses_0df003478ffemmkuDZxnyC31Tc`，独立 general 子代理）。全部基线事实经实时仓库核实准确（业务类型 70/80/90 复用、字典缺口 DIRECT_PURCHASE/CANCELLED、disposal-type SCRAPPED(10)/SOLD(20)、asset-status SCRAPPED(40)/SOLD(50)、资产卡片/类别缺列、BizModel 空存根、过账 Facade/SPI/Registry 可行、折旧公式/残值/损益与 owner-doc 一致、assets→finance 单向无环）。1 残留 BLOCKER：plan 沿用 `CLOSED_FINAL` 期间状态值，但 `erp-fin/period-status` 实际为 `OPEN(10)/CLOSING(20)/CLOSED(30)/NEVER_OPENED(40)`，无 `CLOSED_FINAL`——按字面实现将引用不存在值。**已修订（实施时映射）**：所有 `CLOSED_FINAL` → `CLOSED(30)`，即折旧执行前校验 `ErpFinAccountingPeriod.status != OPEN(10)`（CLOSED=30 即拒绝，符合「已结账期间不允许补提折旧」）。S 级 nit：依赖措辞补 `erp-fin-service`（SPI 所在）；owner-doc §1.1/§7.1 仍列 DISPOSAL_SCRAP/DISPOSAL_SALE 旧业务类型（本计划已正确解析为单一 DISPOSAL(90)，owner-doc 过时行属后续）。结论：修订后可安全实施，无架构/编译/运行期阻塞。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [x] 范围内行为完成：资本化建卡/计划 + 三种折旧/批量 + 处置损益 + 四类凭证 + 残值约束 + 期间控制 + 红字冲减，行为测试通过
- [x] 相关文档对齐：`extended-roadmap.md` 2.5 标注 done；当日日志已记；`depreciation-and-posting.md` 偏离（减值/库存转固/nop-job Non-Goal）补注
- [x] 已运行验证：`mvn clean install -DskipTests` + `mvn test -pl module-assets/erp-ast-service -am`（改动模块）
- [x] 无范围内项目静默降级（减值/库存转固/nop-job/拆分合并/移动/报表均为计划内 Non-Goal）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控、日志一致
- [x] 保护区域（model/*.orm.xml）实施前已获人工批准
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 资产减值/重估（VALUE_ADJUSTMENT）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `ErpAstValueAdjustment` 实体存在但减值/重估不在工作项 2.5 标题内。
- Successor Required: yes（触发条件：减值测试需求启动时）

### 库存物料转固（IErpInvStockMoveBiz 出库）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 库存转固需跨域调 inventory 出库；本计划 sourceType 仅 DIRECT_PURCHASE(30) + CIP(20)。
- Successor Required: yes（触发条件：库存转固业务上线时）

### nop-job 定时自动折旧

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划提供手动 executeBatchDepreciation；期末结账（1000-3）可调用。nop-job 定时调度未接线。
- Successor Required: yes（触发条件：nop-job 接线时）

## Closure

Status Note: 计划 4 阶段全部完成（Phase 1 ORM 加列+字典+codegen 回归；Phase 2 资本化审批→建卡→折旧计划→CAPITALIZATION(80) 过账；Phase 3 折旧三策略计算/执行/批量+DEPRECIATION(70) 过账+残值约束+期间控制+幂等；Phase 4 处置审批→清理损益→DISPOSAL(90) 过账+资本化/折旧/处置红字冲减）。验证全绿：`mvn test -pl module-assets/erp-ast-service -am` = 19 tests/0 Failures/0 Errors；根 `mvn clean install -DskipTests` = BUILD SUCCESS（146 模块）。草案审查 iter3 pass-after-reconciliation（CLOSED_FINAL→OPEN(10)/CLOSED(30) 映射）。独立结束审计 verdict pass（无 BLOCKER）。Non-Goal 偏离补注 `depreciation-and-posting.md §十`；roadmap 2.5 标 ✅ done；日志 `docs/logs/2026/07-02.md`。

Closure Audit Evidence:

- Auditor / Agent: 独立 general 子代理 `ses_0debf4a97ffeznijO8lS2Gtbz5`（新会话，冷审；执行者未自我审计）
- Evidence: verdict `passes closure audit`——逐项核实 ORM/约定/过账集成/期间控制（无 CLOSED_FINAL）/测试 19 green/Goal 覆盖/一致性/无静默降级，全 PASS，无 BLOCKER。证据详见该会话返回。日志同日条目记录。

Follow-up:

- 资产减值/重估（见上方 Deferred）
- 库存物料转固（见上方 Deferred）
- nop-job 定时自动折旧（见上方 Deferred）
