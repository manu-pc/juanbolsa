package bolsa;
import java.io.Serial;
import java.io.Serializable;

public class Accion implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    // serializable para que se poda pasar convertido a bytes en RMI

    private String nombre;
    private String ultimoPrecio;
    private String variacion;

    public Accion(String nombre, String ultimoPrecio, String variacion) {
        this.nombre = nombre;
        this.ultimoPrecio = ultimoPrecio;
        this.variacion = variacion;
    }

    public String getNombre() { return nombre; }
    public String getUltimoPrecio() { return ultimoPrecio.replace(",", "."); }
    public String getVariacion() { return variacion; }
    public float getUltimoPrecioFloat() { return Float.parseFloat(getUltimoPrecio()); }
    @Override
    public String toString() {
        return String.format("%-20s | %-10s | %s", nombre, ultimoPrecio, variacion);

    }

    public void setUltimoPrecio(String nuevoPrecioStr) {
        this.ultimoPrecio = nuevoPrecioStr.replace(",", "."); // por se aca
    }

    public void setVariacion(String variacion) {
        this.variacion = variacion.replace(",", ".");
    }

}