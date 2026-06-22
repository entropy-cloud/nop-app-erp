---
调研日期: 2026-06-22
来源: 13 个 ERP 项目源码实测（~/sources/erp/）
状态: 已完成（基于源码实测）
---

# ERP 业务流程实现分析：流程引擎 vs 状态变迁

> 本文档系统分析 13 个 ERP 项目如何实现业务单据的流转（审批、过账、取消等），区分**流程引擎（Workflow Engine）**与**状态变迁（State Transition）**两种范式，并给出对 nop-app-erp 的落地建议。

## 1. 核心概念区分

| 维度 | 流程引擎（Workflow Engine） | 状态变迁（State Transition） |
|------|---------------------------|---------------------------|
| **定义** | 独立的运行时子系统，管理流程实例、节点执行、条件分支、并行审批 | 业务对象自身的状态字段 + 代码中的转换逻辑 |
| **典型代表** | iDempiere AD_Workflow、WMES FlowRuntime、warm-flow（星云） | Odoo fields.Selection + action_confirm()、ERPNext docstatus + StatusUpdater |
| **流程定义** | 可视化设计器 / 数据库表 / JSON 模板 | 代码中的 if-else 或 Selection 字段 |
| **运行时跟踪** | 独立的 Process/Activity 实例表 | 仅状态字段本身（无独立运行时记录） |
| **并行/会签** | 原生支持（AND-split/join, fork/join） | 不支持或需自行实现 |
| **条件分支** | 节点间转移条件（数据比较、表达式） | 代码中的 if-else 分支 |
| **外部回调** | 节点执行时可触发 HTTP/邮件/子流程 | 需在转换方法中硬编码 |
| **审计追踪** | 独立的流程历史表 | 依赖通用 changelog 或无 |

## 2. 13 个项目分类总览

### 第一类：完整流程引擎（3 个）

| 项目 | 引擎 | 流程定义方式 | 运行时跟踪 | 并行/会签 |
|------|------|-------------|-----------|----------|
| **iDempiere** | 自研 AD_Workflow（OMG 规范） | AD_WF_Node/AD_WF_NodeNext 数据库表 + 可视化设计器 | AD_WF_Process + AD_WF_Activity | ✅ AND/XOR split/join |
| **Metasfresh** | 自研 de.metas.workflow（继承 ADempiere） | 同上 + REST API 层 | WFProcess + WFActivity | ✅ AND/XOR split/join |
| **WMES** | 自研 FlowRuntime | JSON 模板 + 可视化设计器 | oms_flowinstance + oms_flowinstancehis | ✅ fork/join 会签 |

### 第二类：可选/轻量流程引擎（2 个）

| 项目 | 引擎 | 流程定义方式 | 特点 |
|------|------|-------------|------|
| **星云 ERP** | warm-flow（Dromara BPM） | 按模块配置 processCode | BPM 可选开关，关闭时退回直接审批 |
| **管伊佳 ERP** | 插件式多级审批（API 调用） | `/api/plugin/workflow/` 接口 | 默认关闭，通过 SystemConfig.multiLevelApprovalFlag 控制 |

### 第三类：触发器式流程自动化（1 个）

| 项目 | 引擎 | 流程定义方式 | 特点 |
|------|------|-------------|------|
| **Dolibarr** | WorkflowManager 触发器 | 全局常量开关（admin 页面配置） | 无可视化设计器；触发器链式自动推进（报价→订单→发票→发货→开票） |

### 第四类：纯状态变迁（7 个）

| 项目 | 状态字段 | 转换逻辑 | 特点 |
|------|---------|---------|------|
| **Odoo** | fields.Selection + action_*() 方法 | 命令式 Python（guard + write） | 8.0 后移除 workflow engine；base.automation 做响应式触发 |
| **ERPNext** | docstatus(0/1/2) + StatusUpdater.status_map | eval 表达式驱动状态计算 | 状态由收货/开票百分比自动推导，非事件驱动 |
| **Tryton** | Workflow mixin + _transitions set | @transition 装饰器 + 声明式转换集 | 最优雅的状态变迁模式 |
| **赤龙 ERP** | approve_status(4 态) + status(业务态) | SQL UPDATE + guard check | 双轴状态（审批+业务），乐观锁 version |
| **若依 ERP v1** | checkedStatus(int: -1/0/1) | Controller 直接 setCheckedStatus | 最简单：无 guard，无乐观锁 |
| **若依 ERP v2** | status(String) + workflowId(Long) | 未实现（占位符） | PowerJob 仅做调度，非 BPM |
| **OCA/l10n-china** | — | — | 空仓库，无代码 |

## 3. 详细分析：每个项目的关键实现

### 3.1 iDempiere — 最完整的自研流程引擎

**流程引擎**: ✅ 完整 OMG 规范实现

**双状态机架构**:
- **StateEngine**（流程实例状态）: NotStarted → Running → Suspended → Completed/Aborted/Terminated
- **DocumentEngine**（单据动作状态）: Drafted → InProgress → Completed → Closed → Reversed

**核心表结构**（`AD_*` 前缀）:

| 表 | 职责 |
|---|---|
| `AD_Workflow` | 流程定义（6 种类型: General/Manufacturing/DocumentProcess/Quality/DocumentValue/Wizard） |
| `AD_WF_Node` | 流程节点（5 种动作: DocumentAction/EMail/AppsProcess/AppsReport/WaitSleep） |
| `AD_WF_NodeNext` | 节点间转移 |
| `AD_WF_NextCondition` | 转移条件 |
| `AD_WF_Process` | 运行时流程实例 |
| `AD_WF_Activity` | 运行时节点执行 |
| `AD_WF_Responsible` | 节点负责人（角色/用户/调用者） |

**关键类**（`org.compiere.wf.*`）:
- `MWorkflow.start()` (:761) — 启动流程实例
- `MWFProcess.startWork()` (:586) — 驱动流程执行
- `MWFActivity.performWork()` (:1072) — 执行节点动作，`Action="D"` 时调用 `doc.processIt(node.getDocAction())`
- `DocWorkflowManager.process()` (:81) — PO 保存时自动触发 DocumentValue 类型流程

**与单据集成**: 工作流节点可通过 `DocumentAction` 动作直接驱动单据状态转换（如节点配置 `DocAction="CO"` 则自动完成单据）。

**后台处理器**: `WorkflowProcessor` 定时唤醒挂起活动、调整优先级、发送告警。

**证据文件**:
- `/Users/abc/sources/erp/idempiere/org.adempiere.base/src/org/compiere/wf/MWorkflow.java`
- `/Users/abc/sources/erp/idempiere/org.adempiere.base/src/org/compiere/wf/MWFActivity.java`
- `/Users/abc/sources/erp/idempiere/org.adempiere.base/src/org/compiere/process/DocumentEngine.java`
- `/Users/abc/sources/erp/idempiere/org.adempiere.base/src/org/compiere/process/StateEngine.java`

### 3.2 Metasfresh — 现代化演进的流程引擎

**流程引擎**: ✅ 继承 ADempiere + 现代化重构

**与 iDempiere 的关键差异**:
- 旧 `org.compiere.wf.*` 包标记 `@Deprecated`，迁移至 `de.metas.workflow.*`
- 新增 `de.metas.workflow.rest-api` 模块，为移动端（拣货、制造、配送）提供 REST API 工作流
- `DocumentHandler` SPI（28 个实现）替代了 iDempiere 的 `DocAction` 接口直接实现

**现代化包结构**:

| 包 | 职责 |
|---|---|
| `de.metas.workflow` | 核心域模型（Workflow/WFNode/WFNodeAction/WFState） |
| `de.metas.workflow.execution` | 运行时引擎（WorkflowExecutor/WFProcess/WFActivity） |
| `de.metas.workflow.service` | DAO/Service 层 |
| `de.metas.workflow.rest-api` | 移动端 REST API |

**WFNodeAction 扩展**（比 iDempiere 多 3 种）: WaitSleep, UserChoice, SubWorkflow, SetVariable, UserWindow, UserForm, AppsTask, AppsReport, AppsProcess, DocumentAction, EMail, UserWorkbench

**证据文件**:
- `/Users/abc/sources/erp/metasfresh/backend/de.metas.adempiere.adempiere/base/src/main/java/de/metas/workflow/Workflow.java`
- `/Users/abc/sources/erp/metasfresh/backend/de.metas.adempiere.adempiere/base/src/main/java/de/metas/workflow/execution/WFActivity.java`
- `/Users/abc/sources/erp/metasfresh/backend/de.metas.business/src/main/java/de/metas/document/engine/impl/DocumentEngine.java`

### 3.3 WMES — 可视化流程设计器 + 运行时引擎

**流程引擎**: ✅ 完整自研（JSON 模板 + 可视化设计器）

**架构**:
- `sys_flowscheme` 表存储 JSON 流程模板（SchemeContent）
- `oms_flowinstance` 表跟踪运行中实例
- `FlowRuntime` 解析 JSON、遍历节点图、评估分支条件
- `FlowinstanceService` 编排审批/拒绝/撤回

**节点类型**: start → node → fork(会签开始) → join(会签结束) → end

**分支条件**: 数据驱动（`DataCompare`），支持 `=, !=, >, <, >=, <=, in, not in`，支持 `and/or` 逻辑组合

**会签模式**: "one"（任一通过）vs "all"（全部通过）

**外部回调**: 每个节点可配置 `ThirdPartyUrl`，执行时 HTTP POST 通知

**审计**: `oms_flowinstancehis`（转移历史）+ `oms_flowinstanceinfo`（操作日志）

**证据文件**:
- `/Users/abc/sources/erp/wmes/WaterCloud.Domain/Entity/FlowManage/FlowRuntime.cs`
- `/Users/abc/sources/erp/wmes/WaterCloud.Service/FlowManage/FlowinstanceService.cs`
- `/Users/abc/sources/erp/wmes/WaterCloud.Code/Flow/FlowNode.cs`

### 3.4 星云 ERP — 可选 BPM（warm-flow）

**流程引擎**: ✅ 可选（warm-flow，Dromara 生态轻量 BPMN 引擎）

**Maven 依赖**: `com.lframework:bpm-starter`（`xingyun-core/pom.xml:31`）

**按模块可配开关**: `PurchaseConfig.purchaseRequireBpm`（true=走 BPM，false=直接审批）

**BPM 集成模式**（Form 表延迟持久化）:
1. 创建时若开启 BPM → 数据写入 `*Form` 表 → 启动 BPM 实例
2. BPM 流程完成后 → `businessComplete()` 回调 → 数据从 Form 表复制到正式表 → 设置 APPROVE_PASS

**直接审批（BPM 关闭时）**: `approvePass()` / `approveRefuse()` 方法，乐观锁（LambdaUpdateWrapper）

**统一三态枚举**: CREATED(0) → APPROVE_PASS(3) / APPROVE_REFUSE(6)

**证据文件**:
- `/Users/abc/sources/erp/xingyun-erp/xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/purchase/PurchaseOrderServiceImpl.java`
- `/Users/abc/sources/erp/xingyun-erp/xingyun-core/pom.xml`

### 3.5 管伊佳 ERP — 插件式可选审批

**流程引擎**: ⚠️ 可选插件（非内建）

**双层审批**:
- **Tier 1（默认）**: `DepotHead.status` 字段（String: "0"未审核/"1"已审核/"2"完成/"3"部分），Service 方法 guard check
- **Tier 2（可选）**: `SystemConfig.multiLevelApprovalFlag` 开启后，前端显示 `/workflow` 菜单，调用 `/api/plugin/workflow/workflowTask/add`

**状态转换守卫**（`DepotHeadService.batchSetStatus()` :717-796）:
- 审核: 仅 status="0" 可审核
- 反审核: 仅 status="1" 且 purchaseStatus="0" 可反审核
- 完成/部分采购的单据不可反审核

**前端状态展示**: status="0" → 红色"未审核"，status="1" → 绿色"已审核"，status="9" → 橙色"审核中"（BPM 进行中）

**证据文件**:
- `/Users/abc/sources/erp/jsh-erp/jshERP-boot/src/main/java/com/jsh/erp/service/DepotHeadService.java`
- `/Users/abc/sources/erp/jsh-erp/jshERP-boot/src/main/java/com/jsh/erp/constants/BusinessConstants.java`

### 3.6 赤龙 ERP — 双轴状态 + 手动审批

**流程引擎**: ❌ 无（UI 驱动的直接状态更新）

**双轴状态模型**:
- `status`: 业务状态（NEW/CONFIRM/CANCEL/ALTER）
- `approve_status`: 审批状态（UNSUBMIT/SUBMIT/APPROVE/REJECT）
- `shipment_status` / `receipt_status`: 执行状态（N/Y/PART）

**转换逻辑**: Controller 接收 `approveStatus` 参数 → `*DaoImpl.updateApproveStatus()` 执行 SQL UPDATE

**乐观锁**: `version` 字段（+1 递增）

**批量一致性**: 审批同时更新 `status='CONFIRM'` + `approve_status='APPROVE'`（同一 SQL）

**证据文件**:
- `/Users/abc/sources/erp/redragon-erp/erp-parent/erp-order/src/main/java/com/erp/order/soa/dao/model/SoAgreementHeadBase.java`
- `/Users/abc/sources/erp/redragon-erp/erp-parent/erp-inv/src/main/java/com/erp/inv/input/dao/model/InvInputHead.java`

### 3.7 Odoo — 命令式状态变迁（无流程引擎）

**流程引擎**: ❌ 8.0 后移除（历史版本有 osv.workflow）

**状态变迁模式**: `fields.Selection` + 命令式方法

```python
state = fields.Selection([
    ('draft', 'Quotation'), ('sent', 'Quotation Sent'),
    ('sale', 'Sales Order'), ('cancel', 'Cancelled'),
], default='draft', tracking=True)
```

**转换方法**: `action_confirm()` → guard check (`state not in {'draft','sent'}` → UserError) → `self.write({'state': 'sale'})`

**变更追踪**: `mail.thread` + `tracking=True` → `_track_subtype()` → `mail.message.subtype` → 通知关注者

**可配置自动化**: `base.automation` 模块（`on_state_set` 触发器），但非流程引擎

**picking 状态派生**: `stock.picking.state` 由子 `stock.move` 状态计算（`_compute_state()`），非直接设置

**证据文件**:
- `/Users/abc/sources/erp/odoo/addons/sale/models/sale_order.py`
- `/Users/abc/sources/erp/odoo/addons/stock/models/stock_move.py`
- `/Users/abc/sources/erp/odoo/addons/base_automation/models/base_automation.py`

### 3.8 ERPNext — 量化驱动的状态计算

**流程引擎**: ❌ 无（Frappe Workflow 可选叠加层，默认未启用）

**三层状态模型**:
1. **docstatus** (int): 0=Draft, 1=Submitted, 2=Cancelled（Frappe 框架内建）
2. **status** (string): 由 `StatusUpdater.status_map` 基于百分比自动计算
3. **workflow_state** (string): 仅在启用 Frappe Workflow 时存在

**量化驱动**: `status = f(per_billed, per_delivered, per_received, docstatus, ...)`
- 例: PO `per_received >= 100 and per_billed < 100` → "To Bill"
- 例: SO `per_delivered >= 100 and per_billed >= 100` → "Completed"

**关键洞察**: 状态变化由子交易累积（收货/开票百分比）自动触发，而非显式"转换动作"。

**Frappe Workflow 叠加层**: 可选，数据库存储，定义 states + transitions + workflow_state_field；ERPNext 本身不预装任何 Workflow 定义。

**证据文件**:
- `/Users/abc/sources/erp/erpnext/erpnext/controllers/status_updater.py`
- `/Users/abc/sources/erp/erpnext/erpnext/buying/doctype/purchase_order/purchase_order.py`
- `/Users/abc/sources/erp/erpnext/erpnext/selling/doctype/sales_order/sales_order.py`

### 3.9 Tryton — 声明式状态机（最优雅）

**流程引擎**: ❌ 无（但 Workflow mixin 提供声明式状态机）

**Workflow mixin** (`trytond/model/workflow.py`):
```python
class Workflow(object):
    _transition_state = 'state'
    cls._transitions = set()  # {('draft','confirmed'), ('confirmed','done'), ...}

    @staticmethod
    def transition(state):
        # 装饰器：运行时自动校验当前状态是否允许转换
```

**使用方式**:
1. Model 声明 `_transitions = {('draft','confirmed'), ('confirmed','done'), ...}`
2. 方法用 `@Workflow.transition('done')` 装饰
3. 运行时自动校验，非法转换抛异常

**优势**: 转换规则集中声明 + 编译期可检查，比散落 if-else 更安全

**证据文件**:
- `/Users/abc/sources/erp/tryton/trytond/model/workflow.py`

### 3.10 Dolibarr — 触发器链式自动化

**流程引擎**: ⚠️ 触发器式（无可视化设计器）

**WorkflowManager 触发器** (`interface_20_modWorkflow_WorkflowManager.class.php`):
- 监听业务事件（PROPAL_CLOSE_SIGNED, ORDER_CLOSE, BILL_VALIDATE 等）
- 自动链式推进: 报价→订单→发票→发货→开票
- 所有规则可通过 admin 页面全局常量开关

**10 条自动链**:
1. 报价签约 → 自动创建订单
2. 订单关闭 → 自动创建发票
3. 订单开票 → 自动标记报价已开票
4. 发票验证 → 自动标记订单已开票
5. 发票验证 → 自动关闭/标记发货单
6. 发票付款 → 自动标记订单已开票
7. 发货验证 → 自动关闭订单
8. 收货验证 → 自动关闭采购订单
9. 工单创建 → 自动创建服务单
10. 各环节均有全局常量开关控制

**证据文件**:
- `/Users/abc/sources/erp/dolibarr/htdocs/core/triggers/interface_20_modWorkflow_WorkflowManager.class.php`
- `/Users/abc/sources/erp/dolibarr/htdocs/admin/workflow.php`

### 3.11 若依 ERP v1/v2 — 最简状态变迁

**流程引擎**: ❌ 无

**v1**: `checkedStatus` (int: -1=作废/0=待审/1=完成)，Controller 直接 set，无 guard

**v2**: PowerJob 做分布式调度（非 BPM）；WmsOrderType.workflowId 是占位字段，未实现

## 4. 横向对比矩阵

| 项目 | 流程引擎 | 可视化设计器 | 并行/会签 | 条件分支 | 外部回调 | 审计表 | 状态维度 | 乐观锁 |
|------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| iDempiere | ✅✅ | ✅ | ✅ AND/XOR | ✅ | ✅ 邮件 | ✅ | 双机 | — |
| Metasfresh | ✅✅ | ✅ | ✅ AND/XOR | ✅ | ✅ REST | ✅ | 双机 | — |
| WMES | ✅✅ | ✅ | ✅ fork/join | ✅ 数据驱动 | ✅ HTTP | ✅ | 单机 | — |
| 星云 | ✅ | — | — | — | — | — | 三态 | ✅ |
| 管伊佳 | ⚠️ 可选 | — | — | — | — | — | 5 态 | — |
| Dolibarr | ⚠️ 触发器 | — | — | — | ✅ Webhook | — | 整数 | — |
| Odoo | ❌ | — | — | — | — | — | Selection | — |
| ERPNext | ❌ | — | — | — | — | — | 三层 | — |
| Tryton | ❌ | — | — | — | — | — | 声明式 | — |
| 赤龙 | ❌ | — | — | — | — | — | 双轴 | ✅ |
| 若依 v1 | ❌ | — | — | — | — | — | 整数 | — |
| 若依 v2 | ❌ | — | — | — | — | — | 字符串 | — |
| OCA | — | — | — | — | — | — | — | — |

## 5. 对 nop-app-erp 的落地建议

### 5.1 推荐方案：声明式状态机 + 可选审批流

基于 13 个项目的实测分析，推荐 **三层分离** 方案：

**第一层：声明式状态机（核心，必选）**

参考 Tryton 的 `_transitions` + `@transition` 模式，结合赤龙的双轴状态：

```
单据状态轴:
  - docStatus: draft → submitted → approved → posted → cancelled
  - approveStatus: unsent → pending → approved → rejected
```

在 nop 平台中用 **DSL/字典声明转换规则**，集中校验，避免散落 if-else。nop 的 `@BizMutation` 方法天然支持这种模式——方法入口即 guard check。

**第二层：业务事件钩子（业财一体，必选）**

参考 ERPNext 的 `on_submit` / Dolibarr 的触发器链：

- 统一 `onSubmit` / `onReverse` 钩子
- 业务单据过账走同一入口
- 业务取消即冲销 GL

**第三层：可选审批流（增强，可选）**

参考星云的 warm-flow 模式：

- 基础场景：直接审批（第一层状态机足够）
- 复杂场景：可插入 nop 流程引擎（若 nop 平台提供）或集成外部 BPM
- 审批流是状态机的"叠加层"，不替代状态机本身

### 5.2 不建议的做法

| 不建议 | 原因 | 反例 |
|--------|------|------|
| 直接在 Controller set 状态（无 guard） | 无校验、无审计、易出错 | 若依 v1 |
| 逗号分隔字符串存多值 | 反范式、难查询 | 管伊佳 accountIdList |
| 用 30s 轮询替代事件驱动 | 延迟高、资源浪费 | iDempiere AcctProcessor |
| 内嵌重量级 BPM 作为默认 | 大多数场景不需要 | — |

### 5.3 nop 平台适配点

| 设计点 | nop 机制 | 参考来源 |
|--------|---------|---------|
| 声明式状态机 | nop 规则引擎 / ORM 字典约束 | Tryton Workflow mixin |
| 审批状态分离 | `@BizMutation` 中双字段写入 | 赤龙 dual-axis |
| 业务事件钩子 | nop 消息总线 / 事件队列 | ERPNext on_submit + Dolibarr 触发器 |
| 可选 BPM 集成 | nop 插件机制（delta 可拔） | 星云 warm-flow |
| 乐观锁 | ORM version 字段 | 赤龙 + 星云 |
| 审计追踪 | nop changelog / 通用审计表 | iDempiere AD_WF_EventAudit |

## 6. 关键证据文件汇总

| 项目 | 关键文件 |
|------|---------|
| iDempiere | `org.compiere.wf/MWorkflow.java`, `MWFActivity.java`, `DocumentEngine.java`, `StateEngine.java` |
| Metasfresh | `de.metas.workflow/Workflow.java`, `execution/WFActivity.java`, `document/engine/impl/DocumentEngine.java` |
| WMES | `FlowManage/FlowRuntime.cs`, `FlowinstanceService.cs`, `Flow/FlowNode.cs` |
| 星云 | `PurchaseOrderServiceImpl.java`, `PurchaseConfig.java` |
| 管伊佳 | `DepotHeadService.java`, `BusinessConstants.java`, `WorkflowIframe.vue` |
| 赤龙 | `SoAgreementHeadBase.java`, `InvInputHead.java`, 各 `*DaoImpl.updateApproveStatus()` |
| Odoo | `sale_order.py`, `stock_move.py`, `base_automation.py` |
| ERPNext | `status_updater.py`, `purchase_order.py`, `sales_order.py` |
| Tryton | `trytond/model/workflow.py` |
| Dolibarr | `interface_20_modWorkflow_WorkflowManager.class.php`, `admin/workflow.php` |
