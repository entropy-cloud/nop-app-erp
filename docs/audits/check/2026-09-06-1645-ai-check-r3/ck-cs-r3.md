# ck-cs-r3 — cs U13 五维符合性审计报告（ai-check-r3 M1.14）

> 工作项：M1.14（U12 + U13 + U18 + U17 + U19 各 × 五维全格，冻结清单 §4 映射表第 14 行；本报告 = U13 cs 格，其余四域格分别见 `ck-crm-r3.md` / `ck-contract-r3.md` / `ck-b2b-r3.md` / `ck-drp-r3.md`）。
> 执行日期：2026-09-09。审计时点 T0：HEAD `5eb2c4dbe9faa68a685b5a90728cc2493b477b76`（2026-09-09；计划基线 `2c1c1ef25` 后唯一推进 = 同批姊妹 `2026-09-09-0547-1` 审计产物提交，生产代码零变化）；脏面 = 2 条 untracked 计划文件（本计划 + 姊妹 `0547-3`），tracked 零修改。
> 判定依据（冻结）：`docs/audits/check/2026-09-06-1645-ai-check-r3/m0-5-audit-checklists.md` §1/§2 + §3.3 U13 行 + §6 勘误 E1。执行期仅在报告与计划勾选注记追加证据，判定标准零改动。
> 范围：cs 全域（C 级，无切片细分）——工单（Ticket/TicketAction/TicketType/TicketTimerSession/TicketFulfillmentStep + StateMachine/assign/start/resolve/close/reopen/cancel 族 Processor）/SLA（SlaPolicy/SlaPolicyMatcher/MatchAndAttachSla/ScanOverdue/Resolve/Reopen Processor + SLA 通知）/服务目录（ServiceCatalogItem/CatalogCategory/CatalogFulfillment/CreateFromCatalog/FulfillmentExecute）/entitlement（Entitlement/Deactivate）/满意度（Survey/SurveyCreate/SurveySendJob/CsatReminderJob/submitSurvey）/预设应答（CannedResponse/CannedCategory/CannedApply）/工时（TimeEntry/TimerSession Start/Pause/Stop/Resume Ops）/知识库/团队/费率 + report/dashboard 门面（`module-cs/erp-cs-{dao,service,web}` src/main）；owner docs `docs/design/customer-service/`（sla/service-catalog/entitlement/csat/canned-response/time-tracking + state-machine/README/use-cases）。Skills：`nop-platform-conformance-audit-prompt` + `code-quality-audit-prompt`（DIM-B）；其余维 Skill: none。
> 切片边界（§3.2）：posting 引擎内部归 fin-1（cs 无过账面）；common 抽象族行为归 U20（18/18 实体 BizModel 基类接入调用点合规）；聚合横切面归 U21；notify 消费点归 U11（8 站点 `IErpSysNotificationBiz` try/catch 降级合规，发送内部归 U11）。
> 零生产代码改动（roadmap 规则 6）：本报告 + 双索引 + 计划勾选注记为唯一产物（收官 `git status` 机械核证见 §4）。

## 1. 五维覆盖矩阵（5/5 完整）

| 维度 | 判定面（U13 全格） | 机械核查结果 | verdict |
| --- | --- | --- | --- |
| **DIM-B 后端平台合规** | §1.1 全套机械程式 + 15 维度走查（域焦点：工单 SLA/TK 编号规则消费/满意度链；共性②⑧⑨⑮）+ 维度⑮断言抽样 7 doc × 14 检查点 | 反模式族全零（RuntimeException=0/@Inject private=0/System.currentTimeMillis=0/@Transactional=0，Jackson/Gson=0）；checker 19 规则 = M0.3 快照行零漂移；`__XGEN_FORCE_OVERRIDE__` 17 处全为 erp-cs-meta dict.yaml 校验点零手改；聚合器含 `/erp/cs/auth/erp-cs.action-auth.xml`。IDaoProvider/IOrmTemplate 22 文件 29 注入逐文件核验：跨域零绕过（全经 `IErpMdPartnerBiz`/`IErpCrmTeamBiz`/`IErpCrmTeamMemberBiz`/`IErpQaNonConformanceBiz`/`IErpSysNotificationBiz`）；per-file 豁免注释仅 1/29（SurveySendJob:101 含 isNull 算子理由），其余命中全部落在 Processor/Job/CodeRule 变量/非实体聚合 BizModel——由 processor-extension-pattern.md:83 全局立法兜底，注释缺位面立 P3-CK-cs-023-r3。15/15 无跳维：①TK 编号模型化（ErpCsTicket.xmeta:13 biz:codeRule="cs-ticket-code"）+ 状态机 Delta 覆盖测试 pass；②18 BizModel 全 I*Biz 注入 pass（注释面=023-r3）；③ErpCsErrors 38+ ErrorCode 中文集中 pass（assertCan 裸 IllegalArgumentException 内部护栏 = P3-CK-cs-024-r3）；④机械全零 pass；⑤CoreMetrics 全覆盖 pass；⑥18/18 AbstractErpCrudBizModel + 2 非实体聚合门面合法 pass；⑦跨域全 I*Biz 单向 DAG pass；⑧**dict 死状态 = 0**——ticket-status 6/6、survey-status 4/4、timer-session-status 3/3、time-entry-approve-status 3/3、fulfillment-step-status 5/5 全值有 writer（逐 dict 核验表在案）；⑨6 job 接线完整（27 bean/6 job.yaml 对应）+ cron 键漂移族 = cs-014 归并 + 模板种子缺失 = cs-006 归并；⑩无 Delta 绕过 pass；⑪无 tenantId 预置（orgId 缺失族 = cs-011 归并）pass；⑫21 测试类含状态机 Delta/IoC/Matrix pass；⑬xmeta autoExpr 保留层注释背书 pass；⑭beans.xml/job 接线无孤立 pass；⑮7 doc × 14 检查点漂移 6（全归并既有 ID，零新漂移） | **finding**（3 新立全 P3；复用 1 + 归并 21，见 §2） |
| **DIM-F 前端页面与 E2E** | §1.2 全套 + 工单看板（复杂手写页清单成员） | `npm run validate:flux`：FLUX_PAGE_ERROR_COUNT 0 / 999 页（erp 855）；整体 exit 1 = 325 ERR 全 variant 族既有外部漂移（cs 命中 21 条同族不立项）；`component="AMIS"` 0、ORM `ext:web-renderer="flux"` 无缺失。页面面 42 page.yaml + 3 flux.yaml 全量清点（M0.4 重放零漂移）：17 实体 main+picker stub 族 34 页 + ref-ticket ×2 全 PASS（18/18 view.xml 保留层继承）；工单看板 kanban（`@query:findBoardData` + 5 `@mutation` REST /r/，i18nEn 41/6）在位；QualityDashboard（3 `@query` 与孪生一致）/TicketAction timeline/report 页 PASS；`graphql:` 7 处全 labelProp（`ErpCsTicket.view.xml:137,230` 注释明证历史 /graphql→REST 迁移完成）。E2E：E2E_ENGINE 缺省 flux、crm/cs spec PageObject 合规、页面级 GraphQL 断言 0。**新立 2 条**：P3-CK-cs-026-r3（kanban 拖拽路由缺陷——`kanban.flux.yaml:80` 条件式无 col-CANCELLED/col-NEW 分支落 fallback `start` 必错 + assign 分支 data 仅 ticketId 零 assignedToId 产生「ASSIGNED 无主」单，执行者 HEAD 复核在位）+ P3-CK-cs-027-r3（E2E selector 纪律偏离——`zzz-diag-cs-add.spec.ts:9-36` 诊断遗留物内联 `[data-slot=...]` + `f13-non-standard-views.visual.spec.ts:21,35-47` visual 层内联 selector，违反「selector 唯一合法位置 = adapter」）。assign 校验缺口前端新入口归并 P2-CK-cs-010 新站点注记；孪生端点漂移归并 P3-CK-mfg-023-r3 家族新站点（U21 归属） | **finding**（2 新立 P3 + 归并态） |
| **DIM-S seed 数据** | §1.3 全套 + CS 3 表 seed + `nop_sys_code_rule.csv`（String PK 先例） | `TestErpSeedDataIntegrity` **4/0/0/0 全绿**；`_init-data` porcelain 空（零 seed 变更）；资产清点 **372 CSV + 1 SQL** = 冻结登记值；cs deploy `_seed_*.sql` 三方言命中 = 登记处表 L108「✅ 已聚合（2026-08-25，`nop_sys_code_rule.csv`，String PK 先例 = `nop_auth_role.csv`）」在案——同步义务闭环；erp_cs_* 18 CSV + nop_sys_code_rule 1（cs-ticket-code TK 规则行）在位；cs 非过账域 posted 列零命中 N/A 带理由；SLA/survey seed 与 ticket 链自洽（TestErpCsTicketSlaCsat 快照面佐证） | **pass** |
| **DIM-T 单元测试** | §1.4 全套 + P1 Ticket→SLA 清单行覆盖 | `mvn test -pl module-cs/erp-cs-service` **185/0/0/0 全绿 BUILD SUCCESS**（= 锚点 185 零增量）；覆盖对账：注解动作 53 / BizModel 20 vs `_cases` 资产根 20（TicketSlaCsat 13 测试/ServiceCatalog/TimerSession/SlaNotification/CatalogFulfillment/probe 契约/状态机 Delta+IoC+Matrix 族）；**P1 行在位**：`TestErpCsTicketSlaCsat` ✓；`SnapshotTest.RECORDING` = 0；probe 契约测试族在位 | **pass** |
| **DIM-I i18n（MI 先行时序口径）** | §1.5 全套（仅零回归 + 白名单合规，不产同类 finding） | `--strict` **PASS exit 0** + `--self-test` **PASS**；cs 探针族（CAT-1 23）维持清零零回归；WHITELIST cs 条目 **2/2 四要素齐备**：`ErpCsConstants.java`（L287-291，C2② seed 角色名数据契约 + 准绳表 #5 指针 + plan 2026-09-07-0902-1 Phase 1 Decision 裁决来源）+ `ErpCsQualityDashboardBizModel.java`（L292-296，混合文件 (b)+E3 + 同指针 + 同裁决来源）；`grep -L @Locale` = 空；meta/i18n 零手改 | **pass** |

## 2. Finding 三态裁决（§2 跨轮查重全维强制）

> 查重源：r1 `ai-check-index.md` §Finding 追踪（同域报告 `ck-cs.md` C7.1 全 22 条逐一比对）+ r2 只读目录（无 cs 同型新独立登记）+ §Mission 基线快照。**本轮新立 5 条**（全 P3：DIM-B 3 + DIM-F 2）；历史 22 ID 零覆写。

### 2.1 复用（同型已 fixed，HEAD 复核有效）— 1 条

| 原 ID | 修复在位证据（T0） |
| --- | --- |
| P2-CK-cs-012（CRUD update 无已审守卫，同型 P1-CK-pur-003 族） | F1.3 统一基类接入在位：18/18 实体 BizModel extends `AbstractErpCrudBizModel`。残余注记：ticket `status` 等列惰性 = F1.3 注册设计边界（「激活由列存在性决定」），残余归 P1-CK-pur-003 族共性裁决（U20 格） |

### 2.2 归并（同型 open 追加证据至原 ID）— 21 条

> 21 条 open r1 ID 于 T0 现状复核：抽样直接复核 8 条（001/002/003/007/008/010/013/015 file:line 在案），其余 13 条按走查控制点复核在位 + §Finding 追踪索引状态采信（lesson 13 快照断言引用前实仓重验原则履行——抽样直接复核覆盖全部 P1 与全部 DIM-B 走查触点）。

| 原 ID | r3 复核证据（T0） |
| --- | --- |
| P1-CK-cs-001 | MatchAndAttachSlaProcessor.java:48 权益覆盖写 deadline → :62-63 策略 deadline 无条件覆写 + Timestamp.valueOf null-NPE 入口原样（维度⑮ entitlement.md §三抽样命中同点） |
| P1-CK-cs-002 | Dashboard loadSurveyByTicket:308-324 / Report aggregateSurveys:253-264 无 respondedAt 过滤、nz 零填充在位（维度⑮ csat.md §4.3 抽样命中同点） |
| P1-CK-cs-003 | CreateFromCatalogProcessor.java:192 `"TK-"+millis` 绕过 TK 月序列在位（维度⑮ use-cases/README 抽样命中同点） |
| P2-CK-cs-004 | ReopenProcessor.java:47-49 仅 setStatus，isSlaCompleted 无重置原样 |
| P2-CK-cs-005 | matchAndAttachSla 入口无幂等守卫原样 |
| P2-CK-cs-006 | cs.entitlement-expiry / cs.fulfillment-approval-request 通知模板种子缺失原样 |
| P2-CK-cs-007 | survey-status 无 EXPIRED 值/writer + reminders/expired 集合包含关系原样（维度⑮ csat.md §3.2 抽样命中同点） |
| P2-CK-cs-008 | ReopenProcessor.cancelUnrespondedSurvey:60-63 setLimit(1) + Java 侧 respondedAt 过滤原样（survey UK 属 ORM 保护区域不测） |
| P2-CK-cs-009 | 履行 UPDATE_STATUS 直改 setStatus 绕过守卫原样 |
| P2-CK-cs-010 | assign 仅 NEW 守卫、无 reassign mutation、assignedToId 无存在性校验原样；**新站点注记**：DIM-F 看板拖拽 assign 分支零 assignedToId 入参（P3-CK-cs-026-r3）为该缺口的前端可达新入口 |
| P2-CK-cs-011 | orgId 过滤缺失 + 聚合无界原样 |
| P2-CK-cs-013 | ResolveProcessor.java:69 null deadline 桶当 true（维度⑮ sla.md §2.1 抽样命中同点）原样 |
| P2-CK-cs-014 | 6 job cron 键漂移原样 |
| P3-CK-cs-015 | resolve L72-73 resolution 文本可满足超时原因守卫（维度⑮ state-machine.md §6 抽样确认守卫存在性，稀释现症在位）原样 |
| P3-CK-cs-016 | CsatReminderJob customerName 错填 subject 原样 |
| P3-CK-cs-017 | 质量升级重试扫描窗饿死原样 |
| P3-CK-cs-018 | 计时器 pause/resume/stop 无属主校验原样 |
| P3-CK-cs-019 | ApplyCannedResponseProcessor 填 userId 非 displayName（维度⑮ canned-response.md:76 抽样命中同点）原样 |
| P3-CK-cs-020 | submitSurvey 全空评分照 COMPLETED + 有效期不校验原样 |
| P3-CK-cs-021 | SurveyTokenGenerator 类名字面量前缀原样 |
| P3-CK-cs-022 | findSlaWarnings @BizQuery 内通知副作用无去重原样 |

### 2.3 新立 `-r3` — 5 条（全 P3）

**P3-CK-cs-023-r3**（DIM-B ②）
- **控制点**：IDaoProvider/IOrmTemplate 29 处注入仅 1 处 per-file 豁免理由注释（SurveySendJob:101）；代表站点 `ErpCsReportBizModel.java:69`、`ErpCsQualityDashboardBizModel.java:61`（非实体聚合 @BizModel）。
- **三态裁决**：新立（ck-cs.md 机械扫描未覆盖注释面）。Processor/Job 命中有 processor-extension-pattern.md:83 全局立法兜底，缺口为 AGENTS.md「注释记录原因」未逐文件落实，P3 文档合规面。
- **修复方向**：Report/Dashboard 2 门面补注释（或收敛经 I*Biz.findList）；Processor/Job 批量一行注记指向 pattern doc。

**P3-CK-cs-024-r3**（DIM-B ③）
- **控制点**：`ErpCsTicketBizModel.java:634` assertCan switch-default 抛裸 `IllegalArgumentException("unexpected action: ...")`——内部不可达护栏（三 case 闭合）非 NopException 族。
- **三态裁决**：新立（r1 扫描的是 extends 面非 throw 点）。P3。
- **修复方向**：改抛 NopException 内部码或 AssertionError 语义显式化；影响面 ≈0。

**P3-CK-cs-025-r3**（DIM-B ⑤/④）
- **控制点**：`TicketAssignResolver.java:66-69` `catch (RuntimeException e)` 跨域降级返空池**无 LOG**——与全域 8 个 notify 站点「降级必 WARN」范式不一致，自动分配失效不可诊断。
- **三态裁决**：新立（r1-011 仅覆盖该站点 orgId 维度，静默降级零同型）。P3 可观测性。
- **修复方向**：补 `LOG.warn`（降级原因 + teamCode），保持空池降级语义。

**P3-CK-cs-026-r3**（DIM-F）
- **控制点**：`ErpCsTicket/kanban.flux.yaml:80` onCardMove url 条件式仅覆盖 col-ASSIGNED/IN_PROGRESS/RESOLVED/CLOSED，col-CANCELLED/col-NEW 落 fallback `@mutation:ErpCsTicket__start`（start 断言 from=ASSIGNED，StateMachine:45）——拖拽至 CANCELLED 列**永远报错**，cancel 语义拖拽面不可达；:82-83 assign 分支 data 仅 ticketId（后端 `@Optional assignedToId`）——产生「ASSIGNED 无 assignee」工单。
- **三态裁决**：新立（r1 cs 族无 DIM-F finding；执行者 HEAD 复核 kanban.flux.yaml:80-84 + assertCanStart 在位）。P3（前端单入口缺陷，cancel 后端通道在位；null-assignee 面归并 P2-CK-cs-010 新站点注记）。
- **修复方向**：条件式补 col-CANCELLED→cancel 分支 + col-NEW 禁拖；assign 分支补 assignedToId 载体或走后端分配引擎；与 f13-kanban-drag 浏览器级用例（现仅 prj）同批补拖拽写路径回归。

**P3-CK-cs-027-r3**（DIM-F/E2E 纪律）
- **控制点**：`tests/e2e/zzz-diag-cs-add.spec.ts:9-36` spec 内联 `[data-slot=...]` 裸选择器（诊断遗留物入库）；`tests/e2e/visual/f13-non-standard-views.visual.spec.ts:21,35-47` visual 层内联 `[data-slot]/.nop-calendar`——违反 runbook「selector 唯一合法位置 = adapter 层」。
- **三态裁决**：新立（r1 cs 族零 E2E 纪律 finding；runbook L109 临时/诊断 spec 条款不含入库遗留物）。P3。
- **修复方向**：zzz-diag 遗留物删除或 adapter 化；f13 visual selector 移入 adapter/PageObject 层。

### 2.4 归属标注（§3.2 共享代码边界 + 跨域横切）

- posting 引擎：cs 无过账消费点，不适用。
- common 抽象族调用点合规（18/18 基类接入）；基类行为（status 列惰性边界）归 U20。
- 聚合横切面归 U21：action-auth cs 注册在位；孪生端点漂移（kanban.page.yaml vs flux.yaml）归并 P3-CK-mfg-023-r3 家族新站点，裁决归属 U21/M1.16；E2E selector 纪律主登记面归本格（cs spec 站点），f13 visual 站点跨域共享注记。
- notify 消费点 8 站点 try/catch 降级合规，发送引擎内部归 U11。

### 2.5 维度⑮断言抽样记录（7 doc × 14 检查点，漂移 6 全归并）

主抽：state-machine.md（六态表 ✓ / 迁移 9 边 ✓ / §6 超时原因守卫 ✓ 存在性 + cs-015 稀释现症 / §4 场景 B 重新分派 → **漂移** = cs-010）、sla.md（§1.2 isNull 匹配 ✓ / §3.2 ESCALATE 审计载体 ✓ / §2.1 isSlaCompleted 判定式 → **漂移** = cs-013）、csat.md（§4.3 查询示例 → **漂移** = cs-002 / §3.2 EXPIRED 终态 → **漂移** = cs-007 / §1.1 四值写路径 ✓）。扩样（≥2 漂移触发）：entitlement.md §三（→ cs-001 **漂移**）、use-cases+README TK 规则（→ cs-003 **漂移**，catalog 路径单点）、canned-response.md:76（→ cs-019 **漂移**）、time-tracking.md:152（totalTimeSpent 口径 ✓ 一致）。6/14 漂移全部归并既有 open ID，**零新漂移立项**；service-catalog/seed-data/ui-patterns 3 doc 为 UI/种子专题无后端关键断言可抽，记 N/A。

## 3. 统计

| 级别 | 本轮新立 | 复用（fixed 复核有效） | 归并（open 追加证据） |
| --- | --- | --- | --- |
| P0 | 0 | 0 | 0 |
| P1 | 0 | 0 | 3（cs-001/002/003） |
| P2 | 0 | 1（cs-012） | 10（cs-004..011/013/014） |
| P3 | 5（cs-023/024/025/026/027-r3） | 0 | 8（cs-015..022） |
| **合计** | **5** | **1** | **21** |

五格 verdict：DIM-B **finding**（3 新立 P3 + 归并态）/ DIM-F **finding**（2 新立 P3）/ DIM-S **pass** / DIM-T **pass** / DIM-I **pass**。历史 22 条 r1 ID 状态零覆写（1 fixed 复核有效 + 21 open 追加证据）。

## 4. 剩余风险声明（横切关注点 13 完整四件套）

- **已查**：erp-cs-{dao,service} 全部 processor（14 族）+ 18 实体 BizModel + 2 非实体聚合门面 + SlaPolicyMatcher/CodeRule 变量 + 5 dict 逐值 writer 核验 + 6 job 接线 + ErpCsErrors/Constants + 机械程式全套实跑（checker/反模式/codegen/聚合 E1/validate:flux/seed 门禁/cs 回归 185/strict+self-test）；owner docs 7 doc × 14 检查点抽样（漂移全归并）；r1 22 条逐一比对（8 条直接 file:line 复核 + 13 条控制点复核 + 1 fixed 复用）；r2 目录核对；cs seed 18 CSV + code_rule 抽查。
- **未深查（边界归属）**：`AbstractErpCrudBizModel` 基类内部（归 U20）；`erp-cs-web` 渲染时行为（静态 + 门禁，浏览器回归归看板专项）；P2-CK-cs-008 的 survey UK 修复面（ORM 保护区域，本审计未触碰，修复须双 agent 批准）；service-catalog/seed-data/ui-patterns 断言面（N/A 记录）。
- **残留风险（登记不裁决）**：① 21 条归并 open 修复归 M2.x，P1 三条（SLA 覆写/CSAT 分母/目录单 SLA 链断）建议优先——均直接影响 SLA/满意度口径正确性；② P3-CK-cs-026-r3 与 P2-CK-cs-010 联动（看板拖拽入口在 reassign/assign 校验落地前持续产生无主单）建议同批修复；③ E2E 拖拽写路径零浏览器用例（f13-kanban-drag 仅 prj）——026-r3 类缺陷仅静态走查可发现，建议 M2.x 测试批补齐；④ cron 键漂移族（cs-014）与通知模板种子缺失（cs-006）为跨域共性族（crm-014/log-002/aps-006 同型），建议统一批次。
- **successor 触发条件**：M1.17 收官以覆盖矩阵完整性校验本报告 5/5 格；M2.x 修复以「先写失败测试」承接 open 项；TK 编号 catalog 路径修复后 erp_cs_knowledge_base 等 catalog 族 seed 面需复跑完整性门禁。
