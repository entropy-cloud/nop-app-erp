# 批处理现状审计与 nop-batch 迁移分析报告

> 日期：2026-07-18
> 范围：全仓 18+1 域定时作业 + Service 层批量数据处理
> 结论：当前**所有**批量作业均采用一次性加载 + 内存全文迭代模式，存在严重的内存风险与缺乏断点续传/记录级容错能力。nop-batch 平台能力已成熟，可正式迁移。

---

## 1. 现状：全仓批处理全景

### 1.1 调度框架

- **nop-job-local**（`app-erp-all/pom.xml:182`）已完成系统级接入，`BeanMethodJobInvoker` 反射调用无参 `execute()` 方法
- **scheduler.yaml**（旧机制）：登记 19 个定时作业，全部采用 cron + 门控配置键（空=不执行）
- **19 个 Job Bean 类**：分布在 9 个域（fin×3, ast×1, mnt×1, mfg×2, cs×3, crm×5, hr×1, prj×1, qa×2）

### 1.2 关键发现：全仓零 nop-batch 使用

| 检查项 | 结果 |
|--------|------|
| `nop-batch` Maven 依赖 | ❌ 不存在（任何 pom.xml 均无） |
| `*.batch.xml` 配置文件 | ❌ 零个 |
| `*.job.yaml` 独立作业定义（平台推荐的新机制） | ❌ 零个（全在旧式 `scheduler.yaml` 内联） |
| 通过 `nopBatchTaskRunner` 直接触发 batch | ❌ 零个 |
| Chunk 处理 / 断点续传 / 记录级幂等 | ❌ 零个 |
| `nop-task` / `task.xml` / `nop-message` | ❌ 零个 |

### 1.3 内存处理反模式（所有 Job Bean）

所有 19 个 Job Bean 共用同一反模式：

```java
// 模式：一次性加载 → 内存迭代
List<Entity> all = biz.findList(query);           // 全量加载到内存
for (Entity e : all) {                             // 同步遍历
    try { biz.process(e); } catch (Exception ex) { LOG.error(...); }
}
```

典型实例（`ErpCrmLeadScoringRecalcJob.java:53-73`）：

```
execute()
  └─ leadBiz.findList(q)            ← 一次性加载所有 ACTIVE 线索到内存
  └─ for (lead : activeLeads)       ← 单线程串行迭代
       └─ leadScoreBiz.recalculateScore()
```

**风险**：
- 无内存上限保护：数据增长 → OOM 或 Full GC
- 无断点续传：中途失败 → 下次从头重跑
- 无记录级重试/跳过：单条失败 → try/catch 吞异常继续，无重试策略
- 无事务分片：整个作业一个隐式事务（`IServiceContext ctx = new ServiceContextImpl()`）

### 1.4 架构已识别但在 defer

`docs/architecture/job-scheduling.md` §7 已识别 **12 个 batch-candidate 作业**并通过四维裁决（处理量级 / 事务边界 / 断点续跑需求 / 重试粒度），但迁移被 deferred。Javadoc 中明确标注 `// migration to nop-batch is Deferred`。

---

## 2. nop-batch 平台能力（docs-for-ai 文档分析 + 平台源码锚点）

### 2.1 核心模型

```
Loader（批量加载）→ Processor（flatMap 处理）→ Consumer（批量写入）
    ↑                    ↑                         ↑
  load(batchSize)     process(item, consume)    consume(Collection)
```

三接口均接收 `IBatchChunkContext`，无需跨类传递全局状态。

### 2.2 关键能力

| 能力 | 本仓库现状 | nop-batch 方案 |
|------|-----------|---------------|
| 内存控制 | 全量加载到内存 | `batchSize` 分 chunk 加载，内存可控 |
| 断点续传 | ❌ 失败重跑全部 | `completedIndex` 持久化进度，断点恢复 |
| 记录级幂等 | ❌ | `NopBatchRecordResult` + `recordKey` |
| 重试策略 | try/catch 吞异常 | `retryPolicy`（指数退避 + jitter）；`retryOneByOne` chunk 失败降级到单条 |
| 跳过策略 | ❌ | `skipPolicy`（容忍坏记录继续，超阈值则任务失败） |
| 事务分片 | 整个作业一个事务 | `transactionScope` 支持 `none/chunk/process/consume` 四级 |
| 并发处理 | 单线程 | `concurrency` + `executor` 线程池 |
| 分区并行 | ❌ | `dispatcher` + `partitionIndexField`，同一 partition 保证顺序 |
| 生命周期 | ❌ | 12 种 listener（onTaskBegin/End, onChunkBegin/End, onLoadBegin/End 等） |

### 2.3 权威集成模式：`.job.yaml` 直接触发 batch（平台标准）

**这是平台已证明的标准模式**。`nop-batch-sys` 模块的 `sys-event-batch-consumer.job.yaml`（`nop-entropy/nop-batch/nop-batch-sys/src/main/resources/_vfs/nop/job/conf/`）：

```yaml
jobName: sys-event-batch-consumer
enabled: "@cfg:nop.job.sys-event-batch-consumer.enabled|false"
displayName: Sys Event Non-Broadcast Consumer
jobGroup: nop-sys
trigger:
  cronExpr: "@cfg:nop.job.sys-event-batch-consumer.cron-expr|0/5 * * * * ?"
invoker:
  bean: nopBatchTaskRunner          # 平台内置 bean
  method: executeAsync              # 异步执行
params:
  taskPath: /nop/batch-task/sys-event/non-broadcast-consumer.batch.xml
  params: {}
```

**关键要点**：
- **无 Java Job Bean 类**——`nopBatchTaskRunner` 是平台内置 Bean，直接加载 `batch.xml` 并执行
- **`taskPath`** 参数指定 `batch.xml` 在 VFS 中的路径
- 每个 job 一个 `.job.yaml` 文件，放在 `/nop/job/conf/` 下（新机制，替代旧式 `scheduler.yaml` 内联）
- `enabled` 通过 `@cfg:` 绑定到独立配置键，缺省 `false`，应用层显式开启

这是本项目应该直接采用的模式。

### 2.4 与 nop-job 集成的两种方式

| 方式 | 适用场景 | 参考 |
|------|----------|------|
| **`.job.yaml` → `nopBatchTaskRunner.executeAsync`**（推荐） | 纯批处理，无需 DAG 编排，直接触发 batch.xml | `nop-batch-sys` 的 `sys-event-batch-consumer.job.yaml` |
| **`task.xml` → `<step customType="batch:Execute">`** | 需 nop-task 编排的复杂工作流，batch 作为其中一步 | `batch-demo.task.xml` |

### 2.5 `orm-reader` 用法（权威示例）

平台真实示例（`non-broadcast-consumer.batch.xml`）：

```xml
<loader>
    <orm-reader entityName="io.nop.sys.dao.entity.NopSysEvent">
        <query>
            <filter>
                <c:script>
                    import io.nop.sys.dao.NopSysDaoConstants;
                    const messageService = inject('nopSysDaoMessageService');
                    const endTime = messageService.getEstimatedNow();
                </c:script>
                <and>
                    <in name="eventTopic" value="${messageService.getNonBroadcastTopics()}"/>
                    <or>
                        <and>
                            <eq name="eventStatus"
                                value="${NopSysDaoConstants.SYS_EVENT_STATUS_WAITING}"/>
                            <le name="scheduleTime" value="${endTime}"/>
                        </and>
                        <and>
                            <eq name="eventStatus"
                                value="${NopSysDaoConstants.SYS_EVENT_STATUS_CLAIMED}"/>
                            <le name="leaseExpireTime" value="${endTime}"/>
                        </and>
                    </or>
                </and>
            </filter>
        </query>
    </orm-reader>
</loader>
```

另一种简写方式（`batch-demo.task.xml`）：

```xml
<orm-reader entityName="DemoIncomingTxn"/>
```

不带 `query` = 全表扫描。带 `<query>` = 结构化查询模型（`filter` + 条件表达式）。

### 2.6 Processor 调用方式（共享核心业务逻辑）

**关键原则：在线处理和批处理共享核心代码，不重复实现业务逻辑。**

Processor 的 `<source>` 是 xpl 脚本环境，通过 `inject('beanName')` 直接获取 IoC 容器中的 I*Biz 接口：

```xml
<processor name="process">
    <source><![CDATA[
        const svc = inject('nopSysDaoMessageService');
        svc.processClaimedNonBroadcastEvent(item);
    ]]></source>
</processor>
```

再如 `batch-demo.task.xml` 中的 consumer：

```xml
<consumer name="deleteInput">
    <filter>return item instanceof String;</filter>
    <source><![CDATA[
        const daoProvider = inject('nopDaoProvider');
        daoProvider.dao('DemoIncomingTxn').deleteAllByIds(items);
    ]]></source>
</consumer>
```

**核心理解**：
- 在线处理：单条记录 → 调 I*Biz 接口 → I*Biz 内部可能调 Processor 做状态机/业务规则
- 批处理：多条记录 → nop-batch 分 chunk → 每 chunk 独立事务 → processor 中每条记录调同一 I*Biz 接口
- **业务逻辑层面不需要感知自己是在在线还是批处理**——都是同一个 `I*Biz.doSomething(item)`，nop-orm 和 nop-batch 内在的批量优化机制（batch load props、批量 flush 等）自动生效
- `inject()` 和 `@Inject` 获取的 I*Biz 是同一组 Bean

---

## 3. 迁移方案

### 3.1 迁移优先级

| 优先级 | 作业 | 理由 |
|--------|------|------|
| **P0** | `erp-fin-ar-ap-auto-recon` | 大（全量未清 AR/AP），记录级重试需求明确 |
| **P0** | `erp-ast-depreciation` | 大（批量计提），平台文档明示 nop-batch 适用场景 |
| **P1** | `erp-fin-cash-forecast-refresh` | 中-大（聚合未清项），断点续跑需求 |
| **P1** | `erp-crm-lead-scoring-recalc` | 小-中但典型性高，适合作为首个迁移示范 |
| **P2** | `erp-qa-spc-sampling`, `erp-qa-spc-capability` | 中-大（全表扫描检验行），需断点续跑 |
| **P3** | 其余 7 个 batch-candidate（`job-scheduling.md` §7 共 12 个） | 按各自迁移触发条件 |

### 3.2 迁移范式（精确）

**当前**（旧式内联 scheduler.yaml + Java Job Bean）：

```yaml
# scheduler.yaml（旧机制）
jobs:
  - jobName: erp-crm-lead-scoring-recalc
    trigger:
      cronExpr: "0 2 * * *"
    invoker:
      bean: erpCrmLeadScoringRecalcJob
      method: execute
```

```java
// Java Job Bean（全量加载 + 内存迭代）
public class ErpCrmLeadScoringRecalcJob {
    @Inject IErpCrmLeadBiz leadBiz;
    @Inject IErpCrmLeadScoreBiz leadScoreBiz;
    public void execute() {
        // 全量加载到内存
        List<ErpCrmLead> activeLeads = leadBiz.findList(q);
        for (ErpCrmLead lead : activeLeads) {
            try { leadScoreBiz.recalculateScore(lead.getId(), ...); }
            catch (Exception e) { LOG.error(...); }
        }
    }
}
```

**目标**（.job.yaml + batch.xml，删除 Java Job Bean）：

```yaml
# /nop/job/conf/erp-crm-lead-scoring-recalc.job.yaml
jobName: erp-crm-lead-scoring-recalc
enabled: "@cfg:nop.job.erp-crm-lead-scoring-recalc.enabled|false"
displayName: CRM Lead Scoring Recalc
trigger:
  cronExpr: "@cfg:nop.job.erp-crm-lead-scoring-recalc.cron-expr|0 2 * * *"
invoker:
  bean: nopBatchTaskRunner
  method: executeAsync
params:
  taskPath: /nop/batch-task/crm/lead-scoring-recalc.batch.xml
  params: {}
```

```xml
<!-- /nop/batch-task/crm/lead-scoring-recalc.batch.xml -->
<batch taskName="crm.lead-scoring-recalc" batchSize="200" saveState="true"
       x:schema="/nop/schema/task/batch.xdef" xmlns:x="/nop/schema/xdsl.xdef">

    <skipPolicy maxSkipCount="100"/>

    <loader>
        <orm-reader entityName="app.erp.crm.dao.entity.ErpCrmLead">
            <query>
                <filter>
                    <notIn name="docStatus" value="${['CONVERTED','LOST','CANCELLED']}"/>
                </filter>
            </query>
        </orm-reader>
    </loader>

    <processor name="recalcScore">
        <source><![CDATA[
            const biz = inject('IErpCrmLeadScoreBiz');
            // processor 通过 inject() 共享同一组 I*Biz Bean，与在线处理代码一致
            // 批处理不需要显式创建 IServiceContext——IBatchChunkContext 提供 getServiceContext() default 委派
            biz.recalculateScore(item.id, 'SCHEDULED', batchChunkCtx.serviceContext);
        ]]></source>
    </processor>
</batch>
```

> **注意**：processor 的 `<source>` 函数签名为 `xpl-fn:(item,consume,batchChunkCtx)=>void`，因此 `consume(item)` 可选调用——processor 可直接通过 `inject()` 完成业务操作而不输出到 consumer（如 `non-broadcast-consumer.batch.xml` 所示）。`batchChunkCtx.serviceContext` 是 `IBatchChunkContext` 上新增的 default 委派方法（转调 `getTaskContext().getServiceContext()`），与 `getTaskName()`/`getTaskId()`/`getTaskKey()` 模式一致。

**删除**：Java `ErpCrmLeadScoringRecalcJob.java` 不再需要，`app-service.beans.xml` 中对应的 bean 注册也删除。

### 3.3 每个 batch-candidate 的迁移要点

| 作业 | orm-reader 查询语义（filter 表达式简写） | Processor `inject()` + 调用 | 关键配置 |
|------|----------------------------------------|----------------------------|----------|
| `erp-fin-ar-ap-auto-recon` | filter `eq(status, 'OPEN')` | `IErpFinReconciliationBiz.runAutoReconciliation()` | batchSize=100, skipPolicy, saveState=true |
| `erp-ast-depreciation` | filter `eq(status, 'PENDING')` + `lt(nextRunDate, now)` | `IErpAstDepreciationScheduleBiz.executeBatchDepreciation()` | batchSize=50, retryPolicy, saveState |
| `erp-crm-lead-scoring-recalc` | filter `notIn(docStatus, ['CONVERTED','LOST','CANCELLED'])` | `IErpCrmLeadScoreBiz.recalculateScore()` | batchSize=200, skipPolicy(maxSkipCount=100) |
| `erp-fin-cash-forecast-refresh` | filter `eq(status,'OPEN')` + notes | `IErpFinCashForecastBiz.refreshItem()` | batchSize=500 |
| `erp-qa-spc-sampling` | filter `eq(status,'APPROVED')` | `IErpQaSpcSampleBiz.buildFromInspection()` | batchSize=200, saveState |
| `erp-qa-spc-capability` | filter `eq(active, true)` | `IErpQaSpcCapabilityBiz.calculateCpk()` | batchSize=100 |

> 表内 filter 为语义简写。实际 XML 需写成 `<filter><eq name="status" value="OPEN"/></filter>` 结构（参考 §2.5 `non-broadcast-consumer.batch.xml` 示例）。

### 3.4 `scheduler.yaml` → `.job.yaml` 迁移

根据 nop-job 文档（v2.0）：

| 机制 | 说明 | 本项目当前状态 |
|------|------|--------------|
| `scheduler.yaml`（全局开关） | 仅含 `enabled: true`，控制调度器是否启动 | ✅ 已存在（但内联了 19 个 jobs） |
| `*.job.yaml`（每个 job 一个文件） | 新机制，自动扫描 `/nop/job/conf/*.job.yaml` | ❌ 零个 |

**迁移步骤**：
1. 清理 `scheduler.yaml`：删除 `jobs:` 段，仅保留 `enabled: true`
2. 为每个现有的 19 个 job 创建 `<jobName>.job.yaml`（沿用当前 cron 和 bean 配置作为初始值）
3. 对于迁移到 nop-batch 的 job，用 `nopBatchTaskRunner` + `taskPath` 替换 `BeanMethodJobInvoker`，并删除对应的 Java Job Bean
4. 对于不迁移到 nop-batch 的小作业，保持 `.job.yaml` 中 `invoker.bean` 指向现有的 Java Bean

### 3.5 增量引入，无需一次性迁移

1. 先在 `app-erp-all/pom.xml` 添加 `nop-batch-dsl` 依赖（自动传递依赖 `nop-batch-core`、`nop-batch-orm`、`nop-batch-dao`）。若需 BizModel 管理页面再追加 `nop-batch-service`（非必须，`nopBatchTaskRunner` 已在 `nop-batch-dsl` 中注册）
2. 从 P0 开始（如 `erp-crm-lead-scoring-recalc`，数据量适中、逻辑简单）作为首个迁移示范
3. 创建 `batch.xml`（放在各域 `_vfs/nop/batch-task/<domain>/` 下）
4. 创建 `erp-xxx.job.yaml`（放在 `_vfs/nop/job/conf/` 下）
5. 删除对应的 Java Job Bean + bean 注册
6. 清理 `scheduler.yaml` 中的内联条目
7. 验证：通过 `nopBatchTaskRunner` 执行测试
8. 逐步迁移 P1→P3

---

## 4. 对 Service/Processor 层 `findAllByQuery()` 的建议

不在本波强制迁移范围，但应建立代码审查规则：
- 任何 `findAllByQuery()` / `findList()` 必须声明预期数据集大小的注释
- 面向全表扫描的逻辑应逐步包装为 `batch.xml` → `inject(I*Biz)` 模式
- 新增批量业务逻辑应优先考虑 nop-batch DSL

---

## 5. 风险与缓解

| 风险 | 缓解 |
|------|------|
| nop-batch 模块依赖增加，构建变慢 | 仅在 `app-erp-all` 聚合 pom 中添加，域模块不直接依赖 |
| 业务逻辑已封装在 I*Biz 接口中，batch logic 不重复 | `inject('IErpXxxBiz')` 直接在 processor source 中使用，与在线处理共享同一 Bean |
| 当前 `scheduler.yaml` 新旧机制并存期容易混淆 | 先完成所有 `.job.yaml` 迁移再逐步删除内联条目——同名的 `.job.yaml` 和 `scheduler.yaml.jobs` 条目会互斥（同名 WARN 跳过） |
| 运维需要理解 batch.xml DSL | 与现有 Nop 平台 XML 模式一致（xdef + xpl），学习曲线平坦 |

---

## 参考

- `docs/architecture/job-scheduling.md` — 权威全局作业目录 + 四维裁决
- `nop-entropy/docs-for-ai/03-modules/nop-batch.md` — nop-batch 模块概览
- `nop-entropy/docs-for-ai/02-core-guides/batch-dsl.md` — DSL 配置参考
- `nop-entropy/docs-for-ai/04-reference/xdefs/batch.xdef` — 权威 Schema
- `nop-entropy/docs-for-ai/03-modules/nop-job.md` — nop-job 模块（含 `.job.yaml` 规范）
- `nop-entropy/nop-batch/nop-batch-sys/src/main/resources/_vfs/nop/job/conf/sys-event-batch-consumer.job.yaml` — 平台标准 `.job.yaml` 直接调 batch 示例
- `nop-entropy/nop-batch/nop-batch-sys/src/main/resources/_vfs/nop/batch-task/sys-event/non-broadcast-consumer.batch.xml` — 平台标准 `orm-reader` + `inject()` 示例
- `nop-entropy/nop-runner/nop-cli/demo/_vfs/batch/batch-demo.task.xml` — task.xml 集成 batch 示例
- `module-*/erp-*-service/src/main/java/app/erp/*/service/job/*.java` — 19 个待迁移的 Java Job Bean
