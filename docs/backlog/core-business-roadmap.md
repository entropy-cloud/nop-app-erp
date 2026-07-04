# Core Business Logic Roadmap

> 最后更新：2026-07-04
> 本路线图覆盖**进销存+财务** 5 域的自定义 BizModel 方法、跨域编排、业财过账。
> 前置条件：`crud-roadmap.md` 中对应域的 CRUD 已完成。

## Work Item Status

> 状态在工作项上；Milestone 仅为分组。各工作项的实施记录见 Implementation Order。

### Milestone M1 — 核心业务循环
- 1.0a 采购申请审批→转订单逻辑：`done`
- 1.0b 销售报价单审批→转订单逻辑：`done`
- 1.1 Purchase Order BizModel：`partial`
- 1.2 Sales Order BizModel：`partial`（审批/信用额度 + 出库触发段 done；过账/发票/收款仍 todo，归 1.7）
- 1.3 StockMove BizModel：`done`
- 1.4 三单匹配逻辑：`done`
- 1.5 过账 Provider：`done`
- 1.6 采购到付款串联：`partial`（AP 段 PO→Receive→Invoice→Pay + 三单匹配 + 域级核销 done，计划 0300-1；**财务辅助账 ErpFinArApItem 生成 + 正式核销单 ErpFinReconciliation + 往来余额/账龄 done，计划 0300-3**；自动核销/退货归 0300-3 Non-Goal / 1.9）
- 1.7 销售到收款串联：`partial`（AR 段 SO→Delivery→Invoice→Receipt + AR_INVOICE/RECEIPT 过账 + 域级核销 done，计划 0300-2；**财务辅助账 + 正式核销单 + 往来余额/账龄 done，计划 0300-3**；自动核销/汇兑损益归 0300-3 Non-Goal，销售退货归 1.10）
- 1.8 费用报销/票据/资金：`done`（费用报销 + 员工借款子面 done，计划 0700-2：三实体 + 三轴审批 + EXPENSE_CLAIM/EMPLOYEE_ADVANCE/EMPLOYEE_ADVANCE_SETTLE 过账 + 员工应付/预支双方向辅助账 + 报销抵扣借款净额核销；**票据/资金子面 done，计划 1000-1：五实体（应收/应付票据/贴现/授信/现金预测）+ 7 态状态机 + 授信强一致校验 + 贴现科目分解五件套 + NOTES_RECEIVABLE/PAYABLE 七类型过账 + 票据收到/背书经核销单联动 AR/AP + 现金预测 refreshForecast 聚合 + 红字冲销**）
- 1.9 采购退货与退款：`done`（计划 0456-1：退货单三轴审批状态机 + 库存反向出库 + PURCHASE_RETURN 红字冲减凭证 + DIRECTION_PAYABLE 负 openAmount 辅助账回减应付；红字发票自动生成/换货/批次退货/现金退款归 Non-Goal）
- 1.10 销售退货与退款：`done`（计划 0456-2：退货单三轴审批状态机 + 库存反向入库 + SALES_RETURN 反向 SALES_OUTPUT 凭证（借存货/贷成本）+ DIRECTION_RECEIVABLE 负 openAmount 辅助账回减应收 + 已收款退货反向收款核销；红字发票自动生成/退款方式路由/换货/退货质检/批次退货归 Non-Goal）
- 1.11 批次追溯链：`done`（计划 0700-1：移动单自追溯上链 originMoveId/originReturnedMoveId + 四类追溯查询 forward/backward/return/batch + 退货移动单透传挂链）

### Milestone M4 — 业财一体端到端
- 4.3 期末结账全流程：`done`（计划 1000-3；含 4.3 前置「存货成本核算」done 计划 1538-1：记账器策略分派 MOVING_AVERAGE/FIFO + ErpInvCostLayer FIFO 队列 + period-close step2 接线 IErpInvCostingBiz.reclosePeriodCosts）
- 4.1/4.2/4.4：`done`（计划 1018-1：4.1/4.2 扩展既有 P2P/O2C E2E 断言财务核销层——AP_INVOICE/AR_INVOICE/PAYMENT/RECEIPT 过账生成辅助账 + ErpFinReconciliation 核销 openAmount 归零 + 账龄；4.4 新增采购/销售退货到退款连续链 E2E——退货审核→反向库存→红字过账→负 openAmount 辅助账回减→cancelOnReverse 归零 + 异常路径）

### Milestone M5 — 业财可运维性与闭环
- 5.1 会计日志与可观测性：`done`（设计 done `finance/posting-log.md`；**plan `2026-07-04-1452-1` completed**——traceId 端到端贯穿 + 规则命中结构化日志 + 变更审计复用平台 `NopSysChangeLog`（模板/模板行 `tagSet="audit"`）+ 过账异常工作台（新增 `ErpFinPostingException` 实体，REQUIRES_NEW 独立事务落 PENDING，重试/忽略/补录三入口 ErrorCode 守门）+ 期末结账前置门控扫描未处置异常阻止结账）
- 5.2 冲销反写闭环：`done`（设计 done `finance/posting.md` §冲销机制方向二；**plan `2026-07-04-1452-2` completed**——finance 定义 `IErpFinVoucherReversedListener` SPI + `ErpFinReversalListenerRegistry`（镜像 `ErpFinAcctDocRegistry` 范式），`reverseProcess()` 红字凭证+回链+辅助账落库后构造 `VoucherReversedEvent` 派发（默认 SYNC 同事务同步通知，ASYNC 经 `txn().afterCommit`）；失败隔离 try/catch 包裹——单监听者抛错不阻断其他监听者、不回滚红字凭证（法律效力），失败落入 5.1 异常工作台；purchase/sales/inventory 三域各实现监听者按裁决 4 回退目标态表回退 `posted`+`docStatus`/`approveStatus`）
- 5.3 运行监控：`todo`（设计 done `finance/posting-log.md` §运行监控指标；**P1**——四指标自动化记账率/时延/异常率/业财闭环成功率；**关键发现：平台无 metrics API（CoreMetrics 仅时钟），`posting-log.md`"接入平台监控大盘"表述为 owner-doc 漂移，须 Phase 1 Decision 裁定真实落地路径**；**plan active `2026-07-04-1452-3`**）

> **M5 定位**：M1/M4 已交付业财打通的**功能正确性**（凭证生成、辅助账、核销、期末结账）；M5 补其**运营成熟度**——可观测（日志）、可闭环（冲销反写）、可监控（指标）。属生产级运维层，非新业务功能。落地须各自起草 `docs/plans/` 计划并经独立草案/结束审计；会计日志优先评估复用 nop-platform 审计能力（避免触及 ORM 保护区域）。

## Implementation Order

### M1 — 核心业务循环

| # | 工作项 | 域 | 设计文档 | 状态 |
|---|--------|-----|---------|------|
| 1.0a | 采购申请审批→转订单逻辑 | purchase | `purchase/requisition.md` | ✅ `done` |
| 1.0b | 销售报价单审批→转订单逻辑 | sales | `sales/quotation.md` | ✅ `done` |
| 1.1 | Purchase Order BizModel（审批/入库触发/过账） | purchase | `purchase/state-machine.md` | 🔶 `partial` |
| 1.2 | Sales Order BizModel（审批/出库触发/过账） | sales | `sales/state-machine.md` | 🔶 `partial`（审批+信用额度+出库触发 done；过账归 1.7） |
| 1.3 | StockMove BizModel（库存移动/流水/余额） | inventory | `inventory/state-machine.md` | ✅ `done` |
| 1.4 | 三单匹配逻辑（PO/Receive/Invoice） | purchase | `purchase/three-way-match.md` | ✅ `done` |
| 1.5 | IErpFinAcctDocProvider 过账 Provider | finance | `finance/posting.md` | ✅ `done` |
| 1.6 | 采购到付款端到端串联 | purchase/finance | `flow-overview.md` | 🔶 `partial`（AP 段 done 计划 0300-1；辅助账+核销+账龄 done 计划 0300-3；自动核销/退货归 0300-3 Non-Goal / 1.9） |
| 1.7 | 销售到收款端到端串联 | sales/finance | `flow-overview.md` | 🔶 `partial`（AR 段 done 计划 0300-2；辅助账+核销+账龄 done 计划 0300-3；自动核销/汇兑损益归 0300-3 Non-Goal，退货归 1.10） |
| 1.8 | 费用报销/票据/资金模块业务逻辑 | finance | `expense-claim.md`, `treasury.md` | ✅ `done`（费用报销+员工借款子面 done 计划 0700-2；票据/资金子面 done 计划 1000-1） |
| 1.9 | 采购退货与退款 | purchase/finance | `purchase/returns.md` | ✅ `done`（计划 0456-1） |
| 1.10 | 销售退货与退款 | sales/finance | `sales/returns.md` | ✅ `done`（计划 0456-2：三轴审批 + 反向入库 + SALES_RETURN 凭证 + 负 AR 辅助账回减应收 + 反向收款核销） |
| 1.11 | 批次追溯链逻辑 | inventory | `inventory/trace-chain.md` | ✅ `done`（计划 0700-1：单 uplink 自追溯链 + 四类追溯查询 + 退货透传挂链） |

### M4 — 业财一体端到端

| # | 工作项 | 涉及域 |
|---|--------|---------|
| 4.1 | 采购到付款全链路测试（PO→Receive→Invoice→Pay） | purchase/finance | ✅ `done`（计划 1018-1） |
| 4.2 | 销售到收款全链路测试（SO→Delivery→Invoice→Receipt） | sales/finance | ✅ `done`（计划 1018-1） |
| 4.3 | 期末结账全流程（成本核算→汇兑重估→结转损益→关账） ✅ | finance |

> **存货成本核算引擎（M4 前置 / 1000-3 step2 deferred 承接）**：`done` 计划 `2026-07-02-1538-1`——`StockMoveBookkeeper` 重构为按物料 `costMethod` 策略分派（`CostMethodResolver` → `MovingAverageCostingStrategy` 抽取既有逻辑行为不变 / 新增 `FifoCostingStrategy` 维护消耗 `ErpInvCostLayer` FIFO 队列、多层加权 COGS 经既有 `ledger.totalCost` 通道、红冲按加权 unitCost 追加层、首次无成本抛 `ERR_COST_NOT_AVAILABLE`）；`IErpInvCostingBiz.reclosePeriodCosts` + finance `closeInvModule` 接线 period-close §步骤2 兜底重算（finance→inventory R，config-gated）。解除 1000-3 step2 deferred。Non-Goal：BATCH/INDIVIDUAL/STANDARD/全月一次/LIFO/Landed Cost/成本调整/报表（见计划 Deferred）。
| 4.4 | 采购/销售退货到退款全链路 | purchase/sales/finance | ✅ `done`（计划 1018-1） |

### M5 — 业财可运维性与闭环

| # | 工作项 | 域 | 设计文档 | 状态 |
|---|--------|-----|---------|------|
| 5.1 | 会计日志与可观测性（规则命中追溯 / 变更审计 / 异常工作台 / traceId 串联） | finance | `finance/posting-log.md` | ✅ `done`（P0，plan `2026-07-04-1452-1` completed） |
| 5.2 | 冲销反写闭环（`VoucherReversedEvent` + 域 Provider 监听回退业务单据状态） | finance | `finance/posting.md` §冲销机制方向二 | ✅ `done`（P0，plan `2026-07-04-1452-2` completed） |
| 5.3 | 运行监控（自动化记账率 / 时延 / 异常率 / 业财闭环成功率 + 告警 SLA） | finance | `finance/posting-log.md` §运行监控指标 | ⬜ `todo`（P1，plan active `2026-07-04-1452-3`） |

## Reference

第一个 `task.xml` 文件参考：`module-purchase/erp-pur-service/src/main/resources/_vfs/erp/pur/_task/ErpPurOrder/approve.task.xml`，展示了审批流的标准编排模式（校验→规则→分支→I*Biz→后处理）。
