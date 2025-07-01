package com.SAFE_Rescue.API_Comunicacion;

import com.SAFE_Rescue.API_Comunicacion.modelo.BorradorMensaje;
import com.SAFE_Rescue.API_Comunicacion.modelo.Mensaje; // <-- NUEVA IMPORTACIÓN
import com.SAFE_Rescue.API_Comunicacion.repository.BorradorMensajeRepository;
import com.SAFE_Rescue.API_Comunicacion.repository.MensajeRepository; // <-- NUEVA IMPORTACIÓN
import net.datafaker.Faker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List; // Necesario para List
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors; // Necesario para .stream().filter().collect()

@Profile("dev") // Este DataLoader solo se ejecutará cuando el perfil activo sea "dev"
@Component
public class DataLoader implements CommandLineRunner {

    // Inyección de dependencia por campo, como en el DataLoader de tu amigo.
    @Autowired
    private BorradorMensajeRepository borradorMensajeRepository;

    @Autowired // <-- NUEVA INYECCIÓN PARA MENSAJE
    private MensajeRepository mensajeRepository;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("DataLoader para API_Comunicacion está en ejecución...");

        // Instancia de Faker y Random dentro del método run.
        Faker faker = new Faker();
        Random random = new Random();

        // --- Carga de Borradores de Mensajes ---
        if (borradorMensajeRepository.count() == 0) {
            System.out.println("Cargando datos iniciales para BorradorMensajes...");
            // Generar más borradores para tener una buena base para crear mensajes
            for (int i = 0; i < 15; i++) { // Cambiado a 15 borradores
                BorradorMensaje borrador = new BorradorMensaje();

                borrador.setIdBrdrEmisor(faker.number().numberBetween(1, 20)); // ID de emisor aleatorio
                borrador.setFechaBrdrMensaje(faker.date().past(random.nextInt(90), TimeUnit.DAYS));

                String tituloGenerado = faker.lorem().sentence(random.nextInt(6) + 1);
                borrador.setBrdrTitulo(tituloGenerado.length() > 30 ? tituloGenerado.substring(0, 30) : tituloGenerado);

                String contenidoGenerado = faker.lorem().paragraph(random.nextInt(4) + 1);
                borrador.setBrdrContenido(contenidoGenerado.length() > 250 ? contenidoGenerado.substring(0, 250) : contenidoGenerado);

                // Estado de borrador enviado (aleatorio). Los borradores que se usan para mensajes, se marcarán como enviados después.
                // Es buena idea que no todos sean enviados al principio, para poder usarlos.
                borrador.setBorradorEnviado(false);

                try {
                    borradorMensajeRepository.save(borrador);
                    // System.out.println("Borrador guardado: " + borrador.getIdBrdrMensaje()); // Puedes descomentar para depurar
                } catch (Exception e) {
                    System.err.println("Error al guardar BorradorMensaje: " + e.getMessage());
                }
            }
            System.out.println("Carga de datos iniciales de BorradorMensajes completada.");
        } else {
            System.out.println("La base de datos ya contiene BorradorMensajes. No se cargarán datos iniciales.");
        }

        // --- Carga de Mensajes ---
        // Verificar si la tabla de mensajes está vacía para no duplicar datos
        if (mensajeRepository.count() == 0) {
            System.out.println("Cargando datos iniciales para Mensajes...");

            List<BorradorMensaje> borradoresDisponibles = borradorMensajeRepository.findAll();

            if (borradoresDisponibles.isEmpty()) {
                System.out.println("No hay borradores disponibles para crear mensajes. Omita la carga de Mensajes.");
                // Si no hay borradores, no podemos crear mensajes a partir de ellos.
            } else {
                // Filtrar borradores que no han sido enviados para usarlos en mensajes
                List<BorradorMensaje> borradoresNoEnviados = borradoresDisponibles.stream()
                        .filter(b -> !b.isBorradorEnviado())
                        .collect(Collectors.toList());

                // Crear hasta un máximo de 10 mensajes, o tantos como borradores no enviados tengamos
                int mensajesACrear = Math.min(borradoresNoEnviados.size(), 10);

                if (mensajesACrear == 0) {
                    System.out.println("Todos los borradores disponibles ya han sido marcados como enviados, o no hay borradores no enviados. Omita la carga de Mensajes.");
                } else {
                    for (int i = 0; i < mensajesACrear; i++) {
                        // Tomar un borrador aleatorio de los que aún no han sido enviados
                        BorradorMensaje borradorSeleccionado = borradoresNoEnviados.remove(random.nextInt(borradoresNoEnviados.size()));

                        Mensaje mensaje = new Mensaje();
                        mensaje.setIdEmisor(borradorSeleccionado.getIdBrdrEmisor());
                        mensaje.setIdReceptor(faker.number().numberBetween(1, 20)); // ID de receptor aleatorio
                        mensaje.setFechaMensaje(new Date()); // Fecha actual del envío del mensaje
                        mensaje.setTitulo(borradorSeleccionado.getBrdrTitulo());
                        mensaje.setContenido(borradorSeleccionado.getBrdrContenido());
                        mensaje.setBorradorOriginal(borradorSeleccionado); // Vincula al borrador original

                        try {
                            mensajeRepository.save(mensaje);
                            // Importante: Marcar el borrador como enviado DESPUÉS de que el mensaje se guarde exitosamente
                            borradorSeleccionado.setBorradorEnviado(true);
                            borradorMensajeRepository.save(borradorSeleccionado);
                            // System.out.println("Mensaje guardado a partir de borrador: " + borradorSeleccionado.getIdBrdrMensaje()); // Descomentar para depurar
                        } catch (Exception e) {
                            System.err.println("Error al guardar Mensaje o actualizar Borrador: " + e.getMessage());
                            // Opcional: Si un mensaje falla, podrías considerar no marcar el borrador como enviado
                        }
                    }
                    System.out.println("Carga de datos iniciales de Mensajes completada.");
                }
            }
        } else {
            System.out.println("La base de datos ya contiene Mensajes. No se cargarán datos iniciales.");
        }

        System.out.println("DataLoader para API_Comunicacion finalizado.");
    }
}