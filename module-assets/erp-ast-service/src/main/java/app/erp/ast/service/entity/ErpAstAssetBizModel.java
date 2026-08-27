
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstAssetBiz;
import app.erp.ast.dao.ErpAstDaoConstants;
import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetActionLog;
import app.erp.ast.dao.entity.ErpAstAssetModel;
import app.erp.ast.service.ErpAstErrors;
import app.erp.ast.service.audit.ErpAstAssetAuditRecorder;
import app.erp.ast.service.processor.ErpAstAssetSuspendResumeProcessor;
import app.erp.common.service.AbstractErpCrudBizModel;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.EntityData;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import io.nop.core.lang.json.JsonTool;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 资产卡片 BizModel（Facade）。CRUD 走 CrudBizModel 默认；RC-R1.54 增 suspend/resume
 * 闲置状态机 mutation（L1 UC-AST-03），编排委托 {@link ErpAstAssetSuspendResumeProcessor}
 * （R6.3 per-mutation Processor，protected step 方法供下游覆盖）。
 *
 * <p>E3 扩展：(1) E3.3 型号级 ext 字段集校验——save/update 钩子按 {@link ErpAstAssetModel#extFieldDefs}
 * 声明校验 {@link ErpAstAsset#extFieldValues}（非法键/缺必填/类型不匹配拒绝，ErrorCode 范式）；
 * (2) E3.8 审计——CRUD 主入口记录 CREATE/UPDATE/STATUS_CHANGE/TRANSFER/VALUATION 事件
 * （diff 变更前后值），{@link #getAssetAuditTrail} 返回时间轴。
 */
@BizModel("ErpAstAsset")
public class ErpAstAssetBizModel extends AbstractErpCrudBizModel<ErpAstAsset> implements IErpAstAssetBiz {

    @Inject
    ErpAstAssetSuspendResumeProcessor suspendResumeProcessor;
    @Inject
    ErpAstAssetAuditRecorder auditRecorder;
    @Inject
    IOrmTemplate ormTemplate;

    public ErpAstAssetBizModel() {
        setEntityName(ErpAstAsset.class.getName());
    }

    @Override
    @BizMutation
    public ErpAstAsset suspend(@Name("assetId") String assetId, IServiceContext context) {
        return suspendResumeProcessor.suspend(assetId, context);
    }

    @Override
    @BizMutation
    public ErpAstAsset resume(@Name("assetId") String assetId, IServiceContext context) {
        return suspendResumeProcessor.resume(assetId, context);
    }

    @Override
    @BizQuery
    public List<Map<String, Object>> getAssetAuditTrail(@Name("assetId") String assetId, IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            QueryBean q = new QueryBean();
            q.addFilter(eq("assetId", assetId));
            q.addOrderField("createTime", true);
            q.addOrderField("id", true);
            IEntityDao<ErpAstAssetActionLog> dao = daoProvider().daoFor(ErpAstAssetActionLog.class);
            List<ErpAstAssetActionLog> logs = dao.findAllByQuery(q);
            List<Map<String, Object>> rows = new ArrayList<>(logs.size());
            for (ErpAstAssetActionLog log : logs) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", log.getId());
                row.put("assetId", log.getAssetId());
                row.put("eventType", log.getEventType());
                row.put("fromStatus", log.getFromStatus());
                row.put("toStatus", log.getToStatus());
                row.put("fromDepartmentId", log.getFromDepartmentId());
                row.put("toDepartmentId", log.getToDepartmentId());
                row.put("fromLocationId", log.getFromLocationId());
                row.put("toLocationId", log.getToLocationId());
                row.put("fromStaffId", log.getFromStaffId());
                row.put("toStaffId", log.getToStaffId());
                row.put("refEntityName", log.getRefEntityName());
                row.put("refEntityId", log.getRefEntityId());
                row.put("summary", log.getSummary());
                row.put("createdBy", log.getCreatedBy());
                row.put("createTime", log.getCreateTime());
                rows.add(row);
            }
            return rows;
        });
    }

    // ---------- CRUD 钩子：E3.3 校验 + E3.8 审计 ----------

    @Override
    protected void defaultPrepareSave(EntityData<ErpAstAsset> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        validateExtFieldValues(entityData.getEntity());
    }

    /** CREATE 审计在保存后记录（seq 主键在 save 后才可读；对齐 hr afterEntityChange 范式）。 */
    @Override
    protected void afterEntityChange(@io.nop.api.core.annotations.core.Name("entity") ErpAstAsset entity,
                                     @io.nop.api.core.annotations.core.Name("action") String action,
                                     IServiceContext context) {
        if (io.nop.biz.BizConstants.METHOD_SAVE.equals(action)) {
            if (entity.getId() == null) {
                orm().flushSession();
            }
            auditRecorder.record(entity, ErpAstDaoConstants.AUDIT_EVENT_TYPE_CREATE, null,
                    null, null, "资产创建");
        }
    }

    @Override
    protected void defaultPrepareUpdate(EntityData<ErpAstAsset> entityData, IServiceContext context) {
        super.defaultPrepareUpdate(entityData, context);
        ErpAstAsset asset = entityData.getEntity();
        validateExtFieldValues(asset);

        Map<String, Object> oldValues = asset.orm_dirtyOldValues();
        ErpAstAssetAuditRecorder.Before before = new ErpAstAssetAuditRecorder.Before(
                str(oldValues.get("status")), str(oldValues.get("departmentId")),
                str(oldValues.get("locationId")), str(oldValues.get("employeeId")));

        if (changed(oldValues, "status")) {
            auditRecorder.record(asset, ErpAstDaoConstants.AUDIT_EVENT_TYPE_STATUS_CHANGE, before,
                    null, null, "资产状态变更（CRUD 更新）：" + oldValues.get("status") + " → " + asset.getStatus());
        }
        if (changed(oldValues, "departmentId") || changed(oldValues, "locationId") || changed(oldValues, "employeeId")) {
            auditRecorder.record(asset, ErpAstDaoConstants.AUDIT_EVENT_TYPE_TRANSFER, before,
                    null, null, "资产归属变更（部门/地点/使用人）");
        }
        if (changed(oldValues, "currentValue")) {
            BigDecimal from = toBigDecimal(oldValues.get("currentValue"));
            BigDecimal to = asset.getCurrentValue() == null ? BigDecimal.ZERO : asset.getCurrentValue();
            auditRecorder.record(asset, ErpAstDaoConstants.AUDIT_EVENT_TYPE_VALUATION, before,
                    null, null, "资产价值调整：" + from + " → " + to);
        }
        if (changed(oldValues, "name") || changed(oldValues, "brandModel") || changed(oldValues, "remark")
                || changed(oldValues, "extFieldValues") || changed(oldValues, "modelId")) {
            auditRecorder.record(asset, ErpAstDaoConstants.AUDIT_EVENT_TYPE_UPDATE, before,
                    null, null, "资产信息更新");
        }
    }

    // ---------- E3.3 ext 字段集校验 ----------

    /**
     * 按型号 {@code extFieldDefs}（[{key,label,type,required}]）校验资产 {@code extFieldValues}：
     * 无型号不允许携带值；有型号时非法键拒绝、必填缺失拒绝、类型不匹配拒绝（string/number/boolean）。
     */
    protected void validateExtFieldValues(ErpAstAsset asset) {
        Map<String, Object> values = parseJsonMap(asset.getExtFieldValues());
        String modelId = asset.getModelId();

        if (StringHelper.isEmpty(modelId)) {
            if (values != null && !values.isEmpty()) {
                throw extError(ErpAstErrors.ERR_AST_EXT_FIELD_WITHOUT_MODEL, asset, null, null, null);
            }
            return;
        }

        ErpAstAssetModel model = asset.getModel();
        if (model == null) {
            throw new NopException(ErpAstErrors.ERR_AST_ASSET_MODEL_NOT_FOUND)
                    .param(ErpAstErrors.ARG_MODEL_ID, modelId);
        }

        Map<String, Map<String, Object>> defs = parseExtFieldDefs(model);
        if (defs == null || defs.isEmpty()) {
            if (values != null && !values.isEmpty()) {
                Map.Entry<String, Object> entry = values.entrySet().iterator().next();
                throw extError(ErpAstErrors.ERR_AST_EXT_FIELD_NOT_DECLARED, asset, model, entry.getKey(), null);
            }
            return;
        }

        if (values != null) {
            for (String key : values.keySet()) {
                if (!defs.containsKey(key)) {
                    throw extError(ErpAstErrors.ERR_AST_EXT_FIELD_NOT_DECLARED, asset, model, key, null);
                }
            }
        }
        for (Map.Entry<String, Map<String, Object>> defEntry : defs.entrySet()) {
            String key = defEntry.getKey();
            Map<String, Object> def = defEntry.getValue();
            boolean required = Boolean.TRUE.equals(def.get("required"));
            Object value = values != null ? values.get(key) : null;
            if (value == null) {
                if (required) {
                    throw extError(ErpAstErrors.ERR_AST_EXT_FIELD_REQUIRED_MISSING, asset, model, key, null);
                }
                continue;
            }
            String type = str(def.get("type"));
            if (StringHelper.isEmpty(type) || "string".equals(type)) {
                continue;
            }
            boolean matches = "number".equals(type) ? value instanceof Number
                    : "boolean".equals(type) && value instanceof Boolean;
            if (!matches) {
                throw extError(ErpAstErrors.ERR_AST_EXT_FIELD_TYPE_MISMATCH, asset, model, key,
                        type + " ≠ " + value.getClass().getSimpleName());
            }
        }
    }

    /** 解析型号字段集声明 [{key,label,type,required}] → key → def。结构非法视为空声明（由校验兜底）。 */
    protected Map<String, Map<String, Object>> parseExtFieldDefs(ErpAstAssetModel model) {
        List<Map<String, Object>> defs = parseJsonList(model.getExtFieldDefs());
        if (defs == null) {
            return null;
        }
        Map<String, Map<String, Object>> byKey = new HashMap<>();
        for (Map<String, Object> def : defs) {
            String key = def != null ? str(def.get("key")) : null;
            if (!StringHelper.isEmpty(key)) {
                byKey.put(key, def);
            }
        }
        return byKey;
    }

    private static NopException extError(io.nop.api.core.exceptions.ErrorCode code, ErpAstAsset asset,
                                         ErpAstAssetModel model, String key, String detail) {
        NopException e = new NopException(code).param(ErpAstErrors.ARG_ASSET_CODE, asset.getCode());
        if (model != null) {
            e.param(ErpAstErrors.ARG_MODEL_CODE, model.getCode());
        }
        if (key != null) {
            e.param(ErpAstErrors.ARG_EXT_FIELD_KEY, key);
        }
        if (detail != null) {
            e.param(ErpAstErrors.ARG_EXT_FIELD_TYPE, detail);
        }
        return e;
    }

    // ---------- helpers ----------

    private static boolean changed(Map<String, Object> oldValues, String prop) {
        return oldValues.containsKey(prop);
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static BigDecimal toBigDecimal(Object v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        if (v instanceof BigDecimal) {
            return (BigDecimal) v;
        }
        return new BigDecimal(String.valueOf(v));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseJsonMap(String json) {
        if (StringHelper.isEmpty(json)) {
            return null;
        }
        try {
            Object v = JsonTool.parse(json);
            if (v instanceof Map) {
                return (Map<String, Object>) v;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> parseJsonList(String json) {
        if (StringHelper.isEmpty(json)) {
            return null;
        }
        try {
            Object v = JsonTool.parse(json);
            if (v instanceof List) {
                return (List<Map<String, Object>>) v;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
