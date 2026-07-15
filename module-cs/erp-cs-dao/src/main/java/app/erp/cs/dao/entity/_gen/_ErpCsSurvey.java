package app.erp.cs.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.cs.dao.entity.ErpCsSurvey;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  满意度调查: erp_cs_survey
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpCsSurvey extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 2;
    
    /* 关联工单: TICKET_ID BIGINT */
    public static final String PROP_NAME_ticketId = "ticketId";
    public static final int PROP_ID_ticketId = 3;
    
    /* 调查令牌: SURVEY_TOKEN VARCHAR */
    public static final String PROP_NAME_surveyToken = "surveyToken";
    public static final int PROP_ID_surveyToken = 4;
    
    /* CSAT评分: CSAT_SCORE INTEGER */
    public static final String PROP_NAME_csatScore = "csatScore";
    public static final int PROP_ID_csatScore = 5;
    
    /* NPS评分: NPS_SCORE INTEGER */
    public static final String PROP_NAME_npsScore = "npsScore";
    public static final int PROP_ID_npsScore = 6;
    
    /* CES评分: CES_SCORE INTEGER */
    public static final String PROP_NAME_cesScore = "cesScore";
    public static final int PROP_ID_cesScore = 7;
    
    /* 文字反馈: COMMENT VARCHAR */
    public static final String PROP_NAME_comment = "comment";
    public static final int PROP_ID_comment = 8;
    
    /* 响应时间: RESPONDED_AT TIMESTAMP */
    public static final String PROP_NAME_respondedAt = "respondedAt";
    public static final int PROP_ID_respondedAt = 9;
    
    /* 调查发送时间: SURVEY_SENT_AT TIMESTAMP */
    public static final String PROP_NAME_surveySentAt = "surveySentAt";
    public static final int PROP_ID_surveySentAt = 10;
    
    /* 发送渠道: SURVEY_CHANNEL VARCHAR */
    public static final String PROP_NAME_surveyChannel = "surveyChannel";
    public static final int PROP_ID_surveyChannel = 11;
    
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
    public static final String PROP_NAME_ticket = "ticket";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[18];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_ticketId] = PROP_NAME_ticketId;
          PROP_NAME_TO_ID.put(PROP_NAME_ticketId, PROP_ID_ticketId);
      
          PROP_ID_TO_NAME[PROP_ID_surveyToken] = PROP_NAME_surveyToken;
          PROP_NAME_TO_ID.put(PROP_NAME_surveyToken, PROP_ID_surveyToken);
      
          PROP_ID_TO_NAME[PROP_ID_csatScore] = PROP_NAME_csatScore;
          PROP_NAME_TO_ID.put(PROP_NAME_csatScore, PROP_ID_csatScore);
      
          PROP_ID_TO_NAME[PROP_ID_npsScore] = PROP_NAME_npsScore;
          PROP_NAME_TO_ID.put(PROP_NAME_npsScore, PROP_ID_npsScore);
      
          PROP_ID_TO_NAME[PROP_ID_cesScore] = PROP_NAME_cesScore;
          PROP_NAME_TO_ID.put(PROP_NAME_cesScore, PROP_ID_cesScore);
      
          PROP_ID_TO_NAME[PROP_ID_comment] = PROP_NAME_comment;
          PROP_NAME_TO_ID.put(PROP_NAME_comment, PROP_ID_comment);
      
          PROP_ID_TO_NAME[PROP_ID_respondedAt] = PROP_NAME_respondedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_respondedAt, PROP_ID_respondedAt);
      
          PROP_ID_TO_NAME[PROP_ID_surveySentAt] = PROP_NAME_surveySentAt;
          PROP_NAME_TO_ID.put(PROP_NAME_surveySentAt, PROP_ID_surveySentAt);
      
          PROP_ID_TO_NAME[PROP_ID_surveyChannel] = PROP_NAME_surveyChannel;
          PROP_NAME_TO_ID.put(PROP_NAME_surveyChannel, PROP_ID_surveyChannel);
      
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
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 关联工单: TICKET_ID */
    private java.lang.Long _ticketId;
    
    /* 调查令牌: SURVEY_TOKEN */
    private java.lang.String _surveyToken;
    
    /* CSAT评分: CSAT_SCORE */
    private java.lang.Integer _csatScore;
    
    /* NPS评分: NPS_SCORE */
    private java.lang.Integer _npsScore;
    
    /* CES评分: CES_SCORE */
    private java.lang.Integer _cesScore;
    
    /* 文字反馈: COMMENT */
    private java.lang.String _comment;
    
    /* 响应时间: RESPONDED_AT */
    private java.sql.Timestamp _respondedAt;
    
    /* 调查发送时间: SURVEY_SENT_AT */
    private java.sql.Timestamp _surveySentAt;
    
    /* 发送渠道: SURVEY_CHANNEL */
    private java.lang.String _surveyChannel;
    
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
    

    public _ErpCsSurvey(){
        // for debug
    }

    protected ErpCsSurvey newInstance(){
        ErpCsSurvey entity = new ErpCsSurvey();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpCsSurvey cloneInstance() {
        ErpCsSurvey entity = newInstance();
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
      return "app.erp.cs.dao.entity.ErpCsSurvey";
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
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_ticketId:
               return getTicketId();
        
            case PROP_ID_surveyToken:
               return getSurveyToken();
        
            case PROP_ID_csatScore:
               return getCsatScore();
        
            case PROP_ID_npsScore:
               return getNpsScore();
        
            case PROP_ID_cesScore:
               return getCesScore();
        
            case PROP_ID_comment:
               return getComment();
        
            case PROP_ID_respondedAt:
               return getRespondedAt();
        
            case PROP_ID_surveySentAt:
               return getSurveySentAt();
        
            case PROP_ID_surveyChannel:
               return getSurveyChannel();
        
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
        
            case PROP_ID_orgId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_orgId));
               }
               setOrgId(typedValue);
               break;
            }
        
            case PROP_ID_ticketId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_ticketId));
               }
               setTicketId(typedValue);
               break;
            }
        
            case PROP_ID_surveyToken:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_surveyToken));
               }
               setSurveyToken(typedValue);
               break;
            }
        
            case PROP_ID_csatScore:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_csatScore));
               }
               setCsatScore(typedValue);
               break;
            }
        
            case PROP_ID_npsScore:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_npsScore));
               }
               setNpsScore(typedValue);
               break;
            }
        
            case PROP_ID_cesScore:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_cesScore));
               }
               setCesScore(typedValue);
               break;
            }
        
            case PROP_ID_comment:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_comment));
               }
               setComment(typedValue);
               break;
            }
        
            case PROP_ID_respondedAt:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_respondedAt));
               }
               setRespondedAt(typedValue);
               break;
            }
        
            case PROP_ID_surveySentAt:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_surveySentAt));
               }
               setSurveySentAt(typedValue);
               break;
            }
        
            case PROP_ID_surveyChannel:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_surveyChannel));
               }
               setSurveyChannel(typedValue);
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
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_ticketId:{
               onInitProp(propId);
               this._ticketId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_surveyToken:{
               onInitProp(propId);
               this._surveyToken = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_csatScore:{
               onInitProp(propId);
               this._csatScore = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_npsScore:{
               onInitProp(propId);
               this._npsScore = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_cesScore:{
               onInitProp(propId);
               this._cesScore = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_comment:{
               onInitProp(propId);
               this._comment = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_respondedAt:{
               onInitProp(propId);
               this._respondedAt = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_surveySentAt:{
               onInitProp(propId);
               this._surveySentAt = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_surveyChannel:{
               onInitProp(propId);
               this._surveyChannel = (java.lang.String)value;
               
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
     * 关联工单: TICKET_ID
     */
    public final java.lang.Long getTicketId(){
         onPropGet(PROP_ID_ticketId);
         return _ticketId;
    }

    /**
     * 关联工单: TICKET_ID
     */
    public final void setTicketId(java.lang.Long value){
        if(onPropSet(PROP_ID_ticketId,value)){
            this._ticketId = value;
            internalClearRefs(PROP_ID_ticketId);
            
        }
    }
    
    /**
     * 调查令牌: SURVEY_TOKEN
     */
    public final java.lang.String getSurveyToken(){
         onPropGet(PROP_ID_surveyToken);
         return _surveyToken;
    }

    /**
     * 调查令牌: SURVEY_TOKEN
     */
    public final void setSurveyToken(java.lang.String value){
        if(onPropSet(PROP_ID_surveyToken,value)){
            this._surveyToken = value;
            internalClearRefs(PROP_ID_surveyToken);
            
        }
    }
    
    /**
     * CSAT评分: CSAT_SCORE
     */
    public final java.lang.Integer getCsatScore(){
         onPropGet(PROP_ID_csatScore);
         return _csatScore;
    }

    /**
     * CSAT评分: CSAT_SCORE
     */
    public final void setCsatScore(java.lang.Integer value){
        if(onPropSet(PROP_ID_csatScore,value)){
            this._csatScore = value;
            internalClearRefs(PROP_ID_csatScore);
            
        }
    }
    
    /**
     * NPS评分: NPS_SCORE
     */
    public final java.lang.Integer getNpsScore(){
         onPropGet(PROP_ID_npsScore);
         return _npsScore;
    }

    /**
     * NPS评分: NPS_SCORE
     */
    public final void setNpsScore(java.lang.Integer value){
        if(onPropSet(PROP_ID_npsScore,value)){
            this._npsScore = value;
            internalClearRefs(PROP_ID_npsScore);
            
        }
    }
    
    /**
     * CES评分: CES_SCORE
     */
    public final java.lang.Integer getCesScore(){
         onPropGet(PROP_ID_cesScore);
         return _cesScore;
    }

    /**
     * CES评分: CES_SCORE
     */
    public final void setCesScore(java.lang.Integer value){
        if(onPropSet(PROP_ID_cesScore,value)){
            this._cesScore = value;
            internalClearRefs(PROP_ID_cesScore);
            
        }
    }
    
    /**
     * 文字反馈: COMMENT
     */
    public final java.lang.String getComment(){
         onPropGet(PROP_ID_comment);
         return _comment;
    }

    /**
     * 文字反馈: COMMENT
     */
    public final void setComment(java.lang.String value){
        if(onPropSet(PROP_ID_comment,value)){
            this._comment = value;
            internalClearRefs(PROP_ID_comment);
            
        }
    }
    
    /**
     * 响应时间: RESPONDED_AT
     */
    public final java.sql.Timestamp getRespondedAt(){
         onPropGet(PROP_ID_respondedAt);
         return _respondedAt;
    }

    /**
     * 响应时间: RESPONDED_AT
     */
    public final void setRespondedAt(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_respondedAt,value)){
            this._respondedAt = value;
            internalClearRefs(PROP_ID_respondedAt);
            
        }
    }
    
    /**
     * 调查发送时间: SURVEY_SENT_AT
     */
    public final java.sql.Timestamp getSurveySentAt(){
         onPropGet(PROP_ID_surveySentAt);
         return _surveySentAt;
    }

    /**
     * 调查发送时间: SURVEY_SENT_AT
     */
    public final void setSurveySentAt(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_surveySentAt,value)){
            this._surveySentAt = value;
            internalClearRefs(PROP_ID_surveySentAt);
            
        }
    }
    
    /**
     * 发送渠道: SURVEY_CHANNEL
     */
    public final java.lang.String getSurveyChannel(){
         onPropGet(PROP_ID_surveyChannel);
         return _surveyChannel;
    }

    /**
     * 发送渠道: SURVEY_CHANNEL
     */
    public final void setSurveyChannel(java.lang.String value){
        if(onPropSet(PROP_ID_surveyChannel,value)){
            this._surveyChannel = value;
            internalClearRefs(PROP_ID_surveyChannel);
            
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
    public final app.erp.cs.dao.entity.ErpCsTicket getTicket(){
       return (app.erp.cs.dao.entity.ErpCsTicket)internalGetRefEntity(PROP_NAME_ticket);
    }

    public final void setTicket(app.erp.cs.dao.entity.ErpCsTicket refEntity){
   
           if(refEntity == null){
           
                   this.setTicketId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_ticket, refEntity,()->{
           
                           this.setTicketId(refEntity.getId());
                       
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
