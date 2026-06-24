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

import app.erp.mfg.dao.entity.ErpMfgJobCard;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  作业卡: erp_mfg_job_card
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpMfgJobCard extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 工单ID: WORK_ORDER_ID BIGINT */
    public static final String PROP_NAME_workOrderId = "workOrderId";
    public static final int PROP_ID_workOrderId = 2;
    
    /* 工序ID: OPERATION_ID BIGINT */
    public static final String PROP_NAME_operationId = "operationId";
    public static final int PROP_ID_operationId = 3;
    
    /* 行号: LINE_NO INTEGER */
    public static final String PROP_NAME_lineNo = "lineNo";
    public static final int PROP_ID_lineNo = 4;
    
    /* 计划数量: PLANNED_QUANTITY DECIMAL */
    public static final String PROP_NAME_plannedQuantity = "plannedQuantity";
    public static final int PROP_ID_plannedQuantity = 5;
    
    /* 完工数量: COMPLETED_QUANTITY DECIMAL */
    public static final String PROP_NAME_completedQuantity = "completedQuantity";
    public static final int PROP_ID_completedQuantity = 6;
    
    /* 报废数量: SCRAPPED_QUANTITY DECIMAL */
    public static final String PROP_NAME_scrappedQuantity = "scrappedQuantity";
    public static final int PROP_ID_scrappedQuantity = 7;
    
    /* 状态: STATUS INTEGER */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 8;
    
    /* 工作中心: WORKCENTER_ID BIGINT */
    public static final String PROP_NAME_workcenterId = "workcenterId";
    public static final int PROP_ID_workcenterId = 9;
    
    /* 实际开始时间: ACTUAL_START_TIME DATETIME */
    public static final String PROP_NAME_actualStartTime = "actualStartTime";
    public static final int PROP_ID_actualStartTime = 10;
    
    /* 实际结束时间: ACTUAL_END_TIME DATETIME */
    public static final String PROP_NAME_actualEndTime = "actualEndTime";
    public static final int PROP_ID_actualEndTime = 11;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 12;
    
    /* 作业卡编号: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 13;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 14;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 15;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 16;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 17;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 18;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 19;
    

    private static int _PROP_ID_BOUND = 20;

    
    /* relation:  */
    public static final String PROP_NAME_workOrder = "workOrder";
    
    /* relation:  */
    public static final String PROP_NAME_workcenter = "workcenter";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[20];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_workOrderId] = PROP_NAME_workOrderId;
          PROP_NAME_TO_ID.put(PROP_NAME_workOrderId, PROP_ID_workOrderId);
      
          PROP_ID_TO_NAME[PROP_ID_operationId] = PROP_NAME_operationId;
          PROP_NAME_TO_ID.put(PROP_NAME_operationId, PROP_ID_operationId);
      
          PROP_ID_TO_NAME[PROP_ID_lineNo] = PROP_NAME_lineNo;
          PROP_NAME_TO_ID.put(PROP_NAME_lineNo, PROP_ID_lineNo);
      
          PROP_ID_TO_NAME[PROP_ID_plannedQuantity] = PROP_NAME_plannedQuantity;
          PROP_NAME_TO_ID.put(PROP_NAME_plannedQuantity, PROP_ID_plannedQuantity);
      
          PROP_ID_TO_NAME[PROP_ID_completedQuantity] = PROP_NAME_completedQuantity;
          PROP_NAME_TO_ID.put(PROP_NAME_completedQuantity, PROP_ID_completedQuantity);
      
          PROP_ID_TO_NAME[PROP_ID_scrappedQuantity] = PROP_NAME_scrappedQuantity;
          PROP_NAME_TO_ID.put(PROP_NAME_scrappedQuantity, PROP_ID_scrappedQuantity);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_workcenterId] = PROP_NAME_workcenterId;
          PROP_NAME_TO_ID.put(PROP_NAME_workcenterId, PROP_ID_workcenterId);
      
          PROP_ID_TO_NAME[PROP_ID_actualStartTime] = PROP_NAME_actualStartTime;
          PROP_NAME_TO_ID.put(PROP_NAME_actualStartTime, PROP_ID_actualStartTime);
      
          PROP_ID_TO_NAME[PROP_ID_actualEndTime] = PROP_NAME_actualEndTime;
          PROP_NAME_TO_ID.put(PROP_NAME_actualEndTime, PROP_ID_actualEndTime);
      
          PROP_ID_TO_NAME[PROP_ID_remark] = PROP_NAME_remark;
          PROP_NAME_TO_ID.put(PROP_NAME_remark, PROP_ID_remark);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
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
    
    /* 工单ID: WORK_ORDER_ID */
    private java.lang.Long _workOrderId;
    
    /* 工序ID: OPERATION_ID */
    private java.lang.Long _operationId;
    
    /* 行号: LINE_NO */
    private java.lang.Integer _lineNo;
    
    /* 计划数量: PLANNED_QUANTITY */
    private java.lang.String _plannedQuantity;
    
    /* 完工数量: COMPLETED_QUANTITY */
    private java.lang.String _completedQuantity;
    
    /* 报废数量: SCRAPPED_QUANTITY */
    private java.lang.String _scrappedQuantity;
    
    /* 状态: STATUS */
    private java.lang.Integer _status;
    
    /* 工作中心: WORKCENTER_ID */
    private java.lang.Long _workcenterId;
    
    /* 实际开始时间: ACTUAL_START_TIME */
    private java.time.LocalDateTime _actualStartTime;
    
    /* 实际结束时间: ACTUAL_END_TIME */
    private java.time.LocalDateTime _actualEndTime;
    
    /* 备注: REMARK */
    private java.lang.String _remark;
    
    /* 作业卡编号: CODE */
    private java.lang.String _code;
    
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
    

    public _ErpMfgJobCard(){
        // for debug
    }

    protected ErpMfgJobCard newInstance(){
        ErpMfgJobCard entity = new ErpMfgJobCard();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpMfgJobCard cloneInstance() {
        ErpMfgJobCard entity = newInstance();
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
      return "app.erp.mfg.dao.entity.ErpMfgJobCard";
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
        
            case PROP_ID_workOrderId:
               return getWorkOrderId();
        
            case PROP_ID_operationId:
               return getOperationId();
        
            case PROP_ID_lineNo:
               return getLineNo();
        
            case PROP_ID_plannedQuantity:
               return getPlannedQuantity();
        
            case PROP_ID_completedQuantity:
               return getCompletedQuantity();
        
            case PROP_ID_scrappedQuantity:
               return getScrappedQuantity();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_workcenterId:
               return getWorkcenterId();
        
            case PROP_ID_actualStartTime:
               return getActualStartTime();
        
            case PROP_ID_actualEndTime:
               return getActualEndTime();
        
            case PROP_ID_remark:
               return getRemark();
        
            case PROP_ID_code:
               return getCode();
        
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
        
            case PROP_ID_workOrderId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_workOrderId));
               }
               setWorkOrderId(typedValue);
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
        
            case PROP_ID_lineNo:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_lineNo));
               }
               setLineNo(typedValue);
               break;
            }
        
            case PROP_ID_plannedQuantity:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_plannedQuantity));
               }
               setPlannedQuantity(typedValue);
               break;
            }
        
            case PROP_ID_completedQuantity:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_completedQuantity));
               }
               setCompletedQuantity(typedValue);
               break;
            }
        
            case PROP_ID_scrappedQuantity:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_scrappedQuantity));
               }
               setScrappedQuantity(typedValue);
               break;
            }
        
            case PROP_ID_status:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_status));
               }
               setStatus(typedValue);
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
        
            case PROP_ID_actualStartTime:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_actualStartTime));
               }
               setActualStartTime(typedValue);
               break;
            }
        
            case PROP_ID_actualEndTime:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_actualEndTime));
               }
               setActualEndTime(typedValue);
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
        
            case PROP_ID_code:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_code));
               }
               setCode(typedValue);
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
        
            case PROP_ID_workOrderId:{
               onInitProp(propId);
               this._workOrderId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_operationId:{
               onInitProp(propId);
               this._operationId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_lineNo:{
               onInitProp(propId);
               this._lineNo = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_plannedQuantity:{
               onInitProp(propId);
               this._plannedQuantity = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_completedQuantity:{
               onInitProp(propId);
               this._completedQuantity = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_scrappedQuantity:{
               onInitProp(propId);
               this._scrappedQuantity = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_workcenterId:{
               onInitProp(propId);
               this._workcenterId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_actualStartTime:{
               onInitProp(propId);
               this._actualStartTime = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_actualEndTime:{
               onInitProp(propId);
               this._actualEndTime = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_remark:{
               onInitProp(propId);
               this._remark = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_code:{
               onInitProp(propId);
               this._code = (java.lang.String)value;
               
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
     * 工单ID: WORK_ORDER_ID
     */
    public final java.lang.Long getWorkOrderId(){
         onPropGet(PROP_ID_workOrderId);
         return _workOrderId;
    }

    /**
     * 工单ID: WORK_ORDER_ID
     */
    public final void setWorkOrderId(java.lang.Long value){
        if(onPropSet(PROP_ID_workOrderId,value)){
            this._workOrderId = value;
            internalClearRefs(PROP_ID_workOrderId);
            
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
     * 计划数量: PLANNED_QUANTITY
     */
    public final java.lang.String getPlannedQuantity(){
         onPropGet(PROP_ID_plannedQuantity);
         return _plannedQuantity;
    }

    /**
     * 计划数量: PLANNED_QUANTITY
     */
    public final void setPlannedQuantity(java.lang.String value){
        if(onPropSet(PROP_ID_plannedQuantity,value)){
            this._plannedQuantity = value;
            internalClearRefs(PROP_ID_plannedQuantity);
            
        }
    }
    
    /**
     * 完工数量: COMPLETED_QUANTITY
     */
    public final java.lang.String getCompletedQuantity(){
         onPropGet(PROP_ID_completedQuantity);
         return _completedQuantity;
    }

    /**
     * 完工数量: COMPLETED_QUANTITY
     */
    public final void setCompletedQuantity(java.lang.String value){
        if(onPropSet(PROP_ID_completedQuantity,value)){
            this._completedQuantity = value;
            internalClearRefs(PROP_ID_completedQuantity);
            
        }
    }
    
    /**
     * 报废数量: SCRAPPED_QUANTITY
     */
    public final java.lang.String getScrappedQuantity(){
         onPropGet(PROP_ID_scrappedQuantity);
         return _scrappedQuantity;
    }

    /**
     * 报废数量: SCRAPPED_QUANTITY
     */
    public final void setScrappedQuantity(java.lang.String value){
        if(onPropSet(PROP_ID_scrappedQuantity,value)){
            this._scrappedQuantity = value;
            internalClearRefs(PROP_ID_scrappedQuantity);
            
        }
    }
    
    /**
     * 状态: STATUS
     */
    public final java.lang.Integer getStatus(){
         onPropGet(PROP_ID_status);
         return _status;
    }

    /**
     * 状态: STATUS
     */
    public final void setStatus(java.lang.Integer value){
        if(onPropSet(PROP_ID_status,value)){
            this._status = value;
            internalClearRefs(PROP_ID_status);
            
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
     * 实际开始时间: ACTUAL_START_TIME
     */
    public final java.time.LocalDateTime getActualStartTime(){
         onPropGet(PROP_ID_actualStartTime);
         return _actualStartTime;
    }

    /**
     * 实际开始时间: ACTUAL_START_TIME
     */
    public final void setActualStartTime(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_actualStartTime,value)){
            this._actualStartTime = value;
            internalClearRefs(PROP_ID_actualStartTime);
            
        }
    }
    
    /**
     * 实际结束时间: ACTUAL_END_TIME
     */
    public final java.time.LocalDateTime getActualEndTime(){
         onPropGet(PROP_ID_actualEndTime);
         return _actualEndTime;
    }

    /**
     * 实际结束时间: ACTUAL_END_TIME
     */
    public final void setActualEndTime(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_actualEndTime,value)){
            this._actualEndTime = value;
            internalClearRefs(PROP_ID_actualEndTime);
            
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
     * 作业卡编号: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * 作业卡编号: CODE
     */
    public final void setCode(java.lang.String value){
        if(onPropSet(PROP_ID_code,value)){
            this._code = value;
            internalClearRefs(PROP_ID_code);
            
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
    public final app.erp.mfg.dao.entity.ErpMfgWorkOrder getWorkOrder(){
       return (app.erp.mfg.dao.entity.ErpMfgWorkOrder)internalGetRefEntity(PROP_NAME_workOrder);
    }

    public final void setWorkOrder(app.erp.mfg.dao.entity.ErpMfgWorkOrder refEntity){
   
           if(refEntity == null){
           
                   this.setWorkOrderId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_workOrder, refEntity,()->{
           
                           this.setWorkOrderId(refEntity.getId());
                       
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
