---
调研日期: 2026-06-30
来源: docs/analysis/erp-survey/（20+ 项目源码调研）+ docs/design/ 各域 owner doc + docs/architecture/ 技术决策
状态: 完成
---

# 评分表等复杂功能的技术实现栈分析

## 目的

回答两个问题：
1. 项目中涉及"评分/评估/打分"的功能到底有哪些？具体技术实现路线是什么？
2. 对比 erp-survey 下各开源产品，我们在评分引擎/规则引擎/状态机领域处于什么位置？

---

## 一、项目中"评分表"类功能全景

"评分"不是一张表，而是**分布在 5 个业务域、7 种完全不同的评分语义**的功能族：

| # | 功能 | 所属域 | 核心语义 | 评分方法 | 设计文档 |
|---|------|--------|----------|----------|----------|
| 1 | **线索评分 (Lead Scoring)** | CRM | Lead 质量打分 → 自动转商机 | LOOKUP/FORMULA/BOOLEAN 可配置规则 + 归一化 0-100 | `crm/lead-scoring.md` |
| 2 | **供应商评分卡 (Supplier Scorecard)** | purchase | 供应商绩效评分 → 评级/AVL 联动/RFQ 控制 | 维度×公式×权重 + 变量从业务 path 取值 | `purchase/supplier-evaluation.md` |
| 3 | **满意度调查 (CSAT/NPS/CES)** | customer-service | 工单解决后客户打分 | 三种行业标准评分体系 (5星/0-10/1-7) | `customer-service/csat.md` |
| 4 | **风险评分 (Risk Score)** | quality | likelihood × severity → riskScore | 数值乘积（简单） | `app-erp-quality.orm.xml` ErpQaRiskRegister |
| 5 | **物流优先级评分** | logistics | 运输分配加权排序 | 运费×时效加权 | `logistics/delivery-window.md` |
| 6 | **供应商可靠性评分** | drp | 准时率/稳定性/准确率/合格率综合 | 多因素综合（DRP 补货参数联动） | `drp/use-cases.md` |
| 7 | **价格竞争力评分** | purchase | RFQ 比价综合排序 | 价格+交期+质量评分 standing | `purchase/requisition.md` |

### 1.1 CRM 线索评分（最复杂）

- **实体**：`ErpCrmLeadScoreConfig`（规则配置头）+ `ErpCrmLeadScoreConfigLine`（准则行）+ `ErpCrmLeadScore`（评分记录头）+ `ErpCrmLeadScoreLine`（评分明细行）
- **评分方法**：LOOKUP（查值表映射）/ FORMULA（公式-预留规则引擎）/ BOOLEAN（是否匹配）
- **触发**：手动 / 字段变更异步 / 定时批量
- **联动**：总分 ≥ autoQualifyThreshold → 自动 QUALIFY 线索（docStatus NEW→QUALIFIED）
- **开源参考**：ERPNext `lead_scoring_criteria` + `lead_scoring_result`；Odoo `crm_lead_score`

### 1.2 供应商评分卡（第二复杂）

- **实体**：`ErpPurSupplierScorecard`（评分周期）+ `ErpPurSupplierScorecardCriteria`（评分维度）+ `ErpPurSupplierScorecardVariable`（评分变量）
- **评分方法**：维度(criteria) × 公式(formula) × 权重(weight)，公式引用 variable 从业务 path 取值
- **联动**：GREEN→正常询价 / YELLOW→warn / RED→hold/prevent → 自动 SUSPEND 供应商
- **开源参考**：ERPNext 8-doctype 完整体系（`supplier_scorecard*`）

### 1.3 满意度调查（独立简单）

- **实体**：`ErpCsSurvey`（工单→调查关联，独立 token）
- **评分方法**：CSAT 1-5 / NPS 0-10 / CES 1-7，纯录入不计算
- **联动**：RESOLVED 触发 → 延迟发送 → CSAT 作为客服绩效 KPI

---

## 二、技术实现栈：引擎与框架选型

### 2.1 核心决策原则

从 `docs/architecture/` 和 `docs/analysis/erp-survey/` 提炼的工程约束：

| 原则 | 来源 | 依据 |
|------|------|------|
| **不使用 Activiti/Camunda BPMN** | `document-engine.md` | 13 个 ERP 项目中无一使用标准 BPMN 引擎 |
| **声明式 DSL/配置 替代 硬编码 Java** | `document-engine.md`, `supplier-evaluation.md`, `business-design-takeaways.md` | 转换规则、评分公式、科目映射均用 DSL |
| **三轴状态分离** | `document-engine.md` | docStatus + approveStatus + postedStatus 正交独立 |
| **异步过账不阻塞主事务** | `document-engine.md`, `business-design-takeaways.md` | Metasfresh EventBus 模式 |
| **评分计算不出域** | `lead-scoring.md` | 降低延迟与耦合 |
| **评分历史只追加不覆盖** | `lead-scoring.md`, `supplier-evaluation.md` | 周期快照非实时累加 |

### 2.2 技术引擎清单

| 能力 | 选用引擎 | 用途 | 设计文档 |
|------|----------|------|----------|
| **规则引擎** | **nop-rule** | 科目映射决策表、凭证模板、评分公式、审批条件、容差校验 | `feature-inventory.md:114` |
| **状态机引擎** | **DocumentEngine + 声明式 DSL** | 所有业务单据状态流转（draft→prepared→completed→closed） | `document-engine.md` |
| **审批流程** | **nop-wf**（可选） | 仅用于需要人工决策的多级审批，不替代核心状态机 | `approval-framework.md` |
| **异步消息** | **nop-stream / nop-message** | EventBus 异步过账、质检事件投递 | `document-engine.md:454` |
| **定时任务** | **nop-job** | 批量评分、兜底过账扫描、周期评估、折旧计算 | `feature-inventory.md:113` |
| **动态实体** | **nop-dyn** | 运行时新增业务表（调查问卷、自定义审批单） | `customization-capabilities.md` |
| **计算字段** | **@BizLoader** | 派生属性（未交量、当前库存） | `customization-capabilities.md` |
| **扩展字段** | **nop-sys EAV** | 运行时加字段不改表 | `customization-capabilities.md` |
| **Delta 定制** | **Nop 平台机制** | 基线覆盖、升级兼容 | `customization-capabilities.md` |
| **规则/公式 DSL** | **nop-rule + 自定义表达式** | 评分公式（FORMULA）、凭证模板占位符、转移规则 | `supplier-evaluation.md:77`, `lead-scoring.md:49` |

### 2.3 评分功能的具体实现路线

```
评分功能实现路线（按复杂度）
        │
        ├─ LEVEL 1: 纯录入（满意度调查）
        │      └─ ErpCsSurvey 实体 + 状态机（PENDING→SENT→COMPLETED）
        │
        ├─ LEVEL 2: 数值计算（风险评分）
        │      └─ riskScore = likelihood × severity（Entity 方法）
        │
        ├─ LEVEL 3: 可配置评分规则 + 归一化（线索评分）
        │      ├─ ErpCrmLeadScoreConfig（规则配置头）
        │      ├─ ErpCrmLeadScoreConfigLine（准则行：LOOKUP/FORMULA/BOOLEAN）
        │      ├─ 评分引擎：遍历 configLines → 查值表/公式/布尔 → 归一化
        │      ├─ 异步触发（nop-message）+ 定时兜底（nop-job）
        │      └─ 联动：总分 ≥ threshold → nop-wf/状态迁移
        │
        └─ LEVEL 4: 多维度公式+变量+周期快照+业务联动（供应商评分卡）
              ├─ ErpPurSupplierScorecard（周期头）
              ├─ ErpPurSupplierScorecardCriteria（维度 × weight × formula）
              ├─ ErpPurSupplierScorecardVariable（variable × path 取值）
              ├─ formula 用 nop-rule 引擎/DSL 表达
              ├─ 周期评估定时任务（nop-job）
              └─ 联动：standing=RED → 自动 SUSPEND 供应商（I*Biz 跨域）→ RFQ 校验阻塞
```

---

## 三、与 erp-survey 开源产品的对比分析

### 3.1 评分功能覆盖矩阵

| 项目 | 线索评分 | 供应商评分卡 | 满意度 | 风险评分 | 价格评分 | 评分引擎类型 |
|------|:--------:|:-----------:|:------:|:--------:|:--------:|--------------|
| **本项目 (nop-app-erp)** | ✅✅ 设计 | ✅✅ 设计 | ✅✅ 设计 | ✅ 设计 | ✅ 设计 | 可配置规则 + nop-rule DSL |
| **ERPNext** | ✅✅ 源码 | ✅✅ 8-doctype | ⚠️ | — | ⚠️ | 配置驱动准则 |
| **Odoo** | ✅✅ 互动评分 | ⚠️ | ✅✅ | — | ✅ | Python 方法 |
| **Axelor** | ✅✅ Lead→Opp | ⚠️ | — | — | — | Java 方法 |
| **赤龙** | — | — | — | — | ✅ | — |
| **管伊佳** | — | ⚠️ | — | — | ⚠️ | — |
| **Metasfresh** | — | ✅ | — | — | — | Java 方法 |
| **iDempiere** | — | ✅ | — | — | — | Java 方法 |

> ✅✅ = 完整实现 / ✅ = 支持 / ⚠️ = 基础 / — = 无

### 3.2 规则引擎/评分引擎横向对比

| 项目 | 规则表达方式 | 配置化程度 | 运行时扩展 | 我们的位置 |
|------|-------------|-----------|-----------|-----------|
| **Odoo** | Python `@api.depends` + 方法覆盖 | 需改代码 | 💡 模块热加载 | 比 Odoo 更配置化 |
| **ERPNext** | Doctype 配置准则 (scoring_field + score) | 🟢 管理员可配 | ❌ 无独立明细行 | 借鉴 ERPNext 8-doctype 范式 |
| **Axelor** | Java 实体方法 | 需改代码 | ❌ | 不如我们灵活 |
| **iDempiere** | Java + MProcess 调用 | 需改代码 | ❌ | 不如我们灵活 |
| **本项目** | **nop-rule DSL / xbiz 配置** | 🟢 GUI 可配 | 🟢 Delta 定制 + 扩展字段 + nop-dyn | **最优** |

### 3.3 状态机/流程引擎横向对比

| 项目 | 核心机制 | BPM 引擎 | 灵活性 |
|------|----------|----------|--------|
| **Odoo** | Python 状态字段 + 方法 | 不使用 | 中 |
| **ERPNext** | 状态字段 + on_submit 钩子 | 不使用 | 中 |
| **iDempiere** | DocumentEngine + AD_Workflow | 自研 OMG 规范 | 高（但复杂） |
| **Tryton** | 声明式 `_transitions` + `@transition` | 不使用 | 高（简洁） |
| **星云** | 状态字段 + warm-flow 审批 | warm-flow 可选 | 中-高 |
| **赤龙** | 状态字段 + 审批触发 | 不使用 | 中 |
| **本项目** | **DocumentEngine + 声明式 DSL + nop-wf 可选** | nop-wf 可选 | **高（组合最优）** |

### 3.4 技术栈成熟度对比

| 维度 | Odoo | ERPNext | iDempiere/Metasfresh | 赤龙/管伊佳 | **本项目** |
|------|------|---------|---------------------|------------|-----------|
| 技术架构 | Python/Monolith | Python/Monolith | Java Modular | SpringBoot | **Nop 模型驱动** |
| 定制能力 | 模块安装+继承 | Doctype 配置 | Delta 差量 | 改源码 | **Delta+EAV+Dyn** |
| 评分引擎配置 | 代码覆盖 | Doctype 配置 | Java 方法 | 代码硬编码 | **规则引擎 DSL** |
| 状态机 | 代码 | 代码+钩子 | 自研引擎 | 代码 | **声明式 DSL** |
| 业财一体 | 基础 | ✅ on_submit | ✅ 成熟 | ✅ 完整 | **设计借鉴最优** |
| 多租户 | ✅ Cloud | ✅ | ✅ AD_Client | ⚠️ | ✅ nop-auth |
| 多币种 | ✅ | ✅ | ✅✅ 双币种 | ⚠️ | ✅ 设计 |

---

## 四、现有调研报告的缺口与补充建议

### 4.1 已覆盖情况

docs/analysis/erp-survey/ 已有 **24 份调研报告**，覆盖了：
- 13 个初始项目 + 7 个补充项目 + 3 个追加项目 (Axelor/AureusERP/IDURAR) + 1 个子域覆盖分析
- ✅ 业财一体过账机制（`business-design-takeaways.md`）
- ✅ 流程引擎 vs 状态机（`workflow-vs-state-machine.md` + `workflow-vs-state-transition.md`）
- ✅ 模块拆分比较（`module-split-comparison.md`）
- ✅ 子域覆盖缺口（`subdomain-opensource-coverage.md`）

### 4.2 缺口1：评分引擎/规则引擎专项分析

现有报告**未专门分析**各开源项目的评分引擎/规则引擎架构。建议补充：

| 缺失维度 | 推荐切入点 | 需要查看的源码 |
|----------|-----------|---------------|
| ERPNext `lead_scoring_criteria` 完整解析 | 能否直接用？不足在哪？ | `erpnext/crm/doctype/lead_scoring_criteria/` |
| Odoo `crm.lead.scoring` 互动评分 | 活动事件如何自动加分？ | `crm/models/crm_lead.py` scoring 相关 |
| ERPNext `supplier_scorecard` 8-doctype | 变量 path 如何从业务取值？ | `erpnext/buying/doctype/supplier_scorecard_*/` |
| Axelor 规则引擎 | Axelor 有自己的规则引擎吗？ | `axelor-core/src/main/java/com/axelor/meta/` |

### 4.3 缺口2：CRM 评分与 CPQ 配置引擎完整方案

现有 `crm/lead-scoring.md` 和 `crm/cpq.md` 依赖 erp-survey 调研，但 survey 报告未详细展开 Salesforce CPQ、ERPNext CRM/Odoo CRM 的评分和配置规则引擎源码分析。建议：
- 从 ERPNext `crm/doctype/lead/` 和 `crm/doctype/opportunity/` 源码读取评分触发逻辑
- 从 Odoo `crm/models/crm_lead.py` 读取 `score_total` 计算链路

### 4.4 缺口3：各项目的"公式表达式引擎"实现方式

多个设计文档提到"用 nop 规则引擎/DSL 表达公式"，但现有调研未分析各开源产品如何实现公式表达式：

| 项目 | 公式实现 | 我们的借鉴 |
|------|----------|-----------|
| ERPNext | Python eval（有安全风险） | 应避免 |
| Odoo | `fields.Float(compute=...)` + `@api.depends` | 代码级，不适用 |
| iDempiere | MFormula + JSVC | 过重 |
| 赤龙 | Java 代码 | 硬编码 |
| **推荐 nop** | **nop-rule + 决策表 + 自定义表达式函数** | 安全+可配 |

---

## 五、总结

### 技术选型结论

1. **评分引擎不自研**：复用 nop-rule 规则引擎表达评分公式 + 决策表
2. **状态机不引入 BPM**：DocumentEngine + 声明式 DSL（参考 Tryton `_transitions`）
3. **审批可选**：nop-wf 仅用于多级审批，不替代核心状态机
4. **异步过账**：nop-stream/nop-message EventBus（参考 Metasfresh）
5. **跨域不跨 ORM**：走 I*Biz 接口（Maven DAG）

### 相对于开源产品的独特优势

| 优势 | 说明 |
|------|------|
| **模型驱动** | 评分规则、凭证模板、状态转换用 DSL/配置，零改代码 |
| **Delta 定制** | 评分公式可被客户 Delta 覆盖，升级兼容 |
| **评分历史可追溯** | 行级快照（独立明细行），ERPNext 无此能力 |
| **三轴状态分离** | docStatus + approveStatus + postedStatus，赤龙范式 |
| **异步不阻塞** | 评分和过账都是事件驱动，不阻塞主单据事务 |
| **跨域协作整洁** | 评分不出域，跨域走 I*Biz 事件 |

### 建议下一步行动

1. 对 ERPNext `supplier_scorecard` 8-doctype 做一次深度源码分析，确认 formula 引擎的边界情况
2. 对 Odoo `crm_lead_score` 互动评分做溯源，确认活动事件到评分的映射逻辑
3. 验证 nop-rule 能否满足评分公式 DSL 的需求（E2E 原型测试）
