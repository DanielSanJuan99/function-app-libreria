package cl.duoc.biblioteca.functions.domain;

public class Notificacion {

    private String id;
    private String idUsuario;
    private String tipo;
    private String asunto;
    private String cuerpo;
    private String estado;
    private String fechaCreacion;
    private String fechaEnvio;

    public Notificacion() {
    }

    public Notificacion(String id, String idUsuario, String tipo, String asunto, String cuerpo,
                        String estado, String fechaCreacion, String fechaEnvio) {
        this.id = id;
        this.idUsuario = idUsuario;
        this.tipo = tipo;
        this.asunto = asunto;
        this.cuerpo = cuerpo;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
        this.fechaEnvio = fechaEnvio;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(String idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public String getCuerpo() {
        return cuerpo;
    }

    public void setCuerpo(String cuerpo) {
        this.cuerpo = cuerpo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(String fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(String fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }
}
