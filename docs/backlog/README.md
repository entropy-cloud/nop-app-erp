# 待办事项

## 目的

使用此文件列出 AI 可以检查或执行的候选工作。

待办事项不是需求、owner docs 或计划的替代品。它仅有助于选择下一个切片。

## 工作项

| 优先级 | 项目 | Owner Doc | 计划 | 状态 | AI 自主权 | 阻塞项 | 最后检查 |
|--------|------|-----------|------|------|-----------|--------|----------|
| P0 | master-data 域 BizModel 深化 + 页面定制 | `docs/design/master-data/README.md` | `docs/analysis/2026-06-25-1649-ai-automation-roadmap.md` Phase 1 | `ready` | `implement` | `none` | 2026-06-25 |
| P1 | 库存域 BizModel 深化（StockMoveBiz: generateMove/confirm/cancel） | `docs/design/inventory/README.md` | roadmap Phase 2.1 | `ready` | `plan-first` | `none` | 2026-06-25 |
| P2 | 采购域 BizModel 深化（PurOrderBiz/PurReceiveBiz） | `docs/design/purchase/README.md` | roadmap Phase 2.2 | `ready` | `plan-first` | `none` | 2026-06-25 |
| P3 | 财务域 BizModel 深化（凭证引擎 AcctDocRegistry + Provider） | `docs/design/finance/README.md` | roadmap Phase 2.3 | `ready` | `plan-first` | `none` | 2026-06-25 |
| P4 | 采购→入库→凭证 端到端串联验证 | `docs/design/flow-overview.md` | roadmap Phase 2.4 | `ready` | `plan-first` | `none` | 2026-06-25 |
| P5 | 销售域 BizModel 深化（SalesOrderBiz/DeliveryBiz） | `docs/design/sales/README.md` | roadmap Phase 3 | `ready` | `implement` | `none` | 2026-06-25 |
| P6 | 制造域 BizModel 深化（WorkOrderBiz/BomBiz） | `docs/design/manufacturing/README.md` | roadmap Phase 3 | `ready` | `implement` | `none` | 2026-06-25 |
| P7 | 资产域 BizModel 深化（AssetCardBiz/DepreciationBiz） | `docs/design/assets/README.md` | roadmap Phase 3 | `ready` | `implement` | `none` | 2026-06-25 |
| P8 | 项目域 BizModel 深化（ProjectBiz/TaskBiz） | `docs/design/projects/README.md` | roadmap Phase 3 | `ready` | `implement` | `none` | 2026-06-25 |
| P9 | 质量域 BizModel 深化（InspectionBiz/NcrBiz） | `docs/design/quality/README.md` | roadmap Phase 3 | `ready` | `implement` | `none` | 2026-06-25 |
| P10 | 维护域 BizModel 深化（MaintenancePlanBiz/EquipmentBiz） | `docs/design/maintenance/README.md` | roadmap Phase 3 | `ready` | `implement` | `none` | 2026-06-25 |

## 就绪不变量

`ready` 意味着以下所有条件都为真：

- owner doc 路径存在且对此切片已知不过时
- `docs/context/project-context.md` 中的验证命令是真实的
- 无阻塞性未解决问题或明确标记为非阻塞
- 保护区域在 `docs/context/ai-autonomy-policy.md` 中配置

## 状态值

- `ready` - AI 可以根据自主权标签继续
- `in-progress` - 当前正在实施或计划中
- `blocked` - 在阻塞项解决之前无法继续
- `done` - 已完成并验证

## AI 自主权值

- `implement` - 可直接实现（设计文档充分，AI 自行决定实现细节）
- `plan-first` - 需先编写计划（跨域/业财打通等复杂场景）
- `ask-first` - 需人工确认（涉及 ORM 模型保护区域）

## 选择规则

当被要求在未命名任务的情况下继续时，选择优先级最高的 `ready` 项目。

如果表过时，请降级行或在实施前询问。
