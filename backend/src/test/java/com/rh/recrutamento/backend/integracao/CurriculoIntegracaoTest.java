package com.rh.recrutamento.backend.integracao;

import com.rh.recrutamento.backend.entity.Usuario;
import com.rh.recrutamento.backend.repository.CurriculoRepository;
import com.rh.recrutamento.backend.repository.UsuarioRepository;
import com.rh.recrutamento.backend.service.TokenService;
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

    @Autowired
    private TokenService tokenService;

    @Test
    void deveCriarPrimeiroCurriculoDoCandidatoEDevolverDadosCompletos() throws Exception {
        Long usuarioId = novoCandidato("ana.integracao@teste.com");

        String corpo = """
            {
             "dataNascimento":"1998-04-12","sexo":"feminino","cidade":"Campinas","uf":"SP",
             "numeroContato":"19999990000","perfilLinkedin":"https://linkedin.com/in/ana",
             "competencias":"Java, SQL","certificacoes":"AWS Cloud Practitioner","resumo":"Resumo",
             "formacoes":[{"curso":"ADS","instituicao":"Fatec","dataInicio":"2020-02-01","dataTermino":"2023-12-15"}],
             "experiencias":[{"cargo":"Dev","empresa":"Acme","dataContratacao":"2023-01-10",
                              "dataDemissao":"2024-05-30","trabalhoAtual":true,"descricaoAtividades":"APIs"}]}
            """;

        mockMvc.perform(post("/curriculos").header("Authorization", tokenDe(usuarioId)).contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.usuarioId").value(usuarioId))
            .andExpect(jsonPath("$.idade").isNumber())
            .andExpect(jsonPath("$.formacoes[0].instituicao").value("Fatec"))
            // trabalhoAtual = true: a data de demissao enviada e descartada
            .andExpect(jsonPath("$.experiencias[0].dataDemissao").doesNotExist());

        mockMvc.perform(get("/curriculos/usuario/" + usuarioId).header("Authorization", tokenDe(usuarioId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cidade").value("Campinas"))
            .andExpect(jsonPath("$.experiencias[0].trabalhoAtual").value(true));
    }

    @Test
    void deveRecusarSegundoCurriculoDoMesmoCandidato() throws Exception {
        Long usuarioId = novoCandidato("bruno.integracao@teste.com");
        String corpo = """
            {"resumo":"Primeiro"}
            """;

        mockMvc.perform(post("/curriculos").header("Authorization", tokenDe(usuarioId)).contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/curriculos").header("Authorization", tokenDe(usuarioId)).contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isConflict());
    }

    @Test
    void deveAnexarEBaixarOPdfDoCurriculo() throws Exception {
        Long usuarioId = novoCandidato("carla.integracao@teste.com");
        mockMvc.perform(post("/curriculos").header("Authorization", tokenDe(usuarioId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"resumo":"Com anexo"}
                    """))
            .andExpect(status().isCreated());
        Long id = idDoCurriculoDe(usuarioId);

        mockMvc.perform(multipart("/curriculos/" + id + "/arquivo").header("Authorization", tokenDe(usuarioId))
                .file(new MockMultipartFile("arquivo", "carla.pdf", "application/pdf", "%PDF-1.4 conteudo".getBytes())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.arquivo.nomeOriginal").value("carla.pdf"));

        mockMvc.perform(get("/curriculos/" + id + "/arquivo").header("Authorization", tokenDe(usuarioId)))
            .andExpect(status().isOk())
            .andExpect(content().bytes("%PDF-1.4 conteudo".getBytes()));
    }

    /** Regressao: substituir o PDF violava uk_arquivo_curriculo (insert antes do delete) e virava 500. */
    @Test
    void deveSubstituirOPdfJaAnexadoAoCurriculo() throws Exception {
        Long usuarioId = novoCandidato("elisa.integracao@teste.com");
        mockMvc.perform(post("/curriculos").header("Authorization", tokenDe(usuarioId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {}
                    """))
            .andExpect(status().isCreated());
        Long id = idDoCurriculoDe(usuarioId);

        mockMvc.perform(multipart("/curriculos/" + id + "/arquivo").header("Authorization", tokenDe(usuarioId))
                .file(new MockMultipartFile("arquivo", "v1.pdf", "application/pdf", "%PDF-1.4 versao 1".getBytes())))
            .andExpect(status().isOk());

        mockMvc.perform(multipart("/curriculos/" + id + "/arquivo").header("Authorization", tokenDe(usuarioId))
                .file(new MockMultipartFile("arquivo", "v2.pdf", "application/pdf", "%PDF-1.4 versao 2".getBytes())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.arquivo.nomeOriginal").value("v2.pdf"));

        mockMvc.perform(get("/curriculos/" + id + "/arquivo").header("Authorization", tokenDe(usuarioId)))
            .andExpect(status().isOk())
            .andExpect(content().bytes("%PDF-1.4 versao 2".getBytes()));
    }

    /** Regressao: rota inexistente caia no handler generico e respondia 500 em vez de 404. */
    @Test
    void deveResponder404ParaRotaInexistente() throws Exception {
        Long usuarioId = novoCandidato("fabio.integracao@teste.com");
        mockMvc.perform(get("/rota-que-nao-existe").header("Authorization", tokenDe(usuarioId)))
            .andExpect(status().isNotFound());
    }

    @Test
    void deveRecusarArquivoQueNaoEhPdf() throws Exception {
        Long usuarioId = novoCandidato("diego.integracao@teste.com");
        mockMvc.perform(post("/curriculos").header("Authorization", tokenDe(usuarioId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {}
                    """))
            .andExpect(status().isCreated());
        Long id = idDoCurriculoDe(usuarioId);

        mockMvc.perform(multipart("/curriculos/" + id + "/arquivo").header("Authorization", tokenDe(usuarioId))
                .file(new MockMultipartFile("arquivo", "foto.png", "image/png", "png".getBytes())))
            .andExpect(status().isBadRequest());
    }

    private String tokenDe(Long usuarioId) {
        return "Bearer " + tokenService.gerar(usuarioRepository.findById(usuarioId).orElseThrow());
    }

    private Long idDoCurriculoDe(Long usuarioId) {
        return curriculoRepository.findByUsuario_Id(usuarioId).orElseThrow().getId();
    }

    private Long novoCandidato(String email) {
        return usuarioRepository.save(new Usuario(
            "Candidato Teste", email, "hash", Usuario.Perfil.candidato, Usuario.Status.ativo)).getId();
    }
}
