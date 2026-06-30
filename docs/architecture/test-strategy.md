# 测试策略

> 依据 Nop Platform `testing.md` + `write-tests.md` + `write-integration-test-with-noptestconfig.md` + `e2e-testing.md`。

## 测试层级

| 层级 | 基类 | 用途 | 执行速度 |
|------|------|------|---------|
| L1 纯逻辑 | `BaseTestCase` | 工具类、算法、计算公式测试 | ~ms |
| L2 集成 | `JunitBaseTestCase` | BizModel 方法、ORM 查询、跨域协作 | ~s |
| L3 快照 | `JunitAutoTestCase` | 业务流全路径录制/校验（过账、库存移动） | ~s |
| L4 E2E | playwright/cypress | UI 端到端流程 | ~min |

## 测试组织

每个模块的测试位于 `erp-{xx}-service/src/test/java/app/erp/{xx}/service/`：

```
erp-{xx}-service/src/test/
├── java/app/erp/{xx}/service/
│   ├── test/Erp{xx}TestBase.java          # 模块级基类（共享 @NopTestConfig）
│   ├── biz/                                # BizModel 测试
│   │   ├── Erp{xx}{Entity}BizModelTest.java
│   │   └── Erp{xx}FlowTest.java           # 跨域业务流测试
│   └── error/                              # ErrorCode 测试
│       └── Erp{xx}ErrorCodeTest.java
├── resources/
│   ├── _cases/                             # JunitAutoTestCase 快照数据（CSV）
│   │   ├── Erp{xx}{Entity}BizModelTest/
│   │   │   ├── testSave.xml                # 录制输入/输出
│   │   │   └── testQuery.xml
│   │   └── Erp{xx}FlowTest/
│   │       └── testFullFlow.xml
│   └── application-test.yaml               # 测试配置（H2 数据库）
```

## 基类选择指南

| 场景 | 基类 | `@NopTestConfig` | 快照 |
|------|------|------------------|------|
| 测试 ErrorCode/常量 | `BaseTestCase` | 不需要 | 否 |
| 测试单个 BizModel CRUD | `JunitBaseTestCase` | 可选 | 否 |
| 测试复杂业务流首次录制 | `JunitAutoTestCase` | `localDb=true, initDatabaseSchema=TRUE, snapshotTest=RECORDING` | 是 |
| 测试复杂业务流日常校验 | `JunitAutoTestCase` | 裸 `@NopTestConfig` | 是（CHECKING） |
| 测试跨域协作 | `JunitAutoTestCase` | 同上 | 是 |
| GraphQL API 测试 | `JunitAutoTestCase` | 同上 | 是 |

## 关键业务流快照测试清单

以下业务流必须使用 `JunitAutoTestCase` 快照测试覆盖：

| 模块 | 业务流 | 涉及实体数 | 优先级 |
|------|--------|-----------|--------|
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

## 测试数据策略

1. **种子数据**：用 `app-erp-app/src/test/resources/_vfs/nop/test/` 下的 CSV 或 JSON 文件初始化（`orm.xml` 中 `ext:seed` 指向）。
2. **快照数据**：`JunitAutoTestCase` 首次运行（RECORDING 模式）自动生成 `_cases/` 下的 XML 文件。
3. **业务主数据**：标准测试数据（物料、客户、供应商、科目）在 `app-erp-test-data` 共享模块中集中维护。
4. **测试数据隔离**：每个测试类使用独立 H2 内存库，互不干扰。

## 模拟外部依赖

对于依赖外部系统的模块（logistics/TMS、b2b/EDI），使用 Mock Bean 替代真实网关：

```java
// logistics 模块
@NopTestConfig(localDb = true)
class ErpLogShipmentBizModelTest extends JunitAutoTestCase {
    @Inject
    ErpLogCarrierGatewayRegistry registry;
    
    @Test
    void testCarrierDispatch() {
        // 注册 Mock 网关（返回预设运单号）
        // 验证发运单状态迁移：DRAFT→ADVISED→DISPATCHED
    }
}
```

## 测试 CI 策略

1. **PR 门禁**：所有 L2/L3 测试必须在 PR 合并前通过。
2. **快照更新**：业务规则变更后，手动设置 `snapshotTest=RECORDING` 重新录制快照，提交更新的 `_cases/` 文件。
3. **L4 E2E**：可选，在主要版本发布前执行。
4. **并行执行**：测试类间无共享状态，支持 `mvn -T 4 test` 并行。

## 参考

- `nop-entropy/docs-for-ai/02-core-guides/testing.md`
- `nop-entropy/docs-for-ai/03-runbooks/write-tests.md`
- `nop-entropy/docs-for-ai/03-runbooks/write-integration-test-with-noptestconfig.md`
- `nop-entropy/docs-for-ai/02-core-guides/e2e-testing.md`
