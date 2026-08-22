//__XGEN_FORCE_OVERRIDE__
    package app.erp.pur.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpPurRequisitionLineInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _requisitionId;

    
        @PropMeta(propId=2)
    
        public String getRequisitionId(){
            return _requisitionId;
        }

        public void setRequisitionId(String value){
            this._requisitionId = value;
        }


        private Integer _lineNo;

    
        @PropMeta(propId=3)
    
        public Integer getLineNo(){
            return _lineNo;
        }

        public void setLineNo(Integer value){
            this._lineNo = value;
        }


        private String _materialId;

    
        @PropMeta(propId=4)
    
        public String getMaterialId(){
            return _materialId;
        }

        public void setMaterialId(String value){
            this._materialId = value;
        }


        private String _uoMId;

    
        @PropMeta(propId=5)
    
        public String getUoMId(){
            return _uoMId;
        }

        public void setUoMId(String value){
            this._uoMId = value;
        }


        private java.math.BigDecimal _quantity;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getQuantity(){
            return _quantity;
        }

        public void setQuantity(java.math.BigDecimal value){
            this._quantity = value;
        }


        private java.time.LocalDate _requiredDate;

    
        @PropMeta(propId=7)
    
        public java.time.LocalDate getRequiredDate(){
            return _requiredDate;
        }

        public void setRequiredDate(java.time.LocalDate value){
            this._requiredDate = value;
        }


        private String _suggestedSupplierId;

    
        @PropMeta(propId=8)
    
        public String getSuggestedSupplierId(){
            return _suggestedSupplierId;
        }

        public void setSuggestedSupplierId(String value){
            this._suggestedSupplierId = value;
        }


        private String _projectId;

    
        @PropMeta(propId=9)
    
        public String getProjectId(){
            return _projectId;
        }

        public void setProjectId(String value){
            this._projectId = value;
        }


        private String _remark;

    
        @PropMeta(propId=10)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
