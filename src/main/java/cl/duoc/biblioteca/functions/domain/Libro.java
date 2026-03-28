package cl.duoc.biblioteca.functions.domain;

public class Libro {

    private String id;
    private String isbn;
    private String titulo;
    private Integer anioPublicacion;
    private Integer copiasTotales;
    private Integer copiasDisponible;
    private String idAutor;

    public Libro() {
    }

    public Libro(String id, String isbn, String titulo, Integer anioPublicacion, Integer copiasTotales, Integer copiasDisponible, String idAutor) {
        this.id = id;
        this.isbn = isbn;
        this.titulo = titulo;
        this.anioPublicacion = anioPublicacion;
        this.copiasTotales = copiasTotales;
        this.copiasDisponible = copiasDisponible;
        this.idAutor = idAutor;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public Integer getAnioPublicacion() {
        return anioPublicacion;
    }

    public void setAnioPublicacion(Integer anioPublicacion) {
        this.anioPublicacion = anioPublicacion;
    }

    public Integer getCopiasTotales() {
        return copiasTotales;
    }

    public void setCopiasTotales(Integer copiasTotales) {
        this.copiasTotales = copiasTotales;
    }

    public Integer getCopiasDisponible() {
        return copiasDisponible;
    }

    public void setCopiasDisponible(Integer copiasDisponible) {
        this.copiasDisponible = copiasDisponible;
    }

    public String getIdAutor() {
        return idAutor;
    }

    public void setIdAutor(String idAutor) {
        this.idAutor = idAutor;
    }
}
