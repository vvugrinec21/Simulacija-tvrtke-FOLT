package edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3.jf;

import edu.unizg.foi.nwtis.podaci.Jelovnik;
import edu.unizg.foi.nwtis.podaci.KartaPica;
import edu.unizg.foi.nwtis.podaci.Narudzba;
import edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3.GlobalniPodaci;
import edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3.jpa.entiteti.Zapisi;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.UserTransaction;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named("narudzbaPregled")
@ViewScoped
public class NarudzbaPregled implements Serializable {
	
    private static final long serialVersionUID = 1L;

    @Inject private PrijavaKorisnika prijavaKorisnika;
    @Inject private JelovnikPregled jelovnikPregled;
    @Inject private PicaPregled picaPregled;
    
    @Inject
    private HttpServletRequest request;

    
    @PersistenceContext(unitName = "vjezba_08_dz_3_jpa")
    private EntityManager em;

    @Inject
    private UserTransaction utx;
    
    
    @Inject
    private GlobalniPodaci globalniPodaci;
    
    @RestClient
    @Inject private ServisPartnerKlijent servis;

    private String odabranoJeloId;
    private String odabranoPiceId;
    private int kolicinaJelo = 1;
    private int kolicinaPice = 1;
    private String poruka;

    private boolean aktivna = false;
    private boolean aktivnaProvjerena = false;

    private List<Narudzba> narucenaJela = new ArrayList<>();
    private List<Narudzba> narucenaPica = new ArrayList<>();

    @PostConstruct
    public void init() {
    	
    	System.out.println("[DEBUG] NarudzbaPregled.init() pozvan");
        System.out.println("[DEBUG] REST servis injektiran? " + (servis != null));
        
        provjeriNarudzbu();
    }

    public void novaNarudzba() {
    	     
        try {
            Response response = servis.postNarudzba(
                prijavaKorisnika.getKorisnickoIme(),
                prijavaKorisnika.getLozinka()
            );

            int status = response.getStatus();

            if (status == 201) {
                aktivna = true;
                poruka = "✔ Nova narudžba je uspješno kreirana.";
                
                int idPartner = globalniPodaci.getIdPartnera();
                globalniPodaci.povecajOtvoreneNarudzbe(idPartner);
                
                provjeriNarudzbu();
            } else if (status == 409) {
                aktivna = true;
                poruka = "Narudžba već postoji. Možete je dopuniti ili platiti.";
                provjeriNarudzbu();
            } else {
            	poruka = "Došlo je do greške pokušajte ponovno";
            }

        } catch (WebApplicationException ex) {
            int status = ex.getResponse().getStatus();
            if (status == 409) {
                aktivna = true;
                poruka = "ℹ Narudžba već postoji. Možete je dopuniti ili platiti.";
            } else {
                poruka = "Došlo je do greške pokušajte ponovno";
            }
        } catch (Exception e) {
        	poruka = "Došlo je do greške pokušajte ponovno";
        }
    }

    public void naruciJelo() {
    	
    	System.out.println("[DEBUG] naruciJelo() pozvana");
        System.out.println("[DEBUG] Odabrano jelo ID: " + odabranoJeloId);
        System.out.println("[DEBUG] Količina jela: " + kolicinaJelo);
    	   
        try {
            Narudzba narudzba = new Narudzba(prijavaKorisnika.getKorisnickoIme(), odabranoJeloId, true, kolicinaJelo, 0, 0);
            Response response = servis.postJelo(prijavaKorisnika.getKorisnickoIme(), prijavaKorisnika.getLozinka(), narudzba);
            if (response.getStatus() == 201) {
                poruka = "✔ Jelo je naručeno.";
                provjeriNarudzbu();
            } else {
            	poruka = "Došlo je do greške pokušajte ponovno";
            }
        } catch (Exception e) {
        	poruka = "Došlo je do greške pokušajte ponovno";
        }
    }

    public void naruciPice() {
    	
    	System.out.println("[DEBUG] naruciPice() pozvana");
        System.out.println("[DEBUG] Odabrano piće ID: " + odabranoPiceId);
        try {
            Narudzba narudzba = new Narudzba(prijavaKorisnika.getKorisnickoIme(), odabranoPiceId, false, kolicinaPice, 0, 0);
            Response response = servis.postPice(prijavaKorisnika.getKorisnickoIme(), prijavaKorisnika.getLozinka(), narudzba);
            if (response.getStatus() == 201) {
                poruka = "✔ Piće je naručeno.";
                provjeriNarudzbu();
            } else {
            	poruka = "Došlo je do greške pokušajte ponovno";
            }
        } catch (Exception e) {
        	poruka = "Došlo je do greške pokušajte ponovno";
        }
    }

    public void platiNarudzbu() {
        try {
            Response response = servis.postRacun(prijavaKorisnika.getKorisnickoIme(), prijavaKorisnika.getLozinka());
            if (response.getStatus() == 201) {
                aktivna = false;
                narucenaJela.clear();
                narucenaPica.clear();
                poruka = "Narudžba je uspješno plaćena.";
                
                int idPartner = globalniPodaci.getIdPartnera();
                globalniPodaci.povecajBrojRacuna(idPartner);
                
                globalniPodaci.smanjiOtvoreneNarudzbe(idPartner);
                
                dodajZapisUBazu(prijavaKorisnika.getKorisnickoIme(), "Narudžba plaćena");
                
            } else {
            	poruka = "Došlo je do greške pokušajte ponovno";
            }
        } catch (Exception e) {
        	poruka = "Došlo je do greške pokušajte ponovno";
        }
    }

    private void provjeriNarudzbu() {
        try {
            Response response = servis.dohvatiTrenutnuNarudzbu(
                prijavaKorisnika.getKorisnickoIme(),
                prijavaKorisnika.getLozinka()
            );

            int status = response.getStatus();

            if (status == 200) {
                String json = response.readEntity(String.class);
                Jsonb jsonb = JsonbBuilder.create();
                Narudzba[] narudzbe = jsonb.fromJson(json, Narudzba[].class);

                narucenaJela.clear();
                narucenaPica.clear();

                for (Narudzba n : narudzbe) {
                    if (n.jelo()) {
                        narucenaJela.add(n);
                    } else {
                        narucenaPica.add(n);
                    }
                }

                aktivna = true;
                poruka = "✔ Postoji aktivna narudžba.";

            } else {
                String tekst = response.readEntity(String.class);
                if (status == 500 && tekst != null && tekst.contains("Ne postoji otvorena narudžba")) {
                    aktivna = false;
                    poruka = "Nema aktivne narudžbe. Možete započeti novu.";
                } else {
                    aktivna = false;
                    poruka = "Došlo je do greške pokušajte ponovno";
                }
            }

        } catch (WebApplicationException ex) {
            int status = ex.getResponse().getStatus();
            String tekst = "";
            try {
                tekst = ex.getResponse().readEntity(String.class);
            } catch (Exception ignored) {}

            if (status == 500 && tekst.contains("Ne postoji otvorena narudžba")) {
                aktivna = false;
                poruka = "Nema aktivne narudžbe. Možete započeti novu.";
            } else {
                aktivna = false;
                poruka = "Došlo je do greške pokušajte ponovno";
            }
        } catch (Exception e) {
            aktivna = false;
            poruka = "Došlo je do greške pokušajte ponovno";
        }

        aktivnaProvjerena = true;
    }

    public boolean isAktivan() {
        if (!aktivnaProvjerena) provjeriNarudzbu();
        return aktivna;
    }

    public List<Jelovnik> getJelovnik() {
        return jelovnikPregled.getJelovnik();
    }

    public List<KartaPica> getPica() {
        return picaPregled.getPica();
    }

    public List<Narudzba> getNarucenaJela() {
        return narucenaJela;
    }

    public List<Narudzba> getNarucenaPica() {
        return narucenaPica;
    }

    public String getOdabranoJeloId() {
        return odabranoJeloId;
    }

    public void setOdabranoJeloId(String id) {
        this.odabranoJeloId = id;
    }

    public String getOdabranoPiceId() {
        return odabranoPiceId;
    }

    public void setOdabranoPiceId(String id) {
        this.odabranoPiceId = id;
    }

    public int getKolicinaJelo() {
        return kolicinaJelo;
    }

    public void setKolicinaJelo(int kolicinaJelo) {
        this.kolicinaJelo = kolicinaJelo;
    }

    public int getKolicinaPice() {
        return kolicinaPice;
    }

    public void setKolicinaPice(int kolicinaPice) {
        this.kolicinaPice = kolicinaPice;
    }

    public String getPoruka() {
        return poruka;
    }
    
    private void dodajZapisUBazu(String korisnickoIme, String opis) {
        try {
            utx.begin();

            Zapisi zapis = new Zapisi();
            zapis.setKorisnickoime(korisnickoIme);
            zapis.setOpisrada(opis);
            zapis.setVrijeme(new java.sql.Timestamp(System.currentTimeMillis()));

            var request = FacesContext.getCurrentInstance().getExternalContext().getRequest();
            if (request instanceof HttpServletRequest req) {
                zapis.setIpadresaracunala(req.getRemoteAddr());
                zapis.setAdresaracunala(req.getRemoteHost());
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
