package com.mobiquity.packer;

import com.mobiquity.exception.APIException;
import com.mobiquity.model.Item;
import com.mobiquity.model.Pack;
import com.mobiquity.model.PackInput;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Packer {

  /**
   * Cache for pack by given element number and the remaining package size.
   */
  private static Map<CacheKey, Pack> sumByElementNumberAndPackSize;

  private Packer() {
  }

  /**
   * Parses the given file and solves the package challenge.
   *
   * Pack challenge description: given a package size and a list of items which can be put into a pack.
   * Each item has id, weight and price. Find the combination of items with the biggest price which fits the package size.
   *
   * @param  filePath absolute path to a file
   *
   * @return a {@code String} divided into separate lines for each input case.
   *         These lines contain items ids of the most expensive combination which fits into the given package size.
   *         If there are more than one
   * @throws APIException if incorrect parameters are passed.
   */
  public static String pack(String filePath) throws APIException {
    var lines = readItemsFromFile(filePath);
    var packInputs = new ArrayList<PackInput>();
    for (int i = 0; i < lines.length; i++) {
      packInputs.add(parsePackInput(lines[i], i));
    }
    return packInputs.stream()
            .map(Packer::packOneInput)
            .map(Packer::convertListOfIdsToString)
            .collect(Collectors.joining("\n"));
  }

  /**
   * Reads the file and splits it into separate lines.
   * @param filePath absolute path to a file
   * @return array of strings
   * @throws APIException if the file is empty.
   */
  private static String[] readItemsFromFile(String filePath) throws APIException {
    String textFromFile;
    try {
      textFromFile = Files.readString(Path.of(filePath));
    } catch (IOException e) {
      throw new APIException(e.getMessage());
    }

    if(textFromFile.isBlank()) {
      throw new APIException("The file is empty.");
    }

    return textFromFile.split("\\n");
  }

  /**
   * Parses an inputLine into a {@code PackInput} object.
   *
   * Valid input lines:
   * <blockquote><pre>
   * 81 : (1,53.38,€45) (2,88.62,€98) (3,78.48,€3) (4,72.30,€76) (5,30.18,€9) (6,46.34,€48)
   * 8 : (1,15.3,€34)
   * </pre></blockquote>
   *
   * @param  inputLine a string which represents an input case to parse.
   * @param  lineIndex an index of a line for a more informative exception message.
   * @return parsed case for the package challenge
   * @throws APIException if incorrect parameters are passed.
   */
  private static PackInput parsePackInput(String inputLine, int lineIndex) throws APIException {
    if (!inputLine.contains(":")) {
      throw new APIException("Incorrect parameters: there is no colon in the line " + lineIndex);
    }

    String[] split = inputLine.split(":");
    if (split[0].isBlank() || !StringUtils.isNumeric(split[0].trim())) {
      throw new APIException("Incorrect parameters: could not parse a pack size in the line " + lineIndex);
    }

    // Assumption: weight is a decimal number with 2 digits after the point.
    // Hence max weight of an item is <= 100, weight can be converted to an integer for more accuracy and convenience
    int packSize = (int) Double.parseDouble(split[0]) * 100;

    if (split.length < 2) {
      throw new APIException("Incorrect parameters: there is no items to pack in the line " + lineIndex);
    }
    String[] items = split[1].trim().split("\\) \\(|\\(|\\)");

    List<Item> itemsToPack = new ArrayList<>();
    try {
      for (String item : items) {
        if (!item.isBlank()) {
          itemsToPack.add(parseItemToPack(item));
        }
      }
    } catch (APIException e) {
      throw new APIException("Incorrect parameters in the line " + lineIndex + ": " + e.getMessage());
    }
    return new PackInput(packSize, itemsToPack);
  }

  /**
   * Parses item string into an {@code Item} object.
   * Valid item string examples:
   * <blockquote><pre>
   * 1,1.0,€1
   * 100,100.0,€100
   * </pre></blockquote>
   *
   * @param itemStr a string which represents an item to parse.
   * @return parsed item to pack {@code Item}
   * @throws APIException when incorrect parameters are passed.
   */
  private static Item parseItemToPack(String itemStr) throws APIException {
    String[] itemArr = itemStr.split(",");
    if (itemArr.length != 3 || !itemArr[2].startsWith("€") || !NumberUtils.isCreatable(itemArr[1]) || !NumberUtils.isCreatable(itemArr[2].substring(1))) {
      throw new APIException("Item [" + itemStr + "] has the wrong format.");
    }
    Item item = new Item();
    item.setIndex(itemArr[0]);

    // Assumption: weight is a decimal number with 2 digits after the point.
    // Hence max weight of an item is <= 100, weight can be converted to an integer for more accuracy and convenience
    item.setWeight((int) (Double.parseDouble(itemArr[1]) * 100));
    item.setCost(Integer.parseInt(itemArr[2].substring(1)));
    return item;
  }

  private static Pack packOneInput(PackInput packInput) {
    sumByElementNumberAndPackSize = new HashMap<>();
    return fit(0, packInput.getPackageWeightLimit(), packInput.getItemsToPack());
  }

  /**
   * Packs items into the {@code Pack} in a way to maximize the price of a pack but minimize the weight.
   *
   * Uses {@code Map<CacheKey, Pack> sumByElementNumberAndPackSize} to cache intermediate pack configuration by element index and remaining pack size.
   *
   * @param i index of an element to decide either take or skip
   * @param size remaining package size
   * @param items items to pack
   * @return {@code Pack} pack filled with items, it's total price, total weight and list of element indexes which are put into the pack.
   */
  private static Pack fit(int i, int size, List<Item> items) {
    if (size < 0) return new Pack(Integer.MIN_VALUE, 0, new ArrayList<>());
    if (i == items.size()) return new Pack(0, 0, new ArrayList<>());

    CacheKey key = new CacheKey(i, size);

    if (!sumByElementNumberAndPackSize.containsKey(key)) {
      Item currentItem = items.get(i);
      Pack currentIfTake = fit(i + 1, size - currentItem.getWeight(), items);

      int currentPackWeightIfTake = currentIfTake.getPackageWeight();
      int priceIfTakeCurrent = currentItem.getCost() + currentIfTake.getPackagePrice();
      int weightIfTakeCurrent = currentPackWeightIfTake + currentItem.getWeight();

      List<String> idsIfTakeCurrent = new ArrayList<>(currentIfTake.getPackedItemsIndexes());
      idsIfTakeCurrent.add(currentItem.getIndex());

      Pack take = new Pack(priceIfTakeCurrent, weightIfTakeCurrent,  idsIfTakeCurrent);
      Pack skip = fit(i + 1, size, items);

      Pack bestPriceAndWeightCombination;
      if (take.getPackagePrice() == skip.getPackagePrice()) {
        bestPriceAndWeightCombination = take.getPackageWeight() < skip.getPackageWeight() ? take : skip;
      } else {
        bestPriceAndWeightCombination = take.getPackagePrice() > skip.getPackagePrice() ? take : skip;
      }
      sumByElementNumberAndPackSize.put(key, bestPriceAndWeightCombination);
    }
    return sumByElementNumberAndPackSize.get(key);
  }

  private static String convertListOfIdsToString(Pack packedPackage) {
    if (packedPackage.getPackedItemsIndexes().isEmpty()) {
      return "-";
    } else {
      return String.join(",", packedPackage.getPackedItemsIndexes());
    }
  }

  @Data
  @AllArgsConstructor
  private static class CacheKey {
    int elementNumber;
    int size;
  }
}
