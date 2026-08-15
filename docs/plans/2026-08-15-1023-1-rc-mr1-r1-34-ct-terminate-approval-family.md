# 2026-08-15-1023-1-rc-mr1-r1-34-ct-terminate-approval-family RC-R1.34 — contract 终止与审批族（MR1 第一批纯预授权）

> Plan Status: completed
> Last Reviewed: 2026-08-15
> Mission: requirement-compliance
> Work Item: RC-R1.34（P1-RC-076 UC-CT-06 terminate 法务审批门控/截停 InvoicePlan/版本归档/善后 TODO + P1-RC-077 UC-CT-07 ApprovalWorkflowEngine，同域同修复方式族）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.34 行 + `docs/audits/arm-index.md` P1-RC-076/077 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`（G8 contract 终止审批族，纯 BizModel 审批/终止编排）
> Related: `docs/design/contract/use-cases.md`（L1 UC-CT-06/07）；`docs/design/contract/state-machine.md`（§2/§6 terminate 法务审批契约 + §NEGOTIATION）；`docs/design/contract/approval-workflow.md`（审批引擎设计 + 配置点）；`docs/audits/2026-08-08-0135-rc-ma4-a4-2-155-162-contract-runtime.md`（A4.2.159/160 运行时证据）；`docs/plans/2026-08-15-0456-2-rc-mr1-r1-32-ct-create-validation-version-family.md`（同域前行，submit/rejectAmend/版本语义）
> Audit: required

## Current Baseline

- **finding P1-RC-076（arm-index 行，UC-CT-06 A/C/D）**：L1（`use-cases.md:121-128/132`）逐字「提交法务审批 / 法务审批通过后，系统执行终止操作[合同→TERMINATED + 当前版本 isCurrent=false 归档 + 截停所有未执行 InvoicePlan + 生成 TODO 善后结算] / 法务驳回 → 合同保持原状态」。L3 实仓：
  - `ErpCtContractBizModel#terminate:161-179` **单向翻转**——守卫 `status∈{ACTIVE,NEGOTIATION}`（Bean `assertCanTerminate` 多源）+ `setStatus(TERMINATED)+updateEntity`，**零法务审批门控/角色校验**（grep `legalApproval|法务.*审批|approveTerminate` 零命中）+ **零显式截停**（`:172-175` 注释自承「InvoicePlan 无独立状态列经合同头隐式失效」）+ **零 TODO 生成** + **零版本 isCurrent=false 归档**（terminate 不触碰 ErpCtContractVersion）+ **零法务驳回路径**。
  - 版本实体 `ErpCtContractVersion`（orm.xml）：`isCurrent`（propId 7）+ `status`（dict `erp-ct/version-status` DRAFT/FINALIZED/SIGNED）+ `approvedBy/approvedAt`——归档载体（isCurrent 翻转）已就绪。
  - InvoicePlan（orm.xml:274-302）：**无独立状态列**（isInvoiced/invoiceBillCode/invoiceDate/invoiceTerm/remark），A4.2.159 已证实 `triggerInvoice:35-54` 对 TERMINATED 合同拒绝（隐式失效运行时成立，无显式截停标记）。
  - **行号基线注记**：arm-index P1-RC-076 行引用的 `terminate:97-113`/`:107-110` 为 R1.32 前陈旧基线；本计划基线以实时仓库实测为准（`ErpCtContractBizModel.java:161-179`，含 `:172-175` 注释自承）。
  - 善后 TODO：**全域无 TODO 实体**（grep `generateTodo|善后` 零命中）——TODO 落点须 Decision（见 Execution Plan Phase 1）。
- **finding P1-RC-077（arm-index 行，UC-CT-07 A/B/C/D）**：L1（`use-cases.md:141-143`）逐字「系统读取 ErpCtApprovalMatrix，按 totalAmount 匹配适用的审批节点列表 / 生成 ErpCtApprovalRecord（每节点一条），第一个节点激活（PENDING），其余 WAITING / 审批人逐节点审批，通过后激活下一节点 / 所有节点通过后合同可进入 ACTIVE 状态 / 驳回超限（默认 3 次）后锁定需强制升级；超时未处理（默认 72h）升级通知上一级」。L3 实仓：
  - `ErpCtApprovalMatrixBizModel`/`ErpCtApprovalRecordBizModel` 均 17 行 CRUD 桩（仅 setEntityName）。
  - ORM 实体+配置字段**已存在** ✅：`ErpCtApprovalMatrix`（code/orgId/minAmount/maxAmount/approverRole mandatory/approvalOrder mandatory/contractType/allowSkip/isActive/remark + UK_CT_APPROVAL_MATRIX_CODE_ORG）；`ErpCtApprovalRecord`（contractId mandatory/orgId/approvalMatrixId/approvalOrder/approverId `stdDomain=userId`/approvalStatus dict `erp-ct/approval-status` WAITING/PENDING/APPROVED/REJECTED/SKIPPED mandatory/comment/approvedAt/rejectedAt/remark）。
  - grep `matchMatrix|generateApprovalRecord|rejectCount|rejectLimit|72h|timeout.*escalat` 跨 erp-ct-service/src/main **零命中**——无任何工作流逻辑。
  - **rejectCount 无 ORM 列**（ApprovalRecord 字段集无计数器列）——驳回超限计数须 Decision（派生计数 vs ORM 列；本行为纯预授权行，触 ORM 须 ask-first 不在本行）。
  - **approverId 角色解析载体**：ApprovalMatrix.approverRole 为角色编码串；`ErpCtApprovalRecord.approverId` 为 userId——角色→用户解析须 Decision（跨域 master-data 查询 or 平台 nop-auth 角色查询，对齐 A1.51 notify ROLE resolver 证据 `resolveRole:117-156` 双过滤范式）。
- **state-machine.md §6 契约**（`:101/103`）：`ACTIVE→SUSPENDED | 合同管理员（需法务审批）`、`ACTIVE→TERMINATED | 合同管理员 + 法务审批`、版本审批法务/合规部门、`:165-166` 提前终止需法务审批 + 终止协议签署确认——terminate 门控的 owner-doc 义务明确（§6 无 Deferred 标注，Q4=(a) 强制实现）。
- **approval-workflow.md 配置点**（`:164-172`）：`erp-ct.approval-enabled`(false) / `erp-ct.approval-max-retries`(3) / `erp-ct.approval-timeout-hours`(72) / `erp-ct.approval-urgent-threshold`(500000) / `erp-ct.amendment-reapproval-threshold`(0.2)——config 键已设计未登记 `ErpCtConfigs`（当前仅有 volume-discount/rebate/invoiceplan/e-signature 键，见 R1.33 注记）。**72h 超时升级须 scheduler/job + `IErpSysNotificationBiz`**——纯预授权调度接线（对齐 R1.4 `erp-hr-leave-approver-timeout` job 范式 + R1.23/R1.27 batch-task 先例，job.yaml 注册权威点 `app-erp-all/src/main/resources/_vfs/nop/job/conf/`）。
- **R1.32/R1.33 前行交互**：`submit`（DRAFT→NEGOTIATION）已落地（R1.32）——审批引擎的触发入口 = submit 后生成 Records（UC-CT-07 触发条件「经办人提交合同」）；`activate` 经 `ErpCtContractActivateProcessor`（:42-54，含 `assertCanActivate` NEGOTIATION 守卫 + type/direction 校验 + FINALIZED→signVersion 副作用）——审批全通过→可 ACTIVE 的联动接线在本行（Decision：activate 前置审批全通过校验 vs 独立联动）。
- **测试基线**：erp-ct-service ≈ 110 tests（R1.33 结束审计 110/110 全绿基线）；既有 `TestErpCtContractTerminate`（terminate 状态翻转强测）——本行改 terminate 行为须零回归。
- **预授权判据**（第一批纯预授权）：纯 BizModel/Processor/Bean 代码逻辑 + 调度接线（job.yaml + config + notify），**不触 ORM 结构/会计过账/数据删除**（InvoicePlan 截停经逻辑删除 `useLogicalDelete=true` 或 remark 注记 Decision；rejectCount 派生计数；无新列）；roadmap RC-R1.34 行 `todo`，Deps（R1.0 done）已满足。
- **涉及文件**：`ErpCtContractBizModel.java`（terminate 改造 + approveTerminate）；`ErpCtApprovalMatrixBizModel.java`/`ErpCtApprovalRecordBizModel.java`（引擎编排或新 `ErpCtApprovalWorkflowEngine`）；`IErpCtApprovalMatrixBiz.java`/`IErpCtApprovalRecordBiz.java`/`IErpCtContractBiz.java`（契约）；`ErpCtErrors.java`/`ErpCtConstants.java`/`ErpCtConfigs.java`；新 job（`ErpCtApprovalTimeoutEscalationJob` 或等价）+ `app-erp-all/_vfs/nop/job/conf/erp-ct-approval-timeout.job.yaml`；`beans.xml` 注册；测试类（新增 `TestErpCtApprovalWorkflow` 等）；owner doc（approval-workflow.md/state-machine.md 注记）+ arm-index + roadmap + `docs/logs/2026/08-15.md`（回填）。

## Goals

- **terminate 法务审批门控运行时成立（P1-RC-076 ①③⑤）**：terminate 流程两段化——①发起终止申请（原因/附件 → 触发法务审批）；②法务审批通过后执行终止操作（合同→TERMINATED + 当前版本 isCurrent=false 归档 + 显式截停未执行 InvoicePlan + 生成善后 TODO）；法务驳回 → 合同保持原状态（Decision：门控形态 = 独立 approveTerminate mutation vs 复用 ApprovalWorkflowEngine 法务节点）。
- **terminate 副作用全落地（P1-RC-076 ②④）**：显式截停未执行 InvoicePlan（isInvoiced=false 行，Decision：逻辑删除 vs remark 注记——隐式失效已成立，本行补显式标记）+ 当前版本 isCurrent=false 归档 + 善后结算 TODO（Decision：落点载体）。
- **ApprovalWorkflowEngine 运行时成立（P1-RC-077 ①-④）**：`matchByAmount`（读 ApprovalMatrix 按 totalAmount 匹配节点列表，approvalOrder 升序）+ `generateRecords`（每节点一条：首 PENDING 其余 WAITING）+ `approve/reject` 逐节点审批（通过激活下一节点 / 驳回保持 NEGOTIATION + 通知）+ 全通过 → 合同可 ACTIVE（activate 联动 Decision）+ **驳回重提循环（D7：重新激活驳回节点及其后续节点，派生计数递增）** + 驳回超限锁定（D3：派生 rejectCount ≥ max-retries）+ 超时 72h 升级 job（scheduler + notify）。
- **config 键登记**：approval-workflow.md 五键（approval-enabled/approval-max-retries/approval-timeout-hours/approval-urgent-threshold/amendment-reapproval-threshold）登记 `ErpCtConfigs`——`erp-ct.approval-enabled` 默认 false 门控引擎整体接线（config-gate = 部署启用决策，对齐 A4.1.4 范式）；dead 键恢复消费。
- **测试**：terminate 两段化（发起→法务通过→副作用全落地 / 驳回→原状态）；引擎（金额匹配节点/逐节点推进/全通过 activate 联动/驳回激活下一节点）；超限锁定；72h 升级 job（batch 或 job 级测试）；GraphQL RPC 冒烟 + 快照录制（拒绝路径直断言范式，对齐 R1.32/R1.33）。
- **零回归**：erp-ct-service 既有测试全绿（110 基线）+ 全仓构建 + compliance checker 零漂移（新 helper REQUIRES_NEW 若引入须 per-site 证据登记，镜像 R1.23/R1.27 R10 先例）。
- **回填**：arm-index P1-RC-076/077 → `done (RC-R1.34)` + roadmap 行 → `done` + owner doc 注记 + `docs/logs/` 日志条目。

## Non-Goals

- **不实现 P1-RC-074/075**（invoiceTerm 生成/已开票锁/消耗计费——独立行 RC-R1.33 已 done）。
- **不实现 P1-MA2-071**（到期自动化 job——独立行 RC-R1.35）。
- **不实现 P1-RC-078/079**（折扣消费方接线/文档仓库——独立行 RC-R1.79/R1.80 越界项）。
- **不触 ORM 结构**（零列/零索引/零 UK 变更——rejectCount 派生计数、截停经既有 useLogicalDelete 逻辑删除或 remark、approverId 复用既有列；任何新列须 ask-first 移出本行）。
- **不改 triggerInvoice 既有行为**（TERMINATED 隐式失效已证实 A4.2.159，本行只补显式截停标记不触碰触发守卫）。
- **不做前端 AMIS 接线**（审批操作按钮/终止向导均不在本行；后端 mutation 提供能力面）。
- **不改真相源契约段落**（use-cases L1 不动）。
- **不实现审批撤回/转交**（approval-workflow.md §业务规则 5/6 撤回 NEGOTIATION→DRAFT + 转交——L1 UC-CT-07 未列，属设计扩展，Deferred 登记）。**驳回重提（step 6）不在本 Non-Goal 之列**——按 D7 裁决纳入范围或显式登记（见 Phase 1）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧的预授权代码逻辑修复，Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/contract/use-cases.md`（L1 UC-CT-06/07）+ `docs/design/contract/state-machine.md`（§2/§6 terminate 法务审批契约）+ `docs/design/contract/approval-workflow.md`（引擎设计 + 配置点）+ `docs/audits/2026-08-08-0135-rc-ma4-a4-2-155-162-contract-runtime.md`（A4.2.159/160 运行时证据）
- Skill Selection Basis: 实现面 = CrudBizModel + per-mutation Processor + 实体状态机 Bean + notify 派发 + job/scheduler 接线（`nop-backend-dev`——跨实体经 IBiz 注入 + 角色→用户解析 + 失败隔离 helper 范式）；测试（`nop-testing`：JunitAutoTestCase + GraphQL RPC + 快照 + notify 模板 seed 范式——对齐 R1.4/R1.23/R1.33 测试范式）。无 view.xml/xbiz/ORM 变更。

## Infrastructure And Config Prereqs

- 无新外部服务/环境变量。config key 登记 `ErpCtConfigs`：`erp-ct.approval-enabled`（默认 false）/`erp-ct.approval-max-retries`（3）/`erp-ct.approval-timeout-hours`（72）/`erp-ct.approval-urgent-threshold`（500000）/`erp-ct.amendment-reapproval-threshold`（0.2，若本行引入）/72h 升级 job cron（`erp-ct.approval-timeout-cron` 或 job.yaml `@cfg` 门控，Decision）。
- 72h 升级 job.yaml 注册于 `app-erp-all/src/main/resources/_vfs/nop/job/conf/erp-ct-approval-timeout.job.yaml`（对齐 erp-hr-leave-approver-timeout 范式：`enabled: '@cfg:nop.job.erp-ct-approval-timeout.enabled|false'` + cronExpr `@cfg:...|0 0 1 * * ?'` + invoker bean/method）。
- notify 依赖：erp-ct-service pom 已含 notify-dao（compile）+ notify-service（test）（R1.33 已加）——直接复用；通知事件常量（`ct.approval-task` / `ct.approval-timeout-escalation` 等，Decision）登记 `ErpCtConstants`。
- 分域验证前置：`mvn install -DskipTests` 后 `mvn test -pl module-contract/erp-ct-service`。

## Execution Plan

### Phase 1 - Explore 门控形态与载体裁决（Decision）

Status: completed
Targets: `ErpCtContractBizModel.java`；`ErpCtApprovalMatrixBizModel.java`；`ErpCtApprovalRecordBizModel.java`；`ErpCtConfigs.java`；`ErpCtConstants.java`；`ErpCtErrors.java`
Skill: `nop-backend-dev`

- Item Types: `Decision | Proof`
- Prereqs: 无（既有基线）

- [x] `Decision` **terminate 法务门控形态裁决（D1）**：选项 A（裁决候选）：复用 ApprovalWorkflowEngine 生成法务审批节点（terminate 申请 → 引擎生成 Legal 节点 PENDING → 法务 approve 后执行终止副作用）——统一引擎单一真相源 + UC-CT-07「所有节点通过后可 ACTIVE」与「法务审批通过后执行终止」语义同构；选项 B：独立轻量 approveTermination mutation（合同管理员发起 + 法务角色校验 IUserContext.isUserInRole + 独立审批记录）——与审批引擎双轨语义重复。**决策记录理由 + 备选**；选项 A 时须裁决终止申请载体（Decision：新 ErpCtTerminationRequest 实体[触 ORM ask-first 移出] vs 合同 remark + 引擎法务节点[零 ORM]）。
      **决策记录（已执行）**：**选项 B（独立 approveTermination/rejectTermination mutation + ErpCtApprovalRecord 复用为法务审批记录载体，approvalMatrixId=null 判别）**。理由：(1) **法务门控强制性与 config-gate 语义冲突**——state-machine.md §6 `ACTIVE→TERMINATED | 合同管理员 + 法务审批` 无 Deferred 标注（Q4=(a) 强制义务），门控须无条件生效；A 方案将门控挂入 config-gated 引擎（approval-enabled 默认 false = 零生成零阻塞），须为 terminate 破例强制生成法务节点，config-gate 语义破裂（部署关闭审批流 ≠ 关闭法务终止门控）；(2) **生命周期阶段异构**——submit 链面向 NEGOTIATION 提交（多节点推进 + 全通过→activate 联动），terminate 面向 ACTIVE/NEGOTIATION 提前终止（单法务节点 + 通过→终止副作用）；A 方案须在引擎 approve 内按记录目的分支（approvalMatrixId null 判别）执行终止终局动作，引擎与合同生命周期耦合扩大；(3) **副作用归属**——终止副作用（版本归档/截停/TODO 通知）是合同域业务逻辑，归 ErpCtContractBizModel 内聚；引擎保持审批通用编排不感知合同终局动作；(4) **载体复用零新实体**——ApprovalRecord（approvalMatrixId=null + approvalOrder=1 + approverId 按 D2 解析 + PENDING）+ 审批轨迹可审计（approvedAt/rejectedAt/comment），「双轨」仅指 mutation 入口不同，记录生命周期语义与引擎记录一致。**法务角色载体裁决**：新增 config 键 `erp-ct.terminate-approver-role`（默认「合同审批人」——nop_auth_role.csv 冻结词表内 ct 域既有 roleId；approval-workflow.md 的 `LEGAL_MANAGER` 等角色码为部署配置语义，运行时以 nop-auth roleName 为准），terminate 记录 approverId 经 D2 解析。**终止申请信息载体**：record.remark 承载 `reason`（+ 可选 `[附件:{attachmentId}]` 引用，零 ORM）；新 ErpCtTerminationRequest 实体（ask-first 越界）否决。**approveTermination 守卫**：记录 PENDING + approvalMatrixId==null + approverId 匹配（null 时任意操作员——D2 无命中手工指定语义）。备选 A（引擎复用）否决理由：统一入口收益被 config-gate 破裂 + 记录目的隐式判别脆弱 + 引擎感知合同终局动作的耦合成本抵消。
      - Skill: `nop-backend-dev`
- [x] `Decision` **角色→用户解析载体裁决（D2）**：ApprovalMatrix.approverRole（角色编码）→ ApprovalRecord.approverId（userId）——选项 A（裁决候选）：平台 nop-auth 角色查询（对齐 A1.51 `resolveRole:117-156` 双过滤范式，经 NopAuthRole/NopAuthUser 查询 roleName 匹配用户，approverId 缺省留空由操作员手工指定）；选项 B：master-data ErpMdRole 组织架构解析——跨域契约复杂度更高。**决策记录理由 + 备选**。
      **决策记录（已执行）**：**选项 A（平台 nop-auth 角色查询）**——approverRole 视为 nop-auth **roleName**：`INopAuthRoleBiz.findList(eq("roleName", role))` → roleIds → `INopAuthUserRoleBiz.findList(in("roleId", roleIds))` → userIds，**取确定序首个**（userId 字符串排序最小者，确定性可测）写入 approverId；无命中（角色不存在或无成员）→ approverId 留空（操作员手工指定语义——approve 守卫对 approverId=null 放行任意操作员）。实仓核验：`NopAuthRole.roleName`/`NopAuthUserRole.roleId` 生成 xmeta 均 `queryable="true"`（_dump merged xmeta 实证），IBiz.findList 过滤可行——**零 daoFor 站点**（镜像 A1.51 `resolveRole:117-156` 双过滤范式，但经 IBiz 注入替代 raw daoFor，合规零漂移）。选项 B（master-data ErpMdRole）否决：跨域契约复杂度更高 + 与平台 IUserContext 角色集（nop-auth）双轨 + 本行纯预授权边界内无既有 ErpMdRole 成员解析面。
      - Skill: `nop-backend-dev`
- [x] `Decision` **驳回超限计数载体裁决（D3）**：ApprovalRecord 无 rejectCount 列——选项 A（裁决候选）：派生计数（同 contractId+approvalOrder 的 REJECTED 记录数，config `erp-ct.approval-max-retries` 默认 3 比较，零 ORM）；选项 B：ORM 加列（ask-first 移出本行）。**决策记录理由 + 备选**。
      **决策记录（已执行）**：**选项 A（派生计数）**——`rejectedCount(contractId, approvalOrder)` = 该 (contractId, approvalOrder) 组的 REJECTED 记录数（`IErpCtApprovalRecordBiz.findList` eq+eq 过滤，全列 queryable 实证可行），与 `erp-ct.approval-max-retries`（默认 3）比较：reject 前计数已 ≥ 上限 → `ERR_CT_APPROVAL_LOCKED` 拒绝（「允许驳回最多 N 次，超限后锁定」= 第 N 次驳回后计数=N 即锁定，后续 approve/reject/resubmit 均拒）；锁定同时 best-effort 派发 `ct.approval-locked` 强制升级通知（经办人）。派生计数与 D7 追加行生命周期自洽（每轮重提追加行 → 计数递增 → 锁定可达）。选项 B（ORM 加列）否决：ask-first 越界 + 派生计数语义完备。
      - Skill: `nop-backend-dev`
- [x] `Decision` **驳回重提循环裁决（D7）**：L1 UC-CT-07 基本流程 step 6（`use-cases.md:141`）「驳回时经办人修改后可重新提交（仅重新激活驳回节点及其后续节点）」+ approval-workflow.md 规则 4（`:152`）——**重提循环是 D3 超限锁定的可达性前提**（无重提则 REJECTED 记录不可再操作，派生计数恒为 1，锁定永不触发）。选项 A（裁决候选）：纳入范围——`resubmit` @BizMutation（合同 NEGOTIATION → 重新激活驳回节点及其后续节点为 PENDING，已 APPROVED 节点保持不动，派生 REJECTED 计数递增使锁定可达；**记录生命周期须显式钉住**：每轮重提**追加新 ApprovalRecord 行**（保留 REJECTED 历史行 + 新建 PENDING 行），禁止 REJECTED→PENDING 原地翻转——原地翻转将致派生计数恒 1，锁定不可达）；选项 B：显式移出范围并登记 Non-Goals + Deferred But Adjudicated（依据：roadmap/arm-index 修复建议 ①-④ 未列重提），但须同步调整 D3 锁定语义（此时锁定退化为单次驳回后锁定或取消）。**L1 字面含 step 6，Q4=(a) 禁止静默裁剪，建议选项 A**。**决策记录理由 + 备选**。
      **决策记录（已执行）**：**选项 A（纳入范围）**——`ErpCtApprovalRecordBizModel#resubmit(contractId)` @BizMutation：守卫（合同 NEGOTIATION + 最新轮次存在 REJECTED 记录[否则 `ERR_CT_APPROVAL_NO_REJECTED`] + 该节点派生计数 < maxRetries[否则 `ERR_CT_APPROVAL_LOCKED`]）→ 以最新 REJECTED 记录的 approvalOrder 为界，对 approvalOrder ≥ 该值的节点**追加新 ApprovalRecord 行**（首节点 PENDING 其余 WAITING，approverId 重新经 D2 解析，approvalMatrixId 沿用节点矩阵 id），已 APPROVED 节点（< 界）保持不动——**记录生命周期钉住：只追加不原地翻转**（REJECTED 历史行保留，派生计数递增使 D3 锁定可达）。选项 B（移出范围）否决：L1 step 6 字面含重提（Q4=(a) 禁止静默裁剪）+ 无重提则 D3 锁定派生计数恒 1 永不触发（锁定修复义务失效）。
      - Skill: `nop-backend-dev`
- [x] `Decision` **InvoicePlan 截停载体裁决（D4）**：无状态列——选项 A（裁决候选）：逻辑删除（`useLogicalDelete=true` 既有语义，delVersion=1 截停未执行行，隐式失效 + 显式标记双保险；**预授权依据**：arm-index P1-RC-076 修复建议②「标记作废或加状态列」属预授权范围 + 实体已带 `useLogicalDelete=true`（orm.xml:276）——逻辑删除是 BizModel 写操作，非「数据删除/迁移」ask-first 类目）；选项 B：remark 注记「[terminate] {date} 截停」——保留行可审计但不阻断读路径。**决策记录理由 + 备选**。
      **决策记录（已执行）**：**选项 A（逻辑删除）**——approveTermination 副作用中，对合同行集合（`IErpCtContractLineBiz.findList`）的 `isInvoiced=false` InvoicePlan（`IErpCtInvoicePlanBiz.findList` eq contractLineId in + eq isInvoiced false）逐条 `IErpCtInvoicePlanBiz.delete(id)`（useLogicalDelete 既有语义 delVersion=1）——「标记作废」显式落库 + TERMINATED 隐式失效（triggerInvoice ACTIVE 守卫）双保险；已开票（isInvoiced=true）行保留（历史发票证据）。选项 B（remark 注记）否决：不阻断读路径，非 L1「标记为作废」后置条件的显式达成。预授权判据：P1-RC-076 修复建议②「标记作废或加状态列」属预授权范围 + 实体已带 useLogicalDelete=true——逻辑删除是 BizModel 写操作，非「数据删除/迁移」ask-first 类目。
      - Skill: `nop-backend-dev`
- [x] `Decision` **善后 TODO 载体裁决（D5）**：全域无 TODO 实体——选项 A（裁决候选）：`IErpSysNotificationBiz.notify` 派发善后结算任务通知（经办人，事件 `ct.terminate-winddown`）+ owner doc 注记 TODO 语义由通知承载；选项 B：新建实体（ask-first 移出）。**决策记录理由 + 备选**。
      **决策记录（已执行）**：**选项 A（通知承载 TODO 语义）**——approveTermination 副作用中 `IErpSysNotificationBiz.notify("ct.terminate-winddown", ctx)`，接收人 = 合同经办人（`contract.createdBy`——ORM createdBy 域即 userId，context 键 `submitterUserId` 供 USER_LIST 模板 `${submitterUserId}` 插值，对齐 R1.33 `ct.consumption-over-120-percent` 通知范式），context 键集 {contractId, contractCode, submitterUserId, terminationReason}；无 ACTIVE 模板静默跳过（R1.4 范式，不阻断终止）。owner doc 注记「善后 TODO 语义由终止善后通知承载」——state-machine.md §8 TODO 表 TERMINATED 行语义达成。选项 B（新建 TODO 实体）否决：ask-first 越界 + 全域无 TODO 实体基建。
      - Skill: `nop-backend-dev`
- [x] `Decision` **72h 升级 job 形态裁决（D6）**：对齐 R1.4（简单 job bean + job.yaml）还是 R1.23/R1.27（batch-task + helper REQUIRES_NEW）——审批超时升级为低基数扫描（PENDING 节点超时），建议简单 job bean + 逐条失败隔离；cron config 键 + notify 事件（`ct.approval-timeout-escalation`，无 ACTIVE 模板静默跳过 R1.4 范式）。**决策记录理由 + 备选**。
      **决策记录（已执行）**：**简单 job bean + job.yaml（R1.4 范式）**——`ErpCtApprovalTimeoutEscalationJob`（无参 `execute()` 反射入口，镜像 `ErpHrLeaveApproverTimeoutJob`）：双层门控（job.yaml `nop.job.erp-ct-approval-timeout.enabled|cron-expr` 调度级 + bean 内 `erp-ct.approval-timeout-cron` 空值跳过「不调度」语义）+ 低基数扫描（PENDING 链记录 + `updateTime < now - erp-ct.approval-timeout-hours`[默认 72]，dateTimeBetween(epoch, cutoff) 表达，对齐 R1.4 :126 先例）逐条失败隔离（try/catch WARN per record）+ 升级通知 `ct.approval-timeout-escalation`（接收人 = 上一节点审批人[approvalOrder-1 最新记录 approverId]，无则合同经办人 createdBy；USER_LIST 模板 `${escalationUserId}` 插值）。备选 batch-task（R1.23/R1.27）否决：低基数查询 + 单点更新无批量拆分收益 + REQUIRES_NEW helper 引入 R10 基线漂移（本行零 R10 变更）。**cron 键注册**：`erp-ct.approval-timeout-cron`（bean 门控键，默认空）+ job.yaml `@cfg:nop.job.erp-ct-approval-timeout.cron-expr|0 0 1 * * ?`（调度键，镜像 R1.4 双键范式）。
      - Skill: `nop-backend-dev`
- [x] `Proof` **既有测试误伤面核查**：grep erp-ct 测试集 `ErpCtContract__terminate`/`__submit`/`__activate` 调用面——terminate 改造后既有 `TestErpCtContractTerminate`（ACTIVE/NEGOTIATION→TERMINATED + 非法源态拒绝）是否零误伤（门控 config 默认 false 时行为不变？）；submit 生成 Records 后既有 `TestErpCtContractCreateValidate` 零冲突核查。
      **核查结论（已执行）**：**非零误伤，识别调整点 3 处（全部为既有测试对 terminate 旧语义的强断言，须随两段化改造更新——非回归，属计划内行为变更）**：(1) `TestErpCtContractTerminate`（6 @Test）——`testTerminateFromActiveSucceeds:47-57`/`testTerminateFromNegotiationSucceeds:59-70` 断言 terminate 后 status==TERMINATED（旧单段语义）→ 改为 terminate→approveTermination 两段后断言 TERMINATED；`testTerminateRejectedFor*` 4 组非法源态拒绝断言**零改动**（守卫仍在 terminate mutation 上）；setupContractInStatus 的 TERMINATED 分支 :123-127 经 terminate 造终态 → 改两段流。(2) `TestErpCtContractPosting#testTriggerInvoiceRejectedForTerminatedContract:107-120` —— seed ACTIVE→terminate 后直接 saveInvoicePlan+triggerInvoice 断言拒绝——terminate 不再翻转 TERMINATED，测试须补 approveTermination（或改为直接断言 terminate 后合同非 ACTIVE 亦满足拒绝路径——取补 approveTermination 保 TERMINATED 语义）。(3) E2E `ct-contract-lifecycle.action.spec.ts:74-77/131`（happy path terminate→TERMINATED + 终态守卫场景）——浏览器层 E2E 非 mvn 面，补两段流（terminate→findPage ApprovalRecord→approveTermination）。**submit/activate 误伤面**：submit 后置记录生成受 `erp-ct.approval-enabled`（默认 false）门控——false 时零生成零行为变化 → `TestErpCtContractCreateValidate` 16 组（submit/全链/amend）**零冲突**；activate 前置链完整性校验同为 config-gated（false 时跳过）→ 既有 activate 全链测试零冲突；terminate 法务门控**不受 config 门控**（D1 强制义务）→ 所有 terminate 调用面（含 E2E）须走两段。调整点清单 = 3 处（上述），全部为测试侧更新。
      - Skill: `nop-testing`

Exit Criteria:

- [x] D1-D7 决策记录落盘（含理由 + 备选）+ 误伤面核查结论（零误伤或已识别调整点）
- [x] terminate 门控形态契约确认（引擎复用 vs 独立 mutation）+ 引擎触发入口契约（submit 联动）+ 驳回重提循环契约（D7）确认

### Phase 2 - ApprovalWorkflowEngine 落地（P1-RC-077 核心）

Status: completed
Targets: `ErpCtApprovalWorkflowEngine.java`（或 `ErpCtApprovalRecordBizModel` 扩展）；`IErpCtApprovalMatrixBiz.java`/`IErpCtApprovalRecordBiz.java`（契约）；`ErpCtErrors.java`；`ErpCtConstants.java`；`ErpCtConfigs.java`
Skill: `nop-backend-dev`

- Item Types: `Add | Fix`
- Prereqs: Phase 1 完成

- [x] `Add` **引擎核心**：`matchByAmount(contract)`——读 ApprovalMatrix（isActive=true + contractType 匹配/null 通配 + orgId 匹配 + minAmount≤totalAmount≤maxAmount[null 无界]）→ approvalOrder 升序节点列表；`generateRecords(contract, matrixNodes)`——每节点一条 ApprovalRecord（首 PENDING 其余 WAITING，approverId 按 D2 解析）；config-gated（`erp-ct.approval-enabled` 默认 false——false 时零生成零阻塞，D1 联动 terminate 门控形态时 false 语义须明确）。Decision：引擎触发入口 = `submit` 后置接线（UC-CT-07「经办人提交合同」）vs 独立 mutation。
      **落地记录（已执行）**：新建 `app.erp.ct.service.approval.ErpCtApprovalWorkflowEngine`（无状态编排助手 Bean，beans.xml FQCN 注册）——`matchByAmount`（isActive=true + contractType/orgId null 通配 + 金额窗口 + approvalOrder 升序）+ `generateRecords`（首 PENDING 其余 WAITING + D2 approverId 解析）+ 链状态推导（`findRecords`/`latestRecord`/`rejectedCount`/`latestRejected`/`isChainComplete`/`hasPendingTermination`）+ `resolveApproverId`（D2：roleName→NopAuthRole→NopAuthUserRole→userId 确定序最小，IBiz 注入零 daoFor）。**触发入口裁决 = submit 后置接线**（L1 触发条件「经办人提交合同」= submit 语义；独立 mutation 否决）——`ErpCtContractBizModel#submit` 增 `generateApprovalRecordsIfEnabled`（config-gated：approval-enabled=false 或矩阵零匹配 → 零生成零阻塞，既有行为零变化）+ 首节点 `ct.approval-task` 通知。
      - Skill: `nop-backend-dev`
- [x] `Add` **approve/reject @BizMutation**：`ErpCtApprovalRecordBizModel#approve(recordId, comment)`——PENDING→APPROVED + approvedAt + 激活下一 WAITING 节点（→PENDING）+ 全通过后合同「可 ACTIVE」（Decision：activate 前置校验接线）；`#reject(recordId, comment)`——PENDING→REJECTED + rejectedAt + 合同保持 NEGOTIATION + notify 经办人（`ct.approval-rejected`）+ D3 超限锁定（REJECTED 派生计数 ≥ max-retries → 后续 approve 拒绝 + 强制升级标记）。守卫：仅 PENDING 可操作 + approverId 匹配当前操作人（Decision 强度）。
      **落地记录（已执行）**：`IErpCtApprovalRecordBiz` 契约增 approve/reject/resubmit（@BizMutation + @Optional comment）+ `ErpCtApprovalRecordBizModel` 实现——approve（requireChainRecord[approvalMatrixId!=null 判别，terminate 记录双轨拒绝] → guardPending → guardApprover[approverId 非空须 == ctx.getUserId，空=手工指定放行] → guardNotLocked[D3 派生计数] → APPROVED+approvedAt → `activateNext`[最小 order>current 的 WAITING 最新记录 → PENDING + ct.approval-task 通知]）；reject（同守卫 → REJECTED+rejectedAt → `ct.approval-rejected` 通知经办人 createdBy → 计数达上限时 `ct.approval-locked` 强制升级通知）。**activate 联动裁决 = activate 前置校验接线**（自动联动否决：绕过签署确认违反 UC-CT-01）——`ErpCtContractActivateProcessor.activate` 增 config-gated 链完整性校验（approval-enabled && 链记录存在 && 非全 APPROVED → `ERR_CT_APPROVAL_NOT_COMPLETE`；L1「可进入 ACTIVE」= 前置满足，签署确认仍显式 activate）。
      - Skill: `nop-backend-dev`
- [x] `Add` **resubmit @BizMutation（按 D7 裁决；选项 A 时）**：合同 NEGOTIATION → 重新激活驳回节点及其后续节点（→PENDING），已 APPROVED 节点保持不动；**每轮追加新 ApprovalRecord 行**（REJECTED 历史行保留 + 新建 PENDING 行），派生 REJECTED 计数递增（D3 锁定可达性闭环）。
      **落地记录（已执行）**：`resubmit(contractId)`——守卫（合同 NEGOTIATION[非 NEGOTIATION 抛非法迁移] + `latestRejected` 存在[无 → `ERR_CT_APPROVAL_NO_REJECTED`] + guardNotLocked[超限 → `ERR_CT_APPROVAL_LOCKED`]）→ `matchByAmount` 复取节点 → 对 approvalOrder ≥ 最新驳回节点 order 的节点**追加新行**（首 PENDING 其余 WAITING，approverId 重新 D2 解析，approvalMatrixId 沿用节点 id；已 APPROVED 节点不追加）→ 首节点 `ct.approval-task` 通知 → 返回追加行数。追加行生命周期断言（REJECTED 历史保留 + 新 PENDING 行 + 计数递增）见 Phase 5 ③ 闭环测试。
      - Skill: `nop-backend-dev`
- [x] `Add` **错误码 define + 参数表注册**（按需：`ERR_CT_APPROVAL_*` 族——记录不存在/状态非法/超限锁定/审批人不匹配）。
      **落地记录（已执行）**：`ErpCtErrors` 新增 7 码——`ERR_CT_APPROVAL_RECORD_NOT_FOUND`（erp.err.ct.approval-record-not-found）/ `ERR_CT_APPROVAL_ILLEGAL_STATUS`（approval-illegal-status）/ `ERR_CT_APPROVAL_APPROVER_MISMATCH`（approval-approver-mismatch）/ `ERR_CT_APPROVAL_LOCKED`（approval-locked）/ `ERR_CT_APPROVAL_NOT_COMPLETE`（approval-not-complete）/ `ERR_CT_APPROVAL_NO_REJECTED`（approval-no-rejected）/ `ERR_CT_TERMINATE_ALREADY_PENDING`（terminate-already-pending）+ ARG_APPROVAL_RECORD_ID/ARG_APPROVAL_ORDER/ARG_MAX_RETRIES/ARG_APPROVER_ID/ARG_USER_ID 参数表注册，描述中文。
      - Skill: `nop-backend-dev`
- [x] `Add` **config 键登记**：approval-workflow.md 五键登记 `ErpCtConfigs`（D1/D6 决策的键一并登记）。
      **落地记录（已执行）**：`ErpCtConfigs` 登记 7 键 + 默认值——`erp-ct.approval-enabled`(false，引擎生成门控)/ `erp-ct.approval-max-retries`(3，D3 锁定阈值，approve/reject/resubmit 三守卫消费)/ `erp-ct.approval-timeout-hours`(72，D6 job 阈值)/ `erp-ct.approval-urgent-threshold`(500000，design doc 顶层边界默认语义——矩阵边界运行时权威，本键登记供种子/部署对齐)/ `erp-ct.amendment-reapproval-threshold`(0.2，amend→submit 全链重审结构性达成，小额免审快捷通道 Deferred 注记)/ `erp-ct.approval-timeout-cron`(空=不调度，D6 bean 门控键)/ `erp-ct.terminate-approver-role`(「合同审批人」，D1 法务角色)。`ErpCtConstants` 增审批状态 5 常量（WAITING/PENDING/APPROVED/REJECTED/SKIPPED，dict erp-ct/approval-status）+ 6 通知事件（ct.approval-task / ct.approval-rejected / ct.approval-locked / ct.approval-timeout-escalation / ct.terminate-winddown / ct.terminate-rejected）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 引擎接线且守卫落地（GraphQL 实调：submit 后生成 Records 首 PENDING 其余 WAITING / 逐节点 approve 推进 / 全通过 activate 联动 / reject 保持 NEGOTIATION + 通知）
- [x] config-gated 双路径运行时验证（enabled=false 零生成零阻塞 / enabled=true 全链）
- [x] 超限锁定 + 审批人守卫运行时拒绝（GraphQL 实调错误码断言）

### Phase 3 - terminate 两段化与副作用落地（P1-RC-076 核心）

Status: completed
Targets: `ErpCtContractBizModel.java`；`ErpCtContractVersionBizModel.java`（或版本 IBiz）；`IErpCtContractBiz.java`；`ErpCtErrors.java`
Skill: `nop-backend-dev`

- Item Types: `Add | Fix`
- Prereqs: Phase 1-2 完成（D1 引擎复用形态时依赖 Phase 2）

- [x] `Fix` **terminate 两段化改造**：按 D1 形态——`terminate` 增法务审批前置（未过审批拒绝，错误码）+ `approveTermination` @BizMutation（法务通过后执行：setStatus(TERMINATED) + 版本归档 + 截停 + TODO 通知）；法务驳回 → 合同保持原状态路径。守卫链保留既有 Bean `assertCanTerminate` 多源语义。
      **落地记录（已执行）**：D1 选项 B（独立 mutation）——`ErpCtContractBizModel#terminate(contractId, reason, attachmentId)` 改造为发起语义（Bean `assertCanTerminate` 多源守卫保留 + `hasPendingTermination` 重复发起守卫 `ERR_CT_TERMINATE_ALREADY_PENDING` + 生成法务记录[approvalMatrixId=null + approvalOrder=1 + approverId 按 `erp-ct.terminate-approver-role` D2 解析 + PENDING + remark=reason[附件引用] + 首节点 `ct.approval-task` 通知] + **合同保持原状态**——不再单向翻转）；`approveTermination(recordId, comment)`（requireTerminationRecord[approvalMatrixId=null 判别] + guardTerminationRecord[PENDING + approver 匹配] → 执行终止操作：setStatus(TERMINATED) + 版本归档 + InvoicePlan 截停 + 善后通知 → 记录 APPROVED+approvedAt）；`rejectTermination(recordId, comment)`（同守卫 → 记录 REJECTED+rejectedAt + `ct.terminate-rejected` 通知经办人 + **合同保持原状态**）。`IErpCtContractBiz` 契约同步（terminate 增 @Optional reason/attachmentId——既有调用面零破坏；approveTermination/rejectTermination 新契约）。
      - Skill: `nop-backend-dev`
- [x] `Add` **版本归档**：当前版本（isCurrent=true）→ isCurrent=false（D5 落点或独立实现——对齐 R1.32 `restoreCurrentVersion` 原子翻转范式）。
      **落地记录（已执行）**：`archiveCurrentVersion(contractId)`——`findCurrentVersion`（isCurrent=true）→ `isCurrent=false` + updateEntity（经 `IErpCtContractVersionBiz` 权限管道，对齐 R1.32 restoreCurrentVersion 翻转范式）。
      - Skill: `nop-backend-dev`
- [x] `Add` **InvoicePlan 显式截停**：按 D4 载体——未执行（isInvoiced=false）InvoicePlan 批量截停标记（逻辑删除 or remark）。
      **落地记录（已执行）**：`haltUnexecutedInvoicePlans(contractId)`——D4 选项 A 逻辑删除：合同行集合 → `in("contractLineId", lineIds)` + `eq("isInvoiced", false)` 查询 → 逐条 `IErpCtInvoicePlanBiz.delete(id)`（useLogicalDelete 既有语义，delVersion 置删除标记；已开票行保留）。测试断言读路径零命中 + delVersion 非 0。
      - Skill: `nop-backend-dev`
- [x] `Add` **善后 TODO 派发**：按 D5 载体——`IErpSysNotificationBiz.notify(ct.terminate-winddown, ctx)` 经办人善后结算通知（无 ACTIVE 模板静默跳过）。
      **落地记录（已执行）**：`notifyWinddown`——事件 `ct.terminate-winddown`，context 键集 {contractId, contractCode, submitterUserId[=createdBy 经办人], terminationReason[记录 remark]}，USER_LIST 模板 `${submitterUserId}` 插值（无 ACTIVE 模板 notify 内部静默跳过 R1.4 范式）；测试断言通知落库 + recipient 断言。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] terminate 两段化运行时成立（GraphQL 实调：未过法务拒绝 / 通过后副作用全落地[TERMINATED + isCurrent=false + 截停 + TODO 通知] / 驳回保持原状态）
- [x] 版本归档 + InvoicePlan 截停 + TODO 通知落库断言（非仅静态接线）

### Phase 4 - 72h 超时升级 job（P1-RC-077 ④）

Status: completed
Targets: `ErpCtApprovalTimeoutEscalationJob.java`（或等价）；`app-erp-all/src/main/resources/_vfs/nop/job/conf/erp-ct-approval-timeout.job.yaml`；`ErpCtConstants.java`；`ErpCtConfigs.java`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 2 完成（依赖引擎 PENDING 节点语义）

- [x] `Add` **job bean + job.yaml 接线**（D6 形态）：cron 门控扫描 PENDING 且 approvedAt null 且 updateTime 超 `erp-ct.approval-timeout-hours` 的 ApprovalRecord → 升级通知上一级（或审批人上级，载体 Decision）+ 逐条失败隔离；job.yaml 注册于 app-erp-all。
      **落地记录（已执行）**：`app.erp.ct.service.job.ErpCtApprovalTimeoutEscalationJob`（R1.4 简单 job bean：无参 `execute()` + 双层门控[cron 空跳过「不调度」] + `dateTimeBetween(epoch, cutoff)` 超时过滤 + 逐条 try/catch WARN 失败隔离 + SCAN_LIMIT 200）+ `app-erp-all/_vfs/nop/job/conf/erp-ct-approval-timeout.job.yaml`（`enabled: @cfg:nop.job.erp-ct-approval-timeout.enabled|false` + `cronExpr: @cfg:nop.job.erp-ct-approval-timeout.cron-expr|0 0 1 * * ?` + invoker bean `erpCtApprovalTimeoutEscalationJob.execute`，镜像 R1.4 双键范式）+ `app-service.beans.xml` 注册（bean id = `erpCtApprovalTimeoutEscalationJob`）+ `TestErpAllJobYamlLoading` 21→22 计数同步。**升级接收人载体（D6 契约）**：链记录（approvalMatrixId != null）→ 上一节点（approvalOrder-1 最新记录 approverId）审批人，无则合同经办人 createdBy；终止法务记录（approvalMatrixId=null）→ 合同经办人；无接收人 LOG.warn 跳过（隔离）。
      - Skill: `nop-backend-dev`
- [x] `Add` notify 事件常量 + context 键集（`ct.approval-timeout-escalation`，D6 契约）。
      **落地记录（已执行）**：`ErpCtConstants.NOTIFY_EVENT_APPROVAL_TIMEOUT_ESCALATION = "ct.approval-timeout-escalation"` + context 键集 {contractId, contractCode, approvalOrder, escalationUserId}（USER_LIST 模板 `${escalationUserId}` 插值，无 ACTIVE 模板静默跳过）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] job 级测试运行时成立（cron 门控空值跳过 / 超时节点升级通知落库 + 未超时零动作 / 单条失败隔离不阻断）

### Phase 5 - 测试矩阵

Status: completed
Targets: `module-contract/erp-ct-service/src/test/java/app/erp/ct/service/`（新增 `TestErpCtApprovalWorkflow` / `TestErpCtTerminateGate` 或扩展既有）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 2-4 完成

- [x] `Add` 测试组（按 Goals）：① terminate 两段化（未过法务拒绝 + 通过后副作用全落地 + 驳回保持原状态）；② 引擎全链（金额匹配节点/逐节点推进/全通过 activate 联动/reject 保持 NEGOTIATION + 通知/审批人守卫）；③ 超限锁定（max-retries 边界 + **驳回→重提→再驳回×3→锁定拒绝闭环**——D7 追加行生命周期断言[REJECTED 历史保留 + 新 PENDING 行 + 派生计数递增]）；④ 72h 升级 job（超时通知 + 未超时零动作 + 失败隔离）；⑤ config-gated 双路径（enabled=false 零生成）；⑥ GraphQL RPC 冒烟 + 快照录制（拒绝路径直断言范式，对齐 R1.32/R1.33）。
      **落地记录（已执行）**：新增 3 测试类 22 组全绿——`TestErpCtApprovalWorkflow` 11 组（⑤ config-gated 双路径 3[disabled 零生成/金额匹配生成首 PENDING 余 WAITING + D2 解析/matrix 窗口外零节点] + ② 引擎全链 5[逐节点推进/全通过 activate 联动/链未完成 activate 拒绝 ERR_CT_APPROVAL_NOT_COMPLETE/reject 保持 NEGOTIATION + 经办人通知落库/审批人 mismatch 守卫/WAITING 节点不可直批] + ③ D7 闭环 2[驳回→重提×2→再驳回×3→锁定拒绝 + 追加行生命周期断言[REJECTED 历史保留 + 新 PENDING 行 + 派生计数 1→2→3 递增] + 无驳回 resubmit 拒绝]）；`TestErpCtTerminateGate` 6 组（① 发起[PENDING 记录 + approvalMatrixId=null 判别 + D2 法务角色解析 + remark reason + 合同保持原状态]/重复发起拒绝 ERR_CT_TERMINATE_ALREADY_PENDING/未过法务 InvoicePlan 仍可触发[副作用未执行]/通过后副作用全落地[TERMINATED + 版本 isCurrent=false 归档 + InvoicePlan 逻辑删除 delVersion 非 0 + 读路径零命中 + 善后通知落库 recipient 断言]/approver mismatch 守卫/驳回保持原状态 + 经办人通知 + 驳回后可重新发起）；`TestErpCtApprovalTimeoutJob` 5 组（④ 超时升级通知上一节点审批人 recipient 断言/未超时零动作/cron 空跳过/合同缺失单条跳过隔离不阻断/job 门控 config 绑定）。**既有测试调整**（误伤面核查识别调整点）：`TestErpCtContractTerminate` 两段化（terminate→approveTermination，TERMINATED 分支 + 非法源态拒绝保持）；`TestErpCtContractPosting#testTriggerInvoiceRejectedForTerminatedContract` 补 approveTermination；`TestErpCtContractRebate#testContractStateMachine` 两段化 + 快照重录（erp_ct_contract_version IS_CURRENT false + VERSION 2 + erp_ct_approval_record 表录入）；E2E `ct-contract-lifecycle.action.spec.ts` 两段化（terminateAndApprove helper——findFirst PENDING 记录 + loginAsRole「合同审批人」[nop_auth_user_role.csv userId 19 绑定，D2 解析落 approverId=19]）；`TestErpAllJobYamlLoading` 21→22。快照：`_cases/` 自动录制（TestErpCtApprovalWorkflow/TestErpCtTerminateGate/TestErpCtApprovalTimeoutJob 新目录 + Rebase 重录），拒绝路径直断言范式（对齐 R1.32/R1.33）。
      - Skill: `nop-testing`
- [x] `Proof` 既有 erp-ct-service 测试零回归：`mvn test -pl module-contract/erp-ct-service`（110 基线 + 新增全绿）。
      **落地记录（已执行）**：`mvn test -pl module-contract/erp-ct-service` **132/132 全绿**（110 基线 + 22 新增，0 failures / 0 errors）——Posting 4/CrudSmoke 5/Terminate 6/ESignature 19/Rebate 8[快照重录后 8 绿]/RebateSettlementEnd 2/ResponseMasking 4/CreateValidate 16/BillingFamily 12/statemachine 5 类（9+7+5+7+3）+ ApprovalWorkflow 11/TerminateGate 6/ApprovalTimeoutJob 5 零回归。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 新增测试组全绿 + erp-ct-service 全模块零回归（BUILD SUCCESS）
- [x] 门控/引擎/截停/升级有运行时断言证据（GraphQL RPC 实调，非仅静态接线）

### Phase 6 - 文档回填 + arm-index/roadmap 状态

Status: completed
Targets: `docs/design/contract/approval-workflow.md`；`docs/design/contract/state-machine.md`；`docs/audits/arm-index.md`；`docs/backlog/requirement-compliance-roadmap.md`；`docs/logs/2026/08-15.md`
Skill: none

- Item Types: `Add | Fix`
- Prereqs: Phase 1-5 完成

- [x] `Add` owner doc 注记：审批引擎实现注记（触发入口/节点推进/超限锁定/72h 升级接线 + config 键消费）+ terminate 两段化注记（法务门控形态/截停载体/TODO 载体 + D1-D6 裁决摘要）；不修改需求契约段（use-cases L1 不动）。
      **落地记录（已执行）**：`approval-workflow.md` 新增「实现注记（RC-R1.34）」段（引擎/触发入口 submit 后置/逐节点审批/activate 前置校验联动/D2 解析/D3 锁定/D7 追加行/72h job/通知事件 4 个/config 键 7 个消费语义/测试/Deferred 已裁定三项）；`state-machine.md` §3 增「terminate 两段化实现注记（RC-R1.34）」段（D1 选项 B 门控形态 + 副作用载体 D4/D5 + 守卫语义 + 测试证据；§2/§6 契约段未改动，use-cases L1 零改动）。
      - Skill: none
- [x] `Add` arm-index P1-RC-076/077 → `done (RC-R1.34)` + 修复落地摘要；roadmap RC-R1.34 → done ✅（含落地摘要）；`docs/logs/2026/08-15.md` 日志条目写入。
      **落地记录（已执行）**：arm-index P1-RC-076（:262）→ `done (RC-R1.34)`（terminate 两段化落地摘要：发起/approveTermination 副作用全落地/rejectTermination/门控不受 config 门控/TestErpCtTerminateGate 6 组 + 既有测试两段化调整）；P1-RC-077（:263）→ `done (RC-R1.34)`（引擎落地摘要：matchByAmount/generateRecords/approve/reject/resubmit/activate 联动/submit 接线/72h job/config 键/TestErpCtApprovalWorkflow 11 + TimeoutJob 5 + JobYamlLoading 22）；roadmap RC-R1.34（:426）→ `done ✅`（含落地摘要）；`docs/logs/2026/08-15.md` 首条目（RC-R1.34 全量落地 + 132 tests 验证基线）。
      - Skill: none

Exit Criteria:

- [x] arm-index/roadmap 状态回填 + owner doc 注记落盘 + 日志条目写入

## Draft Review Record

- Independent draft review iteration 1: needs revision（独立子代理 ses_ffcb88d91ffeEOR2lIYpe2egzp）— 0 BLOCKER / 1 MAJOR / 4 MINOR。MAJOR：L1 UC-CT-07 step 6 驳回重提/重新激活缺失，且无重提循环时 D3 超限锁定派生计数恒 1 永不触发（roadmap 行内范围「驳回超限锁定」失效）。修订：新增 D7 决策项（重提循环纳入/移出裁决 + 锁定可达性论证）+ Phase 2 resubmit @BizMutation Add 项 + Non-Goals 明确排除 + Goals 更新；MINOR 全部折叠（arm-index 陈旧行号注记 / D4 逻辑删除预授权依据显式化 / Deferred 项补 Why Not Blocking Closure / activate 描述精度）。
- Independent draft review iteration 2: accept（独立子代理 ses_ffcad5c89ffec5L4QWoiRNlWrN）— 0 BLOCKER / 0 MAJOR / 3 MINOR（全部折叠）：D7 追加 vs 原地翻转记录生命周期未钉住 → 决策项与实现项显式钉住「每轮追加新行」；重提循环测试用例缺失 → Phase 5 ③ 补闭环用例；Draft Review Record 迭代记录补全（本条）。**计划可标记 active。**

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。**完整仓库验证在此处**：结束时运行一次全量验证。

- [x] 范围内行为完成——P1-RC-076 terminate 法务门控/副作用 + P1-RC-077 审批引擎/超限锁定/72h 升级运行时成立（独立结束审计逐文件核验）
- [x] 相关文档对齐——arm-index/roadmap/owner doc/日志回填（独立结束审计核验）
- [x] 已运行验证（`mvn test -pl module-contract/erp-ct-service` 全绿 + `mvn clean install -DskipTests` 全量 BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline；helper REQUIRES_NEW 若引入须 R10 基线上调 per-site 证据登记）
- [x] 无范围内项目降级为 deferred/follow-up（Deferred But Adjudicated 各项有 successor 分类与触发条件，无已确认缺陷/契约漂移）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

（本行结束审计时按实际裁决登记。draft 期已识别候选，供结束审计前定稿：

- **审批撤回（NEGOTIATION→DRAFT）与转交**（approval-workflow.md §业务规则 5/6，L1 UC-CT-07 未列）：
  - Classification: `out-of-scope improvement`
  - Why Not Blocking Closure: 撤回/转交是 owner-doc 设计扩展项，L1 UC-CT-07 基本流程 6 项 + 异常 2 项未列，主路径（匹配/生成/逐节点审批/驳回/锁定/升级）不受影响；后端 mutation 能力面已提供。
  - Successor Required: `yes`（触发条件 = PM 审批操作完备性需求立项）
- **terminate 申请独立实体载体**（D1 选项 B 备选，触 ORM）：
  - Classification: `out-of-scope improvement`
  - Why Not Blocking Closure: D1 裁决候选 A（引擎法务节点 + 合同 remark 承载申请信息）零 ORM 即可实现 L1 语义；独立申请实体仅增强审计追溯，非 L1 字面义务。
  - Successor Required: `yes`（触发条件 = 终止申请审计追溯需求立项）
- **前端审批操作 AMIS 接线**：
  - Classification: `watch-only residual`
  - Why Not Blocking Closure: 本行落地后端 mutation 能力面（能力面 = L1 后端义务）；前端按钮接线属 UI 增强。
  - Successor Required: `no`

### 实际裁决登记（2026-08-15 结束审计时定稿）

- **审批撤回（NEGOTIATION→DRAFT）与转交**：维持 draft 期登记——`out-of-scope improvement`，Successor `yes`（触发条件 = PM 审批操作完备性需求立项）。未实现不影响 L1 主路径（匹配/生成/逐节点/驳回/重提/锁定/升级）。
- **terminate 申请独立实体载体**：维持 draft 期登记——`out-of-scope improvement`，Successor `yes`（触发条件 = 终止申请审计追溯需求立项）。终止申请信息经 ApprovalRecord.remark 承载（reason + 附件引用），L1 语义达成。
- **前端审批操作 AMIS 接线**：维持 draft 期登记——`watch-only residual`，Successor `no`。
- **amendment-reapproval-threshold 小额免审快捷通道**（Phase 2 落地时新增）：`watch-only residual`——config 键已登记（ErpCtConfigs.CFG_AMENDMENT_REAPPROVAL_THRESHOLD 默认 0.2），消费点 = amend→submit 全链重审已结构性达成（引擎启用时 amend 后 submit 生成全新审批链），小额变更免审直接签署快捷通道未实现（owner doc approval-workflow.md 版本与审批联动段注记）。Successor `no`。
- **无范围内项目降级为 deferred/follow-up**：范围内 8 项（terminate 两段化 4 副作用 + 引擎 4 能力）全部 landed；Deferred 4 项均有裁决分类与 successor 触发条件，无一为已确认缺陷/契约漂移。

## Closure

Status Note: 独立结束审计 8/8 Closure Gates 全部 PASS——范围内行为（terminate 两段化 + 审批引擎 + 超限锁定 + 72h 升级）逐文件核验运行时接线成立；实测验证 `mvn test -pl module-contract/erp-ct-service` 132/132 全绿（0 failures / 0 errors）+ compliance checker 全 19 规则 actual ≤ baseline（零漂移）+ `TestErpAllJobYamlLoading` 22 通过；文档回填/Deferred 裁决/草案审查/文本一致性均核验通过。计划可关闭（`Plan Status: active → completed` 由执行者在审计通过后更新，本审计不改动）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，无执行者上下文；本会话只读审计 + 仅修改本计划文件）
- Evidence: 逐文件核验（ErpCtApprovalWorkflowEngine 8 方法 :68/:109/:138/:170/:185/:200/:219/:237；ErpCtApprovalRecordBizModel approve:56/reject:75/resubmit:99 + PENDING:159 + approverId 守卫:169 + D3 锁定:184 + D7 追加行:220；ErpCtContractBizModel terminate 发起:218/approveTermination:256[TERMINATED+archiveCurrentVersion+haltUnexecutedInvoicePlans+notifyWinddown]/rejectTermination:280/submit 后置 generateApprovalRecordsIfEnabled:149；ErpCtContractActivateProcessor config-gated 链校验:59-63；ErpCtApprovalTimeoutEscalationJob + erp-ct-approval-timeout.job.yaml + TestErpAllJobYamlLoading 21→22；ErpCtErrors 7 码 + ErpCtConstants 5 状态常量/6 通知事件 + ErpCtConfigs 7 键 + I*Biz 契约 + beans.xml 注册；测试 11/6/5 组 + 既有 TestErpCtContractTerminate/Posting/Rebate 两段化调整 + E2E terminateAndApprove helper）。实测数据：`mvn test -pl module-contract/erp-ct-service` → **132 tests, 0 failures, 0 errors, BUILD SUCCESS**（2026-08-15 复跑）；`bash docs/audits/nop-compliance-checker.sh` → R1d=14 R2a=34 R2b=230 R2c=1399 R2d=34 R3=5 R4=0 R5=0 R6=2 R7=0 R8=0 R10=9 R11=0 R12a=69 R12b=66 R12c=40，全 ≤ `compliance-baseline.md` §BASELINE 机器可读块（R10=9≤9 零新增 REQUIRES_NEW，无需 per-site 登记）；`mvn test -pl app-erp-all -Dtest=TestErpAllJobYamlLoading` → 1 passed。文档回填：arm-index :262/:263 `done (RC-R1.34)` + roadmap :426 `done ✅` + approval-workflow.md :180 实现注记 + state-machine.md §3 :74 两段化注记 + docs/logs/2026/08-15.md :1 条目。Deferred 4 项均有分类 + successor 触发条件；Draft Review Record 2 iterations；6 Phase 全 completed + 34/34 项勾选。

Follow-up:

- 非阻塞：E2E `ct-contract-lifecycle.action.spec.ts` 头部注释（:18-19）与 `terminateAndApprove` 注释（:60）对「合同审批人」角色用户绑定描述不一致（一为 approverId null 任意操作员可批，一为 userId 19 绑定 D2 解析落 approverId=19）——仅注释级不一致，helper 代码在两种语义下均正确运行（loginAsRole「合同审批人」满足两种守卫路径），不影响断言有效性；建议后续文案修正，不阻塞关闭。
