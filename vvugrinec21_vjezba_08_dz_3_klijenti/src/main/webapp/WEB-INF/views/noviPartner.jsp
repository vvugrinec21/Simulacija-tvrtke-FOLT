<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <link href="${pageContext.request.contextPath}/resources/css/styles.css" rel="stylesheet" type="text/css" />
    <title>Dodaj partnera</title>
</head>
<body>

<h2>Dodaj novog partnera</h2>

<form method="post" action="${pageContext.servletContext.contextPath}/mvc/tvrtka/admin/partner/dodaj">
	<label>ID: <input type="number" name="id" min="1" required></label><br/>
    <label>Naziv: <input type="text" name="naziv" required></label><br/>
    <label>Vrsta kuhinje: <input type="text" name="vrstaKuhinje" required></label><br/>
    <label>Adresa: <input type="text" name="adresa" required></label><br/>
    <label>Mrežna vrata: <input type="number" name="mreznaVrata" required></label><br/>
    <label>Mrežna vrata za kraj: <input type="number" name="mreznaVrataKraj" required></label><br/>
    <label>GPS širina: <input type="number" name="gpsSirina" step="0.000001" required></label><br/>
    <label>GPS dužina: <input type="number" name="gpsDuzina" step="0.000001" required></label><br/>
    <label>Sigurnosni kod: <input type="text" name="sigurnosniKod" required></label><br/>
    <label>Admin kod: <input type="text" name="adminKod" required></label><br/><br/>
    <input type="submit" value="Dodaj partnera">
</form>


<% String uspjeh = (String) request.getAttribute("uspjeh"); %>
<% if (uspjeh != null) { %>
    <p style="color:green;"><%= uspjeh %></p>
<% } %>

<% String greska = (String) request.getAttribute("greska"); %>
<% if (greska != null) { %>
    <p style="color:red;"><%= greska %></p>
<% } %>


<br/>

<a href="${pageContext.servletContext.contextPath}/mvc/tvrtka/pocetak">⟵ Početna stranica</a>
<br/>
<br/>
<a href="${pageContext.servletContext.contextPath}/mvc/tvrtka/partner">Pregled svih partnera</a>

</body>
</html>
