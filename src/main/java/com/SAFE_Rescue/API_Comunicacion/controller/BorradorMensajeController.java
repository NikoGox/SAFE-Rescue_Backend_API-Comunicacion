package com.SAFE_Rescue.API_Comunicacion.controller;

import com.SAFE_Rescue.API_Comunicacion.modelo.BorradorMensaje;
import com.SAFE_Rescue.API_Comunicacion.service.BorradorMensajeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api-administrador/v1/borradores-mensajes")
public class BorradorMensajeController {

    private final BorradorMensajeService borradorMensajeService;

    @Autowired
    public BorradorMensajeController(BorradorMensajeService borradorMensajeService) {
        this.borradorMensajeService = borradorMensajeService;
    }

    @PostMapping
    public ResponseEntity<BorradorMensaje> crearBorrador(@RequestBody BorradorMensaje borradorMensaje) {
        BorradorMensaje nuevoBorrador = borradorMensajeService.guardarBorrador(borradorMensaje);
        return new ResponseEntity<>(nuevoBorrador, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<BorradorMensaje>> obtenerTodosLosBorradores() {
        List<BorradorMensaje> borradores = borradorMensajeService.obtenerTodosLosBorradores();
        return new ResponseEntity<>(borradores, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BorradorMensaje> obtenerBorradorPorId(@PathVariable int id) {
        Optional<BorradorMensaje> borrador = borradorMensajeService.obtenerBorradorPorId(id);
        return borrador.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<BorradorMensaje> actualizarBorrador(
            @PathVariable("id") int idBorrador,
            @RequestBody BorradorMensaje datosActualizados) {
        try {
            BorradorMensaje borradorActualizado = borradorMensajeService.actualizarBorrador(idBorrador, datosActualizados);
            return new ResponseEntity<>(borradorActualizado, HttpStatus.OK);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/{id}/enviar")
    public ResponseEntity<BorradorMensaje> enviarBorrador(@PathVariable int id) {
        try {
            BorradorMensaje borradorEnviado = borradorMensajeService.enviarBorrador(id);
            return new ResponseEntity<>(borradorEnviado, HttpStatus.OK);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarBorrador(@PathVariable int id) {
        try {
            borradorMensajeService.eliminarBorrador(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}