package app.erp.notify.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.notify.dao.entity.ErpSysNotification;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  通知实例: erp_sys_notification
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpSysNotification extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 模板ID: TEMPLATE_ID BIGINT */
    public static final String PROP_NAME_templateId = "templateId";
    public static final int PROP_ID_templateId = 2;
    
    /* 业务事件键: NOTIFICATION_TYPE VARCHAR */
    public static final String PROP_NAME_notificationType = "notificationType";
    public static final int PROP_ID_notificationType = 3;
    
    /* 接收用户: RECIPIENT_USER_ID VARCHAR */
    public static final String PROP_NAME_recipientUserId = "recipientUserId";
    public static final int PROP_ID_recipientUserId = 4;
    
    /* 接收业务伙伴: RECIPIENT_PARTNER_ID BIGINT */
    public static final String PROP_NAME_recipientPartnerId = "recipientPartnerId";
    public static final int PROP_ID_recipientPartnerId = 5;
    
    /* 接收部门: RECIPIENT_DEPT_ID BIGINT */
    public static final String PROP_NAME_recipientDeptId = "recipientDeptId";
    public static final int PROP_ID_recipientDeptId = 6;
    
    /* 渠道: CHANNEL VARCHAR */
    public static final String PROP_NAME_channel = "channel";
    public static final int PROP_ID_channel = 7;
    
    /* 标题: SUBJECT VARCHAR */
    public static final String PROP_NAME_subject = "subject";
    public static final int PROP_ID_subject = 8;
    
    /* 正文: BODY VARCHAR */
    public static final String PROP_NAME_body = "body";
    public static final int PROP_ID_body = 9;
    
    /* 负载JSON: PAYLOAD_JSON VARCHAR */
    public static final String PROP_NAME_payloadJson = "payloadJson";
    public static final int PROP_ID_payloadJson = 10;
    
    /* 状态: STATUS VARCHAR */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 11;
    
    /* 合并组ID: MERGE_GROUP_ID VARCHAR */
    public static final String PROP_NAME_mergeGroupId = "mergeGroupId";
    public static final int PROP_ID_mergeGroupId = 12;
    
    /* 合并次数: MERGE_COUNT INTEGER */
    public static final String PROP_NAME_mergeCount = "mergeCount";
    public static final int PROP_ID_mergeCount = 13;
    
    /* 发送时间: SENT_AT TIMESTAMP */
    public static final String PROP_NAME_sentAt = "sentAt";
    public static final int PROP_ID_sentAt = 14;
    
    /* 错误信息: ERROR_MSG VARCHAR */
    public static final String PROP_NAME_errorMsg = "errorMsg";
    public static final int PROP_ID_errorMsg = 15;
    
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
    public static final String PROP_NAME_template = "template";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[22];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_templateId] = PROP_NAME_templateId;
          PROP_NAME_TO_ID.put(PROP_NAME_templateId, PROP_ID_templateId);
      
          PROP_ID_TO_NAME[PROP_ID_notificationType] = PROP_NAME_notificationType;
          PROP_NAME_TO_ID.put(PROP_NAME_notificationType, PROP_ID_notificationType);
      
          PROP_ID_TO_NAME[PROP_ID_recipientUserId] = PROP_NAME_recipientUserId;
          PROP_NAME_TO_ID.put(PROP_NAME_recipientUserId, PROP_ID_recipientUserId);
      
          PROP_ID_TO_NAME[PROP_ID_recipientPartnerId] = PROP_NAME_recipientPartnerId;
          PROP_NAME_TO_ID.put(PROP_NAME_recipientPartnerId, PROP_ID_recipientPartnerId);
      
          PROP_ID_TO_NAME[PROP_ID_recipientDeptId] = PROP_NAME_recipientDeptId;
          PROP_NAME_TO_ID.put(PROP_NAME_recipientDeptId, PROP_ID_recipientDeptId);
      
          PROP_ID_TO_NAME[PROP_ID_channel] = PROP_NAME_channel;
          PROP_NAME_TO_ID.put(PROP_NAME_channel, PROP_ID_channel);
      
          PROP_ID_TO_NAME[PROP_ID_subject] = PROP_NAME_subject;
          PROP_NAME_TO_ID.put(PROP_NAME_subject, PROP_ID_subject);
      
          PROP_ID_TO_NAME[PROP_ID_body] = PROP_NAME_body;
          PROP_NAME_TO_ID.put(PROP_NAME_body, PROP_ID_body);
      
          PROP_ID_TO_NAME[PROP_ID_payloadJson] = PROP_NAME_payloadJson;
          PROP_NAME_TO_ID.put(PROP_NAME_payloadJson, PROP_ID_payloadJson);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_mergeGroupId] = PROP_NAME_mergeGroupId;
          PROP_NAME_TO_ID.put(PROP_NAME_mergeGroupId, PROP_ID_mergeGroupId);
      
          PROP_ID_TO_NAME[PROP_ID_mergeCount] = PROP_NAME_mergeCount;
          PROP_NAME_TO_ID.put(PROP_NAME_mergeCount, PROP_ID_mergeCount);
      
          PROP_ID_TO_NAME[PROP_ID_sentAt] = PROP_NAME_sentAt;
          PROP_NAME_TO_ID.put(PROP_NAME_sentAt, PROP_ID_sentAt);
      
          PROP_ID_TO_NAME[PROP_ID_errorMsg] = PROP_NAME_errorMsg;
          PROP_NAME_TO_ID.put(PROP_NAME_errorMsg, PROP_ID_errorMsg);
      
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
    
    /* 模板ID: TEMPLATE_ID */
    private java.lang.Long _templateId;
    
    /* 业务事件键: NOTIFICATION_TYPE */
    private java.lang.String _notificationType;
    
    /* 接收用户: RECIPIENT_USER_ID */
    private java.lang.String _recipientUserId;
    
    /* 接收业务伙伴: RECIPIENT_PARTNER_ID */
    private java.lang.Long _recipientPartnerId;
    
    /* 接收部门: RECIPIENT_DEPT_ID */
    private java.lang.Long _recipientDeptId;
    
    /* 渠道: CHANNEL */
    private java.lang.String _channel;
    
    /* 标题: SUBJECT */
    private java.lang.String _subject;
    
    /* 正文: BODY */
    private java.lang.String _body;
    
    /* 负载JSON: PAYLOAD_JSON */
    private java.lang.String _payloadJson;
    
    /* 状态: STATUS */
    private java.lang.String _status;
    
    /* 合并组ID: MERGE_GROUP_ID */
    private java.lang.String _mergeGroupId;
    
    /* 合并次数: MERGE_COUNT */
    private java.lang.Integer _mergeCount;
    
    /* 发送时间: SENT_AT */
    private java.sql.Timestamp _sentAt;
    
    /* 错误信息: ERROR_MSG */
    private java.lang.String _errorMsg;
    
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
    

    public _ErpSysNotification(){
        // for debug
    }

    protected ErpSysNotification newInstance(){
        ErpSysNotification entity = new ErpSysNotification();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpSysNotification cloneInstance() {
        ErpSysNotification entity = newInstance();
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
      return "app.erp.notify.dao.entity.ErpSysNotification";
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
        
            case PROP_ID_templateId:
               return getTemplateId();
        
            case PROP_ID_notificationType:
               return getNotificationType();
        
            case PROP_ID_recipientUserId:
               return getRecipientUserId();
        
            case PROP_ID_recipientPartnerId:
               return getRecipientPartnerId();
        
            case PROP_ID_recipientDeptId:
               return getRecipientDeptId();
        
            case PROP_ID_channel:
               return getChannel();
        
            case PROP_ID_subject:
               return getSubject();
        
            case PROP_ID_body:
               return getBody();
        
            case PROP_ID_payloadJson:
               return getPayloadJson();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_mergeGroupId:
               return getMergeGroupId();
        
            case PROP_ID_mergeCount:
               return getMergeCount();
        
            case PROP_ID_sentAt:
               return getSentAt();
        
            case PROP_ID_errorMsg:
               return getErrorMsg();
        
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
        
            case PROP_ID_templateId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_templateId));
               }
               setTemplateId(typedValue);
               break;
            }
        
            case PROP_ID_notificationType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_notificationType));
               }
               setNotificationType(typedValue);
               break;
            }
        
            case PROP_ID_recipientUserId:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_recipientUserId));
               }
               setRecipientUserId(typedValue);
               break;
            }
        
            case PROP_ID_recipientPartnerId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_recipientPartnerId));
               }
               setRecipientPartnerId(typedValue);
               break;
            }
        
            case PROP_ID_recipientDeptId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_recipientDeptId));
               }
               setRecipientDeptId(typedValue);
               break;
            }
        
            case PROP_ID_channel:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_channel));
               }
               setChannel(typedValue);
               break;
            }
        
            case PROP_ID_subject:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_subject));
               }
               setSubject(typedValue);
               break;
            }
        
            case PROP_ID_body:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_body));
               }
               setBody(typedValue);
               break;
            }
        
            case PROP_ID_payloadJson:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_payloadJson));
               }
               setPayloadJson(typedValue);
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
        
            case PROP_ID_mergeGroupId:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_mergeGroupId));
               }
               setMergeGroupId(typedValue);
               break;
            }
        
            case PROP_ID_mergeCount:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_mergeCount));
               }
               setMergeCount(typedValue);
               break;
            }
        
            case PROP_ID_sentAt:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_sentAt));
               }
               setSentAt(typedValue);
               break;
            }
        
            case PROP_ID_errorMsg:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_errorMsg));
               }
               setErrorMsg(typedValue);
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
        
            case PROP_ID_templateId:{
               onInitProp(propId);
               this._templateId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_notificationType:{
               onInitProp(propId);
               this._notificationType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_recipientUserId:{
               onInitProp(propId);
               this._recipientUserId = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_recipientPartnerId:{
               onInitProp(propId);
               this._recipientPartnerId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_recipientDeptId:{
               onInitProp(propId);
               this._recipientDeptId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_channel:{
               onInitProp(propId);
               this._channel = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_subject:{
               onInitProp(propId);
               this._subject = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_body:{
               onInitProp(propId);
               this._body = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_payloadJson:{
               onInitProp(propId);
               this._payloadJson = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_mergeGroupId:{
               onInitProp(propId);
               this._mergeGroupId = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_mergeCount:{
               onInitProp(propId);
               this._mergeCount = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_sentAt:{
               onInitProp(propId);
               this._sentAt = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_errorMsg:{
               onInitProp(propId);
               this._errorMsg = (java.lang.String)value;
               
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
     * 模板ID: TEMPLATE_ID
     */
    public final java.lang.Long getTemplateId(){
         onPropGet(PROP_ID_templateId);
         return _templateId;
    }

    /**
     * 模板ID: TEMPLATE_ID
     */
    public final void setTemplateId(java.lang.Long value){
        if(onPropSet(PROP_ID_templateId,value)){
            this._templateId = value;
            internalClearRefs(PROP_ID_templateId);
            
        }
    }
    
    /**
     * 业务事件键: NOTIFICATION_TYPE
     */
    public final java.lang.String getNotificationType(){
         onPropGet(PROP_ID_notificationType);
         return _notificationType;
    }

    /**
     * 业务事件键: NOTIFICATION_TYPE
     */
    public final void setNotificationType(java.lang.String value){
        if(onPropSet(PROP_ID_notificationType,value)){
            this._notificationType = value;
            internalClearRefs(PROP_ID_notificationType);
            
        }
    }
    
    /**
     * 接收用户: RECIPIENT_USER_ID
     */
    public final java.lang.String getRecipientUserId(){
         onPropGet(PROP_ID_recipientUserId);
         return _recipientUserId;
    }

    /**
     * 接收用户: RECIPIENT_USER_ID
     */
    public final void setRecipientUserId(java.lang.String value){
        if(onPropSet(PROP_ID_recipientUserId,value)){
            this._recipientUserId = value;
            internalClearRefs(PROP_ID_recipientUserId);
            
        }
    }
    
    /**
     * 接收业务伙伴: RECIPIENT_PARTNER_ID
     */
    public final java.lang.Long getRecipientPartnerId(){
         onPropGet(PROP_ID_recipientPartnerId);
         return _recipientPartnerId;
    }

    /**
     * 接收业务伙伴: RECIPIENT_PARTNER_ID
     */
    public final void setRecipientPartnerId(java.lang.Long value){
        if(onPropSet(PROP_ID_recipientPartnerId,value)){
            this._recipientPartnerId = value;
            internalClearRefs(PROP_ID_recipientPartnerId);
            
        }
    }
    
    /**
     * 接收部门: RECIPIENT_DEPT_ID
     */
    public final java.lang.Long getRecipientDeptId(){
         onPropGet(PROP_ID_recipientDeptId);
         return _recipientDeptId;
    }

    /**
     * 接收部门: RECIPIENT_DEPT_ID
     */
    public final void setRecipientDeptId(java.lang.Long value){
        if(onPropSet(PROP_ID_recipientDeptId,value)){
            this._recipientDeptId = value;
            internalClearRefs(PROP_ID_recipientDeptId);
            
        }
    }
    
    /**
     * 渠道: CHANNEL
     */
    public final java.lang.String getChannel(){
         onPropGet(PROP_ID_channel);
         return _channel;
    }

    /**
     * 渠道: CHANNEL
     */
    public final void setChannel(java.lang.String value){
        if(onPropSet(PROP_ID_channel,value)){
            this._channel = value;
            internalClearRefs(PROP_ID_channel);
            
        }
    }
    
    /**
     * 标题: SUBJECT
     */
    public final java.lang.String getSubject(){
         onPropGet(PROP_ID_subject);
         return _subject;
    }

    /**
     * 标题: SUBJECT
     */
    public final void setSubject(java.lang.String value){
        if(onPropSet(PROP_ID_subject,value)){
            this._subject = value;
            internalClearRefs(PROP_ID_subject);
            
        }
    }
    
    /**
     * 正文: BODY
     */
    public final java.lang.String getBody(){
         onPropGet(PROP_ID_body);
         return _body;
    }

    /**
     * 正文: BODY
     */
    public final void setBody(java.lang.String value){
        if(onPropSet(PROP_ID_body,value)){
            this._body = value;
            internalClearRefs(PROP_ID_body);
            
        }
    }
    
    /**
     * 负载JSON: PAYLOAD_JSON
     */
    public final java.lang.String getPayloadJson(){
         onPropGet(PROP_ID_payloadJson);
         return _payloadJson;
    }

    /**
     * 负载JSON: PAYLOAD_JSON
     */
    public final void setPayloadJson(java.lang.String value){
        if(onPropSet(PROP_ID_payloadJson,value)){
            this._payloadJson = value;
            internalClearRefs(PROP_ID_payloadJson);
            
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
     * 合并组ID: MERGE_GROUP_ID
     */
    public final java.lang.String getMergeGroupId(){
         onPropGet(PROP_ID_mergeGroupId);
         return _mergeGroupId;
    }

    /**
     * 合并组ID: MERGE_GROUP_ID
     */
    public final void setMergeGroupId(java.lang.String value){
        if(onPropSet(PROP_ID_mergeGroupId,value)){
            this._mergeGroupId = value;
            internalClearRefs(PROP_ID_mergeGroupId);
            
        }
    }
    
    /**
     * 合并次数: MERGE_COUNT
     */
    public final java.lang.Integer getMergeCount(){
         onPropGet(PROP_ID_mergeCount);
         return _mergeCount;
    }

    /**
     * 合并次数: MERGE_COUNT
     */
    public final void setMergeCount(java.lang.Integer value){
        if(onPropSet(PROP_ID_mergeCount,value)){
            this._mergeCount = value;
            internalClearRefs(PROP_ID_mergeCount);
            
        }
    }
    
    /**
     * 发送时间: SENT_AT
     */
    public final java.sql.Timestamp getSentAt(){
         onPropGet(PROP_ID_sentAt);
         return _sentAt;
    }

    /**
     * 发送时间: SENT_AT
     */
    public final void setSentAt(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_sentAt,value)){
            this._sentAt = value;
            internalClearRefs(PROP_ID_sentAt);
            
        }
    }
    
    /**
     * 错误信息: ERROR_MSG
     */
    public final java.lang.String getErrorMsg(){
         onPropGet(PROP_ID_errorMsg);
         return _errorMsg;
    }

    /**
     * 错误信息: ERROR_MSG
     */
    public final void setErrorMsg(java.lang.String value){
        if(onPropSet(PROP_ID_errorMsg,value)){
            this._errorMsg = value;
            internalClearRefs(PROP_ID_errorMsg);
            
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
    public final app.erp.notify.dao.entity.ErpSysNotificationTemplate getTemplate(){
       return (app.erp.notify.dao.entity.ErpSysNotificationTemplate)internalGetRefEntity(PROP_NAME_template);
    }

    public final void setTemplate(app.erp.notify.dao.entity.ErpSysNotificationTemplate refEntity){
   
           if(refEntity == null){
           
                   this.setTemplateId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_template, refEntity,()->{
           
                           this.setTemplateId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
