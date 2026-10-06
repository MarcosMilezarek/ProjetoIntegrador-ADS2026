package com.rh.recrutamento.backend.comum.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Senha com no maximo 72 bytes em UTF-8, o limite do BCrypt. Valor nulo e valido (outras anotacoes cuidam disso). */
@Documented
@Constraint(validatedBy = SenhaCompativelValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface SenhaCompativel {

    String message() default "A senha deve ter no máximo 72 caracteres.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
