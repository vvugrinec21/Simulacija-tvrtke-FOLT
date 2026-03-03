<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="edu.unizg.foi.nwtis.podaci.Partner" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <link href="${pageContext.request.contextPath}/resources/css/styles.css" rel="stylesheet" type="text/css" />
    <title>Detalji partnera</title>
    <style type="text/css">
        table, th, td {
            border: 1px solid;
            padding: 5px;
        }
        th {
            text-align: left;
            background-color: #f0f0f0;
        }
    </style>
</head>
<body>

    <h1>Detalji partnera</h1>

    <%
        Partner partner = (Partner) request.getAttribute("partner");
        String greska = (String) request.getAttribute("greska");
        if (partner != null) {
    %>
        <table class="detail-table">
            <tr><th>ID</th><td><%= partner.id() %></td></tr>
            <tr><th>Naziv</th><td><%= partner.naziv() %></td></tr>
            <tr><th>Adresa</th><td><%= partner.adresa() %></td></tr>
            <tr><th>Mrežna vrata</th><td><%= partner.mreznaVrata() %></td></tr>
            <tr><th>Mrežna vrata za kraj</th><td><%= partner.mreznaVrataKraj() %></td></tr>
            <tr><th>Admin kod</th><td><%= partner.adminKod() %></td></tr>
        </table>
    <%
        } else {
    %>
        <p class="error-message"><strong><%= greska != null ? greska : "Partner nije pronađen." %></strong></p>

    <%
        }
    %>

    <br/>
    <a class="back-link" href="${pageContext.servletContext.contextPath}/mvc/tvrtka/partner">⟵ Natrag na popis partnera</a>


</body>
</html>
