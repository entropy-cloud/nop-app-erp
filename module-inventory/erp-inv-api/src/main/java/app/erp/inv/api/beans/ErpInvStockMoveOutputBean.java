//__XGEN_FORCE_OVERRIDE__
    package app.erp.inv.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import java.util.List;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpInvStockMoveOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private String _code;

    
        @PropMeta(propId=2)
    
        public String getCode(){
            return _code;
        }

        public void setCode(String value){
            this._code = value;
        }


        private String _moveType;

    
        @PropMeta(propId=3)
    
        public String getMoveType(){
            return _moveType;
        }

        public void setMoveType(String value){
            this._moveType = value;
        }


        private String _moveType_label;

    
        public String getMoveType_label(){
            return _moveType_label;
        }

        public void setMoveType_label(String value){
            this._moveType_label = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=4)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=5)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private Long _sourceWarehouseId;

    
        @PropMeta(propId=6)
    
        public Long getSourceWarehouseId(){
            return _sourceWarehouseId;
        }

        public void setSourceWarehouseId(Long value){
            this._sourceWarehouseId = value;
        }


        private Long _sourceLocationId;

    
        @PropMeta(propId=7)
    
        public Long getSourceLocationId(){
            return _sourceLocationId;
        }

        public void setSourceLocationId(Long value){
            this._sourceLocationId = value;
        }


        private Long _destWarehouseId;

    
        @PropMeta(propId=8)
    
        public Long getDestWarehouseId(){
            return _destWarehouseId;
        }

        public void setDestWarehouseId(Long value){
            this._destWarehouseId = value;
        }


        private Long _destLocationId;

    
        @PropMeta(propId=9)
    
        public Long getDestLocationId(){
            return _destLocationId;
        }

        public void setDestLocationId(Long value){
            this._destLocationId = value;
        }


        private String _docStatus;

    
        @PropMeta(propId=10)
    
        public String getDocStatus(){
            return _docStatus;
        }

        public void setDocStatus(String value){
            this._docStatus = value;
        }


        private String _docStatus_label;

    
        public String getDocStatus_label(){
            return _docStatus_label;
        }

        public void setDocStatus_label(String value){
            this._docStatus_label = value;
        }


        private String _approveStatus;

    
        @PropMeta(propId=11)
    
        public String getApproveStatus(){
            return _approveStatus;
        }

        public void setApproveStatus(String value){
            this._approveStatus = value;
        }


        private String _approveStatus_label;

    
        public String getApproveStatus_label(){
            return _approveStatus_label;
        }

        public void setApproveStatus_label(String value){
            this._approveStatus_label = value;
        }


        private Boolean _posted;

    
        @PropMeta(propId=12)
    
        public Boolean getPosted(){
            return _posted;
        }

        public void setPosted(Boolean value){
            this._posted = value;
        }


        private java.time.LocalDateTime _postedAt;

    
        @PropMeta(propId=13)
    
        public java.time.LocalDateTime getPostedAt(){
            return _postedAt;
        }

        public void setPostedAt(java.time.LocalDateTime value){
            this._postedAt = value;
        }


        private String _postedBy;

    
        @PropMeta(propId=14)
    
        public String getPostedBy(){
            return _postedBy;
        }

        public void setPostedBy(String value){
            this._postedBy = value;
        }


        private String _relatedBillType;

    
        @PropMeta(propId=15)
    
        public String getRelatedBillType(){
            return _relatedBillType;
        }

        public void setRelatedBillType(String value){
            this._relatedBillType = value;
        }


        private String _relatedBillCode;

    
        @PropMeta(propId=16)
    
        public String getRelatedBillCode(){
            return _relatedBillCode;
        }

        public void setRelatedBillCode(String value){
            this._relatedBillCode = value;
        }


        private String _remark;

    
        @PropMeta(propId=17)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=18)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=19)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=20)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=21)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=22)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=23)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Long _originMoveId;

    
        @PropMeta(propId=24)
    
        public Long getOriginMoveId(){
            return _originMoveId;
        }

        public void setOriginMoveId(Long value){
            this._originMoveId = value;
        }


        private Long _originReturnedMoveId;

    
        @PropMeta(propId=25)
    
        public Long getOriginReturnedMoveId(){
            return _originReturnedMoveId;
        }

        public void setOriginReturnedMoveId(Long value){
            this._originReturnedMoveId = value;
        }


        private Map<String,Object> _sourceWarehouse;

        public Map<String,Object> getSourceWarehouse(){
            return _sourceWarehouse;
        }

        public void setSourceWarehouse(Map<String,Object> value){
            this._sourceWarehouse = value;
        }


        private Map<String,Object> _destWarehouse;

        public Map<String,Object> getDestWarehouse(){
            return _destWarehouse;
        }

        public void setDestWarehouse(Map<String,Object> value){
            this._destWarehouse = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


        private Map<String,Object> _sourceLocation;

        public Map<String,Object> getSourceLocation(){
            return _sourceLocation;
        }

        public void setSourceLocation(Map<String,Object> value){
            this._sourceLocation = value;
        }


        private Map<String,Object> _destLocation;

        public Map<String,Object> getDestLocation(){
            return _destLocation;
        }

        public void setDestLocation(Map<String,Object> value){
            this._destLocation = value;
        }


        private Map<String,Object> _originMove;

        public Map<String,Object> getOriginMove(){
            return _originMove;
        }

        public void setOriginMove(Map<String,Object> value){
            this._originMove = value;
        }


        private Map<String,Object> _originReturnedMove;

        public Map<String,Object> getOriginReturnedMove(){
            return _originReturnedMove;
        }

        public void setOriginReturnedMove(Map<String,Object> value){
            this._originReturnedMove = value;
        }


        private List<Map<String,Object>> _lines;

        public List<Map<String,Object>> getLines(){
            return _lines;
        }

        public void setLines(List<Map<String,Object>> value){
            this._lines = value;
        }


    }
