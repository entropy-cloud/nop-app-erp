# 变异测试有效性（MQ Q1）—— Phase 1 设计文档

> Owner Doc for Milestone MQ Q1（变异测试有效性）
> 创建日期：2026-08-01
> Plan：`docs/plans/2026-08-01-1158-2-mq-q1-mutation-testing-design-doc.md`
> 单一真相源依赖：本文档是 MQ 文档先行工作流 **Phase 1** 产物（设计/策略文档），**不实现任何代码/ORM/CI 变更**。Phase 2 实现 plan（pitest 插件接入 + 首跑基线 + 存活变异体分析）须在本文档审查收敛后方可起草。
> 上游真相源（**只引用**，不重推导，避免双真相源漂移）：
> - `docs/backlog/audit-remediation-roadmap.md` §Milestone MQ Q1（line 674 工作项表 + line 783-784 维度说明 + §横切关注点 §文档先行工作流 line 843-862）
> - `docs/architecture/quality-engineering/README.md`（Q0 范围矩阵 + 复杂度分级 + 实施顺序裁决基线，Q1 位 2）
> - `docs/plans/2026-08-01-1121-1-mq-q0-quality-gap-analysis-readme.md` §Current Baseline（Q1 NOT FOUND 实仓证据已核验：全仓零 pitest 配置）
> - MA5 测试覆盖深度审计报告（`docs/audits/2026-07-29-1430-arm-ma5-{finance,mfg,hr,assets}-test-coverage.md`，测试/mutation 比历史快照）
> - sibling plan `docs/plans/2026-08-01-1158-3-mq-q4-fault-injection-design-doc.md`（Q4，Q1↔Q4 协同——Q1 盲区类即 Q4 优先覆盖路径）

## 1. 现状评估

> 本节**引用**（非重推导）上游真相源已核验事实，每条标注可复现核验命令 + 核验日期，便于 Phase 2 plan 与独立审查复核。证据核验日期：2026-08-01（HEAD 含 R6.9 收口）。

### 1.1 全仓零 pitest 配置（mutation score 完全未知）

ERP 全仓无任何变异测试工具接入。1903 个测试的 mutation score（杀死变异体比例）**完全未知**——审计文档 MA5 反复写「需 pitest 运行」但从未执行。

- 核验命令（2026-08-01 复核零命中）：`rg "pitest" --glob '*.xml'`（工作目录 = nop-app-erp）→ **EXIT=1（零命中）**。覆盖范围：nop-app-erp 工作树内全部 `pom.xml` / `*.pom.xml` / CI workflow 内嵌 XML 段——无 `pitest-maven` 插件声明、无 `<mutationCoverage>` 配置、无 `org.pitest` 依赖。
  - **nop-entropy 父 pom 传递配置复核**：上述命令搜 nop-app-erp 工作树，不搜兄弟目录 `../nop-entropy/`。父 pom（`../nop-entropy/pom.xml`，经本项目根 pom `<parent>` 继承）的 pitest 传递配置由 Phase 2 接入时经 `mvn -Pmutation help:effective-pom -pl module-finance/erp-fin-service` 复核（届时确认父 pom 无既有 pitest 配置与本设计的 profile 声明冲突）。本期 §现状评估 的"零 pitest"主张限定于 nop-app-erp 工作树。
- 引用源：roadmap line 697 + line 783-784；Q0 README §范围矩阵 §Q1（核验日期 2026-08-01）；Q0 plan §Current Baseline NOT FOUND 证据第 1 条。
- 后果：当前「测试有效性」无量化基线。单元测试数 1903 + 覆盖率只能证明「代码被执行过」，**不能证明测试能捕捉缺陷**——变异测试通过主动改坏代码（插入变异体）验证测试是否捕捉到，是覆盖率之上的有效性度量。

### 1.2 测试/mutation 比历史快照（MA5 审计，非实时）

> **⚠️ 已被 Phase 2 实测值取代（2026-08-01）**：下表 mutation 数为 MA5 pitest **估算值**，严重低估。Phase 2 首跑 pitest 实测：finance generated **4826**（估算 137 的 35 倍）/ mfg **~2580**（估算 74 的 35 倍）/ inv **1807**（首次基线）。实测 mutation score：finance **61%** / mfg **60%** / inv **59%**。权威基线见 `docs/architecture/quality-engineering/mutation-baseline.md`。本表保留仅作 MA5 历史快照引用（不删除以维持审计可追溯），**不再作为决策输入**。

MA5 测试审计（`docs/audits/2026-07-29-1430-arm-ma5-*-test-coverage.md`，A5.1-A5.4 覆盖 finance/mfg/hr/assets 四 S 级域）记录了测试/mutation 比的历史估算。**重要诚实标注**：mutation 数来自 pitest **估算值**（未实际运行），文件数为 MA5 审计实测值。比值为快照、非实时，仅作为目标域选择与首跑预期的参考输入。**本表纯引用 MA5 已核验值，不重测、不引入第二组分子**（避免双真相源）。

| 域 | 测试文件数（MA5 实测） | mutation 数（pitest 估算） | 测试/mutation 比 | 信号 |
|----|------------------------|-----------------------------|-------------------|------|
| finance | 64 | 137 | 0.47（四 S 级最高） | mutation 绝对数最高，测试投入相对最充分 |
| mfg | 29（roadmap 写 30，高估 1） | 74 | 0.41（roadmap）/ 0.39（MA5 按实测 29 修正） | 深测比例 41% 全域最高 |
| hr | 15 | 92 | 0.16（**全域最低**） | 异常路径系统性零覆盖，真实测试债务 |
| assets | 14 | 61 | 0.23 | 48 Processor 共享折旧算术 |
| inv | —（无 MA5 历史） | —（无估算） | — | inv 不在 A5.1-A5.4 S 级审计范围；Phase 2 首跑首次建立基线 |

> 引用源：MA5 finance §7.3（0.47 比裁决，line 178-180）/ mfg §7.3（0.41→0.39 修正，line 157-159）/ hr §1.3（0.16 比根因裁决，line 33-43）/ assets §7.3（0.23 比裁决，line 142-144）。
> **诚实边界**：分子（测试文件数）经 MA5 实测准确；分母（mutation 数）为 pitest 估算、未实测重算。MA5 明确指出「比反映文件级覆盖广度，非路径级覆盖深度」。**本表文件计数不另行重测**（重测口径差异——含/排除基类/helper——会与 MA5 不同源，制造漂移）。Phase 2 首跑将以 pitest 实测 mutation 数**取代**本估算表，作为权威基线。

### 1.3 Nop 代码生成产物分布（关键噪声约束——`_gen` 包 + `api.beans`/`api.crud` 生成 Java）

Nop 平台经 `nop-cli gen` + 增量 `mvn install` 从 ORM XML / api.xml 生成大量代码。这些是**机器生成代码**，对其进行变异测试无意义（生成器改了它们会重生，手动测试覆盖生成代码不保护业务逻辑）。若 pitest 不排除生成代码包，存活变异体将被生成代码噪声主导，淹没真实测试盲区信号。生成代码分布在**两类包**，均须排除：

**类一：`_gen` 包（实体生成基类）**

- 核验命令（2026-08-01 复核）：
  - `find module-finance module-manufacturing module-inventory -path '*/src/main/java/*_gen*' -type d` → 三域各 1 个 `_gen` 目录：
    - `module-finance/erp-fin-dao/src/main/java/app/erp/fin/dao/entity/_gen`
    - `module-manufacturing/erp-mfg-dao/src/main/java/app/erp/mfg/dao/entity/_gen`
    - `module-inventory/erp-inv-dao/src/main/java/app/erp/inv/dao/entity/_gen`
  - `_gen` 类计数：finance 36 / mfg 31 / inv 21（合计 88 个生成类）
  - 包名样本（`rg "^package" <gen类>`）：`package app.erp.fin.dao.entity._gen;` / `app.erp.mfg.dao.entity._gen;` / `app.erp.inv.dao.entity._gen;`

**类二：`api.beans` + `api.crud` 包（生成 `@DataBean` + 生成 CRUD API，规模是 `_gen` 的 3 倍）**

- 核验命令（2026-08-01 复核）：`rg -l "__XGEN_FORCE_OVERRIDE__" --glob '*.java' module-finance module-manufacturing module-inventory` → **264 个文件**（标记 `__XGEN_FORCE_OVERRIDE__` 的生成 Java），全部在 `app.erp.{fin,mfg,inv}.api.{beans,crud}` 包下：
  - `api.beans`（生成 `@DataBean` DTO）：finance 72 / mfg 62 / inv 42（合计 176）
  - `api.crud`（生成 CRUD API）：finance 36 / mfg 31 / inv 21（合计 88）
  - 包路径：`app.erp.fin.api.beans` / `app.erp.fin.api.crud`（mfg/inv 同构）
- **关键**：类二是类一的 **3 倍**（264 vs 88），且全是 getter/setter/trivial CRUD 委托——若不排除，等价变异体噪声将远超 `_gen`，**主导**存活变异体清单。这是比 `_gen` 更大的噪声源，§3.3 `excludedClasses` 须同时覆盖两类。

**完整性核验（两类合计）**：三域生成代码合计 88 + 264 = **352 个生成类**，分布仅限 `*.dao.entity._gen.*` / `*.api.beans.*` / `*.api.crud.*` 三种包通配（核验：`rg -l "__XGEN_FORCE_OVERRIDE__|_gen" --glob '*.java' module-{finance,manufacturing,inventory} | rg -o "app/erp/(fin|mfg|inv)/[a-z_/]+" | sort -u`）。除这三类外，三域无非生成噪声源。

- **非 Java 的 `_gen`**：`erp-*-web/src/main/resources/_vfs/erp/<domain>/pages/*/gen/`（AMIS 页面生成产物，核验：`find . -path '*/_vfs/*_gen*' -type d`，非 Java 字节码，pitest 不触及，仅作完整性记录）。
- **非生成的 `erp-*-codegen` 模块 + `*_app.orm.xml`/`*_service.beans.xml`**：codegen 模块本身无主代码（仅 codegen 入口），pitest 对其无目标；`*_app.orm.xml`/`*.beans.xml` 是资源非 Java，不触及。
- 引用源：roadmap line 783（Q1 关键风险声明：「pitest 须配置 `excludedClasses`/`targetClasses` 排除 `*_gen` 包，否则存活变异体被生成代码噪声主导」）；Q0 README §复杂度分级 §Q1 行。**本节扩展了 roadmap 的「排除 `_gen`」为「排除全部生成包（`_gen` + `api.beans` + `api.crud`）」**——roadmap 仅点名 `_gen`，但实仓 `api.beans`/`api.crud` 生成 Java 噪声是 `_gen` 的 3 倍，须一并排除。

### 1.4 目标域选择裁决（finance / mfg / inv 高复杂度优先）

- **finance**：mutation 估算绝对数全域最高（137）+ 业财一体核心 + 业财异常路径系统性零覆盖（MA5 P1-MA5-003）。首跑最高优先级。
- **mfg**：深测比例全域最高（41%）+ 业财一体异常路径零覆盖（MA5 P1-MA5-005）+ 成本卷算/差异重算复杂算术。首跑第二优先级。
- **inv**：成本层累加 = 余额表是 ERP 强不变量（Q3 属性测试核心目标域）+ 库存核算 reclose 是 Q5 性能基线关键路径。inv **不在** MA5 S 级审计范围（A5.1-A5.4 仅 finance/mfg/hr/assets），无历史 mutation 估算——Phase 2 首跑将首次为其建立 mutation 基线（§1.2 表 inv 行为「无 MA5 历史」）。
- **其余 16 域（含 hr 0.16 全域最低）作为 successor**：首跑聚焦三域（高复杂度优先 + 避免全域首跑即爆炸），其余域（含测试/mutation 比最低的 hr 0.16）作为 successor 首跑扩展。理由：(1) hr 0.16 低比根因已由 MA5 裁决为「真实异常路径缺口」（非纯口径偏差），其修复路径是**补异常路径测试**（MA4/MR3 范围）而非先跑 pitest——pitest 只是量化已知的低比，首跑对 hr 无新信息增量；(2) 全域首跑 1903 测试 × 多模块，首次变异体产出将爆炸，无分类工作流支撑则不可消费（§4.3）；(3) 三域首跑沉淀分类工作流后扩展更可控。

## 2. 目标与非目标

### 2.1 目标（Phase 1 = 本文档；Phase 2 实现见 §4）

1. **裁决变异测试的技术选型**（工具 / 变异算子集 / 插件配置范式），给出候选、考虑的替代、残留风险三要素——满足 plan authoring guide §规则 9（Decision 项记录理由）。
2. **为 Phase 2 实现 plan 提供实施契约**：pitest 插件接入位置 + 三目标域首跑流程 + 存活变异体分类工作流（见 §4）。
3. **定义可验证的验收判据**（见 §5）：三目标域 mutation score 基线落盘 + 存活变异体分类清单（真实盲区 / `_gen` 噪声 / 等价变异）+ `_gen` 排除验证。
4. **裁决 CI 门控形态**（见 §6）：nightly 调度 + 软门控（score 退化阈值）vs per-commit 增量。
5. **声明与 Q4 的协同接口**（见 §8）：Q1 输出的盲区类清单格式，供 Q4 Phase 2 优先覆盖消费。

### 2.2 非目标

- **不实现任何代码/ORM/CI 变更**——本文档仅产出设计。Phase 2 实现（pitest 插件接入 + 首跑）是独立后续 plan，须在本文档审查收敛后方可起草（MQ 文档先行工作流硬约束）。
- **不首跑 pitest**——首跑属 Phase 2，会产生大量首次发现，须先有分类工作流（§4.3）才能消费。
- **不覆盖全域首跑**——Q1 首轮聚焦 finance/mfg/inv 三域（高复杂度优先）；其余 16 域（含 hr 0.16）作为 successor 首跑扩展（§7）。
- **不编写 Q4 故障注入设计**——同批独立 sibling plan（Q4 plan 已存在）。
- **不重新推导 NOT FOUND 证据**——§1 引用 Q0 README + MA5 审计 + roadmap，避免双真相源。
- **不修复发现的测试盲区**——Q1 产出盲区类清单（分类），修复属后续 MR3-style 测试补强 / Q4 故障注入覆盖，非本期。
- **不动用 `nop-testing` skill 写测试**——本期纯设计文档；`nop-testing`（JunitAutoTestCase/快照）留待 Phase 2 实现 plan。

## 3. 技术选型

> 本节裁决三轴：工具（§3.1）/ 变异算子集（§3.2）/ 插件配置范式（§3.3）。每轴记录候选 + 优缺点 + 与现有基础设施冲突点。裁决（§3.4）记录候选、考虑的替代、残留风险（plan authoring guide §规则 9）。

### 3.1 工具选型：pitest（默认）vs 其他

#### 3.1.1 pitest（PItest TEST）—— 默认候选

- **机制**：开源 Java 变异测试框架。Maven 插件 `pitest-maven` 在 `test` 阶段插入字节码变异体（修改 `.class`），逐个跑现有单元测试，存活变异体（测试未捕捉到的）即为测试盲区信号。输出 HTML 报告 + XML（line/mutation 覆盖矩阵）。
- **优点**：
  - **Maven 原生集成**：标准 `<plugin>` 声明，与本项目 Maven 多模块结构（156 模块）天然契合，无须额外构建工具。
  - **复用现有测试**：变异测试直接跑本项目 1903 个 JUnit 测试，无须改写测试。
  - **`targetClasses`/`excludedClasses` 精细控制**（§3.3 关键能力）：可精确排除 `_gen` 包噪声。
  - **活跃维护 + Java 21 兼容**：pitest 1.15+ 支持 JDK 21（本项目 CI `setup-java java-version: '21'`）。
  - **mutation score 量化基线**：HTML/XML 报告可提取 per-class / per-line mutation score，落盘为基线供 §6 软门控对比。
- **缺点 / 风险**：
  - **耗时**：变异测试对每个变异体重跑相关测试，三域首跑预计显著慢于常规 `mvn test`（§3.3 历史不可复用——本项目零 pitest 历史，§4.2 首跑须实测耗时）。
  - **等价变异体（equivalent mutants）**：部分变异体语义上等价于原代码（如 `i++` → `i--` 在 `i` 未被读的上下文），永远「存活」但非测试缺陷——须分类工作流识别（§4.3）。
  - **字节码插桩与 Nop 平台机制潜在交互**：Nop 用反射/XPL 动态加载；pitest 改字节码后若某些路径经平台动态分发，变异体可能未被任何测试触及→假存活。首跑须复核异常存活的可复现性。

#### 3.1.2 考虑的替代工具

- **Jumble**：否决。(1) 维护停滞（最近发布多年前）；(2) 无 Maven 原生 plugin，集成成本高；(3) 无 `targetClasses`/`excludedClasses` 等价能力排除 `_gen` 噪声——本项目的关键约束无法满足。
- **µTest / 手写变异**：否决。手写变异不可规模化到 1903 测试 + 88 `_gen` 类规模，且无法产生可复现的量化 mutation score 基线。
- **仅用 JaCoCo 行覆盖率**：否决。行覆盖率只证明「代码被执行」，不证明「测试能捕捉缺陷」——覆盖率 100% 的代码仍可能有大量存活变异体（典型：断言缺失）。这正是 Q1 要弥补的 MA5 已知盲区（MA5 反复写「需 pitest 运行」即因覆盖率不足以证明测试有效性）。

### 3.2 变异算子集：默认全集 vs 收缩子集（首跑降噪）

pitest 默认启用 `DEFAULT` 算子集（条件边界翻转 / 数学运算符替换 / 返回值突变 / 移除 void 调用等约 10+ 类）。考虑的替代是首跑收缩到子集降噪。

- **候选 A：默认全集（`DEFAULT`）** —— 信号最全，但首跑变异体产出爆炸（三域估算 finance 137 + mfg 74 + inv 未知 ≈ 200+ 变异体），分类工作流压力大。
- **候选 B：收缩子集**（如 `NEGATE_CONDITIONALS` + `MATH` + `RETURN_VALS` 三类高信号算子）—— 首跑变异体减半，降噪便于分类工作流沉淀；但可能遗漏某些盲区类型。

### 3.3 插件配置范式：`targetClasses`/`excludedClasses` 排除全部生成包 + 限定首跑三域

> 本节是最关键的可执行设计——生成代码噪声排除（§1.3 两类生成包：`_gen` + `api.beans` + `api.crud`，合计 352 类）的具体机制。

pitest 的两个核心配置参数：

- **`targetClasses`**：限定变异测试的**目标类**（在哪写代码插入变异体）。用包通配符限定到三域的非生成业务代码。
- **`excludedClasses`**：从 `targetClasses` 中**显式排除**的类（不插变异体）。排除全部生成包（`_gen` + `api.beans` + `api.crud`，§1.3）+ 测试类本身。

**三域首跑的 `targetClasses` / `excludedClasses` 范式（Phase 2 实施契约）**：

```
pitest-maven 插件配置（在 erp-*-service 模块的 pom，或经 profile 激活）：
  targetClasses:
    - app.erp.fin.*        # finance 全域业务代码（service/processor/bizmodel）
    - app.erp.mfg.*
    - app.erp.inv.*
  excludedClasses:
    - *.dao.entity._gen.*  # 排除三域 _gen 包（§1.3 类一，finance 36 + mfg 31 + inv 21 = 88 类）
    - *.dao.entity._gen._* # 含下划线前缀的内部生成基类
    - *.api.beans.*        # 排除三域生成 @DataBean DTO（§1.3 类二，finance 72 + mfg 62 + inv 42 = 176 类）
    - *.api.crud.*         # 排除三域生成 CRUD API（§1.3 类二，finance 36 + mfg 31 + inv 21 = 88 类）
    - *.codegen.*          # 排除 codegen 模块类（若有泄漏到 targetClasses）
    - *Test*               # 排除测试类（变异目标是生产代码，非测试）
    - *IT                  # 排除集成测试类
  targetTests:
    - app.erp.fin.*Test    # 限定跑哪些测试去杀变异体（三域测试）
    - app.erp.mfg.*Test
    - app.erp.inv.*Test
```

> **关键设计点（噪声排除完整性）**：`targetClasses` 用域包通配（`app.erp.fin.*`）而非全仓 `app.erp.*`，天然限定首跑到三域；`excludedClasses` 须排除 §1.3 的**两类全部生成包**——`_gen`（88 类）+ `api.beans`（176 类）+ `api.crud`（88 类），合计 352 个生成类。`api.beans`/`api.crud` 是 `_gen` 的 3 倍且全是 getter/setter/trivial CRUD 委托，**若遗漏则等价变异体噪声将主导存活清单**（§1.3 已核验）。双控（域包限定 + 两类生成包排除）保证首跑只变异三域非生成业务代码、只跑三域测试。Phase 2 首跑 dry-run（§4.1 step 3）须闭环核验这三类包在变异目标中为零（§5 验收 3）。

**插件接入位置（per-module vs profile 策略）**：

- **候选 P1：per-module 接入**（在 `erp-fin-service` / `erp-mfg-service` / `erp-inv-service` 三模块 pom 各声明 pitest）—— 优点：精确限定三域、不污染其他 153 模块 build、Phase 2 可分域渐进。缺点：三处重复配置。
- **候选 P2：父 pom + profile 激活**（在 `app-erp/pom.xml` 声明 pitest + `<profile><id>mutation</id>`，仅 `-Pmutation` 激活）—— 优点：单处配置、默认不激活（不影响常规 `mvn test` 1903 测试基线）。缺点：profile 激活后作用于全 156 模块，须靠 `targetClasses` 限定三域（首跑不跑其余域），且 profile 与 nop-entropy 父 pom 的继承关系须复核。
- **裁决见 §3.4**。

### 3.4 裁决（Decision）

> 决策输入：§1 现状（零 pitest + `_gen` 噪声约束 + 三域高复杂度）+ §3.1-3.3 候选优缺点 + Q0 README §复杂度分级（Q1 平台依赖低、基础设施接入中）+ AGENTS.md「应用层闭环优先，不动平台」。

**裁决：**
1. **工具：选 pitest**（§3.1.1）。Jumble / µTest / 仅 JaCoCo 否决（§3.1.2）。
2. **变异算子集：首跑选默认全集（`DEFAULT`）**——候选 B（收缩子集）否决作为首跑默认。理由：首跑目的是建立**完整基线**，收缩子集会从一开始就系统性遗漏某些盲区类型，使基线不可比。降噪通过 §4.3 分类工作流（区分 `_gen` 噪声 / 等价变异 / 真实盲区）处理，而非通过砍算子集。首跑后若某类算子（如 `VOID_METHOD_CALLS`）确认产出大量等价变异、信号噪比低，Phase 2 续跑可调子集——但首跑须全集以建立可比基线。
3. **插件接入位置：选候选 P2（父 pom + `<profile><id>mutation</id>` 激活）**——候选 P1（per-module）否决作为主路径。理由：(1) 单处配置避免三处重复漂移；(2) profile 默认不激活，保护常规 `mvn test` 1903 测试基线 + 156 模块构建时间不受影响（Q0 README §复杂度分级 Q1 行：「全仓 build profile」风险由 profile 默认关闭消除）；(3) profile 激活后靠 `targetClasses: app.erp.{fin,mfg,inv}.*` 限定三域，不跑其余 16 域。**Phase 2 须复核 profile 与 nop-entropy 父 pom 的继承关系**（nop-entropy 父 pom 是 `<parent>`，本项目根 pom 是其子——profile 声明在根 pom 即可，不触及 nop-entropy 源码）。

**裁决理由汇总：**

1. **pitest 是本项目约束下的唯一合理选择**：Maven 原生 + `targetClasses`/`excludedClasses` 精细排除 `_gen`（§1.3 关键约束的唯一满足方案）+ 复用现有 1903 测试 + Java 21 兼容。其他工具要么无 Maven 集成（Jumble），要么无排除能力，要么不是变异测试（JaCoCo）。
2. **首跑默认全集是基线可比性的前提**：MA5 历史估算（finance 137 mutation）是基于默认全集的估算，首跑须用同口径才能与历史对话 + 后续续跑可比。降噪靠分类工作流（§4.3），不靠砍算子集。
3. **profile 激活保护常规基线 + 应用层闭环**：profile 在根 pom 声明，不动 nop-entropy，默认关闭使 `mvn test` 1903 测试 / `mvn clean install -DskipTests` 156 模块构建基线（`docs/testing/known-good-baselines.md`）不受影响。

**考虑的替代（记录为何否决）：**

- **候选 B（收缩算子集）作首跑默认**：否决——系统性遗漏盲区类型，基线不可比。保留为续跑优化（首跑确认某算子信号噪比低后调整）。
- **候选 P1（per-module 接入）**：否决作为主路径——三处重复配置易漂移。保留为 profile 方案受阻时的退路（若 Phase 2 复核发现 profile 与 nop-entropy 父 pom 继承冲突，退回 per-module）。
- **全仓首跑（不限三域）**：否决——1903 测试 × 88+ `_gen` 类 + 全域变异体爆炸，无分类工作流支撑则不可消费（§4.3）。三域优先沉淀工作流后扩展。
- **仅跑 finance 单域首跑**：否决——mfg/inv 信号互补（mfg 深测比例最高 + inv 成本不变量），单域不足以验证分类工作流的通用性。

**残留风险：**

- **R1（首跑耗时不可预知）**：本项目零 pitest 历史，三域首跑耗时不参照其他项目经验。Phase 2 首跑须实测并记录（§5 验收 1 含耗时基线）；若超阈值（如 > 30 分钟）评估算子集收缩或分域分批跑。
- **R2（等价变异体比例）**：默认全集在 getter/setter/简单委托多的代码上等价变异比例高。三域 `_gen` 已排除，但 BizModel 内仍可能有简单委托。分类工作流（§4.3）须识别并登记，不误判为测试盲区。
- **R3（字节码插桩与 Nop 平台动态分发交互）**：Nop 用反射/XPL/`@Inject` 动态装配，pitest 改字节码后某些变异体可能经平台动态分发路径未被测试触及→假存活。Phase 2 须对异常存活抽样复核可复现性。
- **R4（profile 与 nop-entropy 父 pom 继承复核未完成）**：本设计裁决 profile 方案，但 profile 与父 pom 继承的实际交互须 Phase 2 复核。若冲突，退回候选 P1（per-module）。

## 4. 实施步骤（Phase 2 实现 plan 的范围契约）

> 本节为 Phase 2 实现 plan 提供步骤骨架与边界声明。Phase 2 plan 起草时（加载 `nop-testing` skill）以本节为实施契约，可细化但不得偏离已裁决的 pitest + profile + 默认全集 + 三域首跑范围。

### 4.1 pitest 插件接入（profile 激活）

1. 在 `app-erp/pom.xml`（根 pom）`<build><plugins>` 或 `<profiles>` 声明 `pitest-maven` 插件，包裹在 `<profile><id>mutation</id>` 内（默认不激活）：
   - `<groupId>org.pitest</groupId><artifactId>pitest-maven</artifactId><version>1.15.x（Java 21 兼容版本，Phase 2 锁定具体版本）</version>`
   - 配置 `targetClasses`/`excludedClasses`/`targetTests` 按 §3.3 范式
   - 配置 `outputFormats`: HTML + XML（供 §5 基线落盘 + §6 软门控解析）
   - 配置 `timestampedReports`: false（稳定输出路径便于基线对比）
2. 复核 profile 与 nop-entropy 父 pom 继承关系（§3.4 R4）：
   - 核验命令：`mvn -Pmutation help:effective-pom -pl module-finance/erp-fin-service` 确认 profile 生效 + pitest 配置可见
   - 若继承冲突，退回候选 P1（per-module，在 erp-{fin,mfg,inv}-service 三 pom 各声明）
3. 首跑前 dry-run 单域（finance）确认 `_gen` 排除生效：
   - `mvn -Pmutation test-compile org.pitest:pitest-maven:mutationCoverage -pl module-finance/erp-fin-service` （或经 profile 触发的 goal）
   - 核验 HTML 报告：`app.erp.fin.dao.entity._gen` 包内类**不出现在**变异目标（§5 验收 3）

### 4.2 三目标域首跑基线

1. finance 首跑：`mvn -Pmutation -pl module-finance/erp-fin-service -am ...` → 产出 HTML/XML 报告 + mutation score + 存活变异体清单
2. mfg 首跑：同上，`module-manufacturing/erp-mfg-service`
3. inv 首跑：同上，`module-inventory/erp-inv-service`（inv 首次建立 mutation 基线，无历史估算对照）
4. **耗时实测**：记录三域各自首跑耗时（§5 验收 1 基线 + §6 CI 调度决策输入）
5. 三域 mutation score 落盘到 `docs/testing/`（或本目录新建 `mutation-baseline.md`），替换 §1.2 MA5 估算表为 pitest 实测值

### 4.3 存活变异体分类工作流（首跑后分析）

> 首跑必产生大量存活变异体（本项目零 pitest 历史）。直接发布存活清单不可消费——须经分类工作流区分信号类型。

存活变异体三类分类：

1. **`_gen` 噪声**（须为零——§3.3 已排除）：若 HTML 报告中 `*.dao.entity._gen.*` 类有存活变异体，说明 `excludedClasses` 配置失效，须修配置而非记为盲区。§5 验收 3 闭环校验此类为零。
2. **等价变异（equivalent mutants）**：变异后语义与原代码等价（测试无法也无需杀死）。典型：getter/setter 内 `return x` → `return null` 若调用方不读返回值；`i++` → `i--` 若 `i` 后续未读。此类登记为等价、不计入盲区，但记录比例（高比例指示该模块代码风格可优化——如过多无意义委托）。
3. **真实测试盲区（real blind spots）**：变异体存活且非等价 → 该代码路径的测试不足以捕捉该缺陷。此类是 Q1 的核心产出，按域 + 类聚合，输出为 §8 Q4 协同接口消费的盲区类清单。

分类工作流步骤（Phase 2）：

1. 从 pitest XML 报告提取全部存活变异体（surviving mutations）
2. 按类聚合，先过滤全部生成包噪声——`_gen` + `api.beans` + `api.crud`（三者应为空，§5 验收 3 闭环校验；若非空说明 `excludedClasses` 配置失效，须修配置而非记为盲区）+ 过滤明显等价（getter/setter/trivial return）
3. 剩余存活变异体逐类抽样人工/辅助判定：等价 vs 真实盲区
4. 真实盲区按域 + 类聚合成清单，落盘为 §8 协同接口产物 + 登记后续修复 successor（MR3-style 测试补强 / Q4 故障注入覆盖）

### 4.4 跨 nop-entropy 改造边界声明

| 改动面 | 位置 | Q1 Phase 2 是否触碰 | 说明 |
|--------|------|----------------------|------|
| `pitest-maven` 插件声明 | `app-erp/pom.xml`（根 pom，应用层） | **是** | profile 包裹，默认不激活 |
| 三域 service 模块 pom | `erp-{fin,mfg,inv}-service/pom.xml` | **可能**（若 profile 受阻退 P1） | 仅在 profile 方案冲突时 per-module 接入 |
| nop-entropy 父 pom | `../nop-entropy/pom.xml` | **否** | profile 在子 pom（根 pom）声明，不触及平台源码 |
| 生产业务代码 | `module-*/erp-*-service/src/main` | **否** | pitest 在 `target/classes` 字节码层插桩，不改源码 |
| CI workflow | `.github/workflows/` | **是**（§6 裁决若纳入 nightly） | 新建 mutation.yml 或加 job 到 compliance.yml |
| 测试代码 | `module-*/erp-*-service/src/test` | **否** | 复用现有测试，不改写 |

> 边界裁决：Q1 Phase 2 **零 nop-entropy 改动**，全部在应用层（根 pom profile + 可选 CI workflow）。Phase 2 无须在 `nop-entropy/ai-dev/logs/` 记日志。

### 4.5 Phase 2 执行顺序建议

1. 4.1 step 1-2（pitest 插件 profile 接入 + 与父 pom 继承复核）——基础设施，先行
2. 4.1 step 3（finance 单域 dry-run 确认 `_gen` + `api.beans` + `api.crud` 全部生成包排除生效）
3. 4.2 三域首跑基线（finance → mfg → inv，含耗时实测）
4. 4.3 分类工作流（存活变异体三分类，产出盲区类清单）
5. §5 全量验收 + 三域 mutation score 基线落盘
6. §6 CI 门控接线（若 §6 裁决纳入 nightly）
7. §8 Q4 协同产物交付（盲区类清单格式化）

## 5. 验收判据（Phase 2 closure gate 契约）

> 每条须在 Phase 2 closure audit 时由独立子代理在 live repo 核验。每条给出具体可执行机制。

1. **pitest 插件接入且 profile 默认不激活**：根 pom 声明 `<profile><id>mutation</id>` + `pitest-maven` 插件。**可执行核验**：
   - `rg "pitest-maven" pom.xml` 命中插件声明
   - `rg "<id>mutation</id>" pom.xml` 命中 profile
   - `mvn help:active-profiles`（不加 `-Pmutation`）输出**不含** `mutation` profile（保护常规基线）
   - `mvn -Pmutation help:active-profiles` 输出**含** `mutation` profile
2. **三目标域 mutation score 基线落盘**：finance / mfg / inv 三域首跑 HTML + XML 报告 + 提取的 per-class mutation score 落盘到 `docs/testing/`（或 `mutation-baseline.md`）。**可执行核验**：
   - 三域报告文件存在
   - mutation score 数值（实测）替换 §1.2 MA5 估算表（finance 实测 ≠ 137 估算）
   - inv 首次有 mutation 基线（无历史对照，记录首跑值）
3. **生成代码噪声排除验证（关键约束闭环——`_gen` + `api.beans` + `api.crud`）**：三域 HTML 报告中**全部生成包**内类**不出现**在变异目标。**可执行核验**：
   - 抽查 finance HTML 报告：`app.erp.fin.dao.entity._gen._*` / `app.erp.fin.api.beans.*` / `app.erp.fin.api.crud.*` 类均无变异记录
   - `excludedClasses` 配置含 `*.dao.entity._gen.*` + `*.api.beans.*` + `*.api.crud.*`（§1.3 两类生成包，合计 352 类）
4. **存活变异体分类清单落盘**：经 §4.3 三分类（生成代码噪声=零 / 等价变异 / 真实盲区）的清单落盘。**可执行核验**：
   - 分类清单文件存在，含三类计数
   - 生成代码噪声（`_gen` + `api.beans` + `api.crud`）类计数为零（与验收 3 一致）
   - 真实盲区类清单格式符合 §8 协同接口（可被 Q4 消费）
5. **全量回归绿（profile 不激活时基线不变）**：`mvn clean install -DskipTests` 156 模块 BUILD SUCCESS；`mvn test` 0 failures / 0 errors（profile 默认不激活，常规基线不受 pitest 接入影响）。测试计数基线以 `docs/testing/known-good-baselines.md` 最近全量绿基线为准。
6. **首跑耗时基线记录**：三域各自首跑耗时落盘（§6 CI 调度决策输入）。
7. **无双真相源**：本文档 §1 引用上游真相源（Q0 README + MA5 + roadmap），Phase 2 plan 引用本文档，Phase 2 基线落盘后 §1.2 估算表标注「已被 Phase 2 实测值取代」。

## 6. CI 门控设计

> 裁决 CI 门控形态：per-commit 全量不现实（耗时），裁决 nightly 调度 + 软门控 vs per-commit 增量。记录候选、考虑的替代、残留风险。对齐 compliance-baseline 单向收紧模式（§6.4）。

### 6.1 现状

- 现有 CI（`.github/workflows/`）：`maven.yml`（`mvn -B package` 全量构建/测试，timeout 30 分钟）+ `compliance.yml`（F8 反模式基线单向收紧门控 + F15 i18n + web 页面校验）+ `e2e.yml`。
- **零变异测试维度**——mutation score 无 CI 追踪，退化只能在 Phase 2 首跑后下一次手动跑时发现。

### 6.2 候选

- **C-1：不引入 CI 门控**（仅 Phase 2 首跑 + 手动续跑）。残留：mutation score 退化无持续追踪，盲区回潮无预警。
- **C-2：nightly 调度 + 软门控（score 退化阈值）**：新建 `.github/workflows/mutation.yml`（或加 job 到 `compliance.yml`），`schedule: cron nightly` + `workflow_dispatch`，跑三域 pitest，将 mutation score 与基线对比，退化超阈值则红。软门控：仅追踪退化、不阻断 per-commit（mutation score 提升鼓励但不强制）。
- **C-3：per-commit 增量变异测试**：每次 push/PR 跑增量 pitest（仅变异变更代码）。pitest 1.15+ 支持 `--withHistory` 增量模式。

### 6.3 裁决（Decision）

> 决策输入：§3.4 R1（首跑耗时不可预知，三域全量 pitest 远慢于 `mvn test`）+ §1.2（三域 mutation 估算 200+ 变异体，per-commit 全量不现实）+ compliance-baseline 单向收紧门控先例（F8/F15 已验证模式）。

**裁决：Phase 2 引入 C-2（nightly 调度 + 软门控）作为回归层；C-3 per-commit 增量列为 watch-only successor。C-1 否决（无持续追踪则盲区回潮无预警）。**

**裁决理由：**

1. **per-commit 全量不现实**：三域 pitest 全量跑耗时远超 `maven.yml` 的 30 分钟 timeout（§3.4 R1，Phase 2 首跑实测后确认）。per-commit 跑会拖垮 PR 反馈循环。
2. **nightly + 软门控对齐 compliance-baseline 单向收紧模式**：`compliance.yml` 的 F8/F15 已验证「基线落盘 + 单向收紧（actual > baseline → fail）」模式（见 `docs/audits/compliance-baseline.md` §回归门控规则）。mutation score 软门控复用同模式：mutation score **下降**（退化，actual < baseline）→ nightly 红；提升（actual > baseline）→ 鼓励非强制。门控逻辑放 CI（对齐 F8 option (b)），pitest 本身是 pure reporter。
3. **C-3 增量收益不足以抵复杂性**：pitest `--withHistory` 增量模式在频繁变更的 PR 上信号有限（变异体与测试映射的增量边界复杂），且首跑基线未建立时增量无参照。列为 successor（触发：三域基线稳定 + 团队 PR 节奏需要更快反馈时）。
4. **C-1 否决**：仅靠 Phase 2 一次性首跑，mutation score 退化无持续追踪——这违背 Q1「建立可追踪基线」目标。

**考虑的替代：**

- **C-1（不引入）**：否决——无持续追踪，盲区回潮无预警。
- **C-3（per-commit 增量）**：否决作为本期——增量模式复杂性 + 首跑基线未建。列为 successor。
- **per-commit 全量**：否决——耗时超阈值，拖垮 PR 反馈。

**残留风险：**

- **R5（nightly 与 per-commit 漂移）**：nightly mutation 红与 per-commit `mvn test` 绿之间的窗口期可能引入盲区退化未被即时发现。接受（mutation 退化非阻塞交付，nightly 频率合理）。
- **R6（GitHub Actions runner 上 pitest 耗时 / 资源）**：三域 pitest 在 runner 上的实际耗时须 Phase 2 实测后定 nightly timeout（默认 360 分钟可能不足或过剩）。
- **R7（软门控基线初始值）**：首跑基线的 mutation score 作为初始门控值。若首跑 score 偏低（如某域 30%），门控初期会频繁红——Phase 2 须裁决初始阈值是「首跑实测值」还是「宽松过渡阈值」（允许逐步收紧，对齐 compliance-baseline 「调高基线须独立计划」反向：mutation score 调低须独立计划裁决）。

### 6.4 与现有 CI 的集成方式（Phase 2 落地）

- 新建 `.github/workflows/mutation.yml`（或加 job 到 `compliance.yml`）：
  - `schedule: cron: '0 4 * * *'`（nightly，错开 Q6 clock-rollover nightly 的 `0 3 * * *`）+ `workflow_dispatch`
  - 步骤：`setup-java 21` → `mvn -Pmutation -pl module-finance/erp-fin-service -am test-compile org.pitest:pitest-maven:mutationCoverage`（三域分 job 或串行）→ 解析 XML 报告 mutation score → 与基线对比（单向收紧：score 退化 → fail）
  - **门控逻辑放 CI（对齐 F8 option (b) 架构，非字面复用 F8 解析器）**：python 解析 pitest **XML** 报告提取 per-domain mutation score，与 `docs/testing/mutation-baseline.md`（或本目录）的 YAML 基线块对比，actual < baseline → exit 1。pitest 本身不判 pass/fail。**注**：F8（`compliance.yml`）解析的是 checker **文本输出**（`checker-output.txt`），与本设计的 pitest **XML** 解析器不同——对齐的是「checker=pure reporter + gate 逻辑在 CI + 单向收紧」**架构模式**，非字面复用 F8 的 python 解析器。Phase 2 须新写 pitest XML 解析逻辑。
  - 失败即红，通知机制对齐现有 workflow
- Phase 2 plan 须给出该 workflow 实际接线 + 首次 nightly 绿基线 + 初始阈值裁决（R7）

## 7. 残留风险汇总与 successor

> 汇总 §3.4 + §6.3 残留风险，登记 successor 触发条件（plan authoring guide §反松弛规则：Follow-up 须命名触发条件）。

| 风险 ID | 描述 | 分类 | successor 触发条件 |
|---------|------|------|--------------------|
| R1 | 首跑耗时不可预知（零 pitest 历史） | Phase 2 实施约束 | Phase 2 首跑实测落盘基线；超阈值则评估算子收缩/分域分批 |
| R2 | 等价变异体比例（getter/setter/委托多） | 分类工作流约束 | §4.3 分类工作流识别登记；高比例模块记录代码风格优化建议 |
| R3 | 字节码插桩与 Nop 动态分发交互（假存活） | Phase 2 实施约束 | Phase 2 对异常存活抽样复核可复现性 |
| R4 | profile 与 nop-entropy 父 pom 继承未复核 | Phase 2 实施约束 | Phase 2 复核；冲突则退回候选 P1（per-module） |
| R5 | nightly 与 per-commit 漂移 | 接受 | mutation 退化非阻塞，nightly 频率合理 |
| R6 | runner 上 pitest 耗时/资源 | Phase 2 实施约束 | Phase 2 实测定 nightly timeout |
| R7 | 软门控初始阈值裁决 | Phase 2 实施约束 | Phase 2 裁决初始值（实测值 vs 宽松过渡） |
| —（successor） | Q1 Phase 2 实现 plan（pitest 接入 + 首跑 + 分类） | out-of-scope（本文档 Phase 1） | 本文档经 ≥2 轮独立审查收敛（§Review Record）+ 技术选型裁决落定（§3.4）→ DRAFT_PLANS 起草 |
| —（successor） | 其余 16 域 mutation score 基线（含 hr 0.16） | optimization candidate | 三域基线落盘 + 分类工作流沉淀后扩展；hr 优先级取决于其异常路径测试补强（MA5/MR3）进度 |
| —（successor） | C-3 per-commit 增量变异测试 | watch-only successor | 三域基线稳定 + 团队 PR 节奏需更快反馈时 |
| —（successor） | 真实盲区类修复（MR3-style 测试补强 / Q4 故障注入覆盖） | out-of-scope improvement | §4.3 分类工作流产出真实盲区清单后，由 Q4 Phase 2 + 后续测试补强 plan 消费 |
| —（successor，adjudicated residual） | mfg `excludedTestClasses` workaround 移除（`TestErpMfgWorkOrderEndToEnd` 纳入基线） | 平台级 successor（根因在 nop-entropy，不可在本仓修） | **根因已裁决**（plan `2026-08-02-1500-2`）：平台级非确定性——`OrmTimestampHelper` 用 `CoreMetrics.currentTimeMillis()`（真实墙钟，app 层 `ThreadLocalFrozenClock` 依设计不冻结 `currentTimeMillis`）+ `AutoTestVars` 按捕获计数/按值反查索引易挥发时间戳→同类多次 flush 同毫秒碰撞→`@var:...@updateTime_N` 偶发 `null`→`check-output-fail`。实证非确定（3 次复跑 1 pass/2 fail）。**触发**：nop-entropy 侧任一修复落地——(a) `OrmTimestampHelper` 改用 `currentDateTime()`；(b) `ThreadLocalFrozenClock` 增冻结 `currentTimeMillis` 选项；(c) `AutoTestVars` 对时间戳易挥发字段改按名稳定索引 |

## 8. 与 Q4 协同接口

> Q1↔Q4 协同（roadmap line 786，Q0 README §实施顺序裁决 line 152）：Q1 发现的测试盲区类正是 Q4 应优先覆盖的可恢复性路径（tryPost 吞异常同型根因跨 6 域）。本节声明 Q1 输出的盲区类清单格式，供 Q4 Phase 2 优先覆盖消费。

### 8.1 协同契约

- **Q1 产出**：§4.3 分类工作流输出的「真实测试盲区类清单」（剔除全部生成代码噪声 `_gen`+`api.beans`+`api.crud` + 等价变异后的真实盲区）。
- **Q4 消费**：Q4 Phase 2（故障注入 harness 覆盖 6 域过账悬挂路径）以 Q1 盲区类清单作为**优先覆盖目标**——清单中属于 finance/hr/assets/qa/projects/maintenance 6 域过账 dispatcher/Processor 的盲区类，正是 tryPost 吞异常同型根因（P1-MA2-032/048/060/064/068/074）的可恢复性路径。
- **首批协同覆盖不对称（重要边界）**：Q1 首批只跑 finance/mfg/inv 三域（§1.4），Q4 的 6 域目标是 finance/hr/assets/qa/projects/maintenance——**首批交集仅 finance**。故 Q1 首批盲区清单对 Q4 的即时可消费域仅 finance；hr/assets/qa/projects/maintenance 的盲区须待 Q1 successor 扩展（§7）跑这些域后方能供 Q4 消费。Q4 排期不阻塞于 Q1 全域完成（Q4 plan §残留风险：协同假设若盲区与过账路径不重合，Q4 可独立前移，§8.3）。
- **协同时序**：Q4 Phase 2 plan 起草建议在 Q1 Phase 2 产出盲区类清单后启动（Q4 plan §Deferred 已记录：「建议在 Q1 Phase 2 产出盲区类清单后启动以充分消费协同」）。Q1/Q4 的 Phase 1 设计文档（本文档 + Q4 doc）同批起草，Phase 2 可独立或协同推进。

### 8.2 盲区类清单格式（Q1 输出 → Q4 消费契约）

Q1 Phase 2 落盘的盲区类清单须遵循以下格式，确保 Q4 可机械消费：

```
# 盲区类清单（Q1 Phase 2 产出，供 Q4 消费）
# 来源：pitest 存活变异体经 §4.3 三分类后的「真实盲区」子集
# 核验日期：<Phase 2 落盘日期>

## 按域聚合（盲区类 → 存活变异体数 → 是否过账路径）

### finance
| 类 | 存活变异体数 | 是否过账 dispatcher/Processor | Q4 可消费性 |
|----|--------------|-------------------------------|-------------|
| app.erp.fin.service.posting.NotesPostingDispatcher | N | 是（P1-MA2-032 同型） | 高（Q4 优先覆盖） |
| ... | ... | ... | ... |

### mfg / inv / hr / assets / qa / projects / maintenance
（同格式）

## Q4 优先覆盖候选（交集：盲区类 ∩ 6 域过账悬挂路径）
- <类清单，对应 Q4 plan §6 域过账悬挂路径覆盖清单>
```

> **格式约束**：(1) 类全限定名（FQCN），便于 Q4 grep 定位；(2) 标注「是否过账 dispatcher/Processor」——Q4 的覆盖目标是过账悬挂路径，此列是协同的 join key；(3) 对应 Q4 plan 引用的 finding ID（P1-MA2-032 等），便于交叉验证。

### 8.3 协同边界

- Q1 **不负责**修复盲区（仅产出清单）；修复属 Q4 故障注入覆盖（过账路径盲区）+ 后续 MR3-style 测试补强（非过账路径盲区）。
- Q4 **不负责**重跑变异测试（仅消费清单）；Q1 的 mutation score 基线是 Q4 补测试后回归验证的参照（Q4 补测试后某盲区类的存活变异体应被杀死→mutation score 提升）。
- 若 Q1 盲区类与 Q4 过账悬挂路径不重合（Q0 README §残留风险：「协同假设基于变异测试盲区 = 故障注入优先覆盖，若实际盲区分布与过账悬挂路径不重合，Q4 排期可独立前移」），Q4 排期可独立前移，协同清单为空集亦有效。

## Review Record

> 审查记录：MQ 文档先行工作流要求 ≥2 轮独立子代理审查（第 1 轮规范合规 + 第 2 轮覆盖面/可执行性），由不同子代理会话执行（不同 task id），审查者不可与作者为同一会话。每轮输出 BLOCKER/MAJOR/MINOR 分级意见，作者修订后重审直至收敛（无残留 BLOCKER/MAJOR）。本文档经 3 轮审查收敛（R1 合规 + R2 覆盖/可执行性 + R3 收敛复核）。

- **Round 1（规范合规审查）**: `ses_0445457edffe19i2REIBjuF0dm`（独立子代理 fresh session cold context）— **needs-revision**，0 BLOCKER / 1 MAJOR / 1 MINOR。
  - MAJOR-1：§1.2 把无命令、与 MA5 不一致的重测文件计数（finance "67" / mfg "30" / inv "27"）注入标注为「MA5 审计」的表，且 mfg 行自相矛盾（实测 30 vs 按实测 29 修正），违反单一真相源 + 计划 Exit Criteria「每条证据标注可复现核验命令」。
  - MINOR-1：§1.1 prose 高估 `rg "pitest"` 命令覆盖范围（声称覆盖 nop-entropy 父 pom 传递配置，实际只搜工作树）。
  - 修改摘要：§1.2 整表回归纯 MA5 引用（finance 64 / mfg 29 / hr 15 / assets 14，inv 标「无 MA5 历史」），删除重测列 + 注脚强调「本表纯引用 MA5 已核验值，不重测」；§1.1 收窄命令覆盖范围为「nop-app-erp 工作树」+ 增父 pom 复核子句（Phase 2 经 `mvn -Pmutation help:effective-pom` 复核）；§1.4 inv 行同步去除「实测 27」。
  - Phase 1 item 覆盖映射：6 content items + 2 exit criteria 全部 ✓（subject to MAJOR-1 证据修正，已修）。

- **Round 2（覆盖面与可执行性审查）**: `ses_044506629ffeiPgpK9ry6CDTpB`（**另一个**独立子代理，不同 task id，新会话）— **needs-revision**，1 BLOCKER-equiv（记为 MAJOR）/ 2 MINOR。R1 两项 finding 经 R2 复核均 **resolved**。
  - MAJOR-1（BLOCKER-equivalent）：`_gen` 排除策略遗漏 264 个额外生成类——实仓 `api.beans`（176）+ `api.crud`（88）是 `_gen`（88）的 3 倍，§3.3 `excludedClasses` 未覆盖；§1.3「非 `_gen` 生成产物」分析事实错误（声称仅 codegen 模块 + XML 资源，遗漏生成 Java）；§5 验收 3 仅校验 `_gen`。实仓核验 `rg -l "__XGEN_FORCE_OVERRIDE__"` = 264 文件。
  - MINOR-1：§8.1 协同覆盖不对称——Q1 首批 fin/mfg/inv，Q4 目标 fin/hr/assets/qa/projects/maintenance，首批交集仅 finance，但 §8.1 措辞暗示更广覆盖。
  - MINOR-2：§6.4 措辞暗示字面复用 F8 解析器，但 F8 解析文本、pitest 输出 XML——对齐是架构非字面复用。
  - 修改摘要：§1.3 重写为「两类生成包」框架（`_gen` 88 + `api.beans` 176 + `api.crud` 88 = 352 类，含完整性核验命令 + 扩展 roadmap「排除 `_gen`」为「排除全部生成包」）；§3.3 `excludedClasses` 增 `*.api.beans.*` + `*.api.crud.*` + 关键设计点改为「两类全部生成包」；§4.3 step 2 / §4.5 step 2 / §5 验收 3&4 同步改为「全部生成包」；§8.1 增「首批协同覆盖不对称（首批交集仅 finance）」边界声明；§6.4 注明「对齐 F8 架构非字面复用解析器，Phase 2 须新写 pitest XML 解析逻辑」。

- **Round 3（收敛复核）**: `ses_0444c8c83ffeeF02BO51NeU415`（独立子代理 fresh session cold context，不同于 R1/R2）— **pass**，0 BLOCKER / 0 MAJOR / 1 NEW MINOR。R1+R2 共 5 项 finding 全部 **resolved**（逐条 live 实仓复核：XGEN 计数 264 精确、_gen 目录 3、per-domain/subpackage 计数与 §1.3 完全一致、pitest zero-hit、无 stale leftover）。
  - NEW-MINOR-1：§3.3 标题 + intro bullet 仅提 `_gen`，未含 `api.beans`/`api.crud`（描述性 prose 过时，可执行契约 config block + 关键设计点段正确）。
  - 修改摘要：§3.3 标题改为「排除全部生成包」+ intro bullet 改为「排除全部生成包（`_gen` + `api.beans` + `api.crud`，§1.3）」。
  - Round 3 结论原文：「The doc has converged: all 5 prior findings are resolved with evidence verified against the live repo... only residual issue (MINOR-1: §3.3 title/intro prose) is non-blocking and does not affect the executable Phase 2 contract. The doc... can serve as the converged contract for the Phase 2 implementation plan.」

**收敛结论**：3 轮审查后无残留 BLOCKER / 无残留 MAJOR（R1 的 1 MAJOR + 1 MINOR、R2 的 1 MAJOR + 2 MINOR 经 R3 逐条 live 复核 resolved；R3 的 1 NEW MINOR 已修订）。文档可作为 Phase 2 实现 plan 的实施契约。MINOR 不阻塞收敛。

<!-- 审查者多样性已满足：R1（ses_0445457e...）/ R2（ses_04450662...）/ R3（ses_0444c8c8...）三会话 task id 不同，均独立 fresh cold context，未复用作者上下文。 -->
