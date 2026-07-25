# 2026-07-25-1016-2 GL Mapping orgId dimension activation

> Plan Status: completed
> Last Reviewed: 2026-07-25
> Source: dormant dimension收口 + doc/code drift — `docs/plans/2026-07-24-1351-1-gl-mapping-provider-rollout.md` §Deferred「orgId 维度激活」+ owner doc `docs/design/finance/gl-mapping-rules.md:118/192` 已规定 orgId 参与 cache key 但代码自 A1 起漂移
> Related: `docs/plans/2026-07-21-0827-1-finance-gl-mapping-rule-tables.md`（A1 GL Mapping Rule Tables）、`docs/plans/2026-07-22-1000-1-finance-multi-company-operational-depth.md` A3（多组织基础设施就绪，构成 orgId 维度的使用场景前置）
> Audit: required

## Current Baseline

GL 映射规则表（A1 `2026-07-21-0827-1`）已落地并完成全 28 routing Provider 接入（`2026-07-24-1351-1`）。A3 多公司运营深度（`2026-07-22-1000-1`）已落地跨法人交易基础设施。

**orgId 维度为已建模 + owner-doc 已规定但代码未实现（doc/code drift）状态**（实时仓库核实 2026-07-25）：

- `ErpFinGlMappingRule` 实体（`module-finance/model/app-erp-finance.orm.xml:2014`）：`orgId` 列 propId=4，`mandatory="true"`，displayName="核算组织"，含 `<to-one name="org">` 关系 + `UK_FIN_GL_MAPPING_RULE_BIZ`（含 orgId）+ `IDX_FIN_GL_MAPPING_RULE_ORG_ID`。**实体层完全建模**。
- **owner doc 已规定 orgId 参与 cache key**：`docs/design/finance/gl-mapping-rules.md:118` 伪代码 `rulesByIndex.get((orgId, businessType, accountKey))` + `:192` "按 `(orgId, businessType, accountKey)` 索引"。**owner doc 设计真相源已包含 orgId 维度**。
- `ErpFinGlMappingResolver`（`erp-fin-service/.../posting/ErpFinGlMappingResolver.java`）**代码与 owner doc 漂移**：
  - `resolveOrgIdFromDimensions(GlMappingDimensions)`（:224-226）：**硬编码 `return null`**。
  - `reloadCache()`（:247-262）：cache key 使用 `cacheKey(null, rule.getBusinessType(), rule.getAccountKey())`——**rule.orgId 被丢弃**，与 owner doc `:118/192` 规定的 `(orgId, ...)` 索引不一致。
  - `matches(rule, dims, acctSchemaId)`（:156-177）：检查 6 维，**不含 orgId**。
  - `specificity()`（:145-154）：计数 6 维（注释 :143 声称「共 8 维」含 fromOrgId/toOrgId——**注释漂移**，代码仅计 6 维）。
- `GlMappingDimensions` DTO（`erp-fin-dao/.../dto/GlMappingDimensions.java`）：有 `fromOrgId`/`toOrgId`（A3 intercompany，:23-26）但**无通用 `orgId` 字段**。`expandDimensions`（:194-195）透传 fromOrgId/toOrgId，但 `matches`/`specificity` 均不消费——**fromOrgId/toOrgId 当前零匹配效果**（且 `buildGlMappingDimensions` 从未设置它们，源头即 null）。
- `ErpFinGlMappingRule` 实体**无 fromOrgId/toOrgId 列**（仅有 orgId）。

**orgId 数据流 plumbing gap**：`PostingEvent.getOrgId()` / `AcctDocContext.getOrgId()`（`ErpFinPostingProcessor:518/531`）携带 orgId，但 `resolveSubjects(List<VoucherFact>, IServiceContext)`（:552）不接收 orgId，`buildGlMappingDimensions(VoucherFact)`（:611）仅从 VoucherFact 取维度字段——`VoucherFact`（:11-27）**无 orgId 字段**。orgId 在调用链中可达但未传递至 resolver。

**触发条件诚实评估**：A1/1351-1 §Deferred 登记 orgId 激活触发条件为「多组织差异化科目映射需求」。A3 多公司运营深度落地了多组织**基础设施**（跨法人凭证 + 转移定价 + 配对 + 抵消），构成 orgId 维度的使用场景前置；intercompany 凭证经硬编码 fallback 科目（1131/5001/1401/2202）工作，不强制要求 org 差异化映射。**本计划不以「业务需求已满足」为由强制触发**，而是以 **owner doc 已规定 orgId cache key 但代码漂移**（doc/code 一致性缺口）+ dormant 维度收口为由推进，config-gated 默认关闭——业务需求在多组织部署启用 org-dimension 时自证。

## Goals

- 收口 owner doc / code drift：使 `ErpFinGlMappingResolver` 的 cache key + 匹配逻辑与 `gl-mapping-rules.md:118/192` 已规定的 `(orgId, businessType, accountKey)` 索引一致。
- 激活 `ErpFinGlMappingRule.orgId` 维度参与规则匹配（config-gated `erp-fin.gl-mapping.org-dimension-enabled` 默认 `false`）：关闭时行为 = 现状（org-agnostic，向后兼容）；开启时 org 精确匹配（不同组织可为同一 (businessType, accountKey) 配置不同 targetSubjectCode）。
- 打通 orgId plumbing：`PostingEvent.orgId` → `VoucherFact.orgId`（新增字段）→ `GlMappingDimensions.orgId`（新增字段）→ resolver。
- 单测覆盖 org-dimension-on/off 两路径 + owner doc drift 注记。

## Non-Goals

- **fromOrgId/toOrgId 作为匹配维度**：需在 `ErpFinGlMappingRule` 新增列（ORM 变更 + ask-first），且语义为 intercompany 双方组织（与 orgId 核算组织概念不同），归独立 successor。
- **GL Distribution（科目分摊）**：A1 既定 Deferred，按 posting.md §FactsValidator 独立扩展点。
- **多节点分布式缓存一致性**：A1 既定 Deferred。
- **orgId nullable / 全局通配规则**：当前 orgId mandatory=true；引入 null=全局通配须改 ORM mandatory（ask-first）。
- **模板驱动路径 accountKey 统一**：1351-1 既定 Deferred。
- **业务场景驱动的多组织规则种子**：本计划仅激活维度 + config-gate；具体多组织规则待业务部署时配置。

## Task Route

- Type: `implementation-only change`（resolver 维度激活 + DTO 字段新增 + doc/code drift 收口；orgId 列已存在，无 ORM 模型变更——`VoucherFact`/`GlMappingDimensions` 为手写 DTO 非 `_gen/` 生成实体）
- Owner Docs: `docs/design/finance/gl-mapping-rules.md`（§3/§4 orgId 维度段 — 注记 doc/code drift 收口）、`docs/architecture/multi-company.md`（§与 Posting+GL Mapping 关系 EXPAND orgId 激活）
- Skill Selection Basis: `nop-backend-dev`（service 层 Java + config-gated 特性 + protected 方法 + IDaoProvider 范式）

## Infrastructure And Config Prereqs

- 新增 config：`erp-fin.gl-mapping.org-dimension-enabled`（默认 `false`，对齐 A2/A3 config-gated 范式）
- No other infra prereqs

## Execution Plan

### Phase 1 - orgId plumbing + 维度激活（含 Decision）

Status: completed
Targets: `ErpFinGlMappingResolver.java`、`GlMappingDimensions.java`、`VoucherFact.java`、`ErpFinPostingProcessor.java`（`buildGlMappingDimensions` + fact 构建点）
Skill: `nop-backend-dev`

- Item Types: `Decision | Add`
- Prereqs: 无

- [x] `Decision`: orgId plumbing 路径裁决。候选：
  - (a) `VoucherFact` 增 `orgId` 字段，在 fact 构建点（`ErpFinPostingProcessor` 内 `PostingEvent.getOrgId()` 可达处）设置 → `buildGlMappingDimensions` 复制至 `GlMappingDimensions.orgId`（新增字段）。
  - (b) `resolveSubjects` 签名增 orgId 参数（或经 `IServiceContext` 传递），`buildGlMappingDimensions` 接收 orgId。
  - 裁决倾向 (a)（VoucherFact 为 per-fact 上下文载体，orgId 与 partnerId/warehouseId 同级自然；避免改 resolveSubjects 签名扩散）。记录选择 + 替代方案。
  - Skill: `nop-backend-dev`
  - **裁决记录**：采用 (a)。`VoucherFact.orgId` 与既有 `partnerId`/`warehouseId` 同级（per-fact 上下文载体）；在 `generateFacts` validator 链后批量设置（避免 validator 返回新列表时丢失）；`buildGlMappingDimensions` 透传至 `GlMappingDimensions.orgId`。替代方案 (b) 改 `resolveSubjects` 签名会扩散至所有调用点，否决。
- [x] `Add`: 按Decision 实现（config-gated）：
  - `GlMappingDimensions` 增 `orgId` 字段 + `VoucherFact` 增 `orgId` 字段（手写 DTO，非 ORM）。
  - fact 构建点设置 `fact.setOrgId(event.getOrgId())`（经 `ErpFinPostingProcessor` 内 event 可达处）。
  - `buildGlMappingDimensions` 复制 `dims.setOrgId(fact.getOrgId())`。
  - `expandDimensions`（:183-208）构造新 `GlMappingDimensions` 时复制 `expanded.setOrgId(input.getOrgId())`——**必须显式列出**，否则该中间转换会静默丢弃 orgId 致 `resolveOrgIdFromDimensions(effectiveDims)` 见 null。
  - `resolveOrgIdFromDimensions`：`org-dimension-enabled=true` 时返回 `dimensions.getOrgId()`；`false` 时返回 null（现状）。
  - `reloadCache`：cache key 使用 `cacheKey(rule.getOrgId(), businessType, accountKey)`——按 orgId 分桶（对齐 owner doc `:118/192`）。
  - `loadFromCache`/`loadFromDb`：orgId 非空时按 orgId 精确取候选；`loadFromDb`（:239-245）须加 `eq("orgId", orgId)` 过滤否则 cache 禁用路径绕过分桶；orgId=null 时取全量（兼容关闭态）。
  - `matches`：`org-dimension-enabled=true` 且 rule.orgId 非空时检查 `rule.orgId == dimensions.orgId`（exact）。
  - `specificity`：orgId 非空 +1；修正注释为正确维度计数。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] orgId 维度激活代码落地，config-gated 默认 false（关闭态行为 = 现状，既有 `TestErpFinGlMappingResolver` 场景零回归）
- [x] `ErpFinPostingProcessor` `buildGlMappingDimensions` 在 fact.orgId 非空时传递至 dims（受影响模块 `mvn compile` 通过）

### Phase 2 - 单测 + owner doc drift 收口

Status: completed
Targets: `TestErpFinGlMappingResolver.java`、`docs/design/finance/gl-mapping-rules.md`、`docs/architecture/multi-company.md`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 完成

- [x] `Add`: `TestErpFinGlMappingResolver` 扩展 org-dimension 场景（config on/off 两路径）：
  - 关闭态：既有场景零回归（orgId 被忽略，行为不变）。
  - 开启态：org 精确匹配（orgId=2 命中 orgId=2 规则）+ org 不匹配返回 null（orgId=2 不命中 orgId=1 规则）+ 缓存按 orgId 分桶 + specificity 含 orgId 计数。
  - Skill: `nop-backend-dev`
- [x] `Proof`: owner doc drift 收口——`docs/design/finance/gl-mapping-rules.md` §优先级链/缓存策略 注记 doc/code drift 已收口（代码现与 `:118/192` 规定的 `(orgId, ...)` 索引一致）+ config-gated + exact 匹配语义 + 关闭态行为；`docs/architecture/multi-company.md` §与 Posting+GL Mapping 关系 增 orgId 激活注记。
  - Skill: none

Exit Criteria:

- [x] `TestErpFinGlMappingResolver` org-dimension 场景全绿（关闭态零回归 + 开启态新场景通过）
- [x] owner doc gl-mapping-rules.md drift 收口注记 + multi-company.md EXPAND 段落地

## Draft Review Record

- Independent draft review iteration 1: needs-revision（`ses_068eceef9ffeeUwO40hNPtGBqh`）— trigger honesty（"业务需求已满足"强制触发）+ VoucherFact.orgId plumbing 未明确 + owner doc drift 未识别。已修订：trigger 重框为 doc/code drift 收口 + dormant 维度收口（非业务需求强制触发）；plumbing 经实时仓库核实明确 VoucherFact→GlMappingDimensions 路径；owner doc `:118/192` drift 纳入基线。
- Independent draft review iteration 2: acceptable-as-is（`ses_068e7787bffe2ka13qwie3jSEV`）— 三项 iteration 1 阻塞项全部核实已修复 + 7 项 live-repo 验证全通过。采纳 2 项 non-blocking 建议（`expandDimensions` 显式复制 orgId 防静默丢弃 + `loadFromDb` 加 `eq("orgId", orgId)` 过滤）。Plan Status → active。

## Closure Gates

- [x] 范围内行为完成（orgId 维度激活 config-gated + plumbing + 单测 + owner doc drift 收口）
- [x] 相关文档对齐（gl-mapping-rules.md / multi-company.md）
- [x] 已运行验证：`mvn clean install -DskipTests`（154 模块）+ `mvn test -pl module-finance/erp-fin-service`（含 TestErpFinGlMappingResolver）+ 关闭态既有场景零回归
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

### 执行证据（executor-provided）

- **Phase 1 验证**：`mvn -pl module-finance/erp-fin-dao,module-finance/erp-fin-service -am compile` BUILD SUCCESS。
- **零回归验证**：`mvn -pl module-finance/erp-fin-service test -Dtest=TestErpFinGlMappingResolver` → Tests run: 15, Failures: 0, Errors: 0（含关闭态 10 既有场景全绿）。
- **finance-service 全量**：`mvn -pl module-finance/erp-fin-service test` → Tests run: 285, Failures: 0, Errors: 0。
- **全工程构建**：`mvn clean install -DskipTests`（154 模块）BUILD SUCCESS。
- **新增场景**：(k) 关闭态忽略 orgId / (l) 开启态 org 精确匹配 / (m) 开启态 org 不匹配返回 null / (n) 开启态不同组织不同科目（cache 按 orgId 分桶）/ (o) 开启态 specificity 含 orgId 计数。

## Deferred But Adjudicated

### fromOrgId/toOrgId 作为匹配维度

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 需在 `ErpFinGlMappingRule` 新增列（ORM 变更 + ask-first）；语义为 intercompany 双方组织，与 orgId 核算组织概念不同。
- Successor Required: `yes`（触发条件：intercompany 科目需按双方组织差异化 + finance owner doc 授权）

### orgId nullable / 全局通配规则

- Classification: `optimization candidate`
- Why Not Blocking Closure: 当前 orgId mandatory=true；引入 null=全局通配须改 ORM mandatory（ask-first）。本计划采用 exact 匹配。
- Successor Required: `yes`（触发条件：需「组织级规则 + 全局兜底」两级匹配语义时）

## Closure

Status Note: owner doc / code drift 已收口——`ErpFinGlMappingRule.orgId` dormant 维度经 config-gate `erp-fin.gl-mapping.org-dimension-enabled`（默认 `false`）激活，plumbing 链路（`PostingEvent.orgId → VoucherFact.orgId → GlMappingDimensions.orgId → resolver`）打通，cache key + 匹配逻辑现与 owner doc `gl-mapping-rules.md:118/192` 的 `(orgId, businessType, accountKey)` 索引一致。关闭态零回归（既有 10 场景全绿 + finance-service 全量 285 passed），开启态 5 新场景全绿，154 模块 clean install BUILD SUCCESS。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，不重用执行者上下文，task: closure-audit 2026-07-25-1016-2）。
- Live-repo 验证（语义核对，非盲信 `[x]`）：
  - `ErpFinGlMappingResolver.java`：`CONFIG_ORG_DIMENSION_ENABLED` 常量（:51）+ `resolveOrgIdFromDimensions` config-gated（:235-240）+ `reloadCache` 按 `rule.orgId` 分桶（:273-281）+ `loadFromDb` 加 `eq("orgId", orgId)`（:262-264）+ `matches` exact 校验（:159-162）+ `specificity` 含 orgId 计数（:148）+ `expandDimensions` 显式复制 orgId（:194）+ 注释修正为「共 7 维」（:143）。
  - `GlMappingDimensions.java`：`orgId` 字段 + getter/setter（:16, 29-35）。
  - `VoucherFact.java`：`orgId` 字段 + getter/setter（:21, 94-100）。
  - `ErpFinPostingProcessor.java`：`generateFacts` 后批量 `fact.setOrgId(event.getOrgId())`（:552-555）+ `buildGlMappingDimensions` 透传 `dims.setOrgId(fact.getOrgId())`（:621-623）+ `translateFactsForSchema` copy 保 orgId（:674）。
  - `TestErpFinGlMappingResolver.java`：5 新场景 (k)-(o) 全落地（:241-334）+ `withOrgDimensionEnabled` helper finally 恢复 + invalidate（:336-348）。
  - `docs/design/finance/gl-mapping-rules.md`：§3 orgId 维度激活注记（:114-120）+ §4 orgId 分桶对齐注记（:197-199）。
  - `docs/architecture/multi-company.md`：§与 Posting+GL Mapping 关系 EXPAND orgId 激活注记（:238-243）。
  - `docs/logs/2026/07-25.md`：聚合日志条目含 full-green 验证声明（:3-9）。
- Anti-hollow 检查：config-gated 代码经 `resolveSubjectCode` 主路径实际调用（`isOrgDimensionEnabled()` 在 reloadCache/matches/specificity 三处运行时消费）；`withOrgDimensionEnabled` 测试经 `AppConfig.assignConfigValue` 真实切换配置，非桩。无空函数体 / 无 `return null` 占位（关闭态 null 为设计语义，非占位）/ 无吞异常。
- 文本一致性：Plan Status `completed` ↔ Phase 1/2 Status `completed` ↔ 所有 Exit Criteria `[x]` ↔ Closure Gates 全 `[x]`（含本审计门）↔ logs 07-25.md full-green 声明 — 全部一致。

Follow-up:

- fromOrgId/toOrgId 匹配维度（触发条件见上 Deferred But Adjudicated）
- orgId nullable 全局通配（触发条件见上 Deferred But Adjudicated）
