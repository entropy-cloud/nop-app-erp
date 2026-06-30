# 测试策略

## 目的

定义 nop-app-erp 的三层测试体系，确保系统质量和回归防护。

## 三层测试

| 层 | 测试类型 | 工具 | 覆盖范围 |
|----|----------|------|----------|
| L1 | 单元测试 | JUnit 5 + Mockito | BizModel 方法、工具类、状态机 |
| L2 | 集成测试 | Nop 测试框架（IGraphQLEngine） | 跨域调用、事务边界、事件驱动 |
| L3 | 端到端测试 | Playwright | 完整业务流程、UI 交互 |

## Nop 测试框架

### 测试基类

- `JunitAutoTestCase`：基于快照录制/回放的自动化测试
- `IGraphQLEngine`：GraphQL 查询引擎，用于模拟跨域调用

### 测试流程

```
RECORDING 模式：
    1. 执行测试
    2. 录制实际输出为快照文件
    3. 人工审查快照正确性

CHECKING 模式：
    1. 执行测试
    2. 比较实际输出与快照
    3. 不一致则测试失败
```

### request.json5 手写

测试输入使用 `request.json5` 文件，支持 `@var` 变量机制：

```json5
{
  "orderId": "@var:orderId",
  "items": [
    { "materialId": "MAT001", "qty": 100 }
  ]
}
```

## 测试覆盖要求

| 测试类型 | 覆盖要求 |
|----------|----------|
| BizModel 方法 | 每个公开方法至少 1 个测试 |
| 状态机迁移 | 每条迁移至少 1 个测试 |
| 跨域调用 | 每个跨域场景至少 1 个测试 |
| 错误处理 | 每个 ErrorCode 至少 1 个测试 |

## 测试数据管理

- 冒烟测试采用**自包含设计**（先 mutation 建实体再验证），CHECKING 模式每方法从 `_cases/.../input/tables/*.csv` 自动恢复录制时的库快照（含 `nop_sys_sequence`）到 H2 内存库，不依赖外部种子数据，保证可稳定复现。
- 测试间数据隔离（每方法独立事务回滚；序列由 input 快照恢复，故自增 id 在录制↔校验间稳定）。
- 之前提及的种子数据模块 `app-erp-seed` **尚不存在**；若后续业务测试需要共享主数据夹具，可作为独立 follow-up（见各执行计划的 Deferred 项），当前不阻塞冒烟测试。

## Nop 测试 runbook 落地（Phase 4 CRUD 冒烟实践沉淀）

> 以下由 `docs/plans/2026-06-30-2328-2-phase4-crud-smoke-tests.md` 实测沉淀，供后续 BizModel 业务测试复用。

### 落位约定

- CRUD 行为测试置于 `module-<domain>/erp-<short>-service/src/test/java/app/erp/<short>/service/`（service 层是 CRUD 行为归属层；`-app` 模块会引入 Quarkus 完整启动开销，冒烟测试不需要）。
- 测试类继承 `JunitAutoTestCase`；输入可手写 `request.json5` 或直接在 Java 内 `ApiRequest.build(...)` 构造（跨步引用 id 时更推荐后者）；快照置于测试类相对的模块根 `_cases/<pkg>/<TestClass>/<method>/{input,output}/`。

### Schema bootstrap（`@NopTestConfig`）

`-service` 模块无 `application.yaml`，`init-database-schema` 仅在 `-app` 且反向不传递。故用类级注解显式控制：

- **首次录制**（空 H2）：`@NopTestConfig(localDb = true, initDatabaseSchema = OptionalBoolean.TRUE, enableActionAuth = OptionalBoolean.FALSE, snapshotTest = SnapshotTest.RECORDING)`。
- **日常校验**：去掉 `snapshotTest`（默认 CHECKING），保留 `localDb/initDatabaseSchema/enableActionAuth`（CHECKING 仍在全新 H2 内存库运行，需建表）。
- RECORDING 每方法执行后框架抛 `nop.err.autotest.snapshot-finished`（Maven 显示 `Errors: X` 为预期），切 CHECKING 后归零。

### 关键约束（实测）

1. **CRUD 动作映射**：`<Biz>__save`=纯插入，`<Biz>__update`=按 id 合并（编辑保存用例必须用 `update`），`<Biz>__delete`=逻辑删除，`<Biz>__get`/`<Biz>__findPage`=查询。
2. **主键非自动变量**：ERP 主键一律 `tagSet="seq-default"`，而平台自动变量机制仅认 `seq`/`var`/`clock`/时间属性，故 `@var:Entity@id` 不会自动注册。多步测试在 Java 内从首步响应取 id 后传入次步；CHECKING 序列由快照恢复，id 稳定。
3. **跨域引用校验**：`ObjMetaBasedValidator.validateRefValue` 对**已提供**的引用列校验引用方业务对象是否注册、记录是否存在。跨域**强制**外键（如 sales Quotation 的 customer/currency）需在被测模块加 `app-erp-master-data-service` test 依赖并以 `createPrereqs()` 自建主数据；跨域**可选**外键直接省略即可通过。
4. **快照非确定性屏蔽**：逻辑删除使 `delVersion` 设为 `currentTimeMillis`（时钟型、非变量），手工将删除用例 `output/tables/*.csv` 的 `DEL_VERSION` 列置通配符 `*`；同理 decimal 域列（quantity/amount/price/exchangeRate 等，多为 VARCHAR 存储）录制↔校验刻度格式不一致（`0`↔`0.0000`），统一置 `*`。`CREATE_TIME`/`UPDATE_TIME` 由框架自动 `*` 屏蔽。

### 推荐写法

三层验证叠加：显式 `assertEquals` 核心业务字段 + `output()` 完整响应快照 + 框架自动数据库状态快照（`output/tables/*.csv`）。
