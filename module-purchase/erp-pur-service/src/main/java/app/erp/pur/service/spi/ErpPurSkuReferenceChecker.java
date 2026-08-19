package app.erp.pur.service.spi;

import app.erp.md.dao.entity.ErpMdMaterialSku;
import app.erp.md.spi.IErpMdSkuReferenceChecker;
import app.erp.pur.dao.constants.ErpPurDocStatus;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import app.erp.pur.dao.entity.ErpPurReceiveLine;
import app.erp.pur.dao.entity.ErpPurReturnLine;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ne;

/**
 * SKU 业务单据引用检查 SPI 生产实现——purchase 域（RC-R1.72，plan 2026-08-19-0445-1 Phase 2 D3）。
 *
 * <p>端口 {@link IErpMdSkuReferenceChecker} 声明在 master-data（基础域不可反向依赖下游域），
 * 本实现落在 purchase-service（purchase → master-data 为合法 DAG 边，镜像 finance
 * {@code ErpMdEmployeeReferenceCheckerImpl} 先例；多域聚合经 md 侧
 * {@code ErpMdSkuReferenceCheckerRegistry} List 收集器承接）。
 *
 * <p><b>活跃引用口径（D3）</b>：OrderLine / ReceiveLine / ReturnLine 经 header docStatus ≠ CANCELLED
 * （开放单据行构成引用；取消单不阻断）。E3 自检：daoFor 均为 purchase 同域实体（SPI 检查器无 I*Biz
 * 需求——只读 exists 判定，fin employee checker 同型先例）；exists 经 limit 1 查询非全量加载；
 * header 状态过滤经关联属性路径（{@code order.docStatus} 等，DAO 层查询翻译，BankLedgerQuery 先例）。
 */
public class ErpPurSkuReferenceChecker implements IErpMdSkuReferenceChecker {

    @Inject
    IDaoProvider daoProvider;

    @Override
    public boolean isReferencedByBill(ErpMdMaterialSku sku) {
        if (sku == null || sku.getId() == null) {
            return false;
        }
        return existsOrderLine(sku.getId()) || existsReceiveLine(sku.getId()) || existsReturnLine(sku.getId());
    }

    private boolean existsOrderLine(Long skuId) {
        IEntityDao<ErpPurOrderLine> dao = daoProvider.daoFor(ErpPurOrderLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("skuId", skuId));
        q.addFilter(ne("order.docStatus", ErpPurDocStatus.DOC_STATUS_CANCELLED));
        q.setLimit(1);
        return !dao.findAllByQuery(q).isEmpty();
    }

    private boolean existsReceiveLine(Long skuId) {
        IEntityDao<ErpPurReceiveLine> dao = daoProvider.daoFor(ErpPurReceiveLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("skuId", skuId));
        q.addFilter(ne("receive.docStatus", ErpPurDocStatus.DOC_STATUS_CANCELLED));
        q.setLimit(1);
        return !dao.findAllByQuery(q).isEmpty();
    }

    private boolean existsReturnLine(Long skuId) {
        IEntityDao<ErpPurReturnLine> dao = daoProvider.daoFor(ErpPurReturnLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("skuId", skuId));
        q.addFilter(ne("return.docStatus", ErpPurDocStatus.DOC_STATUS_CANCELLED));
        q.setLimit(1);
        return !dao.findAllByQuery(q).isEmpty();
    }
}
