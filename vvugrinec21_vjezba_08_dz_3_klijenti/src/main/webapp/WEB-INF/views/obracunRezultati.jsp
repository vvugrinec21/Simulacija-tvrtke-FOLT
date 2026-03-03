<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="java.util.List"%>
<%@page import="edu.unizg.foi.nwtis.podaci.Obracun"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <link href="${pageContext.request.contextPath}/resources/css/styles.css" rel="stylesheet" type="text/css" />
    <title>Rezultati obračuna</title>
  
</head>
<body>

<h2>Rezultati obračuna</h2>

<%
    String greska = (String) request.getAttribute("greska");
    if (greska != null) {
%>
    <p style="color:red;"><%= greska %></p>
<%
    } else {
        List<Obracun> obracuni = (List<Obracun>) request.getAttribute("obracuni");
        if (obracuni != null && !obracuni.isEmpty()) {
%>
    <table>
        <tr>
            <th>ID</th>
            <th>Vrijeme</th>
            <th>Vrsta</th>
            <th>Partner ID</th>
            <th>Količina</th>
            <th>Cijena</th>
        </tr>
        <%
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            for (Obracun o : obracuni) {
                String vrsta = o.jelo() ? "Jelo" : "Piće";
                String vrijemeStr = sdf.format(new java.util.Date(o.vrijeme()));
        %>
        <tr>
            <td><%= o.id() %></td>
            <td><%= vrijemeStr %></td>
            <td><%= vrsta %></td>
            <td><%= o.partner() %></td>
            <td><%= o.kolicina() %></td>
            <td><%= o.cijena() %></td>
        </tr>
        <%
            }
        %>
    </table>
<%
        } else {
%>
    <p>Nema obračuna za zadane kriterije.</p>
<%
        }
    }
%>

<br/>
<a href="${pageContext.servletContext.contextPath}/mvc/tvrtka/privatno/obracun/${povratak}">⟵ Natrag na obrazac</a>


</body>
</html>
