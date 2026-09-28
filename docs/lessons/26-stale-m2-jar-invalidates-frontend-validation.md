# Lesson 26: 前端资源改动后未经模块重装的验证链验证的是旧态——stale .m2 jar 陷阱

> **来源**：2026-09-28 perf-ux mission 批次 5（plan `2026-09-28-0906-3`）。批次 1（plan 2026-09-27-0318-1）日志已记载「`mvn -pl app-erp-all test` 依赖 .m2 旧 jar，模块资源改动后须先重装该模块再验证」，同日批次 5 内仍三次灼伤：①全仓构建后继续修 view.xml，未重装即跑 validate:flux——报告数字与损坏中间态逐字相同造成「门禁已过」假象；②单模块修复后直接重跑 validate:flux（stale jar 复现旧态 failedPages=3）；③结束审计代理 R-3 以 manifest failedPages=44 + 报告字节级与旧态一致戳穿证据链。
> **适用场景**：任何「构建产物 → 测试/导出/校验」管线消费 `.m2` 或 `target/` 构件的场景——`npm run validate:flux`（ErpAllFluxPagesTest 导出 erp 模块页面 → flux-compiler 校验）、模块资源（view.xml/page.yaml/xmeta）改动后的任何渲染类验证、跨模块 resource 依赖的集成验证。
> **失败模式**：修复源文件 → 复跑校验 → 报告数字「正常」→ 声称验证通过。实际校验的是修复前的 jar 旧态：损坏中间态可能「恰好」产生与基线相同的计数（本例 errors 325=325），假绿证据链直通结束审计，被审计方以 manifest/报告 mtime+failedPages 交叉证伪。

## 核心论点

验证链消费的是**构件**而非**源文件**。源文件修复与构件重装是两个必须显式串联的步骤，缺省的增量工具链（`mvn -pl app-erp-all test`）只重装聚合模块自身，不会重装被聚合的资源模块。校验报告与源文件状态之间没有任何自动一致性保证。

## 防御规则

1. **修正 → 重装 → 校验**三步必须同一条命令链显式串联（`mvn -pl <触及模块> install -DskipTests && npm run validate:flux`），或在全仓 `mvn clean install` 之后**不得再改任何源文件**——改了就重跑全链。
2. **新鲜度自证**：校验报告必须附 mtime/内容指纹与最后一次源文件 mtime 的先后关系；`flux-pages/manifest.json` 的 `failedPages` 是导出层健康的独立信号，必须与 validator totals 同时核对（本例 totals 「正常」而 failedPages=44 正是穿帮点）。
3. **审计方交叉证伪抓手**：报告 mtime vs 最后源文件 mtime、manifest generatedAt、jar 内资源内容 vs 源文件（unzip 抽查）、failedPages 计数——四者任一矛盾即证据链失效。

## 关联

- 批次 1 日志（2026-09-27）「验证期发现」同型先例（picker 重装后实证）。
- Lesson 24（并发会话干扰隔离）：同属「验证证据不可信」族，但 24 是并发污染、本课是**时序陈旧**。
