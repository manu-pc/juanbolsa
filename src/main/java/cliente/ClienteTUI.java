package cliente;

import bolsa.Accion;
import bolsa.Alerta;
import com.googlecode.lanterna.*;
import com.googlecode.lanterna.screen.*;
import com.googlecode.lanterna.terminal.*;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.dialogs.*;
import com.googlecode.lanterna.gui2.table.*;

import java.util.List;

public class ClienteTUI {
    //? ClienteTUI: clase que manexa a interfaz en lanterna
    private final Cliente cliente;
    private Screen screen;
    private MultiWindowTextGUI gui;
    private Table<String> tabla;

    public ClienteTUI(Cliente cliente) {
        this.cliente = cliente;
    }

    // init
    public void inicializarTUI() {
        try {
            Terminal terminal = new DefaultTerminalFactory().createTerminal();
            screen = new TerminalScreen(terminal);
            screen.startScreen();

            gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(),
                    new EmptySpace(TextColor.ANSI.BLUE));

            BasicWindow ventana = new BasicWindow("Monitor de Bolsa - IBEX 35");
            ventana.setHints(java.util.Set.of(Window.Hint.CENTERED));

            Panel mainPanel = new Panel();
            mainPanel.setLayoutManager(new GridLayout(1));

            // título
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

            // tabla de acciones
            tabla = crearTablaAcciones();
            tabla.setLayoutData(GridLayout.createLayoutData(
                    GridLayout.Alignment.FILL,
                    GridLayout.Alignment.FILL,
                    true,
                    true
            ));

            mainPanel.addComponent(tabla.withBorder(Borders.singleLine()));

            mainPanel.addComponent(new EmptySpace(TerminalSize.ONE));

            // botóns
            mainPanel.addComponent(new Button("Actualizar", cliente::actualizarAcciones));
            mainPanel.addComponent(new Button("Salir", cliente::cerrarAplicacion));

            ventana.setComponent(mainPanel);

            // carga inicial
            cliente.actualizarAcciones();

            gui.addWindowAndWait(ventana);

        } catch (Exception e) {
            System.err.println("Error inicializando TUI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // tabla coas accións
    private Table<String> crearTablaAcciones() {
        Table<String> tabla = new Table<>("Nombre", "Precio Actual", "Variación");
        tabla.setTableCellRenderer(new TableCellRenderer<>() {
            @Override
            public void drawCell(Table<String> table, String cell, int col, int row,
                                 TextGUIGraphics graphics) {
                List<Accion> acciones = cliente.getAcciones();
                if (row >= acciones.size() || cell == null) {
                    graphics.putString(0, 0, cell != null ? cell : "");
                    return;
                }   // placeholder para rellenar espacio se se accede a unha posición inválida

                TextColor color = TextColor.ANSI.WHITE;
                if (col == 2) { // variacion, con color especial se é neg
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
            // tamaño da celda
            public TerminalSize getPreferredSize(Table<String> table, String cell,
                                                 int col, int row) {
                if (cell == null) return new TerminalSize(10, 1);
                return new TerminalSize(cell.length() + 2, 1);
            }
        });

        tabla.setSelectAction(() -> {
            int idx = tabla.getSelectedRow();
            List<Accion> acciones = cliente.getAcciones();
            if (idx >= 0 && idx < acciones.size()) {
                mostrarDialogoAlerta(acciones.get(idx));
            }
        }); // ao clicar nunha celda abrese o dialoigo de crear unha alerta para esa accion

        return tabla;
    }

    public void actualizarTablaAcciones(List<Accion> acciones) {
        if (tabla != null) {
            gui.getGUIThread().invokeLater(() -> {
                tabla.getTableModel().clear();
                for (Accion accion : acciones) {
                    tabla.getTableModel().addRow(
                            accion.getNombre(),
                            accion.getUltimoPrecio(),
                            accion.getVariacion()
                    );
                }
            });
        }
    }

    // popup para crear unha alerta
    private void mostrarDialogoAlerta(Accion accion) {
        BasicWindow dialogoAlerta = new BasicWindow("Crear Alerta");
        dialogoAlerta.setHints(java.util.Set.of(Window.Hint.CENTERED));

        Panel mainPanel = new Panel();
        mainPanel.setLayoutManager(new GridLayout(1));

        double precioActual = Double.parseDouble(accion.getUltimoPrecio().replace(",", "."));

        // yítulo
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

        // a acción
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

        // input
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

        mainPanel.addComponent(new Button("Crear Alerta", () -> {
            try {
                double valor = Double.parseDouble(txtValor.getText().replace(",", "."));
                boolean esCompra = comboTipo.getSelectedIndex() == 0;
                cliente.crearAlerta(accion, valor, esCompra);
                dialogoAlerta.close();
            } catch (NumberFormatException e) {
                mostrarError("Valor inválido. Ingrese un número válido.");
            }
        }));

        mainPanel.addComponent(new Button("Cancelar", dialogoAlerta::close));

        dialogoAlerta.setComponent(mainPanel);
        gui.addWindow(dialogoAlerta);
    }

    // cando se cumple unha alertaç
    public void mostrarAlerta(Alerta alerta) {
        gui.getGUIThread().invokeLater(() -> {
            try {
                MessageDialog.showMessageDialog(gui, "[!] ALERTA ACTIVADA [!]",
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

    public void mostrarMensaje(String titulo, String mensaje) {
        gui.getGUIThread().invokeLater(() -> MessageDialog.showMessageDialog(gui, titulo, mensaje, MessageDialogButton.OK));
    }

    public void mostrarError(String mensaje) {
        gui.getGUIThread().invokeLater(() -> MessageDialog.showMessageDialog(gui, "Error", mensaje, MessageDialogButton.OK));
    }

    public void cerrarGUI() {
        try {
            if (screen != null) {
                screen.stopScreen();
            }
        } catch (Exception e) {
            System.err.println("Error cerrando GUI: " + e.getMessage());
        }
    }
}