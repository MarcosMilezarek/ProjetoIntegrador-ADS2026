package com.rh.recrutamento.backend.comum.service;

import com.rh.recrutamento.backend.comum.exception.ArquivoInvalidoException;
import com.rh.recrutamento.backend.comum.exception.RecursoNaoEncontradoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

/**
 * Guarda em disco os arquivos enviados (PDF do curriculo, documentos de contratacao), cada tipo
 * em uma pasta dentro de app.upload.dir; o banco so referencia o nome gerado.
 * Quais formatos cada caso aceita e regra de quem chama.
 */
@Component
public class ArquivoStorage {

    static final long TAMANHO_MAXIMO_BYTES = 5L * 1024 * 1024;

    /** Bytes iniciais de cada formato aceito: PDF abre com "%PDF-" e DOCX e um zip ("PK" seguido de 3 e 4). */
    private static final Map<String, byte[]> ASSINATURAS = Map.of(
        "pdf", "%PDF-".getBytes(StandardCharsets.US_ASCII),
        "docx", new byte[] {'P', 'K', 3, 4});

    private final Path raiz;

    public ArquivoStorage(@Value("${app.upload.dir}") String uploadDir) {
        this.raiz = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    /** Valida e grava o arquivo, devolvendo o nome gerado (nunca o nome enviado pelo cliente). */
    public String salvar(String pasta, MultipartFile arquivo, String extensao) {
        validar(arquivo);
        conferirAssinatura(arquivo, extensao);
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

    /** O Content-Type vem do cliente: os primeiros bytes confirmam que o conteudo e mesmo do formato declarado. */
    private void conferirAssinatura(MultipartFile arquivo, String extensao) {
        byte[] esperada = ASSINATURAS.get(extensao);
        if (esperada == null) {
            return;
        }
        try (var entrada = arquivo.getInputStream()) {
            if (!Arrays.equals(entrada.readNBytes(esperada.length), esperada)) {
                throw new ArquivoInvalidoException("O conteúdo do arquivo não é um " + extensao.toUpperCase()
                    + " válido. Salve o seu arquivo nesse formato e envie novamente.");
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler o arquivo enviado.", e);
        }
    }

    private void validar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ArquivoInvalidoException("Envie um arquivo.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw new ArquivoInvalidoException(
                "O arquivo ultrapassa o limite de 5MB. Reduza o tamanho (por exemplo, comprimindo o PDF) e envie novamente.");
        }
    }
}
