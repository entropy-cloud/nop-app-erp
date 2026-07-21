
package app.erp.md.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;

import app.erp.md.biz.IErpMdExchangeRateBiz;
import app.erp.md.dao.entity.ErpMdExchangeRate;
import app.erp.md.service.ErpMdErrors;
import app.erp.md.service.daterange.ErpDateRangeOverlapValidator;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 汇率主数据 BizModel（C3 日期范围有效性模式试点，docs/design/date-ranged-validity-pattern.md §6/§7）。
 *
 * <p>基础 CRUD 走 {@link CrudBizModel} 默认实现。本类扩展 1 个前置校验钩子：
 * 同维度（fromCurrencyId + toCurrencyId + rateType）区间互斥（MUTEX 策略）——
 * 在 {@code defaultPrepareSave} / {@code defaultPrepareUpdate} 中调用
 * {@link ErpDateRangeOverlapValidator#enforceMutex}，重叠抛 {@link ErpMdErrors#ERR_MD_DATE_RANGE_OVERLAP}。
 *
 * <p>config-gated：{@code erp-md.exchange-rate-overlap-check-enabled}（默认 {@code true}）。
 * 关闭时跳过校验，回归历史「允许多条同日汇率」行为，用于业务方临时放行场景。
 */
@BizModel("ErpMdExchangeRate")
public class ErpMdExchangeRateBizModel extends CrudBizModel<ErpMdExchangeRate> implements IErpMdExchangeRateBiz {

    public ErpMdExchangeRateBizModel() {
        setEntityName(ErpMdExchangeRate.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpMdExchangeRate> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        enforceNoOverlap(entityData.getEntity());
    }

    @Override
    protected void defaultPrepareUpdate(EntityData<ErpMdExchangeRate> entityData, IServiceContext context) {
        super.defaultPrepareUpdate(entityData, context);
        enforceNoOverlap(entityData.getEntity());
    }

    /**
     * 同维度（fromCurrencyId + toCurrencyId + rateType）区间互斥校验。
     *
     * <p>实现说明：直接经 {@code dao().findAllByQuery(query)} 查询避免 CrudBizModel 管道的
     * objMeta filter 校验限制（与 {@code ErpMdMaterialCustomsBizModel.enforceDeclarationNoUnique} 同范式）。
     * 维度键 rateType 允许 null（按 SQL NULL 语义，null=null 视为同维度，仅当两侧均 null 时）。
     */
    protected void enforceNoOverlap(ErpMdExchangeRate entity) {
        if (entity == null) {
            return;
        }
        QueryBean query = new QueryBean();
        query.addFilter(eq("fromCurrencyId", entity.getFromCurrencyId()));
        query.addFilter(eq("toCurrencyId", entity.getToCurrencyId()));
        query.addFilter(eq("rateType", entity.getRateType()));
        ErpDateRangeOverlapValidator.enforceMutex(
                entity,
                dao().findAllByQuery(query),
                ErpMdErrors.ERR_MD_DATE_RANGE_OVERLAP,
                entity.getId());
    }
}
