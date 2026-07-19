# 视图按钮需求覆盖分析 — 全局汇总

> 分析日期：2026-07-19
> 审计方法：每域独立子代理，基于 `METHODOLOGY.md` 定义的 CRUD 基线 + 设计文档推导期望按钮
> 源数据：`_tmp/view-buttons/`（23 域自动采集）、`docs/design/<domain>/ui-patterns.md`、`state-machine.md`

## 概览

| 指标 | 数量 |
|------|------|
| 总实体数 | 391 |
| 无差距实体（clean） | 313（80.0%） |
| 有 Blocker 差距的实体 | 25（6.4%） |
| 有 Major 差距的实体 | 12（3.1%） |
| 有 Minor/Info 差距的实体 | 41（10.5%） |

## 按域汇总

### 业务域（18 域，有 ui-patterns.md）

| 域 | 实体数 | Clean | Blocker | Major | Minor/Info | Clean % |
|----|--------|-------|---------|-------|------------|---------|
| master-data | 24 | 20 | 0 | 0 | 5 | 83.3% |
| purchase | 20 | 13 | 0 | 2 | 5 | 65.0% |
| sales | 16 | 11 | 0 | 0 | 6 | 68.8% |
| inventory | 21 | 13 | 3 | 4 | 0 | 61.9% |
| finance | 30 | 22 | 2 | 0 | 5 | 73.3% |
| assets | 22 | 19 | 1 | 0 | 3 | 86.4% |
| manufacturing | 28 | 26 | 1 | 1 | 1 | 92.9% |
| projects | 16 | 13 | 3 | 0 | 0 | 81.3% |
| quality | 8 | 6 | 2 | 0 | 0 | 75.0% |
| maintenance | 13 | 10 | 2 | 0 | 1 | 76.9% |
| crm | 34 | 30 | 1 | 1 | 1 | 88.2% |
| customer-service | 16 | 13 | 1 | 0 | 1 | 81.3% |
| human-resource | 36 | 28 | 1 | 0 | 5 | 77.8% |
| aps | 6 | 3 | 2 | 0 | 1 | 50.0% |
| logistics | 7 | 4 | 1 | 0 | 2 | 57.1% |
| b2b | 13 | 9 | 2 | 0 | 1 | 69.2% |
| contract | 15 | 9 | 1 | 3 | 3 | 60.0% |
| drp | 7 | 5 | 2 | 1 | 0 | 71.4% |
| **小计** | **332** | ~254 | **25** | **12** | ~40 | ~76.5% |

### 系统域（5 域，无 ui-patterns.md，纯 CRUD 基线）

| 域 | 实体数 | Clean | Blocker | Major | Minor/Info | Clean % |
|----|--------|-------|---------|-------|------------|---------|
| nop-auth | 14 | 14 | 0 | 0 | 0 | 100% |
| nop-wf | 14 | 14 | 0 | 0 | 0 | 100% |
| nop-sys | 19 | 19 | 0 | 0 | 0 | 100% |
| nop-report | 9 | 9 | 0 | 0 | 0 | 100% |
| notify | 3 | 3 | 0 | 0 | 0 | 100% |
| **小计** | **59** | **59** | **0** | **0** | **0** | **100%** |

### 全部合计

| 总计 | 实体数 | Clean | Blocker | Major | Minor/Info | Clean % |
|------|--------|-------|---------|-------|------------|---------|
| 23 域 | **391** | **~313**（~80%） | **25**（6.4%） | **12**（3.1%） | **~40**（~10%） | ~80% |

> 注：实体数与 `_tmp/view-buttons/SUMMARY.md` 的 394 有 3 差异，因部分报告排除了无页面的实体和占位页面。

## Blocker 差距清单（25 实体）

设计文档明确要求但 view.xml 完全缺失核心按钮的实体——按域分组：

| # | 域 | 实体 | 缺失的核心按钮 |
|---|----|------|---------------|
| 1 | inventory | ErpInvStockMove | submit, cancel（调拨流程阻断） |
| 2 | inventory | ErpInvStockLedger | view（流水日志不可 CRUD 编辑） |
| 3 | inventory | ErpInvStockTake | submit, approve, reject（盘点流程阻断） |
| 4 | finance | ErpFinVoucher | post（过账）, reverse（红冲） |
| 5 | finance | ErpFinAccountingPeriod | close, reverse-close（结账流程阻断） |
| 6 | assets | ErpAstAsset | move, value-adjust, dispose（资产生命周期动作） |
| 7 | manufacturing | ErpMfgWorkOrder | issue-materials, receive-finished, post-processing-fee, cancel（工单执行阻断） |
| 8 | projects | ErpPrjProject | start, hold, resume, complete, cancel（项目生命周期阻断） |
| 9 | projects | ErpPrjTask | start, complete, block, unblock（任务执行阻断） |
| 10 | projects | ErpPrjTimesheet | submit（工时提交审批阻断） |
| 11 | quality | ErpQaInspection | pass, fail, re-inspect（检验结果录入阻断） |
| 12 | quality | ErpQaNcr | investigate, resolve, verify, close, reject（不合格处理流程阻断） |
| 13 | maintenance | ErpMntVisit | schedule, start, complete, cancel（维保执行阻断） |
| 14 | maintenance | ErpMntRequest | accept, complete, reject, cancel（报修处理流程阻断） |
| 15 | crm | ErpCrmLead | qualify, convert, lose, cancel（线索→客户转化阻断） |
| 16 | customer-service | ErpCsTicket | assign, cancel, start, resolve, escalate（工单处理阻断） |
| 17 | human-resource | ErpHrTimesheet | submit（工时提交审批阻断） |
| 18 | aps | ErpApsOperationOrder | schedule, start, complete（排产执行阻断） |
| 19 | aps | ErpApsSchedule | run, publish, release（排程方案发布阻断） |
| 20 | logistics | ErpLogShipment | confirm-shipment, cancel（发运业务阻断） |
| 21 | b2b | ErpB2bEdiDoc | retry, cancel（EDI 消息处理阻断） |
| 22 | b2b | ErpB2bPartnerProfile | activate, suspend, deactivate（合作伙伴上线阻断） |
| 23 | contract | ErpCtContract | submit, approve, reject, suspend, terminate, resume（合同生命周期阻断） |
| 24 | drp | ErpDrpPlan | run-drp, approve-all, generate-order（补货计划执行阻断） |
| 25 | drp | ErpDrpLine | approve, reject（补货建议审批阻断） |

## Major 差距清单（12 实体）

设计文档有较明确要求但缺少次核心按钮，或纯 CRUD 实体配置不当：

| # | 域 | 实体 | 问题 |
|---|----|------|------|
| 1 | purchase | ErpPurRfq | 缺少整套询价流程状态按钮 |
| 2 | purchase | ErpPurQuotation | 缺少整套报价流程状态按钮 |
| 3 | inventory | ErpInvStockBalance | 库存余额只读视图仍显示 add/update/delete |
| 4 | inventory | ErpInvTransferOrder | 缺少 confirm 按钮（调拨确认） |
| 5 | inventory | ErpInvBatch | 批次只读视图仍显示 add/update/delete |
| 6 | inventory | ErpInvSerialNumber | 序列号只读视图仍显示 add/update/delete |
| 7 | manufacturing | ErpMfgWorkOrder | 缺 row-cancel-button |
| 8 | crm | ErpCrmEvent | 缺 complete/cancel 状态迁移按钮 |
| 9 | contract | ErpCtContractVersion | 缺少版本管理审批按钮 |
| 10 | contract | ErpCtRebateAgreement | 缺少审批/生效按钮 |
| 11 | contract | ErpCtSignatureRequest | 缺少签名状态流转按钮 |
| 12 | drp | ErpDrpLine | 缺 reject、cancel 按钮 |

## 系统性模式

### 模式 1：业务单据头缺 `row-cancel-button`（7 域，系统性）

purchase（ErpPurOrder 以外的 5 个头实体）、sales（5 个头实体）、inventory（StockMove）、manufacturing（WorkOrder）、maintenance（Visit/Request）、logistics（Shipment）、drp（Line）都存在此问题。ErpPurOrder 和 ErpSalOrder 是唯一正确配置了 cancel 按钮的头实体。

### 模式 2：状态机已设计但 view 层未接线（15 实体，主要 blocker 来源）

Projects、quality、maintenance、aps、contract、crm、cs、drp、b2b 的 state-machine.md 定义的状态流转在 view.xml 层完全没有落地——视图停留在 codegen 生成的 CRUD 骨架。

### 模式 3：只读展示实体不应有 CRUD 按钮（inventory 5 实体 + nop-auth 2 实体）

StockLedger、StockBalance、Batch、SerialNumber、DispatchLog 等审计/日志类实体仍暴露 add/update/delete 按钮，应转为只读或隐藏。

## 修复建议优先级

1. **P0（立即）**：补齐 25 个 blocker 实体的缺失按钮——切断业务流程的入口。
2. **P1（高）**：补齐系统性 `row-cancel-button` 缺失（~10 个实体）和 major 项中的只读实体 CRUD 暴露。
3. **P2（中）**：补齐 minor 差距（树实体验证、worker 审批流对齐）。
4. **P3（低）**：info 增强点（导出、打印、导入按钮）——属于功能增强而非缺陷。

## 按域报告

| 域 | 报告文件 |
|----|---------|
| master-data | [`master-data.md`](master-data.md) |
| purchase | [`purchase.md`](purchase.md) |
| sales | [`sales.md`](sales.md) |
| inventory | [`inventory.md`](inventory.md) |
| finance | [`finance.md`](finance.md) |
| assets | [`assets.md`](assets.md) |
| manufacturing | [`manufacturing.md`](manufacturing.md) |
| projects | [`projects.md`](projects.md) |
| quality | [`quality.md`](quality.md) |
| maintenance | [`maintenance.md`](maintenance.md) |
| crm | [`crm.md`](crm.md) |
| customer-service | [`customer-service.md`](customer-service.md) |
| human-resource | [`human-resource.md`](human-resource.md) |
| aps | [`aps.md`](aps.md) |
| logistics | [`logistics.md`](logistics.md) |
| b2b | [`b2b.md`](b2b.md) |
| contract | [`contract.md`](contract.md) |
| drp | [`drp.md`](drp.md) |
| nop-auth | [`nop-auth.md`](nop-auth.md) |
| nop-wf | [`nop-wf.md`](nop-wf.md) |
| nop-sys | [`nop-sys.md`](nop-sys.md) |
| nop-report | [`nop-report.md`](nop-report.md) |
| notify | [`notify.md`](notify.md) |
| **方法论** | [`METHODOLOGY.md`](METHODOLOGY.md) |

## 方法论说明

参见 [`METHODOLOGY.md`](METHODOLOGY.md) 获得完整的按钮推导规则、严重级别定义、Prose→Button-ID 翻译字典和审计流程约束。

## 关于审批按钮的关键说明

本审计**不将** `row-submit-button` / `row-approve-button` / `row-reject-button` 等标准审批按钮缺失计为差距，因为：

- **DIRECT 模式**（`tagSet="use-approval"`）：codegen **自动生成**这些按钮到 view.xml + 对应的 xbiz action。无需手动添加。
- **WORKFLOW 模式**（`useWorkflow="true"`）：nop-wf 引擎运行时动态注入审批按钮，view.xml 无需静态声明。
- **无审批模式**：实体本身不应有审批按钮。

所有 25 个 blocker 差距涉及的缺失按钮均为**域专用状态迁移按钮**（如 `issue-materials`、`receive-finished`、`pass/fail`、`confirm-shipment`、`start/complete` 等），codegen 无法自动生成，nop-wf 引擎无法覆盖。详见 [`METHODOLOGY.md §1.4`](METHODOLOGY.md)。

## 已知局限

1. 审计只覆盖 view.xml 中声明的 `<action>` 组件——不覆盖 edit 页面工具栏的动态条件渲染（`visibleOn` 未评估）。
2. nop-wf 审批流的按钮由工作流引擎动态注入，不经过 view.xml 声明，因此不在本审计范围内。
3. [导出]、[打印] 等操作可能通过 BizModel 完成而非 view.xml action，虽列为 info 级但实际可能可通过配置启用。
