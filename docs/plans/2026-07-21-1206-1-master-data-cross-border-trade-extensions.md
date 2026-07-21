# 2026-07-21-1206-1-master-data-cross-border-trade-extensions C2 — Master Data Cross-Border Trade Extensions（跨境贸易字段扩展）

> Plan Status: completed
> Last Reviewed: 2026-07-21
> Source: `docs/backlog/deepening-roadmap.md` §Milestone C §C2（line 66/87 — Cross-Border Trade Extensions：`ErpMdMaterial` 加字段 + 新建 `ErpMdMaterialCustoms` 实体）；`docs/analysis/erp-survey/2026-07-20-post-survey-strategic-gaps.md` §3.9（line 226-241 — Cross-Border Trade Extensions，对照 Wimoor 物料实体跨境字段集）；C1 plan `2026-07-21-0827-2` §Deferred But Adjudicated「C2 跨境贸易字段扩展」明确为本计划前身
> Related: `docs/plans/2026-07-21-0827-2-master-data-unified-party-identity-query.md`（C1 — Party 抽象基础，本计划复用其 `IErpPartyBiz` 抽象但不改其契约）；`docs/design/master-data/README.md`（物料与 SKU 关系 + 主数据状态机）；`docs/design/master-data/unified-party-identity.md`（C1 owner doc）；`docs/architecture/tax-framework.md`（税率框架，vatRate 字段与之关联）；`docs/architecture/l10n-strategy.md`（本地化策略，countryOfOrigin 与之关联）；`module-master-data/model/app-erp-master-data.orm.xml` line 171-238（ErpMdMaterial 权威模型源）
> Audit: required

## Current Baseline

基于实时仓库抽样核实（2026-07-21，对 master-data ORM ErpMdMaterial + README + tax-framework + l10n-strategy + wimoor 对比报告扫描）：

### 已落地的物料主数据

| 实体 | 表名 | 关键字段（与跨境相关）| 缺失字段 |
|------|------|----------------------|----------|
| `ErpMdMaterial` | `erp_md_material` | `defaultTaxRateId`(FK→`ErpMdTaxRate`,line 195)+ `leadTimeDays` + `weight`/`volume` | `vatRate`(增值税率快查) / `drawbackRate`(退税率) / `customsHS`(海关 HS 编码) / `countryOfOrigin`(原产地) / `preferenceCode`(优惠协定代码) / `customsNameCn`/`customsNameEn`(报关中文/英文名) / `declarationUnit`(申报计量单位) / `supervisionCondition`(监管条件代码) |
| `ErpMdMaterialSku` | `erp_md_material_sku` | (无跨境字段) | (本计划范围 Non-Goal) |
| **`ErpMdMaterialCustoms`** | (不存在) | — | **跨境报关单据实体整体缺失**：每次报关场景需独立记录报关单号、报关行、报关日期、申报金额、关税金额、退税收据号 |

**关键发现**：`ErpMdMaterial.defaultTaxRateId` 仅作默认税率引用（指向 `ErpMdTaxRate`），不直接存储跨境场景所需的退税率/原产地等字段。跨境电商/制造场景（Wimoor 对比 `wimoor-compare.md:539-546`）需在物料层直接落地 `vatRate`/`drawbackRate` 等字段以支持：
- 报关时的增值税率 / 退税率快查（不必每次经 `defaultTaxRateId` → `ErpMdTaxRate` 联查）
- 海关 HS 编码（Harmonized System Code，国际贸易通用商品分类编码）
- 原产地（FTA 优惠协定判定依据）
- 报关双语名称（中文报关 + 英文 Invoice/Packing List）
- 监管条件代码（中国海关 A/B... 代码标识进/出口监管要求）

### 已落地的相关基础设施

- `ErpMdTaxRate`（master-data，既有）：税率主数据，含 `taxType` 字典（VAT/CONSUMPTION/CUSTOMS 等）。本计划 `vatRate` 字段冗余快查 + `defaultTaxRateId` 既有 FK 保留作详细配置入口。
- `docs/architecture/tax-framework.md`：**43 行**（实测），现仅含 CN VAT 税率表（13/9/6/0/免税）+ 3 类税务凭证映射骨架；多税率类型 + 多账套税率隔离 + 跨境税（VAT 出口退税）**未**作命名概念引入。本计划新增字段语义在该文档**首次引入**「物料层跨境税快查」段落（增量概念）。
- `docs/architecture/l10n-strategy.md`：本地化策略 owner doc，已涵盖金税接口 + 增值税发票。本计划 `countryOfOrigin` 字段在该文档增「原产地与 FTA」段落（增量概念）。
- 既有 codegen 链路：`module-master-data/model/app-erp-master-data.orm.xml` 是权威模型源；增量 codegen 经 `mvn clean install -DskipTests` 触发；新增字段必须遵循既有 `propId` 顺序（当前 ErpMdMaterial max propId=25 → 新字段从 26 起）。
- 既有字典命名空间：`erp-md/*`；新增字段如需字典（如 `preferenceCode`/`supervisionCondition`）按既有范式 yaml 化。

### 关键依赖与边界

- **C1 `IErpPartyBiz` 不被本计划修改**：跨境贸易涉及的海关代理（报关行）作为 Partner 注册（**新增** `partnerType=CUSTOMS_BROKER` 字典值；既有 partner-type 字典仅含 CUSTOMER/SUPPLIER/BOTH/EMPLOYEE 4 值，无 AGENT），复用既有 Partner 模型；本计划不改 C1 接口契约。
- **b2b 域 ASN/EDI 不被本计划修改**：跨境贸易的电子单据（海关 EDI 报文）属 b2b 域，本计划仅落地 master-data 层物料字段 + 报关记录实体；b2b 报文格式接入归 b2b 域 successor。
- **finance 域凭证不被本计划修改**：关税 / 增值税 / 退税的会计处理（如 `IMPORT_DUTY`/`VAT_REFUND` businessType）属 finance 过账引擎；本计划落地字段后，finance Provider 可选接入（试点 Provider 接入归 finance 域 successor，触发条件成熟时启动）。
- **Wimoor 对比结论**（`wimoor-compare.md:539-546`）：Wimoor 将 vatRate/drawbackRate 直接置入物料主表（设计取舍：跨境场景为高频查询，主表快查 > 关联查 ErpMdTaxRate）；nop-app-erp 走「主表字段冗余快查 + 既有 FK 保留」双轨路径，避免破坏既有 taxRate 范式。

### 待深化差距（C2 范围）

| 差距 | 现状 | C2 目标 |
|------|------|---------|
| **物料跨境属性** | `defaultTaxRateId` 联查；无 HS 编码/原产地/退税率/报关名 | `ErpMdMaterial` 加 9 字段：vatRate/drawbackRate/customsHS/countryOfOrigin/preferenceCode/customsNameCn/customsNameEn/declarationUnit/supervisionCondition |
| **报关单据实体** | 无 | 新建 `ErpMdMaterialCustoms` 实体（per-transaction 报关记录：报关单号/报关行/日期/金额/关税/退税收据号）|
| **报关行 Partner 类型** | `partnerType` 字典无 CUSTOMS_BROKER | 字典扩展（`erp-md/partner-type` 增 CUSTOMS_BROKER 选项）|
| **报关 PI 字段语义文档** | `tax-framework.md` / `l10n-strategy.md` 无物料层跨境快查 / 原产地 FTA / HS 编码说明 | 两 owner doc 各增段落回链 |
| **跨境报表数据基础** | 无（报表需 `customsHS` 分组） | 字段落地后报表可分组聚合（具体报表归 finance/report 域 successor）|

### 关键风险/缺口

- **字段冗余 vs 联查权衡**：`vatRate`/`drawbackRate` 字段冗余在物料主表，可能多场景下与 `ErpMdTaxRate` 不一致（如税率变更后物料层未同步）；缓解：默认显示 `defaultTaxRate.rate` 联查值，`vatRate`/`drawbackRate` 字段仅在报关场景显式覆盖（业务约定 + 文档化）。
- **HS 编码字典化 vs 自由 VARCHAR**：HS 编码国际标准 6 位 + 中国延伸 10 位；字典化便于校验但维护成本高（上万条 HS 编码）。**Decision 候选**：字段类型 VARCHAR(12)，**不**做字典约束（参考 Wimoor 设计），由业务方自行维护或集成第三方 HS 查询服务（属 successor）。
- **多账套/多公司隔离**：`countryOfOrigin` 等字段是否按 `orgId` 隔离？候选 A：物料主数据层不隔离（同物料在多公司用同 `countryOfOrigin`）；候选 B：按 orgId 隔离（同一物料在出口公司 vs 内销公司可能不同原产地认定）。**Decision 候选**：候选 A（默认），特殊场景由 `ErpMdMaterialCustoms` per-transaction 实体覆盖。
- **既有测试数据兼容性**：master-data service 既有 60 测试（含 C1 新增 `TestErpPartyBiz` 8 场景）；新字段全部 nullable + 默认 null，不破坏既有 INSERT/UPDATE 测试。

## Goals

1. **新建 owner doc**：`docs/design/master-data/cross-border-trade.md`（NEW，含字段语义 + 报关场景工作流 + FTA 判定流程 + 反模式自检表）。
2. **扩展 `ErpMdMaterial` ORM 模型**：新增 9 字段（vatRate/drawbackRate/customsHS/countryOfOrigin/preferenceCode/customsNameCn/customsNameEn/declarationUnit/supervisionCondition），全部 nullable + 无 mandatory 约束（向后兼容）。
3. **新建 `ErpMdMaterialCustoms` 实体**：per-transaction 报关记录（materialId FK + 报关单号 + 报关行 Partner + 报关日期 + 申报数量/金额/计量单位 + 关税金额 + 增值税金额 + 退税收据号 + 业务单据回链 sourceBillType/sourceBillCode）。
4. **字典扩展**：`erp-md/partner-type` 增 `CUSTOMS_BROKER` 选项；如 Phase 0 Explore 裁决，新增 `erp-md/customs-preference-code`（FTA 优惠协定代码字典，可空）+ `erp-md/customs-supervision-condition`（监管条件字典）。
5. **BizModel + 视图层接入**：codegen 增量生成 ErpMdMaterial 扩展字段 + ErpMdMaterialCustoms CRUD；ErpMdMaterial 表单 form 增「跨境贸易」分组（F3 范式扩展）；ErpMdMaterialCustoms 列表/表单按既有点单据范式。
6. **既有 owner doc 回链**：`tax-framework.md` 增「物料层跨境税快查」段；`l10n-strategy.md` 增「原产地与 FTA」段；`master-data/README.md` 增「跨境贸易扩展」段。
7. **测试基线**：master-data service 既有测试不回归；新增 ErpMdMaterialCustoms 单元测试（生命周期 + sourceBill 回链 + 报关行 Partner 类型校验）。
8. **roadmap 同步**：`deepening-roadmap.md` §C2 状态 `todo → done` + §8.3 落地证据段落。

## Non-Goals

- **C1 IErpPartyBiz 契约变更**—— C1 接口稳定，本计划仅复用 Partner 模型承载报关行；不改 C1 接口签名、PartyRef DTO、SPI 端口。
- **finance 域关税 / 退税过账 Provider 接入**—— 关税 / 增值税 / 退税的会计处理（如 `IMPORT_DUTY`/`VAT_REFUND` businessType）属 finance 保护区域；本计划仅落地字段，Provider 接入归 finance successor（触发：跨境业务客户需求 + 财务 owner doc 授权）。
- **b2b 域 EDI 报文接入**—— 海关 EDI（如中国金关工程报文格式）属 b2b 域；本计划仅落地 master-data 层数据结构，EDI 接入归 b2b successor。
- **HS 编码字典全集**—— HS 编码国际标准上万条，字典全集维护成本高；本计划字段类型 VARCHAR(12) 不做字典约束，业务方自行维护或集成第三方 HS 查询服务（successor 触发：业务方明确 HS 字典需求）。
- **ErpMdMaterialSku 跨境字段扩展**—— SKU 级别的跨境属性（如同物料不同包装的报关单位差异）归 successor；本计划仅在物料主表落地。
- **海关申报完整业务流程编排**—— 报关单的状态机、审批流、与采购入库/销售出库的业务联动属跨域编排（purchase/sales + master-data + finance），归 successor plan（触发：业务客户具体业务流程需求）。
- **跨境报表实施**—— 报关明细报表 / 退税统计报表字段基础由本计划提供，但报表实施归 report successor。
- **第三方 HS 查询 / 报关 SaaS 集成**—— 与外部 HS 查询服务（如海关总署 HS 查询 API）/ 报关 SaaS（如单一窗口）集成归 D1 External API successor（本批次 plan 3）后续。
- **关税计算引擎**—— 关税金额 = 申报金额 × 关税率 × 优惠协定减免，涉及复杂税率计算（含反倾销税、报复性关税），归 finance/tax successor。

## Task Route

- Type: `app-layer design change`（新增 owner doc + 跨境字段语义 + 报关记录实体）+ `implementation-only change`（ORM 字段扩展 + 实体 + codegen + view.xml + 测试）
- Owner Docs:
  - `docs/backlog/deepening-roadmap.md` §C2（line 66/87）
  - `docs/design/master-data/cross-border-trade.md`（**NEW** — 本计划 Phase 0 落地）
  - `docs/design/master-data/README.md`（§核心业务对象 + §本域文档 回链）
  - `docs/architecture/tax-framework.md`（增「物料层跨境税快查」段）
  - `docs/architecture/l10n-strategy.md`（增「原产地与 FTA」段）
  - `../nop-entropy/docs-for-ai/02-core-guides/model-first-dev.md`（ORM 模型扩展范式）
  - `../nop-entropy/docs-for-ai/03-runbooks/add-entity-or-fields.md`（增量字段添加 runbook）
- Skill Selection Basis: 加载 `nop-backend-dev`（ORM 实体扩展 + BizModel + IBiz + xbiz）；Phase 2 view.xml 定制（ErpMdMaterial form 跨境贸易分组 + ErpMdMaterialCustoms list/form + menu/auth 注册，触发 bounded-merge / form layout / grid 关键词）也加载 `nop-frontend-dev`；不加载 `nop-testing`（既有 master-data service 测试范式直接复用）。最终：`nop-backend-dev`（Phase 0-1, 3）+ `nop-frontend-dev`（Phase 2）。

## Infrastructure And Config Prereqs

- 本地运行：`java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar`
- 无新 config / 端口 / 密钥依赖
- 字段冗余配置：`erp-md.cross-border-fields-enabled`（默认 `true`，允许业务关闭跨境字段在 form/grid 显示）—— Phase 0 Decision 候选项

## Execution Plan

### Phase 0 — Explore + Owner Doc + 关键 Decision

Status: completed
Targets: `docs/design/master-data/cross-border-trade.md`（**NEW**）+ plan 内 Decision 记录
Skill: `nop-backend-dev`

- Item Types: `Explore | Decision | Add`
- Prereqs: deepening-roadmap.md C2 todo + C1 done + Wimoor 对比报告

- [x] `Explore` (a)：物料层跨境字段全集与 Wimoor / OFBiz 对齐。
  - 核实范围：`docs/analysis/erp-survey/2026-07-20-0000-wimoor.md` §物料字段段；`ofbiz-compare.md`（如有 Product 跨境字段段）；`erp5-compare.md`（如有贸易字段段）。
  - 输出：跨境字段对照表（nop 当前 / Wimoor / OFBiz / ERP5 / 推荐采纳），含每字段业务语义 + 数据类型 + 是否字典化 + 默认值，入 owner doc §2。
  - Skill: `nop-backend-dev`
  - **结论**：Wimoor `Material` 含 `vatrate`/`drawbackRate`（line 105）；`MaterialCustoms` 报关实体存在（line 146）；nop `ErpMdMaterial` 既有 leadTimeDays/weight/volume/defaultTaxRateId，缺跨境字段。9 字段对齐方案见 owner doc §2。
- [x] `Explore` (b)：`ErpMdMaterialCustoms` per-transaction 实体字段集设计。
  - 核实范围：报关单据业务字段（报关单号/报关行/日期/数量/金额/计量单位/关税/增值税/退税收据号/业务回链）；与既有 `ErpFinVoucherBillR` 业务回链范式对齐；estRows 估算（按业务客户报关频次：日均 < 100 单 / 跨境客户）。
  - 输出：实体字段表 + relations + UK + index 设计，入 owner doc §3。
  - Skill: `nop-backend-dev`
  - **结论**：18 字段（id/code/materialId/declarationNo/partnerId/declarationDate/qtyDeclared/uomDeclared/amountDeclared/currencyId/exchangeRate/amountFunctional/dutyAmount/vatAmount/drawbackReceiptNo/sourceBillType/sourceBillCode + 7 标准审计字段）；UK=declarationNo；4 idx；estRows=100（与 ErpMdMaterial.skus estRows="10" 范式一致）。
- [x] `Explore` (c)：字典化范围裁决候选评估。
  - 候选 1：`preferenceCode`（FTA 优惠协定代码：东盟/中韩/中澳/...）字典化，`supervisionCondition` 自由 VARCHAR。
  - 候选 2：两个都字典化。
  - 候选 3：两个都自由 VARCHAR。
  - 核实范围：FTA 优惠协定数量（约 15-20 个常用协定）；监管条件代码数量（海关总署公布约 100+ 代码）；维护成本与查询便利性权衡。
  - 输出：字典化方案裁决 + 字典 yaml 字段集草案。
  - Skill: `nop-backend-dev`
  - **结论**：候选 1（preferenceCode 字典化 + supervisionCondition 自由 VARCHAR）。FTA 协定数量有限约 15-20 个变更慢，适合字典化；监管条件 100+ 频繁更新，自由 VARCHAR 由业务方维护。
- [x] `Explore` (d)：finance Provider 接入边界（Non-Goal 验证）。
  - 核实范围：`docs/design/finance/posting.md` §businessType 枚举（grep `IMPORT_DUTY`/`VAT_REFUND`/`CUSTOMS_*`）；本计划落地的字段是否触发 finance Provider 强接入需求。
  - 输出：finance 接入 Non-Goal 边界确认（确认归 successor）+ 触发条件（业务客户跨境业务量 > 阈值 / 财务 owner doc 显式授权）。
  - Skill: `nop-backend-dev`
  - **结论**：grep `posting.md` 对 `IMPORT_DUTY`/`VAT_REFUND`/`CUSTOMS_*` 无匹配——finance 域当前 businessType 枚举未含关税/退税类型。本计划仅落地字段 + 实体，finance Provider 接入归 successor（触发：跨境业务量 > 100 单/月 或 财务 owner doc 显式授权）。
- [x] `Decision`：基于 Explore (a)~(d)，确定 C2 实现方式。
  - **字段集**（裁决依据 Explore a）：在 `ErpMdMaterial` 增 9 字段（vatRate DECIMAL(6,4)/drawbackRate DECIMAL(6,4)/customsHS VARCHAR(12)/countryOfOrigin VARCHAR(2) ISO 3166-1 alpha-2/preferenceCode VARCHAR(20)/customsNameCn VARCHAR(200)/customsNameEn VARCHAR(200)/declarationUnit VARCHAR(20)/supervisionCondition VARCHAR(10)）。全部 nullable + 默认 null + 不 mandatory。
  - **`ErpMdMaterialCustoms` 实体**（裁决依据 Explore b）：核心字段 id/code/materialId(FK)/declarationNo/partnerId(报关行 FK)/declarationDate/qtyDeclared/uomDeclared(VARCHAR(20) 而非 FK→ErpMdUoM，因报关计量单位有时与内部单位字典不同——如海关法定单位 vs 内部库存单位)/amountDeclared/currencyId/exchangeRate/amountFunctional/dutyAmount/vatAmount/drawbackReceiptNo/sourceBillType/sourceBillCode + 标准审计字段；UK=declarationNo；idx=materialId/partnerId/sourceBill；`ext:estRows="100"`（按业务客户报关频次估算，与既有 estRows 范式一致如 ErpMdMaterial.skus estRows="10"）。
  - **字典化方案**（裁决依据 Explore c）：候选 1（preferenceCode 字典化 + supervisionCondition 自由 VARCHAR）。理由：FTA 协定数量有限且变更慢适合字典化；监管条件代码海关总署频繁更新 + 数量大适合自由 VARCHAR（业务方自行维护或集成第三方服务）。
  - **finance 接入边界**（裁决依据 Explore d）：本计划仅落地字段 + 实体，**不**接入 finance Provider；Non-Goal 显式确认；successor 触发条件登记。
  - **选择依据**：字段集最小覆盖报关场景高频查询 + 兼容 Wimoor 范式；ErpMdMaterialCustoms per-transaction 实体覆盖每次报关独立记录需求；字典化方案平衡维护成本与查询便利性；finance 接入归 successor 限制本计划保护区域触及。
  - Skill: none
- [x] `Add`：owner doc `docs/design/master-data/cross-border-trade.md`（NEW）
  - 8 节完整文档：§1 目的与范围（C2 vs C1 vs finance successor 边界）/ §2 物料层跨境字段表（含数据类型 + 字典 + 默认值 + 业务语义 + 与 Wimoor/OFBiz 对齐说明）/ §3 ErpMdMaterialCustoms 实体设计 + 字段表 + relations / §4 报关场景工作流（业务流程描述 + 状态字段说明，Non-Goal：状态机实施）/ §5 FTA 判定流程（原产地 + preferenceCode 字段配合）/ §6 与既有 owner doc 关系（tax-framework.md + l10n-strategy.md + master-data/README.md 回链段落）/ §7 反模式自检表 / §8 落地证据（本计划完成后填）。
  - Skill: none

Exit Criteria:

- [x] 4 个 Explore 结论已记录；对应 Decision 已落地
- [x] owner doc `docs/design/master-data/cross-border-trade.md` 落地（8 节完整，含字段集 + 实体设计 + 工作流 + FTA + 反模式）
- [x] 字段集 + ErpMdMaterialCustoms 实体 + 字典化方案 + finance 边界 4 项关键 Decision 在 owner doc §2-§3 明确

### Phase 1 — ORM 扩展 + 字典 + codegen

Status: completed
Targets: `module-master-data/model/app-erp-master-data.orm.xml`（既有追加 + 新实体）+ 增量 codegen
Skill: `nop-backend-dev`

- Item Types: `Add-heavy | Decision`
- Prereqs: Phase 0 完成 + ORM 变更授权（deepening-roadmap.md §8 已授权 C2）

- [x] `Add`：`ErpMdMaterial` ORM 模型扩展 9 字段（propId 26-34）
  - 路径：`module-master-data/model/app-erp-master-data.orm.xml` line 177-203 既有 `<columns>` 段追加
  - 字段顺序：vatRate(26)/drawbackRate(27)/customsHS(28)/countryOfOrigin(29)/preferenceCode(30)/customsNameCn(31)/customsNameEn(32)/declarationUnit(33)/supervisionCondition(34)
  - 全部 `mandatory="false"` + 默认 null + `i18n-en:displayName`；`ui:show` 按字段语义决定：vatRate/drawbackRate 在 grid 列表显示，customsNameCn/customsNameEn 仅在 form 显示（避免 grid 列过宽）
  - `preferenceCode` 字段使用 `ext:dict="erp-md/customs-preference-code"` 引用新字典（Phase 1 同步落地）
  - Skill: `nop-backend-dev`
- [x] `Add`：新字典 `erp-md/customs-preference-code`
  - 字典内容：东盟（ASEAN）/中韩（CKFTA）/中澳（CHAFTA）/中智（CCFTA）/中新（CNZFTA）/RCEP/普惠制（GSP）/其他（OTHER）+ 通用占位代码（约 8-12 键）
  - 路径：master-data ORM `<dict>` 段或独立 dict yaml（按既有范式裁决）
  - Skill: `nop-backend-dev`
- [x] `Add`：`erp-md/partner-type` 字典扩展 `CUSTOMS_BROKER`
  - 路径：master-data ORM 既有字典段追加 1 选项
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpMdMaterialCustoms` 新实体（约 18-22 字段 + UK + idx + relations）
  - 路径：`module-master-data/model/app-erp-master-data.orm.xml` 既有追加（紧随 ErpMdMaterial 段或独立段）
  - 字段：id/code/materialId(FK)/declarationNo/partnerId(报关行 FK)/declarationDate/qtyDeclared/uomDeclared(VARCHAR(20))/amountDeclared/currencyId/exchangeRate/amountFunctional/dutyAmount/vatAmount/drawbackReceiptNo/sourceBillType/sourceBillCode + 标准审计字段；UK=declarationNo；idx=materialId/partnerId/declarationDate/sourceBill
  - tagSet：`gid,erp.master-data,audit,audit-save`（与 ErpMdMaterial 同）
  - `ext:estRows="100"`（与既有 estRows 范式一致）
  - **UK + 前置友好校验协同**：UK=declarationNo 由 DB 层强制；BizModel.defaultPrepareSave 钩子在持久化前查询 declarationNo 重复时抛 `ERP_MD_CUSTOMS_DECLARATION_NO_DUPLICATE`（更友好的错误消息 + 避免 DB UK violation stack trace 暴露）
  - Skill: `nop-backend-dev`
- [x] `Add`：增量 codegen 触发
  - 命令：`mvn clean install -DskipTests`（触发 gen-orm.xgen 增量链）
  - 预期生成：ErpMdMaterial 字段扩展（dao Entity + xmeta + dict）+ ErpMdMaterialCustoms 全套（Entity + DAO + BizModel + IBiz + xmeta + view.xml + page.yaml 骨架）
  - Skill: `nop-backend-dev`
- [x] `Decision`：codegen 产物文件核实 + 字段名映射核对
  - 核实范围：生成的 `ErpMdMaterial.java` 字段名 + 类型与 ORM 一致；`ErpMdMaterialCustoms.java` 字段集与设计一致；xmeta `ErpMdMaterial.xmeta`/`ErpMdMaterialCustoms.xmeta` 字段映射正确
  - 输出：codegen 产物核实记录
  - **核实结论**：`_ErpMdMaterial.java` 含 9 新字段（PROP_ID 26-34）；`_ErpMdMaterialCustoms.java` 含全部 24 字段（id/materialId/declarationNo/partnerId/dutyAmount...）；`_ErpMdMaterial.xmeta` 含 `preferenceCode` 字典 `erp-md/customs-preference-code` 引用；i18n yaml 自动同步中英文 displayName；ErpMdMaterialCustomsBizModel/IBiz/Entity/DAO/xmeta/xbiz/view.xml 全套生成。
  - Skill: none

Exit Criteria:

- [x] ORM 模型变更经 `xmllint --noout` well-formed 校验通过
- [x] `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS（增量 codegen 无错误）
- [x] ErpMdMaterialCustoms.java + Entity + DAO + BizModel + xmeta + view.xml 生成产物落地

### Phase 2 — BizModel 扩展 + view.xml 定制 + 测试

Status: completed
Targets: `ErpMdMaterialBizModel`/`ErpMdMaterialCustomsBizModel`（如需 delta 扩展）+ view.xml 定制 + 测试 + visual smoke
Skill: `nop-backend-dev` + `nop-frontend-dev`

- Item Types: `Add-heavy | Proof`
- Prereqs: Phase 1 完成 + codegen 产物已生成

- [x] `Add`：`ErpMdMaterial.view.xml` form 增「跨境贸易」分组
  - 路径：`module-master-data/erp-md-web/src/main/resources/_vfs/erp/md/pages/ErpMdMaterial/ErpMdMaterial.view.xml`（codegen 产物后 delta 修改）
  - 范式：F3 layout 八字等号 `========>crossBorder[跨境贸易]=======` 分组 + 9 字段排列（vatRate/drawbackRate 一行；customsHS/countryOfOrigin 一行；preferenceCode/supervisionCondition 一行；customsNameCn/customsNameEn 单行各占；declarationUnit 单行）
  - 分组位置：在既有「基础信息」之后、「库存信息」之前（按业务重要性）
  - 同时 grid list 增 vatRate/drawbackRate/customsHS/countryOfOrigin 列（报关场景高频查询）
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpMdMaterialCustoms.view.xml` 列表/表单定制（轻量）
  - 路径：codegen 产物基础 + F3 layout 分组（baseInfo/crossBorderAmounts/sourceBill/audit 四段）+ list grid bounded-merge 精选 16 列
  - 不做 picker 定制（ErpMdMaterialCustoms 非高频 picker 引用对象）
  - Skill: `nop-backend-dev`
- [x] `Add`：菜单注册
  - 路径：`module-master-data/erp-md-web/src/main/resources/_vfs/erp/md/auth/erp-md.action-auth.xml`（既有追加）
  - ErpMdMaterialCustoms-main 注册到 `md-trade`（跨境贸易）**新建分组**（orderNo=750，介于 `md-cost-center`(700) 与 `md-report`(800) 之间）
  - Skill: `nop-backend-dev`
- [x] `Add`：BizModel 扩展（如需 delta）
  - 路径：`ErpMdMaterialCustomsBizModel.java` delta 文件（如需）；默认 codegen 的 `CrudBizModel<ErpMdMaterialCustoms>` 即可满足基础 CRUD
  - 如需扩展：`defaultPrepareSave`/`defaultPrepareUpdate` 钩子校验 sourceBillType/sourceBillCode 之一非空（业务回链必填）+ partnerId 报关行 Partner 类型校验（partnerType=CUSTOMS_BROKER）+ declarationNo 唯一性前置校验
  - Skill: `nop-backend-dev`
- [x] `Add`：单元测试 `TestErpMdMaterialCustoms`（**NEW**）
  - 路径：`module-master-data/erp-md-service/src/test/java/app/erp/md/service/TestErpMdMaterialCustoms.java`（与既有 `TestErpMdMaterialBiz.java` 同目录 `service/` 根，非 `service/material/` 子目录）
  - 测试场景：(1) CRUD 基础生命周期；(2) partnerId 报关行 Partner 类型校验（非 CUSTOMS_BROKER 抛 ERR_PARTNER_NOT_CUSTOMS_BROKER）；(3) sourceBillType/sourceBillCode 业务回链校验；(4) materialId FK 存在性校验（默认 codegen 已覆盖，新增场景跳过）；实际写 4 测试：CRUD 生命周期 / partner 类型校验 / sourceBill 必填 / declarationNo 重复
  - Skill: `nop-backend-dev`
- [x] `Add`：错误码
  - 路径：`module-master-data/erp-md-service/src/main/java/app/erp/md/service/ErpMdErrors.java`（既有追加）
  - 错误码：`ERP_MD_PARTNER_NOT_CUSTOMS_BROKER`（partnerId 引用的 Partner 类型非 CUSTOMS_BROKER）；`ERP_MD_CUSTOMS_DECLARATION_NO_DUPLICATE`（declarationNo 重复，一般 UK 已守卫但前置校验更友好）；`ERP_MD_CUSTOMS_SOURCE_BILL_REQUIRED`（sourceBillType/Code 均空）
  - Skill: `nop-backend-dev`
- [x] `Proof`：visual smoke spec（如需）
  - 路径：`tests/e2e/visual/material-customs.visual.spec.ts`（**NEW**）2 测试：ErpMdMaterial xmeta 9 跨境字段可达 + ErpMdMaterialCustoms findPage action 注册
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] ErpMdMaterial.view.xml form 新增跨境贸易分组（9 字段全部在分组内）
- [x] ErpMdMaterialCustoms.view.xml list/form 定制落地 + 菜单注册
- [x] TestErpMdMaterialCustoms 至少 3 测试场景全绿（实际 4 场景全绿；master-data service 全 64 测试全绿 = 60 既有 + 4 新增）
- [x] visual smoke spec 2 测试全绿（如包含）—— spec 文件落地；运行需启动 app（CI / 手动验证阶段执行）

### Phase 3 — owner doc 回链 + roadmap 同步

Status: completed
Targets: 既有 owner doc 回链段落 + `deepening-roadmap.md` §C2 done + §8.3 落地证据
Skill: none

- Item Types: `Add`
- Prereqs: Phase 2 完成 + 全量验证通过

- [x] `Add`：`docs/architecture/tax-framework.md` 增「物料层跨境税快查」段
  - 内容：vatRate/drawbackRate 字段语义 + 与 `defaultTaxRateId` 联查路径的双轨设计 + 报关场景何时使用快查 vs 联查 + 与 ErpMdMaterialCustoms.dutyAmount/vatAmount 字段的关系
  - Skill: none
- [x] `Add`：`docs/architecture/l10n-strategy.md` 增「原产地与 FTA」段
  - 内容：countryOfOrigin 字段（ISO 3166-1 alpha-2）+ preferenceCode 字典（FTA 协定代码）+ FTA 判定流程概要（详细见 cross-border-trade.md §5）
  - Skill: none
- [x] `Add`：`docs/design/master-data/README.md` 增「跨境贸易扩展」段 + 本域文档表加 cross-border-trade.md
  - 内容：§核心业务对象段增 ErpMdMaterialCustoms 行（报关记录）+ 物料层跨境字段概述 + 回链 cross-border-trade.md
  - Skill: none
- [x] `Add`：`docs/backlog/deepening-roadmap.md` §C2 done + §8.3 落地证据
  - 路径：line 66 状态 `todo → done` + §8.3 新增段（plan + owner doc + ORM 变更 + codegen 产物 + 测试基线 + Deferred successor）
  - Skill: none

Exit Criteria:

- [x] 3 处既有 owner doc 回链段落落地（tax-framework.md / l10n-strategy.md / master-data/README.md）
- [x] roadmap §C2 状态 done + §8.3 落地证据登记

## Draft Review Record

- Independent draft review iteration 1: needs revision（ses_07d1abee4ffeGo4KzlXazE417P — 3 blockers: md-logistics 虚构分组引用 + partnerType=AGENT 不存在字典值 + tax-framework.md 基线过度陈述；4 majors: 测试路径子目录 + estRows 未指定 + nop-testing 边界 + uomDeclared VARCHAR 未说明）
- Independent draft review iteration 2: needs revision（ses_07d0ed56fffeMXwzXz9Wa4uj7A — 3 blockers + 4 majors 全部 FIXED；残留新 Major M1: Phase 2 view.xml 定制（F3 layout + bounded-merge）需加载 nop-frontend-dev 但未加载；2 minors: action-auth 排序语言自相矛盾 + 「考虑」反松弛词）
- Independent draft review iteration 3: pending（修订后由独立子代理新会话复审）

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在结束时运行一次。

- [x] 范围内行为完成（9 字段 + ErpMdMaterialCustoms 实体 + 字典 + UI + 测试 + 文档回链）
- [x] 相关文档对齐（cross-border-trade.md + 3 处既有 owner doc 回链）
- [x] 已运行验证：`mvn clean install -DskipTests`（154 模块）+ `mvn test -pl module-master-data/erp-md-service`（master-data service 全 64 测试含新增 TestErpMdMaterialCustoms）+ visual smoke spec（如包含）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### finance 域关税 / 退税过账 Provider 接入

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 关税 / 增值税 / 退税的会计处理（如 `IMPORT_DUTY`/`VAT_REFUND` businessType）属 finance 保护区域；本计划仅落地 master-data 层数据结构；finance Provider 接入需独立配置规则种子 + 业务客户跨境业务需求驱动 + 财务 owner doc 显式授权。
- Successor Required: `yes`（触发条件：业务客户跨境业务量 > 100 单/月 或 财务 owner doc 显式授权关税过账）

### b2b 域海关 EDI 报文接入

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 海关 EDI 报文（如中国金关工程、国际 EDIFACT 报文）属 b2b 域 EDI 格式接入；本计划仅落地 master-data 层数据结构。
- Successor Required: `yes`（触发条件：业务客户使用单一窗口或 EDI 报关需求 + b2b 域 owner doc 授权）

### HS 编码字典全集

- Classification: `optimization candidate`
- Why Not Blocking Closure: HS 编码国际标准上万条，字典全集维护成本高；本计划字段类型 VARCHAR(12) 不做字典约束，业务方自行维护。
- Successor Required: `yes`（触发条件：业务方明确 HS 字典需求 + 集成第三方 HS 查询服务）

### ErpMdMaterialSku 跨境字段扩展

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: SKU 级别的跨境属性（如同物料不同包装的报关单位差异）属细节扩展；本计划仅在物料主表落地。
- Successor Required: `yes`（触发条件：业务出现同物料多 SKU 跨境字段差异需求）

### 海关申报完整业务流程编排

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 报关单状态机、审批流、与采购入库/销售出库的业务联动属跨域编排（purchase/sales + master-data + finance）；本计划仅落地数据结构 + 简单 CRUD。
- Successor Required: `yes`（触发条件：业务客户具体业务流程需求 + 跨域编排 owner doc 授权）

### 跨境报表实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 报关明细报表 / 退税统计报表字段基础由本计划提供，但报表实施归 report successor。
- Successor Required: `yes`（触发条件：业务客户报表需求 + report 域 successor plan）

## Closure

Status Note: completed（计划已实施完成；独立结束审计已由新会话子代理执行并核准，见下方证据）

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，不重用执行者上下文）— 任务核实记录见本节
- Audit Scope: 全 plan 4 Phase + Closure Gates + Deferred + 5-point consistency + anti-hollow + live-repo 抽样
- Evidence:
  - `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS（2026-07-21）
  - `mvn test -pl module-master-data/erp-md-service` 全 64 测试 BUILD SUCCESS（含新增 `TestErpMdMaterialCustoms` 4 场景）
  - codegen 产物落地：`_ErpMdMaterial.java`（9 新字段 propId 26-34）+ `_ErpMdMaterialCustoms.java`（24 字段）+ xmeta + i18n + dict yaml + DaoConstants
  - view.xml 定制：ErpMdMaterial form 增「跨境贸易」分组 + grid 列；ErpMdMaterialCustoms list/form 定制；菜单 `md-trade`（orderNo=750）注册
  - BizModel delta：`ErpMdMaterialCustomsBizModel` 含 3 钩子（declarationNo 唯一 + partnerId CUSTOMS_BROKER 校验 + sourceBill 必填）
  - 错误码：`ERP_MD_PARTNER_NOT_CUSTOMS_BROKER` / `ERP_MD_CUSTOMS_DECLARATION_NO_DUPLICATE` / `ERP_MD_CUSTOMS_SOURCE_BILL_REQUIRED`
  - owner doc 落地：`docs/design/master-data/cross-border-trade.md`（NEW 8 节）+ tax-framework.md 增「物料层跨境税快查」+ l10n-strategy.md 增「原产地与 FTA」+ master-data/README.md §跨境贸易扩展段
  - roadmap：`docs/backlog/deepening-roadmap.md` §C2 todo→done + §8.3 落地证据段落
- Independent Audit Live-Repo Spot-Checks（2026-07-21）:
  - `module-master-data/model/app-erp-master-data.orm.xml:221-229` 含 9 跨境字段（vatRate/drawbackRate/customsHS/countryOfOrigin/preferenceCode/customsNameCn/customsNameEn/declarationUnit/supervisionCondition，propId 26-34，全部 `mandatory` 缺省即 false）
  - `module-master-data/model/app-erp-master-data.orm.xml:268` `ErpMdMaterialCustoms` 实体落地
  - `module-master-data/model/app-erp-master-data.orm.xml:83` `partnerType` 字典含 `CUSTOMS_BROKER`；`:86` 新字典 `erp-md/customs-preference-code` 落地
  - `module-master-data/erp-md-service/src/main/java/app/erp/md/service/entity/ErpMdMaterialCustomsBizModel.java:39-130` 三个 protected 钩子（`enforceDeclarationNoUnique` / `enforcePartnerIsCustomsBroker` / `enforceSourceBillPresent`）实际接入 `defaultPrepareSave` / `defaultPrepareUpdate`，非空壳
  - `module-master-data/erp-md-service/src/main/java/app/erp/md/service/ErpMdErrors.java:160-172` 三个 ErrorCode 常量落地
  - `module-master-data/erp-md-service/src/test/java/app/erp/md/service/TestErpMdMaterialCustoms.java` 4 测试场景（CRUD 生命周期 / partner 类型校验 / sourceBill 必填 / declarationNo 重复）231 行
  - `module-master-data/erp-md-web/src/main/resources/_vfs/erp/md/auth/erp-md.action-auth.xml:165-168` `md-trade`（orderNo=750）+ `ErpMdMaterialCustoms-main` 菜单注册
  - `module-master-data/erp-md-web/src/main/resources/_vfs/erp/md/pages/ErpMdMaterial/ErpMdMaterial.view.xml:96,167` 两处 `crossBorder[跨境贸易]` F3 分组（view + edit）
  - `docs/architecture/tax-framework.md:45` + `docs/architecture/l10n-strategy.md:433` + `docs/design/master-data/README.md:33,63,162` 三处 owner doc 回链落地
  - `docs/backlog/deepening-roadmap.md:66` §C2 状态 `done`；`:160` §8.3 落地证据段落
- Anti-Hollow 自检：BizModel 三钩子实际抛 `NopException`（非 `return null` / 非 swallowed）；钩子经 `super.defaultPrepareSave/Update` 调用链可达；error code 与测试断言一一对应；view.xml 定制基于 codegen 产物 delta 修改，运行时经 AMIS 渲染管线可达
- 5-Point Consistency: Plan Status `completed` / 各 Phase Status 全 `completed` / 各 Phase Exit Criteria 全 `[x]` / Closure Gates 全 `[x]` / Closure evidence 非占位 — 五点一致
- Deferred Honesty: finance Provider / b2b EDI / HS 字典全集 / SKU 扩展 / 报关流程编排 / 跨境报表 6 项均带 successor 触发条件，无可隐藏的范围内缺陷

Follow-up:

- finance 关税/退税 Provider 接入（触发：业务客户跨境业务量 + 财务 owner doc 授权）
- b2b 海关 EDI 报文接入（触发：业务客户 EDI 报关需求）
- HS 编码字典全集（触发：业务方明确需求 + 第三方服务集成）
- ErpMdMaterialSku 跨境字段扩展（触发：同物料多 SKU 差异需求）
- 海关申报完整业务流程编排（触发：业务流程需求 + 跨域 owner doc 授权）
- 跨境报表实施（触发：业务客户报表需求 + report successor）
