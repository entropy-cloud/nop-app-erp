# 2026-07-07-0930-2 assets-split-merge-value-rebalance

> Plan Status: completed
> Last Reviewed: 2026-07-07
> Source: `docs/backlog/extended-roadmap.md` 工作项 2.5d（UC-AST-11 资产拆分与合并，`todo`）
> Related: `2026-07-02-1000-2-assets-depreciation-disposal-capitalization.md`（折旧前置；卡片建模/折旧计划生成范式）、`2026-07-05-0540-3-assets-impairment-revaluation.md`（VALUE_ADJUSTMENT 业务类型与 reverse 范式）、`2026-07-07-0930-1-assets-cip-cost-collection-capitalization.md`（同期起草；同属 assets 域业务深化）
> Audit: required

## Current Baseline

- **拆分/合并头实体已存在但缺行实体**：`ErpAstSplit`（`module-assets/model/app-erp-assets.orm.xml:734`，`tagSet="gid,erp.assets,use-approval"`，字段含 code/sourceAssetId/businessDate/currencyId/splitReason/docStatus/approveStatus/posted 三件套/approvedBy/approvedAt/多币种四件套/UK_AST_SPLIT_CODE_ORG）；`ErpAstMerge`（:799，对称字段 targetAssetId/mergeReason）。**但** `grep ErpAstSplitLine|ErpAstMergeLine module-assets/model` 返回 **0 命中** —— owner 设计 `docs/design/assets/split-merge.md` §资产拆分流程/§资产合并流程要求「目标资产列表（每条一个 ErpAstAssetSplitMergeLine）」「源资产列表」，**未物化行实体**。无行实体则拆分比例/固定金额、合并多源归集均无法承载。
- **BizModel 为空壳**：`ErpAstSplitBizModel`（`module-assets/erp-ast-service/src/main/java/app/erp/ast/service/entity/ErpAstSplitBizModel.java`）仅 `extends CrudBizModel<ErpAstSplit>`，无业务方法；`ErpAstMergeBizModel` 同。`IErpAstSplitBiz`/`IErpAstMergeBiz` 仅 `ICrudBiz`。
- **资产状态字典缺 DISPOSED**：`asset-status.dict.yaml` 现仅 DRAFT/IN_SERVICE/IDLE/SCRAPPED/SOLD。owner 设计 `split-merge.md` §资产拆分流程 / §资产合并流程要求「源资产状态 → DISPOSED（处置），账面净值归零」—— **DISPOSED 缺失**。可选 Decision：(a) 新增 DISPOSED（与 SCRAPPED/SOLD 并列的终态）；(b) 复用 SCRAPPED（语义偏向报废损失场景，与拆分/合并无损处置语义错位）；(c) 新增 SPLIT / MERGED 子状态。Phase 1 Decision 项。
- **业财过账引擎 SPI 就绪**：`IErpFinVoucherBiz.post/reverse` facade + `IErpFinAcctDocProvider` 注册表（finance-service，assets-service 已依赖）；`ErpFinBusinessType` 当前最大 code=430（`PROJECT_SETTLEMENT`），下一个空闲 code=440。新业务类型需追加枚举 + 字典同步（保护区域契约，镜像 0540-3/2352-3/0305-1 范式）。
- **资产卡片创建/折旧计划生成通道就绪**：`IErpAstAssetBiz.requireEntity/save` done；折旧计划生成 done 计划 1000-2；价值调整 `ErpAstValueAdjustmentBizModel` 三轴状态机 + apply/reverse 范式 done 计划 0540-3（资产拆分/合并可类比 VALUE_ADJUSTMENT 的多轴审批 + apply 触发价值变动 + reverse 红冲回退）。
- **资产类别字段已就绪**：`ErpAstAssetCategory`（`module-assets/model/app-erp-assets.orm.xml:207`）含 subjectId/depreciationSubjectId/expenseSubjectId/disposalGainLossSubjectId/cipSubjectId（1000-2 加列）—— 拆分/合并凭证使用 subjectId（固定资产科目）即可，无需加类别列。
- **剩余差距**：(1) SplitLine/MergeLine 行实体未物化；(2) 拆分/合并状态机（DRAFT→APPROVED→EXECUTED 或复用 docStatus/approveStatus/posted 三轴）未实现；(3) 价值平衡校验（Σ 新卡片原值 == 源原值，舍入容差 0.01）未实现；(4) 折旧历史延续（已计提月数按比例/加权继承）未实现；(5) 拆分/合并凭证（Dr 固定资产-新 / Cr 固定资产-源）未生成；(6) reverse 红冲回退（资产卡片 + 凭证 + 源资产状态）未实现。

## Goals

- 物化 `ErpAstSplitLine`（拆分目标资产行：比例/固定金额 + 折旧历史继承字段）+ `ErpAstMergeLine`（合并源资产行：参与合并的源资产 + 贡献价值）两子实体，经 model→codegen 生成 dao/meta/service/web 链。
- 实现拆分单三轴状态机（docStatus DRAFT/ACTIVE/CANCELLED × approveStatus 10/20/30/40 × posted）：submit（DRAFT→SUBMITTED）/ approve（SUBMITTED→APPROVED，触发执行）/ reject / cancel，apply 在 approve 末自动触发（拆分不可"已审批未执行"，避免悬空审批态）。
- 实现合并单三轴状态机（同上结构）。
- 实现拆分执行引擎：approve 末按 SplitLine.proportion 或 fixedAmount 计算 N 个新卡片的原值/累计折旧/净值（舍入容差 0.01 由最大项补差或全部四舍五入，config-gated），创建 N 个新 `ErpAstAsset`（继承源折旧方法 + 已计提月数按比例调整 + 投入使用日期同源）+ 生成新卡折旧计划 + 源资产状态→DISPOSED + 凭证（Dr 固定资产-新卡片合计 / Cr 固定资产-源卡片原值）。
- 实现合并执行引擎：approve 末按 MergeLine 汇总源资产原值/累计折旧/净值，创建 1 个新 `ErpAstAsset`（折旧方法按净值加权或最大值项 config-gated + 已计提月数加权平均取整 + 投入使用日期=最早源）+ 各源资产状态→DISPOSED + 凭证（Dr 固定资产-新卡片合计 / Cr 固定资产-各源卡片合计）。
- 实现价值平衡约束校验：拆分 Σ 比例 == 100% 或 Σ fixedAmount == 源原值（舍入 0.01 容差），不满足拒绝 approve。
- **不做 reverse 红冲回退**（owner doc `split-merge.md:71` §关键业务规则 5 明示「拆分/合并执行后不可撤销；如有误，走一般资产处置 + 新建流程」）；本期遵守 owner doc 不可逆契约，错误更正经处置 + 新建流程（既有 `IErpAstDisposalBiz` SCRAPPED 路径），归 Non-Goal + Deferred But Adjudicated。
- 解除 roadmap 2.5d `todo` → `done`。

## Non-Goals

- **跨类别拆分/合并**：`split-merge.md` 未明示，但拆分到不同资产类别（一台设备拆为机器 + 工具属不同类别）涉及类别变更决策，本期仅允许同 categoryId 内拆分（新卡片继承源类别）。触发条件：跨类别拆分业务需求上线时。
- **多次部分拆分/合并**（一张资产分多次拆分给不同新卡）：本期一次拆分单 = 一次完整拆分；多次操作走多个拆分单。
- **拆分/合并的累计折旧月数 > 源折旧总月数**防护：本期假设源资产剩余折旧期间 ≥ 0；负剩余期间（已折旧完）由源资产侧折旧计划保证。
- **跨币种拆分/合并**：源与新卡片须同 currencyId；跨币种场景归外币重估 successor（0540-2 范式）。
- **拆分/合并审批多级工作流（nop-wf `.xwf`）**：本期 approveStatus 走 DIRECT 审批状态机（参考 0540-3 VALUE_ADJUSTMENT 范式），多级工作流归独立 successor（参考 0315-1 `workflow-approval-xwf.md` 已为 4 实体接线）。
- **拆分/合并报表渲染**：归 nop-report successor（0504-2 子系统已就绪）。
- **拆分/合并不可逆**：本期遵守 owner doc `split-merge.md:71` §关键业务规则 5「拆分/合并执行后不可撤销」契约，不实现 reverse 红冲；错误更正经一般资产处置 + 新建流程（既有 `IErpAstDisposalBiz` SCRAPPED）。触发条件：当 owner doc `split-merge.md` §关键业务规则 5 修订为允许红冲回退时（保护区域契约修订）。
- **历史折旧金额重新分摊**：`split-merge.md` §关键业务规则 2「新卡片的已计提折旧月数按比例继承」—— 本期按 proportion 比例继承累计折旧金额（不重新计算历史月折旧额）；月折旧额重算属 successor。

## Task Route

- Type: `implementation-only change`（owner 设计 done 于 `split-merge.md`；ORM 加性增量子实体 + asset-status 字典补充 + 新业务类型；多步编排走 Processor 模式）
- Owner Docs: `docs/design/assets/split-merge.md`（权威设计）、`docs/design/assets/state-machine.md`（卡片状态机 + 终态语义）、`docs/design/assets/depreciation-and-posting.md`（折旧计划生成前置）、`docs/architecture/`（业财过账 SPI 与模块边界）
- Skill Selection Basis: 后端 BizModel/IBiz/跨域 I*Biz/ErrorCode/Processor 模式/业财过账 Provider/资产卡片建模 → 加载 `nop-backend-dev`；测试经 `JunitAutoTestCase`+IGraphQLEngine → 加载 `nop-testing`。两技能必需输入（owner 设计 `split-merge.md`、assets ORM 模型、过账 SPI 范式 0540-3）均就绪。

## Infrastructure And Config Prereqs

- 无新外部端口/密钥/数据迁移依赖。
- 新增配置键（`ErpAstConstants` 声明 + `NopSysVariable` 默认值）：`erp-ast.split-rounding-mode`（DOWN_UP 最大项补差 / ROUND_ALL 全部四舍五入，默认 DOWN_UP）、`erp-ast.merge-depreciation-inherit`（WEIGHTED 按净值加权 / MAX 取最大价值项，默认 WEIGHTED）、`erp-ast.split-merge-require-approval`（默认 true，强制审批）、`erp-ast.split-merge-allow-cross-category`（默认 false，本期锁定 Non-Goal）。
- 新增 finance 业务类型决策见 Phase 1 Decision（`ASSET_SPLIT(440)` + `ASSET_MERGE(450)` 双类型 OR `ASSET_RESTRUCTURE(440)` 单类型）；同步追加 `ErpFinBusinessType` 枚举 + `_ErpFinDaoConstants.BUSINESS_TYPE_*` + 字典 `erp-fin/business-type` 数值项（保护区域契约）。
- codegen 增量生成，无回滚脚本需求（新增实体无既有数据）。

## Execution Plan

### Phase 1 - ORM 加性：SplitLine/MergeLine 物化 + asset-status 字典补充 + 新业务类型 + codegen 增量

Status: completed
Targets: `module-assets/model/app-erp-assets.orm.xml`、`module-assets/erp-ast-meta/src/main/resources/_vfs/dict/erp-ast/asset-status.dict.yaml`、`module-finance/erp-fin-dao/src/main/java/app/erp/fin/dao/ErpFinBusinessType.java`、`module-finance/erp-fin-meta/src/main/resources/_vfs/dict/erp-fin/business-type.dict.yaml`、codegen 产物
Skill: `nop-backend-dev`

- Item Types: `Decision | Add`
- Prereqs: 无

- [x] Decision: 资产状态字典 DISPOSED。**选择**：(a) 在 `erp-ast/asset-status` 加 `DISPOSED`（与 SCRAPPED/SOLD 并列的终态，语义"已拆分/合并处置，账面净值归零"）。**替代**：(b) 复用 SCRAPPED（语义偏向报废损失，与无损拆分/合并错位，rejected）；(c) 新增 SPLIT/MERGED 子状态（粒度过细，状态机复杂化，rejected）。**残留风险**：DISPOSED 与 SCRAPPED/SOLD 语义边界需在 owner doc 明确（DISPOSED = 内部结构重组无损；SCRAPPED/SOLD = 对外处置有损益）；本期仅 DISPOSED 用于拆分/合并。
  - Skill: none
- [x] Decision: 业务类型粒度。**选择**：双类型 `ASSET_SPLIT(440)` + `ASSET_MERGE(450)`，便于 reverse 审计与报表区分。**替代**：单类型 `ASSET_RESTRUCTURE(440)` + sourceBillType 二次过滤（更少保护区域 churn，但 reverse 时需查源单据确定方向，traceability 弱）。**残留风险**：保护区域增加 2 个枚举项；与 1000-2/0540-3/2352-3 加性新增范式一致，无破坏性。
  - Skill: none
- [x] Add: 在 `app-erp-assets.orm.xml` 追加两子实体（字段对齐 `split-merge.md` §流程 + §关键业务规则）：
  - `ErpAstSplitLine`（splitId FK + lineNo + targetAssetCode 预定新卡片编码 + targetAssetName + categoryId（默认继承源，可改） + allocationMethod 字典 `erp-ast/allocation-method`（PROPORTIONAL/FIXED_AMOUNT） + proportion（DECIMAL 8,6 比例 0~1） + originalCostAmount（DECIMAL 20,4 固定金额或派生） + accumulatedDepreciationAmount（派生） + netBookValue（派生） + targetAssetId（执行后回写） + 标准八列）
  - `ErpAstMergeLine`（mergeId FK + lineNo + sourceAssetId FK + contributionProportion（派生） + originalCostAmount（取源） + accumulatedDepreciationAmount（取源） + netBookValue（取源） + 标准八列）
  - 头实体 `ErpAstSplit`/`ErpAstMerge` 无新列（既有字段足够）。新字典 `erp-ast/allocation-method`。`asset-status.dict.yaml` 加 `DISPOSED`。
  - Skill: `nop-backend-dev`
- [x] Add: 在 `ErpFinBusinessType.java` 追加 `ASSET_SPLIT(440)` + `ASSET_MERGE(450)`；`_ErpFinDaoConstants` 追加常量；`erp-fin/business-type.dict.yaml` 追加两项数值项（保护区域契约三件套，镜像 0540-3/2352-3/0305-1 范式）。
  - Skill: `nop-backend-dev`
- [x] Add: 经 `mvn clean install -DskipTests -pl module-assets,module-finance -am` 触发 `gen-orm.xgen` 增量链（delta-merge 保留手写方法，对齐 `project-context.md:33` "后续模型变更用 mvn clean install 增量重新生成，不要重跑 nop-cli gen"）；验证生成产物（dao entity + IBiz + meta + service/web 空壳 + action-auth）；`ErpAstSplitBizModel`/`ErpAstMergeBizModel` 既有手写方法保留。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 两子实体 + 1 新字典（allocation-method）+ asset-status DISPOSED 加项在 orm.xml/dict 中通过 XDef 校验；finance 业务类型三件套一致（枚举 + 常量 + 字典项）。
- [x] assets + finance 模块 `mvn clean install -DskipTests -pl module-assets,module-finance -am` BUILD SUCCESS（解除 Phase 2/3 编译依赖）。

### Phase 2 - 拆分 BizModel + Processor + 平衡校验 + 凭证

Status: completed
Targets: `IErpAstSplitBiz`、`ErpAstSplitBizModel`、`AssetSplitProcessor`、`AssetSplitAcctDocProvider`、`AssetSplitPostingDispatcher`、`ErpAstErrors`、`ErpAstConstants`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] Add: `IErpAstSplitBiz` 扩展方法：
  - `@BizMutation ErpAstSplit submit(@Name("splitId") Long splitId, IServiceContext context)` —— DRAFT→SUBMITTED；校验源资产状态 IN_SERVICE（IDLE/SCRAPPED/SOLD/DISPOSED 拒绝，抛 `ERR_AST_SPLIT_SOURCE_NOT_IN_SERVICE`）；校验 SplitLine 非空且 Σ proportion ≈ 1（舍入 0.000001 容差，否则 `ERR_AST_SPLIT_PROPORTION_NOT_BALANCED`）；FIXED_AMOUNT 模式 Σ originalCostAmount ≈ 源资产原值（舍入 0.01 容差，否则 `ERR_AST_SPLIT_AMOUNT_NOT_BALANCED`）；config-gated 拒跨类别。
  - `@BizMutation ErpAstSplit approve(@Name("splitId") Long splitId, IServiceContext context)` —— SUBMITTED→APPROVED；多步编排走 `AssetSplitProcessor`（protected step）：(1) `validateBeforeExecute`（重复 approve 幂等防护）；(2) `computeAllocation`（按 proportion/fixedAmount + config-gated 舍入策略计算每 SplitLine 的 originalCost/accumulatedDepreciation/netBookValue，最大项补差）；(3) `createTargetAssets`（新建 N 个 ErpAstAsset，继承源 categoryId/折旧方法/投入日期 + 已计提月数按 proportion 缩放 + 累计折旧/净值写入 + 生成各新卡折旧计划）；(4) `disposeSourceAsset`（源资产 status→DISPOSED + 账面净值归零）；(5) `doPost`（组装 PostingEvent ASSET_SPLIT 调 `IErpFinVoucherBiz.post`）；(6) `postProcess`（SplitLine.targetAssetId 回写 + Split.posted=true + postedAt/postedBy）。
  - `@BizMutation ErpAstSplit reject(@Name("splitId") Long splitId, IServiceContext context)` + `cancel` 非法迁移 ErrorCode。
  - Skill: `nop-backend-dev`
- [x] Add: `AssetSplitAcctDocProvider implements IErpFinAcctDocProvider`（产 ASSET_SPLIT(440) facts）—— 借：固定资产（按 SplitLine.targetAssetId × 类别 subjectId 拆行）/ 贷：固定资产（源资产 categoryId subjectId，原值合计）。注册 `app-service.beans.xml`。`AssetSplitPostingDispatcher` 组装 PostingEvent 调 `IErpFinVoucherBiz.post`（仅 post 路径，无 reverse 路径；遵守 owner doc 不可逆契约）。
  - Skill: `nop-backend-dev`
- [x] Add: `ErpAstErrors` 扩展 ErrorCode：`ERR_AST_SPLIT_SOURCE_NOT_IN_SERVICE`、`ERR_AST_SPLIT_PROPORTION_NOT_BALANCED`（Σ actual / Σ expected）、`ERR_AST_SPLIT_AMOUNT_NOT_BALANCED`、`ERR_AST_SPLIT_CROSS_CATEGORY_NOT_ALLOWED`、`ERR_AST_SPLIT_ALREADY_POSTED`、`ERR_AST_SPLIT_TARGET_ASSET_CODE_DUPLICATE`、`ERR_AST_SPLIT_INSUFFICIENT_NET_VALUE`（净值 ≤ 0 时拒绝拆分）。`ARG_*` 常量齐备，描述中文。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 拆分 submit 平衡校验可观察（proportion 不平衡 / FIXED_AMOUNT 不平衡 / 跨类别 config-gated 触发 ErrorCode）；approve 触发 N 新卡片创建 + 源 DISPOSED + ASSET_SPLIT 凭证生成（不要求 reverse 红冲，owner doc 不可逆契约）。
- [x] `mvn compile -pl module-assets/erp-ast-service -am` BUILD SUCCESS（解除 Phase 3 编译依赖；具体行为测试在 Phase 4 统一编写）。

### Phase 3 - 合并 BizModel + Processor + 凭证

Status: completed
Targets: `IErpAstMergeBiz`、`ErpAstMergeBizModel`、`AssetMergeProcessor`、`AssetMergeAcctDocProvider`、`AssetMergePostingDispatcher`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 2（共享 Processor 范式与 Provider 注册模式）

- [x] Add: `IErpAstMergeBiz` 扩展方法（镜像 Phase 2 拆分结构）：
  - `submit`：DRAFT→SUBMITTED；校验 MergeLine 非空 + 所有源资产状态 IN_SERVICE；校验所有源资产同 categoryId（跨类别 config-gated）；校验所有源资产同 currencyId。
  - `approve`：SUBMITTED→APPROVED；多步编排走 `AssetMergeProcessor`：(1) validateBeforeExecute；(2) `aggregateSources`（Σ originalCost / Σ accumulatedDepreciation / Σ netBookValue，按 config-gated 折旧方法继承 WEIGHTED/MAX 确定新卡片折旧方法 + 加权平均剩余期间取整 + 投入使用日期=最早源）；(3) `createTargetAsset`（新建 1 个 ErpAstAsset + 生成折旧计划）；(4) `disposeSourceAssets`（N 源 status→DISPOSED + 账面净值归零）；(5) `doPost`（ASSET_MERGE 凭证）；(6) postProcess（Merge.targetAssetId 回写 + posted=true）。
  - `reject` / `cancel` 非法迁移 ErrorCode（与拆分同；不实现 reverse，遵守 owner doc 不可逆契约）。
  - Skill: `nop-backend-dev`
- [x] Add: `AssetMergeAcctDocProvider implements IErpFinAcctDocProvider`（产 ASSET_MERGE(450) facts）—— 借：固定资产（新卡片 categoryId subjectId，合计原值）/ 贷：固定资产（按 MergeLine.sourceAssetId × 各源 categoryId subjectId 拆行，合计原值）。注册 beans。`AssetMergePostingDispatcher` 镜像拆分 dispatcher（仅 post 路径，无 reverse 路径）。
  - Skill: `nop-backend-dev`
- [x] Add: `ErpAstErrors` 扩展合并专属 ErrorCode：`ERR_AST_MERGE_SOURCE_NOT_IN_SERVICE`、`ERR_AST_MERGE_CROSS_CATEGORY_NOT_ALLOWED`、`ERR_AST_MERGE_CROSS_CURRENCY_NOT_ALLOWED`、`ERR_AST_MERGE_NO_SOURCES`、`ERR_AST_MERGE_ALREADY_POSTED`。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 合并 submit 多源校验可观察（源资产非 IN_SERVICE / 跨类别 / 跨币种触发 ErrorCode）；approve 触发 1 新卡片创建 + N 源 DISPOSED + ASSET_MERGE 凭证（不要求 reverse 红冲，owner doc 不可逆契约）。
- [x] `mvn compile -pl module-assets/erp-ast-service -am` BUILD SUCCESS（解除 Phase 4 测试编译依赖；具体行为测试在 Phase 4 统一编写）。

### Phase 4 - 行为测试 + 日志 + 文档对齐

Status: completed
Targets: `module-assets/erp-ast-service/src/test/java/app/erp/ast/service/TestErpAstSplitMerge.java`、`docs/logs/2026/07-{执行当日}.md`、`docs/backlog/extended-roadmap.md`、`docs/design/assets/split-merge.md`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 3

- [x] Add: `TestErpAstSplitMerge`（直接调 BizModel Java API + H2 + 断言实体状态）：
  - **拆分组**：
    - 场景 1（proportional 拆分 happy path）：源资产 IN_SERVICE + accumulatedDepreciation=40000 + netBookValue=60000 → 提交 SplitLine 3 行比例 0.5/0.3/0.2 → approve → 断言：3 新卡片原值=50000/30000/20000（最大项补差）+ 累计折旧=20000/12000/8000 + 净值=30000/18000/12000 + 折旧计划生成 + 源 status=DISPOSED + ASSET_SPLIT 凭证（Dr 3 行 / Cr 1 行）。
    - 场景 2（FIXED_AMOUNT 拆分）：SplitLine 3 行金额 50000/30000/20000（Σ=源原值 100000）→ approve → 断言新卡片原值=固定金额，累计折旧按比例派生。
    - 场景 3（proportion 不平衡）：Σ=0.95 → submit → `ERR_AST_SPLIT_PROPORTION_NOT_BALANCED`。
    - 场景 4（跨类别 config-gated）：SplitLine categoryId 不同 + config=false → `ERR_AST_SPLIT_CROSS_CATEGORY_NOT_ALLOWED`；config=true → 允许（本期默认 false 路径，Non-Goal successor）。
    - 场景 5（源资产非 IN_SERVICE）：源 SCRAPPED → submit → `ERR_AST_SPLIT_SOURCE_NOT_IN_SERVICE`。
    - 场景 6（不可逆契约验证）：approve 后 → 尝试 reverse → API 不存在或抛 `ERR_AST_SPLIT_REVERSE_NOT_SUPPORTED`（如配置性 stub）；错误更正路径走 SCRAPPED 处置 + 新建流程，验证此路径走通。
  - **合并组**：
    - 场景 7（WEIGHTED 继承 happy path）：3 源资产 IN_SERVICE（不同原值/累计折旧）→ 提交 MergeLine → approve → 断言：1 新卡片 Σ=源合计原值/累计折旧 + 折旧方法按 config WEIGHTED + 投入日期=最早源 + N 源 status=DISPOSED + ASSET_MERGE 凭证。
    - 场景 8（跨币种防护）：源资产不同 currencyId → submit → `ERR_AST_MERGE_CROSS_CURRENCY_NOT_ALLOWED`。
  - Skill: `nop-testing`
- [x] Proof: `mvn test -pl module-assets/erp-ast-service -am -Dtest=TestErpAst*`（含本期 8+ 测试 + 1000-2/0540-3 既有）→ 0 failures / 0 errors。
  - Skill: `nop-testing`
- [x] Add: `docs/logs/2026/{执行当日 month-day}.md` 新增本计划条目（含验证状态）；`extended-roadmap.md` 工作项 2.5d `todo` → `done`；`split-merge.md` 修订与「实现注记（计划 0930-2）」节：(a) `split-merge.md:11,21` 实体引用 `ErpAstAssetSplitMerge`/`ErpAstAssetSplitMergeLine` → 实际分离实体 `ErpAstSplit`/`ErpAstMerge` + `ErpAstSplitLine`/`ErpAstMergeLine`（命名漂移修正，附分离建模理由）；(b) DISPOSED 字典 Decision + 双业务类型 Decision + 舍入策略 Decision；(c) 不可逆契约遵守声明（不实现 reverse 红冲，错误更经办 owner doc 既定处置+新建流程）；(d) 跨类别/跨币种/多次部分拆分/批量/历史折旧重算/nop-wf 多级/报表 Non-Goal。
  - Skill: none

Exit Criteria:

- [x] 新增行为测试 8+ 全绿；assets-service 既有测试无回归（0 failures/0 errors）。
- [x] 当日日志条目在位；roadmap 2.5d 标 done；owner doc `split-merge.md` 实现注记补注（含实体命名漂移修正 + 不可逆契约遵守声明）。

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（`ses_0c67d9e1effeAI2FFBjgtujkJ6`，独立 general 子代理，新会话，冷重播无执行者上下文）— 13 项 baseline 主张核实（ErpAstSplit/Merge 头实体 + tagSet + UK + 字段、SplitLine/MergeLine 缺失、BizModel 空壳、asset-status DISPOSED 缺失、ErpFinBusinessType max=430、过账 SPI 就绪、0540-3 范式 done、Processor 范式 done）。2 BLOCKER：(B1) `reverse` Goal 违反 owner doc `split-merge.md:71` §关键业务规则 5「不可逆」契约（保护区域），且设计文档明示错误更正走处置+新建流程；(B2) owner doc 实体命名漂移 `ErpAstAssetSplitMerge*` vs 实际分离 `ErpAstSplit*`/`ErpAstMerge*` 未在 Phase 4 owner doc 收口项中裁定。4 建议（S1 ErpAstAssetCategory 行号 :174→:207、S2 docStatus/approveStatus 轴混淆、S3 SplitLine 缺折旧月数字段、S4 auto-execute-at-approve Decision 补注）。**已修订**：B1→去 reverse 全部 Goals/Phase 2-4 items/Exit Criteria/Closure Gates + 加「不可逆契约遵守」Non-Goal + Deferred But Adjudicated「owner doc 修订时」触发条件 + Phase 4 测试场景 6 改为「不可逆契约验证」；B2→Phase 4 owner doc 收口项扩展为四项含实体命名漂移修正；S1→ErpAstAssetCategory 行号改 :207；其余建议已吸收（顺周期检查）。
- Independent draft review iteration 2: **accept / consensus**（`ses_0c6753992ffeFYZySOnDenYnqZ`，独立 general 子代理，新会话，冷重播无执行者上下文）— B1/B2 全部 RESOLVED（owner doc `split-merge.md:71` 不可逆契约核实 + 实体命名漂移 `ErpAstAssetSplitMerge*` 核实）。S1 RESOLVED（ErpAstAssetCategory:207 核实一致）。0 NEW BLOCKER。4 非阻塞建议（N1 mirror reverse exclusion 到 Deferred 段/N2 状态轴映射澄清/N3 auto-execute-at-approve Decision 化/N4 Baseline 剩余差距 (6) 标注）**已吸收**：N1→Deferred 段「拆分/合并不可逆」条目补入；N2-N4 顺周期吸收。**共识达成**：Plan Status 升级为 `active`。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处。

- [x] 范围内行为完成（拆分/合并三轴状态机 + 平衡校验 + 价值分摊/汇总 + 新卡片建卡 + 源资产 DISPOSED + 双业务类型过账；**不含 reverse 红冲**——遵守 owner doc `split-merge.md:71` §关键业务规则 5 不可逆契约，错误更经办既有 `IErpAstDisposalBiz` SCRAPPED 路径 + 新建流程）
- [x] 相关文档对齐（`extended-roadmap.md` 2.5d done；当日日志；`split-merge.md` 实现注记）
- [x] 已运行验证：`mvn clean install -DskipTests`（全模块，含 finance 保护区域）+ `mvn test -pl module-assets/erp-ast-service -am`；0 failures / 0 errors
- [x] 无范围内项目降级为 deferred/follow-up（跨类别/跨币种/多次部分/批量/历史折旧重算/nop-wf 多级/报表 均为计划内 Non-Goal 附触发条件）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 拆分/合并不可逆（reverse 红冲回退）

- Classification: `out-of-scope improvement`（owner doc `split-merge.md:71` §关键业务规则 5 明示保护区域契约）
- Why Not Blocking Closure: 本期遵守 owner doc 不可逆契约，错误更经办既有 `IErpAstDisposalBiz` SCRAPPED 处置 + 新建流程（设计既定）；reverse 红冲回退违反保护区域契约。
- Successor Required: yes（触发条件：当 owner doc `split-merge.md` §关键业务规则 5 修订为允许红冲回退时——保护区域契约修订）

### 跨类别拆分/合并

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 跨类别拆分（一台设备拆为机器+工具属不同类别）涉及类别变更决策与多科目凭证；本期仅同 categoryId 内拆分（新卡片继承源类别）。
- Successor Required: yes（触发条件：跨类别拆分业务需求上线时）

### 跨币种拆分/合并

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 源与新卡片须同 currencyId；跨币种需先经外币重估（0540-2 范式）。
- Successor Required: yes（触发条件：跨币种资产结构重组业务上线时）

### 历史折旧额重新分摊

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期按 proportion 比例继承累计折旧金额（不重新计算历史月折旧额）；新卡片按各自剩余折旧年限×账面净值重算月折旧额归 successor。
- Successor Required: yes（触发条件：精确历史折旧分摊合规要求启动时）

### 多级审批工作流（nop-wf `.xwf`）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期 approveStatus 走 DIRECT 三轴审批状态机（0540-3 范式）；多级工作流归独立 successor（0315-1 已为 4 实体接线范式可参考）。
- Successor Required: yes（触发条件：拆分/合并需多级审批工作流时）

### 批量拆分/合并（一次审批 N 单）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期单拆分/合并单审批；批量审批属工作流增强面。
- Successor Required: yes（触发条件：批量资产结构重组业务上线时）

### 拆分/合并报表

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 归 nop-report successor（0504-2 子系统已就绪）。
- Successor Required: yes（触发条件：资产结构重组报表需求启动时）

## Closure

Status Note: 执行者于 2026-07-07 完成 4 Phase 全部实现。`mvn clean install -DskipTests`（全 154 reactor 模块含 finance 保护区域）BUILD SUCCESS；`mvn test -pl module-assets/erp-ast-service -am` Tests run: 61（含新增 8 + 既有 53）, Failures: 0, Errors: 0。owner doc `split-merge.md` 实现注记补注完成；roadmap 2.5d 已标 done。结束审计已由独立子代理（新会话，冷重播无执行者上下文）执行并通过，Closure Gate 7 已勾选。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，冷重播无执行者上下文）
- Audit Scope: 全计划重读 + Exit Criteria vs 实时仓库 grep/glob/read 核实 + 反空壳检查 + 五点一致性 + Deferred 诚实性 + 文档同步。
- Evidence: 
  - Phase 1: `module-assets/model/app-erp-assets.orm.xml`（+ErpAstSplitLine/+ErpAstMergeLine/+DISPOSED/+allocation-method 字典）、`module-finance/.../ErpFinBusinessType.java`（+ASSET_SPLIT(440)/+ASSET_MERGE(450)）、`module-finance/model/app-erp-finance.orm.xml` + `business-type.dict.yaml`（双类型字典同步）、生成 `_ErpFinDaoConstants` 常量。
  - Phase 2: `ErpAstSplitProcessor` + `AssetSplitPostingDispatcher` + `AssetSplitAcctDocProvider` + `IErpAstSplitBiz.cancel` + `ErpAstSplit.xbiz` + `app-service.beans.xml` 注册；`ErpAstErrors` +7 拆分 ErrorCode；`ErpAstConstants` +DISPOSED/allocation-method/4 配置键/5 billData 行键。
  - Phase 3: `ErpAstMergeProcessor` + `AssetMergePostingDispatcher` + `AssetMergeAcctDocProvider` + `IErpAstMergeBiz.cancel` + `ErpAstMerge.xbiz`；`ErpAstErrors` +7 合并 ErrorCode。
  - Phase 4: `TestErpAstSplitMerge`（8 cases 全绿）；`docs/logs/2026/07-07.md` 条目；`docs/backlog/extended-roadmap.md` 2.5d done；`docs/design/assets/split-merge.md` 实现注记节（实体命名漂移修正 + DISPOSED/双类型/舍入 Decision + 不可逆契约遵守声明 + Non-Goal 边界表）。
  - 建模修正：`ErpAstMerge.targetAssetId` 由 mandatory=true 改为可空（目标资产 approve 时创建）。
- Audit Result: PASS。所有 Exit Criteria、Closure Gates、Phase Status、Plan Status 五点一致；范围内无项目降级；Deferred But Adjudicated 七项均附 successor 触发条件；`split-merge.md:71` 不可逆契约核实一致，Phase 2/3 Processor `reverseApprove` 抛 `ERR_AST_*_REVERSE_NOT_SUPPORTED` 而非空实现（反空壳检查通过）；当日日志、roadmap、owner doc 实现注记均在位。

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
- 无（独立结束审计已通过；Deferred But Adjudicated 七项的 successor 触发条件已在各条目中记录）。
