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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.htree.HTree;
import jdbm.helper.FastIterator;
import java.io.IOException;
import java.io.Serializable;

public class Spider {
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

    public void crawl() throws ParserException {
        queue.add(url);
        int count = 0;

        while (!queue.isEmpty() && count < numPages) {
            String currentUrl = queue.poll();

            if (!visitedUrls.contains(currentUrl)) {
                visitedUrls.add(currentUrl);

                // Perform checks before fetching the page (e.g., existence in index, last modification date)
                if (!existsInIndex(currentUrl) || needsUpdate(currentUrl)) {
                    fetchPage(currentUrl);
                    count++;
                }

                Vector<String> links = extractLinks(currentUrl);
                for (String link : links) {
                    if (!visitedUrls.contains(link)) {
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

    private void addChildPage(String parentUrl, String childUrl) {
        Set<String> childPages = (Set<String>) parentChildMapForward.get(parentUrl);
        if (childPages == null) {
            childPages = new HashSet<>();
        }
        childPages.add(childUrl);
        parentChildMapForward.put(parentUrl, childPages);
        
        for(Set<String> childPages: childurl){
            parentChildMapBackward.put(childurl, parentUrl);
        }
    }

    public Set<String> getChildPages(String parentUrl) {
        Set<String> childPages = (Set<String>) parentChildMapForward.get(parentUrl);
        if (childPages == null) {
            childPages = new HashSet<>();
        }
        return childPages;
    }

    public Set<String> getParentPages(String childUrl) {
        Set<String> parentPages = (Set<String>) parentChildMapBackward.get(childUrl);
        if (parentPages == null) {
            parentPages = new HashSet<>();
        }
        return parentPages;
    }

    private boolean existsInIndex(String url) {
        // Check if the URL exists in the index
        // Implement your logic here
        return false;
    }

    private boolean needsUpdate(String url) {
        // Check if the URL needs to be updated based on the last modification date
        // Implement your logic here
        return true;
    }

    private void fetchPage(String url) {
        // Fetch the page and perform indexing functions
        // Implement your logic here
    }

    private Vector<String> extractLinks(String url) throws ParserException {
        LinkBean lb = new LinkBean();
        lb.setURL(url);
        URL[] links = lb.getLinks();
        Vector<String> vec_links = new Vector<String>();
        for (int i = 0; i < links.length; i++) {
            vec_links.add(links[i].toString());
        }
        return vec_links;
    }
}