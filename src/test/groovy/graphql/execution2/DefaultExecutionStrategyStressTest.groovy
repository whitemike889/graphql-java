package graphql.execution2

import graphql.ExecutionInput
import graphql.GraphQL
import graphql.TestUtil
import graphql.execution.ExecutionId
import graphql.schema.DataFetcher
import groovy.time.TimeCategory
import org.apache.commons.lang3.RandomStringUtils
import spock.lang.Specification

class DefaultExecutionStrategyStressTest extends Specification {

    def count = 1000
    def stringCount = 1000


    Map generateFooData() {
        def fooData = [id: "root"]
        def curMap = fooData
        (1..count).each { it ->
            def tmp = ["id"    : it.toString(), "field1:": RandomStringUtils.random(stringCount), "field2": RandomStringUtils.random(stringCount),
                       "field3": RandomStringUtils.random(stringCount)]
            curMap.put("subFoo", tmp)
            curMap = tmp
        }
        fooData
    }

    def "deep query"() {
        def query = "{foo{"
        (1..count).each {
            query += "id field1 field2 field3 subFoo{"
        }
        query += "id}"
        (1..count).each {
            query += "}"
        }
        query += "}"
//        println(query)

        def dataFetchers = [
                Query: [foo: { env -> generateFooData() } as DataFetcher]
        ]
        def schema = TestUtil.schema("""
        type Query {
            foo: Foo
        }
        type Foo {
            id: ID
            field1: String
            field2: String
            field3: String
            subFoo: Foo
        }
        """, dataFetchers)


        def document = TestUtil.parseQuery(query)

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .build()

        Execution execution = new Execution()
        def graphql = GraphQL.newGraphQL(schema).build()


        when:
        (1..5).forEach {
            new Thread({
                println elapsedTime {
                    execution.execute(DefaultExecutionStrategy, document, schema, ExecutionId.generate(), executionInput).get()
                }
//                println elapsedTime {
//                    graphql.execute(query)
//                }
            }).start()
        }

        then:
        System.sleep(100000)

    }


    def elapsedTime(Closure closure) {
        def timeStart = new Date()
        closure()
        def timeStop = new Date()
        TimeCategory.minus(timeStop, timeStart)
    }
}

