-- 客服域种子（RC-R1.65，P1-RC-054，UC-CS-01 ⑥）：工单 TK 编号规则。
-- TK{@year}{@month}{@csTicketMonthSeq:4}：csTicketMonthSeq = 自定义 CodeRule 变量
--（bean nopCodeRuleVariable_csTicketMonthSeq），按月懒建序列行 cs_ticket_code_seq_{yyyyMM}
--（stepSize=1/cacheSize=0，DB 行锁原子取号），避免 {@seq:4} 单序列 4 位回绕
--（平台 Sequence resetType 不自动归零，见 nop-entropy runbook generate-business-code.md）。
-- SEQ_NAME='default' 为非消费占位：pattern 不含 {@seq}，SysCodeRuleGenerator 仅作非空校验。
INSERT INTO nop_sys_code_rule
  (SID, NAME, DISPLAY_NAME, CODE_PATTERN, SEQ_NAME, DEL_FLAG, VERSION,
   CREATED_BY, CREATE_TIME, UPDATED_BY, UPDATE_TIME, REMARK)
VALUES
  ('cs-ticket-code-rule', 'cs-ticket-code', '客服工单TK编号规则',
   'TK{@year}{@month}{@csTicketMonthSeq:4}', 'default', 0, 0,
   'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP,
   'RC-R1.65 UC-CS-01 ⑥ TK{YYYYMM}{SEQ4} 按月序列编号');
