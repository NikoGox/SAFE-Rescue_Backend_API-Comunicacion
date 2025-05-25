package com.SAFE_Rescue.API_Comunicacion.service;

import com.SAFE_Rescue.API_Comunicacion.modelo.BorradorMensaje;
import com.SAFE_Rescue.API_Comunicacion.repository.BorradorMensajeRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Transactional
@Service
public class BorradorMensajeService {

    private final BorradorMensajeRepository borradorMensajeRepository;

    @Autowired
    public BorradorMensajeService(BorradorMensajeRepository borradorMensajeRepository) {
        this.borradorMensajeRepository = borradorMensajeRepository;
    }

    public BorradorMensaje guardarBorrador(BorradorMensaje borrador) {
        if (borrador.getFechaBrdrMensaje() == null) {
            borrador.setFechaBrdrMensaje(new Date());
        }
        borrador.setBorradorEnviado(false);
        return borradorMensajeRepository.save(borrador);
    }

    public BorradorMensaje actualizarBorrador(int idBorrador, BorradorMensaje datosActualizados) {
        Optional<BorradorMensaje> borradorExistente = borradorMensajeRepository.findById(idBorrador);

        if (borradorExistente.isPresent()) {
            BorradorMensaje borrador = borradorExistente.get();

            if (borrador.isBorradorEnviado()) {
                throw new IllegalStateException("No se puede actualizar un borrador que ya ha sido enviado.");
            }

            if (datosActualizados.getBrdrTitulo() != null) {
                borrador.setBrdrTitulo(datosActualizados.getBrdrTitulo());
            }
            if (datosActualizados.getBrdrContenido() != null) {
                borrador.setBrdrContenido(datosActualizados.getBrdrContenido());
            }

            return borradorMensajeRepository.save(borrador);
        } else {
            throw new RuntimeException("Borrador no encontrado con ID: " + idBorrador);
        }
    }

    public BorradorMensaje enviarBorrador(int idBorrador) {
        Optional<BorradorMensaje> borradorExistente = borradorMensajeRepository.findById(idBorrador);

        if (borradorExistente.isPresent()) {
            BorradorMensaje borrador = borradorExistente.get();

            if (borrador.isBorradorEnviado()) {
                throw new IllegalStateException("El borrador con ID: " + idBorrador + " ya ha sido enviado.");
            }

            borrador.setBorradorEnviado(true);
            return borradorMensajeRepository.save(borrador);
        } else {
            throw new RuntimeException("Borrador no encontrado con ID: " + idBorrador + " para enviar.");
        }
    }

    public List<BorradorMensaje> obtenerTodosLosBorradores() {
        return borradorMensajeRepository.findAll();
    }

    public Optional<BorradorMensaje> obtenerBorradorPorId(int id) {
        return borradorMensajeRepository.findById(id);
    }

    public void eliminarBorrador(int id) {
        if (borradorMensajeRepository.existsById(id)) {
            BorradorMensaje borrador = borradorMensajeRepository.findById(id).orElseThrow(
                    () -> new RuntimeException("Error al obtener borrador para eliminar con ID: " + id)
            );
            if (borrador.isBorradorEnviado()) {
                throw new IllegalStateException("No se puede eliminar un borrador que ya ha sido enviado.");
            }
            borradorMensajeRepository.deleteById(id);
        } else {
            throw new RuntimeException("Borrador no encontrado con ID: " + id + " para eliminar.");
        }
    }
}