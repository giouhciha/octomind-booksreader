# 0002. Calibración local del perfil lector

- Estado: sustituida por 0014
- Fecha: 2026-08-22

## Contexto

Una velocidad predeterminada no representa el ritmo cómodo de todas las personas. La primera calibración debe observar el avance real del lector sin depender de servicios de red, sin convertir cada interacción en telemetría permanente y sin confundir interrupciones o dobles toques con velocidad de lectura.

## Opciones consideradas

1. Comenzar siempre con una velocidad fija y pedir al usuario que la ajuste manualmente.
2. Ejecutar automáticamente el marcador y solicitar confirmación al final.
3. Pedir un toque al terminar cada bloque durante la primera apertura de un libro nuevo.

## Decisión

Antes de calibrar, el usuario desplaza el libro, elige el modo por palabras o por puntuación y confirma el inicio real mediante un bloque resaltado. En el modo por palabras también elige entre una y ocho palabras por bloque. La navegación previa no cuenta como lectura, no incrementa retrocesos ni incluye portada, créditos, índice o datos editoriales en las muestras. El punto confirmado se guarda como una ubicación estable del libro. El modo y el tamaño se mantienen fijos durante las muestras y pueden volver a modificarse después de calibrar.

A partir de ese punto se utiliza una calibración manual de 30 intervalos válidos al abrir por primera vez un libro nuevo. Cada intervalo relaciona tiempo monotónico, cantidad de palabras y la pausa lingüística calculada para el bloque.

Se descartan intervalos menores de 150 milisegundos y mayores de 10 segundos. La velocidad inicial se estima con la mediana de las muestras plausibles, se limita a un rango seguro y se guarda como parte de un perfil lector local agregado. Al finalizar también se registra en el libro que la calibración fue completada.

Las interrupciones por pérdida de primer plano reinician el inicio de la muestra. No se persiste la secuencia cruda de tiempos ni el contenido leído asociado a cada toque.

Los libros importados antes de incorporar esta función se consideran ya calibrados para no alterar inesperadamente la experiencia existente.

## Consecuencias

- El ritmo inicial refleja el comportamiento observable del lector y funciona sin conexión.
- Los preliminares del libro no distorsionan el perfil si el usuario elige el comienzo del contenido principal.
- La puntuación y las pausas del texto no reducen artificialmente la velocidad estimada.
- La mediana limita la influencia de muestras atípicas.
- La calibración mide ritmo, no comprensión; las evaluaciones futuras deberán validar cualquier aumento automático.
- El usuario todavía puede modificar la velocidad resultante en cualquier momento.
- La calibración por libro permite observar cambios futuros sin conservar datos detallados innecesarios.

## Reversión

La experiencia puede sustituirse por una calibración guiada diferente sin cambiar las ubicaciones de lectura. Se puede ignorar o eliminar el indicador `calibrationCompleted` y conservar la velocidad manual; el perfil agregado no es requisito para renderizar ni reanudar un libro.
