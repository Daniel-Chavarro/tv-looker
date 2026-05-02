# Ranking-Based Aggregation (Borda Count) - Implementacion

**Fecha:** 2026-04-19  
**Estado:** Implementado  
**Tipo:** Feature  
**Milestone:** Completa la estrategia de agregacion basada en ranking (Phase 3)

---

## 1) Objetivo

Agregar una estrategia de agregacion alternativa a las basadas en score absoluto, usando **consenso por ranking** con **Borda Count**.

Esta estrategia es util cuando:
- Las estrategias tienen escalas de score diferentes.
- Queremos que importe mas la posicion relativa de cada item que la magnitud del score.
- Buscamos una combinacion mas robusta entre estrategias heterogeneas.

---

## 2) Como funciona

La estrategia implementada es `RankingBasedAggregation` y aplica estos pasos:

1. Toma los resultados ordenados de cada estrategia (`Map<String, List<ScoredItem>>`).
2. Para cada estrategia, asigna puntos Borda por posicion.
3. Suma puntos por item entre todas las estrategias.
4. Normaliza el puntaje final a rango `[0, 1]`.
5. Devuelve una lista final ordenada por score descendente.

### Formula Borda usada

Para una lista de longitud `N` y un item en posicion `rankIndex` (base 0):

- `points = N - rankIndex`

Equivalente a la forma clasica `N - rank + 1` con rank base 1.

### Normalizacion

- `maxPossiblePoints = suma de N_i` (longitud de cada lista de estrategia)
- `normalizedScore = totalPointsItem / maxPossiblePoints`

Con esto, todos los scores finales quedan en `[0, 1]`.

---

## 3) Comportamiento clave

- **Items parciales:** si un item aparece en algunas estrategias y en otras no, solo suma donde aparece.
- **Listas de distinta longitud:** soportado de forma nativa (`N` se calcula por estrategia).
- **Sin resultados:** si no hay items rankeados, retorna lista vacia.
- **Explicacion:** cada item devuelve `"Consensus from X strategies"`.
- **sourceStrategy:** `"ranking-based-borda-count"`.
- **Empates:** desempate determinista por `itemId` ascendente tras ordenar por score descendente.

---

## 4) Configuracion

La seleccion de agregacion se hace con:

```properties
recommendation.aggregation.type=ranking
```

La resolucion de esta opcion esta en `RecommendationConfig`, dentro del `switch` de `aggregationStrategy(...)`.

> Nota: los pesos (`recommendation.weights.*`) aplican a estrategias tipo convexa; en `ranking`, la logica se basa en posiciones (Borda), no en pesos.

---

## 5) Archivos de implementacion

### Produccion

- `src/main/java/org/tvl/tvlooker/domain/strategy/aggregation/RankingBasedAggregation.java`
- `src/main/java/org/tvl/tvlooker/config/RecommendationConfig.java`

### Testing

- `src/test/java/org/tvl/tvlooker/domain/strategy/aggregation/RankingBasedAggregationTest.java`

---

## 6) Casos cubiertos por tests

`RankingBasedAggregationTest` valida:

1. Nombre de agregacion correcto.
2. Calculo Borda + normalizacion con ejemplo manual conocido.
3. Manejo de listas de diferente longitud.
4. Manejo de entradas vacias / sin items rankeados.
5. Garantia de score final en rango `[0, 1]`.

---

## 7) Ejemplo rapido

Dado:

- Strategy A: `[Item1, Item2, Item3]` (`N=3`)
- Strategy B: `[Item2, Item1, Item4]` (`N=3`)

Puntos Borda:

- Item1: `3 + 2 = 5`
- Item2: `2 + 3 = 5`
- Item3: `1 + 0 = 1`
- Item4: `0 + 1 = 1`

Normalizado (`maxPossiblePoints = 3 + 3 = 6`):

- Item1: `5/6`
- Item2: `5/6`
- Item3: `1/6`
- Item4: `1/6`

---

## 8) Como validarlo localmente

```powershell
Set-Location "C:\Users\user\OneDrive\Documentos\TV-Looker\tv-looker"
mvn "-Dtest=RankingBasedAggregationTest" test
```

Validacion de regresion minima:

```powershell
Set-Location "C:\Users\user\OneDrive\Documentos\TV-Looker\tv-looker"
mvn "-Dtest=HybridRecommendationEngineTest,RecommendationServiceTest" test
```

Para correr app usando esta agregacion sin cambiar archivos:

```powershell
Set-Location "C:\Users\user\OneDrive\Documentos\TV-Looker\tv-looker"
mvn "-Dspring-boot.run.arguments=--recommendation.aggregation.type=ranking" spring-boot:run
```

---

## 9) Limitaciones actuales

Fuera de alcance en esta feature:

- Pesos por estrategia en rank fusion.
- Penalizacion por desacuerdos.
- Otras tecnicas (RRF, CombMNZ, etc.).

Estas extensiones pueden agregarse en futuras estrategias de agregacion.

