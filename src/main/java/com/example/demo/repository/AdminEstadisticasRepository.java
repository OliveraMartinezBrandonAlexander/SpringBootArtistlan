package com.example.demo.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class AdminEstadisticasRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<Object[]> obtenerObrasPorCategoria() {
        return entityManager.createNativeQuery("""
                SELECT c.id_categoria, c.nombre_categoria, COUNT(o.id_obra) AS total
                FROM categoria c
                LEFT JOIN categoria_obra co ON co.id_categoria = c.id_categoria
                LEFT JOIN obra o
                    ON o.id_obra = co.id_obra
                   AND COALESCE(o.oculta, 0) = 0
                   AND UPPER(COALESCE(o.estado_moderacion, 'SIN_REPORTES')) NOT IN ('OCULTO', 'ELIMINADO_POR_MODERACION')
                WHERE c.id_categoria BETWEEN 1 AND 18
                GROUP BY c.id_categoria, c.nombre_categoria
                ORDER BY total DESC, c.nombre_categoria ASC
                """).getResultList();
    }

    public List<Object[]> obtenerServiciosPorCategoria() {
        return entityManager.createNativeQuery("""
                SELECT c.id_categoria, c.nombre_categoria, COUNT(s.id_servicio) AS total
                FROM categoria c
                LEFT JOIN categoria_servicio cs ON cs.id_categoria = c.id_categoria
                LEFT JOIN servicio s
                    ON s.id_servicio = cs.id_servicio
                   AND COALESCE(s.oculto, 0) = 0
                   AND UPPER(COALESCE(s.estado_moderacion, 'SIN_REPORTES')) NOT IN ('OCULTO', 'ELIMINADO_POR_MODERACION')
                WHERE c.id_categoria BETWEEN 19 AND 37
                GROUP BY c.id_categoria, c.nombre_categoria
                ORDER BY total DESC, c.nombre_categoria ASC
                """).getResultList();
    }

    public List<Object[]> obtenerArtistasPorCategoria() {
        return entityManager.createNativeQuery("""
                SELECT c.id_categoria, c.nombre_categoria, COUNT(u.id_usuario) AS total
                FROM categoria c
                LEFT JOIN categoria_usuario cu ON cu.id_categoria = c.id_categoria
                LEFT JOIN usuario u
                    ON u.id_usuario = cu.id_usuario
                   AND UPPER(COALESCE(u.estado_cuenta, 'ACTIVO')) = 'ACTIVO'
                WHERE c.id_categoria BETWEEN 19 AND 37
                GROUP BY c.id_categoria, c.nombre_categoria
                ORDER BY total DESC, c.nombre_categoria ASC
                """).getResultList();
    }

    public Number contarArtistasSinCategoria() {
        return (Number) entityManager.createNativeQuery("""
                SELECT COUNT(u.id_usuario)
                FROM usuario u
                LEFT JOIN categoria_usuario cu ON cu.id_usuario = u.id_usuario
                WHERE UPPER(COALESCE(u.estado_cuenta, 'ACTIVO')) = 'ACTIVO'
                  AND cu.id_usuario IS NULL
                """).getSingleResult();
    }

    public List<Object[]> obtenerVentasDirectasPorDia(LocalDateTime inicio, LocalDateTime finExclusive) {
        Query query = entityManager.createNativeQuery("""
                SELECT DATE(COALESCE(co.fecha_captura, co.fecha_creacion)) AS fecha,
                       COUNT(*) AS total_ventas,
                       COALESCE(SUM(co.monto), 0) AS total_ingresos
                FROM compra_obra co
                WHERE UPPER(co.estado) IN ('CAPTURADA', 'PAGADA', 'COMPLETADA')
                  AND COALESCE(co.fecha_captura, co.fecha_creacion) >= :inicio
                  AND COALESCE(co.fecha_captura, co.fecha_creacion) < :finExclusive
                GROUP BY DATE(COALESCE(co.fecha_captura, co.fecha_creacion))
                ORDER BY fecha ASC
                """);
        query.setParameter("inicio", inicio);
        query.setParameter("finExclusive", finExclusive);
        return query.getResultList();
    }

    public List<Object[]> obtenerVentasCarritoPorDia(LocalDateTime inicio, LocalDateTime finExclusive) {
        Query query = entityManager.createNativeQuery("""
                SELECT DATE(COALESCE(cc.fecha_captura, cc.fecha_creacion)) AS fecha,
                       COUNT(*) AS total_ventas,
                       COALESCE(SUM(cc.monto_total), 0) AS total_ingresos
                FROM compra_carrito cc
                WHERE UPPER(cc.estado) IN ('CAPTURADA', 'PAGADA', 'COMPLETADA')
                  AND COALESCE(cc.fecha_captura, cc.fecha_creacion) >= :inicio
                  AND COALESCE(cc.fecha_captura, cc.fecha_creacion) < :finExclusive
                GROUP BY DATE(COALESCE(cc.fecha_captura, cc.fecha_creacion))
                ORDER BY fecha ASC
                """);
        query.setParameter("inicio", inicio);
        query.setParameter("finExclusive", finExclusive);
        return query.getResultList();
    }

    public List<Object[]> obtenerRankingObras(int limit) {
        Query query = entityManager.createNativeQuery("""
                SELECT o.id_obra,
                       o.titulo,
                       COUNT(f.id_favorito) AS total,
                       o.imagen1 AS imagen,
                       COALESCE(NULLIF(TRIM(u.nombre_completo), ''), u.usuario) AS autor,
                       COALESCE(NULLIF(TRIM(u.nombre_completo), ''), u.usuario) AS subtitulo,
                       NULL AS contacto,
                       NULL AS tipo_contacto,
                       u.foto_perfil AS imagen_autor
                FROM obra o
                JOIN usuario u ON u.id_usuario = o.id_usuario
                LEFT JOIN favorito f ON f.id_obra = o.id_obra
                WHERE COALESCE(o.oculta, 0) = 0
                  AND UPPER(COALESCE(o.estado_moderacion, 'SIN_REPORTES')) NOT IN ('OCULTO', 'ELIMINADO_POR_MODERACION')
                GROUP BY o.id_obra, o.titulo, o.imagen1, u.nombre_completo, u.usuario
                HAVING COUNT(f.id_favorito) > 0
                ORDER BY total DESC, o.id_obra ASC
                """);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    public List<Object[]> obtenerRankingServicios(int limit) {
        Query query = entityManager.createNativeQuery("""
                SELECT s.id_servicio,
                       s.titulo,
                       COUNT(f.id_favorito) AS total,
                       u.foto_perfil AS imagen,
                       COALESCE(NULLIF(TRIM(u.nombre_completo), ''), u.usuario) AS autor,
                       COALESCE(NULLIF(TRIM(s.tecnicas), ''), COALESCE(NULLIF(TRIM(u.nombre_completo), ''), u.usuario)) AS subtitulo,
                       NULLIF(TRIM(s.contacto), '') AS contacto,
                       NULLIF(TRIM(s.tipo_contacto), '') AS tipo_contacto,
                       NULL AS imagen_autor
                FROM servicio s
                JOIN usuario u ON u.id_usuario = s.id_usuario
                LEFT JOIN favorito f ON f.id_servicio = s.id_servicio
                WHERE COALESCE(s.oculto, 0) = 0
                  AND UPPER(COALESCE(s.estado_moderacion, 'SIN_REPORTES')) NOT IN ('OCULTO', 'ELIMINADO_POR_MODERACION')
                GROUP BY s.id_servicio, s.titulo, u.foto_perfil, u.nombre_completo, u.usuario, s.tecnicas, s.contacto, s.tipo_contacto
                HAVING COUNT(f.id_favorito) > 0
                ORDER BY total DESC, s.id_servicio ASC
                """);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    public List<Object[]> obtenerRankingArtistas(int limit) {
        Query query = entityManager.createNativeQuery("""
                SELECT u.id_usuario,
                       COALESCE(NULLIF(TRIM(u.nombre_completo), ''), u.usuario) AS artista,
                       (
                           COALESCE(fo.total_obras, 0)
                           + COALESCE(fs.total_servicios, 0)
                           + COALESCE(fa.total_directos, 0)
                       ) AS total,
                       u.foto_perfil AS imagen,
                       NULL AS autor,
                       COALESCE(NULLIF(TRIM(cu_cat.nombre_categoria), ''), 'Perfil artistico') AS subtitulo,
                       NULL AS contacto,
                       NULL AS tipo_contacto,
                       NULL AS imagen_autor
                FROM usuario u
                LEFT JOIN (
                    SELECT o.id_usuario, COUNT(f.id_favorito) AS total_obras
                    FROM obra o
                    JOIN favorito f ON f.id_obra = o.id_obra
                    WHERE COALESCE(o.oculta, 0) = 0
                      AND UPPER(COALESCE(o.estado_moderacion, 'SIN_REPORTES')) NOT IN ('OCULTO', 'ELIMINADO_POR_MODERACION')
                    GROUP BY o.id_usuario
                ) fo ON fo.id_usuario = u.id_usuario
                LEFT JOIN (
                    SELECT s.id_usuario, COUNT(f.id_favorito) AS total_servicios
                    FROM servicio s
                    JOIN favorito f ON f.id_servicio = s.id_servicio
                    WHERE COALESCE(s.oculto, 0) = 0
                      AND UPPER(COALESCE(s.estado_moderacion, 'SIN_REPORTES')) NOT IN ('OCULTO', 'ELIMINADO_POR_MODERACION')
                    GROUP BY s.id_usuario
                ) fs ON fs.id_usuario = u.id_usuario
                LEFT JOIN (
                    SELECT f.id_artista AS id_usuario, COUNT(f.id_favorito) AS total_directos
                    FROM favorito f
                    WHERE f.id_artista IS NOT NULL
                    GROUP BY f.id_artista
                ) fa ON fa.id_usuario = u.id_usuario
                LEFT JOIN (
                    SELECT cu.id_usuario, MIN(c.nombre_categoria) AS nombre_categoria
                    FROM categoria_usuario cu
                    JOIN categoria c ON c.id_categoria = cu.id_categoria
                    WHERE c.id_categoria BETWEEN 19 AND 37
                    GROUP BY cu.id_usuario
                ) cu_cat ON cu_cat.id_usuario = u.id_usuario
                WHERE UPPER(COALESCE(u.rol, 'USER')) = 'USER'
                  AND UPPER(COALESCE(u.estado_cuenta, 'ACTIVO')) = 'ACTIVO'
                  AND (
                      COALESCE(fo.total_obras, 0)
                      + COALESCE(fs.total_servicios, 0)
                      + COALESCE(fa.total_directos, 0)
                  ) > 0
                ORDER BY total DESC, u.id_usuario ASC
                """);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    public List<Object[]> obtenerPublicacionesObrasPorDia(LocalDateTime inicio, LocalDateTime finExclusive) {
        Query query = entityManager.createNativeQuery("""
                SELECT DATE(o.fecha_publicacion) AS fecha, COUNT(*) AS total
                FROM obra o
                WHERE o.fecha_publicacion >= :inicio
                  AND o.fecha_publicacion < :finExclusive
                  AND COALESCE(o.oculta, 0) = 0
                  AND UPPER(COALESCE(o.estado_moderacion, 'SIN_REPORTES')) NOT IN ('OCULTO', 'ELIMINADO_POR_MODERACION')
                GROUP BY DATE(o.fecha_publicacion)
                ORDER BY fecha ASC
                """);
        query.setParameter("inicio", inicio);
        query.setParameter("finExclusive", finExclusive);
        return query.getResultList();
    }

    public List<Object[]> obtenerPublicacionesServiciosPorDia(LocalDateTime inicio, LocalDateTime finExclusive) {
        Query query = entityManager.createNativeQuery("""
                SELECT DATE(s.fecha_publicacion) AS fecha, COUNT(*) AS total
                FROM servicio s
                WHERE s.fecha_publicacion >= :inicio
                  AND s.fecha_publicacion < :finExclusive
                  AND COALESCE(s.oculto, 0) = 0
                  AND UPPER(COALESCE(s.estado_moderacion, 'SIN_REPORTES')) NOT IN ('OCULTO', 'ELIMINADO_POR_MODERACION')
                GROUP BY DATE(s.fecha_publicacion)
                ORDER BY fecha ASC
                """);
        query.setParameter("inicio", inicio);
        query.setParameter("finExclusive", finExclusive);
        return query.getResultList();
    }

    public List<Object[]> obtenerArtistasNuevosPorDia(LocalDateTime inicio, LocalDateTime finExclusive) {
        Query query = entityManager.createNativeQuery("""
                SELECT DATE(u.fecha_registro) AS fecha, COUNT(*) AS total
                FROM usuario u
                WHERE UPPER(COALESCE(u.rol, 'USER')) = 'USER'
                  AND UPPER(COALESCE(u.estado_cuenta, 'ACTIVO')) = 'ACTIVO'
                  AND u.fecha_registro >= :inicio
                  AND u.fecha_registro < :finExclusive
                GROUP BY DATE(u.fecha_registro)
                ORDER BY fecha ASC
                """);
        query.setParameter("inicio", inicio);
        query.setParameter("finExclusive", finExclusive);
        return query.getResultList();
    }
}
