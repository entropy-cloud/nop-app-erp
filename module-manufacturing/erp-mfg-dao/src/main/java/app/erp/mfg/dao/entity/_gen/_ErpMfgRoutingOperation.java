package app.erp.mfg.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.mfg.dao.entity.ErpMfgRoutingOperation;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  工艺路线工序: erp_mfg_routing_operation
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpMfgRoutingOperation extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 工艺路线ID: ROUTING_ID BIGINT */
    public static final String PROP_NAME_routingId = "routingId";
    public static final int PROP_ID_routingId = 2;
    
    /* 行号: LINE_NO INTEGER */
    public static final String PROP_NAME_lineNo = "lineNo";
    public static final int PROP_ID_lineNo = 3;
    
    /* 工序编码: OPERATION_CODE VARCHAR */
    public static final String PROP_NAME_operationCode = "operationCode";
    public static final int PROP_ID_operationCode = 4;
    
    /* 工序名称: OPERATION_NAME VARCHAR */
    public static final String PROP_NAME_operationName = "operationName";
    public static final int PROP_ID_operationName = 5;
    
    /* 工作中心: WORKCENTER_ID BIGINT */
    public static final String PROP_NAME_workcenterId = "workcenterId";
    public static final int PROP_ID_workcenterId = 6;
    
    /* 标准工时: STANDARD_TIME DECIMAL */
    public static final String PROP_NAME_standardTime = "standardTime";
    public static final int PROP_ID_standardTime = 7;
    
    /* 时间单位: TIME_UNIT VARCHAR */
    public static final String PROP_NAME_timeUnit = "timeUnit";
    public static final int PROP_ID_timeUnit = 8;
    
    /* 准备时间: SETUP_TIME DECIMAL */
    public static final String PROP_NAME_setupTime = "setupTime";
    public static final int PROP_ID_setupTime = 9;
    
    /* 加工时间: RUN_TIME DECIMAL */
    public static final String PROP_NAME_runTime = "runTime";
    public static final int PROP_ID_runTime = 10;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 11;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 12;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 13;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 14;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 15;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 16;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 17;
    

    private static int _PROP_ID_BOUND = 18;

    
    /* relation:  */
    public static final String PROP_NAME_routing = "routing";
    
    /* relation:  */
    public static final String PROP_NAME_workcenter = "workcenter";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[18];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_routingId] = PROP_NAME_routingId;
          PROP_NAME_TO_ID.put(PROP_NAME_routingId, PROP_ID_routingId);
      
          PROP_ID_TO_NAME[PROP_ID_lineNo] = PROP_NAME_lineNo;
          PROP_NAME_TO_ID.put(PROP_NAME_lineNo, PROP_ID_lineNo);
      
          PROP_ID_TO_NAME[PROP_ID_operationCode] = PROP_NAME_operationCode;
          PROP_NAME_TO_ID.put(PROP_NAME_operationCode, PROP_ID_operationCode);
      
          PROP_ID_TO_NAME[PROP_ID_operationName] = PROP_NAME_operationName;
          PROP_NAME_TO_ID.put(PROP_NAME_operationName, PROP_ID_operationName);
      
          PROP_ID_TO_NAME[PROP_ID_workcenterId] = PROP_NAME_workcenterId;
          PROP_NAME_TO_ID.put(PROP_NAME_workcenterId, PROP_ID_workcenterId);
      
          PROP_ID_TO_NAME[PROP_ID_standardTime] = PROP_NAME_standardTime;
          PROP_NAME_TO_ID.put(PROP_NAME_standardTime, PROP_ID_standardTime);
      
          PROP_ID_TO_NAME[PROP_ID_timeUnit] = PROP_NAME_timeUnit;
          PROP_NAME_TO_ID.put(PROP_NAME_timeUnit, PROP_ID_timeUnit);
      
          PROP_ID_TO_NAME[PROP_ID_setupTime] = PROP_NAME_setupTime;
          PROP_NAME_TO_ID.put(PROP_NAME_setupTime, PROP_ID_setupTime);
      
          PROP_ID_TO_NAME[PROP_ID_runTime] = PROP_NAME_runTime;
          PROP_NAME_TO_ID.put(PROP_NAME_runTime, PROP_ID_runTime);
      
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
    
    /* 工艺路线ID: ROUTING_ID */
    private java.lang.Long _routingId;
    
    /* 行号: LINE_NO */
    private java.lang.Integer _lineNo;
    
    /* 工序编码: OPERATION_CODE */
    private java.lang.String _operationCode;
    
    /* 工序名称: OPERATION_NAME */
    private java.lang.String _operationName;
    
    /* 工作中心: WORKCENTER_ID */
    private java.lang.Long _workcenterId;
    
    /* 标准工时: STANDARD_TIME */
    private java.time.LocalDateTime _standardTime;
    
    /* 时间单位: TIME_UNIT */
    private java.lang.String _timeUnit;
    
    /* 准备时间: SETUP_TIME */
    private java.time.LocalDateTime _setupTime;
    
    /* 加工时间: RUN_TIME */
    private java.time.LocalDateTime _runTime;
    
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
    

    public _ErpMfgRoutingOperation(){
        // for debug
    }

    protected ErpMfgRoutingOperation newInstance(){
        ErpMfgRoutingOperation entity = new ErpMfgRoutingOperation();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpMfgRoutingOperation cloneInstance() {
        ErpMfgRoutingOperation entity = newInstance();
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
      return "app.erp.mfg.dao.entity.ErpMfgRoutingOperation";
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
        
            case PROP_ID_routingId:
               return getRoutingId();
        
            case PROP_ID_lineNo:
               return getLineNo();
        
            case PROP_ID_operationCode:
               return getOperationCode();
        
            case PROP_ID_operationName:
               return getOperationName();
        
            case PROP_ID_workcenterId:
               return getWorkcenterId();
        
            case PROP_ID_standardTime:
               return getStandardTime();
        
            case PROP_ID_timeUnit:
               return getTimeUnit();
        
            case PROP_ID_setupTime:
               return getSetupTime();
        
            case PROP_ID_runTime:
               return getRunTime();
        
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
        
            case PROP_ID_routingId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_routingId));
               }
               setRoutingId(typedValue);
               break;
            }
        
            case PROP_ID_lineNo:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_lineNo));
               }
               setLineNo(typedValue);
               break;
            }
        
            case PROP_ID_operationCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_operationCode));
               }
               setOperationCode(typedValue);
               break;
            }
        
            case PROP_ID_operationName:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_operationName));
               }
               setOperationName(typedValue);
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
        
            case PROP_ID_standardTime:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_standardTime));
               }
               setStandardTime(typedValue);
               break;
            }
        
            case PROP_ID_timeUnit:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_timeUnit));
               }
               setTimeUnit(typedValue);
               break;
            }
        
            case PROP_ID_setupTime:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_setupTime));
               }
               setSetupTime(typedValue);
               break;
            }
        
            case PROP_ID_runTime:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_runTime));
               }
               setRunTime(typedValue);
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
        
            case PROP_ID_routingId:{
               onInitProp(propId);
               this._routingId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_lineNo:{
               onInitProp(propId);
               this._lineNo = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_operationCode:{
               onInitProp(propId);
               this._operationCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_operationName:{
               onInitProp(propId);
               this._operationName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_workcenterId:{
               onInitProp(propId);
               this._workcenterId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_standardTime:{
               onInitProp(propId);
               this._standardTime = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_timeUnit:{
               onInitProp(propId);
               this._timeUnit = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_setupTime:{
               onInitProp(propId);
               this._setupTime = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_runTime:{
               onInitProp(propId);
               this._runTime = (java.time.LocalDateTime)value;
               
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
     * 工艺路线ID: ROUTING_ID
     */
    public final java.lang.Long getRoutingId(){
         onPropGet(PROP_ID_routingId);
         return _routingId;
    }

    /**
     * 工艺路线ID: ROUTING_ID
     */
    public final void setRoutingId(java.lang.Long value){
        if(onPropSet(PROP_ID_routingId,value)){
            this._routingId = value;
            internalClearRefs(PROP_ID_routingId);
            
        }
    }
    
    /**
     * 行号: LINE_NO
     */
    public final java.lang.Integer getLineNo(){
         onPropGet(PROP_ID_lineNo);
         return _lineNo;
    }

    /**
     * 行号: LINE_NO
     */
    public final void setLineNo(java.lang.Integer value){
        if(onPropSet(PROP_ID_lineNo,value)){
            this._lineNo = value;
            internalClearRefs(PROP_ID_lineNo);
            
        }
    }
    
    /**
     * 工序编码: OPERATION_CODE
     */
    public final java.lang.String getOperationCode(){
         onPropGet(PROP_ID_operationCode);
         return _operationCode;
    }

    /**
     * 工序编码: OPERATION_CODE
     */
    public final void setOperationCode(java.lang.String value){
        if(onPropSet(PROP_ID_operationCode,value)){
            this._operationCode = value;
            internalClearRefs(PROP_ID_operationCode);
            
        }
    }
    
    /**
     * 工序名称: OPERATION_NAME
     */
    public final java.lang.String getOperationName(){
         onPropGet(PROP_ID_operationName);
         return _operationName;
    }

    /**
     * 工序名称: OPERATION_NAME
     */
    public final void setOperationName(java.lang.String value){
        if(onPropSet(PROP_ID_operationName,value)){
            this._operationName = value;
            internalClearRefs(PROP_ID_operationName);
            
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
     * 标准工时: STANDARD_TIME
     */
    public final java.time.LocalDateTime getStandardTime(){
         onPropGet(PROP_ID_standardTime);
         return _standardTime;
    }

    /**
     * 标准工时: STANDARD_TIME
     */
    public final void setStandardTime(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_standardTime,value)){
            this._standardTime = value;
            internalClearRefs(PROP_ID_standardTime);
            
        }
    }
    
    /**
     * 时间单位: TIME_UNIT
     */
    public final java.lang.String getTimeUnit(){
         onPropGet(PROP_ID_timeUnit);
         return _timeUnit;
    }

    /**
     * 时间单位: TIME_UNIT
     */
    public final void setTimeUnit(java.lang.String value){
        if(onPropSet(PROP_ID_timeUnit,value)){
            this._timeUnit = value;
            internalClearRefs(PROP_ID_timeUnit);
            
        }
    }
    
    /**
     * 准备时间: SETUP_TIME
     */
    public final java.time.LocalDateTime getSetupTime(){
         onPropGet(PROP_ID_setupTime);
         return _setupTime;
    }

    /**
     * 准备时间: SETUP_TIME
     */
    public final void setSetupTime(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_setupTime,value)){
            this._setupTime = value;
            internalClearRefs(PROP_ID_setupTime);
            
        }
    }
    
    /**
     * 加工时间: RUN_TIME
     */
    public final java.time.LocalDateTime getRunTime(){
         onPropGet(PROP_ID_runTime);
         return _runTime;
    }

    /**
     * 加工时间: RUN_TIME
     */
    public final void setRunTime(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_runTime,value)){
            this._runTime = value;
            internalClearRefs(PROP_ID_runTime);
            
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
    public final app.erp.mfg.dao.entity.ErpMfgRouting getRouting(){
       return (app.erp.mfg.dao.entity.ErpMfgRouting)internalGetRefEntity(PROP_NAME_routing);
    }

    public final void setRouting(app.erp.mfg.dao.entity.ErpMfgRouting refEntity){
   
           if(refEntity == null){
           
                   this.setRoutingId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_routing, refEntity,()->{
           
                           this.setRoutingId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.mfg.dao.entity.ErpMfgWorkcenter getWorkcenter(){
       return (app.erp.mfg.dao.entity.ErpMfgWorkcenter)internalGetRefEntity(PROP_NAME_workcenter);
    }

    public final void setWorkcenter(app.erp.mfg.dao.entity.ErpMfgWorkcenter refEntity){
   
           if(refEntity == null){
           
                   this.setWorkcenterId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_workcenter, refEntity,()->{
           
                           this.setWorkcenterId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
