package app.erp.log.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.log.dao.entity.ErpLogShipment;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  发运单: erp_log_shipment
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpLogShipment extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 单号: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 承运商ID: CARRIER_ID BIGINT */
    public static final String PROP_NAME_carrierId = "carrierId";
    public static final int PROP_ID_carrierId = 4;
    
    /* 承运商配置ID: CARRIER_CONFIG_ID BIGINT */
    public static final String PROP_NAME_carrierConfigId = "carrierConfigId";
    public static final int PROP_ID_carrierConfigId = 5;
    
    /* 关联单据类型: RELATED_BILL_TYPE VARCHAR */
    public static final String PROP_NAME_relatedBillType = "relatedBillType";
    public static final int PROP_ID_relatedBillType = 6;
    
    /* 关联单据号: RELATED_BILL_CODE VARCHAR */
    public static final String PROP_NAME_relatedBillCode = "relatedBillCode";
    public static final int PROP_ID_relatedBillCode = 7;
    
    /* 发运日期: SHIPMENT_DATE DATE */
    public static final String PROP_NAME_shipmentDate = "shipmentDate";
    public static final int PROP_ID_shipmentDate = 8;
    
    /* 运单号: TRACKING_NO VARCHAR */
    public static final String PROP_NAME_trackingNo = "trackingNo";
    public static final int PROP_ID_trackingNo = 9;
    
    /* 面单URL: LABEL_URL VARCHAR */
    public static final String PROP_NAME_labelUrl = "labelUrl";
    public static final int PROP_ID_labelUrl = 10;
    
    /* 运费: FREIGHT_AMOUNT DECIMAL */
    public static final String PROP_NAME_freightAmount = "freightAmount";
    public static final int PROP_ID_freightAmount = 11;
    
    /* 运费币种: FREIGHT_CURRENCY_ID BIGINT */
    public static final String PROP_NAME_freightCurrencyId = "freightCurrencyId";
    public static final int PROP_ID_freightCurrencyId = 12;
    
    /* 运费条款: FREIGHT_TERMS VARCHAR */
    public static final String PROP_NAME_freightTerms = "freightTerms";
    public static final int PROP_ID_freightTerms = 13;
    
    /* 运费结算状态: FREIGHT_SETTLEMENT_STATUS VARCHAR */
    public static final String PROP_NAME_freightSettlementStatus = "freightSettlementStatus";
    public static final int PROP_ID_freightSettlementStatus = 14;
    
    /* 总重量(kg): TOTAL_WEIGHT DECIMAL */
    public static final String PROP_NAME_totalWeight = "totalWeight";
    public static final int PROP_ID_totalWeight = 15;
    
    /* 总体积(m³): TOTAL_VOLUME DECIMAL */
    public static final String PROP_NAME_totalVolume = "totalVolume";
    public static final int PROP_ID_totalVolume = 16;
    
    /* 总包裹数: TOTAL_PARCELS INTEGER */
    public static final String PROP_NAME_totalParcels = "totalParcels";
    public static final int PROP_ID_totalParcels = 17;
    
    /* 收货人: RECEIVER_NAME VARCHAR */
    public static final String PROP_NAME_receiverName = "receiverName";
    public static final int PROP_ID_receiverName = 18;
    
    /* 收货人电话: RECEIVER_PHONE VARCHAR */
    public static final String PROP_NAME_receiverPhone = "receiverPhone";
    public static final int PROP_ID_receiverPhone = 19;
    
    /* 收货地址: RECEIVER_ADDRESS VARCHAR */
    public static final String PROP_NAME_receiverAddress = "receiverAddress";
    public static final int PROP_ID_receiverAddress = 20;
    
    /* 国家: RECEIVER_COUNTRY VARCHAR */
    public static final String PROP_NAME_receiverCountry = "receiverCountry";
    public static final int PROP_ID_receiverCountry = 21;
    
    /* 省份: RECEIVER_PROVINCE VARCHAR */
    public static final String PROP_NAME_receiverProvince = "receiverProvince";
    public static final int PROP_ID_receiverProvince = 22;
    
    /* 城市: RECEIVER_CITY VARCHAR */
    public static final String PROP_NAME_receiverCity = "receiverCity";
    public static final int PROP_ID_receiverCity = 23;
    
    /* 区县: RECEIVER_DISTRICT VARCHAR */
    public static final String PROP_NAME_receiverDistrict = "receiverDistrict";
    public static final int PROP_ID_receiverDistrict = 24;
    
    /* 发货人: SENDER_NAME VARCHAR */
    public static final String PROP_NAME_senderName = "senderName";
    public static final int PROP_ID_senderName = 25;
    
    /* 发货人电话: SENDER_PHONE VARCHAR */
    public static final String PROP_NAME_senderPhone = "senderPhone";
    public static final int PROP_ID_senderPhone = 26;
    
    /* 发货地址: SENDER_ADDRESS VARCHAR */
    public static final String PROP_NAME_senderAddress = "senderAddress";
    public static final int PROP_ID_senderAddress = 27;
    
    /* 预计送达日期: ESTIMATED_DELIVERY_DATE DATE */
    public static final String PROP_NAME_estimatedDeliveryDate = "estimatedDeliveryDate";
    public static final int PROP_ID_estimatedDeliveryDate = 28;
    
    /* 实际送达日期: ACTUAL_DELIVERY_DATE DATE */
    public static final String PROP_NAME_actualDeliveryDate = "actualDeliveryDate";
    public static final int PROP_ID_actualDeliveryDate = 29;
    
    /* 签收人: SIGNED_BY VARCHAR */
    public static final String PROP_NAME_signedBy = "signedBy";
    public static final int PROP_ID_signedBy = 30;
    
    /* 发货员(职员): SHIPPER_ID BIGINT */
    public static final String PROP_NAME_shipperId = "shipperId";
    public static final int PROP_ID_shipperId = 31;
    
    /* 状态: STATUS VARCHAR */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 32;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 33;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 34;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 35;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 36;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 37;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 38;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 39;
    
    /* 业务日期: BUSINESS_DATE DATE */
    public static final String PROP_NAME_businessDate = "businessDate";
    public static final int PROP_ID_businessDate = 40;
    
    /* 已过账: POSTED BOOLEAN */
    public static final String PROP_NAME_posted = "posted";
    public static final int PROP_ID_posted = 41;
    

    private static int _PROP_ID_BOUND = 42;

    
    /* relation:  */
    public static final String PROP_NAME_carrier = "carrier";
    
    /* relation:  */
    public static final String PROP_NAME_carrierConfig = "carrierConfig";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    
    /* relation:  */
    public static final String PROP_NAME_shipper = "shipper";
    
    /* relation:  */
    public static final String PROP_NAME_lines = "lines";
    
    /* relation:  */
    public static final String PROP_NAME_parcels = "parcels";
    
    /* relation:  */
    public static final String PROP_NAME_logs = "logs";
    
    /* relation:  */
    public static final String PROP_NAME_freightCurrency = "freightCurrency";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[42];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_carrierId] = PROP_NAME_carrierId;
          PROP_NAME_TO_ID.put(PROP_NAME_carrierId, PROP_ID_carrierId);
      
          PROP_ID_TO_NAME[PROP_ID_carrierConfigId] = PROP_NAME_carrierConfigId;
          PROP_NAME_TO_ID.put(PROP_NAME_carrierConfigId, PROP_ID_carrierConfigId);
      
          PROP_ID_TO_NAME[PROP_ID_relatedBillType] = PROP_NAME_relatedBillType;
          PROP_NAME_TO_ID.put(PROP_NAME_relatedBillType, PROP_ID_relatedBillType);
      
          PROP_ID_TO_NAME[PROP_ID_relatedBillCode] = PROP_NAME_relatedBillCode;
          PROP_NAME_TO_ID.put(PROP_NAME_relatedBillCode, PROP_ID_relatedBillCode);
      
          PROP_ID_TO_NAME[PROP_ID_shipmentDate] = PROP_NAME_shipmentDate;
          PROP_NAME_TO_ID.put(PROP_NAME_shipmentDate, PROP_ID_shipmentDate);
      
          PROP_ID_TO_NAME[PROP_ID_trackingNo] = PROP_NAME_trackingNo;
          PROP_NAME_TO_ID.put(PROP_NAME_trackingNo, PROP_ID_trackingNo);
      
          PROP_ID_TO_NAME[PROP_ID_labelUrl] = PROP_NAME_labelUrl;
          PROP_NAME_TO_ID.put(PROP_NAME_labelUrl, PROP_ID_labelUrl);
      
          PROP_ID_TO_NAME[PROP_ID_freightAmount] = PROP_NAME_freightAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_freightAmount, PROP_ID_freightAmount);
      
          PROP_ID_TO_NAME[PROP_ID_freightCurrencyId] = PROP_NAME_freightCurrencyId;
          PROP_NAME_TO_ID.put(PROP_NAME_freightCurrencyId, PROP_ID_freightCurrencyId);
      
          PROP_ID_TO_NAME[PROP_ID_freightTerms] = PROP_NAME_freightTerms;
          PROP_NAME_TO_ID.put(PROP_NAME_freightTerms, PROP_ID_freightTerms);
      
          PROP_ID_TO_NAME[PROP_ID_freightSettlementStatus] = PROP_NAME_freightSettlementStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_freightSettlementStatus, PROP_ID_freightSettlementStatus);
      
          PROP_ID_TO_NAME[PROP_ID_totalWeight] = PROP_NAME_totalWeight;
          PROP_NAME_TO_ID.put(PROP_NAME_totalWeight, PROP_ID_totalWeight);
      
          PROP_ID_TO_NAME[PROP_ID_totalVolume] = PROP_NAME_totalVolume;
          PROP_NAME_TO_ID.put(PROP_NAME_totalVolume, PROP_ID_totalVolume);
      
          PROP_ID_TO_NAME[PROP_ID_totalParcels] = PROP_NAME_totalParcels;
          PROP_NAME_TO_ID.put(PROP_NAME_totalParcels, PROP_ID_totalParcels);
      
          PROP_ID_TO_NAME[PROP_ID_receiverName] = PROP_NAME_receiverName;
          PROP_NAME_TO_ID.put(PROP_NAME_receiverName, PROP_ID_receiverName);
      
          PROP_ID_TO_NAME[PROP_ID_receiverPhone] = PROP_NAME_receiverPhone;
          PROP_NAME_TO_ID.put(PROP_NAME_receiverPhone, PROP_ID_receiverPhone);
      
          PROP_ID_TO_NAME[PROP_ID_receiverAddress] = PROP_NAME_receiverAddress;
          PROP_NAME_TO_ID.put(PROP_NAME_receiverAddress, PROP_ID_receiverAddress);
      
          PROP_ID_TO_NAME[PROP_ID_receiverCountry] = PROP_NAME_receiverCountry;
          PROP_NAME_TO_ID.put(PROP_NAME_receiverCountry, PROP_ID_receiverCountry);
      
          PROP_ID_TO_NAME[PROP_ID_receiverProvince] = PROP_NAME_receiverProvince;
          PROP_NAME_TO_ID.put(PROP_NAME_receiverProvince, PROP_ID_receiverProvince);
      
          PROP_ID_TO_NAME[PROP_ID_receiverCity] = PROP_NAME_receiverCity;
          PROP_NAME_TO_ID.put(PROP_NAME_receiverCity, PROP_ID_receiverCity);
      
          PROP_ID_TO_NAME[PROP_ID_receiverDistrict] = PROP_NAME_receiverDistrict;
          PROP_NAME_TO_ID.put(PROP_NAME_receiverDistrict, PROP_ID_receiverDistrict);
      
          PROP_ID_TO_NAME[PROP_ID_senderName] = PROP_NAME_senderName;
          PROP_NAME_TO_ID.put(PROP_NAME_senderName, PROP_ID_senderName);
      
          PROP_ID_TO_NAME[PROP_ID_senderPhone] = PROP_NAME_senderPhone;
          PROP_NAME_TO_ID.put(PROP_NAME_senderPhone, PROP_ID_senderPhone);
      
          PROP_ID_TO_NAME[PROP_ID_senderAddress] = PROP_NAME_senderAddress;
          PROP_NAME_TO_ID.put(PROP_NAME_senderAddress, PROP_ID_senderAddress);
      
          PROP_ID_TO_NAME[PROP_ID_estimatedDeliveryDate] = PROP_NAME_estimatedDeliveryDate;
          PROP_NAME_TO_ID.put(PROP_NAME_estimatedDeliveryDate, PROP_ID_estimatedDeliveryDate);
      
          PROP_ID_TO_NAME[PROP_ID_actualDeliveryDate] = PROP_NAME_actualDeliveryDate;
          PROP_NAME_TO_ID.put(PROP_NAME_actualDeliveryDate, PROP_ID_actualDeliveryDate);
      
          PROP_ID_TO_NAME[PROP_ID_signedBy] = PROP_NAME_signedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_signedBy, PROP_ID_signedBy);
      
          PROP_ID_TO_NAME[PROP_ID_shipperId] = PROP_NAME_shipperId;
          PROP_NAME_TO_ID.put(PROP_NAME_shipperId, PROP_ID_shipperId);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
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
      
          PROP_ID_TO_NAME[PROP_ID_businessDate] = PROP_NAME_businessDate;
          PROP_NAME_TO_ID.put(PROP_NAME_businessDate, PROP_ID_businessDate);
      
          PROP_ID_TO_NAME[PROP_ID_posted] = PROP_NAME_posted;
          PROP_NAME_TO_ID.put(PROP_NAME_posted, PROP_ID_posted);
      
    }

    
    /* ID: ID */
    private java.lang.Long _id;
    
    /* 单号: CODE */
    private java.lang.String _code;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 承运商ID: CARRIER_ID */
    private java.lang.Long _carrierId;
    
    /* 承运商配置ID: CARRIER_CONFIG_ID */
    private java.lang.Long _carrierConfigId;
    
    /* 关联单据类型: RELATED_BILL_TYPE */
    private java.lang.String _relatedBillType;
    
    /* 关联单据号: RELATED_BILL_CODE */
    private java.lang.String _relatedBillCode;
    
    /* 发运日期: SHIPMENT_DATE */
    private java.time.LocalDate _shipmentDate;
    
    /* 运单号: TRACKING_NO */
    private java.lang.String _trackingNo;
    
    /* 面单URL: LABEL_URL */
    private java.lang.String _labelUrl;
    
    /* 运费: FREIGHT_AMOUNT */
    private java.math.BigDecimal _freightAmount;
    
    /* 运费币种: FREIGHT_CURRENCY_ID */
    private java.lang.Long _freightCurrencyId;
    
    /* 运费条款: FREIGHT_TERMS */
    private java.lang.String _freightTerms;
    
    /* 运费结算状态: FREIGHT_SETTLEMENT_STATUS */
    private java.lang.String _freightSettlementStatus;
    
    /* 总重量(kg): TOTAL_WEIGHT */
    private java.math.BigDecimal _totalWeight;
    
    /* 总体积(m³): TOTAL_VOLUME */
    private java.math.BigDecimal _totalVolume;
    
    /* 总包裹数: TOTAL_PARCELS */
    private java.lang.Integer _totalParcels;
    
    /* 收货人: RECEIVER_NAME */
    private java.lang.String _receiverName;
    
    /* 收货人电话: RECEIVER_PHONE */
    private java.lang.String _receiverPhone;
    
    /* 收货地址: RECEIVER_ADDRESS */
    private java.lang.String _receiverAddress;
    
    /* 国家: RECEIVER_COUNTRY */
    private java.lang.String _receiverCountry;
    
    /* 省份: RECEIVER_PROVINCE */
    private java.lang.String _receiverProvince;
    
    /* 城市: RECEIVER_CITY */
    private java.lang.String _receiverCity;
    
    /* 区县: RECEIVER_DISTRICT */
    private java.lang.String _receiverDistrict;
    
    /* 发货人: SENDER_NAME */
    private java.lang.String _senderName;
    
    /* 发货人电话: SENDER_PHONE */
    private java.lang.String _senderPhone;
    
    /* 发货地址: SENDER_ADDRESS */
    private java.lang.String _senderAddress;
    
    /* 预计送达日期: ESTIMATED_DELIVERY_DATE */
    private java.time.LocalDate _estimatedDeliveryDate;
    
    /* 实际送达日期: ACTUAL_DELIVERY_DATE */
    private java.time.LocalDate _actualDeliveryDate;
    
    /* 签收人: SIGNED_BY */
    private java.lang.String _signedBy;
    
    /* 发货员(职员): SHIPPER_ID */
    private java.lang.Long _shipperId;
    
    /* 状态: STATUS */
    private java.lang.String _status;
    
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
    
    /* 业务日期: BUSINESS_DATE */
    private java.time.LocalDate _businessDate;
    
    /* 已过账: POSTED */
    private java.lang.Boolean _posted;
    

    public _ErpLogShipment(){
        // for debug
    }

    protected ErpLogShipment newInstance(){
        ErpLogShipment entity = new ErpLogShipment();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpLogShipment cloneInstance() {
        ErpLogShipment entity = newInstance();
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
      return "app.erp.log.dao.entity.ErpLogShipment";
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
        
            case PROP_ID_carrierId:
               return getCarrierId();
        
            case PROP_ID_carrierConfigId:
               return getCarrierConfigId();
        
            case PROP_ID_relatedBillType:
               return getRelatedBillType();
        
            case PROP_ID_relatedBillCode:
               return getRelatedBillCode();
        
            case PROP_ID_shipmentDate:
               return getShipmentDate();
        
            case PROP_ID_trackingNo:
               return getTrackingNo();
        
            case PROP_ID_labelUrl:
               return getLabelUrl();
        
            case PROP_ID_freightAmount:
               return getFreightAmount();
        
            case PROP_ID_freightCurrencyId:
               return getFreightCurrencyId();
        
            case PROP_ID_freightTerms:
               return getFreightTerms();
        
            case PROP_ID_freightSettlementStatus:
               return getFreightSettlementStatus();
        
            case PROP_ID_totalWeight:
               return getTotalWeight();
        
            case PROP_ID_totalVolume:
               return getTotalVolume();
        
            case PROP_ID_totalParcels:
               return getTotalParcels();
        
            case PROP_ID_receiverName:
               return getReceiverName();
        
            case PROP_ID_receiverPhone:
               return getReceiverPhone();
        
            case PROP_ID_receiverAddress:
               return getReceiverAddress();
        
            case PROP_ID_receiverCountry:
               return getReceiverCountry();
        
            case PROP_ID_receiverProvince:
               return getReceiverProvince();
        
            case PROP_ID_receiverCity:
               return getReceiverCity();
        
            case PROP_ID_receiverDistrict:
               return getReceiverDistrict();
        
            case PROP_ID_senderName:
               return getSenderName();
        
            case PROP_ID_senderPhone:
               return getSenderPhone();
        
            case PROP_ID_senderAddress:
               return getSenderAddress();
        
            case PROP_ID_estimatedDeliveryDate:
               return getEstimatedDeliveryDate();
        
            case PROP_ID_actualDeliveryDate:
               return getActualDeliveryDate();
        
            case PROP_ID_signedBy:
               return getSignedBy();
        
            case PROP_ID_shipperId:
               return getShipperId();
        
            case PROP_ID_status:
               return getStatus();
        
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
        
            case PROP_ID_businessDate:
               return getBusinessDate();
        
            case PROP_ID_posted:
               return getPosted();
        
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
        
            case PROP_ID_carrierId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_carrierId));
               }
               setCarrierId(typedValue);
               break;
            }
        
            case PROP_ID_carrierConfigId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_carrierConfigId));
               }
               setCarrierConfigId(typedValue);
               break;
            }
        
            case PROP_ID_relatedBillType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_relatedBillType));
               }
               setRelatedBillType(typedValue);
               break;
            }
        
            case PROP_ID_relatedBillCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_relatedBillCode));
               }
               setRelatedBillCode(typedValue);
               break;
            }
        
            case PROP_ID_shipmentDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_shipmentDate));
               }
               setShipmentDate(typedValue);
               break;
            }
        
            case PROP_ID_trackingNo:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_trackingNo));
               }
               setTrackingNo(typedValue);
               break;
            }
        
            case PROP_ID_labelUrl:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_labelUrl));
               }
               setLabelUrl(typedValue);
               break;
            }
        
            case PROP_ID_freightAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_freightAmount));
               }
               setFreightAmount(typedValue);
               break;
            }
        
            case PROP_ID_freightCurrencyId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_freightCurrencyId));
               }
               setFreightCurrencyId(typedValue);
               break;
            }
        
            case PROP_ID_freightTerms:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_freightTerms));
               }
               setFreightTerms(typedValue);
               break;
            }
        
            case PROP_ID_freightSettlementStatus:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_freightSettlementStatus));
               }
               setFreightSettlementStatus(typedValue);
               break;
            }
        
            case PROP_ID_totalWeight:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_totalWeight));
               }
               setTotalWeight(typedValue);
               break;
            }
        
            case PROP_ID_totalVolume:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_totalVolume));
               }
               setTotalVolume(typedValue);
               break;
            }
        
            case PROP_ID_totalParcels:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_totalParcels));
               }
               setTotalParcels(typedValue);
               break;
            }
        
            case PROP_ID_receiverName:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_receiverName));
               }
               setReceiverName(typedValue);
               break;
            }
        
            case PROP_ID_receiverPhone:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_receiverPhone));
               }
               setReceiverPhone(typedValue);
               break;
            }
        
            case PROP_ID_receiverAddress:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_receiverAddress));
               }
               setReceiverAddress(typedValue);
               break;
            }
        
            case PROP_ID_receiverCountry:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_receiverCountry));
               }
               setReceiverCountry(typedValue);
               break;
            }
        
            case PROP_ID_receiverProvince:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_receiverProvince));
               }
               setReceiverProvince(typedValue);
               break;
            }
        
            case PROP_ID_receiverCity:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_receiverCity));
               }
               setReceiverCity(typedValue);
               break;
            }
        
            case PROP_ID_receiverDistrict:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_receiverDistrict));
               }
               setReceiverDistrict(typedValue);
               break;
            }
        
            case PROP_ID_senderName:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_senderName));
               }
               setSenderName(typedValue);
               break;
            }
        
            case PROP_ID_senderPhone:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_senderPhone));
               }
               setSenderPhone(typedValue);
               break;
            }
        
            case PROP_ID_senderAddress:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_senderAddress));
               }
               setSenderAddress(typedValue);
               break;
            }
        
            case PROP_ID_estimatedDeliveryDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_estimatedDeliveryDate));
               }
               setEstimatedDeliveryDate(typedValue);
               break;
            }
        
            case PROP_ID_actualDeliveryDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_actualDeliveryDate));
               }
               setActualDeliveryDate(typedValue);
               break;
            }
        
            case PROP_ID_signedBy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_signedBy));
               }
               setSignedBy(typedValue);
               break;
            }
        
            case PROP_ID_shipperId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_shipperId));
               }
               setShipperId(typedValue);
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
        
            case PROP_ID_businessDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_businessDate));
               }
               setBusinessDate(typedValue);
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
        
            case PROP_ID_carrierId:{
               onInitProp(propId);
               this._carrierId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_carrierConfigId:{
               onInitProp(propId);
               this._carrierConfigId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_relatedBillType:{
               onInitProp(propId);
               this._relatedBillType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_relatedBillCode:{
               onInitProp(propId);
               this._relatedBillCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_shipmentDate:{
               onInitProp(propId);
               this._shipmentDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_trackingNo:{
               onInitProp(propId);
               this._trackingNo = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_labelUrl:{
               onInitProp(propId);
               this._labelUrl = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_freightAmount:{
               onInitProp(propId);
               this._freightAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_freightCurrencyId:{
               onInitProp(propId);
               this._freightCurrencyId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_freightTerms:{
               onInitProp(propId);
               this._freightTerms = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_freightSettlementStatus:{
               onInitProp(propId);
               this._freightSettlementStatus = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_totalWeight:{
               onInitProp(propId);
               this._totalWeight = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_totalVolume:{
               onInitProp(propId);
               this._totalVolume = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_totalParcels:{
               onInitProp(propId);
               this._totalParcels = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_receiverName:{
               onInitProp(propId);
               this._receiverName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_receiverPhone:{
               onInitProp(propId);
               this._receiverPhone = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_receiverAddress:{
               onInitProp(propId);
               this._receiverAddress = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_receiverCountry:{
               onInitProp(propId);
               this._receiverCountry = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_receiverProvince:{
               onInitProp(propId);
               this._receiverProvince = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_receiverCity:{
               onInitProp(propId);
               this._receiverCity = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_receiverDistrict:{
               onInitProp(propId);
               this._receiverDistrict = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_senderName:{
               onInitProp(propId);
               this._senderName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_senderPhone:{
               onInitProp(propId);
               this._senderPhone = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_senderAddress:{
               onInitProp(propId);
               this._senderAddress = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_estimatedDeliveryDate:{
               onInitProp(propId);
               this._estimatedDeliveryDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_actualDeliveryDate:{
               onInitProp(propId);
               this._actualDeliveryDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_signedBy:{
               onInitProp(propId);
               this._signedBy = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_shipperId:{
               onInitProp(propId);
               this._shipperId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.String)value;
               
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
        
            case PROP_ID_businessDate:{
               onInitProp(propId);
               this._businessDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_posted:{
               onInitProp(propId);
               this._posted = (java.lang.Boolean)value;
               
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
     * 承运商ID: CARRIER_ID
     */
    public final java.lang.Long getCarrierId(){
         onPropGet(PROP_ID_carrierId);
         return _carrierId;
    }

    /**
     * 承运商ID: CARRIER_ID
     */
    public final void setCarrierId(java.lang.Long value){
        if(onPropSet(PROP_ID_carrierId,value)){
            this._carrierId = value;
            internalClearRefs(PROP_ID_carrierId);
            
        }
    }
    
    /**
     * 承运商配置ID: CARRIER_CONFIG_ID
     */
    public final java.lang.Long getCarrierConfigId(){
         onPropGet(PROP_ID_carrierConfigId);
         return _carrierConfigId;
    }

    /**
     * 承运商配置ID: CARRIER_CONFIG_ID
     */
    public final void setCarrierConfigId(java.lang.Long value){
        if(onPropSet(PROP_ID_carrierConfigId,value)){
            this._carrierConfigId = value;
            internalClearRefs(PROP_ID_carrierConfigId);
            
        }
    }
    
    /**
     * 关联单据类型: RELATED_BILL_TYPE
     */
    public final java.lang.String getRelatedBillType(){
         onPropGet(PROP_ID_relatedBillType);
         return _relatedBillType;
    }

    /**
     * 关联单据类型: RELATED_BILL_TYPE
     */
    public final void setRelatedBillType(java.lang.String value){
        if(onPropSet(PROP_ID_relatedBillType,value)){
            this._relatedBillType = value;
            internalClearRefs(PROP_ID_relatedBillType);
            
        }
    }
    
    /**
     * 关联单据号: RELATED_BILL_CODE
     */
    public final java.lang.String getRelatedBillCode(){
         onPropGet(PROP_ID_relatedBillCode);
         return _relatedBillCode;
    }

    /**
     * 关联单据号: RELATED_BILL_CODE
     */
    public final void setRelatedBillCode(java.lang.String value){
        if(onPropSet(PROP_ID_relatedBillCode,value)){
            this._relatedBillCode = value;
            internalClearRefs(PROP_ID_relatedBillCode);
            
        }
    }
    
    /**
     * 发运日期: SHIPMENT_DATE
     */
    public final java.time.LocalDate getShipmentDate(){
         onPropGet(PROP_ID_shipmentDate);
         return _shipmentDate;
    }

    /**
     * 发运日期: SHIPMENT_DATE
     */
    public final void setShipmentDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_shipmentDate,value)){
            this._shipmentDate = value;
            internalClearRefs(PROP_ID_shipmentDate);
            
        }
    }
    
    /**
     * 运单号: TRACKING_NO
     */
    public final java.lang.String getTrackingNo(){
         onPropGet(PROP_ID_trackingNo);
         return _trackingNo;
    }

    /**
     * 运单号: TRACKING_NO
     */
    public final void setTrackingNo(java.lang.String value){
        if(onPropSet(PROP_ID_trackingNo,value)){
            this._trackingNo = value;
            internalClearRefs(PROP_ID_trackingNo);
            
        }
    }
    
    /**
     * 面单URL: LABEL_URL
     */
    public final java.lang.String getLabelUrl(){
         onPropGet(PROP_ID_labelUrl);
         return _labelUrl;
    }

    /**
     * 面单URL: LABEL_URL
     */
    public final void setLabelUrl(java.lang.String value){
        if(onPropSet(PROP_ID_labelUrl,value)){
            this._labelUrl = value;
            internalClearRefs(PROP_ID_labelUrl);
            
        }
    }
    
    /**
     * 运费: FREIGHT_AMOUNT
     */
    public final java.math.BigDecimal getFreightAmount(){
         onPropGet(PROP_ID_freightAmount);
         return _freightAmount;
    }

    /**
     * 运费: FREIGHT_AMOUNT
     */
    public final void setFreightAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_freightAmount,value)){
            this._freightAmount = value;
            internalClearRefs(PROP_ID_freightAmount);
            
        }
    }
    
    /**
     * 运费币种: FREIGHT_CURRENCY_ID
     */
    public final java.lang.Long getFreightCurrencyId(){
         onPropGet(PROP_ID_freightCurrencyId);
         return _freightCurrencyId;
    }

    /**
     * 运费币种: FREIGHT_CURRENCY_ID
     */
    public final void setFreightCurrencyId(java.lang.Long value){
        if(onPropSet(PROP_ID_freightCurrencyId,value)){
            this._freightCurrencyId = value;
            internalClearRefs(PROP_ID_freightCurrencyId);
            
        }
    }
    
    /**
     * 运费条款: FREIGHT_TERMS
     */
    public final java.lang.String getFreightTerms(){
         onPropGet(PROP_ID_freightTerms);
         return _freightTerms;
    }

    /**
     * 运费条款: FREIGHT_TERMS
     */
    public final void setFreightTerms(java.lang.String value){
        if(onPropSet(PROP_ID_freightTerms,value)){
            this._freightTerms = value;
            internalClearRefs(PROP_ID_freightTerms);
            
        }
    }
    
    /**
     * 运费结算状态: FREIGHT_SETTLEMENT_STATUS
     */
    public final java.lang.String getFreightSettlementStatus(){
         onPropGet(PROP_ID_freightSettlementStatus);
         return _freightSettlementStatus;
    }

    /**
     * 运费结算状态: FREIGHT_SETTLEMENT_STATUS
     */
    public final void setFreightSettlementStatus(java.lang.String value){
        if(onPropSet(PROP_ID_freightSettlementStatus,value)){
            this._freightSettlementStatus = value;
            internalClearRefs(PROP_ID_freightSettlementStatus);
            
        }
    }
    
    /**
     * 总重量(kg): TOTAL_WEIGHT
     */
    public final java.math.BigDecimal getTotalWeight(){
         onPropGet(PROP_ID_totalWeight);
         return _totalWeight;
    }

    /**
     * 总重量(kg): TOTAL_WEIGHT
     */
    public final void setTotalWeight(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_totalWeight,value)){
            this._totalWeight = value;
            internalClearRefs(PROP_ID_totalWeight);
            
        }
    }
    
    /**
     * 总体积(m³): TOTAL_VOLUME
     */
    public final java.math.BigDecimal getTotalVolume(){
         onPropGet(PROP_ID_totalVolume);
         return _totalVolume;
    }

    /**
     * 总体积(m³): TOTAL_VOLUME
     */
    public final void setTotalVolume(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_totalVolume,value)){
            this._totalVolume = value;
            internalClearRefs(PROP_ID_totalVolume);
            
        }
    }
    
    /**
     * 总包裹数: TOTAL_PARCELS
     */
    public final java.lang.Integer getTotalParcels(){
         onPropGet(PROP_ID_totalParcels);
         return _totalParcels;
    }

    /**
     * 总包裹数: TOTAL_PARCELS
     */
    public final void setTotalParcels(java.lang.Integer value){
        if(onPropSet(PROP_ID_totalParcels,value)){
            this._totalParcels = value;
            internalClearRefs(PROP_ID_totalParcels);
            
        }
    }
    
    /**
     * 收货人: RECEIVER_NAME
     */
    public final java.lang.String getReceiverName(){
         onPropGet(PROP_ID_receiverName);
         return _receiverName;
    }

    /**
     * 收货人: RECEIVER_NAME
     */
    public final void setReceiverName(java.lang.String value){
        if(onPropSet(PROP_ID_receiverName,value)){
            this._receiverName = value;
            internalClearRefs(PROP_ID_receiverName);
            
        }
    }
    
    /**
     * 收货人电话: RECEIVER_PHONE
     */
    public final java.lang.String getReceiverPhone(){
         onPropGet(PROP_ID_receiverPhone);
         return _receiverPhone;
    }

    /**
     * 收货人电话: RECEIVER_PHONE
     */
    public final void setReceiverPhone(java.lang.String value){
        if(onPropSet(PROP_ID_receiverPhone,value)){
            this._receiverPhone = value;
            internalClearRefs(PROP_ID_receiverPhone);
            
        }
    }
    
    /**
     * 收货地址: RECEIVER_ADDRESS
     */
    public final java.lang.String getReceiverAddress(){
         onPropGet(PROP_ID_receiverAddress);
         return _receiverAddress;
    }

    /**
     * 收货地址: RECEIVER_ADDRESS
     */
    public final void setReceiverAddress(java.lang.String value){
        if(onPropSet(PROP_ID_receiverAddress,value)){
            this._receiverAddress = value;
            internalClearRefs(PROP_ID_receiverAddress);
            
        }
    }
    
    /**
     * 国家: RECEIVER_COUNTRY
     */
    public final java.lang.String getReceiverCountry(){
         onPropGet(PROP_ID_receiverCountry);
         return _receiverCountry;
    }

    /**
     * 国家: RECEIVER_COUNTRY
     */
    public final void setReceiverCountry(java.lang.String value){
        if(onPropSet(PROP_ID_receiverCountry,value)){
            this._receiverCountry = value;
            internalClearRefs(PROP_ID_receiverCountry);
            
        }
    }
    
    /**
     * 省份: RECEIVER_PROVINCE
     */
    public final java.lang.String getReceiverProvince(){
         onPropGet(PROP_ID_receiverProvince);
         return _receiverProvince;
    }

    /**
     * 省份: RECEIVER_PROVINCE
     */
    public final void setReceiverProvince(java.lang.String value){
        if(onPropSet(PROP_ID_receiverProvince,value)){
            this._receiverProvince = value;
            internalClearRefs(PROP_ID_receiverProvince);
            
        }
    }
    
    /**
     * 城市: RECEIVER_CITY
     */
    public final java.lang.String getReceiverCity(){
         onPropGet(PROP_ID_receiverCity);
         return _receiverCity;
    }

    /**
     * 城市: RECEIVER_CITY
     */
    public final void setReceiverCity(java.lang.String value){
        if(onPropSet(PROP_ID_receiverCity,value)){
            this._receiverCity = value;
            internalClearRefs(PROP_ID_receiverCity);
            
        }
    }
    
    /**
     * 区县: RECEIVER_DISTRICT
     */
    public final java.lang.String getReceiverDistrict(){
         onPropGet(PROP_ID_receiverDistrict);
         return _receiverDistrict;
    }

    /**
     * 区县: RECEIVER_DISTRICT
     */
    public final void setReceiverDistrict(java.lang.String value){
        if(onPropSet(PROP_ID_receiverDistrict,value)){
            this._receiverDistrict = value;
            internalClearRefs(PROP_ID_receiverDistrict);
            
        }
    }
    
    /**
     * 发货人: SENDER_NAME
     */
    public final java.lang.String getSenderName(){
         onPropGet(PROP_ID_senderName);
         return _senderName;
    }

    /**
     * 发货人: SENDER_NAME
     */
    public final void setSenderName(java.lang.String value){
        if(onPropSet(PROP_ID_senderName,value)){
            this._senderName = value;
            internalClearRefs(PROP_ID_senderName);
            
        }
    }
    
    /**
     * 发货人电话: SENDER_PHONE
     */
    public final java.lang.String getSenderPhone(){
         onPropGet(PROP_ID_senderPhone);
         return _senderPhone;
    }

    /**
     * 发货人电话: SENDER_PHONE
     */
    public final void setSenderPhone(java.lang.String value){
        if(onPropSet(PROP_ID_senderPhone,value)){
            this._senderPhone = value;
            internalClearRefs(PROP_ID_senderPhone);
            
        }
    }
    
    /**
     * 发货地址: SENDER_ADDRESS
     */
    public final java.lang.String getSenderAddress(){
         onPropGet(PROP_ID_senderAddress);
         return _senderAddress;
    }

    /**
     * 发货地址: SENDER_ADDRESS
     */
    public final void setSenderAddress(java.lang.String value){
        if(onPropSet(PROP_ID_senderAddress,value)){
            this._senderAddress = value;
            internalClearRefs(PROP_ID_senderAddress);
            
        }
    }
    
    /**
     * 预计送达日期: ESTIMATED_DELIVERY_DATE
     */
    public final java.time.LocalDate getEstimatedDeliveryDate(){
         onPropGet(PROP_ID_estimatedDeliveryDate);
         return _estimatedDeliveryDate;
    }

    /**
     * 预计送达日期: ESTIMATED_DELIVERY_DATE
     */
    public final void setEstimatedDeliveryDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_estimatedDeliveryDate,value)){
            this._estimatedDeliveryDate = value;
            internalClearRefs(PROP_ID_estimatedDeliveryDate);
            
        }
    }
    
    /**
     * 实际送达日期: ACTUAL_DELIVERY_DATE
     */
    public final java.time.LocalDate getActualDeliveryDate(){
         onPropGet(PROP_ID_actualDeliveryDate);
         return _actualDeliveryDate;
    }

    /**
     * 实际送达日期: ACTUAL_DELIVERY_DATE
     */
    public final void setActualDeliveryDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_actualDeliveryDate,value)){
            this._actualDeliveryDate = value;
            internalClearRefs(PROP_ID_actualDeliveryDate);
            
        }
    }
    
    /**
     * 签收人: SIGNED_BY
     */
    public final java.lang.String getSignedBy(){
         onPropGet(PROP_ID_signedBy);
         return _signedBy;
    }

    /**
     * 签收人: SIGNED_BY
     */
    public final void setSignedBy(java.lang.String value){
        if(onPropSet(PROP_ID_signedBy,value)){
            this._signedBy = value;
            internalClearRefs(PROP_ID_signedBy);
            
        }
    }
    
    /**
     * 发货员(职员): SHIPPER_ID
     */
    public final java.lang.Long getShipperId(){
         onPropGet(PROP_ID_shipperId);
         return _shipperId;
    }

    /**
     * 发货员(职员): SHIPPER_ID
     */
    public final void setShipperId(java.lang.Long value){
        if(onPropSet(PROP_ID_shipperId,value)){
            this._shipperId = value;
            internalClearRefs(PROP_ID_shipperId);
            
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
     * 已过账: POSTED
     */
    public final java.lang.Boolean getPosted(){
         onPropGet(PROP_ID_posted);
         return _posted;
    }

    /**
     * 已过账: POSTED
     */
    public final void setPosted(java.lang.Boolean value){
        if(onPropSet(PROP_ID_posted,value)){
            this._posted = value;
            internalClearRefs(PROP_ID_posted);
            
        }
    }
    
    /**
     * 
     */
    public final app.erp.log.dao.entity.ErpLogCarrier getCarrier(){
       return (app.erp.log.dao.entity.ErpLogCarrier)internalGetRefEntity(PROP_NAME_carrier);
    }

    public final void setCarrier(app.erp.log.dao.entity.ErpLogCarrier refEntity){
   
           if(refEntity == null){
           
                   this.setCarrierId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_carrier, refEntity,()->{
           
                           this.setCarrierId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.log.dao.entity.ErpLogCarrierConfig getCarrierConfig(){
       return (app.erp.log.dao.entity.ErpLogCarrierConfig)internalGetRefEntity(PROP_NAME_carrierConfig);
    }

    public final void setCarrierConfig(app.erp.log.dao.entity.ErpLogCarrierConfig refEntity){
   
           if(refEntity == null){
           
                   this.setCarrierConfigId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_carrierConfig, refEntity,()->{
           
                           this.setCarrierConfigId(refEntity.getId());
                       
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
    public final app.erp.md.dao.entity.ErpMdEmployee getShipper(){
       return (app.erp.md.dao.entity.ErpMdEmployee)internalGetRefEntity(PROP_NAME_shipper);
    }

    public final void setShipper(app.erp.md.dao.entity.ErpMdEmployee refEntity){
   
           if(refEntity == null){
           
                   this.setShipperId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_shipper, refEntity,()->{
           
                           this.setShipperId(refEntity.getId());
                       
           });
           }
       
    }
       
    private final OrmEntitySet<app.erp.log.dao.entity.ErpLogShipmentLine> _lines = new OrmEntitySet<>(this, PROP_NAME_lines,
        null, null,app.erp.log.dao.entity.ErpLogShipmentLine.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.log.dao.entity.ErpLogShipmentLine> getLines(){
       return _lines;
    }
       
    private final OrmEntitySet<app.erp.log.dao.entity.ErpLogShipmentParcel> _parcels = new OrmEntitySet<>(this, PROP_NAME_parcels,
        null, null,app.erp.log.dao.entity.ErpLogShipmentParcel.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.log.dao.entity.ErpLogShipmentParcel> getParcels(){
       return _parcels;
    }
       
    private final OrmEntitySet<app.erp.log.dao.entity.ErpLogShipmentLog> _logs = new OrmEntitySet<>(this, PROP_NAME_logs,
        null, null,app.erp.log.dao.entity.ErpLogShipmentLog.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.log.dao.entity.ErpLogShipmentLog> getLogs(){
       return _logs;
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdCurrency getFreightCurrency(){
       return (app.erp.md.dao.entity.ErpMdCurrency)internalGetRefEntity(PROP_NAME_freightCurrency);
    }

    public final void setFreightCurrency(app.erp.md.dao.entity.ErpMdCurrency refEntity){
   
           if(refEntity == null){
           
                   this.setFreightCurrencyId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_freightCurrency, refEntity,()->{
           
                           this.setFreightCurrencyId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
