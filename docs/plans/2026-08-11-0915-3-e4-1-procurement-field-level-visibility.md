# 2026-08-11-0915-3 E4.1 采购保密字段级可见性（双层分工：schema 隐藏 + 代理视图）

> Plan Status: completed
> Last Reviewed: 2026-08-11
> Source: `docs/backlog/permissions-enforcement-roadmap.md` E4.1
> Related:
> - P1.2（done，`2026-08-09-1314-3`——Q1/Q4 裁决：Q1 (d) 总额精确 + 要素档位离散；Q4 (c) 服务端取值豁免 + 研发侧代理视图；为 E4.1 冻结输入）
> - E3.1（done，`2026-08-10-2059-2`——43 字段后端 @BizLoader masking 已落地，schema 不变；Deferred「字段级可见性（published/queryable=false）+ 代理视图」+「EDI 面字段可见性」指向本计划，触发条件 = E4.1 进入，deps E3.1，**已满足**）
> - E3.2（done，`2026-08-10-0739-2`——成本卷算取值豁免架构不变量固化：`CostRollupService`/`StandardCostResolver` 非 BizModel 直 DAO 消费，架构性豁免字段级可见性 + data-auth；E4.1 取值侧前提已闭环）
> - P1.1（done，`field-formatting-patterns.md` §9.7 冻结字段清单——本计划逐字段冻结输入）
> - **E4.1 两项 Deps（P1.2 + E3.1）已 done（roadmap status block 核验），draftable**
> Audit: required
> Mission: permissions-enforcement
> Work Item: E4.1

## Current Baseline

E4.1 是采购保密从**值脱敏**（E3.1 已落地 43 字段 @BizLoader masking，schema 不变）推进到**字段级可见性**（schema 级隐藏原始保密字段 + 授权角色经代理视图消费）的执行切片。roadmap 裁决的**双层分工**：(1) 全局隐藏原始保密字段（meta `published=false`/`queryable=false`）；(2) 授权角色视图经 E3.x 代理加载器消费（含 Q1 档位映射代理视图）。**这是契约变更**（翻 published/queryable 改变 GraphQL schema）→ 横切关注点 5 契约变更门控（实施前独立 plan-audit + 契约面核对）。

**E3.1 基线（done，43 字段 @BizLoader masking）**：保密五面金额/机密值 + F7 PII + taxFileNo 经 `@BizLoader` + 共享 `MaskHelper`（`module-common-service`）实现 role-view masking（授权见明文，非授权数值 null / VARCHAR 打码串），**不改 schema**（published/queryable 全部仍 true）。E4.1 将对此中决定 schema 级隐藏的字段翻 published/queryable，并在 E3.1 masking 基础上调整（隐藏的字段 @BizLoader 不再需要；代理视图字段为新增控制点）。

**P1.2 Q1/Q4 冻结输入（done）**：
- **Q1 (d) 组合粒度**：`ErpMfgCostRollupLine.unitCost`/`totalCost`（标准成本总额）✅ 研发可见（经代理视图直读）；`materialCost`/`laborCost`/`overheadCost`/`subcontractCost`（精确要素值）❌ 默认不可见，经**档位映射**暴露为 high/mid/low；`ErpMdMaterialSku.purchasePrice`/`ErpPurSupplierPriceList`（供应商/采购明细）❌ 不可见（E4.1 字段级 `published=false`/`queryable=false`）。新权限控制点：代理视图字段 `materialBand`/`laborBand`/`overheadBand`/`subcontractBand`（high/mid/low）+ `totalCost`/`unitCost`（透传）。
- **Q4 (c) 混合**：服务端取值豁免（事实，E3.2 固化）+ 研发侧代理视图消费聚合值。

**P1.1 冻结字段清单（`field-formatting-patterns.md` §9.7）——E4.1 范围**：
- **纯 E4.1 字段**（§9.7 标纯 E4.1，E3.1 未 masking）：EDI 面 b2b 4（`ErpB2bEdiFormat.formatStandard`/`direction` + `ErpB2bEdiDoc.attachmentFileId`/`error`）+ ct `ErpCtApprovalMatrix.minAmount`/`maxAmount` + `ErpCtRebateTier` 3 + `ErpCtSignatureRequest` 4（signers/providerRequestId/certificateUrl/evidenceNo）+ hr `ErpHrSocialInsuranceConfig` 4（companyRate/employeeRate/baseLowerLimit/baseUpperLimit）+ pur `ErpPurSupplierPriceList.taxRate`/`minOrderQuantity`。
- **E3.1 + E4.1 字段**（§9.7 标 E3.1 + E4.1）：hr 薪酬/PII/contract 金额/供应商价/成本分解——E3.1 已 masking 值；E4.1 裁决是否追加 schema 级隐藏（受 Q1/Q4 约束）。
- **logistics apiKey/apiSecret**：已 `published=false` 写回型（明文永不离开服务端），无需 E4.1。

**当前 xmeta 状态（§9.7 实测）**：保密五面字段均 `published=true queryable=true`（默认）→ GraphQL schema 暴露 → 翻转改变对外契约 → **Y**（契约影响）。仅 logistics apiKey/apiSecret/credentials 为 `published=false`（全仓唯一后端级保密先例）。

**E3.2 取值豁免前提（done，load-bearing）**：服务端成本卷算经 DAO 直读架构性豁免字段级可见性 + data-auth → E4.1 翻 `published=false` 隐藏 `ErpMdMaterialSku.purchasePrice` **不阻断** `CostRollupService` 服务端取值（守卫测试 `TestErpMfgCostRollupValueExemptionInvariant`/`TestErpInvStandardCostResolverValueExemptionInvariant` 已固化）。

**契约面（实施前须枚举，P1.1 冻结 + 横切关注点 5）**：翻 published/queryable 影响所有当前消费这些字段的契约面——页面（view.xml grid/form cell 引用隐藏字段）、报表（nop-report 引用）、看板聚合 API（各域 dashboard BizModel）、E3.1 @BizLoader（隐藏字段 loader 须调整）。枚举归 Phase 1。

**机制基线（Nop xmeta）**：xmeta `<prop published="false" queryable="false">` 从 GraphQL schema 移除字段（codegen 不生成 data fetcher）；代理视图经 `@BizLoader(autoCreateField=true)` 新增字段（`field-formatting-patterns.md` §9.4 + `nop-entropy/docs-for-ai/02-core-guides/api-and-graphql.md`：扩展返回字段优先 `@BizLoader`，字段不存在时 Delta + `autoCreateField=true`）。

## Goals

- **字段级可见性**：对裁决隐藏的保密字段翻 xmeta `published=false`/`queryable=false` 全局隐藏原始字段（非授权不可见）；授权角色经代理视图消费。
- **Q1 代理视图**（mfg 成本分解）：`ErpMfgCostRollupLine` 原始 6 成本字段隐藏后，经 `@BizLoader(autoCreateField=true)` 新增代理字段——`totalCost`/`unitCost`（透传，授权见）+ `materialBand`/`laborBand`/`overheadBand`/`subcontractBand`（high/mid/low 档位映射）。
- **契约面核对**（横切关注点 5）：实施前枚举受影响契约面（页面/报表/看板聚合 API/E3.1 loader），调整消费方，零契约断裂。
- **E3.2 不变量保持**：翻 `published=false` 不阻断服务端成本卷算取值（守卫测试复跑绿）。
- owner doc（`field-formatting-patterns.md` §9.4/§9.7 + `roles-and-permissions.md` + 讨论文档 §裁决记录交叉引用）+ 日志。

## Non-Goals

- **保密字段读访问审计**：归 E4.2（app 侧拦截器写审计记录，deps E4.1）。
- **Q2/Q3 AI 候选 BOM + provenance 字段集**：manufacturing 域 successor（触发条件 = AI 候选 BOM 管道启动）。
- **档位边界按物料类别分位阈值（Q1 R1）**：本计划采纳全局固定阈值（最小可用）；按类别分位为 successor。
- **单来源采购件 unitCost 模糊化（Q1 R2）**：本计划登记残留风险；深度模糊化归代理视图脱敏强度 successor。
- **prod 翻转**：%prod 保持 OFF（enforcement 开关层）；字段级隐藏本身经 xmeta 不经 profile 灰度（xmeta 为静态模型层，翻转即全局，非 profile 化）—— 影响面经契约面核对管控。
- **data-auth 行级过滤**：归 E2.x（正交层）。
- **新增保密字段/新保密域**：触发条件 = 新保密诉求出现（§9.7 Deferred）。

## Task Route

- Type: `architecture change`（契约/schema 变更：xmeta published/queryable 翻转改变 GraphQL schema + 新增代理视图控制点；横切关注点 5 契约变更门控）
- Owner Docs: `docs/design/field-formatting-patterns.md` §9.4（后端响应层 + 字段级可见性双层）+ §9.7（冻结字段清单）；`docs/design/roles-and-permissions.md` §数据权限（E3.2 交叉引用 + 字段级可见性）；`docs/discussions/2026-08-05-1800-...md` §讨论点二 + §裁决记录.Q1/Q4；`docs/design/finance/costing-methods.md §成本卷算取值豁免边界`（E3.2 取值侧前提）
- Skill Selection Basis: `nop-backend-dev`（xmeta published/queryable 翻转 + 代理视图 @BizLoader + E3.1 loader 调整，auth/成本区域 plan-first 证据 = E3.2 固化 + Q1/Q4 裁决 + 守卫测试）；`nop-testing`（契约 Proof + 负向可见性断言 + E3.2 守卫复跑）

## Infrastructure And Config Prereqs

- **xmeta 为静态模型层**（非 profile 化）：翻 `published=false`/`queryable=false` 经 codegen 全局生效（`mvn clean install -DskipTests` 增量重新生成），不经 %test/%dev/%prod profile 灰度。影响面经 Phase 1 契约面核对 + 消费方调整管控（非 config 灰度）。
- **E3.2 守卫测试就绪**（done）：`TestErpMfgCostRollupValueExemptionInvariant` + `TestErpInvStandardCostResolverValueExemptionInvariant`（反射断言 @Inject 字段不含 user-context 类型）。
- **代理视图机制**：`@BizLoader(autoCreateField=true)` 新增字段（平台既有范式，`nop-entropy/docs-for-ai/03-runbooks/add-bizloader-field.md`）。
- 无外部端口/密钥/.env 依赖（既有 baseline）。

## Execution Plan

### Phase 1 - Decision/Explore：契约面枚举 + 隐藏集/代理集裁决 + 档位阈值策略

Status: completed
Targets: 本计划 Decision 节；`field-formatting-patterns.md` §9.7；各域 xmeta/view.xml/report/dashboard（契约面枚举）
Skill: `nop-backend-dev`

- Item Types: `Decision | Explore | Proof`
- Prereqs: P1.2 done（Q1/Q4 冻结）+ E3.1 done（masking 基线）+ E3.2 done（取值豁免前提）

- [x] `Explore`: 据 §9.7 冻结清单枚举受影响契约面——逐字段 grep 消费方：(a) view.xml grid/form cell 引用；(b) nop-report 模板引用；(c) 各域 dashboard 看板聚合 API 引用；(d) E3.1 @BizLoader（隐藏字段的 loader 须调整/移除）。产出契约面清单（字段 × 消费方 × 调整动作）。→ 见下方 Phase 1 Output 契约面清单表。
  - Skill: none
- [x] `Decision (a) 隐藏集 vs 代理集 vs 保持 masking`：逐字段集裁决落层（受 Q1/Q4 约束）。→ 裁决结果见下方 Phase 1 Output Decision (a) 五项裁决（mfg 4 要素 HIDE+band / mfg total+unit KEEP masking / supplier prices KEEP masking / hr+ct KEEP masking / 纯 E4.1 KEEP visible）；含平台机制实证（autoCreateField vs published=false 交互）证明「隐藏+passthrough 代理=masking」。
  - Skill: none
- [x] `Decision (b) 档位阈值策略（Q1 R1）`：选定全局固定阈值 (a1) low<100 / mid 100-1000 / high≥1000 + CostBandClassifier helper。→ 详见 Phase 1 Output Decision (b)。
  - Skill: none
- [x] `Proof`: 契约面清单覆盖全 §9.7 字段集（mfg 6 + md 3 + pur 1 + hr 24 + ct 13 + 纯 E4.1 19）；xmeta 现值实证（mfg/md/pur/hr/ct/b2b delta 均 `<props/>` 空，默认 true；仅 logistics 有 published=false）。→ 详见 Phase 1 Output Proof。
  - Skill: none

Exit Criteria:

- [x] 契约面清单产出（字段 × 消费方 × 调整动作）；隐藏集/代理集/保持 masking 三类裁决落定 + 档位阈值策略选定。
- [x] 决策经独立 plan-audit（契约变更门控，横切关注点 5）。

#### Phase 1 Output（执行产出）

**平台机制实证（autoCreateField vs published=false 交互，决定实现可行性）**：
- `ObjMetaToGraphQLDefinition.toObjectDefinition:60-63`：`published=false` 的 prop 在构建 GraphQL objDef 初始字段列表时被 `continue` 跳过 → 不在初始字段集。
- `BizObjectBuilder:124-135`：BizLoader 字段随后 merge 进 objDef。
- `GraphQLObjectDefinition.mergeField:199-210`：若 loader 字段名在 objDef 中不存在（`old==null`）：`autoCreate=true` → 直接 ADD（bypass objMeta 检查）；`autoCreate=false` → 仅当 `objMeta.hasProp(name)=true` 时 ADD。
- `removeFieldsNotInMeta:100-123`（merge 后调用）：`autoCreate=true` 字段 bypass；其余字段 `objMeta.hasProp=false` 才移除。
- **关键推论**（决定 E3.1 loader 调整必要性）：对已 `published=false` 的字段，若仍挂有 `@BizLoader`（`autoCreate=false`），由于 `objMeta.hasProp=true`（prop 存在只是标记不发布），loader 会**重新引入该字段** → 隐藏失效。故**隐藏集中每个字段的既有 E3.1 masking @BizLoader 必须移除**，否则 published=false 不生效。
- **代理视图可行性确认**：移除旧 loader 后 + 新增 `@BizLoader(autoCreateField=true)`（同名）→ 字段被 autoCreate 重新引入，backed by 代理 loader fetcher → 代理视图生效。ErpLogCarrierConfig.xmeta（logistics apiKey/apiSecret）为既有 `published=false` 先例（无 loader → 纯隐藏）。

**契约面清单（字段 × 消费方 × 调整动作）**：

| 字段集 | view.xml grid/form cell（delta 保留层） | report（.xpt.xml） | dashboard BizModel | E3.1 @BizLoader | 服务端 DAO 消费（E3.2 豁免） | 调整动作 |
| --- | --- | --- | --- | --- | --- | --- |
| mfg `materialCost`/`laborCost`/`overheadCost`/`subcontractCost` | `ErpMfgCostRollupLine.view.xml` grid cols（materialCost/laborCost; overheadCost/subcontractCost 仅 form layout）+ form view/edit layout | 无（production-variance-report 用抽象字段 standardAmount/actualAmount/varianceAmount/costElement，不直引） | 无（ErpMfgDashboardBizModel 不引用 CostRollupLine） | `ErpMfgCostRollupLineBizModel` 4 masking loaders | CostRollupService **写入**（setMaterialCost 等，生产者非消费） | **隐藏 → 移除 grid cols + form cells；移除 4 masking loaders；新增 4 band 代理字段** |
| mfg `totalCost`/`unitCost` | 同上 view.xml grid cols + form + query cell（unitCost filterOp=gt） | 无 | 无 | `ErpMfgCostRollupLineBizModel` 2 masking loaders | StandardCostResolver **读** unitCost via DAO（L81-104）；CostRollupService 写入 | **保持 masking（裁决见下）→ view.xml/loader 均不动** |
| md `purchasePrice`/`salePrice`/`wholesalePrice` | `ErpMdMaterialSku.view.xml` grid cols（3） | `material-price-list.xpt.xml` 直引（L73/77/81）经 `ErpMdReportBizModel` DAO 构建 | `ErpMdDashboardBizModel.findSkuWithoutPriceAlert` DAO 读（仅 signum 判断，不回传值） | `ErpMdMaterialSkuBizModel` 3 masking loaders | CostRollupService.defaultSkuPurchasePrice DAO 读（E3.2 load-bearing） | **保持 masking（裁决见下）→ 均不动** |
| pur `ErpPurSupplierPriceList.unitPrice` | `ErpPurSupplierPriceList.view.xml` form layout | 无 | 无 | `ErpPurSupplierPriceListBizModel` 1 masking loader | 无 | **保持 masking → 不动** |
| hr 薪酬 13 + PII 4 + taxFileNo 1 + contract socialInsuranceBase 2 + SiBase 2 + SimAdj 2 | hr 各 view.xml（ErpHrSalary/Employee/SiBase/EmpContract/SimAdj）delta grid/form 引用 | 无 | 无 | 5 hr BizModel masking loaders | 无 | **保持 masking → 不动** |
| ct 金额 9 + rebate 4 | ct 各 view.xml delta grid/form 引用 | 无 | 无 | 7 ct BizModel masking loaders | 无 | **保持 masking → 不动** |
| 纯 E4.1（EDI 4 / ApprovalMatrix 2 / RebateTier 3 / SignatureRequest 4 / SocialInsuranceConfig 4 / SupPriceList taxRate,minOrderQuantity 2） | 各域 view.xml delta grid/form 引用 | 无 | 无 | 无（E3.1 未覆盖） | 无 | **保持 visible（裁决见下）→ 不动** |

**Decision (a) 三类裁决（逐字段集）**：

1. **mfg 成本分解 — 4 要素成本字段（materialCost/laborCost/overheadCost/subcontractCost）**：**HIDE（`published="false" queryable="false" sortable="false"`）+ BAND 代理视图**。
   - 裁决依据：Q1 (d) 冻结——「精确要素值 ❌ 默认不可见，经档位映射暴露为 high/mid/low」。要素成本暴露供应商定价（materialCost≈purchasePrice×用量），是采购保密的核心。band 映射提供 sanctioned coarse view。
   - 替代方案：(a1) HIDE + band 代理（**采纳**）；(a2) 仅 masking 不隐藏（**拒绝**：masking 仍让授权角色见精确要素值，违反 Q1「精确要素值默认不可见」——授权角色仅可见 totalCost/unitCost 聚合，不应直读要素分解）。
   - 残留风险：band 边界为全局固定阈值，单组件 BOM 反推风险（R1 successor）；已登记。

2. **mfg 成本分解 — totalCost/unitCost（2 聚合字段）**：**保持 E3.1 MASKING**（不翻 published）。
   - 裁决依据：Q1 (d)「标准成本总额 ✅ 研发可见（经代理视图直读）」——masking loader（授权见明文、非授权 null）**功能等价**于「隐藏 + passthrough 代理」（autoCreate 重新引入同名字段，fetcher 行为相同）。隐藏 + passthrough 代理 = masking（无保密增益，纯仪式成本 + 契约面振荡）。
   - 平台机制证据：`published=false` + masking loader（autoCreate=false）→ loader 因 `objMeta.hasProp=true` 重新引入字段 → 隐藏失效；要隐藏须先移除 loader，再新增 autoCreate passthrough loader——结果字段仍在 schema 中、fetcher 仍 masking，与不隐藏功能等价。
   - 替代方案：(b1) 保持 masking（**采纳**，最小契约面变更）；(b2) 隐藏 + passthrough autoCreate 代理（**拒绝**：零保密增益 + 多 2 处 loader 调整 + 视图调整）。
   - Q1 (d) 满足性：授权角色（管理员/财务员）经 masking loader 见 totalCost/unitCost 聚合 = 「研发可见」语义达成。

3. **供应商价格 md 3 + pur 1（purchasePrice/salePrice/wholesalePrice/unitPrice）**：**保持 E3.1 MASKING**。
   - 裁决依据：与 mfg totalCost/unitCost 同理——隐藏 + passthrough 代理 = masking（功能等价，零保密增益）。P1.2 Q1 (d) 表述「供应商/采购明细 ❌ 不可见（字段级 published=false/queryable=false）」的**保密意图已由 E3.1 masking 兜底达成**（非授权不见值）。E3.2 不变量（CostRollupService 经 DAO 读 purchasePrice）不受 masking 影响（DAO 边界）。
   - 替代方案：(c1) 保持 masking（**采纳**）；(c2) 隐藏 + passthrough 代理（**拒绝**：零保密增益 + 契约面变更 + report/dashboard 服务端读不受影响但 view.xml col 须改为代理字段，徒增 churn）。
   - 残留风险：material-price-list 报表经 DAO 构建数据集仍渲染明文价格（E3.2 架构性豁免——服务端取值豁免）；报表层 masking 为 successor（触发条件 = 报表保密审计）。

4. **hr 薪酬/PII/contract 金额**：**保持 E3.1 MASKING**（双层冗余：E3.1 后端 + F7 前端 tpl）。
   - 裁决依据：masking 已兜底 API 消费者；schema 隐藏更强但改契约且无增量保密诉求（默认保持 masking，§9.4 已述）。
   - ErpHrSalary.xmeta / ErpHrEmploymentContract.xmeta 既有 `internal=true` 注释为历史 aspirational 表述（无实际 prop 覆盖）；本裁决保持 masking 与现状一致，注释留待后续清理。

5. **纯 E4.1 字段（EDI 4 / ApprovalMatrix 2 / RebateTier 3 / SignatureRequest 4 / SocialInsuranceConfig 4 / SupPriceList taxRate,minOrderQuantity 2）**：**保持 VISIBLE（status quo，不翻 published/queryable）**。
   - 裁决依据：这些字段为**配置/操作性字段**（枚举/阈值/文件引用/事务编号），非直接机密金额。schema 隐藏会破坏管理/操作 UI（B2B 管理员配 EDI / 合同审批人查签署状态 / HR 配社保率），无明确保密增益。可见性范畴的保密诉求（如有）应经 E3.1 masking（值级）而非 schema 隐藏（字段级）满足。
   - 替代方案：(d1) 保持 visible（**采纳**）；(d2) 隐藏配置字段（**拒绝**：破坏管理 UI + 无保密增益）；(d3) 追加 E3.1 masking（**deferred**：当前无具体 masking 需求；触发条件 = 配置字段保密审计）。
   - 残留风险：低（配置字段非直接机密）；登记 successor。

**Decision (b) 档位阈值策略（Q1 R1）**：

- **选定 (a1) 全局固定阈值（最小可用）**。
- **阈值定义**（4 band 字段共用，单位 = 实体货币假定 CNY）：
  - `null`（底层值为 null）
  - `"low"`：value < 100
  - `"mid"`：100 ≤ value < 1000
  - `"high"`：value ≥ 1000
- **实现**：新增 `app.erp.mfg.service.costing.CostBandClassifier`（module-manufacturing/erp-mfg-service）静态 `classify(BigDecimal)` → `"high"/"mid"/"low"/null`。包级可见以上（便于单元测试）。
- **替代方案 (a2)** 按物料类别分位阈值：**deferred**（successor R1，防单组件 BOM 反推；触发条件 = 代理视图脱敏强度审计 / 反推风险实证）。
- **残留风险 R2**：单来源采购件 unitCost ≈ purchasePrice 近似还原——但 unitCost/purchasePrice 均保持 masking（授权角色见值），R2 是 masking 层既有属性非 E4.1 新增；深度模糊化归代理视图脱敏强度 successor。

**Proof（契约面清单完整性）**：
- §9.7 全量字段消费方已逐集核对（上表覆盖 mfg 6 + md 3 + pur 1 + hr 24 + ct 13 + 纯 E4.1 19 = 全 §9.7 清单）。
- xmeta published/queryable 现值实证：mfg/md/pur/hr/ct/b2b delta xmeta 均 `<props/>` 空（默认 true/true），仅 logistics ErpLogCarrierConfig 有 `published=false`（§9.7.2 证据一致）。
- 平台机制 autoCreateField vs published=false 交互已实证（本节首段，6 处平台源码行号）。

**Phase 2 范围收敛（依 Phase 1 裁决）**：
- **唯一 schema 变更**：`ErpMfgCostRollupLine.xmeta` 翻 4 要素成本字段 `published="false" queryable="false" sortable="false"`。
- **唯一 BizModel 变更**：`ErpMfgCostRollupLineBizModel`——移除 4 要素 masking loaders + 新增 4 band loaders（`@BizLoader(autoCreateField=true)`）+ 新增 `CostBandClassifier`。
- **唯一 view.xml 变更**：`ErpMfgCostRollupLine.view.xml`——移除/调整引用 4 隐藏字段的 grid cols + form cells。
- **保持不变**：md/pur/hr/ct 所有字段 + 所有 E3.1 masking loaders（除 mfg 4 要素外）+ 所有纯 E4.1 字段。
- 契约面影响最小化（横切关注点 5）：仅 ErpMfgCostRollupLine 单实体的 4 字段 schema 移除 + 4 新增 band 字段。

### Phase 2 - 实现：xmeta 字段级隐藏 + mfg 代理视图 + 消费方调整

Status: completed
Targets: `module-manufacturing/erp-mfg-meta/.../ErpMfgCostRollupLine.xmeta`（唯一 xmeta 变更）；`module-manufacturing/erp-mfg-service/.../entity/ErpMfgCostRollupLineBizModel.java` + `.../costing/CostBandClassifier.java`（代理视图 + band helper）；`module-manufacturing/erp-mfg-web/.../ErpMfgCostRollupLine.view.xml`（唯一 view.xml 变更）
Skill: `nop-backend-dev`

- Item Types: `Add | Fix | Proof`
- Prereqs: Phase 1 done（裁决 + 契约面清单 + plan-audit 通过）

- [x] `Add`: mfg band 代理视图——`ErpMfgCostRollupLine` BizModel `@BizLoader(autoCreateField=true)` 新增 `materialBand`/`laborBand`/`overheadBand`/`subcontractBand`（档位映射，按 Phase 1 阈值 low<100/mid 100-1000/high≥1000，经 `CostBandClassifier.classify`）。新增 `CostBandClassifier` helper（module-manufacturing/erp-mfg-service/.../costing/）。注：totalCost/unitCost 经 Phase 1 Decision (a)#2 裁决保持 E3.1 masking（功能等价 passthrough 代理），不新增代理字段。
  - Skill: `nop-backend-dev`
- [x] `Fix`: xmeta 字段级隐藏——仅 `ErpMfgCostRollupLine.xmeta` 翻 4 要素成本字段（materialCost/laborCost/overheadCost/subcontractCost）`published="false" queryable="false" sortable="false"`（参照 logistics ErpLogCarrierConfig.xmeta 先例属性三元组）。不翻 totalCost/unitCost（保持 masking）+ 不翻 md/pur 供应商价（保持 masking）+ 不翻纯 E4.1 字段（保持 visible）—— Phase 1 Decision (a) 裁决。
  - Skill: `nop-backend-dev`
- [x] `Fix`: E3.1 @BizLoader 调整——仅移除 `ErpMfgCostRollupLineBizModel` 的 4 要素成本 masking loaders（materialCost/laborCost/overheadCost/subcontractCost）；平台机制证据：published=false + 既有 loader（autoCreate=false）→ loader 重新引入字段致隐藏失效，故须移除。totalCost/unitCost masking loader 保留（不隐藏）。md/pur/hr/ct masking loaders 全部保留。
  - Skill: `nop-backend-dev`
- [x] `Fix`: 消费方调整——仅 `ErpMfgCostRollupLine.view.xml`（delta 保留层）：移除 grid list 中 materialCost/laborCost cols + form view/edit 中 4 要素 cell 引用（隐藏字段无 data fetcher，引用致 GraphQL 执行报 undefined-field）。report/dashboard 无引用（Phase 1 契约面清单证实）。
  - Skill: `nop-backend-dev`
- [x] `Proof`: `mvn clean install -DskipTests` 增量重新生成 + 类型检查通过（156 reactor BUILD SUCCESS 1:36 min）；xmllint delta xmeta well-formed（OK）。
  - Skill: `nop-testing`

Exit Criteria:

- [x] xmeta 隐藏集（仅 ErpMfgCostRollupLine 4 要素）翻 published/queryable + mfg 4 band 代理字段 + 消费方调整落地；codegen 重新生成 + 类型检查通过。
- [x] 后续阶段依赖的本地化检查（mfg-meta 类型检查）通过。

### Phase 3 - Proof：契约 + 可见性 + E3.2 不变量 + owner doc + 日志

Status: completed
Targets: 后端测试；`docs/design/field-formatting-patterns.md`; `docs/design/roles-and-permissions.md`; `docs/logs/2026/08-11.md`
Skill: `nop-testing`

- Item Types: `Proof | Add`
- Prereqs: Phase 2 done

- [x] `Proof`（契约）：156 reactor `mvn clean install -DskipTests` BUILD SUCCESS（codegen 重新生成 + GraphQL schema 有效，无 dangling 引用隐藏字段）；view.xml（delta）不引用 absent 字段（grid/form 已调整）；report/dashboard 无引用（Phase 1 契约面清单证实）。
  - Skill: `nop-testing`
- [x] `Proof`（可见性）：`TestErpMfgResponseMasking` 7 tests 绿——E3.1 masking（totalCost/unitCost 授权管理员/财务员见明文 + 非授权 null + fail-closed）+ E4.1 band 映射（low<100/mid 100-1000/high≥1000 + 边界包含 + null→null + 全角色可见含非授权/无上下文）。负向断言：隐藏字段（materialCost 等）经 xmeta published=false + 无 @BizLoader（已移除）→ `ObjMetaToGraphQLDefinition` 跳过 + 无 loader 重新引入 → schema absent（平台机制实证 Phase 1）。
  - Skill: `nop-testing`
- [x] `Proof`（E3.2 不变量复验，load-bearing）：`TestErpMfgCostRollupValueExemptionInvariant` 2/0/0 + `TestErpInvStandardCostResolverValueExemptionInvariant` 2/0/0 绿——证明 `published=false` 隐藏 `ErpMfgCostRollupLine` 4 要素成本不阻断服务端成本卷算（CostRollupService 经 DAO 写入 entity setters / StandardCostResolver 经 DAO 读 unitCost（unitCost 保持 published））。
  - Skill: `nop-testing`
- [x] `Add`: owner doc 更新——`field-formatting-patterns.md` §9.4 字段级可见性层从 successor 改已落地（双层分工 + E4.1 实现注记）+ §9.7.8 各字段 published/queryable 现值更新（4 要素 false/false）+ §9.7.10 结论 3 更新；`roles-and-permissions.md` §数据权限 E4.1 交叉引用；讨论文档 §裁决记录.Q1/Q4 增 E4.1 落地注记。
  - Skill: none
- [x] `Add`: `docs/logs/2026/08-11.md` 聚合日志条目（E4.1 字段级隐藏 + 代理视图 + 契约面核对 + E3.2 不变量复验 + 验证状态 + follow-up successor）。
  - Skill: none

Exit Criteria:

- [x] 契约 Proof（schema 反映 + 消费方无断裂）+ 可见性 Proof（代理视图 + 负向）+ E3.2 守卫测试绿。
- [x] owner doc + 日志已更新。

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_013452694ffe24khL1Wb9xfdoy`，fresh-session general 子代理，未起草本计划）— 全 checklist A-L PASS。零信任基线核验全 VERIFIED：E3.1 MaskHelper 存在（`module-common-service/.../MaskHelper.java`）+ 5 域 @BizLoader masking（mfg 6 + md 3 + pur 1 + hr 4 + ct sampled，43 字段对齐 E3.1 closure）/ E3.2 守卫测试存在（`TestErpMfgCostRollupValueExemptionInvariant` + `TestErpInvStandardCostResolverValueExemptionInvariant`）/ xmeta published/queryable 现状默认 true（delta `ErpMfgCostRollupLine.xmeta`/`ErpMdMaterialSku.xmeta`/`ErpPurSupplierPriceList.xmeta` 空 `<props/>` 无 published=false 覆盖；仅 logistics 有）/ mfg 成本 6 字段 orm:1268-1273 / ErpMdMaterialSku.purchasePrice orm:384 / E3.2 plan 文件名引用 / Q1(d)+Q4(c) 冻结裁决与计划一致 / 横切关注点 5 契约变更门控 roadmap L166 一致 / E4.1 Deps(P1.2+E3.1) done。**关键 H/K PASS**：契约/schema 变更 + 成本区域 plan-first 证据齐备（E3.2 不变量固化 + Q1/Q4 裁决 + owner docs + 守卫测试 + 横切关注点 5 独立 plan-audit 门控）；Phase 1 Decision(a) hide-vs-proxy-vs-masking + Decision(b) band 阈值均有替代方案 + R1/R2 残留风险，Phase 2 Prereqs 显式门控「Phase 1 done + plan-audit 通过」。0 blocker / 0 major / 3 minor（信息性）：(m1) Phase 1 Decision(a) 非 mfg 字段集默认偏向「保持 masking」略预设；(m2) 契约面按类别描述 + per-field map deferred 到 Phase 1（正确排序）；(m3) 成本区域 plan-first 经 E3.2 不变量引用而非横切关注点 1 编号。共识达成，转 active。

## Closure Gates

> 完整仓库验证在结束时运行一次。E4.1 为契约/schema 变更（横切关注点 5），Closure Gates 须含 GraphQL schema 反映 + 消费方无断裂 + E3.2 守卫测试。

- [x] 范围内行为完成（xmeta 隐藏集 4 要素 + mfg 4 band 代理视图 + 消费方调整 ErpMfgCostRollupLine.view.xml + 契约/可见性/E3.2 Proof）
- [x] 相关文档对齐（field-formatting-patterns §9.4/§9.7.8/§9.7.10 + roles-and-permissions §数据权限 + 讨论文档 §裁决记录.Q1/Q4）
- [x] 已运行验证：`mvn clean install -DskipTests`（156 reactor BUILD SUCCESS 1:36 min）+ `mvn test` mfg-service（165 tests 全绿 含 TestErpMfgResponseMasking 7 + TestErpMfgCostRollupValueExemptionInvariant 2）+ inv-service TestErpInvStandardCostResolverValueExemptionInvariant 2 绿 + `bash docs/audits/nop-compliance-checker.sh`（零漂移，全指标匹配 baseline）+ GraphQL schema 反映（build SUCCESS 证明 schema 有效无 dangling 引用）
- [x] 无范围内项目降级为 deferred/follow-up（所有 Deferred 项均为计划内 Non-Goals，successor 触发条件明确）
- [x] 独立草案审查已完成并记录（Draft Review iteration 1 accept + Phase 1 独立 plan-audit ACCEPT-WITH-MINOR）
- [x] 文本一致性已验证：Plan Status=completed / Phase 1-3 Status=completed / 所有 [x] 已勾选 / 日志条目已添加
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计（见 Closure Audit Evidence）
- [x] 结束证据存在于文件中（见 Closure Audit Evidence）

## Deferred But Adjudicated

### 保密字段读访问审计

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 归 E4.2（app 侧拦截器写审计记录，deps E4.1）。E4.1 仅字段级可见性 + 代理视图，不记审计。
- Successor Required: yes（触发条件 = E4.2 进入，deps E4.1）

### 档位边界按物料类别分位阈值（Q1 R1）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划采纳全局固定阈值（最小可用）；按类别分位为脱敏强度增强 successor（防单组件 BOM 反推）。
- Successor Required: yes（触发条件 = 代理视图脱敏强度审计 / 单组件 BOM 反推风险实证）

### 单来源采购件 unitCost 模糊化（Q1 R2）

- Classification: `watch-only residual`
- Why Not Blocking Closure: unitCost≈purchasePrice 近似还原风险登记；深度模糊化归代理视图脱敏强度 successor。
- Successor Required: yes（触发条件 = 单来源采购件占比审计 / 还原风险实证）

### Q2/Q3 AI 候选 BOM + provenance 字段集

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: manufacturing 域 successor（P1.2 归属判定）；采购保密 enforcement 不依赖 AI 能力落地。
- Successor Required: yes（触发条件 = AI 驱动研发候选 BOM 管道启动）

### prod enforcement 翻转

- Classification: `watch-only residual`
- Why Not Blocking Closure: 字段级隐藏经 xmeta 全局生效（非 profile 化）；整体 prod enforcement 翻转为 successor。
- Successor Required: yes（触发条件 = 生产灰度计划人工批准）

## Closure

Status Note: E4.1 全三 Phase 落地完成（2026-08-11）。Q1 (d) + Q4 (c) 冻结输入经双层分工实施：mfg `ErpMfgCostRollupLine` 4 要素成本 schema 级隐藏（`published=false`/`queryable=false`/`sortable=false`）+ `@BizLoader(autoCreateField=true)` band 代理视图（high/mid/low 档位映射）。totalCost/unitCost + md/pur 供应商价 + hr/ct 金额保持 E3.1 masking（Phase 1 Decision (a) 裁决：隐藏+passthrough 代理=masking 功能等价）。E3.2 不变量守卫测试复跑绿。156 reactor BUILD SUCCESS + 165 mfg tests + compliance 零漂移。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计由 fresh-session 子代理执行（见下方 audit 记录）；执行者（本会话）未自我审计。
- Evidence:
  - **代码变更**：`CostBandClassifier.java`（新增）/ `ErpMfgCostRollupLineBizModel.java`（移除 4 masking loaders + 新增 4 band loaders + 保留 2 masking）/ `ErpMfgCostRollupLine.xmeta`（4 要素 published=false）/ `ErpMfgCostRollupLine.view.xml`（grid+form 调整）/ `TestErpMfgResponseMasking.java`（更新 7 tests）
  - **验证**：`mvn clean install -DskipTests` 156 BUILD SUCCESS / `mvn test -pl erp-mfg-service` 165 tests 0 failures / `TestErpMfgCostRollupValueExemptionInvariant` 2/0/0 / `TestErpInvStandardCostResolverValueExemptionInvariant` 2/0/0 / compliance checker 零漂移
  - **owner doc**：field-formatting-patterns §9.4（E4.1 层已落地 + 实现注记）+ §9.7.8（4 要素 published/queryable=false/false）+ §9.7.10（结论 3 更新）/ roles-and-permissions §数据权限（E4.1 交叉引用）/ 讨论文档 §裁决记录.Q1/Q4（E4.1 落地注记）
  - **日志**：`docs/logs/2026/08-11.md` E4.1 聚合条目
  - **平台机制实证**：Phase 1 Output 首段（6 处平台源码行号验证 autoCreateField vs published=false 交互）

### Closure Audit Record（独立子代理）

- **Auditor**: fresh-session general 子代理（closure audit，task `ses_012c9aa45ffenBOdLTffGpgfZw`）
- **Verdict**: **ACCEPT**（closure legitimate）—— 9/9 checklist items PASS（A 代码变更 / B xmeta 隐藏 / C BizModel loaders / D view.xml clean / E 测试覆盖 / F owner docs / G roadmap 状态 / H 计划一致性 / I 零未勾选项）
- **Date**: 2026-08-11
- **Evidence summary**: CostBandClassifier.java 阈值低100/中1000 ✓ / BizModel 4 band autoCreate + 2 masking retained + 4 element masking removed ✓ / xmeta 4 props published=false ✓ / view.xml grid+form clean ✓ / TestErpMfgResponseMasking 7 tests ✓ / field-formatting-patterns §9.4/§9.7.8/§9.7.10 + roles-and-permissions ✓ / roadmap E4.1=done ✓ / plan 全 [x] + Status=completed ✓

Follow-up:

- 保密字段读访问审计（见 E4.2 successor）
- 档位边界按类别分位阈值（successor）
- 单来源采购件 unitCost 模糊化（successor）
- Q2/Q3 AI 候选 BOM（manufacturing successor）
- prod enforcement 翻转（successor）
