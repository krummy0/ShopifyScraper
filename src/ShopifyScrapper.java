import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class ShopifyScrapper extends Scrapper {
	private static String key;
	private static String cx;
	private static int threads;
	
	private String search;
	private String country;
	private String includes;
	private String excludes;
	private List<String> urls = new ArrayList<String>();
	
	private Set<String> shops = new HashSet<String>();
	
	public ShopifyScrapper(String search, String includes,
			String excludes, String country, int count) throws Exception {
		this.search = search;
		this.includes = includes;
		this.excludes = excludes;
		this.country = country;
		//search google for list of urls
		//save duplicates for purpose of maintaining correct index
		System.out.println("Getting websites from google");
		while (google()) {
			System.out.println("\rProgress: " + urls.size() + "/" + count);
			if (urls.size() >= count)
				break;
		}
		//multi thread giving to checker
        if (urls.size() > 0) {
            Set<String> urlSet = new HashSet<>(urls); // Remove duplicates
            int totalTasks = urlSet.size();
            ExecutorService executor = Executors.newFixedThreadPool(threads);

            List<Future<Void>> futures = new ArrayList<>();
            AtomicInteger completedTasks = new AtomicInteger(0);

            for (String str : urlSet) {
                futures.add(executor.submit(() -> {
                    Checker check = new Checker(str);
                    if (check.isShopify()) {
                        shops.add(check.getLink());
                    }
                    // Update progress
                    int completed = completedTasks.incrementAndGet();
                    printProgress(completed, totalTasks);
                    return null; // Return type must match Future<Void>
                }));
            }

            // Wait for all tasks to complete
            for (Future<Void> future : futures) {
            	future.get();
            }

            executor.shutdown();

            System.out.println();
        }
	}
	
    private static void printProgress(int completed, int total) {
        // Calculate the percentage completed
        int percent = (int) ((double) completed / total * 100);
        // Create a progress bar
        StringBuilder bar = new StringBuilder("[");
        int barLength = 50; // Length of the progress bar
        int progress = (int) ((double) completed / total * barLength);

        for (int i = 0; i < barLength; i++) {
            if (i < progress) {
                bar.append("=");
            } else {
                bar.append(" ");
            }
        }
        bar.append("]");

        // Print progress report on the same line
        System.out.print("\rCompleted: " + completed + "/" + total + "  | " + percent + "% " + bar);
    }
	
	public static void setGoogleApi(String apiKey) {
		key = apiKey;
	}
	
	public static void setCx(String cxKey) {
		cx = cxKey;
	}
	
	public static void setThreads(int thread) {
		threads = thread;
	}
	
	public Set<String> getShops() {
		return shops;
	}
	
	public double getPercent() {
		return shops.size() / (double)urls.size();
	}
	
	private boolean google() throws Exception {
		String url = "https://www.googleapis.com/customsearch/v1?";
		//add params
		url += "key=" + key;
		url += "&cx=" + cx;
		url += "&start=" + urls.size();
		if (country != null)
			url += "&cr=" + country;
		if(excludes != null) 
			url += "&excludeTerms=" + excludes.replaceAll(" ", "+");
		if(includes != null) 
			url += "&exactTerms=" + includes.replaceAll(" ", "+");
		url += "&q=" + search.replaceAll(" ", "+");
		
		Response response = makeCurlRequest(url, "GET", null, null);
		
		if (response.getStatusCode() == 200) {
			JSONParser parser = new JSONParser();
			JSONObject jsonFull = (JSONObject) parser.parse(response.getBody());
			JSONArray items = (JSONArray) jsonFull.get("items");
			for (int i = 0; i < items.size(); i++) {
				JSONObject obj = (JSONObject)items.get(i);
				urls.add(formatUrl((String)obj.get("link")));
			}
			return true;
		}
		return false;
	}
	
    private String formatUrl(String url) {
        // Remove leading and trailing whitespace
        url = url.trim();

        // Ensure the URL starts with "https://"
        if (!url.startsWith("https://")) {
            url = url.replace("http://", "https://");
        }

        // Remove any trailing slash
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        // Remove any subdirectories or paths
        int domainEndIndex = url.indexOf('/', 8); // Starting from index 8 to skip "https://"
        if (domainEndIndex != -1) {
            url = url.substring(0, domainEndIndex);
        }

        return url;
    }
}
