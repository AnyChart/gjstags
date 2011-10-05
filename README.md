# gjstags
CTags generator for JavaScript.  
Licensed under Apache License 2.0. Based on Google Closure Compiler.
Works under Mac OS X or Linux.

## Installation
### Cloning source:
```
git clone git://github.com/AnyChart/gjstags.git
```
### Building:
```
./configure
make
make install
```

#### ./configure arguments:
`--prefix path` /usr/local by default  
`--with-javac javac` javac by default  
`--with-jar jar-path` jar by default  


## Usage
```
gjstags [options] file(s)
```
see `gjstags --help` for more details.

## Uninstall
```
make clean
```