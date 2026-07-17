# Nop DDD 实现差距分析：实体贫血与 daoFor() 滥用

> 编写日期：2026-07-16
> 状态：addressed（Gap A 全 21 Processor 状态方法已上提到实体 / Gap B Type 1+4 已修复 / 防护机制已建立；详见 `docs/plans/2026-07-16-2134-1-ddd-entity-methods-daofor-convergence.md` completed）
> 审计来源：logs（06-22~07-15）、audits（07-01~07-05）、docs-for-ai DDD 指南、生产代码扫描

---

## 1. 分析范围

本报告分析两个独立但相关的实现差距，它们常被混为一谈但根因不同：

| 差距 | 本质 | 严重度 |
|------|------|--------|
| **(A) 实体贫血** | 实体手写层（`ErpPurOrder.java`）11 行空壳，无任何领域方法 | 中 |
| **(B) daoFor() 滥用** | BizModel/Processor 中 `daoFor(Xxx.class)` 直读直写，绕过 I*Biz 管道 | 高（~600 处） |

---

## 2. 事实确认

### 2.1 实体现状

全域（18+1 域）实体手写类均为 11 行空壳：

```java
// ErpPurOrder.java (11 行) — 无任何方法
@BizObjName("ErpPurOrder")
public class ErpPurOrder extends _ErpPurOrder{
}
```

同一模式的验证：purchase/sales/finance 三域的 `ErpPurOrder`、`ErpSalOrder`、`ErpFinVoucher` 全部确认。

### 2.2 daoFor() 现状

来源：`docs/audits/2026-07-05-1500-supplementary-audit-and-checker.md`，工具检测结果：

| 规则 | 命中 | 说明 |
|------|------|------|
| BizModel `daoFor(Erp*)` 跨域 | 116 | 含同域子实体（约半数），跨域~60 |
| Processor `daoFor(ErpMd*)` 跨 master-data | 28 | 跨域辅助层 |
| 全生产代码 `daoFor()` 总量 | **~600** | 含同域直读 |
| `dao().updateEntity()` in BizModel | 48 | 绕过 CrudBizModel 管道 |
| `new Erp*()` 直接构造 | 38 | 未用 newEntity() |

### 2.3 Nop DDD 文档要求

来源：`nop-entropy/docs-for-ai/02-core-guides/domain-logic-and-ddd.md`

| 期望位置 | 内容 | 当前状态 |
|---------|------|---------|
| **Entity** | `isXxx()`, `canXxx()`, `calculateXxx()`, 基于字段和关联的只读 helper | ❌ 全缺 |
| **Entity** | `requireBiz(I*Biz.class)` 做只读查询（可缓存 `computeIfAbsent`） | ❌ 全缺 |
| **BizModel** | `@BizQuery`/`@BizMutation` 入口，事务/权限边界 | ✅ 86 个含真实 `@BizMutation`/`@BizQuery` 方法（来源：`docs/audits/2026-07-05-1300-code-vs-design-vs-best-practices-audit.md:31`） |
| **Processor** | 多步编排，protected step 方法，跨聚合协作 | ✅ 模式正确 |

`05-examples/entity-class.java` 提供了标准示例：

```java
public class Order extends _Order {
    public boolean isStatus(int status) { ... }
    public void recalcTotalPrice() { ... }
    public List<OrderItem> getActiveItems() {
        IOrderItemBiz biz = requireBiz(IOrderItemBiz.class);
        // 只读查询
    }
}
```

---

## 3. 差距 (A)：实体贫血——根因分析

### 3.1 根因一：代码生成器产生空壳，无计划要求填补

项目始于 `nop-cli gen` 生成的骨架。所有实体手写类是空壳，且 `docs/plans/` 中所有业务深化计划（经 grep 抽样验证零命中"实体方法"/"上提"/"领域方法"等关键词）**检查清单都不检查实体方法**。开发者的任务描述始终是"在 BizModel 上加方法"，从未收到"请到实体上加领域方法"的指令。

证据：`docs/analysis/2026-06-25-1649-ai-automation-roadmap.md:11`：
> "所有域 codegen 骨架已生成（1096 个 Java 文件），含实体类、DAO、I*Biz 接口、BizModel 空壳、XMeta、view.xml 骨架"

### 3.2 根因二：BizModel 是唯一被教授和使用的模式

Nop 文档、示例代码、技能（nop-backend-dev）都以 BizModel/Processor/I*Biz 为核心。`domain-logic-and-ddd.md` 虽在 docs-for-ai 中存在，但**没有对应路由**要求开发者实现实体方法。自然形成了"业务逻辑→BizModel"的肌肉记忆。

### 3.3 根因三：Processor 层复制粘贴了状态判断方法

每个 Processor 都重复定义了相同的 `isAlreadyApproved()`/`isAlreadyRejected()`/`validateNotCancelled()`：

```java
// ErpPurOrderProcessor.java:268
protected boolean isAlreadyApproved(ErpPurOrder order) {
    String status = order.getApproveStatus();
    return status != null && Objects.equals(status, ErpPurConstants.APPROVE_STATUS_APPROVED);
}

// ErpSalOrderProcessor.java:222 — 完全相同逻辑
// ErpPurReceiveProcessor.java:343 — 完全相同逻辑
// ErpPurInvoiceProcessor.java:248 — 完全相同逻辑
// ErpPurRequisitionProcessor.java:268 — 完全相同逻辑
// ErpPurReturnProcessor.java:302 — 完全相同逻辑
// ErpPurPaymentProcessor.java:312 — 完全相同逻辑
// ErpSalDeliveryProcessor.java:364 — 完全相同逻辑
// ErpSalInvoiceProcessor.java:267 — 完全相同逻辑
// ErpSalQuotationProcessor.java:278 — 完全相同逻辑
// ErpSalReceiptProcessor.java:... — 完全相同逻辑
// ... 更多
```

这些方法在语义上属于实体（稳定的领域事实），不应散落在各个 Processor 中。

> 注：`validateNotCancelled` 抛出 `NopException`，不满足实体方法只读约束——实体只承担 `isCancelled()` 布尔判断，异常抛出的校验逻辑保留在 Processor 中。

### 3.4 根因四：无审查机制捕捉该缺口

`nop-backend-dev` skill 的自检清单有 20 项技术正确性检查（来源：`bizmodel-method-selfcheck.md`） + 4 项产品化可定制性检查，但**没有一条要求检查实体是否有可上提的领域方法**。

---

## 4. 差距 (B)：daoFor() 滥用——根因分析

### 4.1 错误归因澄清

**daoFor() 之所以泛滥，原因不是单一的，而是六种不同力量叠加。** 必须区分对待，不能一概而论：

#### 类型 1：ORM 关系导航替代（❗ 明显反模式，无正当理由）

```java
// ErpSalOrderBizModel.java:87-92
protected List<ErpSalOrderLine> loadOrderLines(Long orderId) {
    IEntityDao<ErpSalOrderLine> dao = daoFor(ErpSalOrderLine.class);
    QueryBean q = new QueryBean();
    q.addFilter(eq("orderId", orderId));
    return new ArrayList<>(dao.findAllByQuery(q));
    // 应直接 return order.getLines()
}
```

`order.getLines()` 可直接通过 ORM 延迟加载获取，`loadOrderLines` 多余。

> 注：此模式也出现在 Processor 中（如 `ErpPurOrderProcessor.loadLines:297`），归入本类型的 ~90 处估算。

#### 类型 2：同域子实体访问（🟡 有争议，可接受但可优化）

```java
// ErpPurOrderBizModel.java:62 — save 同域子实体
for (ErpPurOrderLine orderLine : converter.buildLines(order, lines, request)) {
    daoFor(ErpPurOrderLine.class).saveEntity(orderLine);
}
```

同域子实体访问在审计中标记为"假阳性"（`2026-07-05-1500` audit §2.5："R2b 的 116 处包含同域 `daoFor`...不全是跨域"）——即跨域检测工具对同域调用过度捕获，并非说同域 `daoFor` 是推荐实践。可优化为通过 ORM `order.getLines().addAll(...)` 利用级联保存，但非强制。

#### 类型 3：Processor 反向调用 BizModel（🔴 架构约束，不可用 I*Biz）

```java
// ErpPurOrderProcessor.java:205 — Processor 内查会计期间
IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
```

Processor 的 BizModel 注入是单向的（BizModel → Processor），不可反注入 I*Biz（会导致 IoC 循环依赖）。这一事实在 `docs/logs/2026/07-08.md:378` 明确记录：

> "Processor 的 BizModel 注入为单向（BizModel → Processor），不可反注入 I*Biz（循环依赖）。Processor 内部 `xxxDao().updateEntity(entity)` 是 `processor-extension-pattern.md` 认可的内部编排持久化模式，不强制重构。"

**这是 Processor 架构的设计约定**：Processor 的注入方向为单向（BizModel → Processor），日志 H-4 裁决明确禁止反注入 I*Biz——即使当前实例不构成硬循环，允许反注入也会破坏编排层的封装边界，使 Processor 不再可独立测试和定制。Nop IoC 容器的 `ioc:ignore-depends` 是逃生舱而非默认行为。

#### 类型 4：BizModel ↔ BizModel 双向注入（🔴 设计问题，不应以 daoFor() 解决）

```java
// ErpCsSurveyBizModel 不注入 IErpCsTicketBiz，用 daoFor() 代替
// 原因：TicketBizModel 已注入 IErpCsSurveyBiz → 形成循环
```

**这是架构边界错误**，不是循环依赖问题。如果 Survey 只需校验工单是否存在，应通过 ORM 关联导航解决（Survey 有 `ticketId` 外键），根本不需要注入 IErpCsTicketBiz。正确的修复方向是**调整模块边界或使用 ORM 关联**，而非退化为 daoFor()。

#### 类型 5：看板/报表只读聚合（🟢 可接受，有注释即合规）

```java
// ErpSalDashboardBizModel.java — 跨域只读聚合
IDaoProvider daoProvider;
List<ErpSalInvoice> invoices = daoProvider.daoFor(ErpSalInvoice.class).findAllByQuery(q);
```

纯只读聚合查询（不写库、不经业务管道）有合理的存在理由——经过 I*Biz 的 `findList` 会受 XMeta 查询算子白名单约束，内部聚合查询不需要这些约束。但需要加注释说明原因。

#### 类型 6：历史遗留/早期代码（🟡 已认知的技术债务）

~600 处 `daoFor()` 中，早期实现（6月25-30日）占了相当比例。7月1日审计后主入口方法已整改，但 Processor 层和扩展域仍有大量残留。

### 4.2 各类占比估算

> **估算方法说明**：以下比例为粗估（±10pp），基于审计报告的 600 处总量和三类已知数据点（116 处 BizModel 跨域 daoFor、28 处 Processor 跨 master-data daoFor、工具标记的假阳性）按典型模式 grep 样本外推。各类型处数由总量×占比反推，**不是精确测量**，仅用于示意各类的相对规模。

| 类型 | 估算占比 | 处数 | 处理策略 |
|------|---------|------|---------|
| 1. ORM 导航替代 | ~15% | ~90 | 应重构 |
| 2. 同域子实体 | ~35% | ~210 | 可优化，非强制 |
| 3. Processor 架构约束 | ~20% | ~120 | 保留，加注释 |
| 4. 设计边界错误 | ~5% | ~30 | 应重构（改边界） |
| 5. 看板/报表 | ~10% | ~60 | 保留，加注释 |
| 6. 历史残留 | ~15% | ~90 | 逐步清理 |

---

## 5. 差距 (A) 与 (B) 的关系

**两者是独立的，不应混为一谈：**

- 即使所有实体加上了 `isApproved()`/`isCancelled()` 等方法，Processor 中的 `daoFor()` 问题依然存在
- 即使所有 `daoFor()` 替换为 I*Biz 注入，实体依然贫血
- 修复方案不同，应分两条线推进

---

## 6. 修复建议

### 6.1 差距 (A)：实体方法上提

**原则：** 只上提满足 `domain-logic-and-ddd.md` 四条约束的方法：
1. 只读，不写库
2. 不调用外部系统
3. 不依赖易变业务策略
4. 表达稳定领域事实

**操作步骤：**

1. **搜索公式化的状态判断**：全域搜索 Processor 中的 `isAlreadyApproved()`/`isAlreadyRejected()`/`validateNotCancelled()`，统一上提到对应实体：
   ```java
   // ErpPurOrder.java
   public boolean isApproved() {
       return Objects.equals(getApproveStatus(), ErpPurConstants.APPROVE_STATUS_APPROVED);
   }
   public boolean isRejected() {
       return Objects.equals(getApproveStatus(), ErpPurConstants.APPROVE_STATUS_REJECTED);
   }
   public boolean isCancelled() {
       return Objects.equals(getDocStatus(), ErpPurConstants.DOC_STATUS_CANCELLED);
   }
   public boolean isSubmitted() {
       return Objects.equals(getApproveStatus(), ErpPurConstants.APPROVE_STATUS_SUBMITTED);
   }
   ```

2. **搜索纯计算逻辑**：BizModel 中基于实体字段的只读计算（如金额汇总、状态组合判断）上提到实体。

3. **更新 Processor**：将 `order.isAlreadyApproved(order)` 替换为 `order.isApproved()`。

4. **修改 skill 自检清单**：在 `nop-backend-dev` skill 中增加 E1 检查项："实体上有无可上提的稳定领域方法？"

### 6.2 差距 (B)：daoFor() 收敛

**分三阶段推进：**

| 阶段 | 范围 | 策略 | 工作量 |
|------|------|------|--------|
| **P0** | 类型 1 + 类型 4（~120 处） | 重构：ORM 导航替换 + 模块边界调整 | 3-5 天 |
| **P1** | 类型 6（~90 处历史残留） | 逐处审计：注入 I*Biz 或加注释 | 2-3 天 |
| **P2** | 类型 3 + 类型 5（~180 处） | 加注释说明原因，不强制重构 | 1 天 |

**具体操作示例：**

```java
// ❌ 类型 1：应直接用 ORM 导航
protected List<ErpPurOrderLine> loadLines(Long orderId) {
    IEntityDao<ErpPurOrderLine> dao = daoProvider.daoFor(ErpPurOrderLine.class);
    QueryBean q = new QueryBean();
    q.addFilter(eq("orderId", orderId));
    return new ArrayList<>(dao.findAllByQuery(q));
}

// ✅ 改为：order.getLines()
```

```java
// ❌ 类型 4：设计边界错误，不应以 daoFor() 绕循环
// ErpCsSurveyBizModel 校验工单存在
ErpCsTicket ticket = daoFor(ErpCsTicket.class).getEntityById(ticketId);

// ✅ 正确方案：如果 Survey 有 ticketId 外键，直接 ORM 导航
ErpCsTicket ticket = this.getTicket(); // ORM 透明懒加载，无循环依赖
```

```java
// ✅ 类型 3 + 5：保留 daoFor()，但加标准注释
// daoProvider 直读原因：Processor 不可反注入 I*Biz（架构约束，见 processor-extension-pattern.md）
```

---

## 7. 防止复发

1. **修改 `nop-backend-dev` skill 自检清单**：增加 E1（实体方法）、E2（ORM 导航优先）、E3（daoFor 注释原因）三项检查。
2. **修改计划模板**：在所有业务深化计划的检查清单中增加"实体方法上提"项。
3. **添加审计规则**：`docs/audits/nop-compliance-checker.sh` 增加 `isAlreadyApproved`/`isAlreadyRejected` 在 Processor 中出现次数的追踪（作为实体方法缺位的代理指标）。

---

## 8. 结论

| 问题 | 根因 | 修复方向 | 优先级 |
|------|------|---------|--------|
| 实体贫血 | 计划项缺失（无检查清单要求）+ 认知惯性（BizModel 唯一模式） | 汇总状态判断方法到实体，修改 skill/计划模板 | 中 |
| daoFor() 滥用 | 六种不同原因叠加，大部非恶意 | 分类处理：ORM 导航替代 + 注释保留 + 边界调整 | 高 |
