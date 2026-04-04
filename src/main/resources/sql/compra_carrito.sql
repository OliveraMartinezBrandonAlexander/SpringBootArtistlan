CREATE TABLE compra_carrito (
    id_compra_carrito INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario_comprador INT NOT NULL,
    monto_total DECIMAL(10,2) NOT NULL,
    moneda VARCHAR(10) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    paypal_order_id VARCHAR(100) NOT NULL,
    paypal_capture_id VARCHAR(100) NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_captura DATETIME NULL,
    CONSTRAINT uq_compra_carrito_paypal_order UNIQUE (paypal_order_id),
    CONSTRAINT uq_compra_carrito_paypal_capture UNIQUE (paypal_capture_id),
    CONSTRAINT fk_compra_carrito_comprador
        FOREIGN KEY (id_usuario_comprador) REFERENCES usuario(id_usuario)
);

CREATE INDEX idx_compra_carrito_usuario_comprador
    ON compra_carrito(id_usuario_comprador);

CREATE INDEX idx_compra_carrito_estado
    ON compra_carrito(estado);

CREATE TABLE compra_carrito_detalle (
    id_detalle INT AUTO_INCREMENT PRIMARY KEY,
    id_compra_carrito INT NOT NULL,
    id_obra INT NOT NULL,
    id_vendedor INT NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    CONSTRAINT uq_compra_carrito_detalle_compra_obra UNIQUE (id_compra_carrito, id_obra),
    CONSTRAINT fk_compra_carrito_detalle_compra
        FOREIGN KEY (id_compra_carrito) REFERENCES compra_carrito(id_compra_carrito),
    CONSTRAINT fk_compra_carrito_detalle_obra
        FOREIGN KEY (id_obra) REFERENCES obra(id_obra),
    CONSTRAINT fk_compra_carrito_detalle_vendedor
        FOREIGN KEY (id_vendedor) REFERENCES usuario(id_usuario)
);

CREATE INDEX idx_compra_carrito_detalle_obra
    ON compra_carrito_detalle(id_obra);
