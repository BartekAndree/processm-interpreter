# Wymagania Projektu - ProcessM Interpreter

**Data:** 2025-11-16
**Źródło:** Oryginalna specyfikacja zadania

---

## 📋 Opis Zadania

Praca dotyczy stworzenia **nowego interpretera języka PQL** dla systemu ProcessM (https://processm.cs.put.poznan.pl).

Ten interpreter powinien być **osobnym komponentem**, który udostępniałby przez REST podstawowe operacje:
- **CRUD na logach** (Create, Read, Update, Delete)
- **Interpreter języka zapytań PQL**

---

## 🎯 Główne Wymagania

### 1. Format Danych: XES Standard

Podstawowym formatem danych, który system powinien obsłużyć jest **XES** (eXtensible Event Stream).

**Standard:** IEEE 1849-2016 (http://www.xes-standard.org/)

**Przykładowe logi:**
- Repository: https://github.com/ProcessMPUT/processm/tree/master/xes-logs
- Zawiera: BPIC series, Hospital, Road Traffic Fine, Sepsis Cases, CoSeLoG WABO i wiele innych

### 2. Język Zapytań: PQL (Process Query Language)

**Specyfikacja języka:**
- Dokumentacja: https://github.com/ProcessMPUT/processm/blob/master/docs/pql.md
- Artykuł: Sekcja 5 zawiera wiele przykładów zapytań PQL (w załączeniu do zadania)

**Parser (ANTLR4):**
- Grammar: https://github.com/ProcessMPUT/processm/tree/master/processm.core/src/main/antlr4/processm/core/querylanguage
- Reprezentacja obiektowa (Kotlin): https://github.com/ProcessMPUT/processm/blob/master/processm.core/src/main/kotlin/processm/core/querylanguage/Query.kt

**Obecny interpreter ProcessM (do zastąpienia):**
- Lokalizacja: https://github.com/ProcessMPUT/processm/blob/master/processm.core/src/main/kotlin/processm/core/log/hierarchical/TranslatedQuery.kt
- Problem: Tłumaczy PQL na serię zapytań SQL do PostgreSQL
- **To nie jest efektywne rozwiązanie** ← kluczowa motywacja projektu

---

## 🔄 Podstawowy Przypadek Użycia (Usługa REST)

System powinien obsługiwać następujący workflow:

### Krok 1: Załadowanie Logu XES do Bazy ✅
**Wymaganie:**
- Potencjalnie **wiele logów** może być załadowanych jednocześnie
- Nadanie unikalnego `identity:id` jeśli log go nie ma
- ProcessM używa tego podejścia

**Status implementacji:** ✅ ZREALIZOWANE
- `POST /api/logs/upload` - endpoint do uploadu XES
- `XESParser` - parsowanie XES XML
- `XESLoader` - ładowanie do Neo4j z batch processing
- Generowanie unique `logId` jeśli nie podane

### Krok 2: Odebranie Zapytania PQL ✅
**Wymaganie:**
- REST endpoint przyjmujący zapytanie PQL jako string

**Status implementacji:** ✅ ZREALIZOWANE
- `POST /api/query/execute` - endpoint wykonania zapytania
- `POST /api/query/validate` - endpoint walidacji zapytania
- `PQLQueryController` - obsługa HTTP requests

### Krok 3: Interpretacja/Tłumaczenie na Język Bazy NoSQL ✅
**Wymaganie:**
- Translacja PQL na język bazy NoSQL
- W naszym przypadku: **Neo4j Cypher**

**Status implementacji:** ✅ ZREALIZOWANE
- `AntlrPQLTranslator` - parser ANTLR4
- `QueryBuilder` - budowanie Query model z parse tree
- `QLToCypherVisitor` - translacja Query → Cypher
- Parametryzowane zapytania (security)
- 182/182 testy przechodzące

### Krok 4: Zwrócenie Logu Wynikowego w Formacie XES ❌
**Wymaganie:**
- Wynik zapytania zwracany w **formacie XES**
- Zgodnie ze standardem http://www.xes-standard.org/

**Status implementacji:** ❌ **BRAK - KRYTYCZNA LUKA!**
- Obecnie: wyniki zwracane jako JSON (`List<Map<String, Any?>>`)
- Brakuje: `XESWriter` / `XESSerializer`
- Brakuje: endpoint zwracający `application/xml`
- **To jest główny brakujący element zadania!**

---

## ✅ Wymagania Testowe

### Wymaganie: Przejście WSZYSTKICH Testów z ProcessM Repo

> "Pana system powinien przechodzić wszystkie testy zdefiniowane na repo"

#### Testy Parsera ❌
**Lokalizacja:** https://github.com/ProcessMPUT/processm/tree/master/processm.core/src/test/kotlin/processm/core/querylanguage

**Pliki testowe:**
1. `AttributeTests.kt` - testy atrybutów
2. `FunctionTests.kt` - testy funkcji
3. `LiteralTests.kt` - testy literałów
4. `OrderDirectionTests.kt` - testy kierunku sortowania
5. `QueryTests.kt` - **71 testów** głównego Query model
6. `ScopeTests.kt` - testy zakresów (LOG/TRACE/EVENT)

**Status:** ❌ **NIE PORTOWANE**
- Mamy własne testy: `QueryBuilderTests.kt` (34 testy)
- Nie pokrywają wszystkich przypadków z ProcessM

#### Testy Interpretera ❌
**Lokalizacja:** https://github.com/ProcessMPUT/processm/tree/master/processm.core/src/test/kotlin/processm/core/log/hierarchical

**Pliki testowe:**
1. `DBHierarchicalXESInputStreamTests.kt`
2. `DBHierarchicalXESInputStreamWithQueryTests.kt`
3. `DBHierarchicalXESInputStreamWithSelectQueryTests.kt`
4. `DBHierarchicalXESInputStreamWithWhereQueryTests.kt`
5. `HoneyBadgerHierarchicalXESInputStreamTests.kt`
6. `LogTests.kt`

**Status:** ❌ **NIE PORTOWANE**
- Mamy własne testy: `QLToCypherVisitorTest.kt` (34 testy)
- Brak testów end-to-end (XES in → query → XES out)

---

## 🏗️ Architektura Docelowa

### Komponenty Systemu

```
┌─────────────────────────────────────────┐
│         REST API (Spring Boot)          │
├─────────────────────────────────────────┤
│  Upload XES  │  Execute Query  │  CRUD  │
└──────┬───────┴────────┬────────┴────────┘
       │                │
       ▼                ▼
┌─────────────┐  ┌──────────────┐
│ XES Parser  │  │ PQL Parser   │
│  (Input)    │  │  (ANTLR4)    │
└──────┬──────┘  └──────┬───────┘
       │                │
       │                ▼
       │         ┌──────────────┐
       │         │ Query Model  │
       │         └──────┬───────┘
       │                │
       │                ▼
       │         ┌─────────────────┐
       │         │ Cypher Visitor  │
       │         │  (Translation)  │
       │         └──────┬──────────┘
       │                │
       ▼                ▼
   ┌────────────────────────┐
   │   Neo4j Graph DB       │
   │  (Data Storage)        │
   └──────┬─────────────────┘
          │
          ▼
   ┌──────────────┐
   │ XESWriter    │  ← BRAKUJĄCY KOMPONENT!
   │  (Output)    │
   └──────┬───────┘
          │
          ▼
   ┌──────────────┐
   │  XES Result  │
   └──────────────┘
```

### Model Danych Neo4j

```
(Log) -[:CONTAINS]-> (Trace) -[:HAS_EVENT]-> (Event)
                                    │
                                    └-[:FOLLOWS]-> (Event)

Properties:
- Log: logId, name, attributes (XES extensions)
- Trace: traceId, caseId, attributes
- Event: eventId, activity, timestamp, resource, lifecycle, cost, attributes
```

---

## 🎨 Wymagania Funkcjonalne

### REST API Endpoints (Wymagane)

#### 1. Zarządzanie Logami (CRUD)
```
POST   /api/logs/upload         - Upload XES file
GET    /api/logs/{logId}        - Pobranie metadanych logu
GET    /api/logs                - Lista wszystkich logów
DELETE /api/logs/{logId}        - Usunięcie logu
```

**Status:** ✅ Zaimplementowane (LogController.kt)

#### 2. Wykonywanie Zapytań PQL
```
POST   /api/query/execute       - Wykonanie zapytania (JSON response)
POST   /api/query/execute-xes   - Wykonanie zapytania (XES response) ← BRAKUJE!
POST   /api/query/validate      - Walidacja składni PQL
GET    /api/query/statistics    - Statystyki zapytań
GET    /api/query/features      - Wspierane funkcje PQL
```

**Status:** 🔶 Częściowo (brak XES output)

### Funkcjonalności PQL (Wymagane)

Zgodnie z specyfikacją PQL (https://github.com/ProcessMPUT/processm/blob/master/docs/pql.md):

#### ✅ Zaimplementowane Klauzule:
- [x] SELECT (standard attributes, expressions, multi-scope, `*`)
- [x] WHERE (complex logic, operators: `=`, `!=`, `<`, `>`, `<=`, `>=`, `LIKE`)
- [x] WHERE (IN lists, IS NULL, IS NOT NULL, AND, OR, NOT)
- [x] ORDER BY (ASC/DESC, multi-scope)
- [x] LIMIT (per-scope)
- [x] OFFSET (per-scope)
- [x] DELETE (with WHERE, ORDER BY, LIMIT)

#### 🔶 Częściowo Zaimplementowane:
- [~] GROUP BY (podstawowe przypadki działają, wymaga rozszerzenia)
  - [x] Single attribute GROUP BY
  - [ ] Multi-scope GROUP BY
  - [ ] GROUP BY z hoisting
  - [ ] Pełna walidacja GROUP BY rules

#### ✅ Zaimplementowane Funkcje:

**Funkcje Skalarne (15):**
- [x] Date/time: `year`, `month`, `day`, `hour`, `minute`, `second`, `millisecond`, `quarter`, `dayofweek`, `date`, `time`, `now`
- [x] String: `upper`, `lower`
- [x] Math: `round`

**Funkcje Agregujące (5):**
- [x] `count`, `sum`, `avg`, `min`, `max` (podstawowe wsparcie)

#### ✅ Inne Funkcjonalności:
- [x] Scope system (LOG, TRACE, EVENT)
- [x] Hoisting (`^`, `^^`)
- [x] Standard attributes (XES IEEE 1849-2016)
- [x] Custom attributes
- [x] Literals (string, number, datetime, boolean, null, UUID)

---

## 📊 Wymagania Niefunkcjonalne

### Wydajność
**Motywacja projektu:**
> "Obecnie ProcessM korzysta z interpretera, który tłumaczy PQL na serię zapytań SQL do PostgreSQL. To nie jest efektywne rozwiązanie."

**Wymaganie:**
- System powinien być **wydajniejszy** niż PostgreSQL-based interpreter
- Graph database (Neo4j) lepiej pasuje do hierarchical event data
- Optymalizacja zapytań Cypher

**Status:** 🟡 Do zmierzenia (benchmark needed)

### Skalowalność
- Obsługa wielu logów jednocześnie
- Batch processing dla dużych XES files
- Connection pooling dla Neo4j

**Status:** 🔶 Częściowo (XESLoader ma batch processing)

### Bezpieczeństwo
- Parametryzowane zapytania Cypher (SQL injection prevention)
- Input validation dla PQL
- CORS configuration

**Status:** ✅ Podstawowe zabezpieczenia zaimplementowane

### Testowalność
**Wymaganie:**
> "Pana system powinien przechodzić wszystkie testy zdefiniowane na repo"

- Unit tests
- Integration tests
- End-to-end tests
- Compatibility tests z ProcessM

**Status:** ❌ ProcessM tests nie portowane

---

## 🔗 Kluczowe Referencje

### ProcessM System & Dokumentacja
- **ProcessM Official:** https://processm.cs.put.poznan.pl
- **ProcessM Repository:** https://github.com/ProcessMPUT/processm
- **PQL Specification:** https://github.com/ProcessMPUT/processm/blob/master/docs/pql.md
- **Artykuł z przykładami PQL:** (załącznik do zadania - Sekcja 5)

### ProcessM Implementation
- **Parser Grammar (ANTLR4):** https://github.com/ProcessMPUT/processm/tree/master/processm.core/src/main/antlr4/processm/core/querylanguage
- **Query Model (Kotlin):** https://github.com/ProcessMPUT/processm/blob/master/processm.core/src/main/kotlin/processm/core/querylanguage/Query.kt
- **Original Interpreter (PostgreSQL):** https://github.com/ProcessMPUT/processm/blob/master/processm.core/src/main/kotlin/processm/core/log/hierarchical/TranslatedQuery.kt

### ProcessM Tests (WYMAGANE)
- **Parser Tests:** https://github.com/ProcessMPUT/processm/tree/master/processm.core/src/test/kotlin/processm/core/querylanguage
  - AttributeTests, FunctionTests, LiteralTests, OrderDirectionTests, QueryTests (71), ScopeTests
- **Interpreter Tests:** https://github.com/ProcessMPUT/processm/tree/master/processm.core/src/test/kotlin/processm/core/log/hierarchical
  - DBHierarchicalXESInputStreamTests, WithQueryTests, WithSelectQueryTests, WithWhereQueryTests

### Data & Standards
- **Example XES Logs:** https://github.com/ProcessMPUT/processm/tree/master/xes-logs
  - BPIC series, Hospital, Road Traffic Fine, Sepsis Cases, CoSeLoG WABO
- **XES Standard (IEEE 1849-2016):** http://www.xes-standard.org/
- **OpenXES Library:** http://www.openxes.org/

### Technology Stack
- **Neo4j Graph Database:** https://neo4j.com/
- **Neo4j Cypher Manual:** https://neo4j.com/docs/cypher-manual/
- **ANTLR4:** https://www.antlr.org/
- **Spring Boot:** https://spring.io/projects/spring-boot
- **Kotlin:** https://kotlinlang.org/

---

## ✅ Status Realizacji Wymagań

### ✅ ZREALIZOWANE (70%)

| Wymaganie | Status | Notatki |
|-----------|--------|---------|
| Standalone REST Component | ✅ | Spring Boot REST API |
| XES Input (Upload) | ✅ | XESParser + XESLoader |
| XES Parsing | ✅ | XML DOM parsing, proper attribute handling |
| Multiple Logs Support | ✅ | Unique logId generation |
| PQL Parser (ANTLR4) | ✅ | Official ProcessM grammar |
| Query Model | ✅ | Full Expression hierarchy |
| Scope System | ✅ | LOG/TRACE/EVENT + hoisting |
| Standard Attributes | ✅ | XES IEEE 1849-2016 |
| PQL → Cypher Translation | ✅ | QLToCypherVisitor |
| Parameterized Queries | ✅ | Security (SQL injection prevention) |
| SELECT Queries | ✅ | Multi-scope, expressions, * |
| WHERE Queries | ✅ | Complex logic, all operators |
| ORDER BY | ✅ | ASC/DESC, multi-scope |
| LIMIT/OFFSET | ✅ | Per-scope pagination |
| DELETE Queries | ✅ | With WHERE, ORDER BY, LIMIT |
| Scalar Functions | ✅ | 15 functions (date/time, string, math) |
| Aggregation Functions | 🔶 | 5 functions (podstawowe wsparcie) |
| GROUP BY | 🔶 | Podstawowe przypadki działają |
| Neo4j Storage | ✅ | Hierarchical model (Log→Trace→Event) |
| Batch Processing | ✅ | XESLoader batch operations |
| REST CRUD | ✅ | Upload, get, list, delete logs |
| Query Execution API | 🔶 | Execute, validate (JSON only) |

### ❌ NIEZREALIZOWANE (30%) - KRYTYCZNE LUKI

| Wymaganie | Status | Priorytet |
|-----------|--------|-----------|
| **XES Output** | ❌ | 🔴 KRYTYCZNY |
| XESWriter/Serializer | ❌ | 🔴 KRYTYCZNY |
| `/api/query/execute-xes` endpoint | ❌ | 🔴 KRYTYCZNY |
| **ProcessM Parser Tests** | ❌ | 🔴 KRYTYCZNY |
| Port AttributeTests.kt | ❌ | 🔴 KRYTYCZNY |
| Port FunctionTests.kt | ❌ | 🔴 KRYTYCZNY |
| Port LiteralTests.kt | ❌ | 🔴 KRYTYCZNY |
| Port OrderDirectionTests.kt | ❌ | 🔴 KRYTYCZNY |
| Port QueryTests.kt (71 tests) | ❌ | 🔴 KRYTYCZNY |
| Port ScopeTests.kt | ❌ | 🔴 KRYTYCZNY |
| **ProcessM Interpreter Tests** | ❌ | 🔴 KRYTYCZNY |
| End-to-end XES workflow tests | ❌ | 🔴 KRYTYCZNY |
| **Benchmark XES Logs** | ❌ | 🟡 Ważny |
| GROUP BY Multi-scope | ❌ | 🟢 Średni |
| GROUP BY Hoisting | ❌ | 🟢 Średni |
| GROUP BY Validation | ❌ | 🟢 Średni |
| Query Statistics | ❌ | 🟢 Średni |
| Performance Benchmarking | ❌ | 🟢 Średni |

---

## 📝 Podsumowanie

### Co Działa Dobrze ✅
1. **Solidne fundamenty:** Parser, Query Model, Cypher Translation
2. **XES Input:** Pełna obsługa ładowania XES do Neo4j
3. **PQL Support:** SELECT, WHERE, ORDER BY, LIMIT/OFFSET, DELETE
4. **Funkcje:** Wszystkie skalarne, podstawowe agregujące
5. **REST API:** CRUD operations na logach
6. **Testy:** 182/182 własne testy przechodzące

### Krytyczne Luki ❌
1. **XES Output:** Brak serializacji wyników do XES XML
2. **ProcessM Tests:** Nie portowane, nie zwalidowane
3. **Dokumentacja:** Brak kontekstu ProcessM, brak wymaganych linków

### Szacowany Czas do Pełnej Zgodności
**5-6 tygodni** (Fazy 0-4 z GAP_ANALYSIS.md):
- Faza 0: Dokumentacja - 1 dzień
- Faza 1: XES Output - 1-2 tygodnie
- Faza 2: Parser Tests - 1 tydzień
- Faza 3: Interpreter Tests - 2 tygodnie
- Faza 4: Benchmark Logs & Validation - 3-4 dni

---

**Data utworzenia:** 2025-11-16
**Ostatnia aktualizacja:** 2025-11-16
**Status:** Dokument wymagań bazujący na oryginalnej specyfikacji zadania
