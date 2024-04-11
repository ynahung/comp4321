import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.helper.FastIterator;
import jdbm.htree.HTree;
import src.Porter;
import src.PageInfo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchEngine {
    private static final String STOPWORDS_FILE_PATH = "stopwords.txt";

    private HTree wordIDMapForward;
    private HTree urlPageIDMapBackward;
    private HTree parentIDPageInfoMap;
    private RecordManager recman;
    private HashSet<String> stopWords;
    private Porter porter;
    private int numPages; // This should be set to the actual number of indexed pages

    public SearchEngine() throws IOException {
        recman = RecordManagerFactory.createRecordManager("database");
        long recid = recman.getNamedObject("wordIDMapForward");
        if (recid != 0) {
            wordIDMapForward = HTree.load(recman, recid);
        } else {
            wordIDMapForward = HTree.createInstance(recman);
            recman.setNamedObject("wordIDMapForward", wordIDMapForward.getRecid());
        }

        recid = recman.getNamedObject("urlPageIDMapBackward");
        if (recid != 0) {
            urlPageIDMapBackward = HTree.load(recman, recid);
        } else {
            urlPageIDMapBackward = HTree.createInstance(recman);
            recman.setNamedObject("urlPageIDMapBackward", urlPageIDMapBackward.getRecid());
        }

        recid = recman.getNamedObject("parentIDPageInfoMap");
        if (recid != 0) {
            parentIDPageInfoMap = HTree.load(recman, recid);
        } else {
            parentIDPageInfoMap = HTree.createInstance(recman);
            recman.setNamedObject("parentIDPageInfoMap", parentIDPageInfoMap.getRecid());
        }

        stopWords = new HashSet<>();
        loadStopWords(STOPWORDS_FILE_PATH); // Load stop words from file

        porter = new Porter();
    }

    private void loadStopWords(String filePath) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                stopWords.add(line.trim().toLowerCase());
            }
        } catch (IOException e) {
            System.err.println("Error loading stop words from file: " + e.getMessage());
            throw e; // Re-throw exception to signal failure in loading
        }
    }

    private int getDocumentFrequency(String term) throws IOException {
        Integer wordID = (Integer) wordIDMapForward.get(term);
        if (wordID == null) {
            return 0; // Term not found in the index.
        }
        long recid = recman.getNamedObject("documentWordFreqMap" + wordID);
        if (recid == 0) {
            return 0; // No HTree exists for this wordID.
        }
        HTree documentWordFreq = HTree.load(recman, recid);
        FastIterator iter = documentWordFreq.keys();
        int docCount = 0;
        while (iter.next() != null) {
            docCount++;
        }
        return docCount;
    }

    private int getTermFrequency(Integer docId, String term) throws IOException {
        Integer wordID = (Integer) wordIDMapForward.get(term);
        if (wordID == null) {
            return 0; // Term not found in the index.
        }
        long recid = recman.getNamedObject("documentWordFreqMap" + wordID);
        if (recid == 0) {
            return 0; // No HTree exists for this wordID.
        }
        HTree documentWordFreq = HTree.load(recman, recid);
        Integer frequency = (Integer) documentWordFreq.get(docId);
        if (frequency == null) {
            return 0; // The term does not appear in this specific document.
        }
        return frequency;
    }

    private List<String> processQuery(String queryString) {
        List<String> processedTerms = new ArrayList<>();
        Matcher m = Pattern.compile("\"([^\"]*)\"|(\\S+)").matcher(queryString.toLowerCase());
        while (m.find()) {
            if (m.group(1) != null) { // Phrase
                processedTerms.add(porter.stripAffixes(m.group(1))); // Modify to handle phrases appropriately
            } else if (m.group(2) != null) { // Single word
                String term = m.group(2);
                if (!stopWords.contains(term)) {
                    processedTerms.add(porter.stripAffixes(term));
                }
            }
        }
        return processedTerms;
    }

    private List<Integer> getDocumentsWithTerm(String term) throws IOException {
        List<Integer> documents = new ArrayList<>();
        Integer wordID = (Integer) wordIDMapForward.get(term);
        if (wordID == null) {
            return documents; // Return an empty list if the term is not indexed
        }
        
        // Iterate through all documents to find occurrences of the term
        FastIterator docIdIter = parentIDPageInfoMap.keys();
        Integer docId;
        while ((docId = (Integer) docIdIter.next()) != null) {
            boolean found = false;
            
            // Check body frequency map
            long recid = recman.getNamedObject("wordBodyFreqMap" + docId);
            if (recid != 0) {
                HTree wordBodyFreqMap = HTree.load(recman, recid);
                Integer frequency = (Integer) wordBodyFreqMap.get(wordID);
                if (frequency != null && frequency > 0) {
                    found = true;
                }
            }
            
            // Check title frequency map if not found in body
            if (!found) {
                recid = recman.getNamedObject("wordTitleFreqMap" + docId);
                if (recid != 0) {
                    HTree wordTitleFreqMap = HTree.load(recman, recid);
                    Integer frequency = (Integer) wordTitleFreqMap.get(wordID);
                    if (frequency != null && frequency > 0) {
                        found = true;
                    }
                }
            }
            
            // If the term is found in either the body or title, add the document ID to the list
            if (found) {
                documents.add(docId);
            }
        }
        
        return documents;
    }

    private double getDocumentLength(Integer docId) throws IOException {
        PageInfo pageInfo = (PageInfo) parentIDPageInfoMap.get(docId);
        return pageInfo != null ? pageInfo.getSize() : 0;
    }

    private String getDocumentURL(Integer docId) throws IOException {
        PageInfo pageInfo = (PageInfo) parentIDPageInfoMap.get(docId);
        return pageInfo != null ? (String) urlPageIDMapBackward.get(docId) : null;
    }

    private String getDocumentTitle(Integer docId) throws IOException {
        PageInfo pageInfo = (PageInfo) parentIDPageInfoMap.get(docId);
        return pageInfo != null ? pageInfo.getPageTitle() : null;
    }

    // Method to calculate document scores based on the processed query
    private Map<Integer, Double> calculateDocumentScores(List<String> processedQueryTerms) throws IOException {
        Map<String, Integer> documentFrequencies = new HashMap<>();
        Map<String, Double> termIDFs = new HashMap<>();
        Map<Integer, Double> documentScores = new HashMap<>();
        int totalDocuments = numPages; // Assuming numPages is the total count of documents indexed

        // Calculate Document Frequencies (DF) for query terms
        for (String term : processedQueryTerms) {
            int df = getDocumentFrequency(term);
            documentFrequencies.put(term, df);
        }
        System.out.println("Document Frequencies: " + documentFrequencies);

        // Pre-compute IDF for all query terms
        for (String term : processedQueryTerms) {
            int df = documentFrequencies.get(term);
            double idf = (df == 0) ? 0 : Math.log((double) totalDocuments / (df + 1));
            termIDFs.put(term, idf);
        }
        System.out.println("Term IDFs: " + termIDFs);

        // Calculate TF-IDF for each document for each query term
        for (String term : processedQueryTerms) {
            double idf = termIDFs.get(term); // Get pre-computed IDF

            List<Integer> documentsWithTerm = getDocumentsWithTerm(term);
            for (Integer docId : documentsWithTerm) {
                int tf = getTermFrequency(docId, term); // Use TF from the document
                double tfIdf = tf * idf;

                // Accumulate TF-IDF scores for each document
                documentScores.put(docId, documentScores.getOrDefault(docId, 0.0) + tfIdf);
            }
        }
        System.out.println("Document Scores (TF-IDF): " + documentScores);

        // Normalize document scores (part of cosine similarity calculation)
        for (Map.Entry<Integer, Double> entry : documentScores.entrySet()) {
            Integer docId = entry.getKey();
            Double score = entry.getValue();

            // Assuming a method getDocumentLength(docId) that returns the length of the document vector
            double length = getDocumentLength(docId);
            double normalizedScore = score / (length + 1e-10); // Add a small number to avoid division by zero

            documentScores.put(docId, normalizedScore);
        }
        System.out.println("Normalized Document Scores: " + documentScores);

        return documentScores;
    }

    // Method to retrieve document URL by document ID
    public List<SearchResult> search(String queryString) throws IOException {
        List<SearchResult> searchResults = new ArrayList<>();
        
        // Process the query string to extract terms
        List<String> processedQueryTerms = processQuery(queryString);
        System.out.println("Processed Query Terms: " + processedQueryTerms);
        
        // Calculate document scores based on the processed query
        Map<Integer, Double> documentScores = calculateDocumentScores(processedQueryTerms);
        System.out.println("Document Scores: " + documentScores);
        
        // Sort document scores to rank documents
        List<Map.Entry<Integer, Double>> sortedDocuments = new ArrayList<>(documentScores.entrySet());
        sortedDocuments.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        System.out.println("Sorted Documents: " + sortedDocuments);
        
        // Limit the results to top 50 or the number of documents, whichever is smaller
        int resultCount = Math.min(50, sortedDocuments.size());
        for (int i = 0; i < resultCount; i++) {
            Map.Entry<Integer, Double> entry = sortedDocuments.get(i);
            Integer docId = entry.getKey();
            Double score = entry.getValue();
            
            // Assuming methods to retrieve document URL and title by document ID
            String url = getDocumentURL(docId);
            String title = getDocumentTitle(docId);
            
            searchResults.add(new SearchResult(url, title, score));
        }
        
        return searchResults;
    }

    // SearchResult class to hold the search result details
    public static class SearchResult {
        private String url;
        private String title;
        private Double score;

        public SearchResult(String url, String title, Double score) {
            this.url = url;
            this.title = title;
            this.score = score;
        }

        public String getUrl() { return url; }
        public String getTitle() { return title; }
        public Double getScore() { return score; }
    }

    // Main method for testing
    public static void main(String[] args) {
        try {
            SearchEngine searchEngine = new SearchEngine();
            List<SearchResult> results = searchEngine.search("read books");
            System.out.println("Search Results:");
            for (SearchResult result : results) {
                System.out.println(result.getTitle() + " (" + result.getUrl() + "): " + result.getScore());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
