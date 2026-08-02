package app.erp.log.service.processor;

import app.erp.log.dao.entity.ErpLogShipment;
import app.erp.log.service.ErpLogErrors;
import app.erp.common.service.UniqueConstraintHelper;
import io.nop.api.core.exceptions.NopException;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

/**
 * ErpLogShipment save per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 *
 * <p>承载保存后 flush 触发 INSERT/UPDATE 命中 {@code UK_LOG_SHIPMENT_TRACKING_CARRIER}（并发越过前置校验时）
 * → 翻译为友好错误码（plan 2026-07-30-0841-2 R1.28 P1-MA2-092）。
 *
 * <p><b>签名偏差说明</b>：原 {@code @BizMutation save(Map data, IServiceContext)} 体首行调用 {@code super.save}
 * （CrudBizModel 完整管道：buildEntityDataForSave/copyToEntity/checkDataAuth/delayedRelationActions/doSaveEntity），
 * 该管道深绑 CrudBizModel 内部（crudToolProvider/objMeta/evalScope），非 CrudBizModel 的 Processor 无法忠实复刻。
 * 故 BizModel 保留 {@code super.save(data, context)} 调用，仅将持久化后的 flush + 唯一约束翻译迁移至本 Processor
 * （编排位置迁移，业务语义不变）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpLogShipmentSaveProcessor {

    @Inject
    IOrmTemplate ormTemplate;

    /**
     * 仅当已持久化结果携带 trackingNo+carrierId 时额外 flush 以触发 UK 校验；命中违例翻译为友好错误码。
     *
     * @param result 已由 BizModel {@code super.save} 持久化的运单
     */
    public ErpLogShipment save(ErpLogShipment result) {
        if (result != null && result.getTrackingNo() != null && result.getCarrierId() != null) {
            try {
                ormTemplate.flushSession();
            } catch (Exception e) {
                if (UniqueConstraintHelper.isUniqueConstraintViolation(e)) {
                    throw new NopException(ErpLogErrors.ERR_LOG_SHIPMENT_TRACKING_NO_DUPLICATE)
                            .param(ErpLogErrors.ARG_TRACKING_NO, result.getTrackingNo())
                            .param(ErpLogErrors.ARG_CARRIER_ID, result.getCarrierId());
                }
                throw e;
            }
        }
        return result;
    }
}
