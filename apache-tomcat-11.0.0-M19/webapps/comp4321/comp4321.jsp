<%@ page import="src.SearchEngine" %>

<html>
<body>
<%
if(request.getParameter("query")!="")
{

    String query = request.getParameter("query");
    query = query.replaceAll("\"(\\w+)\\s(\\w+)\"", "$1_$2");

    SearchEngine searchEngine = new SearchEngine();
    String results = searchEngine.searchString(query);
    System.out.println(results);
}
else
{
	out.println("You input nothing");
}
%>

<body>
<html>
