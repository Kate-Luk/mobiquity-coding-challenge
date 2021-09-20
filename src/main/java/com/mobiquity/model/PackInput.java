package com.mobiquity.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PackInput {
    private int packageWeightLimit;
    private List<Item> itemsToPack;
}
