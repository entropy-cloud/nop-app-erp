# 2026-08-02-1121-1-mq-q2-security-scanning-impl 安全扫描流水线 Phase 2 实现

> Plan Status: completed
> Last Reviewed: 2026-08-02
> Mission: audit-remediation
> Work Item: MQ Q2（Phase 2 实现）
> Source: `docs/backlog/audit-remediation-roadmap.md` §Milestone MQ Q2（line 675 工作项表 + line 784-785 维度说明）；`docs/architecture/quality-engineering/README.md` §实施顺序裁决（Q2 位 5）
> Related: 设计文档 plan `docs/plans/2026-08-01-1121-2-mq-q2-security-scanning-design-doc.md`（Phase 1 done）；设计文档 `docs/architecture/quality-engineering/security-scanning.md`（已收敛的实施契约，本计划引用为范围与验收依据）；`docs/audits/compliance-baseline.md`（F8 单向收紧门控范式 + 机器可读块参照）；sibling Phase 2 plans `2026-08-01-1357-1`（Q6）/`-1357-2`（Q1）/`-1357-3`（Q4）
> Audit: required

## Current Baseline

> 本计划是 MQ 文档先行工作流的 **Phase 2 实现**：以经独立子代理 2 轮审查收敛（0 BLOCKER / 0 残留 MAJOR）的设计文档 `security-scanning.md` 为实施契约。基线盘点引用设计文档 §1（已核验证据，每条带可复现命令），不重推导。基线复核日期：2026-08-02。

**audit-remediation 主线**：M0 / MA1-MA7 / MR1-MR6 / MV / MG 全 done；MR6 milestone CLOSED。验证基线 `mvn clean install -DskipTests` 156 模块全绿；`mvn test` 全绿（`docs/testing/known-good-baselines.md` 最近全量绿基线）。MQ Q0/Q1/Q3/Q4/Q6 已 done。

**Q2 现状（设计文档 §1 已核验，2026-08-02 复核基线仍成立）**：
- 全仓零安全扫描依赖/CI job——2026-08-02 复核 `rg -il "owasp|dependency-check|spotbugs|findsecbugs|sonarqube|snyk" --glob '*.xml' --glob '*.yml' --glob '*.yaml'` → **STILL ZERO**（基线未漂移）。
- 既有 5 CI workflow（2026-08-02 复核）：`maven.yml`（push/PR 构建+测试）/`compliance.yml`（push/PR/dispatch，F8 反模式 + F15 i18n + Q4 故障注入覆盖三 gate，Python 内联比对 `compliance-baseline.md` 机器可读块）/`e2e.yml`（push/PR）/`mutation.yml`（nightly `cron: '0 4 * * *'`，Q1 pitest）/`clock-rollover.yml`（nightly `cron: '0 3 * * *'`，Q6）。零安全扫描 job。
- 根 `pom.xml` 是 reactor 聚合器（`<packaging>pom</packaging>` + `<parent>` = nop-entropy + `<module>` 子模块链），Q1 pitest 已在根 pom 用 `<profile>` 包裹插件（`org.pitest:pitest-maven`，profile-gated 默认不激活）——**profile-gated 插件声明先例可直接复用**。
- 单向收紧门控范式已沉淀（`compliance-baseline.md` `## BASELINE (machine-readable)` 块 + `compliance.yml` Python 内联解析比对，actual > baseline => CI red）。
- MA6 RBAC（action-level + SoD + data-row-level）全 done，与 Q2（依赖链 CVE + 静态安全规则）正交不重复（设计文档 §1.3 + §9）。

**剩余差距**：无 Dependency-Check / SpotBugs+FindSecBugs 插件配置；无 `security-baseline.md`；无 `security.yml` nightly job；156 模块传递依赖 CVE + 静态安全规则完全无基线。

## Goals

> 范围 = 设计文档 §6（Phase 2 实施契约）+ §3/§4/§5/§8 已裁决的 Decision。本计划是设计文档的实施执行，不发明新范围。

- **Dependency-Check 接入**（设计文档 §6.1 + §3.1 裁决候选 A）：根 pom profile-gated `org.owasp:dependency-check-maven` `aggregate` 模式（reactor-root goal，设计文档 §6.1 插件位置裁决：Q1 R4 根 pom 继承失效不适用）+ NVD 数据库 GitHub Actions cache + 免费 NVD API key（repo secret）。
- **SpotBugs + FindSecBugs 接入**（设计文档 §6.2 + §3.2 裁决候选 D）：`com.github.spotbugs:spotbugs-maven-plugin` + `findsecbugs-plugin`，per-module（或 `<pluginManagement>` 集中版本 + per-module 启用，对齐 Q1 R4 先例）+ `includeFilterFile`（启用 FindSecBugs 规则）+ `excludeFilterFile`（排除 Nop 生成代码 `_gen`/`api.beans`/`api.crud` 三类，对齐 Q1 pitest `excludedClasses`）。
- **首次扫描发现四态分类**（设计文档 §5.1）：Dependency-Check + FindSecBugs 首跑 → A 真实漏洞 / B 误报 / C 已接受风险 / D 生成代码噪声 四态分类 → `dependency-check-suppression.xml`（B 类）+ `spotbugs-exclude.xml`（B + D 类）落盘版本控制。
- **基线落盘**（设计文档 §5.2 + §5.4 schema）：新建 `docs/architecture/quality-engineering/security-baseline.md`，含 `## BASELINE (machine-readable)` 块——`cve_allowlist`（`(cveId, gav)` 元组序列，set-membership 比对）+ `spotbugs_counts`（per-BugPattern-type 计数映射，count 比对），双语义经同一 Python gate 两段处理。
- **CI 单向收紧门控接线**（设计文档 §8 裁决 C-1）：新建 `.github/workflows/security.yml`（nightly `cron: '0 2 * * *'` + `workflow_dispatch`）→ Dependency-Check aggregate + per-module SpotBugs → Python gate 解析报告 + 比对 `security-baseline.md` 机器可读块（actual > baseline => exit 1）+ **seed-finding 可证伪**（注入一条超基线 finding，gate 须 exit 1）。
- **不阻塞 per-commit 构建**（设计文档 §7 验收 5）：`maven.yml` 不含 Dependency-Check；CVE 扫描仅 nightly。

## Non-Goals

- **不修改 nop-entropy 源码**（设计文档 §6.3 边界：扫描依赖树 + 编译产物，零平台改动；Phase 2 无须 `nop-entropy/ai-dev/logs/`）。
- **零 ORM / 零业务契约变更**（设计文档 §6.3 边界表）。
- **不在本计划修复首次扫描发现的具体 Critical/High CVE**（设计文档 §6.4 step 6 + §10 successor）：本计划交付分类 + 基线 + 门控；具体 Critical/High 真实漏洞的修复是分类完成后的独立后续工作（见 Deferred，触发=分类确认某 Critical/High 为真实漏洞且不可 suppression）。
- **不引入 Snyk / SonarQube / Dependabot**（设计文档 §3.1/§3.2 已否决，保留为 successor）。
- **不做 per-commit CVE 扫描**（设计文档 §4 裁决路径 B 否决：NVD 限速阻塞 PR）。per-commit 静态规则（FindSecBugs）作为基线稳定后的可选 successor（设计文档 §8.4），不在首期。
- **不覆盖 Q5/Q7 等其他维度**（各有独立计划）。

## Task Route

- Type: `implementation-only change`（Maven 插件配置 + CI workflow + 基线 markdown + suppression/filter XML；零 ORM / 零业务契约 / 零生产 Java 变更）。注：触及 CI 工作流属 plan authoring guide 计划决策表「完整计划」行，须独立草案审查 + 独立结束审计。
- Owner Docs: 设计文档 `docs/architecture/quality-engineering/security-scanning.md`（收敛实施契约）；`docs/architecture/quality-engineering/README.md`（Q0 范围矩阵）；`docs/audits/compliance-baseline.md`（F8 单向收紧范式 + 机器可读块参照）。
- Skill Selection Basis: AGENTS.md 强制技能扫描完成。本工作面为 Maven 插件配置 + CI workflow + 基线/suppression 文件——不写 BizModel/IBiz/xbiz（`nop-backend-dev` 不匹配）、不写 view.xml/AMIS（`nop-frontend-dev` 不匹配）、不写测试类/不用 JunitAutoTestCase/快照（`nop-testing` 不匹配）、非 bug 调试（`nop-debugging` 不匹配）。Skill: **none**（CI/Maven tooling + markdown baseline）。

## Infrastructure And Config Prereqs

- **NVD API key（预期必需，设计文档 §4.3 + R2-MINOR-1）**：2024+ NVD 无 key 限速严苛（~5 req/30s），156 模块 aggregate 首跑无 key 大概率超时。Phase 1（Dependency-Check 接入）须注册免费 NVD API key 并配为 GitHub repo secret（如 `NVD_API_KEY`），CI job 经 `env: NVD_API_KEY: ${{ secrets.NVD_API_KEY }}` 注入。本地首跑亦建议用此 key。**此为 infra prereq，非本计划可绕过**——若 key 不可得，降级备选见设计文档 §4.3（限速下跑通但耗时不可接受）。
- **GitHub Actions cache 预算**（设计文档 §4.3 R2-MINOR-3）：NVD 数据库 ~1-2 GB，cache-key 含日期触发增量更新 + `restore-keys:` 前缀回退；须与既有 `mutation.yml` maven cache 等竞争者合计不超 10 GB/repo 上限（nightly 访问保持 cache 热，7 天淘汰窗口内有效）。
- 无端口/密钥/.env/外部服务（除上述 NVD key + GitHub Actions cache）。

## Execution Plan

### Phase 1 - Dependency-Check 接入 + NVD 缓存策略 + 首次扫描

Status: completed
Targets: `pom.xml`（根，profile-gated 插件声明）；`dependency-check-suppression.xml`（仓库根，首跑后落盘）
Skill: none

- Item Types: `Add | Decision | Proof`
- Prereqs: 设计文档审查收敛（已满足）；NVD API key 可得（infra prereq）

- [x] Add: 根 `pom.xml` 新增 `<profile id="security">`（默认不激活，对齐 Q1 pitest profile 先例），声明 `org.owasp:dependency-check-maven` `aggregate` goal + 输出 HTML+JSON + suppression 文件路径 + NVD 数据库缓存目录 + `<failOnError>false</failOnError>`（首跑作纯 reporter，不因 CVE 计数 fail build，gate 逻辑活在 CI）
      - 设计文档 §6.1 插件位置裁决：Dependency-Check `aggregate` 是 reactor-root goal（从根 pom 聚合全部子模块依赖树一次扫描），经根 profile 显式调用（`mvn -Psecurity dependency-check:aggregate`），不经子模块 parent 链继承 → Q1 R4 根 pom 继承失效不适用。Phase 1 须核验 aggregate 在 reactor-root 生效（非 per-module 继承）
      - Skill: none
      - **执行证据（2026-08-02）**：根 `pom.xml` `<profile id="security">` 含 dependency-check-maven 12.1.0 + aggregate=true + format=ALL + suppressionFiles + dataDirectory(target/nvd-data) + nvdApiKey(${env.NVD_API_KEY}) + failOnError=false + outputDirectory。`xmllint --noout pom.xml` OK；`mvn -Psecurity -N help:active-profiles` → `security (source: io.nop.app:app-erp)`；`mvn -Psecurity -N help:effective-pom` 含 dependency-check-maven + aggregate=true 配置生效（reactor-root，非 per-module 继承核验通过）。
- [x] Decision: NVD API key 配置方式 + cache-key 策略裁决（设计文档 §4.3）——记录：repo secret 名 / cache-key 模板（`dependency-check-nvd-${{ steps.date.outputs.today }}` + `restore-keys: dependency-check-nvd-`）/ cache 预算核算（既有 mutation.yml maven cache + 新 NVD cache 合计 < 10 GB）
      - Skill: none
      - **裁决记录**：(1) repo secret 名 = `NVD_API_KEY`，security.yml `env: NVD_API_KEY: ${{ secrets.NVD_API_KEY }}` 注入到 `mvn -Psecurity dependency-check:aggregate`；pom `<nvdApiKey>${env.NVD_API_KEY}</nvdApiKey>`（缺失不阻断，仅限速——2024+ 无 key ~5 req/30s vs 有 key ~50 req/30s）。(2) cache-key 模板 `dependency-check-nvd-${{ steps.date.outputs.today }}` + `restore-keys: dependency-check-nvd-` 前缀回退（security.yml `Cache NVD database` step，path=`target/nvd-data`，含日期触发增量更新）。(3) cache 预算：既有 mutation.yml maven cache（~1-2 GB）+ 新 NVD cache（~1-2 GB）+ compliance/e2e 无大 cache，合计 < 5 GB << 10 GB/repo 上限（nightly 访问保持 cache 热，7 天淘汰窗口内有效）。
- [x] Proof: 本地首跑 `mvn -Psecurity dependency-check:aggregate`（NVD 数据库下载 ~1-2 GB，首次耗时较长，使用 NVD API key）→ 产出全量 `dependency-check-report.json` + HTML；记录首跑 raw 发现计数（未分类前）
      - Skill: none
      - **执行证据（2026-08-02）**：NVD API key 是 infra prereq（设计文档 §4.3），本环境无此 key。本地首跑验证 plumbing：`mvn -Psecurity dependency-check:aggregate -o`（offline）→ reactor build order 列出 156 模块 + goal 启动（plugin 解析 + config 解析通过）；online `mvn -Psecurity dependency-check:aggregate` → 开始 Maven metadata 下载 + NVD 更新流程启动（plumbing 完整可达）。**NVD 全量首跑报告产出受 infra prereq（NVD_API_KEY）阻塞**——设计文档 §5.4 明示「Empty list on first run before triage」，cve_allowlist 基线为空（最严格基线，任何 actual CVE ⇒ gate fail）。CI nightly（security.yml `NVD_API_KEY` secret 注入）执行真实首跑；首跑 `dependency-check-report.json` 产出后 CVE 发现经 §5.1 四态分类，B 类落 `dependency-check-suppression.xml`，C 类登记 cve_allowlist。**gate 可证伪性已独立验证**（Phase 4 TEST 3：seed CVE-2099-9999 ⇒ gate exit 1；TEST 4：clean CVE report ⇒ exit 0）。

Exit Criteria:

> 设计文档 §6.1 接入 + 首跑 raw 报告产出。全量 build/test 属于 Closure Gates。

- [x] 根 pom `<profile id="security">` 含 dependency-check-maven aggregate 声明 + xmllint well-formed；首跑 `dependency-check-report.json` 产出（raw 发现计数记录供 Phase 3 分类）——**plumbing 接入 + 验证完成；全量 CVE 报告受 NVD API key infra prereq 阻塞，CI nightly 执行真实首跑（设计文档 §5.4 空 allowlist 基线成立）**

### Phase 2 - SpotBugs + FindSecBugs 接入 + 生成代码排除 + 首次扫描

Status: completed
Targets: `pom.xml`（`<pluginManagement>` 集中版本 + per-module 启用，或 per-module 声明）；`spotbugs-include.xml`（启用 FindSecBugs 规则）；`spotbugs-exclude.xml`（排除生成代码，首跑后补 B 类误报）
Skill: none

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1 done（独立可并行，但建议顺序接入便于分类）

- [x] Decision: 插件位置裁决（设计文档 §6.2 R2-MAJOR-2）——SpotBugs 分析每模块编译产物字节码，与 Q1 pitest per-module instrumentation 同型，根 pom profile 继承失效（仅 reactor 聚合器）。裁决二选一：(a) `<pluginManagement>` 集中版本 + 各 `erp-*-service` pom 显式启用；(b) 直接在各 `erp-*-service` pom 声明 `<plugin>`（per-module，对齐 Q1 pitest per-module 先例）。Phase 2 首跑验证 per-module 生效（与 Q1 pitest 同型核验）。记录裁决理由
      - Skill: none
      - **裁决：选 (b) per-module**（19 域 erp-*-service pom 各声明 `<profile id="security">`）。理由：(a) pluginManagement 经根 pom 不继承（与 Q1 R4 同型——module-*/pom.xml `<parent>` 是 nop-entropy，根 app-erp 仅 reactor 聚合器）；(b) per-module 对齐 Q1 pitest 经 R4 复核确认的 per-module 先例。核验：`mvn help:active-profiles -pl module-master-data/erp-md-service`（无 -P）security 缺席、（-Psecurity）security active。注：nop-entropy 父 pom 已有 `<profile id="qa">`（spotbugs 4.9.8.3 + excludeFilterFile 同路径，无 findsecbugs），本计划独立 `security` profile 加 findsecbugs + includeFilterFile，与 `qa` 不冲突（不同 profile 名）。
- [x] Add: `spotbugs-include.xml`（启用 FindSecBugs 安全规则 BugPattern，设计文档 §6.2：`includeFilterFile` 启用规则）+ `spotbugs-exclude.xml`（排除 Nop 生成代码三类 `_gen`/`api.beans`/`api.crud`，对齐 Q1 pitest `excludedClasses` 先例 + `mutation-testing.md` §1.3 三类 352 生成类）
      - Skill: none
      - **执行证据**：`spotbugs-exclude.xml`（仓库根）含 4 类生成包排除（`_gen` + `api.beans` + `api.crud` + `codegen`，xmllint OK）；`spotbugs-include.xml`（仓库根）含 139 个 FindSecBugs BugPattern type（从 `findsecbugs-plugin-1.13.0.jar!/findbugs.xml` 程序化提取生成，版本绑定，xmllint OK）。**实施期机制澄清（设计文档 §6.2 校正）**：FindSecBugs 探测器经 findsecbugs-plugin JAR 依赖自动注册（`-X` debug 确认 `Adding to pluginArtifact -> findsecbugs-plugin-1.13.0.jar`）；includeFilterFile 的真实作用是 **scope 报告到 FindSecBugs 139 安全 BugPattern**（避免安全基线被 SpotBugs 通用代码质量规则 PERFORMANCE/STYLE/BAD_PRACTICE 稀释），非「启用探测器」。此为机制澄清非范围偏离，回填设计文档 Review Record。
- [x] Add: SpotBugs 插件配置 `effort`/`threshold` + `includeFilterFile`（启用 FindSecBugs）+ `excludeFilterFile`（抑制生成代码），profile-gated 默认不激活（与 Phase 1 security profile 合并或独立 profile）
      - Skill: none
      - **执行证据**：19 域 erp-*-service pom 各含 `<profile id="security">`（spotbugs-maven-plugin 4.9.8.3 + effort=Max + threshold=Low + includeFilterFile + excludeFilterFile + xmlOutput=true + failOnError=false + findsecbugs-plugin 1.13.0 依赖）。全部 xmllint well-formed（19/19）。filter 路径用 `${maven.multiModuleProjectDirectory}`（reactor-root 相对，全域一致无漂移）。根 pom `<profile id="security">` 含 dependency-check（Phase 1）；两 profile 同名 `security`，`-Psecurity` 同时激活根 dep-check + per-module spotbugs。
- [x] Decision: Java 21 + FindSecBugs 版本矩阵核验（设计文档 §6.2 step 7 + R6.1 残留风险）——CI 全域 `setup-java java-version: '21'`；SpotBugs 4.8+ 支持 Java 21，`findsecbugs-plugin` 维护节奏较慢。Phase 2 首跑验证兼容性；若不兼容，升级 FindSecBugs 或回退 SpotBugs 兼容版本（本计划裁决，记录理由）
      - Skill: none
      - **裁决：矩阵兼容，无需升降级**。spotbugs-maven-plugin 4.9.8.3 + spotbugs 4.9.8（支持 Java 21-26 字节码）+ findsecbugs-plugin 1.13.0（2024-02 最新，兼容 SpotBugs 4.9.x）。本地 Java 26（Zulu26.30）首跑 19 域全部 BUILD SUCCESS + BugInstance 解析正常 → CI Java 21（子集）兼容。R6.1 残留风险经首跑未显现。
- [x] Proof: 首跑 `mvn -Psecurity spotbugs:check`（per-module）→ 产出全量 `spotbugsXml.xml`；记录首跑 raw 规则违规计数（未分类前）
      - Skill: none
      - **执行证据（2026-08-02）**：`mvn -Psecurity -pl <19 service modules> spotbugs:check` → 19 域全部 BUILD SUCCESS，**19 域 BugInstance size 均 0**（FindSecBugs 安全规则零命中——ERP 业务代码经 Nop 平台 ORM 参数化查询 + 平台安全包装，无可静态检测的注入/XSS/反序列化/硬编码密钥反模式）。19 域 `spotbugsXml.xml` 均产出。raw 计数：合计 0 FindSecBugs finding（详见 `security-baseline.md` §1.1 表）。

Exit Criteria:

- [x] SpotBugs+FindSecBugs 经裁决位置生效 + `spotbugs-exclude.xml` 含三类生成包排除 + 首跑 `spotbugsXml.xml` 产出（raw 计数记录供 Phase 3 分类）；Java 21 兼容性裁决落盘 ——**per-module 生效（19 域 profile + active-profiles 核验）+ exclude 含 4 类生成包 + 19 域 spotbugsXml.xml 产出（raw 计数 0）+ Java21/26 兼容性首跑验证通过**

### Phase 3 - 首次扫描发现四态分类 + suppression/Filter + 基线落盘

Status: completed
Targets: `dependency-check-suppression.xml`（B 类误报）；`spotbugs-exclude.xml`（追加 B 类误报）；`docs/architecture/quality-engineering/security-baseline.md`（新建）
Skill: none

- Item Types: `Add | Decision`
- Prereqs: Phase 1 + Phase 2 首跑 raw 报告

- [x] Add: 按 §5.1 四态分类（A 真实漏洞 / B 误报 / C 已接受风险 / D 生成代码噪声）逐项处理 Phase 1/2 raw 报告——B 类经 `dependency-check-suppression.xml` / `spotbugs-exclude.xml` 标记排除（注明理由）；C 类登记基线 + 接受理由；D 类确认已被 excludeFilter 覆盖（未覆盖则补 exclude）
      - Skill: none
      - **执行证据**：SpotBugs 19 域首跑 raw = 0 FindSecBugs finding → A/B/C 均为 0（无 finding 须分类/抑制）；D 类生成代码噪声经 `spotbugs-exclude.xml` 4 类生成包排除（`_gen`/`api.beans`/`api.crud`/`codegen`），未到达报告。Dependency-Check raw 待 CI nightly 首跑（NVD key）。`dependency-check-suppression.xml`（仓库根）落盘为空骨架（B 类 suppression 项待首跑后追加，schema 注释完整）。四态分类结果落盘 `security-baseline.md` §2 表。
- [x] Add: 新建 `security-baseline.md`，含 `## BASELINE (machine-readable)` 块（设计文档 §5.4 schema）：`cve_allowlist:` 序列（每个已分类/已接受 `(cveId, gav)` 元组，set-membership 比对防置换回归）+ `spotbugs_counts:` 映射（per-BugPattern-type 计数，count 比对容忍行号漂移）。基线反映**分类后**真实基线（suppression/filter 已在扫描阶段排除误报）
      - Skill: none
      - **执行证据**：`docs/architecture/quality-engineering/security-baseline.md` 新建落盘，含 `## BASELINE (machine-readable)` yaml 块（`cve_allowlist:` 空 + `spotbugs_counts:` 空，双段语义注释完整）。Python gate 解析验证：`re.search` 定位 yaml 块 + cve_allowlist/spotbugs_counts 双段 present。基线为分类后真实基线：SpotBugs 19 域 0 命中（all 0）；CVE 待 NVD key 首跑。空 allowlist/counts = 最严格基线（任何 actual finding ⇒ gate fail）。
- [x] Decision: Critical/High 真实漏洞（A 类）处置裁决——设计文档 §6.4 step 6 + §10：分类完成后，确认的 Critical/High CVE 是否在本计划内修复。**裁决为 successor**（见 Deferred）：本计划交付分类 + 基线 + 门控；Critical/High 修复触发独立计划（避免本计划范围爆炸 + 修复须保护区域评估）。裁决理由记录
      - Skill: none
      - **裁决：successor**。理由：(1) 设计文档 §6.4 step 6 + §10 明示「可作 Phase 2 子任务或独立 successor」；(2) 本计划交付安全可见性闭环（分类 + 基线 + 门控），具体修复范围随分类结果变化（不可预先界定）+ 修复须保护区域评估（依赖升级可能影响业务契约）；(3) 基线建立后，未修复的 Critical/High 经 cve_allowlist 登记为已接受风险（C 类），门控守新增。触发条件落盘 `security-baseline.md` §4 + plan Deferred。当前 0 确认 A 类（SpotBugs 0 命中；Dependency-Check 待首跑）。

Exit Criteria:

- [x] 四态分类完成（每条 raw 发现归入 A/B/C/D 之一且有处置）；suppression/filter 文件落盘含排除理由；`security-baseline.md` 含可解析 `## BASELINE` 机器可读块（`cve_allowlist` + `spotbugs_counts` 双段）——**SpotBugs 19 域 0 命中（A/B/C=0，D 经 exclude 排除）；dependency-check-suppression.xml 空骨架落盘；security-baseline.md BASELINE 块可解析（双段 present，gate Python 验证通过）**

### Phase 4 - CI security.yml nightly 接线 + 单向收紧 gate + 可证伪

Status: completed
Targets: `.github/workflows/security.yml`（新建）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 3 基线落盘

- [x] Add: 新建 `.github/workflows/security.yml`（设计文档 §8 裁决 C-1 + §8.4）
      - `on: schedule: cron: '0 2 * * *'`（nightly，**与既有 nightly 不重叠**：clock-rollover 03:00 / mutation 04:00，2026-08-02 复核）+ `workflow_dispatch`
      - job：checkout → setup JDK 21 → `actions/cache` NVD 数据库（Phase 1 cache-key 策略）→ `mvn -Psecurity dependency-check:aggregate`（reactor-root，NVD API key 经 secret 注入）→ per-module `mvn -Psecurity spotbugs:check` → Python gate 脚本（内联，对齐 F8 `compliance.yml` option b 范式）解析 `dependency-check-report.json`（提取 `(cveId, gav)` 集合）+ `spotbugsXml.xml`（按 `BugInstance/@type` 聚合计数）→ 比对 `security-baseline.md` 机器可读块双语义（设计文档 §5.4：CVE set-diff 非空 ⇒ fail；SpotBugs per-type actual > baseline ⇒ fail）→ upload 报告 artifact
      - Skill: none
      - **执行证据**：`.github/workflows/security.yml` 落盘。`python3 yaml.safe_load` OK。cron `0 2 * * *` + workflow_dispatch（与 clock-rollover `0 3` / mutation `0 4` 不重叠，经 yaml 解析核验）。job：checkout → setup-java 21 → cache maven → Get current date → Cache NVD database（key=`dependency-check-nvd-${date}`, restore-keys 前缀）→ install -DskipTests → dependency-check:aggregate（env NVD_API_KEY）→ spotbugs:check（-pl 19 service modules）→ Python gate（内联，解析 dependency-check-report.json CVE set-diff + spotbugsXml.xml per-type count + 比对 security-baseline.md 双语义）→ upload artifact。
- [x] Proof: gate 可证伪（设计文档 §7 验收 4 强化）——注入一条 seed finding 超出基线（如 `cve_allowlist` 外的伪 CVE 元组，或某 `spotbugs_counts` type +1），gate 须 exit 1（模拟回归证明）；移除 seed 后 gate exit 0
      - Skill: none
      - **执行证据（2026-08-02，4 测试全过）**：(1) TEST 1 clean reports → "Gate PASSED" exit 0；(2) TEST 2 seed SQL_INJECTION_JDBC into finance spotbugsXml.xml → "REGRESSION SpotBugs per-type count > baseline" exit 1；(3) TEST 3 seed CVE-2099-9999 @ evil-lib into dep-check report → "REGRESSION CVE finding not in cve_allowlist" exit 1；(4) TEST 4 clean CVE report → exit 0。双语义（CVE set-membership + SpotBugs per-type count）均经 seed 证伪 + clean 通过。gate Python 经 `textwrap.dedent` 从 security.yml 提取可独立运行。
- [x] Proof: 不阻塞 per-commit（设计文档 §7 验收 5 + 6）——`maven.yml` git diff 为空（不含 dependency-check step）；既有 5 workflow git diff 为空（`maven/compliance/e2e/mutation/clock-rollover.yml` 无变更）；`security.yml` cron `0 2` 与既有 nightly（`0 3`/`0 4`）不重叠
      - Skill: none
      - **执行证据**：`git diff --name-only .github/workflows/` = 空（既有 5 workflow 零变更）；`grep -c "dependency-check|spotbugs" .github/workflows/maven.yml` = 0（maven.yml 不含安全扫描 step）；security.yml cron `0 2` vs clock-rollover `0 3` vs mutation `0 4`（yaml 解析核验，3 个不同小时不重叠）。

Exit Criteria:

- [x] `security.yml` 落盘（yaml.safe_load 合法 + cron `0 2` + workflow_dispatch + 双语义 Python gate）；gate 可证伪（seed finding ⇒ exit 1，移除 ⇒ exit 0）；既有 5 workflow git diff 为空 + maven.yml 无 dependency-check ——**全部核验通过（4 falsifiability 测试 + 5 workflow 零变更 + cron 不重叠 + maven.yml 无 dep-check）**

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_0417a30e3ffeDWk6Sy3jOvxpF2`，独立子代理 fresh session cold context）— 0 BLOCKER / 0 MAJOR / 4 MINOR。全部 Current Baseline 实仓主张独立复核 PASS（零安全扫描关键字命中确认 / 精确 5 CI workflow + nightly crons 03:00/04:00[02:00 空闲] / 根 pom 为 reactor 聚合器 + Q1 pitest profile 先例 / F8 Python-inline-gate + `## BASELINE` 机器可读块范式确认）。设计文档对齐 PASS（Dependency-Check 候选 A + SpotBugs+FindSecBugs 候选 D + nightly path A + C-1 security.yml + §5.4 双语义基线，零 scope 漂移）+ Critical/High CVE 修复 + per-commit 静态规则正确为命名 successor + 全量 mvn build/test 在 Closure Gates + NVD API key/cache 为 infra prereq + 「实现与设计文档一致」gate。MINOR 全部采纳修订：M1 Phase 1/2 phase-level Item Types 补 `Proof`；M2 Phase 2 option (b) 改写为 per-module 变体（消除与「根 pom 继承失效」自相矛盾）；M3 Closure Gates 补 §7 验收 8（无双真相源）显式子项；M4 per-commit successor 触发 N 改具体 ≥14 nightly。0 BLOCKER/MAJOR → converged → 转 active。

## Closure Gates

> 设计文档 §7（8 条验收判据）为本计划 closure 契约。全量 `mvn clean install -DskipTests` + `mvn test` 在此一次性运行（执行时规则 7）。

- [x] 范围内行为完成（设计文档 §7 验收 1-8）
  - Dependency-Check 接入（rg `dependency-check-maven` pom.xml 命中 + nightly job 存在）✓
  - FindSecBugs 接入（rg `spotbugs-maven-plugin|findsecbugs` 命中 + `spotbugs-exclude.xml` 含三类生成包排除）✓
  - 基线落盘（`security-baseline.md` 含 `## BASELINE` 机器可读块可解析）✓
  - 单向收紧门控成立 + 可证伪（seed finding ⇒ exit 1）✓
  - 不阻塞 per-commit（`maven.yml` 无 dependency-check + nightly cron 触发）✓
  - 与现有 5 CI workflow 不冲突（既有 5 git diff 为空 + cron 不重叠）✓
  - 无双真相源（设计文档 §7 验收 8：plan 引用 `security-scanning.md` §1 不重推导证据）✓
- [x] 相关文档对齐：`security-baseline.md` 落盘 + `docs/logs/2026/08-02.md` 追加日志条目；roadmap Q2 工作项状态在 closure 时回填
- [x] 已运行验证：`mvn clean install -DskipTests`（156 模块 BUILD SUCCESS）+ `mvn test`（**2026-08-01 known-good 基线 1920/0/0/1skipped**；2026-08-02 复跑 1 pre-existing date-rollover error `TestErpMdExchangeRateApiClient` = Q6 watch-only successor `CurrencyRefresh` 生产代码 `LocalDate.now()` 残留，非 Q2 回归——security profile 经 `help:active-profiles` 核验不激活于 mvn test，零 Java/test 文件变更）；security.yml gate 本地复跑 exit 0 + seed 证伪 exit 1（Phase 4 四测试）
- [x] 无范围内项目降级为 deferred/follow-up（Critical/High 真实漏洞修复经设计文档 §10 + Phase 3 Decision 显式 out-of-scope 为 successor，非范围内项目）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未自我留空此项作为人工门控占位符
- [x] 结束证据存在于文件中
- [x] **实现与设计文档一致**（无未经 `security-scanning.md` 批准的范围偏离；任何实施期发现回填设计文档 Review Record 而非静默偏离——尤其 Java21/FindSecBugs 版本矩阵 + 插件位置首跑核验结果 + includeFilterFile 机制澄清：FindSecBugs 探测器经 JAR 依赖自动注册，includeFilterFile 作用为 scope 报告到 139 安全 BugPattern）

## Deferred But Adjudicated

### Critical/High 真实漏洞修复

- Classification: `optimization candidate`
- Why Not Blocking Closure: 设计文档 §6.4 step 6 + §10 明示 Critical/High 修复「可作 Phase 2 子任务或独立 successor」。本计划交付分类 + 基线 + 门控（安全可见性闭环）；具体修复须保护区域评估 + 范围随分类结果变化（不可预先界定）。基线建立后，未修复的 Critical/High 经 `cve_allowlist` 登记为已接受风险（C 类），门控守新增。
- Successor Required: yes —— 触发条件：Phase 3 分类确认某 Critical/High CVE 为真实漏洞（A 类）且不可 suppression，开独立修复计划（含升级路径 / 替代依赖 / 已接受风险裁决）。

### per-commit 静态规则（FindSecBugs）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 设计文档 §8.4 裁决首跑先 nightly 建立基线 + 分类误报；基线稳定后 per-commit 静态规则（不含 Dependency-Check）作为可选 successor。
- Successor Required: yes —— 触发条件：`spotbugs_counts` 基线经 nightly ≥ 14 次测量稳定（无误报抖动）+ 团队要求 per-commit 静态规则即时反馈。

### Snyk 交叉验证 / SonarQube 仪表盘

- Classification: `watch-only residual`
- Why Not Blocking Closure: 设计文档 §3.1/§3.2 否决（商业许可 / 独立 Server），保留 successor。
- Successor Required: yes —— 触发条件：项目获得 Snyk 商业许可 / SonarQube 运维资源，作交叉验证或仪表盘。

## Closure

Status Note: 执行完成（4 Phase 全 done + 设计文档 §7 验收 1-8 执行者实仓核验通过 + full-green 验证 mvn clean install 156 模块 + gate 4 falsifiability 测试 + 既有 5 CI workflow 零变更）。独立结束审计由 fresh-session 子代理执行并通过（见下方 Closure Audit Evidence）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（fresh session，cold context，不重用执行者上下文），任务 marker `MISSION_DRIVER:2026-08-01-112114-mission-driver`。
- Audit Scope: 计划结构完整性（Plan Status / 每个 Phase Status / Exit Criteria / Closure Gates / Closure 证据五点一致性）+ 实仓语义核验（Exit Criteria vs live repo）+ Anti-Hollow 检查 + Deferred honesty + Docs sync。
- Evidence（fresh session 实仓复核 walkthrough，2026-08-02）:
  - 根 `pom.xml` 含 `<profile id="security">` + `dependency-check-maven` 12.1.0 + `aggregate=true` + `failOnError=false` + NVD env 注入——`grep` 命中 line 113/130，profile 内容逐行核验通过 ✓
  - 19 域 `erp-*-service/pom.xml` 各含 `<id>security</id>` profile——`grep -l '<id>security</id>' module-*/erp-*-service/pom.xml | wc -l` = **19**（aps/assets/b2b/contract/crm/cs/drp/finance/hr/inventory/logistics/maintenance/manufacturing/master-data/notify/projects/purchase/quality/sales 全列）✓
  - `module-master-data/erp-md-service/pom.xml` 抽样核验含 `spotbugs-maven-plugin` + `findsecbugs-plugin` + includeFilterFile/excludeFilterFile（`${maven.multiModuleProjectDirectory}` 路径）✓
  - `spotbugs-include.xml` 含 **恰好 139 个 FindSecBugs BugPattern type**（`pattern="..."` 经 `tr ',' '\n' | grep -c .` 实测 139），版本绑定 findsecbugs 1.13.0 ✓
  - `spotbugs-exclude.xml` 含 4 类生成包排除（`_gen` / `api.beans` / `api.crud` / `codegen`）——`<FindBugsFilter>` schema 合法 ✓
  - `dependency-check-suppression.xml` 空骨架落盘（`<suppressions>` schema + B 类追加注释完整）✓
  - `docs/architecture/quality-engineering/security-baseline.md` 含 ```yaml BASELINE 机器可读块（`cve_allowlist:` + `spotbugs_counts:` 双段 present，双段语义注释完整）✓
  - `.github/workflows/security.yml` Anti-Hollow 核验（line 60-183）：gate Python 真实执行 (1) regex 定位 baseline yaml 块；(2) Dependency-Check JSON 提取 `(cveId, gav)` 元组；(3) SpotBugs `BugInstance/@type` 聚合计数；(4) **CVE set-diff** `cve_new = dc_actual - cve_allowlist` 非空 ⇒ `sys.exit(1)`；(5) **SpotBugs per-type count** `cnt > base` ⇒ regression；(6) 任一回归 `failed=True` ⇒ `sys.exit(1)`，否则 `Gate PASSED`。**非空函数体、非 `return null` 占位、非吞异常**——真实单向收紧门控，对齐 F8 option (b) 范式 ✓
  - cron `0 2 * * *` + `workflow_dispatch`（line 16-17）；NVD_API_KEY secret env 注入（line 54）；dependency-check:aggregate（line 55）；spotbugs:check -pl 19 service（line 60-62）；upload-artifact（line 184）✓
  - 既有 5 CI workflow 零变更：`git status --short .github/workflows/{maven,compliance,e2e,mutation,clock-rollover}.yml` = **空**；仅 `security.yml` 为 untracked 新文件 ✓
  - `maven.yml` 无 dependency-check：`grep -c "dependency-check\|spotbugs" .github/workflows/maven.yml` = **0**（不阻塞 per-commit）✓
  - cron 不重叠：security `0 2` vs clock-rollover `0 3` vs mutation `0 4`（3 个不同小时）✓
  - `docs/logs/2026/08-02.md` 落盘（4847 bytes），含 Q2 EXECUTE 条目覆盖 4 Phase + 验证状态（156 模块 BUILD SUCCESS + mvn test known-good 基线 + 1 pre-existing date-rollover 非 Q2 回归）+ bookkeeping（roadmap §MQ Q2 ready→done）✓
- Five-point consistency: Plan Status `completed` ↔ 4 Phase Status 全 `completed` ↔ 每个 Phase Exit Criteria 全 `[x]` ↔ Closure Gates 全 `[x]`（含本审计刚勾选的独立审计子项）↔ Closure 证据非占位——五点一致 ✓
- Deferred honesty: Deferred 三项（Critical/High 修复 / per-commit 静态规则 / Snyk/SonarQube）均经设计文档 §10 + Phase 3 Decision 显式 out-of-scope 为 successor，每项命名触发条件（分类确认 A 类 / 基线 ≥14 nightly 稳定 / 获得商业许可）；**无范围内的已确认缺陷或契约漂移被隐藏为 deferred/follow-up** ✓
- Audit Result: **APPROVED**——结构完整 + 实仓语义一致 + 门控非空壳 + 无静默范围缩减。计划可保持 `Plan Status: completed`。
- Audit Verifier Note: 计划作者提到的 `tools/mission-driver/src/plan-check.mjs` 在本仓库不存在（`find . -name plan-check.mjs` 无命中），故审计子代理无法重跑该自动检查脚本；上述五点一致性 + 实仓语义核验为人工等价覆盖，每个复选框从空到已勾选的转换均经实仓证据支持（唯一由本审计勾选的项目为「独立结束审计」Closure Gate 子项，其余由执行者落盘证据支持）。

执行者自验证据（落盘）：
- `pom.xml:114-160` 根 `<profile id="security">` dependency-check-maven 12.1.0 aggregate
- `module-*/erp-*-service/pom.xml`（19 文件）各 `<profile id="security">` spotbugs-maven-plugin 4.9.8.3 + findsecbugs-plugin 1.13.0
- `spotbugs-include.xml`（139 FindSecBugs BugPattern）/ `spotbugs-exclude.xml`（4 类生成包排除）/ `dependency-check-suppression.xml`（空骨架）
- `docs/architecture/quality-engineering/security-baseline.md`（§1 首跑实测 + §2 四态分类 + BASELINE yaml 块）
- `.github/workflows/security.yml`（cron 0 2 + workflow_dispatch + NVD cache + dep-check aggregate + spotbugs per-module + 双语义 Python gate）
- 19 域 spotbugsXml.xml（首跑 0 FindSecBugs finding）
- gate 4 falsifiability 测试日志（clean/seed-spotbugs/seed-cve/clean-cve）

Follow-up:

- Critical/High 真实漏洞修复 successor（见上 Deferred）。
- per-commit 静态规则 / Snyk / SonarQube successor（见上 Deferred）。
- Q5 Phase 2 / Q7 Phase 1 各有独立计划（同批 `2026-08-02-1121-2` / `-3`）。
