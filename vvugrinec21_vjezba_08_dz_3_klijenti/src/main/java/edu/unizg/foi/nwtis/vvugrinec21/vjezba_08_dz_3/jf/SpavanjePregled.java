package edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3.jf;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.io.Serializable;

@Named("spavanjePregled")
@RequestScoped
public class SpavanjePregled implements Serializable {

    private static final long serialVersionUID = 1L;

    private int trajanje;
    private String poruka;

    @Inject
    private PrijavaKorisnika prijavaKorisnika;

    @RestClient
    @Inject
    private ServisPartnerKlijent servis;

    public void aktivirajSpavanje() {
        try {

            Response response = servis.spava(
                prijavaKorisnika.getKorisnickoIme(),
                prijavaKorisnika.getLozinka(),
                trajanje
            );

            int status = response.getStatus();
            if (status == 200) {
                poruka = "Sustav spava " + trajanje + " sekundi.";
            } else {
                poruka = "Greška pri spavanju. Status: " + status;
            }

        } catch (Exception e) {
            poruka = "Izuzetak: " + (e.getMessage() != null ? e.getMessage() : "Nepoznata greška");
        }
    }

    public int getTrajanje() {
        return trajanje;
    }

    public void setTrajanje(int trajanje) {
        this.trajanje = trajanje;
    }

    public String getPoruka() {
        return poruka;
    }
}
