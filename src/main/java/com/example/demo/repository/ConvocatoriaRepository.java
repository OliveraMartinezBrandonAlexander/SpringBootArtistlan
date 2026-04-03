package com.example.demo.repository;

import com.example.demo.model.Convocatoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConvocatoriaRepository extends JpaRepository<Convocatoria, Integer> {
}