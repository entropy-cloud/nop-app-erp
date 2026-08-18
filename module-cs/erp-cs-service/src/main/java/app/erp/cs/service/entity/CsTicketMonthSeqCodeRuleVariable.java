package app.erp.cs.service.entity;

import io.nop.api.core.annotations.txn.TransactionPropagation;
import io.nop.api.core.convert.ConvertHelper;
import io.nop.commons.util.StringHelper;
import io.nop.dao.coderule.CodeRuleParams;
import io.nop.dao.coderule.ICodeRuleVariable;
import io.nop.dao.seq.ISequenceGenerator;
import io.nop.dao.txn.ITransactionTemplate;
import io.nop.orm.IOrmSession;
import io.nop.orm.IOrmTemplate;
import io.nop.sys.dao.entity.NopSysSequence;
import jakarta.inject.Inject;

/**
 * TK 工单编号按月序列 CodeRule 变量（RC-R1.65，P1-RC-054，UC-CS-01 ⑥）。
 *
 * <p>codePattern {@code TK{@year}{@month}{@csTicketMonthSeq:4}} 的 {@code csTicketMonthSeq} 实现：
 * 按月 seqName {@code cs_ticket_code_seq_{yyyyMM}}（runbook generate-business-code.md:89 认可的按月重置
 * 应用层实现——平台 {@code resetType} 不自动归零，单序列 4 位回绕在单月 &gt;10^4 票时碰撞 UK(code,orgId)）。
 *
 * <p>月行懒建（REQUIRES_NEW 独立事务，镜像 {@code SysSequenceGenerator.runLocal}）：必须先于首次
 * {@code generateLong} 提交，否则 {@code findSeqItem} 对缺失行落入 uuid 随机项。stepSize=1/cacheSize=0
 * 连续号语义，DB 行锁保证并发无重号。
 */
public class CsTicketMonthSeqCodeRuleVariable implements ICodeRuleVariable {

    public static final String SEQ_NAME_PREFIX = "cs_ticket_code_seq_";

    @Inject
    IOrmTemplate ormTemplate;

    @Inject
    ITransactionTemplate transactionTemplate;

    @Inject
    ISequenceGenerator sequenceGenerator;

    @Override
    public String resolve(String options, CodeRuleParams params) {
        int count = ConvertHelper.toPrimitiveInt(
                StringHelper.isEmpty(options) ? "4" : options,
                err -> new io.nop.api.core.exceptions.NopException(err));

        String yyyyMM = String.format("%04d%02d", params.getNow().getYear(), params.getNow().getMonthValue());
        String seqName = SEQ_NAME_PREFIX + yyyyMM;
        ensureMonthlySequenceRow(seqName);
        long seq = sequenceGenerator.generateLong(seqName, false);
        return formatSeq(seq, count);
    }

    /**
     * 懒建月序列行（查到即返回）；并发插入冲突按已有行处理。
     * REQUIRES_NEW 独立事务（nop-check：镜像平台 SysSequenceGenerator.runLocal 的序列行初始化边界，
     * 保证提交可见性，避免外层事务未提交行与取号新会话行锁互等）。
     */
    protected void ensureMonthlySequenceRow(String seqName) {
        ormTemplate.runInNewSession(session ->
                transactionTemplate.runInTransaction(null, TransactionPropagation.REQUIRES_NEW, txn -> {
                    NopSysSequence existing = findBySeqName(session, seqName);
                    if (existing != null) {
                        return null;
                    }
                    NopSysSequence entity = (NopSysSequence) ormTemplate.newEntity(NopSysSequence.class.getName());
                    entity.setSeqName(seqName);
                    entity.setSeqType("seq");
                    entity.setIsUuid((byte) 0);
                    entity.setNextValue(1L);
                    entity.setStepSize(1);
                    entity.setCacheSize(0);
                    try {
                        session.save(entity);
                        session.flush();
                    } catch (Exception e) {
                        // 并发初始化冲突：行已由并发方插入，按已有行继续
                        NopSysSequence raced = findBySeqName(session, seqName);
                        if (raced == null) {
                            throw e;
                        }
                    }
                    return null;
                }));
    }

    private NopSysSequence findBySeqName(IOrmSession session, String seqName) {
        NopSysSequence example = new NopSysSequence();
        example.setSeqName(seqName);
        return (NopSysSequence) session.findFirstByExample(example);
    }

    /**
     * %0Nd 左补零，超长右截断回绕（镜像平台 DefaultCodeRule.generateSeq 语义）。
     */
    static String formatSeq(long seq, int count) {
        String str = String.valueOf(seq);
        if (str.length() < count) {
            return StringHelper.leftPad(str, count, '0');
        }
        return str.substring(str.length() - count);
    }

    static String monthlySeqName(int year, int month) {
        return SEQ_NAME_PREFIX + String.format("%04d%02d", year, month);
    }
}
