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

import app.erp.notify.dao.entity.ErpSysNotificationTemplate;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  通知模板: erp_sys_notification_template
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpSysNotificationTemplate extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 业务事件键: NOTIFICATION_TYPE VARCHAR */
    public static final String PROP_NAME_notificationType = "notificationType";
    public static final int PROP_ID_notificationType = 2;
    
    /* 模板名称: NAME VARCHAR */
    public static final String PROP_NAME_name = "name";
    public static final int PROP_ID_name = 3;
    
    /* 渠道集合: CHANNEL_SET VARCHAR */
    public static final String PROP_NAME_channelSet = "channelSet";
    public static final int PROP_ID_channelSet = 4;
    
    /* 标题模板: SUBJECT_TPL VARCHAR */
    public static final String PROP_NAME_subjectTpl = "subjectTpl";
    public static final int PROP_ID_subjectTpl = 5;
    
    /* 正文模板: BODY_TPL VARCHAR */
    public static final String PROP_NAME_bodyTpl = "bodyTpl";
    public static final int PROP_ID_bodyTpl = 6;
    
    /* 接收人解析器: RECIPIENT_RESOLVER VARCHAR */
    public static final String PROP_NAME_recipientResolver = "recipientResolver";
    public static final int PROP_ID_recipientResolver = 7;
    
    /* 接收人配置(JSON): RECIPIENT_CONFIG VARCHAR */
    public static final String PROP_NAME_recipientConfig = "recipientConfig";
    public static final int PROP_ID_recipientConfig = 8;
    
    /* 合并窗口(秒): MERGE_WINDOW_SECONDS INTEGER */
    public static final String PROP_NAME_mergeWindowSeconds = "mergeWindowSeconds";
    public static final int PROP_ID_mergeWindowSeconds = 9;
    
    /* 合并策略: MERGE_STRATEGY VARCHAR */
    public static final String PROP_NAME_mergeStrategy = "mergeStrategy";
    public static final int PROP_ID_mergeStrategy = 10;
    
    /* 模板状态: STATUS VARCHAR */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 11;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 12;
    
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

    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[19];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_notificationType] = PROP_NAME_notificationType;
          PROP_NAME_TO_ID.put(PROP_NAME_notificationType, PROP_ID_notificationType);
      
          PROP_ID_TO_NAME[PROP_ID_name] = PROP_NAME_name;
          PROP_NAME_TO_ID.put(PROP_NAME_name, PROP_ID_name);
      
          PROP_ID_TO_NAME[PROP_ID_channelSet] = PROP_NAME_channelSet;
          PROP_NAME_TO_ID.put(PROP_NAME_channelSet, PROP_ID_channelSet);
      
          PROP_ID_TO_NAME[PROP_ID_subjectTpl] = PROP_NAME_subjectTpl;
          PROP_NAME_TO_ID.put(PROP_NAME_subjectTpl, PROP_ID_subjectTpl);
      
          PROP_ID_TO_NAME[PROP_ID_bodyTpl] = PROP_NAME_bodyTpl;
          PROP_NAME_TO_ID.put(PROP_NAME_bodyTpl, PROP_ID_bodyTpl);
      
          PROP_ID_TO_NAME[PROP_ID_recipientResolver] = PROP_NAME_recipientResolver;
          PROP_NAME_TO_ID.put(PROP_NAME_recipientResolver, PROP_ID_recipientResolver);
      
          PROP_ID_TO_NAME[PROP_ID_recipientConfig] = PROP_NAME_recipientConfig;
          PROP_NAME_TO_ID.put(PROP_NAME_recipientConfig, PROP_ID_recipientConfig);
      
          PROP_ID_TO_NAME[PROP_ID_mergeWindowSeconds] = PROP_NAME_mergeWindowSeconds;
          PROP_NAME_TO_ID.put(PROP_NAME_mergeWindowSeconds, PROP_ID_mergeWindowSeconds);
      
          PROP_ID_TO_NAME[PROP_ID_mergeStrategy] = PROP_NAME_mergeStrategy;
          PROP_NAME_TO_ID.put(PROP_NAME_mergeStrategy, PROP_ID_mergeStrategy);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
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
    
    /* 业务事件键: NOTIFICATION_TYPE */
    private java.lang.String _notificationType;
    
    /* 模板名称: NAME */
    private java.lang.String _name;
    
    /* 渠道集合: CHANNEL_SET */
    private java.lang.String _channelSet;
    
    /* 标题模板: SUBJECT_TPL */
    private java.lang.String _subjectTpl;
    
    /* 正文模板: BODY_TPL */
    private java.lang.String _bodyTpl;
    
    /* 接收人解析器: RECIPIENT_RESOLVER */
    private java.lang.String _recipientResolver;
    
    /* 接收人配置(JSON): RECIPIENT_CONFIG */
    private java.lang.String _recipientConfig;
    
    /* 合并窗口(秒): MERGE_WINDOW_SECONDS */
    private java.lang.Integer _mergeWindowSeconds;
    
    /* 合并策略: MERGE_STRATEGY */
    private java.lang.String _mergeStrategy;
    
    /* 模板状态: STATUS */
    private java.lang.String _status;
    
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
    

    public _ErpSysNotificationTemplate(){
        // for debug
    }

    protected ErpSysNotificationTemplate newInstance(){
        ErpSysNotificationTemplate entity = new ErpSysNotificationTemplate();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpSysNotificationTemplate cloneInstance() {
        ErpSysNotificationTemplate entity = newInstance();
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
      return "app.erp.notify.dao.entity.ErpSysNotificationTemplate";
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
        
            case PROP_ID_notificationType:
               return getNotificationType();
        
            case PROP_ID_name:
               return getName();
        
            case PROP_ID_channelSet:
               return getChannelSet();
        
            case PROP_ID_subjectTpl:
               return getSubjectTpl();
        
            case PROP_ID_bodyTpl:
               return getBodyTpl();
        
            case PROP_ID_recipientResolver:
               return getRecipientResolver();
        
            case PROP_ID_recipientConfig:
               return getRecipientConfig();
        
            case PROP_ID_mergeWindowSeconds:
               return getMergeWindowSeconds();
        
            case PROP_ID_mergeStrategy:
               return getMergeStrategy();
        
            case PROP_ID_status:
               return getStatus();
        
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
        
            case PROP_ID_notificationType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_notificationType));
               }
               setNotificationType(typedValue);
               break;
            }
        
            case PROP_ID_name:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_name));
               }
               setName(typedValue);
               break;
            }
        
            case PROP_ID_channelSet:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_channelSet));
               }
               setChannelSet(typedValue);
               break;
            }
        
            case PROP_ID_subjectTpl:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_subjectTpl));
               }
               setSubjectTpl(typedValue);
               break;
            }
        
            case PROP_ID_bodyTpl:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_bodyTpl));
               }
               setBodyTpl(typedValue);
               break;
            }
        
            case PROP_ID_recipientResolver:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_recipientResolver));
               }
               setRecipientResolver(typedValue);
               break;
            }
        
            case PROP_ID_recipientConfig:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_recipientConfig));
               }
               setRecipientConfig(typedValue);
               break;
            }
        
            case PROP_ID_mergeWindowSeconds:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_mergeWindowSeconds));
               }
               setMergeWindowSeconds(typedValue);
               break;
            }
        
            case PROP_ID_mergeStrategy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_mergeStrategy));
               }
               setMergeStrategy(typedValue);
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
        
            case PROP_ID_notificationType:{
               onInitProp(propId);
               this._notificationType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_name:{
               onInitProp(propId);
               this._name = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_channelSet:{
               onInitProp(propId);
               this._channelSet = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_subjectTpl:{
               onInitProp(propId);
               this._subjectTpl = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_bodyTpl:{
               onInitProp(propId);
               this._bodyTpl = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_recipientResolver:{
               onInitProp(propId);
               this._recipientResolver = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_recipientConfig:{
               onInitProp(propId);
               this._recipientConfig = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_mergeWindowSeconds:{
               onInitProp(propId);
               this._mergeWindowSeconds = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_mergeStrategy:{
               onInitProp(propId);
               this._mergeStrategy = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.String)value;
               
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
     * 模板名称: NAME
     */
    public final java.lang.String getName(){
         onPropGet(PROP_ID_name);
         return _name;
    }

    /**
     * 模板名称: NAME
     */
    public final void setName(java.lang.String value){
        if(onPropSet(PROP_ID_name,value)){
            this._name = value;
            internalClearRefs(PROP_ID_name);
            
        }
    }
    
    /**
     * 渠道集合: CHANNEL_SET
     */
    public final java.lang.String getChannelSet(){
         onPropGet(PROP_ID_channelSet);
         return _channelSet;
    }

    /**
     * 渠道集合: CHANNEL_SET
     */
    public final void setChannelSet(java.lang.String value){
        if(onPropSet(PROP_ID_channelSet,value)){
            this._channelSet = value;
            internalClearRefs(PROP_ID_channelSet);
            
        }
    }
    
    /**
     * 标题模板: SUBJECT_TPL
     */
    public final java.lang.String getSubjectTpl(){
         onPropGet(PROP_ID_subjectTpl);
         return _subjectTpl;
    }

    /**
     * 标题模板: SUBJECT_TPL
     */
    public final void setSubjectTpl(java.lang.String value){
        if(onPropSet(PROP_ID_subjectTpl,value)){
            this._subjectTpl = value;
            internalClearRefs(PROP_ID_subjectTpl);
            
        }
    }
    
    /**
     * 正文模板: BODY_TPL
     */
    public final java.lang.String getBodyTpl(){
         onPropGet(PROP_ID_bodyTpl);
         return _bodyTpl;
    }

    /**
     * 正文模板: BODY_TPL
     */
    public final void setBodyTpl(java.lang.String value){
        if(onPropSet(PROP_ID_bodyTpl,value)){
            this._bodyTpl = value;
            internalClearRefs(PROP_ID_bodyTpl);
            
        }
    }
    
    /**
     * 接收人解析器: RECIPIENT_RESOLVER
     */
    public final java.lang.String getRecipientResolver(){
         onPropGet(PROP_ID_recipientResolver);
         return _recipientResolver;
    }

    /**
     * 接收人解析器: RECIPIENT_RESOLVER
     */
    public final void setRecipientResolver(java.lang.String value){
        if(onPropSet(PROP_ID_recipientResolver,value)){
            this._recipientResolver = value;
            internalClearRefs(PROP_ID_recipientResolver);
            
        }
    }
    
    /**
     * 接收人配置(JSON): RECIPIENT_CONFIG
     */
    public final java.lang.String getRecipientConfig(){
         onPropGet(PROP_ID_recipientConfig);
         return _recipientConfig;
    }

    /**
     * 接收人配置(JSON): RECIPIENT_CONFIG
     */
    public final void setRecipientConfig(java.lang.String value){
        if(onPropSet(PROP_ID_recipientConfig,value)){
            this._recipientConfig = value;
            internalClearRefs(PROP_ID_recipientConfig);
            
        }
    }
    
    /**
     * 合并窗口(秒): MERGE_WINDOW_SECONDS
     */
    public final java.lang.Integer getMergeWindowSeconds(){
         onPropGet(PROP_ID_mergeWindowSeconds);
         return _mergeWindowSeconds;
    }

    /**
     * 合并窗口(秒): MERGE_WINDOW_SECONDS
     */
    public final void setMergeWindowSeconds(java.lang.Integer value){
        if(onPropSet(PROP_ID_mergeWindowSeconds,value)){
            this._mergeWindowSeconds = value;
            internalClearRefs(PROP_ID_mergeWindowSeconds);
            
        }
    }
    
    /**
     * 合并策略: MERGE_STRATEGY
     */
    public final java.lang.String getMergeStrategy(){
         onPropGet(PROP_ID_mergeStrategy);
         return _mergeStrategy;
    }

    /**
     * 合并策略: MERGE_STRATEGY
     */
    public final void setMergeStrategy(java.lang.String value){
        if(onPropSet(PROP_ID_mergeStrategy,value)){
            this._mergeStrategy = value;
            internalClearRefs(PROP_ID_mergeStrategy);
            
        }
    }
    
    /**
     * 模板状态: STATUS
     */
    public final java.lang.String getStatus(){
         onPropGet(PROP_ID_status);
         return _status;
    }

    /**
     * 模板状态: STATUS
     */
    public final void setStatus(java.lang.String value){
        if(onPropSet(PROP_ID_status,value)){
            this._status = value;
            internalClearRefs(PROP_ID_status);
            
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
    
}
// resume CPD analysis - CPD-ON
