# 2026-08-01-1357-2-mq-q1-mutation-testing-impl 变异测试有效性 Phase 2 实现

> Plan Status: active
> Last Reviewed: 2026-08-01
> Mission: audit-remediation
> Work Item: MQ Q1（Phase 2 实现）
> Source: `docs/backlog/audit-remediation-roadmap.md` §Milestone MQ Q1（line 674 工作项表 + line 783-784 维度说明）；`docs/architecture/quality-engineering/README.md` §实施顺序裁决（Q1 位 2）
> Related: 设计文档 plan `docs/plans/2026-08-01-1158-2-mq-q1-mutation-testing-design-doc.md`（Phase 1 done）；设计文档 `docs/architecture/quality-engineering/mutation-testing.md`（已收敛的实施契约，本计划引用为范围与验收依据）；sibling plan `2026-08-01-1357-3-mq-q4-fault-injection-impl.md`（Q4 Phase 2，Q1↔Q4 协同——Q1 盲区类清单即 Q4 优先覆盖路径）
> Audit: required

## Current Baseline

> 本计划是 MQ 文档先行工作流的 **Phase 2 实现**：以经独立子代理 3 轮审查收敛的设计文档 `mutation-testing.md` 为实施契约。基线盘点引用设计文档 §1（已核验证据），不重推导。

**audit-remediation 主线**：全 done + MR6 CLOSED；`mvn clean install -DskipTests` 156 模块全绿；`mvn test` 1903 测试 0 failures/0 errors。

**Q1 现状（Phase 1 已诊断，设计文档 §1）**：
- **全仓零 pitest 配置**（核验 `rg "pitest" --glob '*.xml'` EXIT=1）——1903 测试的 mutation score **完全未知**（MA5 审计反复写「需 pitest 运行」但从未执行）。
- **Nop 代码生成产物是关键噪声源**（设计文档 §1.3，两类生成包合计 **352 个生成类**）：
  - 类一 `_gen` 包：finance 36 + mfg 31 + inv 21 = 88 类
  - 类二 `api.beans` + `api.crud` 包：finance 72+36 + mfg 62+31 + inv 42+21 = 176+88 = 264 类（是 `_gen` 的 3 倍）
  - 核验 `rg -l "__XGEN_FORCE_OVERRIDE__" --glob '*.java' module-{finance,manufacturing,inventory}` = 264 文件
- **MA5 测试/mutation 比历史快照（非实时，纯引用）**：finance 0.47（137 估算 mutation）/ mfg 0.39-0.41 / hr 0.16（全域最低）；inv 无 MA5 历史（Phase 2 首次建立基线）。
- 目标域裁决（设计文档 §1.4）：**finance / mfg / inv 三域首跑**（高复杂度优先）；其余 16 域（含 hr 0.16）successor。

**剩余差距**：无 pitest 插件；无 mutation score 基线；无存活变异体分类工作流；无 Q4 可消费的盲区类清单。

## Goals

> 范围 = 设计文档 §3.4 裁决（pitest + `<profile><id>mutation</id>` 激活 + 默认全集 + 三域首跑）+ §6 裁决（CI C-2 nightly 软门控）。本计划是设计文档的实施执行。

- **pitest 接入（设计文档 §4.1）**：根 pom `<profile><id>mutation</id>` 声明 `pitest-maven`（默认不激活），`targetClasses` 限定三域、`excludedClasses` 排除**全部生成包**（`_gen` + `api.beans` + `api.crud` = 352 类）+ 测试类。
- **三域首跑基线（设计文档 §4.2）**：finance → mfg → inv 首跑，产出 HTML+XML 报告 + per-class mutation score + 耗时实测，落盘替换 §1.2 MA5 估算表为 pitest 实测值。
- **存活变异体分类（设计文档 §4.3）**：三分类（生成噪声=零 / 等价变异 / 真实盲区）工作流落地，产出盲区类清单（设计文档 §8.2 格式，供 Q4 消费）。
- **CI C-2 接线（设计文档 §6.4）**：新建 `.github/workflows/mutation.yml`（nightly 软门控：mutation score 退化 → fail，对齐 compliance-baseline 单向收紧架构）。

## Non-Goals

- **不实现任何生产代码变更**（设计文档 §4.4：pitest 在 `target/classes` 字节码层插桩，不改源码；零 ORM/契约/生产代码变更）。
- **不修改 nop-entropy 父 pom**（设计文档 §3.4 R4：profile 声明在根 pom 子层，不触及平台源码；Phase 2 复核继承关系，冲突则退候选 P1 per-module）。
- **不覆盖全域首跑**（设计文档 §1.4：首跑聚焦 finance/mfg/inv；其余 16 域 successor）。
- **不修复发现的测试盲区**（设计文档 §2.2：Q1 产出盲区清单；修复属 Q4 故障注入覆盖 + 后续 MR3-style 测试补强，非本期）。
- **不改常规 `mvn test` 1903 测试基线**（profile 默认不激活，保护既有基线不受影响）。
- **不首跑 hr 0.16**（设计文档 §1.4：hr 低比根因是真实异常路径缺口，修复路径是补异常路径测试而非先跑 pitest；首跑对 hr 无新信息增量）。

## Task Route

- Type: `implementation-only change`（构建配置 pitest 接入 + 首跑 + 分类分析 + CI workflow；零 ORM/契约/生产代码变更）
- Owner Docs: 设计文档 `docs/architecture/quality-engineering/mutation-testing.md`（收敛实施契约）；`docs/architecture/quality-engineering/README.md`；MA5 审计报告 `docs/audits/2026-07-29-1430-arm-ma5-*-test-coverage.md`
- Skill Selection Basis: AGENTS.md 强制技能扫描完成。主体是 pitest Maven 插件配置 + 报告分析，非编写测试用例；但理解测试基线结构（`JunitAutoTestCase`/快照/`_gen` 分布）须参照 `nop-testing`。设计文档 §4 亦明示「Phase 2 起草加载 nop-testing skill」。Profile/pom 配置阶段标 `Skill: none`（构建工程），分析阶段参照 nop-testing 测试结构。

## Infrastructure And Config Prereqs

- pitest 须兼容 JDK 21（本项目 CI `setup-java java-version: '21'`，设计文档 §3.1.1）——Phase 2 锁定 pitest 1.15+ 具体 Java 21 兼容版本。
- 首跑耗时不可预知（设计文档 §3.4 R1：零 pitest 历史，三域首跑远慢于常规 `mvn test`）——Phase 2 实测后定 CI timeout。

## Execution Plan

### Phase 1 - pitest 插件 profile 接入 + 父 pom 继承复核

Status: planned
Targets: `app-erp/pom.xml`（根 pom，`<profiles>`/`<build>`）
Skill: none

- Item Types: `Add | Decision`
- Prereqs: 设计文档审查收敛（已满足）

- [ ] Add: 在根 pom `<profile><id>mutation</id>` 内声明 `pitest-maven`（设计文档 §3.3/§4.1 step 1）
      - version：1.15.x Java 21 兼容版本（Phase 2 锁定具体版本）
      - `targetClasses`: `app.erp.fin.*` / `app.erp.mfg.*` / `app.erp.inv.*`（域包通配限定三域）
      - `excludedClasses`: `*.dao.entity._gen.*` + `*.dao.entity._gen._*` + `*.api.beans.*` + `*.api.crud.*` + `*.codegen.*` + `*Test*` + `*IT`（设计文档 §1.3 两类全部生成包 352 类 + 测试类）
      - `targetTests`: `app.erp.fin.*Test` / `app.erp.mfg.*Test` / `app.erp.inv.*Test`
      - `outputFormats`: HTML + XML；`timestampedReports`: false（稳定输出路径）
      - **不配置 `<mutators>` = pitest 默认全集 `DEFAULT`**（对齐设计文档 §3.4 裁决2：首跑建完整可比基线，不收缩算子集；降噪靠 §4.3 分类工作流而非砍算子）
      - Skill: none
- [ ] Proof: 复核 profile 与 nop-entropy 父 pom 继承关系（设计文档 §3.4 R4）：`mvn -Pmutation help:effective-pom -pl module-finance/erp-fin-service` 确认 profile 生效 + pitest 配置可见
      - Skill: none
- [ ] Decision: 若继承冲突，退候选 P1（per-module，在 `erp-{fin,mfg,inv}-service` 三 pom 各声明）——裁决理由落盘（候选 + 替代）
      - Skill: none

Exit Criteria:

> 设计文档 §5 验收 1（profile 默认不激活）。全量 build/test 属 Closure Gates。

- [ ] profile 默认不激活（`mvn help:active-profiles` 不含 mutation；`mvn -Pmutation help:active-profiles` 含）+ pitest 配置可见

### Phase 2 - finance 单域 dry-run 确认全部生成包排除

Status: planned
Targets: finance 域 pitest dry-run
Skill: none

- Item Types: `Proof`
- Prereqs: Phase 1 done

- [ ] Proof: finance 单域 dry-run（设计文档 §4.1 step 3）：`mvn -Pmutation test-compile org.pitest:pitest-maven:mutationCoverage -pl module-finance/erp-fin-service`
      - Skill: none
- [ ] Proof: 核验 HTML 报告：`app.erp.fin.dao.entity._gen._*` / `app.erp.fin.api.beans.*` / `app.erp.fin.api.crud.*` 类**均不出现**在变异目标（设计文档 §5 验收 3 关键约束闭环）
      - Skill: none

Exit Criteria:

- [ ] finance HTML 报告中全部生成包（`_gen` + `api.beans` + `api.crud`）内类无变异记录（双控生效：域包限定 + 两类生成包排除）

### Phase 3 - 三域首跑基线 + 耗时实测

Status: planned
Targets: finance / mfg / inv 三域 pitest 报告 + baseline 落盘
Skill: nop-testing

- Item Types: `Add | Proof`
- Prereqs: Phase 2 done（生成包排除确认）

- [ ] Proof: finance 首跑（`mvn -Pmutation -pl module-finance/erp-fin-service -am ... mutationCoverage`）→ HTML/XML 报告 + mutation score + 存活变异体清单
      - Skill: nop-testing
- [ ] Proof: mfg 首跑（`module-manufacturing/erp-mfg-service`）
      - Skill: nop-testing
- [ ] Proof: inv 首跑（`module-inventory/erp-inv-service`，inv 首次建立 mutation 基线，无历史对照）
      - Skill: nop-testing
- [ ] Add: 三域 mutation score 落盘到 `docs/architecture/quality-engineering/mutation-baseline.md`（**单一固定路径**，供 Phase 5 CI XML 解析器与 §1.2「已被取代」注记解析；设计文档 §4.2 step 5 的「或 `docs/testing/`」在此收敛为本固定路径），替换设计文档 §1.2 MA5 估算表为 pitest 实测值（finance 实测 ≠ 137 估算）；记录三域各自首跑耗时
      - Skill: nop-testing

Exit Criteria:

- [ ] 三域 HTML+XML 报告 + per-class mutation score 落盘；inv 首次有 mutation 基线；耗时基线记录（CI 调度决策输入）

### Phase 4 - 存活变异体分类工作流 + Q4 协同产物

Status: planned
Targets: 分类清单（`mutation-baseline.md` 或独立文件）；Q4 可消费盲区类清单
Skill: nop-testing

- Item Types: `Add | Proof`
- Prereqs: Phase 3 done

- [ ] Proof: 从 pitest XML 提取全部存活变异体，按类聚合（设计文档 §4.3 step 1-2）
      - Skill: nop-testing
- [ ] Proof: 三分类——(1) 生成代码噪声（`_gen`+`api.beans`+`api.crud`，应为零，与验收 3 一致；若非空说明配置失效须修配置）；(2) 等价变异（getter/setter/trivial return）；(3) 真实盲区
      - Skill: nop-testing
- [ ] Add: 真实盲区按域+类聚合成清单，按设计文档 §8.2 格式落盘（FQCN + 存活变异体数 + 是否过账 dispatcher/Processor + Q4 可消费性），供 sibling plan Q4 消费
      - Skill: nop-testing
- [ ] Proof: 字节码插桩与 Nop 动态分发交互抽样复核（设计文档 §3.4 R3）——对异常存活抽样核验可复现性，排除假存活
      - Skill: nop-testing

Exit Criteria:

- [ ] 分类清单落盘（三类计数）；生成噪声计数为零；真实盲区清单格式符合 §8.2（首批协同交集仅 finance，设计文档 §8.1 边界）

### Phase 5 - CI C-2 nightly 软门控 + 初始阈值裁决

Status: planned
Targets: `.github/workflows/mutation.yml`（新建）；`mutation-baseline.md` YAML 基线块
Skill: none

- Item Types: `Add | Decision`
- Prereqs: Phase 3 done（首跑基线）

- [ ] Add: 新建 `.github/workflows/mutation.yml`（设计文档 §6.4）
      - `schedule: cron: '0 4 * * *'`（nightly，错开 Q6 clock-rollover `0 3 * * *`）+ `workflow_dispatch`
      - `setup-java 21` → 三域 `mvn -Pmutation ... mutationCoverage`（分 job 或串行）
      - **门控逻辑放 CI（对齐 F8 架构非字面复用解析器）**：python 解析 pitest **XML** 报告提取 per-domain mutation score，与 `mutation-baseline.md` YAML 基线块对比，actual < baseline → exit 1（单向收紧：退化 fail，提升鼓励非强制）。pitest 本身 pure reporter。须新写 pitest XML 解析逻辑（F8 解析文本，不同源）。
      - Skill: none
- [ ] Decision: 初始阈值裁决（设计文档 §6.3 R7）——「首跑实测值」vs「宽松过渡阈值」（允许逐步收紧）。裁决理由落盘（候选 + 替代 + 残留风险）。
      - Skill: none
- [ ] Proof: runner 上 pitest 耗时/资源实测（设计文档 §3.4 R6）→ 定 nightly timeout
      - Skill: none

Exit Criteria:

- [ ] mutation.yml 落盘 + pitest XML 解析逻辑可工作 + 初始阈值裁决落盘 + nightly timeout 定

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_04416d020ffegtV7Szfb50lOly`，独立子代理 fresh session cold context）— 0 BLOCKER / 0 MAJOR / 2 MINOR。全部 Current Baseline 实仓主张独立复核 PASS（`rg "pitest" --glob '*.xml'` EXIT=1 零配置、`__XGEN_FORCE_OVERRIDE__` 三域 264 文件、`_gen` 88 + api.beans/api.crud 264 = 352 非重叠、三目标 service 模块存在、根 pom 无 `<profiles>`、compliance.yml F8 单向收紧范式存在、设计文档 Review Record 3 轮收敛无 BLOCKER/MAJOR）。MINOR-1（Phase 1 未显式声明算子集决策）已采纳——Phase 1 增「不配置 `<mutators>` = 默认全集 DEFAULT」注。MINOR-2（基线落盘路径含「或」）已采纳——收敛为单一固定路径 `docs/architecture/quality-engineering/mutation-baseline.md`。无 BLOCKER/MAJOR → converged → 转 active。

## Closure Gates

> 设计文档 §5 验收判据为本计划 closure 契约。全量 `mvn clean install -DskipTests` + `mvn test`（profile 不激活）在此一次性运行。

- [ ] 范围内行为完成（设计文档 §5 验收 1-7）
  - pitest profile 默认不激活（`rg "pitest-maven" pom.xml` + `rg "<id>mutation</id>" pom.xml`；`mvn help:active-profiles` 不含 mutation）
  - 三域 mutation score 基线落盘（HTML+XML 报告 + per-class score；finance 实测替换 137 估算；inv 首次基线）
  - 全部生成包噪声排除验证（三域 HTML 中 `_gen`+`api.beans`+`api.crud` 类无变异记录；`excludedClasses` 含三类包通配）
  - 存活变异体分类清单落盘（三类计数；生成噪声=0；真实盲区清单格式符合 §8.2）
  - 首跑耗时基线记录
- [ ] 相关文档对齐：设计文档 `mutation-testing.md` 无未经批准偏离；§1.2 估算表标注「已被 Phase 2 实测值取代」；`docs/logs/{year}/{month}-{day}.md` 追加日志条目
- [ ] 已运行验证：`mvn clean install -DskipTests`（156 模块 BUILD SUCCESS）+ `mvn test`（0 failures / 0 errors，profile 不激活常规基线不变）；compliance checker 不新增命中（零生产代码变更）
- [ ] 无范围内项目降级为 deferred/follow-up（其余 16 域 successor 经设计文档 §1.4 显式 out-of-scope；盲区修复属 Q4/后续 plan）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中
- [ ] **实现与设计文档一致**（无未经设计文档 `mutation-testing.md` 批准的范围偏离）

## Deferred But Adjudicated

### 其余 16 域 mutation score 基线（含 hr 0.16）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 设计文档 §1.4 裁决三域首跑聚焦（高复杂度优先 + 避免全域首跑即爆炸）；分类工作流沉淀后扩展更可控。
- Successor Required: yes —— 触发条件：三域基线落盘 + 分类工作流沉淀后；hr 优先级取决于其异常路径测试补强（MA5/MR3）进度。

### C-3 per-commit 增量变异测试

- Classification: `watch-only successor`
- Why Not Blocking Closure: 设计文档 §6.3 裁决增量模式复杂性 + 首跑基线未建时增量无参照；nightly 软门控已覆盖持续追踪。
- Successor Required: yes —— 触发条件：三域基线稳定 + 团队 PR 节奏需更快反馈时。

### 真实盲区类修复

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 设计文档 §2.2 裁决 Q1 仅产出盲区清单；过账路径盲区修复属 Q4 故障注入覆盖（sibling plan），非过账路径属后续 MR3-style 测试补强。
- Successor Required: yes —— 触发条件：分类工作流产出真实盲区清单后，由 Q4 Phase 2 + 后续测试补强 plan 消费。

## Closure

Status Note: pending（独立结束审计后填写）

Closure Audit Evidence:

- Auditor / Agent: pending（独立结束审计子代理，新会话 fresh cold context）

Follow-up:

- 其余 16 域基线 successor（见上 Deferred）。
- 真实盲区修复由 sibling plan Q4 + 后续测试补强消费。
