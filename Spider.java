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
    private HTree parentChildMapForward;
    private HTree parentChildMapBackward;
    private RecordManager recman;

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
            Spider mySpider = new Spider("https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm", 10);
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

        Set<String> childPages = (Set<String>) parentChildMapForward.get(parentUrl);
        if (childPages == null) {
            childPages = new HashSet<>();
        }
        childPages.add(childUrl);
        parentChildMapForward.put(parentUrl, new PageInfo(date, childPages));
        
        for(String childurl: childPages){
            Parser parser = new Parser(childurl);
            date = simpleDateFormat.parse(parser.VERSION_DATE);

            parentChildMapBackward.put(childurl, new PageInfo(date, parentUrl));
        }
    }

    public Set<String> getChildPages(String parentUrl) throws IOException {
        Set<String> childPages = (Set<String>) parentChildMapForward.get(parentUrl);
        if (childPages == null) {
            childPages = new HashSet<>();
        }
        return childPages;
    }

    public Set<String> getParentPages(String childUrl) throws IOException {
        Set<String> parentPages = (Set<String>) parentChildMapBackward.get(childUrl);
        if (parentPages == null) {
            parentPages = new HashSet<>();
        }
        return parentPages;
    }

    private boolean iscyclic(String parentUrl, String childUrl) throws IOException {
        boolean forwardIsCyclic = parentChildMapForward.get(childUrl) != null && parentChildMapForward.get(childUrl).equals(parentUrl);
        boolean backwardIsCyclic = parentChildMapBackward.get(parentUrl) != null && parentChildMapBackward.get(parentUrl).equals(childUrl);
        return forwardIsCyclic || backwardIsCyclic;
        // This simplistic check assumes direct cyclic (parent-child-parent). For deeper cycles, a more comprehensive check is needed
    }

    private boolean needsUpdate(String url){
        // Check if the URL needs to be updated based on the last modification date
        return true;
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