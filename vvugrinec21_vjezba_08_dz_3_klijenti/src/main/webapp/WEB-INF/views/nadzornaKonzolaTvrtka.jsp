<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <link href="${pageContext.request.contextPath}/resources/css/styles.css" rel="stylesheet" type="text/css" />
    <title>Nadzorna konzola Tvrtka</title>
    <style>
        .zeleno { color: green; font-weight: bold; }
        .crveno { color: red; font-weight: bold; }
    </style>
</head>
<body>

<h2>Nadzorna konzola Tvrtka</h2>

<h3>Status poslužitelja</h3>
<ul>
    <%
        String statusT = String.valueOf(request.getAttribute("statusT"));
        String statusT1 = String.valueOf(request.getAttribute("statusT1"));
        String statusT2 = String.valueOf(request.getAttribute("statusT2"));
    %>
    <li>Tvrtka: 
        <span class="<%= "200".equals(statusT) ? "zeleno" : "crveno" %>">
            <%= "200".equals(statusT) ? "RADI" : "NE RADI" %>
        </span>
    </li>
    <li>Registracija (ID 1): 
        <span class="<%= "200".equals(statusT1) ? "zeleno" : "crveno" %>">
            <%= "200".equals(statusT1) ? "RADI" : "NE RADI" %>
        </span>
    </li>
    <li>Partneri (ID 2): 
        <span class="<%= "200".equals(statusT2) ? "zeleno" : "crveno" %>">
            <%= "200".equals(statusT2) ? "RADI" : "NE RADI" %>
        </span>
    </li>
</ul>

<h3>Upravljanje</h3>
<form action="${pageContext.servletContext.contextPath}/mvc/tvrtka/pauza/1" method="get">
    <button type="submit">Pauza - Registracija (ID 1)</button>
</form>
<form action="${pageContext.servletContext.contextPath}/mvc/tvrtka/start/1" method="get">
    <button type="submit">Start - Registracija (ID 1)</button>
</form>

<form action="${pageContext.servletContext.contextPath}/mvc/tvrtka/pauza/2" method="get">
    <button type="submit">Pauza - Partneri (ID 2)</button>
</form>
<form action="${pageContext.servletContext.contextPath}/mvc/tvrtka/start/2" method="get">
    <button type="submit">Start - Partneri (ID 2)</button>
</form>

<form action="${pageContext.servletContext.contextPath}/mvc/tvrtka/kraj" method="get">
    <button type="submit" style="color: red;">Kraj rada</button>
</form>

<h3>Status uživo</h3>
<div>
    Status poslužitelja: <span id="statusSocket" class="crveno">Nepoznat</span><br/>
    Broj obračuna: <span id="brojObracuna"></span><br/>
    Interna poruka: <span id="poruka"></span>
</div>

<h3>Pošalji internu poruku</h3>
<form id="porukaForma">
    <input type="text" id="inputPoruka" placeholder="Unesi poruku" required />
    <button type="submit">Pošalji</button>
</form>

<br/>
<a href="${pageContext.servletContext.contextPath}/mvc/tvrtka/pocetak">⟵ Natrag na početnu</a>

<script>
  var wsocket;

  function connect() {
    var adresa = window.location.pathname;
    var dijelovi = adresa.split("/");
    var basePath = dijelovi[1]; 
    var wsUrl = "ws://" + window.location.hostname + ":" + window.location.port + "/" + basePath + "/ws/tvrtka";

    console.log("Povezujem na WebSocket:", wsUrl);

    if ('WebSocket' in window) {
      wsocket = new WebSocket(wsUrl);
    } else if ('MozWebSocket' in window) {
      wsocket = new MozWebSocket(wsUrl);
    } else {
      alert('WebSocket nije podržan od web preglednika.');
      return;
    }

    wsocket.onmessage = function(evt) {
      console.log("Primljena poruka:", evt.data);
      var podaci = evt.data.split(";");
      if (podaci.length < 3) {
        console.error("Pogrešan format poruke!");
        return;
      }

      var status = podaci[0];
      var brojObracuna = podaci[1];
      var internaPoruka = podaci[2];

      var statusElem = document.getElementById("statusSocket");
      var brojObracunaElem = document.getElementById("brojObracuna");
      var internaPorukaElem = document.getElementById("poruka");

      statusElem.textContent = status;
      statusElem.className = (status === "RADI") ? "zeleno" : "crveno";

      brojObracunaElem.textContent = brojObracuna;

      internaPorukaElem.textContent = internaPoruka ? internaPoruka : "-";
    };

    wsocket.onerror = function(evt) {
      console.error("Greška WebSocket veze:", evt);
    };

    wsocket.onclose = function(evt) {
      console.log(`Veza zatvorena, code: ${evt.code}, reason: ${evt.reason}`);
      var statusElem = document.getElementById("statusSocket");
      statusElem.textContent = "Veza zatvorena";
      statusElem.className = "crveno";
    };
  }
	
  window.addEventListener("load", connect, false);
  
  
  
  function posaljiPoruku() {
	    const input = document.getElementById("inputPoruka");
	    const tekst = input.value.trim();
	    
	    const dijelovi = tekst.split(";")
	    
	    if (!dijelovi[2]) {
        console.log("Prazna interna poruka, neće se slati.");
        return;
    }
	    
	    if (tekst && wsocket && wsocket.readyState === WebSocket.OPEN) {
	      wsocket.send(tekst);
	      input.value = "";
	    }
	  }
  
  window.addEventListener("load", function() {
	    connect();

	    const forma = document.getElementById("porukaForma");
	    if (forma) {
	      forma.addEventListener("submit", function(event) {
	        event.preventDefault(); 
	        posaljiPoruku();
	      });
	    } else {
	      console.error("Forma za slanje poruke nije pronađena.");
	    }
	  });
  
</script>


</body>
</html>
