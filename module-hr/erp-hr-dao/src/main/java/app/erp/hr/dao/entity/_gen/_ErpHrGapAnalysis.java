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

import app.erp.hr.dao.entity.ErpHrGapAnalysis;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  差距分析: erp_hr_gap_analysis
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpHrGapAnalysis extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 员工: EMPLOYEE_ID BIGINT */
    public static final String PROP_NAME_employeeId = "employeeId";
    public static final int PROP_ID_employeeId = 2;
    
    /* 胜任力: COMPETENCY_ID BIGINT */
    public static final String PROP_NAME_competencyId = "competencyId";
    public static final int PROP_ID_competencyId = 3;
    
    /* 要求等级: REQUIRED_LEVEL INTEGER */
    public static final String PROP_NAME_requiredLevel = "requiredLevel";
    public static final int PROP_ID_requiredLevel = 4;
    
    /* 实际等级: ACTUAL_LEVEL INTEGER */
    public static final String PROP_NAME_actualLevel = "actualLevel";
    public static final int PROP_ID_actualLevel = 5;
    
    /* 差距值: GAP_VALUE INTEGER */
    public static final String PROP_NAME_gapValue = "gapValue";
    public static final int PROP_ID_gapValue = 6;
    
    /* 差距严重程度: GAP_SEVERITY VARCHAR */
    public static final String PROP_NAME_gapSeverity = "gapSeverity";
    public static final int PROP_ID_gapSeverity = 7;
    
    /* 评估日期: ASSESSMENT_DATE DATE */
    public static final String PROP_NAME_assessmentDate = "assessmentDate";
    public static final int PROP_ID_assessmentDate = 8;
    
    /* 分析日期: ANALYSIS_DATE DATETIME */
    public static final String PROP_NAME_analysisDate = "analysisDate";
    public static final int PROP_ID_analysisDate = 9;
    
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
    public static final String PROP_NAME_employee = "employee";
    
    /* relation:  */
    public static final String PROP_NAME_competency = "competency";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[16];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_employeeId] = PROP_NAME_employeeId;
          PROP_NAME_TO_ID.put(PROP_NAME_employeeId, PROP_ID_employeeId);
      
          PROP_ID_TO_NAME[PROP_ID_competencyId] = PROP_NAME_competencyId;
          PROP_NAME_TO_ID.put(PROP_NAME_competencyId, PROP_ID_competencyId);
      
          PROP_ID_TO_NAME[PROP_ID_requiredLevel] = PROP_NAME_requiredLevel;
          PROP_NAME_TO_ID.put(PROP_NAME_requiredLevel, PROP_ID_requiredLevel);
      
          PROP_ID_TO_NAME[PROP_ID_actualLevel] = PROP_NAME_actualLevel;
          PROP_NAME_TO_ID.put(PROP_NAME_actualLevel, PROP_ID_actualLevel);
      
          PROP_ID_TO_NAME[PROP_ID_gapValue] = PROP_NAME_gapValue;
          PROP_NAME_TO_ID.put(PROP_NAME_gapValue, PROP_ID_gapValue);
      
          PROP_ID_TO_NAME[PROP_ID_gapSeverity] = PROP_NAME_gapSeverity;
          PROP_NAME_TO_ID.put(PROP_NAME_gapSeverity, PROP_ID_gapSeverity);
      
          PROP_ID_TO_NAME[PROP_ID_assessmentDate] = PROP_NAME_assessmentDate;
          PROP_NAME_TO_ID.put(PROP_NAME_assessmentDate, PROP_ID_assessmentDate);
      
          PROP_ID_TO_NAME[PROP_ID_analysisDate] = PROP_NAME_analysisDate;
          PROP_NAME_TO_ID.put(PROP_NAME_analysisDate, PROP_ID_analysisDate);
      
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
    
    /* 员工: EMPLOYEE_ID */
    private java.lang.Long _employeeId;
    
    /* 胜任力: COMPETENCY_ID */
    private java.lang.Long _competencyId;
    
    /* 要求等级: REQUIRED_LEVEL */
    private java.lang.Integer _requiredLevel;
    
    /* 实际等级: ACTUAL_LEVEL */
    private java.lang.Integer _actualLevel;
    
    /* 差距值: GAP_VALUE */
    private java.lang.Integer _gapValue;
    
    /* 差距严重程度: GAP_SEVERITY */
    private java.lang.String _gapSeverity;
    
    /* 评估日期: ASSESSMENT_DATE */
    private java.time.LocalDate _assessmentDate;
    
    /* 分析日期: ANALYSIS_DATE */
    private java.time.LocalDateTime _analysisDate;
    
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
    

    public _ErpHrGapAnalysis(){
        // for debug
    }

    protected ErpHrGapAnalysis newInstance(){
        ErpHrGapAnalysis entity = new ErpHrGapAnalysis();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpHrGapAnalysis cloneInstance() {
        ErpHrGapAnalysis entity = newInstance();
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
      return "app.erp.hr.dao.entity.ErpHrGapAnalysis";
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
        
            case PROP_ID_employeeId:
               return getEmployeeId();
        
            case PROP_ID_competencyId:
               return getCompetencyId();
        
            case PROP_ID_requiredLevel:
               return getRequiredLevel();
        
            case PROP_ID_actualLevel:
               return getActualLevel();
        
            case PROP_ID_gapValue:
               return getGapValue();
        
            case PROP_ID_gapSeverity:
               return getGapSeverity();
        
            case PROP_ID_assessmentDate:
               return getAssessmentDate();
        
            case PROP_ID_analysisDate:
               return getAnalysisDate();
        
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
        
            case PROP_ID_employeeId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_employeeId));
               }
               setEmployeeId(typedValue);
               break;
            }
        
            case PROP_ID_competencyId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_competencyId));
               }
               setCompetencyId(typedValue);
               break;
            }
        
            case PROP_ID_requiredLevel:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_requiredLevel));
               }
               setRequiredLevel(typedValue);
               break;
            }
        
            case PROP_ID_actualLevel:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_actualLevel));
               }
               setActualLevel(typedValue);
               break;
            }
        
            case PROP_ID_gapValue:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_gapValue));
               }
               setGapValue(typedValue);
               break;
            }
        
            case PROP_ID_gapSeverity:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_gapSeverity));
               }
               setGapSeverity(typedValue);
               break;
            }
        
            case PROP_ID_assessmentDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_assessmentDate));
               }
               setAssessmentDate(typedValue);
               break;
            }
        
            case PROP_ID_analysisDate:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_analysisDate));
               }
               setAnalysisDate(typedValue);
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
        
            case PROP_ID_employeeId:{
               onInitProp(propId);
               this._employeeId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_competencyId:{
               onInitProp(propId);
               this._competencyId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_requiredLevel:{
               onInitProp(propId);
               this._requiredLevel = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_actualLevel:{
               onInitProp(propId);
               this._actualLevel = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_gapValue:{
               onInitProp(propId);
               this._gapValue = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_gapSeverity:{
               onInitProp(propId);
               this._gapSeverity = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_assessmentDate:{
               onInitProp(propId);
               this._assessmentDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_analysisDate:{
               onInitProp(propId);
               this._analysisDate = (java.time.LocalDateTime)value;
               
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
     * 员工: EMPLOYEE_ID
     */
    public final java.lang.Long getEmployeeId(){
         onPropGet(PROP_ID_employeeId);
         return _employeeId;
    }

    /**
     * 员工: EMPLOYEE_ID
     */
    public final void setEmployeeId(java.lang.Long value){
        if(onPropSet(PROP_ID_employeeId,value)){
            this._employeeId = value;
            internalClearRefs(PROP_ID_employeeId);
            
        }
    }
    
    /**
     * 胜任力: COMPETENCY_ID
     */
    public final java.lang.Long getCompetencyId(){
         onPropGet(PROP_ID_competencyId);
         return _competencyId;
    }

    /**
     * 胜任力: COMPETENCY_ID
     */
    public final void setCompetencyId(java.lang.Long value){
        if(onPropSet(PROP_ID_competencyId,value)){
            this._competencyId = value;
            internalClearRefs(PROP_ID_competencyId);
            
        }
    }
    
    /**
     * 要求等级: REQUIRED_LEVEL
     */
    public final java.lang.Integer getRequiredLevel(){
         onPropGet(PROP_ID_requiredLevel);
         return _requiredLevel;
    }

    /**
     * 要求等级: REQUIRED_LEVEL
     */
    public final void setRequiredLevel(java.lang.Integer value){
        if(onPropSet(PROP_ID_requiredLevel,value)){
            this._requiredLevel = value;
            internalClearRefs(PROP_ID_requiredLevel);
            
        }
    }
    
    /**
     * 实际等级: ACTUAL_LEVEL
     */
    public final java.lang.Integer getActualLevel(){
         onPropGet(PROP_ID_actualLevel);
         return _actualLevel;
    }

    /**
     * 实际等级: ACTUAL_LEVEL
     */
    public final void setActualLevel(java.lang.Integer value){
        if(onPropSet(PROP_ID_actualLevel,value)){
            this._actualLevel = value;
            internalClearRefs(PROP_ID_actualLevel);
            
        }
    }
    
    /**
     * 差距值: GAP_VALUE
     */
    public final java.lang.Integer getGapValue(){
         onPropGet(PROP_ID_gapValue);
         return _gapValue;
    }

    /**
     * 差距值: GAP_VALUE
     */
    public final void setGapValue(java.lang.Integer value){
        if(onPropSet(PROP_ID_gapValue,value)){
            this._gapValue = value;
            internalClearRefs(PROP_ID_gapValue);
            
        }
    }
    
    /**
     * 差距严重程度: GAP_SEVERITY
     */
    public final java.lang.String getGapSeverity(){
         onPropGet(PROP_ID_gapSeverity);
         return _gapSeverity;
    }

    /**
     * 差距严重程度: GAP_SEVERITY
     */
    public final void setGapSeverity(java.lang.String value){
        if(onPropSet(PROP_ID_gapSeverity,value)){
            this._gapSeverity = value;
            internalClearRefs(PROP_ID_gapSeverity);
            
        }
    }
    
    /**
     * 评估日期: ASSESSMENT_DATE
     */
    public final java.time.LocalDate getAssessmentDate(){
         onPropGet(PROP_ID_assessmentDate);
         return _assessmentDate;
    }

    /**
     * 评估日期: ASSESSMENT_DATE
     */
    public final void setAssessmentDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_assessmentDate,value)){
            this._assessmentDate = value;
            internalClearRefs(PROP_ID_assessmentDate);
            
        }
    }
    
    /**
     * 分析日期: ANALYSIS_DATE
     */
    public final java.time.LocalDateTime getAnalysisDate(){
         onPropGet(PROP_ID_analysisDate);
         return _analysisDate;
    }

    /**
     * 分析日期: ANALYSIS_DATE
     */
    public final void setAnalysisDate(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_analysisDate,value)){
            this._analysisDate = value;
            internalClearRefs(PROP_ID_analysisDate);
            
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
    public final app.erp.hr.dao.entity.ErpHrEmployee getEmployee(){
       return (app.erp.hr.dao.entity.ErpHrEmployee)internalGetRefEntity(PROP_NAME_employee);
    }

    public final void setEmployee(app.erp.hr.dao.entity.ErpHrEmployee refEntity){
   
           if(refEntity == null){
           
                   this.setEmployeeId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_employee, refEntity,()->{
           
                           this.setEmployeeId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.hr.dao.entity.ErpHrCompetency getCompetency(){
       return (app.erp.hr.dao.entity.ErpHrCompetency)internalGetRefEntity(PROP_NAME_competency);
    }

    public final void setCompetency(app.erp.hr.dao.entity.ErpHrCompetency refEntity){
   
           if(refEntity == null){
           
                   this.setCompetencyId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_competency, refEntity,()->{
           
                           this.setCompetencyId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
