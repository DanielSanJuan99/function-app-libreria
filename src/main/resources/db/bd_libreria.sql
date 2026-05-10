-- =====================================================================
-- BD: bdlibreriacn2 (Oracle Autonomous Database)
-- Esquema: ADMIN
-- Proposito: Sistema de Biblioteca - Desarrollo Cloud Native II (Semana 9)
--
-- Este script crea la estructura completa de la base de datos del caso
-- "Sistema de Biblioteca", incluyendo:
--   * Tablas de negocio (TIPO_USUARIO, TIPO_ESTADO, AUTOR, USUARIO,
--     LIBRO, PRESTAMO, NOTIFICACIONES)
--   * Tabla EVENTO_PROCESADO para idempotencia del consumer Event Grid
--   * Secuencias e indices unicos
--   * Foreign keys
--   * Datos seed coherentes (1 registro nuevo encadenado por tabla)
--
-- Para ejecutar desde cero, descomentar la seccion DROP del inicio.
-- Para una BD existente, los DML usan MERGE / WHERE NOT EXISTS para
-- evitar duplicados.
-- =====================================================================

-- ---------------------------------------------------------------------
-- (Opcional) Limpieza completa - DESCOMENTAR para regenerar desde cero
-- ---------------------------------------------------------------------
-- DROP TABLE NOTIFICACIONES CASCADE CONSTRAINTS;
-- DROP TABLE EVENTO_PROCESADO CASCADE CONSTRAINTS;
-- DROP TABLE PRESTAMO CASCADE CONSTRAINTS;
-- DROP TABLE LIBRO CASCADE CONSTRAINTS;
-- DROP TABLE USUARIO CASCADE CONSTRAINTS;
-- DROP TABLE AUTOR CASCADE CONSTRAINTS;
-- DROP TABLE TIPO_ESTADO CASCADE CONSTRAINTS;
-- DROP TABLE TIPO_USUARIO CASCADE CONSTRAINTS;
-- DROP SEQUENCE TU_ID_TIPO_USUARIO_SEQ;
-- DROP SEQUENCE TE_ID_ESTADO_SEQ;
-- DROP SEQUENCE A_ID_AUTOR_SEQ;
-- DROP SEQUENCE U_ID_USUARIO_SEQ;
-- DROP SEQUENCE L_ID_LIBRO_SEQ;
-- DROP SEQUENCE P_ID_PRESTAMO_SEQ;

-- =====================================================================
-- 1. TABLAS CATALOGO
-- =====================================================================
CREATE TABLE TIPO_USUARIO (
    ID_TIPO_USUARIO NUMBER(*,0)    NOT NULL,
    NOMBRE_TIPO     VARCHAR2(200)  NOT NULL,
    CONSTRAINT TIPO_USUARIO_PK PRIMARY KEY (ID_TIPO_USUARIO)
);

CREATE TABLE TIPO_ESTADO (
    ID_ESTADO     NUMBER(*,0)    NOT NULL,
    NOMBRE_ESTADO VARCHAR2(200)  NOT NULL,
    CONSTRAINT TIPO_ESTADO_PK PRIMARY KEY (ID_ESTADO)
);

CREATE TABLE AUTOR (
    ID_AUTOR     NUMBER(*,0)    NOT NULL,
    NOMBRE_AUTOR VARCHAR2(800)  NOT NULL,
    CONSTRAINT AUTOR_PK PRIMARY KEY (ID_AUTOR)
);

-- =====================================================================
-- 2. TABLA USUARIO
-- =====================================================================
CREATE TABLE USUARIO (
    ID_USUARIO       NUMBER(*,0)    NOT NULL,
    RUT              NUMBER(8,0)    NOT NULL,
    DV               CHAR(4)        NOT NULL,
    NOMBRE           VARCHAR2(400)  NOT NULL,
    APELLIDO_P       VARCHAR2(400)  NOT NULL,
    APELLIDO_M       VARCHAR2(400)  NOT NULL,
    CORREO           VARCHAR2(600)  NOT NULL,
    FECHA_REGISTRO   DATE           NOT NULL,
    ID_TIPO_USUARIO  NUMBER(*,0)    NOT NULL,
    ACTIVO           NUMBER(1,0)    DEFAULT 1 NOT NULL,
    CONSTRAINT USUARIO_PK PRIMARY KEY (ID_USUARIO),
    CONSTRAINT USUARIO_DV_CK CHECK (DV IN ('0','1','2','3','5','6','7','8','9','K','k')),
    CONSTRAINT USUARIO_ACTIVO_CK CHECK (ACTIVO IN (0, 1)),
    CONSTRAINT USUARIO_TIPO_FK FOREIGN KEY (ID_TIPO_USUARIO)
        REFERENCES TIPO_USUARIO (ID_TIPO_USUARIO)
);

CREATE UNIQUE INDEX RUT_IDX ON USUARIO (RUT);

-- =====================================================================
-- 3. TABLA LIBRO
-- =====================================================================
CREATE TABLE LIBRO (
    ID_LIBRO          NUMBER(*,0)   NOT NULL,
    ISBN              VARCHAR2(52)  NOT NULL,
    TITULO            VARCHAR2(400) NOT NULL,
    ANIO_PUBLICACION  NUMBER(4,0)   NOT NULL,
    COPIAS_TOTALES    NUMBER        NOT NULL,
    COPIAS_DISPONIBLE NUMBER        NOT NULL,
    ID_AUTOR          NUMBER(*,0)   NOT NULL,
    CONSTRAINT LIBRO_PK PRIMARY KEY (ID_LIBRO),
    CONSTRAINT LIBRO_AUTOR_FK FOREIGN KEY (ID_AUTOR)
        REFERENCES AUTOR (ID_AUTOR)
);

CREATE UNIQUE INDEX ISBN_IDX ON LIBRO (ISBN);

-- =====================================================================
-- 4. TABLA PRESTAMO
-- =====================================================================
CREATE TABLE PRESTAMO (
    ID_PRESTAMO               NUMBER(*,0) NOT NULL,
    ID_USUARIO                NUMBER(*,0) NOT NULL,
    LIBRO_ID_LIBRO            NUMBER(*,0) NOT NULL,
    FECHA_PRESTAMO            DATE        NOT NULL,
    FECHA_DEVOLUCION_ESPERADA DATE        NOT NULL,
    FECHA_DEVOLUCION_REAL     DATE,
    ID_ESTADO                 NUMBER(*,0) NOT NULL,
    CONSTRAINT PRESTAMO_PK PRIMARY KEY (ID_PRESTAMO),
    CONSTRAINT PREST_USU_FK    FOREIGN KEY (ID_USUARIO)     REFERENCES USUARIO     (ID_USUARIO),
    CONSTRAINT PRESTLIBRO_FK   FOREIGN KEY (LIBRO_ID_LIBRO) REFERENCES LIBRO       (ID_LIBRO),
    CONSTRAINT PREST_TIPO_EST_FK FOREIGN KEY (ID_ESTADO)    REFERENCES TIPO_ESTADO (ID_ESTADO)
);

-- =====================================================================
-- 5. TABLA NOTIFICACIONES (auditoria de eventos Event Grid)
-- =====================================================================
CREATE TABLE NOTIFICACIONES (
    ID             NUMBER GENERATED BY DEFAULT AS IDENTITY,
    ID_USUARIO     VARCHAR2(50),
    TIPO           VARCHAR2(50)   NOT NULL,
    ASUNTO         VARCHAR2(200)  NOT NULL,
    CUERPO         VARCHAR2(2000) NOT NULL,
    ESTADO         VARCHAR2(20)   DEFAULT 'PENDIENTE' NOT NULL,
    FECHA_CREACION TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    FECHA_ENVIO    TIMESTAMP,
    CONSTRAINT NOTIFICACIONES_PK PRIMARY KEY (ID)
);

CREATE INDEX IDX_NOTIF_USUARIO ON NOTIFICACIONES (ID_USUARIO);
CREATE INDEX IDX_NOTIF_ESTADO  ON NOTIFICACIONES (ESTADO);
CREATE INDEX IDX_NOTIF_FECHA   ON NOTIFICACIONES (FECHA_CREACION);

-- =====================================================================
-- 6. TABLA EVENTO_PROCESADO (idempotencia de consumers Event Grid)
-- Cada evento lleva un ID unico (Event Grid event.id). Antes de aplicar
-- un side-effect (decrementar stock, cascade delete), el consumer
-- inserta aqui el event_id; si ya existe, se omite el procesamiento.
-- =====================================================================
CREATE TABLE EVENTO_PROCESADO (
    EVENT_ID        VARCHAR2(128) NOT NULL,
    EVENT_TYPE      VARCHAR2(80)  NOT NULL,
    FECHA_PROCESO   TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    DETALLE         VARCHAR2(2000),
    CONSTRAINT EVENTO_PROCESADO_PK PRIMARY KEY (EVENT_ID)
);

CREATE INDEX IDX_EVENTO_TIPO  ON EVENTO_PROCESADO (EVENT_TYPE);
CREATE INDEX IDX_EVENTO_FECHA ON EVENTO_PROCESADO (FECHA_PROCESO);

-- =====================================================================
-- 7. SECUENCIAS
-- =====================================================================
CREATE SEQUENCE TU_ID_TIPO_USUARIO_SEQ START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE TE_ID_ESTADO_SEQ       START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE A_ID_AUTOR_SEQ         START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE U_ID_USUARIO_SEQ       START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE L_ID_LIBRO_SEQ         START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE P_ID_PRESTAMO_SEQ      START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- =====================================================================
-- 8. DATOS SEMILLA - CATALOGOS
-- =====================================================================
INSERT INTO TIPO_USUARIO (ID_TIPO_USUARIO, NOMBRE_TIPO) VALUES (1, 'administrador');
INSERT INTO TIPO_USUARIO (ID_TIPO_USUARIO, NOMBRE_TIPO) VALUES (2, 'cliente');
INSERT INTO TIPO_USUARIO (ID_TIPO_USUARIO, NOMBRE_TIPO) VALUES (3, 'docente'); -- nuevo

INSERT INTO TIPO_ESTADO (ID_ESTADO, NOMBRE_ESTADO) VALUES (1, 'ACTIVO');
INSERT INTO TIPO_ESTADO (ID_ESTADO, NOMBRE_ESTADO) VALUES (2, 'DEVUELTO');
INSERT INTO TIPO_ESTADO (ID_ESTADO, NOMBRE_ESTADO) VALUES (3, 'ATRASADO');
INSERT INTO TIPO_ESTADO (ID_ESTADO, NOMBRE_ESTADO) VALUES (4, 'CANCELADO'); -- nuevo (cascade delete usuario)

-- =====================================================================
-- 9. DATOS SEMILLA - AUTORES
-- =====================================================================
INSERT INTO AUTOR (ID_AUTOR, NOMBRE_AUTOR) VALUES (1, 'J.R.R. Tolkien');
INSERT INTO AUTOR (ID_AUTOR, NOMBRE_AUTOR) VALUES (2, 'Gabriel García Márquez');
INSERT INTO AUTOR (ID_AUTOR, NOMBRE_AUTOR) VALUES (3, 'Isaac Asimov');
INSERT INTO AUTOR (ID_AUTOR, NOMBRE_AUTOR) VALUES (4, 'Isabel Allende');
INSERT INTO AUTOR (ID_AUTOR, NOMBRE_AUTOR) VALUES (5, 'George Orwell');
INSERT INTO AUTOR (ID_AUTOR, NOMBRE_AUTOR) VALUES (6, 'Agatha Christie');
INSERT INTO AUTOR (ID_AUTOR, NOMBRE_AUTOR) VALUES (7, 'Harper Lee');
INSERT INTO AUTOR (ID_AUTOR, NOMBRE_AUTOR) VALUES (8, 'Mario Vargas Llosa');
INSERT INTO AUTOR (ID_AUTOR, NOMBRE_AUTOR) VALUES (9, 'Ursula K. Le Guin'); -- nuevo

-- =====================================================================
-- 10. DATOS SEMILLA - USUARIOS
-- =====================================================================
INSERT INTO USUARIO (ID_USUARIO, RUT, DV, NOMBRE, APELLIDO_P, APELLIDO_M, CORREO, FECHA_REGISTRO, ID_TIPO_USUARIO, ACTIVO)
VALUES (1, 11111111, '1', 'Daniel', 'Admin',     'Root',   'admin@biblioteca.cl',   DATE '2025-01-10', 1, 1);
INSERT INTO USUARIO (ID_USUARIO, RUT, DV, NOMBRE, APELLIDO_P, APELLIDO_M, CORREO, FECHA_REGISTRO, ID_TIPO_USUARIO, ACTIVO)
VALUES (2, 12345678, '5', 'Juan',   'Pérez',    'Soto',   'juan.perez@correo.cl',  DATE '2026-02-15', 2, 0);
INSERT INTO USUARIO (ID_USUARIO, RUT, DV, NOMBRE, APELLIDO_P, APELLIDO_M, CORREO, FECHA_REGISTRO, ID_TIPO_USUARIO, ACTIVO)
VALUES (3, 23456789, 'K', 'María',  'González', 'Tapia',  'maria.g@correo.cl',     DATE '2026-02-20', 2, 1);
INSERT INTO USUARIO (ID_USUARIO, RUT, DV, NOMBRE, APELLIDO_P, APELLIDO_M, CORREO, FECHA_REGISTRO, ID_TIPO_USUARIO, ACTIVO)
VALUES (4, 98765432, '0', 'Carlos', 'Molina',   'Rojas',  'cmolina@correo.cl',     DATE '2026-03-05', 2, 1);
INSERT INTO USUARIO (ID_USUARIO, RUT, DV, NOMBRE, APELLIDO_P, APELLIDO_M, CORREO, FECHA_REGISTRO, ID_TIPO_USUARIO, ACTIVO)
VALUES (5, 19283746, '5', 'Ana',    'Silva',    'Gómez',  'asilva@correo.cl',      DATE '2026-03-10', 2, 1);
INSERT INTO USUARIO (ID_USUARIO, RUT, DV, NOMBRE, APELLIDO_P, APELLIDO_M, CORREO, FECHA_REGISTRO, ID_TIPO_USUARIO, ACTIVO)
VALUES (6, 10000006, '9', 'Pedro',  'Soto',     'García', 'pedro.soto@correo.cl',  DATE '2026-03-31', 2, 1);
-- nuevo: docente que pidio el libro de Le Guin
INSERT INTO USUARIO (ID_USUARIO, RUT, DV, NOMBRE, APELLIDO_P, APELLIDO_M, CORREO, FECHA_REGISTRO, ID_TIPO_USUARIO, ACTIVO)
VALUES (7, 17654321, '8', 'Camila', 'Fernández', 'Núñez', 'camila.f@correo.cl',    DATE '2026-05-08', 3, 1);

-- =====================================================================
-- 11. DATOS SEMILLA - LIBROS
-- =====================================================================
INSERT INTO LIBRO (ID_LIBRO, ISBN, TITULO, ANIO_PUBLICACION, COPIAS_TOTALES, COPIAS_DISPONIBLE, ID_AUTOR)
VALUES (1, '9788445070316', 'La Comunidad del Anillo',    1954,  5,  4, 1);
INSERT INTO LIBRO (ID_LIBRO, ISBN, TITULO, ANIO_PUBLICACION, COPIAS_TOTALES, COPIAS_DISPONIBLE, ID_AUTOR)
VALUES (2, '9788445071757', 'El Hobbit',                  1937,  3,  3, 1);
INSERT INTO LIBRO (ID_LIBRO, ISBN, TITULO, ANIO_PUBLICACION, COPIAS_TOTALES, COPIAS_DISPONIBLE, ID_AUTOR)
VALUES (3, '9780307474728', 'Cien Años de Soledad',       1967, 10,  8, 2);
INSERT INTO LIBRO (ID_LIBRO, ISBN, TITULO, ANIO_PUBLICACION, COPIAS_TOTALES, COPIAS_DISPONIBLE, ID_AUTOR)
VALUES (4, '9788497592208', 'Fundación',                  1951,  4,  4, 3);
INSERT INTO LIBRO (ID_LIBRO, ISBN, TITULO, ANIO_PUBLICACION, COPIAS_TOTALES, COPIAS_DISPONIBLE, ID_AUTOR)
VALUES (5, '9788401322889', 'La Casa de los Espíritus',   1982,  6,  6, 4);
INSERT INTO LIBRO (ID_LIBRO, ISBN, TITULO, ANIO_PUBLICACION, COPIAS_TOTALES, COPIAS_DISPONIBLE, ID_AUTOR)
VALUES (6, '9780061120084', 'To Kill a Mockingbird',      1960,  3,  2, 2);
-- nuevo: libro asociado al autor 9 (Le Guin) y prestable al usuario 7
INSERT INTO LIBRO (ID_LIBRO, ISBN, TITULO, ANIO_PUBLICACION, COPIAS_TOTALES, COPIAS_DISPONIBLE, ID_AUTOR)
VALUES (7, '9780553262506', 'Un Mago de Terramar',        1968,  3,  2, 9);

-- =====================================================================
-- 12. DATOS SEMILLA - PRESTAMOS
-- =====================================================================
INSERT INTO PRESTAMO (ID_PRESTAMO, ID_USUARIO, LIBRO_ID_LIBRO, FECHA_PRESTAMO, FECHA_DEVOLUCION_ESPERADA, FECHA_DEVOLUCION_REAL, ID_ESTADO)
VALUES (1, 2, 1, DATE '2026-03-01', DATE '2026-03-15', NULL,             3);
INSERT INTO PRESTAMO (ID_PRESTAMO, ID_USUARIO, LIBRO_ID_LIBRO, FECHA_PRESTAMO, FECHA_DEVOLUCION_ESPERADA, FECHA_DEVOLUCION_REAL, ID_ESTADO)
VALUES (2, 3, 3, DATE '2026-03-10', DATE '2026-03-24', DATE '2026-03-20', 2);
INSERT INTO PRESTAMO (ID_PRESTAMO, ID_USUARIO, LIBRO_ID_LIBRO, FECHA_PRESTAMO, FECHA_DEVOLUCION_ESPERADA, FECHA_DEVOLUCION_REAL, ID_ESTADO)
VALUES (3, 4, 3, DATE '2026-03-20', DATE '2026-04-03', NULL,             1);
INSERT INTO PRESTAMO (ID_PRESTAMO, ID_USUARIO, LIBRO_ID_LIBRO, FECHA_PRESTAMO, FECHA_DEVOLUCION_ESPERADA, FECHA_DEVOLUCION_REAL, ID_ESTADO)
VALUES (4, 5, 1, DATE '2026-03-25', DATE '2026-03-27', NULL,             1);
INSERT INTO PRESTAMO (ID_PRESTAMO, ID_USUARIO, LIBRO_ID_LIBRO, FECHA_PRESTAMO, FECHA_DEVOLUCION_ESPERADA, FECHA_DEVOLUCION_REAL, ID_ESTADO)
VALUES (5, 6, 1, DATE '2026-05-02', DATE '2026-05-16', DATE '2026-05-16', 2);
-- nuevo: prestamo del usuario 7 (Camila) sobre el libro 7 (Le Guin),
-- coherente con COPIAS_DISPONIBLE = 2 ya decrementada arriba (3 - 1)
INSERT INTO PRESTAMO (ID_PRESTAMO, ID_USUARIO, LIBRO_ID_LIBRO, FECHA_PRESTAMO, FECHA_DEVOLUCION_ESPERADA, FECHA_DEVOLUCION_REAL, ID_ESTADO)
VALUES (6, 7, 7, DATE '2026-05-08', DATE '2026-05-22', NULL,             1);

-- =====================================================================
-- 13. DATOS SEMILLA - NOTIFICACION asociada al prestamo nuevo
-- =====================================================================
INSERT INTO NOTIFICACIONES (ID_USUARIO, TIPO, ASUNTO, CUERPO, ESTADO)
VALUES ('7', 'PRESTAMO_CREADO', 'Préstamo registrado',
        'Hola Camila, su préstamo #6 del libro 7 (Un Mago de Terramar) ha sido registrado correctamente. Recuerde devolverlo antes del 2026-05-22.',
        'PENDIENTE');

COMMIT;

-- =====================================================================
-- 14. AJUSTE DE SECUENCIAS (siguiente valor disponible)
-- =====================================================================
-- Tras los inserts anteriores, dejamos las secuencias listas para el
-- siguiente registro. Si la BD ya tiene secuencias activas con last_number
-- mas alto, NO ejecutar este bloque.
-- ALTER SEQUENCE TU_ID_TIPO_USUARIO_SEQ RESTART START WITH 4;
-- ALTER SEQUENCE TE_ID_ESTADO_SEQ       RESTART START WITH 5;
-- ALTER SEQUENCE A_ID_AUTOR_SEQ         RESTART START WITH 10;
-- ALTER SEQUENCE U_ID_USUARIO_SEQ       RESTART START WITH 8;
-- ALTER SEQUENCE L_ID_LIBRO_SEQ         RESTART START WITH 8;
-- ALTER SEQUENCE P_ID_PRESTAMO_SEQ      RESTART START WITH 7;
