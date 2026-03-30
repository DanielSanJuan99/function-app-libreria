package cl.duoc.biblioteca.functions.domain;

public class Autor {

    private String id;
    private String nombreAutor;

    public Autor() {
    }

    public Autor(String id, String nombreAutor) {
        this.id = id;
        this.nombreAutor = nombreAutor;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombreAutor() {
        return nombreAutor;
    }

    public void setNombreAutor(String nombreAutor) {
        this.nombreAutor = nombreAutor;
    }
}
