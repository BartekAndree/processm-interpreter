grammar PQL;

// ========================================
// PARSER RULES
// ========================================

/**
 * Root rule - query must be a complete select statement
 */
query
    : selectStatement EOF
    ;

/**
 * SELECT statement with all possible clauses
 * Structure: SELECT [DISTINCT] <fields> FROM <source> [WHERE <condition>]
 *            [GROUP BY <fields>] [HAVING <condition>] [ORDER BY <fields>] [LIMIT <number>]
 */
selectStatement
    : SELECT distinct=DISTINCT? selectList
      FROM fromClause
      (WHERE whereClause)?
      (GROUP BY groupByClause)?
      (HAVING havingClause)?
      (ORDER BY orderByClause)?
      (LIMIT limitValue=NUMBER)?
    ;

/**
 * SELECT field list - either * or comma-separated list of fields/aggregates
 */
selectList
    : STAR                                   # SelectAll
    | selectItem (COMMA selectItem)*         # SelectItems
    ;

/**
 * Single item in SELECT clause - can be aggregate function or regular field
 */
selectItem
    : aggregateFunction (AS alias=identifier)?   # SelectAggregate
    | field=identifier (AS alias=identifier)?    # SelectField
    ;

/**
 * Aggregate functions: COUNT, AVG, SUM, MIN, MAX
 */
aggregateFunction
    : COUNT LPAREN (STAR | identifier) RPAREN
    | (AVG | SUM | MIN | MAX) LPAREN identifier RPAREN
    ;

/**
 * FROM clause - specifies data source (log, trace, or event)
 */
fromClause
    : LOG | TRACE | EVENT
    ;

/**
 * WHERE clause - boolean expression with OR precedence
 */
whereClause
    : orCondition
    ;

/**
 * OR has lower precedence than AND
 */
orCondition
    : andCondition (OR andCondition)*
    ;

/**
 * AND has higher precedence than OR
 */
andCondition
    : condition (AND condition)*
    ;

/**
 * Individual condition - supports various operators and parentheses for grouping
 */
condition
    : identifier IS notNull=NOT? NULL                                    # IsNullCondition
    | identifier BETWEEN value1=STRING AND value2=STRING                 # BetweenCondition
    | identifier IN LPAREN stringList RPAREN                             # InCondition
    | identifier comparisonOp value=(STRING | NUMBER)                    # ComparisonCondition
    | LPAREN orCondition RPAREN                                          # ParenCondition
    ;

/**
 * Comparison operators
 */
comparisonOp
    : EQ | NEQ | LT | GT | LTE | GTE | LIKE
    ;

/**
 * GROUP BY clause - comma-separated list of field names
 */
groupByClause
    : identifier (COMMA identifier)*
    ;

/**
 * HAVING clause - condition on aggregated results
 */
havingClause
    : field=identifier comparisonOp valueExpr
    ;

/**
 * Value expression - can be a number or an identifier (field/alias reference)
 */
valueExpr
    : NUMBER
    | identifier
    ;

/**
 * ORDER BY clause - comma-separated list of field names with optional direction
 */
orderByClause
    : orderByItem (COMMA orderByItem)*
    ;

/**
 * Single ORDER BY item with optional ASC/DESC direction
 */
orderByItem
    : identifier direction=(ASC | DESC)?
    ;

/**
 * List of string literals (for IN operator)
 */
stringList
    : STRING (COMMA STRING)*
    ;

// ========================================
// LEXER RULES (TOKENS)
// ========================================

// SQL Keywords - case insensitive
SELECT   : [Ss][Ee][Ll][Ee][Cc][Tt];
DISTINCT : [Dd][Ii][Ss][Tt][Ii][Nn][Cc][Tt];
FROM     : [Ff][Rr][Oo][Mm];
WHERE    : [Ww][Hh][Ee][Rr][Ee];
GROUP    : [Gg][Rr][Oo][Uu][Pp];
BY       : [Bb][Yy];
HAVING   : [Hh][Aa][Vv][Ii][Nn][Gg];
ORDER    : [Oo][Rr][Dd][Ee][Rr];
LIMIT    : [Ll][Ii][Mm][Ii][Tt];
AS       : [Aa][Ss];
AND      : [Aa][Nn][Dd];
OR       : [Oo][Rr];
IN       : [Ii][Nn];
BETWEEN  : [Bb][Ee][Tt][Ww][Ee][Ee][Nn];
IS       : [Ii][Ss];
NULL     : [Nn][Uu][Ll][Ll];
NOT      : [Nn][Oo][Tt];
LIKE     : [Ll][Ii][Kk][Ee];
ASC      : [Aa][Ss][Cc];
DESC     : [Dd][Ee][Ss][Cc];

// Aggregate functions
COUNT : [Cc][Oo][Uu][Nn][Tt];
AVG   : [Aa][Vv][Gg];
SUM   : [Ss][Uu][Mm];
MIN   : [Mm][Ii][Nn];
MAX   : [Mm][Aa][Xx];

// Table names (data sources)
LOG   : [Ll][Oo][Gg];
TRACE : [Tt][Rr][Aa][Cc][Ee];
EVENT : [Ee][Vv][Ee][Nn][Tt];

// Comparison operators
EQ  : '=';
NEQ : '!=' | '<>';
LT  : '<';
GT  : '>';
LTE : '<=';
GTE : '>=';

// Delimiters and symbols
LPAREN : '(';
RPAREN : ')';
COMMA  : ',';
STAR   : '*';

// Literals - supports integers and floating point numbers
NUMBER : [0-9]+ ('.' [0-9]+)?;

// String literals - single or double quoted
STRING
    : '\'' (~['\\] | '\\' .)* '\''
    | '"' (~["\\] | '\\' .)* '"'
    ;

// Identifiers - field names, aliases, etc.
// Supports namespace prefixes (e.g., org:group, concept:name, time:timestamp)
IDENTIFIER : [a-zA-Z_][a-zA-Z0-9_]* (':' [a-zA-Z_][a-zA-Z0-9_]*)?;

// Whitespace - skip
WS : [ \t\r\n]+ -> skip;

// ========================================
// PARSER RULES FOR IDENTIFIERS
// ========================================

/**
 * Identifier rule that allows keywords to be used as field names/aliases
 * This solves the problem of reserved keywords like 'count', 'sum', etc.
 */
identifier
    : IDENTIFIER
    | COUNT | AVG | SUM | MIN | MAX     // Allow aggregate function names as identifiers
    | LOG | TRACE | EVENT                // Allow table names as identifiers
    | ASC | DESC                         // Allow sort directions as identifiers
    ;