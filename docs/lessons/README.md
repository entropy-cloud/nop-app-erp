# Lessons Index

Use this directory for numbered reusable lessons learned from development.

These are not day-by-day notes. They are durable engineering lessons that should help future sessions avoid repeating the same mistake.

Recommended filenames:

- `01-requirement-source-was-not-implementation-ready.md`
- `02-prototype-fidelity-did-not-cover-business-rules.md`
- `03-plan-closure-claimed-too-early.md`

When a bug, retrospective, or audit reveals a repeatable pattern, consider promoting it into `docs/lessons/`.

## Lessons

- `04-bizmodel-service-method-contract-and-testing.md` — BizModel/I*Biz 服务方法契约（注解 + IServiceContext 末参 + @Name）与测试必须经 IGraphQLEngine（直调缺 session）。含 `@SingleSession` 误判实录与验证结论。
- `05-nop-e2e-failure-log-first-diagnosis.md` — Nop 失败诊断：日志优先、从后向前定位。Playwright 超时只是「果」，因几乎总在服务端；不信遗留 server、跑最小复现、读 `errorCode=`/`@_loc`/`Caused by:` 因果链。含「渲染超时」长期误诊为环境问题的实录与 `tools/parse-nop-errors.mjs`。
- `06-codegen-product-edit-overwrite.md` — **代码生成产物编辑必被 `mvn clean install` 覆盖**：`_` 前缀文件 / `_gen/` 子目录 / `__XGEN_FORCE_OVERRIDE__` 标记 / 从 ORM `<dict>` 生成的 yaml 均为生成产物。改这些 = 临时状态。**唯一正确位置是模型源或保留层 Delta**。含 notify inbox saga + business-type.dict.yaml 两案各 3 轮审计实录与编辑前自检清单。
