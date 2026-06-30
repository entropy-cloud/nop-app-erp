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

import app.erp.hr.dao.entity.ErpHrSurveyResult;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  调研结果: erp_hr_survey_result
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpHrSurveyResult extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 所属问卷: SURVEY_ID BIGINT */
    public static final String PROP_NAME_surveyId = "surveyId";
    public static final int PROP_ID_surveyId = 2;
    
    /* 部门: DEPARTMENT_ID BIGINT */
    public static final String PROP_NAME_departmentId = "departmentId";
    public static final int PROP_ID_departmentId = 3;
    
    /* 答卷数: TOTAL_RESPONSES INTEGER */
    public static final String PROP_NAME_totalResponses = "totalResponses";
    public static final int PROP_ID_totalResponses = 4;
    
    /* 平均分: AVG_SCORE DECIMAL */
    public static final String PROP_NAME_avgScore = "avgScore";
    public static final int PROP_ID_avgScore = 5;
    
    /* eNPS得分: ENPS_SCORE INTEGER */
    public static final String PROP_NAME_eNpsScore = "eNpsScore";
    public static final int PROP_ID_eNpsScore = 6;
    
    /* 驱动因子得分: DRIVER_SCORES VARCHAR */
    public static final String PROP_NAME_driverScores = "driverScores";
    public static final int PROP_ID_driverScores = 7;
    
    /* 每题得分: QUESTION_BREAKDOWN VARCHAR */
    public static final String PROP_NAME_questionBreakdown = "questionBreakdown";
    public static final int PROP_ID_questionBreakdown = 8;
    
    /* 历史趋势: TREND_DATA VARCHAR */
    public static final String PROP_NAME_trendData = "trendData";
    public static final int PROP_ID_trendData = 9;
    
    /* 最后计算时间: LAST_CALCULATED_AT DATETIME */
    public static final String PROP_NAME_lastCalculatedAt = "lastCalculatedAt";
    public static final int PROP_ID_lastCalculatedAt = 10;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 11;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 12;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 13;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 14;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 15;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 16;
    

    private static int _PROP_ID_BOUND = 17;

    
    /* relation:  */
    public static final String PROP_NAME_survey = "survey";
    
    /* relation:  */
    public static final String PROP_NAME_department = "department";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[17];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_surveyId] = PROP_NAME_surveyId;
          PROP_NAME_TO_ID.put(PROP_NAME_surveyId, PROP_ID_surveyId);
      
          PROP_ID_TO_NAME[PROP_ID_departmentId] = PROP_NAME_departmentId;
          PROP_NAME_TO_ID.put(PROP_NAME_departmentId, PROP_ID_departmentId);
      
          PROP_ID_TO_NAME[PROP_ID_totalResponses] = PROP_NAME_totalResponses;
          PROP_NAME_TO_ID.put(PROP_NAME_totalResponses, PROP_ID_totalResponses);
      
          PROP_ID_TO_NAME[PROP_ID_avgScore] = PROP_NAME_avgScore;
          PROP_NAME_TO_ID.put(PROP_NAME_avgScore, PROP_ID_avgScore);
      
          PROP_ID_TO_NAME[PROP_ID_eNpsScore] = PROP_NAME_eNpsScore;
          PROP_NAME_TO_ID.put(PROP_NAME_eNpsScore, PROP_ID_eNpsScore);
      
          PROP_ID_TO_NAME[PROP_ID_driverScores] = PROP_NAME_driverScores;
          PROP_NAME_TO_ID.put(PROP_NAME_driverScores, PROP_ID_driverScores);
      
          PROP_ID_TO_NAME[PROP_ID_questionBreakdown] = PROP_NAME_questionBreakdown;
          PROP_NAME_TO_ID.put(PROP_NAME_questionBreakdown, PROP_ID_questionBreakdown);
      
          PROP_ID_TO_NAME[PROP_ID_trendData] = PROP_NAME_trendData;
          PROP_NAME_TO_ID.put(PROP_NAME_trendData, PROP_ID_trendData);
      
          PROP_ID_TO_NAME[PROP_ID_lastCalculatedAt] = PROP_NAME_lastCalculatedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_lastCalculatedAt, PROP_ID_lastCalculatedAt);
      
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
    
    /* 所属问卷: SURVEY_ID */
    private java.lang.Long _surveyId;
    
    /* 部门: DEPARTMENT_ID */
    private java.lang.Long _departmentId;
    
    /* 答卷数: TOTAL_RESPONSES */
    private java.lang.Integer _totalResponses;
    
    /* 平均分: AVG_SCORE */
    private java.math.BigDecimal _avgScore;
    
    /* eNPS得分: ENPS_SCORE */
    private java.lang.Integer _eNpsScore;
    
    /* 驱动因子得分: DRIVER_SCORES */
    private java.lang.String _driverScores;
    
    /* 每题得分: QUESTION_BREAKDOWN */
    private java.lang.String _questionBreakdown;
    
    /* 历史趋势: TREND_DATA */
    private java.lang.String _trendData;
    
    /* 最后计算时间: LAST_CALCULATED_AT */
    private java.time.LocalDateTime _lastCalculatedAt;
    
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
    

    public _ErpHrSurveyResult(){
        // for debug
    }

    protected ErpHrSurveyResult newInstance(){
        ErpHrSurveyResult entity = new ErpHrSurveyResult();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpHrSurveyResult cloneInstance() {
        ErpHrSurveyResult entity = newInstance();
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
      return "app.erp.hr.dao.entity.ErpHrSurveyResult";
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
        
            case PROP_ID_surveyId:
               return getSurveyId();
        
            case PROP_ID_departmentId:
               return getDepartmentId();
        
            case PROP_ID_totalResponses:
               return getTotalResponses();
        
            case PROP_ID_avgScore:
               return getAvgScore();
        
            case PROP_ID_eNpsScore:
               return getENpsScore();
        
            case PROP_ID_driverScores:
               return getDriverScores();
        
            case PROP_ID_questionBreakdown:
               return getQuestionBreakdown();
        
            case PROP_ID_trendData:
               return getTrendData();
        
            case PROP_ID_lastCalculatedAt:
               return getLastCalculatedAt();
        
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
        
            case PROP_ID_surveyId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_surveyId));
               }
               setSurveyId(typedValue);
               break;
            }
        
            case PROP_ID_departmentId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_departmentId));
               }
               setDepartmentId(typedValue);
               break;
            }
        
            case PROP_ID_totalResponses:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_totalResponses));
               }
               setTotalResponses(typedValue);
               break;
            }
        
            case PROP_ID_avgScore:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_avgScore));
               }
               setAvgScore(typedValue);
               break;
            }
        
            case PROP_ID_eNpsScore:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_eNpsScore));
               }
               setENpsScore(typedValue);
               break;
            }
        
            case PROP_ID_driverScores:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_driverScores));
               }
               setDriverScores(typedValue);
               break;
            }
        
            case PROP_ID_questionBreakdown:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_questionBreakdown));
               }
               setQuestionBreakdown(typedValue);
               break;
            }
        
            case PROP_ID_trendData:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_trendData));
               }
               setTrendData(typedValue);
               break;
            }
        
            case PROP_ID_lastCalculatedAt:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_lastCalculatedAt));
               }
               setLastCalculatedAt(typedValue);
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
        
            case PROP_ID_surveyId:{
               onInitProp(propId);
               this._surveyId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_departmentId:{
               onInitProp(propId);
               this._departmentId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_totalResponses:{
               onInitProp(propId);
               this._totalResponses = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_avgScore:{
               onInitProp(propId);
               this._avgScore = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_eNpsScore:{
               onInitProp(propId);
               this._eNpsScore = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_driverScores:{
               onInitProp(propId);
               this._driverScores = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_questionBreakdown:{
               onInitProp(propId);
               this._questionBreakdown = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_trendData:{
               onInitProp(propId);
               this._trendData = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_lastCalculatedAt:{
               onInitProp(propId);
               this._lastCalculatedAt = (java.time.LocalDateTime)value;
               
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
     * 所属问卷: SURVEY_ID
     */
    public final java.lang.Long getSurveyId(){
         onPropGet(PROP_ID_surveyId);
         return _surveyId;
    }

    /**
     * 所属问卷: SURVEY_ID
     */
    public final void setSurveyId(java.lang.Long value){
        if(onPropSet(PROP_ID_surveyId,value)){
            this._surveyId = value;
            internalClearRefs(PROP_ID_surveyId);
            
        }
    }
    
    /**
     * 部门: DEPARTMENT_ID
     */
    public final java.lang.Long getDepartmentId(){
         onPropGet(PROP_ID_departmentId);
         return _departmentId;
    }

    /**
     * 部门: DEPARTMENT_ID
     */
    public final void setDepartmentId(java.lang.Long value){
        if(onPropSet(PROP_ID_departmentId,value)){
            this._departmentId = value;
            internalClearRefs(PROP_ID_departmentId);
            
        }
    }
    
    /**
     * 答卷数: TOTAL_RESPONSES
     */
    public final java.lang.Integer getTotalResponses(){
         onPropGet(PROP_ID_totalResponses);
         return _totalResponses;
    }

    /**
     * 答卷数: TOTAL_RESPONSES
     */
    public final void setTotalResponses(java.lang.Integer value){
        if(onPropSet(PROP_ID_totalResponses,value)){
            this._totalResponses = value;
            internalClearRefs(PROP_ID_totalResponses);
            
        }
    }
    
    /**
     * 平均分: AVG_SCORE
     */
    public final java.math.BigDecimal getAvgScore(){
         onPropGet(PROP_ID_avgScore);
         return _avgScore;
    }

    /**
     * 平均分: AVG_SCORE
     */
    public final void setAvgScore(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_avgScore,value)){
            this._avgScore = value;
            internalClearRefs(PROP_ID_avgScore);
            
        }
    }
    
    /**
     * eNPS得分: ENPS_SCORE
     */
    public final java.lang.Integer getENpsScore(){
         onPropGet(PROP_ID_eNpsScore);
         return _eNpsScore;
    }

    /**
     * eNPS得分: ENPS_SCORE
     */
    public final void setENpsScore(java.lang.Integer value){
        if(onPropSet(PROP_ID_eNpsScore,value)){
            this._eNpsScore = value;
            internalClearRefs(PROP_ID_eNpsScore);
            
        }
    }
    
    /**
     * 驱动因子得分: DRIVER_SCORES
     */
    public final java.lang.String getDriverScores(){
         onPropGet(PROP_ID_driverScores);
         return _driverScores;
    }

    /**
     * 驱动因子得分: DRIVER_SCORES
     */
    public final void setDriverScores(java.lang.String value){
        if(onPropSet(PROP_ID_driverScores,value)){
            this._driverScores = value;
            internalClearRefs(PROP_ID_driverScores);
            
        }
    }
    
    /**
     * 每题得分: QUESTION_BREAKDOWN
     */
    public final java.lang.String getQuestionBreakdown(){
         onPropGet(PROP_ID_questionBreakdown);
         return _questionBreakdown;
    }

    /**
     * 每题得分: QUESTION_BREAKDOWN
     */
    public final void setQuestionBreakdown(java.lang.String value){
        if(onPropSet(PROP_ID_questionBreakdown,value)){
            this._questionBreakdown = value;
            internalClearRefs(PROP_ID_questionBreakdown);
            
        }
    }
    
    /**
     * 历史趋势: TREND_DATA
     */
    public final java.lang.String getTrendData(){
         onPropGet(PROP_ID_trendData);
         return _trendData;
    }

    /**
     * 历史趋势: TREND_DATA
     */
    public final void setTrendData(java.lang.String value){
        if(onPropSet(PROP_ID_trendData,value)){
            this._trendData = value;
            internalClearRefs(PROP_ID_trendData);
            
        }
    }
    
    /**
     * 最后计算时间: LAST_CALCULATED_AT
     */
    public final java.time.LocalDateTime getLastCalculatedAt(){
         onPropGet(PROP_ID_lastCalculatedAt);
         return _lastCalculatedAt;
    }

    /**
     * 最后计算时间: LAST_CALCULATED_AT
     */
    public final void setLastCalculatedAt(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_lastCalculatedAt,value)){
            this._lastCalculatedAt = value;
            internalClearRefs(PROP_ID_lastCalculatedAt);
            
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
    public final app.erp.hr.dao.entity.ErpHrSurvey getSurvey(){
       return (app.erp.hr.dao.entity.ErpHrSurvey)internalGetRefEntity(PROP_NAME_survey);
    }

    public final void setSurvey(app.erp.hr.dao.entity.ErpHrSurvey refEntity){
   
           if(refEntity == null){
           
                   this.setSurveyId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_survey, refEntity,()->{
           
                           this.setSurveyId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.hr.dao.entity.ErpHrDepartment getDepartment(){
       return (app.erp.hr.dao.entity.ErpHrDepartment)internalGetRefEntity(PROP_NAME_department);
    }

    public final void setDepartment(app.erp.hr.dao.entity.ErpHrDepartment refEntity){
   
           if(refEntity == null){
           
                   this.setDepartmentId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_department, refEntity,()->{
           
                           this.setDepartmentId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
