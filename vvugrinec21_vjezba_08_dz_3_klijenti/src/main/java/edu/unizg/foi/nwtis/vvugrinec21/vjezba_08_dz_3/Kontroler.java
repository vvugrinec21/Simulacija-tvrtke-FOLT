/*
 * To change this license header, choose License Headers in Project Properties. To change this
 * template file, choose Tools | Templates and open the template in the editor.
 */
package edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.sql.Timestamp;

import java.util.List;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import edu.unizg.foi.nwtis.podaci.Obracun;
import edu.unizg.foi.nwtis.podaci.Partner;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.mvc.Controller;
import jakarta.mvc.Models;
import jakarta.mvc.View;
import jakarta.mvc.binding.BindingResult;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;

/**
 *
 * @author NWTiS
 */
@Controller
@Path("tvrtka")
@RequestScoped
public class Kontroler {

  @Inject
  private Models model;

  @Inject
  private BindingResult bindingResult;

  @Inject
  @RestClient
  ServisTvrtkaKlijent servisTvrtka;

  @GET
  @Path("pocetak")
  @View("index.jsp")
  public void pocetak() {}

  @GET
  @Path("kraj")
  @View("status.jsp")
  public void kraj() {
    try {
      var status = this.servisTvrtka.headPosluziteljKraj().getStatus();
      this.model.put("statusOperacije", status);
      dohvatiStatuse();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @GET
  @Path("status")
  @View("status.jsp")
  public void status() {
    dohvatiStatuse();
  }

  @GET
  @Path("start/{id}")
  @View("status.jsp")
  public void startId(@PathParam("id") int id) {
    var status = this.servisTvrtka.headPosluziteljStart(id).getStatus();
    this.model.put("status", status);
    this.model.put("samoOperacija", true);
  }

  @GET
  @Path("pauza/{id}")
  @View("status.jsp")
  public void pauzatId(@PathParam("id") int id) {
    var status = this.servisTvrtka.headPosluziteljPauza(id).getStatus();
    this.model.put("status", status);
    this.model.put("samoOperacija", true);
  }

  @GET
  @Path("partner")
  @View("partneri.jsp")
  public void partneri() {
    var odgovor = this.servisTvrtka.getPartneri();
    var status = odgovor.getStatus();
    if (status == 200) {
      var partneri = odgovor.readEntity(new GenericType<List<Partner>>() {});
      this.model.put("status", status);
      this.model.put("partneri", partneri);
    }
  }
  
  @GET
  @Path("partner/{id}")
  @View("partner.jsp")
  public void jedanPartner(@PathParam("id") int id) {
      var odgovor = this.servisTvrtka.getPartner(id);
      var status = odgovor.getStatus();
      if (status == 200) {
          var partner = odgovor.readEntity(Partner.class);
          this.model.put("partner", partner);
      } else {
          this.model.put("greska", "Partner nije pronađen (status " + status + ")");
      }
  }
 
  
  @GET
  @Path("admin/nadzornaKonzolaTvrtka")
  @View("nadzornaKonzolaTvrtka.jsp")
  public void nadzornaKonzolaTvrtka() {
	  dohvatiStatuse();
	  
  }
  	
  
  private void dohvatiStatuse() {
	    this.model.put("samoOperacija", false);

	    try {
	        var statusT = this.servisTvrtka.headPosluzitelj().getStatus();
	        this.model.put("statusT", statusT);
	    } catch (Exception e) {
	        this.model.put("statusT", "NE RADI");
	    }

	    try {
	        var statusT1 = this.servisTvrtka.headPosluziteljStatus(1).getStatus();
	        this.model.put("statusT1", statusT1);
	    } catch (Exception e) {
	        this.model.put("statusT1", "NE RADI");
	    }

	    try {
	        var statusT2 = this.servisTvrtka.headPosluziteljStatus(2).getStatus();
	        this.model.put("statusT2", statusT2);
	    } catch (Exception e) {
	        this.model.put("statusT2", "NE RADI");
	    }
	}
  
  
  @GET
  @Path("privatno/obracun/vrsta")
  @View("obracunVrstaForm.jsp")
  public void formaPoVrsti() {}

  @GET
  @Path("privatno/obracun/partner")
  @View("obracunPartnerForm.jsp")
  public void formaPoPartneru() {}
  
  @GET
  @Path("privatno/obracun/rezultati")
  @View("obracunRezultati.jsp")
  public void dohvatiObracune(@jakarta.ws.rs.QueryParam("od") String od,
                              @jakarta.ws.rs.QueryParam("do") String ddo,
                              @jakarta.ws.rs.QueryParam("tip") String tip,
                              @jakarta.ws.rs.QueryParam("partnerId") Integer partnerId) {

      Long odMillis = null;
      Long doMillis = null;

      try {
    	  DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    	  if (od != null && !od.isBlank()) {
    	      LocalDateTime odLdt = LocalDateTime.parse(od, formatter);
    	      odMillis = Timestamp.valueOf(odLdt).getTime();
    	  }
    	  if (ddo != null && !ddo.isBlank()) {
    	      LocalDateTime doLdt = LocalDateTime.parse(ddo, formatter);
    	      doMillis = Timestamp.valueOf(doLdt).getTime();
    	  }
      } catch (Exception e) {
          this.model.put("greska", "Greška u unosu datuma: " + e.getMessage());
          this.model.put("povratak", (partnerId != null) ? "partner" : "vrsta");
          return;
      }

      Response response;
      try {
          switch (tip != null ? tip : "") {
              case "jelo" -> response = this.servisTvrtka.getObracunJelo(odMillis, doMillis);
              case "pice" -> response = this.servisTvrtka.getObracunPice(odMillis, doMillis);
              default -> {
                  if (partnerId != null) {
                      response = this.servisTvrtka.getObracunPartner(partnerId, odMillis, doMillis);
                  } else {
                      response = this.servisTvrtka.getObracun(odMillis, doMillis);
                  }
              }
          }

          int status = response.getStatus();
          if (status == 200) {
              List<Obracun> obracuni = response.readEntity(new GenericType<>() {});
              this.model.put("obracuni", obracuni);
          } else {
              this.model.put("greska", "Greška pri dohvaćanju obračuna (status " + status + ")");
          }

      } catch (Exception e) {
          this.model.put("greska", "Greška tijekom poziva REST servisa: " + e.getMessage());
      }

      this.model.put("povratak", (partnerId != null) ? "partner" : "vrsta");
  }
  
  
  @GET
  @Path("admin/partner/dodaj")
  @View("noviPartner.jsp")  
  public void prikaziFormuPartnera() {

  }
  
  @POST
  @Path("admin/partner/dodaj")
  @View("noviPartner.jsp")
  public void dodajPartnera(@FormParam("id") int id,
                            @FormParam("naziv") String naziv,
                            @FormParam("vrstaKuhinje") String vrstaKuhinje,
                            @FormParam("adresa") String adresa,
                            @FormParam("mreznaVrata") int mreznaVrata,
                            @FormParam("mreznaVrataKraj") int mreznaVrataKraj,
                            @FormParam("gpsSirina") float gpsSirina,
                            @FormParam("gpsDuzina") float gpsDuzina,
                            @FormParam("sigurnosniKod") String sigurnosniKod,
                            @FormParam("adminKod") String adminKod) {

      Partner novi = new Partner(id, naziv, vrstaKuhinje, adresa,
                                 mreznaVrata, mreznaVrataKraj,
                                 gpsSirina, gpsDuzina, sigurnosniKod, adminKod);

      
      try {
          Response response = this.servisTvrtka.dodajPartner(novi);
          int status = response.getStatus();

          if (status == 200 || status == 201) {
              this.model.put("uspjeh", "Partner je uspješno dodan!");
          } else {
              this.model.put("greska", "Greška pri dodavanju partnera (status " + status + ")");
          }

      } catch (jakarta.ws.rs.WebApplicationException e) {
          int status = e.getResponse().getStatus();

          if (status == 409) {
              this.model.put("greska", "Partner s tim ID-em već postoji!");
          } else {
              this.model.put("greska", "Greška: " + e.getMessage() + " (status " + status + ")");
          }

      } catch (Exception e) {
          this.model.put("greska", "Neočekivana greška: " + e.getMessage());
      }
  }

  @GET
  @Path("admin/spava")
  @View("spavanje.jsp")
  public void prikaziSpavanjeFormu() {}
  
  
  @POST
  @Path("admin/spava")
  @View("spavanje.jsp")
  public void aktivirajSpavanje(@FormParam("vrijeme") int vrijeme) {
      Response res = this.servisTvrtka.spava(vrijeme);

      if (res.getStatus() == 200) {
          model.put("uspjeh", "Spavanje aktivirano na " + vrijeme + " sekundi.");
          model.put("onemoguci", vrijeme);
      } else {
          model.put("greska", "Greška pri aktivaciji spavanja (status " + res.getStatus() + ")");
      }
  }

}
  
