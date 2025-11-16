package cliente;

import bolsa.Alerta;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ICliente extends Remote {
    void notificarAlerta(Alerta alerta) throws RemoteException;

}
