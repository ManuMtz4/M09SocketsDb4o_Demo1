package server;

/**
 * Missatge DB4o
 *
 * @author Manuel Martinez
 *         Copyright 2017, ManuMtz
 */

public class Missatge {

    private int id;
    private String missatge;

    public Missatge(int id) {
        this.id = id;
    }

    public Missatge(int id, String missatge) {
        this.id = id;
        this.missatge = missatge;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMissatge() {
        return missatge;
    }

    public void setMissatge(String missatge) {
        this.missatge = missatge;
    }

    @Override
    public String toString() {
        return "Missatge "+id+" [ " + missatge+ " ]";
    }
}
