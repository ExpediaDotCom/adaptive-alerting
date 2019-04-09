spring:
  main:
    web-environment: true
    show-banner: true
  datasource:
    driverClassName: "com.mysql.jdbc.Driver"
    url: "${db_endpoint}"
    username: $${DB_USERNAME}
    password: $${DB_PASSWORD}
    maximum-pool-size: 8
    minimum-idle: 0
    # TODO Add other data source settings here
  jpa:
    openInView: true
    database: MYSQL
    show-sql: false
    generate-ddl: false
    hibernate:
      naming_strategy: org.hibernate.cfg.ImprovedNamingStrategy
  data:
    rest:
      base-path: /api     
endpoints:
  health:
    sensitive: false
management:
  context-path: "/admin"
  metrics:
    export:
      jmx:
        domain: spring
    enable:
      jvm: false
      tomcat: false
      system: false
      process: false
graphite:
  urlTemplate: "${graphite_url}"
security:
  signingKey: $${SIGNING_KEY}
