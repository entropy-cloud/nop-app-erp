# 2026-09-01-0527-2-report-flux-browser-assertion-drift-fix 报表 flux 重写浏览器断言漂移修复

> Plan Status: active
> Last Reviewed: 2026-09-01
> Mission: comprehensive-test-data-and-visual-coverage
> Work Item: M0.3 Closure Follow-up——报表正文解包产品缺陷修复 + reports spec 谓词 /r/ 化 + inventory 像素基线确定性重录 + global-setup 期间守卫修复
> Source: `docs/bugs/2026-09-01-0400-report-flux-rewrite-browser-assertion-drift.md`（M0.3 plan `2026-09-01-0301-2` 执行期发现的 4 项修复方向，登记为 Closure Follow-up「successor plan」；触发条件「M2.x DRAFT_PLANS 前或下一视觉回归维护批」——本批 DRAFT_PLANS 即视觉回归维护批启动点，且缺陷为**产品可见**（浏览器报表正文不显示），不修复则 M2.3 报表像素扩面基线不可用）
> Related: `2026-09-01-0301-2-m03-visual-methodology-codification.md`（视觉方法论段 = 本计划重录与 mask 的规范依据）、`docs/bugs/2026-09-01-0400`（三层根因实证）、`2026-08-29-1913-2-page-graphql-to-rest-migration-phase2.md`（缺陷引入批——报表页 flux 原生重写）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 2026-09-01）：

- **应用侧产品缺陷（全量在位）**：`module-*/erp-*-web/src/main/resources/_vfs/erp/*/pages/report/*.page.yaml` 实测 **25 个**文件全部为缺陷形态 `html: "${reportHtmlData ?? ''}"`（`rg` 复核：25 命中 / 修复形态 0 命中）——bug 笔记记「全 24 报表」，draft 时 `find` 实测 25（含 `fin/period-close-report` 等；执行时逐文件清点以实仓为准并勘误 bug 笔记计数）。data-source 结果对象未解包 `.data`，html 渲染器输出空 → **浏览器报表正文不显示**（`/r/` 响应 200 + 完整 HTML 实证，`[data-slot="html"]` innerHTML 长度 0）。正确形态 = `${reportHtmlData?.data ?? ''}`（同 `9a4427d84` 确立的 `previewData?.x` 可选访问范式）。
- **测试侧契约漂移**：`tests/e2e/visual/_helper.ts:265` `assertReportRendered` 等待谓词仍按旧 AMIS 契约 `/graphql` + body 含 `renderHtml`（`:270`）→ flux 原生重写后取数改为 REST `/r/ErpXxxReport__renderHtml`，24 个 `reports.visual.spec.ts` 测试 + 6 个 `reports.snapshot.spec.ts` 测试恒超时（TimeoutError 30s）；`reports.snapshot.spec.ts:34-38` 另有同型内联谓词残留 + `:58` 渲染报表按钮点击编排。dashboards 侧同型漂移已由 commit `c677f76cc` 修复（`_helper.ts:164-171` `/r/` 谓词范本在位），reports 侧漏改。
- **看板像素基线跨月漂移**：`dashboards.snapshot.spec.ts` inventory 1 测试 08-31 录基线 → 09-01 跑 `13801 pixels (ratio 0.02) different` > 容差 0.01——inventory spec 无确定性日期/期间参数（finance 传 `periodId=1`、assets 传 `periodId='2026-07'`，inventory 无），滞销/批次效期预警行集随 today 相对窗口漂移。
- **global-setup 期间守卫失效**：`tests/e2e/global-setup.ts:40` `ensureCurrentMonthOpenPeriod` 期间查询使用不支持运算符 `le`（受支持集 = `eq`/`in`/`dateBetween`/`dateTimeBetween`）→ 运行月 OPEN 期间预置静默失败（警告日志 `[global-setup] period find failed` 实证），runbook「日期漂移防护」守卫空转。
- **规范就绪**：M0.3 视觉方法论段已发布（`e2e-runbook.md`「视觉方法论（M0.3 固化，2026-09-01）」九要素：mask 标准 / 双面重录协议 / `--update-snapshots` 仅 CI 重录 / 快照重录合规声明载体 = plan Draft Review Record / `toHaveScreenshot` 命名锁定 / helper 扩展规则）；M0.1「视觉断言扩面边界」段已发布。
- **验证基线**：`known-good-baselines.md` 2026-08-31 plan-1426-2 行（`mvn test -pl app-erp-all` 69/0/0/1；compliance R2c=1542）；本计划为前端页面 yaml + E2E spec 变更，不触 Java 生产代码。
- **差距**：bug 笔记 4 项修复方向全部未落地；31 个视觉测试（24 visual + 6 reports.snapshot + 1 inventory）持续全红；产品缺陷（报表正文不显示）持续在位。

## Goals

- 产品缺陷修复：全量报表 page.yaml（实仓清点为准）html 模板解包 `.data`，报表正文恢复浏览器显示。
- 测试侧契约对齐：`assertReportRendered` 及 reports 双 spec 等待谓词 `/r/` 化（镜像 c677f76cc dashboards 侧修法），24 + 6 测试回绿。
- 看板像素基线确定性：inventory spec 日期相对内容确定性处理 + 像素基线按双面协议重录，二连跑稳定。
- global-setup 期间守卫修复：`ensureCurrentMonthOpenPeriod` 改用受支持运算符，警告消失、期间预置生效。
- 解锁 M2.3 报表像素扩面基线（bug 笔记明示：修复前不可用）。

## Non-Goals

- **不做 M2.x 扩面本身**——报表/看板像素断言扩面归 M2.3/M2.4；本计划仅修复既有 31 个测试回绿 + inventory 基线重录。
- **不触 nop-chaos-flux 仓库代码**——bug 笔记三路径分诊结论：根因在本仓库应用层页面 schema 与测试侧契约漂移（非路径 1/2/3）；外部仓库修改须双独立子 agent 批准（`ai-autonomy-policy.md`），本计划无此需要。
- **不改既有 `assertSnapshot` 像素统一封装**——M0.3 方法论 helper 规则沿用；本计划仅修 `assertReportRendered`（DOM 层 helper，授权见 Phase 2 Decision）。
- **不新增报表下载产物字节级 diff**——roadmap/0204-1 Non-Goal 沿用。
- **不引入跨浏览器矩阵**——2010-2 Non-Goal 沿用。
- **不修 2 处 hr/drp 域预存回归**——与视觉层无关，successor 归属已在 `known-good-baselines.md` 登记。

## Task Route

- Type: `bug investigation` → `implementation-only change`（三层根因已由 M0.3 执行期 probe 实证并登记 bug 笔记；本计划为 Fix 承载）
- Owner Docs: `docs/bugs/2026-09-01-0400-report-flux-rewrite-browser-assertion-drift.md`（修复状态回写）+ `docs/testing/e2e-runbook.md`（视觉方法论段——重录合规声明载体；如谓词修法需注记则原位补充）
- Skill Selection Basis: `Skill: nop-testing`（主——Playwright E2E spec/谓词/基线重录）+ `nop-frontend-dev`（辅——page.yaml data-source/html 模板修改遵循 flux 页面 DSL 约束）。执行期按 AGENTS.md 强制技能加载规则两技能均已加载核对。

## Infrastructure And Config Prereqs

- Playwright 运行前置（既有基线）：app 在 8011 端口运行（`java -Dquarkus.profile=dev -jar app-erp-all/target/...-runner.jar` 或 `./scripts/start-app.sh`）+ `BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test ...`（flux 渲染模式缺省，runbook「渲染模式」节）。
- 像素基线重录在 fresh-DB 装载后进行（`scripts/start-app.sh` fresh-DB 重置入口，97 CSV + 1 SQL 装载 0 冲突基线）。
- 无新增端口/环境变量/密钥依赖。

## Execution Plan

### Phase 1 - 应用侧报表正文解包修复

Status: planned
Targets: `module-*/erp-*-web/src/main/resources/_vfs/erp/*/pages/report/*.page.yaml`（25 个，执行时以实仓 `find` 清点为准）
Skill: `nop-frontend-dev`

- Item Types: `Fix | Decision`（Fix 1 + Decision 1，共 2 项）
- Prereqs: 无

- [ ] Fix: 全量报表 page.yaml `html: "${reportHtmlData ?? ''}"` → `html: "${reportHtmlData?.data ?? ''}"`（逐文件修改 + `rg` 复核缺陷形态 0 残留 + 修复形态 25/25 全覆盖；与 bug 笔记「24」计数的差异在执行注记中勘误回写 bug 笔记）
      - Skill: `nop-frontend-dev`
- [ ] Decision: 解包方式裁决——`${reportHtmlData?.data ?? ''}` 模板内解包 vs data-source adaptor 拍平（bug 笔记给出两路）；记录选择理由、替代方案与残留风险（倾向模板内解包：与 `9a4427d84` 既有 `previewData?.x` 范式一致、25 文件单行 diff 最小化、不引入 adaptor 函数维护面）；若执行期发现部分页面结构不同（如多 data-source 页），逐页登记处置
      - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] 抽样 ≥ 3 个报表页（跨域：fin/md/inv 至少各 1）浏览器实测 `[data-slot="html"]` 内报表正文非空（临时 probe spec 或既有 visual spec 单条跑通实证；probe 用后即删不入库）
- [ ] `rg 'reportHtmlData \?\? ..'` 全仓 0 命中 + bug 笔记计数勘误完成

### Phase 2 - 测试侧等待谓词 /r/ 化

Status: planned
Targets: `tests/e2e/visual/_helper.ts`、`tests/e2e/visual/reports.visual.spec.ts`、`tests/e2e/visual/reports.snapshot.spec.ts`
Skill: `nop-testing`

- Item Types: `Fix | Decision | Add`（Fix 2 + Decision 1 + Add 1，共 4 项）
- Prereqs: Phase 1（谓词修复后断言的报表正文依赖解包修复生效）

- [ ] Decision: `assertReportRendered` 既有 DOM 层 helper 修改授权登记——M0.3 方法论「M2.x 只能新增像素层 helper 子集、不可改既有函数」规则约束的是 **M2.x 扩面批**；本计划是 bug 笔记修复方向 2 明示的「独立计划承接」（M0.3 Task Route 当时拒绝在无计划状态下动 spec），故本计划修改 `assertReportRendered` 为授权行为；`assertSnapshot`（像素统一封装）不触碰的边界重申；记录于本项执行注记
      - Skill: `nop-testing`
- [ ] Fix: `_helper.ts` `assertReportRendered` 等待谓词 `/graphql` + body 含 `renderHtml` → `/r/` + URL 含 `ErpXxxReport__renderHtml`（镜像 `_helper.ts:164-171` dashboards 侧 c677f76cc 修法；`page.waitForResponse` 须在 navigation 前注册的既有纪律保持）
      - Skill: `nop-testing`
- [ ] Fix: `reports.visual.spec.ts` + `reports.snapshot.spec.ts` 内独立谓词/编排残留 `/r/` 化——实证在位：`reports.snapshot.spec.ts:34-38` 内联 `/graphql`+renderHtml 谓词 + `:58` 渲染报表按钮点击编排；`reports.visual.spec.ts` 逐处核对（`assertReportRendered` 承载部分随 helper 修复自动生效）；`assertReportRendered` 注释块所述编排与 flux 页面「加载即自动取数」行为对齐（旧范式「渲染报表按钮点击后取数」的编排假设逐处核对修正）
      - Skill: `nop-testing`
- [ ] Add: `docs/testing/e2e-runbook.md` §7（M2.x helper 扩展规则）范围澄清注记——原位补充一句：「既有 DOM 层 helper『签名与语义冻结』限定于 M2.x 扩面批；bug 修复批经独立计划授权可修改（见 plan 2026-09-01-0527-2）」——消除本计划 Phase 2 Decision 授权与 runbook §7 冻结表述的 owner-doc 矛盾（本 helper 修改为已确定事实，非条件项）
      - Skill: `none`

Exit Criteria:

- [ ] `npx playwright test tests/e2e/visual/reports.visual.spec.ts tests/e2e/visual/reports.snapshot.spec.ts` 30/30 全绿（24 + 6，零 skip 零降级）
- [ ] `_helper.ts` diff 中 `assertSnapshot` 函数体零变更
- [ ] runbook §7 范围澄清注记落地（与 Phase 2 Decision 授权无矛盾）

### Phase 3 - 看板基线确定性、global-setup 守卫与重录合规

Status: planned
Targets: `tests/e2e/global-setup.ts`、`tests/e2e/visual/dashboards.snapshot.spec.ts`、`tests/e2e/visual/dashboards.snapshot.spec.ts-snapshots/`
Skill: `nop-testing`

- Item Types: `Fix | Add | Decision`（Fix 1 + Add 1 + Decision 1，共 3 项）
- Prereqs: 无硬前置（与 Phase 1/2 无依赖冲突，但重录须在 global-setup 守卫生效后进行——本阶段内部排序：守卫修复 → 确定性处理 → 重录）

- [ ] Fix: `global-setup.ts` `ensureCurrentMonthOpenPeriod` 期间查询 `le` → 受支持运算符（`dateBetween` 或 `eq`，按查询字段类型裁决）；验证警告日志零出现 + 运行月 OPEN 期间预置实际生效（DB 抽样或 API 抽查）
      - Skill: `nop-testing`
- [ ] Decision: inventory spec 日期相对内容确定性处理方案——(a) 与 finance/assets 范式对齐传确定性 `periodId` 参数 vs (b) 按 M0.3 方法论 §2 mask 标准对预警行集区域 mask vs (c) seed 侧固定窗口数据；记录选择、替代方案与残留风险（倾向 (a)：根除漂移源且与既有 2 个传参 spec 范式一致；mask 是兜底非首选——mask 掩盖行集漂移信号）
      - Skill: `nop-testing`
- [ ] Add: **独立 plan-audit（视觉 mask 调整 + 像素基线重录保护区域）**——按 M0.3 双面重录协议执行：(a) 确定性处理落地后 inventory spec 像素基线重录（`--update-snapshots`）；(b) 双面对账 = `git diff --stat tests/e2e/visual/dashboards.snapshot.spec.ts-snapshots/` 与 DOM 断言基线同步核查（禁单面）；(c) 独立子代理（新会话）复核 mask 区域合理性与确定性处理方案；(d) **快照重录合规声明**按 runbook §5 载体裁决记录于本计划 `## Draft Review Record`（触发源 / 双面完成 / mask 理由 / `git diff --stat` 对账），本项执行注记留副本；(e) 批准记录落盘本计划本项执行注记
      - Skill: `nop-testing`

Exit Criteria:

- [ ] `npx playwright test tests/e2e/visual/dashboards.snapshot.spec.ts` 10/10 全绿 + 二连跑稳定（同日两次 exit 0；跨月稳定性由确定性处理方案根除漂移源承载，方法裁决记录残留风险。注：`reports.snapshot.spec.ts` 6 + `dashboards.snapshot.spec.ts` 10 = snapshot 层合计 16，与 runbook「共 2 spec / 16 测试」口径一致）
- [ ] global-setup 运行日志零 `period find failed` 警告
- [ ] 独立 plan-audit 批准记录在位 + 双面重录对账完成

## Draft Review Record

- Independent draft review iteration 1: needs revision (独立子代理 ses_fa6438b77ffeZPTVt03M9pAsYC) because 1 Major + 3 Minor——Major：dashboards.snapshot 测试计数误写 16/16（实仓 10，16 为双 snapshot spec 合计，runbook:944 口径）；Minor m1：「如有独立谓词」条件式措辞（实仓已确证 reports.snapshot.spec.ts:34-38 内联谓词在位）+ runbook §7 冻结表述与本计划 helper 修改授权的 owner-doc 矛盾未显式处理；Minor m2：快照重录合规声明载体未对齐 runbook §5 裁决（须落本计划 Draft Review Record）；Minor m3：`_helper.ts:164-167` 行号截断谓词体。全部已修订：10/10 + 合计 16 注记；谓词残留改确证事实 + 新增 Phase 2 Add（runbook §7 范围澄清注记）+ 对应 Exit Criterion；合规声明载体改 `## Draft Review Record`；行号改 `:164-171`。
- Independent draft review iteration 2: accept (独立子代理 ses_fa63be8e2ffeIQITft4VBHzXLK) after 上述 1 Major + 3 Minor 全部确认修复（计数 10/6/24 实仓复核一致、4 项 bug 修复方向全覆盖、保护区域 plan-audit checkbox 与 assertSnapshot 零变更边界完好）；残留 4 Minor 均为非阻塞文本记账（Item Types 头计数 3→4、Fix 项行号 `:164-171`、`:57`→`:58`、本审查记录补填），已随转 active 前内联修复。

## Closure Gates

> 前端页面 yaml + E2E spec 变更，无 Java 生产代码变更；全量 `mvn clean install -DskipTests` 运行一次确认零影响（page.yaml 为 `_vfs` 资源，预期 BUILD SUCCESS 无编译外溢）；compliance checker 复跑（page.yaml 属生产资源文件，如触发计数漂移按 known-failure-mode 规则开基线裁决，预期零漂移）。视觉/UX 主结果面：验证门控自定义 = Phase 2/3 的 Playwright 全绿 + 二连跑稳定（替代通用单测门控，理由：本计划交付面即浏览器行为）。

- [ ] 范围内行为完成（三层修复落地：产品缺陷 + 测试契约 + 基线确定性）
- [ ] 相关文档对齐（bug 笔记修复状态回写 + 计数勘误；runbook 方法论段如有谓词修法注记原位补充；与 M0.3 方法论/重录协议无矛盾）
- [ ] 已运行验证（reports 30/30 + dashboards.snapshot 10/10 全绿 + 二连跑稳定 + `mvn clean install -DskipTests` BUILD SUCCESS + compliance checker 零漂移——漂移则基线裁决）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 独立 plan-audit（视觉 mask + 基线重录保护区域）已完成并落盘批准记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

（无——bug 笔记 4 项修复方向全部纳入 Phase 1/2/3；「M2.4 扩面时按方法论 §2 处理日期相对内容」属 M2.4 范畴不纳入本计划，已在 Non-Goals 登记）

## Closure

Status Note: (closure 时填写)

Closure Audit Evidence:

- Auditor / Agent: (closure 时填写)
- Evidence: (closure 时填写)

Follow-up:

- (仅非阻塞跟进项目；M2.3/M2.4 扩面为 roadmap 后续工作项非本计划 Follow-up)
