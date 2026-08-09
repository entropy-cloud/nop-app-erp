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
| 管理员 | 高危操作与系统管理 | = 平台 superuser（`nop.auth.skip-check-for-admin=true` 默认启用，跳过权限检查拥有全系统访问）。反审核已审核单据、作废已审核单据、反结账会计期间、负库存配置、强制部分齐套开工、资产报废/出售审批 |

## 权限规则

### 职责分离（程序级强制）

- 单据创建人与审核人不可为同一人（采购员/销售员创建 → 审核人审核）。
- 质检员与让步接收审批人不可为同一人（质检员提建议 → 质量主管审批）。
- 财务员与期末结账审批（若需）分离。

> **程序级强制（plan 2026-07-31-1023-2 R3.3）**：purchase/sales/finance/manufacturing 4 域全部 approve 路径已落地 SoD 程序级守卫——比对单据 `createdBy` 与当前审核人 `userId`，相等抛 `erp.err.<domain>.approver-is-creator`。共享守卫 `app.erp.common.service.SoDGuard`；null-user（wf 回调）放行（保留回调既有行为）。扩展域 approve 路径为显式 successor（触发条件：扩展域 SoD 抽样审计或自审回归）。

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

> **敏感字段保密清单（交叉引用）**：保密五面（薪酬/合同/EDI/供应商价格/成本分解）+ F7 已落地 PII 基线 + `taxFileNo`（隐藏非脱敏）的逐字段七元组清单（实体×字段×propId×现脱敏方式×xmeta published/queryable×GraphQL schema 影响×拟落地层）以 `docs/design/field-formatting-patterns.md` §9.7 为单一真相源。字段级可见性裁决（哪些角色可见哪些字段）归 E4.1，受 P1.2 Q1/Q4 裁决约束；本表不重复字段定义。

> **行级过滤落地状态（plan 2026-07-31-1023-3 R3.4，P1-MA6-002）**：角色侧行级过滤已落地为 `erp-*.data-auth.xml` 规则 + config-gated `ErpRoleDataAuthChecker`，**灰度默认 OFF**（双层门控：`nop.auth.enable-data-auth=false` + `erp.data-auth.role-row-filter-enabled=false`），单组织基线零回归。翻转至 enforcement 为 successor（须人工批准 + role 种子 + 灰度计划）。
>
> **过滤列与列域分类**（实测各域 orm.xml，已纠正 `createdById`/`assigneeId` 误称）：
> - **userId 域列**（直接与 `${userContext.userId}` 比较，已落地规则）：sales `createdBy`（业务员×6 单据）；quality `ownerId`（质检员×ErpQaRiskRegister，VARCHAR stdDomain="userId"）。
> - **employee-id 域列**（BIGINT 职员 id，须 user→employee 解析，**successor**）：quality `inspectorId`（ErpQaInspection，"检验员(职员)"）；maintenance `assignedTo`（ErpMntVisit/Request，"指派人"）。`ErpMdEmployee` 当前无 userId 列，user→employee 解析不可行，待模型扩展后补规则。
> - **财务员"全见"**：finance 维持空 `<objs/>`（DefaultDataAuthChecker 对未声明 obj 返回 null = 无 filter = 全见，语义正确），为设计决定非巧合。
>
> **EL 表达式**：filter 用 `${userContext.userId}`（`DefaultDataAuthChecker.newEvalScope` 注入的 scope 变量 = `IUserContext`）。注意 `${$context.user.userId}` 无效——`$context` 解析为 `IContext`，`IContext` 无 `getUser()` 方法。

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
- **数据权限规则**：`data-auth.xml` 行级过滤——设计能力独立于操作级开关（平台机制见 `nop-entropy/docs-for-ai/02-core-guides/auth-and-permissions.md` 数据权限节）。**运行时灰度**：本 app 经 config-gated `ErpRoleDataAuthChecker`（bean `nopDataAuthChecker`）门控，双层默认 OFF（`nop.auth.enable-data-auth=false` + `erp.data-auth.role-row-filter-enabled=false`）→ checker `getFilter` 返回 null → 不附加任何条件（单组织基线零回归）。翻转须同时开启两者（successor，见上方"行级过滤落地状态"）。

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

### 第二批扩展域角色基线（CRM/CS/HR/APS/Logistics/B2B/Contract/DRP）

> 第二批扩展域的角色基线按「是否承载敏感操作」分两类处理：敏感子集（HR 工资审批 / 合同审批电子签 / B2B EDI 对账）已定义角色；非敏感子集显式声明 deferred 范围边界（触发条件明确，非沉默「尚未定义」）。

**A. 承载敏感操作的域（角色已定义）**：

| 角色 | 职责域 | 主要操作 |
|------|--------|----------|
| HR 专员 | human-resource | 员工/合同/考勤/休假维护；提交薪酬核算 |
| 薪酬审批人 | human-resource | 薪酬（Salary）审批（xwf/DIRECT 三轴审批，含个税/社保等薪酬机密数据）|
| 合同专员 | contract | 合同起草/谈判/版本管理；提交合同审批 |
| 合同审批人 | contract | 合同审批与电子签触发（e-signature，含 NEGOTIATION→ACTIVE 迁移门控）|
| B2B 对账员 | b2b | EDI 事务处理、ASN 接收、代码映射维护、B2B 对账确认 |
| B2B 管理员 | b2b | EDI 出站自动化配置、错误升级处理、归档管理 |

> 敏感操作（薪酬审批 / 合同审批 / EDI 对账）的权限点在灰度启用 `enable-action-auth=true` 后按角色绑定 FNPT 资源。运行时权限注解落地归 R2.7（P1-MA3-046 代码侧）+ MA6，本表为角色基线语义锚点。

**B. 非敏感操作的域（角色映射 deferred）**：

CRM / CS / APS / Logistics / DRP 域的业务操作（线索跟进 / 工单处理 / 排产 / 发运 / 补货）暂未定义独立 ERP 角色映射。

- **Deferred 触发条件**：该域深化部署，或多公司 / 多团队数据隔离需求出现时。
- **当前基线范围边界**：这些域的 SUBM / FNPT 资源已由 codegen 产出（`_erp-*.action-auth.xml`），灰度启用操作级拦截时可按需为上述域的新建角色（如客服人员 / 排产计划员 / 物流调度员 / 补货计划员 / CRM 销售员）分配对应 SUBM 资源。
- **数据权限独立于操作级开关**：行级 orgId / 角色侧过滤独立于 `enable-action-auth` 开关（见"数据权限"节），经独立灰度门控（默认 OFF，翻转 successor），保证多公司 / 多团队数据隔离不依赖角色映射落地。

## 运行基线（当前拦截状态）

> "已定义 ≠ 默认全部开启"。当前运行基线对**操作级拦截**默认关闭，灰度启用按下方步骤。

| 项 | 当前值 | 说明 |
|----|---------|------|
| `nop.auth.enable-action-auth` | `false`（默认） | 操作级拦截关闭：菜单与 FNPT 全量可见可操作。数据权限不受此开关影响（有独立灰度门控，见"数据权限"节，默认 OFF）。 |
| `nop.auth.skip-check-for-admin` | `true`（默认） | 管理员跳过权限检查 |

**灰度启用操作级拦截的步骤**：
1. 在 `app-erp-all/application.yaml` 设 `nop.auth.enable-action-auth: true`。
2. 按角色配置 `FNPT` 资源授权（`*.action-auth.xml` 已就绪，无需新增资源点）。
3. 启用后菜单按角色过滤、未授权资源标记 DISABLED。
4. 灰度范围建议：先对高危操作（反审核/作废/反结账/处置）开启，再逐步铺开。

> **行业参照**：Axelor 等 ERP 的 portal/模块权限也是"权限定义随模块安装生效、默认非全开"（见 `docs/analysis/erp-survey/2026-06-30-0000-axelor-open-suite.md`），"已定义≠默认开启"是行业常态。

### action-level 权限声明层（R2.7 / P1-MA3-046，config-gated）

> 来源：plan `2026-07-31-0310-2-r2-7-api-contract-consistency.md` Phase 4。A3.6 审计 P1-MA3-046 发现全域敏感动作零运行时权限保护 + FNPT 粒度粗（每实体仅 `{Entity}:query`/`{Entity}:mutation`，敏感动作坍缩进 `mutation` 桶）。本节建立 **per-action FNPT 声明层**，保持 enforcement OFF。

**已落地（声明层，enforcement 仍 OFF）**：在高危域的 delta `erp-*.action-auth.xml`（`x:extends` 生成基，非生成文件）为最高危敏感动作声明**独立 per-action FNPT 权限点**（不坍缩进泛化 `mutation`），并用 `<resource ... roles="...">` 属性承载**静态 role-resource 种子**（Nop 原生静态映射，运行时并入 `permissionToRoles`，等价 `nop_auth_role_resource` 静态种子）：

| 域 | 实体 | 独立 FNPT 点 | roles 种子（角色） |
|----|------|-------------|------------------|
| finance | ErpFinVoucher | `:post` / `:reverse` | 财务员 |
| finance | ErpFinAccountingPeriod | `:closePeriod` / `:reverseClose`（反结账 kill-switch） | closePeriod=财务员 / reverseClose=管理员 |
| finance | ErpFinBadDebt | `:writeOff` / `:reverseApprove` | writeOff=财务员 / reverseApprove=管理员 |
| b2b | ErpB2bEdiDoc | `:markSent` / `:cancel` | markSent=B2B 对账员 / cancel=B2B 管理员 |
| b2b | ErpB2bAsn | `:handleInboundWebhook`（入站 webhook 高危） | B2B 管理员 |
| manufacturing | ErpMfgWorkOrder | `:start` / `:close` / `:cancel` | 生产主管 |
| inventory | ErpInvStockMove | `:confirm` | 库管员 |
| inventory | ErpInvLandedCost | `:approve` | 库管员 |
| human-resource | ErpHrLeaveRequest | `:approve` | HR 专员 |
| human-resource | ErpHrSalary | `:approve`（薪酬机密）/ `:markPaid` | 薪酬审批人 |

**当前运行时行为不变**：`nop.auth.enable-action-auth=false` 保持——声明层仅"已就绪可授权"，不拦截任何调用。`roles` 属性在 enforcement OFF 时不生效，仅在翻转后并入 `permissionToRoles` 校验。

**高危动作升级路径（successor）**：`reverseClose`（反结账）/ `writeOff`（坏账核销）/ `handleInboundWebhook`（B2B 入站）为最高危——其 FNPT 点已声明并 seed 给管理员/B2B 管理员，但**真正 enforcement 翻转**为 config-gated successor（见下方"Deferred"），须人工批准 + 灰度 + 负向隔离测试后分域启用。

**灰度推进路线（staged rollout，successor）**：finance/b2b/mfg/inventory/hr 为首批落地（含全部 A3.6 点名的最高危动作：reverseClose/writeOff/handleInboundWebhook + 各域敏感状态迁移）；其余（pur/sal 审批集 / mfg approve subcontract / assets 处置 / 全 EDI 生命周期 等）的 per-action FNPT 声明随 enforcement 灰度分批补齐（触发条件 = 该域 `enable-action-auth=true` 灰度批准前）。新增高危实体应按本模式直接声明独立 FNPT 点（API 命名约定见 `domain-design-guidelines.md §16A`）。

### 浏览器层审批路径已知限制（xwf 4 实体）

> 来源：plan `2026-07-09-2330-1` 权威裁决（NOT FEASIBLE）；M-2（plan `2026-07-20-2200-1`）落地补充。

**4 个 `useWorkflow="true"` 实体的 xwf 审批轴（Payment / Receipt / Disposal / Salary）目前在浏览器层 E2E 不可达**——根因是 nop-wf 的 `WorkflowEngineImpl.newSteps` fallback 在浏览器层调用 `submitForApproval` 时为 submit step 委托 `sysUser(0)` 作 step owner，但 `NopAuthUser.userId` 因 `tagSet="seq"` 覆盖显式 "0" 为 UUID（实测 `3d0538b1...`），致 `allowCallByUser:1053` 拒绝 nop uuid。

**影响范围（4 实体经 xwf 的 approve/submit 路径）：**

| 域 | 实体 | xwf 文件 | 影响 |
|----|------|---------|------|
| finance | ErpFinPayment | `payment-approval/v1.xwf` | 浏览器层 `submitForApproval`→`approve` 链不可达；后端 BizModel `approve_direct` / DIRECT 三轴审批仍可用（不依赖 xwf） |
| finance | ErpFinReceipt | `receipt-approval/v1.xwf` | 同上 |
| assets | ErpAstDisposal | `disposal-approval/v1.xwf` | 同上；资产处置审批走 DIRECT 三轴 |
| hr | ErpHrSalary | `salary-approval/v1.xwf`（三级链） | 同上；薪资审批走 DIRECT 三轴 |

**替代路径**：4 实体的 DIRECT 三轴审批状态机（`approveStatus` DIRECT 路径，见 `docs/plans/2026-07-05-0540-3`）不依赖 xwf，浏览器层 E2E 可达且全绿。需要多级审批链的业务场景在浏览器层目前无法验证（仅后端单测覆盖）。

**解除条件（满足任一即可）：**

1. nop-wf 平台修复 `sysUser(0)` 在浏览器层的物化路径（nop uuid → 真实 userId=0）
2. nop-wf 提供 API 允许浏览器层显式指定 step owner（破坏审批隔离的设计原则，**不推荐**）
3. 4 实体改回 DIRECT 审批，移除 `useWorkflow="true"`（产品决策）

**详细裁决与探针证据**：见 `docs/plans/2026-07-09-2330-1-use-workflow-browser-e2e-feasibility.md`（plan 内 §Closure Audit Evidence）。

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
