function fn() {
  // GESTIÓN DE LA CONFIGURACIÓN: la misma suite corre en cualquier entorno;
  // solo cambia la URL base, que llega por -DbaseUrl o por karate.env.
  var env = karate.env || 'local';
  var urls = {
    local: 'http://localhost:8080',
    ci: 'http://localhost:8080',
    staging: 'https://staging.example.com'
  };

  var config = {
    env: env,
    baseUrl: karate.properties['baseUrl'] || urls[env],
    expectedVersion: karate.properties['expectedVersion'] || ''
  };

  karate.log('karate.env =', env, '| baseUrl =', config.baseUrl);
  karate.configure('connectTimeout', 5000);
  karate.configure('readTimeout', 10000);
  return config;
}
