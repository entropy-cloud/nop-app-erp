package app.erp.sal.service.spi;

import app.erp.md.dao.entity.ErpMdMaterialSku;
import app.erp.md.spi.IErpMdSkuReferenceChecker;
import app.erp.sal.dao.constants.ErpSalDocStatus;
import app.erp.sal.dao.entity.ErpSalDeliveryLine;
import app.erp.sal.dao.entity.ErpSalOrderLine;
import app.erp.sal.dao.entity.ErpSalPriceListLine;
import app.erp.sal.dao.entity.ErpSalReturnLine;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.isNull;
import static io.nop.api.core.beans.FilterBeans.ne;
import static io.nop.api.core.beans.FilterBeans.or;

/**
 * SKU 业务单据引用检查 SPI 生产实现——sales 域（RC-R1.72，plan 2026-08-19-0445-1 Phase 2 D3）。
 *
 * <p>端口 {@link IErpMdSkuReferenceChecker} 声明在 master-data，本实现落在 sales-service
 * （sales → master-data 为合法 DAG 边；多域聚合经 md 侧 {@code ErpMdSkuReferenceCheckerRegistry}
 * List 收集器承接）。
 *
 * <p><b>活跃引用口径（D3）</b>：OrderLine / DeliveryLine / ReturnLine 经 header docStatus ≠ CANCELLED；
 * PriceListLine 经 priceList.isActive=true 且 priceList.validTo ≥ 当日或为空（过期价目表不阻断）。
 * E3 自检：daoFor 均为 sales 同域实体（SPI 只读 exists 判定，fin employee checker 同型先例）；
 * exists 经 limit 1 查询非全量加载；header 过滤经关联属性路径（DAO 层查询翻译）。
 */
public class ErpSalSkuReferenceChecker implements IErpMdSkuReferenceChecker {

    @Inject
    IDaoProvider daoProvider;

    @Override
    public boolean isReferencedByBill(ErpMdMaterialSku sku) {
        if (sku == null || sku.getId() == null) {
            return false;
        }
        return existsOrderLine(sku.getId()) || existsDeliveryLine(sku.getId())
                || existsReturnLine(sku.getId()) || existsPriceListLine(sku.getId());
    }

    private boolean existsOrderLine(Long skuId) {
        IEntityDao<ErpSalOrderLine> dao = daoProvider.daoFor(ErpSalOrderLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("skuId", skuId));
        q.addFilter(ne("order.docStatus", ErpSalDocStatus.DOC_STATUS_CANCELLED));
        q.setLimit(1);
        return !dao.findAllByQuery(q).isEmpty();
    }

    private boolean existsDeliveryLine(Long skuId) {
        IEntityDao<ErpSalDeliveryLine> dao = daoProvider.daoFor(ErpSalDeliveryLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("skuId", skuId));
        q.addFilter(ne("delivery.docStatus", ErpSalDocStatus.DOC_STATUS_CANCELLED));
        q.setLimit(1);
        return !dao.findAllByQuery(q).isEmpty();
    }

    private boolean existsReturnLine(Long skuId) {
        IEntityDao<ErpSalReturnLine> dao = daoProvider.daoFor(ErpSalReturnLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("skuId", skuId));
        q.addFilter(ne("return.docStatus", ErpSalDocStatus.DOC_STATUS_CANCELLED));
        q.setLimit(1);
        return !dao.findAllByQuery(q).isEmpty();
    }

    private boolean existsPriceListLine(Long skuId) {
        IEntityDao<ErpSalPriceListLine> dao = daoProvider.daoFor(ErpSalPriceListLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("skuId", skuId));
        q.addFilter(eq("priceList.isActive", Boolean.TRUE));
        // 过期价目表不阻断：validTo 为空（不限期）或 ≥ 当日
        q.addFilter(or(isNull("priceList.validTo"), ge("priceList.validTo", CoreMetrics.today())));
        q.setLimit(1);
        return !dao.findAllByQuery(q).isEmpty();
    }
}
