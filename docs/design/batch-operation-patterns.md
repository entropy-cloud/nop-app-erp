# 批量操作范式（Batch Operation Patterns）

> Owner docs: `docs/backlog/frontend-ui-roadmap.md` §F11、各域 `ui-patterns.md`
> 落地计划：`docs/plans/2026-07-22-0444-2-frontend-f11-domain-batch-operations.md`
> 相关范式：`docs/design/child-table-editor-patterns.md`（F4 子表 form-level "从订单导入行" 范式，非列表页批量）

## 1. 目的与范围

固化「ERP 列表页批量操作」的标准范式，供长尾域按图施工。

**适用范围**：列表页选中 N 行 → 一次性执行同种业务动作（批量审批/批量状态切换/批量排程/全局触发型批量）。

**不适用**：
- 子表行内编辑（→ F4 `child-table-editor-patterns.md`）
- form-level "从订单导入行" picker 多选（→ F9，已覆盖；与列表页"批量"语义不同）
- 文件型批量导入（Excel → DB，需独立 import 框架，F11 successor）
- 像素级视觉回归（Non-Goal in roadmap）

## 2. 模式分类

| 模式 | 后端动作 | 前端按钮形态 | 代表用例 |
|------|---------|------------|---------|
| **A：新增原子 batch mutation** | 新增 `@BizMutation batchXxx(Collection<String> ids, context)`，循环调单条 Processor，返回 `BatchOperationResult` | `<listActions><action batch="true">` 调 `@mutation:Xxx__batchXxx?ids=$ids` | F11 批量审批（PurOrder/SalOrder/QaInspection）、批量重新排程（ApsSchedule） |
| **B：平台 builtin batch mutation** | 直接调 `ICrudBiz.batchUpdate/batchDelete/batchModify`（已 builtin，无后端改动） | `<listActions><action batch="true">` 调 `@mutation:Xxx__batchUpdate` + `<data>` 块传更新字段 | F11 批量启用/停用（ErpMdPartner/ErpMdMaterial status 切换）、平台 NopAuthUser 批量删除 |
| **C：全局触发型 mutation（非 row-id 批量）** | 已存在的单条 mutation（参数为 direction/partnerId/strategy 等业务键，非 id 列表），前端通过 list-level `<listActions>` 按钮触发（不要求选中行） | `<listActions><action>` 非 `batch=true` + `<data>` 块传业务参数 + 弹窗收集 | F11 自动核销（`ErpFinReconciliation__runAutoReconciliation(direction,partnerId,strategy)`） |

**裁决矩阵**（Phase 0 决策记录）：

| 操作特征 | 选模式 |
|---------|-------|
| 状态机迁移（每行有 approveStatus 等状态守卫） | **A** |
| 纯字段批量更新（status 切换、批量赋值） | **B** |
| 全局计算型（按业务键聚合，与具体行无关） | **C** |

**模式 B 否决场景**：若 batchUpdate 需要触发业务联动（如停用时校验引用、状态切换时回写其他表），必须改用模式 A 在后端实现联动；否则 batchUpdate 仅写裸字段，绕过状态机。

## 3. 后端模式 A：新增 batch mutation

### 3.1 IBiz 接口声明

```java
@BizMutation
BatchOperationResult batchApprove(@Name("ids") Collection<String> ids, IServiceContext context);
```

- `Collection<String>`（不直接用 `List<Long>`）：与 `ICrudBiz.batchDelete(Set<String> ids)` 一致，AMIS `$ids` 逗号分隔字符串经平台 GraphQL 自动转换；后端按需 `Long.valueOf(id)` 转换
- `IServiceContext context` 必须为末参（项目 P2 自检规则）

### 3.2 BizModel 实现（逐行执行模式 b）

```java
@Override
@BizMutation
public BatchOperationResult batchApprove(@Name("ids") Collection<String> ids, IServiceContext context) {
    BatchOperationResult result = BatchOperationResult.forTotal(ids == null ? 0 : ids.size());
    if (ids == null || ids.isEmpty()) {
        return result;
    }
    for (String id : ids) {
        try {
            orderProcessor.approve(id, context);
            result.recordSuccess();
        } catch (NopException e) {
            result.recordFailure(id, e.getErrorCode(), e.getDescription());
        }
    }
    return result;
}
```

**关键规则**：
- **循环调单条 Processor**（不重写状态机逻辑）：保证业务规则（供应商 active、预算校验、commitment hook）与权限检查与单条审批完全一致
- **catch NopException 而非 RuntimeException**：业务异常（状态非法、规则不满足）记入 failures，系统异常（NPE 等）继续抛出
- **不捕获 NumberFormatException**（id 已为 String）；若 id 来自其他源需转换 Long，可加 catch NumberFormatException 记 failure（见 `ErpQaInspectionBizModel.batchPassInspection` 实现）

### 3.3 BatchOperationResult DTO 结构

```java
public class BatchOperationResult {
    int totalCount;          // 入参 ids.size()
    int successCount;        // 成功行数
    int failedCount;         // 失败行数
    List<BatchItemFailure> failures;  // 失败明细
    
    public static class BatchItemFailure {
        String id;           // 失败行 ID
        String code;         // ErrorCode（来自 NopException.getErrorCode）
        String message;      // 错误描述（来自 NopException.getDescription）
    }
}
```

**域隔离原则**：每个域维护自己的 `BatchOperationResult`（不跨域依赖），结构相同。原因：dao 模块间不互相依赖（purchase-dao 不依赖 sales-dao），跨域共享 DTO 会引入非法依赖。

## 4. 前端模式：`<listActions>` + `batch="true"`

### 4.1 最小模板

```xml
<pages>
    <crud name="main">
        <listActions>
            <action id="batch-approve-button" label="批量审批" level="success" icon="fa fa-check-circle-o" batch="true">
                <api url="@mutation:ErpPurOrder__batchApprove?ids=$ids"/>
                <confirmText>确认批量审批选中的采购订单？仅 SUBMITTED 状态的单据会被审批，其他状态将记入失败清单。</confirmText>
                <messages>
                    <success>批量审批已执行（详见返回结果）</success>
                </messages>
            </action>
        </listActions>
        <rowActions x:override="bounded-merge">
            <!-- ... -->
        </rowActions>
    </crud>
</pages>
```

**codegen 展开链**：
- view.xml `<listActions>` + `batch="true"` → AMIS `bulkActions` 数组（每行选中行 checkbox 出现，按钮只在 ≥1 行选中时启用）
- `<api url="@mutation:Xxx__batchApprove?ids=$ids"/>` → AMIS 自动收集 `${selectedItems|map:id|join:','}` 注入 `$ids` URL 参数
- 平台 GraphQL 解析 `ids=$ids` 逗号分隔字符串 → `Collection<String>` 入参

### 4.2 平台 builtin batchUpdate 的 data 块

```xml
<action id="batch-active-button" label="批量启用" level="success" icon="fa fa-check-circle-o" batch="true">
    <api url="@mutation:ErpMdPartner__batchUpdate">
        <data>
            <ids>${ids | split:','}</ids>
            <data>{status:'ACTIVE'}</data>
        </data>
    </api>
    <confirmText>确认批量启用选中的往来单位？</confirmText>
</action>
```

**注意**：`batchUpdate` 返回 `void`（非对象），前端 `<messages><success>` 仍可触发；E2E 测试需用 `gql.raw()` 直接发 mutation（`callMutation` 默认附 `{fields}` 选择集对 void 类型非法）。

### 4.3 全局触发型按钮（非 batch）

```xml
<listActions>
    <action id="auto-reconcile-button" label="自动核销" level="primary" icon="fa fa-magic">
        <api url="@mutation:ErpFinReconciliation__runAutoReconciliation">
            <data>
                <direction>${direction | default:'RECEIVABLE'}</direction>
                <partnerId>${partnerId | default:null}</partnerId>
                <strategy>${strategy | default:null}</strategy>
            </data>
        </api>
        <confirmText>...</confirmText>
    </action>
</listActions>
```

**不要求 `batch="true"`**：按钮始终可见，与选中行无关。

## 5. AMIS bulkActions vs headerToolbar bulkActions

view.xml `<listActions>` 中 `batch="true"` 的 action 由 codegen 自动归入 AMIS `bulkActions` 数组（与 `headerToolbar` 同级，仅在选中行时显示）。**不要手写 AMIS `bulkActions` JSON**——通过 view.xml 抽象生成。

参考 `nop-entropy/docs-for-ai/03-runbooks/add-export-or-batch-operations.md` + `nop-auth/NopAuthUser.view.xml:223`（平台 builtin batchDelete 范式）。

## 6. 部分失败处理策略（模式 b 详解）

**选择**：逐行执行 + 返回 `BatchOperationResult` 含成功数 + 失败明细。

**理由**：
- ERP 批量审批面对 N 张单据往往包含混合状态（已审批、未提交、已作废）；整体回滚（a）要求用户先手工筛选"纯净子集"，违背"批量"初衷
- 预校验（c）把"哪些行有问题"判断推回前端，违反"前端不写业务判断"原则
- 模式 b 让后端用领域规则判定每行，前端只展示结果

**适用场景反例**（不应使用模式 b）：
- 金融过账类强一致场景（如凭证批量过账，必须全成功或全回滚）→ 用模式 a
- 预校验成本远低于执行成本的场景（如批量删除前的引用预览，已由 F7 §3 覆盖）→ 用模式 c

**前端展示**：成功数 + 失败清单（每行 ID + ErrorCode + 描述）；AMIS `messages.success` 通用提示，详细失败清单可通过返回值 dialog 展开（F11 当前用 confirm + messages 最小闭环，详细可视化归 successor）。

## 7. 反模式自检表

| # | 不要这样写 | 应该这样写 |
|---|-----------|-----------|
| 1 | 前端 forEach N 次调单条 mutation（模式 B） | 后端新增 batch mutation（模式 A），单次 GraphQL 调用 |
| 2 | batch mutation 内重写状态机逻辑（复制单条 Processor 代码） | 循环调单条 Processor.approve()，复用业务规则 |
| 3 | catch RuntimeException（吞掉所有异常） | catch NopException（业务异常）+ recordFailure；系统异常继续抛 |
| 4 | `Collection<Long> ids`（要求前端转 Long） | `Collection<String> ids`，后端按需 `Long.valueOf()`（与 ICrudBiz.batchDelete 一致） |
| 5 | 跨域共享 BatchOperationResult DTO（dao 模块跨域依赖） | 每个域维护自己的 BatchOperationResult，结构相同但包名独立 |
| 6 | 在前端 visibleOn 中判断"所有选中行都是 SUBMITTED"（前端业务判断） | 后端逐行处理，行级失败返回 failures 清单 |
| 7 | list-level batch 按钮调用 form-level 操作（混淆"列表批量"与"子表多选"） | 列表批量用 `<listActions>`；form-level 子表多选用 F4 `<cell>` + AMIS picker multi-select |
| 8 | 用 `@BizMutation batchXxx(List<Long>)` + 在前端循环调单条（混合反模式） | 后端 batch mutation 单次调用 + 内部循环（不是前端循环） |
| 9 | 全局触发型 mutation（如 runAutoReconciliation）使用 `batch="true"` | 不加 `batch="true"`，按钮始终可见；经 `<data>` 块传业务参数 |
| 10 | `batchUpdate` 调用涉及业务联动（如 status 切换触发引用校验） | 改用模式 A 新增 batch mutation，确保状态机规则生效 |

## 8. 落地证据（F11 plan 2026-07-22-0444-2）

### 8.1 后端 batch mutation（模式 A）

| 域 | 实体 | 方法 | 证据 |
|----|------|------|------|
| purchase | ErpPurOrder | `batchApprove` | `IErpPurOrderBiz.java:25` + `ErpPurOrderBizModel.java:50-72` |
| sales | ErpSalOrder | `batchApprove` | `IErpSalOrderBiz.java` + `ErpSalOrderBizModel.batchApprove` |
| quality | ErpQaInspection | `batchPassInspection` | `IErpQaInspectionBiz.java` + `ErpQaInspectionBizModel.batchPassInspection`（用 result 状态轴） |
| aps | ErpApsOperationOrder | `batchScheduleForward` | `IErpApsOperationOrderBiz.java` + `ErpApsOperationOrderBizModel.batchScheduleForward` |

DTO：4 域各维护 `BatchOperationResult`（同结构、独立包）。

### 8.2 前端 batch 按钮

| 文件 | 行号 | 模式 |
|------|------|------|
| `ErpPurOrder.view.xml` | 188-196 | A: batchApprove |
| `ErpSalOrder.view.xml` | 159-167 | A: batchApprove |
| `ErpQaInspection.view.xml` | 129-137 | A: batchPassInspection |
| `ErpApsSchedule.view.xml` | 125-133 | A: batchScheduleForward |
| `ErpMdPartner.view.xml` | 243-269 | B: batchUpdate (启用+停用) |
| `ErpMdMaterial.view.xml` | 318-344 | B: batchUpdate (启用+停用) |
| `ErpFinReconciliation.view.xml` | 88-105 | C: runAutoReconciliation (非 batch) |

### 8.3 测试

`tests/e2e/business-actions/f11-batch-operations.action.spec.ts` 5 用例全绿：
- (a) PurOrder 3+1 混合 → 3 success + 1 failure（部分失败守卫）
- (a-empty) 空入参边界
- (b) SalOrder 单条正路径
- (c) QaInspection PENDING→ACCEPTED 翻转
- (d) ErpMdPartner batchUpdate（平台 builtin，void 返回值经 raw() 测试）

## 9. 落地后影响与 successor

- **长尾域批量操作扩展**：crm/cs/hr/logistics/b2b/contract/drp 等域按需逐域补齐（触发：业务客户明确要求某长尾域批量操作）
- **批量操作性能优化（分页/异步）**：当前批量操作面向管理后台低频场景；超大批量（> 1000 行）的异步任务化 + 进度条为性能优化（触发：单次 > 500 行且响应 P95 > 3s）
- **Excel 文件批量导入框架**：Nop 平台无 builtin `__import` mutation，需要独立 import 框架（/f/upload + ImportTask）（触发：业务客户明确要求 Excel 批量导入 Partner/Material）
- **失败清单可视化增强**：当前 confirm + messages 最小闭环；详细失败清单 dialog 展开 + 重试入口归 successor
- **跨域共享 BatchOperationResult 重构**：当 4+ 域各自维护相同结构 DTO 时，可考虑提取到 common-dao 共享模块（当前域隔离原则优先）

## 10. 变更记录

- 2026-07-22：F11 plan `2026-07-22-0444-2` Phase 3 落地。建立 3 模式分类（A/B/C）+ 后端 batch mutation 范式 + 前端 `<listActions>` + `batch="true"` 范式 + 10 项反模式自检表。
