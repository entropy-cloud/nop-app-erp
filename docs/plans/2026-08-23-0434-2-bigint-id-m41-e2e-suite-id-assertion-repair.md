# 2026-08-23-0434-2-bigint-id-m41-e2e-suite-id-assertion-repair 主键/外键 string 化 M4.1（2/3）：E2E 套件 id 断言 String 化修复 + flux 模式全量回归

> Plan Status: active（2026-08-23 独立草案审查两轮收敛：iteration 2 `acceptable as-is`，共识达成；批准记录见 Draft Review Record）
> Mission: id-string-migration
> Work Item: M4.1（其二：E2E 套件 id 断言/传参 String 化修复 + flux 全量回归）
> Last Reviewed: 2026-08-23
> Source: `docs/backlog/id-string-migration-roadmap.md` M4.1 行（E2E 套件修复）+ 横切 §4（Playwright E2E 统一在 M4.1 修复，中途不跑）+ M4 收尾清单（`Number()`→字符串/`eqFilter` 调整）
> Related: `docs/plans/2026-08-23-0434-1-bigint-id-m41-full-build-test-restore.md`（批内序 1，前置：runner jar + 全量构建绿）、`docs/plans/2026-08-23-0434-3-bigint-id-m41-compliance-baseline-docs-closure.md`（批内序 3）、`docs/audits/2026-08-21-1045-id-migration-m0-cross-domain-coupling-appendix.md` 附录 F（E2E 影响面权威清单，M4.1 修复输入）、`docs/testing/e2e-runbook.md`（E2E 编写规范与运行口径 owner doc）
> Audit: required（独立草案审查 + 独立结束审计；无 `model/*.orm.xml` 变更，不触发保护区域双批准）

## Current Baseline

（live 实测 2026-08-23，附录 F 为 08-21 复测口径）

- **E2E 空白期**：E2E 自 mission 启动（08-21）未运行（横切 §4 设计：中途不跑）。**权威基线 = 2026-08-11 全 enforcement 栈 sweep**（known-good-baselines 08-11 行）：`491 passed / 13 failed / 8 skipped`（~82min，六目录 `tests/e2e/{dashboards,negative,crud,reports,orchestration,business-actions}/`，flux 引擎）——**13 失败全在白名单**（7 类：role-login 2（P2.4 known-impact）/ master-data.write.amis（test-infra Non-Goal）/ crud 表单对话框 3（cs-kb-suggestion/cs/finance，enforcement-induced UI 渲染回归归 cross-repo flux successor）/ negative qa riskName 1 + inspectorId FK 2（pre-existing 测试 bug）/ negative mnt PLANNED 字典 2（pre-existing）/ negative sal loginAsRole race 2（test-infra））。次新基线 2026-07-25（490/1/3，七目录含 examples/pages，enforcement OFF 时代）——其命令口径含 `examples/pages` 两目录，08-11 sweep 未含。E2E spec 总量 286 文件（`tests/e2e/**/*.spec.ts`）。
- **enforcement 实况（基线口径前提）**：`app-erp-all/application.yaml` `%test` 块 enforcement 三开关 ON（action-auth + data-auth + role-row-filter，另 SoD `%test=false` config-gate）；webServer.command 与 `_tmp-server.sh` 均带 `-Dquarkus.profile=test` → E2E 服务器必以 enforcement ON 启动。**已知失败口径以 08-11 白名单为起点**（07-25 的「仅 master-data.write.amis」口径系 enforcement OFF 时代产物，不可复现）；`docs/testing/e2e-runbook.md` enforcement 段若与实况相悖（「三开关 OFF」陈述漂移）随本计划 Closure 同步修正。
- **id 冲击面（附录 F 权威 + 08-23 live 快照，口径注明）**：
  - `Number(` 全量：附录 F 命令口径（`*.ts`）08-21 = 874/105 文件；08-23 live 快照 `*.spec.ts` 口径 = 767/102——两口径并列易混，Phase 1 复测统一以附录 F 复核命令（`*.ts`）为准；
  - **id 族简单形态 234 处必改**（`Number(<标识符链>)` 且参数名 `id`/`*Id` 结尾：employeeId 55、voucherId 17、periodId 13、materialId 10、shiftId 9、targetDeptId 8、scenarioId 8、targetPositionId 6 等）；
  - **复合表达式形态 117 处逐个判定**（`.value` 链/内联表达式——id 族改、数值族保持）；
  - **数值族 523+ 处合法保持不改**（金额/控制限/比率——误改即制造回归）；
  - `eqFilter('id'` 08-21 口径 36（`*.ts`；`*.spec.ts` live 30）+ FK 字段 eqFilter（employeeId 13、moveId 10、orderId 7、materialId 5、competencyId 5）——值来源多为行内实体 id，随 String 化调整；
  - **附录 F 清单外形态（08-23 live 实证补登）：字面量数字 id 传参 130 处/44 文件**（`rg '\b[a-zA-Z]*[iI]d:\s*\d+' tests/e2e --glob '*.spec.ts'`：business-actions 89、**negative 19**、reports 10、dashboards 6、visual 6；实例 `negative/e2-1-*.spec.ts:51-55` `orgId: 2`/`customerId: 1` `__save` data、`e1-1-*.spec.ts:30` `DUMMY_ID = 999999`）——GraphQL String 参数传 Int 字面量被类型系统拒绝，setup 将直接崩溃；
  - 既有正向兼容 `String(...[iI]d...)` 27 处——迁移后行为不变，核对即可。
  - 目录分布（`Number(` 锚点）：business-actions 764、orchestration 81、crud 14、dashboards 11、visual 2、negative 2。
- **类型系统硬约束**：GraphQL String 参数传 Int 字面量被类型系统直接拒绝（运行时暴露，无 Number 宽容）；后端全部 PK/FK 已 String（M1-M3 done）。
- **运行模式与基础设施**：flux-only 强制（`E2E_ENGINE` 缺省即 flux；`playwright.config.ts` webServer command 含 `-Dnop.web.render-mode=flux` + 全套 `erp-*` config keys + fresh-DB `-Dnop.orm.init-database-data=true` + `-Dquarkus.profile=test` enforcement 栈）；服务器口径 = `BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1` + `./_tmp-server.sh restart`（**JVM args 须与 webServer.command 同步**，2026-07-25 基线教训）；runner jar 依赖批内序 1 全量构建产物。
- **前置联动**：批内序 1 修复的 4 处 page.yaml（notify inbox markRead / aps schedule-gantt / b2b edi-detail / asn-flow）运行时行为复核归本计划（adaptor 不再静默降级）。复核载体：inbox = 既有 `business-actions/notify-inbox.action.spec.ts`（countUnread/markRead 断言在位）；schedule-gantt/edi-detail/asn-flow = 无既有 spec 载体，**新增断言/spec**（Add 义务）。
- **NOP_SYS_SEQUENCE / seed CSV 不受影响**（roadmap 当前基线节裁决：DB 层零改动，`zz-sequence-advance.sql` 序列推进机制不变）。

## Goals

- id 冲击面全量修复：附录 F 三形态（234 简单 + 117 复合判定 + eqFilter id/FK 族）+ **字面量数字 id 传参形态（130 处/44 文件）**；数值族 523+ 处零误改（修复后 diff 抽查证明）。
- flux 模式 E2E 分目录 → 全量回归：主门 = 08-11 基线六目录命令（dashboards/negative/crud/reports/orchestration/business-actions）+ examples/pages 两目录补充跑（07-25 口径残留目录）；已知失败口径以 08-11 白名单（13 失败/7 类）为起点逐条裁决。
- 4 处 page.yaml 修复页面的运行时行为复核（查询非静默降级：数据非空或行为生效断言；3 处新增断言/spec 载体）。
- 新失败分流：id 迁移引发 = 本计划 Fix；非 id 预存缺陷 = 诊断 + bug 登记（缺陷不降级）。
- known-good-baselines E2E 基线条目 + e2e-runbook enforcement/计数段漂移同步 + 日志。

## Non-Goals

- 不改生产代码（spec/`_helper.ts` 测试层 only；发现产品缺陷 → `nop-debugging` 诊断 + bug 登记 + 独立修复载体，不在本计划静默改产品行为）。
- 不跑 visual/ 像素快照套件（历史基线口径不含 visual/，plan `2026-07-17-2010-2` 所有权）。
- 不修 08-11 白名单预存失败本体（13 失败/7 类，含 `master-data.write.amis`——enforcement OFF 时代之外的既有登记，非 id 相关；Phase 3 仅逐条复核维持/转绿/登记，白名单项的修复义务归其登记的 successor）。
- 不跑 compliance checker / 文档收尾 / roadmap done（批内序 3）。
- 不重录 JUnit 快照（批内序 1 已完成全量复查）。

## Task Route

- Type: `implementation-only change`（测试层修复 + 全量验证）
- Owner Docs: `docs/testing/e2e-runbook.md`（E2E 编写规范强制节 + 渲染模式 flux-only 节 + 运行命令表）、`docs/audits/2026-08-21-1045-id-migration-m0-cross-domain-coupling-appendix.md` 附录 F（修复输入权威清单）、`docs/testing/known-good-baselines.md`（E2E 基线口径）
- Skill Selection Basis: `nop-debugging`（E2E 失败先诊断后修，测试失败属其触发词；已知失败模式清单优先对照）；`nop-testing` 面向 JUnit/IGraphQLEngine 快照层，不适用 Playwright 层——Skill: none 于 spec 修复 phase（先例模式 + 附录 F 清单驱动）。

## Infrastructure And Config Prereqs

- runner jar：`app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar`（批内序 1 `mvn clean install -DskipTests` 产物——**硬前置**）。
- Playwright：`npx playwright test`（已安装基线）；服务器 `./_tmp-server.sh restart`（fresh-DB + 8011 端口），其 JVM args 与 `playwright.config.ts` webServer.command 同步核对后再启动。
- 全量运行时长参照 ~1.0h（2026-07-25 基线，`--workers=1`）。

## Execution Plan

### Phase 1 - 冲击面复测 + spec 修复

Status: planned
Targets: `tests/e2e/**/*.ts`（附录 F 清单域）
Skill: none（清单驱动修复；先例：`String(...)` 兼容形态）

- Item Types: `Fix | Proof`
- Prereqs: 批内序 1 完成（非硬编译前置，但基线口径统一）

- [ ] Proof: 附录 F 复核命令重跑（`Number(` 全量 `*.ts` 口径 / id 族简单形态正则 / `eqFilter('` id+FK 族 / **字面量数字 id 正则 `\b[a-zA-Z]*[iI]d:\s*\d+` + const 赋值形态 `\b[a-zA-Z_][a-zA-Z0-9_]*[iI]d\s*=\s*\d+`**（`DUMMY_ID = 999999` 类）），08-21 vs 执行期计数对照落盘（口径统一以附录 F 复核命令为准）。
  - Skill: none
- [ ] Fix: id 族 `Number(...)` 字符串化/移除——简单形态 234 处全改 + 复合形态 117 处逐个判定（id 族改、数值族保持，判定记录留档）；断言侧改字符串比较，传参侧移除 Number 包装。
  - Skill: none
- [ ] Fix: 字面量数字 id 传参形态 130 处/44 文件——真 id 字段必改字符串字面量（`orgId: 2`→`orgId: '2'`、`DUMMY_ID = 999999`→`'999999'`）；数值语义字段名撞车（非 id 而形如 `*Id`）逐条判定留档。
  - Skill: none
- [ ] Fix: `eqFilter('id'` 30-36 处 + FK eqFilter 族值来源调整（行内实体 id 已 String，helper 构造与比较形态对齐）。
  - Skill: none
- [ ] Proof: 既有 `String(...[iI]d...)` 27 处核对零行为变化 + 数值族零误改复核（修复 diff 按附录 F 数值族清单抽查 ≥20 处）。
  - Skill: none

Exit Criteria:

- [ ] id 族 `Number(` 残留 = 0（附录 F 正则复测）+ 字面量数字 id 残留 = 0（真 id 字段，含 const 赋值形态复测；判定留档）+ eqFilter id/FK 族清零 + 修复清单 per-file 落盘
- [ ] 数值族零误改抽查记录落盘（≥20 处）

### Phase 2 - flux 全量 E2E 运行 + 修复循环

Status: planned
Targets: `tests/e2e/`（08-11 基线六目录主门 + examples/pages 补充）
Skill: `nop-debugging`

- Item Types: `Fix | Proof | Add`
- Prereqs: Phase 1 + 批内序 1 runner jar

- [ ] Proof: `_tmp-server.sh` JVM args 与 webServer.command 同步核对（含 enforcement 栈与 SoD config-gate）+ fresh-DB 启动 + dashboards 抽样冒烟（finance/master-data value spec 先行）。
  - Skill: none
- [ ] Fix: 分目录回归修复循环——business-actions（最大面）→ negative → crud → orchestration → dashboards → reports；失败按 `nop-debugging` 诊断分流（GraphQL String 类型拒绝 / 断言形态 / 清理原语残留三类预期主体；negative 预期叠加 08-11 白名单预存失败）。
  - Skill: `nop-debugging`
- [ ] Proof: 主门全量 = 08-11 基线命令口径 `BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 E2E_ENGINE=flux npx playwright test tests/e2e/{dashboards,negative,crud,reports,orchestration,business-actions}/ --workers=1` passed/failed/skipped 权威计数落盘（对照 08-11 基线 491/13/8 逐目录解释差量）；补充跑 examples/pages 两目录（07-25 口径残留目录，07-25 曾全绿）单独计数与裁决。
  - Skill: none
- [ ] Add: 4 处 page.yaml 修复页面运行时行为复核载体——inbox 复用既有 `business-actions/notify-inbox.action.spec.ts`（markRead 后 countUnread 递减）；schedule-gantt（machineId 过滤返回非空行）/ edi-detail（doc 加载非空）/ asn-flow（asn get 非空）**新增断言/spec**（按 e2e-runbook E2E 编写规范，engine-agnostic PageObject 形态）——adaptor 静默降级消除实证。
  - Skill: none

Exit Criteria:

- [ ] 主门六目录全量 0 failed（除 08-11 白名单口径，见 Phase 3 裁决）+ 逐目录计数落盘；examples/pages 补充跑 0 failed 或逐条裁决
- [ ] 4 处页面行为复核证据落盘（任一失败 → 按 Fix 就地修复或登记缺陷裁决，不得静默）

### Phase 3 - 失败裁决 + 基线登记

Status: planned
Targets: `docs/testing/known-good-baselines.md`、`docs/testing/e2e-runbook.md`（enforcement/计数段漂移同步）、`docs/logs/2026/08-23.md`（或执行当日）
Skill: none

- Item Types: `Decision | Add`
- Prereqs: Phase 2

- [ ] Decision: 残留失败终局裁决——以 08-11 白名单 13 失败/7 类为起点逐条复核（维持 known-impact/Non-Goal/pre-existing 分类 / id 修复后转绿 / 新增 bug 登记），叠加本计划新暴露失败的 id/非 id 分流；裁决记录落盘本计划。白名单中 enforcement-induced UI 渲染回归 3 项维持 cross-repo flux successor 登记。
  - Skill: none
- [ ] Add: known-good-baselines 新增 E2E 基线条目（命令/计数/known failures/git state）+ e2e-runbook 漂移段同步（enforcement 段与 `%test` 实况对齐 + 套件计数刷新）+ 日志条目（含验证状态段）。
  - Skill: none

Exit Criteria:

- [ ] 残留失败裁决记录落盘（对照 08-11 白名单逐条 + 新失败分流，零未裁决项）
- [ ] E2E 基线条目 + runbook 同步 + 日志落盘

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（ses_fd4cbb3ceffefhatq9TlJ0USrx，fresh session，治理+技术双视角）——0 BLOCKER / 3 MAJOR / 3 MINOR，根因 = 基线锚定停在 07-25（enforcement OFF 时代）。**MAJOR-1**：Phase 2 全量遗漏 `tests/e2e/negative/` → 已修订：主门命令改 08-11 基线六目录口径（含 negative）+ examples/pages 补充跑。**MAJOR-2**：基线口径不完整 + `%test` enforcement 三开关 ON 实况未登记 → 已修订：Baseline 增补 08-11 基线行（491/13/8 + 13 失败白名单 7 类）+ enforcement 实况段 + Phase 3 已知失败口径改为 08-11 白名单逐条裁决 + runbook enforcement 段漂移同步义务。**MAJOR-3**：附录 F 清单外形态——字面量数字 id 传参 130 处/44 文件（negative 19 处）未入修复面 → 已修订：Baseline 补登 + Phase 1 新增 Fix 项 + 清零 Exit Criteria。**MINOR-1**：`Number(` 双口径并列混淆 → 已修订（口径注明：附录 F 命令 `*.ts` 为准）。**MINOR-2**：4 页面复核载体未声明（inbox 既有 spec；gantt/edi/asn 需新增）→ 已修订（Item Types 增 Add + 载体注明）。**MINOR-3**：runbook enforcement 段漂移 → 已并入 Closure Gates 同步义务。live 复核计数（286 spec/767/102/30/27/目录分布）全部证实。
- Independent draft review iteration 2: `acceptable as-is`（ses_fd4bf741effeU3eYgDlOMB6Ny5，fresh session）——3 MAJOR + 3 MINOR 全部核验解决且 live 证据链零偏差（08-11 基线命令逐字符一致 / 白名单 7 类计数吻合 / %test enforcement 四开关实测一致 / 130/44 目录分布逐目录吻合 / runbook :127 漂移实证）；无 BLOCKER/MAJOR 新发现；2 新 MINOR（「八目录」→「七目录」数字转述误差 + const 赋值形态复测 grep 缺失）已当场修订。**共识达成，Plan Status → active。**

## Closure Gates

> 验证门 = 本计划交付物（flux 全量 E2E）；Phase 2 已覆盖全量命令，此处汇总证据要求。JVM 层全量构建/测试由批内序 1 兑现，本计划引用其证据不重复。

- [ ] 范围内行为完成（id 族修复清零三形态 + 4 页面行为复核含 3 新增载体）
- [ ] 相关文档对齐（known-good-baselines + e2e-runbook enforcement/计数段漂移同步 + 日志）
- [ ] 已运行验证：主门六目录全量 E2E 命令（08-11 基线口径，--workers=1）权威计数落盘 + examples/pages 补充跑
- [ ] 无范围内项目降级为 deferred/follow-up（08-11 白名单预存失败为既有登记口径维持，非本计划范围降级）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 08-11 白名单预存失败（13 失败/7 类）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 08-11 全 enforcement 栈基线登记的白名单失败（role-login 2 / master-data.write.amis / crud 表单对话框 3（cross-repo flux successor）/ negative qa riskName+FK 3 / negative mnt PLANNED 2 / negative sal loginAsRole race 2），均预存且与本 mission 无关；Phase 3 逐条复核维持/转绿/bug 登记。
- Successor Required: `no`（08-11 基线行登记载体；enforcement-induced UI 渲染回归 3 项归 cross-repo flux 调查 successor，08-11 行已登记）

### visual/ 像素快照套件

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 历史基线口径从不包含 visual/（plan `2026-07-17-2010-2` 所有权）；本计划对齐既有口径。
- Successor Required: `no`（归 plan 2026-07-17-2010-2 所有权链）

### 非 id 预存产品缺陷（Phase 2/3 发现时）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 与 id 迁移无关的预存缺陷按不可降级规则 bug 登记（docs/bugs/）+ 触发条件，不在本计划静默修复（生产代码修复须独立载体）。
- Successor Required: `yes`（触发条件：bug 登记后由 backlog 择期；若为 P0/P1 级阻断 E2E 基线，立即升级人工）

## Closure

Status Note: （待执行后填写）

Closure Audit Evidence:

- Auditor / Agent: （待独立结束审计填写）
- Evidence: （待填写）

Follow-up:

- （无范围内跟进项；已确认缺陷不得出现在此处。）
