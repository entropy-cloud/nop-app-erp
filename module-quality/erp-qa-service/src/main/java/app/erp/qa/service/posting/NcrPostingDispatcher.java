package app.erp.qa.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.qa.dao.entity.ErpQaNonConformance;
import app.erp.qa.service.ErpQaConstants;
import app.erp.qa.service.ErpQaErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * NCR 过账派发器。NCR resolve（→RESOLVED）或人工 postNcr 时按 dispositionType 分派财务处理。
 *
 * <p>SCRAP 处置：构造 {@link PostingEvent}(businessType=NCR_SCRAP) → {@link NcrPostingExecutor#postEvent}
 * → {@link app.erp.fin.biz.IErpFinVoucherBiz#post}。金额 = NCR.quantity × 物料单位成本（ErpInvStockBalance.avgCost）。
 * 成功置 posted 三件套。
 *
 * <p>承接 {@code InvPostingDispatcher} 范式（plan 2026-07-05-2352-2）。
 *
 * <p><b>报废存货出库简化</b>：NCR_SCRAP 凭证贷记存货科目（1401）已表达报废消耗的会计影响。
 * 物理库存量（ErpInvStockBalance.totalQuantity）的同步扣减属 inventory 域 successor（避免与 InvPostingDispatcher
 * 的 SALES_OUTPUT 双计存货贷方——物理出库移动会触发额外存货估值凭证）。
 */
public class NcrPostingDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(NcrPostingDispatcher.class);

    @Inject
    NcrPostingExecutor executor;

    @Inject
    IDaoProvider daoProvider;

    public void setExecutor(NcrPostingExecutor executor) {
        this.executor = executor;
    }

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    /**
     * 分派 SCRAP 处置过账。金额 = quantity × 物料平均成本。
     * 成功置 posted 三件套并返回凭证 ID；幂等命中（已过账）返回 null。
     */
    public Long dispatchScrap(ErpQaNonConformance ncr, IServiceContext context) {
        BigDecimal quantity = ncr.getQuantity();
        if (quantity == null || quantity.signum() <= 0) {
            throw new NopException(ErpQaErrors.ERR_NCR_NO_QUANTITY).param(ErpQaErrors.ARG_NCR_CODE, ncr.getCode());
        }
        ErpInvStockBalance balance = resolveStockBalance(ncr.getMaterialId());
        BigDecimal unitCost = balance != null && balance.getAvgCost() != null ? balance.getAvgCost() : BigDecimal.ZERO;
        BigDecimal scrapAmount = quantity.multiply(unitCost);
        Long currencyId = balance != null ? balance.getCurrencyId() : null;
        Long warehouseId = balance != null ? balance.getWarehouseId() : null;
        Long orgId = balance != null ? balance.getOrgId() : null;

        PostingEvent event = buildScrapEvent(ncr, scrapAmount, currencyId, warehouseId, orgId);
        Long voucherId = executor.postEvent(event);
        if (voucherId != null) {
            ncr.setPosted(Boolean.TRUE);
            ncr.setPostedAt(CoreMetrics.currentDateTime());
            ncr.setPostedBy(resolveUserId(context));
        }
        return voucherId;
    }

    /**
     * 红冲 SCRAP 凭证。按 NCR.code 反查原已过账凭证，生成红字冲销。
     * 成功后清除 posted 三件套。
     */
    public Long reverseScrap(ErpQaNonConformance ncr) {
        Long voucherId = executor.reverse(ncr.getCode(), ErpFinBusinessType.NCR_SCRAP);
        ncr.setPosted(Boolean.FALSE);
        ncr.setPostedAt(null);
        ncr.setPostedBy(null);
        return voucherId;
    }

    private PostingEvent buildScrapEvent(ErpQaNonConformance ncr, BigDecimal scrapAmount, Long currencyId, Long warehouseId, Long orgId) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.NCR_SCRAP);
        event.setBillHeadCode(ncr.getCode());
        event.setOrgId(orgId);
        event.setAcctSchemaId(resolveAcctSchemaId());
        event.setCurrencyId(currencyId);
        event.setExchangeRate(BigDecimal.ONE);
        event.setVoucherDate(ncr.getNcrDate() != null ? ncr.getNcrDate() : CoreMetrics.today());

        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(NcrScrapAcctDocProvider.KEY_SCRAP_AMOUNT, scrapAmount);
        billData.put(NcrScrapAcctDocProvider.KEY_MATERIAL_ID, ncr.getMaterialId());
        billData.put(NcrScrapAcctDocProvider.KEY_WAREHOUSE_ID, warehouseId);
        event.setBillData(billData);
        return event;
    }

    private ErpInvStockBalance resolveStockBalance(Long materialId) {
        if (materialId == null) {
            return null;
        }
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        q.setLimit(1);
        List<ErpInvStockBalance> balances = dao.findAllByQuery(q);
        if (balances.isEmpty()) {
            LOG.warn("NCR 报废物料 {} 无库存余额记录，单位成本按 0 计（凭证金额为 0）", materialId);
            return null;
        }
        return balances.get(0);
    }

    private Long resolveAcctSchemaId() {
        String raw = AppConfig.var(ErpQaConstants.CONFIG_NCR_DEFAULT_ACCT_SCHEMA, "");
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String resolveUserId(IServiceContext context) {
        if (context == null) {
            return null;
        }
        try {
            return context.getUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
