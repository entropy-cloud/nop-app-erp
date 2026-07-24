# 服务层实现架构

## 定位

定义 nop-app-erp 后端服务方法的实现模式——何时用 task.xml 编排、何时用 Java 直接写、各步骤实现方式如何选择。

本文为 `system-baseline.md` 中"服务层编写规范"的展开，所有规则继承自基线约定。

## 理念：双轨编排 + I*Biz 逻辑

编排分两轨，按拓扑稳定性选择：

- **task.xml**（拓扑可变）= WHAT + WHEN（调什么、什么顺序），逻辑留 I*Biz；步骤可经 Delta 覆盖，不重发版
- **Java Processor**（拓扑稳定）= 强类型流程骨架 + protected 步骤；单步实现可派生 bean 覆盖（见 `processor-extension-pattern.md`）

两轨都只负责"编排"，不负责"逻辑"——业务逻辑保留在 `I*Biz` / BizModel 中。这是为了实现编排层的可 Delta 定制（不改逻辑改顺序）或 Java 层的派生覆盖（不改骨架改单步），同时保持业务逻辑的测试性和安全性。

## CRUD 方法

save、update、delete、get、findPage、findList 等标准 CRUD 方法使用 `CrudBizModel` 默认实现，**不写 task.xml，不写 Java，不写 xbiz**。CrudBizModel 已自动暴露为 GraphQL 服务。

## CRUD 方法

save、update、delete、get、findPage、findList 等标准 CRUD 方法使用 `CrudBizModel` 默认实现，**不写 task.xml，不写 Java Processor，不写 xbiz**。CrudBizModel 已自动暴露为 GraphQL 服务。

## 多步骤编排方法

approve、submit、cancel、batchCreate、sync、import、recalc 及任何含 ≥3 步编排逻辑的方法，拆分为 **Java Processor**（见 `processor-extension-pattern.md`），BizModel 的 `@BizMutation` 仅做参数解析 + 委托调用 Processor。

Processor 是通用方法分解器：复杂方法拆到 Processor 的 `protected` 步骤方法中，保持 BizModel 轻量。

> **task.xml 是进阶选项**：当流程拓扑需要按客户/行业定制时，将 Processor 步骤迁移到 task.xml（平台支持 xbiz action 从 `<source>` 改 `task:name` 一行迁移，Java 零改动）。初期统一用 Processor 降低认知负担。

### 硬规则：每 mutation 一个 Processor

**每个 `@BizMutation` 方法对应一个独立的 Processor 类**，不允许多个 mutation 共用同一个 Processor。命名规则：

```
<Entity><Method>Processor
```

例如：
- `ErpPurOrder__approve` → `ErpPurOrderApproveProcessor`
- `ErpPurOrder__cancel` → `ErpPurOrderCancelProcessor`
- `ErpPurOrder__settle` → `ErpPurOrderSettleProcessor`
- `ErpSalOrder__convertToOrder` → `ErpSalOrderConvertToOrderProcessor`

**例外**（可以留在 BizModel，无需 Processor）：
- 纯查询（`@BizQuery`）且 ≤2 步
- 单步状态翻转（`setStatus` + `updateEntity`），且无关跨域编排

**标准 CRUD 方法不在此规则范围内**（走 CrudBizModel 默认实现）。

此规则保证：
1. **机械定位**：知道 mutation 名就能推导出 Processor 类名，无需搜索
2. **关注点聚焦**：每个文件只有一个 public 方法，打开即知职责
3. **Delta 精确覆盖**：客户定制精确到单个 action，覆盖一个 Processor 不影响其他

### 判定标准

| 方法特征 | 推荐模式 |
|----------|----------|
| 单步简单逻辑（≤2 步） | BizModel Java 方法 或 xbiz script |
| 多步骤逻辑（≥3 步），流程拓扑稳定 | **每 mutation 一个 Java Processor** |
| 多步骤逻辑，且步骤顺序/组合需按项目定制 | **task.xml 编排** |
| 纯 CRUD | CrudBizModel 默认 |

### 步骤实现方式选择

| 步骤诉求 | 写法 | 理由 |
|----------|------|------|
| 调已有的 `I*Biz` 业务方法 | `<invoke bean="erpXxxBiz" method="xxx">` | `I*Biz` 已有方法签名、事务、安全；零额外 Java |
| 调 nop-rule 做决策 | `customType="rule:Execute"` | 命名空间属性自动映射到 `<rule:Execute>` 标签 |
| 数据准备、判断、结果整形 | `<step><source><c:script>` | 一行 XPL 搞定，无需 Java 类 |
| 跨多个 task 复用的通用能力（通知、日志、同步） | `<simple bean="sharedStepName">` | 注册为 `ITaskStep` bean，多 flow 共用 |
| 完整的子流程 | `<call-task>` | 复用另一个 task.xml 定义 |

### 不推荐的做法

**不推荐通过 `ITaskStep` 封装业务动作**。`I*Biz` 已经是一等业务服务，task 再包一层 `ITaskStep` 只增加无意义的 Bean 类，无额外价值。

## 映射约定

xbiz action 通过 `task:name` 或 `task:path` 绑定到 task flow。这是一个**编译期标签库转换**——xbiz 文件通过 `<x:post-extends><biz-gen:TaskFlowSupport xpl:lib="/nop/core/xlib/biz-gen.xlib"/></x:post-extends>` 引入转换器，编译时扫描 `task:name`/`task:path` 属性，加载 `TaskFlowModel`，自动生成 `<arg>`、`<return>` 和 `<source>`。

两种寻址方式：

| 属性 | 解析方式 | 示例 |
|------|----------|------|
| `task:name` | 按名称从 `/nop/task/` 下解析（带版本可选） | `task:name="ErpSalOrder/approveOrder"` |
| `task:path` | 直接加载 VFS 路径，优先级高于 `task:name`，两者互斥 | `task:path="/nop/task/ErpSalOrder/approveOrder.task.xml"` |

```xml
<!-- task:name——按约定解析 -->
<mutation name="approveOrder" task:name="ErpSalOrder/approveOrder"/>

<!-- task:path——直接定位 VFS 文件 -->
<mutation name="approveOrder" task:path="/nop/task/ErpSalOrder/approveOrder.task.xml"/>
```

task 文件在 VFS 中的位置：

| 寻址方式 | 文件路径 |
|----------|---------|
| `task:name="ErpSalOrder/approveOrder"` | `_vfs/nop/task/ErpSalOrder/approveOrder.task.xml`（无版本）或 `_vfs/nop/task/ErpSalOrder/approveOrder/v1.task.xml`（带版本） |
| `task:path="/nop/task/ErpSalOrder/approveOrder.task.xml"` | 直接对应 VFS 路径 |

VFS 根目录 `/nop/task/` 在 `task.register-model.xml` 中定义（`resolveInDir="/nop/task"`）。Delta 定制方在 `_delta/{deltaDir}` 下覆盖同样 VFS 路径的文件。

**注意**：
- 使用前需确保 xbiz 文件已通过 `<x:post-extends>` 引入了 `biz-gen:TaskFlowSupport` 标签库（`xpl:lib="/nop/core/xlib/biz-gen.xlib"`），否则 `task:name`/`task:path` 属性会被忽略。
- `task:path` 与 `task:name` 在同一 action 上互斥——同时定义会抛异常 `nop.err.biz.task-conflict-path-and-name`。
- 本项目已有 `_vfs/erp/pur/_task/ErpPurReceive/approve.task.xml` 是早期手写、未被 xbiz 引用的孤立文件，不符合上述 VFS 约定。接入 `use-approval` 时删除，新 task 文件应放在 `_vfs/nop/task/` 下。

## 与 nop-rule 集成

task.xml 中通过 `customType="rule:Execute"` 调用规则引擎，`rule:*` 前缀属性自动映射到 `<rule:Execute>` 标签（前缀被剥掉）。

```xml
<step name="checkApprovalRoute" customType="rule:Execute"
      rule:ruleName="sal-order-approval"
      rule:inputs="${{amount: order.totalAmount, level: order.customerLevel}}">
    <input name="order"/>
    <output name="route"/>
</step>
```

如需规则模型可被 Delta 替换，将 `rule:ruleModelPath` 指向 VFS 路径而非 `rule:ruleName`，使 delta 可通过覆写 model 文件更换规则逻辑。

## 作用域与返回值

`<source>` 中 XLang 脚本的返回值存放在父作用域的 `RESULT`（大写）变量中：

```xml
<step name="calc">
    <source><![CDATA[
        100;  // 返回值，父作用域中 RESULT = 100
    ]]></source>
</step>
```

通过 `<output>` 将 `RESULT` 或表达式提取到父作用域指定名称：

```xml
<step name="calc">
    <output name="discount" value="${RESULT}"/>
    <source><![CDATA[
        100;  // RESULT = 100, 同时父作用域 discount = 100
    ]]></source>
</step>
```

也可在 source 中直接用 `$scope` 写父作用域：

```xml
<step name="calc">
    <source><![CDATA[
        $scope.discount = 100;
    ]]></source>
</step>
```

## 验证模式

```xml
<step name="validate">
    <source><![CDATA[
        if (order.status != 'SUBMITTED')
            throw new NopScriptError("erp.purchase.order-not-submitted")
                .param("orderId", order.id);
    ]]></source>
</step>
```

`NopScriptError` 是 XLang 脚本中的异常类（继承 `NopEvalException → NopException`），构造参数为 errorCode 字符串，`.param()` 链式传递错误参数。

## 完整示例

参考 `module-purchase/erp-pur-service/src/main/resources/_vfs/erp/pur/_task/ErpPurReceive/approve.task.xml`（采购入库单审核：校验状态和数量 → 调规则判断超收 → 调库存域生成移动单 → 回写订单数量和状态）。

> 该文件是早期手写、未被 xbiz `task:name` 引用的孤立 task.xml，路径 `_task/` 不符合 Nop VFS 约定。新 task 文件应放在 `_vfs/nop/task/` 下。该文件保留作步骤结构参考。

## Delta 定制

定制方在 `_delta/{deltaDir}/` 下覆盖对应 `task.xml`，通过 `x:extends="super"` 继承基线，只覆写需要变动的步骤。例如替换超收规则，其他步骤不变：

```xml
<step name="checkOverReceive" x:override="replace" customType="rule:Execute">
    <input name="ruleName" value="'pur-receive-over-allowance-pharma'"/>
    <input name="inputs" value="${{receive: receive}}"/>
    <output name="allowed" value="${RESULT.allowed}"/>
</step>
```

## Java Processor 编排（默认模式）

≥3 步的编排方法默认拆入 Java Processor（`processor-extension-pattern.md` 的 Facade + Processor 两层结构）。Processor 通过 `protected` 步骤 + `IServiceContext` 末参 + 派生 bean 同名覆盖为单步实现留配置余地（产品化按客户/行业覆盖单步，不改流程骨架）。

**改用 task.xml 的条件**：仅当流程的步骤顺序可能需要在不同项目中定制（拓扑可变）时，从 Processor 迁移到 task.xml。

判定规则、Facade + Processor 两层职责、派生覆盖写法与反模式见 `processor-extension-pattern.md`。

## BizModel Java + Processor 委托（标准模式）

`@BizMutation` 方法在 BizModel Java 中直接调用对应的 per-mutation Processor：

```java
@BizMutation
public ErpPurOrder approve(@Name("id") String id, IServiceContext svcCtx) {
    return erpPurOrderApproveProcessor.approve(id, svcCtx);
}

@BizMutation
public ErpPurOrder cancel(@Name("id") String id, IServiceContext svcCtx) {
    return erpPurOrderCancelProcessor.cancel(id, svcCtx);
}
```

**不使用 xbiz `<source>` 委托**。xbiz 仅在 Delta 定制需要覆盖某个 mutation 的 Java 实现时使用。

此模式适用于任何 BizModel action → Processor 委托。每 mutation 一个 Processor 是强制架构纪律，见 `processor-extension-pattern.md`。

## 与 use-approval / approval-support.xbiz 的关系

**本项目不使用 `use-approval` ORM 标签和 `approval-support.xbiz` 平台资源**。

`use-approval` 是 codegen 条件标签，作用是：
1. 生成 `I*Biz extends IApprovableBiz`（5 个 default 方法占位）
2. 生成 `_*Biz.xbiz` 继承 `approval-support.xbiz`（5 个 XLang mutation source）

在 per-mutation Processor 模式下：
- 状态校验/转换/审计字段回写完全由 Processor Java 实现
- `IApprovableBiz` 的 5 个 default 方法全抛 `UnsupportedOperationException`，无运行时价值
- 可选 wf 启动由 `AbstractSubmitForApprovalProcessor` 按实体 xmeta `wf:wfName` 配置条件执行（见 `processor-extension-pattern.md`）

因此 ORM 模型中只保留 `useWorkflow="true"`（控制 `nopFlowId` 列），不继承 `approval-support.xbiz`。wf 回调 `bizObj.invoke('approve', ...)` 在没有 xbiz `<source>` 时自动 fallback 到 Java `@BizMutation`。

## 相关文档

- `system-baseline.md` — 技术基线与服务层编写规范（本文为其展开）
- `customization-capabilities.md` — 定制能力总览（Delta 定制原理）
- `../nop-entropy/docs-for-ai/03-modules/nop-task.md` — task flow 平台文档（含 customType 扩展机制）
- `../nop-entropy/docs-for-ai/03-modules/nop-rule.md` — 规则引擎平台文档
