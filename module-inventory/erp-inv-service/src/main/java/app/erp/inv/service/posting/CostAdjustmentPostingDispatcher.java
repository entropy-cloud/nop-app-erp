package app.erp.inv.service.posting;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.service.ErpFinConstants;
import app.erp.inv.dao.entity.ErpInvCostAdjust;
import app.erp.inv.dao.entity.ErpInvCostAdjustLine;
import app.erp.inv.service.ErpInvConstants;
import app.erp.md.dao.AcctSchemaResolver;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
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
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 成本调整过账派发器（plan 2026-07-05-2352-3）。
 *
 * <p>apply 末尾组装 {@link PostingEvent}(COST_ADJUSTMENT) 经 {@link IErpFinVoucherBiz} 调用财务过账引擎；
 * reverse 经 {@code reverse} 生成红字冲销凭证。billHeadCode = 调整单 code，作为幂等/红冲键。
 *
 * <p>失败语义对齐 {@link InvPostingDispatcher}/{@code ValueAdjustmentPostingDispatcher}：
 * 过账失败吞异常返回 null（保持 posted=false），不阻塞调整终态。
 *
 * <p>方向相关：Σ 行 adjustAmount 带符号 → INCREASE（>0，借存货/贷差异）/ DECREASE（<0，借差异/贷存货）；
 * 净额为 0 跳过过账。
 */
public class CostAdjustmentPostingDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(CostAdjustmentPostingDispatcher.class);

    private static final String POSTING_TYPE_NORMAL = ErpFinConstants.POSTING_TYPE_NORMAL;

    @Inject
    IErpFinVoucherBiz voucherBiz;

    @Inject
    IDaoProvider daoProvider;

    public Long tryPost(ErpInvCostAdjust adjust, List<ErpInvCostAdjustLine> lines, BigDecimal totalAdjustAmount) {
        if (totalAdjustAmount == null || totalAdjustAmount.signum() == 0) {
            return null;
        }
        PostingEvent event = buildEvent(adjust, lines, totalAdjustAmount);
        try {
            return postEvent(event);
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("成本调整过账失败，调整单 {} 保持 posted=false：{}", adjust.getCode(), e.getMessage());
            } else {
                LOG.error("成本调整过账异常，调整单 {} 保持 posted=false", adjust.getCode(), e);
            }
            return null;
        }
    }

    public void reverse(ErpInvCostAdjust adjust) {
        try {
            IServiceContext context = ctx();
            voucherBiz.reverse(adjust.getCode(), ErpFinBusinessType.COST_ADJUSTMENT, context);
            markOriginalVoucherReversed(adjust.getCode());
        } catch (Exception e) {
            if (e instanceof NopException) {
                LOG.warn("成本调整红字冲销失败，调整单 {}：{}", adjust.getCode(), e.getMessage());
            } else {
                LOG.error("成本调整红字冲销异常，调整单 {}", adjust.getCode(), e);
            }
            throw e;
        }
    }

    private Long postEvent(PostingEvent event) {
        return voucherBiz.post(event, ctx());
    }

    private PostingEvent buildEvent(ErpInvCostAdjust adjust, List<ErpInvCostAdjustLine> lines,
                                     BigDecimal totalAdjustAmount) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.COST_ADJUSTMENT);
        event.setBillHeadCode(adjust.getCode());
        event.setOrgId(adjust.getOrgId());
        event.setAcctSchemaId(resolveAcctSchemaId(adjust.getOrgId()));
        event.setCurrencyId(adjust.getCurrencyId());
        event.setExchangeRate(BigDecimal.ONE);
        LocalDate voucherDate = adjust.getBusinessDate() != null ? adjust.getBusinessDate() : CoreMetrics.today();
        event.setVoucherDate(voucherDate);

        String direction = totalAdjustAmount.signum() > 0
                ? ErpInvConstants.DIRECTION_INCREASE
                : ErpInvConstants.DIRECTION_DECREASE;
        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put(ErpInvConstants.BILL_DATA_ADJUST_AMOUNT, totalAdjustAmount.abs());
        billData.put(ErpInvConstants.BILL_DATA_ADJUST_DIRECTION, direction);
        billData.put(ErpInvConstants.BILL_DATA_ADJUST_TYPE, adjust.getAdjustType());
        if (!lines.isEmpty()) {
            billData.put("MATERIAL_ID", lines.get(0).getMaterialId());
            billData.put("WAREHOUSE_ID", lines.get(0).getWarehouseId());
        }
        event.setBillData(billData);
        return event;
    }

    private void markOriginalVoucherReversed(String billHeadCode) {
        IEntityDao<ErpFinVoucherBillR> linkDao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billHeadCode),
                eq("businessType", ErpFinBusinessType.COST_ADJUSTMENT.name())));
        List<ErpFinVoucherBillR> links = linkDao.findAllByQuery(q);
        IEntityDao<ErpFinVoucher> voucherDao = daoProvider.daoFor(ErpFinVoucher.class);
        for (ErpFinVoucherBillR link : links) {
            ErpFinVoucher voucher = voucherDao.getEntityById(link.getVoucherId());
            if (voucher != null && Objects.equals(voucher.getDocStatus(), ErpFinConstants.VOUCHER_STATUS_POSTED)
                    && !Boolean.TRUE.equals(voucher.getIsReversed())
                    && (voucher.getPostingType() == null || Objects.equals(voucher.getPostingType(), POSTING_TYPE_NORMAL))) {
                voucher.setIsReversed(true);
                voucherDao.updateEntity(voucher);
            }
        }
    }

    private IServiceContext ctx() {
        IServiceContext context = IServiceContext.getCtx();
        return context != null ? context : new ServiceContextImpl();
    }

    private Long resolveAcctSchemaId(Long orgId) {
        return AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);
    }
}
