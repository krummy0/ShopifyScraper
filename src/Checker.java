import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class Checker extends Scrapper {
	private final boolean valid;
	private final String link;
	
	public Checker(String url) throws Exception {
		super();
		setURI(URI.create(url));
		link = url;
		valid = setUp();
	}
	
	public boolean isShopify() {
		return valid;
	}
	
	public String getLink() {
		return link;
	}
	
	private boolean setUp() throws Exception {
        String url = link + "/admin/";
        String method = "GET";

        //headers
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");
        headers.put("Accept-Encoding", "");
        headers.put("Accept-Language", "en-US,en;q=0.6");
        headers.put("Sec-Fetch-Dest", "document");
        headers.put("Sec-Fetch-Mode", "navigate");
        headers.put("Sec-Fetch-Site", "none");
        headers.put("Sec-Fetch-User", "?1");
        headers.put("Sec-GPC", "1");
        headers.put("Upgrade-Insecure-Requests", "1");
        headers.put("User-Agent", getUA());

        Response response = makeCurlRequest(url, method, null, headers);
        if (response == null)
        	return false;
        
        //check location header if it contains shopify
        String location = response.getHeaders().firstValue("Location").orElse(null);
        if (location != null && location.contains("shopify"))     
        	return true;
        return false;
	}
}
