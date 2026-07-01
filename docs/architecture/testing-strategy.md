# 测试策略

> **唯一 owner doc**：本文是 nop-app-erp 测试策略的**唯一权威文档**（与平台 `testing.md` 命名对齐）。

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
- 共享主数据夹具模块 `app-erp-test-data`（测试资产）已建骨架；部署用的种子数据模块 `app-erp-seed`（部署资产，非测试资产）尚不存在，属独立 follow-up，当前不阻塞冒烟测试（见 `seed-data.md`）。

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

## 四类测试资产边界

> 测试资产与部署资产必须区分。下表定义四类资产的归属、生命周期与发现方式。

| 资产类 | 位置 | 生命周期 | 发现方式 | 当前状态 |
|--------|------|----------|----------|----------|
| **部署 seed**（初始化数据） | `_init-data/`（部署时） | 随部署一次性导入，生产库常驻 | 部署脚本 | deferred（当前不存在，独立 follow-up；与测试无关，见 `seed-data.md`） |
| **共享测试 fixture**（跨类共享主数据） | `app-erp-test-data` 模块 `_vfs/test-data/tables/*.csv` | 测试编译期产物，多测试类共享 | VFS classpath 扫描 | 新建（见下文"app-erp-test-data 模块"） |
| **`_cases/input`**（单方法库快照） | 各 `-service` 模块 `_cases/<pkg>/<TestClass>/<method>/input/tables/*.csv` | 每测试方法私有，CHECKING 每方法恢复 | `JunitAutoTestCase` 框架自动 | 已就绪（Phase 4 沉淀） |
| **`_cases/output`**（单方法期望快照） | 各 `-service` 模块 `_cases/.../<method>/output/` | 每测试方法私有，录制后人工审查 | `JunitAutoTestCase` 框架自动 | 已就绪 |

> **关键区分**：`_cases/{input,output}` 是**每方法私有快照**（录制时的库状态），`app-erp-test-data` 是**跨类共享的主数据夹具**（物料/客户/供应商/科目等）。前者由框架管理，后者需显式声明依赖与加载顺序。

## 跨域业务流测试归属（三层规则）

跨域业务流（如采购审核→库存→过账）的测试归属按以下三层规则裁决：

| 层 | 归属 | 范围 | 落位模块 |
|----|------|------|----------|
| **触发域 owner 全链路** | 触发业务流的域 | 端到端完整链路（含跨域 S 写/P 的断言） | 触发域 `-service` 测试 |
| **被调域仅契约测试** | 被调用的域 | 只验证自身被调用后的行为（库存写入、凭证生成），不重复全链路 | 被调域 `-service` 测试 |
| **多触发/系统不变量** | 无单一触发方的不变量 | 跨多个触发域的系统级约束（如总账平衡、库存永续） | `app-erp-all` 集成测试 |

> **原则**：全链路测试只在**触发域**写一份，被调域不重复（避免冗余与维护负担）。例：采购入库过账全链路归 `purchase-service`，`inventory-service` 只测"被调写入 stock_move"，`finance-service` 只测"被调生成凭证"。

## 异步过账测试时序模型

> Nop 平台无内建"测试期同步化"开关（`@NopTestConfig` 无相关 flag，见 `testing.md`）。过账默认 SYNC 时测试简单（同事务断言）；切 ASYNC 后需自建时序处理。本节给出三点统一缝。

| 模式 | 测试手法 | 说明 |
|------|----------|------|
| **同步缝**（SYNC + 兜底直调） | `IErpFinPostingBiz.postNow(billType, billHeadCode)` 同事务直调 + `JunitAutoTestCase` 快照断言 | 绕过 ASYNC 时序，直接完成凭证生成。`postNow` 入口在 `posting.md §同步测试缝` 定义，测试与生产共用同一缝。SYNC 模式下 `postNow` 即默认路径。 |
| **异步轮询**（ASYNC 验证） | `@Timeout` 自旋断言 `posted` 翻转 | ASYNC 模式下，主事务提交后轮询查询 `posted=true`（带超时与指数退避），验证 post-commit 过账最终完成。 |
| **兜底直调**（不依赖时序） | `sweepJob.runOnce()` 触发兜底扫描 | 不依赖事件时序，直接跑兜底扫描重新过账 `posted=false` 的单据，验证兜底机制正确性。 |

> **推荐**：业务规则测试优先用**同步缝**（`postNow`），时序无关、最稳定。ASYNC 路径与兜底机制作为专项测试单独覆盖。

## 关键业务流快照测试清单（P0/P1/P2）

> 以下业务流必须使用 `JunitAutoTestCase` 快照测试覆盖。归属按上文"跨域业务流测试归属"规则。

| 触发模块 | 业务流 | 涉及实体数 | 优先级 |
|----------|--------|-----------|--------|
| purchase | 采购到付款全流程：PO→Receive→Invoice→Payment→Reconcile | 5+ | P0 |
| sales | 销售到收款全流程：SO→Delivery→Invoice→Receipt→Reconcile | 5+ | P0 |
| inventory | 库存移动+流水+余额一致性 | 3 | P0 |
| finance | 凭证生成+过账+红字冲销 | 3 | P0 |
| finance | 多币种过账+汇兑损益 | 3 | P1 |
| finance | 期末结账+成本核算 | 4 | P1 |
| quality | 来料检验→NCR→退货 | 4 | P1 |
| manufacturing | 工单→领料→报工→完工入库→成本结转 | 5 | P1 |
| crm | Lead→Convert→Opportunity→Quotation | 3 | P1 |
| cs | Ticket→SLA→Resolution | 3 | P1 |
| hr | 考勤→薪酬→过账 | 4 | P1 |
| aps | OperationOrder→前向/后向排产 | 2 | P2 |
| contract | InvoicePlan→AP/AR Invoice 生成 | 2 | P2 |
| drp | DRP→净需求→补货单生成 | 2 | P2 |
| logistics | Shipment→carrier dispatch→freight posting | 3 | P2 |
| b2b | EDI outbound→send→status update | 2 | P2 |
