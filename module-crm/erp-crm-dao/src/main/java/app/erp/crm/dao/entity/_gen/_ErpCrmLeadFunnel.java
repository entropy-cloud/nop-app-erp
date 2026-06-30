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

import app.erp.crm.dao.entity.ErpCrmLeadFunnel;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  线索漏斗: erp_crm_lead_funnel
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpCrmLeadFunnel extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 2;
    
    /* 漏斗名称: FUNNEL_NAME VARCHAR */
    public static final String PROP_NAME_funnelName = "funnelName";
    public static final int PROP_ID_funnelName = 3;
    
    /* 分析期间开始: PERIOD_START DATE */
    public static final String PROP_NAME_periodStart = "periodStart";
    public static final int PROP_ID_periodStart = 4;
    
    /* 分析期间结束: PERIOD_END DATE */
    public static final String PROP_NAME_periodEnd = "periodEnd";
    public static final int PROP_ID_periodEnd = 5;
    
    /* 区域维度: TERRITORY_ID BIGINT */
    public static final String PROP_NAME_territoryId = "territoryId";
    public static final int PROP_ID_territoryId = 6;
    
    /* 团队维度: TEAM_ID BIGINT */
    public static final String PROP_NAME_teamId = "teamId";
    public static final int PROP_ID_teamId = 7;
    
    /* 来源维度: SOURCE_ID BIGINT */
    public static final String PROP_NAME_sourceId = "sourceId";
    public static final int PROP_ID_sourceId = 8;
    
    /* 漏斗顶部线索量: TOTAL_LEADS_AT_TOP INTEGER */
    public static final String PROP_NAME_totalLeadsAtTop = "totalLeadsAtTop";
    public static final int PROP_ID_totalLeadsAtTop = 9;
    
    /* 商机总量: TOTAL_OPPORTUNITIES INTEGER */
    public static final String PROP_NAME_totalOpportunities = "totalOpportunities";
    public static final int PROP_ID_totalOpportunities = 10;
    
    /* 赢单总量: TOTAL_WON INTEGER */
    public static final String PROP_NAME_totalWon = "totalWon";
    public static final int PROP_ID_totalWon = 11;
    
    /* 丢单总量: TOTAL_LOST INTEGER */
    public static final String PROP_NAME_totalLost = "totalLost";
    public static final int PROP_ID_totalLost = 12;
    
    /* 赢单总收入: TOTAL_REVENUE DECIMAL */
    public static final String PROP_NAME_totalRevenue = "totalRevenue";
    public static final int PROP_ID_totalRevenue = 13;
    
    /* 丢失金额合计: LOST_REVENUE DECIMAL */
    public static final String PROP_NAME_lostRevenue = "lostRevenue";
    public static final int PROP_ID_lostRevenue = 14;
    
    /* 加权收入合计: WEIGHTED_REVENUE DECIMAL */
    public static final String PROP_NAME_weightedRevenue = "weightedRevenue";
    public static final int PROP_ID_weightedRevenue = 15;
    
    /* 平均赢单金额: AVG_DEAL_SIZE DECIMAL */
    public static final String PROP_NAME_avgDealSize = "avgDealSize";
    public static final int PROP_ID_avgDealSize = 16;
    
    /* 平均销售周期天数: AVG_SALES_CYCLE_DAYS DECIMAL */
    public static final String PROP_NAME_avgSalesCycleDays = "avgSalesCycleDays";
    public static final int PROP_ID_avgSalesCycleDays = 17;
    
    /* 聚合计算时间: CALCULATED_AT DATETIME */
    public static final String PROP_NAME_calculatedAt = "calculatedAt";
    public static final int PROP_ID_calculatedAt = 18;
    
    /* 聚合计算人: CALCULATED_BY VARCHAR */
    public static final String PROP_NAME_calculatedBy = "calculatedBy";
    public static final int PROP_ID_calculatedBy = 19;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 20;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 21;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 22;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 23;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 24;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 25;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 26;
    

    private static int _PROP_ID_BOUND = 27;

    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    
    /* relation:  */
    public static final String PROP_NAME_territory = "territory";
    
    /* relation:  */
    public static final String PROP_NAME_team = "team";
    
    /* relation:  */
    public static final String PROP_NAME_source = "source";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[27];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_funnelName] = PROP_NAME_funnelName;
          PROP_NAME_TO_ID.put(PROP_NAME_funnelName, PROP_ID_funnelName);
      
          PROP_ID_TO_NAME[PROP_ID_periodStart] = PROP_NAME_periodStart;
          PROP_NAME_TO_ID.put(PROP_NAME_periodStart, PROP_ID_periodStart);
      
          PROP_ID_TO_NAME[PROP_ID_periodEnd] = PROP_NAME_periodEnd;
          PROP_NAME_TO_ID.put(PROP_NAME_periodEnd, PROP_ID_periodEnd);
      
          PROP_ID_TO_NAME[PROP_ID_territoryId] = PROP_NAME_territoryId;
          PROP_NAME_TO_ID.put(PROP_NAME_territoryId, PROP_ID_territoryId);
      
          PROP_ID_TO_NAME[PROP_ID_teamId] = PROP_NAME_teamId;
          PROP_NAME_TO_ID.put(PROP_NAME_teamId, PROP_ID_teamId);
      
          PROP_ID_TO_NAME[PROP_ID_sourceId] = PROP_NAME_sourceId;
          PROP_NAME_TO_ID.put(PROP_NAME_sourceId, PROP_ID_sourceId);
      
          PROP_ID_TO_NAME[PROP_ID_totalLeadsAtTop] = PROP_NAME_totalLeadsAtTop;
          PROP_NAME_TO_ID.put(PROP_NAME_totalLeadsAtTop, PROP_ID_totalLeadsAtTop);
      
          PROP_ID_TO_NAME[PROP_ID_totalOpportunities] = PROP_NAME_totalOpportunities;
          PROP_NAME_TO_ID.put(PROP_NAME_totalOpportunities, PROP_ID_totalOpportunities);
      
          PROP_ID_TO_NAME[PROP_ID_totalWon] = PROP_NAME_totalWon;
          PROP_NAME_TO_ID.put(PROP_NAME_totalWon, PROP_ID_totalWon);
      
          PROP_ID_TO_NAME[PROP_ID_totalLost] = PROP_NAME_totalLost;
          PROP_NAME_TO_ID.put(PROP_NAME_totalLost, PROP_ID_totalLost);
      
          PROP_ID_TO_NAME[PROP_ID_totalRevenue] = PROP_NAME_totalRevenue;
          PROP_NAME_TO_ID.put(PROP_NAME_totalRevenue, PROP_ID_totalRevenue);
      
          PROP_ID_TO_NAME[PROP_ID_lostRevenue] = PROP_NAME_lostRevenue;
          PROP_NAME_TO_ID.put(PROP_NAME_lostRevenue, PROP_ID_lostRevenue);
      
          PROP_ID_TO_NAME[PROP_ID_weightedRevenue] = PROP_NAME_weightedRevenue;
          PROP_NAME_TO_ID.put(PROP_NAME_weightedRevenue, PROP_ID_weightedRevenue);
      
          PROP_ID_TO_NAME[PROP_ID_avgDealSize] = PROP_NAME_avgDealSize;
          PROP_NAME_TO_ID.put(PROP_NAME_avgDealSize, PROP_ID_avgDealSize);
      
          PROP_ID_TO_NAME[PROP_ID_avgSalesCycleDays] = PROP_NAME_avgSalesCycleDays;
          PROP_NAME_TO_ID.put(PROP_NAME_avgSalesCycleDays, PROP_ID_avgSalesCycleDays);
      
          PROP_ID_TO_NAME[PROP_ID_calculatedAt] = PROP_NAME_calculatedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_calculatedAt, PROP_ID_calculatedAt);
      
          PROP_ID_TO_NAME[PROP_ID_calculatedBy] = PROP_NAME_calculatedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_calculatedBy, PROP_ID_calculatedBy);
      
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
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 漏斗名称: FUNNEL_NAME */
    private java.lang.String _funnelName;
    
    /* 分析期间开始: PERIOD_START */
    private java.time.LocalDate _periodStart;
    
    /* 分析期间结束: PERIOD_END */
    private java.time.LocalDate _periodEnd;
    
    /* 区域维度: TERRITORY_ID */
    private java.lang.Long _territoryId;
    
    /* 团队维度: TEAM_ID */
    private java.lang.Long _teamId;
    
    /* 来源维度: SOURCE_ID */
    private java.lang.Long _sourceId;
    
    /* 漏斗顶部线索量: TOTAL_LEADS_AT_TOP */
    private java.lang.Integer _totalLeadsAtTop;
    
    /* 商机总量: TOTAL_OPPORTUNITIES */
    private java.lang.Integer _totalOpportunities;
    
    /* 赢单总量: TOTAL_WON */
    private java.lang.Integer _totalWon;
    
    /* 丢单总量: TOTAL_LOST */
    private java.lang.Integer _totalLost;
    
    /* 赢单总收入: TOTAL_REVENUE */
    private java.lang.String _totalRevenue;
    
    /* 丢失金额合计: LOST_REVENUE */
    private java.lang.String _lostRevenue;
    
    /* 加权收入合计: WEIGHTED_REVENUE */
    private java.lang.String _weightedRevenue;
    
    /* 平均赢单金额: AVG_DEAL_SIZE */
    private java.lang.String _avgDealSize;
    
    /* 平均销售周期天数: AVG_SALES_CYCLE_DAYS */
    private java.lang.Double _avgSalesCycleDays;
    
    /* 聚合计算时间: CALCULATED_AT */
    private java.time.LocalDateTime _calculatedAt;
    
    /* 聚合计算人: CALCULATED_BY */
    private java.lang.String _calculatedBy;
    
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
    

    public _ErpCrmLeadFunnel(){
        // for debug
    }

    protected ErpCrmLeadFunnel newInstance(){
        ErpCrmLeadFunnel entity = new ErpCrmLeadFunnel();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpCrmLeadFunnel cloneInstance() {
        ErpCrmLeadFunnel entity = newInstance();
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
      return "app.erp.crm.dao.entity.ErpCrmLeadFunnel";
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
        
            case PROP_ID_funnelName:
               return getFunnelName();
        
            case PROP_ID_periodStart:
               return getPeriodStart();
        
            case PROP_ID_periodEnd:
               return getPeriodEnd();
        
            case PROP_ID_territoryId:
               return getTerritoryId();
        
            case PROP_ID_teamId:
               return getTeamId();
        
            case PROP_ID_sourceId:
               return getSourceId();
        
            case PROP_ID_totalLeadsAtTop:
               return getTotalLeadsAtTop();
        
            case PROP_ID_totalOpportunities:
               return getTotalOpportunities();
        
            case PROP_ID_totalWon:
               return getTotalWon();
        
            case PROP_ID_totalLost:
               return getTotalLost();
        
            case PROP_ID_totalRevenue:
               return getTotalRevenue();
        
            case PROP_ID_lostRevenue:
               return getLostRevenue();
        
            case PROP_ID_weightedRevenue:
               return getWeightedRevenue();
        
            case PROP_ID_avgDealSize:
               return getAvgDealSize();
        
            case PROP_ID_avgSalesCycleDays:
               return getAvgSalesCycleDays();
        
            case PROP_ID_calculatedAt:
               return getCalculatedAt();
        
            case PROP_ID_calculatedBy:
               return getCalculatedBy();
        
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
        
            case PROP_ID_funnelName:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_funnelName));
               }
               setFunnelName(typedValue);
               break;
            }
        
            case PROP_ID_periodStart:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_periodStart));
               }
               setPeriodStart(typedValue);
               break;
            }
        
            case PROP_ID_periodEnd:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_periodEnd));
               }
               setPeriodEnd(typedValue);
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
        
            case PROP_ID_sourceId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_sourceId));
               }
               setSourceId(typedValue);
               break;
            }
        
            case PROP_ID_totalLeadsAtTop:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_totalLeadsAtTop));
               }
               setTotalLeadsAtTop(typedValue);
               break;
            }
        
            case PROP_ID_totalOpportunities:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_totalOpportunities));
               }
               setTotalOpportunities(typedValue);
               break;
            }
        
            case PROP_ID_totalWon:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_totalWon));
               }
               setTotalWon(typedValue);
               break;
            }
        
            case PROP_ID_totalLost:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_totalLost));
               }
               setTotalLost(typedValue);
               break;
            }
        
            case PROP_ID_totalRevenue:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_totalRevenue));
               }
               setTotalRevenue(typedValue);
               break;
            }
        
            case PROP_ID_lostRevenue:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_lostRevenue));
               }
               setLostRevenue(typedValue);
               break;
            }
        
            case PROP_ID_weightedRevenue:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_weightedRevenue));
               }
               setWeightedRevenue(typedValue);
               break;
            }
        
            case PROP_ID_avgDealSize:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_avgDealSize));
               }
               setAvgDealSize(typedValue);
               break;
            }
        
            case PROP_ID_avgSalesCycleDays:{
               java.lang.Double typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toDouble(value,
                       err-> newTypeConversionError(PROP_NAME_avgSalesCycleDays));
               }
               setAvgSalesCycleDays(typedValue);
               break;
            }
        
            case PROP_ID_calculatedAt:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_calculatedAt));
               }
               setCalculatedAt(typedValue);
               break;
            }
        
            case PROP_ID_calculatedBy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_calculatedBy));
               }
               setCalculatedBy(typedValue);
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
        
            case PROP_ID_funnelName:{
               onInitProp(propId);
               this._funnelName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_periodStart:{
               onInitProp(propId);
               this._periodStart = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_periodEnd:{
               onInitProp(propId);
               this._periodEnd = (java.time.LocalDate)value;
               
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
        
            case PROP_ID_sourceId:{
               onInitProp(propId);
               this._sourceId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_totalLeadsAtTop:{
               onInitProp(propId);
               this._totalLeadsAtTop = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_totalOpportunities:{
               onInitProp(propId);
               this._totalOpportunities = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_totalWon:{
               onInitProp(propId);
               this._totalWon = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_totalLost:{
               onInitProp(propId);
               this._totalLost = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_totalRevenue:{
               onInitProp(propId);
               this._totalRevenue = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_lostRevenue:{
               onInitProp(propId);
               this._lostRevenue = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_weightedRevenue:{
               onInitProp(propId);
               this._weightedRevenue = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_avgDealSize:{
               onInitProp(propId);
               this._avgDealSize = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_avgSalesCycleDays:{
               onInitProp(propId);
               this._avgSalesCycleDays = (java.lang.Double)value;
               
               break;
            }
        
            case PROP_ID_calculatedAt:{
               onInitProp(propId);
               this._calculatedAt = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_calculatedBy:{
               onInitProp(propId);
               this._calculatedBy = (java.lang.String)value;
               
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
     * 漏斗名称: FUNNEL_NAME
     */
    public final java.lang.String getFunnelName(){
         onPropGet(PROP_ID_funnelName);
         return _funnelName;
    }

    /**
     * 漏斗名称: FUNNEL_NAME
     */
    public final void setFunnelName(java.lang.String value){
        if(onPropSet(PROP_ID_funnelName,value)){
            this._funnelName = value;
            internalClearRefs(PROP_ID_funnelName);
            
        }
    }
    
    /**
     * 分析期间开始: PERIOD_START
     */
    public final java.time.LocalDate getPeriodStart(){
         onPropGet(PROP_ID_periodStart);
         return _periodStart;
    }

    /**
     * 分析期间开始: PERIOD_START
     */
    public final void setPeriodStart(java.time.LocalDate value){
        if(onPropSet(PROP_ID_periodStart,value)){
            this._periodStart = value;
            internalClearRefs(PROP_ID_periodStart);
            
        }
    }
    
    /**
     * 分析期间结束: PERIOD_END
     */
    public final java.time.LocalDate getPeriodEnd(){
         onPropGet(PROP_ID_periodEnd);
         return _periodEnd;
    }

    /**
     * 分析期间结束: PERIOD_END
     */
    public final void setPeriodEnd(java.time.LocalDate value){
        if(onPropSet(PROP_ID_periodEnd,value)){
            this._periodEnd = value;
            internalClearRefs(PROP_ID_periodEnd);
            
        }
    }
    
    /**
     * 区域维度: TERRITORY_ID
     */
    public final java.lang.Long getTerritoryId(){
         onPropGet(PROP_ID_territoryId);
         return _territoryId;
    }

    /**
     * 区域维度: TERRITORY_ID
     */
    public final void setTerritoryId(java.lang.Long value){
        if(onPropSet(PROP_ID_territoryId,value)){
            this._territoryId = value;
            internalClearRefs(PROP_ID_territoryId);
            
        }
    }
    
    /**
     * 团队维度: TEAM_ID
     */
    public final java.lang.Long getTeamId(){
         onPropGet(PROP_ID_teamId);
         return _teamId;
    }

    /**
     * 团队维度: TEAM_ID
     */
    public final void setTeamId(java.lang.Long value){
        if(onPropSet(PROP_ID_teamId,value)){
            this._teamId = value;
            internalClearRefs(PROP_ID_teamId);
            
        }
    }
    
    /**
     * 来源维度: SOURCE_ID
     */
    public final java.lang.Long getSourceId(){
         onPropGet(PROP_ID_sourceId);
         return _sourceId;
    }

    /**
     * 来源维度: SOURCE_ID
     */
    public final void setSourceId(java.lang.Long value){
        if(onPropSet(PROP_ID_sourceId,value)){
            this._sourceId = value;
            internalClearRefs(PROP_ID_sourceId);
            
        }
    }
    
    /**
     * 漏斗顶部线索量: TOTAL_LEADS_AT_TOP
     */
    public final java.lang.Integer getTotalLeadsAtTop(){
         onPropGet(PROP_ID_totalLeadsAtTop);
         return _totalLeadsAtTop;
    }

    /**
     * 漏斗顶部线索量: TOTAL_LEADS_AT_TOP
     */
    public final void setTotalLeadsAtTop(java.lang.Integer value){
        if(onPropSet(PROP_ID_totalLeadsAtTop,value)){
            this._totalLeadsAtTop = value;
            internalClearRefs(PROP_ID_totalLeadsAtTop);
            
        }
    }
    
    /**
     * 商机总量: TOTAL_OPPORTUNITIES
     */
    public final java.lang.Integer getTotalOpportunities(){
         onPropGet(PROP_ID_totalOpportunities);
         return _totalOpportunities;
    }

    /**
     * 商机总量: TOTAL_OPPORTUNITIES
     */
    public final void setTotalOpportunities(java.lang.Integer value){
        if(onPropSet(PROP_ID_totalOpportunities,value)){
            this._totalOpportunities = value;
            internalClearRefs(PROP_ID_totalOpportunities);
            
        }
    }
    
    /**
     * 赢单总量: TOTAL_WON
     */
    public final java.lang.Integer getTotalWon(){
         onPropGet(PROP_ID_totalWon);
         return _totalWon;
    }

    /**
     * 赢单总量: TOTAL_WON
     */
    public final void setTotalWon(java.lang.Integer value){
        if(onPropSet(PROP_ID_totalWon,value)){
            this._totalWon = value;
            internalClearRefs(PROP_ID_totalWon);
            
        }
    }
    
    /**
     * 丢单总量: TOTAL_LOST
     */
    public final java.lang.Integer getTotalLost(){
         onPropGet(PROP_ID_totalLost);
         return _totalLost;
    }

    /**
     * 丢单总量: TOTAL_LOST
     */
    public final void setTotalLost(java.lang.Integer value){
        if(onPropSet(PROP_ID_totalLost,value)){
            this._totalLost = value;
            internalClearRefs(PROP_ID_totalLost);
            
        }
    }
    
    /**
     * 赢单总收入: TOTAL_REVENUE
     */
    public final java.lang.String getTotalRevenue(){
         onPropGet(PROP_ID_totalRevenue);
         return _totalRevenue;
    }

    /**
     * 赢单总收入: TOTAL_REVENUE
     */
    public final void setTotalRevenue(java.lang.String value){
        if(onPropSet(PROP_ID_totalRevenue,value)){
            this._totalRevenue = value;
            internalClearRefs(PROP_ID_totalRevenue);
            
        }
    }
    
    /**
     * 丢失金额合计: LOST_REVENUE
     */
    public final java.lang.String getLostRevenue(){
         onPropGet(PROP_ID_lostRevenue);
         return _lostRevenue;
    }

    /**
     * 丢失金额合计: LOST_REVENUE
     */
    public final void setLostRevenue(java.lang.String value){
        if(onPropSet(PROP_ID_lostRevenue,value)){
            this._lostRevenue = value;
            internalClearRefs(PROP_ID_lostRevenue);
            
        }
    }
    
    /**
     * 加权收入合计: WEIGHTED_REVENUE
     */
    public final java.lang.String getWeightedRevenue(){
         onPropGet(PROP_ID_weightedRevenue);
         return _weightedRevenue;
    }

    /**
     * 加权收入合计: WEIGHTED_REVENUE
     */
    public final void setWeightedRevenue(java.lang.String value){
        if(onPropSet(PROP_ID_weightedRevenue,value)){
            this._weightedRevenue = value;
            internalClearRefs(PROP_ID_weightedRevenue);
            
        }
    }
    
    /**
     * 平均赢单金额: AVG_DEAL_SIZE
     */
    public final java.lang.String getAvgDealSize(){
         onPropGet(PROP_ID_avgDealSize);
         return _avgDealSize;
    }

    /**
     * 平均赢单金额: AVG_DEAL_SIZE
     */
    public final void setAvgDealSize(java.lang.String value){
        if(onPropSet(PROP_ID_avgDealSize,value)){
            this._avgDealSize = value;
            internalClearRefs(PROP_ID_avgDealSize);
            
        }
    }
    
    /**
     * 平均销售周期天数: AVG_SALES_CYCLE_DAYS
     */
    public final java.lang.Double getAvgSalesCycleDays(){
         onPropGet(PROP_ID_avgSalesCycleDays);
         return _avgSalesCycleDays;
    }

    /**
     * 平均销售周期天数: AVG_SALES_CYCLE_DAYS
     */
    public final void setAvgSalesCycleDays(java.lang.Double value){
        if(onPropSet(PROP_ID_avgSalesCycleDays,value)){
            this._avgSalesCycleDays = value;
            internalClearRefs(PROP_ID_avgSalesCycleDays);
            
        }
    }
    
    /**
     * 聚合计算时间: CALCULATED_AT
     */
    public final java.time.LocalDateTime getCalculatedAt(){
         onPropGet(PROP_ID_calculatedAt);
         return _calculatedAt;
    }

    /**
     * 聚合计算时间: CALCULATED_AT
     */
    public final void setCalculatedAt(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_calculatedAt,value)){
            this._calculatedAt = value;
            internalClearRefs(PROP_ID_calculatedAt);
            
        }
    }
    
    /**
     * 聚合计算人: CALCULATED_BY
     */
    public final java.lang.String getCalculatedBy(){
         onPropGet(PROP_ID_calculatedBy);
         return _calculatedBy;
    }

    /**
     * 聚合计算人: CALCULATED_BY
     */
    public final void setCalculatedBy(java.lang.String value){
        if(onPropSet(PROP_ID_calculatedBy,value)){
            this._calculatedBy = value;
            internalClearRefs(PROP_ID_calculatedBy);
            
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
    public final app.erp.crm.dao.entity.ErpCrmSource getSource(){
       return (app.erp.crm.dao.entity.ErpCrmSource)internalGetRefEntity(PROP_NAME_source);
    }

    public final void setSource(app.erp.crm.dao.entity.ErpCrmSource refEntity){
   
           if(refEntity == null){
           
                   this.setSourceId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_source, refEntity,()->{
           
                           this.setSourceId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
