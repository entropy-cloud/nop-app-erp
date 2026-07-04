
package app.erp.mfg.service.entity;

import app.erp.mfg.biz.IErpMfgCostRollupBiz;
import app.erp.mfg.dao.entity.ErpMfgCostRollup;
import app.erp.mfg.dao.entity.ErpMfgCostRollupLine;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

@BizModel("ErpMfgCostRollup")
public class ErpMfgCostRollupBizModel extends CrudBizModel<ErpMfgCostRollup> implements IErpMfgCostRollupBiz {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    static final String STATUS_FIRMED = "FIRMED";

    public ErpMfgCostRollupBizModel() {
        setEntityName(ErpMfgCostRollup.class.getName());
    }

    @Override
    @BizQuery
    public BigDecimal findLatestFirmedStandardCost(@Name("materialId") Long materialId, IServiceContext context) {
        if (materialId == null) {
            return null;
        }
        ormTemplate.flushSession();
        IEntityDao<ErpMfgCostRollup> headerDao = daoProvider.daoFor(ErpMfgCostRollup.class);
        QueryBean hq = new QueryBean();
        hq.addFilter(eq("status", STATUS_FIRMED));
        List<ErpMfgCostRollup> firmedList = headerDao.findAllByQuery(hq);
        if (firmedList.isEmpty()) {
            return null;
        }
        firmedList.sort(Comparator.comparing(
                h -> h.getBusinessDate() != null ? h.getBusinessDate() : java.time.LocalDate.MIN,
                Comparator.reverseOrder()));

        IEntityDao<ErpMfgCostRollupLine> lineDao = daoProvider.daoFor(ErpMfgCostRollupLine.class);
        for (ErpMfgCostRollup header : firmedList) {
            QueryBean lq = new QueryBean();
            lq.addFilter(eq("costRollupId", header.getId()));
            lq.addFilter(eq("materialId", materialId));
            List<ErpMfgCostRollupLine> lines = lineDao.findAllByQuery(lq);
            if (!lines.isEmpty()) {
                return lines.get(0).getUnitCost();
            }
        }
        return null;
    }
}
