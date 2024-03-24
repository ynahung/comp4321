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
import src.*;
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
                    new Spider("https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm", 3);
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
            // Check if the URL has been visited or needs to be updated
            if (!visitedUrls.contains(currentUrl) || needsUpdate(currentUrl)) {
                if (!visitedUrls.contains(currentUrl)) {
                    visitedUrls.add(currentUrl);
                    count++;
                }
            }

            Vector<String> links = extractLinks(currentUrl);
            for (String link : links) {
                if (!visitedUrls.contains(link)) {
                    addPageID(link);
                }
                // Always add child relationship even if not fetching again
                addChildPage(currentUrl, link);
                if (!visitedUrls.contains(link) && !queue.contains(link)) {
                    queue.add(link);
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

    private long getSize(String url) {
        return 0;
    }

    private void addPageID(String url) throws IOException {
        Integer existingPageID = (Integer) urlPageIDMapForward.get(url);

        if (existingPageID == null) {
            // No existing page ID found, so we assign a new one
            urlPageIDMapForward.put(url, PAGEID);
            urlPageIDMapBackward.put(PAGEID, url);

            // Initialize PageInfo with empty lists for a new URL
            PageInfo pageInfo = new PageInfo(new Date(), new ArrayList<>(), true); // Assuming
                                                                                   // true
            // for childUrls
            // initialization
            parentIDPageInfoMap.put(PAGEID, pageInfo); // Link pageID with PageInfo in forward map

            PageInfo reversePageInfo = new PageInfo(new Date(), new ArrayList<>(), false); // False
            // for
            // parentUrls
            childIDPageInfoMap.put(PAGEID, reversePageInfo); // Similarly for backward map


            PAGEID++; // Increment pageID for the next URL
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
        if (!parentPageInfo.getchildUrls().contains(childUrl)) {
            parentPageInfo.addChildPage(childUrl);
            parentIDPageInfoMap.put(parentPageID, parentPageInfo);
        }

        myparser = new Parser(childUrl);
        date = simpleDateFormat.parse(myparser.VERSION_DATE);

        int childPageID = (int) urlPageIDMapForward.get(childUrl);
        PageInfo childPageInfo = (PageInfo) childIDPageInfoMap.get(childPageID);
        if (!childPageInfo.getParentUrls().contains(parentUrl)) {
            childPageInfo.addParentUrl(parentUrl);
            childIDPageInfoMap.put(childPageID, childPageInfo);
        }
    }

    public ArrayList<String> getchildUrls(String parentUrl) throws IOException {
        PageInfo pageInfo = (PageInfo) parentIDPageInfoMap.get(urlPageIDMapForward.get(parentUrl));
        ArrayList<String> childUrls = (ArrayList<String>) pageInfo.getchildUrls();
        return childUrls;
    }

    public ArrayList<String> getParentUrls(String childUrl) throws IOException {
        PageInfo pageInfo = (PageInfo) childIDPageInfoMap.get(urlPageIDMapForward.get(childUrl));
        ArrayList<String> parentUrls = pageInfo.getParentUrls();
        return parentUrls;
    }

    private boolean needsUpdate(String url) {
        // Check if the URL needs to be updated based on the last modification date
        return false;
    }

    private void fetchPage(String currentUrl) throws ParserException, IOException {
        // Fetch the page and perform indexing functions
        // Implement your logic here
        Vector<String> words = extractWords(currentUrl);
        int pageID = (int) urlPageIDMapForward.get(currentUrl);

        // Create word freqency database for this page
        HTree wordFreqMap = HTree.createInstance(recman);
        System.out.println((String) ("wordFreqMap" + pageID));
        recman.setNamedObject((String) ("wordFreqMap" + pageID), wordFreqMap.getRecid());

        System.out.println("URL: " + currentUrl + ", ID: " + pageID);
        for (String word : words) {
            if (wordIDMapForward.get(word) == null) {
                WORDID++;
                wordIDMapForward.put(word, WORDID);
                wordIDMapBackward.put(WORDID, word);
            }

            int wordID = (int) wordIDMapForward.get(word);
            if (wordFreqMap.get(wordID) != null) {
                // Increase count by 1 if key exists
                wordFreqMap.put(wordID, (int) wordFreqMap.get(wordID) + 1);
            } else {
                wordFreqMap.put(wordID, 1);
            }
            System.out.println("\tWORD: " + word + ", WORD_ID: " + wordIDMapForward.get(word)
                    + ", COUNT: " + wordFreqMap.get(wordIDMapForward.get(word)));
        }
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
        sb.setLinks(false);
        sb.setURL(currentUrl);
        sb.setReplaceNonBreakingSpaces(true);
        sb.setCollapse(true);
        String text = sb.getStrings();
        String[] tokens = text.split("[ ,?.-]+");
        Vector<String> vec_tokens = new Vector<>();
        Porter porter = new Porter();
        for (int i = 0; i < tokens.length; i++) {
            // lowercase and remove whitespace
            String token = tokens[i].toLowerCase().replaceAll("\\s", "");
            // remove stopword, and run Porter Algorithm
            if (!stopWords.contains(token)) {
                vec_tokens.add(porter.stripAffixes(token));
            }

        }
        return vec_tokens;
    }
}
