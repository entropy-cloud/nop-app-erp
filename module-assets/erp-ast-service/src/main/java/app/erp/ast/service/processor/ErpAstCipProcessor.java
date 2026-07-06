package app.erp.ast.service.processor;

import app.erp.ast.biz.IErpAstAssetCapitalizationBiz;
import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCapitalization;
import app.erp.ast.dao.entity.ErpAstCip;
import app.erp.ast.dao.entity.ErpAstCipCostItem;
import app.erp.ast.dao.entity.ErpAstCipProgressBilling;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.ErpAstErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.dao.IOrmEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * 在建工程（CIP）业务编排 Processor（Facade + protected step 模式，对齐项目既有 ErpAst*Processor 范式）。
 *
 * <p>承担：
 * <ul>
 *   <li>三态状态机（DRAFT → IN_CONSTRUCTION → TRANSFERRED）</li>
 *   <li>成本归集（{@code addCostItem}，累加 accumulatedCost；INTEREST_CAPITALIZATION config-gated）</li>
 *   <li>进度付款记录（{@code addProgressBilling}，不参与转固成本）</li>
 *   <li>完工转固（{@code transferToAsset}，全部 + 部分转固；复用 {@link ErpAstAssetCapitalizationProcessor}
 *       资本化审批链建卡 + 出 CAPITALIZATION(80) 凭证）</li>
 *   <li>红字冲销（{@code reverseTransfer}，委托 {@link IErpAstAssetCapitalizationBiz#reverseApprove}）</li>
 * </ul>
 *
 * <p>语义见 {@code docs/design/assets/cip.md}；状态字典 {@code erp-ast/cip-status}；
 * 成本类型字典 {@code erp-ast/cip-cost-type}。
 */
public class ErpAstCipProcessor {

    private static final int SCALE = 4;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    @Inject
    IDaoProvider daoProvider;

    @Inject
    ErpAstAssetCapitalizationProcessor capitalizationProcessor;

    // ==================== Phase 2: 状态机 + 成本归集 + 进度付款 ====================

    public ErpAstCip startConstruction(Long cipId, IServiceContext context) {
        ErpAstCip cip = requireCip(cipId, context);
        String current = cip.getStatus();
        if (!Objects.equals(current, ErpAstConstants.CIP_STATUS_DRAFT)) {
            throw illegalTransition(cip, current, ErpAstConstants.CIP_STATUS_IN_CONSTRUCTION);
        }
        validateCipInfoComplete(cip, context);
        cip.setStatus(ErpAstConstants.CIP_STATUS_IN_CONSTRUCTION);
        cipDao().saveOrUpdateEntity(cip);
        orm().flushSession();
        return cip;
    }

    public ErpAstCipCostItem addCostItem(Long cipId, String costType, BigDecimal amountFunctional,
                                          String sourceBillType, String sourceBillCode, String remark,
                                          IServiceContext context) {
        ErpAstCip cip = requireCip(cipId, context);
        requireInConstruction(cip);
        validateCostType(costType, cip);
        validateAmountPositive(amountFunctional, cip);
        if (Objects.equals(costType, ErpAstConstants.CIP_COST_TYPE_INTEREST_CAPITALIZATION)
                && !isInterestCapitalizationEnabled()) {
            throw new NopException(ErpAstErrors.ERR_CIP_INTEREST_CAPITALIZATION_DISABLED)
                    .param(ErpAstErrors.ARG_CIP_CODE, cip.getCode());
        }

        BigDecimal exchangeRate = nz(cip.getExchangeRate(), BigDecimal.ONE);
        BigDecimal amountSource = amountFunctional.divide(exchangeRate, SCALE, RoundingMode.HALF_UP);

        IEntityDao<ErpAstCipCostItem> dao = costItemDao();
        ErpAstCipCostItem item = dao.newEntity();
        item.setCipId(cip.getId());
        item.setOrgId(cip.getOrgId());
        item.setLineNo(nextCostItemLineNo(cip.getId()));
        item.setCostType(costType);
        item.setAmountFunctional(amountFunctional);
        item.setExchangeRate(exchangeRate);
        item.setAmountSource(amountSource);
        item.setCurrencyId(cip.getCurrencyId());
        item.setSourceBillType(sourceBillType);
        item.setSourceBillCode(sourceBillCode);
        item.setPostedTransferFlag(false);
        item.setBusinessDate(CoreMetrics.today());
        item.setRemark(remark);
        dao.saveEntity(item);

        cip.setAccumulatedCost(nz(cip.getAccumulatedCost(), BigDecimal.ZERO).add(amountFunctional));
        cipDao().saveOrUpdateEntity(cip);
        orm().flushSession();
        return item;
    }

    public ErpAstCipProgressBilling addProgressBilling(Long cipId, LocalDate billingDate, String billingMilestone,
                                                        BigDecimal amountFunctional, String paymentVoucherCode,
                                                        IServiceContext context) {
        ErpAstCip cip = requireCip(cipId, context);
        requireInConstruction(cip);
        validateAmountPositive(amountFunctional, cip);
        if (billingDate == null) {
            throw new NopException(ErpAstErrors.ERR_CIP_AMOUNT_INVALID)
                    .param(ErpAstErrors.ARG_CIP_CODE, cip.getCode())
                    .param(ErpAstErrors.ARG_AMOUNT, null);
        }

        BigDecimal exchangeRate = nz(cip.getExchangeRate(), BigDecimal.ONE);
        BigDecimal amountSource = amountFunctional.divide(exchangeRate, SCALE, RoundingMode.HALF_UP);

        IEntityDao<ErpAstCipProgressBilling> dao = progressBillingDao();
        ErpAstCipProgressBilling billing = dao.newEntity();
        billing.setCipId(cip.getId());
        billing.setOrgId(cip.getOrgId());
        billing.setLineNo(nextProgressBillingLineNo(cip.getId()));
        billing.setBillingDate(billingDate);
        billing.setBillingMilestone(billingMilestone);
        billing.setAmountFunctional(amountFunctional);
        billing.setExchangeRate(exchangeRate);
        billing.setAmountSource(amountSource);
        billing.setCurrencyId(cip.getCurrencyId());
        billing.setPaymentVoucherCode(paymentVoucherCode);
        billing.setPaidFlag(true);
        dao.saveEntity(billing);
        return billing;
    }

    public List<ErpAstCipCostItem> findCostItems(Long cipId, boolean onlyUntransferred, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("cipId", cipId));
        if (onlyUntransferred) {
            q.addFilter(eq("postedTransferFlag", false));
        }
        q.addOrderField("lineNo", false);
        return costItemDao().findAllByQuery(q);
    }

    public List<ErpAstCipProgressBilling> findProgressBillings(Long cipId, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("cipId", cipId));
        q.addOrderField("billingDate", false);
        return progressBillingDao().findAllByQuery(q);
    }

    // ==================== Phase 3: 完工转固（全部 + 部分） + reverseTransfer ====================

    /**
     * CIP 完工转固。多步编排走 protected step 方法（下游可逐 step 覆盖）。
     */
    public ErpAstCip transferToAsset(Long cipId, List<Long> costItemIds, LocalDate transferDate,
                                      IServiceContext context) {
        ErpAstCip cip = requireCip(cipId, context);
        List<ErpAstCipCostItem> costItems = resolveCostItems(cip, costItemIds);
        validateTransferable(cip, costItems, context);
        ErpAstAssetCapitalization cap = buildCapitalizationRequest(cip, costItems, transferDate, context);
        ErpAstAssetCapitalization approved = doTransfer(cap, cip, costItems, context);
        ErpAstCip managedCip = cipDao().getEntityById(cipId);
        postProcess(managedCip, costItems, approved, context);
        cipDao().saveOrUpdateEntity(managedCip);
        orm().flushSession();
        return managedCip;
    }

    /**
     * CIP 转固红字冲销：委托 {@link ErpAstAssetCapitalizationProcessor#reverseApprove} 红冲凭证 +
     * 资产卡片 status 回 DRAFT + 取消折旧计划 → 回退 CostItem.postedTransferFlag → CIP 状态回 IN_CONSTRUCTION。
     *
     * <p>本期仅支持全部红冲（部分红冲抛 {@link ErpAstErrors#ERR_CIP_PARTIAL_REVERSE_NOT_SUPPORTED}）。
     */
    public ErpAstCip reverseTransfer(Long cipId, Long capitalizationId, IServiceContext context) {
        ErpAstCip cip = requireCip(cipId, context);
        if (!Objects.equals(cip.getStatus(), ErpAstConstants.CIP_STATUS_TRANSFERRED)) {
            throw illegalTransition(cip, cip.getStatus(), ErpAstConstants.CIP_STATUS_IN_CONSTRUCTION);
        }

        List<ErpAstCipCostItem> capCostItems = findCostItemsByCapitalization(capitalizationId);
        List<ErpAstCipCostItem> allCipCostItems = findCostItems(cipId, false, context);
        if (capCostItems.size() < allCipCostItems.size()) {
            throw new NopException(ErpAstErrors.ERR_CIP_PARTIAL_REVERSE_NOT_SUPPORTED)
                    .param(ErpAstErrors.ARG_CIP_CODE, cip.getCode());
        }

        capitalizationProcessor.reverseApprove(String.valueOf(capitalizationId), context);

        IEntityDao<ErpAstCipCostItem> dao = costItemDao();
        for (ErpAstCipCostItem item : capCostItems) {
            ErpAstCipCostItem managed = dao.getEntityById(item.getId());
            managed.setPostedTransferFlag(false);
            managed.setCapitalizationId(null);
        }

        ErpAstCip managedCip = cipDao().getEntityById(cip.getId());
        managedCip.setStatus(ErpAstConstants.CIP_STATUS_IN_CONSTRUCTION);
        managedCip.setIsCompleted(false);
        managedCip.setCompletedAssetId(null);
        orm().flushSession();
        return managedCip;
    }

    // ---------- transferToAsset protected steps ----------

    /**
     * step 1：转固前置校验。CIP 须 IN_CONSTRUCTION；CostItem 非空且均未转固；
     * 配置不允许部分转固时必须选择全部。
     */
    protected void validateTransferable(ErpAstCip cip, List<ErpAstCipCostItem> costItems, IServiceContext context) {
        String current = cip.getStatus();
        if (Objects.equals(current, ErpAstConstants.CIP_STATUS_TRANSFERRED)
                && Boolean.TRUE.equals(cip.getIsCompleted())) {
            throw new NopException(ErpAstErrors.ERR_CIP_ALREADY_COMPLETED)
                    .param(ErpAstErrors.ARG_CIP_CODE, cip.getCode());
        }
        if (!Objects.equals(current, ErpAstConstants.CIP_STATUS_IN_CONSTRUCTION)) {
            throw new NopException(ErpAstErrors.ERR_CIP_NOT_IN_CONSTRUCTION)
                    .param(ErpAstErrors.ARG_CIP_CODE, cip.getCode())
                    .param(ErpAstErrors.ARG_CURRENT_STATUS, current);
        }
        if (costItems.isEmpty()) {
            throw new NopException(ErpAstErrors.ERR_CIP_NO_COST_TO_TRANSFER)
                    .param(ErpAstErrors.ARG_CIP_CODE, cip.getCode());
        }
        for (ErpAstCipCostItem item : costItems) {
            if (Boolean.TRUE.equals(item.getPostedTransferFlag())) {
                throw new NopException(ErpAstErrors.ERR_CIP_COST_ITEM_ALREADY_TRANSFERRED)
                        .param(ErpAstErrors.ARG_CIP_CODE, cip.getCode());
            }
        }
        if (!isPartialTransferAllowed()) {
            List<ErpAstCipCostItem> allUntransferred = findCostItems(cip.getId(), true, context);
            if (costItems.size() < allUntransferred.size()) {
                throw new NopException(ErpAstErrors.ERR_CIP_PARTIAL_TRANSFER_NOT_ALLOWED)
                        .param(ErpAstErrors.ARG_CIP_CODE, cip.getCode());
            }
        }
    }

    /**
     * step 2：构造资本化单（sourceType=CIP(20)，acquisitionCost=所选 CostItem amountFunctional 汇总）。
     */
    protected ErpAstAssetCapitalization buildCapitalizationRequest(ErpAstCip cip,
                                                                    List<ErpAstCipCostItem> costItems,
                                                                    LocalDate transferDate,
                                                                    IServiceContext context) {
        BigDecimal total = BigDecimal.ZERO;
        StringBuilder lineNos = new StringBuilder();
        for (ErpAstCipCostItem item : costItems) {
            total = total.add(nz(item.getAmountFunctional(), BigDecimal.ZERO));
            if (lineNos.length() > 0) {
                lineNos.append(",");
            }
            lineNos.append(item.getLineNo());
        }
        IEntityDao<ErpAstAssetCapitalization> dao = daoProvider.daoFor(ErpAstAssetCapitalization.class);
        ErpAstAssetCapitalization cap = dao.newEntity();
        cap.setCode(generateCapitalizationCode(cip));
        cap.setOrgId(cip.getOrgId());
        cap.setAssetCode(generateAssetCode(cip));
        cap.setAssetName(cip.getName());
        cap.setCategoryId(cip.getCategoryId());
        cap.setCurrencyId(cip.getCurrencyId());
        cap.setCapitalizationDate(transferDate != null ? transferDate : CoreMetrics.today());
        cap.setOriginalValue(total);
        cap.setSourceType(ErpAstConstants.SOURCE_TYPE_CIP);
        cap.setSourceCode(cip.getCode());
        cap.setExchangeRate(nz(cip.getExchangeRate(), BigDecimal.ONE));
        cap.setAmountFunctional(total);
        cap.setAmountSource(total.divide(nz(cip.getExchangeRate(), BigDecimal.ONE), SCALE, RoundingMode.HALF_UP));
        cap.setDocStatus(ErpAstConstants.DOC_STATUS_DRAFT);
        cap.setApproveStatus(ErpAstConstants.APPROVE_STATUS_UNSUBMITTED);
        cap.setBusinessDate(transferDate != null ? transferDate : CoreMetrics.today());
        cap.setRemark("CIP转固:行[" + lineNos + "]");
        dao.saveEntity(cap);
        orm().flushSession();
        return cap;
    }

    /**
     * step 3：调既有资本化审批链（DIRECT 模式：submit → approve 立即建卡 + 出 CAPITALIZATION 凭证）。
     * 复用 {@link ErpAstAssetCapitalizationProcessor} 1000-2 已审计管线，DRY。
     */
    protected ErpAstAssetCapitalization doTransfer(ErpAstAssetCapitalization cap, ErpAstCip cip,
                                                    List<ErpAstCipCostItem> costItems,
                                                    IServiceContext context) {
        String id = String.valueOf(cap.getId());
        capitalizationProcessor.submitForApproval(id, context);
        return capitalizationProcessor.approve(id, context);
    }

    /**
     * step 4：标记 CostItem 已转固；若全部转固 → CIP.status=TRANSFERRED + isCompleted=true + 回写 completedAssetId。
     */
    protected void postProcess(ErpAstCip cip, List<ErpAstCipCostItem> costItems,
                                ErpAstAssetCapitalization approvedCap, IServiceContext context) {
        IEntityDao<ErpAstCipCostItem> dao = costItemDao();
        for (ErpAstCipCostItem item : costItems) {
            ErpAstCipCostItem managed = dao.getEntityById(item.getId());
            managed.setPostedTransferFlag(true);
            managed.setCapitalizationId(approvedCap.getId());
        }
        orm().flushSession();

        ErpAstAsset asset = findAssetByCode(resolveCapitalizationAssetCode(approvedCap));
        if (asset != null) {
            cip.setCompletedAssetId(asset.getId());
        }
        List<ErpAstCipCostItem> remaining = findCostItems(cip.getId(), true, context);
        if (remaining.isEmpty()) {
            cip.setStatus(ErpAstConstants.CIP_STATUS_TRANSFERRED);
            cip.setIsCompleted(true);
        } else {
            cip.setStatus(ErpAstConstants.CIP_STATUS_IN_CONSTRUCTION);
            cip.setIsCompleted(false);
        }
    }

    // ---------- Phase 2 step 校验 ----------

    protected void requireInConstruction(ErpAstCip cip) {
        String current = cip.getStatus();
        if (!Objects.equals(current, ErpAstConstants.CIP_STATUS_IN_CONSTRUCTION)) {
            throw new NopException(ErpAstErrors.ERR_CIP_NOT_IN_CONSTRUCTION)
                    .param(ErpAstErrors.ARG_CIP_CODE, cip.getCode())
                    .param(ErpAstErrors.ARG_CURRENT_STATUS, current);
        }
    }

    protected void validateCipInfoComplete(ErpAstCip cip, IServiceContext context) {
        // businessDate mandatory on ORM; categoryId required for 转固（在 buildCapitalizationRequest 时校验更精准）
        // 这里只做最小校验，留扩展空间
    }

    protected void validateCostType(String costType, ErpAstCip cip) {
        if (costType == null
                || (!Objects.equals(costType, ErpAstConstants.CIP_COST_TYPE_PURCHASE)
                && !Objects.equals(costType, ErpAstConstants.CIP_COST_TYPE_SERVICE)
                && !Objects.equals(costType, ErpAstConstants.CIP_COST_TYPE_LABOR)
                && !Objects.equals(costType, ErpAstConstants.CIP_COST_TYPE_INTEREST_CAPITALIZATION)
                && !Objects.equals(costType, ErpAstConstants.CIP_COST_TYPE_OTHER))) {
            throw new NopException(ErpAstErrors.ERR_CIP_COST_TYPE_INVALID)
                    .param(ErpAstErrors.ARG_CIP_CODE, cip.getCode())
                    .param(ErpAstErrors.ARG_COST_TYPE, costType);
        }
    }

    protected void validateAmountPositive(BigDecimal amount, ErpAstCip cip) {
        if (amount == null || amount.signum() <= 0) {
            throw new NopException(ErpAstErrors.ERR_CIP_AMOUNT_INVALID)
                    .param(ErpAstErrors.ARG_CIP_CODE, cip.getCode())
                    .param(ErpAstErrors.ARG_AMOUNT, amount);
        }
    }

    protected boolean isInterestCapitalizationEnabled() {
        return AppConfig.var(ErpAstConstants.CONFIG_CIP_INTEREST_CAPITALIZATION_ENABLED, false);
    }

    protected boolean isPartialTransferAllowed() {
        return AppConfig.var(ErpAstConstants.CONFIG_CIP_PARTIAL_TRANSFER_ALLOWED, true);
    }

    // ---------- 查询/解析辅助 ----------

    protected ErpAstCip requireCip(Long cipId, IServiceContext context) {
        ErpAstCip cip = cipDao().getEntityById(cipId);
        if (cip == null) {
            throw new NopException(ErpAstErrors.ERR_CIP_NOT_FOUND)
                    .param(ErpAstErrors.ARG_CIP_ID, cipId);
        }
        return cip;
    }

    protected List<ErpAstCipCostItem> resolveCostItems(ErpAstCip cip, List<Long> costItemIds) {
        if (costItemIds == null || costItemIds.isEmpty()) {
            return findCostItems(cip.getId(), true, null);
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("cipId", cip.getId()));
        q.addFilter(in("id", costItemIds));
        q.addOrderField("lineNo", false);
        return costItemDao().findAllByQuery(q);
    }

    protected List<ErpAstCipCostItem> findCostItemsByCapitalization(Long capitalizationId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("capitalizationId", capitalizationId));
        return costItemDao().findAllByQuery(q);
    }

    protected ErpAstAsset findAssetByCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        List<ErpAstAsset> list = daoProvider.daoFor(ErpAstAsset.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    protected String resolveCapitalizationAssetCode(ErpAstAssetCapitalization cap) {
        if (cap.getAssetCode() != null && !cap.getAssetCode().trim().isEmpty()) {
            return cap.getAssetCode();
        }
        return "AST-" + cap.getId();
    }

    protected String generateCapitalizationCode(ErpAstCip cip) {
        return "CAP-CIP-" + cip.getCode() + "-" + CoreMetrics.currentTimeMillis();
    }

    protected String generateAssetCode(ErpAstCip cip) {
        return "AST-CIP-" + cip.getCode() + "-" + CoreMetrics.currentTimeMillis();
    }

    protected int nextCostItemLineNo(Long cipId) {
        List<ErpAstCipCostItem> items = findCostItems(cipId, false, null);
        int max = 0;
        for (ErpAstCipCostItem item : items) {
            if (item.getLineNo() != null && item.getLineNo() > max) {
                max = item.getLineNo();
            }
        }
        return max + 10;
    }

    protected int nextProgressBillingLineNo(Long cipId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("cipId", cipId));
        List<ErpAstCipProgressBilling> list = progressBillingDao().findAllByQuery(q);
        int max = 0;
        for (ErpAstCipProgressBilling b : list) {
            if (b.getLineNo() != null && b.getLineNo() > max) {
                max = b.getLineNo();
            }
        }
        return max + 10;
    }

    protected NopException illegalTransition(ErpAstCip cip, String current, String target) {
        return new NopException(ErpAstErrors.ERR_CIP_ILLEGAL_STATUS_TRANSITION)
                .param(ErpAstErrors.ARG_CIP_CODE, cip.getCode())
                .param(ErpAstErrors.ARG_CURRENT_STATUS, current)
                .param(ErpAstErrors.ARG_TARGET_STATUS, target);
    }

    // ---------- dao helpers ----------

    protected IEntityDao<ErpAstCip> cipDao() {
        return daoProvider.daoFor(ErpAstCip.class);
    }

    protected IEntityDao<ErpAstCipCostItem> costItemDao() {
        return daoProvider.daoFor(ErpAstCipCostItem.class);
    }

    protected IEntityDao<ErpAstCipProgressBilling> progressBillingDao() {
        return daoProvider.daoFor(ErpAstCipProgressBilling.class);
    }

    protected IOrmTemplate orm() {
        return ((IOrmEntityDao<?>) cipDao()).getOrmTemplate();
    }

    private static BigDecimal nz(BigDecimal v, BigDecimal fallback) {
        return v != null ? v : fallback;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
