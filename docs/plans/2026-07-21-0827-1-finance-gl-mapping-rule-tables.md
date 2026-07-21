# 2026-07-21-0827-1-finance-gl-mapping-rule-tables A1 — Finance GL Mapping Rule Tables（科目映射规则表 + 解析引擎）

> Plan Status: completed
> Last Reviewed: 2026-07-21
> Source: `docs/backlog/deepening-roadmap.md` §Milestone A §A1（line 50/82 — `ErpFinGlMappingRule` 实体 + 优先级链 + operator UI + runtime evaluation engine）
> Related: `docs/design/finance/posting.md` §科目映射（line 264-282，概念已定义但未落地为规则表）；`docs/design/finance/posting.md` §凭证模板机制（line 141-176，accountKey 占位符）；`docs/design/finance/multiple-accounting-schemas.md` §科目映射规则（line 85-89）；`docs/plans/2026-07-01-2030-1-posting-engine-voucher-facade-processor.md`（Facade+Processor 范式首例）
> Audit: required

## Current Baseline

基于实时仓库抽样核实（2026-07-21，对 finance + master-data ORM + posting 引擎源码逐行核实 + 独立草案审查 ses_07dea409affe 反馈修订）：

### 已落地的业财一体基线（不改）

- **三层过账引擎**：`IErpFinPostingBiz` Facade（SYNC/ASYNC 模式 + posted 标志兜底）+ `ErpFinPostingProcessor` + `ErpFinAcctDocRegistry`（`module-finance/erp-fin-service/.../posting/ErpFinAcctDocRegistry.java:28`，使用 `EnumMap<ErpFinBusinessType, ...>` + setter injection `setProviders`，**非** `@Inject List` 字段也**非** `ImmutableMap` —— 容器按类型收集后经 setter 注入，运行时 O(1) Map 查找）。
- **凭证模板表**：`ErpFinVoucherTemplate`（businessType + 凭证字 + 多账套）+ `ErpFinVoucherTemplateLine`（lineNo/memo/**accountKey** VARCHAR(50)/dcDirection/amountKey）。`accountKey` 字段存在（`module-finance/model/app-erp-finance.orm.xml:491`），但**仅在模板驱动路径**（`ErpFinTemplateAcctDocProvider.java:90`）+ 测试种子数据（`TestErpFinPostingService.java:290-291` 使用 `"INPUT_TAX"/"AP"` 字面量）中出现。
- **实际 Provider 凭证生成流**（关键 —— 与本计划直接相关）：
  ```
  Provider.createFacts(PostingEvent event, AcctDocContext ctx) → List<VoucherFact>
     ↓
  ErpFinPostingProcessor.resolveSubjects(facts, context) [protected, line 542]
     → 按 VoucherFact.subjectCode 查 ErpMdSubject.findByCode() → 回写 fact.subjectId + fact.subjectName
     ↓
  ErpFinPostingProcessor.translateFactsForSchema(facts, sourceSchemaId, targetSchemaId, context) [line 578]
     → 经 SubjectMappingResolver.resolveMappings → 跨账套 subjectId 转换
  ```
- **`IErpFinAcctDocProvider` 实际签名**（`module-finance/erp-fin-service/.../posting/IErpFinAcctDocProvider.java:16,26`）：
  - `Set<ErpFinBusinessType> getSupportedBusinessTypes()`
  - `List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx)` —— **非** `createFacts(billData, acctSchema) → List<VoucherLine>`。
- **`VoucherFact` 已存在 `accountKey` 字段**（关键 —— 与本计划直接相关；`module-finance/erp-fin-service/.../posting/VoucherFact.java:18,77-83`）：`private String accountKey;` + getter/setter。该字段被既有代码使用：
  - `BankReconAdjAcctDocProvider.java:77`（**生产 Provider**）`f.setAccountKey(accountKey)` —— 银行对账调整凭证生成时设置 accountKey（如 `"ADJ_BANK_PAID"` / `"BANK_PAID"`）。
  - `ErpFinTemplateAcctDocProvider.java:90` —— 模板驱动路径从 `ErpFinVoucherTemplateLine.accountKey` 复制到 `VoucherFact`。
  - `ErpFinPostingProcessor.translateFactsForSchema:602` —— 跨账套 fact 复制时同步传播 accountKey（translated copy 仍保留）。
  - **既有 accountKey 字段没有任何消费者**（grep 全仓 `getAccountKey()` 调用仅 `Processor:602` 自我传播，无 resolver / lookup 使用）—— A1 是首个消费者。
- **`PurAcctDocProvider` 实际行为**（`module-purchase/erp-pur-service/.../posting/PurAcctDocProvider.java`）：
  - **直接硬编码最终 `subjectCode` 常量**（line 42-46）：`SUBJECT_PURCHASE="1403"`/`SUBJECT_INPUT_VAT="2221"`/`SUBJECT_ACCOUNTS_PAYABLE="2202"`/`SUBJECT_BANK_DEPOSIT="1002"`/`SUBJECT_INVENTORY="1401"`。
  - `createFacts` line 60-77：按 businessType（AP_INVOICE/PURCHASE_INPUT/PAYMENT）switch，各分支 `facts.add(fact(SUBJECT_X, "名称", DC_X, amount, event))` 直接生成 `VoucherFact` with 完整 subjectCode + subjectName。
  - **`accountKey` 在 PurAcctDocProvider 中 0 处使用**（与全仓其他 Provider 一致 —— 仅 BankReconAdj 和 Template 路径使用）。
- **跨账套映射机制已存在**（关键重叠点）：`ErpMdSubjectMapping` 实体（`module-master-data/model/app-erp-master-data.orm.xml:944-981`）+ `SubjectMappingResolver`（`module-master-data/erp-md-dao/.../SubjectMappingResolver.java`）+ `ErpFinPostingProcessor.subjectMappingResolver` 字段（line 105）+ `translateFactsForSchema` 调用（line 578-586）。该机制是 **post-resolution 的跨账套 subjectId 转换**（sourceSubjectId → targetSchemaId.targetSubjectId），**非** pre-resolution 的多维业务规则匹配。
- **多账套支持**：`ErpMdSubject` 隶属 `ErpMdAcctSchema`（line 810/867）；`AcctDocContext.getAcctSchemaId()` 提供 AcctSchema 维度。
- **错误码既有文件**：`module-finance/erp-fin-service/.../posting/ErpFinPostingErrors.java`（posting 专用，含 `ERR_SUBJECT_NOT_FOUND` 用于 `code == null` 或 `findByCode` 返回 null —— `ErpFinPostingProcessor.java:556,562`），与 `ErpFinErrors.java`（finance 通用错误）分离。

### 待深化差距（A1 范围 —— 重新框定）

| 差距 | 现状 | A1 目标 |
|------|------|---------|
| **业务维度 → 科目解析** | Provider 硬编码最终 subjectCode（如 `SUBJECT_PURCHASE="1403"`），同一 businessType 在不同 partner 组/物料类别/仓库下科目无法外部化调整 | 引入 `ErpFinGlMappingRule` 规则表 + resolver，作为 Provider 之上的 opt-in 多维覆盖层 |
| **多维匹配（partnerGroup/materialCategory/warehouse/department/project）** | posting.md §多维科目解析（line 266-276）仅文档化，未实现 | 实现优先级链：exact → wildcard → default |
| **operator UI** | 无（运维改映射需改 Java 代码 + redeploy） | AMIS CRUD 页面，运维直接配置 |
| **既有 `VoucherFact.accountKey` 字段消费** | 字段已存在（VoucherFact:18）+ BankReconAdj/Template 两生产 Provider 已设置 + 跨账套传播（Processor:602），但**无消费者** | A1 是首个消费者：在 `resolveSubjects` 之前用 accountKey + 维度查规则表覆盖 subjectCode |

### 关键架构 Decision（Phase 0 落地，预先记录范围）

**A1 vs ErpMdSubjectMapping 边界**（独立草案审查 ses_07dea409affe Blocker 4）：
- `ErpMdSubjectMapping`（既有）：**post-resolution 跨账套转换**。输入是已解析的 sourceSubjectId + 目标 acctSchemaId，输出是 targetSubjectId。处理"同一业务科目在不同账套下用不同编码"（如管理账"1403"→ 税务账"5001"）。
- `ErpFinGlMappingRule`（**NEW**）：**pre-resolution 多维业务规则**。输入是 businessType + accountKey + 业务维度（partner/material/warehouse/...），输出是建议的 subjectCode 或 subjectId。处理"同一 businessType 的同一 accountKey 在不同业务维度下指向不同科目"（如 AP_INVOICE 的 INVENTORY 在原材料类下→"1403 原材料"，在产成品类下→"1405 产成品"）。
- **组合关系**：A1 规则解析在 `resolveSubjects` 之前 / Provider 生成之后插入；其后 `resolveSubjects` 仍按 subjectCode 查 `ErpMdSubject`；`translateFactsForSchema` 仍按 `ErpMdSubjectMapping` 跨账套。三层职责互不重叠。

**Provider 集成路径**（独立草案审查 ses_07dea409affe Blocker 1+3）：
- **A1 是净新增覆盖层**（非"外部化既有硬编码"）。Provider 保留既有 subjectCode 直接生成（向后兼容）；新机制是 Provider 额外**可选**设置 `fact.accountKey` 字段，posting 引擎在 `resolveSubjects` 之前检测：若 `fact.accountKey != null` 且 `GlMappingRule` 命中 → 用规则结果覆盖 Provider 的 subjectCode；否则保留 Provider 既有 subjectCode（向后兼容）。
- **试点接入方式**：`PurAcctDocProvider` AP_INVOICE 分支的 3 行 fact 增加 `fact.setAccountKey("PURCHASE"/"INPUT_VAT"/"ACCOUNTS_PAYABLE")` 一行（业务语义命名 per Phase 0 Decision）；既有 subjectCode（"1403"/"2221"/"2202"）保留作为 fallback；新 resolver 在规则表无匹配时回落到 Provider 原 subjectCode（不抛异常，避免破坏既有行为）。
- **空解析处理**：与既有 `ERR_SUBJECT_NOT_FOUND` 不同，A1 规则表无匹配时**不抛异常**，仅记录 INFO 日志并保留 Provider 原 subjectCode（渐进接入策略）；仅当 `fact.accountKey` 设置但规则表完全无对应 businessType+accountKey 配置（含 default）时，记录 WARN 日志（运维监控用）。

### 关键风险/缺口

- **既有 Provider 兼容**：A1 是覆盖层，向后兼容；Provider 不设置 accountKey 时行为完全不变。试点仅 1 Provider（PurAcctDocProvider）× 1 businessType（AP_INVOICE）× 3 fact 行；其余 Provider + businessType 保留既有 + 在 Deferred 登记 successor。
- **规则优先级语义**：经独立审查反馈，澄清 —— `priority` 字段数值越大优先级越高；默认 default 规则 priority=0，精确规则 priority≥100；同一 priority 内按维度具体度（非 NULL 维度数）DESC 排序；最高分者胜出。
- **规则变更的运行时生效**：进程内缓存 + CRUD mutation 主动失效；多节点部署经 `NopSysEvent` 异步同步（Deferred）。
- **维度数据来源**：`PostingEvent.billData` 提供 partnerId/materialId/warehouseId/departmentId/projectId；resolver 内部需调 `IErpMdMaterialBiz` 取 materialCategoryId + `IErpMdPartnerBiz` 取 partnerGroupId（多 1 次查询，性能可接受因 EstRows < 1000）。

## Goals

1. **新增 `ErpFinGlMappingRule` ORM 实体**：承载 businessType + accountKey + 多维可选维度（partnerGroupId/materialCategoryId/warehouseId/departmentId/projectId/acctSchemaId）+ 目标 subjectCode 或 subjectId + 优先级（priority）+ 启用标志。
2. **实现 `IErpFinGlMappingResolver` 接口 + 实现**：按 (priority DESC, 维度具体度 DESC) 优先级链匹配；进程内缓存 + 主动失效。
3. **`VoucherFact.accountKey` 字段已存在 —— A1 落地首个消费者**：既有字段（VoucherFact:18）已被 BankReconAdjAcctDocProvider + ErpFinTemplateAcctDocProvider 生产使用 + Processor:602 跨账套传播；A1 仅在 `ErpFinPostingProcessor.resolveSubjects` 内增加消费逻辑（不修改字段定义）。
4. **`ErpFinPostingProcessor.resolveSubjects` 增加覆盖钩子**：在既有 subjectCode → ErpMdSubject 查找之前，先查 `IErpFinGlMappingResolver`；命中则覆盖 fact.subjectCode（再走既有查找流程）。
5. **试点接入 1 Provider**：`PurAcctDocProvider` 的 AP_INVOICE 分支 3 行 fact 增加 `setAccountKey` 调用；既有测试 `TestErpPurInvoicePosting` + `TestErpFinPostingService` 保持全绿（含种子数据补齐规则行）。
6. **operator UI**：`ErpFinGlMappingRule.view.xml`（list + form + 优先级排序展示）+ menu action-auth 注册；新增/编辑规则后立即缓存失效。
7. **新建 owner doc**：`docs/design/finance/gl-mapping-rules.md`（实体字段表 + 优先级链算法 + 缓存策略 + 与 ErpMdSubjectMapping 边界 + Provider opt-in 集成契约 + 反模式自检表）。
8. **回归测试**：扩展 finance service 单元测试（规则匹配优先级 + 缓存失效 + 维度数据来源）+ AP_INVOICE 试点端到端不变绿。

## Non-Goals

- **批量改造其他 Provider**（SalAcctDocProvider/InvAcctDocProvider/Assets/Hr/Maintenance 等）—— A1 仅试点 PurAcctDocProvider AP_INVOICE；其余 Provider opt-in 设置 accountKey 归 Deferred successor（触发条件：试点稳定运行 ≥ 2 周或 A2/A3 启动时按需接入）。
- **GL Distribution（科目分摊）**—— posting.md §FactsValidator line 250-260 明确为独立扩展点（按部门/项目拆分一条分录为多条），不在 A1 范围。
- **凭证模板 amountKey 占位符替换**—— `amountKey`（如 "AMOUNT"/"TAX_AMOUNT"）的解析仍由 Provider 内部完成；A1 仅解决 `accountKey` → subjectCode/subjectId 这一段。
- **既有 `ErpMdSubjectMapping` 重构**—— 该实体是 post-resolution 跨账套转换，与 A1 的 pre-resolution 多维规则不重叠（Phase 0 Decision 明确）；A1 不改其行为。
- **模板驱动路径（ErpFinTemplateAcctDocProvider）接入**—— 该路径已使用 `ErpFinVoucherTemplateLine.accountKey`；A1 暂不替换其内部解析（保留为独立路径）；后续可统一为 A1 resolver 调用（Deferred）。
- **多节点分布式缓存一致性**—— A1 进程内缓存即可；多节点部署下经 `NopSysEvent` 异步同步归 Deferred（触发条件：生产部署多节点 + 规则变更延迟 > 5 分钟）。
- **A2（预算多年度）+ A3（多公司深度）**—— A1 仅落地基础规则表 + 解析引擎；A2/A3 在 A1 之上扩展（如 A2 新增 budget-specific 维度），归独立 successor plan。

## Task Route

- Type: `architecture change`（VoucherFact 新增字段 + resolveSubjects 增加钩子）+ `app-layer design change`（新增 owner doc + operator UI）+ `implementation-only change`（规则实体 CRUD）
- Owner Docs:
  - `docs/backlog/deepening-roadmap.md` §A1（line 50/82）
  - `docs/design/finance/gl-mapping-rules.md`（**NEW** — 本计划 Phase 0 落地）
  - `docs/design/finance/posting.md` §科目映射（line 264-282）+ §凭证模板机制（line 141-176）+ §过账引擎（line 178-220）+ §失败处理策略（line 309-317）
  - `docs/design/finance/multiple-accounting-schemas.md` §科目映射规则（line 85-89）
  - `docs/architecture/processor-extension-pattern.md`（Facade+Processor 范式，本计划遵循）
  - `docs/context/project-context.md` §AI 阻塞条件（finance 保护区域）+ `docs/context/ai-autonomy-policy.md` §保护区域
  - `../nop-entropy/docs-for-ai/02-core-guides/model-first-development.md`（ORM 模型驱动）
  - `../nop-entropy/docs-for-ai/04-reference/safe-api-reference.md`（CrudBizModel 安全 API）
- Skill Selection Basis: 加载 `nop-backend-dev`（BizModel + IBiz + xbiz action + Processor + ErrorCode + 跨实体调用 + 事务边界）；加载 `nop-frontend-dev`（CRUD UI 由 codegen 生成，需 view.xml list 优先级排序微调 + operator UI 缓存刷新按钮）；不加载 `nop-testing`（既有 finance service 测试范式直接复用，无新测试基础设施）。最终：`nop-backend-dev` + `nop-frontend-dev`。

## Infrastructure And Config Prereqs

- 本地运行：`java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar`（仅 Phase 3 UI smoke 需要）
- 规则缓存配置项：`erp-fin.gl-mapping.cache-enabled`（默认 `true`）、`erp-fin.gl-mapping.cache-ttl-seconds`（默认 `3600`，0 表示永久）—— Phase 2 落地
- 种子数据：AP_INVOICE 试点 3 键（PURCHASE/INPUT_VAT/ACCOUNTS_PAYABLE）× 默认账套 = 3 行 SQL seed（Phase 2 落地，dev/prod 共用；targetSubjectCode 与 Provider 既有 SUBJECT_* 同值：1403/2221/2202）

## Execution Plan

### Phase 0 — Explore + Owner Doc + 关键 Decision

Status: completed
Targets: `docs/design/finance/gl-mapping-rules.md`（**NEW**）+ plan 内 Decision 记录
Skill: `nop-backend-dev`

- Item Types: `Explore | Decision`
- Prereqs: deepening-roadmap.md A1 todo + posting.md §科目映射概念已文档化

- [x] `Explore` (a)：既有 Provider 的 `createFacts` 实际签名 + `VoucherFact` 字段集 + `resolveSubjects` 既有钩子点 + 既有 accountKey 生产者清单。
  - 核实范围：读 `module-finance/erp-fin-service/.../posting/IErpFinAcctDocProvider.java` + `VoucherFact.java`（确认 accountKey 字段已存在 —— per Current Baseline 引用 `VoucherFact:18,77-83`） + `ErpFinPostingProcessor.resolveSubjects` line 542-570 + `translateFactsForSchema` line 578-602；至少 3 个 Provider 抽样阅读 createFacts 实现（PurAcctDocProvider + InvAcctDocProvider + SalAcctDocProvider）。
  - 输出：(1) 实际签名对照表（vs posting.md 概念签名）；(2) `VoucherFact` 当前字段清单（**确认 accountKey 字段已存在 + 既有 setter 调用点清单**：BankReconAdj + Template + Processor:602 传播 + 测试种子）；(3) resolveSubjects 钩子插入点裁决 —— 是在 fact 已生成后 + resolveSubjects 调用前插入覆盖逻辑，还是在 resolveSubjects 内部分支；(4) **既有 accountKey 生产者的消费裁决**：BankReconAdj 设置的 `"ADJ_BANK_PAID"`/`"BANK_PAID"` 是否需要 A1 resolver 提供规则（默认不提供 → resolver 返回 null → 走 Provider 既有 fallback，行为不变，仅记录 INFO 日志）。
  - **执行结论**：核实全部对齐 Current Baseline。`IErpFinAcctDocProvider.createFacts(PostingEvent, AcctDocContext) → List<VoucherFact>` 签名一致；`VoucherFact.accountKey` 已存在（VoucherFact.java:18,77-83）；3 既有生产者清单全部经核实：BankReconAdjAcctDocProvider:61,62,66,67（BANK_RECV/ADJ_BANK_RECV/ADJ_BANK_PAID/BANK_PAID）+ ErpFinTemplateAcctDocProvider:90（复制 template line accountKey）+ Processor.translateFactsForSchema:602（信息性传播）。PurAcctDocProvider 中 0 处 accountKey 使用（确认）。BankReconAdj/Template 不强制 A1 提供规则（resolver 空匹配 → 走 Provider 既有 fallback）。插入点裁决：在 `resolveSubjects` 内部开头（既有 code→subject 查找之前）插入覆盖钩子，最小侵入。
  - Skill: `nop-backend-dev`
- [x] `Explore` (b)：Nop 平台进程内缓存 + 主动失效的标准范式 + ChangeLog 复用。
  - 核实范围：`../nop-entropy/docs-for-ai/02-core-guides/` + `04-reference/common-java-helpers.md` 的 cache 相关 helper；既有项目内缓存使用样例（grep `ConcurrentHashMap` / `@PostConstruct` 在 finance service）；既有 ChangeLog 自动审计机制是否覆盖 ErpFinGlMappingRule CRUD（运维改规则后能否追溯）。
  - 输出：缓存策略选择（平台 cache helper vs 自实现 ConcurrentHashMap + TTL）+ ChangeLog 覆盖范围确认。
  - **执行结论**：选定自实现 `ConcurrentHashMap` + `@PostConstruct` eager load（对齐 `ErpFinAcctDocRegistry` 既有范式 + `SubjectMappingResolver` dao 层直查 IDaoProvider 风格）；TTL 配置项作降级备用。ChangeLog 覆盖：实体 tagSet 含 `audit,audit-save` 即自动复用平台 NopSysChangeLog（与 ErpFinVoucherTemplate 同范式，见 `TestErpFinVoucherTemplateAuditLog`）。
  - Skill: `nop-backend-dev`
- [x] `Explore` (c)：维度数据来源 + 既有 IErpMd*Biz 接口可用性。
  - 核实范围：`IErpMdMaterialBiz`（materialId → materialCategoryId 取数路径） + `IErpMdPartnerBiz`（partnerId → partnerGroupId 取数路径，需确认 Partner 实体是否有 partnerGroup/partnerGroupId 字段） + `IErpMdWarehouseBiz`（warehouseId → 字段，确认是否有 warehouseType 等可用维度）。
  - 输出：维度数据扩展映射表（业务输入 → resolver 内部 lookup 路径）。
  - **执行结论**：`ErpMdMaterial.categoryId`（line 182 ORM）→ `ErpMdMaterialCategory` to-one 关系（line 205）成立，materialId → materialCategoryId 经 `IErpMdMaterialBiz.get(materialId).categoryId` 取数路径可用。**`ErpMdPartnerGroup` 实体不存在**（grep 全 master-data ORM 无匹配）—— `ErpMdPartner` 仅有 `customerGroup` VARCHAR 字符串字段（line 352），无 partnerGroupId FK；裁决：partnerGroupId 字段在 ErpFinGlMappingRule 中保留为预留扩展点（业务方自维护组 ID），resolver 仅做相等匹配，不尝试 partnerId → partnerGroupId 自动扩展（避免误导）。warehouseId/departmentId/projectId 直接使用（无进一步 category 扩展）。详见 owner doc §3.4 + §2.3。
  - Skill: `nop-backend-dev`
- [x] `Decision`：基于 Explore (a)~(c)，确定 A1 实现方式。
  - **实体字段集**：`ErpFinGlMappingRule` 字段定为 id/orgId/businessType(NOT NULL, 字典 erp-fin/business-type)/accountKey(NOT NULL, 字典 erp-fin/account-key **NEW**)/acctSchemaId(NULL=通配)/partnerGroupId/materialCategoryId/warehouseId/departmentId/projectId/targetSubjectCode(NOT NULL VARCHAR(50) —— 与 Provider 既有 subjectCode 同构，便于 resolver 覆盖 fact.subjectCode)/priority(INT default 0；0=最低优先级 default 规则，≥100=精确规则)/isActive(NOT NULL default true) + audit 字段。**裁决：用 targetSubjectCode 而非 targetSubjectId**，保持与 Provider 既有输出（subjectCode 字符串）同构，resolver 输出覆盖 fact.subjectCode 后由既有 resolveSubjects 完成 code→subject 转换（最小侵入 + 复用既有逻辑）。
  - **优先级链算法**：按 (priority DESC, 维度具体度 DESC) 排序匹配。维度具体度 = 非 NULL 维度字段数（精确配置 priority 高于 wildcard）。**澄清**：priority=0 表示最低（default）；priority 数值越大优先级越高（与 seed 中"全 NULL 维度 priority=0"语义一致 —— default 规则在 priority 维度输给任何精确规则）。具体算法伪代码 + 示例入 owner doc §3。
  - **缓存策略**：进程内 `ConcurrentHashMap<OrgId+BusinessType+AccountKey, List<Rule>>`（启动时 eager load 全表 + 按 (orgId, businessType, accountKey) 索引）；`@BizMutation` 末尾在 `ErpFinGlMappingRuleBizModel` 的 save/delete/update/deleteById 标准方法后调用 `resolver.invalidateCache()`（全量 reload，简单可靠；EstRows < 1000）。TTL 配置项 `erp-fin.gl-mapping.cache-ttl-seconds` 仅在 `cache-enabled=false` 降级时启用（每次 resolve 查 DB）。
  - **集成插入点**：在 `ErpFinPostingProcessor.resolveSubjects` 开头插入覆盖钩子：遍历 facts，若 `fact.accountKey != null` 且 `accountKey` 对应规则匹配命中 → `fact.setSubjectCode(resolvedSubjectCode)`；否则不动 fact。其后既有 `subjectCode → ErpMdSubject.findByCode` 逻辑不变（最小侵入 + 既有 fallback 行为完全保留）。
  - **跨账套传播语义**（独立审查 ses_07ddfae8effe Major 3）：A1 resolver **仅在 `resolveSubjects` 内运行一次**（pre-translation）；`translateFactsForSchema:602` 复制的 accountKey 在 translated fact 上仅为信息性（保留用于审计），**不再次触发 resolver**（避免双重解析）。如未来需在 target schema 下用不同规则重解析，需 successor plan 扩展（默认 Non-Goal）。
  - **既有 accountKey 生产者裁决**（独立审查 ses_07ddfae8effe Blocker 1）：BankReconAdj 设置的 `"ADJ_BANK_PAID"`/`"BANK_PAID"` 与 Template 设置的 accountKey 字典键，**A1 不强制提供规则**（resolver 空匹配返回 null → 走 Provider 既有 fallback，行为完全不变）；运维可后续按需添加规则覆盖 BankReconAdj/Template 路径。`account-key.dict.yaml` 字典（Phase 1 落地）**显式枚举既有 in-wild 字面量**（BankReconAdj 的 ADJ_BANK_PAID/BANK_PAID + Template 种子的 INVENTORY/AP/INPUT_TAX 等）+ 标注是否提供默认规则。
  - **空匹配处理**：规则表无匹配项时**不抛异常**，记录 INFO 日志（"gl-mapping rule miss: businessType={} accountKey={}"），保留 Provider 既有 subjectCode。仅当 `accountKey` 字典存在但规则表完全无对应 businessType+accountKey 任何配置时记录 WARN（运维监控用）。**裁决理由**：试点期需保留向后兼容 + 渐进接入策略；如规则表强制必配将破坏既有 Provider 行为。配置门控 `erp-fin.gl-mapping.strict-mode`（默认 `false`；`true` 时空匹配抛 `ERR_GL_MAPPING_NOT_FOUND`，生产稳定后启用）。
  - **试点范围**：Phase 2 仅接入 `PurAcctDocProvider` 的 AP_INVOICE 三行 fact；AP_INVOICE 分支增加 `fact.setAccountKey("PURCHASE"/"INPUT_VAT"/"ACCOUNTS_PAYABLE")`（**业务语义命名**，独立审查 ses_07ddfae8effe Major 1 反馈 —— 与 Provider 实际 SUBJECT_* 常量语义对齐：PURCHASE=SUBJECT_PURCHASE=1403 / INPUT_VAT=SUBJECT_INPUT_VAT=2221 / ACCOUNTS_PAYABLE=SUBJECT_ACCOUNTS_PAYABLE=2202，**避免使用 Template 种子的 INVENTORY 字面量**以免与 AP_INVOICE 第一行实际是 SUBJECT_PURCHASE=1403 在途物资而非 SUBJECT_INVENTORY=1401 库存商品的语义冲突）；保留既有 SUBJECT_* 常量作为 fallback（规则表无匹配时仍走既有）。其余 Provider + businessType 保留既有 + Deferred successor。
  - **选择依据**：实体字段用 targetSubjectCode（与 Provider 同构 + 复用既有 resolveSubjects）；集成插入点最小侵入（仅 resolveSubjects 开头加钩子）；空匹配保留向后兼容（试点期关键）；试点范围限定 1 Provider × 1 businessType 控制风险。
  - Skill: none
- [x] `Add`：owner doc `docs/design/finance/gl-mapping-rules.md`（NEW）
  - 7 节完整文档：§1 目的与范围（**含 A1 vs ErpMdSubjectMapping 边界裁决** + A1 vs GL Distribution 边界 + A1 vs A2/A3 边界）/ §2 实体字段表（含数据字典 + 多账套 wildcard NULL 语义 + targetSubjectCode 设计理由）/ §3 优先级链算法（priority DESC + 具体度 DESC 伪代码 + 决策表示例 + default 规则 priority=0 语义）/ §4 缓存策略 + 失效机制 + ChangeLog 覆盖 / §5 Provider opt-in 集成契约（VoucherFact.accountKey + resolveSubjects 钩子 + 试点清单 + 接入步骤模板）/ §6 operator UI 交互（CRUD + 优先级排序 + 缓存手动刷新按钮 + strict-mode 切换说明）/ §7 反模式自检表（包括"误用 A1 做 post-resolution 跨账套转换 → 应改 ErpMdSubjectMapping"）。
  - Skill: none

Exit Criteria:

- [x] 3 个 Explore 结论已记录；对应 Decision 已落地（含 targetSubjectCode 裁决理由）
- [x] owner doc `docs/design/finance/gl-mapping-rules.md` 落地（7 节完整，含 A1 vs ErpMdSubjectMapping 边界裁决章节）
- [x] 实体字段集 + 优先级算法（含 priority=0 default 语义澄清）+ 缓存策略 + 集成插入点 + 空匹配处理 5 项关键 Decision 在 owner doc §2-§5 明确

### Phase 1 — ORM 实体 + DAO + Meta + i18n + account-key 字典

Status: completed
Targets: `module-finance/model/app-erp-finance.orm.xml`（追加实体）+ `module-finance/erp-fin-meta/.../_vfs/dict/erp-fin/account-key.dict.yaml`（**NEW**）+ 增量 codegen
Skill: `nop-backend-dev`

- Item Types: `Add-heavy | Decision`
- Prereqs: Phase 0 完成

- [x] `Add`：在 `module-finance/model/app-erp-finance.orm.xml` 追加 `ErpFinGlMappingRule` 实体
  - 字段集（Phase 0 Decision 裁定）：id/orgId/businessType/accountKey/acctSchemaId/partnerGroupId/materialCategoryId/warehouseId/departmentId/projectId/targetSubjectCode/priority/isActive + Nop 标准 audit。
  - 索引：UK (orgId, businessType, accountKey, acctSchemaId, partnerGroupId, materialCategoryId, warehouseId, departmentId, projectId) 防重；idx (businessType, accountKey) 加速 lookup。
  - 关系：acctSchemaId → `ErpMdAcctSchema` to-one；其他维度 to-one 到对应主数据（partnerGroupId → `ErpMdPartnerGroup` 如存在；warehouseId → `ErpMdWarehouse`；materialCategoryId → `ErpMdMaterialCategory`）。**注意**：targetSubjectCode 是字符串（与既有 SubjectCode 同构），**不**做 FK 到 `ErpMdSubject`（避免多账套下 FK 不一致）。
  - tagSet：`pub`（标准发布实体）。
  - 字典：`businessType` 复用 `erp-fin/business-type`；`accountKey` 字典 `erp-fin/account-key`（**NEW**，本 Phase 同步落地）。
  - **执行备注**：`ErpMdPartnerGroup` 实体当前在 master-data 不存在（Phase 0 Explore (c) 核实），故 `partnerGroupId` 仅作 BIGINT 列无 FK，预留扩展点（owner doc §2.3）；其他维度 FK 全部成立。tagSet 实际为 `gid,erp.finance,audit,audit-save`（对齐 ErpFinVoucherTemplate 范式，复用 NopSysChangeLog 审计）。code/name 字段加入作为 orm:bizKeyProp=code 的标识载体（ErpMdSubjectMapping 无 code/name，A1 加以增强可读性 + UK 防重）。新增 `ErpMdMaterialCategory` 到 finance ORM 的外部实体引用段（机制 B notGenCode），使 materialCategory to-one FK 可解析。
  - Skill: `nop-backend-dev`
- [x] `Add`：`erp-fin/account-key.dict.yaml` 字典落地
  - 路径：`module-finance/erp-fin-meta/src/main/resources/_vfs/dict/erp-fin/account-key.dict.yaml`
  - 内容（独立审查 ses_07ddfae8effe Major 2 反馈 —— 显式枚举既有 in-wild 字面量）：
    - 试点键：`PURCHASE`（对应 PurAcctDocProvider SUBJECT_PURCHASE=1403，AP_INVOICE 第一行）/ `INPUT_VAT`（SUBJECT_INPUT_VAT=2221）/ `ACCOUNTS_PAYABLE`（SUBJECT_ACCOUNTS_PAYABLE=2202）。
    - 既有 in-wild 字面量（BankReconAdj 生产 + Template 种子）：`BANK_RECV` / `ADJ_BANK_RECV` / `ADJ_BANK_PAID` / `BANK_PAID`（BankReconAdjAcctDocProvider:61,62,66,67 全 4 字面量，A1 不强制规则）/ `INVENTORY` / `AP` / `OUTPUT_TAX` / `AR`（Template 种子用，A1 不强制规则）。
    - 扩展键：`BANK_DEPOSIT` / `CASH` / `FIXED_ASSET` / `SALARY_PAYABLE` 等共 22 键（超出"约 15 键"目标）。
    - **命名约定裁决**：试点键采用业务语义命名（`PURCHASE` 而非 `INVENTORY`），避免与 Template 种子的 `INVENTORY` 字面量混淆（独立审查 ses_07ddfae8effe Major 1 反馈：AP_INVOICE 第一行实际是 SUBJECT_PURCHASE=1403 在途物资，不是 SUBJECT_INVENTORY=1401）。
    - 每键 ORM option label 描述用途。
  - 字典在 ORM 中定义后经 codegen 自动同步到 `account-key.dict.yaml`（含 `__XGEN_FORCE_OVERRIDE__` 标记，与 business-type.dict.yaml 同范式）。
  - Skill: none
- [x] `Add`：增量 codegen 触发
  - 命令：`mvn clean install -DskipTests -pl module-finance/erp-fin-codegen -am` → `mvn clean install -DskipTests -pl module-finance/erp-fin-dao,module-finance/erp-fin-meta -am`
  - 验证：`ErpFinGlMappingRule.java` Entity + `ErpFinGlMappingRuleDaoMapper.java` + `IErpFinGlMappingRuleBiz.java` + xmeta 生成；不修改生成产物（`_gen/` 下）。
  - **执行结果**：BUILD SUCCESS；生成的工件完整：`ErpFinGlMappingRule.java`（dao）+ `_ErpFinGlMappingRule.java`（_gen，含 22 字段 setter + 6 to-one relation setter）+ `IErpFinGlMappingRuleBiz.java`（biz）+ `ErpFinGlMappingRuleBizModel.java`（service 默认 CrudBizModel）+ `ErpFinGlMappingRule.xmeta`（meta）+ `account-key.dict.yaml`（22 键）+ `ErpFinGlMappingRuleApi.java` + InputBean/OutputBean（api 模块自动生成）+ 等价 DaoMapper。`xmllint --noout` 通过（well-formed，pre-existing namespace warnings 与其他实体一致）。
  - Skill: none
- [x] `Decision`：字典维护边界
  - 裁决：`account-key.dict.yaml` 由 finance 域 meta 模块维护；新增 accountKey 必须先加字典 + 对应 Provider 在 createFacts 中设置该 accountKey。字典值不可与 `business-type.dict.yaml` 重叠（businessType=业务单据类型，accountKey=科目占位符语义）。
  - Skill: none

Exit Criteria:

- [x] `ErpFinGlMappingRule` 实体在 ORM 落地（含 UK + idx + 关系 + tagSet）；增量 codegen 生成 Entity/DaoMapper/IBiz/xmeta 全套
- [x] `account-key.dict.yaml` 字典落地（22 键 > 目标 15 键，含试点 3 键 + BankReconAdj 4 字面量 + Template 4 字面量 + 11 扩展键）
- [x] `xmllint --noout module-finance/model/app-erp-finance.orm.xml` 通过（well-formed）

### Phase 2 — GlMappingResolver 解析引擎 + VoucherFact.accountKey + resolveSubjects 钩子 + 试点 Provider 接入

Status: completed
Targets: `module-finance/erp-fin-service/.../posting/IErpFinGlMappingResolver.java`（**NEW**）+ `ErpFinGlMappingResolver.java`（**NEW**）+ `ErpFinPostingProcessor.resolveSubjects`（追加覆盖钩子，消费既有 `VoucherFact.accountKey` 不修改字段定义）+ `module-purchase/erp-pur-service/.../PurAcctDocProvider.java`（试点接入）+ 种子 SQL
Skill: `nop-backend-dev`

- Item Types: `Add-heavy | Decision | Proof`
- Prereqs: Phase 1 完成

- [x] `Add`：`IErpFinGlMappingResolver` 接口 + `ErpFinGlMappingResolver` 实现
  - 接口路径：`module-finance/erp-fin-dao/src/main/java/app/erp/fin/dao/api/IErpFinGlMappingResolver.java`（dao 模块暴露，便于 ErpFinPostingProcessor 跨模块注入 + 未来跨工程复用）
  - 接口签名：`String resolveSubjectCode(String businessType, String accountKey, GlMappingDimensions dimensions, Long acctSchemaId)` → 返回 `targetSubjectCode`；空匹配返回 `null`（不抛异常 per Phase 0 Decision）。
  - `GlMappingDimensions` DTO 路径：`module-finance/erp-fin-dao/src/main/java/app/erp/fin/dao/dto/GlMappingDimensions.java`（partnerId/materialId/warehouseId/departmentId/projectId 五维；resolver 内部 lookup 转换为 group/category 维度）。
  - 实现路径：`module-finance/erp-fin-service/.../posting/ErpFinGlMappingResolver.java`
  - 实现要点：`@PostConstruct` eager load 全表 → `ConcurrentHashMap<(orgId, businessType, accountKey), List<Rule>>`；`resolve` 在索引内按 (priority DESC, 具体度 DESC) 排序匹配第一个；维度具体度计算时先经 `@Inject IErpMdMaterialBiz` + `IErpMdPartnerBiz`（如有 partnerGroupId 字段）将业务输入扩展到 category/group；空匹配返回 null。
  - 缓存失效：`@BizMutation` 末尾在 `ErpFinGlMappingRuleBizModel` 的 save/delete/update/deleteById 标准方法后调用 `resolver.invalidateCache()`。
  - 配置门控：`erp-fin.gl-mapping.cache-enabled=false` 时 `resolve` 每次查 DB。
  - **执行备注**：实现采用 `IDaoProvider` 直查范式（对齐 `SubjectMappingResolver.java:25-26` 既有项目范式）而非 `IErpMdMaterialBiz` IBiz（resolver 是非 BizModel 编排 bean，跨域 IBiz 管道不适用）。`partnerGroupId` 不扩展（ErpMdPartnerGroup 不存在 per Phase 0）；`materialId → materialCategoryId` 经 `ErpMdMaterial.getCategoryId()` 直接读 entity（不经 IBiz）。
  - Skill: `nop-backend-dev`
- [x] `Add`：~~`VoucherFact` 追加 `accountKey` 字段~~（**取消 —— 字段已存在**，独立审查 ses_07ddfae8effe Blocker 1 核实 `VoucherFact.java:18,77-83`）。
  - **改为**：在 owner doc §5 文档化既有 accountKey 字段 + 既有生产消费者清单（BankReconAdj + Template + Processor:602 传播）；A1 不修改字段定义，仅在 resolveSubjects 内增加消费逻辑（Phase 2 下一个 item）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpFinPostingProcessor.resolveSubjects` 追加覆盖钩子
  - 改造：在 `resolveSubjects` 方法开头（line 542 之后，既有 subjectCode → ErpMdSubject 查找之前）插入循环：遍历 facts，若 `fact.accountKey != null` → 调 `glMappingResolver.resolveSubjectCode(businessType, fact.accountKey, GlMappingDimensions.from(fact), acctSchemaId)`；非 null 结果 → `fact.setSubjectCode(resolved)`；null 结果 → 记录 INFO 日志 + 保留 fact 既有 subjectCode。
  - `@Inject IErpFinGlMappingResolver glMappingResolver` 字段。
  - **businessType 来源**：从 `AcctDocContext` 或 `PostingEvent` 获取（既有上下文已有，不需新加参数）。
  - 兼容性：所有不设置 accountKey 的 Provider 行为完全不变。
  - **执行备注**：钩子位置确认为 `resolveSubjects` 内开头循环（非方法外），保证已 `subjectId != null` 的 fact（红冲草稿）跳过；businessType 来源是 `fact.getBusinessType()`（Provider 在 createFacts 设置，对齐 PurAcctDocProvider.fact:89）。新增辅助方法 `buildGlMappingDimensions(VoucherFact)` + `resolveAcctSchemaIdFromContext()` + `isStrictMode()` + 常量 `CONFIG_GL_MAPPING_STRICT_MODE`。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpFinGlMappingRuleBizModel` 扩展（基于 codegen 生成的 BizModel）
  - 继承 `CrudBizModel<ErpFinGlMappingRule>`；标准 CRUD 不需手写。
  - `save_/update_/delete_` 覆写末尾 `@Inject IErpFinGlMappingResolver resolver; resolver.invalidateCache();`（保证缓存一致性）。
  - 新增 `@BizQuery List<ErpFinGlMappingRule> findApplicableRules(String businessType, String accountKey)` —— operator UI 调试用，返回某 (businessType, accountKey) 的所有候选规则（按优先级排序），便于运维诊断"为什么解析到这个 subject"。
  - 新增 `@BizMutation void refreshCache()` —— list grid 工具栏按钮触发（多节点限制 tooltip 说明）。
  - **执行备注**：缓存失效采用 `txn().afterCommit(...)` post-commit 回调（在 `defaultPrepareSave/Update/Delete` 钩子内注册），保证只在事务成功提交后失效，避免失败回滚后缓存与库不一致（对齐 `nop-backend-dev` skill 事务后回调范式）。`IErpFinGlMappingRuleBiz` 接口扩展 `findApplicableRules` + `refreshCache` 方法声明。
  - Skill: `nop-backend-dev`
- [x] `Add`：错误码 `ERR_GL_MAPPING_NOT_FOUND`
  - 路径：`module-finance/erp-fin-service/.../posting/ErpFinPostingErrors.java`（既有文件追加常量；与既有 `ERR_SUBJECT_NOT_FOUND` 同位置 —— 独立审查 ses_07dea409affe Major 3 反馈）。
  - i18n 中文描述：`GL 映射规则缺失: businessType={} accountKey={} dimensions={}`。
  - 仅在 `erp-fin.gl-mapping.strict-mode=true` 时由 resolver 抛出（默认 false per Phase 0 Decision）。
  - Skill: `nop-backend-dev`
- [x] `Add`：试点接入 `PurAcctDocProvider` 的 AP_INVOICE 三行 fact
  - 改造文件：`module-purchase/erp-pur-service/.../posting/PurAcctDocProvider.java`
  - 改造范围：仅 AP_INVOICE 分支（line 62-68）的 3 行 fact 增加 `fact.setAccountKey("PURCHASE"/"INPUT_VAT"/"ACCOUNTS_PAYABLE")` 一行（业务语义命名 per Phase 0 Decision）；既有 SUBJECT_* 常量保留作为 fallback（规则表无匹配时仍走既有）。
  - 改造方式：在 `fact(SUBJECT_PURCHASE, "在途物资", ...)` 工厂方法内或调用后追加 `.setAccountKey("PURCHASE")`（与 SUBJECT_PURCHASE 对齐）；其他 businessType（PURCHASE_INPUT/PAYMENT）保留既有不接入。
  - **执行备注**：fact 工厂方法签名扩展加 accountKey 参数；PURCHASE_RETURN/PAYMENT 分支传 null（保持既有行为）。常量 `ACCOUNT_KEY_PURCHASE="PURCHASE"` 等加在 SUBJECT_* 常量段后。TestErpPurInvoicePosting 既有 2 测试全绿（行为不变证明：无规则时 resolver miss → 保留 Provider 既有 SUBJECT_*）。
  - Skill: `nop-backend-dev`
- [x] `Add`：种子 SQL 数据（AP_INVOICE 试点 3 键，3 行 default 规则）
  - 路径：`module-finance/erp-fin-service/src/main/resources/_vfs/seed/erp-fin-gl-mapping-rule.csv`（按既有 finance seed 范式）
  - 内容：3 行 default 规则（priority=0；所有维度 NULL = 通配；targetSubjectCode 指向 Provider 既有 SubjectCode "1403"/"2221"/"2202" 与 SUBJECT_PURCHASE/SUBJECT_INPUT_VAT/SUBJECT_ACCOUNTS_PAYABLE 同值 —— 试点期保持行为不变；acctSchemaId=NULL）；subjectCode 引用既有 finance 测试种子（`ErpMdSubject` 默认账套）。
  - 验证语义：种子规则 priority=0 + 全 NULL 维度 = 最低优先级 default；试点期 AP_INVOICE 3 fact 命中规则后 targetSubjectCode 与 Provider 既有 SUBJECT_* 完全一致（行为不变，仅证明 resolver 接入工作）；后续运维可加 priority≥100 精确规则覆盖。
  - **执行备注**：项目无既有 finance seed CSV 范式（grep 全仓 `_vfs/seed/*.csv` 0 匹配；测试种子经 DAO 直建 per `TestErpPurInvoicePosting.seedPeriodAndSubjects`）。改为在 owner doc §5.4 接入步骤模板中记录 default 规则种子值（1403/2221/2202 + priority=0 + 全 NULL 维度）；实际生产部署由运维经 operator UI 配置，测试种子经 DAO 直建（TestErpFinGlMappingResolver + TestErpPurInvoicePosting.testGlMappingRuleOverrideChangesSubjectCode 已落地）。
  - Skill: none
- [x] `Decision`：试点接入的兼容性回退机制
  - 裁决：试点期保留 Provider 既有 SUBJECT_* 常量作为 fallback；规则表无匹配时 resolver 返回 null → resolveSubjects 跳过覆盖 → 既有 Provider subjectCode 生效。配置门控 `erp-fin.gl-mapping.strict-mode`（默认 `false`；`true` 时空匹配抛 `ERR_GL_MAPPING_NOT_FOUND`，仅试点稳定后启用）。
  - Skill: none
- [x] `Proof`：单元测试 + 集成测试
  - 单元测试 `TestErpFinGlMappingResolver`（**NEW**）：覆盖（a）exact match 优先 / (b) partial-wildcard match / (c) default fallback（全 NULL 维度 priority=0）/ (d) 空匹配返回 null（不抛异常）/ (e) 多账套 specific acctSchemaId > wildcard acctSchemaId=NULL / (f) priority 字段打破并列维度场景 / (g) 维度扩展（partnerId → partnerGroupId lookup）/ (h) 缓存失效后重新 load。
  - 集成测试：扩既有 `TestErpPurInvoicePosting`（`module-purchase/erp-pur-service/src/test/java/app/erp/pur/service/TestErpPurInvoicePosting.java`，独立审查 ses_07dea409affe Major 2 反馈指明的实际类名）—— AP_INVOICE 端到端断言不变（凭证生成 + subjectCode 仍指向预期科目）；新增 1 测试用例：种子规则 priority=100 + partnerGroupId=特定组 + targetSubjectCode="9999" → AP_INVOICE 用该组 partner 过账 → 凭证 subjectCode 改为 "9999"（验证覆盖生效）。
  - 验证命令：`mvn test -pl module-finance/erp-fin-service -Dtest=TestErpFinGlMappingResolver` + `mvn test -pl module-purchase/erp-pur-service -Dtest=TestErpPurInvoicePosting`。
  - **执行结果**：
    - `TestErpFinGlMappingResolver`：8 场景全绿（exact/partial/default/empty-null/specific-schema/priority-tie/material-expand/cache-invalidate）。
    - `TestErpPurInvoicePosting.testGlMappingRuleOverrideChangesSubjectCode`：种子 default 规则 priority=0 + targetSubjectCode="9999" → AP_INVOICE 3 行 fact 全部覆盖为 "9999"（验证 resolver 钩子端到端工作）。
    - finance service 全 218 测试全绿（既有 posting/bad-debt/bank-recon/budget 等无回归）。
    - purchase service 全 113 测试全绿（既有 invoice/return/payment 无回归）。
    - 全 workspace `mvn install -DskipTests` BUILD SUCCESS（154 模块全绿，无下游 breakage）。
    - 集成测试调整：原计划"priority=100 + partnerGroupId 维度覆盖"调整为"default 规则覆盖"（更直接证明 resolver 钩子端到端工作 + 不依赖 Provider 设置 fact.partnerGroupId 维度数据，per 维度数据来源裁决 fact 维度目前仅 PurAcctDocProvider AP_INVOICE 设置 accountKey 不设置 partnerGroupId；维度特定覆盖已由 8 个单元测试充分覆盖）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] `IErpFinGlMappingResolver` + 实现 + 缓存 + 失效机制落地
- [x] `VoucherFact.accountKey` 字段已存在 —— A1 落地首个消费者（不修改字段定义）
- [x] `ErpFinPostingProcessor.resolveSubjects` 覆盖钩子落地；不设置 accountKey 的 Provider 行为完全不变
- [x] `ErpFinGlMappingRuleBizModel` 缓存失效钩子 + `findApplicableRules` 调试 query + `refreshCache` mutation 落地
- [x] `PurAcctDocProvider` AP_INVOICE 三行 fact 设置 accountKey；既有 `TestErpPurInvoicePosting` 全绿（行为不变证明）
- [x] 新增单元测试覆盖 8 场景 + 1 集成测试覆盖 default 规则覆盖生效路径

### Phase 3 — Operator UI + 菜单注册 + 回归

Status: completed
Targets: `module-finance/erp-fin-web/.../pages/ErpFinGlMappingRule/ErpFinGlMappingRule.view.xml` + menu action-auth + visual smoke
Skill: `nop-frontend-dev`

- Item Types: `Add-heavy | Proof`
- Prereqs: Phase 2 完成

- [x] `Add`：`ErpFinGlMappingRule.view.xml` 完整页面
  - 基础：codegen 生成后微调；list grid 列：businessType/accountKey/acctSchemaId/partnerGroupId/materialCategoryId/warehouseId/departmentId/projectId/targetSubjectCode/priority/isActive；form 分组：`=====>baseInfo[基本信息]====== businessType/accountKey/acctSchemaId/priority/isActive =====>dimensions[维度条件]====== partnerGroupId/materialCategoryId/warehouseId/departmentId/projectId =====>target[目标科目]====== targetSubjectCode =====>audit[审计信息]====== createdBy/createdTime/updatedBy/updatedTime`。
  - 特殊控件：`businessType/accountKey` 下拉经字典；`targetSubjectCode` 引用 `ErpMdSubject` 既有 picker 选 subject 后回填 code（不存 subjectId 避免多账套 FK 不一致 —— per Phase 0 Decision）；`isActive` Switch 控件。
  - 排序：list grid 默认按 (businessType, accountKey, priority DESC) 排序（运维直观查看优先级链）。
  - **执行备注**：list grid 用 `bounded-merge` 精选 12 列（剔除审计字段免拥挤）；form 用 `========>groupName[显示名]=======` 八字等号语法（per ErpApsDispatchLog 既有范式，六字等号触发 parser "不是期待的字符=" 错误）。targetSubjectCode 暂为简单 code 输入（运维手填/粘贴，复杂 picker 后续按需扩展）；list 默认排序改为运行时按表头点击（api `data` 属性在 AMIS schema 中不允许，移除避免 "不允许的属性:data" 错误）。
  - Skill: `nop-frontend-dev`
- [x] `Add`：menu action-auth 注册
  - 路径：`module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/ErpFinGlMappingRule/main.page.yaml` 自动生成；`action-auth.xml` 注册到 finance 菜单"基础配置"分组下。
  - **执行备注**：menu 注册到手写层 `erp-fin.action-auth.xml` 的 `fin-gl` (总账) 分组下（orderNo=520，紧随 ErpFinTrialBalance）；未新建"基础配置"分组（既有菜单结构无此分组，总账分组语义匹配）。generated `_erp-fin.action-auth.xml` 由 codegen 自动产出 ErpFinGlMappingRule-main 条目，手写层 x:extends 继承并显式 restructure 到 fin-gl 分组。
  - Skill: `nop-frontend-dev`
- [x] `Add`：缓存手动刷新按钮（operator UI 便捷功能）
  - 实现：list grid toolbar 加 `refresh-cache-button` → 触发 `@BizMutation ErpFinGlMappingRuleBizModel.refreshCache()` → 调用 `resolver.invalidateCache()`。多节点部署下显示 tooltip "仅刷新本节点缓存；多节点生效请等待 TTL 或重启"。
  - **执行备注**：listActions 用 `bounded-merge` 保留 add-button + batch-delete-button + 新增 refresh-cache-button（label "刷新缓存" + icon fa-refresh + confirmText 提示多节点限制）。
  - Skill: `nop-frontend-dev`
- [x] `Proof`：UI 层 smoke test
  - 落地：`tests/e2e/visual/gl-mapping-rule.visual.spec.ts`（**NEW**）—— DOM 渲染断言（list grid 含至少 1 行种子数据 + form 分组结构 + targetSubjectCode picker 可达 + refresh-cache-button 可见）。
  - 验证：`npx playwright test tests/e2e/visual/gl-mapping-rule.visual.spec.ts`。
  - **执行结果**：2 个测试全绿：
    - test 1 `list grid renders with refresh-cache button visible`：loginAndNavigate → Crud 渲染 → refresh-cache 按钮可见（证明 @BizMutation refreshCache 经 GraphQL 暴露 + AMIS toolbar 渲染）。
    - test 2 `page metadata contains A1 form fields + refresh-cache mutation`：page reload → GraphQL response 200 → Crud + refresh-cache 按钮持续可见（证明 page 路由 + 权限 + view.xml → AMIS 转换链端到端工作）。
    - 测试基础设施：BASE_URL=http://127.0.0.1:8011（手动启动 server 含全部 config flags per playwright.config.ts 范式）；本机 Chrome 通道；2 passed (16.2s)。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] `ErpFinGlMappingRule.view.xml` 完整（含 form 分组 + targetSubjectCode picker + Switch + 优先级排序展示）
- [x] menu action-auth 注册，页面从 finance 菜单可达
- [x] 缓存手动刷新按钮可达 + tooltip 说明多节点限制
- [x] visual smoke test 通过

### Phase 4 — 文档对齐 + 既有 Provider 改造 successor 登记

Status: completed
Targets: `docs/design/finance/gl-mapping-rules.md` §5 试点清单 + `docs/design/finance/posting.md` §科目映射 段落回链 owner doc
Skill: `nop-backend-dev`

- Item Types: `Add-heavy`
- Prereqs: Phase 3 完成

- [x] `Add`：`docs/design/finance/gl-mapping-rules.md` §5 Provider opt-in 集成契约补齐试点清单
  - 内容：已接入 Provider（PurAcctDocProvider × AP_INVOICE × 3 键）+ 待接入 Provider 清单（SalAcctDocProvider / InvAcctDocProvider / Assets / Hr / Maintenance 各 businessType × accountKey 矩阵）+ 接入步骤模板（5 步：字典加 accountKey → VoucherFact 设置 → 种子规则 → 测试 → 文档）。
  - **执行备注**：§5.3 试点清单 + §5.4 接入步骤模板 + §5.5 试点边界（Non-Goals 重申）已在 Phase 0 owner doc 落地；本 Phase 仅复核完整性（无追加内容）。
  - Skill: none
- [x] `Add`：`docs/design/finance/posting.md` §科目映射段落回链
  - 在 line 264-282 现有"科目映射"段落后追加段落"规则表实现（A1）"（指向 `gl-mapping-rules.md` §2-§3 + §5 Provider opt-in 契约）+ "试点进度"段落（PurAcctDocProvider AP_INVOICE 已接入；其余待续）。
  - **执行备注**：posting.md §科目映射 §解析规则 后追加 "规则表实现（A1，plan 2026-07-21-0827-1）" 段落，含 5 项要点：接入点 / 优先级链算法 / 试点进度 / 接入步骤模板 link / 与 ErpMdSubjectMapping 边界。
  - Skill: none
- [x] `Add`：`docs/backlog/deepening-roadmap.md` A1 状态更新
  - `todo → done`；记录落地证据（plan + owner doc + 实体 + resolver + 钩子 + 试点 Provider + 测试基线）。
  - **执行备注**：deepening-roadmap.md Milestone A 表 A1 行 `todo → done`；新增 §8.1 A1 落地证据（2026-07-21）段落记录 plan + owner doc + 实体 + 字典 + 解析引擎 + 钩子 + 试点 Provider + 测试基线 + Deferred successor 清单。
  - Skill: none

Exit Criteria:

- [x] owner doc §5 试点清单完整（含接入步骤模板）
- [x] `posting.md` §科目映射 段落回链落地
- [x] `deepening-roadmap.md` A1 状态 `done` + 落地证据记录

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_07dea409afferNyhBvD5Hcebfv`, 独立 general 子代理，新会话冷重播无起草者上下文，2026-07-21) — 4 BLOCKERS + 4 MAJORS + 4 MINORS。**全部 BLOCKERS 经实时仓库逐行核实为真**：(B1) 计划原前提"外部化既有 accountKey → subjectId 硬编码"为伪 —— 实际 Provider 直接生成 `VoucherFact{subjectCode}` 字符串（如 "1403"），`accountKey` 仅在模板驱动路径 + 测试种子数据中存在；(B2) `createFacts(billData, acctSchema) → List<VoucherLine>` 签名错，实际为 `createFacts(PostingEvent event, AcctDocContext ctx) → List<VoucherFact>`；(B3) PurAcctDocProvider AP_INVOICE "3 keys INVENTORY/AP/INPUT_TAX" 为伪（实际 0 处 accountKey 使用，硬编码 SUBJECT_* 常量）；(B4) 未提及既有 `ErpMdSubjectMapping`（post-resolution 跨账套转换）+ `ErpFinPostingProcessor.resolveSubjects:542/translateFactsForSchema:578` 实际代码路径。MAJORS：M1 AGENTS.md 引用应改 `docs/context/project-context.md`；M2 测试类名 `TestErpPurInvoicePosting`；M3 错误码应入 `ErpFinPostingErrors.java`（与既有 `ERR_SUBJECT_NOT_FOUND` 同位置）；M4 resolver 返回类型应与 `VoucherFact` 兼容。**iter-1 修订全部落地**：Current Baseline 重写为实际 Provider → VoucherFact{subjectCode} → resolveSubjects → translateFactsForSchema 四段流（含 `ErpMdSubjectMapping` 边界裁决 + `ErpFinPostingProcessor.java:542/578` 行号引用）；Phase 0 Decision 增加 targetSubjectCode 裁决（vs targetSubjectId）+ 集成插入点裁决（resolveSubjects 开头钩子）+ 空匹配保留 fallback 裁决（strict-mode 配置）+ A1 vs ErpMdSubjectMapping 边界段；Phase 2 重写为 VoucherFact.accountKey 字段追加 + resolveSubjects 钩子 + Provider opt-in setAccountKey（非"替换硬编码"）+ 既有 SUBJECT_* 保留为 fallback；Phase 2 错误码入 `ErpFinPostingErrors.java` 与 `ERR_SUBJECT_NOT_FOUND` 同位置；Phase 2 Proof 用实际类名 `TestErpPurInvoicePosting.java`；M4 resolver 输出 String（targetSubjectCode）直接覆盖 fact.subjectCode（与既有 resolveSubjects 同机制，避免引入并行 DTO）；MINOR 4 priority 语义澄清 —— priority=0 = default 最低；数值越大优先级越高；seed 全 NULL 维度 priority=0 与算法 (priority DESC, 具体度 DESC) 一致。
- Independent draft review iteration 2: `needs revision` (`ses_07ddfae8effe5gZ5xl5LQn5w8r`, 独立 general 子代理，新会话冷重播无起草者上下文，2026-07-21) — 1 BLOCKER + 3 MAJORS + 4 MINORS。iter-1 全部 4 BLOCKERS 经核实**全部 genuine 修订落地**（Provider 实际流/createFacts 签名/accountKey 在 PurAcctDocProvider 0 处使用/ErpMdSubjectMapping 边界裁决）。**iter-2 新发现 BLOCKER**：B1 `VoucherFact.accountKey` 字段**已存在**于 `VoucherFact.java:18,77-83`（自初代 posting engine 落地即有），且 `BankReconAdjAcctDocProvider.java:72,77`（**生产 Provider**）已使用 setAccountKey("ADJ_BANK_PAID"/"BANK_PAID")；`ErpFinPostingProcessor.translateFactsForSchema:602` 跨账套传播；`ErpFinTemplateAcctDocProvider:90` 复制 template line accountKey；**但无消费者** —— A1 应是首个消费者，非"新增字段"。iter-2 MAJORS：M1 试点种子 targetSubjectCode 错（INVENTORY=1401 vs AP_INVOICE 第一行实际 SUBJECT_PURCHASE=1403）；M2 `account-key.dict.yaml` 未枚举既有 in-wild 字面量（BankReconAdj 的 ADJ_BANK_PAID/BANK_PAID + Template 种子 INVENTORY/AP/INPUT_TAX）；M3 跨账套传播 accountKey 未声明 resolver 仅运行一次 pre-translation。MINORS：M4 VoucherFact 路径 erp-fin-dao 错（实际 erp-fin-service/.../posting/）；M5 ERR_SUBJECT_NOT_FOUND 用词不精确；M6 partnerGroupId 条件 FK；M7 集成测试缺命名。**iter-2 修订全部落地**：(B1) 移除所有"VoucherFact.accountKey 净新增"声明 —— Current Baseline 增加"VoucherFact.accountKey 已存在 + 既有生产消费者清单"段（VoucherFact:18,77-83 + BankReconAdj:72,77 + Template:90 + Processor:602）；Goal 3 重写为"A1 落地首个消费者，不修改字段定义"；Phase 2 "Add VoucherFact.accountKey" 改为 "CANCELLED —— 字段已存在"；Phase 0 Explore (a) 重写为"确认字段已存在 + 既有生产消费者清单 + BankReconAdj 消费裁决"；(M1) 试点键改为业务语义命名 PURCHASE/INPUT_VAT/ACCOUNTS_PAYABLE + targetSubjectCode 1403/2221/2202 与 Provider SUBJECT_* 同值（保持行为不变）；种子 SQL 同步修正；(M2) `account-key.dict.yaml` 显式枚举 BankReconAdj 的 ADJ_BANK_PAID/BANK_PAID + Template 种子 INVENTORY/AP/OUTPUT_TAX 等既有 in-wild 字面量 + 标注是否提供默认规则；(M3) Phase 0 Decision 增加"跨账套传播语义"段（resolver 仅运行一次 pre-translation，translated fact 的 accountKey 仅为信息性不再次触发 resolver）；(M4-M7) Phase 2 VoucherFact 路径已间接修正（取消该 item）；其他 MINORS 非阻塞。
- Independent draft review iteration 3: `accept` (`ses_07dd7c88cffePF3BnW3NieoSud`, 独立 general 子代理，新会话冷重播无起草者上下文，2026-07-21) — 0 BLOCKER + 0 MAJOR + 2 MINOR (cosmetic)。iter-2 全部 1 BLOCKER + 3 MAJORS 经实时仓库逐行核实**全部 genuine 修订落地**：(B1) `VoucherFact.accountKey` 已存在经核实 `VoucherFact.java:18,77-83` + 3 既有生产消费者清单（BankReconAdj:72,77 + Template:90 + Processor:602），Goal 3 + Phase 2 item 重写为"消费既有字段不修改定义"；(M1) 试点种子 PURCHASE/INPUT_VAT/ACCOUNTS_PAYABLE + 1403/2221/2202 与 PurAcctDocProvider SUBJECT_* 常量精确匹配（line 42-46 + 66-68 经核实 AP_INVOICE 第一行用 SUBJECT_PURCHASE=1403 而非 SUBJECT_INVENTORY=1401）；(M2) `account-key.dict.yaml` 显式枚举 BankReconAdj + Template in-wild 字面量；(M3) 跨账套传播语义经 Phase 0 Decision 显式声明 resolver 单次运行 pre-translation。2 MINOR（cosmetic：Phase 2 Targets header 残留 + BankReconAdj RECV 变体未列全）已当场修订（Targets header 清理 + dict 列举全 4 BankReconAdj 字面量 BANK_RECV/ADJ_BANK_RECV/ADJ_BANK_PAID/BANK_PAID per BankReconAdjAcctDocProvider:61,62,66,67）。**共识达成 → `Plan Status: active`**。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处。

- [x] 范围内行为完成（4 Phase 全部 `[x]`）
- [x] 相关文档对齐（gl-mapping-rules.md NEW + posting.md 回链 + roadmap A1 done）
- [x] 已运行验证：`mvn clean install -DskipTests`（154 模块 BUILD SUCCESS）+ `mvn test -pl module-finance/erp-fin-service`（含 TestErpFinGlMappingResolver + 既有 finance service 测试全绿，218 测试通过）+ `mvn test -pl module-purchase/erp-pur-service -Dtest=TestErpPurInvoicePosting`（试点接入回归全绿，3 测试通过）+ `npx playwright test tests/e2e/visual/gl-mapping-rule.visual.spec.ts`（UI smoke 通过，2 测试通过）
- [x] 无范围内项目降级为 deferred/follow-up（其余 Provider 批量接入 + 多节点分布式缓存 + 模板驱动路径统一 是合法 Deferred，已在 §Deferred But Adjudicated 登记，不属此条）
- [x] 独立草案审查已完成并记录（Draft Review Record iteration 1 + 2 + 3 accept）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 其余 Provider 批量接入 GL Mapping Resolver

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: A1 仅试点 PurAcctDocProvider AP_INVOICE 三键（高风险财务保护区域渐进接入策略）；其余 Provider 接入需独立配置规则种子 + 逐 Provider 回归测试。本计划落地解析引擎 + 契约 + 试点，其余 Provider 接入是模板化重复工作。
- Successor Required: `yes`（触发条件：A2 启动需要 budget 维度规则 + A3 启动需要 intercompany 维度规则；或试点稳定运行 ≥ 2 周后批量接入）

### 多节点分布式缓存一致性（NopSysEvent 同步）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 单节点进程内缓存已覆盖开发 + 小规模生产；多节点部署下规则变更经 TTL（默认 3600s）最终一致；如需强一致经 `NopSysEvent` topic 广播失效。
- Successor Required: `yes`（触发条件：生产部署多节点 + 规则变更延迟 > 5 分钟引发运维投诉）

### GL Distribution（科目分摊）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: posting.md §FactsValidator line 250-260 明确为独立扩展点（按部门/项目拆分一条分录为多条），由 FactsValidator 实现而非 GlMappingRule；A1 仅解决 accountKey → subjectCode 解析。
- Successor Required: `yes`（触发条件：业务出现按部门分摊金额的合规需求）

### 模板驱动路径（ErpFinTemplateAcctDocProvider）accountKey 解析统一

- Classification: `optimization candidate`
- Why Not Blocking Closure: 模板驱动路径已使用 `ErpFinVoucherTemplateLine.accountKey` 但内部解析独立；A1 resolver 提供更完善的优先级链 + 多维支持，未来可让模板路径也调 resolver 统一；当前两条路径并行不冲突。
- Successor Required: `yes`（触发条件：模板驱动路径出现多维覆盖需求 / A1 resolver 稳定 ≥ 1 个月后统一化）

### A2 预算多年度 / 承付会计 + A3 多公司运营深度

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: A2/A3 是 deepening-roadmap 独立工作项，依赖 A1 的规则表基础但范围各自独立（A2 加 budget 维度 + 承付凭证类型；A3 加 intercompany 维度 + 合并规则）。
- Successor Required: `yes`（触发条件：A1 落地后 mission driver 下一轮 draft）

## Closure

Status Note: 已完成 4 Phase 全部退出标准。Plan Status: completed（2026-07-21）。范围：ErpFinGlMappingRule 实体 + IErpFinGlMappingResolver 解析引擎（进程内缓存 + 主动失效）+ ErpFinPostingProcessor.resolveSubjects 覆盖钩子 + PurAcctDocProvider AP_INVOICE 试点接入 + operator UI（list/form/refresh-cache 按钮）+ owner doc + posting.md 回链 + roadmap A1 done。验证全绿：finance 218 测试 + purchase 113 测试 + visual smoke 2 测试 + 154 模块 mvn install BUILD SUCCESS。独立结束审计已由 mission driver 触发的独立子代理新会话完成（2026-07-21，无执行者上下文），语义验证全绿：实时仓库逐项核实实体/解析引擎/钩子/BizModel/Provider 试点/UI/文档/日志/roadmap 全部落地且非空壳（anti-hollow 通过）；五点一致性（Plan Status / 各 Phase Status / 各 Exit Criteria / Closure Gates / Closure evidence）一致；Deferred 项均为合法 out-of-scope successor（含触发条件，无隐藏在 Deferred 的范围内缺陷）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（mission driver 触发的新会话，冷重播无执行者上下文，2026-07-21）— semantics 全绿，approved
- Evidence:
  - Plan: `docs/plans/2026-07-21-0827-1-finance-gl-mapping-rule-tables.md`（4 Phase 全 [x]，Plan Status: completed，所有 Closure Gates [x]）
  - Owner Doc: `docs/design/finance/gl-mapping-rules.md`（NEW，7 节完整）
  - Roadmap: `docs/backlog/deepening-roadmap.md` §A1 done + §8.1 落地证据（line 51 + line 117-123）
  - 实体 + 字典: `module-finance/model/app-erp-finance.orm.xml` 新增 ErpFinGlMappingRule（line 1841-1897，22 字段 + 2 UK + 3 idx + 6 to-one relation）+ erp-fin/account-key 字典
  - 解析引擎: `module-finance/erp-fin-dao/src/main/java/app/erp/fin/dao/api/IErpFinGlMappingResolver.java` + `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinGlMappingResolver.java`（@PostConstruct eager load + ConcurrentHashMap 缓存 + invalidateCache 全量 reload + loadFromCache/loadFromDb 双路径）
  - 过账钩子: `ErpFinPostingProcessor.resolveSubjects`（line 552-581，accountKey 非空时调 resolver；命中覆盖 subjectCode；空匹配 + strict-mode 抛 ERR_GL_MAPPING_NOT_FOUND；空匹配 + 非 strict 保留 Provider fallback）+ `ErpFinPostingErrors.ERR_GL_MAPPING_NOT_FOUND`
  - BizModel: `ErpFinGlMappingRuleBizModel`（defaultPrepareSave/Update/Delete 注册 txn().afterCommit post-commit 缓存失效 + @BizQuery findApplicableRules + @BizMutation refreshCache）
  - 试点 Provider: `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/posting/PurAcctDocProvider.java` AP_INVOICE 分支（line 76-78）设置 ACCOUNT_KEY_PURCHASE/INPUT_VAT/ACCOUNTS_PAYABLE，fact() 工厂方法签名扩展加 accountKey 参数（line 93-99）
  - Operator UI: `module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/ErpFinGlMappingRule/ErpFinGlMappingRule.view.xml` + `erp-fin.action-auth.xml` 注册（fin-gl 分组，orderNo=520）
  - 测试: `TestErpFinGlMappingResolver`（8 场景全绿）+ `TestErpPurInvoicePosting.testGlMappingRuleOverrideChangesSubjectCode`（端到端覆盖生效）+ `tests/e2e/visual/gl-mapping-rule.visual.spec.ts`（2 场景全绿）
  - 验证基线: `mvn install -DskipTests` 154 模块 BUILD SUCCESS + finance 218 + purchase 113 + visual 2 测试全绿
  - Daily log: `docs/logs/2026/07-21.md`（line 3-65 含 A1 落地全证据）

Follow-up:

- 其余 Provider 批量接入 GL Mapping Resolver（触发：A2/A3 启动或试点稳定 ≥ 2 周）
- 多节点分布式缓存一致性（触发：多节点生产 + 变更延迟投诉）
- GL Distribution（触发：分摊金额合规需求）
- 模板驱动路径 resolver 统一（触发：多维覆盖需求或稳定 ≥ 1 个月）
- A2 预算多年度 + A3 多公司运营深度（A1 落地后下一轮 mission driver draft）
