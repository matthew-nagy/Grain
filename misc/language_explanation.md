# How to use Grain

This is going to assume you already know how to program. Some knowlage 
of how the SNES works is assumed, the YouTube channel [Retro Game 
Mechanics Explained](https://www.youtube.com/@RGMechEx) has some fantastic 
videos on SNES features. (They also commented on [my first YouTube video](
https://youtu.be/239N5OOyhJY
) on 
this project)

## Program necessities
A `main` function is always needed. This is the entry point to the program. It should take 
no arguments and return nothing. Due to a poor design decision at the start of the project, 
it is currently not able to be called recursivly.

A `VBlank` function is also needed. This is run once a frame while graphics 
aren't being drawn to the screen. This makes it the best time to update graphics data. 
Due to a mix of poor design on my part and the horrible design of 90's tech, this 
function is also not recursable, although you really *shouldn't* want to do that.

## Variables and types
### Basic Types
Grain only has a few standard types you can use:
- `word`: a 16 bit signed integer type
- `bool`: can be `true` or `false`. Also 16 bits because of some poor early decisions. 
This'll probably change in later versions

### Pointers and Arrays
You can put `ptr` as many times as you like after these basic types to make 
it a pointer to it.
You can put square brackets with a number in them to create a set size array. You 
can't use these for function arguments or as return types, but arrays can be implicitly 
converted into pointers, so pointer arguments are the way to go.

### High WRAM
The keyword `hiram` can be put before the type for a global variable's 
declaration. This puts the memory in higher WRAM not accessable in bank 0. 

For the sake of the tutorial, all this tends to mean is there is around a 20% increase in the cycles needed to 
read or write to it, and any pointer to it given to a standard library function probably 
won't work. It may in fact go horribly wrong.

If you want more information watch some RGME videos.

## Inline Assembly
Sometimes you just need the SNES to do some funky stuff that I haven't covered in 
the provided standard library. You can use the keyword `asm` then a block 
of strings to input raw assembly into the program. As an example:

```
asm{
    "pha"   //Preserve A
    "lda #$F1"//Some constant
    "clc"
    "adc 4  ;Add whats in 4" //<- assembly comment
    "sta 6"
    "pla"//Store the result and reset the state
}
```

## Functions

```
func exampleSubroutine(){
    ...
}
//Takes in a bool, returns a word
func exampleFunction(someArgument: bool): word{
    ...
}
```

If a function is going to be written in pure assembly, you can put 
the keyword `asm` after the return type (or where one would be), and then 
it is assumed to only be assembly in the braces.

This can give a small efficiency increase because it doesn't make a stack frame 
or auto-insert a return at the end. As the optimiser got better the benefits got 
less and less, but it saves at most about 12 cycles, probably less.

---

---

I'll get to these sections in a bit, exam prep is taking priority

You can look into snake.txt or Akermann.grain to try glean more stuff

I sort of played a bit fast and loose with file extentions; its all text at the end of the day


## Classes/Structs

While they are refered to with the keyword `class`, they are a lot closer 
to C structs. They may have member variables and member functions, however 
there is no inheritance or overloading.

When function pointers are supported, faux overloading could be performed by 
giving an object function pointers. If these functions have their last argument be 
a pointer to the objects type, it will pass itself automatically, as this is 
how member functions work under the hood.

Example:
```
class Vec2{
    x: word
    y: word
    
    func set(x: word, y: word){
        @this.x = x
        @this.y = y
    }
    func offset(dx: word, dy: word){
        @this.x += dx
        @this.y += dy
    }
}
```
You cannot have a class as a return type currently, so if you need to return one 
you should pass in a pointer to an object. This was due to some poor planning 
at the start of the project, and may be fixed later if I do some reworks

## Loading data

There are three different filetypes directly supported. PNG files, CSV files, and 
binary files.

Some language features are provided to help you get information about the data you 
have loaded which is of use if you plan to use it with library functions. 

`sizeof(foo)` will give you the size in bytes of the variable `foo`

`bankof(foo)` will give you the data bank the variable `foo` is stored in

`bppof(foo)` will give you the bits per pixel of `foo`, assuming that makes sense for the type.
Otherwise you'll be given an error.

PNG example:
```
load enemySprites, enemyPalette from "../art/enemySpriteImage.png"
```
This loads in enemySpriteImage.png into ROM. The variable `enemySprites` 
holds a pointer to it. It converts the colours into a SNES palette and stores 
a pointer to that in `enemyPalette`. Their types are `tile_data ptr` and 
`palette_data ptr` respectivly. 


CSV/BIN example:
```
load level1Map from "../maps/level1.csv"
```
`level1Map` will have the type `rom_word[x]` where x is the size of the file in words. 
rom_word can't be modified *or read*. This isn't ideal. In the future when `const` is implimented 
and the way the IR and optimisations handle loading has changed a bit, it will just be an array 
of `const word`. For now if you want to access it you will have to DMA it into WRAM.

Binary files can be used to load in any data you need. CSV's were specifically used because most professional 
tilemap making software (such as [tiled](https://www.mapeditor.org/)) let you export maps as CSV.

## Including files
`include pathToMyFile` includes the file in the same way as C includes files.
In the future I want to add something that prevents the same file from being included 
multiple times, as that really prevents larger projects from being built.

By including files stored in the src/main/GrainLib folder you can use some assembly functions I have 
written to access certain SNES functions not encapsulated by this language.