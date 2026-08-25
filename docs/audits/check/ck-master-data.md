# ck-master-data — master-data 实现代码检查报告

> 工作项：C1.1（pilot）。执行日期：2026-08-25。执行者：主 agent（ZCode 会话）。
> 范围：`module-master-data/erp-md-service` 全部手写生产代码（35 个生产文件，约 3,957 行；含 entity/ 22 个 BizModel、party/、dashboard/、report/、daterange/、exchange/、processor/ 2、statemachine/ 2、spi/ 2）。`erp-md-web` 手写层仅 auth xml + 测试（无业务逻辑）；`erp-md-api` 为骨架 bean/crud 声明。无 batch.xml/job → **D4=N/A**。
> 方法：Skill: `code-quality-audit-prompt`（发现骨架 + P0-P3 分级）+ `behavioral-failure-mode-scan-prompt`（B1/B2/B3.2/B4 grep 程式）。机械扫描 + 逐文件深读 + 平台 API 语义验证（`QueryBean.addOrderField(name, desc)` 经 nop-entropy 源码确认）。

## 发现清单

> 状态初始 `open`；修复阶段逐项测试验证后回填 `ai-check-index.md`。

### P2-CK-md-001（D3/D8）findEffectiveByPartner 无日期窗口过滤且无确定性排序

- **控制点**：`module-master-data/erp-md-service/src/main/java/app/erp/md/service/entity/ErpMdSupplierApprovalBizModel.java#findEffectiveByPartner`（约 L196-212）
- **证据**：查询仅 `eq("partnerId", partnerId)` 后 `findList(q, null, context)` 遍历返回第一条非 REJECTED 记录——无 `validFrom/validTo` 过滤、无 `orderBy`。
- **问题**：区间 MUTEX 只保证同 partner 有效记录**不重叠**，允许「历史区间 + 当前区间」共存（如过期 PROBATION 记录 + 当前 APPROVED 记录）。本方法可能返回过期记录：下游 `module-purchase/.../SupplierEligibilityChecker.java#check`（L45-53）据此判定 SUSPENDED→PREVENT——过期 SUSPENDED 记录会造成**误拒供应商**（或相反，当前 SUSPENDED 被过期 APPROVED 遮蔽而漏拒）。
- **建议修复方向**：加 `ErpDateRanges.contains(approval, CoreMetrics.today())` 过滤 + `validFrom` desc 排序取最新；或语义上仅返回 status ∈ {APPROVED, PROBATION, APPLIED} 的当前区间记录。

### P2-CK-md-002（D6）resolvePriceWithSource 缺供应商价格清单层（与 resolvePrice 语义不一致）

- **控制点**：`ErpMdMaterialSkuBizModel.java#resolvePriceWithSource`（约 L181-199）
- **证据**：`resolvePrice`（L153-177）为四层优先级（manualPrice > customerPriceResolver > supplierPriceResolver > SKU 默认档）；`resolvePriceWithSource` 只有 customerPriceResolver → 直接 `pickDefaultTierPrice`，**supplierPriceResolver 分支缺失**。
- **问题**：同 BizModel 两个价格解析端点行为不一致；经 `resolvePriceWithSource` 消费时供应商价格表价被误报为 `PRICING_SOURCE_SKU_DEFAULT`（含 0 兜底）。仓内无生产调用方（纯 GraphQL 端点），影响外部/前端消费者。owner doc 承诺见 `docs/design/master-data/use-cases.md` UC-MD-03 三级优先级。
- **建议修复方向**：补 supplierPriceResolver 层（返回 `PRICING_SOURCE_SUPPLIER_PRICE_LIST` 或实际来源常量），两方法共享一条解析链。

### P2-CK-md-003（D6）未定价 SKU 兜底价 0（nullSafe/nz 族）

- **控制点**：`ErpMdMaterialSkuBizModel#pickDefaultTierPrice`（L386-400，`nullSafe`→ZERO）；同型 `ErpMdReportBizModel#nz`（L280-282，报表 0 价展示）。
- **问题**：无任何价格档的 SKU 经 `resolvePrice`/`resolvePriceWithSource` 返回 `0` 而非 null/报错——0 元价可流入开单/展示路径，语义上「未定价」≠「免费」。与「无默认 SKU 时价格列留空」的 null 处理不一致。
- **建议修复方向**：无价返回 null（调用方/UI 决定必填或报错），或 strict 配置下抛错；报表保持 null 展示。

### P2-CK-md-004（D6/D2）汇率刷新区间 [today, today+1] 闭区间语义下覆盖 2 天 + 绕过互斥钩子

- **控制点**：`ErpMdCurrencyRefreshRatesFromApiProcessor.java#refreshRatesFromApi`（L65-66 `validFrom=today; validTo=today.plusDays(1)`；L89-91 `rateDao.saveEntity/updateEntity`）
- **证据**：`ErpDateRanges.contains` 为双侧**闭区间**（`date.isAfter(to)` 才排除）→ `[today, today+1]` 实际覆盖 2 天。连续每日刷新时昨日记录（validTo=今日）与今日记录（validFrom=今日）在今日**双有效**。且 Processor 直用 dao 写库，绕过 `ErpMdExchangeRateBizModel.defaultPrepareSave/Update` 的 MUTEX 互斥校验——API 刷新路径不受日期重叠校验约束。
- **问题**：汇率数据出现同日双有效记录，消费方（sales `ErpSalPriceListLineBizModel` 等）取值不确定。
- **建议修复方向**：`validTo=today`（单日闭区间）；如需走 BizModel 校验则经 IBiz 管道写。消费方取值语义在 C2.2（sales）交叉核对。

### P2-CK-md-005（D6）convertQty 不尝试反向换算（倒数系数）

- **控制点**：`ErpMdUoMConversionBizModel.java#resolveConversionRate`（L82-90）
- **证据**：仅查 `fromUoMId→toUoMId` 精确方向（物料级 + 通用层）；未尝试 `toUoMId→fromUoMId` 记录取倒数（`BigDecimal.ONE.divide(rate, scale, HALF_UP)`）。
- **问题**：换算表单向维护（如仅录 箱→个=12）时，个→箱 在 strict 模式（默认 true）直接抛 `ERR_UOM_CONVERSION_NOT_FOUND`。需在修复阶段对照 `docs/design/master-data/sku-multi-unit.md` 是否约定双向维护；若约定单向录入则此为功能缺陷（P2 成立），若约定双向录入则降级 not-a-problem。
- **建议修复方向**：反向查找 + 倒数换算（scale=4 HALF_UP 对齐现约定）。

### P3-CK-md-006（D8）ExchangeRateBizModel javadoc 声称 config-gate 但实现无 gate

- **控制点**：`ErpMdExchangeRateBizModel.java` 类 javadoc（L25-26「config-gated：erp-md.exchange-rate-overlap-check-enabled（默认 true），关闭时跳过校验」）vs `enforceNoOverlap`（L54-67 无任何 `AppConfig.var` 检查）。
- **问题**：文档承诺的运维开关不存在（行为偏严格方向，无正确性风险，但 doc-code 漂移违反文档可信性）。修复方向二选一：实现 gate 或删除 javadoc 承诺。

### P3-CK-md-007（D2/D10）DateRange idOf 反射失败静默返回 null

- **控制点**：`ErpDateRangeOverlapValidator.java#idOf`（L87-93，`catch (Exception e) { return null; }`）
- **问题**：反射取 id 失败被吞且无日志；update 自身记录时 self-exclusion 失效 → 对自身误报重叠（安全方向，阻断保存）。实体均有 `getId()`，实际触发概率低。修复方向：至少 log.warn 或窄化 catch（ReflectiveOperationException）。

### P3-CK-md-008（D2）currentUserId 宽 catch 返回 null 无日志

- **控制点**：`ErpMdSupplierApprovalBizModel#currentUserId`（L269-276）
- **问题**：`approve`/`reinstate` 的审计字段 `approvedBy` 可能为 null 且异常被吞无痕迹。`IUserContext.get()` 正常路径不抛，防御性 catch 但应窄化或记日志。

### P3-CK-md-009（D1）ErpPartyBizModel default 分支抛 IllegalArgumentException

- **控制点**：`ErpPartyBizModel#buildKeywordFilter`（L222）
- **问题**：违反「业务异常必须 extends NopException」平台规则；枚举穷尽后不可达，风险低。修复方向：改 `NopException` 或删 default。

### P3-CK-md-010（D10）Party 搜索 LIKE 未转义通配符

- **控制点**：`ErpPartyBizModel#buildKeywordFilter`（L206 `"%"+keyword+"%"`）
- **问题**：keyword 含 `%`/`_` 时作为通配符扩大匹配范围（非注入风险，行为意外）。修复方向：转义 `%`/`_`/`\`。

### P3-CK-md-011（D5）主数据删除守卫覆盖不均

- **控制点**：`ErpMdWarehouseBizModel`（裸 CrudBizModel，无任何钩子）；同型 Currency/UoM/TaxRate/Subject/SettlementMethod/Location 等。
- **问题**：Material/SKU/Partner 有应用层引用守卫（SPI 聚合 + defaultPrepareDelete），Warehouse 等被 inventory/purchase 引用（orm 列存在）却无应用层 in-use 检查，删除行为依赖 ORM/DB FK 约束（是否真约束待修复阶段核对 orm relation 定义）。修复方向：补 in-use 守卫或核实 FK 阻断后裁决 not-a-problem。

### P3-CK-md-012（D9）Dashboard 预警扫描 5000 行截断无可见性标记

- **控制点**：`ErpMdDashboardBizModel#findMaterialWithoutSkuAlert / findSkuWithoutPriceAlert`（ALERT_MAX_ROWS=5000 双侧截断）
- **问题**：超限时预警静默漏报（类注释声明「类 D 裁决保留」= 显式设计决策）。观察项：截断时在返回中附 `truncated: true` 标记更可观测。

### P3-CK-md-013（D6）报表价格 null→0 展示不一致

- **控制点**：`ErpMdReportBizModel#buildMaterialPriceListDataset`（L216-219 `nz()`）
- **问题**：「无默认 SKU 价格列留空（null）」但「有 SKU 无价格显示 0」——同一张报表两种空价语义。对齐 P2-CK-md-003 一并裁决。

### P3-CK-md-014（D1）requireSku 错误码语义漂移

- **控制点**：`ErpMdMaterialSkuBizModel#requireSku`（L332-343）
- **问题**：「skuId 为空」「SKU 未找到」复用 `ERR_SKU_DEFAULT_REQUIRED`（默认 SKU 必填）——错误码语义与场景不符，排障误导。修复方向：引入/复用 not-found 类错误码。

## 验证为正确（显式排除，防误报）

- `QueryBean.addOrderField(name, desc)` 第二参为 **desc**（nop-entropy `nop-kernel/nop-api-core/.../QueryBean.java:433` 实证）：`SupplierEligibilityChecker` "取最新周期"（periodTo, true=desc）与 md 报表 code 排序（false=asc）均正确。
- B2 dict 可达性：`ErpMdSupplierApprovalStateMachine` 5 个状态（APPLIED/APPROVED/PROBATION/SUSPENDED/REJECTED）全部有 writer（target 方法），迁移矩阵 10 边与 javadoc 一致；REJECTED 可恢复性有层 2 Decision 登记。
- B1：无过账类吞异常悬挂模式（md 域无过账）；`ErpMdReportBizModel#download` 的 catch 重抛 `NopException.adapt` 正确。
- 平台合规：R4（RuntimeException）/R5（@Inject private）/R7（System.currentTimeMillis）全域零命中；`@BizMutation/@BizQuery` 注解完整（35 个公开动作均带注解，含 state machine 动作）。
- `ErpDateRanges` sweep line 闭区间语义实现正确；`ErpMdAcctSchemaBizModel.findFirstByOrg` 内存确定性择优正确。
- SKU 条码 TOCTOU 已有文档化 Deferred（DB 唯一索引 G1），不重复登记。

## arm-index 复用 or 新增裁决

- 关键符号 `resolvePriceWithSource`/`findEffectiveByPartner`/`convertQty`/汇率区间写入 在 `docs/audits/arm-index.md` 零命中 → **全部新增**。
- P1-RC-018（采购价格差异过账）为不同控制点（ThreeWayMatcher/AP 发票过账），不构成复用。
- dashboard vendorCount 字典漂移（2026-07-09-1145-1）已修复（`PARTNER_TYPE_VENDOR="SUPPLIER"` 与 dict 对齐，验证通过）。

## 统计

| 级别 | 数量 | 编号 |
| --- | --- | --- |
| P0 | 0 | — |
| P1 | 0 | — |
| P2 | 5 | P2-CK-md-001/002/003/004/005 |
| P3 | 9 | P3-CK-md-006..014 |

按维度：D1×2、D2×3、D3×1、D5×1、D6×5、D8×1、D9×1、D10×2（P2-CK-md-001 跨 D3/D8 计主 D3，P2-CK-md-004 跨 D6/D2 计主 D6）。

## 剩余风险（查了什么/没查什么）

- **已查**：erp-md-service 全部 35 个生产文件（22 个 entity BizModel 中的 18 个逐行深读，4 个同构裸 CRUD 抽样）+ 2 Processor + 2 状态机 + daterange/exchange/party/dashboard/report 全读 + 机械 grep 全量。
- **未深查**：`erp-md-web` AMIS view.xml 与后端契约 drift（低频抽查未执行，属 C8.2 范畴）；`erp-md-api` 骨架正确性（生成物范畴）；P2-CK-md-004 的消费方取值语义（归 C2.2 sales 交叉核对，已在 finding 中登记）。
