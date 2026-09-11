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
- `15-xbiz-xscript-no-trycatch-sink-to-java-bean.md` — **xbiz XScript 编排下沉 Java Bean**（2026-09-07 勘误：XScript 现已支持 try/catch/finally，见 nop-entropy plan 2258；"无法 try/catch"是历史版本结论，非当前能力边界）。多步编排 + 各自失败隔离、跨实体事务组合、幂等防御仍建议下沉 Java Bean（Guard/StateMachine/编排 Processor 范式）——事务边界/类型安全/可测试性，xbiz source 只做薄委托（状态写回 + 一行 inject）。M4.64 机制注记 + RC-R1.89 D2 薪酬三路计提编排两案定稿。
- `16-cross-repo-schema-contract-consumer-source-verification.md` — **跨仓库 schema 契约须对照消费端渲染器源码验证**：松耦合契约失效是静默降级非抛错——生成侧输出 `valueField`、消费侧读 `valueKey`，运行时退化 fallback 无任何报错；结构性契约测试（键存在性）146 全绿但功能全坏。plan 2026-08-24-1147-1 三轮 plan-audit 全过、closure-audit 捕获 2 项 P0 实录。含四步强制清单（读消费端解析点 / 契约对照表进文档 / 深度断言 / 抽样落盘统计分布）。
- `17-code-history-deferred-triangulation-audit.md` — **代码 × 历史 × Deferred 三路交叉审计**：新一輪审计 mission 的非平凡增量 = 三路交叉（代码 / 历史 / Deferred），第三路（读最近 50 份 plan 的 Deferred 段）= 隐藏的 finding 金矿。ai-check-r2 M0.2 扫描揭示 8 项已满足 / 11 项部分满足 / 21 项未满足的 deferred 触发条件。含多次执行隔离纪律 + 状态机延用 + 同型 finding 合并基类 + 保护区域不绕。
- `18-schema-contract-redesign-no-type-sniffing-no-sugar.md` — **契约重设计中禁类型嗅探与语法糖复发**：类型嗅探（`type === 'crud'`）与 sugar（顶层 `columns`/`options` 兼容层）是同一架构失败的两个症状——在 renderer 命令式设计集成而非在 schema 声明式设计协议。破坏性重设计下旧测试是迁移清单非验收标准；"让旧测试不改通过"驱动连环回摆（picker v3 实施期三次回摆实录）。含声明/身份判别式 + 契约回摆四驱动力表 + 决策树。
- `19-statemachine-throw-domain-error-code-directly.md` — **组件直接抛领域错误码，禁"先抛通用码再外层转码"**：通用码无通用消费方（唯一消费方是转码层自己）+ StateMachine 无共享基类 = 转码是纯 boilerplate，裸奔实体则跨域错误码不一致。正确形态：StateMachine 的 `illegal()` 直抛领域码，Guard/Processor 转码层退役。含三判据反模式判定 + 自检清单。全局修复计划 `2026-09-07-2200-1`。
- `20-mechanical-violations-script-baseline-gating.md` — **机械类违规须脚本基线门控而非抽样审计**：抽样对机械可判定违规零遗漏性缺陷（r1/r2「执行多轮仍存在问题」）——确定性清剿五要素（全量扫描脚本 / CAT 分级 / 基线快照 + 单向收紧 strict 门控 / anti-fake-green 自证 / 白名单唯一豁免 + 批注账），脚本绿 = 闭环。含 r3 CAT 终态对账实录（CAT-1 335→0 / CAT-2 204→0 / CAT-3 390→0 / CAT-4 1700→0、白名单 27 文件四要素、`--self-test` 13/13）+ 与 lesson 07（门控运维面）/17（发现面 vs 清剿面）划界 + 三段自检清单。
- `21-standard-codification-before-conformance-audit.md` — **规范成文先于符合性审计**：标准不成文 → finding 无判定准绳 → 修复无授权（F15 后端消息 Non-Goal 反事实实录）。方案三步（权威 owner doc 成文 + 判定冲突唯一权威声明 / 成文时点快照冻结防真相源漂移[lesson 13] / 检查清单冻结后才开审计[M0.5 先例]）。含 r3 零争议实录（`i18n-compliance.md` 承载裁定 → MI.2~MI.6 + M1.x DIM-I 维零标准争议复判）+ 开工前三问（标准在哪/谁裁决/冲突以谁为准）+ 与 lesson 12/20 划界。
- `22-closure-assertion-scope-full-gating-table.md` — **收官断言必须覆盖门控全表**：门控有 N 个维度，收官断言就有 N 个必检项——「只盯上轮漂移维度」的三元断言让新漂移穿越多轮收尾（R1b/R1d 穿越 M2.5~MV.1 四轮假阴性绿，M2.9 全表同跑才暴露；F2.9 仅核 daoFor 漏 R12 import 面同型实录）。含全表断言收尾协议 + 与 lesson 07（漂移运维面）/20（建门面）划界。
- `23-snapshot-surgical-text-transform-no-full-rerecord.md` — **`_cases` 快照对齐必须外科式文本变换**：载体文案变更仅消息/名称/摘要列旧值→新值、保留 `*`/`@var:` 断言自由度，禁 `force-save-output` 全量重录（实测窄化通配符断言，MI.6 批 1 实证）。含 RFC 4180 重引号 / seed 字典真相收敛 / 语义树等价重序列化协议（r3 四批实录）+ 与 lesson 06 / seed-data.md 双面重录义务划界。
- `24-concurrent-session-interference-isolated-full-verification.md` — **同机姊妹会话并发干扰下的全量验证须隔离鉴别**：并发编辑/构建产出伪失败、共享 `~/.m2` 并发 install 污染（09-02 typo 态混态 + 09-08 MI.9 四次全量伪失败两案实录）——涉事模块隔离复跑鉴别 + `git clone` 钉 HEAD（0 脏文件）隔离重跑 + Git State 干扰面披露。与 lesson 05（日志优先）/22（断言范围）划界。

> **2026-08-28 提升裁决（ai-check-r2 M0.7 自审）**：M0 阶段执行后沉淀方法学——三路交叉审计范式入课为 `17`。

> **2026-08-20 提升裁决（plan `2026-08-20-1255-2` G.1，requirement-compliance mission MG）**：roadmap 点名两类必入——`12` 文档化简化滥用 + `13` 需求基线陈旧；mission 证据高频候选 5 项逐一裁决——config-gate 认定范式（跨 ≥10 切片复现）入课为 `14`、xbiz XScript 编排下沉（M4.64+R1.89 双案平台机制约束）入课为 `15`、死常量/死列激活与 dangling dict 值设计预防**划界归并 lesson 10**（"声明但无写点"同族，已扩边界注记）、owner doc 表述过时未随实现更新**划界归并 lesson 13**（审计快照型陈旧案例族，已收录 payroll.md 案）。

> **2026-08-28 提升裁决（ai-check-r2 M0.7 自审 + M5.3 收口预演）**：M0 阶段执行后沉淀方法学——三路交叉审计范式入课为 `17`。M5.3 closure audit CG1-CG5 主会话全通过，CG6 successor 触发条件登记（子代理通道恢复 / 人工裁决）。

> **2026-09-11 提升裁决（ai-check-r3 MV.3，plan `2026-09-11-0906-3`，roadmap 点名义务）**：roadmap MV.3 行点名两类必入——「机械类违规须脚本基线门控而非抽样审计」入课为 `20`（r3 M0.2/M0.3/MI.x 主线实绩：`tools/check-hardcoded-cjk.mjs` + `docs/audits/cjk-baseline.md` 单向收紧 + CAT 终态四条对账链，lesson 17 的发现面/本课清剿面、lesson 07 的建设期/运维面互补划界）+「规范成文先于符合性审计」入课为 `21`（M0.1 反事实对照：F15 Non-Goal 根因 = 标准不成文；`docs/architecture/i18n-compliance.md` 唯一权威 + M0.5 清单冻结先例，与 lesson 12 关闭载体判别式/lesson 13 快照冻结防御划界互链）。同族划界：两条新课不重写 lesson 01–19 既有案例，互链仅限本索引行与新课正文反向引用。

> **2026-09-11 提升裁决（ai-check-r3 MG.1，plan `2026-09-11-1425-1`，roadmap MG.1 点名义务 + MV.3 移交候选终审）**：MV.3 移交四项逐一裁决——①R1b/R1d 三元断言新失败模式入课为 `22`（复现 ≥2：M2.5~MV.1 穿越多轮 + F2.9 仅核 daoFor 漏 R12 import 面；与 lesson 07/20 划界互链，07 课同日附「全表断言」边界扩展注记）；②CJK 白名单裁决范式（roadmap 点名）**划界归并 lesson 20 边界扩展注记**——第五要素已含白名单 + 批注账，C1/C2 窄类裁决与脚本门控耦合无独立复用面；③known-good-baselines 终态行复核 + ④历史执行目录只读盘点为履行性义务非失败模式（`2026-09-11-1425-1` Phase 3 Proof 落实，不立项）。Phase 1 机械枚举补充候选裁决——`_cases` 快照外科式文本变换入课为 `23`（复现 ≥4：MI.6 批 1/批 2、MI.7、MI.8 批 2；trap 面 force-save-output 窄化与正确面成对）、姊妹会话并发干扰隔离入课为 `24`（复现 ≥2：09-02 共享 m2 污染 + 09-08 MI.9 伪失败四连）；watch-only 候选六项登记原计划 Deferred But Adjudicated 节（validate:flux 325 variant 外部漂移 / 磁盘资源耗尽验证截断 / surefire 汇总 vs Results 块聚合计数口径 / 墙钟毫秒竞态 flaky[bugs 专文承载] / I*Biz 代理 install 刷新 / CLOSURE_SCRIPT_CHECK 结构缺陷[指南承载]），不立项。skills 面零提升（注册表无缺位：基线漂移裁决已有 `compliance-baseline-drift-adjudication-prompt.md`）。同族划界：三条新课与两条边界扩展注记不重写 lesson 01–21 既有正文，互链仅限本索引行与新课/注记反向引用。
