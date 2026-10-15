@smoke
Feature: Salud y versión del servicio desplegado
  Como equipo de operaciones
  Quiero verificar que el artefacto desplegado está vivo y es la versión esperada
  Para detectar despliegues fallidos o de la versión equivocada (trazabilidad de la configuración)

  Background:
    * url baseUrl

  Scenario: El servicio responde UP
    Given path 'actuator', 'health'
    When method get
    Then status 200
    And match response.status == 'UP'

  Scenario: El servicio expone su versión y commit
    Given path 'actuator', 'info'
    When method get
    Then status 200
    And match response.build.artifact == 'tareas-api'
    And match response.build.version == '#string'
    # En el pipeline (-DexpectedVersion) se valida que lo desplegado sea exactamente la versión a liberar
    * def versionEsperada = expectedVersion ? expectedVersion : response.build.version
    And match response.build.version == versionEsperada
