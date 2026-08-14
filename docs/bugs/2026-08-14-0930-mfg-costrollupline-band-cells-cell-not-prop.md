# mfg ErpMfgCostRollupLine view.xml 引用非实体属性档位字段致 cell-not-prop 页面验证回归

## 问题

- 什么坏了：`ErpAllFluxPagesTest.testAllPagesRenderInFluxMode` 与 `ErpAllWebPagesTest.testValidateAllPages`（app-erp-all 页面验证测试）失败——`ErpMfgCostRollupLine` 页面构建错误 `nop.err.xui.form.cell-not-prop`：表单[view]的字段[materialBand]不是已定义的实体属性。
- 在哪里坏了：`module-manufacturing/erp-mfg-web/.../ErpMfgCostRollupLine.view.xml:34`（form view layout 引用 `materialBand`/`laborBand`/`overheadBand`/`subcontractBand` 四档位 cell）+ 同文件 grid cols（:16-19 四 band cols）。
- 最小可见症状：`mvn test` 全 reactor 时 app-erp-all 模块 FAILURE；`ErpAllFluxPagesTest` 报 2 错误（main.page.yaml + picker.page.yaml，均指向 form view `materialBand`）。
- 影响或严重性：mfg 成本滚算行明细页面在 flux 渲染模式下构建失败（页面不可渲染）；同时阻塞全 reactor `mvn test` 绿基线（fail-fast 截断下游模块测试）。非本计划（`2026-08-13-1146-1` finance notes 状态机）引入——clean HEAD 重编译 module-finance 后复现不变。

## 复现

- 环境和前提条件：HEAD 含 `452e418d0`（2026-08-11 E4.1 计划提交，权限保密字段级可见性）。
- 触发步骤：
  1. `mvn test`（全 reactor）
  2. 观察 app-erp-all FAILURE：`ErpAllFluxPagesTest` / `ErpAllWebPagesTest`
- 最小复现（单模块）：
  ```bash
  mvn test -pl app-erp-all -Dtest=ErpAllFluxPagesTest
  ```

## 诊断方法

- 诊断难度：直接——surefire 报告错误信息明确指向 `ErpMfgCostRollupLine.view.xml:34` 的 `materialBand` cell。
- 调查路径：
  1. 全 reactor `mvn test` 失败后读取 `app-erp-all/target/surefire-reports/`：3 个失败测试（`ErpAllFluxPagesTest` / `ErpAllWebPagesTest` / `TestAuthSeedLoadingProof`），前两者为同一 mfg view 错误，后者为独立 NPE（另记）。
  2. `git status` 确认工作树仅改 module-finance + docs——mfg view 文件零改动 → 怀疑预存回归。
  3. `git log -- module-manufacturing/.../ErpMfgCostRollupLine.view.xml` 定位最后改动提交 `452e418d0`（2026-08-11 E4.1）：grid 移除 materialCost/laborCost cols 改 4 band cols；form view 替换要素 cells 为 band cells。
  4. 决定性证据：`git stash` 后重编译 module-finance（`mvn clean install -DskipTests -pl module-finance/erp-fin-service -am`）再跑 `ErpAllFluxPagesTest`——**依然失败**（clean HEAD 复现），证明非本计划变更引入。
- 被拒绝的假设：~~当前计划（finance notes 状态机 Bean）破坏页面构建~~——工作树仅改 module-finance，且 clean HEAD 复现。

## 根本原因

- `452e418d0`（E4.1）按设计将 mfg 成本 4 要素字段（materialCost 等）`published=false` 隐藏，改经 `@BizLoader(autoCreateField=true)` 输出 4 个**代理视图字段**（materialBand/laborBand/overheadBand/subcontractBand，档位映射 high/mid/low）。
- 代理字段仅存在于 OutputBean 的 GraphQL 响应视图，**不是实体属性**；E4.1 同时把 view.xml 的 grid cols 与 form view cells 改写为引用这些代理字段。
- 平台 `UiFormModel.validate` 对 form layout cell 校验实体属性存在性 → `cell-not-prop` 抛错 → 页面构建失败。grid cols 同理潜在 col-not-prop 风险（本错误先于 grid 校验暴露）。
- E4.1 计划验证仅跑 module-manufacturing 模块级测试（165 tests green），**未跑 app-erp-all 的页面全量验证测试**（`ErpAllFluxPagesTest`/`ErpAllWebPagesTest`），回归漏检。

## 修复

- 未修复（本 VERIFY 运行仅记录）。修复归属 E4.1 计划/权限保密 successor，候选方案：
  - 方案 A：form layout 档位 cells 改显式 `<cells>` 声明 + `custom="true"`（对齐 `ErpMfgSubcontractOrder.view.xml` 发料/收货参数 cell 先例，plan 2026-07-13-0701-2 / 07-16 修复 0012-1 范式）。
  - 方案 B：grid cols 与 form cells 移除档位列（若档位仅供 API 消费，不展示页面）。
  - 需 E4.1 owner 裁决展示意图后再动。

## 测试

- 回归覆盖缺口：E4.1 未跑 app-erp-all 页面验证（`ErpAllFluxPagesTest` FLUX_PAGE_ERROR_COUNT / `ErpAllWebPagesTest` validateAllPages）。修复后应跑 `mvn test -pl app-erp-all -Dtest=ErpAllFluxPagesTest,ErpAllWebPagesTest` 确认 0 errors。

## 受影响的工件

- `module-manufacturing/erp-mfg-web/src/main/resources/_vfs/erp/mfg/pages/ErpMfgCostRollupLine/ErpMfgCostRollupLine.view.xml:16-19,34`（grid 4 band cols + form view 4 band cells）
- 引入提交：`452e418d0`（plan `2026-08-11-0915-3` E4.1）

## 未来重构注意事项

- 新增 `@BizLoader(autoCreateField=true)` 代理字段到 view.xml 时，必须同步确认 form layout cell 用显式 `<cells>` + `custom="true"`，否则页面验证失败。
- 页面级验证测试（app-erp-all `ErpAllFluxPagesTest`/`ErpAllWebPagesTest`）是跨域 view.xml 回归的唯一防线——任何修改 view.xml 的计划都应将其纳入验证命令（E4.1 教训）。

## 预防差距

- E4.1 计划验证命令为模块级 `mvn test -pl module-manufacturing/erp-mfg-service -am`，未包含 app-erp-all 页面全量验证；应作为该计划的 Closure 门禁项。
