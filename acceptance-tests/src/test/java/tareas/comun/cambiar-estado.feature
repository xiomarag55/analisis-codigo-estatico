@ignore
Feature: Paso reutilizable - cambiar el estado de una tarea (no valida el status: lo hace quien llama)

  Scenario:
    Given url baseUrl
    And path 'api', 'v1', 'tareas', id, 'estado'
    And request { estado: '#(estado)' }
    When method patch
