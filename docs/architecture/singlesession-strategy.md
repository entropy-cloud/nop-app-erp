# @SingleSession 使用策略

> **总原则**：`@SingleSession` 仅用于需要保持 ORM Session 跨多次实体操作的 `@BizMutation` 方法。查询（`@BizQuery`）方法不需要。

## 适用场景

1. **批量写操作**：方法内部循环 save/update 多个实体实例时
2. **跨实体目录状态转移**：状态机转换需要加载/修改多个关联实体时
3. **Facade → Processor 多步委托**：Facade 方法依次调用 Processor 的多步方法，每步涉及实体读写时
4. **过账编排**：财务过账需要写凭证行、更新余额、记录流水在多步中保持相同 Session 时

## 不适用场景

1. **纯查询**：查询不走 Session 级联懒加载的，不需要
2. **单实体简单 CRUD**：CrudBizModel 默认 `get`/`save`/`update`/`delete` 自动管理 Session，无需额外标记
3. **@BizQuery 方法**：查询方法不应带 `@SingleSession`（本项目中也无此反模式）

## 当前使用审计

| 域 | 使用方式 | 判定 |
|----|---------|------|
| inventory | `ErpInvCostAdjustBizModel` 上 2 处多步过账 | ✅ 合理 |
| assets | CIP/Merge/Split/Inventory/Depreciation/ValueAdjustment/Maintenance 全部 Facade 方法 | ✅ 合理，每步操作多条实体 |
| projects | Settlement/CostCollection/Task/Timesheet/Project/Pnl 全部 Facade 方法 | ✅ 合理 |
| aps | OperationOrder/Schedule 状态推进多实体 | ✅ 合理 |
| contract | SignatureRequest 多步签署流程 | ✅ 合理 |
| logistics | Shipment 发运状态多实体操作 | ✅ 合理 |
| b2b | EdiDoc/Asn 多步解析+处理 | ✅ 合理 |
| finance | BadDebt/BankStatementLine/CashForecast/PostingException 过账多步 | ✅ 合理 |

**判定**：当前代码库中 `@SingleSession` 的使用均为必要场景，无防御性编程实例。无需清理。

## 审查规则

新增 `@BizMutation` 方法时：
- 如果方法内部只有**一次** `get` + `updateEntity`，**不需要** `@SingleSession`
- 如果方法内部有**两次或以上**实体操作（save/get/update 跨多个实体），且这些操作共享同一实体对象图，则**需要** `@SingleSession`
