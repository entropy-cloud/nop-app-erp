package app.erp.drp.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.drp.dao.entity.ErpInvDrpCrossDock;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  越库执行记录: erp_inv_drp_cross_dock
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpInvDrpCrossDock extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 编号: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* DRP行: DRP_LINE_ID BIGINT */
    public static final String PROP_NAME_drpLineId = "drpLineId";
    public static final int PROP_ID_drpLineId = 4;
    
    /* 入站移动单: INBOUND_MOVE_ID BIGINT */
    public static final String PROP_NAME_inboundMoveId = "inboundMoveId";
    public static final int PROP_ID_inboundMoveId = 5;
    
    /* 出站移动单: OUTBOUND_MOVE_ID BIGINT */
    public static final String PROP_NAME_outboundMoveId = "outboundMoveId";
    public static final int PROP_ID_outboundMoveId = 6;
    
    /* 来源单据类型: SOURCE_BILL_TYPE VARCHAR */
    public static final String PROP_NAME_sourceBillType = "sourceBillType";
    public static final int PROP_ID_sourceBillType = 7;
    
    /* 来源单据号: SOURCE_BILL_CODE VARCHAR */
    public static final String PROP_NAME_sourceBillCode = "sourceBillCode";
    public static final int PROP_ID_sourceBillCode = 8;
    
    /* 目标单据类型: TARGET_BILL_TYPE VARCHAR */
    public static final String PROP_NAME_targetBillType = "targetBillType";
    public static final int PROP_ID_targetBillType = 9;
    
    /* 目标单据号: TARGET_BILL_CODE VARCHAR */
    public static final String PROP_NAME_targetBillCode = "targetBillCode";
    public static final int PROP_ID_targetBillCode = 10;
    
    /* 物料: MATERIAL_ID BIGINT */
    public static final String PROP_NAME_materialId = "materialId";
    public static final int PROP_ID_materialId = 11;
    
    /* 越库数量: QUANTITY DECIMAL */
    public static final String PROP_NAME_quantity = "quantity";
    public static final int PROP_ID_quantity = 12;
    
    /* 暂存库位: STAGING_LOCATION_ID BIGINT */
    public static final String PROP_NAME_stagingLocationId = "stagingLocationId";
    public static final int PROP_ID_stagingLocationId = 13;
    
    /* 月台时间窗口: DOCK_SLOT_TIME DATETIME */
    public static final String PROP_NAME_dockSlotTime = "dockSlotTime";
    public static final int PROP_ID_dockSlotTime = 14;
    
    /* 状态: STATUS INTEGER */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 15;
    
    /* 匹配时间: MATCHED_AT DATETIME */
    public static final String PROP_NAME_matchedAt = "matchedAt";
    public static final int PROP_ID_matchedAt = 16;
    
    /* 装车完成时间: LOADED_AT DATETIME */
    public static final String PROP_NAME_loadedAt = "loadedAt";
    public static final int PROP_ID_loadedAt = 17;
    
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

    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[25];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_drpLineId] = PROP_NAME_drpLineId;
          PROP_NAME_TO_ID.put(PROP_NAME_drpLineId, PROP_ID_drpLineId);
      
          PROP_ID_TO_NAME[PROP_ID_inboundMoveId] = PROP_NAME_inboundMoveId;
          PROP_NAME_TO_ID.put(PROP_NAME_inboundMoveId, PROP_ID_inboundMoveId);
      
          PROP_ID_TO_NAME[PROP_ID_outboundMoveId] = PROP_NAME_outboundMoveId;
          PROP_NAME_TO_ID.put(PROP_NAME_outboundMoveId, PROP_ID_outboundMoveId);
      
          PROP_ID_TO_NAME[PROP_ID_sourceBillType] = PROP_NAME_sourceBillType;
          PROP_NAME_TO_ID.put(PROP_NAME_sourceBillType, PROP_ID_sourceBillType);
      
          PROP_ID_TO_NAME[PROP_ID_sourceBillCode] = PROP_NAME_sourceBillCode;
          PROP_NAME_TO_ID.put(PROP_NAME_sourceBillCode, PROP_ID_sourceBillCode);
      
          PROP_ID_TO_NAME[PROP_ID_targetBillType] = PROP_NAME_targetBillType;
          PROP_NAME_TO_ID.put(PROP_NAME_targetBillType, PROP_ID_targetBillType);
      
          PROP_ID_TO_NAME[PROP_ID_targetBillCode] = PROP_NAME_targetBillCode;
          PROP_NAME_TO_ID.put(PROP_NAME_targetBillCode, PROP_ID_targetBillCode);
      
          PROP_ID_TO_NAME[PROP_ID_materialId] = PROP_NAME_materialId;
          PROP_NAME_TO_ID.put(PROP_NAME_materialId, PROP_ID_materialId);
      
          PROP_ID_TO_NAME[PROP_ID_quantity] = PROP_NAME_quantity;
          PROP_NAME_TO_ID.put(PROP_NAME_quantity, PROP_ID_quantity);
      
          PROP_ID_TO_NAME[PROP_ID_stagingLocationId] = PROP_NAME_stagingLocationId;
          PROP_NAME_TO_ID.put(PROP_NAME_stagingLocationId, PROP_ID_stagingLocationId);
      
          PROP_ID_TO_NAME[PROP_ID_dockSlotTime] = PROP_NAME_dockSlotTime;
          PROP_NAME_TO_ID.put(PROP_NAME_dockSlotTime, PROP_ID_dockSlotTime);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_matchedAt] = PROP_NAME_matchedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_matchedAt, PROP_ID_matchedAt);
      
          PROP_ID_TO_NAME[PROP_ID_loadedAt] = PROP_NAME_loadedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_loadedAt, PROP_ID_loadedAt);
      
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
    
    /* 编号: CODE */
    private java.lang.String _code;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* DRP行: DRP_LINE_ID */
    private java.lang.Long _drpLineId;
    
    /* 入站移动单: INBOUND_MOVE_ID */
    private java.lang.Long _inboundMoveId;
    
    /* 出站移动单: OUTBOUND_MOVE_ID */
    private java.lang.Long _outboundMoveId;
    
    /* 来源单据类型: SOURCE_BILL_TYPE */
    private java.lang.String _sourceBillType;
    
    /* 来源单据号: SOURCE_BILL_CODE */
    private java.lang.String _sourceBillCode;
    
    /* 目标单据类型: TARGET_BILL_TYPE */
    private java.lang.String _targetBillType;
    
    /* 目标单据号: TARGET_BILL_CODE */
    private java.lang.String _targetBillCode;
    
    /* 物料: MATERIAL_ID */
    private java.lang.Long _materialId;
    
    /* 越库数量: QUANTITY */
    private java.math.BigDecimal _quantity;
    
    /* 暂存库位: STAGING_LOCATION_ID */
    private java.lang.Long _stagingLocationId;
    
    /* 月台时间窗口: DOCK_SLOT_TIME */
    private java.time.LocalDateTime _dockSlotTime;
    
    /* 状态: STATUS */
    private java.lang.Integer _status;
    
    /* 匹配时间: MATCHED_AT */
    private java.time.LocalDateTime _matchedAt;
    
    /* 装车完成时间: LOADED_AT */
    private java.time.LocalDateTime _loadedAt;
    
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
    

    public _ErpInvDrpCrossDock(){
        // for debug
    }

    protected ErpInvDrpCrossDock newInstance(){
        ErpInvDrpCrossDock entity = new ErpInvDrpCrossDock();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpInvDrpCrossDock cloneInstance() {
        ErpInvDrpCrossDock entity = newInstance();
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
      return "app.erp.drp.dao.entity.ErpInvDrpCrossDock";
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
        
            case PROP_ID_drpLineId:
               return getDrpLineId();
        
            case PROP_ID_inboundMoveId:
               return getInboundMoveId();
        
            case PROP_ID_outboundMoveId:
               return getOutboundMoveId();
        
            case PROP_ID_sourceBillType:
               return getSourceBillType();
        
            case PROP_ID_sourceBillCode:
               return getSourceBillCode();
        
            case PROP_ID_targetBillType:
               return getTargetBillType();
        
            case PROP_ID_targetBillCode:
               return getTargetBillCode();
        
            case PROP_ID_materialId:
               return getMaterialId();
        
            case PROP_ID_quantity:
               return getQuantity();
        
            case PROP_ID_stagingLocationId:
               return getStagingLocationId();
        
            case PROP_ID_dockSlotTime:
               return getDockSlotTime();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_matchedAt:
               return getMatchedAt();
        
            case PROP_ID_loadedAt:
               return getLoadedAt();
        
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
        
            case PROP_ID_drpLineId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_drpLineId));
               }
               setDrpLineId(typedValue);
               break;
            }
        
            case PROP_ID_inboundMoveId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_inboundMoveId));
               }
               setInboundMoveId(typedValue);
               break;
            }
        
            case PROP_ID_outboundMoveId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_outboundMoveId));
               }
               setOutboundMoveId(typedValue);
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
        
            case PROP_ID_sourceBillCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_sourceBillCode));
               }
               setSourceBillCode(typedValue);
               break;
            }
        
            case PROP_ID_targetBillType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_targetBillType));
               }
               setTargetBillType(typedValue);
               break;
            }
        
            case PROP_ID_targetBillCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_targetBillCode));
               }
               setTargetBillCode(typedValue);
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
        
            case PROP_ID_quantity:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_quantity));
               }
               setQuantity(typedValue);
               break;
            }
        
            case PROP_ID_stagingLocationId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_stagingLocationId));
               }
               setStagingLocationId(typedValue);
               break;
            }
        
            case PROP_ID_dockSlotTime:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_dockSlotTime));
               }
               setDockSlotTime(typedValue);
               break;
            }
        
            case PROP_ID_status:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_status));
               }
               setStatus(typedValue);
               break;
            }
        
            case PROP_ID_matchedAt:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_matchedAt));
               }
               setMatchedAt(typedValue);
               break;
            }
        
            case PROP_ID_loadedAt:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_loadedAt));
               }
               setLoadedAt(typedValue);
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
        
            case PROP_ID_drpLineId:{
               onInitProp(propId);
               this._drpLineId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_inboundMoveId:{
               onInitProp(propId);
               this._inboundMoveId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_outboundMoveId:{
               onInitProp(propId);
               this._outboundMoveId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_sourceBillType:{
               onInitProp(propId);
               this._sourceBillType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_sourceBillCode:{
               onInitProp(propId);
               this._sourceBillCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_targetBillType:{
               onInitProp(propId);
               this._targetBillType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_targetBillCode:{
               onInitProp(propId);
               this._targetBillCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_materialId:{
               onInitProp(propId);
               this._materialId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_quantity:{
               onInitProp(propId);
               this._quantity = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_stagingLocationId:{
               onInitProp(propId);
               this._stagingLocationId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_dockSlotTime:{
               onInitProp(propId);
               this._dockSlotTime = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_matchedAt:{
               onInitProp(propId);
               this._matchedAt = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_loadedAt:{
               onInitProp(propId);
               this._loadedAt = (java.time.LocalDateTime)value;
               
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
     * DRP行: DRP_LINE_ID
     */
    public final java.lang.Long getDrpLineId(){
         onPropGet(PROP_ID_drpLineId);
         return _drpLineId;
    }

    /**
     * DRP行: DRP_LINE_ID
     */
    public final void setDrpLineId(java.lang.Long value){
        if(onPropSet(PROP_ID_drpLineId,value)){
            this._drpLineId = value;
            internalClearRefs(PROP_ID_drpLineId);
            
        }
    }
    
    /**
     * 入站移动单: INBOUND_MOVE_ID
     */
    public final java.lang.Long getInboundMoveId(){
         onPropGet(PROP_ID_inboundMoveId);
         return _inboundMoveId;
    }

    /**
     * 入站移动单: INBOUND_MOVE_ID
     */
    public final void setInboundMoveId(java.lang.Long value){
        if(onPropSet(PROP_ID_inboundMoveId,value)){
            this._inboundMoveId = value;
            internalClearRefs(PROP_ID_inboundMoveId);
            
        }
    }
    
    /**
     * 出站移动单: OUTBOUND_MOVE_ID
     */
    public final java.lang.Long getOutboundMoveId(){
         onPropGet(PROP_ID_outboundMoveId);
         return _outboundMoveId;
    }

    /**
     * 出站移动单: OUTBOUND_MOVE_ID
     */
    public final void setOutboundMoveId(java.lang.Long value){
        if(onPropSet(PROP_ID_outboundMoveId,value)){
            this._outboundMoveId = value;
            internalClearRefs(PROP_ID_outboundMoveId);
            
        }
    }
    
    /**
     * 来源单据类型: SOURCE_BILL_TYPE
     */
    public final java.lang.String getSourceBillType(){
         onPropGet(PROP_ID_sourceBillType);
         return _sourceBillType;
    }

    /**
     * 来源单据类型: SOURCE_BILL_TYPE
     */
    public final void setSourceBillType(java.lang.String value){
        if(onPropSet(PROP_ID_sourceBillType,value)){
            this._sourceBillType = value;
            internalClearRefs(PROP_ID_sourceBillType);
            
        }
    }
    
    /**
     * 来源单据号: SOURCE_BILL_CODE
     */
    public final java.lang.String getSourceBillCode(){
         onPropGet(PROP_ID_sourceBillCode);
         return _sourceBillCode;
    }

    /**
     * 来源单据号: SOURCE_BILL_CODE
     */
    public final void setSourceBillCode(java.lang.String value){
        if(onPropSet(PROP_ID_sourceBillCode,value)){
            this._sourceBillCode = value;
            internalClearRefs(PROP_ID_sourceBillCode);
            
        }
    }
    
    /**
     * 目标单据类型: TARGET_BILL_TYPE
     */
    public final java.lang.String getTargetBillType(){
         onPropGet(PROP_ID_targetBillType);
         return _targetBillType;
    }

    /**
     * 目标单据类型: TARGET_BILL_TYPE
     */
    public final void setTargetBillType(java.lang.String value){
        if(onPropSet(PROP_ID_targetBillType,value)){
            this._targetBillType = value;
            internalClearRefs(PROP_ID_targetBillType);
            
        }
    }
    
    /**
     * 目标单据号: TARGET_BILL_CODE
     */
    public final java.lang.String getTargetBillCode(){
         onPropGet(PROP_ID_targetBillCode);
         return _targetBillCode;
    }

    /**
     * 目标单据号: TARGET_BILL_CODE
     */
    public final void setTargetBillCode(java.lang.String value){
        if(onPropSet(PROP_ID_targetBillCode,value)){
            this._targetBillCode = value;
            internalClearRefs(PROP_ID_targetBillCode);
            
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
     * 越库数量: QUANTITY
     */
    public final java.math.BigDecimal getQuantity(){
         onPropGet(PROP_ID_quantity);
         return _quantity;
    }

    /**
     * 越库数量: QUANTITY
     */
    public final void setQuantity(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_quantity,value)){
            this._quantity = value;
            internalClearRefs(PROP_ID_quantity);
            
        }
    }
    
    /**
     * 暂存库位: STAGING_LOCATION_ID
     */
    public final java.lang.Long getStagingLocationId(){
         onPropGet(PROP_ID_stagingLocationId);
         return _stagingLocationId;
    }

    /**
     * 暂存库位: STAGING_LOCATION_ID
     */
    public final void setStagingLocationId(java.lang.Long value){
        if(onPropSet(PROP_ID_stagingLocationId,value)){
            this._stagingLocationId = value;
            internalClearRefs(PROP_ID_stagingLocationId);
            
        }
    }
    
    /**
     * 月台时间窗口: DOCK_SLOT_TIME
     */
    public final java.time.LocalDateTime getDockSlotTime(){
         onPropGet(PROP_ID_dockSlotTime);
         return _dockSlotTime;
    }

    /**
     * 月台时间窗口: DOCK_SLOT_TIME
     */
    public final void setDockSlotTime(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_dockSlotTime,value)){
            this._dockSlotTime = value;
            internalClearRefs(PROP_ID_dockSlotTime);
            
        }
    }
    
    /**
     * 状态: STATUS
     */
    public final java.lang.Integer getStatus(){
         onPropGet(PROP_ID_status);
         return _status;
    }

    /**
     * 状态: STATUS
     */
    public final void setStatus(java.lang.Integer value){
        if(onPropSet(PROP_ID_status,value)){
            this._status = value;
            internalClearRefs(PROP_ID_status);
            
        }
    }
    
    /**
     * 匹配时间: MATCHED_AT
     */
    public final java.time.LocalDateTime getMatchedAt(){
         onPropGet(PROP_ID_matchedAt);
         return _matchedAt;
    }

    /**
     * 匹配时间: MATCHED_AT
     */
    public final void setMatchedAt(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_matchedAt,value)){
            this._matchedAt = value;
            internalClearRefs(PROP_ID_matchedAt);
            
        }
    }
    
    /**
     * 装车完成时间: LOADED_AT
     */
    public final java.time.LocalDateTime getLoadedAt(){
         onPropGet(PROP_ID_loadedAt);
         return _loadedAt;
    }

    /**
     * 装车完成时间: LOADED_AT
     */
    public final void setLoadedAt(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_loadedAt,value)){
            this._loadedAt = value;
            internalClearRefs(PROP_ID_loadedAt);
            
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
    
}
// resume CPD analysis - CPD-ON
