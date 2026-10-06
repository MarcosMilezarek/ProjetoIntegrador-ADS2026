package com.rh.recrutamento.backend.comum.service;

import com.rh.recrutamento.backend.comum.exception.ArquivoInvalidoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArquivoStorageTest {

    @TempDir
    Path raiz;

    private ArquivoStorage storage() {
        return new ArquivoStorage(raiz.toString());
    }

    private static MockMultipartFile arquivo(String contentType, byte[] conteudo) {
        return new MockMultipartFile("arquivo", "enviado", contentType, conteudo);
    }

    @Test
    void gravaPdfComAssinaturaValidaComNomeGeradoPeloServidor() {
        String nome = storage().salvar("documento", arquivo("application/pdf", "%PDF-1.4 conteudo".getBytes()), "pdf");

        assertThat(nome).endsWith(".pdf").isNotEqualTo("enviado");
        assertThat(Files.isRegularFile(raiz.resolve("documento").resolve(nome))).isTrue();
    }

    @Test
    void gravaDocxComAssinaturaDeZip() {
        byte[] zip = {'P', 'K', 3, 4, 0, 0};

        String nome = storage().salvar("documento", arquivo("application/octet-stream", zip), "docx");

        assertThat(nome).endsWith(".docx");
    }

    @Test
    void recusaTextoDeclaradoComoPdfSemGravarNada() {
        assertThatThrownBy(() -> storage().salvar("documento", arquivo("application/pdf", "<html>nao e pdf</html>".getBytes()), "pdf"))
            .isInstanceOf(ArquivoInvalidoException.class)
            .hasMessageContaining("PDF");

        assertThat(Files.exists(raiz.resolve("documento"))).isFalse();
    }

    @Test
    void recusaPdfDeclaradoComoDocx() {
        assertThatThrownBy(() -> storage().salvar("documento", arquivo("application/octet-stream", "%PDF-1.4".getBytes()), "docx"))
            .isInstanceOf(ArquivoInvalidoException.class)
            .hasMessageContaining("DOCX");
    }

    @Test
    void recusaArquivoMenorQueAAssinatura() {
        assertThatThrownBy(() -> storage().salvar("documento", arquivo("application/pdf", "%PD".getBytes()), "pdf"))
            .isInstanceOf(ArquivoInvalidoException.class);
    }
}
