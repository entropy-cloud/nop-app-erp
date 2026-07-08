# 待办事项

## 目的

使用此文件列出 AI 可以检查或执行的候选工作。

待办事项不是需求、owner docs 或计划的替代品。它仅有助于选择下一个切片。

## 工作项

| 优先级 | 工作项 | 路线图 | 状态 | AI 自主权 |
|--------|--------|--------|------|-----------|
| P0 | CRUD 全 18 域（Milestone 1-3） | `crud-roadmap.md` | ✅ done | `implement` |
| P1 | 核心业务循环（进销存+财务） | `core-business-roadmap.md` M1 | ✅ done | `plan-first` |
| P2 | 扩展 5 域业务逻辑 | `extended-roadmap.md` M2 | ✅ done | `plan-first` |
| P3 | 新增 8 域业务逻辑 | `extended-roadmap.md` M3 | ✅ done | `plan-first` |
| P4 | 业财一体端到端 | `core-business-roadmap.md` M4 | ✅ done | `plan-first` |
| — | 部署期演示种子数据（`_vfs/_init-data/` CSV + DataInitInitializer，解除空库阻断） | `2026-07-08-1234-1` | ✅ done | `plan-first` |
| — | 18 域 CRUD 列表/表单页面 Playwright E2E 冒烟回归套件（每域 1 代表性主单据头实体，spec 35→53） | `2026-07-08-1234-2` | ✅ done | `plan-first` |
| — | 核心业财端到端业务交易单据部署期种子（P2P+O2C 最小连通集：源单据+已过账财务产物直 seed，23 张交易 CSV；解除 1234-1 Deferred「业务交易单据种子」） | `2026-07-08-1445-1` | ✅ done | `plan-first` |
| P5 | 7 扩展域 posted/businessDate 标准字段补充（cs/hr/logistics/b2b/contract/drp/aps） | `2026-07-08-0056-1` | ✅ done | `ask-first`（ORM 保护区域，经 mission-driver 显式指令授权） |

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
