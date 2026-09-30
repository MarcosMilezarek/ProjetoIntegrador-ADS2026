package com.rh.recrutamento.backend.integracao;

import com.rh.recrutamento.backend.entity.Candidatura;
import com.rh.recrutamento.backend.entity.HistoricoStatus;
import com.rh.recrutamento.backend.entity.Usuario;
import com.rh.recrutamento.backend.entity.Vaga;
import com.rh.recrutamento.backend.repository.HistoricoStatusRepository;
import com.rh.recrutamento.backend.repository.UsuarioRepository;
import com.rh.recrutamento.backend.repository.VagaRepository;
import com.rh.recrutamento.backend.service.TokenService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Painel do RH ponta a ponta: candidato se inscreve, RH ve inscritos, muda a etapa e le o curriculo. */
@SpringBootTest
@AutoConfigureMockMvc
class PainelRhIntegracaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private VagaRepository vagaRepository;

    @Autowired
    private HistoricoStatusRepository historicoRepository;

    @Autowired
    private TokenService tokenService;

    @Test
    void fluxoDoProcessoSeletivo() throws Exception {
        Usuario rita = usuario("rita@painel.test", Usuario.Perfil.rh);
        Usuario paulo = usuario("paulo@painel.test", Usuario.Perfil.rh);
        Usuario ana = usuario("ana@painel.test", Usuario.Perfil.candidato);
        Usuario bruno = usuario("bruno@painel.test", Usuario.Perfil.candidato);
        Vaga vaga = vagaRepository.save(new Vaga(rita, "Backend Java", "Descricao", null, null,
            Vaga.Modalidade.remoto, Vaga.TipoContrato.clt, Vaga.Status.aberta, null));

        // sem curriculo nao se candidata (UC04)
        mockMvc.perform(post("/candidaturas").header("Authorization", token(ana))
                .contentType(MediaType.APPLICATION_JSON).content("{\"vagaId\":" + vaga.getId() + "}"))
            .andExpect(status().isConflict());

        mockMvc.perform(post("/curriculos").header("Authorization", token(ana))
                .contentType(MediaType.APPLICATION_JSON).content("{\"resumo\":\"Ana\"}"))
            .andExpect(status().isCreated());
        String inscricao = mockMvc.perform(post("/candidaturas").header("Authorization", token(ana))
                .contentType(MediaType.APPLICATION_JSON).content("{\"vagaId\":" + vaga.getId() + "}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("inscrito"))
            .andReturn().getResponse().getContentAsString();
        Integer candidaturaId = JsonPath.read(inscricao, "$.id");

        // RN01: segunda inscricao na mesma vaga
        mockMvc.perform(post("/candidaturas").header("Authorization", token(ana))
                .contentType(MediaType.APPLICATION_JSON).content("{\"vagaId\":" + vaga.getId() + "}"))
            .andExpect(status().isConflict());

        // inscritos: so a responsavel pela vaga
        mockMvc.perform(get("/vagas/" + vaga.getId() + "/candidaturas").header("Authorization", token(rita)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].candidatoNome").value(ana.getNome()));
        mockMvc.perform(get("/vagas/" + vaga.getId() + "/candidaturas").header("Authorization", token(paulo)))
            .andExpect(status().isForbidden());

        // curriculo do inscrito: a responsavel le; outro RH e outro candidato nao
        mockMvc.perform(get("/curriculos/usuario/" + ana.getId()).header("Authorization", token(rita)))
            .andExpect(status().isOk());
        mockMvc.perform(get("/curriculos/usuario/" + ana.getId()).header("Authorization", token(paulo)))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/curriculos/usuario/" + ana.getId()).header("Authorization", token(bruno)))
            .andExpect(status().isForbidden());

        // mudanca de etapa: outro RH nao consegue; a responsavel sim, e fica no historico
        String entrevista = "{\"status\":\"entrevista\",\"observacao\":\"Entrevista tecnica\"}";
        mockMvc.perform(put("/candidaturas/" + candidaturaId + "/status").header("Authorization", token(paulo))
                .contentType(MediaType.APPLICATION_JSON).content(entrevista))
            .andExpect(status().isForbidden());
        mockMvc.perform(put("/candidaturas/" + candidaturaId + "/status").header("Authorization", token(rita))
                .contentType(MediaType.APPLICATION_JSON).content(entrevista))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("entrevista"));

        assertThat(historicoRepository.findAll())
            .filteredOn(h -> h.getCandidatura().getId().equals(candidaturaId.longValue()))
            .extracting(HistoricoStatus::getStatusNovo)
            .containsExactly(Candidatura.Status.inscrito, Candidatura.Status.entrevista);

        // o candidato acompanha a propria candidatura; o outro candidato nao a ve
        mockMvc.perform(get("/candidaturas/minhas").header("Authorization", token(ana)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].status").value("entrevista"));
        mockMvc.perform(get("/candidaturas/minhas").header("Authorization", token(bruno)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    private Usuario usuario(String email, Usuario.Perfil perfil) {
        return usuarioRepository.save(new Usuario(email.substring(0, email.indexOf('@')), email, "hash", perfil,
            Usuario.Status.ativo));
    }

    private String token(Usuario usuario) {
        return "Bearer " + tokenService.gerar(usuario);
    }
}
