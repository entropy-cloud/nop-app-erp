package app.erp.crm.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.crm.dao.entity.ErpCrmLeadScore;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  线索评分记录: erp_crm_lead_score
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpCrmLeadScore extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 线索/商机: LEAD_ID BIGINT */
    public static final String PROP_NAME_leadId = "leadId";
    public static final int PROP_ID_leadId = 2;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 评分规则版本: CONFIG_ID BIGINT */
    public static final String PROP_NAME_configId = "configId";
    public static final int PROP_ID_configId = 4;
    
    /* 总分: TOTAL_SCORE INTEGER */
    public static final String PROP_NAME_totalScore = "totalScore";
    public static final int PROP_ID_totalScore = 5;
    
    /* 评分快照(JSON): SCORE_BREAKDOWN VARCHAR */
    public static final String PROP_NAME_scoreBreakdown = "scoreBreakdown";
    public static final int PROP_ID_scoreBreakdown = 6;
    
    /* 是否自动转商机: AUTO_QUALIFIED BOOLEAN */
    public static final String PROP_NAME_autoQualified = "autoQualified";
    public static final int PROP_ID_autoQualified = 7;
    
    /* 触发动作: TRIGGERED_ACTION VARCHAR */
    public static final String PROP_NAME_triggeredAction = "triggeredAction";
    public static final int PROP_ID_triggeredAction = 8;
    
    /* 计算时间: CALCULATED_AT TIMESTAMP */
    public static final String PROP_NAME_calculatedAt = "calculatedAt";
    public static final int PROP_ID_calculatedAt = 9;
    
    /* 触发计算事件: TRIGGER_EVENT VARCHAR */
    public static final String PROP_NAME_triggerEvent = "triggerEvent";
    public static final int PROP_ID_triggerEvent = 10;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 11;
    
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
    public static final String PROP_NAME_lead = "lead";
    
    /* relation:  */
    public static final String PROP_NAME_config = "config";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[18];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_leadId] = PROP_NAME_leadId;
          PROP_NAME_TO_ID.put(PROP_NAME_leadId, PROP_ID_leadId);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_configId] = PROP_NAME_configId;
          PROP_NAME_TO_ID.put(PROP_NAME_configId, PROP_ID_configId);
      
          PROP_ID_TO_NAME[PROP_ID_totalScore] = PROP_NAME_totalScore;
          PROP_NAME_TO_ID.put(PROP_NAME_totalScore, PROP_ID_totalScore);
      
          PROP_ID_TO_NAME[PROP_ID_scoreBreakdown] = PROP_NAME_scoreBreakdown;
          PROP_NAME_TO_ID.put(PROP_NAME_scoreBreakdown, PROP_ID_scoreBreakdown);
      
          PROP_ID_TO_NAME[PROP_ID_autoQualified] = PROP_NAME_autoQualified;
          PROP_NAME_TO_ID.put(PROP_NAME_autoQualified, PROP_ID_autoQualified);
      
          PROP_ID_TO_NAME[PROP_ID_triggeredAction] = PROP_NAME_triggeredAction;
          PROP_NAME_TO_ID.put(PROP_NAME_triggeredAction, PROP_ID_triggeredAction);
      
          PROP_ID_TO_NAME[PROP_ID_calculatedAt] = PROP_NAME_calculatedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_calculatedAt, PROP_ID_calculatedAt);
      
          PROP_ID_TO_NAME[PROP_ID_triggerEvent] = PROP_NAME_triggerEvent;
          PROP_NAME_TO_ID.put(PROP_NAME_triggerEvent, PROP_ID_triggerEvent);
      
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
    
    /* 线索/商机: LEAD_ID */
    private java.lang.Long _leadId;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 评分规则版本: CONFIG_ID */
    private java.lang.Long _configId;
    
    /* 总分: TOTAL_SCORE */
    private java.lang.Integer _totalScore;
    
    /* 评分快照(JSON): SCORE_BREAKDOWN */
    private java.lang.String _scoreBreakdown;
    
    /* 是否自动转商机: AUTO_QUALIFIED */
    private java.lang.Boolean _autoQualified;
    
    /* 触发动作: TRIGGERED_ACTION */
    private java.lang.String _triggeredAction;
    
    /* 计算时间: CALCULATED_AT */
    private java.sql.Timestamp _calculatedAt;
    
    /* 触发计算事件: TRIGGER_EVENT */
    private java.lang.String _triggerEvent;
    
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
    

    public _ErpCrmLeadScore(){
        // for debug
    }

    protected ErpCrmLeadScore newInstance(){
        ErpCrmLeadScore entity = new ErpCrmLeadScore();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpCrmLeadScore cloneInstance() {
        ErpCrmLeadScore entity = newInstance();
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
      return "app.erp.crm.dao.entity.ErpCrmLeadScore";
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
        
            case PROP_ID_leadId:
               return getLeadId();
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_configId:
               return getConfigId();
        
            case PROP_ID_totalScore:
               return getTotalScore();
        
            case PROP_ID_scoreBreakdown:
               return getScoreBreakdown();
        
            case PROP_ID_autoQualified:
               return getAutoQualified();
        
            case PROP_ID_triggeredAction:
               return getTriggeredAction();
        
            case PROP_ID_calculatedAt:
               return getCalculatedAt();
        
            case PROP_ID_triggerEvent:
               return getTriggerEvent();
        
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
        
            case PROP_ID_leadId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_leadId));
               }
               setLeadId(typedValue);
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
        
            case PROP_ID_configId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_configId));
               }
               setConfigId(typedValue);
               break;
            }
        
            case PROP_ID_totalScore:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_totalScore));
               }
               setTotalScore(typedValue);
               break;
            }
        
            case PROP_ID_scoreBreakdown:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_scoreBreakdown));
               }
               setScoreBreakdown(typedValue);
               break;
            }
        
            case PROP_ID_autoQualified:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_autoQualified));
               }
               setAutoQualified(typedValue);
               break;
            }
        
            case PROP_ID_triggeredAction:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_triggeredAction));
               }
               setTriggeredAction(typedValue);
               break;
            }
        
            case PROP_ID_calculatedAt:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_calculatedAt));
               }
               setCalculatedAt(typedValue);
               break;
            }
        
            case PROP_ID_triggerEvent:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_triggerEvent));
               }
               setTriggerEvent(typedValue);
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
        
            case PROP_ID_leadId:{
               onInitProp(propId);
               this._leadId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_configId:{
               onInitProp(propId);
               this._configId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_totalScore:{
               onInitProp(propId);
               this._totalScore = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_scoreBreakdown:{
               onInitProp(propId);
               this._scoreBreakdown = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_autoQualified:{
               onInitProp(propId);
               this._autoQualified = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_triggeredAction:{
               onInitProp(propId);
               this._triggeredAction = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_calculatedAt:{
               onInitProp(propId);
               this._calculatedAt = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_triggerEvent:{
               onInitProp(propId);
               this._triggerEvent = (java.lang.String)value;
               
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
     * 线索/商机: LEAD_ID
     */
    public final java.lang.Long getLeadId(){
         onPropGet(PROP_ID_leadId);
         return _leadId;
    }

    /**
     * 线索/商机: LEAD_ID
     */
    public final void setLeadId(java.lang.Long value){
        if(onPropSet(PROP_ID_leadId,value)){
            this._leadId = value;
            internalClearRefs(PROP_ID_leadId);
            
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
     * 评分规则版本: CONFIG_ID
     */
    public final java.lang.Long getConfigId(){
         onPropGet(PROP_ID_configId);
         return _configId;
    }

    /**
     * 评分规则版本: CONFIG_ID
     */
    public final void setConfigId(java.lang.Long value){
        if(onPropSet(PROP_ID_configId,value)){
            this._configId = value;
            internalClearRefs(PROP_ID_configId);
            
        }
    }
    
    /**
     * 总分: TOTAL_SCORE
     */
    public final java.lang.Integer getTotalScore(){
         onPropGet(PROP_ID_totalScore);
         return _totalScore;
    }

    /**
     * 总分: TOTAL_SCORE
     */
    public final void setTotalScore(java.lang.Integer value){
        if(onPropSet(PROP_ID_totalScore,value)){
            this._totalScore = value;
            internalClearRefs(PROP_ID_totalScore);
            
        }
    }
    
    /**
     * 评分快照(JSON): SCORE_BREAKDOWN
     */
    public final java.lang.String getScoreBreakdown(){
         onPropGet(PROP_ID_scoreBreakdown);
         return _scoreBreakdown;
    }

    /**
     * 评分快照(JSON): SCORE_BREAKDOWN
     */
    public final void setScoreBreakdown(java.lang.String value){
        if(onPropSet(PROP_ID_scoreBreakdown,value)){
            this._scoreBreakdown = value;
            internalClearRefs(PROP_ID_scoreBreakdown);
            
        }
    }
    
    /**
     * 是否自动转商机: AUTO_QUALIFIED
     */
    public final java.lang.Boolean getAutoQualified(){
         onPropGet(PROP_ID_autoQualified);
         return _autoQualified;
    }

    /**
     * 是否自动转商机: AUTO_QUALIFIED
     */
    public final void setAutoQualified(java.lang.Boolean value){
        if(onPropSet(PROP_ID_autoQualified,value)){
            this._autoQualified = value;
            internalClearRefs(PROP_ID_autoQualified);
            
        }
    }
    
    /**
     * 触发动作: TRIGGERED_ACTION
     */
    public final java.lang.String getTriggeredAction(){
         onPropGet(PROP_ID_triggeredAction);
         return _triggeredAction;
    }

    /**
     * 触发动作: TRIGGERED_ACTION
     */
    public final void setTriggeredAction(java.lang.String value){
        if(onPropSet(PROP_ID_triggeredAction,value)){
            this._triggeredAction = value;
            internalClearRefs(PROP_ID_triggeredAction);
            
        }
    }
    
    /**
     * 计算时间: CALCULATED_AT
     */
    public final java.sql.Timestamp getCalculatedAt(){
         onPropGet(PROP_ID_calculatedAt);
         return _calculatedAt;
    }

    /**
     * 计算时间: CALCULATED_AT
     */
    public final void setCalculatedAt(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_calculatedAt,value)){
            this._calculatedAt = value;
            internalClearRefs(PROP_ID_calculatedAt);
            
        }
    }
    
    /**
     * 触发计算事件: TRIGGER_EVENT
     */
    public final java.lang.String getTriggerEvent(){
         onPropGet(PROP_ID_triggerEvent);
         return _triggerEvent;
    }

    /**
     * 触发计算事件: TRIGGER_EVENT
     */
    public final void setTriggerEvent(java.lang.String value){
        if(onPropSet(PROP_ID_triggerEvent,value)){
            this._triggerEvent = value;
            internalClearRefs(PROP_ID_triggerEvent);
            
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
    public final app.erp.crm.dao.entity.ErpCrmLead getLead(){
       return (app.erp.crm.dao.entity.ErpCrmLead)internalGetRefEntity(PROP_NAME_lead);
    }

    public final void setLead(app.erp.crm.dao.entity.ErpCrmLead refEntity){
   
           if(refEntity == null){
           
                   this.setLeadId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_lead, refEntity,()->{
           
                           this.setLeadId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.crm.dao.entity.ErpCrmLeadScoreConfig getConfig(){
       return (app.erp.crm.dao.entity.ErpCrmLeadScoreConfig)internalGetRefEntity(PROP_NAME_config);
    }

    public final void setConfig(app.erp.crm.dao.entity.ErpCrmLeadScoreConfig refEntity){
   
           if(refEntity == null){
           
                   this.setConfigId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_config, refEntity,()->{
           
                           this.setConfigId(refEntity.getId());
                       
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
