package org.example;

import scales.dev.SpreadsheetUtil;
import scales.dev.Workbook;
import scales.dev.Worksheet;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

public class Main {
  private static final Instant MIN_DATE = LocalDate.of(2026, 1, 1)
          .atStartOfDay().toInstant(ZoneOffset.UTC);

  public static void main(String[] args) {
    Path salesFile = Path.of(args[0]);

    // todo: spreadsheet util should have a "readSheet" function
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
                Integer.parseInt(row.get("productPrice"))
        ));
      }
      else break;
    }

    // todo: spreadsheet util should have a "writeSheet" function

    //ExportProducts.exportAllProducts(false);
    //Path productsFile = Path.of("./products.csv");
    //Worksheet products = (Worksheet) SpreadsheetUtil.read(productsFile, true)
    //        .worksheets.values().toArray()[0];
  }

  private record Product(String name, int quantity, int price) {}
}