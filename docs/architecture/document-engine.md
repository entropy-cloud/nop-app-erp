# DocumentEngine 统一状态机设计

## 目的

说明 ERP 系统中所有业务单据的统一状态机引擎设计。DocumentEngine 是驱动所有单据状态变迁的核心机制，参考 iDempiere/Metasfresh 的成熟模式，结合 Tryton 的声明式转换规则，为 nop-app-erp 提供统一、可扩展的单据生命周期管理。

本文件是 `workflow-vs-state-machine.md` 调研结论的落地设计。

## 设计背景

### 调研发现

从 13 个 ERP 项目的调研中发现：

| 发现 | 说明 | 参考项目 |
|------|------|----------|
| ERP 流程 ≠ BPMN 流程引擎 | ERP 单据流转是短周期、确定性、可自动化的状态变迁 | 全部 13 项目 |
| DocumentEngine 是行业标准 | 一个统一的状态机服务所有单据类型 | iDempiere、Metasfresh |
| 声明式状态机是优雅解 | 转换规则集中声明，运行时自动校验 | Tryton |
| 三轴状态分离值得借鉴 | 业务状态/审批状态/财务状态正交独立 | 赤龙 |
| BPM 只管审批，不管业务流转 | 核心流转靠状态字段，审批接 BPM | 星云 |
| 钩子是横切关注点的最佳实践 | on_submit/on_cancel 统一钩子触发财务过账 | ERPNext |

### 核心结论

- **不使用 Activiti/Camunda 等标准 BPMN 流程引擎**
- **建立统一的 DocumentEngine 驱动所有单据状态变迁**
- **采用声明式转换规则（DSL/配置）**
- **三轴状态分离（业务/审批/财务）**
- **过账通过统一钩子触发**

## DocumentEngine 架构

### 核心组件

```
DocumentEngine 架构
        │
        ├─► IDocumentAction 接口
        │      ├─ prepareIt()
        │      ├─ completeIt()
        │      ├─ voidIt()
        │      ├─ reverseIt()
        │      ├─ closeIt()
        │      ├─ reActivateIt()
        │      └─ postIt()
        │
        ├─► DocumentEngine 服务
        │      ├─ processAction(document, action)
        │      ├─ validateTransition(currentStatus, action)
        │      ├─ executeAction(document, action)
        │      └─ triggerHooks(document, action)
        │
        ├─► TransitionRegistry（声明式转换规则）
        │      ├─ transitions: Map<EntityName, Set<Transition>>
        │      ├─ isValidTransition(entityName, fromStatus, toStatus)
        │      └─ getAvailableActions(entityName, currentStatus)
        │
        ├─► DocumentHooks（横切关注点钩子）
        │      ├─ onPrepare(document)
        │      ├─ onComplete(document) → 触发过账
        │      ├─ onVoid(document) → 触发冲销
        │      ├─ onReverse(document)
        │      └─ onClose(document)
        │
        └─► DocumentStatus（三轴状态）
               ├─ docStatus（业务生命周期）
               ├─ approveStatus（审批状态）
               └─ paidStatus（财务状态）
```

## 三轴状态设计

### 状态轴定义

| 状态轴 | 字段名 | 说明 | 适用范围 |
|--------|--------|------|----------|
| 业务状态 | `docStatus` | 单据业务生命周期 | 所有业务单据 |
| 审批状态 | `approveStatus` | 审批流程状态 | 需审批单据 |
| 财务状态 | `paidStatus` / `postedStatus` | 收付款/过账状态 | 财务相关单据 |

### 业务状态（docStatus）

标准业务状态定义：

| 状态 | 编码 | 说明 | 可执行动作 |
|------|------|------|------------|
| 草稿 | DRAFT | 单据创建，可修改 | Prepare, Cancel |
| 已准备 | PREPARED | 单据准备完成，待审批/完成 | Complete, Void |
| 已完成 | COMPLETED | 单据业务完成，已生效 | Close, Reverse, Void |
| 已关闭 | CLOSED | 单据归档，不可操作 | — |
| 已取消 | CANCELLED | 草稿状态取消 | — |
| 已作废 | VOIDED | 已完成单据作废 | — |
| 已冲销 | REVERSED | 已完成单据冲销（生成反向凭证） | — |

### 审批状态（approveStatus）

审批状态与业务状态正交：

| 状态 | 编码 | 说明 |
|------|------|------|
| 未提交 | UNSUBMITTED | 草稿状态，未提交审批 |
| 已提交 | SUBMITTED | 已提交审批，待审核 |
| 已审核 | APPROVED | 审批通过 |
| 已驳回 | REJECTED | 审批驳回，可修改后重新提交 |

### 财务状态（paidStatus / postedStatus）

财务状态与业务状态正交：

| 状态 | 编码 | 说明 |
|------|------|------|
| 未过账 | UNPOSTED | 未生成凭证 |
| 已过账 | POSTED | 已生成凭证 |
| 未付款/收款 | UNPAID | 未核销 |
| 部分付款/收款 | PARTIAL_PAID | 部分核销 |
| 已付款/收款 | PAID | 全额核销 |

### 三轴状态关系

```
三轴状态关系图

业务状态轴：
  DRAFT → PREPARED → COMPLETED → CLOSED
         ↓           ↓           ↓
       CANCELLED   VOIDED      REVERSED

审批状态轴（与业务状态正交）：
  UNSUBMITTED → SUBMITTED → APPROVED
                           ↓
                        REJECTED → UNSUBMITTED（修改后重新提交）

财务状态轴（与业务状态正交）：
  UNPOSTED → POSTED
  UNPAID → PARTIAL_PAID → PAID

关键约束：
  - Complete 动作要求 approveStatus = APPROVED
  - Post 动作要求 docStatus = COMPLETED
  - Reverse 动作要求 postedStatus = POSTED
```

## 声明式转换规则

### 转换规则 DSL

转换规则用 DSL/配置文件集中定义：

```yaml
# 单据状态转换规则示例（purchase_order.yaml）
entity: ErpPurOrder
transitions:
  # 草稿 → 已准备
  - from: DRAFT
    to: PREPARED
    action: Prepare
    preConditions:
      - hasLines
      - hasValidPartner
    postHooks:
      - calculateAmounts
  
  # 已准备 → 已完成
  - from: PREPARED
    to: COMPLETED
    action: Complete
    preConditions:
      - approveStatus == APPROVED
    postHooks:
      - generateStockMove
      - triggerPosting
  
  # 已完成 → 已关闭
  - from: COMPLETED
    to: CLOSED
    action: Close
    preConditions:
      - allLinesProcessed
    postHooks:
      - archiveDocument
  
  # 已完成 → 已冲销
  - from: COMPLETED
    to: REVERSED
    action: Reverse
    preConditions:
      - postedStatus == POSTED
    postHooks:
      - generateReverseVoucher
      - reverseStockMove
  
  # 已完成 → 已作废
  - from: COMPLETED
    to: VOIDED
    action: Void
    preConditions:
      - postedStatus == UNPOSTED
    postHooks:
      - cancelRelatedDocuments
  
  # 草稿 → 已取消
  - from: DRAFT
    to: CANCELLED
    action: Cancel
    postHooks:
      - cleanupDraftData
```

### 转换规则注册

转换规则通过 nop IoC 注册：

```java
@Component
public class DocumentTransitionRegistry {
    @Inject
    private Map<String, IDocumentTransitionProvider> providers; // key = entityName
    
    public boolean isValidTransition(String entityName, String fromStatus, String action) {
        IDocumentTransitionProvider provider = providers.get(entityName);
        if (provider == null) return false;
        return provider.getTransitions().stream()
            .anyMatch(t -> t.getFrom().equals(fromStatus) && t.getAction().equals(action));
    }
    
    public Set<String> getAvailableActions(String entityName, String currentStatus) {
        IDocumentTransitionProvider provider = providers.get(entityName);
        if (provider == null) return Collections.emptySet();
        return provider.getTransitions().stream()
            .filter(t -> t.getFrom().equals(currentStatus))
            .map(Transition::getAction)
            .collect(Collectors.toSet());
    }
}
```

## IDocumentAction 接口

### 接口定义

```java
public interface IDocumentAction {
    /**
     * 准备单据：校验数据完整性，计算金额，进入待审批/待完成状态
     */
    DocumentActionResult prepareIt();
    
    /**
     * 完成单据：业务生效，触发库存/财务联动
     */
    DocumentActionResult completeIt();
    
    /**
     * 作废单据：已完成单据作废（未过账）
     */
    DocumentActionResult voidIt();
    
    /**
     * 冲销单据：已过账单据生成反向凭证
     */
    DocumentActionResult reverseIt();
    
    /**
     * 关闭单据：归档，不可操作
     */
    DocumentActionResult closeIt();
    
    /**
     * 重新激活：从已关闭恢复到已完成
     */
    DocumentActionResult reActivateIt();
    
    /**
     * 过账：生成会计凭证
     */
    DocumentActionResult postIt();
    
    /**
     * 获取当前单据状态
     */
    String getDocStatus();
    
    /**
     * 获取单据类型（用于凭证模板匹配）
     */
    String getDocType();
}
```

### DocumentActionResult

```java
public class DocumentActionResult {
    private boolean success;
    private String newStatus;
    private String errorMessage;
    private List<String> executedHooks;
    private Map<String, Object> generatedDocuments; // 生成的关联单据
}
```

## DocumentEngine 服务

### 核心逻辑

```java
@Component
public class DocumentEngine {
    @Inject
    private DocumentTransitionRegistry transitionRegistry;
    
    @Inject
    private DocumentHookRegistry hookRegistry;
    
    @Inject
    private Map<String, IDocumentActionFactory> actionFactories; // key = entityName
    
    /**
     * 处理单据动作
     */
    public DocumentActionResult processAction(Object document, String action) {
        String entityName = getEntityName(document);
        String currentStatus = getDocStatus(document);
        
        // 1. 校验转换合法性
        if (!transitionRegistry.isValidTransition(entityName, currentStatus, action)) {
            return DocumentActionResult.error("Invalid transition: " + currentStatus + " → " + action);
        }
        
        // 2. 获取动作执行器
        IDocumentActionFactory factory = actionFactories.get(entityName);
        IDocumentAction docAction = factory.create(document);
        
        // 3. 执行前置钩子
        hookRegistry.executePreHooks(entityName, action, document);
        
        // 4. 执行动作
        DocumentActionResult result = executeAction(docAction, action);
        
        // 5. 执行后置钩子
        if (result.isSuccess()) {
            hookRegistry.executePostHooks(entityName, action, document);
        }
        
        return result;
    }
    
    private DocumentActionResult executeAction(IDocumentAction docAction, String action) {
        switch (action) {
            case "Prepare": return docAction.prepareIt();
            case "Complete": return docAction.completeIt();
            case "Void": return docAction.voidIt();
            case "Reverse": return docAction.reverseIt();
            case "Close": return docAction.closeIt();
            case "ReActivate": return docAction.reActivateIt();
            case "Post": return docAction.postIt();
            default: return DocumentActionResult.error("Unknown action: " + action);
        }
    }
}
```

## DocumentHooks（横切关注点）

### 钩子注册

```java
@Component
public class DocumentHookRegistry {
    @Inject
    private List<IDocumentHook> hooks; // 自动聚合所有实现
    
    public void executePreHooks(String entityName, String action, Object document) {
        hooks.stream()
            .filter(h -> h.matches(entityName, action, HookPhase.PRE))
            .forEach(h -> h.execute(document));
    }
    
    public void executePostHooks(String entityName, String action, Object document) {
        hooks.stream()
            .filter(h -> h.matches(entityName, action, HookPhase.POST))
            .forEach(h -> h.execute(document));
    }
}
```

### 钩子接口

```java
public interface IDocumentHook {
    /**
     * 匹配条件：实体名、动作、阶段
     */
    boolean matches(String entityName, String action, HookPhase phase);
    
    /**
     * 执行钩子逻辑
     */
    void execute(Object document);
}

public enum HookPhase {
    PRE, POST
}
```

### 核心钩子示例

| 钩子 | 触发时机 | 作用 |
|------|----------|------|
| `StockMoveGenerateHook` | Complete（采购入库/销售出库） | 生成库存移动单 |
| `PostingHook` | Complete（所有财务相关单据） | 触发凭证生成 |
| `ReversePostingHook` | Reverse | 生成反向凭证 |
| `AmountCalculateHook` | Prepare | 计算单据金额 |
| `ArchiveDocumentHook` | Close | 归档单据数据 |

### 业财一体钩子（PostingHook）

```java
@Component
public class PostingHook implements IDocumentHook {
    @Inject
    private IErpFinPostingService postingService;
    
    @Override
    public boolean matches(String entityName, String action, HookPhase phase) {
        // Complete 动作后触发过账
        return "Complete".equals(action) && phase == HookPhase.POST;
    }
    
    @Override
    public void execute(Object document) {
        // 异步过账（EventBus）
        postingService.postAsync(document);
    }
}
```

## 异步过账机制

### EventBus 异步过账

参考 Metasfresh 的 post-commit EventBus 模式：

```
异步过账流程
        │
        ├─► 步骤1：主事务落单据
        │      ├─ 单据状态更新为 COMPLETED
        │      ├─ posted = false
        │      └─ 主事务提交
        │
        ├─► 步骤2：事务提交后投递事件
        │      ├─ EventBus.post(new PostingEvent(document))
        │      └─ 不阻塞主事务
        │
        ├─► 步骤3：异步消费事件
        │      ├─ PostingConsumer 接收事件
        │      ├─ 加载单据（乐观锁）
        │      ├─ 生成凭证
        │      ├─ 更新 posted = true
        │      └─ 提交过账事务
        │
        └─► 步骤4：兜底扫描
               ├─ 定时 Job 扫描 posted=false 的单据
               ├─ 重试过账
               └─ 记录异常日志
```

### 兜底扫描 Job

```java
@Component
public class PostingRecoveryJob {
    @Inject
    private IErpFinPostingService postingService;
    
    @Scheduled(cron = "0 */5 * * * ?") // 每 5 分钟
    public void scanUnpostedDocuments() {
        // 查询 posted=false 且 docStatus=COMPLETED 的单据
        List<Object> unposted = findUnpostedDocuments();
        
        for (Object doc : unposted) {
            try {
                postingService.postNow(doc);
            } catch (Exception e) {
                log.error("Posting recovery failed: {}", doc, e);
                // 记录异常，不阻塞其他单据
            }
        }
    }
}
```

## 与审批流程的协作

### 审批流程定位

审批流程（nop-wf）只处理"需要人工决策的审批环节"，不替代业务状态机：

```
审批流程与状态机协作
        │
        ├─► 业务状态机驱动业务流转
        │      ├─ DRAFT → PREPARED（提交审批）
        │      ├─ PREPARED → COMPLETED（审批通过）
        │      └─ PREPARED → DRAFT（审批驳回）
        │
        ├─► 审批流程处理人工决策
        │      ├─ 启动审批实例
        │      ├─ 审批人审批
        │      ├─ 审批结果回调
        │      └─ 更新 approveStatus
        │
        └─► 状态机与审批流程解耦
               ├─ 状态机不依赖审批流程
               ├─ 审批流程只更新 approveStatus
               └─ Complete 动作校验 approveStatus
```

### 审批回调

```java
@Component
public class ApprovalCallbackListener {
    @Inject
    private DocumentEngine documentEngine;
    
    /**
     * 审批通过回调
     */
    public void onApprovalPassed(String entityName, Long documentId) {
        Object document = loadDocument(entityName, documentId);
        // 更新审批状态
        setApproveStatus(document, "APPROVED");
        // 尝试自动完成（如果其他条件满足）
        if (canAutoComplete(document)) {
            documentEngine.processAction(document, "Complete");
        }
    }
    
    /**
     * 审批驳回回调
     */
    public void onApprovalRejected(String entityName, Long documentId) {
        Object document = loadDocument(entityName, documentId);
        // 更新审批状态
        setApproveStatus(document, "REJECTED");
        // 回退到草稿状态
        setDocStatus(document, "DRAFT");
    }
}
```

## 与 nop 平台的集成

### nop-entropy 能力利用

| nop 能力 | 用途 |
|----------|------|
| `nop-auth` 多租户 | 单据带 `tenantId`/`orgId` |
| `nop-wf` 流程引擎 | 审批流程（可选） |
| `nop-message`/`nop-stream` | EventBus 异步过账 |
| `nop-dao` 乐观锁 | 并发控制 |
| `nop-ioc` 注入聚合 | DocumentActionFactory 注册 |
| `xmeta` 元数据 | 状态字段字典定义 |

### 实体模型约定

所有业务单据实体必须包含：

```xml
<entity name="ErpPurOrder">
    <column name="docStatus" dict="erp/doc-status" mandatory="true"/>
    <column name="approveStatus" dict="erp/approve-status" mandatory="true"/>
    <column name="postedStatus" dict="erp/posted-status" mandatory="true"/>
    <column name="posted" type="Boolean" mandatory="true" default="false"/>
    <column name="postedTime" type="DateTime"/>
    <column name="postedError" type="String" length="500"/>
</entity>
```

## 各域单据状态机

### 采购域

| 单据 | 状态机 | 特殊动作 |
|------|--------|----------|
| 采购订单 | DRAFT→PREPARED→COMPLETED→CLOSED | Complete 触发生成入库单 |
| 采购入库单 | DRAFT→PREPARED→COMPLETED | Complete 触发库存入库+过账 |
| 采购发票 | DRAFT→PREPARED→COMPLETED | Complete 触发应付凭证 |

### 销售域

| 单据 | 状态机 | 特殊动作 |
|------|--------|----------|
| 销售订单 | DRAFT→PREPARED→COMPLETED→CLOSED | Complete 触发生成出库单 |
| 销售出库单 | DRAFT→PREPARED→COMPLETED | Complete 触发库存出库+过账 |
| 销售发票 | DRAFT→PREPARED→COMPLETED | Complete 触发应收凭证 |

### 库存域

| 单据 | 状态机 | 特殊动作 |
|------|--------|----------|
| 库存移动单 | DRAFT→CONFIRMED→COMPLETED | Confirm 占用预留量，Complete 写库存流水 |
| 调拨单 | DRAFT→CONFIRMED→COMPLETED | 双向移动 |
| 盘点单 | DRAFT→CONFIRMED→COMPLETED | 差异调整 |

### 财务域

| 单据 | 状态机 | 特殊动作 |
|------|--------|----------|
| 凭证 | DRAFT→PREPARED→POSTED | Post 写入总账 |
| 收款单 | DRAFT→PREPARED→COMPLETED | Complete 触发核销 |
| 付款单 | DRAFT→PREPARED→COMPLETED | Complete 触发核销 |

## 配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp.doc.auto-complete-on-approve` | true | 审批通过后自动完成 |
| `erp.doc.async-posting` | true | 是否异步过账 |
| `erp.doc.posting-retry-interval` | 300 | 兜底扫描间隔（秒） |
| `erp.doc.posting-max-retries` | 3 | 过账最大重试次数 |

## 开源参考

| 项目 | 参考维度 | 具体借鉴 |
|------|----------|----------|
| iDempiere | DocumentEngine 统一状态机 | DocAction 接口 + DocumentEngine 服务 |
| Metasfresh | EventBus 异步过账 | post-commit 投递 + 兜底扫描 |
| Tryton | 声明式状态机 | `_transitions` set + `@transition` 装饰器 |
| 赤龙 | 三轴状态分离 | status + approveStatus + paidStatus |
| ERPNext | 钩子驱动 | on_submit/on_cancel 统一入口 |
| 星云 | BPM 与状态机分层 | warm-flow 只管审批，核心流转靠状态 |