
package app.erp.mfg.service.entity;

import app.erp.common.service.MaskHelper;
import app.erp.mfg.biz.IErpMfgCostRollupBiz;
import app.erp.mfg.dao.entity.ErpMfgCostRollup;
import app.erp.mfg.dao.entity.ErpMfgCostRollupLine;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;

@BizModel("ErpMfgCostRollup")
public class ErpMfgCostRollupBizModel extends AbstractErpCrudBizModel<ErpMfgCostRollup> implements IErpMfgCostRollupBiz {

    @Inject
    IOrmTemplate ormTemplate;

    static final String STATUS_FIRMED = "FIRMED";

    // ---------- E3.1 读取面脱敏（@BizQuery 就地 masking，plan 2026-08-25-1956-1）----------
    // 授权 = 管理员/财务员（与 ErpMfgCostRollupLine 实体面 loader masking 同源，COST_ROLES）；
    // 非授权 = null；无用户上下文 fail-closed。审计载体 = 命中的 rollup line 实体。
    // E3.2 豁免保径：服务端原始取值一律走 DAO 直读（StandardCostResolver 既有范式），
    // 禁止改调此 GraphQL 暴露查询。
    private static final Set<String> COST_ROLES = Set.of(MaskHelper.ROLE_BIZ_ADMIN, MaskHelper.ROLE_FINANCE_STAFF);

    public ErpMfgCostRollupBizModel() {
        setEntityName(ErpMfgCostRollup.class.getName());
    }

    @Override
    @BizQuery
    public BigDecimal findLatestFirmedStandardCost(@Name("materialId") String materialId, IServiceContext context) {
        if (materialId == null) {
            return null;
        }
        ormTemplate.flushSession();
        IEntityDao<ErpMfgCostRollup> headerDao = daoProvider().daoFor(ErpMfgCostRollup.class);
        QueryBean hq = new QueryBean();
        hq.addFilter(eq("status", STATUS_FIRMED));
        List<ErpMfgCostRollup> firmedList = headerDao.findAllByQuery(hq);
        if (firmedList.isEmpty()) {
            return null;
        }
        firmedList.sort(Comparator.comparing(
                h -> h.getBusinessDate() != null ? h.getBusinessDate() : java.time.LocalDate.MIN,
                Comparator.reverseOrder()));

        IEntityDao<ErpMfgCostRollupLine> lineDao = daoProvider().daoFor(ErpMfgCostRollupLine.class);
        for (ErpMfgCostRollup header : firmedList) {
            QueryBean lq = new QueryBean();
            lq.addFilter(eq("costRollupId", header.getId()));
            lq.addFilter(eq("materialId", materialId));
            List<ErpMfgCostRollupLine> lines = lineDao.findAllByQuery(lq);
            if (!lines.isEmpty()) {
                return MaskHelper.maskDecimal(lines.get(0).getUnitCost(), COST_ROLES, lines.get(0), "unitCost");
            }
        }
        return null;
    }

}
