# 角色与权限

## 目的

记录当前支持的角色模型。角色名与各域状态机的迁移执行角色同源（状态机审查第 6 维度、设计文档审查第 8 维度要求一致）。

## 角色体系

角色按业务职能划分，每个角色对应一个或多个业务域的操作权限。角色名使用业务词汇（与 `domain-glossary.md` 一致），不使用技术术语。

### 核心业务角色

| 角色 | 职责域 | 主要操作 |
|------|--------|----------|
| 采购员 | purchase | 创建采购订单/入库/发票/付款/退货；提交单据审核 |
| 销售员 | sales | 创建销售订单/出库/发票/收款/退货；提交单据审核 |
| 库管员 | inventory | 库存移动单 CRUD、确认/取消、调拨、盘点、冲销 |
| 财务员 | finance | 凭证 CRUD、过账、红冲、收付款核销、期末结账发起、资产折旧执行 |

### 扩展业务角色

| 角色 | 职责域 | 主要操作 |
|------|--------|----------|
| 资产管理员 | assets | 资产卡片 CRUD、资本化入账、暂停/恢复、价值调整 |
| 项目经理 | projects | 项目立项、暂停/恢复、完成、任务管理、工时审核 |
| 生产计划员 | manufacturing | 创建工单、提交审核、取消 |
| 生产主管 | manufacturing | 工单审核、开工、停工/恢复、关闭 |
| 作业员 | manufacturing | 通过作业卡报工、记录工时 |
| 质检员 | quality | 质检单录入结果（合格/不合格）、提让步建议 |
| 质量主管 | quality | 让步接收审批、NCR 评审、CAPA 验证 |
| 维护主管 | maintenance | 维护计划管理、维护访问排程、报修受理/拒绝 |
| 维护人员 | maintenance | 维护访问执行、消耗备件、记录维护内容 |

### 审核与管理角色

| 角色 | 职责 | 说明 |
|------|------|------|
| 审核人 | 各业务域单据审核 | 按单据类型配置审批流（nop-wf），与单据创建人职责分离 |
| 管理员 | 高危操作与系统管理 | 反审核已审核单据、作废已审核单据、反结账会计期间、负库存配置、强制部分齐套开工、资产报废/出售审批 |

## 权限规则

### 职责分离（建议配置）

- 单据创建人与审核人不可为同一人（采购员/销售员创建 → 审核人审核）。
- 质检员与让步接收审批人不可为同一人（质检员提建议 → 质量主管审批）。
- 财务员与期末结账审批（若需）分离。

### 高危操作权限

以下操作需管理员权限或额外审批：

| 操作 | 权限要求 | 原因 |
|------|----------|------|
| 反审核已审核单据 | 管理员 + 冲销前置校验 | 影响已入账库存与凭证 |
| 作废已审核单据 | 管理员 | 需冲销已生成结果 |
| 反结账会计期间 | 管理员 + 审批 | 影响已出具报表与税务申报 |
| 资产报废/出售处置 | 资产管理员 + 审批 | 结转资产价值影响报表 |
| 工单关闭（部分完工） | 生产主管/管理员 | 影响成本结转 |
| 强制部分齐套开工 | 生产主管 | 有缺料风险 |
| 让步接收（降级使用） | 质量主管审批 | 质量风险 |
| 负库存放行配置 | 管理员 | 允许余额为负的特殊场景 |
| 红冲已过账凭证 | 财务员 + 二次确认 | 影响总账与报表 |
| 期末批量折旧 | 财务员 + 确认 | 影响所有资产 |

### 数据权限

- 业务员只能看自己创建的单据（可配置为部门可见）。
- 财务员可见所有财务相关单据与凭证。
- 质检员只能看分配给自己的质检任务。
- 维护人员只能看分配给自己的维护访问。

### 状态机绑定

每个状态机的迁移都绑定执行角色（详见各域 `state-machine.md` 的"角色与权限"节）。角色名与本文档一致。新增状态机迁移时必须同步更新本文档。

## 审批与审计要求

- 单据审核支持配置审批流（引入 nop-wf）：单审/会审/多级审。
- 高危操作（反审核/作废/反结账/处置）留审计日志：操作人、时间、原因。
- 让步接收记录让步理由与审批人。
- 红冲凭证摘要注明冲销原凭证号与原因。

## 设计能力基线（已沉淀，始终生效）

以下能力随模块定义落地，**始终生效**（其中数据权限不依赖操作级开关）：

- **角色矩阵**：见本文"角色体系"——按业务职能划分，角色名与各域状态机迁移执行角色同源。
- **操作权限资源点**：`*.action-auth.xml`（`TOPM`/`SUBM`/`FNPT`）由 codegen 自动产出，定义菜单与功能权限点。三层文件链与定制约定见 `app-overview.md §菜单权威源与定制约定`。
- **角色→权限点映射**（粗粒度）：见下方"角色→权限点映射"节。权限点 ID 引用 `_erp-*.action-auth.xml` 生成文件为真相源（AGENTS.md 规则 7 ——不在散文重复生成文件定义）。FNPT 权限点模式：每实体约 2 个（query/mutation），格式 `<permissions>{EntityName}:{action}</permissions>`，详见各域 `_erp-*.action-auth.xml`。
- **数据权限规则**：`data-auth.xml` 行级过滤——**独立于操作级开关，始终附加到查询条件**（平台机制见 `nop-entropy/docs-for-ai/02-core-guides/auth-and-permissions.md` 数据权限节）。

## 角色→权限点映射

映射层级：粗粒度（15 角色 × 域/菜单组 SUBM 层 + 关键 FNPT 前缀引用）。细粒度 15 × 674 FNPT 全矩阵过大易与生成文件漂移，逐权限点映射归 successor（触发条件：RBAC 精细化或合规审计需求）。

角色名与 `domain-design-guidelines.md §6.1` 职责分离矩阵一致。

> **FNPT 权限点模式说明**：生成文件 `_erp-*.action-auth.xml` 中每实体仅有两个 FNPT 权限点——`{EntityName}:query`（查询）与 `{EntityName}:mutation`（修改，覆盖所有写操作）。下表"FNPT 前缀 + 业务动作"列中，`{前缀}*` 为 FNPT 权限点前缀（实际点为 `{前缀}:{query,mutation}`），冒号后列出的动作（如 `save/update/submitForApproval`）是 BizModel 业务方法名，均归入 `mutation` 权限点，**不是**独立的 FNPT 权限点 ID。

| 角色 | 可访问 SUBM 域/菜单组 | FNPT 前缀（→ `{前缀}:{query,mutation}`）+ 覆盖的业务动作 |
|------|-----------------------|---------------|
| 采购员 | `erp-pur`（采购管理）全部 + `erp-md`（主数据）只读（物料/往来单位/地址）+ `erp-inv`（库存）只读（库存余额查看） | `ErpPur*:{query,save,update,delete,submitForApproval}`、`ErpInv*:query` |
| 销售员 | `erp-sal`（销售管理）全部 + `erp-md` 只读 + `erp-inv` 只读 | `ErpSal*:{query,save,update,delete,submitForApproval}`、`ErpMd*:query` |
| 库管员 | `erp-inv`（库存管理）全部 + `erp-md` 只读（物料/仓库/库存维度） | `ErpInv*:{query,save,update,delete,confirm,cancel,transfer}`、`ErpMdMaterial*:query`、`ErpMdWarehouse*:query` |
| 财务员 | `erp-fin`（财务管理）全部 + `erp-md` 只读（科目/结算方式）+ 报表 `sys-report` | `ErpFin*:{query,save,update,delete,post,reverse,close,batchDepreciation,reconcile}`、`ErpMdSubject*:query` |
| 资产管理员 | `erp-ast`（资产管理）全部 + `erp-md` 只读 | `ErpAst*:{query,save,update,delete,capitalize,suspend,resume}`、`ErpMd*:query` |
| 项目经理 | `erp-prj`（项目管理）全部 + `erp-md` 只读（员工/往来单位） | `ErpPrj*:{query,save,update,delete,start,suspend,resume,complete}`、`ErpMdEmployee*:query` |
| 生产计划员 | `erp-mfg`（制造管理）工单创建/提交子集 + `erp-md` 只读（物料 BOM） | `ErpMfgWorkOrder:{query,save,update,delete,submitForApproval,cancel}`、`ErpMfgBom*:query`、`ErpMdMaterial*:query` |
| 生产主管 | `erp-mfg`（制造管理）全部（含审核/开工/停工/关闭） | `ErpMfg*:{query,approve,reject,start,stop,resume,close,checkAvailability,reportCompletion}` |
| 作业员 | `erp-mfg` 工单报工子集 `mfg-jobcard`（作业卡管理） | `ErpMfgJobCard:{query,recordWork}` |
| 质检员 | `erp-qa`（质量管理）质检单录入/提让步子集 | `ErpQaInspection:{query,save,update,recordResult}`、`ErpQaNcr:query` |
| 质量主管 | `erp-qa`（质量管理）全部（含 NCR 评审/CAPA 验证/让步审批/召回） | `ErpQa*:{query,save,update,delete,approve,reject,verify,review,escalate,locateTargets,close}` |
| 维护主管 | `erp-mnt`（维护管理）全部（含排程/受理/拒绝） | `ErpMnt*:{query,save,update,delete,accept,reject,schedule,assign}` |
| 维护人员 | `erp-mnt` 维护执行子集（访问单执行/备件消耗） | `ErpMntVisit:{query,start,complete,recordSparePart}`、`ErpMntRequest:query` |
| 审核人 | 各域审核相关 SUBM（取决于审批流配置） | 按审批流分配 `*:{approve,reject}` 权限 |
| 管理员 | **全部域 TOPM + SUBM** + `sys-*`（系统管理全部含工作流/报表/监控）+ `erp-l10n-cn` | 所有 FNPT 前缀全权限（`*:*`）；`nop.auth.skip-check-for-admin=true` 默认启用 |

> **说明**：上表为粗粒度映射蓝图。实际权限配置在 `app.action-auth.xml` 按角色关联 SUBM 资源 + `_erp-*.action-auth.xml` 的 FNPT 权限点。当前运行基线 `nop.auth.enable-action-auth=false`（见"运行基线"节），启用操作级拦截后方生效。

> **SUB 域（CRM/CS/HR/APS/Logistics/B2B/Contract/DRP）**：这些域的业务操作由对应角色（如客服人员、HR 专员等）在各自域内执行，尚未定义独立 ERP 角色映射。灰度启用操作级拦截时可按需为上述域的新建角色分配对应 SUBM 资源。

## 运行基线（当前拦截状态）

> "已定义 ≠ 默认全部开启"。当前运行基线对**操作级拦截**默认关闭，灰度启用按下方步骤。

| 项 | 当前值 | 说明 |
|----|---------|------|
| `nop.auth.enable-action-auth` | `false`（默认） | 操作级拦截关闭：菜单与 FNPT 全量可见可操作。**数据权限不受此开关影响，始终启用**。 |
| `nop.auth.skip-check-for-admin` | `true`（默认） | 管理员跳过权限检查 |

**灰度启用操作级拦截的步骤**：
1. 在 `app-erp-all/application.yaml` 设 `nop.auth.enable-action-auth: true`。
2. 按角色配置 `FNPT` 资源授权（`*.action-auth.xml` 已就绪，无需新增资源点）。
3. 启用后菜单按角色过滤、未授权资源标记 DISABLED。
4. 灰度范围建议：先对高危操作（反审核/作废/反结账/处置）开启，再逐步铺开。

> **行业参照**：Axelor 等 ERP 的 portal/模块权限也是"权限定义随模块安装生效、默认非全开"（见 `docs/analysis/erp-survey/2026-06-30-0000-axelor-open-suite.md`），"已定义≠默认开启"是行业常态。

## 实现机制（平台组件）

- 角色与权限：nop-auth 的 RBAC 体系（用户→角色→资源）。
- 数据权限：nop-auth 的行级过滤（`nopDataAuthChecker`）。
- 审批流：nop-wf 引擎。
- 审计日志：nop-auth 的操作日志。

## 外部主体

本文仅覆盖**内部 ERP 角色**。外部 portal 主体（客户/供应商自助）见 `docs/design/portal/identity-and-access.md`（future extension，非当前基线）。

## 与其他文档的关系

- 角色名与各域 `state-machine.md` 的"角色与权限"节保持一致。
- 业务术语见 `domain-glossary.md`。
- 高危操作的具体状态迁移规则见各域状态机文档。
