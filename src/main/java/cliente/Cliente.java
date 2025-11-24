package cliente;

import bolsa.Accion;
import bolsa.Alerta;
import servidor.IServidor;

import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.ArrayList;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

//? clase Cliente que manexa a conexion entre o servidor e a TUI
public class Cliente extends UnicastRemoteObject implements ICliente {

    private IServidor serv;
    private List<Accion> acciones;
    private ClienteTUI gui;

    public Cliente() throws RemoteException {
        super();
        acciones = new ArrayList<>();
    }

    public void init() throws RemoteException {
        Registry registry = LocateRegistry.getRegistry("localhost", 1099);

        try {
            serv = (IServidor) registry.lookup("ServidorBolsa");
            // solo funciona se xa está iniciado o serv
        } catch (NotBoundException es) {
            System.err.println("Error buscando el registro. ¿Está iniciado el servidor?");
            System.exit(1);
        }

        System.out.println("[CLIENTE] conectado al servidor!");
        gui = new ClienteTUI(this);
        gui.inicializarTUI();
    }

    public List<Accion> getAcciones() {
        return acciones;
    }

    public void actualizarAcciones() {
        try {
            // método que chama aio metodo remoto de serv para leer as stocks que ten ahora
            acciones = serv.getStocks();
            System.out.println("[CLIENTE] Acciones actualizadas: " + acciones.size());
            gui.actualizarTablaAcciones(acciones);
        } catch (RemoteException e) {
            gui.mostrarError("Error al obtener acciones: " + e.getMessage());
        }
    }

    // método que chama ao metodo remoto de serv para pasarlle unha nova alerta
    public void crearAlerta(Accion accion, double valor, boolean esCompra) {
        try {
            Alerta alerta = new Alerta(accion.getNombre(), valor, esCompra);
            serv.pedirAlerta(alerta, this);
            System.out.println("[CLIENTE] Alerta creada: " + alerta.getNombreStock()
                    + " - " + valor + " € - " + (esCompra ? "Compra" : "Venta"));
            gui.mostrarMensaje("Éxito",
                    String.format("Alerta creada:\n%s: %.2f € (%s)",
                            accion.getNombre(), valor, esCompra ? "Compra" : "Venta"));
        } catch (RemoteException e) {
            gui.mostrarError("Error al enviar alerta: " + e.getMessage());
        }
    }

    // método remoto que usa o serv para pasarlle unha alerta que se cumpliu
    @Override
    public void notificarAlerta(Alerta alerta) throws RemoteException {
        System.out.println("[CLIENTE] ¡ALERTA RECIBIDA! " + alerta.getNombreStock()
                + " - " + alerta.getValor() + " € - " + (alerta.getEsCompra() ? "Compra" : "Venta"));

        if (gui != null) {
            gui.mostrarAlerta(alerta);
        }
    }

    public void cerrarAplicacion() {
        try {
            gui.cerrarGUI();
            System.exit(0);
        } catch (Exception e) {
            System.err.println("Error cerrando aplicación: " + e.getMessage());
        }
    }

}