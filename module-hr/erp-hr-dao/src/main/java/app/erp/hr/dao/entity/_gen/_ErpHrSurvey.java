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

import app.erp.hr.dao.entity.ErpHrSurvey;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  问卷模板: erp_hr_survey
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpHrSurvey extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 编号: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 问卷标题: TITLE VARCHAR */
    public static final String PROP_NAME_title = "title";
    public static final int PROP_ID_title = 3;
    
    /* 问卷说明: DESCRIPTION VARCHAR */
    public static final String PROP_NAME_description = "description";
    public static final int PROP_ID_description = 4;
    
    /* 调研类型: SURVEY_TYPE VARCHAR */
    public static final String PROP_NAME_surveyType = "surveyType";
    public static final int PROP_ID_surveyType = 5;
    
    /* 是否匿名: IS_ANONYMOUS BOOLEAN */
    public static final String PROP_NAME_isAnonymous = "isAnonymous";
    public static final int PROP_ID_isAnonymous = 6;
    
    /* 状态: STATUS VARCHAR */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 7;
    
    /* 开始日期: START_DATE DATE */
    public static final String PROP_NAME_startDate = "startDate";
    public static final int PROP_ID_startDate = 8;
    
    /* 截止日期: END_DATE DATE */
    public static final String PROP_NAME_endDate = "endDate";
    public static final int PROP_ID_endDate = 9;
    
    /* 目标部门: TARGET_DEPARTMENT_ID BIGINT */
    public static final String PROP_NAME_targetDepartmentId = "targetDepartmentId";
    public static final int PROP_ID_targetDepartmentId = 10;
    
    /* 包含eNPS: INCLUDE_ENPS BOOLEAN */
    public static final String PROP_NAME_includeENps = "includeENps";
    public static final int PROP_ID_includeENps = 11;
    
    /* eNPS题面: ENPS_QUESTION VARCHAR */
    public static final String PROP_NAME_eNpsQuestion = "eNpsQuestion";
    public static final int PROP_ID_eNpsQuestion = 12;
    
    /* 催填间隔天数: REMINDER_DAYS INTEGER */
    public static final String PROP_NAME_reminderDays = "reminderDays";
    public static final int PROP_ID_reminderDays = 13;
    
    /* 总题数: TOTAL_QUESTIONS INTEGER */
    public static final String PROP_NAME_totalQuestions = "totalQuestions";
    public static final int PROP_ID_totalQuestions = 14;
    
    /* 总答卷数: TOTAL_RESPONSES INTEGER */
    public static final String PROP_NAME_totalResponses = "totalResponses";
    public static final int PROP_ID_totalResponses = 15;
    
    /* 完成率: COMPLETION_RATE DECIMAL */
    public static final String PROP_NAME_completionRate = "completionRate";
    public static final int PROP_ID_completionRate = 16;
    
    /* 平均分: AVG_SCORE DECIMAL */
    public static final String PROP_NAME_avgScore = "avgScore";
    public static final int PROP_ID_avgScore = 17;
    
    /* eNPS得分: ENPS_SCORE INTEGER */
    public static final String PROP_NAME_eNpsScore = "eNpsScore";
    public static final int PROP_ID_eNpsScore = 18;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 19;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 20;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 21;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 22;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 23;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 24;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 25;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 26;
    

    private static int _PROP_ID_BOUND = 27;

    
    /* relation:  */
    public static final String PROP_NAME_questions = "questions";
    
    /* relation:  */
    public static final String PROP_NAME_responses = "responses";
    
    /* relation:  */
    public static final String PROP_NAME_targetDepartment = "targetDepartment";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[27];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_title] = PROP_NAME_title;
          PROP_NAME_TO_ID.put(PROP_NAME_title, PROP_ID_title);
      
          PROP_ID_TO_NAME[PROP_ID_description] = PROP_NAME_description;
          PROP_NAME_TO_ID.put(PROP_NAME_description, PROP_ID_description);
      
          PROP_ID_TO_NAME[PROP_ID_surveyType] = PROP_NAME_surveyType;
          PROP_NAME_TO_ID.put(PROP_NAME_surveyType, PROP_ID_surveyType);
      
          PROP_ID_TO_NAME[PROP_ID_isAnonymous] = PROP_NAME_isAnonymous;
          PROP_NAME_TO_ID.put(PROP_NAME_isAnonymous, PROP_ID_isAnonymous);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_startDate] = PROP_NAME_startDate;
          PROP_NAME_TO_ID.put(PROP_NAME_startDate, PROP_ID_startDate);
      
          PROP_ID_TO_NAME[PROP_ID_endDate] = PROP_NAME_endDate;
          PROP_NAME_TO_ID.put(PROP_NAME_endDate, PROP_ID_endDate);
      
          PROP_ID_TO_NAME[PROP_ID_targetDepartmentId] = PROP_NAME_targetDepartmentId;
          PROP_NAME_TO_ID.put(PROP_NAME_targetDepartmentId, PROP_ID_targetDepartmentId);
      
          PROP_ID_TO_NAME[PROP_ID_includeENps] = PROP_NAME_includeENps;
          PROP_NAME_TO_ID.put(PROP_NAME_includeENps, PROP_ID_includeENps);
      
          PROP_ID_TO_NAME[PROP_ID_eNpsQuestion] = PROP_NAME_eNpsQuestion;
          PROP_NAME_TO_ID.put(PROP_NAME_eNpsQuestion, PROP_ID_eNpsQuestion);
      
          PROP_ID_TO_NAME[PROP_ID_reminderDays] = PROP_NAME_reminderDays;
          PROP_NAME_TO_ID.put(PROP_NAME_reminderDays, PROP_ID_reminderDays);
      
          PROP_ID_TO_NAME[PROP_ID_totalQuestions] = PROP_NAME_totalQuestions;
          PROP_NAME_TO_ID.put(PROP_NAME_totalQuestions, PROP_ID_totalQuestions);
      
          PROP_ID_TO_NAME[PROP_ID_totalResponses] = PROP_NAME_totalResponses;
          PROP_NAME_TO_ID.put(PROP_NAME_totalResponses, PROP_ID_totalResponses);
      
          PROP_ID_TO_NAME[PROP_ID_completionRate] = PROP_NAME_completionRate;
          PROP_NAME_TO_ID.put(PROP_NAME_completionRate, PROP_ID_completionRate);
      
          PROP_ID_TO_NAME[PROP_ID_avgScore] = PROP_NAME_avgScore;
          PROP_NAME_TO_ID.put(PROP_NAME_avgScore, PROP_ID_avgScore);
      
          PROP_ID_TO_NAME[PROP_ID_eNpsScore] = PROP_NAME_eNpsScore;
          PROP_NAME_TO_ID.put(PROP_NAME_eNpsScore, PROP_ID_eNpsScore);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
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
    
    /* 编号: CODE */
    private java.lang.String _code;
    
    /* 问卷标题: TITLE */
    private java.lang.String _title;
    
    /* 问卷说明: DESCRIPTION */
    private java.lang.String _description;
    
    /* 调研类型: SURVEY_TYPE */
    private java.lang.String _surveyType;
    
    /* 是否匿名: IS_ANONYMOUS */
    private java.lang.Boolean _isAnonymous;
    
    /* 状态: STATUS */
    private java.lang.String _status;
    
    /* 开始日期: START_DATE */
    private java.time.LocalDate _startDate;
    
    /* 截止日期: END_DATE */
    private java.time.LocalDate _endDate;
    
    /* 目标部门: TARGET_DEPARTMENT_ID */
    private java.lang.Long _targetDepartmentId;
    
    /* 包含eNPS: INCLUDE_ENPS */
    private java.lang.Boolean _includeENps;
    
    /* eNPS题面: ENPS_QUESTION */
    private java.lang.String _eNpsQuestion;
    
    /* 催填间隔天数: REMINDER_DAYS */
    private java.lang.Integer _reminderDays;
    
    /* 总题数: TOTAL_QUESTIONS */
    private java.lang.Integer _totalQuestions;
    
    /* 总答卷数: TOTAL_RESPONSES */
    private java.lang.Integer _totalResponses;
    
    /* 完成率: COMPLETION_RATE */
    private java.math.BigDecimal _completionRate;
    
    /* 平均分: AVG_SCORE */
    private java.math.BigDecimal _avgScore;
    
    /* eNPS得分: ENPS_SCORE */
    private java.lang.Integer _eNpsScore;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
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
    

    public _ErpHrSurvey(){
        // for debug
    }

    protected ErpHrSurvey newInstance(){
        ErpHrSurvey entity = new ErpHrSurvey();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpHrSurvey cloneInstance() {
        ErpHrSurvey entity = newInstance();
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
      return "app.erp.hr.dao.entity.ErpHrSurvey";
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
        
            case PROP_ID_title:
               return getTitle();
        
            case PROP_ID_description:
               return getDescription();
        
            case PROP_ID_surveyType:
               return getSurveyType();
        
            case PROP_ID_isAnonymous:
               return getIsAnonymous();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_startDate:
               return getStartDate();
        
            case PROP_ID_endDate:
               return getEndDate();
        
            case PROP_ID_targetDepartmentId:
               return getTargetDepartmentId();
        
            case PROP_ID_includeENps:
               return getIncludeENps();
        
            case PROP_ID_eNpsQuestion:
               return getENpsQuestion();
        
            case PROP_ID_reminderDays:
               return getReminderDays();
        
            case PROP_ID_totalQuestions:
               return getTotalQuestions();
        
            case PROP_ID_totalResponses:
               return getTotalResponses();
        
            case PROP_ID_completionRate:
               return getCompletionRate();
        
            case PROP_ID_avgScore:
               return getAvgScore();
        
            case PROP_ID_eNpsScore:
               return getENpsScore();
        
            case PROP_ID_orgId:
               return getOrgId();
        
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
        
            case PROP_ID_code:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_code));
               }
               setCode(typedValue);
               break;
            }
        
            case PROP_ID_title:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_title));
               }
               setTitle(typedValue);
               break;
            }
        
            case PROP_ID_description:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_description));
               }
               setDescription(typedValue);
               break;
            }
        
            case PROP_ID_surveyType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_surveyType));
               }
               setSurveyType(typedValue);
               break;
            }
        
            case PROP_ID_isAnonymous:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isAnonymous));
               }
               setIsAnonymous(typedValue);
               break;
            }
        
            case PROP_ID_status:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_status));
               }
               setStatus(typedValue);
               break;
            }
        
            case PROP_ID_startDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_startDate));
               }
               setStartDate(typedValue);
               break;
            }
        
            case PROP_ID_endDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_endDate));
               }
               setEndDate(typedValue);
               break;
            }
        
            case PROP_ID_targetDepartmentId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_targetDepartmentId));
               }
               setTargetDepartmentId(typedValue);
               break;
            }
        
            case PROP_ID_includeENps:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_includeENps));
               }
               setIncludeENps(typedValue);
               break;
            }
        
            case PROP_ID_eNpsQuestion:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_eNpsQuestion));
               }
               setENpsQuestion(typedValue);
               break;
            }
        
            case PROP_ID_reminderDays:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_reminderDays));
               }
               setReminderDays(typedValue);
               break;
            }
        
            case PROP_ID_totalQuestions:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_totalQuestions));
               }
               setTotalQuestions(typedValue);
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
        
            case PROP_ID_completionRate:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_completionRate));
               }
               setCompletionRate(typedValue);
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
        
            case PROP_ID_orgId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_orgId));
               }
               setOrgId(typedValue);
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
        
            case PROP_ID_code:{
               onInitProp(propId);
               this._code = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_title:{
               onInitProp(propId);
               this._title = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_description:{
               onInitProp(propId);
               this._description = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_surveyType:{
               onInitProp(propId);
               this._surveyType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_isAnonymous:{
               onInitProp(propId);
               this._isAnonymous = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_startDate:{
               onInitProp(propId);
               this._startDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_endDate:{
               onInitProp(propId);
               this._endDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_targetDepartmentId:{
               onInitProp(propId);
               this._targetDepartmentId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_includeENps:{
               onInitProp(propId);
               this._includeENps = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_eNpsQuestion:{
               onInitProp(propId);
               this._eNpsQuestion = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_reminderDays:{
               onInitProp(propId);
               this._reminderDays = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_totalQuestions:{
               onInitProp(propId);
               this._totalQuestions = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_totalResponses:{
               onInitProp(propId);
               this._totalResponses = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_completionRate:{
               onInitProp(propId);
               this._completionRate = (java.math.BigDecimal)value;
               
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
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
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
     * 问卷标题: TITLE
     */
    public final java.lang.String getTitle(){
         onPropGet(PROP_ID_title);
         return _title;
    }

    /**
     * 问卷标题: TITLE
     */
    public final void setTitle(java.lang.String value){
        if(onPropSet(PROP_ID_title,value)){
            this._title = value;
            internalClearRefs(PROP_ID_title);
            
        }
    }
    
    /**
     * 问卷说明: DESCRIPTION
     */
    public final java.lang.String getDescription(){
         onPropGet(PROP_ID_description);
         return _description;
    }

    /**
     * 问卷说明: DESCRIPTION
     */
    public final void setDescription(java.lang.String value){
        if(onPropSet(PROP_ID_description,value)){
            this._description = value;
            internalClearRefs(PROP_ID_description);
            
        }
    }
    
    /**
     * 调研类型: SURVEY_TYPE
     */
    public final java.lang.String getSurveyType(){
         onPropGet(PROP_ID_surveyType);
         return _surveyType;
    }

    /**
     * 调研类型: SURVEY_TYPE
     */
    public final void setSurveyType(java.lang.String value){
        if(onPropSet(PROP_ID_surveyType,value)){
            this._surveyType = value;
            internalClearRefs(PROP_ID_surveyType);
            
        }
    }
    
    /**
     * 是否匿名: IS_ANONYMOUS
     */
    public final java.lang.Boolean getIsAnonymous(){
         onPropGet(PROP_ID_isAnonymous);
         return _isAnonymous;
    }

    /**
     * 是否匿名: IS_ANONYMOUS
     */
    public final void setIsAnonymous(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isAnonymous,value)){
            this._isAnonymous = value;
            internalClearRefs(PROP_ID_isAnonymous);
            
        }
    }
    
    /**
     * 状态: STATUS
     */
    public final java.lang.String getStatus(){
         onPropGet(PROP_ID_status);
         return _status;
    }

    /**
     * 状态: STATUS
     */
    public final void setStatus(java.lang.String value){
        if(onPropSet(PROP_ID_status,value)){
            this._status = value;
            internalClearRefs(PROP_ID_status);
            
        }
    }
    
    /**
     * 开始日期: START_DATE
     */
    public final java.time.LocalDate getStartDate(){
         onPropGet(PROP_ID_startDate);
         return _startDate;
    }

    /**
     * 开始日期: START_DATE
     */
    public final void setStartDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_startDate,value)){
            this._startDate = value;
            internalClearRefs(PROP_ID_startDate);
            
        }
    }
    
    /**
     * 截止日期: END_DATE
     */
    public final java.time.LocalDate getEndDate(){
         onPropGet(PROP_ID_endDate);
         return _endDate;
    }

    /**
     * 截止日期: END_DATE
     */
    public final void setEndDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_endDate,value)){
            this._endDate = value;
            internalClearRefs(PROP_ID_endDate);
            
        }
    }
    
    /**
     * 目标部门: TARGET_DEPARTMENT_ID
     */
    public final java.lang.Long getTargetDepartmentId(){
         onPropGet(PROP_ID_targetDepartmentId);
         return _targetDepartmentId;
    }

    /**
     * 目标部门: TARGET_DEPARTMENT_ID
     */
    public final void setTargetDepartmentId(java.lang.Long value){
        if(onPropSet(PROP_ID_targetDepartmentId,value)){
            this._targetDepartmentId = value;
            internalClearRefs(PROP_ID_targetDepartmentId);
            
        }
    }
    
    /**
     * 包含eNPS: INCLUDE_ENPS
     */
    public final java.lang.Boolean getIncludeENps(){
         onPropGet(PROP_ID_includeENps);
         return _includeENps;
    }

    /**
     * 包含eNPS: INCLUDE_ENPS
     */
    public final void setIncludeENps(java.lang.Boolean value){
        if(onPropSet(PROP_ID_includeENps,value)){
            this._includeENps = value;
            internalClearRefs(PROP_ID_includeENps);
            
        }
    }
    
    /**
     * eNPS题面: ENPS_QUESTION
     */
    public final java.lang.String getENpsQuestion(){
         onPropGet(PROP_ID_eNpsQuestion);
         return _eNpsQuestion;
    }

    /**
     * eNPS题面: ENPS_QUESTION
     */
    public final void setENpsQuestion(java.lang.String value){
        if(onPropSet(PROP_ID_eNpsQuestion,value)){
            this._eNpsQuestion = value;
            internalClearRefs(PROP_ID_eNpsQuestion);
            
        }
    }
    
    /**
     * 催填间隔天数: REMINDER_DAYS
     */
    public final java.lang.Integer getReminderDays(){
         onPropGet(PROP_ID_reminderDays);
         return _reminderDays;
    }

    /**
     * 催填间隔天数: REMINDER_DAYS
     */
    public final void setReminderDays(java.lang.Integer value){
        if(onPropSet(PROP_ID_reminderDays,value)){
            this._reminderDays = value;
            internalClearRefs(PROP_ID_reminderDays);
            
        }
    }
    
    /**
     * 总题数: TOTAL_QUESTIONS
     */
    public final java.lang.Integer getTotalQuestions(){
         onPropGet(PROP_ID_totalQuestions);
         return _totalQuestions;
    }

    /**
     * 总题数: TOTAL_QUESTIONS
     */
    public final void setTotalQuestions(java.lang.Integer value){
        if(onPropSet(PROP_ID_totalQuestions,value)){
            this._totalQuestions = value;
            internalClearRefs(PROP_ID_totalQuestions);
            
        }
    }
    
    /**
     * 总答卷数: TOTAL_RESPONSES
     */
    public final java.lang.Integer getTotalResponses(){
         onPropGet(PROP_ID_totalResponses);
         return _totalResponses;
    }

    /**
     * 总答卷数: TOTAL_RESPONSES
     */
    public final void setTotalResponses(java.lang.Integer value){
        if(onPropSet(PROP_ID_totalResponses,value)){
            this._totalResponses = value;
            internalClearRefs(PROP_ID_totalResponses);
            
        }
    }
    
    /**
     * 完成率: COMPLETION_RATE
     */
    public final java.math.BigDecimal getCompletionRate(){
         onPropGet(PROP_ID_completionRate);
         return _completionRate;
    }

    /**
     * 完成率: COMPLETION_RATE
     */
    public final void setCompletionRate(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_completionRate,value)){
            this._completionRate = value;
            internalClearRefs(PROP_ID_completionRate);
            
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
    
    private final OrmEntitySet<app.erp.hr.dao.entity.ErpHrSurveyQuestion> _questions = new OrmEntitySet<>(this, PROP_NAME_questions,
        null, null,app.erp.hr.dao.entity.ErpHrSurveyQuestion.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.hr.dao.entity.ErpHrSurveyQuestion> getQuestions(){
       return _questions;
    }
       
    private final OrmEntitySet<app.erp.hr.dao.entity.ErpHrSurveyResponse> _responses = new OrmEntitySet<>(this, PROP_NAME_responses,
        null, null,app.erp.hr.dao.entity.ErpHrSurveyResponse.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.hr.dao.entity.ErpHrSurveyResponse> getResponses(){
       return _responses;
    }
       
    /**
     * 
     */
    public final app.erp.hr.dao.entity.ErpHrDepartment getTargetDepartment(){
       return (app.erp.hr.dao.entity.ErpHrDepartment)internalGetRefEntity(PROP_NAME_targetDepartment);
    }

    public final void setTargetDepartment(app.erp.hr.dao.entity.ErpHrDepartment refEntity){
   
           if(refEntity == null){
           
                   this.setTargetDepartmentId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_targetDepartment, refEntity,()->{
           
                           this.setTargetDepartmentId(refEntity.getId());
                       
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
