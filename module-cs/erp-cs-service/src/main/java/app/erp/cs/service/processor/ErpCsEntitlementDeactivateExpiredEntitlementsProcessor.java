package app.erp.cs.service.processor;

import app.erp.cs.dao.entity.ErpCsEntitlement;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.lt;

/**
 * ErpCsEntitlement deactivateExpiredEntitlements per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含过期权益批量停用编排（endDate&lt;now 的 active 权益置 isActive=false，单条失败隔离）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCsEntitlementDeactivateExpiredEntitlementsProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErpCsEntitlementDeactivateExpiredEntitlementsProcessor.class);

    @Inject
    IDaoProvider daoProvider;

    public List<ErpCsEntitlement> deactivateExpiredEntitlements(IServiceContext context) {
        LocalDate now = io.nop.api.core.time.CoreMetrics.currentDateTime().toLocalDate();
        QueryBean q = new QueryBean();
        q.addFilter(eq("isActive", Boolean.TRUE));
        // endDate < now（已过期）；endDate 为 null（无固定到期日）永不自动停用
        // endDate 为 DATE 列；用 lt + LocalDate（不用 isNotNull，因 null endDate 永不过期）
        q.addFilter(io.nop.api.core.beans.FilterBeans.lt("endDate", now));
        // 直接走 entity dao 绕过 endDate 字段 meta 的 lt 限制（同 ErpCsTicket.scanOverdueTickets 模式）
        List<ErpCsEntitlement> expired = dao().findAllByQuery(q);
        List<ErpCsEntitlement> deactivated = new ArrayList<>();
        for (ErpCsEntitlement e : expired) {
            try {
                e.setIsActive(Boolean.FALSE);
                dao().updateEntity(e);
                deactivated.add(e);
            } catch (Exception ex) {
                // 单条失败隔离，不阻断批量停用
                LOG.warn("entitlement-deactivate-failed: id={}, reason={}", e.getId(), ex.getMessage());
            }
        }
        return deactivated;
    }

    private IEntityDao<ErpCsEntitlement> dao() {
        return daoProvider.daoFor(ErpCsEntitlement.class);
    }
}
