package edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3.jf;

import edu.unizg.foi.nwtis.podaci.Korisnik;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.io.Serializable;
import java.util.List;

@Named("pregledRestKorisnika")
@SessionScoped
public class PregledRestKorisnika implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<Korisnik> korisnici;
    private Korisnik odabraniKorisnik;

    @Inject
    @RestClient
    private ServisPartnerKlijent servis;

    @PostConstruct
    public void init() {
        ucitajKorisnike();
    }

    public void ucitajKorisnike() {
        korisnici = servis.dohvatiKorisnike();
    }

    public String odaberiKorisnika(String id) {
        odabraniKorisnik = servis.dohvatiKorisnika(id);
        return "/admin/korisniciDetalji.xhtml?faces-redirect=true";
    }


    public List<Korisnik> getKorisnici() {
        return korisnici;
    }

    public Korisnik getOdabraniKorisnik() {
        return odabraniKorisnik;
    }


    public String korisnickoIme(Korisnik k) {
        return k.korisnik();
    }

    public String lozinka(Korisnik k) {
        return k.lozinka(); 
    }

    public String ime(Korisnik k) {
        return k.ime();
    }

    public String prezime(Korisnik k) {
        return k.prezime();
    }

    public String email(Korisnik k) {
        return k.email();
    }

    public String lozinkaOdabranog() {
        return odabraniKorisnik != null ? odabraniKorisnik.lozinka() : "";
    }
}
