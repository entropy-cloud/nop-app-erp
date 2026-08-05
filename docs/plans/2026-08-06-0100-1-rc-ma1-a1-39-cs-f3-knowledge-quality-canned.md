# 2026-08-06-0100-1 rc-ma1-a1-39-cs-f3-knowledge-quality-canned 客服域 cs-F3 知识库/质量联动/预设应答需求符合性审计

> Plan Status: completed
> Last Reviewed: 2026-08-06
> Mission: requirement-compliance
> Work Item: A1.39（MA1 需求追踪矩阵审计 — cs-F3 知识库搜索与建议 / 工单升级为质量事件 / 预设应答使用：searchKnowledge+suggestForTicket LIKE 匹配 / adoptKnowledge 采纳审计 / UC-CS-06 质量事件跨域联动 / canned-response 渲染+变量校验+usageCount）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.39
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.39 的 0.2 依赖）、`2026-08-05-2330-2-rc-ma1-a1-37-cs-f1-ticket-lifecycle.md`（cs-F1 同域前置，P1-RC-054/055 + P2-RC-051 已登记）、`2026-08-05-2330-3-rc-ma1-a1-38-cs-f2-sla-escalation.md`（cs-F2 同域前置，P1-RC-056 + P2-RC-052 已登记；本批次续编自 P1-RC-056 / P2-RC-052）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点被审功能现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.39 给出 UC 清单 = `UC-CS-05/06/07`（3 UC），覆盖率 `✅ 一致`（无基线分歧 D-xx）。本切片为 cs 域第 3 个 RC 切片（A1.37/38 done；cs 域共 4 切片，本切片 + A1.40 收尾 cs 域）。

- **L1 需求契约（权威真相源）**：`docs/design/customer-service/use-cases.md`：
  - **UC-CS-05 知识库搜索与建议**（`:84`）：触发=创建/编辑工单输入 subject；前置=知识库存在已发布文章（isPublished=true）。流程：① 实时解析 subject 关键词；② **按关键词全文搜索 erp_cs_knowledge_base 表，按相关性排序**；③ 工单界面展示 Top 5 匹配文章（标题+摘要）；④ 点击查看完整内容，可"采纳"标记已参考；⑤ **如采纳文章解决问题，工单直接标记 RESOLVED**。后置：**采纳记录计入知识库使用统计**。异常：无匹配文章 → 提示客服创建新知识库条目（工单解决后自动推送建议）。**`use-cases.md:101` 注记**：searchKnowledge/suggestForTicket 采用 LIKE 关键词匹配（title+content），非全文引擎；采纳经 ErpCsTicketAction（actionType=NOTE + knowledgeBaseId 引用）；**全文搜索引擎归 Deferred**（触发条件：文章量超万级或 LIKE 时延/相关性质量不满足时）。
  - **UC-CS-06 工单升级为质量事件**（`:105`）：触发=处理人确认工单问题属于产品质量缺陷；前置=工单 IN_PROGRESS。流程：① 处理人标记"质量问题"，填写缺陷描述、物料/批次信息；② **系统调用 quality 域 I*Biz 接口创建 ErpQaNonConformance**；③ 工单操作日志记录 ESCALATE 操作（关联 NCR 编号）；④ 工单继续原有流程（RESOLVED→CLOSED），NCR 流程独立进行；⑤ **NCR 闭环后，工单可查看到 NCR 处理结果**。后置：工单与 NCR 关联，跨域可追溯。异常：**quality 域服务不可用 → 延迟创建 NCR，工单先保留状态，后台自动重试**。
  - **UC-CS-07 预设应答使用**（`:124`）：触发=客服选择插入预设应答模板；前置=模板已维护（ErpCsCannedResponse.isActive=true）。流程：① 客服点击"插入预设应答"；② **系统展开分类树（billing/technical/account），显示可用模板列表**；③ 选择模板 → 系统预览渲染后内容（自动填充系统变量）；④ 客服补充自定义变量（如 {email}）→ 系统校验必填变量；⑤ 点击插入 → 渲染正文填入工单回复编辑框；⑥ 客服可修改后发送 → 创建工单操作日志（actionType=NOTE）；⑦ **usageCount+1**。后置：工单操作日志记录渲染后的应答内容。异常：缺失必填变量 → 禁止发送，高亮未填项。
  - **L1 关键不变量**：UC-CS-05：Top5 / 采纳 NOTE 审计 / **采纳统计** / **采纳→RESOLVED** / 无匹配→建条目建议；UC-CS-06：**调 quality I*Biz 建 NCR** / ESCALATE 审计关联 NCR / **NCR 闭环结果可查** / **服务不可用后台重试**；UC-CS-07：分类树 / 渲染系统变量 / 必填变量校验 / usageCount+1 / NOTE 记录渲染内容。

- **L3 代码实现现状（实测）**——**UC-CS-05/07 完整+强测，UC-CS-06 全域结构性缺失（candidate P1）**：
  - **UC-CS-05 知识库（✅ 搜索/建议/采纳核心完整，⚠️ 统计+采纳转解决+无匹配建议缺失）**：
    - `searchKnowledge` @BizQuery `ErpCsKnowledgeBaseBizModel.java:37-80`——LIKE 匹配 title OR content（`%kw%`）+ filter `isPublished=true` + 可选 categoryId + limit 钳制（默认 5 / max 20，config `ErpCsConfigs.getKnowledgeSearchDefaultLimit():117`=5）+ 内存相关性排序（title-match 优先再 createTime desc）+ `doFindListByQueryDirectly` 绕 XMeta filterOp 限制。✅ Top5。
    - `suggestForTicket` @BizQuery `ErpCsKnowledgeBaseBizModel.java:84-98`——取 subject 首个 ≥ `SUGGEST_SUBJECT_MIN_LENGTH` token 委托 searchKnowledge（默认 limit=5）。✅。
    - `adoptKnowledge` @BizMutation `ErpCsTicketBizModel.java:200-208`——写 `ErpCsTicketAction`（actionType=NOTE，content="采纳知识库文章参考: knowledgeBaseId=…"）。**不转 RESOLVED，不增任何 KB 使用计数器**。
    - **缺失**：`ErpCsKnowledgeBase` ORM（`app-erp-cs.orm.xml:362-394`）**无 usageCount/viewCount/adoptCount 列** → UC-CS-05 后置"采纳记录计入知识库使用统计"❌；`adoptKnowledge` **不转 RESOLVED** → UC-CS-05 ⑤"采纳文章解决问题直接 RESOLVED"❌；**无"无匹配→创建新条目建议（工单解决后自动推送）"逻辑** → UC-CS-05 异常❌。
  - **UC-CS-06 质量事件联动（❌ 全域结构性未实现）**：grep 全 `module-cs/` `NonConformance|ErpQa|qualityIssue|isQuality|ncrId|non_conformance` 业务命中**仅** `app-erp-cs.orm.xml:22` 头注释（"质量问题升级到 quality 域 NCR"——愿望性文档，无代码）。**无 quality I*Biz 注入**（无 `app.erp.qa.*` import）；ErpCsTicketBizModel 12 方法（defaultPrepareSave/assign/start/resolve/close/reopen/cancel/adoptKnowledge/matchAndAttachSla/scanOverdueTickets/findSlaWarnings/findBoardData）**无一涉质量**；ErpCsTicket/ErpCsTicketAction **无 ncrId/nonConformanceId 列、无缺陷描述/物料/批次列**；`ACTION_TYPE_ESCALATE`（`ErpCsConstants:37`）**仅**被 SLA 超时扫描 `ErpCsTicketScanOverdueTicketsProcessor:70` 消费，**非质量**；**无"服务不可用后台重试"**。→ UC-CS-06 流程①-⑤+后置+异常 **全 ❌**。
  - **UC-CS-07 预设应答（✅ 渲染/匹配/应用/usageCount/必填校验/审计完整，⚠️ 分类树浏览缺独立方法）**：
    - `renderTemplate` @BizQuery `ErpCsCannedResponseBizModel.java:77-85`——断言 active + 解析系统变量（`{today}/{now}/{agent_name}/{ticket_id}/{customer_name}` 经 `IErpMdPartnerBiz`）→ 委托 `CannedResponseRenderer.render`。
    - `suggestForTicket` @BizQuery `ErpCsCannedResponseBizModel.java:89-126`——config-gated（`isCannedResponseEnabled` 默认 true）+ 三级宏匹配（exact[type+priority] → type-only → global fallback）+ 内存过滤（避开 isNull XMeta 限制）。
    - `applyCannedResponse` @BizMutation `ErpCsCannedResponseApplyCannedResponseProcessor.java:45-61`——渲染 + **usageCount+1**（`:53-55` 经 `daoProvider.daoFor(ErpCsCannedResponse).updateEntity`）+ TicketAction NOTE 审计（content=渲染后正文 `:57-58,127-137`）。✅ usageCount+1 + NOTE 记录渲染内容。
    - `CannedResponseRenderer.render`（纯函数）`CannedResponseRenderer.java:42-48`——mergeVars（custom 覆盖 system）→ `validateRequired`（抛 `ERR_CANNED_RESPONSE_REQUIRED_VAR_MISSING`）→ replacePlaceholders。✅ 必填变量缺失禁止。
    - **缺失**：`ErpCsCannedCategory` 实体（orm.xml:397-429，parentId 树）+ `ErpCsCannedCategoryBizModel` 存在，但**无独立"浏览分类树"方法**（`getCategoryTree`/`browseCatalog` grep 零）——选择经宏自动匹配（type+priority），非显式分类驱动浏览；标准 CRUD list 作兜底。→ UC-CS-07 ②"展开分类树"⚠️（宏匹配替代 + CRUD 兜底）。
  - **跨域 daoFor**：module-cs 生产代码零跨域 daoFor（同 A1.37/A1.38 基线，A2.14:320 复用）。跨域经 Facade：`IErpMdPartnerBiz`（`ErpCsCannedResponseBizModel:57`/`ErpCsTicketBizModel:62`）、`IErpSysNotificationBiz`（`ErpCsTicketBizModel:64`）。P1-MA1-022 不涉及 cs。

- **L4 测试证据现状**（`module-cs/erp-cs-service/src/test/java/`）：
  - UC-CS-05：`TestErpCsKnowledgeBaseSearch.java`（7 @Test，**强**）——`testSearchKnowledgeHitsPublishedExcludesUnpublished:57` / `testSearchKnowledgeCategoryFilter:80` / `testSearchKnowledgeLimitClamping:97`（limit=3/0→5/999→20）/ `testSuggestForTicketParsesSubject:144`（≤5）/ `testAdoptKnowledgeRecordsTicketAction:182`（断言 NOTE action + content 含 kbId）。**缺口**：KB 使用统计零测试 / 采纳→RESOLVED 零测试 / 无匹配建条目建议零测试。
  - UC-CS-06：**零测试**（功能不存在）。
  - UC-CS-07：`TestErpCsCannedResponseBiz.java`（7 @Test，**强**）——`testApplyCannedResponseIncrementsUsageAndWritesAction:179`（断言 usageCount 5→6 + NOTE action）/ inactive reject 错误码 / exact/type/global 匹配；`TestCannedResponseRenderer.java`（9 @Test，**强**）——`testRequiredVarMissingThrows:56`（错误码 + variableKey 参数）。**缺口**：分类树浏览零测试（功能为宏匹配替代）。

- **L5 既有证据（MA2 复用输入）**：
  - `docs/audits/2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`（A2.14）：**cs 域 verdict `✅(A2.14✅) zero P1 zero P0`**——范围 = ErpCsTicketBizModel（assign/start/resolve/close/reopen/cancel + SLA 方法）+ SlaDeadlineCalculator + ErpCsSlaScanJob；`:320` 确认 cs 生产代码无跨模块 daoFor；**:21-23 范围不含 KnowledgeBase/CannedResponse/质量升级 BizModel**——**A2.14 不覆盖 UC-CS-05/06/07**（知识/预设/质量升级超出 MA2 状态机范围）。
  - **cs 相关既有 finding**：`P2-MA2-067`（cs NEW>1h/ASSIGNED>2h 滞留升级 watch-only）、`P1-MA2-086`（cron 并发幂等 resolved R1.28）；RC 系列：`P1-RC-054`（A1.37 UC-CS-01 创建自动富化 6 项全缺 + `auto-assign-on-create` 死标志）、`P1-RC-055`（A1.37 UC-CS-11 计时器 session 全缺）、`P2-RC-051`（A1.37 UC-CS-02/03 拒绝路径+客户门控+7 天自动关闭）、`P1-RC-056`（A1.38 UC-CS-04 重复升级/L2-L3 结构性不可实现）、`P2-RC-052`（A1.38 UC-CS-04 延长 deadline 缺失）。**UC-CS-05/06/07 无既有 finding**（cs 域首批涉此三 UC 的 RC 切片）。
  - **本切片须声明与 MA2 报告差异增量**（报告段落 9）：A2.14 范围不含 UC-CS-05/06/07，无可复用行为证据；本切片为这些 UC 的**首份行为/需求视角证据**。

- **arm-index 既有 finding 衔接**：grep arm-index cs knowledge/knowledgebase/canned/escalat.*qual/NCR.*cs/adoptKnowledge/searchKnowledge/suggestForTicket/applyCannedResponse/UC-CS-0[567] → **零命中**。本切片新 finding 续全仓 RC 序列（当前最高 P1-RC-056 / P2-RC-052，本切片续编 **P1-RC-057+ / P2-RC-053+**，执行时 grep arm-index 取最新续编避免与同批 N=2/N=3 冲突）。本切片须 grep arm-index cs knowledge/canned/quality/escalat/NCR/non-conformance 同域同控制点后裁决。

- **保护区域**：本审计为**只读审计**。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按 §10。本切片候选偏差（UC-CS-06 质量跨域联动 / KB 使用统计 / 采纳转解决 / 分类树）属**代码逻辑**类（预授权——quality I*Biz 注入 + ErpCsTicket NCR 关联列若触及 ORM 须 ask-first；KB 统计列若加 ORM 须 ask-first；纯 BizModel/Processor 逻辑预授权）。

- **剩余差距**：A1.39 切片五级追踪审计报告缺失 = MA4 及 MR1 该切片证据缺口来源。本计划产出 A1.39 报告并登记 finding，解除 cs 域知识/质量/预设切片证据缺口。

## Goals

- 产出 A1.39 切片审计报告 `docs/audits/2026-08-06-0100-1-rc-ma1-a1-39-cs-f3-knowledge-quality-canned.md`，含方法论 §6 **9 段全部内容**。
- 对 UC-CS-05/06/07 逐条核验**每条验收标准**（完整枚举，§3）：UC-CS-05 实时解析/Top5/采纳 NOTE/采纳统计/采纳→RESOLVED/无匹配建条目建议；UC-CS-06 标记质量+缺陷信息/调 quality I*Biz 建 NCR/ESCALATE 审计关联/NCR 闭环结果可查/服务不可用后台重试；UC-CS-07 分类树/渲染系统变量/必填校验/usageCount+1/NOTE 渲染内容 全链逐条。
- 对候选缺口给出分级结论：①UC-CS-06 **全域结构性未实现**（无 quality I*Biz 注入 + 无 NCR 关联 + 无质量标记 mutation + 无重试，L1 全条无 Deferred 注记）倾向 **P1**（**§4 三判据关键裁决**——须核 `docs/design/customer-service/README.md`/质量相关 owner doc 是否显式标 UC-CS-06 Non-Goal 且经人工批准[i]plan-audit/[ii]AI 自标 ≠ 人工批准[methodology §4]/[iii]product-scope 裁剪；**use-cases.md:105-120 无 Deferred 注记**）；②UC-CS-05 **采纳统计/采纳→RESOLVED/无匹配建条目建议缺失**倾向 **P1/P2**（L1 后置⑤+异常明确要求；LIKE 非全文属 L1 注记 Deferred 不计）；③UC-CS-07 **分类树浏览缺独立方法**倾向 **P2/接受**（宏匹配替代 + ErpCsCannedCategory CRUD 兜底，行为可达成）；④UC-CS-05 搜索/采纳核心 + UC-CS-07 渲染/校验/usageCount → 倾向**接受**（强测）——按 §2 判据定级，若为 P0/P1 则新建 `P*-RC-xxx`（续编 P1-RC-057+ / P2-RC-053+）并按 §10 触发 MR1（本计划仅登记，不实施修复）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区；新 audit reports 表行）。

## Non-Goals

- **不修复 finding**（修复属 MR0/MR1；本计划是审计）。
- **不修改真相源**（§9 冻结条款——分歧记入报告，不直改 use-cases.md/README.md/product-scope.md）。
- **不修改代码/ORM/api.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.37 cs-F1 / A1.38 cs-F2 done；A1.40 cs-F4 独立 plan；本切片仅 UC-CS-05/06/07）。
- **不复审工单生命周期/SLA/调查/权益/目录/履行**（UC-CS-01/02/03/11 属 A1.37 / UC-CS-04 属 A1.38 / UC-CS-08/09/10/12 属 A1.40）。
- **不重审 P2-MA2-067 cs 滞留升级 / P1-MA2-086 cron 幂等 / P1-RC-054~056**（§去重协议：这些 finding 不同控制点，本切片引用不复审）。
- **不执行 MA4 运行时探针展开**（本计划只产出静态存疑点清单）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.39 工作项）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.39 UC 锚点）+ `docs/design/customer-service/use-cases.md`（L1 真相源）+ `docs/design/customer-service/README.md`（L2 设计参考，非真相源——UC-CS-06 若标 Non-Goal 须 §4 三判据复核）+ `docs/audits/arm-index.md`（finding 衔接）+ A2.14 报告（L5 既有证据，范围不含本切片 UC）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。其必需输入均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。若需即时行为确认可跑既有 JUnit（如 `mvn test -pl module-cs/erp-cs-service -Dtest=TestErpCsKnowledgeBaseSearch,TestErpCsCannedResponseBiz,TestCannedResponseRenderer`）。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论

Status: completed
Targets: `docs/audits/2026-08-06-0100-1-rc-ma1-a1-39-cs-f3-knowledge-quality-canned.md`（产出 §1-§5）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done

- [x] `Proof` 对 UC-CS-05/06/07 **逐验收标准一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:84/105/124` 验收标准原文（**含 UC-CS-05 `:101` LIKE-Deferred 注记逐字引用**）；L2 引用 `README.md`（标注"设计参考，冲突以 L1 为准"——UC-CS-06 若有 Non-Goal 标注须 §4 三判据复核）；L3 引用 `ErpCsKnowledgeBaseBizModel`/`ErpCsTicketBizModel#adoptKnowledge`/`ErpCsCannedResponseBizModel`/`ErpCsCannedResponseApplyCannedResponseProcessor`/`CannedResponseRenderer`/`ErpCsConfigs`/`ErpCsConstants`/`ErpCsKnowledgeBase`+`ErpCsCannedResponse`+`ErpCsCannedCategory` ORM（含行号）；L4 引用 `TestErpCsKnowledgeBaseSearch`#method + `TestErpCsCannedResponseBiz`#method + `TestCannedResponseRenderer`#method（注明断言强度；UC-CS-06 注明零测试）；L5 标注 A2.14 范围不含本切片 UC（无可复用行为证据，本切片为首份）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 重点核验**候选缺口**（逐条验收标准对照）：UC-CS-05 ①实时解析（suggestForTicket:84-98 ✅）②Top5（config 默认 5 ✅）③采纳 NOTE（adoptKnowledge:200-208 ✅）④**采纳统计**（ErpCsKnowledgeBase ORM 无计数列 ❌）⑤**采纳→RESOLVED**（adoptKnowledge 不转状态 ❌）⑥**无匹配→建条目建议**（无逻辑 ❌）+ LIKE-非-全文属 L1 Deferred（不计缺口）；UC-CS-06 ①标记质量+缺陷信息（无 mutation/列 ❌）②**调 quality I*Biz 建 NCR**（无注入 ❌）③ESCALATE 审计关联 NCR（ESCALATE 仅 SLA ❌）④NCR 闭环结果可查（无关联 ❌）⑤**服务不可用后台重试**（无 ❌）；UC-CS-07 ①分类树（ErpCsCannedCategory 实体+CRUD ⚠️ 无独立浏览方法/宏匹配替代）②渲染系统变量（renderTemplate ✅）③必填校验（validateRequired ✅）④usageCount+1（Processor:53-55 ✅）⑤NOTE 渲染内容（writeNoteAction:127-137 ✅）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` 按 §2 判据对 UC-CS-05/06/07 给出符合性结论（取最高）：UC-CS-06 → 全域结构性未实现倾向 **P1**（**§4 三判据关键裁决**：UC-CS-06 `use-cases.md:105-120` 全条无 Deferred 注记——核 README.md/质量 owner doc 是否标 Non-Goal：判据[i]plan-audit 通过记录 / 判据[ii]owner doc 显式 documented simplification 经**人工批准**痕迹（grep git log commit author，AI 自标 ≠ 人工批准 methodology §4 line 168）/ 判据[iii]product-scope 裁剪；三判据均不成立 → Q4=(a) 强制实现；UC-CS-06 跨域 quality I*Biz + NCR 关联属代码逻辑预授权，若加 ErpCsTicket NCR 关联列触及 ORM 须 ask-first）；UC-CS-05 采纳统计/采纳→RESOLVED/无匹配建条目建议缺失倾向 **P1/P2**（L1 后置⑤+异常明确要求，但核心搜索/采纳完整+强测，分级须 §4 三判据复核 README 是否显式标注）；UC-CS-07 分类树浏览倾向 **P2/接受**（宏匹配+CRUD 兜底，行为可达成）；UC-CS-05 搜索/采纳核心 + UC-CS-07 渲染/校验/usageCount → **接受**。每结论须列明命中判据编号 + 三源对照 + §4 三判据复核（**P1 项核 owner doc Non-Goal/Deferred 标注的人工批准痕迹**——判据[ii]关键：grep git log commit author）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] 报告 §1-§5 已落盘：UC-CS-05/06/07 矩阵行（逐验收标准进入 L5 判读），L1 逐字引用（含 UC-CS-05 LIKE-Deferred 注记）、L3 含行号、L4 注明断言强度（UC-CS-06 零测试）、L5 标注 A2.14 范围不含本切片
- [x] UC-CS-05/06/07 有符合性结论（P0/P1/P2/接受）且列明 §2 判据编号；候选缺口有明确分级；UC-CS-06 P1 裁决须含 owner doc Non-Goal 标注的人工批准痕迹核查结论；核心搜索/采纳/渲染接受结论成立

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: completed
Targets: `docs/audits/2026-08-06-0100-1-rc-ma1-a1-39-cs-f3-knowledge-quality-canned.md`（补 §6-§9）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成

- [x] `Decision` **复用 or 新增 裁决**（§7）：grep `arm-index.md` cs knowledge/knowledgebase/canned/escalat/quality/NCR/non-conformance/adopt/search/suggest/render/usageCount 同域同控制点后裁决——UC-CS-06 全域缺失为**新根因**（既有 arm-index 无 RC finding 涉 cs 质量事件跨域联动需求契约维度）→ 新建 `P1-RC-057`（UC-CS-06 全域结构性未实现，续 A1.38 P1-RC-056）；UC-CS-05 采纳统计/采纳→RESOLVED/无匹配建条目建议为**新根因** → 新建 `P1-RC-058`（续 A1.38 P2-RC-052，§4 三判据复核后定 P1）；UC-CS-07 分类树浏览若裁决 P2 → 新建 `P2-RC-053`；若裁决接受则不新建。执行时 grep arm-index 取最新续编号避免与同批 N=2/N=3 冲突。禁止未经比对新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR1）+ **UC-CS-06 跨域联动修复须协调 quality 域 IErpQaNonConformanceBiz 注入 + NCR 关联 + 后台重试**（若加 ErpCsTicket NCR 列触及 ORM 须 ask-first + 独立 plan-audit）。
      - Skill: none
- [x] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记 L5 无法静态定论、需运行时确认的点（SP-1 searchKnowledge LIKE 在大文章量下的时延/相关性质量是否触发 use-cases.md:101 Deferred 触发条件 / SP-2 suggestForTicket 取首 token 对多词 subject 的命中召回率 / SP-3 applyCannedResponse usageCount 并发递增是否乐观锁保护 / SP-4 ErpCsCannedCategory CRUD 是否实际驱动前端分类树选择；每存疑点一行）。**P0 即时通道评估**（UC-CS-06 全缺是否破坏活跃数据/会计正确性——倾向否：质量联动缺失影响可追溯性不破坏数据；不触发 MR0）。
      - Skill: none
- [x] `Proof` 报告 §8 过程纪律自检段：实际运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 退出码 0 为门控通过依据**（无生产代码变更，注明"无回归风险"）。
      - Skill: none
- [x] `Add` 报告 §9 与 MA2 报告差异增量声明：A2.14（`:21-23`）范围不含 UC-CS-05/06/07（KnowledgeBase/CannedResponse/质量升级 BizModel），**无可复用行为证据**；本切片为这些 UC 的首份行为/需求视角证据，列明只补的需求视角差异（UC-CS-06 全缺 / UC-CS-05 统计+采纳转解决+建条目建议缺 / UC-CS-07 分类树缺独立方法）。
      - Skill: none
- [x] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 `P1-RC-057`（+ P1-RC-058 / P2-RC-053）入 RC 发现追踪分区；audit reports 表新增 A1.39 行。
      - Skill: none
- [x] `Proof` 报告 9 段完整性自检：落盘前自查 §1-§9 全部存在。
      - Skill: none

Exit Criteria:

- [x] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据
- [x] 新 RC finding 已写入 `arm-index.md`；静态存疑点清单已登记（SP-1~SP-5 供 A4.1/A4.2 展开）
- [x] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_02ec440ecffeJ4a1akopSWMVZq，fresh session，未起草本计划）。范围/依赖/方法论/反 slack/模板/保护区域全 PASS；load-bearing 引用经实仓复核 CONFIRMED TRUE：①A1.39 UC-CS-05/06/07 锚点 ✅；②UC-CS-06 全域未实现（grep module-cs NonConformance/ErpQa/qualityIssue 业务零命中，仅 orm.xml:22 愿望注释）✅；③ErpCsKnowledgeBase ORM 无 usageCount/adoptCount 列（orm.xml:362-394）+ adoptKnowledge:200-208 不转 RESOLVED ✅；④applyCannedResponse usageCount+1（Processor:53-55）+ CannedResponseRenderer.validateRequired 抛错误码 ✅；⑤A2.14:21-23 范围不含 UC-CS-05/06/07 ✅；⑥RC 编号最高 P1-RC-056/P2-RC-052，续编 057/053 ✅；⑦README.md:97-99 Non-Goal 仅 3 项（SLA working-days/L1 升级/SLA pause），UC-CS-06 在 README.md:49/state-machine.md:86 为**活跃设计契约非 Non-Goal**——支撑 P1 裁决路径。共识达成，转 active。（非阻塞修正：TestErpCsCannedResponseBiz @Test 计数 8→7；个别测试方法行号 ±1-2 偏移在 A1.x 模式容差内。）

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 报告 9 段完整性 + 五级矩阵逐验收标准覆盖 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A1.39 报告 9 段齐全 + UC-CS-05/06/07 矩阵行（逐验收标准）+ finding 登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.39 锚点一致
- [x] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（独立结束审计 ses_02eade3ffffeS2DH7xbtNfFWYM 于 2026-08-06 执行：报告 9 段完整性/§4 三判据/finding 去重/真相源冻结全 PASS，仅闭合纪律 fixable items 已修复后 re-pass）
- [x] 结束证据存在于文件中（独立子代理 ses_02eade3ffffeS2DH7xbtNfFWYM closure audit 结果记录于本节 + 报告 §8 独立性声明）

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按 §10 经 MR0（P0）/ MR1（R1.0 展开 RC-R1.n，P1）实施；本切片候选偏差（UC-CS-06 质量跨域联动 / KB 统计 / 采纳转解决 / 分类树）属**代码逻辑**类（预授权——quality I*Biz 注入 + BizModel/Processor 逻辑）；**UC-CS-06 若加 ErpCsTicket NCR 关联列 / KB 统计列 须 ask-first + 独立 plan-audit**。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行；UC-CS-06 跨域联动修复须与 quality 域 NCR 创建路径[UC-QA-05 NCR-CAPA，A1.32 done]协同）

## Closure

Status Note: 已完成 Phase 1 + Phase 2 全部 [x] 项。A1.39 审计报告 9 段齐全落盘于 `docs/audits/2026-08-06-0100-1-rc-ma1-a1-39-cs-f3-knowledge-quality-canned.md`，3 UC（UC-CS-05/06/07）逐验收标准五级追踪完成；3 新 finding（P1-RC-057 UC-CS-06 全域结构性未实现 + P1-RC-058 UC-CS-05 采纳统计/采纳转解决/无匹配建议三项合并缺失 + P2-RC-053 UC-CS-07 分类树浏览 watch-only）登记入 `docs/audits/arm-index.md` RC 发现追踪分区 + audit reports 表新增 A1.39 行 + RC 交叉引用注记段（line 222）；零 P0。本计划为只读审计无代码/ORM/api.xml/view.xml/真相源变更故跳过 mvn test/build/lint 门控（Closure Gates 顶部已声明）。独立结束审计由 fresh session 子代理（ses_02eade3ffffeS2DH7xbtNfFWYM）执行 PASS。

Closure Audit Evidence:

- Auditor / Agent: Independent closure auditor — subagent session `ses_02eade3ffffeS2DH7xbtNfFWYM`（fresh session, 2026-08-06, 未起草/执行本计划）
- Evidence: 报告落盘于 `docs/audits/2026-08-06-0100-1-rc-ma1-a1-39-cs-f3-knowledge-quality-canned.md`（9 段齐全 §1-§9 + 段落完整性自检 [x] 全勾 + §8 过程纪律自检 actual vs baseline 实测表）；`docs/audits/arm-index.md` 新增 audit reports 表行（line 106）+ 3 新 finding 入 RC 发现追踪分区（P1-RC-057 line 218 / P1-RC-058 line 219 / P2-RC-053 line 220）+ A1.39 RC 交叉引用注记段（line 222）。
- Evidence: 独立结束审计 PASS（fresh session）。9 sections §1-§9 齐全（§5 五级矩阵 UC-CS-05 9 验收标准 + UC-CS-06 9 验收标准 + UC-CS-07 11 验收标准完整枚举逐条结论）。3 新 finding + 1 reuse 注记（A2.14:21-23 cs 范围不含本切片 UC + A2.14:320 cs 跨域 Facade 注入现状）登记入 arm-index 双向可追溯。Load-bearing claims 经实仓独立复核 CONFIRMED TRUE：(a) grep `NonConformance|ErpQa|qualityIssue|isQuality|ncrId|non_conformance|markQuality|flagQuality|defectDescription|materialLot|batchInfo|IErpQaNonConformanceBiz|app.erp.qa` 跨 `module-cs/erp-cs-service/src/main` 零业务命中（仅 `app-erp-cs.orm.xml:22` 头注释愿望性文档无代码）；(b) `ErpCsKnowledgeBase` ORM `app-erp-cs.orm.xml:362-394` 无 usageCount/viewCount/adoptCount 列（grep 跨 `module-cs/model/app-erp-cs.orm.xml` 仅 `ErpCsCannedResponse.usageCount:447` 命中，KB 实体零命中）；(c) `ErpCsTicketBizModel.adoptKnowledge:200-208` 仅写 NOTE 不修改 ticket.status 不转 RESOLVED；(d) grep `getCategoryTree|browseCatalog|loadCategoryTree|treeOfCategories|categoryTree` 跨 main 零命中（UC-CS-07 分类树浏览经宏匹配+CRUD 兜底）；(e) owner doc 反向支撑 P1-RC-057：`README.md §跨域协作:49` + `state-machine.md §外部依赖:86` 是活跃设计契约承诺非 Non-Goal（Non-Goal 列表 `README.md:97-101` 仅 3 项 SLA working-days/L1 升级/SLA pause，UC-CS-06 不在其中）；(f) §4 三判据复核三 finding 均不成立 documented simplification → Q4=(a) 强制实现（P1）或 Q4=(a) 张力声明（P2-RC-053）。Finding 编号顺序无冲突（P1-RC-057/058 续 P1-RC-056；P2-RC-053 续 P2-RC-052）。与方法论 §1-§10 + §去重协议 + §4 三判据一致；与 rc-requirement-baseline-inventory A1.39 锚点（UC-CS-05/06/07）一致。无范围内项目降级 deferred/follow-up（Deferred But Adjudicated 仅 finding 修复实施，按 §10 正确归类为 out-of-scope improvement）。文本一致性 PASS（Plan Status=completed、Phase 1/2 Status=completed、Exit Criteria [x] 全勾、Closure Gates [x] 全勾）。

Follow-up:

- MR1 按 P1-RC-057 / P1-RC-058 / P2-RC-053 展开修复行（UC-CS-06 跨域联动修复须协调 quality 域 `IErpQaNonConformanceBiz` 注入 + ESCALATE 字段借用冲突须考虑独立 actionType 或 fromStatus/toStatus+content 区分 + 与 R1.28[P1-MA2-086] `hasEscalationAction` 幂等守卫交互须复核；UC-CS-05 采纳统计/采纳转解决/无匹配建议修复[P1-RC-058] + UC-CS-07 分类树浏览修复[P2-RC-053 watch-only]）；触及 ORM 结构变更[ErpCsTicket/ErpCsTicketAction 加 NCR 关联列 + ErpCsKnowledgeBase 加 usageCount 列 + 可能新增 NCR 重试 Job 实体]须 ask-first + 独立 plan-audit（§5 ORM 类）。
- MA4（A4.1/A4.2）按报告 §7 静态存疑点 SP-1~SP-5 展开运行时探针（SP-1 searchKnowledge LIKE 在大文章量下的时延/相关性质量是否触发 use-cases.md:101 Deferred 触发条件 / SP-2 suggestForTicket 取首 token 对多词 subject 的命中召回率 / SP-3 applyCannedResponse usageCount 并发递增是否经乐观锁保护 / SP-4 ErpCsCannedCategory CRUD 是否实际驱动前端分类树选择 / SP-5 UC-CS-06 修复时 ESCALATE 字段借用冲突在 SLA+质量双路径下与 R1.28 幂等守卫的交互行为）。
