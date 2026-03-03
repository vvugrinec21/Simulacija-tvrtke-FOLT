package edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3.jf;

import edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3.jpa.entiteti.Korisnici;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.io.Serializable;
import java.util.List;

@Named("pregledKorisnika")
@RequestScoped
public class PregledKorisnika implements Serializable {

    @PersistenceContext(unitName = "vjezba_08_dz_3_jpa")
    private EntityManager em;

    private String ime;
    private String prezime;
    private List<Korisnici> rezultati;
    private String poruka;

    public void pretrazi() {
        try {
            StringBuilder upit = new StringBuilder("SELECT k FROM Korisnici k WHERE 1=1");

            if (ime != null && !ime.isBlank()) {
                upit.append(" AND LOWER(k.ime) LIKE LOWER(:ime)");
            }
            if (prezime != null && !prezime.isBlank()) {
                upit.append(" AND LOWER(k.prezime) LIKE LOWER(:prezime)");
            }

            TypedQuery<Korisnici> query = em.createQuery(upit.toString(), Korisnici.class);

            if (ime != null && !ime.isBlank()) {
                query.setParameter("ime", "%" + ime + "%");
            }
            if (prezime != null && !prezime.isBlank()) {
                query.setParameter("prezime", "%" + prezime + "%");
            }

            rezultati = query.getResultList();
            poruka = "Pronađeno korisnika: " + rezultati.size();

        } catch (Exception e) {
            poruka = "Greška pri pretraživanju: " + e.getMessage();
            rezultati = null;
        }
    }

    public String getIme() {
        return ime;
    }

    public void setIme(String ime) {
        this.ime = ime;
    }

    public String getPrezime() {
        return prezime;
    }

    public void setPrezime(String prezime) {
        this.prezime = prezime;
    }

    public List<Korisnici> getRezultati() {
        return rezultati;
    }

    public String getPoruka() {
        return poruka;
    }
}
