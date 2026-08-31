# 2026-08-30-1126-1-flux-page-export-and-validation Flux 页面批量导出与编译验证工具链（三仓）

> Plan Status: completed
> Last Reviewed: 2026-08-30
> Source: 用户请求（/goal：通用工具验证生成的 flux 页面符合 flux 框架要求；Java 导出 + JS 验证；工具内置 nop-entropy；nop-app-erp 验证）
> Related: 设计文档 `docs/architecture/flux-page-export-and-validation.md`（本计划的架构契约 owner doc）
> Audit: required

## Current Baseline

- 设计文档已落地：`docs/architecture/flux-page-export-and-validation.md`（决策 D1–D7，本计划是其执行切片）。
- Java 侧现状（nop-entropy `@<local checkout>`）：
  - `PageProvider.renderPagesTo(PageRenderOptions, File)` 的既有调用方是 `nop-runner/nop-cli/demo/scripts/render-pages.xrun`（demo 脚本），由 `TestNopCli.testRenderPages`/`testParameter`（`nop-runner/nop-cli-core/src/test/java/io/nop/cli/TestNopCli.java:236,252`）在 CI 执行——**改造 `renderPage` 行为与升级脚本均波及该回归面**。
  - `renderPage` 直接 `loadPage(page.yaml 资源)`，**绕过** `PageModelLoaderFactory` 的 flux.yaml 回退（`PageModelLoaderFactory.java:38-46`）。
  - `PageRenderOptions` 的 `useResolver/resolveI18n/transformPermissions` 缺省全 `false`，与生产 `getPage`（恒定 `resolveI18n=true` + 解析器启用 + 权限转换）**相反**——缺省导出会保留未解析 `@i18n:` 串（ERP view.xml 生成链大量使用）。
  - 渲染模式切换卫生协议有既有范本：`TestFluxYamlPages`（切 flux → clearCache xlib+xpage → `@AfterEach` 恢复 amis 再双清）。
  - 组件缓存 key `locale|path` 不含 render mode（切模式必须清 xpage；web.xlib 编译产物含模式分支必须清 xlib）。
- JS 侧现状（nop-chaos-flux `@<local checkout>`，pnpm+turbo monorepo，全 ESM，node v25.3.0，dist 已构建）：
  - `flux-guide/scripts/validate.mjs` 已实证 Node 无浏览器批量 `validateSchema`（九个 renderer 包组装 registry；该仓 `flux-guide/package.json` 的 `validate` script 以 `css-stub.mjs` loader + `env-stub.mjs` import 双桩运行）。
  - `validate.mjs` 的包装层对 null/非对象/未知 type 根**静默跳过**（对文档示例合理，对导出产物是绿灯漏洞，S2 禁止沿用）；`validateSchema` 内部已调用编译并收集编译诊断（L1 内嵌 L2 诊断，须去重）。
  - `@nop-chaos/flux-compiler` 为 private workspace 包（`package.json` `private: true`，从未发布 tgz）；flux-bundle tgz 不导出 compiler API（sdks 演进为 Deferred）。
- ERP 侧现状（本仓）：
  - 页面基线：19 模块共 855 个 `.page.yaml`（全部 `pages/<dir>/<file>` 两层结构），31 个手写 `.flux.yaml` **全部有 page.yaml 孪生**（flux 回退可全覆盖）；736 个 `.view.xml` 经 GenPage 引入。
  - `app-erp-all` 聚合层已有两个全量页面测试且基线绿（2026-08-28：3947 tests / 0 failures / 1 skipped）：`ErpAllWebPagesTest`（amis 模式 `validateAllPages`）与 **`ErpAllFluxPagesTest`（flux 模式全页 `getPage` 断言零错误——加载正确性已验证，E1 是其产物导出互补而非替代）**；`ErpAllWebPagesCollectTest` 因 zulu-26/ANTLR H-2 `@Disabled`。
  - 根 `package.json` 存在（playwright e2e scripts）；`_tmp/` 已 gitignore。
- 环境约束：nop-entropy / nop-chaos-flux / nop-chaos-next 均为本仓 ai-autonomy-policy 保护区域（`auto + dual-agent-approval`：跨仓库 plan + 两个独立子 agent 分别批准，记录落盘本文件）。

## Goals

- nop-entropy 内置 Java 导出工具：flux 回退修复（J1）+ `WebPageExporter` 编排器（J2）+ `render-pages.xrun` 落地（J3）+ 平台测试（J4）+ cli.md 更新（J5）。
- nop-chaos-flux 内置 JS 验证 CLI：`flux-registry.mjs` 抽取（S1）+ `validate-pages.mjs`（S2）+ package.json script（S3）。
- nop-app-erp 接入：`ErpAllFluxPagesExportTest`（E1）+ `scripts/validate-flux-pages.sh`（E2）+ npm script（E3）+ e2e-runbook 增补（E4）。
- 端到端验证：`npm run validate:flux` 全链 exit 0（或产出分级发现清单，见 Phase 4 验收口径）。

## Non-Goals

- 浏览器渲染冒烟（L3）、flux 框架编译器/渲染器修改、compiler API 打包进 flux-bundle/sdks（§8 Deferred）。
- YAML 源直验（Java 导出已归一化为 JSON 覆盖 `.flux.yaml` 源）。
- ERP 存量页面发现问题的**修复**（本计划止于发现 + 分级登记；修复归后续切片）。
- 逐模块（19 个 erp-*-web）导出接线（工具支持 moduleId 过滤，聚合层优先，见设计 D4）。

## Task Route

- Type: `architecture change`（跨仓库工具链；不含业务行为/模型变更）
- Owner Docs: `docs/architecture/flux-page-export-and-validation.md`（本切片架构契约）；`docs/architecture/view-and-page-strategy.md`（渲染模式基线）；`docs/testing/e2e-runbook.md`（接入文档）
- Skill Selection Basis: 实现阶段无匹配技能（平台工程，非业务切片，遵循 nop-entropy `docs-for-ai/` 与各仓自身规范）；审计阶段用 `plan-audit-prompt.md`（草案审查）与 `closure-audit-prompt.md`（结束审计）。

## Infrastructure And Config Prereqs

- nop-entropy 本地 checkout 可构建（改动后 `mvn install` 刷新本地 m2，ERP 才能消费新 `nop-web`）。
- nop-chaos-flux 本地 checkout dist 已构建（已核实存在）；若缺失 wrapper 提示 `pnpm build`。
- node ≥ 20（本机 v25.3.0）、pnpm（flux 仓 workspace）。
- 无数据库/端口/密钥新增依赖。

## Execution Plan

### Phase 1 - nop-entropy：Java 导出工具（J1–J6）

Status: completed
Targets: `../nop-entropy/nop-frontend-support/nop-web/src/main/java/io/nop/web/page/`、`../nop-entropy/nop-frontend-support/nop-web/src/test/java/io/nop/web/page/`、`../nop-entropy/nop-runner/nop-cli/demo/scripts/render-pages.xrun`、`../nop-entropy/docs/dev-guide/cli.md`、`../nop-entropy/ai-dev/logs/`
Skill: none

- Item Types: `Fix | Add | Proof`
- Prereqs: 双独立子代理批准（保护区域门控，见 Draft Review Record——已落盘）

- [x] Fix：`PageProvider.renderPageTo`/`renderPage` 应用 flux 模式资源回退（`WebPageHelper.isFluxMode()` + `toFluxPagePath` + 存在性探测，与 `PageModelLoaderFactory` 同语义；输出文件名仍按原始 page.yaml 路径）
- [x] Add：`PageExportOptions` 与 `WebPageExporter.exportPages(options, targetDir) → ExportResult`（enabled-modules 枚举、render-mode 切换 + xlib/xpage 双清、线程安全失败收集、manifest.json 落盘、不自动恢复 render-mode；**缺省生产语义**：`useResolver=true`、`resolveI18n=true`、`transformPermissions=true`、`locale=AppConfig.defaultLocale()`、**`threadCount=1`**——`threadCount>1` 为实验性，本切片不默认启用）
- [x] Add：升级 `nop-runner/nop-cli/demo/scripts/render-pages.xrun` 为 `WebPageExporter` 薄包装（改造既有 demo 脚本，**不**另立新脚本；`renderMode` 经 `$scope.containsValue` 判存传递——XLang 对未定义 scope 变量直接抛 `scope-var-is-undefined`；`-i`/`-o` 参数保持兼容）
- [x] Add：cli.md 更新 render-pages 节（xrun 实参数、render-mode、manifest、classpath 要求：业务模块 `_vfs` 须在 classpath / repackage 打包）
- [x] Proof：`TestWebPageExporter` 4/4（(a) flux 回退等价：导出内容与 `getPage(path,'zh-CN')` 全等；(b) manifest 结构与计数；(c) 失败收集；(d) threadCount=2 与串行一致）；`TestNopCli#testRenderPages+testParameter` 2/2（需 `-Djunit.jupiter.conditions.deactivate` 激活，类级 `@Disabled` 常规跳过——已核实干净 target 下真实导出 pageCount=1）；nop-web 全量 91/0/1
- [x] Add：nop-entropy `ai-dev/logs/2026/08-30.md`（引用跨仓计划、含验证状态与 standalone dev-mode 类加载器边界记录）

Exit Criteria:

- [x] `mvn -pl nop-frontend-support/nop-web -am install -DskipTests` 成功（m2 jar 12:11 刷新）
- [x] J4 测试通过（回退等价 / manifest / 失败收集 / TestNopCli 回归四点均有断言证明）

### Phase 2 - nop-chaos-flux：JS 验证 CLI（S1–S4）

Status: completed
Targets: `../nop-chaos-flux/flux-guide/scripts/flux-registry.mjs`、`../nop-chaos-flux/flux-guide/scripts/validate-pages.mjs`、`../nop-chaos-flux/flux-guide/scripts/validate.mjs`（改为复用共享 registry）、`../nop-chaos-flux/package.json`
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 无依赖，可与 Phase 1 并行；保护区域门控同上

- [x] Add：registry 组装抽取——落地为 `shared.mjs` 新增 `buildRegistry(packages?)` + `buildFullRegistry()`（该文件已有 `loadRegisteredDefinitions()`，抽取在其上完成；`validate.mjs` 改用 `buildRegistry` 并**保持历史 9 包集合**，避免改变其校验输出可比性；全量 11 包归 validate-pages.mjs）
- [x] Add：`validate-pages.mjs`（设计 §6.2 CLI 契约：目录/文件输入、`--pattern`、`--level=compile|validate`、`--report`、`xui:*` 剥离计数（D7）、L0 禁止静默跳过、L1/L2 诊断 `(jsonPath,message)` 去重 + `compile-throw`、error→exit 1、环境错误→exit 2；实现中发现并修复：块注释内 `**/` 提前闭合、glob 末段匹配文件名语义、pnpm `--` 透传、剥离计数 NaN）
- [x] Add：根 package.json `flux:validate-pages` script（`css-stub` loader + `env-stub` import 双桩）
- [x] Proof：自举验证——(a) 金标集 87 文件（example.json 严格校验干净子集 85 + 手写 2 含 xui 剥离用例）0 error 且 exit 0、strippedXui=1；(b) 坏 fixture 4 类（未知 type/坏表达式 `${1+}`/非法 JSON/非对象根）各 1 error 且 exit 1。附实证：未闭合 `${1+` 是纯文本非表达式标记（探针脚本）
- [x] Add：仓库检查纪律——`prettier --check` 新增文件合规（`validate.mjs`/`shared.mjs` 告警经 stash 对照确认预存）；`pnpm flux-guide:validate` 的 `pnpm build` 前缀因本 checkout **预存** TS 错误失败（`flux-renderers-content/src/markdown.tsx` TS2339 `ApiResponse.ok`，HEAD `01770f770` 移除 `ok` 后未迁移），改为直跑 `validate.mjs` 对照：与 git HEAD 原版输出**完全一致**（blocks=473 nodes=459 errors=37 warnings=76，37 error 为预存）；`flux-renderers-industrial` dist 被失败 build 清理后已单独重建；仓库日志 `docs/logs/2026/08-30.md` 已登记（含预存状态 3 项供跟进）

Exit Criteria:

- [x] 自举验证 (a)(b) 通过；validate.mjs 与原版输出一致（无回归）；prettier 新增文件合规、既有告警未扩大
- [x] 仓库自身日志已登记（`docs/logs/2026/08-30.md`，含预存 build 失败与 example 松弛示例登记）

### Phase 3 - nop-app-erp：接入与端到端（E1–E5）

Status: completed
Targets: `app-erp-all/src/test/java/io/nop/app/all/web/ErpAllFluxPagesExportTest.java`、`scripts/validate-flux-pages.sh`、`package.json`、`docs/testing/e2e-runbook.md`、`docs/logs/2026/08-30.md`
Skill: none

- Item Types: `Add | Proof | Decision`
- Prereqs: Phase 1（nop-web 已 install 到本地 m2）、Phase 2（validator 可用）

- [x] Add：`ErpAllFluxPagesExportTest`（`@NopTestConfig(initDatabaseSchema=TRUE)`，与 `ErpAllWebPagesTest` 同环境；renderMode=flux、locale 钉 `zh-CN`、生产语义选项导出全部 enabled modules → `getTargetDir()/flux-pages`；断言 failedPages 空 && pageCount>800；`@AfterEach` 恢复 amis + xlib/xpage 双清；Javadoc 写明与既有 `ErpAllFluxPagesTest`（加载正确性）的职责边界并交叉引用）
- [x] Add：`scripts/validate-flux-pages.sh`（四步：mvn 导出 → manifest 存在性校验 → flux 仓 13 包 dist 就绪检查 → validate-pages.mjs；报告落 `_tmp/flux-page-validation-report.json`；退出码透传）
- [x] Add：根 package.json `"validate:flux"` script
- [x] Add：e2e-runbook「渲染模式」节增补「flux 页面静态门禁」小节，遵守三条措辞约束（独立轴不改三路径编号 / 双批准门控锚定 / 不触碰 E2E_ENGINE 表述）
- [x] Proof：端到端 `npm run validate:flux` 全链跑通，退出码语义正确（999 文件全部校验，exit 1 = 存在 error 级发现——正确报告形态）；三项对账——(a) manifest.pageCount 999 == 磁盘 `.page.json` 999；(b) manifest `erp/*` 855 == 源清单 `find module-* -path '*src/main/resources/_vfs*' -name '*.page.yaml' | wc -l` 855（排除 target 副本口径）；(c) 3 个手写 flux.yaml 孪生页面（schedule-gantt / calendar / period-close-wizard-main）导出 JSON == `getPage(path,'zh-CN')` 序列化全等，且以**内置断言**形式永久化在导出测试中。（初始验证语料为陈旧 m2 产物，已在「结束审计整改」节披露并以当前工作树语料复验刷新）
- [x] Add：`docs/logs/2026/08-30.md` 日志条目（含三项对账证据与端到端退出码）

Exit Criteria:

- [x] 端到端链路跑通且退出码语义正确（发现项完整报告并进入 Phase 4 分级）
- [x] 三项对账证明记录于日志/计划

### Phase 3 执行中的两项范围外事件（披露）

1. **m2 混装事故与恢复**：master 工作树 `-am install` 把 nop-dao 等内核 jar 刷为 master 版，而 ERP 基线平台 jar 来自 fix-ai-check 分支（`SnowflakeSequenceGenerator` 正确拼写 vs master `SnowflakeSequenceGeneator`，commit 69835f1fb2），导致导出测试 NoClassDefFoundError。处置：Phase 1 变更**同源移植**到 `nop-entropy-wt/nop-entropy-fix-ai-check` 并从该树 `-am install` 恢复 m2（两树该文件族 diff 仅本变更集；fix-ai-check 侧已补日志）。
2. **ERP HEAD 预存编译缺陷 ×2（drive-by Fix，范围外披露）**：`module-quality/erp-qa-service/.../SpcSamplingService.java` 使用 `PageBean` 未 import（补一行 import）；`module-sales/erp-sal-service/.../ErpSalInvoiceProcessor.java:421` 对 Biz 接口误用 DAO 方法 `voucherBiz.getEntityById(...)`（`ICrudBiz` 无此方法）→ 等价改 `voucherBiz.get(id, true, null)`。两处均属 F2.8-F2.12 提交批（08-28 23:49 ~ 08-29 07:46，晚于最后一次全绿基线，HEAD 从未全量源码编译）。Closure Gates 要求全量构建绿，该类已确认实时缺陷不可降级为 follow-up，最小等价修复并披露；全仓其余 1300+ 处 `getEntityById` 均为 DAO 接收者无同类误用。

### 结束审计整改（iteration 1 → 2，2026-08-30）

独立结束审计（iteration 1）裁决 REJECTED，Blocker：`module-notify/.../ErpSysNotification/inbox.page.yaml`（plan 2026-08-29-1913-1 未提交批次）3 处同级 `then:` 键重复（duplicate-key），且初始验证语料是全量构建刷新 m2 **之前**的陈旧 classpath 产物（`-pl app-erp-all` 消费 m2 jar 而非工作树源），Closure Gate 声明不可复现。整改（全部完成）：

1. **Fix（drive-by #3，范围外披露）**：inbox.page.yaml 三处（markAllRead/刷新按钮/行内 markRead 的 after-success 链）同级 `then:` 重复 → 按语料规范（inv dashboard 嵌套链）改为嵌套 then 链；python yaml 重复键检测 NONE。
2. **Proof（刷新语料复验）**：`mvn -pl module-notify/erp-notify-web install` 后重跑 `ErpAllFluxPagesExportTest` → **999 页 / erp 855 / failed 0 / 1/1 绿（当前工作树语料）**，含 3 孪生页面等价断言；重跑 `npm run validate:flux` 全链 → 999 文件全量校验、新报告 1104 error / 41477 warning / strippedXui 9、exit 1（发现即报告）；对账 (a) 磁盘 999 == manifest 999。Phase 4 分级数字已按新语料刷新（如上）。
3. **Add（教训落盘）**：e2e-runbook 静态门禁节补「m2 新鲜度」警示（`-pl` 消费 m2 jar，工作树页面变更后须先 install 对应模块再导出）。
4. M2 整改：cli.md 已补移植到 fix-ai-check 树（该树日志声明与实仓一致）；m1 数字已修正（region 2 + slot 1 + shape 1，不再笼统「7」）；m2 对账命令注明排除 target 副本；m3 master 树构建副产物（nop-vfs-index.txt 等）披露不还原——**审计 r2 归因修正**：`nop-runner/nop-cli-core/src/main/resources/nop-vfs-index.txt` 的 mtime 12:10 为本计划 Phase 1 `-am install` 构建触碰再生（nop-demo 系副产物则会话前 8/28 预存），均为生成物非源码变更。

**残留 Minor（审计 iteration 2 登记，不阻塞）**：r1——invalid-property-value 326 中 23 条（grid `columns` 22 + `hidden` 1）未逐项登记 A/B 去向，归后续 A 类修复切片立项时补齐（报告可完整复原）；r3——已在 plan 2026-08-29-1913-1 的 Notify 模块条目补交接注记。

### Phase 4 - 发现分级与收口

Status: completed
Targets: 本计划文件、`docs/logs/2026/08-30.md`、三仓日志
Skill: `closure-audit-prompt.md`（结束审计由独立子代理执行）

- Item Types: `Decision | Proof | Follow-up`
- Prereqs: Phase 3 完成

- [x] Decision：首跑发现集分级（**数字以结束审计后刷新语料为准**：999 文件、1104 error / 41477 warning、strippedXui=9——初始语料为修复 inbox 坏页前的陈旧 m2 产物 1172/41341，见下方「结束审计整改」）——
  - **A 类（Java 生成管线 amis 泄漏，后续修复切片）**：未知 renderer type 172（static 94 / tpl 45 / code 16 / number 6 / wrapper 4 / panel 4 / _hidden 3——经 registry 实证，验证器 11 包 ⊃ 浏览器 bundle 6 包均无这些类型，浏览器同样无法渲染，**非误报**；初始语料中的 service 53 / button-toolbar 25 在 8/29 重写页进入语料后已消失）；属性枚举越界 303（button.variant=primary 296 + success 1 + container.direction=vertical 6）。分布：ERP 448 文件（855 中）+ 平台页（nop/wf 136 / nop/auth 43 / nop/sys 24 / nop/report 9）。归 nop-entropy flux-web 管线映射补全或页面侧修正。
  - **B 类（编译器规则 vs 运行时容忍，nop-chaos-flux 裁决）**：invalid-reaction-deps 532（`/body/loadAction/dependsOn` 空——运行时容忍、页面数据加载正常，规则是否应对 loadAction 豁免需裁决）、invalid-action-shape 51、unresolved-action-selector 5（plain selector 如 `notify` 无 `xui:actions` 定义）、unhandled-compilation-error 14、invalid-region-node 2 / slot-used-outside-region 1 / invalid-property-shape 1。
  - strippedXui=9 剥离按设计工作；设计文档 D7 措辞已按实测修正。
  - 未以放宽层级或跳过页面换取绿灯（999 文件全量校验，skipped=0）。
- [x] Proof：Closure Gates 全量验证（全量构建/checker/日志——见下）
- [x] Follow-up：sdks 演进（设计 §8）触发条件登记（Deferred 节）

Exit Criteria:

- [x] 发现清单落盘（本节 + 日志），每条有分级与去向
- [x] 设计文档 D7 修正完成，版本一致

### Phase 4 - 发现分级与收口（原草案段，已被上方执行版取代）

## Draft Review Record

- Independent draft review iteration 1（含保护区域双批准，2026-08-30）：
  - **Approver A**（agent_a76674c5-0712-4cfe-8139-2ed74a46e166，fresh session）：NEEDS_REVISION——3 MAJOR + 3 MINOR，全部为基线纠错与验证补强，技术决策 D1–D7 经实地核实成立；声明「修订落盘后转为有条件通过，可作为第一批准记录」。必改项：MAJOR-1 renderPagesTo 既有调用方（demo xrun + TestNopCli）披露与回归、J3 防双轨；MAJOR-2 ErpAllFluxPagesTest 披露与职责边界；MAJOR-3 flux 仓检查纪律；MINOR-1 D7 消费机制措辞（transform.ts 而非 beforeCompile 插件）+ xui:component；MINOR-2 S4/E5 显式化；MINOR-3 J6 证据链。
  - **Approver B**（agent_cae7e776-33d7-4b30-b716-1f42f4b8c86d，fresh session，与 A 互不通信）：REJECT 附 3 必改，声明「修订落盘后本批准记录即为批准凭证」。必改项：P0 导出选项钉死生产语义（useResolver=true/resolveI18n=true/locale/transformPermissions，消除 @i18n 失真）；P1 S2 禁止沿用 validate.mjs 静默跳过语义；P1 L1/L2 诊断去重或互斥。建议项（已采纳）：E1 threadCount=1、E4 三条措辞约束、Phase 1 Exit -am 备注、双桩证据源收窄为 flux-guide/package.json validate script。
  - **修订落盘核对（执行者，2026-08-30）**：上述 MAJOR-1/2/3、MINOR-1/2/3、P0、P1×2、建议×4 已全部修订进本计划与设计文档（`docs/architecture/flux-page-export-and-validation.md` §1/§4 D1/D2/D4/D6/D7、§5.1 J 表、§5.2、§6.1–6.3；本文件 Current Baseline/Phase 1–3/Exit Criteria）。两位批准人的条件均满足，本计划视为已获 dual-agent-approval，Plan Status 转 active。

## Closure Gates

- [x] 范围内行为完成（J1-J6 / S1-S4 / E1-E5 全清单；两处范围外 drive-by 预存编译缺陷已披露并最小等价修复）
- [x] 相关文档对齐（设计文档含 D7 实测修正、e2e-runbook 静态门禁节、nop-entropy cli.md、三仓日志；`docs/index.md` 无需新增路由——设计文档经 e2e-runbook/计划/日志引用可达）
- [x] 已运行验证：nop-entropy `mvn -pl nop-frontend-support/nop-web -am install -DskipTests` + `TestWebPageExporter` 4/4 + `TestNopCli` render-pages 2/2（deactivate 类级 @Disabled）+ nop-web 全量 91/0/1；flux 仓自举金标 87 文件 0 error exit 0 + 坏 fixture 4 类 exit 1 + `validate.mjs` 与 git HEAD 原版输出一致（37/76，预存红非本次引入）+ 新增文件 prettier 合规（`pnpm flux-guide:validate` 因该仓预存 TS 错误无法全跑，已以直跑对照替代并登记其日志）；ERP `npm run validate:flux` 全链（999 文件、三项对账、exit 1=发现即报告的语义正确形态）+ `ErpAllFluxPagesExportTest` 1/1 + 全量 `mvn clean install -DskipTests` BUILD SUCCESS 01:39 + `bash docs/audits/nop-compliance-checker.sh` exit 0 零漂移
- [x] 无范围内项目降级为 deferred/follow-up（Deferred 项均有触发条件登记；首跑发现集 1172 error 是工具产出而非范围内义务，已全量分级登记去向）
- [x] 独立草案审查（含保护区域双批准）已完成并记录（Draft Review Record：Approver A/B 必改项全数落盘）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### compiler API 进 flux-bundle/sdks（设计 §8 演进）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: flux-bundle 不导出 compiler API 是现状事实，本切片以 flux 仓 CLI 达成目标；repack 链成本高且无第二消费方
- Successor Required: yes（触发：出现第二个需本地 flux 页面验证的下游工程，或 ERP 需在无 flux 仓 checkout 环境跑验证）

### YAML 源直验（.flux.yaml 不经 Java 导出直接验证）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: Java 导出已把全部页面（含 flux.yaml 源）归一化为 JSON 覆盖验证；直验需引入 YAML 解析依赖与 x:extends 合并语义（delta-merge），增益有限
- Successor Required: no（若未来原型工作流需要，随 §8 演进一并评估）

## Closure

Status Note: 全部阶段 completed，Closure Gates 全勾。工具链三仓落地并端到端打通（999 页导出 / JS 编译验证 / 三项对账）；首跑发现集 1104 error 已全量分级（A 类 amis 泄漏归后续修复切片、B 类归 nop-chaos-flux 裁决），未绿灯化。执行中三处范围外 drive-by（qa PageBean import、sal getEntityById→get、notify inbox.page.yaml 重复 then 键）均为已确认实时缺陷的最小等价修复并披露。m2 混装事故（master vs fix-ai-check 工作树）以双树同源落地 + 分支树 install 恢复。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理 agent_16db051b-bb20-4bf8-b0ed-81aff04345ef（fresh session，与执行者不共享上下文）
- Evidence: iteration 1 REJECTED（Blocker：inbox.page.yaml 重复 then 键 + 初始验证语料为陈旧 m2 classpath 产物）→ 整改（见「结束审计整改」节）→ iteration 2 **APPROVED**（含独立重放：`ErpAllFluxPagesExportTest` 999/855/0 绿、对账 999==999 / 855==855、报告 by-code 总账全平 1104、两树 cli.md diff 空、YAML 重复键检测 NONE）；残留 3 Minor（r1/r2/r3）已登记处理。审计全文见会话记录，要点已落盘本计划。

Follow-up:

- ERP 页面发现项修复切片（A 类：未知 renderer type 172——static 94/tpl 45/code 16/number 6/wrapper 4/panel 4/_hidden 3；属性枚举越界 303+23——variant=primary 296 等；Java flux-web 管线映射补全或页面侧修正；B 类 532+51+14+5+8 归 nop-chaos-flux 裁决。含 r1 的 23 条 columns/hidden 逐项登记）
- sdks 演进（见 Deferred）
- flux 仓预存 `pnpm build` TS 错误（flux-renderers-content markdown.tsx ApiResponse.ok）与 example.json 松弛示例——已登记其日志，归该仓跟进
