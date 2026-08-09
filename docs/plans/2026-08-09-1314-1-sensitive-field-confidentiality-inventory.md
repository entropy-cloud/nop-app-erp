# 2026-08-09-1314-1 sensitive-field-confidentiality-inventory

> Plan Status: completed
> Last Reviewed: 2026-08-09
> Source: `docs/backlog/permissions-enforcement-roadmap.md` P1.1
> Related: `docs/plans/2026-07-22-1400-3-cross-cutting-sensitive-field-masking.md`（F7 前端脱敏已落地）、mission `permissions-enforcement`
> Audit: required
> Mission: permissions-enforcement
> Work Item: P1.1

## Current Baseline

F7 前端渲染层脱敏已落地（gen-control tpl 打码），单一真相源为 `docs/design/field-formatting-patterns.md` §9（§9 line 279 明确覆盖 4 个 hr 字段 + 2 个 logistics 字段）：

- **hr `ErpHrEmployee`**（`module-hr/model/app-erp-hr.orm.xml:283-303`）：`idCardNo`（证件号, propId=9）/ `mobilePhone`（手机号, propId=11）/ `bankAccountId`（工资卡账户, BIGINT propId=28）/ `socialSecurityNo`（社保号, propId=29）—— **4 字段**，经 view.xml gen-control tpl 打码。
- **logistics `ErpLogCarrierConfig`**（`module-logistics/model/app-erp-logistics.orm.xml:138-139`）：`apiKey` / `apiSecret`（写回型凭据，xmeta `published="false" queryable="false"`，全仓唯一后端级保密先例）。

**已发现的脱敏缺口（非 F7 集，但属敏感字段）**：hr `ErpHrEmployee.taxFileNo`（个税档案号, propId=30）当前**非 tpl 脱敏**，而是经 `<visibleOn>${false}</visibleOn>` **隐藏**（`ErpHrEmployee.view.xml:223-225`）——属 §9.5 反模式自检表点名的"字段不可见，非脱敏"。本清单将其作为"已发现敏感字段、现状态=隐藏（非脱敏）、拟路由至 E3.1 后端响应层脱敏"单独登记，**不**计入 F7 已落地基线。

enforcement 路线图点名的**保密五面**当前**均无脱敏**（既无前端 tpl，也无后端 `@BizLoader`），仅 F7 PII 集有脱敏。已实测定位的字段：

- **薪酬面**：hr `ErpHrSalary`（`grossSalary`/`socialInsurance`/`taxAmount`/`netSalary`，propId=12/13/15/17）、`ErpHrSalarySimulation*`、`ErpHrEmploymentContract.socialInsuranceBase`（propId=14）、`ErpHrSocialInsuranceBase`/`ErpHrSocialInsuranceConfig`。
- **合同面**：contract `ErpCtContract.totalAmount`（propId=9）、合同开票/返利金额行（`amount`/`totalAccumulatedAmount`/`estimatedRebateAmount`）、电子签配置（`docs/design/b2b/edi-formats.md` 邻近的 e-sign dict）。
- **EDI 面**：b2b `ErpB2bEdiFormat` / `ErpB2bEdiDoc`（formatStandard/direction/载荷字段）。
- **供应商价格面**：purchase `ErpPurSupplierPriceList`（`module-purchase/model/app-erp-purchase.orm.xml:384`）+ master-data `ErpMdMaterialSku.purchasePrice`（`module-master-data/model/app-erp-master-data.orm.xml:384`）。
- **成本分解面**：mfg `ErpMfgCostRollupLine`（`module-manufacturing/model/app-erp-manufacturing.orm.xml:1259-1281`：`materialCost`/`laborCost`/`overheadCost`/`subcontractCost`/`totalCost`/`unitCost`，propId=6-11）。

**缺口**：五面无字段级清单（实体×字段×现脱敏方式×GraphQL schema 影响标记）；F7 PII 集现状未在 roadmap 语境下显式复核确认；后端响应层脱敏（§9.4 successor）与字段级可见性（E4.1）的输入边界尚未冻结——二者均依赖本清单作为 P1.1 下游门控。

## Goals

- 产出**保密五面字段级清单**：每面逐字段记录 `{实体, 字段名, propId, stdSqlType, 当前脱敏方式（F7 前端 tpl / 写回型 published=false / 无）, xmeta published/queryable 现值（GraphQL schema 是否暴露该字段）, 拟落地的 enforcement 层（E3.1 后端响应层 / E4.1 字段级可见性）}`。
- **显式复核确认 F7 既有 PII 集**（hr employee **4 字段**：idCardNo/mobilePhone/bankAccountId/socialSecurityNo + logistics carrier config 2 字段：apiKey/apiSecret）的当前运行时状态与 GraphQL 暴露面，作为 P1.1 范围内"已落地基线"锚点；同时登记 `taxFileNo` 为"隐藏非脱敏"敏感字段（路由 E3.1）。
- 每字段标注**是否影响 GraphQL schema**（即 `published`/`queryable` 翻转是否会改变对外契约），为 E4.1 契约变更门控（横切关注点 5）提供冻结输入。
- 清单落盘为 `docs/design/field-formatting-patterns.md` §9.4 区域的**字段级清单小节**（单一真相源），并从 `docs/design/roles-and-permissions.md` 数据权限/敏感字段相关节交叉引用。

## Non-Goals

- **不实现**后端响应层脱敏控制点（E3.1）或字段级可见性（E4.1）——本计划仅产清单。
- **不改 GraphQL schema**（不改 xmeta published/queryable、不改 ORM）——仅读取并记录现状。
- **不做逐字段角色绑定裁决**（哪些角色可见哪些字段归 E4.1，受 Q1/Q4 裁决 P1.2 约束）。
- **不做成本取值豁免裁决**（CostRollupService 跨域读值归 P1.2 Q4 + E3.2）。
- **不覆盖长尾低敏字段**（如各域审计时间戳、非业务核心配置 rate）——以保密五面为边界。

## Task Route

- Type: `requirement clarification`（字段级清点 + 范围冻结，产文档非代码）
- Owner Docs: `docs/design/field-formatting-patterns.md` §9（F7）/ §9.4（后端脱敏 successor）；`docs/design/roles-and-permissions.md`（敏感字段与数据权限交叉引用）
- Skill Selection Basis: `none` —— 纯字段清点与文档记录，无 Nop 代码/模型/页面模式应用；与 roadmap 表格 P1.1 Skill 列一致。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（只读 ORM/xmeta 现状，不改运行时）。

## Execution Plan

### Phase 1 - 五面字段枚举与现状记录

Status: completed
Targets: `module-hr/`、`module-contract/`、`module-b2b/`、`module-purchase/`、`module-master-data/`、`module-manufacturing/` 下相关 `*.orm.xml` + 对应 `*.xmeta`（published/queryable 现值）
Skill: none

- Item Types: `Add`（清单条目）/ `Proof`（现状可证实）
- Prereqs: 无

- [x] **Add**：薪酬面逐字段枚举（`ErpHrSalary` 金额字段、`ErpHrSalarySimulation*`、`ErpHrEmploymentContract.socialInsuranceBase`、`ErpHrSocialInsuranceBase/Config`），记录 `{字段, 当前脱敏方式, xmeta published/queryable 现值, GraphQL schema 影响 Y/N, 拟落地层}`。
  - Skill: none
- [x] **Add**：合同面逐字段枚举（`ErpCtContract.totalAmount` + 开票/返利金额行 + 电子签相关字段），同上记录格式。
  - Skill: none
- [x] **Add**：EDI 面逐字段枚举（`ErpB2bEdiFormat`/`ErpB2bEdiDoc` 载荷/方向/标准字段），同上记录格式。
  - Skill: none
- [x] **Add**：供应商价格面逐字段枚举（`ErpPurSupplierPriceList` 全价格字段 + `ErpMdMaterialSku.purchasePrice`），同上记录格式。
  - Skill: none
- [x] **Add**：成本分解面逐字段枚举（`ErpMfgCostRollupLine` materialCost/laborCost/overheadCost/subcontractCost/totalCost/unitCost），同上记录格式。
  - Skill: none
- [x] **Proof**：对每个枚举字段，引用其 `*.orm.xml` 行号 + 对应 `*.xmeta` 的 published/queryable 现值（grep 证据），证实"GraphQL schema 影响 Y/N"判定有据。
  - Skill: none
- [x] **Proof**：单独审计 hr `ErpHrEmployee.taxFileNo` 现状（`visibleOn=${false}` 隐藏非脱敏，§9.5 反模式），在清单中登记为"已发现敏感字段、现状态=隐藏、拟路由 E3.1 后端响应层脱敏"。
  - Skill: none

Exit Criteria:

- [x] 五面字段清单条目齐全（每面至少覆盖 Goal 点名的实体/字段，无遗漏已点名字段）。
- [x] 每条目携带 `{实体, 字段, propId, 当前脱敏方式, published/queryable 现值, GraphQL schema 影响, 拟落地层}` 七元组，可被 E3.1/E4.1 直接消费。

### Phase 2 - F7 既有 PII 集复核确认

Status: completed
Targets: `module-hr/erp-hr-web/`（hr employee view.xml gen-control tpl）、`module-logistics/erp-log-web/`（carrier config）、对应 `*.xmeta`
Skill: none

- Item Types: `Proof`
- Prereqs: 无（F7 PII 复核与五面枚举逻辑独立，可并行）

- [x] **Proof**：核验 hr `ErpHrEmployee` 4 字段（idCardNo/mobilePhone/bankAccountId/socialSecurityNo）的前端 tpl 打码现状（view.xml gen-control 证据）+ GraphQL 暴露面（xmeta published 现值 → 响应是否含明文）。
  - Skill: none
- [x] **Proof**：核验 logistics `ErpLogCarrierConfig` apiKey/apiSecret 的写回型保密现状（xmeta `published="false" queryable="false"` 证据 → 响应不含字段值）。
  - Skill: none
- [x] **Proof**：在清单中显式标注 F7 PII 集（hr 4 + logistics 2）为"已落地前端脱敏基线"，区分于五面"待落地后端/字段级"与 taxFileNo"隐藏非脱敏"。
  - Skill: none

Exit Criteria:

- [x] F7 PII 集现状经实测证据确认（grep/行号引用），与 §9 文档描述一致或差异已登记。

### Phase 3 - 清单落盘与交叉引用

Status: completed
Targets: `docs/design/field-formatting-patterns.md` §9.4、`docs/design/roles-and-permissions.md`
Skill: none

- Item Types: `Add` / `Decision`
- Prereqs: Phase 1 + Phase 2

- [x] **Add**：将五面清单 + F7 PII 集确认写入 `field-formatting-patterns.md` §9.4（后端响应层脱敏 successor 区）作为字段级清单小节，作为 E3.1/E4.1 的冻结输入。
  - Skill: none
- [x] **Decision**：对"清单边界取舍"记录决策——长尾低敏字段（各域审计时间戳、非业务核心配置 rate）移出范围。考虑的替代方案：(a) 仅保密五面（采纳，roadmap 显式边界）；(b) 含长尾（拒绝：非保密诉求，与 F6 §7 长尾 defer 范畴重叠，稀释清单信噪比）。残留风险：若未来出现新保密诉求，需扩清单（已记 Deferred 触发条件）。
  - Skill: none
- [x] **Add**：在 `roles-and-permissions.md` 数据权限/敏感字段相关节增加一句交叉引用指向 §9.4 清单（不在散文重复字段定义）。
  - Skill: none

Exit Criteria:

- [x] 清单以机器可消费的表格形式（七元组）落盘于单一真相源（§9.4），可被 P1.3/E3.1/E4.1 直接引用。
- [x] 交叉引用落地，无散文重复生成文件已定义的字段。

## Draft Review Record

- Independent draft review iteration 1: needs revision（0 blocker / 1 major / 3 minor）（ses_01b0baaccffe1boSkcETKxKyYe）。Major：`taxFileNo` 误列为 F7 tpl 脱敏集（实为 `visibleOn=${false}` 隐藏非脱敏，§9 文档明确 F7 集=4 hr 字段）；minor：`*.xmeta.xml`→`*.xmeta`、Phase 2→Phase 1 伪依赖、Phase 3 Decision 替代方案偏薄。
- 合并修订（iteration 1 → v2）：F7 集修正为 hr 4 字段 + logistics 2 字段；`taxFileNo` 重分类为"已发现敏感字段、现状态=隐藏非脱敏、路由 E3.1"并单列 Proof 审计项；`*.xmeta` 修正；Phase 2 Prereqs 放宽为"无（可并行）"；Phase 3 Decision 补替代方案 (a)/(b) + 残留风险。
- Independent draft review iteration 2: accept（0 blocker / 0 major / 0 minor，2 informational）（ses_01b07180bffejcEw04fPGc2Pd5）。4 项 iteration-1 发现全部 RESOLVED（live-repo 核验：§9 line 279 确认 F7 集=4 hr 字段；`ErpHrEmployee.view.xml:223-225` 确认 taxFileNo 为 `visibleOn=${false}` 隐藏非 tpl 脱敏；plan 内无残留 F7-masked 错称；`*.xmeta`/Phase 2 Prereqs/Phase 3 Decision 替代方案均已落位）；无新阻塞项。共识达成，可转 `active`。

## Closure Gates

> 本计划为纯文档/裁决工作（无生产代码、无 ORM/xmeta/view 改动）。按计划指南模板注记，移除 `typecheck`/`build`/`test` 验证命令门控，理由：无代码变更，验证对象为文档内部一致性与证据可追溯性。

- [x] 范围内行为完成（五面清单 + F7 PII 确认齐全且落盘）
- [x] 相关文档对齐（§9.4 清单 + `roles-and-permissions.md` 交叉引用）
- [x] 无代码变更 → 跳过 build/test 门控（已说明理由）；文档内部一致性 + 证据可追溯性已核验
- [x] 无范围内项目降级为 deferred/follow-up（长尾字段显式移出范围并记录理由，非降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 长尾低敏字段（各域审计时间戳 / 非业务核心配置 rate）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 保密五面为 roadmap 显式边界；长尾字段非保密诉求，沿用 F6 §7 长尾 defer 范畴。
- Successor Required: no（触发条件 = 出现新保密诉求时再扩清单）

## Closure

Status Note: 完成。保密五面（薪酬/合同/EDI/供应商价格/成本分解）+ F7 已落地 PII 基线（hr 4 + logistics 2）+ taxFileNo（隐藏非脱敏）字段级七元组清单已落盘于 `docs/design/field-formatting-patterns.md` §9.7（§9.4 增指针），并在 `docs/design/roles-and-permissions.md` 数据权限节增加交叉引用。纯文档工作，无代码/ORM/xmeta/view 变更，跳过 build/test 门控。roadmap P1.1 状态 `ready`→`done`。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 `ses_01afee863ffeiYEDZpJz5e6xGL`（general agent，新会话，非执行者）
- Verdict: PASS（0 blocker / 0 major / 3 informational）
- Evidence: 逐字段核对 live ORM propId + 行号（hr/contract/b2b/purchase/md/mfg 全部命中）；xmeta `published="false"` 跨域 grep 仅命中 logistics apiKey/apiSecret/credentials；hr PII 字段无 published 覆盖（GraphQL 影响=Y 正确）；view.xml gen-control tpl 打码 + taxFileNo `visibleOn=${false}` 行号证据确认；taxFileNo 正确排除出 F7 基线计数；git status 仅 doc 文件变更（无 *.orm.xml/*.xmeta/*.view.xml/*.java）。informational 项（§9.7 标题层级、Closure 占位符）已于审计后修复。

Follow-up:

- F7 既有 tpl 实际使用 JS `.slice()`（非 §9.2/§9.5 规范的 amis-formula `LEFT/RIGHT`）——已在本清单 §9.7.2 透明登记，属 F7 follow-up，非 P1.1 范围。
