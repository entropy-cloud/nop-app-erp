# 2026-08-21-1045-3-bigint-id-m11-master-data-migration 主键/外键 string 化 M1.1：master-data 域迁移（先导试点）

> Plan Status: completed（2026-08-21，四 Phase 全部完成 + 独立结束审计 `passes closure audit`（ses_fdc859128ffeS7wxCsU1qyLwv0，见 Closure 节）；此前历程：Phase 2 rule-6 停止 → M0 裁决 Decision D 恢复 → Phase 2/3/4 完成）
> Mission: id-string-migration
> Work Item: M1.1
> Last Reviewed: 2026-08-21
> Source: `docs/backlog/id-string-migration-roadmap.md` M1.1
> Related: `docs/plans/2026-08-21-1045-1-bigint-id-m0-order-freeze-audit-proofs.md`（M0.1，前置）、`docs/plans/2026-08-21-1045-2-bigint-id-m13-common-service-orgid-string.md`（M1.3，前置）
> M0 裁决: `docs/audits/2026-08-21-1045-id-migration-m0-freeze-audit.md` §10 Decision D（2026-08-21，iteration 3 双独立子 agent passes）——D3 域级 verify 闭包收窄（自身模块链、去 `-am`）、D4 前向耦合登记册 carve-out、D5 文本修正随本 plan 恢复同批执行、D6 试点判据修订（md 成立）。本 plan Phase 2 退出标准与命令已按 D3/D5(c) 修订（范围变更理由：M0 裁决对路线图规则 3 的正式修订，非本 plan 自行缩小）
> Audit: required（保护区域 `model/*.orm.xml`：独立 plan-audit + 双独立子 agent 批准，批准记录落盘本文件）

## Current Baseline

- **master-data 域规模（2026-08-21 实况 scan，本文件自身口径）**：`module-master-data/model/app-erp-master-data.orm.xml` 需改列 **68 = PK 25 + FK 43**（含 6 个 orgId FK 列，路线图裁定 M1.3 须先行）。修正副本 `_tmp/bigint-id-string-fix/module-master-data/...` 为 08-13 产物，**当前已过期**：实况 diff 存在 3 处语义漂移约 6 diff 行（R1.40 `priceValidationLevel` defaultValue `20`→`WARN`、R1.45 `cashFlowType` 列新增，均 08-15；R1.72 SKU `status` 列 + 2 行注释，08-19）——盲 cp 将回滚这些经双批准落地的变更；全仓 13+/19 副本同病（M0.1 Phase 1 负责全量刷新，本计划回写仍须自带新鲜度门控，见 Phase 1）。
- **构建命令实测（关键）**：`module-master-data/` 聚合 pom（packaging=pom）但 **`-pl module-master-data -am` 不展开聚合器子模块**——实测 reactor 仅含聚合器自身 1 模块（恒绿 no-op，不可作 verify）。正确域级构建命令（实测 19 模块、含 md 全部 7 个子模块 + 上游）：`mvn clean install -pl module-master-data/erp-md-api,module-master-data/erp-md-app -am -DskipTests`。
- **verify 闭包构成（实测）**：`-pl module-master-data/erp-md-service -am` = 16 模块，除 md 链 + common-service/common-test 外，**经 common-test 的 optional 依赖拉入未迁移域 fin/prj/notify 的 dao+codegen 模块**——域级 verify 实际承压 M0.1 审计 ③ 的惰性 dao 假设（fin/prj/notify 三域）；任一 dao 存在 md id 类型耦合时 Phase 2/3 当场失败，按路线图规则 6 停止本 plan 回报 M0 裁决（不自行重排）。
- **手写代码冲击面**：md 域手写代码 id 引用 511 处（2026-08-16 路线图快照，执行时以编译器清单为准）。**erp-md-dao 手写 main Java 69 个文件**（非 `_gen`），含真实 Long id 签名：`IErpMdSupplierPriceResolver.java:27`（`Long partnerId`）、`IErpMdCustomerPriceResolver.java:32-33`（`Long partnerId/currencyId`）、4 个 `IErpMd*ReferenceChecker.countReferences(Long xxxId)`、`SubjectMappingResolver.java:35-40`（`Map<Long,Long>`/`List<Long>`）；erp-md-web main 手写 Java 实测 0。md 被其余域引用 ~120 处（126 文件/131 处实体引用实测；下游未迁移域的编译破坏属中间态设计使然，不在本计划修复范围）。
- **测试资产**：module-master-data 下测试 Java 33 个文件 = **25 个含 `@Test` 测试类**（service 24 + web 1（`ErpMdWebPagesTest` page 校验））+ 8 个支撑文件（2 个 codegen 入口类 + 1 个 `ErpMdSupplierApprovalStateMachineDelta` 子类 + **5 个 `TestStub*` SPI 桩——直接 implements dao 层 `IErpMd*ReferenceChecker`/`IErpMdSupplierPriceResolver` 接口，Phase 2 改 dao 签名瞬间其编译即断，属预期移交 Phase 3**）；快照 `_cases` 584 个文件（`module-master-data/erp-md-service/_cases/`），其中含数字实体 id 的输出快照须重录（08-16 全局基线 35/291 含数字 id，md 份额以重录时实测为准）。注意 `mvn test -pl erp-md-service -am` **不运行** erp-md-web 的 `ErpMdWebPagesTest`，须显式并入（见 Phase 3）。
- **平台机制（已由路线图/平台文档确认）**：`stdDataType` 变更后域级构建（上述 19 模块命令）触发增量重生成 `_gen/` 实体、I*Biz、xmeta、view、api 契约（`erp-md-codegen/postcompile/gen-orm.xgen` 等链），不需手改生成件；`tagSet="seq-default"` + BIGINT 列 → `OrmEntityIdGenerator.genSeq` 走序列号引擎，Entity setter 自动 `ConvertHelper.toString` 转 String；`stdSqlType` 保持 BIGINT（DDL/CSV 种子/`NOP_SYS_SEQUENCE` 零影响）。
- **手写 view.xml 预期零改动**（dict int→string 先例 `2026-07-03-2108-1` 已实证：按字段名引用，类型随 xmeta 重生成）。
- **先例**：`docs/plans/2026-07-03-2108-1-dict-int-to-string-refactor.md`（dict int→string）——编译器驱动修复 + 语义陷阱 grep 门控方法论已验证。
- **剩余差距**：源 orm.xml 仍全 `stdDataType="long"`（68 列待改）；md 手写代码/测试/快照全部 Long 形态。

## Goals

- md 域 68 列 PK/FK `stdDataType` long→string 落源（仅此一处源文件变更，`stdSqlType` 零变化）。
- 增量重生成 + 编译器驱动修复 md 全部手写代码（dao/service 层接口与实现、BizModel、Processor、Provider、测试）。
- 快照每域重录（RECORDING→CHECKING，用户裁决——不依赖 `JsonMatchHelper` Number 宽容）。
- 语义陷阱 grep 门控清零（路线图横切 §3 清单，md 范围）。
- 作为**先导试点**验证冻结序判据：「根域迁移后其 `-am` 闭包（含闭包内未迁移惰性 dao 模块 fin/prj/notify）仍全绿」，为后续域顺序提供 Proof 先例。
- 路线图 M1.1 → done + 日志。

## Non-Goals

- 不修复未迁移下游域（purchase/sales/crm 等 ~120 处引用 md 实体的 Long 用法）——各自域 plan 修复（路线图横切 §1：中间态全量构建失败属设计使然）。
- 不改 `delVersion` 等非 PK/FK BIGINT 列（保持 long）。
- 不跑全量构建/全量测试/E2E（归 M4.1）；不跑 compliance checker（归 M4.1 统一复跑）。
- 不手改任何生成件（`_gen/`、`_` 前缀文件）。
- 不做 md owner docs 之外的文档重写（`domain-design-guidelines.md` §16A 偏离表清理归 M4.1）。

## Task Route

- Type: `implementation-only change`（含保护区域 ORM 变更）
- Owner Docs: `docs/backlog/id-string-migration-roadmap.md` M1.1 + 横切 §5 设计证据（`../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md` §主键设计方案 B + `docs/design/domain-design-guidelines.md` §16A.4 + M0.1 审计结论——工件名以 M0.1 Phase 4 实际产物为准，计划引用 `docs/audits/2026-08-21-1045-id-migration-m0-freeze-audit.md` 为预期名）
- Skill Selection Basis: 路线图 §M1-M3「预期技能」指定域迁移 plan 加载 `nop-backend-dev`（BizModel/跨实体约定）+ `nop-testing`（快照重录 RECORDING→CHECKING 流程）；ORM 变更本身由 M0.1 审计与平台文档背书，无需再加载 orm 审计技能。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（无 DB DDL 变更、无端口/密钥/外部服务；DB 列保持 BIGINT，回滚 = revert orm.xml + `mvn clean install -pl module-master-data/erp-md-codegen,module-master-data/erp-md-dao,module-master-data/erp-md-meta,module-master-data/erp-md-service,module-master-data/erp-md-web,module-master-data/erp-md-app,module-master-data/erp-md-api -DskipTests` 重生成回 Long 形态【D3 修订：原 `-pl erp-md-api,erp-md-app -am` 19 模块口径废止】）。

## Execution Plan

### Phase 1 - orm 回写（保护区域，双批准前置）

Status: completed
Targets: `module-master-data/model/app-erp-master-data.orm.xml`
Skill: none

- Item Types: `Fix | Proof`
- Prereqs: M0.1 done + M1.3 done；本计划已通过独立 plan-audit + 第二独立子 agent 复核（保护区域 `auto + dual-agent-approval`，批准记录落盘 Draft Review Record）

- [x] Proof: 双独立子 agent 批准记录落盘（批准人指针 + 结论 + 时间），未获批不得进入后续 Phase。
  - Skill: none
- [x] Fix: 回写 orm——按 M0.1 Phase 1 裁定机制执行，且无论裁定为何，本计划强制自带新鲜度门控：① 回写前重跑 dry-run 得到**时点副本**（当前工具无 per-domain scope，为全量 dry-run 后取 md 副本；若 M0.1 裁定新增 scope 则按裁定执行）；② `diff 实况源 vs 时点副本` 必须仅含 `stdDataType="long"→"string"` 差异行（零非 stdDataType 行——防回滚 08-13 之后落地的 RC 增量，当前 `_tmp/` 旧副本的 3 处漂移即为此类风险实例）；③ 门控通过后才落源。禁止盲 cp 静态副本。
  - Skill: none
- [x] Proof: `git diff module-master-data/model/app-erp-master-data.orm.xml` 逐行核对——仅 PK/FK 列 `stdDataType="long"→"string"`（68 处），`stdSqlType` 零变化、`delVersion` 等非 PK/FK 列零变化、标签结构零变化；`node tools/check-bigint-id-types.mjs`（md 范围）重扫零 `NEEDS FIX` 残留。
  - Skill: none

Exit Criteria:

- [x] 双批准记录在案；新鲜度门控（时点副本 diff 零非 stdDataType 行）+ git diff 与工具重扫双重证明变更面精确 = 68 列 stdDataType

> **执行记录（2026-08-21）**：批准记录在案（Draft Review Record iteration 3 双 passes + 裁定行）。① `node tools/check-bigint-id-types.mjs dry-run` 时点全量刷新（19 文件/1662 列/幂等 yes/XML 19/19）；② `node tools/verify-id-fix-copy-diff.mjs module-master-data` 门控通过——变更行 68、非法差异行 0（时点副本含全部 08-13 后 RC 增量，无回滚风险）；③ 单文件落源（cp 时点 md 副本）。git diff 证明：68 insertions/68 deletions 全部为 `<column>` 行，`stdDataType="long"`→`"string"` 各 68 处，stdSqlType 屏蔽后归一化 diff = 0 行（结构/stdSqlType/delVersion 零变化）；工具 scan md 段重扫 `NEEDS FIX` = 0。

### Phase 2 - 增量重生成 + 主代码编译修复

Status: completed（2026-08-21 M0 裁决落盘后按 Decision D 恢复并完成；此前 blocked = rule-6 停止，见下方 Rule-6 Stop Report）
Targets: `module-master-data/erp-md-dao/src/main/java/**`、`module-master-data/erp-md-service/src/main/java/**`（手写接口/实现/BizModel/Processor/Provider 等；web main 手写 Java 实测 0）
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: Phase 1

- [x] Fix: `mvn clean install -pl module-master-data/erp-md-api,module-master-data/erp-md-app -am -Dmaven.test.skip=true` 触发增量重生成（19 模块 reactor 含 codegen→dao→meta→service→web→app→api 全链；`_gen/` 实体、I*Biz、xmeta、view、api 契约随模型更新；生成件零手改）。**必须用 `-Dmaven.test.skip=true` 而非 `-DskipTests`**：后者仍编译测试源，而 5 个 `TestStub*` SPI 桩实现 Phase 2 正在改签名的 dao 接口，测试编译在本阶段边界**预期断**（`-DskipTests` 会让 19 模块 install 在 md-service test-compile 处中止，web/app/api 三模块当次不重生成）。**【D3 修订，2026-08-21】**M0 裁决后本 plan 的重生成/构建 verify 命令口径 = md 自身模块链显式列表**不带 `-am`**（上游经本地 Maven 仓库解析）：`mvn clean install -pl module-master-data/erp-md-codegen,module-master-data/erp-md-dao,module-master-data/erp-md-meta,module-master-data/erp-md-service,module-master-data/erp-md-web,module-master-data/erp-md-app,module-master-data/erp-md-api -Dmaven.test.skip=true`；原 19 模块 `-am` 命令保留为历史执行记录（重生成已完成，md 链当时全绿后 reactor 在 prj-dao 停止）。
  - Skill: `nop-backend-dev`
- [x] Fix: 编译器驱动修复主代码——逐条修复 md **dao + service** 手写代码类型错误（`Long id` 参数——含 `IErpMdSupplierPriceResolver`/`IErpMdCustomerPriceResolver`/4 个 `IErpMd*ReferenceChecker`/`SubjectMappingResolver` 等 dao 层 Long id 签名、`.getId()` 赋 Long、`setXxxId(Long)`、`Map<Long,...>` 等），直到 19 模块 reactor `-Dmaven.test.skip=true` 构建全绿（main 代码）。修复清单（错误类型 × 处数，分 dao/service 层）+ 测试编译错误清单（预期含 5 个 `TestStub*`）移交 Phase 3。**【D3 修订，2026-08-21】**「19 模块 reactor 全绿」按 M0 裁决修订为「md 自身模块链（7 模块，no-`-am`）构建全绿」。
  - Skill: `nop-backend-dev`
- [x] Fix: 闭包内惰性 dao 破坏处置——19 模块闭包含未迁移域 fin/prj/notify 的 dao+codegen 模块（经 common-test optional 依赖拉入）；若这些模块出现 md id 类型耦合编译错误（M0.1 审计 ③ 假设被证伪），**停止本 plan**，按路线图规则 6 回报 M0 裁决（调整顺序或合并域 plan），不自行修复他域代码、不自行重排。**【D4 carve-out，2026-08-21 M0 裁决已落盘】**rule-6 停止已触发并完成回报：M0 裁决 = `docs/audits/2026-08-21-1045-id-migration-m0-freeze-audit.md` §10 Decision D（选项 (c)：域级 verify 闭包收窄 + 前向耦合登记册 + M4.1 兜底，冻结总序维持）。按 D3/D4（规则 6 修订条款）恢复执行：prj-dao/fin-dao 的 `_gen` 生成件破坏 = **已登记中间态**（successor = M2.7 projects / M2.1 finance 域 plan 愈合，M4.1 兜底），不再触发 rule-6 停止；登记义务 = (i) 破坏模块清单 + successor 指针 + (ii) 逐模块 javac 错误点清单证明登记错误 100% 位于 `_gen` 生成胶水或已登记手写前向边（见 Phase 2 恢复执行记录）。未登记破坏仍触发 rule-6 停止。
  - Skill: `nop-backend-dev`

> **执行记录（2026-08-21）**：
> ① 重生成：19 模块 reactor 命令执行，md 全链（codegen/api/dao/meta 及后续 service/web/app）重生成成功，生成件零手改。
> ② md 主代码修复完成（编译器驱动，3 轮迭代至零错误）——**dao 层 11 文件**：`AcctSchemaResolver`（`resolvePrimarySchemaId` Long→String）、`SubjectMappingResolver`（`Map<Long,Long>`/`List<Long>`→String）、6 个 SPI（`IErpMdSupplierPriceResolver`/`IErpMdCustomerPriceResolver` partnerId/currencyId、4 个 `IErpMd*ReferenceChecker.countReferences` Long→String）、4 个 IBiz（`IErpPartyBiz` partyId×2、`IErpMdMaterialSkuBiz` 7 方法、`IErpMdMaterialBiz` excludeId/countReferences、`IErpMdPartnerBiz` findById/isCodeUnique/countReferences、`IErpMdOrganizationBiz` countReferences、`IErpMdSupplierApprovalBiz` approvalId×7 方法、`IErpMdUoMConversionBiz` convertQty、`IErpMdSubjectBiz` excludeId）、dto `PartyRef.partyId`；**service 层 12 文件**：`ErpPartyBizModel`（getParty/findReferences/loadPartyRef）、`ErpMdMaterialSkuBizModel`（10 处签名 Long→String）、`ErpMdMaterialBizModel`、`ErpMdMaterialCustomsBizModel`（partnerId 局部变量）、`ErpMdReportBizModel`（Set<Long>/Map<Long>→String）、`ErpMdDashboardBizModel`（Set<Long>→Set<String>）、`ErpMdCurrencyRefreshRatesFromApiProcessor`（findExistingRate）、`ErpMdPartnerBizModel`、`ErpMdOrganizationBizModel`、`ErpMdSubjectBizModel`、`ErpMdSupplierApprovalBizModel`、`ErpMdUoMConversionBizModel`、`ErpMdSupplierApprovalSuspendByPartnerProcessor`、`ErpMdOrganizationReferenceChecker`。md 自身 5 模块链验证：`mvn clean install -pl module-master-data/erp-md-{dao,service,web,app,api} -Dmaven.test.skip=true` **BUILD SUCCESS**（main 代码全绿）。`SettlementAllocation.invoiceId` 保持 Long（语义 FK 指向未迁移 fin 域，随 fin 域翻转）。
> ③ **rule-6 停止**：19 模块 reactor 在 `app-erp-projects-dao` FAILURE（28 个编译错误，全部位于**生成件** `_gen/_ErpPrj*.java` 的 to-one 关系胶水 `internalSetRefEntity(..., () -> this.setXxxId(refEntity.getId()))`——refEntity 为 md 实体（String getId），prj FK 列仍 Long）。详见下方 Rule-6 Stop Report。测试编译错误清单移交 Phase 3（未达，Phase 3 阻塞）：预期含 5 个 `TestStub*` 桩 + request.json5/断言 Long 用法（未开始盘点）。

#### Rule-6 Stop Report（2026-08-21，回报 M0 裁决）

- **触发条款**：本计划 Phase 2 第 3 项 + 路线图规则 6——闭包内未迁移域惰性 dao 模块出现 md id 类型耦合编译错误，M0.1 审计 ③「惰性 dao」假设**被证伪**。按条款**停止本 plan**，不自行修复他域代码、不自行重排，回报 M0 裁决。
- **失败现场**：`mvn clean install -pl module-master-data/erp-md-api,module-master-data/erp-md-app -am -Dmaven.test.skip=true` → reactor 在 `app-erp-projects-dao` FAILURE（28 错），md 全链此前 SUCCESS，finance/notify/common-test/common-service/md-service/md-web/md-app 全部 SKIPPED（reactor 顺序：md(codegen/api/dao/meta) → projects → finance → notify → common-test → common-service → md-service → md-web → md-app）。
- **证伪机理（关键——审计盲区在生成件而非手写码）**：M0.1 审计 ③ 逐域枚举的是 dao **手写**跨域 import（仅 crm 3 文件 + pur/sal 4 行），结论「dao 惰性、Long 自洽可编译」。但 dao 模块 `_gen/` 实体的 to-one 关系胶水含**编译级 md 类型耦合**：`_ErpPrjProject.java:1286` = `internalSetRefEntity(PROP_NAME_manager, refEntity, () -> { this.setManagerId(refEntity.getId()); })`，refEntity 类型 `app.erp.md.dao.entity.ErpMdEmployee`。md 迁移后 `getId()` 返回 String、prj `managerId` 列仍 Long → 生成件不可编译。**该胶水由 prj 自身 orm 的 to-one 关系（refEntityName=md 实体）生成，prj orm 未迁移则重生成亦无法修复**（重生成产物相同）。手写码层面审计结论仍成立（prj-dao 手写码零错误，28 错全在 `_gen/`）。
- **影响面实测**：prj orm `refEntityName="app.erp.md.*"` **27 处**（28 错对应）；fin orm **97 处**、fin-dao `_gen` 32 文件引用 md 实体且同样存在 `refEntity.getId());` 胶水（如 `_ErpFinVoucherTemplate.java`）——reactor 未及编译，同机理**预期同样失败**；notify orm **0 处** md 引用（notify-dao 零 md 耦合，不受影响）。git status 证实 fin/prj/notify 本次构建零文件变更（破坏纯由 md String getId 因果触发）。
- **md 域本体状态**：md 5 模块链 main 代码全绿（独立验证）；先导 Proof 判据「根域迁移后其 -am 闭包（含 fin/prj/notify 惰性 dao）仍全绿」**不成立**。
- **待 M0 裁决选项（供参考，不自行裁定）**：(a) 合并域 plan（md+fin+prj 同批迁移，闭包内消除未迁移 md-FK 域）；(b) 调整冻结序（fin/prj 提前，但 fin/prj 自身 -am 闭包反向含 md 及更多下游，需重跑 `tools/freeze-id-migration-order.mjs` 判据迭代并复核环）；(c) 收窄各域 plan 的 verify 闭包口径（如 verify 排除经 common-test optional 拉入的惰性 dao，codegen 重生成仍走全链）+ M4.1 兜底全量恢复——需修订路线图规则 3「域级 -am」定义本身。冻结总序的其余部分（notify 及位次 3+ 域）不受本证伪直接影响。
- **恢复路径**：M0 裁决落盘（roadmap 或 M0 审计工件修订）后，本 plan 从 Phase 2 Exit Criteria 断点恢复（md 主代码修复已完成，无需重做；Phase 2 剩余=按裁定消除闭包破坏 → Phase 3/4 照旧）。
- **恢复执行记录（2026-08-21，M0 裁决 = audit §10 Decision D，iteration 3 双 passes）**：
  - **D5(c) 文本修正落地**：本 plan Phase 2 退出标准/重生成/回滚命令、Phase 3/Closure Gates 命令改 D3 口径（自身模块链显式列表、去 `-am`）+ Phase 2 第 3 项补 D4 carve-out；D5(b) roadmap 七处修正 + M0.2 工作项登记同批完成。
  - **D3 自身链构建取证**：`mvn clean install -pl module-master-data/erp-md-codegen,module-master-data/erp-md-dao,module-master-data/erp-md-meta,module-master-data/erp-md-service,module-master-data/erp-md-web,module-master-data/erp-md-app,module-master-data/erp-md-api -Dmaven.test.skip=true`（no-`-am`，上游经本地 Maven 仓库解析）→ reactor 7 模块（codegen/api/dao/meta/service/web/app）全部 **SUCCESS / BUILD SUCCESS**（15:50:30，总时 7.6s，重生成幂等无 diff）。
  - **D3 登记义务 (i)+(ii)——闭包破坏登记（javac 权威口径，`mvn clean compile -pl <模块>` 复现，错误行去重计数，与 M0 裁决 §10.1 一致）**：
    - `module-projects/erp-prj-dao`：**27 错误点/15 文件，非 `_gen` = 0（100% 生成件 to-one 关系胶水）**，successor = **M2.7 projects**（prj 域 plan 愈合）+ M4.1 兜底。逐文件：_ErpPrjProject×4、_ErpPrjTimesheet×3、_ErpPrjProjectSettlement×3、_ErpPrjBilling×3、_ErpPrjProjectPnl×2、_ErpPrjCostCollection×2、_ErpPrjBudget×2、_ErpPrjTask/_ErpPrjProjectUser/_ErpPrjProjectType/_ErpPrjProjectSettlementLine/_ErpPrjCostCollectionLine/_ErpPrjBudgetLine/_ErpPrjBillingLine/_ErpPrjActivityType 各×1（全路径 `module-projects/erp-prj-dao/src/main/java/app/erp/prj/dao/entity/_gen/`）。
    - `module-finance/erp-fin-dao`：**97 错误点/32 文件，非 `_gen` = 0（100% 生成件）**，successor = **M2.1 finance**（fin 域 plan 愈合）+ M4.1 兜底。逐文件：_ErpFinVoucherLine×9、_ErpFinBudgetLine×9、_ErpFinGlBalance×7、_ErpFinIntercompanyTransferPrice×5、_ErpFinGlMappingRule×5、_ErpFinReconciliation×4、_ErpFinIntercompanyMatch×4、_ErpFinExpenseClaim×4、_ErpFinBadDebt×4、_ErpFinArApItem×4、_ErpFinTrialBalance×3、_ErpFinPostingException×3、_ErpFinNotesReceivable×3、_ErpFinNotesPayable×3、_ErpFinFundAccount×3、_ErpFinEmployeeAdvance×3、_ErpFinConsolidationElimination×3、_ErpFinBudgetScenario×3、_ErpFinBudgetControlLog×3、_ErpFinVoucher×2、_ErpFinExpenseClaimLine×2、×1 各：_ErpFinVoucherTemplate/_ErpFinNotesDiscount/_ErpFinCreditFacility/_ErpFinCashForecast/_ErpFinBudgetRollforwardLog/_ErpFinBudgetCarryForwardLog/_ErpFinBankStatementLine/_ErpFinBankStatement/_ErpFinBankReconciliation/_ErpFinAccountingPeriodStatus/_ErpFinAccountingPeriod（全路径 `module-finance/erp-fin-dao/src/main/java/app/erp/fin/dao/entity/_gen/`）。
    - `module-notify/erp-notify-dao`：**BUILD SUCCESS 零错误**（notify orm 0 处 md 引用，不受影响，无需登记）。
    - 原始 javac 错误行清单副本（file:line 全量）留档 `_tmp/bigint-id-m11-registration/{prj,fin}-dao-errors.txt`（git-ignored 工作区产物，以本节登记为准，`mvn clean compile -pl <模块>` 可随时复现）。
  - **测试编译错误清单移交 Phase 3**（预期：5 个 `TestStub*` SPI 桩 + request.json5/断言 Long 用法），Phase 3 首步实测盘点。

Exit Criteria:

- [x] md 自身模块链（7 模块 codegen/dao/meta/service/web/app/api，显式列表、**不带 `-am`**，`-Dmaven.test.skip=true`）构建全绿（main 代码）；主代码修复清单 + 测试编译错误移交清单（预期含 5 个 `TestStub*`）在案。**【D3 修订说明】**本退出标准原为「19 模块 reactor 全绿」；2026-08-21 M0 裁决（audit §10 Decision D3，路线图规则 3 同批修订）将域级 verify 闭包正式收窄为自身模块链 no-`-am` 口径——经 optional test 边或直接编译依赖进入 `-am` reactor 的未迁移域模块（prj-dao/fin-dao），其编译破坏为已登记中间态（successor 愈合 + M4.1 兜底）。范围变更为 M0 裁决对路线图规则的正式修订，非本 plan 自行缩小（plan 指南规则 10 登记）。恢复执行须附 D3 登记义务证据：破坏模块清单 + successor 指针 + 逐模块 javac 错误点清单（100% `_gen` 证明）。
  - **现状（2026-08-21 恢复执行）**：✅ 达成——7 模块 no-am BUILD SUCCESS；prj-dao 27 错/15 文件 + fin-dao 97 错/32 文件 100% `_gen` 已登记（successor M2.7/M2.1 + M4.1）；notify-dao 零破坏；测试编译错误清单移交 Phase 3

### Phase 3 - 测试修复 + 快照重录 + 域级测试

Status: completed（2026-08-21）
Targets: `module-master-data/**/src/test/**`、`module-master-data/erp-md-service/_cases/**`
Skill: `nop-testing`

- Item Types: `Fix | Proof`
- Prereqs: Phase 2

- [x] Fix: 测试代码修复——request.json5 显式 id 值、断言中 id 类型/比较、测试 helper 的 Long 用法，以及 Phase 2 移交的测试编译错误（含 5 个 `TestStub*` SPI 桩随新 dao 签名改型），逐文件修复至测试编译通过。
  - Skill: `nop-testing`
- [x] Fix: 快照每域重录（用户裁决固定步骤）——`RECORDING` 模式运行 md service 测试 → 逐案审核 `_cases/` 新形态（数字 id → String 形态）→ 切回 `CHECKING` 复跑确认全绿。重录文件数与审核结论记录本计划。
  - Skill: `nop-testing`
- [x] Proof: `mvn test -pl module-master-data/erp-md-service,module-master-data/erp-md-web` 全绿（**D3 修订：不带 `-am`**，上游经本地 Maven 仓库解析；覆盖 25 个测试类全量——service 24 + web 1（`ErpMdWebPagesTest`，单选 erp-md-service 不运行 web 测试，故显式并入），含重录后快照比对）；顺带断言先导 Proof——任一无显式 id 保存路径的测试产物 id 为 String 非空（若 M0.1 Proof 载体为本域则此处为正式落点，双保险来自 M0.1 审计工件）。
  - Skill: `nop-testing`

Exit Criteria:

- [x] md 域级测试全绿（25 测试类，service 24 + web 1）；快照重录完成且 `CHECKING` 复跑通过；重录清单在案【web 1 类偏差登记见执行记录：`ErpMdWebPagesTest` 按已提交治理决策（plan 2026-07-24-0930-1）模块级排除（@Tag("full-app") + surefire excludedGroups），实证依赖全量 classpath（`/erp/xlib/control.xlib` 模块级缺失），页面校验 successor = M4.1 app-erp-all `ErpAllWebPagesTest`，与本计划 Non-Goal「不跑全量构建」一致；service 24 类 155/155 全绿】

> **执行记录（2026-08-21）**：
> ① 测试代码修复完成（编译器驱动）——20 个测试类 Long→String 改型（`TestErpMdDateRangePilots`/`TestErpMdMaterialBiz`/`TestErpMdMaterialCustoms`/`TestErpMdOrganizationBiz`/`TestErpMdPartnerBiz`/`TestErpMdSkuPriceValidation`（含 `PARTNER_1` 9001L→"9001"、`seedCategory`/`seedMaterialAndSku`/`skuIdFor` 签名 String 化）/`TestErpMdSkuReferenceAggregation{Empty,Multi}`/`TestErpMdSkuServices`/`TestErpMdSkuStatusConstraints`/`TestErpMdSubjectBiz`/`TestErpMdSupplierApprovalStateMachine`/dashboard/report/statemachine/party/exchange 各类）+ **5 个 `TestStub*` SPI 桩**（`TestStub{Material,Partner,Sku,SkuSecondary}ReferenceChecker` 的 `Map<Long,...>`/`Set<Long>`/`countReferences(Long)` → String、`TestStubSupplierPriceResolver` partnerId/skuId String）；request.json5 显式 id 值复核零未引用数字 id（grep 门控 `"(id|[a-zA-Z]+Id)":\s*[0-9]+` 于 `_cases/**.json5` = 0 命中）。
> ② 快照重录完成（RECORDING→CHECKING）——重录足迹 = **git status 标记 93 个已跟踪 `_cases` 文件修改（其中 44 个有内容 diff；另 49 个为重录工具 CRLF 行尾差异，git diff 归一化后零内容变化，提交时 autocrlf 吸收）+ 11 个方法新增 input/output 落盘（22 个未跟踪目录**，此前仅 `autotest.yaml`）：`TestErpMdSkuPriceValidation`×3（ConvergedToWarn/Explicit/NonDictFallback）、`TestErpMdSkuReferenceAggregationEmpty`/`Multi` 各 1、statemachine `BaselineIoC`×3 + `DeltaOverride`×3；逐案审核结论：全部 id/FK 值以 String 形态（引号）落盘，`_cases` json5 数字形式实体 id = 0 残留；`CHECKING` 复跑两次全量全绿（155/155）。
> ③ 域级测试命令全绿：`mvn test -pl module-master-data/erp-md-service,module-master-data/erp-md-web` → service **24 测试类 155/155 全绿**（含重录后快照比对），web 模块 BUILD SUCCESS（0 tests，见下方偏差登记）。**web 1 类偏差登记**：`ErpMdWebPagesTest` 带已提交治理标签 `@Tag("full-app")` 且 erp-md-web pom surefire `<excludedGroups>full-app</excludedGroups>`（plan 2026-07-24-0930-1 committed decision，早于本 mission；pom 显式配置 CLI 属性不可覆盖）；模块级独立执行实证其确需全量 classpath——JUnit Platform Launcher 直跑失败于 `/erp/xlib/control.xlib` 资源缺失（md `main.page.yaml` 引用 app 级 xlib），印证「仅全量 classpath 可运行」语义。页面校验 successor = **M4.1**（app-erp-all `ErpAllWebPagesTest`，CI 既有 web-pages-validation job 口径），与本计划 Non-Goal「不跑全量构建/全量测试/E2E（归 M4.1）」一致。本计划不改该治理排除（超范围）。
> ④ 先导 Proof 落点：`TestErpMdPartnerCrudSmoke.testPartnerAddressRelation`（关系导航无显式 id 保存路径）重录产物 `output/2_saveAddress_response.json5` = `"id": "2"`、`"partnerId": "1"`——**String 非空** ✅（M0.1 `TestSeqStringIdProof` 4/4 为第一落点，此处为域级双保险）。
> ⑤ 执行期发现修复：平台 IoC 回归（nop-entropy 08-21 09:33 重装 jar 后 `nopSequenceGenerator` bean-init-self-wait，bisect 证实与本迁移无关）——test-scope VFS delta `erp-md-service/src/test/resources/_vfs/_delta/default/nop/sys/beans/app-dao.beans.xml`（`ioc:lazy-property`，镜像平台先例）修复，bug 登记 `docs/bugs/2026-08-21-nop-sequence-generator-ioc-self-wait-after-platform-reinstall.md`（successor：平台修复后移除 delta）。

### Phase 4 - 语义陷阱 grep 门控 + 收尾

Status: completed（2026-08-21）
Targets: `module-master-data/**`（手写代码）、`docs/backlog/id-string-migration-roadmap.md`、`docs/logs/2026/08-21.md`
Skill: none

- Item Types: `Proof | Add`
- Prereqs: Phase 3

- [x] Proof: 语义陷阱 grep 门控（路线图横切 §3，md 手写代码范围）清零——`\.longValue\(\)`、`Long\.parseLong\(`、`Map<Long`、`String\.format\("%d`（含 `%d` 变体）零命中；`Long` 装箱 `==`/`!=` 比较（id 上下文）逐条核清；sql-lib.xml `:id` 参数条目仓内零存在（执行时注明即可）。结果逐项记录本计划。
  - Skill: none
- [x] Proof: 手写 view.xml 零改动验证——`git status module-master-data/erp-md-web` 确认无手写 view 文件被动变更（生成 view 随 codegen 更新不在此列）。
  - Skill: none
- [x] Add: owner doc 注记——grep `docs/design/master-data/` 与 `docs/design/domain-design-guidelines.md` 中关于 md id 为 Long/数字的陈述；存在则就地注记 Java 层已 String 化（引用本计划），不存在则记录「无 Long id 陈述，零文档变更」结论。
  - Skill: none
- [x] Add: 路线图 M1.1 → `done`（含先导试点结论：根域迁移后 19 模块闭包（含 fin/prj/notify 惰性 dao）全绿成立与否——不成立时按路线图规则 6 停止并回报 M0 裁决）；日志条目（含验证状态）。
  - Skill: none

Exit Criteria:

- [x] grep 门控零残留（例外为零或逐条核清记录）；view 零手改动在案
- [x] 路线图状态、试点结论、日志三者一致

> **执行记录（2026-08-21）**：
> ① grep 门控（md 手写 main + test，排除 `_gen/`）：`\.longValue\(\)`=0、`Long\.parseLong\(`=0、`Map<Long`=0、`String\.format\("%d` 及 `%d` 变体=0；Long 装箱 id `==`/`!=` 与 `xxxId.equals(` 模式=0；sql-lib.xml 仓内**零存在**（find 全仓 0 文件，`:id` 参数条目随之零存在——注明）。残留 `Long` 逐条核清（全部合法非 id 或已登记）：`Map<String, Long>`（countReferences 返回值，Long=引用计数非 id，签名保持）、`setDelVersion(Long)`（delVersion 非 PK/FK 列，Non-Goal 保持 long）、`SettlementAllocation.invoiceId`（语义 FK 指向未迁移 fin 域，Phase 2 已登记随 M2.1 翻转）、`ErpDateRangeOverlapValidator` Javadoc 过期陈述已就地修正（getId 返 Long→String）。
> ② view 零手改动：`git status module-master-data/erp-md-web` 全部 25 处 view.xml 变更均在 `_gen/_*.view.xml`（生成件，随 codegen 更新），非 gen 变更 = 0；erp-md-web pom.xml 零改动（治理排除配置未触碰）。
> ③ owner doc 注记 3 处：`docs/design/master-data/ui-patterns.md`（§跨域范式参考块下加 id 类型注记——`isCodeUnique(..., Long excludeId)`/`countReferences(Long id)` Java 层已 String 化，表内保留历史签名作语义参考，`Map<String,Long>` Long 为计数非 id）；`docs/design/master-data/unified-party-identity.md` §2.1 对齐表 `partyId` 备注改 String + §4 SPI 端口签名改 `countReferences(String partyId)`。`docs/design/domain-design-guidelines.md` §16A 已知偏离表实测**无 md 实体行**（仅 purchase/sales/mfg ~13 实体），md 专属 Long id 陈述零存在；§16A 清理按 Non-Goal 归 M4.1。
> ④ roadmap M1.1 → `done`（Work Item Status 表 + 依赖图 + 头部「最后更新」），试点结论 = **D6 修订判据成立**（自身模块链全绿 + 闭包破坏仅限已登记 `_gen` 耦合点 prj-dao 27/fin-dao 97，successor M2.7/M2.1 + M4.1 兜底；原 19 模块闭包全绿判据经 M0 裁决 D6 修订，无需再触发规则 6）；日志条目落 `docs/logs/2026/08-21.md`（含验证状态全绿）。
> ⑤ Closure 验证三命令（域级口径）：`mvn clean install -pl <md 7 模块显式列表> -DskipTests` = 7 模块 BUILD SUCCESS；`mvn test -pl module-master-data/erp-md-service,module-master-data/erp-md-web` = service 24 类 **155/155** + web BUILD SUCCESS；`node tools/check-bigint-id-types.mjs` md 段 `NEEDS FIX` = **0**（全仓其余 1594 命中全部位于 18 个未迁移域 orm，属各 successor plan 范围）。

## Draft Review Record

- Independent draft review iteration 1 (plan-audit 视角, ses_fddbbb187ffehhAL61gdxnlB69): `needs revision` — 1 BLOCKER（`-pl module-master-data -am` 不展开聚合器子模块，实测 reactor 仅 1 模块恒绿 no-op——主构建/验证命令失效，codegen 不触发、false-green gate）+ 2 MAJOR（① md `-am` 闭包实测含未迁移 fin/prj/notify dao+codegen 模块（16 模块，经 common-test optional 依赖），基线「仅 nop-entropy + common」失实；② Phase 2 Targets 漏 erp-md-dao 手写 69 文件的真实 Long id 签名）+ 5 MINOR（副本漂移实为 3 处/约 6 行且两处早于 08-18、工具无 per-domain scope、33=30 测试类+3 辅助类且 web 测试不在 service -pl 闭包、erp-md-api 不在任何验证闭包、前向引用/错字）。
- Independent draft review iteration 1 (独立复核视角, ses_fddbb8219ffeLHcQ6CBAjL0jNk): `passes draft review` — 0 BLOCKER / 0 MAJOR / 3 MINOR（副本漂移低报、33 测试类口径、后续域 plan 应沿用 own-file 口径）；设计证据链（orm-model-design.md §方案 B 机制逐条 verbatim 核实）、双批准记录机制、drp notGenCode 延后正确性、回滚可行性、模板合规均验证通过。
- 修订（iteration 1 → 2）：BLOCKER 修复——全部构建/验证命令改为实测 19 模块 reactor `-pl module-master-data/erp-md-api,module-master-data/erp-md-app -am`（聚合器 no-op 命令全数清除，回滚命令同步）；MAJOR① ——基线新增「verify 闭包构成」条目 + Phase 2 新增惰性 dao 破坏处置项（规则 6 停止条款）+ Goals/Phase 4 试点口径含惰性 dao；MAJOR②——Phase 2 Targets 扩为 dao+service 手写代码并列举 dao 层 Long 签名实例；MINOR 全处理——副本漂移口径改 3 处/约 6 行（含 08-15 两处）、dry-run 措辞改「全量 dry-run 后取 md 副本」、测试口径改 30 测试类 + web 测试显式并入 test 命令（`-pl erp-md-service,erp-md-web -am`）、erp-md-api 并入 19 模块构建闭包、错字修正。
- **激活协议（保护区域）**：本计划双独立子 agent 批准（两条针对最终修订文本的 `passes draft review`/accept 记录）落盘本节后 `Plan Status` → `active`；orm 回写（Phase 1）前批准记录必须已在案。**针对被超越草案的 passes 记录不计入双批准**（iteration 1 复核视角的 pass 基于修订前文本，其批准效力由 iteration 2 复核确认承载）。
- Independent draft review iteration 2 (plan-audit 视角, ses_fddaf317affebZ4vrWan6TYIsj): `needs revision` — 0 BLOCKER / 1 MAJOR / 1 MINOR。B1/M1/M2 与 5 MINOR 全部核实已解决（19 模块 reactor 实测吻合、闭包构成逐模块吻合、dao Long 签名全部实测命中、副本漂移 3 处/6 行逐字吻合）；MAJOR：Phase 2 首跑命令用 `-DskipTests` 仍编译测试源——5 个 `TestStub*` 桩实现 Phase 2 正在改签名的 dao SPI，19 模块 install 将在 md-service test-compile 处中止（web/app/api 当次不重生成），Phase 2 退出标准不可达成（false-red gate）；MINOR：测试类计数仍不实（实况 = 25 个 `@Test` 测试类（service 24 + web 1）+ 8 个支撑文件）。
- Independent draft review iteration 2 (独立复核视角, ses_fddaf04daffeZ6eGK45P0SrkxW): `passes draft review`（基于修订文本）— 0 BLOCKER / 0 MAJOR / 3 MINOR（① 测试类 split 计数误（实为 service 24/29 + web 1，total 与门控不受影响）；② Phase 2 退出标准 `-DskipTests` 措辞歧义（同 plan-audit 侧 MAJOR 的轻量表述）；③ 后续域 plan 须自派生计数口径仅隐式）。B1/M1/M2 修复、19 模块/17 模块闭包、惰性 dao 规则 6、设计证据链、新鲜度门控、反松弛全部独立复核通过。
- 修订（iteration 2 → 3）：MAJOR 修复——Phase 2 重生成与退出标准全部改用 `-Dmaven.test.skip=true`（明示 `-DskipTests` 不可用的原因与 TestStub* 断编译预期，测试编译错误清单移交 Phase 3；Closure Gates 保留 `-DskipTests` 并注明彼时测试已修复语义正确）；MINOR 修复——测试资产口径改「25 个 `@Test` 测试类（service 24 + web 1）+ 8 个支撑文件（2 codegen 入口 + 1 Delta 子类 + 5 TestStub* 桩）」（executing agent 逐文件实测复核），Phase 3/Closure Gates 计数同步 25。
- Independent draft review iteration 3 (plan-audit 视角最终文本确认, ses_fdda6ff38ffekcFEMFryTZ094y): `passes draft review` — 0 BLOCKER / 0 MAJOR / 0 MINOR。六项验证全部通过（Phase 2 flag 修复 + 理由完整、计数 25/8 全文一致且逐文件实测复核、TestStub* implements dao SPI 事实独立确认、审查记录诚实完整、窄修订零新问题、命令形态与 iteration-2 批准版一致）；residual note：双批准须补 dual 侧对最终文本的确认（已由下条完成）。
- Independent draft review iteration 3 (独立复核视角最终文本确认, ses_fdda47eedffebc1LoLJrQA64uQ): `passes draft review` — 处方修订逐字落地、设计证据链/保护区域协议/新鲜度门控/结束门控真实性零改动，确认 attached to final text。
- **裁定：双独立子 agent 批准达成（iteration 3 两条 passes 均针对最终文本），Plan Status → active。**orm 回写（Phase 1）执行前提：M0.1 done + M1.3 done（roadmap 依赖），且 Phase 1 批准记录项在案。roadmap M1.1 工作项保持 `todo` 至其依赖前置项 done 后转 `ready`（roadmap 规则 1）。

## Closure Gates

> 完整仓库验证定制为域级口径（路线图规则 3：禁止以全量构建为中间 gate；全量构建仅存在于 M4.1。**D3 修订，2026-08-21**：域级 verify = 自身模块链显式列表、不带 `-am`，上游经本地 Maven 仓库解析）。

- [x] 范围内行为完成（68 列落源 + 重生成 + 手写代码/测试修复 + 快照重录 + grep 门控清零）
- [x] 相关文档对齐（owner doc 注记或零变更结论、路线图 M1.1 状态、日志）
- [x] 已运行验证：`mvn clean install -pl module-master-data/erp-md-codegen,module-master-data/erp-md-dao,module-master-data/erp-md-meta,module-master-data/erp-md-service,module-master-data/erp-md-web,module-master-data/erp-md-app,module-master-data/erp-md-api -DskipTests` 全绿（7 模块自身链，**D3 修订：不带 `-am`**；结束阶段测试已修复，`-DskipTests` 语义正确）+ `mvn test -pl module-master-data/erp-md-service,module-master-data/erp-md-web` 全绿（service 24 类 155/155；web 1 类按已提交治理排除——偏差登记见 Phase 3 执行记录，页面校验 successor M4.1）+ 工具重扫零残留（md 段 `NEEDS FIX` = 0）
- [x] 无范围内项目降级为 deferred/follow-up（`ErpMdWebPagesTest` 模块级排除为**先于本计划的已提交治理决策**（2026-07-24-0930-1）且与本计划 Non-Goal「不跑全量构建」一致，属偏差登记而非范围降级；其余中间态 prj-dao/fin-dao/`invoiceId` 均为 M0 裁决/Phase 2 已登记，successor 不变）
- [x] 保护区域双独立子 agent 批准记录落盘（Phase 1 前置）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（审计 = ses_fdc859128ffeS7wxCsU1qyLwv0，`passes closure audit`，2026-08-21，见 Closure 节证据）
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 下游未迁移域对 md 实体的 Long 引用（~120 处）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 路线图横切 §1 设计使然——中间态全量构建失败预期存在，下游域各自 plan 修复；md 的 verify 闭包（**D3 修订后 = md 自身 7 模块链，no-`-am`**；原 19 模块 `-am` 口径下仅含 fin/prj/notify 的惰性 dao 模块，其 `_gen` 破坏已按 M0 裁决登记为中间态：prj-dao/fin-dao successor = M2.7/M2.1 + M4.1 兜底），不含任何下游 **service** 模块。
- Successor Required: `yes`（M2/M3 各域 plan，按 M0.1 冻结序）

## Closure

Status Note: closed（2026-08-21，四 Phase 完成 + 独立结束审计 passes）

Closure Audit Evidence:

- Auditor / Agent: ses_fdc859128ffeS7wxCsU1qyLwv0（独立子代理，新会话，2026-08-21）
- Evidence: **`passes closure audit`（0 BLOCKER / 0 MAJOR / 3 MINOR）**。审计第一手复核（节选）：① ORM diff = 68/68 全 `stdDataType` 行、0 非 stdDataType；② 7 模块 no-am `clean install -DskipTests` BUILD SUCCESS + `mvn test` 155/155 绿；③ 语义 grep = 0、无 RECORDING/forceSaveOutput 残留；④ 已登记中间态未被静默修复——`mvn compile -pl erp-prj-dao` 27 错/15 文件、`erp-fin-dao` 97/32，100% `_gen` 胶水（`_ErpPrjProject.java:1286` 复核命中），`erp-notify-dao` SUCCESS，与 D3/D4 登记精确一致；⑤ 生成件为 codegen 产物（`_ErpMdPartner.java` String id）；⑥ 快照 String 形态 + untracked = 恰 11 方法×input/output；⑦ roadmap/日志/owner doc 注记/bug note↔delta 一致。3 MINOR 均为记录准确性/收尾形式项，已按审计意见修正：重录足迹口径 44→「93 标记 = 44 内容 diff + 49 CRLF 行尾」、view 变更 27→25、Closure Gates/Status Note 本条落盘。

Follow-up:

- 无（已确认缺陷零；下游引用已有 successor 登记——prj-dao/fin-dao `_gen` = M2.7/M2.1 + M4.1、`ErpMdWebPagesTest` 页面校验 = M4.1、`SettlementAllocation.invoiceId` = M2.1）
