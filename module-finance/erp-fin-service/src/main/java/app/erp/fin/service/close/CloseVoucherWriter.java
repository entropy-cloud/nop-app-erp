package app.erp.fin.service.close;

import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.commons.util.StringHelper;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import java.util.Objects;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 期末结账凭证写入器（损益结转 / 汇兑重估共用）。期末结账的凭证分录来自余额/辅助账聚合（非来源单据），
 * 不走 {@code IErpFinVoucherBiz.post} 的 Provider 模型（无 PERIOD_CLOSE/FX Provider 且 post 会触发 ArApItem 生成），
 * 故直接持久化 ErpFinVoucher + Lines + BillR，结构与过账引擎产出的凭证一致（供 {@code voucherBiz.reverse} 红冲）。
 *
 * <p>本类为静态工具（非 Bean），由 ProfitLossClosingService / ExchangeRevaluationService 调用。
 */
public final class CloseVoucherWriter {

    public static final String POSTING_TYPE_NORMAL = "NORMAL";
    public static final String VOUCHER_TYPE_TRANSFER = "TRANSFER";

    private CloseVoucherWriter() {
    }

    /** 结账凭证分录（借/贷 + 科目 + 金额 + 辅助维度）。 */
    public static final class Line {
        public final Long subjectId;
        public final String subjectCode;
        public final String subjectName;
        public final String dcDirection;
        public final BigDecimal amount;
        public final Long partnerId;

        public Line(Long subjectId, String subjectCode, String subjectName, String dcDirection,
                    BigDecimal amount, Long partnerId) {
            this.subjectId = subjectId;
            this.subjectCode = subjectCode;
            this.subjectName = subjectName;
            this.dcDirection = dcDirection;
            this.amount = amount == null ? BigDecimal.ZERO : amount;
            this.partnerId = partnerId;
        }
    }

    /**
     * 持久化一张期末结账凭证（含分录 + 业财回链），返回凭证 ID。
     *
     * @param codePrefix      凭证号前缀
     * @param billHeadCode    业财回链 billHeadCode（反结账按此反查冲销）
     * @param businessTypeCode 业财类型数值（ErpFinBusinessType.getCode）
     * @param businessTypeName 业财类型名（ErpFinBusinessType.name，写入 BillR.billType）
     */
    public static Long writeVoucher(IDaoProvider daoProvider, String codePrefix, String billHeadCode,
                                    String businessTypeCode, String businessTypeName,
                                    Long orgId, Long acctSchemaId, Long periodId, Long currencyId,
                                    BigDecimal exchangeRate, LocalDate voucherDate,
                                    List<Line> lines, String memo) {
        if (lines == null || lines.isEmpty()) {
            return null;
        }
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (Line l : lines) {
            if (Objects.equals(l.dcDirection, ErpFinConstants.DC_CREDIT)) {
                totalCredit = totalCredit.add(l.amount);
            } else {
                totalDebit = totalDebit.add(l.amount);
            }
        }
        if (totalDebit.compareTo(BigDecimal.ZERO) == 0 && totalCredit.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new NopException(io.nop.api.core.exceptions.ErrorCode.define(
                    "erp.err.fin.period-close.unbalanced", "期末结账凭证借贷不平衡：借={td}, 贷={tc}"))
                    .param("td", totalDebit.toPlainString()).param("tc", totalCredit.toPlainString());
        }

        IEntityDao<ErpFinVoucher> voucherDao = daoProvider.daoFor(ErpFinVoucher.class);
        IEntityDao<ErpFinVoucherLine> lineDao = daoProvider.daoFor(ErpFinVoucherLine.class);
        IEntityDao<ErpFinVoucherBillR> billRDao = daoProvider.daoFor(ErpFinVoucherBillR.class);

        ErpFinVoucher voucher = voucherDao.newEntity();
        voucher.setCode(codePrefix + "-" + StringHelper.generateUUID().substring(0, 12));
        voucher.setVoucherType(VOUCHER_TYPE_TRANSFER);
        voucher.setPostingType(POSTING_TYPE_NORMAL);
        voucher.setVoucherDate(voucherDate);
        voucher.setOrgId(orgId);
        voucher.setAcctSchemaId(acctSchemaId);
        voucher.setPeriodId(periodId);
        voucher.setTotalDebit(totalDebit);
        voucher.setTotalCredit(totalCredit);
        voucher.setIsReversed(false);
        voucher.setDocStatus(ErpFinConstants.VOUCHER_STATUS_POSTED);
        voucher.setPostedAt(CoreMetrics.currentTimestamp());
        voucherDao.saveEntity(voucher);
        Long voucherId = voucher.getId();

        BigDecimal rate = exchangeRate != null ? exchangeRate : BigDecimal.ONE;
        int lineNo = 1;
        for (Line l : lines) {
            ErpFinVoucherLine line = lineDao.newEntity();
            line.setVoucherId(voucherId);
            line.setLineNo(lineNo++);
            line.setSubjectId(l.subjectId);
            line.setSubjectCode(l.subjectCode);
            line.setSubjectName(l.subjectName);
            line.setDcDirection(l.dcDirection);
            boolean isCredit = Objects.equals(l.dcDirection, ErpFinConstants.DC_CREDIT);
            line.setDebitAmount(isCredit ? BigDecimal.ZERO : l.amount);
            line.setCreditAmount(isCredit ? l.amount : BigDecimal.ZERO);
            line.setCurrencyId(currencyId);
            line.setExchangeRate(rate);
            line.setAmountSource(l.amount);
            line.setAmountFunctional(l.amount);
            line.setAcctSchemaId(acctSchemaId);
            line.setBusinessType(businessTypeCode);
            line.setMemo(memo);
            line.setPartnerId(l.partnerId);
            lineDao.saveEntity(line);
        }

        ErpFinVoucherBillR billR = billRDao.newEntity();
        billR.setVoucherId(voucherId);
        billR.setBillType(businessTypeName);
        billR.setBillCode(billHeadCode);
        billR.setBusinessType(businessTypeCode);
        billRDao.saveEntity(billR);

        return voucherId;
    }
}
