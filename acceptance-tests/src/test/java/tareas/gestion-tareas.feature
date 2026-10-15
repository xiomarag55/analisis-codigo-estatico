@regresion
Feature: Gestión de tareas
  Como usuario de la API de tareas
  Quiero crear, consultar, modificar y eliminar tareas
  Para organizar mi trabajo

  Background:
    * url baseUrl
    * def tareaSchema =
      """
      {
        id: '#number',
        titulo: '#string',
        descripcion: '##string',
        prioridad: '#regex (BAJA|MEDIA|ALTA|CRITICA)',
        estado: '#regex (PENDIENTE|EN_PROGRESO|COMPLETADA|CANCELADA)',
        fechaLimite: '##string',
        creadaEn: '#string',
        vencida: '#boolean',
        urgencia: '#number? _ >= 0 && _ <= 100'
      }
      """

  Scenario: Crear una tarea y consultarla
    Given path 'api', 'v1', 'tareas'
    And request { titulo: 'Configurar quality gate', descripcion: 'Sonar', prioridad: 'ALTA' }
    When method post
    Then status 201
    And match response == tareaSchema
    And match response.estado == 'PENDIENTE'
    And match responseHeaders['Location'][0] == '/api/v1/tareas/' + response.id
    * def id = response.id

    Given path 'api', 'v1', 'tareas', id
    When method get
    Then status 200
    And match response.titulo == 'Configurar quality gate'

  Scenario: Actualizar una tarea
    * def tarea = call read('classpath:tareas/comun/crear-tarea.feature') { titulo: 'Borrador', prioridad: 'BAJA' }

    Given path 'api', 'v1', 'tareas', tarea.id
    And request { titulo: 'Definitiva', prioridad: 'MEDIA' }
    When method put
    Then status 200
    And match response contains { titulo: 'Definitiva', prioridad: 'MEDIA' }

  Scenario: Eliminar una tarea
    * def tarea = call read('classpath:tareas/comun/crear-tarea.feature') { titulo: 'Temporal', prioridad: 'BAJA' }

    Given path 'api', 'v1', 'tareas', tarea.id
    When method delete
    Then status 204

    Given path 'api', 'v1', 'tareas', tarea.id
    When method get
    Then status 404
    And match response.title == 'Tarea no encontrada'

  Scenario: Una tarea vencida tiene urgencia máxima
    Given path 'api', 'v1', 'tareas'
    And request { titulo: 'Atrasada', prioridad: 'BAJA', fechaLimite: '2020-01-01' }
    When method post
    Then status 201
    And match response contains { vencida: true, urgencia: 100 }

  Scenario Outline: Validaciones de entrada - <caso>
    Given path 'api', 'v1', 'tareas'
    And request <cuerpo>
    When method post
    Then status 400
    And match response.title == 'Datos inválidos'
    And match response.errores contains { '<campo>': '#string' }

    Examples:
      | caso                 | cuerpo                                | campo     |
      | título vacío         | { titulo: '', prioridad: 'ALTA' }     | titulo    |
      | sin prioridad        | { titulo: 'Sin prioridad' }           | prioridad |
