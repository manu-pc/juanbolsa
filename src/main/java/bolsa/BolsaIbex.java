package bolsa;

import java.time.LocalDateTime;
import java.time.DayOfWeek;

public class BolsaIbex {
    public static int numAcciones = 35;
    public static boolean mercadoAbierto() {
        // devuelve true si es entre semana y entre las 9 y las 17
        DayOfWeek dia = LocalDateTime.now().getDayOfWeek();
        if (dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY) {
            return false;
        }

        int hora = LocalDateTime.now().getHour();

        return (hora > 9 && hora < 17);
    }
}