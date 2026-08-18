# 2026-08-18-1849-1-rc-mr1-r1-68-69-cs-quality-escalation-knowledge-adoption RC-R1.68+69 — cs 质量事件联动 + 知识库采纳族（B 类预授权：TicketAction 双弱指针载体 + actionType dict 追加）

> Plan Status: completed
> Last Reviewed: 2026-08-18
> Mission: requirement-compliance
> Work Item: RC-R1.68（P1-RC-057，UC-CS-06 工单升级为质量事件全域结构性未实现）+ RC-R1.69（P1-RC-058，UC-CS-05 ⑦采纳转解决 + ⑧采纳统计 + ⑨无匹配建议）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.68/RC-R1.69 行 + `docs/audits/arm-index.md` P1-RC-057 行（:233）/ P1-RC-058 行（:234）+ 2026-08-12 批量裁决 B 类（roadmap 头 :48-49：「RC-R1.68（actionType dict + TicketAction 记录 NCR，A4.2.137 自述不触 ORM）」「RC-R1.69（usageCount 可从 TicketAction 派生，非结构必需）」——降级为预授权自动执行，不再须 ask-first checkbox；行标签仍携旧「越界项」措辞，done 回写时按 R1.61-67 先例同步改写）
> Related: `docs/design/customer-service/use-cases.md`（L1 UC-CS-05 :90-99 / UC-CS-06 :101-120）；`docs/design/customer-service/README.md`（§跨域协作 :49 质量问题升级承诺）；`docs/design/customer-service/state-machine.md`（§外部依赖 :86 质量升级到 NCR 承诺）；`docs/plans/2026-08-17-2125-3-rc-mr1-r1-67-cs-multi-level-escalation.md`（Deferred「ESCALATE 与质量升级 actionType 区分」successor=yes → 本计划承接）；`docs/plans/2026-08-14-2304-2-rc-mr1-r1-26-qa-spc-evaluate-wiring.md`（quality 域 SPC 级联创建 NCR 先例）；`docs/plans/2026-08-15-0320-3-rc-mr1-r1-31-mnt-report-additional-fault.md`（跨域 requestBiz.save data map 建单先例）；`docs/plans/2026-08-17-2125-1-rc-mr1-r1-65-cs-ticket-create-enrichment.md`（cs→crm dao compile 依赖 + mock 先例）
> Audit: required

## Current Baseline

- **finding P1-RC-057（arm-index:233，UC-CS-06 ③④⑤⑥⑦⑧⑨）**：L1（`use-cases.md:101-120`）逐字要求：流程① 处理人标记"质量问题"填写缺陷描述/物料/批次信息 → ② 系统调 quality 域 I*Biz 接口创建 ErpQaNonConformance → ③ 工单操作日志记录 ESCALATE 操作（关联 NCR 编号）→ ④ 工单继续原有流程（RESOLVED→CLOSED），NCR 流程独立 → ⑤ NCR 闭环后工单可查看到 NCR 处理结果；后置：工单与 NCR 关联跨域可追溯；异常：quality 域服务不可用 → 延迟创建 NCR，工单先保留状态，后台自动重试。L3 实仓（HEAD 核查）：
  - **cs 侧零质量代码**：grep `NonConformance|ErpQa|ncrId|IErpQaNonConformanceBiz|app.erp.qa` 跨 `module-cs/erp-cs-service/src/main` 零业务命中（仅 `app-erp-cs.orm.xml:22` 头注释愿望性文档）；`ErpCsTicketBizModel`（622 行）12+ 方法清单无一涉质量；`ErpCsTicket`/`ErpCsTicketAction` ORM 无 NCR 关联列；`module-cs/erp-cs-service/pom.xml` 无 erp-qa-dao 依赖（现有 compile：cs-dao/cs-meta/notify-dao/master-data-dao/crm-dao[R1.65]）。
  - **actionType dict 既有 6 值**（`app-erp-cs.orm.xml:64-71` + `erp-cs-meta/_vfs/dict/erp-cs/action-type.dict.yaml`）：ASSIGN/NOTE/ATTACH/ESCALATE/CLOSE/CANCEL——无质量语义值；`ACTION_TYPE_ESCALATE`（`ErpCsConstants:37`）当前仅 SLA 路径语义。
  - **R1.67 协调前提已就绪**：R1.67 已**删除** `hasEscalationAction` 反查守卫（升级幂等改由计数器/时间窗判定），质量路径新增独立 actionType 不再与 SLA 升级守卫互扰（R1.67 Deferred 协调项 successor=yes → 本计划承接落地）。
- **finding P1-RC-058（arm-index:234，UC-CS-05 ⑦⑧⑨）**：L1（`use-cases.md:95-99`）逐字要求：流程⑤「如采纳的文章解决了问题，工单直接标记为 RESOLVED」+ 后置「采纳记录计入知识库使用统计」+ 异常「无匹配文章 → 提示客服创建新知识库条目（工单解决后自动推送建议）」。L3 实仓：
  - ⑦ `ErpCsTicketBizModel.adoptKnowledge:316-326` 仅写 `ACTION_TYPE_NOTE` 审计行（content `"采纳知识库文章参考: knowledgeBaseId=" + id`，:323-324），**不修改 ticket.status**（resolve 仅经 `ErpCsTicketResolveProcessor` 独立 mutation :256-262）；
  - ⑧ `ErpCsKnowledgeBase`（orm.xml:372-404）**无 usageCount/viewCount/adoptCount 列**（grep 跨 cs orm 仅 `ErpCsCannedResponse.usageCount:457` 命中），adoptKnowledge 不调任何计数更新——字段载体不存在；
  - ⑨ grep `suggestCreate|建议创建|knowledgeSuggest|noMatchSuggestion` 跨 main 零命中——空集返回后无"提示建条目"逻辑 + 无"工单解决后自动推送建议"逻辑；
  - `searchKnowledge:35-80` / `suggestForTicket:82-98`（`ErpCsKnowledgeBaseBizModel`）主路径完整强测（TestErpCsKnowledgeBaseSearch 7 @Test，含 `testAdoptKnowledgeRecordsTicketAction:183` 断言 NOTE——**须随 actionType 变更同步改造**）。
- **quality 域跨域面（就绪，无需新建）**：`IErpQaNonConformanceBiz`（`module-quality/erp-qa-dao/.../IErpQaNonConformanceBiz.java`）`extends ICrudBiz<ErpQaNonConformance>`（save data map 可用，R1.31 requestBiz.save 先例）+ 自定义 mutation（submitReview/resolve/cancel 等）；`ErpQaNonConformance`（`app-erp-quality.orm.xml:351-442`）关键字段：`sourceType`（propId 4，VARCHAR 50 **无 dict 约束自由值**）/`sourceCode`（propId 5，VARCHAR 50）——**反向关联载体已存在**；`materialId`（propId 6 **mandatory**）/`ncrDate`（propId 3 **mandatory DATE**）/`severity`（propId 10 **mandatory** dict erp-qa/severity：LOW/NORMAL/HIGH/CRITICAL）/`status`（propId 12 mandatory dict erp-qa/ncr-status：OPEN/IN_REVIEW/RESOLVED/ESCALATED_TO_RECALL/CANCELLED）/`quantity`/`supplierId`/`description`（VARCHAR 2000）；quality 域 SPC 级联创建 NCR 先例 = `SpcOutOfControlHandler.createNcrAndAction`（R1.26 审计引述 :116-123）——NCR code 生成方式执行期按此先例核对取用。
- **notify 基础设施（cs 侧范式就绪）**：`@Inject IErpSysNotificationBiz notificationBiz` + setter 模式（ErpCsTicketBizModel:69-70/:219 等五处）；seed 模板三方言 `module-notify/deploy/sql/{mysql,oracle,postgresql}/_seed_erp-notify.sql` 当前最大 ID **7203**（:131 cs.ticket-assign-no-match）→ 本计划新模板 **7204**。
- **Q4 判据**：两 finding 均 §2 P1①/P1② + 三判据复核均不成立（arm-index:233/:234）→ Q4=(a) 强制实现。**2026-08-12 B 类批量裁决**（roadmap 头 :48-49，用户批准）：两项均降级预授权自动执行——**NCR 关联/采纳统计载体 = actionType dict 追加值 + TicketAction 记录/派生，不触 ORM 结构变更**（dict 追加 = 数据变更非结构变更，R1.88「dict 加值是数据非结构变更」裁决先例）；越界回落双独立子 agent 批准。
- **测试基线**：erp-cs-service **144 @Test 全绿**（R1.67 后，surefire reuseForks=false 每类独立 JVM）；`app-erp-all/src/main/resources/_vfs/nop/job/conf/` 现存 **26 个 .job.yaml**（另 1 个 scheduler.yaml 非注册文件），TestErpAllJobYamlLoading 断言 **26**。
- **compliance 基线**（`docs/audits/compliance-baseline.md` §BASELINE 机器可读块）：R2b=235 / R2c=1439 / R2d=35 / R10=11 / R12a=70（设计取向：跨域经 IBiz 注入 + 审计写入经既有 `IErpCsTicketActionBiz`/`writeAction` 通道，预期零新增 daoFor 面；若执行期出现新增站点则 baseline-raise + per-site 证据）。

## Goals

- **UC-CS-06 质量事件联动运行时成立**：`escalateToQuality` @BizMutation（IN_PROCESS 守卫 + materialId/缺陷描述必填 + 批次/数量/严重度/供应商可选）→ 经 `IErpQaNonConformanceBiz.save` 创建 NCR（sourceType=CS_TICKET + sourceCode=ticket.code 反向关联）→ 写独立 `QUALITY_ESCALATE` 审计行（content 携带 NCR 编号）→ 工单保持原状态机继续（L1 ④）。
- **L1 ⑤ NCR 闭环可查**：`findQualityNcrs(ticketId)` @BizQuery 经 qaNcrBiz 反查（sourceType+sourceCode）投影 NCR 状态/闭环结果。
- **L1 异常后台重试**：quality 调用失败 → 写 `PENDING:` 前缀审计行（工单状态保持）+ 新增简单 job bean `ErpCsQualityEscalationRetryJob`（R1.37 范式：cron 空值跳过 + limit + 逐条失败隔离）扫描重试，成功后原行 content 修正为 NCR 编号；重试上限 config（默认 3）。
- **UC-CS-05 ⑦⑧⑨运行时成立**：⑦ `adoptKnowledge` 增 `autoResolve` 可选参（true → 委托 resolveProcessor 转 RESOLVED）；⑧ 采纳统计 = 新 dict 值 `ADOPT_KNOWLEDGE` 审计行 + `knowledgeUsageStats` @BizQuery 派生计数（零 ORM 列，B 类裁决载体）；⑨ resolve 后置 config-gated 推送「建议创建知识库条目」notify（模板 seed 7204）+ searchKnowledge 空集前端提示注记。
- **R1.67 Deferred 协调项闭合**：独立 actionType `QUALITY_ESCALATE` 落地（与 SLA `ESCALATE` 语义解耦；R1.67 已删 hasEscalationAction 无互扰）。
- **测试补强**：新测试组（质量联动 + 采纳族）+ 既有 `testAdoptKnowledgeRecordsTicketAction` 改造 + 144 基线零回归 + 全量构建 + checker 零漂移（或 baseline-raise per-site 证据）+ TestErpAllJobYamlLoading 26→27。
- **owner doc 收敛**：README.md §跨域协作 + state-machine.md §外部依赖 补实现注记（承诺→已实现）+ use-cases.md 附录实现注记（L1 正文不动）+ arm-index P1-RC-057/P1-RC-058 → done + roadmap RC-R1.68/RC-R1.69 行 done + 行标签 B 类改写 + logs 条目。

## Non-Goals

- **不改 quality 域**（IErpQaNonConformanceBiz 既有面消费，零 quality 侧代码/ORM 变更）。
- **不加 ErpCsTicket/ErpCsTicketAction/ErpCsKnowledgeBase ORM 列**（B 类裁决明确载体边界；usageCount 物化列 = successor）。
- **不实现外部邮件/门户实际投递**（IN_APP 占位语义为仓内既定范式，nop-notification 独立面 successor）。
- **不做质检单（inspection）联动**（UC-CS-06 只要求 NCR）。
- **不做 KB 全文搜索引擎升级**（P2 维度既有 Deferred 维持）。
- **不做采纳统计报表页面**（@BizQuery 暴露 + owner doc 注记即满足"统计"后置；AMIS 报表页 successor）。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/design/customer-service/use-cases.md`（L1 真相源，正文不动）+ `docs/design/customer-service/README.md` + `docs/design/customer-service/state-machine.md`
- Skill Selection Basis: BizModel/per-mutation Processor/跨域 IBiz 注入（`nop-backend-dev`：@BizMutation 事务/IBiz 注入/ErrorCode 范式）；测试（`nop-testing`：JunitAutoTestCase + mock IBiz + `_cases/` 快照）；dict 追加与 seed 模板无匹配 skill（平台文档范式按 Related 计划先例）。

## Infrastructure And Config Prereqs

- 新 config 键（`ErpCsConfigs` 登记 + owner doc 配置表）：`erp-cs.quality-escalation-enabled`（默认 true，总门控）+ `erp-cs.quality-retry-cron`（默认空 = 跳过，job 运行时门控）+ `erp-cs.quality-retry-max`（默认 3）+ `erp-cs.knowledge-suggest-on-resolve`（默认 true）。
- 新 job.yaml：`app-erp-all/src/main/resources/_vfs/nop/job/conf/erp-cs-quality-retry.job.yaml`（enabled 默认 false + cronExpr `@cfg:nop.job.erp-cs-quality-retry.cron-expr|0 0/10 * * * ?`）——TestErpAllJobYamlLoading 计数 26→27。
- 新 seed 模板：`cs.knowledge-suggest-create`（ID **7204**，USER_LIST `${handlerUserId}`，module-notify 三方言顺延）。
- pom：`module-cs/erp-cs-service/pom.xml` 增 `app-erp-qa-dao` compile 依赖（镜像 R1.65 crm-dao 先例）；测试经 mock `IErpQaNonConformanceBiz`（`TestMockCrmBizModels` 先例扩容），不引入 qa-service 全量闭包。
- 无数据库迁移回滚需求（零 ORM 结构变更；dict 追加值为模型数据变更，`mvn clean install -DskipTests` 增量重生成）。

## Execution Plan

### Phase 1 - RC-R1.68 质量事件联动（escalateToQuality + NCR 弱指针 + 后台重试）

Status: completed
Targets: `module-cs/erp-cs-service/src/main/java/app/erp/cs/service/entity/ErpCsTicketBizModel.java`、`module-cs/erp-cs-service/src/main/java/app/erp/cs/service/processor/ErpCsTicketEscalateToQualityProcessor.java`（新）、`module-cs/erp-cs-service/src/main/java/app/erp/cs/service/job/ErpCsQualityEscalationRetryJob.java`（新）、`module-cs/erp-cs-service/src/main/java/app/erp/cs/service/ErpCsConstants.java`、`ErpCsConfigs.java`、`module-cs/model/app-erp-cs.orm.xml`（仅 dict 追加）、`module-cs/erp-cs-meta/src/main/resources/_vfs/dict/erp-cs/action-type.dict.yaml`、`app-erp-all/src/main/resources/_vfs/nop/job/conf/erp-cs-quality-retry.job.yaml`（新）、`module-cs/erp-cs-service/pom.xml`、`module-cs/erp-cs-web`（AMIS 工单行操作按钮）
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: 无（R1.67 已删 hasEscalationAction，无互扰前提）

- [x] **D1 NCR 关联载体裁决（双弱指针，零 ORM）**：反向 = NCR 侧 `sourceType="CS_TICKET"` + `sourceCode=ticket.code`（qa orm propId 4/5 既有自由 VARCHAR）；正向 = `ErpCsTicketAction.actionType=QUALITY_ESCALATE` + content 携带 NCR code。否决 ErpCsTicket/TicketAction 加列（B 类裁决明确不触 ORM）；残留风险 = ①弱指针无 FK 强约束 ②ticket UK=(code, orgId) 而 NCR 无 org 维度——跨组织同 code 工单理论上可交叉匹配（单组织部署无影响，owner doc 注记）。
      - Skill: `nop-backend-dev`
- [x] **D2 独立 actionType = QUALITY_ESCALATE**：dict `erp-cs/action-type` 追加值（orm dict + meta dict yaml 双处，数据变更非结构变更）+ `ErpCsConstants.ACTION_TYPE_QUALITY_ESCALATE`——落实 R1.67 Deferred 协调项。
      - Skill: `nop-backend-dev`
- [x] **D3 escalateToQuality 契约**：`@BizMutation ErpCsTicket escalateToQuality(ticketId, materialId*, defectDescription*, batchInfo?, quantity?, severity?=NORMAL, supplierId?, ctx)` + per-mutation Processor（守卫链：config 门控 → 工单存在 → `assertCan` status==IN_PROCESS（L1 前置）→ 参数非空）；NCR data map：`ncrDate=today`、`sourceType=CS_TICKET`、`sourceCode=ticket.code`、`materialId`、`description=defectDescription`（batchInfo 非空追加「批次：{batchInfo}」）、`quantity`、`severity`、`supplierId`、`status=OPEN`——经 `IErpQaNonConformanceBiz.save(data, ctx)`（R1.31 save-map 先例）；NCR code 显式构造 `NCR-CS-{ticket.code}`（镜像 quality 域 SPC 级联先例 `SpcOutOfControlHandler:101` `NCR-SPC-{chart}-{subgroup}` 显式 setCode 模式）。成功 → 写 QUALITY_ESCALATE 审计行 content=`NCR:{code}` + 工单**不改状态**（L1 ④ 独立进行）。（执行注记：materialId/defectDescription 接口声明 @Optional 使域错误码成为拒绝点——守卫链仍在 Processor，防纵深保持）
      - Skill: `nop-backend-dev`
- [x] **D4 服务不可用后台重试**：quality 调用 catch（NopException/RuntimeException，不 rethrow → 外层 @BizMutation 正常提交，无需 REQUIRES_NEW）→ 写 QUALITY_ESCALATE 审计行 content=`PENDING:{defectDescription摘要}`（重试队列载体）+ 工单保持 IN_PROCESS；新 `ErpCsQualityEscalationRetryJob`（R1.37 简单 job 范式：无参 execute + cron 空值跳过 + limit 200 + 逐条 try/catch 隔离 + `ormTemplate.runInSession` 包裹）扫描 PENDING 行 → 单事务内重试创建，且**创建前先反查既有 NCR**（sourceType=CS_TICKET + sourceCode=ticket.code，防 crash-间隙重复创建）→ 成功将原行 content 修正为 `NCR:{code}`；重试次数载体 = PENDING 行 content 尾部 `#retry={n}` 计数，超 `erp-cs.quality-retry-max`（默认 3）跳过并 LOG.warn（L1 无超限通知要求）。（执行注记：PENDING 载荷为自足 JSON 全参数，非仅描述摘要——重试可完整重建 NCR）
      - Skill: `nop-backend-dev`
- [x] **D5 NCR 闭环可查**：`@BizQuery List<Map> findQualityNcrs(ticketId, ctx)` 经 qaNcrBiz.findList（eq sourceType=CS_TICKET + eq sourceCode=ticket.code）投影 `{code, status, severity, ncrDate, resolvedAt, resolution}`（L1 ⑤）。
      - Skill: `nop-backend-dev`
- [x] pom `app-erp-qa-dao` compile + `@Inject IErpQaNonConformanceBiz qaNcrBiz`（非 private + setter）+ per-mutation Processor beans.xml 注册（R1.21/R1.33 先例；BizModel Facade 委托）。（执行注记：实际 artifactId = `app-erp-quality-dao`[module-quality/erp-qa-dao 目录]，plan 简写路径修正；Job bean 同步注册 app-service.beans.xml）
      - Skill: `nop-backend-dev`
- [x] AMIS 最小接线：工单 rowActions 增「质量问题升级」按钮（dialog 收集 materialId/defectDescription/batchInfo/quantity/severity，调 `ErpCsTicket__escalateToQuality`），对齐 R1.44 D4 最小前端范式。（落地 = `escalateQuality` 参数表单 + row-escalate-quality-button dialog[visibleOn IN_PROGRESS] + simple 提交页 withFormData，对齐 ErpFinAccountingPeriod reverseClose 范式）
      - Skill: `nop-frontend-dev`
- [x] **Proof**：新 `TestErpCsQualityEscalation`（IoC + mock IErpQaNonConformanceBiz）：① IN_PROCESS 成功（NCR data map 全字段断言 + QUALITY_ESCALATE 审计 + 工单状态不变）② 非 IN_PROCESS 拒绝 + 错误码 + 零 NCR 零审计 ③ materialId/defectDescription 缺失拒绝 ④ mock 抛异常 → PENDING 审计行 + 工单保持 ⑤ 重试 job 成功 PENDING→`NCR:{code}` 修正 ⑥ 重试超限跳过（retry 计数封顶）⑦ cron 空值跳过 ⑧ findQualityNcrs 闭环投影 ⑨ GraphQL `ErpCsTicket__escalateToQuality` RPC 冒烟（真实引擎，qaNcrBiz mock 注册）+ `_cases/` 快照。验证命令：`mvn test -pl module-cs/erp-cs-service`。（mock = `TestMockQaBizModels` + `app-test-mock-qa.beans.xml`[镜像 R1.65 crm mock 范式]；`_cases/` = 空 autotest.yaml 标记——R1.65 断言式先例；⑤ 附幂等二次运行不重复创建断言；① 附 QUALITY_ESCALATE 与 SLA ESCALATE 语义隔离断言）
      - Skill: `nop-testing`

Exit Criteria:

- [x] escalateToQuality 全链运行时成立（成功/失败/重试三路径测试绿；QUALITY_ESCALATE 与 SLA ESCALATE 审计语义隔离断言绿）
- [x] TestErpAllJobYamlLoading 26→27 通过

### Phase 2 - RC-R1.69 知识库采纳族（autoResolve + 派生统计 + 无匹配建议）

Status: completed
Targets: `ErpCsTicketBizModel.java`（adoptKnowledge）、`ErpCsTicketResolveProcessor.java`（suggest 后置）、`ErpCsKnowledgeBaseBizModel.java`（usageStats）、`ErpCsConstants.java`/`ErpCsConfigs.java`、action-type dict 追加、`module-notify/deploy/sql/{mysql,oracle,postgresql}/_seed_erp-notify.sql`、`module-cs/erp-cs-web`（AMIS adopt autoResolve 选项 + 空态提示文案）
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1 dict 追加模式已定（同一 dict 文件二次追加）

- [x] **D6 ⑦采纳转解决**：`adoptKnowledge(ticketId, knowledgeBaseId, autoResolve?=false, ctx)`——`autoResolve=true` → 委托 `resolveProcessor.resolve(ticketId, "采纳知识库文章解决: knowledgeBaseId={id}")`（状态机守卫/RESOLVED 审计/survey 触发链复用既有 resolve 路径）；adopt 审计行保留先行写入。
      - Skill: `nop-backend-dev`
- [x] **D7 ⑧采纳统计载体（B 类裁决 = TicketAction 派生）**：adoptKnowledge 审计行 actionType NOTE → 新 dict 值 `ADOPT_KNOWLEDGE`（dict 追加 + `ACTION_TYPE_ADOPT_KNOWLEDGE` 常量），content 固定整串格式 `knowledgeBaseId={id}`（无其他内容，统计查询用 **eq 精确匹配**杜绝 `id=1` 前缀碰撞 `id=12` 计数错误）；`@BizQuery List<Map> knowledgeUsageStats(knowledgeBaseId?, ctx)` 经 `IErpCsTicketActionBiz` 注入查询 actionType=ADOPT_KNOWLEDGE + content eq 计数（单条或全量 group）；遗留 NOTE 旧格式行不计入（owner doc 注记边界）。
      - Skill: `nop-backend-dev`
- [x] **D8 ⑨无匹配建议**：`ErpCsTicketResolveProcessor.resolve` 增 config-gated（`erp-cs.knowledge-suggest-on-resolve` 默认 true）protected step `suggestKnowledgeCreation`：工单 resolve 时若无 ADOPT_KNOWLEDGE 审计行 → `notificationBiz.notify("cs.knowledge-suggest-create", {ticketCode, handlerUserId=assignedToId 回退 operatorId}, ctx)`（try/catch LOG.warn 降级范式）+ seed 模板 7204（USER_LIST `${handlerUserId}`，三方言）。
      - Skill: `nop-backend-dev`
- [x] AMIS 最小接线（Phase 2 侧）：① `ErpCsTicket.view.xml` 既有 adopt 按钮（:164-167/:237-240 调 `ErpCsTicket__adoptKnowledge`）dialog 增 `autoResolve` 勾选项（⑦ 运行时前端可达）；② 工单知识库推荐面板 searchKnowledge 空结果空态文案「未匹配到知识库文章，建议解决后创建新条目」（⑨ 提示客服，arm-index P1-RC-058 修复纲要 ⑤ 项义务）。（落地 = adopt 按钮 ajax→dialog[adoptKnowledge simple 页 + autoResolve checkbox 表单，add/edit 双表单两处]；空态 = list placeholder 三元表达式按 subject 已输区分提示）
      - Skill: `nop-frontend-dev`
- [x] **Proof**：新 `TestErpCsKnowledgeAdoption`：① autoResolve=true → RESOLVED + resolve 审计 + survey 触发链（config on）② autoResolve=false 仅审计（既有行为回归）③ ADOPT_KNOWLEDGE 审计格式断言 + knowledgeUsageStats 单条/全量计数 ④ resolve 无采纳 → notify 落库（recipient=handler）⑤ config 关闭跳过 ⑥ 既有 `testAdoptKnowledgeRecordsTicketAction` 改造（NOTE→ADOPT_KNOWLEDGE）+ TestErpCsKnowledgeBaseSearch 其余回归。验证命令：`mvn test -pl module-cs/erp-cs-service`。（⑤ 落地为双路径：resolve 有采纳 → 不推送[语义=无匹配才推送] + config off → 跳过；既有测试快照 output/tables CSV 同步 NOTE→ADOPT_KNOWLEDGE 改造）
      - Skill: `nop-testing`

Exit Criteria:

- [x] UC-CS-05 ⑦⑧⑨ 三路径测试绿（采纳转解决 / 派生计数 / 建议推送）

### Phase 3 - 验证收口 + 文档回填

Status: completed
Targets: `docs/design/customer-service/README.md`、`docs/design/customer-service/state-machine.md`、`docs/design/customer-service/use-cases.md`（附录注记）、`docs/audits/arm-index.md`、`docs/backlog/requirement-compliance-roadmap.md`、`docs/logs/2026/{当期}.md`
Skill: none

- Item Types: `Proof | Add`
- Prereqs: Phase 1-2 全绿

- [x] 全量验证：`mvn test -pl module-cs/erp-cs-service` 全绿（144 基线 + 新增零回归）+ `mvn clean install -DskipTests` BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh`（actual ≤ baseline 或 baseline-raise per-site 证据落 compliance-baseline.md）+ TestErpAllJobYamlLoading 27/27。（实测：**159/0/0**（144 基线 + 9 质量联动 + 6 采纳族）+ 全仓 156 模块 BUILD SUCCESS + checker 全 19 规则 ≤ 基线**零漂移零 baseline-raise**[R2b=235/R2c=1438≤1439/R2d=35/R10=11/R12a=70/R12b=66/R12c=40] + TestErpAllJobYamlLoading 1/0/0（27/27））
      - Skill: none
- [x] owner doc 回填：README §跨域协作 + state-machine §外部依赖「质量问题升级」承诺 → 已实现注记（弱指针载体 + D1-D5 裁决 + 残留风险）；use-cases.md UC-CS-05/06 附录实现注记（L1 正文不动）；arm-index P1-RC-057/P1-RC-058 → done (RC-R1.68/69)；roadmap 两行 done + 行标签按 B 类裁决改写（对齐 R1.61-67 先例）；logs 条目（全绿验证状态）。（另含 README §关键业务规则 4 知识库采纳族注记 + 规则 6 actionType 8 值 + §配置点 4 新键登记）
      - Skill: none

Exit Criteria:

- [x] 六处回填一致（代码 / arm-index / roadmap / owner docs / logs / use-cases 注记）

## Draft Review Record

- Independent draft review iteration 1: needs-revision（task `ses_feb7c79b7ffecY3Cne51PGAnsv`，2026-08-18）——MAJOR：① job.yaml 基线计数 off-by-one（27→实际 26 + scheduler.yaml 误计，TestErpAllJobYamlLoading 断言 26）② Phase 2 缺 AMIS 接线条目（⑦ adopt autoResolve 前端不可达 + ⑨ 空态提示缺 edit 项，违反 arm-index 修复纲要 ⑤）③ D7 统计谓词前缀碰撞（like `id=1%` 误配 `id=12`）+ 笔误 ADOPT_KNOWLEDPT；MINOR：D4 重试原子性/防重、D1 跨组织同 code 残留、beans 注册措辞、D3 code 机制应显式点名（SpcOutOfControlHandler:101）、路径漂移 3 处。
- Independent draft review iteration 2: needs-revision（task `ses_feb725de1ffeyqW4EIj5g5y6oe`，2026-08-18）——M2/M3/全部 MINOR 确认解决；M1 残留两处陈旧计数（Goals :35「27→28」与 Phase 3 :127「28/28」未随基线修正）。
- Independent draft review iteration 3: acceptable（task `ses_feb6e34cfffeZMjKsE8GVvVhHA`，2026-08-18）——残留计数两处确认修复，五处计数位全一致，无新问题。共识达成，Plan Status → active。

## Closure Gates

- [x] 范围内行为完成（UC-CS-06 流程①-⑤+后置+异常；UC-CS-05 ⑦⑧⑨）
- [x] 相关文档对齐（README/state-machine/use-cases 注记/arm-index/roadmap/logs）
- [x] 已运行验证（`mvn test -pl module-cs/erp-cs-service` 全绿 + 全仓 `mvn clean install -DskipTests` + checker 零漂移或 baseline-raise 登记）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（MISSION_DRIVER:2026-08-17-212541-mission-driver 独立结束审计会话，非执行者上下文）
- [x] 结束证据存在于文件中（见下方 Closure Audit Evidence）

## Deferred But Adjudicated

### usageCount 物化列（ErpCsKnowledgeBase 加列）

- Classification: `optimization candidate`
- Why Not Blocking Closure: B 类批量裁决明确「usageCount 可从 TicketAction 派生，非结构必需」；派生计数满足 L1「计入使用统计」后置
- Successor Required: `yes`（计数查询性能不满足或需 KB 列表排序展示时，按 A 类纯加性加列立项）

### NCR 强关联（FK/独立关联实体）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: B 类裁决载体 = 弱指针双写（sourceType/sourceCode + TicketAction content）；跨域可追溯后置经双向查询达成
- Successor Required: `no`

### 外部邮件/门户实际投递

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: IN_APP 占位语义为仓内既定范式（csat.md/sla.md 既有声明）
- Successor Required: `yes`（nop-notification 独立面接入时）

## Closure

Status Note: 实现完成且独立结束审计通过（Phase 1-3 全部 completed；审计会话独立复核实时仓库并重跑验证命令：erp-cs-service **159/0/0** + TestErpAllJobYamlLoading **1/0/0**[27/27 job.yaml]；Phase 1-2 全部代码工件[Processor/RetryJob/dict 双值/seed 7204 三方言/pom/AMIS 按钮+autoResolve+空态文案]与 Phase 3 六处文档回填[README/state-machine/use-cases/arm-index :233/:234 done/roadmap :460-:461 done/logs 08-18]逐项 grep/read 实证一致；Deferred 三项均为预裁定载体决策非范围内缺陷）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（MISSION_DRIVER:2026-08-17-212541-mission-driver closure-audit 会话，2026-08-18，非执行会话）
- Evidence: ① 实时仓库逐项核验：`ErpCsTicketBizModel.escalateToQuality:351/findQualityNcrs:367/adoptKnowledge autoResolve:328-336` + `ErpCsTicketEscalateToQualityProcessor` + `ErpCsQualityEscalationRetryJob`（cron 空值跳过 :73）+ dict `action-type.dict.yaml` 8 值含 QUALITY_ESCALATE/ADOPT_KNOWLEDGE + `ErpCsConstants:47/:59-76` 常量与 4 config 键（ErpCsConfigs 消费）+ `erp-cs-quality-retry.job.yaml`（job/conf 27 个 .job.yaml）+ pom `app-erp-quality-dao:69` + seed 7204 三方言各 1 处 + `ErpCsKnowledgeBaseBizModel.knowledgeUsageStats:120` + `ErpCsTicketResolveProcessor.suggestKnowledgeCreation:84/:95` + AMIS `escalateQuality` 表单/dialog/提交页（:272/:358/:387）+ autoResolve checkbox（:313-319）+ 空态文案 3 处（:1525/:2835/:3828）+ mock `TestMockQaBizModels`/`app-test-mock-qa.beans.xml` + 测试类 9+6 @Test；② 文档六处：README :53/:67-68/:90-93、state-machine :94、use-cases :101/:103/:124、arm-index :233（done RC-R1.68）/:234（done RC-R1.69）、roadmap :460-:461（done ✅）、logs/2026/08-18.md :5-:11；③ 审计会话重跑真实验证命令：`mvn test -pl module-cs/erp-cs-service` → **Tests run: 159, Failures: 0, Errors: 0, Skipped: 0** + `mvn test -pl app-erp-all -Dtest=TestErpAllJobYamlLoading` → **1/0/0 BUILD SUCCESS**；④ 反空心检查：新代码全部有运行时消费者（Processor/Job 经 app-service.beans.xml 注册、AMIS 按钮可达、dict 值经常量消费）；⑤ 轻微注记（非缺陷）：config 键定义于 ErpCsConstants.CONFIG_*（ErpCsConfigs 消费，与既有 SLA config 范式一致），plan Infrastructure 节「ErpCsConfigs 登记」措辞与实际 Constants/Configs 分工存在无害漂移。

Follow-up:

- 无（范围内零遗留预期）

Post-Closure Correction（2026-08-19，mission VERIFY 会话）:

- 结束审计后 mission VERIFY 发现 `ErpCsTicket.view.xml` 重复追加损坏（4196 行 vs HEAD 335 行，~22 份残缺拷贝 + 首拷贝 add 表单 kbSuggestion 两属性截断），致 app-erp-all 4 页面校验测试红——结束审计验证范围（cs-service + TestErpAllJobYamlLoading + build）不含页面校验组，形成盲区；Evidence 中「空态文案 3 处（:1525/:2835/:3828）」实为对损坏拷贝的 grep 命中（预期语义 add/edit 2 处）。已重建为单一预期结构（功能面不变：escalateQuality/adoptKnowledge 表单 + row 按钮 + simple 页 + adopt dialog + 空态三元文案 add/edit 两处 + minRows 引号规范化，diff vs HEAD 96+/26−），重验全仓 `mvn clean install -DskipTests` + 全仓 `mvn test` 全绿 + checker 零漂移。详见 `docs/bugs/2026-08-19-cs-ticket-view-xml-append-corruption.md` 与 `docs/logs/2026/08-19.md`。
