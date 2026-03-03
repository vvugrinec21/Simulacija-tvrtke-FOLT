package edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;


@ApplicationScoped
@Named("globalniPodaci")
public class GlobalniPodaci implements Serializable {

	
	@Inject
	@ConfigProperty(name = "idPartner")
	private String idPartnerTekst;

	private int idPartnera;

	@PostConstruct
	private void init() {
	    try {
	        idPartnera = Integer.parseInt(idPartnerTekst);
	    } catch (NumberFormatException e) {
	        idPartnera = 1; 
	    }
	}
	
	
    private static final long serialVersionUID = 1L;

    private int brojObracuna = 0;

    private Map<Integer, Integer> brojOtvorenihNarudzbi = new ConcurrentHashMap<>();

    private Map<Integer, Integer> brojRacuna = new ConcurrentHashMap<>();


    public int getBrojObracuna() {
        return brojObracuna;
    }

    public void povecajBrojObracuna() {
        this.brojObracuna++;
    }

    public void resetirajBrojObracuna() {
        this.brojObracuna = 0;
    }

    public void povecajOtvoreneNarudzbe(int idPartner) {
        brojOtvorenihNarudzbi.merge(idPartner, 1, Integer::sum);
    }

    public void smanjiOtvoreneNarudzbe(int idPartner) {
        if (brojOtvorenihNarudzbi.containsKey(idPartner)) {
            int broj = brojOtvorenihNarudzbi.get(idPartner);
            if (broj > 0) {
                brojOtvorenihNarudzbi.put(idPartner, broj - 1);
            } else {
                brojOtvorenihNarudzbi.put(idPartner, 0);
            }
        }
    }

    public int getOtvoreneNarudzbe(int idPartner) {
        return brojOtvorenihNarudzbi.getOrDefault(idPartner, 0);
    }

    public void povecajBrojRacuna(int idPartner) {
        brojRacuna.merge(idPartner, 1, Integer::sum);
    }

    public int getBrojRacuna(int idPartner) {
        return brojRacuna.getOrDefault(idPartner, 0);
    }

    public void resetirajSvePodatke() {
        brojObracuna = 0;
        brojOtvorenihNarudzbi.clear();
        brojRacuna.clear();
    }
    
    public int getIdPartnera() {
        return idPartnera;
    }
    
    
}
