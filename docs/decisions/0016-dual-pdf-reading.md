# 0016. Lectura PDF adaptada y original

## Contexto

Octomind necesita aplicar Focus, narradores, fuentes y temas a libros PDF sin perder el diseño de la obra ni la posibilidad de verificar tablas, imágenes y referencias por página. Android puede renderizar páginas originales desde API 21, pero la extracción de texto de la plataforma no cubre el nivel mínimo actual de la aplicación.

## Opciones consideradas

1. Mostrar únicamente el PDF original. Conserva el diseño, pero impide la experiencia adaptada de Octomind.
2. Extraer únicamente el texto. Permite Focus, pero elimina una referencia visual importante y no sirve para revisar contenido con maquetación compleja.
3. Conservar el original y crear una representación de lectura conectada mediante anclas de página.

## Decisión

Se elige la tercera opción. La importación usa PDFBox para Android localmente para extraer texto seleccionable, metadatos y una portada. Cada página con texto produce un `BookPageAnchor` con índice de página y desplazamiento inicial dentro del texto normalizado. El archivo original se copia al almacenamiento privado de la biblioteca.

La vista adaptada utiliza el mismo motor de lectura que EPUB y TXT. La vista original usa `PdfRenderer` de Android y renderiza una página a la vez fuera del hilo principal. Al cambiar de página, el lector busca el ancla más cercana y actualiza el bloque estable; al regresar a la lectura adaptada conserva ese punto, el narrador y las métricas del libro.

Los PDF cifrados o puramente escaneados no se procesan silenciosamente. La aplicación explica que requieren compatibilidad adicional u OCR. No se registra ni transmite el texto extraído.

El algoritmo de normalización se versiona. Cuando cambia, un PDF ya importado se vuelve a procesar una sola vez desde su original privado; el progreso se relaciona mediante la página anclada y las citas se buscan de nuevo por su texto exacto antes de actualizar sus rangos.

## Consecuencias

- El respaldo cifrado existente incluye automáticamente el PDF original porque respalda el directorio privado de la biblioteca.
- El tamaño máximo de importación es 200 MB y el máximo de páginas es 5,000 para limitar memoria, disco y tiempo de procesamiento.
- Las tablas y diseños complejos pueden perder estructura en la vista adaptada, pero permanecen disponibles en la vista original.
- Se agrega PDFBox Android como dependencia; la vista original no depende de esa biblioteca durante la lectura.

## Reversión

La vista original y las anclas son campos opcionales y compatibles con registros anteriores. Puede retirarse PDFBox y deshabilitar nuevas importaciones PDF sin afectar libros TXT/EPUB ni la estructura base del lector; los archivos PDF ya guardados pueden conservarse o eliminarse junto con su libro.
