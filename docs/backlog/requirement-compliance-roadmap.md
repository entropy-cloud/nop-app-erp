# 需求-实现符合性审计路线图（骨架）

> 最后更新：2026-08-02（v1.3 — Q1/Q4 已裁决：Q1=(c) 逐项对照需求真相源优先 / Q4=(a) P0/P1 必须实现禁止方案 B 无例外；M0 转 ready，解除执行阻塞。v1.2 经两轮独立子代理审查修订：MA1 完整枚举 51 切片；MA2 导出口径+分区约束；MR0/MR1 改执行时追加实体行；A4 展开器范式对齐 R1.0；Q1/Q4 硬门控；保护区域暂停协议+预授权声明；MA1↔既有审计去重协议；MA3 三源对账）
> 来源：`docs/skills/audit-remediation-roadmap-authoring-prompt.md` 同型模式（需求-实现符合性变体）
> 范围文档：`docs/discussions/2026-08-02-1700-requirement-implementation-compliance-audit.md`
> **状态：可执行版本。Q1/Q4 已裁决（2026-08-02），M0 工作项转 ready，可启动 mission driver。**

## 目的

本路线图覆盖 nop-app-erp（19 域、156 模块）的**需求→实现符合性审计**：从需求真相源（product-scope + use-cases + owner doc 需求契约）出发，逐模块逐功能点核对运行时行为是否符合需求，识别并修复被"文档化简化/Deferred"合法化掩盖的需求-实现分歧。引用 `docs/backlog/00-roadmap-authoring-guide.md` 作为规范。**不复跑 MA1-MA7 架构漂移类审计**（详见讨论文档 §根因分析）。

> **预授权声明**（对齐 audit-remediation "ORM 变更已授权" 范式；Q4 已裁决=(a) 无例外，本声明为预授权类目清单）：
> - 文档更新类修复（owner doc / use-cases / arm-index）：预授权自动执行
> - 代码逻辑修复（BizModel / Processor / xbiz / view.xml）：预授权自动执行
> - ORM 结构变更（orm.xml 字段/索引/UK/实体）：**须 ask-first** + 独立 plan-audit
> - 会计过账逻辑变更（VoucherFact / PostingProcessor 核心路径）：**须 ask-first** + 独立 plan-audit
> - 数据删除 / 数据迁移：**须 ask-first** + 独立 plan-audit
> - 未列明的修复类目默认须 ask-first

## Work Item Status

> 唯一的动态状态块。状态：`todo` / `ready` / `done`。初始全 `todo`。

### Milestone M0 — 审计编排基线（方法论文档先行）

| # | Work Item | Status | Owner Doc | Deps | Skill |
|---|-----------|--------|-----------|------|-------|
| 0.1 | 需求-实现符合性审计方法论文档（五级追踪矩阵模板 + 判据 + **完整枚举纪律（禁止抽样）** + Q1 裁决的真相源层级（product-scope + use-cases 权威 > owner doc 参考，冲突一律以需求真相源为准）+ Q4 裁决的修复义务（P0/P1 必须实现禁止方案 B 无例外）+ 输出格式，落盘 `docs/audits/requirement-compliance-methodology.md`，独立子代理 ≥2 轮审查收敛） | done ✅ | 讨论文档 `2026-08-02-1700`（Q1/Q4 已裁决） | — | 参考 MQ 文档先行范式 |
| 0.2 | 需求基线提取与清单化（product-scope 修正陈旧段 + 18 域 192 UC 清单化 + **notify 补写完整 use-cases**[Q1 裁决：不标注 N/A，notify 是已实现子系统必须补需求契约] + **按功能切片拆分 51 个 A1.x 工作项**[notify 补写后可能新增切片行] + 五级追踪矩阵初始化） | done ✅ | `docs/requirements/product-scope.md` + 各域 `use-cases.md` | 0.1 | `docs/skills/design-completeness-scan-prompt.md` |
| 0.3 | 存量清单导出（arm-index documented simplification / Deferred / successor 全量清单 + **按域枚举 MA2 复查行** + 复杂度分级 + 优先级排序） | done ✅ | `docs/audits/arm-index.md` | 0.1 | 参考 `docs/audits/scripts/` |

### Milestone MA1 — 需求追踪矩阵审计（逐域逐功能切片）

> **完整枚举原则（本里程碑核心纪律）**：每个工作项 = 一个**功能切片 × 显式 UC 清单**，一份计划即可完整实施，全表即完整工作量（18 域 192 UC 统计覆盖无遗漏，拆为 50 个 UC 切片 + 1 个 notify 切片 = 51 行；logistics 04/05/06 为非标准 heading，0.2 归一化后复校）。**禁止合并切片抽样**；完成判据 = 本表全部行 `done`，任何"代表性抽样"即视为未完成。

> **与既有审计逐维度去重（MA1 ↔ audit-remediation MA2/MA3/MA4/MA5）**：新 MA1 的"运行时行为证据"维度与既有 MA2 状态机/业财链路行为审查**对象重叠**——去重协议：① 既有 MA2 报告（`docs/audits/2026-07-2*-arm-ma2-*`）已证实的状态机迁移/链路行为**直接作为既有证据输入**，MA1 只补"需求契约↔实际行为"差异（use-case 验收标准视角），不重新核实行为本身；② 每切片报告开头声明"与 MA2 报告的差异增量"；③ 新 finding 按 §MA1 详情"复用 or 新增"规则衔接 arm-index；④ MA4 与 A5.6（E2E 断言强度审计）边界见横切关注点 #3。**不复跑 MA1-MA7**：架构/平台一致性/文档漂移/代码质量/测试覆盖/保护区域纪律/审计面各维度均以 audit-remediation 收口为准。

| # | Work Item（功能切片 + UC 清单） | Status | Owner Doc | Deps | Skill |
|---|--------------------------------|--------|-----------|------|-------|
| A1.1 | **finance-F1 过账引擎与凭证链路**（UC-FIN-01/02/03/04/12/15） | done | `docs/design/finance/use-cases.md` + `posting.md` | 0.2 | `docs/skills/multi-dimensional-audit-prompt.md` |
| A1.2 | **finance-F2 预算与承付**（UC-FIN-11/13） | done | `docs/design/finance/budget.md` | 0.2 | 同上 |
| A1.3 | **finance-F3 AR/AP 核销与坏账**（UC-FIN-08） | done | `docs/design/finance/ar-ap-reconciliation.md` | 0.2 | 同上 |
| A1.4 | **finance-F4 银行对账**（UC-FIN-09/14 标题重复[同为"银行对账与未达账项"]，0.2 清单化时裁决去重） | todo | `docs/design/finance/bank-reconciliation.md` | 0.2 | 同上 |
| A1.5 | **finance-F5 成本核算**（UC-FIN-10） | done | `docs/design/finance/costing-methods.md` | 0.2 | 同上 |
| A1.6 | **finance-F6 期间与结账**（UC-FIN-06/07） | done | `docs/design/finance/period-close.md` | 0.2 | 同上 |
| A1.7 | **finance-F7 报表/看板/多账套**（UC-FIN-05/16/17） | done | `docs/design/finance/` + `docs/design/dashboards.md`（全局） | 0.2 | 同上 |
| A1.8 | **mfg-F1 MRP/DRP 引擎**（UC-MFG-05/08） | done | `docs/design/manufacturing/mrp.md` | 0.2 | 同上 |
| A1.9 | **mfg-F2 工单与报工**（UC-MFG-01/03/04/06/07/09） | done | `docs/design/manufacturing/` | 0.2 | 同上 |
| A1.10 | **mfg-F3 BOM 与工艺路线**（UC-MFG-02/10） | todo | `docs/design/manufacturing/` | 0.2 | 同上 |
| A1.11 | **mfg-F4 差异/批次/看板**（UC-MFG-11/12/13） | todo | `docs/design/manufacturing/variance-analysis.md` | 0.2 | 同上 |
| A1.12 | **hr-F1 员工与组织**（UC-HR-01/05/07/08/12） | todo | `docs/design/human-resource/` | 0.2 | 同上 |
| A1.13 | **hr-F2 排班与考勤**（UC-HR-02/06/09） | todo | `docs/design/human-resource/` | 0.2 | 同上 |
| A1.14 | **hr-F3 薪酬与调研**（UC-HR-03/04/10/11） | todo | `docs/design/human-resource/payroll.md` | 0.2 | 同上 |
| A1.15 | **purchase-F1 主流程与请购**（UC-PUR-01/08） | todo | `docs/design/purchase/` | 0.2 | 同上 |
| A1.16 | **purchase-F2 三单匹配与差异**（UC-PUR-02/03/05/06） | todo | `docs/design/purchase/three-way-match.md` | 0.2 | 同上 |
| A1.17 | **purchase-F3 退货与业财**（UC-PUR-04/07） | todo | `docs/design/purchase/returns.md` | 0.2 | 同上 |
| A1.18 | **sales-F1 主流程与价格**（UC-SAL-01/11） | todo | `docs/design/sales/` | 0.2 | 同上 |
| A1.19 | **sales-F2 出库与并发**（UC-SAL-02/03/10） | todo | `docs/design/sales/` | 0.2 | 同上 |
| A1.20 | **sales-F3 退货族**（UC-SAL-04/05/06/07/09） | todo | `docs/design/sales/returns.md` | 0.2 | 同上 |
| A1.21 | **sales-F4 赠品与看板**（UC-SAL-08/12） | todo | `docs/design/sales/` + `docs/design/dashboards.md`（全局） | 0.2 | 同上 |
| A1.22 | **assets-F1 折旧引擎**（UC-AST-02/07/08） | todo | `docs/design/assets/` | 0.2 | 同上 |
| A1.23 | **assets-F2 处置**（UC-AST-04/05） | todo | `docs/design/assets/` | 0.2 | 同上 |
| A1.24 | **assets-F3 资本化/拆分/盘点/维修/看板**（UC-AST-01/03/06/09/10/11/12） | todo | `docs/design/assets/` | 0.2 | 同上 |
| A1.25 | **inventory-F1 移动单主链与追溯**（UC-INV-01/03/04/05） | todo | `docs/design/inventory/` | 0.2 | 同上 |
| A1.26 | **inventory-F2 批次与可用量**（UC-INV-02/06/09） | todo | `docs/design/inventory/` | 0.2 | 同上 |
| A1.27 | **inventory-F3 盘点/估值/并发/看板**（UC-INV-07/08/10/11） | todo | `docs/design/inventory/` | 0.2 | 同上 |
| A1.28 | **crm-F1 线索生命周期**（UC-CRM-01/02/03/04/09/11） | todo | `docs/design/crm/` | 0.2 | 同上 |
| A1.29 | **crm-F2 营销/预测/配额/序列/事件提醒**（UC-CRM-05/07/08/10/12/14/15） | todo | `docs/design/crm/` | 0.2 | 同上 |
| A1.30 | **crm-F3 CPQ/漏斗推进**（UC-CRM-06/13） | todo | `docs/design/crm/` | 0.2 | 同上 |
| A1.31 | **quality-F1 检验门控**（UC-QA-01/02/03/04/06/07/08） | todo | `docs/design/quality/` | 0.2 | 同上 |
| A1.32 | **quality-F2 NCR-CAPA 闭环**（UC-QA-05） | todo | `docs/design/quality/` | 0.2 | 同上 |
| A1.33 | **quality-F3 SPC 与看板**（UC-QA-09/10/11/12） | todo | `docs/design/quality/` | 0.2 | 同上 |
| A1.34 | **projects-F1 立项与成本归集**（UC-PRJ-01/02/03/09） | todo | `docs/design/projects/` | 0.2 | 同上 |
| A1.35 | **projects-F2 预算与 DAG**（UC-PRJ-04/05） | todo | `docs/design/projects/` | 0.2 | 同上 |
| A1.36 | **projects-F3 结算与看板**（UC-PRJ-06/07/08/10） | todo | `docs/design/projects/` | 0.2 | 同上 |
| A1.37 | **cs-F1 工单生命周期**（UC-CS-01/02/03/11） | todo | `docs/design/customer-service/` | 0.2 | 同上 |
| A1.38 | **cs-F2 SLA 与升级**（UC-CS-04） | todo | `docs/design/customer-service/` | 0.2 | 同上 |
| A1.39 | **cs-F3 知识库/质量联动/预设应答**（UC-CS-05/06/07） | todo | `docs/design/customer-service/` | 0.2 | 同上 |
| A1.40 | **cs-F4 调查/权益/目录/履行**（UC-CS-08/09/10/12） | todo | `docs/design/customer-service/` | 0.2 | 同上 |
| A1.41 | **master-data 全功能**（UC-MD-01~07，7 UC） | todo | `docs/design/master-data/` | 0.2 | 同上 |
| A1.42 | **maintenance-F1 调度与冲突**（UC-MAIN-01/02/09） | todo | `docs/design/maintenance/` | 0.2 | 同上 |
| A1.43 | **maintenance-F2 访问与备件**（UC-MAIN-03/04） | todo | `docs/design/maintenance/` | 0.2 | 同上 |
| A1.44 | **maintenance-F3 响应/联动/OEE/看板**（UC-MAIN-05/06/07/08/10/11） | todo | `docs/design/maintenance/` | 0.2 | 同上 |
| A1.45 | **contract-F1 生命周期与签署**（UC-CT-01/02/05/06/07/09） | todo | `docs/design/contract/` | 0.2 | 同上 |
| A1.46 | **contract-F2 计费与返利**（UC-CT-03/04/08/10） | todo | `docs/design/contract/` | 0.2 | 同上 |
| A1.47 | **b2b 全功能**（UC-B2B-001~008，8 UC） | todo | `docs/design/b2b/` | 0.2 | 同上 |
| A1.48 | **drp 全功能**（UC-DRP-01~08，8 UC） | todo | `docs/design/drp/` | 0.2 | 同上 |
| A1.49 | **logistics 全功能**（UC-LOG-01~07，7 UC；04/05/06 在 use-cases.md 用非标准 heading `## 用例X：…`，0.2 清单化时归一化） | todo | `docs/design/logistics/` | 0.2 | 同上 |
| A1.50 | **aps 全功能**（UC-APS-01~07，7 UC） | todo | `docs/design/aps/` | 0.2 | 同上 |
| A1.51 | **notify 通知派发**（无 use-cases 文件，0.2 裁决后按功能点核验） | todo | `docs/design/notify/` | 0.2 | 同上 |

### Milestone MA2 — 已裁决简化/Deferred 复查

> **完整枚举原则（同 MA1）**：arm-index 全部方案 B（documented simplification / Deferred 标注）关闭项逐项复核：与 product-scope 对照，区分"有意设计"vs"静默降级"，重新分级。**0.3 导出口径（关键）**：按 arm-index 各行 resolved 注记中的**关闭方式标签**（方案 B / documented simplification / Deferred 标注）筛行，**排除 `resolved (R*.n done)` 类实现修复项**；示例 ID 仅作锚点，精确清单以 0.3 导出为准。**分区约束：每个 finding ID 恰好归属一个 A2.x 行**（跨域项按主域归属并显式标注，0.3 导出后脚本校验分区不重叠）。判据：与 product-scope 冲突 / 影响报表·过账正确性 / 无显式人工批准记录（证据标准见 Work Item Details）→ 重新打开并入 MR1。

| # | Work Item（域 × 方案 B 关闭项全集） | Status | Owner Doc | Deps | Skill |
|---|--------------------------------|--------|-----------|------|-------|
| A2.1 | finance 会计保护区域简化复查（锚点：GRNI 冲回 P1-MA2-001 / 年初余额 P1-MA2-018 / 辅助账对账 P1-MA2-019 / 反结账 P1-MA2-020 / FX 重估 P1-MA2-022；其余以 0.3 按标签导出） | todo | `docs/design/finance/` + `arm-index.md` | 0.2 + 0.3 | `docs/skills/open-ended-audit-prompt.md` |
| A2.2 | finance 非保护区域简化复查（0.3 按标签导出；示例非方案 B 项如 P1-MA2-095~099 系实现修复，不属本行） | todo | `docs/design/finance/` + `arm-index.md` | 0.2 + 0.3 | 同上 |
| A2.3 | mfg 简化复查（锚点：作业卡 P1-MA2-035 / MRP·预测 P1-MA2-036 / 建议单释放 P1-MA2-037；其余 0.3 导出） | todo | `docs/design/manufacturing/` + `arm-index.md` | 0.2 + 0.3 | 同上 |
| A2.4 | hr 简化复查（锚点：员工/合同/调查/发展计划/工时/银行文件/排班/薪酬 P1-MA2-039~048 族；0.3 复核关闭方式标签后筛行） | todo | `docs/design/human-resource/` + `arm-index.md` | 0.2 + 0.3 | 同上 |
| A2.5 | purchase + sales 简化复查（锚点：采购 P1-MA2-049/050/051 / 销售 P1-MA2-056/057；P1-MA2-001 GRNI 冲回归 A2.1 finance 保护区域[会计过账本质]，082/083 承付族跨域项 0.3 按主域分区） | todo | `docs/design/purchase/`+`sales/` + `arm-index.md` | 0.2 + 0.3 | 同上 |
| A2.6 | assets + inventory 简化复查（锚点：资产状态机 P1-MA2-058~061 / 盘点 P1-MA2-062 / 拣货 P1-MA2-063；P1-MA2-024/085 系实现修复、089 系并发实现修复，0.3 复核排除） | todo | `docs/design/assets/`+`inventory/` + `arm-index.md` | 0.2 + 0.3 | 同上 |
| A2.7 | projects + quality 简化复查（锚点：NCR/质检 P1-MA2-064~066 / 项目 P1-MA2-067~070；0.3 复核关闭方式标签） | todo | `docs/design/projects/`+`quality/` + `arm-index.md` | 0.2 + 0.3 | 同上 |
| A2.8 | 扩展域简化复查（锚点：contract P1-MA2-071/072 / b2b P1-MA2-073 / maintenance P1-MA2-074 / crm P1-MA2-075/076 / aps+logistics P1-MA2-077~080；0.3 复核标签） | todo | 各域 owner doc + `arm-index.md` | 0.2 + 0.3 | 同上 |
| A2.9 | 跨域简化复查（0.3 按标签导出跨域项；P1-MA2-086/088/089 系并发实现修复，不属本行） | todo | `docs/design/` 各域 + `arm-index.md` | 0.2 + 0.3 | 同上 |

### Milestone MA3 — successor 追踪完整性与回队复查

> **完整枚举原则（同 MA1）**：0.3 **三源对账导出**（arm-index 行内 successor 声明 + owner doc 内嵌声明 + `docs/backlog/README.md` 既有 81 行追踪）后，**按域分组逐项核对**（部分声明可能已 done，需对账消歧）：① 触发条件是否已满足（已满足 → 回队 MA1/R1.0）；② 是否该回队（回到审计、R1.0 修复或 backlog README）；③ 无触发条件的补登记；④ backlog/README 既有行的覆盖与正确性复核（防"已登记但从未触发"）。每行 = 一份计划，覆盖该行全部 successor 项，禁止抽样。

| # | Work Item（域 × successor 清单） | Status | Owner Doc | Deps | Skill |
|---|--------------------------------|--------|-----------|------|-------|
| A3.1 | finance 域 successor 复查（如 GL 余额引擎 / 多账套 UK 升级 / 冲销恢复承付 / GRNI 自动冲回触发条件等） | todo | `docs/audits/arm-index.md` + `docs/backlog/README.md` | 0.3 | `docs/skills/open-ended-audit-prompt.md` |
| A3.2 | mfg + inventory + purchase 域 successor 复查（如物料预留实现 / STANDARD 红冲 / 拣货 WMS / 盘点自动移动单等） | todo | `docs/audits/arm-index.md` + `docs/backlog/README.md` | 0.3 | 同上 |
| A3.3 | sales + assets + projects + quality 域 successor 复查（如订单维度核销 / 资产转固 / employee-id 行过滤等） | todo | `docs/audits/arm-index.md` + `docs/backlog/README.md` | 0.3 | 同上 |
| A3.4 | hr + crm + cs 域 successor 复查（如 recruitment 多实体 / 滞留升级 / 序列推进等） | todo | `docs/audits/arm-index.md` + `docs/backlog/README.md` | 0.3 | 同上 |
| A3.5 | 扩展域 + 跨域 successor 复查（contract/b2b/logistics/drp/aps/maintenance/notify + 多公司/数据权限/SoD 铺开等） | todo | `docs/audits/arm-index.md` + `docs/backlog/README.md` | 0.3 | 同上 |

### Milestone MA4 — 运行时行为验证

> **完整枚举原则（同 MA1）**：对 MA1 发现的**全部**"静态存疑点"做运行时行为确认，输出证据链。**MA1 每份报告须列明存疑点清单**。A4.1/A4.2 为**展开器工作项**（对齐 R1.0 范式，非容器行/非预注册固定范围）：读取 MA1 报告存疑点清单 → 向本表追加 A4.1.n/A4.2.n 实体验证行（每存疑点一行）→ **展开完成（全部实体行已追加）后 A4.1/A4.2 标记 done**；后续 A4.1.n/A4.2.n 各自独立 plan + 独立 done。**无存疑点的切片不产生实体行**（此时展开器直接 done）。既有证据复用：MA2 已证实的状态机/链路行为（`docs/audits/2026-07-2*-arm-ma2-*` 报告）作为既有证据输入，MA4 只补"需求↔行为"差异，不重复验证。

| # | Work Item | Status | Owner Doc | Deps | Skill |
|---|-----------|--------|-----------|------|-------|
| A4.1 | 业财域 MA1 存疑点运行时确认**展开器**（读取 MA1 业财切片报告全部存疑点清单，展开为本表 A4.1.n 实体验证行） | todo | `docs/design/flow-overview.md` + `tests/e2e/` | MA1 done | `docs/skills/multi-dimensional-audit-prompt.md` |
| A4.2 | 扩展域 MA1 存疑点运行时确认**展开器**（读取 MA1 扩展域切片报告全部存疑点清单，展开为本表 A4.2.n 实体验证行） | todo | 各域 owner doc | MA1 done | 同上 |

### Milestone MR0 — P0 即时修复通道（发现即止血）

> **审计→修复闭环的第一环**：MA1-MA4 任何工作项发现 P0（活跃数据破坏/会计错误/安全漏洞）时，**不等整个里程碑完成**，立即经独立 fix plan 注入修复（对齐 audit-remediation P0-MA2-016/017/019/020 即时通道先例：异步独立 plan + 独立 plan-audit + ask-first 保护区域门控）。修复后回写 MA 报告状态。
>
> **执行机制**：本里程碑**不预注册固定行**（避免"R*.x 占位符卡死"反模式，对齐 audit-remediation 修法）。MA 工作项执行中发现 P0 时，**向本里程碑表内追加实体行**（编号 R0.1, R0.2…，含 finding ID/域/修复范围/Skill/触及保护区域标注），mission driver 按追加行正常执行；P0 行与 MA 行可并行（不阻塞后续 MA 工作项）。保护区域 P0 行仍须 ask-first + 独立 plan-audit（见横切关注点 #5 与 M0.1 保护区域暂停协议）。

### Milestone MR1 — P0/P1 修复（需求符合性，批量自动修复）

> **判据（Q4 已裁决=(a)，正式生效，无例外）**：P0/P1 需求分歧**必须实现**，**禁止用方案 B 关闭**；**无"技术不可行"例外通道**——技术不可行项（如 P0-MA2-018）须更深设计变更（如重构 billR 加判别列 + 对应 UK）；唯一出口 = 审计发现需求本身不合理时经人工批准修改 product-scope（需求变更非降级）；会计/数据安全类强制实现无例外（对齐 `ai-autonomy-policy.md`）；P2 登记不强制。R1.0 为展开器工作项（仿 audit-remediation 范式）；RC-R1.n 由 R1.0 按审计发现**自动展开**，mission driver 逐项 DRAFT_PLANS → 独立草案审查 → EXECUTE → 独立结束审计 → 写回 done，无需人工介入每个修复项（触及保护区域者除外，见横切关注点 #5）。
>
> **执行机制**：本里程碑**不预注册 RC-R1.n 占位行**。R1.0 展开时向本里程碑表内追加实体行（编号 RC-R1.1, RC-R1.2…，前缀 RC 避免与 audit-remediation 的 R1.x 系列混淆；每行含 finding ID 交叉引用/域/修复范围/触及保护区域标注/Skill），mission driver 按追加行正常执行。完成判据 = 本表全部实体行 done。

| # | Work Item | Status | Owner Doc | Deps | Skill |
|---|-----------|--------|-----------|------|-------|
| R1.0 | MA1-MA4 P0/P1 需求分歧汇总、排序并展开为具体修复工作项行（对每个分歧裁决"修复"或"登记"；触及保护区域行标注） | todo | MA1-MA4 报告 | MA1+MA2+MA3+MA4 done | none |

### Milestone MV — 全量验证与跨维度一致性回归

| # | Work Item | Status | Owner Doc | Deps | Skill |
|---|-----------|--------|-----------|------|-------|
| V.1 | 全量 `mvn clean install -DskipTests` + `mvn test` 绿色验证 | todo | — | MR1 done | none |
| V.2 | compliance checker 基线对比（不得高于既有基线） | todo | `docs/audits/compliance-baseline.md` | V.1 | compliance-checker |
| V.3 | 独立子代理 closure audit（全部 P0 + 关键 P1 修复） | todo | MA1-MA4 报告 + MR1 修复 | V.1-V.2 | `docs/skills/closure-audit-prompt.md` |

### Milestone MG — 知识沉淀与守卫强化

| # | Work Item | Status | Owner Doc | Deps | Skill |
|---|-----------|--------|-----------|------|-------|
| G.1 | 新失败模式提升为 docs/lessons/（文档化简化滥用 / 需求基线陈旧等） | todo | `docs/lessons/` | MV done | none |
| G.2 | 审计方法提升为 docs/skills/（需求符合性审计 prompt） | todo | `docs/skills/` | MV done | none |
| G.3 | 更新 project-context.md + README.md 已知失败模式 | todo | `docs/context/project-context.md` | MV done | none |

## 框架/平台复用

| 能力 | 提供方式 |
|------|----------|
| 多维审计 prompt | `docs/skills/multi-dimensional-audit-prompt.md` / `open-ended-audit-prompt.md` / `closure-audit-prompt.md` |
| 需求完整性扫描 | `docs/skills/design-completeness-scan-prompt.md` + `tools/use-case-map.cjs` |
| Compliance checker（19 规则） | `docs/audits/nop-compliance-checker.sh` + CI `.github/workflows/compliance.yml` |
| 存量 finding 索引 | `docs/audits/arm-index.md`（292 条可追溯） |
| 测试基础设施 | JUnit（1900+ 测试）+ Playwright E2E（260+ spec）+ 故障注入 harness（module-common-test） |

## 当前基线

- **验证基线**：`mvn clean install -DskipTests` 全绿（156 模块）；`mvn test` 全绿（1903 测试）
- **需求基线**：`docs/requirements/product-scope.md`（0.2 已做残余事实性核实——成功指标计数 1902→1903 校正，里程碑框架由 R2.2 修复）+ 18 域 `use-cases.md`（192 UC）+ notify `use-cases.md`（0.2 补写 7 UC-SYS，UC 总数 199，拆为 50 个 UC 切片 + 1 notify 切片 = 51 行）；UC 权威清单 + 五级矩阵骨架见 `docs/audits/rc-requirement-baseline-inventory.md`
- **存量分歧**：arm-index 方案 B 关闭项（documented simplification / Deferred）+ 41 successor 声明（待 0.3 三源对账导出精确清单）
- **P0 deferred 边界声明**：既有 arm-index P0 deferred 项（如 P0-MA2-018 凭证幂等键，字面 UK 经 plan-audit 裁定不可实施）**不属本审计自动重开范围**；仅当 MA2 复查将其重新分级时进入 MR1。本 MR1 判据"P0 强制实现"指**本审计新发现**的 P0。
- **已知过程失败模式**：compliance 基线漂移复发、closure-audit 独立性、方案 B 滥用（见 `project-context.md §已知失败模式`）

## 依赖图

```mermaid
flowchart TD
    Q[讨论 Q1/Q4 裁决] --> M01[0.1 方法论文档]
    M01 --> M02[0.2 需求基线]
    M01 --> M03[0.3 存量清单]
    M02 --> MA1[MA1 逐域审计]
    M02 --> MA2[MA2 简化复查]
    M03 --> MA2[MA2 简化复查]
    M03 --> MA3[MA3 successor 复查]
    MA1 --> MA4[MA4 运行时验证]
    MA1 --> R0[MR0 P0 即时止血]
    MA2 --> R0
    MA3 --> R0
    MA4 --> R0
    MA1 --> MR1[MR1 批量自动修复]
    MA2 --> MR1[MR1 批量自动修复]
    MA3 --> MR1[MR1 批量自动修复]
    MA4 --> MR1[MR1 批量自动修复]
    R0 --> MR1
    MR1 --> MV[MV 全量验证]
    MV --> MG[MG 知识沉淀]
```

## 范围裁决（讨论文档候选审计内容 5-8 的归属/排除）

讨论文档 `2026-08-02-1700` §候选审计内容表的 8 项与本 roadmap 的映射如下——**执行期范围以此清单为准，候选表不构成独立范围**：

| 候选 # | 内容 | 归属/排除 |
|---|---|---|
| 1 | 需求追踪矩阵 | → MA1（全部 51 切片） |
| 2 | 已裁决简化复查 | → MA2（A2.1-A2.9） |
| 3 | successor 触发条件复查 | → MA3（A3.1-A3.5） |
| 4 | 运行时行为验证 | → MA4（A4.1/A4.2，按存疑点展开） |
| 5 | 跨域契约行为一致性 | **并入 MA1 五级追踪的行为层**（每个切片含跨域 Facade 调用链核查）；不设独立里程碑 |
| 6 | Nop 最佳实践回归复核 | **仅查新引入回归**（MA1 报告含"新引入平台纪律问题"检查项）；存量以 audit-remediation 收口为准，不重审 |
| 7 | 保护区域行为复核 | **并入 MA4 A4.1**（业财核心链路运行时验证覆盖会计过账/删除/ORM 行为守卫）；不设独立里程碑 |
| 8 | 审计过程自身纪律 | **并入 M0.1 方法论文档"过程纪律自检段" + MV V.3 closure audit 核查项**（checker 退出码门控、closure-audit 独立性声明）；不设独立里程碑（避免本已庞大的审计再叠一层审计） |

## 审计→修复闭环执行机制

本路线图由 mission driver（`./tools/mission-driver.sh run requirement-compliance`）自主驱动，**审计与修复在同一闭环内自动衔接**：

1. **逐项执行**：MA1-MA4 每个审计工作项由 mission driver DRAFT_PLANS → 独立草案审查 → EXECUTE → 独立结束审计 → 写回 `done`。审计报告落盘 `docs/audits/`，发现按 P0/P1/P2/接受 分级。
2. **P0 即时就近修复**：任何审计项发现 P0 → 立即经 MR0 通道创建独立 fix plan（不等里程碑完成，向 MR0 表追加 R0.n 实体行），修复后回写报告状态。
3. **P1 批量自动修复**：MA1-MA4 完成后，R1.0 展开器自动把全部 P0/P1 分歧汇总为 RC-R1.n 修复行（含 finding 交叉引用）→ mission driver 继续逐行执行修复（DRAFT_PLANS → 审查 → EXECUTE → closure audit → done），**无需人工逐个介入**（触及保护区域者按暂停协议暂停等待人工批准）。
4. **修复义务**：P0/P1 强制实现（Q4 已裁决=(a)，**禁止方案 B，无例外通道**）；唯一出口 = 需求本身不合理时经人工批准修改 product-scope（需求变更非降级）。
5. **验证收口**：MR1 全 done → MV 全量验证 + 独立 closure audit（cold-context 子代理）→ MG 知识沉淀。全绿基线记入 git commit message。

## 横切关注点

1. **文档先行**：M0 方法论文档经独立子代理 ≥2 轮审查收敛后，才展开 MA1-MA4（对齐 MQ Q0-Q7 范式）。
2. **禁止方案 B 关闭 P0/P1（Q4 已裁决=(a)，正式判据）**：审计发现的 P0/P1 需求分歧**必须实现**，**禁止用方案 B（documented simplification / Deferred）关闭**；**无"技术不可行"例外通道**——技术不可行项须更深设计变更而非退缩到方案 B。唯一出口：审计发现需求本身不合理时，经人工批准修改 product-scope（需求变更非降级）。会计/数据安全类强制实现无例外。P2 登记不强制。
3. **与既有审计去重**：不复跑 MA1-MA7；**Q5 已定方向：本审计审"行为是否符合需求"，与 A5.6 审"E2E 断言强度"边界按此执行（Q5 非阻塞，MA4 不设门控）**；MA1 与既有 MA2 状态机/链路审计的逐维度去重见 §MA1 去重表。
4. **审计证据可追溯**：每工作项产出报告落盘 `docs/audits/`，finding 新建 **P1-RC-xxx 系列**（命名规则见 Work Item Details MA1 节；RC = Requirement Compliance）并回填 arm-index；修复行（R0.n/RC-R1.n）与 finding 双向可追溯（对齐 MV V.3 内置可追溯校验）。
5. **保护区域**：涉及 ORM/会计/删除的修复沿用 ask-first + 独立 plan-audit（P0 即时通道也不豁免）；**无人值守 driver 下按 M0.1 方法论文档"保护区域暂停协议"执行**（触及行暂停等待人工批准，非触及行继续）。
6. **修复质量保障**：每修复项独立 plan + 独立结束审计；修复后分域 `mvn test` + 全量构建 + compliance checker 对比基线。

## 规则

1. 保持粗粒度；Work Item Details 是简短列表，不是实现步骤（引用 `00-roadmap-authoring-guide.md`）。
2. 状态只存在于工作项上；里程碑无状态。
3. 独立草案审查通过后 `todo` → `ready`；独立结束审计通过后 `ready` → `done`。
4. 工作项大于单次交付范围时必须拆分。
5. 本骨架在 Q1/Q4 裁决 + 独立子代理审查后升版为可执行版本。
6. **执行门控（已满足——Q1/Q4 于 2026-08-02 裁决）**：~~Q1（需求真相源权威性）/ Q4（修复义务边界）裁决完成前，DRAFT_PLANS agent 不得 draft 任何工作项~~。**裁决结果**：Q1=(c) 逐项对照需求真相源优先；Q4=(a) P0/P1 必须实现禁止方案 B 无例外。M0 工作项已转 ready，mission driver 可启动。本条保留作为裁决记录。
7. **DRAFT_PLANS Deps 检查义务**：DRAFT_PLANS agent 必须**逐行检查 Deps 列**，仅 draft 其全部 Deps 已 done 的 `todo`/`ready` 工作项；Deps 引用里程碑名（如 "MA1 done"）时，该里程碑下全部工作项须 done；展开器工作项（R1.0/A4.1/A4.2）的 done = 展开完成（实体行已追加），而非"全部实体行 done"。

## Work Item Details

### M0
- 0.1：方法论文档覆盖五级追踪矩阵模板（use-case → owner doc 契约 → 代码路径 → 测试断言 → 运行时行为）、P0/P1/P2/接受 分级判据、**完整枚举纪律（工作项 = 功能切片 × 显式 UC 清单，禁止抽样）**、报告输出格式、与 arm-index 的命名衔接。独立子代理 ≥2 轮审查（规范合规 + 覆盖面/可执行性）。
- 0.1 必含 **保护区域暂停协议**：修复行在 R1.0/MR0 展开时标注"触及保护区域"（ORM/会计/删除三类）；触及行的 plan 必须含显式 `ask-first 人工确认` checkbox（吸取 P2-MA6-001 教训——不能只依赖"草案审查预授权"）；mission driver 执行到触及行时**暂停该行**等待人工批准记录（登记于 plan 文件），非触及行继续执行；也可在 mission 启动前由人工按修复类目预授权（须列明授权类目清单）。
- 0.1 必含 **审计过程纪律自检段**（每份 MA 报告）：checker 退出码门控核查 + closure-audit 独立性声明 + 本报告发现/结论与 arm-index 的交叉去重声明。MV V.3 closure audit prompt 显式纳入 checker 门控核查项。
- 0.1 必含 **真相源冻结条款**：审计期间 product-scope/use-cases/owner doc 需求契约修订须经人工批准并登记（防重蹈"真相源被自身修订"根因）；审计发现的 doc 分歧记入报告，不直接改真相源。
- 0.2：修正 product-scope 陈旧段；18 域 use-cases 清单化（域 × UC 编号 × 功能点，**含 logistics use-cases.md 四个非标准 heading `## 用例X：…` 的归一化**）；notify 无 use-cases 的裁决（补写或标注 N/A）；初始化五级追踪矩阵空表；切片覆盖统计复校（192 UC / 51 行）。
- 0.3：从 arm-index 导出 documented simplification / Deferred / successor 全量清单（**三源对账：arm-index 行内声明 + owner doc 内嵌声明 + `docs/backlog/README.md` 既有 81 行追踪**；successor 计数 41 为 arm-index 内嵌声明数，非残留未执行数，导出后与 backlog 对账修正），按域与影响面排序；**导出按 resolved 注记关闭方式标签筛行（排除 `resolved (R*.n done)` 实现修复项）+ 每 finding ID 恰属一个 A2.x 行分区校验**；复杂度分级复用 arm-scope §1.2。

### MA1
- A1.1-A1.51：每行 = 一个**功能切片 × 显式 UC 清单**（见里程碑表），一份 plan 覆盖该行全部 UC 的五级追踪。每报告含：需求契约原文 → 实现证据（代码路径引用）→ 测试证据 → 运行时行为证据 → 符合性结论（P0/P1/P2/接受）+ 与 arm-index 既有 finding 的衔接（复用 or 新增）+ **该切片的"静态存疑点"清单（供 MA4 展开）** + **过程纪律自检段**（checker 退出码门控 + closure-audit 独立性声明）。**完整枚举纪律：全部 51 行 done 才可推进，任何合并/抽样即视为未完成。**
- S 级域（finance/mfg/hr）切片最小，每片独立报告；行内 UC 编号与 `use-cases.md` 标题一一对应，审计时逐 UC 核对不可跳号（logistics 04/05/06 以 0.2 归一化后的标题为准）。
- **与既有 finding 的衔接判定规则（复用 or 新增）**：① 同根因同控制点 → **复用**既有 finding ID（在既有 arm-index 行追加 RC 交叉引用注记），不新建编号；② 新根因/新功能点/新维度 → **新建 P1-RC-xxx 系列**（RC = Requirement Compliance）并在报告中列明与既有 finding 的差异依据；③ 每份报告必须 grep arm-index 同域同控制点后给出"复用 or 新增"裁决及理由，禁止未经比对直接新建。
- **既有 MA2 报告输入**：每切片报告开头声明"与 MA2 报告（`docs/audits/2026-07-2*-arm-ma2-*`）的差异增量"——复用其已证实的状态机/链路行为证据，只补需求视角差异。

### MA2
- A2.1-A2.9：每行 = 一个域的方案 B（documented simplification / Deferred）关闭项全集，0.3 按关闭方式标签导出后**逐 finding ID 核对**。A2.1：会计保护区域简化项逐一复核——重新打开判据：与 product-scope 冲突、影响报表/过账正确性、无显式人工批准记录。
- **"显式人工批准记录"证据标准**（判据三的判定方法，避免字面误重开）：(i) plan 文件含独立 plan-audit 通过记录；(ii) owner doc 显式 documented simplification 标注且经人工批准（AI 自写标注不算，参照 `ai-autonomy-policy.md`）；(iii) product-scope 范围裁剪登记。判据三仅当 (i)/(ii) 均不成立时兜底触发。代理独立审计通过**算**"审计裁决质量证据"（可区分"静默降级"vs"经审计裁决的简化"），**不算**人工批准。
- A2.2-A2.9：非会计域简化项复核，重点：owner doc "Deferred" 是否显式、是否影响主路径。跨域行（A2.9）按 finding ID 清单逐项核对。
- **MA2 ↔ MA3 协作**：方案 B 关闭项伴随后续 successor 的（如反结账审批流 successor、FX 重估 successor），同一 finding 的两面——**关闭裁决本身归 A2.x 复核，其 successor 触发条件归 A3.x 复核**，各自报告交叉引用，不重复登记。

### MA3
- A3.1-A3.5：0.3 三源对账导出的 successor 清单逐一核对：触发条件是否已满足、是否该回队（回到 MA1/R1.0 或 backlog README）、无触发条件的补登记、backlog/README 既有行覆盖与正确性复核。每行覆盖该行域分组的全部 successor 项。owner doc 内嵌 successor（如 `costing-methods.md:66` FIFO 红冲）经 0.3 导出纳入，不遗漏。

### MA4
- A4.1/A4.2（展开器，对齐 R1.0 范式）：MA1 全部报告完成后，读取存疑点清单并向本表追加实体行（A4.1.n/A4.2.n，每个存疑点一行）。**展开完成（全部实体行已追加）后 A4.1/A4.2 标记 done**（同 R1.0 的 done 判据 = 展开完成，而非"全部修复完成"）。每个 A4.1.n/A4.2.n 实体行 = 一个运行时验证项（复用 `tests/e2e/` + `orchestration/_helper.ts` 原语），独立 plan + 独立 done。**无存疑点的切片不产生实体行**（此时展开器直接 done，A4.1/A4.2 无可展开内容即完成）。既有 MA2 报告已证实的行为不重复验证，只补"需求↔行为"差异。MA4 里程碑 done 判据 = A4.1+A4.2 done + 全部 A4.1.n/A4.2.n 实体行 done。

### MR0
- R0.n：P0 即时修复通道。MA1-MA4 执行中发现 P0 时动态创建独立 fix plan（对齐 audit-remediation P0 即时通道先例：`P0-MA2-016/017/019/020`）并向 MR0 表追加实体行（R0.1, R0.2…）；含 ask-first 保护区域门控 + 独立 plan-audit + 保护区域暂停协议（触及行等待人工批准）。

### MR1
- R1.0：展开器。MA1-MA4 发现的 P0/P1 需求分歧汇总、排序、展开为具体修复行（**RC-R1.1, RC-R1.2...，前缀 RC 避免与 audit-remediation R1.x 系列混淆**），每行引用需求契约 + finding ID + 分歧描述 + 修复方案 + 触及保护区域标注。
- RC-R1.n：R1.0 展开的实体修复工作项（执行时向 MR1 表追加，不预注册）。mission driver 逐项自动执行：DRAFT_PLANS → 独立草案审查 → EXECUTE → 独立结束审计 → 写回 done。**禁止方案 B 关闭（Q4 已裁决=(a)，无例外）**；触及保护区域行按暂停协议等待人工批准。

### MV / MG
- 全量验证 + 独立 closure audit（cold-context 子代理）；知识沉淀按 lessons/skills/context 三通道。
