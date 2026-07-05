# 审批流集成（ERP 应用层）

## 定位

本文是 nop-app-erp 应用层对接平台审批能力的**落位说明**：哪些 ERP 单据使用审批、ERP 业务联动如何接、首批落地范围。

**平台级设计权威在 nop-entropy**（不在本文）：
- 平台设计：`../nop-entropy/ai-dev/design/nop-wf/approvable-entity-design.md`（`use-approval` tag、`IApprovableBiz` 接口、objMeta 流程配置、codegen 骨架、两流正交分离、状态归业务处理、wf 回调串联、DIRECT/WORKFLOW 双模）
- 使用指南：`../nop-entropy/docs-for-ai/03-runbooks/enable-approval-on-entity.md`（让实体具备审批能力的分步手册）

本文只记录 ERP 特有的落位决策。审批四态语义、模式策略、委托规则归 `approval-framework.md`。

## 产品化设计原则：配置即切换

本框架的核心设计原则是：**审批复杂度通过配置升迁，不通过代码分支**。同一个实体、同一套 Processor、同一套 xbiz bridge，通过 xmeta 一个属性值切换 DIRECT ↔ WORKFLOW：

```
xmeta 无 wf:wfName  →  DIRECT 模式（单级审批，approval-support.xbiz 标准 source 直接处理）
xmeta 有 wf:wfName  →  WORKFLOW 模式（多级审批，nop-wf 引擎驱动，wf 回调触发标准 action）
```

Java Processor 和 xbiz bridge 对两种模式无感知——Processor 只关心"审批通过后执行业务联动"，不关心 approveStatus 由谁迁移。配置升迁时 Processor 代码零改动。

## ERP 审批能力使用方式

ERP 单据**必须**通过平台 `use-approval` 能力接入审批，禁止自建审批状态迁移或 Java `@BizMutation` 实现 approve/reject：

1. **ORM** 实体标 `tagSet="use-approval"`（codegen 自动生成 `I*Biz extends IApprovableBiz` + 标准 action + `flowInstanceId` 字段）。
2. **xmeta** 配 `wf:wfName`（`wf:` 命名空间属性）——**这是复杂度切换的唯一配置点**：
   - 无 `wfName` → **DIRECT**：approval-support.xbiz 标准 source 直接完成 approveStatus 迁移，无需 wf 引擎
   - 有 `wfName` → **WORKFLOW**：审批走 nop-wf 引擎，流程结束后 wf 回调触发标准 action，完成 approveStatus 迁移
   - `wfVersion` 不配，默认用最新版本；**不用配置表**，随 xmeta 走 Delta 定制，不同客户可配不同 wfName
3. **xbiz** 在自定义 xbiz 中覆盖 `approve` action source，通过 `inject()` 获取 Processor bean 注入 ERP 业务联动（见 §推荐模式：xbiz `<source>` inject Processor）。这段 xbiz 对 DIRECT/WORKFLOW 通用。
4. **WORKFLOW 专属** 另配 `.xwf` 流程定义文件（描述多级审批节点、路由、审批人规则），见平台使用指南。

具体步骤见平台 runbook `enable-approval-on-entity.md`。

## ERP 审批实体落位

审批模式与适用单据的策略归 `approval-framework.md §按单据类型配置`。首批接入 **WORKFLOW**（多级审批）的实体需加 `flowInstanceId` 字段：

| 实体 | 域 | 模式（由 `wf:wfName` 有无决定） | 待加字段 |
|------|----|------------------------------|---------|
| 付款单 | finance | WORKFLOW（配 wfName） | `flowInstanceId` |
| 收款单 | finance | WORKFLOW（配 wfName） | `flowInstanceId` |
| 资产处置 | assets | WORKFLOW（配 wfName） | `flowInstanceId` |

DIRECT 模式单据（采购订单、销售订单等）标 `use-approval`、不配 `wf:wfName`，即获得标准审批 action，无需 `flowInstanceId`。无审批单据（库存移动单等）不标 `use-approval`。

> 现状（2026-07-06 收口）：11 域 ~37 审批实体已标 `use-approval`，`_*.xbiz` 已继承 `approval-support.xbiz`，`I*Biz` 已 `extends IApprovableBiz`。WORKFLOW 模式实体（付款/收款/资产处置/HR 薪酬）已落地——见 `docs/plans/2026-07-06-0315-1-workflow-approval-xwf.md`：4 实体 ORM 标 `useWorkflow="true"`（自动补 `nopFlowId`）+ xmeta 配 `wf:wfName` + 4 个 `.xwf` 流程定义（HR 三级 hr-review→finance-review→manager-approval；付款/收款/资产处置两级）+ wf 结束 listener 回调 approve/reject action。bootstrap actor 用 `actorType="all"`，精确角色路由待角色基础设施落地（Deferred）。
>
> 平台已知限制：`approval-support.xbiz` 标准 `submitForApproval` source 传 `bizObjId: entity.id`（Long），而 `WorkflowEngineImpl.removeStdStartParam` 强制 `(String)` 转换会 ClassCastException。ERP 应用层 4 实体 xbiz `submitForApproval` 均覆盖标准 source 显式 `'' + entity.id` 转 String 规避（本计划 Non-Goal 不改平台源码）。

## ERP 业务联动约定

ERP 审批通过后的业务联动在 **xbiz 层注入**，不写 Java 钩子（平台机制见 nop-entropy 使用指南）。对接既有业财/库存机制：

| 联动 | 机制 | 文档 |
|------|------|------|
| 业财过账 | `PostingEvent` + `*PostingDispatcher`（单据 APPROVED 后组装 event 过账） | `docs/design/finance/posting.md` |
| 库存写入 | `IErpInvStockMoveBiz`（同事务强一致） | `docs/design/inventory/state-machine.md` |
| 单据状态机 | 三轴状态（docStatus/approveStatus/postedStatus） | `document-engine.md` + 各域 `state-machine.md` |

审批状态（`approveStatus`）由平台标准 action source 迁移，**联动代码只做业务，不改 approveStatus**。

### 推荐模式：xbiz `<source>` inject Processor（三层桥接）

当 `use-approval` 激活后，`approval-support.xbiz` 生成标准 approve/reject action 并**覆盖 Java `@BizMutation`**（xbiz 优先级高于 Java）。自定义 xbiz 通过 `<source>` 覆盖标准 action 并桥接到 Java Processor：

```
approval-support.xbiz (标准 action, 管理 approveStatus)
       ↓ x:override="replace"
自定义 xbiz (source: inject Processor, 注入业务联动)
       ↓ inject()
Java Processor (业务逻辑, 跨域编排)
```

**示例**（purchase 域 `ErpPurReceive.xbiz`，权威实现）：

```xml
<!-- 自定义 xbiz：x:override="replace" 重写标准 source，保留状态守卫 + 调 Processor 业务回调 + 设状态 -->
<mutation name="approve">
    <source x:override="replace"><c:script><![CDATA[
        const entity = thisObj.invoke("requireEntity", {id}, null, svcCtx);
        const status = entity.approveStatus;
        if (status !== 'SUBMITTED') {
            throw new NopScriptError("nop.err.wf.approve.invalid-status")
                .param("bizObjName", thisObj.bizObjName)
                .param("bizObjId", entity.id)
                .param("action", "approve")
                .param("currentStatus", status)
                .param("expectedStatus", "SUBMITTED");
        }
        inject('app.erp.pur.service.processor.ErpPurReceiveProcessor').onApproved(entity, svcCtx);
        entity.approveStatus = 'APPROVED';
        entity.approvedBy = svcCtx.getUserId();
        entity.approvedAt = now();
        return entity;
    ]]></c:script></source>
</mutation>
```

> **裁决**：`x:override="replace"` 为权威模式（非整源委托 `processor.approve()`）。状态守卫 + approveStatus/approvedBy/approvedAt 管理保留在 xbiz source 中（与 `approval-support.xbiz` 标准 source 对齐），仅业务联动（校验、库存触发、过账）经 `inject().onApproved()` 委托 Processor。Processor 变为纯业务回调（`onSubmit`/`onApproved`/`onReverseApproved`），不含任何 `setApproveStatus` 状态迁移。

这种三层桥接是 **产品化架构的关键**——审批状态迁移（approveStatus）归平台标准 action，业务逻辑归 Java Processor，`wf:wfName` 配置决定走 DIRECT 还是 WORKFLOW。三层各自独立变化，无需因为审批复杂度升迁而改动其他层。

| 维度 | 说明 |
|------|------|
| **类型安全** | Processor 是 Java bean，编译期检查 |
| **自然对齐** | xbiz action 覆盖 xbiz action，无需 `x:override="remove"` hack |
| **业务留在 Java** | Processor 内 protected 步骤可被派生 bean 覆盖（见 `processor-extension-pattern.md`） |
| **DIRECT/WORKFLOW 无感** | Processor 不感知审批模式，只处理审批通过后的业务联动 |
| **双向兼容** | 不标 `use-approval` 时 Java Processor 直接由 Java `@BizMutation` 调用，逻辑一致 |

### 替代方案与选择理由

| 方案 | 评价 | 选用场景 |
|------|------|----------|
| xbiz `<source>` inject Processor | **默认** | 审批逻辑稳定，无需拓扑可变编排 |
| xbiz `task:name`/`task:path` → task.xml | **灵活定制** | 审批通过后的业务联动需要分步 Delta 覆盖（不同客户重排校验→规则→过账顺序） |
| Java `@BizMutation` + xbiz `x:override="remove"` | **禁止** | 脱离平台审批标准流程，失去产品化配置升迁能力 |
| xbiz `<observes>` append | 辅助 | 仅追加广播联动（通知、日志），不改变主逻辑 |

### 灵活切换：从 xbiz `<source>` 到 `task:name`

需要灵活编排时，在 xbiz 层做一行改动即可——不需要改 Java Processor：

```xml
<!-- 默认：xbiz <source> x:override="replace" 保留守卫 + inject Processor 业务回调 -->
<mutation name="approve">
    <source x:override="replace"><c:script><![CDATA[
        const entity = thisObj.invoke("requireEntity", {id}, null, svcCtx);
        if (entity.approveStatus !== 'SUBMITTED') { /* throw guard error */ }
        inject("...Processor").onApproved(entity, svcCtx);
        entity.approveStatus = 'APPROVED';
        entity.approvedBy = svcCtx.getUserId();
        entity.approvedAt = now();
        return entity;
    ]]></c:script></source>
</mutation>

<!-- 灵活：xbiz task:name → task.xml，同一套 Processor -->
<!-- 编译期由 biz-gen:TaskFlowSupport 展开（arg/return/source 自动生成），不指定 version 取最新 -->
<mutation name="approve" x:override="replace"
          task:name="ErpPurReceive/approve"/>
<!-- 也可用 task:path 直接指定 VFS 路径 -->
<mutation name="approve" x:override="replace"
          task:path="/nop/task/ErpPurReceive/approve.task.xml"/>
```

task.xml 内通过 `inject()` 复用既有 Processor：

```xml
<!-- task.xml -->
<step name="validate">
    <source><![CDATA[
        const processor = inject("erpPurReceiveApproveProcessor");
        processor.validate(id, svcCtx);
    ]]></source>
</step>
<step name="postProcess">
    <source><![CDATA[
        inject("erpPurReceiveApproveProcessor").postProcess(id, svcCtx);
    ]]></source>
</step>
```

两种模式共享同一套 Java Processor 的 `protected` 方法，不需要预埋 `ITaskStepLib` 或双轨适配。xbiz 层的切换是 Delta 定制（`x:extends="super"` + `x:override="replace"`），不同客户可以走不同路径。

## 现有 Processor 迁移到三层桥接

当前 Processor（Java `@BizMutation` + Processor 模式）直接在 Java 层处理审批，包含了 approveStatus 迁移 + 业务联动。接入 `use-approval` 三层桥接后：

| 现有职责 | 迁移目标 | 说明 |
|----------|----------|------|
| approveStatus 迁移 | → 平台标准 action source | 从 Processor 中移除 |
| 业务校验（状态、权限等） | → 保留在 Processor | 变为纯业务校验 |
| 跨域联动（过账、库存等） | → 保留在 Processor | 不变 |
| xbiz action | → 新增 `<source>` inject Processor | 桥接层，一行 XLang |

迁移后 Processor 变为**审批模式无感知**——它不再关心自己是 DIRECT 还是 WORKFLOW，也不手动设置 approveStatus。当未来客户需求变化需要从 DIRECT 升迁到 WORKFLOW 时：
1. xmeta 加 `wf:wfName`
2. 配 `.xwf` 流程定义
3. Processor 零改动

这是产品化框架的核心价值：**配置升迁不涉及代码变更**。

现有孤立 task.xml（如 `ErpPurReceive/approve.task.xml`）未被 xbiz 引用且无法在 XLang 中编译，接入 `use-approval` 时删除。若日后需要灵活编排，在 xbiz 层改 `task:name` 引用新 task.xml 即可，Processor 无需预埋任何 task 感知代码。

## 相关文档

- 平台设计：`../nop-entropy/ai-dev/design/nop-wf/approvable-entity-design.md`
- 平台使用指南：`../nop-entropy/docs-for-ai/03-runbooks/enable-approval-on-entity.md`
- `approval-framework.md` — ERP 审批策略（模式/单据配置/委托/四态语义）
- `document-engine.md` — 单据三轴状态与流转基线
- `service-layer-orchestration.md` — task.xml 编排约定
- `docs/design/finance/posting.md` — 业财过账机制
