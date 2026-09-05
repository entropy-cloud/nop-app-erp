# material-customs.visual.spec 两用例预存失败——findPage 顶层 `limit` 参数为非法调用形态

- 日期：2026-09-01
- 发现于：plan `2026-09-01-0838-1-m11a-md-sal-seed-expansion` Phase 3 视觉快照双面义务核查（运行受影响视觉 spec 时）
- 状态：fixed（2026-09-03 全绿复验：plan `2026-09-03-0400-1-m21-crud-page-pixel-snapshot-expansion` Phase 2 按 0945 方案落地 2 处 `findPage(query: {limit: N})` 后，双 amis 阻塞（`2026-09-03-dual-amis-instance-cell-renderer-double-registration-boot-pageerror`）经 plan `2026-09-03-0938-1` 修复解除，fresh runner 上 `npx playwright test tests/e2e/visual/material-customs.visual.spec.ts --workers=1` → **2/2 绿**）

## 症状

`tests/e2e/visual/material-customs.visual.spec.ts` 两个用例 100% 失败：

- `ErpMdMaterial xmeta exposes 9 cross-border fields`（L22，`ErpMdMaterial__findPage(limit: 1)`）
- `ErpMdMaterialCustoms findPage action is registered and returns page`（L61，`ErpMdMaterialCustoms__findPage(limit: 10)`）

GraphQL 报错：`对象[ErpXxx]的属性[ErpXxx__findPage]没有定义参数[limit]`（`nop.err.graphql.undefined-field-arg`）。

## 根因

spec 调用形态错误：`findPage` 的平台签名（`ICrudBiz`/`CrudBizModel`，javap 实证）仅接受
`query`（QueryBean）+ selection + context，**从未有顶层 `limit` 参数**。正确形态为
`findPage(query: {limit: 10})`（与同仓 `material-customs.visual.spec.ts` 之外的全部 findPage 调用一致）。

spec 编写于 plan `2026-07-21-1206-1`（当时声称全绿），推测随后 nop-entropy 升级使 GraphQL
参数校验转严格（或当时验证未真正覆盖本文件），此后无人单独运行本文件——属**预存失败**，
非 M1.1a seed 批次引入。

## 与 M1.1a 无关的证据链

1. 失败点是 GraphQL schema 层参数名校验，发生在任何数据访问之前；与 `_init-data` 行集无关。
2. 两个用例（`ErpMdMaterial` / `ErpMdMaterialCustoms`）同报错；`ErpMdMaterial` 自 2026-07-08 起
   即有 seed，数据面在 M1.1a 前后无差异。
3. M1.1a 变更面 = 12 个新增 CSV + 测试基线常量 + docs；零 ORM / xmeta / 生产 Java 触点
  （GraphQL schema 无变化）。

## 修复方案（successor）

两处调用改为 `findPage(query: {limit: N})`；修后该文件 2 用例应全绿（findPage 返回形态断言
与 seed 行数无关，M1.1a 后 `ErpMdMaterialCustoms` 有 2 行，仍满足 `Array.isArray(items)`）。

## 回归证据

2026-09-01 M1.1a Phase 3 视觉核查运行：`material-customs.visual.spec.ts` + `dashboards.visual` +
`reports.visual` + `dashboards.snapshot` + `reports.snapshot` 共 52 用例 → **50 passed / 2 failed**，
失败即本 bug 两用例；10 域看板 DOM + 像素、报表 DOM + 像素基线零漂移。
