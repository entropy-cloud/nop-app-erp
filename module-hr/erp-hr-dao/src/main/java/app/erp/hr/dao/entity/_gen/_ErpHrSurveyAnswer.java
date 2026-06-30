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

import app.erp.hr.dao.entity.ErpHrSurveyAnswer;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  回答明细: erp_hr_survey_answer
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpHrSurveyAnswer extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 答卷: RESPONSE_ID BIGINT */
    public static final String PROP_NAME_responseId = "responseId";
    public static final int PROP_ID_responseId = 2;
    
    /* 题目: QUESTION_ID BIGINT */
    public static final String PROP_NAME_questionId = "questionId";
    public static final int PROP_ID_questionId = 3;
    
    /* 评分值: RATING_VALUE INTEGER */
    public static final String PROP_NAME_ratingValue = "ratingValue";
    public static final int PROP_ID_ratingValue = 4;
    
    /* 选项: SELECTED_OPTION VARCHAR */
    public static final String PROP_NAME_selectedOption = "selectedOption";
    public static final int PROP_ID_selectedOption = 5;
    
    /* 文本回答: OPEN_TEXT VARCHAR */
    public static final String PROP_NAME_openText = "openText";
    public static final int PROP_ID_openText = 6;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 7;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 8;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 9;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 10;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 11;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 12;
    

    private static int _PROP_ID_BOUND = 13;

    
    /* relation:  */
    public static final String PROP_NAME_response = "response";
    
    /* relation:  */
    public static final String PROP_NAME_question = "question";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[13];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_responseId] = PROP_NAME_responseId;
          PROP_NAME_TO_ID.put(PROP_NAME_responseId, PROP_ID_responseId);
      
          PROP_ID_TO_NAME[PROP_ID_questionId] = PROP_NAME_questionId;
          PROP_NAME_TO_ID.put(PROP_NAME_questionId, PROP_ID_questionId);
      
          PROP_ID_TO_NAME[PROP_ID_ratingValue] = PROP_NAME_ratingValue;
          PROP_NAME_TO_ID.put(PROP_NAME_ratingValue, PROP_ID_ratingValue);
      
          PROP_ID_TO_NAME[PROP_ID_selectedOption] = PROP_NAME_selectedOption;
          PROP_NAME_TO_ID.put(PROP_NAME_selectedOption, PROP_ID_selectedOption);
      
          PROP_ID_TO_NAME[PROP_ID_openText] = PROP_NAME_openText;
          PROP_NAME_TO_ID.put(PROP_NAME_openText, PROP_ID_openText);
      
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
    
    /* 答卷: RESPONSE_ID */
    private java.lang.Long _responseId;
    
    /* 题目: QUESTION_ID */
    private java.lang.Long _questionId;
    
    /* 评分值: RATING_VALUE */
    private java.lang.Integer _ratingValue;
    
    /* 选项: SELECTED_OPTION */
    private java.lang.String _selectedOption;
    
    /* 文本回答: OPEN_TEXT */
    private java.lang.String _openText;
    
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
    

    public _ErpHrSurveyAnswer(){
        // for debug
    }

    protected ErpHrSurveyAnswer newInstance(){
        ErpHrSurveyAnswer entity = new ErpHrSurveyAnswer();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpHrSurveyAnswer cloneInstance() {
        ErpHrSurveyAnswer entity = newInstance();
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
      return "app.erp.hr.dao.entity.ErpHrSurveyAnswer";
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
        
            case PROP_ID_responseId:
               return getResponseId();
        
            case PROP_ID_questionId:
               return getQuestionId();
        
            case PROP_ID_ratingValue:
               return getRatingValue();
        
            case PROP_ID_selectedOption:
               return getSelectedOption();
        
            case PROP_ID_openText:
               return getOpenText();
        
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
        
            case PROP_ID_responseId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_responseId));
               }
               setResponseId(typedValue);
               break;
            }
        
            case PROP_ID_questionId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_questionId));
               }
               setQuestionId(typedValue);
               break;
            }
        
            case PROP_ID_ratingValue:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_ratingValue));
               }
               setRatingValue(typedValue);
               break;
            }
        
            case PROP_ID_selectedOption:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_selectedOption));
               }
               setSelectedOption(typedValue);
               break;
            }
        
            case PROP_ID_openText:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_openText));
               }
               setOpenText(typedValue);
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
        
            case PROP_ID_responseId:{
               onInitProp(propId);
               this._responseId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_questionId:{
               onInitProp(propId);
               this._questionId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_ratingValue:{
               onInitProp(propId);
               this._ratingValue = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_selectedOption:{
               onInitProp(propId);
               this._selectedOption = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_openText:{
               onInitProp(propId);
               this._openText = (java.lang.String)value;
               
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
     * 答卷: RESPONSE_ID
     */
    public final java.lang.Long getResponseId(){
         onPropGet(PROP_ID_responseId);
         return _responseId;
    }

    /**
     * 答卷: RESPONSE_ID
     */
    public final void setResponseId(java.lang.Long value){
        if(onPropSet(PROP_ID_responseId,value)){
            this._responseId = value;
            internalClearRefs(PROP_ID_responseId);
            
        }
    }
    
    /**
     * 题目: QUESTION_ID
     */
    public final java.lang.Long getQuestionId(){
         onPropGet(PROP_ID_questionId);
         return _questionId;
    }

    /**
     * 题目: QUESTION_ID
     */
    public final void setQuestionId(java.lang.Long value){
        if(onPropSet(PROP_ID_questionId,value)){
            this._questionId = value;
            internalClearRefs(PROP_ID_questionId);
            
        }
    }
    
    /**
     * 评分值: RATING_VALUE
     */
    public final java.lang.Integer getRatingValue(){
         onPropGet(PROP_ID_ratingValue);
         return _ratingValue;
    }

    /**
     * 评分值: RATING_VALUE
     */
    public final void setRatingValue(java.lang.Integer value){
        if(onPropSet(PROP_ID_ratingValue,value)){
            this._ratingValue = value;
            internalClearRefs(PROP_ID_ratingValue);
            
        }
    }
    
    /**
     * 选项: SELECTED_OPTION
     */
    public final java.lang.String getSelectedOption(){
         onPropGet(PROP_ID_selectedOption);
         return _selectedOption;
    }

    /**
     * 选项: SELECTED_OPTION
     */
    public final void setSelectedOption(java.lang.String value){
        if(onPropSet(PROP_ID_selectedOption,value)){
            this._selectedOption = value;
            internalClearRefs(PROP_ID_selectedOption);
            
        }
    }
    
    /**
     * 文本回答: OPEN_TEXT
     */
    public final java.lang.String getOpenText(){
         onPropGet(PROP_ID_openText);
         return _openText;
    }

    /**
     * 文本回答: OPEN_TEXT
     */
    public final void setOpenText(java.lang.String value){
        if(onPropSet(PROP_ID_openText,value)){
            this._openText = value;
            internalClearRefs(PROP_ID_openText);
            
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
    public final app.erp.hr.dao.entity.ErpHrSurveyResponse getResponse(){
       return (app.erp.hr.dao.entity.ErpHrSurveyResponse)internalGetRefEntity(PROP_NAME_response);
    }

    public final void setResponse(app.erp.hr.dao.entity.ErpHrSurveyResponse refEntity){
   
           if(refEntity == null){
           
                   this.setResponseId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_response, refEntity,()->{
           
                           this.setResponseId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.hr.dao.entity.ErpHrSurveyQuestion getQuestion(){
       return (app.erp.hr.dao.entity.ErpHrSurveyQuestion)internalGetRefEntity(PROP_NAME_question);
    }

    public final void setQuestion(app.erp.hr.dao.entity.ErpHrSurveyQuestion refEntity){
   
           if(refEntity == null){
           
                   this.setQuestionId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_question, refEntity,()->{
           
                           this.setQuestionId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
