// Types grouped by SpringBank technical layer.
MATCH (type:Type)
WHERE type.fqn STARTS WITH 'com.banking_microservices.'
RETURN
  CASE
    WHEN type.fqn CONTAINS '.controller.' THEN 'controller'
    WHEN type.fqn CONTAINS '.service.' THEN 'service'
    WHEN type.fqn CONTAINS '.repository.' THEN 'repository'
    WHEN type.fqn CONTAINS '.grpc.' THEN 'grpc'
    WHEN type.fqn CONTAINS '.kafka.' THEN 'kafka'
    WHEN type.fqn CONTAINS '.dto.' THEN 'dto'
    WHEN type.fqn CONTAINS '.domain.' OR type.fqn CONTAINS '.model.' THEN 'domain-model'
    ELSE 'other'
  END AS layer,
  count(type) AS typeCount
ORDER BY typeCount DESC;

// Direct dependencies that cross technical layers.
MATCH (source:Type)-[:DEPENDS_ON]->(target:Type)
WHERE source.fqn STARTS WITH 'com.banking_microservices.'
  AND target.fqn STARTS WITH 'com.banking_microservices.'
RETURN source.fqn AS source, target.fqn AS target
ORDER BY source, target;

// Largest application types, useful for detecting classes that collect too many responsibilities.
MATCH (type:Type)-[:DECLARES]->(method:Method)
WHERE type.fqn STARTS WITH 'com.banking_microservices.'
RETURN type.fqn AS type, count(method) AS methodCount
ORDER BY methodCount DESC
LIMIT 30;
