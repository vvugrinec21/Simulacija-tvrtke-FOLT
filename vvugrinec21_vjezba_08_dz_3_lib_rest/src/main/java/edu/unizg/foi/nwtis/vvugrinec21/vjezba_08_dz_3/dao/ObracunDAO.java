package edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3.dao;

import edu.unizg.foi.nwtis.podaci.Obracun;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

// TODO: Auto-generated Javadoc
/**
 * The Class ObracunDAO.
 */
public class ObracunDAO {
    
    /** The veza BP. */
    private final Connection vezaBP;

    /**
     * Instantiates a new obracun DAO.
     *
     * @param vezaBP the veza BP
     */
    public ObracunDAO(Connection vezaBP) {
        this.vezaBP = vezaBP;
    }

    /**
     * Dohvati sve.
     *
     * @param od the od
     * @param doVrijeme the do vrijeme
     * @param partnerId the partner id
     * @return the list
     */
    public List<Obracun> dohvatiSve(Long od, Long doVrijeme, Integer partnerId) {
        StringBuilder upit = new StringBuilder("SELECT ID, PARTNER, JELO, KOLICINA, CIJENA, VRIJEME FROM obracuni WHERE 1=1");

        if (od != null) upit.append(" AND VRIJEME >= ?");
        if (doVrijeme != null) upit.append(" AND VRIJEME <= ?");
        if (partnerId != null) upit.append(" AND PARTNER = ?");

        upit.append(" ORDER BY VRIJEME DESC");

        List<Obracun> lista = new ArrayList<>();
        try (PreparedStatement s = vezaBP.prepareStatement(upit.toString())) {
            int indeks = 1;
            if (od != null) s.setTimestamp(indeks++, new Timestamp(od));
            if (doVrijeme != null) s.setTimestamp(indeks++, new Timestamp(doVrijeme));
            
            if (partnerId != null) s.setInt(indeks++, partnerId);

            ResultSet rs = s.executeQuery();
            while (rs.next()) {
                Obracun o = new Obracun(
                    rs.getInt("PARTNER"),
                    rs.getString("ID"), 
                    rs.getBoolean("JELO"),
                    rs.getFloat("KOLICINA"),
                    rs.getFloat("CIJENA"),
                    rs.getTimestamp("VRIJEME").getTime() 
                );
                lista.add(o);
            }
        } catch (SQLException ex) {
            Logger.getLogger(ObracunDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
        return lista;
    }

    /**
     * Dohvati sve jelo.
     *
     * @param od the od
     * @param doVrijeme the do vrijeme
     * @return the list
     */
    public List<Obracun> dohvatiSveJelo(Long od, Long doVrijeme) {
        StringBuilder upit = new StringBuilder("SELECT * FROM obracuni WHERE JELO = true");
        if (od != null) upit.append(" AND VRIJEME >= ?");
        if (doVrijeme != null) upit.append(" AND VRIJEME <= ?");
        upit.append(" ORDER BY VRIJEME DESC");

        List<Obracun> lista = new ArrayList<>();
        try (PreparedStatement s = vezaBP.prepareStatement(upit.toString())) {
            int i = 1;
            if (od != null) s.setTimestamp(i++, new Timestamp(od));
            if (doVrijeme != null) s.setTimestamp(i++, new Timestamp(doVrijeme));

            ResultSet rs = s.executeQuery();
            while (rs.next()) {
                lista.add(new Obracun(
                    rs.getInt("PARTNER"),
                    rs.getString("ID"),
                    rs.getBoolean("JELO"),
                    rs.getFloat("KOLICINA"),
                    rs.getFloat("CIJENA"),
                    rs.getTimestamp("VRIJEME").getTime()
                ));
            }
        } catch (SQLException ex) {
            Logger.getLogger(ObracunDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
        return lista;
    }


    /**
     * Dohvati sve pice.
     *
     * @param od the od
     * @param doVrijeme the do vrijeme
     * @return the list
     */
    public List<Obracun> dohvatiSvePice(Long od, Long doVrijeme) {
        return dohvatiSve(od, doVrijeme, null).stream()
                .filter(o -> !o.jelo()).toList();
    }


    /**
     * Dohvati sve partner.
     *
     * @param partnerId the partner id
     * @param od the od
     * @param doVrijeme the do vrijeme
     * @return the list
     */
    public List<Obracun> dohvatiSvePartner(int partnerId, Long od, Long doVrijeme) {
        return dohvatiSve(od, doVrijeme, partnerId);
    }

    /**
     * Dodaj.
     *
     * @param o the o
     * @return true, if successful
     */
    public boolean dodaj(Obracun o) {
        String upit = "INSERT INTO obracuni (partner, id, jelo, kolicina, cijena, vrijeme) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement s = vezaBP.prepareStatement(upit)) {
            s.setInt(1, o.partner());
            s.setString(2, o.id());
            s.setBoolean(3, o.jelo());
            s.setFloat(4, o.kolicina());
            s.setFloat(5, o.cijena());
            s.setTimestamp(6, new java.sql.Timestamp(o.vrijeme())); 
            return s.executeUpdate() == 1;
        } catch (SQLException ex) {
            Logger.getLogger(ObracunDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }
    
    /**
     * Dohvati sigurnosni kod.
     *
     * @param idPartnera the id partnera
     * @return the string
     */
    public String dohvatiSigurnosniKod(int idPartnera) {
        String upit = """
              SELECT p.sigurnosniKod
              FROM partneri p
              WHERE p.id = ?
            """;

        try (PreparedStatement stmt = this.vezaBP.prepareStatement(upit)) {
            stmt.setInt(1, idPartnera);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("sigurnosniKod");
            }
        } catch (SQLException e) {
            Logger.getLogger(ObracunDAO.class.getName()).log(Level.SEVERE, null, e);
        }
        return "";
    }
    
   
}
