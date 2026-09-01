# 2026-09-01-0527-2-report-flux-browser-assertion-drift-fix 报表 flux 重写浏览器断言漂移修复

> Plan Status: completed
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

Status: completed
Targets: `module-*/erp-*-web/src/main/resources/_vfs/erp/*/pages/report/*.page.yaml`（25 个，执行时以实仓 `find` 清点为准）
Skill: `nop-frontend-dev`

- Item Types: `Fix | Decision`（Fix 1 + Decision 1，共 2 项）
- Prereqs: 无

- [x] Fix: 全量报表 page.yaml `html: "${reportHtmlData ?? ''}"` → `html: "${reportHtmlData?.data ?? ''}"`（逐文件修改 + `rg` 复核缺陷形态 0 残留 + 修复形态 25/25 全覆盖；与 bug 笔记「24」计数的差异在执行注记中勘误回写 bug 笔记）
      - Skill: `nop-frontend-dev`
      - **执行注记（2026-09-01）**：实仓清点 25 文件确认（`rg -l` 25 命中 / 修复形态 0 命中）。首轮按计划字面形态改为 `${reportHtmlData?.data ?? ''}` 后**浏览器 probe 仍空渲染**（`[data-slot="html"]` 保持 `data-state="empty"`），实跑取证推翻解包假设：`/r/` 响应 envelope `{"data":"<html>","status":0}` 经前端 ajax 层 `unwrapApiPayload` 已解包一次，flux 作用域 `reportHtmlData` 即 html 字符串；空渲染真因 = flux `HtmlSchema` 契约的内容 prop 名是 **`content`**，`html:` key 被编译期归类为未知 prop 静默忽略（nop-chaos-flux `flux-renderers-content/src/html.tsx` 只读 `slotProps.content`，schema.d.ts 契约同）。最终修复形态（25/25 全覆盖，`rg` 复核：缺陷 `html:` key 0 残留）：html 节点改为 `content: "${reportHtmlData ?? ''}"` + `sanitize: false`（报表 HTML 为第一方 xpt 引擎产物、含 `<style>` 布局 CSS；DOMPurify 缺省 `FORBID_TAGS:['style']` 会剥 style 致表格裸排，故按渲染器信任逃逸口声明 `sanitize: false`）。`git diff --stat` = 25 files / +50 −26（含 ar-ap-aging `${NOW()}` 默认值移除行，见附带修复）。计数勘误（24→25）+ 根因勘误已回写 bug 笔记「修复记录」节。
- [x] Decision: 解包方式裁决——`${reportHtmlData?.data ?? ''}` 模板内解包 vs data-source adaptor 拍平（bug 笔记给出两路）；记录选择理由、替代方案与残留风险（倾向模板内解包：与 `9a4427d84` 既有 `previewData?.x` 范式一致、25 文件单行 diff 最小化、不引入 adaptor 函数维护面）；若执行期发现部分页面结构不同（如多 data-source 页），逐页登记处置
      - Skill: `nop-frontend-dev`
      - **裁决记录（2026-09-01，执行期证据修订）**：25 页面结构逐文件核对为同构（单 `reportHtmlData` data-source + 单 html 节点），无需逐页特判。**选择**：模板内绑定 `content: "${reportHtmlData ?? ''}"`（ajax 层已解包 envelope，作用域值即字符串，无需 `.data`）。**替代方案**：(a) 计划原拟 `.data` 模板内解包——rejected，实跑证伪（envelope 已被 ajax 层解包，字符串上取 `.data` = undefined → 仍空渲染）；(b) data-source adaptor 拍平——rejected，flux data-source 无 adaptor 概念等价物，且维护面更大。**附带修复（ar-ap-aging 页登记）**：其 input-date 默认 `value: "${NOW()}"` 在 flux 模板无 NOW() 函数 → 节点渲染错误、日期选择器不可用；移除默认值（留空 = 后端全量范围口径，与 value-spec 层零日期行为一致）。**残留风险**：`sanitize: false` 为信任逃逸口——报表 HTML 全部来自第一方 xpt 模板 + 引擎渲染，若未来允许用户输入直通报表 HTML 需重新评估；DOMPurify 对报表表格样式（内联 `<style>`）的剥除由该开关规避，像素基线依赖此前提。

Exit Criteria:

- [x] 抽样 ≥ 3 个报表页（跨域：fin/md/inv 至少各 1）浏览器实测 `[data-slot="html"]` 内报表正文非空（临时 probe spec 或既有 visual spec 单条跑通实证；probe 用后即删不入库）
- [x] `rg 'reportHtmlData \?\? ..'` 全仓 0 命中 + bug 笔记计数勘误完成
      - **执行注记**：probe（fin/md/inv 三页，临时 spec 用后即删）全绿：innerHTML 长度 2123 / 4597 / 2645，报表专属 token（主营业务收入 / CUST-001 / 库存追溯链可视化报表）全部在位。缺陷形态全仓扫描：`*.page.yaml` 代码面 0 残留（`rg 'reportHtmlData \?\? '` 剩余命中均为 docs/ 下对旧缺陷形态的**历史记录文本引用**，非代码，按勘误记录保留）；bug 笔记 24→25 勘误 + 修复状态回写完成。

### Phase 2 - 测试侧等待谓词 /r/ 化

Status: completed
Targets: `tests/e2e/visual/_helper.ts`、`tests/e2e/visual/reports.visual.spec.ts`、`tests/e2e/visual/reports.snapshot.spec.ts`
Skill: `nop-testing`

- Item Types: `Fix | Decision | Add`（Fix 2 + Decision 1 + Add 1，共 4 项）
- Prereqs: Phase 1（谓词修复后断言的报表正文依赖解包修复生效）

- [x] Decision: `assertReportRendered` 既有 DOM 层 helper 修改授权登记——M0.3 方法论「M2.x 只能新增像素层 helper 子集、不可改既有函数」规则约束的是 **M2.x 扩面批**；本计划是 bug 笔记修复方向 2 明示的「独立计划承接」（M0.3 Task Route 当时拒绝在无计划状态下动 spec），故本计划修改 `assertReportRendered` 为授权行为；`assertSnapshot`（像素统一封装）不触碰的边界重申；记录于本项执行注记
      - Skill: `nop-testing`
      - **授权登记（2026-09-01）**：本计划修改 `assertReportRendered`（DOM 层 helper）为 bug 修复批经独立计划（本计划，独立草案审查 iteration 2 accept）授权的行为；runbook §7 已原位补范围澄清注记消除矛盾。`git diff` 复核：`assertSnapshot` 函数体零变更（唯一其他改动 = 同文件私有函数 `pickFluxDate` 的**导出**，为 reports.snapshot spec 复用 flux 日历拾取所必需，属只增不改边界，见 Phase 3 注记）。
- [x] Fix: `_helper.ts` `assertReportRendered` 等待谓词 `/graphql` + body 含 `renderHtml` → `/r/` + URL 含 `ErpXxxReport__renderHtml`（镜像 `_helper.ts:164-171` dashboards 侧 c677f76cc 修法；`page.waitForResponse` 须在 navigation 前注册的既有纪律保持）
      - Skill: `nop-testing`
      - **执行注记**：谓词改为 `/r/` + URL 含 `renderHtml`（decodeURIComponent 双保险，镜像 dashboards 范式）；编排对齐 flux「加载即自动取数」：initial response 先等 → 填充触发去抖 reload（listener 先于填充注册）；**同值填充不触发 reload** 的 flux 语义实跑发现（fill 值 == 页面默认值时无重新派发，恒 30s 超时），helper 以 `inputValue()` 预比对、仅在实际变更时等待 reload（未变更时初始加载已按该值渲染，token 断言语义不变）。旧「渲染报表按钮点击」编排删除（flux 下点击不可靠且被 dedupe，镜像 dashboards 注释）。
- [x] Fix: `reports.visual.spec.ts` + `reports.snapshot.spec.ts` 内独立谓词/编排残留 `/r/ 化——实证在位：`reports.snapshot.spec.ts:34-38` 内联 `/graphql`+renderHtml 谓词 + `:58` 渲染报表按钮点击编排；`reports.visual.spec.ts` 逐处核对（`assertReportRendered` 承载部分随 helper 修复自动生效）；`assertReportRendered` 注释块所述编排与 flux 页面「加载即自动取数」行为对齐（旧范式「渲染报表按钮点击后取数」的编排假设逐处核对修正）
      - Skill: `nop-testing`
      - **执行注记**：reports.snapshot `driveReportAndSnapshot` 同步重写（谓词 /r/ 化 + 同值填充处理 + flux 日历拾取 `pickFluxDate` 替代 AMIS `dateInputByLabel().fill()`——flux 日期控件为 button+popover 无可填 input，旧 fill 路径在 flux 下必然失效）；reports.visual 头部注释块 AMIS 编排叙述整体改写为 flux 事实（含 defect B 历史注记保留）。
- [x] Add: `docs/testing/e2e-runbook.md` §7（M2.x helper 扩展规则）范围澄清注记——原位补充一句：「既有 DOM 层 helper『签名与语义冻结』限定于 M2.x 扩面批；bug 修复批经独立计划授权可修改（见 plan 2026-09-01-0527-2）」——消除本计划 Phase 2 Decision 授权与 runbook §7 冻结表述的 owner-doc 矛盾（本 helper 修改为已确定事实，非条件项）
      - Skill: `none`
      - **执行注记**：已落 runbook「视觉方法论 §7」第三条「范围澄清」（含 `assertSnapshot` 函数体零变更边界重申）。

Exit Criteria:

- [x] `npx playwright test tests/e2e/visual/reports.visual.spec.ts tests/e2e/visual/reports.snapshot.spec.ts` 30/30 全绿（24 + 6，零 skip 零降级）
- [x] `_helper.ts` diff 中 `assertSnapshot` 函数体零变更
- [x] runbook §7 范围澄清注记落地（与 Phase 2 Decision 授权无矛盾）
      - **执行注记**：30/30 实测全绿（24 visual + 6 snapshot；6 snapshot 基线按双面协议重录，见 Phase 3/合规声明）。

### Phase 3 - 看板基线确定性、global-setup 守卫与重录合规

Status: completed
Targets: `tests/e2e/global-setup.ts`、`tests/e2e/visual/dashboards.snapshot.spec.ts`、`tests/e2e/visual/dashboards.snapshot.spec.ts-snapshots/`
Skill: `nop-testing`

- Item Types: `Fix | Add | Decision`（Fix 1 + Add 1 + Decision 1，共 3 项）
- Prereqs: 无硬前置（与 Phase 1/2 无依赖冲突，但重录须在 global-setup 守卫生效后进行——本阶段内部排序：守卫修复 → 确定性处理 → 重录）

- [x] Fix: `global-setup.ts` `ensureCurrentMonthOpenPeriod` 期间查询 `le` → 受支持运算符（`dateBetween` 或 `eq`，按查询字段类型裁决）；验证警告日志零出现 + 运行月 OPEN 期间预置实际生效（DB 抽样或 API 抽查）
      - Skill: `nop-testing`
      - **执行注记**：裁决 `dateBetween`（ startDate/endDate 为 LocalDate 字段，缺省 allowFilterOp = eq/in/dateBetween/dateTimeBetween 实证自 nop-biz `ObjMetaBasedFilterValidator.DEFAULT_ALLOW_FILTER_OP`）：「期间包含 today」= `startDate dateBetween [1900-01-01, today]`（≤today）+ `endDate dateBetween [today, 2999-12-31]`（≥today），value 数组形态经 `FilterBeanToSQLTransformer`（toCsvList→min/max）证实。验证：fresh-DB 重启后跑 spec，`[global-setup] period find failed` 警告**零出现**；API 抽查 `E2E-AUTO-202609` OPEN（2026-09-01..2026-09-30）在位且同款 dateBetween 查询返回该期间。
- [x] Decision: inventory spec 日期相对内容确定性处理方案——(a) 与 finance/assets 范式对齐传确定性 `periodId` 参数 vs (b) 按 M0.3 方法论 §2 mask 标准对预警行集区域 mask vs (c) seed 侧固定窗口数据；记录选择、替代方案与残留风险（倾向 (a)：根除漂移源且与既有 2 个传参 spec 范式一致；mask 是兜底非首选——mask 掩盖行集漂移信号）
      - Skill: `nop-testing`
      - **裁决记录（2026-09-01，执行期实证修订）**：**(a)+(b) 组合**，且漂移源经实仓 BizModel 复核修正：(a) inventory 看板无 `periodId` 参数，其日期过滤器为 flux `startDate/endDate`（`filterDates` 经 `pickFluxDate` 传 2026-07-01..2026-07-31，与 dashboards.visual DOM 层 cfg **完全一致**——双面协议要求两层消费同一渲染态），根除 KPI 月窗口翻转（`留空=本月1日..今天` 跨月漂移，08-31→09-01 实际主漂移源）；(b) 趋势图为 `getDashboardTrend` 12 个月滚动窗（**无日期参数可传**，page 仅发 months:12）且 flux 渲染为 recharts **SVG**（不在 canvas canonical mask 内），月轴 key + 尾月半截柱跨月必变 → 按方法论 §2（服务端时间相对内容）页级 mask `svg.recharts-surface first()`。**替代方案**：(c) seed 固定窗口——rejected，cutoff/horizon 均运行时取 today，seed 无法根除；(b) 全页 mask（含预警表）——rejected 过度 mask。**勘误**：滞销/批次效期预警表阈值默认 0=关闭（`ErpInvConstants.DEFAULT_DASH_INV_*=0`，无 yaml 覆盖），渲染恒「暂无数据」静态——bug 笔记原假设「预警行集漂移」不成立（已回写勘误），故预警表**不 mask**（保留未来若开启阈值的像素信号）。**残留风险**（plan-audit 注记 1 修订）：趋势图内容回归在像素层不可见，且实仓复核无任何 E2E spec 断言 `getDashboardTrend`/`netValueChange`——趋势月度聚合正确性当前**三层均未观测**（前置存量缺口，非本计划引入，plan-audit 建议归 M2.4 value 层扩面登记）；若未来 E2E 配置开启预警阈值，其 today 相对行集须按方法论 §2 追加 mask 或确定性处理。
- [x] Add: **独立 plan-audit（视觉 mask 调整 + 像素基线重录保护区域）**——按 M0.3 双面重录协议执行：(a) 确定性处理落地后 inventory spec 像素基线重录（`--update-snapshots`）；(b) 双面对账 = `git diff --stat tests/e2e/visual/dashboards.snapshot.spec.ts-snapshots/` 与 DOM 断言基线同步核查（禁单面）；(c) 独立子代理（新会话）复核 mask 区域合理性与确定性处理方案；(d) **快照重录合规声明**按 runbook §5 载体裁决记录于本计划 `## Draft Review Record`（触发源 / 双面完成 / mask 理由 / `git diff --stat` 对账），本项执行注记留副本；(e) 批准记录落盘本计划本项执行注记
      - Skill: `nop-testing`
      - **执行注记**：(a) inventory 基线重录完成（`--update-snapshots`，仅 inventory 1 文件变更，其余 9 看板零漂移证实其确定性）；(b) 双面对账完成——DOM 层 `dashboards.visual.spec.ts` inventory cfg 已有同款确定性 filterDates（本计划像素层 cfg 镜像之，两层同渲染态；reports 侧 DOM 层 24 测试全绿为像素层重录前置），`git diff --stat` 对账 = dashboards 侧 1 文件（inventory）+ reports 侧 6 文件（Phase 1 page.yaml 变更触发），均与触发源一一对应、无源头外变更；(d) 合规声明见 `## Draft Review Record` 快照重录合规声明节；(e) **批准记录：独立子代理（新会话）`ses_fa5c4c0b9ffeEr292RiYx6xDjB`，AUDIT VERDICT: APPROVE**——6 项检查全 PASS：①计划/声明内部一致且与实仓相符；②mask 合理性（目标=趋势图 bar chart first() 在 pie 之前、不遮 KPI/表格、非 §4.3 违规形态、双面 filterDates 与 DOM 层 :59 逐字一致）；③对账 7 文件 name-for-name 匹配、无未解释变更；④`assertSnapshot` 函数体零 hunk、`pickFluxDate` 仅加 export、runbook §7 注记在位 `e2e-runbook.md:950`；⑤两触发源重录前 diff 复核在案（符合 §4.2）；⑥预警表 config-gate-off 实证（Constants :163/:166 + BizModel 守卫 + yaml 零覆盖）与不 mask 裁决一致。非阻塞注记 3 条已处置：注记 1（趋势聚合三层均未观测的存量缺口措辞修正）已回写 Decision 残留风险并归 M2.4 登记；注记 2（`getEngine` import 清理 + 注释更新属授权改写波及面）记账无语义变更；注记 3（审计者自述 rg 笔误已复核）不影响结论。

Exit Criteria:

- [x] `npx playwright test tests/e2e/visual/dashboards.snapshot.spec.ts` 10/10 全绿 + 二连跑稳定（同日两次 exit 0；跨月稳定性由确定性处理方案根除漂移源承载，方法裁决记录残留风险。注：`reports.snapshot.spec.ts` 6 + `dashboards.snapshot.spec.ts` 10 = snapshot 层合计 16，与 runbook「共 2 spec / 16 测试」口径一致）
- [x] global-setup 运行日志零 `period find failed` 警告
- [x] 独立 plan-audit 批准记录在位 + 双面重录对账完成
      - **执行注记**：dashboards.snapshot 10/10 **三连跑**全绿（超二连跑要求，exit 0 ×3）；global-setup 警告零出现实证见 Fix 注记；plan-audit APPROVE（`ses_fa5c4c0b9ffeEr292RiYx6xDjB`）见 Add 注记 (e)。

## Draft Review Record

- Independent draft review iteration 1: needs revision (独立子代理 ses_fa6438b77ffeZPTVt03M9pAsYC) because 1 Major + 3 Minor——Major：dashboards.snapshot 测试计数误写 16/16（实仓 10，16 为双 snapshot spec 合计，runbook:944 口径）；Minor m1：「如有独立谓词」条件式措辞（实仓已确证 reports.snapshot.spec.ts:34-38 内联谓词在位）+ runbook §7 冻结表述与本计划 helper 修改授权的 owner-doc 矛盾未显式处理；Minor m2：快照重录合规声明载体未对齐 runbook §5 裁决（须落本计划 Draft Review Record）；Minor m3：`_helper.ts:164-167` 行号截断谓词体。全部已修订：10/10 + 合计 16 注记；谓词残留改确证事实 + 新增 Phase 2 Add（runbook §7 范围澄清注记）+ 对应 Exit Criterion；合规声明载体改 `## Draft Review Record`；行号改 `:164-171`。
- Independent draft review iteration 2: accept (独立子代理 ses_fa63be8e2ffeIQITft4VBHzXLK) after 上述 1 Major + 3 Minor 全部确认修复（计数 10/6/24 实仓复核一致、4 项 bug 修复方向全覆盖、保护区域 plan-audit checkbox 与 assertSnapshot 零变更边界完好）；残留 4 Minor 均为非阻塞文本记账（Item Types 头计数 3→4、Fix 项行号 `:164-171`、`:57`→`:58`、本审查记录补填），已随转 active 前内联修复。

### 快照重录合规声明（runbook §5 载体裁决：plan Draft Review Record，2026-09-01）

- **触发源 1（reports 6 基线）**：Phase 1 page.yaml 变更（25 文件 `html:`→`content:` + sanitize:false，报表正文由空渲染恢复真实渲染）+ flux 渲染范式（AMIS-era 07-17 基线本体已因页面范式重写过期）。重录前置复核：未重录先跑 `reports.snapshot.spec.ts` 6/6 失败，diff 报告逐对审视（income-statement 实证对：基线=raw HTML 源码文本框 + AMIS 壳层；actual=真渲染报表表格（边框/表头/`5001 主营业务收入 1,130.00` 数据行）+ flux 壳层）——漂移全部来自预期变更（页面范式 + 修复使正文真实渲染），无真实布局回归信号。
- **触发源 2（inventory 1 基线）**：Phase 3 确定性处理（spec 传 `filterDates` 2026-07-01..31 + 趋势图 SVG 页级 mask）改变了预期渲染态。重录前置复核：未重录先跑单测失败 `156136 pixels (ratio 0.17)`，actual 审视 = KPI 卡为确定性 7 月窗口值（¥10450/0/0/0）+ 趋势图区域为 mask 覆盖（品红矩形）——全部为裁决内预期变化。
- **双面（DOM + 像素）同步完成**：reports 侧 DOM 层 24 测试全绿在先（同一修复生效后），像素层 6 基线随后重录；inventory 侧 DOM 层 `dashboards.visual.spec.ts` cfg 既有同款确定性 filterDates（像素层镜像之），两层消费同一渲染态；非单面重录。
- **mask 合理性自查**：mask 仅 `svg.recharts-surface` first()（趋势图，today 相对滚动窗且无参数可传）；未扩大到正文/表格区域；仓库分布饼图与三张预警表（阈值默认 0=关闭、恒静态「暂无数据」）保留像素信号。自查结论：无「mask 掩盖真实回归换绿灯」违规。
- **`git diff --stat` 对账**：`tests/e2e/visual/reports.snapshot.spec.ts-snapshots/` 6 文件（fin-income-statement / md-material-price-list / crm-lead-conversion-funnel / fin-ar-ap-aging / cs-ticket-sla-csat-summary / mfg-crp-load——全部属触发源 1 清单）+ `tests/e2e/visual/dashboards.snapshot.spec.ts-snapshots/` 1 文件（inventory-dashboard——触发源 2）；**7 个变更文件与 2 个触发源一一对应，无源头外变更**。

## Closure Gates

> 前端页面 yaml + E2E spec 变更，无 Java 生产代码变更；全量 `mvn clean install -DskipTests` 运行一次确认零影响（page.yaml 为 `_vfs` 资源，预期 BUILD SUCCESS 无编译外溢）；compliance checker 复跑（page.yaml 属生产资源文件，如触发计数漂移按 known-failure-mode 规则开基线裁决，预期零漂移）。视觉/UX 主结果面：验证门控自定义 = Phase 2/3 的 Playwright 全绿 + 二连跑稳定（替代通用单测门控，理由：本计划交付面即浏览器行为）。

- [x] 范围内行为完成（三层修复落地：产品缺陷 + 测试契约 + 基线确定性）
      - 证据：25/25 page.yaml 修复（Phase 1 注记）+ 谓词 /r/ 化与编排对齐（Phase 2 注记）+ 守卫修复与 inventory 确定性 + 基线重录（Phase 3 注记）。
- [x] 相关文档对齐（bug 笔记修复状态回写 + 计数勘误；runbook 方法论段如有谓词修法注记原位补充；与 M0.3 方法论/重录协议无矛盾）
      - 证据：bug 笔记「修复记录」节（状态已修复 + 24→25 勘误 + 根因 2/3 勘误 + 附带修复登记）；runbook §7「范围澄清」注记；合规声明落本计划 Draft Review Record。
- [x] 已运行验证（reports 30/30 + dashboards.snapshot 10/10 全绿 + 二连跑稳定 + `mvn clean install -DskipTests` BUILD SUCCESS + compliance checker 零漂移——漂移则基线裁决）
      - 证据（全部 2026-09-01 本会话实测）：`reports.visual 24 + reports.snapshot 6` 30/30 全绿（Phase 2 收口跑 + 终态合并跑）；`dashboards.snapshot` 10/10 **三连跑** exit 0；终态 jar 重启后 40/40（24+6+10）合并跑 exit 0；`dashboards.visual` 10/10 全绿（共享 helper 健全性）；`mvn clean install -DskipTests` 全 156 reactor 模块 BUILD SUCCESS（终态树复跑，exit 0）；`mvn test -pl app-erp-all` **71/0/0/1** 全绿（08-31 基线 69/0/0/1 后 +2 为同日 plan 0527-1 M0.2 门禁新增测试，非本计划变更面，0 失败 0 错误持平）；`bash docs/audits/nop-compliance-checker.sh` 复跑 exit 0 零漂移（R2c=1542 与 known-good-baselines plan-1426-2 已吸收态一致；本计划零 Java 生产代码变更）。
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
      - 证据：Draft Review Record iteration 1 needs-revision + iteration 2 accept。
- [x] 独立 plan-audit（视觉 mask + 基线重录保护区域）已完成并落盘批准记录
      - 证据：`ses_fa5c4c0b9ffeEr292RiYx6xDjB` AUDIT VERDICT: APPROVE（6 检查全 PASS），Phase 3 Add 注记 (e)。
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
      - 证据：Plan Status: completed 与三 Phase `Status: completed` 及全部 `[x]` 一致；执行/退出注记与 Closure 本节证据同源；日志条目见 `docs/logs/2026/09-01.md`。
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
      - 证据：独立子代理（新会话，无执行者上下文）`ses_fa5b06520ffeI1mZOIf1r0vC8A` CLOSURE AUDIT: **APPROVE**（A-F 六检查全 PASS：阶段/勾选一致性、变更集与声明范围精确匹配 40 文件、global-setup dateBetween 语义正确、验证证据内部一致含 md-material-price-list 现场抽查 1 passed、草案/plan-audit 流程完备、范围完整性/Non-Goals 全守恒）；审计者 6 项完成条目（验证证据落本计划、门控勾选、Closure 填写、状态置 completed、日志补 0527-2 条目、2 处文本漂移修正）均已由执行者在本文件与日志中落实。
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

（无——bug 笔记 4 项修复方向全部纳入 Phase 1/2/3；「M2.4 扩面时按方法论 §2 处理日期相对内容」属 M2.4 范畴不纳入本计划，已在 Non-Goals 登记）

## Closure

Status Note: 4 项修复方向全部落地（应用侧 25/25 page.yaml `content:` 修复使报表正文恢复浏览器显示；测试侧谓词 /r/ 化 + flux 编排对齐使 24+6 reports 测试回绿；inventory 看板经确定性 filterDates + 趋势图 mask 根除跨月漂移并重录基线三连跑稳定；global-setup 守卫 dateBetween 化恢复运行月 OPEN 期间预置）。执行期实跑证伪并勘误了 bug 笔记的两处根因假设（envelope 解包层次、预警行集漂移），附带修复 ar-ap-aging NOW() 渲染缺陷；全部裁决与替代方案、残留风险已在计划内记录。独立草案审查（2 轮）+ 独立 plan-audit（APPROVE）+ 独立结束审计（APPROVE）三重独立审查通过。M2.3 报表像素扩面基线解锁（bug 笔记明示的解除条件已满足）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（新会话，无执行者上下文）`ses_fa5b06520ffeI1mZOIf1r0vC8A`
- Evidence: CLOSURE AUDIT: APPROVE（2026-09-01）——A 阶段/勾选一致性（Execution Plan 零残留 `[ ]`）；B 变更集 40 文件与计划声明范围精确匹配（25 page.yaml 形态计数复核 + 缺陷形态 0 残留）；C global-setup dateBetween 包含语义正确（无 le/ge）；D 验证证据内部一致 + 现场抽查 `reports.snapshot.spec.ts -g 'md-material-price-list'` 1 passed（经 8011 存活 app 端到端证明修复链路）；E 草案 2 轮 + plan-audit `ses_fa5c4c0b9ffeEr292RiYx6xDjB` APPROVE + §5 合规声明齐备；F Non-Goals 全守恒（零越界文件、probe 用后即删、兄弟仓库零触碰）。审计者 6 项完成条目全部落实（含 `+50/−26` 与 reports.visual 注释漂移修正、本计划验证证据落盘、日志条目补充）。

Follow-up:

- (仅非阻塞跟进项目；M2.3/M2.4 扩面为 roadmap 后续工作项非本计划 Follow-up)
