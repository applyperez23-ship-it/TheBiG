# Plan de Implementación Definitivo - TheBiG v2.0 (Edición Completa & Profesional)

Este plan detalla la arquitectura, flujos visuales, persistencia de datos y desarrollo funcional completo para culminar la aplicación nativa **TheBiG** en su versión 2.0 comercial sin errores, con animaciones 2D/3D fluidas, control de torneos profesional y panel de herramientas avanzado.

---

## Decisiones Críticas y Resumen Ejecutivo

> [!IMPORTANT]
> **Compatibilidad y Persistencia**: Todas las estructuras de base de datos Room (v7/v8) se actualizarán manteniendo migración destructiva controlada para que al instalar la nueva versión no se produzcan bloqueos ni pérdida de datos en copias de seguridad `.tb` o `JSON`.

- **Mínimo de 3 Equipos**: Validación estricta en el creador de torneos para exigir $\ge 3$ clubes antes de permitir el inicio de fixture.
- **Modo Ida y Vuelta (Home / Away)**: Generación automática de dobles enfrentamientos en liga regular, repechajes y eliminatorias con cálculo de marcador global acumulado.
- **Cronómetro Inteligente y Tanda de Penaltis**: División exacta en 2 tiempos con contador regresivo, vibración al término, tiempo añadido (+ / -) y módulo interactivo de penaltis en caso de empate en eliminación directa.
- **Nombres Únicos**: Bloqueo insensible a mayúsculas/minúsculas para equipos y jugadores repetidos dentro del mismo torneo.
- **Enlace a Grupo de WhatsApp**: Configuración global de link de invitación con toggle activo/inactivo para compartir reportes y tablas al grupo del torneo.
- **Panel de Desarrollador Daniel ("Modo Cheats")**: Manipulación en tiempo real de cruces/sorteos a medida, modificador directo de puntos/goles en tablas e inmunidad de plantilla (limpieza de tarjetas).

---

## 1. Visión General del Producto y Arquitectura

```
┌────────────────────────────────────────────────────────────────────────┐
│                        APP THEBIG v2.0 (KOTLIN)                       │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
    ┌───────────────────────────────┼───────────────────────────────┐
    │                               │                               │
┌───▼──────────────────┐   ┌────────▼─────────────┐   ┌─────────────▼──────────┐
│ MODELO DE COMPETICIÓN│   │ CRONÓMETRO Y PARTIDOS│   │ RESPALDOS & MULTIMEDIA │
│ • Mínimo 3 Equipos   │   │ • 1er/2do Tiempo     │   │ • Archivos .tb / JSON  │
│ • Ida y Vuelta       │   │ • Tiempo Añadido     │   │ • Escudos/Fotos Base64 │
│ • Bye / Fecha Libre  │   │ • Tanda de Penaltis  │   │ • Nombres Únicos       │
│ • Forfeit (3-0)      │   │ • Notificación Sync │   │ • WhatsApp Group Link  │
└──────────────────────┘   └──────────────────────┘   └────────────────────────┘
    │                               │                               │
    └───────────────────────────────┼───────────────────────────────┘
                                    │
    ┌───────────────────────────────┼───────────────────────────────┐
    │                               │                               │
┌───▼──────────────────┐   ┌────────▼─────────────┐   ┌─────────────▼──────────┐
│ PIZARRA TÁCTICA 2D   │   │ FIXTURE & BRACKETS   │   │ PANEL DE DESARROLLADOR │
│ • Formaciones F5-F11 │   │ • Llaves Verticales  │   │ • Sorteo a Medida      │
│ • Balón Animado 2D   │   │ • Sin Banners        │   │ • Ajuste Puntos/Goles  │
│ • Drag & Swap        │   │ • Cuadros Compactos  │   │ • Inmunidad Tarjetas   │
│ • Stats Popup        │   │ • Exportación HD 1:1 │   │ • Telemetría Room      │
└──────────────────────┘   └──────────────────────┘   └────────────────────────┘
```

---

## 2. Experiencia de Usuario e Interfaz Gráfica (UI/UX)

### A. Fixture y Llaves Eliminatorias Continuas Verticales
- **Sin banners estáticos invasivos**: La vista de eliminación directa fluye de forma orgánica por debajo de la tabla o en la pestaña de Fixture.
- **Tarjetas compactas con escudos**: Cada enfrentamiento muestra el escudo oficial a la izquierda o arriba y el nombre del club limpio abajo.
- **Compartir Llave Completa**: Generación de tarjeta Instagram 1:1 que incluye los cruces de semifinales, pase a la final y distintivo de campeón sin cortes.

### B. Pizarra Táctica y Plantilla Visual
- **Nombres de Jugadores**: Ajuste de texto con `maxLines = 1` y ancho contenido para prevenir cortes verticales o solapamientos.
- **Padding del Terreno de Juego**: Márgenes de seguridad (`16.dp`) para evitar que las camisetas o el arquero toquen las líneas de banda o el botón de "Simular".
- **Balón Animado 2D**: Esfera de fútbol 2D que rueda libremente entre jugadores en la cancha con un movimiento curvo aleatorio y micro-reacciones de pulso en las camisetas.
- **Popup de Estadísticas**: Al tocar a cualquier jugador se despliega un cuadro elegante con sus goles, asistencias, tarjetas y minutos jugados.
- **Sustituciones Drag & Drop**: Arrastrar un jugador de la banca sobre uno de la cancha realiza el cambio con animación de reemplazo.

### C. Reloj en Vivo y Tanda de Penaltis
- **Panel Principal**: Minutero en fuente digital de alto contraste con contador de tiempo transcurrido y mini indicador de tiempo restante.
- **Tiempo Añadido (+ / -)**: Al llegar al minuto reglamentario, el reloj vibra, muestra diálogo para agregar tiempo de compensación y continúa la cuenta hasta completar la mitad.
- **Interfase de Penaltis**: Registro mediante botones táctiles rápidos (GOL / FALLÓ) con contador de lanzamientos por equipo y declaración instantánea del ganador.

### D. Finanzas y WhatsApp
- **Barra al 0%**: Renderizado proporcional estricto sin relleno en cero.
- **Control de Cuotas**: Lista de jugadores con estado de pago de cuota ("PAGADO" / "PENDIENTE") y Switch de activación.
- **WhatsApp Group Integration**: Campo en Ajustes para ingresar URL de invitación al grupo de WhatsApp. Botones de compartir con opción "Enviar al Grupo de WhatsApp".

---

## 3. Especificaciones de Lógica y Persistencia de Datos

1. **Room Entities & DAO**:
   - `Tournament`: Campos `minTeams = 3`, `roundsCount`, `isHomeAwayEnabled = true/false`, `whatsappGroupUrl`.
   - `Match`: Campos `homeAggregateScore`, `awayAggregateScore`, `isLeg2`, `penaltyHomeScore`, `penaltyAwayScore`.
   - `Player`: Campo `photoUri`.
   - `Team`: Campo `customShieldUri`.

2. **Fórmula de Forfeit Automático (3-0)**:
   - Al retirar un equipo de un torneo activo, todos sus partidos pendientes no jugados cambian automáticamente a estado `FINISHED` con marcador `0 - 3` a favor del rival.

3. **Herramientas de Desarrollador Daniel ("Modo Cheats")**:
   - *Sorteo a Medida*: Lista de equipos vivos ordenados por tabla general con dropdown para forzar el rival del próximo cruce.
   - *Modificador de Puntos y Goles*: Contadores $+1 / -1$ para Puntos, Goles a Favor y Goles en Contra directamente en las filas de la tabla de posiciones.
   - *Inmunidad de Plantilla*: Botón "Limpiar Tarjetas" que resetea las amonestaciones de los jugadores de un club antes de partidos clave.

---

## 4. Plan de Ejecución Código por Código

1. **`com.example.data.local.entity.Tournament` & `Match`**:
   - Añadir soporte para dobles vueltas (Ida/Vuelta), penales y configuraciones de WhatsApp.
2. **`com.example.util.TournamentScheduler`**:
   - Algoritmo de generación de partidos con soporte Ida/Vuelta (Home/Away) y cálculo de dobles enfrentamientos.
   - Gestión automática de jornadas impares con fecha libre ("Descanso").
3. **`com.example.util.TheBiGBackupHelper`**:
   - Serialización de imágenes en Base64 para escudos y fotos de jugadores.
   - Copia de archivos recuperados al almacenamiento interno del dispositivo.
   - Validación estricta de nombres de equipos/jugadores únicos.
4. **`com.example.ui.screens.TacticalPitchScreen`**:
   - Corrección de padding de campo y nombres de jugadores.
   - Animación 2D del balón rondando entre líneas tácticas.
   - Diálogo emergente de estadísticas del jugador y sustituciones.
5. **`com.example.ui.components.VerticalKnockoutTreeFlow`**:
   - Tarjetas compactas con escudos y nombres sin banners obstructivos.
6. **`com.example.ui.screens.MatchLiveSheetScreen`**:
   - Reloj con división de tiempos, tiempo añadido, vibración y módulo interactivo de penaltis.
7. **`com.example.ui.screens.SettingsScreen`**:
   - Panel de Desarrollador Daniel enriquecido con menú "Cheats" (Forzar Cruce, Modificar Puntos/Goles, Limpiar Tarjetas).
   - Configuración de enlace de Grupo de WhatsApp.
8. **Compilación y Verificación**:
   - Ejecutar `compile_applet` para confirmar la ausencia total de errores de sintaxis y compilación.

---

## Estado del Plan
Pronto para ser presentado al usuario para revisión e inicio de refactorización definitiva.
