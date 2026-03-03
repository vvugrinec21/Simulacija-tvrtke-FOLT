package edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3.jf;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import jakarta.ws.rs.core.Response;

@Named("provjeraPosluzitelja")
@RequestScoped
public class PosluziteljStatus {

    @Inject
    @RestClient
    ServisPartnerKlijent servis;

    private boolean radi = false;

    public boolean isRadi() {
        return radi;
    }

    public String provjeri() {
        try {
            Response response = servis.provjeraRada();
            System.out.println("STATUS POSLUŽITELJA: " + response.getStatus()); 
            this.radi = response.getStatus() == 200;
        } catch (Exception e) {
            e.printStackTrace();
            this.radi = false;
        }
        return null;
    }
}
