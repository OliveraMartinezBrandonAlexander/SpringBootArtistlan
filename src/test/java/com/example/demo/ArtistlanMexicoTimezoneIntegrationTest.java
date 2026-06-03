package com.example.demo;

import com.example.demo.dto.admin.AdminCrecimientoDTO;
import com.example.demo.dto.admin.AdminPuntoSerieDTO;
import com.example.demo.dto.admin.AdminSerieTemporalDTO;
import com.example.demo.enums.AdminDashboardTipo;
import com.example.demo.model.CompraObra;
import com.example.demo.model.Obra;
import com.example.demo.model.Usuario;
import com.example.demo.repository.CompraObraRepository;
import com.example.demo.repository.ObraRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.AdminEstadisticasService;
import com.example.demo.util.ArtistlanDateTimeUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class ArtistlanMexicoTimezoneIntegrationTest {

    private static final DateTimeFormatter MYSQL_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ObraRepository obraRepository;

    @Autowired
    private CompraObraRepository compraObraRepository;

    @Autowired
    private AdminEstadisticasService adminEstadisticasService;

    @Test
    void helperMexicoDebeCoincidirConNowDeMysql() {
        LocalDateTime mysqlNow = consultarFechaMysql("SELECT DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s')");
        LocalDateTime mexicoNow = ArtistlanDateTimeUtils.nowMexico().withNano(0);

        long diferenciaSegundos = Math.abs(Duration.between(mysqlNow, mexicoNow).getSeconds());
        assertTrue(diferenciaSegundos < 120,
                "La hora de MySQL y la hora Mexico del backend deberian diferir menos de 2 minutos.");

        String sessionTimeZone = jdbcTemplate.queryForObject("SELECT @@session.time_zone", String.class);
        assertNotNull(sessionTimeZone);
    }

    @Test
    void obraYCompraNuevasDebenQuedarEnDiaMexicoYReflejarseEnEstadisticasActuales() {
        LocalDate hoyMexico = ArtistlanDateTimeUtils.todayMexico();

        AdminCrecimientoDTO crecimientoAntes =
                adminEstadisticasService.obtenerCrecimientoSemanal(AdminDashboardTipo.OBRAS, hoyMexico);
        AdminSerieTemporalDTO ventasAntes =
                adminEstadisticasService.obtenerVentasSemanales(hoyMexico);

        Usuario usuario = usuarioRepository.findAll().stream()
                .min(Comparator.comparing(Usuario::getIdUsuario))
                .orElseThrow(() -> new IllegalStateException("No hay usuarios disponibles para la prueba."));

        Obra obra = obraRepository.saveAndFlush(Obra.builder()
                .titulo("TZ Test " + UUID.randomUUID())
                .descripcion("Validacion de timezone Mexico")
                .estado("EN_VENTA")
                .precio(new BigDecimal("123.45"))
                .imagen1("https://artistlan.test/obra-timezone.jpg")
                .confirmacionAutoria(Boolean.TRUE)
                .usuario(usuario)
                .build());

        LocalDateTime fechaPublicacionMysql = consultarFechaMysql(
                "SELECT DATE_FORMAT(fecha_publicacion, '%Y-%m-%d %H:%i:%s') FROM obra WHERE id_obra = ?",
                obra.getIdObra()
        );
        assertFechaCercanaANowMexico(fechaPublicacionMysql);
        assertEquals(hoyMexico, fechaPublicacionMysql.toLocalDate(),
                "La obra nueva debe guardarse con la fecha local de Mexico.");

        CompraObra compra = compraObraRepository.saveAndFlush(CompraObra.builder()
                .obra(obra)
                .comprador(usuario)
                .vendedor(usuario)
                .monto(new BigDecimal("123.45"))
                .moneda("MXN")
                .estado("CAPTURADA")
                .paypalOrderId("tz-order-" + UUID.randomUUID())
                .paypalCaptureId("tz-capture-" + UUID.randomUUID())
                .fechaCaptura(ArtistlanDateTimeUtils.nowMexico())
                .build());

        LocalDateTime fechaCreacionMysql = consultarFechaMysql(
                "SELECT DATE_FORMAT(fecha_creacion, '%Y-%m-%d %H:%i:%s') FROM compra_obra WHERE id_compra = ?",
                compra.getIdCompra()
        );
        LocalDateTime fechaCapturaMysql = consultarFechaMysql(
                "SELECT DATE_FORMAT(fecha_captura, '%Y-%m-%d %H:%i:%s') FROM compra_obra WHERE id_compra = ?",
                compra.getIdCompra()
        );
        assertFechaCercanaANowMexico(fechaCreacionMysql);
        assertFechaCercanaANowMexico(fechaCapturaMysql);
        assertEquals(hoyMexico, fechaCreacionMysql.toLocalDate(),
                "La compra nueva debe guardarse con la fecha local de Mexico.");
        assertEquals(hoyMexico, fechaCapturaMysql.toLocalDate(),
                "La captura nueva debe guardarse con la fecha local de Mexico.");

        AdminCrecimientoDTO crecimientoDespues =
                adminEstadisticasService.obtenerCrecimientoSemanal(AdminDashboardTipo.OBRAS, hoyMexico);
        AdminSerieTemporalDTO ventasDespues =
                adminEstadisticasService.obtenerVentasSemanales(hoyMexico);

        assertEquals(crecimientoAntes.getTotalSemanaActual() + 1, crecimientoDespues.getTotalSemanaActual(),
                "La semana actual de crecimiento debe aumentar en 1 obra.");
        assertEquals(valorEnFecha(crecimientoAntes.getSerieSemanaActual(), hoyMexico) + 1,
                valorEnFecha(crecimientoDespues.getSerieSemanaActual(), hoyMexico),
                "El punto del dia actual en crecimiento debe reflejar la nueva obra.");

        assertEquals(ventasAntes.getTotalVentas() + 1, ventasDespues.getTotalVentas(),
                "La semana actual de ventas debe aumentar en 1 compra.");
        assertEquals(valorEnFecha(ventasAntes.getPuntos(), hoyMexico) + 1,
                valorEnFecha(ventasDespues.getPuntos(), hoyMexico),
                "El punto del dia actual en ventas debe reflejar la nueva compra.");

        assertSinDatosFuturos(crecimientoDespues.getSerieSemanaActual(), hoyMexico);
        assertSinDatosFuturos(ventasDespues.getPuntos(), hoyMexico);
    }

    private void assertFechaCercanaANowMexico(LocalDateTime fecha) {
        LocalDateTime mysqlNow = consultarFechaMysql("SELECT DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s')");
        long diferenciaSegundos = Math.abs(Duration.between(mysqlNow, fecha.withNano(0)).getSeconds());
        assertTrue(diferenciaSegundos < 120,
                "La fecha persistida deberia quedar alineada con NOW() de MySQL en hora Mexico.");
    }

    private void assertSinDatosFuturos(List<AdminPuntoSerieDTO> puntos, LocalDate hoyMexico) {
        long futuros = puntos.stream()
                .filter(punto -> punto.getFecha() != null && punto.getFecha().isAfter(hoyMexico))
                .mapToLong(AdminPuntoSerieDTO::getValor)
                .sum();
        assertEquals(0L, futuros, "La serie semanal actual no debe mostrar datos futuros falsos.");
    }

    private long valorEnFecha(List<AdminPuntoSerieDTO> puntos, LocalDate fecha) {
        return puntos.stream()
                .filter(punto -> fecha.equals(punto.getFecha()))
                .findFirst()
                .map(AdminPuntoSerieDTO::getValor)
                .orElse(0L);
    }

    private LocalDateTime consultarFechaMysql(String sql, Object... args) {
        String valor = jdbcTemplate.queryForObject(sql, String.class, args);
        assertNotNull(valor);
        return LocalDateTime.parse(valor, MYSQL_DATE_TIME);
    }
}
