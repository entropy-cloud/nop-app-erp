package app.erp.hr.service.entity;

import app.erp.hr.dao.entity.ErpHrSalary;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P1-CK-hr2-005 回归：readSalaryField 必须覆盖 SALARY_ITEM_CODES 全部 13 项（含
 * grossSalary/socialInsurance/housingFund/taxAmount/netSalary 5 个派生字段）——
 * 修复前 5 派生项落 default 分支恒返回 0，getComparison 对比视图合计/扣款五行数据错误
 * （plan 2026-09-11-1530-1 Phase 2）。
 */
public class TestErpHrSalarySimulationFieldReader {

    private ErpHrSalary newFullyPopulatedSalary() {
        ErpHrSalary salary = new ErpHrSalary();
        salary.setBasicSalary(new BigDecimal("100.01"));
        salary.setPositionAllowance(new BigDecimal("200.02"));
        salary.setPerformanceBonus(new BigDecimal("300.03"));
        salary.setOvertimePay(new BigDecimal("400.04"));
        salary.setMealAllowance(new BigDecimal("500.05"));
        salary.setTransportAllowance(new BigDecimal("600.06"));
        salary.setOtherAllowance(new BigDecimal("700.07"));
        salary.setGrossSalary(new BigDecimal("2800.28"));
        salary.setSocialInsurance(new BigDecimal("900.09"));
        salary.setHousingFund(new BigDecimal("800.08"));
        salary.setTaxAmount(new BigDecimal("70.07"));
        salary.setOtherDeductions(new BigDecimal("60.06"));
        salary.setNetSalary(new BigDecimal("970.97"));
        return salary;
    }

    @Test
    public void testReadSalaryFieldCoversAllItemCodes() {
        ErpHrSalarySimulationBizModel bizModel = new ErpHrSalarySimulationBizModel();
        ErpHrSalary salary = newFullyPopulatedSalary();
        for (String itemCode : ErpHrSalarySimulationBizModel.SALARY_ITEM_CODES) {
            BigDecimal value = bizModel.readSalaryField(salary, itemCode);
            assertTrue(value.signum() != 0,
                    "itemCode " + itemCode + " 读取值不应恒为 0（派生字段读取映射缺失）");
        }
        assertEquals(new BigDecimal("2800.28"), bizModel.readSalaryField(salary, "grossSalary"));
        assertEquals(new BigDecimal("900.09"), bizModel.readSalaryField(salary, "socialInsurance"));
        assertEquals(new BigDecimal("800.08"), bizModel.readSalaryField(salary, "housingFund"));
        assertEquals(new BigDecimal("70.07"), bizModel.readSalaryField(salary, "taxAmount"));
        assertEquals(new BigDecimal("970.97"), bizModel.readSalaryField(salary, "netSalary"));
    }

    @Test
    public void testReadSalaryFieldNullSafe() {
        ErpHrSalarySimulationBizModel bizModel = new ErpHrSalarySimulationBizModel();
        assertEquals(BigDecimal.ZERO, bizModel.readSalaryField(null, "basicSalary"));
        assertEquals(BigDecimal.ZERO, bizModel.readSalaryField(newFullyPopulatedSalary(), null));
        assertEquals(BigDecimal.ZERO,
                bizModel.readSalaryField(new ErpHrSalary(), "basicSalary"));
    }
}
