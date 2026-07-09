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
| — | 核心域（finance/sales/purchase）看板 KPI + 报表渲染数据驱动精确数值浏览器 E2E 断言（`*.value.spec.ts` 6 spec，GraphQL 取值断言 + 期望值表派生；解除 0637-1 + 1234-1 Deferred「数据驱动 KPI 精确数值断言」，spec 53→59） | `2026-07-08-1445-2` | ✅ done | `plan-first` |
| P5 | 7 扩展域 posted/businessDate 标准字段补充（cs/hr/logistics/b2b/contract/drp/aps） | `2026-07-08-0056-1` | ✅ done | `ask-first`（ORM 保护区域，经 mission-driver 显式指令授权） |
| — | 运营域（库存/资产/项目）业务交易单据 + 计算产物部署期种子（最小连通集，解除 1445-1 扩展域种子 Deferred + 为运营域数值断言提供数据基线） | `2026-07-08-2210-1` | ✅ done | `plan-first` |
| — | 运营域（库存/资产/项目）看板 KPI + 报表渲染数据驱动精确数值浏览器 E2E 断言（依赖 2210-1 种子，解除 1445-2 扩展域数值断言 Deferred） | `2026-07-08-2210-2` | ✅ done | `plan-first` |
| — | 制造域业务交易单据部署期种子（最小连通集 4 表：work_order/cost_variance/forecast/forecast_line；解除 2210-1 Deferred「其他扩展域交易种子（manufacturing）」子集；使制造域看板 4 KPI + production-variance/forecast-variance 报表数值非空；crp_load 因 workcenter 配置链依赖归 Deferred） | `2026-07-09-0930-1` | ✅ done | `plan-first` |
| — | 维护+质量域业务交易单据部署期种子（最小连通集 11 表：维护 8 表 equipment_category/equipment/schedule/request/downtime_entry/visit/visit_task/spare_part_usage + 质量 3 表 inspection/non_conformance/action；解除 2210-1 Deferred「其他扩展域交易种子（maintenance/quality）」子集；使维护+质量域看板 getDashboardKpi + 4 报表 + 3 预警数值非空；SPC 三表因 spc_chart.parameterId 配置链依赖归 Deferred） | `2026-07-09-0930-2` | ✅ done | `plan-first` |
| — | 制造/维护/质量域看板 KPI + 报表渲染数据驱动精确数值浏览器 E2E 断言（依赖 0930-1/0930-2 种子，7 `*.value.spec.ts`：3 看板 getDashboardKpi + qa getSpcOutOfControlWarning 确定性 0 + 4 报表 production-variance/forecast-variance/maintenance-history/inspection-summary；解除 2210-2 Deferred「其他扩展域看板/报表数值断言（manufacturing/quality/maintenance 子集）」） | `2026-07-09-0930-3` | ✅ done | `plan-first` |
| — | CRM/客服/人力域业务交易单据部署期种子（最小连通集 12 表 + 2 处加性追加：CRM 5 表 stage/lead/forecast_period/forecast/forecast_line + CS 3 表 ticket_type/ticket/survey + HR 4 表 department/employee/salary_simulation/simulation_item_adj + 跨域追加 md_partner EMPLOYEE 行/fin_ar_ap_item EMPLOYEE_ADVANCE·EXPENSE_CLAIM·OPEN 行；解除 2210-1 Deferred「其他扩展域交易种子（CRM/CS/HR）」子集；使三域纯报表域 5 报表 lead-conversion-funnel/forecast-accuracy/ticket-sla-csat-summary/payroll-simulation-comparison/employee-net-balance 数值非空；CRM/CS/HR 配置表 + GL 凭证归 Deferred） | `2026-07-09-1045-1` | ✅ done | `plan-first` |
| — | CRM/客服/人力域报表渲染数据驱动精确数值浏览器 E2E 断言（纯报表域无看板，5 `*.value.spec.ts`：CRM lead-conversion-funnel/forecast-accuracy + CS ticket-sla-csat + HR payroll-simulation-comparison/employee-net-balance；解除 0930-3 Deferred「其他扩展域看板/报表数值断言（CRM/CS/HR 子集）」；spec 72→77 文件） | `2026-07-09-1045-2` | ✅ done | `plan-first` |
| — | 主数据域看板 KPI + 2 报表渲染数据驱动精确数值浏览器 E2E 断言；并修复 `getDashboardKpi.vendorCount` 字典值漂移缺陷（`ErpMdDashboardBizModel` 常量 VENDOR→SUPPLIER 对齐权威字典 `erp-md/partner-type`，原致 vendorCount 永远返回 0；3 `*.value.spec.ts`：getDashboardKpi 5 字段 + 2 预警空集 + 物料价格清单/往来单位清单报表；解除 1045-2 Deferred「其他扩展域看板/报表数值断言（master-data 子集）」；完成 10 域看板 + 全报表域数值断言覆盖里程碑；spec 77→80 文件） | `2026-07-09-1145-1` | ✅ done | `plan-first` |
| — | 质量域 SPC 三表（spc_chart/spc_sample/spc_capability）最小连通种子 + `getSpcOutOfControlWarning` 数据驱动数值断言（Strategy C 完整参照完整性：spc_chart 1 行 parameterId=0 占位软引用 + spc_sample 1 行 isOutOfControl=true + spc_capability 1 行 INADEQUATE + non_conformance 追加 SPC·OPEN 行；使三计数器 outOfControlChartCount/inadequateCapabilityCount/openSpcNcrCount 由确定性 0 转非空 1/1/1；解除 0930-2 Deferred「SPC 三表 seed」+ 0930-3「确定性 0」状态；关闭质量看板最后一个预警可观测性缺口；spec 文件数不变，断言更新） | `2026-07-09-1145-2` | ✅ done | `plan-first` |
| — | 制造域工作中心配置链（workcenter/workcenter_calendar/workcenter_capacity）+ crp_load 最小连通种子，使 CRP 负荷报表（crp-load-report）由空转非空可观测并叠加数据驱动数值断言（Strategy C 完整参照：workcenter 1 行 WC-001 + 单班 08:00~16:00 ALL_WEEK calendar + efficiencyFactor=1 capacity + loadDate=2026-07-15 loadHours=4 crp_load；capacityHours=8.00 / loadRate=0.50 确定性派生；解除 0930-1 Deferred「crp_load 表 + crp-load 报表 seed」；完成全报表域数值断言覆盖里程碑 crp-load 最后一个缺口；spec 80→81） | `2026-07-09-0628-1` | ✅ done | `plan-first` |
| — | CRUD 读写两面数据驱动 E2E 验证：13 域数据驱动列表断言（`*.list-value.spec.ts` 经 GraphQL findPage 断言 seed 行数 + token）+ master-data CRUD 写路径（`master-data.write.spec.ts` create/update/delete 持久化经 mutation + 序列 warm-up 重试验证）；解除 1234-2 两 Deferred「CRUD 写操作 E2E」+「数据驱动 CRUD 列表断言」；测试数 84→99） | `2026-07-09-0628-2` | ✅ done | `plan-first` |

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
