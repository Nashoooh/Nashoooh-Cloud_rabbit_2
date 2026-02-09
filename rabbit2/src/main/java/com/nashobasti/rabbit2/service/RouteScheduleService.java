package com.nashobasti.rabbit2.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nashobasti.rabbit2.entity.RouteUpdate;
import com.nashobasti.rabbit2.repository.RouteUpdateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RouteScheduleService {
    
    @Autowired
    private RouteUpdateRepository routeUpdateRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String OUTPUT_DIRECTORY = "outputs/json-reports"; // Directorio relativo al proyecto
    
    // Configuración de zona horaria UTC-3 y formato de fecha
    private final ZoneId UTC_MINUS_3 = ZoneId.of("America/Santiago"); // UTC-3 (Chile/Argentina)
    private final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    
    public void processScheduleMessage(String message) {
        try {
            RouteUpdate routeUpdate = parseMessage(message);
            if (routeUpdate != null) {
                // Guardar en base de datos
                RouteUpdate saved = routeUpdateRepository.save(routeUpdate);
                System.out.println(" [✓] Actualización guardada en BD - ID: " + saved.getId() + 
                                 " | Ruta: " + saved.getRutaId() + 
                                 " | Descripción: " + saved.getDescripcion());
                
                // Generar archivo JSON
                generateJsonFile(saved);
            }
        } catch (Exception e) {
            System.err.println(" [✗] Error procesando mensaje de horario: " + e.getMessage());
            System.err.println(" [✗] Mensaje: " + message);
        }
    }
    
    private RouteUpdate parseMessage(String message) {
        // Patrón para: "actualizacion ruta R01 descripcion Cambio-de-horario salida 09-02-2026 14:30:00 llegada 09-02-2026 15:45:00"
        String pattern = "actualizacion\\s+ruta\\s+(\\S+)\\s+descripcion\\s+([^\\s]+(?:\\s+[^\\s]+)*)\\s+salida\\s+(\\d{2}-\\d{2}-\\d{4}\\s+\\d{2}:\\d{2}:\\d{2})\\s+llegada\\s+(\\d{2}-\\d{2}-\\d{4}\\s+\\d{2}:\\d{2}:\\d{2})";
        Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = regex.matcher(message);
        
        if (matcher.find()) {
            try {
                String rutaId = matcher.group(1);
                String descripcion = matcher.group(2);
                String horaSalidaStr = matcher.group(3);
                String horaLlegadaStr = matcher.group(4);
                
                // Validaciones
                if (rutaId == null || rutaId.trim().isEmpty()) {
                    throw new IllegalArgumentException("Ruta ID no puede estar vacío");
                }
                if (descripcion == null || descripcion.trim().isEmpty()) {
                    throw new IllegalArgumentException("Descripción no puede estar vacía");
                }
                
                // Parsear fechas con formato dd-MM-yyyy HH:mm:ss
                LocalDateTime horaSalida = LocalDateTime.parse(horaSalidaStr, DATE_FORMAT);
                LocalDateTime horaLlegada = LocalDateTime.parse(horaLlegadaStr, DATE_FORMAT);
                
                // Convertir a UTC-3 (ya están en hora local, solo validamos)
                ZonedDateTime salidaUTC3 = horaSalida.atZone(UTC_MINUS_3);
                ZonedDateTime llegadaUTC3 = horaLlegada.atZone(UTC_MINUS_3);
                
                if (llegadaUTC3.isBefore(salidaUTC3)) {
                    throw new IllegalArgumentException("Hora de llegada no puede ser anterior a hora de salida");
                }
                
                return new RouteUpdate(rutaId, descripcion, horaSalida, horaLlegada);
                
            } catch (Exception e) {
                throw new IllegalArgumentException("Error parseando mensaje: " + e.getMessage());
            }
        } else {
            throw new IllegalArgumentException("Formato de mensaje inválido. Esperado: 'actualizacion ruta RXX descripcion XXXX salida dd-MM-yyyy HH:mm:ss llegada dd-MM-yyyy HH:mm:ss'");
        }
    }
    
    private void generateJsonFile(RouteUpdate routeUpdate) throws IOException {
        // Crear el directorio si no existe
        Path outputPath = Paths.get(OUTPUT_DIRECTORY);
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
        }
        
        // Crear el objeto JSON con la información de la actualización
        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put("id", routeUpdate.getId());
        jsonData.put("rutaId", routeUpdate.getRutaId());
        jsonData.put("descripcion", routeUpdate.getDescripcion());
        jsonData.put("horaSalida", routeUpdate.getHoraSalida().format(DATE_FORMAT));
        jsonData.put("horaLlegada", routeUpdate.getHoraLlegada().format(DATE_FORMAT));
        jsonData.put("timestamp", routeUpdate.getTimestamp().format(DATE_FORMAT));
        jsonData.put("zonaHoraria", "UTC-3");
        
        // Generar nombre del archivo - solo ruta + fecha (sin hora)
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String fileName = String.format("actualizaciones_%s_%s.json", 
            routeUpdate.getRutaId(), 
            fecha);
        
        File outputFile = new File(outputPath.toFile(), fileName);
        
        // Crear estructura del archivo JSON
        Map<String, Object> archivoCompleto = new HashMap<>();
        List<Map<String, Object>> actualizaciones = new ArrayList<>();
        
        // Si el archivo ya existe, leer las actualizaciones previas
        if (outputFile.exists()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> datosExistentes = objectMapper.readValue(outputFile, Map.class);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> listExistente = (List<Map<String, Object>>) datosExistentes.get("actualizaciones");
                if (listExistente != null) {
                    actualizaciones.addAll(listExistente);
                }
            } catch (Exception e) {
                System.out.println(" [⚠] Error leyendo archivo existente: " + e.getMessage());
                // Si hay error, crear archivo nuevo
                actualizaciones = new ArrayList<>();
            }
        }
        
        // Agregar la nueva actualización
        actualizaciones.add(jsonData);
        
        // Estructura final del archivo
        archivoCompleto.put("rutaId", routeUpdate.getRutaId());
        archivoCompleto.put("fecha", fecha);
        archivoCompleto.put("totalActualizaciones", actualizaciones.size());
        archivoCompleto.put("actualizaciones", actualizaciones);
        
        // Escribir el archivo completo
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, archivoCompleto);
        
        System.out.println(" [📄] Archivo JSON actualizado: " + outputFile.getAbsolutePath() + 
                          " (Total: " + actualizaciones.size() + " actualizaciones)");
    }
}
