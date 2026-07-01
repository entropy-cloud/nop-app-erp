//__XGEN_FORCE_OVERRIDE__
    package app.erp.qa.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpQaInspectionInputBean extends CrudInputBase {

    
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


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Integer _inspectionType;

    
        @PropMeta(propId=4)
    
        public Integer getInspectionType(){
            return _inspectionType;
        }

        public void setInspectionType(Integer value){
            this._inspectionType = value;
        }


        private String _relatedBillType;

    
        @PropMeta(propId=5)
    
        public String getRelatedBillType(){
            return _relatedBillType;
        }

        public void setRelatedBillType(String value){
            this._relatedBillType = value;
        }


        private String _relatedBillCode;

    
        @PropMeta(propId=6)
    
        public String getRelatedBillCode(){
            return _relatedBillCode;
        }

        public void setRelatedBillCode(String value){
            this._relatedBillCode = value;
        }


        private String _relatedLineCode;

    
        @PropMeta(propId=7)
    
        public String getRelatedLineCode(){
            return _relatedLineCode;
        }

        public void setRelatedLineCode(String value){
            this._relatedLineCode = value;
        }


        private Long _materialId;

    
        @PropMeta(propId=8)
    
        public Long getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(Long value){
            this._materialId = value;
        }


        private Long _templateId;

    
        @PropMeta(propId=9)
    
        public Long getTemplateId(){
            return _templateId;
        }

        public void setTemplateId(Long value){
            this._templateId = value;
        }


        private Long _supplierId;

    
        @PropMeta(propId=10)
    
        public Long getSupplierId(){
            return _supplierId;
        }

        public void setSupplierId(Long value){
            this._supplierId = value;
        }


        private Long _warehouseId;

    
        @PropMeta(propId=11)
    
        public Long getWarehouseId(){
            return _warehouseId;
        }

        public void setWarehouseId(Long value){
            this._warehouseId = value;
        }


        private String _batchNo;

    
        @PropMeta(propId=13)
    
        public String getBatchNo(){
            return _batchNo;
        }

        public void setBatchNo(String value){
            this._batchNo = value;
        }


        private java.time.LocalDate _inspectionDate;

    
        @PropMeta(propId=14)
    
        public java.time.LocalDate getInspectionDate(){
            return _inspectionDate;
        }

        public void setInspectionDate(java.time.LocalDate value){
            this._inspectionDate = value;
        }


        private java.math.BigDecimal _lotQuantity;

    
        @PropMeta(propId=15)
    
        public java.math.BigDecimal getLotQuantity(){
            return _lotQuantity;
        }

        public void setLotQuantity(java.math.BigDecimal value){
            this._lotQuantity = value;
        }


        private java.math.BigDecimal _sampleQuantity;

    
        @PropMeta(propId=16)
    
        public java.math.BigDecimal getSampleQuantity(){
            return _sampleQuantity;
        }

        public void setSampleQuantity(java.math.BigDecimal value){
            this._sampleQuantity = value;
        }


        private Long _inspectorId;

    
        @PropMeta(propId=17)
    
        public Long getInspectorId(){
            return _inspectorId;
        }

        public void setInspectorId(Long value){
            this._inspectorId = value;
        }


        private Integer _result;

    
        @PropMeta(propId=18)
    
        public Integer getResult(){
            return _result;
        }

        public void setResult(Integer value){
            this._result = value;
        }


        private Integer _docStatus;

    
        @PropMeta(propId=19)
    
        public Integer getDocStatus(){
            return _docStatus;
        }

        public void setDocStatus(Integer value){
            this._docStatus = value;
        }


        private Integer _approveStatus;

    
        @PropMeta(propId=20)
    
        public Integer getApproveStatus(){
            return _approveStatus;
        }

        public void setApproveStatus(Integer value){
            this._approveStatus = value;
        }


        private Boolean _posted;

    
        @PropMeta(propId=21)
    
        public Boolean getPosted(){
            return _posted;
        }

        public void setPosted(Boolean value){
            this._posted = value;
        }


        private java.time.LocalDateTime _postedAt;

    
        @PropMeta(propId=22)
    
        public java.time.LocalDateTime getPostedAt(){
            return _postedAt;
        }

        public void setPostedAt(java.time.LocalDateTime value){
            this._postedAt = value;
        }


        private String _postedBy;

    
        @PropMeta(propId=23)
    
        public String getPostedBy(){
            return _postedBy;
        }

        public void setPostedBy(String value){
            this._postedBy = value;
        }


        private String _approvedBy;

    
        @PropMeta(propId=24)
    
        public String getApprovedBy(){
            return _approvedBy;
        }

        public void setApprovedBy(String value){
            this._approvedBy = value;
        }


        private java.time.LocalDateTime _approvedAt;

    
        @PropMeta(propId=25)
    
        public java.time.LocalDateTime getApprovedAt(){
            return _approvedAt;
        }

        public void setApprovedAt(java.time.LocalDateTime value){
            this._approvedAt = value;
        }


        private String _remark;

    
        @PropMeta(propId=26)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=27)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=33)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private List<ErpQaInspectionLineInputBean> _lines;

        public List<ErpQaInspectionLineInputBean> getLines(){
            return _lines;
        }

        public void setLines(List<ErpQaInspectionLineInputBean> value){
            this._lines = value;
        }


    }
