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

import app.erp.crm.dao.entity.ErpCrmForecastLine;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  预测明细: erp_crm_forecast_line
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpCrmForecastLine extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 预测数据: FORECAST_ID BIGINT */
    public static final String PROP_NAME_forecastId = "forecastId";
    public static final int PROP_ID_forecastId = 2;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 商机: LEAD_ID BIGINT */
    public static final String PROP_NAME_leadId = "leadId";
    public static final int PROP_ID_leadId = 4;
    
    /* 概率快照: PROBABILITY INTEGER */
    public static final String PROP_NAME_probability = "probability";
    public static final int PROP_ID_probability = 5;
    
    /* 预期收入快照: EXPECTED_REVENUE DECIMAL */
    public static final String PROP_NAME_expectedRevenue = "expectedRevenue";
    public static final int PROP_ID_expectedRevenue = 6;
    
    /* 加权收入: WEIGHTED_REVENUE DECIMAL */
    public static final String PROP_NAME_weightedRevenue = "weightedRevenue";
    public static final int PROP_ID_weightedRevenue = 7;
    
    /* 预测分类: FORECAST_CATEGORY VARCHAR */
    public static final String PROP_NAME_forecastCategory = "forecastCategory";
    public static final int PROP_ID_forecastCategory = 8;
    
    /* 是否计入承诺: INCLUDED_IN_COMMIT BOOLEAN */
    public static final String PROP_NAME_includedInCommit = "includedInCommit";
    public static final int PROP_ID_includedInCommit = 9;
    
    /* 阶段名快照: STAGE_NAME VARCHAR */
    public static final String PROP_NAME_stageName = "stageName";
    public static final int PROP_ID_stageName = 10;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 11;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 12;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 13;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 14;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 15;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 16;
    

    private static int _PROP_ID_BOUND = 17;

    
    /* relation:  */
    public static final String PROP_NAME_forecast = "forecast";
    
    /* relation:  */
    public static final String PROP_NAME_lead = "lead";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[17];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_forecastId] = PROP_NAME_forecastId;
          PROP_NAME_TO_ID.put(PROP_NAME_forecastId, PROP_ID_forecastId);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_leadId] = PROP_NAME_leadId;
          PROP_NAME_TO_ID.put(PROP_NAME_leadId, PROP_ID_leadId);
      
          PROP_ID_TO_NAME[PROP_ID_probability] = PROP_NAME_probability;
          PROP_NAME_TO_ID.put(PROP_NAME_probability, PROP_ID_probability);
      
          PROP_ID_TO_NAME[PROP_ID_expectedRevenue] = PROP_NAME_expectedRevenue;
          PROP_NAME_TO_ID.put(PROP_NAME_expectedRevenue, PROP_ID_expectedRevenue);
      
          PROP_ID_TO_NAME[PROP_ID_weightedRevenue] = PROP_NAME_weightedRevenue;
          PROP_NAME_TO_ID.put(PROP_NAME_weightedRevenue, PROP_ID_weightedRevenue);
      
          PROP_ID_TO_NAME[PROP_ID_forecastCategory] = PROP_NAME_forecastCategory;
          PROP_NAME_TO_ID.put(PROP_NAME_forecastCategory, PROP_ID_forecastCategory);
      
          PROP_ID_TO_NAME[PROP_ID_includedInCommit] = PROP_NAME_includedInCommit;
          PROP_NAME_TO_ID.put(PROP_NAME_includedInCommit, PROP_ID_includedInCommit);
      
          PROP_ID_TO_NAME[PROP_ID_stageName] = PROP_NAME_stageName;
          PROP_NAME_TO_ID.put(PROP_NAME_stageName, PROP_ID_stageName);
      
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
    
    /* 预测数据: FORECAST_ID */
    private java.lang.Long _forecastId;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 商机: LEAD_ID */
    private java.lang.Long _leadId;
    
    /* 概率快照: PROBABILITY */
    private java.lang.Integer _probability;
    
    /* 预期收入快照: EXPECTED_REVENUE */
    private java.math.BigDecimal _expectedRevenue;
    
    /* 加权收入: WEIGHTED_REVENUE */
    private java.math.BigDecimal _weightedRevenue;
    
    /* 预测分类: FORECAST_CATEGORY */
    private java.lang.String _forecastCategory;
    
    /* 是否计入承诺: INCLUDED_IN_COMMIT */
    private java.lang.Boolean _includedInCommit;
    
    /* 阶段名快照: STAGE_NAME */
    private java.lang.String _stageName;
    
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
    

    public _ErpCrmForecastLine(){
        // for debug
    }

    protected ErpCrmForecastLine newInstance(){
        ErpCrmForecastLine entity = new ErpCrmForecastLine();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpCrmForecastLine cloneInstance() {
        ErpCrmForecastLine entity = newInstance();
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
      return "app.erp.crm.dao.entity.ErpCrmForecastLine";
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
        
            case PROP_ID_forecastId:
               return getForecastId();
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_leadId:
               return getLeadId();
        
            case PROP_ID_probability:
               return getProbability();
        
            case PROP_ID_expectedRevenue:
               return getExpectedRevenue();
        
            case PROP_ID_weightedRevenue:
               return getWeightedRevenue();
        
            case PROP_ID_forecastCategory:
               return getForecastCategory();
        
            case PROP_ID_includedInCommit:
               return getIncludedInCommit();
        
            case PROP_ID_stageName:
               return getStageName();
        
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
        
            case PROP_ID_forecastId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_forecastId));
               }
               setForecastId(typedValue);
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
        
            case PROP_ID_leadId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_leadId));
               }
               setLeadId(typedValue);
               break;
            }
        
            case PROP_ID_probability:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_probability));
               }
               setProbability(typedValue);
               break;
            }
        
            case PROP_ID_expectedRevenue:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_expectedRevenue));
               }
               setExpectedRevenue(typedValue);
               break;
            }
        
            case PROP_ID_weightedRevenue:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_weightedRevenue));
               }
               setWeightedRevenue(typedValue);
               break;
            }
        
            case PROP_ID_forecastCategory:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_forecastCategory));
               }
               setForecastCategory(typedValue);
               break;
            }
        
            case PROP_ID_includedInCommit:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_includedInCommit));
               }
               setIncludedInCommit(typedValue);
               break;
            }
        
            case PROP_ID_stageName:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_stageName));
               }
               setStageName(typedValue);
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
        
            case PROP_ID_forecastId:{
               onInitProp(propId);
               this._forecastId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_leadId:{
               onInitProp(propId);
               this._leadId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_probability:{
               onInitProp(propId);
               this._probability = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_expectedRevenue:{
               onInitProp(propId);
               this._expectedRevenue = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_weightedRevenue:{
               onInitProp(propId);
               this._weightedRevenue = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_forecastCategory:{
               onInitProp(propId);
               this._forecastCategory = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_includedInCommit:{
               onInitProp(propId);
               this._includedInCommit = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_stageName:{
               onInitProp(propId);
               this._stageName = (java.lang.String)value;
               
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
     * 预测数据: FORECAST_ID
     */
    public final java.lang.Long getForecastId(){
         onPropGet(PROP_ID_forecastId);
         return _forecastId;
    }

    /**
     * 预测数据: FORECAST_ID
     */
    public final void setForecastId(java.lang.Long value){
        if(onPropSet(PROP_ID_forecastId,value)){
            this._forecastId = value;
            internalClearRefs(PROP_ID_forecastId);
            
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
     * 商机: LEAD_ID
     */
    public final java.lang.Long getLeadId(){
         onPropGet(PROP_ID_leadId);
         return _leadId;
    }

    /**
     * 商机: LEAD_ID
     */
    public final void setLeadId(java.lang.Long value){
        if(onPropSet(PROP_ID_leadId,value)){
            this._leadId = value;
            internalClearRefs(PROP_ID_leadId);
            
        }
    }
    
    /**
     * 概率快照: PROBABILITY
     */
    public final java.lang.Integer getProbability(){
         onPropGet(PROP_ID_probability);
         return _probability;
    }

    /**
     * 概率快照: PROBABILITY
     */
    public final void setProbability(java.lang.Integer value){
        if(onPropSet(PROP_ID_probability,value)){
            this._probability = value;
            internalClearRefs(PROP_ID_probability);
            
        }
    }
    
    /**
     * 预期收入快照: EXPECTED_REVENUE
     */
    public final java.math.BigDecimal getExpectedRevenue(){
         onPropGet(PROP_ID_expectedRevenue);
         return _expectedRevenue;
    }

    /**
     * 预期收入快照: EXPECTED_REVENUE
     */
    public final void setExpectedRevenue(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_expectedRevenue,value)){
            this._expectedRevenue = value;
            internalClearRefs(PROP_ID_expectedRevenue);
            
        }
    }
    
    /**
     * 加权收入: WEIGHTED_REVENUE
     */
    public final java.math.BigDecimal getWeightedRevenue(){
         onPropGet(PROP_ID_weightedRevenue);
         return _weightedRevenue;
    }

    /**
     * 加权收入: WEIGHTED_REVENUE
     */
    public final void setWeightedRevenue(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_weightedRevenue,value)){
            this._weightedRevenue = value;
            internalClearRefs(PROP_ID_weightedRevenue);
            
        }
    }
    
    /**
     * 预测分类: FORECAST_CATEGORY
     */
    public final java.lang.String getForecastCategory(){
         onPropGet(PROP_ID_forecastCategory);
         return _forecastCategory;
    }

    /**
     * 预测分类: FORECAST_CATEGORY
     */
    public final void setForecastCategory(java.lang.String value){
        if(onPropSet(PROP_ID_forecastCategory,value)){
            this._forecastCategory = value;
            internalClearRefs(PROP_ID_forecastCategory);
            
        }
    }
    
    /**
     * 是否计入承诺: INCLUDED_IN_COMMIT
     */
    public final java.lang.Boolean getIncludedInCommit(){
         onPropGet(PROP_ID_includedInCommit);
         return _includedInCommit;
    }

    /**
     * 是否计入承诺: INCLUDED_IN_COMMIT
     */
    public final void setIncludedInCommit(java.lang.Boolean value){
        if(onPropSet(PROP_ID_includedInCommit,value)){
            this._includedInCommit = value;
            internalClearRefs(PROP_ID_includedInCommit);
            
        }
    }
    
    /**
     * 阶段名快照: STAGE_NAME
     */
    public final java.lang.String getStageName(){
         onPropGet(PROP_ID_stageName);
         return _stageName;
    }

    /**
     * 阶段名快照: STAGE_NAME
     */
    public final void setStageName(java.lang.String value){
        if(onPropSet(PROP_ID_stageName,value)){
            this._stageName = value;
            internalClearRefs(PROP_ID_stageName);
            
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
    public final app.erp.crm.dao.entity.ErpCrmForecast getForecast(){
       return (app.erp.crm.dao.entity.ErpCrmForecast)internalGetRefEntity(PROP_NAME_forecast);
    }

    public final void setForecast(app.erp.crm.dao.entity.ErpCrmForecast refEntity){
   
           if(refEntity == null){
           
                   this.setForecastId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_forecast, refEntity,()->{
           
                           this.setForecastId(refEntity.getId());
                       
           });
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
