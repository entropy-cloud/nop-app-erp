# 2026-07-08-0056-2-cs-knowledge-base-search-suggestion CS 知识库搜索与建议（UC-CS-05）

> Plan Status: active
> Mission: erp
> Work Item: CS 知识库搜索与建议（UC-CS-05）
> Last Reviewed: 2026-07-08（迭代 2 修订后）
> Source: `docs/design/customer-service/use-cases.md` UC-CS-05（知识库搜索与建议）；Non-Goal 后继路由见 `docs/plans/2026-07-07-1430-1-cs-entitlement-service-catalog.md`（本期不涉及知识库）、`docs/plans/2026-07-04-0700-2-cs-ticket-sla-csat.md`（知识库关键词推荐独立结果面）；审计命名缺口 `docs/audits/2026-07-06-use-case-implementation-audit.md:198`「`ErpCsKnowledgeBaseBizModel` 仅 CRUD，全文搜索服务未实现」
> Related: `docs/plans/2026-07-08-0056-1-extended-domains-posted-businessdate-std-fields.md`（同批 N=1，本计划独立可先行）
> Audit: required

## Current Baseline

实时仓库逐项核实（`read`/`grep`，非采信旧记忆）：

- **实体已就绪**：`module-cs/model/app-erp-cs.orm.xml:362` `ErpCsKnowledgeBase`（知识库）实体已物化——`code`(orderCode,UK)/`title`(VARCHAR 200,mandatory)/`content`(content domain)/`categoryId`(→ErpCsCannedCategory)/`isPublished`(BOOLEAN default false)/`remark` + 标准 gid/审计列 + UK_CS_KNOWLEDGE_BASE_CODE + IDX_CS_KNOWLEDGE_BASE_CATEGORY_ID。**无 posted/businessDate**（与本批 N=1 计划无关，cs 域知识库为内容实体非过账单据）。
- **BizModel 为空壳 CRUD**：`module-cs/erp-cs-service/.../entity/ErpCsKnowledgeBaseBizModel.java` 仅 `extends CrudBizModel<ErpCsKnowledgeBase>`，零自定义方法——无搜索/建议服务。`IErpCsKnowledgeBaseBiz` 接口为 codegen 生成空壳。
- **工单侧入口存在**：`ErpCsTicketBizModel`（`module-cs/erp-cs-service/.../entity/ErpCsTicketBizModel.java`）已落地 0700-2 六态状态机 + SLA；`ErpCsTicketAction` 审计实体（`actionType` 字典 `erp-cs/action-type`）已就绪，可登记采纳事件。
- **设计已收敛**：
  - `use-cases.md` UC-CS-05：触发=创建/编辑工单输入 subject；流程=解析关键词→**全文搜索** `erp_cs_knowledge_base`(isPublished=true)→按相关性排序→Top 5 展示（标题+摘要）→采纳标记已参考→采纳文章解决则工单 RESOLVED；后置=采纳计入使用统计；异常=无匹配提示创建新条目。**注**：需求源（:92）用词为「全文搜索」，本计划**有意降标**为 `LIKE` 关键词匹配（对齐 ui-patterns.md :245 既有口径），全文引擎归 Deferred（见 Non-Goals/Deferred 触发条件）。
  - `ui-patterns.md §知识库嵌入工单创建`(:240-252)：实时搜索（`title LIKE '%主题关键词%'`）→ Top 5 推荐文章 → 采纳/忽略。§知识库管理(:193)：文章标题搜索支持全文检索（title + content）。
  - `README.md §ErpCsKnowledgeBase`(:109)：知识库/FAQ 可选深化项；§知识库建议(:126) 创建工单时按 subject 关键词搜索推荐。**注**：该节列有 `orgId`/`tags` 等字段，但 orm.xml 实体实际无此列（既有 owner-doc 漂移），Phase 3 一并勘误。
- **审计缺口**：`2026-07-06-use-case-implementation-audit.md:198` 明确「全文搜索服务未实现」。
- **action-type 字典约束（关键）**：`erp-cs/action-type` 字典权威定义在 `module-cs/model/app-erp-cs.orm.xml:64-71`（6 值 ASSIGN/NOTE/ATTACH/ESCALATE/CLOSE/CANCEL），meta 层 `action-type.dict.yaml` 为 `__XGEN_FORCE_OVERRIDE__` 生成不可手改。**新增字典值须改 orm.xml（ask-first 保护区域）**。既定先例 `ErpCsConstants.java:26-28`（0700-2）：字典缺码时**复用 `NOTE`（最接近的通用码）**而非新增，避免 ORM 编辑。本计划采纳此先例——采纳事件登记 `actionType=NOTE`，remark/扩展字段记 `knowledgeBaseId` 引用，不新增字典值，保持零 ORM。
- **触发条件裁决**：UC-CS-05 此前仅作 Non-Goal 路由（1430-1/0700-2），无正式 Deferred 触发条件。本计划**正式裁定触发条件已满足**：(1) 实体 + CRUD 壳已就绪（无 ORM 阻塞）；(2) 设计文档（use-cases/ui-patterns/README）完整收敛；(3) 审计明确命名缺口；(4) 项目处于「业务逻辑深化」阶段（AGENTS.md），CS 域业务逻辑补齐属既定方向。

剩余差距：(1) 知识库无关键词搜索 `@BizQuery`；(2) 工单创建无实时建议挂接；(3) UC-CS-05 采纳后置条件（使用统计）缺持久化列（ORM——本计划裁定为 Non-Goal，采纳改经 TicketAction 审计登记）。

## Goals

- 在 `ErpCsKnowledgeBaseBizModel` 落地 `searchKnowledge(keyword, categoryId?, limit?)` `@BizQuery`——对已发布文章（`isPublished=true`）按 `title` + `content` 关键词匹配（`LIKE`，对齐 ui-patterns.md :245/:233 既有口径，非全文引擎），按相关性/命中段排序，返回 Top N（config-gated 上限）。
- 落地 `suggestForTicket(subject, limit?)` `@BizQuery`——工单主题驱动的便捷封装：解析 subject 关键词 → 复用 `searchKnowledge` 返回 Top 5（对齐 UC-CS-05/§嵌入工单创建）。
- 在工单创建/编辑 AMIS 表单挂接实时建议（输入 subject 时调 `suggestForTicket`，表单下方展示 Top 5 推荐文章，点击查看/采纳）。
- 采纳登记经既有 `ErpCsTicketAction` 审计（`actionType` 复用既有 `NOTE` 字典值——对齐 0700-2 `ErpCsConstants.java:26-28` 先例，不新增字典值/不触 ORM），`remark`/扩展字段记 `knowledgeBaseId` 引用，不引入新持久化列。

## Non-Goals

- **不**接入全文搜索引擎（Elasticsearch/Lucene/DB FULLTEXT 索引）——本期用 `LIKE` 关键词匹配（对齐 ui-patterns.md 既有口径 `title LIKE '%关键词%'`）；全文引擎归后继（触发条件=文章量超万级或相关性质量不满足时）。
- **不**新增 `viewCount`/`adoptCount`/`useCount` 列（ORM 保护区域）实现使用统计持久化——采纳改经 `ErpCsTicketAction` 审计登记；统计聚合后继（触发条件=需运营分析知识库命中率时）。
- **不**实现「采纳文章解决则工单自动 RESOLVED」——属工单状态机既有 resolve 动作的人工辅助决策，非自动迁移（避免误关闭工单）；前端仅提供「采纳并参考」入口。
- **不**做预设应答变量渲染（UC-CS-07 `${customer.name}` 占位符引擎，独立结果面）。
- **不**做工单解决后自动推送创建知识库条目（异常分支，独立结果面）。
- **不**触及 `module-cs/model/*.orm.xml`（纯服务层 + view，零 ORM 变更）。

## Task Route

- Type: `implementation-only change`（CS 域业务逻辑补齐：自定义 `@BizQuery` + AMIS 表单挂接；零 ORM/契约变更）
- Owner Docs: `docs/design/customer-service/use-cases.md` UC-CS-05、`docs/design/customer-service/ui-patterns.md` §知识库嵌入工单创建/§知识库管理、`docs/design/customer-service/README.md` §ErpCsKnowledgeBase
- Skill Selection Basis: `nop-backend-dev`（自定义 `@BizQuery` 动作、`QueryBean`/`findList` 关键词查询、ErrorCode、config 键）；`nop-frontend-dev`（AMIS 表单 input→service→suggest 列表挂接）。两技能匹配工作方法；既有 CRUD/状态机范式已由 0700-2/1430-1 验证。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline
- 无 ORM 变更，故无 ask-first 保护区域门控；纯服务层 + view 改动

## Execution Plan

### Phase 1 - 知识库搜索/建议后端服务

Status: planned
Targets: `module-cs/erp-cs-service/.../entity/ErpCsKnowledgeBaseBizModel.java`、`IErpCsKnowledgeBaseBiz`、`ErpCsErrors`、`ErpCsConstants`/`ErpCsConfigs`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: 无

- [ ] `Decision`：关键词匹配口径——对齐 ui-patterns.md :245 既有 `title LIKE '%关键词%'`，扩展为 `title OR content LIKE`（§知识库管理 :233「title + content」）；相关性排序用「title 命中优先 > content 命中 > 创建时间倒序」的简单确定性规则（非 TF-IDF/全文评分，归 Non-Goal 全文引擎）。记录选择与残留风险（LIKE 前缀通配无法走索引，文章量大时性能下降→全文引擎后继触发条件）。
      - Skill: `nop-backend-dev`
- [ ] `Add`：`ErpCsConstants`/`ErpCsConfigs` 增 config 键 `erp-cs.knowledge-search-default-limit`（默认 5）+ `knowledge-search-max-limit`（默认 20，防滥用）；`ErpCsErrors` 增 `ERR_KNOWLEDGE_SEARCH_KEYWORD_TOO_LONG`/`ERR_KNOWLEDGE_SEARCH_LIMIT_EXCEEDED`（镜像域内 ErrorCode 范式，不跨域 import）。
      - Skill: `nop-backend-dev`
- [ ] `Add`：`searchKnowledge(@BizQuery String keyword, @BizQuery String categoryId, @BizQuery Integer limit)` —— 构造 `QueryBean` 过滤 `isPublished=true` + `(title LIKE %keyword% OR content LIKE %keyword%)` + 可选 `categoryId` 等值；`limit` 缺省取 config 默认、钳制 max；返回 `title`+`content` 摘要（截断）+ `id`+`code`；经 `dao().findList`。
      - Skill: `nop-backend-dev`
- [ ] `Add`：`suggestForTicket(@BizQuery String subject, @BizQuery Integer limit)` —— subject 空白/过短守门（< 2 字符返回空列表，不报错）；解析关键词（按空格/标点切分取首个有效词或整体 LIKE）；复用 `searchKnowledge` 逻辑返回 Top 5（对齐 UC-CS-05）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] `searchKnowledge`/`suggestForTicket` 经 GraphQL 可调用，对已发布文章按关键词命中返回结果（未发布文章 excluded）；`isPublished=false` 文章不出现在结果（单元测试断言）。
- [ ] `limit` 缺省/钳制 config 生效；空/过短 keyword 守门不报错返回空集。

### Phase 2 - 工单创建/编辑表单实时建议挂接（AMIS）

Status: planned
Targets: `module-cs/erp-cs-web/...` 工单 view.xml（create/edit 表单 subject 字段）
Skill: `nop-frontend-dev`

- Item Types: `Add`
- Prereqs: Phase 1 后端 `@BizQuery` 可用

- [ ] `Add`：工单 create/edit 表单 `subject` 字段挂接 `suggestForTicket`——经 AMIS `service`（`api: /api/GenericApi` GraphQL `ErpCsKnowledgeBase__suggestForTicket`，`watch` subject 变化 + debounce）+ 下方 `list`/`cards` 展示 Top 5 推荐文章（标题 + 摘要 + 查看链接）；对齐 `ui-patterns.md §知识库嵌入工单创建` ASCII 布局。
      - Skill: `nop-frontend-dev`
- [ ] `Add`：采纳入口——「采纳并参考」按钮调工单既有 `ErpCsTicketBizModel` 记 `TicketAction`（`actionType` 复用既有 `NOTE` 字典值，对齐 0700-2 先例，**不新增字典值**），`remark`/扩展字段登记 `knowledgeBaseId` 引用（无新列）。
      - Skill: `nop-backend-dev | nop-frontend-dev`

Exit Criteria:

- [ ] 工单创建表单输入 subject 时，下方实时展示匹配的已发布知识库文章 Top 5（对齐设计 §嵌入工单创建）；无匹配时不阻断表单提交。
- [ ] 采纳登记写入 TicketAction 审计 `actionType=NOTE` + `knowledgeBaseId` 引用（`grep`/测试验证：无新字典值、无新持久化列引入）。

### Phase 3 - 测试与 owner-doc 对齐

Status: planned
Targets: `module-cs/erp-cs-service/src/test/...`、`docs/design/customer-service/README.md`、`docs/design/customer-service/use-cases.md`
Skill: `nop-testing`

- Item Types: `Proof | Add`
- Prereqs: Phase 1/2 落地

- [ ] `Proof`：`JunitAutoTestCase` + GraphQL `request.json5` 覆盖——`searchKnowledge` 命中已发布/排除未发布/`categoryId` 过滤/`limit` 钳制/空关键词守门；`suggestForTicket` subject 解析/Top 5；采纳登记 TicketAction `actionType=NOTE`。指定测试策略与命令 `mvn test -pl module-cs -am`。
      - Skill: `nop-testing`
- [ ] `Add`：`docs/design/customer-service/README.md` §ErpCsKnowledgeBase——(a) 标注搜索/建议已实现（从「可选深化项」更新为已落地能力，附 `@BizQuery` 方法名）；(b) 勘误既有漂移（删除实体不存在的 `orgId`/`tags` 字段描述，对齐 orm.xml 实际列 id/code/title/content/categoryId/isPublished/remark）。`use-cases.md` UC-CS-05 末尾标注「实现：LIKE 关键词匹配（全文引擎 Deferred）」。`ui-patterns.md` 无需改动（实现即对齐其 ASCII 布局）。
      - Skill: none

Exit Criteria:

- [ ] `mvn test -pl module-cs -am` 新增测试 0 failures/0 errors，既有 CS 测试无回归。
- [ ] owner doc 标注与实现一致（无样板填充；仅改 §ErpCsKnowledgeBase/UC-CS-05 状态描述）。

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (ses_0c274836effePAN1izrKjY3dfd) because 1 BLOCKER + 4 NOTES：
  - B1：计划自相矛盾 Non-Goal「零 ORM 变更」——`erp-cs/action-type` 字典权威定义在 `module-cs/model/app-erp-cs.orm.xml:64-71`（meta 层 `action-type.dict.yaml` 为 `__XGEN_FORCE_OVERRIDE__` 生成不可手改），新增 `SUGGESTION_ADOPTED` 字典值须改 orm.xml（ask-first）。更：采纳先例 0700-2 `ErpCsConstants.java:26-28`（字典缺码时复用 `NOTE`），已清除全部 `SUGGESTION_ADOPTED` 引用（Goals/Phase 2/Deferred），采纳登记 `actionType=NOTE` + `knowledgeBaseId` 引用，保持零 ORM 诚实。
  - N1：UC-CS-05 :92 用词「全文搜索」非 LIKE——baseline 已补注需求源用词 + 本计划有意降标为 LIKE（全文引擎 Deferred），不再将 LIKE 呈现为需求原意。
  - N2：条件性「若需/若既有约定要求」致退出标准不可判定——Phase 2 Targets 已删 `XMeta（若需）`；Phase 3 owner-doc 项改为确定式（README §ErpCsKnowledgeBase 勘误 + UC-CS-05 标注 LIKE 降标）。
  - N3：hook 目标澄清——`suggestForTicket` 落 `ErpCsKnowledgeBaseBizModel`（AMIS 直接调），`ErpCsTicketBizModel` 仅作采纳审计 sink（baseline 已补 `ErpCsTicketAction` 就绪说明）。
  - N4：README §ErpCsKnowledgeBase 既有漂移（列 `orgId`/`tags` 实体无此列）——Phase 3 owner-doc 项已加 (b) 勘误子项。
  - 正面确认（无需变更）：触发条件裁决合法（透明声明 UC-CS-05 此前仅 Non-Goal 路由 + 4 条可验证证据，非臆造范围）；单结果面成立（backend @BizQuery + AMIS 挂接共享 UC-CS-05 单一行为契约，规则 4/14）；LIKE-vs-fulltext Deferred 触发条件（文章量超万级）诚实；命名/header/技能标注/item types/Decision 理由全合规。
- Independent draft review iteration 2: `accept` (ses_0c26a01cfffesP8f7KW4B1Yyw9) — B1 完全解决（`SUGGESTION_ADOPTED` 活动范围 0 命中——仅 Draft Review Record 历史档留存 1 处，依规则 12 须保留为证据；`actionType=NOTE` 复用一致 :21/31/89/95/106/152；action-type 字典确认在 orm.xml:64-71 故 NOTE 复用为正确零 ORM 路径；ErpCsConstants.java:26-28 先例确认）。N1~N4 全部解决。无新增 BLOCKER。本计划非 ORM、plan-first、审查者可用性=subagent，草案审查已收敛 → `Plan Status: active`。

## Closure Gates

> 本计划为纯服务层 + view（零 ORM/契约变更），结束前运行一次完整仓库验证。

- [ ] 范围内行为完成（搜索/建议后端 + 工单表单挂接 + 采纳审计登记）
- [ ] 相关文档对齐（`README.md` §ErpCsKnowledgeBase + use-cases.md UC-CS-05 状态）
- [ ] 已运行验证：`mvn clean install -DskipTests`（154 模块）+ `mvn test -pl module-cs -am`（CS 域无回归）+ 工单 view.xml well-formed
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 全文搜索引擎接入（Elasticsearch/DB FULLTEXT）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本期用 `LIKE` 关键词匹配（对齐 ui-patterns.md 既有口径），满足 UC-CS-05 基本流程与 Top 5 推荐价值。LIKE 前缀通配（`%keyword%`）无法走 B-Tree 索引，文章量增大时性能下降；全文引擎引入额外基础设施（ES 集群或 DB FULLTEXT 索引 + ORM 变更）。
- Successor Required: `yes`
- Trigger Condition: 知识库文章量超万级，或 LIKE 搜索时延/相关性质量不满足运营要求时。

### 知识库使用统计持久化（viewCount/adoptCount）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: UC-CS-05 后置「采纳计入使用统计」经 `ErpCsTicketAction` 审计登记（`actionType=NOTE` + `knowledgeBaseId` 引用）即可追溯采纳事件；`ErpCsKnowledgeBase` 增 viewCount/adoptCount 列属 ORM 保护区域（ask-first），且统计聚合可由 TicketAction 后处理计算，无需物化计数器列。
- Successor Required: `yes`
- Trigger Condition: 需运营分析知识库命中率/热门文章排行（物化计数器或定期聚合表）时，经独立 ORM ask-first 计划承接。

### 采纳文章自动 RESOLVED 工单

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 自动迁移工单至 RESOLVED 存在误关闭风险（采纳≠已解决），属工单状态机既有 resolve 的人工辅助决策；前端仅提供「采纳并参考」入口，resolve 仍经既有 0700-2 resolve 动作人工确认。
- Successor Required: `no`

## Closure

Status Note: <待执行后填写>

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计填写>
- Evidence: <待填写>

Follow-up:

- <仅非阻塞跟进项；已确认缺陷不得出现于此>
