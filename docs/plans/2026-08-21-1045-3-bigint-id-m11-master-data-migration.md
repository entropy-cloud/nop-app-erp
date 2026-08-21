# 2026-08-21-1045-3-bigint-id-m11-master-data-migration 主键/外键 string 化 M1.1：master-data 域迁移（先导试点）

> Plan Status: active
> Mission: id-string-migration
> Work Item: M1.1
> Last Reviewed: 2026-08-21
> Source: `docs/backlog/id-string-migration-roadmap.md` M1.1
> Related: `docs/plans/2026-08-21-1045-1-bigint-id-m0-order-freeze-audit-proofs.md`（M0.1，前置）、`docs/plans/2026-08-21-1045-2-bigint-id-m13-common-service-orgid-string.md`（M1.3，前置）
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

- No infra prereqs beyond existing baseline（无 DB DDL 变更、无端口/密钥/外部服务；DB 列保持 BIGINT，回滚 = revert orm.xml + `mvn clean install -pl module-master-data/erp-md-api,module-master-data/erp-md-app -am -DskipTests` 重生成回 Long 形态）。

## Execution Plan

### Phase 1 - orm 回写（保护区域，双批准前置）

Status: planned
Targets: `module-master-data/model/app-erp-master-data.orm.xml`
Skill: none

- Item Types: `Fix | Proof`
- Prereqs: M0.1 done + M1.3 done；本计划已通过独立 plan-audit + 第二独立子 agent 复核（保护区域 `auto + dual-agent-approval`，批准记录落盘 Draft Review Record）

- [ ] Proof: 双独立子 agent 批准记录落盘（批准人指针 + 结论 + 时间），未获批不得进入后续 Phase。
  - Skill: none
- [ ] Fix: 回写 orm——按 M0.1 Phase 1 裁定机制执行，且无论裁定为何，本计划强制自带新鲜度门控：① 回写前重跑 dry-run 得到**时点副本**（当前工具无 per-domain scope，为全量 dry-run 后取 md 副本；若 M0.1 裁定新增 scope 则按裁定执行）；② `diff 实况源 vs 时点副本` 必须仅含 `stdDataType="long"→"string"` 差异行（零非 stdDataType 行——防回滚 08-13 之后落地的 RC 增量，当前 `_tmp/` 旧副本的 3 处漂移即为此类风险实例）；③ 门控通过后才落源。禁止盲 cp 静态副本。
  - Skill: none
- [ ] Proof: `git diff module-master-data/model/app-erp-master-data.orm.xml` 逐行核对——仅 PK/FK 列 `stdDataType="long"→"string"`（68 处），`stdSqlType` 零变化、`delVersion` 等非 PK/FK 列零变化、标签结构零变化；`node tools/check-bigint-id-types.mjs`（md 范围）重扫零 `NEEDS FIX` 残留。
  - Skill: none

Exit Criteria:

- [ ] 双批准记录在案；新鲜度门控（时点副本 diff 零非 stdDataType 行）+ git diff 与工具重扫双重证明变更面精确 = 68 列 stdDataType

### Phase 2 - 增量重生成 + 主代码编译修复

Status: planned
Targets: `module-master-data/erp-md-dao/src/main/java/**`、`module-master-data/erp-md-service/src/main/java/**`（手写接口/实现/BizModel/Processor/Provider 等；web main 手写 Java 实测 0）
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: Phase 1

- [ ] Fix: `mvn clean install -pl module-master-data/erp-md-api,module-master-data/erp-md-app -am -Dmaven.test.skip=true` 触发增量重生成（19 模块 reactor 含 codegen→dao→meta→service→web→app→api 全链；`_gen/` 实体、I*Biz、xmeta、view、api 契约随模型更新；生成件零手改）。**必须用 `-Dmaven.test.skip=true` 而非 `-DskipTests`**：后者仍编译测试源，而 5 个 `TestStub*` SPI 桩实现 Phase 2 正在改签名的 dao 接口，测试编译在本阶段边界**预期断**（`-DskipTests` 会让 19 模块 install 在 md-service test-compile 处中止，web/app/api 三模块当次不重生成）。
  - Skill: `nop-backend-dev`
- [ ] Fix: 编译器驱动修复主代码——逐条修复 md **dao + service** 手写代码类型错误（`Long id` 参数——含 `IErpMdSupplierPriceResolver`/`IErpMdCustomerPriceResolver`/4 个 `IErpMd*ReferenceChecker`/`SubjectMappingResolver` 等 dao 层 Long id 签名、`.getId()` 赋 Long、`setXxxId(Long)`、`Map<Long,...>` 等），直到 19 模块 reactor `-Dmaven.test.skip=true` 构建全绿（main 代码）。修复清单（错误类型 × 处数，分 dao/service 层）+ 测试编译错误清单（预期含 5 个 `TestStub*`）移交 Phase 3。
  - Skill: `nop-backend-dev`
- [ ] Fix: 闭包内惰性 dao 破坏处置——19 模块闭包含未迁移域 fin/prj/notify 的 dao+codegen 模块（经 common-test optional 依赖拉入）；若这些模块出现 md id 类型耦合编译错误（M0.1 审计 ③ 假设被证伪），**停止本 plan**，按路线图规则 6 回报 M0 裁决（调整顺序或合并域 plan），不自行修复他域代码、不自行重排。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 19 模块 reactor `-Dmaven.test.skip=true` 构建全绿（main 代码，含闭包内 fin/prj/notify dao 模块）；主代码修复清单 + 测试编译错误移交清单（预期含 5 个 `TestStub*`）在案；闭包内零未裁决破坏

### Phase 3 - 测试修复 + 快照重录 + 域级测试

Status: planned
Targets: `module-master-data/**/src/test/**`、`module-master-data/erp-md-service/_cases/**`
Skill: `nop-testing`

- Item Types: `Fix | Proof`
- Prereqs: Phase 2

- [ ] Fix: 测试代码修复——request.json5 显式 id 值、断言中 id 类型/比较、测试 helper 的 Long 用法，以及 Phase 2 移交的测试编译错误（含 5 个 `TestStub*` SPI 桩随新 dao 签名改型），逐文件修复至测试编译通过。
  - Skill: `nop-testing`
- [ ] Fix: 快照每域重录（用户裁决固定步骤）——`RECORDING` 模式运行 md service 测试 → 逐案审核 `_cases/` 新形态（数字 id → String 形态）→ 切回 `CHECKING` 复跑确认全绿。重录文件数与审核结论记录本计划。
  - Skill: `nop-testing`
- [ ] Proof: `mvn test -pl module-master-data/erp-md-service,module-master-data/erp-md-web -am` 全绿（覆盖 25 个测试类全量——service 24 + web 1（`ErpMdWebPagesTest`，`-pl erp-md-service -am` 单选不运行 web 测试，故显式并入），含重录后快照比对）；顺带断言先导 Proof——任一无显式 id 保存路径的测试产物 id 为 String 非空（若 M0.1 Proof 载体为本域则此处为正式落点，双保险来自 M0.1 审计工件）。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] md 域级测试全绿（25 测试类，service 24 + web 1）；快照重录完成且 `CHECKING` 复跑通过；重录清单在案

### Phase 4 - 语义陷阱 grep 门控 + 收尾

Status: planned
Targets: `module-master-data/**`（手写代码）、`docs/backlog/id-string-migration-roadmap.md`、`docs/logs/2026/08-21.md`
Skill: none

- Item Types: `Proof | Add`
- Prereqs: Phase 3

- [ ] Proof: 语义陷阱 grep 门控（路线图横切 §3，md 手写代码范围）清零——`\.longValue\(\)`、`Long\.parseLong\(`、`Map<Long`、`String\.format\("%d`（含 `%d` 变体）零命中；`Long` 装箱 `==`/`!=` 比较（id 上下文）逐条核清；sql-lib.xml `:id` 参数条目仓内零存在（执行时注明即可）。结果逐项记录本计划。
  - Skill: none
- [ ] Proof: 手写 view.xml 零改动验证——`git status module-master-data/erp-md-web` 确认无手写 view 文件被动变更（生成 view 随 codegen 更新不在此列）。
  - Skill: none
- [ ] Add: owner doc 注记——grep `docs/design/master-data/` 与 `docs/design/domain-design-guidelines.md` 中关于 md id 为 Long/数字的陈述；存在则就地注记 Java 层已 String 化（引用本计划），不存在则记录「无 Long id 陈述，零文档变更」结论。
  - Skill: none
- [ ] Add: 路线图 M1.1 → `done`（含先导试点结论：根域迁移后 19 模块闭包（含 fin/prj/notify 惰性 dao）全绿成立与否——不成立时按路线图规则 6 停止并回报 M0 裁决）；日志条目（含验证状态）。
  - Skill: none

Exit Criteria:

- [ ] grep 门控零残留（例外为零或逐条核清记录）；view 零手改动在案
- [ ] 路线图状态、试点结论、日志三者一致

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

> 完整仓库验证定制为域级口径（路线图规则 3：禁止以全量构建为中间 gate；全量构建仅存在于 M4.1）。

- [ ] 范围内行为完成（68 列落源 + 重生成 + 手写代码/测试修复 + 快照重录 + grep 门控清零）
- [ ] 相关文档对齐（owner doc 注记或零变更结论、路线图 M1.1 状态、日志）
- [ ] 已运行验证：`mvn clean install -pl module-master-data/erp-md-api,module-master-data/erp-md-app -am -DskipTests` 全绿（19 模块；结束阶段测试已修复，`-DskipTests` 语义正确）+ `mvn test -pl module-master-data/erp-md-service,module-master-data/erp-md-web -am` 全绿（25 测试类）+ 工具重扫零残留
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 保护区域双独立子 agent 批准记录落盘（Phase 1 前置）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 下游未迁移域对 md 实体的 Long 引用（~120 处）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 路线图横切 §1 设计使然——中间态全量构建失败预期存在，下游域各自 plan 修复；md 的 verify 闭包（19 模块）仅含 fin/prj/notify 的**惰性 dao** 模块（其破坏按 Phase 2 规则 6 条款处置），不含任何下游 **service** 模块。
- Successor Required: `yes`（M2/M3 各域 plan，按 M0.1 冻结序）

## Closure

Status Note: pending

Closure Audit Evidence:

- Auditor / Agent: pending
- Evidence: pending

Follow-up:

- 无（已确认缺陷零；下游引用已有 successor 登记）
