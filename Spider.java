import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.text.ParseException;
import java.util.*;

import org.htmlparser.beans.StringBean;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.beans.LinkBean;
import java.net.URL;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.htree.HTree;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import src.*;
import jdbm.helper.FastIterator;
import java.io.IOException;
import java.io.Serializable;
import java.io.BufferedReader;
import java.io.FileReader;

public class Spider implements Serializable {
    private String url;
    private int numPages;
    private int PAGEID;
    private int WORDID;
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
        PAGEID = 0;
        WORDID = 0;
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
                    new Spider("https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm", 30);
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
            // Check if the URL has been visited or needs to be updated
            if (!visitedUrls.contains(currentUrl) || needsUpdate(currentUrl)) {
                if (!visitedUrls.contains(currentUrl)) {
                    visitedUrls.add(currentUrl);
                    addPageID(currentUrl);
                    count++;
                }
                Vector<String> links = extractLinks(currentUrl);
                for (String link : links) {
                    // Always add child relationship even if not fetching again
                    addChildPage(currentUrl, link);
                    if (!visitedUrls.contains(link) && !queue.contains(link)) {
                        queue.add(link);
                    }
                }
            }
        }

        // Fetch all pages
        FastIterator pageIDIter = parentIDPageInfoMap.keys();
        Object pageID = pageIDIter.next();

        while (pageID != null) {
            String url = (String) urlPageIDMapBackward.get(pageID);
            fetchPage(url);
            pageID = pageIDIter.next();
        }

        recman.commit();
        recman.close();
    }

    private long getSize(String url) throws MalformedURLException {
        URL url1 = new URL(url);
        URLConnection conn = null;
        try {
            conn = url1.openConnection();
            if(conn instanceof HttpURLConnection) {
                ((HttpURLConnection)conn).setRequestMethod("HEAD");
            }
            conn.getInputStream();
            return conn.getContentLength();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if(conn instanceof HttpURLConnection) {
                ((HttpURLConnection)conn).disconnect();
            }
        }
    }

    private void addPageID(String url) throws IOException {
        Integer existingPageID = (Integer) urlPageIDMapForward.get(url);

        if (existingPageID == null) {
            // No existing page ID found, so we assign a new one
            urlPageIDMapForward.put(url, PAGEID);
            urlPageIDMapBackward.put(PAGEID, url);

            // Initialize PageInfo with empty lists for a new URL
            PageInfo pageInfo = new PageInfo(getDate(url), getSize(url), new ArrayList<>(), true, getTitle(url)); // Assuming
                                                                                   // true
            // for childUrls
            // initialization
            parentIDPageInfoMap.put(PAGEID, pageInfo); // Link pageID with PageInfo in forward map

            PageInfo reversePageInfo = new PageInfo(getDate(url), getSize(url), new ArrayList<>(), false, getTitle(url)); // False
            // for
            // parentUrls
            childIDPageInfoMap.put(PAGEID, reversePageInfo); // Similarly for backward map


            PAGEID++; // Increment pageID for the next URL
        }
    }

    private void addChildPage(String parentUrl, String childUrl)
            throws ParserException, ParseException, IOException {

        int pageID = (int) urlPageIDMapForward.get(parentUrl);
        PageInfo parentPageInfo = (PageInfo) parentIDPageInfoMap.get(pageID);
        if (!parentPageInfo.getchildUrls().contains(childUrl)) {
            parentPageInfo.addChildPage(childUrl);
            parentIDPageInfoMap.put(pageID, parentPageInfo);
        }

        PageInfo childPageInfo = (PageInfo) childIDPageInfoMap.get(pageID);
        if (!childPageInfo.getParentUrls().contains(parentUrl)) {
            childPageInfo.addParentUrl(parentUrl);
            childIDPageInfoMap.put(pageID, childPageInfo);

        }
    }

    public ArrayList<String> getchildUrls(String parentUrl) throws IOException {
        PageInfo pageInfo = (PageInfo) parentIDPageInfoMap.get(urlPageIDMapForward.get(parentUrl));
        return (ArrayList<String>) pageInfo.getchildUrls();
    }

    public ArrayList<String> getParentUrls(String childUrl) throws IOException {
        PageInfo pageInfo = (PageInfo) childIDPageInfoMap.get(urlPageIDMapForward.get(childUrl));
        return pageInfo.getParentUrls();
    }

    private Date getDate(String currentUrl) throws IOException {
        URL url = new URL(currentUrl);
        HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
        long date = httpCon.getLastModified();
        if (date == 0)
            return new Date();
        return new Date(date);
    }

    private String getTitle(String currentUrl) throws IOException {
        Document doc = Jsoup.connect(currentUrl).get();
        return doc.title();
    }

    private boolean needsUpdate(String currentUrl) throws IOException {
        // Check if the URL needs to be updated based on the last modification date
        Date currentDate = getDate(currentUrl);
        PageInfo pageInfo = (PageInfo) parentIDPageInfoMap.get(urlPageIDMapForward.get(currentUrl));
        Date lastDate = pageInfo.getDate();
        if (currentDate.after(lastDate)) {
            return true;
        }
        return false;
    }

    private void fetchPage(String currentUrl) throws ParserException, IOException {
        // Fetch the page body and perform indexing functions
        ArrayList<String> words = extractWords(currentUrl);
        int pageID = (int)urlPageIDMapForward.get(currentUrl);

        // Create word frequency database for this page
        HTree wordBodyFreqMap = HTree.createInstance(recman);
        recman.setNamedObject((String) ("wordBodyFreqMap" + pageID), wordBodyFreqMap.getRecid());

        for (String word : words) {
            if (wordIDMapForward.get(word) == null) {
                WORDID++;
                wordIDMapForward.put(word, WORDID);
                wordIDMapBackward.put(WORDID, word);
            }

            int wordID = (int)wordIDMapForward.get(word);
            if (wordBodyFreqMap.get(wordID) != null) {
                // Increase count by 1 if key exists
                wordBodyFreqMap.put(wordID, (int)wordBodyFreqMap.get(wordID) + 1);
            } else {
                wordBodyFreqMap.put(wordID, 1);
            }
        }
        // Fetch the page title and perform indexing functions
        words = extractTitle(currentUrl);

        // Create word frequency database for this page
        HTree wordTitleFreqMap = HTree.createInstance(recman);
        recman.setNamedObject((String) ("wordTitleFreqMap" + pageID), wordTitleFreqMap.getRecid());

        for (String word : words) {
            if (wordIDMapForward.get(word) == null) {
                WORDID++;
                wordIDMapForward.put(word, WORDID);
                wordIDMapBackward.put(WORDID, word);
            }

            int wordID = (int)wordIDMapForward.get(word);
            if (wordTitleFreqMap.get(wordID) != null) {
                // Increase count by 1 if key exists
                wordTitleFreqMap.put(wordID, (int)wordTitleFreqMap.get(wordID) + 1);
            } else {
                wordTitleFreqMap.put(wordID, 1);
            }
        }
    }

    public Vector<String> extractLinks(String currentUrl) throws ParserException {
        LinkBean lb = new LinkBean();
        lb.setURL(currentUrl);
        URL[] links = lb.getLinks();
        Vector<String> vec_links = new Vector<String>();
        for (int i = 0; i < links.length; i++) {
            vec_links.add(links[i].toString());
        }
        return vec_links;
    }

    public ArrayList<String> extractWords(String currentUrl) throws ParserException, IOException {
        ArrayList<String> vec_tokens = new ArrayList<>();
        Document doc = Jsoup.connect(currentUrl).get();
        Elements bodyElements = doc.select("body");

        for (Element element : bodyElements) {
            String text = element.text();
            String[] tokens = text.split("[ ,@%^&*!#$/|©:+=~`?.-]+");
            Porter porter = new Porter();
            for (String token : tokens) {
                token = token.toLowerCase().replaceAll("\\s", "");
                // remove stopword, and run Porter Algorithm
                if (!stopWords.contains(token)) {
                    String temp = porter.stripAffixes(token);
                    if(!temp.trim().isEmpty()) {
                        vec_tokens.add(temp);
                    }
                }
            }
        }
        return vec_tokens;
    }

    public ArrayList<String> extractTitle(String currentUrl) throws ParserException, IOException {
        ArrayList<String> vec_tokens = new ArrayList<>();
        Document doc = Jsoup.connect(currentUrl).get();
        Elements bodyElements = doc.select("title");

        for (Element element : bodyElements) {
            String text = element.text();
            String[] tokens = text.split("[ ,@%^&*!#$/|©:+=~`?.-]+");
            Porter porter = new Porter();
            for (String token : tokens) {
                token = token.toLowerCase().replaceAll("\\s", "");
                // remove stopword, and run Porter Algorithm
                if (!stopWords.contains(token)) {
                    String temp = porter.stripAffixes(token);
                    if(!temp.trim().isEmpty()) {
                        vec_tokens.add(temp);
                    }
                }
            }
        }
        return vec_tokens;
    }
}
