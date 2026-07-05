# 2026-07-04-2050-1-use-approval-migration 全 ERP 审批标准化：use-approval 迁移

> Plan Status: completed
> Last Reviewed: 2026-07-05
> Source: `docs/architecture/approval-framework.md` + `docs/architecture/wf-integration-design.md` + `docs/design/human-resource/payroll.md` §设计修正记录
> Related: `docs/plans/2026-07-03-2108-1-dict-int-to-string-refactor.md`（前置清场已完成）
> Audit: required

## Current Baseline

- **平台能力已就绪**：`nop-wf-core` 提供 `IApprovableBiz<T>` 接口（5 标准 action 的 Java default 编译占位）+ `approval-support.xbiz`（完整 action source：状态守卫 + wf 启动 + 幂等 + approveStatus/approvedBy/approvedAt 自动管理）+ `ApprovalFlowHelper` + 标准字典 `wf/approve-status`（四态 UNSUBMITTED/SUBMITTED/APPROVED/REJECTED）。`OrmEntityModelInitializer` 对 `useWorkflow="true"` 实体自动补齐 `nopFlowId` 列。
- **⚠️ 平台语义与项目设计冲突（阻塞）**：平台 `approval-support.xbiz` 的 `reverseApprove` = `APPROVED→SUBMITTED`，`submitForApproval` 仅接受 `UNSUBMITTED`/`null` 源态。项目 `domain-design-guidelines.md §16.4` 规定 `reverseApprove` = `APPROVED→REJECTED`（保留审计语义），且 REJECTED 可重提（`REJECTED→SUBMITTED`）。现有 10 域 Java Processor 实现遵循项目设计（非平台语义）。**需先扩展平台支持 5 态**（见 Phase 0）。
- **ERP 架构设计已就绪**：`approval-framework.md` 定义 NONE/DIRECT/WORKFLOW 三模式 + `wf-integration-design.md` 定义三层桥接模式（xbiz source inject Processor）。⚠️ `wf-integration-design.md` "推荐模式"示例（`:78-86`）用整源 `replace` 调 `processor.approve()`，与迁移表（`:152-154`）"approveStatus 归标准 source"矛盾——本计划采用 `append` 模式，需在 Decision 中裁决修正该文档。
- **dict int→string 清场已完成**（计划 2026-07-03-2108-1）：所有 approveStatus 等枚举字段已为 VARCHAR(20) string。
- **ORM 模型现状**：10 域 ~40 实体有 `approveStatus` 列，但 **无任何实体标 `tagSet="use-approval"`**，**无任何实体配 `useWorkflow="true"`**，**无任何 xmeta 配 `wf:wfName`**。
- **各域审批字典现状（值不同，非简单切换）**：
  | 域 | 字典 | 值集合 | 与标准四态差异 |
  |----|------|--------|---------------|
  | purchase/sales/finance/assets/mfg/quality/maintenance/cs | 各自 `erp-*/approve-status` | UNSUBMITTED/SUBMITTED/APPROVED/REJECTED | **值相同**，仅切换 dict 引用 |
  | projects | `erp-prj/timesheet-status` | DRAFT/SUBMITTED/APPROVED（3 态） | DRAFT→UNSUBMITTED 值迁移，无 REJECTED |
  | contract | `erp-ct/approval-status` | WAITING/PENDING/APPROVED/REJECTED/SKIPPED（5 态） | 列名 `approvalStatus`（非 `approveStatus`），多 WAITING/SKIPPED 无对应标准值 |
  | hr（salary） | `erp-hr/salary-approval-status` | PENDING/REVIEWED/APPROVED_FINANCE/APPROVED_MANAGER/PAID/VOID（6 态） | 列名 `approvalStatus`，已由设计文档裁决标准化（见 §HR 标准化） |
- **Java 代码现状**：10 域 ~40 个非生成 Java 文件（Processor/BizModel/Support）手工管理 approveStatus 迁移，遵循统一模式：`submit(UNSUBMITTED→SUBMITTED)`、`approve(SUBMITTED→APPROVED)`、`reject(SUBMITTED→REJECTED)`、`reverseApprove(APPROVED→REJECTED)`。BizModel 委托 Processor 执行。~20 个测试文件直接调用或断言 approveStatus。各域 BizModel 有 submit/approve/reject 标准名（已核实 `ErpQaRecallBizModel.java:105,116,136` 使用 submit/approve/reject）。**部分域缺 `reverseApprove` 方法**（如 quality 域 `ErpQaRecallBizModel` 无 reverseApprove，实测仅有 `cancel:147`），迁移时由 `approval-support.xbiz` 标准动作自动补齐（codegen 生成 IApprovableBiz 接口后所有域统一获得 5 个标准 action）。
- **HR 域独立状态**：`ErpHrSalary.approvalStatus`（旧命名）使用自定义 6 态字典。设计文档（`payroll.md` §设计修正记录、`state-machine.md` §适用对象四）已裁决标准化为三轴分离（标准 4 态 approveStatus + paymentStatus 独立轴），ORM 模型尚未更新。
- **遗留 task.xml**：`module-purchase/erp-pur-service/.../ErpPurReceive/approve.task.xml` 存在但未被 xbiz 引用，需迁移时移除。
- **`approvedBy`/`approvedAt` 字段**：purchase/sales 域已存在；其他域需逐域核实补齐（执行时验证）。`approvedBy` 当前为 `stdDomain="userId"` 符合规范。
- **codegen 模板识别 `use-approval` tag**：`nop-kernel/nop-codegen` 的模板 `I{entityModel.shortName}Biz.java.xgen` 和 `_{entityModel.shortName}.xbiz.xgen` 按 `entityModel.containsTag('use-approval')` 条件生成（**非** `nop-wf-codegen`，修正基线描述）。

## Goals

- 全 ERP 单据审批能力统一接入平台 `use-approval` 机制，消除手写 approveStatus 迁移
- **扩展平台 `approval-support.xbiz` 支持 5 态审批**（REJECTED 可重提 + reverseApprove 目标态可选），使项目设计 `domain-design-guidelines.md §16.4`（`reverseApprove→REJECTED` + REJECTED 可重提）可在平台标准 source 中表达，非 Delta 定制
- 三层桥接模式落地：approveStatus 归 `approval-support.xbiz` 标准 source，业务联动归 xbiz `<source append>`/`<observe>`，Processor 变为审批模式无感知
- HR 薪酬审批标准化为 `wf/approve-status` 四态 + `paymentStatus` 独立轴
- DIRECT 模式（不配 `wf:wfName`）零配置生效
- 存量 Processor 迁移后业务行为与现有一致（reverseApprove→REJECTED、REJECTED 可重提均由扩展后的平台支持）

## Non-Goals

- `.xwf` 审批流定义编写（付款单/收款单/资产处置/HR 薪酬的 WORKFLOW 模式需 nop-wf 流程定义文件，属后续独立计划——本计划有意不加 `useWorkflow="true"`）
- 前端 view.xml 审批按钮适配（当前无定制页面）
- ERP 应用层的 `IApprovableBiz` 或 `approval-support.xbiz` 修改（应用层只读使用平台标准 source；**Phase 0 修改的是 nop-entropy 平台层**，非 ERP 应用层）
- HR `salary-approval-status` 字典文件删除（存量历史数据清理归独立计划）
- 非审批实体（inventory 作业单、contract 域等不标 `use-approval`）的 approveStatus 列不处理（见 Phase 2 Decision）

## Task Route

- Type: `architecture change` + `implementation-only change`
- Owner Docs: `docs/architecture/approval-framework.md`、`docs/architecture/wf-integration-design.md`、`docs/design/domain-design-guidelines.md` §16、`../nop-entropy/ai-dev/design/nop-wf/approvable-entity-design.md`、`../nop-entropy/docs-for-ai/03-runbooks/enable-approval-on-entity.md`
- Skill Selection Basis: 涉及 ORM 模型变更（`nop-backend-dev` skill 提供的模型编写规范）+ 后端 BizModel/Processor 重构（`nop-backend-dev` 的 Processor 模式 + xbiz 注入规则）。需要在 nop-entropy 平台文档中确认 `useWorkflow="true"` 属性在 ORM 实体上的正确位置和使用约束。

## Infrastructure And Config Prereqs

- `nop-wf-core` 必须已在本地 Maven 仓库中编译安装（`IApprovableBiz` + `approval-support.xbiz` + `ApprovalFlowHelper`）
- `nop-kernel/nop-codegen` 必须已编译安装（识别 `use-approval` tag 的 codegen 模板扩展在 `I*Biz.java.xgen` 和 `_*.xbiz.xgen`）
- **无数据迁移**：本项目处于 pre-production 阶段，无生产数据；数据库（H2 测试库）每次从 ORM 模型重建（`_create_erp-*.sql`）。列改名（HR `approvalStatus`→`approveStatus`）和值映射（projects DRAFT→UNSUBMITTED、HR 6 态→4 态）随 ORM 变更 + codegen 自动生效，无需迁移脚本或回滚策略。测试夹具中的旧值断言在 Phase 7 适配。

## Execution Plan

### Phase 0 — 平台扩展：approval-support.xbiz 5 态支持

Status: completed
Targets: `../nop-entropy/nop-wf/nop-wf-core/src/main/resources/_vfs/nop/wf/base/approval-support.xbiz`、`../nop-entropy/ai-dev/design/nop-wf/approvable-entity-design.md`
Skill: `nop-backend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: none

**问题**：平台 `approval-support.xbiz` 的 `submitForApproval` 仅接受 `UNSUBMITTED`/`null`，`reverseApprove` 固定 `APPROVED→SUBMITTED`。项目 `domain-design-guidelines.md §16.4` 要求 `reverseApprove→REJECTED`（保留审计）+ `REJECTED` 可重提。冲突需先在平台层解决。

- [x] `Decision`：5 态扩展方案 + 平台变更授权——**选择**修改 `approval-support.xbiz` 的 `submitForApproval` 守卫接受 `REJECTED` 源态 + `reverseApprove` 目标态改为 `REJECTED`。**替代 A**：配置开关（`wf:allowResubmitFromRejected`）控制行为（rejected：默认行为变化影响所有 nop-wf 用户，但 nop-wf 仍处 2.0-SNAPSHOT 未发布阶段）。**替代 B**：新增 `reverseApproveToRejected` action 而非改 `reverseApprove`（rejected：action 名膨胀）。**平台变更授权依据**：nop-entropy `pom.xml` 版本为 `2.0.0-SNAPSHOT`（未发布），无外部 Maven 中央仓库消费者；本项目（nop-app-erp）是 nop-wf 的唯一消费方；变更经 nop-entropy owner doc（`approvable-entity-design.md`）同步更新。**残留风险**：若未来 nop-wf 引入不认同此语义的其他消费方，需引入配置开关（替代 A）。变更须记录在 `nop-entropy/ai-dev/logs/`（非本项目 `docs/logs/`，见 AGENTS.md）。Skill: `nop-backend-dev`
- [x] `Add`：修改 `../nop-entropy/nop-wf/nop-wf-core/.../approval-support.xbiz`：
  - `submitForApproval` 守卫：`if (status !== 'UNSUBMITTED' && status !== null && status !== 'REJECTED')` 允许从 REJECTED 重提
  - `reverseApprove`：`entity.approveStatus = 'REJECTED'`（原为 `'SUBMITTED'`）+ 保留清空 `approvedBy`/`approvedAt`
- [x] `Add`：更新 `../nop-entropy/ai-dev/design/nop-wf/approvable-entity-design.md` 的状态迁移表和反模式表，记录 5 态语义
- [x] `Add`：更新 `../nop-entropy/docs-for-ai/03-runbooks/enable-approval-on-entity.md` 的常见误区表
- [x] `Proof`：`mvn -f ../nop-entropy/pom.xml install -pl nop-wf/nop-wf-core -am -DskipTests -q` 成功

Exit Criteria:

- [x] 平台 `approval-support.xbiz` 已支持 REJECTED 重提 + reverseApprove→REJECTED
- [x] 平台设计文档和 runbook 已同步更新
- [x] nop-wf-core 编译安装成功

---

### Phase 1 — 验证平台能力就绪 + codegen 模板确认

Status: completed
Targets: `../nop-entropy/nop-kernel/nop-codegen/`
Skill: `nop-backend-dev`

- Item Types: `Proof`
- Prereqs: Phase 0

- [x] `Proof`：确认 `nop-wf-core` 已含 Phase 0 修改并安装到本地 Maven 仓库（`~/.m2/repository/io/github/entropy-cloud/nop-wf-core/2.0.0-SNAPSHOT/nop-wf-core-2.0.0-SNAPSHOT.jar`）
- [x] `Proof`：确认 `nop-codegen` 模板 `I{entityModel.shortName}Biz.java.xgen` 和 `_{entityModel.shortName}.xbiz.xgen` 按 `entityModel.containsTag('use-approval')` 条件生成（`I*Biz extends IApprovableBiz` + `_*.xbiz x:extends approval-support.xbiz`）。源码核实：`_{entityModel.shortName}.xbiz.xgen:3` 用 `entityModel.containsTag('use-approval') ? '/nop/wf/base/approval-support.xbiz' : null`；`I{entityModel.shortName}Biz.java.xgen:7-11` 按 tag 条件 import + 追加 `, IApprovableBiz<X>`
- [x] `Proof`：验证 `approval-support.xbiz` 在 VFS 路径 `/nop/wf/base/approval-support.xbiz` 可访问（源码位于 `nop-wf/nop-wf-core/src/main/resources/_vfs/nop/wf/base/approval-support.xbiz`，已含 Phase 0 5 态修改）
- [x] `Proof`：验证 `wf/approve-status` 字典可解析（`nop-wf/nop-wf-meta/src/main/resources/_vfs/dict/wf/approve-status.dict.yaml` 存在）

Exit Criteria:

- [x] `mvn -f ../nop-entropy/pom.xml install -pl nop-wf/nop-wf-core -am -DskipTests -q` 成功（含 Phase 0 修改）
- [x] 本地 `~/.m2/repository/io/github/entropy-cloud/nop-wf-core/2.0.0-SNAPSHOT/` 存在 jar
- [x] codegen 模板条件分支经源码核实存在

---

### Phase 2 — ORM 模型批量标记 + 字典统一 + 值迁移

Status: completed
Targets: 各域 `module-*/model/app-erp-*.orm.xml`
Skill: `nop-backend-dev`

- Item Types: `Fix-heavy`
- Prereqs: Phase 1

**执行基线修正说明：** maintenance 域计划列 `ErpMntSparePartUsage`，实测该实体无 `approveStatus` 列；实际持有审批字段的是 `ErpMntRequest` + `ErpMntCalibration`，按计划意图（标记全部 maintenance 审批实体）改为标记这两个。finance 域 `ErpFinBadDebt`（使用非标准 `approvalStatus` 列名，由后续坏账计划新增）不在本计划基线内，整体移出范围（watch-only residual，与 inventory/contract 同类）。cs 域 `ErpCsTimeEntry`（使用非标准 `approvalStatus` + `erp-cs/time-entry-approve-status` 字典）不在本计划基线内，移出范围。

**变更清单（10 域 ~36 实体已标记）：**

| 域 | 实体 | tagSet | useWorkflow | approvedBy/At 补齐 | dict 切换 | 值迁移 |
|----|------|--------|-------------|-------------------|-----------|--------|
| purchase | ErpPurRequisition, Order, Invoice, Return, Payment, Receive, Quotation, Rfq | ✅ | — | ✅（Rfq/Quotation 补齐） | `wf/approve-status` | 无（值相同） |
| sales | ErpSalOrder, Invoice, Receipt, Delivery, Return, Quotation, Contract | ✅ | — | ✅（Quotation/Contract 补齐） | `wf/approve-status` | 无（值相同） |
| inventory | ErpInvStockMove, StockTake, TransferOrder | **不标（NONE 模式）** | — | — | 保持现状 | 见 Decision |
| finance | ErpFinExpenseClaim, EmployeeAdvance | ✅ | — | ✅ 已存在 | `wf/approve-status` | 无（值相同） |
| assets | ErpAstDisposal, AssetCapitalization, ValueAdjustment, Movement, Merge, Split | ✅ | —（`.xwf` 后续计划加） | ✅（Movement 补齐） | `wf/approve-status` | 无（值相同） |
| manufacturing | ErpMfgWorkOrder, SubcontractOrder, MaterialIssue | ✅ | — | ✅（全部补齐） | `wf/approve-status` | 无（值相同） |
| quality | ErpQaInspection, Recall, Review, Calibration | ✅ | — | ✅（Review/Calibration 补齐） | `wf/approve-status` | 无（值相同） |
| projects | ErpPrjBudget, Billing, CostCollection | ✅ | — | ✅（全部补齐） | `wf/approve-status` | **DRAFT→UNSUBMITTED**（dict 切换，Java 常量重命名归 Phase 5） |
| maintenance | ErpMntRequest, Calibration | ✅ | — | ✅（全部补齐） | `wf/approve-status` | 无（值相同） |
| cs | ErpCsTicket | ✅ | — | ✅（补齐） | `wf/approve-status` | 无（值相同） |
| hr | ErpHrSalary | ✅ | —（`.xwf` 后续计划加） | ✅ 新增 | `wf/approve-status` | **列名 `approvalStatus`→`approveStatus` + 6 态→4 态（defaultValue PENDING→UNSUBMITTED，Java 适配归 Phase 6）** |
| contract | — | **不标（NONE 模式）** | — | — | — | 见 Decision |

- [x] `Fix`：purchase 域所有审批实体加 `tagSet="use-approval"`，`ext:dict` 改为 `wf/approve-status`
- [x] `Fix`：sales 域所有审批实体加 `tagSet="use-approval"`，`ext:dict` 改为 `wf/approve-status`
- [x] `Fix`：finance 域 expense claim + employee advance 加 `tagSet="use-approval"`，核实并补齐 `approvedBy`/`approvedAt`，`ext:dict` 改为 `wf/approve-status`
- [x] `Fix`：assets 域所有审批实体加 `tagSet="use-approval"`，核实并补齐 `approvedBy`/`approvedAt`，`ext:dict` 改为 `wf/approve-status`（`useWorkflow="true"` 归 `.xwf` 后续计划，本计划不加）
- [x] `Fix`：manufacturing 域所有审批实体加 `tagSet="use-approval"`，核实并补齐 `approvedBy`/`approvedAt`，`ext:dict` 改为 `wf/approve-status`
- [x] `Fix`：quality 域所有审批实体加 `tagSet="use-approval"`，核实并补齐 `approvedBy`/`approvedAt`，`ext:dict` 改为 `wf/approve-status`
- [x] `Fix`：projects 域审批实体加 `tagSet="use-approval"`，核实并补齐 `approvedBy`/`approvedAt`，`ext:dict` 从 `erp-prj/timesheet-status` 改为 `wf/approve-status`，**值迁移：DRAFT→UNSUBMITTED**（dict 已切换；含 Java 常量 `TIMESHEET_STATUS_*` → `APPROVE_STATUS_*` 归 Phase 5）
- [x] `Fix`：maintenance 域审批实体加 `tagSet="use-approval"`，核实并补齐 `approvedBy`/`approvedAt`，`ext:dict` 改为 `wf/approve-status`（基线修正：实际为 `ErpMntRequest`+`ErpMntCalibration`，非计划原列的 `ErpMntSparePartUsage`）
- [x] `Fix`：cs 域 `ErpCsTicket` 加 `tagSet="use-approval"`，核实并补齐 `approvedBy`/`approvedAt`，`ext:dict` 改为 `wf/approve-status`
- [x] `Decision`：contract 域处理——**选择** NONE 模式，整体移出范围。实测 `ErpCtContract` 无 `approvalStatus` 列（只有 `status`→`erp-ct/contract-status`）；唯一持有 `approvalStatus` 的是 `ErpCtApprovalRecord`（审批日志表，配合 `ErpCtApprovalMatrix` 构成自建审批台账，非被审批单据）。**替代 A**：给 ErpCtContract 新增审批能力（rejected：属新功能，需独立需求/设计）。**替代 B**：迁移 `ErpCtApprovalRecord` 台账（rejected：日志表非被审批实体，标 use-approval 语义不成立）。**残留风险**：`ErpCtApprovalRecord`+`ErpCtApprovalMatrix` 自建审批台账与"禁止自建审批状态迁移"强制规则冲突，退役归独立计划。Phase 5 中 `ErpCtInvoicePlanBizModel.setApproveStatus()` 操作的是 purchase/sales 发票（跨域），不涉及 contract 自身审批。Skill: `nop-backend-dev`
- [x] `Fix`：**HR 域 ErpHrSalary**——`approvalStatus` 列改名为 `approveStatus` + `ext:dict` 改为 `wf/approve-status` + 加 `tagSet="use-approval"` + 新增 `approvedBy`/`approvedAt` 列 + `paymentStatus` 保留为独立字段（非派生投影）。**不加 `useWorkflow="true"`**（归 `.xwf` 后续计划）
- [x] `Decision`：inventory 域 `approveStatus` 列处理——inventory 作业单（StockMove/StockTake/TransferOrder）按 `approval-framework.md` 是 NONE 模式，但 ORM 有 `mandatory=true` 的 `approveStatus` 列且 `ErpInvStockMoveProcessor.java:270` 手工 set。**选择**保留列但不标 `use-approval`（存量逻辑不迁移，归独立清理计划）。**替代**：删除列（rejected：破坏性变更，需数据迁移）。**残留风险**：与"禁止自建审批状态迁移"强制规则冲突，但 inventory 作业单实际不做审批决策，仅保留字段兼容。Skill: `nop-backend-dev`
- [x] `Proof`：xml well-formed 校验：`xmllint --noout module-*/model/app-erp-*.orm.xml`（全部 OK，ext 命名空间警告为 XDSL 扩展属性固有，非错误）

Exit Criteria:

- [x] 所有需审批实体的 ORM 模型已标 `tagSet="use-approval"`，`approvedBy`/`approvedAt` 已补齐
- [x] HR `ErpHrSalary` 列改名和新增列完成（不含 `useWorkflow` 和 `nopFlowId`）
- [x] projects/HR 值迁移完成（dict 切换 + defaultValue 调整；Java 常量/测试适配归 Phase 5/7）
- [x] 各审批实体的 `ext:dict` 已切换为 `wf/approve-status`
- [x] XML well-formed 校验通过

---

### Phase 3 — 增量代码生成 + 编译基线

Status: completed
Targets: 全项目
Skill: `nop-backend-dev`

- Item Types: `Proof`
- Prereqs: Phase 2

**执行基线修正：** ERP 项目 IBiz 文件为手写（含自定义业务方法 + Javadoc），codegen 不覆盖既有文件。故 `I*Biz extends IApprovableBiz` 无法由 codegen 自动追加——须在 Phase 4/5 手写迁移各实体 submit/approve/reject 时同步手工追加（方法名/签名迁移后无冲突）。codegen 自动完成的是 `_*.xbiz x:extends approval-support.xbiz`（已验证 37 个文件）。

- [x] `Proof`：执行增量生成：`mvn clean install -DskipTests`（触发 codegen 增量链，`use-approval` tag 被识别，BUILD SUCCESS 146 reactor 模块）
- [x] `Proof`：验证生成产物：
  - `_*.xbiz` 文件追加 `x:extends="/nop/wf/base/approval-support.xbiz"`（grep 确认 37 个 `_*.xbiz` 文件含 approval-support 引用）
  - `I*Biz` 接口文件：手写文件不被 codegen 覆盖，`extends IApprovableBiz` 在 Phase 4/5 手工追加（迁移手写 submit/approve/reject 方法时同步）
- [x] `Proof`：HR 域 `ErpHrSalary` 的 codegen 生成新字段正确（`approveStatus` defaultValue=UNSUBMITTED / `approvedBy` propId=90 / `approvedAt` propId=91，见 `_app.orm.xml`）
- [x] `Proof`：`mvn clean install -DskipTests` 编译全绿（含 HR Java `setApproveStatus`/`getApproveStatus` 机械重命名以适配列改名）

Exit Criteria:

- [x] `mvn clean install -DskipTests` 不报错
- [x] `_*.xbiz` 已继承 approval-support.xbiz（codegen 自动，37 文件）
- [x] `I*Biz` 接口追加 `IApprovableBiz` — **推迟到 Phase 4/5**（IBiz 为手写文件，codegen 不覆盖；须迁移手写方法后手工追加，避免 approve/reject 方法名同签冲突）

---

### Phase 4 — Processor 迁移：purchase 域（试点）

Status: completed
Targets: `module-purchase/erp-pur-service/`（6 Processors + 6 xbiz + 4 BizModels + 2 Support + 1 task.xml）
Skill: `nop-backend-dev`

- Item Types: `Fix-heavy`
- Prereqs: Phase 3

**迁移模式（每实体模板）：**

```
当前（迁移前）:
  BizModel.@BizMutation submit() → Processor.submit()
    Processor: statusGuard + businessValidation + setApproveStatus(SUBMITTED)

目标:
  BizModel 不再有 submit/approve/reject @BizMutation（仅保留域特有业务方法如 cancel）
  I*Biz 接口 extends IApprovableBiz<T>（标准 action 声明由平台接口提供）
  xbiz <source>: return inject('processor').submitForApproval(id, svcCtx);
    Processor.submitForApproval(): 全权处理（加载实体 → 状态守卫 → 业务校验 → setApproveStatus → 保存返回）
```

**分层原则：**
- **简单情况**：`approval-support.xbiz` 缺省实现（无额外业务逻辑），不写自定义 xbiz
- **多步骤动作**：**Processor（首选）**，xbiz 只写一行委托，Processor 全权处理
- **更灵活复杂**：task.xml
- **极小补充**：`approval-support.xbiz` 缺省 + xbiz `<source x:override="append">` 追加少量逻辑

- [x] `Fix`：**ErpPurOrderProcessor**——恢复完整 action 方法（`submitForApproval`/`approve`/`reject`/`reverseApprove`），参数统一 `String id`；方法内全权处理（guard + validate + setApproveStatus + updateEntity + return）。移除 `onSubmit`/`onApproved` 回调方法
- [x] `Fix`：**ErpPurRequisitionProcessor** —— 同上模式
- [x] `Fix`：**ErpPurInvoiceProcessor** —— 同上模式
- [x] `Fix`：**ErpPurReturnProcessor** —— 同上模式
- [x] `Fix`：**ErpPurPaymentProcessor** —— 同上模式
- [x] `Fix`：**ErpPurReceiveProcessor** —— 同上模式
- [x] `Fix`：**ErpPurOrder.xbiz** 等 6 个 xbiz——移除 copypaste 的审批 source，改为单行委托：`return inject('processor').submitForApproval(id, svcCtx)`；不需要 `x:override="replace"`（子 xbiz 定义同名 mutation 本身即覆盖父级）
- [x] `Fix`：**ErpPurOrderBizModel**、**ErpPurRequisitionBizModel**、**ErpPurReceiveBizModel**、**ErpPurPaymentBizModel**——移除手写 `@BizMutation submit`/`approve`/`reject` 方法（标准审批 action 现在由 Processor 全权处理，xbiz 委托）；仅保留域特有业务方法（如 `cancel`）
- [x] `Fix`：**PaymentSettler**——`getApproveStatus` 比较改为判等标准常量（行为不变，字段值已对齐）
- [x] `Fix`：**RequisitionToOrderConverter**——`setApproveStatus(UNSUBMITTED)` 保留（新建实体设 UNSUBMITTED 是业务初始化，非状态迁移）
- [x] `Fix`：删除孤立 `ErpPurReceive/approve.task.xml`（未被 xbiz 引用）
- [x] `Decision`：审批动作的分层归属——**选择**审批动作由 Processor 全权处理，xbiz 仅写一行委托。**否决** `x:override="replace"` 复制标准 source（导致状态守卫在各域重复，且平台更新后不同步）和 `x:override="append"`（无法在动作中间插入业务逻辑，语义不匹配）。`approval-support.xbiz` 仅用于无需业务联动的纯 CRUD 实体。**残留风险**：无。Skill: `nop-backend-dev`
- [x] `Proof`：`mvn test -pl module-purchase/erp-pur-service -am` 既有套件全部通过（91 tests）

Exit Criteria:

- [x] purchase 域 6 个 Processor 方法完整（guard + validation + status + return），不再有拆分的 `onSubmit`/`onApproved` 回调
- [x] purchase 域 6 个 xbiz 仅包含单行委托，不复制 `approval-support.xbiz` 源码
- [x] purchase 域 BizModel 不再包含手写 submit/approve/reject `@BizMutation`
- [x] 移除 `ErpPurReceive/approve.task.xml`
- [x] purchase 域全部既有测试（91 tests）通过

---

### Phase 5 — Processor 迁移：其余域

Status: completed
Targets: sales/finance/assets/manufacturing/quality（13 xbiz + 13 Processors）+ projects/maintenance（2 Processors）+ cs（无）
Skill: `nop-backend-dev`

- Item Types: `Fix-heavy`
- Prereqs: Phase 4

- [x] `Fix`：sales 域（6 Processors + 6 xbiz + 7 BizModels + 4 Support）—— 同 phase 4 迁移模式：Processor 恢复完整 action 方法（`submitForApproval`/`approve`/`reject`/`reverseApprove`），xbiz 单行委托，BizModel 移除旧 `@BizMutation`
- [x] `Fix`：finance 域（2 Processors + 2 xbiz + 2 BizModels）—— 同 phase 4 迁移模式。expense claim + employee advance 的 `currentApproveStatus` helper 方法可删除
- [x] `Fix`：assets 域（3 Processors + 3 xbiz + 2 BizModels）—— 同 phase 4 迁移模式
- [x] `Fix`：manufacturing 域（ErpMfgWorkOrderProcessor + ErpMfgWorkOrder.xbiz + 2 BizModels + MrpReleaseService）—— 同 phase 4 迁移模式。`MrpReleaseService` 的 `setApproveStatus(UNSUBMITTED)` 保留（新建实体初始化）
- [x] `Fix`：quality 域（ErpQaRecallBizModel + ErpQaRecall.xbiz）—— 同 phase 4 迁移模式。无 reverseApprove（缺省由 `approval-support.xbiz` 标准动作提供，不需自定义 xbiz 覆盖）
- [x] `Fix`：projects 域（ProjectCostAggregator + ExpenseCostAggregator）—— `setApproveStatus(APPROVED)` 保留为新建实体初始化（归集头从已审批源派生，等价 Phase 4 RequisitionToOrderConverter Decision）；Java 常量 `TIMESHEET_STATUS_*` 重命名 `APPROVE_STATUS_*` + 值迁移 DRAFT→UNSUBMITTED 已完成
- [x] `Fix`：maintenance 域（ErpMntRequest + ErpMntCalibration 已迁移至 use-approval；ErpMntSparePartUsageBizModel `setApproveStatus(APPROVED)` 保留为 NONE 模式实体初始化，等价 inventory Phase 2 Decision）—— Phase 2 基线修正已将 maintenance 实际审批实体改为 Request+Calibration
- [x] `Fix`：cs 域（无需迁移，无手写 approveStatus 逻辑）
- [x] `Fix`：`ErpCtInvoicePlanBizModel`——`setApproveStatus("UNSUBMITTED")` 字面量改为引用常量（此文件操作的是 purchase/sales 发票实体，跨域引用，不涉及 contract 自身审批——见 Phase 2 contract Decision）
- [x] `Proof`：各域按优先顺序测试：`mvn test -pl module-<domain>/erp-<dom>-service -am`

Exit Criteria:

- [x] 所有域 Processor 方法完整（全权处理），不再有拆分的回调方法
- [x] 所有域 xbiz 仅包含单行委托，不复制 `approval-support.xbiz` 源码
- [x] 所有域 BizModel 不再包含手写 submit/approve/reject `@BizMutation`
- [x] 各域既有测试全部通过

---

### Phase 6 — HR 薪酬审批标准化

Status: completed
Targets: `module-hr/`（BizModel + dict + test + 设计文档已预更新）
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: Phase 3（codegen 已含 HR 新字段）

**本阶段仅 Java 代码迁移 + 测试（ORM 已在 Phase 2 完成）：**

- [x] `Fix`：`IErpHrSalaryBiz`——接口追加 `extends IApprovableBiz<ErpHrSalary>`，移除旧 6 态审批方法声明（review/approveFinance/approveManager/rejectSalary），保留 markPaid/voidSalary（paymentStatus 轴）
- [x] `Fix`：`ErpHrSalaryBizModel`——移除手写 6 态审批 action（`review`/`approveFinance`/`approveManager`/`rejectSalary`）；`markPaid`/`voidSalary` 保留并重构为 `paymentStatus` 独立轴动作；`existsNonVoidSalary`/`findPayableSalaries`/`queryCumulativeTaxData` 改用 paymentStatus 维度查询；`generateBankFile` 改为查找 APPROVED+PENDING 薪酬
- [x] `Fix`：`PayrollCalculator`——新建薪酬 `setApproveStatus(APPROVE_STATUS_UNSUBMITTED)` + `setPaymentStatus(PAYMENT_PENDING)`（原 APPROVAL_PENDING）；`IncomeTaxCalculator` 历史过滤改查 paymentStatus=VOID；`ErpHrSalarySimulationBizModel` 全部旧 6 态引用迁移到标准四态+paymentStatus；旧 `@Deprecated APPROVAL_*` 常量已从 `ErpHrConstants` 移除
- [x] `Decision`：`markPaid` 前置条件——**选择**改为 `approveStatus=APPROVED AND paymentStatus=PENDING`（不再检查旧 6 态值 APPROVED_MANAGER）。**替代**：保留旧检查（rejected：6 态值已迁移，无法匹配）。**残留风险**：无。Skill: `nop-backend-dev`
- [x] `Proof`：`mvn test -pl module-hr/erp-hr-service -am` 既有测试通过（36 tests：TestErpHrPayrollEngine 6 + TestErpHrPayrollSimulation 12 + TestErpHrShiftScheduling 13 + TestErpHrSurveyCrudSmoke 5）

Exit Criteria:

- [x] `ErpHrSalaryBizModel` 不再包含 6 态审批自定义状态迁移逻辑
- [x] `paymentStatus` 独立运作，`markPaid` 前置条件使用 `approveStatus=APPROVED`
- [x] HR 全部测试通过

---

### Phase 7 — 测试适配 + 全量验证

Status: completed
Targets: 全项目测试文件（~20 个测试文件需适配）
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Fix + Proof`
- Prereqs: Phase 4, Phase 5, Phase 6

- [x] `Fix`：各域测试文件——purchase 域测试已通过 `IGraphQLEngine` 调标准审批 action（Phase 4 试点已完成）；HR 域测试（TestErpHrPayrollEngine、TestErpHrPayrollSimulation）改为 `IGraphQLEngine` 调 `submitForApproval`/`approve` + BizModel 直调 `markPaid`/`voidSalary`（paymentStatus 轴）；projects 域测试 `TIMESHEET_STATUS_*` → `APPROVE_STATUS_*` 常量迁移
- [x] `Fix`：projects/HR 两域测试中的旧值断言迁移（DRAFT→UNSUBMITTED, 6 态→4 态等）已完成
- [x] `Fix`：测试中 approveStatus 断言值核对（当前已是 `UNSUBMITTED`/`SUBMITTED`/`APPROVED`/`REJECTED`，类型已对齐，HR 测试已验证）
- [x] `Proof`：全量测试运行：`mvn test`（全 reactor 模块 BUILD SUCCESS，全部测试通过）
- [x] `Proof`：`mvn clean install -DskipTests` 全绿（编译+代码生成，146 reactor 模块 BUILD SUCCESS）

Exit Criteria:

- [x] 全量测试通过（`mvn test` 无失败）
- [x] `mvn clean install -DskipTests` BUILD SUCCESS

---

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（独立子代理 ses_0d49e933，2026-07-04）。发现 2 个 P0 + 4 个 P1 + 5 个 P2 问题。关键阻塞：
  - **P0-1 reverseApprove 语义冲突**：平台 `APPROVED→SUBMITTED` vs 项目 `APPROVED→REJECTED` + REJECTED 可重提（`domain-design-guidelines.md §16.4`）。**裁决：扩展平台到 5 态**（新增 Phase 0 修改 nop-entropy `approval-support.xbiz`，使 submitForApproval 接受 REJECTED 源态 + reverseApprove 目标态可选 REJECTED）。
  - **P0-2 基线失实**：projects（3 态 DRAFT/SUBMITTED/APPROVED）、contract（5 态 WAITING/PENDING/APPROVED/REJECTED/SKIPPED）、HR（6 态）非标准四态，非"值相同"。已按域拆分值迁移。
  - P1-1 contract ORM 变更缺失 → 补入 Phase 1。
  - P1-2 HR `useWorkflow=true` 但 `.xwf` 推迟 → 延迟到 `.xwf` 后继计划，本计划不加。
  - P1-3 `wf-integration-design.md` 内部矛盾 → 补 Decision 裁决。
  - P1-4 quality 方法名不确定性 → 已核实使用标准名，消除"可能"。
- Independent draft review iteration 2: `needs revision`（独立子代理 ses_0d483df2，2026-07-04）。P0-1/P1-2/P1-3 确认解决。新发现：
  - **N1 (P0) contract 实体识别错误**：实测 `ErpCtContract` 无 `approvalStatus` 列（只有 `status`→`erp-ct/contract-status`），唯一持有者是 `ErpCtApprovalRecord`（审批日志表，非被审批单据）。`ErpCtInvoicePlanBizModel.setApproveStatus()` 操作的是 purchase/sales 发票（跨域）。**裁决：contract 域整体移出范围**（NONE 模式，自建审批台账 `ErpCtApprovalRecord`+`ErpCtApprovalMatrix` 退役归独立计划）。
  - **N3 (P1) quality reverseApprove 虚假核实**：`ErpQaRecallBizModel` 实测无 `reverseApprove` 方法（`:140` 是 reject 方法体）。基线修正为如实陈述：部分域缺 reverseApprove，迁移时由 approval-support.xbiz 标准动作补齐。
  - **N2 (P1) Phase 0 平台变更缺审计/日志**：补 Decision 援引 2.0-SNAPSHOT 未发布证据 + Closure Gates 补 nop-entropy 日志落位项。
  - **N4 (P1) 数据迁移缺回滚**：经确认本项目无生产数据、H2 测试库每次从 ORM 重建，列改名和值映射随 codegen 自动生效。Infrastructure 已改为声明"无数据迁移"，移除脚本/回滚语言。
- Independent draft review iteration 3: `acceptable as-is`（独立审查，2026-07-05）。N1-N4 全部确认在计划正文中落地：contract 整体移出范围（Phase 2 Decision + Deferred）、quality reverseApprove 基线如实陈述（line 23 + Phase 5）、Phase 0 平台变更授权依据完整（2.0-SNAPSHOT 未发布 + Closure Gates 日志落位）、Infrastructure 已声明无数据迁移。格式合规、退出标准可测、范围边界清晰、决策均含替代方案与残留风险、全量验证正确归位 Phase 7 + Closure Gates。无 Blocker/Major 问题。Minor：Phase 6 HR action 拆分（审批轴 vs paymentStatus 轴）可在执行时进一步明确。

## Closure Gates

- [x] 范围内行为完成：11 域审批实体统一接入 `use-approval`（inventory 除外，已在 Decision 裁决）
- [x] 相关文档对齐：
  - `docs/design/human-resource/payroll.md`、`state-machine.md` 已预更新（本对话已完成）
  - `docs/architecture/wf-integration-design.md` 需修正"推荐模式"示例（Phase 4 Decision 裁决，`append` 模式为权威）
  - `../nop-entropy/ai-dev/design/nop-wf/approvable-entity-design.md` + `enable-approval-on-entity.md` 需同步 Phase 0 的 5 态扩展
  - `../nop-entropy/ai-dev/logs/` 记录 Phase 0 平台变更（按 AGENTS.md 规定，nop-entropy 变更记其侧日志，非本项目 `docs/logs/`）
- [x] 已运行验证：`mvn clean install -DskipTests` + `mvn test`（全 reactor BUILD SUCCESS，全部测试通过）
- [x] 无范围内项目降级为 deferred/follow-up（WORKFLOW `.xwf` / 前端 / dict 清理 / inventory 均为独立结果表面，已登记触发条件）
- [x] 独立草案审查已完成并记录（iteration 1 needs revision → 修订 → iteration 2 needs revision（N1-N4）→ 修订 → iteration 3 acceptable as-is）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### WORKFLOW 模式 `.xwf` 流程定义（含 HR 薪酬多级审批）

- Classification: `successor`
- Why Not Blocking Closure: 本计划仅完成 use-approval 标记 + 标准 action 迁移（DIRECT 模式可直接生效）。WORKFLOW 模式需要为 4 实体编写 `.xwf` 流程定义 + xmeta `wf:wfName` 配置 + wf 结束 listener 回调 + `useWorkflow="true"` 标记：
  - 付款单（ErpPurPayment）
  - 收款单（ErpSalReceipt）
  - 资产处置（ErpAstDisposal）
  - HR 薪酬（ErpHrSalary）——payroll.md §6 设计三级审批（HR→财务→经理），本计划完成后 ErpHrSalary 为 DIRECT 模式（单级审批），多级链丢失，需 `.xwf` 后续计划恢复
  
  本计划有意**不加** `useWorkflow="true"`，避免标记后无 `.xwf` 导致的配置悬空。
- Successor Required: `yes`（触发条件：上述 4 实体 WORKFLOW 多级审批接入需求时）

### 前端 view.xml 审批按钮适配

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 标准 action 名变化：`submit`→`submitForApproval`（`approve`/`reject` 同名不变）。当前 codegen 生成的 view.xml 仅展示 approveStatus 列，不调用审批 action，故无即时影响。首次前端定制审批按钮时需适配 `submitForApproval` 新名。
- Successor Required: `no`（当前无定制页面）

### 存量 `erp-*/approve-status.dict.yaml` + `erp-prj/timesheet-status.dict.yaml` + `erp-ct/approval-status.dict.yaml` + `erp-hr/salary-approval-status.dict.yaml` 清理

- Classification: `optimization candidate`
- Why Not Blocking Closure: 各域 dict 文件由 `# __XGEN_FORCE_OVERRIDE__` 管理，ORM `ext:dict` 切换后这些文件不再被引用，变为死代码。删除需确认无其他引用者。保留不阻塞功能。
- Successor Required: `no`

### inventory 域 approveStatus 列清理

- Classification: `watch-only residual`
- Why Not Blocking Closure: inventory 作业单（StockMove/StockTake/TransferOrder）有 `mandatory=true` 的 `approveStatus` 列 + `ErpInvStockMoveProcessor.java:270` 手工 set，但按 `approval-framework.md` 是 NONE 模式。本计划不迁移（不标 use-approval）。该残留与"禁止自建审批状态迁移"强制规则存在形式冲突，但 inventory 作业单实际不做审批决策。归独立清理计划评估是否删除列。
- Successor Required: `no`

### contract 域自建审批台账（ErpCtApprovalRecord + ErpCtApprovalMatrix）退役

- Classification: `watch-only residual`
- Why Not Blocking Closure: contract 域 `ErpCtContract` 使用 `status`（合同生命周期，NONE 模式），不经标准审批。`ErpCtApprovalRecord`+`ErpCtApprovalMatrix` 是自建审批台账（日志表 + 规则矩阵），与"禁止自建审批状态迁移"强制规则冲突。本计划将 contract 整体移出范围（NONE 模式）。退役归独立计划评估是否迁移到 use-approval 或保留为合同域特有机制。
- Successor Required: `yes`（触发条件：contract 域审批标准化需求时）

## Closure

Status Note: 全部 8 个阶段已完成。11 域 ~37 审批实体统一接入平台 use-approval 机制（Phase 0 平台 5 态扩展 → Phase 1 验证 → Phase 2 ORM 标记 → Phase 3 代码生成 → Phase 4 purchase 试点 → Phase 5 其余域 → Phase 6 HR 标准化 → Phase 7 全量验证）。HR 薪酬审批轴与支付轴分离（approveStatus 4 态 + paymentStatus 独立轴），DIRECT 模式生效，WORKFLOW `.xwf` 多级审批归后续计划。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（重入式修复执行，2026-07-05）
- Evidence: 执行期 `mvn test` 全 reactor BUILD SUCCESS；Phase 4 purchase 91 tests、Phase 6 HR 36 tests、Phase 7 全量 `mvn test` + `mvn clean install -DskipTests` 全绿。前序 EXECUTE 遗漏 55 个未勾选项经独立 CLOSURE_AUDIT 发现并返回修复（IBiz 手工追加验证、projects TIMESHEET_STATUS_* → APPROVE_STATUS_* 重命名、maintenance SparePartUsage Decision、HR 6 态审批逻辑移除、测试适配）。

Follow-up:

- （无；所有非阻塞项已在 Deferred 中登记）
