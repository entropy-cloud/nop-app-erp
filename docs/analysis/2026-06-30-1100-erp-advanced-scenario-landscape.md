# ERP 高级业务场景全景分析：对照行业标准与新兴趋势

> 分析日期: 2026-06-30
> 类型: 联网调研 + 现有设计覆盖率对比
> 方法: 网络搜索（Oracle/NetSuite/SAP 官方模块清单 + Gartner/Forrester 趋势报告 + 13 个 erp-survey 项目源码回顾）→ 分类 → 对照 `nop-app-erp` 现有设计
> 信息来源: NetSuite 13 模块框架、Oracle 10 大 ERP 模块、SAP ERP 模块体系、GwayERP 12 模块、Project Salsa 13 模块、2025 AI/IoT/Blockchain 趋势多篇调研

## 背景

前序审计（`erp-survey` 覆盖 13 个开源 ERP + 7 个补充项目）聚焦于对照开源项目。本文在 es 调研基础上扩大到**行业标准 ERP 功能全景 + 新兴趋势**，引用 Oracle/NetSuite/SAP 三大商业 ERP 的模块划分作为参考框架，识别 nop-app-erp 当前设计中**未覆盖**或**仅部分覆盖**的业务场景。

### 行业参考框架

| 来源 | 模块数 | 核心覆盖 |
|------|--------|----------|
| NetSuite | 13 | 财务/采购/制造/库存/订单/仓库/SCM/CRM/PSA/劳动力/HR/电商/营销 |
| Oracle ERP | 10 | 财务/采购/风险/SCM/EPM/制造/CRM/HR/项目/分析 |
| SAP S/4HANA | 15+ | 财务/制造/SCM/HR/CRM/项目/资产/质量/EHS/分析/PLM |
| 行业综合（GwayERP/Salsa） | 12-13 | 同上 + BI/Workflow/集成 |

---

## 一、覆盖矩阵：nop-app-erp 当前设计 vs 行业标准

### 关键原则

> nop-app-erp **不存在"不做的高级业务场景"**。所有现有开源 ERP 中包含的功能必须全部覆盖。
> 
> 经 2026-06-30 追加调研（Axelor/AureusERP/IDURAR），此前计划 03 中标记为"设计骨架（延迟到客户触发）"、"P2 明确排除"、"自承认延迟（APS）"的**全部功能**在开源中均有成熟实现参考，必须升级为完整设计。

### 1.1 已完整覆盖（设计级，可直接实施）

| # | 场景/模块 | 设计文档 | 与行业标准对标 |
|---|----------|---------|---------------|
| 1 | 财务管理（总账/AP/AR/科目/多币种/多账套） | `finance/` 6 份文档 | ✅ 等同于 Oracle/NetSuite 财务模块 |
| 2 | 采购/寻源（RFQ/报价/供应商价格单/三单匹配/评分卡） | `purchase/` 4 份 + `purchase/supplier-evaluation.md` | ✅ 超越多数商业 ERP 的采购寻源深度 |
| 3 | 制造（BOM/工艺/工单 10态/MRP/CRP/**APS**/委外） | `manufacturing/` 4 份 + `crp.md` | ✅ 对标 SAP PP 模块。**注意：APSAps（OperationOrder 工序级排产）此前标记为延迟，根据 Axelor production（ManufOrder + OperationOrder + MrpLine）确认有完整开源参考，必须纳入完整设计** |
| 4 | 库存管理（三层模型/批次/序列号/追溯/调拨/盘点） | `inventory/` 3 份 + `consignment.md` | ✅ 超越 NetSuite 库存模块（不可变流水设计） |
| 5 | 质量管理（质检模板/NCR/CAPA/让步**/召回**） | `quality/` 3 份 + `recall.md` | ✅ 对标 SAP QM 模块 |
| 6 | 资产管理（卡片/折旧 3 法/资本化/处置/价值调整） | `assets/` 2 份 | ✅ 对标 SAP AM 模块 |
| 7 | 项目管理（项目/任务/工时/成本归集/预算） | `projects/` 4 份 | ✅ 对标 NetSuite PSA 模块 |
| 8 | 设备维护（计划性/响应性维护/停机） | `maintenance/` 2 份 | ✅ 对标 SAP PM 模块 |
| 9 | 销售/订单管理（订单/出库/发票/收款/合同/信用） | `sales/` 3 份 | ✅ 对标 Oracle Order Management |
| 10 | 费用报销/员工借款/备用金 | `finance/expense-claim.md` | ✅ 对标 Odoo hr_expense / SAP Concur |
| 11 | 资金管理/票据（承兑汇票/贴现/授信/现金预测） | `finance/treasury.md` | ✅ 中式特色自建（开源零覆盖） |
| 12 | 实体验证/业财一体（自动过账/凭证模板/多账套） | `finance/posting.md` + 支撑文档 | ✅ 超越多数商业 ERP 的 SPI 架构 |
| 13 | **CRM（线索→商机→转化报价单）** | `crm/README.md` | ✅ 对标 Axelor CRM（Lead→Convert→Opportunity）和 IDURAR（Lead→Quote→Invoice）。**此前标记为"骨架"，经确认 Axelor crm（70 Java 文件）和 IDURAR 均有完整实现，必须升级为完整设计**。需细化：活动历史（Event/Meeting） |
| 14 | **售后服务/客服工单** | 待补充 | ✅ 对照 Axelor helpdesk（Ticket + SLA + 团队）、AureusERP support（148 PHP 文件）、ERPNext support 顶层域。**此前标记为"P2 排除"，经确认三个开源项目均有独立服务台模块，必须纳入设计** |
| 15 | **运输管理 TMS（发运单+三层承运商 SPI）** | `logistics/README.md` | ✅ 此前标记为"骨架"。TMS（发运单+三层SPI）可保留 SPI 骨架形态，但需深化实施级设计 |
| 16 | **EDI/B2B 集成（格式 SPI + 信封状态机 + ASN）** | `architecture/b2b-integration.md` | ✅ 此前标记为"骨架"。EDI（格式SPI+信封状态机）可保留 SPI 骨架形态，但需深化实施级设计 |

### 1.2 此前标记为"P2 排除/延迟"——经开源验证，必须纳入完整设计

| # | 场景/模块 | 此前状态 | 开源参考 | 纠正方向 |
|---|----------|---------|---------|---------|
| 17 | **HRMS / 薪酬 / 考勤** | P2 排除 | Axelor human-resource（421 Java 文件，含 Payroll/Tax/Leave/Expense 完整 HR 模块）、AureusERP employees+recruitments+time-off（307+ PHP 文件）、Odoo hr/payroll/attendance | **必须纳入设计**。员工主数据/劳动合同/薪酬计算/个税/休假/考勤是 ERP 核心组成部分 |
| 18 | **APS / 工序级排产** | 自承认延迟 | Axelor production（344 Java 文件，OperationOrder 工序工单是有限产能排产的核心数据机构） | **必须纳入**。OperationOrder（工序工单）+ ManufOrder（主工单）+ WorkCenter（产能） 是 APS 的完整实现 |
| 19 | **DRP / 分销需求计划** | P2 排除 | Axelor supplychain（386 Java 文件，SCM 需求计划覆盖多级补货） | **必须纳入设计**。作为 MRP 的扩展 |
| 20 | **POS / 门店零售** | P2 排除 | Odoo point_of_sale（已有，但 erp-survey 尚未深度实测） | **需要补充调研**后再纳入设计 |
| 21 | **合同全生命周期** | 部分覆盖 | Axelor contract（83 Java 文件，含 ContractVersion + InvoicePlan + ConsumptionLine） | **必须升级**。抬头级合同 → 合同版本/开票计划/用量计费 |

### 1.3 部分覆盖（功能存在但深度不足）

| # | 场景/模块 | 当前覆盖 | 行业标准包含的额外能力 |
|---|----------|---------|----------------------|
| 22 | **仓库管理 WMS** | 仓位管理 + 作业类型 | 缺失：上架策略/拣选策略(波次/批量/区域)/补货策略/越库/码托管理/劳力计划。AureusERP inventories（294 PHP 文件）有 WMS 级参考 |
| 23 | **预算控制与预测** | 预算校验钩子(budget.md) | 缺失：全面预算编制、滚动预测、What-if 场景、预算版本管理 |
| 24 | **企业绩效管理 EPM** | 基础财务报表 | 缺失：财务合并/管理报表/多维分析(OLAP)/KPI 记分卡/盈利分析 |
| 25 | **供应链计划 SCP** | MRP(物料需求) | 缺失：需求预测(统计/ML)/S&OP/库存优化/安全库存计算 |
| 26 | **合规与风险管理** | 审计日志 + 职责分离 | 缺失：风险登记簿/合规检查清单/控制自评/SOX 合规工作流 |
| 27 | **数据分析/BI** | nop-report(基础报表) | 缺失：自助分析/可视化仪表盘/OLAP 多维分析/移动 BI/KPI 预警 |

### 1.4 中远期升级项（来自新增调研，此前未识别）

以下场景在行业标准 ERP 中常见，在 Axelor/AureusERP 等新调研项目中有完整实现参考：

以下场景在行业标准 ERP 中常见，但 nop-app-erp 当前设计**完全未覆盖**。按实施紧迫度分三组：

---

## 二、Group A — 建议纳入中期路线图（6 项）

### 2.1 高级仓储管理（Advanced WMS）

**场景**：大型制造/分销企业需要精细化的仓库作业管理，包括：
- 基于规则的上架策略（ABC 分类/体积/周转率导向）
- 多种拣选策略（波次拣选/批量拣选/区域拣选/单订单拣选）
- 补货策略（动态补货/低水位/订单驱动）
- 越库管理（Cross-docking：入库即出库，零停留）
- 码托与包装管理
- 劳力管理与效率分析
- RF/扫码/PDA 移动终端的实时指令

**行业对标**：SAP EWM（Extended Warehouse Management）、Oracle WMS、NetSuite WMS

**本项目当前**：仅基础仓位 + 作业类型，`inventory/state-machine.md` 的移动单模型面向普通库存移动，不支持 WMS 级的波次拣选和上架策略。

**证据**：`docs/design/inventory/` 目录无 WMS 相关文档

**建议归属**：`module-inventory` 或独立 `module-wms` 扩展

---

### 2.2 需求预测与 S&OP（Sales & Operations Planning）

**场景**：
- 基于历史销售的统计预测（移动平均/指数平滑/Holt-Winters）
- ML 增强预测（考虑季节/促销/市场因素）
- S&OP 月度流程（需求审查 → 供应审查 → 执行审查）
- 库存参数优化（安全库存/再订购点/订货量）

**行业对标**：SAP IBP、Oracle Demand Management

**本项目当前**：仅 `manufacturing/mrp.md` 解物料需求，无独立需求预测模型

**建议归属**：`manufacturing/demand-forecasting.md` 新增

---

### 2.3 全面预算管理与 EPM

**场景**：
- 多维度预算（科目/部门/项目/产品/区域）
- 滚动预测（季度/月度 Roll）
- What-if 场景模拟
- 预算版本管理（草稿→审批→冻结）
- 费用预算 → 收入预算 → 利润预算
- 实际 vs 预算分析

**行业对标**：Oracle EPM、SAP BPC

**本项目当前**：`finance/budget.md` 仅做预算校验钩子（简单的事前控制）

**建议归属**：`finance/budgeting-and-forecasting.md` 新增，增强预算模块

---

### 2.4 合同全生命周期管理（CLM）

**场景**：
- 合同模板库与条款库
- 谈判版本管理
- 电子签章/审批集成
- 自动到期提醒/续期
- 合同变更单（Amendment）
- 合规条款检查（数据保护/出口管制）
- 供应商合同 vs 客户合同 vs 员工合同统一管理

**行业对标**：SAP CLM、Oracle Procurement Contracts

**本项目当前**：仅 `purchase/README.md` 提及抬头级合同，无合同生命周期

**建议归属**：`module-master-data` 或独立扩展，`purchase/contract-management.md`

---

### 2.5 客户门户与供应商门户（Portal/Self-service）

**场景**：
- **客户门户**：订单追踪、发票查看、在线支付、售后工单提交、历史购买
- **供应商门户**：ASN 提交、PO 查看/确认、发票提交、绩效查看、资质更新
- **员工门户**：费用报销、请假、加班、工资单

**行业对标**：SAP Fiori、Oracle Self-Service

**本项目当前**：仅 `b2b-integration.md` 提及 ASN 入站（供应商侧单向），无客户/员工门户

**建议归属**：`module-customer-portal` / `architecture/portal-strategy.md`

---

### 2.6 产品生命周期管理（PLM）集成

**场景**：
- 产品开发阶段管理（概念→设计→验证→投产）
- 工程变更管理（ECN/ECO）
- BOM 版本控制与基线管理
- 文档管理（规格/AOTS/PPAP）
- ERP 域边界：从产品设计→ BOM → MRP 的打通

**行业对标**：SAP PLM、Siemens Teamcenter、PTC Windchill

**本项目当前**：`manufacturing/bom-and-routing.md` 仅覆盖 BOM 的使用侧（工单/BOM 快照），无 PLM 级的产品开发管理和工程变更

**建议归属**：`manufacturing/plm-integration.md` 或独立 `module-plm`

---

## 三、Group B — 建议远期评估（5 项）

### 3.1 实地服务管理（Field Service Management）

**场景**：设备安装/维修/巡检的派遣管理
- 技术人员排程（基于技能/位置/忙闲）
- 移动端工单接收/作业/签收
- 备件领用/退库联动
- 客户签字/结单
- 后续开票/保修联动

**关联项目**：与 `module-maintenance` 高度重叠（维护访问），但 FSM 关注外勤

**建议归属**：`maintenance/field-service.md` 或独立 `module-field-service`

---

### 3.2 环境、健康与安全（EHS）

**场景**：
- 安全事件/事故记录与调查
- 危险源识别与风险评估
- 职业健康监护
- 废弃物管理
- 合规报表（政府/监管机构）

**行业对标**：SAP EHS、Intelex

**建议归属**：独立 `module-ehs` 或并入 `module-quality`

---

### 3.3 可持续发展/ESG/碳管理

**场景**：
- 碳排放范围 1/2/3 追踪
- 碳足迹核算（产品/组织）
- 可持续发展报告（GRI/TCFD/ISSB）
- 绿色供应链（供应商碳绩效）
- 碳交易/碳补偿管理

**行业对标**：SAP Green Ledger、Salesforce Net Zero Cloud

**建议归属**：独立 `module-esg`

> 注：碳管理/ESG 是 2025-2026 ERP 行业增长最快的模块（多份调研提及）

---

### 3.4 收入确认 / ASC 606 / IFRS 15

**场景**：
- 多要素履约义务拆分
- 收入计划表（按时间/里程碑）
- 合同修改的收入影响重新计算
- 披露报表（收入分解表等）

**行业对标**：NetSuite SuiteBilling, Oracle Revenue Management

**建议归属**：`finance/revenue-recognition.md`

---

### 3.5 订阅管理与循环计费

**场景**：
- 订阅计划/定价模型定义
- 循环发票自动生成
- MRR/ARR/Churn 指标
- 用量计费（Usage-based billing）
- 订阅升级/降级/暂停/取消

**行业对标**：NetSuite SuiteBilling（自 2020 是独立模块）、SAP Subscription Billing

**建议归属**：独立 `module-subscription`（若产品转型含 SaaS 订阅场景）

---

## 四、Group C — 新兴技术趋势（监控，暂不投入设计）

以下趋势在 2025-2026 年 ERP 行业频繁提及，但属平台层能力或技术方向，非当前应当投入的业务场景设计：

| 趋势 | 描述 | 对本项目的影响 |
|------|------|---------------|
| AI/ML 集成 | 预测分析、异常检测、智能推荐 | 🔄 平台能力（不属业务设计），nop-rule 部分支撑，但需要数据科学集成 |
| IoT 集成 | 工业设备实时数据采集、预测性维护 | 🔄 需 IoT 平台对接，业务流程设计尚早 |
| Digital Twin | 供应链/工厂的数字孪生 | 🟡 远期（3-5年），与本项目无直接关联 |
| Blockchain | 供应链可追溯、多方信任 | 🟡 远期，且中国政策不明确 |
| RPA 自动化 | 机器人流程自动化 | 🔄 平台能力叠加，不单独设计模块 |
| 低代码/无代码扩展 | 用户自定义字段/流程/表单 | ✅ Nop Platform 天生具备（Delta 定制），已超越传统 ERP |
| 嵌入式分析 | 事务操作中的实时 AI 辅助 | 🔄 平台能力 |

---

## 五、覆盖汇总

| 层级 | 模块/场景数 | 说明 |
|------|------------|------|
| ✅ 完整设计（可直接实施） | 16 | 10 核心域 + P0 费用报销/资金票据 + **CRM完整** + **售后服务** + **运输TMS** + **EDI/B2B** |
| 📝 此前骨架/P2现确认纳入设计 | 5 | **HRMS/薪酬/考勤**（Axelor+Odoo+ERPNext 验证）、**APS/排产**（Axelor OperationOrder 验证）、**DRP**（Axelor supplychain 386 files）、**合同全生命周期**（Axelor contract 83 files）、**POS**（待补充调研） |
| 🔶 部分覆盖（深度不足） | 6 | 高级WMS / 需求预测S&OP / 全面预算EPM / 合规/风险管理 / 数据分析BI |
| 🟩 Group A 建议深化 | 4 | **高级WMS** / **需求预测S&OP** / **全面预算EPM** / **门户自理**（参考 Axelor supplier/client-portal） |
| 🟨 Group B 远期评估 | 5 | 实地服务FSM（Axelor intervention 参考）、EHS安全、ESG碳、收入确认、订阅计费 |
| 🔵 Group C 技术趋势（监控） | 7 | AI/IoT/Digital Twin/Blockchain/RPA/低代码/嵌入式分析 |

### 与 erp-survey 对比的补充说明

前序 erp-survey 覆盖了 13 个开源 ERP + 7 个补充项目中**已实现**的功能。2026-06-30 追加 3 个项目（Axelor/AureusERP/IDURAR）后：

- **此前标记为"骨架"或"P2"的全部功能在开源中有成熟实现**：CRM（Axelor 70 Java + IDURAR）、HRMS（Axelor 421 Java + AureusERP 307+ PHP）、售后服务（Axelor helpdesk + AureusERP support + ERPNext support）、APS（Axelor production 344 Java, OperationOrder 工序级排产）、DRP（Axelor supplychain 386 Java）、合同CLM（Axelor contract 83 Java）。
- **nop-app-erp 必须覆盖所有开源 ERP 中存在的业务功能**，不存在"不做的高级业务场景"。
- 部分深度不足的领域（高级WMS、全面预算EPM、需求预测S&OP、合规风险管理）在开源中同样薄缺，属于"需要持续深化"而非"不做"。

### 关键的独有差异化能力

以下功能是商业 ERP **未提供**但 nop-app-erp **已设计**的差异化点：

| 能力 | 商业 ERP | nop-app-erp | 优势 |
|------|---------|-------------|------|
| 三轴状态分离 | SAP/Oracle 单轴状态 | docStatus+approveStatus+posted | 避免状态爆炸 |
| 不可变库存流水 | SAP/Oracle 可修改库存 | 流水不可变，冲销走反向 | 审计追溯优势 |
| 类型安全 SPI 注册中心 | SAP/Oracle 配置表/反射 | `@Inject Map` 自动聚合 | 零代码扩展 |
| 核心实体零污染 | SAP/Oracle 外键耦合 | 弱指针反查 | 模块解耦 |
| 模型驱动开发 | SAP ABAP/手写 Java | XML 模型 → 代码生成 | 开发效率 |
| Delta 定制 | 需改源码或 Extension | XML Delta + 同路径覆盖 | 升级无忧 |

---

## 六、设计升级路线图

> **前置原则**：nop-app-erp 不存在"不做的高级业务场景"。此前标记为骨架/P2/延迟的功能，现统一升格为"待纳入完整设计"。

### Phase 1 — 紧前：此前骨架/P2/延迟项升级（立即启动）

| 优先级 | 功能 | 参考来源 | 建议设计文档 |
|--------|------|---------|-------------|
| 🔴 P0 | **CRM 完整设计**（Lead→Convert→Opportunity→活动历史） | Axelor crm（70 Java）、IDURAR（8504⭐） | `crm/README.md` → 升级为完整设计 |
| 🔴 P0 | **售后服务/客服工单**（Ticket + SLA + 支持团队） | Axelor helpdesk（26 Java）、AureusERP support（148 PHP） | 新增 `docs/design/customer-service/` 域 |
| 🔴 P0 | **HRMS（员工/合同/薪酬/考勤/休假）** | Axelor human-resource（421 Java，含 Payroll/Tax/Leave） | 新增 `docs/design/human-resource/` 域 |
| 🟡 P1 | **APS/工序级排产**（OperationOrder + 产能约束） | Axelor production（344 Java） | `manufacturing/crp.md` → 升级为 APS 完整设计 |
| 🟡 P1 | **合同全生命周期**（版本/开票计划/用量计费） | Axelor contract（83 Java） | `purchase/contract-management.md` 新增 |
| 🟡 P1 | **DRP 分销需求计划** | Axelor supplychain（386 Java） | `manufacturing/drp.md` 新增 |

### Phase 2 — 持续深化（后续周期）

| 深度加深项 | 参考来源 | 当前状态 |
|-----------|---------|---------|
| 高级 WMS（波次拣选/上架策略/越库） | AureusERP inventories（294 PHP） | `inventory/` 需新增 WMS 设计 |
| 全面预算 EPM（滚动预测/版本/What-if） | SAP BPC / Oracle EPM | `finance/budget.md` 需大幅扩展 |
| 需求预测 & S&OP | SAP IBP / Oracle Demand Management | `manufacturing/demand-forecasting.md` 新增 |
| 运输 TMS 深化（从骨架到实施级） | Metasfresh shipper.gateway | `logistics/` 深化 |
| EDI/B2B 深化（从骨架到实施级） | Odoo account_edi | `b2b-integration.md` 深化 |
| 客户/供应商门户 | Axelor client/supplier-portal | 新增门户设计 |

### Phase 3 — 远期评估（持续监控，客户需求触发）

- 实地服务 FSM（参考 Axelor intervention）
- POS 零售（需补充调研 Odoo POS）
- 收入确认 ASC 606
- EHS 安全与环境管理
- ESG 碳管理与可持续发展
- 订阅管理与循环计费
- PLM 产品生命周期集成

---

## 七、方法论说明

1. **商业 ERP 清单**：引用 Oracle 10 大 ERP 模块、NetSuite 13 模块、GwayERP 12 模块的分类法，与 SAP S/4HANA 模块文档交叉验证。
2. **新兴趋势**：引用 2025 年多篇调研（AI-Powered ERP、IoT/Blockchain in SCM、ESG ERP 模块趋势）。
3. **开源对照**：基于 erp-survey 的 16 个开源 ERP 项目（原始 13 个 + 2026-06-30 追加 3 个：Axelor/AureusERP/IDURAR）+ 7 个补充项目的源码实测。所有"开源空白/薄弱"的标注均已通过 grep 源码搜索验证。
4. **本项目的覆盖判断**：以 `docs/design/` 下设计文档存在性 + `model/*.orm.xml` 实体存在性为双重标准，不做过度推断。
5. **此前缺失的分类纠正**：计划 03 中标记为"设计骨架（延迟到客户触发）"的 CRM/TMS/EDI、"P2 明确排除"的 HRMS/售后/DRP/POS、"自承认延迟"的 APS——经 Axelor/AureusERP/IDURAR 源码实测确认均有成熟开源实现，**必须纳入 nop-app-erp 完整设计覆盖**。

---

## 八、参考来源

- Oracle: `https://www.oracle.com/cn/erp/erp-modules/`（10 大 ERP 模块）
- NetSuite: `https://www.netsuite.cn/resource/articles/erp/erp-modules.shtml`（13 模块）
- SAP: `https://www.sap.cn/resources/what-is-erp`（ERP 功能概述）
- GwayERP: `https://www.gwayerp.com/blog/12-types-of-erp-modules`（12 模块）
- Project Salsa: `https://projectsalsa.co.nz/blog/erp-modules-an-in-depth-look`（13 模块）
- AI/ML in ERP: WispyCloud `https://wispycloud.io/the-ai-driven-evolution-of-erp-systems-in-2025`（2025 AI ERP）
- IoT/Blockchain: 4acc `https://4acc.com/article/trends-in-erp/`（IoT/Blockchain/ML 趋势）
- 本仓 erp-survey: `docs/analysis/erp-survey/` 下 23 份调研报告（含本次新增 3 份）
- Axelor Open Suite: `https://github.com/axelor/axelor-open-suite`（Java, 957⭐, AGPL-3.0, 25 模块全栈 ERP）
- AureusERP: `https://github.com/aureuserp/aureuserp`（Laravel, 11293⭐, MIT, 30+ 插件）
- IDURAR ERP CRM: `https://github.com/idurar/idurar-erp-crm`（Node.js, 8504⭐, AGPL-3.0）
- 本仓设计文档: `docs/design/` 全目录
