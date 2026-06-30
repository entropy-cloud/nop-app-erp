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

import app.erp.crm.dao.entity.ErpCrmLeadScoreLine;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  评分记录明细: erp_crm_lead_score_line
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpCrmLeadScoreLine extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 评分记录: SCORE_ID BIGINT */
    public static final String PROP_NAME_scoreId = "scoreId";
    public static final int PROP_ID_scoreId = 2;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 对应准则行: CONFIG_LINE_ID BIGINT */
    public static final String PROP_NAME_configLineId = "configLineId";
    public static final int PROP_ID_configLineId = 4;
    
    /* 准则编码(快照): CRITERION_CODE VARCHAR */
    public static final String PROP_NAME_criterionCode = "criterionCode";
    public static final int PROP_ID_criterionCode = 5;
    
    /* 准则名称(快照): CRITERION_NAME VARCHAR */
    public static final String PROP_NAME_criterionName = "criterionName";
    public static final int PROP_ID_criterionName = 6;
    
    /* 原始值: RAW_VALUE VARCHAR */
    public static final String PROP_NAME_rawValue = "rawValue";
    public static final int PROP_ID_rawValue = 7;
    
    /* 匹配档次: LOOKUP_VALUE VARCHAR */
    public static final String PROP_NAME_lookupValue = "lookupValue";
    public static final int PROP_ID_lookupValue = 8;
    
    /* 原始得分: RAW_SCORE INTEGER */
    public static final String PROP_NAME_rawScore = "rawScore";
    public static final int PROP_ID_rawScore = 9;
    
    /* 加权得分: WEIGHTED_SCORE INTEGER */
    public static final String PROP_NAME_weightedScore = "weightedScore";
    public static final int PROP_ID_weightedScore = 10;
    
    /* 排序: SEQUENCE INTEGER */
    public static final String PROP_NAME_sequence = "sequence";
    public static final int PROP_ID_sequence = 11;
    
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
    public static final String PROP_NAME_score = "score";
    
    /* relation:  */
    public static final String PROP_NAME_configLine = "configLine";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[18];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_scoreId] = PROP_NAME_scoreId;
          PROP_NAME_TO_ID.put(PROP_NAME_scoreId, PROP_ID_scoreId);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_configLineId] = PROP_NAME_configLineId;
          PROP_NAME_TO_ID.put(PROP_NAME_configLineId, PROP_ID_configLineId);
      
          PROP_ID_TO_NAME[PROP_ID_criterionCode] = PROP_NAME_criterionCode;
          PROP_NAME_TO_ID.put(PROP_NAME_criterionCode, PROP_ID_criterionCode);
      
          PROP_ID_TO_NAME[PROP_ID_criterionName] = PROP_NAME_criterionName;
          PROP_NAME_TO_ID.put(PROP_NAME_criterionName, PROP_ID_criterionName);
      
          PROP_ID_TO_NAME[PROP_ID_rawValue] = PROP_NAME_rawValue;
          PROP_NAME_TO_ID.put(PROP_NAME_rawValue, PROP_ID_rawValue);
      
          PROP_ID_TO_NAME[PROP_ID_lookupValue] = PROP_NAME_lookupValue;
          PROP_NAME_TO_ID.put(PROP_NAME_lookupValue, PROP_ID_lookupValue);
      
          PROP_ID_TO_NAME[PROP_ID_rawScore] = PROP_NAME_rawScore;
          PROP_NAME_TO_ID.put(PROP_NAME_rawScore, PROP_ID_rawScore);
      
          PROP_ID_TO_NAME[PROP_ID_weightedScore] = PROP_NAME_weightedScore;
          PROP_NAME_TO_ID.put(PROP_NAME_weightedScore, PROP_ID_weightedScore);
      
          PROP_ID_TO_NAME[PROP_ID_sequence] = PROP_NAME_sequence;
          PROP_NAME_TO_ID.put(PROP_NAME_sequence, PROP_ID_sequence);
      
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
    
    /* 评分记录: SCORE_ID */
    private java.lang.Long _scoreId;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 对应准则行: CONFIG_LINE_ID */
    private java.lang.Long _configLineId;
    
    /* 准则编码(快照): CRITERION_CODE */
    private java.lang.String _criterionCode;
    
    /* 准则名称(快照): CRITERION_NAME */
    private java.lang.String _criterionName;
    
    /* 原始值: RAW_VALUE */
    private java.lang.String _rawValue;
    
    /* 匹配档次: LOOKUP_VALUE */
    private java.lang.String _lookupValue;
    
    /* 原始得分: RAW_SCORE */
    private java.lang.Integer _rawScore;
    
    /* 加权得分: WEIGHTED_SCORE */
    private java.lang.Integer _weightedScore;
    
    /* 排序: SEQUENCE */
    private java.lang.Integer _sequence;
    
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
    

    public _ErpCrmLeadScoreLine(){
        // for debug
    }

    protected ErpCrmLeadScoreLine newInstance(){
        ErpCrmLeadScoreLine entity = new ErpCrmLeadScoreLine();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpCrmLeadScoreLine cloneInstance() {
        ErpCrmLeadScoreLine entity = newInstance();
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
      return "app.erp.crm.dao.entity.ErpCrmLeadScoreLine";
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
        
            case PROP_ID_scoreId:
               return getScoreId();
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_configLineId:
               return getConfigLineId();
        
            case PROP_ID_criterionCode:
               return getCriterionCode();
        
            case PROP_ID_criterionName:
               return getCriterionName();
        
            case PROP_ID_rawValue:
               return getRawValue();
        
            case PROP_ID_lookupValue:
               return getLookupValue();
        
            case PROP_ID_rawScore:
               return getRawScore();
        
            case PROP_ID_weightedScore:
               return getWeightedScore();
        
            case PROP_ID_sequence:
               return getSequence();
        
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
        
            case PROP_ID_scoreId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_scoreId));
               }
               setScoreId(typedValue);
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
        
            case PROP_ID_configLineId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_configLineId));
               }
               setConfigLineId(typedValue);
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
        
            case PROP_ID_rawValue:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_rawValue));
               }
               setRawValue(typedValue);
               break;
            }
        
            case PROP_ID_lookupValue:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_lookupValue));
               }
               setLookupValue(typedValue);
               break;
            }
        
            case PROP_ID_rawScore:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_rawScore));
               }
               setRawScore(typedValue);
               break;
            }
        
            case PROP_ID_weightedScore:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_weightedScore));
               }
               setWeightedScore(typedValue);
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
        
            case PROP_ID_scoreId:{
               onInitProp(propId);
               this._scoreId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_configLineId:{
               onInitProp(propId);
               this._configLineId = (java.lang.Long)value;
               
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
        
            case PROP_ID_rawValue:{
               onInitProp(propId);
               this._rawValue = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_lookupValue:{
               onInitProp(propId);
               this._lookupValue = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_rawScore:{
               onInitProp(propId);
               this._rawScore = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_weightedScore:{
               onInitProp(propId);
               this._weightedScore = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_sequence:{
               onInitProp(propId);
               this._sequence = (java.lang.Integer)value;
               
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
     * 评分记录: SCORE_ID
     */
    public final java.lang.Long getScoreId(){
         onPropGet(PROP_ID_scoreId);
         return _scoreId;
    }

    /**
     * 评分记录: SCORE_ID
     */
    public final void setScoreId(java.lang.Long value){
        if(onPropSet(PROP_ID_scoreId,value)){
            this._scoreId = value;
            internalClearRefs(PROP_ID_scoreId);
            
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
     * 对应准则行: CONFIG_LINE_ID
     */
    public final java.lang.Long getConfigLineId(){
         onPropGet(PROP_ID_configLineId);
         return _configLineId;
    }

    /**
     * 对应准则行: CONFIG_LINE_ID
     */
    public final void setConfigLineId(java.lang.Long value){
        if(onPropSet(PROP_ID_configLineId,value)){
            this._configLineId = value;
            internalClearRefs(PROP_ID_configLineId);
            
        }
    }
    
    /**
     * 准则编码(快照): CRITERION_CODE
     */
    public final java.lang.String getCriterionCode(){
         onPropGet(PROP_ID_criterionCode);
         return _criterionCode;
    }

    /**
     * 准则编码(快照): CRITERION_CODE
     */
    public final void setCriterionCode(java.lang.String value){
        if(onPropSet(PROP_ID_criterionCode,value)){
            this._criterionCode = value;
            internalClearRefs(PROP_ID_criterionCode);
            
        }
    }
    
    /**
     * 准则名称(快照): CRITERION_NAME
     */
    public final java.lang.String getCriterionName(){
         onPropGet(PROP_ID_criterionName);
         return _criterionName;
    }

    /**
     * 准则名称(快照): CRITERION_NAME
     */
    public final void setCriterionName(java.lang.String value){
        if(onPropSet(PROP_ID_criterionName,value)){
            this._criterionName = value;
            internalClearRefs(PROP_ID_criterionName);
            
        }
    }
    
    /**
     * 原始值: RAW_VALUE
     */
    public final java.lang.String getRawValue(){
         onPropGet(PROP_ID_rawValue);
         return _rawValue;
    }

    /**
     * 原始值: RAW_VALUE
     */
    public final void setRawValue(java.lang.String value){
        if(onPropSet(PROP_ID_rawValue,value)){
            this._rawValue = value;
            internalClearRefs(PROP_ID_rawValue);
            
        }
    }
    
    /**
     * 匹配档次: LOOKUP_VALUE
     */
    public final java.lang.String getLookupValue(){
         onPropGet(PROP_ID_lookupValue);
         return _lookupValue;
    }

    /**
     * 匹配档次: LOOKUP_VALUE
     */
    public final void setLookupValue(java.lang.String value){
        if(onPropSet(PROP_ID_lookupValue,value)){
            this._lookupValue = value;
            internalClearRefs(PROP_ID_lookupValue);
            
        }
    }
    
    /**
     * 原始得分: RAW_SCORE
     */
    public final java.lang.Integer getRawScore(){
         onPropGet(PROP_ID_rawScore);
         return _rawScore;
    }

    /**
     * 原始得分: RAW_SCORE
     */
    public final void setRawScore(java.lang.Integer value){
        if(onPropSet(PROP_ID_rawScore,value)){
            this._rawScore = value;
            internalClearRefs(PROP_ID_rawScore);
            
        }
    }
    
    /**
     * 加权得分: WEIGHTED_SCORE
     */
    public final java.lang.Integer getWeightedScore(){
         onPropGet(PROP_ID_weightedScore);
         return _weightedScore;
    }

    /**
     * 加权得分: WEIGHTED_SCORE
     */
    public final void setWeightedScore(java.lang.Integer value){
        if(onPropSet(PROP_ID_weightedScore,value)){
            this._weightedScore = value;
            internalClearRefs(PROP_ID_weightedScore);
            
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
    public final app.erp.crm.dao.entity.ErpCrmLeadScore getScore(){
       return (app.erp.crm.dao.entity.ErpCrmLeadScore)internalGetRefEntity(PROP_NAME_score);
    }

    public final void setScore(app.erp.crm.dao.entity.ErpCrmLeadScore refEntity){
   
           if(refEntity == null){
           
                   this.setScoreId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_score, refEntity,()->{
           
                           this.setScoreId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.crm.dao.entity.ErpCrmLeadScoreConfigLine getConfigLine(){
       return (app.erp.crm.dao.entity.ErpCrmLeadScoreConfigLine)internalGetRefEntity(PROP_NAME_configLine);
    }

    public final void setConfigLine(app.erp.crm.dao.entity.ErpCrmLeadScoreConfigLine refEntity){
   
           if(refEntity == null){
           
                   this.setConfigLineId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_configLine, refEntity,()->{
           
                           this.setConfigLineId(refEntity.getId());
                       
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
