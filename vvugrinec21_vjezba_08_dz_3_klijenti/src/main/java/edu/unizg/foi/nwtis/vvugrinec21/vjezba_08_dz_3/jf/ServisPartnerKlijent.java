package edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3.jf;

import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import java.util.List;

import edu.unizg.foi.nwtis.podaci.Korisnik;
import edu.unizg.foi.nwtis.podaci.Narudzba;

@RegisterRestClient(configKey = "klijentPartner")
@Path("api/partner")
public interface ServisPartnerKlijent {

    @HEAD
    Response provjeraRada();
    
    @POST
    @Path("korisnik")
    @Consumes(MediaType.APPLICATION_JSON)
    Response dodajKorisnika(Korisnik korisnik);
    
    @GET
    @Path("jelovnik")
    @Produces(MediaType.APPLICATION_JSON)
    Response dohvatiJelovnikResponse(
        @HeaderParam("korisnik") String korisnik,
        @HeaderParam("lozinka") String lozinka
    );

    @GET
    @Path("kartapica")
    @Produces(MediaType.APPLICATION_JSON)
    Response dohvatiKartuPica(
        @HeaderParam("korisnik") String korisnik,
        @HeaderParam("lozinka") String lozinka
    );
    
    @POST
    @Path("narudzba")
    Response postNarudzba(
        @HeaderParam("korisnik") String korisnik,
        @HeaderParam("lozinka") String lozinka
    );

    @POST
    @Path("jelo")
    @Consumes(MediaType.APPLICATION_JSON)
    Response postJelo(
        @HeaderParam("korisnik") String korisnik,
        @HeaderParam("lozinka") String lozinka,
        Narudzba narudzba
    );

    @POST
    @Path("pice")
    @Consumes(MediaType.APPLICATION_JSON)
    Response postPice(
        @HeaderParam("korisnik") String korisnik,
        @HeaderParam("lozinka") String lozinka,
        Narudzba narudzba
    );

    @POST
    @Path("racun")
    Response postRacun(
        @HeaderParam("korisnik") String korisnik,
        @HeaderParam("lozinka") String lozinka
    );

    @GET
    @Path("narudzba")
    @Produces(MediaType.APPLICATION_JSON)
    Response dohvatiTrenutnuNarudzbu(
        @HeaderParam("korisnik") String korisnik,
        @HeaderParam("lozinka") String lozinka
    );
    
    @GET
    @Path("spava")
    Response spava(
        @HeaderParam("korisnik") String korisnik,
        @HeaderParam("lozinka") String lozinka,
        @QueryParam("vrijeme") int vrijeme
    );
    
    @GET
    @Path("korisnik")
    @Produces(MediaType.APPLICATION_JSON)
    List<Korisnik> dohvatiKorisnike();

    @GET
    @Path("korisnik/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    Korisnik dohvatiKorisnika(@PathParam("id") String id);
    
   
    @HEAD
    @Path("/status/{id}")
    Response headStatus(@PathParam("id") int id);

    @HEAD
    @Path("/pauza/{id}")
    Response headPauza(@PathParam("id") int id);

    @HEAD
    @Path("/start/{id}")
    Response headStart(@PathParam("id") int id);

    @HEAD
    @Path("/kraj")
    Response headKraj();
    
}
