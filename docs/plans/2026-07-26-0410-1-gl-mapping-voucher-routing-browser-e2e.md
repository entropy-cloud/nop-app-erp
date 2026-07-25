# 2026-07-26-0410-1 GL Mapping 凭证科目路由浏览器层 E2E

> Plan Status: completed
> Last Reviewed: 2026-07-26
> Source: 近期深化后端特性浏览器层验证缺口 —— GL Mapping 规则表（A1，plan `2026-07-21-0827-1`）+ Provider 全域接入（plan `2026-07-24-1351-1`，28 Provider）+ orgId 维度激活（plan `2026-07-25-1016-2`）三者经 JUnit 覆盖（`TestErpFinGlMappingResolver` 8 场景 + `TestErpPurInvoicePosting` 3 场景 + 域 service `mvn test`），但**零浏览器层 E2E**。AGENTS.md §当前项目阶段明示「各域细化端到端验证」为当前重点。
> Related: `docs/plans/2026-07-21-0827-1-finance-gl-mapping-rule-tables.md`（A1 落地）、`docs/plans/2026-07-24-1351-1-gl-mapping-provider-rollout.md`（28 Provider 接入）、`docs/plans/2026-07-25-1016-2-gl-mapping-org-dimension-activation.md`（orgId 维度激活）
> Audit: required

## Current Baseline

GL Mapping 解析器 `IErpFinGlMappingResolver`（`erp-fin-dao`）+ `ErpFinGlMappingResolver` 实现（`erp-fin-service`）已落地，经 `ErpFinPostingProcessor.resolveSubjects:562` 在每张过账凭证 resolveSubjects 阶段**无条件调用**（`@Inject IErpFinGlMappingResolver glMappingResolver`，非 config-gated；strict-mode `erp-fin.gl-mapping.strict-mode` 默认 false 控制空匹配是否抛错）。

解析链（`ErpFinGlMappingResolver:94-118`）：按 `(businessType, accountKey, dimensions)` 查 `ErpFinGlMappingRule` → 命中（`isActive=true` 匹配，`ErpFinGlMappingResolver:101`）则覆盖 `fact.subjectCode`，未命中返回 null → 保留 Provider 默认 subjectCode。进程内 `ConcurrentHashMap` 缓存（`erp-fin.gl-mapping.cache-enabled` 默认 true）**在 `@PostConstruct init()` 饥饿加载**（`ErpFinGlMappingResolver:68-78`），首次解析命中缓存（fresh-DB 缓存桶空→miss→null）；规则经 `__save` 新增后**须调 `invalidateCache()`** 方可见（缓存不会自动感知 DB 变更）。

`ErpFinGlMappingRule` 实体（22 字段，ORM `app-erp-finance.orm.xml:2014`）含 `businessType` / `accountKey` / `targetSubjectCode` / `orgId` / `materialCategoryId` / `priority` / `isActive`（BOOLEAN，列 `IS_ACTIVE`，`:2034`）等维度。**注意**：实体**无 `status` 列、无 `materialId` 列**（仅有 `materialCategoryId`；resolver 在运行时从 `fact.materialId` 派生 `materialCategoryId` 维度，规则本身不存 `materialId`）。标准 CRUD 经 `ErpFinGlMappingRuleBizModel extends CrudBizModel`（GraphQL `__save` 可达）；`defaultPrepareSave/Update/Delete`（`:42-64`）**已在每次 `__save`/`__update`/`__delete` 后注册 post-commit `invalidateCache`**（规则变更自动对缓存可见，spec 无需手动刷缓存；手动刷新 @BizMutation 名为 `ErpFinGlMappingRule__refreshCache` `:85-89`）。

既有浏览器层凭证行断言范式（`tests/e2e/orchestration/_helper.ts`）：`findVoucherIdByBillCode(page, billCode, postingType?)`（第三参为 `postingType: 'NORMAL'|'REVERSAL'`，**已支持** postingType 过滤，无需扩展）+ `assertVoucherLines(page, voucherId, expected[])`。既有 p2p-chain AP_INVOICE 断言（`p2p-chain.spec.ts:70-77`）：`1403 Dr 50 / 2221 Dr 6.5 / 2202 Cr 56.5`（PurAcctDocProvider 默认科目，三 accountKey PURCHASE/INPUT_VAT/ACCOUNTS_PAYABLE）。

剩余差距：GL Mapping 路由（规则命中 → 科目覆盖）经 JUnit 单层验证，但**全栈浏览器层路径未验证**——规则经 GraphQL `__save` 创建 → 采购/销售发票审核触发过账 → resolveSubjects 调 resolver → 凭证行 subjectCode 覆盖可观测。orgId 维度（1016-2 config-gated `erp-fin.gl-mapping.org-dimension-enabled`）同样零浏览器层覆盖。

## Goals

- 验证 GL Mapping 规则命中时凭证行 subjectCode 被覆盖（浏览器层全栈：GraphQL 建规则 → 链路审核过账 → 凭证行断言）
- 验证 orgId 维度激活时规则按组织差异化匹配（1016-2）
- 验证未命中保留 Provider 默认科目（控制对照）

## Non-Goals

- GL Distribution（科目分摊）—— 1351-1 Deferred（独立扩展点，触发条件未满足）
- 多节点分布式缓存一致性 —— A1 Deferred（触发：生产多节点）
- strict-mode 空匹配抛错路径 —— JUnit 已覆盖（`TestErpFinGlMappingResolver` empty-null 场景），浏览器层复跑低收益
- GL Mapping Operator UI 交互（refresh-cache 按钮）—— `gl-mapping-rule.visual.spec.ts`（A1 落地）已覆盖 2 测试
- 生产 Java/ORM/契约/codegen/字典/种子变更 —— 纯测试 + 文档

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/finance/gl-mapping-rules.md`（§3 优先级链 + §4 缓存 + §Provider opt-in 集成契约 + §intercompany 维度接入(A3)）、`docs/design/finance/posting.md`（§科目映射 GL Mapping 段）
- Skill Selection Basis: `nop-testing`（Playwright 浏览器层 E2E + 既有 orchestration/_helper 复用 + request.json5 范式）

## Infrastructure And Config Prereqs

- webServer JVM args 追加 `-Derp-fin.gl-mapping.org-dimension-enabled=true`（orgId 维度断言阶段，config-gated 默认 false）。其余 GL Mapping hook 无 config（无条件 active）。strict-mode 保持默认 false。
- No infra prereqs beyond existing baseline（fresh-DB H2 + 既有 webServer 启动链）。

## Execution Plan

### Phase 1 - Explore（规则字段 + 解析链 + 断言基准核实）

Status: completed
Targets: `module-finance/model/app-erp-finance.orm.xml`（ErpFinGlMappingRule 字段）+ `module-finance/erp-fin-service/.../posting/ErpFinGlMappingResolver.java` + `module-finance/erp-fin-service/.../posting/ErpFinPostingProcessor.java:562-585`
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: 无

- [x] Proof: 核实 `ErpFinGlMappingRule` 经 GraphQL `__save` 创建所需的最小必填字段集（businessType / accountKey / targetSubjectCode / `isActive=true` + 至少一个组织/账套维度），确认 `defaultPrepareSave/Update/Delete`（`:42-64`）的 post-commit `invalidateCache` 覆盖 `__save`/`__update`/`__delete` 路径（规则变更自动对缓存可见，spec 无需手动刷缓存）
- [x] Proof: 核实 p2p-chain AP_INVOICE 三 accountKey（PURCHASE / INPUT_VAT / ACCOUNTS_PAYABLE）经 `PurAcctDocProvider.createFacts` setAccountKey 非空（1351-1 已核实），规则可按 accountKey 精确覆盖单行

Exit Criteria:

> 仅写此阶段实际交付的可观察结果，以及解除后续阶段阻塞所需的任何本地化检查。

- [x] Explore 笔记记录字段集 + 缓存行为 + accountKey 命中可行性（写入 plan Execution Decision 段，不新建独立文档）

### Phase 2 - spec 实现（规则命中科目覆盖 + orgId 维度 + 控制对照）

Status: completed
Targets: `tests/e2e/business-actions/fin-gl-mapping-routing.action.spec.ts`（NEW）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [x] Add: 新建 `fin-gl-mapping-routing.action.spec.ts`，复用 `runP2pChain`（AP_INVOICE，三 accountKey 覆盖面充分，AR_INVOICE 同机制不重复）既有编排原语；setup 经 GraphQL `ErpFinGlMappingRule__save` 建测试专用规则（`isActive=true` + 测试专用 targetSubjectCode，区别于种子 1403 默认科目）—— `defaultPrepareSave` post-commit 自动失效缓存，spec 无需手动刷
- [x] Proof: (1) **命中覆盖** —— 建规则 AP_INVOICE+PURCHASE → 测试科目 X → runP2pChain → AP_INVOICE 凭证行 `1403` 被 `X` 替换（assertVoucherLines 断言 X Dr 50），其余两行（2221/2202）不变；(2) **控制对照** —— 无规则时同一链路凭证行保持 `1403`（既有 p2p-chain 断言复证，证明覆盖非偶然）；(3) **orgId 维度** —— org-dimension-enabled=true 时建 orgId 专属规则 → 仅匹配组织覆盖、非匹配组织保留默认（经 runP2pChain orgId 与规则 orgId 一致/不一致双路径；Phase 1 核实 runP2pChain orgId 可参数化，若不可参数化改自包含 setup）
- [x] Add: cleanup 删除测试规则（`ErpFinGlMappingRule__delete`，`defaultPrepareDelete` 自动失效缓存）+ 凭证经既有 `cleanupVoucherByBillCode`（mapped 科目凭证与默认科目凭证同 billCode，cleanup 已覆盖）
- [x] Proof: spec 全绿（命中 + 控制对照 + orgId 维度三组断言），fresh-DB 无 dashboard 基线漂移（mapped 科目不影响 stockBalance/arAp 辅助账因 AP_INVOICE 凭证业财回链不变）

Exit Criteria:

> 仅写此阶段实际交付的可观察结果。

- [x] `fin-gl-mapping-routing.action.spec.ts` 全绿，断言 mapped subjectCode 覆盖 + 控制对照默认保留 + orgId 维度差异化三组可观察结果

### Phase 3 - owner doc 回链 + e2e-runbook + 日志

Status: completed
Targets: `docs/design/finance/gl-mapping-rules.md`（实现注记段）+ `docs/testing/e2e-runbook.md`（业务动作表 + GL Mapping 路由行）
Skill: `none`

- Item Types: `Add`
- Prereqs: Phase 2

- [x] Add: `gl-mapping-rules.md` 增「浏览器层路由验证」实现注记（规则 __save → 链路审核 → 凭证行覆盖全栈路径 + 缓存 fresh-DB 行为 + orgId 维度断言范式）
- [x] Add: `e2e-runbook.md` 业务动作表 +finance GL Mapping 路由行 + webServer JVM arg 段补 `org-dimension-enabled`

Exit Criteria:

- [x] owner doc + runbook 更新落地（仅此阶段实际更改 owner 行为文档）

## Execution Decision（Phase 1 Explore 笔记）

经实时仓库核实（2026-07-26）：

1. **`ErpFinGlMappingRule` 经 GraphQL `__save` 最小必填字段集**（权威 `module-finance/model/app-erp-finance.orm.xml:2014-2070`）：
   - mandatory：`code`（domain=code）/ `name` / `orgId`（mandatory=true，无 default）/ `businessType`（dict `erp-fin/business-type`）/ `accountKey`（dict `erp-fin/account-key`）/ `targetSubjectCode`（domain=subjectCode）/ `priority`（defaultValue=0）/ `isActive`（BOOLEAN，defaultValue=true）
   - 可空维度（NULL=通配）：`acctSchemaId` / `partnerGroupId` / `materialCategoryId` / `warehouseId` / `departmentId` / `projectId`
   - 实体**无 `status` 列、无 `materialId` 列**（仅 `materialCategoryId`；resolver 运行时从 `fact.materialId` 派生 `materialCategoryId`）
   - `id` 为 `seq-default`，平台序列自动生成（种子序列推进至 100000，无碰撞）
2. **缓存自动失效覆盖 `__save`/`__update`/`__delete`**（权威 `ErpFinGlMappingRuleBizModel.java:42-64`）：
   - `defaultPrepareSave/Update/Delete` 三钩子均注册 `txn().afterCommit(null, () -> glMappingResolver.invalidateCache())`（post-commit，仅事务成功提交后失效）
   - `invalidateCache()`（`ErpFinGlMappingResolver.java:124-130`）：`cache.clear()` + `cacheLoaded=false` + 立即 `reloadCache()`（cache-enabled 默认 true）
   - **结论**：spec 经 `ErpFinGlMappingRule__save`/`__delete` 创建/清理规则后**无需手动刷缓存**；手动刷缓存 `@BizMutation` 名为 `ErpFinGlMappingRule__refreshCache`（`:85-89`，本 spec 不需要）
3. **AP_INVOICE 三 accountKey 经 `PurAcctDocProvider.createFacts` setAccountKey 非空**（权威 `docs/design/finance/gl-mapping-rules.md §8.2` + `tests/e2e/orchestration/p2p-chain.spec.ts:70-77` 既有断言 `1403 Dr 50 / 2221 Dr 6.5 / 2202 Cr 56.5`）：
   - accountKey 字面量：`PURCHASE`（Dr 1403 在途物资）/ `INPUT_VAT`（Dr 2221 进项税）/ `ACCOUNTS_PAYABLE`（Cr 2202 应付账款）
   - 三键均在 `erp-fin/account-key` 字典内（dict 校验通过），规则可按 accountKey 精确覆盖单行（仅命中键被覆盖，其余两行保留 Provider 默认）
4. **orgId 维度可参数化裁决**：`runP2pChain`（`orchestration/_helper.ts:262`）固定使用 `SEED.ORG=2` 创建 PO/Receive/Invoice（`orgId: SEED.ORG`），**不可 per-call 参数化**。但 orgId 维度断言无需参数化链路 orgId——通过**规则 orgId 差异化**即可证明：org-dimension-enabled=true（webServer JVM arg 全局）下，建 orgId=2 规则（匹配链路 org=2 → 覆盖）vs orgId=1 规则（非匹配 → 保留默认），同一链路（org=2）双路径即可观测 orgId 维度差异化。org 1（GROUP-HQ）+ org 2（ERP-CO）均存在于种子 `erp_md_organization.csv`，规则 orgId FK 校验通过。
5. **覆盖目标科目选择**：选用种子已有科目 `1401`（原材料，ASSET/DEBIT，`erp_md_subject.csv:9` id=9）作为 PURCHASE 覆盖目标（区别于默认 `1403` 在途物资）。两者同向（DEBIT asset），覆盖后 `resolveSubjects` 的 `code → ErpMdSubject.findByCode` 查找可达（1401 存在于种子），方向语义一致。避免 spec 自行种子新科目（共享 DB 不可幂等）。
6. **fresh-DB 缓存行为**：`@PostConstruct init()`（`ErpFinGlMappingResolver.java:68-78`）启动期 eager load 全表——fresh-DB 无 `erp_fin_gl_mapping_rule.csv` 种子（`app-erp-all/.../_init-data/` 无此文件），启动期 cache 为空（0 规则）。首次 resolve → loadFromCache → 空桶 → null → 保留 Provider 默认（1403）。全局启用 `org-dimension-enabled=true` 对既有 spec **零回归**（空 cache 下 org-dimension 仅影响 cache key 分桶，空集仍空集）。

## Draft Review Record

- Independent draft review iteration 1: needs-revision (ses_0651bf395ffe2d1JcbOUNCPRJZ) because 3 MAJOR 基线事实错误（`status`→实际 `isActive` BOOLEAN；`materialId` 非实体列仅 `materialCategoryId`；helper 第三参 `businessType`→实际 `postingType` 且已支持无需扩展）+ 缓存饥饿加载机制描述错误（`@PostConstruct` 非 fresh-DB 走 DB）+ Phase 2 broad 回归属 Closure Gate。修正：基线字段/缓存/helper 三处对齐实时仓库；移除 o2c-chain 冗余 + helper 扩展；Phase 2 回归移入 Closure Gates
- Independent draft review iteration 2: acceptable-as-is (ses_06515b00cffeb3IF1TY2zp3H7g) — 全部 iteration-1 问题 FIXED（5/5 经实时仓库核实）。1 MAJOR 残留：`ErpFinGlMappingRule__invalidateCache` mutation 名不存在（实际 @BizMutation 名 `refreshCache`）+ 手动刷缓存冗余（`defaultPrepareSave/Update/Delete` 已 post-commit 自动失效）。修正：基线 + Phase 1 + Phase 2 移除手动 invalidateCache 指令，注明 auto-invalidate + 正确 mutation 名 `refreshCache`

## Closure Gates

> 完整仓库验证在此处：结束时运行 `mvn clean install -DskipTests` + 受影响 Playwright 套件一次。

- [x] 范围内行为完成（GL Mapping 命中覆盖 + orgId 维度 + 控制对照三组断言全绿）
- [x] 相关文档对齐（gl-mapping-rules.md + e2e-runbook）
- [x] 已运行验证（`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + `npx playwright test tests/e2e/business-actions/fin-gl-mapping-routing.action.spec.ts` 全绿 + business-actions/orchestration 既有 spec 回归 0 新增失败）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### strict-mode 空匹配抛错浏览器层路径

- Classification: `watch-only residual`
- Why Not Blocking Closure: JUnit `TestErpFinGlMappingResolver` empty-null 场景已覆盖；strict-mode 默认 false，浏览器层复跑低收益
- Successor Required: `no`（触发条件：strict-mode 生产启用 + 空匹配需端到端回归时）

### GL Distribution（科目分摊）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 1351-1 Deferred —— 按 posting.md §FactsValidator 独立扩展点承担，非 accountKey→subjectCode 解析面
- Successor Required: `yes`（触发条件：业务出现按部门分摊金额的合规需求）

## Closure

Status Note: completed — Phase 3 item 2 (e2e-runbook 业务动作表 finance GL Mapping 路由行 + webServer JVM arg `org-dimension-enabled` 段) 已落地；2026-07-26 日志已记录。独立 closure audit 反馈（11 unchecked items → 全勾选 + runbook 文档对齐 + Closure Audit Evidence 补齐）已解决。mission-driver 复验：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + `PLAYWRIGHT_PORT=8011 npx playwright test fin-gl-mapping-routing.action.spec.ts` 2 passed；全 19 项 [x] 一致，6 项 Closure 证据文件经实时仓库核实存在且内容正确。

Closure Audit Evidence:

- Auditor / Agent: 独立 closure audit 子代理（本会话 ses_本轮 audit，非执行者上下文）
- Evidence (spec 落地): `tests/e2e/business-actions/fin-gl-mapping-routing.action.spec.ts`（NEW，7236 bytes，三组断言：命中覆盖 Dr 1401=50 / 控制对照 Dr 1403=50 / orgId 维度 org=1 非匹配保留默认）
- Evidence (owner doc 对齐): `docs/design/finance/gl-mapping-rules.md §9 浏览器层路由验证`（§9.1 全栈路径 + §9.2 三组断言 + §9.3 fresh-DB 缓存行为 + §9.4 orgId 维度断言范式 + §9.5 覆盖目标科目选择，行 490-537）
- Evidence (runbook 对齐): `docs/testing/e2e-runbook.md:322`（业务动作表 finance GL Mapping 凭证科目路由行）+ `:55`（webServer JVM arg 段补 `org-dimension-enabled=true`）
- Evidence (日志): `docs/logs/2026/07-26.md`（plan 0410-1 closure 条目，行 1-8）
- Evidence (仓库验证): `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + `npx playwright test tests/e2e/business-actions/fin-gl-mapping-routing.action.spec.ts` 全绿 + business-actions/orchestration 既有 spec 回归 0 新增失败（Closure Gates 已 `[x]`）

Follow-up:

- 仅非阻塞跟进：strict-mode 浏览器层路径 + GL Distribution（见 Deferred But Adjudicated 段，已确认非缺陷、已裁决）
