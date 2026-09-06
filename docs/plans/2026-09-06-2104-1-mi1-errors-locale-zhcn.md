---
status: active
mission: ai-check-r3
work-item: MI.1
group: "2026-09-06-2104"
verify: [test]
---

# 2026-09-06-2104-1 MI.1 Errors 接口 @Locale("zh-CN") 补齐

## Current Baseline

- 实仓核验（2026-09-06）：`*Errors.java` 共 22 个（21 个分布于 `module-*/erp-*-service`，1 个在 `app-erp-all`），全部**未**标 `@Locale`（`grep -L "@Locale"` 对 22 文件全命中）。清单：
  - `module-aps/erp-aps-service/src/main/java/app/erp/aps/service/ErpApsErrors.java`
  - `module-assets/erp-ast-service/src/main/java/app/erp/ast/service/ErpAstErrors.java`
  - `module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/ErpB2bErrors.java`
  - `module-common-service/src/main/java/app/erp/common/service/ErpCommonErrors.java`
  - `module-contract/erp-ct-service/src/main/java/app/erp/ct/service/ErpCtErrors.java`
  - `module-crm/erp-crm-service/src/main/java/app/erp/crm/service/ErpCrmErrors.java`
  - `module-cs/erp-cs-service/src/main/java/app/erp/cs/service/ErpCsErrors.java`
  - `module-drp/erp-drp-service/src/main/java/app/erp/drp/service/ErpDrpErrors.java`
  - `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/ErpFinErrors.java`
  - `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinPostingErrors.java`
  - `module-hr/erp-hr-service/src/main/java/app/erp/hr/service/ErpHrErrors.java`
  - `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/ErpInvErrors.java`
  - `module-logistics/erp-log-service/src/main/java/app/erp/log/service/ErpLogErrors.java`
  - `module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/ErpMntErrors.java`
  - `module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/ErpMfgErrors.java`
  - `module-master-data/erp-md-service/src/main/java/app/erp/md/service/ErpMdErrors.java`
  - `module-notify/erp-notify-service/src/main/java/app/erp/notify/service/ErpNotifyErrors.java`
  - `module-projects/erp-prj-service/src/main/java/app/erp/prj/service/ErpPrjErrors.java`
  - `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/ErpPurErrors.java`
  - `module-quality/erp-qa-service/src/main/java/app/erp/qa/service/ErpQaErrors.java`
  - `module-sales/erp-sal-service/src/main/java/app/erp/sal/service/ErpSalErrors.java`
  - `app-erp-all/src/main/java/app/erp/all/meta/ErpModuleMetaErrors.java`
- 平台先例实仓核验：`../nop-entropy/nop-batch/nop-batch-core/src/main/java/io/nop/batch/core/BatchErrors.java`、`../nop-entropy/nop-persistence/nop-dao/src/main/java/io/nop/dao/DaoErrors.java`、`../nop-entropy/nop-core-framework/nop-config/src/main/java/io/nop/config/ConfigErrors.java` 均在接口上标 `@Locale("zh-CN")`（import `io.nop.api.core.annotations.core.Locale`）。
- 判定准绳：`docs/architecture/i18n-compliance.md` 合规基线表 #2——`*Errors.java` 接口必须补 `@Locale("zh-CN")` 声明源语言；`ErrorCode.define` 中文描述本身合规（表 #1，本轮不建 en 镜像）。
- CJK 门控基线：`docs/audits/cjk-baseline.md` §SNAPSHOT（CAT1=335/CAT2=204/CAT3=390/CAT4=1700），`--strict` 单向收紧已绿。
- 绿色基线：`docs/testing/known-good-baselines.md` 2026-09-06 `ai-check-r3-m0` 行（156 模块 BUILD SUCCESS；全 reactor 4006 tests / 0 failures / 0 errors / 1 skipped）。
- 剩余差距：22 文件源语言声明缺失，与本轮 i18n 合规基线的「必须补齐」裁定不符。

## Goals

- 22/22 `*Errors.java` 接口标注 `@Locale("zh-CN")` + 对应 import，纯加性改动，错误码描述文本与消息语义零变化。
- 全 reactor `mvn test` 零回归（`verify: [test]` 门控）；`node tools/check-hardcoded-cjk.mjs --strict` 保持绿（注解为 ASCII，各 CAT 计数不升）。

## Non-Goals

- 不改任何 `ErrorCode.define` 描述文本，不新建 en i18n yaml 承载文件（合规基线表 #1 裁定）。
- 不做 CAT-2 异常参数、CAT-3 运行时字符串、CAT-4 页面 yaml 清剿（MI.5a/5b/6/7/8 范围）。
- 不动 page/flux yaml、`_init-data` seed、业务行为与控制流。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/architecture/i18n-compliance.md`（合规基线表 #2）、`docs/backlog/ai-check-r3-roadmap.md`（MI.1 行）、`docs/errors/README.md`
- Skill Selection Basis: roadmap MI.1 行 Skill 列指定 `none`——纯机械注解补齐，镜像 nop-entropy 框架先例，无可复用技能覆盖

## Phase 1 — 22 文件注解补齐

> 类型：逐项标注（2 项 Add | 2 项 Proof）。
> 约束：注解写法镜像 nop-entropy 先例（`BatchErrors`/`DaoErrors`/`ConfigErrors`），属框架强制模式，无需替代方案分析。
> Skill: none（roadmap MI.1 行指定）
> Targets: 上列 22 个 `*Errors.java`
> Prereqs: M0.6（已 done）

- [x] <Add> 21 个 `module-*` service Errors 接口补 `@Locale("zh-CN")` 注解 + `io.nop.api.core.annotations.core.Locale` import——类级注解置于 `public interface` 声明行上方；javadoc、常量、`ErrorCode.define` 文本零改动
      - Skill: none
- [x] <Add> `app-erp-all/src/main/java/app/erp/all/meta/ErpModuleMetaErrors.java` 同型补注解 + import
      - Skill: none
- [x] <Proof> 机械断言零遗漏：对 22 文件清单逐文件 grep `@Locale("zh-CN")`，未命中文件数为 0（实测 `grep -rL` 未命中 = 0，22/22 命中；`git diff` 44 insertions / 0 deletions——每文件恰 +import +注解 2 行，纯加性）
      - Skill: none
- [x] <Proof> `node tools/check-hardcoded-cjk.mjs --strict` exit 0（CAT 计数不高于快照）；受触及 service 模块编译通过（实测 exit 0，CAT1=335/CAT2=204/CAT3=390/CAT4=1700 与冻结快照精确一致；`mvn clean install -DskipTests` 156 模块 BUILD SUCCESS）
      - Skill: none

Exit Criteria:

- [x] 22/22 Errors 接口带 `@Locale("zh-CN")`，grep 断言零遗漏
- [x] `--strict` 保持绿；改动文件编译通过

## Draft Review Record

- dispatch review #review-2026-09-05-123532-mission-driver-2026-09-06-2104-1-mi1-errors-locale-zhcn-1-e4a7c259 to opencode-subagent-2026-09-06-mi1-review
- 2026-09-06：iteration 1，共识 approved #review-2026-09-05-123532-mission-driver-2026-09-06-2104-1-mi1-errors-locale-zhcn-1-e4a7c259

## Verification

- pass build 2026-09-06-mi1 exit=0 `mvn clean install -DskipTests`：156 reactor 模块 BUILD SUCCESS。
- pass test 2026-09-06-mi1 exit=0 全 reactor `mvn test`：BUILD SUCCESS，0 failures / 0 errors / 1 skipped（surefire XML 汇总 3991/0/0/1；对照 `ai-check-r3-m0` 行 4006/0/0/1 零新增失败，failure 剖面精确一致）。计数差 -15 集中于 fin(-8)/inv(-5)/ast(-2) 三模块；独立结束审计受控 A/B 裁决：改动前 HEAD（一次性 git worktree）与改动后工作树各跑 standalone `mvn test -pl app-erp-finance-service,app-erp-inventory-service,app-erp-assets-service`，两侧同为 525/248/337、全零失败、测试类清单逐一同（102/42/39）——注解改动被证明不可能影响测试注册，-15 属构建状态下测试发现的运行间漂移（diff 两侧同现），与本计划无关。
- pass cjk-strict 2026-09-06-mi1 exit=0 `node tools/check-hardcoded-cjk.mjs --strict`：PASS，CAT1=335/CAT2=204/CAT3=390/CAT4=1700 与冻结快照精确一致（注解为 ASCII，各 CAT 计数零变化）。
- pass compliance 2026-09-06-mi1 exit=0 `bash docs/audits/nop-compliance-checker.sh`：零漂移，全表 R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R6=2/R10=14/R12a=71/R12b=66/R12c=42 与 `ai-check-r3-m0` 行精确一致。
- pass test ai-check-r3-verify-2026-09-07-0036 exit=0 mission-driver verify run：全 reactor `mvn clean install -DskipTests` 156 模块 BUILD SUCCESS + `mvn test` BUILD SUCCESS（模块 Results 行汇总 4006 tests / 0 failures / 0 errors / 1 skipped，与 `ai-check-r3-m0` 基线行精确一致，零新增失败）。

## Closure

Status Note: 计划唯一结果表面（22 `*Errors.java` 接口 `@Locale("zh-CN")` 声明源语言）已纯加性落地（44 insertions / 0 deletions），四项验证门控全绿且零漂移，独立结束审计 ACCEPT，无范围内项目降级，owner docs 为常设规则文档落地即满足——计划可关闭。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（新会话，无执行者上下文），task id `ses_f88b6a494ffeh1PiaVxEK4sKBq`，2026-09-06
- Evidence: 审计报告 7 项核查全 PASS——(1) Phase 1 状态/勾选一致性；(2) 22 文件逐文件注解+import 机械复核、`grep -rL` 空；(3) `git diff --numstat` 22×(2,0)=44/0、diff 内容仅 import+注解、零其他生产代码触碰；(4) surefire 复盘（reactor 3991/0/0/1、app-erp-all 70/0/0/1）+ 对照基线零新增失败 + **受控 A/B**（pre-change HEAD 一次性 worktree vs post-change 工作树 standalone 复跑同为 525/248/337、零失败、测试类清单逐一同）证明注解改动不影响测试注册；(5) cjk `--strict` 与 compliance checker 审计侧复跑均 exit 0 且计数与冻结快照/基线行精确一致；(6) Non-Goals 零触碰；(7) owner docs 常设规则判定无需编辑。verdict **ACCEPT**，附 1 advisory（Verification §(b) -15 裁决措辞按 A/B 证据改写）——已修订后闭合。
- Follow-up: 无（本计划无非阻塞跟进项）
- dispatch audit #audit-2026-09-05-123532-mission-driver-2026-09-06-2104-1-mi1-errors-locale-zhcn-1-34ba85c4 to ses_f88b6a494ffeh1PiaVxEK4sKBq models={exec:zhipuai/glm-5.2,aud:zhipuai/glm-5.2}
- accepted #audit-2026-09-05-123532-mission-driver-2026-09-06-2104-1-mi1-errors-locale-zhcn-1-34ba85c4：独立结束审计 ACCEPT——22/22 `*Errors.java` 带 `@Locale("zh-CN")`（live grep 零遗漏、import 22/22、diff 纯加性 44/0）；`mvn clean install -DskipTests` 156 模块 exit 0、全 reactor `mvn test` 0/0/1 零新增失败（-15 漂移经受控 A/B 裁决与本计划无关）、cjk `--strict` 与 compliance checker 审计侧复跑均 exit 0 零漂移；1 advisory 已修订后闭合，无 open finding。
