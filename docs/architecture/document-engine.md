# 单据三轴状态与流转基线

## 定位

定义 nop-app-erp 业务单据的**三轴状态分离**这一跨领域技术基线，以及单据状态流转的真实实现模式。

> **设计演进说明**：本文件早期版本曾设想一套统一的 `DocumentEngine` 服务（含 `IDocumentAction`、`TransitionRegistry`、`DocumentHookRegistry`、声明式转换 DSL、`ApprovalCallbackListener`、`EventBus` 异步过账等）。该设想**未实现**——全量检索 nop-app-erp 与 nop-entropy 源码均不存在这些类。现状落地形态是「**三轴状态字段 + task.xml/Java Processor 编排 + nop-wf 审批 + PostingEvent 业财打通**」。本文件已据此重写，过时的服务化设想已删除；稳定且真实的概念（三轴分离、BPM 只管审批）保留如下。

## 核心洞察（保留自 ERP 调研）

| 洞察 | 说明 |
|------|------|
| ERP 流程 ≠ BPMN 流程引擎 | ERP 单据流转是短周期、确定性、可自动化的状态变迁，不引入 Activiti/Camunda |
| BPM 只管审批，不管业务流转 | 审批接 nop-wf，核心单据流转靠状态字段 + 编排驱动 |
| 三轴状态分离 | 业务/审批/财务状态正交独立 |
| 业财打通用事件 + 可插拔 Provider | 单据审核后发 PostingEvent，财务按 businessType 路由 Provider 生凭证 |

## 三轴状态分离（真实基线）

所有业务单据实体持三个正交的状态轴。字段集权威源为 `<domain>/model/*.orm.xml`（多模块实体已含 `docStatus`/`approveStatus`/`posted`/`postedStatus` 等列）。

| 状态轴 | 字段 | 语义 | 状态值 |
|--------|------|------|--------|
| 业务状态 | `docStatus` | 单据业务生命周期 | DRAFT / PREPARED / COMPLETED / CLOSED / CANCELLED / VOIDED / REVERSED |
| 审批状态 | `approveStatus` | 审批流程结果（四态，见 `approval-framework.md`） | UNSUBMITTED / SUBMITTED / APPROVED / REJECTED |
| 财务状态 | `postedStatus` / `posted` | 过账与收付款核销 | UNPOSTED/POSTED；UNPAID/PARTIAL_PAID/PAID |

### 三轴关系

```
业务轴：    DRAFT → PREPARED → COMPLETED → CLOSED
                     ↓            ↓           ↓
                  CANCELLED     VOIDED     REVERSED

审批轴（与业务轴正交）：UNSUBMITTED → SUBMITTED → APPROVED
                                            ↓
                                         REJECTED →（修改后重提交）

财务轴（与业务轴正交）：UNPOSTED → POSTED
                       UNPAID → PARTIAL_PAID → PAID
```

关键约束（由编排层 task 保证，非引擎内置）：
- 业务进入 COMPLETED 要求 `approveStatus = APPROVED`。
- 过账要求 `docStatus = COMPLETED`。
- 冲销要求 `postedStatus = POSTED`。

> 状态值字典与字段集归 `<domain>/model/*.orm.xml` 为权威源；approveStatus 四态语义归 `approval-framework.md`；具体单据的状态迁移图归 `docs/design/<domain>/state-machine.md`。

## 单据状态流转的真实实现

**不存在统一的 `DocumentEngine` 服务**。单据状态迁移由编排层直接执行：

- **task.xml 编排**（拓扑可变）：`approve`/`submit`/`cancel`/`reverse` 等多步动作绑定 `*.task.xml`，task 步骤内完成「校验 → 规则决策 → 状态迁移 → 业务联动」。范式见 `service-layer-orchestration.md` 与 `module-purchase/.../ErpPurReceive/approve.task.xml`。
- **Java Processor**（拓扑稳定）：步骤顺序被域设计裁定为不可配置的强约束流程，用 Facade + Processor 两层结构，见 `processor-extension-pattern.md`。

**状态字段是唯一持久化真相，编排层是状态迁移的唯一写者**——task/Processor 在一个 `@BizMutation` 事务内原子完成状态变更与业务副作用（库存写入、过账触发等）。

## 与审批流程的协作

审批由 **nop-wf** 承担，只处理「需要人工决策的审批环节」（多级审批、会签/或签、驳回、转办），不替代单据状态机。审批流与业务流的集成接线见 **`wf-integration-design.md`**：

- wf 审批通过/驳回经结束事件回调 BizModel 方法，由**业务处理 task** 执行 `approveStatus` 迁移与业务联动。
- **`approveStatus` 由业务 task 改写，不由 wf 引擎直接写业务表**（不使用 wf 的 `bizEntityStateProp` 自动回写）。

## 与过账的协作（业财打通）

业财打通由 **PostingEvent + 可插拔 Provider** 机制承担，详见 **`docs/design/finance/posting.md`**（三层模型、`IErpFinAcctDocProvider`/`ErpFinAcctDocRegistry`、`businessType` 路由、`posted` 幂等兜底、SYNC/ASYNC 时序）：

- 单据审核后由所在域的 `*PostingDispatcher` 组装 `PostingEvent(businessType)`，经 `*PostingExecutor`（独立新事务）调用财务过账引擎生成凭证 + 辅助账，并回写 `posted=true`。
- 库存写入与单据状态变更恒定同事务强一致（物理库存正确性硬约束）；凭证生成时序默认 SYNC，可按 `(billType, acctSchemaId)` 切 ASYNC。
- 本文件不重复过账机制细节，以 `finance/posting.md` 为权威。

## 各域单据状态机

各业务域单据的具体状态迁移图、特殊动作、状态值约束由应用层 owner doc 持有：

- `docs/design/purchase/state-machine.md`
- `docs/design/sales/state-machine.md`
- `docs/design/inventory/state-machine.md`
- `docs/design/finance/state-machine.md`
- `docs/design/manufacturing/state-machine.md`
- `docs/design/assets/state-machine.md`
- `docs/design/quality/state-machine.md`
- 其余域见 `docs/design/<domain>/state-machine.md`

## 相关文档

- `approval-framework.md` — 审批模式策略与 approveStatus 四态语义
- `wf-integration-design.md` — 审批流（nop-wf）与业务流（nop-task）集成设计
- `service-layer-orchestration.md` — task.xml + Java Processor 双轨编排
- `processor-extension-pattern.md` — 拓扑稳定流程的 Processor 扩展模式
- `docs/design/finance/posting.md` — 业财打通机制（过账）权威 owner doc
- `docs/design/<domain>/state-machine.md` — 各域单据状态机
- `module-boundaries.md` — 业务实体归属与模块边界
