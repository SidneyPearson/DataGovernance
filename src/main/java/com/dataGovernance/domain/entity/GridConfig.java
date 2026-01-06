package com.dataGovernance.domain.entity;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GridConfig {
    private String apiId;
    
    private String gridCode;
    
    private String gridName;
    
    private String street;
    
    private String area;
    
    private LocalDate startDate;
    
    private LocalDate endDate;

    public GridConfig(String apiId, String gridCode, String gridName, String street, String area) {
        this.apiId = apiId;
        this.gridCode = gridCode;
        this.gridName = gridName;
        this.street = street;
        this.area = area;
    }
}