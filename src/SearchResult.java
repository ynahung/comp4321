package src;

public class SearchResult {
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