# 2026-08-22-0002-3-bigint-id-m35-cs-migration 主键/外键 string 化 M3.5：cs 域迁移（冻结序位次 8）

> Plan Status: active（2026-08-22：iteration 1-2 独立草案审查收敛 + 保护区域双独立子 agent 批准（技术 ses_fdaecac03ffezjlpdWu00j4cJW / 治理 ses_fdaec67abffet95xJrKGrvjzSu），见 Draft Review Record）
> Mission: id-string-migration
> Work Item: M3.5（cs，冻结序位次 8）
> Last Reviewed: 2026-08-22
> Source: `docs/backlog/id-string-migration-roadmap.md` M2/M3 冻结总序表位次 8（M3.5）
> Related: `docs/plans/2026-08-22-0002-1-bigint-id-m21-finance-migration.md`（批内序 1，本计划硬前置）、`docs/plans/2026-08-22-0002-2-bigint-id-m24-assets-migration.md`（批内序 2）、`docs/plans/2026-08-21-2025-3-bigint-id-m36-contract-migration.md`（标准结构先例）、`docs/plans/2026-08-21-1657-1-bigint-id-m02-forward-coupling-registry.md`（M0.2 登记册，消费来源）
> Audit: required（保护区域 `model/*.orm.xml`：独立 plan-audit + 双独立子 agent 批准，批准记录落盘本文件）

## Current Baseline

- **cs 域规模（2026-08-22 实况 scan）**：`module-cs/model/app-erp-cs.orm.xml` 需改列 **68 = 自有 66（PK 18 + BIGINT FK 48，18 实体）+ notGenCode md stub 2**（`ErpMdOrganization`/`ErpMdPartner` 各 1 列——md 权威源自 M1.1 已 String，翻转 = 与权威源对齐）。自有 FK 含 `orgId` ×13（自有实体 orgId FK 列实测；`name="orgId"` XML 原始出现 17 次含 `<index>` 成员引用，以列定义口径为准）+ `ticketId`/`customerId`/`contractId` 等本域 FK。**不改列**：VARCHAR FK 7 列（`ErpCsTicket.assignedToId`（用户 id）、`ErpCsTeam.teamLeaderId`、`ErpCsTicketAction.operatorId`、`ErpCsContract.attachmentFileId`、`ErpCsServiceCatalogItem.fulfillmentProcessId` 等，显式 string 本就 String）+ `delVersion` 等非 PK/FK BIGINT 列（规则 4 保持 long）。
- **模块链与编译依赖（pom 实测）**：7 模块 = `module-cs/erp-cs-{codegen,dao,meta,service,web,app,api}`（全链构建，无延后）。`erp-cs-service` main compile 依赖 **notify-dao + md-dao + crm-dao + qa-dao（未迁移 Long jar——A2 桥接对象）** + common-service + nop-report；fin-service 为 **test-scope** 依赖（cs-service pom 注释自述「cs-service 主类 DAG 不含 finance，故测试期引入 finance-service 仅取字体资源」；main `import app.erp.fin` 零命中（实测，test 亦零命中）——fin String jar 对 cs 编译中性，M2.1 前置仅因冻结序 test 边口径）；cs-web main 依赖本域 meta/service + nop-web（md-service 为 **test-scope**）；cs-web test-scope 依赖 ast-dao/prj-dao（**批内序 2（M2.4）已 install String 形态 ast-dao；prj-dao 陈旧 Long jar**，页面测试编译对象——预期零 id 穿越，Phase 3 核验）；cs-app 无域级 web 依赖（可建）。
- **cs orm 外域关系（refEntityName 实测）**：md ×17（M1.1 起 cs-dao `_gen` md 关系胶水处于已登记中间态（惰性 jar 陈旧未重编译），**本域迁移即自愈**）；fin/crm/qa ×0（crm/qa 耦合全在 service 代码层 A2 桥接）。
- **M0.2 登记册 cs 视角（§6.8，起草消费已核）**：A1 orm 延后 = 0；**A2 main 桥接 8 条 = crm 4（bridge-main-053..056：`TicketAssignResolver:5/:6/:52/:59`，`ErpCrmTeam`/`ErpCrmTeamMember` 类型级 + `IErpCrmTeamBiz/TeamMemberBiz.findList` 继承方法——Long 参数签名以晚域翻转时为准）+ qa 4（bridge-main-057..060：`ErpCsTicketEscalateToQualityProcessor:10/:151/:184/:189`，`ErpQaNonConformance` + `IErpQaNonConformanceBiz.findList/save` 继承方法）；退役 owner M3.4（crm，位次 17）/M2.3（qa，位次 13）**；**A3 test 桥接 5 条**（bridge-test-113..117：`TestErpCsCatalogFulfillmentEngine`/`TestErpCsQualityEscalation`/`TestErpCsTicketCreateEnrichment`/`TestMockCrmBizModels`/`TestMockQaBizModels` 引用 crm/qa 实体与 IBiz，owner M3.5 = 本计划 Phase 3 退役）；B 退役义务 = 0；C1 后向 main 2 条（backward-144 → md 7 文件；backward-145 → notify 8 文件）；C2 后向 test 2 条（backward-203 → md 8 测试文件；backward-204 → notify 6 测试文件）。被引用面 = **零**（实测 `rg 'import app.erp.cs'` 外域 main 零命中，登记册 §6.8 无被引用清单——本域迁移无外域破坏面）。
- **手写代码冲击面（实测）**：cs-service main 跨域 import = md 7 文件（C1）+ notify 8 文件（C1，`notify(String,Map,ctx)` 签名不变预期零破坏——M1.2 已证，编译核验即可）+ crm/qa（A2 桥接点 2 文件）；dao 手写 IBiz/值对象 Long 签名以编译器清单为准（M0.1 审计附录 C 本域语义 FK Long 参数清单为 Phase 4 门控输入）。
- **测试资产（实测）**：cs-service **26 个测试类**；`_cases` 快照 **633 文件**；`ErpCsWebPagesTest` `@Tag("full-app")` + surefire excludedGroups 模块级排除（已提交治理决策，successor = M4.1）。
- **手写 page.yaml raw-GraphQL `:Long` 变量（本计划范围内 12 处，迁移即失效的实时缺陷，本批最重页面修复面）**：`erp/cs/pages/ErpCsTicket/kanban.page.yaml` ×11（`$id:Long` ×5 → `ErpCsTicket__assign/start/resolve/close/reopen(ticketId:)` mutation 变量 + `$c:Long` ×6（:44/:107/:155/:213/:271/:300）→ `filter_customerId` 查询变量）+ `erp/cs/pages/ErpCsTicketAction/timeline.page.yaml:36`（`$tid:Long` → `filter_ticketId`）——cs 翻转 String 后静态类型不匹配 → adaptor 静默降级、看板/时间线过滤与动作功能失效（M3.6 结束审计 MAJOR-1 同型，contract version-diff 就地 Fix 先例）——**本计划就地 Fix + cs-web 重建验证**（cs-web 在本计划 7 模块 verify 范围内）。注意 `$a:String`（assignedToId）为 VARCHAR 列本就 String，合法保留。
- **已知风险（先例登记）**：① 平台 IoC 回归 `nopSequenceGenerator` self-wait——六域连续复现，按先例修复（test-scope VFS delta + DeltaOverride delta-layer 补 default 层集）；② no-am 测试 classpath VFS 模块集变化（回退 = seq-proof-yaml 模块禁用模式）；③ 陈旧 jar 二进制不兼容（本地仓 crm/qa/prj dao Long jar——A2 桥接登记例外 / prj test-scope 编译对象；ast-dao 为批内序 2 String 新 jar）。
- **回写机制（M0.1 裁定 Decision A，三步）**：① dry-run 时点刷新；② `verify-id-fix-copy-diff.mjs module-cs` 新鲜度门控；③ 单文件落源 + `git diff` 逐行审核。禁止盲 cp、禁止 apply 模式。
- **剩余差距**：cs orm 68 列全 `stdDataType="long"` 待改；cs 手写代码/测试/快照全部 Long 形态；冻结序位次 8（位次 9 hr 起归后续批次）。

## Goals

- cs 域 68 列（自有 66 + md stub 2）`stdDataType` long→string 落源（唯一源文件变更，`stdSqlType` 保持 BIGINT，DDL 零变化）。
- 增量重生成（no-am 7 模块链）+ 编译器驱动修复 cs 全部手写代码 + A2 前向桥接 8 处落桥（crm 4 + qa 4，退役 owner M3.4/M2.3）。
- 快照每域重录（RECORDING→CHECKING；633 文件基线）。
- 语义陷阱 grep 门控清零 + page.yaml `:Long` 12 处就地 Fix（cs-web 重建验证）。
- 消费 M0.2 登记册：A2/A3 桥接 disposition 落盘，C1/C2 修复定位面消费，heal M1.1 登记的 cs-dao `_gen` md 胶水中间态。
- 路线图 M3.5 → `done` + 日志。

## Non-Goals

- 不迁移 crm/quality 域（A2 桥接目标域，归 M3.4/M2.3——均在后续批次）。
- 不改 `delVersion` 等非 PK/FK BIGINT 列（保持 long）；不修 `ErpCsWebPagesTest` 治理排除（successor M4.1）。
- 不跑全量构建/全量测试/E2E/compliance checker（归 M4.1）；不手改任何生成件；手写 view.xml 预期零改动（Phase 4 验证）。
- 不做 fin web/app 补做与其他域的桥接退役（归各自 owner plan，见登记册 B 义务）。

## Task Route

- Type: `implementation-only change`（含保护区域 ORM 变更）
- Owner Docs: `docs/backlog/id-string-migration-roadmap.md` M2/M3 表位次 8 + 横切 §5 设计证据（`../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md` §主键设计方案 B + `docs/design/domain-design-guidelines.md` §16A.4 + M0.1 审计结论 + M0 裁决 §10）；cs 业务语义 owner doc = `docs/design/customer-service/`（Phase 4 注记对象）
- Skill Selection Basis: 路线图 §M1-M3「预期技能」指定域迁移 plan 加载 `nop-backend-dev` + `nop-testing`；ORM 变更机制由 M0.1 审计与平台文档背书。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（无 DB DDL 变更；DB 列保持 BIGINT）。no-am 构建硬前置 = 最后全绿基线 commit 全量 install + md/notify/common 链 install + 位次 3-5 链 install + **批内前置：M2.1 fin 5 模块链 install（String 形态）**（cs test-scope fin-service 依赖 + 冻结序批内顺序）+ 批内序 2（M2.4 assets）链 install（冻结序执行约束——cs 精确前置仅为 M2.1+M1.1+M1.2，但本批按冻结总序在 assets 之后执行）。回滚策略：revert orm.xml + `mvn clean install -pl module-cs/erp-cs-codegen,module-cs/erp-cs-dao,module-cs/erp-cs-meta,module-cs/erp-cs-service,module-cs/erp-cs-web,module-cs/erp-cs-app,module-cs/erp-cs-api -Dmaven.test.skip=true` 重生成回 Long 形态（**Phase 3 完成后回滚需先 revert 测试代码**——String 测试代码对 Long main 会破坏 test-compile）。

## Execution Plan

### Phase 1 - 消费登记册 + orm 回写（保护区域，双批准前置）

Status: planned
Targets: `module-cs/model/app-erp-cs.orm.xml`
Skill: none

- Item Types: `Proof | Fix`
- Prereqs: M2.1 ✅（批内序 1 完成且 fin 链已 install）+ M1.1 ✅ + M1.2 ✅（精确前置满足）；批内序 2（M2.4）done（冻结序执行约束）；本计划已通过独立 plan-audit + 第二独立子 agent 复核（保护区域 `auto + dual-agent-approval`，批准记录落盘 Draft Review Record）

- [ ] Proof: 消费 M0.2 登记册——读取 `tools/id-migration-registry.json5` + 登记册文档 §6.8 cs 节，逐条核对：(i) A1 orm 延后 = 0（68 列全翻转）；(ii) A2 main 桥接 8 条（crm 4 + qa 4）与本地实测 import 对账（findList/save 为 ICrudBiz 继承方法，Long 参数签名以晚域翻转时为准——桥接按值流转语境落桥）；(iii) A3 = bridge-test-113..117 作为 Phase 3 定位面；(iv) C1 = backward-144（md 7 main 文件）+ backward-145（notify 8 main 文件）与 C2 = backward-203（md 8 测试文件）+ backward-204（notify 6 测试文件）作为 Phase 2/3 定位面；(v) 按 b2b A3' 先例做 FQN 盲区复扫（`rg 'app\.erp\.(hr|inv|mnt|prj|pur|sal|qa|crm|drp|log|mfg)\.' module-cs/erp-cs-service/src/test` 排除 import 行——覆盖本域执行时点全部未迁移晚域，早域 md/notify/aps/b2b/contract/fin/ast 已 String 无盲区风险），命中则补登 + 处置。矛盾则按路线图规则 6 停止回报。
  - Skill: none
- [ ] Proof: 双独立子 agent 批准记录落盘（批准人指针 + 结论 + 时间），未获批不得进入回写。
  - Skill: none
- [ ] Fix: 回写 orm（M0.1 裁定三步机制）——① `node tools/check-bigint-id-types.mjs dry-run` 时点刷新；② `node tools/verify-id-fix-copy-diff.mjs module-cs` 新鲜度门控（零非 stdDataType 行）；③ 门控通过后单文件落源。禁止盲 cp 静态副本、禁止 apply 模式。
  - Skill: none
- [ ] Proof: `git diff module-cs/model/app-erp-cs.orm.xml` 逐行核对——仅 68 列 `stdDataType="long"→"string"`（自有 66 = PK 18 + FK 48 + md stub 2），`stdSqlType` 零变化、`delVersion`/标签结构零变化；scan cs 段重扫零 `NEEDS FIX`/零 `DEFERRED` 残留。
  - Skill: none

Exit Criteria:

- [ ] 登记册消费核对在案（含 FQN 盲区复扫结论）；双批准记录在案；新鲜度门控 + git diff + 工具重扫三重证明变更面精确 = 68 列 stdDataType

### Phase 2 - 增量重生成 + 主代码编译修复 + A2 桥接落桥

Status: planned
Targets: `module-cs/erp-cs-dao/src/main/java/**`、`module-cs/erp-cs-service/src/main/java/**`（手写 IBiz/BizModel/Processor/Job/SPI；api beans 生成件随动）
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: Phase 1

- [ ] Fix: `mvn clean install -pl module-cs/erp-cs-codegen,module-cs/erp-cs-dao,module-cs/erp-cs-meta,module-cs/erp-cs-service,module-cs/erp-cs-web,module-cs/erp-cs-app,module-cs/erp-cs-api -Dmaven.test.skip=true`（D3 口径：7 模块显式列表、不带 `-am`、`-Dmaven.test.skip=true`）触发增量重生成。预期：cs-dao `_gen` md 关系胶水自愈（M1.1 登记中间态）。
  - Skill: `nop-backend-dev`
- [ ] Fix: 编译器驱动修复主代码——逐条修复 cs dao + service 手写代码类型错误（定位面：md 7 文件（C1，String 直传/toLong 桥按语境）+ notify 8 文件（C1，签名不变预期零破坏核验）+ 全域 IBiz/值对象 Long 签名 + `.getId()` 下游；以编译器实际清单为准），直到 7 模块链 `-Dmaven.test.skip=true` 构建全绿。修复清单落盘本计划。
  - Skill: `nop-backend-dev`
- [ ] Fix: A2 前向桥接 8 处落桥（D4 消费协议）——cs String id ↔ crm/qa Long API 的调用点加转换桥（`TicketAssignResolver` ×4（crm，findList 查询构造的 id 值语义桥——**eq/filter 值桥主动识别**，contract 037/038 先例：Long 列传 String 静默空匹配）+ `ErpCsTicketEscalateToQualityProcessor` ×4（qa，findList/save 的 nonConformance 值/实体字段桥）），每处登记 grep 例外清单（条目 id + file:line + 转换方向），退役 owner M3.4（crm 4）/M2.3（qa 4）；代码内 bridge 注释双向指针。
  - Skill: `nop-backend-dev`
- [ ] Fix: 自身链破坏处置（D4 carve-out）——no-am 口径下预期零外域破坏（7 模块全绿，reactor 不含外域模块；本域被引用面 = 零）；未登记破坏按路线图规则 6 停止回报；已登记破坏按中间态继续并履行登记义务。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] cs 7 模块链（显式列表、no-am、`-Dmaven.test.skip=true`）构建全绿（main 代码）；主代码修复清单 + A2 桥接例外清单在案

### Phase 3 - 测试修复 + A3 桥接适配 + 快照重录 + 域级测试

Status: planned
Targets: `module-cs/**/src/test/**`、`module-cs/erp-cs-service/_cases/**`
Skill: `nop-testing`

- Item Types: `Fix | Proof`
- Prereqs: Phase 2

- [ ] Fix: 测试代码修复——26 个测试类的 Long 用法（字面量断言、helper 签名、seed `orm_propValueByName("id", id)` 形态——md/notify 先例），逐文件修复至测试编译通过；cs-web test-scope ast-dao（String 新 jar）/prj-dao（陈旧 Long jar）编译对象零 id 穿越核验（`ErpCsWebPagesTest` 治理排除但参与 test-compile）。
  - Skill: `nop-testing`
- [ ] Fix: A3 test 桥接适配（bridge-test-113..117，5 条）——本域测试引用 crm/qa 实体与 IBiz 的 id 形态桥接（String↔Long 局部转换或 mock 桩签名适配，与 Phase 2 桥接同型），适配后在登记册退役对应 test 桥接条目（owner M3.5 = 本计划）。
  - Skill: `nop-testing`
- [ ] Fix: C2 后向 test 适配——md 8 + notify 6 测试文件对 String 化 API 的适配（M1.1/M1.2 登记的 successor 义务兑付）。
  - Skill: `nop-testing`
- [ ] Fix: 快照每域重录（用户裁决固定步骤）——`RECORDING` 模式运行 cs service 测试 → 逐案审核 `_cases/` 新形态（633 文件基线；id 以 String 形态落盘；非确定性单元格按 aps/contract 先例 `*` 通配修正）→ 注解还原（grep 零 RECORDING/forceSaveOutput 残留）→ 切回 `CHECKING` 复跑确认全绿。重录足迹（内容 diff vs 新增落盘分列）与审核结论记录本计划。
  - Skill: `nop-testing`
- [ ] Proof: `mvn test -pl module-cs/erp-cs-service,module-cs/erp-cs-web`（D3 口径：不带 `-am`）全绿——service 26 测试类 + web BUILD SUCCESS（`ErpCsWebPagesTest` 治理排除，0 tests 预期）。若复现平台 IoC 回归，按先例修复（test-scope VFS delta + DeltaOverride delta-layer 补 default 层集）并登记。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] cs 域级测试全绿（service 26 类；web 治理排除偏差登记）；快照重录完成且 `CHECKING` 复跑通过；重录清单在案

### Phase 4 - 语义陷阱 grep 门控 + page.yaml Fix + 收尾登记

Status: planned
Targets: `module-cs/**`（手写代码 + cs-web 手写 page.yaml）、`docs/backlog/id-string-migration-roadmap.md`、`docs/logs/2026/{08-22 或执行日}.md`、`tools/id-migration-registry.json5`
Skill: none

- Item Types: `Proof | Fix | Add`
- Prereqs: Phase 3

- [ ] Proof: 语义陷阱 grep 门控（路线图横切 §3，cs 手写 main+test 范围）清零——`\.longValue\(\)`、`Long\.parseLong\(`、`Map<Long`、`Set<Long`、`String\.format\("%d` 及 `%d` 变体零命中（A2 桥接转换点为登记例外，逐条列于例外清单并标注退役 owner M3.4/M2.3）；Long 装箱 `==`/`!=` 比较（id 上下文）逐条核清；id 序比较陷阱（String 字典序）专项 grep：`getId\(\)\s*[<>]|comparing.*getId`；残留 `Long` 逐条判定合法非 id 或登记 successor；sql-lib.xml 仓内零存在（注明即可）。结果逐项记录本计划。
  - Skill: none
- [ ] Fix: cs-web 手写 page.yaml raw-GraphQL `:Long` 变量 12 处就地 String 化——`ErpCsTicket/kanban.page.yaml` ×11（`$id:Long` ×5 → `:String`（ticketId mutation 变量）+ `$c:Long` ×6（:44/:107/:155/:213/:271/:300）→ `:String`（filter_customerId 查询变量））+ `ErpCsTicketAction/timeline.page.yaml:36`（`$tid:Long` → `:String`）；`|| 0` 类 Int 字面量兜底改 `|| ''`（若存在）、variables 链与 options value 链一致性核证（contract version-diff 先例）；随后 `rg ':Long' module-cs/erp-cs-web/src/main/resources/_vfs --glob '!**/_gen/**'` 清零（非 id 类型变量如 `$lim:Int`/`$s:String` 合法保留并逐条判定）；cs-web 重建 BUILD SUCCESS 验证。
  - Skill: none
- [ ] Proof: 手写 view.xml 零改动验证——`git status module-cs/erp-cs-web` 确认无手写 view 文件被动变更（生成 view 随 codegen 更新不在此列；page.yaml 修复 diff 为本计划主动变更）。
  - Skill: none
- [ ] Add: 登记册状态更新——A3 test 桥接 5 条（bridge-test-113..117）status → retired（owner M3.5 兑付 note：转换点/读路径核验结论）；A2 main 桥接 8 条保持 active（退役 owner M3.4（crm 4）/M2.3（qa 4），代码内 bridge 注释双向指针）；被引用面确认 = 零（无 successor 义务）。
  - Skill: none
- [ ] Add: owner doc 注记——grep `docs/design/customer-service/` 中关于 cs id 为 Long/数字的陈述；存在则就地注记 Java 层已 String 化（引用本计划），不存在则记录「零 Long id 陈述，零文档变更」结论。
  - Skill: none
- [ ] Add: 路线图 M3.5 → `done`（M2/M3 表位次 8 + 头部「最后更新」；本批三计划收口——位次 9 hr 起归后续批次）+ 日志条目（含验证状态）。
  - Skill: none

Exit Criteria:

- [ ] grep 门控零残留（例外逐条核清 + 桥接例外清单在案）；page.yaml `:Long` 清零 + cs-web 重建绿 + view 零被动变更在案
- [ ] 路线图状态、登记册退役（5 条）、日志三者一致

## Draft Review Record

- Independent draft review iteration 1（2026-08-22，双独立子 agent fresh session）：
  - 审查者 A（技术/执行视角 plan-audit，ses_fdaecac03ffezjlpdWu00j4cJW）：`needs revision` — 0 BLOCKER / 2 MAJOR / 4 MINOR。事实核对大部分属实（68 = 66+2 精确；A2 调用点行级核验；A3 5 文件存在；C1/C2 rg 精确；26/633；page.yaml 12 处；**`import app.erp.fin` main+test 双零命中证实 pom 级主张**；冻结序依赖吻合）。**MAJOR-1**：A2 拆分「crm 5 + qa 3」错误——登记册 §6.8/json5 为 **crm 4（053-056）+ qa 4（057-060）**，错误拆分传播至 Goals/Phase 4 退役指令/Deferred 后续 owner 记账。**MAJOR-2**：fin-service 在 cs pom 为 **test-scope**（pom 注释「主类 DAG 不含 finance，测试期引入仅取字体资源」）非 main compile 依赖——依赖模型陈述与实仓 pom 矛盾（main 零 import/编译中性结论仍成立）。MINOR：③ kanban `$c:Long` 为 ×6 非 ×5（`$id:Long` ×5，合计 11 不变）；④ cs-web md-service test-scope；⑤ orgId ×17 XML 口径（真实 13）；⑥ 回滚 test-compile 陷阱。
  - 审查者 B（治理/规范视角，ses_fdaec67abffet95xJrKGrvjzSu）：`needs revision` — 0 BLOCKER / 1 MAJOR / 3 MINOR。**MAJOR-1**：owner-doc 指针 `docs/design/cs/` 实仓不存在——cs 设计文档位于 `docs/design/customer-service/`，如按错误路径执行 Phase 4 grep 将产出虚假「零陈述」结论使 owner-doc 对齐门控失效。MINOR：① A2 拆分 crm4+qa4（同技术侧）；② FQN regex 漏晚域 hr/mfg/drp/log；③ ast-dao「陈旧 Long jar」与批内序 2 install 后事实矛盾（措辞精度）。
  - **修订（iteration 1 → 2，已落地）**：全部 2+1 MAJOR + 7 MINOR 已处理——A2 拆分全篇改 crm 4 + qa 4（基线/Goals/Phase 1/Phase 4/Deferred 五处）；fin-service 改 test-scope 依赖模型（含 pom 注释引证 + 冻结序 test 边口径说明）；owner doc 路径改 `docs/design/customer-service/`（Task Route + Phase 4 两处）；kanban `$c:Long` ×6/$id ×5；cs-web md-service test-scope；orgId ×13；回滚 `-Dmaven.test.skip=true` + 阶段差异；FQN regex 覆盖全部未迁移晚域；ast-dao String 新 jar / prj-dao 陈旧 jar 分列措辞。
- Independent draft review iteration 2（2026-08-22，双审查者同会话复审）：双审查者 `passes draft review`（0 BLOCKER / 0 MAJOR）。全部 iteration 1 发现核实解决：A2 拆分 crm 4 + qa 4 六处一致（live-verified 吻合登记册 §6.8/json5）；fin-service test-scope 依赖模型（pom 注释 verbatim）；owner-doc 路径 `docs/design/customer-service/`（目录存在性证实）；FQN regex 覆盖全部未迁移晚域；orgId ×13/回滚标志/ast-dao String 新 jar 措辞全清。唯一遗留：Phase 4 line 131 `$c:Long` ×5 计数标签陈旧（baseline 已正确 ×6；文件合计 ×11 与 rg 清零门控使漏执行不可能）——按 plan 1「批准后 MINOR 修订注记」模式于转 active 时同步（已落地 ×6 + 行号清单）。
  - **双独立子 agent 批准（保护区域 `model/*.orm.xml`，`ai-autonomy-policy.md` `auto + dual-agent-approval`）**：
    - 批准 1（技术视角）：ses_fdaecac03ffezjlpdWu00j4cJW，2026-08-22 — 「批准 M3.5 cs orm 保护区域变更（技术视角批准）」。依据：68 列 = 自有 66（PK 18 + FK 48）+ md stub 2 与 md 权威源对齐，零登记册延后、零跨域关系边、零被引用面，D3 no-am 7 模块与 D4 桥接 disposition 经登记册/pom 实证。
    - 批准 2（治理视角）：ses_fdaec67abffet95xJrKGrvjzSu，2026-08-22 — 「批准 M3.5 cs orm 保护区域变更（治理视角批准）」。依据：owner-doc 门控缺陷已解决（双路径指向真实目录）、68 列表面精确有界且机器门控、五要素证据链与文件内双批准门控完整、登记册 §6.8 消费条目准确。
- 共识达成（2026-08-22）：iteration 2 双审查者 0 BLOCKER / 0 MAJOR + 保护区域双批准 → 计划转 `active`。

## Closure Gates

> 完整仓库验证定制为域级口径（路线图规则 3 D3 修订：禁止以全量构建为中间 gate；全量构建仅存在于 M4.1）。

- [ ] 范围内行为完成（68 列落源 + no-am 7 模块重生成 + 手写代码/测试修复 + A2 落桥与 A3 退役 + 快照重录 + grep 门控清零 + page.yaml 12 处 Fix）
- [ ] 相关文档对齐（owner doc 注记结论、路线图 M3.5 状态、登记册退役（5 条）、日志）
- [ ] 已运行验证：`mvn clean install -pl module-cs/erp-cs-{codegen,dao,meta,service,web,app,api} -DskipTests` 全绿 + `mvn test -pl module-cs/erp-cs-service,module-cs/erp-cs-web` 全绿 + 工具重扫零残留（cs 段 `NEEDS FIX` = 0）
- [ ] 无范围内项目降级为 deferred/follow-up（web 页面测试治理排除为已提交决策 + M4.1 successor 登记，属偏差登记而非范围降级）
- [ ] 保护区域双独立子 agent 批准记录落盘（Phase 1 前置）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### A2 main 桥接 8 处（crm 4 + qa 4，String↔Long 临时转换）

- Classification: `watch-only residual`
- Why Not Blocking Closure: D4 登记册预先登记的中间态桥接——crm（位次 17）/qa（位次 13）未迁移，桥接点为编译必需
- Successor Required: `yes`（M3.4 回收 crm 4 条；M2.3 回收 qa 4 条——晚域翻转时退役条目并移除本域桥接点）

### `ErpCsWebPagesTest` 页面校验

- Classification: `watch-only residual`
- Why Not Blocking Closure: `@Tag("full-app")` + surefire excludedGroups 为先于本 mission 的已提交治理决策（plan 2026-07-24-0930-1），实证依赖全量 classpath
- Successor Required: `yes`（M4.1 app-erp-all `ErpAllWebPagesTest`）

### 平台 IoC 回归 delta（若 Phase 3 复现并落盘）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 平台 `nopSequenceGenerator` bean-init-self-wait 为已登记平台 Bug，先例修复 = test-scope VFS delta
- Successor Required: `yes`（平台修复后统一移除，M4.1 复核）

## Closure

Status Note: （待执行与结束审计）

Closure Audit Evidence:

- （待独立结束审计）

Follow-up:

- （无范围内跟进项；已确认缺陷不得出现在此处。中间态 successor 指针见 Deferred But Adjudicated 与 Phase 4 登记记录。）
