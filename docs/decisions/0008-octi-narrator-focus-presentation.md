# 0008. Presentación Octi narrador para Focus

## Contexto

El marcador integrado mantiene el contexto de la página, pero algunos lectores se concentran mejor si ven una sola unidad de sentido. La mascota permite presentar ese fragmento como una narración visual sin imponer velocidad ni incorporar audio.

## Opciones consideradas

1. Sustituir permanentemente el marcador sobre texto.
2. Mostrar palabras de forma progresiva dentro de un diálogo.
3. Añadir una presentación opcional que muestre completo cada bloque existente.

## Decisión

Se añade `FocusPresentation` con los valores `TEXT_MARKER` y `OCTI_NARRATOR`. La elección se guarda localmente y `OCTI_NARRATOR` es el valor inicial. Octi narrador coloca una capa atenuada sobre el libro, un globo con el bloque completo y la mascota debajo; admite las tipografías y tamaños configurados. Las preferencias visuales exclusivas del marcador clásico se agrupan y solo aparecen al seleccionar `TEXT_MARKER`.

La presentación no crea bloques ni altera el avance. El gesto vertical y los controles accesibles llaman a la misma navegación por puntuación, guardan la misma ubicación estable y alimentan las mismas estadísticas. Octi continúa pasando una página únicamente cuando el movimiento es válido.

La instrucción situada debajo de Octi se muestra hasta el primer gesto vertical válido y su descarte se guarda en las preferencias del lector. No desaparece con gestos rechazados ni al utilizar los botones. Focus activo también se persiste como preferencia, aunque su reloj de sesión se pausa cuando la app pierde el primer plano y solo se reanuda mientras la pantalla de lectura está visible.

El panel de controles comienza expandido cuando todavía no existe una preferencia. Cada acción explícita de expandir o colapsar se guarda globalmente y se restaura en sesiones y libros posteriores; abrir un lector no sobrescribe esa decisión.

## Consecuencias

- El lector puede comparar una experiencia contextual con otra inmersiva sin perder avance.
- El fragmento completo preserva el ritmo manual y evita una animación de palabras impuesta.
- El libro queda parcialmente oculto en modo narrador, por decisión explícita y reversible del usuario.
- TalkBack conserva el texto del diálogo y los controles de navegación.

## Reversión

Eliminar la presentación `OCTI_NARRATOR` y tratar preferencias desconocidas como `TEXT_MARKER`. El contenido, progreso y sesiones no requieren migración porque nunca dependen de la presentación.
