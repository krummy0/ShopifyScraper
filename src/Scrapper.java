import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.json.simple.*;
import org.json.simple.parser.JSONParser;


public class Scrapper {
    protected CookieManager cookie = new CookieManager();
    protected URI uri;
    protected Map<String, String> authHeaders = new HashMap<>();
    private String userAgent;
    private static String jsonPath = "Resources/UserAgents.json";
    private static JSONArray userAgents;
    private static int attempts;
    
    protected Scrapper() {
    	generateUA();
    }
    
    public static void setAttempts(int attemptsInt) {
    	attempts = attemptsInt;
    }
    
    protected String getUA() {
    	return userAgent;
    }
    
    private void generateUA() {
        JSONArray userAgents = getUserAgents();

        if (!userAgents.isEmpty()) {
            Random random = new Random();
            int randomIndex = random.nextInt(userAgents.size());
            JSONObject randomUserAgentJson = (JSONObject) userAgents.get(randomIndex);
            String userAgentTmp = (String) randomUserAgentJson.get("ua");
            
            userAgent = userAgentTmp;
        } else {
            System.err.println("No user agents found in file.");
        }
    }
    
    public static void setJSONFile(String path) {
    	jsonPath = path;
    }
    
    private static JSONArray getUserAgents() {
    	if (userAgents == null)
    		readJsonFile();
    	return userAgents;
    }
    
    private static void readJsonFile() {
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(jsonPath))) {
            String currentLine;
            while ((currentLine = br.readLine()) != null) {
                contentBuilder.append(currentLine).append("\n");
            }
            JSONParser jsonParser = new JSONParser();
            userAgents = (JSONArray) jsonParser.parse(contentBuilder.toString());
        }
        catch (Exception e) {
            System.err.println("Failed to read the UserAgent file: " + e.getMessage());
        }
    }
    
    protected void setNewAuthHeader(String name, String value) {
    	authHeaders.put(name, value);
    }
    
    protected void addAuthHeaders(Map<String, String> headers) {
    	headers.putAll(authHeaders);
    }
    
    protected void setURI(URI uri) {
    	this.uri = uri;
    }
    
    protected String getCookieHeader() {
    	return cookie.getCookieStore().getCookies().stream()
                .map(HttpCookie::toString)
                .collect(Collectors.joining("; "));
    }
    
    protected List<String> removeDuplicates(List<String> list) {
    	for (int i = 0; i < list.size(); i++) {
    		for (int j = 0; j < list.size(); j++) {
    			if (i != j && list.get(i).equals(list.get(j))) {
    				list.remove(i);
    				i = 0;
    				j = 0;
    			}
    		}	
    	}
    	return list;
    }
    
    protected void setNewCookie(HttpHeaders head) {
        List<String> cookies = head.allValues("Set-Cookie");
        for (String cookieStr : cookies) {
        	try {
        		List<HttpCookie> httpCookies = HttpCookie.parse(cookieStr);
	            for (HttpCookie httpCookie : httpCookies) {
	                cookie.getCookieStore().add(uri, httpCookie);
	            }
        	}
	        catch (IllegalArgumentException e) {
	            System.err.println("Invalid cookie string: " + cookieStr);
	        }
        }
    }
    
    protected void setNewCookie(String json) throws Exception{
    	JSONParser parser = new JSONParser();
    	setNewCookie((JSONObject) parser.parse(json));
    }
    
    protected void setNewCookie(JSONObject json) {
        for (Object key : json.keySet()) {
            String cookieName = (String) key;
            JSONObject cookieData = (JSONObject) json.get(cookieName);
            
            //make name correct
            if (cookieName.contains("_js_"))
            	cookieName = cookieName.substring(4);

            String value = (String) cookieData.get("value");
            String path = (String) cookieData.get("path");
            String domain = (String) cookieData.get("domain");
            boolean secure = (boolean) cookieData.get("secure");
            boolean httpOnly = (boolean) cookieData.get("http_only");

            // Create a new cookie
            HttpCookie tmp = new HttpCookie(cookieName, value);
            tmp.setPath(path);
            tmp.setDomain(domain);
            tmp.setSecure(secure);
            tmp.setHttpOnly(httpOnly);

            cookie.getCookieStore().add(uri, tmp);
        }
    }

    protected String getCookieValue(String cookieName) {
    	CookieStore cookieStore = cookie.getCookieStore();
        List<HttpCookie> cookies = cookieStore.get(uri);

        for (HttpCookie c : cookies) {
            if (c.getName().equals(cookieName)) {
                return c.getValue();
            }
        }
        return null; // Return null if cookie not found
    }
    
    protected static Response makeCurlRequest(String urlString, String method, String body, Map<String, String> headers) throws Exception {
    	return makeCurlRequest(urlString, method, body, headers, 0);
    }
    
    
    protected static Response makeCurlRequest(String urlString, String method, String body, Map<String, String> headers, int minChars) throws Exception {
    	//try until attempts run out or it returns a value
    	for (int i = 0; i < attempts; i++) {
    		try {
                // Build the HttpClient
                HttpClient client = HttpClient.newBuilder()
                        .build();

                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(new URI(urlString))
                        .method(method, body != null ? HttpRequest.BodyPublishers.ofString(body) : HttpRequest.BodyPublishers.noBody());

                // Add custom headers
                if (headers != null) {
                    for (Map.Entry<String, String> entry : headers.entrySet()) {
                        requestBuilder.header(entry.getKey(), entry.getValue());
                    }
                }

                HttpRequest request = requestBuilder.build();

                // Send the request and get the response
                HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                String responseBodyString;
  
                // Convert response body to String if not encoded
                responseBodyString = new String(response.body(), StandardCharsets.UTF_8);
                
                
            	if ((minChars != 0 && responseBodyString.length() > minChars)
            		|| minChars == 0)
            		return new Response(response, responseBodyString);
    		}
    		catch (IOException e) {
    			//if its this error retry
    		}
    	}
    	return null;
    }
    
    protected String randomNumberStr(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        
        for (int i = 0; i < length; i++) {
            int digit = random.nextInt(10); // Generates a random digit from 0 to 9
            sb.append(digit);
        }
        
        return sb.toString();
    }
}
