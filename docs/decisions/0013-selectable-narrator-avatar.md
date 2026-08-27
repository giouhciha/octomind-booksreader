# 0013. Avatar seleccionable para el narrador

## Contexto

El globo de Focus utiliza a Octi como acompañante visual. Para permitir que cada lector adapte el tono de la experiencia se necesita una segunda apariencia sin duplicar la lógica de lectura ni alterar la ubicación estable del libro.

## Decisión

Se incorpora `NarratorAvatar` con las opciones `OCTI`, `LOVECRAFT_ILLUSTRATION`, `SCHOPENHAUER_ILLUSTRATION`, `NIETZSCHE_ILLUSTRATION`, `CAMUS_ILLUSTRATION` y `CUSTOM_IMAGE`. El selector aparece únicamente dentro de la presentación de narrador. La elección se guarda en el registro local de cada libro y se restaura al abrirlo, de modo que los libros pueden compartir avatar o mantener elecciones distintas. Los registros anteriores y valores desconocidos vuelven de forma segura a Octi. DataStore conserva el resto de preferencias generales del lector.

Los avatares comparten el mismo globo, oración, gesto, ambientación, progreso y métricas. El retrato y la ilustración se empaquetan como recursos locales y nunca se transmiten. Octi conserva su animación de pase de página; el retrato y la ilustración permanecen estáticos.

`CUSTOM_IMAGE` acepta PNG, JPEG y WebP de hasta 10 MB. La app valida MIME, tamaño y dimensiones, reduce imágenes grandes y guarda una copia PNG normalizada en almacenamiento privado. La interfaz aplica un recorte circular no destructivo mediante `ContentScale.Crop`; el original seleccionado no se modifica. El usuario puede reemplazar o eliminar la copia desde Focus. Se conserva mientras existan los datos de la aplicación y se elimina también al borrar dichos datos o desinstalar.

## Consecuencias

- Cambiar el avatar tiene efecto inmediato y no reinicia la sesión.
- La asignación pertenece al libro actual y no sobrescribe la de otros libros.
- Al eliminar la imagen personalizada, cualquier libro que la tuviera asignada vuelve a Octi.
- Una preferencia desconocida o ausente vuelve de forma segura a Octi.
- Si la copia personalizada no está disponible, la interfaz utiliza un marcador neutro y permite elegir otra imagen.
- Agregar nuevos avatares requiere un recurso local, una etiqueta accesible y una nueva opción del enum.

## Reversión

Se puede retirar el selector y mantener `OCTI` como valor único. La clave guardada puede permanecer sin afectar versiones anteriores.
