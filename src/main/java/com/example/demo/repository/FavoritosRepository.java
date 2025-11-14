package com.example.demo.repository;

import com.example.demo.model.Favoritos;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoritosRepository extends JpaRepository<Favoritos, Integer> {


}
