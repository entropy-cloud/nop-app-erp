# 需求基线清单（RC Requirement Baseline Inventory）

> 状态：active
> 来源：plan `docs/plans/2026-08-02-1530-1-requirement-baseline-extraction.md`（roadmap Work Item 0.2）
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§1 五级追踪矩阵 / §3 完整枚举 / §4 Q1 真相源层级 + notify 特例 / §9 真相源冻结条款）
> 角色：MA1（A1.1-A1.51）五级追踪矩阵 L1 的**唯一入口**与逐切片核对的锚点；本文件是"清单/索引"工件，**不是**需求真相源（真相源 = `product-scope.md` + 各域 `use-cases.md`）。
> 工具交叉核对：`node tools/use-case-map.cjs --overview`（菜单↔UC 对照基线）

## §方法论对齐声明

- 本清单是 MA1 各切片报告的 **L1 锚点**：每个 UC 一行，含域/UC 编号/标题/功能点摘要/`use-cases.md:line` 定位。
- **完整枚举纪律**（方法论 §3）：UC 编号逐域核对无跳号、无重复、heading 格式统一；MA1 切片行内 UC 编号须逐 UC 核对，不可跳号。
- **Q1 真相源层级**（方法论 §4）：L1 = use-cases.md（权威功能契约）；L2 = owner doc（设计参考，冲突时以 L1 为准）。
- **真相源冻结条款**（方法论 §9）：本工作项（0.2）**不修改** product-scope / use-cases 的需求契约内容；发现的契约分歧记入本文件"§基线分歧登记"段交 MA1，不直改真相源。

---

## §UC 权威清单（18 域 + notify）

> 数据来源：实测 `rg -n "^## UC-" docs/design/*/use-cases.md`（logistics 经 0.2 归一化）。
> 统计：18 域共 **192 UC**（与 roadmap MA1 基线 192 一致）+ notify 7 UC（0.2 补写，见 Phase 2）= **199 UC**。
> 覆盖核对：`tools/use-case-map.cjs --overview` 实测菜单引用 189 UC（3 个 logistics UC 与部分 UC 未被 `app:useCases` 引用，属"孤儿用例"提示，非清单缺陷——工具的"无用例关联"对报表/配置页是预期内的，见 `use-case-authoring-guide.md §3`）。

### finance（17 UC）— `docs/design/finance/use-cases.md`

| UC 编号 | 标题 | 功能点摘要 | use-cases.md:line |
|---------|------|-----------|-------------------|
| UC-FIN-01 | 业财自动过账 | 业务单据审核触发按 businessType 路由 Provider 生成借贷平衡凭证 + VoucherBillR 业财回链 | :16 |
| UC-FIN-02 | 业务单据作废触发红字冲销 | 已过账单据作废 → 反查关联凭证 → 生成红字凭证（金额取负）+ 原凭证 isReversed | :42 |
| UC-FIN-03 | 可插拔 Provider 路由 | 新增 businessType 注册 IErpFinAcctDocProvider Bean，核心过账引擎零改接入 | :60 |
| UC-FIN-04 | FactsValidator 科目分摊 | 凭证写库前按 GlDistribution 规则拆行，Σ拆分行==原行；Σpercent≠100 拒绝过账 | :76 |
| UC-FIN-05 | 多账套并行过账 | 同业务对每 AcctSchema 各生成一组凭证，GlBalance 按 acctSchemaId 隔离 | :93 |
| UC-FIN-06 | 期末结账前置门禁 | 结账前检查未过账/未审核/未核销/未折旧/未算成本，全过才进 CLOSING | :110 |
| UC-FIN-07 | 反结账 | CLOSED_FINAL→OPEN 需高权限+审批，冲销结转凭证，全程审计 | :129 |
| UC-FIN-08 | 收款核销发票 | 收款单多对多核销发票，按累计核销金额派生核销状态，更新往来余额 | :147 |
| UC-FIN-09 | 银行对账与未达账项 | 导入幂等 + 自动勾对 + 余额调节恒等式 + 未达账项生成调整凭证 | :165 |
| UC-FIN-10 | FIFO 出库成本与到岸成本 | 出库按 incomingDate 升序消耗 StockQueue；运费/关税按比例分摊入入库成本 | :183 |
| UC-FIN-11 | 预算硬拦截 | 采购审核调 BudgetControlBiz.check，余量<0 且 HARD 返回 BLOCKED 抛异常 | :204 |
| UC-FIN-12 | 多币种过账 | 凭证行本位币金额=源币×汇率，汇率缺失拒绝过账；汇兑损益 EXCHANGE_GAIN_LOSS | :223 |
| UC-FIN-13 | 预算管理(编制/控制/对比) | 预算方案审核即过 BUDGET 影子凭证；承付 COMMITMENT 凭证；三列对比报表 | :238 |
| UC-FIN-14 | 银行对账与未达账项 | （与 UC-FIN-09 同标题，更详细的断言版本；见 §基线分歧登记 D-01） | :269 |
| UC-FIN-15 | 科目分摊(GL Distribution) | （与 UC-FIN-04 同主题更详细断言；ErpFinGlDistributionValidator getOrder 较高） | :298 |
| UC-FIN-16 | 财务三大报表 | 资产负债表/利润表/现金流量表基于 GlBalance；恒等式；基于已 CLOSED 期间 | :318 |
| UC-FIN-17 | 财务看板 | KPI 实时聚合非硬编码；预警阈值来自配置；受行级权限约束 | :350 |

### manufacturing（13 UC）— `docs/design/manufacturing/use-cases.md`

| UC 编号 | 标题 | 功能点摘要 | use-cases.md:line |
|---------|------|-----------|-------------------|
| UC-MFG-01 | 工单正常生产全流程 | 工单创建→齐套→发料→报工→完工入库全链 | :18 |
| UC-MFG-02 | 多级 BOM 展开(phantom 虚拟件) | BOM 展开含 phantom 虚拟件穿透 | :43 |
| UC-MFG-03 | 齐套校验 | 工单开工前按 BOM 校验物料齐套 | :59 |
| UC-MFG-04 | 部分齐套强制开工 | 齐套不足但强制开工的分支 | :75 |
| UC-MFG-05 | 工单审核触发物料预留 | 工单 APPROVED 触发库存物料预留 | :90 |
| UC-MFG-06 | 领料扣减预留 | 领料消耗预留量 | :107 |
| UC-MFG-07 | 工单完工入库与成本结转 | 完工入库 + 成本结转凭证 | :125 |
| UC-MFG-08 | 工单取消/完工释放预留 | 取消/完工时释放未消耗预留 | :144 |
| UC-MFG-09 | 完工质检不合格→返工工单 | 质检不合格生成返工工单 | :160 |
| UC-MFG-10 | BOM 变更不影响已开工工单(快照原则) | 已开工工单用 BOM 快照，不受变更影响 | :176 |
| UC-MFG-11 | 制造看板 | 制造域 KPI 看板 | :195 |
| UC-MFG-12 | 生产成本差异分析 | 标准成本 vs 实际成本差异分析 | :216 |
| UC-MFG-13 | 生产批次追溯 | 生产批次全链追溯 | :238 |

### human-resource（12 UC）— `docs/design/human-resource/use-cases.md`

| UC 编号 | 标题 | 功能点摘要 | use-cases.md:line |
|---------|------|-----------|-------------------|
| UC-HR-01 | 员工入职 | 员工入职建档 | :3 |
| UC-HR-02 | 休假申请流程 | 休假申请审批 | :15 |
| UC-HR-03 | 工时表提交 | 工时表提交核算 | :27 |
| UC-HR-04 | 薪酬核算 | 薪酬计算核算 | :39 |
| UC-HR-05 | 招聘录用 | 招聘流程到录用 | :51 |
| UC-HR-06 | 考勤跟踪 | 考勤记录跟踪 | :63 |
| UC-HR-07 | 合同到期提醒 | 合同到期提醒 | :75 |
| UC-HR-08 | 部门调动 | 部门调动流转 | :87 |
| UC-HR-09 | 排班管理 | 排班计划管理 | :101 |
| UC-HR-10 | 薪酬模拟 | 薪酬调整模拟 | :113 |
| UC-HR-11 | 员工调研 | 员工调研问卷 | :125 |
| UC-HR-12 | 胜任力管理与评估 | 胜任力模型评估 | :137 |

### purchase（8 UC）— `docs/design/purchase/use-cases.md`

| UC 编号 | 标题 | 功能点摘要 | use-cases.md:line |
|---------|------|-----------|-------------------|
| UC-PUR-01 | 标准采购全流程(主路径) | 请购→订单→入库→发票→付款全链 | :19 |
| UC-PUR-02 | 三单匹配(订单/入库/发票) | 三单匹配核验 | :55 |
| UC-PUR-03 | 部分入库与分批收货 | 分批收货处理 | :81 |
| UC-PUR-04 | 采购退货 | 采购退货全链 | :104 |
| UC-PUR-05 | 价格差异(发票价 ≠ 订单价) | 发票价与订单价差异处理 | :130 |
| UC-PUR-06 | 数量差异(入库 ≠ 订单) | 入库数量差异（短收） | :151 |
| UC-PUR-07 | 业财一体过账(入库与发票) | 入库与发票过账 | :172 |
| UC-PUR-08 | 请购转订单 | 请购单转采购订单 | :204 |

### sales（12 UC）— `docs/design/sales/use-cases.md`

| UC 编号 | 标题 | 功能点摘要 | use-cases.md:line |
|---------|------|-----------|-------------------|
| UC-SAL-01 | 标准销售全流程(主路径) | 订单→出库→发票→收款全链 | :20 |
| UC-SAL-02 | 出库可用量不足审核回滚(销售独有) | 出库可用量不足审核回滚 | :56 |
| UC-SAL-03 | 分批出库与部分收款 | 分批出库与部分收款 | :76 |
| UC-SAL-04 | 销售退货退款(已开票) | 已开票退货退款 | :99 |
| UC-SAL-05 | 未开票退货冲减暂估应收 | 未开票退货冲减暂估应收 | :132 |
| UC-SAL-06 | 退货换货 | 退货换货处理 | :149 |
| UC-SAL-07 | 退货成本处理 | 退货成本处理 | :165 |
| UC-SAL-08 | 赠品行扣库存 + 价税分离 | 赠品行扣库存 + 价税分离 | :180 |
| UC-SAL-09 | 退货约束校验 | 退货约束校验 | :200 |
| UC-SAL-10 | 并发出库扣同一批次 | 并发出库乐观锁 | :216 |
| UC-SAL-11 | 销售价格管理 | 销售价格策略管理 | :231 |
| UC-SAL-12 | 销售看板 | 销售域 KPI 看板 | :265 |

### assets（12 UC）— `docs/design/assets/use-cases.md`

| UC 编号 | 标题 | 功能点摘要 | use-cases.md:line |
|---------|------|-----------|-------------------|
| UC-AST-01 | 设备购置资本化入账 | 设备购置资本化入账 | :15 |
| UC-AST-02 | 期末直线法折旧 | 期末直线法折旧 | :31 |
| UC-AST-03 | 资产闲置停提与恢复 | 资产闲置停提与恢复 | :50 |
| UC-AST-04 | 资产报废处置 | 资产报废处置 | :65 |
| UC-AST-05 | 资产出售处置 | 资产出售处置 | :84 |
| UC-AST-06 | 在建工程转固 | 在建工程转固定资产 | :101 |
| UC-AST-07 | 折旧漏提补提 | 折旧漏提补提 | :116 |
| UC-AST-08 | 期末批量折旧容错 | 期末批量折旧容错 | :131 |
| UC-AST-09 | 资产盘点 | 资产盘点 | :147 |
| UC-AST-10 | 资产维修 | 资产维修 | :166 |
| UC-AST-11 | 资产拆分与合并 | 资产拆分与合并 | :185 |
| UC-AST-12 | 资产看板 | 资产域 KPI 看板 | :213 |

### inventory（11 UC）— `docs/design/inventory/use-cases.md`

| UC 编号 | 标题 | 功能点摘要 | use-cases.md:line |
|---------|------|-----------|-------------------|
| UC-INV-01 | 采购入库移动单全链 | 采购入库移动单全链 | :15 |
| UC-INV-02 | 销售出库可用量不足拒绝 | 销售出库可用量不足拒绝 | :37 |
| UC-INV-03 | 已完成移动单冲销 | 已完成移动单冲销 | :57 |
| UC-INV-04 | 全链路正向追溯 | 移动单全链路正向追溯 | :76 |
| UC-INV-05 | 退货反查原移动单 | 退货反查原移动单 | :92 |
| UC-INV-06 | 批次追溯与效期拦截 | 批次追溯与效期拦截 | :106 |
| UC-INV-07 | 盘点差异生成移动单 | 盘点差异生成移动单 | :122 |
| UC-INV-08 | 并发扣减乐观锁 | 并发扣减乐观锁 | :140 |
| UC-INV-09 | 负库存放行 | 负库存放行策略 | :155 |
| UC-INV-10 | 移动单触发存货估值凭证 | 移动单触发存货估值凭证 | :171 |
| UC-INV-11 | 库存看板 | 库存域 KPI 看板 | :193 |

### crm（15 UC）— `docs/design/crm/use-cases.md`

| UC 编号 | 标题 | 功能点摘要 | use-cases.md:line |
|---------|------|-----------|-------------------|
| UC-CRM-01 | 线索创建与验证 | 线索创建与验证 | :15 |
| UC-CRM-02 | 线索 → 商机转化 | 线索转商机 | :33 |
| UC-CRM-03 | 商机 → 报价单转化 | 商机转报价单 | :54 |
| UC-CRM-04 | 丢单原因记录 | 丢单原因记录 | :74 |
| UC-CRM-05 | 活动/事件记录 | 活动/事件记录 | :92 |
| UC-CRM-06 | 漏斗阶段推进 | 漏斗阶段推进 | :113 |
| UC-CRM-07 | UTM 营销活动归因 | UTM 营销活动归因 | :132 |
| UC-CRM-08 | 事件提醒 Job | 事件提醒定时任务 | :154 |
| UC-CRM-09 | 线索自动评分 | 线索自动评分 | :175 |
| UC-CRM-10 | 销售预测生成 | 销售预测生成 | :208 |
| UC-CRM-11 | 线索区域自动分配 | 线索区域自动分配 | :239 |
| UC-CRM-12 | 销售配额管理 | 销售配额管理 | :269 |
| UC-CRM-13 | CPQ 配置-定价-报价 | CPQ 配置定价报价 | :300 |
| UC-CRM-14 | 销售序列自动分配与推进 | 销售序列自动分配与推进 | :332 |
| UC-CRM-15 | 线索漏斗分析 | 线索漏斗分析 | :366 |

### quality（12 UC）— `docs/design/quality/use-cases.md`

| UC 编号 | 标题 | 功能点摘要 | use-cases.md:line |
|---------|------|-----------|-------------------|
| UC-QA-01 | 来料强制质检阻塞流转 | 来料强制质检阻塞 | :15 |
| UC-QA-02 | 质检不合格触发退货 | 质检不合格触发退货 | :33 |
| UC-QA-03 | 让步接收 | 让步接收 | :50 |
| UC-QA-04 | 完工检验不合格→返工 | 完工检验不合格返工 | :67 |
| UC-QA-05 | NCR-CAPA 闭环 | NCR 不符合项 + CAPA 纠正预防闭环 | :82 |
| UC-QA-06 | 关键项否决 | 关键项否决 | :101 |
| UC-QA-07 | 质检模板优先级解析 | 质检模板优先级解析 | :116 |
| UC-QA-08 | 业务单据作废联动取消质检 | 单据作废联动取消质检 | :133 |
| UC-QA-09 | SPC 失控预警 | SPC 失控预警 | :148 |
| UC-QA-10 | SPC 过程能力分析 | SPC 过程能力分析 | :166 |
| UC-QA-11 | SPC 数据从 InspectionLine 聚合 | SPC 数据聚合 | :187 |
| UC-QA-12 | 质量看板 | 质量域 KPI 看板 | :207 |

### projects（10 UC）— `docs/design/projects/use-cases.md`

| UC 编号 | 标题 | 功能点摘要 | use-cases.md:line |
|---------|------|-----------|-------------------|
| UC-PRJ-01 | 项目立项 | 项目立项 | :15 |
| UC-PRJ-02 | 工时提交触发人工成本凭证 | 工时提交触发人工成本凭证 | :30 |
| UC-PRJ-03 | 多来源成本归集 | 多来源成本归集 | :49 |
| UC-PRJ-04 | 项目预算 STRICT 超支拦截 | 项目预算 STRICT 超支拦截 | :65 |
| UC-PRJ-05 | 任务依赖 DAG 成环校验 | 任务依赖 DAG 成环校验 | :81 |
| UC-PRJ-06 | 项目损益汇总 | 项目损益汇总 | :96 |
| UC-PRJ-07 | 竣工结算与质保金 | 竣工结算与质保金 | :115 |
| UC-PRJ-08 | 项目结算转固 | 项目结算转固 | :131 |
| UC-PRJ-09 | 项目暂停/关闭约束 | 项目暂停/关闭约束 | :147 |
| UC-PRJ-10 | 项目看板 | 项目域 KPI 看板 | :167 |

### customer-service（12 UC）— `docs/design/customer-service/use-cases.md`

| UC 编号 | 标题 | 功能点摘要 | use-cases.md:line |
|---------|------|-----------|-------------------|
| UC-CS-01 | 客户创建工单（Ticket Creation） | 客户创建工单 | :3 |
| UC-CS-02 | 工单分派与接受（Ticket Assignment） | 工单分派与接受 | :23 |
| UC-CS-03 | 工单解决与客户确认（In-Progress Resolution） | 工单解决与客户确认 | :42 |
| UC-CS-04 | SLA 超时与升级（SLA Breach & Escalation） | SLA 超时与升级 | :63 |
| UC-CS-05 | 知识库搜索与建议（Knowledge Base Suggestion） | 知识库搜索与建议 | :84 |
| UC-CS-06 | 工单升级为质量事件（Escalation to Quality NCR） | 工单升级为质量事件 | :105 |
| UC-CS-07 | 预设应答使用（Canned Response Usage） | 预设应答使用 | :124 |
| UC-CS-08 | 满意度调查发送与评分（CSAT Survey） | 满意度调查发送与评分 | :145 |
| UC-CS-09 | 服务权益校验（Entitlement Check） | 服务权益校验 | :165 |
| UC-CS-10 | 服务目录请求提交（Catalog Request） | 服务目录请求提交 | :184 |
| UC-CS-11 | 工单计时录入（Time Tracking） | 工单计时录入 | :203 |
| UC-CS-12 | 服务目录履行流程（Service Fulfillment） | 服务目录履行流程 | :223 |

### master-data（7 UC）— `docs/design/master-data/use-cases.md`

| UC 编号 | 标题 | 功能点摘要 | use-cases.md:line |
|---------|------|-----------|-------------------|
| UC-MD-01 | 扫码开单 | 扫码开单 | :8 |
| UC-MD-02 | 多单位换算落账 | 多单位换算落账 | :23 |
| UC-MD-03 | 价格优先级解析 | 价格优先级解析 | :39 |
| UC-MD-04 | 最低价校验拦截 | 最低价校验拦截 | :55 |
| UC-MD-05 | 默认 SKU 兜底 | 默认 SKU 兜底 | :71 |
| UC-MD-06 | SKU 状态约束 | SKU 状态约束 | :86 |
| UC-MD-07 | 主数据看板 | 主数据域 KPI 看板 | :106 |

### maintenance（11 UC）— `docs/design/maintenance/use-cases.md`

| UC 编号 | 标题 | 功能点摘要 | use-cases.md:line |
|---------|------|-----------|-------------------|
| UC-MAIN-01 | 预防性维护自动调度(时间周期) | 时间周期预防性维护自动调度 | :16 |
| UC-MAIN-02 | 运行时长触发维护 | 运行时长触发维护 | :34 |
| UC-MAIN-03 | 维护访问全流程 | 维护访问全流程 | :49 |
| UC-MAIN-04 | 备件消耗闭环 | 备件消耗闭环 | :71 |
| UC-MAIN-05 | 报修响应性维护 | 报修响应性维护 | :88 |
| UC-MAIN-06 | 设备故障停机影响排产 | 设备故障停机影响排产 | :103 |
| UC-MAIN-07 | 维护中发现额外故障 | 维护中发现额外故障 | :121 |
| UC-MAIN-08 | 设备资产处置联动 | 设备资产处置联动 | :136 |
| UC-MAIN-09 | 排程冲突检测 | 排程冲突检测 | :151 |
| UC-MAIN-10 | OEE 计算 | OEE 计算 | :166 |
| UC-MAIN-11 | 维护看板 | 维护域 KPI 看板 | :187 |

### contract（10 UC）— `docs/design/contract/use-cases.md`

| UC 编号 | 标题 | 功能点摘要 | use-cases.md:line |
|---------|------|-----------|-------------------|
| UC-CT-01 | 合同创建与签署（Contract Creation & Signing） | 合同创建与签署 | :3 |
| UC-CT-02 | 合同变更与版本管理（Contract Amendment） | 合同变更与版本管理 | :25 |
| UC-CT-03 | 开票计划生成与执行（Invoice Plan Generation） | 开票计划生成与执行 | :47 |
| UC-CT-04 | 消耗计费与用量结算（Consumption Billing） | 消耗计费与用量结算 | :69 |
| UC-CT-05 | 合同到期提醒与续期（Expiry Reminder & Renewal） | 合同到期提醒与续期 | :89 |
| UC-CT-06 | 合同提前终止（Contract Termination） | 合同提前终止 | :111 |
| UC-CT-07 | 合同审批工作流 | 合同审批工作流 | :134 |
| UC-CT-08 | 批量折扣与返利 | 批量折扣与返利 | :146 |
| UC-CT-09 | 电子签章 | 电子签章 | :158 |
| UC-CT-10 | 合同仓库与全文检索 | 合同仓库与全文检索 | :170 |

### b2b（8 UC）— `docs/design/b2b/use-cases.md`

| UC 编号 | 标题 | 功能点摘要 | use-cases.md:line |
|---------|------|-----------|-------------------|
| UC-B2B-001 | EDI 格式配置 | EDI 格式配置 | :3 |
| UC-B2B-002 | EDI 出站发送 | EDI 出站发送 | :35 |
| UC-B2B-003 | ASN 入站接收 | ASN 入站接收 | :69 |
| UC-B2B-004 | 代码映射管理 | 代码映射管理 | :105 |
| UC-B2B-005 | Webhook 集成 | Webhook 集成 | :132 |
| UC-B2B-006 | 错误处理与重试 | 错误处理与重试 | :166 |
| UC-B2B-007 | 合作伙伴上线 | 合作伙伴上线 | :220 |
| UC-B2B-008 | 托管文件传输（MFT）配置 | 托管文件传输配置 | :234 |

### drp（8 UC）— `docs/design/drp/use-cases.md`

| UC 编号 | 标题 | 功能点摘要 | use-cases.md:line |
|---------|------|-----------|-------------------|
| UC-DRP-01 | DRP 计划创建 | DRP 计划创建 | :3 |
| UC-DRP-02 | 净需求计算（DRP 运行） | 净需求计算 DRP 运行 | :15 |
| UC-DRP-03 | 补货建议审批 | 补货建议审批 | :27 |
| UC-DRP-04 | 自动生成补货单 | 自动生成补货单 | :39 |
| UC-DRP-05 | 安全库存调整 | 安全库存调整 | :51 |
| UC-DRP-06 | 安全库存优化 | 安全库存优化 | :63 |
| UC-DRP-07 | 越库（Cross-Dock） | 越库 Cross-Dock | :75 |
| UC-DRP-08 | 提前期跟踪与动态安全库存 | 提前期跟踪与动态安全库存 | :87 |

### logistics（7 UC，0.2 已归一化）— `docs/design/logistics/use-cases.md`

> **归一化记录**：原 3 个非标准 heading `## 用例四/五/六：…`（对应 UC-LOG-04/05/06）已由 0.2 归一化为 `## UC-LOG-NN …` 标准格式（标题文案不变：运费过账/承运商集成/签收确认）。实测 `rg "^## 用例" docs/design/logistics/use-cases.md` 无命中。

| UC 编号 | 标题 | 功能点摘要 | use-cases.md:line |
|---------|------|-----------|-------------------|
| UC-LOG-01 | 发运单创建 | 发运单创建 + 重复发运防护 | :5 |
| UC-LOG-02 | 承运商派发 | 确认发运 + 异步调用承运商网关 + 运单号回写 | :17 |
| UC-LOG-03 | 追踪更新 | 网关回调/轮询追踪 + 签收转 DELIVERED | :29 |
| UC-LOG-04 | 运费过账 | DELIVERED 后按 relatedBillType 路由销售运费/采购到岸成本 | :41 |
| UC-LOG-05 | 承运商集成 | 承运商 + 网关配置 + 凭证加密 + 连通性测试 | :53 |
| UC-LOG-06 | 签收确认 | 签收信息记录 + IN_TRANSIT→DELIVERED + 触发运费过账 | :79 |
| UC-LOG-07 | 配送时间窗口管理 | 配送窗口预约 + 容量校验 + 爽约处理 | :65 |

### aps（7 UC）— `docs/design/aps/use-cases.md`

| UC 编号 | 标题 | 功能点摘要 | use-cases.md:line |
|---------|------|-----------|-------------------|
| UC-APS-01 | 工序工单创建（从主工单） | 从主工单创建工序工单 | :3 |
| UC-APS-02 | 前向排产 | 前向排产 | :15 |
| UC-APS-03 | 后向排产 | 后向排产 | :27 |
| UC-APS-04 | 插单/急单插入 | 插单急单插入 | :39 |
| UC-APS-05 | ATP/CTP 交期承诺 | ATP/CTP 交期承诺 | :51 |
| UC-APS-06 | 替代工艺路线 | 替代工艺路线 | :63 |
| UC-APS-07 | 自动派工 | 自动派工 | :75 |

### notify（7 UC，0.2 补写）— `docs/design/notify/use-cases.md`

> UC 编号前缀 `UC-SYS-NN`（对齐 notify appName `erp-notify` / 二级简称 `sys` / 表前缀 `erp_sys_*` / 实体 `ErpSys*`，与 `use-case-authoring-guide.md` SYS 域简称一致；与 arm-index/roadmap notify 工程命名一致）。补写裁决依据见 §notify 补写纪律自检。

| UC 编号 | 标题 | 功能点摘要 | use-cases.md:line |
|---------|------|-----------|-------------------|
| UC-SYS-01 | 通知模板生命周期与查找 | DRAFT→ACTIVE；按 notificationType 查 ACTIVE 模板；无 ACTIVE 模板 config-gated 跳过 | :28 |
| UC-SYS-02 | 通知实例端到端派发链 | notify() → 渲染→接收人解析→频控合并→站内落库→config-gated 外发；best-effort 不回滚调用方 | :48 |
| UC-SYS-03 | 接收人多策略解析 | ROLE/ORG/USER_LIST/PARTNER 四类 resolver | :74 |
| UC-SYS-04 | 频控窗口合并 | key=notificationType+userId；业务提醒 300s/异常告警 60s/系统通知不合并；命中 mergeCount+1 | :98 |
| UC-SYS-05 | 已读状态管理 | markRead/markAllRead；(notificationId,userId) 唯一键防重；status 非 read | :123 |
| UC-SYS-06 | 用户面收件箱后端查询 | findUnread/findRead/countUnread userId 可选回退 ctx；三态；禁用 status=READ filter | :149 |
| UC-SYS-07 | 外发通道 config-gated 降级 | EMAIL/SMS 经 nop-integration SPI；email/sms-enabled 默认 false；无供应商 WARN 跳过不阻断 | :174 |

---

## §切片覆盖复校（UC 权威清单 × roadmap MA1 表 51 行）

> 逐一核对 UC 权威清单与 `docs/backlog/requirement-compliance-roadmap.md` MA1 表（50 个 UC 切片 + 1 个 notify 切片 = 51 行）的映射。
> **覆盖统计（归一化 + notify 补写后）**：
> - 18 域 UC 实测总数 = **192**（与 roadmap MA1 基线 192 完全一致；归一化前 logistics 少计 3，工具 `--overview` 实测 189，归一化后 192）。
> - notify UC 实测总数 = **7**（UC-SYS-01..07，Phase 2 补写）。
> - 全域 UC 总数 = **199**（192 + 7）。
> - 切片行数 = **51**（A1.1-A1.50 覆盖 18 域 192 UC，A1.51 覆盖 notify 7 UC）；与 roadmap MA1 表 51 行一致，无新增切片行。

| 切片 | 域-功能切片 | UC 清单 | UC 数 | 核对结果 |
|------|------------|---------|------|---------|
| A1.1 | finance-F1 过账引擎与凭证链路 | UC-FIN-01/02/03/04/12/15 | 6 | ✅ 一致 |
| A1.2 | finance-F2 预算与承付 | UC-FIN-11/13 | 2 | ✅ 一致 |
| A1.3 | finance-F3 AR/AP 核销与坏账 | UC-FIN-08 | 1 | ✅ 一致 |
| A1.4 | finance-F4 银行对账 | UC-FIN-09/14 | 2 | ⚠ 标题重复（见 D-01）；roadmap 已裁决同属此切片 |
| A1.5 | finance-F5 成本核算 | UC-FIN-10 | 1 | ✅ 一致 |
| A1.6 | finance-F6 期间与结账 | UC-FIN-06/07 | 2 | ✅ 一致 |
| A1.7 | finance-F7 报表/看板/多账套 | UC-FIN-05/16/17 | 3 | ✅ 一致（FIN-15 按 roadmap 归 A1.1 F1） |
| A1.8 | mfg-F1 MRP/DRP 引擎 | UC-MFG-05/08 | 2 | ✅ 一致 |
| A1.9 | mfg-F2 工单与报工 | UC-MFG-01/03/04/06/07/09 | 6 | ✅ 一致 |
| A1.10 | mfg-F3 BOM 与工艺路线 | UC-MFG-02/10 | 2 | ✅ 一致 |
| A1.11 | mfg-F4 差异/批次/看板 | UC-MFG-11/12/13 | 3 | ✅ 一致 |
| A1.12 | hr-F1 员工与组织 | UC-HR-01/05/07/08/12 | 5 | ✅ 一致 |
| A1.13 | hr-F2 排班与考勤 | UC-HR-02/06/09 | 3 | ✅ 一致 |
| A1.14 | hr-F3 薪酬与调研 | UC-HR-03/04/10/11 | 4 | ✅ 一致 |
| A1.15 | purchase-F1 主流程与请购 | UC-PUR-01/08 | 2 | ✅ 一致 |
| A1.16 | purchase-F2 三单匹配与差异 | UC-PUR-02/03/05/06 | 4 | ✅ 一致 |
| A1.17 | purchase-F3 退货与业财 | UC-PUR-04/07 | 2 | ✅ 一致 |
| A1.18 | sales-F1 主流程与价格 | UC-SAL-01/11 | 2 | ✅ 一致 |
| A1.19 | sales-F2 出库与并发 | UC-SAL-02/03/10 | 3 | ✅ 一致 |
| A1.20 | sales-F3 退货族 | UC-SAL-04/05/06/07/09 | 5 | ✅ 一致 |
| A1.21 | sales-F4 赠品与看板 | UC-SAL-08/12 | 2 | ✅ 一致 |
| A1.22 | assets-F1 折旧引擎 | UC-AST-02/07/08 | 3 | ✅ 一致 |
| A1.23 | assets-F2 处置 | UC-AST-04/05 | 2 | ✅ 一致 |
| A1.24 | assets-F3 资本化/拆分/盘点/维修/看板 | UC-AST-01/03/06/09/10/11/12 | 7 | ✅ 一致 |
| A1.25 | inventory-F1 移动单主链与追溯 | UC-INV-01/03/04/05 | 4 | ✅ 一致 |
| A1.26 | inventory-F2 批次与可用量 | UC-INV-02/06/09 | 3 | ✅ 一致 |
| A1.27 | inventory-F3 盘点/估值/并发/看板 | UC-INV-07/08/10/11 | 4 | ✅ 一致 |
| A1.28 | crm-F1 线索生命周期 | UC-CRM-01/02/03/04/09/11 | 6 | ✅ 一致 |
| A1.29 | crm-F2 营销/预测/配额/序列/事件提醒 | UC-CRM-05/07/08/10/12/14/15 | 7 | ✅ 一致 |
| A1.30 | crm-F3 CPQ/漏斗推进 | UC-CRM-06/13 | 2 | ✅ 一致 |
| A1.31 | quality-F1 检验门控 | UC-QA-01/02/03/04/06/07/08 | 7 | ✅ 一致 |
| A1.32 | quality-F2 NCR-CAPA 闭环 | UC-QA-05 | 1 | ✅ 一致 |
| A1.33 | quality-F3 SPC 与看板 | UC-QA-09/10/11/12 | 4 | ✅ 一致 |
| A1.34 | projects-F1 立项与成本归集 | UC-PRJ-01/02/03/09 | 4 | ✅ 一致 |
| A1.35 | projects-F2 预算与 DAG | UC-PRJ-04/05 | 2 | ✅ 一致 |
| A1.36 | projects-F3 结算与看板 | UC-PRJ-06/07/08/10 | 4 | ✅ 一致 |
| A1.37 | cs-F1 工单生命周期 | UC-CS-01/02/03/11 | 4 | ✅ 一致 |
| A1.38 | cs-F2 SLA 与升级 | UC-CS-04 | 1 | ✅ 一致 |
| A1.39 | cs-F3 知识库/质量联动/预设应答 | UC-CS-05/06/07 | 3 | ✅ 一致 |
| A1.40 | cs-F4 调查/权益/目录/履行 | UC-CS-08/09/10/12 | 4 | ✅ 一致 |
| A1.41 | master-data 全功能 | UC-MD-01~07 | 7 | ✅ 一致 |
| A1.42 | maintenance-F1 调度与冲突 | UC-MAIN-01/02/09 | 3 | ✅ 一致 |
| A1.43 | maintenance-F2 访问与备件 | UC-MAIN-03/04 | 2 | ✅ 一致 |
| A1.44 | maintenance-F3 响应/联动/OEE/看板 | UC-MAIN-05/06/07/08/10/11 | 6 | ✅ 一致 |
| A1.45 | contract-F1 生命周期与签署 | UC-CT-01/02/05/06/07/09 | 6 | ✅ 一致 |
| A1.46 | contract-F2 计费与返利 | UC-CT-03/04/08/10 | 4 | ✅ 一致 |
| A1.47 | b2b 全功能 | UC-B2B-001~008 | 8 | ✅ 一致 |
| A1.48 | drp 全功能 | UC-DRP-01~08 | 8 | ✅ 一致 |
| A1.49 | logistics 全功能 | UC-LOG-01~07 | 7 | ✅ 一致（04/05/06 已归一化） |
| A1.50 | aps 全功能 | UC-APS-01~07 | 7 | ✅ 一致 |
| A1.51 | notify 通知派发 | UC-SYS-01~07 | 7 | ✅ Phase 2 补写后显式 UC 清单 |

**复校结论**：
- 50 个 UC 切片（A1.1-A1.50）的 UC 编号逐一映射到 UC 权威清单，**无跳号、无遗漏、无归属冲突**（唯一需注意 = finance UC-FIN-04/15 同主题分属 A1.1/A1.7、UC-FIN-09/14 同标题同属 A1.4，均由 roadmap 已裁决，见 D-01/D-02）。
- UC 总数 192（18 域）+ 7（notify）= 199，切片行数 51，**与 roadmap MA1 基线一致，无需新增/删减切片行**。
- 0.2 归一化 logistics 3 heading 后，工具 `use-case-map.cjs --overview` 合计从 189 修正为 192，消除清单化前的计数偏差。
- **notify 补写后复算**（Phase 3）：notify UC = 7（UC-SYS-01..07），归入 A1.51 单切片（roadmap 原设计 notify = 1 切片"按功能点核验"，补写后细化为显式 UC 编号清单 UC-SYS-01..07）。**切片总数仍为 51**（无新增/删减切片行）；UC 总数从 192 → 199（+7 notify UC）。

---

## §切片索引 + 五级矩阵骨架

> 供 MA1 各切片报告（`docs/audits/YYYY-MM-DD-HHmm-rc-ma1-a*.md`）直接填充。矩阵列定义来自方法论 §1（5 列 L1-L5）；每个 UC 一行，不可合并（方法论 §3 完整枚举纪律）。

### 五级追踪矩阵空表模板（L1-L5 五列）

| UC 编号 | L1 use-case 需求契约 | L2 owner doc 契约 | L3 代码路径 | L4 测试断言 | L5 运行时行为 | 符合性结论 |
|---------|---------------------|------------------|------------|------------|--------------|-----------|
| UC-XXX-NN | `<use-cases.md>:<line>` 标题 + 验收标准原文（逐字引用，不转述） | `<owner-doc>:<section>`（设计参考，冲突以 L1 为准） | `module-<domain>/erp-<short>-service/.../<X>BizModel.java:<line>`（含行号，跨域调用链列全） | `<TestFile>.java#<method>` / `tests/e2e/<spec>.spec.ts#<describe>`（注明断言强度） | 行为证据（复用 MA2 报告 / E2E / 临时探针） | P0/P1/P2/接受（§2 判据） |

**填充纪律**（方法论 §1）：
- L1 验收标准**逐字引用**，禁止转述（避免"代理转述已向实现妥协" Q1 根因）。
- L2 与 L1 冲突时一律以 L1 为准，在"冲突裁决"列注明"以 L1 为准，L2 推定已向实现妥协"。
- L3 必须**含行号**；跨域调用链须列全（Facade → Processor → 跨域 I*Biz）。
- L5 存疑点登记到该切片报告的"静态存疑点清单"段，交 MA4 展开。
- 已被既有 MA2 报告证实的 L5 行为，注明"行为已证实（引用 MA2 报告）"，不重复验证。

### 51 切片索引（含每切片 UC 编号清单，供 MA1 各切片报告填充）

| 切片 | UC 清单 | 切片 | UC 清单 |
|------|---------|------|---------|
| A1.1 | UC-FIN-01/02/03/04/12/15 | A1.27 | UC-INV-07/08/10/11 |
| A1.2 | UC-FIN-11/13 | A1.28 | UC-CRM-01/02/03/04/09/11 |
| A1.3 | UC-FIN-08 | A1.29 | UC-CRM-05/07/08/10/12/14/15 |
| A1.4 | UC-FIN-09/14 | A1.30 | UC-CRM-06/13 |
| A1.5 | UC-FIN-10 | A1.31 | UC-QA-01/02/03/04/06/07/08 |
| A1.6 | UC-FIN-06/07 | A1.32 | UC-QA-05 |
| A1.7 | UC-FIN-05/16/17 | A1.33 | UC-QA-09/10/11/12 |
| A1.8 | UC-MFG-05/08 | A1.34 | UC-PRJ-01/02/03/09 |
| A1.9 | UC-MFG-01/03/04/06/07/09 | A1.35 | UC-PRJ-04/05 |
| A1.10 | UC-MFG-02/10 | A1.36 | UC-PRJ-06/07/08/10 |
| A1.11 | UC-MFG-11/12/13 | A1.37 | UC-CS-01/02/03/11 |
| A1.12 | UC-HR-01/05/07/08/12 | A1.38 | UC-CS-04 |
| A1.13 | UC-HR-02/06/09 | A1.39 | UC-CS-05/06/07 |
| A1.14 | UC-HR-03/04/10/11 | A1.40 | UC-CS-08/09/10/12 |
| A1.15 | UC-PUR-01/08 | A1.41 | UC-MD-01~07 |
| A1.16 | UC-PUR-02/03/05/06 | A1.42 | UC-MAIN-01/02/09 |
| A1.17 | UC-PUR-04/07 | A1.43 | UC-MAIN-03/04 |
| A1.18 | UC-SAL-01/11 | A1.44 | UC-MAIN-05/06/07/08/10/11 |
| A1.19 | UC-SAL-02/03/10 | A1.45 | UC-CT-01/02/05/06/07/09 |
| A1.20 | UC-SAL-04/05/06/07/09 | A1.46 | UC-CT-03/04/08/10 |
| A1.21 | UC-SAL-08/12 | A1.47 | UC-B2B-001~008 |
| A1.22 | UC-AST-02/07/08 | A1.48 | UC-DRP-01~08 |
| A1.23 | UC-AST-04/05 | A1.49 | UC-LOG-01~07 |
| A1.24 | UC-AST-01/03/06/09/10/11/12 | A1.50 | UC-APS-01~07 |
| A1.25 | UC-INV-01/03/04/05 | A1.51 | UC-SYS-01~07 |
| A1.26 | UC-INV-02/06/09 | | |

> 合计：51 切片 × 199 UC 行（每个 UC 一矩阵行）。MA1 各切片报告以本索引为锚，按方法论 §1 模板填充该切片全部 UC 的 L1-L5。

---

## §notify 补写纪律自检（Q1 根因守卫）

> Phase 2 exit criteria：声明 notify use-cases 的来源映射 + 未参照运行时代码，避免 Q1 根因（真相源向实现妥协）重演。

- **来源映射**（每条 UC 验收标准可追溯到 notify 设计权威文档）：
  | UC 编号 | 来源文档（逐条映射） |
  |---------|---------------------|
  | UC-SYS-01 | `notify/README.md §核心业务对象`（模板 subjectTpl/bodyTpl + notificationType 查找）+ `§状态机`（DRAFT→ACTIVE + 无 ACTIVE 模板 config-gated 跳过） |
  | UC-SYS-02 | `notify/README.md §定位`（统一入口 notify()）+ `§子系统结构与派发链`（六步派发链顺序）+ `§边界`（best-effort 不回滚调用方） |
  | UC-SYS-03 | `notify/README.md §接收人解析策略`（ROLE/ORG/USER_LIST/PARTNER 四表原文） |
  | UC-SYS-04 | `notify/README.md §关键组件`（MergeCoordinator mergeCount+1）+ `§配置项`（合并组 key）+ `notification-strategy.md §频控规则`（窗口表原文） |
  | UC-SYS-05 | `notify/README.md §核心业务对象`（ErpSysNotificationRead 唯一键）+ `§关键组件`（markRead/markAllRead/countUnread） |
  | UC-SYS-06 | `notify/inbox-patterns.md §1`（userId 可选回退 ctx 裁决）+ `§2`（三态数据源策略 + 禁用 status=READ filter） |
  | UC-SYS-07 | `notify/README.md §通道与失败语义`（IN_APP/EMAIL/SMS + config-gated）+ `§配置项`（email/sms-enabled 默认 false）+ `notification-strategy.md §实现方案`（SPI 复用 nop-integration） |
- **未参照运行时代码声明**：本编写过程仅引用上述三份设计权威文档的契约条款，**未参照** `ErpSysNotificationBizModel.java` / `NotificationDispatcher` / `NotificationRecipientResolver` / `NotificationMergeCoordinator` 等运行时实现代码。实现是否符合本文 = MA1 A1.51 切片的审计范围（L1 需求契约 vs L3-L5 实现）。
- **UC 编号前缀裁决**：`UC-SYS-NN`（对齐 notify appName `erp-notify` / 二级简称 `sys`，与 arm-index/roadmap notify 工程命名一致；`use-case-authoring-guide.md §1` 的 `SYS` 域简称覆盖系统管理类，notify 作为跨域系统子系统归此简称）。备选前缀 `UC-NOT-NN` 未采用（notify 表前缀 `erp_sys_*` / 实体 `ErpSys*` 已固化 `sys` 命名，UC 前缀须与持久化命名一致避免引用断裂）。
- **UC 数量裁决**：7 条（UC-SYS-01..07），由 README/inbox-patterns/strategy 功能点决定。归入 A1.51 单切片（roadmap 原设计 notify = 1 切片"按功能点核验"，补写后细化为显式 UC 编号清单）。

---

## §product-scope 残余核实

> Phase 3 proof：核实 `docs/requirements/product-scope.md` 残余事实性陈旧（仅修正事实性 status/count 陈述；P1-MA3-011 里程碑框架已由 R2.2 修复，不重做）。

**核实结果**：
- ✅ **里程碑框架段（line 50-75）**：已由 R2.2（plan `2026-07-31-0010-2`，2026-07-31）对齐 AGENTS.md §当前项目阶段，line 52 含对齐说明。0.2 不重做。
- ✅ **业务域范围（line 7-48）**：18 业务域 + notify 跨域子系统计数、核心能力描述、工程命名映射与 AGENTS.md 一致。
- ✅ **当前已完成项（line 56-67）**：18 域 ORM 模型、156 reactor 模块、CRUD/M1-M5 done、报表/看板子系统——与 AGENTS.md §当前项目阶段一致。
- 🔧 **成功指标计数（line 80）**：原 `1902 单元测试 0 failures` 与 roadmap §当前基线 `mvn test 全绿（1903 测试）` 存在事实性计数漂移。**已修正**为 `1903 单元测试 0 failures`（附 0.2 校正注记，引用 roadmap §当前基线为对齐基准）。属事实性 status/count 修正，非需求契约变更（§9 冻结条款允许）。
- ✅ **约束（line 81-84）**：`model/*.orm.xml` ask-first 保护区域、nop-entropy 父 POM、跨工程实体引用机制 B——与架构文档一致。

**结论**：product-scope 残余事实性陈旧仅成功指标计数一处（已修正）；其余里程碑/范围/约束段落均与 AGENTS.md / roadmap 一致，无事实性漂移。

---

## §基线分歧登记

> Phase 3 decision：核实中发现的**需求契约内容**（非事实性 status/count）分歧记入本段，交 MA1 审计，**不直改真相源**（§9 冻结条款；唯一例外 = 经人工批准的需求变更，本工作项不触发）。

### D-01：finance UC-FIN-09 与 UC-FIN-14 标题重复（"银行对账与未达账项"）

- **分歧点**：`docs/design/finance/use-cases.md` 中 UC-FIN-09（:165）与 UC-FIN-14（:269）标题完全相同（"银行对账与未达账项"），但断言详略不同（UC-FIN-14 为更详细的断言版本，含导入幂等/MATCHED-UNMATCHED-SUSPENSE/红冲等）。
- **三源对照**：
  - use-cases.md（L1 权威）：两条独立 UC，编号不同（09/14），标题相同，断言详略不同。
  - roadmap MA1 A1.4：`UC-FIN-09/14 标题重复[同为"银行对账与未达账项"]，0.2 清单化时裁决去重`——roadmap 已裁决两者同属 A1.4 切片。
  - owner doc `bank-reconciliation.md`：单一银行对账机制（无 09/14 之分）。
- **影响切片**：A1.4（finance-F4 银行对账）。
- **裁决**：**不直改 use-cases.md**（§9 冻结；编号不可复用，见 `use-case-authoring-guide.md §1`）。本清单将 09/14 均映射到 A1.4（roadmap 已裁决）。MA1 A1.4 审计时按 L1 逐字引用两条验收标准，若断言重叠则在该切片报告记录"断言重叠"观察；若需合并为单条 UC，属需求契约修订，须经人工批准（§9）。

### D-02：finance UC-FIN-04 与 UC-FIN-15 同主题（科目分摊）

- **分歧点**：`docs/design/finance/use-cases.md` 中 UC-FIN-04（:76，"FactsValidator 科目分摊"）与 UC-FIN-15（:298，"科目分摊(GL Distribution)"）主题相同（按 GlDistribution 规则拆行），UC-FIN-15 为更详细断言版本（含 ErpFinGlDistributionValidator getOrder）。
- **三源对照**：
  - use-cases.md（L1 权威）：两条独立 UC，编号不同（04/15），主题相同，断言详略不同。
  - roadmap MA1：UC-FIN-04 归 A1.1（F1 过账引擎），UC-FIN-15 归 A1.1 同切片（roadmap A1.1 UC 清单含 04/15）——已同属一切片。
  - owner doc `posting.md §FactsValidator` + `cost-center.md §ErpFinGlDistribution`：单一分摊机制。
- **影响切片**：A1.1（finance-F1 过账引擎与凭证链路）。
- **裁决**：**不直改 use-cases.md**（§9 冻结）。roadmap 已将 04/15 同归 A1.1，无切片归属冲突。MA1 A1.1 审计时按 L1 逐字引用两条验收标准；若需合并，属需求契约修订，须经人工批准。

### D-03：logistics use-cases.md UC 编号物理顺序错位（已归一化，非契约分歧）

- **记录**：归一化后 logistics/use-cases.md 物理顺序为 UC-LOG-01/02/03/04/05/07/06（UC-LOG-07 在 :65，UC-LOG-06 在 :79，物理位置颠倒）。**编号本身无跳号/无重复**（01-07 全覆盖），仅物理排版顺序非严格递增。非契约分歧，无需修订（编号不可重排，见 `use-case-authoring-guide.md §1`）。记录备查。

### product-scope 需求契约分歧

- **无**。product-scope 核实仅发现事实性计数漂移（line 80 单元测试数，已修正），未发现需求契约内容（业务域范围/里程碑框架/约束等）与 use-cases/owner doc 的分歧。



