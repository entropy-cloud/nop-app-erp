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

import app.erp.qa.dao.entity.ErpQaSpcSample;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  SPC 样本数据: erp_qa_spc_sample
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpQaSpcSample extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 控制图ID: CHART_ID BIGINT */
    public static final String PROP_NAME_chartId = "chartId";
    public static final int PROP_ID_chartId = 2;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 子组序号: SUBGROUP_NO INTEGER */
    public static final String PROP_NAME_subgroupNo = "subgroupNo";
    public static final int PROP_ID_subgroupNo = 4;
    
    /* 采样时间: SAMPLE_TIME DATETIME */
    public static final String PROP_NAME_sampleTime = "sampleTime";
    public static final int PROP_ID_sampleTime = 5;
    
    /* 子组实测值(JSON 数组): MEASURED_VALUES VARCHAR */
    public static final String PROP_NAME_measuredValues = "measuredValues";
    public static final int PROP_ID_measuredValues = 6;
    
    /* 子组均值 X̄: MEAN DECIMAL */
    public static final String PROP_NAME_mean = "mean";
    public static final int PROP_ID_mean = 7;
    
    /* 子组极差 R: RANGE DECIMAL */
    public static final String PROP_NAME_range = "range";
    public static final int PROP_ID_range = 8;
    
    /* 子组标准差 s: STD_DEV DECIMAL */
    public static final String PROP_NAME_stdDev = "stdDev";
    public static final int PROP_ID_stdDev = 9;
    
    /* 数据来源单据类型: SOURCE_BILL_TYPE VARCHAR */
    public static final String PROP_NAME_sourceBillType = "sourceBillType";
    public static final int PROP_ID_sourceBillType = 10;
    
    /* 数据来源单号: SOURCE_CODE VARCHAR */
    public static final String PROP_NAME_sourceCode = "sourceCode";
    public static final int PROP_ID_sourceCode = 11;
    
    /* 数据来源行号: SOURCE_LINE_CODE VARCHAR */
    public static final String PROP_NAME_sourceLineCode = "sourceLineCode";
    public static final int PROP_ID_sourceLineCode = 12;
    
    /* 检验员: INSPECTOR_ID BIGINT */
    public static final String PROP_NAME_inspectorId = "inspectorId";
    public static final int PROP_ID_inspectorId = 13;
    
    /* 违反的判异规则(逗号分隔): VIOLATED_RULES VARCHAR */
    public static final String PROP_NAME_violatedRules = "violatedRules";
    public static final int PROP_ID_violatedRules = 14;
    
    /* 是否失控: IS_OUT_OF_CONTROL BOOLEAN */
    public static final String PROP_NAME_isOutOfControl = "isOutOfControl";
    public static final int PROP_ID_isOutOfControl = 15;
    
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
    public static final String PROP_NAME_chart = "chart";
    
    /* relation:  */
    public static final String PROP_NAME_inspector = "inspector";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[23];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_chartId] = PROP_NAME_chartId;
          PROP_NAME_TO_ID.put(PROP_NAME_chartId, PROP_ID_chartId);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_subgroupNo] = PROP_NAME_subgroupNo;
          PROP_NAME_TO_ID.put(PROP_NAME_subgroupNo, PROP_ID_subgroupNo);
      
          PROP_ID_TO_NAME[PROP_ID_sampleTime] = PROP_NAME_sampleTime;
          PROP_NAME_TO_ID.put(PROP_NAME_sampleTime, PROP_ID_sampleTime);
      
          PROP_ID_TO_NAME[PROP_ID_measuredValues] = PROP_NAME_measuredValues;
          PROP_NAME_TO_ID.put(PROP_NAME_measuredValues, PROP_ID_measuredValues);
      
          PROP_ID_TO_NAME[PROP_ID_mean] = PROP_NAME_mean;
          PROP_NAME_TO_ID.put(PROP_NAME_mean, PROP_ID_mean);
      
          PROP_ID_TO_NAME[PROP_ID_range] = PROP_NAME_range;
          PROP_NAME_TO_ID.put(PROP_NAME_range, PROP_ID_range);
      
          PROP_ID_TO_NAME[PROP_ID_stdDev] = PROP_NAME_stdDev;
          PROP_NAME_TO_ID.put(PROP_NAME_stdDev, PROP_ID_stdDev);
      
          PROP_ID_TO_NAME[PROP_ID_sourceBillType] = PROP_NAME_sourceBillType;
          PROP_NAME_TO_ID.put(PROP_NAME_sourceBillType, PROP_ID_sourceBillType);
      
          PROP_ID_TO_NAME[PROP_ID_sourceCode] = PROP_NAME_sourceCode;
          PROP_NAME_TO_ID.put(PROP_NAME_sourceCode, PROP_ID_sourceCode);
      
          PROP_ID_TO_NAME[PROP_ID_sourceLineCode] = PROP_NAME_sourceLineCode;
          PROP_NAME_TO_ID.put(PROP_NAME_sourceLineCode, PROP_ID_sourceLineCode);
      
          PROP_ID_TO_NAME[PROP_ID_inspectorId] = PROP_NAME_inspectorId;
          PROP_NAME_TO_ID.put(PROP_NAME_inspectorId, PROP_ID_inspectorId);
      
          PROP_ID_TO_NAME[PROP_ID_violatedRules] = PROP_NAME_violatedRules;
          PROP_NAME_TO_ID.put(PROP_NAME_violatedRules, PROP_ID_violatedRules);
      
          PROP_ID_TO_NAME[PROP_ID_isOutOfControl] = PROP_NAME_isOutOfControl;
          PROP_NAME_TO_ID.put(PROP_NAME_isOutOfControl, PROP_ID_isOutOfControl);
      
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
    
    /* 子组序号: SUBGROUP_NO */
    private java.lang.Integer _subgroupNo;
    
    /* 采样时间: SAMPLE_TIME */
    private java.time.LocalDateTime _sampleTime;
    
    /* 子组实测值(JSON 数组): MEASURED_VALUES */
    private java.lang.String _measuredValues;
    
    /* 子组均值 X̄: MEAN */
    private java.math.BigDecimal _mean;
    
    /* 子组极差 R: RANGE */
    private java.math.BigDecimal _range;
    
    /* 子组标准差 s: STD_DEV */
    private java.math.BigDecimal _stdDev;
    
    /* 数据来源单据类型: SOURCE_BILL_TYPE */
    private java.lang.String _sourceBillType;
    
    /* 数据来源单号: SOURCE_CODE */
    private java.lang.String _sourceCode;
    
    /* 数据来源行号: SOURCE_LINE_CODE */
    private java.lang.String _sourceLineCode;
    
    /* 检验员: INSPECTOR_ID */
    private java.lang.Long _inspectorId;
    
    /* 违反的判异规则(逗号分隔): VIOLATED_RULES */
    private java.lang.String _violatedRules;
    
    /* 是否失控: IS_OUT_OF_CONTROL */
    private java.lang.Boolean _isOutOfControl;
    
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
    

    public _ErpQaSpcSample(){
        // for debug
    }

    protected ErpQaSpcSample newInstance(){
        ErpQaSpcSample entity = new ErpQaSpcSample();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpQaSpcSample cloneInstance() {
        ErpQaSpcSample entity = newInstance();
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
      return "app.erp.qa.dao.entity.ErpQaSpcSample";
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
        
            case PROP_ID_subgroupNo:
               return getSubgroupNo();
        
            case PROP_ID_sampleTime:
               return getSampleTime();
        
            case PROP_ID_measuredValues:
               return getMeasuredValues();
        
            case PROP_ID_mean:
               return getMean();
        
            case PROP_ID_range:
               return getRange();
        
            case PROP_ID_stdDev:
               return getStdDev();
        
            case PROP_ID_sourceBillType:
               return getSourceBillType();
        
            case PROP_ID_sourceCode:
               return getSourceCode();
        
            case PROP_ID_sourceLineCode:
               return getSourceLineCode();
        
            case PROP_ID_inspectorId:
               return getInspectorId();
        
            case PROP_ID_violatedRules:
               return getViolatedRules();
        
            case PROP_ID_isOutOfControl:
               return getIsOutOfControl();
        
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
        
            case PROP_ID_subgroupNo:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_subgroupNo));
               }
               setSubgroupNo(typedValue);
               break;
            }
        
            case PROP_ID_sampleTime:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_sampleTime));
               }
               setSampleTime(typedValue);
               break;
            }
        
            case PROP_ID_measuredValues:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_measuredValues));
               }
               setMeasuredValues(typedValue);
               break;
            }
        
            case PROP_ID_mean:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_mean));
               }
               setMean(typedValue);
               break;
            }
        
            case PROP_ID_range:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_range));
               }
               setRange(typedValue);
               break;
            }
        
            case PROP_ID_stdDev:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_stdDev));
               }
               setStdDev(typedValue);
               break;
            }
        
            case PROP_ID_sourceBillType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_sourceBillType));
               }
               setSourceBillType(typedValue);
               break;
            }
        
            case PROP_ID_sourceCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_sourceCode));
               }
               setSourceCode(typedValue);
               break;
            }
        
            case PROP_ID_sourceLineCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_sourceLineCode));
               }
               setSourceLineCode(typedValue);
               break;
            }
        
            case PROP_ID_inspectorId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_inspectorId));
               }
               setInspectorId(typedValue);
               break;
            }
        
            case PROP_ID_violatedRules:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_violatedRules));
               }
               setViolatedRules(typedValue);
               break;
            }
        
            case PROP_ID_isOutOfControl:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isOutOfControl));
               }
               setIsOutOfControl(typedValue);
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
        
            case PROP_ID_subgroupNo:{
               onInitProp(propId);
               this._subgroupNo = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_sampleTime:{
               onInitProp(propId);
               this._sampleTime = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_measuredValues:{
               onInitProp(propId);
               this._measuredValues = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_mean:{
               onInitProp(propId);
               this._mean = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_range:{
               onInitProp(propId);
               this._range = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_stdDev:{
               onInitProp(propId);
               this._stdDev = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_sourceBillType:{
               onInitProp(propId);
               this._sourceBillType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_sourceCode:{
               onInitProp(propId);
               this._sourceCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_sourceLineCode:{
               onInitProp(propId);
               this._sourceLineCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_inspectorId:{
               onInitProp(propId);
               this._inspectorId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_violatedRules:{
               onInitProp(propId);
               this._violatedRules = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_isOutOfControl:{
               onInitProp(propId);
               this._isOutOfControl = (java.lang.Boolean)value;
               
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
     * 子组序号: SUBGROUP_NO
     */
    public final java.lang.Integer getSubgroupNo(){
         onPropGet(PROP_ID_subgroupNo);
         return _subgroupNo;
    }

    /**
     * 子组序号: SUBGROUP_NO
     */
    public final void setSubgroupNo(java.lang.Integer value){
        if(onPropSet(PROP_ID_subgroupNo,value)){
            this._subgroupNo = value;
            internalClearRefs(PROP_ID_subgroupNo);
            
        }
    }
    
    /**
     * 采样时间: SAMPLE_TIME
     */
    public final java.time.LocalDateTime getSampleTime(){
         onPropGet(PROP_ID_sampleTime);
         return _sampleTime;
    }

    /**
     * 采样时间: SAMPLE_TIME
     */
    public final void setSampleTime(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_sampleTime,value)){
            this._sampleTime = value;
            internalClearRefs(PROP_ID_sampleTime);
            
        }
    }
    
    /**
     * 子组实测值(JSON 数组): MEASURED_VALUES
     */
    public final java.lang.String getMeasuredValues(){
         onPropGet(PROP_ID_measuredValues);
         return _measuredValues;
    }

    /**
     * 子组实测值(JSON 数组): MEASURED_VALUES
     */
    public final void setMeasuredValues(java.lang.String value){
        if(onPropSet(PROP_ID_measuredValues,value)){
            this._measuredValues = value;
            internalClearRefs(PROP_ID_measuredValues);
            
        }
    }
    
    /**
     * 子组均值 X̄: MEAN
     */
    public final java.math.BigDecimal getMean(){
         onPropGet(PROP_ID_mean);
         return _mean;
    }

    /**
     * 子组均值 X̄: MEAN
     */
    public final void setMean(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_mean,value)){
            this._mean = value;
            internalClearRefs(PROP_ID_mean);
            
        }
    }
    
    /**
     * 子组极差 R: RANGE
     */
    public final java.math.BigDecimal getRange(){
         onPropGet(PROP_ID_range);
         return _range;
    }

    /**
     * 子组极差 R: RANGE
     */
    public final void setRange(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_range,value)){
            this._range = value;
            internalClearRefs(PROP_ID_range);
            
        }
    }
    
    /**
     * 子组标准差 s: STD_DEV
     */
    public final java.math.BigDecimal getStdDev(){
         onPropGet(PROP_ID_stdDev);
         return _stdDev;
    }

    /**
     * 子组标准差 s: STD_DEV
     */
    public final void setStdDev(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_stdDev,value)){
            this._stdDev = value;
            internalClearRefs(PROP_ID_stdDev);
            
        }
    }
    
    /**
     * 数据来源单据类型: SOURCE_BILL_TYPE
     */
    public final java.lang.String getSourceBillType(){
         onPropGet(PROP_ID_sourceBillType);
         return _sourceBillType;
    }

    /**
     * 数据来源单据类型: SOURCE_BILL_TYPE
     */
    public final void setSourceBillType(java.lang.String value){
        if(onPropSet(PROP_ID_sourceBillType,value)){
            this._sourceBillType = value;
            internalClearRefs(PROP_ID_sourceBillType);
            
        }
    }
    
    /**
     * 数据来源单号: SOURCE_CODE
     */
    public final java.lang.String getSourceCode(){
         onPropGet(PROP_ID_sourceCode);
         return _sourceCode;
    }

    /**
     * 数据来源单号: SOURCE_CODE
     */
    public final void setSourceCode(java.lang.String value){
        if(onPropSet(PROP_ID_sourceCode,value)){
            this._sourceCode = value;
            internalClearRefs(PROP_ID_sourceCode);
            
        }
    }
    
    /**
     * 数据来源行号: SOURCE_LINE_CODE
     */
    public final java.lang.String getSourceLineCode(){
         onPropGet(PROP_ID_sourceLineCode);
         return _sourceLineCode;
    }

    /**
     * 数据来源行号: SOURCE_LINE_CODE
     */
    public final void setSourceLineCode(java.lang.String value){
        if(onPropSet(PROP_ID_sourceLineCode,value)){
            this._sourceLineCode = value;
            internalClearRefs(PROP_ID_sourceLineCode);
            
        }
    }
    
    /**
     * 检验员: INSPECTOR_ID
     */
    public final java.lang.Long getInspectorId(){
         onPropGet(PROP_ID_inspectorId);
         return _inspectorId;
    }

    /**
     * 检验员: INSPECTOR_ID
     */
    public final void setInspectorId(java.lang.Long value){
        if(onPropSet(PROP_ID_inspectorId,value)){
            this._inspectorId = value;
            internalClearRefs(PROP_ID_inspectorId);
            
        }
    }
    
    /**
     * 违反的判异规则(逗号分隔): VIOLATED_RULES
     */
    public final java.lang.String getViolatedRules(){
         onPropGet(PROP_ID_violatedRules);
         return _violatedRules;
    }

    /**
     * 违反的判异规则(逗号分隔): VIOLATED_RULES
     */
    public final void setViolatedRules(java.lang.String value){
        if(onPropSet(PROP_ID_violatedRules,value)){
            this._violatedRules = value;
            internalClearRefs(PROP_ID_violatedRules);
            
        }
    }
    
    /**
     * 是否失控: IS_OUT_OF_CONTROL
     */
    public final java.lang.Boolean getIsOutOfControl(){
         onPropGet(PROP_ID_isOutOfControl);
         return _isOutOfControl;
    }

    /**
     * 是否失控: IS_OUT_OF_CONTROL
     */
    public final void setIsOutOfControl(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isOutOfControl,value)){
            this._isOutOfControl = value;
            internalClearRefs(PROP_ID_isOutOfControl);
            
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
    public final app.erp.md.dao.entity.ErpMdEmployee getInspector(){
       return (app.erp.md.dao.entity.ErpMdEmployee)internalGetRefEntity(PROP_NAME_inspector);
    }

    public final void setInspector(app.erp.md.dao.entity.ErpMdEmployee refEntity){
   
           if(refEntity == null){
           
                   this.setInspectorId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_inspector, refEntity,()->{
           
                           this.setInspectorId(refEntity.getId());
                       
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
