package com.example.demo.repository;
import com.example.demo.model.CategoriaServicios;
import com.example.demo.model.CategoriaServiciosID;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaServiciosRepository  extends JpaRepository<CategoriaServicios, CategoriaServiciosID>
{

}
