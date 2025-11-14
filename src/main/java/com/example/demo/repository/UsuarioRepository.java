package com.example.demo.repository;

import com.example.demo.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

// tiene acceso a la base de datos usando JPA

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    // Métodos personalizados aquí
}