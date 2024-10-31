import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Start {
	private static List<String> input = new ArrayList<String>();
	private static String api;
	private static String cx;
	private static int count = 100;
	private static String include;
	private static String exclude;
	private static String country;
	private static String savePath = "Output/output.txt";
	private static String userAgentsPath;
	private static int maxThreads = 5;
	
    public static void main(String[] args) {
        Map<String, String> flagMap = new HashMap<>();

        // Check if any arguments were passed
        if (args.length == 0 || args[0].equals("-h")) {
            help();
            return;
        }

        // Process arguments
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            // Check if the argument is a flag
            if (arg.startsWith("-") && (i + 1) < args.length) {
                String flag = arg;
                String value = args[i + 1];
                flagMap.put(flag, value);
                i++;
            } else {
                System.err.println("Invalid flag usage for: " + arg);
                return;
            }
        }

        for (Map.Entry<String, String> entry : flagMap.entrySet()) {
        	//required
            if (entry.getKey().startsWith("-s") || entry.getKey().startsWith("--search")) {
            	input.add(entry.getValue());
            }
            else if (entry.getKey().equals("-c") || entry.getKey().equals("--count")) {
            	count = Integer.parseInt(entry.getValue());
            }
            else if (entry.getKey().equals("--api")) {
            	api = entry.getValue();
            }
            else if (entry.getKey().equals("--cx")) {
            	cx = entry.getValue();
            }
            //optional
            else if (entry.getKey().equals("--output-file")) {
            	savePath = entry.getValue();
            }
            else if (entry.getKey().equals("--useragents-file")) {
            	userAgentsPath = entry.getValue();
            }
            else if (entry.getKey().equals("--threads")) {
            	maxThreads = Integer.parseInt(entry.getValue());
            }
            else if (entry.getKey().equals("--country")) {
            	country = entry.getValue();
            }
            else if (entry.getKey().equals("--exact")) {
            	include = entry.getValue();
            }
            else if (entry.getKey().equals("--exclude")) {
            	exclude = entry.getValue();
            }
        }
        
        if (userAgentsPath != null)
        	Scrapper.setJSONFile(userAgentsPath);
        Scrapper.setAttempts(1);
        ShopifyScrapper.setThreads(maxThreads);
		ShopifyScrapper.setGoogleApi(api);
		ShopifyScrapper.setCx(cx);
        
        //verify required params are set
        if (input == null || api == null || cx == null) {
        	System.err.println("Required Paramaters Not Given");
        	System.out.println("\n");
        	help();
        }
        
    	try {
    		System.out.println("Starting");    		
    		//scrape
    		List<ShopifyScrapper> scraperList = new ArrayList<>();
    		for (String str : input) {
    			System.out.println("Searching: " + str);
    			scraperList.add(new ShopifyScrapper(str, include, exclude, country, count));
    		}
    		
    		System.out.println("Completed! Saving Data");
    		
    		//Save info
            // Create the directory if it does not exist
            File file = new File(savePath);
            File directory = file.getParentFile();
            
            if (directory != null && !directory.exists()) {
                directory.mkdirs(); // Create the directory and any necessary parent directories
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                for (ShopifyScrapper scrape : scraperList) {
                	writer.write(scrape.getSearch() + ":");
                	writer.newLine();
                	for (String item : scrape.getShops()) {
                		writer.write(item);
                		writer.newLine(); // Write each item on a new line
                	}
                }

            } catch (Exception e) {
                System.err.println("Failed to save to file. Outputting to console. Error:");
                e.printStackTrace();
                System.out.println();
                System.out.println();
                System.out.println();
                for (ShopifyScrapper scrape : scraperList) {
                	System.out.println(scrape.getSearch() + ":");
	                for (String str : scrape.getShops())
	                    System.out.println(str);
                }
            }
            
            //output stats
            System.out.println("Results:");
            for (ShopifyScrapper scrape : scraperList) {
            	System.out.println("\t" + scrape.getSearch() + ":");
                System.out.println("\t\tShops Found: " + scrape.getShops().size());
                System.out.println("\t\t% of Hits: " + scrape.getPercent());
            }
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}
    }
    
    public static void help() {
    	System.out.println("Help:");
    	
    	System.out.println("\tRequired Flags:");
    	System.out.println("\t\t-s or --search:     String to search for");
    	System.out.println("\t\t			        (Put it in quotes if its more than one word. You can pass multiple searches by using -s1, -s2 etc)");
    	System.out.println("\t\t--api:              Google Search API Key");
    	System.out.println("\t\t--cx:               Google Search Browser CX key");
    	
    	System.out.println("\tOptional Flags:");
    	System.out.println("\t\t-c or --count:      How many websites to check (Google API maxes at 100 per search) (Default is 100)");
    	System.out.println("\t\t--output-file:     Program will save information to this file in json format");
    	System.out.println("\t\t                   (Defualt is Output/output.txt)");
    	System.out.println("\t\t--useragents-file: Program will read list of user agents from this file");
    	System.out.println("\t\t                   (Defualt is Resources/userAgents.json)");
    	System.out.println("\t\t                   (Download lists from: https://www.useragents.me/)");
    	System.out.println("\t\t--threads:         How many threads to use");
    	System.out.println("\t\t                   (Defualt is 5)");
    	System.out.println("\t\t--country:         Search in specific country");
    	System.out.println("\t\t                   (Only use valid strings. See link below)");
    	System.out.println("\t\t                   (https://developers.google.com/custom-search/docs/json_api_reference#countryCollections)");
    	System.out.println("\t\t--exact:           Search for websites containing exacltly this string (applies to all searches)");
    	System.out.println("\t\t--exclude:         Search for websites that do not include this string (applies to all searches)");    	
    }
}
