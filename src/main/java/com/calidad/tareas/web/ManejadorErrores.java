package com.calidad.tareas.web;

import com.calidad.tareas.dominio.TareaNoEncontradaException;
import com.calidad.tareas.dominio.TransicionInvalidaException;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce excepciones de dominio a respuestas RFC 9457 (Problem Details).
 */
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(TareaNoEncontradaException.class)
    public ProblemDetail noEncontrada(TareaNoEncontradaException ex) {
        return problema(HttpStatus.NOT_FOUND, "Tarea no encontrada", ex.getMessage());
    }

    @ExceptionHandler(TransicionInvalidaException.class)
    public ProblemDetail transicionInvalida(TransicionInvalidaException ex) {
        return problema(HttpStatus.CONFLICT, "Transición de estado inválida", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacion(MethodArgumentNotValidException ex) {
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Datos inválidos",
                "La petición contiene campos inválidos");
        Map<String, String> errores = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField,
                        e -> String.valueOf(e.getDefaultMessage()), (primero, segundo) -> primero));
        problema.setProperty("errores", errores);
        return problema;
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalle);
        problema.setTitle(titulo);
        return problema;
    }
}
