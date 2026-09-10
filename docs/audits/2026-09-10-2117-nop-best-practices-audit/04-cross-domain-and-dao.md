# 04 — 跨域写路径 / daoFor 反模式 / 裸 dao() / 吞异常

> 规则依据：`safe-api-reference.md`（业务代码跨实体走 I*Biz 管道；daoProvider 属底层写法）+ `processor-extension-pattern.md` 硬规则 2/3（跨域写走 Facade；Processor 内直接 dao() 仅限同聚合）+ 豁免登记 `docs/architecture/posting-exemptions.md`
> 已裁决基线站点（compliance-baseline.md 历次 baseline-raise + posting-exemptions.md 登记 + §9.4 永久只读豁免）不重复报告。

## 1. 未登记跨域 daoFor 写（应走 I*Biz Facade 或补豁免登记）

### 1.1 [blocker] projects→assets：结算回退裸写资产状态

`module-projects/erp-prj-service/.../processor/ErpPrjProjectSettlementProcessor.java:223-232` — `rollbackAssetIfNeeded` 经 `daoFor(ErpAstAsset).updateEntity` 直接回写 assets 域 status，且本地硬编码 `ASSET_STATUS_DRAFT="DRAFT"`(:58)。正向转固走 `IErpAstAssetBiz.save`:213 而逆向裸 dao——绕过资产域生命周期守卫（回退到 DRAFT 是否合法应由 assets 域裁决，不应由 projects 域直写）。
**修复**：IErpAstAssetBiz 增回退接口（如 `deactivateCard(assetCardId)`），常量改引 assets 域 Constants。

### 1.2 [major] inventory→manufacturing：成本调整直写 mfg rollup

`module-inventory/erp-inv-service/.../costing/CostAdjustmentService.java:207-241` — `publishFirmedRollup` 经 `daoFor(ErpMfgCostRollup).newEntity()+saveEntity`（头 `:211-218`、行 `:220-230`）直接写 mfg 域实体；`removeFirmedRollup`(:234-241) 直接 delete。`IErpMfgCostRollupBiz` 已存在（`module-manufacturing/erp-mfg-dao/.../biz/IErpMfgCostRollupBiz.java`）却未用。站点不在 compliance-baseline。
**修复**：注入 `IErpMfgCostRollupBiz`。

### 1.3 [major] drp→inv/pur：DrpReleaseService 跨域直建单据

`module-drp/erp-drp-service/.../drp/DrpReleaseService.java:188-200` — `daoFor(ErpInvTransferOrder/ErpInvTransferOrderLine)+saveEntity`（drp→inv 跨域写）；`:212-228` — `daoFor(ErpPurOrder/ErpPurOrderLine)+saveEntity`（drp→pur）。javadoc(:44-47) 自称「对齐 MrpReleaseService 范式」，但 `posting-exemptions.md` **无 DrpReleaseService 条目**（MrpReleaseService 在 :8-33 完整登记）。
**修复**：补登记条目或收敛为 inv/pur 域 purpose-built I*Biz（`createFromDrpLine`）。

### 1.4 [major] contract→purchase/sales：计费触发直建发票

`module-contract/erp-ct-service/.../processor/ErpCtInvoicePlanTriggerInvoiceProcessor.java:75-150` — `createApInvoiceDraft`/`createArInvoiceDraft` 经 daoFor 直建 `ErpPurInvoice/ErpPurInvoiceLine/ErpSalInvoice/ErpSalInvoiceLine`（ct→pur/sal 跨域写，绕过两域审批管道），无豁免注释登记（对照 rebate O-4 与 b2b P2-CK-b2b-017-r3 均有登记）。
**附带 [major]**：`ErpCtInvoicePlanBizModel.java:137-212` 同名两方法为**逐行复制的死代码**（triggerInvoice 已 :78-79 委托 Processor）——删除死副本即消除一半违规面。

### 1.5 [major] mfg→inventory：批次谱系直写

`module-manufacturing/erp-mfg-service/.../genealogy/BatchGenealogyWriter.java:198,210` — `batchDao().updateEntity(existing)/saveEntity(batch)` 写 `ErpInvBatch`，posting-exemptions.md 无条目。伴随 `ErpMfgBatchGenealogyBizModel.java:144` BizModel 内裸 `daoFor(ErpInvBatch)` 无豁免注释。
**修复**：补登记或经 inv 域 I*Biz。

## 2. 已登记/合规确认（不构成发现）

- mfg 领料/委外过账移动单生成走 `IErpInvStockMoveBiz.generateMove`（`MaterialIssueStockMoveBuilder.java:18-19`）✅
- assets→finance 凭证写全部经 `IErpFinVoucherBiz` Facade（`AssetPostingExecutor.java:27-33`，9 个 Dispatcher 同范式）✅
- mnt 备件出库经 `IErpInvStockMoveBiz` ✅；跨域只读豁免（SalaryPostingDispatcher / NcrPostingDispatcher / OeeCalculator / ErpPrjProjectSettlementProcessor:315 等）注释在案 ✅
- `MrpReleaseService.java:142,158` mfg→pur 写 + 委外绕审批：posting-exemptions.md:8-33 完整登记 ✅（仅命名问题见 `06`）

## 3. [major] ORM to-one 可替代的 daoFor(FK)（Type-1 回潮）

历史上两轮 Type-1 清零（plan 0605-3 / 2000-1 / 0941-1）后新回潮 6 处（均不在 baseline）：

1. `module-purchase/.../entity/ThreeWayMatcher.java:196-204` — `daoFor(ErpPurReceiveLine).getEntityById(receiveLineId)` / `daoFor(ErpPurOrderLine).getEntityById(orderLineId)`，而 `app-erp-purchase.orm.xml:790`（ReceiveLine.orderLine to-one）与 `:1130`（invoiceLine.receiveLine to-one）已声明 → 改 `line.getReceiveLine().getOrderLine()`
2. `module-inventory/.../costing/CostMethodResolver.java:57-64` — `daoFor(ErpMdMaterial).getEntityById(materialId)`，`ErpInvStockMoveLine.material` to-one 已声明（`app-erp-inventory.orm.xml:250`）→ 改 `line.getMaterial()`
3. `CostAdjustmentService.java:297` — 同文件 `:208` 已用 `line.getMaterial()`，`:297` 却 daoFor 直查（同类不一致）
4. `module-quality/.../spc/SpcSamplingService.java:157/247`（经 `:367-372 resolveInspection`）— 循环内逐条 `daoFor(ErpQaInspection).getEntityById(inspectionId)`，`ErpQaInspectionLine.inspection` to-one 已声明（`erp-qa-dao/_app.orm.xml:364-367`）→ 改 `line.getInspection()`（同时消除 N+1，见 `05` §4）

## 4. [major] BizModel 类体内裸 dao 写/读（无豁免注释）

1. `module-hr/erp-hr-service/.../entity/ErpHrShiftBizModel.java:141/147/152/157` — 类体内 `dao.saveEntity`(newAttendance) / `saveOrUpdateEntity` / `updateEntity`；类头 :42 豁免注释仅覆盖已不存在的「findExistingByDate 查询辅助」；对照同域 `ErpHrAttendanceBizModel.java:154-158` 用 CrudBizModel 管道的正确姿势 → 迁 Processor 或改走 attendanceBiz/assignmentBiz
2. `module-hr/.../entity/ErpHrSalarySimulationBizModel.java:485-486/576-577/600-601/850-851/864-865` — 裸 `daoFor(ErpHrSalary).findAllByQuery` 5 处，同实体 `IErpHrSalaryBiz` 已注入且 :385 正在使用（豁免仅限 ItemAdjustment/Employee 只读）→ 统一 salaryBiz.findList；另死方法 `recordAdjustment`:678-693 含 `dao.saveEntity/updateEntity` 裸写（属死代码，见 `06` §3）
3. `module-sales/.../entity/ErpSalOrderBizModel.java:176-179` — `daoFor(ErpSalPricingRule).findAllByQuery`（可走 findList 管道）+ `:190-203` `lineDao.saveEntity/updateEntity` 循环写 → 随 `02` §1 拆 Processor 一并治理（`createFromQuotation`:312-328 有跨聚合写契约注记且经 I*Biz 调用，属登记先例不报）
4. `module-finance/.../entity/ErpFinEmployeeAdvanceBizModel.java:156-162` — `findLatestUnreversedCashRepayLink` daoFor+findAllByQuery（随 `02` §1 Processor 化一并下移）
5. [minor] `module-assets/.../entity/ErpAstAssetBizModel.java:100-101` — `getAssetAuditTrail` 裸 dao()（R2b 基线登记站点，无豁免注释——按 (b) 类披露）
6. [minor] `module-master-data/.../entity/ErpMdMaterialSkuBizModel.java:433-434,457` — daoFor getEntityById 无 I*Biz 注入也无豁免注释（域内只读，SKU→material 有 ORM to-one 可达 category 链）——**待复核**
7. [minor] `module-notify/.../processor/ErpSysNotificationNotifyProcessor.java:48-55` — 通知落库经 daoProvider.daoFor(ErpSysNotification).saveEntity 直写绕过 IErpSysNotificationBiz 管道（见 `05` §2）
8. [minor] `module-crm/.../support/LeadActivityDerivationHelper.java:36-49` — helper 经 daoFor 直写 Lead（应为 Processor 或 Event 实体触发）
9. [minor] `module-contract/.../entity/ErpCtDocumentBizModel.java:182` — purge 用 `dao().deleteEntity(doc)` 绕过自身 CRUD delete 管道（prepareDelete 守卫与 purge 语义冲突，有意为之）→ 注释登记豁免依据

## 5. 吞异常 / 弱一致（B1 已知失败模式复发点）

1. [major] `module-finance/.../posting/EmployeeAdvancePostingDispatcher.java:112-118` + `ErpFinEmployeeAdvanceBizModel.java:94-104` — `postCashRepay` catch(Exception)→LOG.error→return false；BizModel 侧字段先更新、凭证失败仅 LOG.warn 后照常提交——settled/outstanding 已改、凭证缺失的半状态悬挂。引擎侧 recordPostFailure 在 REQUIRES_NEW 内先落 PENDING 再抛出（工作台兜底成立，**待复核**兜底链路对 cashRepay 是否可达），但 BizModel 层 warn-only 提交与 `reverseCashRepay`「先红冲后回退」强一致范式自相矛盾（javadoc 自认）
2. [minor] `module-finance/.../processor/ErpFinAccountingPeriodProcessor.java:503-505,526-529,548-551` — 期末前置检查三处 catch(Exception)→LOG.debug 跳过（意图=单域部署实体未注册安全跳过，但 catch 面过宽，真实 DB 故障被吞→前置检查静默降级后期间可能带悬挂关闭）→ 收窄 catch 类型
3. [minor] 同文件 `:189-192,217-220,245-248` — 折旧/存货成本重算集成 impl-not-ready catch(Exception)→warn+return（G3 分级注释只对 NopException rethrow）→ catch 收窄
4. 正面范式（不构成发现）：`ErpFinPostingProcessor.java:218-227`（catch→recordPostFailure→rethrow）、`ErpFinApDocumentPipelineProcessor.java:362-373,506-523`（失败独立事务落账 FAILED+轨迹）
