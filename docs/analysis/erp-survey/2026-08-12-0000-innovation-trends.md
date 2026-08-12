---
调研日期: 2026-08-12
来源: 网络调研（2026 年开源 ERP 创新趋势）+ ~/sources/erp（14 个新增浅克隆）
分类: 创新设计调研总览（AI-native/低代码/APS/供应链/财务/BI 等 10 个维度）
状态: 已完成（趋势来自 2026 年公开资料，项目事实基于源码实测）
---

# 开源 ERP 创新设计调研总览（2026-08-12 批次）

> 本批调研围绕 **ERP 创新设计** 展开：先上网调研 2026 年开源 ERP 创新趋势，再按趋势维度补充下载 14 个此前未覆盖的开源项目到 `~/sources/erp/`（累计 38 个）。本文件是总览与趋势综合；每个项目另有独立报告 `2026-08-12-0000-<project>.md`。

## 1. 2026 年 ERP 创新趋势（网络调研综合）

### 1.1 AI 从"外挂 Copilot"走向"原生架构"（Agentic ERP）

- **ERPClaw**（2026-02 起）首次定义"开源 AI-native ERP"：AI 助手为主交互界面、动作层即 API、3,234 个 action 全部单事务 + 不可变 GL，宣称 Odoo/ERPNext 均为"AI-decorated bolt-on"（`erpclaw.ai/ai-native-erp/`）。
- **SAP 部署 200+ AI 代理**、**Odoo 嵌 Claude 并开放 MCP 协议**、**Twenty 从第一天起 AI-first 设计**——2026 年 >50% ERP 产品采用 AI 原生架构。
- **Deloitte 2026** 提出"Agentic ERP 三原则"：核心保持精简刚性（合规/审计），周边应用层允许 agent 灵活实验；UI 与核心解耦。
- **AI 就绪评估标准**（DeployMonkey 2026 排名）：强 API 读写能力 + 源码透明 + 文档化的扩展模型 → Odoo Community 第一、ERPNext 第二。
- **对 nop-app-erp 的含义**：`module-notify` 与审批流应预留 agent/action 化接口；"动作层即 API"（action 即 GraphQL BizMutation + 审计）与 Nop 的 BizModel 架构天然契合，无需外挂。

### 1.2 可组合（Composable）与 Headless

- ERP 从"大而全单体"转向**模块化、API 驱动、可组合**：核心保持业务规则刚性，外围应用层通过 API/Webhook 灵活组合（Odoo headless 路线、Medusa workflows-sdk、Fleetbase 扩展机制）。
- **对 nop-app-erp 的含义**：18 域 DAG 独立部署 + `I*Biz` 接口调用已是可组合架构；Medusa 的 workflows-sdk（长流程编排中间件）与 Fleetbase 的扩展索引机制可作编排层参考。

### 1.3 低代码/无代码平台化

- 开源低代码平台（NocoBase/Baserow/Frappe/Grist/Appsmith 等）与 ERP 深度协同："核心是 ERPNext，辅助流程是 Appsmith"成为常见架构。
- **对 nop-app-erp 的含义**：与 Nop Platform 的模型驱动 + Delta 可逆定制对照——Baserow 的自研公式语言（ANTLR）、Frappe 的语义驱动模型定义（受 Semantic Web 启发）均值得对照阅读。

### 1.4 供应链智能化：APS 排产 + 需求预测 + 仿真

- **frePPLe** 是开源 APS 的标杆：C++ 约束求解 + 约束理论（围绕瓶颈的拉动式计划）+ ML 时间序列预测 + 与 Odoo 等 ERP 对接。
- 2026 年趋势：MRP 从"单次确定性计算"走向"多场景 what-if 仿真"（此前 ERP5 调研已识别该缺口）。
- **对 nop-app-erp 的含义**：`module-aps` 的工序级排产（有限产能、正反向）与 frePPLe 的约束求解器设计对照；`2026-07-20-post-survey-strategic-gaps.md` 中 P1 的"MRP/DRP 仿真引擎"缺口可参考 frePPLe 的 forecast/simulation 结构。

### 1.5 数据主权 + 文档 AI + 纯文本记账等财务创新

- 数据主权（GDPR/NIS2/本地部署）继续驱动开源采纳（2026 开源 ERP 市场 $5.31B，CAGR 9.66%）。
- **Paperless-ngx** 展示"文档→数据"的 AI 消费管道（OCR + 自动分类 + 邮件摄取），是 AP/应收票据自动化的参考。
- **Beancount** 代表纯文本复式记账 DSL（严格平衡校验 + 插件 + 查询引擎），是"记账内核可审计"的极端简洁范例。

## 2. 本批次 14 个项目一览

| # | 项目 | 创新维度 | 技术栈 | License | 核心创新点（源码实测） |
|---|------|---------|--------|---------|----------------------|
| 1 | **ERPClaw** | AI-native ERP | Python + SQLite/PG | GPL-3.0 | AI 为主界面；action 即 API（14 域 496+ actions）；GL 不可变（冲销=反向分录）；模块 registry 按需 sparse checkout |
| 2 | **Twenty** | AI-first CRM | TS + NestJS + React | AGPL-3.0 | 对象以代码定义（defineObject SDK）；GraphQL-first；Agents/Workflows 原语 |
| 3 | **Frappe** | 低代码框架 | Python + MariaDB | MIT | 元数据驱动（语义驱动）；模型自动生成 REST API；无代码 Report Builder |
| 4 | **Baserow** | 无代码数据库 | Django + Vue/Nuxt + PG | MIT（open-core） | 自研 BaserowFormula（ANTLR G4）；AI 助手 Kuma；应用构建器 + 自动化 + 仪表盘 |
| 5 | **frePPLe** | APS 排产 | Python/Django + C++ + PG | AGPL-3.0 | 约束理论求解器；ML 需求预测（mlforecast）；与 Odoo 对接；报表管理器 |
| 6 | **OpenBoxes** | WMS/供应链 | Groovy/Grails + MySQL | EPL-1.0 | 库存审计快照 + 周期盘点 + 批次/序列 + 补货/预约/履行（医疗供应链背景） |
| 7 | **Fleetbase** | 物流平台 | PHP/Laravel + Ember.js | AGPL-3.0 | 模块化扩展机制（extensions indexer）；ledger 记账包；IAM 引擎；遥测集成 |
| 8 | **InvenTree** | 库存/制造 | Python/Django + React | MIT | 插件系统；机器学习集成；报告生成；批次/序列号追踪 |
| 9 | **Beancount** | 纯文本记账 | 纯 Python | GPL-2.0 | 文本 DSL；严格平衡校验 parser；插件系统；查询引擎 |
| 10 | **Paperless-ngx** | 文档 AI | Django + Angular + PG | GPL-3.0 | OCR 消费管道；自动分类打标；邮件摄取；AI 模块；审计日志 |
| 11 | **n8n** | 工作流自动化 | TS/Node.js | fair-code | AI agents + 可视化工作流；1500+ 集成；human approvals；RBAC + 审计 |
| 12 | **Apache Superset** | 现代 BI | Flask + React | Apache-2.0 | 轻量语义层；Embedded SDK；SQL Lab；MCP 服务 |
| 13 | **Snipe-IT** | 资产追踪 | PHP 8.2 + Laravel 12 | AGPL-3.0 | 折旧计算；SCIM/LDAP/SAML；自定义字段集；操作审计轨迹 |
| 14 | **Medusa** | Headless 商务 | TS monorepo | MIT（open-core） | 30+ commerce 模块微服务化；workflows-sdk 长流程编排；事件总线 |

## 3. 对 nop-app-erp 的借鉴映射（按域）

| 本仓库域/主题 | 首选参考 | 借鉴点 |
|--------------|---------|--------|
| **aps（排产）** | **frePPLe** | 约束理论求解器结构、瓶颈识别、ML 预测与排产的衔接、与 ERP 的对接层（erpconnection） |
| **crm** | **Twenty** | 对象以代码定义（对比 nop 模型驱动）、GraphQL-first API、agents 作为 CRM 一等公民 |
| **platform 对照** | **Frappe / Baserow** | 元数据驱动 vs 模型驱动（Nop）；公式语言设计（BaserowFormula vs Nop XLang）；语义建模启发 |
| **notify / 审批流** | **n8n / Fleetbase** | AI 工作流编排、human-in-the-loop 审批门、扩展机制与事件总线 |
| **看板子系统** | **Apache Superset** | 轻量语义层、Embedded SDK（对比 AMIS 渲染）、MCP 服务暴露 |
| **finance（记账内核）** | **Beancount / ERPClaw** | 不可变 GL + 反向冲销（ERPClaw）、严格平衡校验 + 纯文本可审计（Beancount）——与 nop 凭证引擎对照 |
| **inventory / logistics** | **OpenBoxes / Fleetbase / InvenTree** | 库存审计快照与周期盘点、批次序列追踪、模块化扩展 + ledger 包 |
| **assets** | **Snipe-IT** | 折旧计算、自定义字段集、SCIM 身份集成、操作审计轨迹 |
| **b2b / drp** | **Medusa** | headless 可组合架构、workflows-sdk 长流程编排、模块化微服务拆分 |
| **finance（AP 自动化）** | **Paperless-ngx** | 文档消费管道（OCR→分类→入账）作为发票/票据自动化的前端 |
| **制造/库存** | **InvenTree** | 插件系统 + 机器学习（预测/图像识别）集成模式 |

## 4. 与平台能力的边界说明

- **工作流/BI 已由平台覆盖**：Nop Platform 自带 `nop-wf`（工作流引擎，含 `nop-wf-ai`）、`nop-task`（任务编排）、`nop-report`（报表系统）、`nop-datav`（数据可视化）。本批次的 **n8n** 与 **Apache Superset** 与平台功能重复，不作为能力缺口，仅作为**设计参考**保留（AI 工作流编排的 human-approval 模式、BI 语义层与 Embedded 模式）。
- **AI 能力已由平台覆盖**：`nop-ai` 提供 LLM 接入。ERPClaw/Twenty 参考的是**业务架构**（action 层、不可变 GL、对象定义）而非 AI 基础设施。

## 5. 结论

2026 年开源 ERP 创新集中在 **AI 原生架构（action 层化 + 不可变内核）、可组合/headless 架构、低代码平台化、APS 智能排产、文档 AI** 五条主线。nop-app-erp 的 18+1 域模型、3 层过账引擎、Delta 可逆定制已处于行业前沿（详见 `2026-07-20-post-survey-strategic-gaps.md` 的覆盖矩阵）；本批次补充的参考主要服务于：aps 排产深化、crm 现代化、记账内核审计性、文档驱动的 AP 自动化、以及平台架构对照。

## 6. 关键证据文件

- 各项目独立报告见 `2026-08-12-0000-<project>.md`（14 份）
- 下载清单与索引：`~/sources/erp/README.md`、`~/sources/erp/INDEX.md`
