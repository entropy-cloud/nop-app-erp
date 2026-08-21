# 2026-08-21-1657-2-bigint-id-m12-notify-migration 主键/外键 string 化 M1.2：notify 域迁移（第二根域）

> Plan Status: active（2026-08-21，iteration 1 双独立审查 `passes draft review` + 保护区域双独立子 agent 批准，见 Draft Review Record；执行硬前置 = M0.2 done）
> Mission: id-string-migration
> Work Item: M1.2
> Last Reviewed: 2026-08-21
> Source: `docs/backlog/id-string-migration-roadmap.md` M1.2
> Related: `docs/plans/2026-08-21-1045-1-bigint-id-m0-order-freeze-audit-proofs.md`（M0.1，前置）、`docs/plans/2026-08-21-1045-2-bigint-id-m13-common-service-orgid-string.md`（M1.3，前置）、`docs/plans/2026-08-21-1045-3-bigint-id-m11-master-data-migration.md`（M1.1，先导试点与 D3/D4 口径来源）、`docs/plans/2026-08-21-1657-1-bigint-id-m02-forward-coupling-registry.md`（M0.2，前置，登记册消费来源）
> Audit: required（保护区域 `model/*.orm.xml`：独立 plan-audit + 双独立子 agent 批准，批准记录落盘本文件）

## Current Baseline

- **notify 域规模（2026-08-21 实况 scan）**：`module-notify/model/app-erp-notify.orm.xml` 需改列 **7 = PK 3 + BIGINT FK 4**——PK：`ErpSysNotificationTemplate.id`、`ErpSysNotification.id`、`ErpSysNotificationRead.id`；FK：`ErpSysNotification.templateId`/`recipientPartnerId`/`recipientDeptId`、`ErpSysNotificationRead.notificationId`。**不改列**：VARCHAR FK 3（`recipientUserId`/`mergeGroupId`/`Read.userId`，显式 `stdDataType="string"` 本就 String）+ `delVersion` 3（非 PK/FK 保持 long，路线图规则 4）。notify 为**唯一无 orgId 域**（roadmap M1.2 注记），common-service 隔离语义（M1.3 已 String 化）无本域适配面。
- **根域属性（零前向边）**：notify main 手写代码跨域 import 实测 **0**（含 `NotificationRecipientResolver`——其接收人解析走平台 `NopAuthUser`/`NopAuthRole`/`NopAuthUserRole`（nop-auth 平台实体，非 erp 域，不在迁移范围）+ `IDaoProvider`）；notify orm 跨域 refEntityName 实测 0。预期 M0.2 登记册 notify 视角 = 零前向义务（Phase 1 消费核对）。
- **手写代码冲击面（实测）**：dao 手写 8 文件，Long 签名唯一 = `IErpSysNotificationBiz.java:29` `markRead(@Name("notificationId") Long notificationId, ctx)`（`IErpSysNotificationReadBiz`/`IErpSysNotificationTemplateBiz` 零 Long）；service 手写 15 文件，Long 相关 5 文件——`ErpSysNotificationMarkReadProcessor:14`（`markRead(Long)`）、`AbstractErpSysNotificationProcessor:47/78`（`isRead(Long,...)`/`newReadEntry(Long,...)`）、`NotificationMergeCoordinator:96`（`isRead(Long,...)`）、`ErpSysNotificationBizModel:62`（`markRead(Long)`）、`NotificationDispatcher:206`（`renderTemplate(Long templateId,...)`）+ dispatcher 内 12 处 `.getId()`；另 3 处 `Set<Long> readIds`（`ErpSysNotificationBizModel:93/:128`、`AbstractErpSysNotificationProcessor:63`，由 `r.getNotificationId()` 喂入，迁移后编译必断）；web main 手写 Java 实测 **0**；app 模块手写仅 `ErpNotifyApplication.java`（标准启动类，0 Long，零风险）。执行时以编译器清单为准。
- **测试资产（实测）**：service 6 个 `@Test` 类 23 方法（`TestErpSysNotificationSubscription` 3 / `TemplateLifecycle` 3 / `SeedTemplates` 4 / `CrossDomain` 3 / `Dispatch` 5 / `RecipientResolverRuntime` 5）+ `ErpNotifyWebPagesTest`（`@Tag("full-app")` + erp-notify-web pom surefire `<excludedGroups>` 模块级排除——与 md 相同的已提交治理决策（plan 2026-07-24-0930-1），页面校验 successor = M4.1 `ErpAllWebPagesTest`）+ 2 个 codegen 入口类。快照 `_cases` = **140 文件 / 23 案例目录，CSV 表格快照形态**（`autotest.yaml` + `input/tables/*.csv` + `output/tables/*.csv`，含 `erp_sys_notification{,_template,_read}.csv` 与 `nop_sys_sequence.csv`；非 md 的 json5 形态）——重录语义：RECORDING 重跑后 output 表以迁移后形态落盘（id 列 String 形态），`nop_sys_sequence.csv` 为平台序列表不受影响。
- **下游耦合面（关键——notify 迁移的破坏面预判）**：外域 java 文件引用 `IErpSysNotificationBiz`（名引用）= 64 文件、import `app.erp.notify.*` = 72 文件（rg 口径：`rg -l 'IErpSysNotificationBiz' -g '*.java' | grep -v '^module-notify/'` / `rg -l '^import app\.erp\.notify\.' -g '*.java' | grep -v '^module-notify/'`；import 面全部为 IBiz，外域 java 零 notify 实体 import），但外域 main 调用**全部为 `notify(eventType, Map, ctx)`**（String/Map 签名，迁移后不变——`IErpSysNotificationBiz` 仅 `markRead` 携带 Long 且**仓内外部调用者 0**；19 orm 反向 `refEntityName="app.erp.notify.*"` = 0，无 md 式 `_gen` 对称胶水破坏）；`module-common-test/FaultInjectionStubs.java` 为 JDK Proxy 桩（`InvocationHandler` 泛化分发，编译安全，仅捕获 String eventType）。**预期 notify 迁移零 main 代码级下游编译破坏**；破坏面限于外域 **test** 代码引用 notify 实体/IBiz（如 contract/cs 等域 test 文件 import `app.erp.notify.*`）——属晚域 test-scope 后向耦合，各 successor 域 plan Phase 3 愈合，M4.1 兜底（Phase 4 登记清单）。
- **模块链与构建口径（D3，M0 裁决）**：7 模块 = `module-notify/erp-notify-{codegen,dao,meta,service,web,app,api}`；verify = 显式列表**不带 `-am`**（上游经本地 Maven 仓库解析；硬前置 = 最后全绿基线 commit 全量 install + md 链/common 链 install，均已就位）。notify-service pom test 依赖 `app-erp-common-test`（已证实 Proxy 桩编译安全）+ main 依赖 `app-erp-common-service`（M1.3 已 String 化）。
- **已知风险（md 先例登记）**：① 平台 IoC 回归 `nopSequenceGenerator` bean-init-self-wait（`docs/bugs/2026-08-21-nop-sequence-generator-ioc-self-wait-after-platform-reinstall.md`）——notify 序列 id 生成路径重度使用 seq，若复现按 md 先例修复模式（test-scope VFS delta `ioc:lazy-property` 镜像平台先例）；② no-am 测试 classpath VFS 模块集变化（已知登记中间态，回退方案 = seq-proof-yaml 模块禁用模式）；③ 陈旧 jar 二进制不兼容（本地仓未迁移 dao jar 引用旧 `getId()` 签名——D3 已登记，域级测试按设计不跨这些边界）。
- **回写机制（M0.1 裁定 Decision A，三步）**：① `node tools/check-bigint-id-types.mjs dry-run` 时点刷新（**须在 M0.2 豁免机制落地后执行**——本批执行顺序保证）；② `node tools/verify-id-fix-copy-diff.mjs module-notify` 新鲜度门控（零非 stdDataType 行）；③ 单文件落源 + `git diff` 逐行审核。禁止盲 cp 静态副本、禁止 apply 模式回写。
- **剩余差距**：notify orm 7 列全 `stdDataType="long"` 待改；notify 手写代码/测试/快照全部 Long 形态；冻结序位次 2（notify 之后 aps/b2b 等 17 域待迁移）。

## Goals

- notify 域 7 列 PK/FK `stdDataType` long→string 落源（唯一源文件变更，`stdSqlType` 保持 BIGINT，DDL 零变化）。
- 增量重生成（no-am 7 模块链）+ 编译器驱动修复 notify 全部手写代码（dao IBiz `markRead` 签名、service 5 文件 Long 语义、测试）。
- 快照每域重录（RECORDING→CHECKING，CSV 表格快照形态，用户裁决——不依赖 Number 宽容）。
- 语义陷阱 grep 门控清零（路线图横切 §3 清单，notify 范围）。
- 消费 M0.2 登记册：核对 notify 视角条目集（预期零前向义务），登记下游后向耦合 successor 指针。
- 路线图 M1.2 → `done` + 日志；冻结序位次 3（aps）解锁。

## Non-Goals

- 不修复外域 test/main 代码对 notify 的引用破坏（contract/cs 等 test 文件 import `app.erp.notify.*`）——各 successor 域 plan Phase 3 修复（路线图横切 §1：中间态设计使然；本计划仅登记清单 + successor 指针）。
- 不改 `delVersion` 等非 PK/FK BIGINT 列（保持 long）；不改 VARCHAR FK 列（本就 String）。
- 不跑全量构建/全量测试/E2E（归 M4.1）；不跑 compliance checker（归 M4.1）。
- 不手改任何生成件（`_gen/`、`_` 前缀文件）；手写 view.xml 预期零改动（dict 先例已实证，Phase 4 验证）。
- 不修 `ErpNotifyWebPagesTest` 的治理排除（`@Tag("full-app")` 为先于本 mission 的已提交决策，页面校验 successor = M4.1）。
- 不做 notify owner docs 之外的文档重写（`domain-design-guidelines.md` §16A 清理归 M4.1）。

## Task Route

- Type: `implementation-only change`（含保护区域 ORM 变更）
- Owner Docs: `docs/backlog/id-string-migration-roadmap.md` M1.2 + 横切 §5 设计证据（`../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md` §主键设计方案 B + `docs/design/domain-design-guidelines.md` §16A.4 + M0.1 审计结论 + M0 裁决 §10）；notify 业务语义 owner doc = `docs/design/notify/`（Phase 4 注记对象）
- Skill Selection Basis: 路线图 §M1-M3「预期技能」指定域迁移 plan 加载 `nop-backend-dev`（BizModel/跨实体约定）+ `nop-testing`（快照重录 RECORDING→CHECKING 流程）；ORM 变更机制由 M0.1 审计与平台文档背书。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（无 DB DDL 变更、无端口/密钥/外部服务；DB 列保持 BIGINT）。回滚策略：revert orm.xml + `mvn clean install -pl module-notify/erp-notify-codegen,module-notify/erp-notify-dao,module-notify/erp-notify-meta,module-notify/erp-notify-service,module-notify/erp-notify-web,module-notify/erp-notify-app,module-notify/erp-notify-api -DskipTests` 重生成回 Long 形态。

## Execution Plan

### Phase 1 - 消费登记册 + orm 回写（保护区域，双批准前置）

Status: planned
Targets: `module-notify/model/app-erp-notify.orm.xml`
Skill: none

- Item Types: `Proof | Fix`
- Prereqs: M0.1 done ✅ + M1.3 done ✅ + M1.1 done ✅（冻结序位次 1 先行；no-am 构建硬前置「md 链 install 已就位」即其产物）+ **M0.2 done（本批 plan 1，硬前置）**；本计划已通过独立 plan-audit + 第二独立子 agent 复核（保护区域 `auto + dual-agent-approval`，批准记录落盘 Draft Review Record）

- [ ] Proof: 消费 M0.2 登记册——读取 notify 视角条目集并逐条核对：预期 (i) 零 orm 级列延后条目（notify orm 跨域 refEntityName = 0）、(ii) 零 service 级前向桥接义务（notify main 跨域 import = 0）、(iii) 作为晚域零退役义务、(iv) 后向被引用清单（外域 test 引用 notify 实体/IBiz 文件集）作为 Phase 4 登记输入。若登记册存在 notify 前向条目与本地实测矛盾，按路线图规则 6 停止回报（登记册与实况冲突 = 真相源冲突，不得自行裁定）。
  - Skill: none
- [ ] Proof: 双独立子 agent 批准记录落盘（批准人指针 + 结论 + 时间），未获批不得进入回写。
  - Skill: none
- [ ] Fix: 回写 orm（M0.1 裁定三步机制）——① `node tools/check-bigint-id-types.mjs dry-run` 时点刷新；② `node tools/verify-id-fix-copy-diff.mjs module-notify` 新鲜度门控（零非 stdDataType 行）；③ 门控通过后单文件落源。禁止盲 cp 静态副本、禁止 apply 模式。
  - Skill: none
- [ ] Proof: `git diff module-notify/model/app-erp-notify.orm.xml` 逐行核对——仅 7 列 `stdDataType="long"→"string"`（PK 3 + FK 4），`stdSqlType` 零变化、VARCHAR FK/delVersion/标签结构零变化；`node tools/check-bigint-id-types.mjs` scan notify 段重扫零 `NEEDS FIX`/零 `DEFERRED` 残留。
  - Skill: none

Exit Criteria:

- [ ] 登记册消费核对在案（零前向义务确认或冲突已停止回报）；双批准记录在案；新鲜度门控 + git diff + 工具重扫三重证明变更面精确 = 7 列 stdDataType

### Phase 2 - 增量重生成 + 主代码编译修复

Status: planned
Targets: `module-notify/erp-notify-dao/src/main/java/**`、`module-notify/erp-notify-service/src/main/java/**`（手写接口/实现/BizModel/Processor/dispatch；web main 手写实测 0）
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: Phase 1

- [ ] Fix: `mvn clean install -pl module-notify/erp-notify-codegen,module-notify/erp-notify-dao,module-notify/erp-notify-meta,module-notify/erp-notify-service,module-notify/erp-notify-web,module-notify/erp-notify-app,module-notify/erp-notify-api -Dmaven.test.skip=true`（D3 口径：7 模块显式列表、**不带 `-am`**、**必须 `-Dmaven.test.skip=true`** 隔离测试编译——预期 service 测试在本阶段边界编译断）触发增量重生成（`_gen/` 实体、I*Biz、xmeta、view、api 契约；生成件零手改）。
  - Skill: `nop-backend-dev`
- [ ] Fix: 编译器驱动修复主代码——逐条修复 notify dao + service 手写代码类型错误（基线预判：`IErpSysNotificationBiz.markRead` :29、`ErpSysNotificationMarkReadProcessor` :14、`AbstractErpSysNotificationProcessor` :47/:78、`NotificationMergeCoordinator` :96、`ErpSysNotificationBizModel` :62、`NotificationDispatcher` :206 及 dispatcher 12 处 `.getId()`；以编译器实际清单为准），直到 7 模块链 `-Dmaven.test.skip=true` 构建全绿（main 代码）。修复清单（错误类型 × 处数，分 dao/service 层）+ 测试编译错误清单移交 Phase 3。
  - Skill: `nop-backend-dev`
- [ ] Fix: 自身链破坏处置（D4 carve-out）——no-am 口径下 reactor 不含外域模块，预期零外域破坏；若出现**未登记**编译破坏（含本地仓陈旧 jar 二进制不兼容的编译期表现），按路线图规则 6 停止回报；登记册内已登记破坏按已登记中间态继续并履行登记义务（破坏模块清单 + successor 指针 + 逐模块 javac 错误点清单）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] notify 7 模块链（显式列表、no-am、`-Dmaven.test.skip=true`）构建全绿（main 代码）；主代码修复清单 + 测试编译错误移交清单在案

### Phase 3 - 测试修复 + 快照重录 + 域级测试

Status: planned
Targets: `module-notify/**/src/test/**`、`module-notify/erp-notify-service/_cases/**`
Skill: `nop-testing`

- Item Types: `Fix | Proof`
- Prereqs: Phase 2

- [ ] Fix: 测试代码修复——6 个 service 测试类的 Long 用法（基线预判每类 1-2 处：`Long`/`long` 字面量断言与 helper 签名）+ Phase 2 移交清单，逐文件修复至测试编译通过。
  - Skill: `nop-testing`
- [ ] Fix: 快照每域重录（用户裁决固定步骤）——`RECORDING` 模式运行 notify service 测试 → 逐案审核 `_cases/` 新形态（23 案例目录，CSV 表格快照：实体 id 列以 String 形态落盘）→ 切回 `CHECKING` 复跑确认全绿。重录足迹（内容 diff vs 行尾差异分列）与审核结论记录本计划。
  - Skill: `nop-testing`
- [ ] Proof: `mvn test -pl module-notify/erp-notify-service,module-notify/erp-notify-web`（D3 口径：不带 `-am`）全绿——service 6 测试类 23 方法 + web 模块 BUILD SUCCESS（`ErpNotifyWebPagesTest` 按已提交治理排除，0 tests 预期）；含重录后快照比对。若复现平台 IoC 回归（`nopSequenceGenerator` self-wait），按 md 先例修复（test-scope VFS delta）并在执行记录登记。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] notify 域级测试全绿（service 23 方法；web 治理排除偏差登记）；快照重录完成且 `CHECKING` 复跑通过；重录清单在案

### Phase 4 - 语义陷阱 grep 门控 + 后向耦合登记 + 收尾

Status: planned
Targets: `module-notify/**`（手写代码）、`docs/backlog/id-string-migration-roadmap.md`、`docs/logs/2026/{08-21 或执行日}.md`
Skill: none

- Item Types: `Proof | Add`
- Prereqs: Phase 3

- [ ] Proof: 语义陷阱 grep 门控（路线图横切 §3，notify 手写 main+test 范围）清零——`\.longValue\(\)`、`Long\.parseLong\(`、`Map<Long`、`String\.format\("%d` 及 `%d` 变体零命中；Long 装箱 `==`/`!=` 比较（id 上下文）逐条核清；残留 `Long` 逐条判定合法非 id（如计数返回值）或登记 successor；sql-lib.xml 仓内零存在（M0.1 已核，注明即可）。结果逐项记录本计划。
  - Skill: none
- [ ] Proof: 手写 view.xml 零改动验证——`git status module-notify/erp-notify-web` 确认无手写 view 文件被动变更（生成 view 随 codegen 更新不在此列）。
  - Skill: none
- [ ] Add: 下游后向耦合登记（D3 登记义务）——登记册消费所得 + 实测复核：外域引用 notify 实体/IBiz 的破坏预判清单（main 侧 `notify(String,Map,ctx)` 签名不变零破坏 + `markRead` 外部调用 0 已证；test 侧引用文件集逐域列出）+ successor 指针（各域 plan Phase 3）+ M4.1 兜底。
  - Skill: none
- [ ] Add: owner doc 注记——grep `docs/design/notify/`（含 `inbox-patterns.md` 等）中关于 notify id 为 Long/数字的陈述；存在则就地注记 Java 层已 String 化（引用本计划），不存在则记录「零 Long id 陈述，零文档变更」结论。
  - Skill: none
- [ ] Add: 路线图 M1.2 → `done`（Work Item Status 表 + 依赖图 + 头部「最后更新」；位次 3 aps 解锁）+ 日志条目（含验证状态）。
  - Skill: none

Exit Criteria:

- [ ] grep 门控零残留（例外为零或逐条核清记录）；view 零手改动在案；下游登记清单在案
- [ ] 路线图状态、登记、日志三者一致

## Draft Review Record

- Independent draft review iteration 1（2026-08-21，双独立子 agent fresh session）：
  - 审查者 A（技术/执行视角 plan-audit，ses_fdc6974f9ffeUnIkgurMlNAxRb）：`passes draft review` — 0 BLOCKER / 0 MAJOR / 3 MINOR。事实核对全部属实：7 列逐行精确（PK :56/:90/:135 + FK :91/:94/:95/:136，VARCHAR FK 3 显式 string，全文件 long 列恰 10 = 7+3）；跨域 main import = 0；Long 签名 6 处 + dispatcher 12 处 `.getId()` 逐行吻合；外域 main 57 处 `.notify(` 调用全为 String/Map/ctx 三参、`markRead` 外部调用 0、19 orm 反向 refEntityName notify = 0（无 `_gen` 对称胶水破坏）；测试资产 6 类 23 方法、`_cases` 140 文件（117 csv + 23 yaml）、web 治理排除实测在案。D3 命令口径与 M1.1 修订后逐字一致。MINOR ①基线遗漏 3 处 `Set<Long> readIds`（:93/:128/:63）→ 已补入基线；②「85 文件」计数不可复现 → 已改 rg 口径（名引用 64 / import 72，含复核命令）；③`ErpNotifyApplication.java` 未入盘点 → 已补注零风险。
  - 审查者 B（治理/规范视角，ses_fdc69491bffeUtMutDmuF8BZZ2）：`passes draft review` — 0 BLOCKER / 0 MAJOR / 1 MINOR（Phase 1 Prereqs 未显式列 M1.1 done → 已补）。保护区域协议三处落点（Phase 1 Prereqs/Proof/Closure Gates）核验通过；反松弛扫描零违禁词；模板结构/Item Types/Skill 行/文档义务四项归属全部合规；Non-Goals 与 roadmap 横切 §1/规则 3/规则 4 逐项对齐。
  - **双独立子 agent 批准（保护区域 `model/*.orm.xml`，`ai-autonomy-policy.md` `auto + dual-agent-approval`）**：
    - 批准 1（技术视角）：ses_fdc6974f9ffeUnIkgurMlNAxRb，2026-08-21 — 「批准 M1.2 orm 保护区域变更（技术视角批准）」。依据：`orm-model-design.md` §主键设计方案 B（:30/:186/:199 BIGINT+seq-default+string 强制方向）+ M0.1 seq-string Proof 4/4 绿 + M1.1 同机制 68 列先例四 Phase 全绿 + notify 零反向 orm 耦合（风险面严格小于先例）。
    - 批准 2（治理视角）：ses_fdc69491bffeUtMutDmuF8BZZ2，2026-08-21 — 「批准 M1.2 orm 保护区域变更（治理视角批准）」。依据：设计证据链五要素齐备（平台 design doc 方案 B + `domain-design-guidelines.md` §16A.4 + M0.1 seq-string Proof + M1.1 双批准先例 + M0 裁决 D3/D4/D6）；变更面 = 7 列 stdDataType、stdSqlType 不变 DDL 零变化。
  - 批准后 MINOR 修订注记：4 项 MINOR 均为事实精度补充/计数口径修正/前置显式化，未变更范围、D3 命令口径与批准依据（orm 变更面仍 = 7 列 stdDataType long→string）。
- 共识达成（2026-08-21）：iteration 1 双审查者 0 BLOCKER / 0 MAJOR + 保护区域双批准 → 计划转 `active`。

## Closure Gates

> 完整仓库验证定制为域级口径（路线图规则 3 D3 修订：禁止以全量构建为中间 gate；全量构建仅存在于 M4.1）。

- [ ] 范围内行为完成（7 列落源 + no-am 重生成 + 手写代码/测试修复 + 快照重录 + grep 门控清零）
- [ ] 相关文档对齐（owner doc 注记或零变更结论、路线图 M1.2 状态、下游登记、日志）
- [ ] 已运行验证：`mvn clean install -pl module-notify/erp-notify-codegen,module-notify/erp-notify-dao,module-notify/erp-notify-meta,module-notify/erp-notify-service,module-notify/erp-notify-web,module-notify/erp-notify-app,module-notify/erp-notify-api -DskipTests` 全绿（7 模块自身链，no-am；结束阶段测试已修复，`-DskipTests` 语义正确）+ `mvn test -pl module-notify/erp-notify-service,module-notify/erp-notify-web` 全绿（service 23 方法；web 治理排除偏差登记）+ 工具重扫零残留（notify 段 `NEEDS FIX` = 0）
- [ ] 无范围内项目降级为 deferred/follow-up（web 页面测试治理排除为先于本计划的已提交决策 + M4.1 successor 登记，属偏差登记而非范围降级）
- [ ] 保护区域双独立子 agent 批准记录落盘（Phase 1 前置）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 外域 test 代码对 notify 实体/IBiz 的引用（后向耦合）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 路线图横切 §1 设计使然——晚域 test 引用早域 String API 的编译破坏属预期中间态，notify verify 闭包（7 模块自身链，no-am）不含外域模块；main 侧已证零破坏（`notify(String,Map,ctx)` 签名不变 + `markRead` 外部调用 0）
- Successor Required: `yes`（各 successor 域 plan Phase 3；M4.1 兜底）

### `ErpNotifyWebPagesTest` 页面校验

- Classification: `watch-only residual`
- Why Not Blocking Closure: `@Tag("full-app")` + surefire excludedGroups 为先于本 mission 的已提交治理决策（plan 2026-07-24-0930-1），实证依赖全量 classpath
- Successor Required: `yes`（M4.1 app-erp-all `ErpAllWebPagesTest`）

## Closure

Status Note: （待收尾填充）

Closure Audit Evidence:

（待收尾填充）

Follow-up:

- （无；已确认缺陷不得出现在此处）
