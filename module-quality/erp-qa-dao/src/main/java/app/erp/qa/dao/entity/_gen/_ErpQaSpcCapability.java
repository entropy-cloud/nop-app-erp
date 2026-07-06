package app.erp.qa.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.qa.dao.entity.ErpQaSpcCapability;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  SPC 过程能力分析: erp_qa_spc_capability
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpQaSpcCapability extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 控制图ID: CHART_ID BIGINT */
    public static final String PROP_NAME_chartId = "chartId";
    public static final int PROP_ID_chartId = 2;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 分析周期起: PERIOD_FROM DATE */
    public static final String PROP_NAME_periodFrom = "periodFrom";
    public static final int PROP_ID_periodFrom = 4;
    
    /* 分析周期止: PERIOD_TO DATE */
    public static final String PROP_NAME_periodTo = "periodTo";
    public static final int PROP_ID_periodTo = 5;
    
    /* 样本数(子组数): SAMPLE_COUNT INTEGER */
    public static final String PROP_NAME_sampleCount = "sampleCount";
    public static final int PROP_ID_sampleCount = 6;
    
    /* 总观测点数: TOTAL_OBSERVATIONS INTEGER */
    public static final String PROP_NAME_totalObservations = "totalObservations";
    public static final int PROP_ID_totalObservations = 7;
    
    /* 总均值 X̄̄: GRAND_MEAN DECIMAL */
    public static final String PROP_NAME_grandMean = "grandMean";
    public static final int PROP_ID_grandMean = 8;
    
    /* 总体标准差(用于 Pp/Ppk): OVERALL_STD_DEV DECIMAL */
    public static final String PROP_NAME_overallStdDev = "overallStdDev";
    public static final int PROP_ID_overallStdDev = 9;
    
    /* 组内标准差 σ̂=R̄/d2(用于 Cp/Cpk): WITHIN_STD_DEV DECIMAL */
    public static final String PROP_NAME_withinStdDev = "withinStdDev";
    public static final int PROP_ID_withinStdDev = 10;
    
    /* 过程能力指数 Cp: CP DECIMAL */
    public static final String PROP_NAME_cp = "cp";
    public static final int PROP_ID_cp = 11;
    
    /* 过程能力指数 Cpk: CPK DECIMAL */
    public static final String PROP_NAME_cpk = "cpk";
    public static final int PROP_ID_cpk = 12;
    
    /* 过程性能指数 Pp: PP DECIMAL */
    public static final String PROP_NAME_pp = "pp";
    public static final int PROP_ID_pp = 13;
    
    /* 过程性能指数 Ppk: PPK DECIMAL */
    public static final String PROP_NAME_ppk = "ppk";
    public static final int PROP_ID_ppk = 14;
    
    /* 偏度修正 Cpm: CPM DECIMAL */
    public static final String PROP_NAME_cpm = "cpm";
    public static final int PROP_ID_cpm = 15;
    
    /* 能力等级评定: CAPABILITY_LEVEL VARCHAR */
    public static final String PROP_NAME_capabilityLevel = "capabilityLevel";
    public static final int PROP_ID_capabilityLevel = 16;
    
    /* 过程是否统计受控: IS_STABLE BOOLEAN */
    public static final String PROP_NAME_isStable = "isStable";
    public static final int PROP_ID_isStable = 17;
    
    /* 计算人: CALCULATED_BY VARCHAR */
    public static final String PROP_NAME_calculatedBy = "calculatedBy";
    public static final int PROP_ID_calculatedBy = 18;
    
    /* 计算时间: CALCULATED_AT DATETIME */
    public static final String PROP_NAME_calculatedAt = "calculatedAt";
    public static final int PROP_ID_calculatedAt = 19;
    
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
    public static final String PROP_NAME_chart = "chart";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[27];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_chartId] = PROP_NAME_chartId;
          PROP_NAME_TO_ID.put(PROP_NAME_chartId, PROP_ID_chartId);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_periodFrom] = PROP_NAME_periodFrom;
          PROP_NAME_TO_ID.put(PROP_NAME_periodFrom, PROP_ID_periodFrom);
      
          PROP_ID_TO_NAME[PROP_ID_periodTo] = PROP_NAME_periodTo;
          PROP_NAME_TO_ID.put(PROP_NAME_periodTo, PROP_ID_periodTo);
      
          PROP_ID_TO_NAME[PROP_ID_sampleCount] = PROP_NAME_sampleCount;
          PROP_NAME_TO_ID.put(PROP_NAME_sampleCount, PROP_ID_sampleCount);
      
          PROP_ID_TO_NAME[PROP_ID_totalObservations] = PROP_NAME_totalObservations;
          PROP_NAME_TO_ID.put(PROP_NAME_totalObservations, PROP_ID_totalObservations);
      
          PROP_ID_TO_NAME[PROP_ID_grandMean] = PROP_NAME_grandMean;
          PROP_NAME_TO_ID.put(PROP_NAME_grandMean, PROP_ID_grandMean);
      
          PROP_ID_TO_NAME[PROP_ID_overallStdDev] = PROP_NAME_overallStdDev;
          PROP_NAME_TO_ID.put(PROP_NAME_overallStdDev, PROP_ID_overallStdDev);
      
          PROP_ID_TO_NAME[PROP_ID_withinStdDev] = PROP_NAME_withinStdDev;
          PROP_NAME_TO_ID.put(PROP_NAME_withinStdDev, PROP_ID_withinStdDev);
      
          PROP_ID_TO_NAME[PROP_ID_cp] = PROP_NAME_cp;
          PROP_NAME_TO_ID.put(PROP_NAME_cp, PROP_ID_cp);
      
          PROP_ID_TO_NAME[PROP_ID_cpk] = PROP_NAME_cpk;
          PROP_NAME_TO_ID.put(PROP_NAME_cpk, PROP_ID_cpk);
      
          PROP_ID_TO_NAME[PROP_ID_pp] = PROP_NAME_pp;
          PROP_NAME_TO_ID.put(PROP_NAME_pp, PROP_ID_pp);
      
          PROP_ID_TO_NAME[PROP_ID_ppk] = PROP_NAME_ppk;
          PROP_NAME_TO_ID.put(PROP_NAME_ppk, PROP_ID_ppk);
      
          PROP_ID_TO_NAME[PROP_ID_cpm] = PROP_NAME_cpm;
          PROP_NAME_TO_ID.put(PROP_NAME_cpm, PROP_ID_cpm);
      
          PROP_ID_TO_NAME[PROP_ID_capabilityLevel] = PROP_NAME_capabilityLevel;
          PROP_NAME_TO_ID.put(PROP_NAME_capabilityLevel, PROP_ID_capabilityLevel);
      
          PROP_ID_TO_NAME[PROP_ID_isStable] = PROP_NAME_isStable;
          PROP_NAME_TO_ID.put(PROP_NAME_isStable, PROP_ID_isStable);
      
          PROP_ID_TO_NAME[PROP_ID_calculatedBy] = PROP_NAME_calculatedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_calculatedBy, PROP_ID_calculatedBy);
      
          PROP_ID_TO_NAME[PROP_ID_calculatedAt] = PROP_NAME_calculatedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_calculatedAt, PROP_ID_calculatedAt);
      
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
    
    /* 控制图ID: CHART_ID */
    private java.lang.Long _chartId;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 分析周期起: PERIOD_FROM */
    private java.time.LocalDate _periodFrom;
    
    /* 分析周期止: PERIOD_TO */
    private java.time.LocalDate _periodTo;
    
    /* 样本数(子组数): SAMPLE_COUNT */
    private java.lang.Integer _sampleCount;
    
    /* 总观测点数: TOTAL_OBSERVATIONS */
    private java.lang.Integer _totalObservations;
    
    /* 总均值 X̄̄: GRAND_MEAN */
    private java.math.BigDecimal _grandMean;
    
    /* 总体标准差(用于 Pp/Ppk): OVERALL_STD_DEV */
    private java.math.BigDecimal _overallStdDev;
    
    /* 组内标准差 σ̂=R̄/d2(用于 Cp/Cpk): WITHIN_STD_DEV */
    private java.math.BigDecimal _withinStdDev;
    
    /* 过程能力指数 Cp: CP */
    private java.math.BigDecimal _cp;
    
    /* 过程能力指数 Cpk: CPK */
    private java.math.BigDecimal _cpk;
    
    /* 过程性能指数 Pp: PP */
    private java.math.BigDecimal _pp;
    
    /* 过程性能指数 Ppk: PPK */
    private java.math.BigDecimal _ppk;
    
    /* 偏度修正 Cpm: CPM */
    private java.math.BigDecimal _cpm;
    
    /* 能力等级评定: CAPABILITY_LEVEL */
    private java.lang.String _capabilityLevel;
    
    /* 过程是否统计受控: IS_STABLE */
    private java.lang.Boolean _isStable;
    
    /* 计算人: CALCULATED_BY */
    private java.lang.String _calculatedBy;
    
    /* 计算时间: CALCULATED_AT */
    private java.time.LocalDateTime _calculatedAt;
    
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
    

    public _ErpQaSpcCapability(){
        // for debug
    }

    protected ErpQaSpcCapability newInstance(){
        ErpQaSpcCapability entity = new ErpQaSpcCapability();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpQaSpcCapability cloneInstance() {
        ErpQaSpcCapability entity = newInstance();
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
      return "app.erp.qa.dao.entity.ErpQaSpcCapability";
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
        
            case PROP_ID_chartId:
               return getChartId();
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_periodFrom:
               return getPeriodFrom();
        
            case PROP_ID_periodTo:
               return getPeriodTo();
        
            case PROP_ID_sampleCount:
               return getSampleCount();
        
            case PROP_ID_totalObservations:
               return getTotalObservations();
        
            case PROP_ID_grandMean:
               return getGrandMean();
        
            case PROP_ID_overallStdDev:
               return getOverallStdDev();
        
            case PROP_ID_withinStdDev:
               return getWithinStdDev();
        
            case PROP_ID_cp:
               return getCp();
        
            case PROP_ID_cpk:
               return getCpk();
        
            case PROP_ID_pp:
               return getPp();
        
            case PROP_ID_ppk:
               return getPpk();
        
            case PROP_ID_cpm:
               return getCpm();
        
            case PROP_ID_capabilityLevel:
               return getCapabilityLevel();
        
            case PROP_ID_isStable:
               return getIsStable();
        
            case PROP_ID_calculatedBy:
               return getCalculatedBy();
        
            case PROP_ID_calculatedAt:
               return getCalculatedAt();
        
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
        
            case PROP_ID_chartId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_chartId));
               }
               setChartId(typedValue);
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
        
            case PROP_ID_periodFrom:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_periodFrom));
               }
               setPeriodFrom(typedValue);
               break;
            }
        
            case PROP_ID_periodTo:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_periodTo));
               }
               setPeriodTo(typedValue);
               break;
            }
        
            case PROP_ID_sampleCount:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_sampleCount));
               }
               setSampleCount(typedValue);
               break;
            }
        
            case PROP_ID_totalObservations:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_totalObservations));
               }
               setTotalObservations(typedValue);
               break;
            }
        
            case PROP_ID_grandMean:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_grandMean));
               }
               setGrandMean(typedValue);
               break;
            }
        
            case PROP_ID_overallStdDev:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_overallStdDev));
               }
               setOverallStdDev(typedValue);
               break;
            }
        
            case PROP_ID_withinStdDev:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_withinStdDev));
               }
               setWithinStdDev(typedValue);
               break;
            }
        
            case PROP_ID_cp:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_cp));
               }
               setCp(typedValue);
               break;
            }
        
            case PROP_ID_cpk:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_cpk));
               }
               setCpk(typedValue);
               break;
            }
        
            case PROP_ID_pp:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_pp));
               }
               setPp(typedValue);
               break;
            }
        
            case PROP_ID_ppk:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_ppk));
               }
               setPpk(typedValue);
               break;
            }
        
            case PROP_ID_cpm:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_cpm));
               }
               setCpm(typedValue);
               break;
            }
        
            case PROP_ID_capabilityLevel:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_capabilityLevel));
               }
               setCapabilityLevel(typedValue);
               break;
            }
        
            case PROP_ID_isStable:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isStable));
               }
               setIsStable(typedValue);
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
        
            case PROP_ID_calculatedAt:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_calculatedAt));
               }
               setCalculatedAt(typedValue);
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
        
            case PROP_ID_chartId:{
               onInitProp(propId);
               this._chartId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_periodFrom:{
               onInitProp(propId);
               this._periodFrom = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_periodTo:{
               onInitProp(propId);
               this._periodTo = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_sampleCount:{
               onInitProp(propId);
               this._sampleCount = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_totalObservations:{
               onInitProp(propId);
               this._totalObservations = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_grandMean:{
               onInitProp(propId);
               this._grandMean = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_overallStdDev:{
               onInitProp(propId);
               this._overallStdDev = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_withinStdDev:{
               onInitProp(propId);
               this._withinStdDev = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_cp:{
               onInitProp(propId);
               this._cp = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_cpk:{
               onInitProp(propId);
               this._cpk = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_pp:{
               onInitProp(propId);
               this._pp = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_ppk:{
               onInitProp(propId);
               this._ppk = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_cpm:{
               onInitProp(propId);
               this._cpm = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_capabilityLevel:{
               onInitProp(propId);
               this._capabilityLevel = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_isStable:{
               onInitProp(propId);
               this._isStable = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_calculatedBy:{
               onInitProp(propId);
               this._calculatedBy = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_calculatedAt:{
               onInitProp(propId);
               this._calculatedAt = (java.time.LocalDateTime)value;
               
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
     * 控制图ID: CHART_ID
     */
    public final java.lang.Long getChartId(){
         onPropGet(PROP_ID_chartId);
         return _chartId;
    }

    /**
     * 控制图ID: CHART_ID
     */
    public final void setChartId(java.lang.Long value){
        if(onPropSet(PROP_ID_chartId,value)){
            this._chartId = value;
            internalClearRefs(PROP_ID_chartId);
            
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
     * 分析周期起: PERIOD_FROM
     */
    public final java.time.LocalDate getPeriodFrom(){
         onPropGet(PROP_ID_periodFrom);
         return _periodFrom;
    }

    /**
     * 分析周期起: PERIOD_FROM
     */
    public final void setPeriodFrom(java.time.LocalDate value){
        if(onPropSet(PROP_ID_periodFrom,value)){
            this._periodFrom = value;
            internalClearRefs(PROP_ID_periodFrom);
            
        }
    }
    
    /**
     * 分析周期止: PERIOD_TO
     */
    public final java.time.LocalDate getPeriodTo(){
         onPropGet(PROP_ID_periodTo);
         return _periodTo;
    }

    /**
     * 分析周期止: PERIOD_TO
     */
    public final void setPeriodTo(java.time.LocalDate value){
        if(onPropSet(PROP_ID_periodTo,value)){
            this._periodTo = value;
            internalClearRefs(PROP_ID_periodTo);
            
        }
    }
    
    /**
     * 样本数(子组数): SAMPLE_COUNT
     */
    public final java.lang.Integer getSampleCount(){
         onPropGet(PROP_ID_sampleCount);
         return _sampleCount;
    }

    /**
     * 样本数(子组数): SAMPLE_COUNT
     */
    public final void setSampleCount(java.lang.Integer value){
        if(onPropSet(PROP_ID_sampleCount,value)){
            this._sampleCount = value;
            internalClearRefs(PROP_ID_sampleCount);
            
        }
    }
    
    /**
     * 总观测点数: TOTAL_OBSERVATIONS
     */
    public final java.lang.Integer getTotalObservations(){
         onPropGet(PROP_ID_totalObservations);
         return _totalObservations;
    }

    /**
     * 总观测点数: TOTAL_OBSERVATIONS
     */
    public final void setTotalObservations(java.lang.Integer value){
        if(onPropSet(PROP_ID_totalObservations,value)){
            this._totalObservations = value;
            internalClearRefs(PROP_ID_totalObservations);
            
        }
    }
    
    /**
     * 总均值 X̄̄: GRAND_MEAN
     */
    public final java.math.BigDecimal getGrandMean(){
         onPropGet(PROP_ID_grandMean);
         return _grandMean;
    }

    /**
     * 总均值 X̄̄: GRAND_MEAN
     */
    public final void setGrandMean(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_grandMean,value)){
            this._grandMean = value;
            internalClearRefs(PROP_ID_grandMean);
            
        }
    }
    
    /**
     * 总体标准差(用于 Pp/Ppk): OVERALL_STD_DEV
     */
    public final java.math.BigDecimal getOverallStdDev(){
         onPropGet(PROP_ID_overallStdDev);
         return _overallStdDev;
    }

    /**
     * 总体标准差(用于 Pp/Ppk): OVERALL_STD_DEV
     */
    public final void setOverallStdDev(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_overallStdDev,value)){
            this._overallStdDev = value;
            internalClearRefs(PROP_ID_overallStdDev);
            
        }
    }
    
    /**
     * 组内标准差 σ̂=R̄/d2(用于 Cp/Cpk): WITHIN_STD_DEV
     */
    public final java.math.BigDecimal getWithinStdDev(){
         onPropGet(PROP_ID_withinStdDev);
         return _withinStdDev;
    }

    /**
     * 组内标准差 σ̂=R̄/d2(用于 Cp/Cpk): WITHIN_STD_DEV
     */
    public final void setWithinStdDev(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_withinStdDev,value)){
            this._withinStdDev = value;
            internalClearRefs(PROP_ID_withinStdDev);
            
        }
    }
    
    /**
     * 过程能力指数 Cp: CP
     */
    public final java.math.BigDecimal getCp(){
         onPropGet(PROP_ID_cp);
         return _cp;
    }

    /**
     * 过程能力指数 Cp: CP
     */
    public final void setCp(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_cp,value)){
            this._cp = value;
            internalClearRefs(PROP_ID_cp);
            
        }
    }
    
    /**
     * 过程能力指数 Cpk: CPK
     */
    public final java.math.BigDecimal getCpk(){
         onPropGet(PROP_ID_cpk);
         return _cpk;
    }

    /**
     * 过程能力指数 Cpk: CPK
     */
    public final void setCpk(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_cpk,value)){
            this._cpk = value;
            internalClearRefs(PROP_ID_cpk);
            
        }
    }
    
    /**
     * 过程性能指数 Pp: PP
     */
    public final java.math.BigDecimal getPp(){
         onPropGet(PROP_ID_pp);
         return _pp;
    }

    /**
     * 过程性能指数 Pp: PP
     */
    public final void setPp(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_pp,value)){
            this._pp = value;
            internalClearRefs(PROP_ID_pp);
            
        }
    }
    
    /**
     * 过程性能指数 Ppk: PPK
     */
    public final java.math.BigDecimal getPpk(){
         onPropGet(PROP_ID_ppk);
         return _ppk;
    }

    /**
     * 过程性能指数 Ppk: PPK
     */
    public final void setPpk(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_ppk,value)){
            this._ppk = value;
            internalClearRefs(PROP_ID_ppk);
            
        }
    }
    
    /**
     * 偏度修正 Cpm: CPM
     */
    public final java.math.BigDecimal getCpm(){
         onPropGet(PROP_ID_cpm);
         return _cpm;
    }

    /**
     * 偏度修正 Cpm: CPM
     */
    public final void setCpm(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_cpm,value)){
            this._cpm = value;
            internalClearRefs(PROP_ID_cpm);
            
        }
    }
    
    /**
     * 能力等级评定: CAPABILITY_LEVEL
     */
    public final java.lang.String getCapabilityLevel(){
         onPropGet(PROP_ID_capabilityLevel);
         return _capabilityLevel;
    }

    /**
     * 能力等级评定: CAPABILITY_LEVEL
     */
    public final void setCapabilityLevel(java.lang.String value){
        if(onPropSet(PROP_ID_capabilityLevel,value)){
            this._capabilityLevel = value;
            internalClearRefs(PROP_ID_capabilityLevel);
            
        }
    }
    
    /**
     * 过程是否统计受控: IS_STABLE
     */
    public final java.lang.Boolean getIsStable(){
         onPropGet(PROP_ID_isStable);
         return _isStable;
    }

    /**
     * 过程是否统计受控: IS_STABLE
     */
    public final void setIsStable(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isStable,value)){
            this._isStable = value;
            internalClearRefs(PROP_ID_isStable);
            
        }
    }
    
    /**
     * 计算人: CALCULATED_BY
     */
    public final java.lang.String getCalculatedBy(){
         onPropGet(PROP_ID_calculatedBy);
         return _calculatedBy;
    }

    /**
     * 计算人: CALCULATED_BY
     */
    public final void setCalculatedBy(java.lang.String value){
        if(onPropSet(PROP_ID_calculatedBy,value)){
            this._calculatedBy = value;
            internalClearRefs(PROP_ID_calculatedBy);
            
        }
    }
    
    /**
     * 计算时间: CALCULATED_AT
     */
    public final java.time.LocalDateTime getCalculatedAt(){
         onPropGet(PROP_ID_calculatedAt);
         return _calculatedAt;
    }

    /**
     * 计算时间: CALCULATED_AT
     */
    public final void setCalculatedAt(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_calculatedAt,value)){
            this._calculatedAt = value;
            internalClearRefs(PROP_ID_calculatedAt);
            
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
    public final app.erp.qa.dao.entity.ErpQaSpcChart getChart(){
       return (app.erp.qa.dao.entity.ErpQaSpcChart)internalGetRefEntity(PROP_NAME_chart);
    }

    public final void setChart(app.erp.qa.dao.entity.ErpQaSpcChart refEntity){
   
           if(refEntity == null){
           
                   this.setChartId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_chart, refEntity,()->{
           
                           this.setChartId(refEntity.getId());
                       
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
