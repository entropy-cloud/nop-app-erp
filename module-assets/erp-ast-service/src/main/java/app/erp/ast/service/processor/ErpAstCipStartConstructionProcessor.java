package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstCip;
import app.erp.ast.service.ErpAstConstants;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.Objects;

/**
 * ErpAstCip startConstruction per-mutation Processor（R6.3，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含开工编排；共享 protected helper 单一真相源在 {@link ErpAstCipProcessor}（slim-to-query-only facade）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpAstCipStartConstructionProcessor {

    @Inject
    ErpAstCipProcessor facade;

    public ErpAstCip startConstruction(Long cipId, IServiceContext context) {
        ErpAstCip cip = facade.requireCip(cipId, context);
        String current = cip.getStatus();
        if (!Objects.equals(current, ErpAstConstants.CIP_STATUS_DRAFT)) {
            throw facade.illegalTransition(cip, current, ErpAstConstants.CIP_STATUS_IN_CONSTRUCTION);
        }
        facade.validateCipInfoComplete(cip, context);
        cip.setStatus(ErpAstConstants.CIP_STATUS_IN_CONSTRUCTION);
        facade.cipDao().saveOrUpdateEntity(cip);
        facade.orm().flushSession();
        return cip;
    }
}
