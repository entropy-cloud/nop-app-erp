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

import app.erp.fin.dao.entity.ErpFinBudgetRollforwardLog;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  预算滚动复制日志: erp_fin_budget_rollforward_log
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpFinBudgetRollforwardLog extends DynamicOrmEntity{
    
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
    
    /* 复制策略: STRATEGY VARCHAR */
    public static final String PROP_NAME_strategy = "strategy";
    public static final int PROP_ID_strategy = 6;
    
    /* 目标年度: NEW_FISCAL_YEAR INTEGER */
    public static final String PROP_NAME_newFiscalYear = "newFiscalYear";
    public static final int PROP_ID_newFiscalYear = 7;
    
    /* 源金额合计: SOURCE_AMOUNT DECIMAL */
    public static final String PROP_NAME_sourceAmount = "sourceAmount";
    public static final int PROP_ID_sourceAmount = 8;
    
    /* 目标金额合计: TARGET_AMOUNT DECIMAL */
    public static final String PROP_NAME_targetAmount = "targetAmount";
    public static final int PROP_ID_targetAmount = 9;
    
    /* 复制时间: ROLLED_AT TIMESTAMP */
    public static final String PROP_NAME_rolledAt = "rolledAt";
    public static final int PROP_ID_rolledAt = 10;
    
    /* 复制人: ROLLED_BY VARCHAR */
    public static final String PROP_NAME_rolledBy = "rolledBy";
    public static final int PROP_ID_rolledBy = 11;
    
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
      
          PROP_ID_TO_NAME[PROP_ID_strategy] = PROP_NAME_strategy;
          PROP_NAME_TO_ID.put(PROP_NAME_strategy, PROP_ID_strategy);
      
          PROP_ID_TO_NAME[PROP_ID_newFiscalYear] = PROP_NAME_newFiscalYear;
          PROP_NAME_TO_ID.put(PROP_NAME_newFiscalYear, PROP_ID_newFiscalYear);
      
          PROP_ID_TO_NAME[PROP_ID_sourceAmount] = PROP_NAME_sourceAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_sourceAmount, PROP_ID_sourceAmount);
      
          PROP_ID_TO_NAME[PROP_ID_targetAmount] = PROP_NAME_targetAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_targetAmount, PROP_ID_targetAmount);
      
          PROP_ID_TO_NAME[PROP_ID_rolledAt] = PROP_NAME_rolledAt;
          PROP_NAME_TO_ID.put(PROP_NAME_rolledAt, PROP_ID_rolledAt);
      
          PROP_ID_TO_NAME[PROP_ID_rolledBy] = PROP_NAME_rolledBy;
          PROP_NAME_TO_ID.put(PROP_NAME_rolledBy, PROP_ID_rolledBy);
      
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
    
    /* 复制策略: STRATEGY */
    private java.lang.String _strategy;
    
    /* 目标年度: NEW_FISCAL_YEAR */
    private java.lang.Integer _newFiscalYear;
    
    /* 源金额合计: SOURCE_AMOUNT */
    private java.math.BigDecimal _sourceAmount;
    
    /* 目标金额合计: TARGET_AMOUNT */
    private java.math.BigDecimal _targetAmount;
    
    /* 复制时间: ROLLED_AT */
    private java.sql.Timestamp _rolledAt;
    
    /* 复制人: ROLLED_BY */
    private java.lang.String _rolledBy;
    
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
    

    public _ErpFinBudgetRollforwardLog(){
        // for debug
    }

    protected ErpFinBudgetRollforwardLog newInstance(){
        ErpFinBudgetRollforwardLog entity = new ErpFinBudgetRollforwardLog();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpFinBudgetRollforwardLog cloneInstance() {
        ErpFinBudgetRollforwardLog entity = newInstance();
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
      return "app.erp.fin.dao.entity.ErpFinBudgetRollforwardLog";
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
        
            case PROP_ID_strategy:
               return getStrategy();
        
            case PROP_ID_newFiscalYear:
               return getNewFiscalYear();
        
            case PROP_ID_sourceAmount:
               return getSourceAmount();
        
            case PROP_ID_targetAmount:
               return getTargetAmount();
        
            case PROP_ID_rolledAt:
               return getRolledAt();
        
            case PROP_ID_rolledBy:
               return getRolledBy();
        
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
        
            case PROP_ID_strategy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_strategy));
               }
               setStrategy(typedValue);
               break;
            }
        
            case PROP_ID_newFiscalYear:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_newFiscalYear));
               }
               setNewFiscalYear(typedValue);
               break;
            }
        
            case PROP_ID_sourceAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_sourceAmount));
               }
               setSourceAmount(typedValue);
               break;
            }
        
            case PROP_ID_targetAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_targetAmount));
               }
               setTargetAmount(typedValue);
               break;
            }
        
            case PROP_ID_rolledAt:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_rolledAt));
               }
               setRolledAt(typedValue);
               break;
            }
        
            case PROP_ID_rolledBy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_rolledBy));
               }
               setRolledBy(typedValue);
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
        
            case PROP_ID_strategy:{
               onInitProp(propId);
               this._strategy = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_newFiscalYear:{
               onInitProp(propId);
               this._newFiscalYear = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_sourceAmount:{
               onInitProp(propId);
               this._sourceAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_targetAmount:{
               onInitProp(propId);
               this._targetAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_rolledAt:{
               onInitProp(propId);
               this._rolledAt = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_rolledBy:{
               onInitProp(propId);
               this._rolledBy = (java.lang.String)value;
               
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
     * 复制策略: STRATEGY
     */
    public final java.lang.String getStrategy(){
         onPropGet(PROP_ID_strategy);
         return _strategy;
    }

    /**
     * 复制策略: STRATEGY
     */
    public final void setStrategy(java.lang.String value){
        if(onPropSet(PROP_ID_strategy,value)){
            this._strategy = value;
            internalClearRefs(PROP_ID_strategy);
            
        }
    }
    
    /**
     * 目标年度: NEW_FISCAL_YEAR
     */
    public final java.lang.Integer getNewFiscalYear(){
         onPropGet(PROP_ID_newFiscalYear);
         return _newFiscalYear;
    }

    /**
     * 目标年度: NEW_FISCAL_YEAR
     */
    public final void setNewFiscalYear(java.lang.Integer value){
        if(onPropSet(PROP_ID_newFiscalYear,value)){
            this._newFiscalYear = value;
            internalClearRefs(PROP_ID_newFiscalYear);
            
        }
    }
    
    /**
     * 源金额合计: SOURCE_AMOUNT
     */
    public final java.math.BigDecimal getSourceAmount(){
         onPropGet(PROP_ID_sourceAmount);
         return _sourceAmount;
    }

    /**
     * 源金额合计: SOURCE_AMOUNT
     */
    public final void setSourceAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_sourceAmount,value)){
            this._sourceAmount = value;
            internalClearRefs(PROP_ID_sourceAmount);
            
        }
    }
    
    /**
     * 目标金额合计: TARGET_AMOUNT
     */
    public final java.math.BigDecimal getTargetAmount(){
         onPropGet(PROP_ID_targetAmount);
         return _targetAmount;
    }

    /**
     * 目标金额合计: TARGET_AMOUNT
     */
    public final void setTargetAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_targetAmount,value)){
            this._targetAmount = value;
            internalClearRefs(PROP_ID_targetAmount);
            
        }
    }
    
    /**
     * 复制时间: ROLLED_AT
     */
    public final java.sql.Timestamp getRolledAt(){
         onPropGet(PROP_ID_rolledAt);
         return _rolledAt;
    }

    /**
     * 复制时间: ROLLED_AT
     */
    public final void setRolledAt(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_rolledAt,value)){
            this._rolledAt = value;
            internalClearRefs(PROP_ID_rolledAt);
            
        }
    }
    
    /**
     * 复制人: ROLLED_BY
     */
    public final java.lang.String getRolledBy(){
         onPropGet(PROP_ID_rolledBy);
         return _rolledBy;
    }

    /**
     * 复制人: ROLLED_BY
     */
    public final void setRolledBy(java.lang.String value){
        if(onPropSet(PROP_ID_rolledBy,value)){
            this._rolledBy = value;
            internalClearRefs(PROP_ID_rolledBy);
            
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
