# 0001. Base local del lector Android

- Estado: aceptada
- Fecha: 2026-08-21

## Contexto

La primera fase debe validar el elemento diferenciador de Octomind: una guía visual que avance por bloques de palabras sin depender de la red. El progreso debe sobrevivir cierres, cambios de orientación y futuras variaciones en la presentación del libro.

## Opciones consideradas

1. Renderizar todos los formatos dentro de un WebView.
2. Guardar páginas renderizadas y mover una capa visual sobre coordenadas fijas.
3. Normalizar el texto y construir un plan de lectura con rangos de caracteres estables.

## Decisión

Se utiliza Kotlin con Jetpack Compose. TXT y EPUB se normalizan a texto local; cada bloque de lectura conserva su rango global y su rango dentro del párrafo. El progreso se guarda como desplazamiento de caracteres, no como número visual de página.

El motor de ritmo recibe bloques, palabras por minuto y pausas lingüísticas. La interfaz representa el bloque activo y lo mantiene centrado dentro del área de lectura que permanece visible después de descontar barras y controles. El menú completo puede ocultarse en lectura normal o Focus. En Focus contraído, retroceso, Play/Pausa y avance permanecen accesibles en la esquina inferior izquierda; el acceso al menú queda separado a la derecha. Cada cambio de altura de los controles recalcula el centro visible. Se agrega espacio desplazable al inicio y al final para poder centrar también los bloques extremos. Un reloj monotónico controla los vencimientos para evitar que pequeños retrasos acumulen deriva.

El tema de página, la familia tipográfica, el tamaño del texto y la intensidad de atenuación de Focus son preferencias globales guardadas en DataStore. Lora, Roboto y Roboto Mono se empaquetan dentro de la aplicación bajo la SIL Open Font License 1.1 para garantizar diferencias reproducibles y funcionamiento sin conexión en todos los dispositivos. El tamaño se limita a un intervalo accesible de 14 a 32 puntos. La atenuación se limita entre 0 % y 80 % y nunca reduce el contraste del bloque activo.

EPUB se procesa localmente con las APIs ZIP y XML de la plataforma. Se respeta el orden de lectura declarado por el `spine`, se aplican límites de tamaño y no se extraen archivos al sistema de carpetas.

## Consecuencias

- El marcador funciona sin conexión y puede probarse sin Android UI.
- Cambiar tamaño de fuente no invalida la posición guardada.
- El lector no necesita desplazar manualmente el texto cuando el marcador cambia de bloque o párrafo.
- La vista de entrenamiento no preserva el diseño visual exacto del EPUB.
- El parser actual cubre EPUB de texto convencional; contenido multimedia, DRM y diseños fijos quedan fuera de esta fase.
- PDF requerirá una estrategia adicional para vincular texto reorganizado con coordenadas de página.

## Reversión

Las interfaces de dominio no dependen del parser. Si se adopta Readium u otro motor EPUB, deberá producir el mismo `BookDocument` con texto, capítulos y ubicaciones estables. El motor de ritmo y la interfaz pueden conservarse.
