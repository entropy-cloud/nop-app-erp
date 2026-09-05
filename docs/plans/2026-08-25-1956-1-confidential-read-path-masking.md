---
status: active
mission: permissions-enforcement
work-item: R2-E-stack-2
group: "2026-08-25-1956"
verify: [test, compliance]
---

# 2026-08-25-1956-1-confidential-read-path-masking 保密面聚合/报表/直读 @BizQuery 路径脱敏与披露审计补齐

## Current Baseline

- **来源发现**：roadmap `docs/backlog/permissions-enforcement-roadmap.md` §Deep Audit Record「E-stack — Deep Audit Findings R2」第 2 项（P1，未勾选）：E3.1/E4.1/E4.2 保密面在报表数据集与自定义 @BizQuery 读取路径整面未脱敏未审计。
- **E3.1 masking 仅落在实体 `@BizLoader` 面**：5 域 15 BizModel 43 字段（hr/ct/mfg/md/pur）。三个读取路径完全绕过 MaskHelper，返回保密字段原始值（2026-08-25 活仓核验，行号以活仓为准）：
  - **hr 薪酬面**：`module-hr/erp-hr-service/src/main/java/app/erp/hr/service/report/ErpHrReportBizModel.java` — `payrollSimulationComparisonData` @BizQuery（:196）与 `buildPayrollSimulationComparisonDataset`（:282-324）从 `ErpHrSalarySimulationItemAdjustment` 聚合，原始值直出 `originalAmount`/`adjustedAmount`（:305-306）、派生 `difference` 明细行（:307）与部门小计行（:319）；报表页 `module-hr/erp-hr-web/src/main/resources/_vfs/erp/hr/pages/report/payroll-simulation-comparison.page.yaml` renderHtml 全链路消费。既有测试 `TestErpHrReportRendering` `:188-190`/`:201` 断言 originalAmount/adjustedAmount/difference 与部门小计明文值（CTX = 无用户上下文 `ServiceContextImpl` :43——fail-closed masking 落地后将见 null，须随本计划适配）。
  - **md 供应商价格面**：`module-master-data/erp-md-service/src/main/java/app/erp/md/service/report/ErpMdReportBizModel.java` — `materialPriceListData` @BizQuery（:176）与 `buildMaterialPriceListDataset`（:195-224）从 `ErpMdMaterial` × 默认 `ErpMdMaterialSku` 合成，`purchasePrice`/`salePrice`/`wholesalePrice`/`retailPrice` 原始值直出（:216-219）；既有测试 `TestErpMdReportRendering` 断言物料编码与渲染非空，且 `:80-83` 断言四档价格明文值（CTX = 无用户上下文 `ServiceContextImpl` :38——fail-closed masking 落地后四断言将见 null，须随本计划适配为 masking 语义）。
  - **mfg 成本面**：`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/entity/ErpMfgCostRollupBizModel.java` — `findLatestFirmedStandardCost` @BizQuery（:40-）返回最近 FIRMED 滚算单位成本原始值；同实体行级 loader 已 mask（`ErpMfgCostRollupLineBizModel.java:45-50`，`COST_ROLES = {管理员, 财务员}`）。活仓核验：该 @BizQuery **无任何服务端调用方**——inv `StandardCostResolver` 并不经它取值，而是走自有 DAO 直读路径（`StandardCostResolver.java:37-46` 架构注释 + `resolveFromRollup` :81-104 复制同款查询逻辑，E3.2 架性豁免）；无前端调用方却 GraphQL 暴露。
- **当前唯一防护 = derived permission deny-by-default 的偶然性**：`Erp*Report:*` 查询与该 @BizQuery 无 roles 种子；E1.x closure 仅枚举 mutations，负向测试零覆盖此读取面。
- **既定脱敏模式与角色基线**（复用对象）：`module-common-service/src/main/java/app/erp/common/service/MaskHelper.java` 数值字段审计重载 `maskDecimal(value, authorizedRoles, entity, fieldName)`（:69-77，授权明文 → `MaskAuditRecorder.recordDisclosureIfEnabled`，非授权返 null，fail-closed）；面角色基线 = md `PRICE_ROLES = {采购员, 管理员}`（`ErpMdMaterialSkuBizModel.java:483`）、mfg `COST_ROLES = {管理员, 财务员}`（`ErpMfgCostRollupLineBizModel.java:39`）、hr 薪酬面 `SALARY_MASK_ROLES = {薪酬审批人}`（`ErpHrSocialInsuranceBaseBizModel.java:24` 范式）。E4.2 config-gate `erp.audit.field-read.enabled` %test=ON。
- **审计上下文载体先例**：`MaskAuditRecorder.recordDisclosure(entity, fieldName, role)` 经 `IOrmEntity.orm_entityName()`/objId 提取审计键（`MaskAuditRecorder.java:110-150`）；三个站点数据行均从 ORM 实体构造，源实体可作审计载体。
- **E3.2 豁免不可破坏**：inv `StandardCostResolver` 服务端计算须继续取原始值；不变量守卫 `TestErpMfgCostRollupValueExemptionInvariant` + `TestErpInvStandardCostResolverValueExemptionInvariant` 现绿，须保持。
- **owner doc 缺口**：`docs/design/field-formatting-patterns.md` §9.7（:415-583）仅字段级七元组，无「读取面」维度——E4.1 closure 声称「按 P1.1 枚举受影响契约面/报表/看板聚合 API」但产物缺位（R2 判定该步骤虚假闭合）。
- **联动风险（本计划排位第 1 的原因）**：R2 明示——后续种子修复（R1-E1-1，本批计划 2）将 `ErpMd*:query` 族权限授予业务角色时会静默打开 md 原始价格报表路径；本计划必须先行落地。
- Task Route: Type = `implementation-only change`；Owner Docs = `docs/design/field-formatting-patterns.md` §9.4/§9.7、`docs/design/roles-and-permissions.md` §保密字段读访问审计、`docs/design/finance/costing-methods.md`（E3.2 豁免语义）。

## Goals

- 三个读取站点全部经 MaskHelper 数值审计重载出值：授权角色见明文并写 E4.2 披露审计记录，非授权角色见 null（数值零泄漏），无用户上下文 fail-closed。
- E3.2 服务端取值豁免保持：inv `StandardCostResolver` 计算路径继续取原始 firmed 成本，不变量守卫测试保持绿。
- §9.7 新增「读取面」维度：登记三个站点 + 立法规则（新报表/数据集/自定义 @BizQuery 触及 §9.7 保密面时必须登记读取面并接 MaskHelper）；roles-and-permissions.md E4.2 注记同步（如计数/覆盖面表述受影响）。
- 三站点正负双向 Proof 落地（授权明文 + 非授权 null + 审计写入），受影响模块测试零回归。

## Non-Goals

- 不新增报表查询的 FNPT 权限点或 roles 种子（derived permission deny-by-default 外层姿态不变；本计划补内层 masking，不改外层授权面）。
- 不处理 SoD 守卫失效（R2-E-stack-1）、负向 E2E 白名单 9 例（R1-E2）、EDI 面 successor 与 webhookSecret（R1-E4）——各自独立工作项。
- 不脱敏非保密报表数据集（partner-list 等不在 §9.7 保密面的数据集）。
- 不改 ORM/xmeta、不改平台代码、不改既有 `@BizLoader` masking 面。

## Phase 1 — 设计裁决：插桩点、审计载体与 E3.2 豁免保径

Skill: nop-backend-dev
Targets: `ErpHrReportBizModel.java`、`ErpMdReportBizModel.java`、`ErpMfgCostRollupBizModel.java`（设计结论记入本文件）

- Item Types: `Decision`

- [x] Decision: 三站点 masking 插桩点 = 数据集构造层（行装配处经 MaskHelper 审计重载）+ @BizQuery 边界，而非仅报表渲染层。理由：`payrollSimulationComparisonData`/`materialPriceListData` 作为 @BizQuery 是独立 GraphQL 面，仅渲染层脱敏时查询面仍泄露原始值；替代方案（仅 renderHtml 前脱敏）被否。
- [x] Decision: mfg `findLatestFirmedStandardCost` 就地 masking——活仓核验该 @BizQuery 零服务端调用方（inv `StandardCostResolver` 经自有 DAO 直读路径取原始值，E3.2 架性豁免，不经此方法），故无需双径拆分/消费方切换：IBiz 接口方法签名与 GraphQL 面保持不变，@BizQuery 返回值直接经 `MaskHelper.maskDecimal(raw, COST_ROLES, line, "unitCost")` 出值。裁决同时立法：服务端原始取值需求一律走 DAO 直读（resolver 既有范式），禁止改调此 GraphQL 暴露查询。替代方案（@BizQuery 内按调用方判断 / 为 resolver 新开内部 IBiz 原始方法）被否——前者判定不可靠且掩盖边界，后者无现实消费方支撑属投机设计。
- [x] Decision: 审计载体与字段名语义——明细行以源 ORM 实体（hr=`ErpHrSalarySimulationItemAdjustment`，md=`ErpMdMaterialSku`）+ 保密字段名（originalAmount/adjustedAmount/difference；purchasePrice/salePrice/wholesalePrice/retailPrice）；hr 派生 `difference` 与部门小计聚合行按同面角色 mask，审计 fieldName 以明细 `difference` 记（聚合行不重复记审计，仅 mask）。部门小计行差异值 = 已 mask 差异的聚合口径（非授权见 null）在裁决中定型；源实体缺席的行（md 无默认 SKU）无披露行为——价格列保持 null 直通，不进审计重载（避免 entity=null 的伪披露记录）。
- [x] Decision: hr 报表面角色集对齐——薪酬模拟对比属薪酬面，授权角色 = `{薪酬审批人}`（与 `ErpHrSalarySimulationItemAdjustment` 实体面 E3.1 角色一致；若该实体面已有 loader masking 角色集则直接复用其字面）。

## Phase 2 — 三站点 masking + E4.2 披露审计落地

Skill: nop-backend-dev
Targets: `module-hr/erp-hr-service/.../report/ErpHrReportBizModel.java`、`module-master-data/erp-md-service/.../report/ErpMdReportBizModel.java`、`module-manufacturing/erp-mfg-service/.../entity/ErpMfgCostRollupBizModel.java`

- Item Types: `Fix | Add`

- [x] Fix: hr `buildPayrollSimulationComparisonDataset` 明细行三金额列 + 部门小计行 difference 经 `MaskHelper.maskDecimal(value, 薪酬面角色集, adj, fieldName)` 出值（Phase 1 裁决口径）。
- [x] Fix: md `buildMaterialPriceListDataset` 四价格列经 `MaskHelper.maskDecimal(value, PRICE_ROLES 对齐 ErpMdMaterialSkuBizModel:483, sku, fieldName)` 出值。
- [x] Fix: mfg `findLatestFirmedStandardCost` 按 Phase 1 裁决就地 masking——@BizQuery 返回 `MaskHelper.maskDecimal(raw, COST_ROLES, line, "unitCost")`；resolver 自有 DAO 路径零改动（E3.2 豁免径保持）。
- [x] Add: 各域 BizModel 常量对齐（角色集字面与实体面同源，不新造角色字面）。

## Phase 3 — 正负双向 Proof + owner-doc 读取面维度 + 回归

Skill: nop-testing
Targets: `module-hr/erp-hr-service/src/test/`、`module-master-data/erp-md-service/src/test/`、`module-manufacturing/erp-mfg-service/src/test/`、`module-inventory/erp-inv-service/src/test/`、`docs/design/field-formatting-patterns.md`、`docs/design/roles-and-permissions.md`

- Item Types: `Proof | Fix`

- [x] Proof: 三站点单测（范式 = `TestErpMfgResponseMasking` + `TestMaskAuditRecorder`）——授权角色明文 + 审计记录写入（%test config ON）；非授权角色 null；无用户上下文 fail-closed null。
- [x] Proof: E3.2 豁免保径——`TestErpMfgCostRollupValueExemptionInvariant` + `TestErpInvStandardCostResolverValueExemptionInvariant` 复跑绿；inv resolver 取值路径新增/复跑一条原始值断言（豁免未因 masking 落地破坏）。
- [x] Fix: `field-formatting-patterns.md` §9.7 新增「读取面」小节：登记三站点（文件+入口+字段）+ 立法规则（新读取面触及 §9.7 面必须登记并接 MaskHelper，closure 审计按此核对）；§9.4 E4.2 注记与 roles-and-permissions.md §保密字段读访问审计中读取面覆盖表述同步。
- [x] Proof: 受影响模块 `mvn test`（hr/md/mfg/inv + common-service）全绿；既有 masking 测试（5 域）零回归；两渲染测试（`TestErpMdReportRendering:80-83`、`TestErpHrReportRendering:188-201`）按 masking 语义适配——数据集明文断言改在授权角色用户上下文运行（兼作正向 Proof），并补无上下文 fail-closed null 断言；适配归因登记于本文件。

## Execution Notes（执行期裁决与归因登记）

- **测试适配归因（Phase 3 item 4）**：`TestErpMdReportRendering.testMaterialPriceListDataset`（原 :80-83 四档价格明文断言）与 `TestErpHrReportRendering.testPayrollSimulationComparisonDataset`/`...DeptSubtotal`（原 :188-190/:201 明文断言）按 masking 语义适配——明文断言改在授权角色上下文（hr=薪酬审批人 / md=采购员，与实体面角色同源）运行，兼作正向 Proof；各补无上下文 fail-closed null 断言。归因：E3.1 读取面 masking 落地后 CTX（`ServiceContextImpl` 无用户上下文）fail-closed 见 null，原明文断言必然失败（plan Current Baseline 预判成立）。
- **执行期发现的潜伏缺陷（已修）**：`ErpMfgCostRollupBizModel` 原声明 `@Inject IDaoProvider daoProvider;` 字段与父类 `CrudBizModel` 的 `private IDaoProvider daoProvider` + `setDaoProvider` setter **字段遮蔽**——IoC 经 setter 注入父类私有字段，子类遮蔽字段恒 null。因 `findLatestFirmedStandardCost` 此前零调用方，该缺陷从未被触发；本计划首个测试（`TestErpMfgCostRollupReadPathMasking` 经 IGraphQLEngine 真实 GraphQL 面）暴露 NPE。修复 = 删除遮蔽字段，改用父类访问器 `daoProvider().daoFor(...)`（同 `ErpCtRebateAgreementBizModel` 既有范式）。
- **mfg 站点测试路径裁决**：经 `IGraphQLEngine`（`ErpMfgCostRollup__findLatestFirmedStandardCost` RPC）而非直接 `@Inject IErpMfgCostRollupBiz` 接口调用——后者经 `BizProxyInvocationHandler` 分发到非托管实例（字段未注入）；GraphQL 路径同时更忠实于「该 @BizQuery 无前端调用方但 GraphQL 暴露」的读取面语义。
- **审计断言机制**：三站点测试经 public `MaskAuditRecorder.init()`（`@PostConstruct` 入口）替换静态 instance 为 fake recorder + 捕获型 `IAuditService`，config 经 `AppConfig.getConfigProvider()` 直设 ON（不依赖 profile 解析）；`@AfterEach` 恢复原 instance 与 config 值。范式与 `TestMaskAuditRecorder`（common-service）一致。
- **验证基线（2026-08-25）**：受影响 7 模块 `mvn test` 全绿（hr 240 / md 160 / mfg 292 / inv 232 / common-service 22 / ct 168 / pur 334 = 1448 tests，0 failures 0 errors；含 5 域既有 masking 测试 + E3.2 双守卫零回归）；全仓 `mvn clean install -DskipTests` BUILD SUCCESS。

## Draft Review Record

- dispatch review #review-2026-08-25-201158-mission-driver-2026-08-25-1956-1-confidential-read-path-masking-1-7c4a1e9b to opencode-reviewer-2026-08-25-201158
- 2026-08-25：iteration 1，共识 accept #review-2026-08-25-201158-mission-driver-2026-08-25-1956-1-confidential-read-path-masking-1-7c4a1e9b
- review 就地修正 3 处 Major 事实基线：mfg @BizQuery 零服务端调用方/resolver 走自有 DAO 直读、`TestErpMdReportRendering:80-83` 实断言四档价格、`TestErpHrReportRendering:188-201` 同须适配；Phase 1/2/3 对应条目已同步。

## Verification

- pass test 2026-08-26-0208-verify basisHash=bf4b49840985bbe284c7a081ed5d641e5cd2e65c147ddf19d4fba133b753bcdf exit=0
- pass compliance 2026-08-26-0208-verify basisHash=bf4b49840985bbe284c7a081ed5d641e5cd2e65c147ddf19d4fba133b753bcdf exit=0

## Closure

- dispatch audit #audit-2026-09-05-123532-mission-driver-2026-08-25-1956-1-confidential-read-path-masking-1-50075771 to ses_auditor_2026-09-05-123532 models={exec:zhipuai/glm-5.3-flash,aud:zhipuai/glm-5.3-flash}
- accepted #audit-2026-09-05-123532-mission-driver-2026-08-25-1956-1-confidential-read-path-masking-1-50075771：独立结束审计通过——三站点读取面 masking 均已在活仓核验落地（hr `ErpHrReportBizModel` SALARY_MASK_ROLES/maskDecimal :318-320、md `ErpMdReportBizModel` PRICE_ROLES/maskPrice :242、mfg `ErpMfgCostRollupBizModel` COST_ROLES/maskDecimal :70 且无 daoProvider 字段遮蔽），三站点测试与 E3.2 双守卫 + `TestErpInvResolverRawValueAfterReadPathMasking` 在仓，owner doc §9.7.11 读取面登记与 E4.2 注记同步，`docs/logs/2026/08-25.md` 已登记；验证 = `plan-check.mjs --strict` 全绿（12/12 checked）+ frontmatter verify 键 `test`/`compliance` 均 pass exit=0。
