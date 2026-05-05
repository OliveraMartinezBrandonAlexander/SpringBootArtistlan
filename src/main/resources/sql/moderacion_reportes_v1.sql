ALTER TABLE usuario
    ADD COLUMN estado_cuenta VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    ADD COLUMN motivo_suspension TEXT NULL,
    ADD COLUMN fecha_suspension DATETIME NULL,
    ADD COLUMN fecha_fin_suspension DATETIME NULL;

ALTER TABLE obra
    ADD COLUMN oculta BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN estado_moderacion VARCHAR(40) NOT NULL DEFAULT 'SIN_REPORTES',
    ADD COLUMN motivo_oculta TEXT NULL,
    ADD COLUMN fecha_oculta DATETIME NULL;

ALTER TABLE servicio
    ADD COLUMN oculto BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN estado_moderacion VARCHAR(40) NOT NULL DEFAULT 'SIN_REPORTES',
    ADD COLUMN motivo_oculto TEXT NULL,
    ADD COLUMN fecha_oculto DATETIME NULL;

CREATE TABLE reporte (
    id_reporte INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario_reportante INT NOT NULL,
    tipo_objetivo VARCHAR(20) NOT NULL,
    id_obra INT NULL,
    id_servicio INT NULL,
    id_usuario_reportado INT NULL,
    motivo VARCHAR(100) NOT NULL,
    descripcion TEXT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    prioridad VARCHAR(20) NOT NULL DEFAULT 'MEDIA',
    id_moderador_asignado INT NULL,
    fecha_reporte DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_inicio_revision DATETIME NULL,
    fecha_actualizacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reporte_usuario_reportante
        FOREIGN KEY (id_usuario_reportante) REFERENCES usuario (id_usuario),
    CONSTRAINT fk_reporte_obra
        FOREIGN KEY (id_obra) REFERENCES obra (id_obra),
    CONSTRAINT fk_reporte_servicio
        FOREIGN KEY (id_servicio) REFERENCES servicio (id_servicio),
    CONSTRAINT fk_reporte_usuario_reportado
        FOREIGN KEY (id_usuario_reportado) REFERENCES usuario (id_usuario),
    CONSTRAINT fk_reporte_moderador_asignado
        FOREIGN KEY (id_moderador_asignado) REFERENCES usuario (id_usuario),
    CONSTRAINT chk_reporte_objetivo
        CHECK (
            (tipo_objetivo = 'OBRA' AND id_obra IS NOT NULL AND id_servicio IS NULL AND id_usuario_reportado IS NULL) OR
            (tipo_objetivo = 'SERVICIO' AND id_obra IS NULL AND id_servicio IS NOT NULL AND id_usuario_reportado IS NULL) OR
            (tipo_objetivo = 'USUARIO' AND id_obra IS NULL AND id_servicio IS NULL AND id_usuario_reportado IS NOT NULL)
        )
);

CREATE TABLE resolucion_reporte (
    id_resolucion INT AUTO_INCREMENT PRIMARY KEY,
    id_reporte INT NOT NULL,
    id_moderador INT NOT NULL,
    mensaje_respuesta TEXT NOT NULL,
    tipo_respuesta VARCHAR(30) NOT NULL,
    accion VARCHAR(50) NOT NULL,
    fecha_resolucion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_resolucion_reporte_reporte
        FOREIGN KEY (id_reporte) REFERENCES reporte (id_reporte),
    CONSTRAINT fk_resolucion_reporte_moderador
        FOREIGN KEY (id_moderador) REFERENCES usuario (id_usuario),
    CONSTRAINT uq_resolucion_reporte UNIQUE (id_reporte)
);

CREATE TABLE moderacion_contenido (
    id_moderacion INT AUTO_INCREMENT PRIMARY KEY,
    id_reporte INT NULL,
    tipo_contenido VARCHAR(20) NOT NULL,
    id_obra INT NULL,
    id_servicio INT NULL,
    id_moderador INT NOT NULL,
    motivo TEXT NOT NULL,
    accion VARCHAR(30) NOT NULL,
    fecha_accion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_moderacion_contenido_reporte
        FOREIGN KEY (id_reporte) REFERENCES reporte (id_reporte),
    CONSTRAINT fk_moderacion_contenido_obra
        FOREIGN KEY (id_obra) REFERENCES obra (id_obra),
    CONSTRAINT fk_moderacion_contenido_servicio
        FOREIGN KEY (id_servicio) REFERENCES servicio (id_servicio),
    CONSTRAINT fk_moderacion_contenido_moderador
        FOREIGN KEY (id_moderador) REFERENCES usuario (id_usuario),
    CONSTRAINT chk_moderacion_contenido_objetivo
        CHECK (
            (tipo_contenido = 'OBRA' AND id_obra IS NOT NULL AND id_servicio IS NULL) OR
            (tipo_contenido = 'SERVICIO' AND id_obra IS NULL AND id_servicio IS NOT NULL)
        )
);

CREATE TABLE suspension_usuario (
    id_suspension INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    id_moderador INT NOT NULL,
    id_reporte INT NULL,
    motivo TEXT NOT NULL,
    tipo_sancion VARCHAR(40) NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVA',
    fecha_inicio DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_fin DATETIME NULL,
    fecha_accion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_suspension_usuario_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
    CONSTRAINT fk_suspension_usuario_moderador
        FOREIGN KEY (id_moderador) REFERENCES usuario (id_usuario),
    CONSTRAINT fk_suspension_usuario_reporte
        FOREIGN KEY (id_reporte) REFERENCES reporte (id_reporte)
);

CREATE INDEX idx_reporte_estado ON reporte (estado);
CREATE INDEX idx_reporte_prioridad ON reporte (prioridad);
CREATE INDEX idx_reporte_tipo_objetivo ON reporte (tipo_objetivo);
CREATE INDEX idx_reporte_fecha_reporte ON reporte (fecha_reporte);
CREATE INDEX idx_reporte_moderador_estado ON reporte (id_moderador_asignado, estado);
CREATE INDEX idx_reporte_estado_prioridad_fecha ON reporte (estado, prioridad, fecha_reporte);
CREATE INDEX idx_reporte_obra ON reporte (id_obra);
CREATE INDEX idx_reporte_servicio ON reporte (id_servicio);
CREATE INDEX idx_reporte_usuario_reportado ON reporte (id_usuario_reportado);
CREATE INDEX idx_reporte_usuario_reportante ON reporte (id_usuario_reportante);

CREATE INDEX idx_obra_estado_moderacion ON obra (estado_moderacion);
CREATE INDEX idx_obra_oculta ON obra (oculta);
CREATE INDEX idx_obra_estado_oculta_moderacion ON obra (estado, oculta, estado_moderacion);

CREATE INDEX idx_servicio_estado_moderacion ON servicio (estado_moderacion);
CREATE INDEX idx_servicio_oculto ON servicio (oculto);

CREATE INDEX idx_usuario_estado_cuenta ON usuario (estado_cuenta);
CREATE INDEX idx_usuario_estado_cuenta_fecha_fin_suspension ON usuario (estado_cuenta, fecha_fin_suspension);
