# 0014. Entrada directa a lectura y Focus

- Estado: aceptada
- Fecha: 2026-08-26

## Contexto

La etapa «Elige dónde empezar / Comenzar aquí» bloqueaba temporalmente Focus en cada libro nuevo. En la versión manual actual no existe calibración cronometrada y el paso se percibe como una barrera, además de hacer parecer que el gesto vertical no funciona.

## Opciones consideradas

1. Mantener la selección inicial y explicar mejor por qué el gesto está desactivado.
2. Permitir el gesto durante la selección sin registrar progreso.
3. Eliminar la etapa y abrir directamente desde la ubicación disponible.

## Decisión

Se elimina la etapa de inicio. Un libro nuevo parte de su primera ubicación estable y uno existente conserva su progreso. Focus restaura inmediatamente su preferencia global y sus gestos están disponibles desde el primer renderizado activo.

Los libros nuevos se registran internamente como compatibles con el flujo directo. El campo histórico `calibrationCompleted` se conserva en el formato local para leer bibliotecas existentes, pero deja de controlar la interfaz. El detector de gestos se reinicia también cuando cambian el bloque, el tamaño del plan o la presentación, evitando que utilice estado anterior.

## Consecuencias

- Desaparece «Comenzar aquí» y ningún estado previo bloquea Focus.
- Portadas, créditos o índices pueden formar parte del inicio hasta que el lector avance o se desplace en lectura normal.
- La posición se sigue guardando después de cada movimiento.
- Un gesto vertical completo continúa moviendo como máximo una oración.

## Reversión

El campo histórico permite reintroducir una selección inicial como función opcional sin migrar los libros existentes. No debe volver a bloquear Focus por defecto.
