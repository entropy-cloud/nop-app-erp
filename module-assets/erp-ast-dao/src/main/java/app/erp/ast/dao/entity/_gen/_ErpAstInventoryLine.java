package app.erp.ast.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.ast.dao.entity.ErpAstInventoryLine;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  资产盘点行: erp_ast_inventory_line
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpAstInventoryLine extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 盘点单: INVENTORY_ID BIGINT */
    public static final String PROP_NAME_inventoryId = "inventoryId";
    public static final int PROP_ID_inventoryId = 2;
    
    /* 所属组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 行号: LINE_NO INTEGER */
    public static final String PROP_NAME_lineNo = "lineNo";
    public static final int PROP_ID_lineNo = 4;
    
    /* 账面资产: ASSET_ID BIGINT */
    public static final String PROP_NAME_assetId = "assetId";
    public static final int PROP_ID_assetId = 5;
    
    /* 资产编码快照: ASSET_CODE_SNAPSHOT VARCHAR */
    public static final String PROP_NAME_assetCodeSnapshot = "assetCodeSnapshot";
    public static final int PROP_ID_assetCodeSnapshot = 6;
    
    /* 资产名称快照: ASSET_NAME_SNAPSHOT VARCHAR */
    public static final String PROP_NAME_assetNameSnapshot = "assetNameSnapshot";
    public static final int PROP_ID_assetNameSnapshot = 7;
    
    /* 资产类别: CATEGORY_ID BIGINT */
    public static final String PROP_NAME_categoryId = "categoryId";
    public static final int PROP_ID_categoryId = 8;
    
    /* 账面数量: BOOK_QUANTITY INTEGER */
    public static final String PROP_NAME_bookQuantity = "bookQuantity";
    public static final int PROP_ID_bookQuantity = 9;
    
    /* 实盘数量: ACTUAL_QUANTITY INTEGER */
    public static final String PROP_NAME_actualQuantity = "actualQuantity";
    public static final int PROP_ID_actualQuantity = 10;
    
    /* 差异数量: VARIANCE_QUANTITY INTEGER */
    public static final String PROP_NAME_varianceQuantity = "varianceQuantity";
    public static final int PROP_ID_varianceQuantity = 11;
    
    /* 差异类型: VARIANCE_TYPE VARCHAR */
    public static final String PROP_NAME_varianceType = "varianceType";
    public static final int PROP_ID_varianceType = 12;
    
    /* 账面价值: BOOK_VALUE DECIMAL */
    public static final String PROP_NAME_bookValue = "bookValue";
    public static final int PROP_ID_bookValue = 13;
    
    /* 评估价值: ASSESSED_VALUE DECIMAL */
    public static final String PROP_NAME_assessedValue = "assessedValue";
    public static final int PROP_ID_assessedValue = 14;
    
    /* 差异金额: VARIANCE_AMOUNT DECIMAL */
    public static final String PROP_NAME_varianceAmount = "varianceAmount";
    public static final int PROP_ID_varianceAmount = 15;
    
    /* 差异处置: DISPOSITION VARCHAR */
    public static final String PROP_NAME_disposition = "disposition";
    public static final int PROP_ID_disposition = 16;
    
    /* 盘盈新建资产: NEW_ASSET_ID BIGINT */
    public static final String PROP_NAME_newAssetId = "newAssetId";
    public static final int PROP_ID_newAssetId = 17;
    
    /* 盘盈资本化单: CAPITALIZATION_ID BIGINT */
    public static final String PROP_NAME_capitalizationId = "capitalizationId";
    public static final int PROP_ID_capitalizationId = 18;
    
    /* 盘亏处置单: DISPOSAL_ID BIGINT */
    public static final String PROP_NAME_disposalId = "disposalId";
    public static final int PROP_ID_disposalId = 19;
    
    /* 调查备注: INVESTIGATED_REMARK VARCHAR */
    public static final String PROP_NAME_investigatedRemark = "investigatedRemark";
    public static final int PROP_ID_investigatedRemark = 20;
    
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
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 27;
    

    private static int _PROP_ID_BOUND = 28;

    
    /* relation:  */
    public static final String PROP_NAME_inventory = "inventory";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    
    /* relation:  */
    public static final String PROP_NAME_asset = "asset";
    
    /* relation:  */
    public static final String PROP_NAME_category = "category";
    
    /* relation:  */
    public static final String PROP_NAME_newAsset = "newAsset";
    
    /* relation:  */
    public static final String PROP_NAME_capitalization = "capitalization";
    
    /* relation:  */
    public static final String PROP_NAME_disposal = "disposal";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[28];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_inventoryId] = PROP_NAME_inventoryId;
          PROP_NAME_TO_ID.put(PROP_NAME_inventoryId, PROP_ID_inventoryId);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_lineNo] = PROP_NAME_lineNo;
          PROP_NAME_TO_ID.put(PROP_NAME_lineNo, PROP_ID_lineNo);
      
          PROP_ID_TO_NAME[PROP_ID_assetId] = PROP_NAME_assetId;
          PROP_NAME_TO_ID.put(PROP_NAME_assetId, PROP_ID_assetId);
      
          PROP_ID_TO_NAME[PROP_ID_assetCodeSnapshot] = PROP_NAME_assetCodeSnapshot;
          PROP_NAME_TO_ID.put(PROP_NAME_assetCodeSnapshot, PROP_ID_assetCodeSnapshot);
      
          PROP_ID_TO_NAME[PROP_ID_assetNameSnapshot] = PROP_NAME_assetNameSnapshot;
          PROP_NAME_TO_ID.put(PROP_NAME_assetNameSnapshot, PROP_ID_assetNameSnapshot);
      
          PROP_ID_TO_NAME[PROP_ID_categoryId] = PROP_NAME_categoryId;
          PROP_NAME_TO_ID.put(PROP_NAME_categoryId, PROP_ID_categoryId);
      
          PROP_ID_TO_NAME[PROP_ID_bookQuantity] = PROP_NAME_bookQuantity;
          PROP_NAME_TO_ID.put(PROP_NAME_bookQuantity, PROP_ID_bookQuantity);
      
          PROP_ID_TO_NAME[PROP_ID_actualQuantity] = PROP_NAME_actualQuantity;
          PROP_NAME_TO_ID.put(PROP_NAME_actualQuantity, PROP_ID_actualQuantity);
      
          PROP_ID_TO_NAME[PROP_ID_varianceQuantity] = PROP_NAME_varianceQuantity;
          PROP_NAME_TO_ID.put(PROP_NAME_varianceQuantity, PROP_ID_varianceQuantity);
      
          PROP_ID_TO_NAME[PROP_ID_varianceType] = PROP_NAME_varianceType;
          PROP_NAME_TO_ID.put(PROP_NAME_varianceType, PROP_ID_varianceType);
      
          PROP_ID_TO_NAME[PROP_ID_bookValue] = PROP_NAME_bookValue;
          PROP_NAME_TO_ID.put(PROP_NAME_bookValue, PROP_ID_bookValue);
      
          PROP_ID_TO_NAME[PROP_ID_assessedValue] = PROP_NAME_assessedValue;
          PROP_NAME_TO_ID.put(PROP_NAME_assessedValue, PROP_ID_assessedValue);
      
          PROP_ID_TO_NAME[PROP_ID_varianceAmount] = PROP_NAME_varianceAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_varianceAmount, PROP_ID_varianceAmount);
      
          PROP_ID_TO_NAME[PROP_ID_disposition] = PROP_NAME_disposition;
          PROP_NAME_TO_ID.put(PROP_NAME_disposition, PROP_ID_disposition);
      
          PROP_ID_TO_NAME[PROP_ID_newAssetId] = PROP_NAME_newAssetId;
          PROP_NAME_TO_ID.put(PROP_NAME_newAssetId, PROP_ID_newAssetId);
      
          PROP_ID_TO_NAME[PROP_ID_capitalizationId] = PROP_NAME_capitalizationId;
          PROP_NAME_TO_ID.put(PROP_NAME_capitalizationId, PROP_ID_capitalizationId);
      
          PROP_ID_TO_NAME[PROP_ID_disposalId] = PROP_NAME_disposalId;
          PROP_NAME_TO_ID.put(PROP_NAME_disposalId, PROP_ID_disposalId);
      
          PROP_ID_TO_NAME[PROP_ID_investigatedRemark] = PROP_NAME_investigatedRemark;
          PROP_NAME_TO_ID.put(PROP_NAME_investigatedRemark, PROP_ID_investigatedRemark);
      
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
      
          PROP_ID_TO_NAME[PROP_ID_remark] = PROP_NAME_remark;
          PROP_NAME_TO_ID.put(PROP_NAME_remark, PROP_ID_remark);
      
    }

    
    /* ID: ID */
    private java.lang.Long _id;
    
    /* 盘点单: INVENTORY_ID */
    private java.lang.Long _inventoryId;
    
    /* 所属组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 行号: LINE_NO */
    private java.lang.Integer _lineNo;
    
    /* 账面资产: ASSET_ID */
    private java.lang.Long _assetId;
    
    /* 资产编码快照: ASSET_CODE_SNAPSHOT */
    private java.lang.String _assetCodeSnapshot;
    
    /* 资产名称快照: ASSET_NAME_SNAPSHOT */
    private java.lang.String _assetNameSnapshot;
    
    /* 资产类别: CATEGORY_ID */
    private java.lang.Long _categoryId;
    
    /* 账面数量: BOOK_QUANTITY */
    private java.lang.Integer _bookQuantity;
    
    /* 实盘数量: ACTUAL_QUANTITY */
    private java.lang.Integer _actualQuantity;
    
    /* 差异数量: VARIANCE_QUANTITY */
    private java.lang.Integer _varianceQuantity;
    
    /* 差异类型: VARIANCE_TYPE */
    private java.lang.String _varianceType;
    
    /* 账面价值: BOOK_VALUE */
    private java.math.BigDecimal _bookValue;
    
    /* 评估价值: ASSESSED_VALUE */
    private java.math.BigDecimal _assessedValue;
    
    /* 差异金额: VARIANCE_AMOUNT */
    private java.math.BigDecimal _varianceAmount;
    
    /* 差异处置: DISPOSITION */
    private java.lang.String _disposition;
    
    /* 盘盈新建资产: NEW_ASSET_ID */
    private java.lang.Long _newAssetId;
    
    /* 盘盈资本化单: CAPITALIZATION_ID */
    private java.lang.Long _capitalizationId;
    
    /* 盘亏处置单: DISPOSAL_ID */
    private java.lang.Long _disposalId;
    
    /* 调查备注: INVESTIGATED_REMARK */
    private java.lang.String _investigatedRemark;
    
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
    
    /* 备注: REMARK */
    private java.lang.String _remark;
    

    public _ErpAstInventoryLine(){
        // for debug
    }

    protected ErpAstInventoryLine newInstance(){
        ErpAstInventoryLine entity = new ErpAstInventoryLine();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpAstInventoryLine cloneInstance() {
        ErpAstInventoryLine entity = newInstance();
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
      return "app.erp.ast.dao.entity.ErpAstInventoryLine";
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
        
            case PROP_ID_inventoryId:
               return getInventoryId();
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_lineNo:
               return getLineNo();
        
            case PROP_ID_assetId:
               return getAssetId();
        
            case PROP_ID_assetCodeSnapshot:
               return getAssetCodeSnapshot();
        
            case PROP_ID_assetNameSnapshot:
               return getAssetNameSnapshot();
        
            case PROP_ID_categoryId:
               return getCategoryId();
        
            case PROP_ID_bookQuantity:
               return getBookQuantity();
        
            case PROP_ID_actualQuantity:
               return getActualQuantity();
        
            case PROP_ID_varianceQuantity:
               return getVarianceQuantity();
        
            case PROP_ID_varianceType:
               return getVarianceType();
        
            case PROP_ID_bookValue:
               return getBookValue();
        
            case PROP_ID_assessedValue:
               return getAssessedValue();
        
            case PROP_ID_varianceAmount:
               return getVarianceAmount();
        
            case PROP_ID_disposition:
               return getDisposition();
        
            case PROP_ID_newAssetId:
               return getNewAssetId();
        
            case PROP_ID_capitalizationId:
               return getCapitalizationId();
        
            case PROP_ID_disposalId:
               return getDisposalId();
        
            case PROP_ID_investigatedRemark:
               return getInvestigatedRemark();
        
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
        
            case PROP_ID_remark:
               return getRemark();
        
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
        
            case PROP_ID_inventoryId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_inventoryId));
               }
               setInventoryId(typedValue);
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
        
            case PROP_ID_lineNo:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_lineNo));
               }
               setLineNo(typedValue);
               break;
            }
        
            case PROP_ID_assetId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_assetId));
               }
               setAssetId(typedValue);
               break;
            }
        
            case PROP_ID_assetCodeSnapshot:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_assetCodeSnapshot));
               }
               setAssetCodeSnapshot(typedValue);
               break;
            }
        
            case PROP_ID_assetNameSnapshot:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_assetNameSnapshot));
               }
               setAssetNameSnapshot(typedValue);
               break;
            }
        
            case PROP_ID_categoryId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_categoryId));
               }
               setCategoryId(typedValue);
               break;
            }
        
            case PROP_ID_bookQuantity:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_bookQuantity));
               }
               setBookQuantity(typedValue);
               break;
            }
        
            case PROP_ID_actualQuantity:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_actualQuantity));
               }
               setActualQuantity(typedValue);
               break;
            }
        
            case PROP_ID_varianceQuantity:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_varianceQuantity));
               }
               setVarianceQuantity(typedValue);
               break;
            }
        
            case PROP_ID_varianceType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_varianceType));
               }
               setVarianceType(typedValue);
               break;
            }
        
            case PROP_ID_bookValue:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_bookValue));
               }
               setBookValue(typedValue);
               break;
            }
        
            case PROP_ID_assessedValue:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_assessedValue));
               }
               setAssessedValue(typedValue);
               break;
            }
        
            case PROP_ID_varianceAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_varianceAmount));
               }
               setVarianceAmount(typedValue);
               break;
            }
        
            case PROP_ID_disposition:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_disposition));
               }
               setDisposition(typedValue);
               break;
            }
        
            case PROP_ID_newAssetId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_newAssetId));
               }
               setNewAssetId(typedValue);
               break;
            }
        
            case PROP_ID_capitalizationId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_capitalizationId));
               }
               setCapitalizationId(typedValue);
               break;
            }
        
            case PROP_ID_disposalId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_disposalId));
               }
               setDisposalId(typedValue);
               break;
            }
        
            case PROP_ID_investigatedRemark:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_investigatedRemark));
               }
               setInvestigatedRemark(typedValue);
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
        
            case PROP_ID_remark:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_remark));
               }
               setRemark(typedValue);
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
        
            case PROP_ID_inventoryId:{
               onInitProp(propId);
               this._inventoryId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_lineNo:{
               onInitProp(propId);
               this._lineNo = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_assetId:{
               onInitProp(propId);
               this._assetId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_assetCodeSnapshot:{
               onInitProp(propId);
               this._assetCodeSnapshot = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_assetNameSnapshot:{
               onInitProp(propId);
               this._assetNameSnapshot = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_categoryId:{
               onInitProp(propId);
               this._categoryId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_bookQuantity:{
               onInitProp(propId);
               this._bookQuantity = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_actualQuantity:{
               onInitProp(propId);
               this._actualQuantity = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_varianceQuantity:{
               onInitProp(propId);
               this._varianceQuantity = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_varianceType:{
               onInitProp(propId);
               this._varianceType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_bookValue:{
               onInitProp(propId);
               this._bookValue = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_assessedValue:{
               onInitProp(propId);
               this._assessedValue = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_varianceAmount:{
               onInitProp(propId);
               this._varianceAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_disposition:{
               onInitProp(propId);
               this._disposition = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_newAssetId:{
               onInitProp(propId);
               this._newAssetId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_capitalizationId:{
               onInitProp(propId);
               this._capitalizationId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_disposalId:{
               onInitProp(propId);
               this._disposalId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_investigatedRemark:{
               onInitProp(propId);
               this._investigatedRemark = (java.lang.String)value;
               
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
        
            case PROP_ID_remark:{
               onInitProp(propId);
               this._remark = (java.lang.String)value;
               
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
     * 盘点单: INVENTORY_ID
     */
    public final java.lang.Long getInventoryId(){
         onPropGet(PROP_ID_inventoryId);
         return _inventoryId;
    }

    /**
     * 盘点单: INVENTORY_ID
     */
    public final void setInventoryId(java.lang.Long value){
        if(onPropSet(PROP_ID_inventoryId,value)){
            this._inventoryId = value;
            internalClearRefs(PROP_ID_inventoryId);
            
        }
    }
    
    /**
     * 所属组织: ORG_ID
     */
    public final java.lang.Long getOrgId(){
         onPropGet(PROP_ID_orgId);
         return _orgId;
    }

    /**
     * 所属组织: ORG_ID
     */
    public final void setOrgId(java.lang.Long value){
        if(onPropSet(PROP_ID_orgId,value)){
            this._orgId = value;
            internalClearRefs(PROP_ID_orgId);
            
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
     * 账面资产: ASSET_ID
     */
    public final java.lang.Long getAssetId(){
         onPropGet(PROP_ID_assetId);
         return _assetId;
    }

    /**
     * 账面资产: ASSET_ID
     */
    public final void setAssetId(java.lang.Long value){
        if(onPropSet(PROP_ID_assetId,value)){
            this._assetId = value;
            internalClearRefs(PROP_ID_assetId);
            
        }
    }
    
    /**
     * 资产编码快照: ASSET_CODE_SNAPSHOT
     */
    public final java.lang.String getAssetCodeSnapshot(){
         onPropGet(PROP_ID_assetCodeSnapshot);
         return _assetCodeSnapshot;
    }

    /**
     * 资产编码快照: ASSET_CODE_SNAPSHOT
     */
    public final void setAssetCodeSnapshot(java.lang.String value){
        if(onPropSet(PROP_ID_assetCodeSnapshot,value)){
            this._assetCodeSnapshot = value;
            internalClearRefs(PROP_ID_assetCodeSnapshot);
            
        }
    }
    
    /**
     * 资产名称快照: ASSET_NAME_SNAPSHOT
     */
    public final java.lang.String getAssetNameSnapshot(){
         onPropGet(PROP_ID_assetNameSnapshot);
         return _assetNameSnapshot;
    }

    /**
     * 资产名称快照: ASSET_NAME_SNAPSHOT
     */
    public final void setAssetNameSnapshot(java.lang.String value){
        if(onPropSet(PROP_ID_assetNameSnapshot,value)){
            this._assetNameSnapshot = value;
            internalClearRefs(PROP_ID_assetNameSnapshot);
            
        }
    }
    
    /**
     * 资产类别: CATEGORY_ID
     */
    public final java.lang.Long getCategoryId(){
         onPropGet(PROP_ID_categoryId);
         return _categoryId;
    }

    /**
     * 资产类别: CATEGORY_ID
     */
    public final void setCategoryId(java.lang.Long value){
        if(onPropSet(PROP_ID_categoryId,value)){
            this._categoryId = value;
            internalClearRefs(PROP_ID_categoryId);
            
        }
    }
    
    /**
     * 账面数量: BOOK_QUANTITY
     */
    public final java.lang.Integer getBookQuantity(){
         onPropGet(PROP_ID_bookQuantity);
         return _bookQuantity;
    }

    /**
     * 账面数量: BOOK_QUANTITY
     */
    public final void setBookQuantity(java.lang.Integer value){
        if(onPropSet(PROP_ID_bookQuantity,value)){
            this._bookQuantity = value;
            internalClearRefs(PROP_ID_bookQuantity);
            
        }
    }
    
    /**
     * 实盘数量: ACTUAL_QUANTITY
     */
    public final java.lang.Integer getActualQuantity(){
         onPropGet(PROP_ID_actualQuantity);
         return _actualQuantity;
    }

    /**
     * 实盘数量: ACTUAL_QUANTITY
     */
    public final void setActualQuantity(java.lang.Integer value){
        if(onPropSet(PROP_ID_actualQuantity,value)){
            this._actualQuantity = value;
            internalClearRefs(PROP_ID_actualQuantity);
            
        }
    }
    
    /**
     * 差异数量: VARIANCE_QUANTITY
     */
    public final java.lang.Integer getVarianceQuantity(){
         onPropGet(PROP_ID_varianceQuantity);
         return _varianceQuantity;
    }

    /**
     * 差异数量: VARIANCE_QUANTITY
     */
    public final void setVarianceQuantity(java.lang.Integer value){
        if(onPropSet(PROP_ID_varianceQuantity,value)){
            this._varianceQuantity = value;
            internalClearRefs(PROP_ID_varianceQuantity);
            
        }
    }
    
    /**
     * 差异类型: VARIANCE_TYPE
     */
    public final java.lang.String getVarianceType(){
         onPropGet(PROP_ID_varianceType);
         return _varianceType;
    }

    /**
     * 差异类型: VARIANCE_TYPE
     */
    public final void setVarianceType(java.lang.String value){
        if(onPropSet(PROP_ID_varianceType,value)){
            this._varianceType = value;
            internalClearRefs(PROP_ID_varianceType);
            
        }
    }
    
    /**
     * 账面价值: BOOK_VALUE
     */
    public final java.math.BigDecimal getBookValue(){
         onPropGet(PROP_ID_bookValue);
         return _bookValue;
    }

    /**
     * 账面价值: BOOK_VALUE
     */
    public final void setBookValue(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_bookValue,value)){
            this._bookValue = value;
            internalClearRefs(PROP_ID_bookValue);
            
        }
    }
    
    /**
     * 评估价值: ASSESSED_VALUE
     */
    public final java.math.BigDecimal getAssessedValue(){
         onPropGet(PROP_ID_assessedValue);
         return _assessedValue;
    }

    /**
     * 评估价值: ASSESSED_VALUE
     */
    public final void setAssessedValue(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_assessedValue,value)){
            this._assessedValue = value;
            internalClearRefs(PROP_ID_assessedValue);
            
        }
    }
    
    /**
     * 差异金额: VARIANCE_AMOUNT
     */
    public final java.math.BigDecimal getVarianceAmount(){
         onPropGet(PROP_ID_varianceAmount);
         return _varianceAmount;
    }

    /**
     * 差异金额: VARIANCE_AMOUNT
     */
    public final void setVarianceAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_varianceAmount,value)){
            this._varianceAmount = value;
            internalClearRefs(PROP_ID_varianceAmount);
            
        }
    }
    
    /**
     * 差异处置: DISPOSITION
     */
    public final java.lang.String getDisposition(){
         onPropGet(PROP_ID_disposition);
         return _disposition;
    }

    /**
     * 差异处置: DISPOSITION
     */
    public final void setDisposition(java.lang.String value){
        if(onPropSet(PROP_ID_disposition,value)){
            this._disposition = value;
            internalClearRefs(PROP_ID_disposition);
            
        }
    }
    
    /**
     * 盘盈新建资产: NEW_ASSET_ID
     */
    public final java.lang.Long getNewAssetId(){
         onPropGet(PROP_ID_newAssetId);
         return _newAssetId;
    }

    /**
     * 盘盈新建资产: NEW_ASSET_ID
     */
    public final void setNewAssetId(java.lang.Long value){
        if(onPropSet(PROP_ID_newAssetId,value)){
            this._newAssetId = value;
            internalClearRefs(PROP_ID_newAssetId);
            
        }
    }
    
    /**
     * 盘盈资本化单: CAPITALIZATION_ID
     */
    public final java.lang.Long getCapitalizationId(){
         onPropGet(PROP_ID_capitalizationId);
         return _capitalizationId;
    }

    /**
     * 盘盈资本化单: CAPITALIZATION_ID
     */
    public final void setCapitalizationId(java.lang.Long value){
        if(onPropSet(PROP_ID_capitalizationId,value)){
            this._capitalizationId = value;
            internalClearRefs(PROP_ID_capitalizationId);
            
        }
    }
    
    /**
     * 盘亏处置单: DISPOSAL_ID
     */
    public final java.lang.Long getDisposalId(){
         onPropGet(PROP_ID_disposalId);
         return _disposalId;
    }

    /**
     * 盘亏处置单: DISPOSAL_ID
     */
    public final void setDisposalId(java.lang.Long value){
        if(onPropSet(PROP_ID_disposalId,value)){
            this._disposalId = value;
            internalClearRefs(PROP_ID_disposalId);
            
        }
    }
    
    /**
     * 调查备注: INVESTIGATED_REMARK
     */
    public final java.lang.String getInvestigatedRemark(){
         onPropGet(PROP_ID_investigatedRemark);
         return _investigatedRemark;
    }

    /**
     * 调查备注: INVESTIGATED_REMARK
     */
    public final void setInvestigatedRemark(java.lang.String value){
        if(onPropSet(PROP_ID_investigatedRemark,value)){
            this._investigatedRemark = value;
            internalClearRefs(PROP_ID_investigatedRemark);
            
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
     * 
     */
    public final app.erp.ast.dao.entity.ErpAstInventory getInventory(){
       return (app.erp.ast.dao.entity.ErpAstInventory)internalGetRefEntity(PROP_NAME_inventory);
    }

    public final void setInventory(app.erp.ast.dao.entity.ErpAstInventory refEntity){
   
           if(refEntity == null){
           
                   this.setInventoryId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_inventory, refEntity,()->{
           
                           this.setInventoryId(refEntity.getId());
                       
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
       
    /**
     * 
     */
    public final app.erp.ast.dao.entity.ErpAstAsset getAsset(){
       return (app.erp.ast.dao.entity.ErpAstAsset)internalGetRefEntity(PROP_NAME_asset);
    }

    public final void setAsset(app.erp.ast.dao.entity.ErpAstAsset refEntity){
   
           if(refEntity == null){
           
                   this.setAssetId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_asset, refEntity,()->{
           
                           this.setAssetId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.ast.dao.entity.ErpAstAssetCategory getCategory(){
       return (app.erp.ast.dao.entity.ErpAstAssetCategory)internalGetRefEntity(PROP_NAME_category);
    }

    public final void setCategory(app.erp.ast.dao.entity.ErpAstAssetCategory refEntity){
   
           if(refEntity == null){
           
                   this.setCategoryId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_category, refEntity,()->{
           
                           this.setCategoryId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.ast.dao.entity.ErpAstAsset getNewAsset(){
       return (app.erp.ast.dao.entity.ErpAstAsset)internalGetRefEntity(PROP_NAME_newAsset);
    }

    public final void setNewAsset(app.erp.ast.dao.entity.ErpAstAsset refEntity){
   
           if(refEntity == null){
           
                   this.setNewAssetId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_newAsset, refEntity,()->{
           
                           this.setNewAssetId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.ast.dao.entity.ErpAstAssetCapitalization getCapitalization(){
       return (app.erp.ast.dao.entity.ErpAstAssetCapitalization)internalGetRefEntity(PROP_NAME_capitalization);
    }

    public final void setCapitalization(app.erp.ast.dao.entity.ErpAstAssetCapitalization refEntity){
   
           if(refEntity == null){
           
                   this.setCapitalizationId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_capitalization, refEntity,()->{
           
                           this.setCapitalizationId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.ast.dao.entity.ErpAstDisposal getDisposal(){
       return (app.erp.ast.dao.entity.ErpAstDisposal)internalGetRefEntity(PROP_NAME_disposal);
    }

    public final void setDisposal(app.erp.ast.dao.entity.ErpAstDisposal refEntity){
   
           if(refEntity == null){
           
                   this.setDisposalId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_disposal, refEntity,()->{
           
                           this.setDisposalId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
