# 2026-08-27-2006-3-introspection-config-doc-alignment 19 分域 app introspection 配置与 owner doc 断言对齐

> Plan Status: active（独立草案审查共识：iteration 1 acceptable-as-is，task `ses_fbcddd4ffffeRGWlJuJy8mTOPT`）
> Last Reviewed: 2026-08-27
> Source: `docs/audits/2026-08-26-2226-multi-audit-erp-enhancement.md` P1-6（多面审计 needs revision）
> Related: plan `2026-08-26-0735-2`（E3 整体实现，已关闭——E3.6 冻结项①「默认保持关闭」语义收口）、plan `2026-08-27-2006-1`（同批 P1 修复）
> Audit: required

## Current Baseline

- 全部 **19 个**分域 app `module-*/erp-*-app/src/main/resources/application.yaml:37-38`（purchase 分域为 :33-34）携带 `nop.graphql.schema-introspection.enabled: true`（自初始 codegen 提交 `15e64237c` 起即如此，覆盖 aps/assets/b2b/contract/crm/cs/drp/finance/hr/inventory/logistics/maintenance/manufacturing/master-data/notify/projects/purchase/quality/sales）；仅 `app-erp-all/src/main/resources/application.yaml:23-25` 为 `false`。Nop 平台该配置默认 `false`。
- owner doc `docs/design/ai-native-interface.md` §前置调研结论断言「经 config `nop.graphql.schema-introspection.enabled` 门控，**默认 false（应用当前显式关闭）**」——对 19 个可独立运行的 Quarkus app 为假（GraphQL schema 全暴露面），属 E3.6 落地时应修正的契约/文档双漂移。
- E3.6 冻结项①本身成立：app-erp-all（生产工件/E2E 运行时）关闭 + `TestErpAiIntrospectionEnabled`（`app-erp-all/src/test/java/io/nop/app/all/it/`）JUnit 双证真实（默认关闭 + 开启后 IntrospectionQuery 可用含 description）——该测试不依赖分域 app yaml，配置翻转零测试影响。

## Goals

- 全部 20 个应用工件（1 聚合 + 19 分域）introspection **默认关闭**，owner doc「应用当前显式关闭」断言为真，消除 GraphQL schema 暴露面漂移。

## Non-Goals

- 不动 app-erp-all（已 false）与平台默认值。
- 不动 E2E/JUnit 对 introspection 开启态的验证逻辑（`TestErpAiIntrospectionEnabled` 测试内自开自证，保持）。
- 多面审计 P2-12（`TestErpAiIntrospectionEnabled` 悬空注释引用）等 P2 项不在范围——已归 roadmap Follow-up Backlog。

## Task Route

- Type: `implementation-only change`（配置漂移修复 + owner doc 对齐）
- Owner Docs: `docs/design/ai-native-interface.md`（§前置调研结论——断言权威源）
- Skill Selection Basis: `Skill: none`——纯 yaml 配置翻转 + 文档注记，无 BizModel/view/测试编写面；nop-backend-dev/nop-frontend-dev 均不匹配

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 - 配置翻转 + 断言对齐

Status: planned
Targets: 19 × `module-*/erp-*-app/src/main/resources/application.yaml`、`docs/design/ai-native-interface.md`
Skill: `none`

- Item Types: `Decision | Fix | Proof`
- Prereqs: none

- [ ] `Decision` **收口路径裁决（审计给出二选一）**：**方案 A（采用倾向）= 19 个分域 yaml 翻 `false`**——secure-by-default、对齐 app-erp-all 与平台默认、E3.6 冻结项①「默认保持关闭」语义在全部工件成立；方案 B（否决倾向）= 断言收窄为「聚合工件关闭」——保留 19 分域暴露面，与冻结清单语义相悖且留审计复发面。理由与残留风险记入本计划。残留风险评估含再生成持久性：19 yaml 为 `src/main/resources` 手持源文件（非 `_gen/`，历史仅 2 次提交且历经 ≥3 轮增量 regen 未被回改，app-erp-all 07-07 翻 false 后 7 周零回退；AGENTS.md 禁止重跑 `nop-cli gen`）——就地翻转变更在现行工作流下持久。Skill: `none`
- [ ] `Fix` 按裁决翻转 19 个 `application.yaml` `schema-introspection.enabled: true → false`（如裁为方案 B 则改为 owner doc 断言收窄 + 分域保持，二选一落地，不得两者都不做）。Skill: `none`
- [ ] `Fix` `ai-native-interface.md` 断言与实态一致：方案 A 下补注记「全部应用工件（app-erp-all + 19 分域 app）显式关闭」；方案 B 下断言收窄并说明分域差异。Skill: `none`
- [ ] `Proof` 全工件一致性验证：`grep -rn -A1 "schema-introspection" --include="application.yaml" module-*/erp-*-app/src/main/resources/ app-erp-all/src/main/resources/` 全部 `enabled: false`（20/20）。Skill: `none`

Exit Criteria:

- [ ] 20/20 应用工件 introspection `enabled: false`（或裁决为 B 时断言与 19 个 true 实态精确一致，无全称假断言）
- [ ] owner doc 断言与 grep 实态逐行一致

### Phase 2 - 零回归验证

Status: planned
Targets: `docs/logs/2026/`（本计划 Closure 证据落点）
Skill: `none`

- Item Types: `Proof`
- Prereqs: Phase 1

- [ ] `Proof` `mvn test -pl app-erp-all -Dtest=TestErpAiIntrospectionEnabled` 绿（配置翻转零测试影响）。Skill: `none`
- [ ] `Proof` compliance checker 复跑零漂移（纯 yaml 变更，预期无规则触及）。Skill: `none`
- [ ] `Proof` 日志条目（`docs/logs/` 当日）。Skill: `none`

Exit Criteria:

- [ ] 测试绿 + checker actual ≤ baseline
- [ ] 日志条目落盘

## Draft Review Record

- Independent draft review iteration 1: **acceptable-as-is（3 MINOR 采纳修订）**（task `ses_fbcddd4ffffeRGWlJuJy8mTOPT`，fresh session）——活仓核verified：19/19 分域 true（行号 :37-38、purchase :33-34）/ app-erp-all :23-25 false / `ai-native-interface.md:65` 断言原样 / `TestErpAiIntrospectionEnabled` 经 `@NopTestProperty` 自开自证零依赖分域 yaml（分域 app 无 src/test）/ 全仓 grep 零消费方依赖分域 enabled（E2E、_tmp-server、playwright 均无 introspection JVM 参数）/ 再生成持久性证真（非 `_gen/`、3 轮 regen 未回改）；3 MINOR 已修：①基线行号精确化；②Decision 残留风险补 regen 持久性证据；③Phase 2 Targets 路径化。

## Closure Gates

> 无业务代码变更（yaml + 文档），验证命令以 scoped 测试 + grep 证明为门控，全量构建归 mission VERIFY 批。

- [ ] 范围内行为完成（P1-6 漂移消除：20 工件与 owner doc 断言一致）
- [ ] 相关文档对齐（`ai-native-interface.md`）
- [ ] 已运行验证（TestErpAiIntrospectionEnabled + 20 工件 grep + compliance checker）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

（无）

## Closure

Status Note: pending

Closure Audit Evidence:

- Auditor / Agent: pending
- Evidence: pending

Follow-up:

- 无
