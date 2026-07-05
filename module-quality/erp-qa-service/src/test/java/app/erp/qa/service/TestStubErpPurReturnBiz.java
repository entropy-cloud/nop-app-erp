package app.erp.qa.service;

import app.erp.pur.biz.IErpPurReturnBiz;
import app.erp.pur.dao.entity.ErpPurReturn;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 测试桩：采购退货 BizModel。仅用于 quality-service 测试（避免引入 purchase-service 形成 reactor 环）。
 *
 * <p>{@link #save} 直接经 {@code dao()} 构造退货单头（绕过 CrudBizModel 动作机制——purchase xbiz 不在测试 classpath）。
 * NCR RETURN 处置编排经此创建退货单并取得生成 code。其余 CRUD/状态机方法为占位。
 */
@BizModel("ErpPurReturn")
public class TestStubErpPurReturnBiz extends CrudBizModel<ErpPurReturn> implements IErpPurReturnBiz {

    public TestStubErpPurReturnBiz() {
        setEntityName(ErpPurReturn.class.getName());
    }

    @Override
    @SuppressWarnings("unchecked")
    public ErpPurReturn save(Map<String, Object> data, IServiceContext context) {
        ErpPurReturn entity = new ErpPurReturn();
        if (data != null) {
            entity.setCode(asString(data.get("code")));
            entity.setSupplierId(asLong(data.get("supplierId")));
            entity.setWarehouseId(asLong(data.get("warehouseId")));
            entity.setCurrencyId(asLong(data.get("currencyId")));
            Object bd = data.get("businessDate");
            if (bd != null) {
                entity.setBusinessDate(bd instanceof LocalDate ? (LocalDate) bd : LocalDate.parse(String.valueOf(bd)));
            }
            entity.setDocStatus(asString(data.getOrDefault("docStatus", "DRAFT")));
            entity.setApproveStatus(asString(data.getOrDefault("approveStatus", "UNSUBMITTED")));
            entity.setPosted(Boolean.FALSE);
        }
        dao().saveEntity(entity);
        return entity;
    }

    @Override
    public ErpPurReturn findFirst(QueryBean query, io.nop.api.core.beans.FieldSelectionBean selection, IServiceContext context) {
        List<ErpPurReturn> list = dao().findAllByQuery(query);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public ErpPurReturn get(String id, boolean ignoreUnknown, IServiceContext context) {
        return dao().getEntityById(Long.valueOf(id));
    }

    @Override
    public ErpPurReturn submitForApproval(String id, IServiceContext context) {
        return get(id, false, context);
    }

    @Override
    public ErpPurReturn withdrawApproval(String id, IServiceContext context) {
        return get(id, false, context);
    }

    @Override
    public ErpPurReturn approve(String id, IServiceContext context) {
        return get(id, false, context);
    }

    @Override
    public ErpPurReturn reject(String id, IServiceContext context) {
        return get(id, false, context);
    }

    @Override
    public ErpPurReturn reverseApprove(String id, IServiceContext context) {
        return get(id, false, context);
    }

    @Override
    public ErpPurReturn cancel(Long returnId, IServiceContext context) {
        return get(String.valueOf(returnId), false, context);
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private static Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.valueOf(value.toString().trim());
    }
}
