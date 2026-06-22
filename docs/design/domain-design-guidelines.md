# 域设计指南

## 目的

定义 nop-app-erp 各业务域的设计原则、边界划分、跨域协作模式、数据一致性策略与架构约束。

---

## 一、设计原则

### 1.1 单一职责原则

每个域只负责自己核心的业务能力，不越界处理其他域的职责：

| 域 | 核心职责 | 不负责 |
|---|----------|--------|
| master-data | 主数据维护（物料/SKU/往来单位/仓库/科目） | 业务单据、余额计算 |
| inventory | 库存移动、流水、余额 | 物料主数据、存货凭证生成 |
| purchase | 采购订单、入库、发票、付款 | 库存写入、应付凭证生成 |
| sales | 销售订单、出库、发票、收款 | 库存扣减、应收凭证生成 |
| finance | 凭证生成、过账、成本核算、期末结账 | 业务单据、物料主数据 |
| assets | 资产卡片、折旧、资本化、处置 | 资产实物维护、折旧科目映射 |
| manufacturing | BOM、工单、工艺、报工 | 库存写入、成本结转凭证 |
| projects | 项目、任务、工时 | 成本归集凭证 |
| maintenance | 设备维护、计划、停机 | 资产折旧、备件库存出库 |
| quality | 质检、NCR、CAPA | 退货/返工执行 |

### 1.2 松耦合原则

- **跨域调用只通过接口**：使用 `I*Biz` 接口，不直接调用实现类
- **避免循环依赖**：按 DAG 顺序组织（master-data → inventory/purchase/sales → manufacturing/finance → assets/projects/maintenance/quality）
- **事件驱动解耦**：非关键路径使用事件异步通知，避免同步阻塞

### 1.3 幂等性原则

所有对外暴露的接口必须保证幂等：
- 使用唯一业务单号作为幂等键
- 重复调用同一单号的相同操作应返回相同结果
- 已完成的操作再次调用视为成功（空操作）

### 1.4 事务边界原则

| 操作类型 | 事务策略 |
|----------|----------|
| 单一域操作 | 本地事务 |
| 跨域同步操作 | 调用方事务传播（REQUIRED） |
| 跨域异步操作 | 各自独立事务 + 最终一致性 |
| 财务过账 | 异步事件 + posted 标志兜底 |

---

## 二、域间边界与交互模式

### 2.1 边界划分准则

**共享主数据**：master-data 域提供，其他域只读引用
- 物料/SKU、往来单位、仓库/库位、科目表/科目、币种/汇率、计量单位

**业务单据**：归属各自业务域，不跨域共享
- 采购订单（purchase）、销售订单（sales）、工单（manufacturing）

**状态流转**：每个域维护自己的状态机，跨域状态通过事件同步

### 2.2 交互模式分类

| 模式 | 适用场景 | 优点 | 缺点 |
|------|----------|------|------|
| **同步调用** | 强一致性要求、阻塞式确认 | 实时性强、一致性高 | 耦合度高、性能瓶颈 |
| **事件驱动** | 弱一致性要求、非阻塞流程 | 解耦、可扩展、异步 | 最终一致性、需处理失败 |
| **数据订阅** | 报表、数据分析 | 批量高效 | 数据延迟 |

### 2.3 标准交互模式

#### 模式 A：同步调用（强一致性）

```
[采购域] → IErpInvStockMoveBiz.generateMove() → [库存域]
              ↑ 事务传播 REQUIRED
```

**适用**：采购入库、销售出库（必须同步确认库存变更）

#### 模式 B：事件驱动（最终一致性）

```
[业务域审核通过] → 发布事件 → [事件总线] → [财务域订阅] → 生成凭证
                                    ↑
                              post-commit 异步
```

**适用**：凭证生成、成本结转（不阻塞业务流程）

#### 模式 C：数据查询（只读）

```
[任何域] → IErpMd*Biz.queryById() → [master-data 域]
              ↑ 只读，无事务
```

**适用**：查询主数据、验证数据有效性

---

## 三、跨域协作详细规则

### 3.1 主数据引用规则

所有域引用主数据必须通过 `IErpMd*Biz` 接口，禁止直接 ORM 跨工程引用：

```java
// ✅ 正确：通过 Biz 接口查询
ErpMdMaterial material = erpMdMaterialBiz.getById(materialId);

// ❌ 错误：直接 ORM 引用
@ManyToOne
@RefEntityName("erp-md:ErpMdMaterial")
private ErpMdMaterial material;
```

### 3.2 库存域协作规则

| 调用方 | 操作 | 接口 |
|--------|------|------|
| purchase | 采购入库 | `IErpInvStockMoveBiz.generateIncomingMove()` |
| sales | 销售出库 | `IErpInvStockMoveBiz.generateOutgoingMove()` |
| manufacturing | 领料/完工 | `IErpInvStockMoveBiz.generateProductionMove()` |
| maintenance | 备件出库 | `IErpInvStockMoveBiz.generateConsumptionMove()` |

### 3.3 财务域协作规则

所有业务域通过 `IErpFinAcctDocProvider` 接口注册凭证生成规则，财务域统一聚合：

```
采购域实现 PurAcctDocProvider → 处理 AP_INVOICE/PAYMENT
销售域实现 SalAcctDocProvider → 处理 AR_INVOICE/RECEIPT
库存域实现 InvAcctDocProvider → 处理存货估值
资产域实现 AstAcctDocProvider → 处理 DEPRECIATION/CAPITALIZATION
```

### 3.4 质检域协作规则

| 触发域 | 事件 | 质量域动作 |
|--------|------|------------|
| purchase | 采购入库审核 | 触发来料检验 |
| sales | 销售出库审核 | 触发出货检验 |
| manufacturing | 工单报工/完工 | 触发制程/完工检验 |

---

## 四、数据一致性策略

### 4.1 强一致性场景

**场景**：库存扣减与业务单据审核必须在同一事务

```
销售出库单审核
    ├─ 校验可用量（库存域）
    ├─ 生成出库移动单（库存域）
    ├─ 更新出库单状态为已审核（销售域）
    └─ 事务提交（任一失败则全部回滚）
```

### 4.2 最终一致性场景

**场景**：业务单据审核与凭证生成可异步

```
业务单据审核（成功）
    ├─ 更新单据状态
    ├─ 设置 posted=false
    ├─ 发布过账事件
    └─ 返回成功

异步过账（事件驱动）
    ├─ 消费过账事件
    ├─ 生成凭证
    ├─ 设置 posted=true
    └─ 失败则重试（兜底扫描）
```

### 4.3 兜底机制

**posted 标志 + 定时扫描**：

```
业务单据字段：posted (boolean)
定时任务：每分钟扫描 posted=false 且已审核超过 5 分钟的单据
         重新触发过账
```

### 4.4 冲销与回滚

| 单据类型 | 冲销方式 |
|----------|----------|
| 业务单据 | 生成红字冲销单（数量取负） |
| 凭证 | 生成红字凭证（金额取负） |
| 库存移动 | 生成反向移动单（方向相反） |

---

## 五、事务与锁策略

### 5.1 乐观锁

所有业务单据必须使用乐观锁版本号：

```java
@Version
private Integer version;
```

**适用场景**：并发编辑、状态变更

### 5.2 悲观锁

**适用场景**：库存扣减、付款核销（需防止并发冲突）

```java
// 库存扣减时锁定批次
@Lock(LockModeType.PESSIMISTIC_WRITE)
ErpInvStockBalance findByMaterialAndBatch(String materialId, String batchId);
```

### 5.3 分布式锁（可选）

**适用场景**：跨服务、跨节点的资源互斥

```java
// 使用 Redis 分布式锁
redissonClient.getLock("stock-lock:" + materialId + ":" + batchId);
```

---

## 六、权限与安全

### 6.1 角色职责分离

| 角色 | 职责 | 禁止 |
|------|------|------|
| 采购员 | 创建/提交采购单据 | 审核自己创建的单据 |
| 销售员 | 创建/提交销售单据 | 审核自己创建的单据 |
| 审核人 | 审核业务单据 | 创建业务单据（可配置） |
| 财务员 | 过账、结账 | 业务单据审核（可配置） |
| 管理员 | 系统配置、反结账 | 日常业务操作（可限制） |

### 6.2 数据权限

**行级权限**：根据组织/部门/项目限制数据可见性

```java
// 只允许查看本部门的单据
@Where("org_id IN (:orgIds)")
List<ErpPurPurchaseOrder> findByOrgIds(List<String> orgIds);
```

**字段级权限**：敏感字段（如成本、利润）只对授权角色可见

### 6.3 危险操作审计

所有危险操作必须记录审计日志：

| 操作 | 审计内容 |
|------|----------|
| 反审核 | 操作人、时间、原状态、冲销凭证号 |
| 反结账 | 操作人、时间、期间、审批人 |
| 红字冲销 | 操作人、时间、原单据号、冲销原因 |

---

## 七、性能优化考虑

### 7.1 读写分离

- **主库**：写操作（业务单据创建、状态变更）
- **从库**：读操作（报表查询、历史数据查询）

### 7.2 缓存策略

| 数据类型 | 缓存策略 | 过期时间 |
|----------|----------|----------|
| 主数据 | 全量缓存 + 变更通知 | 永久（变更时刷新） |
| 配置数据 | 全量缓存 | 10 分钟 |
| 报表数据 | 按需缓存 | 1 小时 |
| 高频查询 | 结果缓存 | 5 分钟 |

### 7.3 批量操作

- 批量导入使用批处理（JDBC Batch）
- 批量折旧使用分页处理（避免一次性加载过多数据）
- 期末结账使用异步分批执行

### 7.4 索引优化

**强制索引**：

| 表 | 索引字段 |
|----|----------|
| 业务单据头 | doc_no（唯一）、status、org_id |
| 业务单据行 | head_id、status |
| 库存流水 | material_id、warehouse_id、create_time |
| 凭证 | voucher_no、posted、acct_period_id |

---

## 八、扩展性设计

### 8.1 插件化架构

通过 SPI 机制支持扩展：

```java
// 凭证生成扩展点
public interface IErpFinAcctDocProvider {
    Set<BusinessType> getSupportedBusinessTypes();
    List<VoucherLine> createFacts(BillData billData, AcctSchema acctSchema);
}
```

### 8.2 规则引擎

业务规则配置化：
- 科目映射规则
- 凭证模板规则
- 审批流程规则
- 容差校验规则

### 8.3 审批流程引擎（nop-wf）

使用 nop-wf 工作流引擎实现灵活的审批流程：

**审批流程类型**：

| 流程类型 | 适用单据 | 流程定义位置 |
|----------|----------|--------------|
| 采购审批流程 | 采购订单、采购入库单、采购发票 | `wf/purchase/*.wf.xml` |
| 销售审批流程 | 销售订单、销售报价单 | `wf/sales/*.wf.xml` |
| 财务审批流程 | 付款单、收款单、费用报销 | `wf/finance/*.wf.xml` |
| 资产审批流程 | 资产购置、资产处置 | `wf/assets/*.wf.xml` |

**工作流集成模式**：

```java
// 工作流服务注入
@Inject
private IWorkflowService workflowService;

// 启动审批流程
public void startApproval(String billId, String billType) {
    WorkflowInstance instance = workflowService.createInstance(
        "erp-wf/" + billType + "-approval",
        Map.of("billId", billId)
    );
    workflowService.start(instance.getId());
}

// 处理审批任务
public void completeTask(String taskId, boolean approve, String comment) {
    WorkflowTask task = workflowService.getTask(taskId);
    task.setVariable("approved", approve);
    task.setVariable("comment", comment);
    workflowService.completeTask(taskId);
}
```

**工作流定义示例**（采购订单审批）：

```xml
<workflow name="purchase-order-approval" xmlns="http://www.baidu.com/nop/schema/wf">
    <start>
        <transition to="dept-manager-approval"/>
    </start>
    
    <task id="dept-manager-approval" name="部门经理审批">
        <assignment expression="bill.deptManagerId"/>
        <transition cond="approved" to="finance-approval"/>
        <transition cond="!approved" to="end-rejected"/>
    </task>
    
    <task id="finance-approval" name="财务审批">
        <assignment expression="bill.amount > 10000 ? 'finance-manager' : 'finance-staff'"/>
        <transition cond="approved" to="end-approved"/>
        <transition cond="!approved" to="end-rejected"/>
    </task>
    
    <end id="end-approved" name="审批通过"/>
    <end id="end-rejected" name="审批拒绝"/>
</workflow>
```

**审批状态与业务状态联动**：

| 工作流状态 | 业务单据状态 | 说明 |
|------------|--------------|------|
| RUNNING | APPROVING | 审批进行中 |
| COMPLETED (approved) | APPROVED | 审批通过 |
| COMPLETED (rejected) | REJECTED | 审批拒绝 |
| CANCELLED | CANCELLED | 流程取消 |

### 8.4 报表引擎（nop-report）

使用 nop-report 报表引擎实现灵活的报表生成：

**报表类型**：

| 报表类型 | 用途 | 报表定义位置 |
|----------|------|--------------|
| 财务报表 | 资产负债表、利润表、现金流量表 | `report/finance/*.report.xml` |
| 业务报表 | 采购报表、销售报表、库存报表 | `report/business/*.report.xml` |
| 管理报表 | 项目成本报表、预算执行报表 | `report/management/*.report.xml` |
| 自定义报表 | 用户自定义查询报表 | `report/custom/*.report.xml` |

**报表引擎集成模式**：

```java
// 报表服务注入
@Inject
private IReportService reportService;

// 执行报表
public ReportOutput executeReport(String reportName, Map<String, Object> params) {
    ReportOutput output = reportService.executeReport(
        "erp-report/" + reportName,
        params
    );
    return output;
}

// 导出报表
public byte[] exportReport(String reportName, Map<String, Object> params, String format) {
    ReportOutput output = executeReport(reportName, params);
    return reportService.export(output, format); // format: PDF/EXCEL/HTML
}
```

**报表定义示例**（销售日报表）：

```xml
<report name="sales-daily" xmlns="http://www.baidu.com/nop/schema/report">
    <dataSource>
        <sql>
            SELECT 
                doc_no, customer_name, amount, tax_amount, total_amount, create_time
            FROM erp_sal_sales_order
            WHERE create_time >= :startDate AND create_time < :endDate
            ORDER BY create_time DESC
        </sql>
    </dataSource>
    
    <template>
        <header>
            <title>销售日报表</title>
            <subtitle>日期范围: ${startDate} 至 ${endDate}</subtitle>
        </header>
        
        <body>
            <table>
                <thead>
                    <tr>
                        <th>订单号</th>
                        <th>客户名称</th>
                        <th>金额</th>
                        <th>税额</th>
                        <th>价税合计</th>
                        <th>创建时间</th>
                    </tr>
                </thead>
                <tbody>
                    <tr foreach="row in data">
                        <td>${row.doc_no}</td>
                        <td>${row.customer_name}</td>
                        <td>${row.amount}</td>
                        <td>${row.tax_amount}</td>
                        <td>${row.total_amount}</td>
                        <td>${row.create_time}</td>
                    </tr>
                </tbody>
                <tfoot>
                    <tr>
                        <td colspan="4">合计</td>
                        <td>${sum(data, 'total_amount')}</td>
                        <td></td>
                    </tr>
                </tfoot>
            </table>
        </body>
    </template>
</report>
```

**报表数据源**：

| 数据源类型 | 说明 | 适用场景 |
|------------|------|----------|
| SQL 查询 | 直接数据库查询 | 简单报表 |
| MDX 查询 | OLAP 多维分析 | 复杂数据分析 |
| 自定义 Bean | Java 代码实现 | 复杂逻辑报表 |
| 组合数据源 | 多数据源联合 | 跨域报表 |

### 8.5 多租户支持

- **隔离级别**：共享数据库 + 独立 schema（推荐）
- **字段隔离**：所有业务表带 `tenant_id` 字段
- **配置隔离**：按租户独立配置

---

## 九、错误处理与恢复

### 9.1 错误分类

| 错误类型 | 处理策略 |
|----------|----------|
| 业务错误 | 返回友好提示，记录日志 |
| 系统错误 | 重试 + 熔断 + 告警 |
| 网络错误 | 重试（指数退避） |
| 数据一致性错误 | 人工介入 + 对账 |

### 9.2 重试策略

```java
// 指数退避重试
RetryTemplate retryTemplate = new RetryTemplate();
retryTemplate.setBackOffPolicy(new ExponentialBackOffPolicy());
retryTemplate.setRetryPolicy(new SimpleRetryPolicy(3));
```

### 9.3 熔断保护

```java
// 过账服务熔断
@CircuitBreaker(name = "posting", fallbackMethod = "fallbackPosting")
public void post(BillData billData) {
    // 过账逻辑
}

public void fallbackPosting(BillData billData, Throwable t) {
    // 降级处理：记录失败，等待兜底扫描
}
```

---

## 十、审计与追溯

### 10.1 操作日志

所有业务操作记录：
- 操作人、操作时间、操作类型
- 变更前/后数据
- IP 地址、操作来源

### 10.2 数据血缘

通过回链表追溯数据来源：
- 凭证 → 业务单据 → 库存移动 → 物料/SKU
- 发票 → 入库单 → 采购订单 → 供应商

### 10.3 数据对账

定期对账任务：
- 库存余额 vs 流水汇总
- 应收余额 vs 发票汇总
- 应付余额 vs 发票汇总
- 总账 vs 明细凭证

---

## 十一、合规与内控

### 11.1 职责分离

- 制单与审核分离
- 记账与对账分离
- 审批与执行分离

### 11.2 审计线索

- 所有操作不可篡改（操作日志追加记录）
- 凭证红冲保留原凭证（不删除）
- 反结账记录审批痕迹

### 11.3 合规检查

| 检查项 | 频率 | 责任人 |
|--------|------|--------|
| 凭证借贷平衡 | 实时 | 系统自动 |
| 期间结账完整性 | 月末 | 财务员 |
| 权限合规性 | 定期 | 管理员 |
| 数据一致性 | 每日 | 系统自动 |

---

## 十二、与 Nop Platform 的对齐

### 12.1 技术栈选择

| 维度 | 选择 | 理由 |
|------|------|------|
| ORM | Nop ORM | 与平台深度集成，支持 XML 模型驱动 |
| IoC | Nop IoC | 支持 `@Inject`、`@BizQuery`、`@BizMutation` |
| 消息 | nop-message | 轻量级事件总线，支持异步解耦 |
| 调度 | nop-job | 分布式任务调度 |
| 缓存 | nop-cache | 统一缓存抽象，支持多级缓存 |

### 12.2 代码生成约定

- 模型驱动：`*.orm.xml` → 自动生成 Entity/DAO/Biz
- 业务逻辑：放在 BizModel（`@BizQuery`/`@BizMutation`）
- 复杂编排：放在 Processor
- 扩展点：使用 `@Inject List<*>` 自动聚合

### 12.3 配置管理

- 域配置：`{domain}/_delta/config/`
- 字典配置：`{domain}/_delta/xdef/`
- 视图配置：`{domain}/_delta/view/`

---

## 十三、版本演进策略

### 13.1 向后兼容

- 新增字段非空时提供默认值
- 删除字段前标记为 deprecated（保留至少一个版本）
- API 变更使用版本号控制

### 13.2 数据迁移

- 使用 nop-db-migration 管理 schema 变更
- 迁移脚本按版本顺序执行
- 迁移前备份数据

### 13.3 灰度发布

- 新功能使用 feature flag 控制
- 先在测试环境验证
- 逐步扩大用户范围

---

## 十四、总结

本指南定义了 nop-app-erp 的核心设计原则与约束，各域设计时必须遵守：

1. **单一职责**：每个域只负责核心业务能力
2. **松耦合**：通过接口和事件解耦
3. **幂等性**：保证重复调用安全
4. **事务边界**：区分强一致性与最终一致性
5. **权限安全**：职责分离、审计日志
6. **性能优化**：读写分离、缓存、索引
7. **扩展性**：插件化、规则引擎、多租户

各域设计文档应引用本指南，确保设计一致性。