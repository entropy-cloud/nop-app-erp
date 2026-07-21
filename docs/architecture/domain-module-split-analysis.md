# 领域模块拆分决策：进销存+财务一体化的工程结构

> **状态**：已生效架构决策（已确认）。落地实施（codegen 生成 Java 模块）仍受 `<domain>/model/*.orm.xml` ask-first 保护区域约束。
>
> **本文定位**：记录 nop-app-erp 多领域工程的拆分决策、前缀方案、跨工程协作规则。`docs/architecture/system-baseline.md` 引用本文作为模块结构的依据。

## TL;DR

nop-app-erp 进销存+财务一体化采用**每业务域独立 Maven 工程**结构：18 个领域工程 + 1 个聚合启动工程（`app-erp-all`）。每个领域工程有自己的 `model/app-erp-<domain>.orm.xml` 作为权威源模型，通过 `nop-cli gen` 生成各自的 `-codegen/-api/-dao/-meta/-service/-web` 模块集。

领域工程分两层：核心业务域（5 个，进销存+财务）与扩展业务域（13 个，资产/项目/制造/质量/维护 + CRM/CS/HR/APS/合同/DRP/物流/B2B）。**逻辑工程名 × 物理目录 × appName × moduleId 的完整映射见 §2.0**。

实体类名统一 `Erp{Domain}{Entity}`，表名 `erp_{domain}_{entity}`，二级域简称 `md/inv/pur/sal/fin` 等。

## 1. 平台 codegen 机制与拆分决策

### 1.1 标准 codegen 是"单 appName 单套模块"

经核实 `../nop-entropy/docs-for-ai/02-core-guides/model-first-development.md:14-20` 与 `application-development-workflow.md:83-91`，标准 codegen 流程是：

```bash
nop-cli gen model/{appName}.orm.xml -t=/nop/templates/orm -o=.
```

一次 `nop-cli gen` 以**一个** `model/{appName}.orm.xml` 为输入，生成**一套**以 `{appName}` 为前缀的标准多模块骨架：`{appName}-codegen/-api/-dao/-meta/-service/-web/-app`（见 `domain-module-pattern.md:1-26`）。后续模型变更通过 Maven（`./mvnw clean install`）触发各子模块挂载的 `.xgen` 脚本增量再生，不再用 CLI（见 `change-model-and-regenerate.md:21-23`）。

**docs-for-ai 没有记载"一个 app 内放多份平行 orm.xml 各自自动生成内部结构"的模式。** 平台内置的多领域模块（nop-auth/nop-sys/nop-wf 等）每个都是**独立的 Maven 工程**，通过 Maven 依赖被 app 引入（见 `nop-app-mall-app/pom.xml` 同时依赖 `app-mall-service` + `nop-auth-service` + `nop-sys-web`）。

### 1.2 决策：采用平台多领域工程模式

nop-app-erp 借鉴平台层"每领域独立工程"的模式应用到应用层：每个业务域建成独立 Maven 工程，每个工程自带 `model/app-erp-<domain>.orm.xml`，各自通过 `nop-cli gen` 生成模块骨架，最后由 `app-erp-all` 聚合依赖启动。

**决策理由**：

1. **预估表数 80-120，远超合理单文件规模**。平台内置模块普遍 ≤21 实体，`nop-app-mall` 单文件 54 实体是已验证最大先例。单 `app-erp.orm.xml` 不可维护。
2. **业务域边界强**（主数据/库存/采购/销售/财务是 ERP 经典分区），独立工程天然表达领域边界。
3. **独立 codegen 各域并行演进**，团队可并行设计不同域的 orm.xml 而不冲突。
4. **跨域协作走 `I*Biz` + Maven 依赖**（DAG），符合 `architecture-principles.md` 的模块依赖单向规则。
 
## 2. 工程结构

### 2.0 工程命名映射表（唯一规范）

> **规则**：以 Maven artifactId 前缀 `app-erp-<domain>` 作为"逻辑工程名"的**唯一规范**，物理目录 `module-<domain>/` 是 codegen 期物理别名。所有 design/architecture 文档统一引用逻辑名，物理目录供构建脚本与 IDE 识别。`app-erp-all` 是最终聚合启动工程的物理目录与逻辑名（一致），不再使用旧名 `app-erp-app`。

| 业务域 | 逻辑工程名（artifactId 前缀） | 顶层目录 | 子模块前缀 | appName（orm.xml `ext:appName`） | VFS moduleId | 二级简称 | 实体类名前缀 | 表前缀 |
|---|---|---|---|---|---|---|---|---|
| 主数据 | `app-erp-master-data` | `module-master-data/` | `erp-md-` | `erp-md` | `erp/md` | `md` | `ErpMd*` | `erp_md_` |
| 库存 | `app-erp-inventory` | `module-inventory/` | `erp-inv-` | `erp-inv` | `erp/inv` | `inv` | `ErpInv*` | `erp_inv_` |
| 采购 | `app-erp-purchase` | `module-purchase/` | `erp-pur-` | `erp-pur` | `erp/pur` | `pur` | `ErpPur*` | `erp_pur_` |
| 销售 | `app-erp-sales` | `module-sales/` | `erp-sal-` | `erp-sal` | `erp/sal` | `sal` | `ErpSal*` | `erp_sal_` |
| 财务 | `app-erp-finance` | `module-finance/` | `erp-fin-` | `erp-fin` | `erp/fin` | `fin` | `ErpFin*` | `erp_fin_` |
| 固定资产 | `app-erp-assets` | `module-assets/` | `erp-ast-` | `erp-ast` | `erp/ast` | `ast` | `ErpAst*` | `erp_ast_` |
| 项目管理 | `app-erp-projects` | `module-projects/` | `erp-prj-` | `erp-prj` | `erp/prj` | `prj` | `ErpPrj*` | `erp_prj_` |
| 制造 | `app-erp-manufacturing` | `module-manufacturing/` | `erp-mfg-` | `erp-mfg` | `erp/mfg` | `mfg` | `ErpMfg*` | `erp_mfg_` |
| 质量管理 | `app-erp-quality` | `module-quality/` | `erp-qa-` | `erp-qa` | `erp/qa` | `qa` | `ErpQa*` | `erp_qa_` |
| 设备维护 | `app-erp-maintenance` | `module-maintenance/` | `erp-mnt-` | `erp-mnt` | `erp/mnt` | `mnt` | `ErpMnt*` | `erp_mnt_` |
| 客户关系 | `app-erp-crm` | `module-crm/` | `erp-crm-` | `erp-crm` | `erp/crm` | `crm` | `ErpCrm*` | `erp_crm_` |
| 客户服务 | `app-erp-cs` | `module-cs/` | `erp-cs-` | `erp-cs` | `erp/cs` | `cs` | `ErpCs*` | `erp_cs_` |
| 人力资源 | `app-erp-hr` | `module-hr/` | `erp-hr-` | `erp-hr` | `erp/hr` | `hr` | `ErpHr*` | `erp_hr_` |
| 高级排程 | `app-erp-aps` | `module-aps/` | `erp-aps-` | `erp-aps` | `erp/aps` | `aps` | `ErpAps*` | `erp_aps_` |
| 合同 | `app-erp-contract` | `module-contract/` | `erp-ct-` | `erp-ct` | `erp/ct` | `ct` | `ErpCt*` | `erp_ct_` |
| 分销资源 | `app-erp-drp` | `module-drp/` | `erp-drp-` | `erp-drp` | `erp/drp` | `drp` | `ErpDrp*` | `erp_drp_` |
| 物流 | `app-erp-logistics` | `module-logistics/` | `erp-log-` | `erp-log` | `erp/log` | `log` | `ErpLog*` | `erp_log_` |
| B2B | `app-erp-b2b` | `module-b2b/` | `erp-b2b-` | `erp-b2b` | `erp/b2b` | `b2b` | `ErpB2b*` | `erp_b2b_` |
| 通知派发 | `app-erp-notify` | `module-notify/` | `erp-notify-` | `erp-notify` | `erp/notify` | `sys`¹ | `ErpSys*`² | `erp_sys_`² |
| 聚合启动 | `app-erp-all`（聚合，物理目录即 `app-erp-all/`） | `app-erp-all/` | — | — | — | — | — | — |

合计 20 行 = 18 业务域 + 1 通知子系统 + 1 聚合启动工程。

> **moduleId ↔ moduleName 推导规则**（源自 `vfs-and-resource-resolution.md:121-122`）：`moduleId = moduleName.replace('-','/')`，`moduleName = moduleId.replace('/','-')`。故 `moduleName=erp-md` ↔ `moduleId=erp/md`（两级斜杠路径），实际 VFS 资源位于 `/_vfs/erp/md/`。`ext:appName` 取 `moduleName` 形式（两级，`erp-<简称>`）。

> **范围说明**：本项目正式基线为 **18 业务域**（含 5 核心域 + 5 第一批扩展域 + 8 第二批扩展域），详见 `docs/requirements/product-scope.md`。

> **关于实体前缀 `ErpB2b*`**：B2B 域实体类名采用首字母大写、其余小写的 `B2b` 形式（如 `ErpB2bEdiFormat`），表前缀 `erp_b2b_` 全小写。
>
> **关于通知子系统前缀**：¹ 通知子系统用作跨域通知派发基础设施，无业务级"简称"，二级简称取 `sys` 反映其系统层定位。² 实体类名前缀 `ErpSys*`（如 `ErpSysNotificationTemplate`）、表前缀 `erp_sys_`，与平台内置 `NopSys*` 区分。

### 2.1 顶层布局

> **物理目录 vs 逻辑工程名**：下表展示**物理顶层目录**（构建脚本与 IDE 识别用）。每个 `module-<domain>/` 对应一个**逻辑工程名** `app-erp-<domain>`，二者映射见 §2.0。所有 design/architecture 文档统一引用逻辑工程名。

```
nop-app-erp/                          （聚合根，pom packaging=pom）
├── module-master-data/               核心域：主数据（逻辑名 app-erp-master-data）
├── module-inventory/                 核心域：库存
├── module-purchase/                  核心域：采购
├── module-sales/                     核心域：销售
├── module-finance/                   核心域：财务
├── module-assets/                    扩展域：固定资产
├── module-projects/                  扩展域：项目管理
├── module-manufacturing/             扩展域：制造
├── module-quality/                   扩展域：质量管理
├── module-maintenance/               扩展域：设备维护
├── module-crm/                       扩展域：客户关系
├── module-cs/                        扩展域：客户服务
├── module-hr/                        扩展域：人力资源
├── module-aps/                       扩展域：高级排程
├── module-contract/                  扩展域：合同
├── module-drp/                       扩展域：分销资源
├── module-logistics/                 扩展域：物流
├── module-b2b/                       扩展域：B2B
├── module-notify/                    跨域通知派发子系统
├── app-erp-all/                      聚合启动工程（Quarkus main，逻辑名=物理名）
└── docs/                             设计/架构/上下文文档
```

> **当前阶段说明**：18 业务域 + 1 通知子系统的 `module-<domain>/model/app-erp-<domain>.orm.xml` 均在各 `module-<domain>/model/` 目录下作为权威源模型，codegen 骨架已生成。`module-<domain>/` 物理目录名是 codegen 期产物，与逻辑工程名 `app-erp-<domain>` 的映射见 §2.0——后续重构可对齐，但当前无需重命名。

### 2.2 领域工程内部结构（由 nop-cli gen 自动生成，不手写）

每个领域工程的标准结构遵循 `domain-module-pattern.md`：

```
app-erp-<domain>/
├── model/app-erp-<domain>.orm.xml    唯一手编辑源
├── app-erp-<domain>-codegen/         代码生成入口（postcompile/gen-orm.xgen）
├── app-erp-<domain>-api/             对外 RPC 接口契约（Typed Service Interface + Message Bean）
├── app-erp-<domain>-dao/             实体、DAO、I*Biz 接口（生成 + 保留层）
├── app-erp-<domain>-meta/            XMeta 与 i18n（生成）
├── app-erp-<domain>-service/         BizModel 实现（*.xbiz + Java）
├── app-erp-<domain>-web/             AMIS 页面（*.view.xml）
└── pom.xml
```

**不手写 pom.xml 或 Java 模块目录**——这些由 `nop-cli gen` 生成。源模型与设计文档维护期只更新 `<domain>/model/*.orm.xml` 与 `docs/` 下设计文档。

## 3. 命名与前缀方案

> **完整 18 域 × 全部命名维度映射见 §2.0**。本节给出命名规则的推导示例（首批核心 5 域 + 扩展 5 域），其余 8 个扩展域（crm/cs/hr/aps/contract/drp/logistics/b2b）遵循同一规则。

> **appName 必须是两级结构**（`erp-<简称>`，如 `erp-md`），与 orm.xml `ext:appName` 实际值一致；不要写成三级 `app-erp-<简称>`。`moduleName = appName`，`moduleId = appName.replace('-','/')`（如 `erp-md` → `erp/md`），详见 §2.0。

### 核心业务域

| 维度 | 规则 | 示例 |
|---|---|---|
| 领域工程目录 | `app-erp-<domain>` | `app-erp-master-data`、`app-erp-inventory`、`app-erp-purchase`、`app-erp-sales`、`app-erp-finance` |
| appName（orm.xml `ext:appName`） | `erp-<简称>`（两级） | `erp-md`、`erp-inv`、`erp-pur`、`erp-sal`、`erp-fin` |
| 二级域简称 | 固定 | `md`(master-data) / `inv`(inventory) / `pur`(purchase) / `sal`(sales) / `fin`(finance) |
| 实体类名 | `Erp<Domain><Entity>` | `ErpMdMaterial`、`ErpInvStockMove`、`ErpPurOrder`、`ErpSalOrder`、`ErpFinVoucher` |
| 表名 | `erp_<简称>_<entity>` | `erp_md_material`、`erp_inv_stock_move`、`erp_pur_order`、`erp_sal_order`、`erp_fin_voucher` |
| 字典命名空间 | `erp-<简称>/<dict-name>` | `erp-md/material-type`、`erp-inv/move-status`、`erp-fin/voucher-type` |

### 扩展业务域

| 维度 | 规则 | 示例 |
|---|---|---|
| 领域工程目录 | `app-erp-<domain>` | `app-erp-assets`、`app-erp-projects`、`app-erp-manufacturing`、`app-erp-quality`、`app-erp-maintenance` |
| appName（orm.xml `ext:appName`） | `erp-<简称>`（两级） | `erp-ast`、`erp-prj`、`erp-mfg`、`erp-qa`、`erp-mnt` |
| 二级域简称 | 固定 | `ast`(assets) / `prj`(projects) / `mfg`(manufacturing) / `qa`(quality) / `mnt`(maintenance) |
| 实体类名 | `Erp<Domain><Entity>` | `ErpAstAsset`、`ErpPrjProject`、`ErpMfgWorkOrder`、`ErpQaInspection`、`ErpMntEquipment` |
| 表名 | `erp_<简称>_<entity>` | `erp_ast_asset`、`erp_prj_project`、`erp_mfg_work_order`、`erp_qa_inspection`、`erp_mnt_equipment` |
| 字典命名空间 | `erp-<简称>/<dict-name>` | `erp-ast/asset-status`、`erp-mfg/work-order-status`、`erp-qa/inspection-result` |

**前缀选择理由**：
- 主前缀 `Erp` 与 nop 内置实体（`Nop*` 前缀）区分，各域 ORM 模型在 `module-<domain>/model/` 中。
- 二级简称取业务域英文首字母缩写（md/inv/pur/sal/fin），3 字符长度统一，表名 `erp_inv_stock_move` 比 `erp_inventory_stock_move` 更简洁直观。
- 类名 `ErpInvStockMove` 比无二级前缀的 `ErpStockMove` 更易定位所属域，避免跨域实体名冲突（如 `ErpOrder` 不明确是采购还是销售订单）。

## 4. 跨工程协作规则

### 4.1 依赖方向（DAG）

```
app-erp-master-data（基础，无业务依赖）
        ↑
app-erp-inventory（依赖 master-data：物料/仓库）
        ↑
app-erp-purchase / app-erp-sales（依赖 master-data + inventory：出入库写库存）
        ↑
app-erp-finance（依赖 master-data + purchase/sales/inventory 的 I*Biz：业务单据过账）

扩展域依赖：
app-erp-assets（依赖 master-data + inventory；被 finance 引用：折旧/处置过账）
app-erp-projects（依赖 master-data；被 finance 引用：项目成本归集）
app-erp-manufacturing（依赖 master-data + inventory；被 finance/quality 引用）
app-erp-quality（依赖 master-data；被 purchase/sales/manufacturing 引用：质检触发）
app-erp-maintenance（依赖 master-data + inventory + assets；被 manufacturing 引用：停机影响排产）

通知子系统（基础设施层，DAG 最底部）：
app-erp-notify（依赖 master-data：组织/用户/角色解析接收人；被所有业务域引用：通知派发）
  所有业务域 → notify：仅通过 I*Biz 接口派发通知，无 ORM 级反向引用
```

Maven 依赖方向严格单向（DAG），下游工程不能反向依赖上游。`app-erp-all` 聚合所有领域工程的 `-service`/`-web` 模块启动。

> **跨域依赖元数据**：全 19 模块的跨域业务依赖现以 BT5 风格元数据声明于各域 `erp-*-meta/precompile/module-meta.yaml`（`businessDependencies` 字段），经运行时读取器（`app-erp-all`）校验完整性。详见 `docs/architecture/business-module-metadata.md`（D2）。

### 4.2 跨工程实体关系：依赖方向必须无环

**核心原则**：ORM 层跨模块 `refEntityName` 引用**允许但必须遵循 DAG 方向**，严格禁止循环依赖。

#### 允许的跨模块引用

| 引用方向 | 是否允许 | 理由 |
|---------|---------|------|
| 所有模块 → master-data（物料/往来单位/组织等） | ✅ **允许** | master-data 是 DAG 根节点，无循环风险；ORM 级关系导航对代码生成的查询效率至关重要 |
| 下游 → 上游（purchase→inventory, finance→purchase 等） | ⚠️ **谨慎允许** | 仅在业务语义非常明确且 I*Biz 不足以满足时使用；优先使用 `source_bill_type/source_bill_code` 弱指针 |
| 上游 → 下游（master-data→inventory 等） | ❌ **禁止** | 会产生循环依赖 |

**关于 master-data 引用的特殊说明**：所有模块引用 master-data 实体（ErpMdMaterial、ErpMdPartner、ErpMdOrganization、ErpMdWarehouse、ErpMdCurrency 等）使用 ORM `<to-one>` 是**推荐的**，因为：
- master-data 处于 DAG 最上游，各模块依赖它是单向的，无循环风险
- 省去大量 `@Inject I*Biz` 查询模板代码
- codegen 可自动生成导航属性，提升开发效率

#### 必须使用 I*Biz 的场景

以下情况必须使用 `I*Biz` 接口，不得使用 ORM `refEntityName`：

| 场景 | 方案 |
|------|------|
| 两个同层模块间引用（如 purchase ↔ sales） | source_bill_type/source_bill_code 弱指针 + I*Biz |
| 上游模块引用下游模块（master-data → purchase） | 禁止，属于循环依赖 |
| 财务跨模块过账查询 | finance 凭证行用 `source_bill_type`/`source_head_code`/`source_line_code` 三元组纯字符串列，BizModel 通过 `@Inject IErpPurInvoiceBiz` 查询源单 |

#### 已批准的跨模块 ORM 引用白名单

| 源模块 | 目标模块 | 引用实体 | 理由 |
|--------|---------|---------|------|
| 全部模块 | master-data | ErpMdOrganization | DAG 根，组织/租户维度 |
| 全部模块 | master-data | ErpMdMaterial/SKU/UoM | DAG 根，物料主数据 |
| 全部模块 | master-data | ErpMdPartner | DAG 根，往来单位 |
| purchase/sales/inventory | master-data | ErpMdWarehouse/Location | DAG 根，仓库库位 |
| purchase/sales | master-data | ErpMdCurrency | DAG 根，币种 |
| finance | master-data | ErpMdSubject/AcctSchema | DAG 根，科目表 |
| projects/manufacturing | master-data | ErpMdEmployee | DAG 根，员工主数据 |
| quality | master-data | ErpMdEmployee | DAG 根 |
| logistics | master-data | ErpMdMaterial/Partner | DAG 根 |
| crm/cs/hr/aps/contract/drp/b2b | master-data | ErpMdOrganization/Partner | DAG 根 |
| hr/maintenance | master-data | ErpMdEmployee/Organization | DAG 根 |
| notify | master-data | ErpMdOrganization/User | DAG 根，接收人解析 |

> **注意**：此白名单不是一刀切许可。新增跨模块引用时必须判断依赖方向是否保持 DAG 无环，并在架构审查中记录理由。

### 4.3 业财打通的跨工程实现

finance 工程定义凭证生成接口与注册中心，各业务工程（purchase/sales/inventory）实现自己的凭证生成 provider，注入 finance 的注册中心。新增单据类型 = 新增 provider Bean，零改动 finance 核心。具体接口设计待 finance 域实体设计时定稿。

## 5. codegen 与手写边界

| 任务 | 何时做 | 方式 |
|---|---|---|
| 首次生成领域工程骨架 | 该域首批实体设计完成（建议 ≥3 实体）后 | `nop-cli gen model/app-erp-<domain>.orm.xml -t=/nop/templates/orm -o=app-erp-<domain>/` |
| 后续模型变更（加字段/加表） | 随时 | 改 `<domain>/model/*.orm.xml` → `./mvnw clean install` 触发增量再生 |
| BizModel 业务逻辑 | codegen 后 | 在 `*-service` 保留层手写（非 `_gen`） |
| 页面定制 | codegen 后 | 在 `*-web` 保留层 `*.view.xml` 继承 `_gen/_*.view.xml` |
| 跨工程 I*Biz 接口 | codegen 后 | 在 `*-dao/.../biz/` 定义，调用方 `@Inject` |
| Delta 扩展 nop-auth/nop-sys | 需要时 | `app-erp-delta` 工程 |

**永不手写**：`_gen/` 目录、`_` 前缀文件、`_app.orm.xml`、`_service.beans.xml`（见 `model-first-development.md:140-146`）。

## 6. 实施路线（分阶段）

| 阶段 | 触发条件 | 做什么 |
|---|---|---|
| **阶段 0（已完成）** | 项目立项 | 5 份 orm.xml 骨架暂存 `model/`；设计文档立项；本文定稿 |
| **阶段 1（已完成）** | 首个域（master-data）实体设计完成 | 跑 `nop-cli gen` 生成 master-data 工程；验证生成链路；将 `module-master-data/model/app-erp-master-data.orm.xml` 移入工程目录 |
| **阶段 2（已完成）** | 全部 18 域实体设计完成 | 逐个生成领域工程；验证跨工程 `I*Biz` 调用 |
| **阶段 3（已完成）** | 全部域工程就位 | `app-erp-all` 聚合启动；端到端联调业财打通 |
| **阶段 4（当前）** | codegen 完成 | BizModel 业务逻辑深化、ErrorCode 完善、页面定制、端到端验证 |

## 7. 相关文档

- `docs/architecture/system-baseline.md` — 模块结构（引用本文）
- `docs/architecture/module-boundaries.md` — 领域工程依赖方向与禁止依赖
- `docs/design/flow-overview.md` — 跨域业务流程（采购→入库→应付→凭证 等）
- `docs/design/{master-data,inventory,purchase,sales,finance}.md` — 各域业务设计
- `../nop-entropy/docs-for-ai/02-core-guides/model-first-development.md` — codegen 机制权威说明
- `../nop-entropy/docs-for-ai/01-repo-map/domain-module-pattern.md` — 业务模块标准骨架
- `../nop-entropy/docs-for-ai/02-core-guides/architecture-principles.md` — 模块依赖单向 + 跨模块 I*Biz 规则
