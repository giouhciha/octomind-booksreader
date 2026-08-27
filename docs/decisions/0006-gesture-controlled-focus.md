# 0006. Focus manual controlado por gestos

- Estado: aceptada
- Fecha: 2026-08-24

## Contexto

La primera prueba con usuarios mostró que el avance cronometrado obliga a perseguir el marcador cuando la persona todavía no tiene un ritmo estable. El control de velocidad se percibió como una restricción y no como una ayuda.

## Opciones consideradas

1. Mantener el temporizador y reducir su velocidad inicial.
2. Ofrecer simultáneamente avance automático y manual.
3. Sustituir el avance automático por gestos discretos entre unidades de puntuación.

## Decisión

Focus utiliza exclusivamente bloques por puntuación. Un gesto vertical completo produce como máximo un avance o retroceso; un umbral de 48 dp descarta movimientos pequeños. Durante Focus se desactiva el desplazamiento libre para evitar conflictos entre el scroll y el cambio de fragmento. Fuera de Focus, el libro conserva el desplazamiento convencional.

Se retiran de la experiencia Play/Pausa, palabras por minuto, tamaño fijo de bloque, calibración cronometrada y adaptación automática de velocidad. Los libros existentes reconstruyen su plan por puntuación y recuperan el bloque correspondiente a su desplazamiento estable guardado.

El centro del bloque se calcula respecto al 42 % de la ventana física, ligeramente por encima de la mitad para dejar más contexto por delante de la lectura. Si el panel abierto reduce el área de lectura y ese punto queda fuera, el objetivo se limita al borde visible. El tiempo se mide únicamente como estadística pasiva durante Focus y nunca controla la navegación.

## Consecuencias

- Cada lector determina su ritmo sin perder la guía visual.
- Un desplazamiento largo no puede saltar varios fragmentos.
- Las unidades sin puntuación pueden abarcar un párrafo completo.
- El ritmo promedio deja de ser un ajuste y pasa a ser una observación de la sesión.
- Las preferencias antiguas de velocidad permanecen legibles para compatibilidad, pero no afectan Focus.

## Reversión

El motor de planes conserva su soporte histórico para bloques fijos y cálculo de pausas. Un modo automático futuro podría agregarse como experiencia separada, sin cambiar las ubicaciones guardadas ni la navegación manual.
