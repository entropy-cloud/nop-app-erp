# 安全扫描基线（MQ Q2）—— Dependency-Check CVE + SpotBugs/FindSecBugs 静态规则

> Owner Doc: `docs/architecture/quality-engineering/security-scanning.md`（收敛实施契约）
> Plan: `docs/plans/2026-08-02-1121-1-mq-q2-security-scanning-impl.md`（Phase 2 实现）
> CI 回归门控: `.github/workflows/security.yml`（nightly，option (b)：扫描工具=pure reporter，gate 逻辑在 CI）
> 单一固定路径：本文件是安全扫描基线**唯一落盘位置**，供 CI Python gate 解析 `## BASELINE` 机器可读块。
> 基线反映**分类后**真实基线（suppression/filter 已在扫描阶段排除误报/生成代码噪声）。

## 1. 首次扫描实测基线

> 计算口径：
> - **SpotBugs/FindSecBugs**：per-module 扫描 19 域 service 编译产物字节码，`includeFilterFile`（spotbugs-include.xml）scope 到 139 个 FindSecBugs 安全 BugPattern；`excludeFilterFile`（spotbugs-exclude.xml）排除 Nop 生成代码三类（`_gen`/`api.beans`/`api.crud`）。BugInstance 按 `@type` 聚合计数。
> - **Dependency-Check**：reactor-root `aggregate` 聚合 156 模块传递依赖，比对 NVD CVE 数据库。首跑受 NVD API key infra prereq 阻塞（设计文档 §4.3），CI nightly 经 `NVD_API_KEY` secret 注入执行真实首跑。

### 1.1 SpotBugs/FindSecBugs 首跑（2026-08-02，19 域 service）

| 域 (service module) | FindSecBugs finding 计数 |
|---------------------|--------------------------|
| erp-md-service（master-data） | **0** |
| erp-notify-service | **0** |
| erp-fin-service（finance） | **0** |
| erp-inv-service（inventory） | **0** |
| erp-qa-service（quality） | **0** |
| erp-pur-service（purchase） | **0** |
| erp-sal-service（sales） | **0** |
| erp-ast-service（assets） | **0** |
| erp-prj-service（projects） | **0** |
| erp-mfg-service（manufacturing） | **0** |
| erp-mnt-service（maintenance） | **0** |
| erp-crm-service | **0** |
| erp-cs-service | **0** |
| erp-hr-service | **0** |
| erp-aps-service | **0** |
| erp-ct-service（contract） | **0** |
| erp-drp-service | **0** |
| erp-log-service（logistics） | **0** |
| erp-b2b-service | **0** |
| **合计** | **0** |

> **19 域 FindSecBugs 安全规则全部零命中**——ERP 业务代码经 Nop 平台 ORM 参数化查询 + 平台安全包装，无可静态检测的注入/XSS/反序列化/硬编码密钥反模式。验证证据：19 域 `spotbugsXml.xml` 均 `BugInstance size is 0`（2026-08-02 实测）。
> **配置验证**：findsecbugs-plugin 1.13.0 经 `-X` debug 确认加载到 spotbugs classpath（`Adding to pluginArtifact -> findsecbugs-plugin-1.13.0.jar`）；spotbugs-maven-plugin 4.9.8.3 + spotbugs 4.9.8 支持 Java 21-26 字节码（本地 Java 26 首跑通过，CI Java 21 子集）。
> **排除验证**：D 类生成代码噪声经 `spotbugs-exclude.xml` 排除（`_gen`/`api.beans`/`api.crud` 三类，对齐 Q1 pitest `excludedClasses` 先例 + mutation-testing.md §1.3 三类 352 生成类）。

### 1.2 Dependency-Check CVE 首跑（受 NVD API key infra prereq 阻塞）

> 状态：**pending CI nightly 首跑**。NVD API key 是 infra prereq（设计文档 §4.3：2024+ 无 key 限速严苛 ~5 req/30s，156 模块 aggregate 首跑无 key 大概率超时），非本计划可绕过。CI nightly `.github/workflows/security.yml` 经 `NVD_API_KEY` repo secret 注入执行真实首跑；首跑 `dependency-check-report.json` 产出后，CVE 发现经 §2 四态分类，B 类（误报）落 `dependency-check-suppression.xml`，C 类（已接受风险）登记 `cve_allowlist`。
> `cve_allowlist` 基线在首跑分类完成前为空（设计文档 §5.4：Empty list on first run before triage）——空 allowlist = 最严格基线（任何 actual CVE 发现均不在 allowlist ⇒ gate fail），门控成立。

## 2. 首次扫描发现四态分类（设计文档 §5.1）

> 四态：A 真实漏洞 / B 误报 / C 已接受风险 / D 生成代码噪声。标签为 Q2 独立定义（与 compliance-baseline R3 三态语义不同）。

| 分类 | SpotBugs/FindSecBugs（19 域首跑） | Dependency-Check（pending NVD key） |
|------|-----------------------------------|-------------------------------------|
| **A 真实漏洞** | 0 | pending（CI nightly 首跑后分类） |
| **B 误报** | 0（无 finding 无须 suppression） | pending |
| **C 已接受风险** | 0 | pending |
| **D 生成代码噪声** | 0 到达报告（excludeFilter 已排除三类生成包） | N/A（dep-check 扫描依赖 GAV，无生成代码概念） |

> **SpotBugs 四态结论**：FindSecBugs 安全规则零命中 → A/B/C 均为 0；D 类经 excludeFilter 在扫描阶段排除（未到达报告）。`spotbugs_counts` 基线为空（所有 BugPattern type 计数 = 0 或不存在），即「任何 FindSecBugs 安全规则命中 ⇒ gate fail」——最严格基线。
> **Dependency-Check 四态结论**：待 CI nightly 首跑（NVD key 注入）后分类。Critical/High 真实漏洞修复为独立 successor（见 §4 Deferred）。

### 2.1 SpotBugs 插件位置裁决（设计文档 §6.2 R2-MAJOR-2）

- **spotbugs-maven-plugin 4.9.8.3 + findsecbugs-plugin 1.13.0**。
- **接入位置**：候选 (a) `<pluginManagement>` 集中版本否决（与 Q1 R4 同型：根 pom app-erp 仅为 reactor 聚合器，子模块 `<parent>` 是 nop-entropy，pluginManagement 经根 pom 不继承）。裁决候选 (b) **per-module**：19 域 service 模块各声明 `<profile><id>security</id>`（对齐 Q1 pitest per-module 先例）。根 pom `<profile id="security">` 仅含 dependency-check aggregate（reactor-root goal，设计文档 §6.1：Q1 R4 不适用）。
- **includeFilterFile 裁决（实施期发现）**：设计文档 §6.2 称 includeFilterFile「启用 FindSecBugs 规则」。实测发现 FindSecBugs 探测器经 `findsecbugs-plugin` JAR 依赖自动注册（`-X` debug 确认 `findsecbugs-plugin-1.13.0.jar` 加入 spotbugs pluginArtifact）；includeFilterFile 的真实作用是 **scope 报告到 FindSecBugs 139 个安全 BugPattern**（从 `findsecbugs-plugin-1.13.0.jar!/findbugs.xml` 提取生成 `spotbugs-include.xml`），避免安全基线被 SpotBugs 通用代码质量规则（PERFORMANCE/STYLE/BAD_PRACTICE）稀释。此为机制澄清非范围偏离。

### 2.2 Java 21 + FindSecBugs 版本矩阵核验（设计文档 §6.2 step 7 + R6.1）

- **CI 全域 `setup-java java-version: '21'`**；本地 Java 26（Zulu26.30）。
- **SpotBugs 4.9.8** 支持 Java 21-26 字节码；**spotbugs-maven-plugin 4.9.8.3**。
- **findsecbugs-plugin 1.13.0**（2024-02 发布，最新）兼容 SpotBugs 4.9.x。
- **首跑验证（2026-08-02）**：本地 Java 26 跑 19 域 spotbugs:check 全部 BUILD SUCCESS + BugInstance 解析正常 → 矩阵兼容。R6.1 残留风险（findsecbugs 维护节奏慢）经首跑未显现。

## BASELINE (machine-readable)

> CI 门控源（`.github/workflows/security.yml` 解析此 yaml 块）。双语义（设计文档 §5.4）：
> - `cve_allowlist` → **set-membership**：actual Dependency-Check 发现的 `(cveId, gav)` 元组不在 allowlist ⇒ FAIL（防置换回归）。
> - `spotbugs_counts` → **per-type count**：actual 某 BugPattern type 计数 > baseline ⇒ FAIL（容忍行号漂移）。
> 两段独立判定，任一 fail 则 CI red。调高基线须开独立计划裁决（设计文档 §5.3 单向收紧）。
> 当前为分类后基线：SpotBugs 19 域首跑零命中（all 0）；Dependency-Check 待 CI nightly 首跑（NVD key）后填充 cve_allowlist。

```yaml
# security-baseline.md machine-readable block. CI gate parses this.
# Two segments with DIFFERENT comparison semantics:
#   cve_allowlist   -> set-membership (any actual finding NOT listed => FAIL)
#   spotbugs_counts -> per-type count (actual > baseline => FAIL)
# Raising either requires an independent adjudication plan (see security-scanning.md §5.3).

cve_allowlist:
  # each entry = a classified/accepted (cveId, dependencyGav) tuple.
  # format: "<cveId> @ <group:artifact:version>"
  # Empty: Dependency-Check first run pending NVD API key (infra prereq, security-scanning.md §4.3).
  # Empty allowlist = strictest baseline: any actual CVE finding => FAIL.
  # Populated by CI nightly first-run triage (§5.1 four-state classification).

spotbugs_counts:
  # key = SpotBugs/FindSecBugs BugPattern type; value = baseline count.
  # one-way tightening: actual count > baseline => FAIL.
  # 19-domain first run (2026-08-02): 0 FindSecBugs findings across all service modules.
  # Empty map = strictest baseline: any FindSecBugs finding => FAIL.
  # (no keys = every type has implicit baseline 0)
```

## 3. 残留风险与 successor

> 引用设计文档 §10 残留风险汇总，本节仅登记本计划 closure 相关 successor。

- **R6.1（Java 21 + FindSecBugs 版本矩阵）**：首跑（2026-08-02）兼容性验证通过（spotbugs 4.9.8 + findsecbugs 1.13.0 + Java 26）。findsecbugs 维护节奏慢，未来 SpotBugs 大版本升级时须重测；includeFilter 的 139 类型清单须按新 findsecbugs 版本重新生成。
- **R1（NVD API key infra prereq）**：Dependency-Check 首跑受此阻塞。CI nightly 经 `NVD_API_KEY` secret 注入执行。无 key 降级备选限速下耗时不可接受（设计文档 §4.3）。

## 4. Deferred（Critical/High 真实漏洞修复）

> 设计文档 §6.4 step 6 + §10 + plan Phase 3 Decision。

- **Critical/High 真实漏洞修复**：本计划交付分类 + 基线 + 门控（安全可见性闭环）；具体 Critical/High CVE 修复为独立 successor。**裁决为 successor**（避免本计划范围爆炸 + 修复须保护区域评估）。触发条件：CI nightly Dependency-Check 首跑 + §5.1 分类确认某 Critical/High CVE 为真实漏洞（A 类）且不可 suppression，开独立修复计划（含升级路径 / 替代依赖 / 已接受风险裁决）。基线建立后，未修复的 Critical/High 经 `cve_allowlist` 登记为已接受风险（C 类），门控守新增。
- **per-commit 静态规则（FindSecBugs）**：设计文档 §8.4 裁决首跑先 nightly 建立基线 + 分类误报；基线稳定后 per-commit 静态规则（不含 Dependency-Check）作为可选 successor。触发条件：`spotbugs_counts` 基线经 nightly ≥ 14 次测量稳定 + 团队要求 per-commit 即时反馈。
