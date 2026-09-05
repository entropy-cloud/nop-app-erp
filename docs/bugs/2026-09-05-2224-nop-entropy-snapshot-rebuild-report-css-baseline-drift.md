# 兄弟仓 nop-entropy 快照重建静默漂移本仓报表渲染测试基线（nop-report-core CSS emit 修复 → 3 集成用例 check-output 红）

## 问题

- 本仓工作树零 Java/测试资产变更（仅 docs），`mvn test` 全 reactor 在 `app-erp-all` 3 个集成用例上 `nop.err.autotest.check-output-fail` 红：
  - `TestErpC21ApsCapacityLoad`（`4_report_render_response.json5`）
  - `TestErpC15CrmLeadForecast`（`11_report_forecast_accuracy_response.json5`）
  - `TestErpC16CsSlaNotification`（`9_report_sla_csat_response.json5`）
- 三者均为 `ErpReport__renderHtml` 输出快照比对失败，层 1 JUnit 锚点断言全过、仅层 2 HTML 字符串不匹配。
- 影响：无本地变更的验证 run 假红；数据行（金额/负荷率/合计）逐字节一致，仅报表内嵌 `<style>` 一行不同。

## 复现

- 前提：兄弟仓 `../nop-entropy` 在本机 `~/.m2` 以 `2.0.0-SNAPSHOT` 供包；某时点有人在 nop-entropy 侧改动报表模块并 `mvn install`。
- 触发：在本仓任意零代码变更状态下跑 `mvn test -pl app-erp-all`（或全 reactor）。
- 最小复现：`mvn test -pl app-erp-all -Dtest=TestErpC21ApsCapacityLoad`（漂移存在时 check-output-fail；对齐后绿）。

## 诊断方法

- 困难点：`git status` 干净（测试资产零变更）、同日早些时候同代码曾全绿——"代码没变测试变红"指向环境/依赖而非本仓。
- 调查路径：① `git diff HEAD~1 --name-only` 确认变更面仅 docs → 排除本仓代码回归；② surefire txt 提取 `check-match-fail` 的 `value=`（实际）与 `expected=`（录制）做行级 diff → 唯一差异行 `#xpt-report .xpt-row{ line-height:normal;}`（实际）vs `line-height:normal}}`（录制）；③ `ls -la ~/.m2/.../nop-report-core/2.0.0-SNAPSHOT/*.jar` → jar 时间戳当日 10:14（本机被重建）；④ `git -C ../nop-entropy log --oneline -5` → 近期 commit 即报表 PDF 导出视觉审计工作。
- 被拒绝假设：数据漂移（金额/统计行 diff 逐字节一致，排除）；日期/时钟型 flake（报表 HTML 不含渲染时刻时间戳，排除）；本仓 `_cases` 被意外改动（`git status --porcelain app-erp-all/_cases/` 为空，排除）。
- 决定性证据：jar 时间戳 + nop-entropy 近期 report 模块 commit + 三用例 diff 收敛到同一 CSS 行。

## 根本原因

- nop-entropy `nop-report-core` 快照 jar 被当日重建并修复了 xpt-report CSS emit 的旧缺陷：`.xpt-row{ line-height:normal}}`（缺分号 + 双右括号，非法 CSS）→ `line-height:normal;}`（合法）。
- 本仓集成用例层 2 快照把整段 HTML（含该 CSS 行）录制成基线，平台包更新后基线随之失配——快照基线对「依赖 jar 的字节级行为」是隐式耦合。

## 修复

- 对 3 个 `_cases/**/output/*_response.json5` 基线文件做单行外科更新：`line-height:normal}}` → `line-height:normal;}`（各 1 行，无其他改动）。
- 选择手工单行修补而非 `forceSaveOutput` 重录：变更面最小化（tables CSV 等零接触），且修补后 CHECKING 模式复跑 3 类 3/3 绿 + 全 reactor `mvn test` BUILD SUCCESS，反证 diff 恰为该行（全量重录无法提供此等价性证明）。

## 测试

- `app-erp-all/src/test/java/io/nop/app/all/it/TestErpC21ApsCapacityLoad.java` 等 3 类（integration，层 2 快照）：修补后 `mvn test -pl app-erp-all -Dtest='TestErpC15CrmLeadForecast,TestErpC16CsSlaNotification,TestErpC21ApsCapacityLoad'` 3/3 绿；全 reactor `mvn test` BUILD SUCCESS（2026-09-05 22:24）。
- 层 1 锚点（WC-001 / 8.00 / 0.50 token、金额、合计）未动且始终全绿，证明仅 CSS 表层漂移。

## 受影响的工件

- `app-erp-all/_cases/io/nop/app/all/it/TestErpC21ApsCapacityLoad/testApsCapacityLoad/output/4_report_render_response.json5` - CSS 行对齐
- `app-erp-all/_cases/io/nop/app/all/it/TestErpC15CrmLeadForecast/testCrmLeadForecastClosedLoop/output/11_report_forecast_accuracy_response.json5` - 同上
- `app-erp-all/_cases/io/nop/app/all/it/TestErpC16CsSlaNotification/testCsSlaNotificationClosedLoop/output/9_report_sla_csat_response.json5` - 同上

## 未来重构注意事项

- 任何本仓验证 run 假红时，先核对 `~/.m2/repository/io/github/entropy-cloud` 相关 jar 的 mtime 与 `../nop-entropy` 近期 commit，再怀疑本仓代码——SNAPSHOT 依赖意味着 nop-entropy 侧 `mvn install` 会即时改变本仓测试输入。
- 若 nop-report-core 再次改 HTML emit 形态，本仓所有含 `renderHtml` 输出快照的集成用例（不止 3 个）会同型漂移；修复应循「surefire diff 收敛分析 → 最小基线修补 → CHECKING 复跑反证」路径，勿直接全量重录掩盖真实回归。

## 预防差距

- nop-entropy 报表模块行为变更（其 ai-dev/log 有记录义务）与本仓快照基线之间无联动声明；跨仓验证 runbook 可补充「依赖 jar mtime 检查」一步。
