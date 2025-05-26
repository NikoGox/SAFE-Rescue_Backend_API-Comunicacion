package com.SAFE_Rescue.API_Comunicacion.controller;

import com.SAFE_Rescue.API_Comunicacion.modelo.Mensaje;
import com.SAFE_Rescue.API_Comunicacion.service.MensajeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@RestController
@RequestMapping("/api-ciudadano/v1/mensajes")
public class MensajeController {

    private final MensajeService mensajeService;

    @Autowired
    public MensajeController(MensajeService mensajeService) {
        this.mensajeService = mensajeService;
    }

    @PostMapping
    public ResponseEntity<Mensaje> crearMensaje(@RequestBody CrearMensajeRequest request) {
        try {
            Mensaje nuevoMensaje = mensajeService.crearMensajeDesdeBorradorYReceptor(
                    request.getIdBorradorOriginal(),
                    request.getIdReceptor()
            );
            return new ResponseEntity<>(nuevoMensaje, HttpStatus.CREATED);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }


    @GetMapping
    public ResponseEntity<List<Mensaje>> obtenerTodosLosMensajes() {
        List<Mensaje> mensajes = mensajeService.obtenerTodosLosMensajes();
        return new ResponseEntity<>(mensajes, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Mensaje> obtenerMensajePorId(@PathVariable int id) {
        Optional<Mensaje> mensaje = mensajeService.obtenerMensajePorId(id);
        return mensaje.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarMensaje(@PathVariable int id) {
        try {
            mensajeService.eliminarMensaje(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class CrearMensajeRequest {
        private int idBorradorOriginal;
        private int idReceptor;
    }
}