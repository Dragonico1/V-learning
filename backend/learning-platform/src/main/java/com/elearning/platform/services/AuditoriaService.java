package com.elearning.platform.services;

import com.elearning.platform.entity.RegistroAuditoria;
import com.elearning.platform.enums.ResultadoAuditoria;
import com.elearning.platform.repository.RegistroAuditoriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Auditoría de solo inserción (RF-013). Nunca debe tumbar la operación auditada. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final RegistroAuditoriaRepository repositorio;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(Long usuarioId, String accion, String recurso, ResultadoAuditoria resultado, String ip) {
        try {
            RegistroAuditoria r = new RegistroAuditoria();
            r.setUsuarioId(usuarioId);
            r.setAccion(recortar(accion, 150));
            r.setRecurso(recortar(recurso, 200));
            r.setResultado(resultado);
            r.setIpOrigen(recortar(ip, 45));
            repositorio.save(r);
        } catch (RuntimeException e) {
            log.error("No se pudo registrar la auditoría ({}): {}", accion, e.getMessage());
        }
    }

    private static String recortar(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
