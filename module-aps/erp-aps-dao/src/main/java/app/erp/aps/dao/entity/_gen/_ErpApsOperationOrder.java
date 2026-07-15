package app.erp.aps.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.aps.dao.entity.ErpApsOperationOrder;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  工序工单: erp_aps_operation_order
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpApsOperationOrder extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 编号: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 主工单: WORK_ORDER_ID BIGINT */
    public static final String PROP_NAME_workOrderId = "workOrderId";
    public static final int PROP_ID_workOrderId = 3;
    
    /* 工序名称: OPERATION_NAME VARCHAR */
    public static final String PROP_NAME_operationName = "operationName";
    public static final int PROP_ID_operationName = 4;
    
    /* 工序顺序: SEQUENCE INTEGER */
    public static final String PROP_NAME_sequence = "sequence";
    public static final int PROP_ID_sequence = 5;
    
    /* 工作中心/设备: MACHINE_ID BIGINT */
    public static final String PROP_NAME_machineId = "machineId";
    public static final int PROP_ID_machineId = 6;
    
    /* 优先级: PRIORITY INTEGER */
    public static final String PROP_NAME_priority = "priority";
    public static final int PROP_ID_priority = 7;
    
    /* 计划开工时间: PLANNED_START_DATE_T TIMESTAMP */
    public static final String PROP_NAME_plannedStartDateT = "plannedStartDateT";
    public static final int PROP_ID_plannedStartDateT = 8;
    
    /* 计划完工时间: PLANNED_END_DATE_T TIMESTAMP */
    public static final String PROP_NAME_plannedEndDateT = "plannedEndDateT";
    public static final int PROP_ID_plannedEndDateT = 9;
    
    /* 实际开工时间: REAL_START_DATE_T TIMESTAMP */
    public static final String PROP_NAME_realStartDateT = "realStartDateT";
    public static final int PROP_ID_realStartDateT = 10;
    
    /* 实际完工时间: REAL_END_DATE_T TIMESTAMP */
    public static final String PROP_NAME_realEndDateT = "realEndDateT";
    public static final int PROP_ID_realEndDateT = 11;
    
    /* 准备时间(分钟): SETUP_TIME DECIMAL */
    public static final String PROP_NAME_setupTime = "setupTime";
    public static final int PROP_ID_setupTime = 12;
    
    /* 每件加工时间(分钟): RUNTIME_PER_UNIT DECIMAL */
    public static final String PROP_NAME_runtimePerUnit = "runtimePerUnit";
    public static final int PROP_ID_runtimePerUnit = 13;
    
    /* 加工数量: QTY DECIMAL */
    public static final String PROP_NAME_qty = "qty";
    public static final int PROP_ID_qty = 14;
    
    /* 总耗时(分钟): TOTAL_DURATION DECIMAL */
    public static final String PROP_NAME_totalDuration = "totalDuration";
    public static final int PROP_ID_totalDuration = 15;
    
    /* 操作工: ASSIGNED_TO_ID VARCHAR */
    public static final String PROP_NAME_assignedToId = "assignedToId";
    public static final int PROP_ID_assignedToId = 16;
    
    /* 是否外协: IS_OUTSOURCED BOOLEAN */
    public static final String PROP_NAME_isOutsourced = "isOutsourced";
    public static final int PROP_ID_isOutsourced = 17;
    
    /* 状态: STATUS VARCHAR */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 18;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 19;
    
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
    
    /* 最早开工时间: EARLIEST_START_DATE_T TIMESTAMP */
    public static final String PROP_NAME_earliestStartDateT = "earliestStartDateT";
    public static final int PROP_ID_earliestStartDateT = 27;
    
    /* 最晚完工时间: LATEST_END_DATE_T TIMESTAMP */
    public static final String PROP_NAME_latestEndDateT = "latestEndDateT";
    public static final int PROP_ID_latestEndDateT = 28;
    
    /* 业务日期: BUSINESS_DATE DATE */
    public static final String PROP_NAME_businessDate = "businessDate";
    public static final int PROP_ID_businessDate = 29;
    

    private static int _PROP_ID_BOUND = 30;

    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[30];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_workOrderId] = PROP_NAME_workOrderId;
          PROP_NAME_TO_ID.put(PROP_NAME_workOrderId, PROP_ID_workOrderId);
      
          PROP_ID_TO_NAME[PROP_ID_operationName] = PROP_NAME_operationName;
          PROP_NAME_TO_ID.put(PROP_NAME_operationName, PROP_ID_operationName);
      
          PROP_ID_TO_NAME[PROP_ID_sequence] = PROP_NAME_sequence;
          PROP_NAME_TO_ID.put(PROP_NAME_sequence, PROP_ID_sequence);
      
          PROP_ID_TO_NAME[PROP_ID_machineId] = PROP_NAME_machineId;
          PROP_NAME_TO_ID.put(PROP_NAME_machineId, PROP_ID_machineId);
      
          PROP_ID_TO_NAME[PROP_ID_priority] = PROP_NAME_priority;
          PROP_NAME_TO_ID.put(PROP_NAME_priority, PROP_ID_priority);
      
          PROP_ID_TO_NAME[PROP_ID_plannedStartDateT] = PROP_NAME_plannedStartDateT;
          PROP_NAME_TO_ID.put(PROP_NAME_plannedStartDateT, PROP_ID_plannedStartDateT);
      
          PROP_ID_TO_NAME[PROP_ID_plannedEndDateT] = PROP_NAME_plannedEndDateT;
          PROP_NAME_TO_ID.put(PROP_NAME_plannedEndDateT, PROP_ID_plannedEndDateT);
      
          PROP_ID_TO_NAME[PROP_ID_realStartDateT] = PROP_NAME_realStartDateT;
          PROP_NAME_TO_ID.put(PROP_NAME_realStartDateT, PROP_ID_realStartDateT);
      
          PROP_ID_TO_NAME[PROP_ID_realEndDateT] = PROP_NAME_realEndDateT;
          PROP_NAME_TO_ID.put(PROP_NAME_realEndDateT, PROP_ID_realEndDateT);
      
          PROP_ID_TO_NAME[PROP_ID_setupTime] = PROP_NAME_setupTime;
          PROP_NAME_TO_ID.put(PROP_NAME_setupTime, PROP_ID_setupTime);
      
          PROP_ID_TO_NAME[PROP_ID_runtimePerUnit] = PROP_NAME_runtimePerUnit;
          PROP_NAME_TO_ID.put(PROP_NAME_runtimePerUnit, PROP_ID_runtimePerUnit);
      
          PROP_ID_TO_NAME[PROP_ID_qty] = PROP_NAME_qty;
          PROP_NAME_TO_ID.put(PROP_NAME_qty, PROP_ID_qty);
      
          PROP_ID_TO_NAME[PROP_ID_totalDuration] = PROP_NAME_totalDuration;
          PROP_NAME_TO_ID.put(PROP_NAME_totalDuration, PROP_ID_totalDuration);
      
          PROP_ID_TO_NAME[PROP_ID_assignedToId] = PROP_NAME_assignedToId;
          PROP_NAME_TO_ID.put(PROP_NAME_assignedToId, PROP_ID_assignedToId);
      
          PROP_ID_TO_NAME[PROP_ID_isOutsourced] = PROP_NAME_isOutsourced;
          PROP_NAME_TO_ID.put(PROP_NAME_isOutsourced, PROP_ID_isOutsourced);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
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
      
          PROP_ID_TO_NAME[PROP_ID_earliestStartDateT] = PROP_NAME_earliestStartDateT;
          PROP_NAME_TO_ID.put(PROP_NAME_earliestStartDateT, PROP_ID_earliestStartDateT);
      
          PROP_ID_TO_NAME[PROP_ID_latestEndDateT] = PROP_NAME_latestEndDateT;
          PROP_NAME_TO_ID.put(PROP_NAME_latestEndDateT, PROP_ID_latestEndDateT);
      
          PROP_ID_TO_NAME[PROP_ID_businessDate] = PROP_NAME_businessDate;
          PROP_NAME_TO_ID.put(PROP_NAME_businessDate, PROP_ID_businessDate);
      
    }

    
    /* ID: ID */
    private java.lang.Long _id;
    
    /* 编号: CODE */
    private java.lang.String _code;
    
    /* 主工单: WORK_ORDER_ID */
    private java.lang.Long _workOrderId;
    
    /* 工序名称: OPERATION_NAME */
    private java.lang.String _operationName;
    
    /* 工序顺序: SEQUENCE */
    private java.lang.Integer _sequence;
    
    /* 工作中心/设备: MACHINE_ID */
    private java.lang.Long _machineId;
    
    /* 优先级: PRIORITY */
    private java.lang.Integer _priority;
    
    /* 计划开工时间: PLANNED_START_DATE_T */
    private java.sql.Timestamp _plannedStartDateT;
    
    /* 计划完工时间: PLANNED_END_DATE_T */
    private java.sql.Timestamp _plannedEndDateT;
    
    /* 实际开工时间: REAL_START_DATE_T */
    private java.sql.Timestamp _realStartDateT;
    
    /* 实际完工时间: REAL_END_DATE_T */
    private java.sql.Timestamp _realEndDateT;
    
    /* 准备时间(分钟): SETUP_TIME */
    private java.math.BigDecimal _setupTime;
    
    /* 每件加工时间(分钟): RUNTIME_PER_UNIT */
    private java.math.BigDecimal _runtimePerUnit;
    
    /* 加工数量: QTY */
    private java.math.BigDecimal _qty;
    
    /* 总耗时(分钟): TOTAL_DURATION */
    private java.math.BigDecimal _totalDuration;
    
    /* 操作工: ASSIGNED_TO_ID */
    private java.lang.String _assignedToId;
    
    /* 是否外协: IS_OUTSOURCED */
    private java.lang.Boolean _isOutsourced;
    
    /* 状态: STATUS */
    private java.lang.String _status;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
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
    
    /* 最早开工时间: EARLIEST_START_DATE_T */
    private java.sql.Timestamp _earliestStartDateT;
    
    /* 最晚完工时间: LATEST_END_DATE_T */
    private java.sql.Timestamp _latestEndDateT;
    
    /* 业务日期: BUSINESS_DATE */
    private java.time.LocalDate _businessDate;
    

    public _ErpApsOperationOrder(){
        // for debug
    }

    protected ErpApsOperationOrder newInstance(){
        ErpApsOperationOrder entity = new ErpApsOperationOrder();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpApsOperationOrder cloneInstance() {
        ErpApsOperationOrder entity = newInstance();
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
      return "app.erp.aps.dao.entity.ErpApsOperationOrder";
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
        
            case PROP_ID_workOrderId:
               return getWorkOrderId();
        
            case PROP_ID_operationName:
               return getOperationName();
        
            case PROP_ID_sequence:
               return getSequence();
        
            case PROP_ID_machineId:
               return getMachineId();
        
            case PROP_ID_priority:
               return getPriority();
        
            case PROP_ID_plannedStartDateT:
               return getPlannedStartDateT();
        
            case PROP_ID_plannedEndDateT:
               return getPlannedEndDateT();
        
            case PROP_ID_realStartDateT:
               return getRealStartDateT();
        
            case PROP_ID_realEndDateT:
               return getRealEndDateT();
        
            case PROP_ID_setupTime:
               return getSetupTime();
        
            case PROP_ID_runtimePerUnit:
               return getRuntimePerUnit();
        
            case PROP_ID_qty:
               return getQty();
        
            case PROP_ID_totalDuration:
               return getTotalDuration();
        
            case PROP_ID_assignedToId:
               return getAssignedToId();
        
            case PROP_ID_isOutsourced:
               return getIsOutsourced();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_orgId:
               return getOrgId();
        
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
        
            case PROP_ID_earliestStartDateT:
               return getEarliestStartDateT();
        
            case PROP_ID_latestEndDateT:
               return getLatestEndDateT();
        
            case PROP_ID_businessDate:
               return getBusinessDate();
        
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
        
            case PROP_ID_workOrderId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_workOrderId));
               }
               setWorkOrderId(typedValue);
               break;
            }
        
            case PROP_ID_operationName:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_operationName));
               }
               setOperationName(typedValue);
               break;
            }
        
            case PROP_ID_sequence:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_sequence));
               }
               setSequence(typedValue);
               break;
            }
        
            case PROP_ID_machineId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_machineId));
               }
               setMachineId(typedValue);
               break;
            }
        
            case PROP_ID_priority:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_priority));
               }
               setPriority(typedValue);
               break;
            }
        
            case PROP_ID_plannedStartDateT:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_plannedStartDateT));
               }
               setPlannedStartDateT(typedValue);
               break;
            }
        
            case PROP_ID_plannedEndDateT:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_plannedEndDateT));
               }
               setPlannedEndDateT(typedValue);
               break;
            }
        
            case PROP_ID_realStartDateT:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_realStartDateT));
               }
               setRealStartDateT(typedValue);
               break;
            }
        
            case PROP_ID_realEndDateT:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_realEndDateT));
               }
               setRealEndDateT(typedValue);
               break;
            }
        
            case PROP_ID_setupTime:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_setupTime));
               }
               setSetupTime(typedValue);
               break;
            }
        
            case PROP_ID_runtimePerUnit:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_runtimePerUnit));
               }
               setRuntimePerUnit(typedValue);
               break;
            }
        
            case PROP_ID_qty:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_qty));
               }
               setQty(typedValue);
               break;
            }
        
            case PROP_ID_totalDuration:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_totalDuration));
               }
               setTotalDuration(typedValue);
               break;
            }
        
            case PROP_ID_assignedToId:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_assignedToId));
               }
               setAssignedToId(typedValue);
               break;
            }
        
            case PROP_ID_isOutsourced:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isOutsourced));
               }
               setIsOutsourced(typedValue);
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
        
            case PROP_ID_orgId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_orgId));
               }
               setOrgId(typedValue);
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
        
            case PROP_ID_earliestStartDateT:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_earliestStartDateT));
               }
               setEarliestStartDateT(typedValue);
               break;
            }
        
            case PROP_ID_latestEndDateT:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_latestEndDateT));
               }
               setLatestEndDateT(typedValue);
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
        
            case PROP_ID_workOrderId:{
               onInitProp(propId);
               this._workOrderId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_operationName:{
               onInitProp(propId);
               this._operationName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_sequence:{
               onInitProp(propId);
               this._sequence = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_machineId:{
               onInitProp(propId);
               this._machineId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_priority:{
               onInitProp(propId);
               this._priority = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_plannedStartDateT:{
               onInitProp(propId);
               this._plannedStartDateT = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_plannedEndDateT:{
               onInitProp(propId);
               this._plannedEndDateT = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_realStartDateT:{
               onInitProp(propId);
               this._realStartDateT = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_realEndDateT:{
               onInitProp(propId);
               this._realEndDateT = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_setupTime:{
               onInitProp(propId);
               this._setupTime = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_runtimePerUnit:{
               onInitProp(propId);
               this._runtimePerUnit = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_qty:{
               onInitProp(propId);
               this._qty = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_totalDuration:{
               onInitProp(propId);
               this._totalDuration = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_assignedToId:{
               onInitProp(propId);
               this._assignedToId = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_isOutsourced:{
               onInitProp(propId);
               this._isOutsourced = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
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
        
            case PROP_ID_earliestStartDateT:{
               onInitProp(propId);
               this._earliestStartDateT = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_latestEndDateT:{
               onInitProp(propId);
               this._latestEndDateT = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_businessDate:{
               onInitProp(propId);
               this._businessDate = (java.time.LocalDate)value;
               
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
     * 编号: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * 编号: CODE
     */
    public final void setCode(java.lang.String value){
        if(onPropSet(PROP_ID_code,value)){
            this._code = value;
            internalClearRefs(PROP_ID_code);
            
        }
    }
    
    /**
     * 主工单: WORK_ORDER_ID
     */
    public final java.lang.Long getWorkOrderId(){
         onPropGet(PROP_ID_workOrderId);
         return _workOrderId;
    }

    /**
     * 主工单: WORK_ORDER_ID
     */
    public final void setWorkOrderId(java.lang.Long value){
        if(onPropSet(PROP_ID_workOrderId,value)){
            this._workOrderId = value;
            internalClearRefs(PROP_ID_workOrderId);
            
        }
    }
    
    /**
     * 工序名称: OPERATION_NAME
     */
    public final java.lang.String getOperationName(){
         onPropGet(PROP_ID_operationName);
         return _operationName;
    }

    /**
     * 工序名称: OPERATION_NAME
     */
    public final void setOperationName(java.lang.String value){
        if(onPropSet(PROP_ID_operationName,value)){
            this._operationName = value;
            internalClearRefs(PROP_ID_operationName);
            
        }
    }
    
    /**
     * 工序顺序: SEQUENCE
     */
    public final java.lang.Integer getSequence(){
         onPropGet(PROP_ID_sequence);
         return _sequence;
    }

    /**
     * 工序顺序: SEQUENCE
     */
    public final void setSequence(java.lang.Integer value){
        if(onPropSet(PROP_ID_sequence,value)){
            this._sequence = value;
            internalClearRefs(PROP_ID_sequence);
            
        }
    }
    
    /**
     * 工作中心/设备: MACHINE_ID
     */
    public final java.lang.Long getMachineId(){
         onPropGet(PROP_ID_machineId);
         return _machineId;
    }

    /**
     * 工作中心/设备: MACHINE_ID
     */
    public final void setMachineId(java.lang.Long value){
        if(onPropSet(PROP_ID_machineId,value)){
            this._machineId = value;
            internalClearRefs(PROP_ID_machineId);
            
        }
    }
    
    /**
     * 优先级: PRIORITY
     */
    public final java.lang.Integer getPriority(){
         onPropGet(PROP_ID_priority);
         return _priority;
    }

    /**
     * 优先级: PRIORITY
     */
    public final void setPriority(java.lang.Integer value){
        if(onPropSet(PROP_ID_priority,value)){
            this._priority = value;
            internalClearRefs(PROP_ID_priority);
            
        }
    }
    
    /**
     * 计划开工时间: PLANNED_START_DATE_T
     */
    public final java.sql.Timestamp getPlannedStartDateT(){
         onPropGet(PROP_ID_plannedStartDateT);
         return _plannedStartDateT;
    }

    /**
     * 计划开工时间: PLANNED_START_DATE_T
     */
    public final void setPlannedStartDateT(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_plannedStartDateT,value)){
            this._plannedStartDateT = value;
            internalClearRefs(PROP_ID_plannedStartDateT);
            
        }
    }
    
    /**
     * 计划完工时间: PLANNED_END_DATE_T
     */
    public final java.sql.Timestamp getPlannedEndDateT(){
         onPropGet(PROP_ID_plannedEndDateT);
         return _plannedEndDateT;
    }

    /**
     * 计划完工时间: PLANNED_END_DATE_T
     */
    public final void setPlannedEndDateT(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_plannedEndDateT,value)){
            this._plannedEndDateT = value;
            internalClearRefs(PROP_ID_plannedEndDateT);
            
        }
    }
    
    /**
     * 实际开工时间: REAL_START_DATE_T
     */
    public final java.sql.Timestamp getRealStartDateT(){
         onPropGet(PROP_ID_realStartDateT);
         return _realStartDateT;
    }

    /**
     * 实际开工时间: REAL_START_DATE_T
     */
    public final void setRealStartDateT(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_realStartDateT,value)){
            this._realStartDateT = value;
            internalClearRefs(PROP_ID_realStartDateT);
            
        }
    }
    
    /**
     * 实际完工时间: REAL_END_DATE_T
     */
    public final java.sql.Timestamp getRealEndDateT(){
         onPropGet(PROP_ID_realEndDateT);
         return _realEndDateT;
    }

    /**
     * 实际完工时间: REAL_END_DATE_T
     */
    public final void setRealEndDateT(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_realEndDateT,value)){
            this._realEndDateT = value;
            internalClearRefs(PROP_ID_realEndDateT);
            
        }
    }
    
    /**
     * 准备时间(分钟): SETUP_TIME
     */
    public final java.math.BigDecimal getSetupTime(){
         onPropGet(PROP_ID_setupTime);
         return _setupTime;
    }

    /**
     * 准备时间(分钟): SETUP_TIME
     */
    public final void setSetupTime(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_setupTime,value)){
            this._setupTime = value;
            internalClearRefs(PROP_ID_setupTime);
            
        }
    }
    
    /**
     * 每件加工时间(分钟): RUNTIME_PER_UNIT
     */
    public final java.math.BigDecimal getRuntimePerUnit(){
         onPropGet(PROP_ID_runtimePerUnit);
         return _runtimePerUnit;
    }

    /**
     * 每件加工时间(分钟): RUNTIME_PER_UNIT
     */
    public final void setRuntimePerUnit(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_runtimePerUnit,value)){
            this._runtimePerUnit = value;
            internalClearRefs(PROP_ID_runtimePerUnit);
            
        }
    }
    
    /**
     * 加工数量: QTY
     */
    public final java.math.BigDecimal getQty(){
         onPropGet(PROP_ID_qty);
         return _qty;
    }

    /**
     * 加工数量: QTY
     */
    public final void setQty(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_qty,value)){
            this._qty = value;
            internalClearRefs(PROP_ID_qty);
            
        }
    }
    
    /**
     * 总耗时(分钟): TOTAL_DURATION
     */
    public final java.math.BigDecimal getTotalDuration(){
         onPropGet(PROP_ID_totalDuration);
         return _totalDuration;
    }

    /**
     * 总耗时(分钟): TOTAL_DURATION
     */
    public final void setTotalDuration(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_totalDuration,value)){
            this._totalDuration = value;
            internalClearRefs(PROP_ID_totalDuration);
            
        }
    }
    
    /**
     * 操作工: ASSIGNED_TO_ID
     */
    public final java.lang.String getAssignedToId(){
         onPropGet(PROP_ID_assignedToId);
         return _assignedToId;
    }

    /**
     * 操作工: ASSIGNED_TO_ID
     */
    public final void setAssignedToId(java.lang.String value){
        if(onPropSet(PROP_ID_assignedToId,value)){
            this._assignedToId = value;
            internalClearRefs(PROP_ID_assignedToId);
            
        }
    }
    
    /**
     * 是否外协: IS_OUTSOURCED
     */
    public final java.lang.Boolean getIsOutsourced(){
         onPropGet(PROP_ID_isOutsourced);
         return _isOutsourced;
    }

    /**
     * 是否外协: IS_OUTSOURCED
     */
    public final void setIsOutsourced(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isOutsourced,value)){
            this._isOutsourced = value;
            internalClearRefs(PROP_ID_isOutsourced);
            
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
     * 最早开工时间: EARLIEST_START_DATE_T
     */
    public final java.sql.Timestamp getEarliestStartDateT(){
         onPropGet(PROP_ID_earliestStartDateT);
         return _earliestStartDateT;
    }

    /**
     * 最早开工时间: EARLIEST_START_DATE_T
     */
    public final void setEarliestStartDateT(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_earliestStartDateT,value)){
            this._earliestStartDateT = value;
            internalClearRefs(PROP_ID_earliestStartDateT);
            
        }
    }
    
    /**
     * 最晚完工时间: LATEST_END_DATE_T
     */
    public final java.sql.Timestamp getLatestEndDateT(){
         onPropGet(PROP_ID_latestEndDateT);
         return _latestEndDateT;
    }

    /**
     * 最晚完工时间: LATEST_END_DATE_T
     */
    public final void setLatestEndDateT(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_latestEndDateT,value)){
            this._latestEndDateT = value;
            internalClearRefs(PROP_ID_latestEndDateT);
            
        }
    }
    
    /**
     * 业务日期: BUSINESS_DATE
     */
    public final java.time.LocalDate getBusinessDate(){
         onPropGet(PROP_ID_businessDate);
         return _businessDate;
    }

    /**
     * 业务日期: BUSINESS_DATE
     */
    public final void setBusinessDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_businessDate,value)){
            this._businessDate = value;
            internalClearRefs(PROP_ID_businessDate);
            
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
