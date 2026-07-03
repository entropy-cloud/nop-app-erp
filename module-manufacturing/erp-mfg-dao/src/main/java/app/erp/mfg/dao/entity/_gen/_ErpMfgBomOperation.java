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

import app.erp.mfg.dao.entity.ErpMfgBomOperation;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  BOM工艺: erp_mfg_bom_operation
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpMfgBomOperation extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* BOM ID: BOM_ID BIGINT */
    public static final String PROP_NAME_bomId = "bomId";
    public static final int PROP_ID_bomId = 2;
    
    /* 行号: LINE_NO INTEGER */
    public static final String PROP_NAME_lineNo = "lineNo";
    public static final int PROP_ID_lineNo = 3;
    
    /* 工序ID: OPERATION_ID BIGINT */
    public static final String PROP_NAME_operationId = "operationId";
    public static final int PROP_ID_operationId = 4;
    
    /* 工作中心: WORKCENTER_ID BIGINT */
    public static final String PROP_NAME_workcenterId = "workcenterId";
    public static final int PROP_ID_workcenterId = 5;
    
    /* 标准工时: STANDARD_TIME DECIMAL */
    public static final String PROP_NAME_standardTime = "standardTime";
    public static final int PROP_ID_standardTime = 6;
    
    /* 时间单位: TIME_UNIT VARCHAR */
    public static final String PROP_NAME_timeUnit = "timeUnit";
    public static final int PROP_ID_timeUnit = 7;
    
    /* 效率系数: RATE DECIMAL */
    public static final String PROP_NAME_rate = "rate";
    public static final int PROP_ID_rate = 8;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 9;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 10;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 11;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 12;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 13;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 14;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 15;
    

    private static int _PROP_ID_BOUND = 16;

    
    /* relation:  */
    public static final String PROP_NAME_bom = "bom";
    
    /* relation:  */
    public static final String PROP_NAME_workcenter = "workcenter";
    
    /* relation:  */
    public static final String PROP_NAME_operation = "operation";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[16];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_bomId] = PROP_NAME_bomId;
          PROP_NAME_TO_ID.put(PROP_NAME_bomId, PROP_ID_bomId);
      
          PROP_ID_TO_NAME[PROP_ID_lineNo] = PROP_NAME_lineNo;
          PROP_NAME_TO_ID.put(PROP_NAME_lineNo, PROP_ID_lineNo);
      
          PROP_ID_TO_NAME[PROP_ID_operationId] = PROP_NAME_operationId;
          PROP_NAME_TO_ID.put(PROP_NAME_operationId, PROP_ID_operationId);
      
          PROP_ID_TO_NAME[PROP_ID_workcenterId] = PROP_NAME_workcenterId;
          PROP_NAME_TO_ID.put(PROP_NAME_workcenterId, PROP_ID_workcenterId);
      
          PROP_ID_TO_NAME[PROP_ID_standardTime] = PROP_NAME_standardTime;
          PROP_NAME_TO_ID.put(PROP_NAME_standardTime, PROP_ID_standardTime);
      
          PROP_ID_TO_NAME[PROP_ID_timeUnit] = PROP_NAME_timeUnit;
          PROP_NAME_TO_ID.put(PROP_NAME_timeUnit, PROP_ID_timeUnit);
      
          PROP_ID_TO_NAME[PROP_ID_rate] = PROP_NAME_rate;
          PROP_NAME_TO_ID.put(PROP_NAME_rate, PROP_ID_rate);
      
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
    
    /* BOM ID: BOM_ID */
    private java.lang.Long _bomId;
    
    /* 行号: LINE_NO */
    private java.lang.Integer _lineNo;
    
    /* 工序ID: OPERATION_ID */
    private java.lang.Long _operationId;
    
    /* 工作中心: WORKCENTER_ID */
    private java.lang.Long _workcenterId;
    
    /* 标准工时: STANDARD_TIME */
    private java.math.BigDecimal _standardTime;
    
    /* 时间单位: TIME_UNIT */
    private java.lang.String _timeUnit;
    
    /* 效率系数: RATE */
    private java.math.BigDecimal _rate;
    
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
    

    public _ErpMfgBomOperation(){
        // for debug
    }

    protected ErpMfgBomOperation newInstance(){
        ErpMfgBomOperation entity = new ErpMfgBomOperation();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpMfgBomOperation cloneInstance() {
        ErpMfgBomOperation entity = newInstance();
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
      return "app.erp.mfg.dao.entity.ErpMfgBomOperation";
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
        
            case PROP_ID_bomId:
               return getBomId();
        
            case PROP_ID_lineNo:
               return getLineNo();
        
            case PROP_ID_operationId:
               return getOperationId();
        
            case PROP_ID_workcenterId:
               return getWorkcenterId();
        
            case PROP_ID_standardTime:
               return getStandardTime();
        
            case PROP_ID_timeUnit:
               return getTimeUnit();
        
            case PROP_ID_rate:
               return getRate();
        
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
        
            case PROP_ID_bomId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_bomId));
               }
               setBomId(typedValue);
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
        
            case PROP_ID_operationId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_operationId));
               }
               setOperationId(typedValue);
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
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
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
        
            case PROP_ID_rate:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_rate));
               }
               setRate(typedValue);
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
        
            case PROP_ID_bomId:{
               onInitProp(propId);
               this._bomId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_lineNo:{
               onInitProp(propId);
               this._lineNo = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_operationId:{
               onInitProp(propId);
               this._operationId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_workcenterId:{
               onInitProp(propId);
               this._workcenterId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_standardTime:{
               onInitProp(propId);
               this._standardTime = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_timeUnit:{
               onInitProp(propId);
               this._timeUnit = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_rate:{
               onInitProp(propId);
               this._rate = (java.math.BigDecimal)value;
               
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
     * BOM ID: BOM_ID
     */
    public final java.lang.Long getBomId(){
         onPropGet(PROP_ID_bomId);
         return _bomId;
    }

    /**
     * BOM ID: BOM_ID
     */
    public final void setBomId(java.lang.Long value){
        if(onPropSet(PROP_ID_bomId,value)){
            this._bomId = value;
            internalClearRefs(PROP_ID_bomId);
            
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
     * 工序ID: OPERATION_ID
     */
    public final java.lang.Long getOperationId(){
         onPropGet(PROP_ID_operationId);
         return _operationId;
    }

    /**
     * 工序ID: OPERATION_ID
     */
    public final void setOperationId(java.lang.Long value){
        if(onPropSet(PROP_ID_operationId,value)){
            this._operationId = value;
            internalClearRefs(PROP_ID_operationId);
            
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
    public final java.math.BigDecimal getStandardTime(){
         onPropGet(PROP_ID_standardTime);
         return _standardTime;
    }

    /**
     * 标准工时: STANDARD_TIME
     */
    public final void setStandardTime(java.math.BigDecimal value){
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
     * 效率系数: RATE
     */
    public final java.math.BigDecimal getRate(){
         onPropGet(PROP_ID_rate);
         return _rate;
    }

    /**
     * 效率系数: RATE
     */
    public final void setRate(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_rate,value)){
            this._rate = value;
            internalClearRefs(PROP_ID_rate);
            
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
    public final app.erp.mfg.dao.entity.ErpMfgBom getBom(){
       return (app.erp.mfg.dao.entity.ErpMfgBom)internalGetRefEntity(PROP_NAME_bom);
    }

    public final void setBom(app.erp.mfg.dao.entity.ErpMfgBom refEntity){
   
           if(refEntity == null){
           
                   this.setBomId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_bom, refEntity,()->{
           
                           this.setBomId(refEntity.getId());
                       
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
       
    /**
     * 
     */
    public final app.erp.mfg.dao.entity.ErpMfgRoutingOperation getOperation(){
       return (app.erp.mfg.dao.entity.ErpMfgRoutingOperation)internalGetRefEntity(PROP_NAME_operation);
    }

    public final void setOperation(app.erp.mfg.dao.entity.ErpMfgRoutingOperation refEntity){
   
           if(refEntity == null){
           
                   this.setOperationId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_operation, refEntity,()->{
           
                           this.setOperationId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
