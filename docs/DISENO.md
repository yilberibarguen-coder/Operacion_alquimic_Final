# Diseno de la solucion - Operacion Alquimia

## 1. Idea general
El sistema recibe lo que informa cada dispositivo de vigilancia (distancia al laboratorio y fragmento del mensaje),
lo conserva temporalmente en memoria y, cuando hay informacion suficiente, calcula **donde esta el laboratorio**
(trilateracion) y **que dice el mensaje** (fusion de fragmentos).

## 2. Separacion en capas y por que
| Capa | Que contiene | Responsabilidad | Depende de |
|---|---|---|---|
| **Dominio** (`domain`) | Entidad, objetos de valor, servicios de dominio, excepciones, puertos | Las reglas del negocio: que es una lectura valida, como se reemplaza o actualiza, como se calcula la posicion y se arma el mensaje | Nada de HTTP ni de infraestructura |
| **Aplicacion** (`domain/services` + `adapters/useCases`) | Un servicio por caso de uso (F1 a F6) y la implementacion del puerto de entrada | Orquestar: pedir al puerto de salida los datos, aplicar las reglas del dominio y devolver el resultado | Dominio |
| **Infraestructura** (`adapters/rest`, `adapters/persistence`) | Controlador REST, DTOs, mapper, manejo de errores, almacenamiento en memoria | Traducir HTTP a llamadas del dominio y guardar en memoria | Dominio |

La regla de dependencias apunta siempre hacia el dominio. Por eso el dominio **no conoce HTTP**: si manana la entrada fuera
una consola o una cola de mensajes, solo se escribiria otro adaptador. Y como el almacenamiento esta detras de un puerto
(`ReadingRepositoryPort`), se puede cambiar el `ConcurrentHashMap` por una base de datos sin tocar el negocio.

## 3. Modelo de dominio (DDD)
| Elemento | Tipo | Responsabilidad |
|---|---|---|
| `DeviceReading` | Entidad (identidad = dispositivo), inmutable | Lo que informa un dispositivo. `replacedBy` (F4) sustituye todo; `applyUpdate` (F5) cambia solo lo enviado |
| `SurveillanceDevice` | Objeto de valor (enum) | Los 3 dispositivos con su posicion fija; normaliza el identificador recibido |
| `Distance` | Objeto de valor | Numero finito y no negativo |
| `MessageFragment` | Objeto de valor | Lista de palabras; `""` = no recibida; sin nulos |
| `Position` | Objeto de valor | Punto (x, y); se redondea a 2 decimales |
| `ReadingUpdate` | Objeto de valor | Actualizacion parcial: distancia y/o fragmento (al menos uno) |
| `LabLocator` | Servicio de dominio | Trilateracion: restando las ecuaciones de los circulos queda un sistema lineal 2x2 |
| `MessageReconstructor` | Servicio de dominio | Fusiona fragmentos ignorando `""`; detecta contradicciones |
| `AlchemyAnalyzer` | Servicio de dominio | Combina las lecturas: comprueba que esten los 3 dispositivos, ubica y reconstruye |

**Por que `DeviceReading` es inmutable:** reemplazar o actualizar produce un objeto nuevo, asi nunca queda una lectura "a medias"
y la diferencia entre F4 (sustituir todo) y F5 (conservar lo no enviado) queda explicita en el codigo.

## 4. Puertos
- **Entrada** `AlchemyPort`: una operacion por requisito (F1 a F6).
- **Salida** `ReadingRepositoryPort`: `saveIfAbsent`, `replace`, `update`, `findById`, `findAll`, `deleteById`.
  Las operaciones son **atomicas** a proposito: el adaptador usa `putIfAbsent`, `computeIfPresent` y `remove` de `ConcurrentHashMap`,
  de modo que registrar, reemplazar, actualizar y eliminar no dejan datos inconsistentes ni siquiera con peticiones simultaneas.

## 5. Requisitos funcionales
| Req. | Servicio | Endpoint | Como se cumple |
|---|---|---|---|
| F1 | `ProcessCompleteInformationService` | `POST /analysis` | Exige exactamente 3 lecturas de dispositivos distintos; procesa sin guardar |
| F2 | `RegisterReadingService` | `POST /devices` | `saveIfAbsent`: cualquier orden; si ya existe, 409 |
| F3 | `ConsultReadingsService`, `ProcessAccumulatedService` | `GET /devices`, `GET /devices/{id}`, `GET /analysis` | Informa lo registrado, lo que falta y si es suficiente; si no, 422 |
| F4 | `ReplaceReadingService` | `PUT /devices/{id}` | Sustituye solo ese dispositivo; exige distancia y fragmento; 404 si no existia |
| F5 | `UpdateReadingService` | `PATCH /devices/{id}` | Cambia solo lo enviado; la lectura conserva el resto; 404 si no existia |
| F6 | `RemoveReadingService` | `DELETE /devices/{id}` | Elimina solo ese dispositivo; 404 si no existia |

## 6. Validaciones y errores
| Caso | Donde se valida | Respuesta |
|---|---|---|
| Campo obligatorio ausente, JSON mal formado | DTOs (Bean Validation) | 400 |
| Dispositivo desconocido | `SurveillanceDevice.from` | 400 |
| Distancia negativa, NaN o infinita | `Distance` | 400 |
| Fragmento vacio o con valores nulos | `MessageFragment` | 400 |
| PATCH sin ningun campo | `ReadingUpdate` | 400 |
| F1 sin exactamente 3 lecturas, o dispositivo repetido | Servicio / analizador | 400 |
| Dispositivo sin datos (consultar, PUT, PATCH, DELETE) | Servicios | 404 |
| Dispositivo ya registrado (POST) | `RegisterReadingService` | 409 |
| Faltan dispositivos o palabras del mensaje | `AlchemyAnalyzer` / `MessageReconstructor` | 422 |
| Fragmentos de distinta longitud o palabras contradictorias | `MessageReconstructor` | 422 |

El traductor de errores (`RestExceptionHandler`) es el unico lugar que conoce los codigos HTTP; el dominio solo lanza excepciones de negocio.

## 7. Decisiones y supuestos (el enunciado no los define)
1. **Formato del mensaje:** cada fragmento es una lista de palabras con `""` para las no recibidas, y todos tienen la misma longitud.
2. **Reconstruccion:** por cada posicion se toma la palabra recibida por cualquier dispositivo; si dos dispositivos dan palabras distintas, hay contradiccion (422).
3. **Datos suficientes:** los 3 dispositivos registrados y ninguna palabra del mensaje sin resolver.
4. **POST sobre un dispositivo ya registrado** es un conflicto (409): para cambiarlo estan PUT (reemplazo) y PATCH (parcial). Asi F2, F4 y F5 no se confunden.
5. **PUT y PATCH sobre un dispositivo sin datos** devuelven 404 (no crean); F2 es la unica forma de registrar.
6. **Distancia 0** es valida (el laboratorio estaria justo en el dispositivo); negativa, NaN o infinita no.
7. **Se valida la coherencia geometrica** antes de redondear la posicion: cada distancia debe coincidir con la calculada dentro de una tolerancia absoluta de 0.1 unidades mas 1e-9 de tolerancia relativa. Se tolera el redondeo habitual de distancias a dos decimales; las incompatibles producen 422. La consulta acumulada indica que no hay datos suficientes para un resultado valido.
8. **Posiciones de los dispositivos fijas** (las del enunciado), por eso el identificador es un enumerado.

## 8. Posibles preguntas
- **Por que el dominio no conoce HTTP?** Para poder cambiar la forma de entrada sin tocar el negocio y probar las reglas sin servidor.
- **Donde esta la diferencia entre reemplazo y actualizacion parcial?** En `DeviceReading.replacedBy` frente a `applyUpdate`, y en los DTOs: PUT exige todo, PATCH acepta campos opcionales.
- **Como garantizas la consistencia?** Lecturas inmutables y operaciones atomicas en el puerto de salida.
- **Que pasa si faltan palabras?** No hay datos suficientes (422); `GET /devices` explica el motivo.
- **Como cambiarias el almacenamiento?** Escribiendo otro adaptador que implemente `ReadingRepositoryPort`.

## 9. Independencia del dominio
Los servicios del dominio son Java puro, sin anotaciones ni dependencias de Spring. `configuration/DomainConfiguration` crea sus beans fuera del dominio. Una prueba compila todo el dominio sin dependencias externas para proteger esta regla.

## 10. Portabilidad
La normalizacion de identificadores usa `Locale.ROOT`. Las pruebas regionales cubren turco y azeri (donde la conversion de la letra i difiere), ademas de ingles y español de Colombia. Se ejecutan de forma aislada y restauran las categorias de idioma para no afectar otras pruebas.
La matriz de CI compila, prueba y empaqueta con JDK 17 y 21 sobre Windows, Linux y macOS. Una prueba adicional ejecuta el JAR real desde una carpeta con espacios, primero en ingles y despues en turco, con puerto dinamico, consulta la API y termina el proceso. Solo usa bibliotecas del JDK para evitar dependencias de Python, curl o PowerShell en esa comprobacion.
