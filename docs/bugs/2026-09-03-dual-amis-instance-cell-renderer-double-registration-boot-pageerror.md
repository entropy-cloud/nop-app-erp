# 主 bundle 双份 amis 实例——每次 fresh boot 重复注册 AMIS renderer `cell` 抛错，全量 E2E 套件红灯

- 日期：2026-09-03
- 发现于：plan `2026-09-03-0400-1-m21-crud-page-pixel-snapshot-expansion` Phase 2 验证（material-customs findPage 修复后运行视觉 spec 时）
- 状态：fixed（未提交，工作树；nop-chaos-next 提交由仓库 owner 执行——移交项）。修复 = plan `2026-09-03-0938-1-dual-amis-office-viewer-peer-alignment` Phase 1（office-viewer peer 对齐 + lockfile 去重，双独立子 agent 批准在案）。遗留：flux 运行时 data-source/公式渲染回归（被本 bug 掩蔽、修复后首次可见）已另立 `docs/bugs/2026-09-03-flux-runtime-datasource-formula-render-regression.md`
- 影响：**全量 Playwright E2E 套件**（凡经 `tests/e2e/fixtures.ts` page fixture 的 spec）100% 失败——非 M2.1 计划引入

## 症状

每个**新鲜浏览器上下文**的 SPA boot 必抛 pageerror：

```
PAGEERROR: The renderer with type "cell" has already exists, please try another type!
    at tm (assets/vendor-amis-core-*.js)
    at vendor-amis-core-*.js (module init)
    at di (vendor-misc-*.js)
    at vendor-amis-*.js:4:79424   ← TableCell 装饰器 init（X({type:'cell',name:'table-cell'})）
    at rolldown-runtime-*.js ...
```

`tests/e2e/fixtures.ts` 的 console/pageerror 守卫对任何未过滤 pageerror 抛错 → 所有 spec 级联失败（`dashboards.visual.spec.ts` 10/10 失败；独立探针 8/8 次 fresh boot 全部复现，确定性非 flaky）。

## 根因（运行时 bundle 证据链）

1. 抛错点 = AMIS `registerRenderer`（`vendor-amis-core` chunk）：registry 中已存在 `cell` 且 component 不同、无 `override` → throw。
2. `vendor-amis` chunk 内 `table-cell` 字符串出现 **2 次**——TableCell 模块在同一 bundle 中存在两份实例，第二次 init 重复注册 `cell`。
3. 两份实例的来源 = **两个物理 `amis` pnpm 实例同时进入 main bundle**：
   - `apps/main/node_modules/amis` → `.pnpm/amis@file+libs+amis-6.13.1-fix.0.tgz_…_amis-core@…a2b44b20b…`（main 的 peer 解析上下文）
   - `packages/amis-react/node_modules/amis` → `.pnpm/amis@file+libs+amis-6.13.1-fix.0.tgz_…_amis-core@…f4ca194cbf…`（amis-react 的 peer 解析上下文）
   - 两个 amis 实例的 `amis-core` 内层 symlink 虽指向同一 `.pnpm/amis-core@…089887d6…` 实例，但 amis 自身目录物理不同 → vite/rolldown 视为两棵模块树 → TableCell 模块重复打包、重复求值。
4. `table-cell` 在 `vendor-amis-core` chunk 中 0 次、`vendor-amis` chunk 中 2 次；registry（`registerRenderer`/tm）仅在 `vendor-amis-core` 一份 → 两份 cell-def 共享同一 registry，第二份注册时抛错。

## 引入窗口与漏检原因

- 2026-09-03 ~03:00：M1.5（plan 2026-09-02-1415-2）闭包验证「视觉双面 50/50 全绿」→ 当时链路无此错。
- 2026-09-03 06:19：picker 闭包（plan 2026-09-02-2028-1）`rebuild-flux-chain.sh` 全链重建后的 runner jar → 本 bug 已在。
- 漏检：2028-1 收口验证 = `npm run validate:flux`（静态）+ Java 测试 + 5 页浏览器抽样（`tests/e2e/dbg-picker-v3-sampling.spec.ts` 调试 spec，**无 fixtures console 守卫**）→ 页面级 pageerror 未被任何门禁捕获。
- 2026-09-03 ~08:30 复核：再次全链重建（flux `44ef5188f`）后错误依旧 → 非陈旧产物问题，是**当前已提交依赖图/打包状态的确定性回归**（nop-chaos-next 自 08-29 未变、amis tgz libs 自 07-22 未变；触发面为 picker 轮前后 bundle 图谱变化，精确 commit 归因归 successor）。

## 修复方案（successor；跨仓库保护区 = `auto + dual-agent-approval`）

按 `docs/context/ai-autonomy-policy.md` 外部仓库代码边界执行（跨仓库 plan + 双独立子 agent 批准）。方向：

1. **统一 amis 实例解析（首选）**：nop-chaos-next 根 `pnpm.overrides` 强制 `amis`/`amis-core`/`amis-ui`/`amis-formula` 全 workspace 单一 `file:libs/…tgz` 实例；或把 `@nop-chaos/amis-react` 对 `amis` 的依赖改为 peer、由 main 注入同一实例。修后 `find ../nop-chaos-next -path "*node_modules*" -maxdepth 6 -type l -name amis` 应只解析到一个 `.pnpm` 物理目录。
2. 打包层兜底：`main-bundle-utils.mjs` chunking 按 realpath 去重（治标，不推荐单用）。
3. 归因补强：定位「两个 amis 实例同时进入 main entry 图谱」的精确引入 commit（bisect flux picker 轮 + next 构建产物）。
4. 门禁补强（本仓可做）：E2E 全套件本身即是守卫——任何收口若只跑调试 spec/静态门禁，须显式声明「未跑 fixtures 守卫套件」的理由。

## 回归证据

- 2026-09-03 08:2x：`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/visual/dashboards.visual.spec.ts --workers=1` → 10/10 失败，错误签名全部为本 bug；material-customs 2 用例的 GraphQL 断言本体（findPage 修复后）已通过，失败同为本 bug 的 pageerror 守卫。
- 2026-09-03 08:5x：独立探针（playwright chromium）8/8 次 fresh boot + login + 导航全部复现同一 pageerror；同会话内二次 hash 导航不再复现（错误仅发生于首次 boot）。
- 2026-09-03（mission-driver 重跑复验）：08:44 fresh runner jar（全链重建产物）在跑，`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/visual/material-customs.visual.spec.ts --workers=1` → 2/2 失败，签名与本 note 完全一致；盘面双 amis 物理实例（`…_a2b44b…` / `…_f4ca19…` realpath 实证）与根 `package.json` `pnpm.overrides` 无 amis 条目均复验在案 → 修复仍未落地，M2.1 计划 Phase 2 基线采集维持冻结。
- 2026-09-03（mission-driver 第三次重跑复验）：PID 82994（08:44 全链重建 runner）在跑，探针 material-customs `--workers=1` → 2/2 失败，签名不变（fresh boot `The renderer with type "cell" has already exists`，fixtures.ts:43 守卫）；盘面双 amis realpath（`…_a2b44b…`/`…_f4ca19…`）与根 `pnpm.overrides`（仅 `webworkify-webpack`）均复验在案；nop-chaos-next HEAD `63899d6`（2026-08-29）未动、`docs/plans/` 无 successor 修复计划落盘 → 修复仍未落地，M2.1 计划 Phase 2 基线采集维持冻结（复验记录见计划 Phase 2 阻塞记录第三次复验段）。
- 2026-09-03（修复落地实证，mission-driver 第四次重跑）：successor plan 0938-1 Phase 1 落地（`packages/amis-react/package.json` 增 `office-viewer: file:../../libs/office-viewer-0.3.14.tgz` 对齐 peer 上下文 + `pnpm install` lockfile 去重）+ `rebuild-flux-chain.sh` 全链重建 → fresh runner（09:52 启动）。实证：① `realpath apps/main/node_modules/amis` ≡ `realpath packages/amis-react/node_modules/amis`（同一 `.pnpm/amis@…_a2b44b…` 物理目录，`.pnpm` 仅 1 份 libs-amis 实例）；② `BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/visual/material-customs.visual.spec.ts --workers=1` → **2/2 绿、零 pageerror**；③ `dashboards.visual.spec.ts` 10/10 绿；④ 当日全量 visual 目录级运行（225 测试）中 dashboards/reports 四 spec 零失败——本 bug 引入的全量红灯消除。**内容差异承认**（双批准 iteration 2 Major-1 条件）：amis-react 分支 office-viewer 统一为 libs 补丁版 tgz（与 npm 0.3.14 非字节等价，如 `Excel.render` 签名差异），与 apps/main 现状对齐，office/excel 预览类页面在全量运行中未出现漂移失败。
