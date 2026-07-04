# 业财过账日志与可观测性

## 目的

说明业财过账全链路的日志记录、变更审计、异常处置与运行监控机制。本文件是 `posting.md` 的可观测性配套——`posting.md` 定义"凭证如何正确生成"，本文件定义"生成过程如何被观测、追溯与处置"。

## 边界

- 本域负责：过账全链路的**可观测性数据**——规则命中追溯、模板/规则变更审计、过账异常处置、运行监控指标。
- 本域不负责：业务单据状态反写（由各业务域自治，见 `posting.md` §反写契约）、总账报表呈现（见 `period-close.md`）、用户操作权限审计（nop-auth 通用能力）。
- 持久化字段、字典以 `module-finance/model/app-erp-finance.orm.xml` 为准；本文件只描述稳定语义与契约，不重复字段定义。

## 设计依据

- **Metasfresh 演进证据**：iDempiere 无专门会计日志表；Metasfresh 在 `Fact_Acct` 之上叠加 `X_Fact_Acct_Log` / `X_Fact_Acct_UserChange` / `X_Fact_Acct_Summary` / `X_Fact_Acct_EndingBalance`，证明生产级 ERP 需要"过账事实日志 + 用户改动审计 + 汇总 + 期末余额"四类可观测性数据（见 `docs/analysis/erp-survey/2026-06-22-0000-metasfresh.md`）。
- **本项目现状**：过账引擎抛 `NopException` + `ErrorCode`，不留过程痕迹；模板/规则变更无审计轨迹。排障"为何没自动记账/为何记错科目"与合规审计均无依据（见 `docs/analysis/2026-07-04-finance-posting-engine-gap-vs-opensource.md`）。

## 日志类型

按用途分四类，覆盖排障、合规、运营三个场景：

| 类型 | 用途 | 触发点 | 对标 Metasfresh |
|---|---|---|---|
| 规则命中日志 | 排障：为何这笔单据命中（或未命中）某模板/Provider | `ErpFinPostingService.post()` 完成 Provider 解析与模板匹配后 | `Fact_Acct_Log` |
| 变更审计日志 | 合规：模板/规则/科目映射被谁何时改了什么 | `ErpFinVoucherTemplate` / `ErpFinVoucherTemplateLine` 增改删 | `Fact_Acct_UserChange` |
| 过账异常记录 | 处置：过账失败的凭证与原因，供异常工作台重试/忽略/补录 | `post()` / `reverse()` 抛 `NopException` 时 | `Fact_Acct_Log`（错误分支） |
| 运行监控指标 | 运营：自动化记账率/时延/异常率/成功率 | `post()` / `reverse()` 入口与出口 | （开源未覆盖，运营基础设施） |

> 操作日志（谁何时对哪张凭证做了过账/红冲动作）由 nop-auth 通用审计能力承载，本文件不重复定义。本域聚焦"过账引擎内部过程"的可观测性。

## 规则命中日志

### 记录内容

每次 `post()` 调用无论成功失败均记录一条过账轨迹，包含：

- **追溯键**：`traceId`（端到端串联）、`billHeadCode` + `businessType`（业财回链反查键）、`voucherId`（成功时）。
- **路由结果**：命中的 `IErpFinAcctDocProvider`（域名 + 是否 fallback）、命中的 `ErpFinVoucherTemplate`（`code` + `version`）。
- **未命中原因**（失败时）：`ERR_NO_PROVIDER` / `ERR_TEMPLATE_NOT_FOUND` / `ERR_SUBJECT_NOT_FOUND` / `ERR_UNBALANCED` / `ERR_PERIOD_CLOSED` 等 `ErrorCode`。
- **耗时**：Provider.createFacts、FactsValidator 链、落库各阶段耗时。

### 核心排障场景

| 问题 | 查询路径 |
|---|---|
| "这笔采购入库为什么没自动生成凭证？" | 按 `billHeadCode` 查规则命中日志 → 看未命中原因（无 Provider / 模板缺失 / 期间关闭） |
| "这笔为什么记到了错误科目？" | 按 `voucherId` 查规则命中日志 → 看命中模板 → 查模板行 `subjectCode` 占位符解析结果 |
| "这次规则改了之后影响了哪些凭证？" | 按 `traceId` 串联同一业务事件的全链路日志 |

### 关联穿透

- 凭证 → 规则命中日志：`ErpFinVoucher.voucherId` 反查。
- 规则命中日志 → 业务单据：`billHeadCode` + `businessType` 经 `ErpFinVoucherBillR` 反查源单。
- 分布式追踪：`traceId` 串联业务域审核 → 事件派发 → 过账编排 → GL 写入全链路（`PostingEvent` 须携带 `traceId`）。

## 变更审计日志

### 记录内容

`ErpFinVoucherTemplate` / `ErpFinVoucherTemplateLine` / 科目映射配置的增删改，记录：

- **变更对象**：实体类型 + 业务键（如模板 `code` + `acctSchemaId`）。
- **变更前后**：字段级 diff（科目编码、金额占位符、借贷方向、有效区间）。
- **变更人/时间/原因**：操作人、时间戳、变更原因说明（必填）。

### 合规约束

- 变更审计日志**只追加、不可改、不可删**（append-only）。
- 已过账凭证引用过的模板，其历史版本须可还原（即凭证分录行已固化科目，但模板变更轨迹须保留以解释历史凭证为何如此记账）。
- 反结账后修改凭证的场景，须能追溯"凭证生成时命中的模板版本"与"当前模板版本"的差异。

## 过账异常处置

### 异常工作台

过账失败（模板缺失、科目缺失、借贷不平衡、期间关闭等）的记录进入异常工作台，提供三个处置入口：

| 处置 | 适用场景 | 结果 |
|---|---|---|
| 重试 | 瞬态失败（主数据延迟、并发竞态） | 重新执行 `post()` |
| 忽略 | 业务上确认无需记账（如内部调拨不跨法人） | 标记忽略，不再重试，保留记录 |
| 手工补录 | 规则覆盖不到的特殊业务 | 财务员手工创建凭证，关联源单 |

### 失败不静默丢弃

任何过账节点失败（事件解析、规则匹配、平衡校验、期间门控、科目反查、落库）**必须**留下异常记录，不允许静默吞掉。期末结账前置检查（见 `period-close.md`）会扫描未处置的异常记录，阻止结账。

## 运行监控指标

四个核心指标。**nop-platform 无内建 metrics API**（`CoreMetrics` 仅时钟；全仓无 Micrometer/Prometheus/Actuator 依赖），故采用**应用级指标快照**落地：自动化记账率/异常率/闭环成功率经 SQL 聚合 `ErpFinVoucher`+`ErpFinPostingException` 由查询接口呈现；凭证生成时延经进程内窗口采样（复用 §裁决 2 各阶段 `nanoTimeDiff`）呈现。阈值 config-gated，越限可检出。完整落地裁决见 §实现策略 裁决 3。

| 指标 | 定义 | 目标 | 低于阈值的含义 |
|---|---|---|---|
| 自动化记账率 | 自动生成凭证数 ÷ (自动凭证数 + 手工补录异常数) | ≥95% | 下降说明有新业务场景未被规则覆盖，需补模板/Provider |
| 凭证生成时延 | 业务事件触发到凭证过账耗时（P99，进程内窗口采样） | <30s | 升高说明规则匹配效率下降或 GL 写入瓶颈 |
| 过账异常率 | 过账失败或规则未命中占比 | <1% | 走高说明业务系统数据质量问题或规则配置需更新 |
| 业财闭环成功率 | 源单据 `posted=true` 翻转成功数 ÷ 过账成功数（SYNC 默认下为代理值，见裁决 3 残留风险） | ≥99.5% | 走低说明域调用方未正确置位 `posted`，需联合排查 |

> 第四个指标（业财闭环成功率）替代了通用文章的"反写成功率"——本项目反写由域自治置位 `posted`（见 `posting.md` §反写契约），故监控"源单据 `posted` 翻转与凭证过账的一致性"，而非引擎主动反写的成功率。

## 设计原则

1. **过程可观测优先于结果可观测**：规则命中日志记录"为什么这样记账"，比"记了什么账"（凭证本身）更利于排障。
2. **复用平台能力优先于新建实体**：变更审计、操作审计优先复用 nop-platform 审计/变更追踪能力；仅在平台能力不足时才引入 ORM 实体（触及保护区域须人工批准）。
3. **append-only 不可篡改**：合规审计日志只追加，任何修改/删除均需留痕。
4. **traceId 端到端串联**：`PostingEvent` 须携带 `traceId`，使业务域审核 → 事件派发 → 过账编排 → GL 写入可串联。
5. **失败不静默**：任何节点失败必留异常记录，异常工作台是结账前置门控。

## 实现策略

> 本节为已落地裁决（计划 `2026-07-04-1452-1` Phase 1 Decision）。具体执行证据见该计划与 `docs/logs/`。
> 运行监控（第四类日志）归 5.3 计划 `2026-07-04-1452-3`；本节不涉及，避免与 5.3 owner-doc 修订撞节。

### 裁决 1：变更审计日志载体——复用平台 `NopSysChangeLog`

- **裁决**：`ErpFinVoucherTemplate` / `ErpFinVoucherTemplateLine` 声明 `tagSet` 增加 `audit`（头）+ `audit-save`（含初始保存全量）；模板头设 `orm:bizKeyProp="code"` 使 `NopSysChangeLog.bizKey` 可读。
- **平台能力核实**：`OrmEntityChangeLogInterceptor`（`nop-sys-dao`）在 ORM 层兜底所有写路径——`postUpdate` 用 `orm_forEachDirtyProp` 只遍历已变更字段记 old→new；`postSave` 记初始全量；`postDelete` 记删除标志。开关 `nop.orm.audit.enabled` 默认启用（`app-dao.beans.xml` 的 `feature:on`，空值或 true 即注册拦截器 bean）。
- **替代方案（被拒绝）**：新建 finance ORM 变更审计实体 + 手写 diff 钩子。被拒理由：平台反模式明确（`audit-field-changes.md:94-98`），手写 diff 漏覆盖直接 DAO 写路径，且重复造轮子。
- **残留风险**：平台 `NopSysChangeLog` 无内建 TTL 清理（与 iDempiere `AD_ChangeLog_Delete_Old` 的 `keepDays` Job 不同），表会无限增长。**Follow-up**：注册 nop-job 定期删除超期变更日志（见计划 Deferred「日志 TTL 清理 nop-job」，不阻塞本计划闭合）。
- **代价**：零 ORM 实体新增、零列新增；仅 `tagSet` 声明（低影响模型声明，非新增受保护结构）。

### 裁决 2：规则命中日志载体——成功用结构化日志，失败用持久化异常记录

- **裁决**：混合方案。
  - **成功路径**：在 `ErpFinPostingProcessor` 各 protected step 埋 SLF4J 结构化日志（`traceId`+`billHeadCode`+`businessType`+`voucherId`+命中 Provider/模板+各阶段耗时），**不持久化**（避免凭证表外的高频写放大；排障经日志聚合系统检索）。
  - **失败路径**：新增 finance ORM 实体 `ErpFinPostingException` 持久化失败记录（`traceId`+`billHeadCode`+`businessType`+`ErrorCode`+失败阶段+时间+处置状态+重试次数），供异常工作台查询与处置。
- **平台能力核实**：平台**无**任务/异常队列表可承载过账异常——`nop-job` 是调度引擎、`nop-task` 是流程编排、`NopAuthOpLog` 是用户操作审计，均非"业务异常处置队列"。故复用平台能力不可行，需新增 finance ORM 实体。
- **`txn().afterCommit` 语义核实**（修正项目日志 `docs/logs/2026/07-04.md:55` 的事实错误）：`ITransactionTemplate.afterCommit` 注册 `onAfterCommit` 监听器，**仅在事务提交成功时触发**；主事务回滚时不触发（见 `nop-entropy/.../txn/ITransactionTemplate.java:87-94`）。故**失败记录持久化不可依赖 `afterCommit`**——须用独立 session / `REQUIRES_NEW` 事务写入，确保不随主事务回滚丢失。
- **替代方案（被拒绝）**：全量持久化规则命中日志（成功也建表）。被拒理由：成功路径高频（每笔过账一条），写放大且与凭证本身信息冗余；排障主要诉求是"失败为何失败"，成功路径结构化日志 + 日志聚合已足够。
- **保护区域影响**：新增 `ErpFinPostingException` 实体触及 finance ORM 保护区域（`model/*.orm.xml` 模式 = ask first，required evidence = design doc + plan audit）。本计划已经独立草案审计（见计划 `Draft Review Record`），且回滚策略为模型增量随重新生成移除、无数据迁移。owner-doc（本文件 §过账异常处置）已描述该实体的业务语义与处置状态机。

### 裁决 3：运行监控落地路径——应用级指标快照 + 内存时延采样

- **裁决**：四指标以**应用级快照**落地，零新依赖、零 ORM 保护区域触及。
  - **自动化记账率**：SQL 聚合 `ErpFinVoucher`（自动凭证数）÷（`ErpFinVoucher` + `ErpFinPostingException.resolution=MANUAL`）。手工补录（`manualEntry`）代表规则未覆盖需人工干预的事件，计入分母。
  - **凭证生成时延 P99**：进程内窗口采样（`ErpFinPostingMetrics` 环形缓冲），复用 §裁决 2 各阶段 `nanoTimeDiff` 求和为单次过账时延。**不持久化**——事件触发时间未入库，SQL 不可行；持久化须加列触保护区域，无充分理由。
  - **过账异常率**：SQL 聚合 `ErpFinPostingException` ÷（`ErpFinVoucher` + `ErpFinPostingException`）。
  - **业财闭环成功率**：**代理值**——`posted` 翻转在源域单据（purchase/sales/inventory/...），finance 不可见。SYNC 默认下 post 成功隐含源单 posted 翻转（域自治强一致），故代理 = 过账成功数÷过账成功数 = 1.0；查询结果标注 `loopbackProxyMode=true`。
- **平台能力核实**：nop-platform **无** metrics 埋点 API——`CoreMetrics` 仅时钟（`currentTimeMillis`/`nanoTime`/`nanoTimeDiff`/`nanoToMillis`），全仓无 Micrometer/Prometheus/Actuator 依赖。可用"监控面"仅 `/q/health*`、`/q/metrics*` 平台信息端点（非业务指标埋点）。故"接入平台监控大盘"在本平台不可行，须应用级自建查询接口。
- **替代方案（被拒绝）**：
  - (a) 引入 Micrometer + Actuator + `/q/metrics` 暴露——需新增 pom 依赖 + 部署侧抓取设施，超出应用层最小落地。**Follow-up**：生产部署需 Prometheus 抓取时采纳（见 Deferred「监控大盘可视化」「上游 metrics 模块回迁」）。
  - (c) 持久化 `ErpFinPostingMetric` 快照表 + 定时 rollup——触及 finance ORM 保护区域（`model/*.orm.xml` ask first），且内存采样 + SQL 聚合已满足"指标可查询呈现"目标。
- **残留风险**：
  - (i) 时延为进程内窗口采样，重启清零、无历史趋势（Follow-up：生产趋势须接 Micrometer + 时序库）。
  - (ii) 闭环成功率为代理值，ASYNC 模式或域忘记翻转 `posted` 不可检出（Follow-up：可选 `IErpFinPostedProbe` SPI 让各域上报翻转确认）。
  - (iii) 无自动告警通道对接（Deferred：SMS/邮件/webhook 通道属运营通知层）。
- **代价**：零 ORM 实体新增、零列新增、零 pom 依赖新增；仅新增应用级查询接口 + 进程内采样组件。

### 其他原则

1. `PostingEvent` 增 `traceId` 字段；引擎入口缺失时 `StringHelper.generateUUID()` 生成；透传至 `AcctDocContext` 与异常记录。
2. `post()` / `reverse()` 入口出口埋点接入运行监控归 5.3（本计划仅落前三类日志）。

## 不做边界

- **不做业务单据状态反写**：源单据 `posted` 由域自治置位（见 `posting.md` §反写契约），日志只观测、不驱动反写。
- **不做用户权限审计**：由 nop-auth 通用能力承载。
- **不做总账报表/科目余额表呈现**：见 `period-close.md`（试算平衡表快照）与 nop-report 报表面。
- **不做独立反写记录表**：反写语义由 `posted` 字段 + `ErpFinVoucherBillR` 业财回链承载，无需冗余载体（见 `docs/analysis/2026-07-04-finance-posting-engine-gap-vs-opensource.md` §4 被拒绝方向）。
- **不做强制异步过账**：SYNC 同事务强一致是默认，ASYNC 为可选优化（见 `posting.md` §总体架构）。

## 与其他文档的关系

| 对端文档 | 关系 |
|---|---|
| `posting.md` | 过账机制权威源；本文件定义其可观测性。反写契约见 `posting.md` §反写契约 |
| `state-machine.md` | 凭证状态机异常路径（过账失败、红冲失败）的异常记录落本文件定义的异常工作台 |
| `period-close.md` | 期末结账前置检查扫描本文件的未处置异常记录 |
| `ar-ap-reconciliation.md` | AR/AP 核销回写辅助账项（`ErpFinArApItem`）的 `settledAmount/openAmount/status` 不属本文件范围（核销回写是领域模型事实，非过账日志） |
