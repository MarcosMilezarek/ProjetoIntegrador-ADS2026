package com.rh.recrutamento.backend.integracao;

import com.rh.recrutamento.backend.auth.service.TokenService;
import com.rh.recrutamento.backend.candidatura.entity.Candidatura;
import com.rh.recrutamento.backend.candidatura.repository.CandidaturaRepository;
import com.rh.recrutamento.backend.funcionario.entity.Funcionario;
import com.rh.recrutamento.backend.funcionario.repository.FuncionarioRepository;
import com.rh.recrutamento.backend.usuario.entity.Usuario;
import com.rh.recrutamento.backend.usuario.repository.UsuarioRepository;
import com.rh.recrutamento.backend.vaga.entity.Vaga;
import com.rh.recrutamento.backend.vaga.repository.VagaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Funcionarios: so o RH da vaga (ou o administrador) ve e inativa; candidato nunca (RN07). */
@SpringBootTest
@AutoConfigureMockMvc
class FuncionarioIntegracaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private VagaRepository vagaRepository;

    @Autowired
    private CandidaturaRepository candidaturaRepository;

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Autowired
    private TokenService tokenService;

    @Test
    void listaEPerfilSaoSoDoRhDaVagaEDoAdministrador() throws Exception {
        Usuario rita = usuario("rita@funcionario.test", Usuario.Perfil.rh);
        Usuario paulo = usuario("paulo@funcionario.test", Usuario.Perfil.rh);
        Usuario admin = usuario("admin@funcionario.test", Usuario.Perfil.administrador);
        Usuario ana = usuario("ana@funcionario.test", Usuario.Perfil.candidato);
        Funcionario contratada = contratar(ana, rita);

        // candidato nao entra no painel de funcionarios
        mockMvc.perform(get("/funcionarios").header("Authorization", token(ana)))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/funcionarios/" + contratada.getId()).header("Authorization", token(ana)))
            .andExpect(status().isForbidden());

        // lista: cada RH ve so os das suas vagas; o administrador ve todos
        mockMvc.perform(get("/funcionarios").header("Authorization", token(rita)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(contratada.getId()))
            .andExpect(jsonPath("$[0].candidatoNome").value(ana.getNome()))
            .andExpect(jsonPath("$[0].status").value("ativo"));
        mockMvc.perform(get("/funcionarios").header("Authorization", token(paulo)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/funcionarios").header("Authorization", token(admin)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id==%d)]".formatted(contratada.getId())).exists());

        // perfil: RH de outra vaga recebe 403; o dono, o administrador e um id inexistente seguem a regra
        mockMvc.perform(get("/funcionarios/" + contratada.getId()).header("Authorization", token(paulo)))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/funcionarios/" + contratada.getId()).header("Authorization", token(rita)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.funcionario.id").value(contratada.getId()))
            .andExpect(jsonPath("$.documentos").isArray());
        mockMvc.perform(get("/funcionarios/" + contratada.getId()).header("Authorization", token(admin)))
            .andExpect(status().isOk());
        mockMvc.perform(get("/funcionarios/999999").header("Authorization", token(rita)))
            .andExpect(status().isNotFound());
    }

    @Test
    void inativarExigeORhDaVagaOuOAdministrador() throws Exception {
        Usuario rita = usuario("rita2@funcionario.test", Usuario.Perfil.rh);
        Usuario paulo = usuario("paulo2@funcionario.test", Usuario.Perfil.rh);
        Usuario ana = usuario("ana2@funcionario.test", Usuario.Perfil.candidato);
        Funcionario contratada = contratar(ana, rita);
        String caminho = "/funcionarios/" + contratada.getId() + "/inativar";

        mockMvc.perform(put(caminho).header("Authorization", token(ana)))
            .andExpect(status().isForbidden());
        mockMvc.perform(put(caminho).header("Authorization", token(paulo)))
            .andExpect(status().isForbidden());
        assertThat(funcionarioRepository.findById(contratada.getId()).orElseThrow().getStatus())
            .isEqualTo(Funcionario.Status.ativo);

        mockMvc.perform(put(caminho).header("Authorization", token(rita)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("inativo"));
        assertThat(funcionarioRepository.findById(contratada.getId()).orElseThrow().getStatus())
            .isEqualTo(Funcionario.Status.inativo);
    }

    /** Candidatura aprovada e contratada numa vaga do RH, gravada direto: a contratacao em si tem testes proprios. */
    private Funcionario contratar(Usuario candidato, Usuario rh) {
        Vaga vaga = vagaRepository.save(new Vaga(rh, "Vaga de " + rh.getNome(), "Descricao", null, null, null, null,
            Vaga.Modalidade.remoto, Vaga.TipoContrato.clt, Vaga.Status.aberta, null));
        Candidatura candidatura = new Candidatura(candidato, vaga);
        candidatura.alterarStatus(Candidatura.Status.contratado);
        return funcionarioRepository.save(new Funcionario(candidaturaRepository.save(candidatura)));
    }

    private Usuario usuario(String email, Usuario.Perfil perfil) {
        return usuarioRepository.save(new Usuario(email.substring(0, email.indexOf('@')), email, "hash", perfil,
            Usuario.Status.ativo));
    }

    private String token(Usuario usuario) {
        return "Bearer " + tokenService.gerar(usuario);
    }
}
