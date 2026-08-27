# 0005. Atenuación visual y pantalla activa durante Focus

- Estado: aceptada
- Fecha: 2026-08-22

## Contexto

El bloque resaltado compite visualmente con el resto del texto y algunos lectores necesitan una separación mayor para seguirlo. Además, una sesión de Focus puede continuar sin interacción táctil durante el tiempo suficiente para que Android suspenda la pantalla.

## Opciones consideradas

1. Ocultar por completo todo el texto fuera del marcador.
2. Atenuar el contenido circundante con una intensidad controlada por el lector.
3. Dibujar una máscara opaca fija alrededor de una ventana central.

Para la pantalla se consideró un bloqueo permanente, un servicio con `WakeLock` y la propiedad de ventana `keepScreenOn` limitada a la vista del lector.

## Decisión

Focus atenúa el texto fuera del bloque activo. La intensidad es una preferencia persistente entre 0 % y 80 %, con 45 % como valor inicial. El bloque activo siempre utiliza los colores de contraste completo del tema. Un valor de 0 % desactiva visualmente la atenuación sin desactivar Focus.

La vista del lector establece `keepScreenOn` únicamente mientras Focus está activo. Al desactivarlo o retirar la vista, se restaura el valor anterior. No se solicita permiso ni se utiliza un `WakeLock`; Android solo mantiene encendida la pantalla cuando la actividad es visible.

## Consecuencias

- El lector puede ajustar la separación visual sin perder el contexto del párrafo.
- Los temas claro, sepia y oscuro comparten el mismo porcentaje y mantienen el bloque activo legible.
- Focus puede consumir más batería durante sesiones largas, pero el usuario lo controla explícitamente.
- La pantalla vuelve a respetar el tiempo de suspensión al salir de Focus.

## Reversión

La atenuación puede fijarse en 0 % o eliminarse sin cambiar el plan de lectura ni las posiciones guardadas. La pantalla activa puede retirarse eliminando el efecto de la vista, sin migraciones de datos ni permisos.
