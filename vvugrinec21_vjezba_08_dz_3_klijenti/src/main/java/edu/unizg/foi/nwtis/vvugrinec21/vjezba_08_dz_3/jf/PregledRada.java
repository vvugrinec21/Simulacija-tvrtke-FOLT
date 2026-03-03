package edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3.jf;

import edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3.jpa.entiteti.Zapisi;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

@Named("pregledRada")
@RequestScoped
public class PregledRada implements Serializable {

    @PersistenceContext(unitName = "vjezba_08_dz_3_jpa")
    private EntityManager em;

    private String korisnickoIme;
    private Date datumOd;
    private Date datumDo;
    private List<Zapisi> zapisi;

    public void ucitajZapise() {
        StringBuilder jpql = new StringBuilder("SELECT z FROM Zapisi z WHERE z.korisnickoime = :korime");

        if (datumOd != null) {
            jpql.append(" AND z.vrijeme >= :od");
        }
        if (datumDo != null) {
            jpql.append(" AND z.vrijeme <= :do");
        }

        TypedQuery<Zapisi> query = em.createQuery(jpql.toString(), Zapisi.class);
        query.setParameter("korime", korisnickoIme);

        if (datumOd != null) {
            query.setParameter("od", new Timestamp(datumOd.getTime()));
        }
        if (datumDo != null) {
            query.setParameter("do", new Timestamp(datumDo.getTime()));
        }

        zapisi = query.getResultList();
    }


    public String getKorisnickoIme() {
        return korisnickoIme;
    }

    public void setKorisnickoIme(String korisnickoIme) {
        this.korisnickoIme = korisnickoIme;
    }

    public Date getDatumOd() {
        return datumOd;
    }

    public void setDatumOd(Date datumOd) {
        this.datumOd = datumOd;
    }

    public Date getDatumDo() {
        return datumDo;
    }

    public void setDatumDo(Date datumDo) {
        this.datumDo = datumDo;
    }

    public List<Zapisi> getZapisi() {
        return zapisi;
    }
}
