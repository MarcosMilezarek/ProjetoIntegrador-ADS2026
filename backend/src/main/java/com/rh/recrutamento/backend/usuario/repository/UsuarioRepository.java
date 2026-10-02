package com.rh.recrutamento.backend.usuario.repository;

import com.rh.recrutamento.backend.usuario.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    List<Usuario> findByPerfilAndStatus(Usuario.Perfil perfil, Usuario.Status status);
}
