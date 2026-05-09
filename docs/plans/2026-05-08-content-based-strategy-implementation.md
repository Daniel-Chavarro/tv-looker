# Content-Based Strategy - Implementacion

**Fecha:** 2026-05-08  
**Estado:** Implementado  
**Tipo:** Feature  
**Milestone:** Phase 1 - Foundation

---

## 1) Objetivo

Implementar la estrategia de recomendacion basada en contenido que utiliza metadatos de items (generos, actores, directores, tags) para recomendar items similares a los que el usuario ha visto.

Esta estrategia:
- Construye perfiles de usuario basados en el contenido de items que han visto/calificado
- Utiliza vectores de caracteristicas con ponderacion TF-IDF
- Calcula similitud coseno entre el perfil del usuario y los items candidatos
- Es la segunda estrategia del sistema (Phase 1 - Foundation)

Referencia: [Recommendation Strategies Design](2026-03-06-recommendation-strategies-and-aggregations-design.md#2-content-based-strategy)

---

## 2) Como funciona

La implementacion consta de cuatro componentes principales:

### ItemFeatureVector (Data Structure)

Representa un item o perfil de usuario como un vector de caracteristicas separadas por tipo:
- `genres`: Map<String, Double> - pesos por genero
- `actors`: Map<String, Double> - pesos por actor
- `directors`: Map<String, Double> - pesos por director
- `tags`: Map<String, Double> - pesos por tag (reservado para futura implementacion)

Operaciones principales:
- `cosineSimilarity(ItemFeatureVector other)`: calcula similitud coseno como `dotProduct / (norm1 * norm2)`
- `addVectorWithWeight(ItemFeatureVector other, double weight)`: agrega otro vector escalado por peso (usado para generar perfiles de usuario)

### ItemFeatureVectorProvider (Data Provider)

Provider ID: `"item-feature-vectors"`

1. Calcula frecuencias de documento (DF) para cada feature (genero, actor, director)
2. Calcula IDF: `log(N / (1 + df))` donde N = total de items
3. Construye vectores TF-IDF para cada item
4. Cache: 24 horas (86400 segundos)

Nota: Tags no se procesan en esta implementacion porque el modelo de datos aun no incluye una entidad Tag. La estructura `ItemFeatureVector` soporta tags para implementacion futura.

### UserProfileProvider (Data Provider)

Provider ID: `"user-content-profiles"`

1. Obtiene los vectores de items de `ItemFeatureVectorProvider`
2. Agrega interacciones de usuarios ponderadas por tipo:
   - LIKE: peso 5.0
   - VIEW: peso 3.0
   - RATING: usa score >= 4.0 como positivo, score <= 2.0 como penalizacion (-score), neutral entre 2 y 4
   - Otros tipos: peso 0.0
3. Construye perfil del usuario sumando vectores de items con sus pesos
4. Cache: 1 hora (3600 segundos)

### ContentBasedStrategy (Recommendation Strategy)

Strategy name: `"content-based"`

1. Obtiene perfiles de usuario y vectores de items del contexto
2. Para cada item candidato, calcula similitud coseno entre perfil de usuario y vector del item
3. Normaliza score a rango `[0, 1]` usando `Math.max(0.0, Math.min(1.0, similarity))`
4. Filtra items con score > 0 y ordena descendente
5. Cold start: usuarios sin historial retornan lista vacia
6. Explicacion: `"Consistent with the type of content you usually enjoy"`

---

## 3) Comportamiento clave

- **TF-IDF**: pondera features raras mas alto (menor df = mayor idf)
- **Cold start users**: retorna lista vacia cuando no hay perfil de usuario
- **Cold start items**: items sin vector en el provider se omiten
- **Scores**: normalizados en [0, 1]
- **Similitud coseno**: como todos los features tienen pesos TF-IDF no negativos, la similitud coseno naturalmente esta en [0, 1]
- **Agregacion de perfil**: ponderada por tipo de interaccion (no por rating numerico exacto debido a limitaciones de datos en contexto)
- **Configuracion deshabilitable**: via `recommendation.content.tfidf.enabled=false`

---

## 4) Configuracion

Habilitar/deshabilitar TF-IDF:

```properties
recommendation.content.tfidf.enabled=true
```

Peso en agregacion constante:

```properties
recommendation.weights.content=0.25
```

Habilitar estrategia (cuando el switch este disponible):

```properties
recommendation.strategies.content.enabled=true
```

---

## 5) Archivos de implementacion

### Produccion

- `src/main/java/org/tvl/tvlooker/domain/data_structure/ItemFeatureVector.java`
- `src/main/java/org/tvl/tvlooker/domain/motor/utils/provider/ItemFeatureVectorProvider.java`
- `src/main/java/org/tvl/tvlooker/domain/motor/utils/provider/UserProfileProvider.java`
- `src/main/java/org/tvl/tvlooker/domain/strategy/recommendation/ContentBasedStrategy.java`
- `src/main/resources/application-dev.properties` (nuevo, configuracion de desarrollo)
- `src/main/resources/application.properties` (modificado, agrega `recommendation.content.tfidf.enabled=true`)

### Testing

- `src/test/java/org/tvl/tvlooker/domain/data_structure/ItemFeatureVectorTest.java`
- `src/test/java/org/tvl/tvlooker/domain/motor/utils/provider/ItemFeatureVectorProviderTest.java`
- `src/test/java/org/tvl/tvlooker/domain/motor/utils/provider/UserProfileProviderTest.java`
- `src/test/java/org/tvl/tvlooker/domain/strategy/recommendation/ContentBasedStrategyTest.java`

---

## 6) Casos cubiertos por tests

### ItemFeatureVectorTest (2 tests)

1. Similitud coseno entre vectores identicos = 1.0
2. Similitud coseno entre vectores ortogonales = 0.0
3. Agregar vector con peso positivo: suma ponderada correcta
4. Agregar vector con peso negativo: resta ponderada correcta

### ItemFeatureVectorProviderTest (3 tests)

1. Extraccion y calculo TF-IDF correcto para generos
2. Verificacion de cache: provider ID y expiracion de 86400 segundos
3. Manejo cuando esta deshabilitado: retorna mapa vacio

### UserProfileProviderTest (1 test)

1. Agregacion de perfiles basada en pesos de interaccion
   - LIKE (peso 5.0) sobre item con genero Action -> Action=5.0 en perfil
   - VIEW (peso 3.0) sobre item con genero Comedy -> Comedy=3.0 en perfil
   - RESEARCH (peso 0.0) se ignora

### ContentBasedStrategyTest (1 test)

1. Recomendacion normal: usuario con perfil obtiene items similares
2. Cold start: usuario sin perfil retorna lista vacia
3. Filtrado de items con score 0
4. Verificacion de nombre de estrategia `"content-based"`
5. Verificacion de sourceStrategy en ScoredItem

---

## 7) Ejemplo rapido

Dado:
- Item1: generos [Action]
- Item2: generos [Action, Comedy]
- Item3: generos [Drama]
- Total items N = 3
- Usuario vio Item1 (LIKE, peso 5.0)

Document frequencies:
- Action: df=2 (Item1, Item2)
- Comedy: df=1 (Item2)
- Drama: df=1 (Item3)

IDF:
- Action: log(3 / (1+2)) = log(1) = 0.0
- Comedy: log(3 / (1+1)) = log(1.5) ≈ 0.405
- Drama: log(3 / (1+1)) = log(1.5) ≈ 0.405

Perfil de usuario (solo vio Item1):
- Action: 5.0 * 0.0 = 0.0

Scores contra candidatos:
- Item1: cosineSimilarity([Action:0.0], [Action:0.0]) = 0.0 -> filtrado
- Item2: cosineSimilarity([Action:0.0], [Action:0.0, Comedy:0.405]) = 0.0 -> filtrado
- Item3: cosineSimilarity([Action:0.0], [Drama:0.405]) = 0.0 -> filtrado

> Nota: con este ejemplo degenerado (Action DF=2, IDF=0), todas las similitudes son 0. En datos reales con mas items y features mas raros, los scores serian no nulos.

---

## 8) Como validarlo localmente

Ejecutar tests especificos:

```powershell
Set-Location "C:\Users\user\OneDrive\Documentos\TV-Looker\tv-looker"
mvn "-Dtest=ItemFeatureVectorTest" test
mvn "-Dtest=ItemFeatureVectorProviderTest" test
mvn "-Dtest=UserProfileProviderTest" test
mvn "-Dtest=ContentBasedStrategyTest" test
```

Validacion de regresion:

```powershell
Set-Location "C:\Users\user\OneDrive\Documentos\TV-Looker\tv-looker"
mvn "-Dtest=HybridRecommendationEngineTest" test
```

---

## 9) Limitaciones actuales

Fuera de alcance en esta feature:

- **Embeddings de texto**: solo TF-IDF, no embeddings semanticos
- **Aprendizaje de pesos dinamicos**: pesos por feature type son fijos
- **Actualizacion incremental de perfiles**: se recomputan desde cero
- **Tags**: no implementados por falta de entidad Tag en modelo de datos
- **Rating exacto en RATING**: se usa score proxy de 4.0 porque el contexto no provee review scores
- **Normalizacion de perfiles**: el perfil de usuario no se normaliza por peso total (acumulacion directa)

---

## 10) Decisiones tecnicas y desviaciones del diseno

### Diferencias respecto al diseno original (2026-03-06)

1. **Nombre de campos en ItemFeatureVector**: el diseno usaba `genreWeights`, `actorWeights`, etc.; la implementacion usa `genres`, `actors`, `directors`, `tags` para consistencia interna y simplicidad.

2. **Tipo de ID de usuario en UserProfileProvider**: el diseno usaba `Map<Long, ItemFeatureVector>` con `user.getId()` tipo Long; la implementacion usa `Map<String, ItemFeatureVector>` con `user.getId().toString()` (UUID) para consistencia con otros providers.

3. **Peso en RATING**: el diseno sugeria usar `interaction.getRating()` como peso; la implementacion usa un heuristico basado en tipo de interaccion (LIKE=5.0, VIEW=3.0) porque el rating numerico no esta disponible en el contexto actual.

4. **Tags**: reservados en la estructura pero no procesados por `ItemFeatureVectorProvider` debido a la ausencia de entidad Tag.

5. **Cache**: implementado via `getCacheExpirationSeconds()` en cada provider, no via `CachedData` interno como sugeria el diseno (la cache es manejada por el consumidor del provider).

---

## 11) Referencias

- [Recommendation Strategies Design](https://github.com/Daniel-Chavarro/tv-looker/blob/main/docs/plans/2026-03-06-recommendation-strategies-and-aggregations-design.md#2-content-based-strategy)
- Issue original: feature request para Content-Based Strategy (Phase 1 - Foundation)
