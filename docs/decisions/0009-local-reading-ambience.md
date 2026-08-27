# 0009. Ambientación local para Octi narrador

## Contexto

Octi narrador puede reforzar la inmersión con un fondo relacionado con el tono del libro. Generar imágenes o consultar un modelo en cada fragmento añadiría latencia, costo, dependencia de red y exposición innecesaria del contenido durante esta fase.

## Opciones consideradas

1. Generar una imagen con IA para cada fragmento.
2. Utilizar un único fondo decorativo para todos los libros.
3. Clasificar localmente el libro y componer ambientes abstractos en Android.

## Decisión

Se implementa un selector local que analiza el título, el encabezado del capítulo y hasta 4.000 caracteres de una muestra en memoria. Produce una categoría entre misterio, fantasía, ciencia ficción, romance, naturaleza, conocimiento y neutral. El título y el capítulo tienen mayor peso que la muestra, se normalizan acentos y solo cuentan palabras completas.

Jetpack Compose dibuja una paleta, degradados y formas abstractas para la categoría elegida. El ambiente cambia por capítulo con una transición de color y ofrece tres intensidades persistentes: desactivada, sutil e inmersiva. La capa no controla la navegación ni modifica el contenido del globo.

## Consecuencias

- Funciona sin conexión, sin costo por lectura y sin enviar texto fuera del dispositivo.
- Los ambientes son interpretaciones generales y pueden no identificar matices narrativos.
- Las formas abstractas reducen spoilers y no sustituyen la imaginación del lector.
- Un clasificador remoto futuro podrá implementar el mismo contrato de categorías sin cambiar la interfaz.

## Reversión

Establecer la intensidad en `OFF` o retirar el selector y utilizar siempre `NEUTRAL`. El progreso y las sesiones no requieren migración.
