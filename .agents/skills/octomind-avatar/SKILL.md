---
name: octomind-avatar
description: Convierte retratos proporcionados por el usuario en avatares ilustrados coherentes con Octomind y los integra como opciones persistentes del narrador Android. Usar cuando el usuario pida un “avatar Octomind” o quiera sumar un retrato al selector de Focus.
---

# Octomind Avatar

## Estilo aprobado

Nombrar el estilo **caricatura editorial anime para mascota de lectura**. Combina rasgos reconocibles y proporciones suavemente caricaturizadas con formas limpias, sombreado brillante y acabado amable de aplicación móvil.

Antes de generar, inspeccionar:

- `assets/octi-style-reference.png`, referencia del lenguaje visual, libro y acabado.
- `assets/approved-avatar-reference.png`, referencia de proporciones humanas y nivel de caricatura aprobado.

El retrato que adjunte el usuario es la referencia de identidad. Las imágenes de esta skill son referencias de estilo, no sujetos adicionales.

## Generación

Usar la herramienta integrada de generación de imágenes en modo de edición o referencia. Crear una ilustración original que conserve los rasgos identificables del retrato sin aplicar un filtro fotográfico.

La composición predeterminada debe incluir cabeza, torso y un libro abierto color turquesa con acentos dorados. Usar traje o vestimenta coherente con el retrato; expresión cálida, atenta y accesible; silueta centrada y compacta. No agregar texto, marcas de agua, marcos, escenarios ni personajes adicionales.

Solicitar fondo con transparencia alfa real. Después de generar, verificar que el PNG sea de 32 bits y que los píxeles de las esquinas tengan alfa 0. Si aparece un tablero dibujado o un fondo sólido, ejecutar una extracción de fondo que preserve únicamente al personaje.

## Integración en Octomind

No reemplazar avatares existentes salvo petición explícita. Agregar una nueva opción a `NarratorAvatar`, un recurso local bajo `drawable-nodpi`, una etiqueta con el nombre indicado por el usuario y una descripción accesible. Mostrar la ilustración completa con `ContentScale.Fit`.

Persistir la elección mediante el `ReaderSettings` y DataStore existentes. Compartir navegación, globo, ambientación, progreso y métricas; el avatar solo cambia la presentación. Mantener la ilustración estática salvo que el usuario pida una animación.

Actualizar versión, README y la decisión de avatares. Ejecutar pruebas, compilación y lint, revisar visualmente el selector abierto y el avatar con el menú colapsado, y confirmar que la preferencia se restaura tras reabrir la app.

## Invocación breve

Con un retrato adjunto basta con pedir: **“Crea un avatar Octomind de esta persona y agrégalo como &lt;nombre&gt;.”**
