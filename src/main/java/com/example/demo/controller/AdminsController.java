package com.example.demo.controller;


import com.example.demo.model.Usuario;
import com.example.demo.service.UsuarioService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admins")
@AllArgsConstructor

public class AdminsController {

    private final UsuarioService usuarioService;

    //GET para obtener solo admins
    @GetMapping
    public List<Usuario> obtenerAdmins(){

        return  usuarioService.listarAdmins();
    }


    // POST para crear un admin
    @PostMapping
    public Usuario crearAdmin(@RequestBody Usuario usuario) {
        usuario.setAdminUsuario(1); // aseguramos que sea admin
        return usuarioService.guardarUsuario(usuario);
    }

    //PUT actualizar admin por ID
    @PutMapping ("/{id}")
    public Usuario actualizarAdmin(@PathVariable Integer id, @RequestBody Usuario usuario){
        usuario.setAdminUsuario(1); // asegurar que siga siendo admin
        return usuarioService.actualizarUsuario(id, usuario).orElse(null);

    }

    // DELETE admin por ID
    @DeleteMapping("/{id}")
    public boolean eliminarAdmin(@PathVariable Integer id) {
        return usuarioService.eliminarUsuario(id);
    }

}
