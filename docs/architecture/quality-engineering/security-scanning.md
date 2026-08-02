# 安全扫描流水线（MQ Q2）—— Phase 1 设计文档

> Owner Doc for Milestone MQ Q2（安全扫描流水线）
> 创建日期：2026-08-01
> Plan：`docs/plans/2026-08-01-1121-2-mq-q2-security-scanning-design-doc.md`
> 单一真相源依赖：本文档是 MQ 文档先行工作流 **Phase 1** 产物（设计/策略文档），**不实现任何代码/ORM/CI 变更**。Phase 2 实现 plan（Dependency-Check + FindSecBugs 接入 CI + 首次扫描 + 基线落盘）须在本文档审查收敛后方可起草。
> 上游真相源（**只引用**，不重推导，避免双真相源漂移）：
> - `docs/backlog/audit-remediation-roadmap.md` §Milestone MQ Q2（line 675 工作项表 + line 784-785 维度说明 + §横切关注点 §文档先行工作流 line 843-862）
> - `docs/architecture/quality-engineering/README.md`（Q0 范围矩阵 + 复杂度分级 + 实施顺序裁决基线，Q2 位 5，独立性强不阻塞其他维度）
> - `docs/plans/2026-08-01-1121-1-mq-q0-quality-gap-analysis-readme.md` §Current Baseline（Q2 NOT FOUND 实仓证据已核验：零安全扫描依赖/CI job）
> - `docs/audits/compliance-baseline.md`（单向收紧门控范式 + `## BASELINE (machine-readable)` 机器可读块参照 + 调高基线的唯一途径）
> - `docs/design/roles-and-permissions.md`（MA6 RBAC 基线边界，Q2 与之正交不重复）
> - `.github/workflows/`（既有 5 CI workflow 清单 + 集成参照）
> - sibling docs：`docs/architecture/quality-engineering/fault-injection.md`（Q4，文档结构参照）/ `mutation-testing.md`（Q1，nightly 软门控先例）

## 1. 现状评估

> 本节**引用**（非重推导）上游真相源已核验事实，每条标注可复现核验命令 + 核验日期，便于 Phase 2 plan 与独立审查复核。证据核验日期：2026-08-01。

### 1.1 全仓零安全扫描依赖与 CI job（关键字零命中）

ERP 全仓无任何安全扫描工具链接入——无 OWASP Dependency-Check、无 SpotBugs/FindSecBugs、无 SonarQube、无 Snyk。156 模块的传递依赖 CVE 风险完全未知，静态代码安全规则（注入 / XSS / 反序列化 / 硬编码密钥）完全无人看。

- 核验命令（2026-08-01 复核零命中）：`rg -il "owasp|dependency-check|spotbugs|findsecbugs|sonarqube|snyk" --glob '*.xml' --glob '*.yml' --glob '*.yaml'`（工作目录 = nop-app-erp）→ **EXIT=1（零命中）**。覆盖范围：nop-app-erp 工作树内全部 `pom.xml` / CI workflow / YAML 配置——无安全扫描插件声明、无安全扫描 CI job、无安全扫描脚本。
- 引用源：roadmap line 697 + line 784-785（Q2 维度说明）；Q0 README §范围矩阵 §Q2（核验日期 2026-08-01）；Q0 plan §Current Baseline NOT FOUND 证据第 2 条。
- 后果：当前「依赖链安全」与「静态代码安全」无任何基线。156 模块经 Maven 传递引入大量第三方库（Spring/Quarkus 生态、Nop 平台、H2/PostgreSQL 驱动、Jackson 等），其已知 CVE（CVE 数据库收录的漏洞）从未被查询；静态安全规则（SQL 注入 / XSS / 不安全反序列化 / 硬编码密钥）从未被扫描。

### 1.2 既有 CI 工作流清单（零安全扫描 job）

`.github/workflows/` 共 **5 个** workflow，均无安全扫描 job：

| workflow 文件 | 触发 | 职责 |
|---------------|------|------|
| `maven.yml` | push/PR | `mvn -B package` 全量构建 + 单元测试 |
| `compliance.yml` | push/PR/dispatch | F8 反模式基线单向收紧（19 规则）+ F15 i18n 覆盖回归 + web 页面校验 + Q4 故障注入覆盖率门控 |
| `e2e.yml` | push/PR | Playwright E2E（260+ spec） |
| `mutation.yml` | nightly | Q1 pitest 变异测试 nightly 软门控 |
| `clock-rollover.yml` | nightly | Q6 时钟翻车夜间回归 |

- 核验命令（2026-08-01 复核）：`ls .github/workflows/` → 上述 5 文件；`rg -il "owasp|dependency-check|spotbugs|findsecbugs|sonarqube|snyk" .github/workflows/` → EXIT=1（零命中）。
- 引用源：本计划 §Current Baseline（5 CI job 清单已核验）；Q0 plan §Current Baseline。
- **关键观察（Q2 集成参照）**：`compliance.yml` 已沉淀**单向收紧门控范式**——F8 checker 作纯 reporter，gate 逻辑（解析输出 + 比对 `compliance-baseline.md` `## BASELINE (machine-readable)` 块）活在 CI（Python 内联脚本）。Q2 安全扫描基线须对齐此范式：首次扫描必然有发现 → 分类 → 基线落盘 → 单向收紧（actual > baseline => CI red）。

### 1.3 MA6 RBAC 基线边界（Q2 与之正交，不重复）

MA6（安全审计，A6.1-A6.4 全 done）覆盖的是**运行时授权维度**，与 Q2（依赖链 CVE + 静态代码安全）**正交互补不重复**：

| MA6 维度 | 落地状态 | 与 Q2 的正交关系 |
|----------|----------|-------------------|
| **action-level 权限注解**（R2.7 / P1-MA3-046） | `@BizMutation`/`@BizQuery` 注解完整性审计 done；config-gated `enable-action-auth` 默认 OFF | 运行时「谁能调此 action」——Q2 不涉及运行时授权 |
| **SoD 职责分离**（R3.3） | purchase/sales/finance/manufacturing 4 域 approve 路径 SoD 程序级守卫（`SoDGuard` 比对 createdBy vs 审核人） | 运行时「创建人不可审核」——Q2 不涉及业务流程 SoD |
| **角色侧行级过滤**（R3.4 / P1-MA6-002） | `erp-*.data-auth.xml` 规则 + config-gated `ErpRoleDataAuthChecker`，双层默认 OFF | 运行时「此角色能看到哪些行」——Q2 不涉及数据可见性 |
| **保护区域纪律**（A6.4） | 保护区域过程纪律审计 done | 开发流程纪律——Q2 不涉及过程纪律 |

- 引用源：`docs/design/roles-and-permissions.md` §运行基线（action-level / 数据权限 / SoD 落地状态）；roadmap line 742-743（MA6 A6.1-A6.4 done）；本计划 §Current Baseline。
- 核验命令（2026-08-01 复核命中）：`rg -l "SoDGuard|ErpRoleDataAuthChecker" --glob '*.java'` → 命中共享守卫 `SoDGuard`（4 S/A 域 approve SoD）+ `ErpRoleDataAuthChecker`（角色侧行级过滤 config-gated）；`rg -l "erp-.*\.data-auth\.xml|data-auth" module-* --glob '*.xml'` → 命中各域 data-auth 规则文件。
- **正交结论**：MA6 是「**授权**——已认证用户能做什么/能看到什么」；Q2 是「**漏洞**——依赖链中的已知 CVE + 静态代码中的安全反模式」。二者维度不同，Q2 不重复 MA6，MA6 也不替代 Q2（授权正确不代表代码没有注入漏洞，反之亦然）。

### 1.4 单向收紧门控先例（Q2 须对齐的范式）

本仓已沉淀**4 个单向收紧门控先例**，Q2 安全扫描基线须对齐此范式：

| 先例 | 载体 | 门控形态 | 首次建立策略 |
|------|------|----------|--------------|
| **F8 compliance checker** | `compliance-baseline.md` §BASELINE 机器可读块 + `compliance.yml` Python 比对 | actual > baseline => CI red | 首次扫描 → 逐项合法性分类 → 基线落盘 |
| **F15 i18n checker** | `compliance.yml` i18n job（DEFECTS=0/GAPS=0 硬编码 + 指向 compliance-baseline.md §F15 注记） | actual > baseline => CI red | A4.9 全域 i18n 审计零基线锚定 |
| **Q1 mutation** | `mutation.yml` nightly 软门控 + `mutation-baseline.md` | mutation score 单向收紧 | 首跑实测基线 + 存活变异体分类 |
| **Q4 fault-injection** | `compliance.yml` `fault-injection-coverage` job（6 域 grep 命中数 ≥ baseline） | 覆盖域不下降 | 首轮 6 域 harness 落盘锚定 |

- 引用源：`compliance-baseline.md` §回归门控规则（门控方向：单向收紧 / 调高基线的唯一途径）；`fault-injection.md` §7（Q4 CI 门控裁决）；`mutation-testing.md`（Q1 nightly 软门控）。
- 核验命令（2026-08-01 复核命中）：`rg "## BASELINE \(machine-readable\)" docs/audits/compliance-baseline.md docs/architecture/quality-engineering/mutation-baseline.md` → 两文件均含机器可读块（F8 per-rule 计数 + Q1 per-domain mutation score），证实单向收紧 + 机器可读块范式先例。
- **Q2 对齐要点**：(1) 安全扫描工具作**纯 reporter**（输出 CVE/规则违规清单），gate 逻辑活在 CI；(2) 首次扫描必然有发现 → 分类工作流（真实漏洞 vs 误报 vs 已接受风险）→ 基线落盘；(3) 单向收紧——新增发现（actual > baseline）=> CI red，调高基线须开独立计划裁决。

## 2. 目标与非目标

### 2.1 目标（Phase 1 = 本文档；Phase 2 实现见 §6）

1. **裁决 CVE 依赖扫描工具选型**（§3.1），给出候选、考虑的替代、残留风险三要素——满足 plan authoring guide §规则 9（Decision 项记录理由）。
2. **裁决静态安全规则工具选型**（§3.2），覆盖注入 / XSS / 反序列化 / 硬编码密钥规则集。
3. **裁决 CI 调度策略**（§4），显式裁决 CVE 限速下的扫描调度（nightly aggregate vs per-commit vs weekly）——roadmap line 784 明示的关键风险。
4. **裁决基线建立策略**（§5），首次扫描发现分类工作流 + 单向收紧门控形态，对齐 compliance-baseline 范式。
5. **为 Phase 2 实现 plan 提供实施契约**（§6）：Dependency-Check + FindSecBugs 接入步骤 + 首次扫描 + 分类 + 基线落盘。
6. **裁决 CI 门控形态**（§8）：nightly/weekly job 设计 + 与现有 5 CI workflow 不冲突 + 集成方式。
7. **声明与 MA6 的正交边界**（§9）：Q2（依赖链 CVE + 静态代码安全）与 MA6（运行时 RBAC 授权）正交互补不重复。

### 2.2 非目标

- **不实现任何代码/ORM/CI 变更**——本文档仅产出设计。Phase 2 实现（Dependency-Check + FindSecBugs 接入 CI + 首次扫描 + 基线落盘）是独立后续 plan，须在本文档审查收敛后方可起草（MQ 文档先行工作流硬约束）。
- **不修改 `nop-entropy` 源码**——安全扫描在依赖树 + 编译产物层面，不动平台生产代码（§6.3 边界声明）。
- **不替代运行时 RBAC（MA6）**——MA6 action-level + data-row-level + SoD 是授权维度；Q2 是依赖链 + 静态代码安全维度，正交（§9）。
- **不修复首次扫描发现的具体 CVE/规则违规**——那是 Phase 2 基线建立后的分类修复 workflow，不属本 Phase 1 设计文档范围（§Deferred）。
- **不重新推导 NOT FOUND 证据**——§1 引用 Q0 README + roadmap + compliance-baseline + roles-and-permissions，避免双真相源。
- **不编写 Q3/Q5 设计**——同批独立 sibling plan（Q3 done / Q5 todo）。

## 3. 技术选型 —— CVE 依赖扫描 + 静态安全规则

### 3.1 CVE 依赖扫描工具选型

> 决策输入：§1 现状（全仓零 CVE 扫描 + 156 模块传递依赖）+ roadmap line 784（Dependency-Check 为默认——开源本地可跑）+ Q0 README §复杂度分级（Q2 基础设施接入高 = NVD 限速）。

**候选 A —— OWASP Dependency-Check（候选首选）**

- **机制**：Maven 插件 `org.owasp:dependency-check-maven`，扫描项目全部传递依赖的 Maven 坐标（GAV），比对 NVD（National Vulnerability Database）CVE 数据库，输出 HTML/JSON 报告（每个依赖的已知 CVE + CVSS 严重度）。支持 `aggregate` 模式（聚合多模块 reactor 到单一报告，一次 NVD 查询）。
- **优点**：
  - **开源本地可跑**——无商业许可，NVD 数据库可本地缓存（首次下载 ~数百 MB，后续增量更新），离线可重跑扫描。
  - **Maven 插件原生集成**——`<plugin>` 声明 + `aggregate` goal，与本项目 Maven 多模块结构契合，无需独立服务。
  - **规则可控**——可配置 suppression 文件（`suppression.xml`）标记误报，基线/suppression 均纳入版本控制可审计。
- **缺点 / 风险**：
  - **NVD API 限速**——首次全量下载 NVD 数据库 + 每日增量更新受 NVD API 限速（roadmap line 784 明示关键风险），per-commit 全量扫描不现实（见 §4 调度裁决）。
  - **误报率**——GAV 匹配可能误报（依赖虽含 CVE 但实际未使用受影响代码路径 / 版本范围宽松匹配）。须 §5 分类工作流 + suppression 文件吸收。
  - **NVD 数据库时效性**——NVD 收录有延迟（CVE 公开到 NVD 收录可能数小时到数天），最新零日漏洞不覆盖。

**候选 B —— Snyk（替代）**

- **机制**：商业 SaaS（Snyk CLI / Maven 插件），扫描依赖 + 容器 + IaC，比 Snyk 自有漏洞数据库（含 NVD + 专有数据）。
- **优点**：漏洞数据库更全面（含 NVD 之外的专有数据）+ 修复建议（升级路径）+ 无限速。
- **缺点 / 风险**：
  - **需联网 + 商业许可**——Snyk 是 SaaS，须 API key + 联网扫描，免费额度有限（本项目 156 模块规模超出免费层）；商业许可引入采购成本与合规审查。
  - **离线不可跑**——CI 须能访问 Snyk API，与本项目「本地可跑 / 离线可复现」基线不一致。
  - **数据主权**——扫描结果经 SaaS，依赖树信息外流（虽 Snyk 有隐私承诺，但本项目偏本地可控）。

**候选 C —— GitHub Dependabot（替代）**

- **机制**：GitHub 平台内建，自动扫描 `pom.xml` 依赖，PR 形式提示升级。
- **优点**：零配置（GitHub 仓库直接启用）+ PR 形式可追溯。
- **缺点 / 风险**：
  - **规则可控性低**——Dependabot 规则与 GitHub 后端绑定，无法本地复现扫描结果，无法自定义 suppression / 基线比对。
  - **门控形态不匹配**——Dependabot 是「提示升级」非「基线单向收紧」，与本项目 compliance-baseline 范式（actual > baseline => CI red）不对齐。
  - **平台耦合**——若未来迁移到非 GitHub 平台则失效。

#### 3.1 裁决（Decision）

**裁决：选候选 A（OWASP Dependency-Check）作为 CVE 依赖扫描工具。候选 B（Snyk）与候选 C（Dependabot）否决。**

**裁决理由**：

1. **开源本地可跑（对齐本项目基线）**——本项目验证基线是 `mvn clean install -DskipTests` + `mvn test` 本地可复现（`docs/context/project-context.md`）。Dependency-Check 是 Maven 插件 + 本地 NVD 缓存，离线可重跑扫描，与「本地可控」基线一致。Snyk 需联网 + 商业许可，Dependabot 需 GitHub 平台，二者均引入外部依赖。
2. **Maven 插件原生 + aggregate 模式契合 156 模块**——Dependency-Check `aggregate` goal 聚合多模块 reactor 依赖树到单一报告，一次 NVD 查询覆盖全域，是应对 NVD 限速的关键机制（见 §4 调度裁决）。Snyk/Dependabot 无等价的 aggregate 聚合控制。
3. **规则可控 + 对齐单向收紧范式**——Dependency-Check 输出可解析的 JSON 报告 + suppression 文件版本控制，可建立基线（CVE 计数/清单）+ CI 比对单向收紧（§5），对齐 F8 compliance-baseline 范式。Dependabot 是提示型非门控型，不匹配。
4. **roadmap 默认 + 文档先行工作流要求**——roadmap line 784 明示「Dependency-Check 为默认——开源本地可跑；Snyk 需联网+商业许可」，本文档裁决与之对齐。

**考虑的替代（记录为何否决）**：

- **候选 B（Snyk）**：否决——商业许可 + 联网 + 离线不可跑 + 数据主权。保留为 successor（触发：项目获得 Snyk 商业许可 + 要求更全漏洞数据库时，可与 Dependency-Check 并行作交叉验证）。
- **候选 C（Dependabot）**：否决——规则可控性低 + 门控形态不匹配（提示型非基线门控）+ 平台耦合。保留为 successor（触发：仅需 GitHub 内建提示无需严格基线门控时）。
- **维持现状（零 CVE 扫描）**：否决——156 模块传递依赖 CVE 风险完全未知，违反「安全可见性」基本要求。

**残留风险**：

- **R1（NVD 限速）**：Dependency-Check NVD 数据库首次下载 + 每日增量受 NVD API 限速（2024+ 无 key 限速严苛）。§4 裁决 nightly aggregate + 本地缓存 + **免费 NVD API key（预期必需，§4.3）**应对。per-commit 全量扫描不现实。
- **R2（误报率）**：GAV 匹配误报须 §5 分类工作流 + suppression 文件吸收。首次扫描误报分类是 Phase 2 工作量。
- **R3（NVD 时效性）**：NVD 收录延迟，最新零日漏洞不覆盖。接受（CVE 扫描定位「已知漏洞基线」非「零日防护」）。

### 3.2 静态安全规则工具选型

> 决策输入：§1 现状（全仓零静态安全规则扫描）+ roadmap line 784（FindSecBugs 规则集）+ 本项目 Java 技术栈。

**候选 D —— SpotBugs + FindSecBugs（候选首选）**

- **机制**：SpotBugs（FindBugs 继承者）是 Java 字节码静态分析框架；FindSecBugs 是 SpotBugs 的安全规则插件，覆盖注入（SQL/HXXS/Command）/ XSS / 不安全反序列化 / 硬编码密钥 / 路径遍历 / 弱加密等规则集。Maven 插件 `com.github.spotbugs:spotbugs-maven-plugin` + `findsecbugs-plugin` 依赖。
- **优点**：
  - **开源本地可跑**——Maven 插件，编译产物字节码分析，无网络依赖，离线可跑。
  - **安全规则集成熟**——FindSecBugs 是 OWASP 推荐的 Java 安全扫描器，规则集覆盖 OWASP Top 10 相关的静态漏洞模式。
  - **与 Nop 字节码兼容**——SpotBugs 分析编译后 `.class` 字节码，不依赖源码 AST，与 Nop 代码生成产物（`_gen` 包）兼容（生成代码也会被扫描，但可配置排除）。
- **缺点 / 风险**：
  - **误报率**——静态分析启发式规则存在误报（如框架安全包装的 SQL 调用被误报为注入）。须 §5 分类 + suppression/Filter 吸收。
  - **规则集覆盖边界**——FindSecBugs 覆盖静态可检测的漏洞模式，不覆盖业务逻辑漏洞 / 运行时授权（后者归 MA6 RBAC）。
  - **与 Nop XPL/Dynamic Dispatch 交互**——Nop 平台大量用反射/XPL 动态分发，SpotBugs 的静态调用图可能不完整，部分规则可能漏报（动态分发的真实调用链不在字节码静态分析范围）。

**候选 E —— SonarQube（替代）**

- **机制**：独立静态分析平台（SonarQube Server + SonarScanner），扫描代码质量 + 安全 + 覆盖率，含自有安全规则集 + 集成 FindSecBugs。
- **优点**：规则集更全（质量 + 安全 + 热点）+ Web 仪表盘 + 增量分析。
- **缺点 / 风险**：
  - **重，需独立服务**——SonarQube 须独立部署 Server（数据库 + Web），运维成本高，与本项目「轻量 CI」基线不一致。
  - **离线不可跑**——Scanner 须连 Server，本地开发者无法独立复现扫描结果。
  - **门控形态**——SonarQube Quality Gate 是平台内门控，与本项目 compliance-baseline「gate 逻辑活在 CI」范式不同（虽可经 CI webhook 集成，但增加复杂度）。

#### 3.2 裁决（Decision）

**裁决：选候选 D（SpotBugs + FindSecBugs）作为静态安全规则扫描工具。候选 E（SonarQube）否决。**

**裁决理由**：

1. **开源本地可跑 + Maven 插件原生**——与 Dependency-Check 同属「Maven 插件 + 本地可跑」范式，CI 集成一致（同一 Maven 生命周期）。SonarQube 需独立 Server，运维成本与本项目轻量基线不一致。
2. **FindSecBugs 安全规则集成熟（OWASP 推荐）**——覆盖注入/XSS/反序列化/硬编码密钥等 roadmap line 784 要求的规则集。
3. **字节码分析与 Nop 兼容**——SpotBugs 分析编译产物，不依赖源码 AST，对 Nop 代码生成产物兼容（§6.2 配置排除生成代码噪声）。
4. **对齐单向收紧范式**——SpotBugs 输出 XML 报告可解析 + Filter 文件版本控制，可建立基线 + CI 比对单向收紧（§5）。

**考虑的替代**：

- **候选 E（SonarQube）**：否决——独立 Server 运维成本 + 离线不可跑 + 门控形态不匹配。保留为 successor（触发：项目要求代码质量仪表盘 + 增量分析 + 获 SonarQube 运维资源时）。
- **仅 CVE 扫描不做静态规则**：否决——CVE 覆盖依赖链已知漏洞，静态规则覆盖自有代码的安全反模式，二者正交互补（依赖安全 ≠ 代码安全）。

**残留风险**：

- **R4（误报率）**：FindSecBugs 启发式误报须 §5 分类 + Filter 吸收。
- **R5（Nop XPL/Dynamic Dispatch 漏报）**：静态分析对动态分发的真实调用链不完整，部分注入路径可能漏报。接受（静态扫描定位「静态可检测反模式基线」，动态路径归运行时测试 + Q4 故障注入）。
- **R6（生成代码噪声）**：Nop 代码生成产物（`_gen` / `api.beans` / `api.crud`，见 `mutation-testing.md` §1.3 三类 352 生成类）会被 SpotBugs 扫描。须 §6.2 配置 `excludeFilterFile` 排除（对齐 Q1 pitest `excludedClasses` 先例），否则规则违规被生成代码噪声主导。
- **R6.1（Java 21 + FindSecBugs 版本矩阵）**：SpotBugs 4.8+ 支持 Java 21，但 `findsecbugs-plugin` 维护节奏慢，与 SpotBugs 4.8+/Java 21 的兼容性须 Phase 2 首跑验证（对齐 Q1 pitest 1.25.8 经 Phase 2 R4 复核先例）。若不兼容，Phase 2 裁决升级/回退版本。

## 4. CI 调度裁决（CVE 限速下的扫描调度策略）

> roadmap line 784 明示关键风险：「156 模块传递依赖 CVE 查询受 NVD API 限速，per-commit 全量扫描不现实」。本节裁决调度策略，给出候选、考虑的替代、残留风险。

### 4.1 候选调度路径

**路径 A —— nightly aggregate 全量扫描 + per-commit 跳过**

- **机制**：Dependency-Check 配置 `aggregate` 模式，nightly CI job（`schedule: cron`）聚合 156 模块依赖树一次 NVD 查询（本地缓存 NVD 数据库，job 内更新一次），per-commit 跳过 CVE 扫描（仅跑 SpotBugs 静态规则，因不限速）。
- **优点**：NVD 限速下唯一可行——一次 nightly 查询覆盖全域，不阻塞 PR。
- **缺点 / 风险**：发现延迟（最长 24h）——凌晨引入的 CVE 依赖次日 nightly 才发现。

**路径 B —— per-commit 增量扫描（仅 changed module）**

- **机制**：每个 PR 仅扫描变更模块的依赖（`-pl <changed-module>`），增量查询 NVD。
- **优点**：发现及时（PR 即时反馈）。
- **缺点 / 风险**：**NVD 限速可能阻塞 PR**——即使增量，NVD 数据库更新仍受限速；且传递依赖可能跨模块（改 A 模块引入 B 模块传递的 CVE），增量扫描漏报。roadmap line 784 明示 per-commit 全量不现实。

**路径 C —— weekly 全量 + per-commit 仅 FindSecBugs（静态规则不限速）**

- **机制**：weekly（如周一）Dependency-Check aggregate 全量 + per-commit 仅 SpotBugs/FindSecBugs（静态规则不查 NVD，不限速）。
- **优点**：CVE 发现延迟容忍为 weekly，per-commit 静态规则即时反馈。
- **缺点 / 风险**：CVE 发现延迟拉长到最长 7 天；weekly 粒度对紧急 CVE 响应慢。

### 4.2 裁决（Decision）

**裁决：选路径 A（nightly aggregate 全量 CVE 扫描 + per-commit 跳过 CVE / 仅静态规则可选）作为主路径。路径 B（per-commit 增量 CVE）否决。路径 C（weekly）作为路径 A 的降级备选（若 nightly 资源紧张时切换）。**

**裁决理由**：

1. **NVD 限速下唯一可行（roadmap line 784 硬约束）**——Dependency-Check aggregate 模式聚合 156 模块依赖树一次 NVD 查询 + 本地缓存复用，是应对限速的关键机制。per-commit 全量/增量都会触发 NVD 查询限速，阻塞 PR（路径 B 否决）。
2. **nightly 对齐既有先例**——本仓 `mutation.yml`（Q1 pitest nightly）+ `clock-rollover.yml`（Q6 nightly）已建立 nightly 软门控先例。Q2 CVE nightly 与之一致，不引入新调度形态。
3. **per-commit 不阻塞构建（Non-Goal 守护）**——CVE 扫描仅 nightly，per-commit 的 `maven.yml` 构建测试不受影响（plan Goals：「不阻塞 per-commit 构建」）。
4. **静态规则（FindSecBugs）不限速可 per-commit（可选）**——SpotBugs 不查 NVD，可在 per-commit 跑。但首跑建议先 nightly（建立基线 + 分类误报），基线稳定后再决定是否 per-commit（§8 CI 门控设计）。

**考虑的替代**：

- **路径 B（per-commit 增量）**：否决——NVD 限速阻塞 PR + 传递依赖跨模块漏报。roadmap line 784 明示不现实。
- **路径 C（weekly）**：降级为备选——CVE 发现延迟从 24h 拉长到 7 天，对紧急 CVE 响应慢。仅在 nightly CI 资源紧张时切换。

**残留风险**：

- **R7（nightly 发现延迟）**：凌晨引入的 CVE 依赖最长次日 nightly 发现（≤24h）。接受（CVE 扫描定位「已知漏洞基线」非实时防护；紧急 CVE 经人工评估另行处理）。
- **R1（NVD 限速，调度应对）**：nightly aggregate + 本地缓存应对；若 NVD 限速仍导致 nightly 超时，Phase 2 须评估 NVD API key（免费但需注册，提高限速配额）或镜像源。

### 4.3 NVD 数据缓存策略

> Phase 2 实施关注，本 Phase 1 评估策略方向 + 关键可行性约束。

- **本地缓存**：Dependency-Check 首次下载 NVD 数据库到本地（`dependency-check-data` 目录，NVD JSON feed 现 **~1-2 GB**），后续 nightly job 增量更新（仅拉取新增 CVE）。CI 用 GitHub Actions `actions/cache` 缓存 NVD 数据库目录，避免每夜全量下载。
- **GitHub Actions cache 策略**：(1) cache-key 须含日期或 NVD 元数据版本以触发增量更新（如 `dependency-check-nvd-${{ steps.date.outputs.today }}` + `restore-keys: dependency-check-nvd-` 前缀回退到最近缓存）；(2) 须核算 cache 预算——GitHub Actions 每 repo 总 cache 上限 10 GB，须与既有 `mutation.yml` maven cache 等竞争者合计不超限（nightly 访问保持 cache 热，7 天淘汰窗口内有效）。
- **NVD API key（预期必需，非可选）**：2024+ NVD 大幅收紧无 key 限速（无 key ~5 req/30s，有 key ~50 req/30s）。156 模块 aggregate 首跑无 key 大概率超时。**Phase 2 须注册 NVD 免费 API key**（免费，无需商业许可，对齐「本地可控」），作为首跑可行性的前提；无 key 仅作降级备选（限速下跑通但耗时不可接受）。
- **离线模式**：基线建立后，可用缓存的 NVD 数据库离线重跑扫描（复现基线），不依赖实时 NVD。

## 5. 基线建立策略（首次扫描发现分类工作流 + 单向收紧）

> 对齐 `compliance-baseline.md` §回归门控规则范式：首次扫描必然有发现 → 分类 → 基线落盘 → 单向收紧（actual > baseline => CI red）。

### 5.1 首次扫描发现分类工作流

首次 Dependency-Check + FindSecBugs 扫描必然产生大量发现（156 模块传递依赖 + 既有代码）。发现须经**四态分类**后才能建立基线：

| 分类 | 含义 | 处置 |
|------|------|------|
| **A 真实漏洞** | 确认存在的 CVE / 确认的安全反模式 | Critical/High 须修复（Phase 2 后的修复 workflow）；Medium/Low 可登记为已接受风险（基线） |
| **B 误报** | GAV 匹配误报（依赖含 CVE 但未用受影响代码）/ 框架安全包装误报 | 经 suppression（Dependency-Check）/ Filter（SpotBugs）标记排除，版本控制 |
| **C 已接受风险** | 真实漏洞但当前接受（如无法升级的间接依赖 / 低 CVSS + 不暴露面） | 登记基线 + 注明接受理由 + successor（升级路径/暴露面变化时复审） |
| **D 生成代码噪声** | Nop 代码生成产物（`_gen`/`api.beans`/`api.crud`）的安全规则命中 | 配置排除（§6.2），不纳入基线 |

> 引用先例：`compliance-baseline.md` R3 三态分类（A 误报 / B 合法 / C 瞬态）+ Q1 pitest 生成代码排除（`mutation-testing.md` §1.3 / §3.3 `excludedClasses`）。Q2 安全扫描分类对齐此范式。
> **注**：本节 A/B/C/D 标签为 Q2 安全扫描分类**独立定义**，与 compliance-baseline R3 三态标签（A 误报 / B 合法 / C 瞬态）语义**不同**（Q2 的 A=真实漏洞 vs R3 的 A=误报），仅对齐「多态分类先例」范式，标签不复用。

### 5.2 基线载体裁决

**裁决：CVE 基线与静态规则基线各自独立载体文件，不复用 `compliance-baseline.md` 新增 §F 块。**

**裁决理由**：

- `compliance-baseline.md` 已承载 F8（19 规则）+ F15 i18n 基线，是**代码架构反模式**门控基线；Q2 是**安全漏洞**门控基线，语义维度不同（反模式 ≠ 漏洞）。混入会模糊 compliance-baseline 的「架构反模式」焦点。
- Q2 CVE 基线形态是**漏洞清单**（CVE-ID × 依赖 × CVSS × 分类），非数值计数（F8 是 per-rule 计数）。清单形态更适合独立文件。
- 独立载体便于 Phase 2 首次扫描分类工作的迭代（基线建立期间频繁更新），不污染 compliance-baseline 的稳定性。
- **先例对齐**：Q1 已落 `mutation-baseline.md` 独立载体（per-domain mutation score 机器可读块，`mutation-baseline.md` §BASELINE），证实独立基线文件范式可行。

**残留风险**：独立载体文件增加一处导航点（CVE/静态基线与 F8 架构反模式基线分离），接受（语义维度不同，混入会模糊 compliance-baseline 焦点；Q1 已有独立基线文件先例）。

**基线载体（Phase 2 落盘）**：

- `docs/architecture/quality-engineering/security-baseline.md`（新建）——CVE 基线清单（Dependency-Check）+ 静态规则基线计数（SpotBugs/FindSecBugs per-rule），含 `## BASELINE (machine-readable)` 块供 CI 比对（对齐 compliance-baseline 机器可读块范式，具体格式见 §5.4）。
- suppression 文件（`dependency-check-suppression.xml`）+ SpotBugs Filter 文件（`spotbugs-exclude.xml`）纳入版本控制，记录误报排除理由。

### 5.3 单向收紧门控形态

对齐 `compliance-baseline.md` §回归门控规则：

- **门控方向：单向收紧**。新增发现（actual > baseline）→ CI 失败（regression）。发现数下降（actual < baseline）→ CI 通过，鼓励更新基线反映改善。
- **调高基线的唯一途径**：开独立计划，逐项人工确认新增发现的合理性（真实漏洞须修复或登记已接受风险 / 误报须 suppression），显式更新基线文件 + 机器可读块。**禁止在功能 PR 中直接调高基线**。
- **门控实现方式**：nightly CI job（`.github/workflows/security.yml` 或加 job 到既有 workflow）解析 Dependency-Check/SpotBugs 报告，比对 `security-baseline.md` 机器可读块。工具作纯 reporter，gate 逻辑活在 CI（对齐 F8 option b）。

### 5.4 基线格式与门控比对语义裁决（Decision）

> R2 审查要求：CVE 是**清单**（set-based）而 SpotBugs 是**计数**（count-based），两者比对语义不同，须在 Phase 1 裁决基线机器可读格式 + 门控比对语义，否则 Phase 2 编写 Python gate 无法推进。

**裁决：CVE 用 set-allowlist 比对 + SpotBugs 用 per-bug-type count 比对，双语义经同一 Python gate 脚本两段处理。机器可读块 schema 如下。**

**裁决理由**：

1. **CVE 必须 set-allowlist（非 count）**——CVE 比对若用总计数（如 `CVE_HIGH: 12`），会**漏看置换回归**（旧 CVE 修复 + 新 CVE 引入，净计数不变但实际有新漏洞）。安全语义要求**逐条身份比对**：actual 报告中的每个 `(cveId, dependencyGav)` 元组必须在 baseline allowlist 中，否则为新发现 ⇒ fail。allowlist 大但精确（每个已接受/已分类 CVE 独立登记 + 分类标签 + 接受理由）。
2. **SpotBugs 用 per-bug-type count（非 set）**——静态规则违规数量大，逐条身份比对（每条违规的 file:line）会因重构导致的行号漂移产生海量噪声 diff，不可维护。per-bug-type 计数（如 `XSS_SQL_INJECTION: 3`）对齐 F8 per-rule 计数范式，容忍行号漂移，单向收紧（某类计数增长 ⇒ fail）。
3. **双语义经同一脚本两段处理**——Python gate 脚本（对齐 F8 option b，活在 CI）解析两份报告：Dependency-Check JSON → 提取 `(cveId, gav)` 集合做 set-diff；SpotBugs XML → 按 `BugPattern/@type` 聚合计数做 count-diff。两段独立判定，任一 fail 则 CI red。

**`## BASELINE (machine-readable)` schema（Phase 2 落盘，本 Phase 1 定 schema）：**

```yaml
# security-baseline.md machine-readable block. CI gate parses this.
# Two segments with DIFFERENT comparison semantics:
#   cve_allowlist  -> set-membership (any actual finding NOT listed => FAIL)
#   spotbugs_counts -> per-type count (actual > baseline => FAIL)
# Raising either requires an independent adjudication plan (see §5.3).

cve_allowlist:
  # each entry = a classified/accepted (cveId, dependencyGav) tuple.
  # format: "<cveId> @ <group:artifact:version>"
  # An entry here means: triaged (real-vuln-accepted / false-positive-suppressed
  # is handled by dependency-check-suppression.xml, not here).
  # Empty list on first run before triage; populated as §5.1 classification completes.
  - example: "CVE-XXXX-YYYY @ org.example:lib:1.2.3"  # placeholder, removed at Phase 2

spotbugs_counts:
  # key = SpotBugs BugPattern type (FindSecBugs rule id); value = baseline count.
  # one-way tightening: actual count > baseline => FAIL.
  # Empty (0) baseline = first run target after §5.1 classification.
  EXAMPLE_XSS: 0  # placeholder keys; real keys populated at Phase 2 from first-scan report
```

> **Phase 2 gate 实现指引**（对齐 F8 `compliance.yml` Python 内联脚本范式）：CVE 段解析 Dependency-Check `dependency-check-report.json` 的 `dependencies/dependency/vulnerabilities/vulnerability` 提取 `(cveId, gav)` 集合，与 `cve_allowlist` 做 set-diff，非空 ⇒ fail；SpotBugs 段解析 `spotbugsXml.xml` 的 `BugInstance/@type` 聚合计数，与 `spotbugs_counts` 逐 type 比对，actual > baseline ⇒ fail。suppression（Dependency-Check）/ Filter（SpotBugs）文件在扫描阶段即排除误报，故 allowlist/counts 反映**分类后**的真实基线。

## 6. 实施步骤（Phase 2 实现 plan 的范围契约）

> 本节为 Phase 2 实现 plan 提供步骤骨架与边界声明。Phase 2 plan 起草时以本节为实施契约，可细化但不得偏离已裁决的工具（Dependency-Check + FindSecBugs）+ nightly aggregate 调度范围。

### 6.1 Dependency-Check 接入（CVE 依赖扫描）

1. **Maven 插件配置**：在根 `pom.xml` 或独立 profile 声明 `org.owasp:dependency-check-maven` 插件，配置 `aggregate` 模式（聚合 156 模块依赖树）+ 输出格式（HTML + JSON）+ suppression 文件路径 + NVD 数据库缓存目录。
   - **插件位置裁决**：Dependency-Check `aggregate` 是**reactor-root goal**（从根 pom 聚合全部子模块依赖树，一次扫描产出单一报告）——在根 pom 声明即可生效，**Q1 R4 根 pom 继承失效问题不适用**（Q1 `mutation-baseline.md` §2：根 pom `app-erp` 仅为 reactor 聚合器，per-module goal 经 profile 继承失效，须 per-module 声明）。Dependency-Check aggregate 由 reactor-root 显式调用（`mvn -N dependency-check:aggregate` 或根 profile），不经子模块 parent 链继承，故 Q1 R4 陷阱不触发。Phase 2 须核验 aggregate 在 reactor-root 生效（非 per-module 继承）。
2. **首次扫描**：执行 `mvn dependency-check:aggregate`，下载 NVD 数据库（首次耗时较长），产出全量 CVE 报告。
3. **发现分类**：按 §5.1 四态分类（真实漏洞 / 误报 / 已接受风险 / 生成代码噪声）逐项处理。
4. **suppression 文件落盘**：误报（B 类）经 `dependency-check-suppression.xml` 标记排除，注明理由。
5. **基线落盘**：`security-baseline.md` 记录 CVE 基线清单（`cve_allowlist` 段，§5.4 schema）+ 机器可读块。

### 6.2 FindSecBugs 接入（静态安全规则扫描）

1. **Maven 插件配置 + 位置裁决**：声明 `com.github.spotbugs:spotbugs-maven-plugin` + `findsecbugs-plugin` 依赖。**插件位置裁决=per-module（`module-*/erp-*-service` 各声明 plugin）**，对齐 Q1 R4 先例——SpotBugs 分析**每模块编译产物字节码**，与 Q1 pitest per-module instrumentation 架构相同，根 pom profile 继承失效（`mutation-baseline.md` §2：根 pom 仅 reactor 聚合器，子模块 `<parent>` 是 nop-entropy），须 per-module 声明（或经 `<pluginManagement>` 集中版本 + per-module 显式启用）。Phase 2 须验证 per-module 生效（与 Q1 pitest 同型核验）。
   - **规则激活**：经 `includeFilterFile`（`spotbugs-include.xml`，**启用** FindSecBugs 安全规则模式）配置，非 `effort`/`threshold` 等价物。两者是不同机制：`includeFilterFile` 选择启用哪些 BugPattern；`excludeFilterFile` 选择抑制哪些匹配。
2. **生成代码排除**：经 `excludeFilterFile`（`spotbugs-exclude.xml`，**抑制** Nop 生成代码匹配）排除生成代码包（`_gen` / `api.beans` / `api.crud`，对齐 Q1 pitest `excludedClasses` 先例，见 `mutation-testing.md` §1.3 三类 352 生成类），避免规则违规被生成代码噪声主导。
3. **首次扫描**：执行 `mvn spotbugs:check`（per-module），产出全量静态规则报告（XML）。
4. **发现分类**：按 §5.1 四态分类。
5. **Filter 文件落盘**：误报经 `spotbugs-exclude.xml` 标记排除（与 step 2 生成代码排除同文件，按 `<Match>` 元素区分生成代码 vs 误报）。
6. **基线落盘**：`security-baseline.md` 记录静态规则基线（`spotbugs_counts` 段，§5.4 schema）+ 机器可读块。
7. **Java 21 兼容性核验（Phase 2 首跑）**：CI `setup-java java-version: '21'`（`compliance.yml`/`mutation.yml` 等全域 21）。SpotBugs 4.8+ 支持 Java 21 字节码；`findsecbugs-plugin` 维护节奏较慢，与 SpotBugs 4.8+/Java 21 的版本矩阵须 Phase 2 首跑验证（对齐 Q1 pitest 1.25.8 经 Phase 2 R4 复核支持 Java 21-26 先例）。若 FindSecBugs 最新版与 Java 21 不兼容，升级 FindSecBugs 或回退 SpotBugs 兼容版本（Phase 2 裁决）。

### 6.3 跨 nop-entropy 改造边界声明

| 改动面 | 位置 | Q2 Phase 2 是否触碰 | 说明 |
|--------|------|----------------------|------|
| Dependency-Check / SpotBugs 插件配置 | 根 `pom.xml` 或独立 profile（应用层） | **是** | Maven 插件声明 |
| suppression / Filter 文件 | 仓库根（应用层） | **是** | 版本控制误报排除 |
| `security-baseline.md` | `docs/architecture/quality-engineering/`（应用层） | **是** | 基线文件 |
| CI workflow | `.github/workflows/` | **是** | 新建 `security.yml` 或加 job |
| 业务 Java 代码 | `module-*/erp-*-service/src/main` | **否**（修复时才触碰） | 扫描只读字节码；修复属分类后 workflow |
| ORM / model | `<domain>/model/*.orm.xml` | **否** | 零 ORM 变更 |
| nop-entropy 平台源码 | `../nop-entropy/` | **否** | 扫描依赖树 + 编译产物，不动平台 |

> 边界裁决：Q2 Phase 2 **零 nop-entropy 改动 + 零 ORM 变更**，全部在 Maven 配置 + CI workflow + 基线文件 + 可选业务代码修复（分类后）。Phase 2 无须在 `nop-entropy/ai-dev/logs/` 记日志。

### 6.4 Phase 2 执行顺序建议

1. 6.1 Dependency-Check 接入 + 首次扫描 + NVD 缓存策略落地
2. 6.2 FindSecBugs 接入 + 生成代码排除 + 首次扫描
3. §5.1 首次扫描发现分类（四态）+ suppression/Filter 落盘
4. §5.2 基线文件 `security-baseline.md` 落盘 + 机器可读块
5. §8 CI 门控接线（nightly job + 单向收紧 gate）
6. Critical/High 真实漏洞修复（分类后 workflow，可作为 Phase 2 子任务或独立 successor）

## 7. 验收判据（Phase 2 closure gate 契约）

> 每条须在 Phase 2 closure audit 时由独立子代理在 live repo 核验。每条给出具体可执行机制。

1. **Dependency-Check 接入 CI**：Maven 插件配置落盘 + nightly job 跑 `aggregate` 扫描。**可执行核验**：`rg "dependency-check-maven" pom.xml`（或 profile）命中 + `.github/workflows/` nightly job 存在。
2. **FindSecBugs 接入 CI**：SpotBugs 插件 + FindSecBugs 规则集配置落盘 + 生成代码排除（`_gen`/`api.beans`/`api.crud`）。**可执行核验**：`rg "spotbugs-maven-plugin|findsecbugs" pom.xml` 命中 + `spotbugs-exclude.xml` 含三类生成包排除。
3. **基线落盘**：`security-baseline.md` 含 CVE 基线清单 + 静态规则基线计数 + `## BASELINE (machine-readable)` 块。**可执行核验**：文件存在 + 机器可读块可解析。
4. **单向收紧门控成立**：nightly CI job 解析报告 + 比对基线块，actual > baseline => CI red。**可执行核验**：CI workflow 含 gate 逻辑（Python 比对脚本，对齐 F8 option b，双语义 §5.4）；**且**门控可证伪——注入一条 seed 超出基线的 finding（如 `cve_allowlist` 外的伪 CVE 或 `spotbugs_counts`+1），gate 须 exit 1（模拟回归证明，对齐 F8 gate 可信机制）。
5. **不阻塞 per-commit 构建**：CVE 扫描仅 nightly，per-commit `maven.yml` 不含 Dependency-Check。**可执行核验**：`maven.yml` 无 `dependency-check` step；nightly job 用 `schedule: cron` 触发。
6. **与现有 5 CI workflow 不冲突**：新建 `security.yml` 不改动既有 5 workflow。**可执行核验**：既有 5 workflow 文件 git diff 为空（`.github/workflows/{maven,compliance,e2e,mutation,clock-rollover}.yml` 无变更）；新 `security.yml` cron（如 `0 2 * * *`）与既有 nightly（`mutation.yml` `0 4 * * *` / `clock-rollover.yml` `0 3 * * *`）不重叠。
7. **全量回归绿**：`mvn clean install -DskipTests` 156 模块 BUILD SUCCESS；`mvn test` 0 failures / 0 errors（基线以 `docs/testing/known-good-baselines.md` 最近全量绿基线为准）。
8. **无双真相源**：本文档 §1 引用上游真相源（Q0 README + roadmap + compliance-baseline + roles-and-permissions），Phase 2 plan 引用本文档，不重推导证据。

## 8. CI 门控设计

> 裁决安全扫描 CI 门控形态。记录候选、考虑的替代、残留风险。

### 8.1 现状

- 现有 CI（`.github/workflows/`）：`maven.yml`（构建测试）+ `compliance.yml`（F8 + F15 + web 校验 + Q4 覆盖）+ `e2e.yml` + `mutation.yml`（nightly）+ `clock-rollover.yml`（nightly）。
- 安全扫描（Phase 2 落地后）CVE 须 nightly（§4 裁决，NVD 限速）；静态规则（FindSecBugs）不限速可 per-commit 或 nightly。

### 8.2 候选

- **C-1：新建 `.github/workflows/security.yml` 独立 nightly job**：独立 workflow 承载 Dependency-Check aggregate + FindSecBugs + 基线单向收紧 gate。残留：与既有 workflow 分离，CI 文件数 +1。
- **C-2：加 job 到 `compliance.yml`**：在 compliance workflow 增 security job（nightly 触发）。残留：compliance.yml 已承载 F8/F15/web/Q4 多 job，再加 security 臃肿；且 compliance.yml 当前是 push/PR 触发，security CVE 须 nightly，触发时机不一致。
- **C-3：加 CVE job 到 `mutation.yml`（nightly 复用）**：nightly workflow 复用。残留：mutation.yml 语义是「变异测试」，混入 CVE 扫描语义混淆。

### 8.3 裁决（Decision）

**裁决：Phase 2 采用 C-1（新建 `.github/workflows/security.yml` 独立 nightly job）作为主路径。C-2（加到 compliance.yml）否决（触发时机不一致 + 臃肿）。C-3（加到 mutation.yml）否决（语义混淆）。**

**裁决理由**：

1. **触发时机独立（nightly vs push/PR）**——CVE 扫描须 nightly（§4 NVD 限速），既有 `compliance.yml` 是 push/PR 触发，触发时机不一致。独立 `security.yml` 用 `schedule: cron` 触发，与 `mutation.yml`/`clock-rollover.yml` 的 nightly 范式一致。
2. **语义清晰**——`security.yml` 专属安全扫描，与 `compliance.yml`（架构反模式 + i18n）/ `mutation.yml`（变异测试）/ `clock-rollover.yml`（时钟）语义分离，CI 文件职责单一。
3. **对齐既有 nightly 先例**——`mutation.yml` + `clock-rollover.yml` 已建立 nightly 独立 workflow 先例，`security.yml` 与之一致。

**考虑的替代**：

- **C-2（加到 compliance.yml）**：否决——触发时机不一致（push/PR vs nightly）+ 多 job 臃肿。
- **C-3（加到 mutation.yml）**：否决——语义混淆（变异测试 ≠ 安全扫描）。
- **per-commit CVE 扫描**：否决——NVD 限速（§4 路径 B 否决理由）。

**残留风险**：

- **R8（CI 文件数 +1）**——新增 `security.yml` 使 workflow 文件数 5→6。接受（语义清晰 + 触发独立收益 > 文件数成本）。
- **R9（nightly job 资源占用）**——Dependency-Check aggregate 扫描 156 模块 + NVD 更新耗时，nightly job 可能较长。Phase 2 评估 GitHub Actions runner 资源 + NVD 缓存策略（§4.3）控制时长。

### 8.4 与现有 CI 的集成方式（Phase 2 落地）

- **主路径（C-1）**：新建 `.github/workflows/security.yml`，`on: schedule: cron`（nightly，`0 2 * * *`——与既有 nightly 不重叠：`mutation.yml` `0 4 * * *` / `clock-rollover.yml` `0 3 * * *`，核验日期 2026-08-01）+ `workflow_dispatch`（手动触发）。job：checkout → setup JDK 21 → `actions/cache` 缓存 NVD 数据库（§4.3 cache-key 策略）→ `mvn dependency-check:aggregate`（reactor-root，§6.1）→ per-module `mvn spotbugs:check`（§6.2 per-module 位置裁决）→ Python gate 脚本解析报告 + 比对 `security-baseline.md` 机器可读块（双语义 §5.4，对齐 F8 option b）→ upload 报告 artifact。
- **静态规则 per-commit（可选）**：基线稳定后，若团队要求 per-commit 静态规则反馈，可在 `maven.yml` 加 SpotBugs step（不含 Dependency-Check，NVD 限速）。Phase 2 视需求裁决。

## 9. 与 MA6 的正交边界

> 显式声明 Q2（依赖链 CVE + 静态代码安全）与 MA6（运行时 RBAC 授权）正交互补不重复。本节是 §1.3 的裁决性总结。

### 9.1 维度对照

| 维度 | Q2（本文档） | MA6（既有 done） |
|------|--------------|------------------|
| **关注点** | 依赖链已知 CVE + 静态代码安全反模式 | 运行时授权（已认证用户能做什么/能看到什么） |
| **扫描对象** | Maven 传递依赖 GAV + 编译产物字节码 | action-level 注解 + 数据权限规则 + SoD 守卫 |
| **检测时机** | 构建期/nightly（静态） | 运行时（每次请求） |
| **漏洞类型** | 已知 CVE（依赖）+ 注入/XSS/反序列化/硬编码密钥（代码） | 越权访问 / 职责未分离 / 数据越行 |
| **门控形态** | nightly 单向收紧基线 | 运行时 config-gated 守卫（部分默认 OFF） |

### 9.2 正交结论

- **不重复**：MA6 的 action-level / data-row-level / SoD 是**授权**维度——即使授权完全正确，代码仍可能有 SQL 注入（静态漏洞）或依赖含 CVE（依赖漏洞）；反之 Q2 修复所有 CVE + 静态反模式，授权仍可能越权（运行时 RBAC 漏洞）。二者检测的漏洞类型不重叠。
- **互补充**：MA6 守护「运行时授权面」，Q2 守护「依赖链 + 静态代码面」，二者合力构成「纵深防御」（Defense in Depth）。Q2 不替代 MA6，MA6 不替代 Q2。
- **边界声明**：Q2 FindSecBugs 的「硬编码密钥」规则与 MA6 的「保护区域纪律」（A6.4）有交集（密钥不应硬编码），但检测机制不同（Q2 静态扫描 vs MA6 过程纪律审计），互补非冲突。

## 10. 残留风险汇总与 successor

> 汇总 §3-§8 残留风险，登记 successor 触发条件（plan authoring guide §反松弛规则：Follow-up 须命名触发条件）。

| 风险 ID | 描述 | 分类 | successor 触发条件 |
|---------|------|------|--------------------|
| R1 | NVD API 限速 | 调度约束 | nightly aggregate + 本地缓存 + **免费 NVD API key（预期必需，§4.3）**应对；若仍超时，Phase 2 评估镜像源 |
| R2 | Dependency-Check GAV 匹配误报 | 工具约束 | §5 分类工作流 + suppression 文件吸收 |
| R3 | NVD 收录延迟（零日不覆盖） | 工具约束 | 接受（CVE 扫描定位「已知漏洞基线」非「零日防护」） |
| R4 | FindSecBugs 启发式误报 | 工具约束 | §5 分类工作流 + Filter 文件吸收 |
| R5 | Nop XPL/Dynamic Dispatch 漏报 | 工具约束 | 静态分析对动态分发不完整；动态路径归运行时测试 + Q4 故障注入 |
| R6 | 生成代码噪声 | 工具约束 | §6.2 排除 `_gen`/`api.beans`/`api.crud`（对齐 Q1 pitest 先例） |
| R7 | nightly 发现延迟（≤24h） | 调度约束 | 接受（已知漏洞基线非实时防护；紧急 CVE 人工评估） |
| R8 | CI 文件数 +1 | CI 组织 | 接受（语义清晰收益 > 文件数成本） |
| R9 | nightly job 资源占用 | CI 资源 | Phase 2 评估 NVD 缓存策略控制时长 |
| —（successor） | Q2 Phase 2 实现 plan | out-of-scope（本文档 Phase 1） | 本文档经 ≥2 轮独立审查收敛（§Review Record）+ 工具选型 + CI 调度 Decision 落定 → DRAFT_PLANS 起草 |
| —（successor） | 首次扫描发现的具体 CVE/规则违规修复 | optimization candidate | Phase 2 首次扫描 + 分类完成后，Critical/High 漏洞修复作为后续工作项 |
| —（successor） | Snyk 交叉验证 / SonarQube 仪表盘 | watch-only successor | 项目获得商业许可 + 要求更全数据库/仪表盘时 |

## Review Record

> 审查记录：MQ 文档先行工作流要求 ≥2 轮独立子代理审查（第 1 轮规范合规 + 第 2 轮覆盖面/可执行性），由不同子代理会话执行（不同 task id），审查者不可与作者为同一会话。每轮输出 BLOCKER/MAJOR/MINOR 分级意见，作者修订后重审直至收敛（无残留 BLOCKER/MAJOR）。

（Phase 2 审查循环产出后填充——见 plan `2026-08-01-1121-2` Phase 2。）

- **Round 1（规范合规审查）**: `ses_0420d11efffehJ2Iny1VpneTHy`（独立子代理 fresh session cold context）— **accept**，0 BLOCKER / 0 MAJOR / 3 MINOR。全部 live-repo 核验 PASS（零安全扫描关键字命中确认 / 5 CI workflow 精确匹配 / compliance-baseline §BASELINE 机器可读块 + 单向收紧范式确认 / MA6 RBAC action-level+SoD+data-row 表征准确 / R3 三态分类引用准确 / roadmap Q2 引用准确 / sibling docs 结构一致）。
  - R1-MINOR-1：§5.2 基线载体裁决缺显式残留风险小节（5 个 Decision 中唯独此条无残留风险）。
  - R1-MINOR-2：§1.3 / §1.4 缺 shell 验证命令（§1.1/§1.2 完全合规）。
  - R1-MINOR-3：§5.1 A/B/C/D 标签与 compliance-baseline R3 三态（A 误报 / B 合法 / C 瞬态）语义冲突（Q2 A=真实漏洞 vs R3 A=误报）。
  - 修改摘要：§5.2 补残留风险小节；§1.3 补 `SoDGuard|ErpRoleDataAuthChecker` + data-auth grep 命令；§1.4 补 `## BASELINE (machine-readable)` grep 命令；§5.1 补「A/B/C/D 标签独立定义，与 R3 三态语义不同，仅对齐多态分类范式」注。

- **Round 2（覆盖面与可执行性审查）**: `ses_0420ce45cffeegHXJAbw2brZSL`（**另一个**独立子代理，不同 task id，新会话）— **needs-revision**，0 BLOCKER / 2 MAJOR / 7 MINOR。R1 三项 MINOR 经 R2 复核均 resolved。覆盖面维度 PASS（CVE 三工具 + 静态两工具替代充分评估 / 三调度路径诚实评估 NVD 限速 / 生成代码噪声 352 类引用准确 / 5 CI 无冲突 nightly cron 不重叠已核验 / MA6 正交严谨 / 独立基线文件先例 mutation-baseline.md 证实）。
  - R2-MAJOR-1（可执行性）：基线机器可读格式 + 门控比对语义未裁决——CVE 是清单（set-based）而 SpotBugs 是计数（count-based），§5.3 用 count 语言描述 set-diff 语义，且 schema 推迟到 Phase 2，Python gate 无法推进。
  - R2-MAJOR-2（可执行性）：插件位置忽略 Q1 R4 同期发现——`mutation-baseline.md` §2 证实根 pom profile 继承失效（仅 reactor 聚合器），SpotBugs per-module 须预裁决（与 Q1 pitest 同型）。
  - R2-MINOR-1：NVD API key 在 2024+ 限速下近乎必需，§4.3 不应框为「可选」。
  - R2-MINOR-2：Java 21 / 工具链版本兼容性未核验（CI 全域 setup-java 21）。
  - R2-MINOR-3：§4.3 NVD cache 缺 GitHub Actions cache-key 策略 + cache 预算核算。
  - R2-MINOR-4：验收判据 4 仅验证脚本存在未验证可证伪。
  - R2-MINOR-5：验收判据 6「语义不变」不可机械核验。
  - R2-MINOR-6：§6.2 includeFilterFile（启用规则）vs excludeFilterFile（抑制匹配）机制命名混淆。
  - R2-MINOR-7：NVD 数据库体积估算陈旧（~数百 MB → ~1-2 GB）。
  - 修改摘要：
    - **R2-MAJOR-1 修正**：新增 §5.4「基线格式与门控比对语义裁决」——CVE 用 set-allowlist（`(cveId, gav)` 元组集合做 set-diff，防置换回归漏看）+ SpotBugs 用 per-bug-type count（对齐 F8 per-rule 计数，容忍行号漂移），双语义经同一 Python gate 两段处理；给出精确 `## BASELINE` YAML schema（`cve_allowlist:` 序列 + `spotbugs_counts:` 映射）+ Phase 2 gate 实现指引（解析 dependency-check-report.json + spotbugsXml.xml）。
    - **R2-MAJOR-2 修正**：§6.1 补插件位置裁决（Dependency-Check aggregate 是 reactor-root goal，Q1 R4 不适用，Phase 2 核验 aggregate 在 reactor-root 生效）；§6.2 补插件位置裁决（SpotBugs per-module，对齐 Q1 R4 根 pom 继承失效先例，或经 pluginManagement 集中版本 + per-module 启用）。
    - **R2-MINOR-1 修正**：§4.3 NVD API key 改为「预期必需」（2024+ 无 key 限速严苛，156 模块首跑无 key 大概率超时），R1 残留风险同步。
    - **R2-MINOR-2 修正**：§6.2 step 7 补 Java 21 兼容性核验（SpotBugs 4.8+ 支持 Java 21，FindSecBugs 版本矩阵 Phase 2 首跑验证）；新增 R6.1 残留风险。
    - **R2-MINOR-3 修正**：§4.3 补 cache-key 策略（日期 key + restore-keys 前缀）+ cache 预算核算（10 GB/repo + 竞争者）。
    - **R2-MINOR-4 修正**：验收判据 4 强化（注入 seed finding 超 baseline，gate 须 exit 1，模拟回归证明）。
    - **R2-MINOR-5 修正**：验收判据 6 强化（既有 5 workflow git diff 为空 + cron 不重叠已核验 `0 2` vs `0 3`/`0 4`）。
    - **R2-MINOR-6 修正**：§6.2 明确 `includeFilterFile`（启用 FindSecBugs 规则）vs `excludeFilterFile`（抑制生成代码匹配）两机制。
    - **R2-MINOR-7 修正**：§4.3 NVD 体积改 ~1-2 GB。

**收敛结论**：2 轮审查后无残留 BLOCKER / 无残留 MAJOR（R1 的 3 MINOR + R2 的 2 MAJOR + 7 MINOR 全部修订；R2 实仓复核确认工具选型替代充分 + NVD 限速诚实评估 + 生成代码噪声引用准确 + 5 CI 无冲突 + 独立基线文件先例 + nop-entropy 边界零改动 + MA6 正交）。文档可作为 Phase 2 实现 plan 的实施契约。MINOR 不阻塞收敛。

<!-- 审查者多样性已满足：R1（ses_0420d11e...）/ R2（ses_0420ce45...）两会话 task id 不同，均独立 fresh cold context，未复用作者上下文。 -->

## 11. Phase 2 实施期发现回填（plan `2026-08-02-1121-1`，2026-08-02）

> plan Closure Gates「实现与设计文档一致」要求：任何实施期发现回填设计文档 Review Record 而非静默偏离。

- **includeFilterFile 机制澄清（对 §6.2 step 1 R2-MINOR-6 修正的精确化）**：§6.2 称 includeFilterFile「启用 FindSecBugs 安全规则」。Phase 2 实测（`mvn -Psecurity spotbugs:check -X`）发现 FindSecBugs 探测器经 `findsecbugs-plugin` JAR 依赖**自动注册**到 spotbugs classpath（debug 输出 `Adding to pluginArtifact -> com.h3xstream.findsecbugs:findsecbugs-plugin:jar:1.13.0`），无需 includeFilterFile 即运行。includeFilterFile 的真实作用是 **scope 报告到 FindSecBugs 安全 BugPattern**（从 `findsecbugs-plugin-1.13.0.jar!/findbugs.xml` 程序化提取 139 type 生成 `spotbugs-include.xml`），避免安全基线被 SpotBugs 通用代码质量规则（PERFORMANCE/STYLE/BAD_PRACTICE）稀释。此为机制精确化，非范围偏离——§6.2 的设计意图（安全基线聚焦 FindSecBugs 规则）不变，仅实现机制描述更精确。
- **首跑实测结果（§1 现状的 Phase 2 验证）**：19 域 erp-*-service FindSecBugs 首跑**全部 0 命中**（spotbugs-maven-plugin 4.9.8.3 + spotbugs 4.9.8 + findsecbugs 1.13.0 + Java 26 本地首跑 BUILD SUCCESS）。ERP 业务代码经 Nop ORM 参数化查询 + 平台安全包装，无静态可检测注入/XSS/反序列化/硬编码密钥反模式。`spotbugs_counts` 基线为空（最严格：任何 FindSecBugs 命中 ⇒ gate fail）。
- **NVD API key infra prereq 确认**：§4.3 裁决的「NVD API key 预期必需」在 Phase 2 实仓确认——本地无 key 全量首跑不可行（156 模块 aggregate + NVD ~1-2GB 下载，无 key 限速严苛）。CI nightly（`.github/workflows/security.yml`）经 `NVD_API_KEY` secret 注入执行真实首跑；`cve_allowlist` 基线为空至首跑分类完成（§5.4 Empty list on first run）。
- **插件位置裁决 R2-MAJOR-2 Phase 2 复核**：SpotBugs per-module（候选 b）经 R4 同型复核确认——19 域 service pom 各声明 `<profile id="security">`，`mvn help:active-profiles -pl <module>`（-Psecurity）security active。根 pom profile 不继承到子模块（module-*/pom.xml `<parent>` 是 nop-entropy）。Dependency-Check aggregate（§6.1）经 reactor-root 核验生效（`mvn -Psecurity -N help:effective-pom` 含 dependency-check-maven + aggregate=true）。
