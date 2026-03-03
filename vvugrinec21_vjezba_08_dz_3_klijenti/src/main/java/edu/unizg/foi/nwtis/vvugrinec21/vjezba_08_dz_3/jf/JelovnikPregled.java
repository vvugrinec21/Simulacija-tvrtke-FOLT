package edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3.jf;

import edu.unizg.foi.nwtis.podaci.Jelovnik;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.io.Serializable;
import java.util.List;

@Named("jelovnikPregled")
@RequestScoped
public class JelovnikPregled implements Serializable {

    @Inject
    private PrijavaKorisnika prijavaKorisnika;

    @RestClient
    @Inject
    private ServisPartnerKlijent servis;

    private List<Jelovnik> jelovnik;
    private String poruka;

    public List<Jelovnik> getJelovnik() {
        if (jelovnik == null && prijavaKorisnika.isPartnerOdabran()) {
            try (Response odgovor = servis.dohvatiJelovnikResponse(
                    prijavaKorisnika.getKorisnickoIme(),
                    prijavaKorisnika.getLozinka())) {

                if (odgovor.getStatus() == 200) {
                    jelovnik = odgovor.readEntity(new GenericType<List<Jelovnik>>() {});
                } else {
                    poruka = "Greška: " + odgovor.getStatus();
                }
            } catch (Exception e) {
                poruka = "Greška pri dohvaćanju jelovnika: " + e.getMessage();
            }
        }
        return jelovnik;
    }

    public String getPoruka() {
        return poruka;
    }
}
