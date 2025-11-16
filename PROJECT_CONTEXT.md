# ProcessM Interpreter - Project Context

**Quick Reference dla Agentów/LLM**
**Data:** 2025-11-16
**Status:** 70% zrealizowane, 30% krytyczne luki

---

## 🎯 TL;DR (Executive Summary)

**Co to jest:**
Alternatywny interpreter języka PQL (Process Query Language) dla systemu ProcessM, wykorzystujący **Neo4j graph database** zamiast PostgreSQL dla wydajniejszego przetwarzania hierarchicznych event logs w formacie XES.

**Główny problem:**
Oryginalny ProcessM interpreter tłumaczy PQL na serię zapytań SQL do PostgreSQL - to jest **nieefektywne** dla hierarchical data (Log → Trace → Event).

**Nasze rozwiązanie:**
Standalone REST component używający Neo4j Cypher dla native graph operations.

**Status implementacji:**
- ✅ 70% działa: Parser, Query Model, Cypher translation, XES input, REST API
- ❌ 30% BRAK: **XES output** (główne wymaganie!), ProcessM tests

---

## 🚨 KRYTYCZNE: Co NIE Działa

### 1. XES Output - GŁÓWNE Wymaganie Zadania (punkt 4)
**Wymaganie:**
> "4. Zwrócenie logu wynikowego w formacie XES"

**Aktualny stan:**
- ✅ System przyjmuje XES **input** (XESParser, XESLoader)
- ❌ **System zwraca JSON, nie XES XML!**
- ❌ Brak `XESWriter` / `XESSerializer`
- ❌ Brak endpoint `/api/query/execute-xes`

**Impact:** System nie spełnia głównego wymagania zadania!

**Pliki do stworzenia:**
- `src/main/kotlin/.../xes/XESWriter.kt` - serializacja results → XES XML
- `PQLQueryController.kt` - endpoint `/api/query/execute-xes`
- `PQLQueryService.kt` - metoda `executePQLQueryAsXES()`

**Czas:** 1-2 tygodnie

### 2. ProcessM Tests - Wymaganie Walidacji
**Wymaganie:**
> "Pana system powinien przechodzić wszystkie testy zdefiniowane na repo"

**Aktualny stan:**
- ✅ Własne testy: 182/182 passing
- ❌ **ProcessM Parser Tests: NIE PORTOWANE** (6 plików, 71+ testów)
- ❌ **ProcessM Interpreter Tests: NIE PORTOWANE** (6 plików)

**Impact:** Brak walidacji zgodności z ProcessM semantyką!

**Testy do portowania:**
- Parser: AttributeTests, FunctionTests, LiteralTests, OrderDirectionTests, **QueryTests (71)**, ScopeTests
- Interpreter: DBHierarchicalXESInputStreamTests, WithQuery/Select/WhereTests

**Czas:** 3 tygodnie (1 tyg parser + 2 tyg interpreter)

---

## 📋 Wymagania Zadania (4 Kroki Use Case)

**Podstawowy workflow REST API:**

1. ✅ **Załadowanie logu XES do bazy**
   - Endpoint: `POST /api/logs/upload`
   - Implementation: XESParser, XESLoader
   - Neo4j model: Log → Trace → Event
   - Status: **DZIAŁA**

2. ✅ **Odebranie zapytania PQL**
   - Endpoint: `POST /api/query/execute`
   - Implementation: PQLQueryController
   - Status: **DZIAŁA**

3. ✅ **Interpretacja/tłumaczenie na Cypher**
   - Parser: ANTLR4 (official ProcessM grammar)
   - Query Model: Full Expression hierarchy
   - Translation: QLToCypherVisitor
   - Status: **DZIAŁA** (182/182 tests)

4. ❌ **Zwrócenie wyniku w formacie XES**
   - Current: Zwraca JSON `List<Map<String, Any?>>`
   - Required: XES XML (zgodny z IEEE 1849-2016)
   - Status: **NIE DZIAŁA** - KRYTYCZNA LUKA!

---

## 🏗️ Architektura (Quick View)

```
┌─────────────────────────────────────────┐
│         REST API (Spring Boot)          │
│  /api/logs/*  │  /api/query/*           │
└──────┬────────┴─────────┬────────────────┘
       │                  │
       ▼                  ▼
   XESParser          PQLParser
       │              (ANTLR4)
       │                  │
       │                  ▼
       │            QueryBuilder
       │                  │
       │                  ▼
       │            Query Model
       │            (Expression tree)
       │                  │
       │                  ▼
       │           QLToCypherVisitor
       │                  │
       ▼                  ▼
   ┌─────────────────────────┐
   │   Neo4j Graph DB        │
   │                         │
   │  (Log)-[:CONTAINS]→     │
   │      (Trace)-[:HAS]→    │
   │         (Event)-[:F]→   │
   └─────────┬───────────────┘
             │
             ▼
        [BRAKUJE!]
       XESWriter ← NIE ZAIMPLEMENTOWANY!
             │
             ▼
       XES XML Output
```

### Model Danych Neo4j

```cypher
// Nodes
(:Log {logId, name, attributes})
(:Trace {traceId, caseId, attributes})
(:Event {eventId, activity, timestamp, resource, lifecycle, cost, attributes})

// Relationships
(Log)-[:CONTAINS]->(Trace)
(Trace)-[:HAS_EVENT]->(Event)
(Event)-[:FOLLOWS]->(Event)
```

---

## ✅ Co Działa (70%)

### PQL Query Language - Pełne Wsparcie
**Klauzule:**
- ✅ SELECT (multi-scope, expressions, `*`, standard attributes)
- ✅ WHERE (all operators: =, !=, <, >, <=, >=, LIKE, IN, IS NULL, AND, OR, NOT)
- ✅ ORDER BY (ASC/DESC, multi-scope)
- ✅ LIMIT/OFFSET (per-scope pagination)
- ✅ DELETE (with WHERE, ORDER BY, LIMIT)
- 🔶 GROUP BY (basic cases work, needs: multi-scope, hoisting, validation)

**Funkcje:**
- ✅ Scalar (15): year, month, day, hour, minute, second, millisecond, quarter, dayofweek, date, time, now, upper, lower, round
- ✅ Aggregation (5): count, sum, avg, min, max (basic support)

**Features:**
- ✅ Scope system (LOG/TRACE/EVENT)
- ✅ Hoisting (`^`, `^^`)
- ✅ Standard attributes (XES IEEE 1849-2016)
- ✅ Custom attributes
- ✅ Literals (string, number, datetime, boolean, null, UUID)
- ✅ Parameterized queries (security)

### Infrastructure
- ✅ Spring Boot REST API
- ✅ Neo4j integration (batch processing, indexes)
- ✅ XES Parser (DOM-based XML parsing)
- ✅ XES Loader (batch operations, FOLLOWS relationships)
- ✅ Error handling & logging
- ✅ Docker Compose setup

### Tests
- ✅ 182/182 własne testy passing
- ✅ QLToCypherVisitorTest (34 tests)
- ✅ QueryBuilderTests (34 tests)
- ✅ Integration tests z Testcontainers

---

## ❌ Co Nie Działa / Brakuje (30%)

### Krytyczne (Blokujące zgodność z zadaniem)
1. ❌ XESWriter/XESSerializer
2. ❌ Endpoint `/api/query/execute-xes`
3. ❌ ProcessM Parser Tests (6 files, 71+ tests)
4. ❌ ProcessM Interpreter Tests (6 files)

### Ważne
5. ❌ XES Benchmark Logs (tylko 2/100+ w projekcie)
6. 🔶 GROUP BY multi-scope, hoisting, validation
7. ❌ Query/Loading statistics (3 TODO comments)

### Opcjonalne
8. ❌ Deprecated code cleanup (~750 lines)
9. ❌ Performance benchmarking
10. ❌ API documentation (Swagger)

---

## 🔗 Kluczowe Linki ProcessM (WAŻNE!)

### ProcessM Official
- **Website:** https://processm.cs.put.poznan.pl
- **Repository:** https://github.com/ProcessMPUT/processm
- **PQL Spec:** https://github.com/ProcessMPUT/processm/blob/master/docs/pql.md

### Implementation References
- **Grammar (ANTLR4):** https://github.com/ProcessMPUT/processm/tree/master/processm.core/src/main/antlr4/processm/core/querylanguage
- **Query Model:** https://github.com/ProcessMPUT/processm/blob/master/processm.core/src/main/kotlin/processm/core/querylanguage/Query.kt
- **Original Interpreter:** https://github.com/ProcessMPUT/processm/blob/master/processm.core/src/main/kotlin/processm/core/log/hierarchical/TranslatedQuery.kt

### Tests (WYMAGANE do portowania)
- **Parser Tests:** https://github.com/ProcessMPUT/processm/tree/master/processm.core/src/test/kotlin/processm/core/querylanguage
- **Interpreter Tests:** https://github.com/ProcessMPUT/processm/tree/master/processm.core/src/test/kotlin/processm/core/log/hierarchical

### Data
- **Example Logs:** https://github.com/ProcessMPUT/processm/tree/master/xes-logs (100+ files)
- **XES Standard:** http://www.xes-standard.org/ (IEEE 1849-2016)
- **OpenXES:** http://www.openxes.org/

---

## 📚 Project Documentation

**Przeczytaj w tej kolejności dla pełnego kontekstu:**

1. **[PROJECT_CONTEXT.md](PROJECT_CONTEXT.md)** ← TEN PLIK (quick reference)
2. **[PROJECT_REQUIREMENTS.md](PROJECT_REQUIREMENTS.md)** - oryginalna specyfikacja zadania (16KB)
3. **[GAP_ANALYSIS.md](GAP_ANALYSIS.md)** - analiza 9 luk, 3 krytyczne (18KB)
4. **[README.md](README.md)** - główna dokumentacja projektu (12KB)
5. **[ROADMAP.md](ROADMAP.md)** - plan rozwoju Fazy 0-7 (34KB)
6. **[NEXT_STEPS.md](NEXT_STEPS.md)** - kolejne kroki z priorytetami (8KB)
7. **[QUERY_MODEL_IMPLEMENTATION.md](QUERY_MODEL_IMPLEMENTATION.md)** - implementacja Query Model (11KB)

**Dla szybkiego startu agenta:**
- Przeczytaj: PROJECT_CONTEXT.md (ten plik)
- Przeczytaj: GAP_ANALYSIS.md (krytyczne luki)
- Zacznij: Faza 1 (XES Output) lub Faza 2 (Parser Tests)

---

## 🗂️ Struktura Kodu (Key Files)

```
src/main/kotlin/com/processm/processminterpreter/
├── controller/
│   ├── LogController.kt           # REST: upload, get, delete logs
│   └── PQLQueryController.kt      # REST: execute, validate queries
│                                   # ❌ BRAK: /execute-xes endpoint
├── service/
│   ├── LogService.kt              # Log CRUD logic
│   └── PQLQueryService.kt         # Query execution
│                                   # ❌ BRAK: executePQLQueryAsXES()
├── pql/
│   ├── model/                     # Query Model (Expression hierarchy)
│   │   ├── Query.kt               # Main query class
│   │   ├── Expression.kt          # Base expression
│   │   ├── Attribute.kt           # Attributes (standard/custom)
│   │   ├── Function.kt            # Scalar + aggregation functions
│   │   ├── Literal.kt             # All literal types
│   │   ├── Scope.kt               # LOG/TRACE/EVENT
│   │   └── ... (10+ files)
│   ├── visitor/
│   │   ├── QueryBuilder.kt        # ANTLR → Query model
│   │   └── QLToCypherVisitor.kt   # Query → Cypher translation
│   │                               # (~750 lines deprecated code)
│   ├── AntlrPQLTranslator.kt      # Main translator facade
│   └── StandardAttributeMapper.kt # XES attribute mapping
├── xes/
│   ├── XESParser.kt               # ✅ XES → Neo4j (INPUT)
│   ├── XESLoader.kt               # ✅ Batch loading to Neo4j
│   └── [XESWriter.kt]             # ❌ BRAK! Neo4j → XES (OUTPUT)
├── model/
│   ├── LogNode.kt                 # Neo4j @Node entity
│   ├── TraceNode.kt               # Neo4j @Node entity
│   └── EventNode.kt               # Neo4j @Node entity
└── repository/
    └── LogRepository.kt           # Neo4j repository

src/main/antlr/
├── QLLexer.g4                     # ANTLR lexer (ProcessM official)
└── QLParser.g4                    # ANTLR parser (ProcessM official)

src/test/kotlin/.../pql/
├── QLToCypherVisitorTest.kt       # ✅ 34 tests (Cypher translation)
├── visitor/
│   └── QueryBuilderTests.kt      # ✅ 34 tests (Query building)
└── [processm/]                    # ❌ BRAK! ProcessM tests folder
    ├── [AttributeProcessMTests.kt]
    ├── [FunctionProcessMTests.kt]
    ├── [LiteralProcessMTests.kt]
    ├── [OrderDirectionProcessMTests.kt]
    ├── [QueryProcessMTests.kt]    # 71 tests!
    └── [ScopeProcessMTests.kt]
```

---

## 🚀 Priorytety (Dla Agenta)

### Priorytet 1: XES Output (1-2 tygodnie) 🔴
**Główne wymaganie zadania - obecnie NIE spełnione!**

**Do zrobienia:**
1. Stwórz `XESWriter.kt`:
   - Input: `List<Map<String, Any?>>` (Neo4j results)
   - Output: XES XML String
   - Features: extensions, classifiers, global attributes

2. Dodaj do `PQLQueryService.kt`:
   - Metoda: `executePQLQueryAsXES(pql: String, logId: String?): String`
   - Rekonstrukcja hierarchii: results → Log → Traces → Events

3. Dodaj do `PQLQueryController.kt`:
   - Endpoint: `POST /api/query/execute-xes`
   - Response: `Content-Type: application/xml`

4. Testy:
   - XESWriterTest
   - End-to-end: upload XES → query → download XES → verify

**Referencje:**
- XES Standard: http://www.xes-standard.org/
- XESParser.kt (input) jako wzór
- Hospital_log.xes (example XES structure)

### Priorytet 2: ProcessM Parser Tests (1 tydzień) 🔴
**Wymaganie walidacji - obecnie NIE spełnione!**

**Do zrobienia:**
1. Port 6 plików testowych z ProcessM:
   - AttributeTests → AttributeProcessMTests.kt
   - FunctionTests → FunctionProcessMTests.kt
   - LiteralTests → LiteralProcessMTests.kt
   - OrderDirectionTests → OrderDirectionProcessMTests.kt
   - **QueryTests → QueryProcessMTests.kt (71 tests!)**
   - ScopeTests → ScopeProcessMTests.kt

2. Fix failures:
   - Identify gaps in Query model
   - Add missing validations
   - Edge cases handling

3. Verify:
   - All ProcessM parser tests passing
   - Own tests still passing (regression)

**Lokalizacja:**
- Source: https://github.com/ProcessMPUT/processm/tree/master/processm.core/src/test/kotlin/processm/core/querylanguage
- Target: `src/test/kotlin/.../pql/processm/`

### Priorytet 3: ProcessM Interpreter Tests (2 tygodnie) 🔴
**Wymaganie walidacji end-to-end**

**Wymaga ukończenia:** Priorytet 1 (XES Output)

**Do zrobienia:**
1. Zrozumienie ProcessM interpreter tests semantyki
2. Port 6 plików testowych (XES streaming + query filtering)
3. End-to-end tests: XES in → query → XES out
4. Download benchmark logs (BPIC12, BPIC15, Sepsis)

**Lokalizacja:**
- Source: https://github.com/ProcessMPUT/processm/tree/master/processm.core/src/test/kotlin/processm/core/log/hierarchical
- Logs: https://github.com/ProcessMPUT/processm/tree/master/xes-logs

---

## 💡 Quick Tips dla Agenta

### Gdy pracujesz nad XES Output:
1. Zobacz jak XESParser parsuje input (to samo potrzebujesz w reverse)
2. XES format wymaga proper XML escaping
3. Extensions auto-detect na podstawie użytych attributes
4. Zachowaj kolejność: log → traces → events → FOLLOWS
5. Date format: `yyyy-MM-dd'T'HH:mm:ss.SSSXXX`

### Gdy portujesz ProcessM tests:
1. Nie kopiuj tylko testów - zrozum CO testują
2. Adaptuj do Neo4j context (jeśli potrzeba)
3. Jeśli test failuje → to odkrycie luki w implementacji
4. ProcessM używa PostgreSQL, my Neo4j - różnice są OK jeśli semantyka się zgadza

### Gdy debugujesz Query translation:
1. Włącz debug logging: `QLToCypherVisitor.kt`
2. Zobacz wygenerowane Cypher + parameters
3. Test w Neo4j Browser: http://localhost:7474
4. Standard attributes mapping: `StandardAttributeMapper.kt`

### Przydatne komendy:
```bash
# Tests
./gradlew test                    # All tests
./gradlew test --tests "*XES*"    # Only XES tests

# Neo4j
docker-compose up -d neo4j        # Start Neo4j
docker-compose logs -f neo4j      # View logs
docker exec -it processm-interpreter-neo4j-1 cypher-shell -u neo4j -p password123

# Build
./gradlew build                   # Full build
./gradlew bootRun                 # Run application
```

---

## 📊 Metryki

**Obecny stan:**
- Linie kodu: ~15,000 (główny kod) + ~5,000 (testy)
- Testy: 182/182 własne ✅ | ProcessM: 0/150+ ❌
- Code coverage: ? (do zmierzenia)
- Deprecated code: ~750 linii (QLToCypherVisitor.kt)

**Target (po Fazach 1-3):**
- Linie kodu: ~18,000 (główny) + ~8,000 (testy)
- Testy: 300+ total ✅
- XES output: ✅ DZIAŁA
- ProcessM compatibility: ✅ ZWALIDOWANA
- Zgodność z zadaniem: ✅ 100%

**Czas do zgodności:** 4-5 tygodni

---

## ⚠️ Znane Problemy / TODOs w Kodzie

1. `PQLQueryService.kt:46` - TODO: Add timing
2. `PQLQueryService.kt:173` - TODO: Implement query statistics
3. `XESLoader.kt:252` - TODO: Implement loading statistics
4. `QLToCypherVisitor.kt:66-721` - Deprecated old visitor code (~750 lines)
5. GROUP BY - multi-scope, hoisting, validation needed
6. No XESWriter - CRITICAL GAP
7. No ProcessM tests - CRITICAL GAP

---

## 🎓 Kluczowe Koncepty

**PQL (Process Query Language):**
- SQL-like language dla process mining
- Hierarchical scopes: LOG → TRACE → EVENT
- Hoisting: `^` (up one level), `^^` (up two levels)
- Standard attributes: XES IEEE 1849-2016

**XES (eXtensible Event Stream):**
- XML format dla event logs
- Extensions: Concept, Time, Organizational, Lifecycle
- Classifiers: sposób klasyfikacji events
- Global attributes: default values per scope

**Neo4j vs PostgreSQL:**
- PostgreSQL: relational, wymaga JOINs dla hierarchii
- Neo4j: graph, native hierarchy traversal
- Cypher: pattern matching dla graph queries
- Better fit dla LOG→TRACE→EVENT model

**ProcessM:**
- Process Mining system (Poznan University of Technology)
- Official PQL implementation (PostgreSQL-based)
- Our project: alternative implementation (Neo4j-based)
- Must be compatible with ProcessM semantics

---

## 📞 Kontakt / Źródła

**ProcessM:**
- Team: PUT (Poznan University of Technology)
- Website: https://processm.cs.put.poznan.pl
- GitHub: https://github.com/ProcessMPUT/processm

**Ten projekt:**
- Język: Kotlin
- Framework: Spring Boot
- Database: Neo4j
- Parser: ANTLR4
- Status: 70% done, 30% critical gaps
- Licencja: MIT (zgodna z ProcessM)

---

**Ostatnia aktualizacja:** 2025-11-16
**Wersja dokumentu:** 1.0
**Dla pytań zobacz:** GAP_ANALYSIS.md, PROJECT_REQUIREMENTS.md

**Start tutaj → Understand gaps → Implement XES Output → Port tests → Done!** 🚀
