package app.erp.pur.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.pur.dao.entity.ErpPurSupplierScorecardCriteria;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  评分维度: erp_pur_supplier_scorecard_criteria
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpPurSupplierScorecardCriteria extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 评分卡: SCORECARD_ID BIGINT */
    public static final String PROP_NAME_scorecardId = "scorecardId";
    public static final int PROP_ID_scorecardId = 2;
    
    /* 维度名: CRITERIA_NAME VARCHAR */
    public static final String PROP_NAME_criteriaName = "criteriaName";
    public static final int PROP_ID_criteriaName = 3;
    
    /* 权重(0-100): WEIGHT DECIMAL */
    public static final String PROP_NAME_weight = "weight";
    public static final int PROP_ID_weight = 4;
    
    /* 公式(XLang表达式): FORMULA VARCHAR */
    public static final String PROP_NAME_formula = "formula";
    public static final int PROP_ID_formula = 5;
    
    /* 维度得分: SCORE DECIMAL */
    public static final String PROP_NAME_score = "score";
    public static final int PROP_ID_score = 6;
    
    /* 加权得分: WEIGHTED_SCORE DECIMAL */
    public static final String PROP_NAME_weightedScore = "weightedScore";
    public static final int PROP_ID_weightedScore = 7;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 8;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 9;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 10;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 11;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 12;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 13;
    

    private static int _PROP_ID_BOUND = 14;

    
    /* relation:  */
    public static final String PROP_NAME_scorecard = "scorecard";
    
    /* relation:  */
    public static final String PROP_NAME_variables = "variables";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[14];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_scorecardId] = PROP_NAME_scorecardId;
          PROP_NAME_TO_ID.put(PROP_NAME_scorecardId, PROP_ID_scorecardId);
      
          PROP_ID_TO_NAME[PROP_ID_criteriaName] = PROP_NAME_criteriaName;
          PROP_NAME_TO_ID.put(PROP_NAME_criteriaName, PROP_ID_criteriaName);
      
          PROP_ID_TO_NAME[PROP_ID_weight] = PROP_NAME_weight;
          PROP_NAME_TO_ID.put(PROP_NAME_weight, PROP_ID_weight);
      
          PROP_ID_TO_NAME[PROP_ID_formula] = PROP_NAME_formula;
          PROP_NAME_TO_ID.put(PROP_NAME_formula, PROP_ID_formula);
      
          PROP_ID_TO_NAME[PROP_ID_score] = PROP_NAME_score;
          PROP_NAME_TO_ID.put(PROP_NAME_score, PROP_ID_score);
      
          PROP_ID_TO_NAME[PROP_ID_weightedScore] = PROP_NAME_weightedScore;
          PROP_NAME_TO_ID.put(PROP_NAME_weightedScore, PROP_ID_weightedScore);
      
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
    
    /* 评分卡: SCORECARD_ID */
    private java.lang.Long _scorecardId;
    
    /* 维度名: CRITERIA_NAME */
    private java.lang.String _criteriaName;
    
    /* 权重(0-100): WEIGHT */
    private java.math.BigDecimal _weight;
    
    /* 公式(XLang表达式): FORMULA */
    private java.lang.String _formula;
    
    /* 维度得分: SCORE */
    private java.math.BigDecimal _score;
    
    /* 加权得分: WEIGHTED_SCORE */
    private java.math.BigDecimal _weightedScore;
    
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
    

    public _ErpPurSupplierScorecardCriteria(){
        // for debug
    }

    protected ErpPurSupplierScorecardCriteria newInstance(){
        ErpPurSupplierScorecardCriteria entity = new ErpPurSupplierScorecardCriteria();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpPurSupplierScorecardCriteria cloneInstance() {
        ErpPurSupplierScorecardCriteria entity = newInstance();
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
      return "app.erp.pur.dao.entity.ErpPurSupplierScorecardCriteria";
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
        
            case PROP_ID_scorecardId:
               return getScorecardId();
        
            case PROP_ID_criteriaName:
               return getCriteriaName();
        
            case PROP_ID_weight:
               return getWeight();
        
            case PROP_ID_formula:
               return getFormula();
        
            case PROP_ID_score:
               return getScore();
        
            case PROP_ID_weightedScore:
               return getWeightedScore();
        
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
        
            case PROP_ID_scorecardId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_scorecardId));
               }
               setScorecardId(typedValue);
               break;
            }
        
            case PROP_ID_criteriaName:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_criteriaName));
               }
               setCriteriaName(typedValue);
               break;
            }
        
            case PROP_ID_weight:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_weight));
               }
               setWeight(typedValue);
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
        
            case PROP_ID_score:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_score));
               }
               setScore(typedValue);
               break;
            }
        
            case PROP_ID_weightedScore:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_weightedScore));
               }
               setWeightedScore(typedValue);
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
        
            case PROP_ID_scorecardId:{
               onInitProp(propId);
               this._scorecardId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_criteriaName:{
               onInitProp(propId);
               this._criteriaName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_weight:{
               onInitProp(propId);
               this._weight = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_formula:{
               onInitProp(propId);
               this._formula = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_score:{
               onInitProp(propId);
               this._score = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_weightedScore:{
               onInitProp(propId);
               this._weightedScore = (java.math.BigDecimal)value;
               
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
     * 评分卡: SCORECARD_ID
     */
    public final java.lang.Long getScorecardId(){
         onPropGet(PROP_ID_scorecardId);
         return _scorecardId;
    }

    /**
     * 评分卡: SCORECARD_ID
     */
    public final void setScorecardId(java.lang.Long value){
        if(onPropSet(PROP_ID_scorecardId,value)){
            this._scorecardId = value;
            internalClearRefs(PROP_ID_scorecardId);
            
        }
    }
    
    /**
     * 维度名: CRITERIA_NAME
     */
    public final java.lang.String getCriteriaName(){
         onPropGet(PROP_ID_criteriaName);
         return _criteriaName;
    }

    /**
     * 维度名: CRITERIA_NAME
     */
    public final void setCriteriaName(java.lang.String value){
        if(onPropSet(PROP_ID_criteriaName,value)){
            this._criteriaName = value;
            internalClearRefs(PROP_ID_criteriaName);
            
        }
    }
    
    /**
     * 权重(0-100): WEIGHT
     */
    public final java.math.BigDecimal getWeight(){
         onPropGet(PROP_ID_weight);
         return _weight;
    }

    /**
     * 权重(0-100): WEIGHT
     */
    public final void setWeight(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_weight,value)){
            this._weight = value;
            internalClearRefs(PROP_ID_weight);
            
        }
    }
    
    /**
     * 公式(XLang表达式): FORMULA
     */
    public final java.lang.String getFormula(){
         onPropGet(PROP_ID_formula);
         return _formula;
    }

    /**
     * 公式(XLang表达式): FORMULA
     */
    public final void setFormula(java.lang.String value){
        if(onPropSet(PROP_ID_formula,value)){
            this._formula = value;
            internalClearRefs(PROP_ID_formula);
            
        }
    }
    
    /**
     * 维度得分: SCORE
     */
    public final java.math.BigDecimal getScore(){
         onPropGet(PROP_ID_score);
         return _score;
    }

    /**
     * 维度得分: SCORE
     */
    public final void setScore(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_score,value)){
            this._score = value;
            internalClearRefs(PROP_ID_score);
            
        }
    }
    
    /**
     * 加权得分: WEIGHTED_SCORE
     */
    public final java.math.BigDecimal getWeightedScore(){
         onPropGet(PROP_ID_weightedScore);
         return _weightedScore;
    }

    /**
     * 加权得分: WEIGHTED_SCORE
     */
    public final void setWeightedScore(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_weightedScore,value)){
            this._weightedScore = value;
            internalClearRefs(PROP_ID_weightedScore);
            
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
    public final app.erp.pur.dao.entity.ErpPurSupplierScorecard getScorecard(){
       return (app.erp.pur.dao.entity.ErpPurSupplierScorecard)internalGetRefEntity(PROP_NAME_scorecard);
    }

    public final void setScorecard(app.erp.pur.dao.entity.ErpPurSupplierScorecard refEntity){
   
           if(refEntity == null){
           
                   this.setScorecardId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_scorecard, refEntity,()->{
           
                           this.setScorecardId(refEntity.getId());
                       
           });
           }
       
    }
       
    private final OrmEntitySet<app.erp.pur.dao.entity.ErpPurSupplierScorecardVariable> _variables = new OrmEntitySet<>(this, PROP_NAME_variables,
        null, null,app.erp.pur.dao.entity.ErpPurSupplierScorecardVariable.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.pur.dao.entity.ErpPurSupplierScorecardVariable> getVariables(){
       return _variables;
    }
       
}
// resume CPD analysis - CPD-ON
