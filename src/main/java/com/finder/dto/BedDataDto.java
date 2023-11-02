package com.finder.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class BedDataDto {
    private String successTime;

    private Double percent;

    private Double otherPercent;

    private List<Integer> twoAgoList;
}