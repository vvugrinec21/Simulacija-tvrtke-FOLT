package org.unizg.foi.nwtis.vvugrinec21.rest;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3.dao.KorisnikDAO;
import edu.unizg.foi.nwtis.podaci.Korisnik;
import edu.unizg.foi.nwtis.podaci.Narudzba;

// TODO: Auto-generated Javadoc
/**
 * The Class PartnerResource.
 */
@Path("api/partner")
public class PartnerResource {

	/** The Constant logger. */
	private static final Logger logger = Logger.getLogger(TvrtkaResource.class.getName());
	
    /** The partner adresa. */
    @Inject
    @ConfigProperty(name = "adresaPartner")
    private String partnerAdresa;

    /** The mrezna vrata kraj partner. */
    @Inject
    @ConfigProperty(name = "mreznaVrataKrajPartner")
    private String mreznaVrataKrajPartner;

    /** The mrezna vrata rad partner. */
    @Inject
    @ConfigProperty(name = "mreznaVrataRadPartner")
    private String mreznaVrataRadPartner;

    /** The kod za admin partnera. */
    @Inject
    @ConfigProperty(name = "kodZaAdminPartnera")
    private String kodZaAdminPartnera;
    
    /** The kod za kraj. */
    @Inject
    @ConfigProperty(name = "kodZaKraj")
    private String kodZaKraj;
    
    /** The rest configuration. */
    @Inject
    RestConfiguration restConfiguration;

    /**
     * Head partner.
     *
     * @return the response
     */
    @HEAD
    public Response headPartner() {
        String odgovor = posaljiKomandu("KRAJ xxx", Integer.parseInt(mreznaVrataKrajPartner));
        return odgovor != null ? Response.ok().build() : Response.status(500).build();
    }

    /**
     * Status.
     *
     * @param id the id
     * @return the response
     */
    @Path("status/{id}")
    @HEAD
    public Response status(@PathParam("id") int id) {
        
    	logger.info("Poziv: status(" + id + ")");
    	
        String komanda = "STATUS " + kodZaAdminPartnera + " " + id;
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
     * @param id the id
     * @return the response
     */
    @Path("pauza/{id}")
    @HEAD
    public Response pauza(@PathParam("id") int id) {
        String komanda = "PAUZA " + kodZaAdminPartnera + " " + id;
        String odgovor = posaljiKomanduZaKrajOdgovor(komanda);
        logger.info("Odgovor od PoslužiteljPartnera za pauza: " + odgovor);
        if (odgovor != null && odgovor.trim().equalsIgnoreCase("OK")) {
            return Response.ok().build(); 
        } else {
            return Response.status(204).build();  
        }
    }

    /**
     * Start.
     *
     * @param id the id
     * @return the response
     */
    @Path("start/{id}")
    @HEAD
    public Response start(@PathParam("id") int id) {
        String komanda = "START " + kodZaAdminPartnera + " " + id;
        String odgovor = posaljiKomanduZaKrajOdgovor(komanda);
        logger.info("Odgovor od PoslužiteljPartnera za start: " + odgovor);
        if (odgovor != null && odgovor.trim().equalsIgnoreCase("OK")) {
            return Response.ok().build(); 
        } else {
            return Response.status(204).build(); 
        }
    }

    /**
     * Kraj.
     *
     * @return the response
     */
    @Path("kraj")
    @HEAD
    public Response kraj() {
        String komanda = "KRAJ " + kodZaKraj;
        String odgovor = posaljiKomanduZaKrajOdgovor(komanda);
        logger.info("Odgovor od PoslužiteljPartnera za kraj: " + odgovor);
        if (odgovor != null && odgovor.trim().equalsIgnoreCase("OK")) {
            return Response.ok().build();
        } else {
            return Response.status(204).build();  
        }
    }

    /**
     * Spava.
     *
     * @param vrijeme the vrijeme
     * @return the response
     */
    @Path("spava")
    @GET
    public Response spava(@QueryParam("vrijeme") long vrijeme) {
        String komanda = "SPAVA " + kodZaAdminPartnera + " " + vrijeme;
        String odgovor = posaljiKomanduZaKrajOdgovor(komanda);
        logger.info("Odgovor od PoslužiteljPartnera za spava: " + odgovor);
        if (odgovor != null && odgovor.trim().equalsIgnoreCase("OK")) {
            return Response.ok().build();  
        } else {
            return Response.status(500).build();  
        }
    }

    /**
     * Gets the jelovnik.
     *
     * @param korisnik the korisnik
     * @param lozinka the lozinka
     * @return the jelovnik
     */
    @Path("jelovnik")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJelovnik(@HeaderParam("korisnik") String korisnik, @HeaderParam("lozinka") String lozinka) {
        if (!autentificirajKorisnika(korisnik, lozinka)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String komanda = "JELOVNIK " + korisnik;
        String odgovor = posaljiKomanduZaJelovnikIliPica(komanda, Integer.parseInt(mreznaVrataRadPartner));
        if (odgovor == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        String[] linije = odgovor.split("\n", 2);
        if (linije[0].trim().equals("OK") && linije.length > 1) {
            return Response.ok(linije[1]).build();  
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build(); 
        }
    }

    /**
     * Gets the karta pica.
     *
     * @param korisnik the korisnik
     * @param lozinka the lozinka
     * @return the karta pica
     */
    @Path("kartapica")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getKartaPica(@HeaderParam("korisnik") String korisnik, @HeaderParam("lozinka") String lozinka) {
        if (!autentificirajKorisnika(korisnik, lozinka)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String komanda = "KARTAPIĆA " + korisnik;
        String odgovor = posaljiKomanduZaJelovnikIliPica(komanda, Integer.parseInt(mreznaVrataRadPartner));
        if (odgovor == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        String[] linije = odgovor.split("\n", 2);
        if (linije[0].trim().equals("OK") && linije.length > 1) {
            return Response.ok(linije[1]).build();  
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();  
        }
    }
    
    /**
     * Post narudzba.
     *
     * @param korisnik the korisnik
     * @param lozinka the lozinka
     * @return the response
     */
    @Path("narudzba")
    @POST
    public Response postNarudzba(@HeaderParam("korisnik") String korisnik, @HeaderParam("lozinka") String lozinka) {
        if (!autentificirajKorisnika(korisnik, lozinka)) {
            return Response.status(Response.Status.UNAUTHORIZED).build(); 
        }

        String komanda = "NARUDŽBA " + korisnik;
        String odgovor = posaljiKomanduZaJedanRed(komanda, Integer.parseInt(mreznaVrataRadPartner));


        if (odgovor == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();  
        }
        if (odgovor.trim().equalsIgnoreCase("OK")) {
            return Response.status(Response.Status.CREATED).build();  
        } else if (odgovor.contains("ERROR 44")) {
            return Response.status(Response.Status.CONFLICT).build();  
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build(); 
        }
    }
    
    
    /**
     * Post jelo.
     *
     * @param korisnik the korisnik
     * @param lozinka the lozinka
     * @param narudzba the narudzba
     * @return the response
     */
    @Path("jelo")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postJelo(@HeaderParam("korisnik") String korisnik,
                             @HeaderParam("lozinka") String lozinka,
                             Narudzba narudzba) {
        if (!autentificirajKorisnika(korisnik, lozinka)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        if (narudzba == null || narudzba.korisnik() == null || narudzba.id() == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        String komanda = String.format("JELO %s %s %.2f", narudzba.korisnik(), narudzba.id(), narudzba.kolicina());
        String odgovor = posaljiKomanduZaJedanRed(komanda, Integer.parseInt(mreznaVrataRadPartner));
        logger.info("Odgovor od PoslužiteljPartnera za jelo: " + odgovor);

        if (odgovor == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        if (odgovor.startsWith("OK")) {
            return Response.status(Response.Status.CREATED).build();
        } else {
            return Response.status(Response.Status.CONFLICT).entity(odgovor).build();
        }
    }
    
    /**
     * Gets the narudzba.
     *
     * @param korisnik the korisnik
     * @param lozinka the lozinka
     * @return the narudzba
     */
    @Path("narudzba")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNarudzba(@HeaderParam("korisnik") String korisnik, @HeaderParam("lozinka") String lozinka) {
        logger.info("Poziv: getNarudzba(" + korisnik + ")");

        if (!autentificirajKorisnika(korisnik, lozinka)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                           .entity("Neautoriziran pristup")
                           .build();
        }

        String komanda = "STANJE " + korisnik;
        String odgovor = posaljiKomanduZaJelovnikIliPica(komanda, Integer.parseInt(mreznaVrataRadPartner));

        if (odgovor == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Greška u komunikaciji s poslužiteljem")
                           .build();
        }

        String[] linije = odgovor.split("\n", 2);
        if (linije[0].trim().equals("OK") && linije.length > 1) {
            return Response.ok(linije[1], MediaType.APPLICATION_JSON).build();
        } else if (linije[0].contains("ERROR 43")) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Ne postoji otvorena narudžba za korisnika")
                           .build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity("Greška pri dohvaćanju narudžbe")
                           .build();
        }
    }

    
    /**
     * Post pice.
     *
     * @param korisnik the korisnik
     * @param lozinka the lozinka
     * @param narudzba the narudzba
     * @return the response
     */
    @Path("pice")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postPice(@HeaderParam("korisnik") String korisnik,
                             @HeaderParam("lozinka") String lozinka,
                             Narudzba narudzba) {
        if (!autentificirajKorisnika(korisnik, lozinka)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        if (narudzba == null || narudzba.korisnik() == null || narudzba.id() == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        String komanda = String.format("PIĆE %s %s %.2f", narudzba.korisnik(), narudzba.id(), narudzba.kolicina());
        String odgovor = posaljiKomanduZaJedanRed(komanda, Integer.parseInt(mreznaVrataRadPartner));
        logger.info("Odgovor od PoslužiteljPartnera za piće: " + odgovor);

        if (odgovor == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        if (odgovor.startsWith("OK")) {
            return Response.status(Response.Status.CREATED).build();
        } else {
            return Response.status(Response.Status.CONFLICT).entity(odgovor).build();
        }
    }

    /**
     * Post racun.
     *
     * @param korisnik the korisnik
     * @param lozinka the lozinka
     * @return the response
     */
    @Path("racun")
    @POST
    public Response postRacun(@HeaderParam("korisnik") String korisnik, @HeaderParam("lozinka") String lozinka) {
        if (!autentificirajKorisnika(korisnik, lozinka)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();  
        }

        String komanda = "RAČUN " + korisnik;
        String odgovor = posaljiKomanduZaJedanRed(komanda, Integer.parseInt(mreznaVrataRadPartner));


        if (odgovor == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();  
        }
        if (odgovor.trim().equalsIgnoreCase("OK")) {
            return Response.status(Response.Status.CREATED).build();  
        } else if (odgovor.contains("ERROR 44")) {
            return Response.status(Response.Status.CONFLICT).build();  
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();  
        }
    }


    /**
     * Gets the korisnici.
     *
     * @return the korisnici
     */
    @Path("korisnik")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getKorisnici() {
        try (var vezaBP = restConfiguration.dajVezu()) {
            var dao = new KorisnikDAO(vezaBP);
            List<Korisnik> korisnici = dao.dohvatiSve();
            return Response.ok(korisnici).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Gets the korisnik.
     *
     * @param id the id
     * @return the korisnik
     */
    @Path("korisnik/{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getKorisnik(@PathParam("id") String id) {
        try (var vezaBP = restConfiguration.dajVezu()) {
            var dao = new KorisnikDAO(vezaBP);
            Korisnik korisnik = dao.dohvati(id, null, false);
            return korisnik != null ? Response.ok(korisnik).build() : Response.status(Response.Status.NOT_FOUND).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Post korisnik.
     *
     * @param korisnik the korisnik
     * @return the response
     */
    @Path("korisnik")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postKorisnik(Korisnik korisnik) {
        try (var vezaBP = restConfiguration.dajVezu()) {
            var dao = new KorisnikDAO(vezaBP);
            boolean dodano = dao.dodaj(korisnik);
            return dodano ? Response.status(Response.Status.CREATED).build() : Response.status(Response.Status.CONFLICT).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Posalji komandu.
     *
     * @param komanda the komanda
     * @param port the port
     * @return the string
     */
    private String posaljiKomandu(String komanda, int port) {
        try (Socket socket = new Socket(partnerAdresa, port);
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "utf8"));
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf8"))) {
            out.write(komanda + "\n");
            out.flush();
            socket.shutdownOutput();
            
            StringBuilder odgovor = new StringBuilder();
            String linija;
            while ((linija = in.readLine()) != null) {
                odgovor.append(linija).append("\n");
            }
            
            return odgovor.toString().trim(); 
        } catch (IOException e) {
            return null;
        }
    }
    
    /**
     * Posalji komandu za kraj odgovor.
     *
     * @param komanda the komanda
     * @return the string
     */
    private String posaljiKomanduZaKrajOdgovor(String komanda) {
        try (Socket socket = new Socket(partnerAdresa, Integer.parseInt(mreznaVrataKrajPartner));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "utf8"));
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf8"))) {
            out.write(komanda + "\n");
            out.flush();
            socket.shutdownOutput();

            StringBuilder odgovor = new StringBuilder();
            String linija;
            while ((linija = in.readLine()) != null) {
                odgovor.append(linija);
            }
            
            logger.info("Odgovor od PoslužiteljaPartnera za komandu '" + komanda + "': " + odgovor.toString());
            
            return odgovor.toString();
        } catch (IOException e) {
            return null;
        }
    }
    
    /**
     * Autentificiraj korisnika.
     *
     * @param korisnik the korisnik
     * @param lozinka the lozinka
     * @return true, if successful
     */
    private boolean autentificirajKorisnika(String korisnik, String lozinka) {
        try (var vezaBP = restConfiguration.dajVezu()) {
            var dao = new KorisnikDAO(vezaBP);
            return dao.dohvati(korisnik, lozinka, true) != null;
        } catch (Exception e) {
            logger.severe("Greška pri autentifikaciji: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Posalji komandu za jelovnik ili pica.
     *
     * @param komanda the komanda
     * @param port the port
     * @return the string
     */
    private String posaljiKomanduZaJelovnikIliPica(String komanda, int port) {
        try (Socket socket = new Socket(partnerAdresa, port);
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "utf8"));
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf8"))) {

            out.write(komanda + "\n");
            out.flush();
            socket.shutdownOutput();

            StringBuilder odgovor = new StringBuilder();
            String linija;
            while ((linija = in.readLine()) != null) {
                odgovor.append(linija).append("\n");
            }
            return odgovor.toString().trim();
        } catch (IOException e) {
            logger.warning("Greška pri slanju komande: " + komanda + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Posalji komandu za jedan red.
     *
     * @param komanda the komanda
     * @param port the port
     * @return the string
     */
    private String posaljiKomanduZaJedanRed(String komanda, int port) {
        try (Socket socket = new Socket(partnerAdresa, port);
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "utf8"));
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf8"))) {

            out.write(komanda + "\n");
            out.flush();
            socket.shutdownOutput();

            return in.readLine();  
        } catch (IOException e) {
            logger.warning("Greška pri slanju komande: " + komanda + " - " + e.getMessage());
            return null;
        }
    }


}
