<%@ page import="myPackage.SearchEngine" %>

<html>
<body>
<%
if(request.getParameter("query")!="")
{

    String query = request.getParameter("query");
    query = query.replaceAll("\"(\\w+)\\s(\\w+)\"", "$1_$2");

    String results = SearchEngine.searchString(query);
    System.out.println(results);
}
else
{
	out.println("You input nothing");
}
%>

<body>
<html>
