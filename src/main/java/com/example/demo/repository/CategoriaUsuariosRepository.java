package com.example.demo.repository;
import com.example.demo.model.CategoriaUsuarios;
import com.example.demo.model.CategoriaUsuariosID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaUsuariosRepository extends JpaRepository<CategoriaUsuarios, CategoriaUsuariosID>
{
}