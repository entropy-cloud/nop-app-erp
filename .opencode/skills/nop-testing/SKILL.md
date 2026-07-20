---
name: nop-testing
description: Nop平台测试开发（JunitAutoTestCase / IGraphQLEngine / 快照录制回放）。涵盖测试基类选择、@NopTestConfig配置、request.json5手写、@var变量机制、RECORDING→CHECKING切换。触发词：写测试、测试、快照、录制、回放、IGraphQLEngine、JunitAutoTestCase。
---

# Nop 测试开发


> **项目定制化层（nop-app-erp）**：使用本技能前必须先读 `AGENTS.md` 与 `docs/skills/README.md §项目定制化层（nop-app-erp）`，将本仓库的保护区域（`module-<domain>/model/*.orm.xml` ask-first、会计/财务/数据删除）、验证命令（`mvn clean install -DskipTests`）、命名约定（`Erp<Domain>` 实体前缀、`erp-<short>/<dict>` 字典、`erp.err.<short>` ErrorCode 前缀）和已知失败模式注入上下文。


## 什么时候用我

| 场景 | 触发关键词 |
|------|-----------|
| 写BizModel测试 | "写测试"、"测试BizModel"、"IGraphQLEngine" |
| 快照录制回放 | "快照"、"录制"、"RECORDING"、"CHECKING" |
| 集成测试 | "集成测试"、"JunitBaseTestCase"、"@NopTestConfig" |
| 多步业务流程测试 | "多步测试"、"流程测试"、"@var" |
| E2E测试 | "E2E"、"Playwright"、"端到端" |
| 纯逻辑测试 | "单元测试"、"纯逻辑"、"BaseTestCase" |

---

## 必读文档路径

以下路径相对于 `{DOCS-FOR-AI}` 目录。`{DOCS-FOR-AI}` 的实际位置由当前项目 `AGENTS.md` 的 "Nop Platform Documentation" 部分指定（例如 `../nop-entropy/docs-for-ai/`）。

### 全局必读（写任何测试代码前全部读完）

| 文档 | 为什么必读 | 不读会怎样 |
|------|-----------|-----------|
| `05-examples/test-examples.java` | 先看示例再写测试：4种测试模式的精简代码骨架 | 选错测试基类；不知道input/output怎么用 |
| `02-core-guides/testing.md` | 示例之后的规则补充：基类选择、@NopTestConfig能力矩阵、快照模式、异步防挂起规则 | 容器不启动或录制回放不生效 |

### 按场景选读

| 场景 | 文档 |
|------|------|
| 编写测试用例 | `03-runbooks/write-tests.md` |
| @NopTestConfig集成测试 | `03-runbooks/write-integration-test-with-noptestconfig.md` |
| Mock bean | `03-runbooks/add-test-mock-bean.md` |
| E2E测试 | `02-core-guides/e2e-testing.md` |

---

## 自检纪律

**每完成一个测试类后，对照下方反模式表逐项校验。** 自检在每个测试类完成后执行，不是逐方法检查。

---

## 项目文件位置

| 测试类型 | 数据位置 | 代码位置 |
|---------|---------|---------|
| 快照测试 `_cases/` | `{module}/_cases/{package}/{TestClass}/{method}/` | `xxx-service/src/test/java/...` |
| 普通资源 | `src/test/resources/...` | 对应模块 `src/test/java/...` |

快照目录结构：
```
_cases/app/mall/service/entity/
  TestLitemallOrderBizModel/
    testCreateOrder/
      input/
        request.json5           ← 手写输入
        tables/                 ← 种子数据CSV（可选）
      output/
        response.json5          ← 录制的输出（自动生成）
        tables/                 ← 录制的DB状态CSV（自动生成）
```

---

## 测试基类选择

| 场景 | 基类 | 特点 |
|------|------|------|
| 纯逻辑（无DB无IoC） | `BaseTestCase` + `CoreInitialization` | 最轻量 |
| 需要容器+DB，不需要快照 | `JunitBaseTestCase` | @Inject可用，localDb |
| 需要录制回放 | `JunitAutoTestCase` | input/output + _cases目录 |
| 多步骤流程 | `JunitAutoTestCase` | 编号文件 + @var占位符 |
| 需要精确断言 | `JunitBaseTestCase` | 直接JUnit assertXxx |

---

## 快照测试工作流

### 1. 首次录制（从空库）

```java
@NopTestConfig(localDb = true, initDatabaseSchema = OptionalBoolean.TRUE,
               snapshotTest = SnapshotTest.RECORDING)
public class TestLitemallOrderBizModel extends JunitAutoTestCase {
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testCreateOrder() {
        ApiRequest<?> request = request("request.json5", Map.class);
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(
            GraphQLOperationType.mutation, "LitemallOrder__createOrder", request);
        ApiResponse<?> result = graphQLEngine.executeRpc(ctx);
        output("response.json5", result);
    }
}
```

手写 `input/request.json5`：
```json5
{
  data: {
    addressId: "addr-001",
    message: "测试订单"
  }
}
```

录制完成后：
- `output/response.json5` 自动生成
- `output/tables/*.csv` 自动生成（录制DB状态）

### 2. 日常校验

```java
@NopTestConfig   // 默认 CHECKING 模式
public class TestLitemallOrderBizModel extends JunitAutoTestCase {
    // 同上，框架自动从 _cases/ 加载快照数据到H2并比对输出
}
```

### 3. 更新输出

```java
@NopTestConfig(forceSaveOutput = true)  // 不校验，仅更新输出文件
```

---

## request.json5 手写规范

### 参数格式取决于方法签名

| 方法签名 | request.json5 结构 |
|---------|-------------------|
| `save(@Name("data") Map data, ...)` | `{ data: { data: { name: "张三" } } }` — 嵌套一层data |
| `cancel(@Name("id") String id, ...)` | `{ data: { id: "..." } }` — 扁平参数 |
| `getMyList(...)` | `{ data: {} }` — 无参数空map |

### 多步测试文件命名

```
input/1_create_request.json5
input/2_cancel_request.json5
input/3_query_request.json5
output/1_create_response.json5
output/2_cancel_response.json5
output/3_query_response.json5
```

---

## 多步测试模式

```java
@EnableSnapshot
@Test
public void testOrderFlow() {
    setUser("0", "test");

    // Step 1: 创建 — ORM钩子自动注册 @var:LitemallOrder@id
    ApiResponse<?> r1 = executeRpc(GraphQLOperationType.mutation,
        "LitemallOrder__createOrder", request("1_create.json5", Map.class));
    output("1_create_response.json5", r1);

    // Step 2: 取消 — @var:LitemallOrder@id 由上一步自动提供
    executeRpc(GraphQLOperationType.mutation,
        "LitemallOrder__cancelOrder", request("2_cancel.json5", Map.class));

    // Step 3: 查询
    ApiResponse<?> r3 = executeRpc(GraphQLOperationType.query,
        "LitemallOrder__findPage", request("3_query.json5", Map.class));
    output("3_query_response.json5", r3);
}
```

### @var 变量机制

- ORM实体的主键（`tagSet="seq"`）自动成为变量：`@var:LitemallOrder@id`
- 外键自动引用被引用表的主键变量
- `request()` 读取时自动解析 `@var:` → 实际值
- `output()` 保存时自动替换实际值 → `@var:`
- **不要手动从ApiResponse提取ID作为Java变量传递**

### request() 与 input() 的关系

- **`input(file, type)`** — 通用 JSON 加载函数。从 `_cases/.../input/` 读取 JSON5 文件，反序列化为 `type` 指定的类型。
- **`request(file, bodyType)`** — 基于 `input()` 实现，专门用于构造 `ApiRequest`。返回 `ApiRequest<bodyType>`，其中 `bodyType` 指定 `ApiRequest.data` 的 Java 类型。

```java
// BizModel 测试：推荐 request()，bodyType 用 Map.class
ApiRequest<Map> req = request("save.json5", Map.class);  // → ApiRequest<Map>

// 等效的 input() 写法（一般不需要这样用）
ApiRequest<Map> req = input("save.json5", new TypeRef<ApiRequest<Map>>(){});
```

**永远不要** `request("file.json5", ApiRequest.class)` — 会产生 `ApiRequest<ApiRequest>` 嵌套。对 BizModel 方法始终传 `Map.class`。

---

## 辅助方法

```java
void setUser(String userId, String userName) {
    ContextProvider.getOrCreateContext().setUserId(userId);
    ContextProvider.getOrCreateContext().setUserName(userName);
}

ApiResponse<?> executeRpc(GraphQLOperationType opType, String action,
                          ApiRequest<?> request) {
    IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
    return graphQLEngine.executeRpc(ctx);
}
```

---

## @NopTestConfig 配置速查

| 场景 | 配置 |
|------|------|
| 首次录制（空库） | `@NopTestConfig(localDb=true, initDatabaseSchema=OptionalBoolean.TRUE, snapshotTest=SnapshotTest.RECORDING)` |
| 首次录制（已有库） | `@NopTestConfig(localDb=true, snapshotTest=SnapshotTest.RECORDING)` |
| 日常校验 | `@NopTestConfig`（裸注解） |
| 更新输出 | `@NopTestConfig(forceSaveOutput=true)` |

---

## @EnableSnapshot 方法级快照控制

`@EnableSnapshot` 是方法级注解，在 CHECKING 模式下按方法控制快照行为。

### 参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `localDb` | `true` | 强制使用 H2 内存数据库 |
| `sqlInput` | `true` | 是否自动执行 input 目录下的 SQL 文件 |
| `sqlInit` | `true` | 是否执行 SQL 初始化脚本 |
| `tableInit` | `true` | 是否将 `input/tables/` CSV 插入数据库 |
| `saveOutput` | `false` | 是否保存输出（设为 `true` 即为录制模式） |
| `checkOutput` | `true` | 是否校验输出与录制结果匹配 |

### 全局 vs 单方法录制控制

| 目标 | 做法 |
|------|------|
| 全部重新录制 | `@NopTestConfig(snapshotTest = SnapshotTest.RECORDING)` |
| 全部仅更新输出 | `@NopTestConfig(forceSaveOutput = true)` |
| 单个方法重新录制 | 类保持裸 `@NopTestConfig`，目标方法加 `@EnableSnapshot(saveOutput = true)` |
| 单个方法跳过校验 | `@EnableSnapshot(checkOutput = false)` |
| 全局禁用快照 | `nop.autotest.disable-snapshot=true` 或 `@NopTestConfig(snapshotTest = SnapshotTest.NOT_USE)` |

### 关键约束

- **类级 RECORDING 模式下 `@EnableSnapshot` 被完全忽略**，所有方法强制录制。
- **裸 `@EnableSnapshot` 与不加行为相同**（默认值与 CHECKING 一致），仅用作多步测试的惯例标记。

### 单方法重新录制示例

```java
@NopTestConfig  // 默认 CHECKING
public class TestOrder extends JunitAutoTestCase {
    @Test
    public void testQuery() { /* 普通校验 */ }

    @EnableSnapshot(saveOutput = true)  // 仅此方法重新录制
    @Test
    public void testCreate() {
        output("response.json5", executeRpc(...));
    }
}
```

录制完成后去掉 `saveOutput = true` 或去掉整个 `@EnableSnapshot`。完整参数说明和源码机制见 `{DOCS-FOR-AI}/02-core-guides/testing.md` 的"@EnableSnapshot 方法级快照控制"章节。

---

## 录制模式行为说明

录制模式下每个测试方法执行完毕后框架抛 `nop.err.autotest.snapshot-finished` 异常表示录制完成。这是**预期行为**不是测试失败。Maven 输出会显示 `Tests run: X, Errors: X`，切换到 CHECKING 模式后 Errors 归零。

### 特殊情况：拒绝路径与 ORM Hook 捕获临时插入

当测试拒绝路径（如校验失败提前抛出异常）时，ORM Hook 可能在异常抛出前已捕获到临时插入的实体行，导致 RECORDING 模式捕获的 `output/tables/*.csv` 中包含这些临时行，而 CHECKING 模式因异常提前退出不产生这些行 → `output-row-not-exists` 比较失败。

**处理方式**：

- 如果能控制拒绝路径的触发时机，确保 ORM Hook 在触发前已经 flush：
  ```java
  ormTemplate().flushSession();
  ```
- 如果无法控制（如框架级校验在前），考虑将拒绝路径测试降级为 CHECKING 模式 + 显式 `@EnableSnapshot(checkOutput = false)`，仅验证响应结果不验证 DB 状态。
- 如果拒绝路径不产生最终持久化业务结果，用 `@EnableSnapshot(saveOutput = false)` 只校验输入 → 输出的契约。**不要在 RECORDING 模式下录制拒绝路径场景的 DB snapshot**，这会产生不可比的基线。

---

## 清理顺序协议（跨域测试）

持久化测试（尤其是涉及多个域的集成测试）必须按照确定的依赖链顺序清理数据，否则残留脏数据会污染后续测试的种子数据基线。

### 清理顺序规则

按**反向依赖链**清理（先子后父，先消费方后提供方）：

```
凭证行/凭证 → AR/AP 明细 → 域业务单据（采购/销售/库存/制造等）→
库存流水 → 库存移动/盘点 → 库存余额 → 批次/序列号 →
测试物料/往来单位/人员/科目 → 主数据（仓库/币种/计量单位等）
```

### 具体规则

1. 每个域在 seed 数据和 `@Before`/`@After` 清理中遵循此顺序。
2. 跨域集成测试必须在一个事务中完成清理，或在测试类级 `@DirtiesContext` 中重建 H2 schema。
3. 永远不要依赖 `H2 MEMORY` 的 `@After` 自动清理——风险太大。**每次测试结束时对涉及的表显式 DELETE FROM**。
4. 如果测试涉及多个域的实体，删除顺序必须遵从 DAG 依赖方向的反序。

### 反例

```java
// ❌ 先删物料（被采购/库存/销售引用）→ DELETE 违反外键约束
cleanup("ErpMdMaterial");
// ❌ 先删财务凭证（被 AR/AP 引用）→ 同上
```

### 正例

```java
// ✅ 先删凭证行 → 凭证 → 业务单据 → 库存 → 物料
cleanup("ErpFinVoucherLine", "ErpFinVoucher");
cleanup("ErpPurOrderLine", "ErpPurOrder");
cleanup("ErpInvStockLedger", "ErpInvStockMove", "ErpInvStockBalance");
cleanup("ErpMdMaterial");
```

---

## 种子数据附加规则

**种子数据只能追加，不能修改已有行。** 这是 nop 快照测试框架的关键约束：

- 已有测试的 `_cases/.../output/tables/*.csv` 快照文件中记录了种子数据行的 ID 值
- 如果修改已有种子行的字段值，快照比较时会检测到差异 → 测试失败
- 如果删除已有种子行，引用该行的测试也会失败（外键不可达）

### 做法

| 场景 | 做法 |
|------|------|
| 新增实体需要种子数据 | append 新文件或新行，ID 不冲突 |
| 需要修改已有数据类型 | 在代码层面兼容，不修改种子数据 |
| 添加种子关联行 | 新建独立的 seed CSV，引用已有种子 ID |
| 确实需要修改 | `mvn clean install -DskipTests` 后全部测试 RECORDING 重录 |


## 三层测试验证模型

持久化业务逻辑测试推荐使用三层验证：

| 层 | 验证内容 | 机制 | 定位 |
|----|---------|------|------|
| 层 1 | 显式锚点断言 | Java `assertEquals` / `assertNotNull` / `assertThrows` | 关键业务断言（状态、金额、异常） |
| 层 2 | 输出响应快照 | `output("response.json5", result)` | 完整响应结构比对 |
| 层 3 | DB 状态快照 | 自动录制 `output/tables/*.csv` | 持久化状态变化全量核对 |

**什么时候用哪层：**

| 测试类型 | 层 1 | 层 2 | 层 3 |
|---------|------|------|------|
| 异常路径/拒绝路径 | ✅ 必须 | ✅ 推荐 | ❌ 跳过（见拒绝路径节） |
| 纯查询测试 | ❌ | ✅ 必须 | ✅ 推荐 |
| 写入+持久化测试 | ✅ 关键字段 | ✅ 必须 | ✅ 必须 |
| 多步流程快照测试 | ✅ 中间状态 | ✅ 每步 | ✅ 每步 |

**层 3 DB 快照最佳实践**：当测试涉及复杂的实体间状态级联时，依赖 DB 快照而非手工逐一断言每字段。

---

## E2E 测试环境协议

Playwright E2E 测试在本项目中有稳定的环境配置模式：

### 环境变量

```
BASE_URL=http://127.0.0.1:8011
SKIP_WEBSERVER=1
```

注意：`application.yaml` 中配置了 `quarkus.http.port=8011`（非默认 8080）。如果不设 `SKIP_WEBSERVER=1`，Playwright 的 webServer 配置会尝试自动启动 app（可能导致端口冲突）。

### 配置位置

需要在每个 E2E spec 的开头或 Playwright 配置中设此环境变量。推荐方式：

```bash
BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/...
```

### 常见失败排查

| 症状 | 可能原因 | 修复 |
|------|---------|------|
| 浏览器白屏（无法路由） | 8011 端口未启动 app | 先启动 runner.jar |
| npm test 无法启动 webServer | port 8080 被占用 | `SKIP_WEBSERVER=1` + `BASE_URL=...` |
| 测试超时 30s | app 未在 8011 监听 | `lsof -i :8011` 检查，启动 app |
| AMIS 弹窗不渲染 | `FormDialog` 对象无 `locator()` 方法 | 用 `page.locator()` 全局查询替代 |

---

## 反模式表

| 不要这样写 | 应该这样写 |
|-----------|-----------|
| `@Inject private` 字段 | `@Inject` 不能 private |
| 实体级测试代替IGraphQLEngine | BizModel方法必须通过IGraphQLEngine测试 |
| `request("file.json5", ApiRequest.class)` | `request("file.json5", Map.class)` |
| 手动从ApiResponse提取ID作为Java变量 | 用 `request()` + `@var:` 机制 |
| 快照数据放错目录 | `_cases/{package}/{TestClass}/{method}/` |
| 首次录制漏设 `initDatabaseSchema` | 空库必须 `OptionalBoolean.TRUE` |
| `JunitAutoTestCase` 忘加 `@NopTestConfig` | 必须加 |
| 对快照测试手写大量重复断言 | 用 `output()` 自动比对 |
| `@Name("data")` 参数用扁平JSON | 嵌套一层：`{data: {name: ...}}` |
| 类级 RECORDING 下用 `@EnableSnapshot` 控制单方法 | RECORDING 下 `@EnableSnapshot` 被忽略，改用 CHECKING + `@EnableSnapshot(saveOutput=true)` |
| 异步测试裸 `future.get()` | `future.get(5, TimeUnit.SECONDS)` |
| 异步测试裸 `BlockingQueue.take()` | `poll(timeout, unit)` |
| 异步测试类无 `@Timeout` | 类级别 `@Timeout(10)` |

---

## 参考文件

- `{DOCS-FOR-AI}/05-examples/test-examples.java` — 4种测试模式完整代码
- `{DOCS-FOR-AI}/02-core-guides/testing.md` — 测试规则与约束
- `{DOCS-FOR-AI}/03-runbooks/write-tests.md` — 写测试用例runbook
