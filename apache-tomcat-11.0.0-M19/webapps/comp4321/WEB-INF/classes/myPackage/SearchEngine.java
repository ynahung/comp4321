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

        numPages = 30; // Update this to the actual number of indexed pages
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

    private List<String> processQuery(String queryString) {
        List<String> processedTerms = new ArrayList<>();
        Matcher m = Pattern.compile("\"([^\"]*)\"|(\\S+)").matcher(queryString.toLowerCase());
        while (m.find()) {
            if (m.group(1) != null) { // Handle phrases
                String phrase = m.group(1).replace(" ", "_"); // Replace spaces with underscores for phrase indexing
                if (!stopWords.contains(phrase)) {
                    processedTerms.add(porter.stripAffixes(phrase));
                }
            } else if (m.group(2) != null) { // Single word
                String term = m.group(2);
                if (!stopWords.contains(term)) {
                    processedTerms.add(porter.stripAffixes(term));
                }
            }
        }
        return processedTerms;
    }

    // Helper to fetch frequency map by document ID and map name prefix
    private HTree getFrequencyMap(long docId, String mapPrefix) throws IOException {
        long recid = recman.getNamedObject(mapPrefix + docId);
        if (recid != 0) {
            return HTree.load(recman, recid);
        }
        return null;  // Return null if map doesn't exist
    }

    // Helper to check if term is present in the frequency map and optionally count frequency
    private boolean isTermPresentInMap(HTree freqMap, Integer wordID, boolean countFrequency) throws IOException {
        if (freqMap != null) {
            Integer frequency = (Integer) freqMap.get(wordID);
            if (frequency != null && (countFrequency || frequency > 0)) {
                return true;
            }
        }
        return false;
    }

    private int getTermFrequency(Integer docId, String term) throws IOException {
        Integer wordID = (Integer) wordIDMapForward.get(term);
        if (wordID == null) return 0;
        
        int totalFrequency = 0;
        HTree bodyFreqMap = getFrequencyMap(docId, "wordBodyFreqMap");
        HTree titleFreqMap = getFrequencyMap(docId, "wordTitleFreqMap");
        int titleWeight = 1; // Weight factor for title matches

        if (bodyFreqMap != null) {
            Integer bodyFrequency = (Integer) bodyFreqMap.get(wordID);
            totalFrequency += (bodyFrequency != null ? bodyFrequency : 0);
        }

        if (titleFreqMap != null) {
            Integer titleFrequency = (Integer) titleFreqMap.get(wordID);
            totalFrequency += (titleFrequency != null ? titleFrequency * titleWeight : 0);
        }

        return totalFrequency;
    }

    private int getDocumentFrequency(String term) throws IOException {
        Integer wordID = (Integer) wordIDMapForward.get(term);
        if (wordID == null) return 0;
        int docFrequency = 0;

        FastIterator docIdIter = parentIDPageInfoMap.keys();
        Integer docId;
        while ((docId = (Integer) docIdIter.next()) != null) {
            HTree bodyFreqMap = getFrequencyMap(docId, "wordBodyFreqMap");
            HTree titleFreqMap = getFrequencyMap(docId, "wordTitleFreqMap");

            // Check both maps for presence of the term
            if (isTermPresentInMap(bodyFreqMap, wordID, false) || isTermPresentInMap(titleFreqMap, wordID, false)) {
                docFrequency++;
            }
        }

        return docFrequency;
    }

    private List<Integer> getDocumentsWithTerm(String term) throws IOException {
        List<Integer> documents = new ArrayList<>();
        Integer wordID = (Integer) wordIDMapForward.get(term);
        if (wordID == null) {
            return documents; // Return an empty list if the term is not indexed
        }

        FastIterator docIdIter = parentIDPageInfoMap.keys();
        Integer docId;
        while ((docId = (Integer) docIdIter.next()) != null) {
            HTree bodyFreqMap = getFrequencyMap(docId, "wordBodyFreqMap");
            HTree titleFreqMap = getFrequencyMap(docId, "wordTitleFreqMap");

            // Check if the term is found in either the body or title
            if (isTermPresentInMap(bodyFreqMap, wordID, true) || isTermPresentInMap(titleFreqMap, wordID, true)) {
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

    // Calculating the TF-IDF value for a term in a document
    private double tfIdf(int termFrequency, int docFrequency, int totalDocs, double docLength) {
        if (termFrequency == 0 || docFrequency == 0) {
            return 0; // Return 0 if term frequency or document frequency is zero to avoid log(0)
        }
        double tf = 1 + Math.log(termFrequency); // Log-normalized frequency: 1 + log(tf)
        double idf = Math.log((double) totalDocs / docFrequency); // Standard IDF formula
        double tfIdfScore = (tf * idf) / (docLength == 0 ? 1 : Math.sqrt(docLength)); // Adjust for document length
        return tfIdfScore;
    }

    // Normalize the scores
    private double normalize(Map<Integer, Double> scores, Integer docId) {
        double sum = scores.values().stream().mapToDouble(score -> score * score).sum();
        return (sum == 0) ? 0 : scores.get(docId) / Math.sqrt(sum);
    }

    // Method to calculate document scores based on the processed query
    private Map<Integer, Double> calculateDocumentScores(List<String> processedQueryTerms) throws IOException {
        Map<Integer, Double> documentScores = new HashMap<>();
        int totalDocuments = numPages; // Update this to the actual number of documents indexed

        for (String term : processedQueryTerms) {
            int docFrequency = getDocumentFrequency(term);
            List<Integer> documentsWithTerm = getDocumentsWithTerm(term);

            System.out.println("Term: " + term + ", Document Frequency: " + docFrequency);

            for (Integer docId : documentsWithTerm) {
                int termFrequency = getTermFrequency(docId, term);
                double docLength = getDocumentLength(docId);
                double score = tfIdf(termFrequency, docFrequency, totalDocuments, docLength);

                System.out.println("Document ID: " + docId + ", Term Frequency: " + termFrequency + ", Score: " + score);

                documentScores.put(docId, documentScores.getOrDefault(docId, 0.0) + score);
            }
        }

        System.out.println("Pre-normalization Scores: " + documentScores);

        // Normalize scores
        Map<Integer, Double> normalizedScores = new HashMap<>();
        for (Integer docId : documentScores.keySet()) {
            double normalizedScore = normalize(documentScores, docId);
            normalizedScores.put(docId, normalizedScore);
            System.out.println("Document ID: " + docId + ", Normalized Score: " + normalizedScore);
        }

        return normalizedScores;
    }

    // Method to retrieve document URL by document ID
    public List<SearchResult> search(String queryString) throws IOException {
        List<SearchResult> searchResults = new ArrayList<>();
        
        // Process the query string to extract terms
        List<String> processedQueryTerms = processQuery(queryString);
        System.out.println("Processed Query Terms: " + processedQueryTerms);
        
        // Calculate document scores based on the processed query
        Map<Integer, Double> documentScores = calculateDocumentScores(processedQueryTerms);
        System.out.println("Normalized Document Scores: " + documentScores);
        
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
