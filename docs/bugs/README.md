# 回归笔记索引

## 目的

`docs/bugs/` 存放非显而易见的回归、微妙根本原因以及应影响未来审查的修复笔记。

本文件是目录索引（回归笔记清单），按发现批次分组。**写作规范**见 `00-bug-fix-note-writing-guide.md`（何时编写、必备小节、命名约定）。

## 与写作指南的关系

- `00-bug-fix-note-writing-guide.md` —— **怎么写**回归笔记（写作指南，非目录索引）。
- 本 README —— **有什么**回归笔记（清单与分类，便于维护者/审查者经单一索引浏览全部回归历史）。

## 回归笔记清单

> 文件名按 `{YYYY-MM-DD}-{HHmm}-?{slug}.md` 命名（见写作指南）。按发现日期排序。

### 命名/表结构类

| 文件 | 回归要点 |
|------|----------|
| `2026-07-05-c1-organization-table-name-typo-propagation.md` | 组织表名拼写错误经 codegen 跨层传播 |

### 平台反模式类（与 nop-entropy best practices 相关）

| 文件 | 回归要点 |
|------|----------|
| `2026-09-03-picker-loadaction-selection-convert-time-misjudgment.md` | picker loadAction `selection` 被误判"convert-time only"删除（实为 runtime 一等参数：request-runtime.ts 显式透传 fetcher）+ `dependsOn` 跨体系串味幻觉配置；五步误判链：gql: 历史包袱泛化 / 只验生产侧不验消费侧 / 静默降级零失败信号 / 副作用当清理目标 / 相邻体系字段串味 |
| `2026-07-07-1915-dao-updateentity-in-bizmodel.md` | BizModel 中 `dao().updateEntity` 绕过 CrudBizModel 管道 |
| `2026-07-07-1915-localdatetime-now-in-12-domains.md` | 12 域误用 `LocalDateTime.now()` 而非 `CoreDate.now()` |
| `2026-07-07-1915-sales-credit-control-multi-currency-and-ar-balance-omission.md` | 销售信用控制多币种 + AR 余额遗漏 |
| `2026-07-07-1915-voucher-amount-plaintext-in-graphql-query.md` | 凭证金额明文暴露在 GraphQL 查询 |

### 页面/视图类

| 文件 | 回归要点 |
|------|----------|
| `2026-07-08-1107-cs-ticket-view-layout-validation-and-generic-api-regression.md` | CS 工单 view 布局校验 + 通用 API 回归 |
| `2026-07-09-1249-dashboard-amis-var-mangling.md` | 看板 AMIS 变量名 mangle 问题 |
| `2026-07-09-1249-report-render-container-wiring.md` | 报表渲染容器接线 |
| `2026-08-23-ar-ap-aging-now-expression-flux.md` | ar-ap-aging 页 `${NOW()}` AMIS 公式 flux 求值失败（console error 门控 4 用例红；08-11 已失败被计数误差漏记） |
| `2026-08-23-report-download-button-flux-gap.md` | 报表下载按钮 button-toolbar/actionType:download flux 渲染缺口（10 AMIS 按钮用例预存红；08-11 同因漏记） |
| `2026-08-23-hr-shift-scheduling-concurrent-snapshot-id-flake.md` | TestErpHrShiftScheduling 并发排班快照存活行 id 非确定性（M3.3 seq-default 迁移引入，~50% flake；survivor id @var 引用修复） |
| `2026-09-01-0400-report-flux-rewrite-browser-assertion-drift.md` | 报表 flux 原生重写（751749e17，08-31）后报表正文 `${reportHtmlData}` 未解包 `.data` 静默空渲染 + reports visual/snapshot 谓词 `/graphql`→`/r/` 漂移 + inventory 像素基线跨月漂移（三层根因，归 successor） |

### 数据/字段长度类

| 文件 | 回归要点 |
|------|----------|
| `2026-07-17-1430-ar-ap-item-code-overflows-vouchercode-for-long-notes-codes.md` | AR/AP item code 溢出 voucherCode（长 notes/codes） |

### 稳定性/回归门禁类

| 文件 | 回归要点 |
|------|----------|
| `2026-07-20-2200-page-error-count-instability.md` | 页面错误数不稳定 |
| `2026-07-23-1408-full-suite-regression-gate-findings.md` | 全套回归门禁发现汇总 |
| `2026-08-08-1130-compliance-checker-r3-whitelist-abort-after-flux-flip.md` | flux 翻转后 compliance-checker R3 白名单零匹配致 set -e 静默中止，checker 只跑到 R3 无汇总表，CI gate 失效 |
| `2026-08-23-e2e-masked-amount-observability.md` | E3.1 金额掩码下 E2E 无可观察账号（FNPT/读授权缺口，4 spec 断言降可观察面） |

### 业务逻辑/钩子容错类

| 文件 | 回归要点 |
|------|----------|
| `2026-07-26-0410-pur-commitment-release-hook-tolerance-asymmetry.md` | 采购承付释放钩子容错不对称 |
| `2026-08-23-ct-terminate-approval-fnpt-deadlock.md` | ct 两段式终止在 enforcement 下无可用账号（approver 身份守卫 × FNPT 声明缺口死锁，2 用例红） |
| `2026-08-23-drp-release-approved-double-advance.md` | drp releaseApproved 双段推进计划状态触发非法迁移（08-12 守卫硬化暴露，多行计划批量释放 1 用例红） |
| `2026-08-23-mfg-kit-check-self-reservation.md` | mfg 齐套校验未计回工单自身预留（approve 预留 × checkAvailability 自斥，E2E 口径 config-gate 关闭恢复） |

## 维护规则

- 新增回归笔记后，在本清单对应分组追加一行。
- 写作前先读 `00-bug-fix-note-writing-guide.md` 确认是否达到编写门槛（非琐碎、跨层、易重现引入）。
- 本清单分类为粗粒度导航；详细根因与防止重现写在各笔记内。
