---
status: active
mission: ai-check-r3
work-item: M2.0
group: "2026-09-10-0425"
verify: [test]
---

# 2026-09-10-0425-1 M2.0 修复方法基线（流程文档化 + 同型 finding 基类化裁决）

## Current Baseline

- M1 审计阶段已执行完毕：五维 × 21 核对单元覆盖矩阵完整（plan `2026-09-10-0251-1` 收官核账——105 基础格：finding 38 / pass 65 / n-a 2，28 报告 140 切片子格全景矩阵 `m1-17-coverage-matrix-final.md` 在案），独立子代理收官审计 ACCEPT（2026-09-10）；roadmap M1.x 行的 done 翻转归 owner/engine 处置（各行「执行完成待收官翻转」注记），非本计划义务。
- r3 轮新立 finding 共 93 条（P0=0 / P1=3 / P2=17 / P3=73），全部登记于跨轮索引 `docs/audits/check/ai-check-index.md` §Finding 追踪且状态 open。P1 分布：P1-CK-mfg-022-r3（归 M2.3）、P1-CK-ct-025-r3 + P1-CK-ct-026-r3（归 M2.7）；M2.2/M2.4/M2.5/M2.6 四个域批的 r3 P1 面为空集（fin/ast/pur+sal+inv/hr+prj+qa 均无 r3 新立 P1）。逐 ID 现值以索引为唯一清单真相源（本计划不复制清单——`docs/lessons/13-requirement-baseline-staleness.md`）。
- M1.x 已完成族聚合双 handoff：`P3-CK-common-014-r3`（daoFor 无豁免注释全仓族——354 文件扫描 42 有注释/312 无，按域分片认领、U20 为聚合追踪点）与 `P3-CK-common-015-r3`（`new ServiceContextImpl()` 裸 new 族——82 站点/61 文件，44 文件裸 new，修复范式 = 镜像 ExpenseCostAggregator ctx 兜底）；另有 FNPT mutation 零注册族（b2b-011/drp-018 r1 站点 + ct-030-r3/hr2-028-r3/prj-022-r3/qa-029-r3/mnt-023-r3 等 + P3-CK-app-001-r3 「enforcement 前置阻塞面」聚合注记）、dict 值域族（P1-CK-ct-025-r3/drp-022-r3/b2b-018-r3 + r1 b2b-013「统一批次」注记 + md-018-r3 死配置）、owner-doc 漂移族（hr2-027-r3/crm-023-r3/ct-028-r3/ct-029-r3/b2b-019-r3/ast2-025-r3/app-002-r3 等）、DIM-F 死状态样式/孪生遮蔽族（P3-CK-pur-017-r3/cs-027-r3/mfg-023-r3）、DIM-T 覆盖缺口族（mfg2-027-r3/mfg2-028-r3 flaky/hr2-030-r3 及 aps/log/mnt 零测试动作）等横切族形态散布于 M1.x 报告。
- 修复方法现状：r1 F0.2 方法基线（读报告 → 先写失败测试 → 复现 → 修复 → 测试绿 + 零回归；证伪 = 书面 not-a-problem；索引回填；保护区路由；checker 不高于快照）以引用块形式内联于 `docs/backlog/ai-check-roadmap.md` MF 节，经 F1.x/F2.x 20+ 修复批实证有效，但**从未落为活 owner doc**；全 `docs/architecture/` 树无修复方法文档（2026-09-10 实核 ls）。r3 roadmap M2 前言沿用 r1 F0.2 + r2 M2.0 加严口径，M2.0 义务 = 「流程文档化 + 同型 finding 基类化裁决」。
- 基类化先例在案：r1 F1.3 `AbstractErpCrudBizModel` 状态锁基类（363 文件全域接入）+ F1.2 REQUIRES_NEW 守卫前置/副作用后置范式（9+ 站点）——「一处基类/范式修复全族回填」路径已被验证。
- 绿色基线：`docs/testing/known-good-baselines.md` 2026-09-09 行（compliance checker R2b=242/R2c=1542/R12a=71 新基线 + 全 reactor `mvn test` 4006/0/0/1 零新增失败）；CJK checker report mode CAT1..4 = 0/0/0/0、`--strict` PASS（2026-09-10 实跑复核一致）。
- 剩余差距：M2.1~M2.9 全部 blocked 于本项——修复方法无活 owner doc（域批/族批执行者无权威操作规程）；r3 各横切族无路由裁决（哪些进 M2.8 族批、哪些留域批、哪些逐条 deferred，未定即执行会产生范围漂移与重复立项）。
- 依赖状态：M1.17 执行完毕 + 独立收官审计 ACCEPT（done 翻转 administrative，归 owner/engine）；本计划为 M2 阶段第 1 计划（组内 N=1），解除 M2.x 全部后续项阻塞。

## Goals

- 修复方法落为活 owner doc（新建 `docs/architecture/finding-remediation-method.md`）：每 finding 强制五步流程（读报告 → 先写失败测试 → 复现 → 修复 → 测试绿 + 既有测试零回归）、证伪路径（书面 not-a-problem 说明）、保护区路由（ORM/api.xml = auto + dual-agent-approval；过账/auth/数据删除 = plan-first）、seed 联动义务（快照重录双面）、双 checker 不高于基线门控、索引回填协议（r3 轮 centralized 归 M2.9，修复批只产证据指针）、族回填范式（F1.3/F1.2 先例）。口径与 r1 F0.2 + r3 M2 前言一致并显式声明来源，使 r2 M2.0（同形项，尚未执行）可复用引用而不重立。
- 同型 finding 基类化裁决落盘：对 r3 轮全部横切族逐族裁决「M2.8 族批（一处基类/范式修复 + 全族回填）/ 留域批逐站点 / doc 批 / 逐条 deferred（M2.9 显式计数）」四路去向 + 每族修复范式与分片方式；裁决记录落本轮执行目录 `docs/audits/check/2026-09-06-1645-ai-check-r3/m2-0-family-adjudication.md`（轮内产物，M2.8 执行者唯一消费入口，沿 m0-4 矩阵先例），稳定方法论部分入 owner doc。
- 检查点（roadmap M2.0 行）：零生产代码改动证明 + 全 reactor `mvn test` 零新增失败 + compliance/cjk 双 checker 不高于基线。

## Non-Goals

- 不修复任何 finding（M2.1~M2.8 在本项完成后按 roadmap 序执行）；不为 M2.8/M2.9 预写执行计划。
- 不做 roadmap 状态翻转、不做双索引 finding 状态回填（M2.9 义务）、不改 `docs/audits/check/ai-check-index.md` 任何 finding 行状态。
- 不改任何生产代码/ORM/api.xml/seed/页面文件（零代码计划）；不动既有 checker 脚本与两份基线冻结块（`compliance-baseline.md`/`cjk-baseline.md`）。
- 不接管 r1/r2 工作项（横切关注点 5）：r1 open finding（b2b-013、mfg3-012、P2-CK-mfg-010 等）只做边界与状态继承注记，不立项修复。
- 不修订 M0.1 `i18n-compliance.md` 判定准绳（DIM-I 面已清零，本项不涉 i18n 判定争议）。

## Phase 1 — 修复方法 owner doc 成文

> 统一类型：Decision | Add（1 Decision + 2 Add）。
> Skill: none（roadmap M2.0 行指定；成文纪律沿 audit-remediation-roadmap-authoring-prompt 的 owner-doc 成文口径，不另立技能义务）
> Targets: `docs/architecture/finding-remediation-method.md`（新建）、`docs/index.md`（路由行）、`docs/architecture/README.md`（如含目录索引则同步一行）
> Prereqs: 无（M1.17 执行完毕即解锁）

- [x] <Decision> 文档落位与口径裁决：新建 `docs/architecture/finding-remediation-method.md` 而非扩写 `processor-extension-pattern.md`（roadmap M2.0 行 Owner Doc 列指向后者）——理由：修复方法横跨 CRUD 守卫/dict 值域/seed/前端/单测五维，非 processor 单一面；`processor-extension-pattern.md` 保持模式专属职责（其既有读者契约不变）。替代方案：(a) 扩写 processor-extension-pattern.md——被否，职责混杂且该 doc 已为过账域审计锚点，混入通用方法会稀释其判定语义；(b) 只写在本计划内不立 owner doc——被否，r2 M2.0 与后续轮次无法引用会话产物，违反「规范成文先行」本轮立轮原则。残余风险：新 doc 可发现性——以 `docs/index.md` 路由行 + doc 头部「消费方」节（列出 r2 M2.0/M2.1~M2.8/MV.1）缓解。决策与理由记录于本项勾选注记。**〔执行注记 2026-09-10〕决策落地：新建 owner doc（零改动 processor-extension-pattern.md）；可发现性缓解两件已落地——index.md 路由行 + doc §消费方节；口径来源四节（r1 F0.2 / r3 M2 前言 / r2 M2.0 加严 / 各 owner doc 权威指针）显式声明。**
      - Skill: none
- [x] <Add> owner doc 成文：五步流程（含失败测试的最低断言强度要求——复现缺陷的可观察行为断言，非仅类型/签名）、证伪路径书面化格式、保护区路由表（对齐 `docs/context/ai-autonomy-policy.md` 现值）、seed 联动义务（`docs/architecture/seed-data.md` §快照重录义务 双面重录触发条件）、双 checker 门控（compliance 不高于 `compliance-baseline.md` 机器块 + CJK `--strict`；漂移走独立基线裁决）、索引回填协议（轮内 centralized 归收官项 vs 修复批只产「fixed + 测试与提交指针」证据注记——r3 口径 = M2.9 集中回填，r1 F0.2 逐条回填口径作为替代模式并注差异）、族回填范式（基类/单一范式点修复 + 按域分片认领 + 聚合追踪点 ID，引 F1.3/F1.2/common-014-r3/common-015-r3 先例）。全部内容显式标注来源（r1 F0.2 引用块 + r3 M2 前言），不自造新规。**〔执行注记 2026-09-10〕七要素齐备（§五步强制流程 / §证伪路径 / §保护区路由 / §seed 联动义务 / §双 checker 门控 / §索引回填协议 / §族回填范式），逐节标注来源；断言强度要求写入第 2 步；r1 逐条回填作为替代模式并注差异。**
      - Skill: none
- [x] <Add> `docs/index.md` 增路由行（architecture 节，面向「执行 ai-check 修复批/M2.x 族批」查询意图）；`docs/architecture/README.md` 若维护目录清单则同步。不改其他 owner doc（无既有 doc 被本 doc 矛盾——成文时逐节核对，发现矛盾即停并登记，不得静默双写）。**〔执行注记 2026-09-10〕index.md「首先阅读」表增行（锚 i18n 行后）；README.md §Initial Owner Docs 同步一行；成文逐节核对：新 doc 将权威让渡给 ai-autonomy-policy/seed-data/compliance-baseline/cjk-baseline/i18n-compliance/testing-strategy 各自节，无矛盾双写。**
      - Skill: none

Exit Criteria:

- [x] owner doc 落盘且五步流程/证伪/保护区/seed/checker/回填/族回填七要素齐备，口径与 r1 F0.2 + r3 M2 前言逐条对账一致（无自造新规）**〔2026-09-10 实核：七节齐全；五步流程 = F0.2 骨架 + r2/r3 加严固化；证伪/seed/checker/保护区均显式让渡权威 owner doc〕**
- [x] `docs/index.md` 路由可达；`processor-extension-pattern.md` 零改动（职责边界裁决在案）**〔2026-09-10 实核：index.md 行在案；processor-extension-pattern.md 本计划零触碰（git status 待 Phase 3 零改动证明复核）〕**

## Phase 2 — 同型 finding 基类化裁决（族路由四分）

> 统一类型：Decision（4 项 Decision）。
> Skill: none
> Targets: `docs/audits/check/2026-09-06-1645-ai-check-r3/m2-0-family-adjudication.md`（新建，本轮执行目录内轮内产物）
> Prereqs: Phase 1 完成（裁决记录引用 owner doc 范式节）

- [x] <Decision> 族清单冻结：以 `docs/audits/check/ai-check-index.md` §Finding 追踪现值 + 28 份 `ck-*-r3.md` 报告为源，机械枚举 r3 轮 93 条 finding 的族归属（逐条归族或「无族独立项」），产出全量映射表（finding ID → 族 → 去向）。禁止凭记忆补 ID（lesson 13）；枚举时逐 ID 复核索引行现值。**〔执行注记 2026-09-10〕机械枚举 `rg` 提取 §Finding 追踪 93 行（P1=3/P2=17/P3=73 与基线一致），逐行读取摘要+「修复归」列后归类；全量映射表落 `m2-0-family-adjudication.md` §1（93 行 8 类去向，对账 46+15+4+24+4=93）。**
      - Skill: none
- [x] <Decision> 逐族四路裁决（每族记录：选择 / 替代方案 / 残余风险，规则 9 义务）：① M2.8 族批——基类/单一范式点修复 + 全族回填可行的族（默认候选：common-014-r3 豁免注释族——逐文件补一行 javadoc 或注入 I*Biz 按域分片；common-015-r3 ctx 兜底族——ExpenseCostAggregator 范式镜像 + job 入口逐站点豁免裁决；FNPT 注册族——聚合 x:extends/注册范式，须先裁决 app-001-r3「enforcement 前置阻塞面」是否将族修复门槛定为 config 投产前置[lesson 14 三源核对适用]；DIM-T 覆盖缺口族；DIM-F 死状态样式族）；② 留域批——P1 三条（mfg-022-r3 → M2.3；ct-025-r3/ct-026-r3 → M2.7）及与其同链不可拆的站点；③ doc 批——owner-doc 漂移族（hr2-027-r3/crm-023-r3/ct-028-r3/ct-029-r3/b2b-019-r3/ast2-025-r3/app-002-r3 等）归入 M2.8 或独立 doc 批次的归属裁决；④ 逐条 deferred——不具族修复价值且不值得独立修复的 P3，显式登记「deferred + 理由」，归 M2.9/MV.2 deferred 计数。dict 值域族（ct-025-r3 + drp-022-r3/b2b-018-r3/b2b-013）单独裁决：P1 修复与 r1 b2b-013「统一批次」注记的跨轮边界（r3 只修 r3 站点，范式一致性注记移交 r1 通道）。**〔执行注记 2026-09-10〕八族/批逐个裁决齐备（§2.1~§2.10，选择/替代/残余风险三件套逐族在案）；族 B 拆 11 注册片 + app-001 聚合片，app-001-r3 裁决 = 修复入族批、enable-action-auth 翻 true 不设为族批门槛（lesson 14 三源核对：缺省 false + seed 零授权行 → opt-in 合法），但翻 true 前族 B 必须全 fixed（触发条件登记）；doc 批归属裁决 = M2.8 内独立分片 5（不新增 roadmap 工作项）；dict 族跨轮边界 = r3 只修 4 个 r3 站点、r1 b2b-013 移交 r1 通道。**
      - Skill: none
- [x] <Decision> 跨轮边界与状态继承注记：P2-CK-mfg-010（r1 open，mfg-022-r3 同控制点升级源）、mfg3-012（r1 open，委外同型）等「修复 m3-r3 P1 即消解其控制点」的继承关系逐条登记（r3 修复批落证据，状态回填归 M2.9 同型 finding 状态继承）；r1/r2 未消化 open finding 不入本裁决范围（横切关注点 5），仅登记「由其所属轮次通道处理」。**〔执行注记 2026-09-10〕§3 继承表 6 行：mfg-010/mfg3-012/sal-010+pur-005/b2b-013/FNPT r1 站点/r1r2 其余 open 面逐条登记，回填归 M2.9。**
      - Skill: none
- [x] <Decision> M2.8 执行切分建议：族批的执行序与分片粒度（按域分片认领的批次划分 + 每片验收口径 = 该族 checker/grep 断言归零），供 M2.8 计划起草直接消费；不预写 M2.8 计划本身。**〔执行注记 2026-09-10〕§4 六分片（B→A→E/F/C→D→doc→dict）执行序 + 每片验收断言 + 域批并行通道 + 收敛判据；M2.8 计划本身未预写。**
      - Skill: none

Exit Criteria:

- [x] `m2-0-family-adjudication.md` 落盘：93 条 r3 finding 全量映射（逐 ID 归族与去向）+ 逐族四路裁决（选择/替代/残余风险齐备）+ 跨轮继承注记 + M2.8 切分建议 **〔2026-09-10 落盘 `docs/audits/check/2026-09-06-1645-ai-check-r3/m2-0-family-adjudication.md` §1~§4〕**
- [x] 裁决与索引现值零冲突（枚举源 = 执行时点索引实值，逐 ID 复核注记在案）；P1 三条去向与 roadmap M2.3/M2.7 行一致 **〔2026-09-10 实核：rg 枚举 93 行与 §1 表逐 ID 对应；P1 去向 mfg-022-r3→M2.3 / ct-025+ct-026→M2.7 与 roadmap 行域范围一致（§5 零冲突声明）〕**

## Phase 3 — 检查点验证（零改动证明）

> 统一类型：Proof（3 项 Proof）。
> Skill: none
> Targets: 无代码改动；验证输出记入本计划勾选注记
> Prereqs: Phase 1~2 完成

- [x] <Proof> 零生产代码改动证明：`git status --porcelain` 过滤 `module-*/`/`app-erp-all` 生产路径为空（本计划变更面仅 docs/）；`mvn clean install -DskipTests` BUILD SUCCESS（156 模块，docs 改动不触构建，作廉价保险）**〔执行证据 2026-09-10：porcelain 过滤 `module-|app-erp-all` 零命中（grep exit 1）；脏面 = 本计划 4 产物（plan/owner doc/index.md/README.md/裁决文件）+ 1 既有 untracked 姊妹计划 `2026-09-10-0425-2-m23-manufacturing-p1-fix-batch.md`（他会话产物，披露不属本计划变更面）；`mvn clean install -DskipTests` exit 0 BUILD SUCCESS，Reactor Summary 156/156 SUCCESS〕**
      - Skill: none
- [x] <Proof> 双 checker 不高于基线（roadmap M2.0 检查点）：`bash docs/audits/nop-compliance-checker.sh` exit 0 且逐规则值 = 2026-09-09 基线行（R2b=242/R2c=1542/R12a=71 等 19 规则）；`node tools/check-hardcoded-cjk.mjs --strict` PASS exit 0（CAT1..4 持平 0/0/0/0）。漂移即停：按「Compliance 基线漂移」失败模式开独立裁决，不在本计划顺手放宽 **〔执行证据 2026-09-10：compliance exit 0，19 规则实测 R1a..R12c = 0/0/0/14/34/242/1542/38/5/0/0/2/0/0/14/0/71/66/42，与 compliance-baseline.md §BASELINE 机器块逐值相等零漂移；CJK `--strict` exit 0 PASS「0 new violations vs frozen snapshot」，本次实扫 CAT1..4 = 0/0/0/0（快照冻结值 0/0/209/1318 为 M0.3 冻结面，170 项 improvements = MI 批既有改善，非漂移；cjk-baseline.md 本计划零触碰）〕**
      - Skill: none
- [x] <Proof> 全 reactor `mvn test` 零新增失败（对照 2026-09-09 基线行 4006/0/0/1 与其登记的预存失败清单 = 无）**〔执行证据 2026-09-10：`mvn test` exit 0 BUILD SUCCESS，全 reactor 聚合 Tests run: 4006, Failures: 0, Errors: 0, Skipped: 1——与 2026-09-09 基线行精确一致，零新增失败〕**
      - Skill: none

Exit Criteria:

- [x] 生产路径零改动证明在案；双 checker 逐值持平基线 **〔2026-09-10 三证齐备，见上方执行证据〕**
- [x] 全 reactor `mvn test` 零新增失败（`verify: [test]` 门控线在 `## Verification` 落 pass 记录）**〔2026-09-10 4006/0/0/1 精确对账，记录见 `## Verification`〕**

## Draft Review Record

- dispatch review #review-2026-09-09-210030-mission-driver-2026-09-10-0425-1-m20-fix-method-baseline-family-adjudication-1-6dfb26ce to 2026-09-09-210030-mission-driver
- 2026-09-10：iteration 1，共识 approved #review-2026-09-09-210030-mission-driver-2026-09-10-0425-1-m20-fix-method-baseline-family-adjudication-1-6dfb26ce

## Verification

- 2026-09-10 执行会话 `verify: [test]` 门控：全 reactor `mvn test` **pass**（exit 0 BUILD SUCCESS，4006 tests / 0 failures / 0 errors / 1 skipped，与 `docs/testing/known-good-baselines.md` 2026-09-09 行精确一致，零新增失败；对照预存失败清单 = 无）。
- 2026-09-10 零改动证明 **pass**：`git status --porcelain` 过滤 `module-*/`/`app-erp-all` 生产路径零命中；`mvn clean install -DskipTests` exit 0（156/156 SUCCESS）。
- 2026-09-10 双 checker **pass**：`bash docs/audits/nop-compliance-checker.sh` exit 0（19 规则实测 0/0/0/14/34/242/1542/38/5/0/0/2/0/0/14/0/71/66/42 = §BASELINE 机器块逐值相等）；`node tools/check-hardcoded-cjk.mjs --strict` exit 0 PASS（0 new violations，实扫 CAT1..4 = 0/0/0/0，快照冻结面 170 项 improvements 为 MI 批既有改善非漂移）。
- 2026-09-10 独立闭包审计会话复跑 **pass**：`mvn clean install -DskipTests` exit 0（156/156 SUCCESS）；全 reactor `mvn test` exit 0 BUILD SUCCESS，surefire 逐类聚合 **3991/0/0/1**（0 failures / 0 errors / 1 skipped，2026-09-09 基线行登记的预存失败清单 = 无 → 零新增失败门控满足）；双 checker 复跑 compliance exit 0 + CJK `--strict` PASS exit 0（与上方执行会话实测逐值一致）。
- 计数对账注记（闭包审计）：本次实测聚合 3991 与历史行登记 4006 差 15——根因 = `m0-6-mvn-test.log` 为一次中断预跑 + 完整重跑的同日志拼接（如 `PropertyErpFinBudgetCommitmentRelease` 的「Running」行出现 2 次，首段 `Tests run: 1` 部分行被「41 模块汇总行求和」口径重复计入：fin +8 / ast +2 / inv +5 = 15），去重后 2026-09-06 真值 = 3991 与本次实测精确一致；基线 commit `8e8ab7fca` 以来 `src/`/test 零变更（`git diff --stat 8e8ab7fca..HEAD -- '*src*' '*test*'` 为空），0 failures / 0 errors 门控实质不受影响。基线行数值勘误归 owner 流程 successor，本计划不改动 `docs/testing/known-good-baselines.md`（auditor 修复权限不含 owner-doc 内容变更）。
- pass test 20260910-0554 exit=0

## Closure

Status Note: 三 Phase 全部执行项与退出标准 `[x]`（16/16 计数域干净）；`verify: [test]` 门控在本闭包审计会话独立复跑绿（exit 0，3991/0/0/1 零新增失败，计数对账注记见 `## Verification`）；双 checker 复跑持平基线；零生产代码改动证明在案；owner doc / 路由行 / 裁决文件 / 日志同步均实仓复核无冲突。frontmatter `status: active` 保持 = ledger 协议（完成态派生，不写 completed）。

Closure Audit Evidence:

- dispatch audit #audit-20260910-0554-2026-09-10-0425-1-m20-fix-method-baseline-family-adjudication-1-bca51564 to 2026-09-09-210030-mission-driver models={exec:zhipuai-coding-plan/glm-5.3-flash,aud:zhipuai-coding-plan/glm-5.3-flash}
- accepted #audit-20260910-0554-2026-09-10-0425-1-m20-fix-method-baseline-family-adjudication-1-bca51564：独立闭包审计通过——Phase 1 owner doc `finding-remediation-method.md` 七要素 + index/README 路由、Phase 2 裁决文件 §1~§5（93 条全量映射/逐族选择-替代-残余风险/跨轮继承/M2.8 六分片）、Phase 3 零改动证明与双 checker 持平基线逐项实仓复核；`mvn test` 本会话复跑 exit 0（3991/0/0/1，与去重基线真值一致，零新增失败）+ `mvn clean install -DskipTests` exit 0（156/156）。exec=aud 同模型单席审计如实声明（zhipuai-coding-plan/glm-5.3-flash）。
