# ck-contract-r3 — contract U18 五维符合性审计报告（ai-check-r3 M1.14）

> 工作项：M1.14（U12 + U13 + U18 + U17 + U19 各 × 五维全格，冻结清单 §4 映射表第 14 行；本报告 = U18 contract 格（短码 ct），其余四域格分别见 `ck-crm-r3.md` / `ck-cs-r3.md` / `ck-b2b-r3.md` / `ck-drp-r3.md`）。
> 执行日期：2026-09-09。审计时点 T0：HEAD `5eb2c4dbe9faa68a685b5a90728cc2493b477b76`（2026-09-09；计划基线 `2c1c1ef25` 后唯一推进 = 同批姊妹审计产物提交，生产代码零变化）；脏面 = 2 条 untracked 计划文件（本计划 + 姊妹 `0547-3`），tracked 零修改。
> 判定依据（冻结）：`docs/audits/check/2026-09-06-1645-ai-check-r3/m0-5-audit-checklists.md` §1/§2 + §3.3 U18 行 + §6 勘误 E1。执行期仅在报告与计划勾选注记追加证据，判定标准零改动。
> 范围：contract 全域（C 级，无切片细分）——合同库（Contract/ContractLine/ContractVersion/Document/Template + amend/activate/signVersion/expire/terminate 编排）/审批（ApprovalMatrix/ApprovalRecord/ApprovalWorkflowEngine/ApprovalTimeoutEscalationJob）/返利（RebateAgreement/RebateTier/RebateAccrual/RebateSettlement/RebateEngine/RunAccrual/PostSettlement/ConsumptionLine/ConsumptionPeriodSummarize）/e-sign（SignatureRequest + Init/HandleCallback/AbstractSignatureRequest Processor + sign-status 状态机 + webhook）/发票计划（InvoicePlan/GenerateByTerm/TriggerInvoice/TriggerDuePlans）/量折扣（VolumeDiscount）/保留策略（DocRetentionJob）+ version-diff dashboard（`module-contract/erp-ct-{dao,service,web}` src/main）；owner docs `docs/design/contract/`（contract-repository/approval-workflow/volume-discount/e-signature + state-machine/README/use-cases）。Skills：`nop-platform-conformance-audit-prompt` + `code-quality-audit-prompt`（DIM-B）；其余维 Skill: none。
> 切片边界（§3.2）：posting 引擎内部归 fin-1——ct 发票计划生成/红冲等消费侧行为归本切片（PostSettlement O-4 豁免锚 posting-exemptions.md 在案），消费侧 finding 涉引擎内部时标注「归属 fin-1」；common 抽象族行为归 U20（15/15 BizModel 基类接入调用点合规）；聚合横切面归 U21；notify 消费点归 U11（到期提醒/审批升级经 `IErpSysNotificationBiz` 消费）。
> 零生产代码改动（roadmap 规则 6）：本报告 + 双索引 + 计划勾选注记为唯一产物（收官 `git status` 机械核证见 §4）。

## 1. 五维覆盖矩阵（5/5 完整）

| 维度 | 判定面（U18 全格） | 机械核查结果 | verdict |
| --- | --- | --- | --- |
| **DIM-B 后端平台合规** | §1.1 全套机械程式 + 15 维度走查（域焦点：合同审批矩阵/返利结算/e-sign 状态机/发票计划生成；共性②⑧⑨⑮）+ 维度⑮断言抽样 5 doc × 16 断言 | 反模式族全零（RuntimeException=0/@Inject private=0/System.currentTimeMillis=0/@Transactional=0，字符串 ==/!= =0，LOG 运行时中文=0）；checker 19 规则 = M0.3 快照行零漂移；`__XGEN_FORCE_OVERRIDE__` 14 处全为 erp-ct-meta dict.yaml 校验点零手改；聚合器含 `/erp/ct/auth/erp-ct.action-auth.xml`。IDaoProvider/IOrmTemplate 17 文件逐文件核验：BizModel 层偏离说明齐备（9/17）+ 3 job runInSession 范式级说明，**5 文件缺注释**（Amend/SignVersion/Activate/AbstractSignatureRequest Processor + RebateEngine）= P2-CK-ct-027-r3。15/15 无跳维：①dict/4 状态机 Bean/R6.7 每 mutation 一 Processor pass；②跨域写仅 O-4 豁免锚站点 + 5 注释缺位站点（027-r3）；③ErpCtErrors 集中中文 pass；④机械全零 + 跨域写同 @BizMutation 事务 pass；⑤CoreMetrics/JsonTool/StringHelper 全覆盖 pass；⑥15/15 AbstractErpCrudBizModel + InvoicePlan R1.33 isInvoiced 字段锁在位 pass；⑦md/pur/sal 只读 to-one + 无反向写 pass；⑧**finding**——sign-status/sign-provider dict value(10..60/99)/code(PENDING_SIGNATURE/MOCK) 双轨、writer 存 code 形态而列绑定按 value 翻译（执行者 HEAD 复核：ErpCtConstants:49-57 + orm.xml:85-92 + InitProcessor:44 setProvider(effectiveProvider) 在位）= **新立 P1-CK-ct-025-r3**；4 组死状态层-2 裁决在位（settlement CANCELLED/rebate-agreement ACTIVE/EXPIRED/SETTLED/contract CANCELLED）但 **ACTIVE 为 runAccrual 强制前置零 writer**（执行者 HEAD 复核：REBATE_AGREEMENT_STATUS_ACTIVE="ACTIVE" + RunAccrualProcessor:50 isActive 守卫 + 全仓零 setStatus(ACTIVE)）= **新立 P1-CK-ct-026-r3**；⑨域内审批引擎矩阵可配置（amount 边界含/order 升序）+ job 双层门控 pass；⑩Delta 覆盖点声明 pass；⑪orgId 透传审批记录 pass；⑫SM 矩阵测试在位（存在性核验）pass；⑬codegen 安全 pass；⑭web 面注册缺口归 DIM-F（030-r3）；⑮5 doc × 16 断言漂移 5（2 新立 + 2 归并 + 1 软漂移归并 025 族） | **finding**（5 新立：2 P1 + 1 P2 + 2 P3；复用 2 + 归并 21，见 §2） |
| **DIM-F 前端页面与 E2E** | §1.2 全套 + 合同库/审批页面 | `npm run validate:flux`：FLUX_PAGE_ERROR_COUNT 0 / 999 页（erp 855）；整体 exit 1 = 325 ERR 全 variant 族既有外部漂移（ct 命中 11 条同族不立项）；`component="AMIS"` 0、ORM `ext:web-renderer="flux"` 无缺失。页面面 33 page.yaml + 1 flux.yaml 全量清点（M0.4 重放零漂移）：15 实体 main+picker codegen stub 族 31 页全 PASS（39/39 view.xml `x:extends="_gen/_X.view.xml"` 保留层、0 MISSING-VIEW、`git status module-contract` 零脏面）；手写页仅 dashboard/version-diff 孪生（`@query:ErpCtContractVersion__findPage/__get` 5 处 REST /r/，i18nEn 14/27）；`graphql:` 5 处全 labelProp；docStatus 死状态样式分支（P3-CK-pur-017-r3 同型）三域含 _gen **0 命中**；合同库/审批面为标准 CRUD + 审批动作经 BizModel mutation（E2E business-actions ct×2 覆盖）。E2E：E2E_ENGINE 缺省 flux、17 spec selector 纪律 0 命中、断言走 REST /r/（`_helper.ts:34-43` rpcResponses ≥1 全 200）、页面级 GraphQL 断言 0。**新立 P3-CK-ct-030-r3**：action-auth FNPT 注册缺口——保留层 erp-ct.action-auth.xml 仅 8 FNPT，`ErpCtContractBizModel.java:118-308` 等 5 BizModel 12 个命名 mutation（submit/rejectAmend/suspend/resume/terminate/approveTermination/rejectTermination/expire/amend + triggerInvoice/postSettlement/runAccrual）零 FNPT 注册，deny-by-default 内部倒挂（b2b-011/drp-018 家族 ct 站点）；drp-018/b2b-011 原站点归并 open 不重开 | **finding**（1 新立 P3 + 家族归并态） |
| **DIM-S seed 数据** | §1.3 全套 + contract 14 表 seed（M1.5 批） | `TestErpSeedDataIntegrity` **4/0/0/0 全绿**；`_init-data` porcelain 空（零 seed 变更）；资产清点 **372 CSV + 1 SQL** = 冻结登记值；ct deploy `_seed_*.sql` 命中 = 0（登记处一致）；erp_ct_* **14 CSV** 精确在册（approval_matrix/approval_record/consumption_line/contract(+line/version)/document/invoice_plan/rebate_(accrual/agreement/settlement/tier)/signature_request/template/volume_discount）；抽查：invoice_plan 行与 contract_line 链接完整、rebate_agreement↔tier 层级自洽、approval_record 与 matrix 角色链一致；ct 非过账域 posted 列零命中（发票草稿由 triggerInvoice 生成至 pur 面，归 pur 格）N/A 带理由 | **pass** |
| **DIM-T 单元测试** | §1.4 全套 + P2 InvoicePlan 清单行覆盖 | `mvn test -pl module-contract/erp-ct-service` **168/0/0/0 全绿 BUILD SUCCESS**（= 锚点 168 零增量）；覆盖对账：注解动作 41 / BizModel 15 vs `_cases` 资产根 17（ApprovalWorkflow/ApprovalTimeoutJob/BillingFamily/ContractPosting/ContractCreateValidate/ContractCrudSmoke/ContractExpiryJob/TerminateGate/RebateSettlementEnd/SM 矩阵族）；**P2 行在位**：`TestErpCtBillingFamily` + `TestErpCtContractPosting` 发票计划生成链 ✓；`SnapshotTest.RECORDING` = 0 | **pass** |
| **DIM-I i18n（MI 先行时序口径）** | §1.5 全套（仅零回归 + 白名单合规，不产同类 finding） | `--strict` **PASS exit 0** + `--self-test` **PASS**；ct 探针族（CAT-1 12）维持清零零回归；WHITELIST ct 条目 **1/1 四要素齐备**：`ErpCtConfigs.java`（L358-362，C2② DEFAULT_TERMINATE_APPROVER_ROLE seed 角色名契约 + 准绳表 #5 指针 + plan 2026-09-07-1715-1 Phase 4 裁决来源）；`grep -L @Locale` = 空；meta/i18n 零手改 | **pass** |

## 2. Finding 三态裁决（§2 跨轮查重全维强制）

> 查重源：r1 `ai-check-index.md` §Finding 追踪（同域报告 `ck-contract.md` C7.2 全 24 条逐一比对）+ r2 只读目录（无 ct 同型新独立登记）+ §Mission 基线快照。**本轮新立 6 条**（2 P1 + 1 P2 + 3 P3）；历史 24 ID 零覆写（实仓核对 2026-09-09：r1 族最大号 CK-ct-024，新立自 025 起）。

### 2.1 复用（同型已 fixed，HEAD 复核有效）— 2 条

| 原 ID | 修复在位证据（T0） |
| --- | --- |
| P2-CK-ct-013（CRUD update 无已审守卫，同型 P1-CK-pur-003 族） | F1.3 统一基类接入在位：15/15 BizModel extends `AbstractErpCrudBizModel`；InvoicePlan 另有 R1.33 isInvoiced 字段锁（defaultPrepareUpdate:108-124）专项在位。残余注记：ct 实体多为 status/approveStatus 语义列，守卫激活面以列存在性为准（F1.3 注册边界），残余归 P1-CK-pur-003 族共性裁决（U20 格） |
| P3-CK-ct-018（BizModel 层大块死代码复制） | 编排单点收敛 Processor（Amend/Activate/SignVersion/PostSettlement/TriggerInvoice 等），BizModel 单行委托（ErpCtRebateAgreementBizModel:75-79 / ErpCtSignatureRequestBizModel:98-122），状态字面量经 SM Bean，无双份实现残留——HEAD 复核有效；**索引行仍 open = 回填缺口注记**（lesson-11 同型，状态回填归索引 owner 流程，本报告 §2.1 即终态证据面） |

### 2.2 归并（同型 open 追加证据至原 ID）— 22 条

> 22 条 open r1 ID 于 T0 逐一现症复核在位（12 条由独立补核任务逐文件实核，10 条由域走查直接复核），原 ID 状态不动仍 open。

| 原 ID | r3 复核证据（T0） |
| --- | --- |
| P1-CK-ct-001 | RunAccrualProcessor:58-65 alreadyAccruedCodes（sourceBillCode 空间）与 sumPeriodInvoices（发票 code 空间）两码空间永不相交去重恒空 + RebateEngine:66-90 无条件累加 + :103-107 `PERIOD-<date>` 原样 |
| P1-CK-ct-002 | :106-118 findPeriodInvoices 零 CT-REBATE- 前缀/来源排除原样 |
| P1-CK-ct-003 | RebateEngine.java:151 `compareTo(toAmount) < 0` 上界排他（维度⑮ volume-discount.md:101-102 抽样命中同点）原样 |
| P1-CK-ct-004 | PostSettlementProcessor:193-200 resolveCurrencyId/MaterialId 返 null + :115/:130 直接 set 零守卫 + TriggerInvoiceProcessor:99-104/:136-141 materialId null 行字段留空原样 |
| P2-CK-ct-005 | ContractBizModel:252-272 approveTermination 零 assertCanTerminate(contract.getStatus())（发起侧 :224 有）原样 |
| P2-CK-ct-006 | :163-177 rejectAmend 无 SIGNED/FINALIZED 前任版本守卫原样 |
| P2-CK-ct-007 | :295-305 手工 expire 零 endDate≤today 校验（批量路径 :339-342 不对称守卫在位）原样 |
| P2-CK-ct-008 | DocumentBizModel:151-152/:530-536/:222-239 + DocRetentionJob:56 `new ServiceContextImpl()` 无用户形态原样 |
| P2-CK-ct-009 | HandleCallbackProcessor:44-51 eventId 复用 remark 单槽判重原样 |
| P2-CK-ct-010 | RebateTierBizModel 18 行裸 stub + RebateEngine:118 signum 过滤负固定额静默跌回 + :121-123 无钳位原样 |
| P2-CK-ct-011 | RunAccrualProcessor:94-104 / RebateEngine:216-227 全量无界查询原样 |
| P2-CK-ct-012 | RebateEngine:78-86 accrual 未 setOrgId + TriggerDuePlans:38-40 无 orgId 过滤原样 |
| P2-CK-ct-014 | ErpCtConfigs:73,119 双键 + DEFAULT_APPROVAL_TIMEOUT_CRON 死常量 0 消费原样 |
| P2-CK-ct-015 | ExpiryJob:137-150 分档派发零已发标记 + TimeoutJob:110-134 零 escalatedAt 载体原样 |
| P2-CK-ct-016 | exchangeRate 硬编码 ONE（TriggerInvoice/PostSettlement:116,149；维度⑮ volume-discount.md:211 抽样命中同点）原样 |
| P2-CK-ct-017 | RunAccrualProcessor:59-60 periodEnd 不钳制 agreement.endDate 原样 |
| P2-CK-ct-024 | ErpCtConfigs:20-37 5 死配置键本轮重扫消费 = 0 原样 |
| P3-CK-ct-019 | GenerateByTerm:49-51 / TriggerInvoice:144-147 not-found 复用错误码原样 |
| P3-CK-ct-020 | GenerateByTerm:85-96 check-then-insert 无 UK + :98-107 零非负校验 + InvoicePlan 零 unique-keys 原样 |
| P3-CK-ct-021 | PostSettlement:57-80 total=0 照开 0 元贷项 + agreement==null 仍 POSTED 原样 |
| P3-CK-ct-022 | AbstractSignatureRequestProcessor:48-49 + SignatureRequestBizModel:76-77 WEBHOOK_SECRET 硬编码双份原样 |
| P3-CK-ct-023 | TimeoutJob:49 SCAN_LIMIT=200 + :111-117 eq(PENDING)+between+setLimit(200) 零 addOrderField 扫描窗饿死原样 |

### 2.3 新立 `-r3` — 6 条（2 P1 + 1 P2 + 3 P3）

**P1-CK-ct-025-r3**（DIM-B ⑧/D1 字段类型约定）
- **控制点**：`orm.xml:85-92`（sign-status/sign-provider dict value+code 双属性声明）+ `ErpCtConstants.java:49-57`（writer 常量 = code 形态 "PENDING_SIGNATURE"/"MOCK"）+ `_ErpCtDaoConstants:214-274`（生成常量 = value 形态 "10"/"99"）。
- **问题**：同域双轨并存——signStatus/signProvider 列存 code 形态、docType/ocrStatus 列存 value 形态；dict 翻译按 value 绑定（`ext:dict`），code 形态列的前端 label 解析/字典过滤必失效；违反 system-baseline §D1「value 与 code 合一」。
- **三态裁决**：新立（r1 24 条无 dict 值域族；e-signature.md:191-196 对 value/code 语义有显式声明故 doc 侧一致，漂移在实现双轨）。**P1**：签章请求全链路状态字典不可解析（用户可见正确性）。执行者 HEAD 复核：常量/orm/InitProcessor:44 三站在位。
- **修复方向**：二选一裁决——dict 收敛单属性（value=code 语义串）或 writer 改 value 形态 + 历史数据迁移；触及 ORM 保护区（dict 声明在 model orm.xml），修复须双 agent 批准；与 b2b-013（blockingLevel defaultValue=10 同族字典值域面）统一批次设计。

**P1-CK-ct-026-r3**（DIM-B ⑧/B2-B3 调度链断裂）
- **控制点**：`ErpCtRebateAgreementStateMachine.java:24-38,49-50`（isActive 守卫 + 同文件「预留死状态」裁定并存）+ `ErpCtRebateAgreementRunAccrualProcessor.java:50`（!isActive 拒绝）+ 全仓零 `setStatus(ACTIVE)` writer。
- **问题**：返利计提→结算链入口依赖 agreement.status=ACTIVE，而 ACTIVE 为登记在案的预留死状态（零 writer）——命名动作路径下返利链**入口永不可达**，唯一「激活」通道 = CK-ct-013 登记的裸 CRUD update 旁路（被守卫依赖的旁路 = 调度链断裂反模式）。执行者 HEAD 复核：常量 + 守卫 + 零 writer 三站在位。
- **三态裁决**：新立（CK-ct-013 只登记「status 可被直改」守卫缺失，未登记「唯一合法激活通道 = 该旁路」的前置悖论；r1 Dead 状态裁定未覆盖此链路语义）。**P1**：返利计提业务流不可达（用户可见功能失效）。
- **修复方向**：补 ACTIVE 激活 writer（协议生效 mutation 如 activate/agree，附审批/日期守卫）或 SM Bean 收窄 isActive 前置 + owner doc 裁决；与 CK-ct-024（rebate-enabled 等死配置键）同链成阈，统一返利链修复批次；先写「DRAFT 协议 runAccrual 拒绝 + 激活后放行」失败测试。

**P2-CK-ct-027-r3**（DIM-B ②）
- **控制点**：`ErpCtContractAmendProcessor.java:34` / `ErpCtContractVersionSignVersionProcessor.java:29` / `ErpCtContractActivateProcessor.java:36` / `AbstractErpCtSignatureRequestProcessor.java:52` / `RebateEngine.java:43`——5 处同域跨实体 IDaoProvider 无偏离说明注释（同类兄弟文件 9 处均有，范式不对称）；3 job IOrmTemplate 仅 runInSession 范式级说明。
- **三态裁决**：新立（r1 无此族）。对齐同模块已豁免范式的注释补齐级，P2（guide major 族按同域只读 + 范式成立降档）。
- **修复方向**：按兄弟文件「偏离说明」javadoc 形态补 5 处。

**P3-CK-ct-028-r3**（DIM-B ⑮）
- **控制点**：`approval-workflow.md:48`「审批节点属性含 requireSignOff」vs `orm.xml` ErpCtApprovalMatrix 字段集（无该列，仅 allowSkip）——设计属性未建模、无运行时消费（approverRole→ErpMdRole 已由实现注记 :188 裁决非漂移）。
- **三态裁决**：新立。P3（设计属性缺模，无行为影响）。
- **修复方向**：doc 修订（移除或标 Deferred）或列新增（ORM 保护区双批准）。

**P3-CK-ct-029-r3**（DIM-B ⑮）
- **控制点**：`e-signature.md:200`「MOCK（value=99）仅在测试 profile 启用，不进入生产 sign-provider 字典」vs `orm.xml:91` 生产 dict 实含 `<option code="MOCK" value="99">` 且为默认 provider（ErpCtConfigs.java:135）。
- **三态裁决**：新立。P3 doc 维护级；MOCK 入生产 dict 是否合规需 PM 裁决，牵连 P3-CK-ct-022（webhook 密钥硬编码）与 025-r3（provider 值域双轨）。
- **修复方向**：doc 与 ORM 对齐（登记生产默认 MOCK 的事实 + 裁决生产 provider 切换路径）。

**P3-CK-ct-030-r3**（DIM-F ⑭）
- **控制点**：`erp-ct.action-auth.xml` 仅 8 FNPT vs 5 BizModel 12 个命名 mutation 零注册（Contract 9 + InvoicePlan triggerInvoice + RebateSettlement postSettlement + RebateAgreement runAccrual）——test profile deny-by-default 下同实体能 CRUD 不能走流程（内部倒挂）。
- **三态裁决**：新立（r1 ck-contract.md 无 FNPT 登记——当时 web 面不在切片内；b2b-011/drp-018 同型家族 ct 站点，对齐家族先例自立 ct ID）。P3。
- **修复方向**：按 FNPT 范式补 12 注册（审批/终止→法务角色、返利结算→财务角色）；与 b2b-011/drp-018/mfg2-024-r3/fin4-023-r3 家族统一批次。

### 2.4 归属标注（§3.2 共享代码边界 + 跨域横切）

- posting 引擎内部归 fin-1：PostSettlement 凭证生成消费侧已按 O-4 豁免锚（posting-exemptions.md §ErpCtRebateSettlementBizModel，:101-103 注释）核验合规；发票计划红冲消费侧行为归本格，引擎内部红冲通道缺陷若发现标注「归属 fin-1」（本轮未发现）。
- common 抽象族调用点合规（15/15 基类接入 + InvoicePlan R1.33 专项锁）；基类行为归 U20。
- 聚合横切面归 U21：action-auth 聚合器 ct 注册在位；FNPT 缺口本体为域内注册面（030-r3 立本格）。
- notify 消费点（ExpiryJob/TimeoutJob 经 `IErpSysNotificationBiz`）合规，发送内部归 U11。
- 跨切片注记：025-r3 字典值域双轨与 b2b-013（defaultValue=10 无效字典值）同族；030-r3 与 drp-018/b2b-011/mfg2-024-r3/fin4-023-r3/mfg3-020-r3/hr2-028-r3 同族。

### 2.5 维度⑮断言抽样记录（5 doc × 16 断言，漂移 5）

state-machine.md 5 断言全一致（contract-status 6 值注记 ✓ / version-status 3 值 ✓ / ACTIVE 预留死状态 ✓（引出 026-r3 链语义）/ settlement DRAFT→POSTED 唯一边 ✓ / 到期事件名逐字 ✓）；e-signature.md 4 断言 3 一致 1 漂移（sign-status value/code 声明 ✓ 但引出 025-r3 双轨 / declined 折叠 ✓ / FULLY_SIGNED→signVersion ✓ / MOCK 生产字典隔离 → **漂移 = 029-r3**）；approval-workflow.md 4 断言 3 一致 1 漂移（approvalStatus 5 值 ✓ / 矩阵边界含 ✓ / config 键 ✓ / requireSignOff → **漂移 = 028-r3**）；扩样 volume-discount.md 2 断言（上界含 → **漂移 = ct-003 归并**；本位币折算 → **漂移 = ct-016 归并**）+ contract-repository.md 1 断言（ocrStatus 检索示例 code 语义 vs 列存 value——软漂移归并 025-r3 族注记）。漂移 ≥2 扩样条款已履行。

## 3. 统计

| 级别 | 本轮新立 | 复用（fixed 复核有效） | 归并（open 追加证据） |
| --- | --- | --- | --- |
| P0 | 0 | 0 | 0 |
| P1 | 2（ct-025-r3 字典双轨、ct-026-r3 ACTIVE 前置悖论） | 0 | 4（ct-001/002/003/004） |
| P2 | 1（ct-027-r3 注释缺位） | 1（ct-013） | 12（ct-005..012/014/015/016/017） |
| P3 | 3（ct-028/029/030-r3） | 1（ct-018，索引 open = 回填缺口注记） | 6（ct-019/020/021/022/023/024） |
| **合计** | **6** | **2** | **22** |

> 计数精确对账：r1 全 24 条 = 复用 2（013/018）+ 归并 22（P1×4 + P2×12 + P3×6）✓；新立 6 = P1×2 + P2×1 + P3×3。

五格 verdict：DIM-B **finding**（6 新立含 2 P1）/ DIM-F **finding**（1 新立 P3 + 家族归并态）/ DIM-S **pass** / DIM-T **pass** / DIM-I **pass**。历史 24 条 r1 ID 状态零覆写（2 fixed/已收敛复核有效 + 22 open 追加证据）。

## 4. 剩余风险声明（横切关注点 13 完整四件套）

- **已查**：erp-ct-{dao,service} 全部 processor（12 族）+ 15 BizModel + 4 状态机 Bean + ApprovalWorkflowEngine/RebateEngine + 3 job + ErpCtErrors/Constants/Configs + 机械程式全套实跑（checker/反模式/codegen/聚合 E1/validate:flux/seed 门禁/ct 回归 168/strict+self-test）；owner docs 5 doc × 16 断言抽样（漂移 2 新立 + 2 归并 + 1 软漂移）；r1 24 条逐一比对（12 条独立补核任务逐文件实核）；r2 目录核对；ct seed 14 CSV 抽查；执行者对 2 条 P1 新立站点三重 HEAD 复核（常量/orm/守卫 grep 在位）。
- **未深查（边界归属）**：`AbstractErpCrudBizModel` 基类内部（归 U20）；posting 引擎内部红冲通道（归 fin-1）；`erp-ct-web` 渲染时行为（静态 + 门禁，浏览器回归归看板专项）；维度⑫测试深度（SM 矩阵存在性核验，深度归 M2.x 测试批）。
- **残留风险（登记不裁决）**：① 21 条归并 open 修复归 M2.x，P1 四条（runAccrual 非幂等/贷项反噬/上界排他/nullable 裸崩）建议最优先——返利链金额正确性问题；② P1-CK-ct-026-r3 修复前返利链实际不可达（风险面被「不可达」 masking，激活旁路经裸 CRUD 直改无审计）；③ 025-r3 字典双轨修复涉 ORM 保护区 + 历史数据迁移，须双 agent 批准 + 与 b2b-013 统一批次；④ FNPT 家族（030-r3 + 5 域站点）建议 M2.x 统一权限注册批次。
- **successor 触发条件**：M1.17 收官以覆盖矩阵完整性校验本报告 5/5 格；M2.x 修复以「先写失败测试」承接 open 项；MOCK 生产 provider 裁决（029-r3）若转向真实 e-sign 供应商则 022/025-r3 同批收敛。
