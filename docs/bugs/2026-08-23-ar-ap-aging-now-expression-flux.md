# 2026-08-23 ar-ap-aging 报表页 `${NOW()}` 表达式 flux 求值失败（AMIS 公式函数残留）

## 现象

应收应付账龄报表页（`module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/report/ar-ap-aging.page.yaml:12`）的账龄基准日默认值 `value: "${NOW()}"` 在 flux 渲染下求值失败，NodeRenderer 抛 `Expression evaluation failed for: ${NOW()}` 并输出 console error，触发 E2E fixtures 的 console-error 门控（`tests/e2e/fixtures.ts:43`）：

- `reports/fin-ar-ap-aging` smoke（页面冒烟）与 value（renderHtml 数值断言）两用例红；
- `reports/_helper.ts:278` 直接下载层 fin ar-ap-aging xlsx/pdf 两用例红（helper 先 `loginAndNavigate` 到 `/#/ar-ap-aging`，导航期 console error 即失败）。

共 4 个 E2E 用例确定性红。

## 根因

`${NOW()}` 是 AMIS 公式函数（返回当前时间戳），页面 2026-07-12 落地时按 AMIS 语义编写。flux 表达式求值器（nop-chaos-flux `flux-formula`）不实现 `NOW()` 函数——公式编译抛错而非降级。flux 的日期相对值支持形态是**裸关键字**（`now`/`today`/`now±Nd` 等，`flux-renderers-form/src/renderers/date/date-utils.ts#resolveRelativeDate`），与 `${...}` 表达式是两套机制。

该缺陷自页面落地（07-12）即存在，非 id 迁移引入：2026-08-11 全 enforcement 栈 sweep 的 reports 批次日志（`_tmp/e2e-results/reports.log`，当时 bundle `pkg-nop-chaos-flux-ttsAZmES.js`）已含同签名失败——但 08-11 计划汇总把 reports 批次误记为「46/0/0」（仅登记 passed 数，60 failed 漏记），白名单因此未收录。2026-08-23 M4.1 flux 全量回归复现并落本登记。

## 影响

- ar-ap-aging 报表页在 flux 下账龄基准日无默认值且首屏渲染带 error monitor 记录（表单仍可用，手输日期可渲染报表——value 层 GraphQL 直调后端不受影响）。
- E2E 4 用例红（预存，08-11 与 08-23 两口径均在）。

## 修复建议（successor）

应用层最小修复（生产代码，出本计划范围）：`value: "${NOW()}"` → `value: "today"`（flux 相对日期关键字，语义=当日 0 点，匹配 valueFormat YYYY-MM-DD）。全仓 grep 证实 `${NOW()}` 仅此 1 处。若需 AMIS/flux 双引擎对照期兼容，可改用后端默认（页面不设默认值，后端 asOfDate 缺省取 today——2026-07-12-1321 宽容解析已就绪）。

## 复现

fresh-DB + flux：`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 E2E_ENGINE=flux npx playwright test tests/e2e/reports/ --grep "ar-ap-aging"`（确定性红，console error 含 `Expression evaluation failed for: ${NOW()}`）。

## 状态

open（登记日 2026-08-23，plan 2026-08-23-0434-2 Phase 2/3 裁决落盘；successor = 应用层 page.yaml 修复，载体归 backlog 择期）
