<%@ page language="java" contentType="image/svg+xml; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page import="java.util.*" %>
<%@ page import="xml.eventbroker.test.Statistics" %>
<%@ page import="xml.eventbroker.test.Statistics.JoinedDataPoint" %>     
<svg xmlns="http://www.w3.org/2000/svg"
     xmlns:xlink="http://www.w3.org/1999/xlink">
 
	<defs>
        <linearGradient id="linearGradient1" x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%"   stop-color="#ffaaaa" stop-opacity="1"/>
          <stop offset="100%" stop-color="#994444" stop-opacity="1"/>
        </linearGradient>
    </defs>
    <svg y="15">
        <line x1="10" y1="10"   x2="10"  y2="190" style="stroke: #000000; stroke-width:2;"/>
        <line x1="10" y1="190"  x2="290" y2="190" style="stroke: #000000; stroke-width:2;"/>
	    <%
	    	Statistics stat  = (Statistics)request.getAttribute("statistic");
        	List<JoinedDataPoint> list  = stat.getReduced(100);;
     		if(list.size() > 0) {
    	%>
        <path d= "
        <% 	
        	Long 	   max   = stat.max;
        	double     xstep = 280.0/(list.size()-1); 
        	double 	   ystep = 180.0/(max);%>
        <%= "M10,"+(190-ystep*list.get(0).avg) %>
        <% 	for (int i = 1; i < list.size(); i++) {%>
        	<%= "L"+(10+xstep*i)+","+(190-ystep*list.get(i).avg) %>
        <% 	}%>
        "

              style="stroke: url(#linearGradient1); stroke-width:2; fill:none;"/>
        <!-- <text x="30" y="130" style="stroke:#000000; fill: #000000; font-family:Arial; font-size: 14px;" >Graphs</text> -->
    	<%	}%>
    </svg>
</svg>