# Parte I - Calentamiento Wait/Notify

## Explicando

Para realizar este punto, se realizarón modificaciones en el programa [PrimeFinder](../java/co/eci/primeFinder/PrimeFinderThread.java) tal y como se solicitaba. Esto con el fin de que la clase [Control](../java/co/eci/primeFinder/Control.java) al modificarse de igual forma, pueda tomar tambien el papel de **monitor compartido** y **reloj** para las pausas (ya que, inicialmente, ya se encargaba de la respectiva creación y arranque de los hilos.)

Las modificaciones hechas fueron para que cada ```TMILISECONDS``` el hilo control pueda:

- Pausar todos los **PrimeFinder** que sigan trabajando

- Imprima el total de números primos encontrados hasta el momento

- Espere un ENTER

- Reanude todos los hilos en pausa

Cada PrimeFinder se encarga de recibir una referencia de control, en la que con cada búsqueda, se encarga de buscar un ```checkPause()```, metodo el cuál se encarga de bloquear al hilo por medio de un ```wait()``` si el sistema se encuentra en pausa y si le corresponde. Apenas el hilo termina el rango, avisa al control por medio del ```threadFinished()```


---

## Sincronización

Las coordinaciones entre hilos se realizaron por medio de ```synchronized()```, ```wait()```, ```notyfyAll()``` sobre un monitor en comun [que en nuestro caso, era **Control**], cuidando de que los hilos no tengan que preguntar en bucle si la condicion para continuar ya cambió

---

## Para diseñarlo

- **Monitor usado:** la instancia propia de conrol (referida en el código como ```this```), esto es para que los métodos que se encargan de leer y modificar el estado que todos los hilos comparten (como ```paused```,```pausedCount```,```activeThreads```) hagan el *synchronized* sobre esa misma instancia.

Esto resulta ser muy importante para una buena coordinación, ya que si los *PrimeFinderThreads* se sincronizaran sobre objetos distintos, las señales que mandan wait() y notify() puede que no se crucen entre sí.

- **Condiciones de espera:** Es la combinación de las 3 variables nombradas anteriormente de forma protegida: 

    - paused --> para saber si debe o no estar detenido
    - pausedCount --> para saber cuantos hilos estan bloqueados de manera efectiva al momento
    - activeThreads --> para saber cuantos hilos aún no han terminado su rango de revisión

de esta manera, no hay fallo, ya que los hilos esperan mientras ```paused == true``` y el controlador espera mientras ```pausedCount < activeTheads```

- **While en vez de if en el wait():**  Para evitar que por accidente Java despierte a un hilo (al parecer, algo llamado *wakeup espurio*) al tener el while, se evita que este caso pueda ocurrir

    De igual forma, evita que se pierdan los avisos, como todo pasa por *synchronized*, evita que se pierdan señales entre hilos

- **notifyAll():** se usa este en vez de ```notify()``` ya que se espera 2 cosas, que los hilos terminen de contar y que el controlador sepa que todos los hilos se detengan.

    Si solo se usara notify(), puede que despierte al hilo incorrecto y que se generen bucles. En cambio, el ```notifyAll()``` despierta a todos y cada uno revisa sus propias condiciones.

- **Problemas:** El controlador esperaba que los 3 hilos se pausaran. Pero no todos se tardan lo mismo en terminar su parte, lo que hacia que terminaran en tiempos distintos.

    Con esto pasando, el hilo no se pausaba nunca más después de la 2da interacción y el controlador lo esperaba por mucho tiempo (por nada, ya que el hilo nunca respondería).

    La solución para ello, se creó una cuenta de cuantos hilos en verdad aún estaban activos y los esperaba a ellos, no a los ```NTHREADS``` fijos que se tenía antes.