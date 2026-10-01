# 2026-10-02-0030-1 M2.2 nop-fixture-bundle skill

> Plan Status: completed
> Last Reviewed: 2026-10-02
> Source: `docs/backlog/fixture-bundle-test-data-roadmap.md` §Work Item Status M2.2（`nop-fixture-bundle` skill：配置推断 → 构造脚本编排 → 导出 → manifest 校验 → 喂测试）
> Related: M1.1/M1.2/M1.3（框架与验收 done——本 skill 编排既有能力，零代码）
> Audit: required

## Current Baseline

- 框架全链 done：bundle 格式/导出器/导入器/校验器（nop-entropy `9b0a704f79`+`bb2edc774c`+`2f7630edbe`）、runbook `../nop-entropy/docs-for-ai/03-runbooks/fixture-bundle.md` 定稿、批量入口（本仓 `785e217a0`）。
- `docs/skills/` 无 fixture-bundle 相关 skill；README 注册表无该行。
- 本仓技能形态先例：`docs/skills/*-prompt.md` 提示模板文件 + `docs/skills/README.md` 注册表行（技能/使用场景/不使用场景/必需输入/预期输出五列）。
- 剩余差距：AI 编排 fixture-bundle 无可复用技能入口。

## Goals

- **skill 文件**：`docs/skills/nop-fixture-bundle-prompt.md`——五阶段编排（① bundle 配置推断[据 ORM 模型建议 base 表与业务键]→② 构造脚本编排[JSON 请求序列落盘，构造必走 `@BizMutation`/GraphQL 业务逻辑入口]→③ 录制会话导出→④ manifest/校验器独立校验→⑤ 喂测试[导入 API/批量入口 M2.1]——**喂应用/app init 装载场景按 roadmap §Non-Goals 门控，不在本 skill 编排范围**），含触发词、反模式表、必读文档路由（runbook `fixture-bundle.md` 为主文档）。
- **README 注册**：`docs/skills/README.md` 注册表新增一行（含与既有技能的互补关系：与 `comprehensive-test-data` 部署 seed 类工作、`integration-test-roadmap` 集成用例的边界）。

## Non-Goals

- 不实现任何框架代码（纯文档 + README 行）；不构造任何真实 bundle 入库（敏感门控 successor 义务仍挂首个真实 bundle 入库 plan）。
- 生产环境装载（Non-Goals 门控不变）。

## Task Route

- Type: `implementation-only change`（纯文档技能资产 + 注册表行，零行为变更）
- Owner Docs: `docs/skills/README.md`
- Skill Selection Basis: `Skill: nop-testing`（按 roadmap M2.2 Skill 列记录——编写 fixture-bundle 编排内容需平台测试域知识；工具原生技能 `.opencode/skills/nop-testing/SKILL.md`，M0.1 确认口径）

## Infrastructure And Config Prereqs

- 无（纯文档）；回滚 = revert 单提交。

## Execution Plan

### Phase 1 - skill 文件与注册

Status: completed
Targets: `docs/skills/nop-fixture-bundle-prompt.md` + `docs/skills/README.md`
Skill: `nop-testing`

- Item Types: `Add`

- [x] skill 文件落盘（五阶段编排——⑤ 阶段须含 app 装载边界声明[Non-Goals 门控，防使用者越界尝试 app-init 装载] + 触发词 + 反模式表[含：静默 no-op beans 命名陷阱/M1.3 机制三教训引 Runbook/直插不触发 validateRefValue/敏感列 masked 纪律/repo 级门控 successor] + 必读路由[runbook/roadmap/testing-strategy]）
      - Skill: `nop-testing`
- [x] README 注册表行落盘（五列 + 互补关系）
      - Skill: `nop-testing`

Exit Criteria:

- [x] 两文件落盘；README 行格式与既有行一致（五列）

## Draft Review Record

- Independent draft review iteration 1: needs revision (agent_e6d46cab, fresh session) — 2 MAJOR（Skill 列 nop-testing 被静默替换为 none——未裁决偏离，对齐先例改记 nop-testing；「喂测试/应用」Non-Goals 门控限定语三处丢失——Goals ⑤ 收紧+skill 内容要求增 app 装载边界声明）+ 2 MINOR（基线 785e217a0 补本仓归属/Type 改 implementation-only）；通过项：Guard none 不弱化审计、敏感门控 successor 未弱化、命名先例符合、五列格式实存。全部修订已落实。
- Independent draft review iteration 2: accept (agent_e6d46cab 增量复审) — 四项修订全部落实且经实存核验（nop-testing SKILL.md 实存且实质对题），无残留缺陷。计划转为可执行契约进入实施（Guard none：无双批准环节，结束审计门控不变）。

## Closure Gates

- [x] 范围内行为完成（skill 文件 + README 注册行）
- [x] 相关文档对齐（README 注册表 + roadmap 审查记录/状态翻转）
- [x] 已运行验证（纯文档：grep 一致性核对——skill 文件被 README 引用、路由路径实存；plan-gates --strict PASS）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

- 起草时无 deferred 项。

## Closure

Status Note: skill 文件与 README 注册落盘（五阶段编排含 app 装载边界声明、反模式表 9 行、三向互补关系）；敏感门控 successor 义务未弱化；纯文档零行为变更。审计后转 done。

Closure Audit Evidence:

- Auditor / Agent: independent subagent agent_575f458b（fresh session，2026-10-02）
- Evidence: 审计 **ACCEPT**（0 BLOCKER / 0 MAJOR / 3 MINOR：闭包簿记 4 项已履行[roadmap 翻转+README+日志 10-02+本回填]/前向引用 testing-strategy 扩列括注留 M2.3/全局缓存教训锚 runbook 已达成[非阻塞]）；执行者自评逐项对表复核通过（五阶段/触发词/反模式 9 行/app 装载边界双落点/三向互补/successor 未弱化）；API 形状逐一活码核验（export/import/validate/run 全实存）；审计方独立复跑 plan-gates --strict PASS。

横切关注点 7（`AutoTestCaseDataSaver.removeInputTable` 路径）：**不适用**——本计划零触碰，按 roadmap 规则在此登记一次。

Follow-up:

- （仅非阻塞跟进；已确认缺陷不得在此）
