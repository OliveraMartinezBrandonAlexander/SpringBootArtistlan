package com.example.demo.repository;

import com.example.demo.model.CategoriaObras;
import com.example.demo.model.CategoriaObrasID;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaObrasRepository extends JpaRepository<CategoriaObras, CategoriaObrasID>
{


}