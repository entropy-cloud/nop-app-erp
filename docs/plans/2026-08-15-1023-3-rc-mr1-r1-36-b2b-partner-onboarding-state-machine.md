# 2026-08-15-1023-3-rc-mr1-r1-36-b2b-partner-onboarding-state-machine RC-R1.36 — b2b 伙伴上线状态机推进（MR1 第一批纯预授权）

> Plan Status: completed
> Last Reviewed: 2026-08-15
> Mission: requirement-compliance
> Work Item: RC-R1.36（P1-RC-080 UC-B2B-007 伙伴上线状态机推进：activate/suspend/deactivate 状态守卫 + promoteToTesting/promoteToCertified + pass-rate≥90%/认证清单门槛 + goLiveDate/archivedAt 回写 + 24h 监控）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.36 行 + `docs/audits/arm-index.md` P1-RC-080 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`（纯 BizModel，ORM 字段已就绪）
> Related: `docs/design/b2b/use-cases.md`（L1 UC-B2B-007）；`docs/design/b2b/partner-onboarding.md`（四阶段流程 + 业务规则 + 配置点）；`docs/audits/2026-08-08-0135-rc-ma4-a4-2-163-169-b2b-runtime.md`（A4.2.168 运行时证据）
> Audit: required

## Current Baseline

- **finding P1-RC-080（arm-index 行，UC-B2B-007 基本流程①-⑥+异常）**：L1（`use-cases.md:220-231`）逐字「REGISTERED→TESTING[交换 EDI 报文，**验证通过率≥90%**]→CERTIFIED[**所有必检项通过**]→PRODUCTION[**设置 goLiveDate**，**上线监控 24 小时**]→TERMINATED[**archivedAt 记录**]」「异常：测试通过率不达标或必检项未通过时无法推进至下一阶段」。L3 实仓：
  - `ErpB2bPartnerProfileBizModel#activate:22-27` **任意态→PRODUCTION 零状态守卫**（requireEntity + setStatus("PRODUCTION") + updateEntity，**未设 goLiveDate**）+ `#suspend:31-36`（→SUSPENDED）+ `#deactivate:40-45`（→TERMINATED **未设 archivedAt**）——A4.2.168 运行时确认成立（TESTING/CERTIFIED 可直接跳 PRODUCTION 绕过门槛）。
  - **TESTING/CERTIFIED 推进 mutation 零实现**（grep `promoteToTesting|promoteToCertified|startTesting|setStatus("TESTING")|setStatus("CERTIFIED")` 跨 erp-b2b-service/src/main 零业务命中）。
  - **通过率≥90% 聚合零实现**：`ErpB2bTestExchangeBizModel` 17 行 CRUD 桩（无 pass-rate 聚合 mutation）；ORM `ErpB2bTestExchange` 有 `passed`（BOOLEAN）/testCaseCode/partnerProfileId 列（orm.xml:443-475）。
  - **认证清单门槛零实现**：`ErpB2bCertificationChecklistBizModel` 17 行 CRUD 桩；ORM `ErpB2bCertificationChecklist` 有 `isMandatory`（defaultValue=true）/`isPassed`/checkedBy/checkedAt/evidence 列（orm.xml:478-507）。
  - **24h 上线监控零实现**（无 scheduler/job——module-b2b 全域零 Job bean/job.yaml）。
  - **监控查询结构锚点缺口（设计裁决输入）**：`partnerProfileId` 仅存在于 `ErpB2bPartnerCredential`（orm.xml:417）/`ErpB2bTestExchange`（:448）/`ErpB2bCertificationChecklist`（:483）——**`ErpB2bEdiDoc`/`ErpB2bEdiLog` 无 partnerProfileId 列**（EdiDoc 经 `formatId`/relatedBill 关联，EdiLog 经 `ediDocId`→EdiDoc 间接关联）；伙伴级失败率聚合仅可经间接路径（`PartnerProfile.allowedFormats` JSON[formatCodes] → `EdiFormat.code` → `EdiDoc.formatId`，或 org 级 scope）——查询锚点形态属 D4 裁决项（见 Phase 1），任何给 EdiDoc 加 partnerProfileId 的诉求触 ORM = ask-first 移出本行。
  - ORM `ErpB2bPartnerProfile` **字段已就绪无 writer/守卫** ✅：`status`（dict `erp-b2b/partner-status` 6 值 REGISTERED/TESTING/CERTIFIED/PRODUCTION/SUSPENDED/TERMINATED，orm.xml:64-70）+ `goLiveDate`（propId 19）+ `archivedAt`（propId 20，tagSet=clock）。
  - `IErpB2bPartnerProfileBiz` 接口已有 activate/suspend/deactivate 三 @BizMutation 契约（`@BizMutation` 注解 + 中文 javadoc 语义「上线：CERTIFIED→PRODUCTION / 暂停：PRODUCTION→SUSPENDED / 终止：任意→TERMINATED」）——守卫语义与接口 javadoc 对齐。
- **partner-onboarding.md 设计契约**：四阶段状态机图（REGISTERED→TESTING→CERTIFIED→PRODUCTION，PRODUCTION→TESTING 回退，`*→SUSPENDED`/`SUSPENDED→原阶段`/`*→TERMINATED`）+ 业务规则 1「**不可跳过阶段**：必须按 REGISTERED→TESTING→CERTIFIED→PRODUCTION 顺序流转」+ 配置点 `erp-b2b.onboarding-test-pass-rate`（0.9）/`erp-b2b.onboarding-production-monitor-hours`（24）/`erp-b2b.onboarding-test-timeout-hours`（72）/`erp-b2b.onboarding-cert-expiry-reminder-days`（30）——config 键设计存在未登记 `ErpB2bConfigs`（当前仅有 b2b.enabled/asn/transport/mft/webhook/error-blocks 键）。
- **测试基线**：erp-b2b-service ≈ 57 @Test（既有 10 测试类：AsnCrudSmoke/AsnInbound/AsnInventoryIntegration/EdiEnvelope/EdiPosting/MftTransport + statemachine 包 4 类[EdiDocStateMachineMatrix/AsnStateMachineMatrix/EdiDocStateMachineDeltaOverride/EdiDocStateMachineBaselineIoC]）。
- **预授权判据**（第一批纯预授权）：纯 BizModel 代码逻辑（状态守卫 + 推进 mutation + 聚合校验 + 字段回写），**不触 ORM 结构**（goLiveDate/archivedAt/status 列已存在）**/会计过账/删除**；24h 监控属纯调度接线（job.yaml + config + notify，对齐已落地先例 R1.2 bank-recon-adj-reverse / R1.23 lead-scoring / R1.27 pnl-calc）；roadmap RC-R1.36 行 `todo`，Deps（R1.0 done）已满足。
- **涉及文件**：`ErpB2bPartnerProfileBizModel.java`（activate/suspend/deactivate 守卫 + promoteToTesting/promoteToCertified + goLiveDate/archivedAt 回写）；`IErpB2bPartnerProfileBiz.java`（契约扩展）；`ErpB2bTestExchangeBizModel.java`/`ErpB2bCertificationChecklistBizModel.java`（聚合查询能力，Decision：BizModel 内 vs helper）；`ErpB2bErrors.java`/`ErpB2bConstants.java`/`ErpB2bConfigs.java`；`beans.xml`（按需注册）；测试类（新增 `TestErpB2bPartnerOnboarding`）；owner doc（partner-onboarding.md 注记）+ arm-index + roadmap + `docs/logs/2026/08-15.md`（回填）。

## Goals

- **状态迁移守卫运行时成立（P1-RC-080 ①）**：activate/suspend/deactivate 增状态守卫——activate 仅 CERTIFIED→PRODUCTION（对齐接口 javadoc「上线：CERTIFIED→PRODUCTION」+ 业务规则 1 不可跳过）；suspend 仅非终态→SUSPENDED；deactivate 非终态→TERMINATED；非法迁移抛领域错误码（`ERR_B2B_PARTNER_ILLEGAL_TRANSITION` 或对齐 EDI/ASN `ERR_B2B_*_ILLEGAL_TRANSITION` 命名族）。
- **推进 mutation + 门槛校验（P1-RC-080 ②）**：`promoteToTesting`（REGISTERED→TESTING，前置守卫：基本配置完整——Decision：校验集合）+ `promoteToCertified`（TESTING→CERTIFIED，前置：测试通过率 ≥ config `erp-b2b.onboarding-test-pass-rate` 默认 0.9 聚合校验[passed 计数/总数，TestExchange 按 partnerProfileId 聚合] + 无阻断项[Decision：关键测试用例 TC-001/TC-004 必过 + 无 ERROR blocking 未处理，对齐 partner-onboarding.md §后置条件]）+ 门槛不达标拒绝（L1 异常路径）。
- **字段回写（P1-RC-080 ③）**：activate 设 goLiveDate（now）+ deactivate 设 archivedAt（now，对齐 tagSet=clock 语义）。
- **24h 上线监控（P1-RC-080 ④）**：job 形态（扫描 PRODUCTION 且 goLiveDate 后 `erp-b2b.onboarding-production-monitor-hours` 窗口内伙伴的 EDI 失败率/解析失败，通知 B2B 管理员）——查询锚点按 D4 裁决（间接路径/窗口收窄/ORM 移出），只读查询 + notify 派发；job.yaml 接线对齐已落地先例（R1.2 bank-recon-adj-reverse / R1.23 lead-scoring / R1.27 pnl-calc 的 job.yaml + helper 失败隔离范式）。
- **config 键登记**：`erp-b2b.onboarding-test-pass-rate`（0.9）/`erp-b2b.onboarding-production-monitor-hours`（24）登记 `ErpB2bConfigs`——dead 键恢复消费。
- **测试**：状态守卫（非法迁移拒绝矩阵）/推进门槛（通过率达标/不达标/认证清单必检项缺失拒绝）/字段回写（goLiveDate/archivedAt 断言）/24h 监控 job/GraphQL RPC 冒烟 + 快照录制。
- **零回归**：erp-b2b-service 既有测试全绿（57 基线）+ 全仓构建 + compliance checker 零漂移。
- **回填**：arm-index P1-RC-080 → `done (RC-R1.36)` + roadmap 行 → `done` + owner doc 注记 + `docs/logs/` 日志条目。

## Non-Goals

- **不实现 P2-RC-062/064/065/068**（isActive 派发门槛/自动 archive/出站映射/MFT 证书停用——P2 登记不强制，非本行范围）。
- **不实现 P1-MA2-073 出站自动化/自动重试**（resolved-via-deferral 注记，非本行范围）。
- **不触 ORM 结构**（零列/零索引/零 UK——goLiveDate/archivedAt/status 已存在；窗口字段 config 化而非新列）。
- **不做凭证轮换/证书到期自动停用**（partner-onboarding.md 业务规则 4/5 属 P2-RC-068/MFT 范围，非本行）。
- **不做前端 AMIS 接线**（上线向导/推进按钮不在本行；后端 mutation 提供能力面）。
- **不改真相源契约段落**（use-cases L1 不动）。
- **不实现真实 EDI 测试执行**（测试消息交换流程经既有 EdiDoc 出站/入站链路，非本行——通过率聚合只读已记录 passed 数据）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧的预授权代码逻辑修复，Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/b2b/use-cases.md`（L1 UC-B2B-007）+ `docs/design/b2b/partner-onboarding.md`（四阶段流程 + 业务规则 + 配置点）+ `docs/audits/2026-08-08-0135-rc-ma4-a4-2-163-169-b2b-runtime.md`（A4.2.168 运行时证据）
- Skill Selection Basis: 实现面 = CrudBizModel + 状态守卫 + 跨实体聚合查询（TestExchange/Checklist 经 IBiz 注入）+ config 门控（`nop-backend-dev`——对齐 R1.22/R1.30 状态守卫先例 + b2b 域 EDI/ASN 状态机错误码命名族）；测试（`nop-testing`：JunitAutoTestCase + GraphQL RPC + 快照范式——对齐 R1.30/R1.31 测试范式）。无 view.xml/xbiz/ORM 变更。

## Infrastructure And Config Prereqs

- 无新外部服务/环境变量。config key 登记 `ErpB2bConfigs`：`erp-b2b.onboarding-test-pass-rate`（默认 0.9）/`erp-b2b.onboarding-production-monitor-hours`（默认 24）/按 Decision 引入的 24h 监控 cron（`erp-b2b.onboarding-monitor-cron` 或 job.yaml `@cfg` 门控）。
- 24h 监控 job.yaml 注册于 `app-erp-all/src/main/resources/_vfs/nop/job/conf/erp-b2b-onboarding-monitor.job.yaml`（对齐已落地先例 R1.23/R1.27 范式，enabled 默认 false）。
- 分域验证前置：`mvn install -DskipTests` 后 `mvn test -pl module-b2b/erp-b2b-service`。

## Execution Plan

### Phase 1 - Explore 门槛契约与监控形态裁决（Decision）

Status: completed
Targets: `ErpB2bPartnerProfileBizModel.java`；`ErpB2bTestExchangeBizModel.java`；`ErpB2bCertificationChecklistBizModel.java`；`ErpB2bConfigs.java`；`ErpB2bConstants.java`；`ErpB2bErrors.java`
Skill: `nop-backend-dev`

- Item Types: `Decision | Proof`
- Prereqs: 无（既有基线）

- [x] `Decision` **推进门槛契约裁决（D1）**：promoteToTesting 前置校验集——选项 A（裁决候选）：最小面（档案存在 + partnerId 非空[关联 ErpMdPartner]）；选项 B：全配置校验（protocol/authMethod/transportEndpoint 必填——partner-onboarding.md Stage 1 后置条件「基本配置完整」）。promoteToCertified 前置——pass-rate≥config（默认 0.9）聚合 + 关键测试用例 TC-001/TC-004 必过（testCaseCode 前缀匹配，Decision）+ 无 `blocking_level=ERROR` 未处理问题（**实体勘正：`blockingLevel` 列在 `ErpB2bEdiDoc`[orm.xml:174] 而非 EdiLog；EdiLog→EdiDoc 经 `ediDocId` 列[:334]/to-one[:351] 间接关联**——查询范围 Decision：本行仅 passed 聚合 + 必检清单，ERROR 阻断查询复杂度评估）。**决策记录理由 + 备选**。
      **决策记录（已执行）**：**promoteToTesting 前置 = 选项 B（全配置校验）**——partnerId 非空 + protocol 非空 + authMethod 非空 + transportEndpoint 非空 + allowedFormats 非空（非空=非 blank 字符串），缺失抛 `ERR_B2B_PARTNER_PROFILE_INCOMPLETE`（缺项清单入参）。理由：(1) partner-onboarding.md Stage 1 完成事项四件「基础信息/配置传输协议和端点/配置认证方法并交换凭证/选择支持的 EDI 格式」+ 迁移表 REGISTERED→TESTING 前置条件「基本配置完整」——全配置校验是设计契约字面义务；(2) protocol/authMethod 虽 ORM mandatory 但 `defaultPrepareSave` 不校验，运输端点/允许格式为可选列，须在推进门卫补上（TestExchange 测试交换依赖传输端点可达性）；(3) partnerId 非空 = UC 前置条件「ErpMdPartner 已存在」。选项 A（最小面）否决：partnerId 单一校验无法阻止无端点/无格式档案进入测试期。**promoteToCertified 前置 = 三门槛链**：(a) pass-rate ≥ config `erp-b2b.onboarding-test-pass-rate`（默认 0.9）= TestExchange 按 partnerProfileId 聚合 passed=true 计数 / 总数，**零 TestExchange 行 → 通过率 0 → 拒绝**（无测试证据不可认证）；(b) 关键用例 TC-001/TC-004 必过 = testCaseCode 前缀匹配（`startsWith("TC-001")`/`startsWith("TC-004")`），各至少 1 行 passed=true，缺失 → 拒绝；(c) **ERROR 阻断查询本行不实现**——EdiDoc 无 partnerProfileId 锚点（与 D4 同源结构缺口），经 allowedFormats 间接路径的伙伴级 ERROR 聚合复杂度高且语义近似（formatId 非伙伴专属），登记 Deferred But Adjudicated（successor = 伙伴级 EDI 统计需求立项，与 D4 Option C 同 successor）；本行门槛 = 通过率聚合 + 必检清单（L1「测试通过率不达标或必检项未通过时无法推进」字面双门槛）。
      - Skill: `nop-backend-dev`
- [x] `Decision` **认证清单门槛裁决（D2）**：promoteToCertified 前置「所有必检项通过」——选项 A（裁决候选）：Checklist 按 partnerProfileId 聚合，`isMandatory=true` 行全部 `isPassed=true`（零 CheckList 行时语义 Decision：空清单=通过 vs 拒绝）；选项 B：仅检查 isMandatory 缺失场景。**决策记录理由 + 备选**。
      **决策记录（已执行）**：**选项 A（全必检项通过校验 + 空清单拒绝）**——按 partnerProfileId 聚合 Checklist：`isMandatory=true` 行全部 `isPassed=true` 才放行，任一必检项未通过 → 抛 `ERR_B2B_PARTNER_CERTIFICATION_NOT_MET`（未通过项清单入参）；**零 Checklist 行 → 拒绝**（同码，未通过项 = 空清单）。理由：(1) L1「逐项完成认证检查清单（ErpB2bCertificationChecklist），所有必检项通过」+ Stage 3 后置条件「所有必检项 isPassed=true」——空清单无法证明「逐项完成」，通过语义须有检查证据行；(2) partner-onboarding.md 标准认证检查项表 10 项（8 必检 + 2 非必检）——零行 = 未执行认证流程，放行将绕过 L1 门槛（与 P1-RC-080 根因同型：绕过门槛）。选项 B（仅查缺失场景）否决：isMandatory 列 defaultValue=true 下缺失场景窄（仅显式 false 行豁免），无法覆盖「已检未过」场景。
      - Skill: `nop-backend-dev`
- [x] `Decision` **错误码命名裁决（D3）**：复用既有 `ERR_B2B_*_ILLEGAL_TRANSITION` 命名族（对齐 EDI `ERR_B2B_EDI_DOC_ILLEGAL_TRANSITION`/ASN `ERR_B2B_ASN_ILLEGAL_TRANSITION`）——新增 `ERR_B2B_PARTNER_ILLEGAL_TRANSITION`（参数 partnerCode/currentStatus/expectedStatus）+ 门槛错误码（`ERR_B2B_PARTNER_PASS_RATE_NOT_MET`/`ERR_B2B_PARTNER_CERTIFICATION_NOT_MET` 或统一门槛码，Decision）。**决策记录理由 + 备选**。
      **决策记录（已执行）**：**选项 A（命名族 + 细分门槛码）**——新增 4 码：`ERR_B2B_PARTNER_ILLEGAL_TRANSITION`（`erp.err.b2b.partner-illegal-transition`，参数 partnerCode/currentStatus/expectedStatus，消息镜像 EDI/ASN 族模板「伙伴 {partnerCode} 当前状态={currentState}，不允许执行该操作（期望状态={expectedState}）」）+ `ERR_B2B_PARTNER_PROFILE_INCOMPLETE`（D1 前置校验，参数 partnerCode/missingFields）+ `ERR_B2B_PARTNER_PASS_RATE_NOT_MET`（参数 partnerCode/passRate/threshold）+ `ERR_B2B_PARTNER_CERTIFICATION_NOT_MET`（参数 partnerCode/unpassedItems）。理由：(1) 守卫码对齐 b2b 域既有命名族（`ERR_B2B_EDI_DOC_ILLEGAL_TRANSITION`/`ERR_B2B_ASN_ILLEGAL_TRANSITION` 同构），错误码前缀 `erp.err.b2b.` 一致；(2) L1 异常路径「测试通过率不达标或必检项未通过」语义可分——统一门槛码会让测试断言与运维诊断无法区分失败原因（R1.30 E1 复用先例仅适用于同语义守卫，门槛失败是三独立语义）。统一门槛码否决：诊断粒度丢失 + 与 EDI/ASN 族既有「每动作独立码」模式不一致。StateMachine Bean 内部仍抛 common 层 `ERR_ILLEGAL_STATUS_TRANSITION`（契约 §2/§7），BizModel 映射为领域码（镜像 EdiDocBizModel:188-226 映射范式）。
      - Skill: `nop-backend-dev`
- [x] `Decision` **24h 监控形态裁决（D4）**：L1 字面「上线监控 24 小时」强制（partner-onboarding.md §生产监控表：EDI 发送失败率>5% 1h / 入站解析失败连续 3 条 / 证书即将过期 30 天 / 端点不可达连续 5 次）。**查询锚点裁决（必选，基线已登记结构缺口）**——选项 A（裁决候选）：间接路径（`PartnerProfile.allowedFormats` JSON → `EdiFormat.code` → `EdiDoc.formatId` 聚合窗口内 ERROR 失败率，org 级 scope 兜底）；选项 B：窗口语义收窄（监控窗口内仅统计伙伴关联 TestExchange 失败率 + 证书过期检查——规避 EdiDoc 无 partnerProfileId 的结构缺口，L1「上线监控」字面弱化）；选项 C：给 EdiDoc 加 partnerProfileId 列（触 ORM = ask-first，移出本行）。**决策记录理由 + 备选**（保持只读查询 + notify 派发，零写路径；任何 ORM 诉求登记 successor；监控窗口/失败率阈值 config 化）。
      **决策记录（已执行）**：**选项 A（间接路径锚点）**——job 扫描 PRODUCTION 且 `goLiveDate ∈ [now - monitorHours, now]` 的伙伴，逐伙伴：解析 `allowedFormats` JSON（formatCode 数组）→ 按 code 查 `ErpB2bEdiFormat` 收 formatId 集 → `ErpB2bEdiDoc` 过滤 `formatId ∈ ids AND orgId = profile.orgId AND createTime ∈ [goLiveDate, now]`（org 级 scope 兜底防跨伙伴误计）→ total/errorCount（state=ERROR）→ 失败率 > config `erp-b2b.onboarding-monitor-failure-rate`（默认 0.05，对齐 §生产监控表「EDI 发送失败率 > 5%」）→ `IErpSysNotificationBiz.notify` 派发 `b2b.onboarding-monitor-alert`（ROLE「B2B 管理员」，逐条失败隔离，无 ACTIVE 模板静默跳过 R1.4 范式）。理由：(1) L1「上线监控 24 小时」字面为**生产**首日业务 EDI 监控——TestExchange 是上线前测试数据非生产流量（选项 B 弱化 L1）；(2) allowedFormats→formatId 间接路径是零 ORM 变更下可得的伙伴级流量近似锚点（伙伴支持格式 = 该伙伴 EDI 事务格式），org 级收窄消除跨伙伴误计；(3) 监控窗口/失败率阈值 config 化保留运维调优面。选项 B 否决：监控对象错位（测试交换非生产流量）+ L1 字面弱化。选项 C（ORM 列）否决：触 ORM = ask-first 移出本行，**登记 successor**（触发条件 = 伙伴级 EDI 统计需求立项，与 D1 ERROR 阻断查询同 successor）。**job 形态**：R1.4 简单 job bean（低基数扫描 + 逐条 try/catch 失败隔离，镜像 ErpCtApprovalTimeoutEscalationJob/R1.35 范式）——否决 batch-task + REQUIRES_NEW helper（R1.23/R1.27 先例）的 R10 基线漂移（R1.34 D6 同型裁决：低基数扫描不值得 REQUIRES_NEW 事务边界）；扫描窗口内伙伴数小（PRODUCTION 且刚上线 24h 内），批处理语义不必要。监控事件常量 `b2b.onboarding-monitor-alert`（`ErpB2bConstants.NOTIFY_EVENT_ONBOARDING_MONITOR_ALERT`）+ context 键集 {partnerCode, partnerName, profileId, totalCount, errorCount, failureRate, goLiveDate, monitorHours}。
      - Skill: `nop-backend-dev`
- [x] `Proof` **既有测试误伤面核查**：grep erp-b2b 测试集 `ErpB2bPartnerProfile__activate|__suspend|__deactivate` 调用面——activate 增守卫后既有测试（若有）零误伤（守卫前态变更是否破坏既有 seed）；新增推进 mutation 与既有 EDI 测试零冲突。
      **核查结论（已执行）**：**零误伤**——(1) grep `__activate|__suspend|__deactivate|activate\(|suspend\(|deactivate\(` 跨 `module-b2b/erp-b2b-service/src/test` **零测试命中**（仅 dao 层接口定义 + BizModel 实现两处命中，见 git 工作区现状）——既有 10 测试类（AsnCrudSmoke/AsnInbound/AsnInventoryIntegration/EdiEnvelope/EdiPosting/MftTransport + statemachine 包 4 类）无任何 PartnerProfile 状态 mutation 调用面，守卫收紧（CERTIFIED 才能 activate / 非终态才能 suspend/deactivate）零误伤既有 seed；(2) 新增 promoteToTesting/promoteToCertified 为全新入口，与既有 EDI 测试（EdiDoc/Asn 状态机）零冲突（不同实体不同控制点）；(3) `TestErpAllJobYamlLoading` 兼容性核查：当前 23 个 job.yaml + 硬编码 `assertEquals(23,...)` 两处（:32/:46）——新增 `erp-b2b-onboarding-monitor.job.yaml` 后须 **23→24 同步更新**（计划内调整点，Phase 3 显式检查项）；job.yaml 结构（jobName/enabled/cronExpr/invoker）经 `LocalJobConfigLoader` 加载路径与既有 23 个同型，无兼容性风险。
      - Skill: `nop-testing`

Exit Criteria:

- [x] D1-D4 决策记录落盘（含理由 + 备选）+ 误伤面核查结论（零误伤或已识别调整点）
- [x] 推进门槛契约确认（promoteToTesting/promoteToCertified 前置校验集 + 错误码命名）

### Phase 2 - 状态守卫 + 推进 mutation + 字段回写落地（P1-RC-080 核心）

Status: completed
Targets: `ErpB2bPartnerProfileBizModel.java`；`IErpB2bPartnerProfileBiz.java`；`ErpB2bErrors.java`；`ErpB2bConstants.java`；`ErpB2bConfigs.java`
Skill: `nop-backend-dev`

- Item Types: `Add | Fix`
- Prereqs: Phase 1 完成

- [x] `Fix` **activate/suspend/deactivate 状态守卫**（`Decision | Fix`——守卫形态决策嵌入：activate 仅 CERTIFIED→PRODUCTION（+ 设 goLiveDate=now）；suspend 非终态→SUSPENDED；deactivate 非终态→TERMINATED（+ 设 archivedAt=now）；非法迁移抛 `ERR_B2B_PARTNER_ILLEGAL_TRANSITION`（D3 命名）。守卫形态 Decision：Processor/BizModel 内联 vs 实体状态机 Bean（对齐 b2b 域 ErpB2bEdiDocStateMachine/ErpB2bAsnStateMachine 先例——建议新增 `ErpB2bPartnerProfileStateMachine` Bean，契约 `entity-state-machine-bean.md`）。**接口 javadoc 同步为显式子项**：`IErpB2bPartnerProfileBiz` javadoc 现为「上线：CERTIFIED→PRODUCTION / 暂停：PRODUCTION→SUSPENDED / 终止：任意→TERMINATED」——守卫放宽（非终态→SUSPENDED/TERMINATED）与 javadoc 对齐更新，并记录 `SUSPENDED→原阶段` resume 缺失的不对称（Deferred 登记，防伙伴滞留）。
      **落地（已执行）**：新增 `ErpB2bPartnerProfileStateMachine` Bean（statemachine 包，严格无状态，12 边：promoteToTesting 1 + promoteToCertified 1 + activate 1 + suspend 4 + deactivate 5；Bean 抛 common 层码契约 §2/§7）；`ErpB2bPartnerProfileBizModel` 五 mutation 守卫经 Bean + `illegalTransition` 映射为 `ERR_B2B_PARTNER_ILLEGAL_TRANSITION`（镜像 EdiDocBizModel:188-226 范式，common 码作 cause）；activate 设 `goLiveDate=CoreMetrics.currentDate()` + deactivate 设 `archivedAt=CoreMetrics.currentTimestamp()`；`IErpB2bPartnerProfileBiz` javadoc 同步（promoteToTesting/promoteToCertified/activate 三文档 + suspend 四源 + deactivate 非终态），`SUSPENDED→原阶段` resume 缺失登记 Deferred（partner-onboarding.md 状态机图有但 UC-B2B-007 L1 未列，successor = 生产回退/暂停恢复需求立项）；beans.xml 注册 StateMachine Bean。
      - Skill: `nop-backend-dev`
- [x] `Add` **promoteToTesting/promoteToCertified @BizMutation**（契约 + 实现）：D1/D2 前置校验链（跨实体聚合经 `IErpB2bTestExchangeBiz`/`IErpB2bCertificationChecklistBiz` 注入）+ 状态迁移 + 门槛拒绝错误码。
      **落地（已执行）**：`promoteToTesting`——Bean 守卫 REGISTERED + D1 全配置校验 `assertProfileComplete`（partnerId/protocol/authMethod/transportEndpoint/allowedFormats 非空，缺失抛 `ERR_B2B_PARTNER_PROFILE_INCOMPLETE` 含缺项清单）；`promoteToCertified`——Bean 守卫 TESTING + `assertPassRateMet`（TestExchange 按 partnerProfileId findCount 聚合 passed/total，零行=0 拒绝，rate+1e-9<threshold 拒绝抛 `ERR_B2B_PARTNER_PASS_RATE_NOT_MET`[rate/threshold/missingKeyCases 入参] + TC-001/TC-004 前缀匹配必过[findList 内存过滤——XMeta 过滤算子白名单 [eq,in,dateBetween,dateTimeBetween] 无 startsWith，经 Java startsWith 判定]）+ `assertCertificationMet`（Checklist 按 partnerProfileId 聚合 isMandatory=true 行全 isPassed=true，零行=空清单拒绝，抛 `ERR_B2B_PARTNER_CERTIFICATION_NOT_MET` 含未通过项清单）；`IErpB2bTestExchangeBiz`/`IErpB2bCertificationChecklistBiz` 经 IBiz 注入零新增 daoFor 站点。
      - Skill: `nop-backend-dev`
- [x] `Add` **config 键登记**：`erp-b2b.onboarding-test-pass-rate`（0.9）/`erp-b2b.onboarding-production-monitor-hours`（24）登记 `ErpB2bConfigs`。
      **落地（已执行）**：`ErpB2bConfigs` 新增 4 键 + 默认值——`erp-b2b.onboarding-test-pass-rate`（0.9）/`erp-b2b.onboarding-production-monitor-hours`（24）/`erp-b2b.onboarding-monitor-failure-rate`（0.05，§生产监控表「EDI 发送失败率 > 5%」）/`erp-b2b.onboarding-monitor-cron`（默认 `0 0 * * * ?`，job.yaml cronExpr 与 bean 空值跳过共用单键模式 D5=R1.35 裁决同型）；`ErpB2bConstants` 6 伙伴状态常量 + `NOTIFY_EVENT_ONBOARDING_MONITOR_ALERT`；`ErpB2bErrors` 4 新码 + 参数表注册。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 状态守卫 + 推进 mutation 接线且运行时成立（GraphQL 实调：CERTIFIED 推进成功 / TESTING 跳 PRODUCTION 拒绝 / 门槛不达标拒绝 + 错误码断言）
- [x] goLiveDate/archivedAt 回写断言（GraphQL 实调后 DAO 落库检查）

### Phase 3 - 24h 上线监控（P1-RC-080 ④）

Status: completed
Targets: 新 `ErpB2bOnboardingMonitorJob.java`（或等价）；`app-erp-all/src/main/resources/_vfs/nop/job/conf/erp-b2b-onboarding-monitor.job.yaml`；`ErpB2bConfigs.java`；`ErpB2bConstants.java`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 2 完成

- [x] `Add` **监控 job**（D4 形态 + 查询锚点裁决）：cron 门控扫描监控窗口内 PRODUCTION 伙伴 → 按 D4 锚点查询失败率（ERROR 聚合）→ 超阈值 notify `b2b.onboarding-monitor-alert`（逐条失败隔离 + 无 ACTIVE 模板静默跳过 R1.4 范式）；job.yaml 注册于 app-erp-all。**R10 基线预声明**：若按 R1.23/R1.27 先例引入 REQUIRES_NEW 失败隔离 helper，compliance R10 基线 +1 须 per-site 证据登记（镜像先例），Closure Gate 的 `actual ≤ baseline` 判定以此为准。
      **落地（已执行）**：`app.erp.b2b.service.job.ErpB2bOnboardingMonitorJob`（R1.4 简单 job bean 范式，D4 裁决否决 batch-task REQUIRES_NEW helper 的 R10 漂移——R1.34 D6 同型：低基数扫描不值得 REQUIRES_NEW 事务边界）——`execute()` 无参（BeanMethodJobInvoker）+ cron 空值跳过 + `runMonitor`[PRODUCTION + `dateBetween(goLiveDate, today-⌈monitorHours/24⌉, today)` 扫描窗口内伙伴（XMeta 白名单无 ge/le 经 dateBetween/dateTimeBetween 表达）] + `monitorPartner`[allowedFormats JSON→EdiFormat.code→formatId 集（空集/解析失败跳过）→ EdiDoc `in(formatId) + eq(orgId) + dateTimeBetween(createTime, goLiveDate, now)` 计数 total/ERROR → 失败率>config（默认 0.05）→ `IErpSysNotificationBiz.notify` 派发 `b2b.onboarding-monitor-alert`[context 键集 partnerCode/partnerName/profileId/totalCount/errorCount/failureRate/goLiveDate/monitorHours，ROLE「B2B 管理员」模板契约] ] + 逐伙伴 try/catch WARN 失败隔离 + 无 ACTIVE 模板 notify 内部静默跳过；`erp-b2b-onboarding-monitor.job.yaml`（app-erp-all 注册：enabled 默认 false + cronExpr `@cfg:erp-b2b.onboarding-monitor-cron|0 0 * * * ?` 单键门控 + invoker erpB2bOnboardingMonitorJob.execute）；beans.xml 注册 job bean；`erp-b2b-service/pom.xml` 增 compile `app-erp-notify-dao` + test `app-erp-notify-service`（对齐 finance 先例）；`TestErpAllJobYamlLoading` 23→24 同步（Phase 1 Proof 计划内调整点）。
      - Skill: `nop-backend-dev`
- [x] `Add` notify 事件常量 + context 键集（D4 契约）。
      **落地（已执行）**：`ErpB2bConstants.NOTIFY_EVENT_ONBOARDING_MONITOR_ALERT = "b2b.onboarding-monitor-alert"`（<域>.<主题> 命名空间对齐 notification-strategy.md）+ context 键集 {partnerCode, partnerName, profileId, totalCount, errorCount, failureRate, goLiveDate, monitorHours}（job `notifyAlert` 构建，模板约定见 D4 决策记录）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] job 级测试运行时成立（窗口内伙伴扫描 / 失败率超阈值通知落库 + 未超阈值零动作 / cron 空值跳过 / 单条失败隔离）

### Phase 4 - 测试矩阵

Status: completed
Targets: `module-b2b/erp-b2b-service/src/test/java/app/erp/b2b/service/`（新增 `TestErpB2bPartnerOnboarding`）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 2-3 完成

- [x] `Add` 测试组（按 Goals）：① 状态守卫（非法迁移拒绝矩阵[REGISTERED 跳 PRODUCTION/TESTING 跳 PRODUCTION/终态再操作] + 合法迁移成功）；② 推进门槛（pass-rate 达标/不达标拒绝 + TC-001/TC-004 必过 + 认证清单 isMandatory 未全过拒绝）；③ 字段回写（goLiveDate/archivedAt 断言）；④ 24h 监控 job（超阈值通知 + 未超零动作 + 失败隔离，锚点按 D4 裁决）；⑤ GraphQL RPC 冒烟 + 快照录制（拒绝路径直断言范式，对齐 R1.30/R1.31）。
      **落地（已执行）**：`TestErpB2bPartnerProfileStateMachineMatrix`（statemachine 包，层 1 纯矩阵 10 组：无重复/冲突边 12 边/终态无出边/五动作来源态合法集 + 非法集/common 码断言）——零 IoC `new` 实例化；`TestErpB2bPartnerOnboarding`（service 包，直断言范式 R1.32/33 拒绝路径不录快照）13 组：① 守卫矩阵 2（非法迁移拒绝 9 断言[REGISTERED/TESTING 直跳 PRODUCTION + 非法推进 + 终态再操作 + 已暂停再暂停] + 零落库断言 + 合法全生命周期[promoteToTesting→promoteToCertified→activate→suspend→deactivate]）② 门槛 6（基本配置不完整/通过率 2/3 拒绝/TC-004 缺失拒绝/空清单拒绝/必检项未全过[非必检不过放行]拒绝/门槛全满足 CERTIFIED）③ 字段回写（activate goLiveDate=FROZEN_TODAY + deactivate archivedAt 非空 DAO 断言）④ 监控 job 6（超阈值通知落库 + recipientUserId 断言/未超阈值 3.3% 零动作 + 窗口外独立格式防泄漏/坏 JSON 失败隔离 + 正常伙伴告警 subject+body 渲染断言/cron 空值跳过/无 ACTIVE 模板静默跳过）——全部经 IGraphQLEngine GraphQL RPC 实调（守卫/门槛/回写）+ 手工 wiring job（setter 注入镜像 TestErpCtApprovalTimeoutJob 范式）。
      - Skill: `nop-testing`
- [x] `Proof` 既有 erp-b2b-service 测试零回归：`mvn test -pl module-b2b/erp-b2b-service`（57 基线 + 新增全绿）。
      **核查结论（已执行）**：`mvn test -pl module-b2b/erp-b2b-service` **80/80 全绿**（57 基线 10 类零回归 + TestErpB2bPartnerProfileStateMachineMatrix 10 + TestErpB2bPartnerOnboarding 13，0 failures / 0 errors）。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 新增测试组全绿 + erp-b2b-service 全模块零回归（BUILD SUCCESS）
- [x] 守卫/门槛/回写/监控有运行时断言证据（GraphQL RPC 实调，非仅静态接线）

### Phase 5 - 文档回填 + arm-index/roadmap 状态

Status: completed
Targets: `docs/design/b2b/partner-onboarding.md`；`docs/audits/arm-index.md`；`docs/backlog/requirement-compliance-roadmap.md`；`docs/logs/2026/08-15.md`
Skill: none

- Item Types: `Add | Fix`
- Prereqs: Phase 1-4 完成

- [x] `Add` owner doc 注记：四阶段状态机实现注记（守卫/推进 mutation/门槛聚合/字段回写/24h 监控 + D1-D4 裁决摘要 + config 键消费）；不修改需求契约段（use-cases L1 不动）。
      **落地（已执行）**：`partner-onboarding.md` 新增「## 实现注记（RC-R1.36，plan `2026-08-15-1023-3`）」段（状态机 Bean 12 边矩阵 + 回退/resume 未编码 Deferred 声明 + 推进门槛链 + ERROR 阻断查询未实现 successor + 字段回写 + 24h 监控 job 形态/锚点/阈值 + config 4 键 + 测试证据；契约段未动）。
      - Skill: none
- [x] `Add` arm-index P1-RC-080 → `done (RC-R1.36)` + 修复落地摘要；roadmap RC-R1.36 → done ✅（含落地摘要）；`docs/logs/2026/08-15.md` 日志条目写入。
      **落地（已执行）**：arm-index P1-RC-080 → `done (RC-R1.36)`（含修复落地摘要 + 三 Deferred 登记）；roadmap RC-R1.36 → done ✅（含落地摘要）；`docs/logs/2026/08-15.md` 顶部新增本计划日志条目。
      - Skill: none

Exit Criteria:

- [x] arm-index/roadmap 状态回填 + owner doc 注记落盘 + 日志条目写入

## Draft Review Record

- Independent draft review iteration 1: needs revision（独立子代理 ses_ffcb86b29ffem6gV0GUr4sSDnV）— 0 BLOCKER / 1 MAJOR / 7 MINOR。MAJOR：24h 监控 per-partner 查询无结构锚点（partnerProfileId 不存在于 EdiDoc/EdiLog，仅间接路径）→ 修订：基线登记锚点缺口 + D4 扩展三选项裁决（间接 allowedFormats→formatCode / 窗口收窄 / ORM 移出）+ D1 实体勘正（blockingLevel 在 EdiDoc 非 EdiLog）；MINOR 折叠：测试类 10 类 / javadoc 同步显式 + resume 不对称 Deferred / ORM 实体范围勘正 / R1.35 先例改已落地先例 / Phase 2 双标签 / R10 预声明。
- Independent draft review iteration 2: needs revision（独立子代理 ses_ffcad4e40ffeEjtLK255UFOMZe）— 0 BLOCKER / 1 MAJOR（残留「若实现」两处 + 反松弛规则）/ 3 MINOR（R1.35 残留引用两处 / ORM 范围勘正未生效 / Draft Review Record 未更新）。修订：Goals/预授权判据/Infra/Phase 3 全部去除「若实现」措辞 + R1.35 引用改 R1.2/R1.23/R1.27 已落地先例 + ORM 范围 :443-475/:478-507 + 本条记录补全。
- Independent draft review iteration 3: accept（独立子代理 ses_ffca73bd1ffeadvQ8SQRbu0Jkc）— 0 BLOCKER / 0 MAJOR / 2 MINOR（全部折叠）：D1 行号锚点精化（blockingLevel 列 :174 / ediDocId 列 :334-to-one :351）+ D4 重复短语清理。**计划可标记 active。**

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。**完整仓库验证在此处**：结束时运行一次全量验证。

- [x] 范围内行为完成——P1-RC-080 状态守卫/推进门槛/字段回写/24h 监控运行时成立（独立结束审计逐文件核验）
- [x] 相关文档对齐——arm-index/roadmap/owner doc/日志回填（独立结束审计核验）
- [x] 已运行验证（`mvn test -pl module-b2b/erp-b2b-service` 全绿 + `mvn clean install -DskipTests` 全量 BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline）
- [x] 无范围内项目降级为 deferred/follow-up（Deferred But Adjudicated 各项有 successor 分类与触发条件，无已确认缺陷/契约漂移）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

（结束审计定稿登记，全部有 successor 分类与触发条件，无已确认缺陷/契约漂移：

- **blocking_level=ERROR 阻断查询**（promoteToCertified 前置三门槛之一，D1 裁决本行不实现）：
  - Classification: `watch-only residual`
  - Why Not Blocking Closure: EdiDoc/EdiLog 无 partnerProfileId 锚点（基线已登记结构缺口），伙伴级 ERROR 聚合须经 allowedFormats 间接路径（语义近似非伙伴专属）；本行门槛 = 通过率聚合 + 必检清单（L1 字面双门槛已覆盖）。
  - Successor Required: `yes`（触发条件 = 伙伴级 EDI 统计需求立项，与 EdiDoc 加列同 successor）
- **PRODUCTION→TESTING 回退 mutation + SUSPENDED→原阶段 resume**（生产回退保护 + 暂停恢复）：
  - Classification: `out-of-scope improvement`
  - Why Not Blocking Closure: L1 UC-B2B-007 基本流程未列回退边/resume（partner-onboarding.md 状态机图有，但本行守卫主路径已阻断非法迁移；SUSPENDED 无 resume 会滞留伙伴——不对称已登记，非本行义务但须 successor 消除）。
  - Successor Required: `yes`（触发条件 = 生产回退/暂停恢复需求立项）
- **EdiDoc 加 partnerProfileId 列**（监控查询锚点 ORM 诉求，D4 选项 C）：
  - Classification: `out-of-scope improvement`
  - Why Not Blocking Closure: 触 ORM = ask-first，D4 已裁决以间接路径（allowedFormats→formatId + org 级 scope）规避；列增设须人工裁决。
  - Successor Required: `yes`（触发条件 = 伙伴级 EDI 统计需求立项）
- **监控窗口内重复告警去重**（超阈值伙伴每次扫描重复告警直至窗口结束）：
  - Classification: `watch-only residual`
  - Why Not Blocking Closure: 需「已通知」标记列 = 触 ORM = ask-first，D4 已注记已知行为（R1.4 简单 job bean 无状态记忆）；参考应用语义可接受。
  - Successor Required: `yes`（触发条件 = 伙伴级 EDI 统计需求立项 / 告警去重需求立项）
- **真实 EDI 测试执行流程**（测试消息交换自动触发）：
  - Classification: `watch-only residual`
  - Why Not Blocking Closure: 通过率聚合只读已记录数据，测试执行经既有 EdiDoc 链路；本行推进门槛校验不依赖测试自动化。
  - Successor Required: `yes`（触发条件 = 测试自动化立项）
- **凭证轮换/证书到期自动停用**（partner-onboarding.md 业务规则 4/5）：
  - Classification: `out-of-scope improvement`
  - Why Not Blocking Closure: 属 P2-RC-068/MFT 证书生命周期范围，非 UC-B2B-007 上线门槛义务。
  - Successor Required: `yes`（触发条件 = MFT 证书生命周期行启动）
- **前端上线向导/推进按钮 AMIS 接线**：
  - Classification: `watch-only residual`
  - Why Not Blocking Closure: 本行落地后端 mutation 能力面（能力面 = L1 后端义务）；前端按钮接线属 UI 增强。
  - Successor Required: `no`

## Closure

Status Note: RC-R1.36（P1-RC-080 UC-B2B-007 伙伴上线状态机推进）全部 5 Phase 完成：Phase 1 D1-D4 裁决落盘 + 误伤面核查（零误伤，TestErpAllJobYamlLoading 23→24 计划内调整点）；Phase 2 落地 `ErpB2bPartnerProfileStateMachine`（12 边）+ `ErpB2bPartnerProfileBizModel` 五 mutation 守卫化 + 推进门槛链（配置完整/pass-rate≥0.9 零行拒绝/TC-001/004 必过/必检清单全过空清单拒绝）+ goLiveDate/archivedAt 回写 + config 4 键/错误码 4 个/常量 + IBiz 契约同步；Phase 3 落地 `ErpB2bOnboardingMonitorJob`（R1.4 简单 job bean，D4 间接锚点 + org 级 scope，超 5% notify `b2b.onboarding-monitor-alert` ROLE「B2B 管理员」）+ `erp-b2b-onboarding-monitor.job.yaml` + pom notify 依赖 + TestErpAllJobYamlLoading 24；Phase 4 `TestErpB2bPartnerProfileStateMachineMatrix` 10 组 + `TestErpB2bPartnerOnboarding` 13 组全绿 + erp-b2b-service 80/80 零回归；Phase 5 回填 arm-index P1-RC-080 `done (RC-R1.36)` + roadmap RC-R1.36 done ✅ + owner doc 实现注记 + 日志条目。全量验证：`mvn clean install -DskipTests` BUILD SUCCESS + compliance checker 全 16 规则 actual == baseline 零漂移（新方法全经 IBiz 注入零新增 daoFor 站点 + R1.4 job 无 REQUIRES_NEW，R2c=1399/R10=9 不变）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 ses_ffb9a4853ffegut1DoQ70AWck7（新会话结束审计）
- Evidence: 独立审计 5 门——(1) 计划一致性：5 Phase 全 [x]/completed + 全 Exit Criteria [x]（审计时 Closure Gates/Plan Status 未填，本闭包补齐后经计划文本一致性复查）；(2) 代码存在性：StateMachine 12 边/BizModel 五 mutation 守卫+门槛链+回写/IBiz 契约/job bean/job.yaml/config 4 键/错误码 4 个/常量/beans.xml/两测试类逐文件核验 PASS；(3) 反模式：R5 零 @Inject private、R7 零 System.currentTimeMillis、R3 零 new Erp*()、R2c 零新增 daoFor、ORM 零变更（git diff module-b2b/model 空）；(4) 文档回填：arm-index `done (RC-R1.36)` + roadmap done ✅ + owner doc 实现注记 + 日志条目 PASS；(5) 验证复跑：TestErpB2bPartnerOnboarding 13/13 + 全模块 80/80 + TestErpAllJobYamlLoading 1/1（24 个 job.yaml）+ compliance checker actual ≤ baseline（R2c=1399/R10=9/R12c=40）PASS；全量 `mvn clean install -DskipTests` 由执行者运行 BUILD SUCCESS（审计未复跑 156 模块全量，接受记录证据并注明依赖可解析佐证）。审计结论：实现/测试/文档/验证全 PASS，仅闭包状态未填（本闭包补齐）。

Follow-up:

- blocking_level=ERROR 阻断查询 + EdiDoc 加 partnerProfileId 列（同 successor：伙伴级 EDI 统计需求立项，watch-only/out-of-scope）
- PRODUCTION→TESTING 回退 + SUSPENDED→原阶段 resume（生产回退/暂停恢复需求立项，out-of-scope improvement，SUSPENDED 滞留不对称已登记）
- 监控窗口内重复告警去重（需 ORM 标记列 = ask-first，watch-only residual）
- 真实 EDI 测试执行自动化（测试自动化立项，watch-only residual）
- 凭证轮换/证书到期自动停用（P2-RC-068 MFT 证书生命周期行，out-of-scope improvement）
- 前端上线向导/推进按钮 AMIS 接线（watch-only residual，successor no）
