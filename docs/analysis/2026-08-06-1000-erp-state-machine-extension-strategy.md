# ERP 实体级状态机 Bean 策略分析

> 日期：2026-08-06
>
> 问题：`nop-entropy` 将 job fire/task/schedule 的状态判断集中到静态 `*StateMachine` 类。本项目是否应为每种 ERP 业务状态机提供对应的 `ErpXxxStateMachine` Bean，通过注入使用，以集中迁移判断并支持未来客户定制？

> 结论：**应该。** 不要建立一个覆盖所有实体的全局泛型状态机；应为具备独立业务生命周期的实体或状态轴建立一个可注入、可 Delta 覆盖的 `ErpXxxStateMachine` Bean。它应成为迁移矩阵和纯状态语义的单一真相源。现有 per-mutation Processor 继续负责事务、实体写回、业务守卫、审计与跨域副作用，不能被 StateMachine Bean 替代。

## 1. 为什么比静态类更适合 ERP

`nop-job` 的 `JobFireStateMachine`、`JobTaskStateMachine` 和 `JobScheduleStateMachine` 都是无状态的静态工具：输入仅为状态值及显式参数，输出为状态分类、动作合法性或聚合结果；不依赖客户配置、组织、用户、持久化实体或副作用。Job 状态机不要求客户定制，因此 `final` 类和静态方法是正确选择。

ERP 的迁移矩阵常有行业差异。例如合同终止、发运完成、工单报工、审批前置条件可能因客户而改变。若仍用静态方法，客户必须修改基线代码或在调用点重新判断，都会破坏升级路径并重新造成规则分散。实体级 Bean 可以通过 Delta 的同名 Bean 覆盖替换基线迁移矩阵，保留产品化扩展能力。

现有项目也已有这条基础设施：`docs/architecture/processor-extension-pattern.md` 已规定 Processor 可通过派生 Bean 同名覆盖；Nop Bean 必须在非生成的 `app-service.beans.xml` 显式注册。实体级状态机 Bean 与这一机制一致。

## 2. StateMachine Bean 的职责边界

以 `ErpCsTicketStateMachine` 为例，它应集中如下稳定语义：

- `assign`：`NEW -> ASSIGNED`
- `start`：`ASSIGNED -> IN_PROGRESS`
- `resolve`：`IN_PROGRESS -> RESOLVED`
- `close`：`RESOLVED -> CLOSED`
- `reopen`：`RESOLVED -> IN_PROGRESS`
- `cancel`：非终态 `-> CANCELLED`
- `isTerminal(status)`：`CLOSED` 或 `CANCELLED`

它不应负责：

- 加载或更新 ORM 实体；
- 开启或控制事务；
- 调用 `I*Biz`、DAO、通知、库存或财务过账；
- 读当前用户、执行职责分离或数据权限；
- 写状态变更审计、时间字段或 ErrorCode 的领域参数。

这些仍由 Processor/BizModel 承担。例如客服工单的 `close` 还要校验 SLA 超时原因、写 `endDateTime`、写 `ErpCsTicketAction` 审计。状态机 Bean 只回答当前状态是否允许 `close`、以及正常目标态是什么。

## 3. 推荐结构

```text
BizModel @BizMutation
  -> <Entity><Action>Processor Bean
     -> <Entity>StateMachine.assertCan<Action>(currentStatus)
     -> 动态业务守卫、权限、职责分离
     -> 状态写回、审计字段、跨域副作用
     -> 持久化
```

建议让 StateMachine Bean 明确暴露实体动作，不让业务代码面对反射型通用接口：

```java
public class ErpLogShipmentStateMachine {
    public void assertCanAdvise(String status) { /* DRAFT only */ }

    public void assertCanComplete(String status) { /* ADVISED only */ }

    public String completionTargetStatus() { /* DISPATCHED */ }

    public boolean isTerminal(String status) { /* DELIVERED or CANCELLED */ }
}
```

这样调用点、状态图和定制点都是可读的。若后续确实需要一个状态机审计工具统一遍历，可让各 Bean 额外提供只读的 `List<TransitionDefinition>`。该元数据接口仅服务于测试、可达性检查和文档一致性校验，不要求 Processor 经泛型接口调用。

## 4. 三轴状态不能混成一张大表

本项目的单据通常有 `docStatus`、`approveStatus`、`posted` 三个正交轴，见 `docs/design/domain-design-guidelines.md` §16。实体级状态机应按轴拆分，不要建立三轴笛卡尔积大状态机：

- `ErpPurOrderApprovalStateMachine`：submit/approve/reject/reverse/withdraw 的审批轴迁移；
- `ErpPurOrderDocumentStateMachine`：业务生命周期和取消的 `docStatus` 迁移；
- `posted`：由业财过账契约、物理锁定和红冲闭环管理，不当作普通单据状态边。

这能避免状态组合爆炸，也与现有设计的三轴分离一致。

## 5. 与 Processor hook 和 task.xml 的分工

| 变化类型 | 默认手段 |
| --- | --- |
| 固定的来源状态、目标状态、终态分类 | 实体级 `ErpXxxStateMachine` Bean |
| 客户替换某实体的迁移矩阵 | Delta 同名 Bean 覆盖 `ErpXxxStateMachine` |
| 客户仅替换一个业务校验、计算或副作用 | 派生 per-mutation Processor，覆盖 `protected` hook |
| 同一可变决策被多个实体稳定复用、需多实现路由 | 窄 Policy/SPI + 注册中心 |
| 客户要增删步骤或改变步骤顺序 | `task.xml` + Delta |
| 工作流会签、加签、人工任务 | nop-wf；业务实体仅接收审批结果 |

StateMachine Bean 不应变成万能策略容器。迁移矩阵可替换，不意味着库存强一致、凭证回链、幂等、物理锁定或红冲闭环也可替换。特别是财务和库存保护区域必须继续遵守 owner doc 的稳定约束。

## 6. 对“迁移完备性分析”的收益

当前多个域的来源状态判断分散在 `validateTransitionForXxx`、BizModel 和 Processor 中。虽然可执行，但难以系统回答：某个状态是否可达、是否有死状态、终态是否仍有出边、所有 dict 值是否存在 writer。

将固定迁移矩阵集中到实体级 StateMachine Bean 后，可以新增表驱动测试：

1. 遍历每个动作的来源状态和目标状态，验证无重复/冲突边。
2. 从初始状态搜索可达性，验证每个声明状态和终态可达。
3. 对照 ORM dict、域 `state-machine.md`、StateMachine 元数据和 `setStatus` writer。
4. 保留现有业务动作测试，证明 Processor 写回、审计和副作用仍正确。
5. Delta 覆盖测试验证客户矩阵替换实际生效。

这正面解决项目已有的“dict 死状态”风险，而不是只做代码形式重构。

## 7. 落地约束

- 每个 Bean 对应一个实体的一条状态轴，命名采用 `Erp<Domain><Entity><Axis?>StateMachine`。
- Bean 只接收状态值、动作和必要的显式业务参数；不注入 DAO、`I*Biz` 或 `IServiceContext`。
- 迁移拒绝可返回 `false`/目标态，或抛由 Processor 提供/映射的领域 ErrorCode；不要把具体领域 ErrorCode 的参数拼装下沉为全局逻辑。
- Processor 的来源状态判断必须调用该 Bean，不能保留一份平行的允许状态集合。
- 同名 Delta 覆盖适用于一个客户替换基线矩阵；多个并行实现按类型路由时才使用 SPI 注册中心。
- Bean 定义写在非生成 `app-service.beans.xml`；`@Inject` 字段不能为 `private`。
- 现有状态机不要全量、大爆炸重构。先在非财务保护域中选择客服工单、维护请求或合同做试点，验证矩阵测试和 Delta 覆盖后再渐进迁移。

## 8. 最终裁决

“每种状态机一个对应的 `XXStateMachine` Bean”是适合本项目的方向，前提是将它严格限定为**实体级迁移矩阵和状态语义**。它应与 Processor 配合而非竞争：

- `ErpXxxStateMachine`：声明和验证固定状态边，可覆盖、可审计；
- `ErpXxx<Action>Processor`：执行业务动作及所有副作用；
- `task.xml`：处理客户可改变的流程拓扑。

这样既能像 `nop-job` 一样把状态判断集中起来，又能满足 ERP 的 Delta 定制、业务隔离和升级兼容要求。
