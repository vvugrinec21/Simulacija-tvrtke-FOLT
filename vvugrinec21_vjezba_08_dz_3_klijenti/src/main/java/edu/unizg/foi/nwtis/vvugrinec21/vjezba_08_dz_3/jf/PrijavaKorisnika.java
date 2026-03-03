package edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3.jf;

import java.io.Serializable;
import java.sql.Timestamp;

import edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3.jpa.entiteti.Zapisi;
import edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3.jpa.pomocnici.KorisniciFacade;
import edu.unizg.foi.nwtis.podaci.Korisnik;
import edu.unizg.foi.nwtis.podaci.Partner;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.security.enterprise.SecurityContext;
import jakarta.transaction.UserTransaction;

@SessionScoped
@Named("prijavaKorisnika")
public class PrijavaKorisnika implements Serializable {
    private static final long serialVersionUID = -1826447622277477398L;

    private String korisnickoIme;
    private String lozinka;
    private Korisnik korisnik;
    private boolean prijavljen = false;
    private String poruka = "";
    private Partner odabraniPartner;
    private boolean partnerOdabran = false;

    @PersistenceContext(unitName = "vjezba_08_dz_3_jpa")
    private EntityManager em;

    @Inject
    private UserTransaction utx;

    @Inject
    RestConfiguration restConfiguration;

    @Inject
    KorisniciFacade korisniciFacade;

    @Inject
    private SecurityContext securityContext;

    public String getKorisnickoIme() {
        return korisnickoIme;
    }

    public void setKorisnickoIme(String korisnickoIme) {
        this.korisnickoIme = korisnickoIme;
    }

    public String getLozinka() {
        return lozinka;
    }

    public void setLozinka(String lozinka) {
        this.lozinka = lozinka;
    }

    public String getIme() {
        return this.korisnik != null ? this.korisnik.ime() : "";
    }

    public String getPrezime() {
        return this.korisnik != null ? this.korisnik.prezime() : "";
    }

    public String getEmail() {
        return this.korisnik != null ? this.korisnik.email() : "";
    }

    public boolean isPrijavljen() {
        if (!this.prijavljen) {
            provjeriPrijavuKorisnika();
        }
        return this.prijavljen;
    }

    public String getPoruka() {
        return poruka;
    }

    public Partner getOdabraniPartner() {
        return odabraniPartner;
    }

    public void setOdabraniPartner(Partner odabraniPartner) {
        this.odabraniPartner = odabraniPartner;
    }

    public boolean isPartnerOdabran() {
        return partnerOdabran;
    }

    public void setPartnerOdabran(boolean partnerOdabran) {
        this.partnerOdabran = partnerOdabran;
    }
    

    @PostConstruct
    private void provjeriPrijavuKorisnika() {
        if (this.securityContext.getCallerPrincipal() != null) {
            var korIme = this.securityContext.getCallerPrincipal().getName();
            this.korisnik = this.korisniciFacade.pretvori(this.korisniciFacade.find(korIme));
            if (this.korisnik != null) {
                this.prijavljen = true;
                this.korisnickoIme = korIme;
                this.lozinka = this.korisnik.lozinka();
                dodajZapis("Prijava korisnika");
            }
        }
    }


    public String odjavaKorisnika() {
        if (this.prijavljen) {
            dodajZapis("Odjava korisnika");
            this.prijavljen = false;

            FacesContext facesContext = FacesContext.getCurrentInstance();
            facesContext.getExternalContext().invalidateSession();

            return "/index.xhtml?faces-redirect=true";
        }
        return "";
    }


    private void dodajZapis(String opis) {
        try {
            utx.begin(); 

            Zapisi zapis = new Zapisi();
            zapis.setKorisnickoime(this.korisnickoIme);
            zapis.setOpisrada(opis);
            zapis.setVrijeme(new Timestamp(System.currentTimeMillis()));

            var request = FacesContext.getCurrentInstance().getExternalContext().getRequest();
            if (request instanceof jakarta.servlet.http.HttpServletRequest req) {
                zapis.setAdresaracunala(req.getRemoteHost());
                zapis.setIpadresaracunala(req.getRemoteAddr());
            }

            em.persist(zapis);

            utx.commit(); 
        } catch (Exception e) {
            try {
                utx.rollback(); 
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        }
    }
}
