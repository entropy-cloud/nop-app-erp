package app.erp.aps.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.aps.dao.entity.ErpApsDispatchLog;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  派工日志: erp_aps_dispatch_log
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpApsDispatchLog extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 2;
    
    /* 工序工单: OPERATION_ORDER_ID BIGINT */
    public static final String PROP_NAME_operationOrderId = "operationOrderId";
    public static final int PROP_ID_operationOrderId = 3;
    
    /* 工作中心: WORKCENTER_ID BIGINT */
    public static final String PROP_NAME_workcenterId = "workcenterId";
    public static final int PROP_ID_workcenterId = 4;
    
    /* 派工类型: DISPATCH_TYPE VARCHAR */
    public static final String PROP_NAME_dispatchType = "dispatchType";
    public static final int PROP_ID_dispatchType = 5;
    
    /* 派工前状态: PREVIOUS_STATUS VARCHAR */
    public static final String PROP_NAME_previousStatus = "previousStatus";
    public static final int PROP_ID_previousStatus = 6;
    
    /* 派工后状态: NEW_STATUS VARCHAR */
    public static final String PROP_NAME_newStatus = "newStatus";
    public static final int PROP_ID_newStatus = 7;
    
    /* 条件检查结果(JSON): CONDITION_CHECK_RESULT VARCHAR */
    public static final String PROP_NAME_conditionCheckResult = "conditionCheckResult";
    public static final int PROP_ID_conditionCheckResult = 8;
    
    /* 派工人: DISPATCHED_BY VARCHAR */
    public static final String PROP_NAME_dispatchedBy = "dispatchedBy";
    public static final int PROP_ID_dispatchedBy = 9;
    
    /* 派工时间: DISPATCHED_AT TIMESTAMP */
    public static final String PROP_NAME_dispatchedAt = "dispatchedAt";
    public static final int PROP_ID_dispatchedAt = 10;
    
    /* 物料齐套: MATERIAL_AVAILABLE BOOLEAN */
    public static final String PROP_NAME_materialAvailable = "materialAvailable";
    public static final int PROP_ID_materialAvailable = 11;
    
    /* 操作工可用: OPERATOR_AVAILABLE BOOLEAN */
    public static final String PROP_NAME_operatorAvailable = "operatorAvailable";
    public static final int PROP_ID_operatorAvailable = 12;
    
    /* 工装可用: TOOLING_AVAILABLE BOOLEAN */
    public static final String PROP_NAME_toolingAvailable = "toolingAvailable";
    public static final int PROP_ID_toolingAvailable = 13;
    
    /* 备注: NOTE VARCHAR */
    public static final String PROP_NAME_note = "note";
    public static final int PROP_ID_note = 14;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 15;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 16;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 17;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 18;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 19;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 20;
    

    private static int _PROP_ID_BOUND = 21;

    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    
    /* relation:  */
    public static final String PROP_NAME_operationOrder = "operationOrder";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[21];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_operationOrderId] = PROP_NAME_operationOrderId;
          PROP_NAME_TO_ID.put(PROP_NAME_operationOrderId, PROP_ID_operationOrderId);
      
          PROP_ID_TO_NAME[PROP_ID_workcenterId] = PROP_NAME_workcenterId;
          PROP_NAME_TO_ID.put(PROP_NAME_workcenterId, PROP_ID_workcenterId);
      
          PROP_ID_TO_NAME[PROP_ID_dispatchType] = PROP_NAME_dispatchType;
          PROP_NAME_TO_ID.put(PROP_NAME_dispatchType, PROP_ID_dispatchType);
      
          PROP_ID_TO_NAME[PROP_ID_previousStatus] = PROP_NAME_previousStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_previousStatus, PROP_ID_previousStatus);
      
          PROP_ID_TO_NAME[PROP_ID_newStatus] = PROP_NAME_newStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_newStatus, PROP_ID_newStatus);
      
          PROP_ID_TO_NAME[PROP_ID_conditionCheckResult] = PROP_NAME_conditionCheckResult;
          PROP_NAME_TO_ID.put(PROP_NAME_conditionCheckResult, PROP_ID_conditionCheckResult);
      
          PROP_ID_TO_NAME[PROP_ID_dispatchedBy] = PROP_NAME_dispatchedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_dispatchedBy, PROP_ID_dispatchedBy);
      
          PROP_ID_TO_NAME[PROP_ID_dispatchedAt] = PROP_NAME_dispatchedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_dispatchedAt, PROP_ID_dispatchedAt);
      
          PROP_ID_TO_NAME[PROP_ID_materialAvailable] = PROP_NAME_materialAvailable;
          PROP_NAME_TO_ID.put(PROP_NAME_materialAvailable, PROP_ID_materialAvailable);
      
          PROP_ID_TO_NAME[PROP_ID_operatorAvailable] = PROP_NAME_operatorAvailable;
          PROP_NAME_TO_ID.put(PROP_NAME_operatorAvailable, PROP_ID_operatorAvailable);
      
          PROP_ID_TO_NAME[PROP_ID_toolingAvailable] = PROP_NAME_toolingAvailable;
          PROP_NAME_TO_ID.put(PROP_NAME_toolingAvailable, PROP_ID_toolingAvailable);
      
          PROP_ID_TO_NAME[PROP_ID_note] = PROP_NAME_note;
          PROP_NAME_TO_ID.put(PROP_NAME_note, PROP_ID_note);
      
          PROP_ID_TO_NAME[PROP_ID_delVersion] = PROP_NAME_delVersion;
          PROP_NAME_TO_ID.put(PROP_NAME_delVersion, PROP_ID_delVersion);
      
          PROP_ID_TO_NAME[PROP_ID_version] = PROP_NAME_version;
          PROP_NAME_TO_ID.put(PROP_NAME_version, PROP_ID_version);
      
          PROP_ID_TO_NAME[PROP_ID_createdBy] = PROP_NAME_createdBy;
          PROP_NAME_TO_ID.put(PROP_NAME_createdBy, PROP_ID_createdBy);
      
          PROP_ID_TO_NAME[PROP_ID_createTime] = PROP_NAME_createTime;
          PROP_NAME_TO_ID.put(PROP_NAME_createTime, PROP_ID_createTime);
      
          PROP_ID_TO_NAME[PROP_ID_updatedBy] = PROP_NAME_updatedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_updatedBy, PROP_ID_updatedBy);
      
          PROP_ID_TO_NAME[PROP_ID_updateTime] = PROP_NAME_updateTime;
          PROP_NAME_TO_ID.put(PROP_NAME_updateTime, PROP_ID_updateTime);
      
    }

    
    /* ID: ID */
    private java.lang.Long _id;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 工序工单: OPERATION_ORDER_ID */
    private java.lang.Long _operationOrderId;
    
    /* 工作中心: WORKCENTER_ID */
    private java.lang.Long _workcenterId;
    
    /* 派工类型: DISPATCH_TYPE */
    private java.lang.String _dispatchType;
    
    /* 派工前状态: PREVIOUS_STATUS */
    private java.lang.String _previousStatus;
    
    /* 派工后状态: NEW_STATUS */
    private java.lang.String _newStatus;
    
    /* 条件检查结果(JSON): CONDITION_CHECK_RESULT */
    private java.lang.String _conditionCheckResult;
    
    /* 派工人: DISPATCHED_BY */
    private java.lang.String _dispatchedBy;
    
    /* 派工时间: DISPATCHED_AT */
    private java.sql.Timestamp _dispatchedAt;
    
    /* 物料齐套: MATERIAL_AVAILABLE */
    private java.lang.Boolean _materialAvailable;
    
    /* 操作工可用: OPERATOR_AVAILABLE */
    private java.lang.Boolean _operatorAvailable;
    
    /* 工装可用: TOOLING_AVAILABLE */
    private java.lang.Boolean _toolingAvailable;
    
    /* 备注: NOTE */
    private java.lang.String _note;
    
    /* 逻辑删除版本: DEL_VERSION */
    private java.lang.Long _delVersion;
    
    /* 数据版本: VERSION */
    private java.lang.Integer _version;
    
    /* 创建人: CREATED_BY */
    private java.lang.String _createdBy;
    
    /* 创建时间: CREATE_TIME */
    private java.sql.Timestamp _createTime;
    
    /* 修改人: UPDATED_BY */
    private java.lang.String _updatedBy;
    
    /* 修改时间: UPDATE_TIME */
    private java.sql.Timestamp _updateTime;
    

    public _ErpApsDispatchLog(){
        // for debug
    }

    protected ErpApsDispatchLog newInstance(){
        ErpApsDispatchLog entity = new ErpApsDispatchLog();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpApsDispatchLog cloneInstance() {
        ErpApsDispatchLog entity = newInstance();
        orm_forEachInitedProp((value, propId) -> {
            entity.orm_propValue(propId,value);
        });
        return entity;
    }

    @Override
    public String orm_entityName() {
      // 如果存在实体模型对象，则以模型对象上的设置为准
      IEntityModel entityModel = orm_entityModel();
      if(entityModel != null)
          return entityModel.getName();
      return "app.erp.aps.dao.entity.ErpApsDispatchLog";
    }

    @Override
    public int orm_propIdBound(){
      IEntityModel entityModel = orm_entityModel();
      if(entityModel != null)
          return entityModel.getPropIdBound();
      return _PROP_ID_BOUND;
    }

    @Override
    public Object orm_id() {
    
        return buildSimpleId(PROP_ID_id);
     
    }

    @Override
    public boolean orm_isPrimary(int propId) {
        
            return propId == PROP_ID_id;
          
    }

    @Override
    public String orm_propName(int propId) {
        if(propId >= PROP_ID_TO_NAME.length)
            return super.orm_propName(propId);
        String propName = PROP_ID_TO_NAME[propId];
        if(propName == null)
           return super.orm_propName(propId);
        return propName;
    }

    @Override
    public int orm_propId(String propName) {
        Integer propId = PROP_NAME_TO_ID.get(propName);
        if(propId == null)
            return super.orm_propId(propName);
        return propId;
    }

    @Override
    public Object orm_propValue(int propId) {
        switch(propId){
        
            case PROP_ID_id:
               return getId();
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_operationOrderId:
               return getOperationOrderId();
        
            case PROP_ID_workcenterId:
               return getWorkcenterId();
        
            case PROP_ID_dispatchType:
               return getDispatchType();
        
            case PROP_ID_previousStatus:
               return getPreviousStatus();
        
            case PROP_ID_newStatus:
               return getNewStatus();
        
            case PROP_ID_conditionCheckResult:
               return getConditionCheckResult();
        
            case PROP_ID_dispatchedBy:
               return getDispatchedBy();
        
            case PROP_ID_dispatchedAt:
               return getDispatchedAt();
        
            case PROP_ID_materialAvailable:
               return getMaterialAvailable();
        
            case PROP_ID_operatorAvailable:
               return getOperatorAvailable();
        
            case PROP_ID_toolingAvailable:
               return getToolingAvailable();
        
            case PROP_ID_note:
               return getNote();
        
            case PROP_ID_delVersion:
               return getDelVersion();
        
            case PROP_ID_version:
               return getVersion();
        
            case PROP_ID_createdBy:
               return getCreatedBy();
        
            case PROP_ID_createTime:
               return getCreateTime();
        
            case PROP_ID_updatedBy:
               return getUpdatedBy();
        
            case PROP_ID_updateTime:
               return getUpdateTime();
        
           default:
              return super.orm_propValue(propId);
        }
    }

    

    @Override
    public void orm_propValue(int propId, Object value){
        switch(propId){
        
            case PROP_ID_id:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_id));
               }
               setId(typedValue);
               break;
            }
        
            case PROP_ID_orgId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_orgId));
               }
               setOrgId(typedValue);
               break;
            }
        
            case PROP_ID_operationOrderId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_operationOrderId));
               }
               setOperationOrderId(typedValue);
               break;
            }
        
            case PROP_ID_workcenterId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_workcenterId));
               }
               setWorkcenterId(typedValue);
               break;
            }
        
            case PROP_ID_dispatchType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_dispatchType));
               }
               setDispatchType(typedValue);
               break;
            }
        
            case PROP_ID_previousStatus:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_previousStatus));
               }
               setPreviousStatus(typedValue);
               break;
            }
        
            case PROP_ID_newStatus:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_newStatus));
               }
               setNewStatus(typedValue);
               break;
            }
        
            case PROP_ID_conditionCheckResult:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_conditionCheckResult));
               }
               setConditionCheckResult(typedValue);
               break;
            }
        
            case PROP_ID_dispatchedBy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_dispatchedBy));
               }
               setDispatchedBy(typedValue);
               break;
            }
        
            case PROP_ID_dispatchedAt:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_dispatchedAt));
               }
               setDispatchedAt(typedValue);
               break;
            }
        
            case PROP_ID_materialAvailable:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_materialAvailable));
               }
               setMaterialAvailable(typedValue);
               break;
            }
        
            case PROP_ID_operatorAvailable:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_operatorAvailable));
               }
               setOperatorAvailable(typedValue);
               break;
            }
        
            case PROP_ID_toolingAvailable:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_toolingAvailable));
               }
               setToolingAvailable(typedValue);
               break;
            }
        
            case PROP_ID_note:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_note));
               }
               setNote(typedValue);
               break;
            }
        
            case PROP_ID_delVersion:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_delVersion));
               }
               setDelVersion(typedValue);
               break;
            }
        
            case PROP_ID_version:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_version));
               }
               setVersion(typedValue);
               break;
            }
        
            case PROP_ID_createdBy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_createdBy));
               }
               setCreatedBy(typedValue);
               break;
            }
        
            case PROP_ID_createTime:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_createTime));
               }
               setCreateTime(typedValue);
               break;
            }
        
            case PROP_ID_updatedBy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_updatedBy));
               }
               setUpdatedBy(typedValue);
               break;
            }
        
            case PROP_ID_updateTime:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_updateTime));
               }
               setUpdateTime(typedValue);
               break;
            }
        
           default:
              super.orm_propValue(propId,value);
        }
    }

    @Override
    public void orm_internalSet(int propId, Object value) {
        switch(propId){
        
            case PROP_ID_id:{
               onInitProp(propId);
               this._id = (java.lang.Long)value;
               orm_id(); // 如果是设置主键字段，则触发watcher
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_operationOrderId:{
               onInitProp(propId);
               this._operationOrderId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_workcenterId:{
               onInitProp(propId);
               this._workcenterId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_dispatchType:{
               onInitProp(propId);
               this._dispatchType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_previousStatus:{
               onInitProp(propId);
               this._previousStatus = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_newStatus:{
               onInitProp(propId);
               this._newStatus = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_conditionCheckResult:{
               onInitProp(propId);
               this._conditionCheckResult = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_dispatchedBy:{
               onInitProp(propId);
               this._dispatchedBy = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_dispatchedAt:{
               onInitProp(propId);
               this._dispatchedAt = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_materialAvailable:{
               onInitProp(propId);
               this._materialAvailable = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_operatorAvailable:{
               onInitProp(propId);
               this._operatorAvailable = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_toolingAvailable:{
               onInitProp(propId);
               this._toolingAvailable = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_note:{
               onInitProp(propId);
               this._note = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_delVersion:{
               onInitProp(propId);
               this._delVersion = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_version:{
               onInitProp(propId);
               this._version = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_createdBy:{
               onInitProp(propId);
               this._createdBy = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_createTime:{
               onInitProp(propId);
               this._createTime = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_updatedBy:{
               onInitProp(propId);
               this._updatedBy = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_updateTime:{
               onInitProp(propId);
               this._updateTime = (java.sql.Timestamp)value;
               
               break;
            }
        
           default:
              super.orm_internalSet(propId,value);
        }
    }

    
    /**
     * ID: ID
     */
    public final java.lang.Long getId(){
         onPropGet(PROP_ID_id);
         return _id;
    }

    /**
     * ID: ID
     */
    public final void setId(java.lang.Long value){
        if(onPropSet(PROP_ID_id,value)){
            this._id = value;
            internalClearRefs(PROP_ID_id);
            orm_id();
        }
    }
    
    /**
     * 业务组织: ORG_ID
     */
    public final java.lang.Long getOrgId(){
         onPropGet(PROP_ID_orgId);
         return _orgId;
    }

    /**
     * 业务组织: ORG_ID
     */
    public final void setOrgId(java.lang.Long value){
        if(onPropSet(PROP_ID_orgId,value)){
            this._orgId = value;
            internalClearRefs(PROP_ID_orgId);
            
        }
    }
    
    /**
     * 工序工单: OPERATION_ORDER_ID
     */
    public final java.lang.Long getOperationOrderId(){
         onPropGet(PROP_ID_operationOrderId);
         return _operationOrderId;
    }

    /**
     * 工序工单: OPERATION_ORDER_ID
     */
    public final void setOperationOrderId(java.lang.Long value){
        if(onPropSet(PROP_ID_operationOrderId,value)){
            this._operationOrderId = value;
            internalClearRefs(PROP_ID_operationOrderId);
            
        }
    }
    
    /**
     * 工作中心: WORKCENTER_ID
     */
    public final java.lang.Long getWorkcenterId(){
         onPropGet(PROP_ID_workcenterId);
         return _workcenterId;
    }

    /**
     * 工作中心: WORKCENTER_ID
     */
    public final void setWorkcenterId(java.lang.Long value){
        if(onPropSet(PROP_ID_workcenterId,value)){
            this._workcenterId = value;
            internalClearRefs(PROP_ID_workcenterId);
            
        }
    }
    
    /**
     * 派工类型: DISPATCH_TYPE
     */
    public final java.lang.String getDispatchType(){
         onPropGet(PROP_ID_dispatchType);
         return _dispatchType;
    }

    /**
     * 派工类型: DISPATCH_TYPE
     */
    public final void setDispatchType(java.lang.String value){
        if(onPropSet(PROP_ID_dispatchType,value)){
            this._dispatchType = value;
            internalClearRefs(PROP_ID_dispatchType);
            
        }
    }
    
    /**
     * 派工前状态: PREVIOUS_STATUS
     */
    public final java.lang.String getPreviousStatus(){
         onPropGet(PROP_ID_previousStatus);
         return _previousStatus;
    }

    /**
     * 派工前状态: PREVIOUS_STATUS
     */
    public final void setPreviousStatus(java.lang.String value){
        if(onPropSet(PROP_ID_previousStatus,value)){
            this._previousStatus = value;
            internalClearRefs(PROP_ID_previousStatus);
            
        }
    }
    
    /**
     * 派工后状态: NEW_STATUS
     */
    public final java.lang.String getNewStatus(){
         onPropGet(PROP_ID_newStatus);
         return _newStatus;
    }

    /**
     * 派工后状态: NEW_STATUS
     */
    public final void setNewStatus(java.lang.String value){
        if(onPropSet(PROP_ID_newStatus,value)){
            this._newStatus = value;
            internalClearRefs(PROP_ID_newStatus);
            
        }
    }
    
    /**
     * 条件检查结果(JSON): CONDITION_CHECK_RESULT
     */
    public final java.lang.String getConditionCheckResult(){
         onPropGet(PROP_ID_conditionCheckResult);
         return _conditionCheckResult;
    }

    /**
     * 条件检查结果(JSON): CONDITION_CHECK_RESULT
     */
    public final void setConditionCheckResult(java.lang.String value){
        if(onPropSet(PROP_ID_conditionCheckResult,value)){
            this._conditionCheckResult = value;
            internalClearRefs(PROP_ID_conditionCheckResult);
            
        }
    }
    
    /**
     * 派工人: DISPATCHED_BY
     */
    public final java.lang.String getDispatchedBy(){
         onPropGet(PROP_ID_dispatchedBy);
         return _dispatchedBy;
    }

    /**
     * 派工人: DISPATCHED_BY
     */
    public final void setDispatchedBy(java.lang.String value){
        if(onPropSet(PROP_ID_dispatchedBy,value)){
            this._dispatchedBy = value;
            internalClearRefs(PROP_ID_dispatchedBy);
            
        }
    }
    
    /**
     * 派工时间: DISPATCHED_AT
     */
    public final java.sql.Timestamp getDispatchedAt(){
         onPropGet(PROP_ID_dispatchedAt);
         return _dispatchedAt;
    }

    /**
     * 派工时间: DISPATCHED_AT
     */
    public final void setDispatchedAt(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_dispatchedAt,value)){
            this._dispatchedAt = value;
            internalClearRefs(PROP_ID_dispatchedAt);
            
        }
    }
    
    /**
     * 物料齐套: MATERIAL_AVAILABLE
     */
    public final java.lang.Boolean getMaterialAvailable(){
         onPropGet(PROP_ID_materialAvailable);
         return _materialAvailable;
    }

    /**
     * 物料齐套: MATERIAL_AVAILABLE
     */
    public final void setMaterialAvailable(java.lang.Boolean value){
        if(onPropSet(PROP_ID_materialAvailable,value)){
            this._materialAvailable = value;
            internalClearRefs(PROP_ID_materialAvailable);
            
        }
    }
    
    /**
     * 操作工可用: OPERATOR_AVAILABLE
     */
    public final java.lang.Boolean getOperatorAvailable(){
         onPropGet(PROP_ID_operatorAvailable);
         return _operatorAvailable;
    }

    /**
     * 操作工可用: OPERATOR_AVAILABLE
     */
    public final void setOperatorAvailable(java.lang.Boolean value){
        if(onPropSet(PROP_ID_operatorAvailable,value)){
            this._operatorAvailable = value;
            internalClearRefs(PROP_ID_operatorAvailable);
            
        }
    }
    
    /**
     * 工装可用: TOOLING_AVAILABLE
     */
    public final java.lang.Boolean getToolingAvailable(){
         onPropGet(PROP_ID_toolingAvailable);
         return _toolingAvailable;
    }

    /**
     * 工装可用: TOOLING_AVAILABLE
     */
    public final void setToolingAvailable(java.lang.Boolean value){
        if(onPropSet(PROP_ID_toolingAvailable,value)){
            this._toolingAvailable = value;
            internalClearRefs(PROP_ID_toolingAvailable);
            
        }
    }
    
    /**
     * 备注: NOTE
     */
    public final java.lang.String getNote(){
         onPropGet(PROP_ID_note);
         return _note;
    }

    /**
     * 备注: NOTE
     */
    public final void setNote(java.lang.String value){
        if(onPropSet(PROP_ID_note,value)){
            this._note = value;
            internalClearRefs(PROP_ID_note);
            
        }
    }
    
    /**
     * 逻辑删除版本: DEL_VERSION
     */
    public final java.lang.Long getDelVersion(){
         onPropGet(PROP_ID_delVersion);
         return _delVersion;
    }

    /**
     * 逻辑删除版本: DEL_VERSION
     */
    public final void setDelVersion(java.lang.Long value){
        if(onPropSet(PROP_ID_delVersion,value)){
            this._delVersion = value;
            internalClearRefs(PROP_ID_delVersion);
            
        }
    }
    
    /**
     * 数据版本: VERSION
     */
    public final java.lang.Integer getVersion(){
         onPropGet(PROP_ID_version);
         return _version;
    }

    /**
     * 数据版本: VERSION
     */
    public final void setVersion(java.lang.Integer value){
        if(onPropSet(PROP_ID_version,value)){
            this._version = value;
            internalClearRefs(PROP_ID_version);
            
        }
    }
    
    /**
     * 创建人: CREATED_BY
     */
    public final java.lang.String getCreatedBy(){
         onPropGet(PROP_ID_createdBy);
         return _createdBy;
    }

    /**
     * 创建人: CREATED_BY
     */
    public final void setCreatedBy(java.lang.String value){
        if(onPropSet(PROP_ID_createdBy,value)){
            this._createdBy = value;
            internalClearRefs(PROP_ID_createdBy);
            
        }
    }
    
    /**
     * 创建时间: CREATE_TIME
     */
    public final java.sql.Timestamp getCreateTime(){
         onPropGet(PROP_ID_createTime);
         return _createTime;
    }

    /**
     * 创建时间: CREATE_TIME
     */
    public final void setCreateTime(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_createTime,value)){
            this._createTime = value;
            internalClearRefs(PROP_ID_createTime);
            
        }
    }
    
    /**
     * 修改人: UPDATED_BY
     */
    public final java.lang.String getUpdatedBy(){
         onPropGet(PROP_ID_updatedBy);
         return _updatedBy;
    }

    /**
     * 修改人: UPDATED_BY
     */
    public final void setUpdatedBy(java.lang.String value){
        if(onPropSet(PROP_ID_updatedBy,value)){
            this._updatedBy = value;
            internalClearRefs(PROP_ID_updatedBy);
            
        }
    }
    
    /**
     * 修改时间: UPDATE_TIME
     */
    public final java.sql.Timestamp getUpdateTime(){
         onPropGet(PROP_ID_updateTime);
         return _updateTime;
    }

    /**
     * 修改时间: UPDATE_TIME
     */
    public final void setUpdateTime(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_updateTime,value)){
            this._updateTime = value;
            internalClearRefs(PROP_ID_updateTime);
            
        }
    }
    
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdOrganization getOrg(){
       return (app.erp.md.dao.entity.ErpMdOrganization)internalGetRefEntity(PROP_NAME_org);
    }

    public final void setOrg(app.erp.md.dao.entity.ErpMdOrganization refEntity){
   
           if(refEntity == null){
           
                   this.setOrgId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_org, refEntity,()->{
           
                           this.setOrgId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.aps.dao.entity.ErpApsOperationOrder getOperationOrder(){
       return (app.erp.aps.dao.entity.ErpApsOperationOrder)internalGetRefEntity(PROP_NAME_operationOrder);
    }

    public final void setOperationOrder(app.erp.aps.dao.entity.ErpApsOperationOrder refEntity){
   
           if(refEntity == null){
           
                   this.setOperationOrderId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_operationOrder, refEntity,()->{
           
                           this.setOperationOrderId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
