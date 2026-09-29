package com.rh.recrutamento.backend.integracao;

import com.rh.recrutamento.backend.entity.Usuario;
import com.rh.recrutamento.backend.repository.CurriculoRepository;
import com.rh.recrutamento.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Fluxo real de cadastro de curriculo, sem mocks (repositorios e banco de verdade).
 * Regressao: antes o primeiro curriculo de um candidato quebrava com 500
 * (AssertionFailure "null identifier" ao salvar a entidade Candidato via merge).
 */
@SpringBootTest
@AutoConfigureMockMvc
class CurriculoIntegracaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CurriculoRepository curriculoRepository;

    @Test
    void deveCriarPrimeiroCurriculoDoCandidatoEDevolverDadosCompletos() throws Exception {
        Long usuarioId = novoCandidato("ana.integracao@teste.com");

        String corpo = """
            {"usuarioId":%d,
             "dataNascimento":"1998-04-12","sexo":"feminino","cidade":"Campinas","uf":"SP",
             "numeroContato":"19999990000","perfilLinkedin":"https://linkedin.com/in/ana",
             "competencias":"Java, SQL","certificacoes":"AWS Cloud Practitioner","resumo":"Resumo",
             "formacoes":[{"curso":"ADS","instituicao":"Fatec","dataInicio":"2020-02-01","dataTermino":"2023-12-15"}],
             "experiencias":[{"cargo":"Dev","empresa":"Acme","dataContratacao":"2023-01-10",
                              "dataDemissao":"2024-05-30","trabalhoAtual":true,"descricaoAtividades":"APIs"}]}
            """.formatted(usuarioId);

        mockMvc.perform(post("/curriculos").contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.usuarioId").value(usuarioId))
            .andExpect(jsonPath("$.idade").isNumber())
            .andExpect(jsonPath("$.formacoes[0].instituicao").value("Fatec"))
            // trabalhoAtual = true: a data de demissao enviada e descartada
            .andExpect(jsonPath("$.experiencias[0].dataDemissao").doesNotExist());

        mockMvc.perform(get("/curriculos/usuario/" + usuarioId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cidade").value("Campinas"))
            .andExpect(jsonPath("$.experiencias[0].trabalhoAtual").value(true));
    }

    @Test
    void deveRecusarSegundoCurriculoDoMesmoCandidato() throws Exception {
        Long usuarioId = novoCandidato("bruno.integracao@teste.com");
        String corpo = """
            {"usuarioId":%d,"resumo":"Primeiro"}
            """.formatted(usuarioId);

        mockMvc.perform(post("/curriculos").contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/curriculos").contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isConflict());
    }

    @Test
    void deveAnexarEBaixarOPdfDoCurriculo() throws Exception {
        Long usuarioId = novoCandidato("carla.integracao@teste.com");
        mockMvc.perform(post("/curriculos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"usuarioId":%d,"resumo":"Com anexo"}
                    """.formatted(usuarioId)))
            .andExpect(status().isCreated());
        Long id = idDoCurriculoDe(usuarioId);

        mockMvc.perform(multipart("/curriculos/" + id + "/arquivo")
                .file(new MockMultipartFile("arquivo", "carla.pdf", "application/pdf", "%PDF-1.4 conteudo".getBytes())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.arquivo.nomeOriginal").value("carla.pdf"));

        mockMvc.perform(get("/curriculos/" + id + "/arquivo"))
            .andExpect(status().isOk())
            .andExpect(content().bytes("%PDF-1.4 conteudo".getBytes()));
    }

    @Test
    void deveRecusarArquivoQueNaoEhPdf() throws Exception {
        Long usuarioId = novoCandidato("diego.integracao@teste.com");
        mockMvc.perform(post("/curriculos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"usuarioId":%d}
                    """.formatted(usuarioId)))
            .andExpect(status().isCreated());
        Long id = idDoCurriculoDe(usuarioId);

        mockMvc.perform(multipart("/curriculos/" + id + "/arquivo")
                .file(new MockMultipartFile("arquivo", "foto.png", "image/png", "png".getBytes())))
            .andExpect(status().isBadRequest());
    }

    private Long idDoCurriculoDe(Long usuarioId) {
        return curriculoRepository.findByUsuario_Id(usuarioId).orElseThrow().getId();
    }

    private Long novoCandidato(String email) {
        return usuarioRepository.save(new Usuario(
            "Candidato Teste", email, "hash", Usuario.Perfil.candidato, Usuario.Status.ativo)).getId();
    }
}
