# Flux 页面批量导出与编译验证工具链（跨仓库设计）

> 维护目的：定义一套**通用**的「把应用全部页面导出为 flux 页面 JSON，再用 JS 工具验证其符合 flux 框架编译要求」的工具链。工具实现分布在 nop-entropy（Java 导出）与 nop-chaos-flux（JS 验证），nop-app-erp 作为首个接入方与验证场。
>
> 所有权：本文件拥有该工具链的架构契约（导出语义、目录约定、验证层级、调用方式）。具体实现细节归各仓库代码与其自身文档。

## 1. 背景与问题

nop-app-erp 的页面模型是 `.page.yaml`（内嵌 AMIS 语义 DSL），flux-only 渲染（见 `docs/architecture/view-and-page-strategy.md`「渲染模式（flux-only，强制）」）。从 `.page.yaml`（含经 `web:GenPage` 引入的 `.view.xml`）到浏览器实际消费的 flux 页面 JSON，转换发生在 **Java 层**（nop-entropy `PageProvider` + `nop.web.render-mode=flux` 下的 xlib 生成管线）；而 flux 页面是否**符合 flux 框架的编译要求**（renderer 类型存在、表达式可编译、结构合法），只有 **JS 层**（`@nop-chaos/flux-compiler`）能判定。

当前缺口：

1. **已有导出能力只有「demo 级」编排**。`PageProvider.renderPagesTo(PageRenderOptions, File)`（`nop-entropy/nop-frontend-support/nop-web/.../page/PageProvider.java:113`）具备批量渲染能力，`nop-runner/nop-cli/demo/scripts/render-pages.xrun` 是其唯一脚本调用方（`TestNopCli.testRenderPages`/`testParameter` 在 CI 中执行该脚本），`docs/dev-guide/cli.md` 亦文档化此用法。但该路径不处理 render-mode 切换与缓存清理、不产出 manifest、失败即中断、options 由调用方散装拼装——不构成可复用的「导出工具」。
2. **现有导出语义与生产路径存在两类偏差**：
   - **flux.yaml 回退被绕过**：flux 模式下生产路径（`PageProviderBizModel.getPage` → `PageModelLoaderFactory`）对 `*.page.yaml` 会**优先回退加载同目录同名 `*.flux.yaml`**（`PageModelLoaderFactory.java:38-46`）；而 `renderPagesTo` 直接对 VFS 找到的 page.yaml 资源调用 `loadPage`。手写 flux.yaml 的页面（本仓 31 个）导出结果将与浏览器实际收到的内容不一致。
   - **解析器选项缺省与生产相反**：`PageRenderOptions` 的 `useResolver/resolveI18n` 缺省均为 `false`，而生产 `getPage`（经 `locale|path` 组件 key，`resolveI18n=true`）恒定启用 i18n/cfg/load 解析器。缺省导出会保留未解析的 `@i18n:` 标记串（本仓 view.xml 生成链大量使用），验证对象失真。
3. **无 JS 侧批量验证入口**。nop-chaos-flux 的 `flux-guide/scripts/validate.mjs` 已证明「Node 无浏览器批量 validateSchema」可行（该仓 `flux-guide/package.json` 的 `validate` script 以 `css-stub` loader + `env-stub` import 双桩运行），但它只扫描 flux-guide 文档内嵌代码块，不是对外部页面产物目录的通用 CLI；且其包装层对 null/非对象/未知 type 根**静默跳过**——对文档示例合理，对导出产物则是「坏页绿灯」漏洞，不可沿用。nop-chaos-next 的 `dist/sdks/` 中的 `nop-chaos-flux-0.1.0.tgz`（flux-bundle）**不导出 compiler API**（`validateSchema`/`createSchemaCompiler`），sdks 目前无法承载此验证。
4. **flux 模式的「加载正确性」验证已存在，但「编译合法性」与「产物消费」缺失**。本仓 `app-erp-all` 已有 `ErpAllFluxPagesTest`（flux 模式对全部 enabled modules 页面 `getPage` 并断言零错误，基线绿）与各模块 `ErpXxxWebPagesTest`（amis 模式 `validateAllPages()`）——两者都只验证「页面能加载」，不产出可被 JS 消费的产物，也不覆盖 flux 编译约束（renderer 类型存在、表达式可编译等只有 JS 侧编译器能判定）。

## 2. 目标与非目标

### 目标

- 提供通用 Java 导出工具（nop-entropy 内置）：任意 Nop 应用的全部（或指定模块）页面 → flux 页面 JSON 文件 + manifest，导出语义与生产 `getPage` 完全一致（含 flux.yaml 回退）。
- 提供通用 JS 验证 CLI（nop-chaos-flux 内置）：对导出目录（或任意 flux 页面 JSON/YAML 文件集）执行 L1 schema 校验 + L2 完整编译，输出诊断与机器可读报告，错误时非零退出。
- nop-app-erp 接入：一条命令完成「导出 → 验证」，产物落 `target/` 供 JS 直接消费。
- 工具对任何 nop 应用可复用，不绑定 ERP 业务。

### 非目标

- 不做浏览器级渲染冒烟（L3）——该职责已由 Playwright E2E（`docs/testing/e2e-runbook.md`）承担。
- 不修改 flux 框架编译器/渲染器本身（发现的框架缺陷走 nop-chaos-flux 自身流程）。
- 不在本切片把 compiler API 打包进 flux-bundle/sdks（见 §8 演进路径）。
- 不替换/重构现有 `ErpXxxWebPagesTest` 的 amis 模式验证。

## 3. 总体架构

```text
┌─────────────────────── Java 层（nop-entropy，新增内置能力）───────────────────────┐
│                                                                                    │
│  WebPageExporter（新，io.nop.web.page）                                             │
│   1.（可选）设置 nop.web.render-mode（如 flux）+ 清 xpage/xlib 组件缓存              │
│   2. 枚举 enabled modules × pattern（默认 pages/*/*.page.yaml）的资源               │
│   3. 逐页渲染（经 PageProvider，含 flux.yaml 回退修复），按资源完整路径落盘           │
│      target/flux-pages/<moduleId>/pages/<dir>/<name>.page.json                     │
│   4. 写 manifest.json（页面清单 + 失败清单 + 元数据）                                │
│                                                                                    │
│  暴露形式：a) Java API（JUnit 测试调用，走 Maven test classpath——ERP 采用）          │
│           b) scripts/render-pages.xrun（nop-cli run 执行，cli.md 文档化路径补齐）     │
└────────────────────────────────────────────────────────────────────────────────┬──┘
                                                                                   │
                                                    target/flux-pages/**/*.page.json + manifest.json
                                                                                   │
┌─────────────────────── JS 层（nop-chaos-flux，新增内置 CLI）───────────────────────┴──┐
│  flux-guide/scripts/validate-pages.mjs                                             │
│   1. 组装完整 RendererRegistry（九个 flux-renderers-* 包，与 validate.mjs 同源）      │
│   2. 扫描输入目录/文件（默认 **/*.page.json）                                        │
│   3. 每页：JSON.parse → validateSchema（L1）→ createSchemaCompiler().compile（L2）    │
│   4. 汇总诊断（ERR/WARN/INFO + 文件 + JSON path），可选写 JSON 报告                   │
│   5. error 数 > 0 → exit 1                                                          │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

两段以**文件系统目录**为契约边界：Java 侧只保证「按资源路径落盘 + manifest」；JS 侧只依赖「目录里有页面 JSON」。两侧可独立演进、独立复用（JS 侧也可直接验证手写 flux 原型页面 JSON，如 nop-chaos-next `prototypes/flux-demo/pages/`）。

## 4. 关键设计决策

### D1 导出必须与生产 `getPage` 同语义（含 flux.yaml 回退与解析器选项）

**决策**：两层对齐：

1. 修复 `PageProvider.renderPageTo`/`renderPage`，在 flux 模式下应用与 `PageModelLoaderFactory` 相同的资源回退（`WebPageHelper.toFluxPagePath` + 存在性探测），使 `renderPagesTo` 的输出与浏览器经 `PageProvider__getPage` 收到的 JSON 一致。输出文件名仍按**原始 page.yaml 路径**命名（页面身份稳定，不因回退改名）。
2. `WebPageExporter` 的缺省解析器选项对齐生产：`useResolver=true`、`resolveI18n=true`、`transformPermissions=true`（生产在 `rolePermissionMapping` 非空时恒做权限转换；null 时自动跳过，行为等价）。locale 缺省 `AppConfig.defaultLocale()`，调用方可显式钉死（ERP 钉 `zh-CN`，与 E2E 生产 dump 会话一致）。

**替代方案**：导出器改调 `pageProvider.getPage(path, locale)`（走组件缓存与 loader 全链路）。否决理由：`renderPage` 支持 `useResolver/resolveI18n/postProcess` 等导出特有选项，`getPage` 不支持；且组件缓存 key（`locale|path`）不含 render mode，在同 JVM 先后以两种模式导出时需额外清缓存，直接回退资源更可控。

**理由**：工具的存在价值取决于「验证的就是线上将渲染的」；绕过回退会让 31 个手写 flux.yaml 页面的验证对象错误，缺省 false 的解析器选项会让全部含 `@i18n:` 的页面导出失真——两类偏差都无告警。

### D2 导出编排器 `WebPageExporter`（而非散装脚本）

**决策**：在 nop-entropy `io.nop.web.page` 包新增 `WebPageExporter`（同包可复用 `PageProvider` 的 protected 渲染方法），职责：

1. 可选切换 `nop.web.render-mode` 并清 `xpage`/`xlib` 缓存（切换顺序：先改配置再清缓存——与 `TestFluxYamlPages` 既有实践一致）；
2. 按 **enabled modules**（`ModuleManager.instance().getEnabledModules(true)`）× pattern 枚举资源——与 `validateAllPages()` 同集合语义，而非 `findAll("/")` 全 VFS 扫描（避免纳入未启用模块）；
3. 逐页渲染并按资源完整路径写 `targetDir` 下（复用 `renderPageTo` 落盘规则）；**缺省选项按 D1.2 对齐生产语义**（`useResolver=true`、`resolveI18n=true`、`transformPermissions=true`）；**缺省 `threadCount=1`**——与 `validateAllPages` 及全部既有绿色路径一致（`CFG_WEB_PAGE_VALIDATION_THREAD_COUNT` 缺省 1）；`threadCount>1` 的并发导出为本工具支持但非缺省的实验性形态（并发 + flux 生成的组合在两仓历史中从未运行过，启用前须以小 fixture 验证）；
4. **失败收集而非 fail-fast**：单页异常记录后继续，结束返回 `ExportResult`（成功清单 + 失败清单 `<path, errorCode, message>`）——沿用 `ErpAllWebPagesCollectTest`/`ErpAllFluxPagesTest` 已验证的诊断形态，供调用方一次看到全部坏页；
5. 写 `manifest.json`：`{ generatedAt, renderMode, pattern, locale, pageCount, pages[], failedPages[] }`——JS 侧与 CI 可交叉核对（防止「导出 0 页却绿灯」）。`generatedAt` 仅供人读，任何 diff 式回归消费必须排除该字段。

**替代方案**：仅升级 demo xrun 脚本在 XPL 里散装编排。否决理由：render-mode 切换 + 缓存清理 + manifest + 失败收集 + 生产语义选项这些「每次导出都必须正确」的逻辑放脚本里必然漂移；脚本应是 exporter 的薄包装（见 J3）。

### D3 JS 验证器放 nop-chaos-flux（而非 nop-chaos-next / nop-app-erp）

**决策**：`flux-guide/scripts/validate-pages.mjs` 落在 nop-chaos-flux 仓库，与 `validate.mjs` 共享 registry 组装模块。

**理由**：

1. `@nop-chaos/flux-compiler` / `flux-core` / `flux-formula` 是 **private workspace 包，从未发布 tgz**；唯一能直接 import 其 dist 的位置就是 flux 仓库本身。nop-chaos-next 只有不含 compiler API 的 flux-bundle tgz。
2. 符合本仓既有教义（e2e-runbook「flux 控件/渲染器能力先进 nop-chaos-flux，再同步回来」）。
3. `css-stub.mjs`（`.css` import 与 `@atlaskit` CJS 子路径 stub）与 `env-stub.mjs`（`leaferui` 模块作用域触碰 `window`/`document`/`Path2D` 等）两个 Node 加载桩已在该目录存在，验证器必须复用。
4. 验证保真度 = 与 flux 仓库自身测试套件使用**同一份 dist 与 registry**，避免「验证用的编译器与线上 bundle 行为不一致」。

**替代方案**：扩展 flux-bundle 导出 compiler API 并 repack tgz 到 nop-chaos-next libs，验证器放 next。否决（本切片）：需走完整 repack 链（build → pack → import-flux-to-libs → pnpm install），成本与风险高，作为演进路径（§8）。

### D4 导出验证合并到 WebPagesTest（codegen 模板 + 聚合层）

**决策**：codegen 模板 `{moduleClassPrefix}WebPagesTest.java.xgen` 生成的测试类直接调用 `WebPageExporter.exportPages()` 完成 flux 模式验证 + JSON 导出（一步完成，不拆分两个测试类）。聚合层 `ErpAllFluxPagesTest` 使用相同模式，作为跨域全量验证入口。

**实现方式**：

- codegen 模板（nop-entropy `WebPagesTest.java.xgen`）：`options.setRenderMode("flux")` + `WebPageExporter.exportPages()` → 零失败 = 页面结构合法 + 导出产物就绪
- `ErpAllFluxPagesTest`（app-erp-all）：同构逻辑，覆盖全模块聚合场景
- 删除了独立的 `ErpAllFluxPagesExportTest`（功能已合并）

**理由**：

1. `exportPages()` 内部逐页调用 `renderPageTo()` → `getPage()`，失败则收集到 `failedPages`——导出成功即证明页面可加载、可编译，验证与导出是同一操作的两面
2. codegen 模板覆盖每个域模块的本地验证，聚合层覆盖跨域全量验证，两层互补

### D5 目录与产物约定

| 项 | 约定 |
| --- | --- |
| 导出根目录 | `<导出模块>/target/flux-pages/`（ERP：`app-erp-all/target/flux-pages/`） |
| 页面文件路径 | 资源完整路径映射：`/erp/pur/pages/ErpPurOrder/main.page.yaml` → `target/flux-pages/erp/pur/pages/ErpPurOrder/main.page.json`（无前导斜杠，扩展名复合替换为 `.json`） |
| manifest | `target/flux-pages/manifest.json`（D2 结构） |
| JS 验证默认 pattern | 目录递归 `**/*.page.json` |
| 验证报告 | 可选 `--report=<file>`（JSON：files/validated/errors/warnings/perFile[]） |

### D6 验证层级定义（无浏览器可达 L2）

| 层级 | 内容 | 实现 | 能抓到的缺陷类别 |
| --- | --- | --- | --- |
| L0 | 文件可读、JSON 可解析、根节点是可校验的页面 schema（Map 且 type 为已注册 renderer，或合法数组根） | 前置检查 | 导出产物损坏、截断、非页面内容 |
| L1 | schema 结构校验（**内嵌编译诊断**） | `validateSchema({schema, registry})` → diagnostics | 未知 renderer type、非法属性结构、region 嵌套错误、`${}` 表达式编译失败（validateSchema 内部调用编译并收集其诊断） |
| L2 | 编译抛错防御 | `createSchemaCompiler({registry}).compile(schema)`（try/catch，仅捕获**异常**，code 标记 `compile-throw`） | validateSchema 未降级为诊断而直接抛出的编译期失败 |
| L3 | 渲染冒烟 | （非本工具链）Playwright E2E / happy-dom | 运行时行为、取数、交互 |

语义约束：

- **L1/L2 不重复计数**：validateSchema 的诊断与 compile 的异常按 `(jsonPath, message)` 去重——compile 抛出的异常仅在其诊断集未覆盖时计一次 `compile-throw` error。`--level=validate` 跳过 L2，`--level=compile`（缺省）两者都跑。
- **禁止静默跳过**：`validate.mjs` 对文档示例的跳过语义（null/非对象/未知 type 根直接 return、`$slot.`/`$Arr.`/`xui:roles` 含即跳过整块）**不适用于导出产物**——L0 不合格即 error 并非零退出；运行时命名空间引用（`$slot.`/`$Arr.`）属合法 schema 内容不跳过（若实测产生误报，按 Phase 4 证据窄化并回写本条）。

### D7 已知语义噪音的预处理（xui:* 与运行时命名空间）

导出的页面 JSON 可能含 `xui:roles`（`transformPermissions` 在配置了 `rolePermissionMapping` 时注入；ERP 聚合导出环境实测 9 个页面出现）等**壳层约定键**。它们由 nop-chaos-next 壳层的页面加载期转换消费（`packages/amis-core/src/page/transform.ts` 的 `transformPageJson`：读取 `xui:roles` 过滤节点、`xui:component` 展开替换节点），不是 flux compiler 的已知属性，裸校验会产生误报（`validate.mjs` 对文档示例直接跳过含此类键的块）。验证器采用**预处理剥离 `xui:*` 键**后校验（一次性深遍历删除），并在报告中注明剥离计数。注意 `xui:component` 剥离后验证对象与生产行为存在差异（壳层会展开该节点），若导出产物中出现 `xui:component`，按 Phase 4 证据决定是否改为展开语义。`$slot.*`/`$Arr.*` 引用是合法的运行时命名空间，不做跳过（若实测出现误报再按证据窄化处理并记录）。

## 5. Java 导出工具详细设计（nop-entropy）

### 5.1 变更清单

| # | 变更 | 类型 | 位置 |
| --- | --- | --- | --- |
| J1 | `renderPageTo`/`renderPage` 应用 flux 模式资源回退（与 `PageModelLoaderFactory` 对齐） | Fix | `PageProvider.java` |
| J2 | 新增 `WebPageExporter` + `PageExportOptions`（生产语义缺省，见 D1.2/D2；含 manifest 与失败收集） | Add | `io.nop.web.page` |
| J3 | 升级 `nop-runner/nop-cli/demo/scripts/render-pages.xrun` 为 exporter 薄包装（**改造既有 demo 脚本而非另立新脚本**，避免双轨漂移；`TestNopCli.testRenderPages`/`testParameter` 随脚本走） | Add/Refactor | `nop-runner/nop-cli/demo/scripts/` |
| J4 | 单元/集成测试：flux 回退等价、manifest 结构、失败收集、`TestNopCli` 既有渲染脚本测试回归 | Add | `nop-web/src/test/` + `nop-cli-core` 既有测试复跑 |
| J5 | `docs/dev-guide/cli.md` 更新（xrun 实参数、render-mode、manifest、classpath 要求） | Add | 文档 |
| J6 | `ai-dev/logs/` 日志（引用本仓跨仓计划路径；说明跨仓主计划挂 ERP 仓、熵仓不另建仓内计划） | Add | nop-entropy 自身规范 |

### 5.2 API 草案

```java
// io.nop.web.page.WebPageExporter
public class WebPageExporter {
    /**
     * @param options moduleId/pattern/locale/renderMode/useResolver/resolveI18n/transformPermissions/threadCount
     *                （缺省：生产语义——useResolver=true, resolveI18n=true, transformPermissions=true,
     *                  locale=AppConfig.defaultLocale(), threadCount=1；见 D1.2/D2）
     * @param targetDir 导出根目录
     * @return ExportResult：pages（相对路径清单）、failedPages（path/errorCode/message）、manifest 落盘路径
     */
    public ExportResult exportPages(PageExportOptions options, File targetDir);
}
```

要点：

- `renderMode` 非空时：`updateConfigValue(CFG_WEB_RENDER_MODE, renderMode)` → `clearCache("xlib")` + `clearCache("xpage")`；导出后**不自动恢复**（调用方负责，测试用 `@AfterEach` 恢复 amis 并再次双清——与既有 flux 测试一致的卫生协议）。
- 失败收集线程安全（`ConcurrentLinkedQueue`），即使 `threadCount=1` 缺省亦走同一收集路径。
- locale：缺省 `AppConfig.defaultLocale()`，与 `validateAllPages()` 一致；ERP 显式钉 `zh-CN`。

### 5.3 正确性边界（自检清单）

- [ ] flux 回退：J1 后，含 `*.flux.yaml` 孪生的页面导出内容 == `getPage(path)` 在 flux 模式下的返回。
- [ ] 输出路径 = 资源路径复合扩展名替换；不因回退改名。
- [ ] manifest 计数与实际文件数一致；failedPages 非空时调用方可见完整清单。
- [ ] 不修改任何 `_` 前缀生成文件；不引入对 nop-autotest 的 main 作用域依赖（exporter 可在纯 main classpath 运行，JUnit 只是消费方）。

## 6. JS 验证工具详细设计（nop-chaos-flux）

### 6.1 变更清单

| # | 变更 | 类型 | 位置 |
| --- | --- | --- | --- |
| S1 | 抽取 `flux-guide/scripts/flux-registry.mjs`（registry 组装 + stubs 说明），`validate.mjs` 改为复用 | Add/Refactor | flux-guide/scripts/ |
| S2 | 新增 `flux-guide/scripts/validate-pages.mjs`（通用 CLI，契约见 §6.2） | Add | 同上 |
| S3 | 根 `package.json` 增 `flux:validate-pages` script（带双桩启动参数） | Add | 根 |
| S4 | 按该仓 AGENTS.md 的 CODE 变更纪律运行其检查（typecheck/lint；若 flux-guide/scripts 不在检查范围，记录说明）+ 仓库自身日志 | Add | 其 docs/ |

### 6.2 CLI 契约

```bash
node --experimental-loader ./flux-guide/scripts/css-stub.mjs \
     --import ./flux-guide/scripts/env-stub.mjs \
     flux-guide/scripts/validate-pages.mjs <dir-or-file>... \
     [--pattern='**/*.page.json'] [--level=compile|validate] \
     [--report=<file>] [--max-errors=<n>]

# 或经 package.json script：
pnpm flux:validate-pages -- <dir>...
```

行为：

1. 递归收集输入下匹配 pattern 的 `.json`（`--pattern` 可扩展至 `.yaml`——`flux.yaml` 手写源直验为增量能力，仅当引入 YAML 解析依赖时启用；本切片 JSON-only，YAML 源经 Java 导出归一化后覆盖）。
2. 每文件：parse → **L0 前置检查（根必须是可校验页面 schema，不合格即 error，禁止静默跳过）** → 剥离 `xui:*`（D7，计数入报告）→ `validateSchema`（L1，含内嵌编译诊断）→ `--level=compile` 时追加 `compile()` 抛错防御（L2，`(jsonPath, message)` 去重，见 D6）。
3. 输出：每诊断一行 `LEVEL <file> <jsonPath> <message>`；尾部 summary（files/validated/errors/warnings/skipped/strippedXui）；`--report` 写机器可读 JSON。
4. 退出码：任何 error 级诊断、L0 不合格或解析失败 → 1；仅 warning → 0。
5. registry 组装失败（dist 缺失）→ 明确报错提示先 `pnpm build`，退出 2。

### 6.3 保真度与自举约束

- registry 组装必须与 `validate.mjs`/flux 仓测试同源（S1 抽取共享模块即为此），新增 renderer 包时两处同步。
- 不引入浏览器全局依赖：`env-stub` 仅满足模块作用域引用（与 flux-guide 既有 `validate`/`generate-types` 相同边界）。
- 自举验证（验证器自身的正确性证明）须沿用 `validate.mjs` 的 skipPackages 排除集（designer/report/spreadsheet 等未注册进九包 registry 的包），或使用精选 fixture——直接跑全集必产生已知误报。

## 7. nop-app-erp 接入设计

| # | 变更 | 位置 |
| --- | --- | --- |
| E1 | `ErpAllFluxPagesTest`（app-erp-all）+ codegen 模板 `*WebPagesTest`：flux 模式导出全部 enabled modules 页面到 `target/flux-pages/`；断言 `failedPages` 为空且 `pageCount > 0` | `app-erp-all/src/test/java/io/nop/app/all/web/` + codegen 模板 |
| E2 | `scripts/validate-flux-pages.sh`：① 跑 E1（`mvn -pl app-erp-all test -Dtest=ErpAllFluxPagesTest`）② 定位兄弟目录 nop-chaos-flux，校验 dist 存在（缺失则提示 `pnpm build`）③ 运行 validate-pages.mjs（报告落 `_tmp/flux-page-validation-report.json`）④ 透传退出码 | `scripts/` |
| E3 | 根 `package.json` 增 `"validate:flux": "bash scripts/validate-flux-pages.sh"` | 根 |
| E4 | `docs/testing/e2e-runbook.md`「渲染模式」节增补一节：工具链用法 + 与三路径的关系（此为第 0 路径：写 E2E 之前的静态门禁） | 文档 |
| E5 | 日志 `docs/logs/2026/08-30.md` | 日志 |

调用链验收形态：

```bash
npm run validate:flux
# → mvn 导出（JUnit）→ app-erp-all/target/flux-pages/*.page.json + manifest.json
# → node validate-pages.mjs → console 诊断 + _tmp/flux-page-validation-report.json
# → step[1/3]（导出）exit 0：FLUX_PAGE_ERROR_COUNT 0 / pageCount=999 / erpPages=855
# → 整体 exit 1 当存在 error 级诊断；当前 325 条既有 `variant="primary"`×dropdown-button
#   codegen stub 外部漂移族（non-variant=0）已裁决 successor 在案（ai-check-r3 roadmap MI.8 行），
#   门禁口径 = 零新增 error（对照 325 基线），全链 exit 0 待漂移族清零后恢复（P3-CK-app-002-r3 修订登记）
```

## 8. 与 nop-chaos-next sdks 的关系与演进路径

现状：`dist/sdks/` 的 `nop-chaos-flux-0.1.0.tgz` 只导出渲染 facade（`createFluxSchemaRenderer` 等），**不含 compiler API**；本切片的 JS 验证因此落在 flux 仓（D3）。演进（登记为 Deferred，触发条件：出现第二个需要本地验证 flux 页面的下游工程，或 ERP 需在无 flux 仓 checkout 的环境跑验证）：

1. nop-chaos-flux：flux-bundle `src/index.ts` 增导出 `validateSchema`/`createSchemaCompiler`（registry 已随 bundle 组装）；
2. 走既有 repack 链刷新 `libs/nop-chaos-flux-*.tgz`（`scripts/import-flux-to-libs.sh`）；
3. 验证器（或其薄包装）进 nop-chaos-next 并入 `dist/sdks/`，下游以 npm 依赖消费。

## 9. 风险与缓解

| 风险 | 影响 | 缓解 |
| --- | --- | --- |
| zulu-26/ANTLR H-2 不稳定波及页面测试 | 页面测试闪烁 | 同环境 `ErpAllWebPagesTest` 基线稳定；若复现 H-2，按 `docs/bugs/2026-07-20-2200-page-error-count-instability.md` 重启用条件处理，并退回「逐模块导出」备选（工具已支持 moduleId 过滤） |
| flux 仓 dist 未构建/过期 | 验证器无法运行或验证旧编译器 | wrapper 前置校验 dist 存在性并提示 `pnpm build`；验证器报错退出码 2 区分环境错误与页面错误 |
| 渲染器定义包模块作用域触碰浏览器全局 | Node 加载即崩 | 复用 `css-stub`/`env-stub` 双桩（`validate.mjs` 已实证） |
| ERP 存量页面暴露真实 flux 编译错误 | 验证红灯 | 这是工具目的而非缺陷：发现项分级处置（页面 schema 问题修页面；框架问题走 nop-chaos-flux 流程；本仓记录于日志/bugs）。首批运行允许以「报告产出 + 已知问题清单」作为阶段性验收，修复归后续切片，但**不得**为绿灯放宽校验层级或跳过页面 |
| `xui:*`/运行时命名空间误报 | 验证噪音 | D7 预处理 + 报告计数；出现新噪音类别按证据窄化并回写本文件 |
| 导出与生产的隐性分歧（未来新增 getPage 后处理） | 验证对象失真 | D1 语义约束落在 `WebPageExporter` 单点；nop-entropy 侧测试守护回退等价性 |
| 双仓（nop-entropy/nop-chaos-flux）修改触保护区域 | 流程违规 | 跨仓库 plan + 两个独立子 agent 分别批准（记录于计划文件），三仓各自日志 |

## 10. 验证策略

1. **nop-entropy 单元级**（J4）：flux 回退等价（导出文件内容 == flux 模式 `getPage`）、manifest 结构、失败收集语义。
2. **nop-chaos-flux 单元级**（S2 自举）：对 `flux-guide` 已知合法示例跑验证器必须 0 error（与 `pnpm flux-guide:validate` 结论一致）；构造含未知 renderer/坏表达式的 fixture 必须 error 且 exit 1。
3. **ERP 端到端**（E2）：`npm run validate:flux` step[1/3]（导出）exit 0（`FLUX_PAGE_ERROR_COUNT: 0 pageCount=999 erpPages=855`）；整体退出码当前为 exit 1，余项 = 325 条既有 `variant="primary"`×dropdown-button codegen stub 外部漂移族（successor 在案，roadmap MI.8 行；门禁口径 = 零新增 error 对照 325 基线，全链 exit 0 待漂移清零——P3-CK-app-002-r3 修订）；manifest 页面数与 `find module-* -name '*.page.yaml' | wc -l`（855）+ 平台模块页面数对账；31 个手写 flux.yaml 页面抽样比对导出内容与 `dumpPageSchemaToFile`（e2e fixture）抓取的生产 schema 一致。
4. **回归**：nop-entropy `mvn install` 后 ERP 全量 `mvn clean install -DskipTests` 绿 + app-erp-all 相关测试绿；`bash docs/audits/nop-compliance-checker.sh` 零漂移。

## 11. 参考

- `docs/architecture/view-and-page-strategy.md` — 渲染模式（flux-only）与页面数据访问基线
- `docs/architecture/flux-integration-gotchas.md` — flux 集成陷阱目录（本工具链产出的发现按其四段式登记）
- `docs/testing/e2e-runbook.md` —「渲染模式与 flux 调试三路径」（本工具链为其前置静态门禁）
- `../nop-entropy/docs/dev-guide/cli.md` —「根据 page.yaml 文件生成页面 json 文件」（本设计 J3/J5 使其落地）
- `../nop-chaos-flux/flux-guide/scripts/validate.mjs` — Node 批量 validateSchema 的既有实证
- `../nop-chaos-flux/flux-guide/13-testing.md` — flux 测试设施与严格校验开关
