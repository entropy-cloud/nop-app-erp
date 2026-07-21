package app.erp.fin.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.fin.dao.entity.ErpFinBudgetCarryForwardLog;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  预算结转日志: erp_fin_budget_carry_forward_log
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpFinBudgetCarryForwardLog extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 核算组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 2;
    
    /* 预算方案: SCENARIO_ID BIGINT */
    public static final String PROP_NAME_scenarioId = "scenarioId";
    public static final int PROP_ID_scenarioId = 3;
    
    /* 源方案: SOURCE_SCENARIO_ID BIGINT */
    public static final String PROP_NAME_sourceScenarioId = "sourceScenarioId";
    public static final int PROP_ID_sourceScenarioId = 4;
    
    /* 目标方案: TARGET_SCENARIO_ID BIGINT */
    public static final String PROP_NAME_targetScenarioId = "targetScenarioId";
    public static final int PROP_ID_targetScenarioId = 5;
    
    /* 结转规则: RULE VARCHAR */
    public static final String PROP_NAME_rule = "rule";
    public static final int PROP_ID_rule = 6;
    
    /* 源方案余量: SOURCE_REMAINING DECIMAL */
    public static final String PROP_NAME_sourceRemaining = "sourceRemaining";
    public static final int PROP_ID_sourceRemaining = 7;
    
    /* 源方案已用: SOURCE_USED DECIMAL */
    public static final String PROP_NAME_sourceUsed = "sourceUsed";
    public static final int PROP_ID_sourceUsed = 8;
    
    /* 结转金额: CARRIED_AMOUNT DECIMAL */
    public static final String PROP_NAME_carriedAmount = "carriedAmount";
    public static final int PROP_ID_carriedAmount = 9;
    
    /* 结转时间: CARRIED_AT TIMESTAMP */
    public static final String PROP_NAME_carriedAt = "carriedAt";
    public static final int PROP_ID_carriedAt = 10;
    
    /* 结转人: CARRIED_BY VARCHAR */
    public static final String PROP_NAME_carriedBy = "carriedBy";
    public static final int PROP_ID_carriedBy = 11;
    
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

    
    /* relation:  */
    public static final String PROP_NAME_scenario = "scenario";
    
    /* relation:  */
    public static final String PROP_NAME_sourceScenario = "sourceScenario";
    
    /* relation:  */
    public static final String PROP_NAME_targetScenario = "targetScenario";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[19];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_scenarioId] = PROP_NAME_scenarioId;
          PROP_NAME_TO_ID.put(PROP_NAME_scenarioId, PROP_ID_scenarioId);
      
          PROP_ID_TO_NAME[PROP_ID_sourceScenarioId] = PROP_NAME_sourceScenarioId;
          PROP_NAME_TO_ID.put(PROP_NAME_sourceScenarioId, PROP_ID_sourceScenarioId);
      
          PROP_ID_TO_NAME[PROP_ID_targetScenarioId] = PROP_NAME_targetScenarioId;
          PROP_NAME_TO_ID.put(PROP_NAME_targetScenarioId, PROP_ID_targetScenarioId);
      
          PROP_ID_TO_NAME[PROP_ID_rule] = PROP_NAME_rule;
          PROP_NAME_TO_ID.put(PROP_NAME_rule, PROP_ID_rule);
      
          PROP_ID_TO_NAME[PROP_ID_sourceRemaining] = PROP_NAME_sourceRemaining;
          PROP_NAME_TO_ID.put(PROP_NAME_sourceRemaining, PROP_ID_sourceRemaining);
      
          PROP_ID_TO_NAME[PROP_ID_sourceUsed] = PROP_NAME_sourceUsed;
          PROP_NAME_TO_ID.put(PROP_NAME_sourceUsed, PROP_ID_sourceUsed);
      
          PROP_ID_TO_NAME[PROP_ID_carriedAmount] = PROP_NAME_carriedAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_carriedAmount, PROP_ID_carriedAmount);
      
          PROP_ID_TO_NAME[PROP_ID_carriedAt] = PROP_NAME_carriedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_carriedAt, PROP_ID_carriedAt);
      
          PROP_ID_TO_NAME[PROP_ID_carriedBy] = PROP_NAME_carriedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_carriedBy, PROP_ID_carriedBy);
      
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
    
    /* 核算组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 预算方案: SCENARIO_ID */
    private java.lang.Long _scenarioId;
    
    /* 源方案: SOURCE_SCENARIO_ID */
    private java.lang.Long _sourceScenarioId;
    
    /* 目标方案: TARGET_SCENARIO_ID */
    private java.lang.Long _targetScenarioId;
    
    /* 结转规则: RULE */
    private java.lang.String _rule;
    
    /* 源方案余量: SOURCE_REMAINING */
    private java.math.BigDecimal _sourceRemaining;
    
    /* 源方案已用: SOURCE_USED */
    private java.math.BigDecimal _sourceUsed;
    
    /* 结转金额: CARRIED_AMOUNT */
    private java.math.BigDecimal _carriedAmount;
    
    /* 结转时间: CARRIED_AT */
    private java.sql.Timestamp _carriedAt;
    
    /* 结转人: CARRIED_BY */
    private java.lang.String _carriedBy;
    
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
    

    public _ErpFinBudgetCarryForwardLog(){
        // for debug
    }

    protected ErpFinBudgetCarryForwardLog newInstance(){
        ErpFinBudgetCarryForwardLog entity = new ErpFinBudgetCarryForwardLog();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpFinBudgetCarryForwardLog cloneInstance() {
        ErpFinBudgetCarryForwardLog entity = newInstance();
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
      return "app.erp.fin.dao.entity.ErpFinBudgetCarryForwardLog";
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
        
            case PROP_ID_scenarioId:
               return getScenarioId();
        
            case PROP_ID_sourceScenarioId:
               return getSourceScenarioId();
        
            case PROP_ID_targetScenarioId:
               return getTargetScenarioId();
        
            case PROP_ID_rule:
               return getRule();
        
            case PROP_ID_sourceRemaining:
               return getSourceRemaining();
        
            case PROP_ID_sourceUsed:
               return getSourceUsed();
        
            case PROP_ID_carriedAmount:
               return getCarriedAmount();
        
            case PROP_ID_carriedAt:
               return getCarriedAt();
        
            case PROP_ID_carriedBy:
               return getCarriedBy();
        
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
        
            case PROP_ID_orgId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_orgId));
               }
               setOrgId(typedValue);
               break;
            }
        
            case PROP_ID_scenarioId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_scenarioId));
               }
               setScenarioId(typedValue);
               break;
            }
        
            case PROP_ID_sourceScenarioId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_sourceScenarioId));
               }
               setSourceScenarioId(typedValue);
               break;
            }
        
            case PROP_ID_targetScenarioId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_targetScenarioId));
               }
               setTargetScenarioId(typedValue);
               break;
            }
        
            case PROP_ID_rule:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_rule));
               }
               setRule(typedValue);
               break;
            }
        
            case PROP_ID_sourceRemaining:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_sourceRemaining));
               }
               setSourceRemaining(typedValue);
               break;
            }
        
            case PROP_ID_sourceUsed:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_sourceUsed));
               }
               setSourceUsed(typedValue);
               break;
            }
        
            case PROP_ID_carriedAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_carriedAmount));
               }
               setCarriedAmount(typedValue);
               break;
            }
        
            case PROP_ID_carriedAt:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_carriedAt));
               }
               setCarriedAt(typedValue);
               break;
            }
        
            case PROP_ID_carriedBy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_carriedBy));
               }
               setCarriedBy(typedValue);
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
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_scenarioId:{
               onInitProp(propId);
               this._scenarioId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_sourceScenarioId:{
               onInitProp(propId);
               this._sourceScenarioId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_targetScenarioId:{
               onInitProp(propId);
               this._targetScenarioId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_rule:{
               onInitProp(propId);
               this._rule = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_sourceRemaining:{
               onInitProp(propId);
               this._sourceRemaining = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_sourceUsed:{
               onInitProp(propId);
               this._sourceUsed = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_carriedAmount:{
               onInitProp(propId);
               this._carriedAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_carriedAt:{
               onInitProp(propId);
               this._carriedAt = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_carriedBy:{
               onInitProp(propId);
               this._carriedBy = (java.lang.String)value;
               
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
     * 核算组织: ORG_ID
     */
    public final java.lang.Long getOrgId(){
         onPropGet(PROP_ID_orgId);
         return _orgId;
    }

    /**
     * 核算组织: ORG_ID
     */
    public final void setOrgId(java.lang.Long value){
        if(onPropSet(PROP_ID_orgId,value)){
            this._orgId = value;
            internalClearRefs(PROP_ID_orgId);
            
        }
    }
    
    /**
     * 预算方案: SCENARIO_ID
     */
    public final java.lang.Long getScenarioId(){
         onPropGet(PROP_ID_scenarioId);
         return _scenarioId;
    }

    /**
     * 预算方案: SCENARIO_ID
     */
    public final void setScenarioId(java.lang.Long value){
        if(onPropSet(PROP_ID_scenarioId,value)){
            this._scenarioId = value;
            internalClearRefs(PROP_ID_scenarioId);
            
        }
    }
    
    /**
     * 源方案: SOURCE_SCENARIO_ID
     */
    public final java.lang.Long getSourceScenarioId(){
         onPropGet(PROP_ID_sourceScenarioId);
         return _sourceScenarioId;
    }

    /**
     * 源方案: SOURCE_SCENARIO_ID
     */
    public final void setSourceScenarioId(java.lang.Long value){
        if(onPropSet(PROP_ID_sourceScenarioId,value)){
            this._sourceScenarioId = value;
            internalClearRefs(PROP_ID_sourceScenarioId);
            
        }
    }
    
    /**
     * 目标方案: TARGET_SCENARIO_ID
     */
    public final java.lang.Long getTargetScenarioId(){
         onPropGet(PROP_ID_targetScenarioId);
         return _targetScenarioId;
    }

    /**
     * 目标方案: TARGET_SCENARIO_ID
     */
    public final void setTargetScenarioId(java.lang.Long value){
        if(onPropSet(PROP_ID_targetScenarioId,value)){
            this._targetScenarioId = value;
            internalClearRefs(PROP_ID_targetScenarioId);
            
        }
    }
    
    /**
     * 结转规则: RULE
     */
    public final java.lang.String getRule(){
         onPropGet(PROP_ID_rule);
         return _rule;
    }

    /**
     * 结转规则: RULE
     */
    public final void setRule(java.lang.String value){
        if(onPropSet(PROP_ID_rule,value)){
            this._rule = value;
            internalClearRefs(PROP_ID_rule);
            
        }
    }
    
    /**
     * 源方案余量: SOURCE_REMAINING
     */
    public final java.math.BigDecimal getSourceRemaining(){
         onPropGet(PROP_ID_sourceRemaining);
         return _sourceRemaining;
    }

    /**
     * 源方案余量: SOURCE_REMAINING
     */
    public final void setSourceRemaining(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_sourceRemaining,value)){
            this._sourceRemaining = value;
            internalClearRefs(PROP_ID_sourceRemaining);
            
        }
    }
    
    /**
     * 源方案已用: SOURCE_USED
     */
    public final java.math.BigDecimal getSourceUsed(){
         onPropGet(PROP_ID_sourceUsed);
         return _sourceUsed;
    }

    /**
     * 源方案已用: SOURCE_USED
     */
    public final void setSourceUsed(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_sourceUsed,value)){
            this._sourceUsed = value;
            internalClearRefs(PROP_ID_sourceUsed);
            
        }
    }
    
    /**
     * 结转金额: CARRIED_AMOUNT
     */
    public final java.math.BigDecimal getCarriedAmount(){
         onPropGet(PROP_ID_carriedAmount);
         return _carriedAmount;
    }

    /**
     * 结转金额: CARRIED_AMOUNT
     */
    public final void setCarriedAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_carriedAmount,value)){
            this._carriedAmount = value;
            internalClearRefs(PROP_ID_carriedAmount);
            
        }
    }
    
    /**
     * 结转时间: CARRIED_AT
     */
    public final java.sql.Timestamp getCarriedAt(){
         onPropGet(PROP_ID_carriedAt);
         return _carriedAt;
    }

    /**
     * 结转时间: CARRIED_AT
     */
    public final void setCarriedAt(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_carriedAt,value)){
            this._carriedAt = value;
            internalClearRefs(PROP_ID_carriedAt);
            
        }
    }
    
    /**
     * 结转人: CARRIED_BY
     */
    public final java.lang.String getCarriedBy(){
         onPropGet(PROP_ID_carriedBy);
         return _carriedBy;
    }

    /**
     * 结转人: CARRIED_BY
     */
    public final void setCarriedBy(java.lang.String value){
        if(onPropSet(PROP_ID_carriedBy,value)){
            this._carriedBy = value;
            internalClearRefs(PROP_ID_carriedBy);
            
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
    public final app.erp.fin.dao.entity.ErpFinBudgetScenario getScenario(){
       return (app.erp.fin.dao.entity.ErpFinBudgetScenario)internalGetRefEntity(PROP_NAME_scenario);
    }

    public final void setScenario(app.erp.fin.dao.entity.ErpFinBudgetScenario refEntity){
   
           if(refEntity == null){
           
                   this.setScenarioId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_scenario, refEntity,()->{
           
                           this.setScenarioId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.fin.dao.entity.ErpFinBudgetScenario getSourceScenario(){
       return (app.erp.fin.dao.entity.ErpFinBudgetScenario)internalGetRefEntity(PROP_NAME_sourceScenario);
    }

    public final void setSourceScenario(app.erp.fin.dao.entity.ErpFinBudgetScenario refEntity){
   
           if(refEntity == null){
           
                   this.setSourceScenarioId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_sourceScenario, refEntity,()->{
           
                           this.setSourceScenarioId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.fin.dao.entity.ErpFinBudgetScenario getTargetScenario(){
       return (app.erp.fin.dao.entity.ErpFinBudgetScenario)internalGetRefEntity(PROP_NAME_targetScenario);
    }

    public final void setTargetScenario(app.erp.fin.dao.entity.ErpFinBudgetScenario refEntity){
   
           if(refEntity == null){
           
                   this.setTargetScenarioId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_targetScenario, refEntity,()->{
           
                           this.setTargetScenarioId(refEntity.getId());
                       
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
