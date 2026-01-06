package com.dataGovernance.domain.constant;

import lombok.Data;

@Data
public class ProcessStats {

    private int total = 0;
    private int inserted = 0;
    private int updated = 0;
//    private int skipped = 0;

    public void incrementTotal() { total++; }
    public void incrementInserted() { inserted++; }
    public void incrementUpdated() { updated++; }
//    public void incrementSkipped() { skipped++; }

}
