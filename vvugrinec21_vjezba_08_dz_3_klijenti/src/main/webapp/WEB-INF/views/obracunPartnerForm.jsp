<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <link href="${pageContext.request.contextPath}/resources/css/styles.css" rel="stylesheet" type="text/css" />
    <title>Odabir obračuna po partneru</title>
</head>
<body>

<h2>Pregled obračuna po partneru</h2>

<form action="${pageContext.servletContext.contextPath}/mvc/tvrtka/privatno/obracun/rezultati" method="get">
    <label>Razdoblje od: <input type="datetime-local" name="od"></label><br/>
    <label>Razdoblje do: <input type="datetime-local" name="do"></label><br/><br/>

    <label>ID partnera: <input type="number" name="partnerId" min="1" required></label><br/><br/>

    <input type="submit" value="Prikaži obračune">
</form>

<br/>
<a href="${pageContext.servletContext.contextPath}/mvc/tvrtka/pocetak">⟵ Natrag na početnu</a>

</body>
</html>
