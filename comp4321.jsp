<%@ page import="SearchEngine" %>

<%
if(request.getParameter("query")!="")
{
    out.println("Search result:");
    out.println("<hr>");

    SearchEngine searchEngine = new SearchEngine();
    String query = request.getParameter("query");
    query = query.replaceAll("\"(\\w+)\\s(\\w+)\"", "$1_$2");

    List<SearchResult> results = searchEngine.search(query);
    for (SearchResult result : results) {
        System.out.println(result.getTitle() + " (" + result.getUrl() + "): " + result.getScore());
    }
}
else
{
	out.println("You input nothing");
}
%>



