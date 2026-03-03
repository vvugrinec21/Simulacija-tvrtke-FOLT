<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <link href="${pageContext.request.contextPath}/resources/css/styles.css" rel="stylesheet" type="text/css" />
    <title>Zadaća 2 - status Tvrtka</title>
    
    <style>
    .status-radi {
        color: green;
        font-weight: bold;
    }
    .status-ne-radi {
        color: red;
        font-weight: bold;
    }

    ul {
        list-style-type: none;
        padding-left: 0;
        max-width: 400px;
        margin: 20px 0;
        font-family: Arial, sans-serif;
    }

    ul li {
        background: #f0f8ff;
        margin-bottom: 10px;
        padding: 12px 15px;
        border-radius: 8px;
        box-shadow: 0 1px 3px rgba(0,0,0,0.1);
        font-size: 1.1rem;
        color: #333;
    }

    ul li p {
        margin: 0;
    }
</style>

    
</head>
<body>
    <h1>Zadaća 2 - status Tvrtka</h1>
    <ul>
        <li>
            <a href="${pageContext.servletContext.contextPath}/mvc/tvrtka/pocetak">Početna stranica</a>
        </li>
<%
    Object statusObj = request.getAttribute("status");
    if(statusObj != null) {
        int status = 0;
        try {
            status = Integer.parseInt(statusObj.toString());
        } catch (NumberFormatException ignored) {}
%>
        <li>
           <p>Status operacije: 
               <span class="<%= (status == 200) ? "status-radi" : "status-ne-radi" %>">
                   <%= (status == 200) ? "RADI" : "NE RADI" %>
               </span>
           </p>
        </li>
<%
    }

    Boolean samoOperacija = (Boolean) request.getAttribute("samoOperacija");
    if(samoOperacija != null && !samoOperacija) {
        Object statusTObj = request.getAttribute("statusT");
        int statusT = 0;
        try {
            statusT = Integer.parseInt(statusTObj != null ? statusTObj.toString() : "0");
        } catch (NumberFormatException ignored) {}
        
        Object statusT1Obj = request.getAttribute("statusT1");
        int statusT1 = 0;
        try {
            statusT1 = Integer.parseInt(statusT1Obj != null ? statusT1Obj.toString() : "0");
        } catch (NumberFormatException ignored) {}
        
        Object statusT2Obj = request.getAttribute("statusT2");
        int statusT2 = 0;
        try {
            statusT2 = Integer.parseInt(statusT2Obj != null ? statusT2Obj.toString() : "0");
        } catch (NumberFormatException ignored) {}
%>
        <li>
           <p>Status poslužitelja: 
               <span class="<%= (statusT == 200) ? "status-radi" : "status-ne-radi" %>">
                   <%= (statusT == 200) ? "RADI" : "NE RADI" %>
               </span>
           </p>
        </li>
        <li>
           <p>Status poslužitelja za registraciju: 
               <span class="<%= (statusT1 == 200) ? "status-radi" : "status-ne-radi" %>">
                   <%= (statusT1 == 200) ? "RADI" : "NE RADI" %>
               </span>
           </p>
        </li>
        <li>
           <p>Status poslužitelja za partnere: 
               <span class="<%= (statusT2 == 200) ? "status-radi" : "status-ne-radi" %>">
                   <%= (statusT2 == 200) ? "RADI" : "NE RADI" %>
               </span>
           </p>
        </li>
<%
    }
%>            
    </ul>          
</body>
</html>
