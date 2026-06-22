---
调研日期: 2026-06-22
来源: 13 个 ERP 项目调研（见 erp-survey/ 同目录）+ ~/sources/erp 源码实测
状态: 已完成（基于源码实测归纳）
---

# ERP 流程实现横向分析：流程引擎 vs 状态变迁

> 本文档专题分析 13 个 ERP 项目中**业务流程如何驱动单据生命周期**——是使用独立的流程引擎（BPM/workflow engine），还是依赖状态字段变迁（state machine / status field），还是两者混合。
>
> **核心结论先行**：13 个项目中**无一使用 Activiti/Camunda 等标准 BPMN 流程引擎**。所有项目都以状态变迁为基础，区别在于状态变迁的"智能程度"——从最简单的 if-else 分支到声明式 DSL 再到独立自建 BPM 模块。只有**星云 ERP** 接入了独立的审批流程引擎（自建 flow_* 表），但也仅限审批流程，核心业务流转仍然靠状态字段。

## 一、分类框架

按"状态流转机制"从简单到复杂排列为四个层次：

| 层次 | 机制 | 代表项目 | 特征 |
|---|---|---|---|
| **L1 原始 if-else** | 硬编码 `if status == X` 分支 | 管伊佳、若依、Dolibarr | 无状态机抽象，散落在 Service 中 |
| **L2 内建状态机** | DocAction/DocumentEngine + 状态常量 | iDempiere、Metasfresh | 统一 DocumentEngine 驱动所有单据状态变迁 |
| **L3 声明式状态机** | DSL/装饰器声明合法转换 | Tryton、Odoo | 转换规则集中定义，运行时自动校验 |
| **L4 状态变迁 + 独立审批流程** | 状态字段 + 自建 BPM 流程引擎 | 星云 ERP | 核心流转靠状态，审批环节接 BPM |
| **L5 钩子驱动** | on_submit/on_cancel 统一钩子 | ERPNext | 单据状态变迁触发横切钩子（过账/库存/财务） |

## 二、逐项目深度分析

### 2.1 Odoo — 内建状态字段 + ORM 级别钩子（非独立流程引擎）

**机制类型**：L3 声明式状态机

**源码证据**：
- `stock/models/stock_move.py:107-115` — `state` 字段定义 7 种状态：
  ```
  draft → waiting → confirmed → partially_available → assigned → done → cancel
  ```
- 状态流转散布在 ORM 方法中（`_action_confirm()`、`_action_assign()`、`_action_done()`），**无集中状态机定义**
- `stock_picking.py` 的 `state` 同样是 Selection 字段（`draft/assigned/done/cancel`）

**关键观察**：
- Odoo **没有**独立的 Workflow 引擎——Odoo 6.x 曾有 `osv.workflow`，**自 Odoo 8.0 起彻底移除**（`odoo/osv/` 目录仅存 `__init__.py` + `expression.py`）
- 状态转换在 ORM 方法内硬编码（`action_confirm()` / `button_cancel()` 等方法内 `self.write({'state': ...})`）
- 通过 `mail.thread` 的 `tracking=True` 实现状态变更日志（`_track_subtype()` → `mail.message.subtype` 通知 followers）
- **`base.automation` 模块**（`addons/base_automation/models/base_automation.py:124`）提供可配置的触发-动作引擎，支持 `on_state_set` / `on_create` / `on_write` / `on_time` / `on_webhook` 等触发器——这是 Odoo 最接近"可配置工作流"的机制，但本质是**响应式触发器**，不是状态机
- **`stock.picking.state` 是 computed field**（`stock_picking.py:816` `_compute_state()`），由子 `stock.move` 状态聚合计算，非直接写入
- **结论**：纯状态字段 + 方法调用 + 可配置触发器，无流程引擎

**对 nop 启示**：Odoo 移除 workflow engine 的教训——对于进销存单据，状态字段 + 声明式转换规则足够，不需要独立流程引擎

### 2.2 ERPNext — 钩子驱动的状态变迁（on_submit/on_cancel 统一入口）

**机制类型**：L5 钩子驱动

**源码证据**：
- `erpnext/controllers/buying_controller.py` / `selling_controller.py` — 基类定义 `on_submit()` / `on_cancel()` 钩子
- `stock/doctype/purchase_receipt/purchase_receipt.py:379` — `on_submit` → `make_gl_entries()`
- `stock/doctype/delivery_note/delivery_note.py:492` — `on_submit` → `make_gl_entries()`
- `accounts/doctype/sales_invoice/sales_invoice.py:458` — `on_submit` → `self.make_gl_entries()`

**关键观察**：
- Frappe 框架提供 `on_submit` / `on_cancel` / `on_update` / `validate` 等**生命周期钩子**
- **所有业务单据通过统一钩子触发财务过账**——这是"横切关注点"而非"流程引擎"
- 单据状态变迁是**三层架构**：
  - **Layer 1**：`docstatus` 整数（0=Draft/1=Submitted/2=Cancelled）——Frappe 内建，所有 submittable 文档共享
  - **Layer 2**：`StatusUpdater` 类（`controllers/status_updater.py:21-178`）——基于 `status_map` 字典，用 **eval 表达式**按百分比（`per_received`/`per_billed`/`per_delivered`）计算可读 status（如 "To Deliver and Bill"、"Completed"）。**状态由子交易量累积驱动，不是事件驱动**
  - **Layer 3**：`StatusService` 领域服务（如 `purchase_order/services/status.py`）——处理 Hold/Closed/Re-open 等手动状态变更
- Frappe 的 `Workflow` DocType（可选覆盖层）支持 `workflow_state_field` + states + transitions，但 **ERPNext 不预装任何 Workflow 定义**——核心单据状态机完全靠 Layer 1-3
- 工单（Work Order）有 9 态状态机、Job Card 有 8 态——仍然是 docstatus + status_map 模式
- **结论**：docstatus 生命周期 + 百分比驱动的 StatusUpdater + 可选 Workflow 覆盖层

**对 nop 启示**：建立统一的 `on_post`/`on_reverse` 钩子——这比流程引擎更轻量，是 ERP 业财一体的核心模式

### 2.3 赤龙 ERP — 审批状态三轴 + 业务 Service 硬编码

**机制类型**：L1 原始 if-else（三轴状态分离）

**源码证据**：
- `ApInvoiceHead` 三轴状态：`status`(NEW/CONFIRM/CANCEL) + `approveStatus`(UNSUBMIT/SUBMIT/APPROVE/REJECT) + `paidStatus`(N/Y/PART)
- `ApInvoiceHeadServiceImpl.java:135-155` — `updateApproveStatus()` 方法中硬编码 `if (APPROVE) { autoCreateVoucher(...) }`
- `InvInputHeadServiceImpl.java:145` — 审批 `APPROVE` 分支调 `autoCreateVoucher("INPUT")`

**关键观察**：
- **三轴状态分离**是亮点（业务/审批/财务各自独立），但状态转换逻辑散布在各 Service 的 `updateApproveStatus()` 中
- 无集中状态机定义，无转换规则校验
- 审批后自动触发凭证生成 = 业务 Service 内硬编码 if-else
- **结论**：状态字段设计好，但转换机制原始（if-else 分支）

**对 nop 启示**：三轴状态分离值得借鉴（业务状态/审批状态/财务状态），但应升级为声明式转换规则（参见 Tryton 模式）

### 2.4 iDempiere — DocumentEngine 统一状态机 + 完整工作流引擎（最成熟的方案）

**机制类型**：L2 内建状态机 + 完整工作流引擎

**源码证据**：
- `org.adempiere.base/src/org/compiere/process/DocumentEngine.java` — **所有单据共享一个状态机引擎**
- 13 种 DocAction 常量：`ACTION_Prepare`/`ACTION_Approve`/`ACTION_Complete`/`ACTION_Void`/`ACTION_ReActivate`/`ACTION_Reverse_Accrual`/`ACTION_Reverse_Correct`/`ACTION_Close`/`ACTION_Unlock`/`ACTION_Post`
- 状态常量：`STATUS_Drafted`/`STATUS_Invalid`/`STATUS_InProgress`/`STATUS_Approved`/`STATUS_NotApproved`/`STATUS_WaitingPayment`/`STATUS_WaitingConfirmation`/`STATUS_Completed`/`STATUS_Reversed`/`STATUS_Closed`/`STATUS_Voided`
- `processDocument()` 方法（:220-427）根据 `DocAction` 分发到具体方法（`prepareIt()`/`approveIt()`/`completeIt()`/`voidIt()` 等）
- `completeIt()` 完成后检查 `MClient.isClientAccountingImmediate()`（:350）→ `postIt()`（:365）自动触发过账

**iDempiere 独有：完整的工作流引擎（AD_Workflow）**：
- `org.compiere.wf.MWorkflow` — 工作流定义模型，加载节点、管理转换、启动实例
- `org.compiere.wf.MWFProcess` — 运行时工作流进程实例，管理 `startWork()` / `checkActivities()` / `startNext()`
- `org.compiere.wf.MWFActivity` — 运行时节点执行（实现 `Runnable`），`performWork()` 分发 DocumentAction / EMail / AppsProcess / AppsReport / WaitSleep
- `org.compiere.wf.MWFNodeNext` + `MWFNextCondition` — 节点间转换 + 条件评估
- `org.compiere.wf.MWFBlock` — AND-split/join 并行块追踪
- `org.compiere.wf.DocWorkflowManager` — 文档保存时自动触发型工作流（DocumentValue 类型）
- `StateEngine`（`process/StateEngine.java`）— 基于 OMG Workflow State 规范的 6 态状态机（NotStarted/Running/Suspended/Completed/Aborted/Terminated）
- 工作流类型：General / Manufacturing / DocumentProcess / Quality / DocumentValue / Wizard
- 后台处理器 `WorkflowProcessor`（`server/WorkflowProcessor.java`）— 唤醒挂起的活动、动态优先级调整、发送警报
- **UI 设计器**：Swing `WorkflowGraphScene` + ZK `WFEditor`/`WFPanel` — 可视化工作流设计

**关键观察**：
- **这是 13 个项目中最完整的双状态机实现**——DocumentEngine 管单据生命周期，AD_Workflow 管业务流程编排
- 工作流节点可驱动文档操作（`Action="D"` → `doc.processIt(m_node.getDocAction())`），形成**工作流→文档状态的桥梁**
- `DocWorkflowManager` 支持文档保存时自动启动工作流（DocumentValue 类型），实现**事件驱动触发**
- 状态转换规则硬编码在 DocumentEngine 中（`isValidAction()` 方法），**不是声明式的**
- 过账在 `Complete` 后自动触发（`Immediate` 模式）或通过 `AcctProcessor` 30 秒轮询
- **结论**：双状态机引擎（DocumentEngine + AD_Workflow）是最成熟的方案，但转换规则硬编码

**对 nop 启示**：建立统一的 DocumentAction 接口 + DocumentEngine 驱动所有单据状态变迁——这是 nop 最值得借鉴的模式；但状态转换规则应改为声明式配置

### 2.5 Metasfresh — DocumentEngine 演进版 + 现代工作流包 + EventBus 异步

**机制类型**：L2 内建状态机（演进版）

**源码证据**：
- 与 iDempiere 共享 `DocAction`/`DocumentEngine` 内核（`de.metas.document.engine/`）
- 差异点：`PostingService.java`（过账入口）+ `DocumentPostingBusService.java`（事务提交后 EventBus 投递）
- 过账从 iDempiere 的 30s 轮询演进为 post-commit 事件驱动

**Metasfresh 独有：现代化工作流包（de.metas.workflow）**：
- `de.metas.workflow.WFNode` — 工作流节点，支持 12 种动作类型：DocumentAction / UserChoice / SubWorkflow / SetVariable / UserWindow / UserForm / AppsTask / AppsReport / AppsProcess / EMail / UserWorkbench / WaitSleep
- `de.metas.workflow.execution.WorkflowExecutor` — 入口：在文档上启动工作流，管理进程生命周期
- `de.metas.workflow.execution.WFProcess` — 运行时工作流进程：管理活动、转换、状态变化
- `de.metas.workflow.execution.WFActivity` — 运行时活动执行（1419 行），按动作类型分发
- `de.metas.workflow.rest-api` — **移动端工作流 REST API**（`WorkflowRestAPIService`），用于拣货/制造/分销工作流——这是 iDempiere 没有的现代化扩展
- 旧版 `org.compiere.wf.*` 已标记 `@Deprecated`，被 `de.metas.workflow.*` 替代

**关键观察**：
- 状态机引擎与 iDempiere 同源，但**工作流包已完全现代化重写**（从 `org.compiere.wf` → `de.metas.workflow`）
- 新增 REST API 工作流层，支持移动端审批/操作
- EventBus 异步过账是工程最佳实践
- **结论**：L2 + 现代工作流 REST API + EventBus，是最现代化的 ADempiere 系实现

### 2.6 Tryton — 声明式状态机（最优雅的设计）

**机制类型**：L3 声明式状态机

**源码证据**：
- `trytond/model/workflow.py:7-58` — **Workflow mixin**：
  ```python
  class Workflow(object):
      _transition_state = 'state'
      cls._transitions = set()  # {(from_state, to_state), ...}
      
      @staticmethod
      def transition(state):
          def check_transition(func):
              # 运行时校验：当前状态是否在 _transitions 允许的目标转换中
              for record in records:
                  current_state = getattr(record, cls._transition_state)
                  transition = (current_state, state)
                  if transition in cls._transitions:
                      filtered.append(record)
              # 执行业务逻辑后自动更新状态
              cls.write(list(to_update.keys()), {cls._transition_state: state})
  ```

**关键观察**：
- **转换规则集中声明**：`_transitions = {('draft','confirmed'), ('confirmed','done'), ...}`
- **装饰器自动校验**：`@Workflow.transition('done')` 标注方法，运行时自动校验合法性
- **声明式 + 编译期可检查**，比散落的 if-else 安全得多
- 业务 model 只需继承 `Workflow` mixin 即可获得状态机能力
- **结论**：最优雅的声明式状态机设计，是 nop 的首选参考

**对 nop 启示**：nop 的单据状态机应采用类似 Tryton 的声明式设计——转换规则用 DSL/配置文件集中定义，运行时自动校验

### 2.7 管伊佳 ERP — 纯 if-else + 可选插件审批

**机制类型**：L1 原始 if-else（含可选审批插件）

**源码证据**：
- `DepotHead.status`（0未审核/1审核/2完成/3部分）+ `purchaseStatus`（0未采购/2完成/3部分）
- `BusinessConstants.java:69-80` — 状态常量定义
- 状态转换散布在 `DepotHeadService.batchSetStatus()`（:717-796）中
- `ExceptionConstants.java:403-460` — 审核/反审核/强制结单的前置条件校验错误信息

**可选多级审批插件**：
- `SystemConfig.multiLevelApprovalFlag`（`SystemConfig.java:28`）控制开关
- 启用后前端显示 `/workflow` 菜单 + `WorkflowIframe.vue` 组件，调用插件 API `/api/plugin/workflow/workflowTask/add`
- 前端状态显示：`status=='9'` → "审核中"（orange tag），表示插件审批进行中

**关键观察**：
- 最简单的实现：状态字段 + Service 中 if-else
- 审批插件是**外部 API 调用**，非内建工作流引擎
- 适合小微进销存，但不适合复杂 ERP

### 2.8 若依 ERP — 若依脚手架 + 状态双维度

**机制类型**：L1 原始 if-else（双维度）

**源码证据**：
- `sales_order` 的 `checked_status`(审核) + `stock_status`(库存) 分离
- 状态转换在 Controller/Service 中硬编码
- zccbbg 版：`ServiceConstants.Status` = -1(作废)/0(待审核)/1(已完成)
- zybfly 版：引入 PowerJob（分布式任务调度，非 BPM），`WmsOrderType.workflowId` 字段预留但未实现

**关键观察**：
- 状态双维度是亮点，但实现机制原始
- v2 版的 `workflowId` 是占位符，无实际工作流引擎

### 2.9 星云 ERP — 状态变迁 + warm-flow BPM 审批流程（唯一使用 BPM 的项目）

**机制类型**：L4 状态变迁 + 独立审批流程（唯一使用 BPM 的项目）

**源码证据**：
- `xingyun-core/pom.xml:31` — Maven 依赖 `com.lframework:bpm-starter`（Dromara 生态的 warm-flow 轻量 BPMN 引擎）
- `PurchaseOrderServiceImpl.java` 导入 `org.dromara.warm.flow.core.dto.FlowParams` / `entity.Instance` / `listener.ListenerVariable` / `service.InsService`
- **状态枚举统一三态**：`CREATED(0)` / `APPROVE_PASS(3)` / `APPROVE_REFUSE(6)`（所有业务模块：PurchaseOrder/SaleOrder/PurchaseReturn/SaleReturn/RetailOutSheet 等共 13 个枚举）
- `PurchaseConfig` 实体：`purchaseRequireBpm`(是否启用) + `purchaseBpmProcessId` + `purchaseBpmProcessCode`（按模块配置）
- **BPM 集成流程**：
  1. 创建时若 `config.getPurchaseRequireBpm()` 为 true → 数据存入 `*Form` 草稿表 → 启动 BPM 实例（`insService.start()`）
  2. BPM 完成后回调 `PurchaseOrderBpmListener.businessComplete()` → 数据从 Form 表物化到正式表 → 调用 `approvePass()`
  3. 不启用 BPM 时直接插入正式表
- **直接审批/拒绝**：`approvePass()` / `approveRefuse()` 方法，用乐观锁（`LambdaUpdateWrapper` + `in(status, statusList)`）保证并发安全
- 所有业务模块（SaleOrder/PurchaseReturn/SaleReturn/ReceiveSheet/ScTransferOrder/StockAdjustSheet/TakeStockSheet/SettleSheet）都实现相同模式

**关键观察**：
- **这是 13 个项目中唯一接入独立 BPM 流程引擎的**（warm-flow，非 Activiti/Camunda）
- **BPM 仅用于审批流程**——核心业务流转（订单→收货→付款）仍然靠状态字段
- **Form 表延迟持久化**：BPM 期间数据在草稿表，完成后才物化到正式表——这是保证 BPM 中途取消不污染正式数据的好设计
- **结论**：BPM 只管审批，不管业务流转——这是正确的设计分层

**对 nop 启示**：nop 已有 `nop-wf` 流程引擎，星云的模式验证了"核心流转靠状态 + 审批接 BPM"的设计分层是行业共识

### 2.10 WMES / ZH-MES — 完整可视化流程设计器 + 运行时引擎

**机制类型**：独立流程引擎（可视化设计器 + 运行时）

**源码证据**：
- `WaterCloud.Domain/Entity/FlowManage/FlowinstanceEntity.cs` — 流程实例（`oms_flowinstance` 表），`IsFinish`：0=运行中/1=通过/2=撤回/3=驳回/4=退回
- `WaterCloud.Domain/Entity/SystemManage/FlowschemeEntity.cs` — 流程模板（`sys_flowscheme` 表），`SchemeContent` 存 JSON 定义
- `WaterCloud.Code/Flow/FlowNode.cs:6-12` — 节点类型：START / END / NODE / FORK(会签开始) / JOIN(会签结束)
- `WaterCloud.Code/Flow/FlowLine.cs` — 分支线支持条件比较（`=`/`!=`/`>`/`<`/`in`/`not in` + and/or 逻辑）
- `WaterCloud.Domain/Entity/FlowManage/FlowRuntime.cs` — **运行时引擎**：解析 JSON 方案、评估分支条件（`DataCompare`）、处理会签逻辑（`NodeConfluence`）、拒绝回退（`RejectNode`）
- `WaterCloud.Service/FlowManage/FlowinstanceService.cs` — 编排器：`CreateInstance()` / `Verification()` / `NodeVerification()` / `NodeReject()` / `CancleForm()`
- 审批人指定：ALL_USER / SPECIAL_USER / SPECIAL_ROLE / RUNTIME_SPECIAL_ROLE / RUNTIME_SPECIAL_USER
- **第三方回调**：每个节点可配 `ThirdPartyUrl`，执行时 HTTP POST 通知外部系统
- **审计追踪**：`oms_flowinstancehis`（转换历史）+ `oms_flowinstanceinfo`（操作日志）

**关键观察**：
- 这是 **13 个项目中唯一有完整可视化流程设计器的**
- 支持会签（fork/join）、条件分支、拒绝回退、第三方回调
- 但**业务实体没有 flowInstanceId 字段**——流程引擎是独立的审批层，不驱动业务状态
- **结论**：完整的审批流程引擎，但与业务状态机解耦

### 2.11 Dolibarr — 触发器驱动的跨实体工作流

**机制类型**：L1 状态字段 + 触发器链（跨实体自动化）

**源码证据**：
- `htdocs/core/triggers/interface_20_modWorkflow_WorkflowManager.class.php:37-677` — **工作流管理器触发器**
- 业务实体状态常量：Propal（`STATUS_DRAFT=0` → `STATUS_VALIDATED=1` → `STATUS_SIGNED=2` / `STATUS_NOTSIGNED=3` → `STATUS_BILLED=4`）、Commande（`STATUS_DRAFT=0` → `STATUS_VALIDATED=1` → `STATUS_SHIPMENTONPROCESS=2` → `STATUS_CLOSED=3`）、Facture（`STATUS_DRAFT=0` → `STATUS_VALIDATED=1` → `STATUS_CLOSED=2`）
- 每个实体有显式转换方法：`valid()` / `setDraft()` / `cloture()` / `cancel()` / `classifyBilled()`

**跨实体触发链**（`WorkflowManager`，全部可通过 admin 开关配置）：
1. 报价签单 → 自动创建订单（`WORKFLOW_PROPAL_AUTOCREATE_ORDER`）
2. 订单关闭 → 自动创建发票（`WORKFLOW_ORDER_AUTOCREATE_INVOICE`）
3. 发票验证 → 自动关闭关联订单/报价（金额匹配）
4. 发票付款 → 自动标记关联订单已结算
5. 发货验证 → 自动关闭订单（全部发货后）
6. 收货验证 → 自动关闭采购订单

**关键观察**：
- 没有可视化设计器，但有**可配置的跨实体触发链**——比纯 if-else 高一级
- 所有规则通过 admin 页面开关控制（`htdocs/admin/workflow.php`）
- 无条件分支、无会签、无并行——线性触发链
- **结论**：轻量级触发器工作流，适合中小 ERP 的跨实体自动化

### 2.12 OCA/l10n-china — 空仓库（无源码）

- 仓库仅含配置文件（`.copier-answers.yml`、`.pre-commit-config.yaml` 等），**无任何 Python 源码**
- 继承 Odoo 的状态字段 + 钩子模式，无独立流程
- 定位为中国本地化骨架（金税/发票/银行对账），尚未发布任何模块

### 2.13 若依 ERP（zybfly 版）— PowerJob 任务调度（非 BPM）

- 引入 `tech.powerjob:powerjob-worker-spring-boot-starter:4.3.6`（分布式任务调度框架）
- `WorkflowStandaloneProcessor` 是 PowerJob 工作流 demo/test，无业务逻辑
- `WmsOrderType.workflowId` 是外键占位符，无代码处理
- **结论**：无工作流引擎，PowerJob 仅用于任务调度

## 三、横向对比矩阵

### 状态流转机制对比

| 项目 | 状态机类型 | 状态字段数 | 转换规则校验 | 过账触发 | 审批流程 |
|---|---|---|---|---|---|
| **Odoo** | Selection 字段 + base.automation 触发器 | 7 态(stock.move) | 无集中校验，ORM 方法内 if-else | `_action_done()` 自动 | 无独立 BPM |
| **ERPNext** | DocStatus + StatusUpdater 百分比驱动 | 2-3 态(DocStatus) + 多态(WorkOrder 9态) | eval 表达式按交易量计算 | `on_submit` 钩子 | 无独立 BPM（可选 Workflow 覆盖层） |
| **赤龙** | 三轴状态(status+approveStatus+paidStatus) | 3×N | 无集中校验，Service if-else | `autoCreateVoucher` 审批触发 | 无独立 BPM |
| **iDempiere** | DocumentEngine + AD_Workflow 双引擎 | 10+ 态(DocStatus) + OMG 6 态(WF) | `isValidAction()` 硬编码 + 工作流节点转换 | `Complete` → `postIt()` 或 30s 轮询 | **完整 AD_Workflow 引擎**（含 UI 设计器） |
| **Metasfresh** | DocumentEngine + 现代工作流包 | 同 iDempiere | 同 iDempiere | EventBus post-commit | **de.metas.workflow + REST API** |
| **Tryton** | 声明式 `_transitions` set | 按 model 自定义 | **`@transition` 装饰器自动校验** | Workflow transition 时 | 无独立 BPM |
| **管伊佳** | int 状态字段 + 可选审批插件 | 4 态 + 审批状态 | 无 | Service 手动调用 | 插件 API 调用（可选） |
| **若依** | int 状态双维度 | 2×N | 无 | Service 手动调用 | 无 |
| **星云** | 状态字段 + **warm-flow BPM** | 3 态(CREATED/PASS/REFUSE) | BPM 流程定义驱动 | BPM 审批完成 | **warm-flow（Dromara BPMN）** |
| **WMES** | **完整可视化流程引擎** | 多态 | **JSON 方案 + 条件分支 + 会签** | 手动 | **可视化设计器 + 运行时引擎** |
| **Dolibarr** | 状态字段 + **触发器链** | 整数常量 | 无集中校验，触发器链跨实体 | 模板触发 | **WorkflowManager 触发器链**（可配置） |

### 是否使用独立流程引擎

| 项目 | 使用独立流程引擎 | 引擎类型 | 覆盖范围 |
|---|---|---|---|
| Odoo | ❌ | — | base.automation 仅触发器 |
| ERPNext | ❌ | — | 可选 Frappe Workflow 覆盖层（不预装） |
| 赤龙 | ❌ | — | — |
| iDempiere | ✅ | AD_Workflow（OMG 规范） | 6 种工作流类型，含 UI 设计器 |
| Metasfresh | ✅ | de.metas.workflow（现代化重写） | 含 REST API 移动端工作流 |
| Tryton | ❌ | — | 声明式状态机（非流程引擎） |
| 管伊佳 | ⚠️（可选插件） | 外部 API 调用 | 仅审批 |
| 若依 | ❌ | — | — |
| **星云** | **✅** | **warm-flow（Dromara BPMN）** | **仅审批流程** |
| **WMES** | **✅** | **自建可视化流程引擎** | **审批 + 条件分支 + 会签** |
| **Dolibarr** | ⚠️（触发器链） | WorkflowManager 触发器 | 跨实体自动化（非审批） |
| OCA/l10n-china | ❌ | — | 空仓库 |

**结论：13 个项目中 3 个有独立流程引擎（iDempiere/Metasfresh 完整、WMES 完整），1 个有 BPM（星云，仅审批），2 个有轻量级自动化（Dolibarr 触发器链、管伊佳 插件），其余 7 个完全依赖状态字段。没有任何项目使用 Activiti/Camunda 等第三方标准 BPMN 引擎。**

## 四、核心发现与设计启示

### 发现 1：ERP 的"流程"≠ BPMN 流程引擎

ERP 业务流转的本质是**单据状态变迁**，不是 BPMN 定义的"流程"：
- BPMN 流程引擎适合**跨系统、跨组织、长周期**的人工审批流
- ERP 单据流转是**短周期、确定性、可自动化**的状态变迁（订单→入库→付款）
- 两者是**不同层次的问题**：状态变迁管"业务逻辑"，BPM 管"审批决策"
- 即使有完整工作流引擎的 iDempiere/Metasfresh，核心业务流转仍靠 DocumentEngine 状态机，工作流引擎主要用于审批和特殊流程编排

### 发现 2：DocumentEngine 模式是行业标准（iDempiere/Metasfresh 范式）

- 一个统一的 `DocumentEngine` 服务所有单据类型
- 每种单据实现 `DocAction` 接口（`prepareIt`/`completeIt`/`voidIt`/`reverseIt`）
- 过账在 `Complete` 后自动触发
- **这是 nop 最值得借鉴的模式**——建立统一的 `IDocumentAction` + `DocumentEngine`

### 发现 3：声明式状态机是优雅解（Tryton 范式）

- 转换规则集中声明：`_transitions = {('draft','confirmed'), ('confirmed','done')}`
- 运行时自动校验：`@transition` 装饰器
- **nop 应采用此模式**——单据状态机用 DSL/配置声明转换规则

### 发现 4：BPM 只管审批，不管业务流转（星云验证）

- 核心业务流转（订单→入库→付款）靠状态字段 + 业务 Service
- BPM 只处理"需要人工决策的审批环节"
- nop 的 `nop-wf` 应定位为**审批流程引擎**，不替代业务状态机

### 发现 5：钩子是横切关注点的最佳实践（ERPNext 范式）

- `on_submit` / `on_cancel` 统一钩子触发财务过账
- **比在每个 Service 里硬编码 `autoCreateVoucher` 更解耦**
- nop 应建立统一的 `on_post` / `on_reverse` 钩子机制

### 发现 6：三轴状态分离值得借鉴（赤龙范式）

- `status`(业务状态) + `approveStatus`(审批状态) + `paidStatus`(财务状态)
- **三个维度正交，互不干扰**——这比单一 status 字段灵活得多
- nop 应采用此模式，但状态转换规则用 Tryton 式声明式定义

## 五、对 nop-app-erp 的综合推荐

| 设计层 | 推荐范式 | 源项目 | 实现建议 |
|---|---|---|---|
| **状态机引擎** | DocumentEngine 统一驱动 | iDempiere | nop 建立 `IDocumentAction` 接口 + `DocumentEngine` 服务 |
| **转换规则** | 声明式 `_transitions` | Tryton | 转换规则用 DSL/配置文件定义，运行时校验 |
| **三轴状态** | status + approveStatus + paidStatus | 赤龙 | 单据头三轴分离，正交独立 |
| **过账触发** | on_post/on_reverse 统一钩子 | ERPNext | 所有过账走同一入口，业务取消即冲销 |
| **审批流程** | nop-wf 流程引擎 | 星云 | nop-wf 只管审批决策，不替代业务状态机 |
| **异步过账** | EventBus post-commit | Metasfresh | 主事务落单据+posted=N，过账异步执行 |

### nop-app-erp 单据状态机设计建议

```
三轴状态设计：
  status:       draft → confirmed → completed → closed / cancelled
  approveStatus: unsubmitted → submitted → approved → rejected
  paidStatus:   unpaid → partial_paid → paid

DocumentEngine 驱动：
  IDocumentAction 接口定义 prepareIt/completeIt/voidIt/reverseIt
  DocumentEngine 根据当前状态 + 目标动作 → 合法性校验 → 执行 → 触发钩子

声明式转换规则（DSL/配置）：
  transitions:
    - from: draft, to: confirmed, action: prepare
    - from: confirmed, to: completed, action: complete
    - from: completed, to: cancelled, action: void
    - from: completed, to: reversed, action: reverse

钩子触发：
  on_complete → auto_post()（过账）
  on_reverse → auto_reverse_post()（冲销）
```

## 六、关键证据文件索引

| 项目 | 关键文件 | 证据内容 |
|---|---|---|
| Odoo | `stock/models/stock_move.py:107-115` | 7 态 state Selection 字段 |
| Odoo | `base_automation/models/base_automation.py:124` | base.automation 可配置触发器引擎 |
| ERPNext | `controllers/status_updater.py:21-178` | StatusUpdater 百分比驱动状态计算 |
| ERPNext | `controllers/buying_controller.py`, `general_ledger.py:34` | on_submit 钩子 + make_gl_entries |
| 赤龙 | `ApInvoiceHeadServiceImpl.java:135-155` | 三轴状态 + 审批触发 autoCreateVoucher |
| iDempiere | `process/DocumentEngine.java:79-427` | 统一状态机引擎，13 种 DocAction |
| iDempiere | `org.compiere.wf/{MWorkflow,MWFProcess,MWFActivity}.java` | 完整 AD_Workflow 工作流引擎 |
| iDempiere | `process/StateEngine.java:29-361` | OMG Workflow State 6 态状态机 |
| Metasfresh | `de.metas.workflow/{WFNode,WFProcess,WFActivity}.java` | 现代化工作流包 |
| Metasfresh | `de.metas.workflow.rest-api/` | 移动端工作流 REST API |
| Metasfresh | `PostingService.java`, `DocumentPostingBusService.java:104` | EventBus 异步过账 |
| Tryton | `model/workflow.py:7-58` | 声明式状态机 mixin |
| 星云 | `xingyun-core/pom.xml:31` + `PurchaseOrderServiceImpl.java` | warm-flow BPM 依赖 + 集成代码 |
| 管伊佳 | `DepotHeadService.java:717-796` + `SystemConfig.java:28` | if-else + 可选审批插件 |
| WMES | `FlowRuntime.cs` + `FlowinstanceService.cs` | 可视化流程引擎 + 运行时 |
| Dolibarr | `interface_20_modWorkflow_WorkflowManager.class.php:37-677` | 触发器链跨实体工作流 |
