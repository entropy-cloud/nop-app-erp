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

import app.erp.crm.dao.entity.ErpCrmFunnelStageMetrics;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  漏斗阶段度量: erp_crm_funnel_stage_metrics
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpCrmFunnelStageMetrics extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 所属漏斗: FUNNEL_ID BIGINT */
    public static final String PROP_NAME_funnelId = "funnelId";
    public static final int PROP_ID_funnelId = 2;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 阶段: STAGE_ID BIGINT */
    public static final String PROP_NAME_stageId = "stageId";
    public static final int PROP_ID_stageId = 4;
    
    /* 阶段排序: STAGE_ORDER INTEGER */
    public static final String PROP_NAME_stageOrder = "stageOrder";
    public static final int PROP_ID_stageOrder = 5;
    
    /* 阶段名称(快照): STAGE_NAME VARCHAR */
    public static final String PROP_NAME_stageName = "stageName";
    public static final int PROP_ID_stageName = 6;
    
    /* 进入本阶段线索数: LEAD_COUNT_IN INTEGER */
    public static final String PROP_NAME_leadCountIn = "leadCountIn";
    public static final int PROP_ID_leadCountIn = 7;
    
    /* 流出本阶段线索数: LEAD_COUNT_OUT INTEGER */
    public static final String PROP_NAME_leadCountOut = "leadCountOut";
    public static final int PROP_ID_leadCountOut = 8;
    
    /* 期末仍在本阶段数: LEAD_COUNT_REMAINING INTEGER */
    public static final String PROP_NAME_leadCountRemaining = "leadCountRemaining";
    public static final int PROP_ID_leadCountRemaining = 9;
    
    /* 转化率: CONVERSION_RATE DECIMAL */
    public static final String PROP_NAME_conversionRate = "conversionRate";
    public static final int PROP_ID_conversionRate = 10;
    
    /* 流失率: DROP_OFF_RATE DECIMAL */
    public static final String PROP_NAME_dropOffRate = "dropOffRate";
    public static final int PROP_ID_dropOffRate = 11;
    
    /* 平均停留天数: AVG_DAYS_IN_STAGE DECIMAL */
    public static final String PROP_NAME_avgDaysInStage = "avgDaysInStage";
    public static final int PROP_ID_avgDaysInStage = 12;
    
    /* 本阶段丢失数: LOST_COUNT INTEGER */
    public static final String PROP_NAME_lostCount = "lostCount";
    public static final int PROP_ID_lostCount = 13;
    
    /* 本阶段丢失金额: LOST_AMOUNT DECIMAL */
    public static final String PROP_NAME_lostAmount = "lostAmount";
    public static final int PROP_ID_lostAmount = 14;
    
    /* TOP丢失原因(JSON): LOST_REASON_TOP VARCHAR */
    public static final String PROP_NAME_lostReasonTop = "lostReasonTop";
    public static final int PROP_ID_lostReasonTop = 15;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 16;
    
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
    public static final String PROP_NAME_funnel = "funnel";
    
    /* relation:  */
    public static final String PROP_NAME_stage = "stage";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[23];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_funnelId] = PROP_NAME_funnelId;
          PROP_NAME_TO_ID.put(PROP_NAME_funnelId, PROP_ID_funnelId);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_stageId] = PROP_NAME_stageId;
          PROP_NAME_TO_ID.put(PROP_NAME_stageId, PROP_ID_stageId);
      
          PROP_ID_TO_NAME[PROP_ID_stageOrder] = PROP_NAME_stageOrder;
          PROP_NAME_TO_ID.put(PROP_NAME_stageOrder, PROP_ID_stageOrder);
      
          PROP_ID_TO_NAME[PROP_ID_stageName] = PROP_NAME_stageName;
          PROP_NAME_TO_ID.put(PROP_NAME_stageName, PROP_ID_stageName);
      
          PROP_ID_TO_NAME[PROP_ID_leadCountIn] = PROP_NAME_leadCountIn;
          PROP_NAME_TO_ID.put(PROP_NAME_leadCountIn, PROP_ID_leadCountIn);
      
          PROP_ID_TO_NAME[PROP_ID_leadCountOut] = PROP_NAME_leadCountOut;
          PROP_NAME_TO_ID.put(PROP_NAME_leadCountOut, PROP_ID_leadCountOut);
      
          PROP_ID_TO_NAME[PROP_ID_leadCountRemaining] = PROP_NAME_leadCountRemaining;
          PROP_NAME_TO_ID.put(PROP_NAME_leadCountRemaining, PROP_ID_leadCountRemaining);
      
          PROP_ID_TO_NAME[PROP_ID_conversionRate] = PROP_NAME_conversionRate;
          PROP_NAME_TO_ID.put(PROP_NAME_conversionRate, PROP_ID_conversionRate);
      
          PROP_ID_TO_NAME[PROP_ID_dropOffRate] = PROP_NAME_dropOffRate;
          PROP_NAME_TO_ID.put(PROP_NAME_dropOffRate, PROP_ID_dropOffRate);
      
          PROP_ID_TO_NAME[PROP_ID_avgDaysInStage] = PROP_NAME_avgDaysInStage;
          PROP_NAME_TO_ID.put(PROP_NAME_avgDaysInStage, PROP_ID_avgDaysInStage);
      
          PROP_ID_TO_NAME[PROP_ID_lostCount] = PROP_NAME_lostCount;
          PROP_NAME_TO_ID.put(PROP_NAME_lostCount, PROP_ID_lostCount);
      
          PROP_ID_TO_NAME[PROP_ID_lostAmount] = PROP_NAME_lostAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_lostAmount, PROP_ID_lostAmount);
      
          PROP_ID_TO_NAME[PROP_ID_lostReasonTop] = PROP_NAME_lostReasonTop;
          PROP_NAME_TO_ID.put(PROP_NAME_lostReasonTop, PROP_ID_lostReasonTop);
      
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
    
    /* 所属漏斗: FUNNEL_ID */
    private java.lang.Long _funnelId;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 阶段: STAGE_ID */
    private java.lang.Long _stageId;
    
    /* 阶段排序: STAGE_ORDER */
    private java.lang.Integer _stageOrder;
    
    /* 阶段名称(快照): STAGE_NAME */
    private java.lang.String _stageName;
    
    /* 进入本阶段线索数: LEAD_COUNT_IN */
    private java.lang.Integer _leadCountIn;
    
    /* 流出本阶段线索数: LEAD_COUNT_OUT */
    private java.lang.Integer _leadCountOut;
    
    /* 期末仍在本阶段数: LEAD_COUNT_REMAINING */
    private java.lang.Integer _leadCountRemaining;
    
    /* 转化率: CONVERSION_RATE */
    private java.lang.Double _conversionRate;
    
    /* 流失率: DROP_OFF_RATE */
    private java.lang.Double _dropOffRate;
    
    /* 平均停留天数: AVG_DAYS_IN_STAGE */
    private java.lang.Double _avgDaysInStage;
    
    /* 本阶段丢失数: LOST_COUNT */
    private java.lang.Integer _lostCount;
    
    /* 本阶段丢失金额: LOST_AMOUNT */
    private java.math.BigDecimal _lostAmount;
    
    /* TOP丢失原因(JSON): LOST_REASON_TOP */
    private java.lang.String _lostReasonTop;
    
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
    

    public _ErpCrmFunnelStageMetrics(){
        // for debug
    }

    protected ErpCrmFunnelStageMetrics newInstance(){
        ErpCrmFunnelStageMetrics entity = new ErpCrmFunnelStageMetrics();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpCrmFunnelStageMetrics cloneInstance() {
        ErpCrmFunnelStageMetrics entity = newInstance();
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
      return "app.erp.crm.dao.entity.ErpCrmFunnelStageMetrics";
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
        
            case PROP_ID_funnelId:
               return getFunnelId();
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_stageId:
               return getStageId();
        
            case PROP_ID_stageOrder:
               return getStageOrder();
        
            case PROP_ID_stageName:
               return getStageName();
        
            case PROP_ID_leadCountIn:
               return getLeadCountIn();
        
            case PROP_ID_leadCountOut:
               return getLeadCountOut();
        
            case PROP_ID_leadCountRemaining:
               return getLeadCountRemaining();
        
            case PROP_ID_conversionRate:
               return getConversionRate();
        
            case PROP_ID_dropOffRate:
               return getDropOffRate();
        
            case PROP_ID_avgDaysInStage:
               return getAvgDaysInStage();
        
            case PROP_ID_lostCount:
               return getLostCount();
        
            case PROP_ID_lostAmount:
               return getLostAmount();
        
            case PROP_ID_lostReasonTop:
               return getLostReasonTop();
        
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
        
            case PROP_ID_funnelId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_funnelId));
               }
               setFunnelId(typedValue);
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
        
            case PROP_ID_stageId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_stageId));
               }
               setStageId(typedValue);
               break;
            }
        
            case PROP_ID_stageOrder:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_stageOrder));
               }
               setStageOrder(typedValue);
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
        
            case PROP_ID_leadCountIn:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_leadCountIn));
               }
               setLeadCountIn(typedValue);
               break;
            }
        
            case PROP_ID_leadCountOut:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_leadCountOut));
               }
               setLeadCountOut(typedValue);
               break;
            }
        
            case PROP_ID_leadCountRemaining:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_leadCountRemaining));
               }
               setLeadCountRemaining(typedValue);
               break;
            }
        
            case PROP_ID_conversionRate:{
               java.lang.Double typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toDouble(value,
                       err-> newTypeConversionError(PROP_NAME_conversionRate));
               }
               setConversionRate(typedValue);
               break;
            }
        
            case PROP_ID_dropOffRate:{
               java.lang.Double typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toDouble(value,
                       err-> newTypeConversionError(PROP_NAME_dropOffRate));
               }
               setDropOffRate(typedValue);
               break;
            }
        
            case PROP_ID_avgDaysInStage:{
               java.lang.Double typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toDouble(value,
                       err-> newTypeConversionError(PROP_NAME_avgDaysInStage));
               }
               setAvgDaysInStage(typedValue);
               break;
            }
        
            case PROP_ID_lostCount:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_lostCount));
               }
               setLostCount(typedValue);
               break;
            }
        
            case PROP_ID_lostAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_lostAmount));
               }
               setLostAmount(typedValue);
               break;
            }
        
            case PROP_ID_lostReasonTop:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_lostReasonTop));
               }
               setLostReasonTop(typedValue);
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
        
            case PROP_ID_funnelId:{
               onInitProp(propId);
               this._funnelId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_stageId:{
               onInitProp(propId);
               this._stageId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_stageOrder:{
               onInitProp(propId);
               this._stageOrder = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_stageName:{
               onInitProp(propId);
               this._stageName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_leadCountIn:{
               onInitProp(propId);
               this._leadCountIn = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_leadCountOut:{
               onInitProp(propId);
               this._leadCountOut = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_leadCountRemaining:{
               onInitProp(propId);
               this._leadCountRemaining = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_conversionRate:{
               onInitProp(propId);
               this._conversionRate = (java.lang.Double)value;
               
               break;
            }
        
            case PROP_ID_dropOffRate:{
               onInitProp(propId);
               this._dropOffRate = (java.lang.Double)value;
               
               break;
            }
        
            case PROP_ID_avgDaysInStage:{
               onInitProp(propId);
               this._avgDaysInStage = (java.lang.Double)value;
               
               break;
            }
        
            case PROP_ID_lostCount:{
               onInitProp(propId);
               this._lostCount = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_lostAmount:{
               onInitProp(propId);
               this._lostAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_lostReasonTop:{
               onInitProp(propId);
               this._lostReasonTop = (java.lang.String)value;
               
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
     * 所属漏斗: FUNNEL_ID
     */
    public final java.lang.Long getFunnelId(){
         onPropGet(PROP_ID_funnelId);
         return _funnelId;
    }

    /**
     * 所属漏斗: FUNNEL_ID
     */
    public final void setFunnelId(java.lang.Long value){
        if(onPropSet(PROP_ID_funnelId,value)){
            this._funnelId = value;
            internalClearRefs(PROP_ID_funnelId);
            
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
     * 阶段: STAGE_ID
     */
    public final java.lang.Long getStageId(){
         onPropGet(PROP_ID_stageId);
         return _stageId;
    }

    /**
     * 阶段: STAGE_ID
     */
    public final void setStageId(java.lang.Long value){
        if(onPropSet(PROP_ID_stageId,value)){
            this._stageId = value;
            internalClearRefs(PROP_ID_stageId);
            
        }
    }
    
    /**
     * 阶段排序: STAGE_ORDER
     */
    public final java.lang.Integer getStageOrder(){
         onPropGet(PROP_ID_stageOrder);
         return _stageOrder;
    }

    /**
     * 阶段排序: STAGE_ORDER
     */
    public final void setStageOrder(java.lang.Integer value){
        if(onPropSet(PROP_ID_stageOrder,value)){
            this._stageOrder = value;
            internalClearRefs(PROP_ID_stageOrder);
            
        }
    }
    
    /**
     * 阶段名称(快照): STAGE_NAME
     */
    public final java.lang.String getStageName(){
         onPropGet(PROP_ID_stageName);
         return _stageName;
    }

    /**
     * 阶段名称(快照): STAGE_NAME
     */
    public final void setStageName(java.lang.String value){
        if(onPropSet(PROP_ID_stageName,value)){
            this._stageName = value;
            internalClearRefs(PROP_ID_stageName);
            
        }
    }
    
    /**
     * 进入本阶段线索数: LEAD_COUNT_IN
     */
    public final java.lang.Integer getLeadCountIn(){
         onPropGet(PROP_ID_leadCountIn);
         return _leadCountIn;
    }

    /**
     * 进入本阶段线索数: LEAD_COUNT_IN
     */
    public final void setLeadCountIn(java.lang.Integer value){
        if(onPropSet(PROP_ID_leadCountIn,value)){
            this._leadCountIn = value;
            internalClearRefs(PROP_ID_leadCountIn);
            
        }
    }
    
    /**
     * 流出本阶段线索数: LEAD_COUNT_OUT
     */
    public final java.lang.Integer getLeadCountOut(){
         onPropGet(PROP_ID_leadCountOut);
         return _leadCountOut;
    }

    /**
     * 流出本阶段线索数: LEAD_COUNT_OUT
     */
    public final void setLeadCountOut(java.lang.Integer value){
        if(onPropSet(PROP_ID_leadCountOut,value)){
            this._leadCountOut = value;
            internalClearRefs(PROP_ID_leadCountOut);
            
        }
    }
    
    /**
     * 期末仍在本阶段数: LEAD_COUNT_REMAINING
     */
    public final java.lang.Integer getLeadCountRemaining(){
         onPropGet(PROP_ID_leadCountRemaining);
         return _leadCountRemaining;
    }

    /**
     * 期末仍在本阶段数: LEAD_COUNT_REMAINING
     */
    public final void setLeadCountRemaining(java.lang.Integer value){
        if(onPropSet(PROP_ID_leadCountRemaining,value)){
            this._leadCountRemaining = value;
            internalClearRefs(PROP_ID_leadCountRemaining);
            
        }
    }
    
    /**
     * 转化率: CONVERSION_RATE
     */
    public final java.lang.Double getConversionRate(){
         onPropGet(PROP_ID_conversionRate);
         return _conversionRate;
    }

    /**
     * 转化率: CONVERSION_RATE
     */
    public final void setConversionRate(java.lang.Double value){
        if(onPropSet(PROP_ID_conversionRate,value)){
            this._conversionRate = value;
            internalClearRefs(PROP_ID_conversionRate);
            
        }
    }
    
    /**
     * 流失率: DROP_OFF_RATE
     */
    public final java.lang.Double getDropOffRate(){
         onPropGet(PROP_ID_dropOffRate);
         return _dropOffRate;
    }

    /**
     * 流失率: DROP_OFF_RATE
     */
    public final void setDropOffRate(java.lang.Double value){
        if(onPropSet(PROP_ID_dropOffRate,value)){
            this._dropOffRate = value;
            internalClearRefs(PROP_ID_dropOffRate);
            
        }
    }
    
    /**
     * 平均停留天数: AVG_DAYS_IN_STAGE
     */
    public final java.lang.Double getAvgDaysInStage(){
         onPropGet(PROP_ID_avgDaysInStage);
         return _avgDaysInStage;
    }

    /**
     * 平均停留天数: AVG_DAYS_IN_STAGE
     */
    public final void setAvgDaysInStage(java.lang.Double value){
        if(onPropSet(PROP_ID_avgDaysInStage,value)){
            this._avgDaysInStage = value;
            internalClearRefs(PROP_ID_avgDaysInStage);
            
        }
    }
    
    /**
     * 本阶段丢失数: LOST_COUNT
     */
    public final java.lang.Integer getLostCount(){
         onPropGet(PROP_ID_lostCount);
         return _lostCount;
    }

    /**
     * 本阶段丢失数: LOST_COUNT
     */
    public final void setLostCount(java.lang.Integer value){
        if(onPropSet(PROP_ID_lostCount,value)){
            this._lostCount = value;
            internalClearRefs(PROP_ID_lostCount);
            
        }
    }
    
    /**
     * 本阶段丢失金额: LOST_AMOUNT
     */
    public final java.math.BigDecimal getLostAmount(){
         onPropGet(PROP_ID_lostAmount);
         return _lostAmount;
    }

    /**
     * 本阶段丢失金额: LOST_AMOUNT
     */
    public final void setLostAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_lostAmount,value)){
            this._lostAmount = value;
            internalClearRefs(PROP_ID_lostAmount);
            
        }
    }
    
    /**
     * TOP丢失原因(JSON): LOST_REASON_TOP
     */
    public final java.lang.String getLostReasonTop(){
         onPropGet(PROP_ID_lostReasonTop);
         return _lostReasonTop;
    }

    /**
     * TOP丢失原因(JSON): LOST_REASON_TOP
     */
    public final void setLostReasonTop(java.lang.String value){
        if(onPropSet(PROP_ID_lostReasonTop,value)){
            this._lostReasonTop = value;
            internalClearRefs(PROP_ID_lostReasonTop);
            
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
    public final app.erp.crm.dao.entity.ErpCrmLeadFunnel getFunnel(){
       return (app.erp.crm.dao.entity.ErpCrmLeadFunnel)internalGetRefEntity(PROP_NAME_funnel);
    }

    public final void setFunnel(app.erp.crm.dao.entity.ErpCrmLeadFunnel refEntity){
   
           if(refEntity == null){
           
                   this.setFunnelId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_funnel, refEntity,()->{
           
                           this.setFunnelId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.crm.dao.entity.ErpCrmStage getStage(){
       return (app.erp.crm.dao.entity.ErpCrmStage)internalGetRefEntity(PROP_NAME_stage);
    }

    public final void setStage(app.erp.crm.dao.entity.ErpCrmStage refEntity){
   
           if(refEntity == null){
           
                   this.setStageId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_stage, refEntity,()->{
           
                           this.setStageId(refEntity.getId());
                       
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
