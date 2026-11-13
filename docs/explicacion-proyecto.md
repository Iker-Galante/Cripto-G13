# Cómo funciona stegobmp

`stegobmp` esconde un archivo cualquiera dentro de una imagen BMP de 24 bits y lo vuelve a sacar.
La imagen resultante se ve igual que la original. Opcionalmente cifra el archivo antes de esconderlo.

Tiene tres operaciones:

| Operación | Qué hace |
| --- | --- |
| `-embed` | Esconde el archivo `-in` en la imagen `-p` y escribe la imagen resultante en `-out` |
| `-extract` | Recupera el archivo escondido en `-p` y lo escribe en `-out` + su extensión |
| `-analyze` | Examina una imagen sin saber cómo se escondió el archivo (agregado nuestro, no lo pide el enunciado) |

El código está en `cripto-project/` (Java 21, Maven, sin bibliotecas externas).

---

## 1. Mapa del código

Todo está bajo `cripto-project/src/main/java/ar/edu/itba/`. Cada carpeta resuelve un solo problema:

| Carpeta | Archivos | Responsabilidad |
| --- | --- | --- |
| (raíz) | `Main.java` | Une todas las partes y convierte los errores en mensajes |
| `cli/` | `ArgumentParser`, `Arguments` | Leer y validar la línea de comandos |
| `bmp/` | `BmpImage` | Separar un BMP en encabezado y bytes de píxeles |
| `payload/` | `PayloadCodec`, `Decoded` | Armar y desarmar el bloque `tamaño + datos + extensión` |
| `crypto/` | `PayloadCipher`, `CipherAlgorithm`, `CipherMode`, `CipherSpec` | Derivar clave e IV, cifrar y descifrar |
| `steg/` | `LsbSteganographer`, `LsbiSteganographer`, `StegMethod` | Meter y sacar bits de los bytes de píxeles |
| `analyze/` | `Analyzer` | El modo `-analyze` |

La idea central del diseño es que **cada capa ignora a las demás**:

- La capa `steg` solo sabe esconder *bytes* en *bytes*. No sabe qué es un BMP, ni un tamaño, ni un cifrado.
- La capa `payload` solo sabe armar el bloque. No sabe dónde se va a esconder.
- La capa `crypto` solo cifra un arreglo de bytes.

Por eso agregar un método o un algoritmo nuevo toca una sola carpeta.

---

## 2. Recorrido completo de una ejecución

### Ocultar (`-embed`)

```
 archivo.txt                                    portadora.bmp
     │                                               │
     ▼                                               ▼
 PayloadCodec.encode                           BmpImage.parse
 tamaño ‖ datos ‖ ".txt\0"                 encabezado │ píxeles
     │                                               │
     ▼  (solo si hay -pass)                          │
 PayloadCipher.encrypt                               │
 tamañoCifrado ‖ cifrado                             │
     │                                               │
     └──────────────► Steganographer.embed ◄─────────┘
                      (LSB1, LSB4 o LSBI)
                              │
                              ▼
                  encabezado + píxeles modificados
                              │
                              ▼
                          salida.bmp
```

### Extraer (`-extract`)

Es el camino inverso:

1. `BmpImage.parse` separa encabezado y píxeles.
2. `Steganographer.extract` lee **todos** los bits que el método podría haber usado. No sabe cuántos son
   del mensaje: devuelve el mensaje seguido de «basura» (los bits que tenía la imagen original).
3. Si hay `-pass`: se leen los 4 bytes del tamaño del cifrado, se toma esa cantidad de bytes y se descifra.
4. `PayloadCodec.decode` lee el tamaño, toma los datos y lee la extensión hasta el `\0`. Lo que sigue se descarta.
5. Se escribe `-out` + extensión. Por eso `-out` se pasa sin extensión.

---

## 3. Cada parte en detalle

### 3.1 Línea de comandos (`cli/`)

`ArgumentParser.parse` recorre los argumentos y devuelve un `Arguments` (un `record` inmutable con el comando,
los archivos, el método y el cifrado).

Reglas que aplica:

- Las opciones pueden venir en cualquier orden y distinguen mayúsculas (`-steg LSB1`, no `lsb1`).
- Exactamente uno de `-embed`, `-extract` o `-analyze`.
- Las opciones con valor (`-in`, `-p`, `-out`, `-steg`, `-a`, `-m`, `-pass`) no se pueden repetir ni quedar sin valor.
- `-embed` exige `-in`; los otros dos lo rechazan.
- `-analyze` solo exige `-p`; `-steg` y `-out` son opcionales.

Reglas del cifrado (las del enunciado):

| Se indica | Resultado |
| --- | --- |
| Sin `-pass` | No se cifra, aunque haya `-a` o `-m` |
| Solo `-pass` | `aes128` en `cbc` |
| `-pass` y `-a` | Ese algoritmo en `cbc` |
| `-pass` y `-m` | `aes128` en ese modo |

`-a` y `-m` se validan aunque no haya `-pass`, para que un error de tipeo no pase inadvertido.
Ante cualquier error se lanza `UsageException` y `Main` muestra el motivo y la sintaxis correcta.

### 3.2 Lectura del BMP (`bmp/BmpImage.java`)

Un BMP de 24 bits tiene dos partes: un encabezado y los píxeles.

```
┌───────────────────────┬──────────────────────────────────────────┐
│ encabezado (54+ bytes)│ píxeles: 3 bytes por píxel (azul, verde,  │
│                       │ rojo), filas de abajo hacia arriba       │
└───────────────────────┴──────────────────────────────────────────┘
0                       ▲
                        └─ posición indicada en el byte 0x0A del encabezado
```

`BmpImage.parse` valida que:

- empiece con `BM` y tenga al menos 54 bytes;
- sea de 24 bits por píxel (byte `0x1C`);
- no esté comprimido (byte `0x1E`).

Después corta el archivo en la posición que indica el propio encabezado (byte `0x0A`). No asume 54: hay
programas que escriben encabezados más largos, y `anillo_g13.bmp` tiene los píxeles en la posición 138.

Dos detalles:

- El encabezado se copia sin cambios a la salida. Solo se modifican los píxeles.
- `trailingBytes()` devuelve lo que haya después de las filas que el encabezado declara (ancho × alto).
  Una imagen normal no tiene nada. Lo usa `-analyze`, y así apareció el texto de `paris.bmp`.

### 3.3 El bloque que se oculta (`payload/PayloadCodec.java`)

Sin cifrado, lo que se esconde es:

```
┌──────────────┬─────────────────┬──────────────────┐
│ tamaño       │ datos del       │ extensión        │
│ 4 bytes      │ archivo         │ ".txt" + byte 0  │
│ (big endian) │                 │                  │
└──────────────┴─────────────────┴──────────────────┘
```

El tamaño va primero porque el extractor necesita saber cuántos bytes leer. La extensión termina en `\0`
porque su largo es variable.

Con cifrado, ese bloque entero se cifra y se le antepone el tamaño del cifrado:

```
┌──────────────────┬──────────────────────────────────────────────┐
│ tamaño cifrado   │ cifrado( tamaño ‖ datos ‖ extensión\0 )      │
│ 4 bytes          │                                              │
└──────────────────┴──────────────────────────────────────────────┘
```

Al decodificar se valida todo, porque la entrada puede ser basura (método equivocado, contraseña equivocada):

- el tamaño no puede ser negativo ni mayor que lo extraído;
- la extensión debe empezar con `.`, terminar en `\0` y ser ASCII;
- la extensión no puede contener `/` ni `\`, porque pasa a formar parte del nombre del archivo de salida.

Si algo falla se lanza `PayloadException`, que `Main` traduce a «no se encontró información oculta».

### 3.4 Esteganografía (`steg/`)

Los tres métodos implementan la misma interfaz, `Steganographer`:

```java
int capacity(byte[] pixels);              // cuántos bytes entran
void embed(byte[] pixels, byte[] payload);// escribe los bits, modifica pixels
byte[] extract(byte[] pixels);            // lee todos los bits disponibles
```

`StegMethod` es el `enum` de los valores de `-steg` y crea el objeto que corresponde.

#### LSB1 y LSB4 (`LsbSteganographer`)

Son la misma clase con un parámetro: cuántos bits bajos se reemplazan en cada byte de píxel (1 o 4).
Los bits del mensaje se escriben empezando por el más significativo.

Ejemplo: esconder el byte `01000001` (la letra `A`).

```
LSB1: usa 8 bytes de píxel, 1 bit en cada uno

  píxel original   bit del mensaje   píxel resultante
  1010110[0]              0           1010110[0]
  0011010[1]              1           0011010[1]
  1110001[1]              0           1110001[0]   ← cambió
  ...                    ...          ...

LSB4: usa 2 bytes de píxel, 4 bits en cada uno

  1010[1100]            0100          1010[0100]
  0011[0101]            0001          0011[0001]
```

| | LSB1 | LSB4 |
| --- | --- | --- |
| Capacidad | 1 byte cada 8 bytes de píxel | 1 byte cada 2 bytes de píxel |
| Cambio máximo por byte de color | 1 | 15 |

Si el bloque no entra, `embed` falla e informa cuánto se necesita y cuál es la capacidad máxima.

#### LSBI (`LsbiSteganographer`)

Es LSB1 con dos diferencias, tomadas del paper de Majeed y Sulaiman:

1. **No usa el canal rojo.** Solo escribe en los bytes azul y verde. En el archivo el orden es azul, verde,
   rojo, así que se saltean los bytes cuya posición cumple `i % 3 == 2`.
2. **Inversión por patrón.** Después de aplicar LSB1, revisa si conviene invertir los bits que escribió.

La inversión funciona así:

1. Cada byte se clasifica según su **patrón**: los bits 2 y 1 (el segundo y tercero menos significativos).
   Hay cuatro patrones: `00`, `01`, `10`, `11`. LSB1 solo toca el bit 0, así que el patrón no cambia.
2. Para cada patrón se cuenta cuántos bytes cambiaron y cuántos quedaron igual.
3. Si cambiaron más de los que quedaron igual, se invierte el último bit de todos los bytes de ese patrón.
   Los que habían cambiado vuelven a su valor original.
4. Los cuatro primeros bytes de la imagen guardan, en su último bit, si se invirtió cada patrón
   (en orden `00`, `01`, `10`, `11`). El mensaje empieza recién en el byte 4.

Ejemplo con 8 bytes y el mensaje `11111111`:

| Byte original | Patrón | Después de LSB1 | | Después de invertir | |
| --- | --- | --- | --- | --- | --- |
| `00000100` | 10 | `00000101` | cambia | `00000100` | igual |
| `00001100` | 10 | `00001101` | cambia | `00001100` | igual |
| `00010100` | 10 | `00010101` | cambia | `00010100` | igual |
| `00000101` | 10 | `00000101` | igual | `00000100` | cambia |
| `00000011` | 01 | `00000011` | igual | `00000011` | igual |
| `00001011` | 01 | `00001011` | igual | `00001011` | igual |
| `00000000` | 00 | `00000001` | cambia | `00000001` | cambia |
| `00001001` | 00 | `00001001` | igual | `00001001` | igual |

- Patrón `10`: 3 cambiaron y 1 no → se invierte.
- Patrón `01`: ninguno cambió → no se invierte.
- Patrón `00`: 1 y 1, empate → no se invierte.

Con LSB1 quedaban 4 bytes modificados; con LSBI quedan 2. Los indicadores guardados son `0, 0, 1, 0`.

Para extraer: se leen los 4 indicadores, se vuelve a invertir el último bit de los bytes cuyos patrones
estén marcados, y se lee como LSB1. En el ejemplo, los cuatro bytes de patrón `10` se invierten de nuevo
y se recupera `11111111`.

El conteo de cambios solo considera los bytes que recibieron un bit del mensaje, no toda la imagen.

### 3.5 Cifrado (`crypto/`)

**Derivación de clave e IV.** No se usa la contraseña directamente como clave. Se aplica
PBKDF2-HMAC-SHA256 con 10.000 iteraciones y un salt de 8 bytes en cero, y se le pide una salida del largo
`clave + IV`. Los primeros bytes son la clave y los siguientes el IV:

| `-a` | Clave | IV |
| --- | --- | --- |
| `aes128` | 16 bytes | 16 bytes |
| `aes192` | 24 bytes | 16 bytes |
| `aes256` | 32 bytes | 16 bytes |
| `3des` | 24 bytes (tres claves de 8) | 8 bytes |

Esos parámetros los fija la cátedra. Con ellos, la contraseña `gloria` y `aes256` dan la clave
`90d92cde…ca752f3f`, la misma que aparece en `salida_g13.txt`.

**Modos.** Se usan las implementaciones de la biblioteca estándar de Java (JCA):

| `-m` | Transformación en Java | Relleno | Usa IV |
| --- | --- | --- | --- |
| `ecb` | `ECB/PKCS5Padding` | Sí, hasta completar el bloque | No |
| `cbc` | `CBC/PKCS5Padding` | Sí | Sí |
| `cfb` | `CFB8/NoPadding` (realimentación de 8 bits) | No | Sí |
| `ofb` | `OFB/NoPadding` (bloque completo) | No | Sí |

Consecuencia del relleno: con `ecb` y `cbc` el cifrado siempre mide un múltiplo del bloque (16 bytes en AES,
8 en 3DES). Con `cfb` y `ofb` mide lo mismo que el texto original. `-analyze` usa este dato.

**Contraseña incorrecta.** Se detecta de dos formas distintas:

- Con `ecb` o `cbc`, el relleno suele quedar inválido y Java lanza un error → «Could not decrypt».
- Con `cfb` u `ofb` no hay relleno, el descifrado «funciona» pero da basura. La detecta `PayloadCodec`
  porque el tamaño o la extensión no son válidos → «No hidden data found».

### 3.6 Analizador (`analyze/Analyzer.java`)

Automatiza lo que haríamos a mano frente a una imagen desconocida. Para cada método (los tres, o el de `-steg`):

1. Extrae y trata de decodificar un archivo sin cifrar. Si lo logra, informa tamaño y extensión.
2. Si no, mira si los 4 primeros bytes forman un tamaño válido. Un tamaño válido sin extensión después es
   la forma que tiene un contenido cifrado (`tamaño ‖ cifrado`). En ese caso da una pista:

   | Lo que observa | Lo que informa |
   | --- | --- |
   | Bloques de 16 bytes repetidos | Modo `ecb` |
   | Bloques de 8 bytes repetidos (y no de 16) | `3des` en `ecb` |
   | Tamaño múltiplo de 16, sin repeticiones | Cualquier algoritmo y modo |
   | Tamaño múltiplo de 8 pero no de 16 | `3des`, o `aes` con `cfb` u `ofb` |
   | Otro tamaño | Solo `cfb` u `ofb` |

3. Si se pasó `-pass`, descifra antes de buscar el archivo.

Al final informa si hay bytes después de los píxeles, y los muestra si son texto.

**Por qué los bloques repetidos delatan ECB:** ECB cifra cada bloque por separado, así que dos bloques de
texto iguales dan dos bloques cifrados iguales. En los demás modos cada bloque depende del anterior o del IV,
y una repetición es prácticamente imposible. Lo contrario no vale: que no haya repeticiones no descarta ECB,
porque un archivo comprimido no tiene bloques iguales.

Con `-out <prefijo>` guarda lo que encuentra: `<prefijo>_<método><extensión>`, `<prefijo>_<método>.enc`
o `<prefijo>_trailing.bin`.

### 3.7 `Main.java`

`main` llama a `run`, que hace tres cosas:

1. Interpreta los argumentos.
2. Ejecuta `embed`, `extract` o `analyze`.
3. Atrapa cualquier excepción y la convierte en **una línea** de error, nunca en una traza de Java:

| Excepción | Mensaje |
| --- | --- |
| `UsageException` | El motivo y la sintaxis correcta |
| `NoSuchFileException` | «File not found» y el archivo |
| `BmpException` | Por qué la imagen no sirve |
| `IllegalArgumentException` | Por ejemplo, el archivo no entra en la portadora |
| `CryptoException` | No se pudo descifrar |
| `PayloadException` | No se encontró información oculta |

El programa termina con código 0 si todo salió bien y 1 si hubo un error.

---

## 4. Tests

Hay 155 tests (JUnit 5) en `cripto-project/src/test/`, organizados igual que el código:

| Clase | Tests | Qué verifica |
| --- | --- | --- |
| `ArgumentParserTest` | 30 | Cada regla de la línea de comandos y sus errores |
| `MainTest` | 27 | El programa de punta a punta: ocultar, extraer, analizar, mensajes de error |
| `PayloadCodecTest` | 25 | El formato del bloque y el rechazo de entradas inválidas |
| `PayloadCipherTest` | 18 | Claves e IV contra OpenSSL y contra los valores de la cátedra |
| `BmpImageTest` | 15 | Lectura del encabezado, imágenes inválidas, bytes sobrantes |
| `LsbSteganographerTest` | 15 | LSB1 y LSB4 bit a bit |
| `LsbiSteganographerTest` | 9 | El ejemplo del paper, el empate, el salteo del rojo |
| `CipherOptionsTest` | 9 | Los valores de `-a` y `-m` |
| `CatedraSamplesTest` | 7 | Las imágenes de ejemplo de la cátedra |

`CatedraSamplesTest` es la prueba más fuerte: extrae el archivo de cada imagen de ejemplo y, al volver a
ocultarlo, obtiene una imagen **idéntica byte a byte** a la de la cátedra. Solo corre si se define
`STEGOBMP_SAMPLES` con la carpeta de las imágenes; si no, se saltea.

Las imágenes de prueba se generan en memoria (`TestBmps`), así el repositorio no necesita archivos binarios.

---

## 5. Compilar y usar

```
cd cripto-project
mvn -q package                 # genera target/stegobmp.jar
```

```
./stegobmp -embed -in secreto.txt -p foto.bmp -out salida.bmp -steg LSBI -a aes256 -m cbc -pass clave
./stegobmp -extract -p salida.bmp -out recuperado -steg LSBI -a aes256 -m cbc -pass clave
./stegobmp -analyze -p sospechosa.bmp -out hallado
```

`stegobmp` es un script de una línea que ejecuta el `.jar`. En Windows se puede usar directamente
`java -jar target/stegobmp.jar ...`.

---

## 6. Los archivos del grupo 13

Las cuatro imágenes forman una cadena de pistas. Todo lo recuperado está en `TP/grupo13/hallazgos/`.

```
avatar.bmp ──LSBI──► PDF: «al .png cambiarle la extensión por .zip y descomprimir»
                                        │
montevideo.bmp ──LSB1──► PNG (buscaminas) ◄┘
                          │        └─ con un ZIP pegado al final ──► sol13.txt
                          │                                            │
                          └─ banderas = 1, cada fila una letra ◄───────┘
                             «AesECB» → aes256 en modo ECB
                                        │
paris.bmp ──bytes al final──► «la password es gloria»
                                        │
anillo_g13.bmp ──LSB4 + aes256 ECB + gloria──► wanted.wmv
```

| Imagen | Cómo estaba oculto | Qué había |
| --- | --- | --- |
| `avatar.bmp` | LSBI | Un PDF con la instrucción para abrir el ZIP |
| `montevideo.bmp` | LSB1 | Un PNG con un buscaminas y un ZIP agregado al final |
| `paris.bmp` | Texto agregado después de los píxeles | La contraseña |
| `anillo_g13.bmp` | LSB4, cifrado con aes256 en ECB | El video `wanted.wmv` |

El comando que saca el video:

```
./stegobmp -extract -p anillo_g13.bmp -out wanted -steg LSB4 -a aes256 -m ecb -pass gloria
```

---

## 7. Decisiones de diseño que conviene poder explicar

- **El orden de los bits.** Se escribe primero el bit más significativo, y en LSB4 primero el nibble alto.
  No lo dice el enunciado: se dedujo de las imágenes de ejemplo y se confirmó regenerándolas idénticas.
- **`extract` devuelve todo.** El método esteganográfico no conoce el tamaño del mensaje. Leer el tamaño es
  tarea de `PayloadCodec`. Así los tres métodos comparten el mismo formato sin repetir código.
- **LSBI saltea el rojo por posición en el arreglo** (`i % 3 == 2`), no por píxel. Coincide con los ejemplos.
- **Los indicadores de LSBI van en los primeros 4 bytes**, antes del mensaje, como pide el enunciado.
- **En LSBI un empate no invierte.** Invertir no reduciría los cambios.
- **La posición de los píxeles se lee del encabezado** en vez de asumir 54.
- **`cfb` es de 8 bits y `ofb` de bloque completo.** Se determinó con las imágenes de ejemplo.
- **Sin dependencias externas.** Todo el cifrado sale de la biblioteca estándar de Java, así compila en
  cualquier máquina con Java 21 y Maven.
