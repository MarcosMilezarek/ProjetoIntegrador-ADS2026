package com.rh.recrutamento.backend.integracao;

import com.rh.recrutamento.backend.usuario.entity.Usuario;
import com.rh.recrutamento.backend.usuario.repository.UsuarioRepository;
import com.rh.recrutamento.backend.vaga.entity.Vaga;
import com.rh.recrutamento.backend.vaga.repository.VagaRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Fluxo real de autenticacao: login devolve o JWT, e o JWT decide papel e propriedade. */
@SpringBootTest
@AutoConfigureMockMvc
class SegurancaIntegracaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private VagaRepository vagaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void loginPorPapelDevolveTokenComOPerfil() throws Exception {
        for (Usuario.Perfil perfil : Usuario.Perfil.values()) {
            String email = "login." + perfil + "@seguranca.test";
            novoUsuario(email, perfil);

            mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"%s\",\"senha\":\"senha123\"}".formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value(perfil.name()))
                .andExpect(jsonPath("$.token").isString());
        }
    }

    @Test
    void rotaProtegidaSemTokenOuComTokenInvalidoRetorna401() throws Exception {
        mockMvc.perform(get("/vagas"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.mensagem").exists());
        mockMvc.perform(get("/vagas").header("Authorization", "Bearer token.forjado.invalido"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void candidatoNaoLeCurriculoDeOutroCandidato() throws Exception {
        String ana = login(novoUsuario("ana@seguranca.test", Usuario.Perfil.candidato));
        Usuario bruno = novoUsuario("bruno@seguranca.test", Usuario.Perfil.candidato);
        String tokenBruno = login(bruno);

        mockMvc.perform(post("/curriculos").header("Authorization", tokenBruno)
                .contentType(MediaType.APPLICATION_JSON).content("{\"resumo\":\"Bruno\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/curriculos/usuario/" + bruno.getId()).header("Authorization", ana))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/curriculos/usuario/" + bruno.getId()).header("Authorization", tokenBruno))
            .andExpect(status().isOk());
    }

    @Test
    void configuracoesSaoNegadasAoRhESoOAdministradorCadastraRh() throws Exception {
        String rh = login(novoUsuario("rh@seguranca.test", Usuario.Perfil.rh));
        String admin = login(novoUsuario("admin@seguranca.test", Usuario.Perfil.administrador));
        String novoRh = """
            {"nome":"Novo RH","email":"%s","senha":"senha123","perfil":"rh"}
            """;

        mockMvc.perform(get("/usuarios").header("Authorization", rh))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/usuarios").header("Authorization", admin))
            .andExpect(status().isOk());

        // cadastro publico nao cria RH, nem com token de RH
        mockMvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON)
                .content(novoRh.formatted("intruso@seguranca.test")))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/usuarios").header("Authorization", rh).contentType(MediaType.APPLICATION_JSON)
                .content(novoRh.formatted("intruso2@seguranca.test")))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/usuarios").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content(novoRh.formatted("contratado@seguranca.test")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.perfil").value("rh"));
    }

    @Test
    void rhNaoAlteraVagaDeOutroRh() throws Exception {
        Usuario rita = novoUsuario("rita@seguranca.test", Usuario.Perfil.rh);
        String paulo = login(novoUsuario("paulo@seguranca.test", Usuario.Perfil.rh));
        Vaga vagaDaRita = vagaRepository.save(new Vaga(rita, "Vaga da Rita", "Descricao", null, null,
            Vaga.Modalidade.remoto, Vaga.TipoContrato.clt, Vaga.Status.aberta, null));
        String corpo = """
            {"titulo":"Tomada","descricao":"x","modalidade":"remoto","tipoContrato":"clt","status":"encerrada"}
            """;

        mockMvc.perform(put("/vagas/" + vagaDaRita.getId()).header("Authorization", paulo)
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isForbidden());
        mockMvc.perform(put("/vagas/" + vagaDaRita.getId()).header("Authorization", login(rita))
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isOk());
    }

    private Usuario novoUsuario(String email, Usuario.Perfil perfil) {
        return usuarioRepository.save(new Usuario(
            "Teste " + perfil, email, passwordEncoder.encode("senha123"), perfil, Usuario.Status.ativo));
    }

    /** Faz o login de verdade e devolve o cabecalho Authorization pronto. */
    private String login(Usuario usuario) throws Exception {
        String resposta = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"senha\":\"senha123\"}".formatted(usuario.getEmail())))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(resposta, "$.token");
    }
}
