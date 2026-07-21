
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinConsolidationEliminationBiz;
import app.erp.fin.dao.entity.ErpFinConsolidationElimination;
import app.erp.fin.dao.entity.ErpFinIntercompanyMatch;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 合并抵消候选识别 BizModel（plan 2026-07-22-1000-1 A3，multi-company.md §合并抵消范围）。
 *
 * <p>{@code generateEliminationCandidates(periodId)} 按 3 类（AR_AP/REVENUE_COST/INVENTORY_PROFIT）扫描配对候选。
 * {@code postElimination(candidateId)} 生成 DRAFT_VOUCHER 抵消分录草稿凭证。
 *
 * <p>config-gated {@code erp-fin.consolidation-elimination-enabled} 默认 false。
 *
 * <p>权威：{@code docs/architecture/multi-company.md §Decision D}。
 */
@BizModel("ErpFinConsolidationElimination")
public class ErpFinConsolidationEliminationBizModel extends CrudBizModel<ErpFinConsolidationElimination>
        implements IErpFinConsolidationEliminationBiz {

    private static final Logger LOG = LoggerFactory.getLogger(ErpFinConsolidationEliminationBizModel.class);

    public ErpFinConsolidationEliminationBizModel() {
        setEntityName(ErpFinConsolidationElimination.class.getName());
    }

    @Override
    @BizMutation
    public int generateEliminationCandidates(@Name("periodId") Long periodId, IServiceContext context) {
        if (!isEliminationEnabled()) {
            return 0;
        }
        if (periodId == null) {
            return 0;
        }

        // 扫描已配对记录（MATCHED）作为 AR_AP 抵消候选
        QueryBean matchQ = new QueryBean();
        matchQ.addFilter(eq("periodId", periodId));
        matchQ.addFilter(eq("status", ErpFinConstants.INTERCOMPANY_MATCH_MATCHED));
        List<ErpFinIntercompanyMatch> matches =
                daoProvider().daoFor(ErpFinIntercompanyMatch.class).findAllByQuery(matchQ);

        int count = 0;
        IEntityDao<ErpFinConsolidationElimination> elimDao =
                daoProvider().daoFor(ErpFinConsolidationElimination.class);

        for (ErpFinIntercompanyMatch m : matches) {
            ErpFinConsolidationElimination candidate = elimDao.newEntity();
            candidate.setCode("ELIM-" + periodId + "-" + StringHelper.generateUUID().substring(0, 8));
            candidate.setOrgId(1L);
            candidate.setEliminationType(ErpFinConstants.ELIMINATION_TYPE_AR_AP);
            candidate.setPeriodId(periodId);
            candidate.setPairKey(m.getPairKey());
            candidate.setMatchId(m.getId());
            candidate.setEliminationAmount(m.getMatchedAmount());
            candidate.setStatus(ErpFinConstants.ELIMINATION_STATUS_CANDIDATE);
            elimDao.saveEntity(candidate);
            count++;
        }

        // REVENUE_COST 抵消候选（简化：复用 MATCHED 记录金额作为收入/成本抵消额）
        for (ErpFinIntercompanyMatch m : matches) {
            ErpFinConsolidationElimination candidate = elimDao.newEntity();
            candidate.setCode("ELIM-RC-" + periodId + "-" + StringHelper.generateUUID().substring(0, 8));
            candidate.setOrgId(1L);
            candidate.setEliminationType(ErpFinConstants.ELIMINATION_TYPE_REVENUE_COST);
            candidate.setPeriodId(periodId);
            candidate.setPairKey(m.getPairKey());
            candidate.setMatchId(m.getId());
            candidate.setEliminationAmount(m.getMatchedAmount());
            candidate.setStatus(ErpFinConstants.ELIMINATION_STATUS_CANDIDATE);
            elimDao.saveEntity(candidate);
            count++;
        }

        // INVENTORY_PROFIT 试点（config-gated）
        if (isInventoryProfitEliminationEnabled()) {
            for (ErpFinIntercompanyMatch m : matches) {
                ErpFinConsolidationElimination candidate = elimDao.newEntity();
                candidate.setCode("ELIM-IP-" + periodId + "-" + StringHelper.generateUUID().substring(0, 8));
                candidate.setOrgId(1L);
                candidate.setEliminationType(ErpFinConstants.ELIMINATION_TYPE_INVENTORY_PROFIT);
                candidate.setPeriodId(periodId);
                candidate.setPairKey(m.getPairKey());
                candidate.setMatchId(m.getId());
                candidate.setEliminationAmount(m.getMatchedAmount());
                candidate.setStatus(ErpFinConstants.ELIMINATION_STATUS_CANDIDATE);
                elimDao.saveEntity(candidate);
                count++;
            }
        }

        if (count == 0) {
            throw new NopException(ErpFinErrors.ERR_ELIMINATION_NO_CANDIDATES)
                    .param(ErpFinErrors.ARG_PERIOD_ID, periodId);
        }

        LOG.info("抵消候选识别完成：期间 {} 识别 {} 条候选", periodId, count);
        return count;
    }

    @Override
    @BizMutation
    public Long postElimination(@Name("candidateId") Long candidateId, IServiceContext context) {
        if (!isEliminationEnabled()) {
            return null;
        }
        IEntityDao<ErpFinConsolidationElimination> elimDao =
                daoProvider().daoFor(ErpFinConsolidationElimination.class);
        ErpFinConsolidationElimination candidate = elimDao.getEntityById(candidateId);
        if (candidate == null) {
            throw new NopException(ErpFinErrors.ERR_ELIMINATION_ALREADY_POSTED)
                    .param(ErpFinErrors.ARG_ELIMINATION_ID, candidateId);
        }
        if (!ErpFinConstants.ELIMINATION_STATUS_CANDIDATE.equals(candidate.getStatus())) {
            throw new NopException(ErpFinErrors.ERR_ELIMINATION_ALREADY_POSTED)
                    .param(ErpFinErrors.ARG_ELIMINATION_ID, candidateId);
        }

        BigDecimal amount = candidate.getEliminationAmount() != null
                ? candidate.getEliminationAmount() : BigDecimal.ZERO;
        Long voucherId = writeDraftEliminationVoucher(candidate, amount);

        candidate.setDraftVoucherId(voucherId);
        candidate.setStatus(ErpFinConstants.ELIMINATION_STATUS_DRAFT_VOUCHER);
        elimDao.updateEntity(candidate);

        LOG.info("抵消分录草稿生成：候选 {} → 草稿凭证 {} 金额 {}", candidateId, voucherId, amount);
        return voucherId;
    }

    // ---------- 内部辅助 ----------

    private boolean isEliminationEnabled() {
        return Boolean.TRUE.equals(
                AppConfig.var(ErpFinConstants.CONFIG_CONSOLIDATION_ELIMINATION_ENABLED, Boolean.FALSE));
    }

    private boolean isInventoryProfitEliminationEnabled() {
        return Boolean.TRUE.equals(
                AppConfig.var(ErpFinConstants.CONFIG_ELIMINATION_INVENTORY_PROFIT_ENABLED, Boolean.FALSE));
    }

    private Long writeDraftEliminationVoucher(ErpFinConsolidationElimination candidate, BigDecimal amount) {
        IEntityDao<ErpFinVoucher> voucherDao = daoProvider().daoFor(ErpFinVoucher.class);
        IEntityDao<ErpFinVoucherLine> lineDao = daoProvider().daoFor(ErpFinVoucherLine.class);
        IEntityDao<ErpFinVoucherBillR> billRDao = daoProvider().daoFor(ErpFinVoucherBillR.class);

        ErpFinVoucher voucher = voucherDao.newEntity();
        voucher.setCode(ErpFinConstants.ELIMINATION_VOUCHER_BILL_CODE_PREFIX
                + StringHelper.generateUUID().substring(0, 12));
        voucher.setVoucherType("TRANSFER");
        voucher.setVoucherDate(CoreMetrics.today());
        voucher.setOrgId(candidate.getOrgId());
        voucher.setAcctSchemaId(1L);
        voucher.setPeriodId(candidate.getPeriodId());
        voucher.setTotalDebit(amount);
        voucher.setTotalCredit(amount);
        voucher.setIsReversed(false);
        voucher.setDocStatus(ErpFinConstants.VOUCHER_STATUS_DRAFT);
        voucherDao.saveEntity(voucher);
        Long voucherId = voucher.getId();

        // 借方行（抵消方向由 eliminationType 决定，简化为 Dr 抵消科目 / Cr 抵消对冲科目）
        String debitSubjectCode = resolveEliminationSubjectCode(candidate.getEliminationType(), true);
        app.erp.md.dao.entity.ErpMdSubject debitSubject = findSubjectByCode(debitSubjectCode);
        ErpFinVoucherLine debitLine = lineDao.newEntity();
        debitLine.setVoucherId(voucherId);
        debitLine.setLineNo(1);
        if (debitSubject != null) {
            debitLine.setSubjectId(debitSubject.getId());
        }
        debitLine.setSubjectCode(debitSubjectCode);
        debitLine.setSubjectName("抵消-" + candidate.getEliminationType());
        debitLine.setDcDirection(ErpFinConstants.DC_DEBIT);
        debitLine.setDebitAmount(amount);
        debitLine.setCreditAmount(BigDecimal.ZERO);
        debitLine.setCurrencyId(1L);
        debitLine.setExchangeRate(BigDecimal.ONE);
        debitLine.setAmountSource(amount);
        debitLine.setAmountFunctional(amount);
        debitLine.setAcctSchemaId(1L);
        debitLine.setOrgId(candidate.getOrgId());
        debitLine.setBusinessType(ErpFinConstants.ELIMINATION_VOUCHER_BILL_TYPE);
        debitLine.setMemo("合并抵消借方-" + candidate.getEliminationType());
        lineDao.saveEntity(debitLine);

        // 贷方行
        String creditSubjectCode = resolveEliminationSubjectCode(candidate.getEliminationType(), false);
        app.erp.md.dao.entity.ErpMdSubject creditSubject = findSubjectByCode(creditSubjectCode);
        ErpFinVoucherLine creditLine = lineDao.newEntity();
        creditLine.setVoucherId(voucherId);
        creditLine.setLineNo(2);
        if (creditSubject != null) {
            creditLine.setSubjectId(creditSubject.getId());
        }
        creditLine.setSubjectCode(creditSubjectCode);
        creditLine.setSubjectName("抵消对冲-" + candidate.getEliminationType());
        creditLine.setDcDirection(ErpFinConstants.DC_CREDIT);
        creditLine.setDebitAmount(BigDecimal.ZERO);
        creditLine.setCreditAmount(amount);
        creditLine.setCurrencyId(1L);
        creditLine.setExchangeRate(BigDecimal.ONE);
        creditLine.setAmountSource(amount);
        creditLine.setAmountFunctional(amount);
        creditLine.setAcctSchemaId(1L);
        creditLine.setOrgId(candidate.getOrgId());
        creditLine.setBusinessType(ErpFinConstants.ELIMINATION_VOUCHER_BILL_TYPE);
        creditLine.setMemo("合并抵消贷方-" + candidate.getEliminationType());
        lineDao.saveEntity(creditLine);

        // 业财回链
        ErpFinVoucherBillR billR = billRDao.newEntity();
        billR.setVoucherId(voucherId);
        billR.setBillType(ErpFinConstants.ELIMINATION_VOUCHER_BILL_TYPE);
        billR.setBillCode(candidate.getCode());
        billR.setBusinessType(ErpFinConstants.ELIMINATION_VOUCHER_BILL_TYPE);
        billRDao.saveEntity(billR);

        return voucherId;
    }

    private String resolveEliminationSubjectCode(String eliminationType, boolean debitSide) {
        // 简化：按抵消类型返回默认科目编码（真实经 GlMappingResolver 解析归 successor）
        if (ErpFinConstants.ELIMINATION_TYPE_AR_AP.equals(eliminationType)) {
            return debitSide ? "2202" : "1131";
        }
        if (ErpFinConstants.ELIMINATION_TYPE_REVENUE_COST.equals(eliminationType)) {
            return debitSide ? "5001" : "1401";
        }
        return debitSide ? "1401" : "5001";
    }

    private app.erp.md.dao.entity.ErpMdSubject findSubjectByCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        IEntityDao<app.erp.md.dao.entity.ErpMdSubject> dao =
                daoProvider().daoFor(app.erp.md.dao.entity.ErpMdSubject.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        List<app.erp.md.dao.entity.ErpMdSubject> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }
}
