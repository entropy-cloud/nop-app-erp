# 主数据域（master-data）

## 目的

说明主数据域的业务语义、核心对象、状态机与跨域协作。主数据是整个 ERP 的基础数据底座，为采购、销售、库存、财务四个业务域提供共享主数据。

## 边界

- 本域负责：物料/SKU、往来单位、仓库/库位、计量单位、币种/汇率、科目表/科目等基础数据的维护。
- 本域不负责：业务单据（订单/出入库/发票/凭证）—— 属于各业务域。
- 本域不负责：库存余量（库存域）、应收应付余额（财务域）。
- 持久化字段、字典、关系以 `model/app-erp-master-data.orm.xml` 为准。
- 跨域引用规则见 `../domain-design-guidelines.md`。

## 工程与模型

| 项 | 值 |
|----|----|
| Maven 工程 | `app-erp-master-data` |
| appName | `erp-md` (两级) |
| 权威模型 | `model/app-erp-master-data.orm.xml` |
| 实体包 | `app.erp.md.dao.entity` |
| 表前缀 | `erp_md_` |
| 类名前缀 | `ErpMd*` |
| 字典命名空间 | `erp-md/*` |

## 核心业务对象

| 对象 | 业务含义 | 被引用方 |
|------|----------|----------|
| 物料（Material） | ERP 管理对象的最小主数据：商品、原材料、产成品、服务 | inventory / purchase / sales / finance |
| SKU | 物料 × 包装单位 × 条码 的唯一可销售/可库存单元，承载多档价格 | inventory / purchase / sales |
| 物料报关记录（Material Customs） | per-transaction 报关记录：报关单号/报关行/申报数量金额/关税增值税/退税收据号 | （独立 CRUD，归 master-data 跨境贸易子域）|
| 往来单位（Partner） | 客户与供应商统一主数据 | purchase / sales / finance |
| 仓库（Warehouse） | 物理或逻辑库存地点 | inventory / purchase / sales |
| 库位（Location） | 仓库内的细分储位 | inventory |
| 计量单位（UoM） | 物料计量单位，分属单位组 | inventory / purchase / sales |
| 单位换算（UoM Conversion） | 同单位组内单位间的换算系数 | inventory / purchase / sales |
| 币种（Currency） | 结算货币主数据 | purchase / sales / finance |
| 汇率（Exchange Rate） | 币种间在某日的换算比率 | purchase / sales / finance |
| 会计科目表（COA） | 某账套的科目体系 | finance |
| 会计科目（Account） | 树形结构，父子层级 + 段值编码 | finance |
| 结算方式 / 支付方式 | 收付款结算与支付手段配置 | purchase / sales / finance |

### 物料与 SKU 关系

物料承载基础属性（编码、名称、规格、型号、品牌、分类、基本单位、保质期规则、批次/序列号开关）。一个物料可有多个 SKU，每个 SKU 是"物料 × 包装单位 × 条码"的唯一单元，承载多档价格（采购价/批发价/零售价/最低价）与默认单位标志。业务单据引用 SKU，不直接引用物料。

### 往来单位

往来单位是客户与供应商的统一主数据，一个单位可同时是客户和供应商（类型区分）。承载开票信息（税号、银行、账号、税率）、信用额度、账期。期初与累计应收应付余额由财务域维护，本域只承载主数据。

### 统一 Party 抽象

`Partner`（外部 Party）+ `Employee`（内部人员 Party）+ `Organization`（内部组织 Party）共享 `code/name/status` 公共字段，跨实体检索经统一抽象入口（详见 `unified-party-identity.md`）：

- `IErpPartyBiz`（`module-master-data/erp-md-dao/src/main/java/app/erp/md/biz/`）— 跨域消费者 `@Inject` 入口，暴露 `findParties(keyword, partyTypes, limit)` / `getParty(partyType, partyId)` / `findReferences(partyType, partyId)`。
- `PartyRef` DTO + `ErpPartyType` enum — 统一投影结构，容忍字段异构（Organization 无 phone/email 投影为 null）。
- `party-search/main.picker.page.yaml`（首例手写 picker.page.yaml）— 联合 picker，跨实体关键字检索 + onEvent.setValue 回填。

**约定**：当且仅当非实体 BizModel 有跨工程 `@Inject` 消费者时才暴露 `IErp*Biz` 接口；纯 UI 入口（如 Dashboard）保持无接口。

### 跨境贸易扩展（C2）

物料主表（`ErpMdMaterial`）含 9 个跨境贸易字段（详见 `cross-border-trade.md`）：

- **税率快查**：`vatRate`（增值税率）/ `drawbackRate`（退税率）—— 冗余于 `defaultTaxRateId→ErpMdTaxRate.rate`，报关场景高频查询。
- **海关申报标识**：`customsHS`（HS 编码，VARCHAR(12) 不字典化）/ `countryOfOrigin`（ISO 3166-1 alpha-2）/ `preferenceCode`（字典 `erp-md/customs-preference-code` FTA 协定代码）/ `supervisionCondition`（监管条件代码，自由 VARCHAR）。
- **报关双语名**：`customsNameCn` / `customsNameEn`。
- **申报单位**：`declarationUnit`（VARCHAR 而非 FK→ErpMdUoM，因海关法定单位字典与内部单位字典解耦）。

`ErpMdMaterialCustoms` per-transaction 报关记录实体（报关单号/报关行/申报数量金额/关税增值税/退税收据号/业务单据回链）。报关行 Partner 类型登记为 `CUSTOMS_BROKER`（`erp-md/partner-type` 字典值）。

> 关税/退税过账 Provider 接入归 finance successor；海关 EDI 报文接入归 b2b successor；状态机/审批流归跨域 successor。

### 日期范围有效性（C3）

汇率/税率/供应商准入资格等使用 `validFrom` + `validTo` 表达记录有效期的实体，遵循统一约定（详见 `../date-ranged-validity-pattern.md`）：

- **规范字段命名**：`validFrom` / `validTo`（双侧闭区间，含起止日）；新实体强制采用，历史 `effectiveFrom/effectiveTo` 命名变体不重命名（helper 归一化）
- **同维度互斥（MUTEX）策略**：同维度同一时刻至多 1 条有效记录。试点 3 实体：
  - `ErpMdExchangeRate`：维度 = `fromCurrencyId + toCurrencyId + rateType` → `defaultPrepareSave/Update` 钩子校验
  - `ErpMdTaxRate`：维度 = `taxType` → 同上
  - `ErpMdSupplierApproval`：维度 = `partnerId`（status != REJECTED 时；REJECTED 视为已废弃不占区间）→ 同上
- **可复用 helper**：`ErpDateRanges`（区间运算原语 contains/overlaps/effectiveOn/longestOverlap）+ `ErpDateRangeOverlapValidator`（互斥校验器）位于 `erp-md-service/daterange/`，跨域经 `app-erp-master-data-service` 依赖可达
- **错误码**：`ERR_MD_DATE_RANGE_OVERLAP`（`erp.err.md.date-range.overlap`）

> Follow-up 实体清单（17 个未试点）见 `../date-ranged-validity-pattern.md §10`，按业务驱动逐域接入。

### 组织类型与集团语义（A3）

`ErpMdOrganization` 经 `parentId` 自引用表达集团层级，`orgType`（字典 `erp-md/org-type`）区分组织类型（A3，plan 2026-07-22-1000-1）：

- **GROUP（集团）**：顶层组织（parentId=null），法人根的父节点。一个集团含多个 COMPANY。
- **COMPANY（公司）**：**法人根** —— 法定独立核算实体。跨法人调拨触发内部交易凭证（见 `../architecture/multi-company.md`）。
- **BRANCH/DEPARTMENT/WORKSHOP/STORE**：公司内部组织单元，不构成独立法人。

**法人根判定**（权威 `multi-company.md §组织模型`）：沿 `parentId` 链向上走，首个 orgType=COMPANY 的节点即该组织所属法人。仓库（`ErpMdWarehouse.orgId`）归属的组织决定调拨是否跨法人。

> 集团（`ErpMdCorporation`）未实体化 —— 仓库 grep 零命中；集团为顶层 orgType=GROUP 的 `ErpMdOrganization`。如未来需集团级独立属性（集团编码/合并币种），可补实体（successor）。

## 启用/停用（非状态机）

主数据实体（物料、SKU、往来单位、仓库、库位、计量单位、币种、科目）采用简单的"启用/停用"二态，**不是工作流状态机**——主数据没有多步业务流转，只有"是否可被新业务单据引用"这一控制。

| 状态 | 含义 | 可被新单据引用 | 影响历史数据 |
|------|------|----------------|--------------|
| 启用（active） | 正常使用中 | 是 | — |
| 停用（inactive） | 已停用，不再用于新业务 | 否 | 历史单据与余额不受影响 |

- **启用 → 停用**：管理员执行停用，不强制校验是否被未完成单据引用（停用只影响"新引用"，不影响存量）。
- **停用 → 启用**：无前置条件，随时可恢复。
- **物理删除**：默认禁止删除被业务单据引用过的主数据；删除前必须校验引用关系，校验失败拒绝并提示引用方，建议改用停用。

各实体特殊约束：
- 物料停用后其下所有 SKU 一并不可被新单据引用；SKU 可独立于物料停用。
- 往来单位停用后不可被新采购/销售单据引用，但已有应收应付余额、未核销发票不受影响。停用不等于"结清"，余额清零属于财务域核销动作。
- 仓库停用后不可被新出入库单据引用，但库内已有库存仍可查询与调整；库位有库存时建议不可停用。
- 科目停用后不可被新凭证分录引用，已有余额保留；停用父科目不影响子科目，但不可在停用父科目下新增子科目。

主数据状态变化不主动通知其他域；其他域在创建新单据时通过 `IErpMd*Biz` 查询主数据状态，停用则拒绝引用。历史单据快照不受状态变化影响。

## 主数据关键属性审核规则

主数据的关键属性变更需经过审核流程，防止基础数据被误改影响业务：

### 可配置关键属性列表

| 主数据类型 | 关键属性 | 修改影响 |
|------------|----------|----------|
| 物料 | 基本单位、物料类型 | 影响库存换算、凭证生成 |
| SKU | 包装单位、换算系数 | 影响出入库数量换算 |
| 往来单位 | 税率、信用额度 | 影响发票税额、订单信用检查 |
| 仓库 | 仓库类型、所属公司 | 影响库存归属、组织隔离 |
| 会计科目 | 科目类型、余额方向 | 影响凭证校验、报表生成 |
| 币种 | 汇率 | 影响本位币折算 |

### 审核规则

- 关键属性变更需经过审批（审批流程复用 nop-wf）
- 变更前记录原值，变更后记录新值（审计日志）
- 被未完成单据引用的关键属性不允许修改（如被未结清发票引用的客户税率）
- 配置项：`erp-md.critical-attributes`（按主数据类型配置关键属性列表，可扩展）

> 非关键属性（如名称、规格、描述）的修改不需要审批，直接生效。

## 跨域协作

主数据是被引用方，不主动调用其他业务域。所有业务域通过 `IErpMd*Biz` 接口（本域在 `app-erp-master-data-dao` 暴露）查询主数据，不做 ORM 层跨工程 `refEntityName`。

| 主数据 | 引用方 | 引用方式 |
|--------|--------|----------|
| 物料 / SKU | inventory / purchase / sales / finance | 外键列 + `IErpMdMaterialBiz`/`IErpMdSkuBiz` |
| 往来单位 | purchase / sales / finance | 外键列 + `IErpMdPartnerBiz` |
| **统一 Party（Partner + Employee + Organization）** | **任意跨域消费者** | **`IErpPartyBiz.findParties` 跨实体检索（非实体 BizModel + 跨域 @Inject 入口）** |
| 仓库 / 库位 | inventory / purchase / sales | 外键列 + `IErpMdWarehouseBiz` |
| 计量单位 | inventory / purchase / sales | 外键列 + `IErpMdUoMBiz` |
| 币种 / 汇率 | purchase / sales / finance | 外键列 + `IErpMdCurrencyBiz` |
| 科目表 / 科目 | finance | 外键列 + `IErpMdAccountBiz` |

## 关键业务规则

1. **编码唯一性**：物料编码全局唯一；SKU 条码在同一物料内唯一；往来单位编码全局唯一；科目编码在科目表内唯一。
2. **删除约束**：主数据被业务单据引用后不可物理删除，建议采用停用。删除前必须校验引用关系。
3. **余额归属**：往来单位的应收应付余额由财务域写入，本域不直接维护。
4. **科目树约束**：科目树形结构的父子关系不可成环；停用父科目不影响子科目已有数据，但不可新增子科目。
5. **汇率匹配**：多币种单据按业务日期匹配汇率；缺失汇率时凭证过账报错而非静默使用默认值。
6. **单位换算**：业务单据数量按 SKU 包装单位录入，落账时按换算系数转为基本单位数量。

## 定时作业

主数据域定时作业登记于 `docs/architecture/job-scheduling.md` §3.4：

| 作业 | 频率 | 配置键 | 入口 |
|------|------|--------|------|
| `erp-md-data-sync`（主数据缓存刷新） | 每小时 | `erp-md.data-sync-cron`（默认 `0 0 * * * ?`） | 待实现 |
| `erp-md-exchange-rate-fetch`（汇率源拉取） | 每日 | — | 待实现（见 `exchange-rate-management.md:51`） |

> cron 接线（`scheduler.yaml` 注册）归 follow-up，触发条件见 `job-scheduling.md` §8。

## 本域文档

| 文档 | 职责 |
|------|------|
| `README.md`（本文件） | 域概览、核心对象、启停规则、跨域协作、关键规则 |
| `sku-multi-unit.md` | SKU 多单位多 barcode 设计（物料 SKU 分离、多单位换算、多档价格） |
| `unified-party-identity.md` | 统一 Party 身份查询（C1：跨 Partner/Employee/Organization 抽象 + `IErpPartyBiz` + 联合 picker） |
| `cross-border-trade.md` | 跨境贸易扩展（C2：物料层 9 字段 + `ErpMdMaterialCustoms` per-transaction 实体 + FTA + 报关行 Partner 类型） |
| `../date-ranged-validity-pattern.md`（跨域） | 日期范围有效性模式（C3：`validFrom/validTo` 规范 + 区间运算 helper + MUTEX 互斥校验，3 试点在 master-data 域） |

> 主数据域不包含状态机文档——主数据是启停二态而非工作流状态机（见上文"启用/停用"节）。

## 实现落位提示

| 设计含义 | 默认实现落位 |
|----------|--------------|
| 编码唯一性、停用校验、单位换算计算 | Entity（稳定领域事实、只读 helper） |
| 主数据 CRUD、启停动作、引用校验 | BizModel（事务入口、对外动作） |
| 跨域主数据查询 | `IErpMd*Biz` 接口（`-dao` 暴露） |
| 科目树形展示、物料分类树形展示 | xmeta 配置 + `nop-sys` 树形能力 |
