# 2026-09-28-1735-1 GATE-03 验证链新鲜度包装（tools/validate-flux-fresh.sh）

> Plan Status: completed
> Last Reviewed: 2026-09-28
> Source: `docs/backlog/perf-ux-debt-consolidation-roadmap.md` M1 GATE-03（stale jar 三案防线，lessons/26）
> Related: `docs/lessons/26-stale-m2-jar-invalidates-frontend-validation.md`（失败模式与防御规则）；`scripts/validate-flux-pages.sh`（被包装的既有验证链，语义不得修改）；`docs/architecture/flux-page-export-and-validation.md`（工具链契约与 §7 当前门禁口径）；`tools/check-hardcoded-cjk.mjs`（lesson 20 自证范式）
> Audit: required

## Current Baseline

- 既有验证链 `npm run validate:flux`（= `scripts/validate-flux-pages.sh`）：①`mvn -pl app-erp-all test -Dtest=ErpAllFluxPagesTest` 导出全部页面到 `app-erp-all/target/flux-pages/`（含 `manifest.json`：`pageCount`/`failedPages[]`/`generatedAt`）→ ②校验 nop-chaos-flux dist 就绪 → ③validate-pages.mjs 校验 erp 子树、报告落 `_tmp/flux-page-validation-report.json`（`totals: {files, validated, errors, warnings, skipped, strippedXui}`）→ ④nop 子树校验（平台残留非阻塞）。
- **退出码实况（2026-09-28 实测 + validate-pages.mjs:297 源码实裁）**：validator `totals.errors > 0 → exit 1`；erp 子树当前 325 error（已裁决 `variant="primary"` 基线族）→ **validate:flux 当前按设计 exit 1**（owner doc §7：门禁口径=零新增对照 325 基线，全链 exit 0 待漂移族清零）。本包装必须捕获该退出码继续新鲜度断言，不得因 exit 1 中断、也不得改写其语义（roadmap 硬约束）。
- **lessons/26 失败模式（stale jar 三案）**：验证链消费 `.m2` 构件而非源文件；`mvn -pl app-erp-all test` 只重装聚合模块自身，模块资源（view.xml/page.yaml/xmeta）改动后不重装即验证 = 验证旧态。防御规则 1（修正→重装→校验显式串联）、规则 2（报告 mtime vs 源 mtime 新鲜度自证 + `failedPages` 与 totals 同时核对）、规则 3（审计方交叉证伪抓手）目前全靠执行者自律与审计人肉，无常驻机制。
- **产物新鲜度现状（实测）**：`_tmp/flux-page-validation-report.json` mtime（1790574898）晚于最新触及源 `ErpHrEmployee.view.xml`（1790574788）——批次 5 终态一致；`app-erp-all/target/flux-pages/manifest.json` 当前不存在（target 清理后未重跑），佐证「产物缺席」是常态路径而非常态异常。
- 页面族源文件面：`git ls-files` ∩ `.view.xml`/`.page.yaml`/`.flux.yaml` ∩ `/src/main/resources/`（排除 `/target/`）实测 **1624**（855 page.yaml + 738 view.xml + 31 flux.yaml；git 追踪的 /target/ 路径 0 条）；bash `[[ -nt ]]` 逐文件比较在该量级下毫秒级（darwin bash 3.2.57 实测可用）。**bash 兼容约束（m-8）**：darwin 缺省 bash 3.2——禁用 `declare -A`/`mapfile`/`${var,,}` 等 bash 4+ 特性。
- 剩余差距：三步串联与新鲜度自证无脚本强制，lesson 26 防御规则 1/2 依赖人肉记忆。

## Goals

- 新增 `tools/validate-flux-fresh.sh`（bash，`set -euo pipefail`），两模式：
  - **全链模式（缺省）**：「触及模块 `mvn install -DskipTests` 逐个重装 → `npm run validate:flux` → 新鲜度断言」。触及模块来源 = 位置参数（Maven 模块目录，如 `module-cs/erp-cs-web`）；**无参数时从 `git status --porcelain` 脏文件向上找最近 `pom.xml` 祖先自动推导**，推导边界（草案审查 M-3）：①解析命中**根 pom**（实仓根 `pom.xml` packaging=pom 在位，docs/scripts 等路径最坏都爬升到它）或 **packaging=pom 域聚合 pom**（如 `module-cs/pom.xml`——`module-<domain>/model/` ORM 源目录脏文件会命中它，`-pl` 安装只装 pom 不装资源模块，构成「已重装」假象）→ **跳过并计数注记**；②命中 `module-<domain>/model/` 时输出显式提示「ORM 源模型变更走保护区全量 `mvn clean install -DskipTests` 义务，不在本包装自动化范围」。`npm run validate:flux` 的退出码捕获：`rc=0` 干净 / `rc=1` 已知基线条件或链内失败（歧义消解见 Decision-2 与 M-2 链完成断言）/ `rc≥2` 环境错误立即 exit 2。**链完成断言（M-2）**：全链模式记录链起始时间戳，链条结束后强制断言 REPORT 与 MANIFEST 的 mtime ≥ 链起始（本次运行真实产出）——否则 exit 2「chain did not complete」（防 mvn 失败经 `set -e` 以 rc=1 冒出时，旧 REPORT 因页面源未动而 A3 全绿、以 known-325 注记掩盖链断裂的假绿）。
  - **`--check-only` 复验模式**：跳过 mvn 与 validate:flux，仅对既有产物执行新鲜度断言（结束审计/抽样复核的廉价复验通道）。**输出强制携带前置条件警示行**（m-6）：「仅在全链运行后或全量手工构建后有效；严禁在裸 validate:flux 之后当作完成判据」（lesson 26 案②复活路径=先裸跑 validate:flux 刷新 REPORT 再 --check-only，此时 mtime 全绿而 jar 旧）。
- 新鲜度断言（两模式共用；stale jar 三案防线）：
  - **A1 产物出具**：REPORT 存在且 JSON 可解析、`totals` 在位且 `totals.validated > 0`；MANIFEST 存在且 JSON 可解析、`pageCount > 0`。产物缺席 = exit 2（环境/链断裂，区别于策略违规）。
  - **A2 failedPages=0**：`manifest.failedPages` 数组长度必须为 0——导出层健康的独立信号（lesson 26 穿帮点：totals「正常」而 failedPages>0）→ 非 0 = exit 1。
  - **A3 mtime 新鲜度**：全部页面族源文件（`git ls-files --cached --others --exclude-standard`，m-5：含未追踪新页面文件）**逐一**断言不晚于 REPORT **且**不晚于 MANIFEST（bash `[[ -nt ]]`；全树强口径——「改了任何源文件就必须重跑全链」的机械化身，无需定义模糊的「触及集」）。任一源文件更新于任一产物 = stale，exit 1，输出计数 + 至多 10 个 `file :: mtime` 样例。
- `--self-test` 故障注入自测：在 /tmp 临时 git 仓组装沙箱（`git init` + fixture 源文件/REPORT/MANIFEST，路径经 `VALIDATE_FRESH_ROOT`/`REPORT_FILE`/`MANIFEST_FILE` 环境变量注入），断言：①产物新于源 → PASS；②源更新于产物 → exit 1 stale；③failedPages 非空 → exit 1；④totals 缺失 → exit 2；⑤产物缺席 → exit 2。不触主仓树。三条实现约束（草案审查裁决 e）：**源文件集合扫描根必须随 `VALIDATE_FRESH_ROOT` 注入**（否则沙箱场景扫的是主仓 1624 文件）；**self-test 分支先于一切 mvn/npm 调用短路**；**一切 git 操作限 `git -C "$VALIDATE_FRESH_ROOT"`**。审计方可证伪性：self-test 前后主仓 `git status` 逐字节不变 + 脚本走查确认无沙箱根外写路径。
- 接线：npm script `validate:flux:fresh`（根 package.json，与 validate:flux 同面——GATE-01 Decision-3 同一面裁决）。

## Non-Goals

- 不修改 `scripts/validate-flux-pages.sh` / validate-pages.mjs 既有语义（roadmap 硬约束：只串联与叠加断言；325 基线族的「零新增」判断仍属 validate:flux 自身门禁，本包装只透传其退出码）。
- 不做「零新增 error 对照 325 基线」的数值断言（那是 validate:flux 语义的一部分，包装重复断言 = 双真相源）。
- 不扩大重装范围到全仓 `mvn clean install`（触及模块重装 + 聚合导出已覆盖 lesson 26 三案的成因；全量构建耗时不属本门禁定位——需要全量时人工跑 `mvn clean install -DskipTests` 后用 `--check-only` 复验）。
- 不处理 jar 内容抽查（unzip 对照源文件——lesson 26 规则 3 的审计方抓手；mtime 口径已覆盖三案成因，jar 抽查保留为审计方法非门禁）。

## Task Route

- Type: `implementation-only change`（新增 shell 包装脚本 + npm 接线；不触业务行为、契约、保护区）
- Owner Docs: `docs/lessons/26-stale-m2-jar-invalidates-frontend-validation.md`（防御规则的机制化）；`docs/architecture/flux-page-export-and-validation.md`（工具链契约，Phase 2 登记节）；`docs/backlog/perf-ux-debt-consolidation-roadmap.md` GATE-03 行
- Skill Selection Basis: 已扫描 `docs/skills/README.md` 全表——无 shell 包装/验证链类匹配技能；范式复用 lesson 20 自证义务 + lesson 26 防御规则。Skill: none

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（bash/node/git/mvn 均为既有验证链运行时；沙箱自测仅用 /tmp 与 `git init`）

## Execution Plan

### Phase 1 - 脚本实现 + npm 接线

Status: completed
Targets: `tools/validate-flux-fresh.sh`（新增）、`package.json`（+1 script）
Skill: none

- Item Types: `Add`×4 + `Decision`×2
- Prereqs: none

- [x] Add 全链模式：触及模块推导（位置参数 + git 脏文件 pom 祖先爬升）→ 逐模块 `mvn -pl <module> install -DskipTests` → `npm run validate:flux` 退出码捕获（rc≥2 即 exit 2）→ 新鲜度断言调用
      - Skill: none
- [x] Add 新鲜度断言（A1/A2/A3）+ `--check-only` 分支 + 输出汇总（产物 mtime、扫描源文件数、validate:flux rc 注记）+ 退出码 0/1/2
      - Skill: none
- [x] Add `--self-test`：/tmp 沙箱五场景断言（Goals 清单）
      - Skill: none
- [x] Decision-1 mtime 全树强口径 vs roadmap 字面「最新触及源文件」：采用全树页面族源文件逐一比较（≥ 触及集，且消除「触及集如何定义」的模糊面——lesson 26 规则 1 的「改了就重跑全链」正是全树口径）。替代方案「git 脏文件/参数限定触及集」否决：触及集定义依赖调用方输入，漏传即假绿，恰为门禁要防的失败形态。残留风险：①非页面族文件（如 BizModel Java 改动影响渲染面）不纳入 A3——接受：本门禁定位是「页面源 vs 产物」新鲜度，Java 面由 mvn install 环节的重装保证（全链模式强制重装触及模块，test 类路径走新 jar）；②**已提交未重建源的假绿窗口**（草案审查 M-4）：T1 编辑+提交 → T2 无脏文件 → 全链跑导出消费 T0 旧 jar → REPORT mtime=T2 晚于全部源 → A3 绿——git 脏文件推导与 mtime 双双失明的跨会话时序陈旧变体——**显式接受**：跨会话场景由 closure 纪律（lesson 26 防御规则 1 的「改了就重跑全链」）与 jar 抽查 Deferred 兜底，Deferred 触发条件同步追加该形态
      - Skill: none
- [x] Decision-2 validate:flux rc=1 继续而非中断：owner doc §7 实裁当前门禁口径=零新增对照 325 基线（全链 exit 0 待漂移族清零），rc=1 是已裁决常态而非本包装的失败；包装的总退出码 = rc 与新鲜度违规的或（rc≥2 仍即时 exit 2）。替代方案「rc=1 即失败」否决：会使本门禁在 325 族清零前永远红，且与 validate:flux 语义重复。残留风险：rc=1 注记可能被忽视——输出必须显式打印 `validate:flux rc=1 (known 325 baseline family — own gate semantics)` 字样，且汇总行同步打印 `totals.errors` 实时值（m-7：使「零新增对照 325」的人肉核对降为读一行输出）
      - Skill: none
- [x] Add `package.json` script：`validate:flux:fresh`
      - Skill: none

Exit Criteria:

- [x] `--self-test` 五场景全 PASS
- [x] 全链模式对**仅 docs 类脏文件**的工作树实跑（m-11：推导=空集+跳过计数注记，不重装任何模块）→ validate:flux 全链实跑 + A1/A2/A3 断言各自通过（新鲜度面全绿：failedPages=0 + mtime 新鲜 + 链完成断言过）；**wrapper 总退出码 = 1 且 rc=1 注记在案**（预期 errors=325 已知族 → validate:flux rc=1，按 Decision-2 总退出码=rc 与新鲜度违规的或）
- [x] stale 注入实跑：工作树内 touch 一个页面族源文件使新于产物 → `--check-only` 必须 exit 1 且样例定位该文件；`git checkout` 还原该文件 mtime 不可行则重跑全链模式恢复 exit 0（注入仅 mtime 变更不触内容，`git checkout -- <file>` 后重验）

Phase 1/2 执行证据（2026-09-28 实测）：

- `--self-test`：5/5 场景 PASS，exit 0（沙箱 /tmp git 仓，VALIDATE_FRESH_ROOT 注入；修复一处 fixture 路径缺 `/src/main/resources/` 前导斜杠的枚举不匹配）。
- 全链模式实跑（工作树仅 docs/tools 脏文件）：validate:flux 全链（mvn 导出 + JS 校验）→ A1 通过（validated>0、pageCount>0）+ A2 failedPages=0 + A3 扫描 **1624** 页面族源文件 **0 stale** + 链完成断言通过 → `RESULT: exit 1 (validate:flux rc=1 — known 325 baseline family, own gate semantics; freshness assertions above)`，与 Decision-2 口径逐字一致。
- `--check-only`（全链后）：exit 0 `RESULT: PASS (freshness green)`，前置条件警示行在位。
- stale 注入：touch ErpHrEmployee.view.xml → `RESULT: FAIL (A3: 1 page-family source file(s) newer than artifacts — stale verification chain, lessons/26)` + file:mtime 定位；`git checkout --` + mtime 复位后恢复 `RESULT: PASS` exit 0。
- 模块推导探针（derive_modules 直调，VALIDATE_FRESH_ROOT 注入）：17 个 docs/tools 脏/未跟踪文件全部 `SKIP (no module pom ancestor outside root pom)`，0 模块——与全链运行的推导=空集一致。
- `npm run validate:flux:fresh -- --check-only`：链路可执行 exit 0。
- `bash docs/audits/nop-compliance-checker.sh`：R2b=232 / R2c=1559，零漂移。

结束审计整改证据（2026-09-28，审计 agent_db85b2ff needs revision → 整改）：

- **Major-1（ORM-NOTE 死代码）**：`module-*/model/*` 脏文件此前在聚合 pom SKIP 分支 `continue` 后不可达——已将 ORM-NOTE case 前移至 packaging=pom 判断之前。实证探针（content-modified `module-cs/model/app-erp-cs.orm.xml`）：双行输出 `ORM-NOTE ... (ORM source model change -> protected-area full 'mvn clean install -DskipTests' obligation, not automated here)` + `SKIP ... (aggregator/parent pom ancestor installs no resources: module-cs)`；还原后 0 脏文件。Major-2 所需的 view.xml 推导探针同批实跑：content-modified ErpHrEmployee.view.xml → `MODULE module-hr/erp-hr-web` 单行输出。
- **Major-2（Proof-2 证据落盘）**：真实全链实跑（工作树含 content-modified ErpHrEmployee.view.xml）：推导输出 19 条 derive-skip + `derived touched modules` + **`[install] mvn -pl module-hr/erp-hr-web install -DskipTests ...`**（触及模块真实重装）+ `[validate] npm run validate:flux` + A1/A2/A3 全过（1624 文件 0 stale、failedPages=0、链完成断言过）+ `RESULT: exit 1 (validate:flux rc=1 — known 325 baseline family...)`；随后 `git checkout --` 还原——checkout 会把 mtime 刷为当前时刻（晚于链产物），按 Phase 1 同款规程 `touch -t 202609280000` 复位后 `--check-only` 恢复 exit 0（复审计残留-A/B 整改；初版括注「无需 mtime 复位」与事实相反，已更正）。 Proof-2 的「脏 view.xml → 推导出正确模块并重装（mvn -pl 行可见）→ 还原」全链路证据在案。
- 自测复跑 PASS（5/5）；其余审计核验项（self-test/check-only/stale 注入/代码审查/退出码走查/owner doc §13/触及面/checker）审计方已独立复跑全过。
- Phase 2 文档对齐：owner doc 登记节落盘 + roadmap GATE-03 行 done 口径同步（见 Closure 前回写）+ 日志条目落盘。

### Phase 2 - 验证与文档对齐

Status: completed
Targets: `docs/architecture/flux-page-export-and-validation.md`（§12 后新增登记节：validate:flux:fresh 包装定位、调用方式、**--check-only 使用时机前置条件**）、`docs/backlog/perf-ux-debt-consolidation-roadmap.md`（GATE-03 行标记 done 时同步口径措辞：全树逐一比较 vs 字面「最新触及源文件」——m-10 防交付机制与台账文本漂移）、`docs/logs/2026/09-28.md`
Skill: none

- Item Types: `Proof`×3 + `Add`×1
- Prereqs: Phase 1

- [x] Proof `npm run validate:flux:fresh` 链路可执行且退出码语义一致
      - Skill: none
- [x] Proof 触及模块推导实跑验证：手工制造一个脏 view.xml（git status 可见）→ 全链模式推导出正确模块并重装（输出可见 `mvn -pl <module>` 行）→ 还原
      - Skill: none
- [x] Proof `bash docs/audits/nop-compliance-checker.sh` 零漂移确认（纯 shell 工具，lesson 07 廉价保险）
      - Skill: none
- [x] Add owner doc 登记节 + 日志条目
      - Skill: none

Exit Criteria:

- [x] Phase 1 全部 Exit Criteria 保持绿 + 三条 Proof 证据摘要落盘计划本节下方

## Draft Review Record

- Independent draft review iteration 1: needs revision（独立子代理 agent_03f3c4bb，2026-09-28）——事实断言 11 项全实证（含报告 totals 逐字节、页面族 1624 计数、bash 3.2 `-nt` 实测）；**M-1** Phase 1 退出标准「exit 0」与 Decision-2 总退出码自相矛盾（errors=325 ⇒ rc=1 ⇒ wrapper exit 1）→ 改写为「总退出码=1 + rc=1 注记在案 + A1/A2/A3 各自通过」；**M-2** rc=1 歧义（mvn 失败同以 rc=1 冒出，旧 REPORT 因页面源未动而 A3 全绿掩盖链断裂）→ 增链完成断言（REPORT/MANIFEST mtime ≥ 链起始，否则 exit 2 chain did not complete）；**M-3** 推导边界两处（根 pom 兜底使「docs 跳过」不发生、域聚合 pom 构成假重装）→ 推导规则补跳过计数与 model/ 保护区提示；**M-4** 已提交未重建源假绿窗口未裁决 → Decision-1 残留风险②显式接受 + Deferred 触发追加；**m-5~m-12**：枚举含 untracked/--check-only 前置条件警示行/汇总行打印 totals.errors/bash 3.2 约束/ItemTypes 计数/Phase 2 roadmap 回写/「无脏文件」措辞/self-test 三条实现约束——全部修订。事实备注（不属本计划范围）：validate-flux-pages.sh 自身头注「3 步」与 echo 标签 [1/3]…[4/4] 漂移、erp rc=1 时 [4/4] 因 set -e 不可达——既有脚本内注释问题留痕不修。
- Independent draft review iteration 2: RESOLVED（同一审查代理定点复核，agent_e2401fba，2026-09-28）——4 Major+8 Minor 全部忠实落地且相互自洽；M-2 链完成断言与 rc 三分类的组合确实消解「mvn 失败冒充 known-325」假绿路径（两个 rc=1 出口被断言二分）；实仓抽查（根 pom/module-cs 聚合 pom 均 packaging=pom）通过；三处潜在冲突面（链完成断言 vs --check-only/A1 vs 链完成断言/Exit 措辞 vs 或逻辑）均自洽。非阻塞观察 3 条：①触及模块 install 步失败路径建议实施时同样捕获 rc 并路由 exit 2 chain did not complete（已纳入实施）；②Exit Criteria 3 checkout 重验措辞绕但自覆盖；③本行回填。**计划可置 active 实施**。

## Closure Gates

- [x] 范围内行为完成（Phase 1-2 全部退出标准达成）
- [x] 相关文档对齐（owner doc §13 登记节；roadmap 行口径回写随 done 翻转执行；日志已更新）
- [x] 已运行验证（self-test 五场景 + 全链模式实跑 + stale 注入实跑 + npm 链路 + checker 零漂移）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### jar 内容抽查（lesson 26 规则 3 审计方抓手）门禁化

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: mtime 全树口径已覆盖 stale jar 三案成因（源新于产物必拦）；jar 抽查（unzip 对照源文件）是审计方交叉证伪方法而非执行链门禁，门禁化成本高、触发面窄
- Successor Required: `yes`——触发条件：mtime 口径出现第一例绕过实证（如产物被 touch 刷新但 jar 实为旧构件），**或发现「已提交源未重建导致的全绿误证」形态实际发生**（Decision-1 残留风险②）时升格

## Closure

Status Note: 验证链新鲜度包装落地并全验证：self-test 5/5；全链实跑两轮（docs-only 脏文件推导空集轮 + Proof-2 触及模块推导重装轮，均 A1/A2/A3 全过 + 链完成断言过 + rc=1 注记逐字符合 Decision-2）；stale 注入 FAIL→定位→复位恢复 exit 0；npm 链路；checker 零漂移。草案审查 2 轮收敛（4 Major+8 Minor）；结束审计 1 轮 needs revision（2 Major：ORM-NOTE 死代码/Proof-2 证据未落盘）→ 整改（case 前移 + model/ 脏文件双行探针 + 真实全链实跑落盘）→ 复审计确认两 Major 解决、整改引入的 mtime 残留（checkout 未复位）按残留-A/B 整改（复位 + 括注更正 + check-only exit 0 复验）——复审计明示「以上两处完成即可置位结束审计门控与 Plan Status: completed,无需重跑其余项」,凭该授权置位。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计两轮收敛（round 1 agent_db85b2ff needs revision 2 Major；复审计 agent_3170bbf9 独立复跑 ORM-NOTE/view.xml 推导/self-test/证据走查全过 + 残留-A/B 整改授权，fresh session）
- Evidence: 复审计报告——Major-1 确认解决（ORM-NOTE+SKIP 双行独立复跑）；Major-2 确认解决（[install] mvn -pl module-hr/erp-hr-web 行在案 + hr-web jar/产物时间链旁证）；残留-A（ErpHrEmployee.view.xml mtime 晚于链产物 → touch -t 复位后 check-only exit 0 已复验）；残留-B（计划括注更正为如实记录）；self-test 5/5 复跑 PASS；主仓零残留。

Follow-up:

- （无）
