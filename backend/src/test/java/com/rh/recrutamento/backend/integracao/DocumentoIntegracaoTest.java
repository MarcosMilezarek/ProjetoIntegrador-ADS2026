package com.rh.recrutamento.backend.integracao;

import com.rh.recrutamento.backend.entity.Candidatura;
import com.rh.recrutamento.backend.entity.Usuario;
import com.rh.recrutamento.backend.entity.Vaga;
import com.rh.recrutamento.backend.repository.CandidaturaRepository;
import com.rh.recrutamento.backend.repository.UsuarioRepository;
import com.rh.recrutamento.backend.repository.VagaRepository;
import com.rh.recrutamento.backend.service.TokenService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Documentos separados por candidato, com upload e download reais em disco. */
@SpringBootTest
@AutoConfigureMockMvc
class DocumentoIntegracaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private VagaRepository vagaRepository;

    @Autowired
    private CandidaturaRepository candidaturaRepository;

    @Autowired
    private TokenService tokenService;

    @Test
    void cadaCandidatoVeSoOsSeusEORhVeOsDasSuasVagas() throws Exception {
        Usuario rita = usuario("rita@documento.test", Usuario.Perfil.rh);
        Usuario paulo = usuario("paulo@documento.test", Usuario.Perfil.rh);
        Usuario ana = usuario("ana@documento.test", Usuario.Perfil.candidato);
        Usuario bruno = usuario("bruno@documento.test", Usuario.Perfil.candidato);
        Vaga vaga = vagaRepository.save(new Vaga(rita, "Backend Java", "Descricao", null, null,
            Vaga.Modalidade.remoto, Vaga.TipoContrato.clt, Vaga.Status.aberta, null));
        Candidatura daAna = candidatura(ana, vaga, Candidatura.Status.aprovado);
        Candidatura doBruno = candidatura(bruno, vaga, Candidatura.Status.entrevista);
        MockMultipartFile pdf = new MockMultipartFile("arquivo", "rg.pdf", "application/pdf", "%PDF-1.4 rg".getBytes());

        // RN03: so depois da aprovacao
        mockMvc.perform(multipart("/candidaturas/" + doBruno.getId() + "/documentos").file(pdf).param("tipo", "RG")
                .header("Authorization", token(bruno)))
            .andExpect(status().isConflict());
        // nao envia na candidatura de outra pessoa; RH nao envia
        mockMvc.perform(multipart("/candidaturas/" + daAna.getId() + "/documentos").file(pdf).param("tipo", "RG")
                .header("Authorization", token(bruno)))
            .andExpect(status().isForbidden());
        mockMvc.perform(multipart("/candidaturas/" + daAna.getId() + "/documentos").file(pdf).param("tipo", "RG")
                .header("Authorization", token(rita)))
            .andExpect(status().isForbidden());
        // sem o campo tipo: 400, nao 500
        mockMvc.perform(multipart("/candidaturas/" + daAna.getId() + "/documentos").file(pdf)
                .header("Authorization", token(ana)))
            .andExpect(status().isBadRequest());

        String enviado = mockMvc.perform(multipart("/candidaturas/" + daAna.getId() + "/documentos").file(pdf)
                .param("tipo", "RG").header("Authorization", token(ana)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.candidatoId").value(ana.getId()))
            .andReturn().getResponse().getContentAsString();
        Integer documentoId = JsonPath.read(enviado, "$.id");

        // candidato: so os proprios
        mockMvc.perform(get("/documentos").header("Authorization", token(ana)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].tipo").value("RG"));
        mockMvc.perform(get("/documentos").header("Authorization", token(bruno)))
            .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/documentos").param("candidatoId", ana.getId().toString()).header("Authorization", token(bruno)))
            .andExpect(status().isForbidden());

        // RH: os documentos dos candidatos das suas vagas, por candidato
        mockMvc.perform(get("/documentos").param("candidatoId", ana.getId().toString()).header("Authorization", token(rita)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].candidatoNome").value(ana.getNome()));
        mockMvc.perform(get("/documentos").header("Authorization", token(paulo)))
            .andExpect(jsonPath("$.length()").value(0));

        // download: dono e RH da vaga; outro candidato e outro RH nao
        mockMvc.perform(get("/documentos/" + documentoId + "/arquivo").header("Authorization", token(rita)))
            .andExpect(status().isOk())
            .andExpect(content().bytes("%PDF-1.4 rg".getBytes()));
        mockMvc.perform(get("/documentos/" + documentoId + "/arquivo").header("Authorization", token(ana)))
            .andExpect(status().isOk());
        mockMvc.perform(get("/documentos/" + documentoId + "/arquivo").header("Authorization", token(bruno)))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/documentos/" + documentoId + "/arquivo").header("Authorization", token(paulo)))
            .andExpect(status().isForbidden());
    }

    private Candidatura candidatura(Usuario candidato, Vaga vaga, Candidatura.Status status) {
        Candidatura candidatura = new Candidatura(candidato, vaga);
        candidatura.alterarStatus(status);
        return candidaturaRepository.save(candidatura);
    }

    private Usuario usuario(String email, Usuario.Perfil perfil) {
        return usuarioRepository.save(new Usuario(email.substring(0, email.indexOf('@')), email, "hash", perfil,
            Usuario.Status.ativo));
    }

    private String token(Usuario usuario) {
        return "Bearer " + tokenService.gerar(usuario);
    }
}
