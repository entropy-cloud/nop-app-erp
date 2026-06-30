---
日期: 2026-06-29
来源: Plan 02 文档改进决议 × erp-survey 13 个项目
状态: 分析完成
---

# Plan 02 文档改进 × 开源 ERP 对照分析

> 将 Plan 02 中 81 个设计审查决议与 erp-survey 调研的 13 个开源 ERP 项目的实现做横向对照，识别"我们的选择是否是最优解"和"开源项目中有无更好做法"。

## 分析方法

- **对照对象**：Plan 02 的 8 个阶段 65+ 个执行项
- **参考项目**：Odoo、ERPNext、iDempiere、Metasfresh、赤龙 ERP、管伊佳 ERP、星云 ERP、Dolibarr、Tryton 等
- **评价维度**：设计完整性、开源验证度、工程最佳实践、中国本地化适配

---

## 一、业财过账机制（Phase 1-2 核心）

### 我们的选择

PostingEvent 异步过账 + posted 幂等 + 兜底扫描（Q4/Q14）。

### 开源对比

| 项目 | 过账机制 | 延迟 | 幂等保证 |
|------|----------|------|----------|
| **nop-app-erp** | post-commit 事件 + posted 标志 + 定时扫描 | 异步 | posted 检查 |
| **ERPNext** | `on_submit` → `make_gl_entries()` 同步 | 同步 | 无显式幂等（依赖事务） |
| **赤龙** | 审批触发 → `autoCreateVoucher()` 同步 | 同步 | 无显式幂等 |
| **Metasfresh** | `DocumentPostingBusService` post-commit EventBus | 异步 | 类型安全注册 + 重试 |
| **iDempiere** | 30 秒 DB 轮询 `AcctProcessor` | 轮询 | posted 状态检查 |

### 分析

- **我们的选择是正确的**：Metasfresh 的 EventBus 异步过账是工程最佳实践，我们采纳了其核心思路（post-commit + 异步），并增加了兜底扫描（借鉴 iDempiere 的 posted 轮询）
- **可改进点**：
  1. Metasfresh 的 **类型安全注册**（`ImmutableMap<String, AcctDocFactory>`）比我们的接口注册更优雅——可考虑在 BizModel 层用 `@Inject Map<String, IDocFactory>` 替代反射
  2. ERPNext 的 **统一 on_submit 钩子** 比我们的事件驱动更简洁——但异步更适合高并发场景
  3. iDempiere 的 DB 轮询延迟高（30 秒），我们选择事件 + 扫描双保险是更好的折中

---

## 二、状态机设计（Phase 1-2）

### 我们的选择

三轴状态分离（docStatus + approveStatus + posted），双轴独立描述（Q1/Q3/Q20）。

### 开源对比

| 项目 | 状态机范式 | 状态维度 | 审批方式 |
|------|-----------|---------|---------|
| **nop-app-erp** | 三轴分离（docStatus/approveStatus/posted） | 3 轴 | nop-wf 可选 |
| **赤龙** | 三轴分离（status/approveStatus/paidStatus） | 3 轴 | 内置审批 |
| **ERPNext** | 单状态 + 量化推导 | 1 轴 | 无独立审批 |
| **Tryton** | 声明式 `_transitions` + `@transition` | 1 轴 | 可选 workflow |
| **iDempiere** | DocumentEngine 统一状态机 | 1 轴 | AD_Workflow |
| **管伊佳** | type+subType 双维度 | 2 维 | 插件式多级审批 |

### 分析

- **我们的三轴分离与赤龙一致**，这是国产 ERP 的标准做法——验证了设计正确性
- **可改进点**：
  1. Tryton 的 **声明式状态机**（`_transitions` 字典 + `@transition` 装饰器）比我们的文档描述更形式化——建议在 ORM 模型中用 XML 声明状态转换规则，而非仅在文档中描述
  2. ERPNext 的 **量化推导模式**（状态由收货/开票百分比自动推导）值得借鉴——可考虑在 docStatus 中增加"部分完成"的自动推导逻辑
  3. iDempiere 的 DocumentEngine 虽然强大但过度设计（OMG 规范），我们选择 nop-wf 可选是更务实的

---

## 三、凭证模板与借贷分录（Phase 1-2）

### 我们的选择

凭证三件套（Voucher/VoucherLine/VoucherBillR）+ 凭证模板 + 占位符填充 + 借贷方向约定（Q40）。

### 开源对比

| 项目 | 凭证模型 | 模板机制 | 借贷方向 |
|------|----------|----------|----------|
| **nop-app-erp** | 三件套 + 业财回链 | 模板 + 占位符 | 五大类约定 |
| **赤龙** | 三件套（最完整） | Model + 占位符 `"AMOUNT"` | 隐含在模板中 |
| **ERPNext** | GL Entry 行 | 无模板（硬编码） | 内建 |
| **Metasfresh** | Fact/FactLine | Doc 模板方法 | 类型安全 |
| **iDempiere** | Fact_Acct | Doc_ 反射命名 | 内建 |
| **Dolibarr** | bookkeepingtemplate | PHP 模板类 | 内建 |

### 分析

- **我们的凭证三件套与赤龙完全一致**，这是中式复式记账的标准骨架
- **可改进点**：
  1. 赤龙的 **凭证号按 Redis 流水连续编号**——我们选择 orgId 内唯一是正确的（Q10），但实现时可借鉴 Redis 流水方案避免断号
  2. Metasfresh 的 **Doc 模板方法**（`loadDocumentDetails()` + `createFacts()`）比我们的 Provider 接口更清晰——可考虑在 `IErpFinAcctDocProvider` 中增加 `loadDocumentDetails()` 钩子
  3. iDempiere 的 **FactLine 双币种字段**（`AmtSourceDr/Cr` + `AmtAcctDr/Cr`）是行业标准——我们的多币种四件套设计已覆盖

---

## 四、会计期间与删除策略（Phase 1）

### 我们的选择

- 会计期间：NOT_OPENED→OPEN→CLOSING→CLOSED（Q2）
- 删除策略：三档（草稿物理删除/已审核作废/已过账禁止）（Q11）
- 跨域统一复用 finance 期间（Q19）

### 开源对比

| 项目 | 期间状态 | 删除策略 | 跨域期间 |
|------|----------|----------|----------|
| **nop-app-erp** | 4 态（含 NOT_OPENED） | 三档 | 统一复用 |
| **iDempiere** | OPEN/CLOSED | 软删除 | 多 AcctSchema 独立 |
| **ERPNext** | OPEN/CLOSED | 软删除 | 统一 |
| **赤龙** | OPEN/CLOSED | 软删除 | 统一 |
| **Odoo** | OPEN/CLOSED | 软删除 | 按公司 |

### 分析

- **我们的 NOT_OPENED 初始态比开源项目更精确**——大多数开源项目只有 OPEN/CLOSED 两态，我们增加 NOT_OPENED 解决了"刚创建期间不应是已结账"的语义问题（Q2）
- **可改进点**：
  1. iDempiere 的 **多 AcctSchema 独立期间** 比我们的"统一复用 finance 期间"更灵活——如果未来需要税务账期间与财务账期间不同步，可能需要重新审视
  2. 开源项目普遍使用 **软删除**（deleted flag），我们选择三档策略（物理删除/作废/禁止）更严格——这是 ERP 数据完整性的正确选择

---

## 五、ErrorCode 与异常处理（Phase 1）

### 我们的选择

ErrorCode 域级命名空间 + 4 位编码 + NopException（Q12）。

### 开源对比

| 项目 | 错误码体系 | 描述语言 |
|------|-----------|---------|
| **nop-app-erp** | `erp.<domain>.<code>` 命名空间 | 中文（i18n） |
| **iDempiere** | AD_Message 表驱动 | 多语言 |
| **ERPNext** | 异常类 + 消息键 | 多语言 |
| **赤龙** | 枚举类 | 中文 |
| **管伊佳** | 异常码 + 消息 | 中文 |

### 分析

- **我们的设计与 iDempiere 的 AD_Message 表驱动思路一致**，但更简洁（枚举类 vs 数据库表）
- **可改进点**：
  1. iDempiere 的 **AD_Message 表驱动** 允许运行时修改错误消息而不用改代码——如果我们未来需要非技术人员编辑错误消息，可考虑迁移到表驱动
  2. ERPNext 的 **异常类继承体系** 比纯 ErrorCode 更结构化——但 NopException + ErrorCode 的组合已足够

---

## 六、可配置点清单（Phase 4-5）

### 我们的选择

16 项配置点统一文档化，按全局/按实体可配（Q41）。两套配置机制：实体表字段 + 独立配置表。

### 开源对比

| 项目 | 配置机制 | 实现方式 | 配置层级 |
|------|----------|----------|----------|
| **nop-app-erp** | 实体表字段 + ErpSysConfig 表 | 仓库表加字段 / 独立配置表按 orgId 回退 | 实体→组织→全局 |
| **Odoo** | **实体表字段** | `stock.location.removal_strategy_id` 直接在库位表上 | 按库位 + 物料类别覆盖 |
| **iDempiere** | **独立配置表** | `AD_SysConfig` 表，`Name`+`AD_Client_ID`+`AD_Org_ID` 三级回退 | Client→Org→System |
| **Metasfresh** | **独立配置表** | 同 iDempiere 的 `AD_SysConfig`，Java 侧 3 步内存回退链 | 同上 |
| **ERPNext** | **全局单例** | `Stock Settings` 只有一个全局配置 | 仅全局 |

### 关键差异：实体字段 vs 独立配置表

开源项目中**两种方式都存在**，取决于配置的性质：

| 配置性质 | 推荐方式 | 开源先例 |
|----------|----------|----------|
| 实体自身属性（仓库的批次策略、物料的计价方法） | **实体表加字段** | Odoo：`location.removal_strategy_id` |
| 跨实体全局配置（过账模式、信用额度级别、折旧方法） | **独立配置表** | iDempiere：`AD_SysConfig` |

**我们的设计已对齐**：仓库批次策略用实体字段（Odoo 模式），通用配置用 ErpSysConfig 表（iDempiere 模式）。

---

## 七、库存批次选择策略（Phase 3）

### 我们的选择

FIFO/FEFO/手工指定三种策略，按物料可配（Q38）。

### 开源对比

| 项目 | 批次选择 | 策略可配 |
|------|----------|----------|
| **nop-app-erp** | FIFO/FEFO/MANUAL | 按物料 |
| **Odoo** | FIFO/FEFO/LIFO/按手动 | 按公司/仓库 |
| **ERPNext** | FIFO/FEFO | 按仓库 |
| **iDempiere** | FIFO/FEFO/LIFO/Average | 按 AcctSchema |

### 分析

- **我们的设计与 ERPNext 一致**，覆盖了最常用的策略
- **可改进点**：
  1. Odoo 和 iDempiere 支持 **LIFO（后进先出）**——中国会计准则已禁止 LIFO，所以不支持是正确的
  2. Odoo 的 **按公司/仓库配置** 比我们的"按物料"更灵活——如果同一物料在不同仓库有不同策略（如保税仓 FIFO、普通仓 FEFO），可能需要增加仓库维度

---

## 八、委外加工（Phase 3）

### 我们的选择

委外订单独立于自制工单，发料到供应商库存/在途库（Q30）。

### 开源对比

| 项目 | 委外实现 | 发料方式 |
|------|----------|----------|
| **nop-app-erp** | 委外订单（独立单据） | 供应商库存/在途库 |
| **Odoo** | 外协 BOM + subcontracting | subcontractor 仓库 |
| **ERPNext** | 外协工单 | 供应商仓库 |
| **iDempiere** | 无原生支持 | — |

### 分析

- **我们的设计与 Odoo/ERPNext 一致**，委外是制造的标准场景
- **可改进点**：
  1. Odoo 的 **subcontractor 仓库** 概念比我们的"供应商库存"更明确——建议在 master-data 中增加 `warehouseType=SUBCONTRACTOR` 仓库类型
  2. Odoo 的 **外协 BOM** 将外协工序内嵌在 BOM 中（而非独立单据）——这比我们的独立委外订单更紧凑，但灵活性较低

---

## 九、NCR 财务影响（Phase 2）

### 我们的选择

NCR 关闭时按处置方式（退货/返工/报废）触发不同财务处理（Q25）。

### 开源对比

| 项目 | NCR 财务处理 |
|------|-------------|
| **nop-app-erp** | 退货红字凭证 / 返工成本归集 / 报废损失凭证 |
| **Odoo** | 无原生 NCR 财务处理 |
| **ERPNext** | 无原生 NCR 财务处理 |
| **Carbon QMS** | 检验/不合格品追踪，但无财务集成 |

### 分析

- **我们的设计比所有开源项目都更完整**——开源项目普遍没有将 NCR 与财务域打通
- **这是一个差异化优势**：大多数 ERP 的质量管理是"孤岛"，我们的 NCR→财务凭证链是更成熟的设计

---

## 十、测试策略（Phase 7）

### 我们的选择

三层测试（L1 单元/L2 集成/L3 端到端）+ Nop 测试框架（Q73）。

### 开源对比

| 项目 | 测试策略 | 测试框架 |
|------|----------|----------|
| **nop-app-erp** | 三层 + 快照录制回放 | JunitAutoTestCase |
| **Odoo** | Python unittest + tour | 自研测试框架 |
| **ERPNext** | pytest + Cypress | Frappe 测试框架 |
| **Metasfresh** | JUnit + 集成测试 | Spring 测试 |
| **iDempiere** | JUnit + 手工测试 | OSGi 测试 |

### 分析

- **我们的快照录制回放机制是独特优势**——开源项目没有类似的自动化测试基线管理
- **可改进点**：
  1. ERPNext 的 **Cypress 端到端测试** 覆盖了完整业务流程——建议在 L3 层增加 Playwright 测试
  2. Metasfresh 的 **Spring 集成测试** 模式值得借鉴——在 Nop 测试框架中增加 `@SpringBootTest` 等效支持

---

## 综合评价

### 设计正确性：✅ 高

Plan 02 的 81 个决议与开源最佳实践高度吻合：
- 凭证三件套 ← 赤龙（中式复式记账标准）
- 三轴状态分离 ← 赤龙（国产 ERP 标准）
- 异步过账 ← Metasfresh（工程最佳实践）
- 库存三层 ← Odoo（行业标杆）
- 多币种双字段 ← iDempiere（25 年验证）

### 差异化优势：3 项

| 优势 | 对比 |
|------|------|
| NCR→财务凭证链 | 所有开源项目均无 |
| NOT_OPENED 期间初始态 | 大多数开源项目只有 OPEN/CLOSED |
| 快照录制回放测试 | 无开源项目有类似机制 |

### 建议改进：5 项（已落实）

| 改进项 | 借鉴来源 | 术语解释 | 状态 |
|--------|----------|----------|------|
| 批次策略增加仓库维度 | Odoo | 在仓库表加 `batchSelectionStrategy` 字段，同 Odoo 的 `location.removal_strategy_id` | ✅ ORM 已增加 |
| 凭证引擎类型安全注册 | Metasfresh | Provider 注册从 `List` 遍历改为 `Map<BusinessType, Provider>` 查找（O(1)，编译期安全） | ✅ 已补充 `finance/posting.md` |
| 配置两级覆盖优先级 | iDempiere | 跨域配置用独立 `ErpSysConfig` 表，按 `orgId` 级联回退（同 iDempiere 的 `AD_SysConfig`） | ✅ ORM 已增加 |
| 质量-财务打通 | 无（创新项） | NCR 关闭时自动触发财务凭证——开源项目均无此集成，我们已有设计 | ✅ 已有 `quality/state-machine.md` |
| 状态转换 ORM 声明化 | Tryton | 在 ORM XML 中声明合法的状态转换对。**结论：保持现状**——Nop ORM 不原生支持此标签 | 保持现状 |

### 结论

**Plan 02 的文档改进在设计层面是成熟的**——核心决策（凭证三件套、三轴状态、异步过账、库存三层）都经过了开源项目的长期生产验证。我们的设计在 NCR 财务集成和测试策略上有差异化优势。

**已落实的改进**：批次策略增加仓库维度、凭证引擎类型安全注册、配置两级覆盖优先级——均已补充到对应设计文档。

**保持现状的项**：状态转换声明化——当前状态字典已定义所有合法状态值，转换规则在状态机文档中描述（`state-machine.md`），ORM XML 中暂不增加 `state-transition` 约束。理由：Nop 平台的 ORM 不原生支持 `state-transition` 标签，硬加会增加维护成本；状态转换校验在 BizModel 层通过代码实现即可。
