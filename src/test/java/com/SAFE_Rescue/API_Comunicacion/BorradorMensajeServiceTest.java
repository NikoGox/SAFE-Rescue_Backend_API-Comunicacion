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
import org.springframework.boot.test.mock.mockito.MockBean; // IMPORTANTE: Importar MockBean

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Arrays;

@SpringBootTest
@DisplayName("Tests para BorradorMensajeService")
public class BorradorMensajeServiceTest {

    @Autowired
    private BorradorMensajeService borradorMensajeService;

    // CAMBIO CLAVE: Usa @MockBean para que Spring reemplace el repositorio real con un mock.
    // Esto inyectará el mock automáticamente en borradorMensajeService.
    @MockBean
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
        // Reiniciar los mocks antes de cada prueba es una buena práctica,
        // aunque @MockBean a menudo lo maneja automáticamente.
        reset(borradorMensajeRepository);
    }

    @Test
    @DisplayName("Debería guardar un nuevo borrador y asignar fecha/estado por defecto")
    public void testGuardarBorrador_newBorrador() {
        BorradorMensaje newBorrador = new BorradorMensaje(
                0, // ID 0 para indicar que es nuevo
                faker.number().numberBetween(1, 100),
                null,
                faker.lorem().sentence(3),
                faker.lorem().paragraph(2),
                true // Esto se debería cambiar a false por el servicio
        );

        // Cuando se llama a save, simulamos que el repositorio devuelve el objeto con un ID y fecha
        when(borradorMensajeRepository.save(any(BorradorMensaje.class))).thenAnswer(invocation -> {
            BorradorMensaje savedBorrador = invocation.getArgument(0);
            // El servicio es quien debería establecer estos valores si no están.
            // Aquí en el mock, solo simulamos lo que el repositorio devolvería si el servicio se los pasa.
            // Si tu servicio modifica el objeto antes de save, mockea el save con ese objeto modificado.
            // Para simplificar: el mock solo devuelve el objeto que el servicio le pasó,
            // pero le asigna un ID para simular la persistencia.
            savedBorrador.setIdBrdrMensaje(faker.number().numberBetween(100, 200));
            // La fecha y el estado enviado los maneja el servicio, no el repositorio.
            // Si el servicio no las toca, el mock las devuelve como están.
            // Si el servicio las asigna, el objeto que llega al save() ya tendrá esos valores.
            return savedBorrador;
        });

        BorradorMensaje result = borradorMensajeService.guardarBorrador(newBorrador);

        assertNotNull(result);
        assertThat(result.getIdBrdrMensaje()).isNotZero();
        // Asumiendo que tu servicio asigna la fecha si es null y pone borradorEnviado en false
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
                specificDate, // Fecha proporcionada
                faker.lorem().sentence(3),
                faker.lorem().paragraph(2),
                true // Esto se debería cambiar a false por el servicio
        );

        when(borradorMensajeRepository.save(any(BorradorMensaje.class))).thenAnswer(invocation -> {
            BorradorMensaje savedBorrador = invocation.getArgument(0);
            savedBorrador.setBorradorEnviado(false); // El servicio lo cambia a false
            return savedBorrador;
        });

        BorradorMensaje result = borradorMensajeService.guardarBorrador(existingBorrador);

        assertNotNull(result);
        assertThat(result.getIdBrdrMensaje()).isEqualTo(10);
        assertThat(result.getFechaBrdrMensaje()).isEqualTo(specificDate); // La fecha debe permanecer
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

        // Cuando se busca por ID, devolver el sampleBorrador
        when(borradorMensajeRepository.findById(borradorId)).thenReturn(Optional.of(sampleBorrador));
        // Cuando se llama a save con *cualquier* BorradorMensaje, devolver el mismo argumento que se pasó
        when(borradorMensajeRepository.save(any(BorradorMensaje.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BorradorMensaje updatedBorrador = borradorMensajeService.actualizarBorrador(borradorId, updates);

        assertNotNull(updatedBorrador);
        assertEquals(borradorId, updatedBorrador.getIdBrdrMensaje());
        assertEquals(updates.getBrdrTitulo(), updatedBorrador.getBrdrTitulo());
        assertEquals(updates.getBrdrContenido(), updatedBorrador.getBrdrContenido());
        // La fecha y el estado enviado del sampleBorrador original NO deberían cambiar
        assertEquals(sampleBorrador.getFechaBrdrMensaje(), updatedBorrador.getFechaBrdrMensaje());
        assertEquals(sampleBorrador.isBorradorEnviado(), updatedBorrador.isBorradorEnviado());


        verify(borradorMensajeRepository, times(1)).findById(borradorId);
        // Verifica que se llamó a save con el objeto 'sampleBorrador' modificado (el que fue devuelto por findById)
        // Usamos 'any(BorradorMensaje.class)' o capturamos el argumento para verificar sus propiedades
        verify(borradorMensajeRepository, times(1)).save(argThat(b ->
                b.getIdBrdrMensaje() == borradorId &&
                        b.getBrdrTitulo().equals(updates.getBrdrTitulo()) &&
                        b.getBrdrContenido().equals(updates.getBrdrContenido())
        ));
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
        // doNothing().when() es para métodos void
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