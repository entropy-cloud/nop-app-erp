# ORM `use-approval` 移除范围

> Plan: `docs/plans/2026-07-24-2200-1-cross-domain-code-abstraction.md` Phase 1
> 批准：人工（2026-07-24 会话，audit iteration 10）
> 执行日期：2026-07-25

## 范围

从 10 个 ORM 模型文件中移除 38 处 `use-approval` tagSet 标签。`useWorkflow="true"` **保留不动**（4 处：`erp_pur_payment`、`erp_sal_receipt`、`erp_ast_disposal`、`erp_hr_salary`）。

## 变更明细

| 文件 | 移除数量 |
| --- | --- |
| `module-assets/model/app-erp-assets.orm.xml` | 6 |
| `module-cs/model/app-erp-cs.orm.xml` | 1 |
| `module-finance/model/app-erp-finance.orm.xml` | 2 |
| `module-hr/model/app-erp-hr.orm.xml` | 1 |
| `module-maintenance/model/app-erp-maintenance.orm.xml` | 2 |
| `module-manufacturing/model/app-erp-manufacturing.orm.xml` | 3 |
| `module-projects/model/app-erp-projects.orm.xml` | 3 |
| `module-purchase/model/app-erp-purchase.orm.xml` | 8 |
| `module-quality/model/app-erp-quality.orm.xml` | 5 |
| `module-sales/model/app-erp-sales.orm.xml` | 7 |
| **合计** | **38** |

## 替换模式

```
tagSet="gid,erp.<domain>,use-approval"  →  tagSet="gid,erp.<domain>"
```

## 行为影响

- `_*Biz.xbiz` 不再自动继承 `/nop/wf/base/approval-support.xbiz`
- 5 个标准审批 mutation（`submitForApproval`/`approve`/`reject`/`reverseApprove`/`withdrawApproval`）不再由平台提供，改由应用层 BizModel Java `@BizMutation` 方法 + per-mutation Processor 实现
- 用户层 xbiz 中的 `<source>` 委托块在 Phase 2 完成后由 BizModel Java 接管，删除 `<source>` 块
- 状态转换 + 审计字段 + 可选 wf 启动语义在 `Abstract*Processor` 中保留（与平台 `approval-support.xbiz` 等价）
- `useWorkflow` 独立保留，继续控制 `nopFlowId` 列，wf 启动由 `AbstractSubmitForApprovalProcessor` 条件执行
