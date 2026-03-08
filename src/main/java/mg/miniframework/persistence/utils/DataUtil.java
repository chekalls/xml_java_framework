package mg.miniframework.persistence.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class DataUtil {
    private static String dateTimePattern = "yyyy-MM-dd HH:mm:ss";
    private static String datePattern = "yyyy-MM-dd";

    public static LocalDate convertStringToDate(String dateString, String pattern) {
        if (dateString == null || dateString.isEmpty() || pattern == null || pattern.isEmpty()) {
            return null;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return LocalDate.parse(dateString, formatter);
        } catch (DateTimeParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static LocalDate convertStringToDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(datePattern);
            return LocalDate.parse(dateString, formatter);
        } catch (DateTimeParseException e) {
            try {
                DateTimeFormatter altFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                return LocalDate.parse(dateString, altFormatter);
            } catch (DateTimeParseException ex) {
                e.printStackTrace();
                return null;
            }
        }
    }

    public static LocalDateTime convertStringToDateTime(String dateString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateTimePattern);
        return LocalDateTime.parse(dateString, formatter);
    }

    public static LocalDateTime convertStringToDateTime(String dateString, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return LocalDateTime.parse(dateString, formatter);
    }

    public static String createSearchCondition(String columnName, String[] keywords) {
        if (keywords == null || keywords.length == 0) {
            return "";
        }
        StringBuilder condition = new StringBuilder();
        for (int i = 0; i < keywords.length; i++) {
            if (i > 0) {
                condition.append(" OR ");
            }
            condition.append(columnName).append(" ILIKE ?");
        }
        return condition.toString();
    }

    /**
     * Crée une condition WHERE pour chercher dans plusieurs colonnes
     * 
     * @param columnNames Les noms de colonnes (ex: "id", "reference_type")
     * @return La clause WHERE (ex: "id ILIKE ? OR reference_type ILIKE ?")
     */
    public static String createMultiColumnSearchCondition(String... columnNames) {
        if (columnNames == null || columnNames.length == 0) {
            return "";
        }
        StringBuilder condition = new StringBuilder();
        for (int i = 0; i < columnNames.length; i++) {
            if (i > 0) {
                condition.append(" OR ");
            }
            condition.append(columnNames[i]).append(" ILIKE ?");
        }
        return condition.toString();
    }

    /**
     * Convertit un LocalDateTime en un tableau de chaînes :
     * [0] = date au format français 'dd/MM/yyyy'
     * [1] = heure au format 'HH:mm:ss'
     *
     * @param dateTime l'objet LocalDateTime à convertir
     * @return un tableau de deux chaînes {dateFR, heure}
     */
    public static String[] toFrenchDateAndTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return new String[] { "", "" };
        }
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy").withLocale(Locale.FRANCE);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return new String[] { dateTime.format(dateFormatter), dateTime.format(timeFormatter) };
    }
}
