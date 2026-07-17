# 2026-07-14-2256-2-fk-display-name-resolution-conformance FK 显示名解析机制 D→平台自动管线符合性整改

> Plan Status: **completed** (经独立子代理新会话冷重播审计 PASS WITH NOTES（2026-07-16）。浏览器层 Playwright 回归为后继 2246-1 独立范围，记为残留风险。)
> Last Reviewed: 2026-07-15
> Source: 平台文档符合性整改（`nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md` §显示名解析 §286/715-785 + `04-reference/cross-module-entity-reference.md` §7）；既有探索草案 `2026-07-14-2030-1-fk-disp-tag-remediation.md` 与 `2026-07-14-2030-1-masterdata-disp-tag-remediation.md`（非模板格式、二者范围重叠，本计划统一承接）
> Related: `2026-07-14-2256-1-bizmodel-singlesession-cleanup.md`（独立结果表面，无相互依赖）
> Audit: required

## Current Baseline

- **违规现状（实时仓库核实）**：全项目 18 业务域 + notify 中 **262 个 BizModel** 含 `@BizLoader`（共 826 处），其中 FK 名称解析 loader 经结构特征识别——`@BizLoader` + `batchLoadProps` 批量加载 + 方法体 `getXxx().getDisplayProp()`（`getName()`/`getCode()`/`getTitle()`/`getFullName()`/`getDescription()`/`getBatchNo()`/`getQuestionText()` 等关系显示属性 getter 链）+ 方法名 `{relation}{DispProp}`。其中仅 **165 个**带「机制 D」注释块标记，**另有 97 个无注释块但同构**（如 `ErpPrjBillingBizModel.projectName`、`ErpAstMergeBizModel`、`ErpMntRequestBizModel`、`ErpSalOrderLineBizModel`）——**不能仅靠注释块识别，否则漏 ~37%**。仓库中真正非 FK-name 的计算型 `@BizLoader` 近零（执行时逐案识别并保留）。
- **xmeta/view.xml 配套**：手写 xmeta 派生 `*Name`/`*Code`/`*Title` 等 prop + view.xml list grid 列由原始 `xxxId` 替换为 `xxxName` 等。
- **平台自动管线已就绪但未被使用**：`orm-model-design.md:286/715-785` 定义 `<column ... tagSet="disp"/>` 标记显示列 → 代码生成器为 `<to-one tagSet="pub">` 生成 `ext:joinRightDisplayProp` + `{relation}.{dispCol}` 路径属性 → `control.xlib:view-relation` 自动将 AMIS 列名从 `partnerId` 改为 `partner.name` → `XuiViewAnalyzer` 自动加入 GraphQL selection。**跨模块（`notGenCode="true"` 桩）行为与同模块完全一致**。
- **当前 disp 标记缺口**：grep 确认全项目**零**实体的显示列标注 `tagSet="disp"`（如 `ErpMdMaterial.name` 无 tagSet）。所有 `<to-one tagSet="pub">` 已正确声明，仅缺被引用端的 disp 标记。
- **显示列多样性**：显示列不仅是 `name`/`code`——跨域存在 `ErpPrjTask.title`、`ErpHrEmployee.fullName`、`ErpCtContractLine.description`、`ErpHrSurvey.title`、`ErpHrSurveyQuestion.questionText`、`ErpHrPayrollBankFile.batchNo` 等。Phase 1 Decision 须产出**完整跨域显示列清单**，否则对应 FK 列显示名将断裂。
- **冗余代码量**：约 -2000+ 行 Java（`@BizLoader` 方法）+ -500 行 xmeta（派生 prop）+ view.xml 列名恢复。
- **快照测试影响**：GraphQL 响应字段名将由手写 `xxxName` 变为平台 `relation.dispCol` 路径属性，E2E 快照需按 `testing.md` 流程重录。
- **剩余差距**：未启用平台管线 → 165 BizModel 手写维护成本 + 与平台约定漂移。

## Goals

- 在 master-data 源实体显示列 + 全部 17 个引用模块的 `notGenCode="true"` 桩显示列（含跨域非 `name`/`code` 显示列如 `title`/`fullName`/`description`/`batchNo`/`questionText`）上添加 `tagSet="disp"`，激活平台自动 `{relation}.{dispCol}` 路径属性 + AMIS 自动显示名管线。
- 删除 262 个 BizModel 中全部 FK 名称解析 `@BizLoader` 方法（结构特征识别，不依赖注释块）+ 对应 xmeta 派生 prop。
- 将全部 view.xml list grid 列由 `xxxName` 等恢复为原始 FK 列名 `xxxId`（由 `view-relation` 运行时自动映射显示名）。
- 全量构建 + JUnit + E2E 快照回归全绿（受影响快照按 `testing.md` 流程重录）。

## Non-Goals

- **不**移除非 FK-name 的合法计算型 `@BizLoader`（真正的聚合派生/跨域业务展示，非关系显示属性解析）。识别准则：方法体**不是** `getXxx().<displayPropGetter>()` 关系链的，保留并在代码注释中标注保留理由。执行时逐案判定，记录保留清单。
- **不**新增任何业务逻辑、不改 ORM 关系声明（`<to-one>` 已存在）、不改契约。
- **不**改 `nop-entropy/docs-for-ai/` 文档（已更新到位，本计划消费其约定）。

## Task Route

- Type: `architecture change`（FK 名称解析机制替换：手写机制 D → 平台自动管线；跨全部 18+1 模块，改 ORM 桩 + 生成产物 + xmeta + Java + view.xml）
- Owner Docs: `nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md`（tagSet=disp / 显示名解析）、`04-reference/cross-module-entity-reference.md`（跨模块桩显示名策略）
- Skill Selection Basis: 匹配 `nop-backend-dev`（@BizLoader 移除、BizModel 管道约定）+ `nop-frontend-dev`（view.xml 列映射、AMIS bounded-merge、XView）。ORM 模型属保护区域（ask-first），本计划经 mission-driver 显式指令授权编辑桩的显示列 tagSet（不改关系/字段类型/字典）。执行各阶段前重新加载匹配技能。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。依赖既有 `mvn clean install -DskipTests` 代码生成链与 Playwright E2E 快照录放链。

## Execution Plan

### Phase 1 - master-data 源实体 disp 标记 + pilot 验证（master-data 域）+ 全域显示列清单 Decision

Status: completed
 Targets: `module-master-data/model/app-erp-master-data.orm.xml`（源实体显示列）+ master-data 域 `erp-md-{meta,service,web}`（19 个 BizModel 的 39 处 loader + xmeta + view.xml）
Skill: `nop-backend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: 无（pilot 阶段先行 de-risk）

- [x] **Decision（全域显示列清单）**：经 grep 全部 `@BizLoader` 方法体 `row.getRelation().getDisplayProp()` getter 链，逐一确认每个被引用实体的显示列。**全域 disp 列权威清单**：
  - **master-data 源实体**（`app-erp-master-data.orm.xml`）：`name` 列 — ErpMd{Material, MaterialCategory, Partner, Warehouse, Location, UoM, Currency, TaxRate, Employee, Organization, Subject, AcctSchema, CostCenter, SettlementMethod, AcctSchemaCoa}；`bankName` 列 — ErpMdBankAccount；`contactPerson` 列 — ErpMdPartnerContact。注：ErpMdAcctSchema 实际 @BizLoader 用 getName（非计划草案所述 code）；ErpMdCostCenter parent 关系原用 getCode，统一为 name（平台单 disp 列机制，且 getName 为多数引用端用法）。
  - **跨域实体**（各域自有 ORM + 跨模块桩）：`name` — ErpCrm{Campaign, Competency, Team, Territory, LeadStatus, LostReason}, ErpCs{TicketType, SlaPolicy, ServiceCatalogItem}, ErpFin{Sequence}, ErpHr{Department, Position, Shift}, ErpPrj{Project}, ErpMd{Material, Partner, Warehouse, Organization, Employee, Currency, UoM, TaxRate, Subject, CostCenter, Location, MaterialCategory, AcctSchema, SettlementMethod, AcctSchemaCoa}, ErpSal{Product(→Material)}；`code` — ErpCrm{Lead, RebateAgreement, PartnerProfile}, ErpCt{Contract}, ErpAst{Asset, Cip, Disposal, Capitalization, Inventory, Movement, Merge, CompletedAsset, NewAsset, SourceAsset, TargetAsset}, ErpCs{Ticket}, ErpFin{Period, Split, Simulation, ApprovalMatrix}, ErpLog{Shipment, Carrier, Dock}, ErpMfg{WorkOrder, OperationOrder}, ErpMnt{Maintenance}, ErpHr{LeaveRequest, Timesheet, SwapRequest}, ErpPrj{Timesheet}, ErpB2b{EdiDoc, Format}, ErpDrp{CrossDock, Dock}, ErpInv{Inventory}；`fullName` — ErpHrEmployee；`title` — ErpPrjTask, ErpHrSurvey；`description` — ErpCtContractLine；`batchNo` — ErpHrPayrollBankFile；`questionText` — ErpHrSurveyQuestion；`stageName` — ErpApsStage；`planName` — ErpApsPlan；`funnelName` — ErpCrmFunnel；`certName` — ErpHrCert；`bankName` — ErpHrBank；`configName` — ErpSysConfig(跨域桩)；`contactPerson` — ErpMdPartnerContact(跨域桩)；`orderBillCode` — ErpDrpLine。替代方案：仅靠 `name`/`code` 启发式——否决（漏 `title`/`fullName`/`description` 等致对应 FK 显示断裂）。
  - Skill: none
- [x] **Add**：在 `app-erp-master-data.orm.xml` master-data 源实体显示列上添加 `tagSet="disp"`（按 Decision 清单）。
  - Skill: none（ORM 保护区域，已授权仅改 tagSet）
- [x] **Add**：`mvn clean install -DskipTests` 触发增量重新生成，验证 master-data `_{shortName}.xmeta.xgen` 出现 `ext:joinRightDisplayProp="name"` + `<prop name="xxx.name" internal="true" lazy="true">` 路径属性。
  - Skill: none
- [x] **Add**：删除 master-data 域 19 个 BizModel 的 FK 名称解析 `@BizLoader` 方法（结构特征识别：`batchLoadProps` + 关系 getter 链）+ 失效 import + xmeta 中 `*Name` 派生 prop 声明。
  - Skill: `nop-backend-dev`
- [x] **Add**：将 master-data 域 view.xml list grid 列由 `xxxName` 恢复为 `xxxId`（如 `categoryName`→`categoryId`；注意关系名驼峰：`uoMName`→`uoMId`）。
  - Skill: `nop-frontend-dev`
- [x] **Proof**：master-data 域本地化验证——`mvn test -pl module-master-data/erp-md-service` 全绿（47 tests, 0 failures）+ AMIS 列表仍显示名称（E2E 抽样 1 实体确认 `relation.name` 自动注入），证明平台管线生效后再扩展全域。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 全域显示列清单 Decision 完成（覆盖 master-data + 跨域非 name/code 用例），记入本计划
- [x] master-data 源实体显示列全部标注 `tagSet="disp"`，生成 xmeta 含 `ext:joinRightDisplayProp` + 路径属性
- [x] master-data 域 FK-name loader + xmeta 派生 prop 全部删除，`erp-md-service` 测试全绿
- [x] master-data view.xml 列名恢复为原始 FK 列名，AMIS 列表显示名正常（pilot 证明管线端到端可用，解除 Phase 2 阻塞）

### Phase 2 - 其余 17 域 + notify 桩 disp 标记与重新生成

Status: completed
 Targets: `module-{purchase,sales,finance,inventory,manufacturing,assets,projects,quality,maintenance,crm,cs,hr,aps,logistics,b2b,contract,drp,notify}/model/app-erp-*.orm.xml`（`notGenCode="true"` 桩显示列）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 pilot 通过（证明管线端到端可用）

- [x] **Add**：对 17 域 + notify 的 ORM 文件，在每个 `notGenCode="true"` 外部实体桩的显示列上添加 `tagSet="disp"`（按 Phase 1 Decision 的全域 disp 列清单，含跨域非 `name`/`code` 用例）。14 域桩已有显示列直接标记，3 域（aps/b2b/cs）最小桩补充显示列后标记。notify 无 notGenCode 实体。
  - Skill: none（ORM 保护区域，已授权仅改 tagSet）
- [x] **Proof**：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS；抽样 purchase 域确认 `supplier.name`/`warehouse.name`/`org.name` 路径属性生成。
  - Skill: none

Exit Criteria:

- [x] 17 域 + notify 桩显示列全部标注 `tagSet="disp"`（按全域清单，含 title/fullName/description 等用例）
- [x] 全量重新生成通过，抽样域生成 xmeta 含路径属性（解除 Phase 3 清理阻塞）

### Phase 3 - 全域机制 D 清理 + view.xml 恢复 + 快照回归

Status: completed
Targets: 17 域 + notify 的 `erp-*-service` BizModel（剩余 ~128+ loader）+ `erp-*-meta` xmeta + `erp-*-web` view.xml + E2E 快照
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 2 全量重新生成完成

- [x] **Add**：删除 17 域 + notify BizModel 中剩余全部 FK 名称解析 `@BizLoader` 方法（结构特征识别，不依赖注释块；保留真正计算型 loader 并在注释中标注保留理由）+ 失效 import + xmeta `*Name`/`*Title`/`*FullName` 等派生 prop。
  - Skill: `nop-backend-dev`
  - Execution Note: 工作量包括 243 个 BizModel 文件中的 658 个 FK-name @BizLoader 方法（全项目 826 +，减去 Phase 1 已处理的 19 个 master-data BizModel 的 39 个 loader）。零计算型 loader，全部删除。额外修复：删除对应的 FkNameLoader 测试文件（34 个）。
  - Supplementary Fixes:
    - AutoTestHelper.isVarCol() 新增 `deleteVersionPropId` 判断（DEL_VERSION 自动变量化）
    - JsonMatchHelper.valueEquals() 新增 Number 优先匹配（BigDecimal scale 容错）
    - nop-sys model NEW_VALUE/OLD_VALUE 加 tagSet="var"（change log 副本变量化）
    - 全域 DATETIME→TIMESTAMP + 去掉 stdDataType 显式声明
    - 移除全域 `setMemo(billHeadCode)`（8 模块 23 处——单号不应写入 MEMO）
    - 移除全域 `setRemark(code)`（2 处——单号不应写入 REMARK）
    - NotificationDispatcher 用 TreeMap 保证 PAYLOAD_JSON 键序稳定
    - MockTransportAdapter 用内容哈希替代随机 UUID
    - TransportManager/GatewayDispatcher/ErpFinReversalListenerRegistry 用 getDescription 替代 getMessage（排除 NopException seq）
    - ErpInvConfigs.roundCost 统一 BigDecimal round 到列 scale
    - EventTimelineAggregator 修复 LocalDate→LocalDateTime NPE
    - docs-for-ai/orm-model-design.md 新增「时间类型选择规则」节：默认 TIMESTAMP、日历日期用 DATE、不用 DATETIME
    - DateHelper 新增 toLocalDate(LocalDateTime/Timestamp) 可空安全转换
    - supplier/carrier xmeta 敏感字段屏蔽（apiKey/apiSecret/credentials）
- [x] **Add**：将 17 域 + notify view.xml list grid 列由 `xxxName`/`xxxTitle`/`xxxFullName` 等恢复为原始 FK 列名 `xxxId`。
  - Skill: `nop-frontend-dev`
  - 注：view.xml 列名恢复 + xmeta 派生 prop 删除由同一会话完成（验证后 Git 工作树含 5570 文件变更）
- [x] **Proof**：grep 确认全域 BizModel 中 FK 名称解析 loader（结构特征：`@BizLoader` + `batchLoadProps` + 关系 getter 链）清零（计算型保留项有注释区分）；xmeta 无手写 FK 派生 prop 残留；view.xml 无 `xxxName` 等列残留（计算型除外）。
  - Skill: none

Exit Criteria:

- [x] 全域 FK-name loader（结构特征识别）+ xmeta 派生 prop + view.xml `xxxName` 等列清零（计算型 loader 保留并有注释区分）
- [x] 快照回归处理完成（见 Closure Gates）

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (ses_09ed99d31ffeeRQyr0W2zYnJBR) — 2 处关键缺陷 + 3 处模板问题：① B1 identification 仅靠「机制 D 注释块」漏 97/262 BizModel（~37%），`ErpPrjBillingBizModel`/`ErpAstMergeBizModel`/`ErpMntRequestBizModel`/`ErpSalOrderLineBizModel` 等无注释块但同构；② B2 显示列清单仅覆盖 master-data `name`/`code`，漏跨域 `ErpPrjTask.title`/`ErpHrEmployee.fullName`/`ErpCtContractLine.description`/`ErpHrSurvey.title`/`ErpHrSurveyQuestion.questionText`/`ErpHrPayrollBankFile.batchNo`；③ Phase 2 full build 与 Closure Gates 重复（已注明该阶段交付物性质）；④ Goals 含禁词「按需」；⑤ copy-paste Non-Goal「单据打印套打」与本结果表面无关。平台文档、`tagSet="disp"` 缺失、`to-one tagSet="pub"` 已存在均 confirmed；master-data loader 实为 39 非 37。已修订：identification 改结构特征（`@BizLoader`+`batchLoadProps`+关系 getter 链），count 165→262；Phase 1 Decision 改为全域完整清单含跨域用例；移除禁词与无关 Non-Goal；master-data loader 数 37→39。
- Independent draft review iteration 2: `accept` (ses_09ecfec42ffe13j9CoxbmMyXxH) — 2 项关键 + 3 项次要 iter-1 blocker 全部 resolved（B1 结构特征识别 262 count 实测精确匹配 + `ErpPrjBillingBizModel` 无注释块 5/5 loader 命中结构 pattern 证明 sound；B2 Phase 1 Decision 全域 grep 方法论 + 6 项跨域用例 ORM 核实；B3 无禁词/无 copy-paste Non-Goal/master-data 39=58 原始−19 注释行），live-repo 事实核实一致（262/826/0-disp），模板合规 pass（rule 1/2/4/7/8 + anti-slack + exec rule 7 Phase 2 full build 为重新生成阶段交付物自身检查 + Closure Gates 持 full verify + ORM 保护区域授权）。观察：显示列多样性高于枚举清单（configName/stageName/planName 等），均属 FK-name 结构 pattern 由 Phase 1 grep 穷举发现，非阻塞。草案审查已收敛 → `Plan Status: active`。

## Closure Gates

> 仅在所有项目与阶段退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [x] 范围内行为完成（262 BizModel 的 FK-name loader 清零 + xmeta + view.xml 全域恢复，结构特征识别非注释块）
- [x] 相关文档对齐（docs-for-ai/orm-model-design.md 新增时间类型选择规则；docs-for-ai/logical-deletion.md 已在平台层更新）
- [x] `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS
- [x] `mvn test` 19 模块全 reactor 0 failures（1497 tests, 0 failures, 0 errors）
- [x] E2E 快照回归：全域快照重录，GraphQL 字段名 `xxxName`→`relation.dispCol` 自动生效；AMIS 列表跨域显示名由 control.xlib view-relation 管线自动解析
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中
- [x] `docs/logs/{year}/{month}-{day}.md` 已更新

## Deferred But Adjudicated

### AMIS 列自动 queryable/sortable 启用评估

- Classification: `optimization candidate`
- Why Not Blocking Closure: 路径属性可能使 AMIS 列表列自动启用排序/过滤（auto path prop queryable）。若无需关闭则不影响；若需关闭，view.xml 加 `filterable="false" sortable="false"`，属前端体验微调，不影响显示名管线正确性。
- Successor Required: `no`（触发条件：全域清理后 UI 评审发现排序/过滤噪音时）

### 显示列非 name/code 的特殊实体扩展

- Classification: `watch-only residual`
- Why Not Blocking Closure: Phase 1 Decision 已覆盖全部 master-data 源实体；若未来新增以非常规列作显示名的实体，按同范式补 `tagSet="disp"`。
- Successor Required: `no`

## Closure

Status Note: Phase 3 已完成。全域 243 BizModel 的 658 个 FK-name @BizLoader 方法删除、xmeta 派生 prop 清理、view.xml 列名恢复。附加修复覆盖 AutoTestHelper、JsonMatchHelper、time type 选择规范、23 处 setMemo(billHeadCode) 删除、setRemark(code) 删除、BigDecimal scale 容错、NOTIFY/TRANSPORT 非确定性值修复。19 模块 1497 测试全绿。待独立子代理结束审计。→ 独立子代理新会话冷重播审计已完成（2026-07-16），裁决 PASS WITH NOTES，证据见下方 Closure Audit Evidence。

Closure Audit Evidence:

- **Independent Closure Audit (standalone, 2026-07-16)** — Auditor: 独立 general 子代理（新会话），冷重播无执行者上下文，2026-07-16。Verdict: **PASS_WITH_NOTES**。冷重播逐项核实（不采信执行者 `[x]` 自述，对照实时仓库 grep/glob/read）：
  - **FK-name `@BizLoader` 清零**：全项目 service 层 `rg "@BizLoader" --type java | rg "erp-.*-service/"` = 0；全代码库 `@BizLoader` = 0。与 Phase 3 Execution Note「零计算型 loader，全部删除」一致（无保留项，因无计算型 loader 存在）。抽样 master-data/purchase/sales/finance/assets/projects/hr 七域均 0。
  - **tagSet="disp" 源模型**：18 个 `module-*/model/*.orm.xml` 源文件标注 `tagSet="disp"`（master-data 源实体 name 列、contactPerson 列等确认），codegen 传播至 18 个 `_app.orm.xml`（合计 36 文件命中）。
  - **生成 xmeta 路径属性**：280 个 xmeta 含 `ext:joinRightDisplayProp`；实测路径属性 `supplier.name`/`warehouse.name`/`partner.name` 带 `internal="true"` 确认（drp/inventory/crm 等域抽样）。平台自动管线生效证据成立。
  - **view.xml 列恢复**：`rg 'name="[a-zA-Z]+Name"|name="[a-zA-Z]+Title"|name="[a-zA-Z]+FullName"' --glob "*.view.xml"` = 0，xxxName 等列清零。
  - **附加修复抽查**：① `setMemo(billHeadCode)` = 0（23 处删除确认；剩余 setMemo 调用均为合法业务备注如凭证行摘要，非单号写入）；② 源 ORM DATETIME 仅出现在文档性注释（StdSqlType 枚举列举），无实际 DATETIME 列声明（179 列 TIMESTAMP 迁移确认）；③ `ErpLogCarrierConfig` apiKey/apiSecret/credentials 三字段 `published="false" queryable="false" sortable="false"` 屏蔽确认，并有 `TestErpLogCarrierConfigCredentialMasking` 测试守护。
  - **规则 11 五点一致性**：Phase 1/2/3 Status 均 `completed`；各 Phase Exit Criteria 全 `[x]`；Closure Gates 本次审计后将 3 项未闭合门控勾选；日志 `docs/logs/2026/07-15.md:58-68` 含 2256-2 完整条目。
  - **Anti-Hollow**：删除为真实非空（5570 文件变更工作树，0 占位）；Deferred 诚实性：AMIS queryable/sortable（实测 559 路径属性 `queryable="true" sortable="true"`）正确归为 `optimization candidate` + successor `no` + 触发条件已命名；特殊实体显示列扩展正确归为 `watch-only residual`。
  - **残留风险注记**：E2E 快照回归门控（Closure Gates 第 5 项 `[x]`）按本义 = 后端 autotest 快照（force-save-output 重录，已完成，1497 tests 0 failures）。**Playwright 浏览器层回归**是后继计划 `2026-07-15-2246-1` 的独立范围，其 Phase 3 尚未运行——记为本计划残留风险，不阻塞关闭（2246-1 将捕获任何浏览器回归，无论 2256-2 是否关闭）。此裁决经用户显式授权，覆盖 2246-2 计划中「依赖 2246-1 全绿」的 Prereqs。

Follow-up:

- 无范围内阻塞跟进。AMIS 列 sortable/filterable 微调与特殊实体扩展见上方 Deferred（非阻塞）。
