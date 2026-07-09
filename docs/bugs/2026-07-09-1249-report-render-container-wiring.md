# 2026-07-09 报表渲染容器接线缺陷（23/24 报表渲染 button 无 reportContainer 注入）

> 发现于：`docs/plans/2026-07-09-1249-2-dashboard-report-runtime-visual-regression.md` 草案审查期（Current Baseline）
> 严重度：P1（用户点「渲染报表」后端计算返回但页面容器不显示，23 张报表前端不可见渲染结果）
> 状态：未修复（归 plan 2026-07-09-1249-2 Deferred successor「报表渲染容器接线修复 + 报表 AMIS 前端渲染层 DOM 断言」）

## 症状

24 张报表 page.yaml 的「渲染报表」button 触发 `ErpXxxReport__renderHtml` GraphQL 调用（后端正确计算返回 HTML），但响应 HTML 未注入页面渲染容器——容器 `html: ""` 静态空，用户点按钮后页面无变化。报表 AMIS 前端渲染层 DOM 断言在此缺陷修复前不可达（故 plan 2026-07-09-1249-2 将报表面整体移出范围为 Non-Goal）。

## 根因

报表 page.yaml 的渲染 button 仅 `actionType: ajax` 触发 GraphQL，但**缺少响应管线**将 ajax 响应的 `reportHtml` 经 `setVariable` → `setValue target reportContainer` 注入 html 容器。仅 `balance-sheet.page.yaml` 正确接线（镜像其范式），其余 23 张报表渲染 button 响应被丢弃。

## 实测证据

```bash
# 核实：仅 balance-sheet 将渲染响应注入容器
$ rg -l 'target:.*"reportContainer"|reportHtml' module-*/erp-*-web/src/main/resources/_vfs/**/report/*.page.yaml
module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/report/balance-sheet.page.yaml
# 仅 1 命中（balance-sheet），其余 23 张报表 page.yaml 无 reportContainer/reportHtml 接线
```

`balance-sheet.page.yaml`（正确接线范式）将渲染 button 的 ajax 响应经 `setVariable reportHtml → setValue target reportContainer` 注入 `reportContainer`（html 容器），3 处命中。其余 23 张（income-statement/cash-flow/period-close/ar-ap-aging + 全域其余报表）渲染 button 仅 `actionType: ajax` 触发 GraphQL 但**无响应管线**注入 `reportContainer`（`html: ""` 静态空）。

## 影响范围

24 张报表 page.yaml 中 23 张受影响（仅 balance-sheet 接线正确）：
- `module-finance/.../report/`：income-statement / cash-flow / period-close / ar-ap-aging
- 全域其余报表 page.yaml（ast/inv/prj/mfg/mnt/qa/crm/cs/hr/md 域报表）

## 修复方向（供 successor 计划）

镜像 `balance-sheet.page.yaml` 的接线范式补全 23 张报表 page.yaml：渲染 button 的 ajax 响应经 `setVariable reportHtml → setValue target reportContainer` 注入 html 容器。可与 AMIS `$var` 查询模板损坏修复（`docs/bugs/2026-07-09-1249-dashboard-amis-var-mangling.md`）合并为统一报表/看板前端 successor——报表 `renderHtml` 入参同样受 `$var` 损坏影响（如 `query($reportName:String!,$periodId:BigDecimal)`），故报表 DOM 断言须在两缺陷均修复后才有意义。

successor 计划须：(a) 补全 23 张报表 reportContainer 接线；(b) 修复报表 `$var` 查询模板损坏（与看板同范式）；(c) 落地报表 AMIS 前端渲染层 DOM 断言（renderHtml 返回 HTML 经 AMIS 注入 reportContainer 后断言容器 DOM 含期望数值 token）。

## 复现

```bash
# 启动种子库应用后，浏览器打开任一未接线报表（如利润表）
# /#/income-statement → 点「渲染报表」button → 观察 reportContainer 区域无变化（应为渲染的报表 HTML）
# DevTools Network 确认 renderHtml GraphQL 返回 200 + 非空 HTML，但页面容器仍空
```
