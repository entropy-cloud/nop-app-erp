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

import app.erp.hr.dao.entity.ErpHrSurveyQuestion;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  问卷题目: erp_hr_survey_question
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpHrSurveyQuestion extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 所属问卷: SURVEY_ID BIGINT */
    public static final String PROP_NAME_surveyId = "surveyId";
    public static final int PROP_ID_surveyId = 2;
    
    /* 排序号: SORT_ORDER INTEGER */
    public static final String PROP_NAME_sortOrder = "sortOrder";
    public static final int PROP_ID_sortOrder = 3;
    
    /* 题目内容: QUESTION_TEXT VARCHAR */
    public static final String PROP_NAME_questionText = "questionText";
    public static final int PROP_ID_questionText = 4;
    
    /* 题型: QUESTION_TYPE VARCHAR */
    public static final String PROP_NAME_questionType = "questionType";
    public static final int PROP_ID_questionType = 5;
    
    /* 评分最低分: RATING_SCALE_MIN INTEGER */
    public static final String PROP_NAME_ratingScaleMin = "ratingScaleMin";
    public static final int PROP_ID_ratingScaleMin = 6;
    
    /* 评分最高分: RATING_SCALE_MAX INTEGER */
    public static final String PROP_NAME_ratingScaleMax = "ratingScaleMax";
    public static final int PROP_ID_ratingScaleMax = 7;
    
    /* 最低分标签: RATING_LABEL_MIN VARCHAR */
    public static final String PROP_NAME_ratingLabelMin = "ratingLabelMin";
    public static final int PROP_ID_ratingLabelMin = 8;
    
    /* 最高分标签: RATING_LABEL_MAX VARCHAR */
    public static final String PROP_NAME_ratingLabelMax = "ratingLabelMax";
    public static final int PROP_ID_ratingLabelMax = 9;
    
    /* 选项列表: OPTIONS VARCHAR */
    public static final String PROP_NAME_options = "options";
    public static final int PROP_ID_options = 10;
    
    /* 驱动因子分类: DRIVER_CATEGORY VARCHAR */
    public static final String PROP_NAME_driverCategory = "driverCategory";
    public static final int PROP_ID_driverCategory = 11;
    
    /* 是否必填: IS_REQUIRED BOOLEAN */
    public static final String PROP_NAME_isRequired = "isRequired";
    public static final int PROP_ID_isRequired = 12;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 13;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 14;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 15;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 16;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 17;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 18;
    

    private static int _PROP_ID_BOUND = 19;

    
    /* relation:  */
    public static final String PROP_NAME_survey = "survey";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[19];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_surveyId] = PROP_NAME_surveyId;
          PROP_NAME_TO_ID.put(PROP_NAME_surveyId, PROP_ID_surveyId);
      
          PROP_ID_TO_NAME[PROP_ID_sortOrder] = PROP_NAME_sortOrder;
          PROP_NAME_TO_ID.put(PROP_NAME_sortOrder, PROP_ID_sortOrder);
      
          PROP_ID_TO_NAME[PROP_ID_questionText] = PROP_NAME_questionText;
          PROP_NAME_TO_ID.put(PROP_NAME_questionText, PROP_ID_questionText);
      
          PROP_ID_TO_NAME[PROP_ID_questionType] = PROP_NAME_questionType;
          PROP_NAME_TO_ID.put(PROP_NAME_questionType, PROP_ID_questionType);
      
          PROP_ID_TO_NAME[PROP_ID_ratingScaleMin] = PROP_NAME_ratingScaleMin;
          PROP_NAME_TO_ID.put(PROP_NAME_ratingScaleMin, PROP_ID_ratingScaleMin);
      
          PROP_ID_TO_NAME[PROP_ID_ratingScaleMax] = PROP_NAME_ratingScaleMax;
          PROP_NAME_TO_ID.put(PROP_NAME_ratingScaleMax, PROP_ID_ratingScaleMax);
      
          PROP_ID_TO_NAME[PROP_ID_ratingLabelMin] = PROP_NAME_ratingLabelMin;
          PROP_NAME_TO_ID.put(PROP_NAME_ratingLabelMin, PROP_ID_ratingLabelMin);
      
          PROP_ID_TO_NAME[PROP_ID_ratingLabelMax] = PROP_NAME_ratingLabelMax;
          PROP_NAME_TO_ID.put(PROP_NAME_ratingLabelMax, PROP_ID_ratingLabelMax);
      
          PROP_ID_TO_NAME[PROP_ID_options] = PROP_NAME_options;
          PROP_NAME_TO_ID.put(PROP_NAME_options, PROP_ID_options);
      
          PROP_ID_TO_NAME[PROP_ID_driverCategory] = PROP_NAME_driverCategory;
          PROP_NAME_TO_ID.put(PROP_NAME_driverCategory, PROP_ID_driverCategory);
      
          PROP_ID_TO_NAME[PROP_ID_isRequired] = PROP_NAME_isRequired;
          PROP_NAME_TO_ID.put(PROP_NAME_isRequired, PROP_ID_isRequired);
      
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
    
    /* 排序号: SORT_ORDER */
    private java.lang.Integer _sortOrder;
    
    /* 题目内容: QUESTION_TEXT */
    private java.lang.String _questionText;
    
    /* 题型: QUESTION_TYPE */
    private java.lang.String _questionType;
    
    /* 评分最低分: RATING_SCALE_MIN */
    private java.lang.Integer _ratingScaleMin;
    
    /* 评分最高分: RATING_SCALE_MAX */
    private java.lang.Integer _ratingScaleMax;
    
    /* 最低分标签: RATING_LABEL_MIN */
    private java.lang.String _ratingLabelMin;
    
    /* 最高分标签: RATING_LABEL_MAX */
    private java.lang.String _ratingLabelMax;
    
    /* 选项列表: OPTIONS */
    private java.lang.String _options;
    
    /* 驱动因子分类: DRIVER_CATEGORY */
    private java.lang.String _driverCategory;
    
    /* 是否必填: IS_REQUIRED */
    private java.lang.Boolean _isRequired;
    
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
    

    public _ErpHrSurveyQuestion(){
        // for debug
    }

    protected ErpHrSurveyQuestion newInstance(){
        ErpHrSurveyQuestion entity = new ErpHrSurveyQuestion();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpHrSurveyQuestion cloneInstance() {
        ErpHrSurveyQuestion entity = newInstance();
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
      return "app.erp.hr.dao.entity.ErpHrSurveyQuestion";
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
        
            case PROP_ID_sortOrder:
               return getSortOrder();
        
            case PROP_ID_questionText:
               return getQuestionText();
        
            case PROP_ID_questionType:
               return getQuestionType();
        
            case PROP_ID_ratingScaleMin:
               return getRatingScaleMin();
        
            case PROP_ID_ratingScaleMax:
               return getRatingScaleMax();
        
            case PROP_ID_ratingLabelMin:
               return getRatingLabelMin();
        
            case PROP_ID_ratingLabelMax:
               return getRatingLabelMax();
        
            case PROP_ID_options:
               return getOptions();
        
            case PROP_ID_driverCategory:
               return getDriverCategory();
        
            case PROP_ID_isRequired:
               return getIsRequired();
        
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
        
            case PROP_ID_sortOrder:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_sortOrder));
               }
               setSortOrder(typedValue);
               break;
            }
        
            case PROP_ID_questionText:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_questionText));
               }
               setQuestionText(typedValue);
               break;
            }
        
            case PROP_ID_questionType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_questionType));
               }
               setQuestionType(typedValue);
               break;
            }
        
            case PROP_ID_ratingScaleMin:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_ratingScaleMin));
               }
               setRatingScaleMin(typedValue);
               break;
            }
        
            case PROP_ID_ratingScaleMax:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_ratingScaleMax));
               }
               setRatingScaleMax(typedValue);
               break;
            }
        
            case PROP_ID_ratingLabelMin:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_ratingLabelMin));
               }
               setRatingLabelMin(typedValue);
               break;
            }
        
            case PROP_ID_ratingLabelMax:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_ratingLabelMax));
               }
               setRatingLabelMax(typedValue);
               break;
            }
        
            case PROP_ID_options:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_options));
               }
               setOptions(typedValue);
               break;
            }
        
            case PROP_ID_driverCategory:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_driverCategory));
               }
               setDriverCategory(typedValue);
               break;
            }
        
            case PROP_ID_isRequired:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isRequired));
               }
               setIsRequired(typedValue);
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
        
            case PROP_ID_sortOrder:{
               onInitProp(propId);
               this._sortOrder = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_questionText:{
               onInitProp(propId);
               this._questionText = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_questionType:{
               onInitProp(propId);
               this._questionType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_ratingScaleMin:{
               onInitProp(propId);
               this._ratingScaleMin = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_ratingScaleMax:{
               onInitProp(propId);
               this._ratingScaleMax = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_ratingLabelMin:{
               onInitProp(propId);
               this._ratingLabelMin = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_ratingLabelMax:{
               onInitProp(propId);
               this._ratingLabelMax = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_options:{
               onInitProp(propId);
               this._options = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_driverCategory:{
               onInitProp(propId);
               this._driverCategory = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_isRequired:{
               onInitProp(propId);
               this._isRequired = (java.lang.Boolean)value;
               
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
     * 排序号: SORT_ORDER
     */
    public final java.lang.Integer getSortOrder(){
         onPropGet(PROP_ID_sortOrder);
         return _sortOrder;
    }

    /**
     * 排序号: SORT_ORDER
     */
    public final void setSortOrder(java.lang.Integer value){
        if(onPropSet(PROP_ID_sortOrder,value)){
            this._sortOrder = value;
            internalClearRefs(PROP_ID_sortOrder);
            
        }
    }
    
    /**
     * 题目内容: QUESTION_TEXT
     */
    public final java.lang.String getQuestionText(){
         onPropGet(PROP_ID_questionText);
         return _questionText;
    }

    /**
     * 题目内容: QUESTION_TEXT
     */
    public final void setQuestionText(java.lang.String value){
        if(onPropSet(PROP_ID_questionText,value)){
            this._questionText = value;
            internalClearRefs(PROP_ID_questionText);
            
        }
    }
    
    /**
     * 题型: QUESTION_TYPE
     */
    public final java.lang.String getQuestionType(){
         onPropGet(PROP_ID_questionType);
         return _questionType;
    }

    /**
     * 题型: QUESTION_TYPE
     */
    public final void setQuestionType(java.lang.String value){
        if(onPropSet(PROP_ID_questionType,value)){
            this._questionType = value;
            internalClearRefs(PROP_ID_questionType);
            
        }
    }
    
    /**
     * 评分最低分: RATING_SCALE_MIN
     */
    public final java.lang.Integer getRatingScaleMin(){
         onPropGet(PROP_ID_ratingScaleMin);
         return _ratingScaleMin;
    }

    /**
     * 评分最低分: RATING_SCALE_MIN
     */
    public final void setRatingScaleMin(java.lang.Integer value){
        if(onPropSet(PROP_ID_ratingScaleMin,value)){
            this._ratingScaleMin = value;
            internalClearRefs(PROP_ID_ratingScaleMin);
            
        }
    }
    
    /**
     * 评分最高分: RATING_SCALE_MAX
     */
    public final java.lang.Integer getRatingScaleMax(){
         onPropGet(PROP_ID_ratingScaleMax);
         return _ratingScaleMax;
    }

    /**
     * 评分最高分: RATING_SCALE_MAX
     */
    public final void setRatingScaleMax(java.lang.Integer value){
        if(onPropSet(PROP_ID_ratingScaleMax,value)){
            this._ratingScaleMax = value;
            internalClearRefs(PROP_ID_ratingScaleMax);
            
        }
    }
    
    /**
     * 最低分标签: RATING_LABEL_MIN
     */
    public final java.lang.String getRatingLabelMin(){
         onPropGet(PROP_ID_ratingLabelMin);
         return _ratingLabelMin;
    }

    /**
     * 最低分标签: RATING_LABEL_MIN
     */
    public final void setRatingLabelMin(java.lang.String value){
        if(onPropSet(PROP_ID_ratingLabelMin,value)){
            this._ratingLabelMin = value;
            internalClearRefs(PROP_ID_ratingLabelMin);
            
        }
    }
    
    /**
     * 最高分标签: RATING_LABEL_MAX
     */
    public final java.lang.String getRatingLabelMax(){
         onPropGet(PROP_ID_ratingLabelMax);
         return _ratingLabelMax;
    }

    /**
     * 最高分标签: RATING_LABEL_MAX
     */
    public final void setRatingLabelMax(java.lang.String value){
        if(onPropSet(PROP_ID_ratingLabelMax,value)){
            this._ratingLabelMax = value;
            internalClearRefs(PROP_ID_ratingLabelMax);
            
        }
    }
    
    /**
     * 选项列表: OPTIONS
     */
    public final java.lang.String getOptions(){
         onPropGet(PROP_ID_options);
         return _options;
    }

    /**
     * 选项列表: OPTIONS
     */
    public final void setOptions(java.lang.String value){
        if(onPropSet(PROP_ID_options,value)){
            this._options = value;
            internalClearRefs(PROP_ID_options);
            
        }
    }
    
    /**
     * 驱动因子分类: DRIVER_CATEGORY
     */
    public final java.lang.String getDriverCategory(){
         onPropGet(PROP_ID_driverCategory);
         return _driverCategory;
    }

    /**
     * 驱动因子分类: DRIVER_CATEGORY
     */
    public final void setDriverCategory(java.lang.String value){
        if(onPropSet(PROP_ID_driverCategory,value)){
            this._driverCategory = value;
            internalClearRefs(PROP_ID_driverCategory);
            
        }
    }
    
    /**
     * 是否必填: IS_REQUIRED
     */
    public final java.lang.Boolean getIsRequired(){
         onPropGet(PROP_ID_isRequired);
         return _isRequired;
    }

    /**
     * 是否必填: IS_REQUIRED
     */
    public final void setIsRequired(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isRequired,value)){
            this._isRequired = value;
            internalClearRefs(PROP_ID_isRequired);
            
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
       
}
// resume CPD analysis - CPD-ON
