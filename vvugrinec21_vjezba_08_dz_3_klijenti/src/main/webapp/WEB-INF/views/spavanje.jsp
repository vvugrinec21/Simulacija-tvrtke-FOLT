<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <link href="${pageContext.request.contextPath}/resources/css/styles.css" rel="stylesheet" type="text/css" />
    <title>Aktiviraj spavanje</title>
    <script>
        window.addEventListener("DOMContentLoaded", () => {
            const trajanje = <%= request.getAttribute("onemoguci") != null ? request.getAttribute("onemoguci") : "null" %>;
            const gumb = document.getElementById("gumbSpavanje");

            if (trajanje !== null && gumb) {
                gumb.disabled = true;
                let preostalo = trajanje;
                gumb.value = "Spavanje (" + preostalo + "s)";

                const interval = setInterval(() => {
                    preostalo--;
                    gumb.value = "Spavanje (" + preostalo + "s)";
                    if (preostalo <= 0) {
                        clearInterval(interval);
                        gumb.disabled = false;
                        gumb.value = "Aktiviraj spavanje";
                    }
                }, 1000);
            }
        });
    </script>
</head>
<body>

<h2>Aktiviraj spavanje poslužitelja</h2>

<% if (request.getAttribute("greska") != null) { %>
    <p style="color:red;"><%= request.getAttribute("greska") %></p>
<% } %>

<% if (request.getAttribute("uspjeh") != null) { %>
    <p style="color:green;"><%= request.getAttribute("uspjeh") %></p>
<% } %>

<form method="post" action="${pageContext.servletContext.contextPath}/mvc/tvrtka/admin/spava">
    <label>Trajanje spavanja (sekundi):
        <input type="number" name="vrijeme" min="1" required>
    </label>
    <br/><br/>
    <input type="submit" id="gumbSpavanje" value="Aktiviraj spavanje">
</form>

<br/>
<a href="${pageContext.servletContext.contextPath}/mvc/tvrtka/pocetak">← Natrag</a>

</body>
</html>
