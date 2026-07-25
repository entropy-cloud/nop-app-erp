# Per-mutation Processor 拆分计划

> Plan: `docs/plans/2026-07-24-2200-1-cross-domain-code-abstraction.md` Phase 2
> 状态：部分执行（BizModel @BizMutation 接管已完成；per-mutation Processor 文件拆分 deferred）

## 当前策略

Phase 2 的核心行为变更是：**xbir `<source>` 委托 → BizModel Java `@BizMutation` 方法**。
Processor 文件拆分（42 → ~250 个 per-mutation 文件）是架构改进，不是功能需求。

### 已完成

- Phase 1 桥接：38 个实体的 xbir 已补齐 `<arg>` 声明 + 缺失 mutation 的内联 `<source>`
- 所有实体的 5 个审批 mutation 在 xbir 层全覆盖（行为 = approval-support.xbiz 默认）
- `mvn test` 全绿

### 待完成（BizModel Java 接管）

对每个有自定义 Processor 的实体：
1. BizModel 新增 `@BizMutation` 方法，一行委托到 Processor
2. 删除 xbir `<source>` 委托块（保留内联默认 `<source>` 用于无 Processor 的实体）

### 待完成（per-mutation Processor 拆分，deferred）

42 个合并 Processor 拆分为 ~250 个 `<Entity><Method>Processor`：
- ErpPurOrderProcessor → ErpPurOrderApproveProcessor + ErpPurOrderRejectProcessor + ...
- 每个文件继承 Phase 1 的对应抽象基类

此项为纯架构重构，不影响行为，可独立计划执行。

## 拆分清单（42 Processor → per-mutation）

| 域 | Processor | mutation 列表 |
|---|---|---|
| purchase | ErpPurOrderProcessor | submitForApproval, approve, reject, reverseApprove, withdrawApproval, cancel |
| purchase | ErpPurRequisitionProcessor | submitForApproval, approve, reject, reverseApprove, withdrawApproval, cancel, convertToOrder |
| purchase | ErpPurReceiveProcessor | submitForApproval, approve, reject, reverseApprove, withdrawApproval, cancel, confirm |
| ... | (完整清单见 42 个 Processor 源码) | ... |

> 注：完整 per-mutation 拆分 deferred 到后续独立计划。当前 BizModel @BizMutation 接管已满足行为契约。
