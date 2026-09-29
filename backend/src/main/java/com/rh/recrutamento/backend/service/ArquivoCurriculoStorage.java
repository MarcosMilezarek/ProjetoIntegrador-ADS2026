package com.rh.recrutamento.backend.service;

import com.rh.recrutamento.backend.exception.ArquivoInvalidoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** Guarda o PDF do curriculo em disco; o banco so referencia o nome gerado. */
@Component
public class ArquivoCurriculoStorage {

    static final long TAMANHO_MAXIMO_BYTES = 5L * 1024 * 1024;
    private static final String CONTENT_TYPE_PDF = "application/pdf";

    private final Path diretorio;

    public ArquivoCurriculoStorage(@Value("${app.upload.dir}") String uploadDir) {
        this.diretorio = Path.of(uploadDir, "curriculo").toAbsolutePath().normalize();
    }

    /** Valida e grava o arquivo, devolvendo o nome gerado (nunca o nome enviado pelo cliente). */
    public String salvar(MultipartFile arquivo) {
        validar(arquivo);
        String nomeArmazenado = UUID.randomUUID() + ".pdf";
        try {
            Files.createDirectories(diretorio);
            try (var entrada = arquivo.getInputStream()) {
                Files.copy(entrada, diretorio.resolve(nomeArmazenado), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gravar o arquivo do curriculo.", e);
        }
        return nomeArmazenado;
    }

    public byte[] ler(String nomeArmazenado) {
        try {
            return Files.readAllBytes(caminhoDe(nomeArmazenado));
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler o arquivo do curriculo.", e);
        }
    }

    public void remover(String nomeArmazenado) {
        try {
            Files.deleteIfExists(caminhoDe(nomeArmazenado));
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao remover o arquivo do curriculo.", e);
        }
    }

    /** Resolve dentro do diretorio configurado e recusa qualquer nome que tente escapar dele. */
    private Path caminhoDe(String nomeArmazenado) {
        Path caminho = diretorio.resolve(nomeArmazenado).normalize();
        if (!caminho.startsWith(diretorio)) {
            throw new ArquivoInvalidoException("Nome de arquivo invalido.");
        }
        return caminho;
    }

    private void validar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ArquivoInvalidoException("Envie um arquivo PDF.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw new ArquivoInvalidoException("O arquivo deve ter no maximo 5MB.");
        }
        if (!CONTENT_TYPE_PDF.equalsIgnoreCase(arquivo.getContentType())) {
            throw new ArquivoInvalidoException("Somente arquivos PDF sao aceitos.");
        }
    }
}
