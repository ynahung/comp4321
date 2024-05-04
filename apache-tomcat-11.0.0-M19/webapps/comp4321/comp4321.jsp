<%@ page language="java" import="searchEngine.*,java.util.*,searchEngine.SearchEngine.SearchResult" %>

<html>
<body>
<%
if(request.getParameter("query")!="")
{

    String query = request.getParameter("query");
    query = query.replaceAll("\"(\\w+)\\s(\\w+)\"", "$1_$2");

    SearchEngine searchEngine = new SearchEngine();
    List<SearchResult> results = searchEngine.search(query);

    for (SearchResult result: results) { 
        %>
            <p><%=result.getTitle()%></p>
            <p><%=result.getUrl()%></p>
            <p><%=result.getDate()%>, <%=result.getSize()%></p>
            <p><%=result.getKeywords()%></p>
        <%
        Integer count = 0;
        for (String childUrl: result.getChildUrls()) {
            if (count >= 10) {
                break;
            }
            %><a href=<%=childUrl%>><%=childUrl%></a><br></br><%
            count++;
        }
    }
}
else
{
	out.println("You input nothing");
}
%>

<body>
<html>
