package cliente;

//? clase main para iniciar o cliente
public class ClienteMain {
    static void main() {
        try {
            Cliente cli = new Cliente();
            cli.init();
        }
        catch (Exception e) {
            System.err.println("Error creando Cliente: " + e.getMessage());
            e.printStackTrace();
        }
    }
}