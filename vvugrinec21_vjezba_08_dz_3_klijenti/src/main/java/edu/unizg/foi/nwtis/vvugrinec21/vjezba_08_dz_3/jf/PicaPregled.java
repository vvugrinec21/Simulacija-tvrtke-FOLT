package edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3.jf;

import java.io.Serializable;
import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.GenericType;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import edu.unizg.foi.nwtis.podaci.KartaPica;

@Named("picaPregled")
@RequestScoped
public class PicaPregled implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private PrijavaKorisnika prijavaKorisnika;

    @RestClient
    @Inject
    private ServisPartnerKlijent servis;

    private List<KartaPica> pica;
    private String poruka;

    public List<KartaPica> getPica() {
        if (pica == null && prijavaKorisnika.isPartnerOdabran()) {
            try {
                Response response = servis.dohvatiKartuPica(
                    prijavaKorisnika.getKorisnickoIme(),
                    prijavaKorisnika.getLozinka()
                );

                if (response.getStatus() == 200) {
                    pica = response.readEntity(new GenericType<List<KartaPica>>() {});
                } else {
                    poruka = "Greška pri dohvaćanju pića: status " + response.getStatus();
                }

            } catch (Exception e) {
                poruka = "Greška pri dohvaćanju pića: " + e.getMessage();
            }
        }
        return pica;
    }

    public String getPoruka() {
        return poruka;
    }
}
