//__XGEN_FORCE_OVERRIDE__
    package app.erp.ct.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCtContractLineInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _contractId;

    
        @PropMeta(propId=2)
    
        public String getContractId(){
            return _contractId;
        }

        public void setContractId(String value){
            this._contractId = value;
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


        private String _description;

    
        @PropMeta(propId=5)
    
        public String getDescription(){
            return _description;
        }

        public void setDescription(String value){
            this._description = value;
        }


        private java.math.BigDecimal _quantity;

    
        @PropMeta(propId=6)
    
        public java.math.BigDecimal getQuantity(){
            return _quantity;
        }

        public void setQuantity(java.math.BigDecimal value){
            this._quantity = value;
        }


        private java.math.BigDecimal _unitPrice;

    
        @PropMeta(propId=7)
    
        public java.math.BigDecimal getUnitPrice(){
            return _unitPrice;
        }

        public void setUnitPrice(java.math.BigDecimal value){
            this._unitPrice = value;
        }


        private java.math.BigDecimal _amount;

    
        @PropMeta(propId=8)
    
        public java.math.BigDecimal getAmount(){
            return _amount;
        }

        public void setAmount(java.math.BigDecimal value){
            this._amount = value;
        }


        private String _remark;

    
        @PropMeta(propId=9)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private List<ErpCtInvoicePlanInputBean> _invoicePlans;

        public List<ErpCtInvoicePlanInputBean> getInvoicePlans(){
            return _invoicePlans;
        }

        public void setInvoicePlans(List<ErpCtInvoicePlanInputBean> value){
            this._invoicePlans = value;
        }


        private List<ErpCtConsumptionLineInputBean> _consumptionLines;

        public List<ErpCtConsumptionLineInputBean> getConsumptionLines(){
            return _consumptionLines;
        }

        public void setConsumptionLines(List<ErpCtConsumptionLineInputBean> value){
            this._consumptionLines = value;
        }


    }
