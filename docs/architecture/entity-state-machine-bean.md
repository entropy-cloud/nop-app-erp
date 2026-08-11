# 实体级 StateMachine Bean 契约（ErpXxxStateMachine）

> **定位**：本文是 nop-app-erp 中**实体级业务状态机 Bean** 的稳定技术架构契约。它固化颗粒度/命名、状态轴边界、无状态约束、方法形状、Bean 注册、Delta 覆盖、错误语义、与 Processor/task.xml/SPI 的分工边界、CRUD 写入边界裁定与测试策略分层。
>
> **平台机制权威**：IoC 注册、Delta 同名 Bean 覆盖等平台机制见 `../nop-entropy/docs-for-ai/02-core-guides/delta-customization.md`。本文只规定本项目的契约与决策理由，不重复平台细节。
>
> **声明-实证对齐**：本文按项目「声明-实证」规范标注。平台 IoC/Delta 机制 = 平台层实证可用；业务级 Bean Delta 覆盖运行时实证 = successor（见 §6）。

## 0. 为什么需要实体级 StateMachine Bean

ERP 的固定状态迁移判断当前散布在三处：BizModel 的 `validateTransitionForXxx` 守卫、per-mutation `<Entity><Action>Processor` 的内联 `Objects.equals(getXxxStatus(), CONSTANTS.YYY)`、以及 facade helper（见 `processor-extension-pattern.md §状态判断方法的复用约定`，`APPROVED` 比较在 22 个 Processor 累计 132 次）。这种分散无法系统回答：某状态是否可达、是否存在死状态、终态是否仍有出边、所有 dict 值是否有 writer。

`nop-entropy` 将 job fire/task/schedule 的状态判断集中在**静态 `final` 类**（`JobFireStateMachine` 等），它们是无状态、无客户化的纯工具。ERP 的迁移矩阵常有行业差异（合同终止、发运完成、工单报工、审批前置条件可能因客户而改变），故采用**可注入、可 Delta 覆盖的实体级 Bean**，而非静态类——客户可通过 Delta 同名 Bean 覆盖替换基线迁移矩阵，保留产品化扩展能力，不破坏升级路径。

## 1. 颗粒度与命名

**颗粒度**：一个 Bean 对应**一个实体的一条状态轴**。不建立覆盖全部实体的反射型/泛型全局 `IStateMachine` 运行时调度器。

**命名**：

```
Erp<Domain><Entity>[<Axis>]StateMachine
```

| 状态轴 | Bean 命名示例 |
|--------|-------------|
| 单轴（业务生命周期，`status`） | `ErpCsTicketStateMachine`（cs Ticket 的 `status`） |
| `docStatus` 轴 | `ErpPurOrderDocumentStateMachine`（采购订单业务生命周期轴） |
| `approveStatus` 轴 | `ErpPurOrderApprovalStateMachine`（采购订单审批轴） |

当实体只有一个业务状态轴且字段名即 `status`，可省略 `<Axis>`；当实体有 `docStatus` + `approveStatus` 双轴（见 §3），各自独立 Bean，命名带 `Document`/`Approval` 后缀以区分。

**与 nop-job 静态 `final` 类的取舍理由**：job 状态机输入仅为状态值与显式参数、不依赖客户配置、不要求客户定制，`final` 类与静态方法是正确选择。ERP 迁移矩阵有行业差异、需 Delta 可覆盖，故采用 Bean（非 `final`、非 static），让客户层经 Delta 同名覆盖替换基线矩阵。

## 2. 无状态约束

StateMachine Bean **严格无状态、无副作用**。它只接收**状态值与必要显式业务参数**，返回状态分类、动作合法性或目标态。

**Bean 不允许注入/依赖**：

- DAO、`IDaoProvider`、`IOrmTemplate`
- 其他实体的 `I*Biz` 接口
- `IServiceContext` / `IUserContext`（不读当前用户、不执行数据权限/职责分离）
- 通知、库存、财务过账等跨域副作用
- 事务、ORM Session

**Bean 不负责**：加载或更新 ORM 实体；开启或控制事务；写状态变更审计、时间字段；组装领域 ErrorCode 的实体编号/上下文参数。这些归 Processor/BizModel（见 `processor-extension-pattern.md`）。

> 理由：无状态让 Bean 可被矩阵测试表驱动地完整遍历、可达性/完备性分析可机器化，且 Delta 覆盖替换矩阵时不引入隐藏的持久化/事务耦合。Bean 是「纯函数集合」式的状态语义载体。

## 3. 状态轴边界（三轴分离）

本项目业务单据头使用**三轴状态**（约束来源 `docs/design/domain-design-guidelines.md §16.1`）：

- **`docStatus`（业务生命周期）**：表达单据在业务流程中的位置，独立于审批。
- **`approveStatus`（审批状态）**：表达审批流结果，可与 `docStatus` 解耦组合。
- **`posted`（业财一体标志，boolean）**：业财过账/红冲/物理锁定契约。

**StateMachine Bean 边界裁定**：

- `docStatus` 与 `approveStatus` **各自独立 Bean（或独立轴）**——不合并为笛卡尔积大状态机（组合爆炸 + 与三轴分离设计冲突）。
- **`posted` 不作为 StateMachine 迁移轴**。`posted=true` 物理锁定为只读、纠错走红冲/反向单（`domain-design-guidelines.md §14.2.1`）；它是业财过账/红冲/物理锁定契约，不是普通状态边。迁移项只迁移「触发/逆转过账的业务状态轴」（如 `docStatus`/`approveStatus`），`posted` 本身的翻转继续由过账编排与红冲闭环管理。

> 本节引用 `domain-design-guidelines.md §16.1` 为三轴分离的约束来源，不重复其正文。本文不改动三轴设计。

## 4. 方法形状

主路径为**显式动作方法**（不让业务代码面对反射型通用接口）；**额外**提供只读元数据接口，仅服务于完备性/可达性分析与文档一致性校验。

### 4.1 显式动作方法（主路径）

```java
public class ErpCsTicketStateMachine {
    // 断言当前态是否允许该动作；非法边 → 抛通用非法迁移异常（见 §7）
    public void assertCanResolve(String status) { /* IN_PROGRESS only */ }
    public void assertCanClose(String status)   { /* RESOLVED only */ }
    public void assertCanReopen(String status)  { /* RESOLVED only */ }
    public void assertCanCancel(String status)  { /* 非终态 */ }

    // 动作的目标态（供 Processor 写回）
    public String resolveTargetStatus() { return "IN_PROGRESS"; } // → RESOLVED
    public String closeTargetStatus()   { return "CLOSED"; }

    // 终态分类
    public boolean isTerminal(String status) { return "CLOSED".equals(status) || "CANCELLED".equals(status); }
}
```

**调用点（Processor 内）**：

```java
// <Entity><Action>Processor
public Result resolve(String id, IServiceContext context) {
    ErpCsTicket entity = requireEntity(id);
    stateMachine.assertCanResolve(entity.getStatus());   // 矩阵守卫（非法边在此抛出）
    // ...动态业务守卫、权限、SLA、审计字段、副作用...
    entity.setStatus(stateMachine.resolveTargetStatus());
    // ...持久化...
}
```

### 4.2 只读元数据接口（完备性/可达性分析用，非主调用路径）

```java
public interface TransitionDefinition {
    String getAction();     // 动作名，如 "resolve"
    String getFromStatus(); // 来源态
    String getToStatus();   // 目标态
}

// 每个 StateMachine Bean 额外暴露只读列表，供 M5.1/M5.2 可达性/完备性检查消费
public List<TransitionDefinition> transitions() { /* 返回不可变快照 */ }
public List<String> terminalStatuses()          { /* 终态集合 */ }
public List<String> initialStatuses()           { /* 初始态集合 */ }
```

**「显式方法优先于反射泛型」的理由**：调用点、状态图与定制点都保持可读——Processor 调 `stateMachine.assertCanResolve(status)` 比经泛型 `stateMachine.assertCan("resolve", status)` 更强类型、IDE 可定位、Delta 覆盖精确到方法。只读元数据接口**不要求 Processor 经泛型接口调用**，它仅供测试/守卫/文档工具统一遍历，是否构建统一遍历工具由 M5.2 按实际误报裁决决定。

## 5. Bean ID 与注册

**注册范式**：在非生成 `app-service.beans.xml` 注册，沿用本项目既有 `<bean id="<FQN>" class="<FQN>"/>` 范式（证据：`module-cs/erp-cs-service/src/main/resources/_vfs/erp/cs/beans/app-service.beans.xml` 的 per-mutation Processor 注册）。

```xml
<!-- _vfs/erp/{domain}/beans/app-service.beans.xml -->
<bean id="app.erp.cs.service.statemachine.ErpCsTicketStateMachine"
      class="app.erp.cs.service.statemachine.ErpCsTicketStateMachine"/>
```

**注入方式**：Processor 内**按类型注入**（`@Inject ErpCsTicketStateMachine stateMachine`）。一个 Processor 只注入它处理的那条轴的 Bean。

**`@Inject` 字段不得 `private`**（Nop IoC 规则 + 合规检查器 R5）：使用包级或 `protected` 可见。

**「按类型注入 vs 按 id 注入」取舍**：按类型注入让 Processor 不耦合 bean id 字符串；同名 Delta 覆盖（§6）经 bean id 替换实例、类型不变，Processor 注入点零改动即生效。按 id 注入会在覆盖时引入 id 拼装耦合，故不采用。

## 6. Delta 同名覆盖

**覆盖路径**：客户层经 Delta `_vfs/_delta/{deltaDir}/erp/{domain}/beans/app-service.beans.xml` 以**同名 bean id** 注册派生/替换 Bean 覆盖基线（平台机制权威：`../nop-entropy/docs-for-ai/02-core-guides/delta-customization.md`）。

```xml
<!-- Delta: _vfs/_delta/{deltaDir}/erp/cs/beans/app-service.beans.xml -->
<bean id="app.erp.cs.service.statemachine.ErpCsTicketStateMachine"
      class="com.customer.cs.statemachine.CustomerErpCsTicketStateMachine"/>
```

派生类继承基线 Bean，重载目标动作方法（如放开/收紧某条边），基线其余动作不变，升级时自动合并。覆盖单元是**单条迁移边或单个动作方法**，不是整个状态机语义。

> **声明-实证对齐（必须）**：平台 Delta 同名 Bean 覆盖机制经平台层实证可用（`customization-capabilities.md` 记录 2 个平台层 nop-auth view delta 实证 `x:extends="super"` 差量合并工作）。**业务级 Bean Delta 覆盖运行时实证 = successor（= 迁移路线图 M1.2「客服试点 Delta 同名 Bean 覆盖证明」）**：在真实应用容器中证明基线/Delta 两种加载结果（替换生效）是 M1.2 的硬交付。本契约**只声明平台机制可用与覆盖路径，不越权声称业务级覆盖已验证**；在 M1.2 成功证明前，任何迁移项不得声称业务级 Delta StateMachine 覆盖已验证。

## 7. 错误/拒绝语义

**职责分工**：

- **StateMachine Bean 报告非法边**：`assertCan<Action>(status)` 遇到非法来源态时，抛一个**通用的非法迁移异常**（common 层错误码，如 `illegal-status-transition`），异常携带**拒绝元数据形状**：来源态、目标态（或期望动作名）。Bean 不组装领域 ErrorCode 的实体编号/上下文参数。
- **Processor 保留领域 ErrorCode、实体编号与上下文参数**：Processor 捕获/感知非法边后，映射为**本域的领域 ErrorCode**（如 `ErpCsErrors.ERR_TICKET_INVALID_TRANSITION`），填入实体编号（`ticketCode`）、当前态、操作人等业务上下文，对外抛出 `NopException`。

**为什么不让 common 层错误码抹平领域语义**：领域 ErrorCode 的描述、i18n、错误码值、实体参数组装是各域对外契约的一部分（见 AGENTS.md「异常处理」）。若 Bean 直接抛领域 ErrorCode，会把「实体编号/上下文」这类需要持久化实体的参数下沉到无状态 Bean，破坏 §2 无状态约束；且不同域对同类「非法迁移」有不同错误码与 i18n。故 Bean 只报告「这是非法边 + 拒绝元数据」，领域映射归 Processor。

**拒绝元数据形状**（供 M5.2 守卫消费）：

```java
// 通用非法迁移异常携带的拒绝元数据
class IllegalStatusTransitionException extends NopException {
    String getAction();      // 动作名（如 "resolve"）
    String getFromStatus();  // 实际来源态
    // getExpectedFromStatuses() 可选：该动作允许的来源态集合（用于诊断信息）
}
```

## 8. 分工边界表

| 变化类型 | 默认手段 | StateMachine Bean 的角色 |
|----------|----------|--------------------------|
| 固定的来源状态、目标状态、终态分类 | **实体级 `ErpXxxStateMachine` Bean** | 承载者（迁移矩阵 + 状态分类 + 只读元数据） |
| 客户替换某实体的迁移矩阵 | **Delta 同名 Bean 覆盖** `ErpXxxStateMachine` | Delta 覆盖定制点（业务级实证 = successor） |
| 客户仅替换一个业务校验、计算或副作用 | **派生 per-mutation Processor，覆盖 `protected` hook** | 无（Bean 不持有校验/副作用） |
| 同一可变决策被多个实体稳定复用、需多实现路由 | **窄 Policy/SPI + 注册中心** | 无（避免 Bean 退化为万能策略容器） |
| 客户要增删步骤或改变步骤顺序 | **task.xml + Delta** | 无（拓扑可变不归 Bean） |
| 工作流会签、加签、人工任务 | **nop-wf**；业务实体仅接收审批结果 | 无（人机任务状态归 nop-wf） |
| 业财过账、红冲、物理锁定 | **过账编排 + `posted` 契约** | 无（`posted` 不作迁移轴，见 §3） |

**不适用场景登记**（StateMachine Bean 不解决的问题）：

- 库存强一致、凭证回链、幂等、物理锁定、红冲闭环——这些保护区域约束继续由各自 owner doc 承载，Bean 不替代。
- 普通无独立业务生命周期的 `ACTIVE/INACTIVE` 标志、技术处理状态、notify 子系统中不含业务迁移矩阵的记录——不纳入 Bean 范围。
- 跨域副作用的执行（如审批通过触发过账）——仍经 Processor/`I*Biz` 路径；Bean 只回答「该审批动作在此态是否合法」。

## 9. CRUD 写入边界裁定

### 9.1 探索证据

扫描当前对业务状态字段的**全部写路径**，区分三类 writer：

1. **生产命名动作**（BizModel/Processor）：`@BizMutation` 经 Processor 写迁移目标态（如 `entity.setStatus(stateMachine.resolveTargetStatus())`）。这是矩阵治理的目标路径。
2. **框架入口**（标准 CRUD `__save`/`save`/`update`）：经 `CrudBizModel` 的标准保存路径。**探索结论：当前通用 CRUD/API 能直接写入业务状态字段**——生成 xmeta 中 `status`/`docStatus`/`approveStatus` 均为 `insertable="true" updatable="true"`（抽样 `ErpCsTicket` `status`/`docStatus`/`approveStatus`、`ErpPurOrder`/`ErpPurPayment`/`ErpPurQuotation` `docStatus` 等一致）；全仓 `notUpload`（排除写上传/保存的字段标记）使用计数 = **0**，即**没有任何状态字段被 xmeta 写保护**。
3. **测试 fixture**：测试种子数据直接 `setStatus(...)` 构造初始/任意态（见既有 8 个 `TestErp*StateMachine` 集成测试的 `seedXxx` helper）。

**既有 `__save` 写状态的既有行为**：BizModel 的创建/保存路径在创建时写**初始态**（`DRAFT`/`UNSUBMITTED`/`PENDING` 等，见 contract/inventory/aps 等域 `setDocStatus(DRAFT)`/`setApproveStatus(UNSUBMITTED)` 调用）。这是合法的初始态写入，**不是**非法迁移，但说明「框架入口当前确实写状态字段」。

### 9.2 裁定：选项 (c) 显式排除

在三选项中裁定为 **(c) 显式排除**：

> **声明**：通用 CRUD/API 当前可写业务状态字段（无全局写锁）；`ErpXxxStateMachine` Bean 是**命名业务动作迁移矩阵的唯一权威来源**。运行时唯一性是「**命名动作路径**」声明——即：当状态迁移经命名业务动作（Processor）发生时，Bean 是允许边的唯一判定者；通用 CRUD 的状态字段写入不在矩阵的运行时强制范围内。

### 9.3 考虑的替代方案（被否）

- **(a) 禁止——通用 CRUD/API 不得写业务状态字段（经 xmeta `notUpload`/`updatable="false"` 或 xbiz save-guard 强制）**：**否决**。理由：(i) 需修改全部 19 个生产 ORM/xmeta 模型，触及 ORM/xmeta 保护区域（本项目 AI 阻塞条件 + 路线图 Non-Goal「不修改 model/*.orm.xml」），M0.1 不具备该自主权；(ii) 会破坏既有创建时写初始态的 `__save` 行为（需逐一改造每个 save 路径）；(iii) xbiz save-guard 对每个实体逐一编写成本高且引入新维护面。
- **(b) 限制——仅允许创建时写初始态，所有迁移归命名动作**：**部分采纳但不在 M0.1 强制落地**。理由：当前创建时写初始态本就是现实（§9.1 第 2 类）；但「在框架层强制只允许初始态写入」仍需 xmeta 改变 `updatable`（同 (a) 的保护区域约束）。各迁移项在落地时，可在 Processor 内显式断言「非创建路径写状态须走命名动作」，作为应用层约定（不依赖框架写锁）。

### 9.4 残留风险与对「唯一矩阵」宣称强度的影响

- **残留风险**：无全局写锁时，直接 CRUD `save`/`update` 可绕过矩阵写状态字段（如客户端直接发 `ErpCsTicket__save` 带 `status=CLOSED`）。这意味着 StateMachine Bean 的「唯一矩阵」宣称是**作用域受限**的。
- **对后续迁移项宣称强度的影响（必须遵守）**：迁移项（M1.1 及以后）只能宣称「**命名业务动作的状态迁移经 StateMachine Bean 唯一治理**」，**不得**宣称「运行时无任何其他路径可写状态字段」。更强的全局写锁保证是 **successor**：若需全局强制，须开独立计划修改 ORM/xmeta（触及保护区域 → ask-first），并补各 `__save` 路径的初始态豁免，不可在本路线图重构名义下静默实施。
- **M0.2/M5 的完备性分析前提**：四方对照（dict ↔ owner-doc 迁移图 ↔ StateMachine 元数据 ↔ 生产 `setStatus` writer）必须把「通用 CRUD 是否能写该字段」纳入 writer 盘点，以避免遗漏绕过矩阵的路径。

## 10. 测试策略分层

每个迁移项与 M5.2 共用三层测试义务：

### 层 1 — 矩阵完备性（新增，greenfield）

针对 StateMachine Bean 的表驱动测试：

- 遍历每个动作的来源态/目标态，验证无重复/冲突边；
- 从初始态搜索可达性，验证每个声明状态与终态可达；
- 终态无出边；
- `transitions()` 元数据与显式方法语义一致。

### 层 2 — dict ↔ owner-doc ↔ 元数据 ↔ writer 四方对照（新增，直接服务 dict 死状态检测）

对照 ORM dict、域 `state-machine.md`、StateMachine 元数据与生产 `setStatus` writer，检测 dict 死状态、owner-doc 迁移图与生产 writer 漂移。writer 盘点必须包含通用 CRUD 路径（§9.4）。

### 层 3 — Delta 覆盖 + 既有命名动作回归

- **Delta 覆盖**：证明同名 Bean 覆盖替换生效（业务级实证，归 successor 里程碑）。
- **既有命名动作回归**：证明 Processor 写回、审计字段、副作用不变。

**既有基线登记（必须，避免误作 greenfield 重建）**：层 3 的「既有命名动作回归」**并非 greenfield**。仓库已存在 **8 个 `TestErp*StateMachine` 集成测试**（finance 3：`Period`/`NotesPayable`/`NotesReceivable`；maintenance `VisitRequest`；manufacturing `WorkOrder`；master-data `SupplierApproval`；quality `Inspection`/`Recall`）。它们经 BizModel 入口（`IGraphQLEngine`）断言状态迁移、终态不可恢复、`posted` 翻转与跨域副作用，**构成层 3 的既有基线**。新增的矩阵测试属**层 1**；二者关系：既有集成测试 = 层 3 回归，新增表驱动测试 = 层 1 矩阵。执行者不得将层 3 当空白重建，也不得用层 3 的集成测试冒充层 1 的矩阵完备性。

---

## 与其他文档的关系

| 文档 | 关系 |
|------|------|
| `processor-extension-pattern.md` | Bean 嵌入 Processor 编排点（§"状态判断方法的复用约定"的 successor：内联 `Objects.equals` 判断改调 Bean） |
| `customization-capabilities.md` | Delta 同名 Bean 覆盖定制点（业务级实证 = successor，按其声明-实证规范标注） |
| `domain-design-guidelines.md §16` | 三轴分离约束来源（本文对齐之，不改动） |
| `../nop-entropy/docs-for-ai/02-core-guides/delta-customization.md` | Delta 机制平台权威 |
