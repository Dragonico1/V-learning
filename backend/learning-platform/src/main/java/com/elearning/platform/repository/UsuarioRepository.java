package com.elearning.platform.repository;

import com.elearning.platform.entity.Usuario;
import com.elearning.platform.enums.EstadoUsuario;
import com.elearning.platform.enums.RolUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

    Optional<Usuario> findByCorreoInstitucionalIgnoreCase(String correo);

    boolean existsByCorreoInstitucionalIgnoreCase(String correo);

    List<Usuario> findByRolAndEstado(RolUsuario rol, EstadoUsuario estado);

    List<Usuario> findByRol(RolUsuario rol);
}
