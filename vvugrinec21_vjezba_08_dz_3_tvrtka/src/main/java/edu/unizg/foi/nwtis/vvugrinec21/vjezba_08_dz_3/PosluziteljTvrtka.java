package edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import edu.unizg.foi.nwtis.konfiguracije.Konfiguracija;
import edu.unizg.foi.nwtis.konfiguracije.KonfiguracijaApstraktna;
import edu.unizg.foi.nwtis.konfiguracije.NeispravnaKonfiguracija;
import edu.unizg.foi.nwtis.podaci.Jelovnik;
import edu.unizg.foi.nwtis.podaci.KartaPica;
import edu.unizg.foi.nwtis.podaci.Obracun;
import edu.unizg.foi.nwtis.podaci.Partner;

// TODO: Auto-generated Javadoc
/**
 * The Class PosluziteljTvrtka.
 */

/**
 * Poslužitelj tvrtke koji upravlja jelovnicima, kartama pića i partnerima.
 * Omogućuje registraciju partnera, obradu narudžbi i obračuna, te distribuciju jelovnika i karti pića.
 * Poslužitelj koristi virtualne dretve za obradu zahtjeva i podržava tri vrste servisa:
 *
 * Servis za kraj rada (mreznaVrataKraj)
 * Servis za registraciju partnera (mreznaVrataRegistracija)>
 * Servis za rad s partnerima (mreznaVrataRad)>
 * >
 */
public class PosluziteljTvrtka {

	/** Konfiguracijski podaci. */
	private Konfiguracija konfig;

	/** Pokretač dretvi. */
	private ExecutorService executor = null;

	/** Pauza dretve. */
	private int pauzaDretve = 1000;

	/** Kod za kraj rada. */
	private String kodZaKraj = "";

	/** Zastavica za kraj rada. */
	private AtomicBoolean kraj = new AtomicBoolean(false);

	/** The predlozak kraj. */
	private Pattern predlozakKraj = Pattern.compile("^KRAJ ([A-Z]+)$");

	/** The kuhinje. */
	private Map<String, String> kuhinje = new ConcurrentHashMap<>();
	
	/** The jelovnici. */
	private Map<String, Map<String, Jelovnik>> jelovnici = new ConcurrentHashMap<>();
	
	/** The karta pica. */
	private Map<String, KartaPica> kartaPica = new ConcurrentHashMap<>();
	
	/** The partneri. */
	private Map<Integer, Partner> partneri = new ConcurrentHashMap<>();

	/** The pokrenute dretve. */
	private List<Future<?>> pokrenuteDretve = new ArrayList<>();
	
	/** The aktivne uticnice. */
	private final Map<Future<?>, Socket> aktivneUticnice = new ConcurrentHashMap<>();

	/** The zatvorenih veza pri prekidu. */
	private final AtomicInteger zatvorenihVezaPriPrekidu = new AtomicInteger(0);

	/** The je prekinut ctrl C. */
	private final AtomicBoolean jePrekinutCtrlC = new AtomicBoolean(false);

	/** The tvrtka lock. */
	ReentrantLock tvrtkaLock = new ReentrantLock();

	/** The predlozak jelovnik. */
	private Pattern predlozakJelovnik = Pattern.compile("^JELOVNIK\\s+(\\d+)\\s+([a-zA-Z0-9]+)$");
	
	/** The predlozak karta pica. */
	private Pattern predlozakKartaPica = Pattern.compile("^KARTAPIĆA\\s+(\\d+)\\s+([a-zA-Z0-9]+)$");
	
	/** The predlozak obracun. */
	private Pattern predlozakObracun = Pattern.compile("^OBRAČUN\\s+(\\d+)\\s+([a-zA-Z0-9]+)$");
	
	private Pattern predlozakObracunWS = Pattern.compile("^OBRAČUNWS\\s+(\\d+)\\s+([a-zA-Z0-9]+)$");

	private volatile boolean statusRegistracija = true;
	private volatile boolean statusPartneri = true;
	
	/**
	 * Vraća konfiguraciju poslužitelja.
	 * 
	 * @return objekt konfiguracije
	 */
	public Konfiguracija getKonfig() {
		return konfig;
	}

	/**
	 * Glavna metoda koja pokreće poslužitelj tvrtke.
	 * Provjerava broj argumenata, inicijalizira poslužitelj i postavlja shutdown hook za prekid rada.
	 * 
	 * @param args argumenti komandne linije (očekuje se putanja do konfiguracijske datoteke)
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Broj argumenata nije 1.");
			return;
		}

		var program = new PosluziteljTvrtka();
		var nazivDatoteke = args[0];

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			program.jePrekinutCtrlC.set(true);
			program.prekiniRukovanje();
		}));

		program.pripremiKreni(nazivDatoteke);
	}

	/**
	 * Prekida rad poslužitelja i zatvara sve aktivne veze.
	 * Ako je program prekinut s Ctrl+C, ispisuje statistiku o zatvorenim vezama.
	 */
	private void prekiniRukovanje() {
		if (executor != null) {
			for (Map.Entry<Future<?>, Socket> entry : aktivneUticnice.entrySet()) {
				Future<?> dretva = entry.getKey();
				Socket socket = entry.getValue();
				try {
					if (socket != null && !socket.isClosed()) {
						socket.close();
						zatvorenihVezaPriPrekidu.incrementAndGet();
					}
				} catch (IOException ignored) {
				}
				dretva.cancel(true);
			}

			if (jePrekinutCtrlC.get()) {
				System.out.println("Zatvoreno veza u trenutku prekida: " + zatvorenihVezaPriPrekidu.get());
				System.out.println("Ukupno prekinutih dretvi: " + aktivneUticnice.size());
			}

			executor.shutdownNow();
		}
	}

	/**
	 * Priprema i pokreće poslužitelj.
	 * Učitava konfiguraciju, kartu pića, partnere i jelovnike, te pokreće servise za rad.
	 * 
	 * @param nazivDatoteke putanja do konfiguracijske datoteke
	 */
	public void pripremiKreni(String nazivDatoteke) {
		if (!this.ucitajKonfiguraciju(nazivDatoteke) || !this.ucitajKartuPica() || !this.ucitajPartnere()
				|| !this.ucitajJelovnike()) {
			return;
		}
		this.kodZaKraj = this.konfig.dajPostavku("kodZaKraj");
		this.pauzaDretve = Integer.parseInt(this.konfig.dajPostavku("pauzaDretve"));

		executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory());

		dodajDretvu(this::pokreniPosluziteljKraj);
		dodajDretvu(this::pokreniPosluziteljRegistracija);
		dodajDretvu(this::pokreniPosluziteljRad);

		while (!kraj.get()) {
			try {
				Thread.sleep(pauzaDretve);
			} catch (InterruptedException ignored) {
			}
		}

		if (!jePrekinutCtrlC.get()) {
			prekiniRukovanje();
		}

	}

	/**
	 * Dodaje novu dretvu u listu pokrenutih dretvi.
	 * 
	 * @param zadatak zadatak koji će se izvršavati u dretvi
	 */
	private void dodajDretvu(Runnable zadatak) {
		Future<?> dretva = executor.submit(() -> {
			try {
				zadatak.run();
			} catch (Exception ignored) {
			}
		});
		pokrenuteDretve.add(dretva);
	}

	/**
	 * Registrira utičnicu u mapu aktivnih utičnica.
	 * 
	 * @param dretva dretva koja obrađuje utičnicu
	 * @param socket mrežna utičnica
	 */
	public void registrirajUticnicu(Future<?> dretva, Socket socket) {
		aktivneUticnice.put(dretva, socket);
	}

	/**
	 * Pokreće poslužitelj za registraciju partnera.
	 * Osluškuje na portu definiranom u konfiguraciji i obrađuje zahtjeve za registraciju.
	 */
	public void pokreniPosluziteljRegistracija() {
		int port = Integer.parseInt(konfig.dajPostavku("mreznaVrataRegistracija"));

		try (ServerSocket serverSocket = new ServerSocket(port)) {
			System.out.println("Posluzitelj REGISTRACIJA pokrenut na: " + InetAddress.getLocalHost().getHostAddress() + ":" + serverSocket.getLocalPort());

			while (!kraj.get()) {
				Socket socket = serverSocket.accept();
				executor.submit(() -> obradiRegistraciju(socket));
			}
			serverSocket.close();
		} catch (IOException e) {
		}
	}

	/**
	 * Pokreće poslužitelj za kraj rada.
	 * Osluškuje na portu definiranom u konfiguraciji i obrađuje zahtjeve za kraj rada.
	 */
	public void pokreniPosluziteljKraj() {
	    var mreznaVrata = Integer.parseInt(this.konfig.dajPostavku("mreznaVrataKraj"));
	    var brojCekaca = 0;

	    try (ServerSocket ss = new ServerSocket(mreznaVrata, brojCekaca)) {
	        System.out.println("Posluzitelj KRAJ pokrenut na: " + InetAddress.getLocalHost().getHostAddress() + ":" + ss.getLocalPort());

	        while (!this.kraj.get()) {
	            var mreznaUticnica = ss.accept();

	            executor.submit(() -> {
	                try (BufferedReader in = new BufferedReader(new InputStreamReader(mreznaUticnica.getInputStream(), "utf8"));
	                     PrintWriter out = new PrintWriter(new OutputStreamWriter(mreznaUticnica.getOutputStream(), "utf8"))) {

	                    String linija = in.readLine();
	                    if (linija == null || linija.isBlank()) {
	                        posaljiGresku(out, "ERROR 20 - Format komande nije ispravan\n");
	                        return;
	                    }

	                    obradiKraj(linija, out, mreznaUticnica);

	                } catch (IOException e) {
	                    
	                }
	            });
	        }
	    } catch (IOException e) {
	    }
	}


	/**
	 * Obrađuje zahtjev za registraciju partnera.
	 * Podržava komande: PARTNER, OBRIŠI i POPIS.
	 * 
	 * @param socket mrežna utičnica preko koje je primljen zahtjev
	 */
	public void obradiRegistraciju(Socket socket) {
	    try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf8"));
	         PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "utf8"))) {

	        // Dodana provjera statusa registracije
	        if (!statusRegistracija) {
	            posaljiGresku(out, "ERROR 15 - Poslužitelj za registraciju u pauzi\n");
	            return;
	        }

	        String linija = in.readLine();
	        String[] dijelovi = linija.trim().split(" ", 2);

	        if (dijelovi.length < 1) {
	            posaljiGresku(out, "ERROR 20 - Format komadnije nije ispravan\n");
	            return;
	        }

	        switch (dijelovi[0]) {
	            case "PARTNER":
	                obradiKomanduPartner(linija, out);
	                break;
	            case "OBRIŠI":
	                obradiKomanduObrisi(linija, out);
	                break;
	            case "POPIS":
	                obradiKomanduPopis(out);
	                break;
	            default:
	                posaljiGresku(out, "ERROR 20 - Format komadnije nije ispravan\n");
	        }
	    } catch (Exception e) {
	        posaljiGlobalniError(socket);
	    }
	}

	/**
	 * Obrađuje komandu PARTNER za registraciju novog partnera.
	 * 
	 * @param linija tekst komande
	 * @param out izlazni tok za slanje odgovora
	 */
	private void obradiKomanduPartner(String linija, PrintWriter out) {
		
		Pattern p = Pattern.compile("^PARTNER (\\d+) \"([^\"]+)\" ([A-Z]+) (.+?) (\\d+) (-?\\d+(\\.\\d+)?) (-?\\d+(\\.\\d+)?) (\\d+) (\\S+)$");

	    //Pattern p = Pattern.compile("^PARTNER (\\d+) \"([^\"]+)\" ([A-Z]+) ([^ ]+) (\\d+) (-?\\d+(\\.\\d+)?) (-?\\d+(\\.\\d+)?) (\\S+)$");
	    Matcher m = p.matcher(linija.trim());

	    if (!m.matches()) {
	        posaljiGresku(out, "ERROR 20 - Format komande nije ispravan\n");
	        return;
	    }

	    if (!statusRegistracija) {
	        posaljiGresku(out, "ERROR 24 - Poslužitelj za registraciju partnera u pauzi\n");
	        return;
	    }
	    try
	    {
	    	int id = Integer.parseInt(m.group(1));
	    	
	    	
		    String naziv = m.group(2);
		   
		    
		    String vrstaKuhinje = m.group(3);
		    
		    
		    String adresa = m.group(4);
		    
		    
		    int vrata = Integer.parseInt(m.group(5));
		    
		    
		    float gpsSirina = (float) Double.parseDouble(m.group(6));
		    
		    
		    float gpsDuzina = (float) Double.parseDouble(m.group(8));
		    
		    
		    int vrataKraj = Integer.parseInt(m.group(10));
		    
		    
		    String adminKod = m.group(11);
		     
	  

	    if (partneri.containsKey(id)) {
	        posaljiGresku(out, "ERROR 21 - Već postoji partner s id u kolekciji partnera\n");
	        return;
	    }

	    String sigurnosniKod = Integer.toHexString((naziv + adresa).hashCode());

	    Partner pObj = new Partner(id, naziv, vrstaKuhinje, adresa, vrata, vrataKraj, gpsSirina, gpsDuzina, sigurnosniKod, adminKod);
	    partneri.put(id, pObj);
	    spremiPartnere();

	    out.write("OK " + sigurnosniKod + "\n");
	    out.flush();
	    
	    }catch(Exception e)
	    {
	    	posaljiGresku(out, e.getMessage());
	    }
	}



	/**
	 * Obrađuje komandu OBRIŠI za brisanje partnera.
	 * 
	 * @param linija tekst komande
	 * @param out izlazni tok za slanje odgovora
	 */
	private void obradiKomanduObrisi(String linija, PrintWriter out) {
		Pattern p = Pattern.compile("^OBRIŠI (\\d+) ([a-fA-F0-9]+)$");
		Matcher m = p.matcher(linija.trim());

		if (!m.matches()) {
			posaljiGresku(out, "ERROR 20 - Format komande nije ispravan \n");
			return;
		}

		int id = Integer.parseInt(m.group(1));
		String kod = m.group(2);

		Partner partner = partneri.get(id);

		if (partner == null) {
			posaljiGresku(out, "ERROR 23 - Ne postoji partner s id u kolekciji partnera i/ili neispravan sigurnosni kod partnera\n");
			return;
		}

		if (!partner.sigurnosniKod().equals(kod)) {
			posaljiGresku(out, "ERROR 22 - Neispravan sigurnosni kod partnera\n");
			return;
		}

		partneri.remove(id);
		spremiPartnere();
		out.write("OK\n");
		out.flush();
	}

	/**
	 * Obrađuje komandu POPIS za dohvat popisa svih partnera.
	 * 
	 * @param out izlazni tok za slanje odgovora
	 */
	private void obradiKomanduPopis(PrintWriter out) {
		Gson gson = new Gson();
		String json = gson.toJson(partneri.values());
		out.write("OK\n");
		out.write(json + "\n");
		out.flush();
	}

	/**
	 * Pokreće poslužitelj za rad s partnerima.
	 * Osluškuje na portu definiranom u konfiguraciji i obrađuje zahtjeve za rad.
	 */
	public void pokreniPosluziteljRad() {
		int port = Integer.parseInt(konfig.dajPostavku("mreznaVrataRad"));

		try (ServerSocket serverSocket = new ServerSocket(port)) {
			while (!kraj.get()) {
				Socket socket = serverSocket.accept();
				executor.submit(() -> obradiRad(socket));
			}
			serverSocket.close();
		} catch (IOException e) {
		}
	}

	/**
	 * Obrađuje zahtjev za rad s partnerima.
	 * Podržava komande za dohvat jelovnika, karte pića i slanje obračuna.
	 * 
	 * @param socket mrežna utičnica preko koje je primljen zahtjev
	 */
	public void obradiRad(Socket socket) {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf8"));
				PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "utf8"))) {

			socket.setSoTimeout(1000);
			String linijaKomande = in.readLine();

			if (linijaKomande == null || linijaKomande.isBlank()) {
				posaljiGresku(out, "ERROR 30 - Format komande nije ispravan\n");
				return;
			}

			Matcher mObracun = predlozakObracun.matcher(linijaKomande);
			Matcher mObracunWS = predlozakObracunWS.matcher(linijaKomande);

			if (obradiJelovnikKomandu(linijaKomande, in, out))
			    return;
			if (obradiKartaPicaKomandu(linijaKomande, in, out))
			    return;
			if (mObracun.matches() || mObracunWS.matches()) {
			    boolean jeWS = mObracunWS.matches();
			    obradiObracunKomandu(linijaKomande, in, out, jeWS);
			    return;
			}
			
			
			 if (!statusPartneri) {
			        posaljiGresku(out, "ERROR 15 - Poslužitelj za partnere u pauzi\n");
			        return;
			}

			if (linijaKomande == null || linijaKomande.isBlank()) {
			        posaljiGresku(out, "ERROR 30 - Format komande nije ispravan\n");
			        return;
			}

			posaljiGresku(out, "ERROR 30 - Format komande nije ispravan\n");

		} catch (Exception e) {
			posaljiGlobalniError(socket);
		}
	}

	/**
	 * Šalje poruku o grešci klijentu.
	 * 
	 * @param out izlazni tok za slanje odgovora
	 * @param errorMessage poruka o grešci
	 */
	private void posaljiGresku(PrintWriter out, String errorMessage) {
		out.write(errorMessage);
		out.flush();
	}

	/**
	 * Šalje globalnu poruku o grešci klijentu.
	 * Koristi se kada dođe do neočekivane iznimke tijekom obrade zahtjeva.
	 * 
	 * @param socket mrežna utičnica preko koje je primljen zahtjev
	 */
	private void posaljiGlobalniError(Socket socket) {
		try (PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "utf8"))) {
			out.write("ERROR 39 - Nešto drugo nije u redu\n");
			out.flush();
		} catch (IOException ignored) {
		}
	}

	/**
	 * Obrađuje komandu za dohvat jelovnika.
	 * 
	 * @param linijaKomande tekst komande
	 * @param in ulazni tok za čitanje podataka
	 * @param out izlazni tok za slanje odgovora
	 * @return true ako je komanda prepoznata i obrađena, false inače
	 */
	private boolean obradiJelovnikKomandu(String linijaKomande, BufferedReader in, PrintWriter out) {
		Matcher podudaranjeJelovnik = predlozakJelovnik.matcher(linijaKomande);
		if (!podudaranjeJelovnik.matches()) {
			return false;
		}

		int id = Integer.parseInt(podudaranjeJelovnik.group(1));
		String sigurnosniKod = podudaranjeJelovnik.group(2);

		Partner partner = partneri.get(id);
		if (partner == null || !partner.sigurnosniKod().equals(sigurnosniKod)) {
			posaljiGresku(out, "ERROR 31 - Ne postoji partner s id u kolekciji partnera i/ili neispravan sigurnosni kod partnera\n");
			return true;
		}

		Map<String, Jelovnik> jelovnikZaKuhinju = jelovnici.get(partner.vrstaKuhinje());
		if (jelovnikZaKuhinju == null) {
			posaljiGresku(out, "ERROR 32 - Ne postoji jelovnik s vrstom kuhinje koju partner ima ugovorenu\n");
			return true;
		}

		Gson gson = new Gson();
		out.write("OK\n");
		out.write(gson.toJson(jelovnikZaKuhinju.values()) + "\n");
		out.flush();
		return true;
	}

	/**
	 * Obrađuje komandu za dohvat karte pića.
	 * 
	 * @param linijaKomande tekst komande
	 * @param in ulazni tok za čitanje podataka
	 * @param out izlazni tok za slanje odgovora
	 * @return true ako je komanda prepoznata i obrađena, false inače
	 */
	private boolean obradiKartaPicaKomandu(String linijaKomande, BufferedReader in, PrintWriter out) {
		Matcher podudaranjeKartaPica = predlozakKartaPica.matcher(linijaKomande);
		if (!podudaranjeKartaPica.matches()) {
			return false;
		}

		int id = Integer.parseInt(podudaranjeKartaPica.group(1));
		String sigurnosniKod = podudaranjeKartaPica.group(2);

		Partner partner = partneri.get(id);
		if (partner == null || !partner.sigurnosniKod().equals(sigurnosniKod)) {
			posaljiGresku(out, "ERROR 31 - Ne postoji partner s id u kolekciji partnera i/ili neispravan sigurnosni kod partnera\n");
			return true;
		}

		Gson gson = new Gson();
		out.write("OK\n");
		out.write(gson.toJson(kartaPica.values()) + "\n");
		out.flush();
		return true;
	}

	/**
	 * Obrađuje komandu za slanje obračuna.
	 * 
	 * @param linijaKomande tekst komande
	 * @param in ulazni tok za čitanje podataka
	 * @param out izlazni tok za slanje odgovora
	 * @return true ako je komanda prepoznata i obrađena, false inače
	 */
	private boolean obradiObracunKomandu(String linijaKomande, BufferedReader in, PrintWriter out, boolean jeWS) {
	    Matcher podudaranjeObracun = jeWS ? predlozakObracunWS.matcher(linijaKomande) : predlozakObracun.matcher(linijaKomande);
	    if (!podudaranjeObracun.matches()) {
	        posaljiGresku(out, "ERROR 30 - Format komande nije ispravan\n");
	        return true;
	    }
	    
	    if (!statusPartneri) {
	        posaljiGresku(out, "ERROR 36 - Poslužitelj za partnere u pauzi\n");
	        return true;
	    }

	    int id = Integer.parseInt(podudaranjeObracun.group(1));
	    String sigurnosniKod = podudaranjeObracun.group(2);

	    Partner partner = partneri.get(id);
	    if (partner == null || !partner.sigurnosniKod().equals(sigurnosniKod)) {
	        posaljiGresku(out, "ERROR 31 - Ne postoji partner s id u kolekciji partnera i/ili neispravan sigurnosni kod partnera\n");
	        return true;
	    }

	    try {
	        String jsonString = procitajJSONPodatke(in, out);
	        if (jsonString == null) {
	            return true;
	        }

	        Obracun[] noviObracuni = validirajObracune(jsonString, id, out);
	        if (noviObracuni == null) {
	            return true;
	        }

	        if (!spremiObracune(noviObracuni)) {
	            posaljiGresku(out, "ERROR 35 - Neispravan obračun\n");
	            return true;
	        }

	        if (!jeWS) {
	            try {
	                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
	                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
	                    .uri(java.net.URI.create(this.konfig.dajPostavku("restApiTvrtka") + "/obracun"))
	                    .header("Content-Type", "application/json")
	                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonString))
	                    .build();
	                var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
	                if (response.statusCode() != 200) {
	                    posaljiGresku(out, "ERROR 37 - RESTful zahtjev nije uspješan\n");
	                    return true;
	                }
	            } catch (Exception e) {
	                posaljiGresku(out, "ERROR 37 - RESTful zahtjev nije uspješan\n");
	                return true;
	            }
	        }

	        out.write("OK\n");
	        out.flush();
	        return true;
	    } catch (JsonSyntaxException e) {
	        posaljiGresku(out, "ERROR 35 - Neispravan obračun\n");
	        return true;
	    } catch (Exception e) {
	        posaljiGresku(out, "ERROR 35 - Neispravan obračun\n");
	        return true;
	    }
	}


	/**
	 * Čita JSON podatke iz ulaznog toka.
	 * 
	 * @param in ulazni tok za čitanje podataka
	 * @param out izlazni tok za slanje odgovora u slučaju greške
	 * @return pročitani JSON string ili null u slučaju greške
	 * @throws IOException ako dođe do greške pri čitanju
	 */
	private String procitajJSONPodatke(BufferedReader in, PrintWriter out) throws IOException {
		StringBuilder jsonBuilder = new StringBuilder();
		String red = in.readLine();
		if (red == null || red.isBlank()) {
			posaljiGresku(out, "ERROR 35 - Neispravan obračun\n");
			return null;
		}
		jsonBuilder.append(red);
		int maxIterations = 100;
		int iteration = 0;
		try {
			while (!red.trim().endsWith("]") && iteration < maxIterations) {
				red = in.readLine();
				if (red == null) {
					posaljiGresku(out, "ERROR 35 - Neispravan obračun\n");
					return null;
				}
				jsonBuilder.append("\n").append(red);
				iteration++;
			}
		} catch (SocketTimeoutException e) {
			posaljiGresku(out, "ERROR 35 - Neispravan obračun\n");
			return null;
		}
		if (iteration >= maxIterations) {
			posaljiGresku(out, "ERROR 35 - Neispravan obračun\n");
			return null;
		}
		String jsonString = jsonBuilder.toString();
		if (jsonString.trim().isEmpty() || !jsonString.trim().endsWith("]")) {
			posaljiGresku(out, "ERROR 35 - Neispravan obračun\n");
			return null;
		}
		return jsonString;
	}

	/**
	 * Validira obračune primljene od partnera.
	 * 
	 * @param jsonString JSON string s obračunima
	 * @param partnerId ID partnera
	 * @param out izlazni tok za slanje odgovora u slučaju greške
	 * @return polje validiranih obračuna ili null u slučaju greške
	 */
	private Obracun[] validirajObracune(String jsonString, int partnerId, PrintWriter out) {
		Gson gson = new Gson();
		Obracun[] noviObracuni;

		try {
			noviObracuni = gson.fromJson(jsonString, Obracun[].class);
		} catch (JsonSyntaxException e) {
			posaljiGresku(out, "ERROR 35 - Neispravan obračun\n");
			return null;
		}

		if (noviObracuni == null || noviObracuni.length == 0) {
			posaljiGresku(out, "ERROR 35 - Neispravan obračun\n");
			return null;
		}

		for (Obracun obracun : noviObracuni) {
			if (obracun.partner() != partnerId) {
				posaljiGresku(out, "ERROR 35 - Neispravan obračun\n");
				return null;
			}
		}

		return noviObracuni;
	}

	/**
	 * Sprema obračune u datoteku.
	 * 
	 * @param noviObracuni polje novih obračuna za spremanje
	 * @return true ako su obračuni uspješno spremljeni, false inače
	 */
	private boolean spremiObracune(Obracun[] noviObracuni) {
		final ReentrantLock obracuniLock = new ReentrantLock();
		obracuniLock.lock();
		try {
			String nazivDatoteke = konfig.dajPostavku("datotekaObracuna");
			Path datoteka = Path.of(nazivDatoteke);
			List<Obracun> sviObracuni = new ArrayList<>();

			if (Files.exists(datoteka)) {
				try (var br = Files.newBufferedReader(datoteka)) {
					Gson gson = new Gson();
					Obracun[] postojeci = gson.fromJson(br, Obracun[].class);
					if (postojeci != null) {
						sviObracuni.addAll(Arrays.asList(postojeci));
					}
				} catch (IOException e) {
					return false;
				}
			}

			sviObracuni.addAll(Arrays.asList(noviObracuni));

			try (var bw = Files.newBufferedWriter(datoteka)) {
				Gson gson = new Gson();
				gson.toJson(sviObracuni, bw);
				return true;
			} catch (IOException e) {
				return false;
			}
		} finally {
			obracuniLock.unlock();
		}
	}

	/**
	 * Spremi partnere.
	 */
	private void spremiPartnere() {

		var nazivDatoteke = konfig.dajPostavku("datotekaPartnera");
		try (var bw = Files.newBufferedWriter(Path.of(nazivDatoteke))) {
			Gson gson = new Gson();
			gson.toJson(partneri.values(), bw);
		} catch (IOException e) {
		}
	}

	/**
	 * Učitava jelovnike iz datoteka.
	 * 
	 * @return true ako su jelovnici uspješno učitani, false inače
	 */
	public boolean ucitajJelovnike() {
		for (int i = 1; i <= 9; i++) {
			String kljuc = "kuhinja_" + i;
			if (!this.konfig.dajSvePostavke().containsKey(kljuc)) {
				continue;
			}
			var vrijednost = this.konfig.dajPostavku(kljuc);
			var dijelovi = vrijednost.split(";");
			if (dijelovi.length != 2)
				continue;
			var vrstaKuhinje = dijelovi[0];
			var nazivKuhinje = dijelovi[1];
			this.kuhinje.put(vrstaKuhinje, nazivKuhinje);

			var datoteka = Path.of(kljuc + ".json");
			if (!Files.exists(datoteka) || !Files.isRegularFile(datoteka) || !Files.isReadable(datoteka)) {
				continue;
			}
			try (var br = Files.newBufferedReader(datoteka)) {
				Gson gson = new Gson();
				var jelovnikNiz = gson.fromJson(br, Jelovnik[].class);
				Map<String, Jelovnik> mapaJela = new ConcurrentHashMap<>();
				Stream.of(jelovnikNiz).forEach(j -> mapaJela.put(j.id(), j));
				this.jelovnici.put(vrstaKuhinje, mapaJela);

			} catch (IOException ex) {
				continue;
			}
		}
		return true;
	}

	/**
	 * Obradi kraj.
	 *
	 * @param mreznaUticnica the mrezna uticnica
	 * @return the boolean
	 */
	private void obradiKraj(String linija, PrintWriter out, Socket socket) {
	    try {
	        if (linija.startsWith("KRAJ ")) {
	            Matcher matcher = predlozakKraj.matcher(linija);
	            if (!matcher.matches()) {
	                out.write("ERROR 10 - Format komande nije ispravan ili nije ispravan kod za kraj\n");
	                out.flush();
	                return;
	            }

	            String kod = matcher.group(1);
	            if (!kod.equals(this.kodZaKraj)) {
	                out.write("ERROR 10 - Format komande nije ispravan ili nije ispravan kod za kraj\n");
	                out.flush();
	                return;
	            }


	            boolean sviOK = partneri.values().stream().allMatch(p -> {
	                try (Socket s = new Socket(p.adresa(), p.mreznaVrataKraj());
	                     PrintWriter pw = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "utf8"), true);
	                     BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), "utf8"))) {
	                    pw.println("KRAJ " + kod);
	                    return "OK".equals(in.readLine());
	                } catch (IOException e) {
	                    return false;
	                }
	            });

	            if (!sviOK) {
	                out.write("ERROR 14 - Barem jedan partner nije završio rad\n");
	                out.flush();
	                return;
	            }

	            try {
	                var client = java.net.http.HttpClient.newHttpClient();
	                var request = java.net.http.HttpRequest.newBuilder()
	                        .uri(java.net.URI.create(this.konfig.dajPostavku("restApiTvrtka") + "/kraj/info"))
	                        .method("HEAD", java.net.http.HttpRequest.BodyPublishers.noBody())
	                        .build();
	                var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
	                if (response.statusCode() != 200) {
	                    out.write("ERROR 17 - RESTful zahtjev nije uspješan\n");
	                    out.flush();
	                    return;
	                }
	            } catch (Exception e) {
	                out.write("ERROR 17 - RESTful zahtjev nije uspješan\n");
	                out.flush();
	                return;
	            }

	            this.kraj.set(true);
	            out.write("OK\n");
	            out.flush();
	        } else if (linija.startsWith("STATUS ")) {
	            obradiStatusKomandu(linija, out);
	        } else if (linija.startsWith("PAUZA ")) {
	            obradiPauzaKomandu(linija, out);
	        } else if (linija.startsWith("START ")) {
	            obradiStartKomandu(linija, out);
	        } else if (linija.startsWith("SPAVA ")) {
	            obradiSpavaKomandu(linija, out);
	        } else if (linija.startsWith("KRAJWS ")) {
	            obradiKrajWSKomandu(linija, out);
	        } else if (linija.startsWith("OSVJEŽI ")) {
	            obradiOsvjeziKomandu(linija, out);
	        } else {
	            out.write("ERROR 20 - Nepoznata komanda\n");
	            out.flush();
	        }
	    } catch (Exception e) {
	        out.write("ERROR 19 - Nešto drugo nije u redu\n");
	        out.flush();
	    }
	}

	

	/**
	 * Učitava partnere iz datoteke.
	 * Ako datoteka ne postoji, kreira se prazna datoteka.
	 * 
	 * @return true ako su partneri uspješno učitani, false inače
	 */
	public boolean ucitajPartnere() {
		var nazivDatoteke = this.konfig.dajPostavku("datotekaPartnera");
		var datoteka = Path.of(nazivDatoteke);
		if (!Files.exists(datoteka) || !Files.isRegularFile(datoteka) || !Files.isReadable(datoteka)) {

			try {
				Gson gson = new Gson();
				Partner[] prazniPartneri = new Partner[0];
				String jsonString = gson.toJson(prazniPartneri);
				if (datoteka.getParent() != null) {
					Files.createDirectories(datoteka.getParent());
				}

				Files.writeString(datoteka, jsonString);

				System.out.println("Datoteka partnera '" + nazivDatoteke + "' nije postojala i kreirana je prazna.");
				return true;
			} catch (IOException e) {
				System.err.println("Greška prilikom kreiranja datoteke partnera: " + e.getMessage());
				return false;
			}

		}
		try (var br = Files.newBufferedReader(datoteka)) {
			Gson gson = new Gson();
			var partnerNiz = gson.fromJson(br, Partner[].class);
			var partnerTok = Stream.of(partnerNiz);
			partnerTok.forEach(p -> this.partneri.put(p.id(), p));
		} catch (IOException ex) {
			return false;
		}
		return true;

	}

	/**
	 * Učitava kartu pića iz datoteke.
	 * 
	 * @return true ako je karta pića uspješno učitana, false inače
	 */
	public boolean ucitajKartuPica() {
		var nazivDatoteke = this.konfig.dajPostavku("datotekaKartaPica");
		var datoteka = Path.of(nazivDatoteke);
		if (!Files.exists(datoteka) || !Files.isRegularFile(datoteka) || !Files.isReadable(datoteka)) {
			return false;
		}
		try (var br = Files.newBufferedReader(datoteka)) {
			Gson gson = new Gson();
			var kartaPicaNiz = gson.fromJson(br, KartaPica[].class);

			var kartaPicaTok = Stream.of(kartaPicaNiz);
			kartaPicaTok.forEach(kp -> this.kartaPica.put(kp.id(), kp));
		} catch (IOException ex) {

			return false;
		}
		return true;
	}

	/**
	 * Učitava konfiguraciju iz datoteke.
	 * 
	 * @param nazivDatoteke putanja do konfiguracijske datoteke
	 * @return true ako je konfiguracija uspješno učitana, false inače
	 */
	public boolean ucitajKonfiguraciju(String nazivDatoteke) {
		try {
			this.konfig = KonfiguracijaApstraktna.preuzmiKonfiguraciju(nazivDatoteke);
			return true;
		} catch (NeispravnaKonfiguracija ex) {
			System.out.print("ERROR 19 - neispravna konfiguracija\n");
		}
		return false;
	}
	
	private void obradiStatusKomandu(String linija, PrintWriter out) {
	    String[] dijelovi = linija.trim().split(" ");
	    if (dijelovi.length != 3) {
	        posaljiGresku(out, "ERROR 20 - Format komande nije ispravan\n");
	        return;
	    }
	    String kod = dijelovi[1];
	    String dio = dijelovi[2];

	    if (!this.konfig.dajPostavku("kodZaAdminTvrtke").equals(kod)) {
	        posaljiGresku(out, "ERROR 12 - Pogrešan kodZaAdminTvrtke\n");
	        return;
	    }

	    boolean status = dio.equals("1") ? statusRegistracija : statusPartneri;
	    out.write("OK " + (status ? "1" : "0") + "\n");
	    out.flush();
	}

	private void obradiPauzaKomandu(String linija, PrintWriter out) {
	    String[] dijelovi = linija.trim().split(" ");
	    if (dijelovi.length != 3) {
	        posaljiGresku(out, "ERROR 20 - Format komande nije ispravan\n");
	        return;
	    }
	    String kod = dijelovi[1];
	    String dio = dijelovi[2];

	    if (!this.konfig.dajPostavku("kodZaAdminTvrtke").equals(kod)) {
	        posaljiGresku(out, "ERROR 12 - Pogrešan kodZaAdminTvrtke\n");
	        return;
	    }

	    if (dio.equals("1")) {
	        if (!statusRegistracija) {
	            posaljiGresku(out, "ERROR 13 - Pogrešna promjena pauze\n");
	            return;
	        }
	        statusRegistracija = false;
	    } else if (dio.equals("2")) {
	        if (!statusPartneri) {
	            posaljiGresku(out, "ERROR 13 - Pogrešna promjena pauze\n");
	            return;
	        }
	        statusPartneri = false;
	    }
	    out.write("OK\n");
	    out.flush();
	}

	private void obradiStartKomandu(String linija, PrintWriter out) {
	    String[] dijelovi = linija.trim().split(" ");
	    if (dijelovi.length != 3) {
	        posaljiGresku(out, "ERROR 20 - Format komande nije ispravan\n");
	        return;
	    }
	    String kod = dijelovi[1];
	    String dio = dijelovi[2];

	    if (!this.konfig.dajPostavku("kodZaAdminTvrtke").equals(kod)) {
	        posaljiGresku(out, "ERROR 12 - Pogrešan kodZaAdminTvrtke\n");
	        return;
	    }

	    if (dio.equals("1")) {
	        if (statusRegistracija) {
	            posaljiGresku(out, "ERROR 13 - Pogrešna promjena starta\n");
	            return;
	        }
	        statusRegistracija = true;
	    } else if (dio.equals("2")) {
	        if (statusPartneri) {
	            posaljiGresku(out, "ERROR 13 - Pogrešna promjena starta\n");
	            return;
	        }
	        statusPartneri = true;
	    }
	    out.write("OK\n");
	    out.flush();
	}

	private void obradiSpavaKomandu(String linija, PrintWriter out) {
	    String[] dijelovi = linija.trim().split(" ");
	    if (dijelovi.length != 3) {
	        posaljiGresku(out, "ERROR 20 - Format komande nije ispravan\n");
	        return;
	    }
	    String kod = dijelovi[1];
	    int trajanje = Integer.parseInt(dijelovi[2]);

	    if (!this.konfig.dajPostavku("kodZaAdminTvrtke").equals(kod)) {
	        posaljiGresku(out, "ERROR 12 - Pogrešan kodZaAdminTvrtke\n");
	        return;
	    }

	    try {
	        Thread.sleep(trajanje);
	    } catch (InterruptedException e) {
	        posaljiGresku(out, "ERROR 16 - Prekid spavanja dretve\n");
	        return;
	    }
	    out.write("OK\n");
	    out.flush();
	}
	
	
	
	private void obradiKrajWSKomandu(String linija, PrintWriter out) {
	    String[] dijelovi = linija.trim().split(" ");
	    if (dijelovi.length != 2) {
	        posaljiGresku(out, "ERROR 20 - Format komande nije ispravan\n");
	        return;
	    }

	    String kod = dijelovi[1];
	    if (!this.kodZaKraj.equals(kod)) {
	        posaljiGresku(out, "ERROR 12 - Pogrešan AdminTvrtke\n");
	        return;
	    }

	    boolean sviPartneriZavrsili = true;
	    List<String> neuspjesniPartneri = new ArrayList<>();

	    for (Partner partner : partneri.values()) {
	        if (!zaustaviPartnera(partner, kod)) {
	            sviPartneriZavrsili = false;
	            neuspjesniPartneri.add(partner.id() + " (" + partner.adresa() + ":" + partner.mreznaVrataKraj() + ")");
	        }
	    }

	    if (!sviPartneriZavrsili) {
	        out.write("ERROR 14 - Barem jedan partner nije završio rad");
	        out.flush();
	        return; 
	    }

	    this.kraj.set(true);
	    out.write("OK\n");
	    out.flush();
	}

	private boolean zaustaviPartnera(Partner partner, String kodZaKraj) {
	    var adresa = new InetSocketAddress(partner.adresa(), partner.mreznaVrataKraj());
	    try (Socket socket = new Socket()) {
	        socket.connect(adresa, 2000);
	        socket.setSoTimeout(2000);

	        try (PrintWriter izlazniTok = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "utf8"), true);
	             BufferedReader ulazniTok = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf8"))) {

	            izlazniTok.println("KRAJ " + kodZaKraj);
	            izlazniTok.flush();

	            String odgovor = ulazniTok.readLine();
	            if (odgovor == null || !"OK".equalsIgnoreCase(odgovor.trim())) {
	                return true; 
	            }

	            return true; 
	        }
	    } catch (IOException e) {
	        return true; 
	    }
	}




	private void obradiOsvjeziKomandu(String linija, PrintWriter out) {
	    String[] dijelovi = linija.trim().split(" ");
	    if (dijelovi.length != 2) {
	        posaljiGresku(out, "ERROR 20 - Format komande nije ispravan\n");
	        return;
	    }
	    String kod = dijelovi[1];
	    if (!this.konfig.dajPostavku("kodZaAdminTvrtke").equals(kod)) {
	        posaljiGresku(out, "ERROR 12 - Pogrešan kodZaAdminTvrtke\n");
	        return;
	    }
	    if (!statusPartneri) {
	        posaljiGresku(out, "ERROR 15 - Poslužitelj za partnere u pauzi\n");
	        return;
	    }
	    if (!ucitajJelovnike() || !ucitajKartuPica()) {
	        posaljiGresku(out, "ERROR 17 - RESTful zahtjev nije uspješan\n");
	        return;
	    }
	    out.write("OK\n");
	    out.flush();
	}

	private void obradiObracunWSKomandu(String linija, BufferedReader in, PrintWriter out) {
	    String[] dijelovi = linija.trim().split(" ");
	    if (dijelovi.length != 3) {
	        posaljiGresku(out, "ERROR 20 - Format komande nije ispravan\n");
	        return;
	    }

	    int id = Integer.parseInt(dijelovi[1]);
	    String kod = dijelovi[2];
	    Partner partner = partneri.get(id);
	    if (partner == null || !partner.sigurnosniKod().equals(kod)) {
	        posaljiGresku(out, "ERROR 31 - Ne postoji partner s id i/ili neispravan kod\n");
	        return;
	    }

	    if (!statusPartneri) {
	        posaljiGresku(out, "ERROR 36 - Poslužitelj za partnere u pauzi\n");
	        return;
	    }

	    StringBuilder jsonBuilder = new StringBuilder();
	    String linijaJson;
	    try {
	        
	        while ((linijaJson = in.readLine()) != null) {
	            jsonBuilder.append(linijaJson);
	            if (linijaJson.trim().endsWith("]")) {
	                break;
	            }
	        }

	        String jsonString = jsonBuilder.toString().trim();
	        if (jsonString.isEmpty() || !jsonString.endsWith("]")) {
	            posaljiGresku(out, "ERROR 35 - Neispravan obračun\n");
	            return;
	        }

	 
	        Obracun[] obracuni = new Gson().fromJson(jsonString, Obracun[].class);
	        if (obracuni == null || obracuni.length == 0) {
	            posaljiGresku(out, "ERROR 35 - Neispravan obračun\n");
	            return;
	        }

	  
	        for (Obracun obracun : obracuni) {
	            if (obracun.partner() != id) {
	                posaljiGresku(out, "ERROR 35 - Neispravan obračun\n");
	                return;
	            }
	        }

	  
	        Path putanja = Path.of(konfig.dajPostavku("datotekaObracuna"));
	        List<Obracun> lista = new ArrayList<>();
	        if (Files.exists(putanja)) {
	            String postojece = Files.readString(putanja);
	            Obracun[] postojeci = new Gson().fromJson(postojece, Obracun[].class);
	            if (postojeci != null) {
	                lista.addAll(Arrays.asList(postojeci));
	            }
	        }

	      
	        lista.addAll(Arrays.asList(obracuni));

	      
	        Files.writeString(putanja, new Gson().toJson(lista));

	     
	        out.write("OK\n");
	        out.flush();
	    } catch (IOException | JsonSyntaxException e) {
	        posaljiGresku(out, "ERROR 35 - Neispravan obračun\n");
	    }
	}

	
}
