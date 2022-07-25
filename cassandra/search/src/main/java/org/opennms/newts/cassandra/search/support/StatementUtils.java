package org.opennms.newts.cassandra.search.support;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opennms.newts.cassandra.ContextConfigurations;

import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchStatementBuilder;
import com.datastax.oss.driver.api.core.cql.BatchableStatement;
import com.datastax.oss.driver.api.core.cql.DefaultBatchType;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class StatementUtils {

    public static List<Statement> getStatements(ContextConfigurations contextConfigurations, int maxBatchSize,
                                                Set<StatementGenerator> generators) {
        List<Statement> statementsToExecute = Lists.newArrayList();

        Map<String, List<BatchableStatement>> statementsByKey = Maps.newHashMap();
        for (StatementGenerator generator : generators) {
            BatchableStatement statement = generator.toStatement()
                    .setConsistencyLevel(contextConfigurations.getWriteConsistency(generator.getContext()));
            String key = generator.getKey();
            if (key == null) {
                // Don't try batching these
                statementsToExecute.add(statement);
                continue;
            }

            // Group these by key
            List<BatchableStatement> statementsForKey = statementsByKey.get(key);
            if (statementsForKey == null) {
                statementsForKey = Lists.newArrayList();
                statementsByKey.put(key, statementsForKey);
            }
            statementsForKey.add(statement);
        }

        // Consolidate the grouped statements into batches
        for (List<BatchableStatement> statementsForKey: statementsByKey.values()) {
            for (List<BatchableStatement> partition : Lists.partition(statementsForKey, maxBatchSize)) {
                BatchStatementBuilder builder = BatchStatement.builder(DefaultBatchType.UNLOGGED);
                for (BatchableStatement statement : partition) {
                    builder.addStatement(statement);
                }
                statementsToExecute.add(builder.build());
            }
        }

        return statementsToExecute;
    }
}
