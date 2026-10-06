package com.rh.recrutamento.backend.comum.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

public class SenhaCompativelValidator implements ConstraintValidator<SenhaCompativel, String> {

    private static final int LIMITE_BCRYPT_BYTES = 72;

    @Override
    public boolean isValid(String senha, ConstraintValidatorContext contexto) {
        return senha == null || senha.getBytes(StandardCharsets.UTF_8).length <= LIMITE_BCRYPT_BYTES;
    }
}
