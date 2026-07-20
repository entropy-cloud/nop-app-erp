---
name: nop-backend-dev
description: Nop平台后端服务开发（BizModel / IBiz / xbiz action / Processor / task.xml / ErrorCode）。涵盖决策门、xbiz动作声明、实体服务创建、自定义动作、多步编排、Processor/task.xml选择、protected step方法、跨实体调用、错误处理、事务边界、产品化可定制性自检。触发词：后端开发、BizModel、IBiz、xbiz、写方法、加接口、错误码、跨实体、Processor、task、编排。
---

# Nop 后端服务开发


> **项目定制化层（nop-app-erp）**：使用本技能前必须先读 `AGENTS.md` 与 `docs/skills/README.md §项目定制化层（nop-app-erp）`，将本仓库的保护区域（`module-<domain>/model/*.orm.xml` ask-first、会计/财务/数据删除）、验证命令（`mvn clean install -DskipTests`）、命名约定（`Erp<Domain>` 实体前缀、`erp-<short>/<dict>` 字典、`erp.err.<short>` ErrorCode 前缀）和已知失败模式注入上下文。


## 什么时候用我

| 场景 | 触发关键词 |
|------|-----------|
| 创建/修改 BizModel 方法 | "写方法"、"加接口"、"BizModel"、"加动作" |
| 定义 IBiz 接口 | "接口声明"、"IBiz"、"加方法声明" |
| 跨实体调用 | "调用其他实体"、"注入IBiz"、"跨模块" |
| 错误处理 | "错误码"、"ErrorCode"、"NopException"、"抛异常" |
| 事务/并发 | "事务边界"、"乐观锁"、"并发控制" |
| Delta 定制后端 | "覆盖平台BizModel"、"Delta定制" |

---

## 必读文档路径

以下路径相对于 `{DOCS-FOR-AI}` 目录。`{DOCS-FOR-AI}` 的实际位置由当前项目 `AGENTS.md` 的 "Nop Platform Documentation" 部分指定（例如 `../nop-entropy/docs-for-ai/`）。

### 全局必读（写任何后端代码前全部读完）

| 文档 | 为什么必读 | 不读会怎样 |
|------|-----------|-----------|
| `05-examples/README.md` + `05-examples/ibiz-and-bizmodel.java` | Entity/IBiz/BizModel/DTO/ErrorCode 精简代码骨架 | 不知道各类文件怎么写 |
| `02-core-guides/service-layer.md` | IBiz接口契约、注解规则、开发顺序、跨实体访问、safe API、反模式表 | 接口方法漏注解→代理无法路由；@Inject写private；用dao()而非requireEntity |
| `02-core-guides/error-handling.md` | NopException + ErrorCode 规则 | throws RuntimeException；错误消息用中文；丢失异常链 |
| `04-reference/safe-api-reference.md` | requireEntity/doFindList/saveEntity/newEntity 速查 | 绕过CrudBizModel管道直接操作dao() |

### 按场景选读

| 场景 | 文档 |
|------|------|
| 新建实体+代码生成 | `03-runbooks/create-new-entity.md`, `02-core-guides/model-first-development.md` |
| 写BizModel方法（含强制顺序） | `03-runbooks/write-bizmodel-method.md` |
| 扩展CRUD钩子 | `03-runbooks/extend-crud-with-hooks.md` |
| **多步编排流程** | **`03-runbooks/implement-complex-business-flow.md`** — Processor模式、task flow决策矩阵、protected step方法、产品化拓扑可变 |
| 何时拆Processor | `03-runbooks/choose-entity-bizmodel-processor.md` |
| 自定义QueryBean查询 | `03-runbooks/custom-query-with-querybean.md` |
| 跨模块IBiz接口 | `03-runbooks/add-cross-module-biz-interface.md` |
| Request/Response DTO | `03-runbooks/create-request-response-dto.md` |
| Delta定制原理 | `02-core-guides/delta-customization.md` |
| **平台可扩展设计（选读）** | **`06-extensibility/`** — 当你需要理解"Nop为什么这样设计"、做平台级扩展、或判断复杂需求的落层时阅读。常规业务开发不需要 |
| 认证与权限 | `02-core-guides/auth-and-permissions.md` |
| 事务与并发 | `02-core-guides/concurrency-and-transactions.md`, `03-runbooks/transaction-boundaries.md` |
| 错误码runbook | `03-runbooks/error-codes-and-nop-exception.md` |
| 功能实现总流程 | `03-runbooks/feature-implementation-checklist.md` |
| **task.xml 编排** | **`03-modules/nop-task.md`** — task flow模型、step类型、控制结构、xbiz绑定（`task:name`）、与Processor的边界 |

---

## 开发流程

### 强制实现顺序

1. **先 IBiz 接口** → 在 `I*Biz` 接口上声明方法（含 `@BizQuery`/`@BizMutation`/`@BizAction` + `@Name` + `IServiceContext context` 最后一个参数）
2. **再 BizModel 实现** → 实现类中 `@Override` 实现（见下方"实现方式选择"）
3. **自检** → 每写完/修改一个方法后，立即执行自检（见下方"自检纪律"）
4. **测试** → 用 IGraphQLEngine 测试（见 nop-testing skill）

### xbiz 的定位

**xbiz 是增量配置层，不是必须的声明清单。** xbiz 动态覆盖在 Java BizModel 之上：

- Java 已实现的 `@BizMutation` 方法，xbiz 不需要重复声明
- xbiz 只在需要**覆盖/增强**时使用：改 auth 配置、改校验逻辑、绑定 task.xml、用脚本替换 Java 实现
- 当前项目 `*.xbiz` 中 `<actions/>` 为空是**正确状态**——表示该实体所有方法走 Java 实现，不需要 xbiz 覆盖

### xbiz 增量配置场景

| 需求 | xbiz 做什么 | Java 做什么 |
|------|-----------|-----------|
| 不改任何默认行为 | **不写 xbiz**（`<actions/>` 为空即可） | 完整 Java 实现 |
| 只改 auth 权限 | 声明 `<mutation>` + `<auth roles="..." permissions="BizObjName:action"/>` | Java 实现不变 |
| 用 task.xml 替换 Java 实现 | 声明 `<mutation task:name="..." task:version="..."/>` | Java 只占位（空方法体） |
| 用脚本替换 Java 实现 | 声明 `<mutation>` + `<source>` 脚本 | Java 只占位 |
| 多步编排需要步骤级覆盖 | xbiz 绑定 task.xml；task.xml 中每步可被 Delta 覆盖 | Java 不写或只占位 |

> **核心规则：xbiz 有 `<source>` → 执行脚本（覆盖 Java）；xbiz 有 `task:name` → 执行 task.xml（覆盖 Java）；xbiz 只有 `<auth>` 子元素 → 只覆盖权限配置，逻辑仍走 Java；xbiz 不声明 → 全部走 Java 默认。**

### 实现方式选择

| 方法特征 | 实现方式 | 说明 |
|---------|---------|------|
| 纯 CRUD | CrudBizModel 默认 | 不写任何代码 |
| 单步操作 | BizModel `@BizMutation`/`@BizQuery` | Java 完整实现；xbiz 不写 |
| 多步编排，拓扑稳定 | Processor 模式 | Facade + Processor（protected step 方法）；xbiz 不写 |
| **多步编排，拓扑可变** | **task.xml 编排** | **Java 只占位；xbiz 绑定 `task:name`；真正实现在 task.xml** |
| 需要脚本替换 | xbiz `<source>` 脚本 | xbiz 写脚本；Java 只占位 |

> **不要为单步操作强行拆 Processor，也不要为拓扑稳定的流程强行用 task.xml。**

### xbiz 增量配置参考

**xbiz 不需要为每个方法声明 action。** 只在需要覆盖 Java 行为时才写。

```xml
<!-- {EntityName}.xbiz（保留层，x:extends 生成的 _*.xbiz） -->
<biz x:schema="/nop/schema/biz/xbiz.xdef" xmlns:x="/nop/schema/xdsl.xdef"
     x:extends="_{EntityName}.xbiz">
    <actions>

        <!-- 场景1：覆盖权限，不改逻辑 -->
        <mutation name="approve">
            <auth roles="manager" permissions="ErpPurOrder:approve"/>
        </mutation>

        <!-- 场景2：task.xml 覆盖 Java 实现 -->
        <mutation name="approveReceive" task:name="approve-receive" task:version="1">
            <auth roles="user" permissions="ErpPurReceive:approve"/>
        </mutation>

        <!-- 场景3：脚本覆盖 Java 实现 -->
        <mutation name="validateStock">
            <source><![CDATA[
                const invBiz = inject('biz_ErpInvStockBalance');
                // ...
            ]]></source>
        </mutation>

    </actions>
</biz>
```

| xbiz 写了什么 | 效果 |
|-------------|------|
| **不写这个 action** | 全部走 Java（默认，最常见） |
| 写 `<mutation>` + `<auth roles="..." permissions="..."/>` | 权限配置覆盖 Java，逻辑仍走 Java |
| 写 `<mutation task:name="..." task:version="..."/>` | task.xml 替换 Java 实现 |
| 写 `<mutation>` + `<source>` 脚本 | 脚本替换 Java 实现 |

> **xbiz 是覆盖层，不是声明层。** `<actions/>` 为空是正确的——表示全部走 Java 默认。只有需要定制时才添加增量配置。

### 自检纪律（强制）

**每增加或修改一个 public 方法后，必须立即执行两层自检：**

**第一层：技术正确性（平台通用自检）**

执行 `{DOCS-FOR-AI}/04-reference/bizmodel-method-selfcheck.md` 的全部检查项。

**第二层：产品化可定制性（本 skill 补充自检）**

| # | 检查项 | 适用范围 | 不通过的后果 |
|---|--------|---------|------------|
| P1 | **多步编排**时：是否遵循 Processor 模式（protected step 方法）或 task.xml？ | 多步方法 | 下游要改行为必须重写整个方法 |
| P2 | 方法签名是否 `IServiceContext context` 为最后一个参数？ | **所有方法** | 跨域调用丢失用户身份和数据权限 |
| P3 | task.xml 实现时：xbiz 是否用 `task:name`/`task:version` 正确绑定？ | task.xml 场景 | task 无法从前端 GraphQL 调用 |
| P4 | xbiz 脚本实现时：Java 是否只占位（不重复实现）？ | xbiz `<source>` 场景 | 两边都写会混淆优先级 |
| E1 | 当前实体的 Processor 中是否存在可直接上提到实体的稳定状态判断方法（`isAlreadyApproved`/`isAlreadyRejected` 等）？ | 涉及审批/状态流转的方法 | 状态判断逻辑散落在各 Processor，无法复用 |
| E2 | 当 BizModel/Processor 需要获取关联子实体时，是否优先使用 ORM 关系 getter（`entity.getLines()`）而非 `daoFor(ChildEntity.class).findAllByQuery()`？ | 涉及子实体访问的方法 | 冗余查询，绕过 ORM 缓存 |
| E3 | BizModel 中 `daoFor()` 调用是否有注释说明原因（同域子实体/架构约束/只读聚合）？ | 所有 `daoFor()` 调用 | 后续维护者无法区分有理由的 daoFor 和反模式 |

- 不能批量写完所有方法后统一自检
- P2 对所有方法强制执行；P1/P3/P4/E1/E2/E3 按场景检查

### 项目文件位置

| 文件类型 | 位置 |
|---------|------|
| IBiz 接口 | `app-mall-dao/src/main/java/app/mall/biz/I{Entity}Biz.java` |
| BizModel 实现 | `app-mall-service/src/main/java/app/mall/service/entity/{Entity}BizModel.java` |
| 错误码 | `app-mall-service/src/main/java/app/mall/service/AppMallErrors.java` |
| 常量 | `app-mall-service/src/main/java/app/mall/service/AppMallConstants.java` |
| DTO | `app-mall-dao/src/main/java/app/mall/dao/dto/` 或 `app-mall-service/` |
| 外部RPC接口 | `app-mall-api/src/main/java/app/mall/` |

---

## 代码模式速查

### 1. BizModel 最小结构

```java
@BizModel("LitemallOrder")
public class LitemallOrderBizModel extends CrudBizModel<LitemallOrder>
    implements ILitemallOrderBiz {
    public LitemallOrderBizModel() {
        setEntityName(LitemallOrder.class.getName());
    }
}
```

### 2. IBiz 接口声明

```java
public interface ILitemallOrderBiz extends ICrudBiz<LitemallOrder> {
    @BizMutation
    LitemallOrder createOrder(@Name("addressId") String addressId,
                              @Name("message") String message,
                              IServiceContext context);

    @BizQuery
    List<LitemallOrder> myOrders(@Name("orderStatus") Short orderStatus,
                                 IServiceContext context);
}
```

### 3. Processor 模式（多步编排，productization-ready）

```java
// Facade 层：BizModel 入口，负责 @BizMutation 注解和事务边界
@BizModel("Order")
public class OrderBizModel extends CrudBizModel<Order> implements IOrderBiz {

    @Inject
    OrderSubmitProcessor submitProcessor;  // 注入 Processor（非 I*Biz）

    @Override
    @BizMutation
    public Order submit(@Name("orderId") Long orderId, IServiceContext context) {
        return submitProcessor.process(orderId, context);
    }
}

// Processor 层：纯编排，内部 step 方法标记为 protected（下游可逐个覆盖）
public class OrderSubmitProcessor {

    @Inject
    ICustomerBiz customerBiz;

    public Order process(Long orderId, IServiceContext context) {
        Order order = validateTransition(orderId, context);   // step 1
        validateBusinessRules(order, context);                 // step 2
        doSubmit(order, context);                              // step 3
        postProcess(order, context);                           // step 4
        return order;
    }

    // ↓ 每个 step 标记为 protected（不是 private），下游可 derived bean override
    protected Order validateTransition(Long orderId, IServiceContext context) {
        Order order = orderBiz.requireEntity(String.valueOf(orderId), null, context);
        if (order.getApproveStatus() != UNSUBMITTED) {
            throw new NopException(ERR_ILLEGAL_STATUS).param(...);
        }
        return order;
    }

    protected void validateBusinessRules(Order order, IServiceContext context) {
        // 下游可覆盖此方法加入自定义校验
    }

    protected void doSubmit(Order order, IServiceContext context) {
        order.setApproveStatus(SUBMITTED);
        orderBiz.updateEntity(order, null, context);
    }

    protected void postProcess(Order order, IServiceContext context) {
        // 下游可覆盖此方法加入通知/日志等后置处理
    }
}
```

> **为什么这样写：** 下游客户想加"订单金额 >100K 需总监审批"只需覆盖 `validateBusinessRules()` 一行，不用重写整个 `submit()` 方法。这就是产品化的核心机制。

### 4. 外部 RPC Service 接口（api 模块）

`*-api/` 模块中的 Service 接口用于外部系统 RPC 调用。直接使用业务 DTO 类型，不包装 `ApiRequest`/`ApiResponse`：

```java
@BizModel("PayService")
public interface PayService {
    @BizMutation("refund")
    PayRefundResponseBean refund(PayRefundRequestBean req);
}
```

异步调用等高级特性见 `{DOCS-FOR-AI}/04-reference/async-service-guide.md`，一般业务开发不需要使用。

### 5. 参数规则

- ≤5 个参数 → `@Name`
- \>5 个参数 → `@RequestBean` + `@DataBean` DTO
- 不要用 `Object` 或 raw `Map` 代替 DTO

### 6. 跨实体访问

```java
@Inject
ILitemallGoodsProductBiz goodsProductBiz;

// 业务代码：走权限管道
LitemallGoodsProduct product = goodsProductBiz.requireEntity(productId, null, context);

// 已持有实体的关联 → 直接用关系 getter
LitemallGoods goods = product.getGoods();
```

### 7. CrudBizModel API 签名

**基础签名见 `io.nop.orm.biz.ICrudBiz<T>`（已实现）。以下仅列出 CrudBizModel 额外提供的 `do*` 辅助方法和钩子。**

> **写代码前必须确认签名与 ICrudBiz 一致。最常见错误：漏参数、传错类型。**

#### ICrudBiz 高频方法速查（防错）

```java
// ★ 查询类 — 全部三参数：(query, selection, context)
List<T>   findList(QueryBean query, FieldSelectionBean selection, IServiceContext context);
PageBean<T> findPage(QueryBean query, FieldSelectionBean selection, IServiceContext context);
long      findCount(QueryBean query, IServiceContext context);
T         findFirst(QueryBean query, FieldSelectionBean selection, IServiceContext context);
T         get(String id, boolean ignoreUnknown, IServiceContext context);

// ★ 修改类
T         save(Map<String, Object> data, IServiceContext context);
T         update(Map<String, Object> data, IServiceContext context);
boolean   delete(String id, IServiceContext context);           // ★ 参数是String id，不是实体！
Set<String> batchDelete(Set<String> ids, IServiceContext context);

// ★ 管道类（@BizAction）
T         requireEntity(String id, String action, IServiceContext context);
void      saveEntity(T entity, String action, IServiceContext context);
void      updateEntity(T entity, String action, IServiceContext context);
void      deleteEntity(T entity, String action, IServiceContext context);
T         newEntity();
```

#### CrudBizModel 额外 do* 辅助方法

```java
// 带自定义prepareQuery的分页/列表
PageBean<T> doFindPage(QueryBean query,
    BiConsumer<QueryBean, IServiceContext> prepareQuery,
    FieldSelectionBean selection, IServiceContext context);

List<T> doFindList(QueryBean query,
    BiConsumer<QueryBean, IServiceContext> prepareQuery,
    FieldSelectionBean selection, IServiceContext context);

// 绕过管道直接查数据库（底层方法，业务代码谨慎使用，需注释说明原因）
List<T> doFindListByQueryDirectly(QueryBean query, IServiceContext context);
PageBean<T> doFindPageByQueryDirectly(QueryBean query, FieldSelectionBean selection, IServiceContext context);
```

#### 钩子方法（覆盖以注入自定义逻辑）

```java
protected void defaultPrepareQuery(QueryBean query, IServiceContext context);
protected void defaultPrepareSave(EntityData<T> entityData, IServiceContext context);
protected void defaultPrepareUpdate(EntityData<T> entityData, IServiceContext context);
protected void defaultPrepareDelete(T entity, IServiceContext context);
```

#### QueryBean 排序

```java
// 两参数：字段名 + 是否降序（不是 "-fieldName" 字符串！）
query.addOrderField("addTime", true);   // addTime 降序
query.addOrderField("isDefault", true);
```

#### 常见调用模式

```java
List<T> list = findList(query, null, context);              // selection传null
PageBean<T> page = findPage(query, null, context);          // selection传null
T entity = requireEntity(id, null, context);                // action传null
T entity = get(id, false, context);                         // 不存在返回null
delete(id, context);                                        // ★ 传String id
```

### 8. 错误处理

所有业务异常必须使用 `ErrorCode` + `NopException`：

```java
public interface AppMallErrors {
    String ARG_ORDER_ID = "orderId";
    ErrorCode ERR_ORDER_NOT_FOUND = ErrorCode.define(
        "nop.err.mall.order.not-found",
        "Order not found: {orderId}",
        ARG_ORDER_ID
    );
}

throw new NopException(AppMallErrors.ERR_ORDER_NOT_FOUND)
    .param(AppMallErrors.ARG_ORDER_ID, orderId);
```

规则：
- 所有错误码定义在 `*Errors.java` 接口中，不要散落在业务代码里
- `ErrorCode.define()` 描述用中文，框架通过 i18n 翻译
- 用 `.param(...)` 附加上下文参数（实体 ID、当前状态等）
- 包装底层异常：`new NopException(ERR_XXX, e).param(...)`

### 9. 事务后回调

```java
txn().afterCommit(null, () -> {
    sendNotification(order);
});
```

### 10. QueryBean 查询构造

```java
QueryBean query = new QueryBean();
query.addFilter(FilterBeans.eq("userId", userId));
query.addFilter(FilterBeans.in("status", statusList));
query.setLimit(20);
```

---

## 反模式表（快速参考，完整自检见 selfcheck 文档）

> 以下仅列出最常见反模式。**每写完/修改一个方法后，必须用两层自检完整校验，不能只看此表。**

| 不要这样写 | 应该这样写 |
|-----------|-----------|
| `dao().getEntityById(id)` | `requireEntity(id, null, context)` |
| `dao().findAllByQuery(query)` | `findList(query, null, context)` |
| `dao().saveEntity(entity)` | `saveEntity(entity, null, context)` |
| `new LitemallOrder()` | `newEntity()` |
| `@BizMutation @Transactional` | 只用 `@BizMutation`（除非有显式独立事务边界且已文档说明） |
| `@Inject private` | `@Inject` 不能 private |
| `extends RuntimeException` | `extends NopException` |
| `throw new RuntimeException("msg")` 或 `new NopException("msg")` | 必须用 `ErrorCode.define()` + `new NopException(ERR_XXX).param(...)` |
| 直接注入其他BizModel实现类 | 注入 `I*Biz` 接口 |
| IBiz接口方法缺少注解 | 必须有 `@BizQuery`/`@BizMutation`/`@BizAction` |
| BizModel新增public方法未同步IBiz | 必须先在接口声明 |
| 自定义方法与ICrudBiz标准方法重名 | 用不同的名字 |
| 已有ORM关系时用IBiz.get()获取关联 | 用关系getter |
| `daoProvider().daoFor(Xxx.class)` 在业务BizModel中 | 注入 `I*Biz` |
| `findList(query, context)` 少传selection参数 | `findList(query, null, context)` 三参数 |
| `delete(entityObj, context)` 传实体对象 | `delete(id, context)` 传String id |
| `query.addOrderField("-fieldName")` 传字符串 | `query.addOrderField("fieldName", true)` 传name+desc |
| `doFindListByQueryDirectly()` 在业务代码中 | `findList(query, null, context)` 走管道 |
| `new XxxEntity()` 直接new实体 | `newEntity()` 走OrmEntity工厂 |
| `findList(query, null, context).size()` 只为计数 | `findCount(query, context)` |
| 局部DTO放入 `*-api/` 模块 | 放 `*-dao/.../dto/` 或 `*-service/` |
| 创建 `*Service`/`*Controller` 类 | Nop用BizModel/IBiz |
| BizModel返回值无脑用DTO代替Entity | 实体能表达的优先用实体 |
| 创建无xmeta的伪BizModel | GraphQL无法识别 |
| **多步流程直接写在 BizModel 方法体内，全部 `private` | 多步→Processor（protected step）或 task.xml |
| **task.xml 编排时 Java 还写了完整实现 | Java 只占位；真正实现在 task.xml |
| **xbiz 里写了 `<source>` 但 Java 也写了实现 | xbiz 脚本和 Java 实现只选一种，不要两边都写 |

---

## 平台工具类

| 用途 | 正确用法 | 不要用 |
|------|---------|--------|
| 时间 | `io.nop.api.core.time.CoreMetrics.currentTimeMillis()` | `System.currentTimeMillis()` |
| JSON | `JsonTool.parse/serialize()` | 第三方JSON库 |
| 字符串 | `StringHelper.isEmpty/isBlank/...` | Apache Commons |
| 资源关闭 | `IoHelper.safeClose(obj)` | 手写try-catch close |

---

---

## 业务类型注册清单

新增一个 BusinessType（业务类型）时，必须依次完成以下所有注册点，缺一不可：

| # | 注册点 | 文件位置 | 说明 |
|---|--------|---------|------|
| 1 | ORM 字典定义 | `<domain>/model/*.orm.xml` `<dict name="biz-type">` | 真相源，code/value/desc 三者完备 |
| 2 | 生成的 `dict.yaml` | codegen 生成 | 自动从 ORM 同步。**不要手改**（有 `__XGEN_FORCE_OVERRIDE__`） |
| 3 | Java 常量 | `IAppErp<Domain>DaoConstants.java` | codegen 自动从 `biz-type` 字典生成 `BIZ_TYPE_XXX` 常量 |
| 4 | ErrorCode | `*Errors.java` | 如果新类型有独立错误场景，定义 `ERR_...` |
| 5 | Provider 注册 | 域内 `*AcctDocProvider.java` | 如果业务类型参与业财过账，注册 route |
| 6 | Dispatcher 路由 | `*AcctDocRegistry` / Dispatcher | 保证 `businessType` → Provider 可路由 |
| 7 | bean 注入 | `*-service.beans.xml` | Provider/Processor 类注册为 Nop IoC bean |
| 8 | JUnit 测试 | `*Service/src/test/` | 至少测路由+过账语义 |
| 9 | E2E 测试 | `tests/e2e/` | 如果业务类型有独立页面操作路径 |

> **历史教训**：`MAINTENANCE_ISSUE` 因缺 ErrorCode + Provider 注册导致运行时 NPE；`business-type.dict.yaml` 因手改被 codegen 覆盖，废 3 轮执行。注册顺序从真相源（ORM）开始，逐级向下，不跳步。


## ORM 实体构造反转模式（Revert Pattern Awareness）

当你重构一个**纯函数组件**（无 IoC/DB 依赖），将其改为依赖注入的 ORM 实体构造方式（`new ErpXxx()` → `daoProvider().daoFor(...).newEntity()`），需要特别谨慎：

| 判断 | 不应该改 | 应该保留原状 |
|------|---------|-------------|
| 组件创建实体后直接返回，不持久化 | ✅ 保持 `new XxxEntity()` | ❌ 不要改用 `newEntity()` |
| 组件内部完成全部计算，无外部依赖 | ✅ 保持纯函数 | ❌ 不要加 `@Inject` |
| 组件被多个 BizModel/Service 通过 `new` 直接调用 | ✅ 保持构造函数兼容 | ❌ 不要改为 Spring/Nop IoC 托管 |
| 组件有对应的单元测试直接 `new Component()` 而不走容器 | ✅ 保持测试兼容 | ❌ 不要引入 `@Inject` 使测试需要启动容器 |

### 具体反例

```java
// 问题：Engine 是纯函数，由多个调用方 `new Engine()` 使用
// 改成 @Inject + daoProvider().daoFor(...).newEntity() 后：
// 1. 所有调用方需改为容器注入 → 改动范围大
// 2. 测试不能再 new Engine() → 必须上容器
// 3. 无持久化行为的引擎内 newEntity() 无意义

// ✅ 正确做法：纯函数保持纯函数。newEntity() 只在 BizModel 层使用，Engine 不碰 ORM。
```

**判断基准**：如果该组件的主要职责是计算/规则/转换（而非持久化），且调用方通过构造函数直接实例化，保留 `new XxxEntity()` + 手动 setter。只有当组件自身就是持久化流程的编排者时才改为 `newEntity()`。

---

## 参考文件

- `{DOCS-FOR-AI}/05-examples/ibiz-and-bizmodel.java` — IBiz + BizModel 完整示例
- `{DOCS-FOR-AI}/05-examples/dto-and-errors.java` — DTO + ErrorCode 示例
- `{DOCS-FOR-AI}/05-examples/entity-class.java` — 实体类 + 领域方法示例
- `{DOCS-FOR-AI}/05-examples/delta-customization.java` — Delta定制示例
- `{DOCS-FOR-AI}/04-reference/bizmodel-method-selfcheck.md` — 19项方法自检清单
