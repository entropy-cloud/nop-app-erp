package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinIntercompanyMatch;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * ErpFinIntercompanyMatch runMatching per-mutation Processor（R6.1，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含公司间自动配对编排（{@code multi-company.md §公司间自动配对算法 §Decision C}）：按 pairKey 扫描
 * 跨公司 INTERCOMPANY_SALE/PURCHASE 凭证对，写配对记录（含 P1-MA2-098 幂等去重）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpFinIntercompanyMatchRunMatchingProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErpFinIntercompanyMatchRunMatchingProcessor.class);

    @Inject
    IDaoProvider daoProvider;

    public int runMatching(Long periodId, IServiceContext context) {
        if (periodId == null) {
            return 0;
        }
        assertPeriodOpen(periodId);

        // 经 ErpFinVoucherBillR 反查 INTERCOMPANY_SALE/PURCHASE 凭证，按 billCode（调拨/订单 code）配对
        Map<String, List<Long>> saleByBillCode = findIntercompanyVoucherIdsByBillCode(
                ErpFinConstants.INTERCOMPANY_SALE_BILL_TYPE, periodId);
        Map<String, List<Long>> purchaseByBillCode = findIntercompanyVoucherIdsByBillCode(
                ErpFinConstants.INTERCOMPANY_PURCHASE_BILL_TYPE, periodId);

        // 预加载所有相关凭证的 orgId（批量，避免逐 billCode 重复查）
        java.util.Set<Long> allVoucherIds = new java.util.HashSet<>();
        saleByBillCode.values().forEach(allVoucherIds::addAll);
        purchaseByBillCode.values().forEach(allVoucherIds::addAll);
        Map<Long, Long> voucherOrgMap = loadVoucherOrgMap(allVoucherIds);

        int count = 0;
        IEntityDao<ErpFinIntercompanyMatch> matchDao = daoProvider.daoFor(ErpFinIntercompanyMatch.class);
        java.util.Set<String> allBillCodes = new java.util.HashSet<>();
        allBillCodes.addAll(saleByBillCode.keySet());
        allBillCodes.addAll(purchaseByBillCode.keySet());

        java.util.Set<String> existingPairKeys = findExistingPairKeys(periodId, allBillCodes);

        for (String billCode : allBillCodes) {
            // 幂等（P1-MA2-098 前置去重）：同期同 pairKey 已配对则 skip，避免重复 runMatching 产生重复 Match 行
            if (existingPairKeys.contains(billCode)) {
                continue;
            }

            List<Long> saleIds = saleByBillCode.getOrDefault(billCode, java.util.Collections.emptyList());
            List<Long> purchaseIds = purchaseByBillCode.getOrDefault(billCode, java.util.Collections.emptyList());
            BigDecimal saleAmt = sumVoucherAmounts(saleIds);
            BigDecimal purchaseAmt = sumVoucherAmounts(purchaseIds);
            BigDecimal matched = saleAmt.min(purchaseAmt);
            BigDecimal diff = saleAmt.subtract(purchaseAmt).abs();
            String status = diff.compareTo(new BigDecimal("0.01")) <= 0
                    ? ErpFinConstants.INTERCOMPANY_MATCH_MATCHED
                    : ErpFinConstants.INTERCOMPANY_MATCH_DIFF;

            // 审计列填充（P1-MA2-097）：AR 侧 = SALE 凭证，AP 侧 = PURCHASE 凭证
            Long arSideVoucherId = saleIds.isEmpty() ? null : saleIds.get(0);
            Long apSideVoucherId = purchaseIds.isEmpty() ? null : purchaseIds.get(0);
            Long arOrgId = arSideVoucherId != null ? voucherOrgMap.get(arSideVoucherId) : null;
            Long apOrgId = apSideVoucherId != null ? voucherOrgMap.get(apSideVoucherId) : null;
            Long materialId = resolveMaterialId(allVoucherIds);
            // 移除 hardcoded orgId=1L：配对记录归属 AR 侧组织（卖方），AR 侧缺失时回落 AP 侧
            Long recordOrgId = arOrgId != null ? arOrgId : (apOrgId != null ? apOrgId : null);

            ErpFinIntercompanyMatch record = matchDao.newEntity();
            record.setCode("MATCH-" + periodId + "-" + StringHelper.generateUUID().substring(0, 8));
            record.setOrgId(recordOrgId);
            record.setPairKey(billCode);
            record.setPeriodId(periodId);
            record.setArSideVoucherId(arSideVoucherId);
            record.setArOrgId(arOrgId);
            record.setApSideVoucherId(apSideVoucherId);
            record.setApOrgId(apOrgId);
            record.setMaterialId(materialId);
            record.setMatchedAmount(matched);
            record.setDiffAmount(diff);
            record.setStatus(status);
            matchDao.saveEntity(record);
            count++;
        }

        LOG.info("公司间配对完成：期间 {} 识别 {} 条配对记录（去重 {} 条既有 pairKey）",
                periodId, count, allBillCodes.size() - count);
        return count;
    }

    /** 同期同 pairKey 幂等去重：返回已存在 Match 记录的 pairKey 集合（P1-MA2-098）。 */
    protected java.util.Set<String> findExistingPairKeys(Long periodId, java.util.Set<String> pairKeys) {
        if (pairKeys.isEmpty()) {
            return java.util.Collections.emptySet();
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("periodId", periodId));
        q.addFilter(in("pairKey", pairKeys));
        java.util.Set<String> existing = new java.util.HashSet<>();
        for (ErpFinIntercompanyMatch m : daoProvider.daoFor(ErpFinIntercompanyMatch.class).findAllByQuery(q)) {
            if (m.getPairKey() != null) {
                existing.add(m.getPairKey());
            }
        }
        return existing;
    }

    /** 批量加载凭证 orgId（凭证的核算组织）。 */
    protected Map<Long, Long> loadVoucherOrgMap(java.util.Set<Long> voucherIds) {
        if (voucherIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        IEntityDao<ErpFinVoucher> voucherDao = daoProvider.daoFor(ErpFinVoucher.class);
        QueryBean q = new QueryBean();
        q.addFilter(in("id", voucherIds));
        Map<Long, Long> map = new HashMap<>();
        for (ErpFinVoucher v : voucherDao.findAllByQuery(q)) {
            map.put(v.getId(), v.getOrgId());
        }
        return map;
    }

    /** 从相关凭证行取首个非空 materialId（配对审计列，物料维度）。 */
    protected Long resolveMaterialId(java.util.Set<Long> voucherIds) {
        if (voucherIds.isEmpty()) {
            return null;
        }
        IEntityDao<ErpFinVoucherLine> lineDao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(in("voucherId", voucherIds));
        for (ErpFinVoucherLine l : lineDao.findAllByQuery(q)) {
            if (l.getMaterialId() != null) {
                return l.getMaterialId();
            }
        }
        return null;
    }

    /** 按 billType 反查凭证 ID，按 billCode 分组。 */
    protected Map<String, List<Long>> findIntercompanyVoucherIdsByBillCode(String billType, Long periodId) {
        IEntityDao<ErpFinVoucherBillR> billRDao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean billRQ = new QueryBean();
        billRQ.addFilter(eq("billType", billType));
        List<ErpFinVoucherBillR> billRs = billRDao.findAllByQuery(billRQ);

        Map<String, List<Long>> result = new HashMap<>();
        if (billRs.isEmpty()) {
            return result;
        }
        java.util.Set<Long> voucherIds = new java.util.HashSet<>();
        for (ErpFinVoucherBillR br : billRs) {
            voucherIds.add(br.getVoucherId());
        }
        // 仅取本期间内未红冲凭证
        IEntityDao<ErpFinVoucher> voucherDao = daoProvider.daoFor(ErpFinVoucher.class);
        QueryBean vq = new QueryBean();
        vq.addFilter(eq("periodId", periodId));
        vq.addFilter(in("id", voucherIds));
        List<ErpFinVoucher> vouchers = voucherDao.findAllByQuery(vq);
        java.util.Set<Long> validVoucherIds = new java.util.HashSet<>();
        for (ErpFinVoucher v : vouchers) {
            if (!Boolean.TRUE.equals(v.getIsReversed())) {
                validVoucherIds.add(v.getId());
            }
        }
        for (ErpFinVoucherBillR br : billRs) {
            if (validVoucherIds.contains(br.getVoucherId())) {
                result.computeIfAbsent(br.getBillCode(), k -> new java.util.ArrayList<>())
                        .add(br.getVoucherId());
            }
        }
        return result;
    }

    protected BigDecimal sumVoucherAmounts(List<Long> voucherIds) {
        if (voucherIds == null || voucherIds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        IEntityDao<ErpFinVoucher> voucherDao = daoProvider.daoFor(ErpFinVoucher.class);
        QueryBean q = new QueryBean();
        q.addFilter(in("id", new java.util.HashSet<>(voucherIds)));
        List<ErpFinVoucher> vouchers = voucherDao.findAllByQuery(q);
        BigDecimal total = BigDecimal.ZERO;
        for (ErpFinVoucher v : vouchers) {
            BigDecimal amt = v.getTotalDebit() != null ? v.getTotalDebit() : BigDecimal.ZERO;
            total = total.add(amt);
        }
        return total;
    }

    protected void assertPeriodOpen(Long periodId) {
        ErpFinAccountingPeriod period = daoProvider.daoFor(ErpFinAccountingPeriod.class).getEntityById(periodId);
        if (period == null) {
            throw new NopException(ErpFinErrors.ERR_PERIOD_NOT_FOUND).param(ErpFinErrors.ARG_PERIOD_ID, periodId);
        }
        String status = period.getStatus();
        if (ErpFinConstants.PERIOD_STATUS_CLOSED.equals(status)
                || ErpFinConstants.PERIOD_STATUS_CLOSED_FINAL.equals(status)) {
            throw new NopException(ErpFinErrors.ERR_INTERCOMPANY_MATCH_PERIOD_CLOSED)
                    .param(ErpFinErrors.ARG_PERIOD_ID, periodId);
        }
    }
}
