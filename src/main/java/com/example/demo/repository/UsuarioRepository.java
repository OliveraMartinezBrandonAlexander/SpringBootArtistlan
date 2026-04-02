package com.example.demo.repository;

import com.example.demo.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    @Query("SELECT u FROM Usuario u WHERE UPPER(u.rol) = 'ADMIN'")
    List<Usuario> findAdmins();

    boolean existsByUsuario(String usuario);
    boolean existsByCorreo(String correo);

    Optional<Usuario> findByUsuarioAndContrasena(String usuario, String contrasena);
    Optional<Usuario> findByCorreoAndContrasena(String correo, String contrasena);

    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.categoriasUsuarios cu LEFT JOIN FETCH cu.categoria WHERE u.idUsuario = :id")
    Optional<Usuario> findByIdConCategorias(@Param("id") Integer id);

    @Query("SELECT DISTINCT u FROM Usuario u LEFT JOIN FETCH u.categoriasUsuarios cu LEFT JOIN FETCH cu.categoria")
    List<Usuario> findAllConCategorias();
}
