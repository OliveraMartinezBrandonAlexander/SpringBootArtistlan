package com.example.demo.scheduler;

import com.example.demo.service.CarritoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservaScheduler {

    private final CarritoService carritoService;

    @Scheduled(cron = "0 */15 * * * *")
    public void expirarReservas() {
        int expiradas = carritoService.limpiarReservasVencidas();
        if (expiradas > 0) {
            log.info("Reservas expiradas procesadas: {}", expiradas);
        }
    }
}
