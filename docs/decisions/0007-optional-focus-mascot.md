# 0007. Mascota opcional durante Focus

- Estado: aceptada
- Fecha: 2026-08-24

## Contexto

Focus necesita una identidad más cálida sin añadir presión ni distraer de la comprensión. Una mascota puede confirmar que el gesto fue aceptado y vincular visualmente el producto con la marca Octomind.

## Opciones consideradas

1. No mostrar elementos decorativos durante la lectura.
2. Mantener una mascota animándose continuamente.
3. Mostrar una mascota opcional con animación breve únicamente ante navegación válida.

## Decisión

Se incorpora **Octi**, una ilustración original de un pulpo lector con lentes y libro abierto, sobre fondo transparente. Aparece en un espacio reservado al centro de los controles inferiores durante Focus, sin superponerse al texto, y puede desactivarse desde el menú. La preferencia se guarda localmente.

Cada avance o retroceso válido incrementa un evento visual local. Octi inclina ligeramente el cuerpo y una página del libro gira durante 320 ms en la dirección del movimiento. Un gesto por debajo del umbral o un intento fuera de los límites del libro no produce animación. La navegación se ejecuta inmediatamente y nunca espera a que termine el efecto.

## Consecuencias

- La mascota confirma el avance sin utilizar texto ni diálogos.
- La ilustración aumenta el tamaño del APK y el uso de memoria gráfica.
- La animación es breve, no continua, y respeta la escala de animación de Android.
- El usuario puede ocultarla si prefiere una pantalla de lectura mínima.

## Reversión

La preferencia puede fijarse en desactivada o retirarse junto con el composable y el recurso rasterizado. No afecta el plan de lectura, la ubicación guardada ni las estadísticas.
