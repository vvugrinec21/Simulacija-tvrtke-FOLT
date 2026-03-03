package edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3.jf;

import edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3.jpa.entiteti.Obracuni;
import edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3.jpa.entiteti.Partneri;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

@Named("pregledObracuna")
@RequestScoped
public class PregledObracuna implements Serializable {

    @PersistenceContext(unitName = "vjezba_08_dz_3_jpa")
    private EntityManager em;

    private int odabraniPartnerId;
    private Date datumOd;
    private Date datumDo;
    private List<Obracuni> obracuni;

    public List<Partneri> getSviPartneri() {
        return em.createNamedQuery("Partneri.findAll", Partneri.class).getResultList();
    }

    public void ucitajObracune() {
        StringBuilder jpql = new StringBuilder("SELECT o FROM Obracuni o WHERE o.partneri.id = :partnerId");

        if (datumOd != null) {
            jpql.append(" AND o.vrijeme >= :od");
        }
        if (datumDo != null) {
            jpql.append(" AND o.vrijeme <= :do");
        }

        TypedQuery<Obracuni> query = em.createQuery(jpql.toString(), Obracuni.class);
        query.setParameter("partnerId", odabraniPartnerId);

        if (datumOd != null) {
            query.setParameter("od", new Timestamp(datumOd.getTime()));
        }
        if (datumDo != null) {
            query.setParameter("do", new Timestamp(datumDo.getTime()));
        }

        obracuni = query.getResultList();
    }

    public int getOdabraniPartnerId() {
        return odabraniPartnerId;
    }

    public void setOdabraniPartnerId(int odabraniPartnerId) {
        this.odabraniPartnerId = odabraniPartnerId;
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

    public List<Obracuni> getObracuni() {
        return obracuni;
    }
}
