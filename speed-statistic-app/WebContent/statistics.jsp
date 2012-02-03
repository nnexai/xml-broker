<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="java.util.*" %>
<%@ page import="xml.eventbroker.test.Statistics" %>
<%@ page import="xml.eventbroker.test.Statistics.JoinedDataPoint" %>        
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Insert title here</title>
</head>
<body>
<embed src="SpeedStatistics/statistics.svg" width="430" height="270" type="image/svg+xml" pluginspage="http://www.adobe.com/svg/viewer/install/" />
<% Statistics statistics = (Statistics)request.getAttribute("statistic");
   List<JoinedDataPoint> list = statistics.getReduced(100); %>
Recieved: <%= statistics.l.size() %>
<table><tr><th>no</th><th>avg</th><th>median</th><th>min</th><th>max</th><th>#samples</th></tr>
<% for (int i = 0; i < list.size(); i++) { 
	JoinedDataPoint p = list.get(i);%>
<tr><td><%=i+1%></td><td><%=p.avg%></td><td><%=p.median%></td><td><%=p.min%></td><td><%=p.max%></td><td><%=p.count%></td></tr>
<%}%>
</table>
</body>
</html>