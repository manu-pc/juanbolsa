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
    private BasicWindow ventana;

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

        // Inicializar la interfaz TUI
        inicializarTUI();
    }

    private void inicializarTUI() {
        try {
            Terminal terminal = new DefaultTerminalFactory().createTerminal();
            screen = new TerminalScreen(terminal);
            screen.startScreen();

            gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(),
                    new EmptySpace(TextColor.ANSI.BLUE));

            ventana = new BasicWindow("Monitor de Bolsa - IBEX 35");
            ventana.setHints(java.util.Set.of(Window.Hint.CENTERED));

            Panel mainPanel = new Panel();
            mainPanel.setLayoutManager(new GridLayout(1));

            // Título
            Label titulo = new Label("Monitor de Bolsa - IBEX 35");
            titulo.setForegroundColor(TextColor.ANSI.CYAN_BRIGHT);
            titulo.setLayoutData(GridLayout.createLayoutData(
                    GridLayout.Alignment.CENTER,
                    GridLayout.Alignment.CENTER,
                    true,
                    false
            ));
            mainPanel.addComponent(titulo);

            Label instrucciones = new Label("Seleccione una acción y pulse ENTER para crear alerta");
            instrucciones.setForegroundColor(TextColor.ANSI.YELLOW);
            instrucciones.setLayoutData(GridLayout.createLayoutData(
                    GridLayout.Alignment.CENTER,
                    GridLayout.Alignment.CENTER,
                    true,
                    false
            ));
            mainPanel.addComponent(instrucciones);

            mainPanel.addComponent(new EmptySpace(TerminalSize.ONE));

            // Tabla de acciones
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
                    if (col == 2) { // Columna de variación
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

            tabla.setSelectAction(() -> {
                int idx = tabla.getSelectedRow();
                if (idx >= 0 && idx < acciones.size()) {
                    mostrarDialogoAlerta(acciones.get(idx));
                }
            });

            tabla.setLayoutData(GridLayout.createLayoutData(
                    GridLayout.Alignment.FILL,
                    GridLayout.Alignment.FILL,
                    true,
                    true
            ));

            mainPanel.addComponent(tabla.withBorder(Borders.singleLine()));

            mainPanel.addComponent(new EmptySpace(TerminalSize.ONE));

            // Botones
            mainPanel.addComponent(new Button("Actualizar", () -> actualizarAcciones()));
            mainPanel.addComponent(new Button("Salir", () -> cerrarAplicacion()));

            ventana.setComponent(mainPanel);

            // Cargar datos iniciales
            actualizarAcciones();

            gui.addWindowAndWait(ventana);

        } catch (Exception e) {
            System.err.println("Error inicializando TUI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void actualizarAcciones() {
        try {
            acciones = serv.getStocks();
            tabla.getTableModel().clear();

            for (Accion accion : acciones) {
                tabla.getTableModel().addRow(
                        accion.getNombre(),
                        accion.getUltimoPrecio(),
                        accion.getVariacion()
                );
            }

            System.out.println("[CLIENTE] Acciones actualizadas: " + acciones.size());
        } catch (RemoteException e) {
            MessageDialog.showMessageDialog(gui, "Error",
                    "Error al obtener acciones: " + e.getMessage(),
                    MessageDialogButton.OK);
        }
    }

    private void mostrarDialogoAlerta(Accion accion) {
        BasicWindow dialogoAlerta = new BasicWindow("Crear Alerta");
        dialogoAlerta.setHints(java.util.Set.of(Window.Hint.CENTERED));

        Panel mainPanel = new Panel();
        mainPanel.setLayoutManager(new GridLayout(1));

        double precioActual = Double.parseDouble(accion.getUltimoPrecio().replace(",", "."));

        // Título
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

        // Información de la acción
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

        // Formulario
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

        // Botones
        mainPanel.addComponent(new Button("Crear Alerta", () -> {
            try {
                double valor = Double.parseDouble(txtValor.getText().replace(",", "."));
                boolean esCompra = comboTipo.getSelectedIndex() == 0;

                Alerta alerta = new Alerta(accion.getNombre(), valor, esCompra);

                try {
                    serv.pedirAlerta(alerta, this);
                    MessageDialog.showMessageDialog(gui, "Éxito",
                            String.format("Alerta creada:\n%s: %.2f € (%s)",
                                    accion.getNombre(), valor,
                                    esCompra ? "Compra" : "Venta"),
                            MessageDialogButton.OK);
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

    @Override
    public void notificarAlerta(Alerta alerta) throws RemoteException {
        System.out.println("[CLIENTE] ¡ALERTA RECIBIDA! " + alerta.getNombreStock()
                + " - " + alerta.getValor() + " € - " + (alerta.getEsCompra() ? "Compra" : "Venta"));

        if (gui != null) {
            gui.getGUIThread().invokeLater(() -> {
                try {
                    MessageDialog.showMessageDialog(gui, "⚠ ALERTA ACTIVADA ⚠",
                            String.format("Acción: %s\nValor: %.2f €\nTipo: %s\n\n¡El precio ha alcanzado el umbral!",
                                    alerta.getNombreStock(),
                                    alerta.getValor(),
                                    alerta.getEsCompra() ? "COMPRA (bajó)" : "VENTA (subió)"),
                            MessageDialogButton.OK);
                } catch (Exception e) {
                    System.err.println("Error mostrando alerta: " + e.getMessage());
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