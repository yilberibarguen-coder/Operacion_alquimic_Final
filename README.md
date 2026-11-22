# Operacion Alquimia - Protocolo Savitar

API REST con **Java 17 + Spring Boot** que localiza el laboratorio de Alchemy por **trilateracion** y reconstruye el
mensaje interceptado a partir de los fragmentos de tres dispositivos de vigilancia de Central City.
Esta construida con **DDD y arquitectura hexagonal** (puertos y adaptadores). Los datos viven solo en memoria:
no usa base de datos, ni Docker, ni servicios externos.

## Requisitos
- **JDK compatible, como 17 o 21** (comprueba con `java -version`). La matriz de GitHub Actions verifica esas dos versiones; Java 25 tambien se ha probado localmente en Windows.
- Nada mas. Maven viene incluido en el proyecto (`mvnw`), y se descarga solo la primera vez (necesita internet).

## Ejecutar

| Sistema | Arrancar la API | Correr las pruebas |
|---|---|---|
| Windows (PowerShell) | `.\mvnw.cmd spring-boot:run` | `.\mvnw.cmd test` |
| Mac / Linux | `./mvnw spring-boot:run` | `./mvnw test` |

La API queda en `http://localhost:8080`. Para cambiar el puerto: `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=9090"`.

> En Mac/Linux, si dice "permission denied": `chmod +x mvnw`.

## Dispositivos (posiciones fijas)
| Identificador | Dispositivo | Posicion (X, Y) |
|---|---|---|
| `STAR_LABS` | STAR Labs | (-500, -200) |
| `CCPD` | CCPD | (100, -100) |
| `STAR_SATELLITE` | S.T.A.R. Satellite | (500, 100) |

El identificador se acepta sin importar mayusculas, guiones o espacios: `star-labs`, `STAR Labs` y `S.T.A.R. Satellite` tambien sirven.

## Endpoints
| Req. | Metodo y ruta | Que hace | Respuestas |
|---|---|---|---|
| F1 | `POST /api/v1/analysis` | Procesa la informacion completa de los 3 dispositivos (no guarda nada) | 200, 400, 422 |
| F2 | `POST /api/v1/devices` | Registra un dispositivo (cualquier orden y momento) | 201, 400, 409 |
| F3 | `GET /api/v1/devices` | Lo registrado, lo que falta y si ya hay datos suficientes | 200 |
| F3 | `GET /api/v1/devices/{id}` | Lo registrado de un dispositivo | 200, 400, 404 |
| F3 | `GET /api/v1/analysis` | Procesa lo acumulado: ubicacion + mensaje | 200, 422 |
| F4 | `PUT /api/v1/devices/{id}` | Reemplazo completo (distancia y fragmento obligatorios) | 200, 400, 404 |
| F5 | `PATCH /api/v1/devices/{id}` | Actualizacion parcial (distancia y/o fragmento) | 200, 400, 404 |
| F6 | `DELETE /api/v1/devices/{id}` | Elimina un dispositivo | 204, 400, 404 |

Codigos: **400** dato invalido o dispositivo desconocido, **404** el dispositivo no tiene datos registrados,
**409** el dispositivo ya esta registrado (usa PUT o PATCH), **422** no hay datos suficientes o los fragmentos se contradicen.
Todos los errores responden `{ "timestamp", "status", "error", "message" }`.

### Ejemplo (el laboratorio esta en (100, 100))
```json
POST /api/v1/analysis
{ "devices": [
  { "deviceId": "STAR_LABS",      "distance": 670.8203932499369, "message": ["este", "", "", "mensaje", ""] },
  { "deviceId": "CCPD",           "distance": 200,               "message": ["", "es", "un", "", ""] },
  { "deviceId": "STAR_SATELLITE", "distance": 400,               "message": ["este", "", "", "", "secreto"] } ] }
```
Respuesta `200`:
```json
{ "position": { "x": 100.0, "y": 100.0 }, "message": "este es un mensaje secreto" }
```
Registro individual (F2), actualizacion parcial (F5) y reemplazo (F4):
```json
POST  /api/v1/devices          { "deviceId": "CCPD", "distance": 200, "message": ["", "es", "un", "", ""] }
PATCH /api/v1/devices/CCPD     { "distance": 201.5 }
PUT   /api/v1/devices/CCPD     { "distance": 200, "message": ["", "fue", "un", "", ""] }
```

### Probarlo rapido en Windows
Con la API corriendo, en otra terminal: `powershell -ExecutionPolicy Bypass -File .\demo.ps1`
(recorre F1 a F6 paso a paso mostrando cada respuesta).

Con `curl` (Mac/Linux, o `curl.exe` en Windows con el JSON en un archivo):
```bash
curl -X POST localhost:8080/api/v1/devices -H "Content-Type: application/json" \
     -d '{"deviceId":"CCPD","distance":200,"message":["","es","un","",""]}'
curl localhost:8080/api/v1/devices
curl localhost:8080/api/v1/analysis
```

## Reglas principales
- Un fragmento es una lista de palabras; `""` significa "palabra no recibida" y se ignora. Para cada posicion se usa la palabra que haya recibido cualquier dispositivo.
- Todos los fragmentos deben tener la misma cantidad de posiciones, y dos dispositivos no pueden dar palabras distintas en una misma posicion (error 422).
- Hay **datos suficientes** cuando estan los 3 dispositivos y no falta ninguna palabra del mensaje. Si no, se responde 422 "no hay datos suficientes".
- La distancia debe ser un numero finito y no negativo. La ubicacion se redondea a 2 decimales.
- El resultado no depende del orden en que lleguen los datos. Cada operacion (registrar, reemplazar, actualizar, eliminar) es atomica y solo afecta al dispositivo indicado.

## Arquitectura
```
src/main/java/application
├── domain                      <- el negocio; no conoce HTTP ni Spring Web
│   ├── models                  DeviceReading, AlchemyReport, AccumulatedInformation
│   ├── valueobjects            SurveillanceDevice, Position, Distance, MessageFragment, ReadingUpdate
│   ├── services                LabLocator (trilateracion), MessageReconstructor, AlchemyAnalyzer
│   │                           + un servicio por caso de uso (F1..F6)
│   ├── exceptions              errores de negocio
│   └── ports
│       ├── in                  AlchemyPort            (lo que ofrece la aplicacion)
│       └── out                 ReadingRepositoryPort  (lo que necesita: guardar temporalmente)
└── adapters                    <- la tecnologia
    ├── useCases                AlchemyUseCaseImpl (implementa AlchemyPort)
    ├── rest                    controlador, DTOs, mapper y traduccion de errores a HTTP
    └── persistence/memory      almacenamiento en memoria (ConcurrentHashMap)
```
La justificacion completa de las decisiones esta en [`docs/DISENO.md`](docs/DISENO.md).

## Pruebas
`.\mvnw.cmd test` ejecuta pruebas unitarias del dominio, un test por cada requisito F1 a F6 (incluida la consistencia),
pruebas del adaptador HTTP y una prueba de arranque del contexto de Spring.
La suite contiene 80 casos. Incluye registros simultaneos, actualizaciones parciales concurrentes sin perdida de campos,
eliminacion frente a actualizacion, entradas HTTP invalidas y conservacion de los datos cuando una modificacion es rechazada.

## Subirlo a GitHub
Repositorio: https://github.com/yilberibarguen-coder/Operacion_alquimic_Final

En VS Code abre la carpeta `Operacion_alquimic_Final` que contiene `pom.xml` y su propio directorio `.git`.
Para publicar cambios desde el repositorio ya clonado y conectado a GitHub:
```bash
git status
git add .
git commit -m "Actualizar Operacion Alquimia"
git push origin main
```
Git debe tener configurados `user.name` y `user.email`, ademas de una cuenta autenticada con permiso de escritura.
Comprueba en GitHub que se ven `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn`, `.github` y `src`.
El workflow Compatibility verifica la version publicada; sus resultados estan en la pestana Actions.

## Problemas frecuentes
| Sintoma | Solucion |
|---|---|
| `java no se reconoce` | Instala un JDK 17 o superior y reabre la terminal. |
| `.\mvnw.cmd no se reconoce` | Estas en la carpeta equivocada: entra a la que contiene `pom.xml`. |
| `Port 8080 was already in use` | Cierra lo que use ese puerto o usa otro (ver "Ejecutar"). |
| Falla la descarga de Maven | Revisa tu conexion a internet; la primera vez descarga Maven y las librerias. |

## Validaciones adicionales
Las distancias geometricamente incompatibles producen 422. Se admite una tolerancia de 0.1 unidades mas 1e-9 relativo, comprobada antes de redondear la posicion. Los elementos nulos en `devices` producen 400. Los servicios del dominio son Java puro y se configuran desde `configuration/DomainConfiguration`. Las pruebas incluyen peticiones HTTP reales y compilacion aislada del dominio.

## Compatibilidad entre sistemas e idiomas
Los identificadores se normalizan con `Locale.ROOT`, sin depender del idioma del sistema. `DeviceLocaleTest` comprueba los alias de los tres dispositivos con configuraciones turca, azeri, inglesa y colombiana, y restaura la configuracion original al terminar.

El workflow `.github/workflows/compatibility.yml` se ejecuta en GitHub Actions con cada push, pull request o ejecucion manual. Tiene seis combinaciones: Windows, Linux y macOS con Temurin JDK 17 y 21. Cada combinacion ejecuta `verify` mediante el wrapper correspondiente, incluyendo pruebas HTTP y del dominio. Despues inicia el JAR empaquetado primero en ingles y despues en turco, en una carpeta temporal con espacios, y comprueba posicion y mensaje por HTTP. El proceso se detiene al finalizar.

Para activar estas verificaciones, sube el contenido de esta carpeta (incluyendo `.github`, `.mvn` y `scripts`) a la raiz de un repositorio de GitHub con Actions habilitado. Consulta la pestaña Actions, workflow Compatibility: la compatibilidad de las seis combinaciones queda comprobada cuando todos sus trabajos terminan en verde. Agregar el workflow por si solo no significa que esas ejecuciones ya hayan pasado.

Para repetir la comprobacion del JAR localmente, despues de `./mvnw verify` (Linux/macOS) o `.\mvnw.cmd verify` (Windows), ejecuta:
```text
java scripts/VerifyJar.java target/alquimia-savitar-1.0.0.jar
```
Se necesita un JDK compatible y acceso inicial a internet para descargar Maven y las dependencias. En Linux/macOS puede ser necesario `chmod +x mvnw`. El JAR empaquetado incluye las dependencias y puede ejecutarse sin Maven. Los datos se pierden al detener la API, porque el almacenamiento es temporal en memoria.
