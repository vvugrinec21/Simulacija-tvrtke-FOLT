package edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3.jf;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3.GlobalniPodaci;
import edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3.ws.WebSocketPartneri;

import java.io.Serializable;

@Named("partnerUpravitelj")
@SessionScoped
public class PartnerUpravitelj implements Serializable {

    @Inject
    @RestClient
    private ServisPartnerKlijent servis;
    
    
    @Inject
    private GlobalniPodaci globalniPodaci;
    
    
    private String statusPoruka;
    private int odabraniId = 1;
    
     
    
    
    public void dohvatiStatus() {
        Response response = servis.headStatus(odabraniId);
        int status = response.getStatus();
        statusPoruka = "STATUS servisa ID " + odabraniId + ": " +
                (status == 200 ? "<span class='zeleno'>AKTIVAN</span>" : "<span class='crveno'>NEAKTIVAN</span>");
        
        String statusString = (status == 200) ? "RADI" : "NE RADI";
        int otvorene = globalniPodaci.getOtvoreneNarudzbe(odabraniId);
        int placeni = globalniPodaci.getBrojRacuna(odabraniId);

        String wsPoruka = statusString + ";" + otvorene + ";" + placeni;
        WebSocketPartneri.send(wsPoruka);
        
    }

    public void pauziraj() {
        Response response = servis.headPauza(odabraniId);
        int status = response.getStatus();
        statusPoruka = "PAUZA servisa ID " + odabraniId + ": " +
                (status == 200 ? "<span class='zeleno'>Uspješno</span>" : "<span class='crveno'>Neuspješno</span>");
    }

    public void pokreni() {
        Response response = servis.headStart(odabraniId);
        int status = response.getStatus();
        statusPoruka = "START servisa ID " + odabraniId + ": " +
                (status == 200 ? "<span class='zeleno'>Uspješno</span>" : "<span class='crveno'>Neuspješno</span>");
    }

    public void zavrsi() {
        Response response = servis.headKraj();
        int status = response.getStatus();
        statusPoruka = "KRAJ rada poslužitelja: " +
                (status == 200 ? "<span class='zeleno'>Uspješno</span>" : "<span class='crveno'>Neuspješno</span>");
    }

    public String getStatusPoruka() {
        return statusPoruka;
    }

    public int getOdabraniId() {
        return odabraniId;
    }

    public void setOdabraniId(int odabraniId) {
        this.odabraniId = odabraniId;
    }
}
