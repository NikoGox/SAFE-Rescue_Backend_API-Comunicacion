package com.SAFE_Rescue.API_Comunicacion.controller;

import com.SAFE_Rescue.API_Comunicacion.modelo.BorradorMensaje;
import com.SAFE_Rescue.API_Comunicacion.service.BorradorMensajeService;
import io.swagger.v3.oas.annotations.Operation; // <-- Importaciones de OpenAPI
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel; // <-- HATEOAS
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException; // Para manejo de errores
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*; // <-- HATEOAS


/**
 * Controlador REST para la gestión de Borradores de Mensajes.
 * Proporciona endpoints para crear, obtener, actualizar y eliminar borradores.
 */
@RestController
@RequestMapping("/api-comunicacion/v1/borradores-mensajes")
@Tag(name = "Borradores de Mensajes", description = "Operaciones de CRUD relacionadas con borradores de mensajes y sus interacciones.")
public class BorradorMensajeController {

    private final BorradorMensajeService borradorMensajeService;

    /**
     * Constructor para inyección de dependencias del servicio de borradores.
     * @param borradorMensajeService El servicio que maneja la lógica de negocio de los borradores.
     */
    @Autowired
    public BorradorMensajeController(BorradorMensajeService borradorMensajeService) {
        this.borradorMensajeService = borradorMensajeService;
    }

    /**
     * Crea un nuevo borrador de mensaje.
     * @param borradorMensaje Objeto BorradorMensaje con los datos para crear.
     * @return El borrador creado con estado 201 Created y enlaces HATEOAS.
     */
    @PostMapping
    @Operation(summary = "Crear un nuevo borrador de mensaje", description = "Crea un nuevo borrador de mensaje en el sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Borrador de mensaje creado con éxito.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BorradorMensaje.class))), // Devuelve el objeto con HATEOAS
            @ApiResponse(responseCode = "400", description = "Error en la solicitud, datos inválidos."),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor.")
    })
    public ResponseEntity<BorradorMensaje> crearBorrador(
            @RequestBody @Parameter(description = "Datos del borrador de mensaje a crear", required = true)
            BorradorMensaje borradorMensaje) {
        try {
            // Lógica para asignar fecha y estado por defecto, si es necesario.
            if (borradorMensaje.getFechaBrdrMensaje() == null) {
                borradorMensaje.setFechaBrdrMensaje(new Date());
            }
            borradorMensaje.setBorradorEnviado(false); // Asegura que al crear, sea un borrador

            BorradorMensaje nuevoBorrador = borradorMensajeService.guardarBorrador(borradorMensaje);

            // Añadir enlaces al borrador recién creado: 'self' y 'todos-los-borradores'.
            nuevoBorrador.add(linkTo(methodOn(BorradorMensajeController.class).obtenerBorradorPorId(nuevoBorrador.getIdBrdrMensaje())).withSelfRel());
            nuevoBorrador.add(linkTo(methodOn(BorradorMensajeController.class).obtenerTodosLosBorradores()).withRel("todos-los-borradores"));

            return new ResponseEntity<>(nuevoBorrador, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            // Manejar errores de lógica de negocio o validación, si tu servicio los lanza
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST); // O un mensaje más específico
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Obtiene una lista de todos los borradores de mensajes.
     * @return Lista de borradores con estado 200 OK y enlaces HATEOAS.
     */
    @GetMapping
    @Operation(summary = "Obtener todos los borradores de mensajes", description = "Obtiene una lista con todos los borradores de mensajes con enlaces HATEOAS.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de borradores obtenida exitosamente.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BorradorMensaje.class))), // Swagger entenderá la lista con enlaces
            @ApiResponse(responseCode = "204", description = "No hay borradores de mensajes registrados.")
    })
    public ResponseEntity<CollectionModel<BorradorMensaje>> obtenerTodosLosBorradores() {
        List<BorradorMensaje> borradores = borradorMensajeService.obtenerTodosLosBorradores();

        if (borradores.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        // Para cada borrador en la lista, añadimos un enlace 'self' que apunta a su URL individual.
        List<BorradorMensaje> borradoresConEnlaces = borradores.stream()
                .map(borrador -> {
                    return borrador.add(linkTo(methodOn(BorradorMensajeController.class).obtenerBorradorPorId(borrador.getIdBrdrMensaje())).withSelfRel());
                })
                .collect(Collectors.toList());

        // Envuelve la colección de borradores con un enlace 'self' que apunta a la URL de la colección.
        CollectionModel<BorradorMensaje> recursos = CollectionModel.of(borradoresConEnlaces,
                linkTo(methodOn(BorradorMensajeController.class).obtenerTodosLosBorradores()).withSelfRel());

        return new ResponseEntity<>(recursos, HttpStatus.OK);
    }

    /**
     * Obtiene un borrador de mensaje por su ID.
     * @param id El ID del borrador a buscar.
     * @return El borrador encontrado con estado 200 OK y enlaces HATEOAS, o 404 Not Found si no existe.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un borrador de mensaje por su ID", description = "Obtiene un borrador al buscarlo por su ID, incluyendo enlaces HATEOAS.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Borrador de mensaje encontrado.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BorradorMensaje.class))),
            @ApiResponse(responseCode = "404", description = "Borrador de mensaje no encontrado.")
    })
    public ResponseEntity<BorradorMensaje> obtenerBorradorPorId(
            @Parameter(description = "ID del borrador de mensaje a buscar", required = true)
            @PathVariable int id) {
        Optional<BorradorMensaje> borrador = borradorMensajeService.obtenerBorradorPorId(id);
        return borrador.map(b -> {
                    // Enlace 'self': URL para obtener este mismo recurso.
                    b.add(linkTo(methodOn(BorradorMensajeController.class).obtenerBorradorPorId(b.getIdBrdrMensaje())).withSelfRel());
                    // Enlace 'todos-los-borradores': URL para obtener la colección completa.
                    b.add(linkTo(methodOn(BorradorMensajeController.class).obtenerTodosLosBorradores()).withRel("todos-los-borradores"));
                    // Enlace 'actualizar-borrador': URL para actualizar este recurso (PUT).
                    b.add(linkTo(methodOn(BorradorMensajeController.class).actualizarBorrador(b.getIdBrdrMensaje(), null)).withRel("actualizar-borrador"));
                    // Enlace 'eliminar-borrador': URL para eliminar este recurso (DELETE).
                    b.add(linkTo(methodOn(BorradorMensajeController.class).eliminarBorrador(b.getIdBrdrMensaje())).withRel("eliminar-borrador"));
                    return new ResponseEntity<>(b, HttpStatus.OK);
                })
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * Actualiza un borrador de mensaje existente por su ID.
     * @param idBorrador ID del borrador a actualizar.
     * @param datosActualizados Objeto BorradorMensaje con los datos a actualizar.
     * @return El borrador actualizado con estado 200 OK y enlaces HATEOAS, o 404 Not Found si el borrador no existe.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un borrador de mensaje existente", description = "Actualiza los datos de un borrador de mensaje por su ID, incluyendo enlaces HATEOAS.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Borrador de mensaje actualizado con éxito.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BorradorMensaje.class))),
            @ApiResponse(responseCode = "404", description = "Borrador de mensaje no encontrado."),
            @ApiResponse(responseCode = "400", description = "Error en la solicitud, datos inválidos."),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor.")
    })
    public ResponseEntity<BorradorMensaje> actualizarBorrador(
            @Parameter(description = "ID del borrador de mensaje a actualizar", required = true)
            @PathVariable("id") int idBorrador,
            @RequestBody @Parameter(description = "Datos actualizados del borrador de mensaje", required = true)
            BorradorMensaje datosActualizados) {
        try {
            BorradorMensaje borradorActualizado = borradorMensajeService.actualizarBorrador(idBorrador, datosActualizados);
            // Añadir enlaces al borrador actualizado: 'self' y 'todos-los-borradores'.
            borradorActualizado.add(linkTo(methodOn(BorradorMensajeController.class).obtenerBorradorPorId(borradorActualizado.getIdBrdrMensaje())).withSelfRel());
            borradorActualizado.add(linkTo(methodOn(BorradorMensajeController.class).obtenerTodosLosBorradores()).withRel("todos-los-borradores"));
            return new ResponseEntity<>(borradorActualizado, HttpStatus.OK);
        } catch (NoSuchElementException e) { // Capturar NoSuchElementException si tu servicio lo lanza
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST); // O un mensaje más específico
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Elimina un borrador de mensaje por su ID.
     * @param id El ID del borrador a eliminar.
     * @return Estado 204 No Content si la eliminación es exitosa, o 404 Not Found si el borrador no existe.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un borrador de mensaje", description = "Elimina un borrador de mensaje del sistema por su ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Borrador de mensaje eliminado con éxito."),
            @ApiResponse(responseCode = "404", description = "Borrador de mensaje no encontrado."),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor.")
    })
    public ResponseEntity<Void> eliminarBorrador(
            @Parameter(description = "ID del borrador de mensaje a eliminar", required = true)
            @PathVariable int id) {
        try {
            borradorMensajeService.eliminarBorrador(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT); // 204 No Content para eliminación exitosa.
        } catch (NoSuchElementException e) { // Capturar NoSuchElementException si tu servicio lo lanza
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST); // O un mensaje más específico
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}