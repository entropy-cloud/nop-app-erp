# 2026-08-15-1023-2-rc-mr1-r1-35-ct-expiry-job-reopen-family RC-R1.35 — contract 到期自动化 job 重开族（MR1 第一批纯预授权）

> Plan Status: completed
> Last Reviewed: 2026-08-15
> Mission: requirement-compliance
> Work Item: RC-R1.35（P1-MA2-071 reuse 重开，表 E 重开族 Q9 维持强制：UC-CT-05 ErpCtContractExpiryJob + config-gated auto-create-renewal-draft + 30/15/7 分级通知）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.35 行 + `docs/audits/arm-index.md` P1-MA2-071 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`（Q9 重开族强制实现；纯预授权）
> Related: `docs/design/contract/use-cases.md`（L1 UC-CT-05）；`docs/design/contract/state-machine.md`（§2 ACTIVE→EXPIRED 系统自动 + §4 续期草稿 + §7 到期提醒）；`docs/audits/2026-08-08-0135-rc-ma4-a4-2-155-162-contract-runtime.md`（A4.2.158 运行时证据）；`docs/plans/2026-08-15-0456-2-rc-mr1-r1-32-ct-create-validation-version-family.md`（同域前行）；hr 域 `ErpHrContractExpiryJob` 先例 + `erp-hr-contract-expiry.job.yaml`
> Audit: required

## Current Baseline

- **finding P1-MA2-071（arm-index 行，UC-CT-05 A/B/C/E；表 E 重开族 Q9 维持强制）**：L1（`use-cases.md:91-107`）逐字「nop-job 扫描 erp_ct_contract 条件 `status=ACTIVE AND endDate BETWEEN now() AND now()+30d` / 到期前 30 天发送邮件/站内通知经办人 / 15 天再次通知标记即将到期 / 7 天升级通知经办人上级 / 续期按配置 auto-create-renewal-draft 自动创建续期草稿（parentContractId 关联原合同）走 DRAFT / endDate 到达自动 EXPIRED / 异常：endDate 到达仍有未完成的开票计划 → 先完成开票再 EXPIRED」。L3 实仓：
  - **全域零 `ErpCt*Job.java` + 零 nop-job 注册 + 零 `@CronProvider`**（A4.2.158 三重证实：job 注册权威点 `app-erp-all/_vfs/nop/job/conf/` 当前 21 job.yaml 零 `erp-ct-*` job + scheduler.yaml 零注册 + module-contract 零 Job bean）——无自动到期扫描。
  - `ErpCtContractBizModel#expire:181-193` **手工 @BizMutation**（守卫 ACTIVE，单点 contractId）——唯一过期推进入口。
  - **零 `erp-ct.auto-create-renewal-draft` config key**（`ErpCtConfigs` 当前键集见 R1.34 基线）；`parentContractId` 字段**存在**（orm.xml `IDX_CT_CONTRACT_PARENT_CONTRACT_ID` 索引）但**零业务 Java 使用**（state-machine.md §4 Deferred 注记同证）。
  - **零 30/15/7 分级通知**（无 `IErpSysNotificationBiz` 到期类调用；ct 域 notify 仅 R1.33 `ct.consumption-over-120-percent` 事件）。
  - **未完成开票先完成异常路径零实现**（无 expire 前 InvoicePlan 检查）。
  - **R1.22 resolved 实为 resolved-via-deferral**（owner doc state-machine.md §2:51-53 + §4:79-81 + §7:117 显式 Deferred 注记，非 implementation）——§4 三判据复核：AI 子代理裁决≠人工批准 + Deferred git log 全 AI commits + product-scope 未裁剪 → **deferral 不构成合法范围裁剪，Q4=(a) 强制实现**（A4.2.158 维持注记）。
- **hr 域同型先例（可镜像范式）**：`ErpHrContractExpiryJob`（job bean：`execute()` 无参方法 + cron 空值跳过 + `runExpiryWarnings`/`runExpirations` 两步 + 逐条失败隔离 WARN + `IErpSysNotificationBiz.notify`）+ `erp-hr-contract-expiry.job.yaml`（`app-erp-all/_vfs/nop/job/conf/`：`enabled: '@cfg:nop.job.erp-hr-contract-expiry.enabled|false'` + cronExpr `@cfg:nop.job.erp-hr-contract-expiry.cron-expr|0 0 1 * * ?'` + invoker bean/method）。**注意 hr 先例是双键模式**（job.yaml cron-expr 键 + bean 内部 `erp-hr.contract-expiry-cron` 空值跳过键两个键）；本计划提议单键模式（job.yaml cronExpr 与 bean 空值跳过共用 `erp-ct.contract-expiry-cron`，对齐 R1.4 leave-approver-timeout 先例）——键模式属 Phase 1 Decision 项，不盲从 hr 双键。contract 域须扩展：30/15/7 三级窗口（hr 仅单一阈值）+ auto-create-renewal-draft + 未完成开票检查。
  - **R1.32/R1.33 前行交互**：`submit`/`rejectAmend`/版本语义已落地；`expire()` 手工入口守卫 `assertCanExpire(ACTIVE)` 保持——job 批量路径复用同一入口（或经 Bean 守卫 + DAO 批量，Decision）。`ErpCtConfigs` 当前键集见 R1.33（volume-discount/rebate/invoiceplan/settlement/e-signature/approval[待 R1.34]）。
  - **测试基线**：erp-ct-service ≈ 110 tests（R1.33 结束审计 110/110 全绿基线）；`TestErpAllJobYamlLoading`（app-erp-all，21 job.yaml 加载校验，R1.23 佐证）——**新增 job.yaml 后硬编码计数 21→22 须同步更新**（`assertEquals(21,...)` 两处），属 Phase 2/3 显式检查项。
- **预授权判据**（第一批纯预授权）：调度接线 + config + notify（job bean + job.yaml + `ErpCtConfigs` 新键 + `IErpSysNotificationBiz`），**不触 ORM 结构/会计过账/数据删除**（parentContractId 复用既有列；expire 复用既有状态机 Bean）；roadmap RC-R1.35 行 `todo`，Deps（R1.0 done）已满足。
- **涉及文件**：新 `ErpCtContractExpiryJob.java`（`app.erp.ct.service.job` 包）+ `app-erp-all/_vfs/nop/job/conf/erp-ct-contract-expiry.job.yaml`；`ErpCtConfigs.java`（新 config 键）；`ErpCtConstants.java`（notify 事件 + 窗口常量）；`ErpCtContractBizModel.java` 或 `IErpCtContractBiz.java`（批量 expire/续期草稿能力，Decision：BizModel 内 vs 独立 helper）；测试类（新增 `TestErpCtContractExpiryJob`）；owner doc（state-machine.md Deferred 注记更新为已实现）+ arm-index + roadmap + `docs/logs/2026/08-15.md`（回填）。

## Goals

- **ErpCtContractExpiryJob 运行时成立（P1-MA2-071 ①-③）**：job bean + job.yaml 接线（cron 门控消费 config，空值跳过语义，对齐 hr 范式）——(a) **30/15/7 三级到期提醒**：扫描 `status=ACTIVE AND endDate BETWEEN now() AND now()+30d`，30 天通知经办人 / 15 天再次通知标记「即将到期」/ 7 天升级通知经办人上级（Decision：上级解析载体——经办人字段 vs 部门负责人 vs notify ROLE resolver，对齐 R1.4 superiorId→department.managerId 兜底链）；(b) **到期自动 EXPIRED**：扫描 `ACTIVE AND endDate < now()` 批量 expire（复用既有状态机守卫语义）；(c) **异常路径：未完成开票先完成**——Decision：expire 前检查未触发 InvoicePlan（isInvoiced=false 且 planDate ≤ now），存在时先触发 triggerInvoice 再 expire vs 拒绝 expire 并记录（L1 字面「先完成开票再 EXPIRED」）。
- **config-gated auto-create-renewal-draft（P1-MA2-071 ②）**：`erp-ct.auto-create-renewal-draft` config key（默认 false）+ 到期/续期窗口内自动创建续期草稿（parentContractId 关联原合同，DRAFT 状态，对齐 R1.32 版本语义）——Decision：触发时点（到期前窗口 vs 到期时）。
- **config 键登记 + 文档 Deferred 注记收敛**：`erp-ct.contract-expiry-cron`（job.yaml cronExpr 消费）/`erp-ct.auto-create-renewal-draft`/窗口 config（30/15/7 是否 config 化 Decision）；state-machine.md §2/§4/§7 Deferred 注记更新为已实现注记（不修改 L1 契约段）。
- **测试**：job 级测试——30/15/7 三级通知（窗口边界断言 + 落库 + recipient）/批量 expire（含未完成开票异常路径）/续期草稿创建（parentContractId 关联）/cron 空值跳过/单条失败隔离；`TestErpAllJobYamlLoading` 通过（job.yaml 加载校验）。
- **零回归**：erp-ct-service 既有测试全绿（110 基线）+ 全仓构建 + compliance checker 零漂移。
- **回填**：arm-index P1-MA2-071 → `done (RC-R1.35)`（注记 reuse 家族收敛）+ roadmap 行 → `done` + owner doc 注记 + `docs/logs/` 日志条目。

## Non-Goals

- **不实现 P1-RC-074/075**（invoiceTerm 生成/消耗计费——RC-R1.33 已 done）。
- **不实现 P1-RC-076/077**（terminate 法务门控/审批引擎——独立行 RC-R1.34）。
- **不实现 P1-RC-078/079**（折扣消费方/文档仓库——独立行 RC-R1.79/R1.80 越界项）。
- **不触 ORM 结构**（零列/零索引/零 UK——parentContractId 复用既有列；窗口字段 config 化而非新列）。
- **不改 expire() 既有单点入口语义**（job 批量路径复用守卫，不改变手工路径行为）。
- **不做前端 AMIS 接线**（续期操作按钮/到期提醒视图不在本行）。
- **不改真相源契约段落**（use-cases L1 不动）。
- **不实现邮件/短信真实通道**（经既有 `IErpSysNotificationBiz` 站内通知派发，邮件/短信载体属 notify 子系统 successor）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧的预授权调度接线修复，Q4=(a) 强制实现禁止方案 B；Q9 重开族强制）
- Owner Docs: `docs/design/contract/use-cases.md`（L1 UC-CT-05）+ `docs/design/contract/state-machine.md`（§2/§4/§7 到期自动化契约）+ `docs/audits/2026-08-08-0135-rc-ma4-a4-2-155-162-contract-runtime.md`（A4.2.158 运行时证据）
- Skill Selection Basis: 实现面 = job bean + job.yaml + config 门控 + notify 派发 + 批量操作（`nop-backend-dev`——镜像 hr 域 `ErpHrContractExpiryJob` 先例 + R1.4/R1.23/R1.27 调度接线范式）；测试（`nop-testing`：JunitAutoTestCase + job 级测试 + notify 模板 seed 范式——对齐 R1.4/R1.23 测试范式）。无 view.xml/xbiz/ORM 变更。

## Infrastructure And Config Prereqs

- 无新外部服务/环境变量。config key 登记 `ErpCtConfigs`：`erp-ct.contract-expiry-cron`（job.yaml cronExpr 消费，空值=跳过语义对齐 R1.4）/`erp-ct.auto-create-renewal-draft`（默认 false）/窗口 config（30/15/7 是否 config 化，Decision）。
- job.yaml 注册于 `app-erp-all/src/main/resources/_vfs/nop/job/conf/erp-ct-contract-expiry.job.yaml`（`enabled: '@cfg:nop.job.erp-ct-contract-expiry.enabled|false'` + cronExpr `@cfg:erp-ct.contract-expiry-cron|0 0 1 * * ?'` + invoker bean/method，对齐 hr 范式）。
- notify 依赖：erp-ct-service pom 已含 notify-dao（compile）+ notify-service（test）——直接复用；通知事件常量（`ct.contract-expiry-warning-30` / `ct.contract-expiry-warning-15` / `ct.contract-expiry-escalation-7` / `ct.contract-expired` 等，Decision 命名）登记 `ErpCtConstants`。
- 分域验证前置：`mvn install -DskipTests` 后 `mvn test -pl module-contract/erp-ct-service`。

## Execution Plan

### Phase 1 - Explore 窗口/上级解析/异常路径裁决（Decision）

Status: completed
Targets: `ErpCtConfigs.java`；`ErpCtConstants.java`；`ErpCtContractBizModel.java`；`module-hr/.../ErpHrContractExpiryJob.java`（先例读）
Skill: `nop-backend-dev`

- Item Types: `Decision | Proof`
- Prereqs: 无（既有基线）

- [x] `Decision` **窗口 config 化裁决（D1）**：30/15/7 三级窗口——选项 A（裁决候选）：config 化（`erp-ct.contract-expiry-warning-days` 数组或三个独立键，默认 30/15/7）；选项 B：常量硬编码（L1 字面值）。**决策记录理由 + 备选**（对齐 hr 单窗口 config 先例 `erp-hr.contract-expiry-warning-days`）。
      **决策记录（已执行）**：**选项 A（config 化——三个独立键）**：`erp-ct.contract-expiry-warning-days-30`（默认 30）/`erp-ct.contract-expiry-warning-days-15`（默认 15）/`erp-ct.contract-expiry-warning-days-7`（默认 7）。理由：(1) 对齐 hr 单窗口 config 先例（`erp-hr.contract-expiry-warning-days` 经 `ErpHrConfigs.contractExpiryWarningDays()` 读取）——每档独立可调，运维不改码即可调整告警节奏；(2) L1 30/15/7 为默认值而非不可变常量，config 化保留业务调优面；(3) 数组键（`30,15,7` 单键逗号分隔）需解析 + 序约束，三个独立键自文档化且边界语义清晰（`remainingDays ≤ d7` → 升级；`≤ d15` → 15 天；否则 → 30 天，逐日运行下合同随时间推进自然穿越三档，每档各通知一次——对齐 L1「30 天通知 / 15 天再次通知 / 7 天升级」）。选项 B（常量硬编码）否决：L1 值固化于代码，运维调整需发版。
      - Skill: `nop-backend-dev`
- [x] `Decision` **上级解析载体裁决（D2）**：7 天升级通知「经办人上级」——选项 A（裁决候选）：contract 无经办人上级字段，经 `IErpMdEmployeeBiz`/部门负责人解析（对齐 R1.4 hr superiorId→department.managerId 兜底链）；选项 B：notify ROLE resolver（模板 ROLE + 角色配置，A1.51 已证实 `resolveRole:117-156`）。**决策记录理由 + 备选**；双 null 时 LOG.warn 跳过（R1.4 范式）。
      **决策记录（已执行）**：**选项 A（nop-auth 平台上级链，实仓核查后落地为「直接上级 → 部门负责人」兜底）**——实仓核查修正：`ErpMdEmployee` 无 superiorId 字段（md orm.xml:861-884 列集：code/name/orgId/position/phone/email/partnerId/status），`ErpMdOrganization` 亦无 managerId → 字面「IErpMdEmployeeBiz」链在 md 域不可落地；**nop-auth 平台实体提供等价链**（nop-auth.orm.xml）：`NopAuthUser.managerId`（上级，propId 24，to-one manager）+ 兜底 `NopAuthUser.deptId → NopAuthDept.managerId`（部门负责人，propId 6）——与 R1.4「superiorId → department.managerId」语义同构且零新增依赖（nop-auth-dao 已在 ct 依赖面，R1.34 引擎已用 INopAuthRoleBiz/INopAuthUserRoleBiz）；经办人 = `contract.createdBy`（ORM createdBy 域即 userId，R1.34 notify 先例同证）。解析链：`authUserBiz.get(createdBy) → user.managerId（非空即返回）→ 兜底 user.deptId → authDeptBiz.get(deptId).managerId → 双 null → LOG.warn 跳过（R1.4 范式）`。通知经 USER_LIST 模板 `${escalationUserId}` 插值（对齐 R1.34 timeout 升级 job 先例）。选项 B（notify ROLE resolver）否决：角色级广播非「经办人上级」个体语义 + 需配置角色成员，精确性低于 A；保留为备选（若未来上级链数据模型变更）。
      - Skill: `nop-backend-dev`
- [x] `Decision` **未完成开票异常路径裁决（D3）**：L1「endDate 到达仍有未完成的开票计划 → 先完成开票再 EXPIRED」——选项 A（裁决候选）：expire 前检查 `isInvoiced=false AND planDate ≤ now` 的 InvoicePlan，存在时先调 `triggerInvoice`（复用既有 Processor）再 expire；选项 B：存在未开票计划时拒绝 expire 并记录（LOG/notify）。**决策记录理由 + 备选**（触发失败不影响 expire 主路径的失败隔离语义）。
      **决策记录（已执行）**：**选项 A（先触发后 expire + 逐条失败隔离）**——`expireOverdueContracts` 逐合同：查该合同行下 `isInvoiced=false AND planDate ≤ today` 的 InvoicePlan（`dateBetween(planDate, epoch, today)` 表达 ≤，XMeta 过滤算子白名单不含 le——对齐 TriggerDuePlansProcessor:39-44 注记），逐条 `contractInvoicePlanBiz.triggerInvoice`（复用既有 Processor 生成 AP/AR 发票草稿 + 回写 isInvoiced）；**触发失败逐条 try/catch WARN 隔离，不影响该合同 expire**（对齐计划文义「触发失败不影响 expire 主路径的失败隔离语义」）；随后 `stateMachine.assertCanExpire` + EXPIRED。选项 B（拒绝 expire）否决：未开票计划将永久阻塞到期推进（状态悬挂复发）+ 与批量自动化目标冲突。注：triggerInvoice 的 ACTIVE 守卫在 expire 前满足（先触发后置 EXPIRED）。
      - Skill: `nop-backend-dev`
- [x] `Decision` **续期草稿触发时点裁决（D4）**：auto-create-renewal-draft——选项 A（裁决候选）：到期时（endDate 到达批量 expire 前）为未续期合同创建续期草稿；选项 B：30 天窗口内早触发。**决策记录理由 + 备选**；草稿字段继承（合同头复制 vs 空模板）Decision。
      **决策记录（已执行）**：**选项 A（到期时创建）**——`expireOverdueContracts` 逐合同在置 EXPIRED 前，config-gated `erp-ct.auto-create-renewal-draft`（默认 false）时创建续期草稿。理由：(1) L1 语义「续期 按配置 auto-create-renewal-draft → 自动创建续期草稿 / 不续期 endDate 到达时自动 EXPIRED」——草稿创建与到期推进同点，30 天窗口早触发会对最终人工续期/终止的合同产生噪声草稿；(2) 幂等守卫：已存在 `parentContractId==原合同 AND status==DRAFT` 的草稿时跳过（防重复运行重复建）。**草稿字段继承 = 合同头复制（非空模板）**：code=`原code-RN`（满足 UK_CT_CONTRACT_CODE_ORG 唯一性）+ contractName/contractType/contractDirection/partnerId/currencyId/orgId/totalAmount 复制 + startDate=原endDate+1 + endDate=原endDate+原时长（duration=原endDate-原startDate）+ status=DRAFT + parentContractId=原合同 id；行不复制（对齐 R1.32 D3 行保留语义——草稿创建后可编辑，L1 后置条件）。空模板否决：续期草稿应承接原合同主体语义，空模板增加经办人重录成本。
      - Skill: `nop-backend-dev`
- [x] `Decision` **cron 键模式裁决（D5）**：job.yaml cronExpr 消费键与 bean 空值跳过键——选项 A（裁决候选）：单键模式（两者共用 `erp-ct.contract-expiry-cron`，对齐 R1.4 `erp-hr-leave-approver-timeout` 先例——其 job.yaml cronExpr 用 `@cfg:nop.job.erp-hr-leave-approver-timeout.cron-expr` 而 bean 内 `erp-hr.leave-approver-timeout-cron` 双键并存，属历史演进；本行键集新建可统一）；选项 B：双键模式（镜像 hr contract-expiry 双键：job.yaml `@cfg:nop.job.erp-ct-contract-expiry.cron-expr` + bean 内 `erp-ct.contract-expiry-cron` 空值跳过）。**决策记录理由 + 备选**（统一键集减少运维认知负担；双键与 hr 域既有部署习惯一致——部署文档 job-scheduling.md 记录键时按裁决对齐）。
      **决策记录（已执行）**：**选项 A（单键模式）**——job.yaml `trigger.cronExpr: "@cfg:erp-ct.contract-expiry-cron|0 0 1 * * ?"` 与 bean 内 `AppConfig.var(CFG_CONTRACT_EXPIRY_CRON, "")` 空值跳过共用 `erp-ct.contract-expiry-cron`。理由：(1) 平台实证（`ConfigExpressionProcessor.parsePrefixExpr` + `ConfigValueResolver.resolveValue:78-99`）：`@cfg:key|default` 在键值空/缺省时回退默认——job.yaml cronExpr 恒非空（调度注册安全），空值语义由 bean 门控承载（「不调度」），单键语义完备；(2) 统一键集减少运维认知负担（部署文档 job-scheduling.md 记录单键）；(3) R1.4/R1.34 双键并存属历史演进（键集各自新建时未统一），本行键集全新可统一。选项 B（双键）否决：冗余键 + 运维需同时理解两键；备选记录——若未来需调度级与执行级 cron 分离（如调度 01:00 但执行门控独立）再引入双键。
      - Skill: `nop-backend-dev`
- [x] `Proof` **既有测试误伤面核查**：grep erp-ct 测试集 `__expire`/`__triggerInvoice` 调用面——job 批量 expire 接线后既有 `TestErpCtContractPosting`/`TestErpCtContractTerminate` 零误伤；`TestErpAllJobYamlLoading` 新增 job.yaml 兼容性核查。
      **核查结论（已执行）**：**零误伤**——(1) `__triggerInvoice` GraphQL 调用面（TestErpCtContractPosting:56/78/103/123、TestErpCtBillingFamily:165/202、TestErpCtContractRebate:196/221、TestErpCtTerminateGate:147）全部为**手工单点触发路径**（IErpCtInvoicePlanBiz.triggerInvoice 语义不变，job 批量路径仅复用同一入口在 expire 前调用，不改既有单点行为）——零冲突；(2) `__expire` 仅 TestErpCtContractTerminate:159（手工 expire 造终态）——job 批量路径为**新增独立入口**（expireOverdueContracts，守卫语义复用 stateMachine.assertCanExpire），不改 `expire()` 单点 mutation——零冲突；(3) `TestErpAllJobYamlLoading` 兼容性：当前 22 个 job.yaml + 硬编码 `assertEquals(22,...)` 两处（:32/:46）——新增 `erp-ct-contract-expiry.job.yaml` 后须 **22→23 同步更新**（计划内调整点，Phase 2 显式检查项）；job.yaml 结构（jobName/enabled/cronExpr/invoker）经 `LocalJobConfigLoader` 加载路径与既有 22 个同型，无兼容性风险。
      - Skill: `nop-testing`

Exit Criteria:

- [x] D1-D5 决策记录落盘（含理由 + 备选）+ 误伤面核查结论（零误伤或已识别调整点）
- [x] job 契约确认（窗口/上级解析/异常路径/续期草稿/cron 键模式语义）

### Phase 2 - ErpCtContractExpiryJob 落地（P1-MA2-071 核心）

Status: completed
Targets: 新 `ErpCtContractExpiryJob.java`；`app-erp-all/_vfs/nop/job/conf/erp-ct-contract-expiry.job.yaml`；`ErpCtConfigs.java`；`ErpCtConstants.java`；`ErpCtErrors.java`（按需）
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1 完成

- [x] `Add` **job bean**（镜像 `ErpHrContractExpiryJob` 范式）：`execute()` 无参方法——cron config 空值跳过 → 三步：(a) 30/15/7 分级提醒扫描（D1 窗口 + D2 上级解析 + `IErpSysNotificationBiz.notify` 逐条失败隔离）；(b) 批量 expire（`ACTIVE AND endDate < now()`，复用既有状态机守卫语义，D3 异常路径）；(c) 续期草稿（D4 触发时点 + config-gated `erp-ct.auto-create-renewal-draft` + parentContractId 关联）。零生产代码 Inject private（包级字段）。
      - Skill: `nop-backend-dev`
- [x] `Add` **job.yaml 接线**（app-erp-all 注册点，enabled 默认 false + cronExpr config 门控 + invoker bean/method）。
      - Skill: `nop-backend-dev`
- [x] `Add` **config/常量/错误码登记**：D1 窗口键 + `erp-ct.auto-create-renewal-draft` + notify 事件常量（D1 命名契约）+ 按需错误码。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] job bean + job.yaml 接线且运行时成立（job 级测试：30/15/7 通知 / 批量 expire / 续期草稿 / cron 空值跳过 / 单条失败隔离）
- [x] `TestErpAllJobYamlLoading` 通过（新增 job.yaml 加载校验；硬编码计数 22→23 同步更新）

### Phase 3 - 测试矩阵

Status: completed
Targets: `module-contract/erp-ct-service/src/test/java/app/erp/ct/service/`（新增 `TestErpCtContractExpiryJob`）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 2 完成

- [x] `Add` 测试组（按 Goals）：① 30/15/7 三级通知（窗口边界断言 + 落库 + recipient 解析 + 无模板静默跳过）；② 批量 expire（ACTIVE+过期 → EXPIRED；未过期零动作）；③ 未完成开票异常路径（D3 契约断言）；④ 续期草稿（config on/off 双路径 + parentContractId 关联 + DRAFT 状态）；⑤ cron 空值跳过；⑥ 单条失败隔离（WARN 不阻断）；⑦ GraphQL RPC 冒烟 + 快照录制（对齐 R1.4/R1.23 范式）。
      - Skill: `nop-testing`
- [x] `Proof` 既有 erp-ct-service 测试零回归：`mvn test -pl module-contract/erp-ct-service`（110 基线 + 新增全绿）+ `TestErpAllJobYamlLoading`（app-erp-all 范围确认）。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 新增测试组全绿 + erp-ct-service 全模块零回归（BUILD SUCCESS）
- [x] 三级提醒/批量 expire/续期草稿有运行时断言证据（job 级实调，非仅静态接线）

### Phase 4 - 文档回填 + arm-index/roadmap 状态

Status: completed
Targets: `docs/design/contract/state-machine.md`（§2/§4/§7 Deferred 注记收敛）；`docs/audits/arm-index.md`；`docs/backlog/requirement-compliance-roadmap.md`；`docs/logs/2026/08-15.md`
Skill: none

- Item Types: `Add | Fix`
- Prereqs: Phase 1-3 完成

- [x] `Add` owner doc 注记：§2 ACTIVE→EXPIRED 自动化注记更新为已实现（job + config 门控 + 批量 expire + 未完成开票异常路径）+ §4 续期草稿注记更新（auto-create-renewal-draft + parentContractId）+ §7 到期提醒注记更新（30/15/7 分级 + 事件名）；不修改需求契约段（use-cases L1 不动）。
      Skill: none
- [x] `Add` arm-index P1-MA2-071 → `done (RC-R1.35)`（注记 reuse 家族收敛：Q9 重开族强制实现完成）+ 修复落地摘要；roadmap RC-R1.35 → done ✅（含落地摘要）；`docs/logs/2026/08-15.md` 日志条目写入。
      Skill: none

Exit Criteria:

- [x] arm-index/roadmap 状态回填 + owner doc Deferred 注记收敛 + 日志条目写入

## Draft Review Record

- Independent draft review iteration 1: accept（独立子代理 ses_ffcb87c25ffesUMVaa153gyBuC）— 0 BLOCKER / 0 MAJOR / 6 MINOR（全部折叠）：job.yaml 计数 19→21 同步 / hr 双键模式注记 + Phase 1 新增 D5 cron 键模式裁决 / owner-doc 陈旧行号修正（§2:51-53/§4:79-81/§7:117）/「R1.34 基线」指代修正 / `TestErpAllJobYamlLoading` 硬编码计数 21→22 显式检查项 / R10 基线漂移预声明（Closure Gate 已含 ≤ 表述 + 先例覆盖）。**计划可标记 active。**

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。**完整仓库验证在此处**：结束时运行一次全量验证。

- [x] 范围内行为完成——P1-MA2-071 到期自动化（job/三级通知/续期草稿/未完成开票异常路径）运行时成立（独立结束审计逐文件核验）
- [x] 相关文档对齐——arm-index/roadmap/owner doc Deferred 注记收敛/日志回填（独立结束审计核验）
- [x] 已运行验证（`mvn test -pl module-contract/erp-ct-service` 全绿 + `TestErpAllJobYamlLoading` 通过 + `mvn clean install -DskipTests` 全量 BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline）
- [x] 无范围内项目降级为 deferred/follow-up（Deferred But Adjudicated 各项有 successor 分类与触发条件，无已确认缺陷/契约漂移）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

（本行结束审计时按实际裁决登记。draft 期已识别候选，供结束审计前定稿：

- **邮件/短信真实通道载体**：Classification `watch-only residual`；站内通知已派发（30/15/7 三级事件经 `IErpSysNotificationBiz` 落库 SENT），邮件/短信属 notify 子系统载体 successor；触发条件 = 外部通知通道集成立项；Successor Required: `yes`。
- **续期审批流联动**（续期草稿经审批引擎完整流）：Classification `out-of-scope improvement`；本行创建 DRAFT 草稿（parentContractId 关联 + 幂等守卫），审批流联动属 RC-R1.34 引擎范围扩展（approval-enabled config-gated，submit 后置生成已就绪——续期草稿经 submit 提交谈判即可接入既有审批链）；触发条件 = 续期审批需求立项；Successor Required: `yes`。
- **前端到期提醒视图/续期操作 AMIS 接线**：Classification `watch-only residual`；后端能力面已提供（job 派发 + 批量 expire + 续期草稿创建）；Successor Required: `no`。

## Closure

Status Note: RC-R1.35（P1-MA2-071 reuse 重开族）全部 4 Phase 完成：Phase 1 D1-D5 裁决落盘 + 误伤面核查（零误伤，TestErpAllJobYamlLoading 22→23 计划内调整点）；Phase 2 落地 `ErpCtContractExpiryJob` + `erp-ct-contract-expiry.job.yaml`（单键 cron 门控）+ `IErpCtContractBiz.scanExpiringContracts/expireOverdueContracts`（@SingleSession 批量 expire + D3 未完成开票先完成 + D4 config-gated 续期草稿）+ config/常量/beans.xml 登记；Phase 3 `TestErpCtContractExpiryJob` 14 组全绿 + erp-ct-service 146/146（132 基线 + 14 新增）零回归 + TestErpAllJobYamlLoading 23/23；Phase 4 回填 state-machine.md §2/§4/§7 Deferred 注记收敛为已实现 + arm-index P1-MA2-071 `done (RC-R1.35)` + roadmap RC-R1.35 done ✅ + 日志条目。全量验证：`mvn clean install -DskipTests` BUILD SUCCESS + compliance checker 全 16 规则 actual == baseline 零漂移（新方法全经 IBiz 注入零新增 daoFor 站点，R2c=1399 不变）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 ses_ffbd8c125ffe2JrKmYFpPlOHkL（新会话结束审计）
- Evidence: 5 门全部 PASS——(1) 计划一致性：全部 14 执行项 + 8 Closure Gates [x] + 4 Phase completed + Plan Status completed；(2) 代码存在性：job bean/job.yaml/IBiz 契约/BizModel D3-D4/config/常量/beans.xml/测试类/TestErpAllJobYamlLoading 23 全部逐文件核验；(3) 反模式：R5 零 @Inject private、R7 零 System.currentTimeMillis、R3 零 new Erp*()、R2b/R2c 零新增 daoFor、ORM 零变更（git diff module-contract/model 空）；(4) 文档回填：state-machine §2/§4/§7 收敛 + arm-index 关闭注记 + roadmap done ✅ + 日志条目；(5) 验证复跑：TestErpCtContractExpiryJob 14/14 + erp-ct-service 146/146 + TestErpAllJobYamlLoading 1/1 + compliance checker actual == baseline（R2c=1399）。2 项 minor 观察（非阻塞）：runInSession 冗余注记已补注释澄清（hr 先例同型包裹——@SingleSession 不经代理生效）；Closure 证据占位已填实。

Follow-up:

- 邮件/短信真实通道载体（notify 子系统 successor，watch-only）
- 续期审批流联动（approval-enabled 引擎扩展，out-of-scope improvement）
- 前端到期提醒视图/续期操作 AMIS 接线（watch-only residual）
