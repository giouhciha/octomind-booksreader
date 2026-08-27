# 0003. Modos de formación de bloques de lectura

- Estado: aceptada
- Fecha: 2026-08-22

## Contexto

Los bloques con una cantidad fija de palabras producen un ritmo uniforme, pero pueden cortar una unidad de sentido antes de una pausa natural. Algunos lectores pierden el hilo al reconstruir esos fragmentos. Otros prefieren bloques pequeños y predecibles para entrenar velocidad.

## Opciones consideradas

1. Mantener únicamente bloques con cantidad fija de palabras.
2. Sustituirlos completamente por oraciones o cláusulas delimitadas por puntuación.
3. Ofrecer ambos comportamientos como modos explícitos controlados por el lector.

## Decisión

Se ofrecen dos modos locales e independientes de la presentación:

- **Por palabras:** forma bloques de una a ocho palabras y anticipa límites naturales cuando aparecen antes del máximo.
- **Por puntuación:** forma una unidad hasta la siguiente coma, punto y coma, dos puntos, punto, interrogación, exclamación, guion discursivo o final de párrafo. Si no existe ningún signo, conserva el párrafo completo.

El modo elegido se guarda en las preferencias del lector. Puede cambiarse antes de calibrar y durante la lectura normal, pero no durante una calibración activa. Al cambiarlo se reconstruye el plan conservando la ubicación estable actual.

## Consecuencias

- Cada lector puede priorizar uniformidad visual o continuidad semántica.
- El modo por puntuación puede producir bloques largos en textos con poca puntuación; es una consecuencia visible y deliberada de esa elección.
- La duración continúa calculándose con la cantidad real de palabras y la pausa lingüística del bloque.
- Las posiciones guardadas siguen siendo desplazamientos de caracteres y no dependen del modo.
- El perfil futuro deberá distinguir el desempeño por modo antes de comparar velocidades directamente.

## Reversión

El plan de lectura mantiene la misma estructura de bloques y ubicaciones. Un modo puede retirarse o reemplazarse sin migrar libros ni progreso; las preferencias desconocidas vuelven de forma segura al modo por palabras.
