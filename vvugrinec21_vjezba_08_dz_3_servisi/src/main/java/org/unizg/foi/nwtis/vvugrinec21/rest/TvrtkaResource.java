package org.unizg.foi.nwtis.vvugrinec21.rest;

import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3.dao.PartnerDAO;
import edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3.dao.ObracunDAO;
import edu.unizg.foi.nwtis.podaci.Jelovnik;
import edu.unizg.foi.nwtis.podaci.Obracun;
import edu.unizg.foi.nwtis.podaci.Partner;

// TODO: Auto-generated Javadoc
/**
 * The Class TvrtkaResource.
 */
@Path("api/tvrtka")
public class TvrtkaResource {

    /** The Constant logger. */
    private static final Logger logger = Logger.getLogger(TvrtkaResource.class.getName());

    /** The adresa tvrtke. */
    @Inject
    @ConfigProperty(name = "adresa")
    private String adresaTvrtke;
    
    /** The mrezna vrata za kraj. */
    @Inject
    @ConfigProperty(name = "mreznaVrataKraj")
    private String mreznaVrataZaKraj;
    
    /** The mrezna vrata za rad. */
    @Inject
    @ConfigProperty(name = "mreznaVrataRad")
    private String mreznaVrataZaRad;
    
    /** The kod admina tvrtke. */
    @Inject
    @ConfigProperty(name = "kodZaAdminTvrtke")
    private String kodAdminaTvrtke;
    
    /** The kod za kraj. */
    @Inject
    @ConfigProperty(name = "kodZaKraj")
    private String kodZaKraj;
    
    @Inject
    @ConfigProperty(name = "klijentTvrtkaInfo/mp-rest/url")
    private String klijentTvrtkaInfo;
    
    /** The rest konfiguracija. */
    @Inject
    RestConfiguration restKonfiguracija;

    /**
     * Provjera.
     *
     * @return the response
     */
    @HEAD
    public Response headTvrtka() {
        logger.info("Poziv: headTvrtka()");
        String komanda = "KRAJ xxx";
        String odgovor = posaljiKomanduZaKrajOdgovor(komanda);
        return (odgovor != null)
                ? Response.ok().build()
                : Response.status(500).build();
    }

    /**
     * Status.
     *
     * @param dio the dio
     * @return the response
     */
    @HEAD
    @Path("status/{id}")
    public Response status(@PathParam("id") int dio) {
        logger.info("Poziv: status(" + dio + ")");
        
        String komanda = "STATUS " + kodAdminaTvrtke + " " + dio;
        String odgovor = posaljiKomanduZaKrajOdgovor(komanda); 
        
        if (odgovor != null && odgovor.trim().equalsIgnoreCase("OK 1")) {
            return Response.ok().build();  
        } else {
            return Response.status(204).build();  
        }
    }

    /**
     * Pauza.
     *
     * @param dio the dio
     * @return the response
     */
    @HEAD
    @Path("pauza/{id}")
    public Response pauza(@PathParam("id") int dio) {
        logger.info("Poziv: pauza(" + dio + ")");
        boolean rez = posaljiKomanduZaKraj("PAUZA " + kodAdminaTvrtke + " " + dio);
        return rez ? Response.ok().build() : Response.status(204).build();
    }

    /**
     * Start.
     *
     * @param dio the dio
     * @return the response
     */
    @HEAD
    @Path("start/{id}")
    public Response start(@PathParam("id") int dio) {
        logger.info("Poziv: start(" + dio + ")");
        boolean rez = posaljiKomanduZaKraj("START " + kodAdminaTvrtke + " " + dio);
        return rez ? Response.ok().build() : Response.status(204).build();
    }

    /**
     * Kraj.
     *
     * @return the response
     */
    @HEAD
    @Path("kraj")
    public Response kraj() {
        logger.info("Poziv: kraj()");
        String komanda = "KRAJWS " + kodZaKraj;
        boolean odgovor = posaljiKomanduZaKraj(komanda);
        return odgovor ? Response.ok().build() : Response.status(204).build();
    }

    /**
     * Kraj info.
     *
     * @return the response
     */
    @HEAD
    @Path("kraj/info")
    public Response krajInfo() {
        logger.info("Poziv: krajInfo()");
        posaljiKrajInfoKlijentu();
        return Response.ok().build();
    }

    /**
     * Gets the jelovnici.
     *
     * @return the jelovnici
     */
    @GET
    @Path("jelovnik")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJelovnici() {
        logger.info("Poziv: getJelovnici()");
        try (var veza = restKonfiguracija.dajVezu()) {
            var partnerDAO = new PartnerDAO(veza);
            List<Partner> partneri = partnerDAO.dohvatiSve(false);
            List<Jelovnik> sviJelovnici = new ArrayList<>();
            var gson = new com.google.gson.Gson();

            for (Partner partner : partneri) {
                int id = partner.id();
                String kod = partner.sigurnosniKod();
                String komanda = String.format("JELOVNIK %d %s", id, kod);
                String odgovor = posaljiKomanduOdgovor(komanda);

                if (odgovor != null && odgovor.startsWith("OK")) {
                    String json = odgovor.substring(3).trim(); 
                    java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Jelovnik>>(){}.getType();
                    List<Jelovnik> jelovnici = gson.fromJson(json, listType);
                    sviJelovnici.addAll(jelovnici);
                } else {
                    logger.warning("Neispravan ili prazan odgovor za partnera ID " + id);
                }
            }

            return Response.ok(sviJelovnici).build();

        } catch (Exception e) {
            logger.severe("Greška pri dohvaćanju svih jelovnika: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Dohvati jelovnik po id.
     *
     * @param id the id
     * @return the response
     */
    @GET
    @Path("jelovnik/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response dohvatiJelovnikPoId(@PathParam("id") int id) {
        logger.info("Poziv: dohvatiJelovnikPoId(" + id + ")");
        try (var veza = restKonfiguracija.dajVezu()) {
            var partnerDAO = new PartnerDAO(veza);
            Partner partner = partnerDAO.dohvati(id, false);

            if (partner == null) {
                return Response.status(404).entity("Partner s id " + id + " ne postoji").build();
            }
            String sigurnosniKod = partner.sigurnosniKod();
            String komanda = String.format("JELOVNIK %d %s", id, sigurnosniKod);

            try (Socket s = new Socket(adresaTvrtke, Integer.parseInt(mreznaVrataZaRad));
                 BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), "utf8"));
                 PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "utf8"))) {
                out.println(komanda);
                out.flush();
                s.shutdownOutput();
                StringBuilder odgovorBuilder = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    odgovorBuilder.append(line).append("\n");
                }
                s.shutdownInput();
                String odgovor = odgovorBuilder.toString().trim();
                if (!odgovor.startsWith("OK")) {
                    return Response.status(404).entity("Jelovnik nije pronađen ili neispravan kod").build();
                }
                String json = odgovor.substring(odgovor.indexOf('\n') + 1).trim();
                return Response.ok(json, MediaType.APPLICATION_JSON).build();
            }
        } catch (Exception e) {
            return Response.status(500).entity("Greška na serveru").build();
        }
    }


    /**
     * Gets the karta pica.
     *
     * @return the karta pica
     */
    @GET
    @Path("kartapica")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getKartaPica() {
        logger.info("Poziv: getKartaPica()");
        try (var veza = restKonfiguracija.dajVezu()) {
            var partnerDAO = new PartnerDAO(veza);
            List<Partner> partneri = partnerDAO.dohvatiSve(false);
            if (partneri.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND).entity("Nema partnera u bazi").build();
            }

            Partner prviPartner = partneri.get(0);
            String komanda = String.format("KARTAPIĆA %d %s", prviPartner.id(), prviPartner.sigurnosniKod());
            String odgovor = posaljiKomanduOdgovor(komanda);

            if (odgovor != null && odgovor.startsWith("OK")) {
                String json = odgovor.substring(3).trim();
                return Response.ok(json, MediaType.APPLICATION_JSON).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                               .entity("Neispravan odgovor: " + odgovor).build();
            }
        } catch (Exception e) {
            logger.severe("Greška pri dohvaćanju karte pića: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Dohvati partnere.
     *
     * @return the response
     */
    @GET
    @Path("partner")
    @Produces(MediaType.APPLICATION_JSON)
    public Response dohvatiPartnere() {
        logger.info("Poziv: dohvatiPartnere()");
        try (var veza = restKonfiguracija.dajVezu()) {
            List<Partner> lista = new PartnerDAO(veza).dohvatiSve(true);
            return Response.ok(lista, MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            logger.severe("Greška pri dohvaćanju partnera: " + e.getMessage());
            return Response.status(500).entity("Greška pri dohvaćanju partnera").build();
        }
    }

    /**
     * Dohvati partnera.
     *
     * @param id the id
     * @return the response
     */
    @GET
    @Path("partner/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response dohvatiPartnera(@PathParam("id") int id) {
        logger.info("Poziv: dohvatiPartnera(" + id + ")");
        try (var veza = restKonfiguracija.dajVezu()) {
            Partner p = new PartnerDAO(veza).dohvati(id, true);
            return p != null ? Response.ok(p, MediaType.APPLICATION_JSON).build() : Response.status(404).build();
        } catch (Exception e) {
            logger.severe("Greška pri dohvaćanju partnera: " + e.getMessage());
            return Response.status(500).entity("Greška pri dohvaćanju partnera").build();
        }
    }

    /**
     * Provjeri partnere.
     *
     * @return the response
     */
    @GET
    @Path("partner/provjera")
    @Produces(MediaType.APPLICATION_JSON)
    public Response provjeriPartnere() {
        logger.info("Poziv: provjeriPartnere()");
        try (var veza = restKonfiguracija.dajVezu()) {
            List<Partner> svi = new PartnerDAO(veza).dohvatiSve(true);
            String popis = posaljiKomanduOdgovor("POPIS");
            if (popis == null) return Response.status(500).build();
            List<Partner> filtrirani = svi.stream().filter(p -> popis.contains(String.valueOf(p.id()))).collect(Collectors.toList());
            return Response.ok(filtrirani, MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            logger.severe("Greška pri provjeri partnera: " + e.getMessage());
            return Response.status(500).entity("Greška pri provjeri partnera").build();
        }
    }

    /**
     * Dodaj partnera.
     *
     * @param p the p
     * @return the response
     */
    @POST
    @Path("partner")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response dodajPartnera(Partner p) {
        logger.info("Poziv: dodajPartnera(" + p + ")");
        try (var veza = restKonfiguracija.dajVezu()) {
            boolean uspjeh = new PartnerDAO(veza).dodaj(p);
            return uspjeh ? Response.status(201).build() : Response.status(409).build();
        } catch (Exception e) {
            logger.severe("Greška pri dodavanju partnera: " + e.getMessage());
            return Response.status(500).entity("Greška pri dodavanju partnera").build();
        }
    }

    /**
     * Spava.
     *
     * @param trajanje the trajanje
     * @return the response
     */
    @GET
    @Path("spava")
    public Response spava(@QueryParam("vrijeme") int trajanje) {
        logger.info("Poziv: spava(" + trajanje + ")");
        if (trajanje <= 0) {
            return Response.status(500).entity("Vrijeme mora biti veće od 0").build();
        }
        
        String komanda = String.format("SPAVA %s %d", kodAdminaTvrtke, trajanje);
        String odgovor = posaljiKomanduZaKrajOdgovor(komanda);  
        
        if (odgovor != null && odgovor.trim().equalsIgnoreCase("OK")) {
            return Response.ok().build();  
        } else {
            return Response.status(500).entity("Greška prilikom spavanja dretve: " + odgovor).build();  
        }
    }

    
    /**
     * Dohvati obracune.
     *
     * @param od the od
     * @param doVrijeme the do vrijeme
     * @return the response
     */
    @GET
    @Path("obracun")
    @Produces(MediaType.APPLICATION_JSON)
    public Response dohvatiObracune(
        @QueryParam("od") Long od,
        @QueryParam("do") Long doVrijeme
    ) {
        logger.info("Poziv: dohvatiObracune(" + od + ", " + doVrijeme + ")");
        try (var veza = restKonfiguracija.dajVezu()) {
            List<Obracun> lista = new ObracunDAO(veza).dohvatiSve(od, doVrijeme, null);
            return Response.ok(lista).build();
        } catch (Exception e) {
            logger.severe("Greška: " + e.getMessage());
            return Response.status(500).entity("Greška pri dohvaćanju obračuna").build();
        }
    }

    /**
     * Dohvati obracune jelo.
     *
     * @param od the od
     * @param doVrijeme the do vrijeme
     * @return the response
     */
    @GET
    @Path("obracun/jelo")
    @Produces(MediaType.APPLICATION_JSON)
    public Response dohvatiObracuneJelo(
        @QueryParam("od") Long od,
        @QueryParam("do") Long doVrijeme
    ) {
        logger.info("Poziv: dohvatiObracuneJelo(" + od + ", " + doVrijeme + ")");
        try (var veza = restKonfiguracija.dajVezu()) {
            List<Obracun> lista = new ObracunDAO(veza).dohvatiSveJelo(od, doVrijeme);
            return Response.ok(lista).build();
        } catch (Exception e) {
            logger.severe("Greška: " + e.getMessage());
            return Response.status(500).entity("Greška pri dohvaćanju obračuna jela").build();
        }
    }


  
    /**
     * Dohvati obracune pice.
     *
     * @param od the od
     * @param doVrijeme the do vrijeme
     * @return the response
     */
    @GET
    @Path("obracun/pice")
    @Produces(MediaType.APPLICATION_JSON)
    public Response dohvatiObracunePice(
        @QueryParam("od") Long od,
        @QueryParam("do") Long doVrijeme
    ) {
        logger.info("Poziv: dohvatiObracunePice(" + od + ", " + doVrijeme + ")");
        try (var veza = restKonfiguracija.dajVezu()) {
            List<Obracun> lista = new ObracunDAO(veza).dohvatiSvePice(od, doVrijeme);
            return Response.ok(lista).build();
        } catch (Exception e) {
            logger.severe("Greška pri dohvaćanju obračuna pića: " + e.getMessage());
            return Response.status(500).entity("Greška pri dohvaćanju obračuna pića").build();
        }
    }


    /**
     * Dohvati obracune za partnera.
     *
     * @param id the id
     * @param od the od
     * @param doVrijeme the do vrijeme
     * @return the response
     */
    @GET
    @Path("obracun/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response dohvatiObracuneZaPartnera(
        @PathParam("id") int id,
        @QueryParam("od") Long od,
        @QueryParam("do") Long doVrijeme
    ) {
        logger.info("Poziv: dohvatiObracuneZaPartnera(" + id + ", " + od + ", " + doVrijeme + ")");
        try (var veza = restKonfiguracija.dajVezu()) {
            List<Obracun> lista = new ObracunDAO(veza).dohvatiSve(od, doVrijeme, id);
            return Response.ok(lista).build();
        } catch (Exception e) {
            logger.severe("Greška: " + e.getMessage());
            return Response.status(500).entity("Greška pri dohvaćanju obračuna za partnera").build();
        }
    }

    /**
     * Dodaj obracun.
     *
     * @param obracuni the obracuni
     * @return the response
     */
    @POST
    @Path("obracun")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response dodajObracun(List<Obracun> obracuni) {
        logger.info("Poziv: dodajObracun(" + obracuni + ")");
        try (var veza = restKonfiguracija.dajVezu()) {
            ObracunDAO dao = new ObracunDAO(veza);
            boolean sviUspjesni = true;
            for (Obracun o : obracuni) {
                if (!dao.dodaj(o)) {
                    sviUspjesni = false;
                    break;  
                }
            }
            posaljiObracunWsKlijentu();
            return sviUspjesni ? Response.status(201).build() : Response.status(500).entity("Neuspješno dodavanje obračuna").build();
        } catch (Exception e) {
            logger.severe("Greška: " + e.getMessage());
            return Response.status(500).entity("Greška pri dodavanju obračuna").build();
        }
    }

    /**
     * Dodaj obracun WS.
     *
     * @param o the o
     * @return the response
     */
    @POST
    @Path("obracun/ws")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response dodajObracunWS(Obracun o) {
        logger.info("Poziv: dodajObracunWS(" + o + ")");
        try (var veza = restKonfiguracija.dajVezu()) {
         
            boolean uspjeh = new ObracunDAO(veza).dodaj(o);
            if (!uspjeh) {
                return Response.status(500).entity("Neuspješno dodavanje obračuna").build();
            }

      
            String kod = new ObracunDAO(veza).dohvatiSigurnosniKod(o.partner());
            if (kod.isEmpty()) {
                return Response.status(500).entity("Neuspjeh: sigurnosni kod nije pronađen").build();
            }

      
            String komanda = String.format("OBRAČUNWS %d %s", o.partner(), kod);
            String json = new com.google.gson.Gson().toJson(List.of(o));

        
            boolean poslan = posaljiKomanduObracunRedovi(komanda, json);
            return poslan ? Response.status(201).build() : Response.status(500).entity("Neuspješno slanje obračuna").build();

        } catch (Exception e) {
            logger.severe("Greška: " + e.getMessage());
            return Response.status(500).entity("Greška pri dodavanju obračuna WS").build();
        }
    }


    /**
     * Posalji komandu obracun.
     *
     * @param komanda the komanda
     * @param json the json
     * @return true, if successful
     */
    private boolean posaljiKomanduObracun(String komanda, String json) {
        try (Socket s = new Socket(adresaTvrtke, Integer.parseInt(mreznaVrataZaRad));
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), "utf8"));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "utf8"))) {
            out.println(komanda);
            out.println(json);
            out.flush();
            s.shutdownOutput();
            String odgovor = in.readLine();
            logger.info("Odgovor na komandu (Obracun): " + odgovor);
            return odgovor != null && odgovor.trim().equalsIgnoreCase("OK");
        } catch (IOException e) {
            logger.severe("Greška pri slanju komande (Obracun): " + e.getMessage());
            return false;
        }
    }

    /**
     * Posalji komandu obracun redovi.
     *
     * @param komanda the komanda
     * @param json the json
     * @return true, if successful
     */
    private boolean posaljiKomanduObracunRedovi(String komanda, String json) {
        try (Socket s = new Socket(adresaTvrtke, Integer.parseInt(mreznaVrataZaRad));
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), "utf8"));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "utf8"))) {
            
            
            out.println(komanda);
            
            for (String linija : json.split("\n")) {
                out.println(linija);
            }
            
            out.flush();
            s.shutdownOutput();
            
            String odgovor = in.readLine();
            logger.info("Odgovor na komandu (ObracunWS): " + odgovor);
            return odgovor != null && odgovor.trim().equalsIgnoreCase("OK");
        } catch (IOException e) {
            logger.severe("Greška pri slanju komande (ObracunWS): " + e.getMessage());
            return false;
        }
    }

    
    /**
     * Posalji komandu odgovor.
     *
     * @param komanda the komanda
     * @return the string
     */
    private String posaljiKomanduOdgovor(String komanda) {
        logger.info("Slanje komande (sa odgovorom): " + komanda);
        try (Socket s = new Socket(adresaTvrtke, Integer.parseInt(mreznaVrataZaRad));
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), "utf8"));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "utf8"))) {
            out.println(komanda);
            out.flush();
            s.shutdownOutput();
            String odgovor = in.readLine();
            s.shutdownInput();
            logger.info("Odgovor na komandu: " + odgovor);
            return odgovor;
        } catch (IOException e) {
            logger.severe("Greška pri slanju komande '" + komanda + "': " + e.getMessage());
            return null;
        }
    }

    /**
     * Posalji komandu za kraj.
     *
     * @param komanda the komanda
     * @return true, if successful
     */
    private boolean posaljiKomanduZaKraj(String komanda) {
        logger.info("Slanje komande (Kraj): " + komanda);
        try (Socket s = new Socket(adresaTvrtke, Integer.parseInt(mreznaVrataZaKraj));
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), "utf8"));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "utf8"))) {
            out.println(komanda);
            out.flush();
            s.shutdownOutput();
            String odgovor = in.readLine();
            s.shutdownInput();
            logger.info("Odgovor na komandu (Kraj): " + odgovor);
            return odgovor != null && odgovor.trim().equalsIgnoreCase("OK");
        } catch (IOException e) {
            logger.severe("Greška pri slanju komande (Kraj) '" + komanda + "': " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Posalji komandu za kraj odgovor.
     *
     * @param komanda the komanda
     * @return the string
     */
    private String posaljiKomanduZaKrajOdgovor(String komanda) {
        logger.info("Slanje komande (Kraj): " + komanda);
        try (Socket s = new Socket(adresaTvrtke, Integer.parseInt(mreznaVrataZaKraj));
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), "utf8"));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "utf8"))) {
            out.println(komanda);
            out.flush();
            s.shutdownOutput();
            String odgovor = in.readLine();
            s.shutdownInput();
            logger.info("Odgovor na komandu (Kraj): " + odgovor);
            return odgovor;
        } catch (IOException e) {
            logger.severe("Greška pri slanju komande (Kraj) '" + komanda + "': " + e.getMessage());
            return null;
        }
    }
    
    
    public boolean posaljiKrajInfoKlijentu() {
        try {
            HttpClient klijent = HttpClient.newHttpClient();

            HttpRequest zahtjev = HttpRequest.newBuilder()
                .uri(URI.create(klijentTvrtkaInfo + "api/tvrtka/kraj/info"))
                .GET()
                .build();

            var odgovor = klijent.send(zahtjev, BodyHandlers.ofString());

            return odgovor.statusCode() == 200;

        } catch (IOException | InterruptedException e) {
            logger.severe("Greška pri slanju zahtjeva ka klijentu (kraj info): " + e.getMessage());
            return false;
        }
    }
    
    public boolean posaljiObracunWsKlijentu() {
        try {
            HttpClient klijent = HttpClient.newHttpClient();

            HttpRequest zahtjev = HttpRequest.newBuilder()
                .uri(URI.create(klijentTvrtkaInfo + "api/tvrtka/obracun/ws"))
                .GET()
                .build();

            var odgovor = klijent.send(zahtjev, BodyHandlers.ofString());

            return odgovor.statusCode() == 200;

        } catch (IOException | InterruptedException e) {
            logger.severe("Greška pri slanju zahtjeva ka klijentu (obracun ws): " + e.getMessage());
            return false;
        }
    }

    
}
