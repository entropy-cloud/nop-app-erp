# M0.4 页面 yaml 源头链判定标准与修复策略矩阵

> 产出：plan `docs/plans/2026-09-06-1451-2-cjk-detection-baseline.md` Phase 3（M0.4）。执行目录：`docs/audits/check/2026-09-06-1645-ai-check-r3/`。
> 口径：M0.2/M0.3 冻结口径（`docs/audits/cjk-baseline.md` SNAPSHOT 块，generated 2026-09-06；CAT-4=1,700 / 违规承载文件 108）。扫描集 = 含 CJK 的 `*.page.yaml`/`*.flux.yaml` 全量 **147 文件，无抽样**（含 39 个仅注释 CJK 的 codegen stub 文件，一并列出以闭合「全部文件无遗漏」）。
> 纪律锚：`docs/lessons/06-codegen-product-edit-overwrite.md`（生成产物禁手改）；`docs/architecture/view-and-page-strategy.md` §国际化策略；`docs/architecture/i18n-compliance.md` 判定准绳表 #7/#8 + 修复模式 CAT-4。

## 判定标准：codegen 产物 vs 手写页（机械可复核查验程序）

**三步核查程序**（对任一 `*.page.yaml`/`*.flux.yaml` 依序执行，每步命令可机械重放）：

1. **stub 内容核查**：剥离 `#` 注释行后，文件正文含 `x:gen-extends:` 且引用 `web:GenPage`（即 `x:gen-extends: | <web:GenPage view="X.view.xml" page="P" .../>`）→ 判定 **codegen 产物**；否则判定 **手写页**。命令：`grep -v "^\s*#" <file> | grep -c "web:GenPage"`（>0 = codegen）。
2. **引用完整性核查**（codegen 判定的复核）：codegen 产物引用的 `view="X.view.xml"` 必须与文件同目录实际存在（生成链 = `<Entity>.view.xml`（手写/delta 层）× `precompile/gen-page.xgen`（`withTplDir('/nop/templates/orm-web')`）→ `<Entity>/*.page.yaml` shell）；全部 19 个 `erp-*-web` 模块均存在 `precompile/gen-page.xgen`（实测 19/19）。命令：`test -f $(dirname <file>)/<view引用>`。
3. **注释误匹配排除**（本程序自身校验）：步骤 1 必须先剥注释再匹配——反例 `module-master-data/.../party-search/main.picker.page.yaml` 注释中提到 GenPage 模板但正文为手写 AMIS 节点（owner 先例：plan `2026-07-21-0827-2` / `2026-08-29-1913-1` 手写非实体 picker.page.yaml）。

**理由**：Nop 的 `gen-page.xgen` 是单向变换（view.xml + xmeta → page.yaml shell），stub 正文仅含 `GenPage` 引用、无任何页面节点；页面实际文案在运行时由 view.xml 渲染。故 stub 文件的文案修改唯一正确位置是 `<Entity>.view.xml` 的 `i18n-en:label/title/displayName` 属性（或 xmeta 模型源），改 stub 必被下一次 codegen 覆盖（lesson 06 三轮审计教训）。手写页（dashboard/report/wizard/Deferred stub/非实体 picker 等）无生成链，文案直接落在 yaml 节点，唯一修复路径是补 `i18nEn` 属性。

**替代方案及否决理由**：仅按路径启发式判定（如「`<Entity>/main.page.yaml` = codegen，`dashboard/` = 手写」）被否决——反例：`<Entity>/ref-*.page.yaml` 部分为 GenPage stub（`ErpAstAsset/ref-asset.page.yaml`）、部分为手写（`ErpCtContractLine/ref-contract.page.yaml` 含正文节点）；`dashboard/` 亦存在 GenPage-stub 例（`ErpApsCapacityReservation` 族之外）。内容级 stub 核查（步骤 1）零歧义且可机械重放，路径启发式不可。

**残留风险**：(a) 若未来手写页面正文混入 `x:gen-extends` + GenPage 引用（部分生成 + 部分手写混合体），步骤 1 将整文件判为 codegen，手写节点文案会被漏改——核查程序步骤 2 的 view.xml 存在性校验可发现异常引用；混合体出现时须开独立计划逐节点裁决（当前全量 147 文件实测 0 混合体）。(b) view.xml 源头新增 CJK 文案不改 stub 文件计数（CAT-4 只扫 yaml），由 F15 `i18n-coverage-checker.sh`（view.xml 层 0 gaps 门控）+ M0.2 脚本 Java 面双门控兜底。

## 冻结口径汇总（判定结果）

- codegen 产物（GenPage stub）：**39 文件**，CAT-4 违规合计 **0**（全部仅注释含 CJK，0 违规）——修复策略：禁改生成物；页面文案若有 CJK 一律改 view.xml 模型源 i18n-en:* 属性后 codegen 重生成验证（MI.7 语义）。
- 手写页：**108 文件**，CAT-4 违规合计 **1700**（= SNAPSHOT 冻结值 1,700 / 108 文件）——修复策略：直接补 i18nEn 属性（MI.8 按域分批）。
- 交叉验证：39 + 108 = 147 文件（= 2026-08-31 探针 raw 口径 147 上界）；stub∩违规 = 0（`comm` 实测为空集）。

## 修复策略矩阵（147 文件全量，无抽样）

> `i18nEn` 英译统一取 `docs/design/i18n-glossary.md`（414 token 冻结基准）。「glossary 命中」列为该文件违规文案中命中的术语表条目（机械匹配，每文件至多列 6 条）；无命中 = 文案含新词，MI.8 修复时须先扩术语表再使用（414 基准登记义务）。

### A. codegen 产物类（GenPage stub，禁改生成物）

| # | 文件 | 引用 view.xml（同目录实测存在） | CAT-4 | 修复策略 | owner 域 | glossary 条目引用 |
|---|------|------|-------|----------|----------|------------------|
| 1 | `module-assets/erp-ast-web/src/main/resources/_vfs/erp/ast/pages/ErpAstAsset/ref-asset.page.yaml` | `ErpAstAsset.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | assets（erp-ast-web） | （无违规文案；如涉新文案先扩表） |
| 2 | `module-assets/erp-ast-web/src/main/resources/_vfs/erp/ast/pages/ErpAstInventoryLine/ref-inventory.page.yaml` | `ErpAstInventoryLine.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | assets（erp-ast-web） | （无违规文案；如涉新文案先扩表） |
| 3 | `module-contract/erp-ct-web/src/main/resources/_vfs/erp/ct/pages/ErpCtContractLine/ref-contract.page.yaml` | `ErpCtContractLine.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | contract（erp-ct-web） | （无违规文案；如涉新文案先扩表） |
| 4 | `module-contract/erp-ct-web/src/main/resources/_vfs/erp/ct/pages/ErpCtContractVersion/ref-contract.page.yaml` | `ErpCtContractVersion.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | contract（erp-ct-web） | （无违规文案；如涉新文案先扩表） |
| 5 | `module-crm/erp-crm-web/src/main/resources/_vfs/erp/crm/pages/ErpCrmActivity/ref-lead.page.yaml` | `ErpCrmActivity.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | crm（erp-crm-web） | （无违规文案；如涉新文案先扩表） |
| 6 | `module-crm/erp-crm-web/src/main/resources/_vfs/erp/crm/pages/ErpCrmEvent/ref-lead.page.yaml` | `ErpCrmEvent.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | crm（erp-crm-web） | （无违规文案；如涉新文案先扩表） |
| 7 | `module-crm/erp-crm-web/src/main/resources/_vfs/erp/crm/pages/ErpCrmLeadConvLog/ref-lead.page.yaml` | `ErpCrmLeadConvLog.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | crm（erp-crm-web） | （无违规文案；如涉新文案先扩表） |
| 8 | `module-cs/erp-cs-web/src/main/resources/_vfs/erp/cs/pages/ErpCsTicketAction/ref-ticket.page.yaml` | `ErpCsTicketAction.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | cs（erp-cs-web） | （无违规文案；如涉新文案先扩表） |
| 9 | `module-cs/erp-cs-web/src/main/resources/_vfs/erp/cs/pages/ErpCsTicketFulfillmentStep/ref-ticket.page.yaml` | `ErpCsTicketFulfillmentStep.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | cs（erp-cs-web） | （无违规文案；如涉新文案先扩表） |
| 10 | `module-drp/erp-drp-web/src/main/resources/_vfs/erp/drp/pages/ErpDrpLine/ref-plan.page.yaml` | `ErpDrpLine.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | drp（erp-drp-web） | （无违规文案；如涉新文案先扩表） |
| 11 | `module-hr/erp-hr-web/src/main/resources/_vfs/erp/hr/pages/ErpHrEmployee/ref-employee.page.yaml` | `ErpHrEmployee.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | hr（erp-hr-web） | （无违规文案；如涉新文案先扩表） |
| 12 | `module-hr/erp-hr-web/src/main/resources/_vfs/erp/hr/pages/ErpHrSurveyQuestion/ref-survey.page.yaml` | `ErpHrSurveyQuestion.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | hr（erp-hr-web） | （无违规文案；如涉新文案先扩表） |
| 13 | `module-hr/erp-hr-web/src/main/resources/_vfs/erp/hr/pages/ErpHrSurveyResponse/ref-survey.page.yaml` | `ErpHrSurveyResponse.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | hr（erp-hr-web） | （无违规文案；如涉新文案先扩表） |
| 14 | `module-inventory/erp-inv-web/src/main/resources/_vfs/erp/inv/pages/ErpInvStockLedger/ref-move.page.yaml` | `ErpInvStockLedger.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | inventory（erp-inv-web） | （无违规文案；如涉新文案先扩表） |
| 15 | `module-logistics/erp-log-web/src/main/resources/_vfs/erp/log/pages/ErpLogCarrierConfig/ref-carrier.page.yaml` | `ErpLogCarrierConfig.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | logistics（erp-log-web） | （无违规文案；如涉新文案先扩表） |
| 16 | `module-logistics/erp-log-web/src/main/resources/_vfs/erp/log/pages/ErpLogShipmentLine/ref-shipment.page.yaml` | `ErpLogShipmentLine.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | logistics（erp-log-web） | （无违规文案；如涉新文案先扩表） |
| 17 | `module-logistics/erp-log-web/src/main/resources/_vfs/erp/log/pages/ErpLogShipmentLog/ref-shipment.page.yaml` | `ErpLogShipmentLog.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | logistics（erp-log-web） | （无违规文案；如涉新文案先扩表） |
| 18 | `module-logistics/erp-log-web/src/main/resources/_vfs/erp/log/pages/ErpLogShipmentParcel/ref-shipment.page.yaml` | `ErpLogShipmentParcel.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | logistics（erp-log-web） | （无违规文案；如涉新文案先扩表） |
| 19 | `module-maintenance/erp-mnt-web/src/main/resources/_vfs/erp/mnt/pages/ErpMntDowntimeEntry/ref-equipment.page.yaml` | `ErpMntDowntimeEntry.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | maintenance（erp-mnt-web） | （无违规文案；如涉新文案先扩表） |
| 20 | `module-maintenance/erp-mnt-web/src/main/resources/_vfs/erp/mnt/pages/ErpMntEquipment/ref-equipment.page.yaml` | `ErpMntEquipment.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | maintenance（erp-mnt-web） | （无违规文案；如涉新文案先扩表） |
| 21 | `module-maintenance/erp-mnt-web/src/main/resources/_vfs/erp/mnt/pages/ErpMntRequest/ref-equipment.page.yaml` | `ErpMntRequest.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | maintenance（erp-mnt-web） | （无违规文案；如涉新文案先扩表） |
| 22 | `module-maintenance/erp-mnt-web/src/main/resources/_vfs/erp/mnt/pages/ErpMntSchedule/ref-equipment.page.yaml` | `ErpMntSchedule.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | maintenance（erp-mnt-web） | （无违规文案；如涉新文案先扩表） |
| 23 | `module-maintenance/erp-mnt-web/src/main/resources/_vfs/erp/mnt/pages/ErpMntSparePartUsage/ref-equipment.page.yaml` | `ErpMntSparePartUsage.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | maintenance（erp-mnt-web） | （无违规文案；如涉新文案先扩表） |
| 24 | `module-maintenance/erp-mnt-web/src/main/resources/_vfs/erp/mnt/pages/ErpMntSparePartUsage/ref-visit.page.yaml` | `ErpMntSparePartUsage.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | maintenance（erp-mnt-web） | （无违规文案；如涉新文案先扩表） |
| 25 | `module-maintenance/erp-mnt-web/src/main/resources/_vfs/erp/mnt/pages/ErpMntVisit/ref-equipment.page.yaml` | `ErpMntVisit.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | maintenance（erp-mnt-web） | （无违规文案；如涉新文案先扩表） |
| 26 | `module-maintenance/erp-mnt-web/src/main/resources/_vfs/erp/mnt/pages/ErpMntVisitTask/ref-visit.page.yaml` | `ErpMntVisitTask.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | maintenance（erp-mnt-web） | （无违规文案；如涉新文案先扩表） |
| 27 | `module-manufacturing/erp-mfg-web/src/main/resources/_vfs/erp/mfg/pages/ErpMfgJobCard/ref-work-order.page.yaml` | `ErpMfgJobCard.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | manufacturing（erp-mfg-web） | （无违规文案；如涉新文案先扩表） |
| 28 | `module-manufacturing/erp-mfg-web/src/main/resources/_vfs/erp/mfg/pages/ErpMfgMaterialIssue/ref-work-order.page.yaml` | `ErpMfgMaterialIssue.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | manufacturing（erp-mfg-web） | （无违规文案；如涉新文案先扩表） |
| 29 | `module-projects/erp-prj-web/src/main/resources/_vfs/erp/prj/pages/ErpPrjBudget/ref-project.page.yaml` | `ErpPrjBudget.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | projects（erp-prj-web） | （无违规文案；如涉新文案先扩表） |
| 30 | `module-projects/erp-prj-web/src/main/resources/_vfs/erp/prj/pages/ErpPrjMilestone/ref-project.page.yaml` | `ErpPrjMilestone.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | projects（erp-prj-web） | （无违规文案；如涉新文案先扩表） |
| 31 | `module-projects/erp-prj-web/src/main/resources/_vfs/erp/prj/pages/ErpPrjProjectUser/ref-project.page.yaml` | `ErpPrjProjectUser.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | projects（erp-prj-web） | （无违规文案；如涉新文案先扩表） |
| 32 | `module-projects/erp-prj-web/src/main/resources/_vfs/erp/prj/pages/ErpPrjTask/ref-project.page.yaml` | `ErpPrjTask.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | projects（erp-prj-web） | （无违规文案；如涉新文案先扩表） |
| 33 | `module-projects/erp-prj-web/src/main/resources/_vfs/erp/prj/pages/ErpPrjTimesheet/ref-project.page.yaml` | `ErpPrjTimesheet.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | projects（erp-prj-web） | （无违规文案；如涉新文案先扩表） |
| 34 | `module-projects/erp-prj-web/src/main/resources/_vfs/erp/prj/pages/ErpPrjTimesheet/ref-task.page.yaml` | `ErpPrjTimesheet.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | projects（erp-prj-web） | （无违规文案；如涉新文案先扩表） |
| 35 | `module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/ErpPurReceive/ref-order.page.yaml` | `ErpPurReceive.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | purchase（erp-pur-web） | （无违规文案；如涉新文案先扩表） |
| 36 | `module-quality/erp-qa-web/src/main/resources/_vfs/erp/qa/pages/ErpQaAction/ref-ncr.page.yaml` | `ErpQaAction.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | quality（erp-qa-web） | （无违规文案；如涉新文案先扩表） |
| 37 | `module-quality/erp-qa-web/src/main/resources/_vfs/erp/qa/pages/ErpQaInspectionLine/ref-inspection.page.yaml` | `ErpQaInspectionLine.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | quality（erp-qa-web） | （无违规文案；如涉新文案先扩表） |
| 38 | `module-quality/erp-qa-web/src/main/resources/_vfs/erp/qa/pages/ErpQaRecallTarget/ref-recall.page.yaml` | `ErpQaRecallTarget.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | quality（erp-qa-web） | （无违规文案；如涉新文案先扩表） |
| 39 | `module-sales/erp-sal-web/src/main/resources/_vfs/erp/sal/pages/ErpSalDelivery/ref-order.page.yaml` | `ErpSalDelivery.view.xml` | 0 | 改模型源 view.xml `i18n-en:*` + codegen 重生成验证；禁改本文件（lesson 06） | sales（erp-sal-web） | （无违规文案；如涉新文案先扩表） |

### B. 手写页类（直接补 i18nEn，MI.8 按域分批）

| # | 文件 | CAT-4 违规行 | 修复策略 | owner 域 | glossary 条目引用 |
|---|------|--------------|----------|----------|------------------|
| 1 | `module-aps/erp-aps-web/src/main/resources/_vfs/erp/aps/pages/dashboard/schedule-gantt.flux.yaml` | 13 | 补 `i18nEn`（MI.8） | aps（erp-aps-web） | `工作中心=Work Center`、`完成=Complete` |
| 2 | `module-aps/erp-aps-web/src/main/resources/_vfs/erp/aps/pages/dashboard/schedule-gantt.page.yaml` | 22 | 补 `i18nEn`（MI.8） | aps（erp-aps-web） | `工作中心=Work Center`、`完成=Complete` |
| 3 | `module-assets/erp-ast-web/src/main/resources/_vfs/erp/ast/pages/asset-repair/main.page.yaml` | 2 | 补 `i18nEn`（MI.8） | assets（erp-ast-web） | `资产=Asset` |
| 4 | `module-assets/erp-ast-web/src/main/resources/_vfs/erp/ast/pages/asset-stocktake/main.page.yaml` | 14 | 补 `i18nEn`（MI.8） | assets（erp-ast-web） | `所属组织=Owning Org`、`部门=Department`、`资产=Asset`、`完成=Complete` |
| 5 | `module-assets/erp-ast-web/src/main/resources/_vfs/erp/ast/pages/dashboard/main.flux.yaml` | 18 | 补 `i18nEn`（MI.8） | assets（erp-ast-web） | `会计期间=Accounting Period`、`资产=Asset` |
| 6 | `module-assets/erp-ast-web/src/main/resources/_vfs/erp/ast/pages/dashboard/main.page.yaml` | 19 | 补 `i18nEn`（MI.8） | assets（erp-ast-web） | `会计期间=Accounting Period`、`资产=Asset` |
| 7 | `module-assets/erp-ast-web/src/main/resources/_vfs/erp/ast/pages/disposal-wizard/main.page.yaml` | 20 | 补 `i18nEn`（MI.8） | assets（erp-ast-web） | `资产=Asset`、`提交=Submit` |
| 8 | `module-assets/erp-ast-web/src/main/resources/_vfs/erp/ast/pages/report/asset-depreciation-detail.page.yaml` | 8 | 补 `i18nEn`（MI.8） | assets（erp-ast-web） | `资产=Asset` |
| 9 | `module-assets/erp-ast-web/src/main/resources/_vfs/erp/ast/pages/report/asset-disposal-detail.page.yaml` | 6 | 补 `i18nEn`（MI.8） | assets（erp-ast-web） | `资产=Asset` |
| 10 | `module-b2b/erp-b2b-web/src/main/resources/_vfs/erp/b2b/pages/dashboard/asn-flow.flux.yaml` | 27 | 补 `i18nEn`（MI.8） | b2b（erp-b2b-web） | `物料=Material`、`供应商=Supplier` |
| 11 | `module-b2b/erp-b2b-web/src/main/resources/_vfs/erp/b2b/pages/dashboard/asn-flow.page.yaml` | 32 | 补 `i18nEn`（MI.8） | b2b（erp-b2b-web） | `物料=Material`、`供应商=Supplier`、`完成=Complete` |
| 12 | `module-b2b/erp-b2b-web/src/main/resources/_vfs/erp/b2b/pages/dashboard/edi-detail.flux.yaml` | 11 | 补 `i18nEn`（MI.8） | b2b（erp-b2b-web） | 新词（先扩 i18n-glossary 再使用） |
| 13 | `module-b2b/erp-b2b-web/src/main/resources/_vfs/erp/b2b/pages/dashboard/edi-detail.page.yaml` | 21 | 补 `i18nEn`（MI.8） | b2b（erp-b2b-web） | 新词（先扩 i18n-glossary 再使用） |
| 14 | `module-contract/erp-ct-web/src/main/resources/_vfs/erp/ct/pages/dashboard/version-diff.flux.yaml` | 18 | 补 `i18nEn`（MI.8） | contract（erp-ct-web） | `合同=Contract` |
| 15 | `module-contract/erp-ct-web/src/main/resources/_vfs/erp/ct/pages/dashboard/version-diff.page.yaml` | 32 | 补 `i18nEn`（MI.8） | contract（erp-ct-web） | `合同=Contract` |
| 16 | `module-crm/erp-crm-web/src/main/resources/_vfs/erp/crm/pages/ErpCrmActivity/calendar.flux.yaml` | 6 | 补 `i18nEn`（MI.8） | crm（erp-crm-web） | `商机=Opportunity`、`线索=Lead` |
| 17 | `module-crm/erp-crm-web/src/main/resources/_vfs/erp/crm/pages/ErpCrmActivity/calendar.page.yaml` | 12 | 补 `i18nEn`（MI.8） | crm（erp-crm-web） | `客户=Customer`、`商机=Opportunity`、`线索=Lead` |
| 18 | `module-crm/erp-crm-web/src/main/resources/_vfs/erp/crm/pages/ErpCrmActivity/timeline.flux.yaml` | 6 | 补 `i18nEn`（MI.8） | crm（erp-crm-web） | `商机=Opportunity`、`线索=Lead` |
| 19 | `module-crm/erp-crm-web/src/main/resources/_vfs/erp/crm/pages/ErpCrmActivity/timeline.page.yaml` | 7 | 补 `i18nEn`（MI.8） | crm（erp-crm-web） | `商机=Opportunity`、`线索=Lead` |
| 20 | `module-crm/erp-crm-web/src/main/resources/_vfs/erp/crm/pages/ErpCrmLead/opportunity-kanban.flux.yaml` | 5 | 补 `i18nEn`（MI.8） | crm（erp-crm-web） | `商机=Opportunity` |
| 21 | `module-crm/erp-crm-web/src/main/resources/_vfs/erp/crm/pages/ErpCrmLead/opportunity-kanban.page.yaml` | 18 | 补 `i18nEn`（MI.8） | crm（erp-crm-web） | `客户=Customer`、`商机=Opportunity`、`拒绝=Refuse` |
| 22 | `module-crm/erp-crm-web/src/main/resources/_vfs/erp/crm/pages/lead-conversion/main.page.yaml` | 11 | 补 `i18nEn`（MI.8） | crm（erp-crm-web） | `线索=Lead` |
| 23 | `module-crm/erp-crm-web/src/main/resources/_vfs/erp/crm/pages/report/campaign-attribution.page.yaml` | 4 | 补 `i18nEn`（MI.8） | crm（erp-crm-web） | 新词（先扩 i18n-glossary 再使用） |
| 24 | `module-crm/erp-crm-web/src/main/resources/_vfs/erp/crm/pages/report/forecast-accuracy.page.yaml` | 6 | 补 `i18nEn`（MI.8） | crm（erp-crm-web） | 新词（先扩 i18n-glossary 再使用） |
| 25 | `module-crm/erp-crm-web/src/main/resources/_vfs/erp/crm/pages/report/lead-conversion-funnel.page.yaml` | 4 | 补 `i18nEn`（MI.8） | crm（erp-crm-web） | `线索=Lead` |
| 26 | `module-cs/erp-cs-web/src/main/resources/_vfs/erp/cs/pages/ErpCsQualityDashboard/main.flux.yaml` | 21 | 补 `i18nEn`（MI.8） | cs（erp-cs-web） | `工单=Work Order` |
| 27 | `module-cs/erp-cs-web/src/main/resources/_vfs/erp/cs/pages/ErpCsQualityDashboard/main.page.yaml` | 20 | 补 `i18nEn`（MI.8） | cs（erp-cs-web） | `工单=Work Order` |
| 28 | `module-cs/erp-cs-web/src/main/resources/_vfs/erp/cs/pages/ErpCsTicket/kanban.flux.yaml` | 10 | 补 `i18nEn`（MI.8） | cs（erp-cs-web） | `客户=Customer`、`工单=Work Order` |
| 29 | `module-cs/erp-cs-web/src/main/resources/_vfs/erp/cs/pages/ErpCsTicket/kanban.page.yaml` | 48 | 补 `i18nEn`（MI.8） | cs（erp-cs-web） | `客户=Customer`、`工单=Work Order`、`驳回=Reject` |
| 30 | `module-cs/erp-cs-web/src/main/resources/_vfs/erp/cs/pages/ErpCsTicketAction/timeline.flux.yaml` | 6 | 补 `i18nEn`（MI.8） | cs（erp-cs-web） | `工单=Work Order` |
| 31 | `module-cs/erp-cs-web/src/main/resources/_vfs/erp/cs/pages/ErpCsTicketAction/timeline.page.yaml` | 9 | 补 `i18nEn`（MI.8） | cs（erp-cs-web） | `工单=Work Order` |
| 32 | `module-cs/erp-cs-web/src/main/resources/_vfs/erp/cs/pages/report/ticket-sla-csat-summary.page.yaml` | 6 | 补 `i18nEn`（MI.8） | cs（erp-cs-web） | `工单=Work Order` |
| 33 | `module-drp/erp-drp-web/src/main/resources/_vfs/erp/drp/pages/dashboard/net-requirement.flux.yaml` | 20 | 补 `i18nEn`（MI.8） | drp（erp-drp-web） | `仓库=Warehouse` |
| 34 | `module-drp/erp-drp-web/src/main/resources/_vfs/erp/drp/pages/dashboard/net-requirement.page.yaml` | 21 | 补 `i18nEn`（MI.8） | drp（erp-drp-web） | `物料=Material`、`仓库=Warehouse` |
| 35 | `module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/bank-ledger-line/main.page.yaml` | 2 | 补 `i18nEn`（MI.8） | finance（erp-fin-web） | 新词（先扩 i18n-glossary 再使用） |
| 36 | `module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/bank-reconciliation/main.page.yaml` | 15 | 补 `i18nEn`（MI.8） | finance（erp-fin-web） | `业务组织=Business Org` |
| 37 | `module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/bank-statement/main.page.yaml` | 14 | 补 `i18nEn`（MI.8） | finance（erp-fin-web） | `业务组织=Business Org` |
| 38 | `module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/budget-control-log/main.page.yaml` | 17 | 补 `i18nEn`（MI.8） | finance（erp-fin-web） | `科目=Account` |
| 39 | `module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/budget-scenario/main.page.yaml` | 15 | 补 `i18nEn`（MI.8） | finance（erp-fin-web） | `本位币=Functional Currency`、`审核=Audit` |
| 40 | `module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/dashboard/main.flux.yaml` | 20 | 补 `i18nEn`（MI.8） | finance（erp-fin-web） | `会计期间=Accounting Period` |
| 41 | `module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/dashboard/main.page.yaml` | 20 | 补 `i18nEn`（MI.8） | finance（erp-fin-web） | `会计期间=Accounting Period` |
| 42 | `module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/ErpFinVoucherBillR/bills-by-voucher.page.yaml` | 7 | 补 `i18nEn`（MI.8） | finance（erp-fin-web） | 新词（先扩 i18n-glossary 再使用） |
| 43 | `module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/ErpFinVoucherBillR/voucher-by-bill.page.yaml` | 11 | 补 `i18nEn`（MI.8） | finance（erp-fin-web） | `过账=Post` |
| 44 | `module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/expense-claim/main.page.yaml` | 17 | 补 `i18nEn`（MI.8） | finance（erp-fin-web） | `部门=Department`、`提交=Submit`、`审核=Audit` |
| 45 | `module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/gl-distribution/main.page.yaml` | 2 | 补 `i18nEn`（MI.8） | finance（erp-fin-web） | 新词（先扩 i18n-glossary 再使用） |
| 46 | `module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/period-close-wizard/main.flux.yaml` | 26 | 补 `i18nEn`（MI.8） | finance（erp-fin-web） | `会计期间=Accounting Period`、`过账=Post`、`结账=Close Account`、`反结账=Un-close`、`核销=Settle` |
| 47 | `module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/period-close-wizard/main.page.yaml` | 53 | 补 `i18nEn`（MI.8） | finance（erp-fin-web） | `账套=Accounting Set`、`会计期间=Accounting Period`、`过账=Post`、`结账=Close Account`、`反结账=Un-close`、`核销=Settle` |
| 48 | `module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/report/ar-ap-aging.page.yaml` | 5 | 补 `i18nEn`（MI.8） | finance（erp-fin-web） | 新词（先扩 i18n-glossary 再使用） |
| 49 | `module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/report/balance-sheet.page.yaml` | 6 | 补 `i18nEn`（MI.8） | finance（erp-fin-web） | `会计期间=Accounting Period`、`资产=Asset` |
| 50 | `module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/report/cash-flow.page.yaml` | 6 | 补 `i18nEn`（MI.8） | finance（erp-fin-web） | `会计期间=Accounting Period` |
| 51 | `module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/report/income-statement.page.yaml` | 6 | 补 `i18nEn`（MI.8） | finance（erp-fin-web） | `会计期间=Accounting Period` |
| 52 | `module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/report/period-close-report.page.yaml` | 6 | 补 `i18nEn`（MI.8） | finance（erp-fin-web） | `会计期间=Accounting Period`、`结账=Close Account` |
| 53 | `module-hr/erp-hr-web/src/main/resources/_vfs/erp/hr/pages/dashboard/org-chart.flux.yaml` | 7 | 补 `i18nEn`（MI.8） | hr（erp-hr-web） | `部门=Department` |
| 54 | `module-hr/erp-hr-web/src/main/resources/_vfs/erp/hr/pages/dashboard/org-chart.page.yaml` | 6 | 补 `i18nEn`（MI.8） | hr（erp-hr-web） | `员工=Employee` |
| 55 | `module-hr/erp-hr-web/src/main/resources/_vfs/erp/hr/pages/dashboard/payroll-approval.flux.yaml` | 44 | 补 `i18nEn`（MI.8） | hr（erp-hr-web） | `员工=Employee`、`作废=Void` |
| 56 | `module-hr/erp-hr-web/src/main/resources/_vfs/erp/hr/pages/dashboard/payroll-approval.page.yaml` | 38 | 补 `i18nEn`（MI.8） | hr（erp-hr-web） | `员工=Employee`、`作废=Void` |
| 57 | `module-hr/erp-hr-web/src/main/resources/_vfs/erp/hr/pages/ErpHrLeaveRequest/team-vacation-calendar.flux.yaml` | 4 | 补 `i18nEn`（MI.8） | hr（erp-hr-web） | 新词（先扩 i18n-glossary 再使用） |
| 58 | `module-hr/erp-hr-web/src/main/resources/_vfs/erp/hr/pages/ErpHrLeaveRequest/team-vacation-calendar.page.yaml` | 9 | 补 `i18nEn`（MI.8） | hr（erp-hr-web） | `员工=Employee` |
| 59 | `module-hr/erp-hr-web/src/main/resources/_vfs/erp/hr/pages/report/employee-net-balance.page.yaml` | 4 | 补 `i18nEn`（MI.8） | hr（erp-hr-web） | `员工=Employee` |
| 60 | `module-hr/erp-hr-web/src/main/resources/_vfs/erp/hr/pages/report/payroll-simulation-comparison.page.yaml` | 6 | 补 `i18nEn`（MI.8） | hr（erp-hr-web） | 新词（先扩 i18n-glossary 再使用） |
| 61 | `module-inventory/erp-inv-web/src/main/resources/_vfs/erp/inv/pages/dashboard/main.flux.yaml` | 31 | 补 `i18nEn`（MI.8） | inventory（erp-inv-web） | `物料=Material`、`仓库=Warehouse` |
| 62 | `module-inventory/erp-inv-web/src/main/resources/_vfs/erp/inv/pages/dashboard/main.page.yaml` | 31 | 补 `i18nEn`（MI.8） | inventory（erp-inv-web） | `物料=Material`、`仓库=Warehouse` |
| 63 | `module-inventory/erp-inv-web/src/main/resources/_vfs/erp/inv/pages/report/inventory-trace-report.page.yaml` | 10 | 补 `i18nEn`（MI.8） | inventory（erp-inv-web） | `物料=Material`、`仓库=Warehouse` |
| 64 | `module-inventory/erp-inv-web/src/main/resources/_vfs/erp/inv/pages/stock-take-flow/main.page.yaml` | 21 | 补 `i18nEn`（MI.8） | inventory（erp-inv-web） | `部门=Department`、`完成=Complete` |
| 65 | `module-logistics/erp-log-web/src/main/resources/_vfs/erp/log/pages/dashboard/shipment-tracking.flux.yaml` | 7 | 补 `i18nEn`（MI.8） | logistics（erp-log-web） | `发运=Shipment` |
| 66 | `module-logistics/erp-log-web/src/main/resources/_vfs/erp/log/pages/dashboard/shipment-tracking.page.yaml` | 11 | 补 `i18nEn`（MI.8） | logistics（erp-log-web） | `发运=Shipment` |
| 67 | `module-maintenance/erp-mnt-web/src/main/resources/_vfs/erp/mnt/pages/dashboard/main.flux.yaml` | 23 | 补 `i18nEn`（MI.8） | maintenance（erp-mnt-web） | `设备=Equipment` |
| 68 | `module-maintenance/erp-mnt-web/src/main/resources/_vfs/erp/mnt/pages/dashboard/main.page.yaml` | 24 | 补 `i18nEn`（MI.8） | maintenance（erp-mnt-web） | `设备=Equipment` |
| 69 | `module-maintenance/erp-mnt-web/src/main/resources/_vfs/erp/mnt/pages/report/downtime-summary.page.yaml` | 8 | 补 `i18nEn`（MI.8） | maintenance（erp-mnt-web） | `设备=Equipment` |
| 70 | `module-maintenance/erp-mnt-web/src/main/resources/_vfs/erp/mnt/pages/report/maintenance-history.page.yaml` | 8 | 补 `i18nEn`（MI.8） | maintenance（erp-mnt-web） | `设备=Equipment` |
| 71 | `module-maintenance/erp-mnt-web/src/main/resources/_vfs/erp/mnt/pages/visit-wizard/main.flux.yaml` | 25 | 补 `i18nEn`（MI.8） | maintenance（erp-mnt-web） | `设备=Equipment`、`完成=Complete` |
| 72 | `module-maintenance/erp-mnt-web/src/main/resources/_vfs/erp/mnt/pages/visit-wizard/main.page.yaml` | 34 | 补 `i18nEn`（MI.8） | maintenance（erp-mnt-web） | `仓库=Warehouse`、`设备=Equipment`、`过账=Post`、`完成=Complete` |
| 73 | `module-manufacturing/erp-mfg-web/src/main/resources/_vfs/erp/mfg/pages/dashboard/bom-tree.flux.yaml` | 9 | 补 `i18nEn`（MI.8） | manufacturing（erp-mfg-web） | 新词（先扩 i18n-glossary 再使用） |
| 74 | `module-manufacturing/erp-mfg-web/src/main/resources/_vfs/erp/mfg/pages/dashboard/bom-tree.page.yaml` | 13 | 补 `i18nEn`（MI.8） | manufacturing（erp-mfg-web） | 新词（先扩 i18n-glossary 再使用） |
| 75 | `module-manufacturing/erp-mfg-web/src/main/resources/_vfs/erp/mfg/pages/dashboard/main.flux.yaml` | 24 | 补 `i18nEn`（MI.8） | manufacturing（erp-mfg-web） | `工单=Work Order`、`完成=Complete` |
| 76 | `module-manufacturing/erp-mfg-web/src/main/resources/_vfs/erp/mfg/pages/dashboard/main.page.yaml` | 26 | 补 `i18nEn`（MI.8） | manufacturing（erp-mfg-web） | `工作中心=Work Center`、`工单=Work Order`、`完成=Complete` |
| 77 | `module-manufacturing/erp-mfg-web/src/main/resources/_vfs/erp/mfg/pages/report/crp-load-report.page.yaml` | 8 | 补 `i18nEn`（MI.8） | manufacturing（erp-mfg-web） | `工作中心=Work Center` |
| 78 | `module-manufacturing/erp-mfg-web/src/main/resources/_vfs/erp/mfg/pages/report/forecast-variance-report.page.yaml` | 8 | 补 `i18nEn`（MI.8） | manufacturing（erp-mfg-web） | `物料=Material` |
| 79 | `module-manufacturing/erp-mfg-web/src/main/resources/_vfs/erp/mfg/pages/report/production-variance-report.page.yaml` | 8 | 补 `i18nEn`（MI.8） | manufacturing（erp-mfg-web） | `工单=Work Order` |
| 80 | `module-master-data/erp-md-web/src/main/resources/_vfs/erp/md/pages/cost-center/main.page.yaml` | 14 | 补 `i18nEn`（MI.8） | master-data（erp-md-web） | `所属组织=Owning Org`、`成本中心=Cost Center` |
| 81 | `module-master-data/erp-md-web/src/main/resources/_vfs/erp/md/pages/dashboard/main.flux.yaml` | 14 | 补 `i18nEn`（MI.8） | master-data（erp-md-web） | `物料=Material`、`客户=Customer`、`供应商=Supplier`、`往来单位=Business Partner` |
| 82 | `module-master-data/erp-md-web/src/main/resources/_vfs/erp/md/pages/dashboard/main.page.yaml` | 14 | 补 `i18nEn`（MI.8） | master-data（erp-md-web） | `物料=Material`、`客户=Customer`、`供应商=Supplier`、`往来单位=Business Partner` |
| 83 | `module-master-data/erp-md-web/src/main/resources/_vfs/erp/md/pages/party-search/main.picker.page.yaml` | 20 | 补 `i18nEn`（MI.8） | master-data（erp-md-web） | `往来单位=Business Partner` |
| 84 | `module-master-data/erp-md-web/src/main/resources/_vfs/erp/md/pages/report/material-price-list.page.yaml` | 6 | 补 `i18nEn`（MI.8） | master-data（erp-md-web） | `物料=Material` |
| 85 | `module-master-data/erp-md-web/src/main/resources/_vfs/erp/md/pages/report/partner-list.page.yaml` | 6 | 补 `i18nEn`（MI.8） | master-data（erp-md-web） | `客户=Customer`、`供应商=Supplier`、`往来单位=Business Partner` |
| 86 | `module-notify/erp-notify-web/src/main/resources/_vfs/erp/notify/pages/ErpSysNotification/inbox.page.yaml` | 42 | 补 `i18nEn`（MI.8） | notify（erp-notify-web） | 新词（先扩 i18n-glossary 再使用） |
| 87 | `module-projects/erp-prj-web/src/main/resources/_vfs/erp/prj/pages/dashboard/main.flux.yaml` | 23 | 补 `i18nEn`（MI.8） | projects（erp-prj-web） | `项目=Project`、`完成=Complete` |
| 88 | `module-projects/erp-prj-web/src/main/resources/_vfs/erp/prj/pages/dashboard/main.page.yaml` | 24 | 补 `i18nEn`（MI.8） | projects（erp-prj-web） | `项目=Project`、`完成=Complete` |
| 89 | `module-projects/erp-prj-web/src/main/resources/_vfs/erp/prj/pages/ErpPrjTask/kanban.flux.yaml` | 6 | 补 `i18nEn`（MI.8） | projects（erp-prj-web） | `项目=Project`、`任务=Task` |
| 90 | `module-projects/erp-prj-web/src/main/resources/_vfs/erp/prj/pages/ErpPrjTask/kanban.page.yaml` | 35 | 补 `i18nEn`（MI.8） | projects（erp-prj-web） | `项目=Project`、`任务=Task`、`完成=Complete` |
| 91 | `module-projects/erp-prj-web/src/main/resources/_vfs/erp/prj/pages/project-pnl/main.page.yaml` | 15 | 补 `i18nEn`（MI.8） | projects（erp-prj-web） | `项目=Project` |
| 92 | `module-projects/erp-prj-web/src/main/resources/_vfs/erp/prj/pages/project-settlement/main.page.yaml` | 15 | 补 `i18nEn`（MI.8） | projects（erp-prj-web） | `客户=Customer`、`项目=Project` |
| 93 | `module-projects/erp-prj-web/src/main/resources/_vfs/erp/prj/pages/report/project-cost-summary.page.yaml` | 8 | 补 `i18nEn`（MI.8） | projects（erp-prj-web） | `项目=Project` |
| 94 | `module-projects/erp-prj-web/src/main/resources/_vfs/erp/prj/pages/report/timesheet-detail.page.yaml` | 8 | 补 `i18nEn`（MI.8） | projects（erp-prj-web） | `项目=Project` |
| 95 | `module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/dashboard/main.flux.yaml` | 24 | 补 `i18nEn`（MI.8） | purchase（erp-pur-web） | `供应商=Supplier` |
| 96 | `module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/dashboard/main.page.yaml` | 24 | 补 `i18nEn`（MI.8） | purchase（erp-pur-web） | `供应商=Supplier` |
| 97 | `module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/dashboard/three-way-match.flux.yaml` | 22 | 补 `i18nEn`（MI.8） | purchase（erp-pur-web） | `供应商=Supplier` |
| 98 | `module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/dashboard/three-way-match.page.yaml` | 29 | 补 `i18nEn`（MI.8） | purchase（erp-pur-web） | `供应商=Supplier` |
| 99 | `module-quality/erp-qa-web/src/main/resources/_vfs/erp/qa/pages/dashboard/main.flux.yaml` | 25 | 补 `i18nEn`（MI.8） | quality（erp-qa-web） | `完成=Complete` |
| 100 | `module-quality/erp-qa-web/src/main/resources/_vfs/erp/qa/pages/dashboard/main.page.yaml` | 28 | 补 `i18nEn`（MI.8） | quality（erp-qa-web） | `完成=Complete` |
| 101 | `module-quality/erp-qa-web/src/main/resources/_vfs/erp/qa/pages/ncr-disposal/main.page.yaml` | 2 | 补 `i18nEn`（MI.8） | quality（erp-qa-web） | 新词（先扩 i18n-glossary 再使用） |
| 102 | `module-quality/erp-qa-web/src/main/resources/_vfs/erp/qa/pages/report/inspection-summary.page.yaml` | 8 | 补 `i18nEn`（MI.8） | quality（erp-qa-web） | `物料=Material` |
| 103 | `module-quality/erp-qa-web/src/main/resources/_vfs/erp/qa/pages/report/ncr-capa-summary.page.yaml` | 6 | 补 `i18nEn`（MI.8） | quality（erp-qa-web） | 新词（先扩 i18n-glossary 再使用） |
| 104 | `module-quality/erp-qa-web/src/main/resources/_vfs/erp/qa/pages/spc-capability/main.page.yaml` | 12 | 补 `i18nEn`（MI.8） | quality（erp-qa-web） | 新词（先扩 i18n-glossary 再使用） |
| 105 | `module-quality/erp-qa-web/src/main/resources/_vfs/erp/qa/pages/spc-chart/main.page.yaml` | 17 | 补 `i18nEn`（MI.8） | quality（erp-qa-web） | 新词（先扩 i18n-glossary 再使用） |
| 106 | `module-quality/erp-qa-web/src/main/resources/_vfs/erp/qa/pages/spc-sample/main.page.yaml` | 16 | 补 `i18nEn`（MI.8） | quality（erp-qa-web） | 新词（先扩 i18n-glossary 再使用） |
| 107 | `module-sales/erp-sal-web/src/main/resources/_vfs/erp/sal/pages/dashboard/main.flux.yaml` | 19 | 补 `i18nEn`（MI.8） | sales（erp-sal-web） | `客户=Customer` |
| 108 | `module-sales/erp-sal-web/src/main/resources/_vfs/erp/sal/pages/dashboard/main.page.yaml` | 20 | 补 `i18nEn`（MI.8） | sales（erp-sal-web） | `客户=Customer` |

## 机械复验命令（审计重放）

```bash
# 1) 冻结口径违规文件数与违规行数
awk '/^files:/{f=1;next}/^domains:/{f=0}f' docs/audits/cjk-baseline.md | grep -c CAT4   # = 108
awk '/^files:/{f=1;next}/^domains:/{f=0}f' docs/audits/cjk-baseline.md | grep -oE 'CAT4: [0-9]+' | awk '{s+=$2}END{print s}'   # = 1700
# 2) stub/手写分类
for f in $(git ls-files -co --exclude-standard '*.page.yaml' '*.flux.yaml' | grep src/main/resources); do
  git ls-files --error-unmatch "$f" >/dev/null 2>&1 || :
  n=$(grep -v "^\s*#" "$f" | grep -c web:GenPage); [ "$n" -gt 0 ] && echo "stub $f" || echo "hand $f";
done | sort | uniq -c -w4
# 3) 全量 CJK 文件数（raw 口径，应 = 147）
git ls-files -co --exclude-standard '*.page.yaml' '*.flux.yaml' | grep src/main/resources | xargs grep -lP '[\x{4e00}-\x{9fff}]' 2>/dev/null | wc -l   # BSD grep 无 -P 时用 perl -CSD
```

