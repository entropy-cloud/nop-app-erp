# 2026-08-10-2059-2 E3.1 后端响应层脱敏控制点（@BizLoader，保密五面 + F7 PII）

> Plan Status: completed
> Last Reviewed: 2026-08-10
> Source: `docs/backlog/permissions-enforcement-roadmap.md` E3.1
> Related:
> - P1.1（done，敏感字段冻结清单 `docs/design/field-formatting-patterns.md` §9.7——本计划逐字段冻结输入）
> - E1.2（done，action enforcement 闭环——E3.1 不依赖 action 层，但同处 enforcement 大轨道）
> - E3.2（done，`2026-08-10-0739-2`，成本卷算取值豁免架构不变量固化——**E3.1 字段级脱敏取值侧前提已闭环**：服务端 `CostRollupService`/`StandardCostResolver` 经 DAO 直读架构性豁免 @BizLoader/data-auth，故 E3.1 masking 成本字段不阻断服务端跨域取值）
> - P1.2 Q1/Q4（done，`2026-08-09-1314-3`——成本聚合粒度 + 取值边界裁决；Q1/Q4 正式 field-level 裁决归 E4.x，**E3.1 不受 P1.2 阻塞** per roadmap §非阻塞声明）
> - F7 前端脱敏（已落地，hr 4 PII + logistics 2 写回型）——E3.1 将 hr 4 PII 从前端层升级到后端响应层
> - **E3.1 两项 Deps（P1.1 + E1.2）已 done（roadmap status block 核验），draftable**
> Audit: required
> Mission: permissions-enforcement
> Work Item: E3.1

## Current Baseline

E3.1 建立 **后端响应层脱敏控制点**：经 BizModel `@BizLoader` 按 role-view 打码 GraphQL 响应值，使 API 消费者（含第三方集成、F12 网络面板）拿到打码值而非明文。这是 F7 前端渲染层脱敏（AMIS tpl 打码，GraphQL 响应仍含明文）的**安全升级 successor**，覆盖保密五面中的金额/机密值字段 + F7 PII 集 + `taxFileNo`。

**冻结输入（P1.1，`field-formatting-patterns.md` §9.7，逐字段七元组）**：
- **E3.1 范围字段集**（§9.7「拟落地层」标 `E3.1` 或 `E3.1 + E4.1` 者，**排除**纯 `E4.1` 的配置 rate/枚举/集成事务标识）：
  - **面 A 薪酬（hr，~20 字段）**：`ErpHrSalary` 14 金额（basicSalary…netSalary + taxAmount + socialInsurance/housingFund）+ cumulativeData（个税机密）+ `ErpHrEmploymentContract.socialInsuranceBase` + `ErpHrSocialInsuranceBase` 2 + `ErpHrSalarySimulationItemAdjustment` 2。
  - **面 B 合同（ct，~9 字段）**：`ErpCtContract.totalAmount` + `ErpCtContractLine.amount` + `ErpCtInvoicePlan.amount` + `ErpCtConsumptionLine.amount` + `ErpCtRebateAgreement` 2 + `ErpCtRebateAccrual` 2 + `ErpCtRebateSettlement.totalRebateAmount`。（ApprovalMatrix min/maxAmount、RebateTier、SignatureRequest 字段为纯 E4.1，排除。）
  - **面 D 供应商价格（pur + md，~4 字段）**：`ErpPurSupplierPriceList.unitPrice` + `ErpMdMaterialSku` purchasePrice/salePrice/wholesalePrice。（taxRate/minOrderQuantity 为 E4.1，排除。）
  - **面 E 成本分解（mfg，6 字段）**：`ErpMfgCostRollupLine` materialCost/laborCost/overheadCost/subcontractCost/totalCost/unitCost。
  - **F7 PII 升级（hr，4 字段）**：`ErpHrEmployee` idCardNo/mobilePhone/bankAccountId/socialSecurityNo（当前前端 tpl 打码，E3.1 升级到后端 @BizLoader）。
  - **taxFileNo（hr，1 字段）**：当前 `visibleOn=false` 隐藏（§9.5 反模式「字段不可见非脱敏」），E3.1 路由 = unhide + 后端打码。
  - **面 C EDI（b2b）排除**：§9.7.6 全部标纯 `E4.1`（formatStandard/direction/attachmentFileId/error 为可见性范畴非金额脱敏），**不在 E3.1 范围**。
  - **logistics apiKey/apiSecret 排除**：已 `published=false` 写回型（明文永不离开服务端），无需 E3.1。
- **合计 E3.1 ≈ 44 字段**，跨 4 域集群（hr / ct / pur+md / mfg）。

**当前脱敏现状（§9.7 实测）**：保密五面字段均无脱敏（既无前端 tpl 也无后端 @BizLoader），xmeta 均 `published=true queryable=true` → GraphQL schema 影响 **Y**（响应含明文）。E3.1 **不改 published/queryable**（那是 E4.1 契约变更）——字段仍在 schema，值被打码。

**E3.2 取值豁免前提（done，load-bearing）**：服务端成本卷算（`CostRollupService`/`StandardCostResolver`）经 `IDaoProvider`/`IOrmTemplate` 直读，**不遍历 BizModel/GraphQL 边界** → 架构性豁免 @BizLoader masking + data-auth。故 E3.1 masking `ErpMfgCostRollupLine` / `ErpMdMaterialSku.purchasePrice` 的 GraphQL 响应**不阻断**服务端跨域成本取值（守卫测试 `TestErpMfgCostRollupValueExemptionInvariant` / `TestErpInvStandardCostResolverValueExemptionInvariant` 已固化，E3.2 done）。权威：`docs/design/finance/costing-methods.md §成本卷算取值豁免边界`。

**机制基线（Nop @BizLoader）**：`@BizLoader` 注解于 BizModel 方法，覆盖字段值计算（GraphQL 响应取该方法返回值）。读 role 经 `IUserContext.getRoles()`（enforcement 上下文）。既有 @BizLoader 范式见 `domain-design-guidelines.md §6.1` 机制 D。**E3.1 须裁决**：~44 字段的 @BizLoader 结构化方式（逐字段 @BizLoader + 共享 mask helper vs 其他），归 Phase 1 Decision。

**role-view 绑定现状**：§9.7「拟落地层」列对每面给出 P1.1 **建议**授权角色（薪酬→薪酬审批人；合同→合同审批人/合同专员；供应商价→采购员/管理员；成本→admin/财务员；PII→HR 专员/薪酬审批人）。**这不是正式 field-level 裁决**（§9.7 Non-Goal：逐字段角色绑定裁决归 E4.1，受 P1.2 Q1/Q4 约束）。E3.1 须将这些**建议**采纳为 masking role-view 的工作假设并记录（授权角色见明文，其余打码），不 preempt E4.1 的正式 published/queryable + 代理视图裁决。

**账号池（E1.2 done）**：薪酬审批人（role-hr-approver）、合同专员/审批人（role-ct-clerk/ct-approver）、采购员（role-pur）、管理员（nop/role-biz-admin）均已在 `ROLE_ACCOUNTS` + CSV 种子。负向 Proof 主体就绪。

## Goals

- 建立后端响应层脱敏控制点：`@BizLoader` 按 role-view 打码 GraphQL 响应值（保密五面金额/机密值 + F7 PII + taxFileNo，~44 字段）。
- 授权角色（按 §9.7 建议 + Phase 1 裁决）见明文，其余角色见打码值（API 消费者含第三方/F12 拿到打码值）。
- 不改 GraphQL schema（published/queryable 不动——契约变更归 E4.1）。
- E3.2 取值豁免不变量保持（服务端成本卷算不被 masking 阻断，守卫测试复跑绿）。
- owner doc（`field-formatting-patterns.md` §9.4 / §9.7）实现注记更新 + 日志。

## Non-Goals

- **字段级可见性（published/queryable=false）+ 代理视图**：归 E4.1（契约变更门控，受 P1.2 Q1/Q4 约束）。E3.1 仅 masking（值打码，字段仍在 schema）。
- **Q1 成本聚合粒度 + 档位映射代理视图**：归 E4.x（P1.2 Q1 Decision (d) 总额精确 + 要素档位离散）。E3.1 仅 mask 原始成本字段。
- **EDI 面（b2b）字段**：§9.7.6 纯 E4.1（可见性非金额脱敏），不在 E3.1。
- **logistics 凭据（apiKey/apiSecret）**：已 published=false 写回型，无需 E3.1。
- **保密字段读访问审计**：归 E4.2（app 侧拦截器写审计记录）。
- **prod 翻转 / 前端 F7 tpl 移除**：E3.1 落地后端层；前端 tpl 是否保留（双层冗余）或移除归 Phase 1 裁决（默认保留作 UI 防偷窥，后端层兜底 API 消费者）。
- **data-auth 行级过滤**：归 E2.x（正交层）。

## Task Route

- Type: `implementation-only change`（既有 @BizLoader 机制的新应用，不改 schema/ORM/平台；触成本区域须 plan-first 证据 = E3.2 固化 + owner doc + 守卫测试）
- Owner Docs: `docs/design/field-formatting-patterns.md` §9.4（后端响应层 successor）+ §9.7（冻结清单）；`docs/design/finance/costing-methods.md §成本卷算取值豁免边界`（E3.2 取值侧前提）；`docs/design/roles-and-permissions.md` §数据权限（E3.2 交叉引用）
- Skill Selection Basis: `nop-backend-dev`（@BizLoader 实现是核心，auth/成本区域 plan-first）；`nop-testing`（单元 Proof + 负向 masking 断言）

## Infrastructure And Config Prereqs

- 无新 config 开关（E3.1 是 BizModel 层逻辑，不经 data-auth/action-auth 开关；masking 经 `IUserContext` role 判定，无独立灰度门控）。
- 若 Phase 1 裁决 masking 需 config-gated（如 `erp.masking.response-layer-enabled`，默认 ON），须在 %dev/%test/%prod profile 预置——归 Phase 1 Decision。
- 账号池就绪（E1.2），无外部端口/密钥依赖。

## Execution Plan

### Phase 1 - Decision/Explore：masking 机制 + role-view 绑定 + 字段集枚举

Status: completed
Targets: 本计划 Decision 节 + `docs/design/field-formatting-patterns.md` §9.4
Skill: `nop-backend-dev`

- Item Types: `Decision | Explore | Proof`
- Prereqs: P1.1 done（冻结清单）+ E3.2 done（取值豁免前提）

- [x] `Explore`: 核验 Nop @BizLoader masking 机制的可复用结构。证据：`nop-entropy/docs-for-ai/03-runbooks/add-bizloader-field.md`（`@BizLoader(forType=T.class, value="field")` + `@ContextSource T` 覆盖既有字段 fetcher）+ `04-reference/source-anchors.md` BIZ-006（`BizLoader.java` + `LoginApiBizModelDelta.java`）+ `02-core-guides/api-and-graphql.md`（扩展返回字段优先 `@BizLoader`，字段不存在时 Delta + `autoCreateField=true`）+ 平台既有范式 `NopAuthRoleBizModel.roleUsers`/`roleResourceIds`（entity BizModel 内 `@BizLoader` + `@ContextSource Entity`）。**结论**：无平台级 response transformer / field interceptor 更优机制；`BizModelToGraphQLDefinition.toBuilder` 在 `autoCreateField=false`（默认）时覆盖既有字段 data fetcher（field 类型不变），正是 masking 既有字段值的正确机制。逐字段 `@BizLoader` + 共享 `MaskHelper` 为正道。
  - Skill: `nop-backend-dev`
- [x] `Decision (a) 机制`: 采纳候选 (a)——逐字段 `@BizLoader("fieldName")` 于各域 entity BizModel + `@ContextSource Entity` 参数 + 共享 `MaskHelper`（落 `module-common-service` `app.erp.common.service.MaskHelper`，该模块为 5 个目标 service 模块的既有依赖）。替代方案 (b)（平台 response-level transformer）经 Explore 证伪（不存在）。残留风险：~44 个 loader 方法（机械重复，helper 去重逻辑，可接受）。
  - Skill: `nop-backend-dev`
- [x] `Decision (b) role-view 绑定`: 采纳 §9.7「拟落地层」建议为 masking 层工作假设（授权角色见明文，其余打码），逐面记录：薪酬金额 → `{薪酬审批人}`；hr PII 4 → `{HR 专员, 薪酬审批人}`；taxFileNo → `{HR 专员}`；ct 合同金额 → `{合同审批人, 合同专员}`；供应商价（pur+md）→ `{采购员, 管理员}`；mfg 成本分解 → `{管理员, 财务员}`。roleId 字面与 `nop_auth_role.csv` seed + 各域 `erp-*.action-auth.xml` `roles` 属性一致（同 `ErpHrConstants.HR_ROLE_ID="HR 专员"` 范式）。**显式声明**：此为 masking 层视图，不 preempt E4.1 正式 field-level visibility + 代理视图裁决。
  - Skill: none
- [x] `Decision (c) mask 格式`: **类型安全裁决**（不改 GraphQL schema = E3.1 不动 published/queryable，字段类型不变）：
  - DECIMAL/BIGINT 数值字段（薪酬金额、合同金额、供应商价、成本分解、bankAccountId BIGINT）→ 非授权返 `null`（BigDecimal/Long 字段不可表示 `"****"`；null = 值保留不披露，比末位模糊更安全——零泄漏；F7 末4/末N 位格式仅前端 tpl 层对字符串渲染有效，后端层不泄漏位数）。
  - VARCHAR PII：idCardNo → 首1+`******`+末4（`张******1234`，len>5 时，否则全打码）；mobilePhone → 首3+`****`+末4（`138****0000`，len>7 时，否则全打码）；socialSecurityNo → `******`（全打码，F7 §9.2 一致）；taxFileNo → `******`（全打码）；cumulativeData → `******`（全打码，个税机密 JSON）。
  - taxFileNo unhide 路由：移除 `ErpHrEmployee.view.xml` `visibleOn=${false}`（§9.5 反模式修正）+ 后端 @BizLoader 打码。
  - 与 F7 前端 tpl 一致性：VARCHAR PII 格式与 F7 §9.2 模板对齐；DECIMAL null 为后端层增强（前端层不渲染数值字段 tpl，无冲突）。精确档位/末位模糊归 E4.x Q1。
  - Skill: none
- [x] `Decision (d) 前端 F7 tpl 处置`: 保留（默认）。后端层 = API 消费者保护（第三方集成 / F12 网络面板拿到 null/打码值）；F7 前端 tpl = UI 截图防偷窥。授权 HR 用户经 API 拿明文但 UI 见打码（双层冗余，非冲突——UI 页面已由 action-auth 限定 HR 专员可见，后端层兜底未授权 API 消费者）。
  - Skill: none
- [x] `Proof 字段集冻结`: 据 §9.7 核对最终 E3.1 范围 = **43 字段**（§9.7 精确枚举），排除纯 E4.1（EDI 面 4 / ApprovalMatrix min-maxAmount / RebateTier 3 / SignatureRequest 4 / ErpHrSocialInsuranceConfig 4 rate / taxRate / minOrderQuantity）。逐字段实现清单（实体×字段×mask 格式×授权角色）见下方「Phase 1 字段集冻结表」。
  - Skill: none

Exit Criteria:

- [x] 4 项 Decision 落定（机制 / role-view / mask 格式 / 前端 tpl 处置）+ 字段集冻结清单产出（Phase 2-4 实现依据）。
- [x] masking 机制经 Explore 确认（无更优平台机制，@BizLoader + MaskHelper 正道）。

### Phase 1 字段集冻结表（43 字段，Phase 2-4 实现依据）

> 来源：§9.7 逐字段七元组。`mask` 列：`null` = 数值字段非授权返 null；`str(fmt)` = VARCHAR 非授权返打码字符串。`roles` = 授权见明文角色集。

| # | 域 | 实体 | 字段 | mask | roles |
|---|----|------|------|------|-------|
| 1 | hr | ErpHrSalary | basicSalary | null | 薪酬审批人 |
| 2 | hr | ErpHrSalary | positionAllowance | null | 薪酬审批人 |
| 3 | hr | ErpHrSalary | performanceBonus | null | 薪酬审批人 |
| 4 | hr | ErpHrSalary | overtimePay | null | 薪酬审批人 |
| 5 | hr | ErpHrSalary | mealAllowance | null | 薪酬审批人 |
| 6 | hr | ErpHrSalary | transportAllowance | null | 薪酬审批人 |
| 7 | hr | ErpHrSalary | otherAllowance | null | 薪酬审批人 |
| 8 | hr | ErpHrSalary | grossSalary | null | 薪酬审批人 |
| 9 | hr | ErpHrSalary | socialInsurance | null | 薪酬审批人 |
| 10 | hr | ErpHrSalary | housingFund | null | 薪酬审批人 |
| 11 | hr | ErpHrSalary | taxAmount | null | 薪酬审批人 |
| 12 | hr | ErpHrSalary | otherDeductions | null | 薪酬审批人 |
| 13 | hr | ErpHrSalary | netSalary | null | 薪酬审批人 |
| 14 | hr | ErpHrSalary | cumulativeData | str(******) | 薪酬审批人 |
| 15 | hr | ErpHrEmploymentContract | socialInsuranceBase | null | 薪酬审批人 |
| 16 | hr | ErpHrSocialInsuranceBase | socialInsuranceBase | null | 薪酬审批人 |
| 17 | hr | ErpHrSocialInsuranceBase | housingFundBase | null | 薪酬审批人 |
| 18 | hr | ErpHrSalarySimulationItemAdjustment | originalAmount | null | 薪酬审批人 |
| 19 | hr | ErpHrSalarySimulationItemAdjustment | adjustedAmount | null | 薪酬审批人 |
| 20 | hr | ErpHrEmployee | idCardNo | str(首1******末4) | HR 专员, 薪酬审批人 |
| 21 | hr | ErpHrEmployee | mobilePhone | str(首3****末4) | HR 专员, 薪酬审批人 |
| 22 | hr | ErpHrEmployee | bankAccountId | null (BIGINT) | HR 专员, 薪酬审批人 |
| 23 | hr | ErpHrEmployee | socialSecurityNo | str(******) | HR 专员, 薪酬审批人 |
| 24 | hr | ErpHrEmployee | taxFileNo | str(******) | HR 专员 |
| 25 | ct | ErpCtContract | totalAmount | null | 合同审批人, 合同专员 |
| 26 | ct | ErpCtContractLine | amount | null | 合同审批人, 合同专员 |
| 27 | ct | ErpCtInvoicePlan | amount | null | 合同审批人, 合同专员 |
| 28 | ct | ErpCtConsumptionLine | amount | null | 合同审批人, 合同专员 |
| 29 | ct | ErpCtRebateAgreement | totalAccumulatedAmount | null | 合同审批人, 合同专员 |
| 30 | ct | ErpCtRebateAgreement | estimatedRebateAmount | null | 合同审批人, 合同专员 |
| 31 | ct | ErpCtRebateAccrual | billAmountSource | null | 合同审批人, 合同专员 |
| 32 | ct | ErpCtRebateAccrual | accruedRebate | null | 合同审批人, 合同专员 |
| 33 | ct | ErpCtRebateSettlement | totalRebateAmount | null | 合同审批人, 合同专员 |
| 34 | pur | ErpPurSupplierPriceList | unitPrice | null | 采购员, 管理员 |
| 35 | md | ErpMdMaterialSku | purchasePrice | null | 采购员, 管理员 |
| 36 | md | ErpMdMaterialSku | salePrice | null | 采购员, 管理员 |
| 37 | md | ErpMdMaterialSku | wholesalePrice | null | 采购员, 管理员 |
| 38 | mfg | ErpMfgCostRollupLine | materialCost | null | 管理员, 财务员 |
| 39 | mfg | ErpMfgCostRollupLine | laborCost | null | 管理员, 财务员 |
| 40 | mfg | ErpMfgCostRollupLine | overheadCost | null | 管理员, 财务员 |
| 41 | mfg | ErpMfgCostRollupLine | subcontractCost | null | 管理员, 财务员 |
| 42 | mfg | ErpMfgCostRollupLine | totalCost | null | 管理员, 财务员 |
| 43 | mfg | ErpMfgCostRollupLine | unitCost | null | 管理员, 财务员 |

### Phase 2 - hr 域 masking（薪酬面 ~20 + F7 PII 升级 4 + taxFileNo 1）

Status: completed
Targets: `module-hr/erp-hr-service/.../biz/`（ErpHrSalaryBizModel / ErpHrEmployeeBizModel / 等）；`module-hr/erp-hr-web/.../ErpHrEmployee.view.xml`（taxFileNo unhide）
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 done（机制 + role-view + 字段集冻结）

- [x] `Add`: `MaskHelper`（共享脱敏工具，`module-common-service` `app.erp.common.service.MaskHelper` + `StringMaskFormat`）—— `maskDecimal/maskLong/maskString` + `isAuthorized(Set<String> authorizedRoles)`：授权角色返原值，否则按 fmt 打码（数值 null / VARCHAR 打码串）。fail-closed（无 IUserContext = 打码）。
  - Skill: `nop-backend-dev`
- [x] `Add`: ErpHrSalary 13 金额字段 + cumulativeData @BizLoader（委托 MaskHelper，授权=薪酬审批人）；ErpHrEmploymentContract.socialInsuranceBase + ErpHrSocialInsuranceBase 2 + ErpHrSalarySimulationItemAdjustment 2 同模式。
  - Skill: `nop-backend-dev`
- [x] `Add`: F7 PII 升级——ErpHrEmployee idCardNo/mobilePhone/bankAccountId/socialSecurityNo @BizLoader（授权=HR 专员/薪酬审批人，mask 格式与 F7 前端 tpl 一致：idCardNo 首1末4 / mobilePhone 首3末4 / bankAccountId null(BIGINT) / socialSecurityNo 全打码）。
  - Skill: `nop-backend-dev`
- [x] `Add`: taxFileNo unhide + 后端打码——移除 `ErpHrEmployee.view.xml` `visibleOn=${false}`（§9.5 反模式修正，改为静态 `******` tpl）+ @BizLoader 打码（授权=HR 专员）。
  - Skill: `nop-backend-dev`
- [x] `Proof`: hr 域 masking 单元测试 `TestErpHrResponseMasking`（9 用例，授权角色见明文 + 非授权见打码/null + fail-closed，覆盖薪酬金额 + cumulativeData + PII 4 + taxFileNo + contract/insurance/simAdj）；`mvn test -pl module-hr/erp-hr-service -Dtest='*Mask*'` = Tests run: 9, Failures: 0, Errors: 0。全 hr-service 套件 BUILD SUCCESS（无既有快照测试引用 masked 字段，零回归）。
  - Skill: `nop-testing`

Exit Criteria:

- [x] hr 域 25 字段 masking 落地（授权见明文 + 非授权打码/null）；taxFileNo unhide + 打码；单元 Proof 绿。
- [x] hr-service 包类型检查通过（`mvn test -pl module-hr/erp-hr-service` BUILD SUCCESS；MaskHelper 在 common-service 已 install）。

### Phase 3 - ct 域合同金额 + pur/md 供应商价格 masking

Status: completed
Targets: `module-contract/erp-ct-service/.../biz/`；`module-purchase/erp-pur-service/.../biz/`；`module-master-data/erp-md-service/.../biz/`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 2 done（MaskHelper 就绪）

- [x] `Add`: ct 域合同金额 9 字段 @BizLoader（ErpCtContract.totalAmount / ContractLine.amount / InvoicePlan.amount / ConsumptionLine.amount / RebateAgreement 2 / RebateAccrual 2 / RebateSettlement.totalRebateAmount；授权=合同审批人/合同专员，委托 MaskHelper）。
  - Skill: `nop-backend-dev`
- [x] `Add`: pur 域 ErpPurSupplierPriceList.unitPrice + md 域 ErpMdMaterialSku purchasePrice/salePrice/wholesalePrice @BizLoader（授权=采购员/管理员）。
  - Skill: `nop-backend-dev`
- [x] `Proof`: ct + pur/md masking 单元测试（`TestErpCtResponseMasking` 4 用例 + `TestErpPurMdResponseMasking` 3 用例 + `TestErpMdResponseMasking` 3 用例，授权/非授权双侧 + fail-closed）；`mvn test -pl module-contract/erp-ct-service,module-purchase/erp-pur-service,module-master-data/erp-md-service -Dtest='*Mask*'` = Tests run: 10, Failures: 0, Errors: 0。全 3 模块套件 BUILD SUCCESS（零回归）。
  - Skill: `nop-testing`

Exit Criteria:

- [x] ct 9 + pur/md 4 字段 masking 落地；单元 Proof 绿。

### Phase 4 - mfg 成本分解 masking + E3.2 不变量复验 + owner doc + 日志

Status: completed
Targets: `module-manufacturing/erp-mfg-service/.../biz/`；`docs/design/field-formatting-patterns.md`；`docs/design/finance/costing-methods.md`；`docs/logs/2026/08-10.md`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 3 done

- [x] `Add`: mfg 域 ErpMfgCostRollupLine 6 成本字段 @BizLoader（materialCost/laborCost/overheadCost/subcontractCost/totalCost/unitCost；授权=管理员/财务员，委托 MaskHelper）。**成本区域 plan-first 证据**：E3.2 取值豁免不变量保证服务端卷算不阻断——本项仅在 BizModel/GraphQL 边界 masking，不触 `CostRollupService` 业务逻辑。
  - Skill: `nop-backend-dev`
- [x] `Proof`（E3.2 不变量复验，load-bearing）: masking 落地后复跑 `TestErpMfgCostRollupValueExemptionInvariant`（2/0/0）+ `TestErpInvStandardCostResolverValueExemptionInvariant`（2/0/0）绿——证明 @BizLoader masking 不破坏服务端跨域成本取值豁免（反射断言 @Inject 字段不含 user-context 类型仍成立）。
  - Skill: `nop-testing`
- [x] `Proof`: mfg 成本 masking 单元测试 `TestErpMfgResponseMasking`（3 用例：管理员/财务员见明文 + 非授权 null + fail-closed）；`mvn test -pl module-manufacturing/erp-mfg-service -Dtest='*Mask*'` = Tests run: 3, Failures: 0, Errors: 0。
  - Skill: `nop-testing`
- [x] `Add`: owner doc 更新——`field-formatting-patterns.md` §9.4「后端响应层」从 successor 改为「已落地（E3.1）」+ §9.7 header 增 E3.1 状态横幅 + §9.7.10 复核结论更新；`costing-methods.md §成本卷算取值豁免边界` 增 E3.1 masking 交叉引用（消费侧代理视图仍归 E4.x）。
  - Skill: none
- [x] `Add`: `docs/logs/2026/08-10.md` 聚合日志条目（E3.1 masking 落地 + 机制裁决 + E3.2 不变量复验 + 验证状态）。
  - Skill: none

Exit Criteria:

- [x] mfg 成本 6 字段 masking 落地；E3.2 不变量守卫测试复跑绿（取值豁免未被破坏）；单元 Proof 绿。
- [x] owner doc + 日志已更新（§9.4 后端层从 successor 改已落地）。

## Draft Review Record

- Independent draft review iteration 1: **acceptable as-is**（`ses_01436f1f0ffeVuZu8Wb1Mp5iis`，fresh-session general 子代理，未起草本计划）— 全 checklist 项 PASS：A 格式完整 / B Deps 满足[P1.1+E1.2=done，E3.1 不受 P1.2 阻塞 per roadmap 非阻塞条款] / C 单一结果表面[~44 字段 uniform @BizLoader+MaskHelper 机制 + 单一 closure 标准，per-domain phase 划分为合法排序非不当拆分，不过大] / **D Current Baseline 零信任核验全 VERIFIED**（§9.7 字段清单 + 拟落地层 tag / 字段集 ~43≈44 排除纯 E4.1 / F7 hr 4 PII tpl landed / logistics 2 published=false / taxFileNo visibleOn=false / E3.2 守卫测试存在 / ErpMdEmployee 无 userId 列）零 FALSIFIED / E scope 正确排除纯 E4.1（EDI 面/logistics 凭据/config rate/proxy 视图/E4.2 审计）+ 不改 schema / F 反松弛 / G item typing[Phase 1 Explore+Decision 含替代方案] / H Skill / I 成本区域 plan-first 证据齐备（E3.2 固化 + costing-methods.md + 守卫测试，仅 BizModel/GraphQL 边界 masking 不触 CostRollupService 业务逻辑）/ J Closure Gates[含 E3.2 守卫测试 + 独立子代理审计] / K Phase 1 Decision gate 充分（机制/role-view/mask 格式/字段集冻结于实现前闭环）/ L 一致性[「不改 schema」vs「unhide taxFileNo」= view.xml 层 vs xmeta schema 层，区别连贯]。**0 Blocker / 0 Major / 3 Minor（信息性）**：(m1) 面 A 字段计数 ~44 vs 实际 43（Phase 1 Proof 重新冻结精确枚举，非阻塞）；(m2) Phase 1 Decision (b) 平台 transformer 候选与 roadmap 指定 @BizLoader 略冗余（Explore 仍正确验证）；(m3) DECIMAL mask 格式留待 Phase 1 Decision gate（Phase 1 闭环于实现前）。共识达成，转 active。

## Closure Gates

> 完整仓库验证在结束时运行一次。

- [x] 范围内行为完成（43 字段 @BizLoader masking 跨 hr/ct/pur/md/mfg + taxFileNo unhide）
- [x] 相关文档对齐（field-formatting-patterns §9.4/§9.7 + costing-methods §取值豁免边界）
- [x] 已运行验证：`mvn clean install -DskipTests`（156 模块 BUILD SUCCESS）+ `mvn test`（全 reactor 仅 1 pre-existing failure `TestErpDrpSafetyStock`，stash 对照确认与 E3.1 无关；E3.1 范围内 5 service 模块全绿 + 22 masking 单元 Proof 绿 + **E3.2 守卫测试 `TestErpMfgCostRollupValueExemptionInvariant`/`TestErpInvStandardCostResolverValueExemptionInvariant` 绿**）+ 各域 masking 单元 Proof 绿 + `bash docs/audits/nop-compliance-checker.sh`（exit 0，零漂移，对照 `docs/testing/known-good-baselines.md`）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 字段级可见性（published/queryable=false）+ 代理视图

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 归 E4.1（契约变更门控，受 P1.2 Q1/Q4 约束）。E3.1 仅 masking（值打码，schema 不变）；E4.1 做 schema 级隐藏 + Q1 档位代理视图。双层分工，E3.1 是 E4.1 的取值侧前置（已闭环）。
- Successor Required: yes（触发条件 = E4.1 进入，deps E3.1）

### 保密字段读访问审计

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 归 E4.2（app 侧拦截器写审计记录）。E3.1 仅脱敏值，不记审计。
- Successor Required: yes（触发条件 = E4.2 进入，deps E4.1）

### EDI 面（b2b）字段可见性

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: §9.7.6 全标纯 E4.1（可见性非金额脱敏），不在 E3.1 masking 范围。
- Successor Required: yes（触发条件 = E4.1 进入覆盖 b2b 面）

## Closure

Status Note: E3.1 后端响应层脱敏控制点已落地。43 字段经 @BizLoader + 共享 MaskHelper 实现 role-view masking（授权见明文，非授权数值 null / VARCHAR 打码串），不改 GraphQL schema（published/queryable 不动）。E3.2 取值豁免不变量守卫测试复跑绿（masking 不阻断服务端跨域成本取值）。taxFileNo unhide + 后端打码（§9.5 反模式修正）。F7 前端 tpl 保留作 UI 防偷窥双层冗余。全 reactor `mvn clean install -DskipTests` BUILD SUCCESS（156 模块）+ E3.1 范围内 5 service 模块全 `mvn test` 绿 + 22 masking 单元 Proof 绿 + E3.2 守卫测试绿 + compliance checker exit 0（零漂移）。全 reactor `mvn test` 仅 1 pre-existing failure（`TestErpDrpSafetyStock`，stash 对照确认与 E3.1 无关，drp 域安全库存计算业务逻辑 issue）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 `ses_0135909dcffedFdJe7LBb5x9Ph`（general，新会话，未执行本计划）
- Evidence: A-G 全 PASS — 代码存在正确（MaskHelper + 5 域 43 字段 @BizLoader + taxFileNo unhide）/ role-view 绑定匹配 / 22 masking 单元测试（hr 9 + ct 4 + pur 3 + md 3 + mfg 3）全绿 / E3.2 守卫测试 4 绿（取值豁免未破坏）/ owner doc §9.4/§9.7/costing-methods 更新 / roadmap E3.1 done / 计划内部一致。Overall verdict: PASS。

Follow-up:

- 字段级可见性 + 代理视图（见 E4.1 successor）
- 保密字段读访问审计（见 E4.2 successor）
