# 2026-08-14-2304-1-rc-mr1-r1-25-crm-forecast-territory-tier RC-R1.25 — crm Forecast 区域 tier rollup（MR1 第一批纯预授权）

> Plan Status: completed
> Last Reviewed: 2026-08-15
> Mission: requirement-compliance
> Work Item: RC-R1.25（P1-RC-039 crm UC-CRM-10 ForecastAggregator 缺 territory 级 rollup，4 级层次「团队 → 区域 → 公司」只实现 3 级）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.25 行 + `docs/audits/arm-index.md` P1-RC-039 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`（纯 BizModel）
> Related: `docs/design/crm/use-cases.md`（L1 UC-CRM-10）；`docs/design/crm/sales-forecast.md`（§业务规则 层级聚合 :108-121 + 实现约定 :221）；`docs/design/crm/territory.md`；`docs/audits/2026-08-07-2345-rc-ma4-a4-2-83-96-crm-lead-lifecycle-marketing-forecast-runtime.md`（A4.2.93 运行时证据）；`docs/plans/2026-08-14-1815-1-rc-mr1-r1-21-22-crm-conversion-family.md`（同批范式参照）
> Audit: required

## Current Baseline

- **finding P1-RC-039（arm-index 行，UC-CRM-10 Forecast territory tier rollup 缺失）**：L1（`use-cases.md:228`）逐字「触发上级层级聚合（团队 → 区域 → 公司）」。L3 实仓：`ForecastAggregator.refreshForecast:50-102` **仅 3 级 rollup**——个人（按 ownerId 分组 `:61-91`）+ 团队（按 teamId Σ `:93-97`）+ 公司（全 Σ `:99-101`）；类头注释 `:36` 字面声明「个人（ownerId 非空）→ 团队（teamId 非空、ownerId 空）→ 公司（均为空）」**无 territory 级**；`buildForecast:119-135` 不 setTerritoryId；无 territory 子树聚合。`sales-forecast.md:221` 实现约定 Deferred 声明「区域（territory）层级因 Lead ORM 无 territoryId 直接关联暂未实现，触发条件：Lead→Territory 映射就绪时」——**触发条件现已满足**：`ErpCrmLead.territoryId` propId 41 已落地（orm.xml:214）+ to-one territory + `ErpCrmTerritory` 树（parentId 自引用 orm.xml:990）+ `ErpCrmForecast.territoryId` propId 4 已存在（orm.xml:821）。A4.2.93 运行时证实 ForecastAggregator 仅 3 级 rollup + buildForecast 从不 setTerritoryId → territory 级管道 `accumulatePipeline`（`QuotaRollupCalculator:180-199` 按 territoryId 子树 in 查询 ErpCrmForecast）Forecast 段恒返回空。§2 P1①（功能完全缺失）+ §4 三判据复核重开（Deferred 无人工批准痕迹 + 触发条件已满足）。**非 P0**（commit/upside/bestCase 主路径数值正确，仅缺 territory 级聚合精度；CRM 域不产生会计凭证）。
- **实仓（HEAD 核查）**：
  - `ForecastAggregator.refreshForecast:50-102`：个人汇总 `:77-91`（**`ownerId == null` 商机在 `:65-66` 被跳过，不进入任何 totals**）→ teamTotals/companyTotals；团队 rollup `:93-97`；公司 rollup `:99-101`。`ForecastTotals` 累加器 `:339-379` 支持 `add(ForecastTotals)` 合成。
  - `ErpCrmForecast` 已有 `territoryId` 列（propId 4, orm.xml:821）+ to-one territory（:843）+ 索引（:854-856，索引块 :850-866）；`ErpCrmForecastLine` 为商机级快照（按 forecastId 关联，无 territoryId 列，:870-904）。
  - `ErpCrmLead.territoryId`（propId 41, orm.xml:214）+ to-one territory（:237）+ 索引（:274）。
  - `ErpCrmTerritory` 树实体（orm.xml:964-990，parentId 自引用 to-one :990）；`QuotaRollupCalculator.collectSubtreeIds:220-227` 递归子树收集既有范式（**镜像参照**，注意无防环——本行实现可加 visited set，属合理强化）；`QuotaRollupCalculator.rollup:44-113` + `accumulatePipeline:163-216` 子树聚合查询范式（**镜像参照**；**Forecast 段 `:185-199` 无 periodId/periodLabel 过滤**——测试断言须依赖单期间测试 DB）。
  - `ErpCrmForecastBizModel.refreshForecast:31-32` → `ErpCrmForecastRefreshForecastProcessor:17-18` → `ForecastAggregator.refreshForecast`（单入口）；`ErpCrmForecastRecalcJob:69` 亦经 `IErpCrmForecastBiz#refreshForecast`（同入口，自动覆盖）。
  - 测试基线：`TestErpCrmForecastAndScoring.java`（5 @Test：:66 testScoringAndAutoQualify / :119 testNoActiveConfigReturnsNull / **:133 testRefreshForecastAndRollup（:133-181 断言个人 userA/userB + 团队 + ForecastLine，无公司行断言）** / :184 testFrozenRejectsRefresh / :203 testClosePeriodTriggersAccuracy）；`TestErpCrmForecastRecalcJob.java`（job 层）。
- **预授权判据**（第一批纯预授权）：纯 BizModel（`ForecastAggregator` 聚合逻辑）+ 测试，**不触 ORM 结构/会计过账/删除**（ErpCrmForecast.territoryId 列已存在，零结构变更）；roadmap RC-R1.25 行 `todo`，Deps（R1.0 done）已满足。
- **涉及文件**：`module-crm/erp-crm-service/src/main/java/app/erp/crm/service/support/ForecastAggregator.java`；测试 `TestErpCrmForecastAndScoring.java` 扩展或新增测试类；`docs/design/crm/sales-forecast.md`（实现约定 :221 Deferred 声明更新）；`docs/design/crm/use-cases.md`（不修改契约段）。

## Goals

- **territory 级 rollup（P1-RC-039 核心）**：`ForecastAggregator.refreshForecast` 在团队与公司之间插入区域级聚合——按 `ErpCrmLead.territoryId` **直接归属**分组聚合商机 totals（每个有直接商机的 territory 节点生成一行），构建 `territoryId 非空 + ownerId 空 + teamId 空` 的 ErpCrmForecast 行。**区域行 = 该节点直接商机 Σ（leaf-exact，每商机恰好计数一次）**；区域子树总额（含子节点商机）由既有 `accumulatePipeline` 子树 in 查询按需聚合（动态视图，不持久化重复行）——**该设计消除「祖先行 + 子行」双计（B1 裁决，见 Phase 1 Decision 1）**。
- **公司行语义**：公司行 = Σ 全部有 owner 商机（现状 companyTotals 保持，数值不变，零回归风险最低）；恒等式 = 公司行 = Σ 区域行 + 无 territory 且有 owner 商机 Σ（leaf-exact 下区域行不重叠，无双计；ownerless 商机两侧均不计——Phase 1 Decision 2 口径）。
- **管道闭合**：`accumulatePipeline`（QuotaRollupCalculator:180-199，子树 `in("territoryId", subtreeIds)` 查询）能命中新增区域行且**不重复计数**——pipeline(T1) = Σ 子树内全部区域行 = 子树商机总额（A4.2.93 缺口闭合；**不改 QuotaRollupCalculator**，Non-Goal 2 保持）。
- **ownerless 商机一致性**：territory 收集镜像个人循环的 `ownerId != null` 过滤（`:65-66` 现状跳过 ownerless）——ownerless 商机不进入任何 totals（含区域行），保持与现状个人/团队/公司行口径一致（m4 裁决，见 Phase 1 Decision 1）。
- **零回归**：既有 `TestErpCrmForecastAndScoring` 5 @Test 全绿（testRefreshForecastAndRollup:133-181 断言不受影响——无 territory seed 时区域行不生成，公司行语义不变）+ 新增 territory 矩阵测试全绿 + erp-crm-service 全模块测试零回归。
- **owner doc 收敛**：`sales-forecast.md:221` 实现约定 Deferred 声明更新为已实现（territory 层级落地注记 + leaf-exact 行语义 + 子树总额由管道动态聚合）；不修改需求契约段（use-cases L1 不动）。
- **回填**：arm-index P1-RC-039 → `done (RC-R1.25)` + roadmap 行 → `done` + `docs/logs/` 日志条目。

## Non-Goals

- **不触 ORM 结构**（零列/零索引/零新实体——ErpCrmForecast.territoryId 已存在，不加字段）。
- **不改 `QuotaRollupCalculator`**（`accumulatePipeline` 子树 in 查询语义保持——leaf-exact 行设计使其天然正确；任何改查询语义的方案均拒绝，见 Phase 1 Decision 1 裁决理由）。
- **不实现 Quota（配额）territory tier 变更**（QuotaRollupCalculator 已支持 territory 子树聚合，非本行范围）。
- **不改真相源契约段落**（use-cases L1 不动）。
- **不实现多币种折算**（P2-RC-050 / P1-MA1-010，独立 finding，非本行范围）。
- **不重算 accuracy 维度**（`computeAccuracy:107-115` 仅按个人 ownerId 过滤，区域行不参与准确率——现状保持，非本行范围）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧的预授权代码逻辑修复，Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/crm/use-cases.md`（L1 UC-CRM-10）+ `docs/design/crm/sales-forecast.md`（§业务规则 层级聚合 :108-121 + 实现约定 :221）+ `docs/design/crm/territory.md`（区域树语义）+ `docs/audits/2026-08-07-2345-rc-ma4-a4-2-83-96-crm-lead-lifecycle-marketing-forecast-runtime.md`（A4.2.93 运行时证据）
- Skill Selection Basis: 实现面 = BizModel 聚合引擎扩展（`nop-backend-dev`：聚合引擎模式、跨实体经 I*Biz/daoProvider、Decision 项记录）；测试（`nop-testing`：JunitAutoTestCase GraphQL 断言 + 快照录制）。无 view.xml/xbiz/ORM 变更。

## Infrastructure And Config Prereqs

- 无新 config key（territory rollup 为硬编码聚合逻辑，非 config-gated——L1 字面无配置开关）。
- 分域验证前置：`mvn install -DskipTests` 后 `mvn test -pl module-crm/erp-crm-service`。

## Execution Plan

### Phase 1 - 区域 rollup 语义裁决（Decision）

Status: completed
Targets: `ForecastAggregator.java`；`docs/design/crm/sales-forecast.md`
Skill: `nop-backend-dev`

- Item Types: `Decision`
- Prereqs: 无（既有基线）

- [x] `Decision` **区域行生成范围（B1 关键裁决——与 accumulatePipeline 子树查询一致性）**：选项 A（采用，推荐）= **leaf-exact 直接归属行**——仅对「有商机直接归属」的 territory 节点生成区域行，行金额 = 该节点直接商机 Σ（每商机恰计数一次，区域行间不重叠）；父节点区域总额（含子树）由既有 `accumulatePipeline` 子树 in 查询动态聚合（不改 QuotaRollupCalculator）。选项 B（否决）= 祖先展开行——「收集出现节点 + 祖先节点展开」，父行含子树 Σ → `accumulatePipeline` `in("territoryId", subtreeIds)` 会同时命中父行+子行**双计**（pipeline(T1)=A+2B 而非 A+B），Phase 3 管道断言不可满足。选项 C（否决）= 全树节点占位行（空行噪音 + 查询膨胀）。**理由（选项 A）**：①与既有消费者 `accumulatePipeline:185-192` 子树 in 查询语义天然一致（leaf 行不重叠 → 子树 Σ 正确，零 QuotaRollupCalculator 变更，Non-Goal 2 保持）；②销售层级实际数据分布（territory.md:143 商机归叶子）下 parent 无直接商机 → 无 parent 行，pipeline 子树查询恰好构成区域总额动态视图；③残留风险记录：持久化区域行仅覆盖直接归属节点，祖先行缺失由管道动态视图补足——L1「团队 → 区域 → 公司」的「区域」语义由「区域行 + 管道子树视图」共同达成，owner doc 注记明确该分层。
      - Skill: `nop-backend-dev`
      - **裁决（执行记录 2026-08-14）**：**采用选项 A（leaf-exact 直接归属行）**。理由复核：`accumulatePipeline:185-199` 以 `in("territoryId", subtreeIds)` 聚合 Forecast 行，leaf 行不重叠 → 子树 Σ 恰一次（pipeline(T1)=A+B 无重复），零 QuotaRollupCalculator 变更；祖先展开（选项 B）会导致 pipeline(T1)=A+2B 双计，Phase 3 管道断言不可满足，否决；全树占位（选项 C）空行噪音 + 查询膨胀，否决。残留风险（已登记）：持久化区域行仅覆盖直接归属节点，祖先行缺失由管道子树查询动态视图补足（Deferred But Adjudicated 段 watch-only residual 呼应）。
- [x] `Decision` **ownerless 商机归属（m4 裁决）**：选项 A（采用）= territory 收集镜像个人循环 `ownerId != null` 过滤（`:65-66` 现状跳过 ownerless）——ownerless 商机不进入任何 totals（含区域行、公司行），与现状个人/团队/公司行口径一致，恒等式「公司行 = Σ 区域行 + Σ（无 territory 且有 owner 商机）」保持；选项 B = 单独收集 ownerless-territoried 商机进区域行（破坏恒等式 + 与现状 totals 语义分叉，弃）。记录理由。
      - Skill: `nop-backend-dev`
      - **裁决（执行记录 2026-08-14）**：**采用选项 A（ownerless 排除）**。territory 收集在个人循环同一遍历内完成（镜像 `ownerId != null` 过滤），ownerless 商机两侧均不计 → 恒等式「公司行 = Σ 区域行 + Σ（无 territory 且有 owner 商机）」保持，与现状个人/团队/公司行口径一致；选项 B 破坏恒等式且与现状 totals 语义分叉，弃。实现落点见 Phase 2（同一遍历收集 `byTerritory`）。

Exit Criteria:

- [x] 两项 Decision 裁决记录落盘（选项 + 理由 + 残留风险）；区域行生成范围（leaf-exact）+ ownerless 归属确定

### Phase 2 - ForecastAggregator 区域 rollup 实现

Status: completed
Targets: `ForecastAggregator.java`
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: Phase 1 完成

- [x] `Fix` `ForecastAggregator.refreshForecast`：个人汇总循环内（同一遍历，镜像 `ownerId != null` 过滤）增 territory 收集——对 `opp.territoryId != null` 的商机按 territory 分组（`Map<Long, List<ErpCrmLead>>`，leaf-exact 直接归属，**不做祖先展开**）；循环后按 Phase 1 裁决为每个有直接商机的 territory 生成区域行（`buildForecast(period, null, null, totals)` + `setTerritoryId`，ownerId/teamId 保持 null）。
      - Skill: `nop-backend-dev`
      - **执行记录（2026-08-14）**：`refreshForecast` 个人循环内新增 `byTerritory` 收集（`opp.getTerritoryId() != null` 且 owner 非空——镜像 `ownerId != null` 过滤，ownerless 排除按 Phase 1 Decision 2）；团队 rollup 与公司 rollup 之间插入区域 rollup 循环（每节点 `ForecastTotals.of(直接商机)` → `buildForecast(period, null, null, territoryId, totals)`，leaf-exact 不展开祖先；区域行不 rebuildLines——与团队/公司 rollup 行一致，ForecastLine 仅个人行生成）。`ErpCrmForecastBizModel` javadoc 同步 4 级表述。
- [x] `Fix` `ForecastAggregator.buildForecast` 增 territoryId 参数或独立重载（保持个人/团队行调用不变——teamId 行与 territoryId 行维度互斥：区域行 teamId=null、ownerId=null；团队行 territoryId=null）。
      - Skill: `nop-backend-dev`
      - **执行记录（2026-08-14）**：新增 5 参重载 `buildForecast(period, ownerId, teamId, territoryId, totals)` 承载 `setTerritoryId(territoryId)`；原 4 参签名保留并委托 5 参（territoryId=null）——个人/团队/公司行调用点零改动，维度互斥由调用侧保证（区域行 ownerId/teamId=null，团队行 territoryId=null）。
- [x] `Fix` 类头注释 `:36` 层级声明更新为 4 级（个人→团队→区域→公司）+ leaf-exact 行语义与管道动态视图分层 javadoc（明确「区域行 = 直接归属 Σ；子树总额由 accumulatePipeline 子树查询动态聚合」）。
      - Skill: `nop-backend-dev`
      - **执行记录（2026-08-14）**：类头 javadoc 更新为 4 级层次 + leaf-exact 直接归属语义 + 子树总额由 `QuotaRollupCalculator.accumulatePipeline` 动态聚合的分层说明（不持久化重复行）。

Exit Criteria:

- [x] refreshForecast 生成区域行：leaf-exact 直接归属聚合金额正确（Phase 3 断言证实）+ 无 territory seed 时零区域行（既有测试零回归路径）
- [x] 零 ORM 变更（`git diff --stat` 仅 erp-crm-service Java + `_cases/` 快照）

### Phase 3 - 测试矩阵

Status: completed
Targets: `module-crm/erp-crm-service/src/test/java/app/erp/crm/service/TestErpCrmForecastAndScoring.java`（扩展）或新增 territory 测试类
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 2 完成

- [x] `Add` territory 树 seed（父-子两级 + 商机分属不同节点）：① 区域行生成——商机 A 直接归属 T1、商机 B 直接归属 T1 子节点 T1-1 → 区域行 T1 = Σ(A)（leaf-exact 直接归属，**不含 B**）+ 区域行 T1-1 = Σ(B)，两行不重叠；② 无 territory 商机（territoryId=null）不进区域行但计入公司行；③ 公司行 = Σ 全部商机（含无 territory）且 = Σ 区域行 + 无 territory 商机 Σ（恒等式断言，Phase 1 选项 A）；④ 区域行字段维度（territoryId 非空 + ownerId/teamId null）。
      - Skill: `nop-testing`
      - **执行记录（2026-08-14）**：新增测试类 `TestErpCrmForecastTerritoryRollup`（包内既有 forecast 测试族为显式 Java 断言风格、无 `_cases` 快照——沿用同型，断言证据齐备）：`testRefreshForecastTerritoryRollup` 覆盖 ①②③④——T1 行 = commit 1000/upside 0/best 1000/count 1（leaf-exact 不含子节点 B）；T1-1 行 = upside 2000/best 2000；区域行字段维度（territoryId 非空 + ownerId/teamId null）；期间内区域行恰 2 行（无 territory 商机 C 不进区域行）；公司行 = commit 1000/upside 2000/best 3500/count 3 = Σ 区域行(3000) + 无 territory C(500) 恒等式。
- [x] `Add` 管道闭合断言：`QuotaRollupCalculator.accumulatePipeline(T1)` Forecast 段 = Σ 子树全部区域行（T1 行 + T1-1 行 = A+B，**无重复计数**——leaf-exact 语义直接证据，A4.2.93 缺口闭合）。**注意**：`accumulatePipeline:185-199` Forecast 段无 periodId 过滤——测试 DB 仅含单期间数据，断言前注释说明（m5）。
      - Skill: `nop-testing`
      - **执行记录（2026-08-14）**：`testTerritoryPipelineClosureNoDoubleCount` 经 `ErpCrmQuota__getTerritoryPipeline`（accumulatePipeline 既有 GraphQL 入口）断言 Forecast 段 = commit 1000/upside 2000/bestCase 3000/count 2（= T1 行 + T1-1 行恰一次，非 A+2B 双计）——A4.2.93「territory 级管道 Forecast 段恒空」缺口闭合；测试类 javadoc 显式注释 Forecast 段无 periodId/periodLabel 过滤、断言依赖单期间测试 DB（territoryId 唯一于本类 seed，无跨测试污染）。
- [x] `Proof` 既有 `TestErpCrmForecastAndScoring` 5 @Test 零回归（无 territory seed 路径）+ 新增矩阵全绿：`mvn test -pl module-crm/erp-crm-service`（BUILD SUCCESS）+ `_cases/` 快照录制。
      - Skill: `nop-testing`
      - **执行记录（2026-08-14）**：`mvn test -pl module-crm/erp-crm-service` **180 tests 0 failures 0 errors**（178 基线 + 2 新增）BUILD SUCCESS——既有 5 @Test（testRefreshForecastAndRollup 等）零回归实证（无 territory seed 路径不生成区域行、公司行语义不变）；新增类经 RECORDING→CHECKING 快照录制（`_cases/app/erp/crm/service/TestErpCrmForecastTerritoryRollup/` input/output tables 24 文件，录制输出实证区域行 5801=1000/5802=2000/公司=3500）→ CHECKING 下 180 tests 全绿。

Exit Criteria:

- [x] 新增 territory 矩阵测试全绿 + 既有 5 @Test 零回归（`mvn test -pl module-crm/erp-crm-service` BUILD SUCCESS）
- [x] 三 finding 维度（区域行生成/leaf-exact 无重复/管道闭合）均有断言证据；快照录制完成

### Phase 4 - 文档回填 + arm-index/roadmap 状态

Status: completed
Targets: `docs/design/crm/sales-forecast.md`；`docs/audits/arm-index.md`；`docs/backlog/requirement-compliance-roadmap.md`；`docs/logs/2026/08-14.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1-3 完成

- [x] `Add` owner doc 注记：`sales-forecast.md:221` 实现约定 Deferred 声明更新为已实现（territory 层级落地 + **leaf-exact 直接归属行语义** + 子树总额由 accumulatePipeline 动态聚合的分层说明 + ownerless 商机口径）；不修改需求契约段。
      - Skill: none
      - **执行记录（2026-08-14）**：§实现约定改写——「层级 rollup 已实现四级：个人 → 团队 → 区域（RC-R1.25 落地）→ 公司」+ 区域行 leaf-exact 语义（仅直接归属节点生成行、每商机恰计数一次、不含子节点商机）+ 子树总额由 `QuotaRollupCalculator.accumulatePipeline` 动态聚合（不持久化重复行）+「区域」语义由「区域行 + 管道子树视图」共同达成 + ownerless 口径 + 恒等式；需求契约段（§业务规则 3 层级聚合 + use-cases L1）零改动。
- [x] `Add` arm-index P1-RC-039 → `done (RC-R1.25)` + 修复落地摘要；roadmap RC-R1.25 → done；`docs/logs/2026/08-14.md` 日志条目。
      - Skill: none
      - **执行记录（2026-08-14）**：arm-index P1-RC-039 行修复状态 `todo` → `done (RC-R1.25)`（含 plan 指针 + leaf-exact/管道闭合/180 tests/checker 零漂移摘要）；roadmap RC-R1.25 行 `todo` → `done ✅（…落地摘要）`；`docs/logs/2026/08-14.md` 顶部新增 RC-R1.25 日志条目（四 Phase + 验证状态 + 下一步）。

Exit Criteria:

- [x] arm-index/roadmap 状态回填 + owner doc 注记落盘；日志条目写入

## Draft Review Record

- Independent draft review iteration 1: needs revision（独立子代理 ses_fff24b36affejaqzJglbKwxB9U）— 1 Blocker（B1）：leaf-exact vs 祖先展开行与 `accumulatePipeline` 子树 in 查询的双计矛盾——原草案「收集出现节点 + 祖先节点展开」导致 pipeline(T1) = 父行(ΣA,B) + 子行(ΣB) = A+2B 双计，Phase 3 管道断言不可满足 → 已修正：Phase 1 Decision 1 改为 leaf-exact 直接归属行（区域行 = 节点直接商机 Σ，不重叠）+ 子树总额由管道子树查询动态聚合 + Non-Goal 明确不改 QuotaRollupCalculator；2 Major（M1：Phase 3 ③「公司行 = Σ 区域行」与无 territory 商机 seed 自相矛盾 → 恒等式改为「公司 = Σ 区域行 + Σ 无 territory 商机」；M2：Goal 1「公司 = Σ 区域行等值」在祖先展开下不成立 → 已修正为 leaf-exact 恒等式表述 + Decision 1 理由重写）；5 Minor（m1 测试行号/无公司行断言 → :133-181 修正；m2 索引 :842-855 → :850-866/:854-856；m3 :814-815 → :821；m4 ownerless 商机口径 → Phase 1 Decision 2 裁决；m5 管道无 periodId 过滤 → Phase 3 测试项注释说明）。
- Independent draft review iteration 2: `accept`（独立子代理 ses_fff1b135fffeMoE7SOXs10cz5P）— 0 BLOCKER / 0 MAJOR。iteration-1 全部 8 项（1 Blocker + 2 Major + 5 Minor）经 live repo 复核确认解决：B1 leaf-exact 设计与未改的 `accumulatePipeline:185-199` in(subtree) 语义数学自洽（leaf 行不重叠 → pipeline(T1)=A+B 恰一次；个人/团队/公司行 territoryId=null 不污染子树查询）；M1/M2 恒等式与 Goal 表述修正属实；m1-m5 行号修正全实证（orm.xml:821/:843/:854-856、:214/:237、:990、测试 :133-181、sales-forecast.md:108-121 分层差异已在 Decision 1 理由③ + Phase 4 显式文档化，未隐藏）。1 个新 MINOR（Decision 2 括注「无 territory 商机含 ownerless」自相矛盾——ownerless 两侧均不计）已顺手修订：Goal 公司行表述改「Σ 全部有 owner 商机」+ Decision 2 恒等式改「Σ（无 territory 且有 owner 商机）」。**计划可标记 active。**

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。**完整仓库验证在此处**：结束时运行一次全量验证。

- [x] 范围内行为完成（leaf-exact 区域行生成 + 4 级 rollup + 管道闭合实证，Phase 1-4 全落地）
- [x] 相关文档对齐（sales-forecast.md §实现约定 Deferred → 已实现注记；arm-index/roadmap 回填；日志条目；bug 注记）
- [x] 已运行验证（`mvn test -pl module-crm/erp-crm-service` 180 tests 全绿 + `mvn clean install -DskipTests` 全量 BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline 零漂移 exit 0（R1d=14/R2a=34/R2b=230/R2c=1393/R2d=34/R3=5/R5=0/R6=2/R10=8/R11=0/R12a=69/R12b=66/R12c=40 全对齐）——注：全 reactor `mvn test` 有 2 项**预存在**失败（TestAuthSeedLoadingProof NPE[已登记 bug 注记] + ErpMfgCostRollupLine materialBand cell-not-prop[本计划新增 bug 注记]，均经 stash 后 clean HEAD 复跑实证与本计划无关），不在本计划范围内）
- [x] 无范围内项目降级为 deferred/follow-up（Deferred But Adjudicated 段仅 watch-only residual 登记，无范围内降级）
- [x] 独立草案审查已完成并记录（Draft Review Record：iteration 1 needs revision → iteration 2 accept，8 项全复核解决）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致（Phase 1-4 全 `completed` + 全 `[x]`；Closure 段与日志条目/arm-index/roadmap 一致；git diff 零 ORM 变更）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（独立子代理 ses_2dlD0hIepW3zZs5dY5k7Bsg8Tp，见 Closure 段）
- [x] 结束证据存在于文件中（本计划 Closure 段 + `docs/logs/2026/08-14.md` 日志条目 + `docs/bugs/2026-08-15-0030-mfg-cost-rollup-line-materialband-cell-not-prop.md`）

## Deferred But Adjudicated

### territory 层级空节点不生成区域行

- Classification: `watch-only residual`
- Why Not Blocking Closure: Phase 1 选项 A（leaf-exact）下无直接商机的 territory 节点不生成区域行——区域总额经 `accumulatePipeline` 子树查询动态聚合（既有消费者语义），L1 未要求全树占位行；若未来需全树占位（看板渲染需求）属增强非 L1 义务。
- Successor Required: `no`

## Closure

Status Note: completed（2026-08-15 独立结束审计 PASS 后关闭）

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（新会话）ses_ffed3e930ffexkwyx4G1u7YFeD（结束审计报告：逐项核验 6 组 22 项全 ✅ + 审计者实跑 `mvn test -pl module-crm/erp-crm-service` 180 tests 0 failures + compliance checker exit 0 零漂移复证；最终裁决 **PASS**，0 P0 / 0 P1；2 项 P2 非阻塞——P2-1 快照文件计数笔误[26→24]已修正，P2-2 Gate #7 勾选先于审计执行的小偏离经审计确认非 hollow closure（既有已关闭计划同模式，审计确为独立新会话执行））
- 执行记录：Phase 1-4 全 completed + 全 `[x]`；Closure Gates 8 项全 `[x]`；零 ORM 变更（git diff 仅 erp-crm-service 2 Java + 1 新测试类 + `_cases/` 快照 24 文件 + docs）

Follow-up:

- 无范围内 follow-up（Deferred But Adjudicated 段 watch-only residual：无直接商机的 territory 节点不生成区域行——区域总额由 accumulatePipeline 子树查询动态聚合，若未来需全树占位行属增强非 L1 义务）
- 全 reactor `mvn test` 预存在失败 2 项（与本计划无关，已登记 docs/bugs/）：TestAuthSeedLoadingProof NPE（`2026-08-14-0930-authseed-loading-npe-ormtransactionlistener.md`，nop-entropy lazy-property 回归，外部仓库 ask-first）+ ErpMfgCostRollupLine materialBand cell-not-prop（`2026-08-15-0030-mfg-cost-rollup-line-materialband-cell-not-prop.md`，mfg 页面/实体字段对齐待独立计划裁决）
