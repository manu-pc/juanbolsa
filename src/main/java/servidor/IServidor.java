package servidor;

import java.rmi.RemoteException;
import java.rmi.Remote;
import java.util.List;

import bolsa.Accion;
import bolsa.Alerta;
import cliente.ICliente;

public interface IServidor extends Remote {
    List<Accion> getStocks() throws RemoteException;
    void pedirAlerta(Alerta alerta, ICliente cliente) throws RemoteException;

}
