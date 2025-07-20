package com.processm.processminterpreter

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.CassandraContainer
import org.testcontainers.containers.Neo4jContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    fun cassandraContainer(): CassandraContainer<*> {
        return CassandraContainer(DockerImageName.parse("cassandra:latest"))
    }

    @Bean
    @ServiceConnection
    fun neo4jContainer(): Neo4jContainer<*> {
        return Neo4jContainer(DockerImageName.parse("neo4j:latest"))
    }

}
