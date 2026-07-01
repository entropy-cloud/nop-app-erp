package app.erp.mfg.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.mfg.dao.entity.ErpMfgBatchGenealogy;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  批次追溯: erp_mfg_batch_genealogy
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpMfgBatchGenealogy extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 工单: WORK_ORDER_ID BIGINT */
    public static final String PROP_NAME_workOrderId = "workOrderId";
    public static final int PROP_ID_workOrderId = 2;
    
    /* 作业卡: JOB_CARD_ID BIGINT */
    public static final String PROP_NAME_jobCardId = "jobCardId";
    public static final int PROP_ID_jobCardId = 3;
    
    /* 工序: OPERATION_ID BIGINT */
    public static final String PROP_NAME_operationId = "operationId";
    public static final int PROP_ID_operationId = 4;
    
    /* 输入批次: INPUT_LOT_ID BIGINT */
    public static final String PROP_NAME_inputLotId = "inputLotId";
    public static final int PROP_ID_inputLotId = 5;
    
    /* 输入物料: INPUT_MATERIAL_ID BIGINT */
    public static final String PROP_NAME_inputMaterialId = "inputMaterialId";
    public static final int PROP_ID_inputMaterialId = 6;
    
    /* 投入数量: INPUT_QTY DECIMAL */
    public static final String PROP_NAME_inputQty = "inputQty";
    public static final int PROP_ID_inputQty = 7;
    
    /* 投入计量单位: INPUT_UO_M_ID BIGINT */
    public static final String PROP_NAME_inputUoMId = "inputUoMId";
    public static final int PROP_ID_inputUoMId = 8;
    
    /* 产出批次: OUTPUT_LOT_ID BIGINT */
    public static final String PROP_NAME_outputLotId = "outputLotId";
    public static final int PROP_ID_outputLotId = 9;
    
    /* 产出物料: OUTPUT_MATERIAL_ID BIGINT */
    public static final String PROP_NAME_outputMaterialId = "outputMaterialId";
    public static final int PROP_ID_outputMaterialId = 10;
    
    /* 产出数量: OUTPUT_QTY DECIMAL */
    public static final String PROP_NAME_outputQty = "outputQty";
    public static final int PROP_ID_outputQty = 11;
    
    /* 产出计量单位: OUTPUT_UO_M_ID BIGINT */
    public static final String PROP_NAME_outputUoMId = "outputUoMId";
    public static final int PROP_ID_outputUoMId = 12;
    
    /* 生产日期: PRODUCTION_DATE DATE */
    public static final String PROP_NAME_productionDate = "productionDate";
    public static final int PROP_ID_productionDate = 13;
    
    /* 生产时间: PRODUCTION_TIME DATETIME */
    public static final String PROP_NAME_productionTime = "productionTime";
    public static final int PROP_ID_productionTime = 14;
    
    /* 行号: LINE_NO INTEGER */
    public static final String PROP_NAME_lineNo = "lineNo";
    public static final int PROP_ID_lineNo = 15;
    
    /* 批次状态: LOT_STATUS VARCHAR */
    public static final String PROP_NAME_lotStatus = "lotStatus";
    public static final int PROP_ID_lotStatus = 16;
    
    /* 输入是否已消耗: IS_INPUT_CONSUMED BOOLEAN */
    public static final String PROP_NAME_isInputConsumed = "isInputConsumed";
    public static final int PROP_ID_isInputConsumed = 17;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 18;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 19;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 20;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 21;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 22;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 23;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 24;
    

    private static int _PROP_ID_BOUND = 25;

    
    /* relation:  */
    public static final String PROP_NAME_workOrder = "workOrder";
    
    /* relation:  */
    public static final String PROP_NAME_jobCard = "jobCard";
    
    /* relation:  */
    public static final String PROP_NAME_inputMaterial = "inputMaterial";
    
    /* relation:  */
    public static final String PROP_NAME_outputMaterial = "outputMaterial";
    
    /* relation:  */
    public static final String PROP_NAME_operation = "operation";
    
    /* relation:  */
    public static final String PROP_NAME_inputLot = "inputLot";
    
    /* relation:  */
    public static final String PROP_NAME_inputUoM = "inputUoM";
    
    /* relation:  */
    public static final String PROP_NAME_outputLot = "outputLot";
    
    /* relation:  */
    public static final String PROP_NAME_outputUoM = "outputUoM";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[25];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_workOrderId] = PROP_NAME_workOrderId;
          PROP_NAME_TO_ID.put(PROP_NAME_workOrderId, PROP_ID_workOrderId);
      
          PROP_ID_TO_NAME[PROP_ID_jobCardId] = PROP_NAME_jobCardId;
          PROP_NAME_TO_ID.put(PROP_NAME_jobCardId, PROP_ID_jobCardId);
      
          PROP_ID_TO_NAME[PROP_ID_operationId] = PROP_NAME_operationId;
          PROP_NAME_TO_ID.put(PROP_NAME_operationId, PROP_ID_operationId);
      
          PROP_ID_TO_NAME[PROP_ID_inputLotId] = PROP_NAME_inputLotId;
          PROP_NAME_TO_ID.put(PROP_NAME_inputLotId, PROP_ID_inputLotId);
      
          PROP_ID_TO_NAME[PROP_ID_inputMaterialId] = PROP_NAME_inputMaterialId;
          PROP_NAME_TO_ID.put(PROP_NAME_inputMaterialId, PROP_ID_inputMaterialId);
      
          PROP_ID_TO_NAME[PROP_ID_inputQty] = PROP_NAME_inputQty;
          PROP_NAME_TO_ID.put(PROP_NAME_inputQty, PROP_ID_inputQty);
      
          PROP_ID_TO_NAME[PROP_ID_inputUoMId] = PROP_NAME_inputUoMId;
          PROP_NAME_TO_ID.put(PROP_NAME_inputUoMId, PROP_ID_inputUoMId);
      
          PROP_ID_TO_NAME[PROP_ID_outputLotId] = PROP_NAME_outputLotId;
          PROP_NAME_TO_ID.put(PROP_NAME_outputLotId, PROP_ID_outputLotId);
      
          PROP_ID_TO_NAME[PROP_ID_outputMaterialId] = PROP_NAME_outputMaterialId;
          PROP_NAME_TO_ID.put(PROP_NAME_outputMaterialId, PROP_ID_outputMaterialId);
      
          PROP_ID_TO_NAME[PROP_ID_outputQty] = PROP_NAME_outputQty;
          PROP_NAME_TO_ID.put(PROP_NAME_outputQty, PROP_ID_outputQty);
      
          PROP_ID_TO_NAME[PROP_ID_outputUoMId] = PROP_NAME_outputUoMId;
          PROP_NAME_TO_ID.put(PROP_NAME_outputUoMId, PROP_ID_outputUoMId);
      
          PROP_ID_TO_NAME[PROP_ID_productionDate] = PROP_NAME_productionDate;
          PROP_NAME_TO_ID.put(PROP_NAME_productionDate, PROP_ID_productionDate);
      
          PROP_ID_TO_NAME[PROP_ID_productionTime] = PROP_NAME_productionTime;
          PROP_NAME_TO_ID.put(PROP_NAME_productionTime, PROP_ID_productionTime);
      
          PROP_ID_TO_NAME[PROP_ID_lineNo] = PROP_NAME_lineNo;
          PROP_NAME_TO_ID.put(PROP_NAME_lineNo, PROP_ID_lineNo);
      
          PROP_ID_TO_NAME[PROP_ID_lotStatus] = PROP_NAME_lotStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_lotStatus, PROP_ID_lotStatus);
      
          PROP_ID_TO_NAME[PROP_ID_isInputConsumed] = PROP_NAME_isInputConsumed;
          PROP_NAME_TO_ID.put(PROP_NAME_isInputConsumed, PROP_ID_isInputConsumed);
      
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
    
    /* 工单: WORK_ORDER_ID */
    private java.lang.Long _workOrderId;
    
    /* 作业卡: JOB_CARD_ID */
    private java.lang.Long _jobCardId;
    
    /* 工序: OPERATION_ID */
    private java.lang.Long _operationId;
    
    /* 输入批次: INPUT_LOT_ID */
    private java.lang.Long _inputLotId;
    
    /* 输入物料: INPUT_MATERIAL_ID */
    private java.lang.Long _inputMaterialId;
    
    /* 投入数量: INPUT_QTY */
    private java.math.BigDecimal _inputQty;
    
    /* 投入计量单位: INPUT_UO_M_ID */
    private java.lang.Long _inputUoMId;
    
    /* 产出批次: OUTPUT_LOT_ID */
    private java.lang.Long _outputLotId;
    
    /* 产出物料: OUTPUT_MATERIAL_ID */
    private java.lang.Long _outputMaterialId;
    
    /* 产出数量: OUTPUT_QTY */
    private java.math.BigDecimal _outputQty;
    
    /* 产出计量单位: OUTPUT_UO_M_ID */
    private java.lang.Long _outputUoMId;
    
    /* 生产日期: PRODUCTION_DATE */
    private java.time.LocalDate _productionDate;
    
    /* 生产时间: PRODUCTION_TIME */
    private java.time.LocalDateTime _productionTime;
    
    /* 行号: LINE_NO */
    private java.lang.Integer _lineNo;
    
    /* 批次状态: LOT_STATUS */
    private java.lang.String _lotStatus;
    
    /* 输入是否已消耗: IS_INPUT_CONSUMED */
    private java.lang.Boolean _isInputConsumed;
    
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
    

    public _ErpMfgBatchGenealogy(){
        // for debug
    }

    protected ErpMfgBatchGenealogy newInstance(){
        ErpMfgBatchGenealogy entity = new ErpMfgBatchGenealogy();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpMfgBatchGenealogy cloneInstance() {
        ErpMfgBatchGenealogy entity = newInstance();
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
      return "app.erp.mfg.dao.entity.ErpMfgBatchGenealogy";
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
        
            case PROP_ID_workOrderId:
               return getWorkOrderId();
        
            case PROP_ID_jobCardId:
               return getJobCardId();
        
            case PROP_ID_operationId:
               return getOperationId();
        
            case PROP_ID_inputLotId:
               return getInputLotId();
        
            case PROP_ID_inputMaterialId:
               return getInputMaterialId();
        
            case PROP_ID_inputQty:
               return getInputQty();
        
            case PROP_ID_inputUoMId:
               return getInputUoMId();
        
            case PROP_ID_outputLotId:
               return getOutputLotId();
        
            case PROP_ID_outputMaterialId:
               return getOutputMaterialId();
        
            case PROP_ID_outputQty:
               return getOutputQty();
        
            case PROP_ID_outputUoMId:
               return getOutputUoMId();
        
            case PROP_ID_productionDate:
               return getProductionDate();
        
            case PROP_ID_productionTime:
               return getProductionTime();
        
            case PROP_ID_lineNo:
               return getLineNo();
        
            case PROP_ID_lotStatus:
               return getLotStatus();
        
            case PROP_ID_isInputConsumed:
               return getIsInputConsumed();
        
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
        
            case PROP_ID_workOrderId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_workOrderId));
               }
               setWorkOrderId(typedValue);
               break;
            }
        
            case PROP_ID_jobCardId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_jobCardId));
               }
               setJobCardId(typedValue);
               break;
            }
        
            case PROP_ID_operationId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_operationId));
               }
               setOperationId(typedValue);
               break;
            }
        
            case PROP_ID_inputLotId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_inputLotId));
               }
               setInputLotId(typedValue);
               break;
            }
        
            case PROP_ID_inputMaterialId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_inputMaterialId));
               }
               setInputMaterialId(typedValue);
               break;
            }
        
            case PROP_ID_inputQty:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_inputQty));
               }
               setInputQty(typedValue);
               break;
            }
        
            case PROP_ID_inputUoMId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_inputUoMId));
               }
               setInputUoMId(typedValue);
               break;
            }
        
            case PROP_ID_outputLotId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_outputLotId));
               }
               setOutputLotId(typedValue);
               break;
            }
        
            case PROP_ID_outputMaterialId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_outputMaterialId));
               }
               setOutputMaterialId(typedValue);
               break;
            }
        
            case PROP_ID_outputQty:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_outputQty));
               }
               setOutputQty(typedValue);
               break;
            }
        
            case PROP_ID_outputUoMId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_outputUoMId));
               }
               setOutputUoMId(typedValue);
               break;
            }
        
            case PROP_ID_productionDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_productionDate));
               }
               setProductionDate(typedValue);
               break;
            }
        
            case PROP_ID_productionTime:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_productionTime));
               }
               setProductionTime(typedValue);
               break;
            }
        
            case PROP_ID_lineNo:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_lineNo));
               }
               setLineNo(typedValue);
               break;
            }
        
            case PROP_ID_lotStatus:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_lotStatus));
               }
               setLotStatus(typedValue);
               break;
            }
        
            case PROP_ID_isInputConsumed:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isInputConsumed));
               }
               setIsInputConsumed(typedValue);
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
        
            case PROP_ID_workOrderId:{
               onInitProp(propId);
               this._workOrderId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_jobCardId:{
               onInitProp(propId);
               this._jobCardId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_operationId:{
               onInitProp(propId);
               this._operationId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_inputLotId:{
               onInitProp(propId);
               this._inputLotId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_inputMaterialId:{
               onInitProp(propId);
               this._inputMaterialId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_inputQty:{
               onInitProp(propId);
               this._inputQty = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_inputUoMId:{
               onInitProp(propId);
               this._inputUoMId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_outputLotId:{
               onInitProp(propId);
               this._outputLotId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_outputMaterialId:{
               onInitProp(propId);
               this._outputMaterialId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_outputQty:{
               onInitProp(propId);
               this._outputQty = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_outputUoMId:{
               onInitProp(propId);
               this._outputUoMId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_productionDate:{
               onInitProp(propId);
               this._productionDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_productionTime:{
               onInitProp(propId);
               this._productionTime = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_lineNo:{
               onInitProp(propId);
               this._lineNo = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_lotStatus:{
               onInitProp(propId);
               this._lotStatus = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_isInputConsumed:{
               onInitProp(propId);
               this._isInputConsumed = (java.lang.Boolean)value;
               
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
     * 工单: WORK_ORDER_ID
     */
    public final java.lang.Long getWorkOrderId(){
         onPropGet(PROP_ID_workOrderId);
         return _workOrderId;
    }

    /**
     * 工单: WORK_ORDER_ID
     */
    public final void setWorkOrderId(java.lang.Long value){
        if(onPropSet(PROP_ID_workOrderId,value)){
            this._workOrderId = value;
            internalClearRefs(PROP_ID_workOrderId);
            
        }
    }
    
    /**
     * 作业卡: JOB_CARD_ID
     */
    public final java.lang.Long getJobCardId(){
         onPropGet(PROP_ID_jobCardId);
         return _jobCardId;
    }

    /**
     * 作业卡: JOB_CARD_ID
     */
    public final void setJobCardId(java.lang.Long value){
        if(onPropSet(PROP_ID_jobCardId,value)){
            this._jobCardId = value;
            internalClearRefs(PROP_ID_jobCardId);
            
        }
    }
    
    /**
     * 工序: OPERATION_ID
     */
    public final java.lang.Long getOperationId(){
         onPropGet(PROP_ID_operationId);
         return _operationId;
    }

    /**
     * 工序: OPERATION_ID
     */
    public final void setOperationId(java.lang.Long value){
        if(onPropSet(PROP_ID_operationId,value)){
            this._operationId = value;
            internalClearRefs(PROP_ID_operationId);
            
        }
    }
    
    /**
     * 输入批次: INPUT_LOT_ID
     */
    public final java.lang.Long getInputLotId(){
         onPropGet(PROP_ID_inputLotId);
         return _inputLotId;
    }

    /**
     * 输入批次: INPUT_LOT_ID
     */
    public final void setInputLotId(java.lang.Long value){
        if(onPropSet(PROP_ID_inputLotId,value)){
            this._inputLotId = value;
            internalClearRefs(PROP_ID_inputLotId);
            
        }
    }
    
    /**
     * 输入物料: INPUT_MATERIAL_ID
     */
    public final java.lang.Long getInputMaterialId(){
         onPropGet(PROP_ID_inputMaterialId);
         return _inputMaterialId;
    }

    /**
     * 输入物料: INPUT_MATERIAL_ID
     */
    public final void setInputMaterialId(java.lang.Long value){
        if(onPropSet(PROP_ID_inputMaterialId,value)){
            this._inputMaterialId = value;
            internalClearRefs(PROP_ID_inputMaterialId);
            
        }
    }
    
    /**
     * 投入数量: INPUT_QTY
     */
    public final java.math.BigDecimal getInputQty(){
         onPropGet(PROP_ID_inputQty);
         return _inputQty;
    }

    /**
     * 投入数量: INPUT_QTY
     */
    public final void setInputQty(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_inputQty,value)){
            this._inputQty = value;
            internalClearRefs(PROP_ID_inputQty);
            
        }
    }
    
    /**
     * 投入计量单位: INPUT_UO_M_ID
     */
    public final java.lang.Long getInputUoMId(){
         onPropGet(PROP_ID_inputUoMId);
         return _inputUoMId;
    }

    /**
     * 投入计量单位: INPUT_UO_M_ID
     */
    public final void setInputUoMId(java.lang.Long value){
        if(onPropSet(PROP_ID_inputUoMId,value)){
            this._inputUoMId = value;
            internalClearRefs(PROP_ID_inputUoMId);
            
        }
    }
    
    /**
     * 产出批次: OUTPUT_LOT_ID
     */
    public final java.lang.Long getOutputLotId(){
         onPropGet(PROP_ID_outputLotId);
         return _outputLotId;
    }

    /**
     * 产出批次: OUTPUT_LOT_ID
     */
    public final void setOutputLotId(java.lang.Long value){
        if(onPropSet(PROP_ID_outputLotId,value)){
            this._outputLotId = value;
            internalClearRefs(PROP_ID_outputLotId);
            
        }
    }
    
    /**
     * 产出物料: OUTPUT_MATERIAL_ID
     */
    public final java.lang.Long getOutputMaterialId(){
         onPropGet(PROP_ID_outputMaterialId);
         return _outputMaterialId;
    }

    /**
     * 产出物料: OUTPUT_MATERIAL_ID
     */
    public final void setOutputMaterialId(java.lang.Long value){
        if(onPropSet(PROP_ID_outputMaterialId,value)){
            this._outputMaterialId = value;
            internalClearRefs(PROP_ID_outputMaterialId);
            
        }
    }
    
    /**
     * 产出数量: OUTPUT_QTY
     */
    public final java.math.BigDecimal getOutputQty(){
         onPropGet(PROP_ID_outputQty);
         return _outputQty;
    }

    /**
     * 产出数量: OUTPUT_QTY
     */
    public final void setOutputQty(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_outputQty,value)){
            this._outputQty = value;
            internalClearRefs(PROP_ID_outputQty);
            
        }
    }
    
    /**
     * 产出计量单位: OUTPUT_UO_M_ID
     */
    public final java.lang.Long getOutputUoMId(){
         onPropGet(PROP_ID_outputUoMId);
         return _outputUoMId;
    }

    /**
     * 产出计量单位: OUTPUT_UO_M_ID
     */
    public final void setOutputUoMId(java.lang.Long value){
        if(onPropSet(PROP_ID_outputUoMId,value)){
            this._outputUoMId = value;
            internalClearRefs(PROP_ID_outputUoMId);
            
        }
    }
    
    /**
     * 生产日期: PRODUCTION_DATE
     */
    public final java.time.LocalDate getProductionDate(){
         onPropGet(PROP_ID_productionDate);
         return _productionDate;
    }

    /**
     * 生产日期: PRODUCTION_DATE
     */
    public final void setProductionDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_productionDate,value)){
            this._productionDate = value;
            internalClearRefs(PROP_ID_productionDate);
            
        }
    }
    
    /**
     * 生产时间: PRODUCTION_TIME
     */
    public final java.time.LocalDateTime getProductionTime(){
         onPropGet(PROP_ID_productionTime);
         return _productionTime;
    }

    /**
     * 生产时间: PRODUCTION_TIME
     */
    public final void setProductionTime(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_productionTime,value)){
            this._productionTime = value;
            internalClearRefs(PROP_ID_productionTime);
            
        }
    }
    
    /**
     * 行号: LINE_NO
     */
    public final java.lang.Integer getLineNo(){
         onPropGet(PROP_ID_lineNo);
         return _lineNo;
    }

    /**
     * 行号: LINE_NO
     */
    public final void setLineNo(java.lang.Integer value){
        if(onPropSet(PROP_ID_lineNo,value)){
            this._lineNo = value;
            internalClearRefs(PROP_ID_lineNo);
            
        }
    }
    
    /**
     * 批次状态: LOT_STATUS
     */
    public final java.lang.String getLotStatus(){
         onPropGet(PROP_ID_lotStatus);
         return _lotStatus;
    }

    /**
     * 批次状态: LOT_STATUS
     */
    public final void setLotStatus(java.lang.String value){
        if(onPropSet(PROP_ID_lotStatus,value)){
            this._lotStatus = value;
            internalClearRefs(PROP_ID_lotStatus);
            
        }
    }
    
    /**
     * 输入是否已消耗: IS_INPUT_CONSUMED
     */
    public final java.lang.Boolean getIsInputConsumed(){
         onPropGet(PROP_ID_isInputConsumed);
         return _isInputConsumed;
    }

    /**
     * 输入是否已消耗: IS_INPUT_CONSUMED
     */
    public final void setIsInputConsumed(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isInputConsumed,value)){
            this._isInputConsumed = value;
            internalClearRefs(PROP_ID_isInputConsumed);
            
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
    public final app.erp.mfg.dao.entity.ErpMfgWorkOrder getWorkOrder(){
       return (app.erp.mfg.dao.entity.ErpMfgWorkOrder)internalGetRefEntity(PROP_NAME_workOrder);
    }

    public final void setWorkOrder(app.erp.mfg.dao.entity.ErpMfgWorkOrder refEntity){
   
           if(refEntity == null){
           
                   this.setWorkOrderId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_workOrder, refEntity,()->{
           
                           this.setWorkOrderId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.mfg.dao.entity.ErpMfgJobCard getJobCard(){
       return (app.erp.mfg.dao.entity.ErpMfgJobCard)internalGetRefEntity(PROP_NAME_jobCard);
    }

    public final void setJobCard(app.erp.mfg.dao.entity.ErpMfgJobCard refEntity){
   
           if(refEntity == null){
           
                   this.setJobCardId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_jobCard, refEntity,()->{
           
                           this.setJobCardId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdMaterial getInputMaterial(){
       return (app.erp.md.dao.entity.ErpMdMaterial)internalGetRefEntity(PROP_NAME_inputMaterial);
    }

    public final void setInputMaterial(app.erp.md.dao.entity.ErpMdMaterial refEntity){
   
           if(refEntity == null){
           
                   this.setInputMaterialId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_inputMaterial, refEntity,()->{
           
                           this.setInputMaterialId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdMaterial getOutputMaterial(){
       return (app.erp.md.dao.entity.ErpMdMaterial)internalGetRefEntity(PROP_NAME_outputMaterial);
    }

    public final void setOutputMaterial(app.erp.md.dao.entity.ErpMdMaterial refEntity){
   
           if(refEntity == null){
           
                   this.setOutputMaterialId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_outputMaterial, refEntity,()->{
           
                           this.setOutputMaterialId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.mfg.dao.entity.ErpMfgBomOperation getOperation(){
       return (app.erp.mfg.dao.entity.ErpMfgBomOperation)internalGetRefEntity(PROP_NAME_operation);
    }

    public final void setOperation(app.erp.mfg.dao.entity.ErpMfgBomOperation refEntity){
   
           if(refEntity == null){
           
                   this.setOperationId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_operation, refEntity,()->{
           
                           this.setOperationId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.inv.dao.entity.ErpInvBatch getInputLot(){
       return (app.erp.inv.dao.entity.ErpInvBatch)internalGetRefEntity(PROP_NAME_inputLot);
    }

    public final void setInputLot(app.erp.inv.dao.entity.ErpInvBatch refEntity){
   
           if(refEntity == null){
           
                   this.setInputLotId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_inputLot, refEntity,()->{
           
                           this.setInputLotId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdUoM getInputUoM(){
       return (app.erp.md.dao.entity.ErpMdUoM)internalGetRefEntity(PROP_NAME_inputUoM);
    }

    public final void setInputUoM(app.erp.md.dao.entity.ErpMdUoM refEntity){
   
           if(refEntity == null){
           
                   this.setInputUoMId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_inputUoM, refEntity,()->{
           
                           this.setInputUoMId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.inv.dao.entity.ErpInvBatch getOutputLot(){
       return (app.erp.inv.dao.entity.ErpInvBatch)internalGetRefEntity(PROP_NAME_outputLot);
    }

    public final void setOutputLot(app.erp.inv.dao.entity.ErpInvBatch refEntity){
   
           if(refEntity == null){
           
                   this.setOutputLotId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_outputLot, refEntity,()->{
           
                           this.setOutputLotId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdUoM getOutputUoM(){
       return (app.erp.md.dao.entity.ErpMdUoM)internalGetRefEntity(PROP_NAME_outputUoM);
    }

    public final void setOutputUoM(app.erp.md.dao.entity.ErpMdUoM refEntity){
   
           if(refEntity == null){
           
                   this.setOutputUoMId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_outputUoM, refEntity,()->{
           
                           this.setOutputUoMId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
