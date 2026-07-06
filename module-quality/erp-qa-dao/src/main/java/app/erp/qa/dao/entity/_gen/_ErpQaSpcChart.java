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

import app.erp.qa.dao.entity.ErpQaSpcChart;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  SPC 控制图配置: erp_qa_spc_chart
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpQaSpcChart extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 编码: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 名称: NAME VARCHAR */
    public static final String PROP_NAME_name = "name";
    public static final int PROP_ID_name = 3;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 4;
    
    /* 图类型: CHART_TYPE VARCHAR */
    public static final String PROP_NAME_chartType = "chartType";
    public static final int PROP_ID_chartType = 5;
    
    /* 物料: MATERIAL_ID BIGINT */
    public static final String PROP_NAME_materialId = "materialId";
    public static final int PROP_ID_materialId = 6;
    
    /* 质检模板: INSPECTION_TYPE_ID BIGINT */
    public static final String PROP_NAME_inspectionTypeId = "inspectionTypeId";
    public static final int PROP_ID_inspectionTypeId = 7;
    
    /* 关键检验参数: PARAMETER_ID BIGINT */
    public static final String PROP_NAME_parameterId = "parameterId";
    public static final int PROP_ID_parameterId = 8;
    
    /* 规格下限: SPEC_MIN DECIMAL */
    public static final String PROP_NAME_specMin = "specMin";
    public static final int PROP_ID_specMin = 9;
    
    /* 规格上限: SPEC_MAX DECIMAL */
    public static final String PROP_NAME_specMax = "specMax";
    public static final int PROP_ID_specMax = 10;
    
    /* 子组样本量 n: SUBGROUP_SIZE INTEGER */
    public static final String PROP_NAME_subgroupSize = "subgroupSize";
    public static final int PROP_ID_subgroupSize = 11;
    
    /* 采样频率: SAMPLING_FREQUENCY VARCHAR */
    public static final String PROP_NAME_samplingFrequency = "samplingFrequency";
    public static final int PROP_ID_samplingFrequency = 12;
    
    /* 中心线计算方式: CL_CENTER_TYPE VARCHAR */
    public static final String PROP_NAME_clCenterType = "clCenterType";
    public static final int PROP_ID_clCenterType = 13;
    
    /* 判异规则集(逗号分隔): RULE_SET VARCHAR */
    public static final String PROP_NAME_ruleSet = "ruleSet";
    public static final int PROP_ID_ruleSet = 14;
    
    /* 预警阈值(违规次数): ALARM_THRESHOLD INTEGER */
    public static final String PROP_NAME_alarmThreshold = "alarmThreshold";
    public static final int PROP_ID_alarmThreshold = 15;
    
    /* 控制上限(UCL): UCL DECIMAL */
    public static final String PROP_NAME_ucl = "ucl";
    public static final int PROP_ID_ucl = 16;
    
    /* 控制下限(LCL): LCL DECIMAL */
    public static final String PROP_NAME_lcl = "lcl";
    public static final int PROP_ID_lcl = 17;
    
    /* 中心线(CL): CL DECIMAL */
    public static final String PROP_NAME_cl = "cl";
    public static final int PROP_ID_cl = 18;
    
    /* 计算状态: CALC_STATUS VARCHAR */
    public static final String PROP_NAME_calcStatus = "calcStatus";
    public static final int PROP_ID_calcStatus = 19;
    
    /* 是否启用: IS_ACTIVE BOOLEAN */
    public static final String PROP_NAME_isActive = "isActive";
    public static final int PROP_ID_isActive = 20;
    
    /* 单据状态: DOC_STATUS VARCHAR */
    public static final String PROP_NAME_docStatus = "docStatus";
    public static final int PROP_ID_docStatus = 21;
    
    /* 审核状态: APPROVE_STATUS VARCHAR */
    public static final String PROP_NAME_approveStatus = "approveStatus";
    public static final int PROP_ID_approveStatus = 22;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 23;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 24;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 25;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 26;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 27;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 28;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 29;
    

    private static int _PROP_ID_BOUND = 30;

    
    /* relation:  */
    public static final String PROP_NAME_material = "material";
    
    /* relation:  */
    public static final String PROP_NAME_inspectionType = "inspectionType";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    
    /* relation:  */
    public static final String PROP_NAME_samples = "samples";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[30];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_name] = PROP_NAME_name;
          PROP_NAME_TO_ID.put(PROP_NAME_name, PROP_ID_name);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_chartType] = PROP_NAME_chartType;
          PROP_NAME_TO_ID.put(PROP_NAME_chartType, PROP_ID_chartType);
      
          PROP_ID_TO_NAME[PROP_ID_materialId] = PROP_NAME_materialId;
          PROP_NAME_TO_ID.put(PROP_NAME_materialId, PROP_ID_materialId);
      
          PROP_ID_TO_NAME[PROP_ID_inspectionTypeId] = PROP_NAME_inspectionTypeId;
          PROP_NAME_TO_ID.put(PROP_NAME_inspectionTypeId, PROP_ID_inspectionTypeId);
      
          PROP_ID_TO_NAME[PROP_ID_parameterId] = PROP_NAME_parameterId;
          PROP_NAME_TO_ID.put(PROP_NAME_parameterId, PROP_ID_parameterId);
      
          PROP_ID_TO_NAME[PROP_ID_specMin] = PROP_NAME_specMin;
          PROP_NAME_TO_ID.put(PROP_NAME_specMin, PROP_ID_specMin);
      
          PROP_ID_TO_NAME[PROP_ID_specMax] = PROP_NAME_specMax;
          PROP_NAME_TO_ID.put(PROP_NAME_specMax, PROP_ID_specMax);
      
          PROP_ID_TO_NAME[PROP_ID_subgroupSize] = PROP_NAME_subgroupSize;
          PROP_NAME_TO_ID.put(PROP_NAME_subgroupSize, PROP_ID_subgroupSize);
      
          PROP_ID_TO_NAME[PROP_ID_samplingFrequency] = PROP_NAME_samplingFrequency;
          PROP_NAME_TO_ID.put(PROP_NAME_samplingFrequency, PROP_ID_samplingFrequency);
      
          PROP_ID_TO_NAME[PROP_ID_clCenterType] = PROP_NAME_clCenterType;
          PROP_NAME_TO_ID.put(PROP_NAME_clCenterType, PROP_ID_clCenterType);
      
          PROP_ID_TO_NAME[PROP_ID_ruleSet] = PROP_NAME_ruleSet;
          PROP_NAME_TO_ID.put(PROP_NAME_ruleSet, PROP_ID_ruleSet);
      
          PROP_ID_TO_NAME[PROP_ID_alarmThreshold] = PROP_NAME_alarmThreshold;
          PROP_NAME_TO_ID.put(PROP_NAME_alarmThreshold, PROP_ID_alarmThreshold);
      
          PROP_ID_TO_NAME[PROP_ID_ucl] = PROP_NAME_ucl;
          PROP_NAME_TO_ID.put(PROP_NAME_ucl, PROP_ID_ucl);
      
          PROP_ID_TO_NAME[PROP_ID_lcl] = PROP_NAME_lcl;
          PROP_NAME_TO_ID.put(PROP_NAME_lcl, PROP_ID_lcl);
      
          PROP_ID_TO_NAME[PROP_ID_cl] = PROP_NAME_cl;
          PROP_NAME_TO_ID.put(PROP_NAME_cl, PROP_ID_cl);
      
          PROP_ID_TO_NAME[PROP_ID_calcStatus] = PROP_NAME_calcStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_calcStatus, PROP_ID_calcStatus);
      
          PROP_ID_TO_NAME[PROP_ID_isActive] = PROP_NAME_isActive;
          PROP_NAME_TO_ID.put(PROP_NAME_isActive, PROP_ID_isActive);
      
          PROP_ID_TO_NAME[PROP_ID_docStatus] = PROP_NAME_docStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_docStatus, PROP_ID_docStatus);
      
          PROP_ID_TO_NAME[PROP_ID_approveStatus] = PROP_NAME_approveStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_approveStatus, PROP_ID_approveStatus);
      
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
    
    /* 编码: CODE */
    private java.lang.String _code;
    
    /* 名称: NAME */
    private java.lang.String _name;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 图类型: CHART_TYPE */
    private java.lang.String _chartType;
    
    /* 物料: MATERIAL_ID */
    private java.lang.Long _materialId;
    
    /* 质检模板: INSPECTION_TYPE_ID */
    private java.lang.Long _inspectionTypeId;
    
    /* 关键检验参数: PARAMETER_ID */
    private java.lang.Long _parameterId;
    
    /* 规格下限: SPEC_MIN */
    private java.math.BigDecimal _specMin;
    
    /* 规格上限: SPEC_MAX */
    private java.math.BigDecimal _specMax;
    
    /* 子组样本量 n: SUBGROUP_SIZE */
    private java.lang.Integer _subgroupSize;
    
    /* 采样频率: SAMPLING_FREQUENCY */
    private java.lang.String _samplingFrequency;
    
    /* 中心线计算方式: CL_CENTER_TYPE */
    private java.lang.String _clCenterType;
    
    /* 判异规则集(逗号分隔): RULE_SET */
    private java.lang.String _ruleSet;
    
    /* 预警阈值(违规次数): ALARM_THRESHOLD */
    private java.lang.Integer _alarmThreshold;
    
    /* 控制上限(UCL): UCL */
    private java.math.BigDecimal _ucl;
    
    /* 控制下限(LCL): LCL */
    private java.math.BigDecimal _lcl;
    
    /* 中心线(CL): CL */
    private java.math.BigDecimal _cl;
    
    /* 计算状态: CALC_STATUS */
    private java.lang.String _calcStatus;
    
    /* 是否启用: IS_ACTIVE */
    private java.lang.Boolean _isActive;
    
    /* 单据状态: DOC_STATUS */
    private java.lang.String _docStatus;
    
    /* 审核状态: APPROVE_STATUS */
    private java.lang.String _approveStatus;
    
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
    

    public _ErpQaSpcChart(){
        // for debug
    }

    protected ErpQaSpcChart newInstance(){
        ErpQaSpcChart entity = new ErpQaSpcChart();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpQaSpcChart cloneInstance() {
        ErpQaSpcChart entity = newInstance();
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
      return "app.erp.qa.dao.entity.ErpQaSpcChart";
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
        
            case PROP_ID_code:
               return getCode();
        
            case PROP_ID_name:
               return getName();
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_chartType:
               return getChartType();
        
            case PROP_ID_materialId:
               return getMaterialId();
        
            case PROP_ID_inspectionTypeId:
               return getInspectionTypeId();
        
            case PROP_ID_parameterId:
               return getParameterId();
        
            case PROP_ID_specMin:
               return getSpecMin();
        
            case PROP_ID_specMax:
               return getSpecMax();
        
            case PROP_ID_subgroupSize:
               return getSubgroupSize();
        
            case PROP_ID_samplingFrequency:
               return getSamplingFrequency();
        
            case PROP_ID_clCenterType:
               return getClCenterType();
        
            case PROP_ID_ruleSet:
               return getRuleSet();
        
            case PROP_ID_alarmThreshold:
               return getAlarmThreshold();
        
            case PROP_ID_ucl:
               return getUcl();
        
            case PROP_ID_lcl:
               return getLcl();
        
            case PROP_ID_cl:
               return getCl();
        
            case PROP_ID_calcStatus:
               return getCalcStatus();
        
            case PROP_ID_isActive:
               return getIsActive();
        
            case PROP_ID_docStatus:
               return getDocStatus();
        
            case PROP_ID_approveStatus:
               return getApproveStatus();
        
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
        
            case PROP_ID_code:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_code));
               }
               setCode(typedValue);
               break;
            }
        
            case PROP_ID_name:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_name));
               }
               setName(typedValue);
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
        
            case PROP_ID_chartType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_chartType));
               }
               setChartType(typedValue);
               break;
            }
        
            case PROP_ID_materialId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_materialId));
               }
               setMaterialId(typedValue);
               break;
            }
        
            case PROP_ID_inspectionTypeId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_inspectionTypeId));
               }
               setInspectionTypeId(typedValue);
               break;
            }
        
            case PROP_ID_parameterId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_parameterId));
               }
               setParameterId(typedValue);
               break;
            }
        
            case PROP_ID_specMin:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_specMin));
               }
               setSpecMin(typedValue);
               break;
            }
        
            case PROP_ID_specMax:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_specMax));
               }
               setSpecMax(typedValue);
               break;
            }
        
            case PROP_ID_subgroupSize:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_subgroupSize));
               }
               setSubgroupSize(typedValue);
               break;
            }
        
            case PROP_ID_samplingFrequency:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_samplingFrequency));
               }
               setSamplingFrequency(typedValue);
               break;
            }
        
            case PROP_ID_clCenterType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_clCenterType));
               }
               setClCenterType(typedValue);
               break;
            }
        
            case PROP_ID_ruleSet:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_ruleSet));
               }
               setRuleSet(typedValue);
               break;
            }
        
            case PROP_ID_alarmThreshold:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_alarmThreshold));
               }
               setAlarmThreshold(typedValue);
               break;
            }
        
            case PROP_ID_ucl:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_ucl));
               }
               setUcl(typedValue);
               break;
            }
        
            case PROP_ID_lcl:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_lcl));
               }
               setLcl(typedValue);
               break;
            }
        
            case PROP_ID_cl:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_cl));
               }
               setCl(typedValue);
               break;
            }
        
            case PROP_ID_calcStatus:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_calcStatus));
               }
               setCalcStatus(typedValue);
               break;
            }
        
            case PROP_ID_isActive:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isActive));
               }
               setIsActive(typedValue);
               break;
            }
        
            case PROP_ID_docStatus:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_docStatus));
               }
               setDocStatus(typedValue);
               break;
            }
        
            case PROP_ID_approveStatus:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_approveStatus));
               }
               setApproveStatus(typedValue);
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
        
            case PROP_ID_code:{
               onInitProp(propId);
               this._code = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_name:{
               onInitProp(propId);
               this._name = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_chartType:{
               onInitProp(propId);
               this._chartType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_materialId:{
               onInitProp(propId);
               this._materialId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_inspectionTypeId:{
               onInitProp(propId);
               this._inspectionTypeId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_parameterId:{
               onInitProp(propId);
               this._parameterId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_specMin:{
               onInitProp(propId);
               this._specMin = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_specMax:{
               onInitProp(propId);
               this._specMax = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_subgroupSize:{
               onInitProp(propId);
               this._subgroupSize = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_samplingFrequency:{
               onInitProp(propId);
               this._samplingFrequency = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_clCenterType:{
               onInitProp(propId);
               this._clCenterType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_ruleSet:{
               onInitProp(propId);
               this._ruleSet = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_alarmThreshold:{
               onInitProp(propId);
               this._alarmThreshold = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_ucl:{
               onInitProp(propId);
               this._ucl = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_lcl:{
               onInitProp(propId);
               this._lcl = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_cl:{
               onInitProp(propId);
               this._cl = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_calcStatus:{
               onInitProp(propId);
               this._calcStatus = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_isActive:{
               onInitProp(propId);
               this._isActive = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_docStatus:{
               onInitProp(propId);
               this._docStatus = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_approveStatus:{
               onInitProp(propId);
               this._approveStatus = (java.lang.String)value;
               
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
     * 编码: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * 编码: CODE
     */
    public final void setCode(java.lang.String value){
        if(onPropSet(PROP_ID_code,value)){
            this._code = value;
            internalClearRefs(PROP_ID_code);
            
        }
    }
    
    /**
     * 名称: NAME
     */
    public final java.lang.String getName(){
         onPropGet(PROP_ID_name);
         return _name;
    }

    /**
     * 名称: NAME
     */
    public final void setName(java.lang.String value){
        if(onPropSet(PROP_ID_name,value)){
            this._name = value;
            internalClearRefs(PROP_ID_name);
            
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
     * 图类型: CHART_TYPE
     */
    public final java.lang.String getChartType(){
         onPropGet(PROP_ID_chartType);
         return _chartType;
    }

    /**
     * 图类型: CHART_TYPE
     */
    public final void setChartType(java.lang.String value){
        if(onPropSet(PROP_ID_chartType,value)){
            this._chartType = value;
            internalClearRefs(PROP_ID_chartType);
            
        }
    }
    
    /**
     * 物料: MATERIAL_ID
     */
    public final java.lang.Long getMaterialId(){
         onPropGet(PROP_ID_materialId);
         return _materialId;
    }

    /**
     * 物料: MATERIAL_ID
     */
    public final void setMaterialId(java.lang.Long value){
        if(onPropSet(PROP_ID_materialId,value)){
            this._materialId = value;
            internalClearRefs(PROP_ID_materialId);
            
        }
    }
    
    /**
     * 质检模板: INSPECTION_TYPE_ID
     */
    public final java.lang.Long getInspectionTypeId(){
         onPropGet(PROP_ID_inspectionTypeId);
         return _inspectionTypeId;
    }

    /**
     * 质检模板: INSPECTION_TYPE_ID
     */
    public final void setInspectionTypeId(java.lang.Long value){
        if(onPropSet(PROP_ID_inspectionTypeId,value)){
            this._inspectionTypeId = value;
            internalClearRefs(PROP_ID_inspectionTypeId);
            
        }
    }
    
    /**
     * 关键检验参数: PARAMETER_ID
     */
    public final java.lang.Long getParameterId(){
         onPropGet(PROP_ID_parameterId);
         return _parameterId;
    }

    /**
     * 关键检验参数: PARAMETER_ID
     */
    public final void setParameterId(java.lang.Long value){
        if(onPropSet(PROP_ID_parameterId,value)){
            this._parameterId = value;
            internalClearRefs(PROP_ID_parameterId);
            
        }
    }
    
    /**
     * 规格下限: SPEC_MIN
     */
    public final java.math.BigDecimal getSpecMin(){
         onPropGet(PROP_ID_specMin);
         return _specMin;
    }

    /**
     * 规格下限: SPEC_MIN
     */
    public final void setSpecMin(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_specMin,value)){
            this._specMin = value;
            internalClearRefs(PROP_ID_specMin);
            
        }
    }
    
    /**
     * 规格上限: SPEC_MAX
     */
    public final java.math.BigDecimal getSpecMax(){
         onPropGet(PROP_ID_specMax);
         return _specMax;
    }

    /**
     * 规格上限: SPEC_MAX
     */
    public final void setSpecMax(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_specMax,value)){
            this._specMax = value;
            internalClearRefs(PROP_ID_specMax);
            
        }
    }
    
    /**
     * 子组样本量 n: SUBGROUP_SIZE
     */
    public final java.lang.Integer getSubgroupSize(){
         onPropGet(PROP_ID_subgroupSize);
         return _subgroupSize;
    }

    /**
     * 子组样本量 n: SUBGROUP_SIZE
     */
    public final void setSubgroupSize(java.lang.Integer value){
        if(onPropSet(PROP_ID_subgroupSize,value)){
            this._subgroupSize = value;
            internalClearRefs(PROP_ID_subgroupSize);
            
        }
    }
    
    /**
     * 采样频率: SAMPLING_FREQUENCY
     */
    public final java.lang.String getSamplingFrequency(){
         onPropGet(PROP_ID_samplingFrequency);
         return _samplingFrequency;
    }

    /**
     * 采样频率: SAMPLING_FREQUENCY
     */
    public final void setSamplingFrequency(java.lang.String value){
        if(onPropSet(PROP_ID_samplingFrequency,value)){
            this._samplingFrequency = value;
            internalClearRefs(PROP_ID_samplingFrequency);
            
        }
    }
    
    /**
     * 中心线计算方式: CL_CENTER_TYPE
     */
    public final java.lang.String getClCenterType(){
         onPropGet(PROP_ID_clCenterType);
         return _clCenterType;
    }

    /**
     * 中心线计算方式: CL_CENTER_TYPE
     */
    public final void setClCenterType(java.lang.String value){
        if(onPropSet(PROP_ID_clCenterType,value)){
            this._clCenterType = value;
            internalClearRefs(PROP_ID_clCenterType);
            
        }
    }
    
    /**
     * 判异规则集(逗号分隔): RULE_SET
     */
    public final java.lang.String getRuleSet(){
         onPropGet(PROP_ID_ruleSet);
         return _ruleSet;
    }

    /**
     * 判异规则集(逗号分隔): RULE_SET
     */
    public final void setRuleSet(java.lang.String value){
        if(onPropSet(PROP_ID_ruleSet,value)){
            this._ruleSet = value;
            internalClearRefs(PROP_ID_ruleSet);
            
        }
    }
    
    /**
     * 预警阈值(违规次数): ALARM_THRESHOLD
     */
    public final java.lang.Integer getAlarmThreshold(){
         onPropGet(PROP_ID_alarmThreshold);
         return _alarmThreshold;
    }

    /**
     * 预警阈值(违规次数): ALARM_THRESHOLD
     */
    public final void setAlarmThreshold(java.lang.Integer value){
        if(onPropSet(PROP_ID_alarmThreshold,value)){
            this._alarmThreshold = value;
            internalClearRefs(PROP_ID_alarmThreshold);
            
        }
    }
    
    /**
     * 控制上限(UCL): UCL
     */
    public final java.math.BigDecimal getUcl(){
         onPropGet(PROP_ID_ucl);
         return _ucl;
    }

    /**
     * 控制上限(UCL): UCL
     */
    public final void setUcl(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_ucl,value)){
            this._ucl = value;
            internalClearRefs(PROP_ID_ucl);
            
        }
    }
    
    /**
     * 控制下限(LCL): LCL
     */
    public final java.math.BigDecimal getLcl(){
         onPropGet(PROP_ID_lcl);
         return _lcl;
    }

    /**
     * 控制下限(LCL): LCL
     */
    public final void setLcl(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_lcl,value)){
            this._lcl = value;
            internalClearRefs(PROP_ID_lcl);
            
        }
    }
    
    /**
     * 中心线(CL): CL
     */
    public final java.math.BigDecimal getCl(){
         onPropGet(PROP_ID_cl);
         return _cl;
    }

    /**
     * 中心线(CL): CL
     */
    public final void setCl(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_cl,value)){
            this._cl = value;
            internalClearRefs(PROP_ID_cl);
            
        }
    }
    
    /**
     * 计算状态: CALC_STATUS
     */
    public final java.lang.String getCalcStatus(){
         onPropGet(PROP_ID_calcStatus);
         return _calcStatus;
    }

    /**
     * 计算状态: CALC_STATUS
     */
    public final void setCalcStatus(java.lang.String value){
        if(onPropSet(PROP_ID_calcStatus,value)){
            this._calcStatus = value;
            internalClearRefs(PROP_ID_calcStatus);
            
        }
    }
    
    /**
     * 是否启用: IS_ACTIVE
     */
    public final java.lang.Boolean getIsActive(){
         onPropGet(PROP_ID_isActive);
         return _isActive;
    }

    /**
     * 是否启用: IS_ACTIVE
     */
    public final void setIsActive(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isActive,value)){
            this._isActive = value;
            internalClearRefs(PROP_ID_isActive);
            
        }
    }
    
    /**
     * 单据状态: DOC_STATUS
     */
    public final java.lang.String getDocStatus(){
         onPropGet(PROP_ID_docStatus);
         return _docStatus;
    }

    /**
     * 单据状态: DOC_STATUS
     */
    public final void setDocStatus(java.lang.String value){
        if(onPropSet(PROP_ID_docStatus,value)){
            this._docStatus = value;
            internalClearRefs(PROP_ID_docStatus);
            
        }
    }
    
    /**
     * 审核状态: APPROVE_STATUS
     */
    public final java.lang.String getApproveStatus(){
         onPropGet(PROP_ID_approveStatus);
         return _approveStatus;
    }

    /**
     * 审核状态: APPROVE_STATUS
     */
    public final void setApproveStatus(java.lang.String value){
        if(onPropSet(PROP_ID_approveStatus,value)){
            this._approveStatus = value;
            internalClearRefs(PROP_ID_approveStatus);
            
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
    public final app.erp.md.dao.entity.ErpMdMaterial getMaterial(){
       return (app.erp.md.dao.entity.ErpMdMaterial)internalGetRefEntity(PROP_NAME_material);
    }

    public final void setMaterial(app.erp.md.dao.entity.ErpMdMaterial refEntity){
   
           if(refEntity == null){
           
                   this.setMaterialId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_material, refEntity,()->{
           
                           this.setMaterialId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.qa.dao.entity.ErpQaInspectionTemplate getInspectionType(){
       return (app.erp.qa.dao.entity.ErpQaInspectionTemplate)internalGetRefEntity(PROP_NAME_inspectionType);
    }

    public final void setInspectionType(app.erp.qa.dao.entity.ErpQaInspectionTemplate refEntity){
   
           if(refEntity == null){
           
                   this.setInspectionTypeId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_inspectionType, refEntity,()->{
           
                           this.setInspectionTypeId(refEntity.getId());
                       
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
       
    private final OrmEntitySet<app.erp.qa.dao.entity.ErpQaSpcSample> _samples = new OrmEntitySet<>(this, PROP_NAME_samples,
        null, null,app.erp.qa.dao.entity.ErpQaSpcSample.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.qa.dao.entity.ErpQaSpcSample> getSamples(){
       return _samples;
    }
       
}
// resume CPD analysis - CPD-ON
