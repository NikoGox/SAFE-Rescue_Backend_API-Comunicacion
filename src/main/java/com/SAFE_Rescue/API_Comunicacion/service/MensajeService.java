package com.SAFE_Rescue.API_Comunicacion.service;

import com.SAFE_Rescue.API_Comunicacion.modelo.BorradorMensaje;
import com.SAFE_Rescue.API_Comunicacion.modelo.Mensaje;
import com.SAFE_Rescue.API_Comunicacion.repository.BorradorMensajeRepository;
import com.SAFE_Rescue.API_Comunicacion.repository.MensajeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class MensajeService {

    private final MensajeRepository mensajeRepository;
    private final BorradorMensajeRepository borradorMensajeRepository;

    @Autowired
    public MensajeService(MensajeRepository mensajeRepository, BorradorMensajeRepository borradorMensajeRepository) {
        this.mensajeRepository = mensajeRepository;
        this.borradorMensajeRepository = borradorMensajeRepository;
    }

    @Transactional
    public Mensaje crearMensajeDesdeBorradorYReceptor(int idBorrador, int idReceptor) {
        Optional<BorradorMensaje> borradorOptional = borradorMensajeRepository.findById(idBorrador);

        if (borradorOptional.isEmpty()) {
            throw new RuntimeException("Borrador con ID " + idBorrador + " no encontrado para crear el mensaje.");
        }

        BorradorMensaje borrador = borradorOptional.get();


        Mensaje nuevoMensaje = new Mensaje();
        nuevoMensaje.setIdEmisor(borrador.getIdBrdrEmisor());
        nuevoMensaje.setIdReceptor(idReceptor);
        nuevoMensaje.setFechaMensaje(new Date());
        nuevoMensaje.setTitulo(borrador.getBrdrTitulo());
        nuevoMensaje.setContenido(borrador.getBrdrContenido());
        nuevoMensaje.setBorradorOriginal(borrador);

        Mensaje mensajeGuardado = mensajeRepository.save(nuevoMensaje);

        borrador.setBorradorEnviado(true);
        borradorMensajeRepository.save(borrador);

        return mensajeGuardado;
    }


    public List<Mensaje> obtenerTodosLosMensajes() {
        return mensajeRepository.findAll();
    }

    public Optional<Mensaje> obtenerMensajePorId(int idMensaje) {
        return mensajeRepository.findById(idMensaje);
    }

    @Transactional
    public void eliminarMensaje(int idMensaje) {
        if (mensajeRepository.existsById(idMensaje)) {
            mensajeRepository.deleteById(idMensaje);
        } else {
            throw new RuntimeException("Mensaje no encontrado con ID: " + idMensaje + " para eliminar.");
        }
    }
}