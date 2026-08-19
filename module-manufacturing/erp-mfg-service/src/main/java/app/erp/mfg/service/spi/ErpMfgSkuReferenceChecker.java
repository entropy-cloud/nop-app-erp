package app.erp.mfg.service.spi;

import app.erp.md.dao.entity.ErpMdMaterialSku;
import app.erp.md.spi.IErpMdSkuReferenceChecker;
import app.erp.mfg.dao.ErpMfgDaoConstants;
import app.erp.mfg.dao.entity.ErpMfgBomByproduct;
import app.erp.mfg.dao.entity.ErpMfgBomLine;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssueLine;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderLine;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.ne;

/**
 * SKU 业务单据引用检查 SPI 生产实现——manufacturing 域（RC-R1.72，plan 2026-08-19-0445-1 Phase 2 D3）。
 *
 * <p>端口 {@link IErpMdSkuReferenceChecker} 声明在 master-data，本实现落在 mfg-service
 * （manufacturing → master-data 为合法 DAG 边；多域聚合经 md 侧 {@code ErpMdSkuReferenceCheckerRegistry}
 * List 收集器承接）。
 *
 * <p><b>活跃引用口径（D3）</b>：BomLine / BomByproduct 经 bom.isActive=true（活跃配置）；
 * WorkOrderLine 经 workOrder.docStatus ∉ 终态 {CLOSED, CANCELLED}；MaterialIssueLine 经
 * issue.docStatus ∈ {DRAFT, CONFIRMED}。E3 自检：daoFor 均为 manufacturing 同域实体
 * （SPI 只读 exists 判定，fin employee checker 同型先例）；exists 经 limit 1 查询非全量加载；
 * header 过滤经关联属性路径（DAO 层查询翻译）。
 */
public class ErpMfgSkuReferenceChecker implements IErpMdSkuReferenceChecker {

    private static final List<String> TERMINAL_WORK_ORDER_STATUSES =
            List.of(ErpMfgDaoConstants.WORK_ORDER_STATUS_CLOSED, ErpMfgDaoConstants.WORK_ORDER_STATUS_CANCELLED);
    private static final List<String> OPEN_ISSUE_STATUSES =
            List.of(ErpMfgDaoConstants.ISSUE_STATUS_DRAFT, ErpMfgDaoConstants.ISSUE_STATUS_CONFIRMED);

    @Inject
    IDaoProvider daoProvider;

    @Override
    public boolean isReferencedByBill(ErpMdMaterialSku sku) {
        if (sku == null || sku.getId() == null) {
            return false;
        }
        Long skuId = sku.getId();
        return existsBomLine(skuId) || existsBomByproduct(skuId)
                || existsWorkOrderLine(skuId) || existsMaterialIssueLine(skuId);
    }

    private boolean existsBomLine(Long skuId) {
        IEntityDao<ErpMfgBomLine> dao = daoProvider.daoFor(ErpMfgBomLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("skuId", skuId));
        q.addFilter(eq("bom.isActive", Boolean.TRUE));
        q.setLimit(1);
        return !dao.findAllByQuery(q).isEmpty();
    }

    private boolean existsBomByproduct(Long skuId) {
        IEntityDao<ErpMfgBomByproduct> dao = daoProvider.daoFor(ErpMfgBomByproduct.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("skuId", skuId));
        q.addFilter(eq("bom.isActive", Boolean.TRUE));
        q.setLimit(1);
        return !dao.findAllByQuery(q).isEmpty();
    }

    private boolean existsWorkOrderLine(Long skuId) {
        IEntityDao<ErpMfgWorkOrderLine> dao = daoProvider.daoFor(ErpMfgWorkOrderLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("skuId", skuId));
        for (String terminal : TERMINAL_WORK_ORDER_STATUSES) {
            q.addFilter(ne("workOrder.docStatus", terminal));
        }
        q.setLimit(1);
        return !dao.findAllByQuery(q).isEmpty();
    }

    private boolean existsMaterialIssueLine(Long skuId) {
        IEntityDao<ErpMfgMaterialIssueLine> dao = daoProvider.daoFor(ErpMfgMaterialIssueLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("skuId", skuId));
        q.addFilter(in("issue.docStatus", OPEN_ISSUE_STATUSES));
        q.setLimit(1);
        return !dao.findAllByQuery(q).isEmpty();
    }
}
