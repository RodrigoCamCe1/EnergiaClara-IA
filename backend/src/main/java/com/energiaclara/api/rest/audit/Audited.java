package com.energiaclara.api.rest.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {
    String action();
    String entity();
    /** SpEL opcional para extraer el id de la entidad desde args o resultado. Ej: "#id" o "#result.userId" */
    String entityIdExpression() default "";
}