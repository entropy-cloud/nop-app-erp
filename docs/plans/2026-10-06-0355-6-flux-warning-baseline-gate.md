# 2026-10-06-0355-6 flux 基线门禁化收官（warning 冻结基线 + parse 门禁 + 基线文档刷新）

> Plan Status: completed
> Last Reviewed: 2026-10-06
> Source: docs/analysis/2026-10-06-0352-perf-ux-round3-increment-analysis.md §2 D2 定案 + §3.2 F-R8/F-R12
> Related: 2026-10-05-0353 第二轮 §5 D2（触发项）、docs/architecture/flux-page-export-and-validation.md（既有基线）、docs/architecture/flux-integration-gotchas.md G-002
> Audit: required

## Current Baseline

- `npm run validate:flux` → `_tmp/flux-page-validation-report.json`：per-file 完整（totals{files:856, validated:856, errors:325, warnings:21254}，perFile[i].warnings[] 逐条含 path/code/message）——app 侧消费该 JSON 无需动保护区验证器（`nop-chaos-flux/flux-guide/scripts/validate-pages.mjs` 提供 --pattern/--level/--report/--max-errors，无 app 侧基线选项）。warning message 形态稳定可解析：`Unknown property "prop" for renderer type "renderer".`
- app `package.json` 无 yaml 解析库——parse 检查需新增 devDependency（js-yaml）。
- 7 手写页 warning 基线：103 条 100% unknown-property、0 error、接线族零残留（round-3 §2 D2 数据）。但批次 1-4 修复将进一步消灭 hybrid/download/level/labelField 族 warning——**冻结基线须在本 plan 执行时（批次 1-4 之后）新鲜测取**。
- warning 池漂移未裁决入档：18494→21254，新码 `unvalidated-component-target`×1154（生成层信息族）+ `duplicate-schema-id`×7（生成层）；files 856 vs `flux-page-export-and-validation.md:273` 记载 855。
- 死文件零门禁：period-close `main.page.yaml` 非法 YAML（`\'` 转义，PyYAML PARSE FAIL :191）flux 模式下不被解析，一旦激活即路由崩；visit-wizard 等死孪生同族腐化无任何检查。

## Goals

- app 侧新增门禁脚本（消费 `_tmp/flux-page-validation-report.json` + 全量 yaml parse）：
  ①7 手写页 warning **多重集冻结**（语义=禁止新增、允许收缩——上游 i18nEn 白名单化时降数不误报）；
  ②**全部被服务手写页**接线族零容忍——精确匹配规则：解析 message 中引号内 (prop, renderer)，fail 当 `prop ∈ {actionType, api, onEvent}`，或 `renderer == "button" 且 prop ∈ {action, args}`（子串匹配弃用；批次 1 改写后 25 个 report 页与 16 个孪生页的 api/actionType 族应归零，此规则防回归）；
  ③`totals.errors` 钉住 325（漂移即 fail，对齐既有 tools/validate-flux-fresh.sh 口径）；
  ④新码计数器（unvalidated-component-target/duplicate-schema-id）冻结当前值；
  ⑤全量 `*.page.yaml`/`*.flux.yaml` parse 检查（含死文件；解析器=js-yaml devDependency，验收时以 PyYAML 一次性交叉复核）。
  手写页宇宙枚举：确定性路径清单（全部手写 `*.flux.yaml` 31 个 + 7 个手写 AMIS 主页面，排除 target/_dump/_gen），落为脚本内显式清单或 glob+排除规则——新增手写页必须显式入册，防静默逃逸。
- period-close `main.page.yaml` `\'`→`''` 修复（parse 门禁诞生即绿）。
- `flux-page-export-and-validation.md` 基线刷新（warning 池新值+新码裁决登记+856 对账）+ package.json 接线（`check:flux-baseline` 之类命名，对齐既有 check:* 族）。

## Non-Goals

- 不改 nop-chaos-flux 验证器（外部保护区）。
- 不做 325 error 基线族的消解（variant="primary" 生成层既有裁决不变）。
- 不处理死孪生归宿政策（删除 vs 标记——Deferred，须 owner doc）。

## Task Route

- Type: `implementation-only change`（工具 + 文档 + 单文件 YAML 修复）
- Owner Docs: docs/architecture/flux-page-export-and-validation.md、docs/architecture/flux-integration-gotchas.md
- Skill Selection Basis: Skill: none（工具脚本；审计方法无匹配技能）

## Infrastructure And Config Prereqs

- 依赖 `npm run validate:flux` 先行产出新鲜报告（脚本前置步骤；CI/本地均同）。
- 新增 devDependency：`js-yaml`（parse 检查用；当前 package.json 无 yaml 库）。

## Execution Plan

### Phase 1 - 死文件修复与基线测取

Status: completed
Targets: period-close-wizard/main.page.yaml；_tmp 报告
Skill: none

- Item Types: `Fix | Proof`

- [x] Fix: `\'` → `''`（6 处；PyYAML 解析通过）——**执行期发现（已修复）**：死文件修复后导出面激活，同时暴露批次 2 的 YAML 引号终止事故（prj/cs kanban.flux.yaml 内层 /r/ URL 单引号嵌在单引号表达式标量内致 parse fail，FLUX_PAGE_ERROR_COUNT 2/页数 854；批次 2 门禁仅 grep totals 行而掩蔽导出失败）——内层改双引号修复，模块重装后导出恢复 FLUX_PAGE_ERROR_COUNT: 0/856 页；本门禁的 parse 检查即为此类事故的机制性防线
  - Skill: none
- [x] Proof: 批次 1-4 后重跑 validate:flux 测取冻结基线落 `tools/flux-baseline.json`（errors=325 钉住；7 页多重集现值 8 条[原 103——手写页 i18nEn 族随 flux dist 重建从报告消失，基线文档已注记]；新码计数器 1154/7）
  - Skill: none

Exit Criteria:

- [x] 死文件双解析器通过；基线 JSON 在案且 errors=325

### Phase 2 - 门禁脚本与接线

Status: completed
Targets: `tools/check-flux-baseline.mjs` + package.json
Skill: none

- Item Types: `Add`

- [x] Add: 五项检查实现（errors 钉住/7 页多重集冻结[禁止新增+允许收缩]/手写页宇宙接线族零容忍[(prop,renderer) 精确匹配]/新码计数器/全量 yaml parse[js-yaml devDependency]），输出可读 fail 报告
  - Skill: none
- [x] Add: package.json `check:flux-baseline` + `check:flux-baseline:self-test`（6 断言：errors 漂移/接线族/冻结新增/计数器增长/clean+收缩允许/parse 检出）
  - Skill: none
- [x] 真实树负路径验证：死孪生注入坏 yaml → gate exit 1（yaml-parse 检出）→ 还原 → exit 0
  - Skill: none

Exit Criteria:

- [x] 脚本对当前树 exit 0；self-test 6/6 + 真实树注入/还原双向验证在案

### Phase 3 - 基线文档刷新与收官

Status: completed
Targets: docs/architecture/flux-page-export-and-validation.md；docs/analysis/2026-10-06-0352 报告收官注记
Skill: none

- Item Types: `Add`

- [x] 基线刷新：flux-page-export-and-validation.md 新增 §7b（warning 池漂移裁决[含手写页 i18nEn 族消失现象注记]/新码生成层裁决/856 对账/check:flux-baseline 用法/导出计数与 validation error 两层口径辨析）
  - Skill: none
- [x] round-3 报告 D2 收官注记（本 plan 完成态）
  - Skill: none

Exit Criteria:

- [x] 文档与报告一致；`docs/logs/2026/10-06.md` 收官条目在案

## Draft Review Record

- Independent draft review iteration 1: NEEDS REVISION (agent_3826a1be, 2026-10-06) — js-yaml devDependency 未声明；零容忍匹配规则钉死为 (prop,renderer) 解析精确匹配；手写页宇宙枚举方式未指定；Deferred 触发条件 ×2；:self-test 伴侣惯例；验证器 flags 表述精确化 → 全部采纳修订
- Independent draft review iteration 2: ACCEPT (agent_e418a158, 2026-10-06) — 迭代 1 全部要求实证级修复核验通过，无残留

## Closure Gates

- [x] 范围内行为完成（五项检查 + 接线 + 死文件修复 + 基线文档刷新）
- [x] 相关文档对齐（flux-page-export-and-validation.md §7b；G-002 增补归批次 1/2 已在案）
- [x] 已运行验证（validate:flux FLUX_PAGE_ERROR_COUNT: 0/errors=325 + check:flux-baseline exit 0 + self-test 6/6 + 真实树注入/还原双向验证）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录（迭代 1 agent_3826a1be + 迭代 2 agent_e418a158）
- [x] 文本一致性已验证
- [x] 结束审计由独立子代理（新会话）执行（agent_e1a4cc52 2026-10-06：**ACCEPT** 附 2 项回填条件[日志条目+本节回填]，已实施；a-h 八项实证 + 独立复跑 self-test 6/6/门禁 exit 0/负路径注入还原双向）
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 死孪生文件归宿政策

- Classification: `optimization candidate`
- Why Not Blocking Closure: 删除 vs 豁免标记须 owner doc（view-and-page-strategy/ui-patterns）裁决；parse 门禁已止血腐化
- Reopen Trigger: ui-patterns 或 view-and-page-strategy 增补死文件条款时
- Successor Required: `no`

### 325 error 基线族消解

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: variant="primary" 生成层外部漂移族（既有裁决），app 仓不可达
- Reopen Trigger: 外部 codegen variant="primary" 漂移族清零；或相关 roadmap successor 启动
- Successor Required: `no`

## Closure

Status Note: 五项基线门禁落地并双向验证（self-test 6/6 + 真实树注入/还原）；死文件修复 + 批次 2 引号事故暴露与修复；基线文档 §7b 刷新；冻结基线以修复后干净态测取。D2 Deferred 项收官。

Closure Audit Evidence:

- Auditor / Agent: agent_e1a4cc52（见上）

Follow-up:

- (none)
