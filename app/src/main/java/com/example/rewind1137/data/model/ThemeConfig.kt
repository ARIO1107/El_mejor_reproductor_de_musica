package com.example.rewind1137.data.model

enum class ThemeCategory(val categoryName: String) {
    NIGHT("De Noche & Oscuros"),
    DAY_PASTEL("Pasteles & De Día"),
    EXTRAS("Extras & Divertidos")
}

enum class AppThemeMode(
    val title: String,
    val description: String,
    val isLight: Boolean = false,
    val category: ThemeCategory
) {
    // ============ 1. De Noche & Oscuros ============
    REWIND_NEON_BLACK(
        title = "Negro Neón",
        description = "Fondo noche oscuro con deslumbrantes acentos salmón neón",
        category = ThemeCategory.NIGHT
    ),
    AMOLED_PITCH_BLACK(
        title = "Amoled Pitch Black",
        description = "Negro absoluto (#000000) de máxima eficiencia para OLED",
        category = ThemeCategory.NIGHT
    ),
    CYBERPUNK_SALMON(
        title = "Cyberpunk Noche",
        description = "Contrastes vibrantes entre magenta neón y cian noche",
        category = ThemeCategory.NIGHT
    ),
    DARK_EMERALD(
        title = "Esmeralda Profundo",
        description = "Oscuridad bosque nocturno con toques menta brillante",
        category = ThemeCategory.NIGHT
    ),
    DEEP_SAPPHIRE(
        title = "Zafiro Nocturno",
        description = "Azul marino medianoche con destellos zafiro y cian",
        category = ThemeCategory.NIGHT
    ),
    MIDNIGHT_PURPLE(
        title = "Púrpura Místico",
        description = "Púrpura noche profundo con sutiles toques lavanda",
        category = ThemeCategory.NIGHT
    ),
    SUNSET_ORANGE(
        title = "Atardecer Cálido",
        description = "Gradiente envolvente inspirados en atardeceres dorados",
        category = ThemeCategory.NIGHT
    ),
    GOLDEN_AUTUMN(
        title = "Otoño Dorado",
        description = "Miel cálida, tonos terracota y ámbar acogedor",
        category = ThemeCategory.NIGHT
    ),
    ROSE_QUARTZ(
        title = "Cuarzo Rosa",
        description = "Calidez romántica con tonos rosa cuarzo y bronce suave",
        category = ThemeCategory.NIGHT
    ),
    PASTEL_BLUE_PIRANHA(
        title = "Piraña Azul Frío",
        description = "Azul profundo inspirado en el pez piraña oficial de Rewind 1137",
        category = ThemeCategory.NIGHT
    ),
    NORDIC_FROST(
        title = "Helado Nórdico",
        description = "Brisa helada nórdica con azul glacial y blanco puro",
        category = ThemeCategory.NIGHT
    ),

    // ============ 2. Pasteles & De Día ============
    PURE_WHITE_LIGHT(
        title = "Blanco Puro Día",
        description = "Diseño diurno limpio, impecable y minimalista estilo iOS",
        isLight = true,
        category = ThemeCategory.DAY_PASTEL
    ),
    PASTEL_CREAM_DAY(
        title = "Crema Suave Día",
        description = "Fondo marfil cálido con acentos pastel durazno",
        isLight = true,
        category = ThemeCategory.DAY_PASTEL
    ),
    SKY_BLUE_LIGHT(
        title = "Celeste Brisa Día",
        description = "Cielo despejado con tonos celestes y turquesa frescos",
        isLight = true,
        category = ThemeCategory.DAY_PASTEL
    ),
    MINT_SPRING(
        title = "Menta Fresca Día",
        description = "Pastel menta refrescante con toques esmeralda luminoso",
        isLight = true,
        category = ThemeCategory.DAY_PASTEL
    ),
    PEACH_BLOSSOM_DAY(
        title = "Flor Durazno Día",
        description = "Rosa pastel primaveral inspirado en pétalos y crema",
        isLight = true,
        category = ThemeCategory.DAY_PASTEL
    ),
    LAVENDER_MARSHMALLOW(
        title = "Malvavisco Lavanda",
        description = "Lavanda suave y esponjosa con acentos púrpura dulce",
        isLight = true,
        category = ThemeCategory.DAY_PASTEL
    ),
    LEMON_SORBET_DAY(
        title = "Cítrico Limón Día",
        description = "Sorbete de limón con tonos amarillos refrescantes",
        isLight = true,
        category = ThemeCategory.DAY_PASTEL
    ),
    BUBBLEGUM_CLOUD(
        title = "Nube Chicle",
        description = "Nube rosa pastel bubblegum azucarada y juguetona",
        isLight = true,
        category = ThemeCategory.DAY_PASTEL
    ),
    AQUA_LAGOON_DAY(
        title = "Laguna Aqua Día",
        description = "Aguas turquesa cristalinas con brisa tropical",
        isLight = true,
        category = ThemeCategory.DAY_PASTEL
    ),

    // ============ 3. Extras & Divertidos ============
    PSYCHEDELIC_RAINBOW(
        title = "Arcoíris Psicodélico",
        description = "Fiebre de colores neón sobre fondo violeta eléctrico",
        category = ThemeCategory.EXTRAS
    ),
    NEON_GROOVY(
        title = "Neón Groovy",
        description = "Cian y magenta chillones con vibra disco infinita",
        category = ThemeCategory.EXTRAS
    ),
    GALAXY_TRIP(
        title = "Viaje Galáctico",
        description = "Nebulosas púrpura y estrellas en un viaje sin gravedad",
        category = ThemeCategory.EXTRAS
    ),
    RETRO_FIESTA(
        title = "Retro Fiesta 90s",
        description = "Amarillo ácido y malva retro tipo arcade a tope",
        category = ThemeCategory.EXTRAS
    ),
    CANDY_POP(
        title = "Caramelo Pop",
        description = "Dulce explosión de rosa brillante y morado chicle",
        category = ThemeCategory.EXTRAS
    ),
    GLASSMORPHISM_APPLE(
        title = "Modo Vidrio Apple",
        description = "Efecto cristal esmerilado translúcido con bordes luminosos",
        category = ThemeCategory.EXTRAS
    )
}