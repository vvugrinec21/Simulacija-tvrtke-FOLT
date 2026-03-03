package edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;

import edu.unizg.foi.nwtis.konfiguracije.Konfiguracija;
import edu.unizg.foi.nwtis.konfiguracije.KonfiguracijaApstraktna;
import edu.unizg.foi.nwtis.konfiguracije.NeispravnaKonfiguracija;
import edu.unizg.foi.nwtis.podaci.Jelovnik;
import edu.unizg.foi.nwtis.podaci.KartaPica;
import edu.unizg.foi.nwtis.podaci.Narudzba;
import edu.unizg.foi.nwtis.podaci.Obracun;

// TODO: Auto-generated Javadoc
/**
 * Poslužitelj koji implementira funkcionalnost partnera u sustavu za
 * naručivanje hrane i pića. Omogućuje registraciju partnera, preuzimanje
 * jelovnika i karte pića, te obradu narudžbi.
 */
public class PosluziteljPartner {

	/** The konfig. */
	private Konfiguracija konfig;

	/** The gson. */
	private final Gson gson = new Gson();

	/** The jelovnik. */
	private final Map<String, Jelovnik> jelovnik = new ConcurrentHashMap<>();

	/** The pica. */
	private final Map<String, KartaPica> pica = new ConcurrentHashMap<>();

	/** The narudzbe otvorene. */
	private final Map<String, Queue<Narudzba>> narudzbeOtvorene = new ConcurrentHashMap<>();

	/** The narudzbe placene. */
	private final List<Narudzba> narudzbePlacene = new ArrayList<>();

	/** The brojac narudzbi. */
	private int brojacNarudzbi = 0;

	/** The executor. */
	private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

	/** The pauza dretve. */
	private int pauzaDretve;

	/** The kvota narudzbi. */
	private int kvotaNarudzbi;

	/** The regex jelovnik. */
	private final Pattern regexJelovnik = Pattern.compile("^JELOVNIK\\s+(\\w+)$");

	/** The regex kartapica. */
	private final Pattern regexKartapica = Pattern.compile("^KARTAPIĆA\\s+(\\w+)$");

	/** The regex narudzba. */
	private final Pattern regexNarudzba = Pattern.compile("^NARUDŽBA\\s+(\\w+)$");

	/** The regex jelo. */
	private final Pattern regexJelo = Pattern.compile("^JELO\\s+(\\w+)\\s+(\\w+)\\s+([\\d.]+)$");

	/** The regex pice. */
	private final Pattern regexPice = Pattern.compile("^PIĆE\\s+(\\w+)\\s+(\\w+)\\s+([\\d.]+)$");

	/** The regex racun. */
	private final Pattern regexRacun = Pattern.compile("^RAČUN\\s+(\\w+)$");

	/** The partner lock. */
	ReentrantLock partnerLock = new ReentrantLock();
	
	private volatile boolean pauza = false;

	/** The aktivno. */
	private volatile boolean aktivno = true;

	/**
	 * Glavna metoda koja pokreće poslužitelj partnera. Ovisno o argumentima,
	 * izvršava različite operacije:
	 * 
	 * S jednim argumentom (putanja do konfiguracijske datoteke) - registrira
	 * partnera S dva argumenta (putanja i "KRAJ") - šalje zahtjev za kraj rada
	 * glavnom poslužitelju S dva argumenta (putanja i "PARTNER") - preuzima
	 * jelovnik i kartu pića, te pokreće poslužitelj
	 *
	 * 
	 * @param args Argumenti komandne linije (putanja do konfiguracijske datoteke i
	 *             opcionalno "KRAJ" ili "PARTNER")
	 */
	public static void main(String[] args) {
		var program = new PosluziteljPartner();

		if (args.length < 1 || args.length > 2) {
			System.out.println("ERROR 40 - Format komande nije ispravan");
			return;
		}

		if (!program.ucitajKonfiguraciju(args[0])) {
			System.out.println("ERROR 49 - Nešto drugo nije u redu");
			return;
		}

		if (args.length == 1) {

			program.registrirajPartnera();
		} else if (args.length == 2 && "KRAJ".equals(args[1])) {

			program.posaljiZahtjevZaKraj();
		} else if (args.length == 2 && "PARTNER".equals(args[1])) {

			if (!program.preuzmiJelovnik()) {
				System.out.println("ERROR 46 - Neuspješno preuzimanje jelovnika");

				return;
			}
			if (!program.preuzmiKartuPica()) {
				System.out.println("ERROR 47 - Neuspješno preuzimanje karte pića");
				return;
			}
			new Thread(program::pokreniPosluziteljKrajPartnera).start();
			program.pokreniPosluzitelj();
		} else {
			System.out.println("ERROR 40 - Format komande nije ispravan");
		}
	}

	/**
	 * Učitaj konfiguraciju.
	 * @param nazivDatoteke Putanja do konfiguracijske datoteke
	 * @return true ako je konfiguracija uspješno učitana, false inače
	 */
	private boolean ucitajKonfiguraciju(String nazivDatoteke) {
		try {

			konfig = KonfiguracijaApstraktna.preuzmiKonfiguraciju(nazivDatoteke);
			pauzaDretve = Integer.parseInt(konfig.dajPostavku("pauzaDretve"));
			kvotaNarudzbi = Integer.parseInt(konfig.dajPostavku("kvotaNarudzbi"));
			return true;
		} catch (NeispravnaKonfiguracija | NumberFormatException e) {
			return false;
		}
	}

	/**
	 * Šalje zahtjev za kraj rada glavnom poslužitelju.
	 * Koristi kod za kraj definiran u konfiguracijskoj datoteci.
	 */
	private void posaljiZahtjevZaKraj() {
		
		
		try (Socket s = new Socket(konfig.dajPostavku("adresa"),
				Integer.parseInt(konfig.dajPostavku("mreznaVrataKraj")));
				PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "utf8"));
				BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), "utf8"))) {

			out.write("KRAJ " + konfig.dajPostavku("kodZaKraj") + "\n");

			out.flush();
			s.shutdownOutput();

			String odgovor = in.readLine();
			if ("OK".equals(odgovor)) {
				System.out.println("Uspješan kraj poslužitelja.");
			}
		} catch (Exception e) {
			System.out.println("ERROR 49 - Nešto drugo nije u redu");
		}
	}

	/**
	 * Registrira partnera kod glavnog poslužitelja.
	 * Šalje podatke o partneru i sprema dobiveni sigurnosni kod u konfiguracijsku datoteku.
	 */
	private void registrirajPartnera() {
		try (Socket s = new Socket(konfig.dajPostavku("adresa"),
				Integer.parseInt(konfig.dajPostavku("mreznaVrataRegistracija")));
				PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "utf8"));
				BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), "utf8"))) {

			String komanda = String.format("PARTNER %s \"%s\" %s %s %s %s %s", konfig.dajPostavku("id").trim(),
					konfig.dajPostavku("naziv").trim(), konfig.dajPostavku("kuhinja").trim(),
					konfig.dajPostavku("adresa").trim(), konfig.dajPostavku("mreznaVrata").trim(),
					konfig.dajPostavku("gpsSirina").trim(), konfig.dajPostavku("gpsDuzina").trim());

			out.write(komanda + "\n");
			out.flush();

			String odgovor = in.readLine();
			if (odgovor != null && odgovor.startsWith("OK")) {

				String sigKod = odgovor.substring(3);

				konfig.spremiPostavku("sigKod", sigKod);
				konfig.spremiKonfiguraciju();
			}
			else {
				System.out.println(odgovor);
			}
		} catch (Exception e) {
			System.out.println("ERROR 49 - Nešto drugo nije u redu");
		}
	}

	/**
	 * Preuzima jelovnik od glavnog poslužitelja.
	 * @return true ako je jelovnik uspješno preuzet, false inače
	 */
	private boolean preuzmiJelovnik() {
		try (Socket s = new Socket(konfig.dajPostavku("adresa"),
				Integer.parseInt(konfig.dajPostavku("mreznaVrataRad")));
				PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "utf8"));
				BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), "utf8"))) {

			out.write("JELOVNIK " + konfig.dajPostavku("id") + " " + konfig.dajPostavku("sigKod") + "\n");
			out.flush();
			s.shutdownOutput();
			if (!"OK".equals(in.readLine()))
				return false;
			Jelovnik[] niz = gson.fromJson(in.readLine(), Jelovnik[].class);
			Arrays.stream(niz).forEach(j -> jelovnik.put(j.id(), j));
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Preuzima kartu pića od glavnog poslužitelja.
	 * @return true ako je karta pića uspješno preuzeta, false inače
	 */
	private boolean preuzmiKartuPica() {
		try (Socket s = new Socket(konfig.dajPostavku("adresa"),
				Integer.parseInt(konfig.dajPostavku("mreznaVrataRad")));
				PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "utf8"));
				BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), "utf8"))) {

			out.write("KARTAPIĆA " + konfig.dajPostavku("id") + " " + konfig.dajPostavku("sigKod") + "\n");
			out.flush();
			s.shutdownOutput();
			if (!"OK".equals(in.readLine()))
				return false;
			KartaPica[] niz = gson.fromJson(in.readLine(), KartaPica[].class);
			Arrays.stream(niz).forEach(p -> pica.put(p.id(), p));
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Pokreće poslužitelj koji osluškuje zahtjeve klijenata.
	 * Za svaki zahtjev stvara novu virtualnu dretvu koja obrađuje zahtjev.
	 */
	private void pokreniPosluzitelj() {
	    int port = Integer.parseInt(konfig.dajPostavku("mreznaVrata"));
	    try (ServerSocket ss = new ServerSocket(port)) {
	        System.out.println("Poslužitelj pokrenut na portu: " + port);

	        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
	            aktivno = false;
	            zatvoriExecutor();
	            try { ss.close(); } catch (IOException e) {
	                System.out.println("Greška pri zatvaranju server socket-a: " + e.getMessage());
	            }
	            System.out.println("Poslužitelj se gasi...");
	        }));

	        ss.setSoTimeout(1000); 

	        while (aktivno) {
	            try {
	                Socket s = ss.accept();
	                if (!executor.isShutdown()) {
	                    executor.submit(() -> obradiZahtjev(s));
	                } else {
	                    s.close();
	                }
	            } catch (java.net.SocketTimeoutException e) {
	               
	            } catch (IOException e) {
	                if (aktivno) {
	                    System.out.println("Greška pri prihvaćanju veze: " + e.getMessage());
	                }
	            }
	        }
	    } catch (Exception e) {
	        System.out.println("ERROR 49 - Nešto drugo nije u redu");
	        e.printStackTrace();
	    } finally {
	        zatvoriExecutor();
	    }
	}

	
	/**
	 * Zatvori executor.
	 */
	private void zatvoriExecutor() {
	    executor.shutdown();
	    try {
	        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
	            executor.shutdownNow();
	        }
	    } catch (InterruptedException e) {
	        executor.shutdownNow();
	    }
	}
	
	/**
	 * Obrađuje zahtjev klijenta.
	 * Čita zahtjev, određuje vrstu zahtjeva i poziva odgovarajuću metodu za obradu.
	 * 
	 * @param socket Socket preko kojeg je klijent povezan
	 */
	private void obradiZahtjev(Socket socket) {
		try (Socket s = socket;
				BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "utf8"));
				PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "utf8"))) {

			String linija = in.readLine();
			if (linija == null) {
				posaljiGresku(out, "ERROR 40 - Format komande nije ispravan\n");
				return;
			}

			if (obradiJelovnik(linija, out))
				return;
			if (obradiKartaPica(linija, out))
				return;
			if (obradiNarudzbu(linija, out))
				return;
			if (obradiJeloIliPice(linija, out))
				return;
			if (obradiRacun(linija, out))
				return;
			
			if (obradiStanjeKomandu(linija, out))
			    return;
			posaljiGresku(out, "ERROR 40 - Format komande nije ispravan\n");

		} catch (Exception e) {
	        posaljiGlobalniError(socket);
	        try {
	            socket.close();
	        } catch (IOException ignored) {}
	    }
	}

	/**
	 * Obrađuje zahtjev za jelovnik.
	 * Provjerava format zahtjeva i šalje jelovnik klijentu.
	 * 
	 * @param linija Tekst zahtjeva
	 * @param out PrintWriter za slanje odgovora
	 * @return true ako je zahtjev obrađen, false ako format zahtjeva nije odgovarajući
	 */
	private boolean obradiJelovnik(String linija, PrintWriter out) {
		Matcher m = regexJelovnik.matcher(linija);
		if (!m.matches())
			return false;

		out.write("OK\n");
		out.write(gson.toJson(jelovnik.values()) + "\n");
		out.flush();
		return true;
	}

	/**
	 * Obrađuje zahtjev za kartu pića.
	 * Provjerava format zahtjeva i šalje kartu pića klijentu.
	 * 
	 * @param linija Tekst zahtjeva
	 * @param out PrintWriter za slanje odgovora
	 * @return true ako je zahtjev obrađen, false ako format zahtjeva nije odgovarajući
	 */
	private boolean obradiKartaPica(String linija, PrintWriter out) {
		Matcher m = regexKartapica.matcher(linija);
		if (!m.matches())
			return false;

		out.write("OK\n");
		out.write(gson.toJson(pica.values()) + "\n");
		out.flush();
		return true;
	}

	/**
	 * Obrađuje zahtjev za kreiranje nove narudžbe.
	 * Provjerava format zahtjeva i kreira novu narudžbu za kupca.
	 * 
	 * @param linija Tekst zahtjeva
	 * @param out PrintWriter za slanje odgovora
	 * @return true ako je zahtjev obrađen, false ako format zahtjeva nije odgovarajući
	 */
	private boolean obradiNarudzbu(String linija, PrintWriter out) {
		Matcher m = regexNarudzba.matcher(linija);
		if (!m.matches())
			return false;

		String kupac = m.group(1);
		partnerLock.lock();
		try {
			if (narudzbeOtvorene.containsKey(kupac)) {
				posaljiGresku(out, "ERROR 44 - Već postoji otvorena narudžba za korisnika/kupca\n");
			} else {
				narudzbeOtvorene.put(kupac, new ConcurrentLinkedQueue<>());
				out.write("OK\n");
				out.flush();
			}
		} finally {
			partnerLock.unlock();
		}
		return true;
	}

	/**
	 * Obrađuje zahtjev za dodavanje jela ili pića u narudžbu.
	 * Provjerava format zahtjeva, postojanje narudžbe i dostupnost jela/pića.
	 * 
	 * @param linija Tekst zahtjeva
	 * @param out PrintWriter za slanje odgovora
	 * @return true ako je zahtjev obrađen, false ako format zahtjeva nije odgovarajući
	 */
	private boolean obradiJeloIliPice(String linija, PrintWriter out) {
		Matcher mJelo = regexJelo.matcher(linija);
		Matcher mPice = regexPice.matcher(linija);
		boolean jeJelo = mJelo.matches();
		boolean jePice = mPice.matches();

		if (!jeJelo && !jePice)
			return false;

		Matcher m = jeJelo ? mJelo : mPice;
		String kupac = m.group(1);
		String id = m.group(2);
		float kolicina = Float.parseFloat(m.group(3));
		jeJelo = linija.startsWith("JELO");

		partnerLock.lock();
		try {
			if (!narudzbeOtvorene.containsKey(kupac)) {
				posaljiGresku(out, "ERROR 43 - Ne postoji otvorena narudžba za korisnika/kupca\n");
				return true;
			}

			if ((jeJelo && !jelovnik.containsKey(id)) || (!jeJelo && !pica.containsKey(id))) {
				posaljiGresku(out, jeJelo ? "ERROR 41 - Ne postoji jelo s id u kolekciji jelovnika kod partnera\n" : 
					"ERROR 42 - Ne postoji piće s id u kolekciji karte pića kod partnera\n");
				return true;
			}

			Narudzba narudzba = new Narudzba(kupac, id, jeJelo, kolicina,
					(float) (jeJelo ? jelovnik.get(id).cijena() : pica.get(id).cijena()),
					System.currentTimeMillis() / 1000);

			narudzbeOtvorene.get(kupac).add(narudzba);
			out.write("OK\n");
			out.flush();
		} finally {
			partnerLock.unlock();
		}
		return true;
	}

	/**
	 * Obrađuje zahtjev za izdavanje računa.
	 * Provjerava format zahtjeva, postojanje narudžbe i zatvara narudžbu.
	 * Ako je dostignut broj narudžbi za obračun, šalje obračun glavnom poslužitelju.
	 * 
	 * @param linija Tekst zahtjeva
	 * @param out PrintWriter za slanje odgovora
	 * @return true ako je zahtjev obrađen, false ako format zahtjeva nije odgovarajući
	 */
	private boolean obradiRacun(String linija, PrintWriter out) {
		Matcher m = regexRacun.matcher(linija);
		if (!m.matches())
			return false;

		String kupac = m.group(1);

		partnerLock.lock();
		try {
			if (!narudzbeOtvorene.containsKey(kupac)) {
				posaljiGresku(out, "ERROR 43 - Ne postoji otvorena narudžba za korisnika/kupca\n");
				return true;
			}

			Queue<Narudzba> otvorene = narudzbeOtvorene.get(kupac);
			while (!otvorene.isEmpty()) {
				narudzbePlacene.add(otvorene.poll());
			}
			narudzbeOtvorene.remove(kupac);
			brojacNarudzbi++;

			if (brojacNarudzbi % kvotaNarudzbi == 0) {
				if (posaljiObracun()) {
					narudzbePlacene.clear();
				} else {
					posaljiGresku(out, "ERROR 45 - Neuspješno slanje obračuna\n");
				}
			} else {
				out.write("OK\n");
			}
			out.flush();
		} finally {
			partnerLock.unlock();
		}
		return true;
	}

	/**
	 * Šalje poruku o grešci klijentu.
	 * 
	 * @param out PrintWriter za slanje odgovora
	 * @param errorMessage Poruka o grešci koja se šalje
	 */
	private void posaljiGresku(PrintWriter out, String errorMessage) {
		out.write(errorMessage);
		out.flush();
	}

	/**
	 * Šalje globalnu poruku o grešci klijentu.
	 * Koristi se kada dođe do neočekivane iznimke tijekom obrade zahtjeva.
	 * 
	 * @param socket Socket preko kojeg je klijent povezan
	 */
	private void posaljiGlobalniError(Socket socket) {
		try (PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "utf8"))) {
			out.write("ERROR 49 - Nešto drugo nije u redu\n");
			out.flush();
		} catch (IOException ignored) {
		}
	}

	/**
	 * Šalje obračun narudžbi glavnom poslužitelju.
	 * Agregira narudžbe po identifikatoru jela/pića i šalje ukupne količine.
	 * 
	 * @return true ako je obračun uspješno poslan, false inače
	 */
	private boolean posaljiObracun() {
		Map<String, Obracun> agregacija = new ConcurrentHashMap<>();

		for (Narudzba n : narudzbePlacene) {
			String kljuc = n.id();
			Obracun o = agregacija.getOrDefault(kljuc, new Obracun(Integer.parseInt(konfig.dajPostavku("id")), n.id(),
					n.jelo(), 0, n.cijena(), System.currentTimeMillis() / 1000));

			o = new Obracun(o.partner(), o.id(), o.jelo(), o.kolicina() + n.kolicina(), o.cijena(), o.vrijeme());

			agregacija.put(kljuc, o);
		}

		try (Socket s = new Socket(konfig.dajPostavku("adresa"),
				Integer.parseInt(konfig.dajPostavku("mreznaVrataRad")));
				PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "utf8"));
				BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), "utf8"))) {

			out.write("OBRAČUN " + konfig.dajPostavku("id") + " " + konfig.dajPostavku("sigKod") + "\n");
			out.write(gson.toJson(agregacija.values()) + "\n");
			out.flush();

			return "OK".equals(in.readLine());
		} catch (Exception e) {
			return false;
		}
	}
	
	private void pokreniPosluziteljKrajPartnera() {
	    int port = Integer.parseInt(konfig.dajPostavku("mreznaVrataKrajPartner"));
	    try (ServerSocket ss = new ServerSocket(port)) {
	        System.out.println("Poslužitelj KRAJ PARTNER pokrenut na portu: " + port);

	        ss.setSoTimeout(1000); 

	        while (aktivno) {
	            try {
	                Socket socket = ss.accept();
	                if (!executor.isShutdown()) {
	                    executor.submit(() -> obradiZahtjevKrajPartnera(socket));
	                } else {
	                    socket.close();
	                }
	            } catch (java.net.SocketTimeoutException e) {
	                
	            } catch (IOException e) {
	                if (aktivno) {
	                    System.out.println("Greška u KRAJ PARTNER: " + e.getMessage());
	                }
	            }
	        }
	    } catch (IOException e) {
	        System.out.println("Greška u pokretanju poslužitelja KRAJ PARTNER.");
	        e.printStackTrace();
	    } finally {
	        zatvoriExecutor();
	    }
	}
	
	private void obradiZahtjevKrajPartnera(Socket socket) {
	    try (Socket s = socket; 
	         BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), "utf8"));
	         PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "utf8"))) {

	        String linija = in.readLine();
	        if (linija == null) {
	            posaljiGresku(out, "ERROR 60 - Format komande nije ispravan ili nije ispravan kod za kraj\n");
	            return;
	        }

	        
	        System.out.println("Zaprimljena komanda (KRAJ_PARTNER): " + linija);

	        if (linija.startsWith("KRAJ ")) {
	            obradiKrajKomandu(linija, out);
	        } else if (linija.startsWith("OSVJEŽI ")) {
	            obradiOsvjeziKomandu(linija, out);
	        } else if (linija.startsWith("STATUS ")) {
	            obradiStatusKomandu(linija, out);
	        } else if (linija.startsWith("PAUZA ")) {
	            obradiPauzaKomandu(linija, out);
	        } else if (linija.startsWith("START ")) {
	            obradiStartKomandu(linija, out);
	        } else if (linija.startsWith("SPAVA ")) {
	            obradiSpavaKomandu(linija, out);
	        } else {
	            posaljiGresku(out, "ERROR 60 - Format komande nije ispravan ili nije ispravan kod za kraj\n");
	        }

	    } catch (IOException e) {
	        System.out.println("Greška u obradi zahtjeva za kraj partnera: " + e.getMessage());
	    }
	}
	
	private void obradiKrajKomandu(String linija, PrintWriter out) {
	    String[] dijelovi = linija.trim().split(" ");
	    if (dijelovi.length != 2 || !dijelovi[1].equals(konfig.dajPostavku("kodZaKraj"))) {
	        posaljiGresku(out, "ERROR 60 - Format komande nije ispravan ili nije ispravan kod za kraj\n");
	        return;
	    }
	    aktivno = false;
	    out.write("OK\n");
	    out.flush();

	    System.out.println("PosluziteljPartner primio KRAJ i gasi se.");
	    zatvoriExecutor();
	    System.exit(0);
	}
	
	private void obradiOsvjeziKomandu(String linija, PrintWriter out) {
	    String[] dijelovi = linija.trim().split(" ");
	    if (dijelovi.length != 2 || !dijelovi[1].equals(konfig.dajPostavku("kodZaAdmin"))) {
	        posaljiGresku(out, "ERROR 61 - Pogrešan kodZaAdminPartnera\n");
	        return;
	    }
	    if (!aktivno) {
	        posaljiGresku(out, "ERROR 48 - Poslužitelj za prijem zahtjeva kupaca u pauzi\n");
	        return;
	    }
	    if (!preuzmiJelovnik() || !preuzmiKartuPica()) {
	        posaljiGresku(out, "ERROR 69 - Nešto drugo nije u redu\n");
	        return;
	    }
	    if (pauza) {
	        posaljiGresku(out, "ERROR 48 - Poslužitelj za prijem zahtjeva kupaca u pauzi\n");
	        return;
	    }
	    out.write("OK\n");
	    out.flush();
	}
	
	private void obradiStatusKomandu(String linija, PrintWriter out) {
	    String[] dijelovi = linija.trim().split(" ");
	    if (dijelovi.length != 3 || !dijelovi[1].equals(konfig.dajPostavku("kodZaAdmin")) || !"1".equals(dijelovi[2])) {
	        posaljiGresku(out, "ERROR 61 - Pogrešan kodZaAdminPartnera\n");
	        return;
	    }
	    out.write("OK " + (pauza ? "0" : "1") + "\n");
	    out.flush();
	}
	
	private void obradiPauzaKomandu(String linija, PrintWriter out) {
	    String[] dijelovi = linija.trim().split(" ");
	    if (dijelovi.length != 3 || !dijelovi[1].equals(konfig.dajPostavku("kodZaAdmin")) || !"1".equals(dijelovi[2])) {
	        posaljiGresku(out, "ERROR 61 - Pogrešan kodZaAdminPartnera\n");
	        return;
	    }
	    if (pauza) {
	        posaljiGresku(out, "ERROR 62 - Pogrešna promjena pauze\n");
	        return;
	    }
	    pauza = true;
	    out.write("OK\n");
	    out.flush();
	}

	
	private void obradiStartKomandu(String linija, PrintWriter out) {
	    String[] dijelovi = linija.trim().split(" ");
	    if (dijelovi.length != 3 || !dijelovi[1].equals(konfig.dajPostavku("kodZaAdmin")) || !"1".equals(dijelovi[2])) {
	        posaljiGresku(out, "ERROR 61 - Pogrešan kodZaAdminPartnera\n");
	        return;
	    }
	    if (!pauza) {
	        posaljiGresku(out, "ERROR 62 - Pogrešna promjena starta\n");
	        return;
	    }
	    pauza = false;
	    out.write("OK\n");
	    out.flush();
	}

	
	private void obradiSpavaKomandu(String linija, PrintWriter out) {
	    String[] dijelovi = linija.trim().split(" ");
	    if (dijelovi.length != 3 || !dijelovi[1].equals(konfig.dajPostavku("kodZaAdmin"))) {
	        posaljiGresku(out, "ERROR 61 - Pogrešan kodZaAdminPartnera\n");
	        return;
	    }
	    try {
	        Thread.sleep(Integer.parseInt(dijelovi[2]));
	        out.write("OK\n");
	        out.flush();
	    } catch (InterruptedException e) {
	        posaljiGresku(out, "ERROR 63 - Prekid spavanja dretve\n");
	    }
	}

	private boolean obradiStanjeKomandu(String linija, PrintWriter out) {
	    String[] dijelovi = linija.trim().split(" ");
	    if (dijelovi.length != 2) {
	        posaljiGresku(out, "ERROR 60 - Format komande nije ispravan ili nije ispravan kod za kraj\n");
	        return true;
	    }

	    if (!aktivno) {
	        posaljiGresku(out, "ERROR 48 - Poslužitelj za prijem zahtjeva kupaca u pauzi\n");
	        return true;
	    }

	    String korisnik = dijelovi[1];
	    Queue<Narudzba> narudzbe = narudzbeOtvorene.get(korisnik);
	    if (narudzbe == null || narudzbe.isEmpty()) {
	        posaljiGresku(out, "ERROR 43 - Ne postoji otvorena narudžba za korisnika/kupca\n");
	        return true;
	    }

	    out.write("OK\n");
	    out.write(new Gson().toJson(narudzbe) + "\n");
	    out.flush();
	    return true;
	}

}
