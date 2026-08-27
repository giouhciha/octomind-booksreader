# 0011. Home mediante estantería adaptable

## Contexto

La lista de tarjetas comunica información con claridad, pero no transmite la sensación de una biblioteca personal. La referencia visual solicitada utiliza cubiertas apoyadas en repisas de madera y prioriza el reconocimiento espacial de cada libro.

## Opciones consideradas

1. Utilizar una imagen fija de estantería como fondo.
2. Extraer o descargar portadas antes de cambiar el home.
3. Dibujar la estantería y cubiertas provisionales directamente con Compose.

## Decisión

El home se construye de forma nativa con paneles y repisas dibujados por Compose. Cada fila admite tres libros en teléfonos y cinco cuando el ancho alcanza 600 dp. Las cubiertas provisionales se derivan de forma determinista del título mediante una paleta local e incluyen título, autor, progreso, apertura y eliminación.

La importación permanece en el encabezado y muestra su estado ocupado. La biblioteca vacía conserva la misma madera, pero sitúa las instrucciones dentro de una superficie de alto contraste. No se generan ni descargan imágenes y no se transmite información del libro.

## Consecuencias

- La biblioteca tiene una identidad visual cálida y reconocible sin depender de red.
- Libros con títulos iguales pueden compartir color, aunque siguen siendo elementos independientes.
- La tipografía de una cubierta no sustituye una portada editorial real.
- Una fase posterior puede añadir portadas extraídas del EPUB detrás de la misma interfaz.

## Reversión

Restaurar la lista de tarjetas no requiere migrar datos; la estantería solo modifica la presentación de `BookSummary`.
