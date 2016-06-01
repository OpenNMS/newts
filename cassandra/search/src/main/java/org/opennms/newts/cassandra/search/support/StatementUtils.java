package org.opennms.newts.cassandra.search.support;

import static com.datastax.driver.core.querybuilder.QueryBuilder.unloggedBatch;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opennms.newts.cassandra.ContextConfigurations;

import com.datastax.driver.core.RegularStatement;
import com.datastax.driver.core.Statement;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class StatementUtils {

    public static List<Statement> getStatements(ContextConfigurations contextConfigurations, int maxBatchSize,
            Set<StatementGenerator> generators) {
        List<Statement> statementsToExecute = Lists.newArrayList();

        Map<String, List<Statement>> statementsByKey = Maps.newHashMap();
        for (StatementGenerator generator : generators) {
            Statement statement = generator.toStatement()
                    .setConsistencyLevel(contextConfigurations.getWriteConsistency(generator.getContext()));
            String key = generator.getKey();
            if (key == null) {
                // Don't try batching these
                statementsToExecute.add(statement);
                continue;
            }

            // Group these by key
            List<Statement> statementsForKey = statementsByKey.get(key);
            if (statementsForKey == null) {
                statementsForKey = Lists.newArrayList();
                statementsByKey.put(key, statementsForKey);
            }
            statementsForKey.add(statement);
        }

        // Consolidate the grouped statements into batches
        for (List<Statement> statementsForKey: statementsByKey.values()) {
            for (List<Statement> partition : Lists.partition(statementsForKey, maxBatchSize)) {
                statementsToExecute.add(unloggedBatch(partition.toArray(new RegularStatement[partition.size()])));
            }
        }

        return statementsToExecute;
    }
}
