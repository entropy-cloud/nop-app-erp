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

import app.erp.qa.dao.entity.ErpQaNonConformance;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  不合格品报告: erp_qa_non_conformance
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpQaNonConformance extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 单号: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 报告日期: NCR_DATE DATE */
    public static final String PROP_NAME_ncrDate = "ncrDate";
    public static final int PROP_ID_ncrDate = 3;
    
    /* 来源类型: SOURCE_TYPE VARCHAR */
    public static final String PROP_NAME_sourceType = "sourceType";
    public static final int PROP_ID_sourceType = 4;
    
    /* 来源单号: SOURCE_CODE VARCHAR */
    public static final String PROP_NAME_sourceCode = "sourceCode";
    public static final int PROP_ID_sourceCode = 5;
    
    /* 物料: MATERIAL_ID BIGINT */
    public static final String PROP_NAME_materialId = "materialId";
    public static final int PROP_ID_materialId = 6;
    
    /* 质检单ID: INSPECTION_ID BIGINT */
    public static final String PROP_NAME_inspectionId = "inspectionId";
    public static final int PROP_ID_inspectionId = 7;
    
    /* 不合格数量: QUANTITY DECIMAL */
    public static final String PROP_NAME_quantity = "quantity";
    public static final int PROP_ID_quantity = 8;
    
    /* 问题描述: DESCRIPTION VARCHAR */
    public static final String PROP_NAME_description = "description";
    public static final int PROP_ID_description = 9;
    
    /* 严重程度: SEVERITY VARCHAR */
    public static final String PROP_NAME_severity = "severity";
    public static final int PROP_ID_severity = 10;
    
    /* 处理决定: DISPOSITION_TYPE VARCHAR */
    public static final String PROP_NAME_dispositionType = "dispositionType";
    public static final int PROP_ID_dispositionType = 11;
    
    /* 状态: STATUS VARCHAR */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 12;
    
    /* 供应商: SUPPLIER_ID BIGINT */
    public static final String PROP_NAME_supplierId = "supplierId";
    public static final int PROP_ID_supplierId = 13;
    
    /* 不合格参数: PARAMETER_NAME VARCHAR */
    public static final String PROP_NAME_parameterName = "parameterName";
    public static final int PROP_ID_parameterName = 14;
    
    /* 实测值: MEASURED_VALUE VARCHAR */
    public static final String PROP_NAME_measuredValue = "measuredValue";
    public static final int PROP_ID_measuredValue = 15;
    
    /* 规格下限: SPEC_MIN DECIMAL */
    public static final String PROP_NAME_specMin = "specMin";
    public static final int PROP_ID_specMin = 16;
    
    /* 规格上限: SPEC_MAX DECIMAL */
    public static final String PROP_NAME_specMax = "specMax";
    public static final int PROP_ID_specMax = 17;
    
    /* 责任人: ASSIGNED_TO BIGINT */
    public static final String PROP_NAME_assignedTo = "assignedTo";
    public static final int PROP_ID_assignedTo = 18;
    
    /* 解决人: RESOLVED_BY BIGINT */
    public static final String PROP_NAME_resolvedBy = "resolvedBy";
    public static final int PROP_ID_resolvedBy = 19;
    
    /* 解决时间: RESOLVED_AT DATETIME */
    public static final String PROP_NAME_resolvedAt = "resolvedAt";
    public static final int PROP_ID_resolvedAt = 20;
    
    /* 解决措施: RESOLUTION VARCHAR */
    public static final String PROP_NAME_resolution = "resolution";
    public static final int PROP_ID_resolution = 21;
    
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
    
    /* 已过账(报废处置已生成凭证): POSTED BOOLEAN */
    public static final String PROP_NAME_posted = "posted";
    public static final int PROP_ID_posted = 30;
    
    /* 过账时间: POSTED_AT DATETIME */
    public static final String PROP_NAME_postedAt = "postedAt";
    public static final int PROP_ID_postedAt = 31;
    
    /* 过账人: POSTED_BY VARCHAR */
    public static final String PROP_NAME_postedBy = "postedBy";
    public static final int PROP_ID_postedBy = 32;
    
    /* 关联退货单号(RETURN 处置编排退货域后登记): RETURN_CODE VARCHAR */
    public static final String PROP_NAME_returnCode = "returnCode";
    public static final int PROP_ID_returnCode = 33;
    

    private static int _PROP_ID_BOUND = 34;

    
    /* relation:  */
    public static final String PROP_NAME_actions = "actions";
    
    /* relation:  */
    public static final String PROP_NAME_inspection = "inspection";
    
    /* relation:  */
    public static final String PROP_NAME_material = "material";
    
    /* relation:  */
    public static final String PROP_NAME_supplier = "supplier";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[34];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_ncrDate] = PROP_NAME_ncrDate;
          PROP_NAME_TO_ID.put(PROP_NAME_ncrDate, PROP_ID_ncrDate);
      
          PROP_ID_TO_NAME[PROP_ID_sourceType] = PROP_NAME_sourceType;
          PROP_NAME_TO_ID.put(PROP_NAME_sourceType, PROP_ID_sourceType);
      
          PROP_ID_TO_NAME[PROP_ID_sourceCode] = PROP_NAME_sourceCode;
          PROP_NAME_TO_ID.put(PROP_NAME_sourceCode, PROP_ID_sourceCode);
      
          PROP_ID_TO_NAME[PROP_ID_materialId] = PROP_NAME_materialId;
          PROP_NAME_TO_ID.put(PROP_NAME_materialId, PROP_ID_materialId);
      
          PROP_ID_TO_NAME[PROP_ID_inspectionId] = PROP_NAME_inspectionId;
          PROP_NAME_TO_ID.put(PROP_NAME_inspectionId, PROP_ID_inspectionId);
      
          PROP_ID_TO_NAME[PROP_ID_quantity] = PROP_NAME_quantity;
          PROP_NAME_TO_ID.put(PROP_NAME_quantity, PROP_ID_quantity);
      
          PROP_ID_TO_NAME[PROP_ID_description] = PROP_NAME_description;
          PROP_NAME_TO_ID.put(PROP_NAME_description, PROP_ID_description);
      
          PROP_ID_TO_NAME[PROP_ID_severity] = PROP_NAME_severity;
          PROP_NAME_TO_ID.put(PROP_NAME_severity, PROP_ID_severity);
      
          PROP_ID_TO_NAME[PROP_ID_dispositionType] = PROP_NAME_dispositionType;
          PROP_NAME_TO_ID.put(PROP_NAME_dispositionType, PROP_ID_dispositionType);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_supplierId] = PROP_NAME_supplierId;
          PROP_NAME_TO_ID.put(PROP_NAME_supplierId, PROP_ID_supplierId);
      
          PROP_ID_TO_NAME[PROP_ID_parameterName] = PROP_NAME_parameterName;
          PROP_NAME_TO_ID.put(PROP_NAME_parameterName, PROP_ID_parameterName);
      
          PROP_ID_TO_NAME[PROP_ID_measuredValue] = PROP_NAME_measuredValue;
          PROP_NAME_TO_ID.put(PROP_NAME_measuredValue, PROP_ID_measuredValue);
      
          PROP_ID_TO_NAME[PROP_ID_specMin] = PROP_NAME_specMin;
          PROP_NAME_TO_ID.put(PROP_NAME_specMin, PROP_ID_specMin);
      
          PROP_ID_TO_NAME[PROP_ID_specMax] = PROP_NAME_specMax;
          PROP_NAME_TO_ID.put(PROP_NAME_specMax, PROP_ID_specMax);
      
          PROP_ID_TO_NAME[PROP_ID_assignedTo] = PROP_NAME_assignedTo;
          PROP_NAME_TO_ID.put(PROP_NAME_assignedTo, PROP_ID_assignedTo);
      
          PROP_ID_TO_NAME[PROP_ID_resolvedBy] = PROP_NAME_resolvedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_resolvedBy, PROP_ID_resolvedBy);
      
          PROP_ID_TO_NAME[PROP_ID_resolvedAt] = PROP_NAME_resolvedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_resolvedAt, PROP_ID_resolvedAt);
      
          PROP_ID_TO_NAME[PROP_ID_resolution] = PROP_NAME_resolution;
          PROP_NAME_TO_ID.put(PROP_NAME_resolution, PROP_ID_resolution);
      
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
      
          PROP_ID_TO_NAME[PROP_ID_posted] = PROP_NAME_posted;
          PROP_NAME_TO_ID.put(PROP_NAME_posted, PROP_ID_posted);
      
          PROP_ID_TO_NAME[PROP_ID_postedAt] = PROP_NAME_postedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_postedAt, PROP_ID_postedAt);
      
          PROP_ID_TO_NAME[PROP_ID_postedBy] = PROP_NAME_postedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_postedBy, PROP_ID_postedBy);
      
          PROP_ID_TO_NAME[PROP_ID_returnCode] = PROP_NAME_returnCode;
          PROP_NAME_TO_ID.put(PROP_NAME_returnCode, PROP_ID_returnCode);
      
    }

    
    /* ID: ID */
    private java.lang.Long _id;
    
    /* 单号: CODE */
    private java.lang.String _code;
    
    /* 报告日期: NCR_DATE */
    private java.time.LocalDate _ncrDate;
    
    /* 来源类型: SOURCE_TYPE */
    private java.lang.String _sourceType;
    
    /* 来源单号: SOURCE_CODE */
    private java.lang.String _sourceCode;
    
    /* 物料: MATERIAL_ID */
    private java.lang.Long _materialId;
    
    /* 质检单ID: INSPECTION_ID */
    private java.lang.Long _inspectionId;
    
    /* 不合格数量: QUANTITY */
    private java.math.BigDecimal _quantity;
    
    /* 问题描述: DESCRIPTION */
    private java.lang.String _description;
    
    /* 严重程度: SEVERITY */
    private java.lang.String _severity;
    
    /* 处理决定: DISPOSITION_TYPE */
    private java.lang.String _dispositionType;
    
    /* 状态: STATUS */
    private java.lang.String _status;
    
    /* 供应商: SUPPLIER_ID */
    private java.lang.Long _supplierId;
    
    /* 不合格参数: PARAMETER_NAME */
    private java.lang.String _parameterName;
    
    /* 实测值: MEASURED_VALUE */
    private java.lang.String _measuredValue;
    
    /* 规格下限: SPEC_MIN */
    private java.math.BigDecimal _specMin;
    
    /* 规格上限: SPEC_MAX */
    private java.math.BigDecimal _specMax;
    
    /* 责任人: ASSIGNED_TO */
    private java.lang.Long _assignedTo;
    
    /* 解决人: RESOLVED_BY */
    private java.lang.Long _resolvedBy;
    
    /* 解决时间: RESOLVED_AT */
    private java.time.LocalDateTime _resolvedAt;
    
    /* 解决措施: RESOLUTION */
    private java.lang.String _resolution;
    
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
    
    /* 已过账(报废处置已生成凭证): POSTED */
    private java.lang.Boolean _posted;
    
    /* 过账时间: POSTED_AT */
    private java.time.LocalDateTime _postedAt;
    
    /* 过账人: POSTED_BY */
    private java.lang.String _postedBy;
    
    /* 关联退货单号(RETURN 处置编排退货域后登记): RETURN_CODE */
    private java.lang.String _returnCode;
    

    public _ErpQaNonConformance(){
        // for debug
    }

    protected ErpQaNonConformance newInstance(){
        ErpQaNonConformance entity = new ErpQaNonConformance();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpQaNonConformance cloneInstance() {
        ErpQaNonConformance entity = newInstance();
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
      return "app.erp.qa.dao.entity.ErpQaNonConformance";
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
        
            case PROP_ID_ncrDate:
               return getNcrDate();
        
            case PROP_ID_sourceType:
               return getSourceType();
        
            case PROP_ID_sourceCode:
               return getSourceCode();
        
            case PROP_ID_materialId:
               return getMaterialId();
        
            case PROP_ID_inspectionId:
               return getInspectionId();
        
            case PROP_ID_quantity:
               return getQuantity();
        
            case PROP_ID_description:
               return getDescription();
        
            case PROP_ID_severity:
               return getSeverity();
        
            case PROP_ID_dispositionType:
               return getDispositionType();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_supplierId:
               return getSupplierId();
        
            case PROP_ID_parameterName:
               return getParameterName();
        
            case PROP_ID_measuredValue:
               return getMeasuredValue();
        
            case PROP_ID_specMin:
               return getSpecMin();
        
            case PROP_ID_specMax:
               return getSpecMax();
        
            case PROP_ID_assignedTo:
               return getAssignedTo();
        
            case PROP_ID_resolvedBy:
               return getResolvedBy();
        
            case PROP_ID_resolvedAt:
               return getResolvedAt();
        
            case PROP_ID_resolution:
               return getResolution();
        
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
        
            case PROP_ID_posted:
               return getPosted();
        
            case PROP_ID_postedAt:
               return getPostedAt();
        
            case PROP_ID_postedBy:
               return getPostedBy();
        
            case PROP_ID_returnCode:
               return getReturnCode();
        
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
        
            case PROP_ID_ncrDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_ncrDate));
               }
               setNcrDate(typedValue);
               break;
            }
        
            case PROP_ID_sourceType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_sourceType));
               }
               setSourceType(typedValue);
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
        
            case PROP_ID_materialId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_materialId));
               }
               setMaterialId(typedValue);
               break;
            }
        
            case PROP_ID_inspectionId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_inspectionId));
               }
               setInspectionId(typedValue);
               break;
            }
        
            case PROP_ID_quantity:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_quantity));
               }
               setQuantity(typedValue);
               break;
            }
        
            case PROP_ID_description:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_description));
               }
               setDescription(typedValue);
               break;
            }
        
            case PROP_ID_severity:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_severity));
               }
               setSeverity(typedValue);
               break;
            }
        
            case PROP_ID_dispositionType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_dispositionType));
               }
               setDispositionType(typedValue);
               break;
            }
        
            case PROP_ID_status:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_status));
               }
               setStatus(typedValue);
               break;
            }
        
            case PROP_ID_supplierId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_supplierId));
               }
               setSupplierId(typedValue);
               break;
            }
        
            case PROP_ID_parameterName:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_parameterName));
               }
               setParameterName(typedValue);
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
        
            case PROP_ID_assignedTo:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_assignedTo));
               }
               setAssignedTo(typedValue);
               break;
            }
        
            case PROP_ID_resolvedBy:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_resolvedBy));
               }
               setResolvedBy(typedValue);
               break;
            }
        
            case PROP_ID_resolvedAt:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_resolvedAt));
               }
               setResolvedAt(typedValue);
               break;
            }
        
            case PROP_ID_resolution:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_resolution));
               }
               setResolution(typedValue);
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
        
            case PROP_ID_posted:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_posted));
               }
               setPosted(typedValue);
               break;
            }
        
            case PROP_ID_postedAt:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_postedAt));
               }
               setPostedAt(typedValue);
               break;
            }
        
            case PROP_ID_postedBy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_postedBy));
               }
               setPostedBy(typedValue);
               break;
            }
        
            case PROP_ID_returnCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_returnCode));
               }
               setReturnCode(typedValue);
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
        
            case PROP_ID_ncrDate:{
               onInitProp(propId);
               this._ncrDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_sourceType:{
               onInitProp(propId);
               this._sourceType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_sourceCode:{
               onInitProp(propId);
               this._sourceCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_materialId:{
               onInitProp(propId);
               this._materialId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_inspectionId:{
               onInitProp(propId);
               this._inspectionId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_quantity:{
               onInitProp(propId);
               this._quantity = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_description:{
               onInitProp(propId);
               this._description = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_severity:{
               onInitProp(propId);
               this._severity = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_dispositionType:{
               onInitProp(propId);
               this._dispositionType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_supplierId:{
               onInitProp(propId);
               this._supplierId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_parameterName:{
               onInitProp(propId);
               this._parameterName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_measuredValue:{
               onInitProp(propId);
               this._measuredValue = (java.lang.String)value;
               
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
        
            case PROP_ID_assignedTo:{
               onInitProp(propId);
               this._assignedTo = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_resolvedBy:{
               onInitProp(propId);
               this._resolvedBy = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_resolvedAt:{
               onInitProp(propId);
               this._resolvedAt = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_resolution:{
               onInitProp(propId);
               this._resolution = (java.lang.String)value;
               
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
        
            case PROP_ID_posted:{
               onInitProp(propId);
               this._posted = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_postedAt:{
               onInitProp(propId);
               this._postedAt = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_postedBy:{
               onInitProp(propId);
               this._postedBy = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_returnCode:{
               onInitProp(propId);
               this._returnCode = (java.lang.String)value;
               
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
     * 报告日期: NCR_DATE
     */
    public final java.time.LocalDate getNcrDate(){
         onPropGet(PROP_ID_ncrDate);
         return _ncrDate;
    }

    /**
     * 报告日期: NCR_DATE
     */
    public final void setNcrDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_ncrDate,value)){
            this._ncrDate = value;
            internalClearRefs(PROP_ID_ncrDate);
            
        }
    }
    
    /**
     * 来源类型: SOURCE_TYPE
     */
    public final java.lang.String getSourceType(){
         onPropGet(PROP_ID_sourceType);
         return _sourceType;
    }

    /**
     * 来源类型: SOURCE_TYPE
     */
    public final void setSourceType(java.lang.String value){
        if(onPropSet(PROP_ID_sourceType,value)){
            this._sourceType = value;
            internalClearRefs(PROP_ID_sourceType);
            
        }
    }
    
    /**
     * 来源单号: SOURCE_CODE
     */
    public final java.lang.String getSourceCode(){
         onPropGet(PROP_ID_sourceCode);
         return _sourceCode;
    }

    /**
     * 来源单号: SOURCE_CODE
     */
    public final void setSourceCode(java.lang.String value){
        if(onPropSet(PROP_ID_sourceCode,value)){
            this._sourceCode = value;
            internalClearRefs(PROP_ID_sourceCode);
            
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
     * 质检单ID: INSPECTION_ID
     */
    public final java.lang.Long getInspectionId(){
         onPropGet(PROP_ID_inspectionId);
         return _inspectionId;
    }

    /**
     * 质检单ID: INSPECTION_ID
     */
    public final void setInspectionId(java.lang.Long value){
        if(onPropSet(PROP_ID_inspectionId,value)){
            this._inspectionId = value;
            internalClearRefs(PROP_ID_inspectionId);
            
        }
    }
    
    /**
     * 不合格数量: QUANTITY
     */
    public final java.math.BigDecimal getQuantity(){
         onPropGet(PROP_ID_quantity);
         return _quantity;
    }

    /**
     * 不合格数量: QUANTITY
     */
    public final void setQuantity(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_quantity,value)){
            this._quantity = value;
            internalClearRefs(PROP_ID_quantity);
            
        }
    }
    
    /**
     * 问题描述: DESCRIPTION
     */
    public final java.lang.String getDescription(){
         onPropGet(PROP_ID_description);
         return _description;
    }

    /**
     * 问题描述: DESCRIPTION
     */
    public final void setDescription(java.lang.String value){
        if(onPropSet(PROP_ID_description,value)){
            this._description = value;
            internalClearRefs(PROP_ID_description);
            
        }
    }
    
    /**
     * 严重程度: SEVERITY
     */
    public final java.lang.String getSeverity(){
         onPropGet(PROP_ID_severity);
         return _severity;
    }

    /**
     * 严重程度: SEVERITY
     */
    public final void setSeverity(java.lang.String value){
        if(onPropSet(PROP_ID_severity,value)){
            this._severity = value;
            internalClearRefs(PROP_ID_severity);
            
        }
    }
    
    /**
     * 处理决定: DISPOSITION_TYPE
     */
    public final java.lang.String getDispositionType(){
         onPropGet(PROP_ID_dispositionType);
         return _dispositionType;
    }

    /**
     * 处理决定: DISPOSITION_TYPE
     */
    public final void setDispositionType(java.lang.String value){
        if(onPropSet(PROP_ID_dispositionType,value)){
            this._dispositionType = value;
            internalClearRefs(PROP_ID_dispositionType);
            
        }
    }
    
    /**
     * 状态: STATUS
     */
    public final java.lang.String getStatus(){
         onPropGet(PROP_ID_status);
         return _status;
    }

    /**
     * 状态: STATUS
     */
    public final void setStatus(java.lang.String value){
        if(onPropSet(PROP_ID_status,value)){
            this._status = value;
            internalClearRefs(PROP_ID_status);
            
        }
    }
    
    /**
     * 供应商: SUPPLIER_ID
     */
    public final java.lang.Long getSupplierId(){
         onPropGet(PROP_ID_supplierId);
         return _supplierId;
    }

    /**
     * 供应商: SUPPLIER_ID
     */
    public final void setSupplierId(java.lang.Long value){
        if(onPropSet(PROP_ID_supplierId,value)){
            this._supplierId = value;
            internalClearRefs(PROP_ID_supplierId);
            
        }
    }
    
    /**
     * 不合格参数: PARAMETER_NAME
     */
    public final java.lang.String getParameterName(){
         onPropGet(PROP_ID_parameterName);
         return _parameterName;
    }

    /**
     * 不合格参数: PARAMETER_NAME
     */
    public final void setParameterName(java.lang.String value){
        if(onPropSet(PROP_ID_parameterName,value)){
            this._parameterName = value;
            internalClearRefs(PROP_ID_parameterName);
            
        }
    }
    
    /**
     * 实测值: MEASURED_VALUE
     */
    public final java.lang.String getMeasuredValue(){
         onPropGet(PROP_ID_measuredValue);
         return _measuredValue;
    }

    /**
     * 实测值: MEASURED_VALUE
     */
    public final void setMeasuredValue(java.lang.String value){
        if(onPropSet(PROP_ID_measuredValue,value)){
            this._measuredValue = value;
            internalClearRefs(PROP_ID_measuredValue);
            
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
     * 责任人: ASSIGNED_TO
     */
    public final java.lang.Long getAssignedTo(){
         onPropGet(PROP_ID_assignedTo);
         return _assignedTo;
    }

    /**
     * 责任人: ASSIGNED_TO
     */
    public final void setAssignedTo(java.lang.Long value){
        if(onPropSet(PROP_ID_assignedTo,value)){
            this._assignedTo = value;
            internalClearRefs(PROP_ID_assignedTo);
            
        }
    }
    
    /**
     * 解决人: RESOLVED_BY
     */
    public final java.lang.Long getResolvedBy(){
         onPropGet(PROP_ID_resolvedBy);
         return _resolvedBy;
    }

    /**
     * 解决人: RESOLVED_BY
     */
    public final void setResolvedBy(java.lang.Long value){
        if(onPropSet(PROP_ID_resolvedBy,value)){
            this._resolvedBy = value;
            internalClearRefs(PROP_ID_resolvedBy);
            
        }
    }
    
    /**
     * 解决时间: RESOLVED_AT
     */
    public final java.time.LocalDateTime getResolvedAt(){
         onPropGet(PROP_ID_resolvedAt);
         return _resolvedAt;
    }

    /**
     * 解决时间: RESOLVED_AT
     */
    public final void setResolvedAt(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_resolvedAt,value)){
            this._resolvedAt = value;
            internalClearRefs(PROP_ID_resolvedAt);
            
        }
    }
    
    /**
     * 解决措施: RESOLUTION
     */
    public final java.lang.String getResolution(){
         onPropGet(PROP_ID_resolution);
         return _resolution;
    }

    /**
     * 解决措施: RESOLUTION
     */
    public final void setResolution(java.lang.String value){
        if(onPropSet(PROP_ID_resolution,value)){
            this._resolution = value;
            internalClearRefs(PROP_ID_resolution);
            
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
     * 已过账(报废处置已生成凭证): POSTED
     */
    public final java.lang.Boolean getPosted(){
         onPropGet(PROP_ID_posted);
         return _posted;
    }

    /**
     * 已过账(报废处置已生成凭证): POSTED
     */
    public final void setPosted(java.lang.Boolean value){
        if(onPropSet(PROP_ID_posted,value)){
            this._posted = value;
            internalClearRefs(PROP_ID_posted);
            
        }
    }
    
    /**
     * 过账时间: POSTED_AT
     */
    public final java.time.LocalDateTime getPostedAt(){
         onPropGet(PROP_ID_postedAt);
         return _postedAt;
    }

    /**
     * 过账时间: POSTED_AT
     */
    public final void setPostedAt(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_postedAt,value)){
            this._postedAt = value;
            internalClearRefs(PROP_ID_postedAt);
            
        }
    }
    
    /**
     * 过账人: POSTED_BY
     */
    public final java.lang.String getPostedBy(){
         onPropGet(PROP_ID_postedBy);
         return _postedBy;
    }

    /**
     * 过账人: POSTED_BY
     */
    public final void setPostedBy(java.lang.String value){
        if(onPropSet(PROP_ID_postedBy,value)){
            this._postedBy = value;
            internalClearRefs(PROP_ID_postedBy);
            
        }
    }
    
    /**
     * 关联退货单号(RETURN 处置编排退货域后登记): RETURN_CODE
     */
    public final java.lang.String getReturnCode(){
         onPropGet(PROP_ID_returnCode);
         return _returnCode;
    }

    /**
     * 关联退货单号(RETURN 处置编排退货域后登记): RETURN_CODE
     */
    public final void setReturnCode(java.lang.String value){
        if(onPropSet(PROP_ID_returnCode,value)){
            this._returnCode = value;
            internalClearRefs(PROP_ID_returnCode);
            
        }
    }
    
    private final OrmEntitySet<app.erp.qa.dao.entity.ErpQaAction> _actions = new OrmEntitySet<>(this, PROP_NAME_actions,
        null, null,app.erp.qa.dao.entity.ErpQaAction.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.qa.dao.entity.ErpQaAction> getActions(){
       return _actions;
    }
       
    /**
     * 
     */
    public final app.erp.qa.dao.entity.ErpQaInspection getInspection(){
       return (app.erp.qa.dao.entity.ErpQaInspection)internalGetRefEntity(PROP_NAME_inspection);
    }

    public final void setInspection(app.erp.qa.dao.entity.ErpQaInspection refEntity){
   
           if(refEntity == null){
           
                   this.setInspectionId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_inspection, refEntity,()->{
           
                           this.setInspectionId(refEntity.getId());
                       
           });
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
    public final app.erp.md.dao.entity.ErpMdPartner getSupplier(){
       return (app.erp.md.dao.entity.ErpMdPartner)internalGetRefEntity(PROP_NAME_supplier);
    }

    public final void setSupplier(app.erp.md.dao.entity.ErpMdPartner refEntity){
   
           if(refEntity == null){
           
                   this.setSupplierId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_supplier, refEntity,()->{
           
                           this.setSupplierId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
