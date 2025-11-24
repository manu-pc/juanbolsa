package servidor;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Timer;
import java.util.TimerTask;

//? clase main que crea un servidor e o rexistra
public class ServidorMain {
    static void main() {
        try {
            Servidor serv = new Servidor();
            Registry reg = LocateRegistry.createRegistry(1099);
            reg.rebind("ServidorBolsa", serv);
            System.out.println("[MAIN] Servidor RMI listo y esperando clientes! ");

            serv.initServidor();

            Timer timer = new Timer(true); // daemon=trve
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    // vaciar consola
                    System.out.print("\033[H\033[2J");
                    System.out.flush();
                    System.out.println("ultima actualización - " +
                            java.time.LocalDateTime.now().format(
                                    java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
                            )); // imprime hora para debug
                    serv.actualizarValoresBolsa();
                }
            }, 0, 60000); // 1 minuto, o primeiro é instantaneo

        }
        catch (Exception e) {
            System.err.println("Error creando servidor: " + e.getMessage());
            e.printStackTrace();
        }



    }

}