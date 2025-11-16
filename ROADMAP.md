# ProcessM Interpreter - Plan Rozwoju Projektu

**Data utworzenia:** 2025-11-16
**Status:** Plan rozwoju po zakończeniu Fazy 3

## Obecny Stan Projektu

### ✅ Ukończone (Faza 3)

#### Rdzeń PQL
- [x] Parser ANTLR4 z oficjalną gramatyką ProcessM
- [x] Query Model - pełna hierarchia Expression
- [x] Translator PQL → Cypher z parametryzacją
- [x] Scope system (LOG/TRACE/EVENT) z hoisting (^, ^^)
- [x] Standard attributes (XES IEEE 1849-2016)
- [x] **182/182 testy przechodzące** ✅

#### Klauzule SQL-podobne
- [x] SELECT (multi-scope, expressions, `*`)
- [x] WHERE (complex logic, operators, IN lists)
- [x] ORDER BY (multi-scope, ASC/DESC)
- [x] LIMIT/OFFSET (per-scope pagination)
- [x] DELETE (with WHERE, ORDER BY, LIMIT)

#### Funkcje
- [x] **Funkcje skalarne** (15 funkcji):
  - Date/time: `year`, `month`, `day`, `hour`, `minute`, `second`, `millisecond`, `quarter`, `dayofweek`, `date`, `time`, `now`
  - String: `upper`, `lower`
  - Math: `round`
- [x] **Funkcje agregujące** (5 funkcji) - **podstawowe wsparcie**:
  - `count`, `sum`, `avg`, `min`, `max`

#### Infrastruktura
- [x] Neo4j integration z Spring Boot
- [x] XES Parser & Loader
- [x] REST API (PQLQueryController)
- [x] Service layer (PQLQueryService)
- [x] Model danych (LogNode, TraceNode, EventNode)

#### Dokumentacja
- [x] QUERY_MODEL_IMPLEMENTATION.md - szczegółowa dokumentacja implementacji
- [x] README.md - podstawowa dokumentacja projektu
- [x] Testy jednostkowe z przykładami użycia

### 🔶 Częściowo Zaimplementowane

#### GROUP BY - **Podstawowe wsparcie działa**
**Status:** Parsowanie ✅, Query Model ✅, Cypher translation 🔶 (podstawowe przypadki działają)

**Co działa:**
- Parsowanie `GROUP BY` z ANTLR grammar
- Przechowywanie w Query model (`groupByStandardAttributes`, `groupByOtherAttributes`)
- Podstawowa translacja do Cypher (test `test select with group by` przechodzi)
- Implicit grouping w Cypher (Cypher nie ma explicit GROUP BY, grupuje po non-aggregated fields)

**Co wymaga rozszerzenia:**
- [ ] GROUP BY z wieloma scope'ami (`group by e:activity, t:name`)
- [ ] GROUP BY z hoisting (`group by ^e:activity`)
- [ ] GROUP BY z niestandardowymi atrybutami
- [ ] Walidacja GROUP BY (wszystkie non-aggregated SELECT fields muszą być w GROUP BY)
- [ ] Testy edge cases i złożonych przypadków

**Referencja:**
- Test: `QLToCypherVisitorTest.kt:229` (`test select with group by`)
- Komentarz: `QLToCypherVisitorTest.kt:402` ("Neo4j Cypher doesn't have explicit GROUP BY")
- QueryBuilder: `QueryBuilderTests.kt:233-256` (GROUP BY parsing tests)

### ❌ KRYTYCZNE Brakujące Funkcjonalności (Zgodność z Zadaniem)

**⚠️ UWAGA:** Poniższe funkcjonalności są **wymagane przez specyfikację zadania** i blokują pełną zgodność z wymaganiami!

#### 🔴 XES Output Format (BLOKUJĄCE!)
**Wymaganie z zadania (punkt 4):**
> "Zwrócenie logu wynikowego w formacie XES"

**Aktualny stan:**
- ✅ XESParser - parsuje XES **INPUT** (pliki → Neo4j)
- ❌ **XESWriter/XESSerializer - BRAK!**
- ❌ System zwraca JSON (`List<Map<String, Any?>>`), nie XES XML
- ❌ Brak endpoint `/api/query/execute-xes`

**Co trzeba zaimplementować:**
- [ ] XESWriter.kt - serializacja Neo4j results → XES XML
- [ ] XES extensions handling (Concept, Time, Organizational, Lifecycle)
- [ ] XES classifiers & global attributes
- [ ] Endpoint `/api/query/execute-xes` (Content-Type: application/xml)
- [ ] Integration z PQLQueryService
- [ ] Testy XESWriter
- [ ] End-to-end tests: upload XES → query → download XES

**Referencje:**
- XES Standard: http://www.xes-standard.org/
- OpenXES Library: http://www.openxes.org/

#### 🔴 ProcessM Parser Tests (BLOKUJĄCE!)
**Wymaganie z zadania:**
> "Pana system powinien przechodzić wszystkie testy zdefiniowane na repo: Testy parsera"

**Lokalizacja:** https://github.com/ProcessMPUT/processm/tree/master/processm.core/src/test/kotlin/processm/core/querylanguage

**Pliki do portowania:**
- [ ] AttributeTests.kt - testy atrybutów (standard/custom, scopes, hoisting)
- [ ] FunctionTests.kt - testy funkcji (scalar, aggregation, scope validation)
- [ ] LiteralTests.kt - testy literałów (string, number, datetime, boolean, null, UUID)
- [ ] OrderDirectionTests.kt - testy ASC/DESC
- [ ] **QueryTests.kt** - 71 testów głównego Query model (KLUCZOWE!)
- [ ] ScopeTests.kt - testy scope (LOG/TRACE/EVENT, hoisting)

**Aktualny stan:**
- Mamy tylko własne testy: QueryBuilderTests.kt (34 testy)
- Nie pokrywają wszystkich przypadków z ProcessM

#### 🔴 ProcessM Interpreter Tests (BLOKUJĄCE!)
**Wymaganie z zadania:**
> "Pana system powinien przechodzić wszystkie testy zdefiniowane na repo: Testy interpretera"

**Lokalizacja:** https://github.com/ProcessMPUT/processm/tree/master/processm.core/src/test/kotlin/processm/core/log/hierarchical

**Pliki do portowania:**
- [ ] DBHierarchicalXESInputStreamTests.kt
- [ ] DBHierarchicalXESInputStreamWithQueryTests.kt
- [ ] DBHierarchicalXESInputStreamWithSelectQueryTests.kt
- [ ] DBHierarchicalXESInputStreamWithWhereQueryTests.kt
- [ ] HoneyBadgerHierarchicalXESInputStreamTests.kt
- [ ] LogTests.kt

**Te testy weryfikują:**
- XES input streaming z query filtering
- End-to-end workflow: XES in → query → XES out
- Compatibility z ProcessM semantyką

#### 🟡 XES Benchmark Logs (Ważne dla Testowania)
**Wymaganie z zadania:**
> "Przykładowe logi znajdzie Pan na repo: https://github.com/ProcessMPUT/processm/tree/master/xes-logs"

**Aktualny stan:**
- Tylko 2 logi w projekcie: Hospital_log.xes, sample_process.xes
- Brak benchmark logs: BPIC series, Road Traffic Fine, Sepsis Cases

**Do pobrania:**
- [ ] BPIC12, BPIC13, BPIC14, BPIC15, BPIC17
- [ ] Sepsis Cases
- [ ] Road Traffic Fine Management
- [ ] CoSeLoG WABO variants

### ❌ Inne Brakujące Funkcjonalności (Nice-to-have)

#### Monitorowanie i Statystyki
- [ ] Query execution timing (PQLQueryService.kt:46 - TODO comment)
- [ ] Query statistics tracking (PQLQueryService.kt:173 - TODO comment)
- [ ] XES loading statistics (XESLoader.kt:252 - TODO comment)
- [ ] Performance metrics (execution time, memory usage)
- [ ] Query cache & optimization suggestions

#### Optymalizacja Wydajności
- [ ] Query plan optimization
- [ ] Index recommendations dla Neo4j
- [ ] Batch query execution
- [ ] Connection pooling tuning
- [ ] Memory profiling
- [ ] Benchmarking suite

#### Code Quality
- [ ] Usunięcie deprecated kodu (~750 linii w QLToCypherVisitor.kt)
- [ ] KDoc documentation dla public API
- [ ] Code coverage analysis
- [ ] Mutation testing
- [ ] Static analysis (detekt, ktlint)

#### Zaawansowane Funkcje (Nice-to-have)
- [ ] Subqueries (nie wspierane w ProcessM spec)
- [ ] Complex joins (nie wspierane w ProcessM spec)
- [ ] HAVING clause (nie wspierane w ProcessM spec)
- [ ] DISTINCT (nie wspierane w ProcessM spec)
- [ ] UNION/INTERSECT/EXCEPT (nie wspierane w ProcessM spec)

**Uwaga:** Zaawansowane funkcje wykraczają poza specyfikację ProcessM PQL i wymagają analizy czy są potrzebne.

---

## Plan Rozwoju - Kolejne Fazy

**⚠️ UWAGA:** Pierwotny plan (Fazy 4-7) zakładał że podstawowe wymagania są spełnione.
Po analizie oryginalnej specyfikacji zadania odkryto **krytyczne luki** wymagające natychmiastowej uwagi.

**Nowy plan:** Fazy 0-3 (krytyczne wymagania) → następnie Fazy 4-7 (rozszerzenia)

---

### Faza 0: Dokumentacja i Analiza (PILNE) ✅
**Cel:** Dokumentacja stanu projektu i identyfikacja luk
**Priorytet:** 🔴 KRYTYCZNY
**Czas:** 1 dzień
**Status:** ✅ **UKOŃCZONE (2025-11-16)**

#### Wykonane:
- [x] Analiza oryginalnych wymagań zadania
- [x] Identyfikacja krytycznych luk (XES output, ProcessM tests)
- [x] **Stworzenie GAP_ANALYSIS.md** - szczegółowa analiza 9 luk
- [x] **Stworzenie PROJECT_REQUIREMENTS.md** - oryginalna specyfikacja
- [x] **Aktualizacja README.md** - linki ProcessM, opis projektu, status
- [x] **Aktualizacja ROADMAP.md** - dodanie Faz 0-3 (ten dokument)
- [x] **Aktualizacja NEXT_STEPS.md** - skorygowane priorytety
- [x] **Aktualizacja QUERY_MODEL_IMPLEMENTATION.md** - wszystkie linki

**Deliverable:** ✅ Kompletna, poprawna dokumentacja z wszystkimi linkami do ProcessM

---

### Faza 1: XES Output Implementation (KRYTYCZNE) ⏳
**Cel:** Implementacja zwracania wyników w formacie XES
**Priorytet:** 🔴 **BLOKUJĄCE** - główne wymaganie zadania (punkt 4)
**Czas:** 1-2 tygodnie
**Status:** Nie rozpoczęte

**Wymaganie z zadania:**
> "4. Zwrócenie logu wynikowego w formacie XES"

#### Etap 1.1: XESWriter Core Implementation (Tydzień 1)
- [ ] **Design XESWriter API**
  - Input: Query results (List<Map<String, Any?>> z Neo4j)
  - Output: XES XML String/OutputStream
  - Interface design: `XESWriter.kt`

- [ ] **XML Structure Generation**
  - `<log>` element z attributes
  - `<trace>` elements z proper nesting
  - `<event>` elements z temporal ordering
  - Proper XML escaping dla attribute values

- [ ] **XES Extensions Support**
  - Concept extension (concept:name)
  - Time extension (time:timestamp)
  - Organizational extension (org:resource, org:group)
  - Lifecycle extension (lifecycle:transition)
  - Auto-detection extensions based on attributes

- [ ] **XES Classifiers & Global Attributes**
  - `<classifier>` elements
  - `<global>` scope attributes (trace, event)
  - Standard XES header (version, features)

**Plik:** `src/main/kotlin/com/processm/processminterpreter/xes/XESWriter.kt`

#### Etap 1.2: PQLQueryService Integration (Tydzień 1-2)
- [ ] **Nowa metoda executePQLQueryAsXES()**
  - Wrapper nad obecnym `executePQLQuery()`
  - Neo4j results → XES conversion via XESWriter
  - Return: XES XML String

- [ ] **Result Reconstruction**
  - Grouping Neo4j results by log → traces → events
  - Proper hierarchy reconstruction
  - Handling multi-scope queries

- [ ] **Attribute Mapping**
  - Neo4j properties → XES attributes
  - Type conversion (LocalDateTime → XES date format)
  - StandardAttributeMapper reverse mapping

**Plik:** `PQLQueryService.kt` - nowa metoda

#### Etap 1.3: REST Endpoint (Tydzień 2)
- [ ] **Endpoint `/api/query/execute-xes`**
  - POST handler w PQLQueryController
  - Content-Type: `application/xml`
  - Request body: PQLQueryRequest (same as execute)
  - Response: XES XML (not JSON!)

- [ ] **Content Negotiation**
  - Accept header support: `application/xml`, `application/json`
  - Single endpoint `/api/query/execute` with negotiation
  - Or separate endpoints (current approach)

**Plik:** `PQLQueryController.kt`

#### Etap 1.4: Testing (Tydzień 2)
- [ ] **Unit Tests - XESWriter**
  - Simple log → XES
  - Multi-trace log → XES
  - Events with all attribute types
  - Extensions handling
  - Edge cases (empty log, single event, special characters)

- [ ] **Integration Tests**
  - End-to-end: Upload XES → Query → Download XES
  - Round-trip: XES in → Neo4j → XES out (should be equivalent)
  - Query filtering: WHERE clause → filtered XES output
  - Multi-log queries

- [ ] **Validation**
  - XES XML schema validation
  - OpenXES library compatibility test
  - Import output XES to ProcessM (ultimate test!)

**Pliki:**
- `src/test/kotlin/.../xes/XESWriterTest.kt`
- `src/test/kotlin/.../controller/PQLQueryControllerXESTest.kt`

**Deliverables:**
- ✅ XESWriter implementation
- ✅ `/api/query/execute-xes` endpoint working
- ✅ End-to-end XES workflow tested
- ✅ All tests passing (200+ tests estimated)
- ✅ **Spełnione wymaganie zadania (punkt 4)!**

---

### Faza 2: ProcessM Parser Tests (KRYTYCZNE) ⏳
**Cel:** Port i zgodność z testami parsera ProcessM
**Priorytet:** 🔴 **BLOKUJĄCE** - wymaganie zadania
**Czas:** 1 tydzień
**Status:** Nie rozpoczęte

**Wymaganie z zadania:**
> "Pana system powinien przechodzić wszystkie testy zdefiniowane na repo: Testy parsera"

**Lokalizacja:** https://github.com/ProcessMPUT/processm/tree/master/processm.core/src/test/kotlin/processm/core/querylanguage

#### Etap 2.1: Port Testów (Dni 1-3)
- [ ] **AttributeTests.kt** → `AttributeProcessMTests.kt`
  - Testy standard attributes
  - Testy custom attributes
  - Scope attribution
  - Hoisting attributes

- [ ] **FunctionTests.kt** → `FunctionProcessMTests.kt`
  - Scalar functions (15)
  - Aggregation functions (5)
  - Function scope validation
  - Argument count validation

- [ ] **LiteralTests.kt** → `LiteralProcessMTests.kt`
  - String literals
  - Number literals (int, float, scientific notation)
  - DateTime literals (all formats)
  - Boolean literals
  - Null literals
  - UUID literals

- [ ] **OrderDirectionTests.kt** → `OrderDirectionProcessMTests.kt`
  - ASC, DESC parsing
  - Default order direction

- [ ] **QueryTests.kt** → `QueryProcessMTests.kt` ⚠️ **71 TESTÓW!**
  - SELECT validation
  - WHERE validation
  - GROUP BY validation
  - ORDER BY validation
  - DELETE validation
  - Scope rules
  - Hoisting rules

- [ ] **ScopeTests.kt** → `ScopeProcessMTests.kt`
  - LOG/TRACE/EVENT scope
  - Hoisting (^, ^^)
  - Scope compatibility

**Target folder:** `src/test/kotlin/com/processm/processminterpreter/pql/processm/`

#### Etap 2.2: Fix Failures (Dni 4-5)
- [ ] **Analiza failing tests**
  - Identyfikacja rozbieżności Query model
  - Identyfikacja missing validations
  - Identyfikacja różnic w semantyce

- [ ] **Implementation fixes**
  - Query.kt - dodanie missing validation
  - QueryBuilder.kt - poprawki parsowania
  - Expression classes - edge cases

- [ ] **Re-run & verify**
  - All ProcessM parser tests passing
  - Own tests still passing (regression check)

#### Etap 2.3: Documentation (Dzień 6-7)
- [ ] **Test compatibility matrix**
  - Które testy przechodzą
  - Które wymagały fixes
  - Known limitations (jeśli są)

- [ ] **Update QUERY_MODEL_IMPLEMENTATION.md**
  - Sekcja: ProcessM Test Compatibility
  - Link do test files
  - Validation coverage

**Deliverables:**
- ✅ Wszystkie parser tests z ProcessM portowane (~100+ testów)
- ✅ Implementation fixes dla edge cases
- ✅ Dokumentacja compatibility
- ✅ **Spełnione wymaganie validacji parsera!**

---

### Faza 3: ProcessM Interpreter Tests (KRYTYCZNE) ⏳
**Cel:** Port i zgodność z testami interpretera ProcessM
**Priorytet:** 🔴 **BLOKUJĄCE** - wymaganie zadania + walidacja Fazy 1 (XES output)
**Czas:** 2 tygodnie
**Status:** Nie rozpoczęte
**Zależność:** Wymaga ukończenia Fazy 1 (XES Output)

**Wymaganie z zadania:**
> "Pana system powinien przechodzić wszystkie testy zdefiniowane na repo: Testy interpretera"

**Lokalizacja:** https://github.com/ProcessMPUT/processm/tree/master/processm.core/src/test/kotlin/processm/core/log/hierarchical

#### Etap 3.1: Analiza ProcessM Interpreter Tests (Dni 1-2)
- [ ] **Zrozumienie DBHierarchicalXESInputStream**
  - Jak ProcessM strumieniuje XES z query filtering
  - Jak działa hierarchical access (log → trace → event)
  - Lazy loading semantics

- [ ] **Zrozumienie test patterns**
  - Setup: XES loading do PostgreSQL
  - Query execution: PQL → SQL translation
  - Result streaming: filtered XES output
  - Assertions: XES structure validation

- [ ] **Mapping do Neo4j context**
  - Neo4j nie ma streaming (all-at-once retrieval)
  - Adaptacja testów do Neo4j semantyki
  - Equivalent assertions dla XES output

#### Etap 3.2: Port Testów (Dni 3-8)
- [ ] **DBHierarchicalXESInputStreamTests.kt**
  - Basic XES streaming without query
  - Neo4j equivalent: full log retrieval → XES

- [ ] **DBHierarchicalXESInputStreamWithQueryTests.kt**
  - Generic query tests
  - Various PQL queries → filtered results

- [ ] **DBHierarchicalXESInputStreamWithSelectQueryTests.kt**
  - SELECT specific attributes
  - SELECT with expressions
  - SELECT multi-scope

- [ ] **DBHierarchicalXESInputStreamWithWhereQueryTests.kt**
  - WHERE filtering
  - Complex logic (AND, OR, NOT)
  - All operators coverage

- [ ] **HoneyBadgerHierarchicalXESInputStreamTests.kt**
  - Specific implementation tests (may not be applicable)
  - Analyze if relevant for Neo4j

- [ ] **LogTests.kt**
  - General log handling tests
  - Port applicable tests

**Target folder:** `src/test/kotlin/com/processm/processminterpreter/integration/processm/`

#### Etap 3.3: End-to-End Tests (Dni 9-11)
- [ ] **Round-trip Tests**
  - Upload XES → store in Neo4j → query → download XES
  - Verify: output XES structurally equivalent to input
  - Test with benchmark logs (Hospital, sample_process)

- [ ] **Query Filtering Tests**
  - WHERE clause filtering → verify only matching events in output
  - SELECT clause projection → verify only selected attributes
  - ORDER BY → verify event ordering in output
  - LIMIT/OFFSET → verify pagination in output

- [ ] **Multi-Log Tests**
  - Multiple logs in database
  - Query with logId filtering
  - Cross-log queries (if supported)

#### Etap 3.4: Benchmark Logs Integration (Dni 12-14)
- [ ] **Download ProcessM XES logs**
  - BPIC12, BPIC13, BPIC15, BPIC17 (key benchmarks)
  - Sepsis Cases
  - Road Traffic Fine
  - Add to `src/test/resources/logs/benchmark/`

- [ ] **Integration tests z benchmark logs**
  - Load each benchmark log
  - Run representative queries
  - Verify XES output
  - Performance measurement (optional)

- [ ] **Documentation**
  - Which benchmark logs tested
  - Known issues/limitations
  - Performance baseline

**Deliverables:**
- ✅ Wszystkie interpreter tests z ProcessM portowane
- ✅ End-to-end workflow validated
- ✅ Benchmark logs integrated
- ✅ **Spełnione wymaganie validacji interpretera!**
- ✅ **Pełna zgodność z wymaganiami zadania!** 🎉

---

### Faza 4: Kompletna Implementacja GROUP BY i Agregacji
**Cel:** Pełne wsparcie dla GROUP BY zgodne z ProcessM spec
**Priorytet:** 🔴 Wysoki
**Czas:** 2-3 tygodnie
**Status:** Nie rozpoczęte

#### Etap 4.1: Rozszerzenie GROUP BY
- [ ] **Cypher translation dla multi-scope GROUP BY**
  - Zaimplementować grouping po atrybutach z różnych scope'ów
  - Przykład: `select e:activity, t:name, count(e:id) group by e:activity, t:name`
  - Plik: `QLToCypherVisitor.kt` - metoda `translateSelectQuery()`

- [ ] **GROUP BY z hoisting**
  - Obsługa `^` i `^^` w GROUP BY
  - Przykład: `select ^e:name, count(e:id) group by ^e:name`
  - Test: `QLToCypherVisitorTest.kt:551` (istnieje, wymaga weryfikacji)

- [ ] **GROUP BY z custom attributes**
  - Wsparcie dla niestandardowych atrybutów
  - Przykład: `group by e:custom_field, t:org_group`
  - Test: `QLToCypherVisitorTest.kt:390` (istnieje)

- [ ] **Walidacja GROUP BY**
  - Implementacja reguły: wszystkie non-aggregated SELECT fields muszą być w GROUP BY
  - Exception: `InvalidGroupByException` jeśli walidacja nie przejdzie
  - Lokalizacja: `Query.kt` - metoda `validate()`

#### Etap 4.2: Agregacje w różnych kontekstach
- [ ] **Aggregation w ORDER BY**
  - Przykład: `select e:activity, count(e:id) group by e:activity order by count(e:id) desc`
  - Weryfikacja czy działa, dodanie testów

- [ ] **Nested aggregations** (jeśli wspierane przez ProcessM)
  - Analiza spec ProcessM czy wspiera
  - Jeśli tak, implementacja

- [ ] **Aggregation z hoisting w różnych kierunkach**
  - `t:count(e:id)` - count events per trace
  - `l:count(e:id)` - count events per log
  - Test wszystkich kombinacji scope + aggregation

#### Etap 4.3: Testy i Dokumentacja
- [ ] **Unit testy dla GROUP BY**
  - Co najmniej 20 testów pokrywających:
    - Single attribute GROUP BY
    - Multi-attribute GROUP BY
    - Multi-scope GROUP BY
    - GROUP BY z hoisting
    - GROUP BY z custom attributes
    - GROUP BY + ORDER BY + LIMIT
    - Edge cases (empty groups, null values)

- [ ] **Integration testy z Neo4j**
  - Testy z rzeczywistymi danymi XES (Hospital_log.xes, sample_process.xes)
  - Weryfikacja poprawności wyników agregacji
  - Performance testing na większych datasource'ach

- [ ] **Dokumentacja GROUP BY**
  - Dodanie sekcji do QUERY_MODEL_IMPLEMENTATION.md
  - Przykłady użycia w README.md
  - Known limitations

#### Deliverables:
- ✅ Pełne wsparcie GROUP BY zgodne z ProcessM spec
- ✅ Wszystkie testy przechodzące (estymacja: 200+ testów)
- ✅ Dokumentacja z przykładami
- ✅ Aktualizacja README.md (Faza 2 → ✅ Complete)

---

### Faza 5: Optymalizacja Wydajności
**Cel:** Optymalizacja wydajności zapytań i ładowania danych
**Priorytet:** 🟡 Średni
**Czas:** 2-3 tygodnie
**Status:** Nie rozpoczęte

#### Etap 5.1: Benchmarking Infrastructure
- [ ] **Framework do testów wydajnościowych**
  - JMH (Java Microbenchmark Harness) dla mikro-benchmarków
  - Gatling lub K6 dla load testingu API
  - Neo4j profiling tools integration

- [ ] **Benchmark suites**
  - Query parsing benchmarks
  - Cypher translation benchmarks
  - Neo4j query execution benchmarks
  - XES loading benchmarks (różne rozmiary plików)

- [ ] **Baseline metrics**
  - Zmierzenie obecnej wydajności
  - Ustalenie target metrics dla optimization

#### Etap 5.2: Query Optimization
- [ ] **Query plan analysis**
  - Implementacja `EXPLAIN` support dla Cypher queries
  - Analiza planów wykonania
  - Identyfikacja bottlenecków

- [ ] **Cypher optimization**
  - Optymalizacja generowanych zapytań Cypher
  - Redukcja zbędnych MATCH clauses
  - Lepsze wykorzystanie indexes
  - Query hints dla Neo4j optimizer

- [ ] **Query caching**
  - Cache dla przetłumaczonych zapytań PQL → Cypher
  - Parametryzacja cache (expiration, size limits)
  - Metrics dla cache hits/misses

#### Etap 5.3: Neo4j Optimization
- [ ] **Index strategy**
  - Analiza access patterns
  - Rekomendacje dla indexes (beyond current: logId, traceId, eventId)
  - Composite indexes dla często używanych kombinacji
  - Full-text indexes dla string searches

- [ ] **Database tuning**
  - Connection pool configuration
  - Neo4j memory settings recommendations
  - Bolt protocol optimization

- [ ] **Batch operations**
  - Batch queries dla bulk operations
  - Transaction batching dla XES loading
  - Parallel query execution (gdzie bezpieczne)

#### Etap 5.4: XES Loading Optimization
- [ ] **Streaming parser**
  - Zmiana z DOM na SAX/StAX dla dużych plików
  - Memory-efficient parsing
  - Progress reporting

- [ ] **Parallel loading**
  - Multi-threaded trace loading
  - Transaction batching optimization (tuning BATCH_SIZE)
  - Connection pooling dla parallel writes

- [ ] **Incremental loading**
  - Wsparcie dla dodawania traces do istniejącego log
  - Upsert semantics
  - Conflict resolution

#### Etap 5.5: Performance Testing & Validation
- [ ] **Load testing**
  - Symulacja 100, 1000, 10000 concurrent users
  - Stress testing do failure point
  - Endurance testing (długotrwałe obciążenie)

- [ ] **Regression testing**
  - Automated performance regression detection
  - CI/CD integration dla performance tests
  - Alerts na performance degradation

- [ ] **Dokumentacja wyników**
  - Performance report z baseline vs optimized
  - Best practices guide dla deployments
  - Tuning recommendations

#### Deliverables:
- ✅ Benchmarking suite
- ✅ 50%+ improvement w query execution time (target)
- ✅ 2x faster XES loading (target)
- ✅ Performance documentation
- ✅ Tuning guide dla production deployments

---

### Faza 6: Production Readiness
**Cel:** Przygotowanie systemu do wdrożenia produkcyjnego
**Priorytet:** 🟡 Średni
**Czas:** 2-3 tygodnie
**Status:** Nie rozpoczęte

#### Etap 6.1: Monitoring & Observability
- [ ] **Metrics collection**
  - Implementacja query execution timing (PQLQueryService.kt:46)
  - Query statistics tracking (PQLQueryService.kt:173)
  - XES loading statistics (XESLoader.kt:252)
  - Micrometer integration dla Spring Boot Actuator

- [ ] **Health checks**
  - Neo4j connectivity check
  - Database size monitoring
  - Query queue depth
  - Memory usage metrics

- [ ] **Distributed tracing**
  - Spring Cloud Sleuth integration
  - Trace IDs przez cały request flow
  - Integration z Zipkin/Jaeger

- [ ] **Logging enhancement**
  - Structured logging (JSON format)
  - Correlation IDs
  - Log levels configuration per package
  - Sensitive data masking

#### Etap 6.2: API Enhancements
- [ ] **API versioning**
  - Version prefix w endpoints (`/api/v1/query`)
  - Deprecation policy
  - Backward compatibility strategy

- [ ] **Rate limiting**
  - Request rate limiting per user/IP
  - Concurrent query limits
  - Query timeout enforcement

- [ ] **API documentation**
  - OpenAPI/Swagger specification
  - Interactive API docs (Swagger UI)
  - Example requests/responses
  - Error code documentation

- [ ] **Security hardening**
  - Input validation enhancement
  - SQL injection prevention (currently: parameterized queries ✅)
  - XSS prevention
  - CORS configuration refinement
  - Authentication/Authorization (JWT tokens?)

#### Etap 6.3: Error Handling & Resilience
- [ ] **Graceful degradation**
  - Fallback strategies dla Neo4j failures
  - Circuit breaker pattern dla external dependencies
  - Retry logic z exponential backoff

- [ ] **Error reporting**
  - Detailed error messages dla developers
  - User-friendly error messages dla end-users
  - Error tracking (Sentry, Rollbar integration?)
  - Error rate alerting

- [ ] **Validation improvements**
  - Enhanced PQL syntax validation messages
  - Semantic validation with suggestions
  - Query complexity limits (prevent DoS)

#### Etap 6.4: Code Quality & Cleanup
- [ ] **Deprecated code removal**
  - Usunięcie ~750 linii deprecated code z QLToCypherVisitor.kt
  - Verification że wszystkie testy nadal przechodzą
  - Documentation deprecated removals

- [ ] **Code documentation**
  - KDoc dla wszystkich public API classes/methods
  - Package-level documentation
  - Architecture decision records (ADRs)

- [ ] **Code quality tools**
  - Detekt configuration & enforcement
  - Ktlint formatting
  - SonarQube analysis
  - Code coverage target: 80%+

- [ ] **Dependency management**
  - Audit dependencies (security vulnerabilities)
  - Update to latest stable versions
  - Dependency license compliance

#### Etap 6.5: Deployment & Operations
- [ ] **Containerization**
  - Multi-stage Dockerfile optimization
  - Docker image size reduction
  - Health check in Docker

- [ ] **Kubernetes deployment**
  - Helm charts
  - Resource limits & requests
  - HPA (Horizontal Pod Autoscaler)
  - Liveness & readiness probes

- [ ] **CI/CD pipeline**
  - Automated testing on PR
  - Automated deployment to staging
  - Blue-green deployment strategy
  - Rollback procedures

- [ ] **Backup & Recovery**
  - Neo4j backup strategy
  - Disaster recovery plan
  - Data retention policy

#### Deliverables:
- ✅ Production-ready monitoring
- ✅ Comprehensive API documentation
- ✅ 80%+ code coverage
- ✅ Clean codebase (no deprecated code)
- ✅ Deployment automation
- ✅ Operations runbook

---

### Faza 7: Advanced Features & Extensibility
**Cel:** Rozszerzenie funkcjonalności poza ProcessM spec (optional)
**Priorytet:** 🟢 Niski
**Czas:** 3-4 tygodnie
**Status:** Nie rozpoczęte
**Uwaga:** Ta faza wykracza poza oryginalną specyfikację ProcessM - do rozważenia czy potrzebna

#### Etap 7.1: Analiza Potrzeb
- [ ] **Stakeholder interviews**
  - Zebranie wymagań od użytkowników
  - Use cases dla advanced features
  - Priority ranking

- [ ] **Competitive analysis**
  - Analiza innych process mining tools
  - Feature gap analysis
  - Industry best practices

#### Etap 7.2: DISTINCT (jeśli potrzebne)
- [ ] **Spec design**
  - Semantyka DISTINCT w kontekście process mining
  - Impact na performance

- [ ] **Implementation**
  - Grammar extension
  - Query model update
  - Cypher translation (DISTINCT w RETURN)

- [ ] **Testing & documentation**

#### Etap 7.3: HAVING clause (jeśli potrzebne)
- [ ] **Spec design**
  - HAVING vs WHERE (post-aggregation filtering)
  - Syntax design

- [ ] **Implementation**
  - Grammar extension
  - Query model update
  - Cypher translation

- [ ] **Testing & documentation**

#### Etap 7.4: Subqueries (jeśli potrzebne)
- [ ] **Spec design**
  - Subquery syntax
  - Correlation rules
  - Scope handling w subqueries

- [ ] **Implementation**
  - Grammar extension (nested queries)
  - Query model recursive support
  - Cypher translation (WITH clauses)

- [ ] **Testing & documentation**

#### Etap 7.5: Process Mining Specific Features
- [ ] **Process discovery queries**
  - Variant analysis
  - Directly-follows graph extraction
  - Bottleneck detection queries

- [ ] **Conformance checking support**
  - Model-log alignment queries
  - Deviation detection

- [ ] **Performance analysis**
  - Throughput time calculations
  - Waiting time analysis
  - Resource utilization queries

- [ ] **Social network analysis**
  - Handover of work patterns
  - Resource interaction graphs

#### Deliverables:
- ✅ Advanced feature set (based on stakeholder needs)
- ✅ Extended PQL spec documentation
- ✅ Process mining specific query library
- ✅ Use case examples & tutorials

---

## Priorytetyzacja

**⚠️ SKORYGOWANE:** Po analizie oryginalnych wymagań, priorytety zostały całkowicie zmienione!

### Priorytet 0 (PILNE) - Natychmiastowo ✅
**UKOŃCZONE 2025-11-16**
1. **Faza 0: Dokumentacja i Analiza** - Identyfikacja krytycznych luk

### Priorytet 1 (KRYTYCZNY - BLOKUJĄCE) - Q1 2025 (4-5 tygodni)
**Te zadania są WYMAGANE przez specyfikację zadania i blokują zgodność z wymaganiami!**

1. **Faza 1: XES Output** (1-2 tyg) - 🔴 **GŁÓWNE WYMAGANIE ZADANIA**
   - Wymaganie: "Zwrócenie logu wynikowego w formacie XES"
   - Obecnie: BRAK (zwraca JSON, nie XES XML)
   - Blokujące: Faza 3 (interpreter tests)

2. **Faza 2: ProcessM Parser Tests** (1 tyg) - 🔴 **WYMAGANIE VALIDACJI**
   - Wymaganie: "System powinien przechodzić wszystkie testy parsera"
   - Obecnie: BRAK (6 plików testowych nie portowanych, 71+ testów)

3. **Faza 3: ProcessM Interpreter Tests** (2 tyg) - 🔴 **WYMAGANIE VALIDACJI**
   - Wymaganie: "System powinien przechodzić wszystkie testy interpretera"
   - Obecnie: BRAK (6 plików testowych nie portowanych)
   - Wymaga: Faza 1 (XES output) aby działać
   - Bonus: Benchmark logs integration

**Po Faz 1-3 → Pełna zgodność z wymaganiami zadania! 🎉**

### Priorytet 2 (Wysoki) - Q2 2025 (2-3 tygodnie)
4. **Faza 4: Complete GROUP BY** - Dokończenie częściowo zaimplementowanej funkcjonalności
5. **Faza 5: Performance Optimization** - Benchmarking, optimization, tuning

### Priorytet 3 (Średni) - Q2-Q3 2025 (2-3 tygodnie)
6. **Faza 6: Production Readiness** - Monitoring, security, deployment
   - 6.1: Monitoring & Observability (3 TODOs)
   - 6.2-6.3: API & Error handling
   - 6.4: Deprecated code cleanup (~750 linii)
   - 6.5: Deployment automation

### Priorytet 4 (Niski) - Q4 2025+ (3-4 tygodnie)
7. **Faza 7: Advanced Features** - Wykraczające poza ProcessM spec (do rozważenia)

---

## Metryki Sukcesu

### Faza 4 (GROUP BY)
- ✅ Wszystkie testy ProcessM GROUP BY przechodzą
- ✅ Multi-scope GROUP BY działa poprawnie
- ✅ 200+ testów przechodzi

### Faza 5 (Performance)
- ✅ 50%+ szybsze query execution
- ✅ 2x szybsze XES loading
- ✅ Benchmark suite w CI/CD

### Faza 6 (Production)
- ✅ 80%+ code coverage
- ✅ Zero deprecated code
- ✅ Full API documentation
- ✅ Automated deployment working
- ✅ 99.9% uptime w staging (1 miesiąc)

### Faza 7 (Advanced)
- ✅ User adoption metrics
- ✅ Feature usage analytics
- ✅ Positive user feedback

---

## Zasoby i Ryzyka

### Wymagane Zasoby
- **Development time:** 10-15 tygodni (Fazy 4-6)
- **Infrastructure:** Neo4j cluster dla performance testing
- **Tools:** JMH, Gatling, Micrometer, SonarQube

### Główne Ryzyka
1. **Performance regression** podczas implementacji GROUP BY
   - Mitigation: Continuous benchmarking w CI/CD

2. **Compatibility z ProcessM** - odejście od spec
   - Mitigation: Reference tests z ProcessM repo

3. **Neo4j limitations** dla complex GROUP BY
   - Mitigation: Early prototyping, fallback strategies

4. **Resource constraints** dla performance testing
   - Mitigation: Cloud-based testing infrastructure

---

## Referencje

### ProcessM Official
- **Repository:** https://github.com/ProcessMPUT/processm
- **PQL Spec:** https://github.com/ProcessMPUT/processm/blob/master/docs/pql.md
- **Grammar:** processm/src/main/antlr/QL.g4
- **Query Model:** processm/src/main/kotlin/put/processm/ql/model/
- **Tests:** processm/src/test/kotlin/put/processm/ql/model/QueryTests.kt

### Standards
- **XES Standard:** http://www.xes-standard.org/ (IEEE 1849-2016)
- **Neo4j Cypher:** https://neo4j.com/docs/cypher-manual/
- **ANTLR4:** https://www.antlr.org/

### Internal Documentation
- **Implementation:** [QUERY_MODEL_IMPLEMENTATION.md](QUERY_MODEL_IMPLEMENTATION.md)
- **README:** [README.md](README.md)

---

**Ostatnia aktualizacja:** 2025-11-16
**Następny przegląd:** Po zakończeniu Fazy 4

**Autorzy:**
- Plan rozwoju: Claude (Anthropic) + User
- Based on: ProcessM by PUT (Poznan University of Technology)

**Licencja:** MIT (zgodna z ProcessM)
