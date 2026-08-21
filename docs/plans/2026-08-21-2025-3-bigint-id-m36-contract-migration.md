# 2026-08-21-2025-3-bigint-id-m36-contract-migration 主键/外键 string 化 M3.6：contract 域迁移（冻结序位次 5）

> Plan Status: active（2026-08-21：iteration 1 双独立审查 `passes draft review`（0 BLOCKER / 0 MAJOR）+ 保护区域双独立子 agent 批准，见 Draft Review Record）
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

Status: planned
Targets: `module-contract/model/app-erp-contract.orm.xml`
Skill: none

- Item Types: `Proof | Fix`
- Prereqs: M1.1 ✅ + M1.2 ✅ + M0.2 ✅（精确前置已满足）；冻结序位次 3（aps）/4（b2b）done（批内执行顺序）；本计划已通过独立 plan-audit + 第二独立子 agent 复核（保护区域 `auto + dual-agent-approval`，批准记录落盘 Draft Review Record）

- [ ] Proof: 消费 M0.2 登记册——读取 `tools/id-migration-registry.json5` + 登记册文档 §6.5 contract 节，逐条核对：(i) A1 orm 延后 = 0（47 列全翻转）；(ii) A2 main 桥接 20 条（pur 10 + sal 10）与本地实测 import 对账（本计划 Current Baseline 已预对账一致，执行时双源复核）；(iii) A3 test 桥接 5 条作为 Phase 3 定位面；(iv) C1 后向 2 条（md 2 main + notify 6 main 文件）与 C2 后向 2 条（md 13 + notify 5 测试文件，含交集）作为 Phase 2/3 定位面。矛盾则按路线图规则 6 停止回报。
  - Skill: none
- [ ] Proof: 双独立子 agent 批准记录落盘（批准人指针 + 结论 + 时间），未获批不得进入回写。
  - Skill: none
- [ ] Fix: 回写 orm（M0.1 裁定三步机制）——① `node tools/check-bigint-id-types.mjs dry-run` 时点刷新；② `node tools/verify-id-fix-copy-diff.mjs module-contract` 新鲜度门控（零非 stdDataType 行）；③ 门控通过后单文件落源。禁止盲 cp 静态副本、禁止 apply 模式。
  - Skill: none
- [ ] Proof: `git diff module-contract/model/app-erp-contract.orm.xml` 逐行核对——仅 47 列 `stdDataType="long"→"string"`（自有 43 = PK 15 + FK 28 + notGenCode stub 4），`stdSqlType` 零变化、`delVersion`/标签结构零变化；scan contract 段重扫零 `NEEDS FIX`/零 `DEFERRED` 残留。
  - Skill: none

Exit Criteria:

- [ ] 登记册消费核对在案；双批准记录在案；新鲜度门控 + git diff + 工具重扫三重证明变更面精确 = 47 列 stdDataType

### Phase 2 - 增量重生成 + 主代码编译修复 + A2 桥接落桥

Status: planned
Targets: `module-contract/erp-ct-dao/src/main/java/**`、`module-contract/erp-ct-service/src/main/java/**`（手写 IBiz/BizModel/Processor/Job/Engine/SPI；web main 手写实测 0；api beans 生成件随动）
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: Phase 1

- [ ] Fix: `mvn clean install -pl module-contract/erp-ct-codegen,module-contract/erp-ct-dao,module-contract/erp-ct-meta,module-contract/erp-ct-service,module-contract/erp-ct-web,module-contract/erp-ct-app,module-contract/erp-ct-api -Dmaven.test.skip=true`（D3 口径：7 模块显式列表、不带 `-am`、`-Dmaven.test.skip=true`）触发增量重生成。预期：ct-dao `_gen` md 关系胶水（13 处 md refEntityName 对应）自 M1.1 的登记中间态自愈。
  - Skill: `nop-backend-dev`
- [ ] Fix: 编译器驱动修复主代码——逐条修复 ct dao + service 手写代码类型错误（基线预判：dao 12 文件 IBiz 签名/值对象、service 26 文件 Long 语义 + 74 处 `.getId()` 下游；以编译器实际清单为准），直到 7 模块链 `-Dmaven.test.skip=true` 构建全绿。修复清单落盘本计划；测试编译错误由 Phase 3 首轮 `test-compile` 产生后修复（`-Dmaven.test.skip=true` 阶段不编译测试）。
  - Skill: `nop-backend-dev`
- [ ] Fix: A2 前向桥接 20 处落桥（D4 消费协议）——ct String id ↔ pur/sal Long 发票实体 API 的调用点加转换桥（6 文件：InvoicePlanBizModel/RebateAgreementBizModel/RebateSettlementBizModel/InvoicePlanTriggerInvoiceProcessor/RebateAgreementRunAccrualProcessor/RebateSettlementPostSettlementProcessor），每处登记 grep 例外清单（条目 id + file:line + 转换方向），退役 owner M2.5（pur 10 条）/M2.6（sal 10 条）。
  - Skill: `nop-backend-dev`
- [ ] Fix: C1 后向修复——md/notify 引用点对已 String 化 API 的编译适配（编译器驱动，定位面 = backward-139（md 2 文件）/backward-140（notify 6 文件）；notify 侧调用面预期 `notify(String,Map,ctx)` 签名不变——M1.2 已证，编译核验即可）。
  - Skill: `nop-backend-dev`
- [ ] Fix: 自身链破坏处置（D4 carve-out）——no-am 口径下预期零外域破坏；未登记破坏按路线图规则 6 停止回报；已登记破坏按中间态继续并履行登记义务。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] ct 7 模块链（显式列表、no-am、`-Dmaven.test.skip=true`）构建全绿（main 代码）；主代码修复清单 + A2 桥接例外清单在案

### Phase 3 - 测试修复 + A3 桥接适配 + 快照重录 + 域级测试

Status: planned
Targets: `module-contract/**/src/test/**`、`module-contract/erp-ct-service/_cases/**`
Skill: `nop-testing`

- Item Types: `Fix | Proof`
- Prereqs: Phase 2

- [ ] Fix: 测试代码修复——22 个 service 测试类的 Long 用法（字面量断言、helper 签名、seed `orm_propValueByName("id", id)` 形态——md/notify 先例），逐文件修复至测试编译通过；测试编译错误由本轮 `test-compile` 产生（`-Dmaven.test.skip=true` 阶段不编译测试）。
  - Skill: `nop-testing`
- [ ] Fix: A3 test 桥接适配（5 文件，登记册 bridge-test-108..112）——本域测试引用 pur/sal 发票实体的 id 形态桥接（String↔Long 局部转换，与 Phase 2 桥接同型），适配后在登记册退役对应 test 桥接条目（owner M3.6 = 本计划）。
  - Skill: `nop-testing`
- [ ] Fix: C2 后向 test 适配——md 13 + notify 5 测试文件（含交集）对 String 化 API 的适配（M1.2 登记的 notify successor 义务兑付；md 侧自 M1.1 即 String，为存量后向修复面）。
  - Skill: `nop-testing`
- [ ] Fix: 快照每域重录（用户裁决固定步骤）——`RECORDING` 模式运行 ct service 测试 → 逐案审核 `_cases/` 新形态（540 文件基线：393 csv + 137 yaml + 10 json5；id 以 String 形态落盘）→ 注解还原（grep 零 RECORDING/forceSaveOutput 残留）→ 切回 `CHECKING` 复跑确认全绿。重录足迹（内容 diff vs 新增落盘分列）与审核结论记录本计划。
  - Skill: `nop-testing`
- [ ] Proof: `mvn test -pl module-contract/erp-ct-service,module-contract/erp-ct-web`（D3 口径：不带 `-am`）全绿——service 22 测试类 + web BUILD SUCCESS（`ErpCtWebPagesTest` 治理排除，0 tests 预期）。若复现平台 IoC 回归，按 md/notify 先例修复（test-scope VFS delta）并登记。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] ct 域级测试全绿（service 22 类；web 治理排除偏差登记）；快照重录完成且 `CHECKING` 复跑通过；重录清单在案

### Phase 4 - 语义陷阱 grep 门控 + 收尾登记

Status: planned
Targets: `module-contract/**`（手写代码）、`docs/backlog/id-string-migration-roadmap.md`、`docs/logs/2026/{08-21 或执行日}.md`、`tools/id-migration-registry.json5`
Skill: none

- Item Types: `Proof | Add`
- Prereqs: Phase 3

- [ ] Proof: 语义陷阱 grep 门控（路线图横切 §3，ct 手写 main+test 范围）清零——`\.longValue\(\)`、`Long\.parseLong\(`、`Map<Long`、`Set<Long`、`String\.format\("%d` 及 `%d` 变体零命中（A2 桥接转换点为登记例外，逐条列于例外清单并标注退役 owner M2.5/M2.6）；Long 装箱 `==`/`!=` 比较（id 上下文）逐条核清；残留 `Long` 逐条判定合法非 id 或登记 successor；sql-lib.xml 仓内零存在（M0.1 已核，注明即可）。结果逐项记录本计划。
  - Skill: none
- [ ] Proof: 手写 view.xml 零改动验证——`git status module-contract/erp-ct-web` 确认无手写 view 文件被动变更（生成 view 随 codegen 更新不在此列）。
  - Skill: none
- [ ] Add: 登记册状态更新——A3 test 桥接 5 条（bridge-test-108..112）status → retired（owner M3.6）；A2 main 桥接 20 条保持 active（退役 owner M2.5/M2.6，晚域翻转时退役并移除本域桥接点——例外清单留双向指针）；被引用面确认（purchase/sales main 各 1 + test 各 1 文件，successor = M2.5/M2.6 各自 C 定位面）。
  - Skill: none
- [ ] Add: owner doc 注记——grep `docs/design/contract/` 中关于 contract id 为 Long/数字的陈述；存在则就地注记 Java 层已 String 化（引用本计划），不存在则记录「零 Long id 陈述，零文档变更」结论。
  - Skill: none
- [ ] Add: 路线图 M3.6 → `done`（M2/M3 表位次 5 + 头部「最后更新」；位次 6 finance 解锁——注意 finance 为唯一全域构建纠缠域，其 web/app 重生成延后约束由其自身 plan 处理）+ 日志条目（含验证状态）。
  - Skill: none

Exit Criteria:

- [ ] grep 门控零残留（例外为零或逐条核清记录 + 桥接例外清单在案）；view 零手改动在案
- [ ] 路线图状态、登记册退役、日志三者一致

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

- [ ] 范围内行为完成（47 列落源 + no-am 重生成 + 手写代码/测试修复 + A2/A3 桥接落桥与退役 + 快照重录 + grep 门控清零）
- [ ] 相关文档对齐（owner doc 注记或零变更结论、路线图 M3.6 状态、登记册退役、日志）
- [ ] 已运行验证：`mvn clean install -pl module-contract/erp-ct-codegen,module-contract/erp-ct-dao,module-contract/erp-ct-meta,module-contract/erp-ct-service,module-contract/erp-ct-web,module-contract/erp-ct-app,module-contract/erp-ct-api -DskipTests` 全绿 + `mvn test -pl module-contract/erp-ct-service,module-contract/erp-ct-web` 全绿 + 工具重扫零残留（contract 段 `NEEDS FIX` = 0）
- [ ] 无范围内项目降级为 deferred/follow-up（web 页面测试治理排除为已提交决策 + M4.1 successor 登记，属偏差登记而非范围降级）
- [ ] 保护区域双独立子 agent 批准记录落盘（Phase 1 前置）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

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

## Closure

Status Note: （待结束审计）

Closure Audit Evidence:

- （待独立结束审计填充）

Follow-up:

- （无；已确认缺陷不得出现在此处。中间态 successor 指针见 Deferred But Adjudicated 与 Phase 4 登记记录）
