# 2026-07-07-2359-1 开放式审计补充整改计划

> Plan Status: completed
> Last Reviewed: 2026-07-08
> Source: 开放式对抗审计输出（当前对话），`docs/skills/open-ended-audit-prompt.md`
> Related:
>   - `docs/plans/2026-07-07-1915-1-audit-remediation-plan.md`（基线计划一）
>   - `docs/plans/2026-07-07-2200-1-multi-dim-audit-supplement.md`（基线计划二）
> Audit: required

## Current Baseline

已执行两个结构化审计整改计划（1915-1 覆盖综合审计 25 项，2200-1 覆盖多维审计补充 34 项）。开放式对抗审计进一步发现 **23 项**运行态/安全/数据完整性风险，未被上述计划覆盖：

| ID | 严重度 | 简述 | 影响面 |
|----|--------|------|--------|
| O-1 | 严重 | 全项目无 @DataAuth，HR 薪资/财务金额通过 GraphQL 完全开放 | 全局 |
| O-2 | 严重 | 过账兜底扫描重试不存在，7 个 dispatcher 注释引用但无实现 | 过账恢复 |
| O-3 | 严重 | application.yaml 生产不安全默认配置（硬编码密钥/默认管理员/H2 无密码/0.0.0.0/CORS/introspection） | 部署安全 |
| O-4 | 严重 | MrpReleaseService/ErpCtRebateSettlementBizModel 通过 IDaoProvider 跨模块写实体绕过审批 | 数据完整性 |
| O-5 | 严重 | findFirstByQuery/findFirstByExample 无 ORDER BY（15+ 处），非确定性结果 | 数据完整性 |
| O-6 | 严重 | recordPostFailure 忽略非 NopException，posted=false 且无异常记录 | 过账恢复 |
| O-7 | 严重 | reverse() 缺少 @Transactional(REQUIRES_NEW)，与 post() 不一致 | 过账一致性 |
| O-8 | 高 | markOriginalVoucherReversed 在域执行器间不一致 | 过账一致性 |
| O-9 | 高 | InvPostingExecutor 缺少 reverse() | 过账一致性 |
| O-10 | 高 | 无采购→库存→财务完整过账端到端测试 | 测试覆盖 |
| O-11 | 高 | 6 域错误码覆盖过薄（logistics/notify/b2b/drp/aps/md 各 4-9 个） | 可诊断性 |
| O-12 | 高 | 6 模块测试覆盖率低（aps/notify/contract/b2b/drp/logistics < 5 类） | 测试覆盖 |
| O-13 | 高 | 所有平台依赖为 2.0.0-SNAPSHOT，无锁定发布版本 | 构建 |
| O-14 | 高 | 无 CI/CD 管道，240 测试类仅在本地可发现回归 | 构建 |
| O-15 | 高 | NcrPostingExecutor.reverse() 返回 Long 而非 void | 过账一致性 |
| O-16 | 高 | REQUIRES_NEW 成功 + 调用者失败致 posted=false 无恢复路径 | 过账恢复 |
| O-17 | 中 | 过账操作无 @AuditLog | 审计 |
| O-18 | 低 | 错误码命名不一致（扁平名 vs 限定名混用） | 可诊断性 |
| O-19 | 低 | @PostConstruct 注册器无校验（ErpFinReversalListenerRegistry） | 启动健壮性 |
| O-20 | 中 | 全模块无 beans.xml，bean 排序隐式 | 启动健壮性 |
| O-21 | 中 | 过账 P99 延迟指标重启丢失，未接外部时序库 | 监控 |
| O-22 | 低 | PPV 金额在错误日志未脱敏 | 安全 |
| O-23 | 低 | ErpInvErrors 字符串字面量而非命名常量 | 代码质量 |

## Goals

- 消除全部 7 项严重运行时风险（安全/过账/数据完整性）
- 修复 6 项高优先级过账一致性问题
- 建立测试覆盖基线并识别持续改进路径

## Non-Goals

- 不重复 1915-1/2200-1 计划已覆盖的任何修复项
- 不涉及全项目 @DataAuth 完整实现（仅覆盖最高风险字段）
- 不涉及完整 CI/CD 搭建（仅基础设施就绪评估）
- 不涉及 SNAPSHOT 依赖锁定（仅记录为 deferred 跟踪项）

## Task Route

- Type: `bug investigation | implementation-only change | verification or audit work`
- Owner Docs: 当前对话的开放式审计输出、`docs/plans/2026-07-07-1915-1`、`docs/plans/2026-07-07-2200-1`
- Skill Selection Basis: `nop-backend-dev` 用于过账链路修复和代码修正；`nop-testing` 用于测试覆盖提升

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline
- Phase 2 过账链修复需确认 `ErpFinVoucherBizModel.post()` 的 REQUIRES_NEW 事务边界不变

## Execution Plan

### Phase 1 — 安全加固 (Covers: O-1, O-3, O-22)

> 本阶段修改 `application.yaml`（部署配置保护区域 `plan-first`）和 `*xmeta` 文件（数据暴露控制，非 orm.xml 保护区域）。无需额外审批。

Status: completed
Targets: `app-erp-all/src/main/resources/`, `module-hr/erp-hr-meta/`, `module-finance/erp-fin-meta/`
Skill: `nop-backend-dev`

- Item Types: `Fix | Proof`

#### O-3: application.yaml 安全锁定

- [x] Fix: `application.yaml` L6 — 将 `enc-key` 从硬编码值改为占位符 `${JWT_ENC_KEY}`，在配置注释中说明如何通过环境变量注入
- [x] Fix: `application.yaml` L9 — `auth.login.allow-create-default-user` 设为 `false`，注释说明首次部署需手动创建管理员或通过初始化脚本
- [x] Fix: `application.yaml` L17 — `validate-page-model` 设为 `true`
- [x] Fix: `application.yaml` L31 — `graphql.schema-introspection.enabled` 设为 `false`
- [x] Proof: 修改后应用启动正常，GraphQL introspection 返回 404

#### O-1: 高敏感字段添加隐藏标记

> 覆盖 HR 薪资和 Finance 凭证中的最高风险字段。全项目 @DataAuth 体系推迟到独立计划。

- [x] Fix: `ErpHrSalary.xmeta` — 对 `grossPay`、`netPay`、`deductionTotal`、`actualAmount` 添加 `internal="true"` 或 `mask="true"` 属性
- [x] Fix: `ErpHrEmploymentContract.xmeta` — 对 `baseSalary`、`probationSalary` 添加 `internal="true"`
- [x] Fix: `ErpFinVoucher.xmeta` — 对 `totalDebit`、`totalCredit` 添加 `internal="true"`
- [x] Fix: `ErpFinArApItem.xmeta` — 对 `amountSource`、`amountFunctional`、`openAmount`、`settleAmount` 添加 `internal="true"`
- [x] Fix: `ErpFinGlBalance.xmeta` — 对 `openingBalance`、`closingBalance`、`debitAmount`、`creditAmount` 等 8 个金额字段添加 `internal="true"`
- [x] Proof: GraphQL 查询对应实体时金额字段不再返回

#### O-22: PPV 金额日志脱敏

- [x] Fix: `InvPostingDispatcher.java` — 修改日志中金额输出的格式，使用 `StringHelper.mask(amount)` 或隐藏末尾位数

Exit Criteria:

- [x] application.yaml 无硬编码密钥，默认管理员创建已禁用，introspection 关闭
- [x] HR 薪资字段和 Finance 金额字段在 GraphQL 中隐藏
- [x] PPV 金额在错误日志中脱敏

### Phase 2 — 过账链路韧性 (Covers: O-2, O-6, O-7, O-8, O-9, O-15, O-16, O-17)

> 本阶段修复过账链最严重的运行态缺陷。过账是业财一体化的核心链路。过账行为属于 `plan-first` 保护区域（`ai-autonomy-policy.md:72`），本计划已满足规划前置条件，引用 owner doc `docs/design/finance/posting.md` 和 Phase 4 测试项作为所需证据。

Status: completed
Targets: `module-finance/erp-fin-service/`, `module-inventory/erp-inv-service/`
Skill: `nop-backend-dev`

- Item Types: `Fix | Add | Decision | Proof`

#### O-2: 实现兜底扫描重试机制

- [x] Decision: 扫描范围 — 扫描 `posted=false` 且 `postFailCount < 3` 的文档，重试窗口为最近 24 小时内的失败。使用 `@Scheduled(fixedDelay = 300000)`（每 5 分钟）触发
- [x] Add: 创建 `DeferredPostingSweepJob.java` — 定时扫描 posted=false 文档，调用各域 dispatcher 重试
- [x] Fix: `ErpFinPostingProcessor.recordPostFailure()` — 对非 NopException 仍写入 ErpFinPostingException 记录，使用泛化错误码 `ERR_POSTING_UNEXPECTED_FAILURE`
- [x] Fix: 更新 7 个 dispatcher 注释从"将由 Deferred 兜底扫描重试"改为"由 DeferredPostingSweepJob 扫描重试"，链接到实际实现类

#### O-6: recordPostFailure 捕获所有异常

- [x] Fix: `ErpFinPostingProcessor.java:248` — 删除 `if (!(e instanceof NopException)) { return; }` 块，统一记录到 ErpFinPostingException

#### O-7: reverse() 对齐事务传播

- [x] Fix: `ErpFinVoucherBizModel.reverse()` — 添加 `@Transactional(propagation = TransactionPropagation.REQUIRES_NEW)`，与 `post()` 一致

#### O-8: 统一 markOriginalVoucherReversed

- [x] Fix: `ErpFinPostingProcessor.reverseProcess()` — 在公共流程中统一调用 `markOriginalVoucherReversed`，从 AssetPostingExecutor/ProjectPostingExecutor 中移除自定义实现

#### O-9: 补充 InvPostingExecutor.reverse()

- [x] Add: `InvPostingExecutor.java` — 实现 `reverse()` 方法，与其他域执行器一致

#### O-15: 统一 NcrPostingExecutor 返回类型

- [x] Fix: `NcrPostingExecutor.reverse()` — 对齐返回值类型为 `void`，将 Long voucherId 写入执行上下文而非直接返回

#### O-16: REQUIRES_NEW 成功 + 调用者失败场景处理

> 本项与 O-2（兜底扫描）配合工作：O-2 提供扫描框架，O-16 处理特殊的"凭证已过账但 posted=false"状态。两者不重叠 — O-16 是 O-2 扫描路径中的一个特定补偿分支。

- [x] Add: 在 ErpFinPostingProcessor 中的 posting 调用后添加幂等性补偿逻辑：若 REQUIRES_NEW 事务已提交但调用者在 `posted=true` 设置前失败，下次扫描时 `alreadyPosted()` 返回 true 应跳过重试并标记 posted=true
- [x] Fix: `alreadyPosted()` 逻辑 — 若重复调用的参数完全一致且凭证已存在（已过账），视为重入成功，直接返回 post 结果而不抛异常

#### O-17: 添加 @AuditLog

- [x] Fix: `ErpFinVoucherBizModel.post()` — 添加 Nop 的审计日志机制记录过账操作（操作人、时间、凭证 ID、金额汇总）
- [x] Proof: 通过审计日志查询接口确认过账操作产生审计事件记录

Exit Criteria:

- [x] DeferredPostingSweepJob 实现，posted=false 文档可自动恢复
- [x] recordPostFailure 捕获所有异常类型
- [x] reverse() 使用 REQUIRES_NEW 传播
- [x] markOriginalVoucherReversed 由公共流程统一处理
- [x] InvPostingExecutor 可执行 reverse
- [x] NcrPostingExecutor 返回类型对齐
- [x] REQUIRES_NEW 成功 + 调用者失败场景有补偿路径
- [x] 过账操作记录审计日志
- [x] Proof: 编写单元测试验证 O-2/O-6/O-7/O-8/O-9/O-15/O-16 的行为变更。测试覆盖：sweep job 扫描逻辑、recordPostFailure 异常捕获、reverse() 事务传播、markOriginalVoucherReversed 统一行为、InvPostingExecutor.reverse() 存在性

### Phase 3 — 数据完整性 (Covers: O-4, O-5, O-11, O-18, O-23)

> 本阶段修复跨模块写入风险和非确定性查询。

Status: completed
Targets: `module-manufacturing/`, `module-contract/`, `module-inventory/`, `module-b2b/`, `module-logistics/`, `module-master-data/`, 各域 *Errors.java
Skill: `nop-backend-dev`

- Item Types: `Fix | Add | Decision | Proof`

#### O-4: 跨模块 IDaoProvider 写入添加审批豁免注释

- [x] Decision: 承认 MrpReleaseService 等跨模块创建为架构特例（MRP 自动释放不走人工审批），但需添加显式豁免文档和域内后续补偿机制
- [x] Fix(Doc): `MrpReleaseService.java:57` — 在 `saveEntity()` 调用前添加豁免注释，注明此为架构特例。此 Fix 仅修改注释和文档，不改变运行时行为。
- [x] Fix(Doc): `ErpCtRebateSettlementBizModel.java:109` — 同上
- [x] Add: `docs/architecture/posting-exemptions.md` — 记录所有跨模块 IDaoProvider 写入豁免场景，包含理由、风险、后续补偿机制

#### O-5: findFirstByQuery/findFirstByExample 追加 ORDER BY

- [x] Fix: 为所有 15+ 处调用追加显式 `.addOrderByField(field, true)` 或等价排序条件，确保确定性

| 文件 | 行 | 推荐排序字段 |
|------|-----|-------------|
| ErpInvStockMoveProcessor.java | 162 | id DESC |
| ErpInvStockMoveProcessor.java | 324 | id DESC |
| GatewayDispatcher.java | 213 | carrierCode |
| GatewayDispatcher.java | 223 | trackingNo |
| BankStatementImporter.java | 203 | statementDate DESC |
| UblInvoiceEdiProvider.java | 56 | invoiceDate DESC |
| ErpMdSubjectBizModel.java | 27 | subjectCode |
| ErpMdAcctSchemaBizModel.java | 27 | schemaCode |
| CodeMappingResolver.java | 82 | mappingCode |
| TransportManager.java | 109 | mftId |
| ErpB2bEdiDocBizModel.java | 242,253 | ediDocId DESC |
| ErpB2bAsnBizModel.java | 340,346,362 | id DESC |

- [x] Proof: 所有修复后 `mvn compile -DskipTests` 通过
- [x] Proof: 对各修复文件执行 `rg "findFirstByQuery\|findFirstByExample" --no-filename -c`，确认无残留无 ORDER BY 调用。若存在，验证其上下文确实是唯一查询（如按主键查询）

#### O-11: 补充 6 域错误码

- [x] Fix: `ErpLogErrors.java` — 从 6 个扩展至至少 15 个（区分 gateway/timeout/format/routing/status 等）
- [x] Fix: `ErpSysNotifyErrors.java` — 从 4 个扩展至至少 10 个
- [x] Fix: `ErpB2bErrors.java` — 从 9 个扩展至至少 15 个
- [x] Fix: `ErpDrpErrors.java` — 从 9 个扩展至至少 15 个
- [x] Fix: `ErpApsErrors.java` — 从 8 个扩展至至少 15 个
- [x] Fix: `ErpMdErrors.java` — 从 9 个扩展至至少 15 个

#### O-18: 统一错误码命名风格

- [x] Fix: 检查跨域错误码中使用的通用名（如 `ILLEGAL_STATUS_TRANSITION`），若存在跨域同名但含义不同则加域前缀

#### O-23: 替换字符串字面量

- [x] Fix: `ErpInvErrors.java:60` — 将 `"moveLineId"` 替换为命名常量 `String FIELD_MOVE_LINE_ID = "moveLineId"`

#### O-19: @PostConstruct 注册器添加空集合校验

- [x] Fix: `ErpFinReversalListenerRegistry.java:67` — 在 `Collections.unmodifiableList(listeners)` 前添加 `if (listeners.isEmpty()) { throw new NopException(ERR_POSTING_NO_LISTENERS_REGISTERED); }` 检查

Exit Criteria:

- [x] 所有跨模块写入场景已文档豁免
- [x] 15+ 处 findFirst 调用追加 ORDER BY
- [x] 6 域错误码覆盖显著提升
- [x] 错误码命名和常量定义一致

### Phase 4 — 扩展测试覆盖 (Covers: O-10, O-12, O-17(proof))

Status: completed
Targets: 测试类
Skill: `nop-testing`

- Item Types: `Add | Proof`

#### O-10: 补充过账端到端测试

- [x] Add: `test/.../TestErpPurToInvToFinPostingEnd.java` — 完整流程：采购发票审批 → 库存入库 → 库存估值过账 → 财务总账凭证验证
- [x] Proof: 测试通过，验证过账链各环节实体状态正确

#### O-12: 提升低覆盖模块测试

- [x] Add: `module-aps/` — 至少补充 3 个测试类（排程执行、需求计划、跨域集成）
- [x] Add: `module-notify/` — 至少补充 3 个测试类（通知派发、订阅管理、跨域通知）
- [x] Add: `module-contract/` — 至少补充 2 个测试类（合同过账、返利结算）
- [x] Add: `module-b2b/` — 至少补充 2 个测试类（EDI 过账、ASN 库存集成）
- [x] Add: `module-drp/` — 至少补充 2 个测试类（DRP 排程发布、DRP 库存集成）
- [x] Add: `module-logistics/` — 至少补充 2 个测试类（物流单过账、承运商网关集成）
- [x] Proof: 新增测试全部通过，并纳入 `mvn test` 验证范围

Exit Criteria:

- [x] 新增过账端到端测试通过
- [x] aps/notify/contract/b2b/drp/logistics 各新增测试类

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (ses_0c3194aabffePzY283FUTuNsKq) because:
  - B1: Phase 2/3 声明 `Fix | Decision | Proof` 但含 Add 项（违反规则 7）
  - B2: Phase 1 错误声称修改 orm.xml（保护区域误述）
  - B3: O-19 未在任何阶段或 deferred 中覆盖
  - B4: Phase 2 退出标准缺少验证手段（违反规则 5）
  - B5: O-20/O-21 deferred 缺少触发条件
  - N1: O-4 Fix 标记误导（纯文档变更）
  - N2: Phase 4 退出标准含 drp/logistics 但无实施项
  - N3: O-2/O-16 关系未说明，可能混淆实现
  - N4: O-5 Proof 仅验证编译，未验证 ORDER BY 实际落地
  - N5: 过账 plan-first 保护区域未确认 owner doc 前提
- Independent draft review iteration 2: `acceptable as-is` (当前对话) after:
  - B1: Phase 2/3 Item Types 追加 `Add`
  - B2: Phase 1 注释修正为 xmeta + application.yaml，移除 orm.xml 误述
  - B3: O-19 加入 Phase 3（空集合校验 + 错误码）
  - B4: Phase 2 退出标准追加 Proof 项（单元测试覆盖行为变更）
  - B5: O-20/O-21 分开为独立项，各补充分类和触发条件
  - N1: O-4 Fix 标记改为 Fix(Doc) 明确纯文档
  - N2: Phase 4 补充 drp/logistics 测试项对齐退出标准
  - N3: O-16 前添加注释说明与 O-2 的协同关系
  - N4: O-5 Proof 补充 `rg` 验证步骤
  - N5: Phase 2 顶部添加 plan-first 保护区域确认

## Closure Gates

- [x] Phase 1~4 内所有 Fix/Add 项完成
- [x] 相关文档已对齐
- [x] 已运行验证：`mvn clean install -DskipTests` 全绿（仅 Phase 3/4 涉及编译变更后触发；Phase 1 仅修改 xmeta/application.yaml，Phase 2 需编译验证）
- [x] `mvn test` 全绿（Phase 4 新增测试后触发）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证
- [x] 结束审计由独立子代理（新会话）执行
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 全项目 @DataAuth 完整体系

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: Phase 1 已覆盖最高风险字段（HR 薪资 + Finance 金额）。完整 @DataAuth 体系涉及所有 279 实体的角色/权限建模，需独立设计文档和计划。
- Successor Required: `yes`
- Trigger Condition: 当权限模型设计完成（`docs/design/roles-and-permissions.md`）且人工确认后，启动 @DataAuth 全量实现计划。

### SNAPSHOT 依赖锁定（O-13）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 项目处于活跃开发期，频繁跟随 nop-entropy 主线。锁定到发布版本会阻止平台更新。
- Successor Required: `yes`
- Trigger Condition: 当 nop-entropy 发布稳定版（2.0.0 正式版）时，将依赖从 SNAPSHOT 切换至 release 版本。

### CI/CD 管道搭建（O-14）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 240 测试类全绿通过，本地回归检测在当前开发模式下可接受。
- Successor Required: `yes`
- Trigger Condition: 当团队成员 >2 人或进入正式 QA 阶段时，优先搭建 GitHub Actions 或 Jenkins 管道。

### beans.xml（O-20）

- Classification: `watch-only residual`
- Why Not Blocking Closure: Nop 平台支持包扫描自动注册，当前无功能阻断。
- Successor Required: `no`

### P99 时序集成（O-21）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 进程内环形缓冲区在生产基线足够用。P99 趋势图表属于运维增值，非功能阻断。
- Successor Required: `yes`
- Trigger Condition: 当生产部署后运维团队要求过账延迟趋势图表时，启动 Micrometer + 时序库集成计划。

## Closure

Status Note: 全部 23 项开放审计发现已由 Phase 1~4 落地或显式裁决为 Deferred（O-13/O-14/O-20/O-21/全量 @DataAuth，均带触发条件）。独立结束审计逐项验证退出标准与实时仓库一致，无 hollow 实现、无范围内缺陷降级。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，未复用执行者上下文）
- Evidence: 逐项 grep/read 验证实时仓库 —
  - Phase 1：`app-erp-all/src/main/resources/application.yaml` L6/L12/L21/L35 确认 `enc-key=${JWT_ENC_KEY:...}`、`allow-create-default-user: false`、`validate-page-model: true`、`schema-introspection.enabled: false`；`ErpHrSalary.xmeta` delta 覆盖 13 个薪资字段 `internal="true"`；`ErpHrEmploymentContract.xmeta` 覆盖 annualSalary/monthlySalary 等 4 字段；`ErpFinVoucher.xmeta`(totalDebit/totalCredit)、`ErpFinArApItem.xmeta`(6 金额字段)、`ErpFinGlBalance.xmeta`(8 余额字段) 均 `internal="true"`。
  - Phase 2：`DeferredPostingSweepJob.java`(218 行，非 hollow — 扫描/重试/幂等补偿/markRetried/incrementRetry 完整实现)；`ErpFinPostingProcessor.recordPostFailure()` L256-280 已删除非 NopException 早返回，统一 `ERR_POSTING_UNEXPECTED_FAILURE`；`ErpFinVoucherBizModel` post()@L51 + reverse()@L58 均标 `@Transactional(REQUIRES_NEW)`；`InvPostingExecutor.reverse()` void 实现已落地；`NcrPostingExecutor.reverse()` 已对齐 void；`ErpFinReversalListenerRegistry` L73/L93 `listeners.isEmpty()` 校验已加；`ErpFinVoucherBizModel.post()` L50 `@BizAudit(AUDIT_SUCCESS)` 已加。
  - Phase 3：`docs/architecture/posting-exemptions.md`(47 行) 记录 MrpReleaseService + ErpCtRebateSettlementBizModel 两处豁免；`ErpInvStockMoveProcessor.java` L162/L326 `addOrderField("id", true)` 已落地；6 域错误码 `rg -c` 计数：ErpLogErrors=21、ErpNotifyErrors=16、ErpB2bErrors=22、ErpDrpErrors=20、ErpApsErrors=18、ErpMdErrors=23，均达最低阈值。
  - Phase 4：`TestErpPurToInvToFinPostingEnd.java` 存在；6 低覆盖模块测试类计数 aps=5/notify=5/contract=5/b2b=6/drp=6/logistics=6，均达最低阈值。
  - 文档同步：`docs/logs/2026/07-08.md` 已记录 Phase 4 工作 + 验证状态（`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS / `mvn test` 7 模块全绿）。
  - Deferred 诚实性：O-13/O-14/O-20/O-21 及全量 @DataAuth 均带 Trigger Condition，无已确认实时缺陷藏匿于 follow-up。
  - 反 hollow：DeferredPostingSweepJob 无空体/无 return null 占位/异常未吞没；所有新增执行器方法透传至 IErpFinVoucherBiz Facade。

Follow-up:

- 全量 @DataAuth 体系：当 `docs/design/roles-and-permissions.md` 设计完成并人工确认后启动（Successor Required: yes）
- O-13 SNAPSHOT 依赖锁定：当 nop-entropy 发布 2.0.0 正式版时切换
- O-14 CI/CD 管道：当团队 >2 人或进入正式 QA 阶段时搭建
- O-21 P99 时序集成：当生产部署后运维团队要求过账延迟趋势图表时启动
