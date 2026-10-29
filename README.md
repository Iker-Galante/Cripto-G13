# Cripto-G13 — stegobmp

Trabajo práctico de implementación de Criptografía y Seguridad (72.04): **esteganografía en archivos BMP**.

`stegobmp` oculta un archivo cualquiera dentro de un BMP de 24 bits sin compresión (LSB1, LSB4 o LSBI),
opcionalmente encriptado (AES-128/192/256 o 3DES en modo ECB, CBC, CFB u OFB), y lo recupera.
También incluye un modo `-analyze` para el estegoanálisis.

El código está en [`cripto-project/`](cripto-project) (Java 21 + Maven, sin dependencias externas).

## Requisitos

- JDK 21 (`java` y `javac` 21.x).
- Maven 3.6.3 o superior (opcional: ver la compilación sin Maven).

## Cómo obtener el ejecutable

### Con Maven

```bash
cd cripto-project
mvn clean package
```

Genera `cripto-project/target/stegobmp.jar` y corre los tests. Para compilar sin correr los tests:
`mvn clean package -DskipTests`.

### Sin Maven (sólo con el JDK)

```bash
cd cripto-project
mkdir -p target/classes
javac --release 21 -d target/classes $(find src/main/java -name '*.java')
jar --create --file target/stegobmp.jar --main-class ar.edu.itba.StegoBmp -C target/classes .
```

## Ejecución

Desde `cripto-project/`, con el script `stegobmp` (ejecuta `java -jar target/stegobmp.jar`):

```bash
./stegobmp -embed -in mensaje1.txt -p imagen1.bmp -out imagenmas1.bmp -steg LSBI -a 3des -m cbc -pass oculto
```

Si el script perdió el permiso de ejecución, `chmod +x stegobmp`. También se puede usar el jar directamente:
`java -jar target/stegobmp.jar <parámetros>`.

### Ocultar

```
stegobmp -embed -in <archivo> -p <bitmap portador> -out <bitmap salida> -steg <LSB1 | LSB4 | LSBI>
         [-a <aes128 | aes192 | aes256 | 3des>] [-m <ecb | cfb | ofb | cbc>] [-pass <password>]
```

### Extraer

```
stegobmp -extract -p <bitmap portador> -out <archivo salida> -steg <LSB1 | LSB4 | LSBI>
         [-a <aes128 | aes192 | aes256 | 3des>] [-m <ecb | cfb | ofb | cbc>] [-pass <password>]
```

Al nombre indicado en `-out` se le agrega la extensión del archivo oculto: con `-out mensaje1` se escribe
`mensaje1.txt` si lo oculto era un `.txt`.

### Encripción

- Sin `-pass` no se encripta ni desencripta (si se indicó `-a` o `-m` se muestra una advertencia).
- Con `-pass` y sin `-a` se usa `aes128`; con `-pass` y sin `-m` se usa `cbc`.

### Estegoanálisis

```
stegobmp -analyze -p <bitmap> [-steg <LSB1 | LSB4 | LSBI>] [-out <prefijo salida>]
         [-a <aes128 | aes192 | aes256 | 3des>] [-m <ecb | cfb | ofb | cbc>] [-pass <password>]
```

Prueba LSB1, LSB4 y LSBI (o sólo el método de `-steg`) e informa para cada uno si encontró un archivo
oculto válido. Sin `-pass`, si el tamaño oculto es válido pero no hay una extensión después de los datos,
avisa que puede tratarse de información encriptada e indica qué tamaños de bloque son compatibles.
También informa si hay bytes después del bloque de píxeles.

Con `-out`, guarda lo encontrado como `<prefijo>_<método><extensión>`, los posibles cifrados como
`<prefijo>_<método>.enc` y los bytes extra como `<prefijo>_extra.bin`.

```bash
./stegobmp -analyze -p ladoLSBIdescfb.bmp
./stegobmp -analyze -p ladoLSBIdescfb.bmp -steg LSBI -a 3des -m cfb -pass margarita -out encontrado
```

Ante un error (parámetros inválidos, archivo inexistente, BMP no soportado, capacidad insuficiente,
password incorrecta, etc.) el programa lo informa por la salida de error y termina con código 1. Si el error
es de parámetros, muestra además la sintaxis correcta. `stegobmp -help` muestra la ayuda.

## Detalles de implementación

- **BMP**: se aceptan BMP de 24 bits por pixel sin compresión. El portador es el bloque de píxeles completo,
  incluyendo el padding de cada fila; el encabezado y cualquier byte posterior quedan intactos.
- **Formato oculto**: `tamaño (4 bytes, big endian) || datos || extensión`, donde la extensión empieza con
  `.` y termina en `\0`. Un archivo sin extensión se guarda como `.\0`. Con encripción se oculta
  `tamaño del cifrado || encripción(tamaño || datos || extensión)`.
- **Orden de los bits**: cada byte se oculta del bit más significativo al menos significativo; en LSB4,
  primero el nibble alto.
- **LSBI**: los primeros 4 bytes del bloque de píxeles guardan con LSB1 si se invirtió cada patrón
  (`00`, `01`, `10`, `11`, formado por los bits 2 y 1 del byte). El mensaje se oculta desde el byte 4 con
  LSB1, sólo en los bytes azul y verde (se saltean los de índice `% 3 == 2`, contando el bloque de píxeles
  como un único flujo con el padding incluido). Un patrón se invierte si, entre los bytes usados por el
  mensaje, hay más bytes que cambiaron que bytes que no cambiaron.
- **Encripción**: clave e IV derivados con PBKDF2-HMAC-SHA256, 10000 iteraciones y salt de 8 bytes en 0.
  Se derivan `largo de clave + largo de IV` bytes: primero la clave y después el IV. ECB y CBC usan padding
  PKCS5; CFB es CFB8 (8 bits de feedback) y OFB usa el bloque completo (128 bits en AES).
- **Capacidad**: si el archivo no entra en el portador, se informa la capacidad máxima del portador para el
  método elegido.

## Tests

```bash
cd cripto-project
mvn test
```

Además de los tests unitarios, `CourseExamplesTest` usa los archivos de ejemplo de la cátedra
(`TP/ejemploTP.zip`): verifica que se extraiga la imagen oculta en cada uno y que, al volver a ocultarla en
`lado.bmp`, el resultado sea idéntico byte a byte al archivo de la cátedra. Si el zip no está, esos tests se
saltean.

> Nota: el `README.txt` de los ejemplos indica para `ladoLSB1aes128cbc.bmp` el IV
> `212420edc583a686a94d19a3497363a2`, pero ese es el IV de AES-256. El archivo se desencripta con el IV
> derivado `b25f8d99f3173ec0b52849f459a4c20d` (bytes 16 a 31 de la salida de PBKDF2).
