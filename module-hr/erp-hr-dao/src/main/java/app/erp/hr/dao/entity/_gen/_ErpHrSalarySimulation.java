package app.erp.hr.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.hr.dao.entity.ErpHrSalarySimulation;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  薪酬模拟: erp_hr_salary_simulation
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpHrSalarySimulation extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 编号: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 源薪酬记录: SOURCE_SALARY_ID BIGINT */
    public static final String PROP_NAME_sourceSalaryId = "sourceSalaryId";
    public static final int PROP_ID_sourceSalaryId = 4;
    
    /* 模拟年份: SIMULATION_PERIOD_YEAR INTEGER */
    public static final String PROP_NAME_simulationPeriodYear = "simulationPeriodYear";
    public static final int PROP_ID_simulationPeriodYear = 5;
    
    /* 模拟月份: SIMULATION_PERIOD_MONTH INTEGER */
    public static final String PROP_NAME_simulationPeriodMonth = "simulationPeriodMonth";
    public static final int PROP_ID_simulationPeriodMonth = 6;
    
    /* 模拟名称: SIMULATION_NAME VARCHAR */
    public static final String PROP_NAME_simulationName = "simulationName";
    public static final int PROP_ID_simulationName = 7;
    
    /* 状态: STATUS INTEGER */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 8;
    
    /* 审批人: REVIEWER_ID BIGINT */
    public static final String PROP_NAME_reviewerId = "reviewerId";
    public static final int PROP_ID_reviewerId = 9;
    
    /* 审批时间: REVIEWED_AT DATETIME */
    public static final String PROP_NAME_reviewedAt = "reviewedAt";
    public static final int PROP_ID_reviewedAt = 10;
    
    /* 转正式时间: CONVERTED_AT DATETIME */
    public static final String PROP_NAME_convertedAt = "convertedAt";
    public static final int PROP_ID_convertedAt = 11;
    
    /* 转正式薪酬ID: CONVERTED_SALARY_ID BIGINT */
    public static final String PROP_NAME_convertedSalaryId = "convertedSalaryId";
    public static final int PROP_ID_convertedSalaryId = 12;
    
    /* 备注: NOTES VARCHAR */
    public static final String PROP_NAME_notes = "notes";
    public static final int PROP_ID_notes = 13;
    
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
    public static final String PROP_NAME_sourceSalary = "sourceSalary";
    
    /* relation:  */
    public static final String PROP_NAME_reviewer = "reviewer";
    
    /* relation:  */
    public static final String PROP_NAME_convertedSalary = "convertedSalary";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[20];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_sourceSalaryId] = PROP_NAME_sourceSalaryId;
          PROP_NAME_TO_ID.put(PROP_NAME_sourceSalaryId, PROP_ID_sourceSalaryId);
      
          PROP_ID_TO_NAME[PROP_ID_simulationPeriodYear] = PROP_NAME_simulationPeriodYear;
          PROP_NAME_TO_ID.put(PROP_NAME_simulationPeriodYear, PROP_ID_simulationPeriodYear);
      
          PROP_ID_TO_NAME[PROP_ID_simulationPeriodMonth] = PROP_NAME_simulationPeriodMonth;
          PROP_NAME_TO_ID.put(PROP_NAME_simulationPeriodMonth, PROP_ID_simulationPeriodMonth);
      
          PROP_ID_TO_NAME[PROP_ID_simulationName] = PROP_NAME_simulationName;
          PROP_NAME_TO_ID.put(PROP_NAME_simulationName, PROP_ID_simulationName);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_reviewerId] = PROP_NAME_reviewerId;
          PROP_NAME_TO_ID.put(PROP_NAME_reviewerId, PROP_ID_reviewerId);
      
          PROP_ID_TO_NAME[PROP_ID_reviewedAt] = PROP_NAME_reviewedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_reviewedAt, PROP_ID_reviewedAt);
      
          PROP_ID_TO_NAME[PROP_ID_convertedAt] = PROP_NAME_convertedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_convertedAt, PROP_ID_convertedAt);
      
          PROP_ID_TO_NAME[PROP_ID_convertedSalaryId] = PROP_NAME_convertedSalaryId;
          PROP_NAME_TO_ID.put(PROP_NAME_convertedSalaryId, PROP_ID_convertedSalaryId);
      
          PROP_ID_TO_NAME[PROP_ID_notes] = PROP_NAME_notes;
          PROP_NAME_TO_ID.put(PROP_NAME_notes, PROP_ID_notes);
      
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
    
    /* 编号: CODE */
    private java.lang.String _code;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 源薪酬记录: SOURCE_SALARY_ID */
    private java.lang.Long _sourceSalaryId;
    
    /* 模拟年份: SIMULATION_PERIOD_YEAR */
    private java.lang.Integer _simulationPeriodYear;
    
    /* 模拟月份: SIMULATION_PERIOD_MONTH */
    private java.lang.Integer _simulationPeriodMonth;
    
    /* 模拟名称: SIMULATION_NAME */
    private java.lang.String _simulationName;
    
    /* 状态: STATUS */
    private java.lang.Integer _status;
    
    /* 审批人: REVIEWER_ID */
    private java.lang.Long _reviewerId;
    
    /* 审批时间: REVIEWED_AT */
    private java.time.LocalDateTime _reviewedAt;
    
    /* 转正式时间: CONVERTED_AT */
    private java.time.LocalDateTime _convertedAt;
    
    /* 转正式薪酬ID: CONVERTED_SALARY_ID */
    private java.lang.Long _convertedSalaryId;
    
    /* 备注: NOTES */
    private java.lang.String _notes;
    
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
    

    public _ErpHrSalarySimulation(){
        // for debug
    }

    protected ErpHrSalarySimulation newInstance(){
        ErpHrSalarySimulation entity = new ErpHrSalarySimulation();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpHrSalarySimulation cloneInstance() {
        ErpHrSalarySimulation entity = newInstance();
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
      return "app.erp.hr.dao.entity.ErpHrSalarySimulation";
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
        
            case PROP_ID_code:
               return getCode();
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_sourceSalaryId:
               return getSourceSalaryId();
        
            case PROP_ID_simulationPeriodYear:
               return getSimulationPeriodYear();
        
            case PROP_ID_simulationPeriodMonth:
               return getSimulationPeriodMonth();
        
            case PROP_ID_simulationName:
               return getSimulationName();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_reviewerId:
               return getReviewerId();
        
            case PROP_ID_reviewedAt:
               return getReviewedAt();
        
            case PROP_ID_convertedAt:
               return getConvertedAt();
        
            case PROP_ID_convertedSalaryId:
               return getConvertedSalaryId();
        
            case PROP_ID_notes:
               return getNotes();
        
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
        
            case PROP_ID_code:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_code));
               }
               setCode(typedValue);
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
        
            case PROP_ID_sourceSalaryId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_sourceSalaryId));
               }
               setSourceSalaryId(typedValue);
               break;
            }
        
            case PROP_ID_simulationPeriodYear:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_simulationPeriodYear));
               }
               setSimulationPeriodYear(typedValue);
               break;
            }
        
            case PROP_ID_simulationPeriodMonth:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_simulationPeriodMonth));
               }
               setSimulationPeriodMonth(typedValue);
               break;
            }
        
            case PROP_ID_simulationName:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_simulationName));
               }
               setSimulationName(typedValue);
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
        
            case PROP_ID_reviewerId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_reviewerId));
               }
               setReviewerId(typedValue);
               break;
            }
        
            case PROP_ID_reviewedAt:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_reviewedAt));
               }
               setReviewedAt(typedValue);
               break;
            }
        
            case PROP_ID_convertedAt:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_convertedAt));
               }
               setConvertedAt(typedValue);
               break;
            }
        
            case PROP_ID_convertedSalaryId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_convertedSalaryId));
               }
               setConvertedSalaryId(typedValue);
               break;
            }
        
            case PROP_ID_notes:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_notes));
               }
               setNotes(typedValue);
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
        
            case PROP_ID_code:{
               onInitProp(propId);
               this._code = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_sourceSalaryId:{
               onInitProp(propId);
               this._sourceSalaryId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_simulationPeriodYear:{
               onInitProp(propId);
               this._simulationPeriodYear = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_simulationPeriodMonth:{
               onInitProp(propId);
               this._simulationPeriodMonth = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_simulationName:{
               onInitProp(propId);
               this._simulationName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_reviewerId:{
               onInitProp(propId);
               this._reviewerId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_reviewedAt:{
               onInitProp(propId);
               this._reviewedAt = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_convertedAt:{
               onInitProp(propId);
               this._convertedAt = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_convertedSalaryId:{
               onInitProp(propId);
               this._convertedSalaryId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_notes:{
               onInitProp(propId);
               this._notes = (java.lang.String)value;
               
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
     * 编号: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * 编号: CODE
     */
    public final void setCode(java.lang.String value){
        if(onPropSet(PROP_ID_code,value)){
            this._code = value;
            internalClearRefs(PROP_ID_code);
            
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
     * 源薪酬记录: SOURCE_SALARY_ID
     */
    public final java.lang.Long getSourceSalaryId(){
         onPropGet(PROP_ID_sourceSalaryId);
         return _sourceSalaryId;
    }

    /**
     * 源薪酬记录: SOURCE_SALARY_ID
     */
    public final void setSourceSalaryId(java.lang.Long value){
        if(onPropSet(PROP_ID_sourceSalaryId,value)){
            this._sourceSalaryId = value;
            internalClearRefs(PROP_ID_sourceSalaryId);
            
        }
    }
    
    /**
     * 模拟年份: SIMULATION_PERIOD_YEAR
     */
    public final java.lang.Integer getSimulationPeriodYear(){
         onPropGet(PROP_ID_simulationPeriodYear);
         return _simulationPeriodYear;
    }

    /**
     * 模拟年份: SIMULATION_PERIOD_YEAR
     */
    public final void setSimulationPeriodYear(java.lang.Integer value){
        if(onPropSet(PROP_ID_simulationPeriodYear,value)){
            this._simulationPeriodYear = value;
            internalClearRefs(PROP_ID_simulationPeriodYear);
            
        }
    }
    
    /**
     * 模拟月份: SIMULATION_PERIOD_MONTH
     */
    public final java.lang.Integer getSimulationPeriodMonth(){
         onPropGet(PROP_ID_simulationPeriodMonth);
         return _simulationPeriodMonth;
    }

    /**
     * 模拟月份: SIMULATION_PERIOD_MONTH
     */
    public final void setSimulationPeriodMonth(java.lang.Integer value){
        if(onPropSet(PROP_ID_simulationPeriodMonth,value)){
            this._simulationPeriodMonth = value;
            internalClearRefs(PROP_ID_simulationPeriodMonth);
            
        }
    }
    
    /**
     * 模拟名称: SIMULATION_NAME
     */
    public final java.lang.String getSimulationName(){
         onPropGet(PROP_ID_simulationName);
         return _simulationName;
    }

    /**
     * 模拟名称: SIMULATION_NAME
     */
    public final void setSimulationName(java.lang.String value){
        if(onPropSet(PROP_ID_simulationName,value)){
            this._simulationName = value;
            internalClearRefs(PROP_ID_simulationName);
            
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
     * 审批人: REVIEWER_ID
     */
    public final java.lang.Long getReviewerId(){
         onPropGet(PROP_ID_reviewerId);
         return _reviewerId;
    }

    /**
     * 审批人: REVIEWER_ID
     */
    public final void setReviewerId(java.lang.Long value){
        if(onPropSet(PROP_ID_reviewerId,value)){
            this._reviewerId = value;
            internalClearRefs(PROP_ID_reviewerId);
            
        }
    }
    
    /**
     * 审批时间: REVIEWED_AT
     */
    public final java.time.LocalDateTime getReviewedAt(){
         onPropGet(PROP_ID_reviewedAt);
         return _reviewedAt;
    }

    /**
     * 审批时间: REVIEWED_AT
     */
    public final void setReviewedAt(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_reviewedAt,value)){
            this._reviewedAt = value;
            internalClearRefs(PROP_ID_reviewedAt);
            
        }
    }
    
    /**
     * 转正式时间: CONVERTED_AT
     */
    public final java.time.LocalDateTime getConvertedAt(){
         onPropGet(PROP_ID_convertedAt);
         return _convertedAt;
    }

    /**
     * 转正式时间: CONVERTED_AT
     */
    public final void setConvertedAt(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_convertedAt,value)){
            this._convertedAt = value;
            internalClearRefs(PROP_ID_convertedAt);
            
        }
    }
    
    /**
     * 转正式薪酬ID: CONVERTED_SALARY_ID
     */
    public final java.lang.Long getConvertedSalaryId(){
         onPropGet(PROP_ID_convertedSalaryId);
         return _convertedSalaryId;
    }

    /**
     * 转正式薪酬ID: CONVERTED_SALARY_ID
     */
    public final void setConvertedSalaryId(java.lang.Long value){
        if(onPropSet(PROP_ID_convertedSalaryId,value)){
            this._convertedSalaryId = value;
            internalClearRefs(PROP_ID_convertedSalaryId);
            
        }
    }
    
    /**
     * 备注: NOTES
     */
    public final java.lang.String getNotes(){
         onPropGet(PROP_ID_notes);
         return _notes;
    }

    /**
     * 备注: NOTES
     */
    public final void setNotes(java.lang.String value){
        if(onPropSet(PROP_ID_notes,value)){
            this._notes = value;
            internalClearRefs(PROP_ID_notes);
            
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
    public final app.erp.hr.dao.entity.ErpHrSalary getSourceSalary(){
       return (app.erp.hr.dao.entity.ErpHrSalary)internalGetRefEntity(PROP_NAME_sourceSalary);
    }

    public final void setSourceSalary(app.erp.hr.dao.entity.ErpHrSalary refEntity){
   
           if(refEntity == null){
           
                   this.setSourceSalaryId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_sourceSalary, refEntity,()->{
           
                           this.setSourceSalaryId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.hr.dao.entity.ErpHrEmployee getReviewer(){
       return (app.erp.hr.dao.entity.ErpHrEmployee)internalGetRefEntity(PROP_NAME_reviewer);
    }

    public final void setReviewer(app.erp.hr.dao.entity.ErpHrEmployee refEntity){
   
           if(refEntity == null){
           
                   this.setReviewerId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_reviewer, refEntity,()->{
           
                           this.setReviewerId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.hr.dao.entity.ErpHrSalary getConvertedSalary(){
       return (app.erp.hr.dao.entity.ErpHrSalary)internalGetRefEntity(PROP_NAME_convertedSalary);
    }

    public final void setConvertedSalary(app.erp.hr.dao.entity.ErpHrSalary refEntity){
   
           if(refEntity == null){
           
                   this.setConvertedSalaryId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_convertedSalary, refEntity,()->{
           
                           this.setConvertedSalaryId(refEntity.getId());
                       
           });
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
