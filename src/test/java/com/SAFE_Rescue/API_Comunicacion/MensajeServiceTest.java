package com.SAFE_Rescue.API_Comunicacion; // Manteniendo el paquete que tú utilizas

import com.SAFE_Rescue.API_Comunicacion.modelo.BorradorMensaje;
import com.SAFE_Rescue.API_Comunicacion.modelo.Mensaje;
import com.SAFE_Rescue.API_Comunicacion.repository.BorradorMensajeRepository;
import com.SAFE_Rescue.API_Comunicacion.repository.MensajeRepository;
import com.SAFE_Rescue.API_Comunicacion.service.MensajeService; // Asegúrate de que esta importación sea correcta

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
// REMOVER: import org.junit.jupiter.api.extension.ExtendWith; // No es necesario MockitoExtension
// REMOVER: import org.mockito.InjectMocks; // No se inyectan mocks
// REMOVER: import org.mockito.Mock; // No se usan mocks
// REMOVER: import org.mockito.junit.jupiter.MockitoExtension; // No es necesario

import org.springframework.beans.factory.annotation.Autowired; // Añadir para inyectar beans de Spring
import org.springframework.boot.test.context.SpringBootTest; // Añadir para cargar el contexto de Spring Boot
import org.springframework.test.context.ActiveProfiles; // Para activar perfiles de Spring
import org.springframework.transaction.annotation.Transactional; // Para que los tests sean transaccionales

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.NoSuchElementException;
import java.lang.IllegalStateException;

import static org.junit.jupiter.api.Assertions.*;
// REMOVER: import static org.mockito.Mockito.*; // No usaremos Mockito aquí
// (Si tus tests no usan AssertJ, puedes quitarlo)
import static org.assertj.core.api.Assertions.assertThat; // Si sigues usando AssertJ

@SpringBootTest // Carga el contexto completo de Spring Boot
@ActiveProfiles("test") // Activa el perfil 'test' que configuramos en application-test.properties
@Transactional // Cada método de test se ejecutará en una transacción que se revierte al finalizar
@DisplayName("Tests para MensajeService (Integración con Base de Datos H2)")
public class MensajeServiceTest {

    @Autowired // Spring inyectará el servicio real
    private MensajeService mensajeService;

    @Autowired // Spring inyectará el repositorio real
    private BorradorMensajeRepository borradorMensajeRepository;

    @Autowired // Spring inyectará el repositorio real
    private MensajeRepository mensajeRepository;

    // Ya no necesitamos inicializar estos aquí si los vamos a guardar en la DB
    // private BorradorMensaje borradorEjemplo;
    // private Mensaje mensajeEjemplo;

    @BeforeEach
    void setUp() {
        // Limpiar la base de datos si DDL-auto no es create-drop o si hay problemas
        // borradorMensajeRepository.deleteAll();
        // mensajeRepository.deleteAll();

        // Para las pruebas de integración, normalmente insertas datos iniciales
        // directamente en la base de datos usando los repositorios reales
        // o un TestEntityManager si tienes entidades más complejas.
        // Como @Transactional ya nos da un rollback, podríamos insertar por test.
    }

    // --- Pruebas para crearMensajeDesdeBorradorYReceptor ---

    /*
    @Test
    @DisplayName("Debería crear un mensaje y marcar el borrador como enviado cuando el borrador existe y no ha sido enviado")
    void crearMensajeDesdeBorradorYReceptor_BorradorExistenteYNoEnviado_DeberiaCrearMensaje() {
        // GIVEN (Dado que...)
        // Insertar un borrador real en la base de datos de prueba
        BorradorMensaje borradorParaTest = new BorradorMensaje();
        borradorParaTest.setIdBrdrMensaje(0); // El ID se generará automáticamente
        borradorParaTest.setIdBrdrEmisor(101);
        borradorParaTest.setFechaBrdrMensaje(new Date());
        borradorParaTest.setBrdrTitulo("Título de Borrador para Mensaje");
        borradorParaTest.setBrdrContenido("Contenido de Borrador para Mensaje.");
        borradorParaTest.setBorradorEnviado(false);
        borradorMensajeRepository.save(borradorParaTest); // Guarda en la DB real de H2

        int idReceptor = 202;

        // WHEN (Cuando...)
        Mensaje mensajeCreado = mensajeService.crearMensajeDesdeBorradorYReceptor(borradorParaTest.getIdBrdrMensaje(), idReceptor);

        // THEN (Entonces...)
        assertNotNull(mensajeCreado);
        // Verificar que el mensaje fue guardado en la DB
        Optional<Mensaje> mensajeRecuperado = mensajeRepository.findById(mensajeCreado.getIdMensaje());
        assertTrue(mensajeRecuperado.isPresent());
        assertEquals(borradorParaTest.getIdBrdrMensaje(), mensajeRecuperado.get().getBorradorOriginal().getIdBrdrMensaje());
        assertEquals(borradorParaTest.getIdBrdrEmisor(), mensajeRecuperado.get().getIdEmisor());
        assertEquals(idReceptor, mensajeRecuperado.get().getIdReceptor());
        assertThat(mensajeRecuperado.get().getBorradorOriginal().isBorradorEnviado()).isTrue(); // Verificar estado en DB

        // Verificar que el borrador original también se actualizó en la DB
        Optional<BorradorMensaje> borradorActualizado = borradorMensajeRepository.findById(borradorParaTest.getIdBrdrMensaje());
        assertTrue(borradorActualizado.isPresent());
        assertTrue(borradorActualizado.get().isBorradorEnviado());
    }
     */

    @Test
    @DisplayName("Debería lanzar NoSuchElementException cuando el borrador no existe al crear un mensaje")
    void crearMensajeDesdeBorradorYReceptor_BorradorNoExistente_DeberiaLanzarExcepcion() {
        // GIVEN: No insertamos ningún borrador, así simulamos que no existe
        int idBorradorInexistente = 999;
        int idReceptor = 202;

        // WHEN & THEN
        NoSuchElementException thrown = assertThrows(NoSuchElementException.class, () -> {
            mensajeService.crearMensajeDesdeBorradorYReceptor(idBorradorInexistente, idReceptor);
        });

        assertThat(thrown.getMessage()).contains("no encontrado para crear el mensaje.");
        // No hay mocks para verificar, solo nos aseguramos de que no se intenten guardar cosas
        assertThat(mensajeRepository.count()).isZero(); // Asegura que no se creó ningún mensaje
    }

    @Test
    @DisplayName("Debería lanzar IllegalStateException cuando el borrador ya ha sido enviado")
    void crearMensajeDesdeBorradorYReceptor_BorradorYaEnviado_DeberiaLanzarExcepcion() {
        // GIVEN: Insertar un borrador que ya esté marcado como enviado
        BorradorMensaje borradorYaEnviado = new BorradorMensaje();
        borradorYaEnviado.setIdBrdrMensaje(0);
        borradorYaEnviado.setIdBrdrEmisor(101);
        borradorYaEnviado.setFechaBrdrMensaje(new Date());
        borradorYaEnviado.setBrdrTitulo("Título Borrador Enviado");
        borradorYaEnviado.setBrdrContenido("Contenido Borrador Enviado.");
        borradorYaEnviado.setBorradorEnviado(true); // Ya enviado
        borradorMensajeRepository.save(borradorYaEnviado);

        int idReceptor = 202;

        // WHEN & THEN
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
            mensajeService.crearMensajeDesdeBorradorYReceptor(borradorYaEnviado.getIdBrdrMensaje(), idReceptor);
        });

        assertThat(thrown.getMessage()).contains("ya ha sido enviado");
        assertThat(mensajeRepository.count()).isZero(); // Asegura que no se creó ningún mensaje
    }

    // --- Pruebas para obtenerMensajePorId ---

    @Test
    @DisplayName("Debería devolver un mensaje cuando se encuentra por ID")
    void obtenerMensajePorId_MensajeExistente_DeberiaDevolverMensaje() {
        // GIVEN: Guardar un mensaje de ejemplo en la DB
        BorradorMensaje borradorBase = new BorradorMensaje(0, 1, new Date(), "T", "C", false);
        borradorMensajeRepository.save(borradorBase);

        Mensaje mensajeParaTest = new Mensaje(0, 1, 2, new Date(), "Titulo", "Contenido", borradorBase);
        mensajeRepository.save(mensajeParaTest);

        // WHEN
        Optional<Mensaje> resultado = mensajeService.obtenerMensajePorId(mensajeParaTest.getIdMensaje());

        // THEN
        assertTrue(resultado.isPresent());
        assertEquals(mensajeParaTest.getIdMensaje(), resultado.get().getIdMensaje());
        assertEquals(mensajeParaTest.getTitulo(), resultado.get().getTitulo());
    }

    @Test
    @DisplayName("Debería devolver un Optional vacío cuando el mensaje no se encuentra por ID")
    void obtenerMensajePorId_MensajeNoExistente_DeberiaDevolverOptionalVacio() {
        // GIVEN: No hay mensajes guardados con este ID
        int idMensajeInexistente = 999;

        // WHEN
        Optional<Mensaje> resultado = mensajeService.obtenerMensajePorId(idMensajeInexistente);

        // THEN
        assertFalse(resultado.isPresent());
    }

    // --- Pruebas para eliminarMensaje ---

    @Test
    @DisplayName("Debería eliminar un mensaje exitosamente cuando el mensaje existe")
    void eliminarMensaje_MensajeExistente_DeberiaEliminar() {
        // GIVEN: Guardar un mensaje para luego eliminarlo
        BorradorMensaje borradorBase = new BorradorMensaje(0, 1, new Date(), "T", "C", false);
        borradorMensajeRepository.save(borradorBase);
        Mensaje mensajeParaEliminar = new Mensaje(0, 1, 2, new Date(), "Titulo", "Contenido", borradorBase);
        mensajeRepository.save(mensajeParaEliminar);

        // Verificar que existe antes de eliminar
        assertThat(mensajeRepository.existsById(mensajeParaEliminar.getIdMensaje())).isTrue();

        // WHEN
        mensajeService.eliminarMensaje(mensajeParaEliminar.getIdMensaje());

        // THEN
        assertThat(mensajeRepository.existsById(mensajeParaEliminar.getIdMensaje())).isFalse();
    }

    @Test
    @DisplayName("Debería lanzar NoSuchElementException cuando el mensaje no existe al eliminar")
    void eliminarMensaje_MensajeNoExistente_DeberiaLanzarExcepcion() {
        // GIVEN: No hay mensajes con este ID
        int idMensajeInexistente = 999;

        // WHEN & THEN
        NoSuchElementException thrown = assertThrows(NoSuchElementException.class, () -> {
            mensajeService.eliminarMensaje(idMensajeInexistente);
        });

        assertThat(thrown.getMessage()).contains("no encontrado con ID: " + idMensajeInexistente + " para eliminar.");
    }

    // --- Pruebas para obtenerTodosLosMensajes ---
    @Test
    @DisplayName("Debería devolver todos los mensajes")
    void obtenerTodosLosMensajes_DeberiaDevolverListaDeMensajes() {
        // GIVEN: Guardar varios mensajes
        BorradorMensaje b1 = new BorradorMensaje(0, 1, new Date(), "T1", "C1", false);
        BorradorMensaje b2 = new BorradorMensaje(0, 2, new Date(), "T2", "C2", false);
        borradorMensajeRepository.saveAll(List.of(b1, b2)); // Guardar borradores primero

        Mensaje m1 = new Mensaje(0, 1, 10, new Date(), "Mensaje 1", "Contenido 1", b1);
        Mensaje m2 = new Mensaje(0, 2, 20, new Date(), "Mensaje 2", "Contenido 2", b2);
        mensajeRepository.saveAll(List.of(m1, m2)); // Guardar mensajes

        // WHEN
        List<Mensaje> resultado = mensajeService.obtenerTodosLosMensajes();

        // THEN
        assertNotNull(resultado);
        assertThat(resultado).hasSize(2);
        assertThat(resultado).containsExactlyInAnyOrder(m1, m2);
    }

    @Test
    @DisplayName("Debería devolver una lista vacía cuando no hay mensajes")
    void obtenerTodosLosMensajes_NoMensajes_DeberiaDevolverListaVacia() {
        // GIVEN: La DB está vacía por @Transactional o por limpieza inicial

        // WHEN
        List<Mensaje> resultado = mensajeService.obtenerTodosLosMensajes();

        // THEN
        assertNotNull(resultado);
        assertThat(resultado).isEmpty();
    }
}