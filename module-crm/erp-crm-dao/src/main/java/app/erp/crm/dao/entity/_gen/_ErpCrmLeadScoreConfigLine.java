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

import app.erp.crm.dao.entity.ErpCrmLeadScoreConfigLine;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  评分准则明细: erp_crm_lead_score_config_line
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpCrmLeadScoreConfigLine extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 评分规则配置: CONFIG_ID BIGINT */
    public static final String PROP_NAME_configId = "configId";
    public static final int PROP_ID_configId = 2;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 准则编码: CRITERION_CODE VARCHAR */
    public static final String PROP_NAME_criterionCode = "criterionCode";
    public static final int PROP_ID_criterionCode = 4;
    
    /* 准则名称: CRITERION_NAME VARCHAR */
    public static final String PROP_NAME_criterionName = "criterionName";
    public static final int PROP_ID_criterionName = 5;
    
    /* 权重系数: WEIGHT INTEGER */
    public static final String PROP_NAME_weight = "weight";
    public static final int PROP_ID_weight = 6;
    
    /* 评分方法: SCORING_METHOD VARCHAR */
    public static final String PROP_NAME_scoringMethod = "scoringMethod";
    public static final int PROP_ID_scoringMethod = 7;
    
    /* 值表(JSON): LOOKUP_TABLE VARCHAR */
    public static final String PROP_NAME_lookupTable = "lookupTable";
    public static final int PROP_ID_lookupTable = 8;
    
    /* 公式表达式: FORMULA VARCHAR */
    public static final String PROP_NAME_formula = "formula";
    public static final int PROP_ID_formula = 9;
    
    /* 最高分: MAX_SCORE INTEGER */
    public static final String PROP_NAME_maxScore = "maxScore";
    public static final int PROP_ID_maxScore = 10;
    
    /* 排序: SEQUENCE INTEGER */
    public static final String PROP_NAME_sequence = "sequence";
    public static final int PROP_ID_sequence = 11;
    
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
    public static final String PROP_NAME_config = "config";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[19];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_configId] = PROP_NAME_configId;
          PROP_NAME_TO_ID.put(PROP_NAME_configId, PROP_ID_configId);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_criterionCode] = PROP_NAME_criterionCode;
          PROP_NAME_TO_ID.put(PROP_NAME_criterionCode, PROP_ID_criterionCode);
      
          PROP_ID_TO_NAME[PROP_ID_criterionName] = PROP_NAME_criterionName;
          PROP_NAME_TO_ID.put(PROP_NAME_criterionName, PROP_ID_criterionName);
      
          PROP_ID_TO_NAME[PROP_ID_weight] = PROP_NAME_weight;
          PROP_NAME_TO_ID.put(PROP_NAME_weight, PROP_ID_weight);
      
          PROP_ID_TO_NAME[PROP_ID_scoringMethod] = PROP_NAME_scoringMethod;
          PROP_NAME_TO_ID.put(PROP_NAME_scoringMethod, PROP_ID_scoringMethod);
      
          PROP_ID_TO_NAME[PROP_ID_lookupTable] = PROP_NAME_lookupTable;
          PROP_NAME_TO_ID.put(PROP_NAME_lookupTable, PROP_ID_lookupTable);
      
          PROP_ID_TO_NAME[PROP_ID_formula] = PROP_NAME_formula;
          PROP_NAME_TO_ID.put(PROP_NAME_formula, PROP_ID_formula);
      
          PROP_ID_TO_NAME[PROP_ID_maxScore] = PROP_NAME_maxScore;
          PROP_NAME_TO_ID.put(PROP_NAME_maxScore, PROP_ID_maxScore);
      
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
    
    /* 评分规则配置: CONFIG_ID */
    private java.lang.Long _configId;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 准则编码: CRITERION_CODE */
    private java.lang.String _criterionCode;
    
    /* 准则名称: CRITERION_NAME */
    private java.lang.String _criterionName;
    
    /* 权重系数: WEIGHT */
    private java.lang.Integer _weight;
    
    /* 评分方法: SCORING_METHOD */
    private java.lang.String _scoringMethod;
    
    /* 值表(JSON): LOOKUP_TABLE */
    private java.lang.String _lookupTable;
    
    /* 公式表达式: FORMULA */
    private java.lang.String _formula;
    
    /* 最高分: MAX_SCORE */
    private java.lang.Integer _maxScore;
    
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
    

    public _ErpCrmLeadScoreConfigLine(){
        // for debug
    }

    protected ErpCrmLeadScoreConfigLine newInstance(){
        ErpCrmLeadScoreConfigLine entity = new ErpCrmLeadScoreConfigLine();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpCrmLeadScoreConfigLine cloneInstance() {
        ErpCrmLeadScoreConfigLine entity = newInstance();
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
      return "app.erp.crm.dao.entity.ErpCrmLeadScoreConfigLine";
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
        
            case PROP_ID_configId:
               return getConfigId();
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_criterionCode:
               return getCriterionCode();
        
            case PROP_ID_criterionName:
               return getCriterionName();
        
            case PROP_ID_weight:
               return getWeight();
        
            case PROP_ID_scoringMethod:
               return getScoringMethod();
        
            case PROP_ID_lookupTable:
               return getLookupTable();
        
            case PROP_ID_formula:
               return getFormula();
        
            case PROP_ID_maxScore:
               return getMaxScore();
        
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
        
            case PROP_ID_configId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_configId));
               }
               setConfigId(typedValue);
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
        
            case PROP_ID_criterionCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_criterionCode));
               }
               setCriterionCode(typedValue);
               break;
            }
        
            case PROP_ID_criterionName:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_criterionName));
               }
               setCriterionName(typedValue);
               break;
            }
        
            case PROP_ID_weight:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_weight));
               }
               setWeight(typedValue);
               break;
            }
        
            case PROP_ID_scoringMethod:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_scoringMethod));
               }
               setScoringMethod(typedValue);
               break;
            }
        
            case PROP_ID_lookupTable:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_lookupTable));
               }
               setLookupTable(typedValue);
               break;
            }
        
            case PROP_ID_formula:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_formula));
               }
               setFormula(typedValue);
               break;
            }
        
            case PROP_ID_maxScore:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_maxScore));
               }
               setMaxScore(typedValue);
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
        
            case PROP_ID_configId:{
               onInitProp(propId);
               this._configId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_criterionCode:{
               onInitProp(propId);
               this._criterionCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_criterionName:{
               onInitProp(propId);
               this._criterionName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_weight:{
               onInitProp(propId);
               this._weight = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_scoringMethod:{
               onInitProp(propId);
               this._scoringMethod = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_lookupTable:{
               onInitProp(propId);
               this._lookupTable = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_formula:{
               onInitProp(propId);
               this._formula = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_maxScore:{
               onInitProp(propId);
               this._maxScore = (java.lang.Integer)value;
               
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
     * 评分规则配置: CONFIG_ID
     */
    public final java.lang.Long getConfigId(){
         onPropGet(PROP_ID_configId);
         return _configId;
    }

    /**
     * 评分规则配置: CONFIG_ID
     */
    public final void setConfigId(java.lang.Long value){
        if(onPropSet(PROP_ID_configId,value)){
            this._configId = value;
            internalClearRefs(PROP_ID_configId);
            
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
     * 准则编码: CRITERION_CODE
     */
    public final java.lang.String getCriterionCode(){
         onPropGet(PROP_ID_criterionCode);
         return _criterionCode;
    }

    /**
     * 准则编码: CRITERION_CODE
     */
    public final void setCriterionCode(java.lang.String value){
        if(onPropSet(PROP_ID_criterionCode,value)){
            this._criterionCode = value;
            internalClearRefs(PROP_ID_criterionCode);
            
        }
    }
    
    /**
     * 准则名称: CRITERION_NAME
     */
    public final java.lang.String getCriterionName(){
         onPropGet(PROP_ID_criterionName);
         return _criterionName;
    }

    /**
     * 准则名称: CRITERION_NAME
     */
    public final void setCriterionName(java.lang.String value){
        if(onPropSet(PROP_ID_criterionName,value)){
            this._criterionName = value;
            internalClearRefs(PROP_ID_criterionName);
            
        }
    }
    
    /**
     * 权重系数: WEIGHT
     */
    public final java.lang.Integer getWeight(){
         onPropGet(PROP_ID_weight);
         return _weight;
    }

    /**
     * 权重系数: WEIGHT
     */
    public final void setWeight(java.lang.Integer value){
        if(onPropSet(PROP_ID_weight,value)){
            this._weight = value;
            internalClearRefs(PROP_ID_weight);
            
        }
    }
    
    /**
     * 评分方法: SCORING_METHOD
     */
    public final java.lang.String getScoringMethod(){
         onPropGet(PROP_ID_scoringMethod);
         return _scoringMethod;
    }

    /**
     * 评分方法: SCORING_METHOD
     */
    public final void setScoringMethod(java.lang.String value){
        if(onPropSet(PROP_ID_scoringMethod,value)){
            this._scoringMethod = value;
            internalClearRefs(PROP_ID_scoringMethod);
            
        }
    }
    
    /**
     * 值表(JSON): LOOKUP_TABLE
     */
    public final java.lang.String getLookupTable(){
         onPropGet(PROP_ID_lookupTable);
         return _lookupTable;
    }

    /**
     * 值表(JSON): LOOKUP_TABLE
     */
    public final void setLookupTable(java.lang.String value){
        if(onPropSet(PROP_ID_lookupTable,value)){
            this._lookupTable = value;
            internalClearRefs(PROP_ID_lookupTable);
            
        }
    }
    
    /**
     * 公式表达式: FORMULA
     */
    public final java.lang.String getFormula(){
         onPropGet(PROP_ID_formula);
         return _formula;
    }

    /**
     * 公式表达式: FORMULA
     */
    public final void setFormula(java.lang.String value){
        if(onPropSet(PROP_ID_formula,value)){
            this._formula = value;
            internalClearRefs(PROP_ID_formula);
            
        }
    }
    
    /**
     * 最高分: MAX_SCORE
     */
    public final java.lang.Integer getMaxScore(){
         onPropGet(PROP_ID_maxScore);
         return _maxScore;
    }

    /**
     * 最高分: MAX_SCORE
     */
    public final void setMaxScore(java.lang.Integer value){
        if(onPropSet(PROP_ID_maxScore,value)){
            this._maxScore = value;
            internalClearRefs(PROP_ID_maxScore);
            
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
