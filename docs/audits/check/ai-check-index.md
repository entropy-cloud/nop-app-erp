# AI 实现代码检查索引（ai-check）

> ai-check mission 统一入口。路线图：`docs/backlog/ai-check-roadmap.md`（两阶段：检查只读 → 全部完成后逐项测试验证修复，P0-P3 全修）。
> 启动：2026-08-25。纪律：报告产出即更新本索引；修复完成即回填状态；V.2 校验终态。

## Mission 基线快照（C0.2，2026-08-25）

- 手写主代码 3,364 / 测试 753 Java 文件（口径见 roadmap §当前基线）。
- 绿色回归基线：`docs/testing/known-good-baselines.md` 2026-08-25 V.1 行（全 reactor 3,834 tests + 156 模块 install）。
- compliance checker 快照（修复后不得高于此，合法新增须 baseline-raise 登记）：

| 规则 | 描述 | 命中 |
| --- | --- | --- |
| R1d | dao().findAllByQuery (BizModel) | 14 |
| R2a | BizModel daoFor(ErpMd*) | 34 |
| R2b | BizModel daoFor(Erp*) 跨域 | 237 |
| R2c | 全生产代码 daoFor() 总量 | 1505 |
| R2d | Processor daoFor(ErpMd*) | 38 |
| R3 | new Erp*() 构造实体 | 5 |
| R4 | extends RuntimeException | 0 |
| R5 | @Inject private | 0 |
| R6 | @Transactional in BizModel | 2 |
| R7 | System.currentTimeMillis() | 0 |
| R8 | Processor 无 xbiz 接线 | 0 |
| R10 | REQUIRES_NEW 事务 | 12 |
| R11 | Processor 重复状态判断方法 | 0 |
| R12a/b/c | 共享内核 import ErpFinBusinessType/PostingEvent/AcctSchemaResolver | 70/66/41 |

（其余规则 R1a/R1b/R1c 命中 0。基线内条目 = 已裁决偏离；检查阶段只报告**新增违规**与**基线条目中的真实缺陷**。）

## 报告清单

| 报告 | 工作项 | 域 | P0 | P1 | P2 | P3 | 状态 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `ck-master-data.md` | C1.1 | master-data | 0 | 0 | 5 | 9 | done |
| `ck-purchase.md` | C2.1 | purchase | 0 | 3 | 6 | 5 | done |
| `ck-sales.md` | C2.2 | sales | 0 | 4 | 16 | 10 | done |
| `ck-inventory.md` | C2.3 | inventory | 0 | 5 | 11 | 5 | done |
| `ck-finance-posting.md` | C3.1 | finance（过账与凭证） | 0 | 5 | 10 | 4 | done |
| `ck-finance-arap.md` | C3.2 | finance（AR/AP 核销） | 0 | 4 | 5 | 8 | done |
| `ck-finance-budget-costing.md` | C3.3 | finance（预算与成本） | 0 | 5 | 5 | 6 | done |
| `ck-finance-period-misc.md` | C3.4 | finance（期间结账与其他） | 0 | 3 | 8 | 10 | done |
| `ck-mfg-workorder.md` | C4.1 | manufacturing（工单与报工） | 1 | 4 | 6 | 10 | done |
| `ck-mfg-bom-mrp.md` | C4.2 | manufacturing（BOM/MRP/CRP） | 0 | 3 | 9 | 11 | done |
| `ck-mfg-subcontract.md` | C4.3 | manufacturing（委外/追溯/差异） | 0 | 5 | 7 | 6 | done |
| `ck-assets-lifecycle.md` | C4.4 | assets（生命周期） | 0 | 6 | 10 | 11 | done |
| `ck-assets-depreciation.md` | C4.5 | assets（折旧与过账） | 0 | 6 | 8 | 9 | done |
| `ck-projects.md` | C5.1 | projects | 0 | 7 | 7 | 7 | done |
| `ck-quality.md` | C5.2 | quality | 0 | 5 | 9 | 11 | done |
| `ck-maintenance.md` | C5.3 | maintenance | 0 | 3 | 6 | 10 | done |
| `ck-hr-org.md` | C6.1 | hr（组织与员工） | 0 | 1 | 4 | 12 | done |
| `ck-hr-attendance-payroll.md` | C6.2 | hr（考勤与薪酬） | 0 | 5 | 9 | 12 | done |
| `ck-crm-lead.md` | C6.3 | crm（线索与商机） | 0 | 2 | 6 | 11 | done |
| `ck-crm-cpq-forecast.md` | C6.4 | crm（CPQ 与预测） | 0 | 2 | 10 | 8 | done |
| `ck-cs.md` | C7.1 | cs | 0 | 3 | 11 | 8 | done |
| `ck-contract.md` | C7.2 | contract | 0 | 4 | 13 | 7 | done |
| `ck-b2b.md` | C7.3 | b2b | 0 | 1 | 8 | 7 | done |
| `ck-drp.md` | C7.4 | drp | 0 | 3 | 11 | 6 | done |
| `ck-logistics.md` | C8.1 | logistics | 0 | 2 | 7 | 2 | done |
| `ck-aps.md` | C8.1 | aps | 0 | 2 | 6 | 3 | done |
| `ck-notify.md` | C8.1 | notify | 0 | 2 | 4 | 2 | done |
| `ck-common-app.md` | C8.2 | common-service/test + app-erp-all | 0 | 0 | 1 | 9 | done |

## Finding 追踪

> ID 格式：`P{0-3}-CK-{域短码}-{NNN}`。状态机：`open`（检查阶段登记）→ `verifying`（修复阶段测试验证中）→ `fixed`（已修复，附测试证据与提交指针）/ `not-a-problem`（验证后非问题，附书面说明）/ `deferred`（显式例外，须登记触发条件并向用户报告）。

| Finding ID | 级别 | 维度 | 报告 | 摘要 | arm-index 复用裁决 | 状态 | 终态证据 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| P2-CK-md-001 | P2 | D3/D8 | ck-master-data | findEffectiveByPartner 无日期窗过滤+无排序，过期资格致 purchase 误拒/漏拒 | 新增（arm-index 零命中） | open |  |
| P2-CK-md-002 | P2 | D6 | ck-master-data | resolvePriceWithSource 缺供应商价格层，与 resolvePrice 语义不一致 | 新增 | open |  |
| P2-CK-md-003 | P2 | D6 | ck-master-data | 未定价 SKU 兜底价 0（nullSafe/nz 族） | 新增 | open |  |
| P2-CK-md-004 | P2 | D6/D2 | ck-master-data | 汇率刷新 [today,today+1] 闭区间覆盖 2 天 + dao 直写绕过 MUTEX 钩子 | 新增 | open |  |
| P2-CK-md-005 | P2 | D6 | ck-master-data | convertQty 不尝试反向换算（倒数系数） | 新增 | open |  |
| P3-CK-md-006 | P3 | D8 | ck-master-data | ExchangeRate javadoc 声称 config-gate 实现无 gate | 新增 | open |  |
| P3-CK-md-007 | P3 | D2/D10 | ck-master-data | DateRange idOf 反射失败静默 null | 新增 | open |  |
| P3-CK-md-008 | P3 | D2 | ck-master-data | currentUserId 宽 catch 返回 null 无日志 | 新增 | open |  |
| P3-CK-md-009 | P3 | D1 | ck-master-data | Party buildKeywordFilter default 抛 IllegalArgumentException | 新增 | open |  |
| P3-CK-md-010 | P3 | D10 | ck-master-data | Party LIKE 未转义通配符 | 新增 | open |  |
| P3-CK-md-011 | P3 | D5 | ck-master-data | Warehouse 等主数据删除无应用层 in-use 守卫 | 新增 | open |  |
| P3-CK-md-012 | P3 | D9 | ck-master-data | Dashboard 预警 5000 行截断无标记（显式裁决保留） | 新增 | open |  |
| P3-CK-md-013 | P3 | D6 | ck-master-data | 报表价格 null→0 展示不一致 | 新增 | open |  |
| P3-CK-md-014 | P3 | D1 | ck-master-data | requireSku 错误码语义漂移 | 新增 | open |  |
| P1-CK-pur-001 | P1 | D3 | ck-purchase.md | 采购看板全部主查询消费 docStatus=ACTIVE 死状态（全域零 writer），KPI/趋势/TOP N/及时率/三单预警恒空 | 新增 | open |  |
| P1-CK-pur-002 | P1 | D7/D2 | ck-purchase.md | Invoice/Payment approve 链 SoD 守卫位于 doPosting（REQUIRES_NEW 已提交）之后——守卫抛错回滚主事务但凭证已独立提交成孤儿 | 新增 |fixed | F1.2：Invoice/Payment SoD 前移 SoD-first + Invoice 承付 hook 前移（SYNC 同事务）；TestErpPurInvoice/PaymentApproval SoD 零凭证断言；pur 336 全绿 |
| P1-CK-pur-003 | P1 | D5/D3 | ck-purchase.md | 通用 CRUD update 路径无「已审核/已过账不可修改」守卫（头+行全实体）——三单匹配/核销/聚合的数据基线可被直接改写 | 新增 |fixed | F1.3：AbstractErpCrudBizModel 状态锁基类（posted/APPROVED 拒通用 update/delete）+ 363 文件全域接入；TestErpPurCrudStatusLock 五分支绿；19 域 7249/7249 全绿 |
| P2-CK-pur-004 | P2 | D6/D8 | ck-purchase.md | 「CANCELLED 但仍 APPROVED 计入聚合」三处联发 + cancel/reverseApprove 后订单收货进度不重算 | 复用注记：RC-R1.11 口径 | open |  |
| P2-CK-pur-005 | P2 | D8 | ck-purchase.md | Payment cancel/reverseApprove 不守卫也不回滚核销；Invoice cancel 不守卫已核销状态 | 新增 | open |  |
| P2-CK-pur-006 | P2 | D7 | ck-purchase.md | 三处聚合校验无共享行锁的并发竞态：超收容差 / 退货可退量 / 请购转订单幂等 | 新增 | open |  |
| P2-CK-pur-007 | P2 | D6 | ck-purchase.md | Dashboard 三单价格容差与 ThreeWayMatcher 同配置键不同量纲（ratio vs percent） | 新增 | open |  |
| P2-CK-pur-008 | P2 | D5 | ck-purchase.md | 退货行 receiveLineId 可空 → 无回链行完全绕过数量上限校验 | 新增 | open |  |
| P2-CK-pur-009 | P2 | D6/D2 | ck-purchase.md | 请购转订单：非法税率串静默按零税处理（与单价的抛错不对称） | 新增 | open |  |
| P3-CK-pur-010 | P3 | D2/D10 | ck-purchase.md | currentUserId 宽 catch 返回 null 无日志（5 处 Processor 同型） | 新增 | open |  |
| P3-CK-pur-011 | P3 | D9 | ck-purchase.md | Dashboard 无界加载 + N+1 | 新增 | open |  |
| P3-CK-pur-012 | P3 | D4 | ck-purchase.md | 声明未接线的配置/常量（doc-code 漂移） | 新增 | open |  |
| P3-CK-pur-013 | P3 | D7 | ck-purchase.md | batchApprove 逐行吞 NopException 但共享单事务——失败行此前的会话脏写随外层提交 | 新增 | open |  |
| P3-CK-pur-014 | P3 | D10 | ck-purchase.md | ThreeWayMatcher 悬挂回链静默跳过校验 | 新增 | open |  |
| P1-CK-sal-001 | P1 | D6/D8 | ck-sales.md | ReturnRefundOrchestrator 客户级全量反转核销——不限于退货关联发票，无关联发票时也触发 | 新增 | open |  |
| P1-CK-sal-002 | P1 | D3/D6 | ck-sales.md | Dashboard 订单量 KPI 查询死状态 ACTIVE——orderCount/conversionRate 恒 0 | 新增 | open |  |
| P1-CK-sal-003 | P1 | D8，跨域核对归属 C3.1 | ck-sales.md | 延迟过账重试成功后 sales 源单 posted 标志不回写——红冲门控被跳过、暂估判定失真 | 跨域核对归属 C3.1 |fixed | finance 侧控制点 P1-CK-fin-001 已于 F2.1 修复（VoucherPostedEvent 悬挂补写通道 + SalReversalListener dual 接口回写 AR_INVOICE/RECEIPT/SALES_RETURN/SALES_OUTPUT）；sales 侧遗留面（红冲门控/暂估判定的域内复核）归 F2.10 联动收口 |
| P1-CK-sal-004 | P1 | D5 | ck-sales.md | 通用 CRUD 更新/删除无单据状态守卫——「posted=true 后物理锁定」设计承诺未实现 | 新增 |fixed | F1.3：AbstractErpCrudBizModel 状态锁基类（posted/APPROVED 拒通用 update/delete）+ 363 文件全域接入；TestErpPurCrudStatusLock 五分支绿；19 域 7249/7249 全绿 |
| P2-CK-sal-005 | P2 | D6 | ck-sales.md | 促销规则 materialCategoryId 目标维度未参与行匹配——类目规则全局命中所有行 | 新增 | open |  |
| P2-CK-sal-006 | P2 | D6 | ck-sales.md | GIFT 规则触发物料不在订单时仍生成赠品行 | 新增 | open |  |
| P2-CK-sal-007 | P2 | D6 | ck-sales.md | 非栈式规则「break 全链」终止——与自身 javadoc「跳过同类型后续规则」语义相悖 | 新增 | open |  |
| P2-CK-sal-008 | P2 | D6/D9 | ck-sales.md | 价格清单行「最优命中」无确定性排序 + 取价链全量内存过滤 | 新增 | open |  |
| P2-CK-sal-009 | P2 | D6 | ck-sales.md | 可用量预校验/退货 current 成本对批次粒度余额表 setLimit(1) 取单行——未按声明「聚合」 | 新增 | open |  |
| P2-CK-sal-010 | P2 | D5/D3 | ck-sales.md | 收款核销不拒绝已作废单据——settle/reverseSettlement 仅校验 approveStatus | 新增 | open |  |
| P2-CK-sal-011 | P2 | D8/D3 | ck-sales.md | 已审核出库单作废后订单发货进度不回滚——聚合口径未排除 CANCELLED 出库单 | 新增 | open |  |
| P2-CK-sal-012 | P2 | D8/D3 | ck-sales.md | 收款单反审核/作废不反向核销——发票收款状态残留 RECEIVED | 新增 | open |  |
| P2-CK-sal-013 | P2 | D5 | ck-sales.md | withdrawApproval 无「仅提交人可操作」校验 | 新增 | open |  |
| P2-CK-sal-014 | P2 | D5 | ck-sales.md | Contract INLINE approve 缺 SoD 守卫——合同创建人可自审 | 新增 | open |  |
| P2-CK-sal-015 | P2 | D4 | ck-sales.md | 报价过期日扫 job 未实现——设计声明的 `erp-sal.quotation-expiry-check-cron` 零落地 | 新增 | open |  |
| P2-CK-sal-016 | P2 | D3，疑似需求分歧只登记不裁决 | ck-sales.md | 一次报价多次转订单被单活跃订单阻断 | 疑似需求分歧只登记 | open |  |
| P2-CK-sal-017 | P2 | D6 | ck-sales.md | 换货出库单行税额公式与全域价税分离口径不一致（价外税 vs 价内税） | 新增 | open |  |
| P2-CK-sal-018 | P2 | D6/D8，跨域核对归属 C3.1 | ck-sales.md | OFFSET_ESTIMATED_RECEIVABLE 标记硬编码 TRUE——「已开票/未开票」双路径区分失效 | 跨域核对归属 C3.1 | open |  |
| P2-CK-sal-019 | P2 | D2/D7，跨域核对归属 C3.1 | ck-sales.md | PENDING 过账异常与源单 cancel/reverseApprove 竞态——sweep 可能为已作废单生成凭证 | 跨域核对归属 C3.1 | open |  |
| P2-CK-sal-020 | P2 | D5/D6 | ck-sales.md | 应收超期预警双阈值 AND 耦合——只配置单一阈值时永不触发 | 新增 | open |  |
| P3-CK-sal-021 | P3 | D8 | ck-sales.md | 报价→订单转化丢失行级折扣字段 | 新增 | open |  |
| P3-CK-sal-022 | P3 | D8 | ck-sales.md | deliveredQuantity 仅退货审核路径写入——出库审核路径仍零 writer（P2-RC-019 部分残留） | P2-RC-019 部分残留 | open |  |
| P3-CK-sal-023 | P3 | D4/D8 | ck-sales.md | returns.md/quotation.md 声明的配置项与特性未落地清单；sales/contract.md 实体名漂移 | 新增 | open |  |
| P3-CK-sal-024 | P3 | D2 | ck-sales.md | currentUserId 宽 catch 返回 null（6 处处理器同型）+ readBoolConfig 宽 catch 吞异常 | 新增 | open |  |
| P3-CK-sal-025 | P3 | D2 | ck-sales.md | 承付 commit hook 无容错——与 release hook 容错语义不对称 | 新增 | open |  |
| P3-CK-sal-026 | P3 | D9 | ck-sales.md | 聚合计算加载全实体内存求和 / N+1 | 新增 | open |  |
| P3-CK-sal-027 | P3 | D1 | ck-sales.md | ErpSalConfigs 空接口死代码 | 新增 | open |  |
| P3-CK-sal-028 | P3 | D5 | ck-sales.md | settle 静默跳过非正数分配项 | 新增 | open |  |
| P3-CK-sal-029 | P3 | D6 | ck-sales.md | 合同量折扣回退基数取当前行价——重复应用二次折扣风险 | 新增 | open |  |
| P3-CK-sal-030 | P3 | D8 | ck-sales.md | 库存移动行未透传库位/批号/序列号——DTO 支持但构造器留空 | 新增 | open |  |
| P1-CK-inv-001 | P1 | D6/D8 | ck-inventory.md | 4 个出库策略 locationId 回退误用 `move.getSourceWarehouseId()`——仓库 ID 写入库位列，余额维度污染 | 新增 | open |  |
| P1-CK-inv-002 | P1 | D6/D8 | ck-inventory.md | `upsertBalance/findBalance` 查询键与 UK 自然键不一致——skuId 永不过滤、null 维度不做 IS NULL 匹配，余额行错配写入 | 新增 | open |  |
| P1-CK-inv-003 | P1 | D3/D5 | ck-inventory.md | 「库存流水不可变 / 余额由流水驱动」两条核心规则在 CRUD 层零强制——Ledger/Balance 可经通用 mutation 直接改删 | 新增 |fixed | F1.3：AbstractErpImmutableCrudBizModel——ledger/balance 拒全部通用 update/delete；TestErpInvLedgerImmutable 绿 |
| P1-CK-inv-004 | P1 | D3/D5 | ck-inventory.md | owner doc §4「批次/序列号缺失拒绝确认」未实现——批次管控物料无批号出库被跳过放行；序列号「未售校验」全缺 | 新增 | open |  |
| P1-CK-inv-005 | P1 | D5/D3 | ck-inventory.md | 通用 CRUD update 无状态守卫——同型 P1-CK-pur-003/P1-CK-sal-004（不复用展开） | 同型 P1-CK-pur-003/P1-CK-sal-004 |fixed | F1.3：AbstractErpCrudBizModel 状态锁基类（posted/APPROVED 拒通用 update/delete）+ 363 文件全域接入；TestErpPurCrudStatusLock 五分支绿；19 域 7249/7249 全绿 |
| P2-CK-inv-006 | P2 | D6/D10 | ck-inventory.md | OwnershipTransfer `reclassifyBalance` 除法无 scale——非整除即 ArithmeticException，VMI DONE 崩溃 | 新增 | open |  |
| P2-CK-inv-007 | P2 | D6/D8 | ck-inventory.md | VMI 所有权转移对层式计价物料零成本转移——`source.getAvgCost()` 对 FIFO/LIFO/BATCH/SPECIFIC 恒为 null | 新增 | open |  |
| P2-CK-inv-008 | P2 | D6 | ck-inventory.md | SpecificCostingStrategy 声称按 serialNo 匹配但查询从未使用 serialNo——serial-only 出库消耗任意成本层 | 新增 | open |  |
| P2-CK-inv-009 | P2 | D6 | ck-inventory.md | 期末 reclose 全月一次加权平均公式与自身 javadoc 不符且顺序依赖——多出库流水期间重算结果错误 | 新增 | open |  |
| P2-CK-inv-010 | P2 | D6/D8 | ck-inventory.md | 多币种混算无汇率折算——余额/流水按首行币种混加，PostingEvent 汇率恒 1 | 新增 | open |  |
| P2-CK-inv-011 | P2 | D6 | ck-inventory.md | Dashboard 周转率分子读 `line.getTotalCost()`——策略族只刷 unitCost 不刷 totalCost，出库成本≈请求价×量（常为 0） | 新增 | open |  |
| P2-CK-inv-012 | P2 | D7/D2 | ck-inventory.md | REQUIRES_NEW 凭证先于主事务提交——doComplete/approve 尾部失败留下孤儿凭证 | 新增 |fixed | 证伪+收敛：3a StockMove doComplete 子站证伪（dispatchIfApplicable 已是末步）；3b/3c landedCost/costAdjust billHeadCode 稳定经 F1.1 收敛（inv 无代码变更，引擎层传导） |
| P2-CK-inv-013 | P2 | D7 | ck-inventory.md | 预留量写与成本层消耗不走乐观锁重试——违背 owner doc §4「扣减失败重试」与 UC-INV-08 失败语义 | 新增 | open |  |
| P2-CK-inv-014 | P2 | D6/D8 | ck-inventory.md | 到岸成本 GL 金额与库存成本层调整额口径分叉——`applyLine` 用重估公式覆盖了分摊金额 | 新增 | open |  |
| P2-CK-inv-015 | P2 | D8/D9 | ck-inventory.md | StandardCostResolver 全表加载 FIRMED 卷算 + 逐 header N+1 + 无 orgId 过滤 | 新增 | open |  |
| P2-CK-inv-016 | P2 | D9 | ck-inventory.md | reclosePeriodCosts 期末全量 N+1 扫描 | 新增 | open |  |
| P3-CK-inv-017 | P3 | D3 | ck-inventory.md | dict 死状态群：batch-status 4 值 / serial-status 3 值 / reservation EXPIRED 零 writer（部分被引用检查消费） | 新增 | open |  |
| P3-CK-inv-018 | P3 | D2/D10 | ck-inventory.md | currentUserId 宽 catch 返回 null 无日志（2 处，跨域同型） | 新增 | open |  |
| P3-CK-inv-019 | P3 | D6 | ck-inventory.md | reclose recomputeOutgoingCogs 出库流水 totalCost 写正值（符号约定破坏） | 新增 | open |  |
| P3-CK-inv-020 | P3 | D9 | ck-inventory.md | Dashboard/追溯链无界加载与 N+1 | 新增 | open |  |
| P3-CK-inv-021 | P3 | D4 | ck-inventory.md | 死配置键：`erp-inv.concurrent-deduct-retry-backoff-ms` 声明零消费 | 新增 | open |  |
| P1-CK-fin-001 | P1 | D8/D2，承接 P1-CK-sal-003 | ck-finance-posting.md | Sweep 重试成功仅标记 RETRIED——源单 posted 回写通道在 finance 侧完全缺失（无正向 posted 事件） | 承接 P1-CK-sal-003（证实） |fixed | F2.1：VoucherPostedEvent+Registry 镜像对偶+两重试通道派发+FinPostedListener 一期；悬挂闭环集成测试绿（retry→RETRIED+posted=true） |
| P1-CK-fin-002 | P1 | D6 | ck-finance-posting.md | postVoucher（DRAFT→POSTED）无借贷平衡校验——不平衡的手工凭证可过账进 GL | 新增 |fixed | F2.1：postVoucher 过账边平衡断言+头合计重算；不平衡拒/平衡过/红字过三用例绿 |
| P1-CK-fin-003 | P1 | D8 | ck-finance-posting.md | post() 幂等命中返回 null 与全域 dispatcher「null=失败」语义冲突——O-16 场景重审后 posted 永久悬挂 | 新增 |fixed | F1.1 Phase 1：process() 幂等命中改返既有凭证 id（findPostedVoucher 账套过滤重载）；TestErpFinPostingService 幂等断言强化 + fin 全量绿 |
| P1-CK-fin-004 | P1 | D8/D6 | ck-finance-posting.md | SchemaPropagator 查询账套缺 ACTIVE 状态过滤——停用账套被传播过账、主账套可选中停用账套 | 新增 |fixed | F2.1：resolver+propagator ACTIVE 过滤+常量提取+statusScore 化简；混合 ACTIVE/INACTIVE 只返 ACTIVE、仅 INACTIVE 返 null 用例绿 |
| P1-CK-fin-005 | P1 | D10/D2 | ck-finance-posting.md | acctSchemaId 解析为 null 时 post() 静默「成功」零凭证——无异常记录、无告警、指标误报 success | 新增 |fixed | F1.4：引擎 try 块首语句 fail-closed 守卫（ERR_NO_ACTIVE_SCHEMA 经 recordPostFailure→PENDING→sweep 链）；testNullSchemaFailsClosed 绿 + fin 498/498 |
| P2-CK-fin-006 | P2 | D2/D7，承接 P2-CK-sal-019 | ck-finance-posting.md | Sweep 重试无源单有效性校验通道——PENDING 异常可为由 post() 重建的已作废单生成凭证 | 承接 P2-CK-sal-019（证实） | open |  |
| P2-CK-fin-007 | P2 | D1 | ck-finance-posting.md | translateFactsForSchema 内死变量 + 跨域实体 daoFor 直查越权（同方法两种范式并存） | 新增 | open |  |
| P2-CK-fin-008 | P2 | D10 | ck-finance-posting.md | process() catch 块 no-op 自赋值——多账套循环失败后 event.acctSchemaId 停留在最后迭代值 | 新增 | open |  |
| P2-CK-fin-009 | P2 | D6 | ck-finance-posting.md | 红冲草稿丢失 amountSource 按行数据——多币种红冲凭证源币金额失真、汇率仅取首行 | 新增 | open |  |
| P2-CK-fin-010 | P2 | D6/D8 | ck-finance-posting.md | resolveAcctSchemaIdFromContext 恒返回 null——GL 映射的账套精确规则永不命中 | 新增 | open |  |
| P2-CK-fin-011 | P2 | D8/D6 | ck-finance-posting.md | 多套账传播下辅助账幂等去重缺账套维度——仅首个账套生成 ErpFinArApItem | 新增 | open |  |
| P2-CK-fin-012 | P2 | D9 | ck-finance-posting.md | 异常工作台计数全量加载 findAllByQuery().size()——注释宣称聚合 COUNT 与实现不符，5 分钟周期任务 + 期末门控反复全表载入 | 新增 | open |  |
| P2-CK-fin-013 | P2 | D6/D2 | ck-finance-posting.md | 手动重试 rebuildEvent 将缺失汇率回退为 1——绕过 RC-R1.42 外币汇率守卫，与 sweep 路径行为分裂 | 新增 | open |  |
| P2-CK-fin-014 | P2 | D2/D7 | ck-finance-posting.md | 手动 retry 失败既回滚计数又增生重复 PENDING 记录——同单多记录扩大 sweep 并发面 | 新增 | open |  |
| P2-CK-fin-015 | P2 | D8/D2 | ck-finance-posting.md | reverseVoucher（UI 红冲入口）对业务凭证只置 isReversed 标记——源单不回退、辅助账不取消、无 VoucherReversedEvent，业账失配 | 新增 | open |  |
| P3-CK-fin-016 | P3 | D10 | ck-finance-posting.md | findBillLinks 对 null businessType 直接 NPE——sweep 重建事件类型解析失败时以 NPE 而非业务错误码失败 | 新增 | open |  |
| P3-CK-fin-017 | P3 | D10 | ck-finance-posting.md | resolveOpenPeriod 期间命中集无排序——重叠期间配置下 get(0) 不确定 | 新增 | open |  |
| P3-CK-fin-018 | P3 | D6 | ck-finance-posting.md | CommitmentVoucherGenerator 红冲行 dcDirection 保留原方向但借贷互换——与引擎红冲范式字段语义分裂 | 新增 | open |  |
| P3-CK-fin-019 | P3 | D2/D9 | ck-finance-posting.md | PostingRun.captureTemplate 恒置 null——成功日志与失败记录的模板描述观测点死置 | 新增 | open |  |
| P1-CK-fin2-001 | P1 | D6 | ck-finance-arap.md | BY_RATIO 分摊分母不随收付款迭代刷新 + 尾差守卫方向写反——多笔收付款下生成超开分摊行，整批自动核销失败或比例语义失真 | 主 agent 已实证（尾差守卫方向反） |fixed | F2.2：分母每笔重算 + 尾差守卫 subtract 方向；两测试绿（600+500 vs 500+500 无超开 + 3×500 P3 unmatched） |
| P1-CK-fin2-002 | P1 | D6/D8 | ck-finance-arap.md | settleWithFx 双侧按各自汇率不对称结算，reverseSettle 却按行金额对称回滚——FX 核销红冲后付款项辅助账残留 fxGainLoss 偏差 | 新增 |fixed | F2.2：FX 路径 reverseSettle(lines, fxPath=true) 重演回滚（持久化证据 fxGainLoss≠0 分支）；非 FX 路径逐字节不变 |
| P1-CK-fin2-003 | P1 | D5/D6 | ck-finance-arap.md | post 校验不聚合同一辅助账项的多行累计——手工核销单多行共享同一 item 时可静默超核销（settled>amount、open 为负、状态 SETTLED） | 新增 |fixed | F2.2：post 前按 invoice/paymentItemId 分组聚合 validateAggregatedNotOver；两行共享 item 各 60 vs 100 拒绝测试绿 |
| P1-CK-fin2-004 | P1 | D3/D8 | ck-finance-arap.md | 坏账执行体在审批/反审核时点不校验辅助账当前状态——交错时序下 settled/open 可被写穿（负 settled、虚增 open、部分核销残留） | 新增 |fixed | F2.2：execute* 四现态守卫（writeOff/recovery/reverseOf 各自对称）+ executeRecovery settled 对称校验 + executeWriteOff 残额后置断言；悬空 recovery 拒绝测试绿 |
| P2-CK-fin2-005 | P2 | D3/D8，佐证 P2-CK-pur-005 / P2-CK-sal-012 | ck-finance-arap.md | 源单红冲的辅助账回滚通道（cancelOnReverse）不守卫已核销项、不级联已过账核销单——且核销单 reverse 会把 CANCELLED 项复活为 OPEN | 证实 P2-CK-pur-005/sal-012（finance 侧） |fixed | F2.2：cancelOnReverse settled>0 守卫 + applySettlement reverse 对 CANCELLED 拒绝 + reverseOffset 扩展先归零 payable 侧 settled（方案 a；TestErpFinExpenseOffsetAdvance 全绿零回归） |
| P2-CK-fin2-006 | P2 | D8 | ck-finance-arap.md | 坏账核销/收回/反审核与报销抵扣借款改写辅助账 open 后不刷新 ErpMdPartner 余额缓存——receivableBalance/payableBalance 陈旧直至该伙伴下次核销 | 新增 | open |  |
| P2-CK-fin2-007 | P2 | D8/D6，同族 P1-CK-fin-004/011 | ck-finance-arap.md | AR/AP 聚合读路径缺 acctSchemaId/orgId 隔离；多账套计提逐 schema 循环却用全局 Allowance 与全局应收基础 | 新增 | open |  |
| P2-CK-fin2-008 | P2 | D4/D7 | ck-finance-arap.md | 定时自动核销单事务全量原子 + 业务开关关闭时整批抛错——与声明的「记录级重试（单条失败不阻断）」失败接力模型不符 | 新增 | open |  |
| P2-CK-fin2-009 | P2 | D5 | ck-finance-arap.md | 坏账 approve 无 SoD 守卫——坏账单创建人可自审核销/收回（R3.3 SoD 铺开未覆盖 ErpFinBadDebt） | 新增 | open |  |
| P3-CK-fin2-010 | P3 | D4/D8 | ck-finance-arap.md | 定时核销 job 硬编码 'FIFO' 绕过 `erp-fin.auto-recon-strategy` 配置 + 设计文档 cron 键漂移（「deferred」过期） | 新增 | open |  |
| P3-CK-fin2-011 | P3 | D9 | ck-finance-arap.md | findPartnersWithOpenItems 全量载入实体 + O(n²) contains 收集 distinct partner；DualSideConsistencyChecker 逐发票 N+1 反查域级发票 | 新增 | open |  |
| P3-CK-fin2-012 | P3 | D2/D10 | ck-finance-arap.md | currentUserId 宽 catch 返回 null 无日志（跨域同型）+ approve 路径 loadArApItem 无 null 守卫 | 新增 | open |  |
| P3-CK-fin2-013 | P3 | D6/D10 | ck-finance-arap.md | AdvanceOffsetOrchestrator 将本位币值直写 source 侧字段——外币借款/报销项源币口径污染；引擎金额 scale 2 硬编码 + matchFifo 死守卫 | 新增 | open |  |
| P3-CK-fin2-014 | P3 | D5/D8 | ck-finance-arap.md | 核销冲销无原因记录（设计承诺「核销冲销：财务员 + 原因记录」）+ create 入参校验复用方向不匹配错误码 | 新增 | open |  |
| P3-CK-fin2-015 | P3 | D2 | ck-finance-arap.md | DualSideConsistencyChecker 把坏账核销的合法单侧变异报为 INCONSISTENT——告警噪音侵蚀检查器信任 | 新增 | open |  |
| P3-CK-fin2-016 | P3 | D4 | ck-finance-arap.md | 声明配置零消费：`erp-fin.bad-debt-exclude-disputed` 无 disputed 字段支撑，计提范围排除争议项的 doc 承诺不生效 | 新增 | open |  |
| P3-CK-fin2-017 | P3 | D10 | ck-finance-arap.md | resolvePeriodId 无 orgId 过滤无排序 setLimit(1)（同族 P3-CK-fin-017 新站点）+ BadDebtProvisionService 账套解析魔法默认 "1" | 新增 | open |  |
| P1-CK-fin3-001 | P1 | D6/D8 | ck-finance-budget-costing.md | carryForward 结转预算额度传递链三重断裂——结转行科目指向 Scenario id、结转凭证 Dr/Cr 双行同科目净额恒 0、期间挂源年度，结转额度永不参与目标方案预算 | 主 agent 已实证（subjectId=方案 id） |fixed | F2.3：结转行按源方案科目维度写入（subjectId=科目 id + periodId + 占比分摊）+ 净额 0 凭证段移除；TestErpFinBudgetCarryForward 5/5 绿 |
| P1-CK-fin3-002 | P1 | D6 | ck-finance-budget-costing.md | carryForward 余量公式漏减 commitment——`sourceRemaining = budget − actual` 两量口径，owner doc 与控制引擎均为三量 `budget − actual − commitment` | 新增 |fixed | F2.3：sourceRemaining 三量口径补 commitment 减项（aggregateCommitment COMMITMENT 通道聚合） |
| P1-CK-fin3-003 | P1 | D6 | ck-finance-budget-costing.md | getBudgetVsActual 实际数通道用 ΣamountFunctional 方向不敏感——期末结转凭证（postingType=NORMAL）贷方结平行计入后 actual 翻倍、availableAmount 大幅低估 | 新增 |fixed | F2.3：getBudgetVsActual actual 通道贷方行取负（方向敏感）——期末结转凭证不再使 actual 翻倍 |
| P1-CK-fin3-004 | P1 | D7 | ck-finance-budget-costing.md | 预算控制 check-then-act TOCTOU 无并发防护——HARD 控制下并发单据共享预算余量双双通过 | 新增 |fixed | F2.3：check() 外层 per-(subject|costCenter|period) ConcurrentHashMap 串行锁使 check-then-act 原子化 |
| P1-CK-fin3-005 | P1 | D8 | ck-finance-budget-costing.md | 承付凭证 orgId/acctSchemaId/currencyId 硬编码 + 预算控制聚合无账套维度——多账套下承付凭证全部落账套 1、预算控制跨账套混算 | 新增 |fixed | F2.3：resolveOrgAndSchema 经期间 org→AcctSchemaResolver 解析主账套 + resolveCurrencyId 经账套本位币（无账套回退占位） |
| P2-CK-fin3-006 | P2 | D3 | ck-finance-budget-costing.md | rollForward 目标方案 approveStatus 写入字典外值 "DRAFT"——wf/approve-status 值域仅 UNSUBMITTED/SUBMITTED/APPROVED/REJECTED | 新增 | open |  |
| P2-CK-fin3-007 | P2 | D9 | ck-finance-budget-costing.md | 预算聚合三路径全量实体加载 + 内存过滤——check 每 3 通道全量载入当期 POSTED 凭证、carryForward 逐行 N×M 载入、getBudgetVsActual 全量载入 | 新增 | open |  |
| P2-CK-fin3-008 | P2 | D5 跨域 | ck-finance-budget-costing.md | 预算 check 调用侧 costCenterId 恒传 null——成本中心维度预算行不可达，控制只对「无成本中心」预算行生效 | 新增 | open |  |
| P2-CK-fin3-009 | P2 | D10/D2 | ck-finance-budget-costing.md | 预算控制期间解析失败静默 fail-open——periodId 为 null 时 check 匹配 IS NULL 预算行必然落空直接 PASS，无告警无日志 | 新增 | open |  |
| P2-CK-fin3-010 | P2 | D6 | ck-finance-budget-costing.md | 预算/承付/滚动复制多币种字段失真——amountSource 被本位币金额顶替、汇率与币种恒定，源币口径不自洽 | 新增 | open |  |
| P3-CK-fin3-011 | P3 | D6 | ck-finance-budget-costing.md | BudgetVoucherGenerator.toFact 死三元——isReversal 参数在 BudgetLine 分支无效，未来按预算行红冲将不取负 | 新增 | open |  |
| P3-CK-fin3-012 | P3 | D10 | ck-finance-budget-costing.md | 预算链期间解析查询无排序且无 orgId 过滤——setLimit(1)/get(0) 在重叠期间与多 org 期间下归属不确定（与 P3-CK-fin-017 同型 3 新站点） | 新增 | open |  |
| P3-CK-fin3-013 | P3 | D2 | ck-finance-budget-costing.md | resolveUserId 宽 catch 吞咽无日志——与 P3-CK-pur-010 同型 finance 站点 | 新增 | open |  |
| P3-CK-fin3-014 | P3 | D1 文档 | ck-finance-budget-costing.md | CommitmentVoucherGenerator 类 javadoc 描述与实现矛盾——注释称「Cr 行使用同一科目反向（保持平衡）」，实现是单行单边无 Cr 行 | 新增 | open |  |
| P3-CK-fin3-015 | P3 | D5/D10 | ck-finance-budget-costing.md | rollForward 无幂等守卫 + validateNewFiscalYear 自动拆箱 NPE 风险 | 新增 | open |  |
| P3-CK-fin3-016 | P3 | D3 | ck-finance-budget-costing.md | findMatchingBudgetLine 多 APPROVED 方案同维度命中时无排序取首个——controlLevel 取值取决于 DB 返回序 | 新增 | open |  |
| P1-CK-fin4-001 | P1 | D6 | ck-finance-period-misc.md | 银行存款 FX 重估的「账面本位币」基准只聚合本期分录——跨期余额账户每月重复生成全额重估凭证，GL 银行科目金额虚增 | 新增 | fixed | F2.4：ExchangeRevaluationService.aggregateBankSubjectBookFunctional 账面基准改累计口径（移除 periodId 过滤）+ 跨期回归测试 TestErpFinAnnualClose#testBankFxRevaluationCrossPeriodCumulative（8月/9月连续结账 diff=50 非全额）；fin 530 tests 全绿 |
| P1-CK-fin4-002 | P1 | D6/D8 | ck-finance-period-misc.md | 多账套模式下损益结转/年度结转聚合无账套过滤——每个账套的结转凭证都含全域金额（N 倍重复入账），年初余额 populate 循环互删只余最后账套 | 新增 | fixed | F2.4：ProfitLossClosingService.closeForSchema + AnnualCloseService.subjectNetForYear/aggregateYearSubjectActivity/findYearPostedVoucherIds 增 acctSchemaId 过滤 + populateNextYearOpening clear 增账套维度；fin 530 tests 全绿 |
| P1-CK-fin4-003 | P1 | D6/D5 | ck-finance-period-misc.md | 跨法人调拨凭证金额 = 转移定价「单价」（无数量参与）且 materialId 传 null——凭证金额按单价入账（N 倍失真）+ 物料级定价规则永不命中 | 新增 | fixed | F2.4：IErpFinIntercompanyTransferBiz 新增带数量重载 onTransferConfirmed(..., qtyByMaterial, ...) + ErpFinIntercompanyTransferBizModel amount=unitPrice×Σ数量 + materialId 参与定价 + inventory ErpInvTransferOrderConfirmProcessor 传行数量聚合；TestErpFinIntercompanyTransfer#testOnTransferConfirmedQuantityAmount（100×150=15000）；fin+inv 全绿 |
| P2-CK-fin4-004 | P2 | D8/D6 | ck-finance-period-misc.md | FX 重估与年度结转聚合均无 orgId 维度——A 组织结账把 B 组织的外币项目/全年凭证重估结转进 A 的账 | 新增 | open |  |
| P2-CK-fin4-005 | P2 | D6 | ck-finance-period-misc.md | FX AR/AP 重估未排除 WRITTEN_OFF 状态——部分核销坏账后残留 openAmount 的已核销项仍被重估，生成幽灵汇兑损益 | 新增 | open |  |
| P2-CK-fin4-006 | P2 | D2/D8 | ck-finance-period-misc.md | 合并抵销候选识别非幂等——重复 generateEliminationCandidates 产生重复候选行，postElimination 后 GL 重复抵销 | 新增 | open |  |
| P2-CK-fin4-007 | P2 | D8/D3/D10 | ck-finance-period-misc.md | intercompany/抵销凭证直写引擎外路径：voucherDate=今天而非业务日期、无期间锁定守卫、科目解析失败静默降级 subjectId=null | 新增 | open |  |
| P2-CK-fin4-008 | P2 | D2 | ck-finance-period-misc.md | 跨法人调拨过账失败被 inventory 侧 catch(RuntimeException) 吞咽仅 warn——intercompany 凭证缺失无异常工作台记录、无告警、期末不拦截 | 新增 | open |  |
| P2-CK-fin4-009 | P2 | D5 | ck-finance-period-misc.md | 银行对账单导入幂等去重只对比「最近一张」对账单——跨月重导与历史单据去重失效，重复流水可入账 | 新增 | open |  |
| P2-CK-fin4-010 | P2 | D8 | ck-finance-period-misc.md | 次年期间生成/存在性检查/次月定位均无 orgId 维度——自动建次年期间恒挂 orgId="1"，多组织第二家年末结账抛错、反结账门控被他组织误触发 | 新增 | open |  |
| P2-CK-fin4-011 | P2 | D9 | ck-finance-period-misc.md | autoMatch 逐行全量重查——每条未勾对行触发一次窗口凭证全量加载 + 一次账户已勾对行全量加载（N+1） | 新增 | open |  |
| P3-CK-fin4-012 | P3 | D8 | ck-finance-period-misc.md | 反结账不清理试算平衡快照——reverseClose 后 ErpFinTrialBalance 残留旧快照，期间 OPEN 期间无失效标记 | 新增 | open |  |
| P3-CK-fin4-013 | P3 | D1 | ck-finance-period-misc.md | 生产代码使用 LocalDate.now()（非可控时钟）——银行对账自动红冲候选窗口 | 新增 | open |  |
| P3-CK-fin4-014 | P3 | D5 | ck-finance-period-misc.md | expenseType 必填校验误挂「审批开关」配置键下——关闭审批开关同时静默关闭行类型校验 | 新增 | open |  |
| P3-CK-fin4-015 | P3 | D3 | ck-finance-period-misc.md | BankStatement docStatus 永驻 DRAFT——设计声明的 DRAFT→POSTED→CANCELLED 状态机未接线（dict 死状态 + posted 三件套未用） | 新增 | open |  |
| P3-CK-fin4-016 | P3 | D6 | ck-finance-period-misc.md | REVENUE_COST/INVENTORY_PROFIT 抵销额复用 AR/AP 配对余额（存量当流量）——代码注释标「简化」但 owner doc 未登记偏离 | 新增 | open |  |
| P3-CK-fin4-017 | P3 | D2/D5 | ck-finance-period-misc.md | postElimination 候选不存在误抛 ERR_ELIMINATION_ALREADY_POSTED + DRAFT 抵销凭证会触发期末前置检查阻断结账（合并流程与结账互锁无提示） | 新增 | open |  |
| P3-CK-fin4-018 | P3 | D6 | ck-finance-period-misc.md | IntercompanyVoucherGenerator 红冲行 dcDirection 保留原方向但借贷互换——与承付红冲（P3-CK-fin-018）同族凭证范式分裂 | 新增 | open |  |
| P3-CK-fin4-019 | P3 | D10 | ck-finance-period-misc.md | 对账单 endingBalance 无 balanceAfter 输入时回退账户 currentBalance——银行侧期末余额被账面余额顶替，调节恒等式退化为 book vs book | 新增 | open |  |
| P3-CK-fin4-020 | P3 | D1/D5 | ck-finance-period-misc.md | BankReconAdjustmentVoucherBuilder 配置键与 billData 键字符串字面量绕过常量约定 | 新增 | open |  |
| P3-CK-fin4-021 | P3 | D7 | ck-finance-period-misc.md | closePeriod 与在途过账竞态——凭证过账事务先读 OPEN 后提交，可落进已 CLOSED 期间（无提交时复查） | 新增 | open |  |
| P0-CK-mfg-001 | P0 | D8 | ck-mfg-workorder.md | 完工入库移动单幂等键 (ERP_MFG_WORK_ORDER, wo.code) 使首次部分报工后的所有后续报工静默零入库——WO.completedQuantity 与库存/GL 数量静默分叉 | 主 agent 已实证全链（mfg:397-408 固定键 + inv GenerateMove:29-35 findExisting 静默返回） |fixed | F1.1 Phase 2：首单保持精确 code/后续 -C 累计量后缀 + 长度守卫；TestErpMfgWorkOrderEndToEnd 增量用例（3+2 两单守恒）+ mfg 293/293 绿 |
| P1-CK-mfg-002 | P1 | D3 | ck-mfg-workorder.md | reject / reverseApprove 后工单无法重新提交——审批轴与 docStatus 轴双守卫互锁，驳回成为准终态 | 新增 | fixed | F2.5：doReject/doReverseApprove 回写 docStatus=DRAFT（双轴联动，重提路径可达）；TestErpMfgReservationLifecycle#testRejectThenResubmit/testReverseApproveThenResubmit 新增；mfg 302 全绿 |
| P1-CK-mfg-003 | P1 | D8 | ck-mfg-workorder.md | 领料红冲回退闭环断裂——reverseConfirm 只红冲 GL + 反向库存，不回退 WorkOrder.materialCost / WorkOrderLine.actualQuantity / 预留 consumedQuantity | 新增 | fixed | F2.5：reverseConfirm 镜像回退三件套——WorkOrder.materialCost（REVERSAL 流水 \|totalCost\| 冲减）/ WorkOrderLine.actualQuantity（行量回退）/ 预留 consumedQuantity（IErpInvReservationBiz 新增 unconsumeReservation 逆操作 + 余额预留量回补，min 封顶于已消耗）；快照重录 MATERIAL_COST 10→0/TOTAL_COST 10→0/VERSION 1→2 + 行 ACTUAL_QUANTITY 2→0；TestErpMfgReservationLifecycle#testReverseConfirmRestoresReservationConsumed 新增（consumed 3→0/余额预留 1→4/status→OPEN）；mfg 302 全绿 |
| P1-CK-mfg-004 | P1 | D8/D2 | ck-mfg-workorder.md | 完工入库静默缺失——destWarehouseId/uomId 缺失时 generateCompletionMove 静默 return，工单照常 COMPLETED 但产成品永不入库 | 新增 | fixed | F2.5：generateCompletionMove 缺 destWarehouseId/uomId 不再静默 return——LOG.error + 新增 ERR_COMPLETION_WAREHOUSE_MISSING/ERR_COMPLETION_UOM_MISSING（G3 分级温和方案，不阻断完工；阻断曾破坏既有无产出仓工单测试契约） |
| P1-CK-mfg-005 | P1 | D2/D8 | ck-mfg-workorder.md | 领料红冲 GL/库存失败吞异常后仍推进终态 CANCELLED+posted=false——重试入口被守卫永久关闭，悬挂不可恢复且无告警 | 新增 | fixed | F2.5：ErpMfgMaterialIssueReverseConfirmProcessor 重写——红冲 GL/库存失败不再吞异常（NopException 中止整个 @BizMutation，DONE+posted=true 保持可重试，入口不封死）；TestErpMfgMaterialIssueReversal 2 用例绿 |
| P2-CK-mfg-006 | P2 | D5/D3，同型 P1-CK-pur-003 族 | ck-mfg-workorder.md | 通用 CRUD update/delete 无单据状态守卫——工单/作业卡/领料/快照族全实体裸 CrudBizModel | 同型 P1-CK-pur-003 族 | open | 同型注记：快照族实体无 posted/approveStatus 列守卫惰性，实质面为快照可手改（归 F2.5） |
| P2-CK-mfg-007 | P2 | D8 | ck-mfg-workorder.md | 齐套检查与看板聚合无 orgId 过滤——跨组织库存汇总致假齐套，KPI 跨组织混算 | 新增 | open |  |
| P2-CK-mfg-008 | P2 | D5/D4 | ck-mfg-workorder.md | ErpMfgBom.consumption（STRICT/WARNING/FLEXIBLE）运行时零消费——README 关键业务规则 3「消耗控制」未落地 | 新增 | open |  |
| P2-CK-mfg-009 | P2 | D3，疑似需求分歧只登记不裁决 | ck-mfg-workorder.md | STOCK_RESERVED/STOCK_PARTIAL 状态陷阱——齐套检查后工单不可取消不可关闭，预留无法经 cancel 释放 | 新增 | open |  |
| P2-CK-mfg-010 | P2 | D3/D5 | ck-mfg-workorder.md | reverseApprove 无 docStatus 守卫——IN_PROCESS/COMPLETED/CLOSED 工单可被翻 approveStatus=REJECTED 并洗掉审核审计字段 | 新增 | open |  |
| P2-CK-mfg-011 | P2 | D7/D2，同型 P2-CK-inv-012/P1-CK-pur-002 族 | ck-mfg-workorder.md | 完工链 REQUIRES_NEW 凭证先于主事务提交——中途失败留下孤儿凭证 | 新增 |open | F1.2 部分修复：报工链已修（updateEntity+预留释放前移，293 绿）；领料链可前移联动已前置但残余（aggregateIssueMaterialCost/issue DONE/applyMaterialCost + move.code 幂等键不稳定）归 F2.5 successor |
| P3-CK-mfg-012 | P3 | D5/D6 | ck-mfg-workorder.md | 报工入参无边界——recordWork 负数量直接累计且无上限校验；reportCompletion 负数静默按 0 处理 | 新增 | open |  |
| P3-CK-mfg-013 | P3 | D4 | ck-mfg-workorder.md | 死配置常量 + cron 键漂移——`erp-mfg.jobcard-auto-generate-cron` 零消费，实际接线键为 nop.job 前缀且默认 cron 非空 | 新增 | open |  |
| P3-CK-mfg-014 | P3 | D9 | ck-mfg-workorder.md | Dashboard 无界加载——准时率全表载入 COMPLETED 实体、完工量/趋势全实体内存求和 | 新增 | open |  |
| P3-CK-mfg-015 | P3 | D9/D10 | ck-mfg-workorder.md | findWorkOrderIdsWithJobCards 以 WO 数量作卡行查询 limit——截断致已建卡工单误判 pending，批量任务反复空跑告警噪音 | 新增 | open |  |
| P3-CK-mfg-016 | P3 | D2/D10，同型全域族 | ck-mfg-workorder.md | currentUserId 宽 catch 返回 null 无日志 + 配置读取宽 catch 静默默认 | 新增 | open |  |
| P3-CK-mfg-017 | P3 | D7，同型 P3-CK-pur-013 | ck-mfg-workorder.md | generatePendingJobCards 单事务逐单吞异常——失败单已保存的卡随外层提交 | 新增 | open |  |
| P3-CK-mfg-018 | P3 | D10 | ck-mfg-workorder.md | isInspectionGated 对 bomId 为空的工单恒 false——默认 BOM 的 inspectionRequired 不被门控消费 | 新增 | open |  |
| P3-CK-mfg-019 | P3 | D8，复用注记 RC-R1.49 | ck-mfg-workorder.md | AUTO_UPGRADE 读侧 re-resolve 默认 BOM 忽略工单显式 bomId——显式指定非默认 BOM 的工单需求口径被默认 BOM 顶替 | 新增 | open |  |
| P3-CK-mfg-020 | P3 | D5 | ck-mfg-workorder.md | applyLaborCostToWorkOrder 无工单状态守卫——终态/取消工单仍可被累计人工成本并触发单位成本重算 | 新增 | open |  |
| P3-CK-mfg-021 | P3 | D6，同型 P2-CK-inv-010 族 | ck-mfg-workorder.md | mfg 两 PostingEvent 汇率硬编码 ONE + 领料贷方科目 1401 硬编码不可配置（与 WIP 科目可配置不对称） | 新增 | open |  |
| P1-CK-mfg2-001 | P1 | D6 | ck-mfg-bom-mrp.md | SAFETY_STOCK 需求可用量双扣——聚合器已按「安全库存−可用量」求缺口作为需求量，引擎再减一次可用量，安全库存补货系统性低估（可用量 ≥ 安全库存一半时净需求恒 0，永不补货） | 主 agent 已实证（SimulationMrpEngine:285-289 净缺口 + MrpEngine:116-118 二次扣减） | fixed | F2.6：MrpEngine/SimulationMrpEngine top 需求拆分 SAFETY_STOCK（net 行跳过 available 扣减且不消耗）；TestErpMfgMrpEngine#testSafetyStockNetNotDoubleDeducted（safety=100/avail=99 → net=1 修复前 0；safety=100/avail=10 → net=90 修复前 80）红→绿 |
| P1-CK-mfg2-002 | P1 | D6 | ck-mfg-bom-mrp.md | MRP 无低阶码净额归集——同一物料多次出现（多个父件共享子件 / 既是独立需求又是子件）时每次出现独立扣减全部可用量，净需求系统性低估 | 新增 | fixed | F2.6：两引擎 processMaterial run 内 per-material 已消耗可用量累计（availableConsumed map，跨分支可用量仅扣一次）；TestErpMfgMrpEngine#testSharedComponentAvailableConsumedOnce（A+B 共享 C、avail(C)=50 → ΣC planned=150 修复前 100）红→绿；mrp.md L92 低层码声明同步修正 |
| P1-CK-mfg2-003 | P1 | D6 | ck-mfg-bom-mrp.md | SimulationMrpEngine.nextVersionNo 以 ASC 取最小 versionNo+1——同场景第 3 次仿真运行 versionNo 与既有 v2 冲突（UK (scenarioId,versionNo) 原始约束违例），设计声明的「粗调/细调/最终」多版本迭代循环第 3 次起必炸 | 新增 | fixed | F2.6：nextVersionNo 改 DESC 取最大 versionNo；TestErpMfgMrpSimulation#testThirdVersionRunSucceeds（v1→v2→v3 连续三次，第 3 次 versionNo=3）红→绿 |
| P2-CK-mfg2-004 | P2 | D6 | ck-mfg-bom-mrp.md | MRP 消费预测行不过滤 warehouseId——仓级预测行被 MRP 与 DRP 双引擎重复计入需求，MRP 毛需求虚高 | 新增 | open |  |
| P2-CK-mfg2-005 | P2 | D8 | ck-mfg-bom-mrp.md | MRP→工单→CRP 负荷链断裂——释放的工单不写 routingId/plannedEndDate，且 CRP 窗口查询 `ge("plannedEndDate",…)` 排除 NULL 完工日期工单、`distributeByWorkOrder` 要求 routingId 非空，MRP 释放工单对 CRP 永不可见 | 新增 | open |  |
| P2-CK-mfg2-006 | P2 | D4 | ck-mfg-bom-mrp.md | 仿真对比第 4 维「总采购额差」恒为 0——lookupStandardCost 硬编码返回 ZERO（ErpMdMaterial 无 standardCost 列，设计公式依赖不存在的列），owner doc 声明的 4 维 diff 实际 3 维 | 新增 | open |  |
| P2-CK-mfg2-007 | P2 | D7 | ck-mfg-bom-mrp.md | SimulationVersionComparator.indexLines 对 session 托管实体直接写值——@BizQuery 无事务使今日不落库，但污染同请求会话缓存，且任何事务上下文（job/mutation 复用）调用即静默持久化、破坏仿真快照不可变（AP-06） | 新增 | open |  |
| P2-CK-mfg2-008 | P2 | D3/D5 | ck-mfg-bom-mrp.md | cost-rollup-status 状态机缺失——FIRMED 无命名 writer（仅裸 CRUD 直改 status 可达），DRAFT/CANCELLED 为零 writer 死值且无迁移守卫；而标准成本读侧（findLatestFirmedStandardCost/差异计算）以 FIRMED 为消费前提 | 新增 | open |  |
| P2-CK-mfg2-009 | P2 | D4 | ck-mfg-bom-mrp.md | ErpMfgBom.useMultiLevelBom 列零消费——owner doc「工单审核时可配置 use_multi_level_bom 控制展开深度」未落地，齐套/预留展开恒多级硬编码 | 新增 | open |  |
| P2-CK-mfg2-010 | P2 | D6/D10 | ck-mfg-bom-mrp.md | BOM.qty 为 0/空时展开静默归零——divide 除数为零返回 ZERO 无错误，qty=0 的 BOM 全部子件有效用量=0，齐套空过（假齐套）/MRP 子件零需求/成本卷算材料成本 0，全链静默 | 新增 | open |  |
| P2-CK-mfg2-011 | P2 | D6 | ck-mfg-bom-mrp.md | 同物料多需求日聚合取最晚 requirementDate——整批毛需求按最晚日期倒排提前期，最早需求的供给系统性延迟 | 新增 | open |  |
| P2-CK-mfg2-012 | P2 | D5，同型 P2-CK-mfg-006 / P1-CK-pur-003 族 | ck-mfg-bom-mrp.md | 切片内 20 个裸 CrudBizModel 无状态/不可变守卫——计划行 isFirmed/convertedBillCode、卷算成本行、负荷快照可经通用 update 直改 | 同型 P1-CK-pur-003 族 |fixed | F1.3：统一基类接入自动生效（同型 P1-CK-pur-003 族，全域守卫惰性/激活由列存在性决定） |
| P3-CK-mfg2-013 | P3 | D9 | ck-mfg-bom-mrp.md | MRP 需求聚合与安全库存扫描 N+1 + 全表加载——销售订单全表载入+逐单调行、物料全表扫描+逐物料查余额、CRP 工作中心编码全表内存过滤、FIRMED 卷算头全载+逐头查行 | 新增 | open |  |
| P3-CK-mfg2-014 | P3 | D3 | ck-mfg-bom-mrp.md | mrp-order-type 字典 PLANNED_ORDER 死值 + SUBCONTRACT_REQUEST 引擎零产出——仅 PURCHASE/WORK 两类有引擎 writer | 新增 | open |  |
| P3-CK-mfg2-015 | P3 | D7/D2 | ck-mfg-bom-mrp.md | ErpMfgCrpRunJob 直调 BizModel 无事务包裹 + 失败仅 LOG.error 无告警——清区间与重写非原子，job 中途失败留部分负荷快照 | 新增 | open |  |
| P3-CK-mfg2-016 | P3 | D6 | ck-mfg-bom-mrp.md | CRP 产能聚合三处近似——efficiencyFactor 取 per-material 最大值（最乐观）、重叠班次时段双计、shift 起止解析异常静默 0 产能（假超载） | 新增 | open |  |
| P3-CK-mfg2-017 | P3 | D5 | ck-mfg-bom-mrp.md | releasePurchaseRequest/releaseSubcontractRequest 缺 currencyId null 守卫——必填列触发原始 DB 错误而非业务错误码 | 新增 | open |  |
| P3-CK-mfg2-018 | P3 | D2/D6 | ck-mfg-bom-mrp.md | CostRollupService 配置静默降级 + 委外成本归集分子分母口径错配——overhead 费率解析失败静默 0；委外单位成本 = 整单加工费 / 本物料行数量 | 新增 | open |  |
| P3-CK-mfg2-019 | P3 | D8 | ck-mfg-bom-mrp.md | plan.orgId 为空时跨组织合并需求/库存 + CRP 全程无组织维度——违反 mrp.md 关键业务规则 5「不跨公司合并需求」的边界未裁决 | 新增 | open |  |
| P3-CK-mfg2-020 | P3 | D2 | ck-mfg-bom-mrp.md | 仿真链错误码语义漂移——场景/版本「不存在」抛「无基线计划」/「已转正式计划」错误码，诊断误导 | 新增 | open |  |
| P3-CK-mfg2-021 | P3 | D5 | ck-mfg-bom-mrp.md | MRP 销售订单需求来源含未审核单据——仅排除 CANCELLED，DRAFT/未审批订单进入毛需求（幻影需求） | 新增 | open |  |
| P3-CK-mfg2-022 | P3 | D5/D6 | ck-mfg-bom-mrp.md | 仿真/转正 code 后缀拼接无长度守卫——base plan code 接近 50 列宽时 `-SIM-V{n}`/`-PROMOTED-{n}` 溢出触发原始 DB 错误 | 新增 | open |  |
| P3-CK-mfg2-023 | P3 | D8/D2 | ck-mfg-bom-mrp.md | CostRollupService 不写 orgId + 同日重复卷算无去重/无废止——rollup 头 orgId 恒 null，同 BOM 同日多次卷算累积重复 CALCULATED 头 | 新增 | open |  |
| P1-CK-mfg3-001 | P1 | D6/D8 | ck-mfg-subcontract.md | 委外成品入库计价仅含加工费——材料成本未计入产成品存货，1408 委外物资科目永久残留材料成本净额 | 新增 | fixed | F2.7：computeReceiptUnitCost 分子 = 发料移动单流水 \|totalCost\| 合计 + 头 processingFee（aggregateIssueMaterialCost 新增）；TestErpMfgSubcontracting#testFullLifecycleWithPosting 断言 unitCost=(2×5+50)/1=60 + 产成品余额 totalCost=60（修复前 50）；快照重录 |
| P1-CK-mfg3-002 | P1 | D8/D6 | ck-mfg-subcontract.md | reverseCompletion 永不反向委外收货（成品入库）移动单——红冲后材料退回但成品仍滞留库存，数量守恒破坏 | 新增 | fixed | F2.7：inv ErpInvStockMoveProcessor.inverseMoveType(MANUFACTURE)→OUTGOING（入库类反向语义）+ mfg canSafelyReverse 补 MANUFACTURE 分支（要求 destWarehouseId 非空）；TestErpMfgSubcontractReverse#testReverseCompletionRollsBackPostedAndStatus 断言红冲后 P 余额归 0 + M1 余额恢复 10（修复前成品滞留）；快照重录（REVERSAL 收货反向移动单出现） |
| P1-CK-mfg3-003 | P1 | D3/D5 | ck-mfg-subcontract.md | 审批族 Pattern B custom override 绕过骨架 validateNotCancelled——CANCELLED 委外单可被 submit/approve 复活并重新进入发料链 | 新增 | fixed | F2.7：validateTransitionForSubmit/Approve/Reject 前置 assertNotCancelled（docStatus==CANCELLED 拒绝）；TestErpMfgSubcontracting#testCancelledOrderCannotRevive（复活路径一 approve 拒绝 + 路径二 submit 拒绝）红→绿 |
| P1-CK-mfg3-004 | P1 | D6/D10 | ck-mfg-subcontract.md | 基因链输入批次按产成品仓解析——原料批次在原料库时系统性解析失败，前向追溯/召回报告静默为空 | 新增 | fixed | F2.7：BatchGenealogyWriter.findIssueLinesWithBatch 携带领料单头 warehouseId，doWrite 传 issue 仓给 resolveInputLot；TestErpMfgBatchGenealogy#testMultiWarehouseInputLotResolved（输入批次原料仓 3601、产出行成品仓 3602 → 基因链行写入，修复前空）红→绿 |
| P1-CK-mfg3-005 | P1 | D6/D4 | ck-mfg-subcontract.md | SUBCONTRACT 差异实际侧 `wo.subcontractCost` 全仓零 writer——实际委外费恒 0，差异行失真并可过账错误方向凭证 | 新增 | fixed | F2.7：ErpMfgWorkOrderProcessor.applySubcontractCostToWorkOrder 完工归集 writer（config-gated CONFIG_SUBCONTRACT_COST_AGGREGATION_ENABLED，按产品聚合 COMPLETED 委外单加工费 × 完工量/委外总量分摊）；TestErpMfgProductionVariance#testSubcontractCostAggregatedOnCompletion（fee 100/qty 10/完工 2 → wo.subcontractCost=20，修复前恒 0）红→绿 |
| P2-CK-mfg3-006 | P2 | D2/D8，同型 P1-CK-mfg-005 族新站点 | ck-mfg-subcontract.md | 委外红冲三段 GL + 库存反向失败吞异常后仍推进终态 CANCELLED+posted=false——入口守卫永久封口且无告警通道 | 新增 | open |  |
| P2-CK-mfg3-007 | P2 | D5/D10 | ck-mfg-subcontract.md | 三段动作入参边界缺失——仓库参数可空直传库存、收货数量无上限守卫、非正数量静默替换 | 新增 | open |  |
| P2-CK-mfg3-008 | P2 | D6 | ck-mfg-subcontract.md | 基因链同批次多领料行去重丢量——inputQty 只记首个匹配行 | 新增 | open |  |
| P2-CK-mfg3-009 | P2 | D8/D2 | ck-mfg-subcontract.md | 差异重算链 reverseIfExists 以「posted 行存在」为门控 + 红冲吞异常 + post 幂等命中返回 null 三者复合——一次红冲失败后工单差异永久悬挂 | 新增 | open |  |
| P2-CK-mfg3-010 | P2 | D8，同族 P2-CK-mfg-007 | ck-mfg-subcontract.md | findFirmedRollupLine 无 orgId/期间过滤——跨组织标准成本混用 + 差异计算取「全局最新 FIRMED」 | 新增 | open |  |
| P2-CK-mfg3-011 | P2 | D5，同型 P1-CK-pur-003 族 | ck-mfg-subcontract.md | 切片实体全裸 CrudBizModel 无状态守卫——基因链/差异行/委外单可经通用 CRUD 手改 | 同型 P1-CK-pur-003 族 |fixed | F1.3：统一基类接入自动生效（同型 P1-CK-pur-003 族，全域守卫惰性/激活由列存在性决定） |
| P2-CK-mfg3-012 | P2 | D3，同型 P2-CK-mfg-010 | ck-mfg-subcontract.md | reverseApprove 无 docStatus 守卫——ISSUED/RECEIVED/COMPLETED 委外单可翻 approveStatus=REJECTED 并洗掉审批审计字段 | 新增 | open |  |
| P3-CK-mfg3-013 | P3 | D10 | ck-mfg-subcontract.md | traceChain 深度边界 off-by-one——恰好 depth 条边的链抛 MAX_DEPTH_EXCEEDED 并丢弃全部已收集结果 | 新增 | open |  |
| P3-CK-mfg3-014 | P3 | D6/D8 | ck-mfg-subcontract.md | 基因行 outputQty 每行重复写全量 completedQty + recallReport 口径失真（lotStatus 硬编码 RELEASED、中间品计入受影响成品） | 新增 | open |  |
| P3-CK-mfg3-015 | P3 | D2/D10 | ck-mfg-subcontract.md | 基因链写入静默跳过路径零日志——best-effort 语义下缺口不可观测 | 新增 | open |  |
| P3-CK-mfg3-016 | P3 | D5/D4 | ck-mfg-subcontract.md | 死列族——postedStatus/amountSource/amountFunctional/totalAmount/line.unitProcessingFee/line.amount 运行时零消费 | 新增 | open |  |
| P3-CK-mfg3-017 | P3 | D1 | ck-mfg-subcontract.md | SUBJECT_FINISHED_GOODS 常量名/注释与值相悖——名为产成品实为 1401 原材料，共享常量误用陷阱 | 新增 | open |  |
| P3-CK-mfg3-018 | P3 | D9 | ck-mfg-subcontract.md | 差异标准侧全量载入 + 逐头查行 N+1；基因链逐领料单查行；recallReport 无深度上限 | 新增 | open |  |
| P1-CK-ast-001 | P1 | D6 | ck-assets-lifecycle.md | Split PROPORTIONAL 模式累计折旧被二次派生覆盖——最大项补差失效，Σ 新卡累计折旧 ≠ 源资产累计折旧（价值守恒破坏） | 新增 | open |  |
| P1-CK-ast-002 | P1 | D6/D8 | ck-assets-lifecycle.md | Split 目标卡 residualValue 全额复制到每张新卡——Σ 残值 N 倍放大，卡片残值与折旧计划残值口径分叉，直线法折旧系统性低估 | 新增 | open |  |
| P1-CK-ast-003 | P1 | D5 | ck-assets-lifecycle.md | 盘点 reconcile 无实盘数量完整性校验——漏录行按实盘 0 判盘亏，processVariance 将在用资产静默 SCRAPPED | 新增 | open |  |
| P1-CK-ast-004 | P1 | D8/D2 | ck-assets-lifecycle.md | ValueAdjustment 资产净值联动仅在过账同步成功时执行——悬挂窗口 sweep 重试成功后 GL 有凭证但资产 NBV 永不联动，且无重试入口 | 新增 | open |  |
| P1-CK-ast-005 | P1 | D5/D6 | ck-assets-lifecycle.md | ValueAdjustment 减值金额无上限校验——调整额可超过账面净值使 NBV/CurrentValue 为负 | 新增 | open |  |
| P1-CK-ast-006 | P1 | D6 | ck-assets-lifecycle.md | Split/Merge 新卡折旧口径三重错位——Merge 残值归零+全年限加权致执行期过度折旧；两链计划从源购置次月重排（覆盖历史期间）+ 计划基数不减已提 | 新增 | open |  |
| P2-CK-ast-007 | P2 | D5/D3，同型 P1-CK-pur-003 族 | ck-assets-lifecycle.md | 通用 CRUD update/delete 无单据状态守卫——资产卡片可删可改，且 delete 直接传导资本化 reverseApprove 静默跳过回滚 | 同型 P1-CK-pur-003 族 |fixed | F1.3：统一基类接入自动生效（同型 P1-CK-pur-003 族，全域守卫惰性/激活由列存在性决定） |
| P2-CK-ast-008 | P2 | D7/D8 | ck-assets-lifecycle.md | Cap/Disposal executeReverseApprove 红冲凭证（REQUIRES_NEW 已提交）先于资产状态守卫——assert 抛错时 GL 已红冲而主事务回滚，两面失配无信号 | 新增 | open |  |
| P2-CK-ast-009 | P2 | D8，复用注记 P2-RC-086 + 同型 P2-CK-fin2-007/P2-CK-mfg-007 族 | ck-assets-lifecycle.md | Dashboard 全部查询无 orgId 过滤——跨组织 KPI 混算 | 新增 | open |  |
| P2-CK-ast-010 | P2 | D8 | ck-assets-lifecycle.md | CIP 余额 KPI 部分转固后不扣减——accumulatedCost 保留归集全额，已转固成本在固定资产与在建工程余额双计 | 新增 | open |  |
| P2-CK-ast-011 | P2 | D5/D8 | ck-assets-lifecycle.md | CIP reverseTransfer 无 capitalizationId 归属校验——传入他 CIP 资本化单可错误红冲并回退错误 CostItem | 新增 | open |  |
| P2-CK-ast-012 | P2 | D8 | ck-assets-lifecycle.md | 盘点 reverse 只红冲凭证回退单据——不回滚盘盈新卡/盘亏 SCRAPPED，单据轴可逆与资产轴不可逆不对称 | 新增 | open |  |
| P2-CK-ast-013 | P2 | D10 | ck-assets-lifecycle.md | Merge 悬空源引用 null 被 validateSources continue 放行——executeApprove sum() 对 null 实体 NPE（500 而非业务错误码） | 新增 | open |  |
| P2-CK-ast-014 | P2 | D5 | ck-assets-lifecycle.md | Merge 源资产行无去重——同一资产出现在多行时价值双计、贡献比例失真 | 新增 | open |  |
| P2-CK-ast-015 | P2 | D7，同型 P2-CK-inv-012/P1-CK-pur-002/P2-CK-mfg-011 族 | ck-assets-lifecycle.md | REQUIRES_NEW 凭证先于主事务末步提交——ast 六链 approve 末段 updateEntity 仍可失败，中途失败留孤儿凭证 | 新增 |fixed | F1.2：VA applyAssetValueChange 无条件前移 + reverse 无条件回滚配对（R1）；Disposal 双凭证尾部连续（catchUp 数据/凭证段拆分 + 回填后置）；TestErpAstDisposalEquipmentLinkage 双零回链断言；四链 (c) 收敛 |
| P2-CK-ast-016 | P2 | D6 | ck-assets-lifecycle.md | ValueAdjustment 折旧基数调整为死代码——newDepreciableBase 计算后从未使用，减值后未来折旧不降 | 新增 | open |  |
| P3-CK-ast-017 | P3 | D6 | ck-assets-lifecycle.md | 资本化/盘盈建卡 residualValue 硬编码 ZERO——cip.md「预计净残值按资产类别比例计算」无字段支撑 | 新增 | open |  |
| P3-CK-ast-018 | P3 | D6 | ck-assets-lifecycle.md | 非直线法建卡折旧计划 plannedAmount 全 0——DECLINING/UNITS 资产计划骨架空转 | 新增 | open |  |
| P3-CK-ast-019 | P3 | D5 | ck-assets-lifecycle.md | Disposal 处置收入负数无校验——负 disposalAmount 直接进 gainLoss 与凭证 | 新增 | open |  |
| P3-CK-ast-020 | P3 | D10 | ck-assets-lifecycle.md | 处置补提链 YearMonth.parse(lastExecuted) 无守卫——脏 period 数据触发非业务异常 | 新增 | open |  |
| P3-CK-ast-021 | P3 | D2 | ck-assets-lifecycle.md | Split/Merge/VA/Inventory 过账失败即时告警缺失——对照 Cap/Disposal 的 dispatchFailureAlert 不对称 | 新增 | open |  |
| P3-CK-ast-022 | P3 | D2，同型全域族 | ck-assets-lifecycle.md | currentUserId 宽 catch 返回 null 无日志——审计字段 approvedBy/postedBy 写 null 无信号 | 新增 | open |  |
| P3-CK-ast-023 | P3 | D3/D8 | ck-assets-lifecycle.md | Split/Merge/盘亏终态资产不取消 PENDING 折旧计划、盘亏 NBV 不清零——与 Disposal 的 cancelPendingSchedules/NBV 归零不一致 | 新增 | open |  |
| P3-CK-ast-024 | P3 | D9 | ck-assets-lifecycle.md | Dashboard 趋势全表载入（loadExecutedSchedulesInRange 名不符实）+ 本月新购资产折旧假预警 | 新增 | open |  |
| P3-CK-ast-025 | P3 | D3 | ck-assets-lifecycle.md | suspend 备注标记无限追加——多轮闲置/恢复循环后 remark 堆叠，resume 不清理 | 新增 | open |  |
| P3-CK-ast-026 | P3 | D8/D10 | ck-assets-lifecycle.md | findAssetByCode 无 orgId 过滤——UK 为 (code, orgId)，跨组织同 code 时 get(0) 命中不确定 | 新增 | open |  |
| P3-CK-ast-027 | P3 | D8 | ck-assets-lifecycle.md | ValueAdjustment reverseApprove 无资产状态守卫——已处置资产的 NBV 仍被回写 | 新增 | open |  |
| P1-CK-ast2-001 | P1 | D6 | ck-assets-depreciation.md | 工作量法（UNITS）折旧恒为 0——两个调用点均传 null 工作量参数，且 ORM 无任何工作量数据列，dict 可选方法静默失效 | 新增 | open |  |
| P1-CK-ast2-002 | P1 | D6 | ck-assets-depreciation.md | 资本化维修折旧基数增量双计——applyTreatmentCapitalize 先把原值 +=X 再调 recalculateForCapitalizationMaintenance(assetId, X)，recalc 内 `original.add(increment)` 再加一次 X | 主 agent 已实证（MaintenanceProcessor:95 先加 + Recalc:37/58 再加） | open |  |
| P1-CK-ast2-003 | P1 | D6/D4 | ck-assets-depreciation.md | 批量折旧缺「当月增加下月提」守卫——期末结账常规路径对资本化当月资产照常计提，且与 nop-batch job 路径（按次月起的 PENDING 计划行）语义分裂 | 新增 | open |  |
| P1-CK-ast2-004 | P1 | D8 | ck-assets-depreciation.md | 逆资本化红冲回退闭环断裂——reverseApprove 只红冲 CAPITALIZATION 凭证，已执行的 DEPRECIATION 凭证滞留 GL，资产累计却清零；cancelSchedules 无状态过滤直接把 EXECUTED 行写 CANCELLED（绕过 assertCanCancel） | 新增 | open |  |
| P1-CK-ast2-005 | P1 | D8/D2 | ck-assets-depreciation.md | assets 域无凭证红冲反写监听 + 引擎层 REVERSAL 失败被 sweep 异步重试红冲——异步红冲成功后资产侧 posted/累计折旧不回退，事后手动红冲报 ERR_REVERSE_SOURCE_NOT_FOUND 死锁 | 新增 | open |  |
| P1-CK-ast2-006 | P1 | D8 | ck-assets-depreciation.md | 价值调整后资产账面与 GL/处置三方失同步——净值只改 netBookValue/currentValue（originalValue/累计折旧不动），处置凭证净值 = 原值−累计折旧 忽略调整额，减值准备科目处置时不清 | 新增 | open |  |
| P2-CK-ast2-007 | P2 | D6 | ck-assets-depreciation.md | elapsed 口径 = 全部 EXECUTED 行数而非「早于目标期的行数」——补提/重执行非最新期间时双倍余额递减的剩余期数与直线切换点错位 | 新增 | open |  |
| P2-CK-ast2-008 | P2 | D2/D7，同型 P3-CK-pur-013/P3-CK-mfg-017 族·站点升级 | ck-assets-depreciation.md | executeBatchDepreciation 单事务逐资产吞异常——失败资产 session 脏写残留可致后续资产连锁失败/整批回滚，而每资产凭证已 REQUIRES_NEW 独立提交 | 新增 | open |  |
| P2-CK-ast2-009 | P2 | D2/D3 | ck-assets-depreciation.md | 维修过账失败后重试入口被封死且无告警——post() 无论凭证成败都翻 status=POSTED；CAPITALIZE 路径失败时资产原值已增/计划已重算而 GL 缺凭证 | 新增 | open |  |
| P2-CK-ast2-010 | P2 | D4 | ck-assets-depreciation.md | 减值/重估「折旧基数调整」为死代码——newDepreciableBase 计算后丢弃，config `erp-ast.revaluation-adjust-depreciation-base` 声明默认 true 但零效果 | 新增 | open |  |
| P2-CK-ast2-011 | P2 | D5/D3，同型 P1-CK-pur-003 族 | ck-assets-depreciation.md | assets 全实体裸 CrudBizModel 无状态守卫——折旧计划行/资产卡片汇总列可直接手改；executeDepreciation 不调 assertCanExecute，REVERSED/CANCELLED 行可被覆盖复活 | 同型 P1-CK-pur-003 族 |fixed | F1.3：统一基类接入自动生效（同型 P1-CK-pur-003 族，全域守卫惰性/激活由列存在性决定） |
| P2-CK-ast2-012 | P2 | D9 | ck-assets-depreciation.md | 批量折旧 N+1——每资产 4+ 次查询（期间重复查/计划行/EXECUTED 全实体加载/类别懒加载），countExecuted 用 findAllByQuery().size() | 新增 | open |  |
| P2-CK-ast2-013 | P2 | D8 | ck-assets-depreciation.md | 维修独立维修贷方科目分支死代码——Provider 声明「贷存货（备件）/银行（外协）」但 linkedVisit=false 一律贷 1002 银行，inventorySubject 读了不用；科目全部硬编码不读类别配置 | 新增 | open |  |
| P2-CK-ast2-014 | P2 | D5 | ck-assets-depreciation.md | 资产 CRUD 直接建卡无折旧配置校验——0/null 年限静默按 1 个月折完、残值>原值静默零提、UNITS 可选；ERR_DEPRECIATION_USEFUL_LIFE_INVALID 错误码零消费 | 新增 | open |  |
| P3-CK-ast2-015 | P3 | D4 | ck-assets-depreciation.md | 折旧三个死配置常量 + cron 键漂移 + owner doc 声称的 `ErpAstDepreciationJob` 类不存在 | 新增 | open |  |
| P3-CK-ast2-016 | P3 | D1 | ck-assets-depreciation.md | `java.time.YearMonth.now()` 直读系统时钟——CoreMetrics 时间可控约定违例（checker R7 盲区变体） | 新增 | open |  |
| P3-CK-ast2-017 | P3 | D8/D6，同型 P2-CK-inv-010 族 | ck-assets-depreciation.md | 折旧多币种面零填充——schedule 的 currencyId/exchangeRate/amountSource/amountFunctional 四列从不写入；PostingEvent 汇率恒 ONE | 新增 | open |  |
| P3-CK-ast2-018 | P3 | D6 | ck-assets-depreciation.md | 非直线法 plannedAmount 恒 0 + 维修重算固定直线均摊不读折旧方法 | 新增 | open |  |
| P3-CK-ast2-019 | P3 | D5，疑似设计分歧只登记不裁决 | ck-assets-depreciation.md | 残值配置面缺失——资本化强制 residual=0，类别/资本化单均无残值输入列，设计 §1.4 残值约束在主路径形同虚设 | 新增 | open |  |
| P3-CK-ast2-020 | P3 | D8 | ck-assets-depreciation.md | currentValue 字段语义漂移（折旧从不维护、5 处 writer 3 种语义）+ depreciationRate 死列 | 新增 | open |  |
| P3-CK-ast2-021 | P3 | D7，同型 P2-CK-inv-012/P1-CK-pur-002 族 | ck-assets-depreciation.md | 折旧凭证 REQUIRES_NEW 先于主事务剩余步骤提交——乐观锁/回滚窗口留孤儿凭证 | 新增 |fixed | (c) 收敛：billHeadCode=asset#period 稳定 + F1.1 引擎幂等返回既有 id → 重试 posted 收敛（ast 无该链代码变更，引擎层传导） |
| P3-CK-ast2-022 | P3 | D2 | ck-assets-depreciation.md | catchUpDepreciation 补提凭证失败后重跑同参数静默 no-op——全 EXECUTED 跳过导致凭证重试通道不存在；total=0 时行留 posted=false | 新增 | open |  |
| P3-CK-ast2-023 | P3 | D9/D8 | ck-assets-depreciation.md | 折旧明细报表与看板数据面全表加载 + 无 orgId 过滤 | 新增 | open |  |
| P1-CK-prj-001 | P1 | D8 | ck-projects.md | 工时 cancel 只红冲 GL 凭证，不回退成本归集——归集行/头 totalAmount/project.actualCost 永久残留，撤回改时重提后归集金额陈旧 | 新增 | open |  |
| P1-CK-prj-002 | P1 | D6/D8 | ck-projects.md | 收入链路断裂——PnL 收入与结算收入依赖 `ErpPrjBilling.amountFunctional`，该字段全仓零 writer（默认 0），开票金额 totalAmount 永不进入收入计算 | 新增 | open |  |
| P1-CK-prj-003 | P1 | D8 | ck-projects.md | 费用归集幂等键用报销单号（头级）而非行级——跨项目报销单只有第一个刷新的项目被归集，其余项目行被 existsLine 静默吞掉 | 新增 | open |  |
| P1-CK-prj-004 | P1 | D5/D8 | ck-projects.md | 结算单无重复创建守卫——同项目可反复 createSettlement+approve，每张都按全量 finalRevenue/finalCost 过账，GL 收入/成本重复确认 | 新增 | open |  |
| P1-CK-prj-005 | P1 | D8/D2 | ck-projects.md | CLOSE 结算过账失败时资产卡片已建 IN_SERVICE，而 cancel 的资产回退被锁在 posted=true 分支内——卡片永久滞留在役（将进入折旧），且 reverseSettlement 被 posted 硬守卫封死 | 新增 | open |  |
| P1-CK-prj-006 | P1 | D7/D8，同型 P2-CK-inv-012/P1-CK-pur-002 族；P1-CK-fin-003 受影响面确认 | ck-projects.md | 工时/结算 approve 的 tryPost（REQUIRES_NEW）先于主事务提交——主事务失败留下孤儿凭证，且重试时 post() 幂等命中返回 null（fin-003）使 posted 永久 false、cancel 无法红冲，悬挂不可恢复 | 新增 |fixed | F1.1 幂等部分 + F1.2 工时链聚合前移；结算链 billHeadCode 稳定经 F1.1 收敛；testIdempotentRepostKeepsPostedTrue + prj 173 绿；posted 回写窄窗 watch-only（plan Deferred） |
| P1-CK-prj-007 | P1 | D6 | ck-projects.md | 看板毛利率聚合对多快照项目全量求和——每项目多行 PnL 全部相加，收入/成本/毛利随快照数线性放大 | 新增 | open |  |
| P2-CK-prj-008 | P2 | D5/D3，同型 P1-CK-pur-003 族 | ck-projects.md | 全实体裸 CrudBizModel 无状态守卫——已过账工时/结算/归集头可经通用 update/delete 直接改删 | 同型 P1-CK-pur-003 族 |fixed | F1.3：统一基类接入自动生效（同型 P1-CK-pur-003 族，全域守卫惰性/激活由列存在性决定） |
| P2-CK-prj-009 | P2 | D5/D10 | ck-projects.md | 入参边界缺失——工时 submit 不校验负 hours；createSettlement 不校验 settlementType 字典值且 project 缺失时 NPE | 新增 | open |  |
| P2-CK-prj-010 | P2 | D2 | ck-projects.md | 结算过账失败无告警通道——posted=false 悬挂仅 LOG 可见（工时链有 prj.timesheet-posting-failure 告警，结算链缺失） | 新增 | open |  |
| P2-CK-prj-011 | P2 | D6/D4 | ck-projects.md | refreshPnl 默认期间 `to=today()` 使幂等键逐日漂移——job 每日运行/逐日手工刷新每项目新建一行快照，幂等「清旧重建」失效、表无界增长 | 新增 | open |  |
| P2-CK-prj-012 | P2 | D8/D6 | ck-projects.md | CostCollection 头 amountSource/amountFunctional 仅建头时一次性写入，后续聚合只累加 totalAmount——头本位币金额永久停留在首笔，结算 COST 行金额失真 | 新增 | open |  |
| P2-CK-prj-013 | P2 | D6 | ck-projects.md | 结算 Provider 未按 RC-R1.64 双金额范式折算——fact() 不设 amountSource/amountFunctional，event.exchangeRate 透传但引擎 fallback 使汇率不作用于金额（当前潜伏，P2-RC-050 激活后显性） | 新增 | open |  |
| P2-CK-prj-014 | P2 | D1 | ck-projects.md | rollbackAssetIfNeeded 经 daoFor(ErpAstAsset) 直接跨域写 assets 实体——绕过 IErpAstAssetBiz 与 assets 状态机守卫 | 新增 | open |  |
| P3-CK-prj-015 | P3 | D9 | ck-projects.md | 聚合链 N+1 与全表加载——费用归集全表载入已审核报销单+逐单调行；预算/成本内存全量求和；既有头路径逐行 nextLineNo 全量加载 | 新增 | open |  |
| P3-CK-prj-016 | P3 | D7 | ck-projects.md | 归集头创建与幂等去重无 UK/锁支撑——并发聚合可产生多头与重复归集行 | 新增 | open |  |
| P3-CK-prj-017 | P3 | D3/D6 | ck-projects.md | PnL 收入口径与 javadoc 声明漂移——只排除 CANCELLED 不过滤审批态（Billing 审批轴 successor 残留）；PnL posted 列零 writer 使「已过账冻结重算」守卫为死守卫 | 新增 | open |  |
| P3-CK-prj-018 | P3 | D6/D10 | ck-projects.md | PnL 窄期间成本按归集头 businessDate 归属而非行发生日——期间切片快照成本系统性错配 | 新增 | open |  |
| P3-CK-prj-019 | P3 | D3/D9 | ck-projects.md | 延期预警消费终态 CANCELLED——已取消项目持续误报延期；看板全域 orgId 缺失复用 P2-RC-086 注记 | 新增 | open |  |
| P3-CK-prj-020 | P3 | D2/D10，同型全域族 | ck-projects.md | 宽 catch 静默 + 错误码误用 + 审计字段兜底 "system" | 新增 | open |  |
| P3-CK-prj-021 | P3 | D4 | ck-projects.md | 死常量与死方法——结算借贷科目 billData 键零消费、parseAmount 零调用 | 新增 | open |  |
| P1-CK-qa-001 | P1 | D7/D6 | ck-quality.md | SPC 计量型采样幂等键只覆盖每子组首点——第二次起调度将已采样点重混成「幻影子组」，样本表数据污染 → 控制限漂移 → 假失控 → 假 NCR/CAPA | 新增 | open |  |
| P1-CK-qa-002 | P1 | D3/D8 | ck-quality.md | isInspectionCleared 全量 AND 语义使 REJECTED 永久阻塞——owner doc 设计的「复检新建质检单、以复检结果为准」路径无法解锁强制质检门 | 新增 | open |  |
| P1-CK-qa-003 | P1 | D4 | ck-quality.md | spc-capability.batch.xml 简单名 inject 运行时必失败——启用能力分析 job 即每 chunk 抛 ERR_IOC_UNKNOWN_BEAN_FOR_NAME，自动调度链断裂（P1-RC-042 同族，RC-R1.26 已 watch-only 登记未修） | 新增 | open |  |
| P1-CK-qa-004 | P1 | D6/D1 | ck-quality.md | autoCreateNcrFromInspection 用 Integer 20 写 String 字典列 severity——主路径自动 NCR 的 severity 恒为非法字典值 "20"，UI 映射失效并污染召回升级与报表聚合 | 主 agent 已实证（NcrLifecycleService:64 Integer 20 → string dict 列） | open |  |
| P1-CK-qa-005 | P1 | D3/D5 | ck-quality.md | Recall reverseApprove 双轴不联动——审批撤销后 status 仍 APPROVED：重提死锁 + 已撤审召回仍可定位目标/生成退货，绕过强制审批门 | 新增 | open |  |
| P2-CK-qa-006 | P2 | D5/D3，同型 P1-CK-pur-003 族 | ck-quality.md | 通用 CRUD update/delete 无状态守卫——质检单/行/NCR/Action/Recall/SPC 全实体裸 CrudBizModel，终态与已过账数据可直改 | 同型 P1-CK-pur-003 族 |fixed | F1.3：统一基类接入自动生效（同型 P1-CK-pur-003 族，全域守卫惰性/激活由列存在性决定） |
| P2-CK-qa-007 | P2 | D5 | ck-quality.md | recordResult 让步路径同事务自动「自审通过」——记录结果者即让步审批人，owner doc「质量主管审核」简化语义未落地 | 新增 | open |  |
| P2-CK-qa-008 | P2 | D6/D10 | ck-quality.md | classifyByCpk(null) 返回 INADEQUATE——规格限缺失/σ=0 致 Cpk 不可算时误判最差级，触发 QualityGoal 清值 + 每日 RiskRegister 噪音 | 新增 | open |  |
| P2-CK-qa-009 | P2 | D9 | ck-quality.md | SPC 采样每小时全量加载历史 + 逐行 getEntityById N+1——findSamples 全样本 JSON 重解析 + resolveInspection 循环单查 + findApprovedInspectionLines 无时间窗全表扫 | 新增 | open |  |
| P2-CK-qa-010 | P2 | D6/D5 | ck-quality.md | SPC 系数表只覆盖 n=2..10 且越界静默回落 n=10；subgroupSize=1 被 `subgroupSize < 2` 拒绝——subgroupSize>10 控制限偏窄、X_MR 单值图不可采样 | 新增 | open |  |
| P2-CK-qa-011 | P2 | D8，同型 P2-CK-fin2-007/P2-CK-mfg-007 orgId 隔离族 | ck-quality.md | NCR 报废计价与退货编排按 materialId 裸 limit 1 取任意库存余额行——多仓/多组织下金额与仓/币别任意性 | 新增 | open |  |
| P2-CK-qa-012 | P2 | D7/D2，同型 P2-CK-inv-012/P1-CK-pur-002/P2-CK-mfg-011 族 | ck-quality.md | NCR resolve 的 SCRAP 凭证 REQUIRES_NEW 先于主事务后续步骤提交——中途失败留孤儿凭证 | 新增 |fixed | (c) 收敛：billHeadCode=ncr.code 稳定 + qa-013 已修（F1.1）→ SCRAP 幂等重试 posted 收敛（qa 无代码变更，引擎层传导） |
| P2-CK-qa-013 | P2 | D8，同型 P1-CK-fin-003 族 | ck-quality.md | dispatchScrap 以 `voucherId != null` 判成功置 posted——post() 幂等命中返回 null 时 posted 永不置位，postNcr 重试静默无效 + reverseNcr 守卫封死 | 新增 |fixed | 并入 F1.1 引擎层修复（幂等命中非 null，voucherId != null 置 posted 成立） |
| P2-CK-qa-014 | P2 | D8 | ck-quality.md | NcrReturnOrchestrator 创建的采购/销售退货单为无行空壳——仅头字段 + remark，无物料/数量行，NCR.returnCode 登记的是一张「无物可退」的草稿 | 新增 | open |  |
| P3-CK-qa-015 | P3 | D5 | ck-quality.md | resolve 的 RETURN 编排不受 ncr-posting-mode 门控——MANUAL_POST 模式下仍自动创建退货草稿（且 postNcr 拒绝 RETURN，无人工替代入口） | 新增 | open |  |
| P3-CK-qa-016 | P3 | D3 | ck-quality.md | escalateToRecall 终态死胡同——翻 ESCALATED_TO_RECALL 但不建召回事件，此后 upgradeToRecall 被 IN_REVIEW 守卫永久拒绝 | 新增 | open |  |
| P3-CK-qa-017 | P3 | D7 | ck-quality.md | enforceGate check-then-create 无并发防护——同单并发流转双建 PENDING 质检单（P0-CK-mfg-001 同型核查结论：非同型，qa 为重复创建） | 新增 | open |  |
| P3-CK-qa-018 | P3 | D5/D8 | ck-quality.md | recordResult 的 posted 三件套只写 posted 不写 postedAt/postedBy——与 passInspection/failInspection 的 markPosted 路径不一致 | 新增 | open |  |
| P3-CK-qa-019 | P3 | D4/D1 | ck-quality.md | 死配置常量 + batch.xml 内 `java.time.LocalDate.now()`——`erp-qa-spc-sampling-cron`/`spc-capability-cron`/`spc.enabled` 零消费；实际门控为 nop.job.* 双层键 | 新增 | open |  |
| P3-CK-qa-020 | P3 | D9，同型 P3-CK-mfg-014 家族 | ck-quality.md | dashboard/report 全实体载入内存聚合——期间全量检验单/NCR + 全量非完成 CAPA 内存过滤 + distinct chart 全载 | 新增 | open |  |
| P3-CK-qa-021 | P3 | D2/D10，同型全域族 | ck-quality.md | currentUserId/resolveUserId 宽 catch 返回 null 无日志 + findExistingSpcNcr 静默吞幂等预检失败 | 新增 | open |  |
| P3-CK-qa-022 | P3 | D6 | ck-quality.md | NCR-CAPA 报表严重度排序误用 RECALL 字典常量（MEDIUM ≠ NORMAL）——NORMAL 行落入无序尾区；resolvedNcrCount 把 CANCELLED 计入（与 javadoc 不符） | 新增 | open |  |
| P3-CK-qa-023 | P3 | D8 | ck-quality.md | notifyCustomers 仅簿记不派发——「客户通知」无任何系统通知通道（notify 子系统未接入），notifyCustomer=true 纯声明 | 新增 | open |  |
| P3-CK-qa-024 | P3 | D10/D5，带复用注记 P2-RC-040 | ck-quality.md | InspectionTemplateMatcher 对 materialId=null 的调用退化为「任意 active 模板 limit 1（最旧 id）」——无物料业务单据复制到无关模板行 | 新增 | open |  |
| P3-CK-qa-025 | P3 | D6/D10 | ck-quality.md | NCR quantity 兜底 ONE——lotQuantity/sampleQuantity 均空时拒收数量记 1；SPC 失控 NCR quantity 恒 1 | 新增 | open |  |
| P1-CK-hr-001 | P1 | D8 | ck-hr-org.md | 部门/职位删除无任何引用守卫——含在职员工的部门/有编制的职位删除后 Employee/Recruitment/子部门悬挂引用静默产生（子部门提升为根节点、树结构静默重组） | 新增 | open |  |
| P2-CK-hr-002 | P2 | D8 | ck-hr-org.md | 调动跨组织时员工 orgId 不随目标部门同步——orgId 与部门失配并沿续签合同扩散 | 新增 | open |  |
| P2-CK-hr-003 | P2 | D9/D8，同型 P2-CK-fin2-007/P2-CK-mfg-007 orgId 族（并入） | ck-hr-org.md | findDepartmentTree 双表全量加载 + 硬编码 limit 5000 静默截断 + 无 orgId 隔离 + empCount 无雇佣状态口径 | 新增 | open |  |
| P2-CK-hr-004 | P2 | D5/D3，同型 P1-CK-pur-003 族 | ck-hr-org.md | 通用 CRUD update 无状态守卫——HIRED 招聘单/终态合同/员工雇佣状态（可直写 RESIGNED 绕过状态机前提）可被通用 mutation 改写 | 同型 P1-CK-pur-003 族 |fixed | F1.3：统一基类接入自动生效（同型 P1-CK-pur-003 族，全域守卫惰性/激活由列存在性决定） |
| P2-CK-hr-005 | P2 | D8 | ck-hr-org.md | hire 联动不校验招聘单 departmentId/positionId 存在性——悬挂组织引用直接写入新员工主数据 | 新增 | open |  |
| P3-CK-hr-006 | P3 | D10 | ck-hr-org.md | hire/transferEmployee 关键日期参数 null 时 NPE 而非业务错误（GraphQL NonNull 已拦生产面，直接 Java 调用暴露） | 新增 | open |  |
| P3-CK-hr-007 | P3 | D5/D6 | ck-hr-org.md | renew 无 newEndDate 边界校验——到期日可改为早于生效日/缩短/清空（部分由下轮 job 自愈、区间倒序永久驻留） | 新增 | open |  |
| P3-CK-hr-008 | P3 | D3/D6，需求分歧只登记不裁决 | ck-hr-org.md | renew 实现语义与 UC-HR-07 分歧（就地延长 vs 设计「新建合同+原合同 EXPIRED」）+「连续合同次数到无固定期限提示」未实现 | 新增 | open |  |
| P3-CK-hr-009 | P3 | D5，同族注记 P3-CK-mfg-012 | ck-hr-org.md | 招聘 mutation 入参无边界——makeOffer 负薪资直通合同月薪、scheduleInterview 的 interviewerId/date 无校验 | 新增 | open |  |
| P3-CK-hr-010 | P3 | D5/D10 | ck-hr-org.md | targetSuperiorId 无存在性/自引用/成环校验——直接上级链可造环或指向不存在员工 | 新增 | open |  |
| P3-CK-hr-011 | P3 | D1 | ck-hr-org.md | BizModel 与 per-mutation Processor 双份死代码副本（legacy dup，StateMachine javadoc 自证）——双源漂移风险 | 新增 | open |  |
| P3-CK-hr-012 | P3 | D4，同型 P3-CK-mfg-013/inv-021/sal-023/qa-019 家族 | ck-hr-org.md | 合同到期 job cron 三层键漂移——启用需同时配 3 个键，只配 nop.job.enabled 时 execute 永远 skip（INFO 日志易忽略） | 同型 cron 键漂移家族 | open |  |
| P3-CK-hr-013 | P3 | D2 | ck-hr-org.md | ContractExpiryJob 顶层失败仅 LOG.error 无告警通道——到期扫描连续失败静默（合规敏感场景） | 新增 | open |  |
| P3-CK-hr-014 | P3 | D9 | ck-hr-org.md | countReferences 五类引用统计各 findAllByQuery().size() 全实体加载计数 | 新增 | open |  |
| P3-CK-hr-015 | P3 | D5/D8 | ck-hr-org.md | UC-HR-01「证件号码重复提示」未实现——idCardNo（及 socialSecurityNo）无任何唯一性校验 | 新增 | open |  |
| P3-CK-hr-016 | P3 | D6/D10 | ck-hr-org.md | hire 联动新员工 gender 硬编码 "MALE" + 中文姓名拆分假设——入职主数据占位值直落档案 | 新增 | open |  |
| P3-CK-hr-017 | P3 | D3，需求分歧只登记不裁决 | ck-hr-org.md | hire 员工初始 employmentStatus=ACTIVE 与 recruitment.md §6.3「PROBATION」分歧——三份 owner doc 互相矛盾，实现恒 ACTIVE 无入参 | 新增 | open |  |
| P1-CK-mnt-001 | P1 | D3/D5 | ck-maintenance.md | `validateNotConfirmed` 是空守卫——confirm 可作用于任意状态：CANCELLED 终态可复活为 ACTIVE+posted=true，单据状态对已回滚的库存/GL 撒谎 | 新增 | open |  |
| P1-CK-mnt-002 | P1 | D2/D7，同型 P1-CK-mfg-005 族 | ck-maintenance.md | reverseConfirm 红冲 GL/反向库存两步失败吞异常后仍推进 CANCELLED+posted=false 终态——重试入口被守卫永久封死，悬挂不可恢复且无告警 | 新增 | open |  |
| P1-CK-mnt-003 | P1 | D3/D8 | ck-maintenance.md | `restoreToRunning` 无条件恢复覆盖设备当前状态——取消未启动 visit 强改设备 RUNNING、visit/downtime 交叉时互相抹掉对方临时态，污染运行时长聚合与排产门控 | 新增 | open |  |
| P2-CK-mnt-004 | P2 | D6/D8 | ck-maintenance.md | 看板「本期维护访问数」按 `visit.businessDate` 过滤，但两类生成器建 visit 均不写 businessDate（xmeta 无默认值）——自动生成的 visit 完成后 KPI 恒不计入 | 新增 | open |  |
| P2-CK-mnt-005 | P2 | D4/D2 | ck-maintenance.md | due-visit 日批循环无 per-schedule 失败隔离 + job 失败仅 LOG.error 无告警无重试——一条 poison 计划每天中断全批且不可观测 | 新增 | open |  |
| P2-CK-mnt-006 | P2 | D5/D3，同型 P1-CK-pur-003 族 | ck-maintenance.md | 通用 CRUD update/delete 无单据状态守卫——全 15 实体裸 CrudBizModel，CANCELLED 消耗单/COMPLETED 访问/已结束停机/状态日志行均可直接改写 | 同型 P1-CK-pur-003 族 |fixed | F1.3：统一基类接入自动生效（同型 P1-CK-pur-003 族，全域守卫惰性/激活由列存在性决定） |
| P2-CK-mnt-007 | P2 | D7/D2，同型 P2-CK-inv-012/P1-CK-pur-002/P2-CK-mfg-011/P2-CK-ast-015 族 | ck-maintenance.md | visit complete 链 REQUIRES_NEW 工时凭证先于主事务末步提交——restoreToRunning/completeLinkedRequest 失败回滚主事务留下孤儿 MAINTENANCE_LABOR 凭证 | 新增 |fixed | F1.2：restoreToRunning+completeLinkedRequest 前移至 doComplete（工时凭证）前 + 幂等 warn 降 info；mnt 全量绿 |
| P2-CK-mnt-008 | P2 | D8，同型 P2-CK-fin2-007/P2-CK-mfg-007 族 | ck-maintenance.md | Dashboard/报表全部查询无 orgId 过滤——多组织部署下 KPI 跨组织混算；job 生成 visit orgId 恒 null | 新增 | open |  |
| P2-CK-mnt-009 | P2 | D5 | ck-maintenance.md | 备件消耗 confirm 入参边界缺失——warehouseId 可空出库、行数量无正数校验（负出库=加库存）、时长无负值守卫 | 新增 | open |  |
| P3-CK-mnt-010 | P3 | D9 | ck-maintenance.md | OEE fleet 聚合 N+1——每设备 8+ 查询的嵌套循环，看板每次刷新 O(N×8) 次 DAO 往返 | 新增 | open |  |
| P3-CK-mnt-011 | P3 | D8/D4 | ck-maintenance.md | 设计声明的「DRAFT 访问产生 TODO 提醒维护主管」未落地——防滞留机制缺失 | 新增 | open |  |
| P3-CK-mnt-012 | P3 | D4，同型 P3-CK-mfg-013/P3-CK-inv-021/P3-CK-sal-023 家族 | ck-maintenance.md | cron 键三层门控漂移——`erp-mnt.due-visit-cron` 的值从不被当作 cron 表达式消费（仅判空），实际节奏在 nop.job 键且默认非空 | 新增 | open |  |
| P3-CK-mnt-013 | P3 | D6/D10 | ck-maintenance.md | 停机/OEE 计算边界四项——重叠停机双计、quality 可 >100%、报表日期右边界含次日零点、开放窗口含 startTime null 草稿行 | 新增 | open |  |
| P3-CK-mnt-014 | P3 | D5/D1 | ck-maintenance.md | `changeStatus` 无字典成员校验——任意字符串直写 status 列并同步污染状态日志 | 新增 | open |  |
| P3-CK-mnt-015 | P3 | D10/D2，同型 P3-CK-mfg-016 全域族 | ck-maintenance.md | reportAdditionalFault 空描述断链 + toLongUserId 宽 catch 静默 null | 新增 | open |  |
| P3-CK-mnt-016 | P3 | D6，同型 P2-CK-inv-010/P3-CK-mfg-021 族 | ck-maintenance.md | 两 PostingEvent 汇率硬编码 ONE——issue 侧 ledger 币种非本位币时凭证错配 | 新增 | open |  |
| P3-CK-mnt-017 | P3 | D9 | ck-maintenance.md | 报表/看板无界加载 + `setLimit(5000)` 截断致逾期误报 | 新增 | open |  |
| P3-CK-mnt-018 | P3 | D8 | ck-maintenance.md | visit cancel 对已确认备件消耗零动作 + 报修-访问一对一硬绑定——作废语义未裁决的两组残留 | 新增 | open |  |
| P3-CK-mnt-019 | P3 | D7，复用注记 P1-MA2-086 | ck-maintenance.md | 并发幂等的 UK 兜底因 orgId=null 失效——`(code, orgId)` UK 对 NULL orgId 不去重（多数 DB NULL≠NULL） | 新增 | open |  |
| P1-CK-hr2-001 | P1 | D6 | ck-hr-attendance-payroll.md | 出勤折算分母恒为硬编码 22 且有薪假按缺勤扣薪——不足 22 个打卡日的月份全员基本工资被折减、带薪年假双路径扣款 | 新增 | open |  |
| P1-CK-hr2-002 | P1 | D8 | ck-hr-attendance-payroll.md | generateBankFile 批量发放翻 PAID 不触发 SALARY_PAYMENT(280) 凭证——银行文件路径发放的薪酬应付职工薪酬永不冲减 | 新增 | open |  |
| P1-CK-hr2-003 | P1 | D6/D10 | ck-hr-attendance-payroll.md | runPayroll 无逐员工失败隔离——任一员工缺合同/社保基数/税务配置即整批回滚，与 UC-HR-04「跳过并告警」明确分歧 | 新增 | open |  |
| P1-CK-hr2-004 | P1 | D6 | ck-hr-attendance-payroll.md | SocialInsuranceCalculator 完全忽略 effectiveFrom/effectiveTo 有效期——年调后同城同险种多行配置重复计扣、基数历史行任取 | 新增 | open |  |
| P1-CK-hr2-005 | P1 | D6 | ck-hr-attendance-payroll.md | getComparison 对比视图的合计/扣款五行恒返回 0——SALARY_ITEM_CODES 含 5 个派生字段 readSalaryField 不处理 | 新增 | open |  |
| P2-CK-hr2-006 | P2 | D7/D8 | ck-hr-attendance-payroll.md | tryPostPayment 无去重守卫叠加凭证 REQUIRES_NEW 先行提交——markPaid 外层回滚后重试产生重复 280 发放凭证 | 新增 |fixed | F1.2：tryPostPayment 补 alreadyPosted 去重守卫（镜像计提链）；TestErpHrSalaryPostingChain 守卫短路断言 |
| P2-CK-hr2-007 | P2 | D5，同型 P1-CK-pur-003 族——并入 P2-CK-hr-004 家族 hr2 站点 | ck-hr-attendance-payroll.md | salary/attendance/leave/simulation 全实体通用 CRUD 无守卫——ErpHrSalary__update_ 可直写 approveStatus=APPROVED 绕过整条计提链、直写 paymentStatus=PAID 绕过 280 凭证 | 同型 P1-CK-pur-003 族 | open |  |
| P2-CK-hr2-008 | P2 | D5 | ck-hr-attendance-payroll.md | 休假余额守卫对「无余额记录」静默跳过——与 getBalance 同数据相反结论（守卫放行 vs 余额显示负数） | 新增 | open |  |
| P2-CK-hr2-009 | P2 | D5/D8 | ck-hr-attendance-payroll.md | 日期重叠守卫仅同假别——跨假别同期休假可双双 APPROVED 且排班 leaveRequestId 互相覆盖致先批休假的取消联动失效 | 新增 | open |  |
| P2-CK-hr2-010 | P2 | D7 | ck-hr-attendance-payroll.md | ErpHrSalary 无 (employeeId, year, month) UK + existsNonVoidSalary 前置检查 TOCTOU——并发核算产生同员工同月重复薪酬行 | 新增 | open |  |
| P2-CK-hr2-011 | P2 | D6 | ck-hr-attendance-payroll.md | findActiveContract 名不副实——无 status 过滤无排序，多合同员工（调动旧约+新约/历史合同）任取一行作核算基数 | 新增 | open |  |
| P2-CK-hr2-012 | P2 | D6 | ck-hr-attendance-payroll.md | applyBatchAdjustment 四类调整全部写入 basicSalary——ALLOWANCE（津贴调整）语义错置，调津贴实际调了基本工资 | 新增 | open |  |
| P2-CK-hr2-013 | P2 | D9 | ck-hr-attendance-payroll.md | computeAllEmployeeSims 每员工 5+ 查询全量重算——四个汇总/异常 @BizQuery 每次打开 N×6 次查询；runPayroll 同型 N+1 | 新增 | open |  |
| P2-CK-hr2-014 | P2 | D5 | ck-hr-attendance-payroll.md | generateRotation 活跃排班口径仅 SCHEDULED 且新建无 UK flush-catch——PRESENT/ABSENT 日被视为空位，插入命中 UK 抛未翻译原始异常 | 新增 | open |  |
| P3-CK-hr2-015 | P3 | D3 | ck-hr-attendance-payroll.md | shift-assignment-status dict CANCELLED 无 writer（死状态）+ 旧 6 值 salary-approval-status dict 成孤儿声明 | 新增 | open |  |
| P3-CK-hr2-016 | P3 | D4/D2，同型 P3-CK-hr-012/013 家族 | ck-hr-attendance-payroll.md | LeaveApproverTimeoutJob cron 三层键漂移 + 顶层失败仅 LOG.error 无告警通道 | 新增 | open |  |
| P3-CK-hr2-017 | P3 | D10 | ck-hr-attendance-payroll.md | TaxBracketParser 空/畸形税率表致 resolveBracket 裸 IOOBE、坏 rate 值致 NPE——配置数据错误未翻译为业务错误码；latest 月 cumulativeData null 时累计静默重置 | 新增 | open |  |
| P3-CK-hr2-018 | P3 | D6/D8 | ck-hr-attendance-payroll.md | 加班费率/加班阈值/月工作日硬编码——README 声明的三个配置点无 accessor 无消费 | 新增 | open |  |
| P3-CK-hr2-019 | P3 | D1，同型 P3-CK-hr-011 | ck-hr-attendance-payroll.md | 五个 BizModel 保留 processor 化前完整 legacy dup 死代码副本——双源漂移风险面扩大 | 新增 | open |  |
| P3-CK-hr2-020 | P3 | D3，需求分歧只登记不裁决 | ck-hr-attendance-payroll.md | leave REJECTED 无重新提交边——「修改后重新提交」只能新建申请，与 salary 审批轴 REJECTED→SUBMITTED 处理不对称 | 新增 | open |  |
| P3-CK-hr2-021 | P3 | D5/D10 | ck-hr-attendance-payroll.md | computeWorkHours 无负值守卫 + 手工补卡无未来时间校验——负工时可落库并进入加班/出勤聚合 | 新增 | open |  |
| P3-CK-hr2-022 | P3 | D8 | ck-hr-attendance-payroll.md | leave 联动三处小漂移：onLeaveCancelled 无条件置 SCHEDULED、approve 清空转派 approverId、resolveApproverId 恒 null | 新增 | open |  |
| P3-CK-hr2-023 | P3 | D6/D10 | ck-hr-attendance-payroll.md | 休假期间边界三处：跨月休假整单漏算无薪扣减、跨年休假全额计入起始年余额、endDate<startDate 得 0 天可提交 | 新增 | open |  |
| P3-CK-hr2-024 | P3 | D5/D7 | ck-hr-attendance-payroll.md | 排班调换 submit/approve 边界：无同日期校验（跨日交换语义未定义）+ 同 assignment 多 PENDING 双批准无守卫 | 新增 | open |  |
| P3-CK-hr2-025 | P3 | D7 | ck-hr-attendance-payroll.md | 休假余额并发扣减竞态——approve 读派生 used 无锁，两笔并发 approve 均过校验超额审批 | 新增 | open |  |
| P3-CK-hr2-026 | P3 | D6/D8 | ck-hr-attendance-payroll.md | convertToFormal 部分冲突清单静默丢弃 + 个税跳档告警为有效税率近似且仅检升档 | 新增 | open |  |
| P1-CK-crm-001 | P1 | D6/D8 | ck-crm-lead.md | getTerritoryPipeline 管道三段聚合口径混乱——公司级实际/预测段只计 territoryId IS NULL 的记录 + 实际段无 leadType 过滤（convertToCustomer 链同笔生意双计）+ 实际/预测段无期间过滤 | 新增 | open |  |
| P1-CK-crm-002 | P1 | D6/D8 | ck-crm-lead.md | 漏斗聚合 won/lost 期间口径系统性失真——doLose/markLeadConverted 不写 ConvLog，期间内唯一动作是丢失/转化的线索从该期漏斗中消失；totalWon/totalRevenue 无 leadType 过滤双计 | 新增 | open |  |
| P2-CK-crm-003 | P2 | D8 | ck-crm-lead.md | convertToCustomer 新建商机不透传 territoryId/campaignId——转化后商机在区域管道/区域预测中失明 | 新增 | open |  |
| P2-CK-crm-004 | P2 | D6 | ck-crm-lead.md | QuotaRollupCalculator.rollup 层级覆盖语义按平面求和——中间层显式覆盖值与其子明细行并存时上级聚合双计 | 新增 | open |  |
| P2-CK-crm-005 | P2 | D5/D6 | ck-crm-lead.md | distributeAnnualQuota 无重复分配幂等守卫 + 均分尾差不守恒——重复调用生成重复子行使聚合翻倍，Σ子行≠年度总额且无末行吸收 | 新增 | open |  |
| P2-CK-crm-006 | P2 | D9/D8 | ck-crm-lead.md | LeadDuplicateChecker 每次 lead save 全表加载全部非终态线索实体到内存逐一匹配 + 无 orgId 隔离 | 新增 | open |  |
| P2-CK-crm-007 | P2 | D5/D3，同型 P1-CK-pur-003 族 | ck-crm-lead.md | CRUD update 无已审守卫——Lead docStatus/leadType/stageId 可经 update_ 直改绕过状态机与阶段方向守卫；负 expectedRevenue/越界 probability 无校验 | 同型 P1-CK-pur-003 族 |fixed | F1.3：统一基类接入自动生效（同型 P1-CK-pur-003 族，全域守卫惰性/激活由列存在性决定） |
| P2-CK-crm-008 | P2 | D8 | ck-crm-lead.md | Territory 删除仅查子节点守卫——不查 Lead/AssignmentRule/Quota 引用，删叶子区域后悬挂引用静默产生 | 新增 | open |  |
| P3-CK-crm-009 | P3 | D8 | ck-crm-lead.md | moveTerritory 不维护 isLeaf 冗余字段——新父节点仍标叶子、原子树移空后旧父节点仍标非叶 | 新增 | open |  |
| P3-CK-crm-010 | P3 | D9/D8 | ck-crm-lead.md | findOpportunityBoardData 商机硬编码 limit 200 静默截断 + 双查询均无 orgId 过滤 | 新增 | open |  |
| P3-CK-crm-011 | P3 | D6 | ck-crm-lead.md | loadConvLogs 期间上界 off-by-one——le(periodEnd+1day 00:00) 把次日零点整的流转计入本期（跨期毫秒级双计） | 新增 | open |  |
| P3-CK-crm-012 | P3 | D10 | ck-crm-lead.md | AssignmentRule conditionValue 坏 JSON 时异常冒泡——一条配置损坏阻塞此后全部线索创建（auto-assign 默认开启） | 新增 | open |  |
| P3-CK-crm-013 | P3 | D6 | ck-crm-lead.md | computeDaysInStage 忽略仍停留中的线索——avgDaysInStage 只统计已离开阶段的记录，在停 lead 的「分析期末截断」时长缺失，停留均值系统性偏低 | 新增 | open |  |
| P3-CK-crm-014 | P3 | D4，同型 cron 键漂移家族 | ck-crm-lead.md | FunnelAggregationJob 内外层 cron 键不一致——启用需同时配 3 个键，job.yaml description 声明的门控键与 trigger 实际消费键不同 | 新增 | open |  |
| P3-CK-crm-015 | P3 | D2/D4/D7 | ck-crm-lead.md | FunnelAggregationJob 顶层失败仅 LOG.error 无告警通道 + retention-period-months 死配置 + job 直调路径 refreshFunnel 无事务原子性 | 新增 | open |  |
| P3-CK-crm-016 | P3 | D5 | ck-crm-lead.md | assignLead/reassignLead 无终态守卫 + reassignLead 三 ID 无存在性校验 | 新增 | open |  |
| P3-CK-crm-017 | P3 | D1 | ck-crm-lead.md | getCreatedOpportunity 只读查询标注 @BizMutation——查询走写事务路径 | 新增 | open |  |
| P3-CK-crm-018 | P3 | D10/D9 | ck-crm-lead.md | countCompletedEvents leadId=null 退化为全系统事件计数 + findAllByQuery().size() 计数模式 | 新增 | open |  |
| P3-CK-crm-019 | P3 | D10/D5 | ck-crm-lead.md | 查重「默认仅提示」实际零提示——checkAndNotify 返回的候选列表被调用方丢弃，无日志无事件 | 新增 | open |  |
| P1-CK-crm2-001 | P1 | D6 | ck-crm-cpq-forecast.md | countConsecutiveOverdueSteps 前向扫描把已按期完成的步骤计为逾期——按期推进到第 3 步以后的序列进度系统性误报逾期并派发提醒 | 新增 | open |  |
| P1-CK-crm2-002 | P1 | D6/D5 | ck-crm-cpq-forecast.md | PriceRuleEngine 完全忽略 productCategory/customerCategory 维度——类别限定规则退化为全局规则，对所有产品/客户错误匹配定价 | 新增 | open |  |
| P2-CK-crm2-003 | P2 | D6/D8，关联 P1-CK-crm-001/002 | ck-crm-cpq-forecast.md | 预测准确率局部实现——实际关闭段按 expectedCloseDate 代理口径而非转化时间、closePeriod 不做期末终刷、team/territory/company 维度准确率行永不生成 | 新增 | open |  |
| P2-CK-crm2-004 | P2 | D4/D6 | ck-crm-cpq-forecast.md | ForecastRecalcJob 每次运行只重算一个 OPEN 期间（setLimit(1) 无排序）——设计要求「重算所有 OPEN 期间」，月度+季度并行期间下部分期间永不被刷新 | 新增 | open |  |
| P2-CK-crm2-005 | P2 | D5/D8 | ck-crm-cpq-forecast.md | advanceStep 不校验事件与进度的线索归属——任一其他线索的已完成事件可推进本线索序列步骤 | 新增 | open |  |
| P2-CK-crm2-006 | P2 | D5/D8 | ck-crm-cpq-forecast.md | generateQuote 无 lead 类型/状态守卫且无条件覆盖弱指针——可作用于任意状态线索并破坏原 lead 的 CRM_LEAD 反查指针；raw dao 更新绕过 Lead BizModel 更新钩子 | 新增 | open |  |
| P2-CK-crm2-007 | P2 | D3/D4/D8 | ck-crm-cpq-forecast.md | 营销活动状态机与 ROI 计算零实现——ORM 无 status/campaignType/revenue 列、无任何迁移 mutation、无 endDate 自动结束 job、无 ROI 公式（设计-实现分歧登记不裁决） | 新增 | open |  |
| P2-CK-crm2-008 | P2 | D4/D8 | ck-crm-cpq-forecast.md | 序列自动分配流程未接线——auto-assign-on-qualify 死配置（默认 true 零消费）+ assignSequence 无匹配时抛错与设计「不分配序列」语义相悖 | 新增 | open |  |
| P2-CK-crm2-009 | P2 | D8，同型 orgId 隔离族 | ck-crm-cpq-forecast.md | 切片全部聚合/规则加载查询无 orgId 过滤——跨组织数据混入预测、序列分配、CPQ 定价与三张报表 | 新增 | open |  |
| P2-CK-crm2-010 | P2 | D7 | ck-crm-cpq-forecast.md | LeadSequenceProgress 无 (leadId, status) 唯一键——并发 assign/switch 产生双活跃进度行；Forecast 缺设计声明的 period×维度 UK（并发重复面本体复用 P2-MA4-013(a)） | 新增 | open |  |
| P2-CK-crm2-011 | P2 | D2/D7/D4，同型 P3-CK-crm-015 族 | ck-crm-cpq-forecast.md | ForecastRecalcJob 直调 biz 无事务包装（清旧+重建非原子）+ 顶层失败仅 LOG.error 无告警通道 | 新增 | open |  |
| P2-CK-crm2-012 | P2 | D5/D3，同型 P1-CK-pur-003/P2-CK-crm-007 族 | ck-crm-cpq-forecast.md | 切片实体 CRUD update 无已审守卫——ForecastPeriod 状态/期间可经 update_ 直改绕过 closePeriod/freeze 语义 | 同型 P1-CK-pur-003 族 |fixed | F1.3：统一基类接入自动生效（同型 P1-CK-pur-003 族，全域守卫惰性/激活由列存在性决定） |
| P3-CK-crm2-013 | P3 | D6 | ck-crm-cpq-forecast.md | ownerTeam putIfAbsent 首见优先 + loadOpportunities 无排序——owner 商机跨团队时团队归属不确定，团队 rollup 口径漂移 | 新增 | open |  |
| P3-CK-crm2-014 | P3 | D8 | ck-crm-cpq-forecast.md | 声明字段/死配置族——Forecast.currencyId/expectedClosedRevenue、Accuracy.calculatedBy、Period.isCurrent、Sequence.isDefault、Step.isMandatory 永不写入/消费；3 个 CPQ/序列 config 键零消费 | 新增 | open |  |
| P3-CK-crm2-015 | P3 | D4，同型 cron 键漂移家族 | ck-crm-cpq-forecast.md | forecast-recalc + sequence-overdue 两个 job.yaml 内外层 cron 键不一致——启用需同时配 3 键，description 声明的门控键与 trigger 实际消费键不同 | 新增 | open |  |
| P3-CK-crm2-016 | P3 | D9 | ck-crm-cpq-forecast.md | clearPeriodForecasts 逐 forecast 查行删 N+1 + loadActivePriceRules 全表 active 规则加载后内存过滤 | 新增 | open |  |
| P3-CK-crm2-017 | P3 | D10，同型 P3-CK-crm-012 族 | ck-crm-cpq-forecast.md | 坏配置冒泡——SequenceAssignmentEngine 坏 JSON 与 ProductConfigRuleEngine 坏表达式无隔离，单条损坏配置阻塞整个 assign/报价生成操作 | 新增 | open |  |
| P3-CK-crm2-018 | P3 | D6 | ck-crm-cpq-forecast.md | getSequencePerformance 度量语义漂移——stepDropOffRate 实为序列级跳过率（非设计步骤级流失率）；avgCompletionDays 整天截断 | 新增 | open |  |
| P3-CK-crm2-019 | P3 | D5 | ck-crm-cpq-forecast.md | 序列 mutation 无 lead 终态守卫 + 空步骤序列可分配但永不可完成 + switchSequence 错误码语义漂移 + loadDefaultRule 无排序任取 | 新增 | open |  |
| P3-CK-crm2-020 | P3 | D6 | ck-crm-cpq-forecast.md | quotation code 毫秒时间戳并发碰撞 + currencyId 缺失误用 ERR_CPQ_NO_PRICE_MATCHED 错误码 | 新增 | open |  |
| P1-CK-cs-001 | P1 | D6/D8 | ck-cs.md | matchAndAttachSla 权益级 SLA 覆盖被策略匹配无条件覆写 + deadline 与 slaPolicyId 来源错配 + 无 hours/days 策略 NPE | 新增 | open |  |
| P1-CK-cs-002 | P1 | D6 | ck-cs.md | CSAT/NPS 聚合分母包含未响应调查（评分 null 按 0 填充）——看板与报表双站点均分系统性偏低，E2E 数值断言无法捕获 | 新增 | open |  |
| P1-CK-cs-003 | P1 | D8/D6 | ck-cs.md | 目录建单（createFromCatalog）永不计算 SLA deadline——目录工单全链路无 SLA 计时/超时升级/违约统计；权益扣减在特定配置下双计；编号绕过 TK 月序列且毫秒碰撞 | 新增 | open |  |
| P2-CK-cs-004 | P2 | D3/D8 | ck-cs.md | 重开（reopen）后 SLA 升级链断裂——isSlaCompleted 不重置，超时扫描把重开工单永久排除 | 新增 | open |  |
| P2-CK-cs-005 | P2 | D5 | ck-cs.md | matchAndAttachSla 手动 mutation 无幂等守卫——重复调用重复匹配并重复扣减权益 | 新增 | open |  |
| P2-CK-cs-006 | P2 | D4/D2 | ck-cs.md | cs.entitlement-expiry / cs.fulfillment-approval-request 通知模板种子缺失——生产种子下权益到期提醒与履行审批请求 notify 全部静默丢弃 | 新增 | open |  |
| P2-CK-cs-007 | P2 | D2/D4 | ck-cs.md | CSAT 提醒 job 无去重无终态——「一次提醒」「7 天后不再提醒」双违背 + 同一调查 REMINDER/EXPIRED 双发 + 扫描集合无界 | 新增 | open |  |
| P2-CK-cs-008 | P2 | D7/D8 | ck-cs.md | ErpCsSurvey 无 DB 唯一键 + reopen 清理 limit(1)——并发/重复调查时误发问卷且清理不全 | 新增 | open |  |
| P2-CK-cs-009 | P2 | D3/D6 | ck-cs.md | 履行链 UPDATE_STATUS 直改 setStatus 绕过 resolve/close 副作用与守卫——无 duration/isSlaCompleted/endDateTime/survey，close 超时原因守卫被旁路 | 新增 | open |  |
| P2-CK-cs-010 | P2 | D5/D3 | ck-cs.md | 重新分派路径缺失——assign 仅 NEW 守卫，ASSIGNED 态改派无 mutation 载体；assignedToId 无存在性校验 | 新增 | open |  |
| P2-CK-cs-011 | P2 | D8/D9，同型 orgId 隔离族 | ck-cs.md | cs 站点 orgId 过滤缺失 + 聚合查询无界全量加载 | 新增 | open |  |
| P2-CK-cs-012 | P2 | D5，同型 P1-CK-pur-003 族 | ck-cs.md | CRUD update 无已审守卫——ticket status/deadline/isSlaCompleted、survey respondedAt/score、timer session status、entitlement usedTickets 等可经 update_ 直改 | 同型 P1-CK-pur-003 族 |fixed | F1.3：统一基类接入自动生效（同型 P1-CK-pur-003 族，全域守卫惰性/激活由列存在性决定） |
| P2-CK-cs-013 | P2 | D6 | ck-cs.md | SLA 聚合口径三处污染——deadline null → isSlaCompleted=true 计入达标分子；null → breached 桶；报表 totalTickets 不过滤 CLOSED | 新增 | open |  |
| P2-CK-cs-014 | P2 | D4，同型 cron 键漂移家族 | ck-cs.md | 6 个 cs job 内外层 cron 键不一致——启用需同时配 3 个键，description 声明的门控键与 trigger 实际消费键不同 | 新增 | open |  |
| P3-CK-cs-015 | P3 | D5 | ck-cs.md | close 超时原因守卫被 remark 复用稀释——resolve 的 resolution 文本即可满足「注明超时原因」 | 新增 | open |  |
| P3-CK-cs-016 | P3 | D10 | ck-cs.md | CsatReminderJob 通知上下文 customerName 错填 ticket.getSubject()——提醒通知渲染数据错位 | 新增 | open |  |
| P3-CK-cs-017 | P3 | D4/D9 | ck-cs.md | 质量升级重试 PENDING 行扫描窗饿死——updateTime desc + limit 200 + Java 前缀过滤，老 PENDING 行被新成功行永久挤出 | 新增 | open |  |
| P3-CK-cs-018 | P3 | D5 | ck-cs.md | 计时器 pause/resume/stop 无属主校验——任意登录用户可操作他人会话 | 新增 | open |  |
| P3-CK-cs-019 | P3 | D6 | ck-cs.md | {agent_name} 系统变量填充 userId 而非 displayName——预设应答渲染口径与 owner doc 漂移 | 新增 | open |  |
| P3-CK-cs-020 | P3 | D5/D6 | ck-cs.md | submitSurvey 全空评分仍标记 COMPLETED + 调查链接有效期（30 天/expire-days）不校验 | 新增 | open |  |
| P3-CK-cs-021 | P3 | D10 | ck-cs.md | SurveyTokenGenerator 令牌前缀误用类名字面量——"ErpCsConstants-" 前缀无业务语义且长度贴边 | 新增 | open |  |
| P3-CK-cs-022 | P3 | D1/D4 | ck-cs.md | findSlaWarnings 在 @BizQuery 内派发通知副作用且无 warningSentAt 去重 | 新增 | open |  |
| P1-CK-ct-001 | P1 | D6/D8 | ck-contract.md | PERIOD_END runAccrual 非幂等——重复运行把期间发票全额重复累加进返利基数并重复计提 | 新增 | open |  |
| P1-CK-ct-002 | P1 | D8 | ck-contract.md | 返利贷项发票被后续 runAccrual 当作负数发票重复计入——已结算返利反噬累计基数（双重扣减） | 新增 | open |  |
| P1-CK-ct-003 | P1 | D6 | ck-contract.md | tier 区间上界排他 vs 设计「截止数量/金额（含）」——边界命中落空：返利整档归零、量折扣静默回退原价 | 新增 | open |  |
| P1-CK-ct-004 | P1 | D10/D5 | ck-contract.md | 发票生成路径对 nullable 合同数据无守卫——独立协议结算/无物料合同行触发时撞 NOT NULL 列裸崩 | 新增 | open |  |
| P2-CK-ct-005 | P2 | D3 | ck-contract.md | approveTermination 无合同状态再守卫——PENDING 期间合同经 amend/expire 漂移后，法务批准可把任意状态（含 EXPIRED 终态）改成 TERMINATED | 新增 | open |  |
| P2-CK-ct-006 | P2 | D3 | ck-contract.md | rejectAmend 无「amend 来源」判别——全新 DRAFT 合同可直接一键 ACTIVE，绕过审批链/签署/法务全部门控 | 新增 | open |  |
| P2-CK-ct-007 | P2 | D3/D5 | ck-contract.md | 手工 expire 无 endDate 到达守卫——未到期合同可提前 EXPIRED，成为绕过 terminate 法务门控的捷径 | 新增 | open |  |
| P2-CK-ct-008 | P2 | D2/D4 | ck-contract.md | doc-auto-purge 批量销毁路径必死——job 上下文无用户，purgeOverdueDocuments 逐条撞 admin 角色守卫全部隔离跳过 | 新增 | open |  |
| P2-CK-ct-009 | P2 | D7/D8 | ck-contract.md | webhook eventId 幂等只记最后一个且复用 remark 列——旧事件重放穿透 + completed 重放 500 | 新增 | open |  |
| P2-CK-ct-010 | P2 | D5 | ck-contract.md | RebateTier 零边界/零重叠校验（裸 stub）——负返利率、>100% 返利、倒挂带、重叠带全部静默入账；协议日期倒序无校验 | 新增 | open |  |
| P2-CK-ct-011 | P2 | D9 | ck-contract.md | 查询面三处全量加载 + N+1——triggerDuePlans 全表扫后 Java 过滤；PROGRESSIVE 计提 O(N²) 重载；搜索/到期查询无界 | 新增 | open |  |
| P2-CK-ct-012 | P2 | D8，同型 orgId 隔离族 | ck-contract.md | 计提明细/签章请求 orgId 不透传 + 返利发票聚合无 orgId 过滤 + orgId=null 削弱发票 UK 并发兜底 | 新增 | open |  |
| P2-CK-ct-013 | P2 | D5/D3，同型 P1-CK-pur-003 族 | ck-contract.md | CRUD update 无已审守卫——全实体裸面：合同终态复活、审批记录直改 APPROVED、计提 isSettled/金额直改、协议 status 直改 | 同型 P1-CK-pur-003 族 |fixed | F1.3：统一基类接入自动生效（同型 P1-CK-pur-003 族，全域守卫惰性/激活由列存在性决定） |
| P2-CK-ct-014 | P2 | D4，同型 cron 键漂移家族 | ck-contract.md | erp-ct-approval-timeout job 双键漂移——启用需配外层 cron-expr + enabled + 内层 bean-cron 三键；另 1 死配置常量 | 新增 | open |  |
| P2-CK-ct-015 | P2 | D4/D2 | ck-contract.md | 到期分级提醒与审批超时升级无去重载体——同档窗口内每日重复派发 | 新增 | open |  |
| P2-CK-ct-016 | P2 | D6/D8 | ck-contract.md | 返利链多币种零折算——exchangeRate 硬编码 ONE，源币种金额直接混合累加，owner doc 业务规则 3 未实现 | 新增 | open |  |
| P2-CK-ct-017 | P2 | D6 | ck-contract.md | runAccrual 期间上界不钳制 agreement.endDate——协议期外发票计入本期，连续协议同一发票双计提 | 新增 | open |  |
| P3-CK-ct-018 | P3 | D1 代码质量 | ck-contract.md | BizModel 层大块死代码复制——R6.7 Processor 迁移后遗留双份实现，漂移风险 | 新增 | open |  |
| P3-CK-ct-019 | P3 | D5 | ck-contract.md | not-found 复用错误码族——实体不存在抛「非法迁移/已开票/未激活」语义错误码 | 新增 | open |  |
| P3-CK-ct-020 | P3 | D5/D7 | ck-contract.md | generateInvoicePlansByTerm 幂等查重 check-then-insert 无 UK 兜底 + amount 无非负校验 | 新增 | open |  |
| P3-CK-ct-021 | P3 | D10/D8 | ck-contract.md | settlement 空集/负净额边界——0 额贷项照开、负 total 生成正向「贷项」、悬挂 agreement 无发票却 POSTED | 新增 | open |  |
| P3-CK-ct-022 | P3 | D5 安全 | ck-contract.md | webhook HMAC 密钥硬编码仓库常量且不可配置——签章链（可触发合同 signVersion 生效）鉴别基础弱 | 新增 | open |  |
| P3-CK-ct-023 | P3 | D4/D9 | ck-contract.md | approval-timeout 扫描窗无排序饿死——SCAN_LIMIT 200 无 addOrderField，超量 PENDING 时子集不确定 | 新增 | open |  |
| P3-CK-ct-024 | P3 | D4 | ck-contract.md | 5 个死配置键——rebate-enabled/rebate-auto-settle（owner doc 承诺默认 true 自动结算）/rebate-accrual-method/settlement-mode/progressive-retro-topup 零消费 | 新增 | open |  |
| P1-CK-b2b-001 | P1 | D8，跨 D6 | ck-b2b.md | AsnLine.materialId 全链零 writer——webhook 入站把代码映射结果写入 remark 而非 materialId：逐行物料匹配与超量校验对 webhook 路径为死代码 + createReceiveFromAsn 对 webhook 路径必抛守卫错（自动收货链末端断裂），集成测试经直 seed 遮蔽 | 新增 | open |  |
| P2-CK-b2b-002 | P2 | D2 | ck-b2b.md | 解析失败错误路径的 EdiDoc 防重键塌缩为 (formatId, ASN_INBOUND, NULL)——同格式第二次解析失败被误判 ALREADY_PROCESSED：真实解析错误被掩蔽 + 后续失败报文 rawPayload 审计永久丢失 | 新增 | open |  |
| P2-CK-b2b-003 | P2 | D8，orgId 隔离族写侧形态 | ck-b2b.md | EdiDoc/Asn/EdiLog 全链 orgId 零 writer——R1.28 webhook 并发幂等的 UK 兜底因 NULL 不去重而失效 + 24h 上线监控 org 锚点与 Doc orgId 永不相交（有 orgId 的伙伴监控恒盲区） | 新增 | open |  |
| P2-CK-b2b-004 | P2 | D4 | ck-b2b.md | `erp-b2b.enabled` 主开关定义后全域零消费——owner doc 以「config-gated OFF 默认」作为 EDI 出站自动化整体 Deferred 的首要论据，代码层不存在该门 | 新增 | open |  |
| P2-CK-b2b-005 | P2 | D3 | ck-b2b.md | retry 无方向守卫——入站 ERROR Doc 经 retry 进入 TO_SEND（出站生命周期），state machine 提供的 retryInboundTargetStatus() 零调用 | 新增 | open |  |
| P2-CK-b2b-006 | P2 | D5 | ck-b2b.md | 入站数量零边界校验——负数/零/非数值数量全程穿透：匹配不设防 → 收货草稿可落负数量行（任务点名「负数量 ASN 行」核查确认缺失） | 新增 | open |  |
| P2-CK-b2b-007 | P2 | D3，跨 D6 | ck-b2b.md | ASN 部分收货（1:N 入库）未实现——首次建库即终态 + 行级已收计数器零载体 + partial-receipt-enabled config 零消费（plan 勾选与实现不符） | 新增 | open |  |
| P2-CK-b2b-008 | P2 | D6，跨 D3 | ck-b2b.md | matchPurchaseOrder 数量校验粒度缺陷——同物料多 ASN 行不聚合剩余量 + 超额仅写 remark 文本「blockingLevel=WARN」而无一实体落 WARN | 同型 P1-CK-pur-003 族 |fixed | F1.3：统一基类接入自动生效（同型 P1-CK-pur-003 族，全域守卫惰性/激活由列存在性决定） |
| P2-CK-b2b-009 | P2 | D5，同型 P1-CK-pur-003 族 | ck-b2b.md | CRUD update 无已审守卫——EdiDoc state/retryCount/error、Asn status、PartnerProfile status 可经 update_ 直改，9 个 stub BizModel 无任何守卫 | 新增 | open |  |
| P3-CK-b2b-010 | P3 | D1 | ck-b2b.md | `LocalDateTime.now()` 直读系统时钟——CoreMetrics 时间可控约定违例（checker R7 盲区变体，冻结钟测试失效点） | 新增 | open |  |
| P3-CK-b2b-011 | P3 | D5 | ck-b2b.md | action-auth 注册面不一致——createOutbound/createInbound/promoteToTesting/promoteToCertified 4 个 mutation 无 FNPT 注册（同 BizModel 兄弟 mutation 均注册） | 新增 | open |  |
| P3-CK-b2b-012 | P3 | D10 | ck-b2b.md | not-found 误用非法迁移错误码 + markError 日志方向启发式错标入站为出站 | 新增 | open |  |
| P3-CK-b2b-013 | P3 | D3 | ck-b2b.md | blockingLevel ORM defaultValue="10" 与 dict INFO/WARN/ERROR 不符——CRUD 直存路径落无效字典值 | 新增 | open |  |
| P3-CK-b2b-014 | P3 | D5 | ck-b2b.md | webhook 不校验伙伴档案状态与生效性——SUSPENDED/TERMINATED 伙伴仍可推送建 ASN；payload 无大小上限（错误码已定义未用） | 新增 | open |  |
| P3-CK-b2b-015 | P3 | D6 | ck-b2b.md | matchPurchaseOrder 缺日期校验与通知链 + createReceiveFromAsn vendorId 来源与 owner doc 漂移 | 新增 | open |  |
| P3-CK-b2b-016 | P3 | D9 | ck-b2b.md | createOutbound 仅取首个适用 Provider + parseToAsn 逐行查映射表 N+1 | 新增 | open |  |
| P1-CK-drp-001 | P1 | D6，跨 D8 | ck-drp.md | 净需求公式可用量双计——currentStock 取 available（已扣 reserved+locked），allocatedQty 再加回 reserved：reserved 计两次、locked 多扣一次，净需求系统性虚高 → 过量补货 | 新增 | open |  |
| P1-CK-drp-002 | P1 | D6，跨 D8 | ck-drp.md | 在途调拨量不过滤 DONE 完成单——已完成调拨货物已入 currentStock 仍永久计入 onOrderQty：供给双计 → 净需求系统性低估，DRP 释放的调拨单自身完成后即成为永久幻影供给（释放闭环自噬） | 新增 | open |  |
| P1-CK-drp-003 | P1 | D6，跨 D8 | ck-drp.md | 未到货采购量不过滤收货仓库与组织——仓库 A 的净需求计入发往仓库 B 的在途采购：跨仓在途污染 → 欠补；orgId 亦未过滤（跨组织混算） | 新增 | open |  |
| P2-CK-drp-004 | P2 | D3，跨 D8 | ck-drp.md | resetToDraft 行级联与设计偏差——仅清 SUGGESTED 行：①APPROVED 行在 DRAFT 计划下仍可释放（释放无 plan 状态守卫，终点靠 advanceToExecuted 断言兜底回滚、错误码误导）；②重跑后新行 lineNo 从 10 起与存续行重复；③totalReplenishmentQty 置 null 抹掉已释放事实 | 新增 | open |  |
| P2-CK-drp-005 | P2 | D5，跨 D3 | ck-drp.md | approveLine 单行批准不回填 approvedQty——正建议量行经 approveLine + releaseLine 生成 0 数量调拨单/采购单（P2-RC-069「0 值释放」家族的加重控制点：不再限于 net≤0 行） | 新增 | open |  |
| P2-CK-drp-006 | P2 | D6，跨 D8 | ck-drp.md | 安全库存三级优先链（override > calculated > parameter）未接入 runDrp——DrpEngine 直接读 ErpDrpParameter.safetyStock，findEffectiveSafetyStock 生产零消费（仅 BizQuery 暴露） | 新增 | open |  |
| P2-CK-drp-007 | P2 | D9 | ck-drp.md | 聚合器 N+1 + 全表扫描——每个参数行触发 5 次查询，其中未到货采购量每次全表加载非作废采购订单、在途调拨每次加载全仓调拨单：P 个参数 ≈ P×全表 | 新增 | open |  |
| P2-CK-drp-008 | P2 | D8 | ck-drp.md | 跨域读查询零 orgId 过滤——stock balance/调拨单/采购单/出库移动四类查询均不滤组织（plan.orgId 只过滤参数与预测头）：多组织部署跨组织混算（orgId 隔离族 drp 读侧站点） | 新增 | open |  |
| P2-CK-drp-009 | P2 | D6 | ck-drp.md | 安全库存月度需求序列不零填充——仅聚合「有出库记录的月份」，零需求月永不参与均值/标准差（设计 KEEP 语义未落地），σ 低估 → 安全库存建议偏低 | 新增 | open |  |
| P2-CK-drp-010 | P2 | D3，跨 D5 | ck-drp.md | 仿真结果计划混入正式计划面——runSimulation 产出真实 ErpDrpPlan（COMPUTED + SUGGESTED 行），可被标准 approvePlan/releaseApproved 直接审批释放：绕过 promoteToFormalPlan 通道 → 与转正计划双份补货 | 新增 | open |  |
| P2-CK-drp-011 | P2 | D2 | ck-drp.md | 仿真参数解析器进程级缓存永不失效——ErpDrpSimulationParamResolver.cache 无 TTL 无失效钩子，invalidateCache 仅测试调用：场景参数经 CRUD 修改后仿真永远用旧值 | 新增 | open |  |
| P2-CK-drp-012 | P2 | D6，同型 P1-CK-mfg2-003 | ck-drp.md | nextVersionNo 升序取最小 versionNo+1——同场景第 3 次仿真运行 versionNo 与既有 v2 撞 UK_DRP_SCENARIO_VERSION_SCN_VER（未翻译约束异常中断） | 同型 P1-CK-mfg2-003 族 | fixed | F2.6：SimulationDrpEngine.nextVersionNo 改 DESC 取最大 versionNo（同批 P1-CK-mfg2-003）；TestErpDrpSimulation#testThirdVersionRunSucceeds（第 3 次 versionNo=3）红→绿 |
| P2-CK-drp-013 | P2 | D5，跨 D8 | ck-drp.md | ErpDrpParameter 无业务唯一键——同物料同仓多参数行物理可能：聚合器逐行独立生成补货行（重复补货）+ requireParameter/findParameter limit(1) 任取一行；ErpInvDrpSafetyStockCalc 同缺 (material,warehouse,org) UK | 新增 | open |  |
| P2-CK-drp-014 | P2 | D5，同型 P1-CK-pur-003 族 | ck-drp.md | CRUD update 无已审守卫——DrpLine.status/approvedQty/orderBillCode、DrpPlan.status/totalReplenishmentQty、SafetyStockCalc.overrideSafetyStock 可经 update__ 直改，终态复活/绕过状态机 Bean 全旁路 | 同型 P1-CK-pur-003 族 |fixed | F1.3：统一基类接入自动生效（同型 P1-CK-pur-003 族，全域守卫惰性/激活由列存在性决定） |
| P3-CK-drp-015 | P3 | D2，跨 D4 | ck-drp.md | confirmWriteback Processor 死读 auto-writeback 配置 + forecast-horizon 配置定义零消费且默认值与 owner doc 漂移 | 新增 | open |  |
| P3-CK-drp-016 | P3 | D6 | ck-drp.md | STATISTICAL 法零提前期静默产出 SS=0/ROP=0——DEFAULT_REPLENISHMENT_LEAD_TIME_DAYS=0 兜底无告警（ERR_DRP_PARAMETER_LEAD_TIME_INVALID 已定义零使用） | 新增 | open |  |
| P3-CK-drp-017 | P3 | D3 | ck-drp.md | 空计划/全取消计划滞留 APPROVED——approvePlan 零行不拒、cancelLine 不触发 advance、all.isEmpty() 早退三者叠加：EXECUTED 不可达（可 resetToDraft 恢复） | 新增 | open |  |
| P3-CK-drp-018 | P3 | D5 | ck-drp.md | 22 个命名 mutation 零 FNPT 注册——手写 action-auth 仅菜单资源，生成基线 44 FNPT 只覆盖 CRUD 操作：test profile 非管理员可 update__ 明细却不可 runDrp/approvePlan（deny-by-default 内部不一致） | 新增 | open |  |
| P3-CK-drp-019 | P3 | D6，跨 D4 | ck-drp.md | 仿真 LEAD_TIME 参数变体三处就绪零消费——dict/UK/resolveLeadTimeOverride 全在，SimulationDrpEngine fork 只消费 SAFETY_STOCK/REPLENISHMENT_QTY：用户配置 LEAD_TIME 覆盖静默无效 | 新增 | open |  |
| P3-CK-drp-020 | P3 | D10 | ck-drp.md | not-found 误码族 + O-11 错误码群零使用——requireVersion 报 ALREADY_PROMOTED、requireScenario 报 NO_BASELINE_PLAN、requireCalc 报 METHOD_UNSUPPORTED；另有 8 个已定义错误码全域零使用 | 新增 | open |  |
| P1-CK-log-001 | P1 | D8 | ck-logistics.md | onDelivered 的 post 返回 null（幂等命中）分支不 markSettled——DELIVERED 运单运费结算永久悬挂 PENDING 且无告警（P1-CK-fin-003 logistics 传导站点） | 新增 |fixed | 并入 F1.1 引擎层修复（幂等命中返回 id → markSettled 生效），prj 传导回归佐证 |
| P1-CK-log-002 | P1 | D4/D2，B1 族 | ck-logistics.md | G4 告警闭环的两个事件模板种子缺失——log.gateway-dead-letter 与 log.freight-posting-failure 经 notify 静默跳过，死信/过账失败悬挂不可感知 | 新增 | open |  |
| P2-CK-log-003 | P2 | D5 | ck-logistics.md | webhook HMAC 密钥取承运商公开编码 carrier.getCode()——签名校验无真实认证强度，凭证基础设施（CarrierConfig.credentials/apiSecret）零消费 | 新增 | open |  |
| P2-CK-log-004 | P2 | D8 | ck-logistics.md | findShipmentByTrackingNo 不按 carrierId 收窄——跨承运商运单号碰撞时 A 承运商 webhook 推进 B 承运商运单并触发其运费过账 | 新增 | open |  |
| P2-CK-log-005 | P2 | D8 | ck-logistics.md | 交付状态回写无数量 rollup——单一 Shipment DELIVERED 直接把销售订单 deliveryStatus 写成 DELIVERED，覆盖 sales 出库审核的 PARTIAL 语义 | 新增 | open |  |
| P2-CK-log-006 | P2 | D2 | ck-logistics.md | completeShipment 重试循环仅 catch NopException——真实网关非 Nop RuntimeException 逃逸：无网关日志、无死信标记、无告警，事务直接回滚 | 新增 | open |  |
| P2-CK-log-007 | P2 | D3/D6 | ck-logistics.md | 爽约后「优先重新预约权」死机制——markMissed 后 findActiveByShipment 仍返回 MISSED 预约，重新 book 被 ERR_LOG_BOOKING_DUPLICATE 阻断；priorityScore 全域零消费 | 新增 | open |  |
| P2-CK-log-008 | P2 | D6 | ck-logistics.md | 窗口生效期校验以 today 为基准而非 bookedDate——预约日期在窗口失效期外仍可预约（与星期校验的 bookedDate 基准不一致） | 新增 | open |  |
| P2-CK-log-009 | P2 | D5，同型 P1-CK-pur-003 族 | ck-logistics.md | CRUD update 无已审守卫——Shipment status/freightSettlementStatus/trackingNo、Carrier gatewayId/isActive、CarrierConfig credentials 等可经 update_ 直改 | 新增 | open |  |
| P3-CK-log-010 | P3 | D4 | ck-logistics.md | README 配置点 4 键零消费——erp-log.enabled / async-dispatch / log-retention-days / gateway-timeout-secs 声明后无任何 main 代码读取 | 新增 | open |  |
| P3-CK-log-011 | P3 | D3/D10 | ck-logistics.md | 杂项：posted 列零 writer 但列表页展示（恒 false）+ freightSettlementStatus 创建无 PENDING 初始化（NULL）+ not-found 误用非法迁移错误码 + parsePayload 无类型防御 | 新增 | open |  |
| P1-CK-aps-001 | P1 | D6/D10 | ck-aps.md | loadPendingOrders 按 earliestStartDateT 过滤排除 NULL——主建单路径不写 earliestStartDateT，有展望期的排产方案对自动工序整体漏排且无任何冲突报告 | 新增 | open |  |
| P1-CK-aps-002 | P1 | D7/D6 | ck-aps.md | run() 全量排产不感知既有 PLANNED 工序时段/产能预留——引擎时间轴仅含维护约束，增量排产的 persist 预留 pre-check 必然冲突并整轮回滚 | 新增 | open |  |
| P2-CK-aps-003 | P2 | D8 | ck-aps.md | 产能预留生命周期不完整——cancel()/complete()/updateSchedule()/doDispatch() 均不释放预留，ghost 预留行只增不减并持续收缩可排产能 | 新增 | open |  |
| P2-CK-aps-004 | P2 | D5/D6 | ck-aps.md | updateSchedule（甘特拖拽 mutation）零产能校验、零状态守卫、零预留同步——设计规则 8「拖拽需后端产能校验」无实现但 mutation 已暴露 | 新增 | open |  |
| P2-CK-aps-005 | P2 | D6 | ck-aps.md | 前向排产排序键 (priority, latestEndDateT, sequence) 下同工单优先级乱序时前置工序约束失效——后序工序可先于前序工序排定（finish-before-start 倒挂） | 新增 | open |  |
| P2-CK-aps-006 | P2 | D4/D2 | ck-aps.md | 三个告警事件模板种子缺失——aps.workorder-no-routing / aps.operation-workcenter-missing / aps.dispatch-material-shortage 经 notify 静默跳过 | 新增 | open |  |
| P2-CK-aps-007 | P2 | D4 | ck-aps.md | WorkOrderScanJob biz 直调无 runInSession 包裹——与同模块 AutoDispatchJob 自述范式相悖，job 路径实体写入缺会话载体 | 新增 | open |  |
| P2-CK-aps-008 | P2 | D5，同型 P1-CK-pur-003 族 | ck-aps.md | CRUD update 无已审守卫——OperationOrder status/plannedStartDateT/machineId 可经 update_ 直改，绕过 13 边状态机与预留同步 | 新增 | open |  |
| P3-CK-aps-009 | P3 | D4 | ck-aps.md | README 配置点 4 键零消费——default-scheduling-mode / priority-rule / time-bucket-minutes / auto-reschedule-on-insert 声明后 main 代码零读取 | 新增 | open |  |
| P3-CK-aps-010 | P3 | D4，同型 cron 键漂移家族 | ck-aps.md | 两个 job.yaml 的 cronExpr 键与 bean 内层空值跳过键不同名——`<key>.cron-expr` vs `<key>-cron` | 新增 | open |  |
| P3-CK-aps-011 | P3 | D10/D8 | ck-aps.md | 杂项：buildTimelines 对约束空时间无守卫（NPE）+ 物料齐套/ATP 聚合无 orgId 过滤（跨组织口径混合）+ ATP 公式与设计 §7.1 漂移 | 新增 | open |  |
| P1-CK-notify-001 | P1 | D7/D2 | ck-notify.md | 外发通道在调用方事务内、通知持久化之前派发——EMAIL/SMS 不可逆副作用先于 commit 与 saveEntity 执行，与 README 自述「txn().afterCommit」不符 | 新增 | open |  |
| P1-CK-notify-002 | P1 | D4/D2 | ck-notify.md | 模板种子与已接线事件类型系统性失同步——8+ 个已派发事件无 ACTIVE 模板且无任何对账机制，静默跳过设计使漂移不可见（cs 先例已实际发生） | 新增 | open |  |
| P2-CK-notify-003 | P2 | D6 | ck-notify.md | 频控合并不覆盖外发通道——merge 命中后仍对同一实例再次发送邮件/短信，每次合并增量外发一次 | 新增 | open |  |
| P2-CK-notify-004 | P2 | D5/D10 | ck-notify.md | markRead 输入边界——不存在通知也写孤儿已读行 + 无接收人身份校验（任意登录用户可按 id 标记他人通知已读） | 新增 | open |  |
| P2-CK-notify-005 | P2 | D5 | ck-notify.md | EMAIL/SMS 通道无收件地址载体——EmailMessage/SmsMessage 只设 subject/text，收件人解析只产出 userId，启用外发后邮件无法投递 | 新增 | open |  |
| P2-CK-notify-006 | P2 | D5，同型 P1-CK-pur-003 族 | ck-notify.md | CRUD update 无已审守卫——Template status（DRAFT↔ACTIVE）可直改、Notification 实例可裸改 | 新增 | open | 同型注记：Template 用 status 列非 approveStatus，守卫不激活（列名探测设计使然，归 F2.15 裁决） |
| P3-CK-notify-007 | P3 | D9 | ck-notify.md | unreadOf/countUnread/markAllRead 全量加载——countUnread 加载全部实体仅取 size；notIn 大集合无上限 | 新增 | open |  |
| P3-CK-notify-008 | P3 | D6 | ck-notify.md | renderTemplate 缺失 key 静默空串 + toCamelKey 宽松匹配——模板与 context 键漂移渲染为空洞通知且不可见 | 新增 | open |  |
| P2-CK-common-001 | P2 | D2，跨 D10 | ck-common-app.md | MaskAuditRecorder ThreadLocal 去重集无请求级清理——线程池复用线程上跨请求审计抑制，保密字段披露事件静默漏记 | 新增 | open |  |
| P3-CK-common-002 | P3 | D5 | ck-common-app.md | 聚合菜单含「中国本地化」死菜单——3 个 FLUX 页面 URL 指向不存在的模块页面，admin 角色可见可点 | 新增 | open |  |
| P3-CK-common-003 | P3 | D5 | ck-common-app.md | action-auth 聚合头注释声称合并 job 菜单但未聚合——nop-job 管理 UI 在 app 中整体缺席，注释与 x:extends 清单失同步 | 新增 | open |  |
| P3-CK-common-004 | P3 | D10 | ck-common-app.md | defaultNotFoundException 只绑 bizObjId 不绑 bizObjName——67 站点错误消息渲染字面量 `{bizObjName}` 占位符 | 新增 | open |  |
| P3-CK-common-005 | P3 | D6 | ck-common-app.md | DashboardUtil.buildGroupByQuery 生产零调用 + `additionalFilters` 参数被静默忽略——死代码携带无过滤全表聚合陷阱 | 新增 | open |  |
| P3-CK-common-006 | P3 | D1 | ck-common-app.md | DashboardUtil.safeDivide 使用废弃 `BigDecimal.ROUND_HALF_UP` 整型常量 | 新增 | open |  |
| P3-CK-common-007 | P3 | D10 | ck-common-app.md | StringMaskFormat 短值边界近全泄漏——len=6 证件号揭示 5/6 字符、len=8 手机号揭示 7/8 字符 | 新增 | open |  |
| P3-CK-common-008 | P3 | D3 | ck-common-app.md | 9 个文件残留「共享骨架 reverseApprove→SUBMITTED 为已确认 live 缺陷」陈旧 javadoc——骨架已于 2026-08-14 修复为 REJECTED，注释反向误导修复阶段 | 新增 | open |  |
| P3-CK-common-009 | P3 | D10 | ck-common-app.md | UniqueConstraintHelper 把 ERR_SQL_DATA_INTEGRITY_VIOLATION 一并判为唯一约束冲突——非 UK 完整性违例可被误译为「重复记录」友好错误 | 新增 | open |  |
| P3-CK-common-010 | P3 | D1 | ck-common-app.md | ErpCommonErrors 错误码命名空间 `nop.err.erp.common.*` 偏离项目 `erp.err.<short>` 约定 | 新增 | open |  |

## 阶段状态

- 检查阶段（M0-M8）：进行中——**已闭合（2026-08-26）**：28/28 检查工作项 done + C8.3 独立收官审计通过（agent_72a36cff，证据抽验 6/6 真实、索引计数吻合、零代码改动确认）。
- 修复阶段（MF/MV/MG）：**进行中**——F0-F2.3 done（**45 findings 终态**：44 fixed + mfg-011 部分注记）。F2.4 ready。open 余 488。
