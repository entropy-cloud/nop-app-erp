
### mnt-008 写侧 visit orgId 回填

- Classification: `watch-only residual`
- Why Not Blocking Closure: ErpMntSchedule 实体无 orgId 列（ORM 实证），无法从 schedule 继承；需 ORM 加列（保护区）或经 equipment 间接解析
- Successor Required: `yes`（触发条件：ErpMntSchedule 加 orgId 列或 equipment→orgId 解析链落地时）
