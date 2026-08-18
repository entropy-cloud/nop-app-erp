# 2026-08-19 ErpCsTicket.view.xml 重复追加损坏 + 页面校验测试审计盲区

## 现象

- mission requirement-compliance VERIFY（plan `2026-08-18-1849-1`，RC-R1.68+69）全仓 `mvn test` 于 app-erp-all 失败 4 类：`TestAuthSeedLoadingProof` / `TestErpSeedDataIntegrity` / `ErpAllWebPagesTest.testValidateAllPages` / `ErpAllFluxPagesTest`（FLUX_PAGE_ERROR_COUNT=9）。
- 根错误链：`/erp/cs/pages/ErpCsSurvey/main.page.yaml` → `/erp/cs/pages/ErpCsTicket/picker.page.yaml` → 解析 `ErpCsTicket.view.xml` 报 `nop.err.core.xml.string-not-end-properly`（:153 `source="${'` 属性值中途截断）。
- 文件体积 **4196 行 vs HEAD 335 行**：`<forms>/<pages>` 区块被重复追加约 22 份残缺拷贝（每份以不同截断点结束），首拷贝 add 表单 kbSuggestion 块两处属性值截断（`source="${'` / `visibleOn="${'`）。

## 根因

- EXECUTE 阶段对 `module-cs/erp-cs-web/.../ErpCsTicket.view.xml` 的编辑过程发生重复追加事故：整段内容被反复写入且多次中途截断（非 git 合并冲突）。首拷贝已含完整预期功能面（escalateQuality/adoptKnowledge 表单、row-escalate-quality-button、两个 simple 提交页、adopt dialog 按钮、空态三元文案片段），但 add 表单块存在属性截断。
- **为何漏检（审计盲区）**：结束审计验证命令 = `mvn test -pl module-cs/erp-cs-service`（服务层测试不解析 view.xml）+ `mvn test -pl app-erp-all -Dtest=TestErpAllJobYamlLoading`（仅 job.yaml 计数）+ `mvn clean install -DskipTests`（view.xml 仅作资源拷贝不解析）。**未运行 app-erp-all 页面模型校验测试组**（ErpAllWebPagesTest / ErpAllFluxPagesTest / TestErpSeedDataIntegrity / TestAuthSeedLoadingProof），view.xml 层损坏不可见。
- 审计证据失真实例：Closure Audit Evidence 中「空态文案 3 处（:1525/:2835/:3828）」实为对损坏重复拷贝的 grep 命中（预期语义仅 add/edit 2 处）；行号证据未与 HEAD 基线行数（335）交叉核对（实际 4196），量级异常未被识别。

## 修复（2026-08-19 已落地）

- 截断至首拷贝（1–412 行）恢复单一文档结构；从后续残缺拷贝回收预期编辑片段重建 add 表单 kbSuggestion 块（`placeholder` 三元空态文案 `subject 已输区分提示` + adopt 按钮 `ajax→dialog[adoptKnowledge]`）；edit 表单同步应用相同两处编辑；`minRows=2` → `minRows="2"` 引号规范化（全仓无裸属性先例）。
- 最终 diff vs HEAD = 预期最小变更集（96+/26−）；`xmllint --noout` 通过；重装 erp-cs-web 后 4 失败测试 7/0/0 全绿（FLUX_PAGE_ERROR_COUNT=0）；全仓 `mvn test` BUILD SUCCESS 全绿 + checker 19 规则零漂移。

## successor（触发条件/规避）

- **凡计划触及 `*.view.xml` / `*.page.yaml`**：验证命令必须包含页面模型校验组（至少 `mvn test -pl app-erp-all -Dtest='ErpAllWebPagesTest,ErpAllFluxPagesTest'`，涉及种子/全链时加 TestErpSeedDataIntegrity/TestAuthSeedLoadingProof）。
- 结束审计对 view 层工件的行号/计数证据应与基线行数量级交叉核对（本例 4196 vs 335 的 12 倍膨胀是显性信号），防止重复追加型损坏以「多处 grep 命中」伪装成真实证据。

## 关联

- plan `docs/plans/2026-08-18-1849-1-rc-mr1-r1-68-69-cs-quality-escalation-knowledge-adoption.md`（Post-Closure Correction 节）
- 日志 `docs/logs/2026/08-19.md`
