@regresion
Feature: Flujo de estados de una tarea
  Regla de negocio:
    PENDIENTE   -> EN_PROGRESO | CANCELADA
    EN_PROGRESO -> PENDIENTE | COMPLETADA | CANCELADA
    COMPLETADA y CANCELADA son estados finales

  Background:
    * url baseUrl
    * def tarea = call read('classpath:tareas/comun/crear-tarea.feature') { titulo: 'Flujo', prioridad: 'CRITICA' }
    * def cambiarEstado = function(estado){ return karate.call('classpath:tareas/comun/cambiar-estado.feature', { id: tarea.id, estado: estado }) }

  Scenario: Camino feliz hasta completar
    * def r1 = cambiarEstado('EN_PROGRESO')
    * match r1.responseStatus == 200
    * def r2 = cambiarEstado('COMPLETADA')
    * match r2.responseStatus == 200
    * match r2.response contains { estado: 'COMPLETADA', urgencia: 0 }

  Scenario: No se puede completar una tarea que no se ha iniciado
    * def r = cambiarEstado('COMPLETADA')
    * match r.responseStatus == 409
    * match r.response.detail contains 'PENDIENTE a COMPLETADA'

  Scenario: Un estado final no admite más cambios
    * cambiarEstado('CANCELADA')
    * def r = cambiarEstado('EN_PROGRESO')
    * match r.responseStatus == 409

  Scenario: Filtrar por estado
    * cambiarEstado('CANCELADA')
    Given path 'api', 'v1', 'tareas'
    And param estado = 'CANCELADA'
    When method get
    Then status 200
    And match each response contains { estado: 'CANCELADA' }
    And match response[*].id contains tarea.id
