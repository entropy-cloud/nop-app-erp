# 2026-07-09 看板/报表 AMIS `$var` GraphQL 查询模板损坏（生产缺陷）

> 发现于：`docs/plans/2026-07-09-1249-2-dashboard-report-runtime-visual-regression.md` Phase 1（看板 AMIS 前端渲染层 DOM 断言）
> 严重度：P1（全 8 参数化看板 + 全参数化报表页面前端 KPI/渲染值恒为空/0，用户不可见正确数值）
> 状态：未修复（归 plan 2026-07-09-1249-2 Deferred successor「看板/报表 AMIS `$var` 查询模板修复」）

## 症状

看板页面的 KPI 卡片经 AMIS 前端渲染后数值为 `¥0` / 空。冒烟层（`*.smoke.spec.ts`，仅查标签 + GraphQL 200）与数值断言层（`*.value.spec.ts`，`page.request.post('/graphql')` 直调后端绕过 AMIS）**均无法捕获**此缺陷——前者不验数值、后者不经 AMIS。本缺陷由本计划新增的看板前端渲染层 DOM 断言（`tests/e2e/visual/`）首次发现。

## 根因

全参数化看板/报表 `*.page.yaml` 的 `api.data.query` 采用手写 GraphQL 查询字符串 + `$var` 变量引用范式：

```yaml
api:
  url: /graphql
  method: post
  dataType: raw
  data:
    query: "query($periodId:Long){ ErpFinDashboard__getDashboardKpi(periodId:$periodId) }"
    variables:
      periodId: "${periodId || null}"
```

运行时 AMIS（`amis-core`）在构建 api 请求体时对 `data` 对象的字符串值执行模板解析，将查询字符串中的裸 `$periodId`（`$` + 标识符）当作模板变量引用解析为当前作用域值（默认空），导致查询字符串被损坏。

## 实测证据（运行时抓取的 AMIS 实际请求体）

财务看板经 AMIS 前端发出的实际 `/graphql` 请求体（`tests/e2e/visual/_debug.spec.ts` 抓取 `request.postData()`）：

```json
{
  "query": "query(:Long){ ErpFinDashboard__getDashboardKpi(periodId:) }",
  "variables": { "periodId": "" }
}
```

对照源 `main.page.yaml`（`module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/dashboard/main.page.yaml:28`）应为 `query($periodId:Long){ ErpFinDashboard__getDashboardKpi(periodId:$periodId) }`——`$periodId` 两处 token 均被替换为空字符串，`variables.periodId` 也为空串（非 null）。

**影响链**：损坏查询 `getDashboardKpi(periodId:)` 到达后端 → 服务端按 periodId=null/空聚合 → 返回 KPI=0/空 → AMIS tpl 渲染 `¥0` / 空 DOM。

## 影响范围（仓库内核实）

`dataType: raw` + 手写 `query(...$var...)` 范式为 nop-app-erp 自创（非平台文档支持的模式），全仓库一致使用，共 67 处。受影响文件：

- **8 参数化看板**（本缺陷致 KPI 恒 0/空）：finance/sales/purchase/inventory/assets/manufacturing/maintenance/quality 的 `module-*/erp-*-web/.../pages/dashboard/main.page.yaml`
- **全参数化报表**（渲染 button 即使接线，renderHtml 入参也会被损坏）：`module-*/erp-*-web/.../pages/report/*.page.yaml`
- **未受影响**：2 非参数化看板（projects/master-data，查询无 `$var`，如 `{ ErpPrjDashboard__getDashboardKpi }`）渲染正确——已由 `tests/e2e/visual/dashboards.visual.spec.ts` 数值 token 断言证实（projects `50000`、master-data `4`）。
- **注意（successor 须覆盖）**：projects 看板的**第二**个 service `grossMarginService`（`getProjectGrossMargin`，`main.page.yaml` 查询 `query($projectId:Long)...`）**含 `$var` 同样受损坏**——其毛利率卡片渲染 ¥0；visual spec 仅断言免疫的 `getDashboardKpi`，未覆盖 `grossMarginService`。successor 修复 `$var` 时须覆盖 projects 的 `grossMarginService` + 其余看板的全部含 `$var` 子查询（如 `getDashboardTrend`/`findCustomerTopN`/`findCashFlowAlert` 等 trend/topN/alert 子查询也含 `$var` 或经 AMIS 路径，须一并核实）。

核实命令：`rg "dataType: raw" module-*/erp-*-web/src/main/resources`（67 命中）；`rg '\$\w+:\s*(Long|String|Int|BigDecimal)' module-*/erp-*-web/src/main/resources/_vfs/**/dashboard/main.page.yaml`（参数化看板）。

## 为何既未冒烟层也未数值层捕获

| 层 | 机制 | 为何漏检 |
| --- | --- | --- |
| 冒烟层 `*.smoke.spec.ts` | 断言 body 文本 > 100 字符 + 标签关键词 + GraphQL `/graphql` 200 | 损坏查询 `getDashboardKpi(periodId:)` 仍返回 HTTP 200（错误在响应体 KPI=0，非 HTTP 状态）；标签关键词（"收入"等）在 tpl label div 中静态存在，不依赖值 |
| 数值断言层 `*.value.spec.ts` | `page.request.post('/graphql', { query, variables })` 直调后端 | 完全绕过 AMIS——spec 内自带的 `query` 字符串含正确 `$var`，不经 AMIS 模板解析，故取到正确值 |

本计划新增的 **AMIS 前端渲染层 DOM 断言**（驱动真实 AMIS 页面 → 拦截 AMIS 自身 GraphQL 响应 → 断言 DOM）是首个能捕获此缺陷的层。

## 备选修复方向（供 successor 计划评估，本计划 Non-Goal 不实施）

1. **迁移至平台规范的 `@query:` URL 范式**（`<nop-entropy/docs-for-ai/02-core-guides/api-and-graphql.md>` §「`@query:` AMIS API URL 机制」）：`url: "@query:ErpFinDashboard__getDashboardKpi"` + 扁平 `data`，由前端 `nop-chaos/packages/nop-core/src/core/graphql.ts` 的 `handleGraphQLUrl` 在 JS 中生成查询字符串，`$var` token 由代码生成不经模板解析，结构上免疫。
   - **风险**：`guessDefinition(data)` 从表单值推断参数类型——finance `periodId:Long` 传数字 1 会被推断为 `Int`，与 BizModel 声明的 `Long` 不匹配，GraphQL 校验可能报 `Variable '$periodId' of type 'Int' used in position expecting type 'Long'`。需 successor 核实 Long 类型参数的推断行为（日期型 String 参数无此问题）。
2. **转义裸 `$`**：在查询字符串中以 AMIS 模板表达式 `${'$'}` 输出字面 `$`，规避 `\$word` 解析（如 `query(${'$'}periodId:Long){ ...periodId:${'$'}periodId }`）。需 successor 实测 AMIS 单趟解析是否对输出再扫描。
3. **`requestAdaptor` 重建请求体**：在 AMIS adaptor 层重新拼装正确的 query+variables。增加复杂度，非首选。

successor 计划须：(a) 选定修复方向并实测；(b) 修复全 8 参数化看板 + 全参数化报表 page.yaml；(c) 将 `tests/e2e/visual/dashboards.visual.spec.ts` 中 8 参数化域的结构断言升级为确定性数值 token 断言（期望值派生自 `docs/analysis/2026-07-08-1445-2-kpi-expected-values.md` 等）；(d) 报表渲染容器接线修复（plan 2026-07-09-1249-2 既存 Deferred）与本缺陷修复可合并为一个报表前端 successor。

## 复现

```bash
# 1. 启动种子库应用（fresh-DB + 91 CSV 种子）
lsof -ti :8080 | xargs kill -9 2>/dev/null
rm -f db/erp.mv.db db/erp.trace.db
java -Dfile.encoding=UTF8 -Dnop.auth.service-public=true \
     -Dnop.auth.login.allow-create-default-user=true \
     -Dnop.orm.init-database-data=true \
     -jar app-erp-all/target/quarkus-app/quarkus-run.jar

# 2. 浏览器打开 http://127.0.0.1:8080，登录 nop/123，访问 /#/fin-dashboard-main
#    观察 KPI 卡片显示 ¥0（应为 ¥1130 收入/净利润）

# 3. DevTools Network 抓 /graphql 请求，查看 payload.query 含 "query(:Long){ ...periodId: }"（$periodId 被吃）
```
