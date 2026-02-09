# Directorio de Outputs

Este directorio contiene los archivos de salida generados por el microservicio Rabbit2.

## Estructura

```
outputs/
└── json-reports/          # Archivos JSON con actualizaciones de rutas
    ├── .gitkeep          # Mantiene el directorio en Git
    └── *.json            # Archivos generados automáticamente
```

## Archivos Generados

Los archivos JSON se crean con el siguiente patrón de nombres:
- `actualizacion_ruta_{RUTA_ID}_{TIMESTAMP}.json`

Ejemplo:
- `actualizacion_ruta_R01_20260209_143045.json`

## Contenido de los JSON

```json
{
  "id": 1,
  "rutaId": "R01",
  "descripcion": "Cambio-de-horario",
  "horaSalida": "09-02-2026 06:30:00",
  "horaLlegada": "09-02-2026 22:15:00",
  "timestamp": "09-02-2026 14:30:45",
  "zonaHoraria": "UTC-3"
}
```

## Notas

- Los archivos JSON no se suben al repositorio Git (están en .gitignore)
- La estructura de directorios se mantiene con .gitkeep
- Los archivos se crean automáticamente al procesar mensajes de RabbitMQ
