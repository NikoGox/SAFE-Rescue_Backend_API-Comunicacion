package com.SAFE_Rescue.API_Comunicacion.controller;

import com.SAFE_Rescue.API_Comunicacion.modelo.Mensaje;
import com.SAFE_Rescue.API_Comunicacion.service.MensajeService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel; // <-- HATEOAS
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException; // Para manejo de errores 404
import java.util.Optional;
import java.util.stream.Collectors; // Para procesar listas con HATEOAS

// Importaciones para OpenAPI/Swagger
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*; // <-- HATEOAS


/**
 * Controlador REST para la gestión de Mensajes enviados.
 * Proporciona endpoints para crear, obtener y eliminar mensajes.
 */
@RestController
@RequestMapping("/api-comunicacion/v1/mensajes")
@Tag(name = "Mensajes", description = "Operaciones de CRUD relacionadas con mensajes enviados.") // <-- Anotación Swagger
public class MensajeController {

    private final MensajeService mensajeService;

    /**
     * Constructor para inyección de dependencias del servicio de mensajes.
     * @param mensajeService El servicio que maneja la lógica de negocio de los mensajes.
     */
    @Autowired
    public MensajeController(MensajeService mensajeService) {
        this.mensajeService = mensajeService;
    }

    /**
     * DTO para la solicitud de creación de un mensaje.
     * Contiene el ID del borrador original y el ID del receptor.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class CrearMensajeRequest {
        @Schema(description = "ID del borrador original a partir del cual se crea el mensaje", example = "1", required = true)
        private int idBorradorOriginal;
        @Schema(description = "ID del receptor del mensaje", example = "101", required = true)
        private int idReceptor;
    }

    /**
     * Crea un nuevo mensaje a partir de un borrador.
     * @param request Objeto con el ID del borrador y el ID del receptor.
     * @return El mensaje creado con estado 201 Created y enlaces HATEOAS.
     */
    @PostMapping
    @Operation(summary = "Crear un nuevo mensaje desde un borrador", description = "Crea un mensaje marcando el borrador original como enviado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Mensaje creado con éxito.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Mensaje.class))),
            @ApiResponse(responseCode = "400", description = "Error en la solicitud, datos inválidos o borrador ya enviado."),
            @ApiResponse(responseCode = "404", description = "Borrador original no encontrado."),
            @ApiResponse(responseCode = "409", description = "El borrador original ya ha sido enviado."),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor.")
    })
    public ResponseEntity<Mensaje> crearMensaje(
            @RequestBody @Parameter(description = "Datos para crear un mensaje (ID del borrador y ID del receptor)", required = true)
            CrearMensajeRequest request) {
        try {
            Mensaje nuevoMensaje = mensajeService.crearMensajeDesdeBorradorYReceptor(
                    request.getIdBorradorOriginal(),
                    request.getIdReceptor()
            );
            // Añadir enlaces HATEOAS al mensaje creado
            nuevoMensaje.add(linkTo(methodOn(MensajeController.class).obtenerMensajePorId(nuevoMensaje.getIdMensaje())).withSelfRel());
            nuevoMensaje.add(linkTo(methodOn(MensajeController.class).obtenerTodosLosMensajes()).withRel("todos-los-mensajes"));
            return new ResponseEntity<>(nuevoMensaje, HttpStatus.CREATED);
        } catch (NoSuchElementException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND); // Borrador no encontrado
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(HttpStatus.CONFLICT); // Borrador ya enviado
        } catch (RuntimeException e) {
            // Para otros errores generales de negocio/validación del servicio
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Obtiene una lista de todos los mensajes enviados.
     * @return Lista de mensajes con estado 200 OK y enlaces HATEOAS.
     */
    @GetMapping
    @Operation(summary = "Obtener todos los mensajes enviados", description = "Obtiene una lista con todos los mensajes, incluyendo enlaces HATEOAS.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de mensajes obtenida exitosamente.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Mensaje.class))), // Schema indicará la lista
            @ApiResponse(responseCode = "204", description = "No hay mensajes registrados.")
    })
    public ResponseEntity<CollectionModel<Mensaje>> obtenerTodosLosMensajes() {
        List<Mensaje> mensajes = mensajeService.obtenerTodosLosMensajes();

        if (mensajes.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        // Añadir enlaces 'self' a cada mensaje en la lista
        List<Mensaje> mensajesConEnlaces = mensajes.stream()
                .map(mensaje -> {
                    return mensaje.add(linkTo(methodOn(MensajeController.class).obtenerMensajePorId(mensaje.getIdMensaje())).withSelfRel());
                })
                .collect(Collectors.toList());

        // Envolver la colección con un enlace 'self' a la colección misma
        CollectionModel<Mensaje> recursos = CollectionModel.of(mensajesConEnlaces,
                linkTo(methodOn(MensajeController.class).obtenerTodosLosMensajes()).withSelfRel());

        return new ResponseEntity<>(recursos, HttpStatus.OK);
    }

    /**
     * Obtiene un mensaje por su ID.
     * @param id El ID del mensaje a buscar.
     * @return El mensaje encontrado con estado 200 OK y enlaces HATEOAS, o 404 Not Found si no existe.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un mensaje por su ID", description = "Obtiene un mensaje específico por su ID, incluyendo enlaces HATEOAS.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mensaje encontrado.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Mensaje.class))),
            @ApiResponse(responseCode = "404", description = "Mensaje no encontrado.")
    })
    public ResponseEntity<Mensaje> obtenerMensajePorId(
            @Parameter(description = "ID del mensaje a buscar", required = true)
            @PathVariable int id) {
        Optional<Mensaje> mensaje = mensajeService.obtenerMensajePorId(id);
        return mensaje.map(m -> {
                    // Enlaces HATEOAS para el mensaje individual
                    m.add(linkTo(methodOn(MensajeController.class).obtenerMensajePorId(m.getIdMensaje())).withSelfRel());
                    m.add(linkTo(methodOn(MensajeController.class).obtenerTodosLosMensajes()).withRel("todos-los-mensajes"));
                    m.add(linkTo(methodOn(MensajeController.class).eliminarMensaje(m.getIdMensaje())).withRel("eliminar-mensaje"));
                    // Opcional: enlace al borrador original si borradorOriginal no fuera JsonIgnored
                    // if (m.getBorradorOriginal() != null) {
                    //     m.add(linkTo(methodOn(BorradorMensajeController.class).obtenerBorradorPorId(m.getBorradorOriginal().getIdBrdrMensaje())).withRel("borrador-original"));
                    // }
                    return new ResponseEntity<>(m, HttpStatus.OK);
                })
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * Elimina un mensaje por su ID.
     * @param id El ID del mensaje a eliminar.
     * @return Estado 204 No Content si la eliminación es exitosa, o 404 Not Found si el mensaje no existe.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un mensaje", description = "Elimina un mensaje del sistema por su ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Mensaje eliminado con éxito."),
            @ApiResponse(responseCode = "404", description = "Mensaje no encontrado."),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor.")
    })
    public ResponseEntity<Void> eliminarMensaje(
            @Parameter(description = "ID del mensaje a eliminar", required = true)
            @PathVariable int id) {
        try {
            mensajeService.eliminarMensaje(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT); // 204 No Content
        } catch (NoSuchElementException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND); // Mensaje no encontrado
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}