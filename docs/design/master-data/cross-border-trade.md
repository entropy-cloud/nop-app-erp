# 跨境贸易扩展（Cross-Border Trade Extensions）

> Owner Doc for `deepening-roadmap.md` §C2。
> Plan: `docs/plans/2026-07-21-1206-1-master-data-cross-border-trade-extensions.md`。
> 相关：`README.md`（域概览）、`unified-party-identity.md`（C1 Party 抽象，本计划复用 `IErpPartyBiz` 但不改其契约）、`docs/architecture/tax-framework.md`（税务框架，本计划「物料层跨境税快查」段回链）、`docs/architecture/l10n-strategy.md`（本地化策略，本计划「原产地与 FTA」段回链）。

## 1. 目的与范围

本 owner doc 定义 nop-app-erp 主数据域的**跨境贸易字段扩展**业务语义：在物料层落地跨境报关高频查询字段（税率/HS 编码/原产地等）+ 新建 per-transaction 报关记录实体（`ErpMdMaterialCustoms`）+ 字典扩展（`CUSTOMS_BROKER` Partner 类型、`customs-preference-code` FTA 协定代码）。

### 边界

| 范围 | 归属 |
|------|------|
| 物料层 9 个跨境字段 + `ErpMdMaterialCustoms` 实体 + 字典扩展 | **本计划（C2）** |
| Partner 抽象 / 跨实体检索（C1） | C1 plan `2026-07-21-0827-2` —— 本计划复用 `IErpPartyBiz` 抽象，**不改其契约**；新增 Partner 类型字典值 `CUSTOMS_BROKER` 仅追加选项，不动接口 |
| 海关 EDI 报文 / 单一窗口接入（b2b 域） | b2b successor（触发：业务客户 EDI 报关需求 + b2b 域 owner doc 授权） |
| 关税/增值税/退税过账 Provider（finance 域） | finance successor（触发：业务客户跨境业务量 > 阈值 + 财务 owner doc 显式授权） |
| 报关单状态机 / 审批流 / 跨域编排 | successor plan（触发：业务客户具体业务流程需求 + 跨域编排 owner doc 授权） |
| HS 编码字典全集 / 第三方 HS 查询服务集成 | successor（触发：业务方明确需求 + 第三方服务集成） |
| 报关明细报表 / 退税统计报表 | report successor（字段基础由本计划提供，报表实施归 report 域） |
| 关税计算引擎（含反倾销税/报复性关税） | finance/tax successor |

> 关键约束：`ErpMdMaterial.defaultTaxRateId`（既有 FK→`ErpMdTaxRate`）保留作详细税率配置入口；本计划新增的 `vatRate`/`drawbackRate` 是**冗余快查字段**，二者并行，业务约定何时使用快查 vs 联查（见 §6 / `tax-framework.md` 物料层跨境税快查段）。

## 2. 物料层跨境字段扩展

`ErpMdMaterial` 新增 9 个跨境贸易快查字段：`vatRate`（增值税率）、`drawbackRate`（退税率）、`customsHS`（海关 HS 编码）、`countryOfOrigin`（原产地 ISO 3166-1 alpha-2）、`preferenceCode`（FTA 优惠协定代码）、`customsNameCn`/`customsNameEn`（报关中/英文名）、`declarationUnit`（申报计量单位）、`supervisionCondition`（监管条件代码）。字段类型、精度、字典码与 UK/Index 约束以 `module-master-data/model/app-erp-master-data.orm.xml` 为权威源。

**字典化决策（业务语义）**：

- `preferenceCode` 字典化（FTA 协定数量有限约 15-20 个且变更慢，适合字典化），字典 `erp-md/customs-preference-code`。
- `customsHS`/`countryOfOrigin`/`supervisionCondition`/`declarationUnit` **不**字典化：HS 编码全集上万条、监管条件 100+ 且频繁更新、国家全集 200+ 由 ISO 维护、海关法定单位与内部单位字典解耦——均用自由文本字段由业务方维护或集成第三方服务。

**开源对齐**：`vatRate`/`drawbackRate`/`customsHS`/`countryOfOrigin`/`customsNameCn`/`customsNameEn`/`declarationUnit` 对齐 Wimoor 跨境字段集；`customsHS` 同时对齐 OFBiz/ERP5；`preferenceCode`/`supervisionCondition` 为本计划新增。

**统一约束**：

- 全部字段向后兼容（默认 null，非 mandatory），不影响既有 INSERT/UPDATE。
- `ui:show` 按字段语义：`vatRate`/`drawbackRate` 在 grid 列表显示（报关场景高频查询）；`customsNameCn`/`customsNameEn` 仅在 form 显示（避免 grid 列过宽）。

### 字段冗余 vs 联查权衡（vatRate/drawbackRate）

- **风险**：字段冗余在物料主表，可能多场景下与 `ErpMdTaxRate` 不一致（如税率变更后物料层未同步）。
- **缓解**：默认显示 `defaultTaxRate.rate` 联查值；`vatRate`/`drawbackRate` 字段仅在**报关场景显式覆盖**（业务约定 + 文档化于 `tax-framework.md` 物料层跨境税快查段）。

### 多账套/多公司隔离决策

- **候选 A**（采纳）：物料主数据层**不**按 `orgId` 隔离（同物料在多公司用同 `countryOfOrigin`）。
- **候选 B**（拒绝）：按 orgId 隔离（同一物料在出口公司 vs 内销公司可能不同原产地认定）。
- **特殊场景**：候选 B 的需求由 `ErpMdMaterialCustoms` per-transaction 实体覆盖（per-transaction 级别记录原产地）。

## 3. ErpMdMaterialCustoms 实体设计

per-transaction 报关记录实体，每次报关独立记录报关单号、报关行、报关日期、申报数量/金额、关税/增值税金额、退税收据号、业务单据回链。

### 字段

`ErpMdMaterialCustoms` 定义约 20 个业务字段，涵盖：报关记录编码（`code`）、物料回链（`materialId`→ErpMdMaterial）、报关单号（`declarationNo`，海关分配全局唯一）、报关行回链（`partnerId`→ErpMdPartner，须为 `CUSTOMS_BROKER` 类型）、申报数量/单位/金额/币种/汇率/本位币金额、关税/增值税金额（`dutyAmount`/`vatAmount`，由 finance successor 填充）、退税收据号（`drawbackReceiptNo`）、业务单据回链（`sourceBillType`/`sourceBillCode`，二者至少一个非空）+ 标准审计字段。字段类型、精度、mandatory、字典码、UK 与 Index 约束以 `module-master-data/model/app-erp-master-data.orm.xml` 为权威源。

### Relations

- `material`（to-one）→ `ErpMdMaterial`
- `partner`（to-one）→ `ErpMdPartner`（报关行，须 `CUSTOMS_BROKER` 类型）
- `currency`（to-one）→ `ErpMdCurrency`

### 约束与查询索引

- `declarationNo` 全局唯一（UK，DB 层强制）；
- 按 `materialId`/`partnerId`/`declarationDate` 单字段索引 + `sourceBillType`+`sourceBillCode` 复合索引支持业务单据回链查询；
- `ext:estRows` 按业务客户报关频次估算（日均 < 100 单/跨境客户）；
- 完整 UK/Index/estRows 定义见 orm.xml。

### UK + 前置友好校验协同

- **UK=declarationNo** 由 DB 层强制；
- **BizModel.defaultPrepareSave 钩子**在持久化前查询 declarationNo 重复时抛 `ERP_MD_CUSTOMS_DECLARATION_NO_DUPLICATE`（更友好的错误消息 + 避免 DB UK violation stack trace 暴露）；
- **sourceBillType/sourceBillCode 校验**：BizModel.defaultPrepareSave 校验二者之一非空（业务回链必填）；
- **partnerId 报关行校验**：BizModel.defaultPrepareSave 校验 partnerId 引用的 Partner 类型必须为 `CUSTOMS_BROKER`（非此类型抛 `ERP_MD_PARTNER_NOT_CUSTOMS_BROKER`）。

### 浏览器层验证

3 个保存校验钩子（报关单号唯一 / 报关行类型须 CUSTOMS_BROKER / 业务单据回链必填）经 `defaultPrepareSave`/`defaultPrepareUpdate` → `validateOnPersist` 统一触发，覆盖正路径 + 3 守卫拒绝路径 + update 自身排除范式。测试与性能基线见 `docs/testing/`。

## 4. 报关场景工作流

### 业务流程（语义描述，非状态机实施）

1. **业务单据触发**：业务方从采购入库 / 销售出库等单据发起报关需求，回链记录在 `sourceBillType`/`sourceBillCode`。
2. **报关行选择**：选择已登记为 `CUSTOMS_BROKER` 的 Partner 作为报关行（partnerId）。
3. **申报信息录入**：申报数量（`qtyDeclared`）、申报计量单位（`uomDeclared`）、申报金额（`amountDeclared`）、申报币种（`currencyId`）、报关日汇率（`exchangeRate`）、本位币金额（`amountFunctional`）。
4. **报关单号回填**：海关分配的报关单号（`declarationNo`）回填，UK 强制全局唯一。
5. **关税/增值税金额记录**：`dutyAmount`/`vatAmount`（由 finance successor 关税计算引擎填充；本计划仅落地字段）。
6. **退税收据号回填**：税务部门分配的退税收据号（`drawbackReceiptNo`）。

### 状态字段说明（Non-Goal：状态机实施）

本计划**不**实施报关单状态机（如 DRAFT→DECLARED→CLEARED→DREW_BACK）。`ErpMdMaterialCustoms` 仅记录已完成的报关数据快照。状态机/审批流/与采购入库销售出库的业务联动属跨域编排，归 successor plan（触发：业务客户具体业务流程需求 + 跨域编排 owner doc 授权）。

## 5. FTA 判定流程

### 原产地（countryOfOrigin）+ 优惠协定代码（preferenceCode）配合

FTA（Free Trade Agreement）优惠协定判定的字段基础：

1. **原产地录入**：物料主表 `countryOfOrigin`（ISO 3166-1 alpha-2）记录原产国。
2. **优惠协定选择**：报关时根据原产国 + 进口国选择适用的 FTA 协定代码（`preferenceCode` 字典值：`ASEAN`/`CKFTA`/`CHAFTA`/...）。
3. **税率优惠应用**：FTA 协定生效时，适用协定优惠税率（如东盟协定下中国→东盟成员国零关税）。

### 判定流程概要

```
物料 countryOfOrigin + 报关单 进口国/出口国
  ↓
FTA 协定适用性判定（人工或第三方服务）
  ↓
选择 preferenceCode（如 ASEAN）
  ↓
适用协定优惠税率
  ↓
记录在 ErpMdMaterialCustoms（per-transaction，可能因报关场景不同而override物料层preferenceCode）
```

> **详细 FTA 判定算法**（含原产地认定规则、增值比例计算、直接运输规则等）属关税计算引擎，归 finance/tax successor。本计划仅提供字段基础。

## 6. 与既有 owner doc 关系

### `docs/architecture/tax-framework.md` —— 物料层跨境税快查段（Phase 3 增量）

- `vatRate`/`drawbackRate` 字段语义；
- 与 `defaultTaxRateId` 联查路径的双轨设计；
- 报关场景何时使用快查 vs 联查；
- 与 `ErpMdMaterialCustoms.dutyAmount`/`vatAmount` 字段的关系。

### `docs/architecture/l10n-strategy.md` —— 原产地与 FTA 段（Phase 3 增量）

- `countryOfOrigin` 字段（ISO 3166-1 alpha-2）；
- `preferenceCode` 字典（FTA 协定代码）；
- FTA 判定流程概要（详细见本 doc §5）。

### `docs/design/master-data/README.md` —— 跨境贸易扩展段（Phase 3 增量）

- §核心业务对象段增 `ErpMdMaterialCustoms` 行（报关记录）；
- 物料层跨境字段概述；
- 回链本 doc。

### C1 关系（`unified-party-identity.md`）

- 本计划复用 `IErpPartyBiz` 抽象（Partner 抽象基础），但**不改 C1 接口契约**；
- 新增 Partner 类型字典值 `CUSTOMS_BROKER`（仅追加选项到 `erp-md/partner-type` 字典，不动 `IErpPartyBiz` 接口签名 / `PartyRef` DTO / SPI 端口）。

## 7. 反模式自检表

| 反模式 | 正确做法 |
|--------|---------|
| 直接修改 C1 `IErpPartyBiz` 接口签名或 `PartyRef` DTO | 仅在 `erp-md/partner-type` 字典追加 `CUSTOMS_BROKER` 选项 |
| 在 `ErpMdMaterialCustoms.BizModel` 中跨域调用 finance 关税计算 | 本计划 Non-Goal：finance Provider 接入归 successor；BizModel 仅校验 sourceBill/partnerType + CRUD |
| 在 `ErpMdMaterialCustoms` 实施 status 状态机字段 | 本计划 Non-Goal：状态机/审批流归 successor plan |
| `uomDeclared` 用 FK→`ErpMdUoM` | 海关法定单位与内部单位字典解耦，用自由文本字段（见 orm.xml） |
| `customsHS`/`supervisionCondition` 字典化 | 全集过大（HS 上万条/监管条件 100+ 频繁更新），用自由文本字段，业务方自行维护 |
| `vatRate`/`drawbackRate` 默认显示覆盖 `defaultTaxRate.rate` 联查值 | 默认显示联查值，本字段仅在报关场景显式覆盖 |
| `countryOfOrigin` 按 orgId 隔离 | 候选 A：物料主数据层不隔离；per-transaction 差异由 `ErpMdMaterialCustoms` 覆盖 |
| 在 `ErpMdMaterial` view.xml grid 列显示 `customsNameCn`/`customsNameEn` | grid 列过宽；仅 form 分组显示 |
| `ErpMdMaterialCustoms.BizModel.defaultPrepareSave` 不校验 partnerId Partner 类型 | 必须校验 `partnerType=CUSTOMS_BROKER`，抛 `ERP_MD_PARTNER_NOT_CUSTOMS_BROKER` |
| 不前置校验 declarationNo 重复（依赖 DB UK violation） | BizModel 钩子前置校验抛友好错误 `ERP_MD_CUSTOMS_DECLARATION_NO_DUPLICATE` |
| 在本计划接入 finance 关税过账 Provider | 本计划 Non-Goal；successor 触发条件：业务客户跨境业务量 > 100 单/月 或 财务 owner doc 显式授权 |

## 8. 落地证据

（本计划已完成，见 `docs/plans/2026-07-21-1206-1-master-data-cross-border-trade-extensions.md`）

- Plan：`docs/plans/2026-07-21-1206-1-master-data-cross-border-trade-extensions.md`（4 Phase 全 done）
- Owner Doc：本文件（8 节完整）
- ORM 变更：`ErpMdMaterial` 增 9 跨境字段 + 新建 `ErpMdMaterialCustoms` 实体 + `erp-md/customs-preference-code` 字典 + `erp-md/partner-type` 增 `CUSTOMS_BROKER`（权威源见 orm.xml）
- 测试与性能基线见 `docs/testing/`。
- Deferred successor：finance 关税/退税 Provider 接入 / b2b 海关 EDI 报文 / HS 编码字典全集 / ErpMdMaterialSku 跨境字段 / 海关申报完整业务流程编排 / 跨境报表实施 / 关税计算引擎
