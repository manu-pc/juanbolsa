package servidor;

import bolsa.BolsaIbex;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Timer;
import java.util.TimerTask;


public class ServidorMain {
    public static int refreshTime = 60000; // en ms (60000 = 1')
    static void main(String[] args) {
        try {

            if(!BolsaIbex.mercadoAbierto()){
                System.out.println("[MAIN] Aviso: El mercado está cerrado ahora mismo.");
            }
            boolean debug;
            if (args.length > 0 && args[0].equals("1")) {
                System.out.println("[MAIN] Aviso: Modo debug activado.");
                debug = true;
            } else {
                debug = false;
            }
            Servidor serv = new Servidor();

            Registry reg = LocateRegistry.createRegistry(1099);


            reg.rebind("ServidorBolsa", serv);
            System.out.println("[MAIN] Servidor RMI listo y esperando clientes! ");

            serv.initServidor();
            serv.actualizarValoresBolsa(false); // inicializa datos sin variacion

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
                            ));
                    serv.actualizarValoresBolsa(debug); // modo debug: inventar variacion aleatoria
                }
            }, refreshTime, refreshTime);

        }
        catch (Exception e) {
            System.err.println("Error creando servidor: " + e.getMessage());
            e.printStackTrace();
        }



    }

}