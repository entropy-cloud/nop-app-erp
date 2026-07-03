package app.erp.qa.service;

import app.erp.sal.biz.IErpSalReturnBiz;
import app.erp.sal.dao.entity.ErpSalReturn;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 测试桩：销售退货 BizModel。仅用于 quality-service 测试（避免引入 sales-service 形成 reactor 环）。
 *
 * <p>{@link #save} 直接经 {@code dao()} 构造退货单头（绕过 CrudBizModel 动作机制——sales xbiz 不在测试 classpath），
 * 召回 generateReturns 经此创建退货单草稿并取得生成 ID。其余 CRUD/状态机方法为占位。
 */
@BizModel("ErpSalReturn")
public class TestStubErpSalReturnBiz extends CrudBizModel<ErpSalReturn> implements IErpSalReturnBiz {

    public TestStubErpSalReturnBiz() {
        setEntityName(ErpSalReturn.class.getName());
    }

    @Override
    @SuppressWarnings("unchecked")
    public ErpSalReturn save(Map<String, Object> data, IServiceContext context) {
        ErpSalReturn entity = new ErpSalReturn();
        if (data != null) {
            entity.setCode(asString(data.get("code")));
            entity.setCustomerId(asLong(data.get("customerId")));
            entity.setDeliveryId(asLong(data.get("deliveryId")));
            entity.setWarehouseId(asLong(data.get("warehouseId")));
            entity.setCurrencyId(asLong(data.get("currencyId")));
            Object bd = data.get("businessDate");
            if (bd != null) {
                entity.setBusinessDate(bd instanceof LocalDate ? (LocalDate) bd : LocalDate.parse(String.valueOf(bd)));
            }
            Object docStatus = data.get("docStatus");
            if (docStatus != null) {
                entity.setDocStatus(asInt(docStatus));
            }
            Object approveStatus = data.get("approveStatus");
            if (approveStatus != null) {
                entity.setApproveStatus(asInt(approveStatus));
            }
            entity.setPosted(Boolean.FALSE);
        }
        dao().saveEntity(entity);
        return entity;
    }

    @Override
    public ErpSalReturn findFirst(QueryBean query, io.nop.api.core.beans.FieldSelectionBean selection, IServiceContext context) {
        List<ErpSalReturn> list = dao().findAllByQuery(query);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public ErpSalReturn get(String id, boolean ignoreUnknown, IServiceContext context) {
        return dao().getEntityById(Long.valueOf(id));
    }

    @Override
    public ErpSalReturn submit(Long returnId, IServiceContext context) {
        return get(String.valueOf(returnId), false, context);
    }

    @Override
    public ErpSalReturn withdrawSubmit(Long returnId, IServiceContext context) {
        return get(String.valueOf(returnId), false, context);
    }

    @Override
    public ErpSalReturn approve(Long returnId, IServiceContext context) {
        return get(String.valueOf(returnId), false, context);
    }

    @Override
    public ErpSalReturn reject(Long returnId, IServiceContext context) {
        return get(String.valueOf(returnId), false, context);
    }

    @Override
    public ErpSalReturn reverseApprove(Long returnId, IServiceContext context) {
        return get(String.valueOf(returnId), false, context);
    }

    @Override
    public ErpSalReturn cancel(Long returnId, IServiceContext context) {
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

    private static Integer asInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.valueOf(value.toString().trim());
    }
}
