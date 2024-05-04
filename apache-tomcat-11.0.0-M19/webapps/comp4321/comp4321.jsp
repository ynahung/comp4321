<%@ page language="java" import="searchEngine.*" %>

<html>
<body>
<%
if(request.getParameter("query")!="")
{

    String query = request.getParameter("query");
    query = query.replaceAll("\"(\\w+)\\s(\\w+)\"", "$1_$2");

    SearchEngine searchEngine = new SearchEngine();
    String results = searchEngine.searchString(query);
    out.println(results);
}
else
{
	out.println("You input nothing");
}
%>

<body>
<html>
