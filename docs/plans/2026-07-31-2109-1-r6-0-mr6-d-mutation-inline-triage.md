# 2026-07-31-2109-1-r6-0-mr6-d-mutation-inline-triage R6.0 — MR6 D-mutation facade + 内联多步 mutation per-method triage 展开

> Plan Status: completed
> Last Reviewed: 2026-07-31
> Source: `docs/backlog/audit-remediation-roadmap.md` §MR6 R6.0
> Related: `docs/audits/arm-index.md` §P1 详细清单 P1-MA3-062；`docs/plans/00-plan-authoring-and-execution-guide.md` §R*.0 展开机制；`docs/plans/2026-07-31-0958-1-r3-0-mr3-p1-finding-expansion.md`（R3.0 同构先例）；`docs/architecture/processor-extension-pattern.md`（真相源）
> Mission: audit-remediation
> Work Item: R6.0
> Audit: required

## Current Baseline

- **MR5（S-mutation 架构合规）全部 done**：R5.1-R5.8 已完成，149 个 S-mutation per-mutation Processor 文件已填充真实逻辑，30 个 facade Processor 的 S-mutation 方法已替换为单行委托。S-mutation 拆分确实完成且正确——本 plan **不重开 MR5**。
- **P1-MA3-062 已登记入 arm-index**（line 288），定位为"Processor per-mutation 拆分纪律系统性违反"，归属 MR6。该 finding 是 MR5 范围定义遗漏的缺口（MR5 用 owner doc 不承认的"S/D 之分"伪概念把 D-mutation 打包豁免，违反真相源）。
- **真相源（`docs/architecture/processor-extension-pattern.md`）**：
  - `:7` — "任何 ≥3 步的方法都应拆到 Processor"
  - `:29` — "每个 `@BizMutation` 方法对应一个独立的 Processor 类"
  - `:42` — "不允许多个 mutation 共用同一个 Processor 类。这是强制架构纪律"
  - `:44-47` — 例外清单（仅"纯查询 ≤2 步 / 单步状态翻转 / 标准 CRUD"豁免，**无 S/D 之分**）
- **P1-MA3-062 实仓扫描两类违反（arm-index line 288 记录）：**
  - **类别 A（多 mutation 共用 Facade Processor，违反 :42/:119）**：arm-index 记录 26 个裸 `<Entity>Processor` facade 持有 ~88 个 D-mutation 入口方法。**本 plan 起草期实仓扫描**（`find … -name "*Processor.java" -path "*/processor/*"` + grep `IServiceContext` + 排除 `extends Abstract`）发现 **40 个** facade Processor 含 ≥2 个公共 `IServiceContext` 入口方法——与 arm-index 记录的"26"存在差异。差异根因：MR5 完成后部分 facade 的 S-mutation 已瘦身为单行委托，但仍可能持有 D-mutation 入口；另部分 facade 可能经 MR5 后仅剩 S-mutation 委托（不再是违规）。**精确的类别 A 违规计数须由 R6.0 Phase 1 逐 facade 判定**（每个 facade 的公共入口方法中，哪些是 D-mutation[非标准审批六动作]、是否 ≥2 个），不可在起草期锁定。
  - **类别 B（BizModel `@BizMutation` 零 Processor 引用，违反 :5/:7）**：arm-index 记录 89 个 BizModel 含 244 个内联 `@BizMutation` 方法。**本 plan 起草期实仓扫描**（`find … -name "*BizModel.java" -path "*/entity/*"` + grep `@BizMutation` 且 grep `Processor` 零命中）发现 **88 个 BizModel / 243 个内联 mutation**——与 arm-index 记录的"89/244"在 ±1 grep 粒度噪声内一致。高密度代表与 arm-index 一致：`ErpCsTicketBizModel`(9)、`ErpB2bEdiDocBizModel`(8)、`ErpQaNonConformanceBizModel`(7)、`ErpPrjProjectBizModel`(7)、`ErpHrSalarySimulationBizModel`(7)、`ErpCtSignatureRequestBizModel`(7)。
- **roadmap §MR6 表已含 R6.1-R6.7 粗粒度工作项行**（不同于 R3.0 执行前 R3.x 为占位行——MR6 的 R6.1-R6.7 已有粗略域分组与 facade/BizModel 枚举）。R6.0 的职责是**细化**这些行：对类别 B 243 个 mutation 逐方法判定豁免边界（≥3 步须拆 vs ≤2 步/单步翻转合法豁免），并将精确的须拆 mutation 清单 + 合法豁免清单回填到对应 R6.x 行 + arm-index。
- **MV（全量验证）+ MG（知识沉淀）已全部 done**：MR6 是 MV/MG 收口后新登记的债务里程碑，与 MV/MG 时序无关，独立推进至 R6.8（roadmap 依赖图标注 MV2b）。
- **MA6/MA7 工作项状态列滞后**（已知 bookkeeping 不一致，非本 plan 范围）：roadmap §MA6/MA7 表 A6.1-A6.4 / A7.1-A7.3 Status 列仍标 `ready`，但 Work Item Details（line 360-365）+ R3.0 plan closure audit 证据 + MR3 依赖链（MR3 done 要求 MA5+MA6+MA7 done）均确认已 done。此为显示层滞后非活跃 todo，不影响本 plan。

剩余差距：roadmap §MR6 表 R6.1-R6.7 行缺少精确的须拆 mutation 清单与合法豁免清单；类别 A 精确违规 facade 数量未定（26 vs 40 须逐 facade 判定）；类别 B 243 个 mutation 未逐方法判定 ≥3 步 vs 豁免。

## Goals

- 对**类别 B** 全部 ~243 个内联 `@BizMutation` 逐方法判定：≥3 步（须拆到 `<Entity><Method>Processor`）vs ≤2 步 / 单步状态翻转（合法豁免，登记豁免清单引用 `processor-extension-pattern.md:44-47`）。
- 对**类别 A** 全部 facade Processor 逐 facade 判定：哪些公共入口方法是 D-mutation（非标准审批六动作）、是否 ≥2 个 D-mutation 共用一个 facade（违反 :42）、须拆为哪些 `<Entity><Method>Processor`。
- 将 triage 结果按域分组，**细化** roadmap §MR6 表 R6.1-R6.7 行——每行附精确须拆 mutation 清单（entity × method）+ 合法豁免清单指针；若域分组与现有 R6.1-R6.7 边界不符，调整行边界并记录理由。
- 产出**豁免登记文件**（合法 ≤2 步 / 单步翻转 mutation 清单 + 判定依据），作为 R6.8 完成判据核验的对照基准。
- 更新 arm-index P1-MA3-062：triage 结果交叉引用 + 类别 A 精确 facade 计数 + 类别 B 须拆/豁免计数。
- 更新 roadmap R6.0 Status `todo`→`done`。

## Non-Goals

- 实际代码重构（新建 `<Entity><Method>Processor` / facade 瘦身 / BizModel 改 `@Inject` Processor / beans.xml 重注册）——属 R6.1-R6.7。
- 全量验证 + 完成判据核验——属 R6.8。
- 重审或重开 MR5（S-mutation 拆分确实完成且正确）。
- 重新审计 P1-MA3-062 finding 的严重性或评级（R6.0 忠实执行 finding 的 triage，不重新评级）。
- 修复 MA6/MA7 状态列滞后（已知 bookkeeping 不一致，非 MR6 范围）。
- P0 发现（经即时通道，不进批量）。

## Task Route

- Type: `verification or audit work`（research/triage + roadmap/arm-index 文档展开 + 状态 bookkeeping，零代码变更）
- Owner Docs: `docs/architecture/processor-extension-pattern.md`（真相源 :7/:29/:42/:44-47）；`docs/audits/arm-index.md` §P1 详细清单 P1-MA3-062；`docs/backlog/audit-remediation-roadmap.md` §MR6
- Skill Selection Basis: R6.0 的核心工作是应用 `processor-extension-pattern.md` 的 per-mutation 判定规则对 mutation 逐方法 triage。`nop-backend-dev` SKILL.md 含 Processor per-mutation 纪律决策门 + 反模式自检表（路由到真相源），故 Phase 2 triage 标记 `Skill: nop-backend-dev`（加载其判定规则作为 triage 准绳）。Phase 1 机械扫描与 Phase 3 文档展开标记 `Skill: none`（纯 grep + 文档 bookkeeping）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 - 类别 A facade D-mutation 入口扫描 + 类别 B BizModel mutation 全量清点

Status: completed
Targets: 全域 `*-service/.../processor/*Processor.java`（类别 A）+ `*-service/.../entity/*BizModel.java`（类别 B）
Skill: none

- Item Types: `Proof`
- Prereqs: MR5 done（已满足——facade S-mutation 已瘦身，D-mutation 入口可清晰识别）

- [x] Proof: 类别 A 精确扫描——对全部 facade Processor（起草期实测 40 个含 ≥2 公共 `IServiceContext` 入口方法的 facade），逐 facade 列出公共入口方法，标注每个方法是 S-mutation（标准审批六动作：submitForApproval/approve/reject/reverseApprove/withdrawApproval/cancel）还是 D-mutation（域特定操作如 settle/executeDepreciation/generateMove/convertToCustomer/writeOff/issue/honor）。产出"facade × 入口方法 × S/D 分类"表。**类别 A 违规定义**：facade 持有 ≥2 个 D-mutation 入口方法（违反 :42）。仅持有 S-mutation 委托（MR5 已拆）或仅 1 个 D-mutation 的 facade 不属类别 A 违规。
  - Skill: none
  - **结果**：40 个 ≥2 入口 facade → **23 个类别 A 违规**（持 ≥2 D-mutation）+ 16 个纯 S-mutation 委托（D=0，非违规）+ 1 个单 D-mutation facade（R6.8 backstop）。23 违规 facade 持 **92** D-mutation 入口 + **7** ≤2 步查询入口。解决"26 vs 40"差异：精确违规=23。详见 `docs/architecture/processor-per-mutation-exemption-registry.md` §B + roadmap §MR6 "R6.0 triage 展开"。
- [x] Proof: 类别 B 精确扫描——对全部含 `@BizMutation` 且零 Processor 引用的 BizModel（起草期实测 88 个 / 243 mutation），逐方法列出：BizModel 类名 × `@BizMutation` 方法名 × 方法体语句步数（粗计：方法体内独立逻辑语句数——`setStatus`+`updateEntity`=1 步翻转；含校验+状态写+跨域调用+副作用≥3 步）。产出" BizModel × mutation × 步数"全量表。
  - Skill: none
  - **结果**：**88 BizModel / 234 真实 `@BizMutation`**（`grep -c` 243 含 ~10 javadoc `{@code @BizMutation}` 引用；严格 `^@BizMutation` 注解口径=234）。分布：hr 15/46、quality 6/25、finance 11/23 居前。与 arm-index "89/244" 在 ±1 grep 噪声内一致。

Exit Criteria:

- [x] 类别 A "facade × 入口方法 × S/D 分类"表产出，类别 A 违规 facade 计数确定（解决 26 vs 40 差异）— 精确 23 违规 facade / 92 D-mutation
- [x] 类别 B "BizModel × mutation × 步数"全量表产出，总 mutation 数与 arm-index 记录一致（±grep 粒度噪声）— 88 BizModel / 234 mutation

### Phase 2 - 逐方法 triage：≥3 步须拆 vs ≤2 步合法豁免 + 域分组

Status: completed
Targets: Phase 1 产出的类别 A + 类别 B 全量表
Skill: `nop-backend-dev`

- Item Types: `Decision | Proof`
- Prereqs: Phase 1 全量清点完成

- [x] Decision: 类别 B 逐方法 triage——对 ~243 个内联 mutation，按 `processor-extension-pattern.md:44-47` 例外清单逐方法判定：
  - **须拆**（≥3 步 / 含跨域编排 / 含校验+状态写+副作用复合逻辑）→ 标记须拆为 `<Entity><Method>Processor`
  - **合法豁免**（≤2 步 / 单步状态翻转 `setStatus`+`updateEntity` 且无跨域编排 / 纯查询 `@BizQuery`）→ 标记豁免，引用 :44-47 具体条款
  - **边界争议**（2-3 步含轻量校验）→ Decision 记录判定理由 + 残留风险
  - Skill: `nop-backend-dev`
  - **结果**：234 mutation → **须拆 164** + **合法豁免 70**。判定准绳：纯单实体状态翻转（require+守卫+setStatus+至多一次 updateEntity+可选审计 writeAction，无实体创建/跨域/计算/非平凡副作用）→ 豁免（:46）；含跨域 IBiz/引擎派发/循环副作用/多实体写/实体创建/金额计算/非平凡副作用方法 → 须拆。边界争议 14 项 adjudication 记录于 registry §C。
- [x] Decision: 类别 A 逐 facade triage——对类别 A 违规 facade（持有 ≥2 D-mutation 入口），逐 D-mutation 确定须拆出的 `<Entity><Method>Processor` 类名 + 主流程骨架（`process()` + protected step 列表，对齐 :80-97）。facade 被多 mutation 共享的辅助方法确定归属（域专属基类上提 vs 保留 `@Inject` 独立 Service）。facade 瘦身后是否可删除（若 S-mutation 已委托且 D-mutation 全拆出则 facade 可删）。
  - Skill: `nop-backend-dev`
  - **结果**：23 facade × 92 D-mutation 各定目标 Processor（命名 `<Entity><Method>Processor`）。处置：持 S-mutation 委托的 facade→slim 保留；纯 D-mutation facade→delete-after-extract。7 个 ≤2 步查询入口（:45）保留 facade。
- [x] Decision: 域分组验证——将须拆 mutation 按域归入 roadmap 现有 R6.1-R6.7 边界。若某域须拆 mutation 跨越现有行边界（如 finance 拆出的 facade 同时属 R6.1 已枚举与未枚举范围），调整行边界并记录理由。确认 R6.1-R6.7 行的域覆盖无遗漏无重叠。
  - Skill: none
  - **结果**：现有 R6.1-R6.7 域边界覆盖无遗漏无重叠，**无需调整**。实仓复核修正：R6.3 assets facade 枚举 10→4（5 个为 S-mutation 纯委托 D=0）；R6.1 finance 移除 ErpFinPostingProcessor/ErpFinBudgetScenario（同因）。
- [x] Proof: triage 结果产出结构化清单：
  - **须拆清单**（entity × method × 目标 Processor 类名 × 所属 R6.x 行）
  - **合法豁免清单**（entity × method × 豁免条款引用 :44-47 × 所属域）
  - **类别 A facade 处置清单**（facade × 须拆 D-mutation × 瘦身后保留/删除）
  - Skill: none
  - **结果**：须拆清单 256（catB 164 + catA 92）+ 豁免清单 77（catB 70 + catA 7 查询）+ 类别 A 处置清单 23 facade 见 roadmap §MR6 "R6.0 triage 展开" + `docs/architecture/processor-per-mutation-exemption-registry.md`。R6.x 须拆分布：R6.1=41 / R6.2=31 / R6.3=22 / R6.4=14 / R6.5=7 / R6.6=57 / R6.7=84。

Exit Criteria:

- [x] 类别 B 全部 mutation 判定完成（须拆 / 合法豁免 / 边界争议附理由），无未判定项 — 234 全判定（164 须拆 / 70 豁免 / 14 边界 adjudication 记录）
- [x] 类别 A 全部违规 facade 处置方案确定（每 D-mutation 有目标 Processor 类名）— 23 facade / 92 D-mutation 全定目标 Processor
- [x] 域分组与 R6.1-R6.7 行边界对齐，无遗漏无重叠 — 现有边界无需调整

### Phase 3 - roadmap R6.x 行细化 + 豁免登记 + arm-index 回填 + 日志

Status: completed
Targets: `docs/backlog/audit-remediation-roadmap.md` §MR6 表；豁免登记文件；`docs/audits/arm-index.md` §P1 详细清单 P1-MA3-062；`docs/logs/2026/07-31.md`
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 2 triage 完成

- [x] Add: 细化 roadmap §MR6 表 R6.1-R6.7 行——每行 Work Item 列追加精确须拆 mutation 清单（entity × method × 目标 Processor），附须拆计数 + 合法豁免计数（指针指向豁免登记文件）。若 Phase 2 调整了行边界，更新 Work Item 描述 + Deps。R6.1-R6.7 行 Status 保持 `todo`。
  - Skill: none
  - **结果**：R6.1-R6.7 行 Work Item 各附须拆计数（41/31/22/14/7/57/84）+ 豁免计数 + facade 枚举 + 实仓复核修正注；新增"R6.0 triage 展开"详情节（per-R6.x 完整须拆清单 entity×method×目标 Processor）。Status 全 `todo`。
- [x] Add: 产出豁免登记文件 `docs/architecture/processor-per-mutation-exemption-registry.md`（合法 ≤2 步 / 单步翻转 mutation 清单 + 判定依据引用 :44-47 + 域分组），作为 R6.8 完成判据核验的对照基准（"所有须拆 mutation 已拆 + 豁免清单内 mutation 保留 BizModel"）。
  - Skill: none
  - **结果**：文件产出含判定规则 + 类别 B 70 豁免按域分组 + 类别 A 7 查询豁免 + 边界争议 adjudication 14 项 + 非变异守卫说明。
- [x] Add: 更新 arm-index P1-MA3-062：triage 结果交叉引用（须拆 N 项归 R6.x / 合法豁免 M 项登记 registry / 类别 A 精确 facade 计数 + D-mutation 计数）+ 修复状态列指向 R6.0 done。
  - Skill: none
  - **结果**：摘要行（line 128）+ 详细条目状态列（line 288）均更新为"R6.0 triage done"，交叉引用 roadmap §MR6 triage 展开 + registry，精确计数（23 facade/92 D-mut/88 BizModel/234 mutation/须拆 256/豁免 77）。
- [x] Add: 更新 roadmap R6.0 Status `todo`→`done`，Last Reviewed 注记本 plan id + triage 统计（须拆 N / 豁免 M / 类别 A facade K）。
  - Skill: none
  - **结果**：R6.0 行 Status `done`，Work Item 含 triage 统计（须拆 256 = catB 164 + catA 92；豁免 77 = catB 70 + catA 7）+ 指针。
- [x] Add: 追加 `docs/logs/2026/07-31.md` 条目（R6.0 triage：类别 A K facade / 类别 B 须拆 N + 豁免 M → R6.1-R6.7 细化）。
  - Skill: none
  - **结果**：日志条目追加（类别 A 23 facade/92 D-mut + 类别 B 88 BizModel/234 mutation → 须拆 256/豁免 77 → R6.1-R6.7 细化）。
- [x] Proof: 一致性复核——grep 确认 roadmap §MR6 R6.1-R6.7 行的须拆 mutation 清单与 Phase 2 须拆清单单向闭合；豁免登记文件的豁免 mutation 与 Phase 2 豁免清单单向闭合；须拆 + 豁免 = 类别 B 全量 mutation（无遗漏无重叠）；arm-index P1-MA3-062 计数与 triage 统计一致。
  - Skill: none
  - **结果**：(1) R6.1-R6.7 须拆计数和 41+31+22+14+7+57+84=256=catB 164+catA 92 ✓；(2) registry 豁免 70+须拆 164=234=catB 全量 ✓；(3) arm-index 计数与 triage 一致 ✓；(4) 类别 A 23 违规+1 单 D+16 纯 S=40 ≥2 入口 facade ✓。

Exit Criteria:

- [x] roadmap §MR6 R6.1-R6.7 行含精确须拆 mutation 清单 + 豁免计数指针，Status 全 `todo`
- [x] 豁免登记文件产出，每项豁免附 :44-47 条款引用
- [x] arm-index P1-MA3-062 triage 结果交叉引用回填，修复状态指向 R6.0
- [x] roadmap R6.0 Status=done
- [x] 双向可追溯复核通过（须拆清单 ↔ R6.x 行 / 豁免清单 ↔ registry / 须拆 + 豁免 = 全量）

## Draft Review Record

- Independent draft review iteration 1: accept (task `ses_0476580e5ffe89cEC6mT0koFUt`) because 计划是诚实、范围准确、可执行的 triage 契约——忠实遵循 R3.0 同构先例并正确适配（R6.1-R6.7 已存在为粗粒度行，R6.0 细化而非创建）；live baseline 诚实面对 26 vs 40 facade 差异并交由 Phase 1 逐 facade 判定；truth source 对齐正确（应用 :7/29/42/44-47，不发明 S/D 豁免）；零代码 triage 计划正确删除 build/test 门控；anti-slack / item typing / skill 记录 / exit criteria 均合规。采纳 2 项非阻塞观察（类别 A ≥2 D-mutation 阈值外的单 D-mutation 边界由 R6.8 backstop 兜底；Task Route type 选 `verification or audit work` 较 R3.0 的 `implementation-only change` 更准确），均无需修订。

## Closure Gates

> 本 plan 零代码/ORM/view 变更（纯 research/triage + roadmap/arm-index 文档展开 + 状态 bookkeeping），按 authoring guide 执行时规则 7 删除 typecheck/build/lint/test 验证门控。文档一致性以 grep 复核证明。

- [x] 范围内行为完成（类别 A + 类别 B 全量 mutation triage 完成，R6.1-R6.7 行细化，无遗漏）
- [x] 相关文档对齐（roadmap §MR6 表 + arm-index P1-MA3-062 + 豁免登记文件 + 日志）
- [x] 已运行验证（grep 双向可追溯复核 + compliance checker 不适用[纯文档零反模式变更]）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

_（无——R6.0 是 triage 展开器，所有 triage 结果要么归入须拆清单[后续 R6.x 执行]要么归入合法豁免清单[登记 registry]。须拆 mutation 是后续 R6.x 的工作项而非本 plan 的 deferred 项。）_

## Closure

Status Note: _（executed by mission driver 2026-07-31：3 Phase 全 done——类别 A 23 facade/92 D-mutation + 类别 B 88 BizModel/234 mutation triage → 须拆 256[catB 164 + catA 92]/豁免 77[catB 70 + catA 7 查询]；roadmap §MR6 R6.1-R6.7 细化 + R6.0 done；registry/arm-index/log 回填；grep 双向复核通过。）_

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（cold context 新会话，非执行者上下文），closure-audit role，2026-07-31。
- 审计范围与结论：本 plan 为零代码纯 research/triage + 文档展开。审计逐项复核：(1) Phase 状态/项一致性——3 Phase 全 `Status: completed`，执行项 + Exit Criteria 全 `[x]`，无残留 `- [ ]`；(2) Exit Criteria vs 实仓——逐文件读取验证四项交付物真实存在且非占位：registry(`processor-per-mutation-exemption-registry.md` 216 行，含判定规则 + 统计表 + catB 70 豁免按域逐方法 + catA 7 查询豁免 + 14 边界 adjudication + 非变异守卫) / roadmap §MR6(R6.0 行 done[行281] + R6.1-R6.7 行 todo 各附须拆计数 41/31/22/14/7/57/84 + "R6.0 triage 展开"详情节[行302+]) / arm-index P1-MA3-062(行288 状态"R6.0 triage done" + 精确计数 + 交叉引用) / 日志(`07-31.md` R6.0 条目[行3-17])；(3) Anti-Hollow——本计划无新代码，交付文档均含逐方法/逐 facade 实质内容非 `{}`/`return null` 占位；(4) 五点一致性——Plan Status=completed / 3 Phase Status=completed / 全 Exit Criteria `[x]` / Closure Gates（含本审计门已勾）/ Closure 证据全一致；(5) 计数闭合复核——须拆 256=catB164+catA92 且 R6.x 行和 41+31+22+14+7+57+84=256 ✓；豁免 77=catB70+catA7 且 registry §A 计 70（aps2+b2b9+contract7+crm3+cs8+drp1+fin1+hr16+inv3+mnt1+mfg2+prj8+pur2+qa7=70）✓；catB 全量 须拆164+豁免70=234 ✓；类别 A 23 违规+1 单 D+16 纯 S=40 ≥2 入口 facade ✓；(6) Deferred 诚实——Deferred But Adjudicated 节为空（须拆 mutation 属 R6.1-R6.7 successor 非本 plan deferred，无隐藏活缺陷）。结论：passes closure audit，批准关闭。
- 执行证据：`docs/architecture/processor-per-mutation-exemption-registry.md`（新建）；`docs/backlog/audit-remediation-roadmap.md` §MR6（R6.0 done + R6.1-R6.7 细化 + "R6.0 triage 展开"详情节）；`docs/audits/arm-index.md` P1-MA3-062（line 288 详细条目 + 行内摘要计数）；`docs/logs/2026/07-31.md`（R6.0 条目）。

Follow-up:

- _（非阻塞跟进；类别 A/B 须拆 mutation 的实际拆分属 R6.1-R6.7）_
