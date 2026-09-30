package com.rh.recrutamento.backend.comum.service;

import com.rh.recrutamento.backend.comum.exception.ArquivoInvalidoException;
import com.rh.recrutamento.backend.comum.exception.RecursoNaoEncontradoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Guarda em disco os arquivos enviados (PDF do curriculo, documentos de contratacao), cada tipo
 * em uma pasta dentro de app.upload.dir; o banco so referencia o nome gerado.
 * Quais formatos cada caso aceita e regra de quem chama.
 */
@Component
public class ArquivoStorage {

    static final long TAMANHO_MAXIMO_BYTES = 5L * 1024 * 1024;

    private final Path raiz;

    public ArquivoStorage(@Value("${app.upload.dir}") String uploadDir) {
        this.raiz = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    /** Valida e grava o arquivo, devolvendo o nome gerado (nunca o nome enviado pelo cliente). */
    public String salvar(String pasta, MultipartFile arquivo, String extensao) {
        validar(arquivo);
        String nomeArmazenado = UUID.randomUUID() + "." + extensao;
        try {
            Path diretorio = raiz.resolve(pasta);
            Files.createDirectories(diretorio);
            try (var entrada = arquivo.getInputStream()) {
                Files.copy(entrada, diretorio.resolve(nomeArmazenado), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gravar o arquivo.", e);
        }
        return nomeArmazenado;
    }

    public byte[] ler(String pasta, String nomeArmazenado) {
        Path caminho = caminhoDe(pasta, nomeArmazenado);
        if (!Files.isRegularFile(caminho)) {
            throw new RecursoNaoEncontradoException("Arquivo não encontrado no armazenamento.");
        }
        try {
            return Files.readAllBytes(caminho);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler o arquivo.", e);
        }
    }

    public void remover(String pasta, String nomeArmazenado) {
        try {
            Files.deleteIfExists(caminhoDe(pasta, nomeArmazenado));
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao remover o arquivo.", e);
        }
    }

    /** Resolve dentro da pasta configurada e recusa qualquer nome que tente escapar dela. */
    private Path caminhoDe(String pasta, String nomeArmazenado) {
        Path diretorio = raiz.resolve(pasta);
        Path caminho;
        try {
            caminho = diretorio.resolve(nomeArmazenado).normalize();
        } catch (InvalidPathException e) {
            throw new ArquivoInvalidoException("Nome de arquivo invalido.");
        }
        if (!caminho.startsWith(diretorio)) {
            throw new ArquivoInvalidoException("Nome de arquivo invalido.");
        }
        return caminho;
    }

    private void validar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ArquivoInvalidoException("Envie um arquivo.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw new ArquivoInvalidoException("O arquivo deve ter no maximo 5MB.");
        }
    }
}
