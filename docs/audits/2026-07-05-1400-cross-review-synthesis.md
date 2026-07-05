# 多角度独立审查综合报告

> 日期：2026-07-05
> 来源：4 个独立子代理（新会话，无前置上下文）从 4 个对抗性角度审查 + 主代理关键发现实时仓库复核
> 基线：`docs/audits/2026-07-05-1300-code-vs-design-vs-best-practices-audit.md`（首轮审计）
> 性质：只读分析 + 关键发现复核验证

## 审查角度

| 代理 | 视角 | 心态 |
|------|------|------|
| 1 | 对抗性测试质量 | 假设测试可能有缺陷，寻找"看起来在测但实际没验证"的测试 |
| 2 | 平台反模式深查 | 寻找前序审计 S1-S8 未覆盖的新反模式（聚焦 M2/M3 扩展域） |
| 3 | 设计-代码语义保真度 | 逐条比对设计承诺与代码实现，验证业务正确性而非"方法存在" |
| 4 | 文档一致性 + 修复验证 | 审查文档内部矛盾、过时引用，验证 M1/V1 修复完整性 |

---

## 🔴 新发现严重问题（5 项，经主代理复核确认）

### C-1：`erp_md_md_organization` 表名拼写错误（跨 7 域）

**状态**：✅ 已确认（grep 实测 7 域源模型）

7 个域的外部实体声明使用 `tableName="erp_md_md_organization"`（双 `md_`），而 master-data 源模型正确为 `erp_md_organization`（单 `md_`）：

```
module-aps/model/app-erp-aps.orm.xml:246         tableName="erp_md_md_organization"
module-b2b/model/app-erp-b2b.orm.xml:554          tableName="erp_md_md_organization"
module-contract/model/app-erp-contract.orm.xml:584 tableName="erp_md_md_organization"
module-cs/model/app-erp-cs.orm.xml:656            tableName="erp_md_md_organization"
module-drp/model/app-erp-drp.orm.xml:311          tableName="erp_md_md_organization"
module-hr/model/app-erp-hr.orm.xml:1341           tableName="erp_md_md_organization"
module-logistics/model/app-erp-logistics.orm.xml:337 tableName="erp_md_md_organization"
```

**对比**：`module-master-data/model/app-erp-master-data.orm.xml:804` 正确为 `tableName="erp_md_organization"`。

**风险**：运行时查询 organization 外部实体时引用不存在的表 `erp_md_md_organization`，导致 SQL 异常或静默查空。复制粘贴传播的典型 bug。

**建议**：7 域源模型统一改为 `erp_md_organization`，`mvn clean install` 增量重生成。应记入 `docs/bugs/`。

---

### C-2：Sales 信用额度控制语义偏离（3 项设计承诺未实现，无 Non-Goal 声明）

**状态**：✅ 已确认（`CreditLimitChecker.java` 精读 + grep 验证）

**语义保真度评分：4/10**——三个审查域中最低。

| 设计承诺 | 实现状态 | 证据 | 风险 |
|----------|----------|------|------|
| 三级控制含 SPECIAL_APPROVAL（超额度需额外审批人） | 🔴 **未实现** | `CreditLimitChecker.java:64-75` 仅二分：`HARD_BLOCK` 抛异常，其余全 `LOG.warn` 放行。无审批流触发、无审批人路由 | 中等（设计留白） |
| 额度 = 信用额度 − **AR 未核销余额** − 未出库订单 | 🔴 **未实现** | `CreditLimitChecker.java:84` `sumOutstanding` 仅查 `ErpSalOrder`，**完全未纳入 AR_INVOICE**。grep 确认零处 `ErpFin`/`receivable` 引用 | **高（可绕过信用控制）** |
| 多币种按业务日期汇率换算本位币 | 🔴 **未实现** | `CreditLimitChecker.java:62,94-101` 直接累加 `totalAmountWithTax` 不分币种，`available.compareTo(orderAmount)` 跨币种比较。grep 零处 `exchange`/`currency`/`rate` | **高（外币订单信用判断错误）** |

**与 MRP 域对比**：MRP 的简化（在途/在制未纳入、批量法则简化）在 `mrp.md:84-95` 有显式 Non-Goal 声明 + 触发条件。CreditLimitChecker 仅在 javadoc:28 标注"MVP"，**README.md:75-83 未标 Non-Goal**，新读者会以为已完整实现。

**后果举例**：客户信用额度 100 万 CNY。下两笔 10 万 USD 未发货订单（≈144 万 CNY）。代码直接 `100万 − 20万(USD数值) = 80万 > 0` 放行——实际应占用 144 万已超额。开票后 AR 余额也不计入，可继续下单。

**建议**：(1) 至少在 `sales/README.md` 标注 Non-Goal；(2) 多币种问题优先修复（财务风险最高）。

---

### C-3：assets→finance ORM 引用违反 DAG

**状态**：✅ 已确认（`module-assets/model/app-erp-assets.orm.xml:267,713-714`）

```xml
<!-- assets.orm.xml:267 — assets→finance 反向 ORM 引用 -->
<to-one name="voucher" refEntityName="app.erp.fin.dao.entity.ErpFinVoucher" ...>
<!-- assets.orm.xml:713-714 — 外部实体声明 -->
<entity name="app.erp.fin.dao.entity.ErpFinVoucher" notGenCode="true" tableName="erp_fin_voucher">
```

`module-boundaries.md:44` 明确：`app-erp-assets | 允许依赖: master-data / inventory | 禁止依赖: finance（finance 引用 assets，不反向）`。

**同类发现**：`module-hr/model/app-erp-hr.orm.xml:496-497` 也有 hr→projects 的 2 个 to-one（`ErpPrjProject` + `ErpPrjTask`），hr 允许依赖表未列 projects。

**建议**：二选一——(a) 改弱指针（删 to-one，用 `voucherId` 裸字段 + 应用层查询）；(b) 如业务确需，更新 `module-boundaries.md` 白名单（与 M1 同策略）。

---

### C-4：敏感凭证字段 GraphQL 可查询（安全风险）

**状态**：✅ 已确认（XMeta `queryable="true"`）

```
module-logistics/.../ErpLogCarrierConfig/_ErpLogCarrierConfig.xmeta:46  apiKey      queryable="true"
module-logistics/.../ErpLogCarrierConfig/_ErpLogCarrierConfig.xmeta:50  apiSecret   queryable="true"
module-logistics/.../ErpLogCarrierConfig/_ErpLogCarrierConfig.xmeta:54  credentials queryable="true"
module-b2b/.../ErpB2bPartnerCredential/_ErpB2bPartnerCredential.xmeta:39 credentialValue
module-b2b/.../ErpB2bPartnerProfile/_ErpB2bPartnerProfile.xmeta:58    webhookSecret
```

**风险**：承运商 API 密钥、B2B webhook 密钥等凭证字段通过 GraphQL 查询可直接返回明文。`queryable="true"` 使字段出现在列表查询结果中。

**注意**：这些是 `_` 前缀的**生成** XMeta 文件，派生自 ORM 模型。修复应在 ORM 源模型中为这些字段标记 `notQueryable` 或在保留层 XMeta 中用 Delta 覆盖 `queryable="false"`。

**建议**：在 `module-logistics/model/*.orm.xml` + `module-b2b/model/*.orm.xml` 中为凭证字段加安全标注；或保留层 XMeta Delta 覆盖。

---

### C-5：`LocalDateTime.now()` 等时间获取违规蔓延（60 处 / 12 域）

**状态**：✅ 已确认（grep 实测 60 处）

前序审计 S5 仅在 inventory 域发现 1 处 `LocalDate.now()` 遗漏。**扩展域审查发现 60 处**，分布：

| 域 | 处数 | 域 | 处数 |
|----|------|----|------|
| quality | 10 | b2b | 7 |
| hr | 10 | finance | 4 |
| cs | 10 | drp | 4 |
| manufacturing | 8 | maintenance | 3 |
| logistics/crm/contract/aps | 各 1 | | |

**规则**：`ai-defaults.md` 要求用 `CoreMetrics.currentTimeMillis()` / `CoreMetrics.today()` / `CoreMetrics.now()`，以支持平台级时间 mock（测试可控时间）。

**影响**：测试无法通过 `CoreMetrics` mock 控制这些域的时间相关逻辑（如 SLA 截止时间计算、薪酬期间判定、排班日期）。部分处可能合法（如纯展示用），但 60 处需逐处审查。

---

## 🟡 新发现中等问题（6 项）

### C-6：M1 修复不完整（`system-baseline.md` + `data-dependency-matrix.md` §2 未同步）

**状态**：✅ 已确认

M1 修复更新了 `module-boundaries.md` 核心域表 + `data-dependency-matrix.md` §5.6 段，但以下位置仍为旧规则：

| 位置 | 过时内容 | 风险 |
|------|---------|------|
| `system-baseline.md:119` | "跨业务域引用（**finance → projects/assets**）单向合法；其他业务域之间走弱指针"——未提 purchase/sales→projects | 🔴 与 M1 直接冲突 |
| `data-dependency-matrix.md:51` | §2.1 "L1 直接下游（R: master-data；**无业务域间 ORM 引用**）" | 🟡 |
| `data-dependency-matrix.md:79-80,102-103,106` | §2.2/§2.3 purchase/sales/projects 依赖列缺项 | 🟡 |

**建议**：补齐 `system-baseline.md:119` + `data-dependency-matrix.md` §2 全段。

---

### C-7：Roadmap 状态三层漂移

**状态**：✅ 已确认

| 文件 | 问题 |
|------|------|
| `implementation-roadmap.md:11` | 称 extended-roadmap "全 todo"，但 M2/M3 实际已全 done |
| `extended-roadmap.md:36` | Work Item Status 称"3.10–3.21：todo"，同文件 `:70-81` Implementation Order 标 ✅ done |
| `backlog/README.md:13` | P0 = "CRUD Milestone 3（8 新增域）" 标 `implement`，但 crud-roadmap M3 已全 done |

**建议**：统一三层 roadmap 状态。

---

### C-8：`module-boundaries.md:96` Owner Docs 目录名过时

```
| 客户服务域业务规则 | `docs/design/cs/`（待补 README） |   ← 实际目录为 customer-service/，且已有 README
| 人力资源域业务规则 | `docs/design/hr/`（待补 README） |   ← 实际目录为 human-resource/，且已有 README
```

---

### C-9：跨域写绕过 I*Biz（MrpReleaseService 构造 ErpPurOrder）

`MrpReleaseService.java` 释放采购申请时直接 `new ErpPurOrder()` + `daoProvider.daoFor(ErpPurOrder.class).saveEntity()`，绕过 `IErpPurOrderBiz` 管道（数据权限/Meta/状态机校验）。Javadoc 明确"承认偏离"。同类模式可能在其他 release/dispatcher 类中存在。

---

### C-10：异常处理不合规（2 处）

- `InspectionResultEvaluator.java:71` 抛 `IllegalStateException` 而非 `NopException`（违反 `error-handling.md`）
- `StandardCostResolver` / `RequisitionToOrderConverter` 存在 `catch (Exception ignored)` 吞异常后返回 null

---

### C-11：flow-overview 状态机/事务描述过时

- `flow-overview.md:312` 工单列 `INSPECTING` 状态，但 `manufacturing/state-machine.md:171` 明确字典无此态
- `flow-overview.md:499` "期末结账 = 分布式事务"，但项目为单库 Quarkus（`system-baseline.md:9,16`），无分布式事务

---

## 🟢 已确认良好的区域

### 测试质量评分：8.5/10（对抗性审查通过）

代理 1 以对抗性心态逐项排查，**未发现恒真断言或自动通过的快照**：
- 0 处 `assertEquals(x, x)` / `assertTrue(true)`
- 快照 `@var:` 100% 集中在 `createTime`/`updateTime`（正确用法），无业务字段被屏蔽
- BigDecimal 比较全部用 `compareTo`（0 处 `equals`）
- 状态机测试覆盖非法迁移 + 幂等 + 前置条件
- 跨域联动测试验证真实数值变化（余额恢复、凭证金额、辅助账 openAmount）
- 冲销/红冲路径覆盖完整

**轻微风险**：8+ 处配置变更未用 try/finally 清理（`TestErpSalOrderApproval:275` 等）；纯订单审批幂等性（重复 approve 同一已审单）未显式测试。

### 设计-代码语义保真度（Finance 8/10、MRP 7/10）

- **Finance 过账引擎**：借贷平衡校验、红字冲销、Provider 路由、冲销回写双向全部忠实实现。扣分项为 ASYNC 切换 + 兜底扫描未落地（设计自承 follow-up）。
- **MRP 引擎**：BOM 多级递归展开、提前期倒推、计划订单释放忠实实现。扣分项为净需求口径简化（在途/在制未纳入）+ 批量法则简化——**均有 mrp.md:84-95 显式 Non-Goal 声明**。

### M1/V1 修复验证

- **V1（DRP 菜单）**：🟢 通过——5 个资源 url 的 page 目录均存在，实体名匹配
- **M1（projects DAG 文档）**：🟡 目标 5 处通过，但 `system-baseline.md:119` + `data-dependency-matrix.md` §2 未同步（见 C-6）

### 平台合规正面确认

- `@Inject private`：0 命中 ✅
- 第三方 JSON 库：0 命中 ✅
- Apache Commons StringUtils：0 命中 ✅
- M3 新域 dict valueType：全 string（O3 前向指导已采纳）✅
- `@BizMutation @Transactional` 冗余：扩展域 Processor 注释明确不带 ✅

---

## 综合风险矩阵

| 优先级 | ID | 问题 | 影响 |
|--------|----|------|------|
| 🔴 P0 | C-1 | `erp_md_md_organization` 表名错误（7 域） | 运行时 SQL 异常 |
| 🔴 P0 | C-2 | Sales 信用额度多币种 + AR 余额漏算 | 信用控制可绕过（财务风险） |
| 🔴 P0 | C-4 | 敏感凭证字段 GraphQL 可查询 | 安全风险（密钥泄露） |
| 🔴 P1 | C-3 | assets→finance / hr→projects 违反 DAG | 文档-实现冲突 |
| 🔴 P1 | C-5 | 60 处 `LocalDateTime.now()` 蔓延 | 测试不可控时间 |
| 🟡 P2 | C-6 | M1 修复不完整（system-baseline + §2） | 文档自相矛盾 |
| 🟡 P2 | C-7 | Roadmap 三层状态漂移 | 误导工作项选择 |
| 🟡 P2 | C-9 | 跨域写绕过 I*Biz | 数据权限跳过 |
| 🟡 P3 | C-8/C-10/C-11 | 目录名过时 / 异常不合规 / flow-overview 过时 | 文档/代码整洁度 |

---

## 与首轮审计的关系

首轮审计（`2026-07-05-1300-*.md`）评分 ~8.0，聚焦前序 S1-S8 + O1-O4 + M1-V4 的修复进度。本次多角度审查**发现首轮未覆盖的 11 项新问题**（C-1 至 C-11），主要集中在：

1. **扩展域（M2/M3）的盲区**：首轮聚焦核心 4 域，扩展域的 `LocalDateTime.now()`、assets→finance DAG、表名错误均未覆盖
2. **设计语义深层验证**：首轮验证"方法是否存在"，本次验证"是否忠实实现设计承诺"——Sales 信用额度因此暴露
3. **文档横向一致性**：首轮聚焦 M1/V1 目标文件，本次扫描全文档体系——发现 system-baseline / roadmap / flow-overview 的矛盾

**修正后综合评分**：首轮 ~8.0 → 考虑新发现后 **~7.0**（C-1 表名错误 + C-2 信用额度 + C-4 安全字段为实质性风险，非文档级问题）。
