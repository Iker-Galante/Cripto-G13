# stegobmp

Hides a file inside a 24-bit BMP image and gets it back.

## Build

Needs Java 21 and Maven. From this folder:

```
make
```

This creates `target/stegobmp.jar`, which the `stegobmp` script in this folder runs, and makes the
script executable. `make clean` removes the build.

Without `make`, run `mvn -q package` instead. If the script lost its execute permission (a zip made
on Windows drops it), run `chmod +x stegobmp` once.

## Usage

```
./stegobmp -embed -in <file> -p <bitmap> -out <bitmap> -steg <LSB1 | LSB4 | LSBI> [-a <aes128 | aes192 | aes256 | 3des>] [-m <ecb | cfb | ofb | cbc>] [-pass <password>]
./stegobmp -extract -p <bitmap> -out <file> -steg <LSB1 | LSB4 | LSBI> [-a <aes128 | aes192 | aes256 | 3des>] [-m <ecb | cfb | ofb | cbc>] [-pass <password>]
./stegobmp -analyze -p <bitmap> [-steg <LSB1 | LSB4 | LSBI>] [-out <prefix>] [-a <aes128 | aes192 | aes256 | 3des>] [-m <ecb | cfb | ofb | cbc>] [-pass <password>]
```

- `-embed` hides `-in` inside the image `-p` and writes the result to `-out`.
- `-extract` recovers the file hidden in `-p`. Give `-out` without an extension: the program adds
  the extension stored with the file (`-out back` gives `back.txt`).
- `-pass` encrypts the file before hiding it (and decrypts it on extract). The key and IV come
  from the password with PBKDF2-HMAC-SHA256. Extract needs the same `-steg`, `-a`, `-m` and
  `-pass` that embed used.
- `-analyze` looks for a hidden file in `-p` when the method is unknown (see below).

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

## Analyzing an unknown image

`-analyze` tries LSB1, LSB4 and LSBI (or only the `-steg` method) and prints one line per check:

- the file found with each method, with its size and extension;
- for a method that finds no plain file but reads a valid length, a warning that the data may be
  encrypted, with a hint from that length (a multiple of 16 fits any algorithm and mode) or, when
  cipher blocks repeat, the mode: only `ecb` turns equal plain blocks into equal cipher blocks;
- any bytes stored after the pixel rows the header declares, shown as text when they are printable.

With `-out <prefix>` it also saves what it finds as `<prefix>_<method><extension>`,
`<prefix>_<method>.enc` (the possibly encrypted bytes) or `<prefix>_trailing.bin`. With `-a`, `-m`
and `-pass` it decrypts before looking for the file.

```
$ ./stegobmp -analyze -p paris.bmp
Analysis of paris.bmp (1600x1000)
  LSB1: nothing found
  LSB4: nothing found
  LSBI: nothing found
  21 bytes after the pixel array: "la password es gloria"
```

## Tests

`make test` runs the tests, including the checks against the course's sample images in
`../TP/ejemplo`. Set `STEGOBMP_SAMPLES=/path/to/ejemplo` to take the images from another folder; if
the folder does not exist those checks are skipped.

With Maven directly, `mvn -q test` skips them unless `STEGOBMP_SAMPLES` is set.
