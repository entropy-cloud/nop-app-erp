
package app.erp.log.service.entity;

import app.erp.log.biz.IErpLogShipmentBiz;
import app.erp.log.dao.entity.ErpLogShipment;
import app.erp.log.service.ErpLogConstants;
import app.erp.log.service.ErpLogErrors;
import app.erp.log.service.processor.ErpLogShipmentAdviseProcessor;
import app.erp.log.service.processor.ErpLogShipmentCancelShipmentProcessor;
import app.erp.log.service.processor.ErpLogShipmentCompleteShipmentProcessor;
import app.erp.log.service.processor.ErpLogShipmentHandleTrackingWebhookProcessor;
import app.erp.log.service.processor.ErpLogShipmentSaveProcessor;
import app.erp.log.service.processor.ErpLogShipmentScanForPollingProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * 发运单聚合根 Biz。CRUD 之外承载运单状态机与网关集成动作（advise/completeShipment/cancelShipment/
 * handleTrackingWebhook/scanForPolling），网关调用 + ORM 编排委托各 per-mutation Processor（参 InvPostingDispatcher 范式：
 * 一致 dao.getEntityById + dao.saveOrUpdateEntity，避免 requireEntity 与直接 dao 操作的 session 不一致）。
 *
 * <p>状态机（{@code state-machine.md §1/§2}）：DRAFT→ADVISED（{@link #advise}）→DISPATCHED
 * （{@link #completeShipment}）→IN_TRANSIT→DELIVERED（webhook {@link #handleTrackingWebhook}
 * 或轮询 {@link #scanForPolling}）；CANCELLED 终态（{@link #cancelShipment}）。
 *
 * <p>DELIVERED 触发运费过账（path-1）或到岸成本自动创建（path-2，config-gated
 * {@code erp-log.path2-landed-cost-auto-create}，plan 2026-07-11-2329-1），编排位于
 * {@code ErpLogShipmentHandleTrackingWebhookProcessor} / {@code ErpLogShipmentScanForPollingProcessor}
 * 共享基类 {@code AbstractErpLogShipmentDeliveredProcessor}。
 *
 * <p>R6.7：上述 6 个 {@code @BizMutation} 已拆为独立 per-mutation Processor（{@code processor-extension-pattern.md}），
 * 本类仅保留单行委托 + CRUD 钩子（{@link #defaultPrepareSave}）。
 */
@BizModel("ErpLogShipment")
public class ErpLogShipmentBizModel extends CrudBizModel<ErpLogShipment> implements IErpLogShipmentBiz {

    @Inject
    ErpLogShipmentSaveProcessor saveProcessor;
    @Inject
    ErpLogShipmentAdviseProcessor adviseProcessor;
    @Inject
    ErpLogShipmentCompleteShipmentProcessor completeShipmentProcessor;
    @Inject
    ErpLogShipmentCancelShipmentProcessor cancelShipmentProcessor;
    @Inject
    ErpLogShipmentHandleTrackingWebhookProcessor handleTrackingWebhookProcessor;
    @Inject
    ErpLogShipmentScanForPollingProcessor scanForPollingProcessor;

    public ErpLogShipmentBizModel() {
        setEntityName(ErpLogShipment.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpLogShipment> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        ErpLogShipment entity = entityData.getEntity();
        if (entity.getBusinessDate() == null) {
            entity.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        }
        // 应用层前置校验（plan 2026-07-30-0841-2 R1.28 P1-MA2-092）：非空 trackingNo+carrierId 重复 → 友好错误码。
        // DB UK_LOG_SHIPMENT_TRACKING_CARRIER 为并发 TOCTOU 兜底（见 save Processor 的 flush-catch）。
        if (entity.getTrackingNo() != null && entity.getCarrierId() != null) {
            io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
            q.addFilter(io.nop.api.core.beans.FilterBeans.eq("trackingNo", entity.getTrackingNo()));
            q.addFilter(io.nop.api.core.beans.FilterBeans.eq("carrierId", entity.getCarrierId()));
            if (entity.orm_id() != null) {
                q.addFilter(io.nop.api.core.beans.FilterBeans.ne("id", entity.orm_id()));
            }
            q.setLimit(1);
            if (findCount(q, context) > 0) {
                throw new NopException(ErpLogErrors.ERR_LOG_SHIPMENT_TRACKING_NO_DUPLICATE)
                        .param(ErpLogErrors.ARG_TRACKING_NO, entity.getTrackingNo())
                        .param(ErpLogErrors.ARG_CARRIER_ID, entity.getCarrierId());
            }
        }
        // RC-R1.83（P1-RC-083，UC-LOG-01）：relatedBillType+relatedBillCode 非 CANCELLED 重复守卫（重复发运防护）。
        // 命中则拒绝并携带出库单标识（L1「该出库单已存在有效发运单」）；CANCELLED 不阻断；relatedBill 为空（手工发运）不触发。
        // status 的 xmeta 过滤面不支持 ne，故 eq 检索后在内存剔除 CANCELLED（参 existsActiveByQuotation 范式）；
        // 并发 TOCTOU 残余风险见计划 Deferred（DB UK 硬化须另行双独立子 agent 批准）。
        if (entity.getRelatedBillType() != null && entity.getRelatedBillCode() != null) {
            io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
            q.addFilter(io.nop.api.core.beans.FilterBeans.eq("relatedBillType", entity.getRelatedBillType()));
            q.addFilter(io.nop.api.core.beans.FilterBeans.eq("relatedBillCode", entity.getRelatedBillCode()));
            for (ErpLogShipment existing : findList(q, null, context)) {
                if (ErpLogConstants.SHIPMENT_STATUS_CANCELLED.equals(existing.getStatus())) {
                    continue;
                }
                if (entity.orm_id() != null && entity.orm_id().equals(existing.orm_id())) {
                    continue;
                }
                throw new NopException(ErpLogErrors.ERR_LOG_SHIPMENT_RELATED_BILL_DUPLICATE)
                        .param(ErpLogErrors.ARG_RELATED_BILL_TYPE, entity.getRelatedBillType())
                        .param(ErpLogErrors.ARG_RELATED_BILL_CODE, entity.getRelatedBillCode());
            }
        }
    }

    /**
     * 覆写 save：保存后 flush 触发 INSERT/UPDATE，命中 UK_LOG_SHIPMENT_TRACKING_CARRIER（并发越过前置校验时）
     * → 翻译为友好错误码（plan 2026-07-30-0841-2 R1.28 P1-MA2-092）。仅当携带 trackingNo 时额外 flush。
     *
     * <p>super.save（CrudBizModel 完整管道）不可在非 CrudBizModel Processor 中忠实复刻，故 BizModel 保留 super.save
     * 调用，仅 flush + 唯一约束翻译迁移至 {@link ErpLogShipmentSaveProcessor#save}。
     */
    @Override
    @BizMutation
    public ErpLogShipment save(java.util.Map<String, Object> data, IServiceContext context) {
        return saveProcessor.save(super.save(data, context));
    }

    @Override
    @BizMutation
    public ErpLogShipment advise(@Name("shipmentId") Long shipmentId, IServiceContext context) {
        return adviseProcessor.advise(shipmentId, context);
    }

    @Override
    @BizMutation
    public ErpLogShipment completeShipment(@Name("shipmentId") Long shipmentId, IServiceContext context) {
        return completeShipmentProcessor.completeShipment(shipmentId, context);
    }

    @Override
    @BizMutation
    public ErpLogShipment cancelShipment(@Name("shipmentId") Long shipmentId, IServiceContext context) {
        return cancelShipmentProcessor.cancelShipment(shipmentId, context);
    }

    @Override
    @BizMutation
    public ErpLogShipment handleTrackingWebhook(@Name("carrierCode") String carrierCode,
                                                @Name("signature") String signature,
                                                @Name("payload") String payload,
                                                IServiceContext context) {
        return handleTrackingWebhookProcessor.handleTrackingWebhook(carrierCode, signature, payload, context);
    }

    @Override
    @BizMutation
    public int scanForPolling(IServiceContext context) {
        return scanForPollingProcessor.scanForPolling(context);
    }

}
