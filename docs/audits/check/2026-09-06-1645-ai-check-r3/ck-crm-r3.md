# ck-crm-r3 — crm U12 五维符合性审计报告（ai-check-r3 M1.14）

> 工作项：M1.14（U12 + U13 + U18 + U17 + U19 各 × 五维全格，冻结清单 §4 映射表第 14 行；本报告 = U12 crm 格，其余四域格分别见 `ck-cs-r3.md` / `ck-contract-r3.md` / `ck-b2b-r3.md` / `ck-drp-r3.md`）。
> 执行日期：2026-09-09。审计时点 T0：HEAD `5eb2c4dbe9faa68a685b5a90728cc2493b477b76`（2026-09-09；计划基线 `2c1c1ef25` 后唯一推进 = 同批姊妹 `2026-09-09-0547-1`（M1.11 hr hr-2）审计产物提交 `5eb2c4dbe`，生产代码零变化）；脏面 = 2 条 untracked 计划文件（本计划 `2026-09-09-0547-2` + 同批姊妹 `0547-3`），tracked 零修改（MI.9 收官审计脏树实跑先例）。
> 判定依据（冻结）：`docs/audits/check/2026-09-06-1645-ai-check-r3/m0-5-audit-checklists.md` §1/§2 + §3.3 U12 行 + §6 勘误 E1。执行期仅在报告与计划勾选注记追加证据，判定标准零改动。
> 范围：crm 全域（C 级，无切片细分）——线索（Lead/LeadStatus/Stage/Source/LostReason/LeadConvLog）/评分（LeadScore(+Line)/LeadScoreConfig(+Line)/LeadScoringEngine/LeadScoringRecalcJob）/瀑布漏斗（LeadFunnel/FunnelStageMetrics/FunnelAggregationEngine/RefreshFunnelProcessor）/转化（ConversionProcessor/EventCancel/EventComplete Processor）/活动与事件（Activity/Event/EventCategory + calendar/timeline 手写页）/营销（Campaign——r1 已裁决状态机整支缺失）/CPQ（PriceRule/ConfigRule/BundlePricing(+Line)/ProductConfigurator/QuoteTemplate/ProductConfigRuleEngine/PriceRuleEngine/GenerateQuoteProcessor）/销售序列（Sequence/SequenceStep/SequenceAssignment/LeadSequenceProgress + 3 Processor）/区域（Territory/TerritoryAssignmentRule/Team/TeamMember + 2 Processor）/预测（Forecast(+Line)/ForecastPeriod/ForecastAccuracy/ForecastAggregator/QuotaRollupCalculator/ForecastRecalcJob/ClosePeriodProcessor）+ report/dashboard 门面（`module-crm/erp-crm-{dao,service,web}` src/main）；owner docs `docs/design/crm/`（lead-scoring/lead-waterfall/cpq/marketing/sales-forecast/sales-sequence/territory + state-machine/README/use-cases）。Skills：`nop-platform-conformance-audit-prompt` + `code-quality-audit-prompt`（DIM-B）；其余维 Skill: none。
> 切片边界（§3.2）：posting 引擎内部归 fin-1（crm 无过账面，消费点零）；common 抽象族（`AbstractErpCrudBizModel`/`AbstractErpImmutableCrudBizModel`/`AbstractCancelProcessor`）行为归 U20（本切片只审调用点——37/37 实体 BizModel 基类接入合规 + CancelProcessor 1 站点调用合规）；聚合横切面归 U21；notify 消费点归 U11（EventReminder/SequenceOverdue 2 job 经 `IErpSysNotificationBiz` 消费合规）。
> 零生产代码改动（roadmap 规则 6）：本报告 + 双索引 + 计划勾选注记为唯一产物（收官 `git status` 机械核证见 §4）。

## 1. 五维覆盖矩阵（5/5 完整）

| 维度 | 判定面（U12 全格） | 机械核查结果 | verdict |
| --- | --- | --- | --- |
| **DIM-B 后端平台合规** | §1.1 全套机械程式 + 15 维度走查（域焦点：线索评分重算/线索瀑布/CPQ 规则；共性②⑧⑨⑮）+ 维度⑮断言抽样 6 doc × 12 断言 | 反模式族全零（RuntimeException=0/@Inject private=0/System.currentTimeMillis=0/@Transactional=0）；checker 19 规则 = M0.3 快照行零漂移；`__XGEN_FORCE_OVERRIDE__` 23 处全为 erp-crm-meta dict.yaml 校验点零手改；聚合器含 `/erp/crm/auth/erp-crm.action-auth.xml`；IDaoProvider/IOrmTemplate 20 文件 22 注入逐文件核验——唯一正向豁免注释范式 `ErpCrmLeadBizModel.buildTeamMemberResolver:310-312` 在位、Report 门面 2 使用点有内联裁决注释、其余 20 注入点零注释（新立 P3-CK-crm-021-r3）。15/15 无跳维：①dict/状态机 Bean/XLang 表达式驱动（ProductConfigRuleEngine:88-98）pass；②跨域全经 `IErpMdPartnerBiz`/`IErpSalQuotationBiz` I*Biz（ConversionProcessor javadoc:39），同域 daoFor 无注释面=021-r3，GenerateQuoteProcessor raw 写 Lead = P2-CK-crm2-006 归并；③ErpCrmErrors 集中中文 ErrorCode pass；④`@Inject private`=0/`@Transactional`=0 pass；⑤CoreMetrics 全覆盖 pass；⑥37/37 AbstractErpCrudBizModel + Report 虚名门面合法，getCreatedOpportunity 只读 @BizMutation = P3-CK-crm-017 归并；⑦orm 5 个 notGenCode 外部实体（全 erp_md_*）pass；⑧**dict 死状态 = 0**——lead-doc-status 5/5、event-status 3/3、sequence-progress-status 3/3 全有 writer（常量 ErpCrmConstants:14-18 逐一相符），forecast-period-status OPEN 初始态不设防（新立 P3-CK-crm-022-r3），campaign-status dict 整支缺失 = P2-CK-crm2-007 归并；⑨5 job 接线齐全 + notify 消费 2 站点合规，cron 键漂移族 4 站点 = P3-CK-crm-014/crm2-015 归并、job 直调无事务 = P3-CK-crm-015/crm2-011 归并；⑩Delta 覆盖点声明 + 测试验证（TestErpCrmEventStateMachineDeltaOverride）pass；⑪⑬⑭ pass；⑫31 测试类含双状态机矩阵 pass；⑮6 doc × 12 断言（详见 §2.5）漂移 5（2 新立 + 3 归并） | **finding**（5 新立：1 P2 + 4 P3；复用 2 + 归并 37，见 §2） |
| **DIM-F 前端页面与 E2E** | §1.2 全套 + 商机看板/活动日历（复杂手写页清单成员） | `npm run validate:flux`：FLUX_PAGE_ERROR_COUNT 0 / 999 页（erp 855）；整体 exit 1 = 325 ERR 全 variant 族既有外部漂移（crm 命中 29 条同族不立项）；`component="AMIS"` 0、ORM `ext:web-renderer="flux"` 无缺失。页面面 80 page.yaml + 3 flux.yaml 全量清点（M0.4 矩阵重放零漂移）：31 实体 main+picker codegen stub 族 62 页 + ref-lead ×3 全 PASS（`x:gen-extends`→view.xml，36/36 view.xml `x:extends="_gen/_X.view.xml"` 保留层）；商机看板 opportunity-kanban（`@query:findOpportunityBoardData`/`@mutation:moveStage` REST /r/，i18nEn 16/4）在位；活动日历 calendar（`@query:ErpCrmActivity__findPage`，i18nEn 10/5）在位；timeline/lead-conversion/report ×3 手写页全 PASS（renderHtml+download 报表容器范式）；`graphql:` 命中 7 处全为 labelProp 元数据键读取。E2E：E2E_ENGINE 缺省 flux、business-actions/crud/reports spec PageObject 合规、页面级 GraphQL 断言 0。**新立 P3-CK-crm-024-r3**（看板 magic offset `substring(4/5)` + 硬编码 `¥` 无 i18nEn 承载）；孪生端点漂移（page.yaml vs flux.yaml 取数组合错配）归并 P3-CK-mfg-023-r3 家族新站点（U21/M1.16 裁决归属）；P2-MA4-020（Lead.view.xml docStatus badge 消费死值 'ACTIVE'）归并 open 现症在位；覆盖注记：看板拖拽无浏览器级写路径用例（f13-kanban-drag 仅 prj） | **finding**（1 新立 P3 + 家族归并） |
| **DIM-S seed 数据** | §1.3 全套 + CRM 5 表 seed（1045-1 批）+ M1.4a 批配置链 | `TestErpSeedDataIntegrity` **4/0/0/0 全绿**；`_init-data` porcelain 空（零 seed 变更）；资产清点 **372 CSV + 1 SQL** = 冻结登记值；crm deploy `_seed_*.sql` 命中 = 0（登记处 L109 一致）；erp_crm_* 35 CSV 在位（1045-1 批 5 表 + M1.4a 配置链 score_config(+line)/price_rule/config_rule/quota 等）；链式抽查：lead#1（LEAD-2026-001，OPPORTUNITY/QUALIFIED，partner 1↔stage 1）↔ lead_score#1（LEAD_ID=1/CONFIG_ID=1/TOTAL_SCORE=85/AUTO_QUALIFIED=true/TRIGGERED_ACTION=AUTO_QUALIFY）↔ score_config 链自洽；crm 非过账域 posted 列零命中 N/A 带理由 | **pass** |
| **DIM-T 单元测试** | §1.4 全套 + P1 Lead→Convert 清单行覆盖 | `mvn test -pl module-crm/erp-crm-service` **188/0/0/0 全绿 BUILD SUCCESS**（= 锚点 188 零增量，姊妹计划未触 crm 测试面）；覆盖对账：注解动作 44 / BizModel 36 vs `_cases` 资产根 20（ConversionGuards/CpqGenerateQuote/双状态机矩阵/job/report 族，密度 >1 动作/类，纯 CRUD stub 共享 CrudSmoke）；**P1 行在位**：`TestErpCrmConversionGuards` convertToOpportunity 路径 ✓；`SnapshotTest.RECORDING` = 0；快照屏蔽 `@var:ErpCrmLead@updateTime` 机制抽查合规 | **pass** |
| **DIM-I i18n（MI 先行时序口径）** | §1.5 全套（仅零回归 + 白名单合规，不产同类 finding） | `--strict` **PASS exit 0**（0 new violations vs 冻结快照 170 files，单向收紧）；`--self-test` **PASS**；crm 探针族（CAT-1 3）维持清零零回归；WHITELIST crm 条目 **0**（记录条目数 0，与计划基线一致）；`grep -L @Locale` = 空；meta/i18n 零手改 | **pass** |

## 2. Finding 三态裁决（§2 跨轮查重全维强制）

> 查重源：r1 `ai-check-index.md` §Finding 追踪（同域双族报告 `ck-crm-lead.md` C6.3 全 19 条 + `ck-crm-cpq-forecast.md` C6.4 全 20 条逐一比对）+ r2 只读目录（无 crm 同型新独立登记）+ §Mission 基线快照。**本轮新立 5 条**（`P2-CK-crm-020-r3` + 4×P3）；历史 39 ID 零覆写。crm 双族查重口径：`CK-crm-`（lead 001..019）+ `CK-crm2-`（cpq-forecast 001..020）两侧都查，新立统一落 `CK-crm-` 单族自 020 续起（实仓核对 2026-09-09：r1 族最大号 CK-crm-019 / CK-crm2-020）。

### 2.1 复用（同型已 fixed，HEAD 复核有效）— 2 条

| 原 ID | 修复在位证据（T0 = HEAD `5eb2c4dbe`） |
| --- | --- |
| P2-CK-crm-007（CRUD update 无已审守卫，同型 P1-CK-pur-003 族） | F1.3 统一基类接入在位：37/37 实体 BizModel extends `AbstractErpCrudBizModel`（→CrudBizModel + ErpCrudStatusLock），调用点合规。残余注记：状态锁对 `docStatus`/`status` 列实体惰性 = F1.3 修复证据自登记的设计边界（「激活由列存在性决定」，ErpCrudStatusLock.java:26-41 实测），残余面归 P1-CK-pur-003 族共性裁决（U20 基类行为归 M1.15 格） |
| P2-CK-crm2-012（ForecastPeriod 状态/期间可经 update_ 直改，同上族） | 同上：ForecastPeriodBizModel 基类接入在位；`status` 列惰性同属注册边界，残余归共性族。创建路径初始态不设防缺口由本切片新立 P3-CK-crm-022-r3 分立承接（不同控制点） |

### 2.2 归并（同型 open 追加证据至原 ID）— 37 条

> 全部 37 条 open r1 ID 于 T0 逐一现症复核在位（15 条由独立补核任务逐文件实核，22 条由域走查直接复核），证据落本节；原 ID 状态不动仍 open。

| 原 ID | r3 复核证据（T0） |
| --- | --- |
| P1-CK-crm-001 | QuotaRollupCalculator.java:203 实际段仅 eq(docStatus,CONVERTED) 无 leadType 过滤；:187/:205 territoryId=null 时 isNull 漏计已分配；:185-210 双查询无期间过滤原样 |
| P1-CK-crm-002 | doLose/doCancel:139-147 无 writeConvLog + computeHeader:132-137 无 isOpp/isWonStage 条件（维度⑮抽样命中同点）原样 |
| P2-CK-crm-003 | ConversionProcessor.java:98-113 复制 owner/team，无 territoryId/campaignId 透传原样 |
| P2-CK-crm-004 | QuotaRollupCalculator.java:94-99 仅排除当前层级显式行，子区域覆盖行与明细行平面求和原样 |
| P2-CK-crm-005 | :118-136 无子行查重 + :138 divide(count,2,HALF_UP) 无末行吸收 + Quota 实体零 unique-keys 原样 |
| P2-CK-crm-006 | LeadDuplicateChecker.java:55-65 全表加载内存逐一 keyHits、无 orgId 原样 |
| P2-CK-crm-008 | ErpCrmTerritoryBizModel.java:81-91 defaultPrepareDelete 仅查子节点零引用计数原样 |
| P3-CK-crm-009 | MoveTerritoryProcessor.java:73-85 全文件零 setIsLeaf 原样 |
| P3-CK-crm-010 | LeadBizModel.java:399-402 setLimit(200) 无排序无截断标志 + 双查询无 orgId（DIM-F 看板面无 truncated 提示加重证据）原样 |
| P3-CK-crm-011 | RefreshFunnelProcessor.java:150-152 off-by-one `le(+1d 00:00)` 原样 |
| P3-CK-crm-013 | computeDaysInStage 末条停留不计时长（维度⑮抽样命中同点）原样 |
| P3-CK-crm-014 | funnel-aggregation/forecast-recalc/sequence-overdue + event-reminder 4 job.yaml cron 键漂移原样 |
| P3-CK-crm-015 | FunnelAggregationJob.java:52 直调无事务 + 顶层仅 LOG.error 原样 |
| P3-CK-crm-016 | LeadBizModel.java:146-168 assignLead 零 docStatus 校验 + :177-187 reassign 三 ID 零存在性反查原样 |
| P3-CK-crm-017 | ErpCrmLeadBizModel.java:214-218 getCreatedOpportunity 只读标 @BizMutation 原样 |
| P3-CK-crm-018 | LeadScoringEngine.java:241-249 countCompletedEvents null 退化全局计数 + findAllByQuery().size() 原样 |
| P3-CK-crm-019 | LeadBizModel.java:223-224 checkAndNotify 返回值丢弃、无日志无事件（维度⑮抽样命中同点）原样 |
| P1-CK-crm2-001 | 序列逾期扫描误报族现症原样（序列 job/进度扫描面） |
| P1-CK-crm2-002 | PriceRuleEngine.java:85-97 匹配器仅 productId/customerId，类别维度零消费（维度⑮ cpq.md:111/113 抽样命中同点）原样 |
| P2-CK-crm2-003 | ForecastAggregator.java:262-280 expectedCloseDate 代理 + :254-260 仅 ownerId≠null 行 + ClosePeriodProcessor:28-40 无期末终刷原样 |
| P2-CK-crm2-004 | ForecastRecalcJob.java:72-79 setLimit(1) 无排序（维度⑮ sales-forecast §业务规则 4 抽样命中同点）原样 |
| P2-CK-crm2-005 | auto-assign-on-qualify 死配置（默认 true 零消费）+ assignSequence 无匹配抛错语义相悖原样 |
| P2-CK-crm2-006 | GenerateQuoteProcessor leadDao():309-311 raw 写 Lead 绕过管道 + lead 守卫缺失原样 |
| P2-CK-crm2-007 | dict 目录 23 文件零 campaign-status + CampaignBizModel 裸 stub（营销状态机/ROI 整支缺失）原样 |
| P2-CK-crm2-008 | 死配置 + 无匹配抛错原样（同 crm2-005 控制点族） |
| P2-CK-crm2-009 | ReportBizModel 数据集 HEAD 无 orgId filter 原样 |
| P2-CK-crm2-010 | AssignSequenceProcessor.java:51-56 check-then-insert 无锁 + ErpCrmLeadSequenceProgress 无 unique-keys 原样 |
| P2-CK-crm2-011 | ForecastRecalcJob 直调无事务包装（同 crm-015 族 job 面）原样 |
| P3-CK-crm2-013 | ForecastAggregator.java:74-76 putIfAbsent + :239-252 loadOpportunities 零排序原样 |
| P3-CK-crm2-014 | setExpectedClosedRevenue(ZERO)/无 setCurrencyId/无 setCalculatedBy + isCurrent/isDefault/isMandatory 列零消费 + 3 config 键零调用原样 |
| P3-CK-crm2-015 | forecast-recalc + sequence-overdue cron 键漂移（同 crm-014 族）原样 |
| P3-CK-crm2-016 | ForecastAggregator.java:282-291 逐 forecast 查行删 N+1 + GenerateQuoteProcessor:185-198 全表 active 规则内存过滤原样 |
| P3-CK-crm2-017 | ProductConfigRuleEngine.java:92-98 evalCondition rethrow 无隔离原样 |
| P3-CK-crm2-018 | LeadSequenceProgressBizModel.java:158-161 序列级跳过率冒名 stepDropOffRate + :168 toDays() 截断原样 |
| P3-CK-crm2-019 | AssignSequenceProcessor.java:49 零终态守卫 + :78-84 空步骤照建进度 + SwitchSequenceProcessor:52-56 错码复用 + :105-111 setLimit(1) 无排序原样 |
| P3-CK-crm2-020 | GenerateQuoteProcessor.java:225 毫秒时间戳 code + :101-107 currencyId null 误用 NO_PRICE_MATCHED 原样 |

### 2.3 新立 `-r3` — 5 条（1 P2 + 4 P3）

**P2-CK-crm-020-r3**（DIM-B ⑧/⑮）
- **控制点**：`ErpCrmLeadProcessor.validateTransitionForQualify:55-62` + 状态机 Bean `assertCanQualify`。
- **问题**：`state-machine.md:44`（§迁移条件）声明 NEW→QUALIFIED 双前置「leadType=LEAD + 联系人信息必填」，代码仅校验 docStatus，全 service grep contact*/leadType 校验零命中——任意无联系信息的 leadType=LEAD 线索可 QUALIFIED 进入漏斗（数据质量门槛缺失）。
- **三态裁决**：新立（ck-crm-lead 全文零同点——P2-RC-033 为 lastContactDate 维度不同控制点）。级别 P2：用户可见数据质量缺口，owner doc 断言漂移。
- **修复方向**：Processor 层补两前置（leadType≠LEAD 复用 ERR_LEAD_TYPE_MISMATCH；contact 全空抛新码），或 owner doc 显式降级登记；修复阶段先写失败测试。

**P3-CK-crm-021-r3**（DIM-B ②）
- **控制点**：processor/support/job/report 20 文件 22 处 `IDaoProvider/IOrmTemplate` 注入点零豁免理由注释（唯一正向范式 `ErpCrmLeadBizModel:310-312` E3 自检注释形态在位证明规范可落地）。
- **三态裁决**：新立（r1 双族机械扫描未覆盖注释面）。已知失败模式 3 文档合规缺口，P3。
- **修复方向**：按 LeadBizModel E3 注释形态补齐（Processor/Job 可批量注记一行指向 processor-extension-pattern.md 全局立法）。

**P3-CK-crm-022-r3**（DIM-B ⑧，关联 P2-CK-crm2-012 同族）
- **控制点**：ForecastPeriod `status` 列 orm 无 defaultValue（app-erp-crm.orm.xml:813 区域）+ BizModel 无 defaultPrepareSave 初始化 writer——期间创建可带任意 status（含 FROZEN 直建绕过 requireOpen 语义）；对照 Lead/Event 状态机 Bean「初始态由创建路径写入」范式缺位。
- **三态裁决**：新立（crm2-012 已 fixed 且其 F1.3 边界只覆盖 update 侧；创建路径初始化缺失无历史同型）。非死状态（OPEN 可达）故 P3。
- **修复方向**：defaultPrepareSave 强制 status=OPEN。

**P3-CK-crm-023-r3**（DIM-B ⑮/①）
- **控制点**：`LeadScoringEngine.formValue:219-239` 实现 `count×N` 公式语法，超出 `lead-scoring.md:212` 实现注记声明范围（「ENGAGEMENT_SCORE 或字面量整数」）——声明面落后于实现（反向漂移，行为无错）。
- **三态裁决**：新立（零命中）。P3 doc 维护级。
- **修复方向**：owner doc L212 补一句声明。

**P3-CK-crm-024-r3**（DIM-F）
- **控制点**：`opportunity-kanban.flux.yaml:54-55` `cardId.substring(5)`/`toColumnId.substring(4)` 硬编码 flux 内部 `card-`/`col-` 前缀长度（cs kanban 同型 L83）——前缀变更即静默错参；:38 `"¥"+expectedRevenue` 硬编码人民币符号无 i18nEn 承载。
- **三态裁决**：新立（r1 crm 双族无 DIM-F finding 登记）。P3。
- **修复方向**：常量化前缀或事件载荷直传 ID；货币符号走 i18nEn/配置。

### 2.4 归属标注（§3.2 共享代码边界 + 跨域横切）

- posting 引擎：crm 无过账消费点，不适用。
- common 抽象族调用点合规（37/37 基类接入 + AbstractCancelProcessor 1 站点）；基类行为（ErpCrudStatusLock 列存在性边界）归 U20（M1.15）。
- 聚合横切面归 U21：action-auth 聚合器 crm 注册在位（维度⑭仅核注册性）；孪生端点漂移（opportunity-kanban.page.yaml vs flux.yaml）归并 P3-CK-mfg-023-r3 家族新站点，裁决归属 U21/M1.16。
- notify 消费点 2 站点（EventReminder/SequenceOverdue job）经 `IErpSysNotificationBiz` 合规，发送内部归 U11。
- 跨切片注记：F1.3 状态锁对 docStatus 族实体的覆盖缺口为共性族面（P1-CK-pur-003 族），本格不重复立项。

### 2.5 维度⑮断言抽样记录（6 doc × 12 断言，漂移 5）

| owner doc:位置 | 断言 | 代码对照 | 判定 |
| --- | --- | --- | --- |
| state-machine.md:44 | NEW→QUALIFIED 前置 leadType=LEAD + 联系人必填 | ErpCrmLeadProcessor.java:55-62 仅 docStatus | **漂移（= 020-r3）** |
| state-machine.md:37 | convert 原地置 OPPORTUNITY、docStatus 保持 QUALIFIED | ConversionProcessor.java:179-185 | 一致 |
| state-machine.md:56 | 终态 CONVERTED/LOST/CANCELLED 无出边 | ErpCrmLeadStateMachine isTerminal + assertCan* | 一致 |
| state-machine.md:52 | 阶段回退 STRICT 默认 + ERR_STAGE_BACKWARD_MOVE + convLog 留痕 | ErpCrmConstants:34-36 + LeadProcessor:97/162 | 一致 |
| cpq.md:111/113 | PriceRule 类别维度可配置匹配 | PriceRuleEngine.java:85-97 零消费 | **漂移（= crm2-002 归并）** |
| cpq.md:192 | ruleType rank + priority 排序 + priceOverride 优先 | PriceRuleEngine.java:76-78,149-164 | 一致 |
| lead-scoring.md:156 | auto-qualify 默认 true | ErpCrmConstants:57 + LeadScoringEngine:155-156 | 一致 |
| lead-scoring.md:212 | FORMULA 支持「ENGAGEMENT_SCORE 或字面量整数」 | formValue:219-239 额外 count×N | **漂移（= 023-r3，轻微）** |
| lead-waterfall.md:95 | totalWon = CONVERTED 且 isWonStage | FunnelAggregationEngine:132-137 无 isWonStage | **漂移（= crm-002 归并）** |
| lead-waterfall.md:135 | 停留时间截断至分析期末 | computeDaysInStage 末条不计 | **漂移（= crm-013 归并）** |
| README.md:75 | 查重「提示用户合并或跳过」 | LeadBizModel:223-224 返回值丢弃 | **漂移（= crm-019 归并）** |
| sales-forecast §业务规则 4 | job 重算所有 OPEN 期间 | ForecastRecalcJob:72-79 setLimit(1) | **漂移（= crm2-004 归并）** |

扩样条款：漂移 ≥2 已触发，扩样覆盖 6 owner doc 关键面；marketing（campaign dict 缺失 = crm2-007）/sales-sequence/territory 的 r1 已登记漂移均复核在位，未发现新独立漂移点。

## 3. 统计

| 级别 | 本轮新立 | 复用（fixed 复核有效） | 归并（open 追加证据） |
| --- | --- | --- | --- |
| P0 | 0 | 0 | 0 |
| P1 | 0 | 0 | 4（crm-001/002 + crm2-001/002） |
| P2 | 1（crm-020-r3） | 2（crm-007 + crm2-012） | 14（crm-003/004/005/006/008 + crm2-003..011） |
| P3 | 4（crm-021/022/023/024-r3） | 0 | 19（crm-009..019 共 11 + crm2-013..020 共 8） |
| **合计** | **5** | **2** | **37** |

> 计数精确对账：r1 双族全 39 条 = 复用 2（007/012）+ 归并 37（P1×4 + P2×14 + P3×19）✓；新立 5 = P2×1 + P3×4。

## 4. 剩余风险声明（横切关注点 13 完整四件套）

- **已查**：erp-crm-{dao,service} 全部 processor（10 族）+ 38 BizModel（37 基类 + 1 报表门面）+ 3 引擎（LeadScoring/FunnelAggregation/QuotaRollup）+ PriceRule/ProductConfig 规则引擎 + 状态机 Bean ×2 + 5 job + ErpCrmErrors/Constants/Configs + 机械程式全套实跑（checker/反模式/codegen/聚合 E1/validate:flux/seed 门禁/crm 回归 188/strict+self-test）；owner docs 6 doc × 12 断言抽样；r1 双族 39 条逐一比对裁决（15 条独立补核任务逐文件实核）；r2 目录核对；crm seed 35 CSV 链式抽查。
- **未深查（边界归属）**：`AbstractErpCrudBizModel`/ErpCrudStatusLock 基类内部（归 U20）；`erp-crm-web` 渲染时行为（静态走查 + 门禁，浏览器回归归看板运行时专项）；marketing.md/use-cases.md/territory.md 全量断言（抽样 6 doc 达标，新漂移 2 < 扩样后再现阈值）；nop-message 跨域事件面（crm 零发布点）。
- **残留风险（登记不裁决）**：① 37 条归并 open finding 修复归 M2.x，其中 P1-CK-crm-002（漏斗期间口径）与 P1-CK-crm2-002（CPQ 类别维度）建议 M2.x 优先；② P2-CK-crm-020-r3 与线索数据质量 seed 投产联动——leadType≠LEAD 或无联系信息数据入漏斗时升 P1；③ crm2-007 campaign 状态机整支缺失为特性级 Deferred，与营销 ROI 报表页（已落页面）形成「页面有、引擎无」倒挂，建议 M2.x 统一裁决；④ F1.3 status/docStatus 惰性边界跨域共性（crm/docStatus 族 + drp/status 族同型）建议 U20 格统一裁决。
- **successor 触发条件**：M1.17 收官以覆盖矩阵完整性校验本报告 5/5 格；M2.x 修复以「先写失败测试」承接 §2.2/§2.3 open 项；孪生端点漂移家族裁决归 U21/M1.16 格。
