package edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3.jf;

import edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3.jpa.entiteti.Grupe;
import edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3.jpa.entiteti.Korisnici;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;

@Named("registracijaKorisnika")
@RequestScoped
public class DodavanjeKorisnika {

    @PersistenceContext(unitName = "vjezba_08_dz_3_jpa")
    private EntityManager em;

    private String korisnickoIme;
    private String lozinka;
    private String ime;
    private String prezime;
    private String email;
    private String poruka;

    @Transactional
    public String registriraj() {
        try {
            if (em.find(Korisnici.class, korisnickoIme) != null) {
                
            	FacesContext.getCurrentInstance().addMessage(null, 
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Korisnik već postoji.", null));
            	
                return null;
            }

            Korisnici korisnik = new Korisnici();
            korisnik.setKorisnik(korisnickoIme);
            korisnik.setLozinka(lozinka);
            korisnik.setIme(ime);
            korisnik.setPrezime(prezime);
            korisnik.setEmail(email);

            Grupe grupa = em.find(Grupe.class, "nwtis");
            if (grupa == null) {
                poruka = "Grupa 'nwtis' ne postoji.";
                return null;
            }

            korisnik.setGrupes(List.of(grupa));
            em.persist(korisnik);

            
            FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Uspješna registracija!", null));
            
        } catch (Exception e) {
            
        	FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Greška kod registracije: " + e.getMessage(), null));
        }

        return null;
    }

    public String getKorisnickoIme() { return korisnickoIme; }
    public void setKorisnickoIme(String korisnickoIme) { this.korisnickoIme = korisnickoIme; }
    public String getLozinka() { return lozinka; }
    public void setLozinka(String lozinka) { this.lozinka = lozinka; }
    public String getIme() { return ime; }
    public void setIme(String ime) { this.ime = ime; }
    public String getPrezime() { return prezime; }
    public void setPrezime(String prezime) { this.prezime = prezime; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPoruka() { return poruka; }
}
