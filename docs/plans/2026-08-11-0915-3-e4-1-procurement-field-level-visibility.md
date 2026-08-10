# 2026-08-11-0915-3 E4.1 采购保密字段级可见性（双层分工：schema 隐藏 + 代理视图）

> Plan Status: active
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

Status: planned
Targets: 本计划 Decision 节；`field-formatting-patterns.md` §9.7；各域 xmeta/view.xml/report/dashboard（契约面枚举）
Skill: `nop-backend-dev`

- Item Types: `Decision | Explore | Proof`
- Prereqs: P1.2 done（Q1/Q4 冻结）+ E3.1 done（masking 基线）+ E3.2 done（取值豁免前提）

- [ ] `Explore`: 据 §9.7 冻结清单枚举受影响契约面——逐字段 grep 消费方：(a) view.xml grid/form cell 引用；(b) nop-report 模板引用；(c) 各域 dashboard 看板聚合 API 引用；(d) E3.1 @BizLoader（隐藏字段的 loader 须调整/移除）。产出契约面清单（字段 × 消费方 × 调整动作）。
  - Skill: none
- [ ] `Decision (a) 隐藏集 vs 代理集 vs 保持 masking`：逐字段集裁决落层（受 Q1/Q4 约束）：
  - **mfg 成本分解 6 字段**（Q1）：原始字段 `published=false`/`queryable=false` 隐藏 + 代理视图（unitCost/totalCost 透传 + 4 band 映射）—— Q1 (d) 冻结。
  - **供应商价格 pur+md 4 字段**（Q4）：`published=false`/`queryable=false` 隐藏（研发不可见）；授权角色（采购员/管理员）经代理视图或保持 E3.1 masking 消费 —— Phase 1 裁决（代理 vs masking）。
  - **hr 薪酬/PII/contract 金额**：裁决是否追加 schema 级隐藏 vs 保持 E3.1 值 masking（masking 已兜底 API 消费者；schema 隐藏更强但改契约）—— Phase 1 裁决，默认保持 masking（双层冗余，E3.1 §9.4 已述）除非有契约隐藏诉求。
  - **纯 E4.1 字段**（EDI 4 / ApprovalMatrix / RebateTier / SignatureRequest / SocialInsuranceConfig / taxRate / minOrderQuantity）：裁决 published/queryable 落层（可见性范畴，非金额脱敏）。
  - 记录每字段集裁决 + 替代方案 + 残留风险。
  - Skill: none
- [ ] `Decision (b) 档位阈值策略（Q1 R1）`：选定 materialBand/laborBand/overheadBand/subcontractBand 的 high/mid/low 阈值定义。考虑替代方案：(a1) 全局固定阈值（最小可用，本计划采纳）；(a2) 按物料类别分位阈值（successor，防单组件 BOM 反推）。选定 (a1) + 阈值初值 + 理由 + 残留风险（R2 单来源件 unitCost≈purchasePrice 近似还原，登记 successor）。
  - Skill: none
- [ ] `Proof`: 契约面清单完整（无未核对的 §9.7 字段消费方）；xmeta published/queryable 现值实证（§9.7 一致：默认 true，仅 logistics false）。
  - Skill: none

Exit Criteria:

- [ ] 契约面清单产出（字段 × 消费方 × 调整动作）；隐藏集/代理集/保持 masking 三类裁决落定 + 档位阈值策略选定。
- [ ] 决策经独立 plan-audit（契约变更门控，横切关注点 5）。

### Phase 2 - 实现：xmeta 字段级隐藏 + mfg 代理视图 + 消费方调整

Status: planned
Targets: `module-manufacturing/erp-mfg-meta/.../ErpMfgCostRollupLine.xmeta`; `module-master-data/erp-md-meta/.../ErpMdMaterialSku.xmeta`; `module-purchase/erp-pur-meta/.../ErpPurSupplierPriceList.xmeta`; 各域 view.xml/report/dashboard 消费方；E3.1 @BizLoader 调整；`module-manufacturing/erp-mfg-service/.../biz/`（代理视图 BizModel）
Skill: `nop-backend-dev`

- Item Types: `Add | Fix | Proof`
- Prereqs: Phase 1 done（裁决 + 契约面清单 + plan-audit 通过）

- [ ] `Add`: mfg 代理视图——`ErpMfgCostRollupLine` BizModel `@BizLoader(autoCreateField=true)` 新增 `totalCost`/`unitCost`（透传，授权见）+ `materialBand`/`laborBand`/`overheadBand`/`subcontractBand`（档位映射，按 Phase 1 阈值）。
  - Skill: `nop-backend-dev`
- [ ] `Fix`: xmeta 字段级隐藏——按 Phase 1 Decision (a) 隐藏集翻 `published="false" queryable="false"`（mfg 成本 6 原始 + 供应商价 + 纯 E4.1 字段集），delta xmeta（保留层非生成文件）。
  - Skill: `nop-backend-dev`
- [ ] `Fix`: E3.1 @BizLoader 调整——隐藏字段的既有 @BizLoader 移除/迁移到代理视图（隐藏字段无 data fetcher，loader 无目标）；保持 masking 的字段集 loader 不动。
  - Skill: `nop-backend-dev`
- [ ] `Fix`: 消费方调整——按契约面清单调整 view.xml grid/form cell（隐藏字段引用改代理字段或移除）+ report/dashboard 聚合 API 引用。
  - Skill: `nop-backend-dev`
- [ ] `Proof`: `mvn clean install -DskipTests` 增量重新生成 + 类型检查通过；xmllint delta xmeta well-formed。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] xmeta 隐藏集翻 published/queryable + mfg 代理视图 6 字段 + 消费方调整落地；codegen 重新生成 + 类型检查通过。
- [ ] 后续阶段依赖的本地化检查（mfg-meta/md-meta/pur-meta 类型检查）通过。

### Phase 3 - Proof：契约 + 可见性 + E3.2 不变量 + owner doc + 日志

Status: planned
Targets: 后端测试；`docs/design/field-formatting-patterns.md`; `docs/design/roles-and-permissions.md`; `docs/logs/2026/08-11.md`
Skill: `nop-testing`

- Item Types: `Proof | Add`
- Prereqs: Phase 2 done

- [ ] `Proof`（契约）：GraphQL schema 反映隐藏（隐藏字段 absent）+ 代理视图字段 present；消费方无断裂（页面/report/dashboard 不引用 absent 字段）。
  - Skill: `nop-testing`
- [ ] `Proof`（可见性）：代理视图档位映射单元测试（授权角色见 totalCost/unitCost + band；非授权不见原始成本字段）+ 负向断言（隐藏字段 GraphQL 不可达）。
  - Skill: `nop-testing`
- [ ] `Proof`（E3.2 不变量复验，load-bearing）：复跑 `TestErpMfgCostRollupValueExemptionInvariant` + `TestErpInvStandardCostResolverValueExemptionInvariant` 绿——证明 `published=false` 隐藏 `ErpMdMaterialSku.purchasePrice` 不阻断服务端成本卷算取值（DAO 直读架构性豁免）。
  - Skill: `nop-testing`
- [ ] `Add`: owner doc 更新——`field-formatting-patterns.md` §9.4 字段级可见性从 successor 改已落地（双层分工）+ §9.7 各字段 published/queryable 现值更新；`roles-and-permissions.md` §数据权限 E4.1 交叉引用；讨论文档 §裁决记录.Q1/Q4 增 E4.1 落地注记。
  - Skill: none
- [ ] `Add`: `docs/logs/2026/08-11.md` 聚合日志条目（E4.1 字段级隐藏 + 代理视图 + 契约面核对 + E3.2 不变量复验 + 验证状态）。
  - Skill: none

Exit Criteria:

- [ ] 契约 Proof（schema 反映 + 消费方无断裂）+ 可见性 Proof（代理视图 + 负向）+ E3.2 守卫测试绿。
- [ ] owner doc + 日志已更新。

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_013452694ffe24khL1Wb9xfdoy`，fresh-session general 子代理，未起草本计划）— 全 checklist A-L PASS。零信任基线核验全 VERIFIED：E3.1 MaskHelper 存在（`module-common-service/.../MaskHelper.java`）+ 5 域 @BizLoader masking（mfg 6 + md 3 + pur 1 + hr 4 + ct sampled，43 字段对齐 E3.1 closure）/ E3.2 守卫测试存在（`TestErpMfgCostRollupValueExemptionInvariant` + `TestErpInvStandardCostResolverValueExemptionInvariant`）/ xmeta published/queryable 现状默认 true（delta `ErpMfgCostRollupLine.xmeta`/`ErpMdMaterialSku.xmeta`/`ErpPurSupplierPriceList.xmeta` 空 `<props/>` 无 published=false 覆盖；仅 logistics 有）/ mfg 成本 6 字段 orm:1268-1273 / ErpMdMaterialSku.purchasePrice orm:384 / E3.2 plan 文件名引用 / Q1(d)+Q4(c) 冻结裁决与计划一致 / 横切关注点 5 契约变更门控 roadmap L166 一致 / E4.1 Deps(P1.2+E3.1) done。**关键 H/K PASS**：契约/schema 变更 + 成本区域 plan-first 证据齐备（E3.2 不变量固化 + Q1/Q4 裁决 + owner docs + 守卫测试 + 横切关注点 5 独立 plan-audit 门控）；Phase 1 Decision(a) hide-vs-proxy-vs-masking + Decision(b) band 阈值均有替代方案 + R1/R2 残留风险，Phase 2 Prereqs 显式门控「Phase 1 done + plan-audit 通过」。0 blocker / 0 major / 3 minor（信息性）：(m1) Phase 1 Decision(a) 非 mfg 字段集默认偏向「保持 masking」略预设；(m2) 契约面按类别描述 + per-field map deferred 到 Phase 1（正确排序）；(m3) 成本区域 plan-first 经 E3.2 不变量引用而非横切关注点 1 编号。共识达成，转 active。

## Closure Gates

> 完整仓库验证在结束时运行一次。E4.1 为契约/schema 变更（横切关注点 5），Closure Gates 须含 GraphQL schema 反映 + 消费方无断裂 + E3.2 守卫测试。

- [ ] 范围内行为完成（xmeta 隐藏集 + mfg 代理视图 + 消费方调整 + 契约/可见性/E3.2 Proof）
- [ ] 相关文档对齐（field-formatting-patterns §9.4/§9.7 + roles-and-permissions + 讨论文档 §裁决记录 + costing-methods）
- [ ] 已运行验证：`mvn clean install -DskipTests`（增量重新生成 + BUILD SUCCESS）+ `mvn test`（mfg/md/pur/ct/hr 范围全绿 + **E3.2 守卫测试绿**）+ `bash docs/audits/nop-compliance-checker.sh`（零漂移）+ GraphQL schema 反映断言
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录（含契约变更门控独立 plan-audit）
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

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

Status Note: <待执行后填写>

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计>
- Evidence: <待执行后填写>

Follow-up:

- 保密字段读访问审计（见 E4.2 successor）
- 档位边界按类别分位阈值（successor）
- 单来源采购件 unitCost 模糊化（successor）
- Q2/Q3 AI 候选 BOM（manufacturing successor）
- prod enforcement 翻转（successor）
