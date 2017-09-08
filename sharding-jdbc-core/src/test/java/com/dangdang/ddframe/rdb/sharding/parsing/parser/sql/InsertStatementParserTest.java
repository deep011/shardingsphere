/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.dangdang.ddframe.rdb.sharding.parsing.parser.sql;

import com.dangdang.ddframe.rdb.sharding.api.ListShardingValue;
import com.dangdang.ddframe.rdb.sharding.api.rule.DataSourceRule;
import com.dangdang.ddframe.rdb.sharding.api.rule.ShardingRule;
import com.dangdang.ddframe.rdb.sharding.api.rule.TableRule;
import com.dangdang.ddframe.rdb.sharding.api.strategy.sharding.NoneShardingAlgorithm;
import com.dangdang.ddframe.rdb.sharding.constant.DatabaseType;
import com.dangdang.ddframe.rdb.sharding.constant.ShardingOperator;
import com.dangdang.ddframe.rdb.sharding.keygen.fixture.IncrementKeyGenerator;
import com.dangdang.ddframe.rdb.sharding.parsing.SQLParsingEngine;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.context.condition.Column;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.context.condition.Condition;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.exception.SQLParsingUnsupportedException;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.sql.dml.insert.InsertStatement;
import com.dangdang.ddframe.rdb.sharding.routing.strategy.complex.ComplexShardingStrategy;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class InsertStatementParserTest extends AbstractStatementParserTest {
    
    @Test
    public void assertParseWithoutParameter() throws SQLException {
        ShardingRule shardingRule = createShardingRule();
        SQLParsingEngine statementParser = new SQLParsingEngine(DatabaseType.MySQL, "INSERT INTO `TABLE_XXX` (`field1`, `field2`) VALUES (10, 1)", shardingRule);
        InsertStatement insertStatement = (InsertStatement) statementParser.parse();
        assertInsertStatementWithoutParameter(insertStatement);
    }
    
    @Test
    public void assertParseWithParameter() {
        ShardingRule shardingRule = createShardingRule();
        SQLParsingEngine statementParser = new SQLParsingEngine(DatabaseType.MySQL, "INSERT INTO TABLE_XXX (field1, field2) VALUES (?, ?)", shardingRule);
        InsertStatement insertStatement = (InsertStatement) statementParser.parse();
        assertInsertStatementWithParameter(insertStatement);
    }
    
    @Test
    public void assertParseWithGenerateKeyColumnsWithoutParameter() throws SQLException {
        ShardingRule shardingRule = createShardingRuleWithGenerateKeyColumns();
        SQLParsingEngine statementParser = new SQLParsingEngine(DatabaseType.MySQL, "INSERT INTO `TABLE_XXX` (`field1`) VALUES (10)", shardingRule);
        InsertStatement insertStatement = (InsertStatement) statementParser.parse();
        assertInsertStatementWithoutParameter(insertStatement);
    }
    
    @SuppressWarnings("unchecked")
    private void assertInsertStatementWithoutParameter(final InsertStatement insertStatement) {
        assertThat(insertStatement.getTables().find("TABLE_XXX").get().getName(), is("TABLE_XXX"));
        Condition condition = insertStatement.getConditions().find(new Column("field1", "TABLE_XXX")).get();
        assertThat(condition.getOperator(), is(ShardingOperator.EQUAL));
        assertThat(((ListShardingValue<? extends Comparable>) condition.getShardingValue(Collections.emptyList())).getValues().iterator().next(), is((Comparable) 10));
    }
    
    @Test
    public void assertParseWithGenerateKeyColumnsWithParameter() throws SQLException {
        ShardingRule shardingRule = createShardingRuleWithGenerateKeyColumns();
        SQLParsingEngine statementParser = new SQLParsingEngine(DatabaseType.MySQL, "INSERT INTO `TABLE_XXX` (`field1`) VALUES (?)", shardingRule);
        InsertStatement insertStatement = (InsertStatement) statementParser.parse();
        assertInsertStatementWithParameter(insertStatement);
    }
    
    private void assertInsertStatementWithParameter(final InsertStatement insertStatement) {
        assertThat(insertStatement.getTables().find("TABLE_XXX").get().getName(), is("TABLE_XXX"));
        Condition condition = insertStatement.getConditions().find(new Column("field1", "TABLE_XXX")).get();
        assertThat(condition.getOperator(), is(ShardingOperator.EQUAL));
        assertThat(((ListShardingValue<? extends Comparable>) condition.getShardingValue(Collections.<Object>singletonList(0))).getValues().iterator().next(), is((Comparable) 0));
    }
    
    private ShardingRule createShardingRuleWithGenerateKeyColumns() {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
        try {
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.getMetaData()).thenReturn(databaseMetaData);
            when(databaseMetaData.getDatabaseProductName()).thenReturn("H2");
        } catch (final SQLException ex) {
            throw new RuntimeException(ex);
        }
        Map<String, DataSource> dataSourceMap = new HashMap<>(1);
        dataSourceMap.put("ds", dataSource);
        DataSourceRule dataSourceRule = new DataSourceRule(dataSourceMap);
        TableRule tableRule = TableRule.builder("TABLE_XXX").actualTables(Arrays.asList("table_0", "table_1", "table_2")).dataSourceRule(dataSourceRule)
                .tableShardingStrategy(new ComplexShardingStrategy(Arrays.asList("field1", "field2", "field3", "field4", "field5", "field6", "field7"), new NoneShardingAlgorithm()))
                .generateKeyColumn("field1").generateKeyColumn("field2").build();
        return ShardingRule.builder().dataSourceRule(dataSourceRule).tableRules(Collections.singletonList(tableRule)).keyGenerator(IncrementKeyGenerator.class).build();
    }
    
    @Test
    public void parseWithSpecialSyntax() {
//        parseWithSpecialSyntax(DatabaseType.MySQL, "INSERT LOW_PRIORITY IGNORE INTO `TABLE_XXX` PARTITION (partition1,partition2) (`field1`) VALUE (1)");
        parseWithSpecialSyntax(DatabaseType.MySQL, "INSERT INTO TABLE_XXX SET field1=1");
        // TODO
//         parseWithSpecialSyntax(DatabaseType.MySQL, "INSERT INTO TABLE_XXX (field1) SELECT field1 FROM TABLE_XXX2 ON DUPLICATE KEY UPDATE field1=field1+1");
        parseWithSpecialSyntax(DatabaseType.MySQL, "INSERT /*+ index(field1) */ INTO TABLE_XXX (`field1`) VALUES (1) RETURNING field1*2 LOG ERRORS INTO TABLE_LOG");
    }
    
    @SuppressWarnings("unchecked")
    private void parseWithSpecialSyntax(final DatabaseType dbType, final String actualSQL) {
        ShardingRule shardingRule = createShardingRule();
        InsertStatement insertStatement = (InsertStatement) new SQLParsingEngine(dbType, actualSQL, shardingRule).parse();
        assertThat(insertStatement.getTables().find("TABLE_XXX").get().getName(), is("TABLE_XXX"));
        assertFalse(insertStatement.getTables().find("TABLE_XXX").get().getAlias().isPresent());
        Condition condition = insertStatement.getConditions().find(new Column("field1", "TABLE_XXX")).get();
        assertThat(condition.getOperator(), is(ShardingOperator.EQUAL));
        assertThat(((ListShardingValue<? extends Comparable>) condition.getShardingValue(Collections.emptyList())).getValues().iterator().next(), is((Comparable) 1));
    }
    
    @Test
    // TODO assert
    public void parseMultipleInsertForMySQL() {
        ShardingRule shardingRule = createShardingRule();
        new SQLParsingEngine(DatabaseType.MySQL, "INSERT INTO TABLE_XXX (`field1`, `field2`) VALUES (1, 'value_char'), (2, 'value_char')", shardingRule).parse();
    }
    
    @Test(expected = SQLParsingUnsupportedException.class)
    public void parseInsertAllForOracle() {
        ShardingRule shardingRule = createShardingRule();
        new SQLParsingEngine(DatabaseType.Oracle, "INSERT ALL INTO TABLE_XXX (field1) VALUES (field1) SELECT field1 FROM TABLE_XXX2", shardingRule).parse();
    }
    
    @Test(expected = SQLParsingUnsupportedException.class)
    public void parseInsertFirstForOracle() {
        ShardingRule shardingRule = createShardingRule();
        new SQLParsingEngine(DatabaseType.Oracle, "INSERT FIRST INTO TABLE_XXX (field1) VALUES (field1) SELECT field1 FROM TABLE_XXX2", shardingRule).parse();
    }
}
