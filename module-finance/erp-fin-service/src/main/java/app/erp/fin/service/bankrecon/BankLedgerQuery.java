package app.erp.fin.service.bankrecon;

import app.erp.fin.dao.entity.ErpFinFundAccount;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.le;

/**
 * 账面流水按需查询器（替代设计假设的 {@code ErpFinBankLedgerLine} 物化视图，见计划 D1）。
 *
 * <p>查询命中资金账户科目（{@code FundAccount.subjectId}）的已过账 {@link ErpFinVoucherLine}，
 * 经 {@code voucherId} 关联头表 {@link ErpFinVoucher}，仅取 {@code voucherDate} 落在
 * {@code [txnDate − daysWindow, txnDate + daysWindow]} 内、{@code docStatus=POSTED} 且未红冲的分录。
 *
 * <p>方向语义：银行 {@code DEBIT}(扣款/流出) ↔ 账面 {@code CREDIT}(贷方/资金流出)；调用方传入反向。
 */
public class BankLedgerQuery {

    @Inject
    IDaoProvider daoProvider;

    public List<ErpFinVoucherLine> findCandidates(ErpFinFundAccount fundAccount, BigDecimal amount,
                                                    String oppositeDirection, LocalDate txnDate, int daysWindow) {
        if (fundAccount == null || fundAccount.getSubjectId() == null
                || amount == null || txnDate == null || oppositeDirection == null) {
            return new ArrayList<>();
        }
        LocalDate from = txnDate.minusDays(Math.max(0, daysWindow));
        LocalDate to = txnDate.plusDays(Math.max(0, daysWindow));

        // 两步查询：先按日期窗口取出已过账且未红冲的凭证 ID，再按科目+方向+金额取出对应分录。
        // VoucherLine 无日期列，必须经 voucherId 关联头表过滤（见计划 D1 + S1 修订）。
        List<Long> voucherIds = findVoucherIdsInWindow(from, to);
        if (voucherIds.isEmpty()) {
            return new ArrayList<>();
        }

        IEntityDao<ErpFinVoucherLine> lineDao = daoProvider.daoFor(ErpFinVoucherLine.class);
        // 分批 in 查询，避免 SQL IN 列表过长（H2/通用实践阈值 500）。
        List<ErpFinVoucherLine> result = new ArrayList<>();
        int batchSize = 500;
        for (int start = 0; start < voucherIds.size(); start += batchSize) {
            int end = Math.min(start + batchSize, voucherIds.size());
            List<Long> chunk = voucherIds.subList(start, end);

            QueryBean q = new QueryBean();
            q.addFilter(eq("subjectId", fundAccount.getSubjectId()));
            q.addFilter(eq("dcDirection", oppositeDirection));
            q.addFilter(in("voucherId", chunk));
            if (ErpFinConstants.DC_DEBIT.equals(oppositeDirection)) {
                q.addFilter(eq("debitAmount", amount));
            } else {
                q.addFilter(eq("creditAmount", amount));
            }
            result.addAll(lineDao.findAllByQuery(q));
        }

        // 排除已被其他银行流水行勾对占用的分录：单次匹配应使用未占用分录。
        Set<Long> occupied = findOccupiedLineIds(fundAccount.getId());
        List<ErpFinVoucherLine> filtered = new ArrayList<>(result.size());
        for (ErpFinVoucherLine line : result) {
            if (!occupied.contains(line.getId())) {
                filtered.add(line);
            }
        }
        return filtered;
    }

    /** 取日期窗口内已过账（POSTED）且未红冲的凭证 ID。 */
    protected List<Long> findVoucherIdsInWindow(LocalDate from, LocalDate to) {
        IEntityDao<ErpFinVoucher> dao = daoProvider.daoFor(ErpFinVoucher.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("docStatus", ErpFinConstants.VOUCHER_STATUS_POSTED));
        q.addFilter(ge("voucherDate", from));
        q.addFilter(le("voucherDate", to));
        List<ErpFinVoucher> vouchers = dao.findAllByQuery(q);
        List<Long> ids = new ArrayList<>(vouchers.size());
        for (ErpFinVoucher v : vouchers) {
            if (Boolean.TRUE.equals(v.getIsReversed())) {
                continue;
            }
            ids.add(v.getId());
        }
        return ids;
    }

    /** 查询已被勾对的银行流水行（matchStatus MATCHED/MANUAL_MATCHED，且 matchedLineId 非空）所占用的凭证行 ID。 */
    protected Set<Long> findOccupiedLineIds(Long fundAccountId) {
        IEntityDao<app.erp.fin.dao.entity.ErpFinBankStatementLine> dao =
                daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinBankStatementLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("statement.fundAccountId", fundAccountId));
        List<app.erp.fin.dao.entity.ErpFinBankStatementLine> matched = dao.findAllByQuery(q);
        Set<Long> ids = new HashSet<>();
        for (app.erp.fin.dao.entity.ErpFinBankStatementLine l : matched) {
            if (l.getMatchedLineId() == null) {
                continue;
            }
            String status = l.getMatchStatus();
            if (ErpFinConstants.BANK_MATCH_MATCHED.equals(status)
                    || ErpFinConstants.BANK_MATCH_MANUAL_MATCHED.equals(status)) {
                ids.add(l.getMatchedLineId());
            }
        }
        return ids;
    }

    public int resolveDaysWindow() {
        Integer days = AppConfig.var(ErpFinConstants.CONFIG_BANK_MATCH_TOLERANCE_DAYS,
                ErpFinConstants.DEFAULT_BANK_MATCH_TOLERANCE_DAYS);
        if (days == null || days < 0) {
            return ErpFinConstants.DEFAULT_BANK_MATCH_TOLERANCE_DAYS;
        }
        return days;
    }
}
