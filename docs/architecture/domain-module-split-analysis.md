# 领域模块拆分决策：进销存+财务一体化的工程结构

> **状态**：已生效架构决策（已确认）。落地实施（codegen 生成 Java 模块）仍受 `model/*.orm.xml` ask-first 保护区域约束。
>
> **本文定位**：记录 nop-app-erp 多领域工程的拆分决策、前缀方案、跨工程协作规则。`docs/architecture/system-baseline.md` 引用本文作为模块结构的依据。

## TL;DR

nop-app-erp 进销存+财务一体化采用**每业务域独立 Maven 工程**结构：10 个领域工程 + 1 个聚合启动工程（app-erp-app）。每个领域工程有自己的 `model/app-erp-<domain>.orm.xml` 作为权威源模型，通过 `nop-cli gen` 生成各自的 `-codegen/-api/-dao/-meta/-service/-web` 模块集。

领域工程分两层：核心业务域（5 个，进销存+财务）与扩展业务域（5 个，资产/项目/制造/质量/维护）。

实体类名统一 `Erp{Domain}{Entity}`，表名 `erp_{domain}_{entity}`，二级域简称 `md/inv/pur/sal/fin`。

## 1. 平台 codegen 机制与拆分决策

### 1.1 标准 codegen 是"单 appName 单套模块"

经核实 `../nop-entropy/docs-for-ai/02-core-guides/model-first-development.md:14-20` 与 `application-development-workflow.md:83-91`，标准 codegen 流程是：

```bash
nop-cli gen model/{appName}.orm.xml -t=/nop/templates/orm -o=.
```

一次 `nop-cli gen` 以**一个** `model/{appName}.orm.xml` 为输入，生成**一套**以 `{appName}` 为前缀的标准多模块骨架：`{appName}-codegen/-api/-dao/-meta/-service/-web/-app`（见 `domain-module-pattern.md:1-26`）。后续模型变更通过 Maven（`./mvnw clean install`）触发各子模块挂载的 `.xgen` 脚本增量再生，不再用 CLI（见 `change-model-and-regenerate.md:21-23`）。

**docs-for-ai 没有记载"一个 app 内放多份平行 orm.xml 各自自动生成内部结构"的模式。** 平台内置的多领域模块（nop-auth/nop-sys/nop-wf 等）每个都是**独立的 Maven 工程**，通过 Maven 依赖被 app 引入（见 `nop-app-mall-app/pom.xml` 同时依赖 `app-mall-service` + `nop-auth-service` + `nop-sys-web`）。

### 1.2 决策：采用平台多领域工程模式

nop-app-erp 借鉴平台层"每领域独立工程"的模式应用到应用层：把 5 个业务域各建成独立 Maven 工程，每个工程自带 `model/app-erp-<domain>.orm.xml`，各自通过 `nop-cli gen` 生成模块骨架，最后由 `app-erp-app` 聚合依赖启动。

**决策理由**：

1. **预估表数 80-120，远超合理单文件规模**。平台内置模块普遍 ≤21 实体，`nop-app-mall` 单文件 54 实体是已验证最大先例。单 `app-erp.orm.xml` 不可维护。
2. **业务域边界强**（主数据/库存/采购/销售/财务是 ERP 经典分区），独立工程天然表达领域边界。
3. **独立 codegen 各域并行演进**，团队可并行设计不同域的 orm.xml 而不冲突。
4. **跨域协作走 `I*Biz` + Maven 依赖**（DAG），符合 `architecture-principles.md` 的模块依赖单向规则。

### 1.3 对先前文档版本的纠正

本文先前的版本（2026-06-22 早些时候）以 `OrmModelLoader.loadOrmModel()` 运行时合并多个 `/{moduleId}/orm/app.orm.xml` 作为"平台原生支持多 orm.xml"的主要论据。**这是对平台机制的误读**：`OrmModelLoader` 是运行时合并已生成产物的内部机制，不改变"标准 codegen 是单 appName 单套模块"的事实。外部应用要获得多领域结构，必须采用本节的"每领域独立工程"模式，而非依赖运行时合并。先前版本的相关论证已移除。

## 2. 工程结构

### 2.1 顶层布局

```
nop-app-erp/                          （聚合根，pom packaging=pom）
├── app-erp-master-data/              核心域：主数据
├── app-erp-inventory/                核心域：库存
├── app-erp-purchase/                 核心域：采购
├── app-erp-sales/                    核心域：销售
├── app-erp-finance/                  核心域：财务
├── app-erp-assets/                   扩展域：固定资产
├── app-erp-projects/                 扩展域：项目管理
├── app-erp-manufacturing/            扩展域：制造
├── app-erp-quality/                  扩展域：质量管理
├── app-erp-maintenance/              扩展域：设备维护
├── app-erp-delta/                    对 nop-auth/nop-sys 的 Delta 扩展（可选）
├── app-erp-app/                      聚合启动（Quarkus main）
├── model/                            （bootstrap 阶段：10 份 orm.xml 暂存于此）
└── docs/
```

> **bootstrap 阶段说明**：当前 5 份 `model/app-erp-<domain>.orm.xml` 暂存在项目根 `model/` 下作为权威源模型。待首个领域实体设计完成、`nop-cli gen` 跑通后，每份 orm.xml 随其工程目录就位（移到 `app-erp-<domain>/model/` 下）。

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

**不手写 pom.xml 或 Java 模块目录**——这些由 `nop-cli gen` 生成。bootstrap 阶段只维护 `model/*.orm.xml` 源模型与设计文档。

## 3. 命名与前缀方案

### 核心业务域

| 维度 | 规则 | 示例 |
|---|---|---|
| 领域工程目录 | `app-erp-<domain>` | `app-erp-master-data`、`app-erp-inventory`、`app-erp-purchase`、`app-erp-sales`、`app-erp-finance` |
| appName（orm.xml `ext:appName`） | `app-erp-<简称>` | `app-erp-md`、`app-erp-inv`、`app-erp-pur`、`app-erp-sal`、`app-erp-fin` |
| 二级域简称 | 固定 | `md`(master-data) / `inv`(inventory) / `pur`(purchase) / `sal`(sales) / `fin`(finance) |
| 实体类名 | `Erp<Domain><Entity>` | `ErpMdMaterial`、`ErpInvStockMove`、`ErpPurOrder`、`ErpSaleOrder`、`ErpFinVoucher` |
| 表名 | `erp_<简称>_<entity>` | `erp_md_material`、`erp_inv_stock_move`、`erp_pur_order`、`erp_sal_order`、`erp_fin_voucher` |
| 字典命名空间 | `erp-<简称>/<dict-name>` | `erp-md/material-type`、`erp-inv/move-status`、`erp-fin/voucher-type` |

### 扩展业务域

| 维度 | 规则 | 示例 |
|---|---|---|
| 领域工程目录 | `app-erp-<domain>` | `app-erp-assets`、`app-erp-projects`、`app-erp-manufacturing`、`app-erp-quality`、`app-erp-maintenance` |
| appName（orm.xml `ext:appName`） | `app-erp-<简称>` | `app-erp-ast`、`app-erp-prj`、`app-erp-mfg`、`app-erp-qa`、`app-erp-mnt` |
| 二级域简称 | 固定 | `ast`(assets) / `prj`(projects) / `mfg`(manufacturing) / `qa`(quality) / `mnt`(maintenance) |
| 实体类名 | `Erp<Domain><Entity>` | `ErpAstAsset`、`ErpPrjProject`、`ErpMfgWorkOrder`、`ErpQaInspection`、`ErpMntEquipment` |
| 表名 | `erp_<简称>_<entity>` | `erp_ast_asset`、`erp_prj_project`、`erp_mfg_work_order`、`erp_qa_inspection`、`erp_mnt_equipment` |
| 字典命名空间 | `erp-<简称>/<dict-name>` | `erp-ast/asset-status`、`erp-mfg/work-order-status`、`erp-qa/inspection-result` |

**前缀选择理由**：
- 主前缀 `Erp` 与现有 `model/app-erp.orm.xml` 骨架约定一致，避免与 nop 内置实体（`Nop*` 前缀）冲突。
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
```

Maven 依赖方向严格单向（DAG），下游工程不能反向依赖上游。`app-erp-app` 聚合所有领域工程的 `-service`/`-web` 模块启动。

### 4.2 跨工程实体关系：走 I*Biz，不做 ORM 强引用

**硬规则**（源自 `architecture-principles.md:40-62`）：跨工程实体**不做** ORM 层 `refEntityName` 强引用。平台所有内置模块（nop-auth/nop-sys/nop-wf）的源 orm.xml 中 `refEntityName` 全部指向本模块包内实体，零跨包引用。

具体做法：
- 引用方工程用**纯外键列**（如 `erp_pur_order.material_id VARCHAR`），不带 `<to-one>` 关系声明。
- 在 BizModel/Processor 层通过 `@Inject IErpMdMaterialBiz`（master-data 在 `*-dao` 暴露的 `I*Biz` 接口）做只读查询和跨工程动作编排。
- 若需 ORM 层导航，在实体里用 `requireBiz(I*Biz.class)` 做只读关联（见 `domain-logic-and-ddd.md:48-69`）。

**示例**：finance 凭证要引用 purchase 的采购发票：
- ❌ `app-erp-finance` 的 orm.xml 不写 `refEntityName="app.erp.pur.dao.entity.ErpPurInvoice"`
- ✅ finance 凭证行用 `source_bill_type`/`source_head_code`/`source_line_code` 三元组（纯字符串列），BizModel 通过 `@Inject IErpPurInvoiceBiz` 查询源单

### 4.3 业财打通的跨工程实现

finance 工程定义凭证生成接口与注册中心，各业务工程（purchase/sales/inventory）实现自己的凭证生成 provider，注入 finance 的注册中心。新增单据类型 = 新增 provider Bean，零改动 finance 核心。具体接口设计待 finance 域实体设计时定稿。

## 5. codegen 与手写边界

| 任务 | 何时做 | 方式 |
|---|---|---|
| 首次生成领域工程骨架 | 该域首批实体设计完成（建议 ≥3 实体）后 | `nop-cli gen model/app-erp-<domain>.orm.xml -t=/nop/templates/orm -o=app-erp-<domain>/` |
| 后续模型变更（加字段/加表） | 随时 | 改 `model/*.orm.xml` → `./mvnw clean install` 触发增量再生 |
| BizModel 业务逻辑 | codegen 后 | 在 `*-service` 保留层手写（非 `_gen`） |
| 页面定制 | codegen 后 | 在 `*-web` 保留层 `*.view.xml` 继承 `_gen/_*.view.xml` |
| 跨工程 I*Biz 接口 | codegen 后 | 在 `*-dao/.../biz/` 定义，调用方 `@Inject` |
| Delta 扩展 nop-auth/nop-sys | 需要时 | `app-erp-delta` 工程 |

**永不手写**：`_gen/` 目录、`_` 前缀文件、`_app.orm.xml`、`_service.beans.xml`（见 `model-first-development.md:140-146`）。

## 6. 实施路线（分阶段）

| 阶段 | 触发条件 | 做什么 |
|---|---|---|
| **阶段 0（当前）** | bootstrap | 5 份 orm.xml 骨架暂存 `model/`；设计文档立项；本文定稿 |
| **阶段 1** | 首个域（建议 master-data）实体设计完成 | 跑 `nop-cli gen` 生成 master-data 工程；验证生成链路；将 `model/app-erp-master-data.orm.xml` 移入工程目录 |
| **阶段 2** | 第二个域实体设计完成 | 生成第二个领域工程；验证跨工程 `I*Biz` 调用 |
| **阶段 3** | 全部 5 域工程就位 | `app-erp-app` 聚合启动；端到端联调业财打通 |

## 7. 相关文档

- `docs/architecture/system-baseline.md` — 模块结构（引用本文）
- `docs/architecture/module-boundaries.md` — 领域工程依赖方向与禁止依赖
- `docs/design/flow-overview.md` — 跨域业务流程（采购→入库→应付→凭证 等）
- `docs/design/{master-data,inventory,purchase,sales,finance}.md` — 各域业务设计
- `../nop-entropy/docs-for-ai/02-core-guides/model-first-development.md` — codegen 机制权威说明
- `../nop-entropy/docs-for-ai/01-repo-map/domain-module-pattern.md` — 业务模块标准骨架
- `../nop-entropy/docs-for-ai/02-core-guides/architecture-principles.md` — 模块依赖单向 + 跨模块 I*Biz 规则
