package bolsa;

import java.io.Serial;
import java.io.Serializable;

public class Alerta implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String nombreStock;
    private final double valor;
    private final boolean esCompra; // compra: true, avisa cuando precio baja,
                              // venta: false, avisa cuando precio sube

    public Alerta(String nombreStock, double valor, boolean esCompra) {
        this.nombreStock = nombreStock;
        this.valor = valor;
        this.esCompra = esCompra;
    }

    public String getNombreStock() {
        return nombreStock;
    }

    public double getValor() {
        return valor;
    }

    public boolean getEsCompra() {
        return esCompra;
    }
}
