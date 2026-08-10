# AI 驱动制造研发的 BOM 处理与采购信息保密

> 日期：2026-08-05
> 状态：开放（前瞻性设计讨论，尚无实施切片）
> 起因：讨论「单 BOM 模型与三视图（EBOM/MBOM/PBOM）」（已记录于 `docs/design/manufacturing/bom-and-routing.md`）后，人工追问：面向未来 AI 驱动的制造研发，最好的处理方式是什么？同时需满足采购信息保密要求。

## 源文件

- `docs/design/manufacturing/bom-and-routing.md`（§单 BOM 模型与三视图）
- `docs/design/manufacturing/simulation-engine.md`（场景-版本对比 diff 范式）
- `docs/design/manufacturing/mrp.md`（MRP 建议拆分：PURCHASE_REQUEST/WORK_ORDER_REQUEST）
- `docs/design/roles-and-permissions.md`（行级 data-auth 灰度 OFF / action-level 声明层 / 后端响应层脱敏标注为 successor）
- `docs/design/field-formatting-patterns.md` §9（F7 敏感字段脱敏——当前为**前端渲染层**，GraphQL 响应仍含明文；后端 `@BizLoader` 脱敏归安全审计 successor）
- `docs/design/finance/costing-methods.md`（STANDARD 成本：`StandardCostResolver` 读 `ErpMfgCostRollupLine.unitCost`）
- `docs/analysis/erp-survey/2026-07-03-0000-wukong-aicrm.md`（AI Tool 系统 + `AiToolPermissionAspect`/`AiToolExecutionRecordingAspect` 可借鉴模式）

## 已确认的立场（不推翻既有决策）

1. **维持单 BOM 权威源**（`ErpMfgBom`），不因 AI 引入第二张平行 BOM；AI 驱动研发补齐「候选→基线」的**晋升管道**而非新实体。
2. **保密是前置项**，不依赖 AI 落地：成本/供应商字段的可见性控制应优先于 AI 就绪项。
3. 采购保密的核心矛盾是「研发要算成本，但不应看供应商与精确价格」→ 可见性分离，而非隐藏整个 BOM。

## 讨论点一：AI 驱动研发——候选层与基线层分离

AI 制造研发的本质 = 高频生成候选 + 快速对比 + 有损评审（human-in-the-loop）。

**建议能力（均属 succeeded 标注，无当前切片）：**

| 能力 | 落位 | 复用/参照 |
|------|------|-----------|
| 候选 BOM | DRAFT 候选并存 + AI 生成 provenance 标记（来源、模型版本、提示词快照、置信度） | 现有 `versionLabel`/`isActive`/`isDefault` 只支持版本并存，缺候选集管理与晋升动作 |
| 批准动作 | 评审通过 → 晋升为正式版本（置 isDefault） | 参照各域审批动作集约定（`domain-design-guidelines.md §16A`） |
| 候选差异对比 | BOM 结构 diff（物料/用量/工序/聚合成本） | 复用 `simulation-engine.md` 版本对比 4 维 diff 范式 + `BomExpander.explode` 扁平展开 |
| AI 读写边界 | AI 只写提案对象，不直接写生产实体；评审动作生效 | wukong-aicrm `AiToolPermissionAspect` + `AiToolExecutionRecordingAspect`（`wukong-aicrm.md:168`） |
| 数据飞轮 | 实际用量/实际工时/生产差异回喂校准 AI 生成的标准值 | `ProductionVarianceCalculator`（实际 vs 标准差异）为天然校准数据源 |

## 讨论点二：采购信息保密——可见性分离

**核心原则：BOM 结构（研发可见）与成本/供应商（保密受控）解耦。**

| 层 | 处理 | 现状 |
|----|------|------|
| 字段级可见性 | `ErpPurSupplierPriceList`、`ErpMdMaterialSku.purchasePrice`、`ErpMfgCostRollupLine` 成本分解、`StandardCostResolver` 读源——meta 层 `published=false`/`queryable=false` + 角色受控，研发角色默认不可见 | F7 当前是**前端渲染层脱敏**（`field-formatting-patterns.md:289`），GraphQL 仍含明文 → 保密要求下激活**后端响应层脱敏**（`@BizLoader`，已标注安全审计 successor） |
| 行级权限 | 供应商/价格数据加行级维度（org/部门） | `data-auth.xml` 已有，灰度默认 OFF，翻转 successor |
| 聚合值代理 | 给研发/AI 的视图 = 结构 + **脱敏聚合成本**（区间/汇总），由采购/成本服务代理计算，明细供应商数据不离开采购/财务域 | 无此能力（需新权限控制点） |
| AI 上下文隔离 | AI 生成方案时上下文不注入保密字段；AI 工具走统一权限切面，只可调用有权限的工具 | 无（依赖 AI 能力项落地） |
| 审计 | 保密字段访问留痕（含读访问） | 现有 `NopSysChangeLog`（`audit,audit-save` tagSet）仅覆盖写路径，读访问审计需扩展 |

## 讨论点三：同构复杂业务问题索引与实现状态核实

「单 BOM 三视图 + 采购保密」并非孤立场景。仓库内存在一批**同构**复杂业务（一物多口径折叠 / 保密可见性冲突 / 隐含决策需文档化）。2026-08-05 逐项核实实现状态（证据见各 owner doc），结论：**设计有决策 ≠ 实现已处理**。分三档：

### 3.1 已实现（主路径可用）

| 场景 | 证据 |
|------|------|
| 库存三层模型（移动单/流水/余额） | `inventory/trace-chain.md` 落地 |
| 预算三通道 | `finance/budget.md:18` 余量公式显式三通道分离已实现 |
| 到岸成本（Landed Cost） | `ErpInvLandedCost`/Processor + `LANDED_COST` 过账 + 红冲闭环（`finance/costing-methods.md:42-66`） |
| 多币种字段层 | 四件套 + P2P/O2C Provider 显式传双金额；其余域单币种 fallback（P1-MA3-039） |
| 成本方法（部分） | MA/FIFO/STANDARD 已实现；**BATCH/个别计价/全月加权/LIFO 为 Non-Goal**（`costing-methods.md:40`） |
| 多套账 | `acctSchemaId` 原生贯穿凭证/余额/成本层（行为深度有限，见多币种注记） |
| 承运商凭证保密 | 加密存储 + 写回型 `published=false queryable=false`——**全仓唯一后端级保密先例** |
| 资产拆分/合并 | 分离实体 + DIRECT 三轴审批（历史折旧重分摊 Deferred） |

### 3.2 已裁决简化 / 灰度默认 OFF（「处理了」需打引号）

| 场景 | 状态 |
|------|------|
| GRNI 暂估 | **documented simplification**：不自动冲回，靠期末试算发现（`finance/posting.md:95`） |
| 三向匹配 | 付款核销二次门控已落地但 config 默认 false（`purchase/three-way-match.md:50-58`） |
| 公司间抵消 | 抵消/配对实体已建；合并范围经 config+组织树承载，内部存货利润开关默认 false |
| 寄售 | 仅 `VMI_CONSUME` 消耗路径；收货即寄售流 Non-Goal，余额手工建 |
| 行级数据权限 | `data-auth.xml` 规则已写但**双层灰度默认 OFF**，翻转 successor |
| action 级权限 | 敏感动作权限点已声明但 **enforcement 默认 OFF**（`enable-action-auth=false`）→ 薪酬/合同/EDI 保密点均未真正生效 |
| 联副产品分摊 | **Non-Goal**（`manufacturing/bom-and-routing.md:175`） |
| 反结账 | kill-switch 简化实现（非审批流，`finance/state-machine.md:195`） |

### 3.3 未处理（设计/讨论层，无实现切片）

- **采购信息保密**（本讨论）：仅记录，无实现；Q1-Q4 未裁决
- **后端响应层脱敏**：successor（`field-formatting-patterns.md:377`）——当前所有「脱敏」经 F12 可见明文
- **多币种全量折算**：其余域单币种 fallback
- **AI 驱动制造研发**：纯前瞻讨论

**关键发现**：业务主链路（进销存+业财+成本）已闭环；而「多口径」「保密」两类多数停在「设计已裁决 + 实现简化或 OFF」，且因权限 enforcement 整体未开启，**除承运商凭据外无任何保密场景运行时强制生效**。本索引即「设计 vs 实现差距」地图。

## 权限 enforcement 说明（声明层 vs 强制执行层）

权限系统分两层，本项目当前只有第一层：

| 层 | 内容 | 现状 |
|----|------|------|
| 声明层 | `action-auth.xml` 菜单/FNPT 权限点、角色→权限映射种子、`data-auth.xml` 行级过滤规则 | 已落地（文件存在） |
| 强制执行层 | 运行时拦截：无权限拒绝（403）、菜单/按钮按角色隐藏、查询自动附加行级过滤 | **默认关闭** |

强制执行层由三个 config 开关控制（`roles-and-permissions.md:163`）：

| 开关 | 默认 | 含义 |
|------|------|------|
| `nop.auth.enable-action-auth` | `false` | 关闭时所有用户可见可操作全部菜单与按钮 |
| `nop.auth.enable-data-auth` + `erp.data-auth.role-row-filter-enabled` | 均 `false` | 关闭时 checker 返回 null，查询不加过滤条件 |
| `nop.auth.skip-check-for-admin` | `true` | 管理员跳过检查（开启 enforcement 后仍全放行） |

**当前实际效果**：任何登录用户可执行任意操作、查看全部数据（含薪酬/合同/供应商价格）。**开启 enforcement** = 翻上述开关为 true，让拦截器真正拒绝；项目列为 successor（翻转需人工批准 + 角色种子 + 灰度计划，`roles-and-permissions.md:75`），因当前无角色种子，直接开启会导致页面/测试大面积权限失败。

## 讨论点四：角色种子与 enforcement 翻转——触发条件与承接方式（2026-08-05 核实）

承接人工追问「当前运行中的 roadmap 是否开启角色种子」「什么情况下开启」「是否需要独立 roadmap」。逐项核实（证据见 `docs/backlog/` 与 `roles-and-permissions.md`）：

### 4.1 现状核实

| 问 | 结论 | 证据 |
|----|------|------|
| 哪个 roadmap 建了权限层 | `audit-remediation-roadmap.md` 建了声明层（R2.7 高危动作 per-action FNPT + 静态 role-resource 种子（仅 4 类高危实体 delta `action-auth.xml`）、R3.4 行级 data-auth 规则 + 灰度 checker、R3.3 SoD），**该 roadmap 已全部关闭（MR6 CLOSED）** | backlog README + audit-remediation-roadmap |
| 当前在跑的 roadmap 在做什么 | 唯一 in-flight 为 `requirement-compliance-roadmap.md`（审计轨）；A3.5（todo）仅覆盖「数据权限/SoD 铺开 successor 复查」——**复查非实现** | requirement-compliance-roadmap.md A3.5 |
| 全量角色种子是否已做 | **未做**：15 角色 × 674 FNPT 全矩阵明确 Deferred，触发条件「RBAC 精细化或合规审计需求」 | `roles-and-permissions.md:104` |

### 4.2 翻转 enforcement 的门控（四件套，缺一不开）

1. **人工批准**——翻转是产品/安全决策而非技术任务，AI 无权自行翻（ask-first 保护区域）。
2. **角色种子完备**——当前仅高危动作子集静态种子；需其余权限点映射裁决补齐。
3. **灰度计划**——顺序：非敏感域/只读面 → action-level → data-auth，每步回归。
4. **技术前提**——当前 200+ E2E spec 在 enforcement 开启下会大面积权限失败，需权限就绪测试账号与断言调整；且 4 实体（Payment/Receipt/Disposal/Salary）浏览器层 xwf 审批路径本就不可达，开启前需定其权限语义。

### 4.3 独立 roadmap 的裁决

**需要独立 roadmap，但「触发后建」，非现在建。** 理由：

- 剩余工作是散落各处的 successor 声明（arm-index 41 处 + owner doc 内嵌），**无集中追踪**（`docs/discussions/2026-08-02-*` 已把「successor 未系统回队」列为问题）。
- 跨 19 域 + 改共享行为（权限强制执行），满足 AGENTS.md 规划触发条件（跨模块/改共享行为/多会话）。
- 具备清晰里程碑：**M1** 权限点映射裁决 + 角色种子完备 + E2E 就绪 → **M2** 灰度（只读/非敏感） → **M3** action-level 翻转 → **M4** data-auth 翻转 + 后端响应层脱敏 → **M5** 采购保密字段级（依赖 Q1-Q4 先裁决）。
- 硬门槛不变：第一步仍是「scope + 决策」里程碑（含人工批准与 Q1-Q4），非写代码。

**结论句**：enforcement 缺的不是代码，而是「一份准备好的独立 roadmap + 一个人工批准动作」。触发即回队。

## 未解决问题（待人工/后续裁决）

1. **成本聚合可见度的粒度**：研发/AI 视图看到「按成本要素的成本区间」「标准成本总额」还是「仅相对高低（高/中/低）」？——影响新权限控制点的字段范围。**✅ RESOLVED（2026-08-09，plan `2026-08-09-1314-3`）**——见 §裁决记录.Q1。
2. **候选 BOM 建模位置**：扩展 `ErpMfgBom.status`（DRAFT 多候选）还是独立 `ErpMfgBomCandidate` 头（含 provenance + 依附正式 BOM）？倾向后者（不污染正式基线行，provenance 字段不宜混入生产实体）——需评审。**归属判定（2026-08-09）**：归 **manufacturing 域 successor**（见 §裁决记录.Q2/Q3）；字段集设计/实施待该 successor。
3. **AI provenance 的标准字段集**：来源类型/模型/提示词快照/置信度的最小集与保留期限。**归属判定（2026-08-09）**：归 **manufacturing 域 successor**（见 §裁决记录.Q2/Q3）；字段集设计/实施待该 successor。
4. **保密与成本滚算的冲突处理**：`CostRollupService` 需读 `purchasePrice`/供应商价以算制造件成本——聚合成本**由服务内部跨域取值**，取值的可见性边界与「研发角色不可见」如何共存（服务端计算不经由研发角色查询）。**✅ RESOLVED（2026-08-09，plan `2026-08-09-1314-3`）**——见 §裁决记录.Q4 + `docs/design/finance/costing-methods.md §成本卷算取值豁免边界`。

## 裁决记录（2026-08-09，plan `2026-08-09-1314-3-procurement-confidentiality-q1q4-adjudication`）

> 来源：`permissions-enforcement-roadmap.md` P1.2。本节为 Q1/Q4 安全/业务裁决 + Q2/Q3 归属判定，**不改任何代码/ORM/成本服务**（纯裁决落盘）；实施分别归 E3.2（取值豁免，ask-first successor）与 E4.1（字段级可见性）与 manufacturing 域 successor。

### Q4 — 成本滚算跨域取值的可见性边界裁决

#### Proof（事实证据：服务端取值角色无关，架构性豁免）

成本卷算/标准成本解析的取值路径**架构性豁免**字段级可见性（meta `published`/`queryable`，E4.1）与行级 data-auth（`nopDataAuthChecker`），二者仅在 **BizModel/GraphQL 边界**强制。证据：

- **`CostRollupService`（`module-manufacturing/erp-mfg-service/.../costing/CostRollupService.java`）**——**非 BizModel 直接 DAO 消费者**：Javadoc `:61` 明示「本类为非 BizModel 服务助手...直接用 `IDaoProvider`」；注入字段仅 `@Inject IDaoProvider daoProvider`（`:69-70`）+ `@Inject BomExpander bomExpander`（`:71-72`），**无 `IContext` / user-context 注入**。采购件基础成本取值 `defaultSkuPurchasePrice(materialId)`（`:294-304`）经 `daoProvider.daoFor(ErpMdMaterialSku.class).findAllByQuery(q)` 直读 `ErpMdMaterialSku.purchasePrice`——**不遍历任何用户角色的查询路径**。
- **`StandardCostResolver`（`module-inventory/erp-inv-service/.../costing/StandardCostResolver.java`）**——**非 BizModel 直接 DAO 消费者**：Javadoc `:37-38` 明示「本类非 BizModel...经 `IDaoProvider` 直接读 mfg-dao 实体」；注入字段仅 `@Inject IDaoProvider daoProvider`（`:48-49`）+ `@Inject IOrmTemplate ormTemplate`（`:51-52`），**无 user-context 注入**。`resolveFromRollup`（`:73-96`）经 `daoProvider` 直查 `ErpMfgCostRollup`/`ErpMfgCostRollupLine.unitCost`，对齐 `costing-methods.md §实现注记：STANDARD 标准成本法` 链路。
- **触发 vs 读取的区分**：`rollupCost` 由用户经 BizModel `IErpMfgBomBiz.rollupCost` **触发**（`CostRollupService.rollup` 入口 `:85`），但**取值**（读 `purchasePrice`/`unitCost`）**不遍历该用户的查询路径**——这是 Q4 裁决的事实基础。Nop 的字段级/行级权限拦截位于 BizModel/GraphQL 层（`@BizQuery`/`@BizMutation`/`@BizLoader` + `nopDataAuthChecker`），DAO 层（`IDaoProvider`/`IOrmTemplate` 直读）不经这些拦截器。

**结论**：「研发角色不可见（E4.1 字段级隐藏 + data-auth）」与「成本可滚算（服务端跨域取值）」在服务端本就共存——服务端取值不经由研发角色的查询路径，故 E4.1 字段级隐藏**不会**阻断 `CostRollupService`/`StandardCostResolver` 的取值。Q4 的真实矛盾不在取值，而在**研发角色通过何种视图消费聚合结果**（直读字段 vs 代理视图）。

#### Decision（Q4 可见性边界方案）

**选定方案：(c) 混合——服务端取值豁免（事实，非配置）+ 研发侧代理视图消费聚合值。**

考虑的替代方案：

| 方案 | 评估 | 取舍 |
|------|------|------|
| (a) 研发角色直读 `unitCost`/成本字段，由 E4.1 字段级隐藏阻断研发 | 服务端取值不冲突（Proof 已证），但研发若需看聚合成本做设计权衡，则 E4.1 必须**逐字段**开窗，字段范围膨胀且易漏；明细 `materialCost`/`purchasePrice` 仍可能经聚合反推供应商价 | 粒度难控，字段集维护成本高 |
| (b) 研发角色经代理视图/E3.x 聚合值代理消费 | 明细供应商数据不离开采购/财务域（符合 §讨论点二「聚合值代理」原则）；但若服务端取值本身视为需豁免审批，则 (b) 单独不澄清取值边界的合法性 | 未回答取值侧 |
| **(c) 混合：服务端取值豁免（事实）+ 研发侧代理视图** ✅ | 取值侧：服务端 DAO 直读架构性豁免 E4.1/data-auth（Proof 已证，**无需配置豁免**，仅需裁决确认知）；消费侧：研发/AI 经代理视图拿脱敏聚合值，明细成本要素不离开采购/财务域。两端解耦，与 Q1 粒度裁决直接挂钩（代理视图输出 = Q1 选定粒度） | 需 E3.2 确认取值豁免边界（本裁决即 E3.2 冻结输入） |

**残留风险**：
- (R1) 服务端取值豁免依赖「`CostRollupService`/`StandardCostResolver` 始终为非 BizModel 直 DAO 消费者」这一**架构不变量**——若后续重构引入 `IContext`/user-scoped DAO 查询，豁免前提被破坏。E3.2 实施时须在代码注释/契约文档固化此不变量。
- (R2) 代理视图本身是新权限控制点（E4.x successor），其「聚合算法」须确保无法经多次查询反推明细（如对单组件 BOM 卷算可还原材料价）——属代理视图实现的脱敏强度问题，归 E4.x。
- (R3) 本裁决**不实施**代理视图代码（Non-Goal）；E3.2（取值豁免 plan-first）为独立 ask-first successor，依赖本 Q4 裁决作为冻结输入。

**对 E3.2 的冻结输入**：E3.2（`CostRollupService` 取值豁免）的 plan-first 证据 = 本 §裁决记录.Q4 Proof（架构性豁免事实）+ Decision (c)。E3.2 实施时**不改 `CostRollupService`/`StandardCostResolver` 业务逻辑**，仅在代理视图/字段级可见性层落 (c) 方案。

### Q1 — 研发/AI 视图成本聚合可见粒度裁决

#### Decision（Q1 粒度方案）

**选定方案：(d) 组合——标准成本总额（`unitCost`/`totalCost`，精确）+ 按成本要素的相对高低档位（高/中/低，离散）。**

考虑的替代方案：

| 方案 | 评估 | 取舍 |
|------|------|------|
| (a) 按成本要素的**精确**成本区间（材料/人工/制造费用/委外各区间） | 设计权衡最有用（知成本在哪），但精确要素值（尤其 `materialCost`）可经 BOM 用量反推供应商采购价 → 保密强度不足 | 泄密风险高 |
| (b) 仅标准成本总额（`unitCost`/`totalCost`） | 保密强度高（单值难反推明细）；但研发做候选 BOM diff 时只知总高低不知改善方向，设计价值低 | 过度脱敏 |
| (c) 仅相对高低（高/中/低离散档位） | 保密最强；但候选 BOM 对比时无量化差值，难以排序取舍 | 过度脱敏 |
| **(d) 组合：总额精确 + 要素档位离散** ✅ | 总额（`unitCost`）给候选 BOM 量化对比与排序；要素档位（材料/人工/制造/委外各 high/mid/low）给设计改善方向直觉；要素档位离散化使单组件 BOM 也无法反推精确供应商价（档位合并多供应商价格分布）。覆盖 §讨论点二「区间/汇总」二义性 | 档位边界定义归 E4.x |

**对 E4.1 字段范围与新权限控制点的影响**（冻结输入）：

| 字段/视图 | 研发/AI 可见性 | 实现层 |
|-----------|---------------|--------|
| `ErpMfgCostRollupLine.unitCost`/`totalCost`（标准成本总额） | ✅ 可见（经代理视图直读） | 代理视图 = E4.x 新权限控制点 |
| `ErpMfgCostRollupLine.materialCost`/`laborCost`/`overheadCost`/`subcontractCost`（精确要素值） | ❌ 默认不可见；经**档位映射**暴露为 high/mid/low | 代理视图脱敏算法（E4.x） |
| `ErpMdMaterialSku.purchasePrice`、`ErpPurSupplierPriceList`（供应商/采购明细） | ❌ 不可见（P1.1 供应商价格面，本就保密） | E4.1 字段级 `published=false`/`queryable=false` |
| 新权限控制点字段集 | 代理视图：`materialBand`/`laborBand`/`overheadBand`/`subcontractBand`（high/mid/low）+ `totalCost`/`unitCost`（透传） | E4.1 meta + 代理视图 BizModel |

**残留风险**：
- (R1) 档位边界（high/mid/low 阈值）的定义策略（全局固定阈值 vs 按物料类别分位阈值）未定——归 E4.x，须避免「单组件 BOM + 档位」反推。
- (R2) `unitCost` 精确值对**单来源采购件**仍可近似还原采购价（`unitCost ≈ purchasePrice`）——E4.x 代理视图须对此场景追加模糊化（如四舍五入到档位粒度），归代理视图脱敏强度。
- (R3) 本裁决**不实施**（Non-Goal）；E4.1 字段级可见性为独立 successor，依赖本 Q1 裁决作为冻结输入。

> **E4.1 落地注记**（2026-08-11，plan `2026-08-11-0915-3`）：Q1 (d) + Q4 (c) 冻结输入已落地。实施裁决：
> - **mfg 4 要素成本**（materialCost/laborCost/overheadCost/subcontractCost）：xmeta `published=false`/`queryable=false` 隐藏 + `@BizLoader(autoCreateField=true)` 代理视图暴露 `materialBand`/`laborBand`/`overheadBand`/`subcontractBand`（high/mid/low 档位，`CostBandClassifier` 全局固定阈值 low<100/mid 100-1000/high≥1000，R1 全局阈值采纳，按类别分位为 successor）。Q1 (d)「精确要素值默认不可见，经档位映射暴露」满足。
> - **mfg totalCost/unitCost + md/pur 供应商价 + hr/ct 金额**：保持 E3.1 masking（Phase 1 Decision (a) 裁决：隐藏+passthrough 代理 = masking 功能等价，无保密增益）。Q1 (d)「标准成本总额 ✅ 研发可见（经代理视图直读）」满足——masking loader 即代理视图控制点（授权管理员/财务员见明文）。
> - **纯 E4.1 配置字段**（EDI/ApprovalMatrix/RebateTier/SignatureRequest/SocialInsuranceConfig/taxRate/minOrderQuantity）：保持 visible（Phase 1 Decision (a)#5：配置/操作性字段，schema 隐藏破坏管理 UI，无保密增益）。
> - **Q4 (c) 满足**：服务端取值豁免（`CostRollupService` 经 DAO 写入要素成本；`StandardCostResolver` 经 DAO 读 unitCost）经 E3.2 守卫测试复跑绿证明。R2（unitCost≈purchasePrice 近似还原）登记为 successor（两字段均保持 masking，R2 为 masking 层既有属性非 E4.1 新增）。
> - **平台机制实证**：`ObjMetaToGraphQLDefinition` 跳过 `published=false` 字段，`@BizLoader(autoCreateField=true)` 经 `GraphQLObjectDefinition.mergeField` bypass objMeta 检查重新引入代理字段；既有 masking loader（`autoCreate=false`）须移除否则 `objMeta.hasProp=true` 重新引入隐藏字段。

### Q2/Q3 — 候选 BOM 建模位置 / AI provenance 字段集归属判定

#### Decision（归属域）

**判定：Q2（候选 BOM 建模位置）/ Q3（AI provenance 字段集）归属 manufacturing 域 successor（仅归属判定，不设计字段集、不实施）。**

考虑的替代归属：

| 候选归属 | 评估 | 取舍 |
|----------|------|------|
| crm | crm 无 BOM/制造语义；候选 BOM 与 crm 选配无天然耦合 | 不匹配 |
| logistics | logistics 管物流/运费，与 BOM 结构/provenance 无关 | 不匹配 |
| **manufacturing** ✅ | manufacturing 拥有 `ErpMfgBom` 权威源（§讨论点一已维持单 BOM 决策）；候选层「依附正式 BOM」语义天然落此处；provenance 不宜混入生产实体（§讨论点一倾向独立 candidate 头） | 匹配，符合 §讨论点一既有立场 |

**归属范围**：manufacturing successor 负责（触发条件 = AI 驱动研发候选 BOM 管道启动）：
- Q2：候选 BOM 建模（独立 `ErpMfgBomCandidate` 头 vs 扩展 `ErpMfgBom.status`）的字段集设计与实施。
- Q3：AI provenance 标准字段集（来源类型/模型/提示词快照/置信度最小集 + 保留期限）。

**残留风险**：若 AI 研发管道最终跨域（如 crm 选配驱动候选），归属可能 revisited——当前判定基于「BOM 权威源在 manufacturing」，AI 触发源跨域不改变数据归属。

**本计划 Non-Goal**：不实施 Q2/Q3，不设计字段集（显式移出范围为 successor，非降级）。

## 结论

- 单 BOM 决策不因 AI 而变；AI 就绪项 = 候选晋升管道 + diff + provenance + 工具权限切面。
- 保密前置项 = 成本/供应商字段字段级可见性 + 后端响应层脱敏。
- **权限 enforcement 是保密类全部前置项的前提**：开关未开启时，一切字段级/行级/操作级权限声明均不生效。
- §讨论点三索引即「设计 vs 实现差距」地图：保密类场景除承运商凭据外无强制生效项。
- §讨论点四：enforcement 缺的不是代码，而是「准备好的独立 roadmap + 人工批准动作」；触发条件（RBAC 精细化或合规审计需求）当前未满足，roadmap 触发后建。
- 本期无实施切片；待裁决 Q1-Q4 与触发条件满足后，分别归 manufacturing/security 域 successor。