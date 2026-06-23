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

import app.erp.qa.dao.entity.ErpQaCalibration;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  量具校准: erp_qa_calibration
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpQaCalibration extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 单号: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 量具/设备名称: INSTRUMENT_NAME VARCHAR */
    public static final String PROP_NAME_instrumentName = "instrumentName";
    public static final int PROP_ID_instrumentName = 4;
    
    /* 量具编号: INSTRUMENT_CODE VARCHAR */
    public static final String PROP_NAME_instrumentCode = "instrumentCode";
    public static final int PROP_ID_instrumentCode = 5;
    
    /* 校准日期: BUSINESS_DATE DATE */
    public static final String PROP_NAME_businessDate = "businessDate";
    public static final int PROP_ID_businessDate = 6;
    
    /* 参考标准: STANDARD_REF VARCHAR */
    public static final String PROP_NAME_standardRef = "standardRef";
    public static final int PROP_ID_standardRef = 7;
    
    /* 测量值: MEASURED_VALUE DECIMAL */
    public static final String PROP_NAME_measuredValue = "measuredValue";
    public static final int PROP_ID_measuredValue = 8;
    
    /* 目标值: TARGET_VALUE DECIMAL */
    public static final String PROP_NAME_targetValue = "targetValue";
    public static final int PROP_ID_targetValue = 9;
    
    /* 允差: TOLERANCE DECIMAL */
    public static final String PROP_NAME_tolerance = "tolerance";
    public static final int PROP_ID_tolerance = 10;
    
    /* 校准结果: RESULT INTEGER */
    public static final String PROP_NAME_result = "result";
    public static final int PROP_ID_result = 11;
    
    /* 下次校准日期: NEXT_CALIBRATION_DATE DATE */
    public static final String PROP_NAME_nextCalibrationDate = "nextCalibrationDate";
    public static final int PROP_ID_nextCalibrationDate = 12;
    
    /* 校准人(职员): CALIBRATED_BY BIGINT */
    public static final String PROP_NAME_calibratedBy = "calibratedBy";
    public static final int PROP_ID_calibratedBy = 13;
    
    /* 单据状态: DOC_STATUS INTEGER */
    public static final String PROP_NAME_docStatus = "docStatus";
    public static final int PROP_ID_docStatus = 14;
    
    /* 审核状态: APPROVE_STATUS INTEGER */
    public static final String PROP_NAME_approveStatus = "approveStatus";
    public static final int PROP_ID_approveStatus = 15;
    
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
    public static final String PROP_NAME_calibratedByEmployee = "calibratedByEmployee";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[23];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_instrumentName] = PROP_NAME_instrumentName;
          PROP_NAME_TO_ID.put(PROP_NAME_instrumentName, PROP_ID_instrumentName);
      
          PROP_ID_TO_NAME[PROP_ID_instrumentCode] = PROP_NAME_instrumentCode;
          PROP_NAME_TO_ID.put(PROP_NAME_instrumentCode, PROP_ID_instrumentCode);
      
          PROP_ID_TO_NAME[PROP_ID_businessDate] = PROP_NAME_businessDate;
          PROP_NAME_TO_ID.put(PROP_NAME_businessDate, PROP_ID_businessDate);
      
          PROP_ID_TO_NAME[PROP_ID_standardRef] = PROP_NAME_standardRef;
          PROP_NAME_TO_ID.put(PROP_NAME_standardRef, PROP_ID_standardRef);
      
          PROP_ID_TO_NAME[PROP_ID_measuredValue] = PROP_NAME_measuredValue;
          PROP_NAME_TO_ID.put(PROP_NAME_measuredValue, PROP_ID_measuredValue);
      
          PROP_ID_TO_NAME[PROP_ID_targetValue] = PROP_NAME_targetValue;
          PROP_NAME_TO_ID.put(PROP_NAME_targetValue, PROP_ID_targetValue);
      
          PROP_ID_TO_NAME[PROP_ID_tolerance] = PROP_NAME_tolerance;
          PROP_NAME_TO_ID.put(PROP_NAME_tolerance, PROP_ID_tolerance);
      
          PROP_ID_TO_NAME[PROP_ID_result] = PROP_NAME_result;
          PROP_NAME_TO_ID.put(PROP_NAME_result, PROP_ID_result);
      
          PROP_ID_TO_NAME[PROP_ID_nextCalibrationDate] = PROP_NAME_nextCalibrationDate;
          PROP_NAME_TO_ID.put(PROP_NAME_nextCalibrationDate, PROP_ID_nextCalibrationDate);
      
          PROP_ID_TO_NAME[PROP_ID_calibratedBy] = PROP_NAME_calibratedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_calibratedBy, PROP_ID_calibratedBy);
      
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
    
    /* 单号: CODE */
    private java.lang.String _code;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 量具/设备名称: INSTRUMENT_NAME */
    private java.lang.String _instrumentName;
    
    /* 量具编号: INSTRUMENT_CODE */
    private java.lang.String _instrumentCode;
    
    /* 校准日期: BUSINESS_DATE */
    private java.time.LocalDate _businessDate;
    
    /* 参考标准: STANDARD_REF */
    private java.lang.String _standardRef;
    
    /* 测量值: MEASURED_VALUE */
    private java.lang.String _measuredValue;
    
    /* 目标值: TARGET_VALUE */
    private java.lang.String _targetValue;
    
    /* 允差: TOLERANCE */
    private java.lang.String _tolerance;
    
    /* 校准结果: RESULT */
    private java.lang.Integer _result;
    
    /* 下次校准日期: NEXT_CALIBRATION_DATE */
    private java.time.LocalDate _nextCalibrationDate;
    
    /* 校准人(职员): CALIBRATED_BY */
    private java.lang.Long _calibratedBy;
    
    /* 单据状态: DOC_STATUS */
    private java.lang.Integer _docStatus;
    
    /* 审核状态: APPROVE_STATUS */
    private java.lang.Integer _approveStatus;
    
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
    

    public _ErpQaCalibration(){
        // for debug
    }

    protected ErpQaCalibration newInstance(){
        ErpQaCalibration entity = new ErpQaCalibration();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpQaCalibration cloneInstance() {
        ErpQaCalibration entity = newInstance();
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
      return "app.erp.qa.dao.entity.ErpQaCalibration";
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
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_instrumentName:
               return getInstrumentName();
        
            case PROP_ID_instrumentCode:
               return getInstrumentCode();
        
            case PROP_ID_businessDate:
               return getBusinessDate();
        
            case PROP_ID_standardRef:
               return getStandardRef();
        
            case PROP_ID_measuredValue:
               return getMeasuredValue();
        
            case PROP_ID_targetValue:
               return getTargetValue();
        
            case PROP_ID_tolerance:
               return getTolerance();
        
            case PROP_ID_result:
               return getResult();
        
            case PROP_ID_nextCalibrationDate:
               return getNextCalibrationDate();
        
            case PROP_ID_calibratedBy:
               return getCalibratedBy();
        
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
        
            case PROP_ID_orgId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_orgId));
               }
               setOrgId(typedValue);
               break;
            }
        
            case PROP_ID_instrumentName:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_instrumentName));
               }
               setInstrumentName(typedValue);
               break;
            }
        
            case PROP_ID_instrumentCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_instrumentCode));
               }
               setInstrumentCode(typedValue);
               break;
            }
        
            case PROP_ID_businessDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_businessDate));
               }
               setBusinessDate(typedValue);
               break;
            }
        
            case PROP_ID_standardRef:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_standardRef));
               }
               setStandardRef(typedValue);
               break;
            }
        
            case PROP_ID_measuredValue:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_measuredValue));
               }
               setMeasuredValue(typedValue);
               break;
            }
        
            case PROP_ID_targetValue:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_targetValue));
               }
               setTargetValue(typedValue);
               break;
            }
        
            case PROP_ID_tolerance:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_tolerance));
               }
               setTolerance(typedValue);
               break;
            }
        
            case PROP_ID_result:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_result));
               }
               setResult(typedValue);
               break;
            }
        
            case PROP_ID_nextCalibrationDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_nextCalibrationDate));
               }
               setNextCalibrationDate(typedValue);
               break;
            }
        
            case PROP_ID_calibratedBy:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_calibratedBy));
               }
               setCalibratedBy(typedValue);
               break;
            }
        
            case PROP_ID_docStatus:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_docStatus));
               }
               setDocStatus(typedValue);
               break;
            }
        
            case PROP_ID_approveStatus:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
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
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_instrumentName:{
               onInitProp(propId);
               this._instrumentName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_instrumentCode:{
               onInitProp(propId);
               this._instrumentCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_businessDate:{
               onInitProp(propId);
               this._businessDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_standardRef:{
               onInitProp(propId);
               this._standardRef = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_measuredValue:{
               onInitProp(propId);
               this._measuredValue = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_targetValue:{
               onInitProp(propId);
               this._targetValue = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_tolerance:{
               onInitProp(propId);
               this._tolerance = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_result:{
               onInitProp(propId);
               this._result = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_nextCalibrationDate:{
               onInitProp(propId);
               this._nextCalibrationDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_calibratedBy:{
               onInitProp(propId);
               this._calibratedBy = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_docStatus:{
               onInitProp(propId);
               this._docStatus = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_approveStatus:{
               onInitProp(propId);
               this._approveStatus = (java.lang.Integer)value;
               
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
     * 单号: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * 单号: CODE
     */
    public final void setCode(java.lang.String value){
        if(onPropSet(PROP_ID_code,value)){
            this._code = value;
            internalClearRefs(PROP_ID_code);
            
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
     * 量具/设备名称: INSTRUMENT_NAME
     */
    public final java.lang.String getInstrumentName(){
         onPropGet(PROP_ID_instrumentName);
         return _instrumentName;
    }

    /**
     * 量具/设备名称: INSTRUMENT_NAME
     */
    public final void setInstrumentName(java.lang.String value){
        if(onPropSet(PROP_ID_instrumentName,value)){
            this._instrumentName = value;
            internalClearRefs(PROP_ID_instrumentName);
            
        }
    }
    
    /**
     * 量具编号: INSTRUMENT_CODE
     */
    public final java.lang.String getInstrumentCode(){
         onPropGet(PROP_ID_instrumentCode);
         return _instrumentCode;
    }

    /**
     * 量具编号: INSTRUMENT_CODE
     */
    public final void setInstrumentCode(java.lang.String value){
        if(onPropSet(PROP_ID_instrumentCode,value)){
            this._instrumentCode = value;
            internalClearRefs(PROP_ID_instrumentCode);
            
        }
    }
    
    /**
     * 校准日期: BUSINESS_DATE
     */
    public final java.time.LocalDate getBusinessDate(){
         onPropGet(PROP_ID_businessDate);
         return _businessDate;
    }

    /**
     * 校准日期: BUSINESS_DATE
     */
    public final void setBusinessDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_businessDate,value)){
            this._businessDate = value;
            internalClearRefs(PROP_ID_businessDate);
            
        }
    }
    
    /**
     * 参考标准: STANDARD_REF
     */
    public final java.lang.String getStandardRef(){
         onPropGet(PROP_ID_standardRef);
         return _standardRef;
    }

    /**
     * 参考标准: STANDARD_REF
     */
    public final void setStandardRef(java.lang.String value){
        if(onPropSet(PROP_ID_standardRef,value)){
            this._standardRef = value;
            internalClearRefs(PROP_ID_standardRef);
            
        }
    }
    
    /**
     * 测量值: MEASURED_VALUE
     */
    public final java.lang.String getMeasuredValue(){
         onPropGet(PROP_ID_measuredValue);
         return _measuredValue;
    }

    /**
     * 测量值: MEASURED_VALUE
     */
    public final void setMeasuredValue(java.lang.String value){
        if(onPropSet(PROP_ID_measuredValue,value)){
            this._measuredValue = value;
            internalClearRefs(PROP_ID_measuredValue);
            
        }
    }
    
    /**
     * 目标值: TARGET_VALUE
     */
    public final java.lang.String getTargetValue(){
         onPropGet(PROP_ID_targetValue);
         return _targetValue;
    }

    /**
     * 目标值: TARGET_VALUE
     */
    public final void setTargetValue(java.lang.String value){
        if(onPropSet(PROP_ID_targetValue,value)){
            this._targetValue = value;
            internalClearRefs(PROP_ID_targetValue);
            
        }
    }
    
    /**
     * 允差: TOLERANCE
     */
    public final java.lang.String getTolerance(){
         onPropGet(PROP_ID_tolerance);
         return _tolerance;
    }

    /**
     * 允差: TOLERANCE
     */
    public final void setTolerance(java.lang.String value){
        if(onPropSet(PROP_ID_tolerance,value)){
            this._tolerance = value;
            internalClearRefs(PROP_ID_tolerance);
            
        }
    }
    
    /**
     * 校准结果: RESULT
     */
    public final java.lang.Integer getResult(){
         onPropGet(PROP_ID_result);
         return _result;
    }

    /**
     * 校准结果: RESULT
     */
    public final void setResult(java.lang.Integer value){
        if(onPropSet(PROP_ID_result,value)){
            this._result = value;
            internalClearRefs(PROP_ID_result);
            
        }
    }
    
    /**
     * 下次校准日期: NEXT_CALIBRATION_DATE
     */
    public final java.time.LocalDate getNextCalibrationDate(){
         onPropGet(PROP_ID_nextCalibrationDate);
         return _nextCalibrationDate;
    }

    /**
     * 下次校准日期: NEXT_CALIBRATION_DATE
     */
    public final void setNextCalibrationDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_nextCalibrationDate,value)){
            this._nextCalibrationDate = value;
            internalClearRefs(PROP_ID_nextCalibrationDate);
            
        }
    }
    
    /**
     * 校准人(职员): CALIBRATED_BY
     */
    public final java.lang.Long getCalibratedBy(){
         onPropGet(PROP_ID_calibratedBy);
         return _calibratedBy;
    }

    /**
     * 校准人(职员): CALIBRATED_BY
     */
    public final void setCalibratedBy(java.lang.Long value){
        if(onPropSet(PROP_ID_calibratedBy,value)){
            this._calibratedBy = value;
            internalClearRefs(PROP_ID_calibratedBy);
            
        }
    }
    
    /**
     * 单据状态: DOC_STATUS
     */
    public final java.lang.Integer getDocStatus(){
         onPropGet(PROP_ID_docStatus);
         return _docStatus;
    }

    /**
     * 单据状态: DOC_STATUS
     */
    public final void setDocStatus(java.lang.Integer value){
        if(onPropSet(PROP_ID_docStatus,value)){
            this._docStatus = value;
            internalClearRefs(PROP_ID_docStatus);
            
        }
    }
    
    /**
     * 审核状态: APPROVE_STATUS
     */
    public final java.lang.Integer getApproveStatus(){
         onPropGet(PROP_ID_approveStatus);
         return _approveStatus;
    }

    /**
     * 审核状态: APPROVE_STATUS
     */
    public final void setApproveStatus(java.lang.Integer value){
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
    public final app.erp.md.dao.entity.ErpMdEmployee getCalibratedByEmployee(){
       return (app.erp.md.dao.entity.ErpMdEmployee)internalGetRefEntity(PROP_NAME_calibratedByEmployee);
    }

    public final void setCalibratedByEmployee(app.erp.md.dao.entity.ErpMdEmployee refEntity){
   
           if(refEntity == null){
           
                   this.setCalibratedBy(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_calibratedByEmployee, refEntity,()->{
           
                           this.setCalibratedBy(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
