package mg.miniframework.ui.type;

import java.util.Arrays;

public enum HtmlInputType {
    // Les valeurs correspondent aux attributs "type" standards de HTML5
    TEXT("text"),
    DATE("date"),
    DATETIME("datetime-local"),
    NUMBER("number"),
    PASSWORD("password"),
    EMAIL("email"),
    CHECKBOX("checkbox");

    private final String value;

    // Constructeur
    HtmlInputType(String value) {
        this.value = value;
    }

    // Getter pour récupérer la valeur à insérer dans le HTML
    public String getValue() {
        return value;
    }

    /**
     * Permet de retrouver l'Enum à partir d'une chaîne de caractères.
     * Utile pour la désérialisation ou les formulaires.
     */
    public static HtmlInputType fromString(String text) {
        return Arrays.stream(HtmlInputType.values())
                .filter(type -> type.value.equalsIgnoreCase(text))
                .findFirst()
                .orElse(TEXT); // Valeur par défaut
    }

    @Override
    public String toString() {
        return this.value;
    }
}