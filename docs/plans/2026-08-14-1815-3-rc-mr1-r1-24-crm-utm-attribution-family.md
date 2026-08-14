# 2026-08-14-1815-3-rc-mr1-r1-24-crm-utm-attribution-family RC-R1.24 — crm UTM 归因族：copy-on-create + 归因报表（MR1 第一批纯预授权）

> Plan Status: active
> Last Reviewed: 2026-08-14
> Mission: requirement-compliance
> Work Item: RC-R1.24（P1-RC-037 UTM copy-on-create 派生缺失 + P1-RC-038 归因报表完全缺失，同根因[UTM 归因链路]同控制点[campaign 归因数据流]协同）— 同域同 owner doc（`docs/design/crm/` + `use-cases.md`）同结果表面（UTM 归因契约符合性），按计划指南规则 14 合并为一个 owner plan 的两个阶段
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.24 行 + `docs/audits/arm-index.md` P1-RC-037/P1-RC-038 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`（G5 crm UTM 归因族）
> Related: `docs/design/crm/use-cases.md`（L1 UC-CRM-07）；`docs/design/crm/marketing.md`（§二 UTM 追踪与归因）；`docs/design/crm/README.md`（§ErpCrmCampaign §ErpCrmLead）；`docs/audits/2026-08-07-2345-rc-ma4-a4-2-83-96-crm-lead-lifecycle-marketing-forecast-runtime.md`（A4.2.90/91 运行时证据）；`docs/plans/2026-08-14-1815-1-rc-mr1-r1-21-22-crm-conversion-family.md`（同批范式参照）
> Audit: required

## Current Baseline

- **finding P1-RC-037（arm-index 行，UC-CRM-07 UTM copy-on-create 派生缺失）**：L1（`use-cases.md:142-143`）逐字「lead.utmMedium → 复制 campaign.medium（若未显式传入）；lead.utmSource → 复制 campaign.source（若未显式传入）」。L3 实仓：`ErpCrmLeadBizModel.defaultPrepareSave:196-223` 仅 duplicate-check（`:199`）+ territory 分配（`:201-223`），**无 campaign.medium→lead.utmMedium / campaign.source→lead.utmSource 复制分支**；grep `setUtmMedium|setUtmSource` 跨 erp-crm-service/src/main = 0 业务命中（A4.2.90 运行时证实）；`ErpCrmLeadBizModel` imports 仅注入 `IErpCrmStageBiz`（`ErpCrmLeadBizModel.java:4` import / `:53-54` @Inject），**不注入 IErpCrmCampaignBiz**。ORM 列现存：`ErpCrmLead.campaignId`（orm.xml:209 propId=22）+ `utmMedium`（:210 propId=23）+ `utmSource`（:211 propId=24）+ to-one `campaign`（:235）。§2 P1①（功能完全缺失）+ §2 P1⑤（测试断言完全缺失）。**非 P0**（字段持久化 NULL 仅影响营销归因报表精度，不破坏 GL/库存）。
- **finding P1-RC-038（arm-index 行，UC-CRM-07 归因报表完全缺失）**：L1（`use-cases.md:144-148`）逐字「营销活动归因报表：SELECT campaign.name, count(lead.id), sum(lead.expectedRevenue) FROM ErpCrmLead lead JOIN ErpCrmCampaign campaign GROUP BY campaign.id」。L3 实仓：`ErpCrmCampaignBizModel.java:11-19` = 19 行空 CrudBizModel stub（无业务方法）；`ErpCrmReportBizModel.prepareDataset:151-164` switch 仅 `lead-conversion-funnel`（:155-157）+ `forecast-accuracy`（:158-160）两 case，**无 campaign-attribution case**；glob `**/report/crm/*.xpt.xml` = 仅 2 文件（lead-conversion-funnel.xpt.xml + forecast-accuracy.xpt.xml），**无 campaign-attribution.xpt.xml 模板**。§2 P1①（功能完全缺失）+ §2 P1⑤（测试断言完全缺失）。**非 P0**（campaignId 数据完整但无聚合视图消费，影响营销 ROI 决策）。
- **实仓（HEAD 核查）**：
  - `ErpCrmLeadBizModel.defaultPrepareSave:196-223`——**P1-RC-037 修复点**：新建（`lead.getId() == null`）且 campaignId 非空时，读 `ErpCrmCampaign.medium`（orm.xml:426 propId=6）/`source`（:427 propId=7）复制到 lead.utmMedium/utmSource——**「若未显式传入」语义**：entityData 中 utmMedium/utmSource 为 null（未显式传入）才复制；显式传入（非 null）不覆盖（**Decision 项**：显式传入判定载体——entityData 字段 null 判定 vs 前端传参区分，执行时核实 CrudBizModel defaultPrepareSave 的 entity 字段 null 语义）。
  - `ErpCrmCampaign` ORM：`name`（:424 propId=3）+ `medium`（:426 propId=6）+ `source`（:427 propId=7）字段现存；`ErpCrmCampaignBizModel` = 19 行 stub（**零业务方法**，P1-RC-038 报表无需改 CampaignBizModel——报表数据源走 ErpCrmReportBizModel）。
  - `ErpCrmReportBizModel.java`——**P1-RC-038 修复点**：`prepareDataset:151-164` 增 `campaign-attribution` case + 新 `buildCampaignAttributionDataset()`（镜像 `buildLeadConversionFunnelDataset:194-229` 范式：ormTemplate DB 级 GROUP BY campaignId + COUNT + SUM(expectedRevenue)，经 to-one campaign 解析 campaign.name）+ `@BizQuery campaignAttributionData` 暴露原始数据（镜像 `leadConversionFunnelData:176-178`）；新 `campaign-attribution.xpt.xml`（镜像 lead-conversion-funnel.xpt.xml 表结构：campaignName/leadCount/expectedRevenue 三列 + 合计行）。
  - **报表接线现状**：`erp-crm.action-auth.xml:158-170` crm-report 资源下已注册 lead-conversion-funnel（orderNo=10010）+ forecast-accuracy（orderNo=10020）两子资源（url=/erp/crm/pages/report/*.page.yaml）；`/erp/crm/pages/report/lead-conversion-funnel.page.yaml` + `forecast-accuracy.page.yaml` 两 page 文件存在——**P1-RC-038 须新增 campaign-attribution.page.yaml + action-auth 子资源**（镜像 lead-conversion-funnel 范式：FLUX 组件 + ErpCrmReport__renderHtml/download 调用）。L1 报表 SQL 含 campaign.name（JOIN）——dataset 行须含 campaignName 字段（镜像 funnel 的 stageName 解析范式 `resolveStageNames:246-255`）。
  - **测试基线**：`TestErpCrmLeadConversion`（9 @Test）/`TestErpCrmForecastAndScoring`（5 @Test）强测（无 UTM 相关测试）；报表测试既有**同域范式** `TestErpCrmReportRendering`（`service/report/`，renderHtml/download/dataset 直调 ErpCrmReportBizModel + 数据注入断言 + 空数据集 + 路径注入防护）——**Decision 项**：P1-RC-038 断言经 `ErpCrmReportBizModel.renderHtml/buildCampaignAttributionDataset` 直调（镜像 TestErpCrmReportRendering，不依赖 GraphQL 层）或 dataset 级单测。
- **预授权判据**（第一批纯预授权）：纯 BizModel + 报表 dataset/xpt.xml/page/action-auth，**不触 ORM 结构**（campaignId/utmMedium/utmSource/medium/source 列全部现存）/会计过账/删除；roadmap RC-R1.24 行 `todo`，Deps（R1.0 done）已满足；A4.2.90/91 明示修复归 MR1 纯 BizModel/报表模板预授权。
- **涉及文件**：`ErpCrmLeadBizModel.java`（UTM copy + 注入 IErpCrmCampaignBiz）；`ErpCrmReportBizModel.java`（campaign-attribution dataset）；新 `campaign-attribution.xpt.xml` + `campaign-attribution.page.yaml` + action-auth.xml 子资源；测试类 1 个新增 + `_cases/` 快照。

## Goals

- **UTM copy-on-create（P1-RC-037）**：`ErpCrmLeadBizModel` 注入 `IErpCrmCampaignBiz`，`defaultPrepareSave` 新建路径（id==null）+ campaignId 非空 + utmMedium/utmSource 未显式传入（null）时复制 campaign.medium→utmMedium / campaign.source→utmSource；显式传入不覆盖。
- **归因报表（P1-RC-038）**：`ErpCrmReportBizModel.prepareDataset` 增 `campaign-attribution` case + `buildCampaignAttributionDataset()`（GROUP BY campaignId → campaignName/leadCount/expectedRevenue，JOIN campaign 解析 name）+ `@BizQuery campaignAttributionData` + 新 `campaign-attribution.xpt.xml` 模板 + `campaign-attribution.page.yaml` + action-auth.xml 子资源注册。
- **零回归**：既有 crm 测试全绿（`TestErpCrmLeadConversion` 9 @Test 等）+ 全仓构建。
- **owner doc 收敛注记**：`marketing.md §二 UTM 追踪与归因` 或 `README.md §ErpCrmLead` 补 UTM copy + 归因报表实现注记；`use-cases.md` 需求契约段不动（真相源冻结条款）。
- **回填**：arm-index P1-RC-037/P1-RC-038 → `done (RC-R1.24)` + roadmap 行 → `done` + `docs/logs/` 日志条目。

## Non-Goals

- **不实现 UTM term/content 全链复制**（L1 UC-CRM-07 字面仅 campaignId/utmMedium/utmSource 三项；utm_term/utm_content 属 marketing.md 设计扩展非 L1 验收标准，登记 watch-only）。
- **不实现 Campaign ROI/预算计算**（marketing.md 活动生命周期功能，非 UC-CRM-07 归因报表范围）。
- **不触 ORM 结构**（零列/零索引——全部所需字段现存）。
- **不改真相源契约段落**（use-cases L1 不动）。
- **不实现归因报表的权限细分**（按 campaign 维度行级权限——L1 未要求，超出最小修复面）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧的预授权代码逻辑修复，Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/crm/use-cases.md`（L1 UC-CRM-07）+ `docs/design/crm/marketing.md`（§二 UTM 追踪与归因）+ `docs/design/crm/README.md`（§ErpCrmCampaign §ErpCrmLead）+ `docs/audits/2026-08-07-2345-rc-ma4-a4-2-83-96-crm-lead-lifecycle-marketing-forecast-runtime.md`（A4.2.90/91 运行时证据）
- Skill Selection Basis: 实现面 = BizModel defaultPrepareSave 派生 + 报表 dataset/xpt.xml（`nop-backend-dev`：defaultPrepareSave 钩子、跨实体经 I*Biz、DB 级聚合报表数据集范式）；前端 page.yaml/action-auth（`nop-frontend-dev`：报表页镜像范式）；测试（`nop-testing`：JunitAutoTestCase GraphQL 断言 + 快照录制）。无 view.xml/xbiz/ORM 变更。

## Infrastructure And Config Prereqs

- 无新 config key/外部服务（UTM copy 为创建时派生硬逻辑；报表经既有 nop-report 引擎）。
- 分域验证前置：`mvn install -DskipTests` 后 `mvn test -pl module-crm/erp-crm-service`。

## Execution Plan

### Phase 1 - UTM copy-on-create（P1-RC-037）

Status: planned
Targets: `ErpCrmLeadBizModel.java`
Skill: `nop-backend-dev`

- Item Types: `Fix | Decision`
- Prereqs: 无（既有基线）

- [ ] `Decision` **「若未显式传入」判定载体**：选项 A（推荐）= entityData 中 utmMedium/utmSource 为 null 即视为未显式传入（新建路径 id==null 时复制；非 null 保留用户显式值——CrudBizModel save 经 GraphQL 传入字段直接 set 到 entity，null 即未传）；选项 B = 经 entityData 的 source 字段/提交数据 map 区分显式 null vs 未传（超出最小修复面，弃）。记录理由。
      - Skill: `nop-backend-dev`
- [ ] `Fix` `ErpCrmLeadBizModel` 注入 `IErpCrmCampaignBiz`（@Inject 非 private，对齐 Nop IoC 规则）。
      - Skill: `nop-backend-dev`
- [ ] `Fix` `defaultPrepareSave` 新建路径（`lead.getId() == null`）+ `campaignId != null` 时：`utmMedium == null` → 经注入的 IErpCrmCampaignBiz 查 campaign 复制 `medium`；`utmSource == null` → 复制 `source`（campaign 不存在/null 字段跳过不抛异常——**Decision 子项**记录空值跳过语义）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 新建 Lead（campaignId 非空 + 未显式传 utm）→ utmMedium/utmSource 落库 = campaign.medium/source（Phase 3 断言证实）
- [ ] 显式传入 utmMedium/utmSource → 不被覆盖（Phase 3 断言证实）；campaign 缺失/null 字段 → 跳过不抛（Phase 3 断言证实）
- [ ] 零 ORM 变更（`git diff --stat` 仅 erp-crm-service Java + `_cases/` 快照）

### Phase 2 - 归因报表（P1-RC-038）

Status: planned
Targets: `ErpCrmReportBizModel.java`；新 `campaign-attribution.xpt.xml`；新 `campaign-attribution.page.yaml`；`erp-crm.action-auth.xml`
Skill: `nop-backend-dev`（后端 dataset）+ `nop-frontend-dev`（page/action-auth）

- Item Types: `Add`
- Prereqs: 无（报表独立于 Phase 1，可并行但同域连续执行）

- [ ] `Add` `ErpCrmReportBizModel`：`prepareDataset` 增 `case "campaign-attribution"` + `buildCampaignAttributionDataset()`（ormTemplate DB 级 GROUP BY campaignId + COUNT + SUM(expectedRevenue)，经 campaign to-one 解析 campaignName，镜像 `buildLeadConversionFunnelDataset:170-204` + `resolveStageNames:246-255` 范式）+ `@BizQuery campaignAttributionData`（镜像 `leadConversionFunnelData:130-134`）。
      - Skill: `nop-backend-dev`
- [ ] `Add` 新 `campaign-attribution.xpt.xml`（镜像 lead-conversion-funnel.xpt.xml：表头 campaignName/leadCount/expectedRevenue 三列 + 合计行 + styles 复用）。
      - Skill: `nop-backend-dev`
- [ ] `Add` 新 `campaign-attribution.page.yaml`（镜像 lead-conversion-funnel.page.yaml：FLUX + ErpCrmReport__renderHtml/download 双入口）+ `erp-crm.action-auth.xml` crm-report 子资源注册（orderNo=10030，app:useCases="UC-CRM-07"）。
      - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] `ErpCrmReport__renderHtml(reportName="campaign-attribution")` 渲染成功且行含 campaignName/leadCount/expectedRevenue（Phase 3 断言证实）
- [ ] 菜单/权限资源注册可达（page.yaml + action-auth 子资源落盘）；零 ORM 变更

### Phase 3 - 测试矩阵

Status: planned
Targets: `module-crm/erp-crm-service/src/test/java/app/erp/crm/service/`（新增 `TestErpCrmUtmAttribution.java` 或扩展）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1-2 完成

- [ ] `Add` P1-RC-037 矩阵：① 新建 Lead + campaignId + 未传 utm → 落库复制值；② 显式传 utmMedium → 保留显式值；③ campaignId 空/无 campaign → 跳过不抛；④ 更新路径（id!=null）不触发复制（仅新建）。
      - Skill: `nop-testing`
- [ ] `Add` P1-RC-038 矩阵：① seed 多 campaign 多 lead（含 expectedRevenue）→ campaignAttributionData 聚合正确（count + sum 数值断言）；② 无 campaign 关联的 lead 不计入（GROUP BY campaignId 下 null 组排除）；③ `ErpCrmReportBizModel.renderHtml("campaign-attribution")` 冒烟渲染成功（数据注入断言，镜像 TestErpCrmReportRendering 直调范式）。
      - Skill: `nop-testing`
- [ ] `Proof` `_cases/` 快照录制 + 既有 `TestErpCrmLeadConversion`/`TestErpCrmForecastAndScoring` 零回归。
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 新增测试矩阵全绿 + 既有 crm 测试零回归：`mvn test -pl module-crm/erp-crm-service`（BUILD SUCCESS）
- [ ] 两 finding 全部路径均有断言证据；快照录制完成

### Phase 4 - 文档回填 + arm-index/roadmap 状态

Status: planned
Targets: `docs/design/crm/marketing.md`（或 `README.md`）；`docs/audits/arm-index.md`；`docs/backlog/requirement-compliance-roadmap.md`；`docs/logs/2026/08-14.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1-3 完成

- [ ] `Add` owner doc 注记：UTM copy-on-create 实现注记（仅新建路径 + 未显式传入语义）+ 归因报表实现注记（dataset/xpt/page 接线）；不修改需求契约段。
      - Skill: none
- [ ] `Add` arm-index P1-RC-037/P1-RC-038 → `done (RC-R1.24)` + 修复落地摘要；roadmap RC-R1.24 → done；`docs/logs/2026/08-14.md` 日志条目。
      - Skill: none

Exit Criteria:

- [ ] arm-index/roadmap 状态回填 + owner doc 注记落盘；日志条目写入

## Draft Review Record

- Independent draft review iteration 1: accept（mission-driver review，2026-08-14）——逐项核对实仓：`ErpCrmLeadBizModel.defaultPrepareSave:196-223` 无 UTM copy 且仅注入 IErpCrmStageBiz；`ErpCrmReportBizModel.prepareDataset:151-164` 仅 2 case + `ErpCrmCampaignBizModel` 19 行 stub；`**/report/crm/*.xpt.xml` 仅 2 文件；action-auth.xml:158-170 crm-report 两子资源（10010/10020）；ORM 字段（Lead campaignId propId22/utmMedium 23/utmSource 24 + to-one campaign :235；Campaign name propId3/medium 6/source 7）全部现存；roadmap RC-R1.24 行 todo + 预授权声明（代码逻辑类自动执行）；arm-index P1-RC-037/038 行 + A4.2.90/91 裁决吻合。修复 3 处 Minor：`ErpCrmLeadBizModel.java:33`→`:4/:53-54`、报表范式行号漂移（`:170-204`→`:194-229`、`:130-134`→`:176-178`）、Phase 1 item 3「经 IErpCrmCampaignBiz 或 ORM to-one」歧义收紧为注入查证；补强报表测试范式为同域既有 `TestErpCrmReportRendering`（service/report/，直调范式）。零 Blocker/Major，promote active。
- Independent draft review iteration 2: <pending>

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。**完整仓库验证在此处**：结束时运行一次全量验证。

- [ ] 范围内行为完成
- [ ] 相关文档对齐
- [ ] 已运行验证（`mvn test -pl module-crm/erp-crm-service` 全绿 + `mvn clean install -DskipTests` 全量 BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### P1-RC-037 UTM term/content 全链复制

- Classification: `watch-only residual`
- Why Not Blocking Closure: L1 UC-CRM-07 验收标准仅 campaignId/utmMedium/utmSource 三项（`use-cases.md:139-143`）；utm_term/utm_content 出现在 marketing.md 设计段（`:78-80`）非 L1 验收标准——按 §4 Q1 真相源层级（L1 权威），本行实现 L1 三项即满足；term/content 全链复制归营销设计扩展。
- Successor Required: `no`

### P1-RC-038 归因报表 campaign 维度权限/过滤

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: L1 SQL 字面（`use-cases.md:145-147`）无 orgId/时间范围过滤要求；报表数据集按全量 campaign GROUP BY（orgId 过滤经既有平台管道/组织隔离机制兜底）——行级权限细分是增强非 L1 义务。
- Successor Required: `no`

## Closure

Status Note: <pending — 执行完成并独立结束审计后填写>

Closure Audit Evidence:

- Auditor / Agent: <pending — 独立结束审计子代理>
- Evidence: <pending>

Follow-up:

- <pending — 无范围外 follow-up；MR1 第一批后续 RC-R1.25+ 由 mission driver 继续>
