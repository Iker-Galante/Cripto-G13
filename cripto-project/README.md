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
./stegobmp -embed -in <file> -p <bitmap> -out <bitmap> -steg <LSB1 | LSB4>
./stegobmp -extract -p <bitmap> -out <file> -steg <LSB1 | LSB4>
```

- `-embed` hides `-in` inside the image `-p` and writes the result to `-out`.
- `-extract` recovers the file hidden in `-p`. Give `-out` without an extension: the program adds
  the extension stored with the file (`-out back` gives `back.txt`).

`LSB1` and `LSB4` work; no encryption yet.

Example:

```
./stegobmp -embed -in hello.txt -p image.bmp -out with_secret.bmp -steg LSB1
./stegobmp -extract -p with_secret.bmp -out recovered -steg LSB1
cmp hello.txt recovered.txt
```

## Tests

`STEGOBMP_SAMPLES=/path/to/ejemplo mvn -q test` also runs the checks against the course's sample
images; without the variable they are skipped.
