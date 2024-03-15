import java.text.ParseException;
import java.util.Vector;
import org.htmlparser.beans.StringBean;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import java.util.StringTokenizer;
import org.htmlparser.beans.LinkBean;
import java.net.URL;
import java.text.ParseException;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.htree.HTree;
import jdbm.helper.FastIterator;
import java.io.IOException;
import java.io.Serializable;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Spider implements Serializable {
    private String url;
    private int numPages;
    private Set<String> visitedUrls;
    private Queue<String> queue;
    private transient HTree parentChildMapForward;
    private transient HTree parentChildMapBackward;
    private transient RecordManager recman;

    Spider(String _url, int num) throws IOException {
        url = _url;
        numPages = num;
        visitedUrls = new HashSet<>();
        queue = new LinkedList<>();
        recman = RecordManagerFactory.createRecordManager("database");
        parentChildMapForward = HTree.createInstance(recman);
        parentChildMapBackward = HTree.createInstance(recman);
    }


    public static void main(String[] args){
        try {
            Spider mySpider = new Spider("https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm", 4);
            mySpider.crawl();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void crawl() throws ParserException, ParseException, IOException {
        queue.add(url);
        int count = 0;

        while (!queue.isEmpty() && count < numPages) {
            String currentUrl = queue.poll();

            // Perform checks before fetching the page (e.g., existence in visited urls, last modification date)
            if (!visitedUrls.contains(currentUrl) || needsUpdate(currentUrl)) {
                if (!visitedUrls.contains(currentUrl))
                    {visitedUrls.add(currentUrl);}

                fetchPage(currentUrl);
                count++;

                Vector<String> links = extractLinks(currentUrl);
                for (String link : links) {
                    if (!visitedUrls.contains(link) && !iscyclic(currentUrl, link)) {
                        queue.add(link);

                            // Add parent-child relationship to the file structure
                            addChildPage(currentUrl, link);
                        }
                    }
                }
            }
        recman.commit();
        recman.close();
    }

    private class PageInfo implements Serializable {
        private Date date;
        private Set<String> childPages;
        private String parentUrl;

        public PageInfo(Date date,Set<String> pages){
            this.date = date;
            this.childPages = pages;
        }

        public PageInfo(Date date, String parentUrl){
            this.date = date;
            this.parentUrl = parentUrl;
        }

        public Date getDate(){
            return date;
        }
        public Set<String> getChildPages(){
            return childPages;
        }
        public String getParentUrl(){
            return parentUrl;
        }
    }

    private long getSize(String url){
        return 0;
    }

    private void addChildPage(String parentUrl, String childUrl) throws ParserException, ParseException, IOException {
        Parser myparser = new Parser(parentUrl);
        String pattern = "MMM dd, yyyy";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern, Locale.ENGLISH);
        Date date = simpleDateFormat.parse(myparser.VERSION_DATE);

        Set<String> childPages = (Set<String>) getChildPages(parentUrl);
        childPages.add(childUrl);
        parentChildMapForward.put(parentUrl, new PageInfo(date, childPages));
        
        for(String childurl: childPages){
            myparser = new Parser(childurl);
            date = simpleDateFormat.parse(myparser.VERSION_DATE);

            parentChildMapBackward.put(childurl, new PageInfo(date, parentUrl));
        }
    }

    public Set<String> getChildPages(String parentUrl) throws IOException {
        PageInfo pageInfo = (PageInfo) parentChildMapForward.get(parentUrl);
        if (pageInfo == null) {
            pageInfo = new PageInfo(new Date(), new HashSet<String>());
        }
        Set<String> childPages = (Set<String>) pageInfo.getChildPages();
        if (childPages == null) {
            childPages = new HashSet<>();
        }
        return childPages;
    }

    public String getParentPages(String childUrl) throws IOException {
        PageInfo pageInfo = (PageInfo) parentChildMapBackward.get(childUrl);
        if (pageInfo == null) {
            pageInfo = new PageInfo(new Date(), new String());
        }
        String parentPages = pageInfo.getParentUrl();
        if (parentPages == null) {
            parentPages = new String();
        }
        return parentPages;
    }

    private boolean iscyclic(String parentUrl, String childUrl) throws IOException {
        HashSet<String> visited = new HashSet<>();
        // Initialize the visited set with the parentUrl to prevent immediate loop back.
        visited.add(parentUrl);
        return dfsCheckCycle(childUrl, parentUrl, visited);
    }

    private boolean dfsCheckCycle(String currentUrl, String targetUrl, HashSet<String> visited) throws IOException {
        // If the current URL is the same as the target URL, we've found a cycle.
        if (currentUrl.equals(targetUrl)) {
            return true;
        }

        // Check if already visited to avoid infinite loops
        if (visited.contains(currentUrl)) {
            return false; // Avoid revisiting the same URL
        }

        // Mark the current node as visited.
        visited.add(currentUrl);

        // Retrieve child pages of the current URL.
        Set<String> childPages = getChildPages(currentUrl);
        for (String child : childPages) {
            // Recurse with the child as the new current URL.
            if (!visited.contains(child) && dfsCheckCycle(child, targetUrl, visited)) {
                return true;
            }
        }

        // No cycle found.
        return false;
    }

    private boolean needsUpdate(String url) {
        // Check if the URL needs to be updated based on the last modification date
        return false;
    }

    private void fetchPage(String url) {
        // Fetch the page and perform indexing functions
        // Implement your logic here
    }

    public Vector<String> extractLinks(String url) throws ParserException {
        LinkBean lb = new LinkBean();
        lb.setURL(url);
        URL[] links = lb.getLinks();
        Vector<String> vec_links = new Vector<String>();
        for (int i = 0; i < links.length; i++) {
            vec_links.add(links[i].toString());
        }
        return vec_links;
    }

    public Vector<String> extractWords() throws ParserException
	{
		// extract words in url and return them
		// use StringTokenizer to tokenize the result from StringBean
		StringBean sb;
		sb = new StringBean();
		sb.setLinks(true);
		sb.setURL(url);
		String text = sb.getStrings();
		String[] tokens = text.split("[ ,?]+");
		Vector<String> vec_tokens = new Vector<>();
		for(int i = 0; i < tokens.length; i++){
			vec_tokens.add(tokens[i]);
		}
		return vec_tokens;
	}
}