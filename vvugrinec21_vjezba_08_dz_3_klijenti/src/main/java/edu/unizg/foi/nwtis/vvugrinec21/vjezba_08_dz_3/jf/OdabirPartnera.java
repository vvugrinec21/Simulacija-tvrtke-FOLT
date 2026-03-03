package edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3.jf;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import edu.unizg.foi.nwtis.podaci.Partner;
import edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3.jpa.entiteti.Partneri;
import edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3.jpa.pomocnici.PartneriFacade;

@RequestScoped
@Named("odabirParnera")
public class OdabirPartnera implements Serializable {

  private static final long serialVersionUID = -524581462819739622L;

  @Inject
  PrijavaKorisnika prijavaKorisnika;

  @Inject
  private PartneriFacade partneriFacade;

  private List<Partner> partneri;

  private int partner;

  public int getPartner() {
    return partner;
  }

  public void setPartner(int partner) {
    this.partner = partner;
  }

  public List<Partner> getPartneri() {
    return partneri;
  }

  @PostConstruct
  public void ucitajPartnere() {
    List<Partneri> entiteti = partneriFacade.findAll();

    partneri = entiteti.stream()
        .map(e -> new Partner(
            e.getId(),
            e.getNaziv(),
            e.getVrstakuhinje(),
            e.getAdresa(),
            e.getMreznavrata(),
            e.getMreznavratakraj(),
            (float) e.getGpssirina(),
            (float) e.getGpsduzina(),
            e.getSigurnosnikod(),
            e.getAdminkod()
        ))
        .collect(Collectors.toList());
  }

  public String odaberiPartnera() {
    if (this.partner > 0) {
      Optional<Partner> partnerO = this.partneri.stream()
          .filter((p) -> p.id() == this.partner).findFirst();
      if (partnerO.isPresent()) {
        this.prijavaKorisnika.setOdabraniPartner(partnerO.get());
        this.prijavaKorisnika.setPartnerOdabran(true);
      } else {
        this.prijavaKorisnika.setPartnerOdabran(false);
      }
    } else {
      this.prijavaKorisnika.setPartnerOdabran(false);
    }
    return "/index.xhtml?faces-redirect=true";
  }
}
