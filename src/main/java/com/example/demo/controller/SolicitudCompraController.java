package com.example.demo.controller;

import com.example.demo.dto.solicitud.ContadorSolicitudesPendientesDTO;
import com.example.demo.dto.solicitud.CrearSolicitudCompraRequestDTO;
import com.example.demo.dto.PageResponseDTO;
import com.example.demo.dto.solicitud.ResolverSolicitudRequestDTO;
import com.example.demo.dto.solicitud.SolicitudCompraDTO;
import com.example.demo.service.SolicitudCompraService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/solicitudes-compra")
@RequiredArgsConstructor
public class SolicitudCompraController {

    private final SolicitudCompraService solicitudCompraService;

    @PostMapping
    public ResponseEntity<SolicitudCompraDTO> crear(@RequestBody CrearSolicitudCompraRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(solicitudCompraService.crearSolicitud(request));
    }

    @GetMapping("/recibidas/{vendedorId}")
    public ResponseEntity<List<SolicitudCompraDTO>> recibidas(@PathVariable Integer vendedorId) {
        return ResponseEntity.ok(solicitudCompraService.listarRecibidas(vendedorId));
    }

    @GetMapping("/recibidas/{vendedorId}/paginado")
    public ResponseEntity<PageResponseDTO<SolicitudCompraDTO>> recibidasPaginadas(
            @PathVariable Integer vendedorId,
            @RequestParam(required = false) String estado,
            @PageableDefault(size = 10, sort = "fechaCreacion", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(PageResponseDTO.fromPage(
                solicitudCompraService.listarRecibidasPaginadas(vendedorId, estado, pageable)
        ));
    }

    @GetMapping("/enviadas/{compradorId}")
    public ResponseEntity<List<SolicitudCompraDTO>> enviadas(@PathVariable Integer compradorId) {
        return ResponseEntity.ok(solicitudCompraService.listarEnviadas(compradorId));
    }

    @GetMapping("/enviadas/{compradorId}/paginado")
    public ResponseEntity<PageResponseDTO<SolicitudCompraDTO>> enviadasPaginadas(
            @PathVariable Integer compradorId,
            @RequestParam(required = false) String estado,
            @PageableDefault(size = 10, sort = "fechaCreacion", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(PageResponseDTO.fromPage(
                solicitudCompraService.listarEnviadasPaginadas(compradorId, estado, pageable)
        ));
    }

    @GetMapping("/{usuarioId}/contador-pendientes")
    public ResponseEntity<ContadorSolicitudesPendientesDTO> contadorPendientes(@PathVariable Integer usuarioId) {
        return ResponseEntity.ok(ContadorSolicitudesPendientesDTO.builder()
                .pendientes(solicitudCompraService.contarPendientesUsuario(usuarioId))
                .build());
    }

    @GetMapping("/{idSolicitud}")
    public ResponseEntity<SolicitudCompraDTO> detalle(@PathVariable Integer idSolicitud,
                                                      @RequestParam Integer actorId) {
        return ResponseEntity.ok(solicitudCompraService.obtenerDetalle(idSolicitud, actorId));
    }

    @PostMapping("/{idSolicitud}/aceptar")
    public ResponseEntity<SolicitudCompraDTO> aceptar(@PathVariable Integer idSolicitud,
                                                      @RequestBody ResolverSolicitudRequestDTO request) {
        return ResponseEntity.ok(solicitudCompraService.aceptar(idSolicitud, request.getIdVendedor()));
    }

    @PostMapping("/{idSolicitud}/rechazar")
    public ResponseEntity<SolicitudCompraDTO> rechazar(@PathVariable Integer idSolicitud,
                                                       @RequestBody ResolverSolicitudRequestDTO request) {
        return ResponseEntity.ok(solicitudCompraService.rechazar(idSolicitud, request.getIdVendedor(), request.getMotivo()));
    }

    @PostMapping("/{idSolicitud}/cancelar")
    public ResponseEntity<SolicitudCompraDTO> cancelar(@PathVariable Integer idSolicitud,
                                                       @RequestParam Integer idComprador) {
        return ResponseEntity.ok(solicitudCompraService.cancelar(idSolicitud, idComprador));
    }
}
