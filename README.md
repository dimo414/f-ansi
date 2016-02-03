# F-ANSI

A fluent Java API to control console output via ANSI control codes

## Background

Console output doesn't have to be boring black and white. But creating visually
appealing command line programs isn't straightforward. The standard mechanism,
[ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code), are
confusing and tedious to work with. F-ANSI provides an intuitive, fluent
abstraction to create elegant applications without the hassle.

## Usage

**In Alpha**: F-ANSI is still in active development, and the API may change.

F-ANSI's fluent interface allows you to compose readable chains of output; the
following example prints "Hello" in red, followed by a hyphen in the terminal's
default styling, folowed by "World" in green.

    ansi().color(RED).out("Hello").out(" - ").color(LIGHT_GREEN).outln("World");

## Copyright

Copyright 2016 Michael Diamond

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.