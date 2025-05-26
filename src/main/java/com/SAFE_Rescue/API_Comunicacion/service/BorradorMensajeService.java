package com.SAFE_Rescue.API_Comunicacion.service;

import com.SAFE_Rescue.API_Comunicacion.modelo.BorradorMensaje;
import com.SAFE_Rescue.API_Comunicacion.repository.BorradorMensajeRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

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

    @Transactional
    public BorradorMensaje actualizarBorrador(int idBorrador, BorradorMensaje datosActualizados) {
        Optional<BorradorMensaje> borradorExistente = borradorMensajeRepository.findById(idBorrador);

        if (borradorExistente.isPresent()) {
            BorradorMensaje borrador = borradorExistente.get();

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


    public List<BorradorMensaje> obtenerTodosLosBorradores() {
        return borradorMensajeRepository.findAll();
    }

    public Optional<BorradorMensaje> obtenerBorradorPorId(int id) {
        return borradorMensajeRepository.findById(id);
    }

    @Transactional
    public void eliminarBorrador(int id) {
        if (borradorMensajeRepository.existsById(id)) {
            borradorMensajeRepository.deleteById(id);
        } else {
            throw new RuntimeException("Borrador no encontrado con ID: " + id + " para eliminar.");
        }
    }
}