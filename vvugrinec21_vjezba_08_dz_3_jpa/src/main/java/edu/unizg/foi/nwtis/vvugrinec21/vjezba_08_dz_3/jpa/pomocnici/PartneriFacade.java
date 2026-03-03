package edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3.jpa.pomocnici;

import java.util.List;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3.jpa.entiteti.Partneri;

@Stateless
public class PartneriFacade {

    @PersistenceContext(unitName = "vjezba_08_dz_3_jpa")
    private EntityManager em;

    public void create(Partneri partner) {
        em.persist(partner);
    }

    public void edit(Partneri partner) {
        em.merge(partner);
    }

    public void remove(Partneri partner) {
        em.remove(em.merge(partner));
    }

    public Partneri find(Object id) {
        return em.find(Partneri.class, id);
    }

    public List<Partneri> findAll() {
        TypedQuery<Partneri> query = em.createNamedQuery("Partneri.findAll", Partneri.class);
        return query.getResultList();
    }

    public boolean exists(int id) {
        return find(id) != null;
    }
}
