package app.erp.prj.service.posting;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 项目过账执行器：跨域经凭证聚合根 Facade {@link IErpFinVoucherBiz} 调用财务过账引擎
 * （processor-extension-pattern 硬规则 2：跨域注入 IErpXxxBiz，不注入 Processor 具体类）。
 *
 * <p>跨域失败隔离的事务边界由 Facade {@code IErpFinVoucherBiz.post()} 的 {@code @Transactional(REQUIRES_NEW)}
 * 承接。本执行器不带 {@code @Transactional}，对齐 {@code AssetPostingExecutor}/{@code SalPostingExecutor}。
 * 调用方（各 Dispatcher）以 try/catch 包裹。
 *
 * <p>对齐资产域 reverse 语义：补标原正常凭证 isReversed=true，使账簿反映原凭证已被红冲、允许幂等重过账。
 */
public class ProjectPostingExecutor {

    private static final String VOUCHER_STATUS_POSTED = ErpFinConstants.VOUCHER_STATUS_POSTED;
    private static final String POSTING_TYPE_NORMAL = "NORMAL";

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
        markOriginalVoucherReversed(billHeadCode, businessType);
    }

    private void markOriginalVoucherReversed(String billHeadCode, ErpFinBusinessType businessType) {
        IEntityDao<ErpFinVoucherBillR> linkDao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billHeadCode), eq("businessType", businessType.name())));
        List<ErpFinVoucherBillR> links = linkDao.findAllByQuery(q);
        IEntityDao<ErpFinVoucher> voucherDao = daoProvider.daoFor(ErpFinVoucher.class);
        for (ErpFinVoucherBillR link : links) {
            ErpFinVoucher voucher = voucherDao.getEntityById(link.getVoucherId());
            if (voucher != null && Objects.equals(voucher.getDocStatus(), VOUCHER_STATUS_POSTED)
                    && !Boolean.TRUE.equals(voucher.getIsReversed())
                    && (voucher.getPostingType() == null || Objects.equals(voucher.getPostingType(), POSTING_TYPE_NORMAL))) {
                voucher.setIsReversed(true);
                voucherDao.updateEntity(voucher);
            }
        }
    }
}
