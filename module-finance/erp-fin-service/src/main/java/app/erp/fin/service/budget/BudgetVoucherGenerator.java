package app.erp.fin.service.budget;

import app.erp.fin.dao.entity.ErpFinBudgetLine;
import app.erp.fin.dao.entity.ErpFinBudgetScenario;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.time.CoreMetrics;
import io.nop.commons.util.StringHelper;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 预算影子凭证生成器（{@code budget.md §设计范式 §业务规则1}）。预算作为 {@code postingType=BUDGET} 的影子凭证
 * 与实际凭证并行入账，复用凭证结构（Voucher + VoucherLine + VoucherBillR），但不走 Provider 模型（无 ArApItem 生成）。
 *
 * <p><b>借贷规则</b>：按 {@link ErpMdSubject#getDirection()} 自动取（资产/费用类=DEBIT 借方，负债/收入类=CREDIT 贷方）。
 *
 * <p><b>期间粒度</b>：{@link ErpFinVoucherLine} 无独立 periodId（继承凭证头），而预算控制按期间匹配，
 * 故按预算行的 {@code periodId} 分组——每个期间一张 BUDGET 凭证。无 periodId 的预算行不生成凭证（无法参与期间控制）。
 *
 * <p><b>余额来源</b>：预算余额/实际余额统一从 {@link ErpFinVoucherLine} 按关联凭证 {@code postingType} 聚合，
 * 不写 {@code ErpFinGlBalance}（过账引擎本就不维护 GlBalance）。
 *
 * <p>本类为 Bean（需 {@link IDaoProvider}），由 {@code ErpFinBudgetScenarioProcessor} 在 approve/cancel 事务内调用。
 */
public class BudgetVoucherGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(BudgetVoucherGenerator.class);

    public static final String VOUCHER_TYPE_TRANSFER = "TRANSFER";

    @Inject
    IDaoProvider daoProvider;

    /**
     * 审核通过时生成 BUDGET 影子凭证。按 periodId 分组，每组一张凭证。返回生成的凭证 ID 列表
     * （空列表表示无可用预算行）。凭证业财回链 {@code billCode=scenario.code}，便于作废时反查全部预算凭证。
     */
    public List<Long> generate(ErpFinBudgetScenario scenario) {
        List<ErpFinBudgetLine> lines = loadBudgetLines(scenario.getId());
        if (lines.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, List<ErpFinBudgetLine>> byPeriod = new LinkedHashMap<>();
        for (ErpFinBudgetLine l : lines) {
            if (l.getPeriodId() == null) {
                continue;
            }
            byPeriod.computeIfAbsent(l.getPeriodId(), k -> new ArrayList<>()).add(l);
        }
        List<Long> voucherIds = new ArrayList<>();
        for (Map.Entry<Long, List<ErpFinBudgetLine>> e : byPeriod.entrySet()) {
            Long voucherId = writeBudgetVoucher(scenario, e.getKey(), e.getValue(), false, null);
            if (voucherId != null) {
                voucherIds.add(voucherId);
            }
        }
        return voucherIds;
    }

    /**
     * 作废时红冲全部 BUDGET 凭证。按 {@code billCode=scenario.code} 反查所有预算凭证，逐张生成红字冲销凭证
     * （postingType=BUDGET，isReversed=true，金额取反），并将原凭证标记 {@code isReversed=true}。
     */
    public List<Long> reverse(ErpFinBudgetScenario scenario) {
        List<ErpFinVoucher> originals = findBudgetVouchers(scenario.getCode());
        List<Long> reversalIds = new ArrayList<>();
        for (ErpFinVoucher original : originals) {
            if (Boolean.TRUE.equals(original.getIsReversed())) {
                continue;
            }
            List<ErpFinVoucherLine> origLines = loadVoucherLines(original.getId());
            Long reversalId = writeBudgetVoucher(scenario, original.getPeriodId(), origLines, true, original.getId());
            if (reversalId != null) {
                original.setIsReversed(true);
                daoProvider.daoFor(ErpFinVoucher.class).updateEntity(original);
                reversalIds.add(reversalId);
            }
        }
        return reversalIds;
    }

    private Long writeBudgetVoucher(ErpFinBudgetScenario scenario, Long periodId, List<?> rawLines,
                                    boolean isReversal, Long reversalOfVoucherId) {
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        List<VoucherFact> facts = new ArrayList<>(rawLines.size());
        Map<Long, ErpMdSubject> subjectCache = new HashMap<>();
        for (Object raw : rawLines) {
            VoucherFact f = toFact(raw, subjectCache, isReversal);
            if (f == null) {
                continue;
            }
            facts.add(f);
            if (ErpFinConstants.DC_CREDIT.equals(f.dcDirection)) {
                totalCredit = totalCredit.add(f.amount);
            } else {
                totalDebit = totalDebit.add(f.amount);
            }
        }
        if (facts.isEmpty() || (totalDebit.signum() == 0 && totalCredit.signum() == 0)) {
            return null;
        }

        IEntityDao<ErpFinVoucher> voucherDao = daoProvider.daoFor(ErpFinVoucher.class);
        IEntityDao<ErpFinVoucherLine> lineDao = daoProvider.daoFor(ErpFinVoucherLine.class);
        IEntityDao<ErpFinVoucherBillR> billRDao = daoProvider.daoFor(ErpFinVoucherBillR.class);

        ErpFinVoucher voucher = voucherDao.newEntity();
        voucher.setCode(ErpFinConstants.BUDGET_VOUCHER_BILL_CODE_PREFIX + StringHelper.generateUUID().substring(0, 12));
        voucher.setVoucherType(VOUCHER_TYPE_TRANSFER);
        voucher.setPostingType(ErpFinConstants.POSTING_TYPE_BUDGET);
        voucher.setVoucherDate(resolveVoucherDate(scenario));
        voucher.setOrgId(scenario.getOrgId());
        voucher.setAcctSchemaId(scenario.getAcctSchemaId());
        voucher.setPeriodId(periodId);
        voucher.setTotalDebit(totalDebit);
        voucher.setTotalCredit(totalCredit);
        voucher.setIsReversed(isReversal);
        if (reversalOfVoucherId != null) {
            voucher.setReversalOfVoucherId(reversalOfVoucherId);
        }
        voucher.setDocStatus(ErpFinConstants.VOUCHER_STATUS_POSTED);
        voucher.setPostedAt(CoreMetrics.currentTimestamp());
        voucherDao.saveEntity(voucher);
        Long voucherId = voucher.getId();

        int lineNo = 1;
        for (VoucherFact f : facts) {
            ErpFinVoucherLine line = lineDao.newEntity();
            line.setVoucherId(voucherId);
            line.setLineNo(lineNo++);
            line.setSubjectId(f.subjectId);
            line.setSubjectCode(f.subjectCode);
            line.setSubjectName(f.subjectName);
            line.setDcDirection(f.dcDirection);
            boolean isCredit = ErpFinConstants.DC_CREDIT.equals(f.dcDirection);
            line.setDebitAmount(isCredit ? BigDecimal.ZERO : f.amount);
            line.setCreditAmount(isCredit ? f.amount : BigDecimal.ZERO);
            line.setCurrencyId(scenario.getCurrencyId());
            BigDecimal rate = scenario.getExchangeRate() != null ? scenario.getExchangeRate() : BigDecimal.ONE;
            line.setExchangeRate(rate);
            line.setAmountSource(f.amount);
            line.setAmountFunctional(f.amount);
            line.setAcctSchemaId(scenario.getAcctSchemaId());
            line.setOrgId(scenario.getOrgId());
            line.setBusinessType(ErpFinConstants.BUDGET_VOUCHER_BILL_TYPE);
            line.setMemo(isReversal ? "预算作废红冲" : "预算编制");
            line.setPartnerId(f.partnerId);
            line.setDepartmentId(f.departmentId);
            line.setProjectId(f.projectId);
            line.setWarehouseId(f.warehouseId);
            line.setMaterialId(f.materialId);
            line.setCostCenterId(f.costCenterId);
            lineDao.saveEntity(line);
        }

        ErpFinVoucherBillR billR = billRDao.newEntity();
        billR.setVoucherId(voucherId);
        billR.setBillType(ErpFinConstants.BUDGET_VOUCHER_BILL_TYPE);
        billR.setBillCode(scenario.getCode());
        billR.setBusinessType(ErpFinConstants.BUDGET_VOUCHER_BILL_TYPE);
        billRDao.saveEntity(billR);

        return voucherId;
    }

    private VoucherFact toFact(Object raw, Map<Long, ErpMdSubject> subjectCache, boolean isReversal) {
        if (raw instanceof ErpFinBudgetLine) {
            ErpFinBudgetLine l = (ErpFinBudgetLine) raw;
            ErpMdSubject subject = subjectCache.computeIfAbsent(l.getSubjectId(), this::loadSubject);
            if (subject == null || l.getBudgetAmountFunctional() == null) {
                return null;
            }
            BigDecimal amount = isReversal ? l.getBudgetAmountFunctional() : l.getBudgetAmountFunctional();
            return new VoucherFact(subject.getId(), subject.getCode(), subject.getName(),
                    resolveDcDirection(subject), amount, l.getCostCenterId(), l.getProjectId(),
                    l.getPartnerId(), l.getDepartmentId(), l.getWarehouseId(), l.getMaterialId());
        } else if (raw instanceof ErpFinVoucherLine) {
            ErpFinVoucherLine ol = (ErpFinVoucherLine) raw;
            BigDecimal debit = ol.getDebitAmount() != null ? ol.getDebitAmount() : BigDecimal.ZERO;
            BigDecimal credit = ol.getCreditAmount() != null ? ol.getCreditAmount() : BigDecimal.ZERO;
            String dc = ol.getDcDirection();
            BigDecimal amount = ErpFinConstants.DC_CREDIT.equals(dc) ? credit : debit;
            return new VoucherFact(ol.getSubjectId(), ol.getSubjectCode(), ol.getSubjectName(),
                    dc, amount.negate(), ol.getCostCenterId(), ol.getProjectId(), ol.getPartnerId(),
                    ol.getDepartmentId(), ol.getWarehouseId(), ol.getMaterialId());
        }
        return null;
    }

    /** 资产/费用类（DEBIT 余额方向）记借方；负债/收入类（CREDIT 余额方向）记贷方。 */
    private String resolveDcDirection(ErpMdSubject subject) {
        return ErpFinConstants.DC_CREDIT.equals(subject.getDirection())
                ? ErpFinConstants.DC_CREDIT : ErpFinConstants.DC_DEBIT;
    }

    private LocalDate resolveVoucherDate(ErpFinBudgetScenario scenario) {
        if (scenario.getValidFrom() != null) {
            return scenario.getValidFrom();
        }
        return CoreMetrics.today();
    }

    private ErpMdSubject loadSubject(Long id) {
        return daoProvider.daoFor(ErpMdSubject.class).getEntityById(id);
    }

    private List<ErpFinBudgetLine> loadBudgetLines(Long scenarioId) {
        IEntityDao<ErpFinBudgetLine> dao = daoProvider.daoFor(ErpFinBudgetLine.class);
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("scenarioId", scenarioId));
        return dao.findAllByQuery(q);
    }

    private List<ErpFinVoucherLine> loadVoucherLines(Long voucherId) {
        IEntityDao<ErpFinVoucherLine> dao = daoProvider.daoFor(ErpFinVoucherLine.class);
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("voucherId", voucherId));
        return dao.findAllByQuery(q);
    }

    private List<ErpFinVoucher> findBudgetVouchers(String scenarioCode) {
        IEntityDao<ErpFinVoucherBillR> billRDao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        io.nop.api.core.beans.query.QueryBean bq = new io.nop.api.core.beans.query.QueryBean();
        bq.addFilter(io.nop.api.core.beans.FilterBeans.eq("billCode", scenarioCode));
        bq.addFilter(io.nop.api.core.beans.FilterBeans.eq("billType", ErpFinConstants.BUDGET_VOUCHER_BILL_TYPE));
        List<ErpFinVoucherBillR> links = billRDao.findAllByQuery(bq);
        if (links.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> voucherIds = new ArrayList<>(links.size());
        for (ErpFinVoucherBillR link : links) {
            voucherIds.add(link.getVoucherId());
        }
        IEntityDao<ErpFinVoucher> voucherDao = daoProvider.daoFor(ErpFinVoucher.class);
        io.nop.api.core.beans.query.QueryBean vq = new io.nop.api.core.beans.query.QueryBean();
        vq.addFilter(io.nop.api.core.beans.FilterBeans.in("id", voucherIds));
        vq.addFilter(io.nop.api.core.beans.FilterBeans.eq("postingType", ErpFinConstants.POSTING_TYPE_BUDGET));
        return voucherDao.findAllByQuery(vq);
    }

    private static final class VoucherFact {
        final Long subjectId;
        final String subjectCode;
        final String subjectName;
        final String dcDirection;
        final BigDecimal amount;
        final Long costCenterId;
        final Long projectId;
        final Long partnerId;
        final Long departmentId;
        final Long warehouseId;
        final Long materialId;

        VoucherFact(Long subjectId, String subjectCode, String subjectName, String dcDirection,
                    BigDecimal amount, Long costCenterId, Long projectId, Long partnerId,
                    Long departmentId, Long warehouseId, Long materialId) {
            this.subjectId = subjectId;
            this.subjectCode = subjectCode;
            this.subjectName = subjectName;
            this.dcDirection = dcDirection;
            this.amount = amount == null ? BigDecimal.ZERO : amount;
            this.costCenterId = costCenterId;
            this.projectId = projectId;
            this.partnerId = partnerId;
            this.departmentId = departmentId;
            this.warehouseId = warehouseId;
            this.materialId = materialId;
        }
    }
}
