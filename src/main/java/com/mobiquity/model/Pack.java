package com.mobiquity.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class Pack {
    int packagePrice;
    int packageWeight;
    List<String> packedItemsIndexes;
}
