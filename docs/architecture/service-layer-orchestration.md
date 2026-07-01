# 服务层实现架构

## 定位

定义 nop-app-erp 后端服务方法的实现模式——何时用 task.xml 编排、何时用 Java 直接写、各步骤实现方式如何选择。

本文为 `system-baseline.md` 中"服务层编写规范"的展开，所有规则继承自基线约定。

## 理念：task.xml 编排 + I*Biz 逻辑

```
task.xml = WHAT + WHEN（调什么、什么顺序）
I*Biz    = HOW（怎么执行、事务、安全、校验）
```

task.xml 只负责"编排"，不负责"逻辑"——业务逻辑保留在 `I*Biz` / BizModel 中。这是为了实现编排层的可 Delta 定制（不改逻辑改顺序），同时保持业务逻辑的测试性和安全性。

## CRUD 方法

save、update、delete、get、findPage、findList 等标准 CRUD 方法使用 `CrudBizModel` 默认实现，**不写 task.xml，不写 Java，不写 xbiz**。CrudBizModel 已自动暴露为 GraphQL 服务。

## 多步骤编排方法

approve、submit、cancel、batchCreate、sync、import、recalc 及任何含 ≥2 步编排逻辑的方法，使用 `task.xml` + xbiz 绑定作为首选模式。

### 判定标准

| 方法特征 | 推荐模式 |
|----------|----------|
| 单步简单逻辑（一行校验 + 一行状态修改） | BizModel Java 方法 或 xbiz script |
| 存在步骤概念（校验→规则→分支→调服务→后处理），且步骤顺序/组合可能需要在不同项目中定制 | **task.xml 编排** |
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

xbiz action 通过 `task:name` 绑定到 task.xml：

```xml
<!-- xbiz -->
<mutation name="approveOrder" task:name="ErpSalOrder/approveOrder" task:version="1">
    <arg name="orderId" type="String" kind="FIELD"/>
    <return type="ApproveResult"/>
</mutation>
```

task 文件位置：`service/_task/{BizObj}/{method}.task.xml`

对应 Delta 路径：`_delta/{deltaDir}/service/_task/{BizObj}/{method}.task.xml`

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

## Delta 定制

定制方在 `_delta/{deltaDir}/` 下覆盖对应 `task.xml`，通过 `x:extends="super"` 继承基线，只覆写需要变动的步骤。例如替换超收规则，其他步骤不变：

```xml
<step name="checkOverReceive" x:override="replace" customType="rule:Execute">
    <input name="ruleName" value="'pur-receive-over-allowance-pharma'"/>
    <input name="inputs" value="${{receive: receive}}"/>
    <output name="allowed" value="${RESULT.allowed}"/>
</step>
```

## 相关文档

- `system-baseline.md` — 技术基线与服务层编写规范（本文为其展开）
- `customization-capabilities.md` — 定制能力总览（Delta 定制原理）
- `../nop-entropy/docs-for-ai/03-modules/nop-task.md` — task flow 平台文档（含 customType 扩展机制）
- `../nop-entropy/docs-for-ai/03-modules/nop-rule.md` — 规则引擎平台文档
