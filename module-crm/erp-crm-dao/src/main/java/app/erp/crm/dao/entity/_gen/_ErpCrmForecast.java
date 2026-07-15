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

import app.erp.crm.dao.entity.ErpCrmForecast;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  预测数据: erp_crm_forecast
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpCrmForecast extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 2;
    
    /* 预测期间: PERIOD_ID BIGINT */
    public static final String PROP_NAME_periodId = "periodId";
    public static final int PROP_ID_periodId = 3;
    
    /* 销售区域: TERRITORY_ID BIGINT */
    public static final String PROP_NAME_territoryId = "territoryId";
    public static final int PROP_ID_territoryId = 4;
    
    /* 销售团队: TEAM_ID BIGINT */
    public static final String PROP_NAME_teamId = "teamId";
    public static final int PROP_ID_teamId = 5;
    
    /* 销售员: OWNER_ID VARCHAR */
    public static final String PROP_NAME_ownerId = "ownerId";
    public static final int PROP_ID_ownerId = 6;
    
    /* 币种: CURRENCY_ID BIGINT */
    public static final String PROP_NAME_currencyId = "currencyId";
    public static final int PROP_ID_currencyId = 7;
    
    /* 承诺金额: COMMIT_AMOUNT DECIMAL */
    public static final String PROP_NAME_commitAmount = "commitAmount";
    public static final int PROP_ID_commitAmount = 8;
    
    /* 乐观金额: UPSIDE_AMOUNT DECIMAL */
    public static final String PROP_NAME_upsideAmount = "upsideAmount";
    public static final int PROP_ID_upsideAmount = 9;
    
    /* 加权金额: WEIGHTED_AMOUNT DECIMAL */
    public static final String PROP_NAME_weightedAmount = "weightedAmount";
    public static final int PROP_ID_weightedAmount = 10;
    
    /* 最佳金额: BEST_CASE_AMOUNT DECIMAL */
    public static final String PROP_NAME_bestCaseAmount = "bestCaseAmount";
    public static final int PROP_ID_bestCaseAmount = 11;
    
    /* 商机总数: OPPORTUNITY_COUNT INTEGER */
    public static final String PROP_NAME_opportunityCount = "opportunityCount";
    public static final int PROP_ID_opportunityCount = 12;
    
    /* 承诺商机数: COMMIT_OPPORTUNITY_COUNT INTEGER */
    public static final String PROP_NAME_commitOpportunityCount = "commitOpportunityCount";
    public static final int PROP_ID_commitOpportunityCount = 13;
    
    /* 实际已关闭收入: EXPECTED_CLOSED_REVENUE DECIMAL */
    public static final String PROP_NAME_expectedClosedRevenue = "expectedClosedRevenue";
    public static final int PROP_ID_expectedClosedRevenue = 14;
    
    /* 最近计算时间: LAST_CALCULATED_AT TIMESTAMP */
    public static final String PROP_NAME_lastCalculatedAt = "lastCalculatedAt";
    public static final int PROP_ID_lastCalculatedAt = 15;
    
    /* 备注: NOTES VARCHAR */
    public static final String PROP_NAME_notes = "notes";
    public static final int PROP_ID_notes = 16;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 17;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 18;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 19;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 20;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 21;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 22;
    

    private static int _PROP_ID_BOUND = 23;

    
    /* relation:  */
    public static final String PROP_NAME_period = "period";
    
    /* relation:  */
    public static final String PROP_NAME_territory = "territory";
    
    /* relation:  */
    public static final String PROP_NAME_team = "team";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    
    /* relation:  */
    public static final String PROP_NAME_currency = "currency";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[23];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_periodId] = PROP_NAME_periodId;
          PROP_NAME_TO_ID.put(PROP_NAME_periodId, PROP_ID_periodId);
      
          PROP_ID_TO_NAME[PROP_ID_territoryId] = PROP_NAME_territoryId;
          PROP_NAME_TO_ID.put(PROP_NAME_territoryId, PROP_ID_territoryId);
      
          PROP_ID_TO_NAME[PROP_ID_teamId] = PROP_NAME_teamId;
          PROP_NAME_TO_ID.put(PROP_NAME_teamId, PROP_ID_teamId);
      
          PROP_ID_TO_NAME[PROP_ID_ownerId] = PROP_NAME_ownerId;
          PROP_NAME_TO_ID.put(PROP_NAME_ownerId, PROP_ID_ownerId);
      
          PROP_ID_TO_NAME[PROP_ID_currencyId] = PROP_NAME_currencyId;
          PROP_NAME_TO_ID.put(PROP_NAME_currencyId, PROP_ID_currencyId);
      
          PROP_ID_TO_NAME[PROP_ID_commitAmount] = PROP_NAME_commitAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_commitAmount, PROP_ID_commitAmount);
      
          PROP_ID_TO_NAME[PROP_ID_upsideAmount] = PROP_NAME_upsideAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_upsideAmount, PROP_ID_upsideAmount);
      
          PROP_ID_TO_NAME[PROP_ID_weightedAmount] = PROP_NAME_weightedAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_weightedAmount, PROP_ID_weightedAmount);
      
          PROP_ID_TO_NAME[PROP_ID_bestCaseAmount] = PROP_NAME_bestCaseAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_bestCaseAmount, PROP_ID_bestCaseAmount);
      
          PROP_ID_TO_NAME[PROP_ID_opportunityCount] = PROP_NAME_opportunityCount;
          PROP_NAME_TO_ID.put(PROP_NAME_opportunityCount, PROP_ID_opportunityCount);
      
          PROP_ID_TO_NAME[PROP_ID_commitOpportunityCount] = PROP_NAME_commitOpportunityCount;
          PROP_NAME_TO_ID.put(PROP_NAME_commitOpportunityCount, PROP_ID_commitOpportunityCount);
      
          PROP_ID_TO_NAME[PROP_ID_expectedClosedRevenue] = PROP_NAME_expectedClosedRevenue;
          PROP_NAME_TO_ID.put(PROP_NAME_expectedClosedRevenue, PROP_ID_expectedClosedRevenue);
      
          PROP_ID_TO_NAME[PROP_ID_lastCalculatedAt] = PROP_NAME_lastCalculatedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_lastCalculatedAt, PROP_ID_lastCalculatedAt);
      
          PROP_ID_TO_NAME[PROP_ID_notes] = PROP_NAME_notes;
          PROP_NAME_TO_ID.put(PROP_NAME_notes, PROP_ID_notes);
      
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
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 预测期间: PERIOD_ID */
    private java.lang.Long _periodId;
    
    /* 销售区域: TERRITORY_ID */
    private java.lang.Long _territoryId;
    
    /* 销售团队: TEAM_ID */
    private java.lang.Long _teamId;
    
    /* 销售员: OWNER_ID */
    private java.lang.String _ownerId;
    
    /* 币种: CURRENCY_ID */
    private java.lang.Long _currencyId;
    
    /* 承诺金额: COMMIT_AMOUNT */
    private java.math.BigDecimal _commitAmount;
    
    /* 乐观金额: UPSIDE_AMOUNT */
    private java.math.BigDecimal _upsideAmount;
    
    /* 加权金额: WEIGHTED_AMOUNT */
    private java.math.BigDecimal _weightedAmount;
    
    /* 最佳金额: BEST_CASE_AMOUNT */
    private java.math.BigDecimal _bestCaseAmount;
    
    /* 商机总数: OPPORTUNITY_COUNT */
    private java.lang.Integer _opportunityCount;
    
    /* 承诺商机数: COMMIT_OPPORTUNITY_COUNT */
    private java.lang.Integer _commitOpportunityCount;
    
    /* 实际已关闭收入: EXPECTED_CLOSED_REVENUE */
    private java.math.BigDecimal _expectedClosedRevenue;
    
    /* 最近计算时间: LAST_CALCULATED_AT */
    private java.sql.Timestamp _lastCalculatedAt;
    
    /* 备注: NOTES */
    private java.lang.String _notes;
    
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
    

    public _ErpCrmForecast(){
        // for debug
    }

    protected ErpCrmForecast newInstance(){
        ErpCrmForecast entity = new ErpCrmForecast();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpCrmForecast cloneInstance() {
        ErpCrmForecast entity = newInstance();
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
      return "app.erp.crm.dao.entity.ErpCrmForecast";
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
        
            case PROP_ID_periodId:
               return getPeriodId();
        
            case PROP_ID_territoryId:
               return getTerritoryId();
        
            case PROP_ID_teamId:
               return getTeamId();
        
            case PROP_ID_ownerId:
               return getOwnerId();
        
            case PROP_ID_currencyId:
               return getCurrencyId();
        
            case PROP_ID_commitAmount:
               return getCommitAmount();
        
            case PROP_ID_upsideAmount:
               return getUpsideAmount();
        
            case PROP_ID_weightedAmount:
               return getWeightedAmount();
        
            case PROP_ID_bestCaseAmount:
               return getBestCaseAmount();
        
            case PROP_ID_opportunityCount:
               return getOpportunityCount();
        
            case PROP_ID_commitOpportunityCount:
               return getCommitOpportunityCount();
        
            case PROP_ID_expectedClosedRevenue:
               return getExpectedClosedRevenue();
        
            case PROP_ID_lastCalculatedAt:
               return getLastCalculatedAt();
        
            case PROP_ID_notes:
               return getNotes();
        
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
        
            case PROP_ID_periodId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_periodId));
               }
               setPeriodId(typedValue);
               break;
            }
        
            case PROP_ID_territoryId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_territoryId));
               }
               setTerritoryId(typedValue);
               break;
            }
        
            case PROP_ID_teamId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_teamId));
               }
               setTeamId(typedValue);
               break;
            }
        
            case PROP_ID_ownerId:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_ownerId));
               }
               setOwnerId(typedValue);
               break;
            }
        
            case PROP_ID_currencyId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_currencyId));
               }
               setCurrencyId(typedValue);
               break;
            }
        
            case PROP_ID_commitAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_commitAmount));
               }
               setCommitAmount(typedValue);
               break;
            }
        
            case PROP_ID_upsideAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_upsideAmount));
               }
               setUpsideAmount(typedValue);
               break;
            }
        
            case PROP_ID_weightedAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_weightedAmount));
               }
               setWeightedAmount(typedValue);
               break;
            }
        
            case PROP_ID_bestCaseAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_bestCaseAmount));
               }
               setBestCaseAmount(typedValue);
               break;
            }
        
            case PROP_ID_opportunityCount:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_opportunityCount));
               }
               setOpportunityCount(typedValue);
               break;
            }
        
            case PROP_ID_commitOpportunityCount:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_commitOpportunityCount));
               }
               setCommitOpportunityCount(typedValue);
               break;
            }
        
            case PROP_ID_expectedClosedRevenue:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_expectedClosedRevenue));
               }
               setExpectedClosedRevenue(typedValue);
               break;
            }
        
            case PROP_ID_lastCalculatedAt:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_lastCalculatedAt));
               }
               setLastCalculatedAt(typedValue);
               break;
            }
        
            case PROP_ID_notes:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_notes));
               }
               setNotes(typedValue);
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
        
            case PROP_ID_periodId:{
               onInitProp(propId);
               this._periodId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_territoryId:{
               onInitProp(propId);
               this._territoryId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_teamId:{
               onInitProp(propId);
               this._teamId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_ownerId:{
               onInitProp(propId);
               this._ownerId = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_currencyId:{
               onInitProp(propId);
               this._currencyId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_commitAmount:{
               onInitProp(propId);
               this._commitAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_upsideAmount:{
               onInitProp(propId);
               this._upsideAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_weightedAmount:{
               onInitProp(propId);
               this._weightedAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_bestCaseAmount:{
               onInitProp(propId);
               this._bestCaseAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_opportunityCount:{
               onInitProp(propId);
               this._opportunityCount = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_commitOpportunityCount:{
               onInitProp(propId);
               this._commitOpportunityCount = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_expectedClosedRevenue:{
               onInitProp(propId);
               this._expectedClosedRevenue = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_lastCalculatedAt:{
               onInitProp(propId);
               this._lastCalculatedAt = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_notes:{
               onInitProp(propId);
               this._notes = (java.lang.String)value;
               
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
     * 预测期间: PERIOD_ID
     */
    public final java.lang.Long getPeriodId(){
         onPropGet(PROP_ID_periodId);
         return _periodId;
    }

    /**
     * 预测期间: PERIOD_ID
     */
    public final void setPeriodId(java.lang.Long value){
        if(onPropSet(PROP_ID_periodId,value)){
            this._periodId = value;
            internalClearRefs(PROP_ID_periodId);
            
        }
    }
    
    /**
     * 销售区域: TERRITORY_ID
     */
    public final java.lang.Long getTerritoryId(){
         onPropGet(PROP_ID_territoryId);
         return _territoryId;
    }

    /**
     * 销售区域: TERRITORY_ID
     */
    public final void setTerritoryId(java.lang.Long value){
        if(onPropSet(PROP_ID_territoryId,value)){
            this._territoryId = value;
            internalClearRefs(PROP_ID_territoryId);
            
        }
    }
    
    /**
     * 销售团队: TEAM_ID
     */
    public final java.lang.Long getTeamId(){
         onPropGet(PROP_ID_teamId);
         return _teamId;
    }

    /**
     * 销售团队: TEAM_ID
     */
    public final void setTeamId(java.lang.Long value){
        if(onPropSet(PROP_ID_teamId,value)){
            this._teamId = value;
            internalClearRefs(PROP_ID_teamId);
            
        }
    }
    
    /**
     * 销售员: OWNER_ID
     */
    public final java.lang.String getOwnerId(){
         onPropGet(PROP_ID_ownerId);
         return _ownerId;
    }

    /**
     * 销售员: OWNER_ID
     */
    public final void setOwnerId(java.lang.String value){
        if(onPropSet(PROP_ID_ownerId,value)){
            this._ownerId = value;
            internalClearRefs(PROP_ID_ownerId);
            
        }
    }
    
    /**
     * 币种: CURRENCY_ID
     */
    public final java.lang.Long getCurrencyId(){
         onPropGet(PROP_ID_currencyId);
         return _currencyId;
    }

    /**
     * 币种: CURRENCY_ID
     */
    public final void setCurrencyId(java.lang.Long value){
        if(onPropSet(PROP_ID_currencyId,value)){
            this._currencyId = value;
            internalClearRefs(PROP_ID_currencyId);
            
        }
    }
    
    /**
     * 承诺金额: COMMIT_AMOUNT
     */
    public final java.math.BigDecimal getCommitAmount(){
         onPropGet(PROP_ID_commitAmount);
         return _commitAmount;
    }

    /**
     * 承诺金额: COMMIT_AMOUNT
     */
    public final void setCommitAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_commitAmount,value)){
            this._commitAmount = value;
            internalClearRefs(PROP_ID_commitAmount);
            
        }
    }
    
    /**
     * 乐观金额: UPSIDE_AMOUNT
     */
    public final java.math.BigDecimal getUpsideAmount(){
         onPropGet(PROP_ID_upsideAmount);
         return _upsideAmount;
    }

    /**
     * 乐观金额: UPSIDE_AMOUNT
     */
    public final void setUpsideAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_upsideAmount,value)){
            this._upsideAmount = value;
            internalClearRefs(PROP_ID_upsideAmount);
            
        }
    }
    
    /**
     * 加权金额: WEIGHTED_AMOUNT
     */
    public final java.math.BigDecimal getWeightedAmount(){
         onPropGet(PROP_ID_weightedAmount);
         return _weightedAmount;
    }

    /**
     * 加权金额: WEIGHTED_AMOUNT
     */
    public final void setWeightedAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_weightedAmount,value)){
            this._weightedAmount = value;
            internalClearRefs(PROP_ID_weightedAmount);
            
        }
    }
    
    /**
     * 最佳金额: BEST_CASE_AMOUNT
     */
    public final java.math.BigDecimal getBestCaseAmount(){
         onPropGet(PROP_ID_bestCaseAmount);
         return _bestCaseAmount;
    }

    /**
     * 最佳金额: BEST_CASE_AMOUNT
     */
    public final void setBestCaseAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_bestCaseAmount,value)){
            this._bestCaseAmount = value;
            internalClearRefs(PROP_ID_bestCaseAmount);
            
        }
    }
    
    /**
     * 商机总数: OPPORTUNITY_COUNT
     */
    public final java.lang.Integer getOpportunityCount(){
         onPropGet(PROP_ID_opportunityCount);
         return _opportunityCount;
    }

    /**
     * 商机总数: OPPORTUNITY_COUNT
     */
    public final void setOpportunityCount(java.lang.Integer value){
        if(onPropSet(PROP_ID_opportunityCount,value)){
            this._opportunityCount = value;
            internalClearRefs(PROP_ID_opportunityCount);
            
        }
    }
    
    /**
     * 承诺商机数: COMMIT_OPPORTUNITY_COUNT
     */
    public final java.lang.Integer getCommitOpportunityCount(){
         onPropGet(PROP_ID_commitOpportunityCount);
         return _commitOpportunityCount;
    }

    /**
     * 承诺商机数: COMMIT_OPPORTUNITY_COUNT
     */
    public final void setCommitOpportunityCount(java.lang.Integer value){
        if(onPropSet(PROP_ID_commitOpportunityCount,value)){
            this._commitOpportunityCount = value;
            internalClearRefs(PROP_ID_commitOpportunityCount);
            
        }
    }
    
    /**
     * 实际已关闭收入: EXPECTED_CLOSED_REVENUE
     */
    public final java.math.BigDecimal getExpectedClosedRevenue(){
         onPropGet(PROP_ID_expectedClosedRevenue);
         return _expectedClosedRevenue;
    }

    /**
     * 实际已关闭收入: EXPECTED_CLOSED_REVENUE
     */
    public final void setExpectedClosedRevenue(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_expectedClosedRevenue,value)){
            this._expectedClosedRevenue = value;
            internalClearRefs(PROP_ID_expectedClosedRevenue);
            
        }
    }
    
    /**
     * 最近计算时间: LAST_CALCULATED_AT
     */
    public final java.sql.Timestamp getLastCalculatedAt(){
         onPropGet(PROP_ID_lastCalculatedAt);
         return _lastCalculatedAt;
    }

    /**
     * 最近计算时间: LAST_CALCULATED_AT
     */
    public final void setLastCalculatedAt(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_lastCalculatedAt,value)){
            this._lastCalculatedAt = value;
            internalClearRefs(PROP_ID_lastCalculatedAt);
            
        }
    }
    
    /**
     * 备注: NOTES
     */
    public final java.lang.String getNotes(){
         onPropGet(PROP_ID_notes);
         return _notes;
    }

    /**
     * 备注: NOTES
     */
    public final void setNotes(java.lang.String value){
        if(onPropSet(PROP_ID_notes,value)){
            this._notes = value;
            internalClearRefs(PROP_ID_notes);
            
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
    public final app.erp.crm.dao.entity.ErpCrmForecastPeriod getPeriod(){
       return (app.erp.crm.dao.entity.ErpCrmForecastPeriod)internalGetRefEntity(PROP_NAME_period);
    }

    public final void setPeriod(app.erp.crm.dao.entity.ErpCrmForecastPeriod refEntity){
   
           if(refEntity == null){
           
                   this.setPeriodId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_period, refEntity,()->{
           
                           this.setPeriodId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.crm.dao.entity.ErpCrmTerritory getTerritory(){
       return (app.erp.crm.dao.entity.ErpCrmTerritory)internalGetRefEntity(PROP_NAME_territory);
    }

    public final void setTerritory(app.erp.crm.dao.entity.ErpCrmTerritory refEntity){
   
           if(refEntity == null){
           
                   this.setTerritoryId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_territory, refEntity,()->{
           
                           this.setTerritoryId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.crm.dao.entity.ErpCrmTeam getTeam(){
       return (app.erp.crm.dao.entity.ErpCrmTeam)internalGetRefEntity(PROP_NAME_team);
    }

    public final void setTeam(app.erp.crm.dao.entity.ErpCrmTeam refEntity){
   
           if(refEntity == null){
           
                   this.setTeamId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_team, refEntity,()->{
           
                           this.setTeamId(refEntity.getId());
                       
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
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdCurrency getCurrency(){
       return (app.erp.md.dao.entity.ErpMdCurrency)internalGetRefEntity(PROP_NAME_currency);
    }

    public final void setCurrency(app.erp.md.dao.entity.ErpMdCurrency refEntity){
   
           if(refEntity == null){
           
                   this.setCurrencyId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_currency, refEntity,()->{
           
                           this.setCurrencyId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
