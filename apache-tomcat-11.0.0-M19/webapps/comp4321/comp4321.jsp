<%@ page import="java.util.*" %>

<%@ page import="myPackage.SearchEngine" %>
<%@ page import="src.SearchResult" %>


<html>
<body>
<%
if(request.getParameter("query") != null && !request.getParameter("query").isEmpty()) {
    out.println("Search result:");
    out.println("<hr>");

    SearchEngine searchEngine = new SearchEngine();
    String query = request.getParameter("query");
    query = query.replaceAll("\"(\\w+)\\s(\\w+)\"", "$1_$2");

    List<SearchResult> results = searchEngine.search(query);
    for (SearchResult result : results) {
        out.println(result.getTitle() + " (" + result.getUrl() + "): " + result.getScore() + "<br>");
    }
}
else {
    out.println("You input nothing");
}
%>
</body>
</html>


