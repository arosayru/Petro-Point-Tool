
package petro.point.tool;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;

public class FetchFuelPrices {
  public static Map<String, String> fetchFuelPrices() {
    String url = "https://ceypetco.gov.lk/marketing-sales/";
    Map<String, String> prices = new HashMap<>();

    try {
        Document document = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .timeout(10000)
                .get();

        Elements priceSections = document.select(".card:has(h2:matches(Petrol|Diesel))");

        for (Element section : priceSections) {
            String productName = section.select("h2").text();
            Elements details = section.select("p");
            String priceText = details.isEmpty() ? "Price not available" : details.get(0).text();

            // Extract the full price with decimal
            String price = priceText.replaceAll("[^\\d.]", "");  // Keep digits and decimal point

            // Remove the first dot (if exists)
            if (price.startsWith(".")) {
                price = price.substring(1);  // Remove the first character if it's a dot
            }

            if (productName.equalsIgnoreCase("Lanka Petrol 92 Octane")) {
                prices.put("Petrol", price);
            } else if (productName.equalsIgnoreCase("Lanka Auto Diesel")) {
                prices.put("Diesel", price);
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    return prices;
   }
}
