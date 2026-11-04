package com.elearning.platform.services;

import com.elearning.platform.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/** Guarda las portadas subidas por los instructores y devuelve su URL pública. */

@Service
public class PortadaService {

    private static final long TAMANO_MAXIMO = 5L * 1024 * 1024;

    private final Path directorio;

    public PortadaService(@Value("${vlearning.uploads.dir:uploads}") String carpeta) {
        this.directorio = Paths.get(carpeta, "portadas").toAbsolutePath().normalize();
    }

    public Path directorio() {
        return directorio;
    }

    public String guardar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw ApiException.solicitudInvalida("PORTADA_VACIA", "Selecciona una imagen para la portada.");
        }
        if (archivo.getSize() > TAMANO_MAXIMO) {
            throw ApiException.solicitudInvalida("PORTADA_MUY_GRANDE", "La imagen no puede superar los 5 MB.");
        }
        String extension = extension(archivo.getContentType());
        if (extension == null) {
            throw ApiException.solicitudInvalida("PORTADA_FORMATO",
                    "Usa una imagen JPG, PNG, WEBP o GIF.");
        }
        try {
            Files.createDirectories(directorio);
            String nombre = UUID.randomUUID() + "." + extension;
            archivo.transferTo(directorio.resolve(nombre));
            return "/api/archivos/portadas/" + nombre;
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PORTADA_NO_GUARDADA",
                    "No pudimos guardar la imagen. Inténtalo de nuevo.");
        }
    }

    private static String extension(String tipo) {
        if (tipo == null) return null;
        return switch (tipo.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> null;
        };
    }
}
