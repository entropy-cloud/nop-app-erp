# ck-quality-r3 — quality U09 五维符合性审计报告（ai-check-r3 M1.12）

> 工作项：M1.12（U07 × 五维全格 + U09 × 五维全格，冻结清单 §4 映射表第 12 行；本报告 = U09 quality 半格，U07 半格见 `ck-projects-r3.md`）。
> 执行日期：2026-09-08。审计时点 T0：HEAD `c8509908fcfbe6eb81ac94e9685efac8094b2c38`（2026-09-08 20:03）；脏面 = 2 条 untracked 计划文件，tracked 零修改（披露见计划 Phase 1）。
> 判定依据（冻结）：`m0-5-audit-checklists.md` §1/§2 + §3.3 U09 行 + §6 勘误 E1。执行期仅在报告与计划勾选注记追加证据，判定标准零改动。
> 范围：quality 全域（C 级）——inspection/non_conformance/action/recall/SPC 三表/sampling_plan/calibration 全实体族 + 领域服务（NcrLifecycleService/SpcCapabilityCalculator/SpcOutOfControlHandler/NcrReturnOrchestrator/NcrScrapAcctDocProvider/InspectionResultEvaluator/InspectionTemplateMatcher/RecallTargetLocator）+ spc/posting/processor/statemachine 子包（`module-quality/erp-qa-{dao,service,web}` src/main）；owner docs `docs/design/quality/`（README/state-machine/spc/inspection-integration/recall/use-cases）。Skills：`nop-platform-conformance-audit-prompt` + `code-quality-audit-prompt`（DIM-B）；其余维 Skill: none。
> 切片边界（§3.2）：posting 引擎内部归 fin-1（NCR 报废凭证消费侧归本格）；common 抽象族归 U20；聚合横切面归 U21；notify 派发子系统本体归 U11（本格仅核 notifyCustomers 调用点= r1 qa-023）。
> 零生产代码改动（roadmap 规则 6）：本报告 + 双索引 + 计划勾选注记为唯一产物。

## 1. 五维覆盖矩阵（5/5 完整）

| 维度 | 判定面（U09 全格） | 机械核查结果 | verdict |
| --- | --- | --- | --- |
| **DIM-B 后端平台合规** | §1.1 全套机械程式 + 15 维度走查（重点②⑧⑨⑮）+ 维度⑮断言抽样（初抽 2 doc × 14 + 漂移扩样 6 doc × 47） | 反模式族全零（`extends RuntimeException`=0 / `@Inject private`=0 / `System.currentTimeMillis`=0；`@Transactional` 2 文件命中全为 javadoc「事务钉 fin Facade REQUIRES_NEW」引注，真实共存=0）；checker 19 规则零漂移；`__XGEN_FORCE_OVERRIDE__` 34 处全为 dict.yaml codegen 校验点零手改；聚合器（E1 路径）含 `/erp/qa/auth/erp-qa.action-auth.xml`（L13）；15/15 无跳维：①常量/配置集中零手改生成物；②**finding**——16/16 BizModel 干净；I*Biz 化消费点：NCR→退货经 IErpPurReturnBiz/IErpSalReturnBiz ✓、NCR→召回经 IErpQaRecallBiz ✓、RecallTargetLocator 全 I*Biz（inv/sal 四接口）✓、报废凭证经 IErpFinVoucherBiz ✓（NcrScrapAcctDocProvider 纯 Provider 零 DAO）；**但跨域 daoFor 直查无豁免**（NcrPostingDispatcher:121-131 + NcrReturnOrchestrator:131-141 读 ErpInvStockBalance + ErpQaReportBizModel:361 读 ErpMdMaterial 缺 Dashboard 同型豁免注释）→ P2-CK-qa-027-r3；NcrLifecycleService（:31-34）/Dashboard（:43-45）豁免注释在位；③ErpQaErrors 35 码集中；④⑤⑥⑦⑩⑪ pass（notGenCode 5 实体全 md 域带机制 B 注记；tenantId 零预置；qa-011 orgId 面归并）；⑧**finding**——NCR 链 6 动作状态机 Bean 完整（submitReview/resolve/upgradeToRecall/cancel/postNcr/reverseNcr + transitions 元数据）+ Recall 双轴 Bean 同构；dict↔writer 全对照 ncr-status 5 值全有 writer（OVERDUE/STALE 死值 = doc 已登记一致）；**SPC 双层门控裁决复核成立不重开**（双 job `enabled|false` 默认关在位 = 1145-2 §142 + seed-data.md L19 冻结裁决）；**新发现 severity 字典污染**（UpgradeToRecallProcessor:52-54 NCR severity=NORMAL 直写 recall.severityLevel，recall-severity 字典 LOW/MEDIUM/HIGH/CRITICAL 无 NORMAL，:52 注释谎称「码值对齐」）→ P2-CK-qa-026-r3；⑨**finding**——Recall xbiz 五动作 auth 在位 + qa-003 inject 修复复核有效；**NCR 财务 mutation 零显式 auth**（ErpQaNonConformance.xbiz 空 `<actions/>`）→ P3-CK-qa-029-r3；spc-sampling batch 无 per-item 容错 → P3-CK-qa-030-r3；⑮初抽 2 doc × 14 断言现 2 漂移（spc.md:29 clCenterType「=10/20/30」数字注记 vs string 字典；spc.md:168 数据契约 range/stdDev vs loadSpcSamples:307-330 未返回）→ **触发扩样**：全部 6 owner doc × 47 断言，合计 12 漂移聚 4 簇 → 新立 qa-031/032/033-r3（详 §2.3）。**新发现合计 8 条（2 P2 + 6 P3）** | **finding**（8 新立：2 P2 + 6 P3；8 复用 + 17 归并，见 §2） |
| **DIM-F 前端页面与 E2E** | §1.2 全套 + 质检/NCR/SPC 页面族 | 全局面门禁数字同 ck-projects-r3 §1（0 error/999 页/855 页/325 ERR；qa 域 19 条 ERR 全 variant 同族成员）；`component="AMIS"` 0、ORM flux renderer 无缺失；qa 实测 16 实体页面族 + 8 手写页，源头链与 M0.4（B#99-106/A#36-38）100% 一致；NCR 页保留层行按钮 `@mutation:__submitReview/__resolve/__cancel` + CAPA 抽屉；SPC chart/sample/capability 三页手写（chart 页 3×`@query` 含 Dashboard 聚合）；i18nEn 全覆盖（MI.8 批 2 修复态复核：spc-chart tpl 双语、dashboard adaptor EN 孪生）；ncr-disposal 存根页 = plan 2026-08-03-1232-4 Deferred 豁免在册；qa specs（recall/spc）违规选择器 = 0、GraphQL 断言 = 0、`E2E_ENGINE` 缺省 flux；confirmText i18n 债务（qa 22 处）同 ck-projects-r3 §2.4 归属标注 | **pass** |
| **DIM-S seed 数据** | §1.3 全套 + inspection/non_conformance/action + SPC 三表（1145-2 Strategy C）+ 重算防护 | `TestErpSeedDataIntegrity` **4/0/0/0 全绿**；零 seed 变更（`git status --porcelain _init-data/` 空）；关联完整性抽查全命中：NCR-001/002 `inspectionId=2`→inspection 实存 + `sourceCode=INS-2026-002` 一致、NCR-003 `SPC/SPC-CHART-001`→chart 实存、action `ncrId=1`→NCR 实存、recall-001 `sourceNcrId=1`→NCR 实存 + recall_target `batchNo=B20260701` 与 inspection-001 批号一致；SPC 三表静态行 = 1145-2 Strategy C 裁决行逐项在位（chart-1 parameterId=0 占位软引用 + PENDING + cl/ucl/lcl 空、sample-1 isOutOfControl=true、capability-1 INADEQUATE；P/NP/C/U 四图 = plan 2026-07-19-0120-2 登记批）；**重算覆盖防护 = 双 job 门控默认关在位**（`erp-qa-spc-sampling/capability.job.yaml` 均 `enabled: @cfg:…|false`，fresh-DB 不触发重算）= 冻结裁决成立、Non-Goal 不重开；qa 无 deploy seed 零同步义务 | **pass** |
| **DIM-T 单元测试** | §1.4 全套 + inspection/NCR/SPC 族对账 + P1 清单行覆盖 | `mvn test -pl module-quality/erp-qa-service` **184/0/0/0 全绿**（= 批注账锚点 `qa 184` 精确一致）；覆盖对账：Inspection 8 动作 ↔ InspectionStateMachine+Result/ApprovalStateMachineMatrix+InspectionTrigger+CriticalItemVeto+TemplateCrudSmoke+BusinessCancelLinkage；NCR 7 ↔ NcrCapaEndToEnd+NcrPosting+NonConformanceStateMachineMatrix；Recall 6 ↔ RecallE2E+RecallLocateNotifyReturn+RecallStateMachine(+Matrix+ApprovalMatrix)；SPC ↔ SpcSampling+SpcSamplingEvaluateBatch+SpcAttributesSampling/ControlLimit+SpcCapability+SpcOutOfControl+RuleEnginePure+Statistics+CapabilityFormulas+DashboardSpc(+Chart)；跨域 I*Biz 契约 ↔ TestStubErpPurReturnBiz/SalReturnBiz/SalDeliveryBiz——**零业务关键缺口**；P1 清单行「quality 来料检验→NCR→退货（P1）」四层覆盖在位：TestErpQaInspectionTrigger（来料触发）→ NcrCapaEndToEnd/NcrPosting（REJECTED→NCR→RETURN 处置 + Orchestrator 经 stub 断言退货创建）→ RecallLocateNotifyReturn + `TestErpC09QaNcrCapaScrap`（集成）+ E2E specs；`SnapshotTest.RECORDING` = 0；`delVersion` 3 处均为软删审计断言非屏蔽；`*` 通配零命中 | **pass** |
| **DIM-I i18n（MI 先行时序口径）** | §1.5 全套（仅零回归 + 白名单合规） | `--strict` **PASS exit 0** + `--self-test` **PASS**；qa 探针族（CAT-1 1/CAT-2 10）维持清零零回归；WHITELIST qa 条目 1 条（`ErpQaDashboardBizModel.java`，cjk-baseline:368）四要素齐备（路径/理由 @Description 专属 E3 豁免批 2/2 Phase 4/owner doc 判定准绳表 #5/裁决来源 plan 2026-09-07-1715-1 Phase 4），实仓复核 `:68 @Description("质检看板 KPI…")` 登记准确零缺陷（条目数 1 < 3 全数核对）；`grep -L @Locale` *Errors.java = 空（ErpQaErrors @Locale 在位）；meta/i18n 零手改 | **pass** |

## 2. Finding 三态裁决（§2 跨轮查重全维强制）

> 查重源：r1 `ck-quality.md` C5.2 全 25 条逐一比对 + r2 目录（无 qa 同型新独立登记）+ §Mission 基线快照。**本轮新立 8 条**（`P2-CK-qa-026-r3`/`P2-CK-qa-027-r3` + `P3-CK-qa-028-r3`..`P3-CK-qa-033-r3`）；历史 25 ID 零覆写。**SPC 静态行重算覆盖裁决不重开**（计划 Non-Goal：1145-2 分析 §SPC 引擎重算覆盖风险「双层门控默认关 fresh-DB 不触发重算」+ seed-data.md L19 + M1.5 DIM-S 行同判例——引擎侧 source 标记/门控扩展属裁决既定 Deferred 面，其死配置缺陷已归 qa-019）。

### 2.1 复用（同型已 fixed，HEAD 复核有效）— 8 条

| 原 ID | 修复在位证据（T0 = HEAD `c8509908`） |
| --- | --- |
| P1-CK-qa-001（SPC 计量采样幂等键首点覆盖） | `SpcSamplingService.collectSamples` sampledInspectionIds 整点跳过在位；findInspectionByCode I*Biz 化 |
| P1-CK-qa-002（isInspectionCleared 全量 AND 死锁） | `ErpQaInspectionBizModel.isInspectionCleared` 取最新（inspections[size-1]）在位；`TestErpQaInspectionStateMachine` 双向回归行全绿 |
| P1-CK-qa-003（spc-capability.batch.xml 简单名 inject 必炸） | `spc-capability.batch.xml` FQCN inject（`app.erp.qa.service.entity.ErpQaSpcCapabilityBizModel`）在位 |
| P1-CK-qa-004（Integer 20 写 String severity） | `NcrLifecycleService` ncr.setSeverity(ErpQaConstants.NCR_SEVERITY_NORMAL) 字典码值在位 |
| P1-CK-qa-005（Recall reverseApprove 双轴不联动） | `ErpQaRecallProcessor.doReverseApprove:166-176` 双轴镜像 + approvedBy/At 清空在位；reject 联动 status=CANCELLED（:158-164 + RecallStateMachine:90-93） |
| P2-CK-qa-006（裸 CrudBizModel 无状态守卫） | F1.3 统一基类接入：16/16 BizModel extends AbstractErpCrudBizModel（调用点合规，基类缺陷归 U20） |
| P2-CK-qa-012（SCRAP 凭证 REQUIRES_NEW 先行提交） | (c) 收敛：billHeadCode=ncr.code 稳定 + qa-013 已修 → 幂等重试 posted 收敛（引擎层传导，qa 零代码变更） |
| P2-CK-qa-013（dispatchScrap voucherId 判 posted 悬挂） | 并入 F1.1 引擎层修复（幂等命中非 null）——NcrPostingExecutor posted 置位链复核有效 |

### 2.2 归并（同型 open 追加证据至原 ID）— 17 条

| 原 ID | r3 复核证据（T0，原 ID 状态不动仍 open） |
| --- | --- |
| P2-CK-qa-007 | recordResult 让步路径同事务自审原样（RecordResultProcessor:71-75 approveStatus=APPROVED + approvedBy=录入人，无质量主管校验） |
| P2-CK-qa-008 | classifyByCpk(null) 返回 INADEQUATE 原样 |
| P2-CK-qa-009 | SPC 采样每小时全量加载 + 逐行 getEntityById N+1 原样 |
| P2-CK-qa-010 | 系数表 n=2..10 越界静默回落原样（:235-252 回落面；残余：subgroupSize>10 控制限失真无告警） |
| P2-CK-qa-011 | 裸 limit(1) 无 ORDER BY 任取 balance 原样（**追加站点**：NcrPostingDispatcher:124 + NcrReturnOrchestrator:138 双站点排序缺失实证） |
| P2-CK-qa-014 | NcrReturnOrchestrator 空壳退货单原样（:90-116 仅头字段）；**追加对照**：同模块 `ErpQaRecallGenerateReturnsProcessor:60-76` 已带行——带行能力在位未复用 |
| P3-CK-qa-015 | resolve 的 RETURN 编排不受 ncr-posting-mode 门控原样 |
| P3-CK-qa-016 | escalateToRecall 终态死胡同原样（BizModel:97 注释自认「不建召回实体」） |
| P3-CK-qa-017 | enforceGate check-then-create 无并发防护原样 |
| P3-CK-qa-018 | recordResult posted 三件套只写 posted 原样（RETURN 侧 posted 恒 false） |
| P3-CK-qa-019 | 死配置族原样并**扩员**：`erp-qa.spc-enabled`（ErpQaConfigs:118-121）javadoc 自称「双层门控第二层」实为零消费死配置（唯一实门 = nop.job.* 键）+ `spc-sampling-cron`/`spc-capability-cron`（Constants:176-178）零消费 + **新站点** `erp-qua.recall-require-approval`（Configs:79-85）与 `ERR_RECALL_APPROVAL_REQUIRED`（Errors:107-110）零消费（审批强制已由审批轴结构性保证）——同族追加证据 |
| P3-CK-qa-020 | dashboard/report 全实体载入内存聚合原样 |
| P3-CK-qa-021 | 宽 catch 族原样并**追加站点**：SpcOutOfControlHandler:148-150 幂等预检 catch Exception→null（DB 故障静默放行重复建单风险） |
| P3-CK-qa-022 | NCR-CAPA 报表严重度排序误用 RECALL 常量原样 |
| P3-CK-qa-023 | notifyCustomers 仅簿记不派发原样（notify 子系统本体归 U11） |
| P3-CK-qa-024 | InspectionTemplateMatcher materialId=null 退化任意模板原样（P2-RC-040 复用注记维持） |
| P3-CK-qa-025 | NCR quantity 兜底 ONE 原样（SPC 失控 NCR quantity 恒 1） |

### 2.3 新立 `-r3` — 8 条（2 P2 + 6 P3）

**P2-CK-qa-026-r3**（DIM-B 维度⑧/②；NCR→召回升级路径）

- **控制点**：`ErpQaNonConformanceUpgradeToRecallProcessor:52-54`——NCR `severity=NORMAL` 时直接写入 recall `severityLevel`；`recall-severity` 字典值域 = LOW/MEDIUM/HIGH/CRITICAL（dict yaml + ErpQaConstants:82-85 实证），**无 NORMAL**；:52 注释「NCR severity(LOW/NORMAL/HIGH/CRITICAL=10/20/30/40) 与 recall severity(LOW/MEDIUM/HIGH/CRITICAL=10/20/30/40) 码值对齐」与事实相悖（词表不同 + dict valueType=string 无数字编码）。
- **证据**：severity 空值兜底 MEDIUM（:53）——缺陷仅在显式 NORMAL（手工 NCR 常见默认档）升级时触发；非法字典值直落召回单 severityLevel，污染 UI 映射/报表聚合（qa-022 报表排序误用同词表混乱的下游表征）。
- **问题/严重性**：数据字典违例 + 误导性注释；**P2**（qa-004 同型词表污染主路径判 P1，本站为升级路径窄面降档）。
- **建议修复方向**：NORMAL→MEDIUM 显式映射（常量级）+ 修 :52 注释；补升级路径负向测试。

**P2-CK-qa-027-r3**（DIM-B 维度②跨实体访问）

- **控制点**（3 站点，同型聚一条）：① `NcrPostingDispatcher.resolveStockBalance:121-131`（daoFor(ErpInvStockBalance) 跨域读金额取数，setLimit(1) 无排序——limit1 面归 qa-011）；② `NcrReturnOrchestrator.resolveWarehouseId/resolveCurrencyId:131-141`（同实体 daoFor 直查）；③ `ErpQaReportBizModel:361` daoFor(ErpMdMaterial) 跨域读且缺同单元 Dashboard（:43-45）已有的豁免 javadoc——三者均无 I*Biz、无「I*Biz 无法满足」豁免注释。
- **对照**：同单元 NcrLifecycleService（:31-34）/ErpQaDashboardBizModel（:43-45）豁免注释范式在位；RecallTargetLocator 全 I*Biz 证明 inv/sal 契约面可用。
- **问题/严重性**：冻结清单 §1.1 程式②「命中处须有注释理由」直接命中；跨域读绕数据权限/Meta 管道。**P2**（跨域形态对齐 r1 P2-CK-fin-007；对比 ast-028-r3 同模块纯读 P3 降档先例，本条为跨域站点）。
- **建议修复方向**：注入 inv/sal/md 侧 I*Biz（镜像 RecallTargetLocator 范式）或补 E3 豁免注释；与 fin-007/ast-028 同族修复时一并收口（全仓同型扫描面归 U20/M1.15）。

**P3-CK-qa-028-r3**（DIM-B 维度②；SPC 域服务 I*Biz 声明未用簇）

- **控制点**（3 站点）：① `SpcCapabilityCalculator:3-4` import IErpQaQualityGoalBiz/IErpQaRiskRegisterBiz **未使用**，:44-47 javadoc 声称「回写 IErpQaQualityGoalBiz/登记 IErpQaRiskRegisterBiz」，实现 :287-298 却 `daoProvider.daoFor(...)` 直查直写；② `SpcOutOfControlHandler:51-53` ncrBiz/actionBiz 注入后零使用（死注入），建单走 :99/:116 daoFor；③ `InspectionTemplateMatcher:30-73` 静态 match(daoProvider,...) 同域 daoFor 查 ErpQaInspectionTemplate/Line，IErpQaInspectionTemplateBiz 在位未用且无豁免注释（对照 NcrLifecycleService 同类有豁免）。
- **问题/严重性**：声明 I*Biz 未用/文档-实现漂移/同域 daoFor 无豁免；P3（同域只读+写路径经状态机 Bean，行为风险低）。
- **建议修复方向**：改真用 I*Biz 或改 javadoc + 补豁免注释 + 删死注入（三站点同批收口）。

**P3-CK-qa-029-r3**（DIM-B 维度⑨）

- **控制点**：`ErpQaNonConformance.xbiz:4` 空 `<actions/>`——postNcr/reverseNcr/resolve 财务过账动作零显式 auth 声明；action-auth.xml 无 ErpQaNonConformance FNPT 子节点（:52-55）；对照同单元 `ErpQaRecall.xbiz` 五动作全带 `<auth permissions>`（:6/:17/:28/:39/:50，roles=质量主管）——同域不对称。
- **问题/严重性**：财务过账动作权限门控缺失（同 prj-022-r3 族，r1 b2b-011/drp-018 P3 校准）；P3。
- **建议修复方向**：保留层 xbiz 补 postNcr/reverseNcr/resolve auth 声明（镜像 Recall 范式）。

**P3-CK-qa-030-r3**（DIM-B 维度⑨作业）

- **控制点**：`spc-sampling.batch.xml:20-27` 三段链对 collectSamples 抛错（parameterId 空/subgroupSize<2 的 chart）无 per-item try/catch——该 chart 每次 run 毒化 chunk（失败隔离仅到 chunk 级；对照 evaluate 段 per-sample catch + 三类早退达标）。
- **问题/严重性**：脏配置 chart 使采样批反复失败不可自愈；P3（job 默认关 + chunk 事务保数据一致）。
- **建议修复方向**：collectSamples 包 per-chart try/catch WARN 隔离（镜像 helper 单条隔离范式）。

**P3-CK-qa-031-r3**（DIM-B 维度⑮ owner-doc 漂移·触发机制簇，疑似需求分歧只登记不裁决）

- **控制点**（3 doc 同根因）：`README.md:81`/`inspection-integration.md:25`/`use-cases.md:21` 均断言「物料级 `inspection_required` 标志触发强制质检」——代码无物料级字段（orm/grep 零命中），强制质检实为**单据类型级 config**（`erp-qua.mandatory-inspection-bill-types`，ErpQaConfigs:20-31 + ERR_INSPECTION_MANDATORY_BLOCKED 阻断在位）。
- **问题/严重性**：触发维度设计-实现分歧（两种机制均提供强制质检能力）；P3 登记（分歧裁决归需求侧，不在审计内裁决）。
- **建议修复方向**：需求裁决后改代码或改 doc 三处同步。

**P3-CK-qa-032-r3**（DIM-B 维度⑮ owner-doc 漂移·命名/状态/契约簇）

- **控制点**（7 处聚簇）：① `README.md:40` + `inspection-integration.md:40`「reference_type/reference_name(/id)」↔ 实体 `relatedBillType/relatedBillCode`；② `recall.md:46` 字典 `erp-qa/approve-status` ↔ 实绑 `wf/approve-status`（码值本身一致）；③ `inspection-integration.md:182` NCR `IN_EXECUTION` 幽灵态（5 态现状无此值）；④ `use-cases.md:10` NCR 速查 4 态漏 `ESCALATED_TO_RECALL`；⑤ `spc.md:29` clCenterType「AUTO_FROM_DATA=10/MANUAL=20/TARGET=30」数字编码注记 ↔ string 字典；⑥ `spc.md:168` getSpcControlChartData 契约 samples 含 range/stdDev ↔ Dashboard loadSpcSamples:307-330 未返回；⑦ 附 `recall.md:77` Target.batchId ↔ 实列 batchNo。
- **问题/严重性**：doc 字段/字典/状态/契约命名漂移（state-machine.md 与代码已收敛为 5 态基线，漂移集中于其余 doc）；P3。
- **建议修复方向**：doc 批量对齐（owner-doc 维护批，不改契约段语义）。

**P3-CK-qa-033-r3**（DIM-B 维度⑮·模板优先级设计层级未实现，疑似需求分歧只登记不裁决）

- **控制点**（2 doc 同根因）：`inspection-integration.md:199-205`（§5.2）+ `use-cases.md:122`（UC-QA-07）断言模板优先级「物料级 > **物料类别级** > 全局默认」——`InspectionTemplateMatcher:30-48` 仅实现两级（materialId×inspectionType → 全局默认），模板实体无物料类别字段（orm grep category 仅 RiskRegister 命中）。
- **问题/严重性**：中间优先级层级为纯设计愿景未落地；P3 登记（能力部分实现，分歧裁决归需求侧）。
- **建议修复方向**：需求裁决——补类别维度（ORM ask-first）或 doc 降两级描述。

### 2.4 归属标注（§3.2 共享代码边界 + 跨域横切）

- posting 引擎内部（ErpFinPostingProcessor/sweep）**归属 fin-1**；本格仅核对 NcrPostingExecutor/Dispatcher 消费侧（经 IErpFinVoucherBiz Facade + 硬规则事务钉 Facade javadoc 引注在位）。
- common 抽象族（AbstractErpCrudBizModel/AbstractErpQa*Processor 骨架）调用点已审合规；基类行为缺陷归 U20（M1.15）。
- 聚合横切面归 U21（M1.16）；notify 派发子系统本体归 U11（M1.15）——`notifyCustomers` 仅簿记缺陷（qa-023）按边界登记于 qa 格不重复立项到 U11。
- **SPC 双层门控与 seed 静态行**：重算覆盖防护裁决（1145-2 §142 + seed-data.md L19）成立不重开；`spc-enabled` 死配置（名义第二层）缺陷归 P3-CK-qa-019 归并扩员。
- **跨域 i18n 债务**：view.xml confirmText/success 元素文本 CJK（qa 22 处：Recall 10/Inspection 6/NonConformance 6）——同 ck-projects-r3 §2.4 归属标注，归 U21/M1.16 + successor「F15/CAT-4 门控扩展覆盖 view.xml 元素文本时」。
- ncr-disposal 存根页 = plan 2026-08-03-1232-4 Deferred 豁免在册（DIM-F 面）。

## 3. 统计

| 级别 | 本轮新立 | 复用（fixed 复核有效） | 归并（open 追加证据） |
| --- | --- | --- | --- |
| P0 | 0 | 0 | 0 |
| P1 | 0 | 5（qa-001..005） | 0 |
| P2 | 2（qa-026/027-r3，DIM-B ⑧②） | 3（qa-006/012/013） | 6（qa-007..011、014） |
| P3 | 6（qa-028..033-r3，DIM-B ②⑨⑮） | 0 | 11（qa-015..025） |
| **合计** | **8** | **8** | **17** |

五格 verdict：DIM-B **finding**（2 P2 + 6 P3）/ DIM-F **pass** / DIM-S **pass** / DIM-T **pass** / DIM-I **pass**。历史 25 条 r1 ID 状态零覆写（8 fixed 复核有效 + 17 open 追加证据）。

## 4. 剩余风险声明（横切关注点 13 完整四件套）

- **已查**：erp-qa-service main java 全量（16 BizModel + 8 领域服务 + 4 posting + 27 processor + 7 spc + 5 statemachine + Dashboard/Report/Configs/Constants/Errors）+ orm.xml（dict 段/SpcChart 段/notGenCode 段）+ xbiz/beans/batch/job yaml/action-auth + web 88 yaml 全量源头链（6 页精读）+ seed 16 CSV 逐行（SPC 三表 + 关联链全对账）+ 机械程式全套实跑（checker 19 规则/反模式族/codegen 46 站点/聚合 E1/validate:flux/seed 门禁/qa 回归 184/strict+self-test）；owner docs 6 doc × 47 断言（初抽 2 doc + 漂移触发扩样全量）；r1 25 条逐一比对裁决；r2 目录核对。
- **未深查（边界归属）**：posting 引擎本体（fin-1）；`AbstractErpQa*Processor` 骨架与 common 基类内部（U20/M1.15）；notify 派发子系统本体（U11/M1.15——qa-023 消费点外）；SPC 引擎「重算覆盖 source 标记/门控扩展」属 1145-2 裁决既定 Deferred 面（重开需独立计划）；`erp-qa-web` 浏览器运行时行为（静态走查 + 门禁面，看板运行时专项归当前项目重点）。
- **残留风险（登记不裁决）**：① 17 条归并 open finding 修复归 M2.x（M2.6 批），其中 P2-CK-qa-011（裸 limit1，双站点证据追加）与 P2-CK-qa-014（空壳退货单）建议优先；② 新立 P2 两条（026 severity 污染/027 跨域 daoFor）建议 M2.x 优先排序参考，027 与 fin-007/ast-028 同族全仓扫描归 U20/M1.15；③ owner-doc 漂移簇（031/032/033）含 2 项疑似需求分歧——裁决归需求侧流程，审计只登记；④ `spc-enabled` 死配置造成「双层门控」文档表述失真（实际单层 job 门），文档口径归 M2.x 修复 qa-019 时一并对齐；⑤ `validate:flux` 325 条 variant 漂移为批前在案外部事项（successor：nop-chaos-flux dist 基线裁决）。
- **successor 触发条件**：M1.17 收官以覆盖矩阵完整性校验本报告 5/5 格；M2.x 修复以「先写失败测试」承接 §2.2/§2.3 open 项；qa-019 修复时同步 seed-data.md/i18n-compliance 的门控口径表述；owner-doc 漂移簇在需求裁决后由 doc 维护批收口。
