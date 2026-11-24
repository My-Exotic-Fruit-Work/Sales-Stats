package org.example;

import scales.dev.SpreadsheetUtil;
import scales.dev.Workbook;
import scales.dev.Worksheet;
import scales.dev.exportproducts.ExportProducts;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

public class Main {
  private static final Instant MIN_DATE = LocalDate.of(2026, 1, 1)
          .atStartOfDay().toInstant(ZoneOffset.UTC);

  public static void main(String[] args) {
    Path salesFile = Path.of(args[0]);

    // todo: spreadsheet util should have a "readSheet" and "writeSheet" function
    Worksheet sales = (Worksheet) SpreadsheetUtil.read(salesFile, true)
            .worksheets.values().toArray()[0];

    HashMap<String, Product> productsSold = new HashMap<>();

    for (Map<String, String> row : sales.rows) {
      String rowCreatedTime = row.get("createdTime");
      // ignores irrelevant sale data
      if (rowCreatedTime.isEmpty() || Instant.parse(rowCreatedTime).isAfter(MIN_DATE)) {
        String name = getName(row.get("productName"));
        productsSold.putIfAbsent(name, new Product(0,0));
        Product details = productsSold.get(name);

        int quantity = Integer.parseInt(row.get("productQuantity"));
        details.quantity += quantity;
        details.price += Double.parseDouble(row.get("productPrice")) * quantity;
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

      ArrayList<String> productCategories = new ArrayList<>();
      String mainCategory = fixCategoryName(row.get("CategoryPath"));
      if (mainCategory.isBlank()) {
        mainCategory = "nocategory";
      }

      productCategories.add(mainCategory);
      String extraCategoryColumn = fixCategoryName(row.get("CategoryManagement"));
      if (!extraCategoryColumn.isBlank()) {
        String[] extraCategories = extraCategoryColumn.split(":");
        productCategories.addAll(List.of(extraCategories));
      }

      nameToCategoriesMap.put(
              getName(row.get("Name")),
              productCategories
      );
    }
    System.out.println("Parsed categories");

    Map<String, Worksheet> worksheets = new LinkedHashMap<>();

    Worksheet overall = new Worksheet(new ArrayList<>());
    worksheets.put("Overall", overall);

    HashMap<String, Product> categoryStats = new HashMap<>();
    ArrayList<Map<String, String>> categoryStatsArray = new ArrayList<>();
    worksheets.put("Categories", new Worksheet(categoryStatsArray));

    Workbook output = new Workbook(worksheets);
    for (Map.Entry<String, Product> productSet : productsSold.entrySet()) {
      String productName = productSet.getKey();
      Product productStats = productSet.getValue();

      List<String> categories = nameToCategoriesMap.get(productName);
      if (categories == null) continue;

      Map<String, String> productStatsMap = new LinkedHashMap<>();
      productStatsMap.put("Name", productName);
      productStatsMap.put("Quantity Sold", Integer.toString(productStats.quantity));
      productStatsMap.put("Sales Revenue (£)", outputRevenue(productStats.price));

      overall.rows.add(productStatsMap);

      for (String category : categories) {
        worksheets.putIfAbsent(category, new Worksheet(new ArrayList<>()));
        Worksheet worksheet = worksheets.get(category);

        worksheet.rows.add(productStatsMap);

        categoryStats.putIfAbsent(category, new Product(0,0));
        Product categorySales = categoryStats.get(category);

        categorySales.quantity += productStats.quantity;
        categorySales.price += productStats.price;

        worksheet.rows.sort(Comparator.comparingDouble(r -> -Double.parseDouble(r.get("Sales Revenue (£)"))));
      }
    }


    // my brain is NOT working, im sure theres SO much wrong with the logic behind all the code in this project
    for (Map.Entry<String, Product> category : categoryStats.entrySet()) {
      Map<String, String> productStatsMap = new LinkedHashMap<>();
      productStatsMap.put("Name", category.getKey());
      productStatsMap.put("Quantity Sold", Integer.toString(category.getValue().quantity));
      productStatsMap.put("Sales Revenue (£)", outputRevenue(category.getValue().price));

      categoryStatsArray.add(productStatsMap);
    }

    SpreadsheetUtil.write(salesFile.getParent().resolve("sales-this-year.xlsx"), output);
  }

  private static String outputRevenue(double value) {
    return String.format("%.0f", value);
  }

  private static String getName(String name) {
    return name
            .replaceAll("[^A-Za-z0-9()'\"]", " ")
            .replaceAll("\\s+", " ");
  }

  private static String fixCategoryName(String name) {
    return name
            .replace("Home > Shop Online > ", "")
            .replace("Home > Shop Online", "");
  }
  private static class Product {
    public int quantity;
    public double price;

    public Product(int quantity, double price) {
      this.quantity = quantity;
      this.price = price;
    }
  }
}