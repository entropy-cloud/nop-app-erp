# 审计-修复范围与维度矩阵（arm-scope）

> 生成日期：2026-07-27
> 来源：`docs/skills/audit-remediation-roadmap-authoring-prompt.md` 步骤 1 + 步骤 2 + 步骤 4.1
> 用途：M0 里程碑核心交付物，驱动 `docs/backlog/audit-remediation-roadmap.md` 的工作项拆分

## 1. 域复杂度评估矩阵

> 数据采集时间：2026-07-27。复杂度数据会随代码变化而漂移，MV 验证里程碑需重跑确认。

### 1.1 原始指标

| 域 | 实体 | Mutation | Query | Java文件 | Proc/Engine/Resolver | 状态机实体 | 测试 | view.xml | 跨域daoFor |
|----|------|----------|-------|---------|----------------------|-----------|------|----------|-----------|
| finance | 48 | 137 | 36 | 331 | 36 | 24 | 64 | 36 | 49 |
| hr | 42 | 92 | 35 | 127 | 1 | 5 | 15 | 36 | 27 |
| manufacturing | 41 | 74 | 35 | 246 | 21 | 11 | 30 | 31 | 47 |
| crm | 39 | 52 | 25 | 131 | 9 | 3 | 16 | 34 | 32 |
| assets | 24 | 61 | 13 | 176 | 48 | 18 | 14 | 18 | 23 |
| quality | 21 | 53 | 17 | 132 | 8 | 16 | 24 | 16 | 28 |
| projects | 21 | 48 | 18 | 120 | 8 | 16 | 12 | 16 | 23 |
| contract | 19 | 37 | 4 | 60 | 1 | 2 | 5 | 15 | 17 |
| purchase | 32 | 34 | 5 | 187 | 45 | 29 | 29 | 20 | 34 |
| cs | 18 | 35 | 27 | 67 | 0 | 6 | 13 | 16 | 15 |
| sales | 27 | 30 | 4 | 162 | 47 | 25 | 31 | 16 | 28 |
| maintenance | 20 | 30 | 9 | 90 | 2 | 6 | 12 | 12 | 18 |
| b2b | 16 | 31 | 4 | 59 | 1 | 1 | 6 | 13 | 5 |
| inventory | 31 | 36 | 20 | 179 | 18 | 19 | 24 | 21 | 26 |
| master-data | 25 | 16 | 44 | 181 | 4 | 0 | 21 | 25 | 15 |
| drp | 16 | 24 | 4 | 45 | 5 | 0 | 7 | 10 | 19 |
| aps | 7 | 19 | 7 | 35 | 2 | 2 | 5 | 6 | 8 |
| logistics | 12 | 11 | 0 | 44 | 1 | 2 | 7 | 7 | 11 |
| notify | 3 | 6 | 6 | 20 | 2 | 0 | 5 | 3 | 3 |

### 1.2 复杂度分级

> **维度类型区分（v2 修订）**：机械维度（ORM 字段/类型检查、平台合规 grep）不需要理解业务语义，S 级域可整域审计；行为维度（状态机、代码质量、测试覆盖）需理解业务语义，S 级域必须按功能模块拆分。

| 等级 | 判定 | 审计粒度策略 | 落点域 |
|------|------|-------------|--------|
| **S（超高）** | mutation ≥ 70 或 Java ≥ 250 或 Proc ≥ 30 | **行为维度**：按功能模块拆分（每域 2-4 片）；**机械维度**：整域可接受 | **finance**、**manufacturing**、**hr** |
| **A（高）** | mutation 30-69 或 状态字段 ≥ 15 或 跨域引用 ≥ 25 | **行为维度**：单域单工作项；**机械维度**：可 2 域合并 | **assets**(48proc)、**purchase**(45proc)、**sales**(47proc)、**quality**(53mut)、**crm**(52mut)、**projects**(48mut)、**cs**、**contract**、**b2b**、**inventory**(36mut/18proc) |
| **B（中）** | mutation 15-30 且 状态字段 < 15 | 2-3 域合并 | **master-data**(16mut/0状态机)、**maintenance**(30mut/6状态机)、**drp**(24mut/0状态机) |
| **C（低）** | mutation < 15 且 Java < 50 | 3-5 域合并 | **logistics**(11mut)、**notify**(6mut) |

> **aps 特殊处理**：aps 实测 mutation=19 略高于 C 级 mut<15 阈值，但其规模极小（7 实体 / 35 Java 文件 / 2 状态机实体，全域最小业务域之一）。裁决：aps 并入 C 级合并审计——拆分独立审计的边际收益低于合并成本。本裁决比 assets 裁决更轻微（aps 不触发任何 P1 残留风险），仅记录分类边界理由。

> **assets 特殊处理（v2 修订）**：Proc=48 ≥ 30 满足 S 级判定。但 assets 的 Processor 集中在折旧引擎/处置/价值调整/资本化一个子系统中（非分散在多个独立子系统），行为维度拆分的边际收益低于 finance/mfg/hr。裁决：assets 保持 A 级，但在 MA4 中新增 **A4.3 assets 折旧引擎与 Processor 链路专属审计**（独立工作项），确保 48 Processor 得到聚焦审查。折旧正确性直接影响财务报表，是高风区域。

### 1.3 S 级域功能模块拆分

#### finance（S 级，7 个功能模块审计切片）

| 功能模块 | owner doc 锚点 | 关注点 |
|----------|---------------|--------|
| 过账引擎与凭证链路 | `docs/design/finance/posting.md` | 3 层过账、Provider 路由、冲销反写、posted 标志 |
| 预算与承付 | `docs/design/finance/budget.md` | 预算控制、commitment 释放、滚动复制、结转 |
| AR/AP 核销 | `docs/design/finance/` | 应收应付、核销规则、账龄 |
| 坏账与汇兑 | `docs/design/finance/bad-debt.md` | Allowance 法、汇兑损益 |
| 成本核算 | `docs/design/finance/costing-methods.md` | 7 种 costMethod、子计算器注入 |
| 期间与结账 | `docs/design/finance/period-close.md` | 期间状态机、结账流程、结转 |
| GL 映射与科目 | `docs/design/finance/gl-mapping-rules.md` | GL Mapping Rule、科目表、多账套 |

#### manufacturing（S 级，4 个功能模块审计切片）

| 功能模块 | owner doc 锚点 | 关注点 |
|----------|---------------|--------|
| MRP/DRP 引擎 | `docs/design/manufacturing/mrp.md` | MRP 计算、释放、仿真引擎 |
| 工单与报工 | `docs/design/manufacturing/` | 工单状态机、领料/报工/完工 |
| BOM 与工艺路线 | `docs/design/manufacturing/` | BOM 展开、工艺路线、工作中心 |
| 质量集成与 NCR | `docs/design/manufacturing/` + `docs/design/quality/` | 制造-质量跨域、NCR 触发 |

#### hr（S 级，3 个功能模块审计切片）

| 功能模块 | owner doc 锚点 | 关注点 |
|----------|---------------|--------|
| 排班与考勤 | `docs/design/human-resource/` | 排班引擎、考勤记录 |
| 工资核算 | `docs/design/human-resource/` | 工资计算、扣税 |
| 员工与组织 | `docs/design/human-resource/` | 员工生命周期、组织架构 |

## 2. 审计维度矩阵（维度 × 域）

> 标注：`✅` 已审计且无未闭包 finding | `⚠️` 已审计但有残留/deferred | `❓` 未审计（工作项来源） | `N/A` 不适用

### 2.1 MA1 — 结构与架构层

> MA1 ORM 审计（A1.1–A1.9）已于 2026-07-27 完成（详见 `docs/audits/2026-07-27-1015-arm-ma1-{s,a,bc}-tier-orm.md`）；MA1 跨模块依赖/DAG 审计（A1.10）已于 2026-07-27 完成（详见 `docs/audits/2026-07-27-1227-arm-ma1-cross-module-dag.md`）；MA1 平台合规审计 S 级三域（A1.11 finance+mfg+hr）已于 2026-07-27 完成（详见 `docs/audits/2026-07-27-1227-arm-ma1-platform-conformance-s-tier.md`）；MA1 平台合规审计 A 级核心四域（A1.12 pur+sal+assets+inv）已于 2026-07-27 完成（详见 `docs/audits/2026-07-27-1227-arm-ma1-platform-conformance-a-tier-core.md`）；MA1 平台合规审计 BC 合并 12 域（A1.13 crm+qa+prj+cs+ct+b2b+mnt+drp+md+aps+log+notify）已于 2026-07-27 完成（详见 `docs/audits/2026-07-27-1430-arm-ma1-platform-conformance-bc-tier.md`）；MA1 架构治理复审（A1.14 daoFor Type 4 残留 / 字典真相 / 共享内核守卫 / CI guard）已于 2026-07-27 完成（详见 `docs/audits/2026-07-27-1430-arm-ma1-architecture-governance-review.md`）。下表反映终态：跨模块依赖/DAG 行全域 `✅`/`⚠️(P1)`（DAG 零循环零禁止方向、外部声明 100% 覆盖；3 项 P1 待 MR1 文档+代码修复）。ORM 模型规范行：10 域 ✅（9 维度全通过或仅 P2 watch-only）、9 域 ⚠️（有 P1 待 MR1）、0 域 ❓、0 blocker。Nop 平台合规行：mfg ✅（15 维度全通过）/ finance ⚠️(P1)（1 项 P1-MA1-018 enum↔dict 漂移 + 1 项 P2）/ hr ⚠️(P2)（1 项 P2 orphan dict，无 P1）/ pur ✅（仅 D1 P2 watch-only，无 P1）/ sal ✅（15 维度全通过）/ assets ⚠️(P2)（2 项 P2 owner-doc drift + 合并 P1-MA1-022，无 P0）/ inv ⚠️(P1)（**P0-MA1-021 已闭包**——plan `2026-07-27-1430-1`；合并 P1-MA1-022 跨域只读 daoFor 仍待 MR1）；**A1.13 12 域**：crm ✅（DECIMAL↔double P1-MA1-009 已知 MR1 ORM 层，平台合规 15 维度全通过）/ qa ✅（propId P1-MA1-012 已知 MR1 ORM 层，平台合规 15 维度全通过）/ prj ✅（propId P1-MA1-010 已知 MR1 ORM 层，平台合规 15 维度全通过）/ cs ✅（15 维度全通过）/ ct ⚠️(P2)（P2-MA1-027 owner-doc drift + 合并 P1-MA1-022）/ b2b ✅（15 维度全通过；ASN 跨域写豁免已登记）/ mnt ⚠️(P2)（P2-MA1-028 owner-doc drift + propId P1-MA1-011 已知 MR1 + 合并 P1-MA1-022）/ drp ⚠️(P1)（命名 P1-MA1-014 已知 MR1 + 合并 P1-MA1-022）/ md ✅（15 维度全通过；DAG 根域；F4 已裁决）/ aps ⚠️(P1)（合并 P1-MA1-022 跨域只读 daoFor 待 MR1）/ log ✅（15 维度全通过）/ notify ✅（15 维度全通过；跨域通知派发子系统，F5 owner doc 已补）。**MA1 平台合规维度全域 19 列全部 ✅/⚠️(P1)/⚠️(P2)，无 ❓**。**A1.14 架构治理维度全域结论**（2026-07-27 复审）：finance ⚠️(residual)（daoFor Type 4 successor + R12a 共享内核 69 import 已登记）/ mfg ⚠️(residual)（F6 已闭包 + MrpReleaseService 跨域写豁免已登记）/ hr ✅（无 P0/P1）/ assets ⚠️(P1)（合并 P1-MA1-022）/ pur ⚠️(P1)（合并 P1-MA1-022）/ sal ⚠️(P1)（合并 P1-MA1-022）/ qa ⚠️(P1)（合并 P1-MA1-022）/ crm ✅（dashboard 聚合永久接受）/ prj ⚠️(P1)（合并 P1-MA1-022）/ cs ✅（最小跨域面）/ ct ⚠️(P1)（**P1-MA1-029** ErpCtInvoicePlanBizModel 跨域写半治理 + ErpCtRebateSettlementBizModel 已登记豁免）/ b2b ⚠️(residual)（F1 闭包项 #2 已完成 ErpB2bAsnBizModel 豁免登记；successor 待 pur createFromAsn）/ inv ⚠️(P1)（P0-MA1-021 已闭包 + 合并 P1-MA1-022）/ md ⚠️(residual)（F4 闭包完成 + DAG 根域 + 共享内核 AcctSchemaResolver 所在域）/ mnt ⚠️(P1)（合并 P1-MA1-022）/ drp ⚠️(residual)（F7 已闭包 4 ErpInvDrp* 命名例外登记；successor 待 drp 域重大 ORM 变更）/ aps ⚠️(P1)（合并 P1-MA1-022）/ log ✅（极简域）/ notify ✅（F5 已闭包）。**MA1 架构治理维度全域 19 列全部 ✅/⚠️(residual/P1)，0 ❓，0 P0**。`S拆` 后缀用于行为维度（MA2 状态机），ORM 机械维度已不再拆分。

| 维度 | finance | mfg | hr | assets | pur | sal | qa | crm | prj | cs | ct | b2b | inv | md | mnt | drp | aps | log | notify | Skill |
|------|---------|-----|----|----|------|-----|----|----|----|----|----|----|-----|-----|-----|-----|-----|-----|--------|-------|
| ORM 模型规范 | ✅ | ⚠️P1 | ✅ | ⚠️P1 | ✅ | ✅ | ⚠️P1 | ⚠️P1 | ⚠️P1 | ✅ | ✅ | ✅ | ✅ | ⚠️ | ⚠️P1 | ⚠️P1 | ✅ | ✅ | ✅ | orm-model-audit |
| 跨模块依赖/DAG | ⚠️(P1) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | cross-module-dep-audit |
| Nop 平台合规 | ⚠️(P1) | ✅ | ⚠️(P2) | ⚠️(P2) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ⚠️(P2) | ✅ | ⚠️(P1) | ✅ | ⚠️(P2) | ⚠️(P1) | ⚠️(P1) | ✅ | ✅ | nop-conformance-audit |
| 架构治理（daoFor/字典/共享内核/guard） | ⚠️(residual) | ⚠️(residual) | ✅ | ⚠️(P1) | ⚠️(P1) | ⚠️(P1) | ⚠️(P1) | ✅ | ⚠️(P1) | ✅ | ⚠️(P1) | ⚠️(residual) | ⚠️(P1) | ⚠️(residual) | ⚠️(P1) | ⚠️(residual) | ⚠️(P1) | ✅ | ✅ | 参考arch-gov-review |

### 2.2 MA2 — 业务正确性层

> MA2 业务正确性审计 A2.1 P2P 端到端已于 2026-07-27 完成（详见 `docs/audits/2026-07-27-1949-arm-ma2-procure-to-pay-e2e.md`）。下表「业财端到端」行 finance/purchase 列由 `❓` 推进至 `⚠️(P1)`（P2P 链路组件齐备、E2E 覆盖黄金路径+反向冲销，零 P0；3 项 P1 待 MR1：P1-MA2-001 暂估冲回缺失 / P1-MA2-002 多币种 P2P 本位币凭证路径未验证 / P1-MA2-003 付款核销缺三单匹配完成态复核；MA1 finding 运行时复核无升级）。**A2.2 O2C 端到端已于 2026-07-27 完成**（详见 `docs/audits/2026-07-27-1949-arm-ma2-order-to-cash-e2e.md`）。下表「业财端到端」行 sales 列由 `❓` 推进至 `⚠️(P1)`（O2C 链路组件齐备、E2E 覆盖黄金路径+反向冲销+财务正式核销+退货负 credit memo，零 P0；1 项 P1 待 MR1：P1-MA2-009 多币种 O2C + 收款核销汇兑损益未实现；6 项 P2 watch-only；MA1 finding 运行时复核无升级；并发敏感点交接 A2.17）。**A2.3 期末结账端到端已于 2026-07-27 完成**（详见 `docs/audits/2026-07-27-1949-arm-ma2-period-close-e2e.md`）。下表「业财端到端」行 finance/期间结账 列推进至 `⚠️(P0→fix-plan + P1)`（期间结账链路组件齐备、模块关账顺序/状态机/损益结转（单币种）/汇兑重估（per-voucher 平衡）经审计确认，但发现 **1 项 P0** P0-MA2-016 汇兑损益费用类余额未结转至本年利润 [已注入即时通道 fix plan `2026-07-27-1949-arm-fix-p0-ma2-016`] + 6 项 P1 待 MR1：P1-MA2-017 auto-post-on-close 阻断分级 / P1-MA2-018 年初余额非累计 / P1-MA2-019 辅助账对账作用域 / P1-MA2-020 反结账 approval kill-switch / P1-MA2-021 CLOSED_FINAL 凭证锁定 / P1-MA2-022 FX 无前期 reversal；3 项 P2 watch-only；MA1 finding [P1-MA1-016/017/018] 运行时复核无升级；C-11 已自然消解 [flow-overview:499 现为「单库事务 REQUIRED」]；并发敏感点交接 A2.17）。finance 列回退至 `⚠️(P1)`（P0-MA2-016 已闭包：plan `2026-07-27-1949-arm-fix-p0-ma2-016` done，汇兑损益费用类余额已正常结转至本年利润；6 项 P1 仍待 MR1）。**A2.4 库存核算一致性审计已于 2026-07-27 完成**（详见 `docs/audits/2026-07-27-2211-arm-ma2-inventory-costing-consistency.md`）。下表「库存核算一致性」行 inventory 列由 `❓` 推进至 `⚠️(P1)`（库存核算链路组件齐备：7 costMethod 策略 + 子计算器注入模式 + 成本调整 + 到岸成本 + PPV + reclose 兜底；三方对账「成本层 ErpInvCostLayer + 余额 ErpInvStockBalance + 流水 ErpInvStockLedger」在正常路径成立且经证据确认，零 P0；2 项 P1 待 MR1：P1-MA2-023 SPECIFIC 历史成本守卫缺失 / P1-MA2-024 STANDARD 红冲成本不变量跨重估破缺；5 项 P2 watch-only（P2-MA2-026~030）；MA1/MA2 finding [P1-MA1-022/P0-MA1-021 done/P1-MA2-017/P1-MA2-002] 运行时复核无升级；并发敏感点 3 处交接 A2.17）。**A2.5a finance 状态机审查（过账与凭证）已于 2026-07-27 完成**（详见 `docs/audits/2026-07-27-2211-arm-ma2-finance-posting-voucher-state-machine.md`）。下表「状态机正确性」行 finance 列由 `❓S拆` 推进至 `⚠️(P1)`（会计凭证状态机 DRAFT/POSTED/CANCELLED + isReversed + postingType 三轴组合核心契约经证据确认：幂等键 `(billHeadCode, businessType)` + `findAllPostedVouchers` 过滤 `postingType=REVERSAL` 阻断红字凭证再红冲的无限循环 + 引擎 reversal 统一路径；零 P0；2 项 P1 待 MR1：P1-MA2-031 DRAFT→CANCELLED 状态不可达+红字凭证终态归属未定义 / P1-MA2-032 IGNORED 凭证悬挂缺告警闭环；1 项 P2 watch-only（P2-MA2-033）；**P1-MA2-021 CLOSED_FINAL 凭证锁定升级评估裁决：维持 P1 不升 P0**；11 项 MA1/MA2 finding 运行时复核无升级：P0-MA1-021 sustained done / P0-MA2-016 sustained fix / 9 项 P1/P2 与原登记一致；并发敏感点 3 处交接 A2.17）。**A2.5b finance 状态机审查（预算与期间，S 级拆分 2/3）已于 2026-07-27 完成**（详见 `docs/audits/2026-07-27-2315-arm-ma2-finance-period-budget-state-machine.md`）。下表「状态机正确性」行 finance 列维持 `⚠️(P1)`（A2.5a 凭证状态机 + A2.5b 期间/预算状态机覆盖完成）；「预算与承付」行 finance 列由 `❓S拆` 推进至 `⚠️(P1)`（期间状态机 OPEN/CLOSING/CLOSED/NEVER_OPENED/CLOSED_FINAL 5 态 + per-module 关账子状态机 3 态 + 预算方案状态机 DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED/CLOSED 6 态 + 承付 commit/release 独立凭证状态机核心契约经证据确认：closePeriod/finalizePeriod/reverseClose 状态迁移守卫齐全 + @BizMutation 事务回滚保证结账失败时期间状态一致性 + reverseClose 红冲容错齐全 + 承付 commit/release 3 接入点齐全 + release 守卫 + 采购 hook 容错对称性已 fix；零 P0；2 项 P1 待 MR1：P1-MA2-033 NEVER_OPENED→OPEN 迁移路径缺失 / P1-MA2-034 carryForward 不校验源年度全 CLOSED 前置；2 项 P2 watch-only（P2-MA2-034 owner doc 4 处漂移 / P2-MA2-035 REJECTED→DRAFT 直迁缺失）；**P1-MA2-020 反结账 kill-switch 升级评估裁决：维持 P1 不升 P0**（config=false 时合法路径开放 + owner doc 承诺审批流 successor + 默认 config=true 是保护性默认）；**P1-MA2-021 期间侧 CLOSED_FINAL 凭证锁定升级评估裁决：维持 P1 不升 P0**（业务路径 post/reverse 已守卫 + 直接 entity mutation 受权限保护）；7 项已登记 MA2 finding 运行时复核无升级：P1-MA2-017/018/019/022 仅治理缺陷 / P1-MA2-020/021 维持 / P2-MA2-025 交接 A2.17；5 处并发敏感点交接 A2.17）。**A2.5c finance 状态机审查（AR/AP 核销，S 级拆分 3/3）已于 2026-07-28 完成**（详见 `docs/audits/2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md`）。下表「状态机正确性」行 finance 列维持 `⚠️(P1)`（A2.5a 凭证 + A2.5b 期间/预算 + A2.5c AR/AP 核销三拆分全部 done）；AR/AP 辅助账项状态机（OPEN/PARTIAL/SETTLED/CANCELLED/WRITTEN_OFF 5 态）+ 核销单状态机（DRAFT/POSTED/REVERSED 3 态）+ 坏账核销状态机（approveStatus×docType + reverseApprove 红冲闭环）三组件核心契约经证据确认：状态迁移守卫齐全 + @BizMutation 事务回滚保证辅助账与核销单一致性 + reverseApprove 红冲闭环对称回退强一致 + CANCELLED 经 cancelOnReverse 可达（证伪"死状态"假设）+ 域侧 ReceiptSettler/PaymentSettler 与 finance ErpFinReconciliation 双路径为设计并行（非分歧）；零 P0、零新 P1；6 项新 P2 watch-only（P2-MA2-036 ar-ap-reconciliation.md owner doc 漂移 / P2-MA2-037 state-machine.md 缺独立章节 / P2-MA2-038 双路径无对账守卫 / P2-MA2-039 assertOpen 不拒绝 WRITTEN_OFF [config-gated] / P2-MA2-040 坏账 REJECTED 无 resubmit / P2-MA2-041 核销无期间 CLOSED_FINAL 守卫）；**P1-MA2-009 多币种核销辅助账本位币升级评估裁决：维持 P1 不升 P0**（单币种正确+多币种功能缺口非数据破坏）；**P2-MA2-008/014 并发核销 SETTLED 漂移升级评估裁决：维持 P2 并降级**（ErpFinArApItem `versionProp="version"` 透明乐观锁将 silent lost-update 降为 detectable conflict）；6 项已登记 MA2 finding 运行时复核无升级；并发敏感点 5 处交接 A2.17（含 versionProp 降级重要事实）。**S 级状态机审查三拆分（A2.5a/A2.5b/A2.5c）全部 done**）。**A2.6a manufacturing 生产执行状态机审查（S 级拆分 1/2）已于 2026-07-28 完成**（详见 `docs/audits/2026-07-28-0109-arm-ma2-mfg-work-order-jobcard-state-machine.md`）。下表「状态机正确性」行 mfg 列由 `❓S拆` 推进至 `⚠️P1(A2.6a✅;A2.6b❓)`（manufacturing 生产执行四组件状态机：工单 10 态 + 作业卡 8 态 + 领料 4 态 + 委外 8 态+审批轴，核心契约经实仓逐项证据确认——状态迁移守卫齐全、@BizMutation 事务回滚保证单据与库存移动单/GL 凭证一致性、领料 reverseConfirm + 委外 reverseCompletion 红冲闭环对称、过账副作用经 I*Biz 跨域[production 代码无 `daoFor(ErpFin*)`]；零 P0——三个候选 P0 经证据证伪：作业卡 TRANSFERRED 死状态不破坏主路径 / 委外 reverseCompletion 双路径保证最终一致 / 部分齐套强制开工缺料经 @BizMutation 事务回滚有出口；1 项 P1 待 MR1：P1-MA2-035 作业卡 PARTIALLY_TRANSFERRED/MATERIAL_TRANSFERRED dict 死状态 + owner doc 迁移图声明漂移[按 finance A2.5a P1-MA2-031 同型裁决]；3 项 P2 watch-only：P2-MA2-042 报工超产 config-gate 缺失 / P2-MA2-043 领料状态机 owner doc 散落 / P2-MA2-044 字典命名漂移 subcontract-status vs subcontract-order-status；MA1 finding [P1-MA1-001 propId 缺失/P1-MA1-022 跨域只读 daoFor] 运行时复核状态机角度无升级；并发敏感点 5 处交接 A2.17 含 @Version 透明乐观锁降级[ErpMfgWorkOrder/MaterialIssue/SubcontractOrder 均声明 versionProp]。**A2.6b MRP/BOM 计划规划状态机（S 级拆分 2/2）已于 2026-07-28 完成**（详见 `docs/audits/2026-07-28-0109-arm-ma2-mfg-mrp-bom-state-machine.md`）。下表「状态机正确性」行 mfg 列由 `⚠️P1(A2.6a✅;A2.6b❓)` 推进至 `⚠️P1(A2.6a✅;A2.6b✅)`（manufacturing 计划规划五组件状态机：MRP 计划头 5 态 + 预测 4 态 + 建议单隐式生命周期 isFirmed + BOM isActive + 仿真 4 态 config-gated，核心契约经实仓逐项证据确认——状态迁移守卫齐全、@BizMutation 事务边界覆盖 runMrp 全链 + 释放全链 + promote 全链、幂等守卫完整、仿真 E2 fork 单次路径零触及[SimulationMrpEngine 不调 MrpEngine.runMrp]；零 P0——三个候选 P0 经证据证伪：MRP 运算 RUNNING 中途异常经 @BizMutation 事务回滚覆盖 / 释放路径生成目标单失败经 @BizMutation 事务回滚覆盖 / SUBCONTRACT config-gated on 生成 APPROVED 委外单经 config-gate 默认 off 控制裁决 P1 治理；3 项 P1 待 MR1：P1-MA2-036 MRP CANCELLED + 预测 CONSUMED dict 死状态 + owner doc 漂移 / P1-MA2-037 mrp.md §建议单释放 RELEASED 文字 vs isFirmed 布尔漂移 / P1-MA2-038 MrpReleaseService 委外单 APPROVED O-4 豁免登记缺失[与 P1-MA1-029 同型半治理]；2 项 P2 watch-only：P2-MA2-045 state-machine.md 无 MRP/预测独立章节 / P2-MA2-046 mrp.md:93 委外释放注记过时；MA1 finding [P1-MA1-022 跨域只读/P1-MA1-029 同型] 运行时复核状态机角度无升级；并发敏感点 5 处交接 A2.17 含 ErpMfgMrpPlanLine 无 versionProp 行级并发缺口未降级[头级/场景级/版本级 versionProp 降级但行级未覆盖]。**manufacturing 状态机审查 S 级拆分 1/2 + 2/2 全部 done**）。**A2.7a hr 员工与组织状态机审查（S 级拆分 1/2）已于 2026-07-28 完成**（详见 `docs/audits/2026-07-28-0230-arm-ma2-hr-employee-organization-state-machine.md`）。下表「状态机正确性」行 hr 列由 `❓S拆` 推进至 `⚠️P1(A2.7a✅;A2.7b❓)`（hr 员工与组织七组件状态机：员工 5 态 + 合同 4 态 + 招聘 7 态 + 考核 3 态 + 发展计划 4 态 + 发展计划项 4 态 + 调查 4 态，核心契约经实仓逐项证据确认——在职状态机 + 招聘状态机 + 考核状态机 + 发展计划状态机 + 发展计划项状态机的主路径状态迁移守卫齐全、@BizMutation 事务回滚保证招聘 hire 跨实体副作用失败原子性、考核 completeAssessment 跨实体刷新 gapAnalysis 经直传 levels 避免跨事务可见性、合同到期经 cron-gated Job + 单失败隔离 + 跨域通知派发；零 P0——六个候选 P0 经证据证伪或降级：员工离职/退休/转正迁移缺失按 finance P1-MA2-031 + mfg P1-MA2-035/036 同型裁决为 P1 / 合同 SUSPENDED 死状态 P1 / 调查三态死状态 P1 / 发展计划死状态 P1 / 招聘 close 无守卫证伪 P0 是合法入职后清理登记 P2 / 调岗请假冲突 warn 非阻断设计裁定登记 P2；4 项 P1 待 MR1：P1-MA2-039 员工 employmentStatus RESIGNED/TERMINATED/RETIRED 三态死状态 + owner doc §场景D/E 联动完全未实现 / P1-MA2-040 合同 SUSPENDED dict 死状态 + owner doc 无合同独立章节 / P1-MA2-041 调查 OPEN/CLOSED/ARCHIVED 三态死状态 + ErpHrSurveyBizModel 18 行 CRUD 桩 + owner doc §状态机声明漂移 / P1-MA2-042 发展计划 DRAFT/CANCELLED + 计划项 OVERDUE dict 死状态 + 无 cancelPlan + 无 OVERDUE 自动 job——全部按 finance P1-MA2-031 + mfg P1-MA2-035/036 同型裁决；5 项 P2 watch-only：P2-MA2-047 state-machine.md 缺 5 组件独立章节 / P2-MA2-048 招聘 close 无守卫 / P2-MA2-049 recruitment.md 多实体 Deferred 未注记 / P2-MA2-050 调岗请假冲突 warn 非阻断 / P2-MA2-051 长期 PROBATION 未转正无 TODO 提醒；MA1 finding [P2-MA1-020 orphan dict salary-approval-status + P1-MA1-022 跨域只读] 运行时复核状态机角度无升级；并发敏感点 5 处交接 A2.17 含 @Version 透明乐观锁降级[7 个 hr 状态机实体全部声明 versionProp]。**A2.7b hr 考勤与工资状态机审查（S 级拆分 2/2）已于 2026-07-28 完成**（详见 `docs/audits/2026-07-28-0230-arm-ma2-hr-attendance-payroll-state-machine.md`）。下表「状态机正确性」行 hr 列由 `⚠️P1(A2.7a✅;A2.7b❓)` 推进至 `⚠️P1(A2.7a✅;A2.7b✅)`（hr 考勤与工资八组件状态机：请假 5 态 + 考勤布尔 + 工时单 4 态 + 排班分配 4 值无 dict + 换班 4 态 + 工资双轴 approveStatus 4+paymentStatus 3+posted + 仿真 5 态 + 银行文件 3 态，核心契约经实仓逐项证据确认——主路径状态迁移守卫齐全[请假 5 态全迁移 + approve/cancel 触发排班联动 / 工资支付轴 PENDING→PAID/VOID 双守卫 / 仿真 5 态全迁移 / 换班 4 态全迁移 + approve 副作用交换 shiftId]、@BizMutation 事务回滚保证请假→排班联动失败原子性、工资 markPaid 触发跨域过账经 IErpFinVoucherBiz.post() REQUIRES_NEW Facade[hr production 代码无 daoFor(ErpFin*)]、请假 cancel 红冲恢复经 onLeaveCancelled leaveRequestId 匹配回退排班 SCHEDULED、仿真 convertToFormal per-employee 冲突 skip + all-conflict throw 双层容错；零 P0——六个候选 P0 经证据证伪或降级：工时单 APPROVED/REJECTED 死状态按同型裁决 P1 不破坏主路径 / 银行文件 UPLOADED/CONFIRMED 死状态 owner doc Deferred config-gated P1 / 工资过账吞异常悬挂按同型裁决 P1 非正常路径失败 + LOG 可见性；6 项 P1 待 MR1：P1-MA2-043 工时单 APPROVED/REJECTED dict 死状态 + ErpHrTimesheetBizModel 仅 submit + owner doc §场景 F 声明漂移 / P1-MA2-044 工时单硬编码字符串 vs ErpHrConstants 不一致 / P1-MA2-045 银行付款文件 UPLOADED/CONFIRMED dict 死状态 + ErpHrPayrollBankFileBizModel 18 行 CRUD 桩 + owner doc §七 漂移 / P1-MA2-046 排班分配 status 无 dict 绑定 raw VARCHAR + owner doc §二 声明漂移 / P1-MA2-047 SalaryPostingDispatcher javadoc "无 posted 字段" drift + ErpHrSalary.posted 死字段从未写入 / P1-MA2-048 工资过账 tryPostPayment/tryPostAccrual 吞异常致 posted=false 悬挂无告警闭环——全部按 finance P1-MA2-031/032 + mfg P1-MA2-035/036 + hr A2.7a P1-MA2-039~042 同型裁决；1 项 P2 watch-only：P2-MA2-052 state-machine.md 缺考勤/工资/工时单/排班/换班/仿真/银行文件独立章节；MA1 finding [P2-MA1-020 + P1-MA1-022] 运行时复核状态机角度无升级；并发敏感点 5 处交接 A2.17 含 @Version 透明乐观锁降级[7 个 hr 状态机实体全部声明 versionProp]。**hr 状态机审查 S 级拆分 1/2 + 2/2 全部 done**）。**A2.8 purchase 状态机审查（A 级单域，9 实体 × 三轴，29 状态字段）已于 2026-07-28 完成**（详见 `docs/audits/2026-07-28-0230-arm-ma2-purchase-state-machine.md`）。下表「状态机正确性」行 pur 列由 `❓` 推进至 `⚠️(P1)`（采购九实体状态机：Order/Receive/Invoice/Payment/Return/Requisition/Rfq/Quotation/SupplierScorecard × 三轴 docStatus/approveStatus/业务轴[paidStatus/receiveStatus/writtenOffStatus/standing/status]，核心契约经实仓逐项证据确认——**主路径状态迁移守卫齐全**[PROC 路径 `validateNotCancelled`/`validateTransition*`/`validateBusinessRules*` 三段守卫 + `doApprove`/`doReject`/`doReverseApprove`/`doCancel` 四动作齐全]、@BizMutation 事务回滚保证 approve 触发的跨域写[承付 commit/release + 库存 incoming/outgoing + 过账 AP_INVOICE/PAYMENT/PURCHASE_RETURN/PURCHASE_INPUT + AVL SUSPENDED]失败原子性、reverseApprove 红冲闭环强一致[PROC 路径 doReverseApprove 设 REJECTED + 清 approvedBy/At + posted=false + 凭证 reverse 经 IErpFinVoucherBiz Facade]、跨域写经 I*Biz Facade[production 代码无 daoFor 跨域写直写]；零 P0——三个候选 P0 经证据证伪或降级：(1) Quotation/Rfq reverseApprove→SUBMITTED 违反 owner doc §2 但不破坏红冲闭环一致性[Quotation/Rfq 无 posted 副作用，按 finance P1-MA2-031 + mfg P1-MA2-035 + hr P1-MA2-039~042 同型裁决 P1]；(2) INLINE reject/withdrawApproval 缺 isCancelled 守卫但不破坏主终态[docStatus=CANCELLED 持有，approveStatus 副轴漂移不影响业务查询，按危害有限 P1]；(3) PurReversalListener.rollbackReceive 不对称但 Javadoc deliberate + 不破坏业财一致[凭证已红冲 GL 平衡，仅 purchase 域 receive 状态悬挂，按功能性悬挂 P1]；3 项 P1 待 MR1：P1-MA2-049 Quotation/Rfq reverseApprove→SUBMITTED 违反 owner doc §2 强制 REJECTED 契约漂移 / P1-MA2-050 INLINE reject/withdrawApproval 绕过 isCancelled 守卫致 CANCELLED 单据 approveStatus 副轴漂移 / P1-MA2-051 PurReversalListener.rollbackReceive 不对称致冲销后 receive APPROVED+posted=false 悬挂；3 项 P2 watch-only：P2-MA2-053 三种并行模式 owner doc 未声明 / P2-MA2-054 死代码 WithdrawApproval/Reject Processor 未接线 / P2-MA2-055 payment writtenOffStatus 复用 paid-status 字典语义漂移；6 项已登记 MA1/MA2 finding 运行时复核无升级：P1-MA1-022 跨域只读维持治理缺陷 / P1-MA2-001 暂估冲回状态机角度无升级 / P1-MA2-002 多币种状态机角度无影响 / **P1-MA2-003 settle 守卫缺口维持 P1 不升 P0**[APPROVED 是 settle 守卫的必要不充分条件] / P2-MA2-008 并发核销交接 A2.17 / P2-MA1-026 scorecard defaultValue 无升级；并发敏感点 5 处交接 A2.17 含 @Version 透明乐观锁降级[7 个采购状态机实体均声明 versionProp]）。**A2.9 sales 状态机审查（A 级单域，7 实体 × 三轴，18 dict-bound 状态字段）已于 2026-07-28 完成**（详见 `docs/audits/2026-07-28-0400-arm-ma2-sales-state-machine.md`）。下表「状态机正确性」行 sal 列由 `❓` 推进至 `⚠️P1(A2.9✅)`（销售七实体状态机：Order/Delivery/Invoice/Receipt/Return/Quotation/Contract × 三轴 docStatus/approveStatus/业务轴[receivedStatus/deliveryStatus/writtenOffStatus 复用 received-status/isAccepted 布尔]，核心契约经实仓逐项证据确认——**主路径状态迁移守卫齐全**[PROC 路径 6 实体 4 主动作 + 全 cancel 经 BizModel.cancel→大 Processor 三段守卫]、@BizMutation 事务回滚保证 approve 触发的跨域写[承付 commit/release-on-invoice-approve + 库存 outgoing/incoming **含可用量校验销售独有**经库存域 doConfirm→validateAvailable 强制落实 + 过账 AR_INVOICE/RECEIPT/SALES_RETURN + intercompany + 信用占用/hold 三级策略]失败原子性、reverseApprove 红冲闭环强一致[PROC 路径 doReverseApprove 设 REJECTED + 清 approvedBy/At + posted=false + 凭证 reverse + ensureReversed + 承付 release hook]、跨域写经 I*Biz Facade[production 代码无 daoFor 跨域写直写]、SalReversalListener 反向回滚[finance→sales 三实体 Invoice/Receipt/Return 降级 + delivery 仅 posted=false deliberate 不对称]、**出库 approve 可用量校验销售独有约束已落实**经 IErpInvStockMoveBiz.generateMove→ErpInvStockMoveProcessor.doConfirm→validateAvailable、**退货退款红字收款单+回退发票状态已落实**经 ReturnRefundOrchestrator.orchestrateRefund；零 P0——四个候选 P0 经证据证伪或降级：(1) Contract reverseApprove→SUBMITTED 违反 owner doc §2 但不破坏红冲闭环一致性[Contract 无 posted 副作用，按 finance P1-MA2-031 + mfg P1-MA2-035 + hr P1-MA2-039~042 + purchase P1-MA2-049 同型裁决 P1]；(2) INLINE withdrawApproval + Contract 全 INLINE 缺守卫但不破坏主终态[docStatus=CANCELLED 持有，approveStatus 副轴漂移不影响业务查询，按危害有限 P1]；(3) SalReversalListener.rollbackDelivery 不对称但 Javadoc deliberate + 业务侧恢复路径完整[与 purchase P1-MA2-051 不同——sales delivery 经 ensureReversed 链可恢复]+ 不破坏业财一致，按 P2 watch-only；(4) 过账 tryPost 吞异常悬挂与 finance P1-MA2-032 + purchase 同型根因，Deferred 兜底，不升 P0；2 项 P1 待 MR1：P1-MA2-056 Contract reverseApprove→SUBMITTED 违反 owner doc §2 强制 REJECTED 契约漂移 / P1-MA2-057 6 实体 INLINE withdrawApproval + Contract 全 INLINE 缺 isCancelled/customer active/lines empty 守卫致 CANCELLED 单据 approveStatus 副轴漂移——与 purchase P1-MA2-049/050 同型；3 项 P2 watch-only：P2-MA2-056 三种并行模式 + 6 实体 vs Contract 模式分裂 owner doc 未声明 / P2-MA2-057 SalReversalListener.rollbackDelivery 不对称 Javadoc deliberate owner doc 未同步[与 purchase P1-MA2-051 同型但降为 P2——业务侧恢复路径完整] / P2-MA2-058 ErpSalReturn writtenOffStatus/returnStatus/refundStatus 未落地为 ORM 存储字段[returns.md:88-93 已显式漂移注记]；9 项已登记 MA1/MA2 finding 运行时复核无升级：P1-MA1-022 跨域只读维持治理缺陷 / P1-MA2-009 多币种状态机角度无影响 + settle 守卫完整 / P2-MA2-010 invoice 金额守卫缺口维持 P2[必要不充分守卫] / P2-MA2-011 退货过账 drift 状态机角度无影响 / P2-MA2-012 信用控制 doc 漂述状态机角度已落实 / P2-MA2-013 settle 维度功能缺口 / P2-MA2-014 并发核销交接 A2.17 / P2-MA2-015 期间配比归 A2.3 / P2-MA2-038 双路径设计并行非分歧；并发敏感点 5 处交接 A2.17 含 @Version 透明乐观锁降级[7 个 sales 状态机实体 ErpSalOrder/Delivery/Invoice/Receipt/Return/Quotation/Contract 均声明 versionProp]）。**A2.10 assets 状态机审查（A 级单域，资产卡片+折旧计划+7业务单据+CIP+盘点+维修状态机，18 状态字段）已于 2026-07-28 完成**（详见 `docs/audits/2026-07-28-0400-arm-ma2-assets-state-machine.md`）。下表「状态机正确性」行 assets 列由 `❓` 推进至 `⚠️P1(A2.10✅)`（资产状态机：资产卡片生命周期 6 态 + 折旧计划 4 态 + 7 业务单据双轴[Movement/ValueAdjustment/Disposal/Capitalization/Split/Merge + CIP/Inventory/Maintenance] 18 状态字段，核心契约经实仓逐项证据确认——**主路径状态迁移守卫齐全**[资产卡片生命周期迁移经资本化/处置/拆分/合并/盘点 Processor 副作用齐全 + 7 业务单据 6 PROC + 1 INLINE 模式]、@BizMutation 事务回滚保证 approve 触发的跨域写[建卡+折旧计划生成+折旧计提+处置终态+资本化/处置/拆分/合并过账]失败原子性、reverseApprove 红冲闭环经大 Processor 路径强一致[Capitalization/Disposal/ValueAdjustment →REJECTED 合规 + Split/Merge THROW 不可逆契约合规]、跨域写经 I*Biz Facade[production 代码无 daoFor(ErpFin*) 直写]；零 P0——四个候选 P0 经证据证伪或降级：(1) Movement reverseApprove→SUBMITTED 违反 owner doc §2 但不破坏红冲闭环一致性[Movement 无 posted 副作用，按 finance P1-MA2-031 + mfg P1-MA2-035 + hr P1-MA2-039~042 + purchase P1-MA2-049 + sales P1-MA2-056 同型裁决 P1]；(2) Movement 全 INLINE 缺 isCancelled 守卫但不破坏主终态[docStatus=CANCELLED 经服务层不可达仅经 useLogicalDelete 承载，按危害进一步收窄 P1]；(3) Capitalization/Disposal tryPost 吞异常悬挂 + reverseApprove 不对称但 DeferredPostingSweepJob 兜底 + LOG 可见性 + 期末试算人工兜底，按功能性悬挂 P1；(4) IDLE 死状态 + 联动缺失按 finance P1-MA2-031 + mfg P1-MA2-035 + hr P1-MA2-039 同型裁决 P1，不破坏 IN_SERVICE 主路径折旧闭环；4 项 P1 待 MR1：P1-MA2-058 Movement reverseApprove→SUBMITTED INLINE 违反 owner doc §2 强制 REJECTED 契约漂移 / P1-MA2-059 ErpAstMovement 全 5 INLINE 动作缺 isCancelled 守卫致 CANCELLED 单据 approveStatus 副轴漂移 / P1-MA2-060 Capitalization/Disposal tryPost 吞异常悬挂 + reverseApprove 仅 posted=true 时回滚资产状态致资产侧状态悬挂 / P1-MA2-061 ErpAstAsset IDLE 状态机迁移完全未实现[IN_SERVICE↔IDLE 无任何 writer]+联动缺失；3 项 P2 watch-only：P2-MA2-059 state-machine.md 缺 7 业务单据独立章节 / P2-MA2-060 PROC vs INLINE 模式 owner doc 未声明 / P2-MA2-061 6 业务单据 Processor cancel 方法死代码未接线；5 项已登记 MA1 finding 运行时复核——DISPOSED/CANCELLED 经证伪非死状态：P1-MA1-008 propId 无状态机影响 / P1-MA1-016 finance→assets 跨域只读无升级 / P1-MA1-022 跨域只读无升级 / **P2-MA1-023 DISPOSED 经 Split/Merge 可达非死状态，仅 owner doc drift** / **P2-MA1-024 CANCELLED 经 Capitalization.reverseApprove + Disposal.approve 可达非死状态，仅 owner doc drift**；并发敏感点 5 处交接 A2.17 含 @Version 透明乐观锁降级[10 个 assets 状态机实体全部声明 versionProp]）。

| 维度 | finance | mfg | hr | assets | pur | sal | qa | crm | prj | cs | ct | b2b | inv | md | mnt | drp | aps | log | notify | Skill |
|------|---------|-----|----|----|------|-----|----|----|----|----|----|-----|-----|-----|-----|-----|-----|-----|--------|-------|
| 业财端到端 | ⚠️P1 | ❓ | N/A | ❓ | ⚠️P1 | ⚠️P1 | N/A | N/A | ❓ | N/A | ❓ | N/A | ❓ | N/A | ❓ | N/A | N/A | N/A | N/A | 新维度+flow-overview |
| 状态机正确性 | ⚠️P1 | ⚠️P1(A2.6a✅;A2.6b✅) | ⚠️P1(A2.7a✅;A2.7b✅) | ⚠️P1(A2.10✅) | ⚠️P1(A2.8✅) | ⚠️P1(A2.9✅) | ⚠️P0→fix-plan + P1(A2.12✅) | ⚠️P1(A2.14✅) | ⚠️P1(A2.13✅) | ✅(A2.14✅) | ⚠️P1(A2.14✅) | ⚠️P1(A2.14✅) | ⚠️P1(A2.11✅) | N/A | ⚠️P1(A2.14✅) | N/A | ⚠️P1(A2.15✅) | ⚠️P1(A2.15✅) | N/A | state-machine-review |
| 库存核算一致性 | ⚠️P1 | ❓ | N/A | N/A | ❓ | ❓ | N/A | N/A | N/A | N/A | N/A | N/A | ⚠️P1 | N/A | ❓ | ❓ | N/A | N/A | N/A | 新维度 |
| 预算与承付 | ⚠️P1(A2.16✅) | N/A | N/A | N/A | ⚠️P1(A2.16✅) | ⚠️P1(A2.16✅) | N/A | N/A | ❓ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | 新维度 |
| 并发与乐观锁 | ⚠️P0→fix-plan + P1(A2.17✅) | ⚠️P1(A2.17✅) | ⚠️P1(A2.17✅) | ⚠️P1(A2.17✅) | ⚠️P1(A2.17✅) | ⚠️P1(A2.17✅) | ⚠️P1(A2.17✅) | ⚠️P1(A2.17✅) | ⚠️P1(A2.17✅) | ⚠️P1(A2.17✅) | ⚠️P1(A2.17✅) | ⚠️P1(A2.17✅) | ⚠️P0→fix-plan + P1(A2.17✅) | N/A | ⚠️P1(A2.17✅) | N/A | ⚠️P0→fix-plan + P1(A2.17✅) | ⚠️P1(A2.17✅) | N/A | open-ended-audit |

> **A2.12 quality 状态机审查（A 级单域，16 状态字段）已于 2026-07-28 完成**（详见 `docs/audits/2026-07-28-1020-arm-ma2-quality-state-machine.md`）。下表「状态机正确性」行 qa 列由 `❓` 推进至 `⚠️P0→fix-plan + P1(A2.12✅)`（质量状态机核心契约：NCR 5 态 + 召回 5 态 + CAPA 生命周期 + SPC 失控预警 + 强制质检门控 InspectionTrigger.enforceGate config-gated + NCR 过账 SCRAP/RETURN/CONCESSION 分派 + posted 三件套 + reverseNcr 红冲闭环对称 + 跨域写经 I\*Biz Facade 全合规 经证据逐项确认；**1 项 P0** P0-MA2-017 ErpQaInspectionBizModel passInspection/failInspection/reInspect 缺状态守卫致强制质检门控可绕过不合格品 silent 入库 [已注入异步 fix plan `2026-07-28-1020-arm-fix-p0-ma2-017-qa-inspection-state-guard`]；3 项新 P1：P1-MA2-064 业务单据作废联动取消质检单未落地 / P1-MA2-065 QualityGoal/RiskRegister/Calibration/Review/SPC-CalcStatus-STALE/CAPA-OVERDUE dict 死状态合并裁决 / P1-MA2-066 NCR resolve 允许无 CAPA 直接关闭闭环不变量缺口；2 项新 P2 watch-only：P2-MA2-063 state-machine.md 缺 8 状态承载实体独立章节 / P2-MA2-064 §审查提示「事件驱动」vs §实现偏离补注未同步；2 项已登记 MA1 finding [P1-MA1-012 propId / P1-MA1-022 跨域只读] 运行时复核无升级；MANUAL_POST NCR 过账悬挂窗口期同 finance P1-MA2-032 + hr P1-MA2-048 + assets P1-MA2-060 同型根因交接；并发敏感点 4 处交接 A2.17 含 @Version 透明乐观锁降级[4 个 quality 状态机实体均声明 versionProp]）。

> **A2.13 projects 状态机审查（A 级单域，16 状态字段）已于 2026-07-28 完成**（详见 `docs/audits/2026-07-28-1020-arm-ma2-projects-state-machine.md`）。下表「状态机正确性」行 prj 列由 `❓` 推进至 `⚠️P1(A2.13✅)`（项目状态机核心契约：项目 5 态 + 任务 4 态 + 工时审批轴 + DAG 成环检测上行链+HashSet+maxDepth + 项目结算三轴 + PnL 2 态 + 工时成本凭证跨域过账经 IErpFinVoucherBiz Facade + ON_HOLD 费用归集暂停硬拒绝（requireReferenceable 双路径）+ 已取消保留归集成本 + 项目关闭后拒绝引用 + 跨域 Facade 全合规 经证据逐项确认；**零 P0**（5 个候选 P0 经证据证伪或降级：项目完成未强制任务已结束降级 P1 owner doc 用词「或确认剩余不再执行」软门控 / 任务依赖 DAG 成环校验缺失证伪 detectCycle 完整实现 / ON_HOLD 费用归集未暂停证伪双路径硬拒绝 / 项目关闭后仍可被引用证伪 requireReferenceable COMPLETED/CANCELLED 全拒绝 / 工时成本凭证过账失败悬挂降级 P1 同 finance/hr/assets/qa 同型根因）；4 项新 P1：P1-MA2-067 closeProject OPEN→COMPLETED 未强制任务已结束 owner doc §迁移完整性+§审查提示显式前置缺失 / P1-MA2-068 TimesheetPostingDispatcher tryPost 吞异常悬挂致 posted=false 无告警闭环——同 finance P1-MA2-032+hr P1-MA2-048+assets P1-MA2-060+qa A2.12 MANUAL_POST NCR 同型根因 / P1-MA2-069 Milestone/Billing/CostCollection doc-status 字典语义复用偏移 + CRUD 桩死状态合并裁决——Milestone task-status 4 态 + Billing project-status 5 态全死 + CostCollection 写 APPROVED 不在 project-status 字典（dict-value drift，与 finance P1-MA1-018 同型）/ P1-MA2-070 startProject 缺「项目信息完整/起止日期有效/预算已定」前置校验 + cancelProject 接受 DRAFT/ON_HOLD 超出 owner doc 单源声明；2 项新 P2 watch-only：P2-MA2-065 state-machine.md 缺 5 状态承载实体独立章节 / P2-MA2-066 §7 IErpFinAcctDocProvider vs 实现 IErpFinVoucherBiz 文字漂移；2 项已登记 MA1 finding [P1-MA1-010 多币种四件套 propId / P1-MA1-022 跨域只读 daoFor ErpMdSubject+ErpFinExpenseClaimLine] 运行时复核无升级；5 处并发敏感点交接 A2.17 含 @Version 透明乐观锁降级[6 个 prj 状态机实体均声明 versionProp]）。

> **A2.14 crm+cs+contract+b2b+maintenance 状态机审查（A+B 合并，crm Lead/Event/stageId + cs Ticket/SLA + contract 合同/InvoicePlan + b2b EDI/ASN + maintenance visit/request/PostingDispatcher）已于 2026-07-28 完成**（详见 `docs/audits/2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`）。下表「状态机正确性」行 crm/cs/ct/b2b/mnt 列由 `❓` 推进至 `⚠️P1(A2.14✅)` / `✅(A2.14✅)` / `⚠️P1(A2.14✅)` / `⚠️P1(A2.14✅)` / `⚠️P1(A2.14✅)`（五域状态机核心契约：crm Lead 5 态 + Event 3 态 + 转化跨域 Facade + cs Ticket 6 态 + SLA 计时联动完整 + SLA 升级 Job + contract 合同 6 态主路径迁移齐全 + InvoicePlan SUSPENDED 拦截 + e-signature SPI + b2b EDI 8 态 + ASN 4 态 + ASN 跨域收货豁免已登记 + maintenance visit 5 态 + request 6 态全迁移 + EquipmentStatusLinker + DowntimeEntry 时间驱动 经证据逐项确认；**零 P0**（5 个候选 P0 经证据证伪或降级：cs SLA 恢复累加缺失**证伪** reopen 保留 startDateTime 累加重算落实 / contract EXPIRED Job 降级 P1 manual expire 存在 + InvoicePlan unposted DRAFT 经审批可拦截 + missing-automation 同型 / b2b EDI 自动化降级 P1 config-gated OFF 默认 + Mock transport Deferred + 手工迁移可达 / maintenance 过账悬挂降级 P1 同 finance/hr/assets/qa/projects tryPost 吞异常同型 + config-gated OFF + reverseLabor 幂等 / crm stageId 降级 P1 deliberate design + reporting skew 非数据破坏）；**cs 域 zero P1**（候选 P0 证伪 SLA 核心完整）；6 项新 P1：P1-MA2-071 contract EXPIRED 自动到期 Job 缺失 / P1-MA2-072 contract NEGOTIATION→TERMINATED 缺失 / P1-MA2-073 b2b EDI 出站自动化全部缺失 TransportManager wired-but-uncalled / P1-MA2-074 maintenance Labor/Issue 过账 tryPost 吞异常悬挂同型 / P1-MA2-075 crm stageId 单向递增守卫未实现 owner doc 契约漂移 + Funnel reporting skew / P1-MA2-076 crm Event reminderMinutesBefore 字段死字段 silent functional gap；4 项新 P2 watch-only：P2-MA2-067 cs NEW>1h/ASSIGNED>2h 滞留升级未实现 + findSlaWarnings 无 scheduler / P2-MA2-068 b2b state-machine.md 自动化承诺 vs README/MFT transport Deferred 文档不一致 / P2-MA2-069 b2b TO_CANCEL dict 死状态 / P2-MA2-070 5 域 state-machine.md 缺多状态承载实体独立章节；6 项已登记 MA1 finding [P1-MA1-009 crm DECIMAL / P1-MA1-011/013 maintenance propId / P1-MA1-022 5 域跨域只读 / P1-MA1-029 contract InvoicePlan 半治理 / P2-MA1-027 contract CANCELLED drift / P2-MA1-028 maintenance IN_PROGRESS drift] 运行时复核无升级；9 处并发敏感点交接 A2.17 含 @Version 透明乐观锁降级[5 域全部状态承载实体声明 versionProp]）。

### 2.3 MA3 — 文档-实现一致性层

> **A2.16 预算与承付正确性（承付释放路径完整性）多维业务正确性审查已于 2026-07-28 完成**（详见 `docs/audits/2026-07-28-1249-arm-ma2-budget-commitment-release.md`）。下表「预算与承付」行 finance 列维持 `⚠️P1(A2.16✅)`（既有 A2.5b P1 之上叠加 A2.16 4 项 P1 中 3 项触及 finance 余量/聚合语义），pur/sal 列由 `❓` 推进至 `⚠️P1(A2.16✅)`（承付释放路径完整性核心契约：3 接入点齐全 [commit/release-on-cancel/release-on-invoice-approve 采购-sales 对称] + §reject release-on-receive 落实 [ErpPurReceiveProcessor 零 SPI] + 重复释放守卫齐全 [ERR_BUDGET_COMMITMENT_ALREADY_RELEASED] + 取消后再发票容错齐全 + 多年度跨期发票余量一致 + 预算控制强一致 [@BizMutation SYNC 同事务] + 聚合排除已红冲 + config-gate 默认 false 保护 + 与 owner doc budget.md §承付会计 §3 接入点 + §reject + §sales 承付扩展 + §配置项 + §commitment 不结转 全部命中 经证据逐项确认；**零 P0**（六个候选 P0 经证据证伪或降级：部分开票全额释放降级 P1-MA2-081 容错守卫齐全不阻断主路径 / release-on-receive 误释放**证伪** ErpPurReceiveProcessor 零 SPI 接入 / 采购退货未释放降级 P1-MA2-082 保守方向偏移 + config-gated / AP 冲销后 commitment 未恢复降级 P1-MA2-083 同 finance P1-MA2-032 posting-悬挂同型根因 / 取消后再开票绕过守卫**证伪** 严格错误码过滤 + 无裸 voucher 操作 / sales Dr/Cr 方向错误**证伪** subject.direction 自动取 + 结构对称已强制）；**4 项新 P1**（P1-MA2-081 部分开票释放语义未声明 owner doc drift / P1-MA2-082 采购退货未释放承付释放路径完整性缺口保守方向偏移 + config-gated / P1-MA2-083 AP/AR 发票冲销后 commitment 未恢复跨冲销一致性缺口同 finance P1-MA2-032 同型根因 / P1-MA2-084 `ErpFinBudgetControlBiz.aggregateAmount` 实际聚合包含 COMMITMENT 语义混淆等价正确但脆弱——全部不破坏主路径，**目标 MR1**[承付属 MA2 业务正确性批次，与既有 A2.1-A2.15 业务正确性 P1 同型]）；**1 项新 P2** watch-only（P2-MA2-073 TestErpSalOrderCommitment 缺 Dr/Cr 方向 voucher line 断言，结构对称已强制仅缺回归保护）；**3 项已登记 MA1/MA2 finding [P1-MA1-022 跨域只读 / P1-MA2-001 GRNI 冲回 / P1-MA2-009 多币种 O2C] 运行时复核无升级**（承付接入点路径无运行时影响 / GRNI 冲回与承付释放语义正交 / 多币种 O2C 不引入承付侧新缺口）；**4 处并发敏感点交接 A2.17**[并发 commit/release 同一订单 + 部分开票并发释放 + ErpFinVoucher versionProp 透明乐观锁降级]）。

> **A2.15 aps+logistics 状态机审查（C 级合并，aps OperationOrder 5 态 + Schedule 排产方案 3 态 + 排产引擎 + 跨域只读 + logistics Shipment 6 态 + 网关 SPI + 轮询 + FREIGHT 过账 Facade + path-2 到岸成本 config-gated）已于 2026-07-28 完成**（详见 `docs/audits/2026-07-28-1249-arm-ma2-aps-logistics-state-machine.md`）。下表「状态机正确性」行 aps/log 列由 `❓` 推进至 `⚠️P1(A2.15✅)` / `⚠️P1(A2.15✅)`（两域状态机核心契约：aps OperationOrder 5 态主路径迁移齐全 + Schedule 3 态全守卫 + 排产引擎 scheduleForward/Backward + insertRushOrder 区间重排严格窗口限定 + ATP/CTP 跨域只读 + logistics Shipment 6 态全守卫 + 网关 SPI 重试 maxRetries=3 指数退避 + deadLetter 死信保留 ADVISED + scanForPolling 轮询兜底 + DELIVERED→IErpFinVoucherBiz.post Facade + path-2 config-gated IErpInvLandedCostBiz Facade + 跨域写经 I\*Biz Facade 全合规 经证据逐项确认；**零 P0**（5 个候选 P0 经证据证伪或降级：aps 并发排产产能双倍占用**交接 A2.17** owner doc §4 Deferred / aps PLANNED→DRAFT 重排全局化**证伪** insertRushOrder:62-122 严格窗口限定 / logistics DELIVERED 运费过账失败悬挂**降级 P1-MA2-080** 同 finance P1-MA2-032 + hr P1-MA2-048 + assets P1-MA2-060 + qa A2.12 + projects P1-MA2-068 + maintenance P1-MA2-074 tryPost 吞异常同型根因 / logistics 网关异常重试耗尽无告警**降级 P1-MA2-080** 同 b2b P1-MA2-073 + cs P2-MA2-067 missing-automation 同型 / logistics 关联出库单锁定释放**证伪（物流侧角度）** 概念性弱指针非显式锁）；**4 项新 P1**（P1-MA2-077 aps OperationOrder start/complete/cancel 完全缺状态守卫 if/else 死代码 FINISHED/CANCELLED 可被非法迁移——与 qa P0-MA2-017 不同型（aps 无强门控被绕过）/ P1-MA2-078 aps+logistics 取消执行中工序/在途缺审批门控合并 owner doc §6 显式声明生产主管/物流主管审批未落地 / P1-MA2-079 logistics 部分签收完全未实现 owner doc §2/§4 声明但无 TRACKING_EVENT_PARTIAL 常量 + 无部分签收字段 / P1-MA2-080 logistics 网关异常重试耗尽 + DELIVERED 运费过账失败缺告警闭环/TODO 合并——deadLetter 不派发告警 + onDelivered 吞异常 + scanForPolling 不重试 DELIVERED-PENDING，比 peer dispatcher 更严重因 logistics 无 DeferredPostingSweepJob 兜底）；**2 项新 P2** watch-only（P2-MA2-071 2 域 state-machine.md 缺 Schedule/排产引擎/ShipmentLog/网关 SPI/轮询/过账 Facade 独立章节 / P2-MA2-072 关联出库单"锁定/释放"概念性弱指针 owner doc drift）；1 项已登记 MA1 finding（P1-MA1-022 aps 跨域只读 daoFor）运行时复核**无升级**（跨域只读是 ATP/CTP + 排产约束输入副作用，不参与状态机迁移判定，异常路径经 @BizMutation 事务回滚覆盖）；**6 处并发敏感点交接 A2.17** 含 @Version 透明乐观锁降级（aps OperationOrder/Schedule + logistics Shipment/ShipmentLine/ShipmentParcel/ShipmentLog/Carrier/CarrierConfig 全部声明 versionProp））。**MA2 状态机正确性维度全域 13 业务列全部 ✅/⚠️(P1)，notify/md/drp N/A，无 ❓**（aps/log 最后两列 ❓ 由本审计推进至 ⚠️(P1)，状态机正确性维度全域收尾）。


| 维度 | 覆盖范围 | Skill |
|------|---------|-------|
| 设计文档基线 | 全域 docs/design/（7 全局 + 18 域目录 + 18 跨域模式）。**终态：`⚠️(P1)`**——A3.1 已于 2026-07-28 完成（`docs/audits/2026-07-28-1510-arm-ma3-design-doc-baseline.md`）。Verdict FAIL：零 BLOCKER（功能覆盖度维度 PASS，95.8% 覆盖率 + 标配功能深度有 dedicated owner doc + 5 未覆盖项显式分级）；13 项 MAJOR → P1-MA3-001~013（目标 MR2，文档类：系统性实现状态泄漏 dim3 / finance+master-data+8 第二批扩展域 README owner-doc 边界 dim5 / logistics 缺 architecture 拆分 dim5 / 占位行为泄漏 dim2 / 角色冲突+8 域遗漏 dim6+9 / 8 域无角色基线 dim9 / product-scope 陈旧 dim4 / 危险操作审计+状态码目录重复 dim12）；8 项 P2 watch-only（P2-MA3-014~021）；MA2 owner-doc drift 复核全部确认分类一致 | design-doc-audit |
| 设计完整性扫描 | 全域 vs product-scope | design-completeness-scan |
| owner doc vs 代码 drift | S+A 级域抽样 | 新维度 |
| API 契约一致性 | 全域 model/*.api.xml | 新维度 |
| 索引路由 | docs/index.md + 子索引 | index-routing-audit |

### 2.4 MA4-MA7 — 其余层（全域或跨域维度，按复杂度合并）

| 维度 | 覆盖范围 | Skill |
|------|---------|-------|
| 代码质量（MA4） | S 级域单独 + A 级合并 | `code-quality-audit-prompt.md` |
| **assets Processor 链路专属（MA4 新增）** | assets 单域（48 Processor 折旧引擎/处置/价值调整/资本化） | `code-quality-audit-prompt.md` |
| view.xml drift（MA4） | S+A 级域抽样 | `multi-dimensional-audit-prompt.md` |
| i18n 完整性（MA4） | 全域合并（跑 checker） | i18n-coverage-checker.sh |
| 测试覆盖深度（MA5） | S 级域逐个（finance/mfg/hr/assets） | `open-ended-audit-prompt.md` |
| 测试隔离性（MA5） | 全域合并 | `open-ended-audit-prompt.md` |
| E2E 有效性（MA5） | 抽样 260+ spec | `open-ended-audit-prompt.md` |
| 权限注解（MA6） | 全域 grep + S 级抽样 | `open-ended-audit-prompt.md` |
| 数据权限（MA6） | S+A 级抽样 | `multi-dimensional-audit-prompt.md` |
| **保护区域纪律（MA6 新增）** | accounting 过账 / data deletion 是否有 owner doc + 测试 + plan-audit 证据 | `multi-dimensional-audit-prompt.md` |
| 错误码完整性（MA7） | 全域合并 | `open-ended-audit-prompt.md` |
| 索引完整性（MA7） | S+A 级域 | `open-ended-audit-prompt.md` |
| N+1 查询（MA7） | S 级域抽样 | `open-ended-audit-prompt.md` |
| CI/guard 激活（MA7） | 全域合并 | compliance-checker |

### 2.5 新增维度（v2 — 经独立子代理审查后补充）

> 以下维度在 v1 中缺失，经覆盖面审查子代理发现后补充。

> **A2.18 多账套/多公司隔离系统性审查（多维）已于 2026-07-28 完成**（详见 `docs/audits/2026-07-28-1510-arm-ma2-multi-company-isolation.md`）。下表「多账套/多公司隔离」行终态标记 `❓` → `⚠️(P1)`（核心结论：多公司/多账套隔离的**写路径 + 自然键层基本成立**——全域 ~70 事务单据 UK_*_CODE_ORG (code, orgId) 正确 + 多账套传播 stamp acctSchemaId + 法人根解析环形守卫存在 + 转移定价 cache 方向性正确 + CoA/CostMethod/折旧 cache SAFE；**读路径隔离机制全仓未落地**——平台仅支持 tenant 自动过滤而本项目 0 实体启用 useTenant + 19 模块 erp-{module}.data-auth.xml 全部 `<objs/>` 空规则 + 0 个自定义 IDataAuthChecker/IQueryTransformer + IServiceContext/IContext 均无 getOrgId() + 11 dashboard BizModel 经 IDaoProvider 直访绕过仅有的（空）认证管道；**单组织种子 176 行全部 orgId=2 完全掩盖跨组织泄漏**。A2.17 交接点复核收口：P0-MA2-018 维持 deferred（billR 无 acctSchemaId 列 + 加 orgId 不足修复——判别列 postingType/isReversed/acctSchemaId 全在 voucher 不在 billR）；P0-MA2-020 维持 completed（UK_INV_STOCK_BALANCE_NATURAL 已正确含 orgId）。**零 P0**（4 个候选 P0 经证据证伪或降级：orgId 跨组织泄漏降级 P1 单组织种子下无实际腐败 + 账套串户降级 P1 仅 multi-schema-enabled=true 时显现 + 法人根解析环形守卫证伪 + 合并抵消作用域误抵消仅理论）；**7 项新 P1** 登记 arm-index §P1 汇总待 MR1：P1-MA2-093 orgId 查询隔离全仓未落地 / P1-MA2-094 orgId 写入客户端可任意指定 / P1-MA2-095 acctSchemaId 读路径泄漏 / P1-MA2-096 ErpFinGlBalance 无 DB 强制自然键 / P1-MA2-097 跨公司配对 owner doc 算法漂移 + ErpFinIntercompanyMatch 审计列全空 / P1-MA2-098 runMatching 非幂等 / P1-MA2-099 GL 映射 cache 默认配置跨组织泄漏。多公司/多账套维度终态全域 ⚠️(P1)。**MA2 全部 18 个工作项（A2.1–A2.18）现已全部 done**——MR1 R1.0 可启动）。

| 维度 | 里程碑 | 触发依据 | 覆盖范围 | Skill |
|------|--------|---------|---------|-------|
| **并发与乐观锁** | MA2（A2.17）✅ done | use-case-implementation-audit 标记 3 处并发缺口（UC-SAL-10 并发扣批次 / UC-INV-08 乐观锁 / UC-SAL-10 乐观锁）；ERP 核心并发风险（库存扣减/发票核销/期间结账） | S+A 级域 | `open-ended-audit-prompt.md` |
| **多账套/多公司隔离** ⚠️(P1) done | MA2（A2.18） | flow-overview §4.4 多套科目表并行；project-vision "产品化通用 ERP" 定位 | S 级域 + finance | `multi-dimensional-audit-prompt.md` |
| **可定制性验证** | MA3（A3.8） | project-vision 核心价值"可定制"；需验证 Delta/扩展字段实际可用且不破坏基线 | 抽样 | `open-ended-audit-prompt.md` |
| **保护区域纪律** | MA6（A6.4） | ai-autonomy-policy §保护区域 6 区域；accounting/data-deletion 是否有合规路径 | 全域 | `multi-dimensional-audit-prompt.md` |
| **assets Processor 链路** | MA4（A4.3） | assets Proc=48 全域最高密度；折旧正确性直接影响财务报表 | assets 单域 | `code-quality-audit-prompt.md` |

## 3. 未闭包发现清单（步骤 2 产物）

> 来源：遍历 `docs/audits/` 下全部审计文件提取

### 3.1 已闭包 finding（无需重复审计，仅记录）

| Finding | 状态 | 闭包证据 |
|---------|------|---------|
| F1 daoFor 跨域（Type 1 chained/variable-split） | ✅ done | plan 2026-07-24-0941-1 收尾 |
| F2 字典真相碎裂 | ✅ done | plan 2026-07-26-0300-1 doc-status 6 域统一 |
| F3 ORM DAG 边登记 | ✅ done | plan 2026-07-24-0930-3 Phase 2 |
| F4 隐性共享内核 | ✅ done | plan 2026-07-24-1400-1 显式登记 + R12 守卫 |
| F5 notify owner doc | ✅ done | plan 2026-07-24-0930-3 Phase 3 |
| F6 mfg 依赖 qa 生成常量 | ✅ done | plan 2026-07-24-1400-2 Phase 1 |
| F7 drp 命名前缀 | ✅ done | 裁决=登记例外 |
| F8 compliance checker CI | ✅ done | plan 2026-07-24-0930-1 |
| F9 web 冒烟测试 @Disabled | ✅ done | plan 2026-07-24-0930-1 @Tag 方案 |

### 3.2 残留风险与 deferred successor（审计输入）

> MA1 ORM 审计（A1.1–A1.9）已于 2026-07-27 完成对 §3.2 进入 MA1 项的覆盖；MA1 跨模块依赖/DAG 审计（A1.10）已于 2026-07-27 完成对跨模块依赖维度的覆盖（DAG 零循环零禁止方向、外部声明 100% 覆盖、3 项 P1 待 MR1）。结论登记在"处理"列。

| 来源 | 描述 | 严重性 | 处理 |
|------|------|--------|------|
| arch-gov §残留风险 1 | daoFor `findAllByQuery` Type 1 watch-only residual（113 处站点，机械替换候选 <10） | P2 | watch-only，不在本 roadmap |
| arch-gov §残留风险 1 | daoFor Type 4 设计边界错误（~10-30 处）阻塞 successor | P1 | **MA1 已覆盖**：ORM 审计 dim 7（跨模块引用一致性）全域 0 blocker——所有跨模块 refEntityName 经机制 B（notGenCode 外部实体）正确声明。daoFor Type 4 在 ORM 层无残留；下游 codegen/daoFor 调用层若仍有问题，归 MA4 代码质量审计（A4.5） |
| arch-gov §残留风险 1 | b2b→pur 跨域写 ErpB2bAsnBizModel 收敛条件（待 pur 提供 createFromAsn） | P2 | deferred |
| arch-gov §残留风险 2 | finance 77 mutation 的 owner doc 背书未逐项核对 | P1 | 进入 MA2/MA3 审计 |
| arch-gov §残留风险 3 | §5.6.3 禁止清单是否增列 mfg→inv/drp→inv/mnt→ast 待业务裁决 | P2 | deferred |
| arch-gov §残留风险 4 | governed path 成本评估需 nop-entropy 平台层 lazy/SPI 解耦 | P2 | deferred（超本项目范围） |
| arch-gov §残留风险 5 | 未覆盖：性能/索引 N+1 | P1 | 进入 MA7 审计 |
| arch-gov §残留风险 5 | 未覆盖：安全/认证 @BizMutation 权限注解 | P1 | 进入 MA6 审计 |
| arch-gov §残留风险 5 | 未覆盖：AMIS view.xml drift | P1 | 进入 MA4 审计 |
| arch-gov §残留风险 5 | 未覆盖：测试覆盖率深度 | P1 | 进入 MA5 审计 |
| hardcoded-status-literal | mfg doc-status 轴/跨域镜像常量按语义轴判定排除 | P2 | deferred |

### 3.3 已知绿色基线（compliance checker，2026-07-24）

| 规则 | 基线 | 说明 |
|------|------|------|
| R1a-d | 0/0/0/17 | dao 直访 |
| R2a/b/c/d | 37/315/1228/28 | daoFor 跨域 |
| R3 | 5 | new Erp*() |
| R4 | 0 | extends RuntimeException |
| R5 | 0 | @Inject private |
| R6 | 2 | @Transactional |
| R7 | 0 | System.currentTimeMillis |
| R8 | 42 | Processor 无 xbiz |

## 4. 维度来源汇总

| 来源 | 维度数 | 说明 |
|------|--------|------|
| A：已有 19 skill | 15 | 可直接复用 |
| B：残留风险与已知盲区 | 14 | 必须补建的新维度 |
| C：ERP 特定风险 | 5 | 保护区域/可定制/多账套/期间/冲销 |
| **合计去重** | ~22 | 部分维度重叠 |
