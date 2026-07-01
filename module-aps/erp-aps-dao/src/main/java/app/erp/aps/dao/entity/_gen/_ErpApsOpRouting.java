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

import app.erp.aps.dao.entity.ErpApsOpRouting;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  替代工艺路线: erp_aps_op_routing
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpApsOpRouting extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 2;
    
    /* 工序: OPERATION_ID BIGINT */
    public static final String PROP_NAME_operationId = "operationId";
    public static final int PROP_ID_operationId = 3;
    
    /* 工作中心: MACHINE_ID BIGINT */
    public static final String PROP_NAME_machineId = "machineId";
    public static final int PROP_ID_machineId = 4;
    
    /* 优先级: PRIORITY INTEGER */
    public static final String PROP_NAME_priority = "priority";
    public static final int PROP_ID_priority = 5;
    
    /* 换模时间差(分钟): SETUP_TIME_DELTA DECIMAL */
    public static final String PROP_NAME_setupTimeDelta = "setupTimeDelta";
    public static final int PROP_ID_setupTimeDelta = 6;
    
    /* 单件加工时间差(分钟): RUNTIME_PER_UNIT_DELTA DECIMAL */
    public static final String PROP_NAME_runtimePerUnitDelta = "runtimePerUnitDelta";
    public static final int PROP_ID_runtimePerUnitDelta = 7;
    
    /* 默认路由: IS_DEFAULT BOOLEAN */
    public static final String PROP_NAME_isDefault = "isDefault";
    public static final int PROP_ID_isDefault = 8;
    
    /* 启用: IS_ENABLED BOOLEAN */
    public static final String PROP_NAME_isEnabled = "isEnabled";
    public static final int PROP_ID_isEnabled = 9;
    
    /* 生效日期: EFFECTIVE_FROM DATE */
    public static final String PROP_NAME_effectiveFrom = "effectiveFrom";
    public static final int PROP_ID_effectiveFrom = 10;
    
    /* 失效日期: EFFECTIVE_TO DATE */
    public static final String PROP_NAME_effectiveTo = "effectiveTo";
    public static final int PROP_ID_effectiveTo = 11;
    
    /* 最小批量: MIN_BATCH_QTY DECIMAL */
    public static final String PROP_NAME_minBatchQty = "minBatchQty";
    public static final int PROP_ID_minBatchQty = 12;
    
    /* 最大批量: MAX_BATCH_QTY DECIMAL */
    public static final String PROP_NAME_maxBatchQty = "maxBatchQty";
    public static final int PROP_ID_maxBatchQty = 13;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 14;
    
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
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[21];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_operationId] = PROP_NAME_operationId;
          PROP_NAME_TO_ID.put(PROP_NAME_operationId, PROP_ID_operationId);
      
          PROP_ID_TO_NAME[PROP_ID_machineId] = PROP_NAME_machineId;
          PROP_NAME_TO_ID.put(PROP_NAME_machineId, PROP_ID_machineId);
      
          PROP_ID_TO_NAME[PROP_ID_priority] = PROP_NAME_priority;
          PROP_NAME_TO_ID.put(PROP_NAME_priority, PROP_ID_priority);
      
          PROP_ID_TO_NAME[PROP_ID_setupTimeDelta] = PROP_NAME_setupTimeDelta;
          PROP_NAME_TO_ID.put(PROP_NAME_setupTimeDelta, PROP_ID_setupTimeDelta);
      
          PROP_ID_TO_NAME[PROP_ID_runtimePerUnitDelta] = PROP_NAME_runtimePerUnitDelta;
          PROP_NAME_TO_ID.put(PROP_NAME_runtimePerUnitDelta, PROP_ID_runtimePerUnitDelta);
      
          PROP_ID_TO_NAME[PROP_ID_isDefault] = PROP_NAME_isDefault;
          PROP_NAME_TO_ID.put(PROP_NAME_isDefault, PROP_ID_isDefault);
      
          PROP_ID_TO_NAME[PROP_ID_isEnabled] = PROP_NAME_isEnabled;
          PROP_NAME_TO_ID.put(PROP_NAME_isEnabled, PROP_ID_isEnabled);
      
          PROP_ID_TO_NAME[PROP_ID_effectiveFrom] = PROP_NAME_effectiveFrom;
          PROP_NAME_TO_ID.put(PROP_NAME_effectiveFrom, PROP_ID_effectiveFrom);
      
          PROP_ID_TO_NAME[PROP_ID_effectiveTo] = PROP_NAME_effectiveTo;
          PROP_NAME_TO_ID.put(PROP_NAME_effectiveTo, PROP_ID_effectiveTo);
      
          PROP_ID_TO_NAME[PROP_ID_minBatchQty] = PROP_NAME_minBatchQty;
          PROP_NAME_TO_ID.put(PROP_NAME_minBatchQty, PROP_ID_minBatchQty);
      
          PROP_ID_TO_NAME[PROP_ID_maxBatchQty] = PROP_NAME_maxBatchQty;
          PROP_NAME_TO_ID.put(PROP_NAME_maxBatchQty, PROP_ID_maxBatchQty);
      
          PROP_ID_TO_NAME[PROP_ID_remark] = PROP_NAME_remark;
          PROP_NAME_TO_ID.put(PROP_NAME_remark, PROP_ID_remark);
      
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
    
    /* 工序: OPERATION_ID */
    private java.lang.Long _operationId;
    
    /* 工作中心: MACHINE_ID */
    private java.lang.Long _machineId;
    
    /* 优先级: PRIORITY */
    private java.lang.Integer _priority;
    
    /* 换模时间差(分钟): SETUP_TIME_DELTA */
    private java.math.BigDecimal _setupTimeDelta;
    
    /* 单件加工时间差(分钟): RUNTIME_PER_UNIT_DELTA */
    private java.math.BigDecimal _runtimePerUnitDelta;
    
    /* 默认路由: IS_DEFAULT */
    private java.lang.Boolean _isDefault;
    
    /* 启用: IS_ENABLED */
    private java.lang.Boolean _isEnabled;
    
    /* 生效日期: EFFECTIVE_FROM */
    private java.time.LocalDate _effectiveFrom;
    
    /* 失效日期: EFFECTIVE_TO */
    private java.time.LocalDate _effectiveTo;
    
    /* 最小批量: MIN_BATCH_QTY */
    private java.math.BigDecimal _minBatchQty;
    
    /* 最大批量: MAX_BATCH_QTY */
    private java.math.BigDecimal _maxBatchQty;
    
    /* 备注: REMARK */
    private java.lang.String _remark;
    
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
    

    public _ErpApsOpRouting(){
        // for debug
    }

    protected ErpApsOpRouting newInstance(){
        ErpApsOpRouting entity = new ErpApsOpRouting();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpApsOpRouting cloneInstance() {
        ErpApsOpRouting entity = newInstance();
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
      return "app.erp.aps.dao.entity.ErpApsOpRouting";
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
        
            case PROP_ID_operationId:
               return getOperationId();
        
            case PROP_ID_machineId:
               return getMachineId();
        
            case PROP_ID_priority:
               return getPriority();
        
            case PROP_ID_setupTimeDelta:
               return getSetupTimeDelta();
        
            case PROP_ID_runtimePerUnitDelta:
               return getRuntimePerUnitDelta();
        
            case PROP_ID_isDefault:
               return getIsDefault();
        
            case PROP_ID_isEnabled:
               return getIsEnabled();
        
            case PROP_ID_effectiveFrom:
               return getEffectiveFrom();
        
            case PROP_ID_effectiveTo:
               return getEffectiveTo();
        
            case PROP_ID_minBatchQty:
               return getMinBatchQty();
        
            case PROP_ID_maxBatchQty:
               return getMaxBatchQty();
        
            case PROP_ID_remark:
               return getRemark();
        
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
        
            case PROP_ID_operationId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_operationId));
               }
               setOperationId(typedValue);
               break;
            }
        
            case PROP_ID_machineId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_machineId));
               }
               setMachineId(typedValue);
               break;
            }
        
            case PROP_ID_priority:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_priority));
               }
               setPriority(typedValue);
               break;
            }
        
            case PROP_ID_setupTimeDelta:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_setupTimeDelta));
               }
               setSetupTimeDelta(typedValue);
               break;
            }
        
            case PROP_ID_runtimePerUnitDelta:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_runtimePerUnitDelta));
               }
               setRuntimePerUnitDelta(typedValue);
               break;
            }
        
            case PROP_ID_isDefault:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isDefault));
               }
               setIsDefault(typedValue);
               break;
            }
        
            case PROP_ID_isEnabled:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isEnabled));
               }
               setIsEnabled(typedValue);
               break;
            }
        
            case PROP_ID_effectiveFrom:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_effectiveFrom));
               }
               setEffectiveFrom(typedValue);
               break;
            }
        
            case PROP_ID_effectiveTo:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_effectiveTo));
               }
               setEffectiveTo(typedValue);
               break;
            }
        
            case PROP_ID_minBatchQty:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_minBatchQty));
               }
               setMinBatchQty(typedValue);
               break;
            }
        
            case PROP_ID_maxBatchQty:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_maxBatchQty));
               }
               setMaxBatchQty(typedValue);
               break;
            }
        
            case PROP_ID_remark:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_remark));
               }
               setRemark(typedValue);
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
        
            case PROP_ID_operationId:{
               onInitProp(propId);
               this._operationId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_machineId:{
               onInitProp(propId);
               this._machineId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_priority:{
               onInitProp(propId);
               this._priority = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_setupTimeDelta:{
               onInitProp(propId);
               this._setupTimeDelta = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_runtimePerUnitDelta:{
               onInitProp(propId);
               this._runtimePerUnitDelta = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_isDefault:{
               onInitProp(propId);
               this._isDefault = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_isEnabled:{
               onInitProp(propId);
               this._isEnabled = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_effectiveFrom:{
               onInitProp(propId);
               this._effectiveFrom = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_effectiveTo:{
               onInitProp(propId);
               this._effectiveTo = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_minBatchQty:{
               onInitProp(propId);
               this._minBatchQty = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_maxBatchQty:{
               onInitProp(propId);
               this._maxBatchQty = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_remark:{
               onInitProp(propId);
               this._remark = (java.lang.String)value;
               
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
     * 工序: OPERATION_ID
     */
    public final java.lang.Long getOperationId(){
         onPropGet(PROP_ID_operationId);
         return _operationId;
    }

    /**
     * 工序: OPERATION_ID
     */
    public final void setOperationId(java.lang.Long value){
        if(onPropSet(PROP_ID_operationId,value)){
            this._operationId = value;
            internalClearRefs(PROP_ID_operationId);
            
        }
    }
    
    /**
     * 工作中心: MACHINE_ID
     */
    public final java.lang.Long getMachineId(){
         onPropGet(PROP_ID_machineId);
         return _machineId;
    }

    /**
     * 工作中心: MACHINE_ID
     */
    public final void setMachineId(java.lang.Long value){
        if(onPropSet(PROP_ID_machineId,value)){
            this._machineId = value;
            internalClearRefs(PROP_ID_machineId);
            
        }
    }
    
    /**
     * 优先级: PRIORITY
     */
    public final java.lang.Integer getPriority(){
         onPropGet(PROP_ID_priority);
         return _priority;
    }

    /**
     * 优先级: PRIORITY
     */
    public final void setPriority(java.lang.Integer value){
        if(onPropSet(PROP_ID_priority,value)){
            this._priority = value;
            internalClearRefs(PROP_ID_priority);
            
        }
    }
    
    /**
     * 换模时间差(分钟): SETUP_TIME_DELTA
     */
    public final java.math.BigDecimal getSetupTimeDelta(){
         onPropGet(PROP_ID_setupTimeDelta);
         return _setupTimeDelta;
    }

    /**
     * 换模时间差(分钟): SETUP_TIME_DELTA
     */
    public final void setSetupTimeDelta(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_setupTimeDelta,value)){
            this._setupTimeDelta = value;
            internalClearRefs(PROP_ID_setupTimeDelta);
            
        }
    }
    
    /**
     * 单件加工时间差(分钟): RUNTIME_PER_UNIT_DELTA
     */
    public final java.math.BigDecimal getRuntimePerUnitDelta(){
         onPropGet(PROP_ID_runtimePerUnitDelta);
         return _runtimePerUnitDelta;
    }

    /**
     * 单件加工时间差(分钟): RUNTIME_PER_UNIT_DELTA
     */
    public final void setRuntimePerUnitDelta(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_runtimePerUnitDelta,value)){
            this._runtimePerUnitDelta = value;
            internalClearRefs(PROP_ID_runtimePerUnitDelta);
            
        }
    }
    
    /**
     * 默认路由: IS_DEFAULT
     */
    public final java.lang.Boolean getIsDefault(){
         onPropGet(PROP_ID_isDefault);
         return _isDefault;
    }

    /**
     * 默认路由: IS_DEFAULT
     */
    public final void setIsDefault(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isDefault,value)){
            this._isDefault = value;
            internalClearRefs(PROP_ID_isDefault);
            
        }
    }
    
    /**
     * 启用: IS_ENABLED
     */
    public final java.lang.Boolean getIsEnabled(){
         onPropGet(PROP_ID_isEnabled);
         return _isEnabled;
    }

    /**
     * 启用: IS_ENABLED
     */
    public final void setIsEnabled(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isEnabled,value)){
            this._isEnabled = value;
            internalClearRefs(PROP_ID_isEnabled);
            
        }
    }
    
    /**
     * 生效日期: EFFECTIVE_FROM
     */
    public final java.time.LocalDate getEffectiveFrom(){
         onPropGet(PROP_ID_effectiveFrom);
         return _effectiveFrom;
    }

    /**
     * 生效日期: EFFECTIVE_FROM
     */
    public final void setEffectiveFrom(java.time.LocalDate value){
        if(onPropSet(PROP_ID_effectiveFrom,value)){
            this._effectiveFrom = value;
            internalClearRefs(PROP_ID_effectiveFrom);
            
        }
    }
    
    /**
     * 失效日期: EFFECTIVE_TO
     */
    public final java.time.LocalDate getEffectiveTo(){
         onPropGet(PROP_ID_effectiveTo);
         return _effectiveTo;
    }

    /**
     * 失效日期: EFFECTIVE_TO
     */
    public final void setEffectiveTo(java.time.LocalDate value){
        if(onPropSet(PROP_ID_effectiveTo,value)){
            this._effectiveTo = value;
            internalClearRefs(PROP_ID_effectiveTo);
            
        }
    }
    
    /**
     * 最小批量: MIN_BATCH_QTY
     */
    public final java.math.BigDecimal getMinBatchQty(){
         onPropGet(PROP_ID_minBatchQty);
         return _minBatchQty;
    }

    /**
     * 最小批量: MIN_BATCH_QTY
     */
    public final void setMinBatchQty(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_minBatchQty,value)){
            this._minBatchQty = value;
            internalClearRefs(PROP_ID_minBatchQty);
            
        }
    }
    
    /**
     * 最大批量: MAX_BATCH_QTY
     */
    public final java.math.BigDecimal getMaxBatchQty(){
         onPropGet(PROP_ID_maxBatchQty);
         return _maxBatchQty;
    }

    /**
     * 最大批量: MAX_BATCH_QTY
     */
    public final void setMaxBatchQty(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_maxBatchQty,value)){
            this._maxBatchQty = value;
            internalClearRefs(PROP_ID_maxBatchQty);
            
        }
    }
    
    /**
     * 备注: REMARK
     */
    public final java.lang.String getRemark(){
         onPropGet(PROP_ID_remark);
         return _remark;
    }

    /**
     * 备注: REMARK
     */
    public final void setRemark(java.lang.String value){
        if(onPropSet(PROP_ID_remark,value)){
            this._remark = value;
            internalClearRefs(PROP_ID_remark);
            
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
       
}
// resume CPD analysis - CPD-ON
