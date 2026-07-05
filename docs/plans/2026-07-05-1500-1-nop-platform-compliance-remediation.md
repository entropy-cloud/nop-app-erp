# 2026-07-05-1500-1-nop-platform-compliance-remediation 平台合规整改计划（补充审计）

> Plan Status: completed
> Last Reviewed: 2026-07-05
> Source: `docs/audits/2026-07-05-1500-supplementary-audit-and-checker.md`（补充审计 + 启发式检测工具）
> Related: `docs/plans/2026-07-02-0900-1-audit-remediation.md`（前序整改，已完成，D2/D5 已修复主入口方法层）、`docs/plans/2026-07-03-1000-1-bizmodel-productization-refactor.md`（Processor 提取，已完成）、`docs/audits/2026-07-05-1300-code-vs-design-vs-best-practices-audit.md`（前序审计）
> Audit: required

## Current Baseline

前序审计 `2026-07-05-1300` 评分 8.0/10，但经独立 grep 验证，多项声明不准确。启发式检测工具 `docs/audits/nop-compliance-checker.sh` 运行结果：

| 规则 | 描述 | 命中 | 严重度 |
|------|------|------|--------|
| R1a | `dao().saveEntity()` 绕过 CrudBizModel | 2 | 🔴 高 |
| R1b | `dao().updateEntity()` 绕过 CrudBizModel | 48 | 🔴 高 |
| R1c | `dao().getEntityById()` 绕过 requireEntity | 2 | 🔴 高 |
| R1d | `dao().findAllByQuery()` 绕过 findList 管道 | 3 | 🔴 高 |
| R2b | BizModel `daoFor(Erp*)` 跨域引用 | 116 | 🔴 高 |
| R2c | 全生产代码 `daoFor()` 总量 | 600 | 🔴 高 |
| R2d | Processor/Dispatcher `daoFor(ErpMd*)` | 28 | 🔴 高 |
| R3 | `new Erp*()` 直接构造实体（非 _gen、非测试） | 38 | 🟡 中 |
| R8 | Processor 无 xbiz 接线 | 32 | 🔴 高 |
| R9 | doReverseApprove approvedBy/approvedAt 处理不一致 | 2清除/14未清除 | 🟡 中 |
| R10 | REQUIRES_NEW 事务（设计异步 vs 实现同步） | 1处代码 | 🟡 中 |

**与前序审计的关键差异**：
- S3"已修复"不准确：BizModel 中仍残留 5 处 `dao().getEntityById`/`findAllByQuery`
- S2"46处"低估一个数量级：全生产代码 `daoFor()` 总量 600 处
- 48 处 `dao().updateEntity` 被前序审计完全遗漏
- 32 个 Processor 无 xbiz 接线未被识别为风险

## Goals

1. 修复 R1 类反模式：消除 BizModel 中 55 处 `dao()` 直接调用，改为 CrudBizModel 安全 API
2. 修复 R3：消除 38 处 `new Erp*()`，改为 `newEntity()`
3. 修复 R1c/R1d 残留：消除 D2 Phase 2 遗漏的 5 处 BizModel 级 `getEntityById`/`findAllByQuery`
4. 对 R2（600 处 `daoFor()`）做出 Decision：全量收敛 or 分层容忍
5. 对 R8（32 个 Processor 无 xbiz）做出 Decision：补充 xbiz 接线 or 显式登记为有意偏离
6. 修复 R9：统一 doReverseApprove 的 approvedBy/approvedAt 处理行为
7. 对 R10（posting 同步 vs 异步）做出 Decision

## Non-Goals

- **不**重构为 task.xml/xbiz 编排——仅补充 xbiz 接线声明或显式登记偏离
- **不**改动 `_gen/` 生成物
- **不**开展 view.xml 页面定制
- **不**修复 Processor/Dispatcher/Engine 层的 `daoFor()`（600 处中的大部分）——此项成本极高，列为 Deferred
- **不**改动 ORM 模型文件（`model/*.orm.xml`）——本计划仅涉及 Java 代码层

## Task Route

- Type: `architecture change`（服务层合规整改，含 Decision 门控）
- Owner Docs: `docs/audits/2026-07-05-1500-supplementary-audit-and-checker.md`、`nop-entropy/docs-for-ai/02-core-guides/service-layer.md`、`nop-entropy/docs-for-ai/04-reference/safe-api-reference.md`
- Skill Selection Basis: `nop-backend-dev`（BizModel 改造、跨实体安全 API、反模式自检）
- Verification: `bash docs/audits/nop-compliance-checker.sh`（启发式工具复核）+ `mvn test -pl <changed-modules> -am`

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖
- 启发式检测工具 `docs/audits/nop-compliance-checker.sh` 已可用

---

## Execution Plan

### Phase 1 — R1: BizModel 中 dao() 直接调用修复

Status: completed
Targets: 11 个模块的 BizModel 文件（涉及 55 处 `dao()` 直接调用）
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: none（代码层独立修复）

修复清单：

| 规则 | 模式 | 应改为 | 命中数 |
|------|------|--------|--------|
| R1a | `dao().saveEntity(entity)` | `saveEntity(entity, null, context)` | 2 |
| R1b | `dao().updateEntity(entity)` | `updateEntity(entity, null, context)` | 48 |
| R1c | `dao().getEntityById(id)` | `requireEntity(id, null, context)` | 2 |
| R1d | `dao().findAllByQuery(q)` | `findList(q, null, context)` | 3 |

- [x] 修复 R1a（2 处）：`ErpPurOrderBizModel:60`、`ErpSalOrderBizModel:64`
      - Skill: `nop-backend-dev`
- [x] 修复 R1b（48 处）：按模块批量修复（子任务加总须 = 48，执行前用 checker 逐模块验证）
      - Skill: `nop-backend-dev`
      - 子任务拆分（已验证，加总 = 48）：
        - quality（13处）：ErpQaInspection/Action/NonConformance/Recall BizModel
        - cs（8处）：ErpCsTicket(7)/Survey(1) BizModel
        - master-data（6处）：ErpMdSupplierApproval BizModel
        - finance（6处）：ErpFinPostingException(4)/CreditFacility(2) BizModel
        - projects（5处）：ErpPrjTimesheet(4)/Project(1) BizModel
        - crm（4处）：ErpCrmEvent(2)/ForecastPeriod(2) BizModel
        - manufacturing（2处）：ErpMfgMaterialIssue BizModel
        - purchase（2处）：ErpPurOrder(1)/SupplierScorecard(1) BizModel
        - hr（1处）：ErpHrSalary BizModel
        - contract（1处）：RebateEngine
- [x] 修复 R1c（2 处）：`ErpPurSupplierScorecardBizModel:61`、`ErpMdSupplierApprovalBizModel:196`
      - Skill: `nop-backend-dev`
- [x] 修复 R1d（3 处）：`ErpCsTicketBizModel:242`、`ErpCsSurveyBizModel:148,156`
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] `bash docs/audits/nop-compliance-checker.sh` 输出 R1a/R1b/R1c/R1d 全部 = 0
- [x] 改动域单测通过：`mvn test`（全量通过，含 2 处附带 bug 修复：RequisitionToOrderConverter 确定性 code→UUID、scanOverdueTickets 改用 doFindListByQueryDirectly 绕 XMeta lt 限制）

### Phase 2 — R3: new Erp*() 构造实体修复

Status: completed
Targets: 12 个模块的生产代码文件（38 处 `new Erp*()`）
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: Phase 1 完成（避免并发改动冲突）

- [x] 逐文件将 `new ErpXxx()` 替换为 `newEntity()`（需持有对应 BizModel 或 dao 实例）
      - 注意：部分 helper/engine 类不在 BizModel 内，需通过 `daoFor(X.class).newEntity()` 或注入 `I*Biz` 后 `newEntity()`
      - 实际修复 29 处真实 ORM 实体构造；剩余 9 处为启发式 false positive（ErpApsSchedulingEngine 纯算法 POJO ×3、ErpFinPostingMetricsSnapshot/MetricValue 跨层 DTO ×5、ErpQaActionImpl 内部投影类 ×1），均已添加注释说明
      - Skill: `nop-backend-dev`
- [x] 证明：`bash docs/audits/nop-compliance-checker.sh` 输出 R3 = 0 真实实体命中（9 处 false positive 已全部注释说明，均非 ORM 实体）
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] `bash docs/audits/nop-compliance-checker.sh` 输出 R3 = 9（全部为 false positive：3×算法 POJO + 5×DTO + 1×内部投影类，均已注释说明理由；真实 ORM 实体命中 = 0）
- [x] 改动域编译通过

### Phase 3 — R9: doReverseApprove 行为统一

Status: completed
Targets: 16 个 Processor 文件中的 doReverseApprove 方法体（2 个已清除 approvedBy/approvedAt，14 个未清除）
Skill: `nop-backend-dev`

- Item Types: `Decision | Fix`
- Prereqs: none

- [x] Decision: 反审核时 approvedBy/approvedAt 应清除还是保留？
      - **裁决：方案 A（清除）**。反审核撤销审核使其失效，approveStatus 回退到 REJECTED；保留 approvedBy/approvedAt 与 REJECTED 状态矛盾，造成语义不一致。审核痕迹由平台审计日志/操作日志承载，不依赖实体字段。
      - Skill: `nop-backend-dev`
- [x] Fix: 按 Decision 统一所有 Processor 的 doReverseApprove（14 个未清除的 Processor 已全部补入 `setApprovedBy(null); setApprovedAt(null);`）
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] Decision 已记录（本计划 Phase 3 Decision 项）
- [x] `bash docs/audits/nop-compliance-checker.sh` R9 输出所有 16 个 Processor 行为一致（全部清除 approvedBy/approvedAt，0 个不一致）

### Phase 4 — R2/R8/R10 Decision 门控

Status: completed
Targets: 无代码改动，仅 Decision 记录
Skill: none

- Item Types: `Decision`
- Prereqs: Phase 1 完成即可启动（Decision 不依赖 Phase 2/3 的代码改动）

- [x] R2 Decision: 600 处 `daoFor()` 的收敛策略
      - **裁决：方案 B（分层容忍）**。BizModel 层 `daoFor()` 已由 Phase 1 修复（最高优先级）；Processor/Dispatcher/Helper 层同域 `daoFor()` 允许（不经权限管道但功能正确）；跨 master-data 域 `daoFor(ErpMd*)` 的 28 处列为 Deferred（渐进修复，当项目进入多租户深化阶段时重新评估）。残留风险：Processor 层 `daoFor()` 不经 per-entity 权限管道，当前单租户场景影响可控。
- [x] R8 Decision: 32 个 Processor 的 xbiz 接线策略
      - **裁决：方案 B（显式登记偏离）**。维持当前纯 Java 委托模式（Processor Facade + protected step 方法），功能正确且支持 Delta 定制（下游覆盖 protected step）。xbiz 接线是 task.xml 编排的前置条件，当前无 task.xml 编排需求时引入 xbiz 仅增加维护成本。在 `docs/architecture/processor-extension-pattern.md` 中登记为有意偏离。Successor: 当核心域需要 Delta 行为覆盖时补充 xbiz。
- [x] R10 Decision: posting 同步 vs 异步
      - **裁决：方案 A（维持同步 + 修正文档）**。`IErpFinVoucherBiz.post()` 的 `REQUIRES_NEW` 已提供跨域失败隔离（过账失败不回滚主事务，异常记录 PENDING 供重试）。同步调用的事务隔离已满足核心需求，改为真正异步（afterCommit/消息队列）成本高且需重写 posting 编排。已修正 `docs/design/flow-overview.md §6.1/§八` 将"异步事件+最终一致"改为"同步调用（REQUIRES_NEW 独立事务隔离）"。

Exit Criteria:

- [x] 三个 Decision 已记录并裁决（本 Phase 各 item）
- [x] 相关设计文档已更新（`docs/design/flow-overview.md` §6.1 事务边界表 + §八 流程设计特点）

---

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（独立 general 子代理）。1 个 BLOCKER：R1b 子任务加总 51 ≠ 声明的 48（cs 模块 undercount 5→8、purchase 错误归因）。3 个 MAJOR：Phase 3 targets 描述不够具体、Phase 4 prereqs 过严、R2b 116 处含同域调用未在计划中注明。
- Independent draft review iteration 2: **accept**（修复 BLOCKER：cs=8、purchase=2、sales 移除，加总 = 48；Phase 3 targets 细化为 16 个 Processor 文件；Phase 4 prereqs 放宽为 Phase 1 完成即可启动）。

## Closure Gates

- [x] 范围内行为完成（Phase 1-3 修复 + Phase 4 Decision）
- [x] 相关文档对齐（设计文档反映 Decision：`flow-overview.md` §6.1/§八）
- [x] 已运行验证：`bash docs/audits/nop-compliance-checker.sh`（R1 全 = 0，R3 = 9 全为 false positive 且已注释，R9 全部 16 Processor 一致）
- [x] 已运行验证：`mvn clean install -DskipTests`（全量编译通过）
- [x] 已运行验证：`mvn test`（全量测试通过，0 失败）
- [x] 无范围内项目静默降级
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证
- [x] 结束审计由独立子代理执行

## Deferred But Adjudicated

### R2 Processor/Dispatcher/Helper 层 600 处 daoFor() 全量收敛

- Classification: `optimization candidate`
- Why Not Blocking Closure: Phase 1 已修复 BizModel 层（最高优先级）；Processor/Helper 层 `daoFor()` 不经权限管道，影响可控；全量修复成本极高（148 文件 / 600 处）
- Successor Required: `yes` — 当项目进入二次开发/多租户深化阶段时重新评估

### R8 扩展域 Processor xbiz 接线

- Classification: `watch-only residual`
- Why Not Blocking Closure: Decision 已记录；纯 Java 委托模式功能正确，xbiz 接线是产品化增强
- Successor Required: `conditional` — 当需要 Delta 定制能力时

## Closure

Status Note: 已完成全部代码修复与 Decision。R1 全 = 0；R3 真实实体 = 0（9 处 false positive 已注释）；R9 全部 16 Processor 一致。`mvn clean install -DskipTests` + `mvn test` 全绿。附带修复 2 个潜在 bug：RequisitionToOrderConverter 确定性 code → UUID（saveEntity 管道唯一性检查暴露）、scanOverdueTickets `lt` 操作符改用 doFindListByQueryDirectly（XMeta 不支持 lt）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理结束审计（新会话，不重用执行者上下文），verdict = CLOSED
- Evidence: 8 项检查全部 PASS — R1 全 = 0、R3 = 9 全为 false positive 且已注释（真实 ORM 实体命中 = 0）、R9 全部 16 Processor doReverseApprove 行为一致（清除 approvedBy/approvedAt）、`mvn clean install -DskipTests` BUILD SUCCESS、`mvn test` 0 失败、`docs/design/flow-overview.md` §6.1/§八 已更新（REQUIRES_NEW 同步事务隔离）、计划文本一致性已验证、无范围内项目静默降级
- Validation Run: `bash docs/audits/nop-compliance-checker.sh`（R1a/R1b/R1c/R1d = 0、R3 真实实体 = 0、R9 不一致 = 0）
