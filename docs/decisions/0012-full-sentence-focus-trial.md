# 0012. Prueba de Focus mediante oraciones completas

## Contexto

Los fragmentos delimitados por comas y aclaraciones pueden interrumpir el flujo y obligar al lector a reconstruir una misma oración mediante varios gestos. Se desea probar una unidad más amplia antes de decidir el comportamiento definitivo.

## Opciones consideradas

1. Mantener cortes en toda puntuación.
2. Mostrar párrafos completos sin importar su extensión.
3. Utilizar oraciones completas y conservar el final de párrafo como respaldo.

## Decisión

Focus reconstruye el plan con `ReadingMode.SENTENCE` al abrir el libro. El segmentador ignora comas, punto y coma, dos puntos y guiones. Cierra únicamente ante punto, interrogación, exclamación, puntos suspensivos o final de párrafo, después de descartar iniciales y abreviaturas contextuales.

Octi narrador muestra siempre el texto completo. Para evitar desbordamiento, reduce gradualmente la tipografía de oraciones con más de 30, 55 o 90 palabras, respetando un mínimo accesible de 14 puntos. La navegación, ubicación estable, progreso y métricas no cambian.

## Consecuencias

- Se requieren menos gestos y se conserva la idea completa de cada oración.
- Algunas oraciones literarias pueden ocupar gran parte de la pantalla.
- Las comas dejan de representar pausas de navegación, aunque permanecen visibles dentro del texto.
- El modo anterior continúa en el motor para comparar o revertir la prueba.

## Reversión

Cambiar el modo forzado al abrir libros de `SENTENCE` a `PUNCTUATION`. Las ubicaciones guardadas siguen siendo offsets de caracteres y no requieren migración.
