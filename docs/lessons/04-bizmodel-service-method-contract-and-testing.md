# 04-bizmodel-service-method-contract-and-testing

> 日期：2026-07-01
> 来源：I*Biz 接口合规整改（计划 2026-07-01-1900-1）调试过程
> 状态：已验证

## 教训

BizModel / I*Biz 的自定义服务方法有一组容易踩的契约，AI 在整改中连续违反多次。分两层：**实现层契约**（写代码）和**测试层契约**（写测试）。

### 实现层契约（docs-for-ai/02-core-guides/service-layer.md 已收录）

1. **每个 public 方法必须标注 `@BizQuery`/`@BizMutation`/`@BizAction` 之一**。`BizProxyInvocationHandler` 按注解路由，无注解则代理不识别、GraphQL 不暴露。
2. **方法最后一个参数必须是 `IServiceContext context`**。对齐基类 `ICrudBiz`（全方法末参为 context），它承载 `IUserContext`（身份/数据权限）、缓存、事务上下文，跨服务调用（A 域注入调 B 域 `I*Biz`）必须透传。
3. **业务参数（非 context）必须标 `@Name`**。GraphQL 反射 `ReflectionBizModelBuilder` 要求每个非内部参数有 `@Name`，否则启动报 `method-param-no-reflection-name-annotation`。`IServiceContext` 是引擎识别的内部参数，可豁免。
4. **`IServiceContext` 的包是 `io.nop.core.context`**（不是 `io.nop.api.core.context`，后者不存在）。
5. **BizModel 方法不要加 `@SingleSession` / `@Transactional`**。管道（`@BizMutation`）已包事务；ORM Session 由执行环境（GraphQL 引擎 / 管道）管理。`@SingleSession` 仅用于 non-BizModel bean（定时任务、独立 service）。

### 测试层契约（关键，反复犯错）

6. **测试 BizModel 服务方法必须经 `IGraphQLEngine`，不能直调 `bizObj.method(args, context)`**。直调时执行环境不提供 OrmSession，多步方法（saveEntity 后再 updateEntity）会报 `nop.err.orm.dao.update-entity-no-current-session`；且裸 `new ServiceContextImpl()` 不携带身份/权限/缓存。
   - 正确模式见 `docs-for-ai/05-examples/test-examples.java` 示例 2b/4/5：
     ```java
     @Inject IGraphQLEngine graphQLEngine;
     IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(
         GraphQLOperationType.mutation, "ErpInvStockMove__generateMove",
         ApiRequest.build(Map.of("request", requestData)));
     ApiResponse<?> result = graphQLEngine.executeRpc(ctx);
     ```
   - 引擎负责建 session、注入 context、走完整管道（权限/Meta/事务）。这是 CRUD 冒烟测试一直用的方式（`TestErpInvStockMoveCrudSmoke`）。
   - **曾误判**：一度以为"多步 BizModel 方法需要 `@SingleSession` 维持 session"——错。实测 `generateMove` 经 `IGraphQLEngine` 不加任何 `@SingleSession` 即正常（`TestErpInvStockMoveGraphQL` 验证通过）。根因是测试直调缺 session 环境，不是 BizModel 缺注解。

## 代价

整改中因违反上述契约，反复调试：先漏 `IServiceContext` 末参 → 漏 `@Name` → 误加 `@SingleSession`（基于错误根因判断）→ 最终经 `IGraphQLEngine` 验证才锁定"测试方式"是真因。每个错误都触发一轮 编译/测试/回滚。

## 可复用检查工具

`nop-entropy/ai-dev/tools/check-ibiz-interfaces.mjs`（tree-sitter-java）静态检查 I*Biz 接口的契约 1/2（缺注解 / 缺 `IServiceContext` 末参）。整改后应先跑它定位全部违规，再修。

## 关联

- 平台规则：`nop-entropy/docs-for-ai/02-core-guides/service-layer.md` 反模式表
- 测试示例：`nop-entropy/docs-for-ai/05-examples/test-examples.java`
- 计划：`docs/plans/2026-07-01-1900-1-platform-compliance-remediation.md`
