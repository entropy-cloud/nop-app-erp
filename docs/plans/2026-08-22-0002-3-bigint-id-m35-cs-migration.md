# 2026-08-22-0002-3-bigint-id-m35-cs-migration 主键/外键 string 化 M3.5：cs 域迁移（冻结序位次 8）

> Plan Status: completed（2026-08-22：Phase 1-4 全部完成 + 独立结束审计 PASS（ses_fd95f274cffe2Rcj70ZcAn1BgW，7 项核证全 PASS，见 Closure Audit Evidence）；起草期 iteration 1-2 独立草案审查收敛 + 保护区域双独立子 agent 批准（技术 ses_fdaecac03ffezjlpdWu00j4cJW / 治理 ses_fdaec67abffet95xJrKGrvjzSu），见 Draft Review Record）
> Mission: id-string-migration
> Work Item: M3.5（cs，冻结序位次 8）
> Last Reviewed: 2026-08-22
> Source: `docs/backlog/id-string-migration-roadmap.md` M2/M3 冻结总序表位次 8（M3.5）
> Related: `docs/plans/2026-08-22-0002-1-bigint-id-m21-finance-migration.md`（批内序 1，本计划硬前置）、`docs/plans/2026-08-22-0002-2-bigint-id-m24-assets-migration.md`（批内序 2）、`docs/plans/2026-08-21-2025-3-bigint-id-m36-contract-migration.md`（标准结构先例）、`docs/plans/2026-08-21-1657-1-bigint-id-m02-forward-coupling-registry.md`（M0.2 登记册，消费来源）
> Audit: required（保护区域 `model/*.orm.xml`：独立 plan-audit + 双独立子 agent 批准，批准记录落盘本文件）

## Current Baseline

- **cs 域规模（2026-08-22 实况 scan）**：`module-cs/model/app-erp-cs.orm.xml` 需改列 **68 = 自有 66（PK 18 + BIGINT FK 48，18 实体）+ notGenCode md stub 2**（`ErpMdOrganization`/`ErpMdPartner` 各 1 列——md 权威源自 M1.1 已 String，翻转 = 与权威源对齐）。自有 FK 含 `orgId` ×13（自有实体 orgId FK 列实测；`name="orgId"` XML 原始出现 17 次含 `<index>` 成员引用，以列定义口径为准）+ `ticketId`/`customerId`/`contractId` 等本域 FK。**不改列**：VARCHAR FK 7 列（`ErpCsTicket.assignedToId`（用户 id）、`ErpCsTeam.teamLeaderId`、`ErpCsTicketAction.operatorId`、`ErpCsContract.attachmentFileId`、`ErpCsServiceCatalogItem.fulfillmentProcessId` 等，显式 string 本就 String）+ `delVersion` 等非 PK/FK BIGINT 列（规则 4 保持 long）。
- **模块链与编译依赖（pom 实测）**：7 模块 = `module-cs/erp-cs-{codegen,dao,meta,service,web,app,api}`（全链构建，无延后）。`erp-cs-service` main compile 依赖 **notify-dao + md-dao + crm-dao + qa-dao（未迁移 Long jar——A2 桥接对象）** + common-service + nop-report；fin-service 为 **test-scope** 依赖（cs-service pom 注释自述「cs-service 主类 DAG 不含 finance，故测试期引入 finance-service 仅取字体资源」；main `import app.erp.fin` 零命中（实测，test 亦零命中）——fin String jar 对 cs 编译中性，M2.1 前置仅因冻结序 test 边口径）；cs-web main 依赖本域 meta/service + nop-web（md-service 为 **test-scope**）；cs-web test-scope 依赖 ast-dao/prj-dao（**批内序 2（M2.4）已 install String 形态 ast-dao；prj-dao 陈旧 Long jar**，页面测试编译对象——预期零 id 穿越，Phase 3 核验）；cs-app 无域级 web 依赖（可建）。
- **cs orm 外域关系（refEntityName 实测）**：md ×17（M1.1 起 cs-dao `_gen` md 关系胶水处于已登记中间态（惰性 jar 陈旧未重编译），**本域迁移即自愈**）；fin/crm/qa ×0（crm/qa 耦合全在 service 代码层 A2 桥接）。
- **M0.2 登记册 cs 视角（§6.8，起草消费已核）**：A1 orm 延后 = 0；**A2 main 桥接 8 条 = crm 4（bridge-main-053..056：`TicketAssignResolver:5/:6/:52/:59`，`ErpCrmTeam`/`ErpCrmTeamMember` 类型级 + `IErpCrmTeamBiz/TeamMemberBiz.findList` 继承方法——Long 参数签名以晚域翻转时为准）+ qa 4（bridge-main-057..060：`ErpCsTicketEscalateToQualityProcessor:10/:151/:184/:189`，`ErpQaNonConformance` + `IErpQaNonConformanceBiz.findList/save` 继承方法）；退役 owner M3.4（crm，位次 17）/M2.3（qa，位次 13）**；**A3 test 桥接 5 条**（bridge-test-113..117：`TestErpCsCatalogFulfillmentEngine`/`TestErpCsQualityEscalation`/`TestErpCsTicketCreateEnrichment`/`TestMockCrmBizModels`/`TestMockQaBizModels` 引用 crm/qa 实体与 IBiz，owner M3.5 = 本计划 Phase 3 退役）；B 退役义务 = 0；C1 后向 main 2 条（backward-144 → md 7 文件；backward-145 → notify 8 文件）；C2 后向 test 2 条（backward-203 → md 8 测试文件；backward-204 → notify 6 测试文件）。被引用面 = **零**（实测 `rg 'import app.erp.cs'` 外域 main 零命中，登记册 §6.8 无被引用清单——本域迁移无外域破坏面）。
- **手写代码冲击面（实测）**：cs-service main 跨域 import = md 7 文件（C1）+ notify 8 文件（C1，`notify(String,Map,ctx)` 签名不变预期零破坏——M1.2 已证，编译核验即可）+ crm/qa（A2 桥接点 2 文件）；dao 手写 IBiz/值对象 Long 签名以编译器清单为准（M0.1 审计附录 C 本域语义 FK Long 参数清单为 Phase 4 门控输入）。
- **测试资产（实测）**：cs-service **26 个测试类**；`_cases` 快照 **633 文件**；`ErpCsWebPagesTest` `@Tag("full-app")` + surefire excludedGroups 模块级排除（已提交治理决策，successor = M4.1）。
- **手写 page.yaml raw-GraphQL `:Long` 变量（本计划范围内 12 处，迁移即失效的实时缺陷，本批最重页面修复面）**：`erp/cs/pages/ErpCsTicket/kanban.page.yaml` ×11（`$id:Long` ×5 → `ErpCsTicket__assign/start/resolve/close/reopen(ticketId:)` mutation 变量 + `$c:Long` ×6（:44/:107/:155/:213/:271/:300）→ `filter_customerId` 查询变量）+ `erp/cs/pages/ErpCsTicketAction/timeline.page.yaml:36`（`$tid:Long` → `filter_ticketId`）——cs 翻转 String 后静态类型不匹配 → adaptor 静默降级、看板/时间线过滤与动作功能失效（M3.6 结束审计 MAJOR-1 同型，contract version-diff 就地 Fix 先例）——**本计划就地 Fix + cs-web 重建验证**（cs-web 在本计划 7 模块 verify 范围内）。注意 `$a:String`（assignedToId）为 VARCHAR 列本就 String，合法保留。
- **已知风险（先例登记）**：① 平台 IoC 回归 `nopSequenceGenerator` self-wait——六域连续复现，按先例修复（test-scope VFS delta + DeltaOverride delta-layer 补 default 层集）；② no-am 测试 classpath VFS 模块集变化（回退 = seq-proof-yaml 模块禁用模式）；③ 陈旧 jar 二进制不兼容（本地仓 crm/qa/prj dao Long jar——A2 桥接登记例外 / prj test-scope 编译对象；ast-dao 为批内序 2 String 新 jar）。
- **回写机制（M0.1 裁定 Decision A，三步）**：① dry-run 时点刷新；② `verify-id-fix-copy-diff.mjs module-cs` 新鲜度门控；③ 单文件落源 + `git diff` 逐行审核。禁止盲 cp、禁止 apply 模式。
- **剩余差距**：cs orm 68 列全 `stdDataType="long"` 待改；cs 手写代码/测试/快照全部 Long 形态；冻结序位次 8（位次 9 hr 起归后续批次）。

## Goals

- cs 域 68 列（自有 66 + md stub 2）`stdDataType` long→string 落源（唯一源文件变更，`stdSqlType` 保持 BIGINT，DDL 零变化）。
- 增量重生成（no-am 7 模块链）+ 编译器驱动修复 cs 全部手写代码 + A2 前向桥接 8 处落桥（crm 4 + qa 4，退役 owner M3.4/M2.3）。
- 快照每域重录（RECORDING→CHECKING；633 文件基线）。
- 语义陷阱 grep 门控清零 + page.yaml `:Long` 12 处就地 Fix（cs-web 重建验证）。
- 消费 M0.2 登记册：A2/A3 桥接 disposition 落盘，C1/C2 修复定位面消费，heal M1.1 登记的 cs-dao `_gen` md 胶水中间态。
- 路线图 M3.5 → `done` + 日志。

## Non-Goals

- 不迁移 crm/quality 域（A2 桥接目标域，归 M3.4/M2.3——均在后续批次）。
- 不改 `delVersion` 等非 PK/FK BIGINT 列（保持 long）；不修 `ErpCsWebPagesTest` 治理排除（successor M4.1）。
- 不跑全量构建/全量测试/E2E/compliance checker（归 M4.1）；不手改任何生成件；手写 view.xml 预期零改动（Phase 4 验证）。
- 不做 fin web/app 补做与其他域的桥接退役（归各自 owner plan，见登记册 B 义务）。

## Task Route

- Type: `implementation-only change`（含保护区域 ORM 变更）
- Owner Docs: `docs/backlog/id-string-migration-roadmap.md` M2/M3 表位次 8 + 横切 §5 设计证据（`../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md` §主键设计方案 B + `docs/design/domain-design-guidelines.md` §16A.4 + M0.1 审计结论 + M0 裁决 §10）；cs 业务语义 owner doc = `docs/design/customer-service/`（Phase 4 注记对象）
- Skill Selection Basis: 路线图 §M1-M3「预期技能」指定域迁移 plan 加载 `nop-backend-dev` + `nop-testing`；ORM 变更机制由 M0.1 审计与平台文档背书。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（无 DB DDL 变更；DB 列保持 BIGINT）。no-am 构建硬前置 = 最后全绿基线 commit 全量 install + md/notify/common 链 install + 位次 3-5 链 install + **批内前置：M2.1 fin 5 模块链 install（String 形态）**（cs test-scope fin-service 依赖 + 冻结序批内顺序）+ 批内序 2（M2.4 assets）链 install（冻结序执行约束——cs 精确前置仅为 M2.1+M1.1+M1.2，但本批按冻结总序在 assets 之后执行）。回滚策略：revert orm.xml + `mvn clean install -pl module-cs/erp-cs-codegen,module-cs/erp-cs-dao,module-cs/erp-cs-meta,module-cs/erp-cs-service,module-cs/erp-cs-web,module-cs/erp-cs-app,module-cs/erp-cs-api -Dmaven.test.skip=true` 重生成回 Long 形态（**Phase 3 完成后回滚需先 revert 测试代码**——String 测试代码对 Long main 会破坏 test-compile）。

## Execution Plan

### Phase 1 - 消费登记册 + orm 回写（保护区域，双批准前置）

Status: completed（2026-08-22 执行记录见各项执行证据）
Targets: `module-cs/model/app-erp-cs.orm.xml`
Skill: none

- Item Types: `Proof | Fix`
- Prereqs: M2.1 ✅（批内序 1 完成且 fin 链已 install）+ M1.1 ✅ + M1.2 ✅（精确前置满足）；批内序 2（M2.4）done（冻结序执行约束）；本计划已通过独立 plan-audit + 第二独立子 agent 复核（保护区域 `auto + dual-agent-approval`，批准记录落盘 Draft Review Record）

- [x] Proof: 消费 M0.2 登记册——读取 `tools/id-migration-registry.json5` + 登记册文档 §6.8 cs 节，逐条核对：(i) A1 orm 延后 = 0（68 列全翻转）；(ii) A2 main 桥接 8 条（crm 4 + qa 4）与本地实测 import 对账（findList/save 为 ICrudBiz 继承方法，Long 参数签名以晚域翻转时为准——桥接按值流转语境落桥）；(iii) A3 = bridge-test-113..117 作为 Phase 3 定位面；(iv) C1 = backward-144（md 7 main 文件）+ backward-145（notify 8 main 文件）与 C2 = backward-203（md 8 测试文件）+ backward-204（notify 6 测试文件）作为 Phase 2/3 定位面；(v) 按 b2b A3' 先例做 FQN 盲区复扫（`rg 'app\.erp\.(hr|inv|mnt|prj|pur|sal|qa|crm|drp|log|mfg)\.' module-cs/erp-cs-service/src/test` 排除 import 行——覆盖本域执行时点全部未迁移晚域，早域 md/notify/aps/b2b/contract/fin/ast 已 String 无盲区风险），命中则补登 + 处置。矛盾则按路线图规则 6 停止回报。
  - Skill: none
  - **执行证据（2026-08-22）**：① json5 + §6.8 逐条核对通过——A1 = 0（全仓 orm-deferral 8 条均为 fin 6 + hr 2，无 cs）；A2 = bridge-main-053..056（crm，TicketAssignResolver:5/:6/:52/:59）+ 057..060（qa，ErpCsTicketEscalateToQualityProcessor:10/:151/:184/:189）行级吻合；A3 = bridge-test-113..117；B = 0；C1 = backward-144（md 7 文件）+ backward-145（notify 8 文件）、C2 = backward-203（md 8）+ backward-204（notify 6）evidence 文件清单逐一存在；被引用面复核 = `rg 'import app\.erp\.cs'` 外域零命中。② FQN 复扫命中 4 处——`TestErpCsQualityEscalation.java:78`（FQN 字段 `app.erp.qa.biz.IErpQaNonConformanceBiz`，无 import）+ `app-test-mock-qa.beans.xml:11`（ioc:type FQN）+ `app-test-mock-crm.beans.xml:11/:15`（ioc:type FQN）→ 按 assets bridge-test-134 先例补登 **bridge-test-135（qa）/136（crm）**（json5 条目 + §6.8 表行 + 盲区追注，counts service-bridge:test 32→34，owner M3.5 本计划 Phase 3 随批退役）；命中全部落入 114/116/117 已登记文件面或其 mock 基建。③ 零矛盾，未触发规则 6。
- [x] Proof: 双独立子 agent 批准记录落盘（批准人指针 + 结论 + 时间），未获批不得进入回写。
  - Skill: none
  - **执行证据**：Draft Review Record 双批准已在案（批准 1 技术 ses_fdaecac03ffezjlpdWu00j4cJW / 批准 2 治理 ses_fdaec67abffet95xJrKGrvjzSu，2026-08-22，依据与结论完整），回写前复核存在性通过。
- [x] Fix: 回写 orm（M0.1 裁定三步机制）——① `node tools/check-bigint-id-types.mjs dry-run` 时点刷新；② `node tools/verify-id-fix-copy-diff.mjs module-cs` 新鲜度门控（零非 stdDataType 行）；③ 门控通过后单文件落源。禁止盲 cp 静态副本、禁止 apply 模式。
  - Skill: none
  - **执行证据**：① dry-run 全仓刷新（12 文件 1148 列，XML 校验 12/12，幂等 yes）；② `verify-id-fix-copy-diff.mjs module-cs` = 变更行 68、非法差异行 0、延后列 0 → 门控通过；③ 单文件落源 `cp _tmp/.../module-cs/model/app-erp-cs.orm.xml`（未用 apply 模式）。
- [x] Proof: `git diff module-cs/model/app-erp-cs.orm.xml` 逐行核对——仅 68 列 `stdDataType="long"→"string"`（自有 66 = PK 18 + FK 48 + md stub 2），`stdSqlType` 零变化、`delVersion`/标签结构零变化；scan cs 段重扫零 `NEEDS FIX`/零 `DEFERRED` 残留。
  - Skill: none
  - **执行证据**：git diff = 68 insertions / 68 deletions 全部为 stdDataType 行（`^-.*stdDataType="long"` 计数 68；diff 中 ± 行仅含 stdDataType 变化，`stdSqlType` 在 ± 两侧逐行保留（136 = 68×2 属性随行出现，值零变化）、delVersion 零出现）；工具重扫 `app.erp.cs.` 段 NEEDS FIX/DEFERRED 零命中；文件内剩余 `stdDataType="long"` = 18（delVersion 族非 PK/FK，规则 4 合法）。

Exit Criteria:

- [x] 登记册消费核对在案（含 FQN 盲区复扫结论）；双批准记录在案；新鲜度门控 + git diff + 工具重扫三重证明变更面精确 = 68 列 stdDataType

### Phase 2 - 增量重生成 + 主代码编译修复 + A2 桥接落桥

Status: completed（2026-08-22 执行记录见各项执行证据）
Targets: `module-cs/erp-cs-dao/src/main/java/**`、`module-cs/erp-cs-service/src/main/java/**`（手写 IBiz/BizModel/Processor/Job/SPI；api beans 生成件随动）
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: Phase 1

- [x] Fix: `mvn clean install -pl module-cs/erp-cs-codegen,module-cs/erp-cs-dao,module-cs/erp-cs-meta,module-cs/erp-cs-service,module-cs/erp-cs-web,module-cs/erp-cs-app,module-cs/erp-cs-api -Dmaven.test.skip=true`（D3 口径：7 模块显式列表、不带 `-am`、`-Dmaven.test.skip=true`）触发增量重生成。预期：cs-dao `_gen` md 关系胶水自愈（M1.1 登记中间态）。
  - Skill: `nop-backend-dev`
  - **执行证据**：首轮构建 codegen/dao/meta 绿（`_gen` 18 实体重生成 + `_app.orm.xml`/xmeta/_templates 随动），service 96 错（49 唯一位点/18 文件）进入编译器驱动修复；修复后复跑 7 模块链 **BUILD SUCCESS（7/7）**。md 关系胶水自愈：dao `_gen` 重生成后与 md-dao String jar 一致（cs-dao 编译零错 + service 跨 md 调用零类型错）。
- [x] Fix: 编译器驱动修复主代码——逐条修复 cs dao + service 手写代码类型错误（定位面：md 7 文件（C1，String 直传/toLong 桥按语境）+ notify 8 文件（C1，签名不变预期零破坏核验）+ 全域 IBiz/值对象 Long 签名 + `.getId()` 下游；以编译器实际清单为准），直到 7 模块链 `-Dmaven.test.skip=true` 构建全绿。修复清单落盘本计划。
  - Skill: `nop-backend-dev`
  - **执行证据（修复清单）**：**dao 9 个 IBiz 接口**（IErpCsTicketBiz/CatalogFulfillmentBiz/EntitlementBiz/KnowledgeBaseBiz/ServiceCatalogItemBiz/SurveyBiz/TicketTimerSessionBiz/TimeEntryBiz/CannedResponseBiz）id 参数 `Long→String`（`@Name("xxId") Long` 全仓 82 处批量 + `matchForCustomer(Long→String)`）；**service 18 手写文件**：entity 5（TicketBizModel（requireTicket/findEntries/resolveCustomerName String 化 + requireEntity 直传 + resolveEscalationTarget 去 String.valueOf）、CannedResponseBizModel（ticketTypeId/collected/macroTicketTypeId/resolveCustomerName/loadTicket.get 直传）、CatalogCategoryBizModel（selfId/parentId/visited/ancestorId/hasChildren/loadCategory）、EntitlementBizModel（loadActiveByPartner/matchForCustomer/requireEntitlement/resolvePartnerName）、KnowledgeBaseBizModel（parseKnowledgeBaseId 改子串 String，去 Long.valueOf））+ matcher 2（EntitlementMatcher（match/Function<Long>/isPartnerMatched）、TimerSessionCalculator（toEntryAgentId 改恒等 String，D2 0 哨兵随 agentId String 化退役））+ processor 11（Resolve/Reopen/MatchAndAttachSla/SurveyCreate/ServiceCatalogItemCreateFromCatalog（customerId Long.valueOf→String.valueOf）/CatalogFulfillmentExecute（findRetryCandidateTicketIds List<Long>→List<String> 等 12 方法）/CannedResponseApply/TicketScanOverdue（second/escalation 局部 String）/EscalateToQuality（materialId/supplierId String 化 + qa 桥，见下）/TimerSession ×4（Ops.generateTimeEntry agentId 恒等直写去哨兵 WARN + settleIfOverdueInNewTx/requireSession））+ job 3（CsatReminder（loadTicket）/EntitlementExpiry（resolvePartnerName）/FulfillmentRetry（List<Long>→List<String>））+ report 1（ErpCsReportBizModel：聚合 Map/Set 键 Long→String + loadTickets eq 直传去 Long.valueOf）+ dashboard 1（ErpCsQualityDashboardBizModel：slaPolicyIds/policyToTeam/teamIds/agg/ticketIds/surveyByTicket/ticketToAgent/loadSlaPolicyTeamMap/loadTeamNames/loadSurveyByTicket/TeamAgg.teamId 全 String 化）。**C1 核验**：md 7 文件面 = String 直传零 toLong 桥（md M1.1 已 String，如 mdPartnerBiz.findById(String)）；notify 8 文件面 = `notify(String,Map,ctx)` 签名不变零破坏（编译零错证实，与 M1.2 先例一致）。
- [x] Fix: A2 前向桥接 8 处落桥（D4 消费协议）——cs String id ↔ crm/qa Long API 的调用点加转换桥（`TicketAssignResolver` ×4（crm，findList 查询构造的 id 值语义桥——**eq/filter 值桥主动识别**，contract 037/038 先例：Long 列传 String 静默空匹配）+ `ErpCsTicketEscalateToQualityProcessor` ×4（qa，findList/save 的 nonConformance 值/实体字段桥）），每处登记 grep 例外清单（条目 id + file:line + 转换方向），退役 owner M3.4（crm 4）/M2.3（qa 4）；代码内 bridge 注释双向指针。
  - Skill: `nop-backend-dev`
  - **执行证据（A2 桥接例外清单）**：crm 4（退役 owner M3.4）——`TicketAssignResolver.java` import 区 bridge-main-053/054 注释（ErpCrmTeam/ErpCrmTeamMember 类型级引用，crm Long 实体）+ `:60` 附近 findList bridge-main-055 注释（**eq("code", VARCHAR) 过滤，零 id 值穿越，无需转换**）+ `:65-68` findList bridge-main-056 注释（`eq("teamId", matched.get(0).getId())` = crm 域内 Long↔Long 自洽值对（M3.4 翻转后 String↔String 仍自洽），零跨域类型穿越，无需转换）。qa 4（退役 owner M2.3）——`ErpCsTicketEscalateToQualityProcessor.java` import 区 bridge-main-057 注释（ErpQaNonConformance 类型级）+ `findQualityNcrs` bridge-main-058 注释（findList 弱指针 sourceType/sourceCode VARCHAR 过滤零 id 穿越）+ `createNcr` bridge-main-059 **实值转换桥 ×2**：`data.put("materialId", ConvertHelper.toLong(materialId))` + `data.put("supplierId", ConvertHelper.toLong(supplierId))`（cs String → qa ErpQaNonConformance Long 列，qa save-map 值桥）+ `findExistingNcr` bridge-main-060 注释（同 058 零 id 穿越）。PENDING 载荷 JSON id 改 String 形态落盘（buildPendingContent/parsePendingPayload asText），asLong 助手删除（去 Long.valueOf 残留）。
- [x] Fix: 自身链破坏处置（D4 carve-out）——no-am 口径下预期零外域破坏（7 模块全绿，reactor 不含外域模块；本域被引用面 = 零）；未登记破坏按路线图规则 6 停止回报；已登记破坏按中间态继续并履行登记义务。
  - Skill: `nop-backend-dev`
  - **执行证据**：7 模块 reactor（codegen/dao/meta/service/web/app/api）全绿，无外域模块参与；被引用面 = 零（Phase 1 已核 `import app.erp.cs` 外域零命中）→ 零外域破坏，未触发规则 6，无已登记破坏需处置。

Exit Criteria:

- [x] cs 7 模块链（显式列表、no-am、`-Dmaven.test.skip=true`）构建全绿（main 代码）；主代码修复清单 + A2 桥接例外清单在案

### Phase 3 - 测试修复 + A3 桥接适配 + 快照重录 + 域级测试

Status: completed（2026-08-22 执行记录见各项执行证据；前次中断运行已落大半测试代码/桥接/重录，本运行收口：修正超录快照 + 复跑全绿）
Targets: `module-cs/**/src/test/**`、`module-cs/erp-cs-service/_cases/**`
Skill: `nop-testing`

- Item Types: `Fix | Proof`
- Prereqs: Phase 2

- [x] Fix: 测试代码修复——26 个测试类的 Long 用法（字面量断言、helper 签名、seed `orm_propValueByName("id", id)` 形态——md/notify 先例），逐文件修复至测试编译通过；cs-web test-scope ast-dao（String 新 jar）/prj-dao（陈旧 Long jar）编译对象零 id 穿越核验（`ErpCsWebPagesTest` 治理排除但参与 test-compile）。
  - Skill: `nop-testing`
  - **执行证据（2026-08-22）**：26 测试类（`find src/test -name 'Test*.java'` = 26 实测吻合）String 化修复：种子常量 `Long→String`（如 `CUSTOMER_ID = "9401"`）+ seed/helper 签名 String 化 + `orm_propValueByName("id", "9301")` 字符串形态 + 断言字面量 String 化；数值接续常量改独立 String 常量（`TEMPLATE_ID_BASE_1 = "9302"` 等，避免 `String+1` 拼接语义漂移）；cs-web `test-compile` 绿 = test-scope ast-dao（String 新 jar）/prj-dao（陈旧 Long jar）编译对象零 id 穿越（`ErpCsWebPagesTest` 参与 test-compile，`@Tag("full-app")` 治理排除仅跳执行）。`mvn test-compile -pl module-cs/erp-cs-service,module-cs/erp-cs-web` BUILD SUCCESS。
- [x] Fix: A3 test 桥接适配（bridge-test-113..117，5 条）——本域测试引用 crm/qa 实体与 IBiz 的 id 形态桥接（String↔Long 局部转换或 mock 桩签名适配，与 Phase 2 桥接同型），适配后在登记册退役对应 test 桥接条目（owner M3.5 = 本计划）。
  - Skill: `nop-testing`
  - **执行证据（A3 桥接清单）**：bridge-test-113（`TestErpCsCatalogFulfillmentEngine.java:72` 注释 + CRM_TEAM_ID 保持 Long + `seedMember(..., Long id, Long teamId, ...)` crm seed 局部桥）+ bridge-test-115（`TestErpCsTicketCreateEnrichment.java:75` 同型）——crm seed 走 Long 实体侧自洽，cs String 侧经转换桥接（退役 owner M3.4）；bridge-test-114/135（`TestErpCsQualityEscalation.java:66` 注释 + `MATERIAL_ID_LONG = 8301L` 断言侧 Long 桥 `assertEquals(Long.valueOf(8401L), data.get("supplierId"))` 对 qa save-map Long 值）——退役 owner M2.3；bridge-test-116/136（`TestMockCrmBizModels.java:23` 类注释——mock 桩保持 crm Long 实体/IBiz 签名，含 `app-test-mock-crm.beans.xml:11/:15` ioc:type FQN）+ bridge-test-117（`TestMockQaBizModels.java:26` 类注释——mock 桩保持 qa Long 签名（`submitReview(Long ncrId, ...)` 等 7 方法），含 `app-test-mock-qa.beans.xml:11`）——mock 桩与未迁移域 jar 签名一致，cs 侧断言经局部转换消费。登记册退役操作归 Phase 4 Add 项统一落盘。
- [x] Fix: C2 后向 test 适配——md 8 + notify 6 测试文件对 String 化 API 的适配（M1.1/M1.2 登记的 successor 义务兑付）。
  - Skill: `nop-testing`
  - **执行证据**：backward-203 md 8 文件（TestErpCsCannedResponseBiz/CatalogFulfillmentEngine/Entitlement/MultiLevelEscalation/ServiceCatalog/SlaNotification/TicketCreateEnrichment + job/TestErpCsSurveySendJob）+ backward-204 notify 6 文件实测全 String 形态（如 `TestErpCsCannedResponseBiz` `PARTNER_ID = "9201"` String 直传 md String jar、notify 调用签名不变），编译 + CHECKING 全绿证实适配完成（successor 义务兑付，登记册状态更新归 Phase 4）。
- [x] Fix: 快照每域重录（用户裁决固定步骤）——`RECORDING` 模式运行 cs service 测试 → 逐案审核 `_cases/` 新形态（633 文件基线；id 以 String 形态落盘；非确定性单元格按 aps/contract 先例 `*` 通配修正）→ 注解还原（grep 零 RECORDING/forceSaveOutput 残留）→ 切回 `CHECKING` 复跑确认全绿。重录足迹（内容 diff vs 新增落盘分列）与审核结论记录本计划。
  - Skill: `nop-testing`
  - **执行证据（重录足迹与审核结论）**：**内容 diff = 80 个 tracked 文件**（种子/输出表重录，id String 形态落盘——CSV 文本形态 Long/String 同形，schema 头随实体演进出新列如 LAST_ESCALATION_LEVEL）；**新增落盘 = 7 个文件**（均为既有录制测试的 hook 新捕获 input 种子表：TestErpCsServiceCatalog ×2 + TestErpCsTicketSlaCsat ×5，`erp_sys_notification_template.csv`/`erp_cs_ticket_action.csv`，确定性种子）；**超录回退 = 81 个方法目录的新录 input/output 整体移除**——前次中断运行对 HEAD 期「断言式测试 + 空 autotest.yaml 标记」范式（R1.65/R1.68/R1.70/R1.71 类注释自述：TK 编号/审计时间含真实时钟，录制表快照会随日期漂移翻红）的 7 类测试（CatalogFulfillmentEngine/KnowledgeAdoption/MultiLevelEscalation/QualityEscalation/TicketCreateEnrichment/TicketTimerSession/job/SurveySendJob/probe/statemachine ×2/ServiceCatalog 部分 = 81 方法目录）误录了快照，其中 `nop_sys_sequence.csv` 含月度序列名 `cs_ticket_code_seq_202608`（下月即漂移翻红）+ 尾随空格单元格（`标记解决: `/`工单 ... 履行事件 `）CSV 回读裁剪致 7 例 check-match-fail——按已提交范式回退该等新录（保留空 autotest.yaml 标记态），消除漂移地雷与既有 7 失败；`_cases` 终态 = 640 文件（633 基线 + 7 合法新增）。注解还原核验：`rg 'RECORDING|forceSaveOutput' src/test` 零命中；CHECKING 复跑 185/185 绿。
- [x] Proof: `mvn test -pl module-cs/erp-cs-service,module-cs/erp-cs-web`（D3 口径：不带 `-am`）全绿——service 26 测试类 + web BUILD SUCCESS（`ErpCsWebPagesTest` 治理排除，0 tests 预期）。若复现平台 IoC 回归，按先例修复（test-scope VFS delta + DeltaOverride delta-layer 补 default 层集）并登记。
  - Skill: `nop-testing`
  - **执行证据**：`mvn test -pl module-cs/erp-cs-service,module-cs/erp-cs-web` BUILD SUCCESS——service `Tests run: 185, Failures: 0, Errors: 0, Skipped: 0`（26 测试类）+ web `Tests run: 0`（`ErpCsWebPagesTest` @Tag("full-app") surefire excludedGroups 治理排除，successor M4.1）。平台 IoC 回归未复现（前次中断运行已落 test-scope VFS delta：`src/test/resources/_vfs/_delta/default/nop/sys/beans/app-dao.beans.xml` + `_delta/test-cs-delta/erp/cs/beans/app-service.beans.xml`，`nopSequenceGenerator` self-wait 先例修复在位，本运行全绿证实有效——Deferred 登记维持 watch-only）。

Exit Criteria:

- [x] cs 域级测试全绿（service 26 类 185/185；web 治理排除偏差登记）；快照重录完成且 `CHECKING` 复跑通过；重录清单在案

### Phase 4 - 语义陷阱 grep 门控 + page.yaml Fix + 收尾登记

Status: completed（2026-08-22 执行记录见各项执行证据）

Targets: `module-cs/**`（手写代码 + cs-web 手写 page.yaml）、`docs/backlog/id-string-migration-roadmap.md`、`docs/logs/2026/{08-22 或执行日}.md`、`tools/id-migration-registry.json5`
Skill: none

- Item Types: `Proof | Fix | Add`
- Prereqs: Phase 3

- [x] Proof: 语义陷阱 grep 门控（路线图横切 §3，cs 手写 main+test 范围）清零——`\.longValue\(\)`、`Long\.parseLong\(`、`Map<Long`、`Set<Long`、`String\.format\("%d` 及 `%d` 变体零命中（A2 桥接转换点为登记例外，逐条列于例外清单并标注退役 owner M3.4/M2.3）；Long 装箱 `==`/`!=` 比较（id 上下文）逐条核清；id 序比较陷阱（String 字典序）专项 grep：`getId\(\)\s*[<>]|comparing.*getId`；残留 `Long` 逐条判定合法非 id 或登记 successor；sql-lib.xml 仓内零存在（注明即可）。结果逐项记录本计划。
  - Skill: none
  - **执行证据（逐项）**：`.longValue()`/`Long.parseLong` = 1 命中 `TestErpCsTicketTimerSession.java:441` `agg()` helper——**合法非 id**（聚合查询数值结果（时长/计数）Number→long 转换，参数 ticketId 已 String）；`Map<Long`/`Set<Long` = 0；`String.format("%d`/`%d` 变体 = 0；id 序比较专项（`getId() [<>]`/`comparing.*getId`）= 0；装箱 `==`/`!=` = 8 处全为 null 检查（ErpCsReportBizModel:201/:207、ErpCsQualityDashboardBizModel:205/:214/:228、ErpCsCannedResponseBizModel:226/:237）合法；残留 `Long` = A2/A3 登记例外面（crm/qa mock 桩 Long 签名 + seed/断言 Long 常量，退役 owner M3.4/M2.3，Phase 2/3 例外清单在案）+ `TestErpCsCsatReminderJob.newSurvey(Long)` 就地 String 化（`newSurvey("201"/"202")`，免登记）；sql-lib.xml = module-cs 仓内零存在。
- [x] Fix: cs-web 手写 page.yaml raw-GraphQL `:Long` 变量 12 处就地 String 化——`ErpCsTicket/kanban.page.yaml` ×11（`$id:Long` ×5 → `:String`（ticketId mutation 变量）+ `$c:Long` ×6（:44/:107/:155/:213/:271/:300）→ `:String`（filter_customerId 查询变量））+ `ErpCsTicketAction/timeline.page.yaml:36`（`$tid:Long` → `:String`）；`|| 0` 类 Int 字面量兜底改 `|| ''`（若存在）、variables 链与 options value 链一致性核证（contract version-diff 先例）；随后 `rg ':Long' module-cs/erp-cs-web/src/main/resources/_vfs --glob '!**/_gen/**'` 清零（非 id 类型变量如 `$lim:Int`/`$s:String` 合法保留并逐条判定）；cs-web 重建 BUILD SUCCESS 验证。
  - Skill: none
  - **执行证据**：kanban ×11（`$id:Long` ×5 = assign/start/resolve/close/reopen mutation + `$c:Long` ×6 = :44/:107/:155/:213/:271/:300 filter_customerId）+ timeline :36 `$tid:Long` 全部 `:Long→:String`，git diff 精确 12 行 query 语句（perl 逐 pattern 替换；本机 Edit 工具对 `${'$'}` 模板内容替换出现文件膨胀损坏，检出后 `git checkout HEAD --` 还原改用 perl，diff 复核仅 `:Long→:String` 单字符类变更）；`|| 0` 兜底不存在（variables 全为 `${customerId || null}`/`${ticketId || null}`/`${id}` String 表达式，与 String 类型一致——contract version-diff 先例核证口径）；`rg ':Long' …_vfs --glob '!**/_gen/**'` = 0 命中；非 id 变量判定：`$lim:Int`（limit 分页数）/`$s:String`（status 字典）/`$a:String`（assignedToId VARCHAR 列）合法保留；cs-web 重建 = 7 模块链 `mvn clean install … -Dmaven.test.skip=true` BUILD SUCCESS（7/7，含 cs-web）。
- [x] Proof: 手写 view.xml 零改动验证——`git status module-cs/erp-cs-web` 确认无手写 view 文件被动变更（生成 view 随 codegen 更新不在此列；page.yaml 修复 diff 为本计划主动变更）。
  - Skill: none
  - **执行证据**：`git status --porcelain module-cs/erp-cs-web` = 20 M——18 个 `_gen/_*.view.xml`（codegen 随动，预期）+ 2 个手写 page.yaml（kanban/timeline，本计划主动 Fix），零手写 view.xml 被动变更。
- [x] Add: 登记册状态更新——A3 test 桥接 5 条（bridge-test-113..117）status → retired（owner M3.5 兑付 note：转换点/读路径核验结论）；A2 main 桥接 8 条保持 active（退役 owner M3.4（crm 4）/M2.3（qa 4），代码内 bridge 注释双向指针）；被引用面确认 = 零（无 successor 义务）。
  - Skill: none
  - **执行证据**：json5 权威 7 条 → retired（bridge-test-113..117 + Phase 1 补登 135/136，各条 note 附转换点形态 + 读路径核验（185 测试全绿）+ 晚域翻转回收指针；retired 总数 23）；登记册消费工具 fail-closed 解析验证通过（`check-bigint-id-types.mjs dry-run` 正常消费 + dry-run 显示 cs 段 0 待改列 = 1148−68）；审计文档 §6.8 增「A3 退役记录」段（7 条兑付形态 + A2 保持 active + backward-pointer 口径说明）；A2 main 8 条（053..060）保持 active；被引用面 = 零（Phase 1 核证 `import app.erp.cs` 外域零命中，无 successor 义务）。
- [x] Add: owner doc 注记——grep `docs/design/customer-service/` 中关于 cs id 为 Long/数字的陈述；存在则就地注记 Java 层已 String 化（引用本计划），不存在则记录「零 Long id 陈述，零文档变更」结论。
  - Skill: none
  - **执行证据**：grep 命中 3 处 → 就地注记（引用本计划）：`time-tracking.md:287`（`agentId` BIGINT/0 哨兵机制——注记 String 化 + D2 0 哨兵已退役）+ `sla.md:30`（escalationUserId/secondEscalationUserId BIGINT(long)——注记 `stdDataType="string"`）+ `sla.md:352`（escalationUserId 类型陈述——同注记）；其余 7 文件零 Long id 陈述。
- [x] Add: 路线图 M3.5 → `done`（M2/M3 表位次 8 + 头部「最后更新」；本批三计划收口——位次 9 hr 起归后续批次）+ 日志条目（含验证状态）。
  - Skill: none
  - **执行证据**：roadmap 位次 8 行 `todo→done`（四 Phase 证据摘要，同表先例行风格）+ 头部「最后更新」前置 M3.5 条目（位次 9 hr 解锁 + 本域要点：page.yaml 12 处本批最重页面修复面 / A3 7 条退役 / 81 目录超录回退）；日志 `docs/logs/2026/08-22.md` 顶部新增 M3.5 条目（含验证状态全绿：7 模块链 BUILD SUCCESS + 185/185 + dry-run 0 残留 + 中间态 successor 清单）。

Exit Criteria:

- [x] grep 门控零残留（例外逐条核清 + 桥接例外清单在案）；page.yaml `:Long` 清零 + cs-web 重建绿 + view 零被动变更在案
- [x] 路线图状态、登记册退役（7 条）、日志三者一致

## Draft Review Record

- Independent draft review iteration 1（2026-08-22，双独立子 agent fresh session）：
  - 审查者 A（技术/执行视角 plan-audit，ses_fdaecac03ffezjlpdWu00j4cJW）：`needs revision` — 0 BLOCKER / 2 MAJOR / 4 MINOR。事实核对大部分属实（68 = 66+2 精确；A2 调用点行级核验；A3 5 文件存在；C1/C2 rg 精确；26/633；page.yaml 12 处；**`import app.erp.fin` main+test 双零命中证实 pom 级主张**；冻结序依赖吻合）。**MAJOR-1**：A2 拆分「crm 5 + qa 3」错误——登记册 §6.8/json5 为 **crm 4（053-056）+ qa 4（057-060）**，错误拆分传播至 Goals/Phase 4 退役指令/Deferred 后续 owner 记账。**MAJOR-2**：fin-service 在 cs pom 为 **test-scope**（pom 注释「主类 DAG 不含 finance，测试期引入仅取字体资源」）非 main compile 依赖——依赖模型陈述与实仓 pom 矛盾（main 零 import/编译中性结论仍成立）。MINOR：③ kanban `$c:Long` 为 ×6 非 ×5（`$id:Long` ×5，合计 11 不变）；④ cs-web md-service test-scope；⑤ orgId ×17 XML 口径（真实 13）；⑥ 回滚 test-compile 陷阱。
  - 审查者 B（治理/规范视角，ses_fdaec67abffet95xJrKGrvjzSu）：`needs revision` — 0 BLOCKER / 1 MAJOR / 3 MINOR。**MAJOR-1**：owner-doc 指针 `docs/design/cs/` 实仓不存在——cs 设计文档位于 `docs/design/customer-service/`，如按错误路径执行 Phase 4 grep 将产出虚假「零陈述」结论使 owner-doc 对齐门控失效。MINOR：① A2 拆分 crm4+qa4（同技术侧）；② FQN regex 漏晚域 hr/mfg/drp/log；③ ast-dao「陈旧 Long jar」与批内序 2 install 后事实矛盾（措辞精度）。
  - **修订（iteration 1 → 2，已落地）**：全部 2+1 MAJOR + 7 MINOR 已处理——A2 拆分全篇改 crm 4 + qa 4（基线/Goals/Phase 1/Phase 4/Deferred 五处）；fin-service 改 test-scope 依赖模型（含 pom 注释引证 + 冻结序 test 边口径说明）；owner doc 路径改 `docs/design/customer-service/`（Task Route + Phase 4 两处）；kanban `$c:Long` ×6/$id ×5；cs-web md-service test-scope；orgId ×13；回滚 `-Dmaven.test.skip=true` + 阶段差异；FQN regex 覆盖全部未迁移晚域；ast-dao String 新 jar / prj-dao 陈旧 jar 分列措辞。
- Independent draft review iteration 2（2026-08-22，双审查者同会话复审）：双审查者 `passes draft review`（0 BLOCKER / 0 MAJOR）。全部 iteration 1 发现核实解决：A2 拆分 crm 4 + qa 4 六处一致（live-verified 吻合登记册 §6.8/json5）；fin-service test-scope 依赖模型（pom 注释 verbatim）；owner-doc 路径 `docs/design/customer-service/`（目录存在性证实）；FQN regex 覆盖全部未迁移晚域；orgId ×13/回滚标志/ast-dao String 新 jar 措辞全清。唯一遗留：Phase 4 line 131 `$c:Long` ×5 计数标签陈旧（baseline 已正确 ×6；文件合计 ×11 与 rg 清零门控使漏执行不可能）——按 plan 1「批准后 MINOR 修订注记」模式于转 active 时同步（已落地 ×6 + 行号清单）。
  - **双独立子 agent 批准（保护区域 `model/*.orm.xml`，`ai-autonomy-policy.md` `auto + dual-agent-approval`）**：
    - 批准 1（技术视角）：ses_fdaecac03ffezjlpdWu00j4cJW，2026-08-22 — 「批准 M3.5 cs orm 保护区域变更（技术视角批准）」。依据：68 列 = 自有 66（PK 18 + FK 48）+ md stub 2 与 md 权威源对齐，零登记册延后、零跨域关系边、零被引用面，D3 no-am 7 模块与 D4 桥接 disposition 经登记册/pom 实证。
    - 批准 2（治理视角）：ses_fdaec67abffet95xJrKGrvjzSu，2026-08-22 — 「批准 M3.5 cs orm 保护区域变更（治理视角批准）」。依据：owner-doc 门控缺陷已解决（双路径指向真实目录）、68 列表面精确有界且机器门控、五要素证据链与文件内双批准门控完整、登记册 §6.8 消费条目准确。
- 共识达成（2026-08-22）：iteration 2 双审查者 0 BLOCKER / 0 MAJOR + 保护区域双批准 → 计划转 `active`。

## Closure Gates

> 完整仓库验证定制为域级口径（路线图规则 3 D3 修订：禁止以全量构建为中间 gate；全量构建仅存在于 M4.1）。

- [x] 范围内行为完成（68 列落源 + no-am 7 模块重生成 + 手写代码/测试修复 + A2 落桥与 A3 退役 + 快照重录 + grep 门控清零 + page.yaml 12 处 Fix）
- [x] 相关文档对齐（owner doc 注记结论（3 处）、路线图 M3.5 状态、登记册退役（A3 7 条 = bridge-test-113..117 + 135/136）、日志）
- [x] 已运行验证：`mvn clean install -pl module-cs/erp-cs-{codegen,dao,meta,service,web,app,api} -DskipTests` 全绿 + `mvn test -pl module-cs/erp-cs-service,module-cs/erp-cs-web` 全绿（185/185 + web 0 tests 治理排除）+ 工具重扫零残留（cs 段 `NEEDS FIX` = 0，dry-run 待改列 1148→1080 = cs 68 列已落源）
- [x] 无范围内项目降级为 deferred/follow-up（web 页面测试治理排除为已提交决策 + M4.1 successor 登记，属偏差登记而非范围降级；81 方法目录超录回退为恢复已提交「断言式 + 空 autotest.yaml」范式，非范围降级——见 Phase 3 执行证据）
- [x] 保护区域双独立子 agent 批准记录落盘（Phase 1 前置）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（审计会话 ses_fd95f274cffe2Rcj70ZcAn1BgW，2026-08-22，CLOSURE AUDIT: PASS）
- [x] 结束证据存在于文件中（见 Closure Audit Evidence）

## Deferred But Adjudicated

### A2 main 桥接 8 处（crm 4 + qa 4，String↔Long 临时转换）

- Classification: `watch-only residual`
- Why Not Blocking Closure: D4 登记册预先登记的中间态桥接——crm（位次 17）/qa（位次 13）未迁移，桥接点为编译必需
- Successor Required: `yes`（M3.4 回收 crm 4 条；M2.3 回收 qa 4 条——晚域翻转时退役条目并移除本域桥接点）

### `ErpCsWebPagesTest` 页面校验

- Classification: `watch-only residual`
- Why Not Blocking Closure: `@Tag("full-app")` + surefire excludedGroups 为先于本 mission 的已提交治理决策（plan 2026-07-24-0930-1），实证依赖全量 classpath
- Successor Required: `yes`（M4.1 app-erp-all `ErpAllWebPagesTest`）

### 平台 IoC 回归 delta（若 Phase 3 复现并落盘）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 平台 `nopSequenceGenerator` bean-init-self-wait 为已登记平台 Bug，先例修复 = test-scope VFS delta
- Successor Required: `yes`（平台修复后统一移除，M4.1 复核）

## Closure

Status Note: 执行完成（2026-08-22）。Phase 1-2 前轮执行完毕；Phase 3-4 本轮收口：26 测试类 String 化 + A3 桥接 7 条兑付 + C2 后向兑付 + 快照重录（80 内容 diff + 7 新增种子表 + 81 方法目录超录回退——前次中断运行误录「断言式 + 空 autotest.yaml」范式测试，含月度序列名漂移地雷与尾随空格 CSV 回读裁剪 7 处失败，按已提交范式回退）+ grep 门控清零 + page.yaml 12 处 String 化 + 登记/路线图/日志/owner doc 收尾。验证全绿：7 模块链 `-Dmaven.test.skip=true` install BUILD SUCCESS + `mvn test` 185/185（web 0 tests 治理排除）+ dry-run 工具重扫 cs 段 0 残留。

Closure Audit Evidence:

- 独立结束审计（新会话子代理 ses_fd95f274cffe2Rcj70ZcAn1BgW，2026-08-22）：**CLOSURE AUDIT: PASS**——7 项核证全 PASS：① ORM 68 列翻转精确（剩余 18 long 全为 delVersion 族，stdSqlType BIGINT 保留，PK/FK/md stub 抽查全 string）；② main 代码 grep 零残留 + A2 桥接注释 053..060 在位（ConvertHelper.toLong ×2 实证）；③ page.yaml `:Long` 清零（kanban 317 行无损坏，`$id:String` ×5 + `$c:String` ×6 + timeline `$tid:String`）；④ 登记册 7 条 retired + bridge-main-053..060 保持 active（retired 总数 23）；⑤ 路线图/日志/owner doc/审计文档 §6.8 全对齐；⑥ 计划内部一致（Phase 1-4 全 [x] + 4 × Status: completed + Exit Criteria 全 [x]）；⑦ 测试静态健全（零 RECORDING 残留、零月度序列漂移地雷、mock 桩 Long 签名保留）。非阻塞注记 1 条（Closure Gates「5 条」陈旧计数 → 已修正为 7 条）。

Follow-up:

- （无范围内跟进项；已确认缺陷不得出现在此处。中间态 successor 指针见 Deferred But Adjudicated 与 Phase 4 登记记录。）
