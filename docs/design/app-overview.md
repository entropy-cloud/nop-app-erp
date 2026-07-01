# 应用总览

## 目的

说明当前稳定支持的应用级产品基线。

## 主要界面或页面

- 管理后台：主数据维护、采购管理、销售管理、库存管理、财务管理、报表与查询、系统配置
- （前台商城/门户暂不在当前基线范围。详见 `portal/README.md` STATUS 横幅——portal 为 future extension placeholder，实施前需 plan-first + 人工批准）

## 主要导航模型

后台采用侧边栏导航，按以下分组组织：

- 主数据：物料、SKU、往来单位、仓库、库位、计量单位、币种、科目表
- 采购管理：采购订单、采购入库、采购发票、付款、采购退货
- 销售管理：销售订单、销售出库、销售发票、收款、销售退货
- 库存管理：库存移动、库存查询、调拨、盘点、批次/序列号
- 财务管理：凭证、科目、核销、期末结账、报表
- 资产管理：资产卡片、折旧、资产处置、价值调整
- 项目管理：项目、任务、工时记录
- 制造管理：BOM、工单、作业卡、工艺路线、工作中心
- 质量管理：质检单、质检模板、不符合项（NCR）、纠正预防措施
- 设备维护：设备、维护计划、维护访问、维护请求、停机记录
- 系统管理：用户、角色、权限、组织、配置（复用 nop-auth/nop-sys）

> 上面的分组仅用于产品概览，**不是菜单权威源**。菜单结构、页面入口与功能权限点（`TOPM`/`SUBM`/`FNPT` 三级资源）以每域的 `*.action-auth.xml` 为唯一真相源，遵循 AGENTS.md 第 7 条"持久化与契约真相保存在模型 XML、散文不复述"的同一原则，本文档不重复菜单树细节。

## 菜单权威源与定制约定

- **权威源**：菜单与功能权限点以 `*.action-auth.xml` 为准（`TOPM` 一级菜单 / `SUBM` 子菜单 / `FNPT` 功能点），加载路径为 `/nop/main/auth/app.action-auth.xml`，应用层聚合各模块配置。平台规范见 `nop-entropy/docs-for-ai/02-core-guides/auth-and-permissions.md`。
- **生成产物**：代码生成会为每个域产出 `_{moduleName}.action-auth.xml`（下划线前缀），它从 `model/*.orm.xml` 的 `ext:icon` 自动推导出每个实体的菜单骨架（SUBM 的 `icon`、两个 FNPT `{objName}:query` 与 `{objName}:mutation`）。这是**代码生成产物而非测试用文件**，按平台规则禁止手工修改。
- **三层文件链**：codegen 后每域实际形成三层（以采购域为例）：① `_erp-pur.action-auth.xml`（web 模块，生成产物，禁止手改）→ ② `erp-pur.action-auth.xml`（web 模块，手写定制层，`x:extends="_erp-pur.action-auth.xml"`）→ ③ `app.action-auth.xml`（app 模块，应用聚合层，`x:extends="erp-pur.action-auth.xml"`，最终加载入口）。
- **本项目定制方式**：不为整棵菜单另起一份散文规划。在每域手写层（第②层 `{moduleName}.action-auth.xml`）做定制——通过 `x:extends` 继承生成产物，再对不需要的自动生成项用 `x:override="remove"` 删除（合并算子语义见 `nop-entropy/docs-for-ai/02-core-guides/xdef-and-xdsl.md` 的 `x:override` 表）。菜单图标、排序、路由等细节也在这份手写文件中沉淀，不在 `docs/design/` 中复述。
- **与权限的衔接**：`FNPT` 资源声明的权限标识与 `roles-and-permissions.md` 的角色矩阵对接。操作权限检查的当前拦截状态与灰度启用步骤见 `roles-and-permissions.md §运行基线`（数据权限始终生效，不依赖操作级开关）。

## 主要用户角色

- 超级管理员：拥有全系统访问权限
- 采购员：维护采购订单、跟踪入库
- 销售员：维护销售订单、跟踪出库
- 库管员：审核出入库、管理库存、盘点
- 财务员：审核发票、生成凭证、收付款核销、期末结账
- 管理员：在分配职责范围内执行运营动作

详细角色与权限模型见 `roles-and-permissions.md`。

## 核心业务流程

- 主数据维护：物料 → SKU → 往来单位 → 仓库/库位 → 计量单位 → 币种 → 科目表 → [`master-data/README.md`](master-data/README.md)
- 采购流程：采购订单 → 采购入库（写库存）→ 采购发票（生成应付凭证）→ 付款（核销发票）→ [`purchase/README.md`](purchase/README.md)
- 销售流程：销售订单 → 销售出库（写库存）→ 销售发票（生成应收凭证）→ 收款（核销发票）→ [`sales/README.md`](sales/README.md)
- 库存流程：库存移动 → 库存流水（不可变）→ 库存余额更新 → 调拨/盘点 → [`inventory/README.md`](inventory/README.md)
- 财务流程：业务单据审核 → 自动生成凭证 → 核销 → 期末结账 → [`finance/README.md`](finance/README.md)
- 资产流程：购置 → 资本化入账 → 每月折旧 → 报废/出售处置 → [`assets/README.md`](assets/README.md)
- 项目流程：立项 → 任务分解 → 工时记录 → 项目成本归集 → [`projects/README.md`](projects/README.md)
- 生产流程：BOM 定义 → 工单 → 领料 → 报工 → 完工入库 → 质检 → [`manufacturing/README.md`](manufacturing/README.md)
- 质量流程：业务触发质检 → 结果判定 → 不合格开 NCR → 纠正预防措施 → [`quality/README.md`](quality/README.md)
- 维护流程：维护计划 → 维护访问 → 消耗备件 → 设备状态联动 → [`maintenance/README.md`](maintenance/README.md)
- 跨域编排：见 [`flow-overview.md`](flow-overview.md)

## 关键领域区域

- 主数据：物料（Material）、SKU、往来单位（Partner）、仓库（Warehouse）、库位（Location）、计量单位（UoM）、币种（Currency）、汇率、科目表（COA）、科目（Account）
- 库存：库存移动单（StockMove）、库存流水（StockLedger）、库存余额（StockBalance）、调拨单、盘点单、批次、序列号、作业类型
- 采购：采购订单（PurchaseOrder）、采购入库（PurchaseReceive）、采购发票（PurchaseInvoice）、付款（Payment）、采购退货（PurchaseReturn）
- 销售：销售订单（SalesOrder）、销售出库（SalesDelivery）、销售发票（SalesInvoice）、收款（Receipt）、销售退货（SalesReturn）
- 财务：凭证（Voucher）、凭证分录行（VoucherLine）、凭证模板（VoucherTemplate）、业财回链（VoucherBillR）、科目、会计期间、账户

## 集成点

- 平台认证与授权能力（nop-auth）
- 平台系统基础能力（nop-sys：字典、序列号）
- 工作流能力（nop-wf，用于单据审批流，按需引入）
- 报表能力（nop-report，用于财务报表与库存报表）
- 文件存储能力（nop-file，用于附件/单据影像）

## 边界

- 本文件负责应用层的界面范围、角色、流程和领域区域说明。
- 持久化实体、字段和字典定义以 `model/app-erp-*.orm.xml` 为准（每域一份）。
- 技术实现细节属于 `docs/architecture/`。
- 实施顺序属于 `docs/backlog/` 或计划文件，不属于本总览。

## 规则

保持本文件稳定且面向产品。如果某个功能改变了应用支持基线，应在同一次变更中更新本文件或更窄的 owner doc。

不要在这里重复 `docs/architecture/project-vision.md` 中的长期产品愿景，也不要重复 `docs/backlog/` 中的实施顺序。
