package com.elearning.platform.dto;

import com.elearning.platform.enums.CategoriaAccesibilidad;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** DTOs de accesibilidad (RF-014, RF-016). */
public final class AccesibilidadDtos {

    private AccesibilidadDtos() {}

    public record ConfiguracionDto(
            boolean altoContraste,
            @NotNull(message = "Indica el tamaño del texto.") @Min(value = 12, message = "El tamaño mínimo es 12 px.")
            @Max(value = 40, message = "El tamaño máximo es 40 px.") Integer tamanoFuente,
            @Size(max = 100, message = "Máximo 100 caracteres.") String tipografia,
            @NotNull(message = "Indica el interlineado.") @DecimalMin(value = "1.0", message = "El interlineado mínimo es 1.0.")
            @DecimalMax(value = "3.0", message = "El interlineado máximo es 3.0.") BigDecimal espaciadoLinea,
            boolean navegacionTeclado,
            boolean lectorPantalla,
            boolean subtitulos,
            boolean transcripcion,
            boolean textoAVoz,
            @NotNull(message = "Indica el tiempo adicional.") @Min(value = 0, message = "No puede ser negativo.")
            @Max(value = 200, message = "El máximo es 200 %.") Integer tiempoAdicional) {}

    public record PerfilAccesibilidadRequest(@NotNull(message = "Envía la lista de categorías (puede estar vacía).")
                                             Set<CategoriaAccesibilidad> categorias) {}

    public record PerfilAccesibilidadRespuesta(
            boolean configurado,
            Set<CategoriaAccesibilidad> categorias,
            ConfiguracionDto configuracion,
            LocalDateTime fechaActualizacion) {}

    /** Vista previa sin guardar (RF-014, extensión «previsualizar»). */
    public record PrevisualizacionRespuesta(Set<CategoriaAccesibilidad> categorias, ConfiguracionDto configuracion) {}

    /** Resultado de guardar ajustes: avisos de legibilidad y combinación sugerida (RF-016). */
    public record ConfiguracionRespuesta(ConfiguracionDto configuracion, List<String> advertencias,
                                         ConfiguracionDto sugerencia) {}

    public record TemaRespuesta(String id, String nombre, String texto, String fondo, double ratio,
                                boolean cumpleAA, boolean cumpleAAA) {}
}
