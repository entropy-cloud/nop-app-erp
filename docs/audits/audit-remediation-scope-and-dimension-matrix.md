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

| 维度 | finance | mfg | hr | assets | pur | sal | qa | crm | prj | cs | ct | b2b | inv | md | mnt | drp | aps | log | notify | Skill |
|------|---------|-----|----|----|------|-----|----|----|----|----|----|-----|-----|-----|-----|-----|-----|-----|--------|-------|
| ORM 模型规范 | ❓S拆 | ❓S拆 | ❓S拆 | ❓ | ❓ | ❓ | ❓ | ❓ | ❓ | ❓ | ❓ | ❓ | ❓ | ⚠️ | ❓ | ❓ | ❓ | ❓ | ❓ | orm-model-audit |
| 跨模块依赖/DAG | ⚠️ | ⚠️ | ❓ | ❓ | ❓ | ❓ | ❓ | ❓ | ❓ | ❓ | ❓ | ❓ | ❓ | ⚠️ | ❓ | ⚠️ | ❓ | ❓ | ❓ | cross-module-dep-audit |
| Nop 平台合规 | ❓ | ❓ | ❓ | ❓ | ❓ | ❓ | ❓ | ❓ | ❓ | ❓ | ❓ | ❓ | ❓ | ❓ | ❓ | ❓ | ❓ | ❓ | ❓ | nop-conformance-audit |
| 架构治理（daoFor/字典/共享内核/guard） | ⚠️F1residual | ⚠️F6✅ | ❓ | ❓ | ❓ | ❓ | ❓ | ❓ | ❓ | ❓ | ❓ | ⚠️F1half | ⚠️ | ⚠️F4✅ | ❓ | ⚠️F7✅ | ❓ | ❓ | ⚠️F5✅ | 参考arch-gov-review |

### 2.2 MA2 — 业务正确性层

| 维度 | finance | mfg | hr | assets | pur | sal | qa | crm | prj | cs | ct | b2b | inv | md | mnt | drp | aps | log | notify | Skill |
|------|---------|-----|----|----|------|-----|----|----|----|----|----|-----|-----|-----|-----|-----|-----|-----|--------|-------|
| 业财端到端 | ❓S拆 | ❓ | N/A | ❓ | ❓ | ❓ | N/A | N/A | ❓ | N/A | ❓ | N/A | ❓ | N/A | ❓ | N/A | N/A | N/A | N/A | 新维度+flow-overview |
| 状态机正确性 | ❓S拆 | ❓S拆 | ❓S拆 | ❓ | ❓ | ❓ | ❓ | ❓ | ❓ | ❓ | ❓ | ❓ | ❓ | N/A | ❓ | N/A | ❓ | ❓ | N/A | state-machine-review |
| 库存核算一致性 | ❓S拆 | ❓ | N/A | N/A | ❓ | ❓ | N/A | N/A | N/A | N/A | N/A | N/A | ❓ | N/A | ❓ | ❓ | N/A | N/A | N/A | 新维度 |
| 预算与承付 | ❓S拆 | N/A | N/A | N/A | ❓ | ❓ | N/A | N/A | ❓ | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | N/A | 新维度 |

### 2.3 MA3 — 文档-实现一致性层

| 维度 | 覆盖范围 | Skill |
|------|---------|-------|
| 设计文档基线 | 全域 docs/design/ | design-doc-audit |
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

| 维度 | 里程碑 | 触发依据 | 覆盖范围 | Skill |
|------|--------|---------|---------|-------|
| **并发与乐观锁** | MA2（A2.17） | use-case-implementation-audit 标记 3 处并发缺口（UC-SAL-10 并发扣批次 / UC-INV-08 乐观锁 / UC-SAL-10 乐观锁）；ERP 核心并发风险（库存扣减/发票核销/期间结账） | S+A 级域 | `open-ended-audit-prompt.md` |
| **多账套/多公司隔离** | MA2（A2.18） | flow-overview §4.4 多套科目表并行；project-vision "产品化通用 ERP" 定位 | S 级域 + finance | `multi-dimensional-audit-prompt.md` |
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

| 来源 | 描述 | 严重性 | 处理 |
|------|------|--------|------|
| arch-gov §残留风险 1 | daoFor `findAllByQuery` Type 1 watch-only residual（113 处站点，机械替换候选 <10） | P2 | watch-only，不在本 roadmap |
| arch-gov §残留风险 1 | daoFor Type 4 设计边界错误（~10-30 处）阻塞 successor | P1 | 进入 MA1 审计 |
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
