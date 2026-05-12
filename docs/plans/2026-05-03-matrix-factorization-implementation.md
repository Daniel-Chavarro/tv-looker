# Matrix Factorization (SVD) - Implementacion

**Fecha:** 2026-05-03  
**Estado:** Implementado  
**Tipo:** Feature  
**Milestone:** Phase 3 - Advanced Algorithms

---

## 1) Objetivo

Implementar estrategia de recomendacion basada en **factorizacion de matrices** usando **Singular Value Decomposition (SVD)** para descubrir factores latentes en las preferencias de usuarios.

Esta estrategia:
- Descompone la matriz usuario-item en factores latentes que capturan patrones complejos
- Complementa estrategias existentes con descubrimiento de relaciones no obvias
- Usa Apache Commons Math para la descomposicion SVD

---

## 2) Como funciona

La implementacion consta de dos componentes principales:

### MatrixFactorizationProvider (Data Provider)

1. **Construye la matriz de ratings** usuario-item desde interacciones tipo RATING
2. **Ejecuta SVD** usando `SingularValueDecomposition` de Apache Commons Math
3. **Reduce a k dimensiones** (factores latentes configurables)
4. **Almacena factores** con mappings de indices para lookup rapido

### MatrixFactorizationStrategy (Recommendation Strategy)

1. Obtiene los factores SVD del contexto via provider `"svd-factors"`
2. Para cada item candidato, calcula el **dot product** entre vector usuario e item
3. Normaliza el score a rango `[0, 1]`
4. Retorna items ordenados por score descendente

### Formula SVD

Descomposicion: **R ≈ U × Σ × V^T**

Donde:
- `R`: Matriz de ratings usuario-item (m × n)
- `U`: Matriz de usuarios (m × k)
- `Σ`: Valores singulares diagonales (k × k)
- `V^T`: Matriz de items transpuesta (k × n)

Despues de reduccion a k factores:
- `userFactors = U_k × sqrt(Σ_k)` (vectores latentes de usuarios escalados equitativamente)
- `itemFactors = V_k × sqrt(Σ_k)` (vectores latentes de items escalados equitativamente)

### Prediccion

```
score = userVector · itemVector = Σ (userFactors[i] * itemFactors[i])
```

### Normalizacion

```
normalizedScore = clamp(score / 5.0, 0.0, 1.0)
```

Los scores se dividen por 5.0 (maximo rating esperado) y se clamp a [0, 1].

---

## 3) Comportamiento clave

- **Solo ratings**: Solo procesa interacciones de tipo `RATING`, ignora VIEW/LIKE/etc.
- **Cold start**: Usuarios sin historial de ratings no reciben recomendaciones de esta estrategia
- **Matriz sparse**: Maneja eficientemente matrices grandes con pocos ratings (usa 0.0 para celdas vacias)
- **Media por usuario**: Los ratings se centran restando la media del usuario antes de SVD
- **Cache**: Los factores SVD se cachean por 1 semana (604800 segundos) para evitar recomputo
- **Explicacion**: `"Based on your overall preferences"` (generica, no especifica factores individuales)
- **sourceStrategy**: `"matrix-factorization"`

---

## 4) Configuracion

Numero de factores latentes (k):

```properties
recommendation.mf.latent-factors=50
```

- Default: 50 factores
- Minimo efectivo: `min(k, numUsers, numItems)`
- Recomendacion: 20-100 factores dependiendo del tamano del dataset

Cache:
- Configurada en codigo: 604800 segundos (1 semana)
- Reentrenamiento automatico cuando expira el cache

---

## 5) Archivos de implementacion

### Produccion

- `src/main/java/org/tvl/tvlooker/domain/strategy/recommendation/MatrixFactorizationStrategy.java`
- `src/main/java/org/tvl/tvlooker/domain/motor/utils/provider/MatrixFactorizationProvider.java`
- `src/main/java/org/tvl/tvlooker/domain/motor/utils/provider/SVDMatrixProcessor.java`
- `src/main/java/org/tvl/tvlooker/domain/motor/utils/provider/RatingMatrixBuilder.java`
- `src/main/java/org/tvl/tvlooker/domain/motor/utils/provider/RatingAccumulator.java`
- `src/main/java/org/tvl/tvlooker/domain/data_structure/SVDFactors.java`

### Testing

- `src/test/java/org/tvl/tvlooker/domain/strategy/recommendation/MatrixFactorizationStrategyTest.java`
- `src/test/java/org/tvl/tvlooker/domain/motor/utils/provider/MatrixFactorizationProviderTest.java`

---

## 6) Casos cubiertos por tests

### MatrixFactorizationStrategyTest (8 tests)

1. Nombre de estrategia correcto (`"matrix-factorization"`)
2. Lista vacia cuando no hay SVD factors disponibles
3. Lista vacia cuando el usuario no tiene factores latentes (cold user)
4. Generacion de recomendaciones para usuario con historial de ratings
5. Normalizacion de scores a rango [0, 1]
6. Ordenamiento descendente por score
7. Manejo de lista vacia de items candidatos
8. Solo recomienda items presentes en la lista candidata

### MatrixFactorizationProviderTest (15 tests)

1. Provider ID correcto (`"svd-factors"`)
2. Cache de 1 semana (604800 segundos)
3. Es cacheable (`isCacheable()` retorna true)
4. Factores vacios cuando no hay interacciones
5. Factores vacios cuando no hay interacciones de tipo RATING
6. Construccion correcta de matriz usuario-item desde ratings
7. Dimensiones correctas de matrices SVD
8. Reduccion a k factores latentes
9. Predicciones validas via dot product (no NaN, no Infinito)
10. Score 0 para usuario desconocido
11. Score 0 para item desconocido
12. Timestamp `computedAt` establecido
13. Manejo de unico usuario con unico rating (caso borde)
14. Manejo eficiente de matriz sparse grande (20 usuarios × 30 items)
15. Mapeo inverso de indices a itemId consistente

---

## 7) Ejemplo rapido

Dado:
- 2 usuarios, 3 items
- Ratings:
  - User1: Item1=4, Item2=3
  - User2: Item1=5, Item3=2

Matriz de ratings (centrada por media usuario):
```
       Item1   Item2   Item3
User1  +0.5   -0.5     0.0   (media=3.5)
User2  +1.5    0.0    -1.5   (media=3.5)
```

SVD con k=2 produce vectores latentes. Para predecir rating de User1 sobre Item3:
```
score = dot(User1_vector, Item3_vector)
```

---

## 8) Como validarlo localmente

Ejecutar tests especificos:

```powershell
Set-Location "C:\Users\user\OneDrive\Documentos\TV-Looker\tv-looker"
mvn "-Dtest=MatrixFactorizationStrategyTest" test
mvn "-Dtest=MatrixFactorizationProviderTest" test
```

Validacion de regresion:

```powershell
Set-Location "C:\Users\user\OneDrive\Documentos\TV-Looker\tv-looker"
mvn "-Dtest=HybridRecommendationEngineTest" test
```

---

## 9) Limitaciones actuales

Fuera de alcance en esta feature:

- **Implicit ALS**: Solo SVD explicito, no Alternating Least Squares para feedback implicito
- **Regularizacion**: Descomposicion pura sin regularizacion L2
- **Entrenamiento incremental**: Recomputo completo en cada reentrenamiento, no actualizacion online
- **GPU acceleration**: Computo CPU unicamente via Apache Commons Math
- **Analisis de factores**: Los factores latentes no son interpretables (no se etiquetan como "genero", "actor", etc.)
- **Cold start items**: Items sin ratings no tienen vectores latentes

Estas extensiones pueden agregarse en futuras iteraciones de la estrategia.

---

## 10) Referencias

- [Matrix Factorization Design](https://github.com/Daniel-Chavarro/tv-looker/blob/main/docs/plans/2026-03-06-recommendation-strategies-and-aggregations-design.md#5-matrix-factorization-svd)
- Apache Commons Math: https://commons.apache.org/proper/commons-math/
- SVD para recomendaciones: https://surprise.readthedocs.io/en/stable/matrix_factorization.html
