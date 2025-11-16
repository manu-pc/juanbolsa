package servidor;

import bolsa.Accion;
import bolsa.Alerta;
import cliente.ICliente;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.Duration;
import java.util.*;

public class Servidor extends UnicastRemoteObject implements IServidor{
    public static final String urlString = "https://www.bolsasymercados.es/bme-exchange/es/Mercados-y-Cotizaciones/Acciones/Mercado-Continuo/Precios/ibex-35-ES0SI0000005";
    private Map<Alerta, ICliente> alertas;
    private List<Accion> accions;
    private WebDriver driver;

    public Servidor() throws RemoteException {
        this.accions = new ArrayList<>();
        alertas = new HashMap<>();
        java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.SEVERE); // esconder warns
        //initServidor();
        //actualizarValoresBolsa();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> { // executase cando acaba o programa
            System.out.println("[SERVIDOR] Cerrando driver...");
            cerrarDriver();
        }));
    }

    public void initServidor() {
        try{
            System.out.println("[SERVIDOR] Creando driver...");
            WebDriverManager.firefoxdriver().setup();

            // conectar con firefox
            FirefoxOptions options = new FirefoxOptions();
            options.addArguments("--headless");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            driver = new FirefoxDriver(options);
            System.out.println("[SERVIDOR] Conectando a: " + urlString +"...");
        }
        catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            if (driver != null) driver.quit();

        }

        //actualizarValoresBolsa();
    }
    public void actualizarValoresBolsa(boolean debug) {
        accions.clear();
        try {
            driver.get(urlString);
            System.out.println("[SERVIDOR] Leyendo datos de las acciones...");
            // espera maximo 10 segundos a que aparezca a tabla
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("table")));
            extraerDatosAcciones();
            if (debug) simularVariacionesAleatorias();
            for (Accion accion : accions) {
                System.out.println(accion);
            }
            revisarAlertas();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());


    }}


    private void revisarAlertas(){
        for (Alerta a : alertas.keySet()){
            revisarAlerta(a);
        }
    }

    private void revisarAlerta(Alerta ale){
        String nombreAcc = ale.getNombreStock();
        // se asume que los nombres de acciones son unicos
        Accion acc = null;
        for (Accion accion : accions){
            if (accion.getNombre().equals(nombreAcc)){
                acc = accion;
                break;
            }
        }
        if (acc == null){
            System.err.println("[SERVIDOR] ERROR: se ha realizado una alerta para la acción \"" +  nombreAcc + "\", que no existe.");
            alertas.remove(ale);
            return;
        }

        if((ale.getEsCompra() && ale.getValor() > acc.getUltimoPrecioFloat()) || (!ale.getEsCompra() && ale.getValor() < acc.getUltimoPrecioFloat())){
            ICliente cliente = alertas.get(ale);
            try {
                System.out.println("Cliente quería ser informado cuando [" + acc.getNombre() + "]" + (ale.getEsCompra()? " bajase " : " subiese ") + "de (" + ale.getValor()+"), y ahora ha alcanzado " + acc.getUltimoPrecio() + ". Enviando notificación...");
                cliente.notificarAlerta(ale, acc.getUltimoPrecio());
            } catch (RemoteException e) {
                System.err.println("[SERVIDOR] Error notificando alerta: " + e.getMessage());
            }
            alertas.remove(ale);
        }
    }

    private void cerrarDriver(){
        if (driver!= null) driver.quit();
    }

    private void extraerDatosAcciones() {
        List<WebElement> tables = driver.findElements(By.cssSelector("table"));

        if (tables.size() >= 2) {
            WebElement tablaAcciones = tables.get(1); // segunda tabla ten os datos
            List<WebElement> rows = tablaAcciones.findElements(By.cssSelector("tr"));

            for (int i = 1; i < rows.size(); i++) { // empezar desde a segunda fila
                WebElement row = rows.get(i);
                List<WebElement> cells = row.findElements(By.cssSelector("td, th"));

                if (cells.size() >= 3) {
                    String nombre = cells.get(0).getText().trim();
                    String precio = cells.get(1).getText().trim();
                    String variacion = cells.get(2).getText().trim();

                    if (!nombre.isEmpty() && !precio.isEmpty()) {
                        accions.add(new Accion(nombre, precio, variacion));
                    }
                }
            }
        }
    }

    private void simularVariacionesAleatorias() {
        Random random = new Random();
        // modifica todas as accions da lista añadindolles unha variacion aleatoria
        // para facer probas cando o mercado está cerrado

        for (Accion accion : accions) {
            try {
                double precioActual =  accion.getUltimoPrecioFloat();

                // variación aleatoria entre -2% y +2%
                double porcentajeVariacion = (random.nextDouble() * 4.0) - 2.0;
                double variacionPrecio = precioActual * (porcentajeVariacion / 100.0);
                double nuevoPrecio = precioActual + variacionPrecio;

                // as accions non poden valer negativo creo
                if (nuevoPrecio < 0.01) {
                    nuevoPrecio = 0.01;
                }

                String nuevoPrecioStr = String.format(Locale.US, "%.2f", nuevoPrecio).replace(".", ",");

                // variacion porcentual
                String variacionStr = String.format(Locale.US, "%.2f%%", porcentajeVariacion);
                if (porcentajeVariacion > 0) {
                    variacionStr = "+" + variacionStr;
                }

                accion.setUltimoPrecio(nuevoPrecioStr);
                accion.setVariacion(variacionStr);

            } catch (NumberFormatException e) {
                System.err.println("Error parseando precio para " + accion.getNombre() + ": " + accion.getUltimoPrecio());
            }
        }
    }

    @Override
    public List<Accion> getStocks() {
        return new ArrayList<>(accions);
    }

    @Override
    public void pedirAlerta(Alerta alerta, ICliente cliente) throws RemoteException {
        this.alertas.put(alerta, cliente);
        System.out.println("[SERVIDOR] Alerta registrada: " + alerta.getNombreStock() +
                " " + alerta.getEsCompra() + " umbral: " + alerta.getValor());

        revisarAlerta(alerta);

    }

}