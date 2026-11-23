package org.example;

import scales.dev.SpreadsheetUtil;
import scales.dev.Worksheet;
import scales.dev.exportproducts.ExportProducts;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
  private static final Instant MIN_DATE = LocalDate.of(2026, 1, 1)
          .atStartOfDay().toInstant(ZoneOffset.UTC);

  public static void main(String[] args) {
    Path salesFile = Path.of(args[0]);

    // todo: spreadsheet util should have a "readSheet" and "writeSheet" function
    Worksheet sales = (Worksheet) SpreadsheetUtil.read(salesFile, true)
            .worksheets.values().toArray()[0];

    ArrayList<Product> productsSold = new ArrayList<>();

    // removes irrelevant sale data
    for (Map<String, String> row : sales.rows) {
      String rowCreatedTime = row.get("createdTime");
      if (rowCreatedTime.isEmpty() || Instant.parse(rowCreatedTime).isAfter(MIN_DATE)) {
        productsSold.add(new Product(
                row.get("productName"),
                Integer.parseInt(row.get("productQuantity")),
                Double.parseDouble(row.get("productPrice"))
        ));
      }
      else break;
    }
    System.out.println("Parsed relevant sales");

    ExportProducts.exportAllProducts(false);
    Path productsFile = Path.of("./products.csv");
    Worksheet products = (Worksheet) SpreadsheetUtil.read(productsFile, true)
            .worksheets.values().toArray()[0];
    System.out.println("Parsed product data");

    Map<String, ArrayList<String>> nameToCategoriesMap = new HashMap<>();
    for (Map<String, String> row : products.rows) {
      String action = row.get("Action");
      if (!action.equals("Add Product")) continue;

      ArrayList<String> categories = new ArrayList<>();
      String mainCategory = fixCategoryName(row.get("CategoryPath"));
      if (mainCategory.isBlank()) continue;

      categories.add(fixCategoryName(row.get("CategoryPath")));
      String extraCategoryColumn = fixCategoryName(row.get("CategoryManagement"));
      if (!extraCategoryColumn.isBlank()) {
        String[] extraCategories = extraCategoryColumn.split(":");
        categories.addAll(List.of(extraCategories));
      }

      nameToCategoriesMap.put(
              row.get("Name"),
              categories
      );
    }
    System.out.println("Parsed categories");

    System.out.println(nameToCategoriesMap);
  }

  private static String fixCategoryName(String name) {
    return name
            .replace("Home > Shop Online > ", "")
            .replace("Home > Shop Online", "");
  }
  private record Product(String name, int quantity, double price) {}
}