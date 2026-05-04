package com.example.demo.repository;

import com.example.demo.enums.EstadoReporte;
import com.example.demo.model.Reporte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReporteRepository extends JpaRepository<Reporte, Integer> {

    boolean existsByUsuarioReportante_IdUsuarioAndObra_IdObraAndEstadoIn(Integer idUsuarioReportante,
                                                                         Integer idObra,
                                                                         List<EstadoReporte> estados);

    boolean existsByUsuarioReportante_IdUsuarioAndServicio_IdServicioAndEstadoIn(Integer idUsuarioReportante,
                                                                                 Integer idServicio,
                                                                                 List<EstadoReporte> estados);

    boolean existsByUsuarioReportante_IdUsuarioAndUsuarioReportado_IdUsuarioAndEstadoIn(Integer idUsuarioReportante,
                                                                                        Integer idUsuarioReportado,
                                                                                        List<EstadoReporte> estados);

    List<Reporte> findByUsuarioReportante_IdUsuarioOrderByFechaReporteDesc(Integer idUsuarioReportante);

    List<Reporte> findAllByOrderByFechaReporteDesc();

    List<Reporte> findByEstadoOrderByFechaReporteDesc(EstadoReporte estado);

    List<Reporte> findByModeradorAsignado_IdUsuarioOrderByFechaReporteDesc(Integer idModeradorAsignado);

    List<Reporte> findByEstadoAndModeradorAsignado_IdUsuarioOrderByFechaReporteDesc(EstadoReporte estado,
                                                                                    Integer idModeradorAsignado);
}
