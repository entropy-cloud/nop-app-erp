# 2026-08-21-2025-3-bigint-id-m36-contract-migration 主键/外键 string 化 M3.6：contract 域迁移（冻结序位次 5）

> Plan Status: completed（2026-08-21：四 Phase 执行完毕 + 独立结束审计 iteration 3 `passes closure audit`（0 BLOCKER / 0 MAJOR / 0 MINOR），见 Closure；iteration 1 双独立审查 `passes draft review` + 保护区域双独立子 agent 批准，见 Draft Review Record；结束审计 iteration 1 发现 1 MAJOR（version-diff.page.yaml `:Long`）已就地 Fix + iteration 2 文档链三处补全后终审通过）
> Mission: id-string-migration
> Work Item: M3.6（contract，冻结序位次 5）
> Last Reviewed: 2026-08-21
> Source: `docs/backlog/id-string-migration-roadmap.md` M2/M3 冻结总序表位次 5（M3.6）
> Related: `docs/plans/2026-08-21-2025-1-bigint-id-m39-aps-migration.md`（批内序 1）、`docs/plans/2026-08-21-2025-2-bigint-id-m38-b2b-migration.md`（批内序 2）、`docs/plans/2026-08-21-1045-3-bigint-id-m11-master-data-migration.md`（M1.1）、`docs/plans/2026-08-21-1657-2-bigint-id-m12-notify-migration.md`（M1.2）、`docs/plans/2026-08-21-1657-1-bigint-id-m02-forward-coupling-registry.md`（M0.2 登记册，消费来源）
> Audit: required（保护区域 `model/*.orm.xml`：独立 plan-audit + 双独立子 agent 批准，批准记录落盘本文件）

## Current Baseline

- **contract 域规模（2026-08-21 实况 scan）**：`module-contract/model/app-erp-contract.orm.xml` 需改列 **47 = 自有 43（PK 15 + BIGINT FK 28，15 实体）+ notGenCode 外部实体 stub 4**。自有 PK：`ErpCtContract/ContractLine/ContractVersion/InvoicePlan/ConsumptionLine/Template/ApprovalMatrix/ApprovalRecord/VolumeDiscount/RebateAgreement/RebateTier/RebateAccrual/RebateSettlement/SignatureRequest/Document` 各 `.id`；自有 FK 含 `orgId` ×9（M1.3 已 String 语义）+ `partnerId` ×2 + `contractId` ×5 + `contractLineId` ×3 + `rebateAgreementId` ×3 + `materialId`/`currencyId`/`templateId`/`parentContractId`/`approvalMatrixId`/`contractVersionId` 等本域 FK。**不改列**：`delVersion` ×15（非 PK/FK 保持 long，路线图规则 4；另有 `ErpCtDocument.fileSize` 非 id BIGINT 同保持）+ VARCHAR FK 6 列（`attachmentFileId` ×4、`ApprovalRecord.approverId`、`SignatureRequest.providerRequestId`，显式 string 本就 String 零改动）。
- **notGenCode stub（本域特有，需显式登记）**：contract orm 末尾嵌入 md 外部实体 stub 4 个（约 :746-774）：`ErpMdPartner`/`ErpMdOrganization`/`ErpMdCurrency`/`ErpMdMaterial`——md 权威源自 M1.1 已 String，stub 翻转为**与权威源对齐**（非新增决策）；dry-run 副本已核证 4 stub id 一并翻转。orm 内 md refEntityName 13 处 + 本域 refEntityName 20 处。
- **模块链与编译依赖（pom 实测）**：7 模块 = `module-contract/erp-ct-{codegen,dao,meta,service,web,app,api}`（注意：物理模块短名 `erp-ct-*`、orm 文件名 `app-erp-contract.orm.xml`、maven artifactId `app-erp-contract-*`；**包名双轨**——dao 生成实体包 `app.erp.contract.dao.entity.*`（含 `_gen/`，orm 头 `entityPackage` 注释自述与实际 className 不一致，以实际为准）vs 手写代码包 `app.erp.ct.*`（IBiz/值对象 `app.erp.ct.biz`、service `app.erp.ct.service`））。ct-dao main compile 依赖 **md-dao**（已 String）——ct-dao `_gen` 关系胶水自 M1.1 起处于已登记中间态（D3 对称耦合），**本域迁移即自愈**。ct-service main compile 依赖 **pur-dao + sal-dao（均未迁移 Long）+ notify-dao（已 String）**；test 依赖 notify-service + **md-service（test-scope，注册跨域业务对象使 GraphQL save 跨域引用校验可通过——已 String，无陈旧 jar 风险）** + app-erp-common-test。
- **M0.2 登记册 contract 视角（§6.5，起草消费已核）**：A1 orm 延后 = 0；**A2 main 桥接 20 条**（pur 10 + sal 10，跨 6 文件：`ErpCtInvoicePlanBizModel:32-35`、`ErpCtRebateAgreementBizModel:23/24`、`ErpCtRebateSettlementBizModel:27-30`、`ErpCtInvoicePlanTriggerInvoiceProcessor:8-11`、`ErpCtRebateAgreementRunAccrualProcessor:9/10`、`ErpCtRebateSettlementPostSettlementProcessor:11-14`，全部为 ErpPurInvoice/InvoiceLine + ErpSalInvoice/InvoiceLine 实体 import，退役 owner M2.5（pur）/M2.6（sal））；**A3 test 桥接 5 条**（`TestErpCtBillingFamily`/`TestErpCtContractExpiryJob`/`TestErpCtContractPosting`/`TestErpCtContractRebate`/`TestErpCtRebateSettlementEnd` import pur/sal 发票实体）；C1 后向 main 2 条（backward-139 → md：`ErpCtRebateSettlementBizModel`/`ErpCtRebateSettlementPostSettlementProcessor`；backward-140 → notify：6 文件 `ErpCtApprovalRecordBizModel`/`ErpCtContractBizModel`/`ErpCtDocumentBizModel`/`ErpCtApprovalTimeoutEscalationJob`/`ErpCtContractExpiryJob`/`ErpCtConsumptionPeriodSummarizeProcessor`）；C2 后向 test 2 条（backward-199 → md：13 测试文件；backward-200 → notify：5 测试文件，两清单有交集文件）；B 退役义务 = 0。**被引用面**：purchase/sales main 各 1 文件 + test 各 1 文件引用本域（pur/sal plan 各自消费）。
- **手写代码冲击面（实测）**：dao 手写 Long 相关 ~12 文件（IBiz ×10：Contract/ContractVersion/ApprovalRecord/RebateAgreement/ConsumptionLine/VolumeDiscount/InvoicePlan/Document/SignatureRequest/RebateSettlement + 值对象 `ErpCtConsumptionPeriodSummarizeResult`/`ErpCtInvoicePlanGenerateItem`）；service 手写含 in-code `Long` 26 文件（BizModel ×10、Processor ×11、Job ×1、RebateEngine/ApprovalWorkflowEngine、SPI model ×2，全部含 id 相关 Long——如 `OcrRecognizeRequest` documentId、`ErpCtInvoicePlanTriggerDuePlansProcessor` contractId）；api beans 30 文件为 **codegen 生成件**（零手写，orm 翻转后随动）。`.getId()` main 手写 74 处。执行时以编译器清单为准。
- **测试资产（实测）**：service 22 个 `@Test` 类（CrudSmoke/StateMachine Matrix ×4/BaselineIoC/DeltaOverride/StateGuards 类/ApprovalTimeoutJob/ApprovalWorkflow/BillingFamily/ContractCreateValidate/ContractExpiryJob/ContractPosting/ContractRebate/ContractTerminate/DocRetention/DocumentGuards/DocumentRepository/ESignature/RebateSettlementEnd/ResponseMasking/TerminateGate）+ helpers（`CtFrozenClockExtension`/`FixedTextOcrEngine`/`ErpCtContractStateMachineDelta`）；`_cases` 快照 540 文件（393 csv + 137 yaml + 10 json5 CrudSmoke 输出快照——本批三域中最大快照面）+ codegen 入口类；`ErpCtWebPagesTest` `@Tag("full-app")` + surefire `<excludedGroups>` 模块级排除（已提交治理决策，successor = M4.1）。
- **已知风险（md/notify 先例登记）**：① 平台 IoC 回归 `nopSequenceGenerator` self-wait——若复现按先例修复（test-scope VFS delta；ct 现有 `test-ct-delta` 仅覆盖 erp/ct 域 beans，需新增 `nop/sys/beans/app-dao.beans.xml` delta）；② no-am 测试 classpath VFS 模块集变化（已登记中间态，回退方案 = seq-proof-yaml 模块禁用模式）；③ 陈旧 jar 二进制不兼容（本地仓未迁移 pur/sal dao jar——ct 域级测试按设计不跨这些边界）。
- **回写机制（M0.1 裁定 Decision A，三步）**：① dry-run 时点刷新；② `node tools/verify-id-fix-copy-diff.mjs module-contract` 新鲜度门控；③ 单文件落源 + `git diff` 逐行审核。禁止盲 cp、禁止 apply 模式。
- **剩余差距**：contract orm 47 列全 `stdDataType="long"` 待改；ct 手写代码/测试/快照全部 Long 形态；冻结序位次 5（之后位次 6 finance 待迁移）。

## Goals

- contract 域 47 列（自有 43 + md stub 4）`stdDataType` long→string 落源（唯一源文件变更，`stdSqlType` 保持 BIGINT，DDL 零变化）。
- 增量重生成（no-am 7 模块链）+ 编译器驱动修复 ct 全部手写代码（dao ~12 + service ~15 文件 Long 语义）+ A2 前向桥接 20 处落桥（pur 10 + sal 10，退役 owner M2.5/M2.6）。
- 快照每域重录（RECORDING→CHECKING，用户裁决——不依赖 Number 宽容；540 文件基线）。
- 语义陷阱 grep 门控清零（路线图横切 §3 清单，ct 范围）。
- 消费 M0.2 登记册：A2/A3 桥接 disposition 落盘本计划，C1/C2 修复定位面消费，heal M1.1 登记的 ct-dao `_gen` 对称胶水中间态；兑付 M1.2 登记的 notify test 后向 successor 义务（5 文件）。
- 路线图 M3.6 → `done` + 日志；冻结序位次 6（finance）解锁。

## Non-Goals

- 不修复外域代码对 contract 的引用（被引用面仅 purchase/sales main 各 1 + test 各 1 文件——pur/sal plan（M2.5/M2.6）各自的 C 定位面，非本计划义务；本计划仅在 Phase 4 登记确认）。
- 不迁移 purchase/sales 域（A2 桥接目标域，归 M2.5/M2.6）。
- 不改 `delVersion` 等非 PK/FK BIGINT 列（保持 long）。
- 不跑全量构建/全量测试/E2E（归 M4.1）；不跑 compliance checker（归 M4.1）。
- 不手改任何生成件（`_gen/`、`_` 前缀文件、api beans）；手写 view.xml 预期零改动（先例已实证，Phase 4 验证）。
- 不修 `ErpCtWebPagesTest` 的治理排除（已提交决策，successor = M4.1）。
- 不做 contract owner docs 之外的文档重写（`domain-design-guidelines.md` §16A 清理归 M4.1）。

## Task Route

- Type: `implementation-only change`（含保护区域 ORM 变更）
- Owner Docs: `docs/backlog/id-string-migration-roadmap.md` M2/M3 表位次 5 + 横切 §5 设计证据（`../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md` §主键设计方案 B + `docs/design/domain-design-guidelines.md` §16A.4 + M0.1 审计结论 + M0 裁决 §10）；contract 业务语义 owner doc = `docs/design/contract/`（Phase 4 注记对象）
- Skill Selection Basis: 路线图 §M1-M3「预期技能」指定域迁移 plan 加载 `nop-backend-dev` + `nop-testing`；ORM 变更机制由 M0.1 审计与平台文档背书。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（无 DB DDL 变更；DB 列保持 BIGINT）。no-am 构建硬前置 = 最后全绿基线 commit 全量 install + md/notify/common 链 install（已就位）+ pur-dao/sal-dao 基线 jar（未迁移 Long，正是桥接对象）+ **批内前置：位次 3 aps、位次 4 b2b 链 install**（冻结序执行约束——contract 精确前置仅为 M1.1+M1.2，但本批按冻结总序在 aps/b2b 之后执行）。回滚策略：revert orm.xml + `mvn clean install -pl module-contract/erp-ct-codegen,module-contract/erp-ct-dao,module-contract/erp-ct-meta,module-contract/erp-ct-service,module-contract/erp-ct-web,module-contract/erp-ct-app,module-contract/erp-ct-api -DskipTests` 重生成回 Long 形态。

## Execution Plan

### Phase 1 - 消费登记册 + orm 回写（保护区域，双批准前置）

Status: completed（2026-08-21 执行记录：登记册消费核对一致——(i) A1 orm 延后 = 0（登记册 8 条 orm-column-deferral 全为 fin ×6 + hr ×2，零 contract 条目）；(ii) A2 main 桥接 = bridge-main-033..052 恰 20 条（pur 10 = 033/034/037/039/040/043/044/047/049/050；sal 10 = 035/036/038/041/042/045/046/048/051/052，跨 6 文件）与本地实测 import 逐行对账一致（双源复核通过，20 import 行 verbatim 吻合）；(iii) A3 = bridge-test-108..112（5 测试文件，retireOwner M3.6 = 本计划）作为 Phase 3 定位面；另按 b2b A3' 先例做 FQN 盲区复扫（`rg 'app\.erp\.(pur|sal|inv|mfg|fin|prj|qa|mnt|ast)\.' module-contract/erp-ct-service/src/test` 排除 import 行）= **零命中**——contract 无 FQN 级未登记耦合，无 A3' 补登需求，无 rule-6 触发；(iv) C1 = backward-139（md 2 main 文件）+ backward-140（notify 6 main 文件）；C2 = backward-199（md 13 测试文件）+ backward-200（notify 5 测试文件，交集在案）——定位面在案，零冲突。双独立子 agent 批准已在案（Draft Review Record 批准 1 ses_fdb725cdeffeUnmssBAetMnLC6 + 批准 2 ses_fdb723de1ffeHnCCPMyTKFlsXe，2026-08-21）。回写三步执行：dry-run 时点刷新（全量副本 1513 列、登记册延后列已排除）→ `verify-id-fix-copy-diff.mjs module-contract` 门控通过（47 变更行、零非 stdDataType 行、延后列 0）→ 单文件 cp 落源。git diff 归一化逐行核对：47/47 字节一致（归一化 −/+ 集合 byte-identical），方向 47 `long`→47 `string`，94 变更行全含 `stdSqlType="BIGINT"`（0 缺失），全文件 `stdSqlType="BIGINT"` 计数 64=64 不变，`delVersion` ×15/`fileSize`/标签结构零变化。工具重扫 contract 段：47 列全 `ok`（含 4 md stub `(notGenCode) ok`，stub 翻转 = 与 md 权威源对齐），零 `NEEDS FIX`/零 `DEFERRED`。xmllint `ext:`/`ui:` 前缀提示为 19 orm 共有既有形态（非本次引入，dry-run XML 校验 15/15 通过）。回写前 scan 基线 = 恰 47 NEEDS FIX（自有 43 = PK 15 + FK 28 + stub 4），与 Current Baseline 精确吻合）
Targets: `module-contract/model/app-erp-contract.orm.xml`
Skill: none

- Item Types: `Proof | Fix`
- Prereqs: M1.1 ✅ + M1.2 ✅ + M0.2 ✅（精确前置已满足）；冻结序位次 3（aps）/4（b2b）done（批内执行顺序）；本计划已通过独立 plan-audit + 第二独立子 agent 复核（保护区域 `auto + dual-agent-approval`，批准记录落盘 Draft Review Record）

- [x] Proof: 消费 M0.2 登记册——读取 `tools/id-migration-registry.json5` + 登记册文档 §6.5 contract 节，逐条核对：(i) A1 orm 延后 = 0（47 列全翻转）；(ii) A2 main 桥接 20 条（pur 10 + sal 10）与本地实测 import 对账（本计划 Current Baseline 已预对账一致，执行时双源复核）；(iii) A3 test 桥接 5 条作为 Phase 3 定位面；(iv) C1 后向 2 条（md 2 main + notify 6 main 文件）与 C2 后向 2 条（md 13 + notify 5 测试文件，含交集）作为 Phase 2/3 定位面。矛盾则按路线图规则 6 停止回报。
  - Skill: none
- [x] Proof: 双独立子 agent 批准记录落盘（批准人指针 + 结论 + 时间），未获批不得进入回写。
  - Skill: none
- [x] Fix: 回写 orm（M0.1 裁定三步机制）——① `node tools/check-bigint-id-types.mjs dry-run` 时点刷新；② `node tools/verify-id-fix-copy-diff.mjs module-contract` 新鲜度门控（零非 stdDataType 行）；③ 门控通过后单文件落源。禁止盲 cp 静态副本、禁止 apply 模式。
  - Skill: none
- [x] Proof: `git diff module-contract/model/app-erp-contract.orm.xml` 逐行核对——仅 47 列 `stdDataType="long"→"string"`（自有 43 = PK 15 + FK 28 + notGenCode stub 4），`stdSqlType` 零变化、`delVersion`/标签结构零变化；scan contract 段重扫零 `NEEDS FIX`/零 `DEFERRED` 残留。
  - Skill: none

Exit Criteria:

- [x] 登记册消费核对在案；双批准记录在案；新鲜度门控 + git diff + 工具重扫三重证明变更面精确 = 47 列 stdDataType

### Phase 2 - 增量重生成 + 主代码编译修复 + A2 桥接落桥

Status: completed（2026-08-21 执行记录：7 模块链 `clean install`（显式列表、no-am、`-Dmaven.test.skip=true`）**BUILD SUCCESS**（三轮编译器驱动：首轮 82 错/20 文件 → 二轮 13 错/7 文件 → 三轮全绿）。重生成验证：dao `_gen` 15 实体 String 化 + md 关系胶水 `internalSetRefEntity` 两端同 String 自愈（M1.1 登记中间态 D3 兑现）；meta `_templates` 30 + web `_gen` view 15 + api beans 30 全部随动重生成（Long→String）；**手写 view 零改动**（git status 证实 web 仅 `_gen/` 前缀 15 文件变更）。
**主代码修复清单（编译器驱动，合计 dao 12 + service 26 文件；清单 27 项中 AbstractErpCtSignatureRequestProcessor 零直接改动，经 signVersion String 化联动自愈）**：
- dao 手写 12 文件（Long→String 全量翻转，42 行）：IBiz ×10（Contract 8 方法/ContractVersion 2/ApprovalRecord 3/RebateAgreement 1/RebateSettlement 1/Document 6/InvoicePlan 3/SignatureRequest 4/VolumeDiscount 1/ConsumptionLine 1）+ 值对象 ×2（ErpCtConsumptionPeriodSummarizeResult contractLineId/overagePlanId、ErpCtInvoicePlanGenerateItem contractLineId）。
- service 手写 26 文件（清单 27 项，1 项联动自愈）：BizModel ×10（Contract 24 Long 位、ContractVersion、ConsumptionLine、ApprovalRecord 6 位、Document 7 位 + `get(String.valueOf)` 直传简化、RebateAgreement 3 位 + 桥、RebateSettlement 8 位 + 桥、SignatureRequest 4 位 + `requireEntity` 直传、VolumeDiscount 2 位、InvoicePlan 3 入口 + requirePlan + 桥 ×12 位）；Processor ×12（InvoicePlanTriggerInvoice 2 + 桥、RebateSettlementPostSettlement 12 位 + 桥、RebateAgreementRunAccrual 3 + 语义桥、InvoicePlanTriggerDuePlans、InvoicePlanGenerateByTerm 4 位、ContractVersionSignVersion 3、ContractAmend 3、ContractActivate 3、ConsumptionPeriodSummarize 3、Abstract SignatureRequest（经 signVersion 联动自愈）、SignatureRequestInit、SignatureRequestQueryAndUpdateStatus）；Job ×1（ApprovalTimeoutEscalationJob 2 位）；Engine ×2（ApprovalWorkflowEngine 7 签名 + `record.getId() > latest.getId()` id 序比较改 `idOrder`（Long.compare(ConvertHelper.toLong) 保序语义）、RebateEngine 3 位）；SPI model ×2（OcrRecognizeRequest documentId、SignatureInitRequest contractVersionId）。
**A2 桥接例外清单（20 条处置，退役 owner M2.5 pur 10 条 / M2.6 sal 10 条，代码内 bridge 注释双向指针在案）**：
| 条目 | file:line（落桥后） | 转换方向 |
| --- | --- | --- |
| bridge-main-033/035 | ErpCtInvoicePlanBizModel createAp/ArInvoiceDraft：`ConvertHelper.toLong(orgId/partnerId(customerId)/currencyId)` ×6 | ct String → pur/sal Long |
| bridge-main-034/036 | ErpCtInvoicePlanBizModel invLine：`ConvertHelper.toLong(materialId)` + `toLong(material.getUoMId())` | ct/md String → pur/sal Long |
| bridge-main-037/047 | ErpCtRebateAgreementBizModel / RunAccrualProcessor findPeriodInvoices：`eq("supplierId", ConvertHelper.toLong(partnerId))` 过滤值桥 | ct String → pur Long |
| bridge-main-038/048 | 同上 sal 侧：`eq("customerId", ConvertHelper.toLong(partnerId))` | ct String → sal Long |
| bridge-main-039..042 | ErpCtRebateSettlementBizModel：setOrgId/setSupplierId(setCustomerId) toLong + resolveCurrencyId/MaterialId/UoMId 返回 Long 桥（ct/md String → toLong） | ct/md String → pur/sal Long |
| bridge-main-043..046 | ErpCtInvoicePlanTriggerInvoiceProcessor：同 033-036 型 | ct/md String → pur/sal Long |
| bridge-main-049..052 | ErpCtRebateSettlementPostSettlementProcessor：同 039-042 型 + `resolveUoMId(ConvertHelper.toString(materialId))` 调用点转换 | ct/md String ↔ pur/sal Long |
说明：`invoice.getId() → invLine.setInvoiceId(...)` 为 pur/sal 内部 Long 流（非桥接点）；037/038/047/048 为语义级过滤值桥（编译器不报错，主动识别——eq 过滤 Long 列传 String 会静默空匹配）。
**C1 后向修复**：md 侧（backward-139）——`ErpCtRebateSettlementBizModel`/`PostSettlementProcessor` 的 `getEntityById(materialId)` String 直传 + md uoMId 读取处 toLong 桥（md 自 M1.1 String，适配完成）；notify 侧（backward-140）——6 文件 `notify(String,Map,ctx)` 签名不变零编译破坏（M1.2 登记的「main 侧零破坏」核证成立，BUILD SUCCESS 佐证）。
**自身链破坏处置**：no-am 口径下零未登记破坏（7 模块全绿，reactor 不含外域模块）。）
Targets: `module-contract/erp-ct-dao/src/main/java/**`、`module-contract/erp-ct-service/src/main/java/**`（手写 IBiz/BizModel/Processor/Job/Engine/SPI；web main 手写实测 0；api beans 生成件随动）
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: Phase 1

- [x] Fix: `mvn clean install -pl module-contract/erp-ct-codegen,module-contract/erp-ct-dao,module-contract/erp-ct-meta,module-contract/erp-ct-service,module-contract/erp-ct-web,module-contract/erp-ct-app,module-contract/erp-ct-api -Dmaven.test.skip=true`（D3 口径：7 模块显式列表、不带 `-am`、`-Dmaven.test.skip=true`）触发增量重生成。预期：ct-dao `_gen` md 关系胶水（13 处 md refEntityName 对应）自 M1.1 的登记中间态自愈。
  - Skill: `nop-backend-dev`
- [x] Fix: 编译器驱动修复主代码——逐条修复 ct dao + service 手写代码类型错误（基线预判：dao 12 文件 IBiz 签名/值对象、service 26 文件 Long 语义 + 74 处 `.getId()` 下游；以编译器实际清单为准），直到 7 模块链 `-Dmaven.test.skip=true` 构建全绿。修复清单落盘本计划；测试编译错误由 Phase 3 首轮 `test-compile` 产生后修复（`-Dmaven.test.skip=true` 阶段不编译测试）。
  - Skill: `nop-backend-dev`
- [x] Fix: A2 前向桥接 20 处落桥（D4 消费协议）——ct String id ↔ pur/sal Long 发票实体 API 的调用点加转换桥（6 文件：InvoicePlanBizModel/RebateAgreementBizModel/RebateSettlementBizModel/InvoicePlanTriggerInvoiceProcessor/RebateAgreementRunAccrualProcessor/RebateSettlementPostSettlementProcessor），每处登记 grep 例外清单（条目 id + file:line + 转换方向），退役 owner M2.5（pur 10 条）/M2.6（sal 10 条）。
  - Skill: `nop-backend-dev`
- [x] Fix: C1 后向修复——md/notify 引用点对已 String 化 API 的编译适配（编译器驱动，定位面 = backward-139（md 2 文件）/backward-140（notify 6 文件）；notify 侧调用面预期 `notify(String,Map,ctx)` 签名不变——M1.2 已证，编译核验即可）。
  - Skill: `nop-backend-dev`
- [x] Fix: 自身链破坏处置（D4 carve-out）——no-am 口径下预期零外域破坏；未登记破坏按路线图规则 6 停止回报；已登记破坏按中间态继续并履行登记义务。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] ct 7 模块链（显式列表、no-am、`-Dmaven.test.skip=true`）构建全绿（main 代码）；主代码修复清单 + A2 桥接例外清单在案

### Phase 3 - 测试修复 + A3 桥接适配 + 快照重录 + 域级测试

Status: completed（2026-08-21 执行记录：首轮 `test-compile` 产生 199 错/11 文件 → 二轮 14 错/8 文件 → 三轮零编译错；另 3 文件（TestErpCtTerminateGate/TestErpCtContractRebate/TestErpCtContractTerminate）编译可过但含 gated 模式（`longValue`/`parseLong` helper），同批翻转（合计 **14 文件**）：TestErpCtESignature（seedContract/seedVersion/seedPartner/reload 签名 String 化）、TestErpCtContractExpiryJob（seedTemplate id 字面量 8831L→"8831" + payloadContractId 返 String + `== c15` 字典序陷阱改 `.equals` + `: 0` 三元改 `: null`）、TestErpCtDocumentRepository（88001L→"88001" + `.getId().longValue()` 去 longValue）、TestErpCtDocumentGuards/DocRetention（setOrgId "1" + seedTemplate 字面量）、TestErpCtContractCreateValidate/ApprovalWorkflow/ApprovalTimeoutJob（toLong/toLongId helper 返 String.valueOf + notify 模板字面量 String 化）、TestErpCtContractTerminate（0L 哨兵改 null）、TestErpCtTerminateGate（8811L/8812L→String + helper）、TestErpCtBillingFamily（summarize "999999" + `orm_propValueByName("id","8331")` notify seed M1.2 String 适配 + toLong helper）。
**A3 桥接适配（bridge-test-108..112 兑付，4 实转换点 + 读路径核验）**：TestErpCtContractRebate（bridge-test-111）`createPostedApInvoice` 的 `setSupplierId/setCurrencyId(ConvertHelper.toLong(...))` pur 桥 ×2；TestErpCtRebateSettlementEnd（bridge-test-112）`createPostedArInvoice` 的 `setCustomerId/setCurrencyId(ConvertHelper.toLong(...))` sal 桥 ×2；TestErpCtBillingFamily（108）/TestErpCtContractExpiryJob（109）/TestErpCtContractPosting（110）pur/sal 实体仅按 `code`（String）读路径访问（findFirstByQuery/findAllByQuery + 非断言 id 字段）——**零 id 穿越**，类型级引用核验结论（与登记册「类型级引用」注记吻合）。C2 后向兑付（backward-199 md 13 文件 + backward-200 notify 5 文件，交集在案）全部含于上述 14 文件修复（md 字面量 String 化 + notify 模板 seed `orm_propValueByName("id", String)`）。
**平台 IoC 回归复现与修复**：首轮 `mvn test` 17 错类挂 `nop.err.ioc.bean-init-self-wait(nopSequenceGenerator)`（已知风险①如期复现，119 处）——按 md/notify/aps/b2b 先例新增 test-scope VFS delta `_vfs/_delta/default/nop/sys/beans/app-dao.beans.xml`（ioc:lazy-property 镜像，注释指向本 plan）；DeltaOverride 测试 `delta-layer-ids` 由替换 default 层集改为 `value = "default,test-ct-delta"`（aps/b2b 先例）。
**id 序语义修复**：TestErpCtApprovalWorkflow 自有 `latestRecord` helper 用 `Comparator.comparing(getId)`——String 后变字典序（"9" > "10"）致 reject 拿旧驳回行；改 `comparingLong(r -> ConvertHelper.toLong(r.getId()))` 数值序（与主代码 `ErpCtApprovalWorkflowEngine.idOrder` 同型）。
**快照重录（RECORDING→CHECKING）**：17 个快照测试类临时加 `snapshotTest = SnapshotTest.RECORDING` 运行（168 方法，136 个预期 `snapshot-finished` 异常，**零真实失败**）→ 足迹 = 29 文件内容 diff（9 json5 = CrudSmoke 输出 id String 形态实证（`"id": "2"`/`"partnerId": "1"`）+ 字段序随 api beans 重生成（orgId/currencyId 等后移）+ 20 csv = erp_md_material 列刷新/CRLF）+ **1118 新增 table 快照落盘**（此前无 input/output tables 的方法全量首录；540 文件基线 → 1658）→ 逐案审核发现 5 处非确定性单元格（新录 `erp_sys_notification` 表把 `System.nanoTime()` 派生合同编码冻进 SUBJECT/BODY/PAYLOAD_JSON）→ 按 aps 先例 `*` 通配确定性修正（5 文件 ×3 列）→ 注解还原（grep RECORDING/forceSaveOutput 零残留）→ CHECKING 复跑 **168/168 全绿 ×2 次**（稳定性确认）。
**域级测试命令**：`mvn test -pl module-contract/erp-ct-service,module-contract/erp-ct-web`（no-am）——service 22 测试类 168/168 绿 + web BUILD SUCCESS 0 tests（`ErpCtWebPagesTest` 治理排除预期，successor M4.1）。）
Targets: `module-contract/**/src/test/**`、`module-contract/erp-ct-service/_cases/**`
Skill: `nop-testing`

- Item Types: `Fix | Proof`
- Prereqs: Phase 2

- [x] Fix: 测试代码修复——22 个 service 测试类的 Long 用法（字面量断言、helper 签名、seed `orm_propValueByName("id", id)` 形态——md/notify 先例），逐文件修复至测试编译通过；测试编译错误由本轮 `test-compile` 产生（`-Dmaven.test.skip=true` 阶段不编译测试）。
  - Skill: `nop-testing`
- [x] Fix: A3 test 桥接适配（5 文件，登记册 bridge-test-108..112）——本域测试引用 pur/sal 发票实体的 id 形态桥接（String↔Long 局部转换，与 Phase 2 桥接同型），适配后在登记册退役对应 test 桥接条目（owner M3.6 = 本计划）。
  - Skill: `nop-testing`
- [x] Fix: C2 后向 test 适配——md 13 + notify 5 测试文件（含交集）对 String 化 API 的适配（M1.2 登记的 notify successor 义务兑付；md 侧自 M1.1 即 String，为存量后向修复面）。
  - Skill: `nop-testing`
- [x] Fix: 快照每域重录（用户裁决固定步骤）——`RECORDING` 模式运行 ct service 测试 → 逐案审核 `_cases/` 新形态（540 文件基线：393 csv + 137 yaml + 10 json5；id 以 String 形态落盘）→ 注解还原（grep 零 RECORDING/forceSaveOutput 残留）→ 切回 `CHECKING` 复跑确认全绿。重录足迹（内容 diff vs 新增落盘分列）与审核结论记录本计划。
  - Skill: `nop-testing`
- [x] Proof: `mvn test -pl module-contract/erp-ct-service,module-contract/erp-ct-web`（D3 口径：不带 `-am`）全绿——service 22 测试类 + web BUILD SUCCESS（`ErpCtWebPagesTest` 治理排除，0 tests 预期）。若复现平台 IoC 回归，按 md/notify 先例修复（test-scope VFS delta）并登记。
  - Skill: `nop-testing`

Exit Criteria:

- [x] ct 域级测试全绿（service 22 类；web 治理排除偏差登记）；快照重录完成且 `CHECKING` 复跑通过；重录清单在案

### Phase 4 - 语义陷阱 grep 门控 + 收尾登记

Status: completed（2026-08-21 执行记录：**grep 门控结果逐项**——① `\.longValue\(\)`：0 命中；② `Long\.parseLong\(`：0 命中；③ `Map<Long`：0 命中；④ `Set<Long`：0 命中；⑤ `String\.format\("%d`/`%d` 变体：0 命中（仅 TestErpCtESignature HMAC `%02x` hex 编码，非 id，b2b 先例同类豁免）；⑥ Long 装箱 id `==`/`!=`：0 命中（唯一 `ErpCtVolumeDiscountBizModel:109` `getId() != null && getId().equals(...)` = null 检查 + String.equals，合法）；⑦ 残留 `Long` 逐条核清 = `_gen` delVersion ×15 + fileSize（生成件非 PK/FK，规则 4 合法）+ A2 桥接本体（ErpCtRebateSettlementBizModel/PostSettlementProcessor `Long currencyId/materialId/uomId` 局部变量 + resolve* Long 返回桥（bridge-main-039..042/049..052 注释在案）+ `ErpCtApprovalWorkflowEngine.idOrder` Long.compare(ConvertHelper.toLong) id 序比较（seq-string 数值序语义保留））+ bridge 注释文案；test 侧残留 Long = 0；⑧ `sql-lib.xml` 仓内零存在（M0.1 已核，本计划复核维持）。**view 零手改动验证**：`git status module-contract/erp-ct-web` = 仅 15 个 `_gen/` 前缀 view 变更（codegen 随动），手写 view 零被动变更（Closure 复核 `_vfs/erp/ct/pages/` 非 `_gen` 变更 = 0）。**结束审计发现项处置（iteration 1 MAJOR-1 兑付）**：独立结束审计发现手写 raw-GraphQL 页面 `dashboard/version-diff.page.yaml:111` 声明 `$aid:Long,$bid:Long` 传给已 String 化的 `ErpCtContractVersion__get(id:)`（静态变量类型不匹配 → adaptor 静默降级 hasCompare:false，对比版本功能失效——迁移引发的已确认实时缺陷，按不可降级规则就地 Fix 而非 follow-up）：改 `$aid:String,$bid:String` + variables 兜底 `|| 0`（Int 字面量违反 String 变量类型）改 `|| ''`；options value 链一致性核证（`value: v.id` ← findPage String id）；ct 域 `:Long` 残留全量清扫 = 零（`$lim:Int` 为 limit 非 id，合法保留）；ct-web 模块重建 BUILD SUCCESS。同型未登记模式存在于已审计域（notify inbox.page.yaml/aps schedule-gantt/b2b edi-detail/asn-flow——mission 级盲区），非本计划义务，登记于 Deferred But Adjudicated「跨域手写 page.yaml raw-GraphQL `:Long` 变量声明」条目（successor = M4.1 mission 级 page.yaml `:Long` 全量清扫，roadmap M4.1 行同步登记）。**登记册更新**：bridge-test-108..112 status → retired（owner M3.6 兑付 note：108/109/110 读路径核验零 id 穿越 + 111/112 ConvertHelper.toLong ×2 落桥各侧；晚域 M2.5/M2.6 翻转时移除桥接点/复核读路径）；JSON5 解析复核通过（255 entries）；counts 不变（条目总数口径，b2b 补登先例同）；A2 main 桥接 20 条（bridge-main-033..052）保持 active（退役 owner M2.5/M2.6，代码内 bridge 注释双向指针在案）；被引用面确认（purchase/sales main 各 1 + test 各 1 文件引用本域，successor = M2.5/M2.6 各自 C 定位面——D4 已登记中间态）。**owner doc 注记**：grep `docs/design/contract/` 8 文件 = 2 命中（contract-repository.md:186 fileSize 数字区间过滤示例、e-signature.md:141 数字签名摘要）均为非 id 语境——**零 Long id 陈述，零文档变更**。**roadmap + 日志**：M3.6 → done（位次 5 行证据摘要 + 头部最后更新 + 位次 6 finance 解锁含 web/app 延后 caveat）+ `docs/logs/2026/08-21.md` 条目（含验证状态全绿）。）
Targets: `module-contract/**`（手写代码）、`docs/backlog/id-string-migration-roadmap.md`、`docs/logs/2026/{08-21 或执行日}.md`、`tools/id-migration-registry.json5`
Skill: none

- Item Types: `Proof | Add`
- Prereqs: Phase 3

- [x] Proof: 语义陷阱 grep 门控（路线图横切 §3，ct 手写 main+test 范围）清零——`\.longValue\(\)`、`Long\.parseLong\(`、`Map<Long`、`Set<Long`、`String\.format\("%d` 及 `%d` 变体零命中（A2 桥接转换点为登记例外，逐条列于例外清单并标注退役 owner M2.5/M2.6）；Long 装箱 `==`/`!=` 比较（id 上下文）逐条核清；残留 `Long` 逐条判定合法非 id 或登记 successor；sql-lib.xml 仓内零存在（M0.1 已核，注明即可）。结果逐项记录本计划。
  - Skill: none
- [x] Proof: 手写 view.xml 零改动验证——`git status module-contract/erp-ct-web` 确认无手写 view 文件被动变更（生成 view 随 codegen 更新不在此列）。
  - Skill: none
- [x] Add: 登记册状态更新——A3 test 桥接 5 条（bridge-test-108..112）status → retired（owner M3.6）；A2 main 桥接 20 条保持 active（退役 owner M2.5/M2.6，晚域翻转时退役并移除本域桥接点——例外清单留双向指针）；被引用面确认（purchase/sales main 各 1 + test 各 1 文件，successor = M2.5/M2.6 各自 C 定位面）。
  - Skill: none
- [x] Add: owner doc 注记——grep `docs/design/contract/` 中关于 contract id 为 Long/数字的陈述；存在则就地注记 Java 层已 String 化（引用本计划），不存在则记录「零 Long id 陈述，零文档变更」结论。
  - Skill: none
- [x] Add: 路线图 M3.6 → `done`（M2/M3 表位次 5 + 头部「最后更新」；位次 6 finance 解锁——注意 finance 为唯一全域构建纠缠域，其 web/app 重生成延后约束由其自身 plan 处理）+ 日志条目（含验证状态）。
  - Skill: none

Exit Criteria:

- [x] grep 门控零残留（例外为零或逐条核清记录 + 桥接例外清单在案）；view 零手改动在案
- [x] 路线图状态、登记册退役、日志三者一致

## Draft Review Record

- Independent draft review iteration 1（2026-08-21，双独立子 agent fresh session）：
  - 审查者 A（技术/执行视角 plan-audit，ses_fdb725cdeffeUnmssBAetMnLC6）：`passes draft review` — 0 BLOCKER / 0 MAJOR / 3 MINOR。事实核对全部属实（47 列机器复核精确；A2 20 条 6 文件行级 verbatim 吻合；A3/C1/C2 全对；被引用面 pur/sal main+test 各 1 文件实测确认；`_cases` 540 = 393+137+10 精确）。MINOR：① 「本域无 VARCHAR 主/外键」失实（实有 6 列 VARCHAR FK 本就 string）→ 已修正；② contractId ×4 → ×5 → 已修正；③ service Long 文件 ~15 → 实测 26（BizModel 10/Processor 11/Job 1/Engine 2/SPI model 2，全含 id 相关 Long）→ 已修正。
  - 审查者 B（治理/规范视角，ses_fdb723de1ffeHnCCPMyTKFlsXe）：`passes draft review` — 0 BLOCKER / 0 MAJOR / 3 MINOR。模板/反松弛/Item Types/Skill/Non-Goals/路线图对齐（含 finance 解锁 caveat 准确性）/登记册 §1 协议/保护区域三触点/md-stub 呈现/被引用面分类全部 PASS。MINOR：① contractId ×5 → 已修正；② 命名注记不完整（dao 生成实体包 `app.erp.contract.dao.entity` vs 手写 `app.erp.ct.*` 双轨）→ 已补全；③ WebPagesTest 缺 Deferred 条目 → 已补。
  - 批准后 MINOR 修订注记：全部 MINOR 均为事实精度/格式补齐，未变更范围、D3 命令口径与批准依据（orm 变更面仍 = 47 列 stdDataType long→string）。
  - **双独立子 agent 批准（保护区域 `model/*.orm.xml`，`ai-autonomy-policy.md` `auto + dual-agent-approval`）**：
    - 批准 1（技术视角）：ses_fdb725cdeffeUnmssBAetMnLC6，2026-08-21 — 「批准 M3.6 contract orm 保护区域变更（技术视角批准）」。依据：方案 B 机制（stdDataType only、stdSqlType BIGINT 不变、DDL 零变化）+ M0.1 seq-string Proof + M1.1/M1.2 绿先例 + 变更面机器复核（live vs `_tmp` diff 恰 47 stdDataType 翻转，余 16 long = 15 delVersion + 1 fileSize 正确保留）+ 4 stub 翻转 = 与 md 权威源对齐。
    - 批准 2（治理视角）：ses_fdb723de1ffeHnCCPMyTKFlsXe，2026-08-21 — 「批准 M3.6 contract orm 保护区域变更（治理视角批准）」。依据：五要素证据链（平台 design doc 方案 B :186-206 + 强制规则 :30/:199 + `domain-design-guidelines.md` §16A.4 :667 + M0.1 Proof + M1.1 双批准先例 + M0 裁决 D3/D4/D6）；变更面 = 47 列 stdDataType、stdSqlType 不变 DDL 零变化。
- 共识达成（2026-08-21）：iteration 1 双审查者 0 BLOCKER / 0 MAJOR + 保护区域双批准 → 计划转 `active`。

## Closure Gates

> 完整仓库验证定制为域级口径（路线图规则 3 D3 修订：禁止以全量构建为中间 gate；全量构建仅存在于 M4.1）。

- [x] 范围内行为完成（47 列落源 + no-am 重生成 + 手写代码/测试修复 + A2/A3 桥接落桥与退役 + 快照重录 + grep 门控清零）
- [x] 相关文档对齐（owner doc 零 Long id 陈述结论（零文档变更）、路线图 M3.6 状态、登记册退役（bridge-test-108..112 retired）、日志）
- [x] 已运行验证：`mvn clean install -pl module-contract/erp-ct-codegen,module-contract/erp-ct-dao,module-contract/erp-ct-meta,module-contract/erp-ct-service,module-contract/erp-ct-web,module-contract/erp-ct-app,module-contract/erp-ct-api -DskipTests` 全绿 + `mvn test -pl module-contract/erp-ct-service,module-contract/erp-ct-web` 全绿 + 工具重扫零残留（contract 段 `NEEDS FIX` = 0）
- [x] 无范围内项目降级为 deferred/follow-up（web 页面测试治理排除为已提交决策 + M4.1 successor 登记，属偏差登记而非范围降级；结束审计 MAJOR-1 version-diff.page.yaml 为就地 Fix 非降级；跨域 page.yaml `:Long` 存量实例为外域资产 watch-only 登记，successor M4.1）
- [x] 保护区域双独立子 agent 批准记录落盘（Phase 1 前置）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（ses_fdb08d496ffevkOIg0uS8631VX，iteration 1→3，见 Closure Audit Evidence）
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### `ErpCtWebPagesTest` 页面校验

- Classification: `watch-only residual`
- Why Not Blocking Closure: `@Tag("full-app")` + surefire excludedGroups 为先于本 mission 的已提交治理决策（plan 2026-07-24-0930-1），实证依赖全量 classpath
- Successor Required: `yes`（M4.1 app-erp-all `ErpAllWebPagesTest`）

### A2 main 桥接 20 处（pur 10 + sal 10，String↔Long 临时转换）

- Classification: `watch-only residual`
- Why Not Blocking Closure: D4 登记册预先登记的中间态桥接——purchase/sales 未迁移（位次 15/16），桥接点为编译必需；退役 owner M2.5/M2.6 翻转对应域时退役条目并移除桥接点
- Successor Required: `yes`（M2.5 回收 pur 10 条；M2.6 回收 sal 10 条）

### purchase/sales 对 contract 的被引用面（main 各 1 + test 各 1 文件）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 晚域（pur/sal）代码引用本域 String API 的编译破坏属预期中间态（登记册被引用清单），本域 verify 闭包（7 模块自身链，no-am）不含外域模块
- Successor Required: `yes`（M2.5/M2.6 各自 plan 的 C 定位面；M4.1 兜底）

### 平台 IoC 回归 delta（若 Phase 3 复现并落盘）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 平台 `nopSequenceGenerator` bean-init-self-wait 为已登记平台 Bug，先例修复 = test-scope VFS delta
- Successor Required: `yes`（平台修复后统一移除，M4.1 复核）

### 跨域手写 page.yaml raw-GraphQL `:Long` 变量声明（mission 级盲区，M3.6 结束审计发现）

- Classification: `watch-only residual`
- Why Not Blocking Closure: ct 域实例（version-diff.page.yaml）已在本计划 Phase 4 就地 Fix（String 化）；已审计域存量同型实例（notify inbox.page.yaml:177、aps schedule-gantt:59、b2b edi-detail:45/asn-flow:79）为各域资产外的 mission 级模式盲区——dict 先例「手写 view 零改动」口径未覆盖 raw-GraphQL 变量声明，M1.1-M3.8 各 plan 未登记；非本计划范围（不改外域代码）
- Successor Required: `yes`（M4.1 mission 级 page.yaml `:Long` 变量全量清扫 + 各域运行时页面验证；roadmap M4.1 行已登记）

## Closure

Status Note: completed（2026-08-21：四 Phase 全部执行完毕 + 独立结束审计 iteration 3 `passes closure audit`）

Closure Audit Evidence:

- **独立结束审计（ses_fdb08d496ffevkOIg0uS8631VX，2026-08-21，fresh session 仅审计零修改，共 3 轮）：iteration 3 终审 `passes closure audit` — 0 BLOCKER / 0 MAJOR / 0 MINOR**。
  - iteration 1（全量九项活仓核验）→ `needs revision`（1 MAJOR / 1 MINOR）：九项全 PASS（① orm 47 列 stdDataType-only 归一化 47/47 + stdSqlType BIGINT 64 保留 + 残留 16 long = delVersion ×15 + fileSize 正确保留 + scan 零 NEEDS FIX/DEFERRED；② 7 模块 no-am `-DskipTests` BUILD SUCCESS 7/7；③ service 168/168 绿 ×22 类 + web 0 tests（`@Tag("full-app")`:13 + pom excludedGroups:72 证实）；④ grep 门控五项零命中 + 残留 Long 36 处逐条 = A2 桥接本体/注释（20 条 bridge-main id 全在案）+ idOrder helper + `_gen` 规则 4；⑤ web 变更 15 全 `_gen/`；⑥ 登记册 255 entries 解析通过 + bridge-test-108..112 retired（4 处 ConvertHelper.toLong 转换点 :473/:475/:241/:243 实证）+ bridge-main-033..052 20 条 active（retireOwner M2.5×10/M2.6×10 精确）；⑦ roadmap/log/plan 三者一致 + owner doc 零 Long id 陈述核证；⑧ 快照卫生零 RECORDING/forceSaveOutput + `"id": "2"`/`"partnerId": "1"` String 实证 + delVersion 数字 + `_cases` 1658 = 540+1118 精确；⑨ IoC delta 存在（lazy-property 镜像 + plan 引用注释）+ DeltaOverride delta-layer 含 default）。**MAJOR-1**：`version-diff.page.yaml:111` 手写 raw-GraphQL `$aid:Long,$bid:Long` 对已 String 化 `get(id:)` 静态类型不匹配 → 对比功能静默失效（未登记迁移缺陷）；**MINOR-1**：Phase 2 计数 29 vs 实际 26/27。
  - iteration 2（处置复核）→ `needs revision`（0 BLOCKER / 2 MAJOR / 1 MINOR）：代码处置全部验证绿（String 修复 + `|| ''` 兜底 + options 链一致 + ct `:Long` 零残留 + web 重建 SUCCESS + 计数修正）；MAJOR-1 文档面 = 声称的 Follow-up 登记不存在（hollow claim）；MAJOR-2 = 日志未同步（「手写 view 零改动」与活仓 16 文件矛盾）；MINOR-1 = 残留「29 文件」清单头。
  - iteration 3（终审）→ `passes closure audit`（0/0/0）：Deferred But Adjudicated 条目 + roadmap M4.1 行登记 + 日志同步 + 计数清零全部落地；四 Phase completed 全 [x]；git diff 限定 3 文档（+83/+6/+11）无回归；version-diff.page.yaml String 修复保持。
- **验证状态（全绿，审计独立复跑确认）**：`mvn clean install -pl module-contract/erp-ct-{codegen,dao,meta,service,web,app,api} -DskipTests` BUILD SUCCESS（no-am 7/7，执行者 + 审计两侧复跑）+ `mvn test -pl module-contract/erp-ct-service,module-contract/erp-ct-web -o` service 168/168（执行者侧 3 轮 + 审计侧 1 轮全绿）+ `node tools/check-bigint-id-types.mjs` contract 段零残留 + ct-web 重建（含 version-diff Fix）BUILD SUCCESS。

Follow-up:

- （无范围内跟进项；已确认缺陷不得出现在此处。中间态 successor 指针见 Deferred But Adjudicated 与 Phase 4 登记记录；跨域 page.yaml `:Long` mission 级清扫归 M4.1（roadmap M4.1 行已登记）。）
