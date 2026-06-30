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

import app.erp.crm.dao.entity.ErpCrmConfigRule;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  配置规则: erp_crm_config_rule
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpCrmConfigRule extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 所属配置器: CONFIGURATOR_ID BIGINT */
    public static final String PROP_NAME_configuratorId = "configuratorId";
    public static final int PROP_ID_configuratorId = 2;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 规则类型: RULE_TYPE VARCHAR */
    public static final String PROP_NAME_ruleType = "ruleType";
    public static final int PROP_ID_ruleType = 4;
    
    /* 条件特征编码: SOURCE_FEATURE_CODE VARCHAR */
    public static final String PROP_NAME_sourceFeatureCode = "sourceFeatureCode";
    public static final int PROP_ID_sourceFeatureCode = 5;
    
    /* 条件特征值: SOURCE_FEATURE_VALUE VARCHAR */
    public static final String PROP_NAME_sourceFeatureValue = "sourceFeatureValue";
    public static final int PROP_ID_sourceFeatureValue = 6;
    
    /* 目标特征编码: TARGET_FEATURE_CODE VARCHAR */
    public static final String PROP_NAME_targetFeatureCode = "targetFeatureCode";
    public static final int PROP_ID_targetFeatureCode = 7;
    
    /* 目标特征值: TARGET_FEATURE_VALUE VARCHAR */
    public static final String PROP_NAME_targetFeatureValue = "targetFeatureValue";
    public static final int PROP_ID_targetFeatureValue = 8;
    
    /* 复杂条件表达式: CONDITION_EXPRESSION VARCHAR */
    public static final String PROP_NAME_conditionExpression = "conditionExpression";
    public static final int PROP_ID_conditionExpression = 9;
    
    /* 排序: SEQUENCE INTEGER */
    public static final String PROP_NAME_sequence = "sequence";
    public static final int PROP_ID_sequence = 10;
    
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
    public static final String PROP_NAME_configurator = "configurator";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[18];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_configuratorId] = PROP_NAME_configuratorId;
          PROP_NAME_TO_ID.put(PROP_NAME_configuratorId, PROP_ID_configuratorId);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_ruleType] = PROP_NAME_ruleType;
          PROP_NAME_TO_ID.put(PROP_NAME_ruleType, PROP_ID_ruleType);
      
          PROP_ID_TO_NAME[PROP_ID_sourceFeatureCode] = PROP_NAME_sourceFeatureCode;
          PROP_NAME_TO_ID.put(PROP_NAME_sourceFeatureCode, PROP_ID_sourceFeatureCode);
      
          PROP_ID_TO_NAME[PROP_ID_sourceFeatureValue] = PROP_NAME_sourceFeatureValue;
          PROP_NAME_TO_ID.put(PROP_NAME_sourceFeatureValue, PROP_ID_sourceFeatureValue);
      
          PROP_ID_TO_NAME[PROP_ID_targetFeatureCode] = PROP_NAME_targetFeatureCode;
          PROP_NAME_TO_ID.put(PROP_NAME_targetFeatureCode, PROP_ID_targetFeatureCode);
      
          PROP_ID_TO_NAME[PROP_ID_targetFeatureValue] = PROP_NAME_targetFeatureValue;
          PROP_NAME_TO_ID.put(PROP_NAME_targetFeatureValue, PROP_ID_targetFeatureValue);
      
          PROP_ID_TO_NAME[PROP_ID_conditionExpression] = PROP_NAME_conditionExpression;
          PROP_NAME_TO_ID.put(PROP_NAME_conditionExpression, PROP_ID_conditionExpression);
      
          PROP_ID_TO_NAME[PROP_ID_sequence] = PROP_NAME_sequence;
          PROP_NAME_TO_ID.put(PROP_NAME_sequence, PROP_ID_sequence);
      
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
    
    /* 所属配置器: CONFIGURATOR_ID */
    private java.lang.Long _configuratorId;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 规则类型: RULE_TYPE */
    private java.lang.String _ruleType;
    
    /* 条件特征编码: SOURCE_FEATURE_CODE */
    private java.lang.String _sourceFeatureCode;
    
    /* 条件特征值: SOURCE_FEATURE_VALUE */
    private java.lang.String _sourceFeatureValue;
    
    /* 目标特征编码: TARGET_FEATURE_CODE */
    private java.lang.String _targetFeatureCode;
    
    /* 目标特征值: TARGET_FEATURE_VALUE */
    private java.lang.String _targetFeatureValue;
    
    /* 复杂条件表达式: CONDITION_EXPRESSION */
    private java.lang.String _conditionExpression;
    
    /* 排序: SEQUENCE */
    private java.lang.Integer _sequence;
    
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
    

    public _ErpCrmConfigRule(){
        // for debug
    }

    protected ErpCrmConfigRule newInstance(){
        ErpCrmConfigRule entity = new ErpCrmConfigRule();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpCrmConfigRule cloneInstance() {
        ErpCrmConfigRule entity = newInstance();
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
      return "app.erp.crm.dao.entity.ErpCrmConfigRule";
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
        
            case PROP_ID_configuratorId:
               return getConfiguratorId();
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_ruleType:
               return getRuleType();
        
            case PROP_ID_sourceFeatureCode:
               return getSourceFeatureCode();
        
            case PROP_ID_sourceFeatureValue:
               return getSourceFeatureValue();
        
            case PROP_ID_targetFeatureCode:
               return getTargetFeatureCode();
        
            case PROP_ID_targetFeatureValue:
               return getTargetFeatureValue();
        
            case PROP_ID_conditionExpression:
               return getConditionExpression();
        
            case PROP_ID_sequence:
               return getSequence();
        
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
        
            case PROP_ID_configuratorId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_configuratorId));
               }
               setConfiguratorId(typedValue);
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
        
            case PROP_ID_ruleType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_ruleType));
               }
               setRuleType(typedValue);
               break;
            }
        
            case PROP_ID_sourceFeatureCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_sourceFeatureCode));
               }
               setSourceFeatureCode(typedValue);
               break;
            }
        
            case PROP_ID_sourceFeatureValue:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_sourceFeatureValue));
               }
               setSourceFeatureValue(typedValue);
               break;
            }
        
            case PROP_ID_targetFeatureCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_targetFeatureCode));
               }
               setTargetFeatureCode(typedValue);
               break;
            }
        
            case PROP_ID_targetFeatureValue:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_targetFeatureValue));
               }
               setTargetFeatureValue(typedValue);
               break;
            }
        
            case PROP_ID_conditionExpression:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_conditionExpression));
               }
               setConditionExpression(typedValue);
               break;
            }
        
            case PROP_ID_sequence:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_sequence));
               }
               setSequence(typedValue);
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
        
            case PROP_ID_configuratorId:{
               onInitProp(propId);
               this._configuratorId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_ruleType:{
               onInitProp(propId);
               this._ruleType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_sourceFeatureCode:{
               onInitProp(propId);
               this._sourceFeatureCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_sourceFeatureValue:{
               onInitProp(propId);
               this._sourceFeatureValue = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_targetFeatureCode:{
               onInitProp(propId);
               this._targetFeatureCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_targetFeatureValue:{
               onInitProp(propId);
               this._targetFeatureValue = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_conditionExpression:{
               onInitProp(propId);
               this._conditionExpression = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_sequence:{
               onInitProp(propId);
               this._sequence = (java.lang.Integer)value;
               
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
     * 所属配置器: CONFIGURATOR_ID
     */
    public final java.lang.Long getConfiguratorId(){
         onPropGet(PROP_ID_configuratorId);
         return _configuratorId;
    }

    /**
     * 所属配置器: CONFIGURATOR_ID
     */
    public final void setConfiguratorId(java.lang.Long value){
        if(onPropSet(PROP_ID_configuratorId,value)){
            this._configuratorId = value;
            internalClearRefs(PROP_ID_configuratorId);
            
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
     * 规则类型: RULE_TYPE
     */
    public final java.lang.String getRuleType(){
         onPropGet(PROP_ID_ruleType);
         return _ruleType;
    }

    /**
     * 规则类型: RULE_TYPE
     */
    public final void setRuleType(java.lang.String value){
        if(onPropSet(PROP_ID_ruleType,value)){
            this._ruleType = value;
            internalClearRefs(PROP_ID_ruleType);
            
        }
    }
    
    /**
     * 条件特征编码: SOURCE_FEATURE_CODE
     */
    public final java.lang.String getSourceFeatureCode(){
         onPropGet(PROP_ID_sourceFeatureCode);
         return _sourceFeatureCode;
    }

    /**
     * 条件特征编码: SOURCE_FEATURE_CODE
     */
    public final void setSourceFeatureCode(java.lang.String value){
        if(onPropSet(PROP_ID_sourceFeatureCode,value)){
            this._sourceFeatureCode = value;
            internalClearRefs(PROP_ID_sourceFeatureCode);
            
        }
    }
    
    /**
     * 条件特征值: SOURCE_FEATURE_VALUE
     */
    public final java.lang.String getSourceFeatureValue(){
         onPropGet(PROP_ID_sourceFeatureValue);
         return _sourceFeatureValue;
    }

    /**
     * 条件特征值: SOURCE_FEATURE_VALUE
     */
    public final void setSourceFeatureValue(java.lang.String value){
        if(onPropSet(PROP_ID_sourceFeatureValue,value)){
            this._sourceFeatureValue = value;
            internalClearRefs(PROP_ID_sourceFeatureValue);
            
        }
    }
    
    /**
     * 目标特征编码: TARGET_FEATURE_CODE
     */
    public final java.lang.String getTargetFeatureCode(){
         onPropGet(PROP_ID_targetFeatureCode);
         return _targetFeatureCode;
    }

    /**
     * 目标特征编码: TARGET_FEATURE_CODE
     */
    public final void setTargetFeatureCode(java.lang.String value){
        if(onPropSet(PROP_ID_targetFeatureCode,value)){
            this._targetFeatureCode = value;
            internalClearRefs(PROP_ID_targetFeatureCode);
            
        }
    }
    
    /**
     * 目标特征值: TARGET_FEATURE_VALUE
     */
    public final java.lang.String getTargetFeatureValue(){
         onPropGet(PROP_ID_targetFeatureValue);
         return _targetFeatureValue;
    }

    /**
     * 目标特征值: TARGET_FEATURE_VALUE
     */
    public final void setTargetFeatureValue(java.lang.String value){
        if(onPropSet(PROP_ID_targetFeatureValue,value)){
            this._targetFeatureValue = value;
            internalClearRefs(PROP_ID_targetFeatureValue);
            
        }
    }
    
    /**
     * 复杂条件表达式: CONDITION_EXPRESSION
     */
    public final java.lang.String getConditionExpression(){
         onPropGet(PROP_ID_conditionExpression);
         return _conditionExpression;
    }

    /**
     * 复杂条件表达式: CONDITION_EXPRESSION
     */
    public final void setConditionExpression(java.lang.String value){
        if(onPropSet(PROP_ID_conditionExpression,value)){
            this._conditionExpression = value;
            internalClearRefs(PROP_ID_conditionExpression);
            
        }
    }
    
    /**
     * 排序: SEQUENCE
     */
    public final java.lang.Integer getSequence(){
         onPropGet(PROP_ID_sequence);
         return _sequence;
    }

    /**
     * 排序: SEQUENCE
     */
    public final void setSequence(java.lang.Integer value){
        if(onPropSet(PROP_ID_sequence,value)){
            this._sequence = value;
            internalClearRefs(PROP_ID_sequence);
            
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
    public final app.erp.crm.dao.entity.ErpCrmProductConfigurator getConfigurator(){
       return (app.erp.crm.dao.entity.ErpCrmProductConfigurator)internalGetRefEntity(PROP_NAME_configurator);
    }

    public final void setConfigurator(app.erp.crm.dao.entity.ErpCrmProductConfigurator refEntity){
   
           if(refEntity == null){
           
                   this.setConfiguratorId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_configurator, refEntity,()->{
           
                           this.setConfiguratorId(refEntity.getId());
                       
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
