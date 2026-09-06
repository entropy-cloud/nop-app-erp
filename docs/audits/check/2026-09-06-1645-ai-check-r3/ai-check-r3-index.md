# ai-check-r3 本轮索引（Round Index）

> **本轮唯一规范执行目录**（roadmap 规则 8/9）：`docs/audits/check/2026-09-06-1645-ai-check-r3/`
> 本轮（mission `ai-check-r3`）内所有 plan 幂等复用本目录，`mkdir -p` 同一路径，不新建别的目录；历史轮次目录（含 r1/r2）只读，新开一轮执行才建新时间戳目录。
> 路线图：`docs/backlog/ai-check-r3-roadmap.md`；跨轮聚合索引：`docs/audits/check/ai-check-index.md`。

## 本目录产物清单

| 产物 | 来源工作项 | 说明 |
| --- | --- | --- |
| `ai-check-r3-index.md` | M0.3 | 本索引（本轮唯一，M1.17/MV.2 以此单点校验） |
| `m0-3-baseline-records.md` | M0.3 | 四项基线记录（build / test / compliance checker / i18n-coverage checker）+ 快照双写一致性证明 |
| `m0-3-cjk-baseline-snapshot.md` | M0.3 | CJK 基线快照双写副本（与 `docs/audits/cjk-baseline.md` SNAPSHOT 块内容一致） |
| `m0-3-mvn-install.log` / `m0-3-mvn-test.log` | M0.3 | `mvn clean install -DskipTests` / 全 reactor `mvn test` 原始日志 |
| `m0-3-compliance-checker.log` / `m0-3-i18n-coverage-checker.log` | M0.3 | 两个既有 checker 原始输出 |
| `m0-4-page-yaml-source-map.md` | M0.4 | 页面 yaml 源头链判定标准 + 修复策略矩阵（覆盖冻结口径下全部含 CJK 页面 yaml 文件） |

> i18n 工具链产物 = 脚本输出 + 基线登记（roadmap 横切关注点 13）：M0.2/M0.3 的 checker/快照产物落 `docs/audits/cjk-baseline.md` + 本目录，不产 `ck-*` 报告；`ck-*` 报告自 M1.x 审计切片起产出。
