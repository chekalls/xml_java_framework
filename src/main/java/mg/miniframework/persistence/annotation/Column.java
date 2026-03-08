package mg.miniframework.persistence.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Column{   
    String name() default "";
    boolean ignore() default false;
    boolean nullable() default true;
    /**
     * Indique que la colonne est de type JSONB. Si vrai, les valeurs String seront envoyées
     * au driver en tant que jsonb; lors de la lecture, si le champ Java est String la valeur
     * JSON brute est renvoyée, sinon OBJECT_MAPPER tentera de convertir en le type Java.
     */
    boolean jsonb() default false;
}