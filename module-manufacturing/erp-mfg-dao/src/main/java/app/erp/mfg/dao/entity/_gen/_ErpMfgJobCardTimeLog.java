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

import app.erp.mfg.dao.entity.ErpMfgJobCardTimeLog;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  作业工时记录: erp_mfg_job_card_time_log
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpMfgJobCardTimeLog extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 作业卡ID: JOB_CARD_ID BIGINT */
    public static final String PROP_NAME_jobCardId = "jobCardId";
    public static final int PROP_ID_jobCardId = 2;
    
    /* 工单ID: WORK_ORDER_ID BIGINT */
    public static final String PROP_NAME_workOrderId = "workOrderId";
    public static final int PROP_ID_workOrderId = 3;
    
    /* 操作员(职员): OPERATOR_ID VARCHAR */
    public static final String PROP_NAME_operatorId = "operatorId";
    public static final int PROP_ID_operatorId = 4;
    
    /* 作业日期: WORK_DATE DATE */
    public static final String PROP_NAME_workDate = "workDate";
    public static final int PROP_ID_workDate = 5;
    
    /* 开始时间: START_TIME DATETIME */
    public static final String PROP_NAME_startTime = "startTime";
    public static final int PROP_ID_startTime = 6;
    
    /* 结束时间: END_TIME DATETIME */
    public static final String PROP_NAME_endTime = "endTime";
    public static final int PROP_ID_endTime = 7;
    
    /* 工时(分钟): DURATION_MINS DECIMAL */
    public static final String PROP_NAME_durationMins = "durationMins";
    public static final int PROP_ID_durationMins = 8;
    
    /* 准备时间(分钟): SETUP_MINS DECIMAL */
    public static final String PROP_NAME_setupMins = "setupMins";
    public static final int PROP_ID_setupMins = 9;
    
    /* 加工时间(分钟): RUN_MINS DECIMAL */
    public static final String PROP_NAME_runMins = "runMins";
    public static final int PROP_ID_runMins = 10;
    
    /* 本班完工数量: COMPLETED_QUANTITY DECIMAL */
    public static final String PROP_NAME_completedQuantity = "completedQuantity";
    public static final int PROP_ID_completedQuantity = 11;
    
    /* 本班报废数量: SCRAPPED_QUANTITY DECIMAL */
    public static final String PROP_NAME_scrappedQuantity = "scrappedQuantity";
    public static final int PROP_ID_scrappedQuantity = 12;
    
    /* 小时费率: HOURLY_RATE DECIMAL */
    public static final String PROP_NAME_hourlyRate = "hourlyRate";
    public static final int PROP_ID_hourlyRate = 13;
    
    /* 人工成本: LABOR_COST DECIMAL */
    public static final String PROP_NAME_laborCost = "laborCost";
    public static final int PROP_ID_laborCost = 14;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 15;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 16;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 17;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 18;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 19;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 20;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 21;
    

    private static int _PROP_ID_BOUND = 22;

    
    /* relation:  */
    public static final String PROP_NAME_jobCard = "jobCard";
    
    /* relation:  */
    public static final String PROP_NAME_workOrder = "workOrder";
    
    /* relation:  */
    public static final String PROP_NAME_operator = "operator";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[22];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_jobCardId] = PROP_NAME_jobCardId;
          PROP_NAME_TO_ID.put(PROP_NAME_jobCardId, PROP_ID_jobCardId);
      
          PROP_ID_TO_NAME[PROP_ID_workOrderId] = PROP_NAME_workOrderId;
          PROP_NAME_TO_ID.put(PROP_NAME_workOrderId, PROP_ID_workOrderId);
      
          PROP_ID_TO_NAME[PROP_ID_operatorId] = PROP_NAME_operatorId;
          PROP_NAME_TO_ID.put(PROP_NAME_operatorId, PROP_ID_operatorId);
      
          PROP_ID_TO_NAME[PROP_ID_workDate] = PROP_NAME_workDate;
          PROP_NAME_TO_ID.put(PROP_NAME_workDate, PROP_ID_workDate);
      
          PROP_ID_TO_NAME[PROP_ID_startTime] = PROP_NAME_startTime;
          PROP_NAME_TO_ID.put(PROP_NAME_startTime, PROP_ID_startTime);
      
          PROP_ID_TO_NAME[PROP_ID_endTime] = PROP_NAME_endTime;
          PROP_NAME_TO_ID.put(PROP_NAME_endTime, PROP_ID_endTime);
      
          PROP_ID_TO_NAME[PROP_ID_durationMins] = PROP_NAME_durationMins;
          PROP_NAME_TO_ID.put(PROP_NAME_durationMins, PROP_ID_durationMins);
      
          PROP_ID_TO_NAME[PROP_ID_setupMins] = PROP_NAME_setupMins;
          PROP_NAME_TO_ID.put(PROP_NAME_setupMins, PROP_ID_setupMins);
      
          PROP_ID_TO_NAME[PROP_ID_runMins] = PROP_NAME_runMins;
          PROP_NAME_TO_ID.put(PROP_NAME_runMins, PROP_ID_runMins);
      
          PROP_ID_TO_NAME[PROP_ID_completedQuantity] = PROP_NAME_completedQuantity;
          PROP_NAME_TO_ID.put(PROP_NAME_completedQuantity, PROP_ID_completedQuantity);
      
          PROP_ID_TO_NAME[PROP_ID_scrappedQuantity] = PROP_NAME_scrappedQuantity;
          PROP_NAME_TO_ID.put(PROP_NAME_scrappedQuantity, PROP_ID_scrappedQuantity);
      
          PROP_ID_TO_NAME[PROP_ID_hourlyRate] = PROP_NAME_hourlyRate;
          PROP_NAME_TO_ID.put(PROP_NAME_hourlyRate, PROP_ID_hourlyRate);
      
          PROP_ID_TO_NAME[PROP_ID_laborCost] = PROP_NAME_laborCost;
          PROP_NAME_TO_ID.put(PROP_NAME_laborCost, PROP_ID_laborCost);
      
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
    
    /* 作业卡ID: JOB_CARD_ID */
    private java.lang.Long _jobCardId;
    
    /* 工单ID: WORK_ORDER_ID */
    private java.lang.Long _workOrderId;
    
    /* 操作员(职员): OPERATOR_ID */
    private java.lang.String _operatorId;
    
    /* 作业日期: WORK_DATE */
    private java.time.LocalDate _workDate;
    
    /* 开始时间: START_TIME */
    private java.time.LocalDateTime _startTime;
    
    /* 结束时间: END_TIME */
    private java.time.LocalDateTime _endTime;
    
    /* 工时(分钟): DURATION_MINS */
    private java.math.BigDecimal _durationMins;
    
    /* 准备时间(分钟): SETUP_MINS */
    private java.math.BigDecimal _setupMins;
    
    /* 加工时间(分钟): RUN_MINS */
    private java.math.BigDecimal _runMins;
    
    /* 本班完工数量: COMPLETED_QUANTITY */
    private java.math.BigDecimal _completedQuantity;
    
    /* 本班报废数量: SCRAPPED_QUANTITY */
    private java.math.BigDecimal _scrappedQuantity;
    
    /* 小时费率: HOURLY_RATE */
    private java.math.BigDecimal _hourlyRate;
    
    /* 人工成本: LABOR_COST */
    private java.math.BigDecimal _laborCost;
    
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
    

    public _ErpMfgJobCardTimeLog(){
        // for debug
    }

    protected ErpMfgJobCardTimeLog newInstance(){
        ErpMfgJobCardTimeLog entity = new ErpMfgJobCardTimeLog();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpMfgJobCardTimeLog cloneInstance() {
        ErpMfgJobCardTimeLog entity = newInstance();
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
      return "app.erp.mfg.dao.entity.ErpMfgJobCardTimeLog";
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
        
            case PROP_ID_jobCardId:
               return getJobCardId();
        
            case PROP_ID_workOrderId:
               return getWorkOrderId();
        
            case PROP_ID_operatorId:
               return getOperatorId();
        
            case PROP_ID_workDate:
               return getWorkDate();
        
            case PROP_ID_startTime:
               return getStartTime();
        
            case PROP_ID_endTime:
               return getEndTime();
        
            case PROP_ID_durationMins:
               return getDurationMins();
        
            case PROP_ID_setupMins:
               return getSetupMins();
        
            case PROP_ID_runMins:
               return getRunMins();
        
            case PROP_ID_completedQuantity:
               return getCompletedQuantity();
        
            case PROP_ID_scrappedQuantity:
               return getScrappedQuantity();
        
            case PROP_ID_hourlyRate:
               return getHourlyRate();
        
            case PROP_ID_laborCost:
               return getLaborCost();
        
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
        
            case PROP_ID_jobCardId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_jobCardId));
               }
               setJobCardId(typedValue);
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
        
            case PROP_ID_operatorId:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_operatorId));
               }
               setOperatorId(typedValue);
               break;
            }
        
            case PROP_ID_workDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_workDate));
               }
               setWorkDate(typedValue);
               break;
            }
        
            case PROP_ID_startTime:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_startTime));
               }
               setStartTime(typedValue);
               break;
            }
        
            case PROP_ID_endTime:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_endTime));
               }
               setEndTime(typedValue);
               break;
            }
        
            case PROP_ID_durationMins:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_durationMins));
               }
               setDurationMins(typedValue);
               break;
            }
        
            case PROP_ID_setupMins:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_setupMins));
               }
               setSetupMins(typedValue);
               break;
            }
        
            case PROP_ID_runMins:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_runMins));
               }
               setRunMins(typedValue);
               break;
            }
        
            case PROP_ID_completedQuantity:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_completedQuantity));
               }
               setCompletedQuantity(typedValue);
               break;
            }
        
            case PROP_ID_scrappedQuantity:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_scrappedQuantity));
               }
               setScrappedQuantity(typedValue);
               break;
            }
        
            case PROP_ID_hourlyRate:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_hourlyRate));
               }
               setHourlyRate(typedValue);
               break;
            }
        
            case PROP_ID_laborCost:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_laborCost));
               }
               setLaborCost(typedValue);
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
        
            case PROP_ID_jobCardId:{
               onInitProp(propId);
               this._jobCardId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_workOrderId:{
               onInitProp(propId);
               this._workOrderId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_operatorId:{
               onInitProp(propId);
               this._operatorId = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_workDate:{
               onInitProp(propId);
               this._workDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_startTime:{
               onInitProp(propId);
               this._startTime = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_endTime:{
               onInitProp(propId);
               this._endTime = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_durationMins:{
               onInitProp(propId);
               this._durationMins = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_setupMins:{
               onInitProp(propId);
               this._setupMins = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_runMins:{
               onInitProp(propId);
               this._runMins = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_completedQuantity:{
               onInitProp(propId);
               this._completedQuantity = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_scrappedQuantity:{
               onInitProp(propId);
               this._scrappedQuantity = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_hourlyRate:{
               onInitProp(propId);
               this._hourlyRate = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_laborCost:{
               onInitProp(propId);
               this._laborCost = (java.math.BigDecimal)value;
               
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
     * 作业卡ID: JOB_CARD_ID
     */
    public final java.lang.Long getJobCardId(){
         onPropGet(PROP_ID_jobCardId);
         return _jobCardId;
    }

    /**
     * 作业卡ID: JOB_CARD_ID
     */
    public final void setJobCardId(java.lang.Long value){
        if(onPropSet(PROP_ID_jobCardId,value)){
            this._jobCardId = value;
            internalClearRefs(PROP_ID_jobCardId);
            
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
     * 操作员(职员): OPERATOR_ID
     */
    public final java.lang.String getOperatorId(){
         onPropGet(PROP_ID_operatorId);
         return _operatorId;
    }

    /**
     * 操作员(职员): OPERATOR_ID
     */
    public final void setOperatorId(java.lang.String value){
        if(onPropSet(PROP_ID_operatorId,value)){
            this._operatorId = value;
            internalClearRefs(PROP_ID_operatorId);
            
        }
    }
    
    /**
     * 作业日期: WORK_DATE
     */
    public final java.time.LocalDate getWorkDate(){
         onPropGet(PROP_ID_workDate);
         return _workDate;
    }

    /**
     * 作业日期: WORK_DATE
     */
    public final void setWorkDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_workDate,value)){
            this._workDate = value;
            internalClearRefs(PROP_ID_workDate);
            
        }
    }
    
    /**
     * 开始时间: START_TIME
     */
    public final java.time.LocalDateTime getStartTime(){
         onPropGet(PROP_ID_startTime);
         return _startTime;
    }

    /**
     * 开始时间: START_TIME
     */
    public final void setStartTime(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_startTime,value)){
            this._startTime = value;
            internalClearRefs(PROP_ID_startTime);
            
        }
    }
    
    /**
     * 结束时间: END_TIME
     */
    public final java.time.LocalDateTime getEndTime(){
         onPropGet(PROP_ID_endTime);
         return _endTime;
    }

    /**
     * 结束时间: END_TIME
     */
    public final void setEndTime(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_endTime,value)){
            this._endTime = value;
            internalClearRefs(PROP_ID_endTime);
            
        }
    }
    
    /**
     * 工时(分钟): DURATION_MINS
     */
    public final java.math.BigDecimal getDurationMins(){
         onPropGet(PROP_ID_durationMins);
         return _durationMins;
    }

    /**
     * 工时(分钟): DURATION_MINS
     */
    public final void setDurationMins(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_durationMins,value)){
            this._durationMins = value;
            internalClearRefs(PROP_ID_durationMins);
            
        }
    }
    
    /**
     * 准备时间(分钟): SETUP_MINS
     */
    public final java.math.BigDecimal getSetupMins(){
         onPropGet(PROP_ID_setupMins);
         return _setupMins;
    }

    /**
     * 准备时间(分钟): SETUP_MINS
     */
    public final void setSetupMins(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_setupMins,value)){
            this._setupMins = value;
            internalClearRefs(PROP_ID_setupMins);
            
        }
    }
    
    /**
     * 加工时间(分钟): RUN_MINS
     */
    public final java.math.BigDecimal getRunMins(){
         onPropGet(PROP_ID_runMins);
         return _runMins;
    }

    /**
     * 加工时间(分钟): RUN_MINS
     */
    public final void setRunMins(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_runMins,value)){
            this._runMins = value;
            internalClearRefs(PROP_ID_runMins);
            
        }
    }
    
    /**
     * 本班完工数量: COMPLETED_QUANTITY
     */
    public final java.math.BigDecimal getCompletedQuantity(){
         onPropGet(PROP_ID_completedQuantity);
         return _completedQuantity;
    }

    /**
     * 本班完工数量: COMPLETED_QUANTITY
     */
    public final void setCompletedQuantity(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_completedQuantity,value)){
            this._completedQuantity = value;
            internalClearRefs(PROP_ID_completedQuantity);
            
        }
    }
    
    /**
     * 本班报废数量: SCRAPPED_QUANTITY
     */
    public final java.math.BigDecimal getScrappedQuantity(){
         onPropGet(PROP_ID_scrappedQuantity);
         return _scrappedQuantity;
    }

    /**
     * 本班报废数量: SCRAPPED_QUANTITY
     */
    public final void setScrappedQuantity(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_scrappedQuantity,value)){
            this._scrappedQuantity = value;
            internalClearRefs(PROP_ID_scrappedQuantity);
            
        }
    }
    
    /**
     * 小时费率: HOURLY_RATE
     */
    public final java.math.BigDecimal getHourlyRate(){
         onPropGet(PROP_ID_hourlyRate);
         return _hourlyRate;
    }

    /**
     * 小时费率: HOURLY_RATE
     */
    public final void setHourlyRate(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_hourlyRate,value)){
            this._hourlyRate = value;
            internalClearRefs(PROP_ID_hourlyRate);
            
        }
    }
    
    /**
     * 人工成本: LABOR_COST
     */
    public final java.math.BigDecimal getLaborCost(){
         onPropGet(PROP_ID_laborCost);
         return _laborCost;
    }

    /**
     * 人工成本: LABOR_COST
     */
    public final void setLaborCost(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_laborCost,value)){
            this._laborCost = value;
            internalClearRefs(PROP_ID_laborCost);
            
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
    public final app.erp.mfg.dao.entity.ErpMfgJobCard getJobCard(){
       return (app.erp.mfg.dao.entity.ErpMfgJobCard)internalGetRefEntity(PROP_NAME_jobCard);
    }

    public final void setJobCard(app.erp.mfg.dao.entity.ErpMfgJobCard refEntity){
   
           if(refEntity == null){
           
                   this.setJobCardId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_jobCard, refEntity,()->{
           
                           this.setJobCardId(refEntity.getId());
                       
           });
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
    public final app.erp.md.dao.entity.ErpMdEmployee getOperator(){
       return (app.erp.md.dao.entity.ErpMdEmployee)internalGetRefEntity(PROP_NAME_operator);
    }

    public final void setOperator(app.erp.md.dao.entity.ErpMdEmployee refEntity){
   
           if(refEntity == null){
           
                   this.setOperatorId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_operator, refEntity,()->{
           
                           this.orm_propValue(PROP_ID_operatorId,
                           refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
