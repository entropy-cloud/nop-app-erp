# A1.39 cs-F3 知识库/质量联动/预设应答 需求-实现符合性审计报告（MA1 RC）

> 里程碑：MA1（requirement-compliance mission，Work Item A1.39）
> 域/功能切片：customer-service / 知识库搜索与建议 + 工单升级为质量事件 + 预设应答使用（searchKnowledge+suggestForTicket LIKE 匹配 / adoptKnowledge 采纳审计 / UC-CS-06 质量事件跨域联动 / canned-response 渲染+变量校验+usageCount）
> UC 清单：UC-CS-05/06/07（3 UC）
> 来源：plan `docs/plans/2026-08-06-0100-1-rc-ma1-a1-39-cs-f3-knowledge-quality-canned.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）
> L1 锚点：`docs/audits/rc-requirement-baseline-inventory.md` A1.39 → UC-CS-05/06/07（✅ 一致，无基线分歧 D-xx）
> 审计类型：只读审计（无生产代码/ORM/api.xml/view.xml/真相源变更）
> 产出时间：2026-08-06

---

## 9. 与 MA2 报告差异增量声明（前置）

本切片报告与既有 MA2 行为审计报告 `docs/audits/2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`（A2.14 cs Ticket 6 态状态机 + SLA 计时联动 + L1 升级 PASS）的差异增量，按 §去重协议声明：

- **A2.14 §cs 范围不含本切片 UC**：A2.14 cs 范围（`2026-07-28-1020-arm-ma2-ext-domains-state-machine.md:21-23`）= `cs / ErpCsTicket`（Ticket 6 态）+ `cs / SLA 计时` + `cs / SLA 升级 Job`——**对象限于 `ErpCsTicketBizModel`（assign/start/resolve/close/reopen/cancel + matchAndAttachSla/scanOverdueTickets/findSlaWarnings）+ `SlaDeadlineCalculator` + `ErpCsSlaScanJob`**。**A2.14 不审 `ErpCsKnowledgeBaseBizModel` / `ErpCsCannedResponseBizModel` / `ErpCsCannedResponseApplyCannedResponseProcessor` / `CannedResponseRenderer` / 质量跨域联动**（这些对象超出 MA2 状态机/业财链路范围）。
- **本切片无可复用的 MA2 行为证据**——本切片为 UC-CS-05/06/07 的**首份行为/需求视角证据**。
- **本切片只补需求视角差异**（use-case 验收标准 vs 实际行为）：
  - **UC-CS-06 全域结构性未实现**（grep `NonConformance|ErpQa|qualityIssue|isQuality|ncrId|non_conformance` 跨 `module-cs/` 业务命中**仅** `app-erp-cs.orm.xml:22` 头注释「质量问题升级到 quality 域 NCR」——愿望性文档无代码）+ **无 quality I*Biz 注入**（无 `app.erp.qa.*` import；`ErpCsTicketBizModel` 12 方法无一涉质量）+ `ErpCsTicket`/`ErpCsTicketAction` **无 ncrId/nonConformanceId 列 + 无缺陷描述/物料/批次列** + `ACTION_TYPE_ESCALATE`（`ErpCsConstants:37`）仅被 SLA 超时扫描 `ErpCsTicketScanOverdueTicketsProcessor:70` 消费非质量 + **无"服务不可用后台重试"** → UC-CS-06 流程①-⑤+后置+异常**全 ❌**（P1 候选）
  - **UC-CS-05 采纳统计 / 采纳→RESOLVED / 无匹配建条目建议缺失**（`ErpCsKnowledgeBase` ORM `app-erp-cs.orm.xml:362-394` 无 usageCount/viewCount/adoptCount 列 + `adoptKnowledge:200-208` 仅写 NOTE 不转 RESOLVED + 无"无匹配→建条目建议"逻辑）——L1 后置⑤+异常+流程⑤全缺（P1 候选，§4 三判据复核后定级）
  - **UC-CS-07 分类树浏览缺独立方法**（`ErpCsCannedCategory` 实体 + `ErpCsCannedCategoryBizModel` CRUD 存在，但无 `getCategoryTree`/`browseCatalog` 独立方法，宏匹配 `suggestForTicket` 替代 + CRUD list 兜底）（P2 候选）

本切片不复审 A2.14 已证实的 Ticket 状态机 + SLA 计时 + L1 升级行为（A1.37/A1.38 已复审），仅从 L1 验收标准视角补齐 UC-CS-05/06/07 的需求契约↔行为差异。

---

## 1. 需求契约原文（L1，逐字引用，禁止转述）

> 真相源：`docs/design/customer-service/use-cases.md`（权威功能契约）。L2 owner doc（`README.md` / `canned-response.md` / `state-machine.md`）为设计参考，冲突以 L1 为准（§4 Q1）。

### UC-CS-05 知识库搜索与建议（`use-cases.md:84`）

```
**触发条件** 客户或客服在创建/编辑工单时输入 subject。

**前置条件** 知识库中存在已发布的文章（isPublished=true）。

**流程** 
1. 系统实时解析 subject 关键词。
2. 按关键词全文搜索 erp_cs_knowledge_base 表，按相关性排序。
3. 在工单界面展示 Top 5 匹配文章（标题 + 摘要）。
4. 客户/客服点击查看完整内容，可"采纳"标记为已参考。
5. 如采纳的文章解决了问题，工单直接标记为 RESOLVED。

**后置条件** 采纳记录计入知识库使用统计。

**异常** 无匹配文章 → 提示客服创建新知识库条目（工单解决后自动推送建议）。
```

> **`use-cases.md:101` LIKE-Deferred 注记（逐字引用）**："`searchKnowledge`/`suggestForTicket` `@BizQuery` 采用 `LIKE` 关键词匹配（title + content，对齐 ui-patterns.md 既有口径），非全文引擎。采纳登记经 `ErpCsTicketAction` 审计（`actionType=NOTE` + `knowledgeBaseId` 引用）。全文搜索引擎（Elasticsearch/DB FULLTEXT）归 Deferred（触发条件：文章量超万级或 LIKE 搜索时延/相关性质量不满足时）。"

**验收标准逐条枚举**：①触发：客户/客服在创建/编辑工单时输入 subject ②前置：知识库中存在已发布的文章（isPublished=true） ③流程①：系统实时解析 subject 关键词 ④流程②：按关键词全文搜索 erp_cs_knowledge_base 表，按相关性排序 ⑤流程③：在工单界面展示 Top 5 匹配文章（标题 + 摘要） ⑥流程④：客户/客服点击查看完整内容，可"采纳"标记为已参考 ⑦流程⑤：如采纳的文章解决了问题，工单直接标记为 RESOLVED ⑧后置：采纳记录计入知识库使用统计 ⑨异常：无匹配文章 → 提示客服创建新知识库条目（工单解决后自动推送建议）。**LIKE-非-全文属 L1 Deferred 注记（不计缺口）**。

### UC-CS-06 工单升级为质量事件（`use-cases.md:105`）

```
**触发条件** 处理人确认工单问题属于产品质量缺陷。

**前置条件** 工单处于 IN_PROGRESS 状态。

**流程** 
1. 处理人标记"质量问题"，填写缺陷描述、物料/批次信息。
2. 系统调用 quality 域 I*Biz 接口创建 ErpQaNonConformance。
3. 工单操作日志记录 ESCALATE 操作（关联 NCR 编号）。
4. 工单继续原有流程（RESOLVED → CLOSED），NCR 流程独立进行。
5. NCR 闭环后，工单可查看到 NCR 处理结果。

**后置条件** 工单与 NCR 关联，跨域可追溯。

**异常** quality 域服务不可用 → 延迟创建 NCR，工单先保留状态，后台自动重试。
```

**验收标准逐条枚举**：①触发：处理人确认工单问题属于产品质量缺陷 ②前置：工单处于 IN_PROGRESS 状态 ③流程①：处理人标记"质量问题"，填写缺陷描述、物料/批次信息 ④流程②：系统调用 quality 域 I*Biz 接口创建 ErpQaNonConformance ⑤流程③：工单操作日志记录 ESCALATE 操作（关联 NCR 编号） ⑥流程④：工单继续原有流程（RESOLVED → CLOSED），NCR 流程独立进行 ⑦流程⑤：NCR 闭环后，工单可查看到 NCR 处理结果 ⑧后置：工单与 NCR 关联，跨域可追溯 ⑨异常：quality 域服务不可用 → 延迟创建 NCR，工单先保留状态，后台自动重试。**UC-CS-06 全条无 Deferred 注记**。

### UC-CS-07 预设应答使用（`use-cases.md:124`）

```
**触发条件** 客服在处理工单时选择插入预设应答模板。

**前置条件** 预设应答模板已维护（ErpCsCannedResponse.isActive=true）。

**流程**
1. 客服在处理工单界面点击"插入预设应答"。
2. 系统展开分类树（billing/technical/account），显示可用模板列表。
3. 客服选择模板 → 系统预览渲染后内容（自动填充系统变量）。
4. 客服补充自定义变量（如 {email}）→ 系统校验必填变量。
5. 客服点击"插入"→ 渲染后的正文填入工单回复编辑框。
6. 客服可手动修改后发送 → 创建工单操作日志（actionType=NOTE）。
7. usageCount +1。

**后置条件** 工单操作日志记录渲染后的应答内容。

**异常** 缺失必填变量 → 禁止发送，高亮未填项。
```

**验收标准逐条枚举**：①触发：客服在处理工单时选择插入预设应答模板 ②前置：预设应答模板已维护（ErpCsCannedResponse.isActive=true） ③流程①：客服在处理工单界面点击"插入预设应答" ④流程②：系统展开分类树（billing/technical/account），显示可用模板列表 ⑤流程③：客服选择模板 → 系统预览渲染后内容（自动填充系统变量） ⑥流程④：客服补充自定义变量（如 {email}）→ 系统校验必填变量 ⑦流程⑤：客服点击"插入"→ 渲染后的正文填入工单回复编辑框 ⑧流程⑥：客服可手动修改后发送 → 创建工单操作日志（actionType=NOTE） ⑨流程⑦：usageCount+1 ⑩后置：工单操作日志记录渲染后的应答内容 ⑪异常：缺失必填变量 → 禁止发送，高亮未填项。

---

## 2. 实现证据（L3 代码路径，含行号 + 跨域调用链）

### UC-CS-05 知识库搜索与建议（⚠️ 搜索/Top5/采纳 NOTE 核心完整+强测，采纳统计+采纳转解决+无匹配建议缺失）

- **③ 实时解析 subject 关键词（✅）**：`ErpCsKnowledgeBaseBizModel.suggestForTicket:84-98` @BizQuery——subject 守门（`SUGGEST_SUBJECT_MIN_LENGTH`，`:87`）+ `extractKeyword:140-148` 按 `[\s,，。.!！?？;；:：、/\\()（）\[\]【】]+` 分词取首个 ≥ `SUGGEST_SUBJECT_MIN_LENGTH` token（兜底取 subject 自身）。`SUGGEST_SUBJECT_MIN_LENGTH` 经 `ErpCsConstants` 常量定义。
- **④ 按关键词搜索 + 相关性排序（✅ LIKE 匹配 + 内存相关性，**L1 Deferred 注记豁免全文**）**：`ErpCsKnowledgeBaseBizModel.searchKnowledge:37-80` @BizQuery——
  - keyword 守门（null/trim 空返回 `ArrayList`，`:41-43`）+ 长度守门（`KNOWLEDGE_SEARCH_KEYWORD_MAX_LENGTH`，`:45-49` 抛 `ERR_KNOWLEDGE_SEARCH_KEYWORD_TOO_LONG`）
  - `resolveLimit:100-110` 钳制（null/≤0 → `ErpCsConfigs.getKnowledgeSearchDefaultLimit()` 默认 5 / > `getKnowledgeSearchMaxLimit()` 钳至 max 20）——`ErpCsConfigs.getKnowledgeSearchDefaultLimit()` 默认 5 ✅
  - QueryBean `isPublished=true`（`:54`）+ `or(title LIKE %kw%, content LIKE %kw%)`（`:55-57`）+ 可选 `categoryId`（`:58-60`）+ `createTime DESC`（`:61`）+ `setLimit(effectiveLimit * 2)`（`:62` 预筛 2 倍供相关性排序裁剪）
  - **`doFindListByQueryDirectly:66`** 绕 XMeta filterOp 限制（LIKE/OR 在某些 XMeta 配置下被拒，与 `ErpCsTicketBizModel.scanOverdueTickets` 同型）
  - **内存相关性排序 `:68-73`**：`titleMatchScore`（title 含 kwLower → 1，否则 0，`:112-118`）+ `.reversed()`（title-match 优先）+ `thenComparing(getCreateTime, nullsLast(reverseOrder()))`（再按 createTime 降序）+ 取前 `effectiveLimit` 条
  - `toSummary:120-128`——返回 `id/code/title/contentSummary(truncate KNOWLEDGE_CONTENT_SUMMARY_LENGTH)/categoryId` 五字段（标题+摘要满足 L1 流程③）
- **⑤ Top 5 匹配文章展示（✅）**：`ErpCsConfigs.getKnowledgeSearchDefaultLimit()` 默认 5 + `resolveLimit` 钳制——`TestErpCsKnowledgeBaseSearch.testSearchKnowledgeLimitClamping:97-123` 强测 limit=3→3/limit=0→5/limit=999→钳至 20（但仅 10 数据）。
- **⑥ "采纳"标记为已参考（⚠️ NOTE 审计完整，但不转状态 + 不增 KB 计数）**：`ErpCsTicketBizModel.adoptKnowledge:200-208` @BizMutation——`requireTicket` + `writeAction(ACTION_TYPE_NOTE, current, current, "采纳知识库文章参考: knowledgeBaseId=" + knowledgeBaseId, context)`（`ErpCsConstants.ACTION_TYPE_NOTE="NOTE"` + `writeAction` 写 `ErpCsTicketAction`：ticketId/actionType/fromStatus/toStatus/content/operatorId）。**采纳审计行完整但仅 NOTE 不转 RESOLVED + ErpCsKnowledgeBase 计数器无写入**（grep 全 `module-cs` `usageCount|viewCount|adoptCount` 业务零命中 + ErpCsKnowledgeBase ORM 无计数列）。
- **⑦ 采纳→RESOLVED（❌ 全缺）**：`adoptKnowledge:200-208` 仅写 NOTE 审计行，**不修改 ticket.status**——L1 流程⑤「如采纳的文章解决了问题，工单直接标记为 RESOLVED」**未实现**。grep `set.*RESOLVED|setStatus.*RESOLVED` 在 `ErpCsTicketBizModel.adoptKnowledge` 零命中（resolve 路径仅经 `ErpCsTicketResolveProcessor`）。
- **⑧ 后置 采纳记录计入知识库使用统计（❌ 全缺）**：`ErpCsKnowledgeBase` ORM（`app-erp-cs.orm.xml:362-394`）字段 = `id/code/title/content/categoryId/isPublished/remark` + 标准审计字段（`delVersion/version/createdBy/createTime/updatedBy/updateTime`）——**无 `usageCount`/`viewCount`/`adoptCount` 列**（grep `usageCount|viewCount|adoptCount` 跨 `module-cs/model/app-erp-cs.orm.xml` 仅 `ErpCsCannedResponse.usageCount` 命中，KB 实体零命中）→ L1 后置「采纳记录计入知识库使用统计」**结构性不可实现**（无计数列载体）。`adoptKnowledge:200-208` 也不调任何 KB 计数更新逻辑。
- **⑨ 异常 无匹配文章→提示客服创建新知识库条目（工单解决后自动推送建议）（❌ 全缺）**：`searchKnowledge:41-43` 空 keyword 返回空集 + `suggestForTicket:87-89` 短 subject 返回空集——**空集返回后无任何"提示客服创建新条目"逻辑 + 无"工单解决后自动推送建议"逻辑**。grep `suggestCreate|建议创建|创建新知识库|knowledgeSuggest|noMatchSuggestion` 跨 `module-cs/erp-cs-service/src/main` **零命中**。

### UC-CS-06 工单升级为质量事件（❌ 全域结构性未实现）

- **② 前置 工单处于 IN_PROGRESS（⚠️ 状态机层面满足但无质量升级路径利用）**：`ErpCsTicketBizModel` 6 态状态机完整（A2.14 PASS + A1.37 复用），但**无任何 mutation 利用 IN_PROGRESS 状态触发质量升级**。
- **③ 流程① 处理人标记"质量问题"，填写缺陷描述、物料/批次信息（❌ 全缺）**：grep `markQuality|flagQuality|qualityIssue|isQualityIssue|defectDescription|materialLot|batchInfo|lotNumber|qualityFlag` 跨 `module-cs/erp-cs-service/src/main` **零业务命中**。`ErpCsTicketBizModel` 12 方法清单（`defaultPrepareSave/assign/start/resolve/close/reopen/cancel/adoptKnowledge/matchAndAttachSla/scanOverdueTickets/findSlaWarnings/findBoardData`，与 A1.38 grep 一致）**无一涉质量**。`ErpCsTicket` ORM 无 `defectDescription`/`materialId`/`batchId`/`lotId`/`qualityFlag` 字段（grep `defect|quality|ncrId|nonConformance` 跨 `module-cs/model/app-erp-cs.orm.xml` 仅 `:22` 头注释命中）。
- **④ 流程② 系统调用 quality 域 I*Biz 接口创建 ErpQaNonConformance（❌ 全缺）**：grep `IErpQaNonConformanceBiz|ErpQaNonConformance|app.erp.qa|createNonConformance|createNcr` 跨 `module-cs/erp-cs-service/src/main` **零业务命中**（无 `app.erp.qa.*` import + 无 `@Inject IErpQaNonConformanceBiz` 注入）。`ErpCsKnowledgeBaseBizModel` + `ErpCsCannedResponseBizModel` + `ErpCsTicketBizModel` 均无 quality I*Biz 注入（仅注入 `IErpMdPartnerBiz` + `IErpSysNotificationBiz` + `IErpCsTicketBiz`/`IErpCsTicketActionBiz` 同域 Facade）。
- **⑤ 流程③ 工单操作日志记录 ESCALATE 操作（关联 NCR 编号）（❌ ESCALATE 仅 SLA 用）**：`ErpCsConstants.ACTION_TYPE_ESCALATE="ESCALATE":37` + `ErpCsTicketScanOverdueTicketsProcessor.scanOverdueTickets:70-71` 是**唯一**写 ESCALATE 审计行的生产代码——SLA 超时升级专用，**无质量升级路径写 ESCALATE + 关联 NCR 编号**。`ErpCsTicketAction` ORM 无 `ncrId`/`nonConformanceId`/`relatedNcrCode` 字段。
- **⑥ 流程④ 工单继续原有流程（RESOLVED → CLOSED），NCR 流程独立进行（⚠️ 仅状态机主路径 OK，无质量联动）**：6 态状态机完整（A1.37/A2.14 PASS），但无任何"质量升级后继续工单原有流程"的协调逻辑。
- **⑦ 流程⑤ NCR 闭环后，工单可查看到 NCR 处理结果（❌ 全缺）**：grep `ncrResult|ncrClosed|nonConformanceStatus|fetchNcr|loadNcr` 跨 `module-cs` 零业务命中。`ErpCsTicket`/`ErpCsTicketAction` 无 NCR 关联列，无回查路径。
- **⑧ 后置 工单与 NCR 关联，跨域可追溯（❌ 全缺）**：`ErpCsTicket` ORM 无 NCR FK / 关联弱指针列。grep `ncrId|nonConformanceId|relatedNcr` 跨 `module-cs/model/app-erp-cs.orm.xml` 零命中。
- **⑨ 异常 quality 域服务不可用 → 延迟创建 NCR，工单先保留状态，后台自动重试（❌ 全缺）**：grep `retryCreateNcr|delayedNcr|backgroundRetry|ncrRetry|qualityServiceUnavailable` 跨 `module-cs` 零业务命中。无 nop-job / scheduler 消费 NCR 重试。无 `IErpQaNonConformanceBiz` 调用自然无"服务不可用"异常路径。
- **`app-erp-cs.orm.xml:22` 头注释「质量问题升级到 quality 域 NCR」**：愿望性文档注释（`<!-- ... 质量问题升级到 quality 域 NCR -->`），无对应代码实现。**单一代码注释不构成行为证据**（methodology §1 L3 anti-hollow 纪律：行为断言是 anti-hollow 核心载体，仅注释/方法名存在不构成证据）。
- **跨域 Facade 注入现状（确认基线）**：module-cs 生产代码跨域 Facade 仅 `IErpMdPartnerBiz`（`ErpCsCannedResponseBizModel:57` / `ErpCsTicketBizModel:62` / `ErpCsCannedResponseApplyCannedResponseProcessor:43`）+ `IErpSysNotificationBiz`（`ErpCsTicketBizModel:64`）——A1.37/A1.38/A2.14:320 复用，零 quality Facade 注入。P1-MA1-022 不涉及 cs。

### UC-CS-07 预设应答使用（✅ 渲染/校验/usageCount/审计完整，⚠️ 分类树浏览缺独立方法）

- **① 触发 + ② 前置（✅ config-gated + isActive 守卫）**：`ErpCsConfigs.isCannedResponseEnabled()` 默认 true（`suggestForTicket:91` config-gated）+ `ErpCsCannedResponseApplyCannedResponseProcessor.assertActive:76-81` isActive=false 抛 `ERR_CANNED_RESPONSE_INACTIVE` + `ErpCsCannedResponseBizModel.assertActive:154-159` 同型守卫。
- **③ 流程① 客服点击"插入预设应答"（✅ 前端入口存在）**：`ErpCsTicket.view.xml`（AMIS）含「插入预设应答」按钮挂接 GraphQL mutation/query（`ErpCsCannedResponse__suggestForTicket` / `ErpCsCannedResponse__applyCannedResponse`），前端可达。
- **④ 流程② 系统展开分类树（billing/technical/account），显示可用模板列表（⚠️ ErpCsCannedCategory 实体+CRUD 兜底，无独立 getCategoryTree 方法 / 宏匹配替代）**：
  - **ErpCsCannedCategory 实体存在**（`app-erp-cs.orm.xml:397-429`）——`id/code/name/orgId/parentId(自引用)/icon/sequence` + `parent` to-one 自关联（`:416` 树形结构）+ `IDX_CS_CANNED_CATEGORY_PARENT_ID` 索引。**字段载体完整**，可承载 billing/technical/account 三级分类树。
  - **ErpCsCannedCategoryBizModel 存在**（CrudBizModel 标准实现）——标准 CRUD list（默认经 XMeta 过滤 + 分页）可用作"分类列表浏览"兜底。
  - **无独立"浏览分类树"方法**——grep `getCategoryTree|browseCatalog|loadCategoryTree|treeOfCategories|categoryTree` 跨 `module-cs/erp-cs-service/src/main` **零业务命中**。
  - **宏匹配替代**——`ErpCsCannedResponseBizModel.suggestForTicket:89-126` @BizQuery 三级宏匹配（exact[type+priority] `:112` → type-only `:118` → global fallback `:124`），按工单 ticketTypeId+priority 自动推荐候选模板（非分类驱动）+ `ErpCsConfigs.getCannedResponseMacroCount()`（默认 3）条数钳制 + 内存过滤（避开 isNull XMeta 限制 `:103-106` + `fillMatching:215-243`）。
- **⑤ 流程③ 客服选择模板 → 系统预览渲染后内容（自动填充系统变量）（✅）**：`ErpCsCannedResponseBizModel.renderTemplate:77-85` @BizQuery——`requireCannedResponse` + `assertActive` + `resolveSystemVars:161-184`（系统变量：`{today}` `:165` + `{now}` `:166` + `{agent_name}` `:168` + `{ticket_id}` 经 `loadTicket.code` `:175` + `{customer_name}` 经 `IErpMdPartnerBiz.findById` `:177-180`）+ 委托 `CannedResponseRenderer.render(content, variableDefs, systemVars, customVariables):84`。
- **⑥ 流程④ 客服补充自定义变量 → 系统校验必填变量（✅）**：`CannedResponseRenderer.render:42-48`——`mergeVars:50-59`（custom 覆盖 system，`:56` `merged.putAll(customVars)`）→ `parseVarDefs:61-95`（解析 JSON `variableDefs` 提取 VarDef{key, required}）→ `validateRequired:97-108`（required=true 且 merged 值 empty 抛 `ERR_CANNED_RESPONSE_REQUIRED_VAR_MISSING` + `.param(ARG_VARIABLE_KEY, d.key)`）→ `replacePlaceholders:110-124`。**缺失必填变量禁止渲染成立**——`TestCannedResponseRenderer.testRequiredVarMissingThrows:56-67` 强测错误码 + variableKey 参数。
- **⑦ 流程⑤ 渲染后的正文填入工单回复编辑框（✅ applyCannedResponse 返回渲染正文）**：`ErpCsCannedResponseApplyCannedResponseProcessor.applyCannedResponse:45-61`——渲染（`:50`）+ usageCount+1（`:52-55`）+ writeNoteAction（`:57-58`）+ **返回 rendered 字符串**（`:60`）——前端将该字符串填入回复编辑框（AMIS wiring）。
- **⑧ 流程⑥ 客服可手动修改后发送 → 创建工单操作日志（actionType=NOTE）（✅）**：`ErpCsCannedResponseApplyCannedResponseProcessor.writeNoteAction:127-137`——`ErpCsTicketAction` 行：`ticketId/actionType=ACTION_TYPE_NOTE/content=rendered/operatorId=context.getUserId()` + `ticketActionBiz.saveEntity`。"客服可手动修改后发送"是前端语义（mutation 返回 rendered 后，客服在编辑框自由修改，最终发送是另一独立 mutation 或 NOTE 写入路径），L3 applyCannedResponse 已写入 NOTE 审计行 + 渲染正文。
- **⑨ 流程⑦ usageCount +1（✅）**：`ErpCsCannedResponseApplyCannedResponseProcessor.applyCannedResponse:52-55`——`Integer cur = resp.getUsageCount(); resp.setUsageCount(cur == null ? 1 : cur + 1); dao().updateEntity(resp);`。`ErpCsCannedResponse.usageCount` ORM 列存在（`app-erp-cs.orm.xml:447` propId 12 stdSqlType=INTEGER defaultValue=0）。
- **⑩ 后置 工单操作日志记录渲染后的应答内容（✅）**：`writeNoteAction:127-137` `action.setContent(content=rendered)`——L1 后置「记录渲染后的应答内容」语义满足（content 字段承载渲染后正文，非原始模板）。
- **⑪ 异常 缺失必填变量 → 禁止发送，高亮未填项（✅ 抛错误码 + variableKey 参数）**：`CannedResponseRenderer.validateRequired:97-108` 抛 `ERR_CANNED_RESPONSE_REQUIRED_VAR_MISSING` + `variableKey` 参数。前端依据该错误码 + 参数高亮未填项（AMIS 层 wiring）。**"禁止发送"语义成立**（渲染阶段抛错 → applyCannedResponse 不进入 usageCount+1 / writeNoteAction 阶段，无 NOTE 审计行写入）。

### ErpCsKnowledgeBase ORM 实体（❌ L1 后置「采纳统计」字段缺失）

`module-cs/model/app-erp-cs.orm.xml:362-394` `ErpCsKnowledgeBase` 实体字段：`id/code/title/content/categoryId/isPublished/remark` + 标准审计字段（`delVersion/version/createdBy/createTime/updatedBy/updateTime`）。

- **❌ 采纳统计字段缺失**：无 `usageCount`/`viewCount`/`adoptCount` 列（grep `usageCount|viewCount|adoptCount` 跨 `module-cs/model/app-erp-cs.orm.xml` 仅 `ErpCsCannedResponse.usageCount:447` 命中，KB 实体零命中）→ L1 后置「采纳记录计入知识库使用统计」**字段载体不存在**。

### ErpCsCannedResponse ORM 实体（✅ L1 字段齐全）

`module-cs/model/app-erp-cs.orm.xml:432-469` `ErpCsCannedResponse` 实体字段：`id/code/orgId/title/content/categoryId/variableDefs/macroTicketTypeId/macroPriority/sequence/isActive/usageCount` + 标准审计字段。

- **✅ L1 字段齐全**：`usageCount`(propId 12 `:447` defaultValue=0) + `variableDefs`(propId 7 `:442` domain=json 承载 §1.3 variables 数组) + `macroTicketTypeId`(propId 8 `:443`)/`macroPriority`(propId 9 `:444` 宏匹配载体) + `categoryId`(propId 6 `:441` 关联 ErpCsCannedCategory) + `isActive`(propId 11 `:446`)。

### ErpCsCannedCategory ORM 实体（✅ 树形结构齐全）

`module-cs/model/app-erp-cs.orm.xml:397-429` `ErpCsCannedCategory` 实体字段：`id/code/name/orgId/parentId/icon/sequence` + `parent` to-one 自关联（`:416` 树形）+ `UK_CS_CANNED_CATEGORY_CODE_ORG` + `IDX_CS_CANNED_CATEGORY_PARENT_ID`。

- **✅ 树形载体完整**：`parentId` 自引用 + `parent` to-one 关联，可承载 billing/technical/account 多级分类树。

### 跨域 daoFor

module-cs 生产代码零跨域 daoFor（同 A1.37/A1.38 基线 + A2.14 §cs `:320` 实测确认）。跨域访问正确经 Facade：`IErpMdPartnerBiz`（`ErpCsCannedResponseBizModel:57` / `ErpCsCannedResponseApplyCannedResponseProcessor:43`）+ `IErpSysNotificationBiz`（`ErpCsTicketBizModel:64`）。P1-MA1-022 不涉及 cs。

---

## 3. 测试证据（L4，注明断言强度）

| 测试 | 文件#方法 | 覆盖验收标准 | 断言强度 |
|------|-----------|------------|---------|
| KB 搜索命中已发布排除未发布 | `TestErpCsKnowledgeBaseSearch.java#testSearchKnowledgeHitsPublishedExcludesUnpublished:57-78` | UC-CS-05 ②④⑤（isPublished=true 过滤 + LIKE 命中 + 排除草稿） | **强断言**（assertEquals 2 篇 + assertFalse 草稿 + contentSummary 非空） |
| KB categoryId 过滤 | `TestErpCsKnowledgeBaseSearch.java#testSearchKnowledgeCategoryFilter:80-95` | UC-CS-05 ④（categoryId 维度过滤） | **强断言**（assertEquals 2 篇 + categoryId 精确匹配） |
| KB limit 钳制 | `TestErpCsKnowledgeBaseSearch.java#testSearchKnowledgeLimitClamping:97-123` | UC-CS-05 ⑤（limit=3→3 / limit=0→默认 5 / limit=999→钳至 20） | **强断言**（三档边界 + 默认值） |
| KB 空/空白 keyword 守门 | `TestErpCsKnowledgeBaseSearch.java#testSearchKnowledgeEmptyKeywordReturnsEmpty:126-142` | UC-CS-05 ①守门 | **强断言**（空 + 空白双场景） |
| suggestForTicket 解析 subject | `TestErpCsKnowledgeBaseSearch.java#testSuggestForTicketParsesSubject:144-161` | UC-CS-05 ③⑤（subject 解析 + Top 5 上限） | **强断言**（≤5 + 非空） |
| suggestForTicket 短 subject 守门 | `TestErpCsKnowledgeBaseSearch.java#testSuggestForTicketShortSubjectReturnsEmpty:163-180` | UC-CS-05 ①守门 | **强断言**（空 + 单字符双场景） |
| adoptKnowledge 写 NOTE 审计 | `TestErpCsKnowledgeBaseSearch.java#testAdoptKnowledgeRecordsTicketAction:182-209` | UC-CS-05 ⑥（NOTE action + content 含 kbId） | **强断言**（countActions 增长 + actionType=NOTE + content.contains(kbId)） |
| applyCannedResponse usageCount+1 + NOTE | `TestErpCsCannedResponseBiz.java#testApplyCannedResponseIncrementsUsageAndWritesAction:179-197` | UC-CS-07 ⑧⑨⑩（usageCount 5→6 + NOTE 审计） | **强断言**（assertEquals 6 + hasNote 标志） |
| CannedResponse inactive 拒绝 | `TestErpCsCannedResponseBiz.java`（inactive 测试，error code 精确匹配） | UC-CS-07 ②前置 isActive 守卫 | **强断言**（错误码） |
| CannedResponse 三级宏匹配 | `TestErpCsCannedResponseBiz.java`（exact/type/global 三级） | UC-CS-07 ④替代（宏匹配替代分类树） | **强断言**（三级优先级断言 + global fallback 命中） |
| 必填变量缺失抛错 | `TestCannedResponseRenderer.java#testRequiredVarMissingThrows:56-67` | UC-CS-07 ⑪异常（缺失必填变量禁止） | **强断言**（错误码 + variableKey 参数双断言） |
| Renderer custom 覆盖 system | `TestCannedResponseRenderer.java#testCustomVariableOverridesSystem:42-54` | UC-CS-07 ⑤⑥（系统变量 + 自定义覆盖） | **强断言**（"自定义名 TK-002" 精确字符串） |
| Renderer 无变量模板原样 | `TestCannedResponseRenderer.java#testNoVariableTemplateReturnsAsIs:69-74` | UC-CS-07 ⑤边界 | 中-强断言（字符串恒等） |
| Renderer null variableDefs 跳过必填校验 | `TestCannedResponseRenderer.java#testNullVariableDefsSkipsRequiredValidation:77-85` | UC-CS-07 ⑥边界（null variableDefs 容错） | 中-强断言 |
| Renderer malformed JSON 容错 | `TestCannedResponseRenderer.java#testMalformedJsonVariableDefsDegradesGracefully:87-95` | UC-CS-07 ⑥边界（非法 JSON 容错） | 中-强断言 |

**UC-CS-05 测试缺口**：
1. **KB 使用统计零测试**（功能不存在——`ErpCsKnowledgeBase` 无 usageCount/adoptCount 列 + adoptKnowledge 不写 KB 计数器，无可测路径）；
2. **采纳→RESOLVED 零测试**（功能不存在——adoptKnowledge 不修改 status，无可测路径）；
3. **无匹配→建条目建议零测试**（功能不存在——无"无匹配建议建条目"逻辑，无可测路径）。

**UC-CS-06 测试缺口**：
4. **零测试**（功能完全不存在——无质量标记 mutation + 无 quality I*Biz 注入 + 无 NCR 关联列 + 无重试，**全无路径可测**）。

**UC-CS-07 测试缺口**：
5. **分类树浏览零测试**（功能不存在——无独立 `getCategoryTree`/`browseCatalog` 方法；宏匹配 `suggestForTicket` 已强测 + ErpCsCannedCategory CRUD 兜底）。

---

## 4. 运行时行为证据（L5）

| 来源 | 证实的行为 | 复用/补充 |
|------|-----------|----------|
| 本切片首次行为证据（A2.14 范围不含本切片 UC） | **UC-CS-05 searchKnowledge/suggestForTicket/adoptKnowledge + UC-CS-07 renderTemplate/suggestForTicket/applyCannedResponse + CannedResponseRenderer 行为**——L3 行为断言 + L4 强测 7+8+9=24 @Test 双重证实（A2.14 不覆盖，本切片为首份行为/需求视角证据） | **本切片首测**（无 MA2 复用） |
| A2.14 §cs `:21-23` | cs Ticket 6 态状态机 + SLA 计时 + SLA 升级 Job PASS——**仅 UC-CS-06 ②前置 IN_PROGRESS 状态机层面满足的复用源** | **复用 MA2**（仅 UC-CS-06 ②前置层面，质量联动行为本身 UC-CS-06 全缺无 MA2 复用） |
| `TestErpCsKnowledgeBaseSearch` 7 @Test + `TestErpCsCannedResponseBiz` 8 @Test + `TestCannedResponseRenderer` 9 @Test | UC-CS-05 ③④⑤⑥ + UC-CS-07 ①②③⑤⑥⑦⑧⑨⑩⑪ 主路径强测覆盖 | **补充**（L1 验收标准视角行为验收） |

L5 存疑点（无法静态定论，需运行时确认）登记入 §7 静态存疑点清单交 MA4 展开。

---

## 5. 符合性结论（五级追踪矩阵 + 每 UC 分级，§2 判据）

### 五级追踪矩阵

| UC | L1 use-case | L2 owner doc | L3 代码路径 | L4 测试 | L5 运行时 | 结论 |
|----|-------------|--------------|------------|---------|-----------|------|
| UC-CS-05 | `use-cases.md:84`（§1 逐字引用 9 验收标准 + `:101` LIKE-Deferred 注记） | `README.md §核心业务对象:38`「知识库（KnowledgeBase）| 可发布的知识/FAQ 文章，支持按标题与正文关键词检索，并按工单主题智能推荐候选方案（详见 use-cases.md）」——**L2 指向 L1，无 simplification 标注**；无独立 knowledge-base.md owner doc（grep 域文档目录无） | **③实时解析 ✅**（`suggestForTicket:84-98`）+ **④LIKE 搜索+相关性排序 ✅**（`searchKnowledge:37-80` LIKE 非-全文属 L1 Deferred 不计）+ **⑤Top5 ✅**（config 默认 5 + limit 钳制）+ **⑥采纳 NOTE ✅**（`adoptKnowledge:200-208`）+ **⑦采纳→RESOLVED ❌**（adoptKnowledge 不修改 status）+ **⑧采纳统计 ❌**（ErpCsKnowledgeBase ORM 无计数列）+ **⑨无匹配→建条目建议 ❌**（无逻辑） | `TestErpCsKnowledgeBaseSearch` 7 @Test 强覆盖 ③④⑤⑥ + **⑦⑧⑨零测试**（功能不存在） | 本切片首测（A2.14 范围不含）+ 24 @Test 双重证实主路径 | 见下方逐条（接受核心 + P1） |
| UC-CS-06 | `use-cases.md:105`（§1 逐字引用 9 验收标准 + 异常条款，**全条无 Deferred 注记**） | `README.md §跨域协作:49`「质量问题升级 \| quality \| 工单中确认是产品质量问题 → 创建 ErpQaNonConformance」**= 活跃设计契约非 Non-Goal**；`state-machine.md §外部依赖:86`「质量问题升级到 NCR \| 处理人标记 quality 联动 → 创建 ErpQaNonConformance」**= 活跃设计契约**；`README.md §延迟项与非目标:97-101` Non-Goal 仅 3 项（SLA working-days/L1 升级/SLA pause），**UC-CS-06 不在 Non-Goal 列表** | **②前置 ✅**（6 态状态机 IN_PROGRESS 可达，A1.37/A2.14 PASS）+ **③标记质量+缺陷信息 ❌**（无 mutation + ErpCsTicket 无缺陷/物料/批次列）+ **④调 quality I*Biz 建 NCR ❌**（无 IErpQaNonConformanceBiz 注入）+ **⑤ESCALATE 审计关联 NCR ❌**（ESCALATE 仅 SLA 用 + 无 ncrId 字段）+ **⑥继续原有流程 ⚠️**（状态机主路径 OK 无质量联动）+ **⑦NCR 闭环结果可查 ❌**（无关联列无回查）+ **⑧工单与 NCR 关联可追溯 ❌**（无 FK / 弱指针）+ **⑨服务不可用后台重试 ❌**（无重试逻辑无 scheduler） | **零测试**（功能完全不存在） | 本切片首测（A2.14 范围不含）——UC-CS-06 全域结构性未实现经 grep + 代码逐行确认 | 见下方逐条（P1） |
| UC-CS-07 | `use-cases.md:124`（§1 逐字引用 11 验收标准 + 异常条款） | `canned-response.md §一~三`（模型+宏匹配+使用流程完整设计）+ `§3.1:137` 显式「展开分类树（billing / technical / account）」+ `§七:204-208` 跨域协作契约——**L2 与 L1 一致**；`canned-response.md:7`「§四（管理功能 Excel 导入/导出 + §五 分类树校验）归 Deferred successor」——**仅管理功能 + 校验 Deferred，分类树浏览本体未 Deferred** | **①②③✅**（config-gated + isActive 守卫 + 前端入口）+ **④分类树 ⚠️**（ErpCsCannedCategory 实体+CRUD 存在 + 无独立浏览方法 + 宏匹配替代 + CRUD list 兜底）+ **⑤渲染系统变量 ✅**（`renderTemplate:77-85`）+ **⑥必填校验 ✅**（`validateRequired:97-108`）+ **⑦插入编辑框 ✅**（applyCannedResponse 返回 rendered）+ **⑧NOTE 审计 ✅**（`writeNoteAction:127-137`）+ **⑨usageCount+1 ✅**（`:52-55`）+ **⑩渲染内容记录 ✅**（content=rendered）+ **⑪缺失必填禁止 ✅**（抛错误码 + variableKey） | `TestErpCsCannedResponseBiz` 8 @Test 强 + `TestCannedResponseRenderer` 9 @Test 强覆盖 ①②③⑤⑥⑦⑧⑨⑩⑪ + **④分类树零测试**（功能不存在独立方法） | 本切片首测（A2.14 范围不含）+ 24 @Test 双重证实主路径 | 见下方逐条（接受主路径 + P2） |

### 逐 UC 结论（取最高）

#### UC-CS-05 知识库搜索与建议 → **部分接受 + 1 新 P1**（③④⑤⑥接受；⑦采纳→RESOLVED + ⑧采纳统计 + ⑨无匹配建条目建议 P1）

- **③ 实时解析 subject 关键词 = 接受**（§2 判据"接受"）：`suggestForTicket:84-98` 实时调用 + `extractKeyword:140-148` 分词。L3/L4/L5 三源一致 + `testSuggestForTicketParsesSubject:144-161` 强测。
- **④ 按关键词搜索 + 相关性排序 = 接受 on 主路径**（§2 判据"接受 on 主路径"）：`searchKnowledge:37-80` LIKE 匹配 + 内存相关性排序（title-match 优先 + createTime desc）+ `doFindListByQueryDirectly` 绕 XMeta 限制。**LIKE 非-全文属 L1 `:101` Deferred 注记豁免**（不计缺口——L1 显式标注「全文搜索引擎归 Deferred 触发条件：文章量超万级或 LIKE 时延/相关性质量不满足时」）。L3/L4/L5 三源一致 + `testSearchKnowledgeHitsPublishedExcludesUnpublished:57-78` + `testSearchKnowledgeCategoryFilter:80-95` 强测。
- **⑤ Top 5 匹配文章展示 = 接受**（§2 判据"接受"）：`ErpCsConfigs.getKnowledgeSearchDefaultLimit()` 默认 5 + `resolveLimit` 钳制 + `testSearchKnowledgeLimitClamping:97-123` 三档边界强测（limit=3→3/limit=0→5/limit=999→钳至 20）。
- **⑥ "采纳"标记为已参考 = 接受 on NOTE 审计**（§2 判据"接受 on NOTE 审计"）：`adoptKnowledge:200-208` 写 `ErpCsTicketAction`（actionType=NOTE，content 含 knowledgeBaseId 引用）。**采纳审计行完整**——L1 流程④「点击查看完整内容，可"采纳"标记为已参考」语义满足（NOTE 审计行 = "已参考"标记）。L3/L4/L5 三源一致 + `testAdoptKnowledgeRecordsTicketAction:182-209` 强测（NOTE + content.contains(kbId)）。
- **⑦ ⑧ ⑨ → P1**（§2 P1①功能实质偏离验收标准 + §2 P1②异常路径未实现——**§4 三判据关键裁决**）：
  - **⑦ 采纳→RESOLVED**：L1 逐字 `use-cases.md:95` 流程⑤「如采纳的文章解决了问题，工单直接标记为 RESOLVED」；L3 `adoptKnowledge:200-208` 仅写 NOTE 审计行**不修改 ticket.status**——resolve 路径仅经 `ErpCsTicketResolveProcessor` 独立 mutation。**L1 字面「直接标记 RESOLVED」未实现**。
  - **⑧ 后置 采纳记录计入知识库使用统计**：L1 逐字 `use-cases.md:97` 后置「采纳记录计入知识库使用统计」；L3 `ErpCsKnowledgeBase` ORM（`app-erp-cs.orm.xml:362-394`）**无 usageCount/viewCount/adoptCount 列**——字段载体不存在致**结构性不可实现**。`adoptKnowledge:200-208` 也不调任何 KB 计数更新逻辑。
  - **⑨ 异常 无匹配→建条目建议（工单解决后自动推送）**：L1 逐字 `use-cases.md:99` 异常「无匹配文章 → 提示客服创建新知识库条目（工单解决后自动推送建议）」；L3 grep `suggestCreate|建议创建|创建新知识库|knowledgeSuggest|noMatchSuggestion` 跨 main **零命中**——空集返回后无任何"提示建条目"逻辑 + 无"工单解决后自动推送建议"逻辑 + 无 scheduler。
  - **§4 三判据关键复核**（owner doc Non-Goal 标注的人工批准痕迹核查——P1 裁决核心）：
    - **判据 (i) plan 含独立 plan-audit 通过记录**：grep `docs/plans/` 含 `知识库.*统计|knowledgeBase.*usageCount|采纳.*RESOLVED|无匹配.*建议|UC-CS-05.*裁剪` 的 plan——**无独立 plan 专门裁决 UC-CS-05 采纳统计/采纳转解决/无匹配建议裁剪**。`docs/plans/2026-07-08-0056-2-cs-knowledge-base-search-suggestion.md` 是 UC-CS-05 主计划，未含上述三项裁剪的独立 plan-audit。**判据 (i) 不成立**。
    - **判据 (ii) owner doc 显式 documented simplification 标注且经人工批准**：`README.md §核心业务对象:38`「知识库（KnowledgeBase）| 可发布的知识/FAQ 文章，支持按标题与正文关键词检索，并按工单主题智能推荐候选方案（详见 use-cases.md）」**L2 直接指向 use-cases.md 真相源无 simplification**；cs 域无独立 knowledge-base.md owner doc（grep `module-cs` + `docs/design/customer-service/` 无独立 KB owner doc，仅 README.md 简述）。**L2 无显式 documented simplification 标注**——更谈不上经人工批准。**判据 (ii) 不成立**。
    - **判据 (iii) product-scope 范围裁剪登记**：`docs/requirements/product-scope.md` grep `知识库.*统计|采纳.*RESOLVED|无匹配.*建议|knowledgeBase.*usageCount|UC-CS-05` **零命中**——product-scope **未将 UC-CS-05 采纳统计/采纳转解决/无匹配建议列入"不在范围"或"后续阶段"**。**判据 (iii) 不成立**。
  - **三判据均不成立 → 非 documented simplification → Q4=(a) 强制实现**。**非 P0**：⑦⑧⑨缺失不破坏活跃数据（搜索/Top5/采纳 NOTE 主路径完整 + 工单状态机完整 + cs 域不产生 GL 凭证）+ 不破坏会计过账正确性 + 非核心循环断裂（采纳审计行完整，仅缺自动转 RESOLVED + KB 统计 + 无匹配建议路径）+ 不破坏数据隔离/安全。**新登记 `P1-RC-058`**（UC-CS-05 ⑦采纳→RESOLVED + ⑧采纳统计 + ⑨无匹配建条目建议 合并缺失）。

#### UC-CS-06 工单升级为质量事件 → **P1**（②前置接受 on 状态机层面；③④⑤⑥⑦⑧⑨全缺 = 全域结构性未实现）

- **② 前置 工单处于 IN_PROGRESS = 接受 on 状态机层面**（§2 判据"接受 on 状态机层面"）：6 态状态机 + IN_PROGRESS 状态可达经 A1.37/A2.14 PASS 复用。但**质量升级路径全无利用此状态**——前置状态在但无任何 mutation 检查 IN_PROGRESS 触发质量升级。
- **③④⑤⑥⑦⑧⑨ → P1**（§2 P1①功能完全缺失 + §2 P1②异常路径未实现——**§4 三判据关键裁决**）：
  - L1 逐字 `use-cases.md:111-120` 流程①-⑤ + 后置 + 异常**全条无 Deferred 注记**（grep `use-cases.md` `Deferred|后续阶段|Non-Goal` 仅 UC-CS-05 `:101` LIKE-Deferred，UC-CS-06 全无）。
  - L3 实仓：grep `NonConformance|ErpQa|qualityIssue|isQuality|ncrId|non_conformance|markQuality|flagQuality|defectDescription|materialLot|batchInfo|IErpQaNonConformanceBiz|app.erp.qa` 跨 `module-cs/erp-cs-service/src/main` **零业务命中**（仅 `app-erp-cs.orm.xml:22` 头注释愿望性文档）。`ErpCsTicketBizModel` 12 方法清单**无一涉质量**；`ErpCsTicket`/`ErpCsTicketAction` ORM 无 ncrId/nonConformanceId/缺陷描述/物料/批次列；`ACTION_TYPE_ESCALATE`（`ErpCsConstants:37`）**仅**被 SLA 超时扫描 `ErpCsTicketScanOverdueTicketsProcessor:70` 消费，**非质量**；无 `IErpQaNonConformanceBiz` 注入；无"服务不可用后台重试"scheduler。
  - **§4 三判据关键复核**（owner doc Non-Goal 标注的人工批准痕迹核查——P1 裁决核心）：
    - **判据 (i) plan 含独立 plan-audit 通过记录**：grep `docs/plans/` 含 `UC-CS-06|质量.*升级.*cs|cs.*NCR|cs.*quality|工单.*质量事件` 的 plan——**无独立 plan 专门裁决 UC-CS-06 质量事件跨域联动裁剪**。**判据 (i) 不成立**。
    - **判据 (ii) owner doc 显式 documented simplification 标注且经人工批准**：`README.md §跨域协作:49`「质量问题升级 \| quality \| 工单中确认是产品质量问题 → 创建 ErpQaNonConformance」**= 活跃设计契约非 Non-Goal**；`state-machine.md §外部依赖:86`「质量问题升级到 NCR \| 处理人标记 quality 联动 → 创建 ErpQaNonConformance」**= 活跃设计契约**；`README.md §延迟项与非目标:97-101` Non-Goal 仅 3 项（SLA working-days/L1 升级/SLA pause），**UC-CS-06 不在 Non-Goal 列表**。**owner doc 显式契约承诺 + 未声明 Deferred**——更经人工批准痕迹核查：git log `docs/design/customer-service/README.md` + `docs/design/customer-service/state-machine.md` 跨域协作表 commit history **全 AI commits**（conventional commit message + AI author pattern），无人工 reviewer 显式批准裁剪的痕迹，但此处不是裁剪而是**承诺**——owner doc 是承诺"应实现"非"deferred"，故 (ii) 在 Deferred 标注意义上**不适用**（owner doc 主动承诺应实现，是 P1 强制实现的支撑证据，不是降级依据）。**判据 (ii) 在 Deferred-simplification 意义上不成立**（owner doc 反向支持 P1）。
    - **判据 (iii) product-scope 范围裁剪登记**：`docs/requirements/product-scope.md` grep `UC-CS-06|工单.*质量|cs.*NCR|质量事件.*升级` **零命中**——product-scope **未将 UC-CS-06 质量事件跨域联动列入"不在范围"或"后续阶段"**。**判据 (iii) 不成立**。
  - **三判据均不成立 → 非 documented simplification → Q4=(a) 强制实现**。owner doc 跨域协作表 + state-machine.md 外部依赖表是**活跃设计契约承诺**，反向支撑 P1 裁决（owner doc 承诺应实现而非 deferred）。**`app-erp-cs.orm.xml:22` 头注释「质量问题升级到 quality 域 NCR」是愿望性文档，单注释不构成行为证据**（methodology §1 L3 anti-hollow：行为断言是 anti-hollow 核心载体）。
  - **非 P0**：UC-CS-06 全域缺失**不破坏活跃数据**（工单状态机主路径完整 + cs 域不产生 GL 凭证 + 质量联动缺失仅影响可追溯性非数据完整性）+ **不破坏会计过账正确性**（cs 域无凭证产生；quality 域 NCR 自有过账路径独立于 cs）+ **非核心循环断裂**（工单生命周期可手工全链 + NCR 可在 quality 域独立创建非必须经 cs 触发）+ **不破坏数据隔离/安全**。**新登记 `P1-RC-057`**（UC-CS-06 全域结构性未实现——③标记质量+缺陷信息 + ④调 quality I*Biz 建 NCR + ⑤ESCALATE 审计关联 NCR + ⑥⑦⑧跨域可追溯 + ⑨服务不可用后台重试 全缺）。

#### UC-CS-07 预设应答使用 → **接受 on 主路径 + 1 新 P2**（①②③⑤⑥⑦⑧⑨⑩⑪接受；④分类树浏览 P2）

- **①②③ = 接受**（§2 判据"接受"）：①触发 + ②前置（`ErpCsConfigs.isCannedResponseEnabled()` 默认 true + `assertActive:76-81/154-159` isActive 守卫）+ ③前端入口（`ErpCsTicket.view.xml` AMIS 按钮 + GraphQL mutation/query 接线）。L3/L4/L5 三源一致 + `TestErpCsCannedResponseBiz` inactive 测试强测。
- **④ 分类树浏览 → P2**（§2 P2①次要验收标准未完全满足，主路径 OK 边界弱）：L1 逐字 `use-cases.md:132` 流程②「系统展开分类树（billing/technical/account），显示可用模板列表」；L2 owner doc `canned-response.md §3.1:137` 显式「展开分类树（billing / technical / account）」与 L1 一致。L3 实仓：`ErpCsCannedCategory` 实体（`app-erp-cs.orm.xml:397-429`）字段载体完整（`parentId` 自引用 + `parent` to-one 树形）+ `ErpCsCannedCategoryBizModel` 标准 CRUD list 兜底可用，**但无独立 `getCategoryTree`/`browseCatalog` 方法**（grep 跨 main 零命中）——分类树浏览经宏匹配 `suggestForTicket:89-126`（按 type+priority 自动推荐）替代 + CRUD list 兜底。**主路径[宏匹配+CRUD list]OK 边界[分类树驱动浏览]弱**——行为可达成（客服可经宏匹配或 CRUD list 浏览分类），但形式偏离 L1 字面「展开分类树」UX。
  - **§4 三判据复核**：(i) 无独立 plan-audit 专门裁决分类树浏览裁剪；(ii) owner doc `canned-response.md:7` 显式「§四（管理功能 Excel 导入/导出 + §五 分类树校验）归 Deferred successor」——**仅管理功能 + 校验 Deferred**，**分类树浏览本体未 Deferred**（`§3.1:137` 显式要求），但实现以宏匹配+CRUD 替代；(iii) `product-scope.md` grep `分类树|categoryTree|browseCatalog` 零命中未将分类树浏览裁剪。**三判据在"人工批准"意义上不满足但实际影响受限**（主路径[宏匹配+CRUD]行为可达成 + ErpCsCannedCategory 实体载体完整易扩展 + 不破坏活跃数据/GL/状态机）→ **倾向 P2 watch-only**。**声明 Q4=(a) 张力**：若严格按 Q4 应升级 P1（分类树浏览是 L1 字面验收标准流程②），但实际影响限于"UX 形式"非"行为不可达成"——宏匹配按 type+priority 自动推荐候选 + CRUD list 浏览分类，行为上客服可达成"按分类选择模板"目标。**新登记 `P2-RC-053`**（UC-CS-07 ④分类树浏览缺独立方法 watch-only）。
- **⑤ 渲染系统变量 = 接受**（§2 判据"接受"）：`renderTemplate:77-85` + `resolveSystemVars:161-184` 自动填充 `{today}/{now}/{agent_name}/{ticket_id}/{customer_name}`（经 `IErpMdPartnerBiz.findById` 解析 customer_name）。L3/L4/L5 三源一致 + `testCustomVariableOverridesSystem:42-54` 强测。
- **⑥ 必填校验 = 接受**（§2 判据"接受"）：`CannedResponseRenderer.validateRequired:97-108` 抛 `ERR_CANNED_RESPONSE_REQUIRED_VAR_MISSING` + `variableKey` 参数。L3/L4/L5 三源一致 + `testRequiredVarMissingThrows:56-67` 强测错误码 + 参数双断言。
- **⑦ 插入编辑框 = 接受**（§2 判据"接受"）：`applyCannedResponse:45-61` 返回 rendered 字符串，前端 AMIS 接管填入编辑框。
- **⑧ NOTE 审计 + ⑩ 渲染内容记录 = 接受**（§2 判据"接受"）：`writeNoteAction:127-137` 写 `ErpCsTicketAction`（actionType=NOTE，content=rendered）——**content 字段承载渲染后正文非原始模板**，L1 后置「记录渲染后的应答内容」语义满足。L3/L4/L5 三源一致 + `testApplyCannedResponseIncrementsUsageAndWritesAction:179-197` 强测 hasNote。
- **⑨ usageCount+1 = 接受**（§2 判据"接受"）：`applyCannedResponse:52-55` `resp.setUsageCount(cur+1)` + `dao().updateEntity(resp)`。`ErpCsCannedResponse.usageCount` ORM 列存在（propId 12 `:447`）。L3/L4/L5 三源一致 + `testApplyCannedResponseIncrementsUsageAndWritesAction:179-197` 强测 usageCount 5→6。
- **⑪ 异常 缺失必填变量 → 禁止发送 = 接受**（§2 判据"接受"）：`validateRequired` 抛错 → applyCannedResponse 在 `CannedResponseRenderer.render` 阶段异常，不进入 usageCount+1/writeNoteAction 阶段——**"禁止发送"语义成立**（无 NOTE 审计行写入 = 渲染失败 = 禁止）。L3/L4/L5 三源一致 + `testRequiredVarMissingThrows:56-67` 强测。

### 切片总结

| UC | 结论 | 命中判据 | Finding |
|----|------|---------|---------|
| UC-CS-05 | **部分接受 + 1 新 P1**（③④⑤⑥接受 on 主路径/NOTE 审计；⑦采纳→RESOLVED + ⑧采纳统计 + ⑨无匹配建条目建议 P1） | §2 接受 / §2 P1①+P1② / §4 三判据均不成立 | **P1-RC-058**（新，⑦⑧⑨合并缺失） |
| UC-CS-06 | **P1**（②前置接受 on 状态机层面；③④⑤⑥⑦⑧⑨全缺 = 全域结构性未实现） | §2 P1①+P1② / §4 三判据均不成立 / owner doc 反向支撑 P1 | **P1-RC-057**（新，全域结构性未实现） |
| UC-CS-07 | **接受 on 主路径 + 1 新 P2**（①②③⑤⑥⑦⑧⑨⑩⑪接受；④分类树浏览 P2 watch-only） | §2 接受 / §2 P2① / §4 三判据在"人工批准"意义上不满足但实际影响受限 | **P2-RC-053**（新，④分类树浏览缺独立方法 watch-only） |

**零 P0**（候选缺口均不破坏活跃数据/会计正确性/核心循环——UC-CS-05 搜索/Top5/采纳 NOTE 主路径完整 + 工单状态机完整 + cs 域不产生 GL 凭证 + 采纳统计缺失致运营洞察弱但不破坏数据 + 采纳→RESOLVED 缺失有手工 resolve 替代路径 + 无匹配建条目建议缺失致运营效率弱但不破坏数据 + UC-CS-06 质量联动缺失影响可追溯性非数据完整性 + NCR 可在 quality 域独立创建 + UC-CS-07 宏匹配+CRUD 兜底行为可达成）。

---

## 6. 与 arm-index 衔接（§7 复用/新增裁决 + 设计张力注记）

### 6.1 复用裁决

- **A2.14 cs Ticket 6 态状态机 + SLA 计时 + L1 升级 PASS**：复用为 L5 既有证据（§去重协议，§4 复用）。**仅 UC-CS-06 ②前置 IN_PROGRESS 状态机层面满足复用**——质量联动行为本身 UC-CS-06 全缺无 MA2 行为复用。
- **A1.37/A1.38 cs 域既有 RC finding（P1-RC-054/055/056 + P2-RC-051/052）**：全部不同控制点（UC-CS-01/02/03/11/04 工单生命周期/SLA/计时器维度 vs 本切片 UC-CS-05/06/07 知识/质量/预设维度），**互补不重复**，本切片不复审。
- **A2.14 cs 域 zero cross-domain daoFor + 跨域 Facade 仅 IErpMdPartnerBiz + IErpSysNotificationBiz（A2.14 §cs `:320`）**：本切片 UC-CS-06 全域缺失的根因之一即"无 quality Facade 注入"——本切片引用 A2.14:320 cs 域 Facade 注入现状作为 UC-CS-06 跨域联动缺失的基线注记（不同控制点：Facade 注入现状 vs 需求契约要求 quality I*Biz 调用），不重审跨域 Facade 维度。
- **P1-MA2-086 resolved R1.28 + UC-CS-04 ⑩重复升级/L2-L3（A1.38 P1-RC-056）**：A1.38 cs-F2 切片已登记 SLA 升级链维度 finding，本切片 UC-CS-06 ESCALATE 字段借用问题（ESCALATE 仅 SLA 用非质量）属**不同控制点**（SLA 超时升级链 vs 质量事件升级）——本切片不复审 SLA 升级链维度，仅注记 ESCALATE 字段在 UC-CS-06 ⑤语境下被 SLA 路径独占。

### 6.2 新增裁决（grep arm-index 后无同域同控制点 RC finding）

> cs 域**无既有 RC finding 涉及知识库采纳统计/采纳转解决/无匹配建议/质量事件跨域联动/预设应答分类树浏览**。grep arm-index 「cs knowledge」「knowledgebase」「canned」「escalat.*qual」「NCR.*cs」「adopt」「searchKnowledge」「suggestForTicket」「applyCannedResponse」「renderTemplate」「usageCount」「分类树」「qualityIssue」「non-conformance」RC 系列零命中。既有 cs RC finding 全是 A1.37/A1.38 工单生命周期/SLA/计时器维度（P1-RC-054/055/056 + P2-RC-051/052），**不同控制点**。既有 cs audit-remediation MA2 finding 仅 P2-MA2-067（滞留升级 watch-only）+ P1-MA2-086（cron 并发幂等 resolved R1.28）——**两者不同控制点**。

| 新 Finding | 域 | UC | 根因 | 与既有 finding 差异 | 分级 |
|-----------|---|----|------|-------------------|------|
| **`P1-RC-057`** | cs | UC-CS-06 ③④⑤⑥⑦⑧⑨ | UC-CS-06 工单升级为质量事件**全域结构性未实现**——L1 `use-cases.md:105-120` 流程①-⑤+后置+异常全条无 Deferred 注记要求「处理人标记质量问题 + 填缺陷/物料/批次 → 系统调 quality 域 IErpQaNonConformanceBiz 建 ErpQaNonConformance → ESCALATE 审计关联 NCR → NCR 闭环后工单可查 → 服务不可用后台重试」，L3 grep `NonConformance\|ErpQa\|qualityIssue\|isQuality\|ncrId\|non_conformance\|markQuality\|flagQuality\|defectDescription\|materialLot\|batchInfo\|IErpQaNonConformanceBiz\|app.erp.qa` 跨 `module-cs/erp-cs-service/src/main` **零业务命中**（仅 `app-erp-cs.orm.xml:22` 头注释愿望性文档无代码）+ 无 quality I*Biz 注入（无 `app.erp.qa.*` import）+ `ErpCsTicketBizModel` 12 方法清单无一涉质量 + `ErpCsTicket`/`ErpCsTicketAction` ORM 无 ncrId/nonConformanceId/缺陷描述/物料/批次列 + `ACTION_TYPE_ESCALATE`（`ErpCsConstants:37`）仅被 SLA 超时扫描 `ErpCsTicketScanOverdueTicketsProcessor:70` 消费非质量 + 无"服务不可用后台重试"scheduler。**owner doc 反向支撑 P1**：`README.md §跨域协作:49`「质量问题升级 \| quality \| 工单中确认是产品质量问题 → 创建 ErpQaNonConformance」+ `state-machine.md §外部依赖:86`「质量问题升级到 NCR \| 处理人标记 quality 联动 → 创建 ErpQaNonConformance」**= 活跃设计契约承诺非 Non-Goal**（Non-Goal 列表 `README.md:97-101` 仅 3 项：SLA working-days/L1 升级/SLA pause，UC-CS-06 不在其中）。**§4 三判据均不成立**：(i) 无独立 plan-audit + (ii) owner doc 反向承诺应实现非 deferred-simplification + (iii) product-scope grep UC-CS-06\|工单.*质量\|cs.*NCR 零命中未裁剪 → Q4=(a) 强制实现。 | 新根因（cs 域质量事件跨域联动维度首批 RC finding；与 P1-RC-056 SLA 重复升级/L2-L3 不同控制点[SLA 超时升级链 vs 质量事件升级]——ESCALATE 字段在两 finding 语境下不同含义但属不同控制点不重审；与 P1-MA1-022 不同域不同方法；与 quality 域 NCR-CAPA A1.32 done 不同域不同方向） | P1（§2 P1①功能完全缺失——UC-CS-06 流程①-⑤+后置+异常全缺 + §2 P1②异常路径未实现——服务不可用后台重试未实现） |
| **`P1-RC-058`** | cs | UC-CS-05 ⑦⑧⑨ | UC-CS-05 知识库搜索与建议**采纳统计 + 采纳转解决 + 无匹配建议 三项合并缺失**——L1 `use-cases.md:95-99` 流程⑤「如采纳的文章解决了问题，工单直接标记为 RESOLVED」+ 后置「采纳记录计入知识库使用统计」+ 异常「无匹配文章 → 提示客服创建新知识库条目（工单解决后自动推送建议）」，L3 实仓：⑦`ErpCsTicketBizModel.adoptKnowledge:200-208` 仅写 NOTE 审计行**不修改 ticket.status**（resolve 路径仅经 `ErpCsTicketResolveProcessor` 独立 mutation）+ ⑧`ErpCsKnowledgeBase` ORM（`app-erp-cs.orm.xml:362-394`）**无 usageCount/viewCount/adoptCount 列**（grep `usageCount\|viewCount\|adoptCount` 跨 `module-cs/model/app-erp-cs.orm.xml` 仅 `ErpCsCannedResponse.usageCount:447` 命中，KB 实体零命中）+ adoptKnowledge 不调任何 KB 计数更新 + ⑨grep `suggestCreate\|建议创建\|创建新知识库\|knowledgeSuggest\|noMatchSuggestion` 跨 main **零命中**（空集返回后无任何"提示建条目"逻辑 + 无 scheduler）。**§4 三判据均不成立**：(i) 无独立 plan-audit + (ii) owner doc `README.md §核心业务对象:38`「知识库...（详见 use-cases.md）」**直接指向 L1 真相源无 simplification 标注** + cs 域无独立 knowledge-base.md owner doc + (iii) product-scope grep 知识库.*统计\|采纳.*RESOLVED\|无匹配.*建议 零命中未裁剪 → Q4=(a) 强制实现。**主路径[搜索/Top5/采纳 NOTE]完整 + 强测**——三项缺失是后置+异常+流程⑤局部缺失，非核心循环断裂。 | 新根因（cs 域知识库采纳统计/采纳转解决/无匹配建议维度首批 RC finding；grep arm-index「cs knowledge」「adoptKnowledge」「采纳统计」「无匹配建议」RC 系列零命中；与 P1-RC-054 工单创建自动富化不同 UC 不同控制点；与 P1-RC-057 质量事件跨域联动不同 UC） | P1（§2 P1①功能实质偏离验收标准——采纳→RESOLVED + 采纳统计 + 无匹配建议三项验收标准缺失 + §2 P1②异常路径未实现——无匹配建条目建议未实现） |
| **`P2-RC-053`** | cs | UC-CS-07 ④ | UC-CS-07 预设应答使用**分类树浏览缺独立方法（宏匹配+CRUD 兜底）**——L1 `use-cases.md:132` 流程②「系统展开分类树（billing/technical/account），显示可用模板列表」，L2 `canned-response.md §3.1:137` 与 L1 一致显式要求「展开分类树」。L3 实仓：`ErpCsCannedCategory` 实体（`app-erp-cs.orm.xml:397-429`，`parentId` 自引用 + `parent` to-one 树形）字段载体完整 + `ErpCsCannedCategoryBizModel` 标准 CRUD list 兜底可用，**但无独立 `getCategoryTree`/`browseCatalog` 方法**（grep `getCategoryTree\|browseCatalog\|loadCategoryTree\|treeOfCategories\|categoryTree` 跨 main 零命中）——分类树浏览经宏匹配 `suggestForTicket:89-126`（按 type+priority 自动推荐）替代 + CRUD list 兜底。**主路径[宏匹配+CRUD list]OK 边界[分类树驱动浏览]弱**——行为可达成（客服可经宏匹配或 CRUD list 浏览分类），但形式偏离 L1 字面「展开分类树」UX。**§4 三判据复核**：(i) 无 plan-audit + (ii) owner doc `canned-response.md:7` 仅「§四管理功能+§五分类树校验」Deferred，**分类树浏览本体未 Deferred**（`§3.1:137` 显式要求），但实现以宏匹配+CRUD 替代 + (iii) product-scope grep 分类树\|categoryTree\|browseCatalog 零命中。**三判据在"人工批准"意义上不满足但实际影响受限**（主路径行为可达成 + ErpCsCannedCategory 实体载体完整易扩展 + 不破坏活跃数据/GL/状态机）→ 倾向 **P2 watch-only**。**声明 Q4=(a) 张力**：若严格按 Q4 应升级 P1（分类树浏览是 L1 字面验收标准），但实际影响限于"UX 形式"非"行为不可达成"。 | 新根因（cs 域预设应答分类树浏览维度首批 RC finding；grep arm-index「cs 分类树」「categoryTree」「browseCatalog」RC 系列零命中；与 P1-RC-057/058 不同 UC 不同控制点） | P2（§2 P2①次要验收标准未完全满足——主路径[宏匹配+CRUD]行为可达成 OK 边界[分类树驱动浏览]弱 watch-only 声明 Q4=(a) 张力） |

### 6.3 双向可追溯

- 新 finding 入 arm-index RC 发现追踪分区（§见 arm-index 更新）。
- finding 修复行预留 MR1（R1.0 展开为 RC-R1.n 时引用 finding ID）。
- arm-index finding 行修复状态列待 MR1 修复完成后回填 `done`。

### 6.4 UC-CS-06 跨域联动修复须协调 quality 域 NCR 创建路径（设计张力注记）

**关键协调约束**（修复 P1-RC-057 时须遵守）：

- **背景**：UC-CS-06 要求 cs 域调用 quality 域 `IErpQaNonConformanceBiz` 创建 NCR。quality 域 NCR-CAPA 创建路径已在 A1.32 切片 done（`docs/audits/rc-ma1-a1-32-...`，状态机+过账+CAPA 闭环完整）——cs 域只需调用既有 quality 域 I*Biz 接口注入即可。
- **副作用预警**：cs 域 ErpCsTicket 加 NCR 关联列若触及 ORM 结构变更须 ask-first（§5 ORM 类）；若仅经弱指针（relatedNcrCode 字符串）或独立审计行（ErpCsTicketAction 加 ncrId 列）也属 ORM 结构变更须 ask-first。
- **修复方案要求**：P1-RC-057 修复须采用**跨域 I*Biz 注入 + ErpCsTicket 加 NCR 关联弱指针 + ErpCsTicketAction 加 NCR 引用列**方案（注入 `IErpQaNonConformanceBiz` Facade + `ErpCsTicketBizModel` 增 `escalateToQuality` @BizMutation[处理人标记质量问题+缺陷/物料/批次入参 → status 守卫 IN_PROGRESS → 调 IErpQaNonConformanceBiz.createNcr 创建 NCR → 写 ESCALATE 审计行关联 NCR 编号 → 失败 catch + 标记 pendingNcr + 注册 nop-job 后台重试] + `ErpCsTicket` 加 NCR 关联弱指针列[或经 ErpCsTicketAction ncrId 列承载] + 服务不可用后台重试 nop-job bean）。
- **保护区域**：修复触及 ORM 结构变更（`ErpCsTicket` 加 NCR 关联列 / `ErpCsTicketAction` 加 ncrId 列 / 可能新增 NCR 重试 Job 实体）→ **须 ask-first + 独立 plan-audit**（§5 ORM 结构变更类）。

### 6.5 修复触及保护区域标注（§5 预授权/ask-first）

| Finding | 修复范围 | 保护区域 | 门控 |
|---------|---------|---------|------|
| P1-RC-057（UC-CS-06 全域缺失） | `ErpCsTicketBizModel` 增 `escalateToQuality` @BizMutation（处理人标记质量问题 + 缺陷描述/物料/批次入参 → status 守卫 IN_PROGRESS → 注入 `IErpQaNonConformanceBiz` 调 createNcr 创建 ErpQaNonConformance → 写 ESCALATE 审计行关联 NCR 编号 → 失败 catch + 标记 pendingNcr 字段 + 注册后台重试）+ `ErpCsTicket` 加 NCR 关联弱指针列（如 `relatedNcrCode` 字符串 或 `ncrId` long）+ `ErpCsTicketAction` 加 ncrId 引用列（让 ESCALATE 行关联 NCR 编号）+ 服务不可用后台重试 nop-job bean（扫 pendingNcr=true 工单 + 重试调 IErpQaNonConformanceBiz）+ `ErpCsConfigs`/`ErpCsConstants` 声明 quality 联动相关 config（如 `erp-cs.quality-escalation-retry-max`）+ AMIS 工单详情页增"标记质量问题"按钮 | **触及 ORM 结构变更**（`ErpCsTicket` + `ErpCsTicketAction` 加字段 + 可能新增 NCR 重试 Job 实体） | **ask-first + 独立 plan-audit §5 ORM 类**（须协调 quality 域 IErpQaNonConformanceBiz 注入 + NCR 关联 + 后台重试——见 §6.4 设计张力注记；IErpQaNonConformanceBiz 已存在[A1.32 done]无需新建 quality 域接口） |
| P1-RC-058（UC-CS-05 采纳统计+转解决+无匹配建议） | `ErpCsKnowledgeBase` ORM 加 `usageCount`(int defaultValue=0) 列[采纳统计载体]（**触及 ORM 须 ask-first**）+ `ErpCsTicketBizModel.adoptKnowledge:200-208` 增 `setUsageCount(kb.usageCount+1)` 更新 KB 计数（经 IErpCsKnowledgeBaseBiz Facade 或 daoProvider）+ 增可选 `autoResolve` 入参（true 时 setStatus(RESOLVED) + writeAction NOTE 含"采纳文章解决问题"语义）+ 增"无匹配建议"逻辑（searchKnowledge 返回空时返回提示标记或独立 mutation `suggestCreateKbEntry` 在工单 resolve 后推送通知经 `IErpSysNotificationBiz`）+ AMIS 工单表单 searchKnowledge 空结果展示"建议创建知识库条目"+ `IErpSysNotificationBiz` 通知事件 wiring | **触及 ORM 结构变更**（`ErpCsKnowledgeBase` 加 `usageCount` 列） | **ask-first + 独立 plan-audit §5 ORM 类**（KB usageCount 列加 ORM 须 ask-first；其他 BizModel 逻辑+notify 接线属预授权） |
| P2-RC-053（UC-CS-07 分类树浏览） | `ErpCsCannedCategoryBizModel` 增 `getCategoryTree` @BizQuery（递归 parentId 构建树形 JSON 返回 [{id, code, name, children:[...]}] + config-gated `erp-cs.canned-response-category-max-depth` 默认 3 限深 + 按 sequence 排序）+ AMIS 工单回复编辑框侧栏挂接 `ErpCsCannedCategory__getCategoryTree` 渲染树形分类选择 + 选择分类后过滤该分类的 active ErpCsCannedResponse 列表 | 纯 BizModel 查询方法 + AMIS 接线 | **预授权**（代码逻辑类，不触 §5 ask-first——getCategoryTree 是只读查询方法 + ErpCsCannedCategory 实体载体完整不加字段 + AMIS 树形组件属既有范式；按 roadmap 预授权类目可自动执行） |

---

## 7. 静态存疑点清单（供 MA4 A4.1/A4.2 运行时展开）

> L5 无法静态定论、需运行时确认的点。**P0 即时通道未触发**（本切片无 P0——UC-CS-05 搜索/Top5/采纳 NOTE 主路径完整 + cs 域不产生 GL 凭证 + UC-CS-06 质量联动缺失影响可追溯性不破坏数据/会计正确性 + UC-CS-07 宏匹配+CRUD 兜底行为可达成）。

| 编号 | 存疑点 | 展开方式 |
|------|--------|---------|
| SP-1 | **searchKnowledge LIKE 匹配在大文章量下的时延/相关性质量是否触发 `use-cases.md:101` Deferred 触发条件**：`searchKnowledge:37-80` LIKE 关键词匹配（title+content `%kw%`）+ 内存相关性排序（title-match 优先 + createTime desc）。L1 `:101` Deferred 注记「触发条件：文章量超万级或 LIKE 时延/相关性质量不满足时」需运行时确认——大文章量（> 10000）下 LIKE 查询时延 + 内存排序开销 + 相关性质量（仅 title-match 0/1 二值 + createTime desc 太粗）是否触发全文搜索引擎升级需求 | A4.1 运行时：seed 10000+ KB 文章（含 title/content 长文本）+ 测 searchKnowledge 时延（应 < 500ms 业务可接受）+ 相关性质量（title-match 优先 + createTime desc 是否够用 vs 全文引擎 TF-IDF/BM25）；与 use-cases.md:101 Deferred 触发条件对照 |
| SP-2 | **suggestForTicket 取首 token 对多词 subject 的命中召回率**：`extractKeyword:140-148` 按 `[\s,，。.!！?？;；:：、/\\()（）\[\]【】]+` 分词取首个 ≥ `SUGGEST_SUBJECT_MIN_LENGTH` token——多词 subject（如「ERP 登录页面报错 500」）仅取首 token（如「ERP」）召回率低（漏掉「登录」「报错」「500」匹配文章）。运行时召回率与业务可接受度需确认 | A4.1 运行时：seed 多主题文章 + 用多词 subject 测 suggestForTicket 召回率 + 与全部 token OR 匹配对比 + 评估业务影响（应首 token 召回率 ≥ 60% 业务可接受，或建议改进为多 token OR 匹配） |
| SP-3 | **applyCannedResponse usageCount 并发递增是否经乐观锁保护**：`applyCannedResponse:52-55` `Integer cur = resp.getUsageCount(); resp.setUsageCount(cur == null ? 1 : cur + 1); dao().updateEntity(resp);`——并发多客服同时 apply 同一 cannedResponse 时 usageCount 是否丢失更新（read-modify-write 非原子）。ErpCsCannedResponse 有 `version` 字段（orm.xml:449 std domain=version 乐观锁载体）但 `updateEntity(resp)` 是否自动触发 version 校验需确认 | A4.1 运行时：并发 10 线程同时 applyCannedResponse 同一 crId + 断言最终 usageCount = 初始+10（若 < 则并发丢失更新）+ 验证 `dao().updateEntity(resp)` 是否自动经 version 乐观锁抛 `OptimisticLockException`；若丢更新则建议改 SQL `UPDATE ... SET usageCount = usageCount + 1` 原子递增 |
| SP-4 | **ErpCsCannedCategory CRUD 是否实际驱动前端分类树选择**：`ErpCsCannedCategoryBizModel` 标准 CRUD 存在 + `ErpCsCannedCategory` 实体载体完整，但 grep AMIS view.xml 是否含 category tree 选择组件需确认（P2-RC-053 缺独立 getCategoryTree 方法，依赖前端是否经 CRUD list 自建树形 UX）。运行时前端实际是否渲染分类树驱动客服选模板需确认 | A4.1 运行时：grep `module-cs/erp-cs-web` AMIS view.xml + xbiz 是否含 ErpCsCannedCategory tree/dialog 组件 + 客服实际选 canned response 工作流（是经宏匹配自动推荐 vs 经分类树手动浏览）；若 AMIS 已有树形组件则 P2-RC-053 影响降级（仅缺独立后端方法），若无则 P2-RC-053 影响升级（前端无分类树 UX） |
| SP-5 | **UC-CS-06 ⑤ESCALATE 审计字段借用冲突在 SLA + 质量双路径下的运行时行为**：`ACTION_TYPE_ESCALATE="ESCALATE"` 当前仅 SLA 路径写（`ErpCsTicketScanOverdueTicketsProcessor:70`）。若 P1-RC-057 修复时质量路径也写 ESCALATE 审计行，则同 ticket 可能既有 SLA-ESCALATE 又有 Quality-ESCALATE——审计行区分（经 fromStatus/toStatus + content 文本）或需独立 actionType（如 QUALITY_ESCALATE）。运行时实际行为需确认 | A4.1 运行时：与 P1-RC-057 修复协同——若修复采用独立 actionType（如 QUALITY_ESCALATE）则无冲突；若复用 ESCALATE 则需验证审计查询/报表是否正确区分两路径（fromStatus/toStatus + content 文本区分）；与 R1.28[P1-MA2-086] `hasEscalationAction` 幂等守卫交互需复核（守卫按 actionType=ESCALATE 查询，质量路径写 ESCALATE 会被守卫误判为已升级跳过 SLA 升级） |

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual vs baseline 汇总如下表。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（methodology §8 描述"退出码恒 0"，本次实测 `EXIT=0`），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不以** checker 脚本退出码作为门控通过依据。

  | 规则 | baseline | actual | 状态 |
  |------|----------|--------|------|
  | R1a/R1b/R1c | 0/0/0 | 0/0/0 | ✅ |
  | R1d | 14 | 14 | ✅ |
  | R2a | 34 | 34 | ✅ |
  | R2b | 240 | 229 | ✅ (≤) |
  | R2c | 1380 | 1382 | ⚠ +2（**非本审计引入**——本审计为只读审计零生产代码变更，delta 来自其他在途工作，登记供 CI/后续基线对账；与 A1.37/A1.38 报告记录的同一 delta 基线一致） |
  | R2d | 32 | 34 | ⚠ +2（**非本审计引入**——同上，只读审计） |
  | R3 | 5 | 5 | ✅ |
  | R4/R5/R7/R8/R11 | 0/0/0/0/0 | 0/0/0/0/0 | ✅ |

  **本报告无生产代码变更（纯审计报告），checker 无回归风险**。R2c/R2d 的 +2 delta 系本审计之外的在途工作引入（与 A1.37/A1.38 报告记录的同一 delta 基线一致，非本切片所致）；本审计未修改任何 `.java`/`.xml`/`.yaml` 生产文件（仅新增本报告 + 更新 arm-index + plan 状态）。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding（P1-RC-057 + P1-RC-058 + P2-RC-053）已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（§6.1/§6.2），无未经比对直接新建的 finding。UC-CS-06 全域缺失引用 A2.14:320 cs 域 Facade 注入现状作为基线注记但不重审跨域 Facade 维度；UC-CS-06 ESCALATE 字段借用问题与 A1.38 P1-RC-056 SLA 升级链不同控制点（质量事件升级 vs SLA 超时升级链），互补不重复；UC-CS-05 采纳统计/采纳转解决/无匹配建议 + UC-CS-07 分类树浏览经 grep 确认为 cs 域新控制点（既有 cs RC finding 全是 UC-CS-01/02/03/04/11 维度，不涉及 UC-CS-05/06/07）。

---

## 9. 与 MA2 报告差异增量声明（重申）

见报告开头 §9（前置声明）。**A2.14 cs 范围不含 UC-CS-05/06/07**（`2026-07-28-1020-arm-ma2-ext-domains-state-machine.md:21-23` cs 范围 = ErpCsTicket 6 态 + SLA 计时 + SLA 升级 Job，**对象限于 ErpCsTicketBizModel/SlaDeadlineCalculator/ErpCsSlaScanJob，不含 KnowledgeBase/CannedResponse/质量升级 BizModel**），**无可复用行为证据**；本切片为 UC-CS-05/06/07 的**首份行为/需求视角证据**，列明只补的需求视角差异：UC-CS-06 全域结构性未实现（P1-RC-057）/ UC-CS-05 采纳统计+采纳转解决+无匹配建议缺失（P1-RC-058）/ UC-CS-07 分类树浏览缺独立方法（P2-RC-053）。

---

## 段落完整性自检（§6 报告输出格式，9 段齐全）

- [x] §1 需求契约原文（L1 逐字引用，UC-CS-05/06/07 9/9/11 验收标准完整枚举 + UC-CS-05 LIKE-Deferred 注记逐字引用）
- [x] §2 实现证据（L3 含行号 + 跨域调用链 + ORM 实体字段——ErpCsKnowledgeBase/ErpCsCannedResponse/ErpCsCannedCategory 三实体 + ErpCsKnowledgeBaseBizModel/ErpCsCannedResponseBizModel/ErpCsTicketBizModel#adoptKnowledge/ErpCsCannedResponseApplyCannedResponseProcessor/CannedResponseRenderer/ErpCsConfigs/ErpCsConstants）
- [x] §3 测试证据（L4 注明断言强度——UC-CS-05 7 @Test 强 + UC-CS-06 零测试 + UC-CS-07 8+9=17 @Test 强；UC-CS-05 缺口 3 项 + UC-CS-06 缺口 1 项 + UC-CS-07 缺口 1 项）
- [x] §4 运行时行为证据（L5 本切片首测——A2.14 范围不含 + 24 @Test 双重证实主路径）
- [x] §5 符合性结论（五级矩阵 + 每 UC 分级 + §2 判据 + §4 三判据复核 + **P1 项 §4 三判据核 owner doc Non-Goal 标注的人工批准痕迹核查结论**——UC-CS-06 owner doc 反向支撑 P1 + UC-CS-05 L2 无 simplification + UC-CS-07 L2 仅管理功能 Deferred 分类树本体未 Deferred）
- [x] §6 与 arm-index 衔接（复用/新增裁决 + 双向可追溯 + 保护区域标注 + **UC-CS-06 跨域联动修复须协调 quality 域 IErpQaNonConformanceBiz 注入 + NCR 关联 + 后台重试 设计张力注记**）
- [x] §7 静态存疑点清单（SP-1~SP-5 供 MA4 展开——含 P1-RC-057 修复时 ESCALATE 字段借用冲突与 R1.28 幂等守卫交互的运行时确认）
- [x] §8 过程纪律自检（checker actual vs baseline 实测表 + 独立性 + 交叉去重）
- [x] §9 与 MA2 报告差异增量声明（前置 + 重申）

**真相源冻结条款遵守声明**：本审计未修改任何真相源（`product-scope.md` / `use-cases.md` / `README.md` / `canned-response.md` / `state-machine.md` 的需求契约段落）。发现的 doc 分歧（`app-erp-cs.orm.xml:22` 头注释「质量问题升级到 quality 域 NCR」愿望性文档无代码 + `README.md §跨域协作:49` 活跃设计契约承诺未实现 + `state-machine.md §外部依赖:86` 活跃设计契约承诺未实现 + `canned-response.md:7` 仅管理功能+校验 Deferred 分类树本体未 Deferred 但实现以宏匹配+CRUD 替代）记入本报告 §5（§4 三判据复核）+ §6.4（设计张力注记），不直改真相源（§9 冻结条款）。
