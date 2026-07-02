package app.erp.ast.service.posting;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;

import jakarta.inject.Inject;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 资产过账执行器：跨域经凭证聚合根 Facade {@link IErpFinVoucherBiz} 调用财务过账引擎
 * （processor-extension-pattern 硬规则 2：跨域注入 IErpXxxBiz，不注入 Processor 具体类）。
 *
 * <p>跨域失败隔离的事务边界由 Facade {@code IErpFinVoucherBiz.post()} 的 {@code @Transactional(REQUIRES_NEW)}
 * 承接（硬规则 1：事务边界钉 Facade，不下放编排层）。本执行器不带 {@code @Transactional}，对齐
 * {@code SalPostingExecutor} / {@code InvPostingExecutor}。调用方（各 Dispatcher）以 try/catch 包裹。
 */
public class AssetPostingExecutor {

    /** 凭证单据状态：已过账（与引擎 VOUCHER_STATUS_POSTED 一致）。 */
    private static final int VOUCHER_STATUS_POSTED = 20;
    /** 凭证过账类型：正常（与引擎 POSTING_TYPE_NORMAL 一致；红字冲销凭证为 REVERSAL=50）。 */
    private static final int POSTING_TYPE_NORMAL = 10;

    @Inject
    IErpFinVoucherBiz voucherBiz;

    @Inject
    IDaoProvider daoProvider;

    public Long postEvent(PostingEvent event) {
        IServiceContext context = IServiceContext.getCtx();
        if (context == null) {
            context = new ServiceContextImpl();
        }
        return voucherBiz.post(event, context);
    }

    public void reverse(String billHeadCode, ErpFinBusinessType businessType) {
        IServiceContext context = IServiceContext.getCtx();
        if (context == null) {
            context = new ServiceContextImpl();
        }
        voucherBiz.reverse(billHeadCode, businessType, context);
        // 引擎 reverse 只把新建的红字凭证置 isReversed=true，未标记原正常凭证已冲销。
        // 此处补标原正常凭证 isReversed=true，使账簿反映原凭证已被红冲、并允许幂等重过账（同 billCode）。
        markOriginalVoucherReversed(billHeadCode, businessType);
    }

    private void markOriginalVoucherReversed(String billHeadCode, ErpFinBusinessType businessType) {
        IEntityDao<ErpFinVoucherBillR> linkDao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billHeadCode), eq("businessType", businessType.getCode())));
        java.util.List<ErpFinVoucherBillR> links = linkDao.findAllByQuery(q);
        IEntityDao<ErpFinVoucher> voucherDao = daoProvider.daoFor(ErpFinVoucher.class);
        for (ErpFinVoucherBillR link : links) {
            ErpFinVoucher voucher = voucherDao.getEntityById(link.getVoucherId());
            // 标记原正常凭证（非红字、未冲销、已过账）为已冲销；红字凭证(REVERSAL)跳过
            if (voucher != null && Integer.valueOf(VOUCHER_STATUS_POSTED).equals(voucher.getDocStatus())
                    && !Boolean.TRUE.equals(voucher.getIsReversed())
                    && (voucher.getPostingType() == null || voucher.getPostingType() == POSTING_TYPE_NORMAL)) {
                voucher.setIsReversed(true);
                voucherDao.updateEntity(voucher);
            }
        }
    }
}
