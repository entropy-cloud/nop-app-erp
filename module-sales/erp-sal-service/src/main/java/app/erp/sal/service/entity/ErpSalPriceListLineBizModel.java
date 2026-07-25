
package app.erp.sal.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;

import app.erp.md.service.daterange.ErpDateRangeOverlapValidator;
import app.erp.sal.biz.IErpSalPriceListLineBiz;
import app.erp.sal.dao.entity.ErpSalPriceListLine;
import app.erp.sal.service.ErpSalErrors;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 销售价格清单行 BizModel。
 *
 * <p>基础 CRUD 走 {@link CrudBizModel}。扩展 C3 日期范围有效性保存钩子（plan 2026-07-26-0315-1 Phase 2）：
 * 同 priceListId + materialId 维度的多份行记录互斥（MUTEX 策略）—— 在
 * {@code defaultPrepareSave/Update} 中调用 {@link ErpDateRangeOverlapValidator#enforceMutex}，
 * 重叠抛 {@link ErpSalErrors#ERR_SAL_PRICE_LIST_LINE_OVERLAP}。详见
 * {@code docs/design/date-ranged-validity-pattern.md} §6。
 */
@BizModel("ErpSalPriceListLine")
public class ErpSalPriceListLineBizModel extends CrudBizModel<ErpSalPriceListLine> implements IErpSalPriceListLineBiz {

    public ErpSalPriceListLineBizModel() {
        setEntityName(ErpSalPriceListLine.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpSalPriceListLine> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        enforceNoOverlap(entityData.getEntity());
    }

    @Override
    protected void defaultPrepareUpdate(EntityData<ErpSalPriceListLine> entityData, IServiceContext context) {
        super.defaultPrepareUpdate(entityData, context);
        enforceNoOverlap(entityData.getEntity());
    }

    /**
     * 同维度（priceListId + materialId）区间互斥校验。
     *
     * <p>实现说明：直接经 {@code dao().findAllByQuery(query)} 查同维度既有记录
     * （与 {@code ErpMdExchangeRateBizModel.enforceNoOverlap} 同范式），调
     * {@link ErpDateRangeOverlapValidator#enforceMutex} 排除自身（selfId=entity.getId()）。
     */
    protected void enforceNoOverlap(ErpSalPriceListLine entity) {
        if (entity == null) {
            return;
        }
        QueryBean query = new QueryBean();
        query.addFilter(eq("priceListId", entity.getPriceListId()));
        query.addFilter(eq("materialId", entity.getMaterialId()));
        ErpDateRangeOverlapValidator.enforceMutex(
                entity,
                dao().findAllByQuery(query),
                ErpSalErrors.ERR_SAL_PRICE_LIST_LINE_OVERLAP,
                entity.getId());
    }
}
