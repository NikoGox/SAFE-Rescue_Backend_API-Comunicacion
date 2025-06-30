package com.SAFE_Rescue.API_Comunicacion;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.SAFE_Rescue.API_Comunicacion.modelo.BorradorMensaje;
import com.SAFE_Rescue.API_Comunicacion.repository.BorradorMensajeRepository;
import com.SAFE_Rescue.API_Comunicacion.service.BorradorMensajeService;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Arrays;

@SpringBootTest
@DisplayName("Tests para BorradorMensajeService")
public class BorradorMensajeServiceTest {

    @Autowired
    private BorradorMensajeService borradorMensajeService;

    // Mock del repositorio para aislar la lógica del servicio.
    // NOTA: @MockitoBean está deprecated en Spring Boot 3.4.x, pero se mantiene por requisitos de la prueba.
    @MockitoBean
    private BorradorMensajeRepository borradorMensajeRepository;

    private Faker faker;
    private BorradorMensaje sampleBorrador;

    @BeforeEach
    void setUp() {
        faker = new Faker();
        sampleBorrador = new BorradorMensaje(
                1,
                faker.number().numberBetween(1, 100),
                new Date(),
                faker.lorem().sentence(3),
                faker.lorem().paragraph(2),
                false
        );
    }

    @Test
    @DisplayName("Debería guardar un nuevo borrador y asignar fecha/estado por defecto")
    public void testGuardarBorrador_newBorrador() {
        BorradorMensaje newBorrador = new BorradorMensaje(
                0,
                faker.number().numberBetween(1, 100),
                null,
                faker.lorem().sentence(3),
                faker.lorem().paragraph(2),
                true
        );

        when(borradorMensajeRepository.save(any(BorradorMensaje.class))).thenAnswer(invocation -> {
            BorradorMensaje savedBorrador = invocation.getArgument(0);
            if (savedBorrador.getIdBrdrMensaje() == 0) {
                savedBorrador.setIdBrdrMensaje(faker.number().numberBetween(100, 200));
            }
            if (savedBorrador.getFechaBrdrMensaje() == null) {
                savedBorrador.setFechaBrdrMensaje(new Date());
            }
            savedBorrador.setBorradorEnviado(false);
            return savedBorrador;
        });

        BorradorMensaje result = borradorMensajeService.guardarBorrador(newBorrador);

        assertNotNull(result);
        assertThat(result.getIdBrdrMensaje()).isNotZero();
        assertThat(result.getFechaBrdrMensaje()).isNotNull();
        assertThat(result.isBorradorEnviado()).isFalse();
        assertThat(result.getBrdrTitulo()).isEqualTo(newBorrador.getBrdrTitulo());
        assertThat(result.getBrdrContenido()).isEqualTo(newBorrador.getBrdrContenido());

        verify(borradorMensajeRepository, times(1)).save(any(BorradorMensaje.class));
    }

    @Test
    @DisplayName("Debería guardar un borrador manteniendo la fecha proporcionada")
    public void testGuardarBorrador_withProvidedDate() {
        Date specificDate = new Date(System.currentTimeMillis() - 100000);
        BorradorMensaje existingBorrador = new BorradorMensaje(
                10,
                faker.number().numberBetween(1, 100),
                specificDate,
                faker.lorem().sentence(3),
                faker.lorem().paragraph(2),
                true
        );

        when(borradorMensajeRepository.save(any(BorradorMensaje.class))).thenAnswer(invocation -> {
            BorradorMensaje savedBorrador = invocation.getArgument(0);
            savedBorrador.setBorradorEnviado(false);
            return savedBorrador;
        });

        BorradorMensaje result = borradorMensajeService.guardarBorrador(existingBorrador);

        assertNotNull(result);
        assertThat(result.getIdBrdrMensaje()).isEqualTo(10);
        assertThat(result.getFechaBrdrMensaje()).isEqualTo(specificDate);
        assertThat(result.isBorradorEnviado()).isFalse();
        verify(borradorMensajeRepository, times(1)).save(any(BorradorMensaje.class));
    }

    @Test
    @DisplayName("Debería actualizar el título y contenido de un borrador existente")
    public void testActualizarBorrador_existing() {
        int borradorId = sampleBorrador.getIdBrdrMensaje();
        BorradorMensaje updates = new BorradorMensaje();
        updates.setBrdrTitulo("Nuevo Título Actualizado");
        updates.setBrdrContenido("Contenido actualizado para el borrador.");

        when(borradorMensajeRepository.findById(borradorId)).thenReturn(Optional.of(sampleBorrador));
        when(borradorMensajeRepository.save(any(BorradorMensaje.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BorradorMensaje updatedBorrador = borradorMensajeService.actualizarBorrador(borradorId, updates);

        assertNotNull(updatedBorrador);
        assertEquals(borradorId, updatedBorrador.getIdBrdrMensaje());
        assertEquals(updates.getBrdrTitulo(), updatedBorrador.getBrdrTitulo());
        assertEquals(updates.getBrdrContenido(), updatedBorrador.getBrdrContenido());
        verify(borradorMensajeRepository, times(1)).findById(borradorId);
        verify(borradorMensajeRepository, times(1)).save(sampleBorrador);
    }

    @Test
    @DisplayName("Debería lanzar RuntimeException cuando el borrador no se encuentra al actualizar")
    public void testActualizarBorrador_nonExisting_throwsException() {
        int nonExistingId = 999;
        BorradorMensaje updates = new BorradorMensaje();
        updates.setBrdrTitulo("Cualquier título");

        when(borradorMensajeRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            borradorMensajeService.actualizarBorrador(nonExistingId, updates);
        });

        assertThat(thrown.getMessage()).isEqualTo("Borrador no encontrado con ID: " + nonExistingId);
        verify(borradorMensajeRepository, times(1)).findById(nonExistingId);
        verify(borradorMensajeRepository, never()).save(any(BorradorMensaje.class));
    }

    @Test
    @DisplayName("Debería retornar una lista de todos los borradores cuando existen")
    public void testObtenerTodosLosBorradores_whenExist() {
        BorradorMensaje borrador2 = new BorradorMensaje(
                2, faker.number().numberBetween(1, 100), new Date(),
                faker.lorem().sentence(3), faker.lorem().paragraph(2), false
        );
        List<BorradorMensaje> borradores = Arrays.asList(sampleBorrador, borrador2);
        when(borradorMensajeRepository.findAll()).thenReturn(borradores);

        List<BorradorMensaje> result = borradorMensajeService.obtenerTodosLosBorradores();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertThat(result).containsExactlyInAnyOrder(sampleBorrador, borrador2);
        verify(borradorMensajeRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Debería retornar una lista vacía si no hay borradores")
    public void testObtenerTodosLosBorradores_whenEmpty() {
        when(borradorMensajeRepository.findAll()).thenReturn(List.of());

        List<BorradorMensaje> result = borradorMensajeService.obtenerTodosLosBorradores();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(borradorMensajeRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Debería retornar el borrador si se encuentra por ID")
    public void testObtenerBorradorPorId_found() {
        int borradorId = sampleBorrador.getIdBrdrMensaje();
        when(borradorMensajeRepository.findById(borradorId)).thenReturn(Optional.of(sampleBorrador));

        Optional<BorradorMensaje> result = borradorMensajeService.obtenerBorradorPorId(borradorId);

        assertTrue(result.isPresent());
        assertEquals(sampleBorrador, result.get());
        verify(borradorMensajeRepository, times(1)).findById(borradorId);
    }

    @Test
    @DisplayName("Debería retornar un Optional vacío si el borrador no se encuentra por ID")
    public void testObtenerBorradorPorId_notFound() {
        int nonExistingId = 999;
        when(borradorMensajeRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        Optional<BorradorMensaje> result = borradorMensajeService.obtenerBorradorPorId(nonExistingId);

        assertFalse(result.isPresent());
        verify(borradorMensajeRepository, times(1)).findById(nonExistingId);
    }

    @Test
    @DisplayName("Debería eliminar un borrador existente exitosamente")
    public void testEliminarBorrador_existing() {
        int borradorId = sampleBorrador.getIdBrdrMensaje();
        when(borradorMensajeRepository.existsById(borradorId)).thenReturn(true);
        doNothing().when(borradorMensajeRepository).deleteById(borradorId);

        borradorMensajeService.eliminarBorrador(borradorId);

        verify(borradorMensajeRepository, times(1)).existsById(borradorId);
        verify(borradorMensajeRepository, times(1)).deleteById(borradorId);
    }

    @Test
    @DisplayName("Debería lanzar RuntimeException si el borrador no se encuentra para eliminar")
    public void testEliminarBorrador_nonExisting_throwsException() {
        int nonExistingId = 999;
        when(borradorMensajeRepository.existsById(nonExistingId)).thenReturn(false);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            borradorMensajeService.eliminarBorrador(nonExistingId);
        });

        assertThat(thrown.getMessage()).isEqualTo("Borrador no encontrado con ID: " + nonExistingId + " para eliminar.");
        verify(borradorMensajeRepository, times(1)).existsById(nonExistingId);
        verify(borradorMensajeRepository, never()).deleteById(anyInt());
    }
}