# 跨域流程编排（ERP 应用层）

## 定位

本文是 nop-app-erp 应用层**跨域流程编排**的落位说明：在既有业务动作（BizMethod/Processor）、审批流（nop-wf 审批）、事件驱动编排之上，回答「更长的跨域业务链是否需要流程引擎、以什么形态接入」。

它与既有文档的分工：

| 文档 | 覆盖范围 |
|------|----------|
| `docs/design/flow-overview.md` | 业务流总览（四层架构、核心流程、状态映射）——业务语义视角 |
| `docs/architecture/wf-integration-design.md` | **审批**流集成（`use-approval`、DIRECT↔WORKFLOW、xbiz 三层桥接） |
| `docs/architecture/cross-domain-flow-orchestration.md`（本文） | 审批之外的**流程编排**：必要性分析 + 形态裁定 |
| `docs/architecture/processor-extension-pattern.md` | 单动作（per-mutation）Processor 契约 |
| `docs/architecture/entity-state-machine-bean.md` | 实体状态机 Bean 契约（状态轴治理） |

**关键区分**：`wf-integration-design.md` 管「单个单据的审批」；本文管「多单据/多域串成一条流程」的编排。

## 用户方向澄清（2026-08-12）

针对「跨域流程编排是否用 task.xml 把业务链任务化」的讨论，方向已澄清并记入 `docs/design/flow-overview.md`：

1. **不整体替换现有流程实现**——既有的 Processor 链（同步强一致）+ 事件链（最终一致）+ 审批链（nop-wf）保持不动。
2. **采用「补充 wf 关联」形态**——每个现有 BizMethod 是流程的一部分，通过**关联到 wf 节点**的方式纳入流程视图；`nop-wf`（而非 `nop-task`）执行流程配置。
3. **BizMethod 不可变**——保持「输入 → Processor → 输出」的既有形状，作为流程步骤的对齐 cadence（流程引擎调用 BizMethod，不重写 BizMethod）。
4. **先分析必要性，再定形态**——不是所有链都需要流程引擎；分析先行，产出判定矩阵。
5. **子域可能独立微服务**——若每个子域独立部署，需全局协调器跨子域编排（承载流程定义 + 跨服务调用 BizMethod）。

本文是上述澄清的架构化落位。

## 现状编排模型（基线盘点）

### 单动作：processor-per-mutation（强制）

每个 `@BizMutation` 对应一个独立 `<Entity><Method>Processor`（`processor-extension-pattern.md`，processor-per-mutation 强制原则）：

```
输入(请求+实体状态) → Processor(protected 步骤序列, 派生 bean 可覆盖) → 输出(实体新状态)
```

- BizModel 仅 API 入口/参数解析/事务边界（`@BizMutation` 自动包装事务）。
- Processor 是 Java 编排单元：跨域经 `I*Biz` 注入调用；`I*Biz` 不满足时用 `IDaoProvider`/`IOrmTemplate`/`@SqlLibMapper`（需注释说明原因）。
- 豁免项登记在 `processor-per-mutation-exemption-registry.md`。

### 跨域链当前承载方式（三类）

| 链型 | 载体 | 一致性 | 实例 |
|------|------|--------|------|
| 同步强一致链 | BizModel → `I*Biz` 直呼 | 同事务（跨域失败由 `REQUIRES_NEW` 隔离） | 采购入库审核 → `IErpInvStockMoveBiz` 生成入库移动单；销售出库 → 可用量校验+扣减 |
| 事件驱动链 | `NopSysEvent`/PostingEvent（post-commit 异步） | 最终一致（兜底扫描重试） | 单据审核 → 发布过账事件 → finance 生成凭证 |
| 审批链 | nop-wf（`.`xwf` 流程定义 + wf listener 回调标准 action） | 流程实例状态在 `NopWfInstance/StepInstance/Work` 表 | 付款/收款/资产处置/HR 薪酬 4 实体 WORKFLOW |

### 审批链已证明的集成范式（可直接复用）

`wf-integration-design.md` 三层桥接是「补充 wf 关联」形态的已验证先例：

```
xmeta wf:wfName → nop-wf 引擎驱动流程
wf 结束 → 回调标准 approve action（xbiz source）
xbiz source → inject() Processor → 业务联动
```

Processor 对 DIRECT/WORKFLOW 无感知——**流程引擎负责「何时/由谁/按什么顺序」，Processor 负责「做什么」**。跨域流程编排沿用同一分工。

## 必要性分析框架

### 判定矩阵

一条跨域业务链是否需要 nop-wf 关联，逐条检查以下信号（任一命中即建议 wf 关联）：

| # | 信号 | 说明 | 典型场景 |
|---|------|------|----------|
| N1 | 人工门控/审批点存在 | 链中有一个或多个环节需要人确认后才继续 | 委外加工收货验收、发票三单匹配争议、大额付款 |
| N2 | 多级/多角色分派 | 环节处理人按角色/金额/组织逐级分派、可委托/转办/会签 | 金额分级审批、异地审批 |
| N3 | 超时升级/逾期处理 | 环节停留超时需自动升级或提醒 | 供应商响应、付款期限管理 |
| N4 | 流程实例可追溯视线 | 需要看一张「整条链走到哪一步、谁在处理」的流程视图 | 订单全程跟踪、客户催单 |
| N5 | 跨服务/跨组织协调 | 各子域独立部署或异构团队维护，链需要松耦合协调 | 子域独立微服务场景 |
| N6 | 分支/汇聚/回退 | 链中存在条件分支、并行汇聚、驳回重走 | 质检不合格→返工/报废分支 |

**反之，以下情形保持既有实现，不引入流程引擎**：

- 同事务强一致链（如出库→扣库存）：Processor 直呼 `I*Biz` 已经正确，wf 反而引入跨步骤持久化与分布式事务问题。
- 确定性计算链（如 MRP/成本核算）：无人工环节、无分支语义，Processor/Scheduler 承载即可。
- 简单两态事件联动（如审核→过账）：事件驱动 + 兜底重试已闭环。

### 判定输出形态

分析阶段（roadmap E1.7）产出：
1. 全 18 域跨域链清单（按上表信号逐一标注命中/未命中 + 理由）——清单本体为待产出分析交付物（E3.7 前置）。
2. 命中链的期望流程切分点（哪些 BizMethod 作为流程节点）。
3. 未命中链的显式声明（避免将来重复论证）。

## 形态：补充 wf 关联（推荐）

### 原则

1. **流程 = 现有 BizMethod 的引用图**。wf 流程定义（`.xwf`）中，自动步骤/人工步骤的业务动作指向既有 BizMethod（同 JVM 内经 `I*Biz`/BizModel 注入调用）；不新建「流程专用业务方法」。
2. **BizMethod 不可变**。流程引擎为 BizMethod 提供对齐 cadence（输入参数 → Processor → 输出），BizMethod 侧零感知（与审批链 Processor 无感知同一原则）。
3. **流程状态不落入业务表**。流程实例/步骤/待办在 `NopWfInstance/StepInstance/Work` 表；业务实体只保留必要的流程引用（如 `flowInstanceId`，仅当需要业务侧回查流程时才加，参照 4 审批实体）。
4. **业务状态迁移仍走实体状态机 Bean**。wf 回调触发业务 action → 业务动作经 `ErpXxxStateMachine` 迁移（`entity-state-machine-bean.md`）；wf 不直写业务表。

### 接线模式（镜像审批链三层桥接）

```
.xwf 流程定义（步骤序列, 引用 BizMethod/动作名）
    ↓ 步骤执行/完成
wf listener / on-enter（回调业务 action）
    ↓
xbiz action source → inject() Processor（业务联动, 状态机守卫）
```

差异点：审批链回调的是标 `use-approval` 的标准 approve action；流程编排回调的是**任意既有 `@BizMutation`**（也可经 BizModel 暴露为 `I*Biz` 直调）。回调点建议放 wf 步骤级 action（引擎支持步骤级动作），避免为纯自动链包一层审批语义。

### 候选试点链（roadmap E3 门控落地）

从各域既有链中挑选符合「已有若干人工/分支环节、Browser E2E 链已存在」的链，首选建议：

1. **委外加工链**（销售/预测 → 委外订单 → 发料供应商 → 收货入库 → 委外发票 → 付款）：含验收人工点 + 收货入库人工点，`mfg-subcontract-chain` E2E 已存在。
2. **P2P 三单匹配争议路径**（订单-入库-发票匹配失败 → 人工裁决 → 继续）：N1 命中，`runP2pChain` E2E 已存在。
3. **资产处置链**（处置申请 → 审批 → 清理损益凭证）：审批已 wf，可扩展为「处置单 → 审批 → 财务清理」完整链。

试点成功标准：wf 实例可观测（`NopWfInstance` 视图/接口可查）、业务链行为零回归（既有 E2E 全绿）、BizMethod 零改动。

> **实现门控（2026-08-12 用户指示）**：本主题当前只保留分析+设计，**不进入编码状态**；试点落地归 `erp-enhancement-roadmap.md` E3.7（触发条件：真实的 nop-wf 跨域编排需求出现）。

## 形态：全局协调器（子域独立微服务时）

### 背景

若子域独立部署为微服务（每个 `module-*` 独立进程），跨域链无法再靠同 JVM `I*Biz` 直呼。用户澄清明确其为可行演进方向，需要全局协调器。

### 方案要点

1. **流程引擎承载于独立可部署模块**（流程域/协调器服务）：nop-wf 引擎实例 + 全局 `.xwf` 流程定义 + 流程实例状态（天然支持持久化与恢复）。
2. **步骤 action 经 RPC 调子域 BizMethod**：协调器不复制业务逻辑；步骤调子域暴露的 RPC 契约（`*-api` 模块的 `IErpXxxBiz` 远程形态；Nop 平台 GraphQL/RPC driver 承载）。BizMethod 本身不变——仍是「输入 → Processor → 输出」。
3. **每子域保留本地强一致链**：单域内同步动作仍本地事务（Processor + 本地 `I*Biz`）；跨域环节由协调器串联，契约 = 子域 BizMethod 签名 + 返回值（对齐 cadence）。
4. **一致性模型**：跨服务链默认最终一致——协调器调用子域 RPC 成功后推进 wf 步骤；失败重试/人工处理。强一致跨服务链（X/Open 风格）明确排除（与业财 `REQUIRES_NEW` 隔离范式一致，不引入分布式事务）。
5. **失败处理**：复用既有兜底思路——步骤失败记录（`NopWfAction`/错误日志）+ 重试/人工升级；与过账兜底扫描范式同构。

### 与 Medusa workflows-sdk 的能力边界（2026-08-13 源码核对）

Medusa workflows-sdk（`packages/core/orchestration` + `workflows-sdk`）是**声明式 Saga 分布式事务编排框架**（非 BPM 引擎）：`createWorkflow` + `createStep(invoke/compensate)` + `when/parallelize/transform` 原语 + `TransactionOrchestrator` Saga 状态机 + 分布式存储（幂等/持久化/恢复）——解决「跨服务自动步骤链的原子性与补偿」。

**对照结论**：

| 维度 | Medusa workflows-sdk | 本文形态（nop-wf 承载） |
|------|---------------------|--------------------------|
| 人工审批/任务分配/待办 | 无（异步步骤只等事件，无任务/参与者模型） | `NopWfWork` 待办 + 参与者分配 |
| 委托/转办/会签/超时升级 | 无 | 平台全部支持 |
| 单据状态机绑定 | 无（独立事务图） | `use-approval` + wf 回调 → 状态机 Bean |
| 长期运行/驳回重走 | 弱（Saga 语义假设成功或整体补偿） | 流程实例长期驻留 + 驳回重走 |
| 业务规则与编排分离 | 弱（步骤函数直接写业务逻辑） | BizMethod 不可变，wf 只编排 |
| 自动步骤链补偿 | **强**（invoke/compensate 双 handler + 持久化） | 补偿思想同源（红冲/反向动作 reverseApprove 等既有）；nop-wf 步骤失败处理 + 业务级反向动作承载 |

**结论**：Medusa 不能单独解决复杂 ERP 整体流程——它只覆盖「自动化步骤链 + 跨服务补偿」这一小块；人工面、状态机绑定、长期流程、驳回重走均须 nop-wf + 实体状态机 Bean 承载。其补偿思想与 nop 既有红冲/反向动作同源，仅作全局协调器形态下「自动步骤链」的技术参考；**不引入**「业务逻辑写进流程步骤」的 Medusa 风格（AP-2 禁止项，BizMethod 不可变原则保持）。

### 演进路径

- 当前单进程部署：形态 A（补充 wf 关联）先行，协调器形态只作设计预留。
- 触发条件（roadmap E3.7 门控）：出现真实的多子域独立部署/独立团队维护需求。

## 反模式自检表

| # | 反模式 | 正确做法 |
|---|--------|----------|
| AP-1 | 把同事务强一致链整体搬进 wf（引入跨步骤持久化/分布式事务） | 保持 Processor 直呼 `I*Biz`，wf 只关联含人工/分支/跨服务环节的链 |
| AP-2 | 用 nop-task/task.xml 把现有 BizMethod 任务化重写 | 流程配置用 nop-wf（用户澄清）；task 化不改变编排本质且与 BizMethod 对齐 cadence 冲突。**边界**：单动作内部拓扑可变编排仍可经 task.xml（`service-layer-orchestration.md`），本文作用域为跨域流程 |
| AP-3 | 为流程新建「流程专用业务方法」复制业务逻辑 | 流程节点引用现有 BizMethod；业务逻辑单一真相在 Processor |
| AP-4 | wf 直写业务表状态 | 流程状态归 `NopWfInstance/StepInstance/Work`，业务状态经实体状态机 Bean 迁移 |
| AP-5 | 未分析必要性直接给所有链配 wf | 先过判定矩阵（N1-N6），未命中链显式声明排除 |
| AP-6 | 协调器复制子域业务规则做校验 | 协调器只编排；校验/业务规则在子域 BizMethod 内（对齐 cadence 保证） |
| AP-7 | 流程串联用新增 ORM 字段存储中间状态 | 中间状态在 wf 步骤实例；业务实体仅必要时加 `flowInstanceId` 引用（参照 4 审批实体，ORM 变更已获授权，`erp-enhancement-roadmap.md` §8.1） |

## 与既有架构关系

- **`wf-integration-design.md`**：本文是其在审批之外的推广（同一「配置即切换/引擎管流程-Processor 管业务」原则）。
- **`approval-framework.md`**：审批模式选择不受本文影响；NONE/DIRECT/WORKFLOW 三模式保持。
- **`processor-extension-pattern.md`**：BizMethod 不可变约束直接继承 processor-per-mutation 契约；本文不新增 Processor 形态。
- **`entity-state-machine-bean.md`**：wf 回调触发的业务动作仍经状态机 Bean 迁移，wf 不绕过。
- **`service-layer-orchestration.md`**：task.xml（拓扑可变编排）使用边界仍以该文档为准——本文 AP-2「流程配置用 nop-wf 非 nop-task」指**跨域流程**的配置载体；单动作内部拓扑可变的编排需求仍可用 task.xml（不冲突，作用域不同）。
- **`flow-overview.md`**：用户方向澄清摘要已补记（§七 跨域流程编排方向澄清）；本文是其架构落位。

## 相关文档

- `docs/design/flow-overview.md` — 业务流总览 + 用户方向澄清记录
- `docs/architecture/wf-integration-design.md` — 审批流集成与三层桥接（可复用范式）
- `docs/architecture/approval-framework.md` — 审批模式策略
- `docs/architecture/processor-extension-pattern.md` — per-mutation Processor 契约
- `docs/architecture/entity-state-machine-bean.md` — 实体状态机 Bean 契约
- `docs/backlog/erp-enhancement-roadmap.md` — 整个 ERP 增强 roadmap（本主题归 E1 设计补充 + E3 实现门控）
- 平台文档：`../nop-entropy/docs-for-ai/03-modules/nop-wf.md`、`02-core-guides/workflow-configuration.md`、`03-runbooks/build-approval-flow.md`