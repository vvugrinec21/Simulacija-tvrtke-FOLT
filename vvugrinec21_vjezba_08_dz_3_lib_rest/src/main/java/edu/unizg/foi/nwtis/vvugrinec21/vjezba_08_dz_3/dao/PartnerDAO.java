package edu.unizg.foi.nwtis.vvugrinec21.vjezba_08_dz_3.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import edu.unizg.foi.nwtis.podaci.Partner;

// TODO: Auto-generated Javadoc
/**
 * The Class PartnerDAO.
 */
public class PartnerDAO {
    
    /** The veza BP. */
    private Connection vezaBP;

    /**
     * Instantiates a new partner DAO.
     *
     * @param vezaBP the veza BP
     */
    public PartnerDAO(Connection vezaBP) {
        super();
        this.vezaBP = vezaBP;
    }

    /**
     * Dohvati.
     *
     * @param id the id
     * @param sakriKodove the sakri kodove
     * @return the partner
     */
    public Partner dohvati(int id, boolean sakriKodove) {
        String upit = "SELECT naziv, vrstaKuhinje, adresa, mreznaVrata, mreznaVrataKraj, gpsSirina, gpsDuzina, sigurnosniKod, adminKod FROM partneri WHERE id = ?";

        try (PreparedStatement s = this.vezaBP.prepareStatement(upit)) {
            s.setInt(1, id);
            ResultSet rs = s.executeQuery();
            if (rs.next()) {
                String naziv = rs.getString("naziv");
                String vrstaKuhinje = rs.getString("vrstaKuhinje");
                String adresa = rs.getString("adresa");
                int mreznaVrata = rs.getInt("mreznaVrata");
                int mreznaVrataKraj = rs.getInt("mreznaVrataKraj");
                float gpsSirina = rs.getFloat("gpsSirina");
                float gpsDuzina = rs.getFloat("gpsDuzina");
                String sigurnosniKod = rs.getString("sigurnosniKod");
                String adminKod = rs.getString("adminKod");

                Partner p = new Partner(id, naziv, vrstaKuhinje, adresa, mreznaVrata, mreznaVrataKraj, gpsSirina, gpsDuzina, sigurnosniKod, adminKod);
                return sakriKodove ? p.partnerBezKodova() : p;
            }
        } catch (SQLException ex) {
            Logger.getLogger(PartnerDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * Dohvati sve.
     *
     * @param sakriKodove the sakri kodove
     * @return the list
     */
    public List<Partner> dohvatiSve(boolean sakriKodove) {
        String upit = "SELECT id, naziv, vrstaKuhinje, adresa, mreznaVrata, mreznaVrataKraj, gpsSirina, gpsDuzina, sigurnosniKod, adminKod FROM partneri ORDER BY id";
        List<Partner> partneri = new ArrayList<>();

        try (Statement s = this.vezaBP.createStatement(); ResultSet rs = s.executeQuery(upit)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String naziv = rs.getString("naziv");
                String vrstaKuhinje = rs.getString("vrstaKuhinje");
                String adresa = rs.getString("adresa");
                int mreznaVrata = rs.getInt("mreznaVrata");
                int mreznaVrataKraj = rs.getInt("mreznaVrataKraj");
                float gpsSirina = rs.getFloat("gpsSirina");
                float gpsDuzina = rs.getFloat("gpsDuzina");
                String sigurnosniKod = rs.getString("sigurnosniKod");
                String adminKod = rs.getString("adminKod");

                Partner p = new Partner(id, naziv, vrstaKuhinje, adresa, mreznaVrata, mreznaVrataKraj, gpsSirina, gpsDuzina, sigurnosniKod, adminKod);
                partneri.add(sakriKodove ? p.partnerBezKodova() : p);
            }
        } catch (SQLException ex) {
            Logger.getLogger(PartnerDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
        return partneri;
    }

    /**
     * Azuriraj.
     *
     * @param p the p
     * @return true, if successful
     */
    public boolean azuriraj(Partner p) {
        String upit = "UPDATE partneri SET naziv = ?, adresa = ?, mreznaVrata = ?, mreznaVrataKraj = ?, gpsSirina = ?, gpsDuzina = ?, sigurnosniKod = ?, adminKod = ? WHERE id = ?";
        try (PreparedStatement s = this.vezaBP.prepareStatement(upit)) {
            s.setString(1, p.naziv());
            s.setString(2, p.adresa());
            s.setInt(3, p.mreznaVrata());
            s.setInt(4, p.mreznaVrataKraj());
            s.setFloat(5, p.gpsSirina());
            s.setFloat(6, p.gpsDuzina());
            s.setString(7, p.sigurnosniKod());
            s.setString(8, p.adminKod());
            s.setInt(9, p.id());
            return s.executeUpdate() == 1;
        } catch (SQLException ex) {
            Logger.getLogger(PartnerDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    /**
     * Dodaj.
     *
     * @param p the p
     * @return true, if successful
     */
    public boolean dodaj(Partner p) {
        String upit = "INSERT INTO partneri (id, naziv, vrstaKuhinje, adresa, mreznaVrata, mreznaVrataKraj, gpsSirina, gpsDuzina, sigurnosniKod, adminKod) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement s = this.vezaBP.prepareStatement(upit)) {
            s.setInt(1, p.id());
            s.setString(2, p.naziv());
            s.setString(3, p.vrstaKuhinje());
            s.setString(4, p.adresa());
            s.setInt(5, p.mreznaVrata());
            s.setInt(6, p.mreznaVrataKraj());
            s.setFloat(7, p.gpsSirina());
            s.setFloat(8, p.gpsDuzina());
            s.setString(9, p.sigurnosniKod());
            s.setString(10, p.adminKod());
            return s.executeUpdate() == 1;
        } catch (Exception ex) {
            Logger.getLogger(PartnerDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }
}
