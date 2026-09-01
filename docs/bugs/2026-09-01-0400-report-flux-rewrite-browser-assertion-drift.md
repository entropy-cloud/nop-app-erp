# 报表 flux 原生重写后浏览器断言层漂移 + 看板像素基线跨月漂移

> 发现：2026-09-01（plan `2026-09-01-0301-2-m03-visual-methodology-codification.md` Phase 1 Proof 运行既有 snapshot spec 时发现）
> 状态：**已修复**（plan `2026-09-01-0527-2-report-flux-browser-assertion-drift-fix.md`，2026-09-01 执行收口；修复形态与计数勘误见文末「修复记录」节）
> 引入：commit `751749e17`（2026-08-31 08:41，plan 2026-08-30-2238-1 F2 flux 页面编译验证发现集修复批，含 plan 2026-08-29-1913-2 报表页 flux 原生重写遗留批——`git log -S` 双确认 `@query:ErpFinReport__renderHtml` + `${reportHtmlData ?? ''}` 皆入于该 commit）；commit `2eef3c348`（08-31 11:17）为同文件最后触碰（仅 +1 行 `valuesPath: filterForm`，非重写者，避免误读）；commit `9a4427d84`（previewData 可选访问范式确立）；测试侧 spec 最后更新停在 `173d86819`（2026-08-23），早于页面重写无同步
> 影响：`tests/e2e/visual/reports.visual.spec.ts`（24 测试）+ `reports.snapshot.spec.ts`（6 测试）自 08-31 起全红；`dashboards.snapshot.spec.ts` inventory 1 测试跨月像素漂移（08-31 录基线 → 09-01 跑 2% > 1% 容差）；**报表页面产品可见缺陷：浏览器中报表正文不显示**

## 症状

- 全部 24 报表 DOM 断言 + 6 报表像素断言：`TimeoutError: page.waitForResponse（30s）`——等待「`/graphql` 响应且 body 含 `renderHtml`」超时。
- inventory dashboard snapshot：`13801 pixels (ratio 0.02) different`（容差 0.01）。
- global-setup 警告：`[global-setup] period find failed: 查询字段只允许以下查询运算符:[eq, in, dateBetween, dateTimeBetween], 不支持le`。

## 根因（三层，均经实跑 probe 实证）

1. **测试侧契约漂移**：报表页 flux 原生重写后，渲染取数改为 `data-source`（`url: '@query:ErpXxxReport__renderHtml'` → REST `/r/ErpXxxReport__renderHtml`，body 形如 `{"reportName":"income-statement","data":{"periodId":1}}`），页面加载即自动取数；旧 AMIS 范式（service reload → `/graphql` + renderHtml body）不再存在。spec 的 `page.waitForResponse` 谓词仍按旧契约等待 → 恒超时。dashboards 侧同型漂移已在 c677f76cc 修复（等待谓词 /r/ 化），reports 侧漏改。
2. **应用侧报表正文绑定缺陷**：`/r/ErpXxxReport__renderHtml` 返回 200 且响应体 `{"data":"<div id=\"xpt-report\">…"}` 含完整报表 HTML，但页面 html 渲染器模板为 `html: "${reportHtmlData ?? ''}"`——data-source 结果对象未解包（正确形态应为 `${reportHtmlData?.data ?? ''}`，同 `9a4427d84` 确立的 `previewData?.x` 可选访问范式），实跑 `[data-slot="html"]` innerHTML 长度 0、table count 0 → **浏览器中报表正文不显示**（产品可见缺陷）。
3. **看板像素基线跨月漂移**：inventory dashboard spec 无确定性日期/期间参数（finance 传 periodId=1、assets 传 periodId='2026-07'，inventory 无），其滞销/批次效期预警行集随 today 相对窗口变化（08-31 → 09-01 跨月行集移动）→ 像素 diff 2% 超容差。放大器：`tests/e2e/global-setup.ts` `ensureCurrentMonthOpenPeriod` 的期间查询用了不支持的 `le` 运算符，运行月 OPEN 期间预置静默失败（runbook「日期漂移防护」守卫失效）。

## 与三路径分诊的关系

按 runbook「渲染模式与 flux 调试三路径」排查：非路径 1（flux 控件缺陷——`/r/` 取数与 html 渲染器均正常工作）、非路径 2（壳层缺陷——页面加载/导航正常）、非路径 3（bundle 需重建）。根因在本仓库应用层页面 schema 与测试侧 spec 的契约漂移，修复均落本仓库。

## 修复方向（归 successor，本笔记发现时未修复）

1. **应用侧（产品缺陷，优先）**：全 24 报表 `*.page.yaml`（`pages/report/*.page.yaml`）html 模板 `${reportHtmlData ?? ''}` → `${reportHtmlData?.data ?? ''}`（或经 data-source adaptor 拍平），使报表正文恢复显示；建议补 1 个最小行为冒烟（报表 token 进 DOM）。
2. **测试侧**：`reports.visual.spec.ts` + `reports.snapshot.spec.ts`（及 `_helper.ts` `assertReportRendered` 注释块所述编排）等待谓词 `/graphql`+renderHtml body → `/r/`+URL 包含 `ErpXxxReport__renderHtml`；镜像 c677f76cc dashboards 侧修法。涉及 spec/helper 语义修改，按 plan 规则须独立计划承接（M0.3 Task Route 明示 spec 修改须修订计划，故本发现登记而不动 spec）。
3. **像素基线**：inventory-dashboard 基线在应用/测试修复后按双面重录协议重录；方法层面已由 M0.3 视觉方法论段 §2 mask 标准覆盖（日期相对内容须确定性填充或 mask），inventory spec 属该方法论落地前的存量缺口，M2.4 扩面时按新范式处理。
4. **global-setup 期间守卫**：`ensureCurrentMonthOpenPeriod` 查询改用受支持运算符（`eq`/`in`/`dateBetween`），恢复运行月期间预置。

## 经验

- 页面范式重写批（page.yaml → flux 原生）必须同步扫描消费旧页面契约的浏览器 spec（等待谓词/选择器/断言 token）——「页面改了、测试谓词没跟」是 flux 迁移期的高发回归形态（dashboards 侧 c677f76cc 已修、reports 侧漏网的根因即缺这一步扫描）。
- data-source 结果在 flux 模板中是对象，字段访问须显式 `.x` 解包（`previewData?.x`），裸引用对象经 html 渲染器输出为空，且**不报错**——静默空渲染，浏览器冒烟才能捕获。
- **flux html 渲染器的内容 prop 名是 `content` 而非 `html`**（`HtmlSchema` 类型契约；`html:` key 被编译期归类为未知 prop 静默忽略）——本 bug 的空渲染主因即重写批用了 `html:` key；「解包 `.data`」的早期假设经实跑证伪：ajax 层已对 RPC envelope（`{status, data}`）解包一次，data-source 作用域值就是 html 字符串本身（勘误下文根因 2）。

## 修复记录（plan 2026-09-01-0527-2，2026-09-01）

- **计数勘误**：缺陷 page.yaml 实测 **25 个**（`rg` 缺陷形态 25 命中），本文记「全 24 报表」为 draft 期 `find` 口径误差（25 含 `fin/period-close-report` 等）；执行时以实仓清点为准。
- **根因 2 勘误（应用侧）**：实测 `/r/ErpXxxReport__renderHtml` 响应 envelope `{"data":"<html>","status":0}` 经前端 ajax 层 `unwrapApiPayload` 解包一次，flux 作用域 `reportHtmlData` 即 html 字符串；空渲染的真正根因是重写批用了渲染器不消费的 `html:` key（正确契约 = `content:`，见 nop-chaos-flux `HtmlSchema`）。修复形态（25/25 全覆盖，浏览器实测 innerHTML 非空 + token 在位）：`- type: html` 节点改 `content: "${reportHtmlData ?? ''}"` + `sanitize: false`（报表 HTML 为第一方 xpt 引擎产物，含 `<style>` 布局 CSS，DOMPurify 缺省会剥 style）。
- **附带发现（ar-ap-aging 页）**：input-date 默认值 `value: "${NOW()}"` 在 flux 模板无 NOW() 函数 → 节点渲染错误、日期选择器不可用；修复 = 移除该默认值（留空 = 后端全量范围口径，与 value-spec 层零日期行为一致）。
- **根因 1（测试侧）**：已修——`assertReportRendered` 及 `reports.snapshot.spec.ts` 内联谓词 `/graphql`+renderHtml body → `/r/`+URL 含 renderHtml（镜像 c677f76cc）；编排对齐 flux「加载即自动取数 + 填充触发去抖 reload」，删「渲染报表按钮点击」假设；同值填充不触发 reload 的语义在 helper 中显式处理。reports 24 + 6 测试回绿（30/30）。
- **根因 3（inventory 像素漂移）**：已修——实际漂移源为 KPI 月窗口翻转（`留空=本月1日..今天` 跨月）+ 趋势图 12 月滚动窗（recharts SVG 不在 canvas canonical mask 内）；滞销/批次效期预警表阈值默认 0=关闭（静态「暂无数据」，本文原假设的行集漂移不成立，勘误）。修复 = spec 传确定性 `filterDates`（2026-07-01..31，镜像 DOM 层 cfg）+ 趋势图 SVG 页级 mask；基线重录后 dashboards.snapshot 10/10 三连跑全绿。
- **根因 4（global-setup）**：已修——`le`/`ge` → `dateBetween`（value=[min,max] 受支持运算符），运行月 OPEN 期间预置生效（API 抽查 `E2E-AUTO-202609` OPEN 在位）、`period find failed` 警告零出现。
