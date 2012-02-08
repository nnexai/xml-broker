<%@ page language="java" contentType="image/svg+xml; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page import="java.util.*" %>
<%@ page import="xml.eventbroker.test.Statistics" %>
<%@ page import="xml.eventbroker.test.Statistics.JoinedDataPoint" %>     
<svg xmlns="http://www.w3.org/2000/svg"
     xmlns:xlink="http://www.w3.org/1999/xlink">
 
	<defs>
        <marker id = "ArrowMarker" viewBox = "0 0 20 20" refX = "0" refY = "10" markerUnits = "strokeWidth" markerWidth = "4" markerHeight = "4" stroke = "black" stroke-width = "1" fill = "black" orient = "auto">>
        	<polygon stroke="#000000" points="0,4 19,10 0,16 "/>
        </marker>
        <linearGradient id="linearGradient1" x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%"   stop-color="#ffaaaa" stop-opacity="1"/>
          <stop offset="100%" stop-color="#994444" stop-opacity="1"/>
        </linearGradient>
    </defs>
    <svg y="15">
    	<%
        	final double XRES=400.0;
        	final double YRES=200.0;    	
    	%>
        <line x1="10" y1="<%=YRES+16%>"   x2="10"  y2="10" marker-end="url(#ArrowMarker)" style="stroke: #000000; stroke-width:2;"/>
        <line x1="10" y1="<%=YRES+15%>"  x2="<%=XRES+15%>" y2="<%=YRES+15%>" marker-end="url(#ArrowMarker)" style="stroke: #000000; stroke-width:2;"/>
	    <%
	    	Statistics stat  = (Statistics)request.getAttribute("statistic");
        	List<JoinedDataPoint> list  = stat.getReduced(50);
        	long max = stat.max_offset;
        	
        	final int XSTEPS=20;
        	final int YSTEPS=10;
        	
        	double xMarkerStep = XRES/XSTEPS;
			double yMarkerStep = YRES/YSTEPS;
			double xMarkerStepV = (stat.l.size())/(double)XSTEPS;
			double yMarkerStepV = (2*max)/(double)YSTEPS;
			for(int x = 1; x < XSTEPS+1; x++) {
				double xPos = 10+x*xMarkerStep;%>
			<% if (x%2==0) {
				double tYPos = YRES+30+((x%4==0)?10:0);
				double tValue = (long)(x*xMarkerStepV*10)/10.0;%>
				<line x1="<%=xPos%>" y1="<%=YRES+9%>" x2="<%=xPos%>" y2="<%=YRES+21%>" style="stroke: #000000; stroke-width:2;"/>
				<text x="<%=xPos%>" y="<%=tYPos%>" fill="black" font-size="10" text-anchor="middle"><%=tValue%></text>		
		<% 		} else {%>
				<line x1="<%=xPos%>" y1="<%=YRES+11%>" x2="<%=xPos%>" y2="<%=YRES+19%>" style="stroke: #000000; stroke-width:1.2;"/>
		<%		}
			}
			for(int y = 1; y < YSTEPS+1; y++) {
				double yPos = 15+(YSTEPS-y)*yMarkerStep;%>
		<% if ((y)%2==0)
{%><line x1="4" y1="<%=yPos%>" x2="16" y2="<%=yPos%>" style="stroke: #000000; stroke-width:2;"/>
	<text x="19" y="<%=yPos+2.8%>" fill="black" font-size="10"><%=((long)(y*yMarkerStepV-max)*10.0)/10.0%></text><%} else 
{%>	<line x1="6" y1="<%=yPos%>" x2="14" y2="<%=yPos%>" style="stroke: #000000; stroke-width:1.2;"/>
<%}
			}
     		if(list.size() > 0) {
            	double     xstep = XRES/(list.size()-1); 
            	double 	   ystep = YRES/(2.0*max);%>
            	
        <path d= "
        <%= "M10,"+( 15+YRES-ystep*(list.get(0).avg_offset+max) ) %>
        <% 	for (int i = 1; i < list.size(); i++) 
{%><%= "L"+(10+xstep*i)+","+( 15+YRES-ystep*(list.get(i).avg_offset+max) ) %>
        <%}%>" style="stroke: url(#linearGradient1); stroke-width:2; fill:none;"/>

        <path d= "
        <%= "M10,"+( 15+YRES-ystep*(list.get(0).min_offset+max) ) %>
        <% 	for (int i = 1; i < list.size(); i++) 
{%><%= "L"+(10+xstep*i)+","+( 15+YRES-ystep*(list.get(i).min_offset+max) ) %>
        <%}%>" style="stroke: #0000ff; stroke-width:1; fill:none;"/>

        <path d= "
        <%= "M10,"+( 15+YRES-ystep*(list.get(0).max_offset+max) ) %>
        <% 	for (int i = 1; i < list.size(); i++) 
{%><%= "L"+(10+xstep*i)+","+( 15+YRES-ystep*(list.get(i).max_offset+max) ) %>
        <%}%>" style="stroke: #ff0000; stroke-width:1; fill:none;"/>
        		
        <!-- <text x="30" y="130" style="stroke:#000000; fill: #000000; font-family:Arial; font-size: 14px;" >Graphs</text> -->
    	<%	}%>
    </svg>
</svg>