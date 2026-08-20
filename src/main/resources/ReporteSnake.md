# Parte II

## 1) Análisis de concurrencia

### Autonomía
La forma en la que se utilizan los hilos para dar **autonomía** a cada uno de las serpientes es la siguiente:

SnakeApp se encarga de generar un hilo virtual por cada serpiente que exista (usando ```newVirtualThreadPerTaskExecutor()```).
Con cada uno de esos hilos, se ejecuta un ```SnakeRunner```, que se encarga de hacer en un bucle 3 cosas principales:

- Decide si gira de manera aleatoria
- Pide moverse un paso al tablero
- Se duerm un rato (lo hace con ```Thread.sleep```)

Todo esto independiente de los demás hilos.

Por eso es que cada serpiente tiene autonomía, porque no hay un hilo central que las mueve a todas, sino que por el contrario, cada una se encarga de decidir y avanzar por su cuenta a su propio ritmo (según si se choca con un turbo o no)

Además de estos hilos, hay 2 cosas más importantes para este problema:

- **Hilo de interfáz gráfica:** Se encarga de repintar el tablero 60 veces por segundo y anda pendiente del teclado para mover las serpientes asociadas a este.

- **Hilo de reloj del juego:** Que se encarga de que en cada cierto intercalo se dispare el repintado

Con estos hilos adicionales, hay un problema y es que, cuando se observa el runner de cada uno de estos, se observa que todos tocan los mismos objetos compartidos (como lo son el tablero y cada serpiente), y no todos respetan los accesos a estos elementos

---

### Condiciones de Carrera

En el código identificamos que hay varios puntos donde se observan las condiciones de carrera, esas son:

- **Cuerpo de la serpiente:** cuando una serpiente acanza, su cuerpo (un array con varias posiciones) se modifica desde el hilo en su própio ```SnakeRunner``` . Sin embargo, cuando la interfáz repinta el juego, el hilo de la interfáz gráfica le pida a cada serpiente un ```snapshot()``` del cuerpo que tiene para pintarla.

    El problema aquí, es que esa capura ocurre sin nada de coordinación sobre la serpiente como tal.

    Como no se sincroniza, el resultado observado al final depende de en que momento se llegarona cruzar estas 2 operaciones, no es algo que pase siempre, es algo que pasa de forma intermintente y por eso lo hace más complicado de verlo, porque puede no fallar en unas partidas pero en otras sí.

- **Dirección de la serpiente:** según el código ```direction``` es marcado como ```volatile``` (es decir, que cualquier hilo que la lea verá siempre el valor más reciente escrito por otro hilo [[referencia]](https://www.datacamp.com/es/doc/java/volatile)), pero solo garantiza visibilidad del valor más reciente, no atomicidad en el acto como tal, ahí es donde entra el problema, porque las ordenes pueden ser recibidas desde 2 lados al mismo tiempo.

    Un lado, si es controlada por teclado, el hilo de la interfaz gráfica manda giros cuando se presiona las correspondientes flechas.

    Así mismo, el propio ```SnakeRunner``` de la serpiente le puede mandar giros aleatorios al chocar contra algo (cosa que aplica para *todas* las serpientes)

    Y como ```direction``` no posee una variable local en cada turn, al hacer las comparaciones se pueden leer valores distintos si otro hilo lo sobreeescribe en medio del proceso (osea, puede permitir el giro de 180° cuando no debería)

---

### Colecciones no seguras

Los usos inseguros para este desarrollo identificados fueron:

- Snake.body (ArrayDeque<Position>): No es thread-safe y no se encuentra protegida

- Board.mice, Board.obstacles, Board.turbo (HashSet<Position>) y Board.teleports (HashMap): tampoco son thread-safe solas, aunque se encuentran protegidas gracias a los métodos que las tocan (```step(), mice(), obstables()```) son synchronized y devuelven copias

---

### Busy-Wait

Como tal, no se presenta una condicion de bloqueo entre dormir/bloquear, ya que el ```SnakeRunner``` usa el ```Thread-sleep(sleep)``` siempre en las interacciones, así mismo, el ```GameClock``` usa su propio executor, no un loop.

Lo unico que puede llegar a presentarse como algo a revisar, es que el ```GameClock``` sigue avanzando y disparando el *tick* incluso cuando se encuentra en estado 'PAUSED', pero no hay cambios en el Runnable porque tiene una condición para no hacer nada. Cosa que como tal no requiere un arreglo con wait/notify


---
---
## 2) Corrección Mínima

La mayor parte del problema que observamos, se centra en la própia [Snake](../java/co/eci/snake/core/Snake.java), la mejor solución a realizar es que se sincronice sobre el propio monitor de cada instancia de Snaque y no sobre ```Board```, con ello, el acceso solo seria entre el ```SnakeRunner``` de esa serpiente y del hilo de la interfáz gráfica, sin necesidad de afectar las demás serpientes ni demasa regiones del tablero.

Para ello tenemos:

*Sincronización de la serpiente* agregar ```synchronized``` a los metodos que toquen estados que se compartan (es decir, el cuerpo y la direccion)

Se hace en ésta región en específico porque, si se pone el bloque en otro lado (ejemplo, el tablero para sincronizar el hilo de la interfaz), se bloquearía el acceso para todas las serpientes, pero en realidad, el conflicto es individual para cada una.

Con eso, solo se afecta a los hilos que realmente afecten a la serpiente en específico (el SnakeRunney y la de la interfaz) las demas serpientes no se ven afectadas.

**CAMBIOS**

### Synchronized en advance() y snapshot()
- *Riesgo:* el body es un arrayDeque, no es thread-safe, se comienza a escribir desde el runner y se lee desde el hilo de la interfáz sin coordinación, lo que genera intermitencia

- *Solución:* ambos métodos ahora timan el monitor en la instancia Snake, se garantiza que lo escrito y lo copiado nunca pasen al tiempo sobre el mismo objeto

### Synchronized en turn() y direction()
- *Riesgo:* la direccion se escribia desde 2 hilos, por orden de teclado o por el randomTurn(), por lo que tunr() hacia 4 comparaciones seguidas antes de decidirse, lo que generaba una carrera para el acto y donde el valor podía cambiar durante la comparación.

- *Solución:* turn ahora realiza una copia de direction a una variable local dentro de la sincronización antes de comparar, y con todo el método protegido que se encarga del *body*, así no se necesita el volatile porque ahora se tiene al tiempo visibilidad y atomicidad

### Synchronized head()

- *Riesgo:* el head lee ```body.peekFirst()``` cosa que esta protegido por advance() y snapshot(), dejarlo sin sincronización rompería la protección

- *Solución:* se sincroniza tambien bajo el mismo monitor de la instancia en snake 

Con esto se asegura que en definitiva no ocurra un deadlock, ya que el único lugar donde se toman 2 locks a la vez es dentro de ```step()```, pero este se encarga de tomar primero el lock de board al ya tener un *synchronized* y luego ahí si llama a los metodos de Snake que adquieren el lock de la instancia.

Asi que no hay preocupación, ya que se encuentra un orden definido

```Board --> Snake```

evitando totalmente al deadlock

***(Todos estos cambios ya están reflejados en [Snake.java](../java/co/eci/snake/core/Snake.java))***