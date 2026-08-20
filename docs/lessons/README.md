# Lessons Index

Use this directory for numbered reusable lessons learned from development.

These are not day-by-day notes. They are durable engineering lessons that should help future sessions avoid repeating the same mistake.

Recommended filenames:

- `01-requirement-source-was-not-implementation-ready.md`
- `02-prototype-fidelity-did-not-cover-business-rules.md`
- `03-plan-closure-claimed-too-early.md`

When a bug, retrospective, or audit reveals a repeatable pattern, consider promoting it into `docs/lessons/`.

## Lessons

- `04-bizmodel-service-method-contract-and-testing.md` — BizModel/I*Biz 服务方法契约（注解 + IServiceContext 末参 + @Name）与测试必须经 IGraphQLEngine（直调缺 session）。含 `@SingleSession` 误判实录与验证结论。
- `05-nop-e2e-failure-log-first-diagnosis.md` — Nop 失败诊断：日志优先、从后向前定位。Playwright 超时只是「果」，因几乎总在服务端；不信遗留 server、跑最小复现、读 `errorCode=`/`@_loc`/`Caused by:` 因果链。含「渲染超时」长期误诊为环境问题的实录与 `tools/parse-nop-errors.mjs`。
- `06-codegen-product-edit-overwrite.md` — **代码生成产物编辑必被 `mvn clean install` 覆盖**：`_` 前缀文件 / `_gen/` 子目录 / `__XGEN_FORCE_OVERRIDE__` 标记 / 从 ORM `<dict>` 生成的 yaml 均为生成产物。改这些 = 临时状态。**唯一正确位置是模型源或保留层 Delta**。含 notify inbox saga + business-type.dict.yaml 两案各 3 轮审计实录与编辑前自检清单。
- `07-compliance-baseline-drift-on-plan-changes.md` — **compliance checker 基线漂移**：加深/fix 计划新增 daoFor/import/状态字面量后，closure 未重跑 checker 核对基线，未把合法新增提升为基线值（baseline-raise）也未把真违规 Fix。经 1057-1/0823-1/V.2 三轮裁决 + G.1 复核才 0 漂移收敛。含 per-site git diff 分类决策树与 closure 前自检清单。
- `08-plan-closure-without-independent-audit.md` — **计划标 completed 却缺独立 closure audit**：执行者/mission-driver 只勾 `[x]` + 标 `Status: completed` + 自填 Closure，从未让独立子代理 fresh session 跑 closure-audit 留证据。R3.5 第三波发现 14 份（3 轮清理）。与 lesson 03（内部状态文本）正交——本课是缺外部验证门控。
- `09-posting-exception-swallow-suspension.md` — **业财过账 `tryPost` 吞异常致 `posted=false` 永久悬挂**：宽 `catch(Exception)` 吞咽只 `log.warn`，不通知、不进终态、无告警闭环。R1.16 跨域合并 12 findings 同族根因。含失败→悬挂典型路径与 catch 写法自检清单。
- `10-dict-dead-state-unreachable.md` — **状态机 dict 死状态**：dict 声明了无 `setStatus` writer 的状态值（永不出现），owner doc 迁移图却声明进/出迁移。MR1 跨 finance/mfg/hr/inv/qa/prj/contract/aps/logistics 多域同型。含「每个状态值 grep writer」决策树与删/实现/Deferred 三选一裁决。2026-08-20 边界扩展：dangling dict 值设计侧预防（RC-R1.73 D3）+ 死常量/死列激活先例（RC-R1.89）同族归并。
- `11-index-status-not-backfilled-after-fix.md` — **修复完成未回填追踪索引**：roadmap 标 done + 计划 completed，但 arm-index finding 行 `修复状态` 仍 `todo (R*.x)`。V.5 发现 102 条陈旧标签批量回填。状态回填是闭合的一部分，不是事后清理。
- `12-documented-simplification-abuse.md` — **文档化简化滥用**：documented simplification / Deferred / Non-Goal 掩盖需求-实现分歧——P0/P1 级方案 B 关闭（Q4=(a) 禁止）、AI 自写标注冒充人工批准、"文档提及"冒充"功能存在"。R1.27 承付恢复 / P1-RC-025 换货缺失等 RC 重开族实录。含「关闭载体是代码还是文档」判别式与三判据核验决策树。
- `13-requirement-baseline-staleness.md` — **需求基线陈旧**：真相源计数/结构与 owner doc 事实性表述随实现演进被动漂移，被下游审计/修复当前提消费致伪事实传播——0.2 三修正（product-scope 计数 / logistics heading / UC 标题重复）+ RC-R1.89「hr 模块零 xbiz」审计快照过时 BLOCKER 实录。含断言类型决策树（计数型指向权威源 / 快照型引用前 grep 实仓）。
- `14-config-gate-deployment-contract-adjudication.md` — **config-gate 认定范式**：「功能默认关闭」≠「功能缺失」，反之硬契约禁 config 化稀释。A4.1.4 首立三源核对法（真相源部署契约声明 / module-meta optionalFeatures / 生产 yaml 普查），跨 ≥10 切片复用 + MR1 修复 config 化裁决反向应用。含对称误判（假阳性/假阴性）自检清单。
- `15-xbiz-xscript-no-trycatch-sink-to-java-bean.md` — **xbiz XScript 无法 try/catch**：XLang 引擎不支持 TryStatement，多步编排/失败隔离/复杂守卫必须下沉 Java Bean（Guard/StateMachine/编排 Processor 范式），xbiz source 只做薄委托（状态写回 + 一行 inject）。M4.64 机制注记 + RC-R1.89 D2 薪酬三路计提编排两案定稿。

> **2026-07-31 提升裁决（plan `2026-07-31-1330-2` G.2）**：候选 6 模式中 5 个提升为 `07`–`11`；`@Inject private` **排除**——已被 `docs/skills/README.md §已知失败模式 #6` 收录为速查项（单一规则无 case 复杂度，不另建 lesson）。

> **2026-08-20 提升裁决（plan `2026-08-20-1255-2` G.1，requirement-compliance mission MG）**：roadmap 点名两类必入——`12` 文档化简化滥用 + `13` 需求基线陈旧；mission 证据高频候选 5 项逐一裁决——config-gate 认定范式（跨 ≥10 切片复现）入课为 `14`、xbiz XScript 编排下沉（M4.64+R1.89 双案平台机制约束）入课为 `15`、死常量/死列激活与 dangling dict 值设计预防**划界归并 lesson 10**（"声明但无写点"同族，已扩边界注记）、owner doc 表述过时未随实现更新**划界归并 lesson 13**（审计快照型陈旧案例族，已收录 payroll.md 案）。
