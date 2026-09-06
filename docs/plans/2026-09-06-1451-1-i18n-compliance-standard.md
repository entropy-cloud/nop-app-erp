---
status: active
mission: ai-check-r3
work-item: M0.1
group: "2026-09-06-1451"
verify: [test]
---

# 2026-09-06-1451-1 M0.1 i18n/日志语言标准成文（i18n-compliance.md 权威 owner doc）

## Current Baseline

（实仓核验 2026-09-06）

- `docs/architecture/i18n-compliance.md` **不存在**（本计划创建）；`docs/audits/cjk-baseline.md` 亦不存在（M0.2/M0.3 产物，非本计划）。
- roadmap `docs/backlog/ai-check-r3-roadmap.md` §目的 合规基线裁定表（8 行判定）已冻结为历史快照；M0.1 done 后判定冲突以 `docs/architecture/i18n-compliance.md` 为唯一权威（roadmap §合规基线表下方冻结声明 + 规则 5）。
- 8 行判定：`ErrorCode.define` 中文描述**合规**（zh-CN 源语言，英文后期经 i18n yaml 补充）；`*Errors.java` 接口 `@Locale("zh-CN")` **必须补齐**；LOG 消息中文**违规**（统一英文，不走 i18n）；异常路径中文散文参数**违规**（传状态码/枚举名/字典值本身）；运行时字符串中文**违规、白名单制**；Java 注释中文**豁免**；`*.page.yaml`/`*.flux.yaml` 用户可见文案中文**违规**（补 `i18nEn` 或模型源 `i18n-en`）；`_` 前缀生成 i18n yaml **禁手改**（en 覆盖写非下划线手写文件 + `x:extends` 继承）。
- 探针计数（2026-08-31：LOG 335/483、异常参数约 195、运行时字符串 1813 行、page/flux yaml 2183 行、827 个 `ErrorCode.define`、22 个 `*Errors.java` 未标 `@Locale`）是口径敏感快照，判定以 M0.2 脚本 + M0.3 冻结为准；本计划不在 owner doc 中内联复制计数作准绳（防 lesson 13 需求基线陈旧被消费），仅登记探针口径的指针。
- Owner doc 锚点全部实仓在位：`docs/design/domain-design-guidelines.md` §七、`docs/architecture/view-and-page-strategy.md` §国际化策略（l.107 起）、`../nop-entropy/docs-for-ai/02-core-guides/error-handling.md`（l.119 合规依据 + l.332-351 `_` 前缀生成链）。
- `docs/context/conventions.md` 存在，当前无 i18n/日志语言节；`docs/index.md` 为顶级路由器，当前无 i18n-compliance 路由行；`docs/skills/README.md` §已知失败模式（l.144 起）当前无「硬编码中文」条目。
- 平台资产在位：`io.nop.api.core.annotations.core.Locale` 注解、`module-*/erp-*-meta/postcompile/gen-i18n.xgen` 生成链、`_vfs/i18n/{en,zh-CN}/`、`docs/design/i18n-glossary.md`（414 token）。
- 22 个 `*Errors.java`（main 源码实数 find = 22）全部未标 `@Locale`——MI.1 修复面，本计划不改。

## Goals

- 新建 `docs/architecture/i18n-compliance.md`：将 roadmap §目的 合规基线裁定表落为权威 owner doc，含判定准绳、白名单登记格式、修复模式对照表（LOG 英文化 / 异常参数传码 / 页面 yaml i18nEn / 模型源 i18n-en）。
- `docs/context/conventions.md` 增补 i18n/日志语言节并指向权威 doc；`docs/index.md` 增路由行；`docs/skills/README.md` 已知失败模式增补「硬编码中文」条目。
- **不改任何生产代码**（roadmap M0.1 硬边界）。

## Non-Goals

- 不建 827 条 `erp.err.*` 英文镜像 i18n yaml（用户裁定：英文翻译后期经 i18n yaml 补充，本轮不建承载）。
- 不修改任何 `*Errors.java`、LOG 语句、异常参数、页面 yaml——全部归 MI.x 修复批。
- 不编写 M0.2 检测脚本、不落基线快照（M0.2/M0.3 归后续计划 `2026-09-06-1451-2`）。
- 不修订 nop-entropy 平台文档（外部仓库保护区）。

## Phase 1 — i18n-compliance.md 成文

> Skill: `audit-remediation-roadmap-authoring-prompt`（成文纪律，roadmap M0.1 指定）
> Item Types: `Add | Proof`
> Prereqs: 无（M0 全部前置项中本项最先）

- [x] Add: 新建 `docs/architecture/i18n-compliance.md`，三要素齐备：(a) **判定准绳表**——8 类裁定逐条成文，每条含判定规则 + 依据指针（用户裁定 / `error-handling.md:119` / `domain-design-guidelines.md` §七 / `view-and-page-strategy.md` §国际化策略）；(b) **白名单登记格式**——文件路径 + 理由 + owner doc 指针 + 裁决来源，声明白名单是运行时字符串中文（CAT-3）唯一豁免通道，登记落 `docs/audits/cjk-baseline.md`（M0.2 产物，本 doc 只定义格式）；(c) **修复模式对照表**——CAT-1 LOG 英文化（保留 `{}` 占位）/ CAT-2 异常参数传状态码/枚举名/字典值本身 / CAT-3 逐簇裁决（改字典 key、英文或白名单登记；`@Description("中文")` 按 E3 计划豁免登记）/ CAT-4 页面 yaml 补 `i18nEn` 或模型源 `i18n-en` 属性（codegen 产物禁改生成物，lesson 06，M0.4 矩阵为准）。
      - Skill: `audit-remediation-roadmap-authoring-prompt`
- [x] Add: 文档头部声明权威地位与冻结关系——本 doc done 后判定冲突以其为唯一权威，roadmap §合规基线表冻结为历史快照不再更新；探针计数不内联为准绳，仅指针引用。
      - Skill: `audit-remediation-roadmap-authoring-prompt`
- [x] Proof: 成文内容与 roadmap §目的 8 行判定逐行对齐零矛盾；与 `nop-entropy error-handling.md`（l.119 / l.332-351）、`domain-design-guidelines.md` §七、`view-and-page-strategy.md` §国际化策略、`docs/errors/README.md` 交叉核对零冲突；`@Locale` 注解全限定名 `io.nop.api.core.annotations.core.Locale` 实仓核实。

## Phase 2 — 规范路由与已知失败模式增补

> Skill: none（纯 docs 路由与增补，无匹配技能）
> Item Types: `Add | Proof`
> Prereqs: Phase 1（增补内容指向 i18n-compliance.md）

- [x] Add: `docs/context/conventions.md` 增补「i18n 与日志语言」节——LOG 英文不走 i18n、异常参数传码不传中文散文、`*Errors.java` 标 `@Locale("zh-CN")`、页面 yaml 用户可见文案补 `i18nEn`；节内显式声明判定冲突以 `docs/architecture/i18n-compliance.md` 为唯一权威。
- [x] Add: `docs/index.md` 增补路由行（i18n/日志语言合规判定 → `docs/architecture/i18n-compliance.md`），放置与现有 architecture 类路由行风格一致。
- [x] Add: `docs/skills/README.md` §已知失败模式增补「硬编码中文」条目——速查要点：运行时面中文违规 / 白名单唯一豁免通道 / `_` 前缀生成物禁手改 / 探针计数口径敏感须脚本冻结；入口指向 `i18n-compliance.md`。
- [x] Proof: 三处增补互相指向一致（conventions → 权威 doc；index 路由可达目标文件；skills 条目入口可解析），目标路径全部实仓存在。
- [x] Proof: `mvn test` 全 reactor 零新增失败（纯 docs 变更回归核证；对照 `docs/testing/known-good-baselines.md` 2026-09-04 行预存失败清单：hr `TestErpHrDepartmentPositionDeleteGuard#testDeleteEmptyPositionAllowed` + drp `TestErpDrpCrossDock#testStagingTimeoutFallbackJob`，零新增即通过）。

## Draft Review Record

- dispatch review #review-2026-09-05-123532-mission-driver-2026-09-06-1451-1-i18n-compliance-standard-1-bcab870c to opencode-reviewer-2026-09-06-145949
- 2026-09-06：iteration 1，共识 accept #review-2026-09-05-123532-mission-driver-2026-09-06-1451-1-i18n-compliance-standard-1-bcab870c

## Verification

- pass test 2026-09-06-1451-1-mvn exit=0 2026-09-06 全 reactor BUILD SUCCESS（156 模块，8012 tests / 0 failures / 0 errors），优于 `known-good-baselines.md` 2026-09-04 行——该行登记的 2 个预存失败（hr `TestErpHrDepartmentPositionDeleteGuard#testDeleteEmptyPositionAllowed` + drp `TestErpDrpCrossDock#testStagingTimeoutFallbackJob`）本轮实测均已通过，零新增失败。全量日志：`_tmp/2026-09-06-1451-1-mvn-test.log`（68M 行，app-erp-all 集成测试 DDL 逐实体落盘所致，非异常）。
- pass test 2026-09-06-1556-r3m0-verify exit=0 mission-driver verify run：全 reactor `mvn clean install -DskipTests`（2:16）+ `mvn test`（21:55）双 BUILD SUCCESS，Results 行精确汇总 4006 tests / 0 failures / 0 errors / 1 skipped，对照 2026-09-04 行零新增失败。日志：`_tmp/ai-check-r3-r3m0-verify-build.log` / `_tmp/ai-check-r3-r3m0-verify-test.log`。

## Closure

- dispatch audit #audit-2026-09-05-123532-mission-driver-2026-09-06-1451-1-i18n-compliance-standard-1-bd79cce3 to opencode-auditor-2026-09-05-123532-mission-driver models={exec:glm-5.3-flash,aud:glm-5.3-flash}
- accepted #audit-2026-09-05-123532-mission-driver-2026-09-06-1451-1-i18n-compliance-standard-1-bd79cce3：审计通过——两 Phase 8/8 项全勾且逐项实仓核验落地（`docs/architecture/i18n-compliance.md` 三要素在位、`docs/context/conventions.md` §i18n 与日志语言 + `docs/index.md` 路由行 + `docs/skills/README.md` 第 14 条互指一致、roadmap M0.1 `done`、`docs/logs/2026/09-06.md` 同步）；`mvn test` 全 reactor BUILD SUCCESS（8012/0/0，日志 `_tmp/2026-09-06-1451-1-mvn-test.log` BUILD SUCCESS 尾注实查）；`plan-check.mjs --strict` 零结构错误，§5.2 完成公式全 conjunct 满足，`completed` 由引擎派生。
