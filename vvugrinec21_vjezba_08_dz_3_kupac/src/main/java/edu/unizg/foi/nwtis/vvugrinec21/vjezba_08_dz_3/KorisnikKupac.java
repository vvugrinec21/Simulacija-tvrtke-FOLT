package edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Klijentska aplikacija koja omogućuje kupcu interakciju s poslužiteljem partnera.
 * Podržava izvršavanje različitih komandi poput dohvaćanja jelovnika, karte pića,
 * kreiranja narudžbi, dodavanja jela i pića u narudžbu, te izdavanja računa.
 * Komande se učitavaju iz konfiguracijske datoteke i izvršavaju sekvencijalno.
 */
public class KorisnikKupac {

    /** Port za registraciju na poslužitelju */
    private int mreznaVrataRegistracija;
    
    /** Identifikator partnera */
    private int partner;
    
    /** Putanja do datoteke s komandama koje će se izvršavati */
    private String datotekaPodataka;


    /**
     * Postavlja putanju do datoteke s komandama.
     * 
     * @param datotekaPodataka putanja do datoteke s komandama koje će se izvršavati
     */
    public void setDatotekaPodataka(String datotekaPodataka) {
        this.datotekaPodataka = datotekaPodataka;
    }

    /**
     * Učitava konfiguraciju iz datoteke.
     * Postavlja vrijednosti za mrežna vrata registracije i identifikator partnera.
     * U slučaju greške pri učitavanju, program se prekida.
     * 
     * @param nazivDatoteke putanja do konfiguracijske datoteke
     */
    public void ucitajKonfiguraciju(String nazivDatoteke) {
        Path path = Path.of(nazivDatoteke);
        if (!Files.exists(path) || !Files.isReadable(path)) {
            System.exit(1);
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(nazivDatoteke), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#"))
                    continue;
                String[] dijelovi = line.split("=", 2);
                if (dijelovi.length == 2) {
                    String kljuc = dijelovi[0].trim();
                    String vrijednost = dijelovi[1].trim();
                    switch (kljuc) {
                    case "mreznaVrataRegistracija":
                        mreznaVrataRegistracija = Integer.parseInt(vrijednost);
                        break;
                    case "partner":
                        partner = Integer.parseInt(vrijednost);
                        break;
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.exit(1);
        }
    }

    /**
     * Glavna metoda koja pokreće aplikaciju.
     * Provjerava ispravnost argumenata komandne linije, inicijalizira instancu KorisnikKupac
     * i pokreće izvršavanje komandi.
     * 
     * @param args argumenti komandne linije (očekuju se dva argumenta: putanja do konfiguracijske datoteke i putanja do datoteke s komandama)
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("ERROR 40: Format komande nije ispravan");
            return;
        }

        String nazivDatotekeKonfiguracije = args[0];
        String datotekaPodataka = args[1];

        KorisnikKupac kupac = new KorisnikKupac();
        kupac.setDatotekaPodataka(datotekaPodataka);
        kupac.ucitajKonfiguraciju(nazivDatotekeKonfiguracije);
        
        kupac.pokreni();
    }

    /**
     * Pokreće izvršavanje komandi.
     * Učitava komande iz datoteke i sekvencijalno ih izvršava s odgovarajućim pauzama između njih.
     */
    public void pokreni() {
        List<String[]> komande = ucitajKomande();
        if (komande.isEmpty()) {
            return;
        }
        for (String[] komanda : komande) {
            if (komanda.length != 5) {
                System.out.println("ERROR 40: Format komande nije ispravan");
                continue;
            }

            String korisnik = komanda[0];
            String adresa = komanda[1];
            int port;
            int spavanje;
            try {
                port = Integer.parseInt(komanda[2]);
                spavanje = Integer.parseInt(komanda[3]);
            } catch (NumberFormatException e) {
                
                continue;
            }
            String naredba = komanda[4];

            try {
                
                Thread.sleep(spavanje);
                izvrsiKomandu(korisnik, adresa, port, naredba);
            } catch (InterruptedException e) {
                
            } catch (Exception e) {
                
            }
        }
    }

    /**
     * Učitava komande iz datoteke.
     * Svaka linija datoteke treba biti u formatu: korisnik;adresa;port;spavanje;naredba
     * 
     * @return lista polja stringova gdje svako polje predstavlja jednu komandu
     */
    private List<String[]> ucitajKomande() {
        List<String[]> komande = new ArrayList<>();
        Path putanja = Path.of(datotekaPodataka);

        if (!Files.exists(putanja) || !Files.isReadable(putanja)) {
            return komande;
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(datotekaPodataka), StandardCharsets.UTF_8))) {
            String linija;
            while ((linija = br.readLine()) != null) {
                if (linija.trim().isEmpty())
                    continue;
                String[] dijelovi = linija.split(";");
                if (dijelovi.length == 5) {
                    komande.add(dijelovi);
                } else {
                
                }
            }
        } catch (IOException e) {
            
        }
        return komande;
    }
    
    /**
     * Izvršava pojedinačnu komandu.
     * Uspostavlja vezu s poslužiteljem, šalje komandu i obrađuje odgovor.
     * 
     * @param korisnik identifikator korisnika
     * @param adresa adresa poslužitelja
     * @param port port poslužitelja
     * @param naredba komanda koja se šalje poslužitelju
     */
    private void izvrsiKomandu(String korisnik, String adresa, int port, String naredba) {
        
        
        try (Socket socket = new Socket(adresa, port);
                PrintWriter out = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {
            
            if (!validirajKomandu(korisnik, naredba)) {
                return;
            }
            
            if (!posaljiKomandu(out, naredba)) {
                return;
            }
            
        } catch (IOException e) {
            //System.out.println("Greška pri komunikaciji: " + e.getMessage());
        }
    }
    
    /**
     * Validira komandu prije slanja.
     * Provjerava format komande i podudaranje identifikatora korisnika s drugim argumentom komande.
     * 
     * @param korisnik identifikator korisnika
     * @param naredba komanda koja se validira
     * @return true ako je komanda validna, false inače
     */
    private boolean validirajKomandu(String korisnik, String naredba) {
        String prviDio = korisnik.trim();
        String[] dijeloviNaredbe = naredba.split(" ");
        String tipKomande = dijeloviNaredbe[0];
        
        List<String> komandeJednostavne = List.of("POPIS");
        
        if (!komandeJednostavne.contains(tipKomande)) {
            if (dijeloviNaredbe.length < 2) {
                return false;
            }

            String drugiArgument = dijeloviNaredbe[1];
            if (!drugiArgument.equals(prviDio)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Šalje komandu poslužitelju.
     * 
     * @param out izlazni tok za slanje komande
     * @param naredba komanda koja se šalje
     * @return true ako je komanda uspješno poslana
     */
    private boolean posaljiKomandu(PrintWriter out, String naredba) {
        out.println(naredba);
        return true;
    }
  
   
}
