<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Vježba 8 - zadaća 3 - Početna stranica</title>
<style>
    body { font-family: Arial, sans-serif; background-color: #FBE9D0;}
    h1, h2 { color: #333; }
    ul { list-style-type: none; padding-left: 0; }
    li { margin-bottom: 8px; }
    a { text-decoration: none; color: #0066cc; }
    a:hover { text-decoration: underline; }
    .section {
        border: 1px solid #ddd; 
        padding: 15px; 
        margin-bottom: 20px; 
        border-radius: 5px;
        background-color: #f9f9f9;
    }
</style>
</head>
<body>
    <h1>Vježba 8 - zadaća 3 - Početna stranica</h1>

    <div class="section">
        <h2>Glavna početna stranica</h2>
        <ul>
            <li><a href="${pageContext.servletContext.contextPath}/mvc/tvrtka/pocetak">Početna stranica Tvrtka</a></li>
            <li><a href="${pageContext.servletContext.contextPath}/index.xhtml">Početna stranica Partner</a></li>
        </ul>
    </div>

    <div class="section">
        <h2>Javni dio</h2>
        <ul>
            <li><a href="${pageContext.servletContext.contextPath}/mvc/tvrtka/status">Status poslužitelja Tvrtka</a></li>
            <li><a href="${pageContext.servletContext.contextPath}/mvc/tvrtka/partner">Pregled partnera</a></li>
        </ul>
    </div>

    <div class="section">
        <h2>Privatni dio</h2>
        <ul>
            <li><a href="${pageContext.servletContext.contextPath}/mvc/tvrtka/privatno/obracun/vrsta">Obračun po vrsti</a></li>
            <li><a href="${pageContext.servletContext.contextPath}/mvc/tvrtka/privatno/obracun/partner">Obračun po partneru</a></li>
        </ul>
    </div>

    <div class="section">
        <h2>Admin dio</h2>
        <ul>
            <li><a href="${pageContext.servletContext.contextPath}/mvc/tvrtka/admin/nadzornaKonzolaTvrtka">Nadzorna konzola Tvrtka</a></li>
            <li><a href="${pageContext.servletContext.contextPath}/mvc/tvrtka/admin/partner/dodaj">Dodavanje partnera (admin)</a></li>
            <li><a href="${pageContext.servletContext.contextPath}/mvc/tvrtka/admin/spava">Aktiviraj spavanje</a></li>
        </ul>
    </div>
</body>
</html>
