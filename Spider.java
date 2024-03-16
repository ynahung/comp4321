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
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Set;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Spider implements Serializable {
    private String url;
    private int numPages;
    private int pageID;
    private Set<String> visitedUrls;
    private Queue<String> queue;

    private transient HTree urlPageIDMapForward;
    private transient HTree urlPageIDMapBackward;
    private transient HTree wordIDMapForward;
    private transient HTree wordIDMapBackward;
    private transient HTree parentIDPageInfoMap;
    private transient HTree childIDPageInfoMap;
    private transient RecordManager recman;
    private HashSet<String> stopWords;

    Spider(String _url, int num) throws IOException {
        url = _url;
        numPages = num;
        pageID = 0;
        visitedUrls = new HashSet<>();
        queue = new LinkedList<>();

        recman = RecordManagerFactory.createRecordManager("database");
        urlPageIDMapForward = HTree.createInstance(recman);
        recman.setNamedObject("urlPageIDMapForward", urlPageIDMapForward.getRecid());
        urlPageIDMapBackward = HTree.createInstance(recman);
        recman.setNamedObject("urlPageIDMapBackward", urlPageIDMapBackward.getRecid());

        wordIDMapForward = HTree.createInstance(recman);
        recman.setNamedObject("wordIDMapForward", wordIDMapForward.getRecid());
        wordIDMapBackward = HTree.createInstance(recman);
        recman.setNamedObject("wordIDMapBackward", wordIDMapBackward.getRecid());

        parentIDPageInfoMap = HTree.createInstance(recman);
        recman.setNamedObject("parentIDPageInfoMap", parentIDPageInfoMap.getRecid());
        childIDPageInfoMap = HTree.createInstance(recman);
        recman.setNamedObject("childIDPageInfoMap", childIDPageInfoMap.getRecid());



        // Reading stopwords.txt
        stopWords = new HashSet<String>();
        FileReader stopWordFR = new FileReader("stopwords.txt");
        BufferedReader stopWordBR = new BufferedReader(stopWordFR);
        String line;
        while ((line = stopWordBR.readLine()) != null) {
            stopWords.add(line);
        }
        stopWordFR.close();
        stopWordBR.close();
    }


    public static void main(String[] args) {
        try {
            Spider mySpider =
                    new Spider("https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm", 5);
            mySpider.crawl();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void crawl() throws ParserException, ParseException, IOException {
        addPageID(url);
        queue.add(url);
        int count = 1;

        while (!queue.isEmpty() && count < numPages) {
            String currentUrl = queue.poll();
            // Perform checks before fetching the page (e.g., existence in visited urls, last
            // modification date)
            if (!visitedUrls.contains(currentUrl) || needsUpdate(currentUrl)) {
                if (!visitedUrls.contains(currentUrl)) {
                    visitedUrls.add(currentUrl);
                }

                fetchPage(currentUrl);
                count++;

                Vector<String> links = extractLinks(currentUrl);
                for (String link : links) {
                    if (!visitedUrls.contains(link) && !queue.contains(link)) {
                        addPageID(link);
                        queue.add(link);
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
        private ArrayList<String> childUrls;
        private ArrayList<String> parentUrls;

        public PageInfo(Date date, RecordManager recman, ArrayList<String> pages, boolean isChild)
                throws IOException {
            this.date = date;
            if (isChild) {
                this.childUrls = pages;
            } else {
                this.parentUrls = pages; // Use a flag to differentiate between child and parent
                                         // lists
            }
        }

        public Date getDate() {
            return date;
        }

        public ArrayList<String> getchildUrls() {
            if (childUrls == null) {
                childUrls = new ArrayList<String>();
            }
            return childUrls;
        }

        public ArrayList<String> getParentUrls() {
            if (parentUrls == null) {
                parentUrls = new ArrayList<String>();
            }
            return parentUrls;
        }

        public void addChildPage(String page) {
            if (childUrls == null) {
                childUrls = new ArrayList<String>();
            }
            childUrls.add(page);
        }

        public void addParentUrl(String page) {
            if (parentUrls == null) {
                parentUrls = new ArrayList<String>();
            }
            parentUrls.add(page);
        }
    }

    private long getSize(String url) {
        return 0;
    }

    private void addPageID(String url) throws IOException {
        Long existingPageID = (Long) urlPageIDMapForward.get(url);
        if (existingPageID == null) {
            urlPageIDMapForward.put(url, pageID);
            urlPageIDMapBackward.put(pageID, url);

            // Initialize PageInfo with empty lists for a new URL
            PageInfo pageInfo = new PageInfo(new Date(), recman, new ArrayList<>(), true); // Assuming
                                                                                           // true
            // for childUrls
            // initialization
            parentIDPageInfoMap.put(pageID, pageInfo); // Link pageID with PageInfo in forward map

            PageInfo reversePageInfo = new PageInfo(new Date(), recman, new ArrayList<>(), false); // False
            // for
            // parentUrls
            childIDPageInfoMap.put(pageID, reversePageInfo); // Similarly for backward map

            pageID++; // Increment pageID for the next URL
        }
    }

    private void addChildPage(String parentUrl, String childUrl)
            throws ParserException, ParseException, IOException {
        Parser myparser = new Parser(parentUrl);
        String pattern = "MMM dd, yyyy";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern, Locale.ENGLISH);
        Date date = simpleDateFormat.parse(myparser.VERSION_DATE);

        int parentPageID = (int) urlPageIDMapForward.get(parentUrl);
        PageInfo parentPageInfo = (PageInfo) parentIDPageInfoMap.get(parentPageID);
        parentPageInfo.addChildPage(childUrl);
        parentIDPageInfoMap.put(parentPageID, parentPageInfo);

        myparser = new Parser(childUrl);
        date = simpleDateFormat.parse(myparser.VERSION_DATE);

        int childPageID = (int) urlPageIDMapForward.get(childUrl);
        PageInfo childPageInfo = (PageInfo) childIDPageInfoMap.get(childPageID);
        childPageInfo.addParentUrl(parentUrl);
        childIDPageInfoMap.put(childPageID, childPageInfo);
    }

    public ArrayList<String> getchildUrls(String parentUrl) throws IOException {
        PageInfo pageInfo = (PageInfo) parentIDPageInfoMap.get(urlPageIDMapForward.get(parentUrl));
        if (pageInfo == null) {
            pageInfo = new PageInfo(new Date(), recman, new ArrayList<String>(), true);
        }
        ArrayList<String> childUrls = (ArrayList<String>) pageInfo.getchildUrls();
        if (childUrls == null) {
            childUrls = new ArrayList<String>();
        }
        return childUrls;
    }

    public ArrayList<String> getParentUrls(String childUrl) throws IOException {
        PageInfo pageInfo = (PageInfo) childIDPageInfoMap.get(urlPageIDMapForward.get(childUrl));
        if (pageInfo == null) {
            pageInfo = new PageInfo(new Date(), recman, new ArrayList<String>(), false);
        }
        ArrayList<String> parentUrls = pageInfo.getParentUrls();
        if (parentUrls == null) {
            parentUrls = new ArrayList<String>();
        }
        return parentUrls;
    }

    private boolean needsUpdate(String url) {
        // Check if the URL needs to be updated based on the last modification date
        return false;
    }

    private void fetchPage(String currentUrl) throws ParserException {
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

    public Vector<String> extractWords(String currentUrl) throws ParserException {
        // extract words in url and return them
        // use StringTokenizer to tokenize the result from StringBean
        StringBean sb;
        sb = new StringBean();
        sb.setLinks(true);
        sb.setURL(currentUrl);
        sb.setReplaceNonBreakingSpaces(true);
        sb.setCollapse(true);
        String text = sb.getStrings();
        String[] tokens = text.split("[ ,?]+");
        Vector<String> vec_tokens = new Vector<>();
        for (int i = 0; i < tokens.length; i++) {
            // lowercase and remove whitespace
            String token = tokens[i].toLowerCase().replaceAll("\\s", "");
            if (!stopWords.contains(token)) {
                vec_tokens.add(token);
            }

        }
        return vec_tokens;
    }
}
