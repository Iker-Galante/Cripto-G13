# stegobmp

Hides a file inside a 24-bit BMP image and gets it back.

## Build

Needs Java 21 and Maven. From this folder:

```
mvn -q package
```

This creates `target/stegobmp.jar`.

## Usage

```
./stegobmp -embed -in <file> -p <bitmap> -out <bitmap> -steg <LSB1 | LSB4 | LSBI> [-a <aes128 | aes192 | aes256 | 3des>] [-m <ecb | cfb | ofb | cbc>] [-pass <password>]
./stegobmp -extract -p <bitmap> -out <file> -steg <LSB1 | LSB4 | LSBI> [-a <aes128 | aes192 | aes256 | 3des>] [-m <ecb | cfb | ofb | cbc>] [-pass <password>]
```

- `-embed` hides `-in` inside the image `-p` and writes the result to `-out`.
- `-extract` recovers the file hidden in `-p`. Give `-out` without an extension: the program adds
  the extension stored with the file (`-out back` gives `back.txt`).
- `-pass` encrypts the file before hiding it (and decrypts it on extract). The key and IV come
  from the password with PBKDF2-HMAC-SHA256. Extract needs the same `-steg`, `-a`, `-m` and
  `-pass` that embed used.

Encryption defaults:

| Given | Result |
| --- | --- |
| No `-pass` | No encryption, even if `-a` or `-m` is given |
| `-pass` only | `aes128`, `cbc` |
| `-pass` and `-a` | that algorithm, `cbc` |
| `-pass` and `-m` | `aes128`, that mode |
| All three | as given |

A password must not be empty and must not start with `-` (the parser would read it as the next
option).

Example from the assignment, with 3DES:

```
./stegobmp -embed -in "mensaje1.txt" -p "imagen1.bmp" -out "imagenmas1.bmp" -steg LSBI -a 3des -m cbc -pass "oculto"
./stegobmp -extract -p "imagenmas1.bmp" -out "mensaje1_back" -steg LSBI -a 3des -m cbc -pass "oculto"
cmp mensaje1.txt mensaje1_back.txt
```

## Tests

`STEGOBMP_SAMPLES=/path/to/ejemplo mvn -q test` also runs the checks against the course's sample
images; without the variable they are skipped.
