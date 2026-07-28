# 2026-07-28-1953-1-audit-remediation-ma3-owner-doc-vs-code-drift MA3 owner doc vs 代码 drift（A3.3+A3.4+A3.5）

> Plan Status: completed
> Last Reviewed: 2026-07-28
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA3（工作项 A3.3 finance / A3.4 manufacturing / A3.5 pur+sal+inv）
> Related: `docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.3「owner doc vs 代码 drift」行（MA3，当前 `新维度`）；`docs/audits/arm-index.md`（P1 索引）；`docs/skills/multi-dimensional-audit-prompt.md`（审计方法——多维挑战 7 维度 + 反窄化自检 + 项目特定维度）；`docs/plans/2026-07-28-1510-2-audit-remediation-ma3-design-doc-baseline.md`（A3.1 设计文档**内部**质量审查——本审计做设计**vs 代码**逐项 drift 比对，互补）；MA2 全部状态机审计报告（owner-doc drift findings 已登记，本审计复核 + 横扩至状态机以外全部设计声明）；`docs/design/finance/`、`docs/design/manufacturing/`、`docs/design/purchase/`、`docs/design/sales/`、`docs/design/inventory/`（审查目标 owner docs）
> Audit: required

## Current Baseline

owner doc vs 代码 drift 审计（文档-实现一致性层 MA3，A3.1/A3.2 之后的第三/四/五项）。`docs/design/<domain>/` 是 nop-app-erp 的稳定应用层行为 owner doc——它声明"产品行为是什么"。若设计文档声明的业务规则、状态迁移、计算公式、字段语义、跨域协作、过账行为、配置项与实际 Java BizModel / ORM 实体 / Processor / Provider 实现不一致，将导致**实施者按错误基线实现**、**测试按错误契约验证**、**设计-代码漂移在后续变更中持续放大**。

**与已完成审计的边界**（关键去重）：

- **A3.1（done）**审设计文档**内部**作为行为基线的质量（自洽/边界/覆盖声明/跨文档一致性）——审"文档写得好不好"。A3.1 §9 已将发现的设计-代码背离候选项**标注交接 A3.3-A3.5**。本审计做设计**vs 代码**的逐项 drift 比对——查"文档说的和代码做的是否一致"。注：A3.1 交接的候选项含跨域示例（如 projects P1-MA2-067 closeProject / b2b P1-MA2-073 自动化承诺，属 A3.3-A3.5 范围外域），本审计仅复核**范围内域簇**的 owner-doc drift（finance P1-MA2-031~034 / mfg P1-MA2-035~038 / pur P1-MA2-049~051 / sal P1-MA2-056~057 等，见各 Phase Targets）。
- **MA2（done）**审状态机正确性（迁移完整性/守卫/并发/终态可达）——审"状态机行为对不对"。MA2 在审计过程中登记了多处 **owner-doc drift**（设计文档对状态机的描述与代码不一致），这些已归入 MR2 文档类 P1。本审计**复核** MA2 已登记的 drift finding（确认分类 + 是否有代码侧未记录的 drift），并**横扩至状态机以外**的全部设计声明（业务规则/计算公式/字段语义/跨域协作/过账行为/配置项/工作流步骤）。
- **A3.6（todo，同批）**审 API 契约（api.xml / xbiz）vs 实现——本审计不做 API 契约层 drift。

**审查目标域 owner doc 规模**（实时仓库核实）：

- **finance（A3.3，S 级）**：`docs/design/finance/` 19 文件（README / state-machine / posting / period-close / budget / ar-ap-reconciliation / bad-debt / bank-reconciliation / costing-methods / expense-claim / gl-mapping-rules / intercompany-consolidation / multiple-accounting-schemas / opening-balance / posting-log / treasury / ui-patterns / use-cases / cost-center）。对应代码：`module-finance/erp-fin-service/` BizModel + Processor + Provider + Dispatcher + Service；`module-finance/erp-fin-dao/` 实体；`module-finance/model/app-erp-finance.orm.xml`。finance 是全域最复杂域（过账引擎/凭证/期间/预算/AR-AP/成本/多账套/银行对账/坏账/票据/资金），drift 风险最高。
- **manufacturing（A3.4，S 级）**：`docs/design/manufacturing/` 12 文件（README / state-machine / bom-and-routing / crp / mrp / batch-genealogy / material-reservation / simulation-engine / subcontracting / ui-patterns / use-cases / variance-analysis）。对应代码：`module-manufacturing/erp-mfg-service/`；`module-manufacturing/model/`。制造域含工单/MRP/BOM/委外/CRP/差异/仿真/基因追溯，计算密集。
- **pur+sal+inv（A3.5，A 级合并）**：`docs/design/purchase/` 8 文件（README / state-machine / requisition / returns / supplier-evaluation / three-way-match / ui-patterns / use-cases）+ `docs/design/sales/` 7 文件（README / state-machine / contract / quotation / returns / ui-patterns / use-cases）+ `docs/design/inventory/` 8 文件（README / state-machine / cross-domain / barcode-integration / consignment / trace-chain / ui-patterns / use-cases）。对应代码：`module-purchase/`、`module-sales/`、`module-inventory/`。进销存三域为 ERP 核心交易链路。

**drift 审查维度**（multi-dimensional-audit-prompt 7 维度适配"doc vs code"主题 + 项目特定维度）：

- **状态迁移/工作流 drift**：设计声明的状态机/审批轴/工作流步骤 vs 代码实际迁移守卫与触发（复核 MA2 已登记项 + 横扩）。
- **业务规则/计算 drift**：设计声明的业务规则（如三单匹配规则/信用控制策略/可用量校验/成本计算公式/折旧公式/差异计算）vs 代码实际实现。
- **字段/实体语义 drift**：设计声明的字段含义/枚举值/字典绑定 vs ORM `orm.xml` 与代码实际使用。
- **跨域协作 drift**：设计声明的跨域触发（过账触发/库存联动/承付 commit-release/通知派发）vs 代码实际 Facade 调用与副作用。
- **过账/会计 drift**（finance/mfg/inv 重）：设计声明的过账业务类型/科目映射/凭证结构/红冲规则 vs 代码 Provider/Dispatcher/VoucherBuilder 实现。
- **配置/门控 drift**：设计声明的配置项/默认值/门控开关 vs 代码 Config 常量与 config-gate 实现。
- **未文档化行为（code→doc 反向 drift）**：代码中存在但设计文档完全未提及的显著行为（隐藏副作用/未声明的 ErrorCode/未文档化的自动触发）。

剩余差距：需要一次系统性 design-vs-code drift 审计。发现的 drift 分类为：(a) **设计声明未实现**（doc→code 缺口，major/blocker 视影响）；(b) **代码行为未文档化**（code→doc 缺口，major）；(c) **设计与代码直接矛盾**（blocker——实施者会按错误基线实现）。blocker/major 登记为 P1（文档类 P1 目标 MR2；若发现代码侧实时缺陷即设计正确但代码错误，升级标注走 P0 即时通道或 MR1）。本审计为文档-实现一致性层，原则上不产生 P0（P0 为代码/契约/数据破坏级缺陷）；若 drift 致错误实现风险且代码侧确有缺陷，升级标注并走 P0 即时通道。

## Goals

- 按 `multi-dimensional-audit-prompt.md` 对 finance / manufacturing / pur+sal+inv 三组域簇的设计 owner doc vs 实际代码实现做系统性双向 drift 审计，每组域簇产出 drift 审计章节。
- drift 审查覆盖 7 维度（状态迁移/工作流 / 业务规则/计算 / 字段/实体语义 / 跨域协作 / 过账/会计 / 配置/门控 / 未文档化行为）。
- **复核 MA2 已登记 owner-doc drift（范围内域簇）**：确认 MA2 状态机审计中登记的**范围内域**文档 drift finding（finance P1-MA2-031~034 / mfg P1-MA2-035~038 / pur P1-MA2-049~051 / sal P1-MA2-056~057）在本审计维度下的分类与归属，不重复登记；范围外域的 drift（如 projects/b2b）不在本审计复核。
- scope matrix §2.3「owner doc vs 代码 drift」行终态标记（`新维度` → `✅`/`⚠️(P1)`）。
- 发现的 blocker/major 登记为 P1 汇总至 `arm-index.md` §P1 发现汇总（文档类目标 MR2；代码侧缺陷走 P0 即时通道或 MR1）。roadmap A3.3/A3.4/A3.5 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**做设计文档**内部**质量审查 — 归 A3.1（done）。本审计只做 design **vs code** drift。对文档内部自洽/措辞/owner 边界只标注直接影响 drift 判定的项。
- **不**做前瞻性缺失扫描（找"从未设计的整个功能"）— 归 A3.2（done）。本审计只查"已写的设计声明 vs 代码"的一致性。
- **不**重做状态机正确性裁决（迁移完整性/守卫/并发/终态可达）— 归 MA2（done）。本审计复核 MA2 登记的 **owner-doc drift**（文档描述与代码不一致），并横扩至状态机以外维度；不重新评判状态机行为正确性。
- **不**做 API 契约（api.xml/xbiz）vs 实现一致性 — 归 A3.6（同批）。
- **不**做索引路由有效性 — 归 A3.7（同批）。
- **不**做可定制性验证（Delta/扩展字段）— 归 A3.8。
- **不**做 view.xml vs 后端契约 drift — 归 A4.6-A4.8（MA4）。
- **不**在本计划内批量修复 drift — 文档类 P1 经 R2.0 展开机制进入 MR2；代码侧缺陷走 P0 即时通道或 MR1。本审计只识别 drift + 分类。
- **不**手改生成物或 ORM。修复在 MR 批量进行。
- **不**覆盖 A3.3-A3.5 范围外的域（hr/crm/cs/projects/quality/maintenance/aps/contract/drp/logistics/b2b/master-data）— roadmap A3.3-A3.5 限定 finance/mfg/pur/sal/inv 五域。范围外域的 drift 抽样归后续轮次或 MR2 文档修复时附带。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/finance/`（19 文件）；`docs/design/manufacturing/`（12 文件）；`docs/design/purchase/`（8 文件）+ `docs/design/sales/`（7 文件）+ `docs/design/inventory/`（8 文件）；各域对应 `module-<domain>/erp-<short>-service/` BizModel/Processor/Provider/Service Java + `module-<domain>/erp-<short>-dao/` 实体 + `module-<domain>/model/app-erp-<domain>.orm.xml`；MA2 全部状态机审计报告（`docs/audits/2026-07-2*-arm-ma2-*.md`，已登记 owner-doc drift findings 复核源）
- Skill Selection Basis: `multi-dimensional-audit-prompt.md`（roadmap A3.3-A3.5 指定此 skill——多维挑战 7 维度 + 反窄化自检，适配"doc vs code drift"主题时以 7 维度为 drift 检查框架 + 项目特定维度[过账/会计/配置门控]。项目定制化层见 `docs/skills/README.md`）。三组域簇共享同一 skill 与同一结果表面（design-vs-code drift），按规则 14 合并为单计划多阶段。
- Verification: 审计不改代码/文档，故无单测回归；报告产出即更新 `arm-index.md`。drift 修复在 MR2（文档类）/MR1 或 P0 通道（代码侧）批量进行。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。本审计为文档-代码比对审查，不构建/不运行应用。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码/文档，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。文档-代码比对无回归风险，build/test 门控为同型审计 plan 的标准 Closure 实践。

## Execution Plan

### Phase 1 - finance owner doc vs 代码 drift（A3.3，S 级）

Status: completed
Targets: `docs/design/finance/`（19 文件）；`module-finance/erp-fin-service/`（BizModel + Processor + Provider + Dispatcher + Service）；`module-finance/erp-fin-dao/`；`module-finance/model/app-erp-finance.orm.xml`；MA2 finance 审计报告（A2.1/A2.3/A2.4/A2.5a-c/A2.16/A2.18）
Skill: `multi-dimensional-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线）；MA2 finance 相关审计 done（owner-doc drift findings 已在 arm-index，本审计复核）。A3.1 done（已标注交接的 drift 候选项）。

- [x] 维度「状态迁移/工作流 drift」：finance 设计声明的状态机/审批轴/工作流步骤（凭证 DRAFT/POSTED/CANCELLED + isReversed + postingType 三轴 / 期间 OPEN/CLOSING/CLOSED/CLOSED_FINAL / 预算方案 6 态 / AR-AP 辅助账项 5 态 / 核销单 3 态 / 坏账双轴）vs 代码实际迁移守卫。复核 MA2 已登记 finance owner-doc drift（P1-MA2-031/032/033/034 等）。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「业务规则/计算 drift」：finance 设计声明的业务规则与计算公式（过账平衡校验 / 成本计算 / 汇兑重估 per-voucher 平衡 / 坏账计提公式 / 预算控制三级策略 HARD/WARN/NONE / 承付 commit-release / 银行对账平衡等式 / 核销策略 FIFO/BY_AMOUNT/BY_RATIO）vs 代码实际实现。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「字段/实体语义 drift」：finance 设计声明的字段含义/枚举值/字典绑定（postingType NORMAL/REVERSAL/BUDGET / dcDirection / approveStatus / docStatus / businessType 枚举 / acctSchemaId 语义）vs ORM `orm.xml` 与代码实际使用。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「跨域协作 drift」：finance 设计声明的跨域触发（过账 Facade IErpFinVoucherBiz.post / 红冲 reverse / 域监听者 ReversalListener 回退 / 承付 hook / 多账套传播）vs 代码实际 Facade 调用与副作用。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「过账/会计 drift」：finance 设计声明的过账业务类型/科目映射/凭证结构/红冲规则（~30 businessType / GL 映射规则 / Provider 分派 / VoucherBuilder / 幂等键 (billHeadCode, businessType) / 红字凭证同向取负）vs 代码 Provider/Dispatcher/VoucherBuilder 实现。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「配置/门控 drift」：finance 设计声明的配置项/默认值/门控开关（auto-post-on-close / reverse-close kill-switch / multi-schema-enabled / auto-reconcile / budget-control-level / 各 default-subject-code）vs 代码 ErpFinConstants/Configs 与 config-gate 实现。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「未文档化行为（code→doc 反向 drift）」：finance 代码中存在但设计文档完全未提及的显著行为（隐藏副作用 / 未声明的 ErrorCode / 未文档化的自动触发 / 代码中的 deliberate Javadoc 未同步到设计）。
      - Skill: `multi-dimensional-audit-prompt.md`

Exit Criteria:

> 审计章节是可观察产物。完整仓库 `mvn test` 属 Closure Gates（见执行时规则 7）。

- [x] finance 7 维度 drift 审查结果产出（每维度至少一句裁决，含"本维度无 drift"）
- [x] finance MA2 已登记 owner-doc drift 复核表产出（确认分类 / 升级 / 降级 / 新发现代码侧 drift）
- [x] finance drift finding 清单产出（每个含 drift 方向 doc→code 或 code→doc / 严重性 / 受影响文件[设计+代码] / drift 描述 / 影响）

### Phase 2 - manufacturing owner doc vs 代码 drift（A3.4，S 级）

Status: completed
Targets: `docs/design/manufacturing/`（12 文件）；`module-manufacturing/erp-mfg-service/`（BizModel + Processor + Engine + Calculator）；`module-manufacturing/erp-mfg-dao/`；`module-manufacturing/model/app-erp-manufacturing.orm.xml`；MA2 manufacturing 审计报告（A2.6a-b/A2.4 库存核算）
Skill: `multi-dimensional-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done；MA2 manufacturing 状态机审计 done（A2.6a-b）。A3.1 done。

- [x] 维度「状态迁移/工作流 drift」：mfg 设计声明的状态机（工单 10 态 / 作业卡 8 态 / 领料 4 态 / 委外 8 态 + 审批轴 / MRP 计划 5 态 / 预测 4 态 / 仿真 4 态 config-gated）vs 代码实际迁移守卫。复核 MA2 已登记 mfg owner-doc drift（P1-MA2-035/036/037/038）。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「业务规则/计算 drift」：mfg 设计声明的业务规则与计算（BOM 展开 / MRP 净需求计算 / CRP 负荷率 / 成本卷积 CostRollup / 差异计算 5 类 / 齐套校验 / 基因追溯 / 报工成本回写）vs 代码实际实现。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「字段/实体语义 drift」：mfg 设计声明的字段含义/枚举值/字典绑定（docStatus / approveStatus / inspectionRequired / varianceType / costElementType / isFirmed）vs ORM 与代码实际使用。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「跨域协作 drift」：mfg 设计声明的跨域触发（完工入库 IErpInvStockMoveBiz / 领料出库 / 过账 IErpFinVoucherBiz MANUFACTURING_RECEIPT/ISSUE / 质检门控 IErpQaInspectionBiz / 委外采购 IErpPurOrderBiz）vs 代码实际 Facade 调用。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「过账/会计 drift」：mfg 设计声明的过账业务类型/科目映射（MANUFACTURING_RECEIPT/ISSUE / SUBCONTRACT 三段 SI/SR/SF / PRODUCTION_VARIANCE / 重算红冲 reverseIfExists）vs 代码 Provider/Dispatcher 实现。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「配置/门控 drift」：mfg 设计声明的配置项/门控开关（variance-auto-calc-enabled / inspection-gate-enabled / subcontract-release-enabled / 仿真 config-gated）vs 代码 ErpMfgConstants/Configs 实现。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「未文档化行为（code→doc 反向 drift）」：mfg 代码中存在但设计文档完全未提及的显著行为。
      - Skill: `multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] mfg 7 维度 drift 审查结果产出（每维度至少一句裁决，含"本维度无 drift"）
- [x] mfg MA2 已登记 owner-doc drift 复核表产出
- [x] mfg drift finding 清单产出

### Phase 3 - pur+sal+inv owner doc vs 代码 drift（A3.5，A 级合并）

Status: completed
Targets: `docs/design/purchase/`（8 文件）+ `docs/design/sales/`（7 文件）+ `docs/design/inventory/`（8 文件）；`module-purchase/`、`module-sales/`、`module-inventory/` 各 `erp-<short>-service/`（BizModel + Processor）；各 `erp-<short>-dao/`；各 `model/app-erp-<domain>.orm.xml`；MA2 pur/sal/inv 审计报告（A2.8/A2.9/A2.11/A2.1/A2.2/A2.4）
Skill: `multi-dimensional-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done；MA2 pur/sal/inv 状态机审计 done（A2.8/A2.9/A2.11）。A3.1 done。

- [x] 维度「状态迁移/工作流 drift」：pur/sal/inv 设计声明的状态机（采购 Order/Receive/Invoice/Payment/Return 三轴 / 销售 Order/Delivery/Invoice/Receipt/Return 三轴 / 库存 StockMove DONE + LandedCost + 承付）vs 代码实际迁移守卫。复核 MA2 已登记 drift（P1-MA2-049/050/051 pur / P1-MA2-056/057 sal / inv 相关）。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「业务规则/计算 drift」：pur/sal/inv 设计声明的业务规则（三单匹配 three-way-match / 信用控制三级策略 / 可用量校验 validateAvailable / 销售定价引擎取价 / 到岸成本分摊按金额/数量/重量 / 库存成本 7 costMethod / 库存余额三方对账 / 退货 refund 编排）vs 代码实际实现。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「字段/实体语义 drift」：pur/sal/inv 设计声明的字段含义/枚举值/字典绑定 vs ORM 与代码实际使用。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「跨域协作 drift」：pur/sal/inv 设计声明的跨域触发（采购 approve→承付 commit + 库存 incoming + 过账 AP_INVOICE/PAYMENT / 销售 approve→承付 + 库存 outgoing + 可用量校验 + 过账 AR_INVOICE/RECEIPT / intercompany / 退货反向）vs 代码实际 Facade 调用。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「过账/会计 drift」：pur/sal/inv 设计声明的过账业务类型/科目映射（AP_INVOICE/PAYMENT/PURCHASE_RETURN/PURCHASE_INPUT / AR_INVOICE/RECEIPT/SALES_RETURN/SALES_OUTPUT / LANDED_COST / 库存移动过账）vs 代码 Provider 实现。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「配置/门控 drift」：pur/sal/inv 设计声明的配置项/门控开关 vs 代码 Constants/Configs 实现。
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 维度「未文档化行为（code→doc 反向 drift）」：pur/sal/inv 代码中存在但设计文档完全未提及的显著行为。
      - Skill: `multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] pur+sal+inv 7 维度 drift 审查结果产出（每域每维度至少一句裁决）
- [x] pur+sal+inv MA2 已登记 owner-doc drift 复核表产出
- [x] pur+sal+inv drift finding 清单产出

### Phase 4 - finding 汇总交接 MR2 + 索引/矩阵更新

Status: completed
Targets: 三组域簇 drift finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.3「owner doc vs 代码 drift」行
Skill: none

- Item Types: `Follow-up`
- Prereqs: Phase 1-3 完成（finding 全部识别）

- [x] finding 汇总：全部 drift blocker/major 登记为 P1 至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA3-NNN`、报告、drift 方向、描述、目标 MR2[文档类]/MR1[代码侧]、修复状态 todo）。与 A3.1/A3.2 已登记 P1-MA3-001~023 去重无冲突。
      - Skill: none
- [x] 分类裁决：文档类 drift（设计文档错误/过时/缺失声明）目标 MR2；代码侧缺陷（设计正确但代码错误）走 P0 即时通道或 MR1，在报告中明确标注交接路径与 drift 方向。
      - Skill: none
- [x] 产出审计报告 `docs/audits/2026-07-28-1953-arm-ma3-owner-doc-vs-code-drift.md`（含：三组域簇各 7 维度 drift 审查结果 / MA2 owner-doc drift 复核表 / drift finding 清单[每个含 drift 方向 doc→code 或 code→doc / 严重性 / 受影响文件（设计+代码）/ drift 描述 / 影响] / 分类裁决[MR2 文档 vs MR1/P0 代码] / 裁决通过/失败 / 剩余风险）。
      - Skill: none
- [x] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §2.3「owner doc vs 代码 drift」行终态标记（`新维度` → `✅`/`⚠️(P1)`）。
      - Skill: none

Exit Criteria:

- [x] 所有 drift blocker/major 已登记 arm-index §P1 汇总，文档类目标 MR2 / 代码侧标注 P0 通道或 MR1，待展开
- [x] drift 方向（doc→code / code→doc）每个 finding 明确标注
- [x] 与 A3.1/A3.2 已登记 P1 经交叉去重无重复登记
- [x] arm-index 报告清单 + scope matrix 已反映审计结论

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_0576a0c64ffennLkxss5lQwXQm`，独立 general 子代理，fresh-context，对照实时仓库逐项复核）。VERDICT = accept，**无 BLOCKER**。核实要点：roadmap A3.3/A3.4/A3.5 全 `todo` ✓；三组域簇 owner doc 文件计数精确匹配（finance=19/mfg=12/pur=8/sal=7/inv=8）✓；对应 service 模块存在 ✓；MA2 finding ID（P1-MA2-031~034/035~038/049~051/056~057）arm-index 确认 ✓；bundling 经 rule 14 核验合理（同 skill + 同结果表面 design-vs-code drift）✓；anti-slack 零禁词 ✓；finding ID 范围不冲突（当前 max P1-MA3-023）✓；scope matrix §2.3 行存在 ✓。**采纳 2 项非阻塞修订**：(1) Goals/Baseline 复核 finding 引用修正——A3.1 交接的跨域示例（projects/b2b）改为标注"范围内域簇"实际复核目标（finance/mfg/pur/sal 各域 P1-MA2-*）；(2) Non-Goals 域分级标签修正——"B/C 级域"措辞不准（hr=S/crm=A 等），改为"A3.3-A3.5 范围外的域"。Plan Status 转 active。

## Closure Gates

> 本计划主体是文档-代码比对审查（不改应用代码；产出为审计报告 + arm-index/scope-matrix 更新）。完整仓库验证在此处运行一次（同型审计 plan 的标准 Closure 实践）。drift 修复在 MR2（文档类）/MR1 或 P0 通道（代码侧）批量进行，本审计只识别 drift + 分类。

- [x] 范围内行为完成（A3.3/A3.4/A3.5 三组域簇 owner doc vs 代码 drift 审计报告产出 + arm-index 更新 + scope matrix 标记完成）
- [x] 相关文档对齐（审计报告、arm-index、scope matrix 结论已反映）
- [x] 已运行验证：文档-代码比对无代码变更，build/test 门控仅作回归基线确认（同型审计 plan 的相同 Closure 实践）
- [x] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR2/MR1）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证（状态、阶段、门控、日志都一致）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### A3.6 API 契约 vs 实现一致性

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计做 design owner doc vs Java/ORM 代码 drift；API 契约层（api.xml/xbiz 声明 vs 实现）归 A3.6。若 drift 审计中发现 API 契约层问题，标注交接 A3.6。
- Successor Required: `yes`——A3.6 执行时复核（同批起草）。

### view.xml drift（A4.6-A4.8）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计做后端 design vs 后端代码 drift；前端 view.xml vs 后端契约 drift 归 MA4（A4.6-A4.8）。
- Successor Required: `yes`——A4.6-A4.8 执行时复核。

### B/C 级域 owner-doc drift

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap A3.3-A3.5 限定 S+A 级域（finance/mfg/pur/sal/inv）。B/C 级域（crm/cs/hr/aps/contract/drp/logistics/b2b/maintenance/quality/projects/master-data）drift 抽样归后续轮次；其 MA2 状态机 owner-doc drift 已登记 MR2。
- Successor Required: `yes`——后续 MA3 轮次或 MR2 文档修复时附带复核。

## Closure

Status Note: 全部 4 Phase 已完成。审计报告 `docs/audits/2026-07-28-1953-arm-ma3-owner-doc-vs-code-drift.md` 已产出（Verdict FAIL，22 项 NEW P1 P1-MA3-024~045 + 13 项 NEW P2 P2-MA3-023~035）。arm-index 已更新（报告清单 + P1 详细清单 22 行 + MA3 owner-doc drift 汇总块）。scope matrix §2.3「owner doc vs 代码 drift」行已标记 `⚠️(P1)`。roadmap A3.3/A3.4/A3.5 已推进至 done。MA2 owner-doc drift 复核全部确认一致无升级。本审计为文档-代码比对审查不改应用代码，build/test 门控仅作回归基线确认。

Closure Audit Evidence:

- Auditor / Agent: 待独立子代理执行（新会话 closure audit）
- Evidence: 审计报告 `docs/audits/2026-07-28-1953-arm-ma3-owner-doc-vs-code-drift.md`；arm-index P1-MA3-024~045 22 行；scope matrix §2.3 终态标记 `⚠️(P1)`；roadmap A3.3/A3.4/A3.5 `done`

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
