package com.example.demo.dto;

public class UsuarioIdCategoriaDTO {
    private Integer idCategoria;
    private String nombreCategoria;
    private String tipo;

    public UsuarioIdCategoriaDTO(Integer idCategoria, String nombreCategoria, String tipo) {
        this.idCategoria = idCategoria;
        this.nombreCategoria = nombreCategoria;
        this.tipo = tipo;
    }

    public Integer getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(Integer idCategoria) {
        this.idCategoria = idCategoria;
    }

    public String getNombreCategoria() {
        return nombreCategoria;
    }

    public void setNombreCategoria(String nombreCategoria) {
        this.nombreCategoria = nombreCategoria;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
}
