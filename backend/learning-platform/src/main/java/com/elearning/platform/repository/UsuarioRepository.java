package com.elearning.platform.repository;

import com.elearning.platform.entity.Usuario;
import com.elearning.platform.enums.EstadoUsuario;
import com.elearning.platform.enums.RolUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreoInstitucionalIgnoreCase(String correo);

    boolean existsByCorreoInstitucionalIgnoreCase(String correo);

    List<Usuario> findByRolAndEstado(RolUsuario rol, EstadoUsuario estado);

    List<Usuario> findByRol(RolUsuario rol);
}
