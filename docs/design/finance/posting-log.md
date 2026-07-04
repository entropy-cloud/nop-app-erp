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

四个核心指标，接入 nop-platform 监控大盘：

| 指标 | 定义 | 目标 | 低于阈值的含义 |
|---|---|---|---|
| 自动化记账率 | 自动生成凭证数 ÷ 总凭证数 | ≥95% | 下降说明有新业务场景未被规则覆盖，需补模板/Provider |
| 凭证生成时延 | 业务事件触发到凭证过账耗时（P99） | <30s | 升高说明规则匹配效率下降或 GL 写入瓶颈 |
| 过账异常率 | 过账失败或规则未命中占比 | <1% | 走高说明业务系统数据质量问题或规则配置需更新 |
| 业财闭环成功率 | 源单据 `posted=true` 翻转成功数 ÷ 过账成功数 | ≥99.5% | 走低说明域调用方未正确置位 `posted`，需联合排查 |

> 第四个指标（业财闭环成功率）替代了通用文章的"反写成功率"——本项目反写由域自治置位 `posted`（见 `posting.md` §反写契约），故监控"源单据 `posted` 翻转与凭证过账的一致性"，而非引擎主动反写的成功率。

## 设计原则

1. **过程可观测优先于结果可观测**：规则命中日志记录"为什么这样记账"，比"记了什么账"（凭证本身）更利于排障。
2. **复用平台能力优先于新建实体**：变更审计、操作审计优先复用 nop-platform 审计/变更追踪能力；仅在平台能力不足时才引入 ORM 实体（触及保护区域须人工批准）。
3. **append-only 不可篡改**：合规审计日志只追加，任何修改/删除均需留痕。
4. **traceId 端到端串联**：`PostingEvent` 须携带 `traceId`，使业务域审核 → 事件派发 → 过账编排 → GL 写入可串联。
5. **失败不静默**：任何节点失败必留异常记录，异常工作台是结账前置门控。

## 实现策略

> 本节描述实现方向，具体落地以 `docs/plans/` 计划为准（触及 ORM 保护区域须人工批准并经独立审计）。

1. **规则命中日志**：优先评估在 `ErpFinPostingService.post()` 内置轻量日志（结构化日志 + 可选持久化表），而非新建重型 ORM 实体。`PostingEvent` 增加 `traceId` 字段。
2. **变更审计日志**：优先复用 nop-platform 的实体变更追踪能力（`IEntityHistory` 等）；`ErpFinVoucherTemplate` 启用变更追踪。
3. **异常工作台**：基于规则命中日志的失败记录构建查询页面，提供重试/忽略/补录动作。
4. **运行监控**：`post()` / `reverse()` 入口出口埋点，接入 nop-platform 监控（计数器/计时器/仪表盘）。

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
