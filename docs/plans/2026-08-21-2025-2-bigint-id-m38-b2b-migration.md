# 2026-08-21-2025-2-bigint-id-m38-b2b-migration 主键/外键 string 化 M3.8：b2b 域迁移（冻结序位次 4）

> Plan Status: active（2026-08-21：iteration 1 治理审查 pass + 技术审查 needs revision → 修订后 iteration 2 技术复审 `passes draft review`；保护区域双独立子 agent 批准，见 Draft Review Record）
> Mission: id-string-migration
> Work Item: M3.8（b2b，冻结序位次 4）
> Last Reviewed: 2026-08-21
> Source: `docs/backlog/id-string-migration-roadmap.md` M2/M3 冻结总序表位次 4（M3.8）
> Related: `docs/plans/2026-08-21-2025-1-bigint-id-m39-aps-migration.md`（冻结序前一位次，批内序 1）、`docs/plans/2026-08-21-1045-3-bigint-id-m11-master-data-migration.md`（M1.1 先导试点与 D3/D4 口径来源）、`docs/plans/2026-08-21-1657-2-bigint-id-m12-notify-migration.md`（M1.2）、`docs/plans/2026-08-21-1657-1-bigint-id-m02-forward-coupling-registry.md`（M0.2 登记册，消费来源）
> Audit: required（保护区域 `model/*.orm.xml`：独立 plan-audit + 双独立子 agent 批准，批准记录落盘本文件）

## Current Baseline

- **b2b 域规模（2026-08-21 实况 scan）**：`module-b2b/model/app-erp-b2b.orm.xml` 需改列 **40 = 自有 37（PK 13 + BIGINT FK 24，13 实体）+ notGenCode 外部实体 stub 3**。自有 PK：`ErpB2bEdiFormat/EdiDoc/Asn/AsnLine/CodeMapping/EdiLog/PartnerProfile/PartnerCredential/TestExchange/CertificationChecklist/MftConfig/MftCertificate/MftLog` 各 `.id`；自有 FK 含 `orgId` ×9（M1.3 common-service 已 String 语义）+ `partnerId` ×5 + `partnerProfileId` ×3 + `materialId`/`asnId`/`formatId`/`ediDocId`/`certId`/`configId`/`sourceEdiDocId` 等本域 FK。**不改列**：VARCHAR FK `attachmentFileId`（file，显式 string）+ `delVersion` ×13 与 `fileSize`/`durationMs` 等（非 PK/FK 保持 long，路线图规则 4）。
- **notGenCode stub（本域特有，需显式登记）**：b2b orm 末尾嵌入 md 外部实体 stub 3 个（约 :663-683）：`ErpMdPartner`/`ErpMdOrganization`/`ErpMdMaterial`——md 权威源自 M1.1 已 String，stub 翻转为**与权威源对齐**（非新增决策）；dry-run 副本已核证 3 stub id 一并翻转。orm 内 md refEntityName 15 处（org/partner/material to-one）。
- **模块链与编译依赖（pom 实测）**：7 模块 = `module-b2b/erp-b2b-{codegen,dao,meta,service,web,app,api}`。b2b-dao main compile 依赖 **md-dao**（已 String）——b2b-dao `_gen` 关系胶水自 M1.1 起处于已登记中间态（D3 对称耦合），**本域迁移即自愈**。b2b-service main compile 依赖 **pur-dao（未迁移 Long）+ notify-dao（已 String）**；test 依赖 notify-service（已 String）+ app-erp-common-test（Proxy 桩编译安全）。b2b-service **无 sal-dao compile 依赖**——对 `ErpSalInvoice` 的唯一引用经 `IDaoProvider.dao("app.erp.sal.dao.entity.ErpSalInvoice")` 字符串反射（`UblInvoiceEdiProvider:53`），**无编译面，属语义陷阱类别**。
- **M0.2 登记册 b2b 视角（§6.4，起草消费已核 + 本计划补登 1 条）**：A1 orm 延后 = 0；**A2 main 桥接 7 条**（pur 6 条编译级：`ErpB2bAsnCreateReceiveFromAsnProcessor:10-13`（ErpPurOrder/OrderLine/Receive/ReceiveLine）+ `ErpB2bAsnMatchPurchaseOrderProcessor:9/10`（ErpPurOrder/OrderLine），退役 owner M2.5；sal 1 条语义级：`UblInvoiceEdiProvider:53` IDaoProvider 字符串引用，退役 owner M2.6）；**A3 test 桥接 = 0（登记册 import 扫描口径）——但实测存在 1 个 FQN 级 test 前向耦合（登记册扫描器盲区，本计划补登）**：`TestErpB2bAsnInventoryIntegration.java` 以全限定名（无 import 语句）引用 pur 实体 20 处（`app.erp.pur.dao.entity.ErpPurReceive/ReceiveLine/PurOrder/PurOrderLine`，:111-:434，含 `findReceivesByOrderId(Long)`/`receive.getId()` Long 流），编译级耦合（pur-dao 为 b2b-service main compile 依赖，实体类在测试 classpath）——b2b 迁移后该测试在本域 String id 与 pur Long 实体边界混用，需局部 String↔Long 桥接（A3' 补登条目，退役 owner M2.5）；Phase 4 在登记册补登条目并追注扫描器 FQN 盲区；C1 后向 main 2 条（backward-137 → md：`ErpB2bAsnCreateReceiveFromAsnProcessor`（md import :9）；backward-138 → notify：`ErpB2bOnboardingMonitorJob`）；C2 后向 test 2 条（backward-197 → md + backward-198 → notify，同为 `TestErpB2bPartnerOnboarding` 1 文件）；B 退役义务 = 0。
- **手写代码冲击面（实测）**：dao 手写 Long 相关 3 文件（`IErpB2bPartnerProfileBiz`/`IErpB2bEdiDocBiz`/`IErpB2bAsnBiz`）；service 手写含 in-code `Long` 12 文件（BizModel ×3、Processor ×6、Job ×1、TransportManager/CodeMappingResolver；`UblInvoiceEdiProvider` 零 Long 用法——其 sal 耦合为纯语义级，单列于 A2 bridge-main-032）；api beans 26 文件为 **codegen 生成件**（零手写，orm 翻转后随动，零手改——先例「api beans 随动」）。`.getId()` main 手写 26 处。执行时以编译器清单为准。
- **测试资产（实测）**：service 12 个 `@Test` 类（AsnCrudSmoke/AsnInbound/AsnInventoryIntegration/AsnStateMachineMatrix/EdiDocStateMachine ×3/EdiEnvelope/EdiPosting/MftTransport/PartnerOnboarding/PartnerProfileStateMachineMatrix）+ helpers（`B2bFrozenClockExtension`/`ErpB2bEdiDocStateMachineDelta`）；`_cases` 快照 324 文件（261 csv + 53 yaml + 10 json5 CrudSmoke 输出快照）+ codegen 入口类；`ErpB2bWebPagesTest` `@Tag("full-app")` + surefire `<excludedGroups>` 模块级排除（已提交治理决策，successor = M4.1）。
- **已知风险（md/notify 先例登记）**：① 平台 IoC 回归 `nopSequenceGenerator` self-wait——若复现按先例修复（test-scope VFS delta；b2b 现有 `test-b2b-delta` 仅覆盖 erp/b2b 域 beans，需新增 `nop/sys/beans/app-dao.beans.xml` delta）；② no-am 测试 classpath VFS 模块集变化（已登记中间态，回退方案 = seq-proof-yaml 模块禁用模式）；③ 陈旧 jar 二进制不兼容（本地仓未迁移 pur-dao jar——b2b 域级测试按设计不跨这些边界）。
- **回写机制（M0.1 裁定 Decision A，三步）**：① dry-run 时点刷新；② `node tools/verify-id-fix-copy-diff.mjs module-b2b` 新鲜度门控；③ 单文件落源 + `git diff` 逐行审核。禁止盲 cp、禁止 apply 模式。
- **剩余差距**：b2b orm 40 列全 `stdDataType="long"` 待改；b2b 手写代码/测试/快照全部 Long 形态；冻结序位次 4（之后位次 5 contract 待迁移）。

## Goals

- b2b 域 40 列（自有 37 + md stub 3）`stdDataType` long→string 落源（唯一源文件变更，`stdSqlType` 保持 BIGINT，DDL 零变化）。
- 增量重生成（no-am 7 模块链）+ 编译器驱动修复 b2b 全部手写代码（dao 3 文件 + service 12 文件 Long 语义）+ A2 前向桥接落桥（pur 6 处编译级 + sal 1 处语义级核验）+ A3' FQN 级 test 桥接适配（1 文件，登记册补登）。
- 快照每域重录（RECORDING→CHECKING，用户裁决——不依赖 Number 宽容）。
- 语义陷阱 grep 门控清零（路线图横切 §3 清单，b2b 范围，含 `UblInvoiceEdiProvider` 反射路径 id 语义核验）。
- 消费 M0.2 登记册：A2 桥接 disposition 落盘本计划，C1/C2 修复定位面消费，heal M1.1 登记的 b2b-dao `_gen` 对称胶水中间态；兑付 M1.2 登记的 notify test 后向 successor 义务（1 文件）。
- 路线图 M3.8 → `done` + 日志；冻结序位次 5（contract）解锁。

## Non-Goals

- 不修复外域代码对 b2b 的引用（b2b 被引用 main/test = 0/0，登记册 §2 矩阵——叶子域）。
- 不迁移 purchase/sales 域（A2 桥接目标域，归 M2.5/M2.6）。
- 不改 `delVersion` 等非 PK/FK BIGINT 列（保持 long）；不改 VARCHAR FK 列（本就 String）。
- 不跑全量构建/全量测试/E2E（归 M4.1）；不跑 compliance checker（归 M4.1）。
- 不手改任何生成件（`_gen/`、`_` 前缀文件、api beans）；手写 view.xml 预期零改动（先例已实证，Phase 4 验证）。
- 不修 `ErpB2bWebPagesTest` 的治理排除（已提交决策，successor = M4.1）。
- 不做 b2b owner docs 之外的文档重写（`domain-design-guidelines.md` §16A 清理归 M4.1）。

## Task Route

- Type: `implementation-only change`（含保护区域 ORM 变更）
- Owner Docs: `docs/backlog/id-string-migration-roadmap.md` M2/M3 表位次 4 + 横切 §5 设计证据（`../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md` §主键设计方案 B + `docs/design/domain-design-guidelines.md` §16A.4 + M0.1 审计结论 + M0 裁决 §10）；b2b 业务语义 owner doc = `docs/design/b2b/`（Phase 4 注记对象）
- Skill Selection Basis: 路线图 §M1-M3「预期技能」指定域迁移 plan 加载 `nop-backend-dev` + `nop-testing`；ORM 变更机制由 M0.1 审计与平台文档背书。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（无 DB DDL 变更；DB 列保持 BIGINT）。no-am 构建硬前置 = 最后全绿基线 commit 全量 install + md/notify/common 链 install（已就位）+ pur-dao 基线 jar（未迁移 Long，正是桥接对象）+ **批内前置：位次 3 aps 链 install**（冻结序执行约束——b2b 精确前置仅为 M1.1+M1.2，但本批按冻结总序在 aps 之后执行）。回滚策略：revert orm.xml + `mvn clean install -pl module-b2b/erp-b2b-codegen,module-b2b/erp-b2b-dao,module-b2b/erp-b2b-meta,module-b2b/erp-b2b-service,module-b2b/erp-b2b-web,module-b2b/erp-b2b-app,module-b2b/erp-b2b-api -DskipTests` 重生成回 Long 形态。

## Execution Plan

### Phase 1 - 消费登记册 + orm 回写（保护区域，双批准前置）

Status: planned
Targets: `module-b2b/model/app-erp-b2b.orm.xml`
Skill: none

- Item Types: `Proof | Fix`
- Prereqs: M1.1 ✅ + M1.2 ✅ + M0.2 ✅（精确前置已满足）；冻结序位次 3（aps）done（批内执行顺序）；本计划已通过独立 plan-audit + 第二独立子 agent 复核（保护区域 `auto + dual-agent-approval`，批准记录落盘 Draft Review Record）

- [ ] Proof: 消费 M0.2 登记册——读取 `tools/id-migration-registry.json5` + 登记册文档 §6.4 b2b 节，逐条核对：(i) A1 orm 延后 = 0（40 列全翻转）；(ii) A2 main 桥接 7 条与本地实测对账（pur 6 编译级 + sal 1 语义级，本计划 Current Baseline 已预对账一致，执行时双源复核）；(iii) A3 登记册口径 = 0，**另以 FQN 口径复扫本域测试**（`rg 'app\.erp\.(pur|inv|sal|mfg)\.' module-b2b/erp-b2b-service/src/test`，排除 import 行；**按行计数**——该文件 20 行命中 / 22 token，:330/:346 各含双 token，勿以 token 计数误触 rule-6）核证唯一 FQN 耦合文件 = `TestErpB2bAsnInventoryIntegration`（20 处，A3' 补登条目，Phase 3 适配）——若复扫发现**新增**未登记 FQN 耦合，按路线图规则 6 停止回报（登记册与实况冲突）；(iv) C1/C2 后向指针（md 1 main + notify 1 main；test 1 文件双指向）作为 Phase 2/3 定位面。矛盾则按路线图规则 6 停止回报。
  - Skill: none
- [ ] Proof: 双独立子 agent 批准记录落盘（批准人指针 + 结论 + 时间），未获批不得进入回写。
  - Skill: none
- [ ] Fix: 回写 orm（M0.1 裁定三步机制）——① `node tools/check-bigint-id-types.mjs dry-run` 时点刷新；② `node tools/verify-id-fix-copy-diff.mjs module-b2b` 新鲜度门控（零非 stdDataType 行）；③ 门控通过后单文件落源。禁止盲 cp 静态副本、禁止 apply 模式。
  - Skill: none
- [ ] Proof: `git diff module-b2b/model/app-erp-b2b.orm.xml` 逐行核对——仅 40 列 `stdDataType="long"→"string"`（自有 37 = PK 13 + FK 24 + notGenCode stub 3），`stdSqlType` 零变化、`attachmentFileId`/`delVersion`/标签结构零变化；scan b2b 段重扫零 `NEEDS FIX`/零 `DEFERRED` 残留。
  - Skill: none

Exit Criteria:

- [ ] 登记册消费核对在案；双批准记录在案；新鲜度门控 + git diff + 工具重扫三重证明变更面精确 = 40 列 stdDataType

### Phase 2 - 增量重生成 + 主代码编译修复 + A2 桥接落桥

Status: planned
Targets: `module-b2b/erp-b2b-dao/src/main/java/**`、`module-b2b/erp-b2b-service/src/main/java/**`（手写 IBiz/BizModel/Processor/Job/SPI；web main 手写实测 0；api beans 生成件随动）
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: Phase 1

- [ ] Fix: `mvn clean install -pl module-b2b/erp-b2b-codegen,module-b2b/erp-b2b-dao,module-b2b/erp-b2b-meta,module-b2b/erp-b2b-service,module-b2b/erp-b2b-web,module-b2b/erp-b2b-app,module-b2b/erp-b2b-api -Dmaven.test.skip=true`（D3 口径：7 模块显式列表、不带 `-am`、`-Dmaven.test.skip=true`）触发增量重生成。预期：b2b-dao `_gen` md 关系胶水（15 处 md refEntityName 对应）自 M1.1 的登记中间态自愈。
  - Skill: `nop-backend-dev`
- [ ] Fix: 编译器驱动修复主代码——逐条修复 b2b dao + service 手写代码类型错误（基线预判：dao 3 文件 IBiz 签名、service 12 文件 Long 语义 + 26 处 `.getId()` 下游；以编译器实际清单为准），直到 7 模块链 `-Dmaven.test.skip=true` 构建全绿。修复清单落盘本计划；测试编译错误由 Phase 3 首轮 `test-compile` 产生后修复（`-Dmaven.test.skip=true` 阶段不编译测试）。
  - Skill: `nop-backend-dev`
- [ ] Fix: A2 前向桥接落桥（D4 消费协议）——pur 6 处编译级调用点加 String↔Long 转换桥（`ErpB2bAsnCreateReceiveFromAsnProcessor`/`ErpB2bAsnMatchPurchaseOrderProcessor`），每处登记 grep 例外清单（条目 id + file:line + 转换方向），退役 owner M2.5。
  - Skill: `nop-backend-dev`
- [ ] Fix: A2 语义级桥接核验（bridge-main-032）——`UblInvoiceEdiProvider` 经 IDaoProvider 字符串引用 `ErpSalInvoice`（无编译面）：核验其对 invoice 属性读取（code/invoiceDate/关联 id 属性）在 String id 语义下的行为正确性（b2b String id ↔ sal Long 字段的反射读写路径），若存在 id 传递则加归一转换 + 登记；结论落盘（退役 owner M2.6）。
  - Skill: `nop-backend-dev`
- [ ] Fix: C1 后向修复——md/notify 引用点（`ErpB2bAsnCreateReceiveFromAsnProcessor` md 侧；`ErpB2bOnboardingMonitorJob` notify 侧）对已 String 化 API 的编译适配（编译器驱动，定位面 = backward-137/138；notify 侧调用面预期 `notify(String,Map,ctx)` 签名不变零破坏——M1.2 已证，编译核验即可）。
  - Skill: `nop-backend-dev`
- [ ] Fix: 自身链破坏处置（D4 carve-out）——no-am 口径下预期零外域破坏；未登记破坏按路线图规则 6 停止回报；已登记破坏按中间态继续并履行登记义务。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] b2b 7 模块链（显式列表、no-am、`-Dmaven.test.skip=true`）构建全绿（main 代码）；主代码修复清单 + 桥接例外清单（含语义级核验结论）在案

### Phase 3 - 测试修复 + 快照重录 + 域级测试

Status: planned
Targets: `module-b2b/**/src/test/**`、`module-b2b/erp-b2b-service/_cases/**`
Skill: `nop-testing`

- Item Types: `Fix | Proof`
- Prereqs: Phase 2

- [ ] Fix: 测试代码修复——12 个 service 测试类的 Long 用法（字面量断言、helper 签名、seed `orm_propValueByName("id", id)` 形态——md/notify 先例），逐文件修复至测试编译通过；测试编译错误由本轮 `test-compile` 产生后修复（`-Dmaven.test.skip=true` 阶段不编译测试，无移交清单）。
  - Skill: `nop-testing`
- [ ] Fix: A3' test 桥接适配（FQN 级补登条目，1 文件）——`TestErpB2bAsnInventoryIntegration` 对 pur 实体（Long）与本域 String id 边界的 20 处 FQN 引用点加局部 String↔Long 转换（与 Phase 2 桥接同型，转换点入 grep 例外清单，退役 owner M2.5），修复至该测试编译且行为通过。
  - Skill: `nop-testing`
- [ ] Fix: C2 后向 test 适配——`TestErpB2bPartnerOnboarding`（md + notify 双指向，backward-197/198）对 String 化 API 的适配（M1.2 登记的 successor 义务兑付）。
  - Skill: `nop-testing`
- [ ] Fix: 快照每域重录（用户裁决固定步骤）——`RECORDING` 模式运行 b2b service 测试 → 逐案审核 `_cases/` 新形态（324 文件基线：261 csv + 53 yaml + 10 json5；id 以 String 形态落盘）→ 注解还原（grep 零 RECORDING/forceSaveOutput 残留）→ 切回 `CHECKING` 复跑确认全绿。重录足迹（内容 diff vs 新增落盘分列）与审核结论记录本计划。
  - Skill: `nop-testing`
- [ ] Proof: `mvn test -pl module-b2b/erp-b2b-service,module-b2b/erp-b2b-web`（D3 口径：不带 `-am`）全绿——service 12 测试类 + web BUILD SUCCESS（`ErpB2bWebPagesTest` 治理排除，0 tests 预期）。若复现平台 IoC 回归，按 md/notify 先例修复（test-scope VFS delta）并登记。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] b2b 域级测试全绿（service 12 类；web 治理排除偏差登记）；快照重录完成且 `CHECKING` 复跑通过；重录清单在案

### Phase 4 - 语义陷阱 grep 门控 + 收尾登记

Status: planned
Targets: `module-b2b/**`（手写代码）、`docs/backlog/id-string-migration-roadmap.md`、`docs/logs/2026/{08-21 或执行日}.md`、`tools/id-migration-registry.json5`
Skill: none

- Item Types: `Proof | Add`
- Prereqs: Phase 3

- [ ] Proof: 语义陷阱 grep 门控（路线图横切 §3，b2b 手写 main+test 范围）清零——`\.longValue\(\)`、`Long\.parseLong\(`、`Map<Long`、`Set<Long`、`String\.format\("%d` 及 `%d` 变体零命中（A2 桥接转换点为登记例外，逐条列于例外清单并标注退役 owner M2.5/M2.6）；Long 装箱 `==`/`!=` 比较（id 上下文）逐条核清；残留 `Long` 逐条判定合法非 id 或登记 successor；sql-lib.xml 仓内零存在（M0.1 已核，注明即可）。结果逐项记录本计划。
  - Skill: none
- [ ] Proof: 手写 view.xml 零改动验证——`git status module-b2b/erp-b2b-web` 确认无手写 view 文件被动变更（生成 view 随 codegen 更新不在此列）。
  - Skill: none
- [ ] Add: 登记册状态更新与补登——(i) 在 `tools/id-migration-registry.json5` 补登 A3' 条目（b2b test FQN 前向耦合：`TestErpB2bAsnInventoryIntegration` → pur，20 处，owner M3.8，退役 owner M2.5），并在登记册文档 §6.4 追注「扫描器 import 口径对 FQN 内联引用盲区」说明（补登证据 = 本计划 Phase 1 复扫记录）；(ii) A3' 适配完成后（Phase 3）条目 status → retired；(iii) A2 main 桥接 7 条（pur 6 编译级 + sal 1 语义级）保持 active（退役 owner M2.5/M2.6，晚域翻转时退役并移除本域桥接点——例外清单留双向指针）。
  - Skill: none
- [ ] Add: owner doc 注记——grep `docs/design/b2b/` 中关于 b2b id 为 Long/数字的陈述；存在则就地注记 Java 层已 String 化（引用本计划），不存在则记录「零 Long id 陈述，零文档变更」结论。
  - Skill: none
- [ ] Add: 路线图 M3.8 → `done`（M2/M3 表位次 4 + 头部「最后更新」；位次 5 contract 解锁）+ 日志条目（含验证状态）。
  - Skill: none

Exit Criteria:

- [ ] grep 门控零残留（例外为零或逐条核清记录 + 桥接例外清单在案）；view 零手改动在案
- [ ] 路线图状态、登记册、日志三者一致

## Draft Review Record

- Independent draft review iteration 1（2026-08-21，双独立子 agent fresh session）：
  - 审查者 A（技术/执行视角 plan-audit，ses_fdb729db4ffeVRTSo9qFQXAdPI）：`needs revision` — 0 BLOCKER / 1 MAJOR / 3 MINOR。**MAJOR**：A3 = 0 断言被 FQN 级耦合证伪——`TestErpB2bAsnInventoryIntegration` 以全限定名（无 import）引用 pur 实体 20 处（:111-:434），import 口径登记册扫描器盲区；要求补登 A3' 桥接（定位面/disposition/grep 例外归宿/退役 owner M2.5）+ 回报登记册扫描器盲区。MINOR：orgId ×9/partnerId ×5/partnerId 计数修正；service Long 文件 12 非 14（UblInvoiceEdiProvider 零 Long，单列语义桥）；md import :9 锚点。**保护区域技术批准已给出**（明示独立于记账修订）。
  - 审查者 B（治理/规范视角，ses_fdb727e6affed0cGqzLfS1Gte2）：`passes draft review` — 0 BLOCKER / 0 MAJOR / 2 MINOR（FK 子计数、Long 文件计数口径——与审查者 A 的 MINOR 同源）。保护区域治理批准：五要素证据链齐备，变更面 = 40 列 stdDataType、stdSqlType 不变 DDL 零变化。
  - **修订（已落地）**：MAJOR + 全部 MINOR——Current Baseline 补登 A3' FQN 条目（1 文件 20 处，退役 owner M2.5）+ Phase 1 (iii) FQN 复扫门（rule-6 联动）+ Phase 3 A3' 适配项（grep 例外归宿）+ Phase 4 登记册补登与盲区追注 + Deferred But Adjudicated A3' 条目 + Goals 更新；FK 子计数 orgId ×9/partnerId ×5/partnerProfileId ×3；service Long 12 文件（UblInvoiceEdiProvider 单列）；md import :9 锚点；「移交清单」措辞改为「Phase 3 首轮 test-compile 产生」。
- Independent draft review iteration 2（2026-08-21，fresh session 复审修订后文本）：
  - 复审者（技术/执行视角，ses_fdb6895b4ffeOw2YQNEQBE823r）：`passes draft review` — MAJOR/3 MINOR 全部 resolved（逐项复核表在案）；复扫命令实测 = 恰 1 文件/20 行命中、零 import 误报；无结构回归（Skill 行/计数/路线图交叉核对全过）；新增 1 MINOR（20 行 vs 22 token 计数约定注记——已补入 Phase 1 (iii)）。**保护区域技术批准 reaffirm APPROVE**（变更面仍精确 = 40 列 stdDataType，37 自有 + 3 stub 对齐翻转）。
  - 批准后 MINOR 修订注记：计数约定注记未变更范围、命令口径与批准依据。
- **双独立子 agent 批准（保护区域 `model/*.orm.xml`，`ai-autonomy-policy.md` `auto + dual-agent-approval`）**：
  - 批准 1（技术视角）：ses_fdb6895b4ffeOw2YQNEQBE823r（iteration 2 复审 reaffirm；iteration 1 技术审查者 ses_fdb729db4ffeVRTSo9qFQXAdPI 亦明示批准独立于记账修订），2026-08-21 — 「批准 M3.8 b2b orm 保护区域变更（技术视角批准）」。依据：`orm-model-design.md` §主键设计方案 B 强制规则 + M0.1 seq-string Proof 4/4 绿 + M1.1/M1.2 同机制先例 + dry-run 副本实测精确 40 行 stdDataType-only diff。
  - 批准 2（治理视角）：ses_fdb727e6affed0cGqzLfS1Gte2，2026-08-21 — 「批准 M3.8 b2b orm 保护区域变更（治理视角批准）」。依据：五要素证据链（平台 design doc 方案 B + `domain-design-guidelines.md` §16A.4 + M0.1 Proof + M1.1 双批准先例 + M0 裁决 D3/D4/D6）；变更面 = 40 列 stdDataType、stdSqlType 不变 DDL 零变化。批准后修订未触及变更面/命令口径/证据链（iteration 2 技术复审对修订后文本全量复核通过）。
- 共识达成（2026-08-21）：iteration 2 技术复审 + iteration 1 治理审查 0 BLOCKER / 0 MAJOR + 保护区域双批准 → 计划转 `active`。

## Closure Gates

> 完整仓库验证定制为域级口径（路线图规则 3 D3 修订：禁止以全量构建为中间 gate；全量构建仅存在于 M4.1）。

- [ ] 范围内行为完成（40 列落源 + no-am 重生成 + 手写代码/测试修复 + A2 桥接落桥/语义核验 + 快照重录 + grep 门控清零）
- [ ] 相关文档对齐（owner doc 注记或零变更结论、路线图 M3.8 状态、日志）
- [ ] 已运行验证：`mvn clean install -pl module-b2b/erp-b2b-codegen,module-b2b/erp-b2b-dao,module-b2b/erp-b2b-meta,module-b2b/erp-b2b-service,module-b2b/erp-b2b-web,module-b2b/erp-b2b-app,module-b2b/erp-b2b-api -DskipTests` 全绿 + `mvn test -pl module-b2b/erp-b2b-service,module-b2b/erp-b2b-web` 全绿 + 工具重扫零残留（b2b 段 `NEEDS FIX` = 0）
- [ ] 无范围内项目降级为 deferred/follow-up（web 页面测试治理排除为已提交决策 + M4.1 successor 登记，属偏差登记而非范围降级）
- [ ] 保护区域双独立子 agent 批准记录落盘（Phase 1 前置）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### `ErpB2bWebPagesTest` 页面校验

- Classification: `watch-only residual`
- Why Not Blocking Closure: `@Tag("full-app")` + surefire excludedGroups 为先于本 mission 的已提交治理决策（plan 2026-07-24-0930-1），实证依赖全量 classpath
- Successor Required: `yes`（M4.1 app-erp-all `ErpAllWebPagesTest`）

### A3' test 桥接（FQN 级补登：`TestErpB2bAsnInventoryIntegration` → pur，20 处）

- Classification: `watch-only residual`
- Why Not Blocking Closure: D4 登记册消费协议的补登条目——登记册扫描器 import 口径盲区（FQN 内联引用），本计划 Phase 1 复扫发现并补登；purchase 未迁移（位次 15），转换点为测试编译必需；退役 owner M2.5 翻转 pur 时退役并移除桥接点
- Successor Required: `yes`（M2.5）

### A2 main 桥接 7 处（pur 6 编译级 + sal 1 语义级，String↔Long 临时转换/核验）

- Classification: `watch-only residual`
- Why Not Blocking Closure: D4 登记册预先登记的中间态桥接——purchase/sales 未迁移（位次 15/16），桥接点为编译必需（pur）/反射路径核验（sal）；退役 owner M2.5/M2.6 翻转对应域时退役条目并移除桥接点
- Successor Required: `yes`（M2.5 回收 pur 6 条；M2.6 回收 sal 1 条）

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
