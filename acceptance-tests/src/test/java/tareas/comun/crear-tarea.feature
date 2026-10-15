@ignore
Feature: Paso reutilizable - crear una tarea

  Scenario:
    Given url baseUrl
    And path 'api', 'v1', 'tareas'
    And request { titulo: '#(titulo)', prioridad: '#(prioridad)' }
    When method post
    Then status 201
    * def id = response.id
