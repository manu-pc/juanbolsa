package cliente;

import bolsa.Accion;
import bolsa.Alerta;
import bolsa.BolsaIbex;
import servidor.IServidor;

import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.ArrayList;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.screen.*;
import com.googlecode.lanterna.terminal.*;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.dialogs.*;
import com.googlecode.lanterna.gui2.table.*;

public class Cliente extends UnicastRemoteObject implements ICliente {

    private IServidor serv;
    private List<Accion> acciones;
    private Screen screen;
    private MultiWindowTextGUI gui;
    private Table<String> tabla;

    public Cliente() throws RemoteException {
        super();
        acciones = new ArrayList<>();
    }

    public void init() throws RemoteException {
        Registry registry = LocateRegistry.getRegistry("localhost", 1099);

        try {
            serv = (IServidor) registry.lookup("ServidorBolsa");
        } catch (NotBoundException es) {
            System.err.println("Error buscando el registro. ¿Está iniciado el servidor?");
            System.exit(1);
        }

        System.out.println("[CLIENTE] conectado al servidor!");

        // inicia interfaz lanterna
        inicializarTUI();
    }

    private void inicializarTUI() {
        try {
            Terminal terminal = new DefaultTerminalFactory().createTerminal();
            screen = new TerminalScreen(terminal);
            screen.startScreen();

            gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(),
                    new EmptySpace(TextColor.ANSI.BLUE));

            BasicWindow ventana = new BasicWindow("Monitor de Bolsa - IBEX 35");
            ventana.setHints(java.util.Set.of(Window.Hint.CENTERED));

            // ll vertical
            Panel mainPanel = new Panel();
            mainPanel.setLayoutManager(new LinearLayout(Direction.VERTICAL));

            // título -
            Label titulo = new Label("Monitor de Bolsa - IBEX 35");
            titulo.setForegroundColor(TextColor.ANSI.CYAN_BRIGHT);
            titulo.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Center));
            mainPanel.addComponent(titulo);

            // instrucciones
            Label instrucciones = new Label("Seleccione una acción y pulse ENTER para crear alerta");
            instrucciones.setForegroundColor(TextColor.ANSI.YELLOW);
            instrucciones.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Center));
            mainPanel.addComponent(instrucciones);

            // indicador de estado del mercado
            boolean abierto = BolsaIbex.mercadoAbierto();
            Label estadoMercado = new Label(abierto ? "[O] Mercado ABIERTO" : "[X] Mercado CERRADO");
            estadoMercado.setForegroundColor(abierto ? TextColor.ANSI.GREEN : TextColor.ANSI.RED);
            estadoMercado.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Center));
            mainPanel.addComponent(estadoMercado);

            mainPanel.addComponent(new EmptySpace(TerminalSize.ONE));

            // tabla main de acciones
            tabla = new Table<>("Nombre", "Precio Actual", "Variación");
            tabla.setTableCellRenderer(new TableCellRenderer<String>() {
                @Override
                public void drawCell(Table<String> table, String cell, int col, int row,
                                     TextGUIGraphics graphics) {
                    if (row >= acciones.size() || cell == null) {
                        graphics.putString(0, 0, cell != null ? cell : "");
                        return;
                    }

                    TextColor color = TextColor.ANSI.WHITE;
                    if (col == 2) { // columna de variación ten texto custom
                        String var = acciones.get(row).getVariacion();
                        if (var.startsWith("+")) {
                            color = TextColor.ANSI.GREEN_BRIGHT;
                        } else if (var.startsWith("-")) {
                            color = TextColor.ANSI.RED_BRIGHT;
                        }
                    }
                    graphics.setForegroundColor(color);
                    graphics.putString(0, 0, cell);
                }

                @Override
                public TerminalSize getPreferredSize(Table<String> table, String cell,
                                                     int col, int row) {
                    if (cell == null) return new TerminalSize(10, 1);
                    return new TerminalSize(cell.length() + 2, 1);
                }
            });

            tabla.setSelectAction(() -> { // ao seleccionar una fila permite crear unha alerta
                int idx = tabla.getSelectedRow();
                if (idx >= 0 && idx < acciones.size()) {
                    mostrarDialogoAlerta(acciones.get(idx));
                }
            });

            Panel tablePanel = new Panel(new BorderLayout());
            tablePanel.addComponent(tabla.withBorder(Borders.singleLine()));

            // ll para que a tabla se poda expandir
            tablePanel.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));
            mainPanel.addComponent(tablePanel);

            mainPanel.addComponent(new EmptySpace(TerminalSize.ONE));

            // botones
            Panel buttonPanel = new Panel(new LinearLayout(Direction.HORIZONTAL));
            buttonPanel.setLayoutData(LinearLayout.createLayoutData(LinearLayout.Alignment.Center));

            buttonPanel.addComponent(new Button("Actualizar", this::actualizarAcciones));
            buttonPanel.addComponent(new EmptySpace(new TerminalSize(2, 1))); // spacer
            buttonPanel.addComponent(new Button("Salir", this::cerrarAplicacion));

            mainPanel.addComponent(buttonPanel);

            ventana.setComponent(mainPanel);
            actualizarAcciones();

            gui.addWindowAndWait(ventana);

        } catch (Exception e) {
            System.err.println("Error inicializando TUI: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void actualizarAcciones() {
        // intenta recibir accions do servidor.
        // se se intenta recibir xusto no momento mentras o servidor está conectando coa web, devolverá 0,
        // asi que nese caso hai que volver a intentalo

        // utiliza timer exponencial
        try {
            int maxReintentos = 5;
            int baseDelayMs = 1000; // delay inicial: 1 segundo
            int intento = 0;

            while (intento < maxReintentos) {
                acciones = serv.getStocks();

                if (acciones != null && acciones.size() == BolsaIbex.numAcciones) {
                    tabla.getTableModel().clear();

                    for (Accion accion : acciones) {
                        tabla.getTableModel().addRow(
                                accion.getNombre(),
                                accion.getUltimoPrecio(),
                                accion.getVariacion()
                        );
                    }

                    System.out.println("[CLIENTE] Acciones actualizadas: " + acciones.size());
                    return;
                } else {
                    intento++;
                    System.out.println("[CLIENTE] No se obtuvieron suficientes datos. Reintentando...");

                    if (intento < maxReintentos) {
                        // 1, 2, 4, 8...
                        int delay = baseDelayMs * (int) Math.pow(2, intento - 1);
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }

            // se se agotaron todos os reintentos
            mensajeError("No se han podido cargar las acciones. Vuelva a intentarlo más tarde.");

        } catch (RemoteException e) {
            mensajeError("No se han podido cargar las acciones. Vuelva a intentarlo más tarde.");
            System.err.println(e.getMessage());
        }
    }

    private void mensajeError(String mensaje) {
        // crea unha ventana co mensaje dados
        tabla.getTableModel().clear();

        MessageDialog.showMessageDialog(gui, "Error",
                mensaje,
                MessageDialogButton.OK);
    }

    private void mostrarDialogoAlerta(Accion accion) {
        // ventana para crear unha alerta nova
        BasicWindow dialogoAlerta = new BasicWindow("Definir Alerta");
        dialogoAlerta.setHints(java.util.Set.of(Window.Hint.CENTERED));

        Panel mainPanel = new Panel();
        mainPanel.setLayoutManager(new GridLayout(1));

        double precioActual = Double.parseDouble(accion.getUltimoPrecio().replace(",", "."));

        // titulo
        Label titulo = new Label("Crear Alerta");
        titulo.setForegroundColor(TextColor.ANSI.CYAN_BRIGHT);
        titulo.setLayoutData(GridLayout.createLayoutData(
                GridLayout.Alignment.CENTER,
                GridLayout.Alignment.CENTER,
                true,
                false
        ));
        mainPanel.addComponent(titulo);

        mainPanel.addComponent(new EmptySpace(TerminalSize.ONE));

        // info accion + entrada
        Panel panelInfo = new Panel(new GridLayout(2));

        panelInfo.addComponent(new Label("Acción:"));
        Label lblNombre = new Label(accion.getNombre());
        lblNombre.setForegroundColor(TextColor.ANSI.CYAN_BRIGHT);
        panelInfo.addComponent(lblNombre);

        panelInfo.addComponent(new Label("Precio actual:"));
        Label lblPrecio = new Label(String.format("%.2f €", precioActual));
        lblPrecio.setForegroundColor(TextColor.ANSI.GREEN_BRIGHT);
        panelInfo.addComponent(lblPrecio);

        mainPanel.addComponent(panelInfo);
        mainPanel.addComponent(new EmptySpace(TerminalSize.ONE));

        Panel panelForm = new Panel(new GridLayout(2));

        panelForm.addComponent(new Label("Valor umbral:"));
        TextBox txtValor = new TextBox(String.format("%.2f", precioActual));
        panelForm.addComponent(txtValor);

        panelForm.addComponent(new Label("Tipo:"));
        ComboBox<String> comboTipo = new ComboBox<>("Compra (avisa al bajar)",
                "Venta (avisa al subir)");
        panelForm.addComponent(comboTipo);

        mainPanel.addComponent(panelForm);
        mainPanel.addComponent(new EmptySpace(TerminalSize.ONE));

        // botons
        mainPanel.addComponent(new Button("Crear Alerta", () -> {
            try {
                double valor = Double.parseDouble(txtValor.getText().replace(",", "."));
                boolean esCompra = comboTipo.getSelectedIndex() == 0;

                Alerta alerta = new Alerta(accion.getNombre(), valor, esCompra);

                try {
                    // a ventana de info imprimese antes de pedir a alerta para que se a alerta se cumple xa no momento,
                    // queda mellor que sala primeiro o aviso de "alerta creada" e despois o de "notificacion recibida"
                    MessageDialog.showMessageDialog(gui, "Info",
                            String.format("Alerta creada:\n%s: %.2f € (%s)",
                                    accion.getNombre(), valor,
                                    esCompra ? "Compra" : "Venta"),
                            MessageDialogButton.OK);
                    serv.pedirAlerta(alerta, this);

                    System.out.println("[CLIENTE] Alerta creada: " + alerta.getNombreStock()
                            + " - " + valor + " € - " + (esCompra ? "Compra" : "Venta"));
                    dialogoAlerta.close();
                } catch (RemoteException e) {
                    MessageDialog.showMessageDialog(gui, "Error",
                            "Error al enviar alerta: " + e.getMessage(),
                            MessageDialogButton.OK);
                }
            } catch (NumberFormatException e) {
                MessageDialog.showMessageDialog(gui, "Error",
                        "Valor inválido. Ingrese un número válido.",
                        MessageDialogButton.OK);
            }
        }));

        mainPanel.addComponent(new Button("Cancelar", dialogoAlerta::close));

        dialogoAlerta.setComponent(mainPanel);
        gui.addWindow(dialogoAlerta);
    }

    @Override //! metodo interfaz
    public void notificarAlerta(Alerta alerta, String valorActual) throws RemoteException {
        System.out.println("[CLIENTE] ¡ALERTA RECIBIDA! " + alerta.getNombreStock()
                + " - " + alerta.getValor() + " € - " + (alerta.getEsCompra() ? "Compra" : "Venta"));

        if (gui != null) {
            gui.getGUIThread().invokeLater(() -> {
                try {
                    MessageDialog.showMessageDialog(gui, "ALERTA ACTIVADA",
                            String.format("Acción: %s\nUmbral de alerta: %.2f €\nTipo: %s\nValor actual: %s\n\n¡El precio ha alcanzado el umbral!",
                                    alerta.getNombreStock(),
                                    alerta.getValor(),
                                    alerta.getEsCompra() ? "COMPRA (bajó)" : "VENTA (subió)",
                                    valorActual),
                            MessageDialogButton.OK);
                } catch (Exception e) {
                    System.err.println("Error mostrando alerta: " + e.getMessage());
                    mensajeError("Error mostrando alerta: " + e.getMessage());
                }
            });
        }
    }

    private void cerrarAplicacion() {
        try {
            if (screen != null) {
                screen.stopScreen();
            }
            System.exit(0);
        } catch (Exception e) {
            System.err.println("Error cerrando aplicación: " + e.getMessage());
        }
    }
}